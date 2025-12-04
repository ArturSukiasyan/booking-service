package am.asukiasyan.booking;

public final class TestDataHelper {

    public static final String DB_DOWN_MESSAGE = "db down";
    public static final String POSTGRES_IMAGE = "postgres:16-alpine";
    public static final String REDIS_IMAGE = "redis:7.2-alpine";
    public static final int REDIS_PORT = 6379;
    public static final String POSTGRES_DB = "booking_it_";
    public static final String POSTGRES_USER = "postgres";
    public static final String POSTGRES_PASSWORD = "postgres";
    public static final String PROPERTY_DATASOURCE_URL = "spring.datasource.url";
    public static final String PROPERTY_DATASOURCE_USERNAME = "spring.datasource.username";
    public static final String PROPERTY_DATASOURCE_PASSWORD = "spring.datasource.password";
    public static final String PROPERTY_LIQUIBASE_URL = "spring.liquibase.url";
    public static final String PROPERTY_LIQUIBASE_USER = "spring.liquibase.user";
    public static final String PROPERTY_LIQUIBASE_PASSWORD = "spring.liquibase.password";
    public static final String PROPERTY_REDIS_HOST = "spring.data.redis.host";
    public static final String PROPERTY_REDIS_PORT = "spring.data.redis.port";
    public static final String PROPERTY_LIQUIBASE_DROP_FIRST = "spring.liquibase.drop-first";
    public static final String PROPERTY_JPA_DDL_AUTO = "spring.jpa.hibernate.ddl-auto";
    public static final String SERVLET_PATH = "/api/v1";
    public static final String AVAILABILITY_PATH = SERVLET_PATH + "/stats/availability";
    public static final String BOOKING_PATH = SERVLET_PATH + "/bookings";
    public static final String BOOKING_CANCEL_PATH = BOOKING_PATH + "/1/cancel";
    public static final String BOOKING_PAY_PATH = BOOKING_PATH + "/1/pay";
    public static final String UNIT_PATH = SERVLET_PATH + "/units";

    private TestDataHelper() {
    }
}
