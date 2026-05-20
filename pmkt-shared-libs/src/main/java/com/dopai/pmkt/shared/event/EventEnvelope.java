package com.dopai.pmkt.shared.event;

import com.dopai.pmkt.shared.id.PmktIds;
import java.time.Instant;
import java.util.UUID;

/**
 * Event envelope chuẩn cho PMKT MVP1 (BDR §4.4 guardrail 3 — Codex CONSENSUS 2026-05-20).
 *
 * <p>Mọi domain event publish ra Kafka phải wrap trong envelope này.
 *
 * <p>Backward-compat rule: chỉ thêm field (additive), không đổi field hiện có. Consumer pin minor
 * version, không pin major. Deprecation window ≥ 1 release.
 *
 * @param <T> Payload type — service-specific.
 */
public record EventEnvelope<T>(
    UUID eventId,
    String eventType,
    int eventVersion,
    Instant occurredAt,
    UUID tenantId,
    UUID aggregateId,
    int schemaVersion,
    T payload) {

  /**
   * Tạo envelope mới với defaults: {@code eventId} = UUIDv7 mới, {@code occurredAt} = now.
   *
   * @param eventType FQN của event (e.g. {@code com.dopai.pmkt.core.chungtu.ChungTuPosted}).
   * @param eventVersion Semantic version của event (1, 2, 3...) — additive evolution.
   * @param tenantId Tenant ID = ĐVKT (ADR-D, 1:1 mapping).
   * @param aggregateId Root entity event xảy ra trên.
   * @param schemaVersion Schema version của payload (independent of eventVersion).
   * @param payload Domain payload.
   */
  public static <T> EventEnvelope<T> newEnvelope(
      String eventType,
      int eventVersion,
      UUID tenantId,
      UUID aggregateId,
      int schemaVersion,
      T payload) {
    return new EventEnvelope<>(
        PmktIds.newUuidV7(),
        eventType,
        eventVersion,
        Instant.now(),
        tenantId,
        aggregateId,
        schemaVersion,
        payload);
  }
}
