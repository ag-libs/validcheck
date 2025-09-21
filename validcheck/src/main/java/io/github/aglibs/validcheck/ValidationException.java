package io.github.aglibs.validcheck;

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

  /** Immutable list of all validation error messages. */
  private final List<String> errors;

  /**
   * Constructs a new ValidationException with the specified configuration.
   *
   * @param message the detail message combining all validation errors
   * @param errors the list of individual validation error messages
   */
  protected ValidationException(String message, List<String> errors) {
    super(message);
    this.errors = Collections.unmodifiableList(errors);
  }

  /**
   * Creates a new ValidationException with the specified configuration.
   *
   * @param fillStackTrace whether to fill stack traces for performance optimization
   * @param message the detail message combining all validation errors
   * @param errors the list of individual validation error messages
   */
  static ValidationException create(boolean fillStackTrace, String message, List<String> errors) {
    return fillStackTrace
        ? new ValidationException(message, errors)
        : new ValidationException(message, errors) {
          @Override
          public synchronized Throwable fillInStackTrace() {
            return this;
          }
        };
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
