package com.example.doclayout.model;

import java.util.List;

/**
 * 一次图片推理的完整结果，包含尺寸、耗时、检测框和模型元信息。
 */
public record LayoutResult(int originalWidth, int originalHeight, int inputWidth, int inputHeight, long preprocessNanos,
                           long inferenceNanos, long postprocessNanos, List<LayoutBox> boxes, String[] outputNames,
                           String modelVersion) {
    /**
     * 返回预处理、推理和后处理三阶段耗时之和（纳秒）。
     */
    public long totalNanos() {
        return preprocessNanos + inferenceNanos + postprocessNanos;
    }
}
