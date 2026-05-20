package com.dopai.pmkt.shared.testkit;

import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton Kafka container cho integration test event-driven (T1.8).
 *
 * <p>Lock image {@code confluentinc/cp-kafka:7.6.1} (Apache Kafka 3.6 wire-compatible) theo PMKT
 * baseline (BDR §3). Reuse 1 container giữa các test class — {@link #start()} idempotent.
 *
 * <p>Usage pattern:
 *
 * <pre>{@code
 * static {
 *   KafkaContainerSupport.start();
 *   System.setProperty("spring.kafka.bootstrap-servers", KafkaContainerSupport.bootstrapServers());
 * }
 * }</pre>
 *
 * <p>Service phải include {@code org.testcontainers:kafka} test scope vì class này declare
 * {@code <optional>true</optional>}.
 */
public final class KafkaContainerSupport {

  private static final DockerImageName IMAGE =
      DockerImageName.parse("confluentinc/cp-kafka:7.6.1");

  private static final ConfluentKafkaContainer CONTAINER =
      new ConfluentKafkaContainer(IMAGE).withReuse(true);

  private KafkaContainerSupport() {
    throw new UnsupportedOperationException("Utility class");
  }

  /** Khởi động container nếu chưa chạy. Idempotent. */
  public static synchronized void start() {
    if (!CONTAINER.isRunning()) {
      CONTAINER.start();
    }
  }

  public static String bootstrapServers() {
    return CONTAINER.getBootstrapServers();
  }
}
