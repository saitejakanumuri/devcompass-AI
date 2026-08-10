package com.devcompass.ai.provider;

public interface AIProvider {
    
    /**
     * Generates a natural language answer based solely on question, retrieved context, and system prompt.
     */
    String generateAnswer(String question, String context, String systemPrompt, double temperature);

    /**
     * Unique identifier/name of the provider (e.g. claude, gemini, openai, ollama).
     */
    String providerName();

    /**
     * Specific model backing this provider (e.g. claude-3-5-sonnet, gemini-1.5-pro, gpt-4o, llama3).
     */
    String modelName();

    /**
     * Context window size in tokens.
     */
    int contextWindowSize();
}
