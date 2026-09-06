# PP-DocLayoutV3 Java Demo

这是一个基于 Java 17 和 ONNX Runtime 的 PP-DocLayoutV3 推理示例项目，包含命令行推理、结果 JSON、框线图片和可视化 HTML 页面。

## 项目结构

```text
src/main/java/com/example/doclayout/
  cli/                         命令行入口（Main、BenchmarkMain）
  core/                        ONNX 推理、模型检查、图像预处理
  model/                       版面框、识别结果、标签领域对象
  output/                      JSON、框线图片和 HTML 报告输出
src/test/java/                 与生产包结构对应的单元测试
models/                        本地模型目录（模型文件不提交到 Git）
output/                        推理生成目录（自动生成，不提交到 Git）
test.png                       默认示例图片
```

项目遵循 Maven 标准目录布局。`target/`、`output/`、本地模型和 IDE 临时文件已由根目录 `.gitignore` 过滤。

常规推理入口为 `com.example.doclayout.cli.Main`，基准测试入口为
`com.example.doclayout.cli.BenchmarkMain`。

## Model

Copy the official `inference.onnx` that you downloaded into:

    models/inference.onnx

This project targets the actual model structure inspected from the uploaded file:

    Inputs:
      im_shape      float [1, 2] (resized image shape, 800x800)
      image         float [1, 3, 800, 800]
      scale_factor  float [1, 2]

    Outputs:
      fetch_name_0  float [N, 7]
      fetch_name_1  int32 [N]
      fetch_name_2  int32 [N, 200, 200]

The official `inference.yml` uses `NormalizeImage` with scale `1/255`, mean `0`,
and std `1`, then permutes the image to NCHW.

## Required runtime

- JDK 17+
- Maven 3.9+
- ONNX Runtime Java 1.19.2 (the Windows native runtime is verified with JDK 17)

## Test image

Put a real document screenshot/photo as:

    test.png

Recommended image types:

- scanned document
- report/document page
- screenshot containing title, text, table and image

## Run

First inspect and infer:

    mvn -q test
    mvn -q exec:java -Dexec.mainClass=com.example.doclayout.cli.Main

Or:

    mvn -q exec:java -Dexec.mainClass=com.example.doclayout.cli.Main \
      -Dexec.args="models/inference.onnx test.png output 0.5 2 1"

Outputs:

    output/result.json
    output/result.jpg
    output/index.html

Open `output/index.html` in a browser to compare the original image with the
layout boxes side by side. The page also lists each detected class, confidence,
coordinates, and inference time; all images are embedded, so the HTML file can
be opened directly without starting a web server.

## Benchmark

    mvn -q exec:java -Dexec.mainClass=com.example.doclayout.cli.BenchmarkMain \
      -Dexec.args="models/inference.onnx test.png 3 10 2 1"

The thread defaults are intentionally conservative for CPU-heavy video applications.

## Important validation order

1. Model loads.
2. Tensor shapes are exactly as expected.
3. Real image produces sensible layout boxes.
4. `result.jpg` boxes visually line up with the source image.
5. Compare the same image against PaddleOCR's `LayoutDetection(model_name="PP-DocLayoutV3", engine="onnxruntime")` JSON/image output.
6. Only after the above are correct should the code be refactored into an SDK.

## Known limitation in this test stage

`fetch_name_0` is decoded as Paddle's NMS-style seven-column result:

    [class_id, score, x1, y1, x2, y2, batch_id]

`fetch_name_1` is exposed as the per-result order array.
`fetch_name_2` is intentionally not converted to polygons yet; the visualization uses the detected bounding box.

Before SDK packaging, validate these semantics against a real inference result and PaddleOCR reference output.
