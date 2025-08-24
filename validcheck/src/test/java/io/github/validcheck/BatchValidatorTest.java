package io.github.validcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"ConstantValue"})
class BatchValidatorTest {

  @Test
  void batchValidatorCollectsMultipleErrorsAndValidateMethod() {
    // Given - Multiple invalid values
    String nullName = null;
    String emptyEmail = "";
    Integer negativeAge = -5;

    // When - Build up multiple validations
    BatchValidator validator =
        ValidCheck.check()
            .notNull(nullName, "name")
            .notNullOrEmpty(emptyEmail, "email")
            .isPositive(negativeAge, "age");

    // Then - Test getErrors() method
    assertThat(validator.getErrors()).hasSize(3);
    assertThat(validator.getErrors())
        .containsExactlyInAnyOrder(
            "'name' must not be null",
            "'email' must not be null or empty",
            "'age' must be positive, but it was -5");

    // Then - Test isValid() method
    assertThat(validator.isValid()).isFalse();

    // When & Then - Test validate() method throws with all errors
    assertThatThrownBy(validator::validate)
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining(
            "'name' must not be null; 'email' must not be null or empty; 'age' must be positive, but it was -5");

    // Given - Valid data
    BatchValidator validValidator =
        ValidCheck.check().notNull("John", "name").isPositive(25, "age");

    // Then - Test with valid data
    assertThat(validValidator.isValid()).isTrue();
    assertThat(validValidator.getErrors()).isEmpty();
    validValidator.validate(); // Should not throw
  }

  @Test
  void includeMethodCombinesErrorsFromOtherBatchValidators() {
    // Given - Two separate batch validators with errors
    BatchValidator userValidator =
        ValidCheck.check().notNull(null, "username").hasLength("ab", 5, 20, "password");

    BatchValidator addressValidator =
        ValidCheck.check().notBlank("", "street").isPositive(-1, "zipCode");

    // When - Use include() to combine validators
    BatchValidator combinedValidator =
        ValidCheck.check()
            .notNullOrEmpty("", "email") // Add one more error
            .include(userValidator)
            .include(addressValidator);

    // Then - Should have combined all errors from all validators
    assertThat(combinedValidator.getErrors()).hasSize(5);
    assertThat(combinedValidator.getErrors())
        .containsExactlyInAnyOrder(
            "'email' must not be null or empty",
            "'username' must not be null",
            "'password' must have length between 5 and 20, but it was 'ab'",
            "'street' must not be blank",
            "'zipCode' must be positive, but it was -1");

    // When - Include validator with no errors
    BatchValidator emptyValidator = ValidCheck.check();
    combinedValidator.include(emptyValidator);

    // Then - Should not change error count
    assertThat(combinedValidator.getErrors()).hasSize(5);
  }

  @Test
  void whenMethodAppliesConditionalValidations() {
    // Given - Test data and conditions
    String username = "user";
    String password = "weak";
    boolean isAdminMode = true;
    boolean isGuestMode = false;
    boolean requireComplexPassword = true;

    // When - Use when() for conditional validations
    BatchValidator validator =
        ValidCheck.check()
            .notBlank(username, "username")
            .when(
                isAdminMode,
                v ->
                    v.hasLength(username, 5, 30, "username") // Should execute and fail
                        .matches(username, "^admin_.*", "username")) // Should execute and fail
            .when(isGuestMode, v -> v.hasLength(username, 1, 10, "username")) // Should NOT execute
            .notBlank(password, "password")
            .when(
                requireComplexPassword,
                v ->
                    v.hasLength(password, 8, 50, "password") // Should execute and fail
                        .matches(password, ".*[A-Z].*", "password") // Should execute and fail
                        .matches(password, ".*\\d.*", "password")); // Should execute and fail

    // Then - Should only include errors from conditions that were true
    List<String> errors = validator.getErrors();
    assertThat(errors).hasSize(5); // 1 base + 2 admin + 2 complex password

    // Verify admin mode validations were applied (isAdminMode=true)
    assertThat(errors)
        .anyMatch(error -> error.contains("'username' must have length between 5 and 30"));
    assertThat(errors)
        .anyMatch(error -> error.contains("'username' must match pattern '^admin_.*'"));

    // Verify guest mode validations were NOT applied (isGuestMode=false)
    assertThat(errors).noneMatch(error -> error.contains("must have length between 1 and 10"));

    // Verify complex password validations were applied (requireComplexPassword=true)
    assertThat(errors)
        .anyMatch(error -> error.contains("'password' must have length between 8 and 50"));
    assertThat(errors)
        .anyMatch(error -> error.contains("'password' must match pattern '.*[A-Z].*'"));
    assertThat(errors).anyMatch(error -> error.contains("'password' must match pattern '.*\\d.*'"));

    // When - Test with all conditions false
    BatchValidator noConditionsValidator =
        ValidCheck.check()
            .notNull("present", "value")
            .when(false, v -> v.notNull(null, "shouldNotExecute"))
            .when(false, v -> v.isNegative(1, "shouldNotExecute"));

    // Then - Should have no errors since conditional validations didn't execute
    assertThat(noConditionsValidator.isValid()).isTrue();
    assertThat(noConditionsValidator.getErrors()).isEmpty();
  }

