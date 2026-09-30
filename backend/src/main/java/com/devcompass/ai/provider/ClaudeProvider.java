package com.devcompass.ai.provider;

import org.springframework.stereotype.Component;

@Component("claudeProvider")
public class ClaudeProvider implements AIProvider {

    @Override
    public String generateAnswer(String question, String context, String systemPrompt, double temperature) {
        // Simulated Claude 3.5 Sonnet response generation using retrieved context
        return """
          Cannot call Claude LLM, Please use Gemini Provider now.
            """;
    }

    @Override
    public String providerName() {
        return "claude";
    }

    @Override
    public String modelName() {
        return "claude-3-5-sonnet";
    }

    @Override
    public int contextWindowSize() {
        return 200000;
    }
}
