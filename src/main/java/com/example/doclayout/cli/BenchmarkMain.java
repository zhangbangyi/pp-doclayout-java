package com.example.doclayout.cli;

import com.example.doclayout.core.OnnxLayoutDetector;
import com.example.doclayout.model.LayoutResult;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public final class BenchmarkMain {
    private BenchmarkMain() {}

    public static void main(String[] args) throws Exception {
        Path model = Path.of(args.length > 0 ? args[0] : "models/inference.onnx");
        Path image = Path.of(args.length > 1 ? args[1] : "test.png");
        int warmup = args.length > 2 ? Integer.parseInt(args[2]) : 3;
        int runs = args.length > 3 ? Integer.parseInt(args[3]) : 10;
        int intra = args.length > 4 ? Integer.parseInt(args[4]) : 2;
        int inter = args.length > 5 ? Integer.parseInt(args[5]) : 1;

        if (!java.nio.file.Files.isRegularFile(image)) {
            throw new IllegalArgumentException("图片文件不存在: " + image.toAbsolutePath()
                    + "。请传入图片路径，例如: models/inference.onnx test.png 3 10 2 1");
        }
        BufferedImage img;
        try {
            img = ImageIO.read(image.toFile());
        } catch (IOException e) {
            throw new IllegalArgumentException("图片读取失败: " + image.toAbsolutePath(), e);
        }
        if (img == null) {
            throw new IllegalArgumentException("不支持的图片格式或图片内容损坏: " + image.toAbsolutePath());
        }
        try (OnnxLayoutDetector detector = new OnnxLayoutDetector(model, intra, inter)) {
            for (int i = 0; i < warmup; i++) detector.detect(img, 0.5f);
            long sum = 0;
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            for (int i = 0; i < runs; i++) {
                LayoutResult r = detector.detect(img, 0.5f);
                long ns = r.totalNanos();
                sum += ns;
                min = Math.min(min, ns);
                max = Math.max(max, ns);
            }
            System.out.printf(java.util.Locale.ROOT,
                    "runs=%d avgMs=%.2f minMs=%.2f maxMs=%.2f%n",
                    runs, sum / (runs * 1e6), min / 1e6, max / 1e6);
        }
    }
}
