package io.github.aglibs.validcheck;

import java.util.Objects;

/**
 * Represents a single validation error with an optional field name and error message.
 *
 * <p>This class encapsulates validation failure information, making it easier to programmatically
 * handle and format validation errors. The field name is optional and may be null for errors that
 * are not associated with a specific field (e.g., assertion errors).
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * try {
 *   ValidCheck.check()
 *       .notNull(username, "username")
 *       .isPositive(age, "age")
 *       .validate();
 * } catch (ValidationException e) {
 *   List<ValidationError> errors = e.getErrors();
 *
 *   // Access field and message separately
 *   for (ValidationError error : errors) {
 *     String field = error.field();    // "username", "age", or null
 *     String message = error.message(); // "must not be null", "must be positive"
 *   }
 *
 *   // Group errors by field for API responses
 *   Map<String, List<String>> errorMap = errors.stream()
 *       .filter(err -> err.field() != null)
 *       .collect(Collectors.groupingBy(
 *           ValidationError::field,
 *           Collectors.mapping(ValidationError::message, Collectors.toList())
 *       ));
 * }
 * }</pre>
 *
 * @since 0.9.10
 * @see ValidationException
 * @see Validator
 * @see BatchValidator
 */
public final class ValidationError {

  private final String field;
  private final String message;

  /**
   * Constructs a new ValidationError with the specified field name and message.
   *
   * @param field the name of the field that failed validation, or null for non-field-specific
   *     errors
   * @param message the validation error message describing what constraint was violated
   */
  public ValidationError(String field, String message) {
    this.field = field;
    this.message = message;
  }

  /**
   * Returns the name of the field that failed validation.
   *
   * @return the field name, or null for non-field-specific errors
   */
  public String field() {
    return field;
  }

  /**
   * Returns the validation error message.
   *
   * @return the error message describing what constraint was violated
   */
  public String message() {
    return message;
  }

  /**
   * Returns a formatted string representation of this validation error. If the field name is
   * present, the format is "'field' message", otherwise just the message is returned.
   *
   * @return a formatted error string suitable for logging and user display
   */
  @Override
  public String toString() {
    return field != null ? String.format("'%s' %s", field, message) : message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ValidationError that = (ValidationError) o;
    return Objects.equals(field, that.field) && Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(field, message);
  }
}
