// model/service/AccountService.java
package model.service;

import model.entity.Account;
import model.entity.AccountType;
import model.entity.Customer;
import model.repository.AccountRepository;
import util.AccountUtil;

import java.math.BigDecimal;
import java.util.*;

public class AccountService {
    private final AccountRepository accountRepository;
    private final CustomerService customerService;

    // Account limits
    private static final int SAVING_ACCOUNTS_LIMIT = 2; // One USD, One KHR
    private static final int CHECKING_ACCOUNTS_LIMIT = 1;
    private static final int FIXED_ACCOUNTS_LIMIT = 1;

    public AccountService() {
        this.accountRepository = new AccountRepository();
        this.customerService = new CustomerService();
    }

    /**
     * Get customer's accounts
     */
    public List<Account> getCustomerAccounts(Integer customerId) {
        return accountRepository.findAccountsByCustomerId(customerId);
    }

    /**
     * Create new account for customer
     */
    public boolean createAccount(Integer customerId, String accountType, String currency) {
        // Validate customer exists
        Optional<Customer> customerOpt = customerService.findCustomerById(customerId);
        if (customerOpt.isEmpty()) {
            System.out.println("❌ Customer not found!");
            return false;
        }

        Customer customer = customerOpt.get();

        // Validate account type
        Optional<AccountType> accountTypeOpt = accountRepository.findAccountTypeByName(accountType);
        if (accountTypeOpt.isEmpty()) {
            System.out.println("❌ Invalid account type!");
            return false;
        }

        AccountType accType = accountTypeOpt.get();

        // Validate currency
        if (!isValidCurrency(currency)) {
            System.out.println("❌ Invalid currency! Only USD and KHR are supported.");
            return false;
        }

        // Check account limits
        if (!canCreateAccount(customerId, accType.getId(), accountType, currency)) {
            return false;
        }

        // Generate unique account number
        Set<String> existingNumbers = accountRepository.getAllAccountNumbers();
        String accountNumber = AccountUtil.generateAccountNumber(existingNumbers);

        // Generate account name
        String accountName = AccountUtil.generateAccountName(
                customer.getFullName(), accountType, currency);

        // Create account
        Account newAccount = new Account(
                customerId, accountNumber, accountName, currency, accType.getId());

        boolean success = accountRepository.createAccount(newAccount);
        if (success) {
            System.out.println("✅ Account created successfully!");
            System.out.println("   Account Number: " + AccountUtil.formatAccountNumber(accountNumber));
            System.out.println("   Account Name: " + accountName);
            return true;
        } else {
            System.out.println("❌ Failed to create account!");
            return false;
        }
    }

    /**
     * Check if customer can create account of specific type
     */
    public boolean canCreateAccount(Integer customerId, Integer accountTypeId, String accountTypeName, String currency) {
        accountTypeName = accountTypeName.toLowerCase();

        if ("saving".equals(accountTypeName)) {
            // Saving accounts: limit 2 (one USD, one KHR)
            int count = accountRepository.countAccountsByCustomerAndType(customerId, accountTypeId, currency);
            if (count >= 1) {
                System.out.println("❌ You already have a " + accountTypeName + " account in " + currency + "!");
                return false;
            }
        } else if ("checking".equals(accountTypeName)) {
            // Checking accounts: limit 1
            int count = accountRepository.countAccountsByCustomerAndType(customerId, accountTypeId, null);
            if (count >= CHECKING_ACCOUNTS_LIMIT) {
                System.out.println("❌ You already have a " + accountTypeName + " account!");
                return false;
            }
        } else if ("fixed".equals(accountTypeName)) {
            // Fixed accounts: limit 1
            int count = accountRepository.countAccountsByCustomerAndType(customerId, accountTypeId, null);
            if (count >= FIXED_ACCOUNTS_LIMIT) {
                System.out.println("❌ You already have a " + accountTypeName + " account!");
                return false;
            }
        }

        return true;
    }

