// model/service/AuthService.java
package model.service;

import model.entity.User;
import model.repository.AuthRepository;
import util.PasswordUtil;

import java.util.Optional;

public class AuthService {
    private final AuthRepository authRepository;
    private static final int MAX_FAILED_ATTEMPTS = 3;

    public AuthService() {
        this.authRepository = new AuthRepository();
    }

    /**
     * Authenticate user login
     */
    public AuthResult login(String username, String password) {
        try {
            // Validate input
            if (username == null || username.trim().isEmpty() ||
                    password == null || password.isEmpty()) {
                return AuthResult.failure("Username and password cannot be empty!");
            }

            // Find user
            Optional<User> userOpt = authRepository.findByUsername(username.trim());
            if (userOpt.isEmpty()) {
                return AuthResult.failure("Invalid username or password!");
            }

            User user = userOpt.get();

            // Check if account is locked
            if (user.getIsLocked()) {
                return AuthResult.failure("Account is locked due to too many failed attempts. Please contact admin.");
            }

            // Verify password
            if (!PasswordUtil.verifyPassword(password, user.getPassword())) {
                // Increment failed attempts
                int newAttempts = user.getFailedAttempts() + 1;
                boolean shouldLock = newAttempts >= MAX_FAILED_ATTEMPTS;

                authRepository.updateFailedAttempts(username, newAttempts, shouldLock);

                if (shouldLock) {
                    return AuthResult.failure("Account locked due to too many failed attempts. Please contact admin.");
                } else {
                    int remainingAttempts = MAX_FAILED_ATTEMPTS - newAttempts;
                    return AuthResult.failure("Invalid username or password! " + remainingAttempts + " attempts remaining.");
                }
            }

            // Successful login - reset failed attempts
            authRepository.resetFailedAttempts(username);

            return AuthResult.success(user);

        } catch (Exception e) {
            System.err.println("Error during login: " + e.getMessage());
            return AuthResult.failure("An error occurred during login. Please try again.");
        }
    }

    /**
     * Register new customer user
     */
    public AuthResult registerCustomer(String username, String password) {
        try {
            // Validate input
            if (username == null || username.trim().isEmpty()) {
                return AuthResult.failure("Username cannot be empty!");
            }

            if (password == null || password.isEmpty()) {
                return AuthResult.failure("Password cannot be empty!");
            }

            // Validate password strength
            if (!PasswordUtil.isPasswordStrong(password)) {
                return AuthResult.failure("Password does not meet security requirements!");
            }

            // Check if username exists
            if (authRepository.usernameExists(username.trim())) {
                return AuthResult.failure("Username already exists! Please choose a different username.");
            }

            // Hash password
            String hashedPassword = PasswordUtil.hashPassword(password);

            // Create new customer user (customer_id will be null initially)
            User newUser = new User(
                    null,                    // id (will be generated)
                    username.trim(),         // username
                    hashedPassword,          // password (hashed)
                    User.Role.CUSTOMER,      // role
                    null,                    // customerId (null initially)
                    false,                   // isLocked
                    0,                       // failedAttempts
                    false                    // isDeleted
            );

            boolean success = authRepository.createUser(newUser);

            if (success) {
                return AuthResult.success(newUser, "Registration successful! You can now login.");
            } else {
                return AuthResult.failure("Registration failed. Please try again.");
            }

        } catch (Exception e) {
            System.err.println("Error during registration: " + e.getMessage());
            return AuthResult.failure("An error occurred during registration. Please try again.");
        }
    }

    /**
     * Create admin user (for system initialization)
     */
    public boolean createAdmin(String username, String password) {
        try {
            if (authRepository.usernameExists(username)) {
                return false; // Admin already exists
            }

            String hashedPassword = PasswordUtil.hashPassword(password);

            User adminUser = new User(
                    null,                    // id
                    username,                // username
                    hashedPassword,          // password
                    User.Role.ADMIN,         // role
                    null,                    // customerId (null for admin)
                    false,                   // isLocked
                    0,                       // failedAttempts
                    false                    // isDeleted
            );

            return authRepository.createUser(adminUser);

        } catch (Exception e) {
            System.err.println("Error creating admin: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update user's customer ID (after customer profile creation)
     */
    public boolean linkUserToCustomer(Integer userId, Integer customerId) {
        return authRepository.updateUserCustomerId(userId, customerId);
    }

    /**
     * Unlock user account (admin function)
     */
    public boolean unlockAccount(Integer userId) {
        return authRepository.unlockAccount(userId);
    }

    /**
     * Authentication result class
     */
    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final User user;

        private AuthResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public static AuthResult success(User user) {
            return new AuthResult(true, "Login successful!", user);
        }

        public static AuthResult success(User user, String message) {
            return new AuthResult(true, message, user);
        }

        public static AuthResult failure(String message) {
            return new AuthResult(false, message, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public User getUser() { return user; }
    }
}