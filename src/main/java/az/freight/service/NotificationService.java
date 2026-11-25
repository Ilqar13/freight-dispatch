package az.freight.service;

import az.freight.event.IncidentReportedEvent;
import az.freight.event.ReplacementAssignedEvent;
import az.freight.event.TruckAssignedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Hələlik yalnız log yazır. Sonra: SMS/WhatsApp/Telegram və sürücü push bildirişi buraya əlavə olunur. */
@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTruckAssigned(TruckAssignedEvent e) {
        log.info("Sifariş #{} üçün maşın {} təyin olundu, təxmini gəliş {} saat",
                e.orderId(), e.plate(), String.format("%.1f", e.etaHours()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIncident(IncidentReportedEvent e) {
        log.warn("Sifariş #{}: {} maşını dayandı ({}). Dispetçerə və müştəriyə xəbər verilməlidir",
                e.orderId(), e.truckPlate(), e.typeLabel());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReplacement(ReplacementAssignedEvent e) {
        log.info("Sifariş #{}: əvəzedici maşın {} çağırıldı, nasazlıq nöqtəsinə təxminən {} saat. Sürücüyə push göndərilməlidir",
                e.orderId(), e.replacementPlate(), String.format("%.1f", e.pickupEtaHours()));
    }
}
