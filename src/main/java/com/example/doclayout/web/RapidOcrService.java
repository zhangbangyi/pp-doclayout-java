package com.example.doclayout.web;

import com.benjaminwan.ocrlibrary.OcrResult;
import com.example.doclayout.model.LayoutBox;
import io.github.mymonstercat.Model;
import io.github.mymonstercat.ocr.InferenceEngine;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

/** 将版面检测框裁剪为子图，再使用 RapidOCR 提取每个区域中的文字。 */
@Service
public class RapidOcrService {
    private InferenceEngine engine;

    /** 逐框执行 OCR。无文字区域或单框失败返回空文本，不阻断整张图片检测。 */
    public synchronized List<LayoutBox> recognize(BufferedImage original, List<LayoutBox> boxes) {
        List<LayoutBox> recognized = new ArrayList<>(boxes.size());
        for (LayoutBox box : boxes) {
            try {
                recognized.add(box.withOcrText(runOcr(crop(original, box))));
            } catch (Exception ignored) {
                recognized.add(box.withOcrText(""));
            }
        }
        return recognized;
    }

    /** RapidOCR 0.0.7 接收文件路径，因此只在调用期间写入临时 PNG。 */
    private String runOcr(BufferedImage region) throws Exception {
        Path image = Files.createTempFile("pp-doclayout-region-", ".png");
        try {
            if (!ImageIO.write(region, "png", image.toFile())) {
                throw new IOException("无法编码 OCR 区域图片");
            }
            OcrResult result = engine().runOcr(image.toString());
            return result.getStrRes() == null ? "" : result.getStrRes().trim();
        } finally {
            Files.deleteIfExists(image);
        }
    }

    /** 将浮点检测框转换为合法的原图像素裁剪区域。 */
    private static BufferedImage crop(BufferedImage source, LayoutBox box) {
        int x1 = Math.max(0, (int) Math.floor(box.x1()));
        int y1 = Math.max(0, (int) Math.floor(box.y1()));
        int x2 = Math.min(source.getWidth(), (int) Math.ceil(box.x2()));
        int y2 = Math.min(source.getHeight(), (int) Math.ceil(box.y2()));
        if (x2 <= x1 || y2 <= y1) {
            throw new IllegalArgumentException("检测框没有有效的裁剪区域");
        }
        return source.getSubimage(x1, y1, x2 - x1, y2 - y1);
    }

    /** RapidOCR 的本地模型及引擎均由库延迟加载，服务生命周期内只创建一次。 */
    private InferenceEngine engine() {
        if (engine == null) {
            engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V3);
        }
        return engine;
    }
}
