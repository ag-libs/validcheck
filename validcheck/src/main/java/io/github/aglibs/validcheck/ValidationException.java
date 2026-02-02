package io.github.aglibs.validcheck;

import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when validation fails. Contains validation error details.
 *
 * @since 1.0.0
 * @see ValidCheck
 * @see Validator
 * @see BatchValidator
 */
public class ValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** Immutable list of all validation errors. */
  private final List<ValidationError> errors;

  /**
   * Constructs a new ValidationException.
   *
   * @param message the detail message combining all validation errors
   * @param errors the list of individual validation errors
   */
  public ValidationException(String message, List<ValidationError> errors) {
    super(message);
    this.errors = Collections.unmodifiableList(errors);
  }

  /**
   * Constructs a new ValidationException with auto-generated message.
   *
   * @param errors the list of individual validation errors
   */
  public ValidationException(List<ValidationError> errors) {
    this(ValidationError.join(errors), errors);
  }

  /**
   * Constructs a new ValidationException for manual throwing with a cause.
   *
   * <p>This constructor is useful when wrapping other exceptions during manual validation:
   *
   * <pre>{@code
   * try {
   *     // Some operation that might fail
   *     validateBusinessRule(data);
   * } catch (DatabaseException e) {
   *     throw new ValidationException("userId", "user not found in database", e);
   * }
   * }</pre>
   *
   * @param name the name of the field that failed validation
   * @param message the validation error message
   * @param cause the underlying cause of the validation failure
   */
  public ValidationException(String name, String message, Throwable cause) {
    super(message, cause);
    this.errors = List.of(new ValidationError(name, message));
  }

  /**
   * Constructs a new ValidationException with a cause.
   *
   * @param message the validation error message
   * @param cause the underlying cause of the validation failure
   */
  public ValidationException(String message, Throwable cause) {
    this(null, message, cause);
  }

  /**
   * Constructs a new ValidationException with a message.
   *
   * @param message the validation error message
   */
  public ValidationException(String message) {
    this(null, message, null);
  }

  /**
   * Returns the list of individual validation errors.
   *
   * @return an unmodifiable list of validation errors
   */
  public List<ValidationError> getErrors() {
    return errors;
  }
}
