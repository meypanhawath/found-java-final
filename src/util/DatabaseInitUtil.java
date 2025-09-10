// util/DatabaseInitUtil.java
package util;

import database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseInitUtil {
    private final Database database;

    public DatabaseInitUtil() {
        this.database = Database.getInstance();
    }

    /**
     * Initialize all required database data
     */
    public void initializeAll() {
        System.out.println("🔧 Initializing database data...");

        initializeAccountTypes();
        initializeCustomerSegments();
        initializeTransactionTypes();
        initializeBillCategories();

        System.out.println("✅ Database initialization completed successfully!");
    }

    /**
     * Initialize account types if they don't exist
     */
    private void initializeAccountTypes() {
        System.out.println("📊 Setting up account types...");

        String[] accountTypes = {"Saving", "Checking", "Fixed"};

        try (Connection connection = database.getConnection()) {
            for (String type : accountTypes) {
                if (!accountTypeExists(connection, type)) {
                    createAccountType(connection, type);
                    System.out.println("✅ Created account type: " + type);
                } else {
                    System.out.println("ℹ️ Account type already exists: " + type);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error initializing account types: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize customer segments if they don't exist
     */
    private void initializeCustomerSegments() {
        System.out.println("👥 Setting up customer segments...");

        String[][] segments = {
                {"Retail", "Regular retail banking customers"},
                {"VIP", "VIP customers with premium services"},
                {"Business", "Business banking customers"}
        };

        try (Connection connection = database.getConnection()) {
            for (String[] segment : segments) {
                if (!customerSegmentExists(connection, segment[0])) {
                    createCustomerSegment(connection, segment[0], segment[1]);
                    System.out.println("✅ Created customer segment: " + segment[0]);
                } else {
                    System.out.println("ℹ️ Customer segment already exists: " + segment[0]);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error initializing customer segments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize transaction types if they don't exist
     */
    private void initializeTransactionTypes() {
        System.out.println("💸 Setting up transaction types...");

        String[] transactionTypes = {"Deposit", "Withdraw", "Transfer", "Bill Payment"};

        try (Connection connection = database.getConnection()) {
            for (String type : transactionTypes) {
                if (!transactionTypeExists(connection, type)) {
                    createTransactionType(connection, type);
                    System.out.println("✅ Created transaction type: " + type);
                } else {
                    System.out.println("ℹ️ Transaction type already exists: " + type);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error initializing transaction types: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize bill categories if they don't exist
     */
    private void initializeBillCategories() {
        System.out.println("🧾 Setting up bill categories...");

        String[][] categories = {
                {"Electricity", "Electricity bill payments"},
                {"Water", "Water bill payments"},
                {"Internet", "Internet service bill payments"},
                {"Phone", "Phone service bill payments"},
                {"Netflix", "Netflix subscription payments"},
                {"Disney+", "Disney+ subscription payments"},
                {"Spotify", "Spotify subscription payments"},
                {"Insurance", "Insurance premium payments"},
                {"Loan Payment", "Loan repayment"},
                {"Credit Card", "Credit card bill payments"}
        };

        try (Connection connection = database.getConnection()) {
            for (String[] category : categories) {
                if (!billCategoryExists(connection, category[0])) {
                    createBillCategory(connection, category[0], category[1]);
                    System.out.println("✅ Created bill category: " + category[0]);
                } else {
                    System.out.println("ℹ️ Bill category already exists: " + category[0]);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error initializing bill categories: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * Check if account type exists
     */
    private boolean accountTypeExists(Connection connection, String type) throws SQLException {
        String sql = "SELECT COUNT(*) FROM account_types WHERE LOWER(type) = LOWER(?) AND is_deleted = false";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    /**
     * Create account type
     */
    private void createAccountType(Connection connection, String type) throws SQLException {
        String sql = "INSERT INTO account_types (type, is_deleted) VALUES (?, false)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type);
            statement.executeUpdate();
        }
    }

    /**
     * Check if customer segment exists
     */
    private boolean customerSegmentExists(Connection connection, String segment) throws SQLException {
        String sql = "SELECT COUNT(*) FROM customer_segments WHERE LOWER(segment) = LOWER(?) AND is_deleted = false";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, segment);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    /**
     * Create customer segment
     */
    private void createCustomerSegment(Connection connection, String segment, String description) throws SQLException {
        String sql = "INSERT INTO customer_segments (segment, description, is_deleted) VALUES (?, ?, false)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, segment);
            statement.setString(2, description);
            statement.executeUpdate();
        }
    }

    /**
     * Check if transaction type exists
     */
    private boolean transactionTypeExists(Connection connection, String type) throws SQLException {
        String sql = "SELECT COUNT(*) FROM transaction_types WHERE LOWER(type) = LOWER(?) AND is_deleted = false";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    /**
     * Create transaction type
     */
    private void createTransactionType(Connection connection, String type) throws SQLException {
        String sql = "INSERT INTO transaction_types (type, is_deleted) VALUES (?, false)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type);
            statement.executeUpdate();
        }
    }

    /**
     * Check if bill category exists
     */
    private boolean billCategoryExists(Connection connection, String category) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bill_categories WHERE LOWER(category_name) = LOWER(?) AND is_deleted = false";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    /**
     * Create bill category
     */
    private void createBillCategory(Connection connection, String category, String description) throws SQLException {
        String sql = "INSERT INTO bill_categories (category_name, description, is_deleted) VALUES (?, ?, false)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category);
            statement.setString(2, description);
            statement.executeUpdate();
        }
    }
}