package az.freight.service;

import az.freight.dto.CustomerAdminUpdateDto;
import az.freight.dto.CustomerDto;
import az.freight.dto.CustomerProfileDto;
import az.freight.dto.CustomerRegisterDto;
import az.freight.dto.OrderDto;
import az.freight.dto.PasswordResetDto;
import az.freight.entity.Customer;
import az.freight.entity.Order;
import az.freight.enums.CustomerStatus;
import az.freight.enums.CustomerType;
import az.freight.enums.OrderStatus;
import az.freight.exception.ApiException;
import az.freight.exception.ResourceNotFoundException;
import az.freight.mapper.CustomerMapper;
import az.freight.mapper.OrderMapper;
import az.freight.repository.CustomerRepository;
import az.freight.repository.CustomerSessionRepository;
import az.freight.repository.OrderRepository;
import az.freight.util.PhoneUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Müştəri qeydiyyatı, profil və dispetçer idarəsi, müştərinin öz sifarişləri. */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customers;
    private final CustomerSessionRepository sessions;
    private final OrderRepository orders;
    private final CustomerMapper mapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final OrderEventService eventLog;
    private final PasswordEncoder encoder;

    // ------------------------------------------------------------ qeydiyyat

    /** Müştəri özü qeydiyyatdan keçir: status PENDING, dispetçer təsdiq edənə qədər sifariş verə bilməz. */
    @Transactional
    public CustomerDto register(CustomerRegisterDto dto) {
        return mapper.toDto(createCustomer(dto, CustomerStatus.PENDING));
    }

    /** Dispetçer müştərini əl ilə yaradır: dərhal ACTIVE. */
    @Transactional
    public CustomerDto createByDispatcher(CustomerRegisterDto dto) {
        return mapper.toDto(createCustomer(dto, CustomerStatus.ACTIVE));
    }

    private Customer createCustomer(CustomerRegisterDto dto, CustomerStatus status) {
        String phone = PhoneUtil.normalize(dto.getPhone());
        if (phone.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Telefon tələb olunur");
        }
        validateProfile(dto);
        if (customers.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu telefon nömrəsi ilə müştəri artıq qeydiyyatdadır");
        }
        String taxId = blank(dto.getTaxId());
        if (taxId != null && customers.existsByTaxId(taxId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Bu VÖEN ilə müştəri artıq qeydiyyatdadır");
        }
        Customer c = new Customer();
        c.setPhone(phone);
        c.setPasswordHash(encoder.encode(dto.getPassword()));
        c.setStatus(status);
        applyProfile(c, dto);
        return customers.save(c);
    }

    // ------------------------------------------------------------ profil

    @Transactional
    public CustomerDto updateProfile(Customer current, CustomerProfileDto dto) {
        Customer c = customers.findById(current.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Müştəri", current.getId()));
        validateProfile(dto);
        String taxId = blank(dto.getTaxId());
        if (taxId != null) {
            customers.findByTaxId(taxId).ifPresent(o -> {
                if (!o.getId().equals(c.getId())) {
                    throw new ApiException(HttpStatus.CONFLICT, "Bu VÖEN başqa müştəriyə aiddir");
                }
            });
        }
        applyProfile(c, dto);
        return mapper.toDto(c);
    }

    // ------------------------------------------------------------ dispetçer

    @Transactional(readOnly = true)
    public Page<CustomerDto> list(CustomerStatus status, Pageable pageable) {
        Page<Customer> page = status == null ? customers.findAll(pageable) : customers.findByStatus(status, pageable);
        return page.map(c -> {
            CustomerDto d = mapper.toDto(c);
            d.setOrderCount(orders.findByCustomerId(c.getId(), PageRequest.of(0, 1)).getTotalElements());
            return d;
        });
    }

    @Transactional(readOnly = true)
    public CustomerDto get(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return customers.countByStatus(CustomerStatus.PENDING);
    }

    @Transactional
    public CustomerDto adminUpdate(Long id, CustomerAdminUpdateDto dto) {
        Customer c = find(id);
        validateProfile(dto);
        String taxId = blank(dto.getTaxId());
        if (taxId != null) {
            customers.findByTaxId(taxId).ifPresent(o -> {
                if (!o.getId().equals(id)) {
                    throw new ApiException(HttpStatus.CONFLICT, "Bu VÖEN başqa müştəriyə aiddir");
                }
            });
        }
        applyProfile(c, dto);
        c.setStatus(dto.getStatus());
        c.setContractNo(blank(dto.getContractNo()));
        c.setPaymentTermsDays(dto.getPaymentTermsDays());
        c.setCreditLimit(dto.getCreditLimit());
        c.setNotes(blank(dto.getNotes()));
        if (dto.getStatus() == CustomerStatus.BLOCKED) {
            sessions.deleteByCustomer(id);
        }
        return mapper.toDto(c);
    }

    @Transactional
    public void resetPassword(Long id, PasswordResetDto dto) {
        Customer c = find(id);
        c.setPasswordHash(encoder.encode(dto.getPassword()));
        c.setFailedAttempts(0);
        c.setLockedUntil(null);
        sessions.deleteByCustomer(id);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> ordersOf(Long customerId, Pageable pageable) {
        find(customerId);
        return orders.findByCustomerId(customerId, pageable).map(orderMapper::toDto);
    }

    // ------------------------------------------------------------ müştəri portalı

    @Transactional(readOnly = true)
    public Page<OrderDto> myOrders(Customer c, int page, int size) {
        return orders.findByCustomerId(c.getId(),
                PageRequest.of(page, Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(orderMapper::toDto);
    }

    @Transactional
    public OrderDto createOrder(Customer c, OrderDto dto) {
        return orderService.createForCustomer(dto, c);
    }

    @Transactional(readOnly = true)
    public OrderDto myOrder(Customer c, Long orderId) {
        return orderMapper.toDto(owned(c, orderId));
    }

    /** Müştəri yalnız maşın təyin olunmamış (gözləyən) sifarişini ləğv edə bilər. */
    @Transactional
    public OrderDto cancelOrder(Customer c, Long orderId, String reason) {
        Order o = owned(c, orderId);
        if (o.getStatus() != OrderStatus.CREATED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Maşın artıq təyin olunub. Ləğv etmək üçün dispetçerlə əlaqə saxlayın");
        }
        var upd = new az.freight.dto.OrderStatusUpdateDto();
        upd.setStatus(OrderStatus.CANCELLED);
        upd.setReason(reason == null || reason.isBlank() ? "Müştəri ləğv etdi" : reason);
        return orderService.updateStatus(orderId, upd, c.getName());
    }

    @Transactional(readOnly = true)
    public List<az.freight.dto.OrderEventDto> myOrderEvents(Customer c, Long orderId) {
        owned(c, orderId);
        return eventLog.list(orderId);
    }

    private Order owned(Customer c, Long orderId) {
        Order o = orders.findById(orderId).orElseThrow(() -> ResourceNotFoundException.of("Sifariş", orderId));
        if (!c.getId().equals(o.getCustomerId())) {
            // Başqasının sifarişinin mövcudluğunu açıqlamamaq üçün "tapılmadı"
            throw ResourceNotFoundException.of("Sifariş", orderId);
        }
        return o;
    }

    // ------------------------------------------------------------ köməkçilər

    private void validateProfile(CustomerProfileDto p) {
        if (p.getCustomerType() == CustomerType.COMPANY && blank(p.getTaxId()) == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Hüquqi şəxs üçün VÖEN tələb olunur");
        }
    }

    private void applyProfile(Customer c, CustomerProfileDto p) {
        c.setCustomerType(p.getCustomerType());
        c.setName(p.getName().trim());
        c.setTaxId(blank(p.getTaxId()));
        c.setLegalAddress(blank(p.getLegalAddress()));
        c.setContactName(p.getContactName().trim());
        c.setEmail(blank(p.getEmail()));
        c.setBankName(blank(p.getBankName()));
        c.setBankAccount(blank(p.getBankAccount()) == null ? null : p.getBankAccount().trim().toUpperCase());
    }

    private Customer find(Long id) {
        return customers.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Müştəri", id));
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
