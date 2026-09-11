package org.example.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dto.AmountRequest;
import org.example.dto.BalanceChangeResponse;
import org.example.exceptions.AccountNotFoundException;
import org.example.exceptions.InsufficientFoundsException;
import org.example.model.Account;
import org.example.service.AccountService;



import java.io.IOException;
import java.math.BigDecimal;

public class AccountServlet extends HttpServlet {
    private final AccountService accountService;
    private final ObjectMapper objectMapper = new  ObjectMapper()   ;

    public AccountServlet(AccountService accountService) {
        this.accountService = accountService;

    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp ) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        Account newAccount = objectMapper.readValue(req.getInputStream(), Account.class);

        Account created = accountService.createAccount(
                newAccount.getOwner_name(),
                newAccount.getBalance()
        );
        String json = objectMapper.writeValueAsString(created);
        resp.getWriter().write(json);
        resp.setStatus(HttpServletResponse.SC_CREATED); //201

    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp ) throws IOException {
//        GET/accounts/{id}
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        Long id = extractIdFromPath(pathInfo);

        try{
            Account account = accountService.getAccount(id);
            String json = objectMapper.writeValueAsString(account);
            resp.getWriter().write(json);
            resp.setStatus(HttpServletResponse.SC_OK);

        }catch(AccountNotFoundException e){
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\":\"Account not found\"}");
        }
    }

    private Long extractIdFromPath(String pathInfo){
        String path = pathInfo.substring(1);
        return Long.parseLong(path);
    }

    protected void doPut(HttpServletRequest req, HttpServletResponse resp ) throws IOException {
        //PUT/accounts/{id}/deposit
        //PUT/accounts/{id}/withdraw
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        String[] parts = pathInfo.split("/");

        Long id =Long.parseLong(parts[1]);
        String operation = parts[2];

        AmountRequest amountRequest = objectMapper.readValue(req.getInputStream(), AmountRequest.class);

        if (amountRequest.getAmount() == null || amountRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"Ошбка\":\"веденная сумма должна быть больше нуля\"}");
            return;
        }

        try{
            BalanceChangeResponse response;
            if("deposit".equals(operation)){
                response = accountService.deposit(id,amountRequest.getAmount());

            }else if("withdraw".equals(operation)){
                response = accountService.withdraw(id, amountRequest.getAmount());

            }else{
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            String json = objectMapper.writeValueAsString(response);
            resp.getWriter().write(json);
            resp.setStatus(HttpServletResponse.SC_OK);


        }catch(AccountNotFoundException e){
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\":\"Account not found\"}");
        }catch(InsufficientFoundsException e){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);


            String jsonError = String.format(
                    "{\"ОШИБКА\":\"Запрошенная сумма больше доступной\", \"Запрошено\": %s, \"Доступно\": %s}",
                    e.getRequested(),
                    e.getAvailable()
            );

            resp.getWriter().write(jsonError);
        }
    }


}
