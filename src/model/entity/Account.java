// model/entity/Account.java
package model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private Integer id;
    private Integer customerId;
    private String accountNo;
    private String accountName;
    private String accountCurrency;
    private BigDecimal balance = BigDecimal.ZERO;
    private BigDecimal overLimit = BigDecimal.ZERO;
    private boolean isFreeze = false;
    private boolean isDeleted = false;
    private Integer accountTypeId;
    private LocalDate maturityDate; // New field for Fixed accounts

    // Additional fields for display purposes (from joins)
    private String accountTypeName; // From join with account_types
    private String customerName; // From join with customers

    // Constructor for creating new account
    public Account(Integer customerId, String accountNo, String accountName,
                   String accountCurrency, Integer accountTypeId, LocalDate maturityDate) {
        this.customerId = customerId;
        this.accountNo = accountNo;
        this.accountName = accountName;
        this.accountCurrency = accountCurrency;
        this.accountTypeId = accountTypeId;
        this.maturityDate = maturityDate;
        this.balance = BigDecimal.ZERO;
        this.overLimit = BigDecimal.ZERO;
        this.isFreeze = false;
        this.isDeleted = false;
    }

    // Constructor for creating new account (backward compatibility)
    public Account(Integer customerId, String accountNo, String accountName,
                   String accountCurrency, Integer accountTypeId) {
        this(customerId, accountNo, accountName, accountCurrency, accountTypeId, null);
    }

    /**
     * Check if account is active (not frozen and not deleted)
     */
    public boolean isActive() {
        return !isFreeze && !isDeleted;
    }

    /**
     * Check if Fixed account is matured (can withdraw)
     */
    public boolean isMatured() {
        if (maturityDate == null) {
            return true; // Not a fixed account or no maturity restriction
        }
        return !LocalDate.now().isBefore(maturityDate);
    }

    /**
     * Get account status as string
     */
    public String getStatus() {
        if (isDeleted) return "Deleted";
        if (isFreeze) return "Frozen";
        if (maturityDate != null && !isMatured()) {
            return "Active (Fixed - Matures: " + maturityDate + ")";
        }
        return "Active";
    }

    /**
     * Get formatted balance with currency symbol
     */
    public String getFormattedBalance() {
        if ("USD".equalsIgnoreCase(accountCurrency)) {
            return "$" + String.format("%,.2f", balance);
        } else if ("KHR".equalsIgnoreCase(accountCurrency)) {
            return "៛" + String.format("%,.0f", balance);
        }
        return balance + " " + accountCurrency;
    }

    /**
     * Get formatted account number (xxx xxx xxx)
     */
    public String getFormattedAccountNo() {
        if (accountNo != null && accountNo.length() == 9) {
            return accountNo.substring(0, 3) + " " +
                    accountNo.substring(3, 6) + " " +
                    accountNo.substring(6, 9);
        }
        return accountNo;
    }

    /**
     * Get short account display (for menus)
     */
    public String getAccountDisplay() {
        return accountName + " - " + getFormattedAccountNo();
    }

    /**
     * Get full account info for listing
     */
    public String getFullAccountInfo() {
        StringBuilder info = new StringBuilder();
        info.append(String.format("%s - %s\n", accountName, getFormattedAccountNo()));
        info.append(String.format("   Balance: %s\n", getFormattedBalance()));
        info.append(String.format("   Status: %s", getStatus()));

        if (maturityDate != null && !isMatured()) {
            info.append(String.format("\n   ⚠️ Withdrawals restricted until: %s", maturityDate));
        }

        return info.toString();
    }

    /**
     * Get account summary for selection
     */
    public String getAccountSummary() {
        return String.format("%s (%s) - %s",
                accountName, accountCurrency, getFormattedBalance());
    }

    /**
     * Get minimum initial deposit for this account type
     */
    public BigDecimal getMinimumInitialDeposit() {
        if ("USD".equalsIgnoreCase(accountCurrency)) {
            return new BigDecimal("5.00"); // $5 minimum
        } else if ("KHR".equalsIgnoreCase(accountCurrency)) {
            return new BigDecimal("20000"); // ៛20,000 minimum
        }
        return BigDecimal.ZERO;
    }
}