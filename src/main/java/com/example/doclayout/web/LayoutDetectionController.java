package com.example.doclayout.web;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 提供图片上传识别接口，并在进入推理前完成请求级校验。 */
@RestController
public class LayoutDetectionController {
    private static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;
    private final LayoutDetectionService service;

    public LayoutDetectionController(LayoutDetectionService service) {
        this.service = service;
    }

    /**
     * 接收 multipart 图片和可选置信度阈值，返回可直接被前端渲染的识别结果。
     */
    @PostMapping(value = "/api/detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public LayoutDetectionService.DetectionPayload detect(
            @RequestParam("image") MultipartFile image,
            @RequestParam(name = "threshold", defaultValue = "0.3") float threshold) throws Exception {
        // 先限制请求体积和图片格式，避免无效输入占用模型推理资源。
        if (image.isEmpty()) {
            throw new IllegalArgumentException("请选择要识别的图片");
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("图片大小不能超过 20 MB");
        }
        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(image.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("图片读取失败", e);
        }
        if (bufferedImage == null) {
            throw new IllegalArgumentException("不支持的图片格式或图片内容损坏");
        }
        // 具体阈值范围校验由 Service 统一处理，保证 CLI/Web 行为一致。
        return service.detect(bufferedImage, threshold);
    }

    /** 将参数或状态错误转换成前端可读的 400 JSON 响应。 */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorPayload> handleClientError(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorPayload(e.getMessage()));
    }

    /** 统一的错误响应结构。 */
    public record ErrorPayload(String message) {}
}
