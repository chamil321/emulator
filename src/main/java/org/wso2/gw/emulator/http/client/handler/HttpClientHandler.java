/*
 * *
 *  * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.wso2.gw.emulator.http.client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import org.wso2.gw.emulator.http.client.contexts.HttpClientInformationContext;
import org.wso2.gw.emulator.http.client.contexts.HttpClientResponseProcessorContext;
import org.wso2.gw.emulator.http.client.processors.HttpResponseAssertProcessor;
import org.wso2.gw.emulator.http.client.processors.HttpResponseInformationProcessor;

import java.util.concurrent.*;

public class HttpClientHandler extends ChannelInboundHandlerAdapter {
    private HttpResponseInformationProcessor responseInformationProcessor;
    private HttpResponseAssertProcessor responseAssertProcessor;
    private HttpClientResponseProcessorContext processorContext;
    private HttpClientInformationContext clientInformationContext;
    private ScheduledExecutorService scheduledReadingExecutorService;
    private boolean isReadComplete = false;
    private int corePoolSize = 10;

    public HttpClientHandler(HttpClientInformationContext clientInformationContext) {
        this.clientInformationContext = clientInformationContext;
        scheduledReadingExecutorService = Executors.newScheduledThreadPool(corePoolSize);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpResponse) {
            this.processorContext = new HttpClientResponseProcessorContext();
            this.processorContext.setClientInformationContext(clientInformationContext);
            this.responseInformationProcessor = new HttpResponseInformationProcessor();
            this.responseAssertProcessor = new HttpResponseAssertProcessor();
            HttpResponse response = (HttpResponse) msg;
            processorContext.setReceivedResponse(response);
            responseInformationProcessor.process(processorContext);
            isReadComplete = false;
        }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            ByteBuf content = httpContent.content();

            if (content.isReadable()) {
                this.responseInformationProcessor.appendDecoderResult(processorContext.getReceivedResponseContext(), httpContent, content);
            }
        }
        if (msg instanceof LastHttpContent) {
            isReadComplete = true;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        if (responseAssertProcessor != null && isReadComplete) {
            this.responseAssertProcessor.process(processorContext);
            this.clientInformationContext.setReceivedResponseProcessContext(processorContext);
        }
        ctx.close();
    }

    private void readingDelay(int delay) {
        ScheduledFuture scheduledFuture =
                scheduledReadingExecutorService.schedule(new Callable() {
                    public Object call() throws Exception {
                        return "Client Reading";
                    }
                }, delay, TimeUnit.MILLISECONDS);
        try {
            System.out.println("result = " + scheduledFuture.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        scheduledReadingExecutorService.shutdown();
    }
}
