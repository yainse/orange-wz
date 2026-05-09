package orange.wz.provider.tools;

import java.util.Locale;

/**
 * XML 导出格式版本。
 *
 * <p>该枚举位于 provider 层，避免 headless/CLI 导出能力依赖 GUI 表单类。</p>
 */
public enum XmlExportVersion {
    DEFAULT,
    V125;

    public static XmlExportVersion fromCliValue(String value) {
        String normalized = value == null ? "default" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "", "default" -> DEFAULT;
            case "v125", "125" -> V125;
            default -> throw new IllegalArgumentException(
                    "Invalid --xml-version value: " + value + " (expected default, v125)"
            );
        };
    }
}
