package io.github.aglibs.validcheck;

/**
 * Main entry point for the ValidCheck validation library. Provides static factory methods to create
 * validators for parameter validation.
 *
 * <p>This class offers convenient methods for common validation scenarios:
 *
 * <ul>
 *   <li>Batch validation with {@link #check()}
 *   <li>Single validation with fail-fast behavior using {@link #require()}
 *   <li>Quick null checks with {@link #requireNotNull(Object, String)}
 * </ul>
 *
 * @since 1.0.0
 */
public final class ValidCheck {

  /** Private constructor to prevent instantiation of this utility class. */
  private ValidCheck() {}

  /**
   * Creates a new batch validator that collects all validation errors before throwing. This allows
   * multiple validation failures to be reported at once.
   *
   * @return a new {@link BatchValidator} instance configured to include values and fail-fast
   * @see BatchValidator
   */
  public static BatchValidator check() {
    return new BatchValidator(true, true);
  }

  /**
   * Creates a new validator with fail-fast behavior. Throws a {@link ValidationException}
   * immediately upon the first validation failure.
   *
   * @return a new {@link Validator} instance configured to include values, fail-fast, and fill
   *     stack traces
   * @see Validator
   */
  public static Validator require() {
    return new Validator(true, true, true);
  }

  /**
   * Validates that the specified value is not null. This is a convenience method equivalent to
   * {@code require().notNull(value, name)}.
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
   * Validates that the specified value is not null. This is a convenience method equivalent to
   * {@code require().notNull(value)}.
   *
   * @param value the value to check for null
   * @throws ValidationException if the value is null
   * @see Validator#notNull(Object)
   */
  public static void requireNotNull(Object value) {
    require().notNull(value);
  }

  /**
   * Validates that the specified condition is true. This is a convenience method equivalent to
   * {@code require().assertTrue(condition, message)}.
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
