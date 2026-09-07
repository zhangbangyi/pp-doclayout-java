package com.example.doclayout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PP-DocLayoutV3 Web 应用启动类。
 *
 * <p>Spring Boot 会从这里启动内嵌 Web 服务器，并扫描同包及子包下的
 * Controller、Service 等组件。</p>
 */
@SpringBootApplication
public class DocLayoutApplication {
    public static void main(String[] args) {
        // 显式关闭 headless 模式，允许 BrowserOpener 在有桌面环境时打开默认浏览器。
        SpringApplication application = new SpringApplication(DocLayoutApplication.class);
        application.setHeadless(false);
        application.run(args);
    }
}
