package org.wso2.gw.emulator.http.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.apache.log4j.Logger;
import org.wso2.gw.emulator.http.server.contexts.HttpServerInformationContext;

import java.util.concurrent.*;

public class HttpChunkedWriteHandler extends ChunkedWriteHandler {
    private static final Logger log = Logger.getLogger(HttpChunkedWriteHandler.class);
    private final HttpServerInformationContext serverInformationContext;
    private final ScheduledExecutorService scheduledWritingExecutorService;
    private final int corePoolSize = 10;

    public HttpChunkedWriteHandler(HttpServerInformationContext serverInformationContext) {
        this.serverInformationContext = serverInformationContext;
        scheduledWritingExecutorService = Executors.newScheduledThreadPool(corePoolSize);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        waitingDelay(serverInformationContext.getServerConfigBuilderContext().getWritingDelay());
    }

    private void waitingDelay(int delay) {
        if (delay != 0) {
            ScheduledFuture scheduledWaitingFuture =
                    scheduledWritingExecutorService.schedule(new Callable() {
                        public Object call() throws Exception {
                            return "Writing";
                        }
                    }, delay, TimeUnit.MILLISECONDS);
            try {
                log.info("result = " + scheduledWaitingFuture.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            scheduledWritingExecutorService.shutdown();
        }
    }
}
