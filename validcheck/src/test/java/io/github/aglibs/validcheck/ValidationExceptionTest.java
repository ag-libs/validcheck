package io.github.aglibs.validcheck;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ValidationExceptionTest {

  @Test
  void constructorWithMessageAndCause() {
    // Given
    var cause = new RuntimeException("Original error");

    // When
    var exception = new ValidationException("Validation failed", cause);

    // Then
    assertThat(exception.getMessage()).isEqualTo("Validation failed");
    assertThat(exception.getCause()).isSameAs(cause);
    assertThat(exception.getErrors()).hasSize(1);
    assertThat(exception.getErrors().get(0).field()).isNull();
    assertThat(exception.getErrors().get(0).message()).isEqualTo("Validation failed");
  }

  @Test
  void constructorWithMessage() {
    // When
    var exception = new ValidationException("Validation failed");

    // Then
    assertThat(exception.getMessage()).isEqualTo("Validation failed");
    assertThat(exception.getCause()).isNull();
    assertThat(exception.getErrors()).hasSize(1);
    assertThat(exception.getErrors().get(0).field()).isNull();
    assertThat(exception.getErrors().get(0).message()).isEqualTo("Validation failed");
  }

  @Test
  void constructorWithNameMessageAndCause() {
    // Given
    var cause = new IllegalArgumentException("Invalid value");

    // When
    var exception = new ValidationException("userId", "user not found", cause);

    // Then
    assertThat(exception.getMessage()).isEqualTo("user not found");
    assertThat(exception.getCause()).isSameAs(cause);
    assertThat(exception.getErrors()).hasSize(1);
    assertThat(exception.getErrors().get(0).field()).isEqualTo("userId");
    assertThat(exception.getErrors().get(0).message()).isEqualTo("user not found");
  }

  @Test
  void constructorWithMessageAndErrors() {
    // Given
    List<ValidationError> errors =
        List.of(
            new ValidationError("field1", "must not be null"),
            new ValidationError("field2", "must be positive"));

    // When
    var exception = new ValidationException("Custom message", errors);

    // Then
    assertThat(exception.getMessage()).isEqualTo("Custom message");
    assertThat(exception.getErrors()).isEqualTo(errors);
    assertThat(exception.getErrors()).hasSize(2);
  }

  @Test
  void constructorWithErrors() {
    // Given
    List<ValidationError> errors =
        List.of(
            new ValidationError("field1", "must not be null"),
            new ValidationError("field2", "must be positive"));

    // When
    var exception = new ValidationException(errors);

    // Then
    assertThat(exception.getMessage())
        .isEqualTo("'field1' must not be null; 'field2' must be positive");
    assertThat(exception.getErrors()).isEqualTo(errors);
    assertThat(exception.getErrors()).hasSize(2);
  }
}
