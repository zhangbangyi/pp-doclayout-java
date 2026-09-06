package com.example.doclayout.output;

import com.example.doclayout.model.LayoutBox;
import com.example.doclayout.model.LayoutResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class JsonWriter {
    private JsonWriter() {}

    public static void write(LayoutResult r, Path path) throws IOException {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("{\n");
        sb.append("  \"model\": \"").append(esc(r.modelVersion())).append("\",\n");
        sb.append("  \"image\": {\"width\": ").append(r.originalWidth())
          .append(", \"height\": ").append(r.originalHeight()).append("},\n");
        sb.append("  \"timingMs\": {\n")
          .append("    \"preprocess\": ").append(ms(r.preprocessNanos())).append(",\n")
          .append("    \"inference\": ").append(ms(r.inferenceNanos())).append(",\n")
          .append("    \"postprocess\": ").append(ms(r.postprocessNanos())).append(",\n")
          .append("    \"total\": ").append(ms(r.totalNanos())).append("\n  },\n");
        sb.append("  \"boxes\": [\n");
        for (int i = 0; i < r.boxes().size(); i++) {
            LayoutBox b = r.boxes().get(i);
            sb.append("    {\"classId\": ").append(b.classId())
              .append(", \"label\": \"").append(esc(b.label())).append("\"")
              .append(", \"score\": ").append(f(b.score()))
              .append(", \"box\": [").append(f(b.x1())).append(", ").append(f(b.y1()))
              .append(", ").append(f(b.x2())).append(", ").append(f(b.y2())).append("]")
              .append(", \"batchIndex\": ").append(b.batchIndex())
              .append(", \"order\": ").append(b.order()).append("}");
            if (i + 1 < r.boxes().size()) sb.append(',');
            sb.append('\n');
        }
        sb.append("  ]\n}\n");
        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.writeString(path, sb.toString());
    }

    private static double ms(long nanos) { return nanos / 1_000_000.0; }
    private static String f(float x) { return String.format(Locale.ROOT, "%.6f", x); }
    private static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
