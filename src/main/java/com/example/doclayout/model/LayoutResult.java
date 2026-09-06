package com.example.doclayout.model;

import java.util.List;

public record LayoutResult(
        int originalWidth,
        int originalHeight,
        int inputWidth,
        int inputHeight,
        long preprocessNanos,
        long inferenceNanos,
        long postprocessNanos,
        List<LayoutBox> boxes,
        String[] outputNames,
        String modelVersion
) {
    public long totalNanos() {
        return preprocessNanos + inferenceNanos + postprocessNanos;
    }
}
