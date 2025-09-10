// controller/AuthController.java
package controller;

import model.entity.User;
import model.service.AuthService;
import view.AuthView;

public class AuthController {
    private final AuthService authService;
    private final AuthView authView;
    private User currentUser;

    public AuthController() {
        this.authService = new AuthService();
        this.authView = new AuthView();
        this.currentUser = null;
    }

    /**
     * Handle the authentication flow
     * @return authenticated user or null if user chooses to exit
     */
    public User handleAuthentication() {
        while (true) {
            int choice = authView.showLoginMenu();

            switch (choice) {
                case 1:
                    if (handleLogin()) {
                        return currentUser; // Return authenticated user
                    }
                    break;
                case 2:
                    handleRegistration();
                    break;
                case 3:
                    authView.showInfoMessage("Thank you for using ISTAD Banking System. Goodbye!");
                    System.exit(0);
                    break;
                default:
                    authView.showInvalidInput();
            }
        }
    }

    /**
     * Handle login process
     */
    private boolean handleLogin() {
        try {
            // Get credentials from user
            AuthView.LoginCredentials credentials = authView.getLoginCredentials();

            // Validate input
            if (!credentials.isValid()) {
                authView.showErrorMessage("Username and password cannot be empty!");
                authView.waitForEnter();
                return false;
            }

            // Attempt login
            authView.showLoading("Authenticating");
            AuthService.AuthResult result = authService.login(
                    credentials.getUsername(),
                    credentials.getPassword()
            );

            if (result.isSuccess()) {
                currentUser = result.getUser();
                authView.showWelcomeMessage(
                        currentUser.getUsername(),
                        currentUser.getRole().name()
                );
                authView.waitForEnter();
                return true;
            } else {
                authView.showErrorMessage(result.getMessage());
                authView.waitForEnter();
                return false;
            }

        } catch (Exception e) {
            authView.showErrorMessage("An error occurred during login. Please try again.");
            authView.waitForEnter();
            return false;
        }
    }

    /**
     * Handle registration process
     */
    private void handleRegistration() {
        try {
            // Get registration data from user
            AuthView.RegistrationData regData = authView.getRegistrationData();

            // Validate input
            if (!regData.isValid()) {
                authView.showErrorMessage("All fields are required!");
                authView.waitForEnter();
                return;
            }

            // Check if passwords match
            if (!regData.passwordsMatch()) {
                authView.showErrorMessage("Passwords do not match!");
                authView.waitForEnter();
                return;
            }

            // Attempt registration
            authView.showLoading("Creating account");
            AuthService.AuthResult result = authService.registerCustomer(
                    regData.getUsername(),
                    regData.getPassword()
            );

            if (result.isSuccess()) {
                authView.showSuccessMessage(result.getMessage());
                authView.showInfoMessage("You can now login with your new account.");
            } else {
                authView.showErrorMessage(result.getMessage());
            }

            authView.waitForEnter();

        } catch (Exception e) {
            authView.showErrorMessage("An error occurred during registration. Please try again.");
            authView.waitForEnter();
        }
    }

    /**
     * Handle logout
     */
    public void logout() {
        if (currentUser != null) {
            String username = currentUser.getUsername();
            currentUser = null;
            authView.showLogoutMessage(username);
        }
    }

    /**
     * Update user's customer ID after customer profile creation
     */
    public boolean linkUserToCustomer(Integer customerId) {
        if (currentUser != null) {
            boolean success = authService.linkUserToCustomer(currentUser.getId(), customerId);
            if (success) {
                currentUser.setCustomerId(customerId);
            }
            return success;
        }
        return false;
    }

    /**
     * Handle admin unlock account functionality
     */
    public void handleUnlockAccount() {
        if (currentUser == null || currentUser.getRole() != User.Role.ADMIN) {
            authView.showErrorMessage("Admin privileges required!");
            authView.waitForEnter();
            return;
        }

        int userId = authView.getUserIdToUnlock();
        if (userId == -1) {
            authView.showErrorMessage("Invalid User ID!");
            authView.waitForEnter();
            return;
        }

        authView.showLoading("Unlocking account");
        boolean success = authService.unlockAccount(userId);

        if (success) {
            authView.showSuccessMessage("User account unlocked successfully!");
        } else {
            authView.showErrorMessage("Failed to unlock account. User ID may not exist.");
        }

        authView.waitForEnter();
    }

    // Getters
    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == User.Role.ADMIN;
    }

    public boolean isCustomer() {
        return currentUser != null && currentUser.getRole() == User.Role.CUSTOMER;
    }
}