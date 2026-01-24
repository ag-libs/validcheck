package io.github.aglibs.validcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SafeValidCheckTest {

  @Test
  void requireShouldNotIncludeValuesInErrorMessages() {
    // Given
    String sensitiveData = "secret-password-123";

    // When & Then - Error message should NOT contain the sensitive value
    assertThatThrownBy(() -> validator().hasLength(sensitiveData, 50, 100, "password"))
        .isInstanceOf(FastValidationException.class)
        .hasMessageContaining("'password' must have length between 50 and 100")
        .hasMessageNotContaining(sensitiveData); // Value NOT included
  }

  private static Validator validator() {
    return SafeValidCheck.require();
  }

  @Test
  void requireShouldThrowFastValidationException() {
    // Given - SafeValidCheck uses fast exceptions

    // When & Then
    assertThatThrownBy(() -> SafeValidCheck.require().notNull(null, "field"))
        .isInstanceOf(FastValidationException.class)
        .satisfies(e -> assertThat(e.getStackTrace()).isEmpty());
  }

  @Test
  void checkShouldCollectErrorsWithoutValues() {
    // Given
    BatchValidator validator = SafeValidCheck.check();
    String sensitiveValue = "sensitive-data";

    // When
    validator.hasLength(sensitiveValue, 50, 100, "data");

    // Then - Should fail without exposing the value
    assertThatThrownBy(validator::validate)
        .isInstanceOf(FastValidationException.class)
        .hasMessageContaining("'data' must have length between 50 and 100")
        .hasMessageNotContaining(sensitiveValue);
  }

  @Test
  void requireNotNullShouldWorkAsConvenience() {
    // When & Then
    assertThatThrownBy(() -> SafeValidCheck.requireNotNull(null, "param"))
        .isInstanceOf(FastValidationException.class)
        .hasMessageContaining("'param' must not be null");
  }

  @Test
  void basicMethods() {
    SafeValidCheck.requireNotNull("");
    SafeValidCheck.requireNotNull("", "field");
    SafeValidCheck.assertTrue(true, "error2");

    assertThatThrownBy(() -> SafeValidCheck.requireNotNull(null))
        .isInstanceOf(ValidationException.class)
        .hasMessage("parameter must not be null");

    assertThatThrownBy(() -> SafeValidCheck.requireNotNull(null, "field"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("'field' must not be null");

    assertThatThrownBy(() -> SafeValidCheck.assertTrue(false, "error2"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("error2");
  }
}
