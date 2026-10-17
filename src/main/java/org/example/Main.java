package org.example;

import org.eclipse.jetty.servlet.FilterHolder;
import org.example.config.IdempotencyFilter;
import org.example.config.ReconciliationScheduler;
import org.example.model.Account;
import org.example.model.EntryType;
import org.example.model.LedgerEntry;
import org.example.repository.*;
import org.example.service.AccountService;
import org.example.service.AccountServiceImpl;
import org.example.service.ReconciliationService;
import org.example.servlet.AccountServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;



//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("DB_URL = [" + System.getenv("DB_URL") + "]");
        System.out.println("DB_USER = [" + System.getenv("DB_USER") + "]");
        System.out.println("DB_PASSWORD = [" + System.getenv("DB_PASSWORD") + "]");


        JdbcReconciliationRepository reconciliationRepository = new JdbcReconciliationRepository();


        ReconciliationService reconciliationService = new ReconciliationService(reconciliationRepository);


        ReconciliationScheduler scheduler = new ReconciliationScheduler(reconciliationService);
        scheduler.start();

//        LedgerRepository ledgerRepository = new JdbcLedgerRepository();
//        LedgerEntry entry = new LedgerEntry(1L, new BigDecimal("100.00"), EntryType.CREDIT, UUID.randomUUID().toString());
//        ledgerRepository.save(entry);
//        System.out.println(ledgerRepository.sumByAccountId(1L));
        LedgerRepository ledgerRepository = new JdbcLedgerRepository();
        AccountRepository accountRepository = new JdbcAccountRepository(ledgerRepository);

        JdbcIdempotencyRepository idempotencyRepository = new JdbcIdempotencyRepository();

        AccountService service = new AccountServiceImpl(accountRepository, ledgerRepository);

        Server server = new Server(8080);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new AccountServlet(service)), "/accounts/*");

        FilterHolder filterHolder = new FilterHolder(new IdempotencyFilter(idempotencyRepository));
        context.addFilter(filterHolder, "/accounts/*", java.util.EnumSet.of(jakarta.servlet.DispatcherType.REQUEST));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Остановка фоновых служб...");
            scheduler.stop();
        }));

        server.start();
        server.join();
    }
}