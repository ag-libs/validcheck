package io.github.validcheck;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"ConstantValue"})
class ValidatorTest {

  @Test
  void assertionMethodsWithAllOverloads() {
    // Given - Test data for all assertion methods
    boolean validCondition = true;
    boolean invalidCondition = false;
    String message = "Custom error";

    // When & Then - Test assertTrue with string message (valid)
    ValidCheck.require().assertTrue(validCondition, message);

    // When & Then - Test assertTrue with string message (invalid)
    assertThatThrownBy(() -> ValidCheck.require().assertTrue(invalidCondition, message))
        .isInstanceOf(ValidationException.class)
        .hasMessage(message);

    // When & Then - Test assertTrue with supplier message (invalid)
    assertThatThrownBy(
            () -> ValidCheck.require().assertTrue(invalidCondition, () -> "Dynamic: " + message))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Dynamic: " + message);

    // When & Then - Test assertFalse with string message (valid)
    ValidCheck.require().assertFalse(invalidCondition, message);

    // When & Then - Test assertFalse with string message (invalid)
    assertThatThrownBy(() -> ValidCheck.require().assertFalse(validCondition, message))
        .isInstanceOf(ValidationException.class)
        .hasMessage(message);

    // When & Then - Test assertFalse with supplier message (invalid)
    assertThatThrownBy(
            () -> ValidCheck.require().assertFalse(validCondition, () -> "Supplier: " + message))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Supplier: " + message);
  }

