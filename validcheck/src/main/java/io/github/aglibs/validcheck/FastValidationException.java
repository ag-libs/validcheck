package io.github.aglibs.validcheck;

import java.util.List;

/**
 * A validation exception that does not fill stack traces.
 *
 * @since 1.0.0
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
   * @param safeForClient whether error messages are safe for client/API responses
   */
  public FastValidationException(
      String message, List<ValidationError> errors, boolean safeForClient) {
    super(message, errors, safeForClient);
  }

  /**
   * Constructs a new FastValidationException with the specified configuration.
   *
   * @param message the detail message combining all validation errors
   * @param name the name of the field
   * @param safeForClient whether it is safe to include error messages in API responses
   */
  public FastValidationException(String name, String message, boolean safeForClient) {
    super(name, message, safeForClient, null);
  }

  /**
   * Constructs a new FastValidationException with the specified configuration.
   *
   * @param message the detail message combining all validation errors
   * @param safeForClient whether it is safe to include error messages in API responses
   */
  public FastValidationException(String message, boolean safeForClient) {
    super(message, safeForClient);
  }

  /**
   * Overrides to skip stack trace filling.
   *
   * @return this throwable instance
   */
  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }
}
