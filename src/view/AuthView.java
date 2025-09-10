// view/AuthView.java
package view;

import java.util.Scanner;

public class AuthView {
    private final Scanner scanner;

    public AuthView() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Show login menu and get user choice
     */
    public int showLoginMenu() {
        System.out.println("\n================================");
        System.out.println("    ISTAD BANKING SYSTEM");
        System.out.println("================================");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.println("3. Exit");
        System.out.print("Choose an option: ");

        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1; // Invalid input
        }
    }

    /**
     * Get login credentials from user
     */
    public LoginCredentials getLoginCredentials() {
        System.out.println("\n=== LOGIN ===");

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        return new LoginCredentials(username, password);
    }

    /**
     * Get registration data from user
     */
    public RegistrationData getRegistrationData() {
        System.out.println("\n=== CUSTOMER REGISTRATION ===");

        // Show password requirements first
        showPasswordRequirements();

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        System.out.print("Confirm Password: ");
        String confirmPassword = scanner.nextLine();

        return new RegistrationData(username, password, confirmPassword);
    }

    /**
     * Display password requirements
     */
    public void showPasswordRequirements() {
        System.out.println("\n📋 Password Requirements:");
        System.out.println("• At least 8 characters");
        System.out.println("• Contains uppercase letter");
        System.out.println("• Contains lowercase letter");
        System.out.println("• Contains digit");
        System.out.println("• Contains special character (!@#$%^&*)");
    }

    /**
     * Display success message
     */
    public void showSuccessMessage(String message) {
        System.out.println("✅ " + message);
    }

    /**
     * Display error message
     */
    public void showErrorMessage(String message) {
        System.out.println("❌ " + message);
    }

    /**
     * Display info message
     */
    public void showInfoMessage(String message) {
        System.out.println("ℹ️ " + message);
    }

    /**
     * Display loading message
     */
    public void showLoading(String message) {
        System.out.println("⏳ " + message + "...");
    }

    /**
     * Display welcome message after successful login
     */
    public void showWelcomeMessage(String username, String role) {
        System.out.println("✅ Login successful!");
        System.out.println("Welcome, " + username + "! Role: " + role);
    }

    /**
     * Display logout message
     */
    public void showLogoutMessage(String username) {
        System.out.println("👋 Goodbye, " + username + "!");
    }

    /**
     * Display invalid input message
     */
    public void showInvalidInput() {
        System.out.println("❌ Invalid input. Please try again.");
    }

    /**
     * Wait for user to press Enter
     */
    public void waitForEnter() {
        System.out.print("Press Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Clear screen (simple version)
     */
    public void clearScreen() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }

    /**
     * Get user ID for unlock account (admin function)
     */
    public int getUserIdToUnlock() {
        System.out.println("\n=== UNLOCK USER ACCOUNT ===");
        System.out.print("Enter User ID to unlock: ");

        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1; // Invalid input
        }
    }

    /**
     * Inner class for login credentials
     */
    public static class LoginCredentials {
        private final String username;
        private final String password;

        public LoginCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public boolean isValid() {
            return username != null && !username.trim().isEmpty() &&
                    password != null && !password.isEmpty();
        }
    }

    /**
     * Inner class for registration data
     */
    public static class RegistrationData {
        private final String username;
        private final String password;
        private final String confirmPassword;

        public RegistrationData(String username, String password, String confirmPassword) {
            this.username = username;
            this.password = password;
            this.confirmPassword = confirmPassword;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public boolean isValid() {
            return username != null && !username.trim().isEmpty() &&
                    password != null && !password.isEmpty() &&
                    confirmPassword != null && !confirmPassword.isEmpty();
        }

        public boolean passwordsMatch() {
            return password != null && password.equals(confirmPassword);
        }
    }
}