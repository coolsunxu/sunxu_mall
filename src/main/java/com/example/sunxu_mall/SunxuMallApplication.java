package com.example.sunxu_mall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRetry
@EnableScheduling
@SpringBootApplication
@MapperScan("com.example.sunxu_mall.mapper")
public class SunxuMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(SunxuMallApplication.class, args);
    }
}
