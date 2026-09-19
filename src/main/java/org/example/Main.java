package org.example;

import org.example.model.Account;
import org.example.repository.AccountRepository;
import org.example.repository.JdbcAccountRepository;
import org.example.service.AccountService;
import org.example.service.AccountServiceImpl;
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
        AccountRepository repository = new JdbcAccountRepository();
//        Account account = repository.findById(1L);
//        System.out.println(account.getOwner_name() + " — " + account.getBalance());
//        AccountRepository repository = new JdbcAccountRepository();
        AccountService service = new AccountServiceImpl(repository);

        Server server = new Server(8080);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new AccountServlet(service)), "/accounts/*");

        server.start();
        server.join();
    }
}