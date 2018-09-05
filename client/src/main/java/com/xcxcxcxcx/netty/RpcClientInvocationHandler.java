package com.xcxcxcxcx.netty;

import com.xcxcxcxcx.bean.RpcRequest;
import com.xcxcxcxcx.bean.RpcResponse;
import com.xcxcxcxcx.zookeeper.ServiceRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author XCXCXCXCX
 * @date 2018/9/3
 * @comments
 */
public class RpcClientInvocationHandler implements InvocationHandler {

    private static final String classPrefix = "com.xcxcxcxcx.test.";

    private String serviceName;

    private EventLoopGroup group;

    private Bootstrap bootstrap;

    private ServiceRegistry serviceRegistry = new ServiceRegistry();

    public RpcClientInvocationHandler(String ServiceName) {
        this.serviceName = ServiceName;
        //建立连接前的channel初始化
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();

                        pipeline.addLast("decoder", new ObjectDecoder(ClassResolvers.softCachingConcurrentResolver(RpcResponse.class.getClassLoader())));
                        pipeline.addLast("handler", new MyDubboClientHandler());
                        pipeline.addLast("encoder", new ObjectEncoder());

                    }
                });
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("TestService方法执行!");
        //创建RpcRequest
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setServiceName(classPrefix + serviceName);
        rpcRequest.setMethodName(method.getName());
        rpcRequest.setParamType(method.getParameterTypes());
        rpcRequest.setReturnType(method.getReturnType());
        rpcRequest.setArgs(args);

        //服务发现，解析host和port
        String serviceAddress = serviceRegistry.discoverService(serviceName);
        String host = serviceAddress.split(":")[0];
        int port = Integer.parseInt(serviceAddress.split(":")[1]);
        System.out.println(host+":"+port);

        //传递RpcRequest对象，调用远程方法，并异步获取执行结果
        Channel channel = bootstrap.connect(host, port).sync().channel();
        channel.writeAndFlush(rpcRequest);

        return null;
    }

    @Override
    protected void finalize() throws Throwable {
        if (group != null) {
            group.shutdownGracefully();
            System.out.println("group销毁!");
        }
    }
}
