package com.devcompass.ai.model;

public record GitRepoConfigRequest(
    String repoUrl,
    String repoPath,
    String branch,
    String includedExtensions,
    Long maxFileSizeKb
) {}
