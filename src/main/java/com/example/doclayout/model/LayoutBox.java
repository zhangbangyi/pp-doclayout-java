package com.example.doclayout.model;

/**
 * 单个版面元素的检测结果，坐标采用原始图片像素坐标。
 */
public record LayoutBox(int classId, String label, float score, float x1, float y1, float x2, float y2, long modelOrder,
                        long order, String ocrText, int[] mask) {
    /**
     * 在不改变其他字段的情况下替换排序序号。
     */
    public LayoutBox withOrder(long newOrder) {
        return new LayoutBox(classId, label, score, x1, y1, x2, y2, modelOrder, newOrder, ocrText, mask);
    }

    /** 返回保留版面元数据、仅补充 OCR 文字的新对象。 */
    public LayoutBox withOcrText(String newOcrText) {
        return new LayoutBox(classId, label, score, x1, y1, x2, y2, modelOrder, order, newOcrText, mask);
    }
}
