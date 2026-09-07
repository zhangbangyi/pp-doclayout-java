package com.example.doclayout.model;

import java.util.List;

/**
 * PP-DocLayoutV3 类别 id 与可读标签之间的映射表。
 */
public final class Labels {
    private Labels() {
    }

    public static final List<String> NAMES = List.of("abstract", "algorithm", "aside_text", "chart", "content", "display_formula", "doc_title", "figure_title", "footer", "footer_image", "footnote", "formula_number", "header", "header_image", "image", "inline_formula", "number", "paragraph_title", "reference", "reference_content", "seal", "table", "text", "vertical_text", "vision_footnote");

    public static String of(int id) {
        // 对未知类别保留原始 id，避免因模型扩展导致结果解码直接失败。
        return id >= 0 && id < NAMES.size() ? NAMES.get(id) : "unknown(" + id + ")";
    }
}
