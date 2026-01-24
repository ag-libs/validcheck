package io.github.aglibs.validcheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
 *     // No values in errors, fail-fast, with stack traces, default exception
 *     return new MyValidator(false, true, true, null);
 *   }
 *
 *   public static MyValidator lenientValidation() {
 *     // Include values, batch errors, no stack traces, default exception
 *     return new MyValidator(true, false, false, null);
 *   }
 *
 *   protected MyValidator(boolean includeValues, boolean failFast,
 *                          boolean fillStackTrace,
 *                          Function<List<String>, RuntimeException> exceptionFactory) {
 *     super(includeValues, failFast, fillStackTrace, exceptionFactory);
 *   }
 *
 *   // Add custom validation methods
 *   public MyValidator isValidEmail(String email, String name) {
 *     return (MyValidator) matches(email, EMAIL_PATTERN, name);
 *   }
 * }
 * }</pre>
 *
 * <h2>Custom Exception Types</h2>
 *
 * <p>By default, all validation failures throw {@link ValidationException}. To throw custom
 * exception types, pass an exception factory function to the constructor:
 *
 * <pre>{@code
 * // Custom exception with default formatting ("; " separator)
 * Validator validator = new Validator(true, true, true,
 *     errors -> new IllegalArgumentException(String.join("; ", errors)));
 *
 * validator.notNull(null, "value"); // throws IllegalArgumentException
 *
 * // Custom formatting
 * Validator validator = new Validator(true, true, true,
 *     errors -> new MyException("Errors:\n- " + String.join("\n- ", errors)));
 * }</pre>
 *
 * <h2>Performance Optimization</h2>
 *
 * <p>For high-throughput validation where stack traces are not needed, disable stack trace
 * generation:
 *
 * <pre>{@code
 * // ValidationException without stack traces for better performance
 * Validator validator = new Validator(true, true, false, null);
 * validator.notNull(value, "field");
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

  // Error message constants
  private static final String MSG_NOT_NULL_OR_EMPTY = "must not be null or empty";
  private static final String MSG_NULL_OR_NOT_EMPTY = "must be null or not empty";

  /** Whether to include actual values in error messages. */
  protected final boolean includeValues;

  /** Whether to throw immediately on first validation failure. */
  protected final boolean failFast;

  /** Whether to fill stack traces in thrown exceptions (used for default ValidationException). */
  protected final boolean fillStackTrace;

  /** Factory for creating exceptions when validation fails. */
  protected final java.util.function.Function<List<ValidationError>, RuntimeException>
      exceptionFactory;

  /** List of collected validation errors. */
  protected final List<ValidationError> errors;

  /**
   * Constructs a new Validator with the specified configuration.
   *
   * @param includeValues whether to include actual values in error messages for debugging
   * @param failFast whether to throw immediately on first validation failure or collect all errors
   * @param fillStackTrace whether to fill stack traces in thrown exceptions (only applies when
   *     using default ValidationException; ignored if exceptionFactory is provided)
   * @param exceptionFactory factory function to create exceptions from error list; if null, uses
   *     default ValidationException
   */
  protected Validator(
      boolean includeValues,
      boolean failFast,
      boolean fillStackTrace,
      Function<List<ValidationError>, RuntimeException> exceptionFactory) {
    this.includeValues = includeValues;
    this.failFast = failFast;
    this.fillStackTrace = fillStackTrace;
    this.exceptionFactory = exceptionFactory;

    errors = new ArrayList<>();
  }

  private String valueToString(Object value) {
    var string = String.valueOf(value);
    if (string.length() <= MAX_DISPLAYED_VALUE_LENGTH) {
      return string;
    }

    return string.substring(0, MAX_DISPLAYED_VALUE_LENGTH - 3) + "...";
  }

  private ValidationError createError(Supplier<String> messageSupplier) {
    return new ValidationError(null, messageSupplier.get());
  }

  private ValidationError createError(
      Object value, String name, String message, boolean includeValue) {
    var error = name == null ? String.format("parameter %s", message) : message;

    if (this.includeValues && includeValue && value != null) {
      var stringValue = valueToString(value);
      var formattedValue =
          value instanceof String ? String.format("'%s'", stringValue) : stringValue;
      // limit the string length of string representation of the value
      return new ValidationError(name, error + String.format(", but it was %s", formattedValue));
    }

    return new ValidationError(name, error);
  }

  /** Throws ValidationException if any errors have been collected. */
  protected void validate() {
    if (!errors.isEmpty()) {
      throw createException();
    }
  }

  /**
   * Creates the exception to be thrown when validation fails. Uses the exception factory if
   * provided, otherwise creates a default ValidationException with the configured fillStackTrace
   * setting.
   *
   * <p>This method can be overridden in subclasses for additional exception customization beyond
   * the factory.
   *
   * @return the exception to throw, must not be null
   * @since 0.9.9
   */
  protected RuntimeException createException() {
    if (exceptionFactory != null) {
      return exceptionFactory.apply(Collections.unmodifiableList(errors));
    }
    final var errorMessage =
        errors.stream().map(ValidationError::toString).collect(Collectors.joining("; "));
    return ValidationException.create(fillStackTrace, errorMessage, errors);
  }

  // ========================================
  // Assertion Methods
  // ========================================

  /**
   * Validates that the specified condition is true.
   *
   * @param condition the condition to evaluate
   * @param message the error message to use if the condition is false
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     condition is false and fail-fast is enabled
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     condition is false and fail-fast is enabled
   */
  public Validator assertTrue(boolean condition, Supplier<String> messageSupplier) {
    return assertTrueInternal(condition, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified condition is false.
   *
   * @param condition the condition to evaluate
   * @param message the error message to use if the condition is true
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     condition is true and fail-fast is enabled
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     condition is true and fail-fast is enabled
   */
  public Validator assertFalse(boolean condition, Supplier<String> messageSupplier) {
    return assertTrue(!condition, messageSupplier);
  }

  private Validator assertTrueInternal(boolean condition, Supplier<ValidationError> errorSupplier) {
    if (!condition) {
      errors.add(errorSupplier.get());
      if (failFast) {
        validate();
      }
    }

    return this;
  }

  private Validator assertFalseInternal(
      boolean condition, Supplier<ValidationError> errorSupplier) {
    return assertTrueInternal(!condition, errorSupplier);
  }

  // ========================================
  // Null Checking Methods
  // ========================================

  /**
   * Validates that the specified value is not null.
   *
   * @param value the value to check for null
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null and fail-fast is enabled
   */
  public Validator notNull(Object value, String name) {
    return notNullInternal(value, () -> createError(value, name, "must not be null", true));
  }

  /**
   * Validates that the specified value is not null.
   *
   * @param value the value to check for null
   * @param messageSupplier supplier for the error message if the value is null
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null and fail-fast is enabled
   */
  public Validator notNull(Object value, Supplier<String> messageSupplier) {
    return notNullInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified value is not null.
   *
   * @param value the value to check for null
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null and fail-fast is enabled
   */
  public Validator notNull(Object value) {
    return notNull(value, (String) null);
  }

  private Validator notNullInternal(Object value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value == null, errorSupplier);
  }

  /**
   * Validates that the specified value is null.
   *
   * @param value the value to check for null
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and fail-fast is enabled
   */
  public Validator isNull(Object value, String name) {
    return isNullInternal(value, () -> createError(value, name, "must be null", true));
  }

  /**
   * Validates that the specified value is null.
   *
   * @param value the value to check for null
   * @param messageSupplier supplier for the error message if the value is not null
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and fail-fast is enabled
   */
  public Validator isNull(Object value, Supplier<String> messageSupplier) {
    return isNullInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified value is null.
   *
   * @param value the value to check for null
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and fail-fast is enabled
   */
  public Validator isNull(Object value) {
    return isNull(value, (String) null);
  }

  private Validator isNullInternal(Object value, Supplier<ValidationError> errorSupplier) {
    return assertTrueInternal(value == null, errorSupplier);
  }

  // ========================================
  // Range Validation Methods
  // ========================================

  /**
   * Validates that the specified numeric value is within the given range (inclusive).
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param min the minimum allowed value (inclusive)
   * @param max the maximum allowed value (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if min or max are null
   */
  public <T extends Number> Validator inRange(T value, T min, T max, String name) {
    return inRangeInternal(
        value,
        min,
        max,
        () -> createError(value, name, String.format("must be between %s and %s", min, max), true));
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if min or max are null
   */
  public <T extends Number> Validator inRange(
      T value, T min, T max, Supplier<String> messageSupplier) {
    if (min == null || max == null) {
      throw new IllegalArgumentException("min and max cannot be null");
    }

    return inRangeInternal(value, min, max, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified numeric value is within the given range (inclusive).
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param min the minimum allowed value (inclusive)
   * @param max the maximum allowed value (inclusive)
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if min or max are null
   */
  public <T extends Number> Validator inRange(T value, T min, T max) {
    return inRange(value, min, max, (String) null);
  }

  private <T extends Number> Validator inRangeInternal(
      T value, T min, T max, Supplier<ValidationError> errorSupplier) {
    if (min == null || max == null) {
      throw new IllegalArgumentException("min and max cannot be null");
    }

    return assertFalseInternal(
        value == null
            || (value.doubleValue() < min.doubleValue() || value.doubleValue() > max.doubleValue()),
        errorSupplier);
  }

  /**
   * Validates that the specified numeric value is greater than or equal to the minimum value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param minValue the minimum allowed value (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or less than minValue and fail-fast is enabled
   * @throws IllegalArgumentException if minValue is null
   */
  public <T extends Number> Validator min(T value, T minValue, String name) {
    return minInternal(
        value,
        minValue,
        () -> createError(value, name, String.format("must be at least %s", minValue), true));
  }

  /**
   * Validates that the specified numeric value is greater than or equal to the minimum value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param minValue the minimum allowed value (inclusive)
   * @param messageSupplier supplier for the error message if the value is less than minValue
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or less than minValue and fail-fast is enabled
   * @throws IllegalArgumentException if minValue is null
   */
  public <T extends Number> Validator min(T value, T minValue, Supplier<String> messageSupplier) {
    return minInternal(value, minValue, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified numeric value is greater than or equal to the minimum value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param minValue the minimum allowed value (inclusive)
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or less than minValue and fail-fast is enabled
   * @throws IllegalArgumentException if minValue is null
   */
  public <T extends Number> Validator min(T value, T minValue) {
    return min(value, minValue, (String) null);
  }

  private <T extends Number> Validator minInternal(
      T value, T minValue, Supplier<ValidationError> errorSupplier) {
    if (minValue == null) {
      throw new IllegalArgumentException("minValue cannot be null");
    }

    return assertFalseInternal(
        value == null || value.doubleValue() < minValue.doubleValue(), errorSupplier);
  }

  /**
   * Validates that the specified numeric value is less than or equal to the maximum value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param maxValue the maximum allowed value (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or greater than maxValue and fail-fast is enabled
   * @throws IllegalArgumentException if maxValue is null
   */
  public <T extends Number> Validator max(T value, T maxValue, String name) {
    return maxInternal(
        value,
        maxValue,
        () -> createError(value, name, String.format("must be at most %s", maxValue), true));
  }

  /**
   * Validates that the specified numeric value is less than or equal to the maximum value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param maxValue the maximum allowed value (inclusive)
   * @param messageSupplier supplier for the error message if the value is greater than maxValue
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or greater than maxValue and fail-fast is enabled
   * @throws IllegalArgumentException if maxValue is null
   */
  public <T extends Number> Validator max(T value, T maxValue, Supplier<String> messageSupplier) {
    return maxInternal(value, maxValue, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified numeric value is less than or equal to the maximum value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check
   * @param maxValue the maximum allowed value (inclusive)
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or greater than maxValue and fail-fast is enabled
   * @throws IllegalArgumentException if maxValue is null
   */
  public <T extends Number> Validator max(T value, T maxValue) {
    return max(value, maxValue, (String) null);
  }

  private <T extends Number> Validator maxInternal(
      T value, T maxValue, Supplier<ValidationError> errorSupplier) {
    if (maxValue == null) {
      throw new IllegalArgumentException("maxValue cannot be null");
    }

    return assertFalseInternal(
        value == null || value.doubleValue() > maxValue.doubleValue(), errorSupplier);
  }

  // ========================================
  // Sign Validation Methods
  // ========================================

  /**
   * Validates that the specified numeric value is positive (greater than zero).
   *
   * @param value the numeric value to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or not positive and fail-fast is enabled
   */
  public Validator isPositive(Number value, String name) {
    return isPositiveInternal(value, () -> createError(value, name, "must be positive", true));
  }

  /**
   * Validates that the specified numeric value is positive (greater than zero).
   *
   * @param value the numeric value to check
   * @param messageSupplier supplier for the error message if the value is not positive
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or not positive and fail-fast is enabled
   */
  public Validator isPositive(Number value, Supplier<String> messageSupplier) {
    return isPositiveInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified numeric value is positive (greater than zero).
   *
   * @param value the numeric value to check
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or not positive and fail-fast is enabled
   */
  public Validator isPositive(Number value) {
    return isPositive(value, (String) null);
  }

  private Validator isPositiveInternal(Number value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value == null || value.doubleValue() <= 0, errorSupplier);
  }

  /**
   * Validates that the specified numeric value is negative (less than zero).
   *
   * @param value the numeric value to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or not negative and fail-fast is enabled
   */
  public Validator isNegative(Number value, String name) {
    return isNegativeInternal(value, () -> createError(value, name, "must be negative", true));
  }

  /**
   * Validates that the specified numeric value is negative (less than zero).
   *
   * @param value the numeric value to check
   * @param messageSupplier supplier for the error message if the value is not negative
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or not negative and fail-fast is enabled
   */
  public Validator isNegative(Number value, Supplier<String> messageSupplier) {
    return isNegativeInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified numeric value is negative (less than zero).
   *
   * @param value the numeric value to check
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or not negative and fail-fast is enabled
   */
  public Validator isNegative(Number value) {
    return isNegative(value, (String) null);
  }

  private Validator isNegativeInternal(Number value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value == null || value.doubleValue() >= 0, errorSupplier);
  }

  /**
   * Validates that the specified numeric value is non-negative (greater than or equal to zero).
   *
   * @param value the numeric value to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or negative and fail-fast is enabled
   */
  public Validator isNonNegative(Number value, String name) {
    return isNonNegativeInternal(
        value, () -> createError(value, name, "must be non-negative", true));
  }

  /**
   * Validates that the specified numeric value is non-negative (greater than or equal to zero).
   *
   * @param value the numeric value to check
   * @param messageSupplier supplier for the error message if the value is negative
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or negative and fail-fast is enabled
   */
  public Validator isNonNegative(Number value, Supplier<String> messageSupplier) {
    return isNonNegativeInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified numeric value is non-negative (greater than or equal to zero).
   *
   * @param value the numeric value to check
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or negative and fail-fast is enabled
   */
  public Validator isNonNegative(Number value) {
    return isNonNegative(value, (String) null);
  }

  private Validator isNonNegativeInternal(Number value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value == null || value.doubleValue() < 0, errorSupplier);
  }

  /**
   * Validates that the specified numeric value is non-positive (less than or equal to zero).
   *
   * @param value the numeric value to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or positive and fail-fast is enabled
   */
  public Validator isNonPositive(Number value, String name) {
    return isNonPositiveInternal(
        value, () -> createError(value, name, "must be non-positive", true));
  }

  /**
   * Validates that the specified numeric value is non-positive (less than or equal to zero).
   *
   * @param value the numeric value to check
   * @param messageSupplier supplier for the error message if the value is positive
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or positive and fail-fast is enabled
   */
  public Validator isNonPositive(Number value, Supplier<String> messageSupplier) {
    return isNonPositiveInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified numeric value is non-positive (less than or equal to zero).
   *
   * @param value the numeric value to check
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or positive and fail-fast is enabled
   */
  public Validator isNonPositive(Number value) {
    return isNonPositive(value, (String) null);
  }

  private Validator isNonPositiveInternal(Number value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value == null || value.doubleValue() > 0, errorSupplier);
  }

  // ========================================
  // String Validation Methods
  // ========================================

  /**
   * Validates that the specified string is not null and not empty.
   *
   * @param value the string value to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(String value, String name) {
    return notEmptyInternal(value, () -> createError(value, name, MSG_NOT_NULL_OR_EMPTY, false));
  }

  /**
   * Validates that the specified string is not null and not empty.
   *
   * @param value the string value to check
   * @param messageSupplier supplier for the error message if the value is null or empty
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(String value, Supplier<String> messageSupplier) {
    return notEmptyInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified string is not null and not empty.
   *
   * @param value the string value to check
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(String value) {
    return notEmpty(value, (String) null);
  }

  private Validator notEmptyInternal(String value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value == null || value.isEmpty(), errorSupplier);
  }

  /**
   * Validates that the specified string is not null, not empty, and not blank (contains
   * non-whitespace characters).
   *
   * @param value the string value to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null, empty, or blank and fail-fast is enabled
   */
  public Validator notBlank(String value, String name) {
    return notBlankInternal(value, () -> createError(value, name, "must not be blank", false));
  }

  /**
   * Validates that the specified string is not null, not empty, and not blank (contains
   * non-whitespace characters).
   *
   * @param value the string value to check
   * @param messageSupplier supplier for the error message if the value is null, empty, or blank
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null, empty, or blank and fail-fast is enabled
   */
  public Validator notBlank(String value, Supplier<String> messageSupplier) {
    return notBlankInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified string is not null, not empty, and not blank (contains
   * non-whitespace characters).
   *
   * @param value the string value to check
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null, empty, or blank and fail-fast is enabled
   */
  public Validator notBlank(String value) {
    return notBlank(value, (String) null);
  }

  private Validator notBlankInternal(String value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value == null || value.trim().isEmpty(), errorSupplier);
  }

  /**
   * Validates that the specified string has a length within the given range (inclusive).
   *
   * @param value the string value to check
   * @param minLength the minimum allowed length (inclusive)
   * @param maxLength the maximum allowed length (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or length is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minLength is greater than maxLength
   */
  public Validator hasLength(String value, int minLength, int maxLength, String name) {
    return hasLengthInternal(
        value,
        minLength,
        maxLength,
        () ->
            createError(
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or length is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minLength is greater than maxLength
   */
  public Validator hasLength(
      String value, int minLength, int maxLength, Supplier<String> messageSupplier) {
    return hasLengthInternal(value, minLength, maxLength, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified string has a length within the given range (inclusive).
   *
   * @param value the string value to check
   * @param minLength the minimum allowed length (inclusive)
   * @param maxLength the maximum allowed length (inclusive)
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or length is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minLength is greater than maxLength
   */
  public Validator hasLength(String value, int minLength, int maxLength) {
    return hasLength(value, minLength, maxLength, (String) null);
  }

  private Validator hasLengthInternal(
      String value, int minLength, int maxLength, Supplier<ValidationError> errorSupplier) {
    if (minLength > maxLength) {
      throw new IllegalArgumentException("minLength cannot be greater than maxLength");
    }

    return assertFalseInternal(
        value == null || value.length() < minLength || value.length() > maxLength, errorSupplier);
  }

  /**
   * Validates that the specified string matches the given regular expression pattern.
   *
   * @param value the string value to check
   * @param regex the regular expression pattern to match against
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or doesn't match the pattern and fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator matches(String value, String regex, String name) {
    return matchesInternal(
        value,
        regex,
        () -> createError(value, name, String.format("must match pattern '%s'", regex), true));
  }

  /**
   * Validates that the specified string matches the given regular expression pattern.
   *
   * @param value the string value to check
   * @param regex the regular expression pattern to match against
   * @param messageSupplier supplier for the error message if the value doesn't match the pattern
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or doesn't match the pattern and fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator matches(String value, String regex, Supplier<String> messageSupplier) {
    return matchesInternal(value, regex, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified string matches the given regular expression pattern.
   *
   * @param value the string value to check
   * @param regex the regular expression pattern to match against
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or doesn't match the pattern and fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator matches(String value, String regex) {
    return matches(value, regex, (String) null);
  }

  private Validator matchesInternal(
      String value, String regex, Supplier<ValidationError> errorSupplier) {
    if (regex == null) {
      throw new IllegalArgumentException("regex pattern cannot be null");
    }

    return matchesInternal(
        value, PATTERN_CACHE.computeIfAbsent(regex, k -> Pattern.compile(regex)), errorSupplier);
  }

  /**
   * Validates that the specified string matches the given compiled regular expression pattern.
   *
   * @param value the string value to check
   * @param regex the compiled regular expression pattern to match against
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or doesn't match the pattern and fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator matches(String value, Pattern regex, String name) {
    return matchesInternal(
        value,
        regex,
        () ->
            createError(
                value, name, String.format("must match pattern '%s'", regex.pattern()), true));
  }

  /**
   * Validates that the specified string matches the given compiled regular expression pattern.
   *
   * @param value the string value to check
   * @param regex the compiled regular expression pattern to match against
   * @param messageSupplier supplier for the error message if the value doesn't match the pattern
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or doesn't match the pattern and fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator matches(String value, Pattern regex, Supplier<String> messageSupplier) {
    return matchesInternal(value, regex, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified string matches the given compiled regular expression pattern.
   *
   * @param value the string value to check
   * @param regex the compiled regular expression pattern to match against
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or doesn't match the pattern and fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator matches(String value, Pattern regex) {
    return matches(value, regex, (String) null);
  }

  private Validator matchesInternal(
      String value, Pattern regex, Supplier<ValidationError> errorSupplier) {
    if (regex == null) {
      throw new IllegalArgumentException("regex pattern cannot be null");
    }
    return assertFalseInternal(value == null || !regex.matcher(value).matches(), errorSupplier);
  }

  // ========================================
  // Collection/Map Validation Methods
  // ========================================

  /**
   * Validates that the specified collection is not null and not empty.
   *
   * @param value the collection to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(Collection<?> value, String name) {
    return notEmptyInternal(value, () -> createError(value, name, MSG_NOT_NULL_OR_EMPTY, false));
  }

  /**
   * Validates that the specified collection is not null and not empty.
   *
   * @param value the collection to check
   * @param messageSupplier supplier for the error message if the value is null or empty
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(Collection<?> value, Supplier<String> messageSupplier) {
    return notEmptyInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified collection is not null and not empty.
   *
   * @param value the collection to check
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(Collection<?> value) {
    return notEmpty(value, (String) null);
  }

  private Validator notEmptyInternal(Collection<?> value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value == null || value.isEmpty(), errorSupplier);
  }

  /**
   * Validates that the specified map is not null and not empty.
   *
   * @param value the map to check
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(Map<?, ?> value, String name) {
    return notEmptyInternal(value, () -> createError(value, name, MSG_NOT_NULL_OR_EMPTY, false));
  }

  /**
   * Validates that the specified map is not null and not empty.
   *
   * @param value the map to check
   * @param messageSupplier supplier for the error message if the value is null or empty
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(Map<?, ?> value, Supplier<String> messageSupplier) {
    return notEmptyInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified map is not null and not empty.
   *
   * @param value the map to check
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or empty and fail-fast is enabled
   */
  public Validator notEmpty(Map<?, ?> value) {
    return notEmpty(value, (String) null);
  }

  private Validator notEmptyInternal(Map<?, ?> value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value == null || value.isEmpty(), errorSupplier);
  }

  /**
   * Validates that the specified collection has a size within the given range (inclusive).
   *
   * @param value the collection to check
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or size is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator hasSize(Collection<?> value, int minSize, int maxSize, String name) {
    return hasSizeInternal(
        value,
        minSize,
        maxSize,
        () ->
            createError(
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or size is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator hasSize(
      Collection<?> value, int minSize, int maxSize, Supplier<String> messageSupplier) {
    return hasSizeInternal(value, minSize, maxSize, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified collection has a size within the given range (inclusive).
   *
   * @param value the collection to check
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or size is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator hasSize(Collection<?> value, int minSize, int maxSize) {
    return hasSize(value, minSize, maxSize, (String) null);
  }

  private Validator hasSizeInternal(
      Collection<?> value, int minSize, int maxSize, Supplier<ValidationError> errorSupplier) {
    if (minSize > maxSize) {
      throw new IllegalArgumentException("minSize cannot be greater than maxSize");
    }

    return assertFalseInternal(
        value == null || value.size() < minSize || value.size() > maxSize, errorSupplier);
  }

  /**
   * Validates that the specified map has a size within the given range (inclusive).
   *
   * @param value the map to check
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or size is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator hasSize(Map<?, ?> value, int minSize, int maxSize, String name) {
    return hasSizeInternal(
        value,
        minSize,
        maxSize,
        () ->
            createError(
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or size is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator hasSize(
      Map<?, ?> value, int minSize, int maxSize, Supplier<String> messageSupplier) {
    return hasSizeInternal(value, minSize, maxSize, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified map has a size within the given range (inclusive).
   *
   * @param value the map to check
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is null or size is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator hasSize(Map<?, ?> value, int minSize, int maxSize) {
    return hasSize(value, minSize, maxSize, (String) null);
  }

  private Validator hasSizeInternal(
      Map<?, ?> value, int minSize, int maxSize, Supplier<ValidationError> errorSupplier) {
    if (minSize > maxSize) {
      throw new IllegalArgumentException("minSize cannot be greater than maxSize");
    }

    return assertFalseInternal(
        value == null || value.size() < minSize || value.size() > maxSize, errorSupplier);
  }

  // ========================================
  // Null-Tolerant Range Methods
  // ========================================

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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if min or max are null
   */
  public <T extends Number> Validator nullOrInRange(T value, T min, T max, String name) {
    return nullOrInRangeInternal(
        value,
        min,
        max,
        () ->
            createError(
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if min or max are null
   */
  public <T extends Number> Validator nullOrInRange(
      T value, T min, T max, Supplier<String> messageSupplier) {
    return nullOrInRangeInternal(value, min, max, () -> createError(messageSupplier));
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if min or max are null
   */
  public <T extends Number> Validator nullOrInRange(T value, T min, T max) {
    return nullOrInRange(value, min, max, (String) null);
  }

  private <T extends Number> Validator nullOrInRangeInternal(
      T value, T min, T max, Supplier<ValidationError> errorSupplier) {
    if (min == null || max == null) {
      throw new IllegalArgumentException("min and max cannot be null");
    }

    return assertFalseInternal(
        value != null
            && (value.doubleValue() < min.doubleValue() || value.doubleValue() > max.doubleValue()),
        errorSupplier);
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and less than minValue and fail-fast is enabled
   * @throws IllegalArgumentException if minValue is null
   */
  public <T extends Number> Validator nullOrMin(T value, T minValue, String name) {
    return nullOrMinInternal(
        value,
        minValue,
        () ->
            createError(value, name, String.format("must be null or at least %s", minValue), true));
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and less than minValue and fail-fast is enabled
   * @throws IllegalArgumentException if minValue is null
   */
  public <T extends Number> Validator nullOrMin(
      T value, T minValue, Supplier<String> messageSupplier) {
    return nullOrMinInternal(value, minValue, () -> createError(messageSupplier));
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and less than minValue and fail-fast is enabled
   * @throws IllegalArgumentException if minValue is null
   */
  public <T extends Number> Validator nullOrMin(T value, T minValue) {
    return nullOrMin(value, minValue, (String) null);
  }

  private <T extends Number> Validator nullOrMinInternal(
      T value, T minValue, Supplier<ValidationError> errorSupplier) {
    if (minValue == null) {
      throw new IllegalArgumentException("minValue cannot be null");
    }

    return assertFalseInternal(
        value != null && value.doubleValue() < minValue.doubleValue(), errorSupplier);
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and greater than maxValue and fail-fast is enabled
   * @throws IllegalArgumentException if maxValue is null
   */
  public <T extends Number> Validator nullOrMax(T value, T maxValue, String name) {
    return nullOrMaxInternal(
        value,
        maxValue,
        () ->
            createError(value, name, String.format("must be null or at most %s", maxValue), true));
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and greater than maxValue and fail-fast is enabled
   * @throws IllegalArgumentException if maxValue is null
   */
  public <T extends Number> Validator nullOrMax(
      T value, T maxValue, Supplier<String> messageSupplier) {
    return nullOrMaxInternal(value, maxValue, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified numeric value is null or less than or equal to the maximum value.
   * This method passes validation if the value is null OR if it's at most the maximum value.
   *
   * @param <T> the type of the numeric value
   * @param value the numeric value to check (can be null)
   * @param maxValue the maximum allowed value (inclusive)
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and greater than maxValue and fail-fast is enabled
   * @throws IllegalArgumentException if maxValue is null
   */
  public <T extends Number> Validator nullOrMax(T value, T maxValue) {
    return nullOrMax(value, maxValue, (String) null);
  }

  private <T extends Number> Validator nullOrMaxInternal(
      T value, T maxValue, Supplier<ValidationError> errorSupplier) {
    if (maxValue == null) {
      throw new IllegalArgumentException("maxValue cannot be null");
    }

    return assertFalseInternal(
        value != null && value.doubleValue() > maxValue.doubleValue(), errorSupplier);
  }

  // ========================================
  // Null-Tolerant Sign Methods
  // ========================================

  /**
   * Validates that the specified numeric value is null or positive (greater than zero). This method
   * passes validation if the value is null OR if it's positive.
   *
   * @param value the numeric value to check (can be null)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and not positive and fail-fast is enabled
   */
  public Validator nullOrIsPositive(Number value, String name) {
    return nullOrIsPositiveInternal(
        value, () -> createError(value, name, "must be null or positive", true));
  }

  /**
   * Validates that the specified numeric value is null or positive (greater than zero). This method
   * passes validation if the value is null OR if it's positive.
   *
   * @param value the numeric value to check (can be null)
   * @param messageSupplier supplier for the error message if the value is not null and not positive
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and not positive and fail-fast is enabled
   */
  public Validator nullOrIsPositive(Number value, Supplier<String> messageSupplier) {
    return nullOrIsPositiveInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified numeric value is null or positive (greater than zero). This method
   * passes validation if the value is null OR if it's positive.
   *
   * @param value the numeric value to check (can be null)
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and not positive and fail-fast is enabled
   */
  public Validator nullOrIsPositive(Number value) {
    return nullOrIsPositive(value, (String) null);
  }

  private Validator nullOrIsPositiveInternal(
      Number value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value != null && value.doubleValue() <= 0, errorSupplier);
  }

  /**
   * Validates that the specified numeric value is null or negative (less than zero). This method
   * passes validation if the value is null OR if it's negative.
   *
   * @param value the numeric value to check (can be null)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and not negative and fail-fast is enabled
   */
  public Validator nullOrIsNegative(Number value, String name) {
    return nullOrIsNegativeInternal(
        value, () -> createError(value, name, "must be null or negative", true));
  }

  /**
   * Validates that the specified numeric value is null or negative (less than zero). This method
   * passes validation if the value is null OR if it's negative.
   *
   * @param value the numeric value to check (can be null)
   * @param messageSupplier supplier for the error message if the value is not null and not negative
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and not negative and fail-fast is enabled
   */
  public Validator nullOrIsNegative(Number value, Supplier<String> messageSupplier) {
    return nullOrIsNegativeInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified numeric value is null or negative (less than zero). This method
   * passes validation if the value is null OR if it's negative.
   *
   * @param value the numeric value to check (can be null)
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and not negative and fail-fast is enabled
   */
  public Validator nullOrIsNegative(Number value) {
    return nullOrIsNegative(value, (String) null);
  }

  private Validator nullOrIsNegativeInternal(
      Number value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value != null && value.doubleValue() >= 0, errorSupplier);
  }

  /**
   * Validates that the specified numeric value is null or non-negative (greater than or equal to
   * zero). This method passes validation if the value is null OR if it's non-negative.
   *
   * @param value the numeric value to check (can be null)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and negative and fail-fast is enabled
   */
  public Validator nullOrIsNonNegative(Number value, String name) {
    return nullOrIsNonNegativeInternal(
        value, () -> createError(value, name, "must be null or non-negative", true));
  }

  /**
   * Validates that the specified numeric value is null or non-negative (greater than or equal to
   * zero). This method passes validation if the value is null OR if it's non-negative.
   *
   * @param value the numeric value to check (can be null)
   * @param messageSupplier supplier for the error message if the value is not null and negative
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and negative and fail-fast is enabled
   */
  public Validator nullOrIsNonNegative(Number value, Supplier<String> messageSupplier) {
    return nullOrIsNonNegativeInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified numeric value is null or non-negative (greater than or equal to
   * zero). This method passes validation if the value is null OR if it's non-negative.
   *
   * @param value the numeric value to check (can be null)
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and negative and fail-fast is enabled
   */
  public Validator nullOrIsNonNegative(Number value) {
    return nullOrIsNonNegative(value, (String) null);
  }

  private Validator nullOrIsNonNegativeInternal(
      Number value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value != null && value.doubleValue() < 0, errorSupplier);
  }

  /**
   * Validates that the specified numeric value is null or non-positive (less than or equal to
   * zero). This method passes validation if the value is null OR if it's non-positive.
   *
   * @param value the numeric value to check (can be null)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and positive and fail-fast is enabled
   */
  public Validator nullOrIsNonPositive(Number value, String name) {
    return nullOrIsNonPositiveInternal(
        value, () -> createError(value, name, "must be null or non-positive", true));
  }

  /**
   * Validates that the specified numeric value is null or non-positive (less than or equal to
   * zero). This method passes validation if the value is null OR if it's non-positive.
   *
   * @param value the numeric value to check (can be null)
   * @param messageSupplier supplier for the error message if the value is not null and positive
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and positive and fail-fast is enabled
   */
  public Validator nullOrIsNonPositive(Number value, Supplier<String> messageSupplier) {
    return nullOrIsNonPositiveInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified numeric value is null or non-positive (less than or equal to
   * zero). This method passes validation if the value is null OR if it's non-positive.
   *
   * @param value the numeric value to check (can be null)
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and positive and fail-fast is enabled
   */
  public Validator nullOrIsNonPositive(Number value) {
    return nullOrIsNonPositive(value, (String) null);
  }

  private Validator nullOrIsNonPositiveInternal(
      Number value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value != null && value.doubleValue() > 0, errorSupplier);
  }

  // ========================================
  // Null-Tolerant String Methods
  // ========================================

  /**
   * Validates that the specified string is null or not empty. This method passes validation if the
   * value is null OR if it's not empty.
   *
   * @param value the string value to check (can be null)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(String value, String name) {
    return nullOrNotEmptyInternal(
        value, () -> createError(value, name, MSG_NULL_OR_NOT_EMPTY, false));
  }

  /**
   * Validates that the specified string is null or not empty. This method passes validation if the
   * value is null OR if it's not empty.
   *
   * @param value the string value to check (can be null)
   * @param messageSupplier supplier for the error message if the value is not null and empty
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(String value, Supplier<String> messageSupplier) {
    return nullOrNotEmptyInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified string is null or not empty. This method passes validation if the
   * value is null OR if it's not empty.
   *
   * @param value the string value to check (can be null)
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(String value) {
    return nullOrNotEmpty(value, (String) null);
  }

  private Validator nullOrNotEmptyInternal(String value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value != null && value.isEmpty(), errorSupplier);
  }

  /**
   * Validates that the specified string is null or not blank (contains non-whitespace characters).
   * This method passes validation if the value is null OR if it's not blank.
   *
   * @param value the string value to check (can be null)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and blank and fail-fast is enabled
   */
  public Validator nullOrNotBlank(String value, String name) {
    return nullOrNotBlankInternal(
        value, () -> createError(value, name, "must be null or not blank", false));
  }

  /**
   * Validates that the specified string is null or not blank (contains non-whitespace characters).
   * This method passes validation if the value is null OR if it's not blank.
   *
   * @param value the string value to check (can be null)
   * @param messageSupplier supplier for the error message if the value is not null and blank
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and blank and fail-fast is enabled
   */
  public Validator nullOrNotBlank(String value, Supplier<String> messageSupplier) {
    return nullOrNotBlankInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified string is null or not blank (contains non-whitespace characters).
   * This method passes validation if the value is null OR if it's not blank.
   *
   * @param value the string value to check (can be null)
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and blank and fail-fast is enabled
   */
  public Validator nullOrNotBlank(String value) {
    return nullOrNotBlank(value, (String) null);
  }

  private Validator nullOrNotBlankInternal(String value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value != null && value.trim().isEmpty(), errorSupplier);
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and length is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minLength is greater than maxLength
   */
  public Validator nullOrHasLength(String value, int minLength, int maxLength, String name) {
    return nullOrHasLengthInternal(
        value,
        minLength,
        maxLength,
        () ->
            createError(
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and length is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minLength is greater than maxLength
   */
  public Validator nullOrHasLength(
      String value, int minLength, int maxLength, Supplier<String> messageSupplier) {
    return nullOrHasLengthInternal(value, minLength, maxLength, () -> createError(messageSupplier));
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and length is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minLength is greater than maxLength
   */
  public Validator nullOrHasLength(String value, int minLength, int maxLength) {
    return nullOrHasLength(value, minLength, maxLength, (String) null);
  }

  private Validator nullOrHasLengthInternal(
      String value, int minLength, int maxLength, Supplier<ValidationError> errorSupplier) {
    if (minLength > maxLength) {
      throw new IllegalArgumentException("minLength cannot be greater than maxLength");
    }

    return assertFalseInternal(
        value != null && (value.length() < minLength || value.length() > maxLength), errorSupplier);
  }

  /**
   * Validates that the specified string is null or matches the given regular expression pattern.
   * This method passes validation if the value is null OR if it matches the pattern.
   *
   * @param value the string value to check (can be null)
   * @param regex the regular expression pattern to match against
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and doesn't match the pattern and fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator nullOrMatches(String value, String regex, String name) {
    return nullOrMatchesInternal(
        value,
        regex,
        () ->
            createError(
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and doesn't match the pattern and fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator nullOrMatches(String value, String regex, Supplier<String> messageSupplier) {
    return nullOrMatchesInternal(value, regex, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified string is null or matches the given regular expression pattern.
   * This method passes validation if the value is null OR if it matches the pattern.
   *
   * @param value the string value to check (can be null)
   * @param regex the regular expression pattern to match against
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and doesn't match the pattern and fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator nullOrMatches(String value, String regex) {
    return nullOrMatches(value, regex, (String) null);
  }

  private Validator nullOrMatchesInternal(
      String value, String regex, Supplier<ValidationError> errorSupplier) {
    if (regex == null) {
      throw new IllegalArgumentException("regex pattern cannot be null");
    }

    return nullOrMatchesInternal(
        value, PATTERN_CACHE.computeIfAbsent(regex, k -> Pattern.compile(regex)), errorSupplier);
  }

  /**
   * Validates that the specified string is null or matches the given compiled regular expression
   * pattern. This method passes validation if the value is null OR if it matches the pattern.
   *
   * @param value the string value to check (can be null)
   * @param regex the compiled regular expression pattern to match against
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and doesn't match the pattern and fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator nullOrMatches(String value, Pattern regex, String name) {
    return nullOrMatchesInternal(
        value,
        regex,
        () ->
            createError(
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and doesn't match the pattern and fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator nullOrMatches(String value, Pattern regex, Supplier<String> messageSupplier) {
    return nullOrMatchesInternal(value, regex, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified string is null or matches the given compiled regular expression
   * pattern. This method passes validation if the value is null OR if it matches the pattern.
   *
   * @param value the string value to check (can be null)
   * @param regex the compiled regular expression pattern to match against
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and doesn't match the pattern and fail-fast is enabled
   * @throws IllegalArgumentException if regex pattern is null
   */
  public Validator nullOrMatches(String value, Pattern regex) {
    return nullOrMatches(value, regex, (String) null);
  }

  private Validator nullOrMatchesInternal(
      String value, Pattern regex, Supplier<ValidationError> errorSupplier) {
    if (regex == null) {
      throw new IllegalArgumentException("regex pattern cannot be null");
    }
    return assertFalseInternal(value != null && !regex.matcher(value).matches(), errorSupplier);
  }

  // ========================================
  // Null-Tolerant Collection/Map Methods
  // ========================================

  /**
   * Validates that the specified collection is null or not empty. This method passes validation if
   * the value is null OR if it's not empty.
   *
   * @param value the collection to check (can be null)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(Collection<?> value, String name) {
    return nullOrNotEmptyInternal(
        value, () -> createError(value, name, MSG_NULL_OR_NOT_EMPTY, false));
  }

  /**
   * Validates that the specified collection is null or not empty. This method passes validation if
   * the value is null OR if it's not empty.
   *
   * @param value the collection to check (can be null)
   * @param messageSupplier supplier for the error message if the value is not null and empty
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(Collection<?> value, Supplier<String> messageSupplier) {
    return nullOrNotEmptyInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified collection is null or not empty. This method passes validation if
   * the value is null OR if it's not empty.
   *
   * @param value the collection to check (can be null)
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(Collection<?> value) {
    return nullOrNotEmpty(value, (String) null);
  }

  private Validator nullOrNotEmptyInternal(
      Collection<?> value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value != null && value.isEmpty(), errorSupplier);
  }

  /**
   * Validates that the specified map is null or not empty. This method passes validation if the
   * value is null OR if it's not empty.
   *
   * @param value the map to check (can be null)
   * @param name the parameter name for error messages
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(Map<?, ?> value, String name) {
    return nullOrNotEmptyInternal(
        value, () -> createError(value, name, MSG_NULL_OR_NOT_EMPTY, false));
  }

  /**
   * Validates that the specified map is null or not empty. This method passes validation if the
   * value is null OR if it's not empty.
   *
   * @param value the map to check (can be null)
   * @param messageSupplier supplier for the error message if the value is not null and empty
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(Map<?, ?> value, Supplier<String> messageSupplier) {
    return nullOrNotEmptyInternal(value, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified map is null or not empty. This method passes validation if the
   * value is null OR if it's not empty.
   *
   * @param value the map to check (can be null)
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and empty and fail-fast is enabled
   */
  public Validator nullOrNotEmpty(Map<?, ?> value) {
    return nullOrNotEmpty(value, (String) null);
  }

  private Validator nullOrNotEmptyInternal(
      Map<?, ?> value, Supplier<ValidationError> errorSupplier) {
    return assertFalseInternal(value != null && value.isEmpty(), errorSupplier);
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and size is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator nullOrHasSize(Collection<?> value, int minSize, int maxSize, String name) {
    return nullOrHasSizeInternal(
        value,
        minSize,
        maxSize,
        () ->
            createError(
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and size is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator nullOrHasSize(
      Collection<?> value, int minSize, int maxSize, Supplier<String> messageSupplier) {
    return nullOrHasSizeInternal(value, minSize, maxSize, () -> createError(messageSupplier));
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and size is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator nullOrHasSize(Collection<?> value, int minSize, int maxSize) {
    return nullOrHasSize(value, minSize, maxSize, (String) null);
  }

  private Validator nullOrHasSizeInternal(
      Collection<?> value, int minSize, int maxSize, Supplier<ValidationError> errorSupplier) {
    if (minSize > maxSize) {
      throw new IllegalArgumentException("minSize cannot be greater than maxSize");
    }

    return assertFalseInternal(
        value != null && (value.size() < minSize || value.size() > maxSize), errorSupplier);
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and size is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator nullOrHasSize(Map<?, ?> value, int minSize, int maxSize, String name) {
    return nullOrHasSizeInternal(
        value,
        minSize,
        maxSize,
        () ->
            createError(
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
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and size is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator nullOrHasSize(
      Map<?, ?> value, int minSize, int maxSize, Supplier<String> messageSupplier) {
    return nullOrHasSizeInternal(value, minSize, maxSize, () -> createError(messageSupplier));
  }

  /**
   * Validates that the specified map is null or has a size within the given range (inclusive). This
   * method passes validation if the value is null OR if its size is within the specified range.
   *
   * @param value the map to check (can be null)
   * @param minSize the minimum allowed size (inclusive)
   * @param maxSize the maximum allowed size (inclusive)
   * @return this validator instance for method chaining
   * @throws RuntimeException (specifically {@link ValidationException} by default) immediately if
   *     value is not null and size is outside the range and fail-fast is enabled
   * @throws IllegalArgumentException if minSize is greater than maxSize
   */
  public Validator nullOrHasSize(Map<?, ?> value, int minSize, int maxSize) {
    return nullOrHasSize(value, minSize, maxSize, (String) null);
  }

  private Validator nullOrHasSizeInternal(
      Map<?, ?> value, int minSize, int maxSize, Supplier<ValidationError> errorSupplier) {
    if (minSize > maxSize) {
      throw new IllegalArgumentException("minSize cannot be greater than maxSize");
    }

    return assertFalseInternal(
        value != null && (value.size() < minSize || value.size() > maxSize), errorSupplier);
  }
}
