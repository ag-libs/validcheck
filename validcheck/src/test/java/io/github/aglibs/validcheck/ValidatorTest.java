package io.github.aglibs.validcheck;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;
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

    // When & Then - Test string notEmpty with name (valid)
    ValidCheck.require().notEmpty(validString, paramName);

    // When & Then - Test string notEmpty with name (empty)
    assertThatThrownBy(() -> ValidCheck.require().notEmpty(emptyString, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'stringParam' must not be null or empty");

    // When & Then - Test string notEmpty with name (null)
    assertThatThrownBy(() -> ValidCheck.require().notEmpty(nullString, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'stringParam' must not be null or empty");

    // When & Then - Test string notEmpty with supplier (empty)
    assertThatThrownBy(
            () -> ValidCheck.require().notEmpty(emptyString, () -> "String cannot be empty"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("String cannot be empty");

    // When & Then - Test string notEmpty without name (empty)
    assertThatThrownBy(() -> ValidCheck.require().notEmpty(emptyString))
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

    // When & Then - Test collection notEmpty with name (valid)
    ValidCheck.require().notEmpty(validCollection, paramName);

    // When & Then - Test collection notEmpty with name (empty)
    assertThatThrownBy(() -> ValidCheck.require().notEmpty(emptyCollection, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'collection' must not be null or empty");

    // When & Then - Test collection notEmpty with name (null)
    assertThatThrownBy(() -> ValidCheck.require().notEmpty(nullCollection, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'collection' must not be null or empty");

    // When & Then - Test collection notEmpty with supplier (empty)
    assertThatThrownBy(
            () -> ValidCheck.require().notEmpty(emptyCollection, () -> "Collection is required"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Collection is required");

    // When & Then - Test collection notEmpty without name (empty)
    assertThatThrownBy(() -> ValidCheck.require().notEmpty(emptyCollection))
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

    // When & Then - Test map notEmpty with name (valid)
    ValidCheck.require().notEmpty(validMap, paramName);

    // When & Then - Test map notEmpty with name (empty)
    assertThatThrownBy(() -> ValidCheck.require().notEmpty(emptyMap, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'mapParam' must not be null or empty");

    // When & Then - Test map notEmpty with name (null)
    assertThatThrownBy(() -> ValidCheck.require().notEmpty(nullMap, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'mapParam' must not be null or empty");

    // When & Then - Test map notEmpty with supplier (empty)
    assertThatThrownBy(() -> ValidCheck.require().notEmpty(emptyMap, () -> "Map cannot be empty"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Map cannot be empty");

    // When & Then - Test map notEmpty without name (empty)
    assertThatThrownBy(() -> ValidCheck.require().notEmpty(emptyMap))
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
  void mapHasSizeMethodsWithAllOverloads() {
    // Given - Map size test values
    Map<String, String> validMap = Map.of("a", "1", "b", "2", "c", "3");
    Map<String, String> smallMap = Map.of("a", "1");
    Map<String, String> largeMap =
        Map.of("a", "1", "b", "2", "c", "3", "d", "4", "e", "5", "f", "6");
    Map<String, String> nullMap = null;
    int minSize = 2;
    int maxSize = 5;
    String paramName = "config";

    // When & Then - Test hasSize with name (valid)
    ValidCheck.require().hasSize(validMap, minSize, maxSize, paramName);

    // When & Then - Test hasSize with name (too small)
    assertThatThrownBy(() -> ValidCheck.require().hasSize(smallMap, minSize, maxSize, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'config' must have size between 2 and 5");

    // When & Then - Test hasSize with name (too large)
    assertThatThrownBy(() -> ValidCheck.require().hasSize(largeMap, minSize, maxSize, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'config' must have size between 2 and 5");

    // When & Then - Test hasSize with name (null)
    assertThatThrownBy(() -> ValidCheck.require().hasSize(nullMap, minSize, maxSize, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'config' must have size between 2 and 5");

    // When & Then - Test hasSize with supplier (small)
    assertThatThrownBy(
            () ->
                ValidCheck.require().hasSize(smallMap, minSize, maxSize, () -> "Invalid map size"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Invalid map size");

    // When & Then - Test hasSize without name (large)
    assertThatThrownBy(() -> ValidCheck.require().hasSize(largeMap, minSize, maxSize))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must have size between 2 and 5");

    // When & Then - Test IllegalArgumentException for invalid range
    assertThatThrownBy(() -> ValidCheck.require().hasSize(validMap, 10, 5, paramName))
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

    assertThatThrownBy(() -> ValidCheck.require().notEmpty(nullString))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must not be null or empty");

    assertThatThrownBy(() -> ValidCheck.require().notEmpty(List.of()))
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

  @Test
  void nullOrConditionalValidationMethods() {
    // Test nullOrInRange - null values should pass
    ValidCheck.require().nullOrInRange(null, 1, 10, "age");
    ValidCheck.require().nullOrInRange((Integer) null, 1, 10);

    // Test nullOrInRange - valid values should pass
    ValidCheck.require().nullOrInRange(5, 1, 10, "age");
    ValidCheck.require().nullOrInRange(1.5, 1.0, 10.0, "score");

    // Test nullOrInRange - invalid values should fail
    assertThatThrownBy(() -> ValidCheck.require().nullOrInRange(15, 1, 10, "age"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'age' must be null or between 1 and 10");

    assertThatThrownBy(() -> ValidCheck.require().nullOrInRange(-5, 1, 10))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be null or between 1 and 10");

    // Test nullOrNotEmpty (String) - null values should pass
    ValidCheck.require().nullOrNotEmpty((String) null, "description");
    ValidCheck.require().nullOrNotEmpty((String) null);

    // Test nullOrNotEmpty (String) - non-empty values should pass
    ValidCheck.require().nullOrNotEmpty("hello", "description");
    ValidCheck.require().nullOrNotEmpty("world");

    // Test nullOrNotEmpty (String) - empty values should fail
    assertThatThrownBy(() -> ValidCheck.require().nullOrNotEmpty("", "description"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'description' must be null or not empty");

    assertThatThrownBy(() -> ValidCheck.require().nullOrNotEmpty(""))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be null or not empty");

    // Test nullOrNotEmpty (Collection) - null values should pass
    ValidCheck.require().nullOrNotEmpty((Collection<String>) null, "tags");
    ValidCheck.require().nullOrNotEmpty((Collection<String>) null);

    // Test nullOrNotEmpty (Collection) - non-empty values should pass
    ValidCheck.require().nullOrNotEmpty(List.of("tag1"), "tags");
    ValidCheck.require().nullOrNotEmpty(List.of("item"));

    // Test nullOrNotEmpty (Collection) - empty values should fail
    assertThatThrownBy(() -> ValidCheck.require().nullOrNotEmpty(List.of(), "tags"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'tags' must be null or not empty");

    // Test nullOrNotEmpty (Map) - null values should pass
    ValidCheck.require().nullOrNotEmpty((Map<String, String>) null, "config");
    ValidCheck.require().nullOrNotEmpty((Map<String, String>) null);

    // Test nullOrNotEmpty (Map) - non-empty values should pass
    ValidCheck.require().nullOrNotEmpty(Map.of("key", "value"), "config");
    ValidCheck.require().nullOrNotEmpty(Map.of("a", "b"));

    // Test nullOrNotEmpty (Map) - empty values should fail
    assertThatThrownBy(() -> ValidCheck.require().nullOrNotEmpty(Map.of(), "config"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'config' must be null or not empty");

    // Test nullOrNotBlank - null values should pass
    ValidCheck.require().nullOrNotBlank(null, "bio");
    ValidCheck.require().nullOrNotBlank(null);

    // Test nullOrNotBlank - non-blank values should pass
    ValidCheck.require().nullOrNotBlank("hello", "bio");
    ValidCheck.require().nullOrNotBlank("world");

    // Test nullOrNotBlank - blank values should fail
    assertThatThrownBy(() -> ValidCheck.require().nullOrNotBlank("   ", "bio"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'bio' must be null or not blank");

    assertThatThrownBy(() -> ValidCheck.require().nullOrNotBlank(""))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be null or not blank");

    // Test nullOrHasLength - null values should pass
    ValidCheck.require().nullOrHasLength(null, 5, 20, "username");
    ValidCheck.require().nullOrHasLength(null, 1, 10);

    // Test nullOrHasLength - valid lengths should pass
    ValidCheck.require().nullOrHasLength("hello", 3, 10, "username");
    ValidCheck.require().nullOrHasLength("world", 1, 20);

    // Test nullOrHasLength - invalid lengths should fail
    assertThatThrownBy(() -> ValidCheck.require().nullOrHasLength("hi", 5, 20, "username"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'username' must be null or have length between 5 and 20");

    assertThatThrownBy(() -> ValidCheck.require().nullOrHasLength("verylongstring", 1, 10))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be null or have length between 1 and 10");

    // Test nullOrHasSize - null values should pass
    ValidCheck.require().nullOrHasSize((Collection<String>) null, 2, 5, "items");
    ValidCheck.require().nullOrHasSize((Collection<String>) null, 1, 3);

    // Test nullOrHasSize - valid sizes should pass
    ValidCheck.require().nullOrHasSize(List.of("a", "b", "c"), 2, 5, "items");
    ValidCheck.require().nullOrHasSize(List.of("x"), 1, 3);

    // Test nullOrHasSize - invalid sizes should fail
    assertThatThrownBy(() -> ValidCheck.require().nullOrHasSize(List.of("a"), 2, 5, "items"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'items' must be null or have size between 2 and 5");

    assertThatThrownBy(() -> ValidCheck.require().nullOrHasSize(List.of("a", "b", "c", "d"), 1, 3))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be null or have size between 1 and 3");

    // Test nullOrHasSize (Map) - null values should pass
    ValidCheck.require().nullOrHasSize((Map<String, String>) null, 2, 5, "config");
    ValidCheck.require().nullOrHasSize((Map<String, String>) null, 1, 3);

    // Test nullOrHasSize (Map) - valid sizes should pass
    ValidCheck.require().nullOrHasSize(Map.of("a", "1", "b", "2", "c", "3"), 2, 5, "config");
    ValidCheck.require().nullOrHasSize(Map.of("x", "1"), 1, 3);

    // Test nullOrHasSize (Map) - invalid sizes should fail
    assertThatThrownBy(() -> ValidCheck.require().nullOrHasSize(Map.of("a", "1"), 2, 5, "config"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'config' must be null or have size between 2 and 5");

    assertThatThrownBy(
            () ->
                ValidCheck.require()
                    .nullOrHasSize(Map.of("a", "1", "b", "2", "c", "3", "d", "4"), 1, 3))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be null or have size between 1 and 3");

    // Test nullOrIsPositive - null values should pass
    ValidCheck.require().nullOrIsPositive(null, "amount");
    ValidCheck.require().nullOrIsPositive(null);

    // Test nullOrIsPositive - positive values should pass
    ValidCheck.require().nullOrIsPositive(5, "amount");
    ValidCheck.require().nullOrIsPositive(3.14);

    // Test nullOrIsPositive - non-positive values should fail
    assertThatThrownBy(() -> ValidCheck.require().nullOrIsPositive(0, "amount"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'amount' must be null or positive");

    assertThatThrownBy(() -> ValidCheck.require().nullOrIsPositive(-5))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be null or positive");

    // Test nullOrIsNegative - null values should pass
    ValidCheck.require().nullOrIsNegative(null, "deficit");
    ValidCheck.require().nullOrIsNegative(null);

    // Test nullOrIsNegative - negative values should pass
    ValidCheck.require().nullOrIsNegative(-5, "deficit");
    ValidCheck.require().nullOrIsNegative(-3.14);

    // Test nullOrIsNegative - non-negative values should fail
    assertThatThrownBy(() -> ValidCheck.require().nullOrIsNegative(0, "deficit"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'deficit' must be null or negative");

    assertThatThrownBy(() -> ValidCheck.require().nullOrIsNegative(5))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be null or negative");

    // Test nullOrMatches (String regex) - null values should pass
    ValidCheck.require().nullOrMatches(null, "^[a-z]+$", "code");
    ValidCheck.require().nullOrMatches(null, "^\\d+$");

    // Test nullOrMatches (String regex) - matching values should pass
    ValidCheck.require().nullOrMatches("hello", "^[a-z]+$", "code");
    ValidCheck.require().nullOrMatches("123", "^\\d+$");

    // Test nullOrMatches (String regex) - non-matching values should fail
    assertThatThrownBy(() -> ValidCheck.require().nullOrMatches("Hello", "^[a-z]+$", "code"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'code' must be null or match pattern '^[a-z]+$'");

    assertThatThrownBy(() -> ValidCheck.require().nullOrMatches("abc", "^\\d+$"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be null or match pattern '^\\d+$'");

    // Test nullOrMatches (Pattern regex) - null values should pass
    Pattern pattern = Pattern.compile("^[A-Z]+$");
    ValidCheck.require().nullOrMatches(null, pattern, "name");
    ValidCheck.require().nullOrMatches(null, pattern);

    // Test nullOrMatches (Pattern regex) - matching values should pass
    ValidCheck.require().nullOrMatches("HELLO", pattern, "name");
    ValidCheck.require().nullOrMatches("WORLD", pattern);

    // Test nullOrMatches (Pattern regex) - non-matching values should fail
    assertThatThrownBy(() -> ValidCheck.require().nullOrMatches("hello", pattern, "name"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'name' must be null or match pattern '^[A-Z]+$'");

    assertThatThrownBy(() -> ValidCheck.require().nullOrMatches("world", pattern))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be null or match pattern '^[A-Z]+$'");
  }

  @Test
  void nullOrConditionalValidationMethodsWithInvalidArguments() {
    // Test nullOrInRange with null min/max
    assertThatThrownBy(() -> ValidCheck.require().nullOrInRange(5, null, 10, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("min and max cannot be null");

    assertThatThrownBy(() -> ValidCheck.require().nullOrInRange(5, 1, null, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("min and max cannot be null");

    // Test nullOrHasLength with invalid range
    assertThatThrownBy(() -> ValidCheck.require().nullOrHasLength("test", 10, 5, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("minLength cannot be greater than maxLength");

    // Test nullOrHasSize with invalid range
    assertThatThrownBy(() -> ValidCheck.require().nullOrHasSize(List.of(), 5, 2, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("minSize cannot be greater than maxSize");

    // Test nullOrHasSize (Map) with invalid range
    assertThatThrownBy(() -> ValidCheck.require().nullOrHasSize(Map.of(), 5, 2, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("minSize cannot be greater than maxSize");

    // Test nullOrMatches with null regex
    assertThatThrownBy(() -> ValidCheck.require().nullOrMatches("test", (String) null, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("regex pattern cannot be null");

    assertThatThrownBy(() -> ValidCheck.require().nullOrMatches("test", (Pattern) null, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("regex pattern cannot be null");
  }

  @Test
  void isNonNegativeAndIsNonPositiveMethods() {
    // Test isNonNegative - zero should pass (>= 0)
    ValidCheck.require().isNonNegative(0, "value");
    ValidCheck.require().isNonNegative(0.0);
    ValidCheck.require().isNonNegative(5);
    ValidCheck.require().isNonNegative(3.14);

    // Test isNonNegative - negative values should fail
    assertThatThrownBy(() -> ValidCheck.require().isNonNegative(-1, "value"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'value' must be non-negative");

    assertThatThrownBy(() -> ValidCheck.require().isNonNegative(-0.1))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be non-negative");

    // Test isNonNegative - null values should fail
    assertThatThrownBy(() -> ValidCheck.require().isNonNegative(null, "value"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'value' must be non-negative");

    // Test isNonPositive - zero should pass (<= 0)
    ValidCheck.require().isNonPositive(0, "value");
    ValidCheck.require().isNonPositive(-0.0);
    ValidCheck.require().isNonPositive(-5);
    ValidCheck.require().isNonPositive(-3.14);

    // Test isNonPositive - positive values should fail
    assertThatThrownBy(() -> ValidCheck.require().isNonPositive(1, "value"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'value' must be non-positive");

    assertThatThrownBy(() -> ValidCheck.require().isNonPositive(0.1))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be non-positive");

    // Test isNonPositive - null values should fail
    assertThatThrownBy(() -> ValidCheck.require().isNonPositive(null, "value"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'value' must be non-positive");
  }

  @Test
  void nullOrIsNonNegativeAndNullOrIsNonPositiveMethods() {
    // Test nullOrIsNonNegative - null values should pass
    ValidCheck.require().nullOrIsNonNegative(null, "amount");
    ValidCheck.require().nullOrIsNonNegative(null);

    // Test nullOrIsNonNegative - non-negative values should pass (>= 0)
    ValidCheck.require().nullOrIsNonNegative(0, "amount");
    ValidCheck.require().nullOrIsNonNegative(5);
    ValidCheck.require().nullOrIsNonNegative(3.14);

    // Test nullOrIsNonNegative - negative values should fail
    assertThatThrownBy(() -> ValidCheck.require().nullOrIsNonNegative(-1, "amount"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'amount' must be null or non-negative");

    assertThatThrownBy(() -> ValidCheck.require().nullOrIsNonNegative(-0.1))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be null or non-negative");

    // Test nullOrIsNonPositive - null values should pass
    ValidCheck.require().nullOrIsNonPositive(null, "deficit");
    ValidCheck.require().nullOrIsNonPositive(null);

    // Test nullOrIsNonPositive - non-positive values should pass (<= 0)
    ValidCheck.require().nullOrIsNonPositive(0, "deficit");
    ValidCheck.require().nullOrIsNonPositive(-5);
    ValidCheck.require().nullOrIsNonPositive(-3.14);

    // Test nullOrIsNonPositive - positive values should fail
    assertThatThrownBy(() -> ValidCheck.require().nullOrIsNonPositive(1, "deficit"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'deficit' must be null or non-positive");

    assertThatThrownBy(() -> ValidCheck.require().nullOrIsNonPositive(0.1))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be null or non-positive");
  }

  @Test
  void signValidationSemanticDifferences() {
    // Demonstrate the semantic differences between the sign validation methods

    // Value: -1
    assertThatThrownBy(() -> ValidCheck.require().isPositive(-1)) // > 0, fails
        .isInstanceOf(ValidationException.class);
    assertThatThrownBy(() -> ValidCheck.require().isNonNegative(-1)) // >= 0, fails
        .isInstanceOf(ValidationException.class);
    ValidCheck.require().isNegative(-1); // < 0, passes
    ValidCheck.require().isNonPositive(-1); // <= 0, passes

    // Value: 0
    assertThatThrownBy(() -> ValidCheck.require().isPositive(0)) // > 0, fails
        .isInstanceOf(ValidationException.class);
    ValidCheck.require().isNonNegative(0); // >= 0, passes
    assertThatThrownBy(() -> ValidCheck.require().isNegative(0)) // < 0, fails
        .isInstanceOf(ValidationException.class);
    ValidCheck.require().isNonPositive(0); // <= 0, passes

    // Value: 1
    ValidCheck.require().isPositive(1); // > 0, passes
    ValidCheck.require().isNonNegative(1); // >= 0, passes
    assertThatThrownBy(() -> ValidCheck.require().isNegative(1)) // < 0, fails
        .isInstanceOf(ValidationException.class);
    assertThatThrownBy(() -> ValidCheck.require().isNonPositive(1)) // <= 0, fails
        .isInstanceOf(ValidationException.class);
  }

  @Test
  void minAndMaxMethods() {
    // Test min() - valid values should pass (>= minValue)
    ValidCheck.require().min(18, 18, "age");
    ValidCheck.require().min(25, 18);
    ValidCheck.require().min(100.5, 50.0, "score");

    // Test min() - invalid values should fail
    assertThatThrownBy(() -> ValidCheck.require().min(17, 18, "age"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'age' must be at least 18");

    assertThatThrownBy(() -> ValidCheck.require().min(10, 18))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be at least 18");

    // Test min() - null values should fail
    assertThatThrownBy(() -> ValidCheck.require().min(null, 18, "age"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'age' must be at least 18");

    // Test max() - valid values should pass (<= maxValue)
    ValidCheck.require().max(100, 100, "percentage");
    ValidCheck.require().max(75, 100);
    ValidCheck.require().max(4.5, 10.0, "rating");

    // Test max() - invalid values should fail
    assertThatThrownBy(() -> ValidCheck.require().max(101, 100, "percentage"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'percentage' must be at most 100");

    assertThatThrownBy(() -> ValidCheck.require().max(150, 100))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be at most 100");

    // Test max() - null values should fail
    assertThatThrownBy(() -> ValidCheck.require().max(null, 100, "percentage"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'percentage' must be at most 100");
  }

  @Test
  void nullOrMinAndNullOrMaxMethods() {
    // Test nullOrMin() - null values should pass
    ValidCheck.require().nullOrMin(null, 18, "age");
    ValidCheck.require().nullOrMin(null, 0);

    // Test nullOrMin() - valid values should pass (>= minValue)
    ValidCheck.require().nullOrMin(18, 18, "age");
    ValidCheck.require().nullOrMin(25, 18);
    ValidCheck.require().nullOrMin(100.5, 50.0, "score");

    // Test nullOrMin() - invalid values should fail
    assertThatThrownBy(() -> ValidCheck.require().nullOrMin(17, 18, "age"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'age' must be null or at least 18");

    assertThatThrownBy(() -> ValidCheck.require().nullOrMin(10, 18))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be null or at least 18");

    // Test nullOrMax() - null values should pass
    ValidCheck.require().nullOrMax(null, 100, "percentage");
    ValidCheck.require().nullOrMax(null, 10);

    // Test nullOrMax() - valid values should pass (<= maxValue)
    ValidCheck.require().nullOrMax(100, 100, "percentage");
    ValidCheck.require().nullOrMax(75, 100);
    ValidCheck.require().nullOrMax(4.5, 10.0, "rating");

    // Test nullOrMax() - invalid values should fail
    assertThatThrownBy(() -> ValidCheck.require().nullOrMax(101, 100, "percentage"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'percentage' must be null or at most 100");

    assertThatThrownBy(() -> ValidCheck.require().nullOrMax(150, 100))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be null or at most 100");
  }

  @Test
  void minMaxMethodsWithInvalidArguments() {
    // Test min() with null minValue
    assertThatThrownBy(() -> ValidCheck.require().min(5, null, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("minValue cannot be null");

    // Test max() with null maxValue
    assertThatThrownBy(() -> ValidCheck.require().max(5, null, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("maxValue cannot be null");

    // Test nullOrMin() with null minValue
    assertThatThrownBy(() -> ValidCheck.require().nullOrMin(5, null, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("minValue cannot be null");

    // Test nullOrMax() with null maxValue
    assertThatThrownBy(() -> ValidCheck.require().nullOrMax(5, null, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("maxValue cannot be null");
  }

  @Test
  void singleBoundVsRangeValidationComparison() {
    // Demonstrate the difference between single-bound and range validation

    // Range validation requires both bounds (awkward for single bounds)
    ValidCheck.require().inRange(50, 18, 100, "age");

    // Single-bound validation is cleaner and more expressive
    ValidCheck.require().min(50, 18, "age"); // Only care about minimum
    ValidCheck.require().max(50, 100, "age"); // Only care about maximum

    // Conditional versions work the same way
    ValidCheck.require().nullOrInRange(null, 18, 100, "age");
    ValidCheck.require().nullOrMin(null, 18, "age");
    ValidCheck.require().nullOrMax(null, 100, "age");

    // Error messages are more specific
    assertThatThrownBy(() -> ValidCheck.require().min(10, 18, "age"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("must be at least 18"); // Focused message

    assertThatThrownBy(() -> ValidCheck.require().max(150, 100, "percentage"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("must be at most 100"); // Focused message
  }

  @Test
  void isNullMethodsWithAllOverloads() {
    // Given - Test objects
    String nullValue = null;
    String nonNullValue = "test";
    String paramName = "testParam";

    // When & Then - Test isNull with name (valid - null value)
    ValidCheck.require().isNull(nullValue, paramName);

    // When & Then - Test isNull with name (invalid - non-null value)
    assertThatThrownBy(() -> ValidCheck.require().isNull(nonNullValue, paramName))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'testParam' must be null");

    // When & Then - Test isNull with supplier (invalid - non-null value)
    assertThatThrownBy(() -> ValidCheck.require().isNull(nonNullValue, () -> "Custom null message"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Custom null message");

    // When & Then - Test isNull without name (invalid - non-null value)
    assertThatThrownBy(() -> ValidCheck.require().isNull(nonNullValue))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("parameter must be null");
  }

  @Test
  void edgeCaseCoverageForMissedBranches() {
    // Test formatMessage with includeValue=false (covers missed branch in formatMessage)
    Validator validatorNoValues = new Validator(false, true, true); // includeValues = false
    assertThatThrownBy(() -> validatorNoValues.notNull(null, "test"))
        .hasMessage("'test' must not be null"); // Should not include value

    // Test inRange with null parameters (covers missed branches in inRange)
    assertThatThrownBy(() -> ValidCheck.require().inRange(5, null, 10, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("min and max cannot be null");

    assertThatThrownBy(() -> ValidCheck.require().inRange(5, 0, null, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("min and max cannot be null");

    // Test isNegative with edge cases (covers missed branch in isNegative)
    assertThatThrownBy(() -> ValidCheck.require().isNegative(null, () -> "custom null message"))
        .hasMessage("custom null message");

    // Test edge case where value is exactly at boundary for inRange
    ValidCheck.require().inRange(1, 1, 10, "value"); // Min boundary
    ValidCheck.require().inRange(10, 1, 10, "value"); // Max boundary

    // Test formatMessage with null name (covers missed branch in formatMessage)
    assertThatThrownBy(() -> ValidCheck.require().notNull(null, (String) null))
        .hasMessage("parameter must not be null"); // Should use "parameter" when name is null
  }

  @Test
  void includeValuesTrueShowsValuesInErrorMessages() {
    assertThatThrownBy(() -> inputValuesValidator().isPositive(-5, "age"))
        .hasMessage("'age' must be positive, but it was -5");

    assertThatThrownBy(() -> inputValuesValidator().isNegative(10, "temperature"))
        .hasMessage("'temperature' must be negative, but it was 10");

    assertThatThrownBy(() -> inputValuesValidator().inRange(100, 1, 50, "count"))
        .hasMessage("'count' must be between 1 and 50, but it was 100");

    assertThatThrownBy(() -> inputValuesValidator().min(-5, 0, "value"))
        .hasMessage("'value' must be at least 0, but it was -5");

    assertThatThrownBy(() -> inputValuesValidator().max(100, 50, "value"))
        .hasMessage("'value' must be at most 50, but it was 100");

    assertThatThrownBy(() -> inputValuesValidator().isNonNegative(-10, "balance"))
        .hasMessage("'balance' must be non-negative, but it was -10");

    assertThatThrownBy(() -> inputValuesValidator().isNonPositive(5, "debt"))
        .hasMessage("'debt' must be non-positive, but it was 5");

    // Test string validations include the value
    assertThatThrownBy(() -> inputValuesValidator().hasLength("ab", 5, 10, "username"))
        .hasMessage("'username' must have length between 5 and 10, but it was 'ab'");

    assertThatThrownBy(() -> inputValuesValidator().matches("ABC123", "^[a-z]+$", "code"))
        .hasMessage("'code' must match pattern '^[a-z]+$', but it was 'ABC123'");

    // Test with very long string - should truncate
    String longString =
        "a".repeat(150); // More than MAX_DISPLAYED_VALUE_LENGTH (100) so it will be truncated
    assertThatThrownBy(() -> inputValuesValidator().hasLength(longString, 1, 10, "text"))
        .hasMessageContaining("must have length between 1 and 10, but it was 'aaaaaa")
        .hasMessageContaining("...'"); // Should be truncated with ...

    // Test collection validations include the value
    assertThatThrownBy(() -> inputValuesValidator().hasSize(List.of("a"), 2, 5, "items"))
        .hasMessage("'items' must have size between 2 and 5, but it was [a]");

    // Test map validations include the value
    assertThatThrownBy(() -> inputValuesValidator().hasSize(Map.of(), 1, 5, "config"))
        .hasMessage("'config' must have size between 1 and 5, but it was {}");

    // Test that non-string objects are displayed without quotes
    assertThatThrownBy(() -> inputValuesValidator().isNull(42, "number"))
        .hasMessage("'number' must be null, but it was 42");

    // Test parameter-less methods with includeValues=true
    assertThatThrownBy(() -> inputValuesValidator().isPositive(-10))
        .hasMessage("parameter must be positive, but it was -10");

    assertThatThrownBy(() -> inputValuesValidator().hasLength("x", 5, 10))
        .hasMessage("parameter must have length between 5 and 10, but it was 'x'");
  }

  private static Validator inputValuesValidator() {
    return new Validator(true, true, true);
  }

  @Test
  void batchValidatorWithIncludeValuesTrue() {
    // Test BatchValidator with includeValues=true
    BatchValidator batchWithValues = new BatchValidator(true, true); // includeValues = true

    batchWithValues
        .isPositive(-5, "age")
        .hasLength("ab", 5, 10, "username")
        .inRange(100, 1, 50, "score");

    assertThatThrownBy(batchWithValues::validate)
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("'age' must be positive, but it was -5")
        .hasMessageContaining("'username' must have length between 5 and 10, but it was 'ab'")
        .hasMessageContaining("'score' must be between 1 and 50, but it was 100");
  }
}
