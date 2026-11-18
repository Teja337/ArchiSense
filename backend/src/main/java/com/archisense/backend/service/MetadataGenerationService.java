
package com.archisense.backend.service;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import org.springframework.stereotype.Service;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class MetadataGenerationService {

    /**
     * Existing single-module method.
     * Preserves compatibility with existing callers and tests.
     */
    public Path generateLocPerClass(
            Path sourceDirectory,
            Path outputFile) throws IOException {

        Path sourceRoot = sourceDirectory.toAbsolutePath().normalize();

        return generateLocPerClass(
                List.of(sourceRoot),
                sourceRoot,
                outputFile
        );
    }

    /**
     * Generates metadata from multiple Maven module source roots.
     *
     * FilePath is relative to projectRoot so module names remain
     * part of each source file path.
     */
    public Path generateLocPerClass(
            List<Path> sourceDirectories,
            Path projectRoot,
            Path outputFile) throws IOException {

        Path normalizedProjectRoot =
                projectRoot.toAbsolutePath().normalize();

        if (sourceDirectories == null || sourceDirectories.isEmpty()) {
            throw new IOException("No Java source directories were provided.");
        }

        List<ClassMetadata> records = new ArrayList<>();

        for (Path sourceDirectory : sourceDirectories) {

            Path sourceRoot =
                    sourceDirectory.toAbsolutePath().normalize();

            if (!Files.isDirectory(sourceRoot)) {
                throw new IOException(
                        "Source directory does not exist: " + sourceRoot);
            }

            if (!sourceRoot.startsWith(normalizedProjectRoot)) {
                throw new IOException(
                        "Source directory is outside the project: " + sourceRoot);
            }

            try (Stream<Path> files = Files.walk(sourceRoot)) {

                List<Path> javaFiles = files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .sorted()
                        .toList();

                for (Path javaFile : javaFiles) {
                    records.addAll(
                            readClassMetadata(
                                    normalizedProjectRoot,
                                    javaFile
                            )
                    );
                }
            }
        }

        Path parent = outputFile.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                outputFile, StandardCharsets.UTF_8)) {

            writer.write(
                    "FilePath,fullyQualifiedName,loc,UnnamedPackage");
            writer.newLine();

            for (ClassMetadata record : records) {

                writer.write(csvField(record.filePath()));
                writer.write(",");

                writer.write(csvField(record.fullyQualifiedName()));
                writer.write(",");

                writer.write(String.valueOf(record.loc()));
                writer.write(",");

                writer.write(String.valueOf(record.unnamedPackage()));

                writer.newLine();
            }
        }

        return outputFile;
    }

    private List<ClassMetadata> readClassMetadata(
            Path projectRoot,
            Path javaFile) throws IOException {

        JavaCompiler compiler =
                ToolProvider.getSystemJavaCompiler();

        if (compiler == null) {
            throw new IOException(
                    "Java compiler not available. Please use a JDK.");
        }

        DiagnosticCollector<JavaFileObject> diagnostics =
                new DiagnosticCollector<>();

        List<ClassMetadata> records = new ArrayList<>();

        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(
                             diagnostics,
                             Locale.ROOT,
                             StandardCharsets.UTF_8)) {

            Iterable<? extends JavaFileObject> sourceFiles =
                    fileManager.getJavaFileObjects(javaFile.toFile());

            JavacTask task = (JavacTask) compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    List.of("-proc:none"),
                    null,
                    sourceFiles
            );

            Trees trees = Trees.instance(task);

            for (CompilationUnitTree unit : task.parse()) {

                SourcePositions positions =
                        trees.getSourcePositions();

                String packageName = unit.getPackageName() == null
                        ? ""
                        : unit.getPackageName().toString();

                new TreePathScanner<Void, Void>() {

                    private final Deque<String> enclosingTypes =
                            new ArrayDeque<>();

                    @Override
                    public Void visitClass(
                            ClassTree node,
                            Void unused) {

                        String className =
                                node.getSimpleName().toString();

                        // Anonymous classes do not have a class name.
                        if (className.isEmpty()) {
                            return super.visitClass(node, unused);
                        }

                        enclosingTypes.addLast(className);

                        long startPosition =
                                positions.getStartPosition(unit, node);

                        long endPosition =
                                positions.getEndPosition(unit, node);

                        if (startPosition >= 0 && endPosition > startPosition) {

                            long startLine =
                                    unit.getLineMap().getLineNumber(
                                            startPosition);

                            long endLine =
                                    unit.getLineMap().getLineNumber(
                                            endPosition - 1);

                            int loc = (int) (endLine - startLine + 1);

                            String relativePath =
                                    projectRoot.relativize(javaFile)
                                            .toString()
                                            .replace('\\', '/');

                            String typeName =
                                    String.join(".", enclosingTypes);

                            String fullyQualifiedName =
                                    packageName.isEmpty()
                                            ? typeName
                                            : packageName + "." + typeName;

                            records.add(new ClassMetadata(
                                    relativePath,
                                    fullyQualifiedName,
                                    loc,
                                    packageName.isEmpty()
                            ));
                        }

                        super.visitClass(node, unused);

                        enclosingTypes.removeLast();

                        return null;
                    }

                }.scan(unit, null);
            }
        }

        for (Diagnostic<? extends JavaFileObject> diagnostic
                : diagnostics.getDiagnostics()) {

            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                throw new IOException(
                        "Java parsing failed for " + javaFile
                                + ": " + diagnostic.getMessage(Locale.ROOT));
            }
        }

        return records;
    }

    private String csvField(String value) {

        return "\""
                + value.replace("\"", "\"\"")
                + "\"";
    }

    private record ClassMetadata(
            String filePath,
            String fullyQualifiedName,
            int loc,
            boolean unnamedPackage) {
    }
}
