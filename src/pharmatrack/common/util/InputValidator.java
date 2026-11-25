package pharmatrack.common.util;

import pharmatrack.common.exception.ValidationException;

import java.util.Scanner;

public class InputValidator {

    private InputValidator() {
    }

    public static String requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be null or empty.");
        }
        return value.trim();
    }

    public static <T> T requireNotNull(T object, String fieldName) {
        if (object == null) {
            throw new ValidationException(fieldName + " cannot be null.");
        }
        return object;
    }

    public static int requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new ValidationException(fieldName + " must be strictly positive (> 0). Given: " + value);
        }
        return value;
    }

    public static double requirePositive(double value, String fieldName) {
        if (value <= 0.0) {
            throw new ValidationException(fieldName + " must be strictly positive (> 0). Given: " + value);
        }
        return value;
    }

    public static void requireValidTemperatureRange(double minTemp, double maxTemp) {
        if (minTemp >= maxTemp) {
            throw new ValidationException(String.format(
                    "Invalid temperature range: min temperature (%.2f°C) must be strictly less than max temperature (%.2f°C).",
                    minTemp, maxTemp));
        }
    }

    public static String readString(Scanner scanner, String prompt, boolean allowEmpty) {
        while (true) {
            System.out.print(prompt + ": ");
            String input = scanner.nextLine();
            if (input != null) {
                input = input.trim();
            }
            if (!allowEmpty && (input == null || input.isEmpty())) {
                System.out.println("  [!] Input cannot be empty. Please enter a valid value.");
                continue;
            }
            return input;
        }
    }

    public static int readInt(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt + " (" + min + "-" + max + "): ");
            String line = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value < min || value > max) {
                    System.out.printf("  [!] Value must be between %d and %d.%n", min, max);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid number. Please enter an integer.");
            }
        }
    }

    public static double readDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            String line = scanner.nextLine().trim();
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid number. Please enter a valid decimal number.");
            }
        }
    }
}
