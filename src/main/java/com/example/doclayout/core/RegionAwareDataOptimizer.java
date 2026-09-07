package com.example.doclayout.core;

import com.example.doclayout.model.LayoutBox;
import com.example.doclayout.model.LayoutResult;
import com.example.doclayout.model.RegionRelation;
import com.example.doclayout.model.SpatialRelationConfig;
import com.example.doclayout.model.SpatialRelationGraph;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Collections;

/**
 * 区域感知数据优化入口：为检测区域构建空间关系图，并给出关系驱动的稳定阅读顺序。
 *
 * <p>该类不修改或丢弃模型检测框，调用方可以安全地将它作为现有推理流程的后处理层。
 * 阅读顺序返回的是原始 boxes 下标，关系边也使用同一套下标，便于下游合并 OCR、表格
 * 或知识图谱数据。</p>
 */
public final class RegionAwareDataOptimizer {
    private final SpatialRelationConfig config;

    public RegionAwareDataOptimizer() {
        this(SpatialRelationConfig.defaults());
    }

    public RegionAwareDataOptimizer(SpatialRelationConfig config) {
        this.config = config == null ? SpatialRelationConfig.defaults() : config;
    }

    public OptimizationResult optimize(List<LayoutBox> boxes) {
        // 不能使用 List.copyOf：它拒绝 null，而保留 null 节点能让关系端点继续对齐原始下标。
        List<LayoutBox> safeBoxes = boxes == null ? List.of() :
                Collections.unmodifiableList(new ArrayList<>(boxes));
        SpatialRelationGraph graph = SpatialRelationGraph.fromBoxes(safeBoxes, config);
        List<Integer> readingOrder = new ArrayList<>();
        for (int i = 0; i < safeBoxes.size(); i++) {
            if (safeBoxes.get(i) != null) readingOrder.add(i);
        }
        readingOrder.sort(readingComparator(safeBoxes));
        return new OptimizationResult(safeBoxes, graph, readingOrder);
    }

    /** 从完整推理结果提取区域关系数据；原结果本身保持不变。 */
    public OptimizationResult optimize(LayoutResult result) {
        if (result == null) throw new IllegalArgumentException("LayoutResult 不能为空");
        return optimize(result.boxes());
    }

    private static Comparator<Integer> readingComparator(List<LayoutBox> boxes) {
        return Comparator.comparingInt((Integer i) -> {
                    long order = boxes.get(i).order();
                    return order > 0 ? 0 : 1;
                })
                .thenComparingLong(i -> boxes.get(i).order() > 0 ? boxes.get(i).order() : Long.MAX_VALUE)
                .thenComparingDouble(i -> boxes.get(i).y1())
                .thenComparingDouble(i -> boxes.get(i).x1())
                .thenComparingInt(Integer::intValue);
    }

    public record OptimizationResult(List<LayoutBox> boxes, SpatialRelationGraph relations,
                                     List<Integer> readingOrder) {
        public OptimizationResult {
            boxes = boxes == null ? List.of() :
                    Collections.unmodifiableList(new ArrayList<>(boxes));
            relations = relations == null ? SpatialRelationGraph.empty(boxes.size()) : relations;
            readingOrder = readingOrder == null ? List.of() : List.copyOf(readingOrder);
            if (relations.nodeCount() != boxes.size()) {
                throw new IllegalArgumentException("空间关系图节点数量必须与区域数量一致");
            }
        }

        public List<RegionRelation> spatialRelations() {
            return relations.relations();
        }
    }
}
