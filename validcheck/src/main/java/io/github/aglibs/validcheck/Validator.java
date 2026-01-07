package io.github.aglibs.validcheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
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
 * <h2>Customization</h2>
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
 * @since 1.0.0
 * @see ValidCheck#require()
 * @see ValidCheck#check()
 * @see BatchValidator
 * @see ValidationException
 */
public class Validator {

  private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();
  private static final int MAX_DISPLAYED_VALUE_LENGTH = 100;

  /** Whether to include actual values in error messages. */
  protected final boolean includeValues;

  /** Whether to throw immediately on first validation failure. */
  protected final boolean failFast;

  /** List of collected validation errors. */
  protected final List<String> errors;

  private final BiFunction<String, List<String>, RuntimeException> exceptionFactory;

  /**
   * Constructs a new Validator with the specified configuration.
   *
   * @param includeValues whether to include actual values in error messages for debugging
   * @param failFast whether to throw immediately on first validation failure or collect all errors
   * @param exceptionFactory factory that creates the desired validation exception.
   */
  protected Validator(
      boolean includeValues,
      boolean failFast,
      BiFunction<String, List<String>, RuntimeException> exceptionFactory) {
    this.includeValues = includeValues;
    this.failFast = failFast;
    this.exceptionFactory = Objects.requireNonNull(exceptionFactory);

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

  /** Throws ValidationException if any errors have been collected. */
  protected void validate() {
    if (!errors.isEmpty()) {
      final var errorMessage = String.join("; ", errors);
      throw exceptionFactory.apply(errorMessage, errors);
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
   * Validates that the specified numeric value is greater than or equal to the minimum value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param minValue the minimum allowed value (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or less than minValue and fail-fast is
   *     enabled
   * @throws IllegalArgumentException if minValue is null
   */
  public <T extends Number> Validator min(T value, T minValue, String name) {
    return min(
        value,
        minValue,
        () -> formatMessage(value, name, String.format("must be at least %s", minValue), true));
  }

  /**
   * Validates that the specified numeric value is greater than or equal to the minimum value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param minValue the minimum allowed value (inclusive)
   * @param messageSupplier supplier for the error message if the value is less than minValue
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or less than minValue and fail-fast is
   *     enabled
   * @throws IllegalArgumentException if minValue is null
   */
  public <T extends Number> Validator min(T value, T minValue, Supplier<String> messageSupplier) {
    if (minValue == null) {
      throw new IllegalArgumentException("minValue cannot be null");
    }

    return assertFalse(
        value == null || value.doubleValue() < minValue.doubleValue(), messageSupplier);
  }

  /**
   * Validates that the specified numeric value is greater than or equal to the minimum value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param minValue the minimum allowed value (inclusive)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or less than minValue and fail-fast is
   *     enabled
   * @throws IllegalArgumentException if minValue is null
   */
  public <T extends Number> Validator min(T value, T minValue) {
    return min(value, minValue, (String) null);
  }

  /**
   * Validates that the specified numeric value is less than or equal to the maximum value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param maxValue the maximum allowed value (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or greater than maxValue and fail-fast
   *     is enabled
   * @throws IllegalArgumentException if maxValue is null
   */
  public <T extends Number> Validator max(T value, T maxValue, String name) {
    return max(
        value,
        maxValue,
        () -> formatMessage(value, name, String.format("must be at most %s", maxValue), true));
  }

  /**
   * Validates that the specified numeric value is less than or equal to the maximum value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param maxValue the maximum allowed value (inclusive)
   * @param messageSupplier supplier for the error message if the value is greater than maxValue
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or greater than maxValue and fail-fast
   *     is enabled
   * @throws IllegalArgumentException if maxValue is null
   */
  public <T extends Number> Validator max(T value, T maxValue, Supplier<String> messageSupplier) {
    if (maxValue == null) {
      throw new IllegalArgumentException("maxValue cannot be null");
    }

    return assertFalse(
        value == null || value.doubleValue() > maxValue.doubleValue(), messageSupplier);
  }

  /**
   * Validates that the specified numeric value is less than or equal to the maximum value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param maxValue the maximum allowed value (inclusive)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or greater than maxValue and fail-fast
   *     is enabled
   * @throws IllegalArgumentException if maxValue is null
   */
  public <T extends Number> Validator max(T value, T maxValue) {
    return max(value, maxValue, (String) null);
  }

  /**
   * Validates that the specified string is not null and not empty.
   *
   * @param value the string value to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(String value, String name) {
    return notEmpty(value, () -> formatMessage(value, name, "must not be null or empty", false));
  }

  /**
   * Validates that the specified string is not null and not empty.
   *
   * @param value the string value to check
   * @param messageSupplier supplier for the error message if the value is null or empty
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(String value, Supplier<String> messageSupplier) {
    return assertFalse(value == null || value.isEmpty(), messageSupplier);
  }

  /**
   * Validates that the specified string is not null and not empty.
   *
   * @param value the string value to check
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(String value) {
    return notEmpty(value, (String) null);
  }

  /**
   * Validates that the specified collection is not null and not empty.
   *
   * @param value the collection to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(Collection<?> value, String name) {
    return notEmpty(value, () -> formatMessage(value, name, "must not be null or empty", false));
  }

  /**
   * Validates that the specified collection is not null and not empty.
   *
   * @param value the collection to check
   * @param messageSupplier supplier for the error message if the value is null or empty
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(Collection<?> value, Supplier<String> messageSupplier) {
    return assertFalse(value == null || value.isEmpty(), messageSupplier);
  }

  /**
   * Validates that the specified collection is not null and not empty.
   *
   * @param value the collection to check
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(Collection<?> value) {
    return notEmpty(value, (String) null);
  }

  /**
   * Validates that the specified map is not null and not empty.
   *
   * @param value the map to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(Map<?, ?> value, String name) {
    return notEmpty(value, () -> formatMessage(value, name, "must not be null or empty", false));
  }

  /**
   * Validates that the specified map is not null and not empty.
   *
   * @param value the map to check
   * @param messageSupplier supplier for the error message if the value is null or empty
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(Map<?, ?> value, Supplier<String> messageSupplier) {
    return assertFalse(value == null || value.isEmpty(), messageSupplier);
  }

  /**
   * Validates that the specified map is not null and not empty.
   *
   * @param value the map to check
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(Map<?, ?> value) {
    return notEmpty(value, (String) null);
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
   * Validates that the specified map has a size within the given range (inclusive).
   *
   * @param value the map to check
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or size is outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator hasSize(Map<?, ?> value, int minSize, int maxSize, String name) {
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
   * Validates that the specified map has a size within the given range (inclusive).
   *
   * @param value the map to check
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @param messageSupplier supplier for the error message if the size is outside the range
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or size is outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator hasSize(
      Map<?, ?> value, int minSize, int maxSize, Supplier<String> messageSupplier) {
    if (minSize > maxSize) {
      throw new IllegalArgumentException("minSize cannot be greater than maxSize");
    }

    return assertFalse(
        value == null || value.size() < minSize || value.size() > maxSize, messageSupplier);
  }

  /**
   * Validates that the specified map has a size within the given range (inclusive).
   *
   * @param value the map to check
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or size is outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator hasSize(Map<?, ?> value, int minSize, int maxSize) {
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
   * Validates that the specified numeric value is non-negative (greater than or equal to zero).
   *
   * @param value the numeric value to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or negative and fail-fast is enabled
   */
  public Validator isNonNegative(Number value, String name) {
    return isNonNegative(value, () -> formatMessage(value, name, "must be non-negative", true));
  }

  /**
   * Validates that the specified numeric value is non-negative (greater than or equal to zero).
   *
   * @param value the numeric value to check
   * @param messageSupplier supplier for the error message if the value is negative
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or negative and fail-fast is enabled
   */
  public Validator isNonNegative(Number value, Supplier<String> messageSupplier) {
    return assertFalse(value == null || value.doubleValue() < 0, messageSupplier);
  }

  /**
   * Validates that the specified numeric value is non-negative (greater than or equal to zero).
   *
   * @param value the numeric value to check
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or negative and fail-fast is enabled
   */
  public Validator isNonNegative(Number value) {
    return isNonNegative(value, (String) null);
  }

  /**
   * Validates that the specified numeric value is non-positive (less than or equal to zero).
   *
   * @param value the numeric value to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or positive and fail-fast is enabled
   */
  public Validator isNonPositive(Number value, String name) {
    return isNonPositive(value, () -> formatMessage(value, name, "must be non-positive", true));
  }

  /**
   * Validates that the specified numeric value is non-positive (less than or equal to zero).
   *
   * @param value the numeric value to check
   * @param messageSupplier supplier for the error message if the value is positive
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or positive and fail-fast is enabled
   */
  public Validator isNonPositive(Number value, Supplier<String> messageSupplier) {
    return assertFalse(value == null || value.doubleValue() > 0, messageSupplier);
  }

  /**
   * Validates that the specified numeric value is non-positive (less than or equal to zero).
   *
   * @param value the numeric value to check
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is null or positive and fail-fast is enabled
   */
  public Validator isNonPositive(Number value) {
    return isNonPositive(value, (String) null);
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

  /**
   * Validates that the specified numeric value is null or within the given range (inclusive). This
   * method passes validation if the value is null OR if it's within the specified range.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check (can be null)
   * @param min the minimum allowed value (inclusive)
   * @param max the maximum allowed value (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if min or max are null
   */
  public <T extends Number> Validator nullOrInRange(T value, T min, T max, String name) {
    return nullOrInRange(
        value,
        min,
        max,
        () ->
            formatMessage(
                value, name, String.format("must be null or between %s and %s", min, max), true));
  }

  /**
   * Validates that the specified numeric value is null or within the given range (inclusive). This
   * method passes validation if the value is null OR if it's within the specified range.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check (can be null)
   * @param min the minimum allowed value (inclusive)
   * @param max the maximum allowed value (inclusive)
   * @param messageSupplier supplier for the error message if the value is not null and outside the
   *     range
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if min or max are null
   */
  public <T extends Number> Validator nullOrInRange(
      T value, T min, T max, Supplier<String> messageSupplier) {
    if (min == null || max == null) {
      throw new IllegalArgumentException("min and max cannot be null");
    }

    return assertFalse(
        value != null
            && (value.doubleValue() < min.doubleValue() || value.doubleValue() > max.doubleValue()),
        messageSupplier);
  }

  /**
   * Validates that the specified numeric value is null or within the given range (inclusive). This
   * method passes validation if the value is null OR if it's within the specified range.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check (can be null)
   * @param min the minimum allowed value (inclusive)
   * @param max the maximum allowed value (inclusive)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if min or max are null
   */
  public <T extends Number> Validator nullOrInRange(T value, T min, T max) {
    return nullOrInRange(value, min, max, (String) null);
  }

  /**
   * Validates that the specified string is null or not empty. This method passes validation if the
   * value is null OR if it's not empty.
   *
   * @param value the string value to check (can be null)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(String value, String name) {
    return nullOrNotEmpty(
        value, () -> formatMessage(value, name, "must be null or not empty", false));
  }

  /**
   * Validates that the specified string is null or not empty. This method passes validation if the
   * value is null OR if it's not empty.
   *
   * @param value the string value to check (can be null)
   * @param messageSupplier supplier for the error message if the value is not null and empty
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(String value, Supplier<String> messageSupplier) {
    return assertFalse(value != null && value.isEmpty(), messageSupplier);
  }

  /**
   * Validates that the specified string is null or not empty. This method passes validation if the
   * value is null OR if it's not empty.
   *
   * @param value the string value to check (can be null)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(String value) {
    return nullOrNotEmpty(value, (String) null);
  }

  /**
   * Validates that the specified collection is null or not empty. This method passes validation if
   * the value is null OR if it's not empty.
   *
   * @param value the collection to check (can be null)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(Collection<?> value, String name) {
    return nullOrNotEmpty(
        value, () -> formatMessage(value, name, "must be null or not empty", false));
  }

  /**
   * Validates that the specified collection is null or not empty. This method passes validation if
   * the value is null OR if it's not empty.
   *
   * @param value the collection to check (can be null)
   * @param messageSupplier supplier for the error message if the value is not null and empty
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(Collection<?> value, Supplier<String> messageSupplier) {
    return assertFalse(value != null && value.isEmpty(), messageSupplier);
  }

  /**
   * Validates that the specified collection is null or not empty. This method passes validation if
   * the value is null OR if it's not empty.
   *
   * @param value the collection to check (can be null)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(Collection<?> value) {
    return nullOrNotEmpty(value, (String) null);
  }

  /**
   * Validates that the specified map is null or not empty. This method passes validation if the
   * value is null OR if it's not empty.
   *
   * @param value the map to check (can be null)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(Map<?, ?> value, String name) {
    return nullOrNotEmpty(
        value, () -> formatMessage(value, name, "must be null or not empty", false));
  }

  /**
   * Validates that the specified map is null or not empty. This method passes validation if the
   * value is null OR if it's not empty.
   *
   * @param value the map to check (can be null)
   * @param messageSupplier supplier for the error message if the value is not null and empty
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(Map<?, ?> value, Supplier<String> messageSupplier) {
    return assertFalse(value != null && value.isEmpty(), messageSupplier);
  }

  /**
   * Validates that the specified map is null or not empty. This method passes validation if the
   * value is null OR if it's not empty.
   *
   * @param value the map to check (can be null)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(Map<?, ?> value) {
    return nullOrNotEmpty(value, (String) null);
  }

  /**
   * Validates that the specified string is null or not blank (contains non-whitespace characters).
   * This method passes validation if the value is null OR if it's not blank.
   *
   * @param value the string value to check (can be null)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and blank and fail-fast is enabled
   */
  public Validator nullOrNotBlank(String value, String name) {
    return nullOrNotBlank(
        value, () -> formatMessage(value, name, "must be null or not blank", false));
  }

  /**
   * Validates that the specified string is null or not blank (contains non-whitespace characters).
   * This method passes validation if the value is null OR if it's not blank.
   *
   * @param value the string value to check (can be null)
   * @param messageSupplier supplier for the error message if the value is not null and blank
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and blank and fail-fast is enabled
   */
  public Validator nullOrNotBlank(String value, Supplier<String> messageSupplier) {
    return assertFalse(value != null && value.trim().isEmpty(), messageSupplier);
  }

  /**
   * Validates that the specified string is null or not blank (contains non-whitespace characters).
   * This method passes validation if the value is null OR if it's not blank.
   *
   * @param value the string value to check (can be null)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and blank and fail-fast is enabled
   */
  public Validator nullOrNotBlank(String value) {
    return nullOrNotBlank(value, (String) null);
  }

  /**
   * Validates that the specified string is null or has a length within the given range (inclusive).
   * This method passes validation if the value is null OR if its length is within the specified
   * range.
   *
   * @param value the string value to check (can be null)
   * @param minLength the minimum allowed length (inclusive)
   * @param maxLength the maximum allowed length (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and length is outside the range
   *     and fail-fast is enabled
   * @throws IllegalArgumentException if minLength is greater than maxLength
   */
  public Validator nullOrHasLength(String value, int minLength, int maxLength, String name) {
    return nullOrHasLength(
        value,
        minLength,
        maxLength,
        () ->
            formatMessage(
                value,
                name,
                String.format(
                    "must be null or have length between %d and %d", minLength, maxLength),
                true));
  }

  /**
   * Validates that the specified string is null or has a length within the given range (inclusive).
   * This method passes validation if the value is null OR if its length is within the specified
   * range.
   *
   * @param value the string value to check (can be null)
   * @param minLength the minimum allowed length (inclusive)
   * @param maxLength the maximum allowed length (inclusive)
   * @param messageSupplier supplier for the error message if the length is outside the range
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and length is outside the range
   *     and fail-fast is enabled
   * @throws IllegalArgumentException if minLength is greater than maxLength
   */
  public Validator nullOrHasLength(
      String value, int minLength, int maxLength, Supplier<String> messageSupplier) {
    if (minLength > maxLength) {
      throw new IllegalArgumentException("minLength cannot be greater than maxLength");
    }

    return assertFalse(
        value != null && (value.length() < minLength || value.length() > maxLength),
        messageSupplier);
  }

  /**
   * Validates that the specified string is null or has a length within the given range (inclusive).
   * This method passes validation if the value is null OR if its length is within the specified
   * range.
   *
   * @param value the string value to check (can be null)
   * @param minLength the minimum allowed length (inclusive)
   * @param maxLength the maximum allowed length (inclusive)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and length is outside the range
   *     and fail-fast is enabled
   * @throws IllegalArgumentException if minLength is greater than maxLength
   */
  public Validator nullOrHasLength(String value, int minLength, int maxLength) {
    return nullOrHasLength(value, minLength, maxLength, (String) null);
  }

  /**
   * Validates that the specified collection is null or has a size within the given range
   * (inclusive). This method passes validation if the value is null OR if its size is within the
   * specified range.
   *
   * @param value the collection to check (can be null)
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and size is outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator nullOrHasSize(Collection<?> value, int minSize, int maxSize, String name) {
    return nullOrHasSize(
        value,
        minSize,
        maxSize,
        () ->
            formatMessage(
                value,
                name,
                String.format("must be null or have size between %d and %d", minSize, maxSize),
                true));
  }

  /**
   * Validates that the specified collection is null or has a size within the given range
   * (inclusive). This method passes validation if the value is null OR if its size is within the
   * specified range.
   *
   * @param value the collection to check (can be null)
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @param messageSupplier supplier for the error message if the size is outside the range
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and size is outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator nullOrHasSize(
      Collection<?> value, int minSize, int maxSize, Supplier<String> messageSupplier) {
    if (minSize > maxSize) {
      throw new IllegalArgumentException("minSize cannot be greater than maxSize");
    }

    return assertFalse(
        value != null && (value.size() < minSize || value.size() > maxSize), messageSupplier);
  }

  /**
   * Validates that the specified collection is null or has a size within the given range
   * (inclusive). This method passes validation if the value is null OR if its size is within the
   * specified range.
   *
   * @param value the collection to check (can be null)
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and size is outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator nullOrHasSize(Collection<?> value, int minSize, int maxSize) {
    return nullOrHasSize(value, minSize, maxSize, (String) null);
  }

  /**
   * Validates that the specified map is null or has a size within the given range (inclusive). This
   * method passes validation if the value is null OR if its size is within the specified range.
   *
   * @param value the map to check (can be null)
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and size is outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator nullOrHasSize(Map<?, ?> value, int minSize, int maxSize, String name) {
    return nullOrHasSize(
        value,
        minSize,
        maxSize,
        () ->
            formatMessage(
                value,
                name,
                String.format("must be null or have size between %d and %d", minSize, maxSize),
                true));
  }

  /**
   * Validates that the specified map is null or has a size within the given range (inclusive). This
   * method passes validation if the value is null OR if its size is within the specified range.
   *
   * @param value the map to check (can be null)
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @param messageSupplier supplier for the error message if the size is outside the range
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and size is outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator nullOrHasSize(
      Map<?, ?> value, int minSize, int maxSize, Supplier<String> messageSupplier) {
    if (minSize > maxSize) {
      throw new IllegalArgumentException("minSize cannot be greater than maxSize");
    }

    return assertFalse(
        value != null && (value.size() < minSize || value.size() > maxSize), messageSupplier);
  }

  /**
   * Validates that the specified map is null or has a size within the given range (inclusive). This
   * method passes validation if the value is null OR if its size is within the specified range.
   *
   * @param value the map to check (can be null)
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and size is outside the range and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator nullOrHasSize(Map<?, ?> value, int minSize, int maxSize) {
    return nullOrHasSize(value, minSize, maxSize, (String) null);
  }

  /**
   * Validates that the specified numeric value is null or positive (greater than zero). This method
   * passes validation if the value is null OR if it's positive.
   *
   * @param value the numeric value to check (can be null)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and not positive and fail-fast is
   *     enabled
   */
  public Validator nullOrIsPositive(Number value, String name) {
    return nullOrIsPositive(
        value, () -> formatMessage(value, name, "must be null or positive", true));
  }

  /**
   * Validates that the specified numeric value is null or positive (greater than zero). This method
   * passes validation if the value is null OR if it's positive.
   *
   * @param value the numeric value to check (can be null)
   * @param messageSupplier supplier for the error message if the value is not null and not positive
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and not positive and fail-fast is
   *     enabled
   */
  public Validator nullOrIsPositive(Number value, Supplier<String> messageSupplier) {
    return assertFalse(value != null && value.doubleValue() <= 0, messageSupplier);
  }

  /**
   * Validates that the specified numeric value is null or positive (greater than zero). This method
   * passes validation if the value is null OR if it's positive.
   *
   * @param value the numeric value to check (can be null)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and not positive and fail-fast is
   *     enabled
   */
  public Validator nullOrIsPositive(Number value) {
    return nullOrIsPositive(value, (String) null);
  }

  /**
   * Validates that the specified numeric value is null or negative (less than zero). This method
   * passes validation if the value is null OR if it's negative.
   *
   * @param value the numeric value to check (can be null)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and not negative and fail-fast is
   *     enabled
   */
  public Validator nullOrIsNegative(Number value, String name) {
    return nullOrIsNegative(
        value, () -> formatMessage(value, name, "must be null or negative", true));
  }

  /**
   * Validates that the specified numeric value is null or negative (less than zero). This method
   * passes validation if the value is null OR if it's negative.
   *
   * @param value the numeric value to check (can be null)
   * @param messageSupplier supplier for the error message if the value is not null and not negative
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and not negative and fail-fast is
   *     enabled
   */
  public Validator nullOrIsNegative(Number value, Supplier<String> messageSupplier) {
    return assertFalse(value != null && value.doubleValue() >= 0, messageSupplier);
  }

  /**
   * Validates that the specified numeric value is null or negative (less than zero). This method
   * passes validation if the value is null OR if it's negative.
   *
   * @param value the numeric value to check (can be null)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and not negative and fail-fast is
   *     enabled
   */
  public Validator nullOrIsNegative(Number value) {
    return nullOrIsNegative(value, (String) null);
  }

  /**
   * Validates that the specified string is null or matches the given regular expression pattern.
   * This method passes validation if the value is null OR if it matches the pattern.
   *
   * @param value the string value to check (can be null)
   * @param regex the regular expression pattern to match against
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and doesn't match the pattern and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator nullOrMatches(String value, String regex, String name) {
    return nullOrMatches(
        value,
        regex,
        () ->
            formatMessage(
                value, name, String.format("must be null or match pattern '%s'", regex), true));
  }

  /**
   * Validates that the specified string is null or matches the given regular expression pattern.
   * This method passes validation if the value is null OR if it matches the pattern.
   *
   * @param value the string value to check (can be null)
   * @param regex the regular expression pattern to match against
   * @param messageSupplier supplier for the error message if the value doesn't match the pattern
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and doesn't match the pattern and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator nullOrMatches(String value, String regex, Supplier<String> messageSupplier) {
    if (regex == null) {
      throw new IllegalArgumentException("regex pattern cannot be null");
    }

    return nullOrMatches(
        value, PATTERN_CACHE.computeIfAbsent(regex, k -> Pattern.compile(regex)), messageSupplier);
  }

  /**
   * Validates that the specified string is null or matches the given regular expression pattern.
   * This method passes validation if the value is null OR if it matches the pattern.
   *
   * @param value the string value to check (can be null)
   * @param regex the regular expression pattern to match against
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and doesn't match the pattern and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator nullOrMatches(String value, String regex) {
    return nullOrMatches(value, regex, (String) null);
  }

  /**
   * Validates that the specified string is null or matches the given compiled regular expression
   * pattern. This method passes validation if the value is null OR if it matches the pattern.
   *
   * @param value the string value to check (can be null)
   * @param regex the compiled regular expression pattern to match against
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and doesn't match the pattern and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator nullOrMatches(String value, Pattern regex, String name) {
    return nullOrMatches(
        value,
        regex,
        () ->
            formatMessage(
                value,
                name,
                String.format("must be null or match pattern '%s'", regex.pattern()),
                true));
  }

  /**
   * Validates that the specified string is null or matches the given compiled regular expression
   * pattern. This method passes validation if the value is null OR if it matches the pattern.
   *
   * @param value the string value to check (can be null)
   * @param regex the compiled regular expression pattern to match against
   * @param messageSupplier supplier for the error message if the value doesn't match the pattern
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and doesn't match the pattern and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator nullOrMatches(String value, Pattern regex, Supplier<String> messageSupplier) {
    if (regex == null) {
      throw new IllegalArgumentException("regex pattern cannot be null");
    }
    return assertFalse(value != null && !regex.matcher(value).matches(), messageSupplier);
  }

  /**
   * Validates that the specified string is null or matches the given compiled regular expression
   * pattern. This method passes validation if the value is null OR if it matches the pattern.
   *
   * @param value the string value to check (can be null)
   * @param regex the compiled regular expression pattern to match against
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and doesn't match the pattern and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator nullOrMatches(String value, Pattern regex) {
    return nullOrMatches(value, regex, (String) null);
  }

  /**
   * Validates that the specified value is null.
   *
   * @param value the value to check for null
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and fail-fast is enabled
   */
  public Validator isNull(Object value, String name) {
    return isNull(value, () -> formatMessage(value, name, "must be null", true));
  }

  /**
   * Validates that the specified value is null.
   *
   * @param value the value to check for null
   * @param messageSupplier supplier for the error message if the value is not null
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and fail-fast is enabled
   */
  public Validator isNull(Object value, Supplier<String> messageSupplier) {
    return assertTrue(value == null, messageSupplier);
  }

  /**
   * Validates that the specified value is null.
   *
   * @param value the value to check for null
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and fail-fast is enabled
   */
  public Validator isNull(Object value) {
    return isNull(value, (String) null);
  }

  /**
   * Validates that the specified numeric value is null or non-negative (greater than or equal to
   * zero). This method passes validation if the value is null OR if it's non-negative.
   *
   * @param value the numeric value to check (can be null)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and negative and fail-fast is
   *     enabled
   */
  public Validator nullOrIsNonNegative(Number value, String name) {
    return nullOrIsNonNegative(
        value, () -> formatMessage(value, name, "must be null or non-negative", true));
  }

  /**
   * Validates that the specified numeric value is null or non-negative (greater than or equal to
   * zero). This method passes validation if the value is null OR if it's non-negative.
   *
   * @param value the numeric value to check (can be null)
   * @param messageSupplier supplier for the error message if the value is not null and negative
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and negative and fail-fast is
   *     enabled
   */
  public Validator nullOrIsNonNegative(Number value, Supplier<String> messageSupplier) {
    return assertFalse(value != null && value.doubleValue() < 0, messageSupplier);
  }

  /**
   * Validates that the specified numeric value is null or non-negative (greater than or equal to
   * zero). This method passes validation if the value is null OR if it's non-negative.
   *
   * @param value the numeric value to check (can be null)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and negative and fail-fast is
   *     enabled
   */
  public Validator nullOrIsNonNegative(Number value) {
    return nullOrIsNonNegative(value, (String) null);
  }

  /**
   * Validates that the specified numeric value is null or non-positive (less than or equal to
   * zero). This method passes validation if the value is null OR if it's non-positive.
   *
   * @param value the numeric value to check (can be null)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and positive and fail-fast is
   *     enabled
   */
  public Validator nullOrIsNonPositive(Number value, String name) {
    return nullOrIsNonPositive(
        value, () -> formatMessage(value, name, "must be null or non-positive", true));
  }

  /**
   * Validates that the specified numeric value is null or non-positive (less than or equal to
   * zero). This method passes validation if the value is null OR if it's non-positive.
   *
   * @param value the numeric value to check (can be null)
   * @param messageSupplier supplier for the error message if the value is not null and positive
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and positive and fail-fast is
   *     enabled
   */
  public Validator nullOrIsNonPositive(Number value, Supplier<String> messageSupplier) {
    return assertFalse(value != null && value.doubleValue() > 0, messageSupplier);
  }

  /**
   * Validates that the specified numeric value is null or non-positive (less than or equal to
   * zero). This method passes validation if the value is null OR if it's non-positive.
   *
   * @param value the numeric value to check (can be null)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and positive and fail-fast is
   *     enabled
   */
  public Validator nullOrIsNonPositive(Number value) {
    return nullOrIsNonPositive(value, (String) null);
  }

  /**
   * Validates that the specified numeric value is null or greater than or equal to the minimum
   * value. This method passes validation if the value is null OR if it's at least the minimum
   * value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check (can be null)
   * @param minValue the minimum allowed value (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and less than minValue and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minValue is null
   */
  public <T extends Number> Validator nullOrMin(T value, T minValue, String name) {
    return nullOrMin(
        value,
        minValue,
        () ->
            formatMessage(
                value, name, String.format("must be null or at least %s", minValue), true));
  }

  /**
   * Validates that the specified numeric value is null or greater than or equal to the minimum
   * value. This method passes validation if the value is null OR if it's at least the minimum
   * value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check (can be null)
   * @param minValue the minimum allowed value (inclusive)
   * @param messageSupplier supplier for the error message if the value is not null and less than
   *     minValue
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and less than minValue and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minValue is null
   */
  public <T extends Number> Validator nullOrMin(
      T value, T minValue, Supplier<String> messageSupplier) {
    if (minValue == null) {
      throw new IllegalArgumentException("minValue cannot be null");
    }

    return assertFalse(
        value != null && value.doubleValue() < minValue.doubleValue(), messageSupplier);
  }

  /**
   * Validates that the specified numeric value is null or greater than or equal to the minimum
   * value. This method passes validation if the value is null OR if it's at least the minimum
   * value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check (can be null)
   * @param minValue the minimum allowed value (inclusive)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and less than minValue and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if minValue is null
   */
  public <T extends Number> Validator nullOrMin(T value, T minValue) {
    return nullOrMin(value, minValue, (String) null);
  }

  /**
   * Validates that the specified numeric value is null or less than or equal to the maximum value.
   * This method passes validation if the value is null OR if it's at most the maximum value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check (can be null)
   * @param maxValue the maximum allowed value (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and greater than maxValue and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if maxValue is null
   */
  public <T extends Number> Validator nullOrMax(T value, T maxValue, String name) {
    return nullOrMax(
        value,
        maxValue,
        () ->
            formatMessage(
                value, name, String.format("must be null or at most %s", maxValue), true));
  }

  /**
   * Validates that the specified numeric value is null or less than or equal to the maximum value.
   * This method passes validation if the value is null OR if it's at most the maximum value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check (can be null)
   * @param maxValue the maximum allowed value (inclusive)
   * @param messageSupplier supplier for the error message if the value is not null and greater than
   *     maxValue
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and greater than maxValue and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if maxValue is null
   */
  public <T extends Number> Validator nullOrMax(
      T value, T maxValue, Supplier<String> messageSupplier) {
    if (maxValue == null) {
      throw new IllegalArgumentException("maxValue cannot be null");
    }

    return assertFalse(
        value != null && value.doubleValue() > maxValue.doubleValue(), messageSupplier);
  }

  /**
   * Validates that the specified numeric value is null or less than or equal to the maximum value.
   * This method passes validation if the value is null OR if it's at most the maximum value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check (can be null)
   * @param maxValue the maximum allowed value (inclusive)
   * @return this validator instance for method chaining
   * @throws ValidationException immediately if value is not null and greater than maxValue and
   *     fail-fast is enabled
   * @throws IllegalArgumentException if maxValue is null
   */
  public <T extends Number> Validator nullOrMax(T value, T maxValue) {
    return nullOrMax(value, maxValue, (String) null);
  }
}
