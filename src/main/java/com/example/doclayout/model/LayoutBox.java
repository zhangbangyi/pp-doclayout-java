package com.example.doclayout.model;

/**
 * 单个版面元素的检测结果，坐标采用原始图片像素坐标。
 */
public record LayoutBox(int classId, String label, float score, float x1, float y1, float x2, float y2, long modelOrder,
                        long order, int[] mask) {
    /**
     * 在不改变其他字段的情况下替换排序序号。
     */
    public LayoutBox withOrder(long newOrder) {
        return new LayoutBox(classId, label, score, x1, y1, x2, y2, modelOrder, newOrder, mask);
    }
}
