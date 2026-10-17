package org.example.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.example.repository.JdbcIdempotencyRepository;
import org.example.repository.JdbcIdempotencyRepository.IdempotencyRecord;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

public class IdempotencyFilter implements Filter {

    private final JdbcIdempotencyRepository idempotencyRepository;

    public IdempotencyFilter(JdbcIdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String pathInfo = req.getPathInfo();
        String idempotencyKey = req.getHeader("X-Idempotency-Key");




        if (idempotencyKey == null || idempotencyKey.isBlank() || pathInfo == null || !pathInfo.contains("/transfer")) {
            chain.doFilter(request, response);
            return;
        }

        Optional<IdempotencyRecord> record = idempotencyRepository.findByKey(idempotencyKey);

        if (record.isPresent()) {
            IdempotencyRecord existing = record.get();
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            if ("COMPLETED".equals(existing.status())) {
                resp.setStatus(existing.responseCode());
                resp.getWriter().write(existing.responseBody());
                return;


            } else if ("IN_PROGRESS".equals(existing.status())) {
                resp.setStatus(409);
                resp.getWriter().write("{\"error\":\"Запрос с этим Idempotency-Key уже обрабатывается\"}");
                return;
            }
        }


        Long accountId = extractAccountId(pathInfo);
        boolean created = idempotencyRepository.saveInProgress(idempotencyKey, accountId, "hash");

        if (!created) {
            resp.setStatus(409);
            resp.getWriter().write("{\"error\":\"Конфликт идемпотентного ключа\"}");
            return;
        }


        ResponseWrapper responseWrapper = new ResponseWrapper(resp);

        try {
            chain.doFilter(request, responseWrapper);

            String responseBody = responseWrapper.getCapturedResponseBody();
            idempotencyRepository.updateToCompleted(idempotencyKey, responseWrapper.getStatus(), responseBody);


            resp.getWriter().write(responseBody);
        } catch (Exception e) {
            String errorJson = "{\"error\":\"" + e.getMessage() + "\"}";
            idempotencyRepository.updateToCompleted(idempotencyKey, 400, errorJson);
            resp.setStatus(400);
            resp.getWriter().write(errorJson);
        }
    }

    private Long extractAccountId(String pathInfo) {
        try {
            return Long.parseLong(pathInfo.split("/")[1]);
        } catch (Exception e) {
            return 0L;
        }
    }


    private static class ResponseWrapper extends HttpServletResponseWrapper {
        private final CharArrayWriter charWriter = new CharArrayWriter();
        private final PrintWriter printWriter = new PrintWriter(charWriter);

        public ResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public PrintWriter getWriter() {
            return printWriter;
        }

        public String getCapturedResponseBody() {
            printWriter.flush();
            return charWriter.toString();
        }
    }
}