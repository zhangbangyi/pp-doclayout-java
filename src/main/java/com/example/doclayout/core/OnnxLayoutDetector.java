package com.example.doclayout.core;

import com.example.doclayout.model.Labels;
import com.example.doclayout.model.LayoutBox;
import com.example.doclayout.model.LayoutResult;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OnnxValue;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于 ONNX Runtime 的 PP-DocLayoutV3 推理器。
 *
 * <p>实例内部持有可复用的 {@link OrtSession}，因此一次加载模型后可以处理多张图片。
 * 调用方应在使用完毕后关闭实例，以释放 ONNX Runtime 的 native 资源。</p>
 */
public final class OnnxLayoutDetector implements AutoCloseable {
    /** 与 PaddleX 一致：这些区域不参与正文阅读顺序。 */
    private static final Set<String> SKIP_ORDER_LABELS = Set.of(
            "figure_title", "vision_footnote", "image", "chart", "table",
            "header", "header_image", "footer", "footer_image", "footnote", "aside_text"
    );
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String imageInput;
    private final String imShapeInput;
    private final String scaleFactorInput;

    public OnnxLayoutDetector(java.nio.file.Path modelPath, int intraThreads, int interThreads) throws Exception {
        // Environment 是 ONNX Runtime 的全局运行时；Session 则是当前模型的可复用执行计划。
        this.environment = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        // 分别控制单个算子内部和算子之间的 CPU 并行度，基准测试入口会把它们作为参数传入。
        options.setIntraOpNumThreads(intraThreads);
        options.setInterOpNumThreads(interThreads);
        this.session = environment.createSession(modelPath.toString(), options);

        // 先校验输入/输出结构，尽早发现模型文件与当前解码逻辑不兼容的问题。
        requireInput("image");
        requireInput("im_shape");
        requireInput("scale_factor");
        this.imageInput = "image";
        this.imShapeInput = "im_shape";
        this.scaleFactorInput = "scale_factor";

        validateOutputs();
    }

    private void requireInput(String name) {
        // 这里使用模型声明的名字而不是按位置取输入，避免输入顺序变化导致静默错误。
        if (!session.getInputNames().contains(name)) {
            throw new IllegalStateException("模型缺少输入: " + name + ", 实际输入=" + session.getInputNames());
        }
    }

    private void validateOutputs() throws OrtException {
        // PP-DocLayoutV3 当前约定有三个输出：检测框、顺序数组和掩码相关输出。
        if (session.getOutputNames().size() != 3) {
            throw new IllegalStateException("期望 3 个输出，实际=" + session.getOutputNames());
        }
    }

    /**
     * 对单张图片执行一次完整推理，并返回原图坐标系下的检测结果。
     *
     * @param image 待识别的原始图片
     * @param scoreThreshold 过滤低置信度检测框的阈值，范围为 0 到 1
     */
    public LayoutResult detect(BufferedImage image, float scoreThreshold) throws Exception {
        long t0 = System.nanoTime();
        // 预处理会把图片缩放到模型固定的 800x800，并生成 NCHW 浮点数组。
        ImagePreprocessor.Prepared p = ImagePreprocessor.prepare(image);
        long t1 = System.nanoTime();

        // try-with-resources 确保每次请求创建的 Tensor 在推理结束后及时释放 native 内存。
        try (OnnxTensor imageTensor = OnnxTensor.createTensor(
                    environment, FloatBuffer.wrap(p.chw()), new long[]{1, 3, 800, 800});
             OnnxTensor imShapeTensor = OnnxTensor.createTensor(
                    environment, new float[][]{p.imShape()});
             OnnxTensor scaleTensor = OnnxTensor.createTensor(
                    environment, new float[][]{p.scaleFactor()})) {

            // 输入名必须与模型签名一致；HashMap 允许按名字传入而不依赖声明顺序。
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put(imageInput, imageTensor);
            inputs.put(imShapeInput, imShapeTensor);
            inputs.put(scaleFactorInput, scaleTensor);

            long i0 = System.nanoTime();
            try (OrtSession.Result result = session.run(inputs)) {
                long i1 = System.nanoTime();
                // 解码阶段负责阈值过滤、坐标裁剪以及类别名称映射。
                List<LayoutBox> boxes = decode(result, p.originalWidth(), p.originalHeight(), scoreThreshold);
                long t2 = System.nanoTime();
                return new LayoutResult(
                        p.originalWidth(), p.originalHeight(), 800, 800,
                        t1 - t0, i1 - i0, t2 - i1,
                        boxes, session.getOutputNames().toArray(String[]::new), "PP-DocLayoutV3"
                );
            }
        }
    }

