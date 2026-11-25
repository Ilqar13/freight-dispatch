package az.freight.service;

import az.freight.dto.DriverDto;
import az.freight.dto.DriverLoginRequest;
import az.freight.dto.DriverLoginResponse;
import az.freight.entity.Driver;
import az.freight.entity.DriverSession;
import az.freight.entity.Truck;
import az.freight.exception.ApiException;
import az.freight.mapper.DriverMapper;
import az.freight.repository.DriverRepository;
import az.freight.repository.DriverSessionRepository;
import az.freight.repository.TruckRepository;
import az.freight.util.PhoneUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sürücü girişi: telefon + PİN. Uğurlu girişdə təsadüfi token verilir (bazada yalnız SHA-256 hash-i saxlanır).
 * 5 uğursuz cəhddən sonra hesab 5 dəqiqə bloklanır.
 */
@Service
public class DriverAuthService {

    private static final Logger log = LoggerFactory.getLogger(DriverAuthService.class);
    private static final int MAX_FAILED = 5;
    private static final long LOCK_MINUTES = 5;
    private static final String BAD_CREDENTIALS = "Telefon və ya PİN yanlışdır";

    private final DriverRepository drivers;
    private final DriverSessionRepository sessions;
    private final TruckRepository trucks;
    private final DriverMapper mapper;
    private final PasswordEncoder encoder;
    private final long sessionHours;
    /** Mövcud olmayan telefon üçün də eyni müddətdə yoxlama aparmaq (istifadəçi sayma hücumunun qarşısı). */
    private final String dummyHash;
    private final SecureRandom random = new SecureRandom();

    public DriverAuthService(DriverRepository drivers, DriverSessionRepository sessions, TruckRepository trucks,
                             DriverMapper mapper, PasswordEncoder encoder,
                             @Value("${driver.session-hours:12}") long sessionHours) {
        this.drivers = drivers;
        this.sessions = sessions;
        this.trucks = trucks;
        this.mapper = mapper;
        this.encoder = encoder;
        this.sessionHours = sessionHours;
        this.dummyHash = encoder.encode("dummy-pin-for-timing");
    }

    /** Uğursuz cəhd sayğacı yazılsın deyə ApiException-da rollback olmur. */
    @Transactional(noRollbackFor = ApiException.class)
    public DriverLoginResponse login(DriverLoginRequest req) {
        Driver driver = drivers.findByPhone(PhoneUtil.normalize(req.getPhone())).orElse(null);
        if (driver == null || !driver.isActive()) {
            encoder.matches(req.getPin(), dummyHash);
            throw new ApiException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS);
        }
        if (driver.getLockedUntil() != null && Instant.now().isBefore(driver.getLockedUntil())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Çox sayda uğursuz cəhd oldu. Bir neçə dəqiqədən sonra yenidən cəhd edin");
        }
        if (!encoder.matches(req.getPin(), driver.getPinHash())) {
            int failed = driver.getFailedAttempts() + 1;
            if (failed >= MAX_FAILED) {
                driver.setLockedUntil(Instant.now().plus(Duration.ofMinutes(LOCK_MINUTES)));
                failed = 0;
            }
            driver.setFailedAttempts(failed);
            drivers.save(driver);
            throw new ApiException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS);
        }
        driver.setFailedAttempts(0);
        driver.setLockedUntil(null);
        drivers.save(driver);

        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        DriverSession s = new DriverSession();
        s.setTokenHash(sha256(token));
        s.setDriverId(driver.getId());
        s.setExpiresAt(Instant.now().plus(Duration.ofHours(sessionHours)));
        sessions.save(s);

        return new DriverLoginResponse(token, s.getExpiresAt(), toDto(driver));
    }

    /** Tokeni yoxlayır və sürücünü qaytarır; etibarsızdırsa 401. */
    @Transactional(readOnly = true)
    public Driver authenticate(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Giriş tələb olunur");
        }
        DriverSession s = sessions.findById(sha256(token.trim())).orElse(null);
        if (s == null || Instant.now().isAfter(s.getExpiresAt())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Sessiya bitib, yenidən daxil olun");
        }
        Driver d = drivers.findById(s.getDriverId()).orElse(null);
        if (d == null || !d.isActive()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Hesab aktiv deyil");
        }
        return d;
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessions.deleteById(sha256(token.trim()));
        }
    }

    @Transactional(readOnly = true)
    public DriverDto toDto(Driver d) {
        Truck t = d.getTruckId() == null ? null : trucks.findById(d.getTruckId()).orElse(null);
        return mapper.toDto(d, t, true);
    }

    @Scheduled(cron = "0 15 * * * *")
    @Transactional
    public void purgeExpiredSessions() {
        int n = sessions.deleteExpired(Instant.now());
        if (n > 0) log.debug("{} köhnə sürücü sessiyası silindi", n);
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
