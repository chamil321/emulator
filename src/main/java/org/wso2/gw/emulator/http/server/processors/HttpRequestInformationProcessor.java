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

package org.wso2.gw.emulator.http.server.processors;

import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import org.wso2.gw.emulator.http.server.contexts.HttpServerInformationContext;
import org.wso2.gw.emulator.http.server.contexts.HttpServerProcessorContext;
import org.wso2.gw.emulator.http.server.contexts.HttpRequestContext;
import java.util.List;
import java.util.Map;

public class HttpRequestInformationProcessor extends AbstractServerProcessor {

    @Override
    public void process(HttpServerProcessorContext processorContext) {
        HttpRequest request = processorContext.getHttpRequest();
        HttpRequestContext requestContext = processorContext.getHttpRequestContext();
        populateRequestHeaders(request, requestContext);
        populateQueryParameters(request, requestContext);
        populateRequestContext(request, requestContext);
        populateHttpMethod(request, requestContext);
        populateHttpVersion(request, requestContext);
        populateConnectionKeepAlive(request, requestContext);

        if(processorContext.getHttpContent() != null) {
            appendDecoderResult(processorContext.getHttpContent(), requestContext);
        }
    }

    private void populateRequestHeaders(HttpRequest request, HttpRequestContext requestContext) {
        HttpHeaders headers = request.headers();
        if (!headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entries()) {
                requestContext.addHeaderParameter(entry.getKey(), entry.getValue());
            }
        }
    }

    private void appendDecoderResult(HttpContent httpContent, HttpRequestContext requestContext) {
        requestContext.appendResponseContent(httpContent.content().toString(CharsetUtil.UTF_8));
        DecoderResult result = httpContent.getDecoderResult();
        if (result.isSuccess()) {
            return;
        }
        requestContext.appendResponseContent(result.cause());
    }

    private void populateQueryParameters(HttpRequest request, HttpRequestContext requestContext) {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
        Map<String, List<String>> params = queryStringDecoder.parameters();
        requestContext.setQueryParameters(params);
    }

    private void populateRequestContext(HttpRequest request, HttpRequestContext requestContext) {
        requestContext.setUri(request.getUri());
    }

    private void populateHttpMethod(HttpRequest request, HttpRequestContext requestContext) {
        requestContext.setHttpMethod(request.getMethod());
    }

    private void populateHttpVersion(HttpRequest request, HttpRequestContext requestContext) {
        requestContext.setHttpVersion(request.getProtocolVersion());
    }

    private void populateConnectionKeepAlive(HttpRequest request, HttpRequestContext requestContext) {
        requestContext.setKeepAlive(HttpHeaders.isKeepAlive(request));
    }


}
