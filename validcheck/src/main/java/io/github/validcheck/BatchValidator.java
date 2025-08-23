package io.github.validcheck;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * A batch validator that collects multiple validation errors before throwing. Extends {@link
 * Validator} with the ability to explicitly trigger validation after collecting errors.
 *
 * <p>This validator is designed for scenarios where you want to collect all validation failures and
 * report them together, rather than failing on the first error:
 *
 * <pre>{@code
 * BatchValidator validator = ValidCheck.check();
 * validator.notNull(name, "name")
 *          .notBlank(name, "name")
 *          .hasLength(name, 1, 50, "name")
 *          .isPositive(age, "age")
 *          .validate(); // Throws ValidationException with all collected errors
 * }</pre>
 *
 * <p>The validator also supports conditional validation:
 *
 * <pre>{@code
 * ValidCheck.check()
 *          .when(updateMode, v -> v.notNull(id, "id"))
 *          .notBlank(name, "name")
 *          .validate();
 * }</pre>
 *
 * <p>Multiple validators can be combined:
 *
 * <pre>{@code
 * BatchValidator addressValidator = ValidCheck.check()
 *     .notBlank(street, "street")
 *     .notBlank(city, "city");
 *
 * ValidCheck.check()
 *          .notBlank(name, "name")
 *          .include(addressValidator)
 *          .validate();
 * }</pre>
 *
 * <p>Error inspection without throwing exceptions:
 *
 * <pre>{@code
 * BatchValidator validator = ValidCheck.check()
 *     .notNull(name, "name")
 *     .isPositive(age, "age");
 *
 * if (!validator.isValid()) {
 *     List<String> errors = validator.getErrors();
 *     // Handle errors without exception
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see ValidCheck#check()
 * @see Validator
 * @see ValidationException
 */
public class BatchValidator extends Validator {

  /**
   * Constructs a new BatchValidator with the specified configuration.
   *
   * @param includeValues whether to include actual values in error messages for debugging
   * @param fillStackTrace whether to fill stack traces in thrown exceptions
   */
  protected BatchValidator(boolean includeValues, boolean fillStackTrace) {
    super(includeValues, false, fillStackTrace);
  }

  /**
   * Includes all validation errors from another BatchValidator into this one. This allows combining
   * validation results from multiple validators.
   *
   * @param validator the validator whose errors should be included
   * @return this validator instance for method chaining
   */
  public BatchValidator include(BatchValidator validator) {
    errors.addAll(validator.errors);
    return this;
  }

  /**
   * Conditionally applies validation rules based on the specified condition. If the condition is
   * true, the provided validation block is executed.
   *
   * @param condition the condition to evaluate
   * @param validations the validation block to execute if condition is true
   * @return this validator instance for method chaining
   */
  public BatchValidator when(boolean condition, Consumer<BatchValidator> validations) {
    if (condition) {
      validations.accept(this);
    }

    return this;
  }

  /**
   * Returns the list of validation error messages collected so far. This method allows inspection
   * of validation errors before or instead of calling {@link #validate()}.
   *
   * @return a list of the current validation error messages
   */
  public List<String> getErrors() {
    return errors;
  }

  /**
   * Checks if all validations performed so far have passed. This method provides a way to check
   * validation status without throwing an exception.
   *
   * @return {@code true} if no validation errors have been recorded, {@code false} otherwise
   */
  public boolean isValid() {
    return errors.isEmpty();
  }

  /**
   * {@inheritDoc}
   *
   * <p>Validates that no errors have been collected and throws an exception if any exist.
   */
  @Override
  public void validate() {
    super.validate();
  }

  @Override
  public BatchValidator assertTrue(boolean condition, String message) {
    return (BatchValidator) super.assertTrue(condition, message);
  }

  @Override
  public BatchValidator assertTrue(boolean condition, Supplier<String> messageSupplier) {
    return (BatchValidator) super.assertTrue(condition, messageSupplier);
  }

  @Override
  public BatchValidator assertFalse(boolean condition, String message) {
    return (BatchValidator) super.assertFalse(condition, message);
  }

  @Override
  public BatchValidator assertFalse(boolean condition, Supplier<String> messageSupplier) {
    return (BatchValidator) super.assertFalse(condition, messageSupplier);
  }

  @Override
  public BatchValidator notNull(Object value, String name) {
    return (BatchValidator) super.notNull(value, name);
  }

  @Override
  public BatchValidator notNull(Object value, Supplier<String> messageSupplier) {
    return (BatchValidator) super.notNull(value, messageSupplier);
  }

  @Override
  public BatchValidator notNull(Object value) {
    return (BatchValidator) super.notNull(value);
  }

  @Override
  public <T extends Number> BatchValidator inRange(T value, T min, T max, String name) {
    return (BatchValidator) super.inRange(value, min, max, name);
  }

  @Override
  public <T extends Number> BatchValidator inRange(
      T value, T min, T max, Supplier<String> messageSupplier) {
    return (BatchValidator) super.inRange(value, min, max, messageSupplier);
  }

  @Override
  public <T extends Number> BatchValidator inRange(T value, T min, T max) {
    return (BatchValidator) super.inRange(value, min, max);
  }

  @Override
  public BatchValidator notNullOrEmpty(String value, String name) {
    return (BatchValidator) super.notNullOrEmpty(value, name);
  }

  @Override
  public BatchValidator notNullOrEmpty(String value, Supplier<String> messageSupplier) {
    return (BatchValidator) super.notNullOrEmpty(value, messageSupplier);
  }

  @Override
  public BatchValidator notNullOrEmpty(String value) {
    return (BatchValidator) super.notNullOrEmpty(value);
  }

  @Override
  public BatchValidator notNullOrEmpty(Collection<?> value, String name) {
    return (BatchValidator) super.notNullOrEmpty(value, name);
  }

  @Override
  public BatchValidator notNullOrEmpty(Collection<?> value, Supplier<String> messageSupplier) {
    return (BatchValidator) super.notNullOrEmpty(value, messageSupplier);
  }

  @Override
  public BatchValidator notNullOrEmpty(Collection<?> value) {
    return (BatchValidator) super.notNullOrEmpty(value);
  }

  @Override
  public BatchValidator notNullOrEmpty(Map<?, ?> value, String name) {
    return (BatchValidator) super.notNullOrEmpty(value, name);
  }

  @Override
  public BatchValidator notNullOrEmpty(Map<?, ?> value, Supplier<String> messageSupplier) {
    return (BatchValidator) super.notNullOrEmpty(value, messageSupplier);
  }

  @Override
  public BatchValidator notNullOrEmpty(Map<?, ?> value) {
    return (BatchValidator) super.notNullOrEmpty(value);
  }

  @Override
  public BatchValidator notBlank(String value, String name) {
    return (BatchValidator) super.notBlank(value, name);
  }

  @Override
  public BatchValidator notBlank(String value, Supplier<String> messageSupplier) {
    return (BatchValidator) super.notBlank(value, messageSupplier);
  }

  @Override
  public BatchValidator notBlank(String value) {
    return (BatchValidator) super.notBlank(value);
  }

  @Override
  public BatchValidator hasLength(String value, int minLength, int maxLength, String name) {
    return (BatchValidator) super.hasLength(value, minLength, maxLength, name);
  }

  @Override
  public BatchValidator hasLength(
      String value, int minLength, int maxLength, Supplier<String> messageSupplier) {
    return (BatchValidator) super.hasLength(value, minLength, maxLength, messageSupplier);
  }

  @Override
  public BatchValidator hasLength(String value, int minLength, int maxLength) {
    return (BatchValidator) super.hasLength(value, minLength, maxLength);
  }

  @Override
  public BatchValidator hasSize(Collection<?> value, int minSize, int maxSize, String name) {
    return (BatchValidator) super.hasSize(value, minSize, maxSize, name);
  }

  @Override
  public BatchValidator hasSize(
      Collection<?> value, int minSize, int maxSize, Supplier<String> messageSupplier) {
    return (BatchValidator) super.hasSize(value, minSize, maxSize, messageSupplier);
  }

  @Override
  public BatchValidator hasSize(Collection<?> value, int minSize, int maxSize) {
    return (BatchValidator) super.hasSize(value, minSize, maxSize);
  }

  @Override
  public BatchValidator isPositive(Number value, String name) {
    return (BatchValidator) super.isPositive(value, name);
  }

  @Override
  public BatchValidator isPositive(Number value, Supplier<String> messageSupplier) {
    return (BatchValidator) super.isPositive(value, messageSupplier);
  }

  @Override
  public BatchValidator isPositive(Number value) {
    return (BatchValidator) super.isPositive(value);
  }

  @Override
  public BatchValidator isNegative(Number value, String name) {
    return (BatchValidator) super.isNegative(value, name);
  }

  @Override
  public BatchValidator isNegative(Number value, Supplier<String> messageSupplier) {
    return (BatchValidator) super.isNegative(value, messageSupplier);
  }

  @Override
  public BatchValidator isNegative(Number value) {
    return (BatchValidator) super.isNegative(value);
  }

  @Override
  public BatchValidator matches(String value, String regex, String name) {
    return (BatchValidator) super.matches(value, regex, name);
  }

  @Override
  public BatchValidator matches(String value, String regex, Supplier<String> messageSupplier) {
    return (BatchValidator) super.matches(value, regex, messageSupplier);
  }

  @Override
  public BatchValidator matches(String value, String regex) {
    return (BatchValidator) super.matches(value, regex);
  }

  @Override
  public BatchValidator matches(String value, Pattern regex, String name) {
    return (BatchValidator) super.matches(value, regex, name);
  }

  @Override
  public BatchValidator matches(String value, Pattern regex, Supplier<String> messageSupplier) {
    return (BatchValidator) super.matches(value, regex, messageSupplier);
  }

  @Override
  public BatchValidator matches(String value, Pattern regex) {
    return (BatchValidator) super.matches(value, regex);
  }
}
