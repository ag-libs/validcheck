package io.github.aglibs.validcheck;

/**
 * Entry point for ValidCheck validation that does not include input values in error messages.
 * Provides static factory methods to create validators that exclude values from error messages.
 *
 * <p>Methods with "Fast" suffix create exceptions without stack traces.
 *
 * @since 0.9.10
 * @see ValidCheck
 * @see FastValidationException
 */
public final class SafeValidCheck {

  /** Private constructor to prevent instantiation of this utility class. */
  private SafeValidCheck() {}

  /**
   * Creates a new batch validator that collects all validation errors before throwing. Does not
   * include input values in error messages. Uses exceptions with stack traces.
   *
   * @return a new {@link BatchValidator} instance
   * @see BatchValidator
   * @see #checkFast()
   */
  public static BatchValidator check() {
    return new BatchValidator(true, true, null);
  }

  /**
   * Creates a new batch validator that collects all validation errors before throwing. Does not
   * include input values in error messages. Uses fast exceptions without stack traces.
   *
   * @return a new {@link BatchValidator} instance
   * @see BatchValidator
   * @see FastValidationException
   */
  public static BatchValidator checkFast() {
    return new BatchValidator(true, false, null);
  }

  /**
   * Creates a new validator with fail-fast behavior. Throws a {@link ValidationException}
   * immediately upon the first validation failure. Does not include input values in error messages.
   * Uses exceptions with stack traces.
   *
   * @return a new {@link Validator} instance
   * @see Validator
   * @see #requireFast()
   */
  public static Validator require() {
    return new Validator(true, true, true, null);
  }

  /**
   * Creates a new validator with fail-fast behavior. Throws a {@link FastValidationException}
   * immediately upon the first validation failure. Does not include input values in error messages.
   * Uses fast exceptions without stack traces.
   *
   * @return a new {@link Validator} instance
   * @see Validator
   * @see FastValidationException
   */
  public static Validator requireFast() {
    return new Validator(true, true, false, null);
  }

  /**
   * Validates that the specified value is not null.
   *
   * @param value the value to check for null
   * @param name the name of the parameter being validated (used in error messages)
   * @throws ValidationException if the value is null
   * @see Validator#notNull(Object, String)
   */
  public static void requireNotNull(Object value, String name) {
    require().notNull(value, name);
  }

  /**
   * Validates that the specified value is not null.
   *
   * @param value the value to check for null
   * @throws ValidationException if the value is null
   * @see Validator#notNull(Object)
   */
  public static void requireNotNull(Object value) {
    require().notNull(value);
  }

  /**
   * Validates that the specified condition is true.
   *
   * @param condition the condition to evaluate
   * @param message the error message to use if the condition is false
   * @throws ValidationException if the condition is false
   * @see Validator#assertTrue(boolean, String)
   */
  public static void assertTrue(boolean condition, String message) {
    require().assertTrue(condition, message);
  }
}
