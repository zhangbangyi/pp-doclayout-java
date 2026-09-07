package com.example.doclayout.core;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

/**
 * 将原始图片转换为 PP-DocLayoutV3 所需的模型输入。
 * 预处理约定与官方 inference.yml 对齐：缩放到 800x800、像素归一化到 0..1，
 * 并按 NCHW（通道优先）布局组织数据。
 */
public final class ImagePreprocessor {
    public static final int SIZE = 800;

    private ImagePreprocessor() {}

    public static Prepared prepare(BufferedImage source) {
        // 记录原始尺寸，后处理时要把检测框限制在这个坐标系内。
        int width = source.getWidth();
        int height = source.getHeight();
        // 该模型的官方配置为 Resize(800, 800, keep_ratio=false, interp=2)，
        // 即直接缩放为正方形并使用 OpenCV 的 INTER_CUBIC。不能改为留白等比缩放，
        // 否则会偏离训练/导出时的输入分布。
        BufferedImage resized = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = resized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
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
                // NormalizeImage 的参数为 scale=1/255、mean=0、std=1，因此只需除以 255。
                chw[offset] = r / 255.0f;
                chw[plane + offset] = gg / 255.0f;
                chw[2 * plane + offset] = b / 255.0f;
            }
        }

        // im_shape 表示送入模型的高宽；scale_factor 用于将模型坐标还原到原图比例。
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
        /** 返回可直接交给 ONNX Runtime 的浮点缓冲区视图。 */
        public FloatBuffer imageBuffer() { return FloatBuffer.wrap(chw); }
    }
}
