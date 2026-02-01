package io.github.aglibs.validcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class FastValidationExceptionTest {

  @Test
  void constructorsAndStackTraceBehavior() {
    // Given
    List<ValidationError> errors =
        List.of(
            new ValidationError("field1", "must not be null"),
            new ValidationError("field2", "must be positive"));

    // When - Constructor with custom message
    var exception1 = new FastValidationException("Custom message", errors);

    // Then
    assertThat(exception1.getMessage()).isEqualTo("Custom message");
    assertThat(exception1.getErrors()).isEqualTo(errors);
    assertThat(exception1.getStackTrace()).isEmpty();

    // When - Constructor with auto-generated message
    var exception2 = new FastValidationException(errors);

    // Then
    assertThat(exception2.getMessage())
        .isEqualTo("'field1' must not be null; 'field2' must be positive");
    assertThat(exception2.getErrors()).isEqualTo(errors);
    assertThat(exception2.getStackTrace()).isEmpty();

    // When - fillInStackTrace called
    Throwable result = exception2.fillInStackTrace();

    // Then - Should return this without filling
    assertThat(result).isSameAs(exception2);
    assertThat(exception2.getStackTrace()).isEmpty();
  }

  @Test
  void integrationWithRequireWithAndCheckWith() {
    // When & Then - requireWith throws FastValidationException without stack trace
    assertThatThrownBy(
            () -> ValidCheck.requireWith(FastValidationException::new).notNull(null, "field"))
        .isInstanceOf(FastValidationException.class)
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'field' must not be null")
        .satisfies(e -> assertThat(e.getStackTrace()).isEmpty());

    // When & Then - checkWith throws FastValidationException with multiple errors
    var validator = ValidCheck.checkWith(FastValidationException::new);
    validator.notNull(null, "field1").notNull(null, "field2");

    assertThatThrownBy(validator::validate)
        .isInstanceOf(FastValidationException.class)
        .hasMessageContaining("'field1' must not be null")
        .hasMessageContaining("'field2' must not be null")
        .satisfies(
            e -> {
              assertThat(e.getStackTrace()).isEmpty();
              FastValidationException fve = (FastValidationException) e;
              assertThat(fve.getErrors()).hasSize(2);
            });
  }
}
