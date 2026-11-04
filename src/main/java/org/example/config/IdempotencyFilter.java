package org.example.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.example.repository.JdbcIdempotencyRepository;
import org.example.repository.JdbcIdempotencyRepository.IdempotencyRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private final JdbcIdempotencyRepository idempotencyRepository;

    public IdempotencyFilter(JdbcIdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String idempotencyKey = request.getHeader("X-Idempotency-Key");

        if (idempotencyKey == null || idempotencyKey.isBlank() || requestURI == null || !requestURI.contains("/transfer")) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<IdempotencyRecord> record = idempotencyRepository.findByKey(idempotencyKey);

        if (record.isPresent()) {
            IdempotencyRecord existing = record.get();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            if ("COMPLETED".equals(existing.status())) {
                response.setStatus(existing.responseCode());
                response.getWriter().write(existing.responseBody());
                return;
            } else if ("IN_PROGRESS".equals(existing.status())) {
                response.setStatus(409);
                response.getWriter().write("{\"error\":\"Запрос с этим Idempotency-Key уже обрабатывается\"}");
                return;
            }
        }

        Long accountId = extractAccountId(requestURI);
        boolean created = idempotencyRepository.saveInProgress(idempotencyKey, accountId, "hash");

        if (!created) {
            response.setStatus(409);
            response.getWriter().write("{\"error\":\"Конфликт идемпотентного ключа\"}");
            return;
        }

        ResponseWrapper responseWrapper = new ResponseWrapper(response);

        try {
            filterChain.doFilter(request, responseWrapper);

            String responseBody = responseWrapper.getCapturedResponseBody();
            idempotencyRepository.updateToCompleted(idempotencyKey, responseWrapper.getStatus(), responseBody);

            response.getWriter().write(responseBody);
        } catch (Exception e) {
            String errorJson = "{\"error\":\"" + e.getMessage() + "\"}";
            idempotencyRepository.updateToCompleted(idempotencyKey, 400, errorJson);
            response.setStatus(400);
            response.getWriter().write(errorJson);
        }
    }

    private Long extractAccountId(String uri) {
        try {
            // Разбор пути вида /accounts/123/transfer
            String[] parts = uri.split("/");
            for (int i = 0; i < parts.length; i++) {
                if ("accounts".equals(parts[i]) && i + 1 < parts.length) {
                    return Long.parseLong(parts[i + 1]);
                }
            }
            return 0L;
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