package com.xcxcxcxcx;

import com.xcxcxcxcx.bean.RpcRequest;
import com.xcxcxcxcx.netty.MyDubboServerHandler;
import com.xcxcxcxcx.zookeeper.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

/**
 * @author XCXCXCXCX
 * @date 2018/9/2
 * @comments
 */
public class Main {

    public void init() {
        //初始化注册中心,注册已有服务
        new ServiceRegistry().registerService("TestService", "127.0.0.1:8080");
        //开启socket监听，等待客户端调用服务
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast("decoder", new ObjectDecoder(ClassResolvers.softCachingConcurrentResolver(RpcRequest.class.getClassLoader())));
                            pipeline.addLast("handler", new MyDubboServerHandler());
                            pipeline.addLast("encoder", new ObjectEncoder());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            System.out.println("MyDubboServer启动成功!");
            ChannelFuture channelFuture = bootstrap.bind(8080).sync();
            channelFuture.channel().closeFuture().sync();
            channelFuture.channel().writeAndFlush("MyDubboServer已关闭!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            System.out.println("MyDubboServer关闭成功!");
        }

    }

    public static void main(String[] args) {
        new Main().init();
    }
}
