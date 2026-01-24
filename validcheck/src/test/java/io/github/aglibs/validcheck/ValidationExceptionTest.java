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
}
