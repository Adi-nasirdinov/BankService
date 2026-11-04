package org.example.config;

import org.example.dto.ReconciliationReport;
import org.example.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationScheduler.class);
    private final ReconciliationService reconciliationService;

    public ReconciliationScheduler(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(initialDelay = 5000, fixedRate = 30000)
    public void runReconciliation() {
        try {
            log.info("[RECONCILIATION] Запуск сверки балансов...");
            ReconciliationReport report = reconciliationService.runReconciliation();

            if (report.isBalanced()) {
                log.info("[RECONCILIATION] Сверка успешно завершена. Ошибок не обнаружено.");
            } else {
                log.error("[RECONCILIATION ALERT] Обнаружены аномалии в системе!");
                report.getAnomalies().forEach(log::error);
            }
        } catch (Exception e) {
            log.error("[RECONCILIATION ERROR] Сбой при выполнении сверки: {}", e.getMessage(), e);
        }
    }
}