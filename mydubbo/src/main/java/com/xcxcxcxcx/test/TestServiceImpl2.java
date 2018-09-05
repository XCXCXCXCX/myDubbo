package com.xcxcxcxcx.test;

import com.xcxcxcxcx.annotation.RpcService;

/**
 * @author XCXCXCXCX
 * @date 2018/9/3
 * @comments
 */
//@RpcService("TestService")
public class TestServiceImpl2 implements TestService{

    @Override
    public String hello() {
        return null;
    }

    @Override
    public String hello(String helloStr) {
        System.out.println("hello test2!");
        return "服务调用成功了!===="+helloStr;
    }
}
