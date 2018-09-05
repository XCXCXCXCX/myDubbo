package com.xcxcxcxcx.test;

import com.xcxcxcxcx.annotation.RpcService;

/**
 * @author XCXCXCXCX
 * @date 2018/9/2
 * @comments
 */
@RpcService("TestService")
public class TestServiceImpl implements TestService{

    @Override
    public String hello() {
        System.out.println("hello test!");
        return "hello";
    }

    @Override
    public String hello(String helloStr) {
        return null;
    }
}
