package ujfe.router.source;

import ujfe.router.Page;
import ujfe.router.PageClassValidator;
import ujfe.router.RouteDefinition;
import ujfe.router.RouteDiscoveryException;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ReflectionPageScanner implements RouteSource {
    private final List<Class<?>> candidates;
    private final List<String> packageNames;
    private final ClassLoader classLoader;

    public ReflectionPageScanner(Class<?>... candidates) {
        this(
            copyCandidates(candidates),
            List.of(),
            Thread.currentThread()
                .getContextClassLoader()
        );
    }

    private ReflectionPageScanner(List<Class<?>> candidates, List<String> packageNames, ClassLoader classLoader) {
        this.candidates = candidates;
        this.packageNames = packageNames;
        this.classLoader = classLoader == null ? ReflectionPageScanner.class.getClassLoader() : classLoader;
    }

    public static ReflectionPageScanner forPackages(String... packageNames) {
        Objects.requireNonNull(packageNames, "packageNames");
        List<String> packages = new ArrayList<>();
        for (String packageName : packageNames) {
            Objects.requireNonNull(packageName, "packageName");
            if (packageName.isBlank()) {
                throw new IllegalArgumentException("Package name cannot be blank");
            }
            packages.add(packageName);
        }
        return new ReflectionPageScanner(
            List.of(),
            List.copyOf(packages),
            Thread.currentThread()
                .getContextClassLoader()
        );
    }

    @Override
    public Collection<RouteDefinition> routes() {
        List<Class<?>> pageTypes = discoverPageTypes();
        Map<String, Class<?>> seenPaths = new LinkedHashMap<>();
        List<RouteDefinition> discovered = new ArrayList<>();

        for (Class<?> pageType : pageTypes) {
            if (pageType.isInterface()) {
                continue;
            }
            Page page = pageType.getAnnotation(Page.class);
            if (page == null) {
                continue;
            }

            PageClassValidator.validateAnnotatedPageClass(pageType);
            RouteDefinition route = RouteDefinition.pageClass(page.value(), pageType);
            Class<?> previous = seenPaths.putIfAbsent(route.path(), pageType);
            if (previous != null) {
                throw new RouteDiscoveryException("Duplicate route path '" + route.path()
                    + "' for " + previous.getName()
                    + " and " + pageType.getName());
            }
            discovered.add(route);
        }

        return List.copyOf(discovered);
    }

    private List<Class<?>> discoverPageTypes() {
        List<Class<?>> pageTypes = new ArrayList<>(candidates);
        for (String packageName : packageNames) {
            pageTypes.addAll(scanPackage(packageName));
        }
        pageTypes.sort(Comparator.comparing(Class::getName));
        return List.copyOf(pageTypes);
    }

    private List<Class<?>> scanPackage(String packageName) {
        String resourcePath = packageName.replace('.', '/');
        List<Class<?>> classes = new ArrayList<>();
        try {
            Enumeration<URL> resources = classLoader.getResources(resourcePath);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    classes.addAll(scanFileResource(packageName, resource));
                } else if ("jar".equals(resource.getProtocol())) {
                    classes.addAll(scanJarResource(resourcePath, resource));
                }
            }
        } catch (IOException exception) {
            throw new RouteDiscoveryException("Could not scan package " + packageName, exception);
        }
        return classes;
    }

    private List<Class<?>> scanFileResource(String packageName, URL resource) {
        try {
            Path root = Path.of(resource.toURI());
            List<Class<?>> classes = new ArrayList<>();
            try (var stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName()
                        .toString()
                        .endsWith(".class"))
                    .map(path -> toClassName(packageName, root, path))
                    .filter(ReflectionPageScanner::isTopLevelApplicationClass)
                    .map(this::loadClass)
                    .forEach(classes::add);
            }
            return classes;
        } catch (IOException | URISyntaxException exception) {
            throw new RouteDiscoveryException("Could not scan classpath resource " + resource, exception);
        }
    }

    private List<Class<?>> scanJarResource(String resourcePath, URL resource) {
        try {
            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            List<Class<?>> classes = new ArrayList<>();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (entry.isDirectory()
                        || !name.startsWith(resourcePath)
                        || !name.endsWith(".class")) {
                        continue;
                    }
                    String className = name.substring(0, name.length() - ".class".length())
                        .replace('/', '.');
                    if (isTopLevelApplicationClass(className)) {
                        classes.add(loadClass(className));
                    }
                }
            }
            return classes;
        } catch (IOException exception) {
            throw new RouteDiscoveryException("Could not scan jar resource " + resource, exception);
        }
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException exception) {
            throw new RouteDiscoveryException("Could not load route candidate " + className, exception);
        }
    }

    private static String toClassName(String packageName, Path root, Path classFile) {
        Path relative = root.relativize(classFile);
        String nestedName = relative.toString()
            .replace('\\', '.')
            .replace('/', '.');
        String suffix = nestedName.substring(0, nestedName.length() - ".class".length());
        return packageName + "." + suffix;
    }

    private static boolean isTopLevelApplicationClass(String className) {
        return !className.contains("$") && !className.endsWith(".module-info");
    }

    private static List<Class<?>> copyCandidates(Class<?>... candidates) {
        Objects.requireNonNull(candidates, "candidates");
        Arrays.stream(candidates)
            .forEach(candidate -> Objects.requireNonNull(candidate, "candidate"));
        return List.copyOf(Arrays.asList(candidates));
    }
}
