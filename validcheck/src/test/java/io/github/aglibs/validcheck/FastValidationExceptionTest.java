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
        new FastValidationException("Error", List.of(new ValidationError("field", "invalid")));

    // Then
    assertThat(exception.getStackTrace()).isEmpty();
  }

  @Test
  void validatorWithFillStackTraceFalseShouldThrowFastValidationException() {
    // Given
    Validator validator = new Validator(true, true, false, null);

    // Then
    assertThatThrownBy(() -> validator.notNull(null, "value"))
        .isInstanceOf(FastValidationException.class)
        .satisfies(e -> assertThat(e.getStackTrace()).isEmpty());
  }
}
