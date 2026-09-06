package com.example.doclayout.model;

public record LayoutBox(
        int classId,
        String label,
        float score,
        float x1,
        float y1,
        float x2,
        float y2,
        long batchIndex,
        long order,
        int[] mask
) {
    public LayoutBox withOrder(long newOrder) {
        return new LayoutBox(classId, label, score, x1, y1, x2, y2, batchIndex, newOrder, mask);
    }
}
