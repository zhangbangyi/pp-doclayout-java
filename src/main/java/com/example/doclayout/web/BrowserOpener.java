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

@Component
@ConditionalOnProperty(name = "app.browser.auto-open", havingValue = "true", matchIfMissing = true)
public class BrowserOpener {
    private static final Logger log = LoggerFactory.getLogger(BrowserOpener.class);

    private final String contextPath;
    private volatile int port;

    public BrowserOpener(@Value("${server.servlet.context-path:}") String contextPath) {
        this.contextPath = contextPath;
    }

    @EventListener
    public void capturePort(WebServerInitializedEvent event) {
        setPort(event.getWebServer().getPort());
    }

    void setPort(int port) {
        this.port = port;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openIndexInDefaultBrowser() {
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

    URI indexUri() {
        String normalizedContextPath = contextPath.isBlank() || "/".equals(contextPath)
                ? "" : (contextPath.startsWith("/") ? contextPath : "/" + contextPath);
        return URI.create("http://localhost:" + port + normalizedContextPath + "/");
    }
}
