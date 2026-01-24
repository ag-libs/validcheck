package io.github.aglibs.validcheck;

/**
 * Security and performance-focused entry point for ValidCheck validation. Provides static factory
 * methods to create validators that do not include input values in error messages and use fast
 * exceptions without stack traces.
 *
 * <p>This class is designed for scenarios where:
 *
 * <ul>
 *   <li>Input values may contain sensitive data (passwords, tokens, PII) that should not be exposed
 *       in error messages or logs
 *   <li>High-throughput validation where exception creation overhead matters (thousands of
 *       validations per second)
 *   <li>Validation errors are part of normal application flow (e.g., user input validation) and
 *       detailed stack traces are not needed
 * </ul>
 *
 * <p><strong>Example usage:</strong>
 *
 * <pre>{@code
 * // Validate password without exposing it in error messages
 * SafeValidCheck.require()
 *     .notNull(password, "password")
 *     .hasLength(password, 8, 100, "password");
 * // Error: "'password' must have length between 8 and 100"
 * // (password value NOT included)
 *
 * // Batch validation for API input
 * SafeValidCheck.check()
 *     .notNull(username, "username")
 *     .notNull(email, "email")
 *     .validate();
 * // Fast exceptions without stack traces
 * }</pre>
 *
 * @since 0.9.10
 * @see ValidCheck
 * @see FastValidationException
 */
public final class SafeValidCheck {

  /** Private constructor to prevent instantiation of this utility class. */
  private SafeValidCheck() {}

  /**
   * Creates a new batch validator that collects all validation errors before throwing. Configured
   * for security and performance: does not include input values in error messages and uses fast
   * exceptions.
   *
   * @return a new {@link BatchValidator} instance configured to exclude values and use fast
   *     exceptions
   * @see BatchValidator
   */
  public static BatchValidator check() {
    return new BatchValidator(false, false, null);
  }

  /**
   * Creates a new validator with fail-fast behavior. Throws a {@link FastValidationException}
   * immediately upon the first validation failure. Does not include input values in error messages
   * for security.
   *
   * @return a new {@link Validator} instance configured to exclude values, fail-fast, and use fast
   *     exceptions
   * @see Validator
   */
  public static Validator require() {
    return new Validator(false, true, false, null);
  }

  /**
   * Validates that the specified value is not null. This is a convenience method equivalent to
   * {@code require().notNull(value, name)}.
   *
   * <p>Error message will not include the actual value for security.
   *
   * @param value the value to check for null
   * @param name the name of the parameter being validated (used in error messages)
   * @throws FastValidationException if the value is null
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
   * @throws FastValidationException if the value is null
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
   * @throws FastValidationException if the condition is false
   * @see Validator#assertTrue(boolean, String)
   */
  public static void assertTrue(boolean condition, String message) {
    require().assertTrue(condition, message);
  }
}
