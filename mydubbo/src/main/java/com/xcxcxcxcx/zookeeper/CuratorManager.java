package com.xcxcxcxcx.zookeeper;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * @author XCXCXCXCX
 * @date 2018/9/2
 * @comments
 */
public enum CuratorManager {
    INSTANCE;

    private CuratorFramework client;

    private CuratorManager(){
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client =
                CuratorFrameworkFactory.builder()
                        .connectString("10.211.139.56:2181,10.211.139.56:2181,192.168.179.129:2181")
                        .sessionTimeoutMs(5000)
                        .connectionTimeoutMs(5000)
                        .retryPolicy(retryPolicy)
                        .build();
    }

    public CuratorFramework getClient(){
        return CuratorManager.INSTANCE.client;
    }
}
