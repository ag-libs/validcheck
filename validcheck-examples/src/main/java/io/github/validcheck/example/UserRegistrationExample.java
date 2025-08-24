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
  }

  /** Enhanced user profile record demonstrating conditional validation for optional fields. */
  public record UserProfile(
      String username,
      String email,
      String bio, // Optional field
      Integer age, // Optional field
      String website, // Optional field
      List<String> skills, // Optional field
      Double salary, // Optional field
      Integer accountBalance, // Optional field - can be zero or positive
      Double temperatureChange, // Optional field - can be zero or negative
      Integer experienceYears, // Optional field - minimum years required
      Double maxDiscount) { // Optional field - maximum discount allowed

    public UserProfile {
      // Validation with conditional methods for optional parameters
      ValidCheck.check()
          // Required fields - must be present and valid
          .notNullOrEmpty(username, "username")
          .hasLength(username, 3, 30, "username")
          .matches(email, "(?i)^[\\w._%+-]+@[\\w.-]+\\.[A-Z]{2,}$", "email")

          // Optional fields - null is allowed, but if present must be valid
          .nullOrNotBlank(bio, "bio") // bio can be null, but if present must not be blank
          .nullOrHasLength(
              bio, 10, 500, "bio") // bio can be null, but if present must be 10-500 chars
          .nullOrInRange(age, 13, 120, "age") // age can be null, but if present must be 13-120
          .nullOrMatches(
              website,
              "^https?://.*",
              "website") // website can be null, but if present must be valid URL
          .nullOrNotEmpty(skills, "skills") // skills can be null, but if present must not be empty
          .nullOrHasSize(
              skills, 1, 20, "skills") // skills can be null, but if present must have 1-20 items
          .nullOrIsPositive(salary, "salary") // salary can be null, but if present must be > 0
          .nullOrIsNonNegative(
              accountBalance,
              "accountBalance") // account balance can be null, but if present must be >= 0
          .nullOrIsNonPositive(
              temperatureChange,
              "temperatureChange") // temperature change can be null, but if present must be <= 0
          .nullOrMin(
              experienceYears,
              2,
              "experienceYears") // experience can be null, but if present must be >= 2
          .nullOrMax(
              maxDiscount,
              50.0,
              "maxDiscount") // max discount can be null, but if present must be <= 50%
          .validate();
    }
  }

  private static boolean isAdult(LocalDate birthDate) {
    return birthDate != null && Period.between(birthDate, LocalDate.now()).getYears() >= 13;
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
    try {
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
    } catch (Exception e) {
      System.out.println("Registration failed: " + e.getMessage());
    }

    // Demonstrating conditional validation with UserProfile

    // Valid profile with all optional fields present
    UserProfile fullProfile =
        new UserProfile(
            "alice_dev",
            "alice@example.com",
            "Passionate software developer with 5 years of experience", // bio present and valid
            28, // age present and valid
            "https://alice-dev.com", // website present and valid
            List.of("Java", "Spring", "React"), // skills present and valid
            75000.0, // salary present and valid (> 0)
            1000, // account balance present and valid (>= 0)
            -2.5, // temperature change present and valid (<= 0)
            5, // experience years present and valid (>= 2)
            25.0 // max discount present and valid (<= 50%)
            );

    // Valid profile with some optional fields null and zero values
    UserProfile partialProfile =
        new UserProfile(
            "bob_coder",
            "bob@example.com",
            null, // bio is null - allowed
            null, // age is null - allowed
            null, // website is null - allowed
            List.of("Python", "Django"), // skills present and valid
            null, // salary is null - allowed
            0, // account balance is zero - valid for isNonNegative (>= 0)
            0.0, // temperature change is zero - valid for isNonPositive (<= 0)
            null, // experience years is null - allowed
            null // max discount is null - allowed
            );

    // Invalid profile - optional fields present but invalid
    try {
      UserProfile invalidProfile =
          new UserProfile(
              "x", // username too short
              "not-an-email", // email invalid format
              "   ", // bio blank (not null but invalid)
              5, // age too young
              "not-a-url", // website invalid format
              List.of(), // skills empty (not null but invalid)
              -1000.0, // salary negative (not null but invalid, must be > 0)
              -500, // account balance negative (not null but invalid, must be >= 0)
              5.0, // temperature change positive (not null but invalid, must be <= 0)
              1, // experience years too low (not null but invalid, must be >= 2)
              75.0 // max discount too high (not null but invalid, must be <= 50%)
              );
    } catch (Exception e) {
      System.out.println("Profile validation failed: " + e.getMessage());
    }

    // Demonstrate semantic differences between sign validation methods
    try {
      // This would fail because isPositive requires > 0, but 0 is not > 0
      ValidCheck.require().isPositive(0, "strictlyPositiveValue");
    } catch (Exception e) {
      System.out.println("isPositive(0) failed: " + e.getMessage());
    }

    // This passes because isNonNegative allows >= 0, and 0 is >= 0
    ValidCheck.require().isNonNegative(0, "nonNegativeValue");
    System.out.println("isNonNegative(0) passed - allows zero!");

    // Demonstrate single-bound validation benefits
    System.out.println("\n--- Single-bound validation examples ---");

    // Minimum age validation (no upper limit needed)
    ValidCheck.require().min(25, 18, "age");
    System.out.println("min(25, 18) passed - age is at least 18");

    // Maximum discount validation (no lower limit needed)
    ValidCheck.require().max(15.0, 50.0, "discount");
    System.out.println("max(15.0, 50.0) passed - discount is at most 50%");

    // Compare with awkward range validation requiring artificial bounds
    ValidCheck.require().inRange(25, 18, Integer.MAX_VALUE, "age"); // Awkward!
    ValidCheck.require().inRange(15.0, Double.MIN_VALUE, 50.0, "discount"); // Awkward!

    System.out.println("Single-bound validation is cleaner than artificial range bounds!");
  }
}
