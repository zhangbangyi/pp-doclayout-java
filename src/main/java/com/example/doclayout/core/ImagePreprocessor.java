package com.example.doclayout.core;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

public final class ImagePreprocessor {
    public static final int SIZE = 800;

    private ImagePreprocessor() {}

    public static Prepared prepare(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage resized = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = resized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g.drawImage(source, 0, 0, SIZE, SIZE, null);
        } finally {
            g.dispose();
        }

        float[] chw = new float[3 * SIZE * SIZE];
        int plane = SIZE * SIZE;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int rgb = resized.getRGB(x, y);
                int r = (rgb >>> 16) & 0xFF;
                int gg = (rgb >>> 8) & 0xFF;
                int b = rgb & 0xFF;
                int offset = y * SIZE + x;
                // Paddle's NormalizeImage uses scale=1/255 with mean=0 and std=1.
                chw[offset] = r / 255.0f;
                chw[plane + offset] = gg / 255.0f;
                chw[2 * plane + offset] = b / 255.0f;
            }
        }

        // The ONNX model expects NCHW float values in the 0..1 range.
        return new Prepared(
                width,
                height,
                resized,
                chw,
                new float[]{SIZE, SIZE},
                new float[]{SIZE / (float) height, SIZE / (float) width}
        );
    }

    public record Prepared(
            int originalWidth,
            int originalHeight,
            BufferedImage resized,
            float[] chw,
            float[] imShape,
            float[] scaleFactor
    ) {
        public FloatBuffer imageBuffer() { return FloatBuffer.wrap(chw); }
    }
}
