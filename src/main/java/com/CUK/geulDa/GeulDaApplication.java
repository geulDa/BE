package com.CUK.geulDa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class GeulDaApplication {

	public static void main(String[] args) {
        SpringApplication.run(GeulDaApplication.class, args);
	}

}
