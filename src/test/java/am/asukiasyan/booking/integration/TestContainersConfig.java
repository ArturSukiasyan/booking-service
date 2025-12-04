package am.asukiasyan.booking.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import static am.asukiasyan.booking.TestDataHelper.POSTGRES_DB;
import static am.asukiasyan.booking.TestDataHelper.POSTGRES_IMAGE;
import static am.asukiasyan.booking.TestDataHelper.POSTGRES_PASSWORD;
import static am.asukiasyan.booking.TestDataHelper.POSTGRES_USER;
import static am.asukiasyan.booking.TestDataHelper.PROPERTY_DATASOURCE_PASSWORD;
import static am.asukiasyan.booking.TestDataHelper.PROPERTY_DATASOURCE_URL;
import static am.asukiasyan.booking.TestDataHelper.PROPERTY_DATASOURCE_USERNAME;
import static am.asukiasyan.booking.TestDataHelper.PROPERTY_JPA_DDL_AUTO;
import static am.asukiasyan.booking.TestDataHelper.PROPERTY_LIQUIBASE_DROP_FIRST;
import static am.asukiasyan.booking.TestDataHelper.PROPERTY_LIQUIBASE_PASSWORD;
import static am.asukiasyan.booking.TestDataHelper.PROPERTY_LIQUIBASE_URL;
import static am.asukiasyan.booking.TestDataHelper.PROPERTY_LIQUIBASE_USER;
import static am.asukiasyan.booking.TestDataHelper.PROPERTY_REDIS_HOST;
import static am.asukiasyan.booking.TestDataHelper.PROPERTY_REDIS_PORT;
import static am.asukiasyan.booking.TestDataHelper.REDIS_IMAGE;
import static am.asukiasyan.booking.TestDataHelper.REDIS_PORT;

public abstract class TestContainersConfig {

    @Container
    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
            .withDatabaseName(POSTGRES_DB + System.nanoTime())
            .withUsername(POSTGRES_USER)
            .withPassword(POSTGRES_PASSWORD);

    @Container
    @SuppressWarnings("resource")
    protected static final GenericContainer<?> REDIS = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(REDIS_PORT);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
        registry.add(PROPERTY_DATASOURCE_URL, POSTGRES::getJdbcUrl);
        registry.add(PROPERTY_DATASOURCE_USERNAME, POSTGRES::getUsername);
        registry.add(PROPERTY_DATASOURCE_PASSWORD, POSTGRES::getPassword);
        registry.add(PROPERTY_REDIS_HOST, REDIS::getHost);
        registry.add(PROPERTY_REDIS_PORT, () -> REDIS.getMappedPort(REDIS_PORT));
        registry.add(PROPERTY_LIQUIBASE_DROP_FIRST, () -> true);
        registry.add(PROPERTY_JPA_DDL_AUTO, () -> "none");
        registry.add(PROPERTY_LIQUIBASE_URL, POSTGRES::getJdbcUrl);
        registry.add(PROPERTY_LIQUIBASE_USER, POSTGRES::getUsername);
        registry.add(PROPERTY_LIQUIBASE_PASSWORD, POSTGRES::getPassword);
    }
}
