package com.xcxcxcxcx.exception;

import com.xcxcxcxcx.zookeeper.ServiceRegistry;

/**
 * @author XCXCXCXCX
 * @date 2018/9/3
 * @comments
 */
public class ServiceNotFoundException extends RuntimeException{
    public ServiceNotFoundException(String msg){
        super(msg);
    }
}
