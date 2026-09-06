package com.example.doclayout.core;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;

import java.nio.file.Path;

public final class ModelInspector {
    private ModelInspector() {}

    public static void inspect(Path modelPath) throws Exception {
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
        System.out.println(kind + " name=" + name);
        if (node.getInfo() instanceof TensorInfo tensor) {
            System.out.println("  type=" + tensor.type);
            System.out.println("  shape=" + java.util.Arrays.toString(tensor.getShape()));
        } else {
            System.out.println("  info=" + node.getInfo());
        }
    }
}
