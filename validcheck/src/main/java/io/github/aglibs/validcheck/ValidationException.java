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
   * Indicates whether it is safe to include error messages in API responses. When true, error
   * messages do not contain sensitive data and can be exposed to clients.
   */
  private final boolean safeForClient;

  /**
   * Constructs a new ValidationException with the specified configuration.
   *
   * @param message the detail message combining all validation errors
   * @param errors the list of individual validation errors
   * @param safeForClient whether it is safe to include error messages in API responses
   */
  protected ValidationException(
      String message, List<ValidationError> errors, boolean safeForClient) {
    super(message);
    this.errors = Collections.unmodifiableList(errors);
    this.safeForClient = safeForClient;
  }

  /**
   * Returns the list of individual validation errors.
   *
   * @return an unmodifiable list of validation errors
   */
  public List<ValidationError> getErrors() {
    return errors;
  }

  /**
   * Returns whether error messages are safe for client/API responses.
   *
   * @return true if values are not included in error messages, false otherwise
   */
  public boolean isSafeForClient() {
    return safeForClient;
  }
}
