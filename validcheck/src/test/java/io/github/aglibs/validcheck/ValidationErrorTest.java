package io.github.aglibs.validcheck;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ValidationErrorTest {

  @Test
  void toStringFormatsWithField() {
    // Given
    var error = new ValidationError("username", "must not be null");

    // When
    String result = error.toString();

    // Then
    assertThat(result).isEqualTo("'username' must not be null");
  }

  @Test
  void toStringFormatsWithoutField() {
    // Given
    var error = new ValidationError(null, "must not be null");

    // When
    String result = error.toString();

    // Then
    assertThat(result).isEqualTo("must not be null");
  }

  @Test
  void joinCombinesMultipleErrors() {
    // Given
    List<ValidationError> errors =
        List.of(
            new ValidationError("username", "must not be null"),
            new ValidationError("age", "must be positive"),
            new ValidationError(null, "custom assertion failed"));

    // When
    String result = ValidationError.join(errors);

    // Then
    assertThat(result)
        .isEqualTo("'username' must not be null; 'age' must be positive; custom assertion failed");
  }

  @Test
  void joinHandlesEmptyList() {
    // Given
    List<ValidationError> errors = List.of();

    // When
    String result = ValidationError.join(errors);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void joinHandlesSingleError() {
    // Given
    List<ValidationError> errors = List.of(new ValidationError("field", "must not be empty"));

    // When
    String result = ValidationError.join(errors);

    // Then
    assertThat(result).isEqualTo("'field' must not be empty");
  }

  @Test
  void fieldAndMessageAccessors() {
    // Given
    var error = new ValidationError("email", "must match pattern");

    // When & Then
    assertThat(error.field()).isEqualTo("email");
    assertThat(error.message()).isEqualTo("must match pattern");
  }

  @Test
  void equalsAndHashCode() {
    // Given
    var error1 = new ValidationError("field", "message");
    var error2 = new ValidationError("field", "message");
    var error3 = new ValidationError("other", "message");

    // Then
    assertThat(error1).isEqualTo(error2);
    assertThat(error1).hasSameHashCodeAs(error2);
    assertThat(error1).isNotEqualTo(error3);
  }
}
