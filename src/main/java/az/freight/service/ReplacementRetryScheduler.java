package az.freight.service;

import az.freight.enums.IncidentStatus;
import az.freight.repository.IncidentRepository;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Boş maşın tapılmayan nasazlıqlar üçün axtarışı müəyyən fasilə ilə təkrarlayır. */
@Component
@RequiredArgsConstructor
public class ReplacementRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReplacementRetryScheduler.class);

    private final IncidentRepository incidents;
    private final IncidentService service;

    @Scheduled(fixedDelayString = "${dispatch.replacement-retry-seconds:30}", timeUnit = TimeUnit.SECONDS,
            initialDelayString = "${dispatch.replacement-retry-seconds:30}")
    public void retryAwaiting() {
        for (Long id : incidents.findIdsByStatus(IncidentStatus.AWAITING_TRUCK)) {
            try {
                service.retry(id);
            } catch (RuntimeException ex) {
                log.error("Nasazlıq #{} üçün təkrar cəhd uğursuz oldu", id, ex);
            }
        }
    }
}
