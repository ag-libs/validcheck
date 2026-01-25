package io.github.aglibs.validcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class FastValidationExceptionTest {

  @Test
  void fastValidationExceptionShouldNotFillStackTrace() {
    // Given
    FastValidationException exception =
        new FastValidationException(
            "Error", List.of(new ValidationError("field", "invalid")), true);

    // Then
    assertThat(exception.getStackTrace()).isEmpty();
  }

  @Test
  void validatorWithFillStackTraceFalseShouldThrowFastValidationException() {
    // Given - safeForClient=false means include values
    Validator validator = new Validator(false, true, false, null);

    // Then
    assertThatThrownBy(() -> validator.notNull(null, "value"))
        .isInstanceOf(FastValidationException.class)
        .satisfies(e -> assertThat(e.getStackTrace()).isEmpty());
  }

  @Test
  void fastValidationExceptionShouldInheritIncludeValueFlag() {
    // Given - safeForClient=true means values are excluded (safe)
    FastValidationException exceptionSafe =
        new FastValidationException(
            "Error", List.of(new ValidationError("field", "invalid")), true);
    // Given - safeForClient=false means values are included (unsafe)
    FastValidationException exceptionUnsafe =
        new FastValidationException(
            "Error", List.of(new ValidationError("field", "invalid")), false);

    // Then
    assertThat(exceptionSafe.isSafeForClient()).isTrue();
    assertThat(exceptionUnsafe.isSafeForClient()).isFalse();
  }

  @Test
  void constructorWithFieldNameMessageAndSafeForClient() {
    // Given
    String fieldName = "email";
    String message = "invalid format";

    // When
    FastValidationException exception = new FastValidationException(fieldName, message, true);

    // Then
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.isSafeForClient()).isTrue();
    assertThat(exception.getStackTrace()).isEmpty();
    assertThat(exception.getErrors()).hasSize(1);
    assertThat(exception.getErrors().get(0))
        .extracting(ValidationError::field, ValidationError::message)
        .containsExactly(fieldName, message);
  }

  @Test
  void constructorWithMessageAndSafeForClient() {
    // Given
    String message = "validation failed";

    // When
    FastValidationException exception = new FastValidationException(message, false);

    // Then
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.isSafeForClient()).isFalse();
    assertThat(exception.getStackTrace()).isEmpty();
    assertThat(exception.getErrors()).hasSize(1);
    assertThat(exception.getErrors().get(0))
        .extracting(ValidationError::field, ValidationError::message)
        .containsExactly(null, message);
  }
}
