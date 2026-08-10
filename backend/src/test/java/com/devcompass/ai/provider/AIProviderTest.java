package com.devcompass.ai.provider;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AIProviderTest {

    @Test
    void testProviderFactorySwitching() {
        ClaudeProvider claude = new ClaudeProvider();
        GeminiProvider gemini = new GeminiProvider();
        OpenAIProvider openai = new OpenAIProvider();
        OllamaProvider ollama = new OllamaProvider();

        AIProviderFactory factory = new AIProviderFactory(List.of(claude, gemini, openai, ollama), "claude");

        assertEquals("claude", factory.getActiveProvider().providerName());

        factory.setActiveProvider("gemini");
        assertEquals("gemini", factory.getActiveProvider().providerName());

        factory.setActiveProvider("openai");
        assertEquals("openai", factory.getActiveProvider().providerName());

        factory.setActiveProvider("ollama");
        assertEquals("ollama", factory.getActiveProvider().providerName());
    }
}
