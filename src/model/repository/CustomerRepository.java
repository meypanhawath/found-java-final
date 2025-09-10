// model/repository/CustomerRepository.java
package model.repository;

import database.Database;
import model.entity.Customer;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerRepository {
    private final Database database;

    public CustomerRepository() {
        this.database = Database.getInstance();
    }

    /**
     * Create a new customer profile
     */
    public boolean create(Customer customer) {
        String sql = """
            INSERT INTO customers (full_name, dob, gender, address, city_or_province, 
                                 country, zip_code, phone_number, email, employment_type, 
                                 company_name, position, main_source_of_income, 
                                 monthly_income_range, remark, pin, customer_segment_id, is_deleted)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, customer.getFullName());
            statement.setDate(2, Date.valueOf(customer.getDob()));
            statement.setString(3, customer.getGender());
            statement.setString(4, customer.getAddress());
            statement.setString(5, customer.getCityOrProvince());
            statement.setString(6, customer.getCountry());
            statement.setString(7, customer.getZipCode());
            statement.setString(8, customer.getPhoneNumber());
            statement.setString(9, customer.getEmail());
            statement.setString(10, customer.getEmploymentType());
            statement.setString(11, customer.getCompanyName());
            statement.setString(12, customer.getPosition());
            statement.setString(13, customer.getMainSourceOfIncome());
            statement.setString(14, customer.getMonthlyIncomeRange());
            statement.setString(15, customer.getRemark());
            statement.setString(16, customer.getPin());
            statement.setInt(17, customer.getCustomerSegmentId());
            statement.setBoolean(18, customer.isDeleted());

            int rowsAffected = statement.executeUpdate();

            if (rowsAffected > 0) {
                // Get the generated customer ID
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        customer.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Error creating customer: " + e.getMessage());
            return false;
        }
    }

    /**
     * Find customer by ID
     */
    public Optional<Customer> findById(Integer id) {
        String sql = "SELECT * FROM customers WHERE id = ? AND is_deleted = false";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToCustomer(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding customer by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Find customer by phone number
     */
    public Optional<Customer> findByPhoneNumber(String phoneNumber) {
        String sql = "SELECT * FROM customers WHERE phone_number = ? AND is_deleted = false";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, phoneNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToCustomer(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding customer by phone number: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Find customer by email
     */
    public Optional<Customer> findByEmail(String email) {
        String sql = "SELECT * FROM customers WHERE email = ? AND is_deleted = false";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToCustomer(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding customer by email: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Update customer information
     */
    public boolean update(Customer customer) {
        String sql = """
            UPDATE customers SET full_name = ?, dob = ?, gender = ?, address = ?, 
                               city_or_province = ?, country = ?, zip_code = ?, 
                               phone_number = ?, email = ?, employment_type = ?, 
                               company_name = ?, position = ?, main_source_of_income = ?, 
                               monthly_income_range = ?, remark = ?, pin = ?, 
                               customer_segment_id = ?
            WHERE id = ? AND is_deleted = false
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, customer.getFullName());
            statement.setDate(2, Date.valueOf(customer.getDob()));
            statement.setString(3, customer.getGender());
            statement.setString(4, customer.getAddress());
            statement.setString(5, customer.getCityOrProvince());
            statement.setString(6, customer.getCountry());
            statement.setString(7, customer.getZipCode());
            statement.setString(8, customer.getPhoneNumber());
            statement.setString(9, customer.getEmail());
            statement.setString(10, customer.getEmploymentType());
            statement.setString(11, customer.getCompanyName());
            statement.setString(12, customer.getPosition());
            statement.setString(13, customer.getMainSourceOfIncome());
            statement.setString(14, customer.getMonthlyIncomeRange());
            statement.setString(15, customer.getRemark());
            statement.setString(16, customer.getPin());
            statement.setInt(17, customer.getCustomerSegmentId());
            statement.setInt(18, customer.getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating customer: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if phone number exists (excluding specific customer)
     */
    public boolean phoneNumberExists(String phoneNumber, Integer excludeCustomerId) {
        String sql = "SELECT COUNT(*) FROM customers WHERE phone_number = ? AND is_deleted = false";

        if (excludeCustomerId != null) {
            sql += " AND id != ?";
        }

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, phoneNumber);
            if (excludeCustomerId != null) {
                statement.setInt(2, excludeCustomerId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking phone number existence: " + e.getMessage());
        }
        return false;
    }

    /**
     * Check if email exists (excluding specific customer)
     */
    public boolean emailExists(String email, Integer excludeCustomerId) {
        String sql = "SELECT COUNT(*) FROM customers WHERE email = ? AND is_deleted = false";

        if (excludeCustomerId != null) {
            sql += " AND id != ?";
        }

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            if (excludeCustomerId != null) {
                statement.setInt(2, excludeCustomerId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking email existence: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get all active customers (for admin use)
     */
    public List<Customer> findAll() {
        String sql = "SELECT * FROM customers WHERE is_deleted = false ORDER BY full_name";
        List<Customer> customers = new ArrayList<>();

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                customers.add(mapResultSetToCustomer(resultSet));
            }

        } catch (SQLException e) {
            System.err.println("Error finding all customers: " + e.getMessage());
        }
        return customers;
    }

    /**
     * Soft delete customer
     */
    public boolean delete(Integer id) {
        String sql = "UPDATE customers SET is_deleted = true WHERE id = ?";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting customer: " + e.getMessage());
            return false;
        }
    }

    /**
     * Map ResultSet to Customer entity
     */
    private Customer mapResultSetToCustomer(ResultSet resultSet) throws SQLException {
        Customer customer = new Customer();
        customer.setId(resultSet.getInt("id"));
        customer.setKycId((Integer) resultSet.getObject("kyc_id"));
        customer.setFullName(resultSet.getString("full_name"));

        Date dobDate = resultSet.getDate("dob");
        if (dobDate != null) {
            customer.setDob(dobDate.toLocalDate());
        }

        customer.setGender(resultSet.getString("gender"));
        customer.setAddress(resultSet.getString("address"));
        customer.setCityOrProvince(resultSet.getString("city_or_province"));
        customer.setCountry(resultSet.getString("country"));
        customer.setZipCode(resultSet.getString("zip_code"));
        customer.setPhoneNumber(resultSet.getString("phone_number"));
        customer.setEmail(resultSet.getString("email"));
        customer.setEmploymentType(resultSet.getString("employment_type"));
        customer.setCompanyName(resultSet.getString("company_name"));
        customer.setPosition(resultSet.getString("position"));
        customer.setMainSourceOfIncome(resultSet.getString("main_source_of_income"));
        customer.setMonthlyIncomeRange(resultSet.getString("monthly_income_range"));
        customer.setRemark(resultSet.getString("remark"));
        customer.setPin(resultSet.getString("pin"));
        customer.setCustomerSegmentId(resultSet.getInt("customer_segment_id"));
        customer.setDeleted(resultSet.getBoolean("is_deleted"));

        return customer;
    }
}