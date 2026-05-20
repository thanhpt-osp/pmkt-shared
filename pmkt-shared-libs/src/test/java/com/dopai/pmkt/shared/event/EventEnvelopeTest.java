package com.dopai.pmkt.shared.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

  @Test
  void newEnvelope_should_generate_uuidv7_eventId() {
    UUID tenantId = UUID.randomUUID();
    UUID aggregateId = UUID.randomUUID();

    EventEnvelope<String> env =
        EventEnvelope.newEnvelope(
            "com.dopai.pmkt.test.SampleEvent", 1, tenantId, aggregateId, 1, "payload");

    assertThat(env.eventId().version()).isEqualTo(7);
    assertThat(env.eventType()).isEqualTo("com.dopai.pmkt.test.SampleEvent");
    assertThat(env.eventVersion()).isEqualTo(1);
    assertThat(env.tenantId()).isEqualTo(tenantId);
    assertThat(env.aggregateId()).isEqualTo(aggregateId);
    assertThat(env.schemaVersion()).isEqualTo(1);
    assertThat(env.payload()).isEqualTo("payload");
    assertThat(env.occurredAt()).isBeforeOrEqualTo(Instant.now());
  }

  @Test
  void envelope_with_generic_payload_should_preserve_type() {
    record SamplePayload(String name, int value) {}

    EventEnvelope<SamplePayload> env =
        EventEnvelope.newEnvelope(
            "com.dopai.pmkt.test.TypedEvent",
            2,
            UUID.randomUUID(),
            UUID.randomUUID(),
            1,
            new SamplePayload("test", 42));

    assertThat(env.payload().name()).isEqualTo("test");
    assertThat(env.payload().value()).isEqualTo(42);
  }
}
