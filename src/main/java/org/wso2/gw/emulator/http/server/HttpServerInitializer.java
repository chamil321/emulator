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

package org.wso2.gw.emulator.http.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultExecutorServiceFactory;
import io.netty.util.concurrent.ExecutorServiceFactory;
import org.apache.log4j.Logger;
import org.wso2.gw.emulator.dsl.EmulatorType;
import org.wso2.gw.emulator.http.ChannelPipelineInitializer;
import org.wso2.gw.emulator.http.server.contexts.HttpServerInformationContext;
import org.wso2.gw.emulator.http.server.contexts.HttpServerOperationBuilderContext;
import sun.rmi.runtime.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

public class HttpServerInitializer extends Thread{
    private static final boolean SSL = System.getProperty("ssl") != null;
    private static final Logger log = Logger.getLogger(HttpServerInitializer.class);
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private HttpServerInformationContext serverInformationContext;
    private int bossCount;
    private int workerCount;
    private Properties prop;
    private InputStream inputStream;

    public HttpServerInitializer(HttpServerInformationContext serverInformationContext/*, HttpServerOperationBuilderContext serverOperationBuilderContext */){
        this.serverInformationContext = serverInformationContext;
        //this.serverOperationBuilderContext = serverOperationBuilderContext;
        prop = new Properties();
        String propFileName = "server.properties";
        inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
        if (inputStream != null) {
            try {
                prop.load(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        setBossCount();
        setWorkerCount();
    }

    public void run() {
        final SslContext sslCtx = null;
        /*if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }*/
        // Configure the server.
        ExecutorServiceFactory bossExecutorServiceFactory = new DefaultExecutorServiceFactory("sample-bosses");
        bossExecutorServiceFactory.newExecutorService(bossCount);

        ExecutorServiceFactory workerExecutorServiceFactory = new DefaultExecutorServiceFactory("sample-workers");
        workerExecutorServiceFactory.newExecutorService(workerCount);

        bossGroup = new NioEventLoopGroup(bossCount);
        workerGroup = new NioEventLoopGroup(workerCount);

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            ChannelPipelineInitializer channelPipelineInitializer = new ChannelPipelineInitializer(sslCtx,
                    EmulatorType.HTTP_SERVER);
            channelPipelineInitializer.setServerInformationContext(serverInformationContext);
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(channelPipelineInitializer);
            if (serverInformationContext.getServerConfigBuilderContext().getHost() != null && serverInformationContext.getServerConfigBuilderContext().getPort() != 0){
                ChannelFuture f = serverBootstrap.bind(serverInformationContext.getServerConfigBuilderContext().getHost()
                        , serverInformationContext.getServerConfigBuilderContext().getPort()).sync();
            f.channel().closeFuture().sync();

                }
            else{
                if (serverInformationContext.getServerConfigBuilderContext().getHost() == null) {
                    try {
                        throw new Exception("Host is not given");
                    } catch (Exception e) {
                        log.info(e);
                        System.exit(0);
                    }
                }
                else {
                    try {
                        throw new Exception("Port is not given");
                    } catch (Exception e) {
                        log.info(e);
                        System.exit(0);
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    private int getCPUCoreSize() {
        return Runtime.getRuntime().availableProcessors();
    }


    public void setBossCount() {
        this.bossCount = Integer.parseInt(prop.getProperty("boss_count"));;
    }

    public void setWorkerCount() {
        this.workerCount = Integer.parseInt(prop.getProperty("worker_count"));;
    }

   /* public int getBossCount() throws IOException {

        String boss_count = prop.getProperty("boss_count");
        return Integer.parseInt(boss_count);
    }

    public int getWorkerCount() {

        String worker_count = prop.getProperty("worker_count");
        return Integer.parseInt(worker_count);
    }*/

}
