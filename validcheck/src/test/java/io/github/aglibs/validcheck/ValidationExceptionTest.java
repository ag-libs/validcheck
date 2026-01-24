package io.github.aglibs.validcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ValidationExceptionTest {

  @SuppressWarnings("DataFlowIssue")
  @Test
  void constructorAndGetErrorsMethodWithImmutableList() {
    // Given - Exception parameters
    String message = "Error 1; Error 2";
    List<ValidationError> errors =
        List.of(new ValidationError(null, "Error 1"), new ValidationError(null, "Error 2"));

    // When - Create ValidationException
    ValidationException exception = new ValidationException(message, errors);

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
  void fillInStackTraceMethodWithDifferentConfigurations() {
    // Given - Test both fillStackTrace configurations
    List<ValidationError> errors = List.of(new ValidationError(null, "Test error"));

    // When - Create exception with fillStackTrace = true
    ValidationException withStackTrace = ValidationException.create(true, "Message", errors);

    // Then - Should have stack trace elements
    assertThat(withStackTrace.getStackTrace()).isNotEmpty();

    // When - Create exception with fillStackTrace = false
    ValidationException withoutStackTrace = ValidationException.create(false, "Message", errors);

    // Then - Should have empty stack trace
    assertThat(withoutStackTrace.getStackTrace()).isEmpty();
  }

  @Test
  void validationErrorAccessors() {
    // Given - Create ValidationError with field and message
    ValidationError error1 = new ValidationError("username", "must not be null");
    ValidationError error2 = new ValidationError(null, "general error");
    ValidationError error3 = new ValidationError("username", "must not be null"); // For hashCode

    // When & Then - Test field accessor
    assertThat(error1.field()).isEqualTo("username");
    assertThat(error2.field()).isNull();

    // When & Then - Test message accessor
    assertThat(error1.message()).isEqualTo("must not be null");
    assertThat(error2.message()).isEqualTo("general error");

    // When & Then - Test hashCode
    assertThat(error1.hashCode()).isEqualTo(error3.hashCode());
    assertThat(error1.hashCode()).isNotEqualTo(error2.hashCode());
  }
}
