package com.example.doclayout.output;

import com.example.doclayout.model.LayoutBox;
import com.example.doclayout.model.LayoutResult;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import javax.imageio.ImageIO;

public final class HtmlWriter {
    private HtmlWriter() {}

    public static void write(BufferedImage original, BufferedImage annotated,
                             LayoutResult result, Path output) throws IOException {
        String originalData = dataUri(original, "png");
        String annotatedData = dataUri(annotated, "jpeg");
        StringBuilder html = new StringBuilder(24_000);
        html.append("<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">")
            .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
            .append("<title>PP-DocLayoutV3 版面识别</title><style>")
            .append("*{box-sizing:border-box}body{margin:0;background:#f4f6f8;color:#17202a;font-family:Segoe UI,Microsoft YaHei,sans-serif}")
            .append(".shell{max-width:1500px;margin:0 auto;padding:28px 30px 40px}.top{display:flex;justify-content:space-between;align-items:end;gap:20px;margin-bottom:22px}")
            .append("h1{font-size:25px;margin:0 0 6px;font-weight:700;letter-spacing:0}.sub{color:#697586;font-size:13px}.stats{display:flex;gap:10px;flex-wrap:wrap}")
            .append(".stat{background:#fff;border:1px solid #dfe5eb;border-radius:7px;padding:10px 14px;min-width:112px}.stat b{display:block;font-size:18px}.stat span{display:block;color:#697586;font-size:12px;margin-top:2px}")
            .append(".compare{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:18px}.panel{background:#fff;border:1px solid #dfe5eb;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px #17202a0b}")
            .append(".panel-head{display:flex;justify-content:space-between;align-items:center;padding:13px 16px;border-bottom:1px solid #e7ebef;font-size:14px;font-weight:650}.panel-head small{color:#8a95a3;font-weight:400}")
            .append(".stage{padding:16px;background:#e9edf1;display:flex;justify-content:center;align-items:flex-start;min-height:420px}.stage img{display:block;max-width:100%;height:auto;max-height:calc(100vh - 250px);object-fit:contain;box-shadow:0 2px 8px #17202a20}")
            .append(".details{margin-top:18px;background:#fff;border:1px solid #dfe5eb;border-radius:8px;overflow:hidden}.details-head{padding:13px 16px;border-bottom:1px solid #e7ebef;font-size:14px;font-weight:650}.table-wrap{overflow:auto}table{border-collapse:collapse;width:100%;font-size:13px}th,td{text-align:left;padding:10px 14px;border-bottom:1px solid #edf0f3;white-space:nowrap}th{background:#fafbfc;color:#697586;font-size:12px;font-weight:600}tbody tr:last-child td{border-bottom:0}.score{font-variant-numeric:tabular-nums;font-weight:650}.empty{padding:28px;color:#697586;text-align:center}")
            .append("@media(max-width:900px){.shell{padding:18px 14px 28px}.top{display:block}.stats{margin-top:16px}.compare{grid-template-columns:1fr}.stage img{max-height:none}}")
            .append("</style></head><body><main class=\"shell\"><header class=\"top\"><div><h1>PP-DocLayoutV3 版面识别</h1><div class=\"sub\">原图与识别框线对照</div></div><div class=\"stats\">")
            .append(stat(result.boxes().size(), "识别框"))
            .append(stat(String.format(Locale.ROOT, "%.1f ms", result.inferenceNanos() / 1_000_000.0), "推理耗时"))
            .append(stat(result.originalWidth() + " × " + result.originalHeight(), "原图尺寸"))
            .append("</div></header><section class=\"compare\">")
            .append(panel("原图", "输入图片", originalData))
            .append(panel("识别结果", "带框线与标签", annotatedData))
            .append("</section><section class=\"details\"><div class=\"details-head\">识别明细</div>");
        if (result.boxes().isEmpty()) {
            html.append("<div class=\"empty\">没有达到当前置信度阈值的识别框</div>");
        } else {
            html.append("<div class=\"table-wrap\"><table><thead><tr><th>#</th><th>类别</th><th>置信度</th><th>坐标</th></tr></thead><tbody>");
            for (int i = 0; i < result.boxes().size(); i++) {
                LayoutBox b = result.boxes().get(i);
                html.append("<tr><td>").append(i + 1).append("</td><td>").append(escape(b.label()))
                    .append("</td><td class=\"score\">").append(String.format(Locale.ROOT, "%.3f", b.score()))
                    .append("</td><td>").append(String.format(Locale.ROOT, "[%.1f, %.1f, %.1f, %.1f]", b.x1(), b.y1(), b.x2(), b.y2()))
                    .append("</td></tr>");
            }
            html.append("</tbody></table></div>");
        }
        html.append("</section></main></body></html>");
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, html.toString(), StandardCharsets.UTF_8);
    }

    private static String panel(String title, String hint, String image) {
        return "<article class=\"panel\"><div class=\"panel-head\"><span>" + title
                + "</span><small>" + hint + "</small></div><div class=\"stage\"><img src=\""
                + image + "\" alt=\"" + title + "\"></div></article>";
    }

    private static String stat(Object value, String label) {
        return "<div class=\"stat\"><b>" + value + "</b><span>" + label + "</span></div>";
    }

    private static String dataUri(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, format, bytes);
        return "data:image/" + format + ";base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
