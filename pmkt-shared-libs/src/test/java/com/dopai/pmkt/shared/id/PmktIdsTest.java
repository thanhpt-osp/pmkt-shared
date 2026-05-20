package com.dopai.pmkt.shared.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PmktIdsTest {

  @Test
  void newUuidV7_should_have_version_7() {
    UUID id = PmktIds.newUuidV7();
    assertThat(id.version()).isEqualTo(7);
  }

  @Test
  void newUuidV7_should_be_monotonic_across_millis() throws InterruptedException {
    UUID id1 = PmktIds.newUuidV7();
    Thread.sleep(2);
    UUID id2 = PmktIds.newUuidV7();
    // UUIDv7 first 48 bits = epoch millis → id2 > id1 lexicographically
    assertThat(id1.compareTo(id2)).isLessThan(0);
  }

  @Test
  void newUuidV7_should_not_return_same_id_twice() {
    UUID id1 = PmktIds.newUuidV7();
    UUID id2 = PmktIds.newUuidV7();
    assertThat(id1).isNotEqualTo(id2);
  }
}
