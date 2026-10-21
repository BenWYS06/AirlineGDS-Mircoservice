package com.ben.services;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication(scanBasePackages = {"com.ben.services", "com.ben.common_lib"})
@EnableJpaAuditing
@EnableFeignClients
@EnableKafka
public class SeatServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeatServiceApplication.class, args);
	}

}
