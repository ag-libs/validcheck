package io.github.validcheck.example;

import io.github.validcheck.ValidCheck;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

/** Real-world example demonstrating record validation for user registration. */
public class UserRegistrationExample {

  /** User registration record with fail-fast validation in constructor. */
  public record UserRegistration(
      String username,
      String email,
      String password,
      String firstName,
      String lastName,
      LocalDate birthDate,
      List<String> interests) {
    public UserRegistration {
      // Batch validation - collects all errors before throwing
      ValidCheck.check()
          .notNullOrEmpty(username, "username")
          .hasLength(username, 3, 30, "username")
          .matches(email, "(?i)^[\\w._%+-]+@[\\w.-]+\\.[A-Z]{2,}$", "email")
          .hasLength(password, 8, 100, "password")
          .matches(password, ".*[A-Z].*", "password")
          .notBlank(firstName, "firstName")
          .notBlank(lastName, "lastName")
          .assertTrue(
              isAdult(birthDate), () -> "user born " + birthDate + " must be at least 13 years old")
          .hasSize(interests, 1, 10, "interests")
          .validate();
    }

    private static boolean isAdult(LocalDate birthDate) {
      return birthDate != null && Period.between(birthDate, LocalDate.now()).getYears() >= 13;
    }
  }

  @SuppressWarnings("unused")
  public static void main(String[] args) {
    // Valid user registration
    UserRegistration validUser =
        new UserRegistration(
            "john_doe",
            "john@example.com",
            "StrongPass123",
            "John",
            "Doe",
            LocalDate.of(1990, 5, 15),
            List.of("coding", "music"));

    // Invalid user registration - will collect all errors
    UserRegistration invalidUser =
        new UserRegistration(
            "ab", // Too short
            "invalid-email", // Invalid format
            "weak", // Too short, no uppercase
            "", // Blank
            "", // Blank
            LocalDate.of(2020, 1, 1), // Too young
            List.of() // Empty
            );
  }
}
