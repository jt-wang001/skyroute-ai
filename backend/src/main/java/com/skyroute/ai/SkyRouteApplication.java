package com.skyroute.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.skyroute.ai.mapper")
@SpringBootApplication
public class SkyRouteApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkyRouteApplication.class, args);
    }
}
