# ValidCheck

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ag-libs_validcheck&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ag-libs_validcheck)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ag-libs_validcheck&metric=coverage)](https://sonarcloud.io/summary/new_code?id=ag-libs_validcheck)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=ag-libs_validcheck&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=ag-libs_validcheck)

A Java validation library for runtime parameter validation with zero dependencies.

Fluent, opinionated API for validating Java Records. Method chains align nicely when
formatted with Google Java Format.

## Installation

Maven:

```xml
<dependency>
  <groupId>io.github.ag-libs.validcheck</groupId>
  <artifactId>validcheck</artifactId>
  <version>0.10.1</version>
</dependency>
```

Gradle:

```gradle
implementation 'io.github.ag-libs.validcheck:validcheck:0.10.1'
```

## Usage

### Record Validation

Use ValidCheck in record compact constructors to validate constructor parameters.

**Fail-Fast** - Throws on first validation failure:

```java
public record User(String username, String email, int age) {
    public User {
        ValidCheck.require()
            .notNull(username, "username")
            .hasLength(username, 3, 20, "username")
            .matches(email, "(?i)^[\\w._%+-]+@[\\w.-]+\\.[A-Z]{2,}$", "email")
            .inRange(age, 13, 120, "age");
    }
}
```

Error messages do not include actual values, making them safe for logs and API responses.

**Batch** - Collects all errors before throwing:

```java
public record UserRegistration(String username, String email, String password) {
    public UserRegistration {
        ValidCheck.check()
            .notNull(username, "username")
            .hasLength(username, 3, 20, "username")
            .matches(email, "(?i)^[\\w._%+-]+@[\\w.-]+\\.[A-Z]{2,}$", "email")
            .hasLength(password, 8, 100, "password")
            .matches(password, ".*[A-Z].*", "password")
            .validate();
    }
}
```

## Validation Strategies

ValidCheck supports two validation strategies:

**Fail-Fast** - Throws on first validation failure:

```java
ValidCheck.require()
    .notNull(value, "field")
    .isPositive(number, "count");
```

**Batch** - Collects all errors before throwing:

```java
ValidCheck.check()
    .notNull(value, "field")
    .isPositive(number, "count")
    .validate();
```

## Validation Methods

```java
// Null checks
.notNull(value, "field")
.isNull(value, "field")
.notEmpty(text, "text")  
.notBlank(text, "text")

// Range validation  
.inRange(number, min, max, "field")
.isPositive(number, "field")        // > 0 (excludes zero)
.isNegative(number, "field")        // < 0 (excludes zero)
.isNonNegative(number, "field")     // >= 0 (includes zero)
.isNonPositive(number, "field")     // <= 0 (includes zero)
.min(number, minValue, "field")     // >= minValue (single bound)
.max(number, maxValue, "field")     // <= maxValue (single bound)

// String validation
.hasLength(text, min, max, "field")
.matches(text, pattern, "field")

// Collection validation
.hasSize(collection, min, max, "field")
.notEmpty(collection, "field")

// Assertions
.assertTrue(condition, "message")
.assertFalse(condition, "message")
```

Each validation method has three overloads:
- Named: `.notNull(value, "fieldName")`
- Message supplier: `.notNull(value, () -> "custom message")`
- Parameter-less: `.notNull(value)` - uses "parameter" as field name

### Method Chaining

Validation methods return the validator instance for chaining:

```java
ValidCheck.require()
    .notNull(user, "user")
    .notNull(user.getName(), "name")
    .hasLength(user.getName(), 1, 50, "name")
    .matches(user.getEmail(), EMAIL_PATTERN, "email")
    .isPositive(user.getAge(), "age");
```

## Advanced Features

### Conditional Validation

Use `when()` to apply validations conditionally:

```java
ValidCheck.check()
    .notNull(user, "user") 
    .when(user != null && user.isAdmin(), 
          v -> v.hasLength(user.getUsername(), 10, 30, "admin username"))
    .validate();
```

### Validating Optional Fields

Use `nullOr*` methods to validate values that may be null. These methods pass if the value
is null or satisfies the validation:

```java
public record UserProfile(
    String username,    // Required
    String bio,         // Optional - can be null
    Integer age,        // Optional - can be null
    List<String> skills // Optional - can be null
) {
    public UserProfile {
        ValidCheck.check()
            // Required fields
            .notEmpty(username, "username")
            
            // Optional fields - null is allowed, but if present must be valid
            .nullOrNotBlank(bio, "bio")              // null OR not blank
            .nullOrHasLength(bio, 10, 500, "bio")    // null OR 10-500 chars
            .nullOrInRange(age, 13, 120, "age")      // null OR 13-120
            .nullOrNotEmpty(skills, "skills")        // null OR not empty
            .nullOrHasSize(skills, 1, 10, "skills")  // null OR 1-10 items
            .validate();
    }
}
```

Available conditional methods:
- `nullOrNotEmpty()` - String, Collection, Map variants
- `nullOrNotBlank()` - String validation
- `nullOrHasLength()` - String length validation
- `nullOrHasSize()` - Collection size validation
- `nullOrInRange()` - Numeric range validation
- `nullOrIsPositive()` / `nullOrIsNegative()` - Sign validation
- `nullOrIsNonNegative()` / `nullOrIsNonPositive()` - Sign validation (includes zero)
- `nullOrMatches()` - Pattern matching
- `nullOrMin()` / `nullOrMax()` - Single-bound validation

### Message Suppliers

Message suppliers are evaluated only when validation fails:

```java
ValidCheck.require()
    .assertTrue(isValid(data), 
        () -> "Validation failed for complex data: " + data.toString());
```

### Custom Error Messages

Use message suppliers to override default error messages:

```java
ValidCheck.require()
    .notNull(value, () -> "Custom field cannot be null")
    .hasLength(text, 5, 20, () -> "Custom field must be 5-20 characters");
```

### Combining Validators

Use `include()` to combine multiple validation contexts:

```java
BatchValidator userValidator = ValidCheck.check()
    .notNull(username, "username");

BatchValidator emailValidator = ValidCheck.check() 
    .matches(email, EMAIL_PATTERN, "email");

ValidCheck.check()
    .include(userValidator)
    .include(emailValidator)
    .validate();
```

## Parameter-less Methods

Validation methods can be called without a field name. The error message will use
"parameter" as the field name:

```java
ValidCheck.require()
    .notNull(value)           // "parameter must not be null"
    .isPositive(number)       // "parameter must be positive"
    .hasLength(text, 5, 20);  // "parameter must have length between 5 and 20"
```

## Error Handling

All validation failures throw `ValidationException` (or `FastValidationException` if configured)
which contains structured error information.

### Single Error (Fail-Fast)

```java
try {
    ValidCheck.require().isPositive(-5, "age");
} catch (ValidationException e) {
    System.out.println(e.getMessage()); 
    // "'age' must be positive"
    
    List<ValidationError> errors = e.getErrors();
    // [ValidationError{field="age", message="must be positive"}]
}
```

### Multiple Errors (Batch)

```java
try {
    ValidCheck.check()
        .notNull(null, "username")
        .isPositive(-1, "age")
        .validate();
} catch (ValidationException e) {
    System.out.println(e.getMessage());
    // "'username' must not be null; 'age' must be positive"
    
    List<ValidationError> errors = e.getErrors();
    // [ValidationError{field="username", message="must not be null"},
    //  ValidationError{field="age", message="must be positive"}]
}
```

### Working with ValidationError

`ValidationError` provides structured access to field names and error messages:

```java
try {
    ValidCheck.check()
        .notNull(null, "username")
        .isPositive(-1, "age")
        .validate();
} catch (ValidationException e) {
    // Access structured error information
    for (ValidationError error : e.getErrors()) {
        String field = error.field();      // "username", "age"
        String message = error.message();  // "must not be null", ...
        String formatted = error.toString(); // "'username' must not be null"
    }
    
    // Group errors by field for API responses
    Map<String, List<String>> errorsByField = e.getErrors().stream()
        .filter(err -> err.field() != null)
        .collect(Collectors.groupingBy(
            ValidationError::field,
            Collectors.mapping(ValidationError::message, Collectors.toList())
        ));
    // {"username": ["must not be null"], 
    //  "age": ["must be positive"]}
}
```

## Extensibility

### Custom Exception Types

ValidCheck allows you to throw custom exception types instead of the default
`ValidationException`. Pass an exception factory function to `requireWith()` or `checkWith()`:

```java
// Fail-fast with IllegalArgumentException
ValidCheck.requireWith(errors -> new IllegalArgumentException(ValidationError.join(errors)))
    .notNull(null, "value")
    .isPositive(-1, "count"); // throws IllegalArgumentException

// Batch validation with custom exception and formatting
ValidCheck.checkWith(errors -> {
    String message = errors.stream()
        .map(e -> e.field() + ": " + e.message())
        .collect(Collectors.joining("\n- ", 
            "Validation failed:\n- ", ""));
    return new MyCustomException(message);
})
    .notNull(null, "username")
    .isPositive(-1, "age")
    .validate(); // throws MyCustomException
```

The exception factory receives a `List<ValidationError>` with structured error information:
- `field()` - the field name or null
- `message()` - the error message without field name
- `toString()` - formatted as "'field' message"
- `join(errors)` - convenience method to join all errors with "; " separator

This approach is useful when:
- Integrating with frameworks expecting specific exceptions (Spring's `IllegalArgumentException`, Jakarta Bean Validation)
- Building REST APIs that need custom error response formats
- Adding correlation IDs or context to exceptions

### FastValidationException - High-Performance Validation

For rare high-throughput scenarios where stack traces are not needed, use `FastValidationException`:

```java
// Fail-fast without stack traces (better performance)
ValidCheck.requireWith(FastValidationException::new)
    .notNull(apiKey, "apiKey")
    .hasLength(apiKey, 32, 64, "apiKey");

// Batch validation without stack traces
ValidCheck.checkWith(FastValidationException::new)
    .notNull(username, "username")
    .isPositive(age, "age")
    .validate();
```

`FastValidationException` skips stack trace generation, improving performance in:
- High-frequency API request validation
- Performance-critical validation paths

## Examples

Complete examples available in the [examples module](validcheck-examples/):

- [User Registration](validcheck-examples/src/main/java/io/github/aglibs/validcheck/example/UserRegistrationExample.java) -
  Record validation with batch processing, conditional validation for optional fields, and
  single-bound validation examples

## Requirements

- Java 11+
- Zero dependencies

## AI Disclosure

This project uses AI assistance in development. See [AI.md](AI.md) for details.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
