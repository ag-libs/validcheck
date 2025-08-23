package io.github.validcheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Base class for parameter validation with configurable behavior and fluent API. This class is
 * designed for extension and should not be instantiated directly. Use {@link ValidCheck#require()}
 * for fail-fast validation or {@link ValidCheck#check()} for batch validation.
 *
 * <p>The validator provides a fluent API that immediately throws on validation failures:
 *
 * <pre>{@code
 * // Fail-fast validation - throws immediately on first failure
 * ValidCheck.require()
 *          .notNull(name, "name")
 *          .notBlank(name, "name")
 *          .hasLength(name, 1, 50, "name"); // ValidationException thrown if any fail
 * }</pre>
 *
 * <p>For batch validation that collects multiple errors, use {@link BatchValidator}:
 *
 * <pre>{@code
 * // Batch validation - collects all errors before throwing
 * ValidCheck.check()
 *          .notNull(name, "name")
 *          .notBlank(name, "name")
 *          .hasLength(name, 1, 50, "name")
 *          .validate(); // Throws ValidationException with all collected errors
 * }</pre>
 *
 * <h3>Customization</h3>
 *
 * <p>This class can be extended to create custom validators with specialized behavior:
 *
 * <pre>{@code
 * public class MyValidator extends Validator {
 *
 *   public static MyValidator strictValidation() {
 *     return new MyValidator(false, true, true); // No values in errors, fail-fast, with stack traces
 *   }
 *
 *   public static MyValidator lenientValidation() {
 *     return new MyValidator(true, false, false); // Include values, batch errors, no stack traces
 *   }
 *
 *   protected MyValidator(boolean includeValues, boolean failFast, boolean fillStackTrace) {
 *     super(includeValues, failFast, fillStackTrace);
 *   }
 *
 *   // Add custom validation methods
 *   public MyValidator isValidEmail(String email, String name) {
 *     return (MyValidator) matches(email, EMAIL_PATTERN, name);
 *   }
 * }
 * }</pre>
 *
 * @since 0.9.0
 * @see ValidCheck#require()
 * @see ValidCheck#check()
 * @see BatchValidator
 * @see ValidationException
 */
public class Validator {

  private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();
  private static final int MAX_DISPLAYED_VALUE_LENGTH = 100;

  protected final boolean includeValues;
  protected final boolean failFast;
  protected final boolean fillStackTrace;

  protected final List<String> errors;

  /**
   * Constructs a new Validator with the specified configuration.
   *
   * @param includeValues whether to include actual values in error messages for debugging
   * @param failFast whether to throw immediately on first validation failure or collect all errors
   * @param fillStackTrace whether to fill stack traces in thrown exceptions
   */
  protected Validator(boolean includeValues, boolean failFast, boolean fillStackTrace) {
    this.includeValues = includeValues;
    this.failFast = failFast;
    this.fillStackTrace = fillStackTrace;

    errors = new ArrayList<>();
  }

  private String valueToString(Object value) {
    var string = String.valueOf(value);
    if (string.length() <= MAX_DISPLAYED_VALUE_LENGTH) {
      return string;
    }

    return string.substring(0, MAX_DISPLAYED_VALUE_LENGTH - 3) + "...";
  }

  private String formatMessage(Object value, String name, String message, boolean includeValue) {
    var paramName = name == null ? "parameter" : String.format("'%s'", name);
    var error = String.format("%s %s", paramName, message);

    if (this.includeValues && includeValue && value != null) {
      var stringValue = valueToString(value);
      var formattedValue =
          value instanceof String ? String.format("'%s'", stringValue) : stringValue;
      // limit the string length of string representation of the value
      return error + String.format(", but it was %s", formattedValue);
    }

    return error;
  }

  protected void validate() {
    if (!errors.isEmpty()) {
      final var errorMessage = String.join("; ", errors);
      throw new ValidationException(fillStackTrace, errorMessage, errors);
    }
  }

  /**
   * Validates that the specified condition is true.
   *
   * @param condition the condition to evaluate
   * @param message the error message to use if the condition is false
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if condition is false and fail-fast is enabled
   */
  public Validator assertTrue(boolean condition, String message) {
    return assertTrue(condition, () -> message);
  }

  /**
   * Validates that the specified condition is true.
   *
   * @param condition the condition to evaluate
   * @param messageSupplier supplier for the error message if the condition is false
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if condition is false and fail-fast is enabled
   */
  public Validator assertTrue(boolean condition, Supplier<String> messageSupplier) {
    if (!condition) {
      errors.add(messageSupplier.get());
      if (failFast) {
        validate();
      }
    }

    return this;
  }

  /**
   * Validates that the specified condition is false.
   *
   * @param condition the condition to evaluate
   * @param message the error message to use if the condition is true
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if condition is true and fail-fast is enabled
   */
  public Validator assertFalse(boolean condition, String message) {
    return assertTrue(!condition, () -> message);
  }

  /**
   * Validates that the specified condition is false.
   *
   * @param condition the condition to evaluate
   * @param messageSupplier supplier for the error message if the condition is true
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if condition is true and fail-fast is enabled
   */
  public Validator assertFalse(boolean condition, Supplier<String> messageSupplier) {
    return assertTrue(!condition, messageSupplier);
  }

  /**
   * Validates that the specified value is not null.
   *
   * @param value the value to check for null
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null and fail-fast is enabled
   */
  public Validator notNull(Object value, String name) {
    return notNull(value, () -> formatMessage(value, name, "must not be null", true));
  }

  /**
   * Validates that the specified value is not null.
   *
   * @param value the value to check for null
   * @param messageSupplier supplier for the error message if the value is null
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null and fail-fast is enabled
   */
  public Validator notNull(Object value, Supplier<String> messageSupplier) {
    return assertFalse(value == null, messageSupplier);
  }

  /**
   * Validates that the specified value is not null.
   *
   * @param value the value to check for null
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null and fail-fast is enabled
   */
  public Validator notNull(Object value) {
    return notNull(value, (String) null);
  }

  /**
   * Validates that the specified numeric value is within the given range (inclusive).
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param min the minimum allowed value (inclusive)
   * @param max the maximum allowed value (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or outside the range and fail-fast is
   *     enabled
   * @throws IllegalArgumentException if min or max are null
   */
  public <T extends Number> Validator inRange(T value, T min, T max, String name) {
    return inRange(
        value,
        min,
        max,
        () ->
            formatMessage(value, name, String.format("must be between %s and %s", min, max), true));
  }

  /**
   * Validates that the specified numeric value is within the given range (inclusive).
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param min the minimum allowed value (inclusive)
   * @param max the maximum allowed value (inclusive)
   * @param messageSupplier supplier for the error message if the value is outside the range
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or outside the range and fail-fast is
   *     enabled
   * @throws IllegalArgumentException if min or max are null
   */
  public <T extends Number> Validator inRange(
      T value, T min, T max, Supplier<String> messageSupplier) {
    if (min == null || max == null) {
      throw new IllegalArgumentException("min and max cannot be null");
    }

    return assertFalse(
        value == null
            || (value.doubleValue() < min.doubleValue() || value.doubleValue() > max.doubleValue()),
        messageSupplier);
  }

  /**
   * Validates that the specified numeric value is within the given range (inclusive).
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param min the minimum allowed value (inclusive)
   * @param max the maximum allowed value (inclusive)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or outside the range and fail-fast is
   *     enabled
   * @throws IllegalArgumentException if min or max are null
   */
  public <T extends Number> Validator inRange(T value, T min, T max) {
    return inRange(value, min, max, (String) null);
  }

  /**
   * Validates that the specified string is not null and not empty.
   *
   * @param value the string value to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notNullOrEmpty(String value, String name) {
    return notNullOrEmpty(
        value, () -> formatMessage(value, name, "must not be null or empty", false));
  }

  /**
   * Validates that the specified string is not null and not empty.
   *
   * @param value the string value to check
   * @param messageSupplier supplier for the error message if the value is null or empty
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notNullOrEmpty(String value, Supplier<String> messageSupplier) {
    return assertFalse(value == null || value.isEmpty(), messageSupplier);
  }

  /**
   * Validates that the specified string is not null and not empty.
   *
   * @param value the string value to check
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notNullOrEmpty(String value) {
    return notNullOrEmpty(value, (String) null);
  }

  /**
   * Validates that the specified collection is not null and not empty.
   *
   * @param value the collection to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notNullOrEmpty(Collection<?> value, String name) {
    return notNullOrEmpty(
        value, () -> formatMessage(value, name, "must not be null or empty", false));
  }

  /**
   * Validates that the specified collection is not null and not empty.
   *
   * @param value the collection to check
   * @param messageSupplier supplier for the error message if the value is null or empty
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notNullOrEmpty(Collection<?> value, Supplier<String> messageSupplier) {
    return assertFalse(value == null || value.isEmpty(), messageSupplier);
  }

  /**
   * Validates that the specified collection is not null and not empty.
   *
   * @param value the collection to check
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notNullOrEmpty(Collection<?> value) {
    return notNullOrEmpty(value, (String) null);
  }

  /**
   * Validates that the specified map is not null and not empty.
   *
   * @param value the map to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notNullOrEmpty(Map<?, ?> value, String name) {
    return notNullOrEmpty(
        value, () -> formatMessage(value, name, "must not be null or empty", false));
  }

  /**
   * Validates that the specified map is not null and not empty.
   *
   * @param value the map to check
   * @param messageSupplier supplier for the error message if the value is null or empty
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notNullOrEmpty(Map<?, ?> value, Supplier<String> messageSupplier) {
    return assertFalse(value == null || value.isEmpty(), messageSupplier);
  }

  /**
   * Validates that the specified map is not null and not empty.
   *
   * @param value the map to check
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notNullOrEmpty(Map<?, ?> value) {
    return notNullOrEmpty(value, (String) null);
  }

  /**
   * Validates that the specified string is not null, not empty, and not blank (contains
   * non-whitespace characters).
   *
   * @param value the string value to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null, empty, or blank and fail-fast is
   *     enabled
   */
  public Validator notBlank(String value, String name) {
    return notBlank(value, () -> formatMessage(value, name, "must not be blank", false));
  }

  /**
   * Validates that the specified string is not null, not empty, and not blank (contains
   * non-whitespace characters).
   *
   * @param value the string value to check
   * @param messageSupplier supplier for the error message if the value is null, empty, or blank
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null, empty, or blank and fail-fast is
   *     enabled
   */
  public Validator notBlank(String value, Supplier<String> messageSupplier) {
    return assertFalse(value == null || value.trim().isEmpty(), messageSupplier);
  }

  /**
   * Validates that the specified string is not null, not empty, and not blank (contains
   * non-whitespace characters).
   *
   * @param value the string value to check
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null, empty, or blank and fail-fast is
   *     enabled
   */
  public Validator notBlank(String value) {
    return notBlank(value, (String) null);
  }

  /**
   * Validates that the specified string has a length within the given range (inclusive).
   *
   * @param value the string value to check
   * @param minLength the minimum allowed length (inclusive)
   * @param maxLength the maximum allowed length (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or length is outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minLength is greater than maxLength
   */
  public Validator hasLength(String value, int minLength, int maxLength, String name) {
    return hasLength(
        value,
        minLength,
        maxLength,
        () ->
            formatMessage(
                value,
                name,
                String.format("must have length between %d and %d", minLength, maxLength),
                true));
  }

  /**
   * Validates that the specified string has a length within the given range (inclusive).
   *
   * @param value the string value to check
   * @param minLength the minimum allowed length (inclusive)
   * @param maxLength the maximum allowed length (inclusive)
   * @param messageSupplier supplier for the error message if the length is outside the range
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or length is outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minLength is greater than maxLength
   */
  public Validator hasLength(
      String value, int minLength, int maxLength, Supplier<String> messageSupplier) {
    if (minLength > maxLength) {
      throw new IllegalArgumentException("minLength cannot be greater than maxLength");
    }

    return assertFalse(
        value == null || value.length() < minLength || value.length() > maxLength, messageSupplier);
  }

  /**
   * Validates that the specified string has a length within the given range (inclusive).
   *
   * @param value the string value to check
   * @param minLength the minimum allowed length (inclusive)
   * @param maxLength the maximum allowed length (inclusive)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or length is outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minLength is greater than maxLength
   */
  public Validator hasLength(String value, int minLength, int maxLength) {
    return hasLength(value, minLength, maxLength, (String) null);
  }

  /**
   * Validates that the specified collection has a size within the given range (inclusive).
   *
   * @param value the collection to check
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or size is outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator hasSize(Collection<?> value, int minSize, int maxSize, String name) {
    return hasSize(
        value,
        minSize,
        maxSize,
        () ->
            formatMessage(
                value,
                name,
                String.format("must have size between %d and %d", minSize, maxSize),
                true));
  }

  /**
   * Validates that the specified collection has a size within the given range (inclusive).
   *
   * @param value the collection to check
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @param messageSupplier supplier for the error message if the size is outside the range
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or size is outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator hasSize(
      Collection<?> value, int minSize, int maxSize, Supplier<String> messageSupplier) {
    if (minSize > maxSize) {
      throw new IllegalArgumentException("minSize cannot be greater than maxSize");
    }

    return assertFalse(
        value == null || value.size() < minSize || value.size() > maxSize, messageSupplier);
  }

  /**
   * Validates that the specified collection has a size within the given range (inclusive).
   *
   * @param value the collection to check
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or size is outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator hasSize(Collection<?> value, int minSize, int maxSize) {
    return hasSize(value, minSize, maxSize, (String) null);
  }

  /**
   * Validates that the specified numeric value is positive (greater than zero).
   *
   * @param value the numeric value to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or not positive and fail-fast is
   *     enabled
   */
  public Validator isPositive(Number value, String name) {
    return isPositive(value, () -> formatMessage(value, name, "must be positive", true));
  }

  /**
   * Validates that the specified numeric value is positive (greater than zero).
   *
   * @param value the numeric value to check
   * @param messageSupplier supplier for the error message if the value is not positive
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or not positive and fail-fast is
   *     enabled
   */
  public Validator isPositive(Number value, Supplier<String> messageSupplier) {
    return assertFalse(value == null || value.doubleValue() <= 0, messageSupplier);
  }

  /**
   * Validates that the specified numeric value is positive (greater than zero).
   *
   * @param value the numeric value to check
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or not positive and fail-fast is
   *     enabled
   */
  public Validator isPositive(Number value) {
    return isPositive(value, (String) null);
  }

  /**
   * Validates that the specified numeric value is negative (less than zero).
   *
   * @param value the numeric value to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or not negative and fail-fast is
   *     enabled
   */
  public Validator isNegative(Number value, String name) {
    return isNegative(value, () -> formatMessage(value, name, "must be negative", true));
  }

  /**
   * Validates that the specified numeric value is negative (less than zero).
   *
   * @param value the numeric value to check
   * @param messageSupplier supplier for the error message if the value is not negative
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or not negative and fail-fast is
   *     enabled
   */
  public Validator isNegative(Number value, Supplier<String> messageSupplier) {
    return assertFalse(value == null || value.doubleValue() >= 0, messageSupplier);
  }

  /**
   * Validates that the specified numeric value is negative (less than zero).
   *
   * @param value the numeric value to check
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or not negative and fail-fast is
   *     enabled
   */
  public Validator isNegative(Number value) {
    return isNegative(value, (String) null);
  }

  /**
   * Validates that the specified string matches the given regular expression pattern.
   *
   * @param value the string value to check
   * @param regex the regular expression pattern to match against
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or doesn't match the pattern and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator matches(String value, String regex, String name) {
    return matches(
        value,
        regex,
        () -> formatMessage(value, name, String.format("must match pattern '%s'", regex), true));
  }

  /**
   * Validates that the specified string matches the given regular expression pattern.
   *
   * @param value the string value to check
   * @param regex the regular expression pattern to match against
   * @param messageSupplier supplier for the error message if the value doesn't match the pattern
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or doesn't match the pattern and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator matches(String value, String regex, Supplier<String> messageSupplier) {
    if (regex == null) {
      throw new IllegalArgumentException("regex pattern cannot be null");
    }

    return matches(
        value, PATTERN_CACHE.computeIfAbsent(regex, k -> Pattern.compile(regex)), messageSupplier);
  }

  /**
   * Validates that the specified string matches the given regular expression pattern.
   *
   * @param value the string value to check
   * @param regex the regular expression pattern to match against
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or doesn't match the pattern and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator matches(String value, String regex) {
    return matches(value, regex, (String) null);
  }

  /**
   * Validates that the specified string matches the given compiled regular expression pattern.
   *
   * @param value the string value to check
   * @param regex the compiled regular expression pattern to match against
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or doesn't match the pattern and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator matches(String value, Pattern regex, String name) {
    return matches(
        value,
        regex,
        () ->
            formatMessage(
                value, name, String.format("must match pattern '%s'", regex.pattern()), true));
  }

  /**
   * Validates that the specified string matches the given compiled regular expression pattern.
   *
   * @param value the string value to check
   * @param regex the compiled regular expression pattern to match against
   * @param messageSupplier supplier for the error message if the value doesn't match the pattern
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or doesn't match the pattern and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator matches(String value, Pattern regex, Supplier<String> messageSupplier) {
    if (regex == null) {
      throw new IllegalArgumentException("regex pattern cannot be null");
    }
    return assertFalse(value == null || !regex.matcher(value).matches(), messageSupplier);
  }

  /**
   * Validates that the specified string matches the given compiled regular expression pattern.
   *
   * @param value the string value to check
   * @param regex the compiled regular expression pattern to match against
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or doesn't match the pattern and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator matches(String value, Pattern regex) {
    return matches(value, regex, (String) null);
  }
}
