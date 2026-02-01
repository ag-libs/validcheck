package io.github.aglibs.validcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

@SuppressWarnings({"ConstantValue"})
class ValidCheckTest {

  @Test
  void factoryMethodsCreateCorrectValidatorTypes() {
    // Given & When - Test check() factory method
    var batchValidator = ValidCheck.check();

    // Then - Should create BatchValidator instance
    assertThat(batchValidator).isInstanceOf(BatchValidator.class);
    assertThat(batchValidator.isValid()).isTrue();
    assertThat(batchValidator.getErrors()).isEmpty();

    // Given & When - Test require() factory method
    var failFastValidator = ValidCheck.require();

    // Then - Should create Validator but not BatchValidator
    assertThat(failFastValidator).isInstanceOf(Validator.class);
    assertThat(failFastValidator).isNotInstanceOf(BatchValidator.class);
  }

  @Test
  void requireNotNullMethodsCoverAllOverloads() {
    // Given - Test data
    String validName = "John";
    String nullName = null;
    Object validObject = new Object();
    Object nullObject = null;

    // When & Then - Test requireNotNull with name parameter (valid)
    ValidCheck.requireNotNull(validName, "name");

    // When & Then - Test requireNotNull with name parameter (invalid)
    assertThatThrownBy(() -> ValidCheck.requireNotNull(nullName, "name"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'name' must not be null");

    // When & Then - Test requireNotNull without name parameter (valid)
    ValidCheck.requireNotNull(validObject);

    // When & Then - Test requireNotNull without name parameter (invalid)
    assertThatThrownBy(() -> ValidCheck.requireNotNull(nullObject))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must not be null");
  }

  @Test
  void assertTrueMethodValidatesConditionsCorrectly() {
    // Given - Test conditions and messages
    boolean validCondition = true;
    boolean invalidCondition = false;
    String customMessage = "Custom validation failed";

    // When & Then - Test assertTrue with valid condition
    ValidCheck.assertTrue(validCondition, "Should not throw");

    // When & Then - Test assertTrue with invalid condition
    assertThatThrownBy(() -> ValidCheck.assertTrue(invalidCondition, customMessage))
        .isInstanceOf(ValidationException.class)
        .hasMessage(customMessage);

    // Additional test to ensure method delegates to require() correctly
    assertThatThrownBy(() -> ValidCheck.assertTrue(false, "Fail fast test"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Fail fast test");
  }

  @Test
  void basicMethods() {
    ValidCheck.requireNotNull("", "error1");
    ValidCheck.assertTrue(true, "error2");

    assertThatThrownBy(() -> ValidCheck.requireNotNull(null, "name"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("'name' must not be null");

    assertThatThrownBy(() -> ValidCheck.assertTrue(false, "error2"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("error2");
  }

  @Test
  void requireAndCheckShouldIncludeStackTraces() {
    // Given - ValidCheck methods should always include stack traces

    // When & Then - require() includes stack trace
    assertThatThrownBy(() -> ValidCheck.require().notNull(null, "field"))
        .isInstanceOf(ValidationException.class)
        .satisfies(e -> assertThat(e.getStackTrace()).isNotEmpty());

    // When & Then - check() includes stack trace
    BatchValidator validator = ValidCheck.check();
    validator.notNull(null, "field");
    assertThatThrownBy(validator::validate)
        .isInstanceOf(ValidationException.class)
        .satisfies(e -> assertThat(e.getStackTrace()).isNotEmpty());
  }

  @Test
  void requireWithThrowsCustomException() {
    // Given - Custom exception factory using ValidationError.join()
    var customFactory =
        (java.util.function.Function<java.util.List<ValidationError>, RuntimeException>)
            errors -> new IllegalArgumentException("Custom: " + ValidationError.join(errors));

    // When & Then - requireWith() throws custom exception on first failure
    assertThatThrownBy(() -> ValidCheck.requireWith(customFactory).notNull(null, "field"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Custom: 'field' must not be null");
  }

  @Test
  void requireWithFailsFast() {
    // Given - Custom exception factory counting errors
    var errorCount = new java.util.concurrent.atomic.AtomicInteger(0);
    var customFactory =
        (java.util.function.Function<java.util.List<ValidationError>, RuntimeException>)
            errors -> {
              errorCount.set(errors.size());
              return new IllegalStateException("Errors: " + errors.size());
            };

    // When & Then - Should fail on first error (fail-fast)
    assertThatThrownBy(
            () ->
                ValidCheck.requireWith(customFactory)
                    .notNull(null, "field1")
                    .notNull(null, "field2"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Errors: 1");

    assertThat(errorCount.get()).isEqualTo(1);
  }

  @Test
  void checkWithThrowsCustomException() {
    // Given - Custom exception factory
    var customFactory =
        (java.util.function.Function<java.util.List<ValidationError>, RuntimeException>)
            errors ->
                new IllegalArgumentException(
                    "Found " + errors.size() + " error(s): " + errors.get(0).toString());

    // When
    var validator = ValidCheck.checkWith(customFactory);
    validator.notNull(null, "field");

    // Then - Should throw custom exception
    assertThatThrownBy(validator::validate)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Found 1 error(s)")
        .hasMessageContaining("'field' must not be null");
  }

  @Test
  void checkWithCollectsAllErrors() {
    // Given - Custom exception factory using ValidationError.join()
    var customFactory =
        (java.util.function.Function<java.util.List<ValidationError>, RuntimeException>)
            errors -> new IllegalStateException("Batch errors: " + ValidationError.join(errors));

    // When
    var validator = ValidCheck.checkWith(customFactory);
    validator.notNull(null, "field1").notNull(null, "field2").isPositive(-1, "age");

    // Then - Should collect all errors
    assertThatThrownBy(validator::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("field1")
        .hasMessageContaining("field2")
        .hasMessageContaining("age");
  }

  @Test
  void checkWithCreatesCorrectValidatorType() {
    // Given - Custom exception factory
    var customFactory =
        (java.util.function.Function<java.util.List<ValidationError>, RuntimeException>)
            errors -> new IllegalArgumentException("Custom error");

    // When
    var validator = ValidCheck.checkWith(customFactory);

    // Then - Should create BatchValidator instance
    assertThat(validator).isInstanceOf(BatchValidator.class);
  }

  @Test
  void requireWithCreatesCorrectValidatorType() {
    // Given - Custom exception factory
    var customFactory =
        (java.util.function.Function<java.util.List<ValidationError>, RuntimeException>)
            errors -> new IllegalArgumentException("Custom error");

    // When
    var validator = ValidCheck.requireWith(customFactory);

    // Then - Should create Validator but not BatchValidator
    assertThat(validator).isInstanceOf(Validator.class);
    assertThat(validator).isNotInstanceOf(BatchValidator.class);
  }
}
