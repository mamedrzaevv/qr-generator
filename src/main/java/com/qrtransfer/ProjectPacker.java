/*
 * Decompiled with CFR 0.152.
 */
package com.qrtransfer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.stream.Stream;

public class ProjectPacker {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435: java ProjectPacker <\u043f\u0443\u0442\u044c_\u043a_\u043f\u0440\u043e\u0435\u043a\u0442\u0443> [\u0432\u044b\u0445\u043e\u0434\u043d\u043e\u0439_\u0444\u0430\u0439\u043b]");
            System.out.println("\u041f\u0440\u0438\u043c\u0435\u0440: java ProjectPacker . project.txt");
            return;
        }
        String projectPath = args[0];
        String outputFile = args.length > 1 ? args[1] : "project.txt";
        try {
            ProjectPacker.packProject(projectPath, outputFile);
            System.out.println("\u041f\u0440\u043e\u0435\u043a\u0442 \u0443\u0441\u043f\u0435\u0448\u043d\u043e \u0443\u043f\u0430\u043a\u043e\u0432\u0430\u043d \u0432 \u0444\u0430\u0439\u043b: " + outputFile);
        }
        catch (Exception e) {
            System.err.println("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u0443\u043f\u0430\u043a\u043e\u0432\u043a\u0435 \u043f\u0440\u043e\u0435\u043a\u0442\u0430: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void packProject(String projectPath, String outputFile) throws IOException {
        Path projectDir = Paths.get(projectPath, new String[0]);
        if (!Files.exists(projectDir, new LinkOption[0])) {
            throw new IOException("\u0414\u0438\u0440\u0435\u043a\u0442\u043e\u0440\u0438\u044f \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u0430: " + projectPath);
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile));){
            writer.println("=== QR FILE TRANSFER PROJECT ===");
            writer.println("\u0421\u043e\u0437\u0434\u0430\u043d\u043e: " + String.valueOf(LocalDateTime.now()));
            writer.println("\u041f\u0440\u043e\u0435\u043a\u0442: " + String.valueOf(projectDir.toAbsolutePath()));
            writer.println();
            String[] includePatterns = new String[]{"*.java", "*.xml", "*.md", "*.txt", "*.properties", "*.yml", "*.yaml"};
            String[] excludeDirs = new String[]{"target", ".git", ".idea", "node_modules", "bin", "out"};
            try (Stream<Path> paths = Files.walk(projectDir, new FileVisitOption[0]);){
                paths.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).filter(path -> ProjectPacker.shouldIncludeFile(path, includePatterns)).filter(path -> !ProjectPacker.isInExcludedDir(path, projectDir, excludeDirs)).sorted().forEach(path -> {
                    try {
                        ProjectPacker.writeFileContent(writer, path, projectDir);
                    }
                    catch (IOException e) {
                        System.err.println("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u043a\u0435 \u0444\u0430\u0439\u043b\u0430 " + String.valueOf(path) + ": " + e.getMessage());
                    }
                });
            }
            writer.println();
            writer.println("=== \u041a\u041e\u041d\u0415\u0426 \u041f\u0420\u041e\u0415\u041a\u0422\u0410 ===");
        }
    }

    private static boolean shouldIncludeFile(Path file, String[] patterns) {
        String fileName = file.getFileName().toString().toLowerCase();
        for (String pattern : patterns) {
            String extension;
            if (!(pattern.startsWith("*.") ? fileName.endsWith(extension = pattern.substring(1)) : fileName.equals(pattern))) continue;
            return true;
        }
        return false;
    }

    private static boolean isInExcludedDir(Path file, Path projectDir, String[] excludeDirs) {
        Path relativePath = projectDir.relativize(file);
        for (String excludeDir : excludeDirs) {
            if (!relativePath.startsWith(excludeDir)) continue;
            return true;
        }
        return false;
    }

    private static void writeFileContent(PrintWriter writer, Path file, Path projectDir) throws IOException {
        Path relativePath = projectDir.relativize(file);
        writer.println("=== \u0424\u0410\u0419\u041b: " + String.valueOf(relativePath) + " ===");
        writer.println("\u0420\u0430\u0437\u043c\u0435\u0440: " + Files.size(file) + " \u0431\u0430\u0439\u0442");
        writer.println("\u0414\u0430\u0442\u0430 \u0438\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u044f: " + String.valueOf(Files.getLastModifiedTime(file, new LinkOption[0])));
        writer.println();
        try {
            String content = Files.readString(file);
            writer.write(content);
        }
        catch (IOException e) {
            writer.println("\u041e\u0428\u0418\u0411\u041a\u0410 \u0427\u0422\u0415\u041d\u0418\u042f \u0424\u0410\u0419\u041b\u0410: " + e.getMessage());
        }
        writer.println();
        writer.println("---");
        writer.println();
    }
}
