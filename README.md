# PP-DocLayoutV3 Java Demo

这是一个基于 Java 17、Spring Boot 和 ONNX Runtime 的 PP-DocLayoutV3 单体 Web 应用。启动后可在浏览器上传图片进行版面识别，页面直接展示原图、检测框和识别明细。

## 项目结构

```text
src/main/java/com/example/doclayout/
  DocLayoutApplication.java         Spring Boot 启动入口
  cli/                         命令行入口（Main、BenchmarkMain）
  core/                        ONNX 推理、模型检查、图像预处理
  model/                       版面框、识别结果、标签领域对象
  output/                      JSON、框线图片和 HTML 报告输出
  web/                         Web 首页与默认浏览器启动逻辑
src/main/resources/static/     Web 首页 index.html
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
      fetch_name_0  float [N, 7]       # [class, score, x1, y1, x2, y2, model_order]
      fetch_name_1  int32 [batch]      # bbox_num, not reading order
      fetch_name_2  int32 [N, 200, 200] # mask

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

## Run Web application

启动 Web 服务：

    mvn spring-boot:run

服务启动成功后会自动使用系统默认浏览器打开上传识别页面：

    http://localhost:8098/

选择或拖拽 PNG/JPG/JPEG 图片后，设置置信度阈值并点击“开始识别”。模型在首次识别请求时加载，默认使用：

    models/inference.onnx

需要在无图形界面的环境中运行时，可关闭自动打开浏览器：

    mvn spring-boot:run -Dspring-boot.run.arguments="--app.browser.auto-open=false"

## Run command-line inference

First inspect and infer:

    mvn -q test
    mvn -q exec:java -Dexec.mainClass=com.example.doclayout.cli.Main

Or:

    mvn -q exec:java -Dexec.mainClass=com.example.doclayout.cli.Main \
      -Dexec.args="models/inference.onnx test.png output 0.3 2 1"

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

模型固定接收 800×800 输入，预处理必须与官方配置一致：直接缩放
（`keep_ratio=false`）、三次插值、`1/255` 归一化及 NCHW 排列。不能把原图尺寸直接
送入此 ONNX，也不应改为等比留白。

当前已按第七列 `model_order` 排列区域，并为正文生成连续阅读顺序；
`fetch_name_2` 的掩码尚未转换为多边形，展示仍使用矩形框。

Before SDK packaging, validate these semantics against a real inference result and PaddleOCR reference output.
