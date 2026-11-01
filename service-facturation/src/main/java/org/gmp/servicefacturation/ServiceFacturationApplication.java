package org.gmp.servicefacturation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ServiceFacturationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceFacturationApplication.class, args);
    }

}
