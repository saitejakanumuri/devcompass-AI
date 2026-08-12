package com.devcompass.ai.source;

import com.devcompass.ai.model.Document;
import com.devcompass.ai.model.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
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

    private String repoUrl;
    private String repoPath;
    private String branch;
    private String includedExtensions;
    private long maxFileSizeKb;

    public GitKnowledgeSource() {
        this("", "./", "main", "java,ts,tsx,py,go,rs,yml,yaml,md,json", 500L);
    }

    @Autowired
    public GitKnowledgeSource(
        @Value("${devcompass.sources.git.repo-url:}") String repoUrl,
        @Value("${devcompass.sources.git.repo-path:./}") String repoPath,
        @Value("${devcompass.sources.git.branch:main}") String branch,
        @Value("${devcompass.sources.git.included-extensions:java,ts,tsx,py,go,rs,yml,yaml,md,json}") String includedExtensions,
        @Value("${devcompass.sources.git.max-file-size-kb:500}") Long maxFileSizeKb
    ) {
        this.repoUrl = repoUrl != null ? repoUrl.trim() : "";
        this.repoPath = repoPath != null ? repoPath.trim() : "./";
        this.branch = branch != null ? branch.trim() : "main";
        this.includedExtensions = includedExtensions != null ? includedExtensions.trim() : "java,ts,tsx,py,go,rs,yml,yaml,md,json";
        this.maxFileSizeKb = maxFileSizeKb != null ? maxFileSizeKb : 500L;
    }

    public synchronized void updateConfig(String repoUrl, String repoPath, String branch, String includedExtensions, Long maxFileSizeKb) {
        if (repoUrl != null) this.repoUrl = repoUrl.trim();
        if (repoPath != null && !repoPath.isBlank()) this.repoPath = repoPath.trim();
        if (branch != null && !branch.isBlank()) this.branch = branch.trim();
        if (includedExtensions != null && !includedExtensions.isBlank()) this.includedExtensions = includedExtensions.trim();
        if (maxFileSizeKb != null && maxFileSizeKb > 0) this.maxFileSizeKb = maxFileSizeKb;
        log.info("[GitKnowledgeSource] Config updated: repoUrl='{}', repoPath='{}', branch='{}', extensions='{}'", this.repoUrl, this.repoPath, this.branch, this.includedExtensions);
    }

    public Optional<String> validateConfig() {
        if (repoPath == null || repoPath.isBlank()) {
            return Optional.of("Repository file path is empty.");
        }
        try {
            Path rootPath = Paths.get(repoPath).toAbsolutePath().normalize();
            if (!Files.exists(rootPath)) {
                return Optional.of("Configured Git repository path '" + repoPath + "' does not exist on disk.");
            }
            if (!Files.isDirectory(rootPath)) {
                return Optional.of("Configured Git repository path '" + repoPath + "' is not a valid directory.");
            }
        } catch (Exception e) {
            return Optional.of("Invalid Git repository path format: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Map<String, Object> getConfig() {
        Optional<String> validationErr = validateConfig();
        Map<String, Object> config = new HashMap<>();
        config.put("repoUrl", repoUrl);
        config.put("repoPath", repoPath);
        config.put("branch", branch);
        config.put("includedExtensions", includedExtensions);
        config.put("maxFileSizeKb", maxFileSizeKb);
        config.put("healthy", isHealthy());
        config.put("validationError", validationErr.orElse(null));
        return config;
    }

    @Override
    public List<Document> sync() {
        log.info("[GitKnowledgeSource] Scanning Git repository at path '{}' for source code artifacts...", repoPath);
        Path rootPath = Paths.get(repoPath).toAbsolutePath().normalize();

        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            log.warn("[GitKnowledgeSource] Configured Git path '{}' does not exist or is not a directory. Returning empty documents list.", rootPath);
            return new ArrayList<>();
        }

        Set<String> allowedExts = Arrays.stream(includedExtensions.toLowerCase().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());

        List<Document> documents = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(rootPath)) {
            List<Path> files = stream
                .filter(Files::isRegularFile)
                .filter(p -> isAllowedPath(p, rootPath, allowedExts))
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
                    String docId = "git-file-" + relativePath.toString().replace('\\', '/');

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
        } catch (IOException e) {
            log.error("[GitKnowledgeSource] Error scanning Git repository at path {}: {}", rootPath, e.getMessage());
            return new ArrayList<>();
        }

        if (documents.isEmpty()) {
            log.warn("[GitKnowledgeSource] No matching code files found at path '{}'. Returning empty documents list.", rootPath);
            return new ArrayList<>();
        }

        log.info("[GitKnowledgeSource] Scanned and extracted {} source code documents from Git repository.", documents.size());
        return documents;
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
    public String sourceName() {
        return "Git Repository (" + (repoUrl.isBlank() ? repoPath : repoUrl) + ")";
    }

    @Override
    public boolean isHealthy() {
        return validateConfig().isEmpty();
    }
}
