package orange.wz.cli;

import orange.wz.provider.WzAESConstant;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Minimal CLI key alias resolver for common MapleStory WZ AES IV presets.
 */
public final class WzCliKeys {
    private static final Map<String, KeySpec> ALIASES = new LinkedHashMap<>();

    static {
        register("gms", "GMS", WzAESConstant.WZ_GMS_IV, WzAESConstant.DEFAULT_KEY);
        register("cms", "CMS", WzAESConstant.WZ_CMS_IV, WzAESConstant.DEFAULT_KEY);
        register("latest", "LATEST", WzAESConstant.WZ_LATEST_IV, WzAESConstant.DEFAULT_KEY);
        register("empty", "EMPTY", WzAESConstant.WZ_EMPTY_IV, WzAESConstant.DEFAULT_KEY);
    }

    private WzCliKeys() {
    }

    public static KeySpec resolve(String alias) {
        if (alias == null || alias.isBlank()) {
            alias = "gms";
        }
        KeySpec spec = ALIASES.get(alias.toLowerCase(Locale.ROOT));
        if (spec == null) {
            throw new IllegalArgumentException("Unknown key alias: " + alias + ". Available: " + String.join(", ", aliases()));
        }
        return spec.copy();
    }

    public static Set<String> aliases() {
        return ALIASES.keySet();
    }

    private static void register(String alias, String keyBoxName, byte[] iv, byte[] key) {
        ALIASES.put(alias, new KeySpec(alias, keyBoxName, iv, key));
    }

    public record KeySpec(String alias, String keyBoxName, byte[] iv, byte[] key) {
        public KeySpec {
            iv = Arrays.copyOf(iv, iv.length);
            key = Arrays.copyOf(key, key.length);
        }

        public KeySpec copy() {
            return new KeySpec(alias, keyBoxName, iv, key);
        }
    }
}
