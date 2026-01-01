package com.example.sunxu_mall;

import com.example.sunxu_mall.filter.TraceIdFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

class TraceIdFilterTest {

    @Test
    void testTraceIdGeneration() throws ServletException, IOException {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                // Verify traceId exists in MDC during chain execution
                String traceId = MDC.get("traceId");
                Assertions.assertNotNull(traceId, "TraceId should be in MDC");
                System.out.println("TraceId inside chain: " + traceId);
            }
        };

        filter.doFilter(request, response, chain);
        
        // Verify traceId is removed after chain execution
        Assertions.assertNull(MDC.get("traceId"), "MDC should be cleared");
    }

    @Test
    void testTraceIdFromHeader() throws ServletException, IOException {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        String expectedTraceId = "123456789";
        request.addHeader("X-Trace-Id", expectedTraceId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                Assertions.assertEquals(expectedTraceId, MDC.get("traceId"));
                System.out.println("TraceId from header: " + MDC.get("traceId"));
            }
        };

        filter.doFilter(request, response, chain);
        Assertions.assertNull(MDC.get("traceId"));
    }
}
