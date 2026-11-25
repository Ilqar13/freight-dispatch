package az.freight.service;

import az.freight.dto.CustomerDto;
import az.freight.dto.CustomerLoginRequest;
import az.freight.dto.CustomerLoginResponse;
import az.freight.entity.Customer;
import az.freight.entity.CustomerSession;
import az.freight.enums.CustomerStatus;
import az.freight.exception.ApiException;
import az.freight.mapper.CustomerMapper;
import az.freight.repository.CustomerRepository;
import az.freight.repository.CustomerSessionRepository;
import az.freight.util.PhoneUtil;
import az.freight.util.TokenUtil;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Müştəri girişi: telefon + parol. Mexanizm sürücü girişi ilə eynidir (token hash-i, 5 uğursuz cəhd = 5 dəq blok). */
@Service
public class CustomerAuthService {

    private static final Logger log = LoggerFactory.getLogger(CustomerAuthService.class);
    private static final int MAX_FAILED = 5;
    private static final long LOCK_MINUTES = 5;
    private static final String BAD_CREDENTIALS = "Telefon və ya parol yanlışdır";

    private final CustomerRepository customers;
    private final CustomerSessionRepository sessions;
    private final CustomerMapper mapper;
    private final PasswordEncoder encoder;
    private final long sessionHours;
    private final String dummyHash;

    public CustomerAuthService(CustomerRepository customers, CustomerSessionRepository sessions,
                               CustomerMapper mapper, PasswordEncoder encoder,
                               @Value("${customer.session-hours:24}") long sessionHours) {
        this.customers = customers;
        this.sessions = sessions;
        this.mapper = mapper;
        this.encoder = encoder;
        this.sessionHours = sessionHours;
        this.dummyHash = encoder.encode("dummy-password-for-timing");
    }

    @Transactional(noRollbackFor = ApiException.class)
    public CustomerLoginResponse login(CustomerLoginRequest req) {
        Customer c = customers.findByPhone(PhoneUtil.normalize(req.getPhone())).orElse(null);
        if (c == null) {
            encoder.matches(req.getPassword(), dummyHash);
            throw new ApiException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS);
        }
        if (c.getLockedUntil() != null && Instant.now().isBefore(c.getLockedUntil())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Çox sayda uğursuz cəhd oldu. Bir neçə dəqiqədən sonra yenidən cəhd edin");
        }
        if (!encoder.matches(req.getPassword(), c.getPasswordHash())) {
            int failed = c.getFailedAttempts() + 1;
            if (failed >= MAX_FAILED) {
                c.setLockedUntil(Instant.now().plus(Duration.ofMinutes(LOCK_MINUTES)));
                failed = 0;
            }
            c.setFailedAttempts(failed);
            customers.save(c);
            throw new ApiException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS);
        }
        if (c.getStatus() == CustomerStatus.BLOCKED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Hesab bloklanıb. Dispetçerlə əlaqə saxlayın");
        }
        c.setFailedAttempts(0);
        c.setLockedUntil(null);
        customers.save(c);

        String token = TokenUtil.newToken();
        CustomerSession s = new CustomerSession();
        s.setTokenHash(TokenUtil.sha256(token));
        s.setCustomerId(c.getId());
        s.setExpiresAt(Instant.now().plus(Duration.ofHours(sessionHours)));
        sessions.save(s);
        return new CustomerLoginResponse(token, s.getExpiresAt(), mapper.toDto(c));
    }

    @Transactional(readOnly = true)
    public Customer authenticate(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Giriş tələb olunur");
        }
        CustomerSession s = sessions.findById(TokenUtil.sha256(token.trim())).orElse(null);
        if (s == null || Instant.now().isAfter(s.getExpiresAt())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Sessiya bitib, yenidən daxil olun");
        }
        Customer c = customers.findById(s.getCustomerId()).orElse(null);
        if (c == null || c.getStatus() == CustomerStatus.BLOCKED) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Hesab aktiv deyil");
        }
        return c;
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessions.deleteById(TokenUtil.sha256(token.trim()));
        }
    }

    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    public void purgeExpiredSessions() {
        int n = sessions.deleteExpired(Instant.now());
        if (n > 0) log.debug("{} köhnə müştəri sessiyası silindi", n);
    }
}
