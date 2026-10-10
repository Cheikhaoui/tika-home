package com.tikahome;

import org.springframework.boot.SpringApplication;

public class TestTikahomeBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(TikahomeBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
