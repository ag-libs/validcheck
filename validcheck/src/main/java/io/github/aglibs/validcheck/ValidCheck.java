package io.github.aglibs.validcheck;

/**
 * Main entry point for the ValidCheck validation library. Provides static factory methods to create
 * validators for parameter validation.
 *
 * <p>This class creates validators that include actual values in error messages with stack traces.
 *
 * @since 1.0.0
 * @see SafeValidCheck
 */
public final class ValidCheck {

  /** Private constructor to prevent instantiation of this utility class. */
  private ValidCheck() {}

  /**
   * Creates a new batch validator that collects all validation errors before throwing. Includes
   * values in error messages with stack traces.
   *
   * @return a new {@link BatchValidator} instance
   * @see BatchValidator
   */
  public static BatchValidator check() {
    return new BatchValidator(false, true, null);
  }

  /**
   * Creates a new validator with fail-fast behavior. Throws a {@link ValidationException}
   * immediately upon the first validation failure. Includes values in error messages with stack
   * traces.
   *
   * @return a new {@link Validator} instance
   * @see Validator
   */
  public static Validator require() {
    return new Validator(false, true, true, null);
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
