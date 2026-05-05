package com.orang.notificationservice;

import com.orang.shared.autoconfigure.SharedSchedulerAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {SharedSchedulerAutoConfiguration.class})
@ComponentScan(basePackages = {
        "com.orang.notificationservice",
        "com.orang.shared.exception"
})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
