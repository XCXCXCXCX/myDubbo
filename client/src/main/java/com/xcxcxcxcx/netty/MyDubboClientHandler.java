package com.xcxcxcxcx.netty;

import com.xcxcxcxcx.bean.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author XCXCXCXCX
 * @date 2018/9/3
 * @comments
 */
public class MyDubboClientHandler extends ChannelInboundHandlerAdapter{

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcResponse rpcResponse = (RpcResponse)msg;
        Object returnVal = rpcResponse.getReturnObject();
        System.out.println("调用服务后异步获取执行结果，服务返回值为:"+returnVal);
    }
}
