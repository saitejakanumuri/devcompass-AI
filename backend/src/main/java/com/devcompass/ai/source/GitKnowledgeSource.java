package com.devcompass.ai.source;

import com.devcompass.ai.model.Document;
import com.devcompass.ai.model.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class GitKnowledgeSource implements KnowledgeSource {

    private static final Logger log = LoggerFactory.getLogger(GitKnowledgeSource.class);

    public Optional<String> validateConfig(Map<String, Object> config) {
        String repoUrl = str(config, "repoUrl", "");
        String repoPath = str(config, "repoPath", "");

        if (repoUrl.isBlank() && repoPath.isBlank()) {
            return Optional.of("Repository URL or local path must be provided.");
        }

        if (!repoUrl.isBlank()) {
            if (!repoUrl.startsWith("http://") && !repoUrl.startsWith("https://")) {
                return Optional.of("Repository URL must be a valid HTTP/HTTPS URL.");
            }
        } else {
            try {
                Path rootPath = Paths.get(repoPath).toAbsolutePath().normalize();
                if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
                    return Optional.of("Configured Git repository path '" + repoPath + "' is not a valid directory.");
                }
            } catch (Exception e) {
                return Optional.of("Invalid Git repository path format: " + e.getMessage());
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Document> sync(Map<String, Object> config) {
        String repoUrl = str(config, "repoUrl", "");
        String repoPath = str(config, "repoPath", "");
        String branch = str(config, "branch", "main");
        String includedExtensions = str(config, "includedExtensions", "java,ts,tsx,py,go,rs,yml,yaml,md,json");
        long maxFileSizeKb = 500L;
        try {
            maxFileSizeKb = Long.parseLong(str(config, "maxFileSizeKb", "500"));
        } catch (Exception ignored) {}

        Path rootPath = null;
        boolean isTempDir = false;

        try {
            if (!repoUrl.isBlank()) {
                log.info("[GitKnowledgeSource] Cloning Git repository '{}' branch '{}'...", repoUrl, branch);
                rootPath = Files.createTempDirectory("devcompass-git-");
                isTempDir = true;

                ProcessBuilder pb = new ProcessBuilder(
                    "git", "clone", "--depth", "1", "-b", branch, repoUrl, rootPath.toString()
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();
                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    String output = new String(process.getInputStream().readAllBytes());
                    log.error("[GitKnowledgeSource] Git clone failed with exit code {}: {}", exitCode, output);
                    return new ArrayList<>();
                }
                log.info("[GitKnowledgeSource] Clone successful to temporary directory {}", rootPath);
            } else if (!repoPath.isBlank()) {
                log.info("[GitKnowledgeSource] Scanning local Git repository at path '{}'...", repoPath);
                rootPath = Paths.get(repoPath).toAbsolutePath().normalize();
                if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
                    log.warn("[GitKnowledgeSource] Configured Git path '{}' does not exist or is not a directory.", rootPath);
                    return new ArrayList<>();
                }
            } else {
                return new ArrayList<>();
            }

            Set<String> allowedExts = Arrays.stream(includedExtensions.toLowerCase().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

            List<Document> documents = new ArrayList<>();

            final Path finalRootPath = rootPath;
            try (Stream<Path> stream = Files.walk(rootPath)) {
                List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> isAllowedPath(p, finalRootPath, allowedExts))
                    .toList();

                for (Path path : files) {
                    try {
                        long sizeKb = Files.size(path) / 1024;
                        if (sizeKb > maxFileSizeKb) {
                            log.debug("Skipping file {} as size ({} KB) exceeds limit of {} KB", path, sizeKb, maxFileSizeKb);
                            continue;
                        }

                        String content = Files.readString(path);
                        if (content.isBlank()) continue;

                        Path relativePath = rootPath.relativize(path);
                        String fileBasename = path.getFileName().toString();
                        String docId = "git:" + relativePath.toString().replace('\\', '/');

                        Document doc = new Document(
                            UUID.randomUUID().toString(),
                            fileBasename + " (" + relativePath.toString().replace('\\', '/') + ")",
                            docId,
                            SourceType.GIT_REPOSITORY,
                            content,
                            Map.of(
                                "relativePath", relativePath.toString().replace('\\', '/'),
                                "fileName", fileBasename,
                                "repoUrl", repoUrl.isBlank() ? "Local Workspace Repository" : repoUrl,
                                "branch", branch,
                                "fileSizeKb", sizeKb,
                                "source", "Live Git Code File Scanner"
                            )
                        );
                        documents.add(doc);
                    } catch (Exception e) {
                        log.debug("Notice reading file {}: {}", path, e.getMessage());
                    }
                }
            }

            if (documents.isEmpty()) {
                log.warn("[GitKnowledgeSource] No matching code files found at path '{}'. Returning empty documents list.", rootPath);
                return new ArrayList<>();
            }

            log.info("[GitKnowledgeSource] Scanned and extracted {} source code documents from Git repository.", documents.size());
            return documents;

        } catch (Exception e) {
            log.error("[GitKnowledgeSource] Error processing Git repository: {}", e.getMessage());
            return new ArrayList<>();
        } finally {
            if (isTempDir && rootPath != null) {
                try {
                    FileSystemUtils.deleteRecursively(rootPath);
                    log.info("[GitKnowledgeSource] Cleaned up temporary directory {}", rootPath);
                } catch (IOException e) {
                    log.warn("[GitKnowledgeSource] Failed to delete temporary directory {}: {}", rootPath, e.getMessage());
                }
            }
        }
    }

    private boolean isAllowedPath(Path path, Path rootPath, Set<String> allowedExts) {
        String pathString = path.toString().replace('\\', '/');

        // Ignore build artifacts, hidden dirs, and binaries
        if (pathString.contains("/.git/") || 
            pathString.contains("/target/") || 
            pathString.contains("/node_modules/") || 
            pathString.contains("/.idea/") || 
            pathString.contains("/dist/") || 
            pathString.contains("/build/") || 
            pathString.contains("/.mvn/")) {
            return false;
        }

        String fileName = path.getFileName().toString().toLowerCase();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) return false;

        String ext = fileName.substring(dotIndex + 1);
        return allowedExts.contains(ext);
    }

    @Override
    public SourceType type() {
        return SourceType.GIT_REPOSITORY;
    }

    @Override
    public String sourceName(Map<String, Object> config) {
        String repoUrl = str(config, "repoUrl", "");
        String repoPath = str(config, "repoPath", "./");
        return "Git Repository (" + (repoUrl.isBlank() ? repoPath : repoUrl) + ")";
    }

    @Override
    public boolean isHealthy(Map<String, Object> config) {
        return validateConfig(config).isEmpty();
    }
}
