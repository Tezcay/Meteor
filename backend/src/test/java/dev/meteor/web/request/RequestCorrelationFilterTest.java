package dev.meteor.web.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @AfterEach
    void clearMdc() {
        MDC.remove(RequestCorrelationFilter.MDC_KEY);
    }

    @Test
    void preservesSafeClientRequestIdAndRestoresPreviousMdcValue() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String requestId = "browser-checkout_42.1";
        request.addHeader(RequestCorrelationFilter.HEADER_NAME, requestId);
        MDC.put(RequestCorrelationFilter.MDC_KEY, "outer-operation");

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertEquals(requestId, request.getAttribute(RequestCorrelationFilter.REQUEST_ATTRIBUTE));
            assertEquals(requestId, MDC.get(RequestCorrelationFilter.MDC_KEY));
        });

        assertEquals(requestId, response.getHeader(RequestCorrelationFilter.HEADER_NAME));
        assertEquals("outer-operation", MDC.get(RequestCorrelationFilter.MDC_KEY));
    }

    @Test
    void replacesUnsafeRequestIdAndClearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RequestCorrelationFilter.HEADER_NAME, "contains spaces");

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        String generatedRequestId = response.getHeader(RequestCorrelationFilter.HEADER_NAME);
        assertNotEquals("contains spaces", generatedRequestId);
        assertEquals(generatedRequestId, UUID.fromString(generatedRequestId).toString());
        assertNull(MDC.get(RequestCorrelationFilter.MDC_KEY));
    }

    @Test
    void masksOneTimeDownloadTokenInLogPath() {
        assertEquals(
                "/api/downloads/{token}",
                RequestCorrelationFilter.safePath("/api/downloads/secret-one-time-token")
        );
        assertEquals("/api/orders/42", RequestCorrelationFilter.safePath("/api/orders/42"));
    }
}
