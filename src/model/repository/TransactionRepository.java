// model/repository/TransactionRepository.java
package model.repository;

import database.Database;
import model.entity.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransactionRepository {
    private final Database database;

    public TransactionRepository() {
        this.database = Database.getInstance();
    }

    /**
     * Create a new transaction
     */
    public boolean createTransaction(Transaction transaction) {
        String sql = """
            INSERT INTO transactions (sender_id, receiver_id, transaction_type_id, bill_category_id,
                                    amount, status, remark, is_deleted, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, transaction.getSenderId());

            // Handle null receiver_id
            if (transaction.getReceiverId() != null) {
                statement.setInt(2, transaction.getReceiverId());
            } else {
                statement.setNull(2, Types.INTEGER);
            }

            statement.setInt(3, transaction.getTransactionTypeId());

            // Handle null bill_category_id
            if (transaction.getBillCategoryId() != null) {
                statement.setInt(4, transaction.getBillCategoryId());
            } else {
                statement.setNull(4, Types.INTEGER);
            }

            statement.setBigDecimal(5, transaction.getAmount());
            statement.setString(6, transaction.getStatus());
            statement.setString(7, transaction.getRemark());
            statement.setBoolean(8, transaction.isDeleted());
            statement.setTimestamp(9, Timestamp.valueOf(transaction.getCreatedAt()));

            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        transaction.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Error creating transaction: " + e.getMessage());
            return false;
        }
    }

    /**
     * Find transactions by account ID (as sender or receiver)
     */
    public List<Transaction> findTransactionsByAccountId(Integer accountId) {
        String sql = """
            SELECT t.*, tt.type as transaction_type_name,
                   sa.account_name as sender_account_name, sa.account_no as sender_account_no,
                   ra.account_name as receiver_account_name, ra.account_no as receiver_account_no,
                   bc.category_name as bill_category_name
            FROM transactions t
            JOIN transaction_types tt ON t.transaction_type_id = tt.id
            LEFT JOIN accounts sa ON t.sender_id = sa.id
            LEFT JOIN accounts ra ON t.receiver_id = ra.id
            LEFT JOIN bill_categories bc ON t.bill_category_id = bc.id
            WHERE (t.sender_id = ? OR t.receiver_id = ?) AND t.is_deleted = false
            ORDER BY t.created_at DESC
            """;

        List<Transaction> transactions = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, accountId);
            statement.setInt(2, accountId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapResultSetToTransaction(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding transactions by account ID: " + e.getMessage());
        }
        return transactions;
    }

    /**
     * Find transactions by customer ID (all accounts)
     */
    public List<Transaction> findTransactionsByCustomerId(Integer customerId) {
        String sql = """
            SELECT DISTINCT t.*, tt.type as transaction_type_name,
                   sa.account_name as sender_account_name, sa.account_no as sender_account_no,
                   ra.account_name as receiver_account_name, ra.account_no as receiver_account_no,
                   bc.category_name as bill_category_name
            FROM transactions t
            JOIN transaction_types tt ON t.transaction_type_id = tt.id
            LEFT JOIN accounts sa ON t.sender_id = sa.id
            LEFT JOIN accounts ra ON t.receiver_id = ra.id
            LEFT JOIN bill_categories bc ON t.bill_category_id = bc.id
            WHERE (sa.customer_id = ? OR ra.customer_id = ?) AND t.is_deleted = false
            ORDER BY t.created_at DESC
            """;

        List<Transaction> transactions = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, customerId);
            statement.setInt(2, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapResultSetToTransaction(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding transactions by customer ID: " + e.getMessage());
        }
        return transactions;
    }

    /**
     * Find transaction by ID
     */
    public Optional<Transaction> findTransactionById(Integer id) {
        String sql = """
            SELECT t.*, tt.type as transaction_type_name,
                   sa.account_name as sender_account_name, sa.account_no as sender_account_no,
                   ra.account_name as receiver_account_name, ra.account_no as receiver_account_no,
                   bc.category_name as bill_category_name
            FROM transactions t
            JOIN transaction_types tt ON t.transaction_type_id = tt.id
            LEFT JOIN accounts sa ON t.sender_id = sa.id
            LEFT JOIN accounts ra ON t.receiver_id = ra.id
            LEFT JOIN bill_categories bc ON t.bill_category_id = bc.id
            WHERE t.id = ? AND t.is_deleted = false
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToTransaction(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding transaction by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Update transaction status
     */
    public boolean updateTransactionStatus(Integer transactionId, String status) {
        String sql = "UPDATE transactions SET status = ? WHERE id = ? AND is_deleted = false";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setInt(2, transactionId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating transaction status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get transaction type ID by name
     */
    public Optional<Integer> getTransactionTypeId(String typeName) {
        String sql = "SELECT id FROM transaction_types WHERE LOWER(type) = LOWER(?) AND is_deleted = false";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, typeName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getInt("id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting transaction type ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Soft delete transaction
     */
    public boolean deleteTransaction(Integer transactionId) {
        String sql = "UPDATE transactions SET is_deleted = true WHERE id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, transactionId);
            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting transaction: " + e.getMessage());
            return false;
        }
    }

    /**
     * Map ResultSet to Transaction entity
     */
    private Transaction mapResultSetToTransaction(ResultSet resultSet) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(resultSet.getInt("id"));
        transaction.setSenderId(resultSet.getInt("sender_id"));

        // Handle null receiver_id
        Integer receiverId = (Integer) resultSet.getObject("receiver_id");
        transaction.setReceiverId(receiverId);

        transaction.setTransactionTypeId(resultSet.getInt("transaction_type_id"));

        // Handle null bill_category_id
        Integer billCategoryId = (Integer) resultSet.getObject("bill_category_id");
        transaction.setBillCategoryId(billCategoryId);

        transaction.setAmount(resultSet.getBigDecimal("amount"));
        transaction.setStatus(resultSet.getString("status"));
        transaction.setRemark(resultSet.getString("remark"));
        transaction.setDeleted(resultSet.getBoolean("is_deleted"));
        transaction.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());

        // Additional fields from joins
        transaction.setTransactionTypeName(resultSet.getString("transaction_type_name"));
        transaction.setSenderAccountName(resultSet.getString("sender_account_name"));
        transaction.setSenderAccountNo(resultSet.getString("sender_account_no"));
        transaction.setReceiverAccountName(resultSet.getString("receiver_account_name"));
        transaction.setReceiverAccountNo(resultSet.getString("receiver_account_no"));
        transaction.setBillCategoryName(resultSet.getString("bill_category_name"));

        return transaction;
    }
}