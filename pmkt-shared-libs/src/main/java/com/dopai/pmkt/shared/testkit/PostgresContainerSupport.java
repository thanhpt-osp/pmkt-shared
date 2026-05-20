package com.dopai.pmkt.shared.testkit;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton PostgreSQL container cho integration test xuyên service (T1.8).
 *
 * <p>Lock image {@code postgres:16-alpine} theo PMKT baseline (BDR §3). Reuse 1 container giữa các
 * test class trong cùng JVM — {@link #start()} idempotent.
 *
 * <p>Usage pattern (đặt trong test base class hoặc {@code @TestConfiguration}):
 *
 * <pre>{@code
 * static {
 *   PostgresContainerSupport.start();
 *   System.setProperty("spring.datasource.url", PostgresContainerSupport.jdbcUrl());
 *   System.setProperty("spring.datasource.username", PostgresContainerSupport.username());
 *   System.setProperty("spring.datasource.password", PostgresContainerSupport.password());
 * }
 * }</pre>
 *
 * <p>Service phải include {@code org.testcontainers:postgresql} test scope vì class này declare
 * {@code <optional>true</optional>} ở {@code pmkt-shared-libs}.
 */
public final class PostgresContainerSupport {

  private static final DockerImageName IMAGE = DockerImageName.parse("postgres:16-alpine");

  private static final PostgreSQLContainer<?> CONTAINER =
      new PostgreSQLContainer<>(IMAGE)
          .withDatabaseName("pmkt_test")
          .withUsername("pmkt_test")
          .withPassword("pmkt_test")
          .withReuse(true);

  private PostgresContainerSupport() {
    throw new UnsupportedOperationException("Utility class");
  }

  /** Khởi động container nếu chưa chạy. Idempotent. */
  public static synchronized void start() {
    if (!CONTAINER.isRunning()) {
      CONTAINER.start();
    }
  }

  public static String jdbcUrl() {
    return CONTAINER.getJdbcUrl();
  }

  public static String username() {
    return CONTAINER.getUsername();
  }

  public static String password() {
    return CONTAINER.getPassword();
  }

  public static int port() {
    return CONTAINER.getFirstMappedPort();
  }
}
