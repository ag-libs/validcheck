package io.github.validcheck;

import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when validation fails. Contains detailed error messages and supports
 * configurable stack trace generation.
 *
 * <p>This exception is thrown by validators when validation rules are violated. It can contain
 * multiple error messages when used with batch validation:
 *
 * <pre>{@code
 * try {
 *   ValidCheck.check()
 *           .notNull(name, "name")
 *           .isPositive(age, "age")
 *           .validate();
 * } catch (ValidationException e) {
 *   List<String> errors = e.getErrors();
 *   // Handle multiple validation errors
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see ValidCheck
 * @see Validator
 * @see BatchValidator
 */
public class ValidationException extends RuntimeException {
  /** Whether to fill stack traces for performance optimization. */
  private final boolean fillStackTrace;

  /** Immutable list of all validation error messages. */
  private final List<String> errors;

  /**
   * Constructs a new ValidationException with the specified configuration.
   *
   * @param fillStackTrace whether to fill stack traces for performance optimization
   * @param message the detail message combining all validation errors
   * @param errors the list of individual validation error messages
   */
  public ValidationException(boolean fillStackTrace, String message, List<String> errors) {
    super(message);
    this.fillStackTrace = fillStackTrace;
    this.errors = Collections.unmodifiableList(errors);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Conditionally fills the stack trace based on the fillStackTrace configuration. When
   * disabled, this provides better performance for validation scenarios where stack traces are not
   * needed.
   */
  @Override
  public synchronized Throwable fillInStackTrace() {
    return fillStackTrace ? super.fillInStackTrace() : this;
  }

  /**
   * Returns the list of individual validation error messages.
   *
   * @return an unmodifiable list of validation error messages
   */
  public List<String> getErrors() {
    return errors;
  }
}
