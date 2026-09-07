package com.example.doclayout.core;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;

import java.nio.file.Path;

/** 输出 ONNX 模型的输入、输出名称、数据类型和形状，便于确认模型签名。 */
public final class ModelInspector {
    private ModelInspector() {}

    public static void inspect(Path modelPath) throws Exception {
        // 检查工具只在本次调用期间创建 Session，不会长期占用模型资源。
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions();
             OrtSession session = env.createSession(modelPath.toString(), options)) {

            System.out.println("========== MODEL ==========");
            System.out.println("path=" + modelPath.toAbsolutePath());
            System.out.println("ORT=" + env.getVersion());
            System.out.println("inputs=" + session.getNumInputs());
            for (var e : session.getInputInfo().entrySet()) {
                printNode("INPUT", e.getKey(), e.getValue());
            }

            System.out.println("outputs=" + session.getNumOutputs());
            for (var e : session.getOutputInfo().entrySet()) {
                printNode("OUTPUT", e.getKey(), e.getValue());
            }
        }
    }

    private static void printNode(String kind, String name, NodeInfo node) throws OrtException {
        // TensorInfo 能提供最关键的类型和维度；非 Tensor 节点则打印其通用描述。
        System.out.println(kind + " name=" + name);
        if (node.getInfo() instanceof TensorInfo tensor) {
            System.out.println("  type=" + tensor.type);
            System.out.println("  shape=" + java.util.Arrays.toString(tensor.getShape()));
        } else {
            System.out.println("  info=" + node.getInfo());
        }
    }
}
