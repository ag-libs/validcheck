# ValidCheck

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=validcheck&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=validcheck)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=validcheck&metric=coverage)](https://sonarcloud.io/summary/new_code?id=validcheck)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=validcheck&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=validcheck)

A **simple**, **IDE-friendly**, **fluent** and **extensible** Java validation library for runtime parameter validation. Zero dependencies, perfect for records and constructors.

## Why ValidCheck?

**Simple** - Clean API with intuitive method names  
**IDE-friendly** - Full autocomplete support and type safety  
**Fluent** - Method chaining for readable validation code  
**Extensible** - Easy to add custom validation methods

```java
// Simple and readable
ValidCheck.require()
    .notNull(username, "username")
    .hasLength(username, 3, 20, "username")
    .matches(email, "^[\\w._%+-]+@[\\w.-]+\\.[A-Z]{2,}$", "email")
    .isPositive(age, "age");
```

## Installation

Maven:

```xml
<dependency>
  <groupId>io.github.validcheck</groupId>
  <artifactId>validcheck</artifactId>
  <version>0.9.5</version>
</dependency>
```

Gradle:

```gradle
implementation 'io.github.validcheck:validcheck:0.9.5'
```

## Quick Start

### Java Record Validation

ValidCheck is particularly powerful with Java records, where validation in the compact constructor ensures immutable objects are never created in an invalid state. This prevents invalid data from propagating through your application and makes debugging much easier since validation failures happen immediately at object creation.

### Record Validation (Fail-Fast)

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

### Record Validation (Batch - Collect All Errors)

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

## Core API Design

### Two Validation Strategies

**Fail-Fast** - Stops at first error:

```java
ValidCheck.require()  // Throws on first validation failure
    .notNull(value, "field")
    .isPositive(number, "count");
```

**Batch** - Collects all errors:

```java
ValidCheck.check()  // Collects all errors before throwing
    .notNull(value, "field")
    .isPositive(number, "count")
    .validate();  // Throws with all collected errors
```

### IDE-Friendly Method Names

All validation methods have clear, self-documenting names with excellent IDE support. As you type `ValidCheck.check().notNull(...).`, your IDE will suggest only the methods that make sense for the next validation:

```java
// Null checks
.notNull(value, "field")
.notNullOrEmpty(text, "text")  
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
.notNullOrEmpty(collection, "field")

// Assertions
.assertTrue(condition, "message")
.assertFalse(condition, "message")
```

ValidCheck provides **comprehensive validation coverage** with over 130+ validation methods including:
- All standard validation methods with 3 overloads each (named, message supplier, parameter-less)
- Conditional `nullOr*` variants for optional field validation
- BatchValidator overrides for fluent method chaining
- Single-bound `min()`/`max()` methods alongside traditional `inRange()`

The IDE autocomplete guides you to the right validation methods, making the API discoverable and reducing the need to memorize method names.

### Fluent Chaining

Chain validations naturally:

```java
ValidCheck.require()
    .notNull(user, "user")
    .notNull(user.getName(), "name")
    .hasLength(user.getName(), 1, 50, "name")
    .matches(user.getEmail(), EMAIL_PATTERN, "email")
    .isPositive(user.getAge(), "age");
```

## Advanced Features

### Conditional Validation with when()

Apply validations only when conditions are met:

```java
ValidCheck.check()
    .notNull(user, "user") 
    .when(user != null && user.isAdmin(), 
          v -> v.hasLength(user.getUsername(), 10, 30, "admin username"))
    .validate();
```

### Conditional Validation for Optional Fields

ValidCheck provides `nullOr*` methods that allow validation of optional parameters - they pass if the value is null OR meets the validation criteria:

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
            .notNullOrEmpty(username, "username")
            
            // Optional fields - null is allowed, but if present must be valid
            .nullOrNotBlank(bio, "bio")                    // null OR not blank
            .nullOrHasLength(bio, 10, 500, "bio")          // null OR 10-500 chars
            .nullOrInRange(age, 13, 120, "age")            // null OR 13-120
            .nullOrNotEmpty(skills, "skills")              // null OR not empty
            .nullOrHasSize(skills, 1, 10, "skills")        // null OR 1-10 items
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

### Message Suppliers (Lazy Evaluation)

Expensive message computation only when validation fails:

```java
ValidCheck.require()
    .assertTrue(isValid(data), () -> "Validation failed for complex data: " + data.toString());
```

### Custom Error Messages

Override default messages:

```java
ValidCheck.require()
    .notNull(value, "field", "Custom field cannot be null")
    .hasLength(text, 5, 20, "field", "Custom field must be 5-20 characters");
```

### Include Other Validators

Combine multiple validation contexts:

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

All validation methods support parameter-less versions for cleaner code:

```java
ValidCheck.require()
    .notNull(value)           // Uses "parameter" as field name
    .isPositive(number)       // "parameter must be positive"
    .hasLength(text, 5, 20);  // "parameter must have length between 5 and 20"
```

## Error Handling

### Single Error (Fail-Fast)

```java
try {
    ValidCheck.require().isPositive(-5, "age");
} catch (ValidationException e) {
    System.out.println(e.getMessage()); 
    // "'age' must be positive, but it was -5"
    
    List<String> errors = e.getErrors(); // ["'age' must be positive, but it was -5"]
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
    // "'username' must not be null; 'age' must be positive, but it was -1"
    
    List<String> errors = e.getErrors();
    // ["'username' must not be null", "'age' must be positive, but it was -1"]
}
```

## Extensibility

The library is designed to be easily extensible. You can extend the `Validator` and `BatchValidator` classes to add domain-specific validation methods. See the Javadoc in the source code for detailed examples of creating custom validators.

## Examples

Complete examples available in the [examples module](validcheck-examples/):

- [User Registration](validcheck-examples/src/main/java/io/github/validcheck/example/UserRegistrationExample.java) - Record validation with batch processing, conditional validation for optional fields, and single-bound validation examples

## Requirements

- Java 11+
- Zero dependencies

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
