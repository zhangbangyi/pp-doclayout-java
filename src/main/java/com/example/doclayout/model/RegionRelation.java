package com.example.doclayout.model;

/**
 * 空间关系图中的一条有向边。fromIndex/toIndex 对应识别结果 boxes 列表的下标。
 */
public record RegionRelation(int fromIndex, int toIndex, SpatialRelation type,
                             float score, float distance) {
    public RegionRelation {
        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) {
            throw new IllegalArgumentException("关系端点必须是不同的非负区域下标");
        }
        if (type == null) {
            throw new IllegalArgumentException("关系类型不能为空");
        }
        if (!Float.isFinite(score) || score < 0 || score > 1) {
            throw new IllegalArgumentException("关系置信度必须在 0 到 1 之间");
        }
        if (!Float.isFinite(distance) || distance < 0) {
            throw new IllegalArgumentException("区域距离必须是非负有限值");
        }
    }
}
