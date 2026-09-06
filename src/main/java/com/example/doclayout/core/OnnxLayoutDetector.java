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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OnnxLayoutDetector implements AutoCloseable {
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String imageInput;
    private final String imShapeInput;
    private final String scaleFactorInput;

    public OnnxLayoutDetector(java.nio.file.Path modelPath, int intraThreads, int interThreads) throws Exception {
        this.environment = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setIntraOpNumThreads(intraThreads);
        options.setInterOpNumThreads(interThreads);
        this.session = environment.createSession(modelPath.toString(), options);

        requireInput("image");
        requireInput("im_shape");
        requireInput("scale_factor");
        this.imageInput = "image";
        this.imShapeInput = "im_shape";
        this.scaleFactorInput = "scale_factor";

        validateOutputs();
    }

    private void requireInput(String name) {
        if (!session.getInputNames().contains(name)) {
            throw new IllegalStateException("模型缺少输入: " + name + ", 实际输入=" + session.getInputNames());
        }
    }

    private void validateOutputs() throws OrtException {
        if (session.getOutputNames().size() != 3) {
            throw new IllegalStateException("期望 3 个输出，实际=" + session.getOutputNames());
        }
    }

    public LayoutResult detect(BufferedImage image, float scoreThreshold) throws Exception {
        long t0 = System.nanoTime();
        ImagePreprocessor.Prepared p = ImagePreprocessor.prepare(image);
        long t1 = System.nanoTime();

        try (OnnxTensor imageTensor = OnnxTensor.createTensor(
                    environment, FloatBuffer.wrap(p.chw()), new long[]{1, 3, 800, 800});
             OnnxTensor imShapeTensor = OnnxTensor.createTensor(
                    environment, new float[][]{p.imShape()});
             OnnxTensor scaleTensor = OnnxTensor.createTensor(
                    environment, new float[][]{p.scaleFactor()})) {

            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put(imageInput, imageTensor);
            inputs.put(imShapeInput, imShapeTensor);
            inputs.put(scaleFactorInput, scaleTensor);

            long i0 = System.nanoTime();
            try (OrtSession.Result result = session.run(inputs)) {
                long i1 = System.nanoTime();
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

    private List<LayoutBox> decode(OrtSession.Result result, int originalWidth, int originalHeight,
                                   float scoreThreshold) throws OrtException {
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
        long[] orders = flattenLong(data1);
        if (a.length == 0) return List.of();

        List<LayoutBox> out = new ArrayList<>();
        for (int i = 0; i < a.length; i++) {
            float[] row = a[i];
            if (row.length != 7) {
                throw new IllegalStateException("fetch_name_0 每行应为 7 个值，实际=" + row.length);
            }

            // Official Paddle ONNX NMS output is encoded as [class_id, score, x1, y1, x2, y2, batch_id].
            int classId = Math.round(row[0]);
            float score = row[1];
            float x1 = clamp(row[2], 0, originalWidth);
            float y1 = clamp(row[3], 0, originalHeight);
            float x2 = clamp(row[4], 0, originalWidth);
            float y2 = clamp(row[5], 0, originalHeight);
            long batchIndex = Math.round(row[6]);
            if (score < scoreThreshold) continue;
            long order = i < orders.length ? orders[i] : i;
            out.add(new LayoutBox(classId, Labels.of(classId), score,
                    x1, y1, x2, y2, batchIndex, order, null));
        }
        return out;
    }

    private static long[] flattenLong(Object value) {
        if (value instanceof long[] x) return x;
        if (value instanceof long[][] x) {
            long[] out = new long[x.length * (x.length == 0 ? 0 : x[0].length)];
            int k = 0;
            for (long[] row : x) for (long z : row) out[k++] = z;
            return out;
        }
        // ONNX Runtime returns INT32 outputs as int[] (the model declares this output as INT32).
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
        return Math.max(lo, Math.min(hi, v));
    }

    @Override
    public void close() throws Exception {
        session.close();
    }
}
