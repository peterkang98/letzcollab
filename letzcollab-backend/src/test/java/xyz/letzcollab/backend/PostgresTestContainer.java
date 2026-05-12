package xyz.letzcollab.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ SpringBootTest
@ActiveProfiles("test")
public abstract class PostgresTestContainer {
	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
			.withDatabaseName("testdb")
			.withUsername("test")
			.withPassword("test");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
		registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
	}
}