  @Test
  void notNullMethodsWithAllOverloads() {
    // Given - Test objects
    String validValue = "test";
    String nullValue = null;
    String paramName = "testParam";

    // When & Then - Test notNull with name (valid)
    ValidCheck.require().notNull(validValue, paramName);

    // When & Then - Test notNull with name (invalid)
    assertThatThrownBy(() -> ValidCheck.require().notNull(nullValue, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'testParam' must not be null");

    // When & Then - Test notNull with supplier (invalid)
    assertThatThrownBy(() -> ValidCheck.require().notNull(nullValue, () -> "Custom null message"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Custom null message");

    // When & Then - Test notNull without name (invalid)
    assertThatThrownBy(() -> ValidCheck.require().notNull(nullValue))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must not be null");
  }

  @Test
  void numericRangeMethodsWithAllOverloads() {
    // Given - Test numeric values
    Integer validValue = 5;
    Integer invalidValue = 15;
    Integer min = 1;
    Integer max = 10;
    String paramName = "number";

    // When & Then - Test inRange with name (valid)
    ValidCheck.require().inRange(validValue, min, max, paramName);

    // When & Then - Test inRange with name (invalid)
    assertThatThrownBy(() -> ValidCheck.require().inRange(invalidValue, min, max, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'number' must be between 1 and 10");

    // When & Then - Test inRange with supplier (invalid)
    assertThatThrownBy(
            () ->
                ValidCheck.require()
                    .inRange(invalidValue, min, max, () -> "Range validation failed"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Range validation failed");

    // When & Then - Test inRange without name (invalid)
    assertThatThrownBy(() -> ValidCheck.require().inRange(invalidValue, min, max))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be between 1 and 10");

    // When & Then - Test IllegalArgumentException for null min/max
    assertThatThrownBy(() -> ValidCheck.require().inRange(validValue, null, max, paramName))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("min and max cannot be null");
  }

  @Test
  void stringNotNullOrEmptyMethodsWithAllOverloads() {
    // Given - String test values
    String validString = "valid";
    String emptyString = "";
    String nullString = null;
    String paramName = "stringParam";

    // When & Then - Test string notNullOrEmpty with name (valid)
    ValidCheck.require().notNullOrEmpty(validString, paramName);

    // When & Then - Test string notNullOrEmpty with name (empty)
    assertThatThrownBy(() -> ValidCheck.require().notNullOrEmpty(emptyString, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'stringParam' must not be null or empty");

    // When & Then - Test string notNullOrEmpty with name (null)
    assertThatThrownBy(() -> ValidCheck.require().notNullOrEmpty(nullString, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'stringParam' must not be null or empty");

    // When & Then - Test string notNullOrEmpty with supplier (empty)
    assertThatThrownBy(
            () -> ValidCheck.require().notNullOrEmpty(emptyString, () -> "String cannot be empty"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("String cannot be empty");

    // When & Then - Test string notNullOrEmpty without name (empty)
    assertThatThrownBy(() -> ValidCheck.require().notNullOrEmpty(emptyString))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must not be null or empty");
  }

  @Test
  void collectionNotNullOrEmptyMethodsWithAllOverloads() {
    // Given - Collection test values
    List<String> validCollection = List.of("a", "b");
    List<String> emptyCollection = Collections.emptyList();
    List<String> nullCollection = null;
    String paramName = "collection";

    // When & Then - Test collection notNullOrEmpty with name (valid)
    ValidCheck.require().notNullOrEmpty(validCollection, paramName);

    // When & Then - Test collection notNullOrEmpty with name (empty)
    assertThatThrownBy(() -> ValidCheck.require().notNullOrEmpty(emptyCollection, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'collection' must not be null or empty");

    // When & Then - Test collection notNullOrEmpty with name (null)
    assertThatThrownBy(() -> ValidCheck.require().notNullOrEmpty(nullCollection, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'collection' must not be null or empty");

    // When & Then - Test collection notNullOrEmpty with supplier (empty)
    assertThatThrownBy(
            () ->
                ValidCheck.require()
                    .notNullOrEmpty(emptyCollection, () -> "Collection is required"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Collection is required");

    // When & Then - Test collection notNullOrEmpty without name (empty)
    assertThatThrownBy(() -> ValidCheck.require().notNullOrEmpty(emptyCollection))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must not be null or empty");
  }

  @Test
  void mapNotNullOrEmptyMethodsWithAllOverloads() {
    // Given - Map test values
    Map<String, String> validMap = Map.of("key", "value");
    Map<String, String> emptyMap = new HashMap<>();
    Map<String, String> nullMap = null;
    String paramName = "mapParam";

    // When & Then - Test map notNullOrEmpty with name (valid)
    ValidCheck.require().notNullOrEmpty(validMap, paramName);

    // When & Then - Test map notNullOrEmpty with name (empty)
    assertThatThrownBy(() -> ValidCheck.require().notNullOrEmpty(emptyMap, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'mapParam' must not be null or empty");

    // When & Then - Test map notNullOrEmpty with name (null)
    assertThatThrownBy(() -> ValidCheck.require().notNullOrEmpty(nullMap, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'mapParam' must not be null or empty");

    // When & Then - Test map notNullOrEmpty with supplier (empty)
    assertThatThrownBy(
            () -> ValidCheck.require().notNullOrEmpty(emptyMap, () -> "Map cannot be empty"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Map cannot be empty");

    // When & Then - Test map notNullOrEmpty without name (empty)
    assertThatThrownBy(() -> ValidCheck.require().notNullOrEmpty(emptyMap))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must not be null or empty");
  }

  @Test
  void notBlankMethodsWithAllOverloads() {
    // Given - String test values for blank testing
    String validString = "valid";
    String blankString = "   ";
    String emptyString = "";
    String nullString = null;
    String paramName = "text";

    // When & Then - Test notBlank with name (valid)
    ValidCheck.require().notBlank(validString, paramName);

    // When & Then - Test notBlank with name (blank)
    assertThatThrownBy(() -> ValidCheck.require().notBlank(blankString, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'text' must not be blank");

    // When & Then - Test notBlank with name (empty)
    assertThatThrownBy(() -> ValidCheck.require().notBlank(emptyString, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'text' must not be blank");

    // When & Then - Test notBlank with name (null)
    assertThatThrownBy(() -> ValidCheck.require().notBlank(nullString, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'text' must not be blank");

    // When & Then - Test notBlank with supplier (blank)
    assertThatThrownBy(
            () -> ValidCheck.require().notBlank(blankString, () -> "Text cannot be blank"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Text cannot be blank");

    // When & Then - Test notBlank without name (blank)
    assertThatThrownBy(() -> ValidCheck.require().notBlank(blankString))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must not be blank");
  }

  @Test
  void hasLengthMethodsWithAllOverloads() {
    // Given - String length test values
    String validString = "password";
    String shortString = "abc";
    String longString = "a".repeat(101);
    String nullString = null;
    int minLength = 5;
    int maxLength = 20;
    String paramName = "password";

    // When & Then - Test hasLength with name (valid)
    ValidCheck.require().hasLength(validString, minLength, maxLength, paramName);

    // When & Then - Test hasLength with name (too short)
    assertThatThrownBy(
            () -> ValidCheck.require().hasLength(shortString, minLength, maxLength, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'password' must have length between 5 and 20");

    // When & Then - Test hasLength with name (too long)
    assertThatThrownBy(
            () -> ValidCheck.require().hasLength(longString, minLength, maxLength, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'password' must have length between 5 and 20");

    // When & Then - Test hasLength with name (null)
    assertThatThrownBy(
            () -> ValidCheck.require().hasLength(nullString, minLength, maxLength, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'password' must have length between 5 and 20");

    // When & Then - Test hasLength with supplier (short)
    assertThatThrownBy(
            () ->
                ValidCheck.require()
                    .hasLength(shortString, minLength, maxLength, () -> "Invalid length"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Invalid length");

    // When & Then - Test hasLength without name (long)
    assertThatThrownBy(() -> ValidCheck.require().hasLength(longString, minLength, maxLength))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must have length between 5 and 20");

    // When & Then - Test IllegalArgumentException for invalid range
    assertThatThrownBy(() -> ValidCheck.require().hasLength(validString, 10, 5, paramName))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("minLength cannot be greater than maxLength");
  }

  @Test
  void hasSizeMethodsWithAllOverloads() {
    // Given - Collection size test values
    List<String> validCollection = List.of("a", "b", "c");
    List<String> smallCollection = List.of("a");
    List<String> largeCollection = List.of("a", "b", "c", "d", "e", "f");
    List<String> nullCollection = null;
    int minSize = 2;
    int maxSize = 5;
    String paramName = "items";

    // When & Then - Test hasSize with name (valid)
    ValidCheck.require().hasSize(validCollection, minSize, maxSize, paramName);

    // When & Then - Test hasSize with name (too small)
    assertThatThrownBy(
            () -> ValidCheck.require().hasSize(smallCollection, minSize, maxSize, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'items' must have size between 2 and 5");

    // When & Then - Test hasSize with name (too large)
    assertThatThrownBy(
            () -> ValidCheck.require().hasSize(largeCollection, minSize, maxSize, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'items' must have size between 2 and 5");

    // When & Then - Test hasSize with name (null)
    assertThatThrownBy(
            () -> ValidCheck.require().hasSize(nullCollection, minSize, maxSize, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'items' must have size between 2 and 5");

    // When & Then - Test hasSize with supplier (small)
    assertThatThrownBy(
            () ->
                ValidCheck.require()
                    .hasSize(smallCollection, minSize, maxSize, () -> "Invalid size"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Invalid size");

    // When & Then - Test hasSize without name (large)
    assertThatThrownBy(() -> ValidCheck.require().hasSize(largeCollection, minSize, maxSize))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must have size between 2 and 5");

    // When & Then - Test IllegalArgumentException for invalid range
    assertThatThrownBy(() -> ValidCheck.require().hasSize(validCollection, 10, 5, paramName))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("minSize cannot be greater than maxSize");
  }

  @Test
  void numericPositiveAndNegativeMethodsWithAllOverloads() {
    // Given - Numeric test values
    Double positiveValue = 42.5;
    Double zeroValue = 0.0;
    Double negativeValue = -10.0;
    Double nullValue = null;
    String paramName = "amount";

    // When & Then - Test isPositive with name (valid)
    ValidCheck.require().isPositive(positiveValue, paramName);

    // When & Then - Test isPositive with name (zero)
    assertThatThrownBy(() -> ValidCheck.require().isPositive(zeroValue, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'amount' must be positive");

    // When & Then - Test isPositive with name (negative)
    assertThatThrownBy(() -> ValidCheck.require().isPositive(negativeValue, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'amount' must be positive");

    // When & Then - Test isPositive with name (null)
    assertThatThrownBy(() -> ValidCheck.require().isPositive(nullValue, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'amount' must be positive");

    // When & Then - Test isPositive with supplier (zero)
    assertThatThrownBy(() -> ValidCheck.require().isPositive(zeroValue, () -> "Must be positive"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Must be positive");

    // When & Then - Test isPositive without name (negative)
    assertThatThrownBy(() -> ValidCheck.require().isPositive(negativeValue))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be positive");

    // When & Then - Test isNegative with name (valid)
    ValidCheck.require().isNegative(negativeValue, paramName);

    // When & Then - Test isNegative with name (zero)
    assertThatThrownBy(() -> ValidCheck.require().isNegative(zeroValue, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'amount' must be negative");

    // When & Then - Test isNegative with name (positive)
    assertThatThrownBy(() -> ValidCheck.require().isNegative(positiveValue, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'amount' must be negative");

    // When & Then - Test isNegative with supplier (positive)
    assertThatThrownBy(
            () -> ValidCheck.require().isNegative(positiveValue, () -> "Must be negative"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Must be negative");

    // When & Then - Test isNegative without name (positive)
    assertThatThrownBy(() -> ValidCheck.require().isNegative(positiveValue))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be negative");
  }

  @Test
  void matchesStringRegexMethodsWithAllOverloads() {
    // Given - String regex test values
    String validEmail = "test@example.com";
    String invalidEmail = "not-an-email";
    String nullValue = null;
    String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    String paramName = "email";

    // When & Then - Test matches string regex with name (valid)
    ValidCheck.require().matches(validEmail, emailRegex, paramName);

    // When & Then - Test matches string regex with name (invalid)
    assertThatThrownBy(() -> ValidCheck.require().matches(invalidEmail, emailRegex, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'email' must match pattern");

    // When & Then - Test matches string regex with name (null value)
    assertThatThrownBy(() -> ValidCheck.require().matches(nullValue, emailRegex, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'email' must match pattern");

    // When & Then - Test matches string regex with supplier (invalid)
    assertThatThrownBy(
            () -> ValidCheck.require().matches(invalidEmail, emailRegex, () -> "Invalid format"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Invalid format");

    // When & Then - Test matches string regex without name (invalid)
    assertThatThrownBy(() -> ValidCheck.require().matches(invalidEmail, emailRegex))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must match pattern");

    // When & Then - Test IllegalArgumentException for null regex
    assertThatThrownBy(() -> ValidCheck.require().matches(validEmail, (String) null, paramName))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("regex pattern cannot be null");
  }

  @Test
  void matchesPatternMethodsWithAllOverloads() {
    // Given - Pattern test values
    String validInput = "ABC123";
    String invalidInput = "abc123";
    String nullValue = null;
    Pattern uppercasePattern = Pattern.compile("^[A-Z0-9]+$");
    String paramName = "code";

    // When & Then - Test matches Pattern with name (valid)
    ValidCheck.require().matches(validInput, uppercasePattern, paramName);

    // When & Then - Test matches Pattern with name (invalid)
    assertThatThrownBy(
            () -> ValidCheck.require().matches(invalidInput, uppercasePattern, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'code' must match pattern");

    // When & Then - Test matches Pattern with name (null value)
    assertThatThrownBy(() -> ValidCheck.require().matches(nullValue, uppercasePattern, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'code' must match pattern");

    // When & Then - Test matches Pattern with supplier (invalid)
    assertThatThrownBy(
            () ->
                ValidCheck.require()
                    .matches(invalidInput, uppercasePattern, () -> "Must be uppercase"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Must be uppercase");

    // When & Then - Test matches Pattern without name (invalid)
    assertThatThrownBy(() -> ValidCheck.require().matches(invalidInput, uppercasePattern))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must match pattern");

    // When & Then - Test IllegalArgumentException for null Pattern
    assertThatThrownBy(() -> ValidCheck.require().matches(validInput, (Pattern) null, paramName))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("regex pattern cannot be null");
  }

  @Test
  void parameterlessMethodsCoverMissingOverloads() {
    // Given - Values for testing parameter-less overloads
    String nullString = null;
    Integer outOfRange = 50;
    String longString = "a".repeat(200);
    List<String> smallList = List.of("a");
    Integer negativeNumber = -5;
    String invalidPattern = "123";

    // When & Then - Test parameter-less overloads that delegate to named versions
    assertThatThrownBy(() -> ValidCheck.require().inRange(outOfRange, 1, 10))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be between 1 and 10");

    assertThatThrownBy(() -> ValidCheck.require().notNullOrEmpty(nullString))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must not be null or empty");

    assertThatThrownBy(() -> ValidCheck.require().notNullOrEmpty(List.of()))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must not be null or empty");

    assertThatThrownBy(() -> ValidCheck.require().notBlank(nullString))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must not be blank");

    assertThatThrownBy(() -> ValidCheck.require().hasLength(longString, 1, 100))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must have length between 1 and 100");

    assertThatThrownBy(() -> ValidCheck.require().hasSize(smallList, 2, 5))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must have size between 2 and 5");

    assertThatThrownBy(() -> ValidCheck.require().isPositive(negativeNumber))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be positive");

    assertThatThrownBy(() -> ValidCheck.require().isNegative(-negativeNumber))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be negative");

    assertThatThrownBy(() -> ValidCheck.require().matches(invalidPattern, "^[a-z]+$"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must match pattern");

    assertThatThrownBy(
            () -> ValidCheck.require().matches(invalidPattern, Pattern.compile("^[a-z]+$")))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must match pattern");
  }
}
