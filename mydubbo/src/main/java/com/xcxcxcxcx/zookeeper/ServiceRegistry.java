package com.xcxcxcxcx.zookeeper;

import com.xcxcxcxcx.exception.NullParamException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

/**
 * @author XCXCXCXCX
 * @date 2018/9/2
 * @comments
 */
public class ServiceRegistry {

    private static Logger log = LogManager.getLogger("ServiceRegistry");

    private CuratorFramework client = CuratorManager.INSTANCE.getClient();

    public void registerService(String serviceName,String serviceAddress){
        client.start();
        if(serviceName==null||serviceAddress==null){
            throw new NullParamException("ServiceRegistry: serviceName and serviceAddress should not be null!");
        }

        String path = "/registry/" + serviceName;
        try {
            if(client.checkExists().forPath(path) == null){
                client.create().creatingParentContainersIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path,serviceAddress.getBytes());
                log.info("服务注册成功!");
            }else{
                client.setData().forPath(path,serviceAddress.getBytes());
                log.info("服务已存在，更新成功!");
            }
//            System.out.println(client.getChildren().forPath(path));
//            System.out.println(new String(client.getData().forPath(path)));
//            Stat stat = new Stat();
//            System.out.println(new String(client.getData().storingStatIn(stat).forPath(path)));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String discoverService(String serviceName){
        client.start();
        //获取服务地址
        if(serviceName==null){
            throw new NullParamException("ServiceRegistry: serviceName should not be null!");
        }
        String path = "/registry/"+serviceName;
        try {
            return new String(client.getData().forPath(path));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
