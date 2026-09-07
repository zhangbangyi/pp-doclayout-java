package com.example.doclayout.output;

import com.example.doclayout.model.LayoutBox;
import com.example.doclayout.model.LayoutResult;
import com.example.doclayout.model.RegionRelation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * 将推理结果序列化为无需额外依赖的 JSON 文件。
 */
public final class JsonWriter {
    private JsonWriter() {
    }

    /**
     * 写出模型信息、阶段耗时和全部检测框。
     */
    public static void write(LayoutResult r, Path path) throws IOException {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("{\n");
        sb.append("  \"model\": \"").append(esc(r.modelVersion())).append("\",\n");
        sb.append("  \"image\": {\"width\": ").append(r.originalWidth()).append(", \"height\": ").append(r.originalHeight()).append("},\n");
        sb.append("  \"timingMs\": {\n").append("    \"preprocess\": ").append(ms(r.preprocessNanos())).append(",\n").append("    \"inference\": ").append(ms(r.inferenceNanos())).append(",\n").append("    \"postprocess\": ").append(ms(r.postprocessNanos())).append(",\n").append("    \"total\": ").append(ms(r.totalNanos())).append("\n  },\n");
        sb.append("  \"boxes\": [\n");
        for (int i = 0; i < r.boxes().size(); i++) {
            LayoutBox b = r.boxes().get(i);
            sb.append("    {\"classId\": ").append(b.classId()).append(", \"label\": \"").append(esc(b.label())).append("\"").append(", \"score\": ").append(f(b.score())).append(", \"box\": [").append(f(b.x1())).append(", ").append(f(b.y1())).append(", ").append(f(b.x2())).append(", ").append(f(b.y2())).append("]").append(", \"modelOrder\": ").append(b.modelOrder()).append(", \"order\": ").append(b.order()).append(", \"ocrText\": \"").append(esc(b.ocrText())).append("\"}");
            if (i + 1 < r.boxes().size()) sb.append(',');
            sb.append('\n');
        }
        sb.append("  ],\n");
        sb.append("  \"spatialRelations\": [\n");
        for (int i = 0; i < r.spatialRelations().relations().size(); i++) {
            RegionRelation relation = r.spatialRelations().relations().get(i);
            sb.append("    {\"from\": ").append(relation.fromIndex())
                    .append(", \"to\": ").append(relation.toIndex())
                    .append(", \"type\": \"").append(relation.type()).append("\"")
                    .append(", \"score\": ").append(f(relation.score()))
                    .append(", \"distance\": ").append(f(relation.distance())).append("}");
            if (i + 1 < r.spatialRelations().relations().size()) sb.append(',');
            sb.append('\n');
        }
        sb.append("  ]\n}\n");
        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.writeString(path, sb.toString());
    }

    /**
     * 纳秒转毫秒，保留小数以便前端/人工查看耗时。
     */
    private static double ms(long nanos) {
        return nanos / 1_000_000.0;
    }

    /**
     * 使用固定小数位和 ROOT locale，保证不同系统的小数点始终为句点。
     */
    private static String f(float x) {
        return String.format(Locale.ROOT, "%.6f", x);
    }

    /**
     * 转义 JSON 字符串中的反斜杠和双引号。
     */
    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
