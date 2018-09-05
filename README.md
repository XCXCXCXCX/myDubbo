# myDubbo
用netty、zookeeper自己动手实现dubbo基本功能    

###摘要
最近在学习zookeeper的选举机制和使用、java如何调用及应用场景，dubbo理所应当的进入了我的视野，作为阿里开源的强大中间件dubbo，我们也应该了解其基本原理和应用场景。
RPC，远程服务调用，dubbo作为服务调用的中间人，为服务消费者和服务提供者来提供服务，首先，要知道dubbo的主要功能：

 1. 提供服务注册和服务发现的功能（依赖zookeeper）
 2. 服务间的通讯
 3. 实现服务间无感知调用

了解这些之后，大致理一下我们主要需要做的事：

 1. （由于分布式情况下，需要保持一致性）需要从zookeeper上创建、查找服务
 2. 制定某种通讯协议实现服务间的通讯
 3. 使用某种设计模式来实现服务间的无感知调用

思路理清之后，可以开始动手了。
使用技术：
 - zookeeper
 - netty
 - jdk自带动态代理

###服务注册中心：服务注册和服务发现
由于需要调用zookeeper，先搭建好[zookeeper环境](https://blog.csdn.net/morning99/article/details/40426133)。

 1. 既然需要连接zookeeper，需要一个管理zk连接的类

```
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
```

 2. 有了zk的连接类，可以实现一个注册中心的类了

```
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

```


###指定通讯协议，利用netty实现异步通讯
谈到通讯，无非就是server和client，理清server和client的任务：
Server:

 - 开启监听，等待client请求
 - 当client请求调用服务时，查询服务是否已注册在dubbo上
 - 如果存在服务，调用该服务并返回该服务调用情况

Client:

 - 根据服务接口，向远程服务器发起请求
 - 当请求发送成功后，等待远程服务器的响应，获取服务调用情况

Server

```
/**
 * @author XCXCXCXCX
 * @date 2018/9/2
 * @comments
 */
public class Main {

    public void init() {
        //初始化注册中心,注册已有服务
        new ServiceRegistry().registerService("TestService", "127.0.0.1:8080");
        //开启socket监听，等待客户端调用服务
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast("decoder", new ObjectDecoder(ClassResolvers.softCachingConcurrentResolver(RpcRequest.class.getClassLoader())));
                            pipeline.addLast("handler", new MyDubboServerHandler());
                            pipeline.addLast("encoder", new ObjectEncoder());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            System.out.println("MyDubboServer启动成功!");
            ChannelFuture channelFuture = bootstrap.bind(8080).sync();
            channelFuture.channel().closeFuture().sync();
            channelFuture.channel().writeAndFlush("MyDubboServer已关闭!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            System.out.println("MyDubboServer关闭成功!");
        }

    }

    public static void main(String[] args) {
        new Main().init();
    }
}
```
为了玩点骚操作，通过扫描包的方式选择服务实现类
这里加载服务实现的时间点是在client发起请求时加载，当然也不可能每次请求都加载对吧~

```
/**
 * @author XCXCXCXCX
 * @date 2018/9/3
 * @comments
 */
public class RpcInvocationHandler implements InvocationHandler{

    private Object realImp;

    private static Map<String,Class> serviceMap = new HashMap<>();

    static {
        doScanAsFile(Main.class.getPackage());
        //serviceMap.put("TestService", TestServiceImpl2.class);
    }

    public static void doScanAsFile(Package pakage){
        String packageName = pakage.getName();
        String packageDirName = packageName.replace('.', '/');
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            // 循环迭代下去
            while (dirs.hasMoreElements()) {
                // 获取下一个元素
                URL url = dirs.nextElement();
                // 得到协议的名称
                String protocol = url.getProtocol();
                // 如果是以文件的形式保存在服务器上
                if ("file".equals(protocol)) {
                    // 获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件
                    scanAndLoadService(packageName,filePath);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void scanAndLoadService(String packageName,String filePath){
        File dir = new File(filePath);
        if(!dir.exists()){
            System.out.println(filePath+"不存在!");
            return;
        }
        System.out.println("扫描路径:"+filePath);
        if(dir.isDirectory()){
            File file[] = dir.listFiles(new FileFilter() {
                // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
                public boolean accept(File file) {
                    return (file.isDirectory())
                            || (file.getName().endsWith(".class"));
                }
            });

            for(int i = 0;i < file.length;i++){
                System.out.println(file[i].getPath());
                String preDirName = file[i].getName();
                System.out.println(preDirName);
                scanAndLoadService(packageName+"."+preDirName,file[i].getPath());
            }
        }else if(dir.isFile()){
            System.out.println("扫描类:"+dir.getPath());
            try {
//                int start = dir.getName().lastIndexOf("/") + 1;
//                int end = dir.getName().length();
//                String className = dir.getName().substring(start,end);
                int end = packageName.lastIndexOf(".");
                packageName = packageName.substring(0,end);
                Class t = Thread.currentThread().getContextClassLoader().loadClass(packageName);
                RpcService rpcServiceAnnotion = (RpcService)t.getAnnotation(RpcService.class);
                if (rpcServiceAnnotion != null) {
                    //加入到serviceMap
                    serviceMap.put(rpcServiceAnnotion.value(),t);
                    System.out.println(rpcServiceAnnotion.value());
                    System.out.println(serviceMap.get(rpcServiceAnnotion.value()));
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public RpcInvocationHandler(String serviceName){
        if(!serviceMap.containsKey(serviceName)){
            throw new ServiceNotFoundException(serviceName+"==>该服务不存在");
        }
        try {
            realImp = serviceMap.get(serviceName).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
            throw new ServiceNotFoundException(serviceName+"==>该服务不存在");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new ServiceNotFoundException(serviceName+"==>该服务不存在");
        }

    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(realImp,args);
    }
}
```
当client请求数据到达时，如何处理...

```
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
```

Client:
对应客户端netty代码

```
/**
 * @author XCXCXCXCX
 * @date 2018/9/3
 * @comments
 */
public class RpcClientInvocationHandler implements InvocationHandler {

    private static final String classPrefix = "com.xcxcxcxcx.test.";

    private String serviceName;

    private EventLoopGroup group;

    private Bootstrap bootstrap;

    private ServiceRegistry serviceRegistry = new ServiceRegistry();

    public RpcClientInvocationHandler(String ServiceName) {
        this.serviceName = ServiceName;
        //建立连接前的channel初始化
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();

                        pipeline.addLast("decoder", new ObjectDecoder(ClassResolvers.softCachingConcurrentResolver(RpcResponse.class.getClassLoader())));
                        pipeline.addLast("handler", new MyDubboClientHandler());
                        pipeline.addLast("encoder", new ObjectEncoder());

                    }
                });
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("TestService方法执行!");
        //创建RpcRequest
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setServiceName(classPrefix + serviceName);
        rpcRequest.setMethodName(method.getName());
        rpcRequest.setParamType(method.getParameterTypes());
        rpcRequest.setReturnType(method.getReturnType());
        rpcRequest.setArgs(args);

        //服务发现，解析host和port
        String serviceAddress = serviceRegistry.discoverService(serviceName);
        String host = serviceAddress.split(":")[0];
        int port = Integer.parseInt(serviceAddress.split(":")[1]);
        System.out.println(host+":"+port);

        //传递RpcRequest对象，调用远程方法，并异步获取执行结果
        Channel channel = bootstrap.connect(host, port).sync().channel();
        channel.writeAndFlush(rpcRequest);

        return null;
    }

    @Override
    protected void finalize() throws Throwable {
        if (group != null) {
            group.shutdownGracefully();
            System.out.println("group销毁!");
        }
    }
}
```

client处理响应数据，获取服务调用情况

```
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
```


###jdk动态代理实现无感知调用
先感受一下"无感知"

```
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

        //调用方式二：跟调本地服务有啥分别？？
        TestService testService = (TestService) rpcProxy.create();
        testService.hello();
        testService.hello("hohohoho");
    }
}
```
client端的RpcProxy代理类

```
/**
 * @author XCXCXCXCX
 * @date 2018/9/3
 * @comments
 */
public class RpcProxy {

    private static final String classPrefix = "com.xcxcxcxcx.test.";

    ThreadLocal<RpcClientInvocationHandler> rpcClientThreadLocal = new ThreadLocal<>();

    private String serviceName;

    public RpcProxy(String serviceName){
        this.serviceName = serviceName;
        rpcClientThreadLocal.set(new RpcClientInvocationHandler(serviceName));
    }

    public Object create(){
        Class service = null;
        try {
            service = Class.forName(classPrefix + serviceName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Object o = Proxy.newProxyInstance(service.getClassLoader(),new Class[]{service},rpcClientThreadLocal.get());
        return o;
    }

    public void execute(String methodName,Object... args){

        //获取参数类型
        Class[] paramType = new Class[args.length];
        for(int i = 0;i < args.length;i++){
            paramType[i] = args[i].getClass();
            System.out.println("paramType["+i+"]的类型名:"+paramType[i].getTypeName());
        }

        try {
            Class service = Class.forName(classPrefix + serviceName);
            Method method = service.getMethod(methodName,paramType);
            Object o = Proxy.newProxyInstance(service.getClassLoader(),new Class[]{service},rpcClientThreadLocal.get());
            rpcClientThreadLocal.get().invoke(o,method,args);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
```

完成了，测试一下：

 1. 先启动server

![这里写图片描述](https://img-blog.csdn.net/20180905193023420?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3hjMTE1ODg0MDY1Nw==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

 2. 启动client，先看server端console
![这里写图片描述](https://img-blog.csdn.net/20180905193109176?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3hjMTE1ODg0MDY1Nw==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)
![这里写图片描述](https://img-blog.csdn.net/2018090519322396?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3hjMTE1ODg0MDY1Nw==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)
 3. 再看看client端console
![这里写图片描述](https://img-blog.csdn.net/20180905193253699?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3hjMTE1ODg0MDY1Nw==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)
 4. 为了方便测试，client写了一个controller通过浏览器发请求

```
@SpringBootApplication
@RestController
public class ClientApplication {

	TestService testService = (TestService)new RpcProxy("TestService").create();


	@GetMapping("/test1")
	public String test1(){
		return testService.hello();
	}

	@GetMapping("/test2")
	public String test2(@RequestParam(defaultValue = "hello world")String helloStr){
		return testService.hello(helloStr);
	}

	public static void main(String[] args) {
		SpringApplication.run(ClientApplication.class, args);
	}
}
```

###小结
我感觉到我已经开始有代码洁癖了，动手后总觉得有很多不足，比如：

 - server、client没有分离打包，可以实现dubbo服务启动后，用多个服务引入mydubbo-client jar包来测试
 - 服务实现类的装载时间，可以在启动时装载，减少第一次调用的延迟时间
 - 命名上不是很规范，没有达到非常易懂的程度

但也懒得再改了，希望积累多了以后能一动手就写出优美的代码，dubbo的学习告一段落了，继续加油吧

demo上传至我的GitHub
有不足之处望指出
欢迎交流
