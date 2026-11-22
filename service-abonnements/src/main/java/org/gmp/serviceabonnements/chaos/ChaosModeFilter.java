package org.gmp.serviceabonnements.chaos;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that activates the Chaos-Mode for a single request.
 *
 * <ul>
 *   <li>{@code Chaos-Mode: fail}     → returns 503 immediately (simulates CB open)</li>
 *   <li>{@code Chaos-Mode: delay}    → gateway returns a slow future (2s, within the 3s TimeLimiter)</li>
 *   <li>{@code Chaos-Mode: timeout}  → gateway returns a slow future (5s, triggers TimeLimiter → 503)</li>
 * </ul>
 *
 * The mode is stored in {@link ChaosModeContext} (ThreadLocal) and cleared after the request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ChaosModeFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String mode = request.getHeader("Chaos-Mode");
        if (mode != null && !mode.isBlank()) {
            ChaosModeContext.set(mode.trim());
        }
        try {
            if ("fail".equalsIgnoreCase(mode)) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"message\":\"Chaos: client service unavailable (circuit breaker open)\",\"status\":503}"
                );
                return;
            }
            filterChain.doFilter(request, response);
        } finally {
            ChaosModeContext.clear();
        }
    }
}
