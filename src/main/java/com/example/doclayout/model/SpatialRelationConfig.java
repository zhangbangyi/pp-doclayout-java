package com.example.doclayout.model;

/**
 * 空间关系推断的可调参数。所有比例均为 0 到 1 之间的数值。
 */
public record SpatialRelationConfig(
        float overlapIouThreshold,
        float minAxisOverlapRatio,
        float adjacencyGapRatio) {

    public SpatialRelationConfig {
        if (!Float.isFinite(overlapIouThreshold) || overlapIouThreshold < 0 || overlapIouThreshold > 1) {
            throw new IllegalArgumentException("overlapIouThreshold 必须在 0 到 1 之间");
        }
        if (!Float.isFinite(minAxisOverlapRatio) || minAxisOverlapRatio < 0 || minAxisOverlapRatio > 1) {
            throw new IllegalArgumentException("minAxisOverlapRatio 必须在 0 到 1 之间");
        }
        if (!Float.isFinite(adjacencyGapRatio) || adjacencyGapRatio < 0 || adjacencyGapRatio > 1) {
            throw new IllegalArgumentException("adjacencyGapRatio 必须在 0 到 1 之间");
        }
    }

    /** 适合文档检测框的默认阈值。 */
    public static SpatialRelationConfig defaults() {
        return new SpatialRelationConfig(0.10f, 0.20f, 0.05f);
    }
}
