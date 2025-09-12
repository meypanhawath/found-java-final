// model/repository/AccountRepository.java - Enhanced
package model.repository;

import database.Database;
import model.entity.Account;
import model.entity.AccountType;

import java.sql.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class AccountRepository {
    private final Database database;

    public AccountRepository() {
        this.database = Database.getInstance();
    }

    // ========== ACCOUNT OPERATIONS ==========

    /**
     * Create a new account with maturity date support
     */
    public boolean createAccount(Account account) {
        String sql = """
            INSERT INTO accounts (customer_id, account_no, account_name, account_currency, 
                                balance, over_limit, is_freeze, is_deleted, account_type_id, maturity_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, account.getCustomerId());
            statement.setString(2, account.getAccountNo());
            statement.setString(3, account.getAccountName());
            statement.setString(4, account.getAccountCurrency());
            statement.setBigDecimal(5, account.getBalance());
            statement.setBigDecimal(6, account.getOverLimit());
            statement.setBoolean(7, account.isFreeze());
            statement.setBoolean(8, account.isDeleted());
            statement.setInt(9, account.getAccountTypeId());

            // Handle maturity date
            if (account.getMaturityDate() != null) {
                statement.setDate(10, Date.valueOf(account.getMaturityDate()));
            } else {
                statement.setNull(10, Types.DATE);
            }

            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        account.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Error creating account: " + e.getMessage());
            return false;
        }
    }

    /**
     * Find accounts by customer ID with account type information and maturity date
     */
    public List<Account> findAccountsByCustomerId(Integer customerId) {
        String sql = """
            SELECT a.*, at.type as account_type_name, c.full_name as customer_name
            FROM accounts a
            JOIN account_types at ON a.account_type_id = at.id
            JOIN customers c ON a.customer_id = c.id
            WHERE a.customer_id = ? AND a.is_deleted = false
            ORDER BY at.type, a.account_currency
            """;

        List<Account> accounts = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    accounts.add(mapResultSetToAccount(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding accounts by customer ID: " + e.getMessage());
        }
        return accounts;
    }

    /**
     * Find account by account number with maturity date
     */
    public Optional<Account> findAccountByAccountNo(String accountNo) {
        String sql = """
            SELECT a.*, at.type as account_type_name, c.full_name as customer_name
            FROM accounts a
            JOIN account_types at ON a.account_type_id = at.id
            JOIN customers c ON a.customer_id = c.id
            WHERE a.account_no = ? AND a.is_deleted = false
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, accountNo);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToAccount(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding account by account number: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Find account by ID with maturity date
     */
    public Optional<Account> findAccountById(Integer id) {
        String sql = """
            SELECT a.*, at.type as account_type_name, c.full_name as customer_name
            FROM accounts a
            JOIN account_types at ON a.account_type_id = at.id
            JOIN customers c ON a.customer_id = c.id
            WHERE a.id = ? AND a.is_deleted = false
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToAccount(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding account by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Get all existing account numbers for uniqueness check
     */
    public Set<String> getAllAccountNumbers() {
        String sql = "SELECT account_no FROM accounts WHERE is_deleted = false";
        Set<String> accountNumbers = new HashSet<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                accountNumbers.add(resultSet.getString("account_no"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all account numbers: " + e.getMessage());
        }
        return accountNumbers;
    }

    /**
     * Count accounts by customer ID and account type
     */
    public int countAccountsByCustomerAndType(Integer customerId, Integer accountTypeId, String currency) {
        String sql = """
            SELECT COUNT(*) FROM accounts a
            WHERE a.customer_id = ? AND a.account_type_id = ? AND a.is_deleted = false
            """;

        // For savings accounts, we need to check currency too
        if (currency != null) {
            sql += " AND a.account_currency = ?";
        }

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, customerId);
            statement.setInt(2, accountTypeId);
            if (currency != null) {
                statement.setString(3, currency);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error counting accounts by customer and type: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Update account balance
     */
    public boolean updateBalance(String accountNo, BigDecimal newBalance) {
        String sql = "UPDATE accounts SET balance = ? WHERE account_no = ? AND is_deleted = false";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBigDecimal(1, newBalance);
            statement.setString(2, accountNo);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating account balance: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update account balance by account ID
     */
    public boolean updateBalanceById(Integer accountId, BigDecimal newBalance) {
        String sql = "UPDATE accounts SET balance = ? WHERE id = ? AND is_deleted = false";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBigDecimal(1, newBalance);
            statement.setInt(2, accountId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating account balance by ID: " + e.getMessage());
            return false;
        }
    }

    /**
     * Freeze/Unfreeze account
     */
    public boolean updateFreezeStatus(String accountNo, boolean isFreeze) {
        String sql = "UPDATE accounts SET is_freeze = ? WHERE account_no = ? AND is_deleted = false";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBoolean(1, isFreeze);
            statement.setString(2, accountNo);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating account freeze status: " + e.getMessage());
            return false;
        }
    }

    // ========== ACCOUNT TYPE OPERATIONS ==========

    /**
     * Get all account types
     */
    public List<AccountType> findAllAccountTypes() {
        String sql = "SELECT * FROM account_types WHERE is_deleted = false ORDER BY id";
        List<AccountType> accountTypes = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                AccountType accountType = new AccountType();
                accountType.setId(resultSet.getInt("id"));
                accountType.setType(resultSet.getString("type"));
                accountType.setDeleted(resultSet.getBoolean("is_deleted"));
                accountTypes.add(accountType);
            }

        } catch (SQLException e) {
            System.err.println("Error finding all account types: " + e.getMessage());
        }
        return accountTypes;
    }

    /**
     * Find account type by ID
     */
    public Optional<AccountType> findAccountTypeById(Integer id) {
        String sql = "SELECT * FROM account_types WHERE id = ? AND is_deleted = false";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    AccountType accountType = new AccountType();
                    accountType.setId(resultSet.getInt("id"));
                    accountType.setType(resultSet.getString("type"));
                    accountType.setDeleted(resultSet.getBoolean("is_deleted"));
                    return Optional.of(accountType);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding account type by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Find account type by type name
     */
    public Optional<AccountType> findAccountTypeByName(String typeName) {
        String sql = "SELECT * FROM account_types WHERE LOWER(type) = LOWER(?) AND is_deleted = false";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, typeName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    AccountType accountType = new AccountType();
                    accountType.setId(resultSet.getInt("id"));
                    accountType.setType(resultSet.getString("type"));
                    accountType.setDeleted(resultSet.getBoolean("is_deleted"));
                    return Optional.of(accountType);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding account type by name: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Enhanced map ResultSet to Account entity with maturity date
     */
    private Account mapResultSetToAccount(ResultSet resultSet) throws SQLException {
        Account account = new Account();
        account.setId(resultSet.getInt("id"));
        account.setCustomerId(resultSet.getInt("customer_id"));
        account.setAccountNo(resultSet.getString("account_no"));
        account.setAccountName(resultSet.getString("account_name"));
        account.setAccountCurrency(resultSet.getString("account_currency"));
        account.setBalance(resultSet.getBigDecimal("balance"));
        account.setOverLimit(resultSet.getBigDecimal("over_limit"));
        account.setFreeze(resultSet.getBoolean("is_freeze"));
        account.setDeleted(resultSet.getBoolean("is_deleted"));
        account.setAccountTypeId(resultSet.getInt("account_type_id"));

        // Handle maturity date
        Date maturityDate = resultSet.getDate("maturity_date");
        if (maturityDate != null) {
            account.setMaturityDate(maturityDate.toLocalDate());
        }

        // Additional fields from joins
        account.setAccountTypeName(resultSet.getString("account_type_name"));
        account.setCustomerName(resultSet.getString("customer_name"));

        return account;
    }
}