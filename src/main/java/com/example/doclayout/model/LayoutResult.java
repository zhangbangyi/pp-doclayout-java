package com.example.doclayout.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 一次图片推理的完整结果，包含尺寸、耗时、检测框和模型元信息。
 */
public record LayoutResult(int originalWidth, int originalHeight, int inputWidth, int inputHeight, long preprocessNanos,
                           long inferenceNanos, long postprocessNanos, List<LayoutBox> boxes, String[] outputNames,
                           String modelVersion, SpatialRelationGraph spatialRelations) {
    /** 保持已有调用方兼容，并为检测框自动建立默认空间关系图。 */
    public LayoutResult(int originalWidth, int originalHeight, int inputWidth, int inputHeight, long preprocessNanos,
                        long inferenceNanos, long postprocessNanos, List<LayoutBox> boxes, String[] outputNames,
                        String modelVersion) {
        this(originalWidth, originalHeight, inputWidth, inputHeight, preprocessNanos, inferenceNanos,
                postprocessNanos, boxes, outputNames, modelVersion, SpatialRelationGraph.fromBoxes(boxes));
    }

    public LayoutResult {
        boxes = boxes == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(boxes));
        outputNames = outputNames == null ? new String[0] : outputNames.clone();
        spatialRelations = spatialRelations == null ? SpatialRelationGraph.fromBoxes(boxes) : spatialRelations;
        if (spatialRelations.nodeCount() != boxes.size()) {
            throw new IllegalArgumentException("空间关系图节点数量必须与区域数量一致");
        }
    }

    @Override
    public String[] outputNames() {
        return outputNames.clone();
    }
    /**
     * 返回预处理、推理和后处理三阶段耗时之和（纳秒）。
     */
    public long totalNanos() {
        return preprocessNanos + inferenceNanos + postprocessNanos;
    }
}
