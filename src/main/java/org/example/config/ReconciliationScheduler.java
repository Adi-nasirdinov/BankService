package org.example.config;
import org.example.dto.ReconciliationReport;
import org.example.service.ReconciliationService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReconciliationScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ReconciliationService reconciliationService;

    public ReconciliationScheduler(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("[RECONCILIATION] Запуск сверки балансов...");
                ReconciliationReport report = reconciliationService.runReconciliation();


                if (report.isBalanced()) {
                    System.out.println("[RECONCILIATION] Сверка успешно завершена. Ошибок не обнаружено.");

                } else {

                    System.err.println("[RECONCILIATION ALERT] Обнаружены аномалии в системе!");
                    report.getAnomalies().forEach(System.err::println);
                }
            } catch (Exception e) {
                System.err.println("[RECONCILIATION ERROR] Сбой при выполнении сверки: " + e.getMessage());
            }
        }, 5, 30, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }
}