  @Test
  void allInheritedValidationMethodsReturnBatchValidatorForFluentChaining() {
    // Given - Test all validation method categories return correct type
    String validString = "test";
    Integer validNumber = 5;
    List<String> validList = List.of("a", "b");

    // When & Then - Test fluent chaining returns BatchValidator instances
    BatchValidator result =
        ValidCheck.check()
            // Test assertion methods return BatchValidator
            .assertTrue(true, "condition")
            .assertFalse(false, "condition")

            // Test null methods return BatchValidator
            .notNull(validString, "string")

            // Test range methods return BatchValidator
            .inRange(validNumber, 1, 10, "number")

            // Test string methods return BatchValidator
            .notNullOrEmpty(validString, "string")
            .notBlank(validString, "string")
            .hasLength(validString, 1, 10, "string")

            // Test collection methods return BatchValidator
            .notNullOrEmpty(validList, "list")
            .hasSize(validList, 1, 5, "list")

            // Test numeric methods return BatchValidator
            .isPositive(validNumber, "number")

            // Test pattern methods return BatchValidator
            .matches(validString, "^[a-z]+$", "string");

    // Then - Final result should be BatchValidator
    assertThat(result).isInstanceOf(BatchValidator.class);
    assertThat(result.isValid()).isTrue();

    // When - Test with failing validations to ensure chaining works with errors too
    BatchValidator failingResult =
        ValidCheck.check()
            .assertTrue(false, "assertion failed")
            .notNull(null, "null value")
            .isNegative(validNumber, "should be negative");

    // Then - Should still return BatchValidator and collect all errors
    assertThat(failingResult).isInstanceOf(BatchValidator.class);
    assertThat(failingResult.getErrors()).hasSize(3);
  }

  @Test
  void parameterlessMethodsCoverMissingBatchValidatorOverloads() {
    // Given - Values for testing parameter-less overloads in batch mode
    String nullValue = null;
    List<String> nullList = null;
    Integer nullNumber = null;
    Integer outOfRange = 100;
    List<String> emptyList = List.of();
    Map<String, String> emptyMap = Map.of();
    String blankString = "   ";
    String longString = "a".repeat(200);
    List<String> smallList = List.of("a");
    Integer negativeNumber = -5;
    Integer positiveNumber = 5;
    Integer zero = 0;
    String invalidPattern = "ABC123";

    // When - Use BatchValidator parameter-less methods
    BatchValidator validator =
        ValidCheck.check()
            .notNull(nullValue)
            .inRange(outOfRange, 1, 50)
            .notNullOrEmpty(nullValue)
            .notNullOrEmpty(emptyList)
            .notNullOrEmpty(emptyMap)
            .notBlank(blankString)
            .hasLength(longString, 1, 100)
            .hasSize(smallList, 2, 5)
            .hasSize(emptyMap, 2, 5)
            .isPositive(negativeNumber)
            .isNegative(-negativeNumber)
            .matches(invalidPattern, "^[a-z]+$")
            .matches(invalidPattern, Pattern.compile("^[a-z]+$"))

            // Test new conditional (nullOr) methods - all with null values which should pass
            .nullOrNotEmpty(nullValue)
            .nullOrNotEmpty(nullList) // Collection version
            .nullOrNotEmpty((Map<String, String>) null) // Map version
            .nullOrNotBlank(nullValue)
            .nullOrHasLength(nullValue, 1, 10)
            .nullOrHasSize(nullList, 1, 10)
            .nullOrHasSize((Map<String, String>) null, 1, 10)
            .nullOrIsPositive(nullNumber)
            .nullOrIsNegative(nullNumber)
            .nullOrMatches(nullValue, "^[a-z]+$")
            .nullOrMatches(nullValue, Pattern.compile("^[a-z]+$"))
            .nullOrInRange(nullNumber, 1, 10)
            .nullOrIsNonNegative(nullNumber)
            .nullOrIsNonPositive(nullNumber)
            .nullOrMin(nullNumber, 1)
            .nullOrMax(nullNumber, 10)

            // Test sign validation methods with failing values
            .isNonNegative(negativeNumber) // Should fail: -5 is not >= 0
            .isNonPositive(positiveNumber) // Should fail: 5 is not <= 0

            // Test single-bound methods with failing values
            .min(negativeNumber, 0) // Should fail: -5 is not >= 0
            .max(outOfRange, 50); // Should fail: 100 is not <= 50

    // Then - Should collect all errors from parameter-less methods
    assertThat(validator.getErrors())
        .hasSize(17)
        .containsOnly(
            "parameter must not be null",
            "parameter must be between 1 and 50, but it was 100",
            "parameter must not be null or empty",
            "parameter must not be null or empty",
            "parameter must not be null or empty",
            "parameter must not be blank",
            "parameter must have length between 1 and 100, but it was 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa...'",
            "parameter must have size between 2 and 5, but it was [a]",
            "parameter must have size between 2 and 5, but it was {}",
            "parameter must be positive, but it was -5",
            "parameter must be negative, but it was 5",
            "parameter must match pattern '^[a-z]+$', but it was 'ABC123'",
            "parameter must match pattern '^[a-z]+$', but it was 'ABC123'",
            "parameter must be non-negative, but it was -5",
            "parameter must be non-positive, but it was 5",
            "parameter must be at least 0, but it was -5",
            "parameter must be at most 50, but it was 100");
    assertThat(validator.isValid()).isFalse();

    // When & Then - Validate should throw with all collected errors
    assertThatThrownBy(validator::validate).isInstanceOf(ValidationException.class);
  }

  @Test
  void nullOrNotEmptyMethodsCoverAllOverloads() {
    // Given - Test data for different overloads
    List<String> nullList = null;
    Map<String, String> nullMap = null;

    // When - Use all nullOrNotEmpty overloads with named parameters
    BatchValidator validator =
        ValidCheck.check()
            .nullOrNotEmpty(nullList, "testList")
            .nullOrNotEmpty(nullList, () -> "custom list message")
            .nullOrNotEmpty(nullMap, "testMap")
            .nullOrNotEmpty(nullMap, () -> "custom map message");

    // Then - All should pass (null values are allowed)
    assertThat(validator.isValid()).isTrue();
    assertThat(validator.getErrors()).isEmpty();
  }
}
