package io.github.aglibs.validcheck;

import java.util.List;

/**
 * A performance-optimized validation exception that does not fill stack traces.
 *
 * <p>This exception is designed for high-performance scenarios where validation errors are expected
 * and stack traces are not needed for debugging. By not filling stack traces, this exception can be
 * created significantly faster than {@link ValidationException}.
 *
 * <p>Consider using this exception when:
 *
 * <ul>
 *   <li>Validation failures are part of normal application flow (e.g., user input validation)
 *   <li>Performance is critical and thousands of validations per second are expected
 *   <li>Stack traces are not needed for debugging or logging
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Configure validator to use FastValidationException (no stack trace)
 * Validator validator = new Validator(
 *     true,  // includeValues - include invalid values in error messages
 *     true,  // failFast
 *     false, // fillStackTrace - use fast exception
 *     null   // exceptionFactory
 * );
 *
 * try {
 *   validator.notNull(value, "value").validate();
 * } catch (FastValidationException e) {
 *   // Handle validation error without stack trace overhead
 *   List<ValidationError> errors = e.getErrors();
 * }
 * }</pre>
 *
 * @since 0.9.10
 * @see ValidationException
 * @see Validator
 */
public final class FastValidationException extends ValidationException {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new FastValidationException.
   *
   * @param message the detail message combining all validation errors
   * @param errors the list of individual validation errors
   */
  public FastValidationException(String message, List<ValidationError> errors) {
    super(message, errors);
  }

  /**
   * Overrides to skip stack trace filling for performance optimization.
   *
   * <p>This method returns {@code this} without filling the stack trace, making exception creation
   * significantly faster. Stack traces are expensive to create and are not needed for typical
   * validation error scenarios.
   *
   * @return this throwable instance without stack trace
   */
  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }
}
