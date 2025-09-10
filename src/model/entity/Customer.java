// model/entity/Customer.java
package model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    private Integer id;
    private Integer kycId;
    private String fullName;
    private LocalDate dob;
    private String gender;
    private String address;
    private String cityOrProvince;
    private String country;
    private String zipCode;
    private String phoneNumber;
    private String email;
    private String employmentType;
    private String companyName;
    private String position;
    private String mainSourceOfIncome;
    private String monthlyIncomeRange;
    private String remark;
    private String pin; // 6-digit banking PIN
    private Integer customerSegmentId = 1; // Default to Retail segment
    private boolean isDeleted = false;

    /**
     * Constructor for creating new customer (without ID)
     */
    public Customer(String fullName, LocalDate dob, String gender, String address,
                    String cityOrProvince, String country, String zipCode,
                    String phoneNumber, String email, String employmentType,
                    String companyName, String position, String mainSourceOfIncome,
                    String monthlyIncomeRange, String pin) {
        this.fullName = fullName;
        this.dob = dob;
        this.gender = gender;
        this.address = address;
        this.cityOrProvince = cityOrProvince;
        this.country = country;
        this.zipCode = zipCode;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.employmentType = employmentType;
        this.companyName = companyName;
        this.position = position;
        this.mainSourceOfIncome = mainSourceOfIncome;
        this.monthlyIncomeRange = monthlyIncomeRange;
        this.pin = pin;
        this.customerSegmentId = 1; // Default to Retail segment
        this.isDeleted = false;
    }

    /**
     * Get formatted full address
     */
    public String getFullAddress() {
        StringBuilder fullAddress = new StringBuilder();

        if (address != null && !address.trim().isEmpty()) {
            fullAddress.append(address);
        }

        if (cityOrProvince != null && !cityOrProvince.trim().isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(cityOrProvince);
        }

        if (country != null && !country.trim().isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(country);
        }

        if (zipCode != null && !zipCode.trim().isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(" ");
            fullAddress.append(zipCode);
        }

        return fullAddress.length() > 0 ? fullAddress.toString() : "No address provided";
    }

    /**
     * Get customer display name
     */
    public String getDisplayName() {
        return fullName != null ? fullName : "Unknown Customer";
    }

    /**
     * Check if customer profile is complete
     */
    public boolean isProfileComplete() {
        return fullName != null && !fullName.trim().isEmpty() &&
                dob != null &&
                phoneNumber != null && !phoneNumber.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() &&
                pin != null && pin.length() == 6;
    }

    /**
     * Get age from date of birth
     */
    public int getAge() {
        if (dob == null) return 0;
        return java.time.Period.between(dob, LocalDate.now()).getYears();
    }

    /**
     * Get customer summary for display
     */
    public String getSummary() {
        return String.format("""
            Customer: %s
            Age: %d years old
            Email: %s
            Phone: %s
            Address: %s
            Employment: %s
            Income Range: %s
            """,
                getDisplayName(),
                getAge(),
                email != null ? email : "Not provided",
                phoneNumber != null ? phoneNumber : "Not provided",
                getFullAddress(),
                employmentType != null ? employmentType : "Not provided",
                monthlyIncomeRange != null ? monthlyIncomeRange : "Not provided"
        );
    }
}