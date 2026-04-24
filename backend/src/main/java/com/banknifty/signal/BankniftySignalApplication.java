package com.banknifty.signal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
        HttpClientAutoConfiguration.class,
        RestClientAutoConfiguration.class
})
@EnableScheduling
@ConfigurationPropertiesScan
public class BankniftySignalApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankniftySignalApplication.class, args);
    }
}