    /**
     * Get available account types that customer can still create
     */
    public List<String> getAvailableAccountTypes(Integer customerId) {
        List<String> availableTypes = new ArrayList<>();
        List<AccountType> allTypes = accountRepository.findAllAccountTypes();

        for (AccountType type : allTypes) {
            String typeName = type.getType().toLowerCase();

            if ("saving".equals(typeName)) {
                // Check both USD and KHR
                int usdCount = accountRepository.countAccountsByCustomerAndType(customerId, type.getId(), "USD");
                int khrCount = accountRepository.countAccountsByCustomerAndType(customerId, type.getId(), "KHR");

                if (usdCount == 0) {
                    availableTypes.add("Saving Account (USD)");
                }
                if (khrCount == 0) {
                    availableTypes.add("Saving Account (KHR)");
                }
            } else if ("checking".equals(typeName)) {
                int count = accountRepository.countAccountsByCustomerAndType(customerId, type.getId(), null);
                if (count == 0) {
                    availableTypes.add("Checking Account");
                }
            } else if ("fixed".equals(typeName)) {
                int count = accountRepository.countAccountsByCustomerAndType(customerId, type.getId(), null);
                if (count == 0) {
                    availableTypes.add("Fixed Account");
                }
            }
        }

        return availableTypes;
    }

    /**
     * Get account limits summary
     */
    public Map<String, String> getAccountLimitsSummary(Integer customerId) {
        Map<String, String> limits = new HashMap<>();
        List<AccountType> allTypes = accountRepository.findAllAccountTypes();

        for (AccountType type : allTypes) {
            String typeName = type.getType().toLowerCase();

            if ("saving".equals(typeName)) {
                int usdCount = accountRepository.countAccountsByCustomerAndType(customerId, type.getId(), "USD");
                int khrCount = accountRepository.countAccountsByCustomerAndType(customerId, type.getId(), "KHR");
                limits.put("Saving (USD)", usdCount + "/1");
                limits.put("Saving (KHR)", khrCount + "/1");
            } else if ("checking".equals(typeName)) {
                int count = accountRepository.countAccountsByCustomerAndType(customerId, type.getId(), null);
                limits.put("Checking", count + "/" + CHECKING_ACCOUNTS_LIMIT);
            } else if ("fixed".equals(typeName)) {
                int count = accountRepository.countAccountsByCustomerAndType(customerId, type.getId(), null);
                limits.put("Fixed", count + "/" + FIXED_ACCOUNTS_LIMIT);
            }
        }

        return limits;
    }

    /**
     * Find account by account number
     */
    public Optional<Account> findAccountByNumber(String accountNumber) {
        // Remove formatting if present
        String cleanAccountNumber = AccountUtil.unformatAccountNumber(accountNumber);
        return accountRepository.findAccountByAccountNo(cleanAccountNumber);
    }

    /**
     * Find account by ID
     */
    public Optional<Account> findAccountById(Integer accountId) {
        return accountRepository.findAccountById(accountId);
    }

    /**
     * Get account balance
     */
    public BigDecimal getAccountBalance(String accountNumber) {
        Optional<Account> accountOpt = findAccountByNumber(accountNumber);
        return accountOpt.map(Account::getBalance).orElse(BigDecimal.ZERO);
    }

    /**
     * Update account balance
     */
    public boolean updateAccountBalance(String accountNumber, BigDecimal newBalance) {
        String cleanAccountNumber = AccountUtil.unformatAccountNumber(accountNumber);

        // Validate account exists and is active
        Optional<Account> accountOpt = accountRepository.findAccountByAccountNo(cleanAccountNumber);
        if (accountOpt.isEmpty()) {
            System.out.println("❌ Account not found!");
            return false;
        }

        Account account = accountOpt.get();
        if (!account.isActive()) {
            System.out.println("❌ Account is not active!");
            return false;
        }

        return accountRepository.updateBalance(cleanAccountNumber, newBalance);
    }

    /**
     * Freeze/Unfreeze account
     */
    public boolean updateFreezeStatus