    /**
     * 将模型输出转换成领域对象。
     *
     * <p>当前 PP-DocLayoutV3 ONNX 导出物的输出约定为：
     * {@code fetch_name_0=[classId, score, x1, y1, x2, y2, modelOrder]}；
     * {@code fetch_name_1} 是每张图保留的候选框数量（bbox_num），而不是阅读顺序。</p>
     */
    private List<LayoutBox> decode(OrtSession.Result result, int originalWidth, int originalHeight,
                                   float scoreThreshold) throws OrtException {
        // 第三个输出（掩码）暂不参与边界框解码；当前可视化使用检测框结果。
        OnnxValue v0 = result.get(0);
        OnnxValue v1 = result.get(1);
        if (!(v0 instanceof OnnxTensor t0) || !(v1 instanceof OnnxTensor t1)) {
            throw new IllegalStateException("前两个输出必须是 Tensor");
        }

        Object data0 = t0.getValue();
        Object data1 = t1.getValue();
        if (!(data0 instanceof float[][] a)) {
            throw new IllegalStateException("fetch_name_0 期望 float[][]，实际=" + data0.getClass());
        }
        validateBboxCount(data1, a.length);
        if (a.length == 0) return List.of();

        List<LayoutBox> out = new ArrayList<>();
        for (float[] row : a) {
            if (row.length != 7) {
                throw new IllegalStateException("fetch_name_0 每行应为 7 个值，实际=" + row.length);
            }

            // 第七列是模型预测的阅读顺序，不是 batch id；单张图 batch 恒为 0。
            int classId = Math.round(row[0]);
            float score = row[1];
            // 模型偶尔可能产生越界坐标；裁剪后才能安全绘制并保持结果位于原图内。
            float x1 = clamp(row[2], 0, originalWidth);
            float y1 = clamp(row[3], 0, originalHeight);
            float x2 = clamp(row[4], 0, originalWidth);
            float y2 = clamp(row[5], 0, originalHeight);
            long modelOrder = Math.round(row[6]);
            // PaddleX 使用严格大于阈值的规则，并排除无效类别。
            if (score <= scoreThreshold || classId < 0) continue;
            out.add(new LayoutBox(classId, Labels.of(classId), score,
                    x1, y1, x2, y2, modelOrder, 0, "", null));
        }
        // 官方后处理会先按模型顺序排序，再为正文区域编排连续的阅读顺序。
        out.sort(Comparator.comparingLong(LayoutBox::modelOrder));
        long order = 1;
        List<LayoutBox> ordered = new ArrayList<>(out.size());
        for (LayoutBox box : out) {
            if (SKIP_ORDER_LABELS.contains(box.label())) {
                ordered.add(box.withOrder(-1));
            } else {
                ordered.add(box.withOrder(order++));
            }
        }
        return ordered;
    }

    /**
     * 校验 bbox_num 与实际二维框数组一致。该检查能防止把另一版本模型的输出误当成当前协议。
     */
    private static void validateBboxCount(Object value, int boxCount) {
        long[] counts = flattenLong(value);
        long total = 0;
        for (long count : counts) total += count;
        if (total != boxCount) {
            throw new IllegalStateException("fetch_name_1 应为 bbox_num，合计=" + total + "，实际框数=" + boxCount);
        }
    }

    /**
     * 将 ONNX Runtime 可能返回的一维或二维整数数组统一压平成 long[]。
     * 不同模型导出器会把 INT32/INT64 映射成不同 Java 数组类型，因此这里兼容常见组合。
     */
    private static long[] flattenLong(Object value) {
        if (value instanceof long[] x) return x;
        if (value instanceof long[][] x) {
            long[] out = new long[x.length * (x.length == 0 ? 0 : x[0].length)];
            int k = 0;
            for (long[] row : x) for (long z : row) out[k++] = z;
            return out;
        }
        // 模型声明为 INT32 时，ONNX Runtime 通常返回 int[]/int[][]，这里转换为 long[] 统一处理。
        if (value instanceof int[] x) {
            long[] out = new long[x.length];
            for (int i = 0; i < x.length; i++) out[i] = x[i];
            return out;
        }
        if (value instanceof int[][] x) {
            long[] out = new long[x.length * (x.length == 0 ? 0 : x[0].length)];
            int k = 0;
            for (int[] row : x) for (int z : row) out[k++] = z;
            return out;
        }
        throw new IllegalStateException("fetch_name_1 期望 int32/int64 array，实际=" + value.getClass());
    }

    private static float clamp(float v, float lo, float hi) {
        // 把数值限制在闭区间 [lo, hi]，避免绘图或下游序列化出现非法坐标。
        return Math.max(lo, Math.min(hi, v));
    }

    @Override
    public void close() throws Exception {
        // Session 持有 native 图和线程池；关闭它是释放模型资源的关键步骤。
        session.close();
    }
}
