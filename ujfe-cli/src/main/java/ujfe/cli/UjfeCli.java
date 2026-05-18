package ujfe.cli;

public final class UjfeCli {
    private UjfeCli() {
    }

    public static void main(String[] args) {
        try {
            if (args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
                printUsage();
                return;
            }

            if ("convert".equals(args[0])) {
                int exitCode = new ConvertCommand().run(java.util.Arrays.copyOfRange(args, 1, args.length));
                if (exitCode != 0) {
                    System.exit(exitCode);
                }
                return;
            }

            throw new IllegalArgumentException("Unknown command: " + args[0]);
        } catch (RuntimeException exception) {
            System.err.println(exception.getMessage());
            if (java.util.Arrays.asList(args)
                .contains("--debug")) {
                exception.printStackTrace(System.err);
            }
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  ujfe convert page.html --output src/main/java/app/pages/Page.java --type html");
        System.out.println("  ujfe convert page.html --output GeneratedPage.java --class-name GeneratedPage --comments drop");
        System.out.println();
        System.out.println("Future commands: ujfe new, ujfe dev, ujfe build.");
    }
}
