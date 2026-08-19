package com.diamond.leads;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class BackendDiamondTp1Application {

    public static void main(String[] args) {
        SpringApplication.run(BackendDiamondTp1Application.class, args);
    }
}