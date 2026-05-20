package com.dopai.pmkt.shared.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * Contract test cho testkit support class — chỉ kiểm tra class shape (final + utility constructor +
 * static API). KHÔNG start container ở unit test (chuyển sang IT khi có failsafe — Batch 3+).
 */
class TestkitContractTest {

  @Test
  void postgresSupport_should_be_final_utility() {
    assertUtilityClass(PostgresContainerSupport.class);
  }

  @Test
  void kafkaSupport_should_be_final_utility() {
    assertUtilityClass(KafkaContainerSupport.class);
  }

  private static void assertUtilityClass(Class<?> clazz) {
    assertThat(Modifier.isFinal(clazz.getModifiers()))
        .as("%s phải final", clazz.getSimpleName())
        .isTrue();
    Constructor<?>[] ctors = clazz.getDeclaredConstructors();
    assertThat(ctors).hasSize(1);
    Constructor<?> ctor = ctors[0];
    assertThat(Modifier.isPrivate(ctor.getModifiers())).isTrue();
    ctor.setAccessible(true);
    assertThatThrownBy(ctor::newInstance)
        .isInstanceOf(InvocationTargetException.class)
        .hasRootCauseInstanceOf(UnsupportedOperationException.class);
  }
}
