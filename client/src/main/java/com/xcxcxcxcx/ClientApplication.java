package com.xcxcxcxcx;

import com.xcxcxcxcx.netty.RpcProxy;
import com.xcxcxcxcx.test.TestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
