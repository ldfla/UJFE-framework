package ujfe.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

final class ConvertCommand {
    int run(String[] args) {
        Options options = Options.parse(args);
        if (!"html".equals(options.type())) {
            throw new IllegalArgumentException("Unsupported convert type: " + options.type() + ". Supported: html");
        }

        try {
            String html = Files.readString(options.input(), StandardCharsets.UTF_8);
            HtmlParseResult parseResult = new HtmlParser().parse(html);
            String java = new UjfeJavaGenerator().generate(parseResult, options.output());

            Path parent = options.output()
                .getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(options.output(), java, StandardCharsets.UTF_8);

            System.out.println("Converted " + options.input() + " -> " + options.output());
            parseResult.warnings()
                .forEach(warning -> System.out.println("Warning: " + warning));
            return 0;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not convert HTML", exception);
        }
    }

    private static final class Options {
        private final Path input;
        private final Path output;
        private final String type;

        private Options(Path input, Path output, String type) {
            this.input = Objects.requireNonNull(input, "input");
            this.output = Objects.requireNonNull(output, "output");
            this.type = Objects.requireNonNull(type, "type");
        }

        static Options parse(String[] args) {
            if (args.length == 0) {
                throw usage();
            }

            Path input = null;
            Path output = null;
            String type = "html";

            for (int index = 0; index < args.length; index++) {
                String current = args[index];
                switch (current) {
                    case "--out":
                        output = Path.of(requireValue(args, ++index, "--out"));
                        break;
                    case "--type":
                        type = requireValue(args, ++index, "--type").toLowerCase(Locale.ROOT);
                        break;
                    case "--help":
                    case "-h":
                        throw usage();
                    default:
                        if (current.startsWith("--")) {
                            throw new IllegalArgumentException("Unknown option: " + current);
                        }
                        if (input != null) {
                            throw new IllegalArgumentException("Only one input file is supported: " + Arrays.toString(args));
                        }
                        input = Path.of(current);
                        break;
                }
            }

            if (input == null || output == null) {
                throw usage();
            }

            return new Options(input, output, type);
        }

        Path input() {
            return input;
        }

        Path output() {
            return output;
        }

        String type() {
            return type;
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length || args[index].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }

        private static IllegalArgumentException usage() {
            return new IllegalArgumentException("Usage: ujfe convert page.html --out src/main/java/app/pages/Page.java --type html");
        }
    }
}
