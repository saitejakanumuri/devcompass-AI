package com.devcompass.ai.pipeline;

import com.devcompass.ai.model.Document;
import org.springframework.stereotype.Component;

@Component
public class DocumentExtractor {

    public String extractCleanText(Document document) {
        if (document.content() == null) return "";
        
        // Normalize whitespace and strip unneeded formatting for embedding generation
        return document.content()
            .replaceAll("\r\n", "\n")
            .trim();
    }
}
