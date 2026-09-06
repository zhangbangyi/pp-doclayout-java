package com.example.doclayout.model;

import java.util.List;

public final class Labels {
    private Labels() {}

    public static final List<String> NAMES = List.of(
            "abstract", "algorithm", "aside_text", "chart", "content",
            "display_formula", "doc_title", "figure_title", "footer", "footer_image",
            "footnote", "formula_number", "header", "header_image", "image",
            "inline_formula", "number", "paragraph_title", "reference", "reference_content",
            "seal", "table", "text", "vertical_text", "vision_footnote"
    );

    public static String of(int id) {
        return id >= 0 && id < NAMES.size() ? NAMES.get(id) : "unknown(" + id + ")";
    }
}
