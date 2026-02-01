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
   * Constructs a new ValidationException with the specified configuration.
   *
   * @param message the detail message combining all validation errors
   * @param errors the list of individual validation errors
   */
  public ValidationException(String message, List<ValidationError> errors) {
    super(message);
    this.errors = Collections.unmodifiableList(errors);
  }

  public ValidationException(List<ValidationError> errors) {
    this(ValidationError.join(errors), errors);
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
