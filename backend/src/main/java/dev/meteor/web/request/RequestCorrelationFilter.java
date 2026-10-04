package dev.meteor.web.request;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String REQUEST_ATTRIBUTE = RequestCorrelationFilter.class.getName() + ".requestId";
    public static final String MDC_KEY = "requestId";

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestCorrelationFilter.class);
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(HEADER_NAME));
        String previousRequestId = MDC.get(MDC_KEY);
        long startedAt = System.nanoTime();

        request.setAttribute(REQUEST_ATTRIBUTE, requestId);
        response.setHeader(HEADER_NAME, requestId);
        MDC.put(MDC_KEY, requestId);

        try {
            filterChain.doFilter(request, response);
            LOGGER.info(
                    "HTTP request completed method={} path={} status={} durationMs={}",
                    request.getMethod(),
                    safePath(request.getRequestURI()),
                    response.getStatus(),
                    elapsedMillis(startedAt)
            );
        } catch (IOException | ServletException | RuntimeException exception) {
            LOGGER.warn(
                    "HTTP request failed method={} path={} durationMs={}",
                    request.getMethod(),
                    safePath(request.getRequestURI()),
                    elapsedMillis(startedAt),
                    exception
            );
            throw exception;
        } finally {
            if (previousRequestId == null) {
                MDC.remove(MDC_KEY);
            } else {
                MDC.put(MDC_KEY, previousRequestId);
            }
        }
    }

    public static String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_ATTRIBUTE);
        return value instanceof String requestId && !requestId.isBlank() ? requestId : "unavailable";
    }

    static String safePath(String path) {
        return path.startsWith("/api/downloads/") ? "/api/downloads/{token}" : path;
    }

    private static String resolveRequestId(String candidate) {
        if (candidate != null && SAFE_REQUEST_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
