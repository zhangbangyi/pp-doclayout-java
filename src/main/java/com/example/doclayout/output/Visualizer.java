package com.example.doclayout.output;

import com.example.doclayout.model.LayoutBox;
import com.example.doclayout.model.LayoutResult;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * 将检测框绘制到原图上，并负责把结果保存为图片文件。
 */
public final class Visualizer {
    private Visualizer() {
    }

    /**
     * 渲染并写出 JPEG 文件；目录不存在时会自动创建。
     */
    public static void draw(BufferedImage original, LayoutResult result, Path output) throws IOException {
        BufferedImage canvas = render(original, result);
        Files.createDirectories(output.toAbsolutePath().getParent());
        ImageIO.write(canvas, "jpg", output.toFile());
    }

    /**
     * 在内存中生成带标注的图片。每个类别使用稳定的 HSV 颜色，便于同一类别跨图片比较。
     */
    public static BufferedImage render(BufferedImage original, LayoutResult result) {
        BufferedImage canvas = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.drawImage(original, 0, 0, null);
            g.setStroke(new BasicStroke(2.0f));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            for (LayoutBox b : result.boxes()) {
                // 检测框已在解码阶段裁剪到原图范围，这里只需转换为整数像素绘制。
                int x = Math.round(b.x1());
                int y = Math.round(b.y1());
                int w = Math.max(1, Math.round(b.x2() - b.x1()));
                int h = Math.max(1, Math.round(b.y2() - b.y1()));
                // 使用整数哈希生成类别颜色，避免每次渲染随机变色。
                int hash = Math.abs(b.classId() * 265443576 + 17);
                Color c = Color.getHSBColor((hash % 360) / 360f, 0.9f, 0.95f);
                g.setColor(c);
                g.drawRect(x, y, w, h);
                // 标签放在框线上方；靠近图片顶部时向下放置，防止文字越界。
                String label = b.label() + " " + String.format(java.util.Locale.ROOT, "%.3f", b.score());
                int ty = Math.max(14, y - 2);
                g.drawString(label, x, ty);
            }
        } finally {
            g.dispose();
        }
        return canvas;
    }
}
