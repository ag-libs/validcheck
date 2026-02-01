package io.github.aglibs.validcheck;

import java.util.List;

/**
 * A validation exception without stack traces for high-throughput scenarios.
 *
 * <p>This exception is identical to {@link ValidationException} but skips stack trace generation,
 * making it more performant in scenarios where stack traces are not needed (e.g., API request
 * validation, high-frequency validation in hot paths).
 *
 * <p>Use this exception with {@link ValidCheck#requireWith(java.util.function.Function)} or {@link
 * ValidCheck#checkWith(java.util.function.Function)}:
 *
 * <pre>{@code
 * // Fail-fast validation without stack traces
 * ValidCheck.requireWith(FastValidationException::new)
 *     .notNull(apiKey, "apiKey")
 *     .hasLength(apiKey, 32, 64, "apiKey");
 *
 * // Batch validation without stack traces
 * ValidCheck.checkWith(FastValidationException::new)
 *     .notNull(username, "username")
 *     .isPositive(age, "age")
 *     .validate();
 * }</pre>
 *
 * @since 1.0.0
 * @see ValidationException
 * @see ValidCheck#requireWith(java.util.function.Function)
 * @see ValidCheck#checkWith(java.util.function.Function)
 */
public final class FastValidationException extends ValidationException {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new FastValidationException with custom message.
   *
   * @param message the detail message combining all validation errors
   * @param errors the list of individual validation errors
   */
  public FastValidationException(String message, List<ValidationError> errors) {
    super(message, errors);
  }

  /**
   * Constructs a new FastValidationException with auto-generated message using {@link
   * ValidationError#join(List)}.
   *
   * @param errors the list of individual validation errors
   */
  public FastValidationException(List<ValidationError> errors) {
    super(errors);
  }

  /**
   * Overrides to skip stack trace filling for improved performance.
   *
   * @return this throwable instance without filled stack trace
   */
  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }
}
