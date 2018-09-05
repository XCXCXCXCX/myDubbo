package com.xcxcxcxcx.bean;

import java.io.Serializable;

/**
 * @author XCXCXCXCX
 * @date 2018/9/3
 * @comments
 */
public class RpcResponse implements Serializable{

    private static final long serialVersionUID = -310298585211189711L;

    private Class returnType;

    private Object returnObject;

    public Class getReturnType() {
        return returnType;
    }

    public void setReturnType(Class returnType) {
        this.returnType = returnType;
    }

    public Object getReturnObject() {
        return returnObject;
    }

    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }
}
