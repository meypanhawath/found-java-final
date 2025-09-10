// model/repository/AuthRepository.java
package model.repository;

import database.Database;
import model.entity.User;

import java.sql.*;
import java.util.Optional;

public class AuthRepository {
    private final Database database;

    public AuthRepository() {
        this.database = Database.getInstance();
    }

    /**
     * Find user by username for login
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ? AND is_deleted = false";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToUser(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by username: " + e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Create new user (for registration)
     */
    public boolean createUser(User user) {
        String sql = """
            INSERT INTO users (username, password, role, customer_id, is_locked, failed_attempts, is_deleted)
            VALUES (?, ?, ?::text, ?, ?, ?, ?)
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getRole().name());

            // Handle null customer_id
            if (user.getCustomerId() != null) {
                statement.setInt(4, user.getCustomerId());
            } else {
                statement.setNull(4, Types.INTEGER);
            }

            statement.setBoolean(5, user.getIsLocked());
            statement.setInt(6, user.getFailedAttempts());
            statement.setBoolean(7, user.getIsDeleted());

            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update user's customer_id (after customer profile creation)
     */
    public boolean updateUserCustomerId(Integer userId, Integer customerId) {
        String sql = "UPDATE users SET customer_id = ? WHERE id = ? AND is_deleted = false";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (customerId != null) {
                statement.setInt(1, customerId);
            } else {
                statement.setNull(1, Types.INTEGER);
            }
            statement.setInt(2, userId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating user customer ID: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update failed login attempts
     */
    public boolean updateFailedAttempts(String username, int attempts, boolean isLocked) {
        String sql = "UPDATE users SET failed_attempts = ?, is_locked = ? WHERE username = ? AND is_deleted = false";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, attempts);
            statement.setBoolean(2, isLocked);
            statement.setString(3, username);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating failed attempts: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reset failed attempts (successful login)
     */
    public boolean resetFailedAttempts(String username) {
        return updateFailedAttempts(username, 0, false);
    }

    /**
     * Unlock user account
     */
    public boolean unlockAccount(Integer userId) {
        String sql = "UPDATE users SET is_locked = false, failed_attempts = 0 WHERE id = ? AND is_deleted = false";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error unlocking account: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if username exists
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND is_deleted = false";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking username existence: " + e.getMessage());
        }

        return false;
    }

    /**
     * Map ResultSet to User entity
     */
    private User mapResultSetToUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getInt("id"));
        user.setUsername(resultSet.getString("username"));
        user.setPassword(resultSet.getString("password"));
        user.setRole(User.Role.valueOf(resultSet.getString("role")));

        // Handle null customer_id
        Integer customerId = (Integer) resultSet.getObject("customer_id");
        user.setCustomerId(customerId);

        user.setIsLocked(resultSet.getBoolean("is_locked"));
        user.setFailedAttempts(resultSet.getInt("failed_attempts"));
        user.setIsDeleted(resultSet.getBoolean("is_deleted"));

        return user;
    }
}