package com.xcxcxcxcx.bean;


import java.io.Serializable;

/**
 * @author XCXCXCXCX
 * @date 2018/9/2
 * @comments
 */
public class RpcRequest implements Serializable{


    private static final long serialVersionUID = 9137315706822656792L;

    private String serviceName;

    private String methodName;

    private Class[] paramType;

    private Class returnType;

    //private MethodType methodType;

    private Object[] args;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class[] getParamType() {
        return paramType;
    }

    public void setParamType(Class[] paramType) {
        this.paramType = paramType;
    }

    public Class getReturnType() {
        return returnType;
    }

    public void setReturnType(Class returnType) {
        this.returnType = returnType;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }
}
