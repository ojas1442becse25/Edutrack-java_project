package util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Robust, user-friendly console input utility.
 * Wraps java.util.Scanner to catch invalid data formats, malformed types,
 * and boundary violations gracefully, re-prompting the user without crashing.
 */
public class InputValidator {

    private final Scanner scanner;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public InputValidator(Scanner scanner) {
        this.scanner = scanner != null ? scanner : new Scanner(System.in);
    }

    /**
     * Reads an integer within the specified inclusive range [min, max].
     */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println(MenuUtils.errorBanner(String.format("Please enter an integer between %d and %d.", min, max)));
            } catch (NumberFormatException e) {
                System.out.println(MenuUtils.errorBanner("Invalid number format. Please enter a valid whole integer."));
            }
        }
    }

    /**
     * Reads any valid integer.
     */
    public int readInt(String prompt) {
        return readInt(prompt, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Reads a double value within the specified inclusive range [min, max].
     */
    public double readDouble(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                double value = Double.parseDouble(input);
                if (value >= min && value <= max) {
                    return Math.round(value * 100.0) / 100.0;
                }
                System.out.println(MenuUtils.errorBanner(String.format("Please enter a decimal number between %.2f and %.2f.", min, max)));
            } catch (NumberFormatException e) {
                System.out.println(MenuUtils.errorBanner("Invalid decimal format. Please enter a valid number (e.g., 8.75)."));
            }
        }
    }

    /**
     * Reads a non-empty string, ensuring input is not blank and does not exceed maxLength.
     */
    public String readNonEmptyString(String prompt, int maxLength) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println(MenuUtils.errorBanner("This field cannot be empty."));
                continue;
            }
            if (input.length() > maxLength) {
                System.out.println(MenuUtils.errorBanner("Input exceeds maximum allowed length of " + maxLength + " characters."));
                continue;
            }
            return input;
        }
    }

    /**
     * Reads an optional string (can be empty), returning fallback if empty.
     */
    public String readOptionalString(String prompt, String defaultValue, int maxLength) {
        System.out.print(prompt + (defaultValue != null ? " [Default: " + defaultValue + "]: " : ": "));
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return defaultValue;
        }
        if (input.length() > maxLength) {
            System.out.println(MenuUtils.errorBanner("Input truncated to " + maxLength + " characters."));
            return input.substring(0, maxLength);
        }
        return input;
    }

    /**
     * Reads and validates an email address format.
     */
    public String readEmail(String prompt) {
        while (true) {
            System.out.print(prompt);
            String email = scanner.nextLine().trim();
            if (email.contains("@") && email.contains(".") && email.length() <= 100 && !email.contains(" ")) {
                return email.toLowerCase();
            }
            System.out.println(MenuUtils.errorBanner("Invalid email format (e.g., user@example.edu). Try again."));
        }
    }

    /**
     * Reads a date in yyyy-MM-dd format.
     */
    public LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt + " (yyyy-MM-dd): ");
            String input = scanner.nextLine().trim();
            try {
                return LocalDate.parse(input, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println(MenuUtils.errorBanner("Invalid date format. Please use yyyy-MM-dd (e.g., 2026-04-15)."));
            }
        }
    }

    /**
     * Reads a date-time in yyyy-MM-dd HH:mm format.
     */
    public LocalDateTime readDateTime(String prompt) {
        while (true) {
            System.out.print(prompt + " (yyyy-MM-dd HH:mm): ");
            String input = scanner.nextLine().trim();
            try {
                return LocalDateTime.parse(input, DATE_TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println(MenuUtils.errorBanner("Invalid date-time format. Please use yyyy-MM-dd HH:mm (e.g., 2026-04-15 23:59)."));
            }
        }
    }

    /**
     * Confirms yes/no choice.
     */
    public boolean readConfirmation(String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            String input = scanner.nextLine().trim().toLowerCase();
            if ("y".equals(input) || "yes".equals(input)) {
                return true;
            }
            if ("n".equals(input) || "no".equals(input)) {
                return false;
            }
            System.out.println(MenuUtils.errorBanner("Please enter 'y' for yes or 'n' for no."));
        }
    }

    /**
     * Prompts user to press Enter to continue.
     */
    public void pressEnterToContinue() {
        System.out.print("\nPress [Enter] to continue...");
        scanner.nextLine();
    }
}
