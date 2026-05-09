package com.orang.userservice;

import com.orang.shared.autoconfigure.SharedSchedulerAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {SharedSchedulerAutoConfiguration.class})
@ComponentScan(basePackages = {
        "com.orang.userservice",
        "com.orang.shared.exception"
})
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
