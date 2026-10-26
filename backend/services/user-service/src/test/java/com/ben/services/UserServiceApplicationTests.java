package com.ben.services;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:user-service;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		// Migrations are MySQL SQL; this H2 smoke test lets Hibernate build the schema instead
		"spring.flyway.enabled=false"
})
class UserServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
