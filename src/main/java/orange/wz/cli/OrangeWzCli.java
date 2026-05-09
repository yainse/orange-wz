package orange.wz.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import orange.wz.model.Pair;
import orange.wz.provider.WzDirectory;
import orange.wz.provider.WzFile;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageFile;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.WzXmlFile;
import orange.wz.provider.tools.MediaExportType;
import orange.wz.provider.tools.MemoryMode;
import orange.wz.provider.tools.WzMemoryReclaimer;
import orange.wz.provider.tools.XmlExportVersion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Headless Linux-friendly CLI entry point. Does not start Spring or Swing.
 */
public final class OrangeWzCli {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private OrangeWzCli() {
    }

    public static void main(String[] args) {
        int code = new OrangeWzCliRunner(args).run();
        if (code != 0) {
            System.exit(code);
        }
    }

    private static final class OrangeWzCliRunner {
        private final List<String> positional = new ArrayList<>();
        private final Map<String, String> options = new LinkedHashMap<>();
        private boolean help;
        private boolean force;

        private OrangeWzCliRunner(String[] args) {
            parseArgs(args);
        }

        private int run() {
            try {
                if (help || positional.isEmpty()) {
                    printHelp();
                    return 0;
                }

                String command = positional.getFirst().toLowerCase(Locale.ROOT);
                return switch (command) {
                    case "keys" -> keys();
                    case "info" -> info();
                    case "img-to-xml" -> imgToXml();
                    case "xml-to-img" -> xmlToImg();
                    case "wz-to-xml" -> wzToXml();
                    default -> fail("Unknown command: " + command + "\nRun with --help for usage.");
                };
            } catch (IllegalArgumentException e) {
                return fail(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace(System.err);
                return 1;
            }
        }

        private void parseArgs(String[] args) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--help", "-h" -> help = true;
                    case "--force" -> force = true;
                    case "--key", "-k", "--output", "-o", "--indent", "--media", "--xml-version", "--memory-mode" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("Missing value for option: " + arg);
                        }
                        options.put(normalizeOption(arg), args[++i]);
                    }
                    default -> {
                        if (arg.startsWith("-")) {
                            throw new IllegalArgumentException("Unknown option: " + arg);
                        }
                        positional.add(arg);
                    }
                }
            }
        }

        private String normalizeOption(String option) {
            return switch (option) {
                case "--key", "-k" -> "key";
                case "--output", "-o" -> "output";
                case "--indent" -> "indent";
                case "--media" -> "media";
                case "--xml-version" -> "xml-version";
                case "--memory-mode" -> "memory-mode";
                default -> option.replaceFirst("^-+", "");
            };
        }

        private int keys() {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("aliases", WzCliKeys.aliases());
            json.put("default", "gms");
            System.out.println(GSON.toJson(json));
            return 0;
        }

        private int info() {
            Path input = requireInput("info <path>");
            WzCliKeys.KeySpec key = keySpec();
            Map<String, Object> json = baseInfo(input);
            String type = detectType(input);
            json.put("type", type);
            json.put("keyAlias", key.alias());

            if ("img".equals(type)) {
                WzImageFile img = new WzImageFile(input.getFileName().toString(), input.toString(), key.keyBoxName(), key.iv(), key.key());
                boolean parsed = img.parse();
                json.put("parsed", parsed);
                json.put("status", String.valueOf(img.getStatus()));
                json.put("children", parsed ? img.getChildren().size() : 0);
                json.put("propertyCount", parsed ? countImageProperties(img) : 0);
            } else if ("wz".equals(type)) {
                WzFile wz = new WzFile(input.toString(), (short) -1, key.keyBoxName(), key.iv(), key.key());
                boolean parsed = wz.parse();
                json.put("parsed", parsed);
                json.put("status", String.valueOf(wz.getStatus()));
                WzDirectory root = wz.getWzDirectory();
                json.put("children", parsed ? root.getChildren().size() : 0);
                json.put("directoryCount", parsed ? countDirectories(root) : 0);
                json.put("imageCount", parsed ? countImages(root) : 0);
            } else {
                json.put("parsed", false);
                json.put("status", "UNSUPPORTED_TYPE");
            }

            System.out.println(GSON.toJson(json));
            return 0;
        }

        private int imgToXml() throws Exception {
            Path input = requireInput("img-to-xml <input.img>");
            Path output = requireOutput();
            requireType(input, "img");
            WzCliKeys.KeySpec key = keySpec();
            ensureParent(output);

            WzImageFile img = new WzImageFile(input.getFileName().toString(), input.toString(), key.keyBoxName(), key.iv(), key.key());
            if (!img.parse()) {
                return fail("Failed to parse img: " + input + " status=" + img.getStatus());
            }
            if (!img.exportToXml(output, indent(), mediaExportType(), true, xmlExportVersion())) {
                return fail("Failed to export xml: " + output);
            }
            System.out.println("Exported " + input + " -> " + output);
            return 0;
        }

        private int xmlToImg() throws Exception {
            Path input = requireInput("xml-to-img <input.xml>");
            Path output = requireOutput();
            requireType(input, "xml");
            WzCliKeys.KeySpec key = keySpec();
            ensureNotExists(output);
            ensureParent(output);

            WzXmlFile xml = new WzXmlFile(input.getFileName().toString(), input.toString(), key.keyBoxName(), key.iv(), key.key());
            if (!xml.parse()) {
                return fail("Failed to parse xml: " + input);
            }
            if (!xml.saveFromXml(output)) {
                return fail("Failed to save img: " + output);
            }
            System.out.println("Saved " + input + " -> " + output);
            return 0;
        }

        private int wzToXml() throws Exception {
            Path input = requireInput("wz-to-xml <input.wz>");
            Path output = requireOutput();
            requireType(input, "wz");
            WzCliKeys.KeySpec key = keySpec();
            Files.createDirectories(output);

            WzFile wz = new WzFile(input.toString(), (short) -1, key.keyBoxName(), key.iv(), key.key());
            if (!wz.parse()) {
                return fail("Failed to parse wz: " + input + " status=" + wz.getStatus());
            }
            List<Pair<WzImage, Path>> images = new ArrayList<>();
            MemoryMode memoryMode = memoryMode();
            wz.exportFileToXml(output, images);
            int exported = 0;
            for (Pair<WzImage, Path> pair : images) {
                ensureParent(pair.right);
                if (!pair.left.exportToXml(pair.right, indent(), mediaExportType(), true, xmlExportVersion())) {
                    return fail("Failed to export image xml: " + pair.right);
                }
                exported++;
                if (memoryMode == MemoryMode.LOW) {
                    WzMemoryReclaimer.discardDecodedImages(pair.left);
                    if (pair.left.getReader() != null && !pair.left.isChanged()) {
                        pair.left.unparse();
                    }
                }
            }
            System.out.println("Exported " + exported + " image xml file(s) from " + input + " into " + output);
            return 0;
        }

        private WzCliKeys.KeySpec keySpec() {
            return WzCliKeys.resolve(options.getOrDefault("key", "gms"));
        }

        private Path requireInput(String usage) {
            if (positional.size() < 2) {
                throw new IllegalArgumentException("Missing input. Usage: " + usage);
            }
            Path input = Path.of(positional.get(1));
            if (!Files.isRegularFile(input)) {
                throw new IllegalArgumentException("Input file does not exist: " + input);
            }
            return input;
        }

        private Path requireOutput() {
            String output = options.get("output");
            if (output == null || output.isBlank()) {
                throw new IllegalArgumentException("Missing output option: -o <path>");
            }
            return Path.of(output);
        }

        private void ensureNotExists(Path output) {
            if (!force && Files.exists(output)) {
                throw new IllegalArgumentException("Output exists (use --force to overwrite): " + output);
            }
        }

        private void ensureParent(Path output) throws Exception {
            Path parent = output.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        }

        private int indent() {
            String value = options.getOrDefault("indent", "2");
            try {
                int indent = Integer.parseInt(value);
                if (indent < 0 || indent > 8) {
                    throw new NumberFormatException("out of range");
                }
                return indent;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid --indent value: " + value + " (expected 0..8)");
            }
        }

        private MediaExportType mediaExportType() {
            String value = options.getOrDefault("media", "none").toUpperCase(Locale.ROOT);
            try {
                return MediaExportType.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid --media value: " + value.toLowerCase(Locale.ROOT) + " (expected none, base64, file)");
            }
        }

        private XmlExportVersion xmlExportVersion() {
            return XmlExportVersion.fromCliValue(options.getOrDefault("xml-version", "default"));
        }

        private MemoryMode memoryMode() {
            return MemoryMode.fromCliValue(options.getOrDefault("memory-mode", "normal"));
        }

        private String detectType(Path input) {
            String name = input.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(".img")) return "img";
            if (name.endsWith(".wz")) return "wz";
            if (name.endsWith(".xml")) return "xml";
            return "unknown";
        }

        private void requireType(Path input, String expected) {
            String actual = detectType(input);
            if (!expected.equals(actual)) {
                throw new IllegalArgumentException("Expected ." + expected + " input, got: " + input);
            }
        }

        private Map<String, Object> baseInfo(Path input) {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("path", input.toAbsolutePath().normalize().toString());
            json.put("name", input.getFileName().toString());
            return json;
        }

        private int countDirectories(WzDirectory directory) {
            int count = directory.getDirectories().size();
            for (WzDirectory child : directory.getDirectories()) {
                count += countDirectories(child);
            }
            return count;
        }

        private int countImages(WzDirectory directory) {
            int count = directory.getImages().size();
            for (WzDirectory child : directory.getDirectories()) {
                count += countImages(child);
            }
            return count;
        }

        private int countImageProperties(WzImage image) {
            int count = 0;
            for (WzImageProperty child : image.getChildren()) {
                count += countProperty(child);
            }
            return count;
        }

        private int countProperty(WzImageProperty property) {
            int count = 1;
            List<WzImageProperty> children = property.getChildren();
            if (children != null) {
                for (WzImageProperty child : children) {
                    count += countProperty(child);
                }
            }
            return count;
        }

        private int fail(String message) {
            System.err.println(message);
            return 1;
        }
    }

    private static void printHelp() {
        System.out.println("""
                OrangeWz headless CLI

                Usage:
                  java -jar target/OrzRepacker-cli.jar --help
                  java -jar target/OrzRepacker-cli.jar keys
                  java -jar target/OrzRepacker-cli.jar info <path.img|path.wz> --key gms
                  java -jar target/OrzRepacker-cli.jar img-to-xml <input.img> -o <output.xml> --key gms --indent 2 --media none [--xml-version default]
                  java -jar target/OrzRepacker-cli.jar xml-to-img <input.xml> -o <output.img> --key gms [--force]
                  java -jar target/OrzRepacker-cli.jar wz-to-xml <input.wz> -o <output-dir> --key gms --indent 2 --media none [--xml-version default] [--memory-mode normal]

                Commands:
                  keys        Print supported key aliases as JSON.
                  info        Parse .img or .wz and print JSON summary.
                  img-to-xml  Export one .img file to XML with Linux LF line endings.
                  xml-to-img  Convert XML back to .img; refuses to overwrite unless --force is set.
                  wz-to-xml   Export every image in a .wz package to XML files.

                Options:
                  --key, -k   Key alias: gms, cms, latest, empty. Default: gms.
                  -o          Output file or directory.
                  --indent    XML indentation, 0..8. Default: 2.
                  --media     XML media mode: none, base64, file. Default: none.
                  --xml-version XML export version: default, v125. Default: default.
                  --memory-mode Memory mode for wz-to-xml: normal, low. Default: normal.
                  --force     Allow xml-to-img output overwrite.
                """);
    }
}
