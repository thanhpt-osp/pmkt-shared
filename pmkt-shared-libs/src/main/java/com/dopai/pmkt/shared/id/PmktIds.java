package com.dopai.pmkt.shared.id;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import java.util.UUID;

/**
 * UUIDv7 generator (ADR-E — app-side, không phụ thuộc PostgreSQL version).
 *
 * <p>UUIDv7 = epoch-milli time-ordered, monotonic, B-tree friendly. Dùng cho mọi primary key trong
 * PMKT để tránh index fragmentation của UUIDv4.
 *
 * <p>Thread-safe: JUG {@link TimeBasedEpochGenerator} internal synchronized.
 */
public final class PmktIds {

  private static final TimeBasedEpochGenerator GEN = Generators.timeBasedEpochGenerator();

  private PmktIds() {
    throw new UnsupportedOperationException("Utility class");
  }

  /** Sinh UUIDv7 mới. */
  public static UUID newUuidV7() {
    return GEN.generate();
  }
}
