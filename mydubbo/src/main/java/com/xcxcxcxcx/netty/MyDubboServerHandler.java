package com.xcxcxcxcx.netty;

import com.xcxcxcxcx.bean.RpcRequest;
import com.xcxcxcxcx.bean.RpcResponse;
import com.xcxcxcxcx.test.TestService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author XCXCXCXCX
 * @date 2018/9/2
 * @comments
 */
public class MyDubboServerHandler extends ChannelInboundHandlerAdapter{

    private RpcInvocationHandler rpcInvocationHandler;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("client有请求!"+msg);
        //获取到client的请求对象，根据请求对象调用服务，并将执行结果封装后返回给客户端
        RpcRequest rpcRequest = (RpcRequest)msg;
        String serviceName = rpcRequest.getServiceName();
        Class service = Class.forName(serviceName);
        System.out.println(serviceName);
        System.out.println(rpcRequest.getMethodName());
        Method method = service.getMethod(rpcRequest.getMethodName(),rpcRequest.getParamType());
        //MethodType methodType = rpcRequest.getMethodType();
        //Class rerutnType = methodType.returnType();
        //Class[] pType = methodType.parameterArray();
        Object[] args = rpcRequest.getArgs();
        int start = serviceName.lastIndexOf(".") + 1;
        int end = serviceName.length();
        rpcInvocationHandler = new RpcInvocationHandler(serviceName.substring(start,end));
        Object serviceProxy = Proxy.newProxyInstance(service.getClassLoader(), service.getInterfaces(),rpcInvocationHandler);
        try {
            rpcInvocationHandler.invoke(serviceProxy,method,args);
            RpcResponse rpcResponse = new RpcResponse();
            rpcResponse.setReturnObject(rpcInvocationHandler.invoke(serviceProxy,method,args));
            Channel channel = ctx.channel();
            channel.writeAndFlush(rpcResponse);
            System.out.println(rpcResponse);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }


    }

    public static void main(String[] args) {
//        try {
//            Class t = TestService.class;
//            Method method = t.getMethod("hello");
//            Class returnType = String.class;
//            RpcInvocationHandler rpcInvocationHandler = new RpcInvocationHandler(t.getSimpleName());
//            Object serviceProxy = Proxy.newProxyInstance(t.getClassLoader(), t.getInterfaces(),rpcInvocationHandler);
//            Object returnVal = rpcInvocationHandler.invoke(serviceProxy,method,args);
//            System.out.println(returnVal);
//        } catch (Throwable throwable) {
//            throwable.printStackTrace();
//        }
    }
}
