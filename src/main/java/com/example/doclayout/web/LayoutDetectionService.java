package com.example.doclayout.web;

import com.example.doclayout.core.OnnxLayoutDetector;
import com.example.doclayout.model.LayoutResult;
import com.example.doclayout.output.Visualizer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Web 层的识别服务，负责懒加载 ONNX 模型、执行推理并准备浏览器所需的图片数据。
 * Detector 只在第一次有效请求时创建，避免应用启动时因缺少本地模型而直接失败。
 */
@Service
public class LayoutDetectionService {
    private final Path modelPath;
    private final int intraThreads;
    private final int interThreads;
    private OnnxLayoutDetector detector;

    public LayoutDetectionService(
            @Value("${app.model-path:models/inference.onnx}") String modelPath,
            @Value("${app.intra-threads:2}") int intraThreads,
            @Value("${app.inter-threads:1}") int interThreads) {
        // 配置先保存为轻量字段，模型文件和 native 资源留到真正识别时再初始化。
        this.modelPath = Path.of(modelPath);
        this.intraThreads = intraThreads;
        this.interThreads = interThreads;
    }

    /** 执行一次识别并返回原图、标注图和结构化结果。 */
    public synchronized DetectionPayload detect(BufferedImage original, float threshold) throws Exception {
        // synchronized 同时保护懒加载和 Session 使用，避免并发请求重复创建或交叉关闭模型。
        if (threshold < 0 || threshold > 1) {
            throw new IllegalArgumentException("置信度阈值必须在 0 到 1 之间");
        }
        LayoutResult result = detector().detect(original, threshold);
        // 浏览器直接消费 Base64 data URI，不需要额外的静态文件接口。
        BufferedImage annotated = Visualizer.render(original, result);
        return new DetectionPayload(dataUri(original, "png"), dataUri(annotated, "jpeg"), result);
    }

    /** 首次调用时校验模型路径并创建可复用的推理器。 */
    private OnnxLayoutDetector detector() throws Exception {
        if (detector == null) {
            if (!Files.isRegularFile(modelPath)) {
                throw new IllegalStateException("模型文件不存在: " + modelPath.toAbsolutePath());
            }
            detector = new OnnxLayoutDetector(modelPath, intraThreads, interThreads);
        }
        return detector;
    }

    /** Spring 容器销毁时释放 ONNX Runtime 的 native 资源。 */
    @PreDestroy
    public synchronized void close() throws Exception {
        if (detector != null) {
            detector.close();
            detector = null;
        }
    }

    /** 将图片编码成前端可直接设置到 img.src 的 data URI。 */
    private static String dataUri(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (!ImageIO.write(image, format, bytes)) {
            throw new IOException("无法编码识别结果图片: " + format);
        }
        return "data:image/" + format + ";base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    /** Web 接口返回的聚合载荷。 */
    public record DetectionPayload(String originalImage, String annotatedImage, LayoutResult result) {}
}
