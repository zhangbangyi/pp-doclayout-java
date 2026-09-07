package com.example.doclayout.web;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 在 Web 服务就绪后尝试打开系统默认浏览器。
 */
@Component
@ConditionalOnProperty(name = "app.browser.auto-open", havingValue = "true", matchIfMissing = true)
public class BrowserOpener {
    private static final Logger log = LoggerFactory.getLogger(BrowserOpener.class);

    private final String contextPath;
    private volatile int port;

    public BrowserOpener(@Value("${server.servlet.context-path:}") String contextPath) {
        // 保存上下文路径，支持应用部署在根路径或自定义前缀下。
        this.contextPath = contextPath;
    }

    @EventListener
    public void capturePort(WebServerInitializedEvent event) {
        // 随机端口或配置端口都以 WebServer 实际绑定结果为准。
        setPort(event.getWebServer().getPort());
    }

    void setPort(int port) {
        this.port = port;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openIndexInDefaultBrowser() {
        // 无图形界面（例如 CI/服务器）时只记录访问地址，不尝试调用 Desktop API。
        if (GraphicsEnvironment.isHeadless() || !Desktop.isDesktopSupported()) {
            log.info("Web server is ready at {}; browser auto-open is unavailable in this environment.", indexUri());
            return;
        }

        try {
            Desktop.getDesktop().browse(indexUri());
            log.info("Opened {} in the default browser.", indexUri());
        } catch (Exception e) {
            log.warn("Web server is ready at {}, but the browser could not be opened.", indexUri(), e);
        }
    }

    /**
     * 根据端口和上下文路径拼出首页地址。
     */
    URI indexUri() {
        String normalizedContextPath = contextPath.isBlank() || "/".equals(contextPath) ? "" : (contextPath.startsWith("/") ? contextPath : "/" + contextPath);
        return URI.create("http://localhost:" + port + normalizedContextPath + "/");
    }
}
