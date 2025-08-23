package io.github.validcheck;

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
}
