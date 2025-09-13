// model/service/TransactionService.java
package model.service;

import model.entity.Account;
import model.entity.Transaction;
import model.repository.TransactionRepository;
import model.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    // Exchange rates (static as requested)
    private static final BigDecimal USD_TO_KHR_RATE = new BigDecimal("4100");
    private static final BigDecimal KHR_TO_USD_RATE = new BigDecimal("0.000244");

    // Daily limits
    private static final BigDecimal SAVING_DAILY_LIMIT_USD = new BigDecimal("5000");
    private static final BigDecimal SAVING_DAILY_LIMIT_KHR = new BigDecimal("20500000"); // 5000 USD in KHR

    public TransactionService() {
        this.transactionRepository = new TransactionRepository();
        this.accountRepository = new AccountRepository();
    }

    /**
     * Create a deposit transaction
     */
    public boolean deposit(Integer accountId, BigDecimal amount, String pin, String remark) {
        // Validate account
        Optional<Account> accountOpt = accountRepository.findAccountById(accountId);
        if (accountOpt.isEmpty()) {
            System.out.println("❌ Account not found!");
            return false;
        }

        Account account = accountOpt.get();

        // Validate account status
        if (!account.isActive()) {
            System.out.println("❌ Account is not active!");
            return false;
        }

        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("❌ Deposit amount must be greater than zero!");
            return false;
        }

        // Get deposit transaction type
        Optional<Integer> depositTypeId = transactionRepository.getTransactionTypeId("Deposit");
        if (depositTypeId.isEmpty()) {
            System.out.println("❌ Deposit transaction type not found!");
            return false;
        }

        try {
            // Create deposit transaction
            Transaction depositTransaction = new Transaction(
                    accountId,              // sender_id (receiving account)
                    null,                   // receiver_id (null for deposits)
                    depositTypeId.get(),    // transaction_type_id
                    amount,                 // amount
                    "Success",              // status
                    remark != null ? remark : "Cash deposit"
            );

            // Save transaction
            boolean transactionCreated = transactionRepository.createTransaction(depositTransaction);
            if (!transactionCreated) {
                System.out.println("❌ Failed to create deposit transaction!");
                return false;
            }

            // Update account balance
            BigDecimal newBalance = account.getBalance().add(amount);
            boolean balanceUpdated = accountRepository.updateBalanceById(accountId, newBalance);
            if (!balanceUpdated) {
                System.out.println("❌ Failed to update account balance!");
                // TODO: Consider rollback transaction
                return false;
            }

            System.out.println("✅ Deposit successful!");
            System.out.println("   Amount: " + formatCurrency(amount, account.getAccountCurrency()));
            System.out.println("   New Balance: " + formatCurrency(newBalance, account.getAccountCurrency()));
            return true;

        } catch (Exception e) {
            System.err.println("Error processing deposit: " + e.getMessage());
            return false;
        }
    }

    /**
     * Create a withdrawal transaction
     */
    public boolean withdraw(Integer accountId, BigDecimal amount, String pin, String remark) {
        // Validate account
        Optional<Account> accountOpt = accountRepository.findAccountById(accountId);
        if (accountOpt.isEmpty()) {
            System.out.println("❌ Account not found!");
            return false;
        }

        Account account = accountOpt.get();

        // Validate account status
        if (!account.isActive()) {
            System.out.println("❌ Account is not active!");
            return false;
        }

        // Check if Fixed account is matured
        if (account.getMaturityDate() != null && !account.isMatured()) {
            System.out.println("❌ Fixed account withdrawals are not allowed until maturity date: " +
                    account.getMaturityDate());
            return false;
        }

        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("❌ Withdrawal amount must be greater than zero!");
            return false;
        }

        // Check sufficient balance
        if (account.getBalance().compareTo(amount) < 0) {
            System.out.println("❌ Insufficient balance!");
            System.out.println("   Available: " + formatCurrency(account.getBalance(), account.getAccountCurrency()));
            System.out.println("   Requested: " + formatCurrency(amount, account.getAccountCurrency()));
            return false;
        }

        // Check daily limits for Saving accounts
        if ("Saving".equalsIgnoreCase(account.getAccountTypeName())) {
            if (!checkDailyLimit(accountId, amount, account.getAccountCurrency())) {
                return false;
            }
        }

        // Get withdraw transaction type
        Optional<Integer> withdrawTypeId = transactionRepository.getTransactionTypeId("Withdraw");
        if (withdrawTypeId.isEmpty()) {
            System.out.println("❌ Withdraw transaction type not found!");
            return false;
        }

        try {
            // Create withdraw transaction
            Transaction withdrawTransaction = new Transaction(
                    accountId,              // sender_id (account money is withdrawn from)
                    null,                   // receiver_id (null for withdrawals)
                    withdrawTypeId.get(),   // transaction_type_id
                    amount,                 // amount
                    "Success",              // status
                    remark != null ? remark : "Cash withdrawal"
            );

            // Save transaction
            boolean transactionCreated = transactionRepository.createTransaction(withdrawTransaction);
            if (!transactionCreated) {
                System.out.println("❌ Failed to create withdrawal transaction!");
                return false;
            }

            // Update account balance
            BigDecimal newBalance = account.getBalance().subtract(amount);
            boolean balanceUpdated = accountRepository.updateBalanceById(accountId, newBalance);
            if (!balanceUpdated) {
                System.out.println("❌ Failed to update account balance!");
                // TODO: Consider rollback transaction
                return false;
            }

            System.out.println("✅ Withdrawal successful!");
            System.out.println("   Amount: " + formatCurrency(amount, account.getAccountCurrency()));
            System.out.println("   New Balance: " + formatCurrency(newBalance, account.getAccountCurrency()));
            return true;

        } catch (Exception e) {
            System.err.println("Error processing withdrawal: " + e.getMessage());
            return false;
        }
    }

    /**
     * Transfer money between accounts (same customer or different customers)
     */
    public boolean transfer(Integer fromAccountId, Integer toAccountId, BigDecimal amount, String pin, String remark) {
        // Validate sender account
        Optional<Account> fromAccountOpt = accountRepository.findAccountById(fromAccountId);
        if (fromAccountOpt.isEmpty()) {
            System.out.println("❌ Sender account not found!");
            return false;
        }

        Account fromAccount = fromAccountOpt.get();

        // Validate receiver account
        Optional<Account> toAccountOpt = accountRepository.findAccountById(toAccountId);
        if (toAccountOpt.isEmpty()) {
            System.out.println("❌ Receiver account not found!");
            return false;
        }

        Account toAccount = toAccountOpt.get();

        // Validate accounts are different
        if (fromAccountId.equals(toAccountId)) {
            System.out.println("❌ Cannot transfer to the same account!");
            return false;
        }

        // Validate sender account status
        if (!fromAccount.isActive()) {
            System.out.println("❌ Sender account is not active!");
            return false;
        }

        // Validate receiver account status
        if (!toAccount.isActive()) {
            System.out.println("❌ Receiver account is not active!");
            return false;
        }

        // Check if Fixed account is matured (for sender)
        if (fromAccount.getMaturityDate() != null && !fromAccount.isMatured()) {
            System.out.println("❌ Fixed account transfers are not allowed until maturity date: " +
                    fromAccount.getMaturityDate());
            return false;
        }

        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("❌ Transfer amount must be greater than zero!");
            return false;
        }

        // Check sufficient balance
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            System.out.println("❌ Insufficient balance!");
            System.out.println("   Available: " + formatCurrency(fromAccount.getBalance(), fromAccount.getAccountCurrency()));
            System.out.println("   Requested: " + formatCurrency(amount, fromAccount.getAccountCurrency()));
            return false;
        }

        // Check daily limits for Saving accounts
        if ("Saving".equalsIgnoreCase(fromAccount.getAccountTypeName())) {
            if (!checkDailyLimit(fromAccountId, amount, fromAccount.getAccountCurrency())) {
                return false;
            }
        }

        // Handle currency conversion if needed
        BigDecimal convertedAmount = amount;
        String transferRemark = remark != null ? remark : "Account transfer";

        if (!fromAccount.getAccountCurrency().equals(toAccount.getAccountCurrency())) {
            convertedAmount = convertCurrency(amount, fromAccount.getAccountCurrency(), toAccount.getAccountCurrency());
            transferRemark += String.format(" (Converted from %s %s to %s %s at rate %s)",
                    formatCurrency(amount, fromAccount.getAccountCurrency()),
                    fromAccount.getAccountCurrency(),
                    formatCurrency(convertedAmount, toAccount.getAccountCurrency()),
                    toAccount.getAccountCurrency(),
                    getExchangeRate(fromAccount.getAccountCurrency(), toAccount.getAccountCurrency()));
        }

        // Get transfer transaction type
        Optional<Integer> transferTypeId = transactionRepository.getTransactionTypeId("Transfer");
        if (transferTypeId.isEmpty()) {
            System.out.println("❌ Transfer transaction type not found!");
            return false;
        }

        try {
            // Create transfer transaction using static factory method
            Transaction transferTransaction = Transaction.createTransaction(
                    fromAccountId,          // sender_id
                    toAccountId,            // receiver_id
                    transferTypeId.get(),   // transaction_type_id
                    amount,                 // original amount (in sender currency)
                    "Success",              // status
                    transferRemark          // remark with conversion info
            );

            // Save transaction
            boolean transactionCreated = transactionRepository.createTransaction(transferTransaction);
            if (!transactionCreated) {
                System.out.println("❌ Failed to create transfer transaction!");
                return false;
            }

            // Update sender balance
            BigDecimal newFromBalance = fromAccount.getBalance().subtract(amount);
            boolean fromBalanceUpdated = accountRepository.updateBalanceById(fromAccountId, newFromBalance);
            if (!fromBalanceUpdated) {
                System.out.println("❌ Failed to update sender account balance!");
                return false;
            }

            // Update receiver balance
            BigDecimal newToBalance = toAccount.getBalance().add(convertedAmount);
            boolean toBalanceUpdated = accountRepository.updateBalanceById(toAccountId, newToBalance);
            if (!toBalanceUpdated) {
                System.out.println("❌ Failed to update receiver account balance!");
                // TODO: Consider rollback sender balance update
                return false;
            }

            System.out.println("✅ Transfer successful!");
            System.out.println("   From: " + fromAccount.getAccountName() + " - " + fromAccount.getFormattedAccountNo());
            System.out.println("   To: " + toAccount.getAccountName() + " - " + toAccount.getFormattedAccountNo());
            System.out.println("   Amount Sent: " + formatCurrency(amount, fromAccount.getAccountCurrency()));

            if (!fromAccount.getAccountCurrency().equals(toAccount.getAccountCurrency())) {
                System.out.println("   Amount Received: " + formatCurrency(convertedAmount, toAccount.getAccountCurrency()));
                System.out.println("   Exchange Rate: " + getExchangeRate(fromAccount.getAccountCurrency(), toAccount.getAccountCurrency()));
            }

            System.out.println("   New Balance (From): " + formatCurrency(newFromBalance, fromAccount.getAccountCurrency()));
            System.out.println("   New Balance (To): " + formatCurrency(newToBalance, toAccount.getAccountCurrency()));
            return true;

        } catch (Exception e) {
            System.err.println("Error processing transfer: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check daily transaction limits for Saving accounts
     */
    private boolean checkDailyLimit(Integer accountId, BigDecimal amount, String currency) {
        // Get today's transactions for this account
        List<Transaction> todayTransactions = getTodayTransactions(accountId);

        // Calculate total amount transacted today (withdrawals and transfers out)
        BigDecimal totalToday = todayTransactions.stream()
                .filter(t -> t.getSenderId().equals(accountId)) // Only outgoing transactions
                .filter(t -> "Success".equalsIgnoreCase(t.getStatus()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Add current transaction amount
        BigDecimal totalAfterTransaction = totalToday.add(amount);

        // Get appropriate limit
        BigDecimal dailyLimit = "USD".equalsIgnoreCase(currency) ? SAVING_DAILY_LIMIT_USD : SAVING_DAILY_LIMIT_KHR;

        if (totalAfterTransaction.compareTo(dailyLimit) > 0) {
            System.out.println("❌ Daily transaction limit exceeded!");
            System.out.println("   Daily Limit: " + formatCurrency(dailyLimit, currency));
            System.out.println("   Already Used Today: " + formatCurrency(totalToday, currency));
            System.out.println("   Requested: " + formatCurrency(amount, currency));
            System.out.println("   Remaining: " + formatCurrency(dailyLimit.subtract(totalToday), currency));
            return false;
        }

        return true;
    }

    /**
     * Get today's transactions for an account
     */
    private List<Transaction> getTodayTransactions(Integer accountId) {
        List<Transaction> allTransactions = transactionRepository.findTransactionsByAccountId(accountId);
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        return allTransactions.stream()
                .filter(t -> t.getCreatedAt().isAfter(startOfDay) && t.getCreatedAt().isBefore(endOfDay))
                .toList();
    }

    /**
     * Convert currency between USD and KHR
     */
    public BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount; // No conversion needed
        }

        if ("USD".equalsIgnoreCase(fromCurrency) && "KHR".equalsIgnoreCase(toCurrency)) {
            return amount.multiply(USD_TO_KHR_RATE);
        } else if ("KHR".equalsIgnoreCase(fromCurrency) && "USD".equalsIgnoreCase(toCurrency)) {
            return amount.multiply(KHR_TO_USD_RATE);
        }

        throw new IllegalArgumentException("Unsupported currency conversion: " + fromCurrency + " to " + toCurrency);
    }

    /**
     * Get exchange rate for display
     */
    public String getExchangeRate(String fromCurrency, String toCurrency) {
        if ("USD".equalsIgnoreCase(fromCurrency) && "KHR".equalsIgnoreCase(toCurrency)) {
            return "1 USD = " + USD_TO_KHR_RATE + " KHR";
        } else if ("KHR".equalsIgnoreCase(fromCurrency) && "USD".equalsIgnoreCase(toCurrency)) {
            return "1 KHR = " + KHR_TO_USD_RATE + " USD";
        }
        return "1:1";
    }

    /**
     * Get customer's transaction history
     */
    public List<Transaction> getCustomerTransactionHistory(Integer customerId) {
        return transactionRepository.findTransactionsByCustomerId(customerId);
    }

    /**
     * Get account's transaction history
     */
    public List<Transaction> getAccountTransactionHistory(Integer accountId) {
        return transactionRepository.findTransactionsByAccountId(accountId);
    }

    /**
     * Find transaction by ID
     */
    public Optional<Transaction> findTransactionById(Integer transactionId) {
        return transactionRepository.findTransactionById(transactionId);
    }

    /**
     * Format currency amount
     */
    private String formatCurrency(BigDecimal amount, String currency) {
        if ("USD".equalsIgnoreCase(currency)) {
            return "$" + String.format("%,.2f", amount);
        } else if ("KHR".equalsIgnoreCase(currency)) {
            return "៛" + String.format("%,.0f", amount);
        }
        return amount + " " + currency;
    }

    /**
     * Validate PIN (to be implemented with CustomerService)
     */
    public boolean validatePin(Integer customerId, String pin) {
        // TODO: Implement PIN validation with CustomerService
        // For now, return true (to be implemented in Phase 4)
        return true;
    }

    /**
     * Get daily transaction limits
     */
    public BigDecimal getDailyLimit(String currency) {
        return "USD".equalsIgnoreCase(currency) ? SAVING_DAILY_LIMIT_USD : SAVING_DAILY_LIMIT_KHR;
    }

    /**
     * Get remaining daily limit for an account
     */
    public BigDecimal getRemainingDailyLimit(Integer accountId, String currency) {
        List<Transaction> todayTransactions = getTodayTransactions(accountId);

        BigDecimal totalToday = todayTransactions.stream()
                .filter(t -> t.getSenderId().equals(accountId))
                .filter(t -> "Success".equalsIgnoreCase(t.getStatus()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dailyLimit = getDailyLimit(currency);
        return dailyLimit.subtract(totalToday).max(BigDecimal.ZERO);
    }
}