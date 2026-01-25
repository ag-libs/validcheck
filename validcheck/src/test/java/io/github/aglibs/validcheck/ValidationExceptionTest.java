package io.github.aglibs.validcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ValidationExceptionTest {

  @SuppressWarnings("DataFlowIssue")
  @Test
  void constructorShouldStoreErrorsImmutably() {
    // Given - Exception parameters
    String message = "Error 1; Error 2";
    List<ValidationError> errors =
        List.of(new ValidationError(null, "Error 1"), new ValidationError(null, "Error 2"));

    // When - Create ValidationException
    ValidationException exception = new ValidationException(message, errors, true);

    // Then - Test getErrors() returns immutable list
    List<ValidationError> returnedErrors = exception.getErrors();
    assertThat(returnedErrors)
        .containsExactly(
            new ValidationError(null, "Error 1"), new ValidationError(null, "Error 2"));

    // Then - Verify list is immutable
    assertThatThrownBy(() -> returnedErrors.add(new ValidationError(null, "Should fail")))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldStoreAndReturnIncludeValueFlag() {
    // Given - Exception with safeForClient set to true (excludes values)
    ValidationException exceptionSafe =
        new ValidationException("Error", List.of(new ValidationError(null, "Error")), true);

    // Then - isSafeForClient() should return true
    assertThat(exceptionSafe.isSafeForClient()).isTrue();

    // Given - Exception with safeForClient set to false (includes values)
    ValidationException exceptionUnsafe =
        new ValidationException("Error", List.of(new ValidationError(null, "Error")), false);

    // Then - isSafeForClient() should return false
    assertThat(exceptionUnsafe.isSafeForClient()).isFalse();
  }

  @Test
  void validationErrorAccessors() {
    // Given - Create ValidationError with field and message
    ValidationError error1 = new ValidationError("username", "must not be null");
    ValidationError error2 = new ValidationError(null, "general error");
    ValidationError error3 = new ValidationError("username", "must not be null"); // For hashCode

    // When & Then - Test error1 accessors and hashCode
    assertThat(error1)
        .extracting(ValidationError::field, ValidationError::message)
        .containsExactly("username", "must not be null");
    assertThat(error1).hasSameHashCodeAs(error3).doesNotHaveSameHashCodeAs(error2);

    // When & Then - Test error2 field accessor
    assertThat(error2)
        .extracting(ValidationError::field, ValidationError::message)
        .containsExactly(null, "general error");
  }

  @Test
  void constructorWithFieldNameMessageAndSafeForClient() {
    // Given
    String fieldName = "username";
    String message = "must not be empty";

    // When
    ValidationException exception = new ValidationException(fieldName, message, true, null);

    // Then
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.isSafeForClient()).isTrue();
    assertThat(exception.getErrors()).hasSize(1);
    assertThat(exception.getErrors().get(0))
        .extracting(ValidationError::field, ValidationError::message)
        .containsExactly(fieldName, message);
    assertThat(exception.getCause()).isNull();
  }

  @Test
  void constructorWithMessageAndSafeForClient() {
    // Given
    String message = "validation failed";

    // When
    ValidationException exception = new ValidationException(message, true);

    // Then
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.isSafeForClient()).isTrue();
    assertThat(exception.getErrors()).hasSize(1);
    assertThat(exception.getErrors().get(0))
        .extracting(ValidationError::field, ValidationError::message)
        .containsExactly(null, message);
  }

  @Test
  void constructorWithMessageSafeForClientAndCause() {
    // Given
    String message = "validation failed";
    Exception cause = new IllegalArgumentException("root cause");

    // When
    ValidationException exception = new ValidationException(message, false, cause);

    // Then
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.isSafeForClient()).isFalse();
    assertThat(exception.getCause()).isEqualTo(cause);
    assertThat(exception.getErrors()).hasSize(1);
    assertThat(exception.getErrors().get(0))
        .extracting(ValidationError::field, ValidationError::message)
        .containsExactly(null, message);
  }
}
