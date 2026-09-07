package com.example.doclayout.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BrowserOpenerTest {
    @Test
    void buildsIndexUriWithConfiguredContextPath() {
        BrowserOpener opener = new BrowserOpener("/doclayout");
        opener.setPort(18080);

        assertThat(opener.indexUri().toString()).isEqualTo("http://localhost:18080/doclayout/");
    }
}
