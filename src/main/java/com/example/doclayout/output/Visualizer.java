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

public final class Visualizer {
    private Visualizer() {}

    public static void draw(BufferedImage original, LayoutResult result, Path output) throws IOException {
        BufferedImage canvas = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.drawImage(original, 0, 0, null);
            g.setStroke(new BasicStroke(2.0f));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            for (LayoutBox b : result.boxes()) {
                int x = Math.round(b.x1());
                int y = Math.round(b.y1());
                int w = Math.max(1, Math.round(b.x2() - b.x1()));
                int h = Math.max(1, Math.round(b.y2() - b.y1()));
                int hash = Math.abs(b.classId() * 265443576 + 17);
                Color c = Color.getHSBColor((hash % 360) / 360f, 0.9f, 0.95f);
                g.setColor(c);
                g.drawRect(x, y, w, h);
                String label = b.label() + " " + String.format(java.util.Locale.ROOT, "%.3f", b.score());
                int ty = Math.max(14, y - 2);
                g.drawString(label, x, ty);
            }
        } finally {
            g.dispose();
        }
        Files.createDirectories(output.toAbsolutePath().getParent());
        ImageIO.write(canvas, "jpg", output.toFile());
    }
}
