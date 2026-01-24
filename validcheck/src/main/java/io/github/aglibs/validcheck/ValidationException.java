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
 *   List<ValidationError> errors = e.getErrors();
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

  private static final long serialVersionUID = 1L;

  /** Immutable list of all validation errors. */
  private final List<ValidationError> errors;

  /**
   * Constructs a new ValidationException with the specified configuration.
   *
   * @param message the detail message combining all validation errors
   * @param errors the list of individual validation errors
   */
  protected ValidationException(String message, List<ValidationError> errors) {
    super(message);
    this.errors = Collections.unmodifiableList(errors);
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
