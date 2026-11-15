
import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.tools.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class LocPerClassGenerator {

    static class Counter {
        static int count(String source) {
            int count = 0;
            boolean code = false;
            int state = 0; // 0=normal, 1=block comment,
                           // 2=string, 3=character

            for (int i = 0; i < source.length(); i++) {
                char c = source.charAt(i);
                char next = i + 1 < source.length()
                        ? source.charAt(i + 1) : '\0';

                if (state == 1) {
                    if (c == '*' && next == '/') {
                        state = 0;
                        i++;
                    }
                    if (c == '\n' && code) {
                        count++;
                        code = false;
                    }
                    continue;
                }

                if (state == 2 || state == 3) {
                    code = true;

                    if (c == '\\') {
                        i++;
                    } else if ((state == 2 && c == '"')
                            || (state == 3 && c == '\'')) {
                        state = 0;
                    }

                    if (c == '\n') {
                        if (code) count++;
                        code = false;
                    }
                    continue;
                }

                if (c == '/' && next == '*') {
                    state = 1;
                    i++;
                } else if (c == '/' && next == '/') {
                    while (i < source.length()
                            && source.charAt(i) != '\n') {
                        i++;
                    }
                    if (code) count++;
                    code = false;
                } else if (c == '"') {
                    state = 2;
                    code = true;
                } else if (c == '\'') {
                    state = 3;
                    code = true;
                } else if (c == '\n') {
                    if (code) count++;
                    code = false;
                } else if (!Character.isWhitespace(c)) {
                    code = true;
                }
            }

            if (code) count++;
            return count;
        }
    }

    static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println(
                "Usage: java LocPerClassGenerator <source-root> <output-csv>");
            return;
        }

        Path root = Paths.get(args[0]).toAbsolutePath().normalize();
        Path output = Paths.get(args[1]).toAbsolutePath().normalize();

        JavaCompiler compiler =
                ToolProvider.getSystemJavaCompiler();

        if (compiler == null) {
            throw new IllegalStateException(
                "JDK compiler not available. Use a JDK, not a JRE.");
        }

        List<File> files = new ArrayList<File>();

        try (java.util.stream.Stream<Path> paths =
                     Files.walk(root)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                 .forEach(p -> files.add(p.toFile()));
        }

        Collections.sort(files, new Comparator<File>() {
            public int compare(File a, File b) {
                return a.getPath().compareTo(b.getPath());
            }
        });

        DiagnosticCollector<JavaFileObject> diagnostics =
                new DiagnosticCollector<JavaFileObject>();

        StandardJavaFileManager manager =
                compiler.getStandardFileManager(
                    diagnostics, null, StandardCharsets.UTF_8);

        Iterable<? extends JavaFileObject> units =
                manager.getJavaFileObjectsFromFiles(files);

        JavacTask task = (JavacTask) compiler.getTask(
                null, manager, diagnostics,
                Arrays.asList("-proc:none"), null, units);

        Trees trees = Trees.instance(task);
        int rows = 0;

        try (BufferedWriter writer = Files.newBufferedWriter(
                output, StandardCharsets.UTF_8)) {

            writer.write(
                "FilePath,fullyQualifiedName,loc,UnnamedPackage");
            writer.newLine();

            for (CompilationUnitTree unit : task.parse()) {
                String packageName = unit.getPackageName() == null
                        ? ""
                        : unit.getPackageName().toString();

                boolean unnamed = packageName.isEmpty();

                String filePath = Paths.get(
                        unit.getSourceFile().toUri())
                        .toAbsolutePath().normalize()
                        .toString();

                String relativePath;
                try {
                    relativePath = root.relativize(
                            Paths.get(filePath)).toString();
                } catch (Exception ex) {
                    relativePath = filePath;
                }

                SourcePositions positions =
                        trees.getSourcePositions();

                String source = new String(
                        Files.readAllBytes(Paths.get(filePath)),
                        StandardCharsets.UTF_8);

                for (Tree declaration : unit.getTypeDecls()) {
                    if (!(declaration instanceof ClassTree)) {
                        continue;
                    }

                    ClassTree type = (ClassTree) declaration;

                    long start = positions.getStartPosition(
                            unit, declaration);
                    long end = positions.getEndPosition(
                            unit, declaration);

                    if (start < 0 || end < start
                            || end > source.length()) {
                        System.err.println(
                            "Skipped declaration with invalid positions: "
                            + filePath);
                        continue;
                    }

                    String simpleName =
                            type.getSimpleName().toString();

                    if (simpleName.isEmpty()) continue;

                    String qualifiedName = unnamed
                            ? simpleName
                            : packageName + "." + simpleName;

                    String declarationSource =
                            source.substring((int) start, (int) end);

                    int loc = Counter.count(declarationSource);

                    writer.write(
                        csv(relativePath) + ","
                        + csv(qualifiedName) + ","
                        + loc + ","
                        + unnamed);
                    writer.newLine();

                    rows++;
                }
            }
        } finally {
            manager.close();
        }

        System.out.println("Java source files found: " + files.size());
        System.out.println("Top-level type records: " + rows);
        System.out.println("CSV saved: " + output);
    }
}
