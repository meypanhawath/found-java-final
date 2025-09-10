// model/entity/Account.java
package model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

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

    // Additional fields for display purposes (from joins)
    private String accountTypeName; // From join with account_types
    private String customerName; // From join with customers

    // Constructor for creating new account
    public Account(Integer customerId, String accountNo, String accountName,
                   String accountCurrency, Integer accountTypeId) {
        this.customerId = customerId;
        this.accountNo = accountNo;
        this.accountName = accountName;
        this.accountCurrency = accountCurrency;
        this.accountTypeId = accountTypeId;
        this.balance = BigDecimal.ZERO;
        this.overLimit = BigDecimal.ZERO;
        this.isFreeze = false;
        this.isDeleted = false;
    }

    /**
     * Check if account is active (not frozen and not deleted)
     */
    public boolean isActive() {
        return !isFreeze && !isDeleted;
    }

    /**
     * Get account status as string
     */
    public String getStatus() {
        if (isDeleted) return "Deleted";
        if (isFreeze) return "Frozen";
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
        return String.format("%s - %s\n   Balance: %s\n   Status: %s",
                accountName, getFormattedAccountNo(), getFormattedBalance(), getStatus());
    }

    /**
     * Get account summary for selection
     */
    public String getAccountSummary() {
        return String.format("%s (%s) - %s",
                accountName, accountCurrency, getFormattedBalance());
    }
}