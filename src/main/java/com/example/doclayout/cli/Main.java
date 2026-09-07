package com.example.doclayout.cli;

import com.example.doclayout.core.ModelInspector;
import com.example.doclayout.core.OnnxLayoutDetector;
import com.example.doclayout.model.LayoutBox;
import com.example.doclayout.model.LayoutResult;
import com.example.doclayout.output.HtmlWriter;
import com.example.doclayout.output.JsonWriter;
import com.example.doclayout.output.Visualizer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        Path model = Path.of(args.length > 0 ? args[0] : "models/inference.onnx");
        Path image = Path.of(args.length > 1 ? args[1] : "img.png");
        Path outputDir = Path.of(args.length > 2 ? args[2] : "output");
        float threshold = args.length > 3 ? Float.parseFloat(args[3]) : 0.5f;
        int intra = args.length > 4 ? Integer.parseInt(args[4]) : 2;
        int inter = args.length > 5 ? Integer.parseInt(args[5]) : 1;

        ModelInspector.inspect(model);

        if (!java.nio.file.Files.isRegularFile(image)) {
            throw new IllegalArgumentException("图片文件不存在: " + image.toAbsolutePath()
                    + "。请传入图片路径，例如: models/inference.onnx test.png output");
        }
        BufferedImage input;
        try {
            input = ImageIO.read(image.toFile());
        } catch (IOException e) {
            throw new IllegalArgumentException("图片读取失败: " + image.toAbsolutePath(), e);
        }
        if (input == null) {
            throw new IllegalArgumentException("不支持的图片格式或图片内容损坏: " + image.toAbsolutePath());
        }
        System.out.println("image=" + input.getWidth() + "x" + input.getHeight());

        try (OnnxLayoutDetector detector = new OnnxLayoutDetector(model, intra, inter)) {
            LayoutResult result = detector.detect(input, threshold);
            System.out.println("\n========== RESULT ==========");
            System.out.println("boxes=" + result.boxes().size());
            for (LayoutBox b : result.boxes()) {
                System.out.printf(java.util.Locale.ROOT,
                        "%s score=%.4f box=[%.1f,%.1f,%.1f,%.1f] order=%d%n",
                        b.label(), b.score(), b.x1(), b.y1(), b.x2(), b.y2(), b.order());
            }
            System.out.printf(java.util.Locale.ROOT,
                    "timing(ms): preprocess=%.2f inference=%.2f postprocess=%.2f total=%.2f%n",
                    result.preprocessNanos() / 1e6,
                    result.inferenceNanos() / 1e6,
                    result.postprocessNanos() / 1e6,
                    result.totalNanos() / 1e6);

            JsonWriter.write(result, outputDir.resolve("result.json"));
            Visualizer.draw(input, result, outputDir.resolve("result.jpg"));
            BufferedImage annotated = ImageIO.read(outputDir.resolve("result.jpg").toFile());
            HtmlWriter.write(input, annotated, result, outputDir.resolve("index.html"));
            System.out.println("json=" + outputDir.resolve("result.json").toAbsolutePath());
            System.out.println("image=" + outputDir.resolve("result.jpg").toAbsolutePath());
            System.out.println("page=" + outputDir.resolve("index.html").toAbsolutePath());
        }
    }
}
