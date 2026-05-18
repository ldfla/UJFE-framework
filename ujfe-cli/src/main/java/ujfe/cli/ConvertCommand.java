package ujfe.cli;

import java.io.IOException;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

final class ConvertCommand {
    int run(String[] args) {
        ConversionOptions options = Options.parse(args);
        if (!"html".equals(options.type())) {
            throw new HtmlConversionException("Unsupported convert type: " + options.type() + ". Supported: html");
        }

        validateInput(options.input());
        validateOutput(options.output());

        String html = readInput(options);
        HtmlParseResult parseResult = new HtmlParser().parse(
            html,
            options.commentPolicy(),
            options.unsafeFallbackEnabled()
        );
        GeneratedJava generated = new UjfeJavaGenerator().generate(parseResult, options);
        writeOutput(options.output(), generated.pageJava(), options.encoding());
        if (generated.hasCssJava()) {
            writeOutput(generated.cssOutput(), generated.cssJava(), StandardCharsets.UTF_8);
        }

        printSummary(options, generated);
        return 0;
    }

    private static String readInput(ConversionOptions options) {
        try {
            byte[] bytes = Files.readAllBytes(options.input());
            if (bytes.length == 0) {
                throw new HtmlConversionException("Input file is empty: " + options.input());
            }
            return options.encoding()
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(bytes))
                .toString();
        } catch (MalformedInputException exception) {
            throw new HtmlConversionException("Input file cannot be decoded with " + options.encoding()
                .name() + ": " + options.input(), exception);
        } catch (CharacterCodingException exception) {
            throw new HtmlConversionException("Input file has invalid characters for " + options.encoding()
                .name() + ": " + options.input(), exception);
        } catch (IOException exception) {
            throw new HtmlConversionException("Could not read input file: " + options.input(), exception);
        }
    }

    private static void validateInput(Path input) {
        if (!Files.exists(input)) {
            throw new HtmlConversionException("Input file not found: " + input);
        }
        if (Files.isDirectory(input)) {
            throw new HtmlConversionException("Input path is a directory, expected an HTML file: " + input);
        }
        if (!Files.isRegularFile(input)) {
            throw new HtmlConversionException("Input path is not a regular file: " + input);
        }
        if (!Files.isReadable(input)) {
            throw new HtmlConversionException("Input file is not readable: " + input);
        }
    }

    private static void validateOutput(Path output) {
        if (Files.isDirectory(output)) {
            throw new HtmlConversionException("Output path is a directory, expected a Java file: " + output);
        }
        Path parent = output.toAbsolutePath()
            .normalize()
            .getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException exception) {
                throw new HtmlConversionException("Could not create output directory: " + parent, exception);
            }
            if (!Files.isWritable(parent)) {
                throw new HtmlConversionException("Output directory is not writable: " + parent);
            }
        }
    }

    private static void writeOutput(Path output, String content, Charset encoding) {
        try {
            Files.writeString(output, content, encoding);
        } catch (IOException exception) {
            throw new HtmlConversionException("Could not write output file: " + output, exception);
        }
    }

    private static void printSummary(ConversionOptions options, GeneratedJava generated) {
        ConversionStats stats = generated.stats();
        System.out.println("Converted " + options.input() + " -> " + options.output());
        System.out.println("Input: " + options.input());
        System.out.println("Output: " + options.output());
        System.out.println("Generated class: " + options.className());
        if (!options.packageName()
            .isBlank()) {
            System.out.println("Package: " + options.packageName());
        }
        System.out.println("Converted elements: " + stats.convertedElements());
        System.out.println("Preserved attributes: " + stats.preservedAttributes());
        System.out.println("Warnings: " + generated.warnings()
            .size());
        System.out.println("Unsafe fallback used: " + (stats.unsafeFallbacks() > 0));
        System.out.println("Comments: " + options.commentPolicy()
            .name()
            .toLowerCase(Locale.ROOT)
            .replace('_', '-'));
        System.out.println("CSS migration: " + cssSummary(generated, options));
        System.out.println("Component methods: " + stats.componentMethods());
        generated.warnings()
            .forEach(warning -> System.out.println("Warning: " + warning));
    }

    private static String cssSummary(GeneratedJava generated, ConversionOptions options) {
        if (options.cssMigrationMode() != CssMigrationMode.EXTRACT) {
            return options.cssMigrationMode()
                .name()
                .toLowerCase(Locale.ROOT);
        }
        if (!generated.hasCssJava()) {
            return "extract enabled, no CSS extracted";
        }
        return "extracted " + generated.stats()
            .cssBlocks() + " block(s) -> " + generated.cssOutput();
    }

    private static final class Options {
        private Options() {
        }

        static ConversionOptions parse(String[] args) {
            if (args.length == 0) {
                throw usage();
            }

            Path input = null;
            Path output = null;
            String type = "html";
            String className = null;
            String packageName = null;
            Charset encoding = StandardCharsets.UTF_8;
            CommentPolicy commentPolicy = CommentPolicy.DROP;
            boolean unsafeFallback = false;
            CssMigrationMode cssMode = CssMigrationMode.NONE;
            String cssClassName = "GeneratedStyles";
            boolean componentize = false;
            boolean safeClassName = false;
            boolean debug = false;

            for (int index = 0; index < args.length; index++) {
                String current = args[index];
                switch (current) {
                    case "--out":
                    case "--output":
                        output = Path.of(requireValue(args, ++index, current));
                        break;
                    case "--type":
                        type = requireValue(args, ++index, "--type").toLowerCase(Locale.ROOT);
                        break;
                    case "--class-name":
                        className = requireValue(args, ++index, "--class-name");
                        break;
                    case "--package":
                        packageName = requireValue(args, ++index, "--package");
                        break;
                    case "--comments":
                        commentPolicy = commentPolicy(requireValue(args, ++index, "--comments"));
                        break;
                    case "--unsafe-fallback":
                        unsafeFallback = true;
                        break;
                    case "--css":
                        cssMode = cssMode(requireValue(args, ++index, "--css"));
                        break;
                    case "--css-class-name":
                        cssClassName = requireValue(args, ++index, "--css-class-name");
                        break;
                    case "--componentize":
                        componentize = true;
                        break;
                    case "--encoding":
                        encoding = charset(requireValue(args, ++index, "--encoding"));
                        break;
                    case "--fail-on-unsupported":
                    case "--format":
                        break;
                    case "--safe-class-name":
                        safeClassName = true;
                        break;
                    case "--debug":
                        debug = true;
                        break;
                    case "--help":
                    case "-h":
                        throw usage();
                    default:
                        if (current.startsWith("--")) {
                            throw new HtmlConversionException("Unknown option: " + current);
                        }
                        if (input != null) {
                            throw new HtmlConversionException("Only one input file is supported: " + Arrays.toString(args));
                        }
                        input = Path.of(current);
                        break;
                }
            }

            if (input == null || output == null) {
                throw usage();
            }
            return ConversionOptions.builder(input, output)
                .type(type)
                .className(className)
                .packageName(packageName)
                .encoding(encoding)
                .commentPolicy(commentPolicy)
                .unsafeFallbackEnabled(unsafeFallback)
                .cssMigrationMode(cssMode)
                .cssClassName(cssClassName)
                .componentize(componentize)
                .safeClassName(safeClassName)
                .debug(debug)
                .build();
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length || args[index].startsWith("--")) {
                throw new HtmlConversionException("Missing value for " + option);
            }
            return args[index];
        }

        private static CommentPolicy commentPolicy(String value) {
            switch (value.toLowerCase(Locale.ROOT)) {
                case "drop":
                    return CommentPolicy.DROP;
                case "preserve":
                    return CommentPolicy.PRESERVE;
                case "unsafe-fallback":
                    return CommentPolicy.UNSAFE_FALLBACK;
                default:
                    throw new HtmlConversionException("Invalid comment policy: " + value
                        + ". Supported: preserve, drop, unsafe-fallback");
            }
        }

        private static CssMigrationMode cssMode(String value) {
            switch (value.toLowerCase(Locale.ROOT)) {
                case "none":
                    return CssMigrationMode.NONE;
                case "extract":
                    return CssMigrationMode.EXTRACT;
                case "external":
                    return CssMigrationMode.EXTERNAL;
                default:
                    throw new HtmlConversionException("Invalid CSS migration mode: " + value
                        + ". Supported: none, extract, external");
            }
        }

        private static Charset charset(String value) {
            try {
                return Charset.forName(value);
            } catch (IllegalCharsetNameException | UnsupportedCharsetException exception) {
                throw new HtmlConversionException("Unsupported encoding: " + value, exception);
            }
        }

        private static HtmlConversionException usage() {
            return new HtmlConversionException("Usage: ujfe convert page.html --output src/main/java/app/pages/Page.java --type html");
        }
    }
}
