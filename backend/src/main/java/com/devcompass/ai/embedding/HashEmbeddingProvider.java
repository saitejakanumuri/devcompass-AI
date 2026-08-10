package com.devcompass.ai.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component("hashEmbeddingProvider")
public class HashEmbeddingProvider implements EmbeddingProvider {

    private final int dimension;

    public HashEmbeddingProvider(@Value("${devcompass.vector-store.dimension:1536}") int dimension) {
        this.dimension = dimension;
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[dimension];
        if (text == null || text.isBlank()) return vector;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < dimension; i++) {
                byte b = hash[i % hash.length];
                vector[i] = (float) ((b & 0xFF) / 255.0 * 2.0 - 1.0);
            }
        } catch (Exception ignored) {}
        return vector;
    }

    @Override
    public String providerName() {
        return "local-hash";
    }

    @Override
    public int dimension() {
        return dimension;
    }
}
