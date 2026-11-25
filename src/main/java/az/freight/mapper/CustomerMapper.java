package az.freight.mapper;

import az.freight.dto.CustomerDto;
import az.freight.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    /** Parol hash-i heç vaxt DTO-ya düşmür. */
    public CustomerDto toDto(Customer e) {
        if (e == null) return null;
        CustomerDto d = new CustomerDto();
        d.setId(e.getId());
        d.setCustomerType(e.getCustomerType());
        d.setName(e.getName());
        d.setTaxId(e.getTaxId());
        d.setLegalAddress(e.getLegalAddress());
        d.setContactName(e.getContactName());
        d.setPhone(e.getPhone());
        d.setEmail(e.getEmail());
        d.setBankName(e.getBankName());
        d.setBankAccount(e.getBankAccount());
        d.setContractNo(e.getContractNo());
        d.setPaymentTermsDays(e.getPaymentTermsDays());
        d.setCreditLimit(e.getCreditLimit());
        d.setStatus(e.getStatus());
        d.setNotes(e.getNotes());
        d.setCreatedAt(e.getCreatedAt());
        return d;
    }
}
