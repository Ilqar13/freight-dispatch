package az.freight.dto;

import az.freight.enums.CustomerStatus;
import az.freight.enums.CustomerType;
import java.time.Instant;
import lombok.Data;

@Data
public class CustomerDto {
    private Long id;
    private CustomerType customerType;
    private String name;
    private String taxId;
    private String legalAddress;
    private String contactName;
    private String phone;
    private String email;
    private String bankName;
    private String bankAccount;
    private String contractNo;
    private int paymentTermsDays;
    private Double creditLimit;
    private CustomerStatus status;
    private String notes;
    private Instant createdAt;
    /** Dispetçer siyahısı üçün: müştərinin sifariş sayı. */
    private Long orderCount;
}
