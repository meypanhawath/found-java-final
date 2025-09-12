// model/service/AccountService.java - Enhanced with Initial Deposit
package model.service;

import model.entity.Account;
import model.entity.AccountType;
import model.entity.Customer;
import model.entity.Transaction;
import model.repository.AccountRepository;
import model.repository.TransactionRepository;
import util.AccountUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class AccountService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerService customerService;

    // Account limits
    private static final int SAVING_ACCOUNTS_LIMIT = 2; // One USD, One KHR
    private static final int CHECKING_ACCOUNTS_LIMIT = 1;
    private static final int FIXED_ACCOUNTS_LIMIT = 1;

    public AccountService() {
        this.accountRepository = new AccountRepository();
        this.transactionRepository = new TransactionRepository();
        this.customerService = new CustomerService();
    }

    /**
     * Get customer's accounts
     */
    public List<Account> getCustomerAccounts(Integer customerId) {
        return accountRepository.findAccountsByCustomerId(customerId);
    }

    /**
     * Create new account for customer with initial deposit
     */
    public boolean createAccountWithInitialDeposit(Integer customerId, String accountType,
                                                   String currency, BigDecimal initialDeposit,
                                                   LocalDate maturityDate) {
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

        // Validate initial deposit amount
        BigDecimal minimumDeposit = getMinimumInitialDeposit(currency);
        if (initialDeposit.compareTo(minimumDeposit) < 0) {
            System.out.println("❌ Initial deposit must be at least " +
                    AccountUtil.formatCurrency(minimumDeposit, currency) + "!");
            return false;
        }

        // Check account limits
        if (!canCreateAccount(customerId, accType.getId(), accountType, currency)) {
            return false;
        }

        // Validate maturity date for Fixed accounts
        if ("Fixed".equalsIgnoreCase(accountType)) {
            if (maturityDate == null) {
                System.out.println("❌ Fixed accounts require a maturity date!");
                return false;
            }
            if (!maturityDate.isAfter(LocalDate.now())) {
                System.out.println("❌ Maturity date must be in the future!");
                return false;
            }
        }

        // Generate unique account number
        Set<String> existingNumbers = accountRepository.getAllAccountNumbers();
        String accountNumber = AccountUtil.generateAccountNumber(existingNumbers);

        // Generate account name
        String accountName = AccountUtil.generateAccountName(
                customer.getFullName(), accountType, currency);

        // Create account with initial balance
        Account newAccount = new Account(
                customerId, accountNumber, accountName, currency, accType.getId(), maturityDate);
        newAccount.setBalance(initialDeposit); // Set initial balance

        // Create account in database
        boolean accountCreated = accountRepository.createAccount(newAccount);
        if (!accountCreated) {
            System.out.println("❌ Failed to create account!");
            return false;
        }

        // Create initial deposit transaction
        boolean transactionCreated = createInitialDepositTransaction(newAccount, initialDeposit);
        if (!transactionCreated) {
            System.out.println("⚠️ Account created but failed to record initial deposit transaction!");
        }

        System.out.println("✅ Account created successfully!");
        System.out.println("   Account Number: " + AccountUtil.formatAccountNumber(accountNumber));
        System.out.println("   Account Name: " + accountName);
        System.out.println("   Initial Balance: " + AccountUtil.formatCurrency(initialDeposit, currency));

        if (maturityDate != null) {
            System.out.println("   Maturity Date: " + maturityDate);
            System.out.println("   ⚠️ No withdrawals allowed until maturity!");
        }

        return true;
    }

    /**
     * Create initial deposit transaction
     */
    private boolean createInitialDepositTransaction(Account account, BigDecimal amount) {
        try {
            // Get deposit transaction type ID
            Optional<Integer> depositTypeId = transactionRepository.getTransactionTypeId("Deposit");
            if (depositTypeId.isEmpty()) {
                System.err.println("❌ Deposit transaction type not found!");
                return false;
            }

            // Create initial deposit transaction using static factory method
            Transaction initialDeposit = Transaction.createTransaction(
                    account.getId(),        // sender_id (the account receiving money)
                    null,                   // receiver_id (null for deposits)
                    depositTypeId.get(),    // transaction_type_id
                    amount,                 // amount
                    "Success",              // status
                    "Initial deposit for account opening" // remark
            );

            return transactionRepository.createTransaction(initialDeposit);

        } catch (Exception e) {
            System.err.println("Error creating initial deposit transaction: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get minimum initial deposit amount
     */
    public BigDecimal getMinimumInitialDeposit(String currency) {
        if ("USD".equalsIgnoreCase(currency)) {
            return new BigDecimal("5.00"); // $5 minimum
        } else if ("KHR".equalsIgnoreCase(currency)) {
            return new BigDecimal("20000"); // ៛20,000 minimum
        }
        return BigDecimal.ZERO;
    }

    /**
     * Create new account (backward compatibility - without initial deposit)
     */
    @Deprecated
    public boolean createAccount(Integer customerId, String accountType, String currency) {
        BigDecimal minimumDeposit = getMinimumInitialDeposit(currency);
        return createAccountWithInitialDeposit(customerId, accountType, currency, minimumDeposit, null);
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
     * Update account balance by ID
     */
    public boolean updateAccountBalanceById(Integer accountId, BigDecimal newBalance) {
        // Validate account exists and is active
        Optional<Account> accountOpt = accountRepository.findAccountById(accountId);
        if (accountOpt.isEmpty()) {
            System.out.println("❌ Account not found!");
            return false;
        }

        Account account = accountOpt.get();
        if (!account.isActive()) {
            System.out.println("❌ Account is not active!");
            return false;
        }

        return accountRepository.updateBalanceById(accountId, newBalance);
    }

    /**
     * Freeze/Unfreeze account
     */
    public boolean updateFreezeStatus(String accountNumber, boolean isFreeze) {
        String cleanAccountNumber = AccountUtil.unformatAccountNumber(accountNumber);

        // Validate account exists
        Optional<Account> accountOpt = accountRepository.findAccountByAccountNo(cleanAccountNumber);
        if (accountOpt.isEmpty()) {
            System.out.println("❌ Account not found!");
            return false;
        }

        Account account = accountOpt.get();
        if (account.isDeleted()) {
            System.out.println("❌ Cannot modify deleted account!");
            return false;
        }

        boolean success = accountRepository.updateFreezeStatus(cleanAccountNumber, isFreeze);
        if (success) {
            String action = isFreeze ? "frozen" : "unfrozen";
            System.out.println("✅ Account " + AccountUtil.formatAccountNumber(cleanAccountNumber) + " has been " + action + "!");
            return true;
        } else {
            System.out.println("❌ Failed to update account freeze status!");
            return false;
        }
    }

    /**
     * Check if Fixed account can be withdrawn from (maturity check)
     */
    public boolean canWithdrawFromFixedAccount(Account account) {
        if (account.getMaturityDate() == null) {
            return true; // Not a fixed account or no maturity restriction
        }

        boolean isMatured = account.isMatured();
        if (!isMatured) {
            System.out.println("❌ Fixed account withdrawals are not allowed until maturity date: " + account.getMaturityDate());
        }

        return isMatured;
    }

    /**
     * Validate currency
     */
    public boolean isValidCurrency(String currency) {
        return "USD".equalsIgnoreCase(currency) || "KHR".equalsIgnoreCase(currency);
    }

    /**
     * Get all account types
     */
    public List<AccountType> getAllAccountTypes() {
        return accountRepository.findAllAccountTypes();
    }

    /**
     * Get account type by ID
     */
    public Optional<AccountType> getAccountTypeById(Integer id) {
        return accountRepository.findAccountTypeById(id);
    }

    /**
     * Get account summary for customer
     */
    public String getAccountsSummary(Integer customerId) {
        List<Account> accounts = getCustomerAccounts(customerId);

        if (accounts.isEmpty()) {
            return "No accounts found.";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Account Summary:\n");
        summary.append("=".repeat(50)).append("\n");

        for (Account account : accounts) {
            summary.append(account.getFullAccountInfo()).append("\n");
            summary.append("-".repeat(30)).append("\n");
        }

        return summary.toString();
    }

    /**
     * Check if customer has any accounts
     */
    public boolean customerHasAccounts(Integer customerId) {
        return !getCustomerAccounts(customerId).isEmpty();
    }

    /**
     * Get customer's active accounts only
     */
    public List<Account> getActiveAccounts(Integer customerId) {
        List<Account> allAccounts = getCustomerAccounts(customerId);
        return allAccounts.stream()
                .filter(Account::isActive)
                .toList();
    }

    /**
     * Get customer's transactions
     */
    public List<Transaction> getCustomerTransactions(Integer customerId) {
        return transactionRepository.findTransactionsByCustomerId(customerId);
    }

    /**
     * Get account transactions
     */
    public List<Transaction> getAccountTransactions(Integer accountId) {
        return transactionRepository.findTransactionsByAccountId(accountId);
    }
}