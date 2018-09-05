package com.xcxcxcxcx.test;

import com.xcxcxcxcx.netty.RpcProxy;

/**
 * @author XCXCXCXCX
 * @date 2018/9/3
 * @comments
 */
public class RpcTest{

    public static void main(String[] args) {
        RpcProxy rpcProxy = new RpcProxy("TestService");

        //调用方式一
        rpcProxy.execute("hello");
        rpcProxy.execute("hello","hahahaha");

        //调用方式二
        TestService testService = (TestService) rpcProxy.create();
        testService.hello();
        testService.hello("hohohoho");
    }
}
