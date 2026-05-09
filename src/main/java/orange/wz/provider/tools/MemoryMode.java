package orange.wz.provider.tools;

import java.util.Locale;

/**
 * CLI/provider memory behavior for batch export workflows.
 */
public enum MemoryMode {
    NORMAL,
    LOW;

    public static MemoryMode fromCliValue(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "normal" -> NORMAL;
            case "low" -> LOW;
            default -> throw new IllegalArgumentException("Invalid --memory-mode value: " + value + " (expected normal, low)");
        };
    }
}
