package com.pollnet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared test fixture: spins up postgres + redis ONCE for the whole JVM.
 *
 * We deliberately don't use {@code @Testcontainers}. That extension stops the
 * container at the end of each test class — and because our containers are
 * declared in this base, the second test class would inherit the static field
 * but find the container dead. The singleton pattern below keeps both alive
 * for the whole Surefire fork.
 */
// MOCK (the default) keeps MockMvc dispatch in the test's thread, which lets
// @Transactional roll back the seeded data after each test. RANDOM_PORT spins up
// a real servlet container on its own thread and the test transaction wouldn't
// propagate — leading to leaked seed data + UNIQUE constraint conflicts.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES;
    static final GenericContainer<?>    REDIS;

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("pollnet")
                .withUsername("pollnet")
                .withPassword("pollnet");
        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void wireProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host",     REDIS::getHost);
        registry.add("spring.data.redis.port",     REDIS::getFirstMappedPort);
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @BeforeEach
    void wipeRedis() {
        // Each test starts with a fresh Redis namespace so rate-limit counters
        // and refresh tokens from the previous test don't leak in.
        try {
            REDIS.execInContainer("redis-cli", "FLUSHALL");
        } catch (Exception ignored) {
            // Best effort.
        }
    }
}
