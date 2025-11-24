package com.example.facialscanner;

import java.util.ArrayList;
import java.util.List;

public class EmbeddingHelper {

    /**
     * Convert a float[] embedding into a List<Float>
     * so it can be stored in Firestore.
     */
    public static List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float v : array) {
            list.add(v);
        }
        return list;
    }

    /**
     * Convert a raw byte[] embedding (from a quantized UINT8 model)
     * into a normalized float[] in the range [0,1].
     */
    public static float[] toFloatEmbedding(byte[] rawEmbedding) {
        float[] embedding = new float[rawEmbedding.length];
        for (int i = 0; i < rawEmbedding.length; i++) {
            embedding[i] = (rawEmbedding[i] & 0xFF) / 255.0f;
        }
        return embedding;
    }

    /**
     * Calculate Euclidean distance between two embeddings.
     * Useful for recognition when comparing against stored vectors.
     */
    public static double euclideanDistance(float[] embedding, List<Double> storedEmbedding) {
        double sum = 0.0;
        for (int i = 0; i < embedding.length; i++) {
            double diff = embedding[i] - storedEmbedding.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * Calculate cosine similarity between two embeddings.
     * Often better than Euclidean distance for face recognition.
     */
    public static double cosineSimilarity(float[] embedding, List<Double> storedEmbedding) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < embedding.length; i++) {
            dot += embedding[i] * storedEmbedding.get(i);
            normA += embedding[i] * embedding[i];
            normB += storedEmbedding.get(i) * storedEmbedding.get(i);
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
