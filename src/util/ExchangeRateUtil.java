// util/ExchangeRateUtil.java
package util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ExchangeRateUtil {

    // Static exchange rates as requested
    private static final BigDecimal USD_TO_KHR_RATE = new BigDecimal("4100");
    private static final BigDecimal KHR_TO_USD_RATE = new BigDecimal("0.000244");

    /**
     * Convert amount from one currency to another
     * @param amount Amount to convert
     * @param fromCurrency Source currency (USD or KHR)
     * @param toCurrency Target currency (USD or KHR)
     * @return Converted amount
     */
    public static BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null || fromCurrency == null || toCurrency == null) {
            throw new IllegalArgumentException("Amount and currencies cannot be null");
        }

        // No conversion needed if same currency
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        BigDecimal result;

        if ("USD".equalsIgnoreCase(fromCurrency) && "KHR".equalsIgnoreCase(toCurrency)) {
            // USD to KHR
            result = amount.multiply(USD_TO_KHR_RATE);
            // Round to nearest whole number for KHR
            return result.setScale(0, RoundingMode.HALF_UP);

        } else if ("KHR".equalsIgnoreCase(fromCurrency) && "USD".equalsIgnoreCase(toCurrency)) {
            // KHR to USD
            result = amount.multiply(KHR_TO_USD_RATE);
            // Round to 2 decimal places for USD
            return result.setScale(2, RoundingMode.HALF_UP);

        } else {
            throw new IllegalArgumentException("Unsupported currency conversion: " + fromCurrency + " to " + toCurrency);
        }
    }

    /**
     * Get exchange rate for display purposes
     * @param fromCurrency Source currency
     * @param toCurrency Target currency
     * @return Exchange rate as string
     */
    public static String getExchangeRateDisplay(String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null) {
            return "Invalid currencies";
        }

        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return "1:1 (Same currency)";
        }

        if ("USD".equalsIgnoreCase(fromCurrency) && "KHR".equalsIgnoreCase(toCurrency)) {
            return "1 USD = " + USD_TO_KHR_RATE.toPlainString() + " KHR";
        } else if ("KHR".equalsIgnoreCase(fromCurrency) && "USD".equalsIgnoreCase(toCurrency)) {
            return "1 KHR = " + KHR_TO_USD_RATE.toPlainString() + " USD";
        } else {
            return "Unsupported conversion";
        }
    }

    /**
     * Get current USD to KHR rate
     * @return USD to KHR exchange rate
     */
    public static BigDecimal getUsdToKhrRate() {
        return USD_TO_KHR_RATE;
    }

    /**
     * Get current KHR to USD rate
     * @return KHR to USD exchange rate
     */
    public static BigDecimal getKhrToUsdRate() {
        return KHR_TO_USD_RATE;
    }

    /**
     * Check if currency is supported
     * @param currency Currency code to check
     * @return true if supported
     */
    public static boolean isSupportedCurrency(String currency) {
        return currency != null &&
                ("USD".equalsIgnoreCase(currency) || "KHR".equalsIgnoreCase(currency));
    }

    /**
     * Format currency amount for display
     * @param amount Amount to format
     * @param currency Currency code
     * @return Formatted currency string
     */
    public static String formatCurrency(BigDecimal amount, String currency) {
        if (amount == null || currency == null) {
            return "Invalid amount";
        }

        if ("USD".equalsIgnoreCase(currency)) {
            return "$" + String.format("%,.2f", amount);
        } else if ("KHR".equalsIgnoreCase(currency)) {
            return "៛" + String.format("%,.0f", amount);
        } else {
            return amount.toPlainString() + " " + currency;
        }
    }

    /**
     * Get currency symbol
     * @param currency Currency code
     * @return Currency symbol
     */
    public static String getCurrencySymbol(String currency) {
        if (currency == null) {
            return "";
        }

        return switch (currency.toUpperCase()) {
            case "USD" -> "$";
            case "KHR" -> "៛";
            default -> currency;
        };
    }

    /**
     * Calculate conversion with fees (if needed in future)
     * @param amount Original amount
     * @param fromCurrency Source currency
     * @param toCurrency Target currency
     * @param feePercentage Fee percentage (0.01 = 1%)
     * @return ConversionResult with original amount, converted amount, and fees
     */
    public static ConversionResult calculateConversionWithFees(BigDecimal amount, String fromCurrency,
                                                               String toCurrency, BigDecimal feePercentage) {
        BigDecimal convertedAmount = convertCurrency(amount, fromCurrency, toCurrency);
        BigDecimal fee = amount.multiply(feePercentage).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = convertedAmount.subtract(convertCurrency(fee, fromCurrency, toCurrency));

        return new ConversionResult(amount, convertedAmount, fee, finalAmount, fromCurrency, toCurrency);
    }

    /**
     * Result class for currency conversion with fees
     */
    public static class ConversionResult {
        private final BigDecimal originalAmount;
        private final BigDecimal convertedAmount;
        private final BigDecimal fee;
        private final BigDecimal finalAmount;
        private final String fromCurrency;
        private final String toCurrency;

        public ConversionResult(BigDecimal originalAmount, BigDecimal convertedAmount, BigDecimal fee,
                                BigDecimal finalAmount, String fromCurrency, String toCurrency) {
            this.originalAmount = originalAmount;
            this.convertedAmount = convertedAmount;
            this.fee = fee;
            this.finalAmount = finalAmount;
            this.fromCurrency = fromCurrency;
            this.toCurrency = toCurrency;
        }

        // Getters
        public BigDecimal getOriginalAmount() { return originalAmount; }
        public BigDecimal getConvertedAmount() { return convertedAmount; }
        public BigDecimal getFee() { return fee; }
        public BigDecimal getFinalAmount() { return finalAmount; }
        public String getFromCurrency() { return fromCurrency; }
        public String getToCurrency() { return toCurrency; }

        @Override
        public String toString() {
            return String.format("Conversion: %s %s → %s %s (Fee: %s %s, Final: %s %s)",
                    formatCurrency(originalAmount, fromCurrency), fromCurrency,
                    formatCurrency(convertedAmount, toCurrency), toCurrency,
                    formatCurrency(fee, fromCurrency), fromCurrency,
                    formatCurrency(finalAmount, toCurrency), toCurrency);
        }
    }
}