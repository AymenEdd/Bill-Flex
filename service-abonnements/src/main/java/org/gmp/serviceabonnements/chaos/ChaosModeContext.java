package org.gmp.serviceabonnements.chaos;

/**
 * Holds the active Chaos-Mode for the current request thread.
 * Populated by {@link ChaosModeFilter} from the {@code Chaos-Mode} request header
 * and consumed by {@link org.gmp.serviceabonnements.client.ClientServiceGateway}
 * to simulate circuit-breaker / retry / timeout failures.
 */
public final class ChaosModeContext {

    private static final ThreadLocal<String> CONTEXT = ThreadLocal.withInitial(() -> null);

    private ChaosModeContext() {
    }

    public static void set(String mode) {
        CONTEXT.set(mode);
    }

    public static String get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
