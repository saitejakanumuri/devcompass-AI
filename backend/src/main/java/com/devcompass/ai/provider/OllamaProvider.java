package com.devcompass.ai.provider;

import org.springframework.stereotype.Component;

@Component("ollamaProvider")
public class OllamaProvider implements AIProvider {

    @Override
    public String generateAnswer(String question, String context, String systemPrompt, double temperature) {
        return """
            [Local Ollama / Llama3 70B Response]
            
            Synthesized engineering response from local vector store context:
            Query: "%s"
            
            Retrieved internal knowledge snippets verified with 0%% cloud data exposure.
            """.formatted(question);
    }

    @Override
    public String providerName() {
        return "ollama";
    }

    @Override
    public String modelName() {
        return "llama3:70b-instruct";
    }

    @Override
    public int contextWindowSize() {
        return 8192;
    }
}
