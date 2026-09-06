package com.example.doclayout.core;

import com.example.doclayout.core.ImagePreprocessor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class ImagePreprocessorTest {
    @Test
    void preparesNchwInputInNormalizedRange() {
        BufferedImage image = new BufferedImage(2, 4, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, new Color(255, 128, 0).getRGB());

        ImagePreprocessor.Prepared prepared = ImagePreprocessor.prepare(image);

        assertEquals(2, prepared.originalWidth());
        assertEquals(4, prepared.originalHeight());
        assertEquals(3 * 800 * 800, prepared.chw().length);
        assertEquals(800f, prepared.imShape()[0]);
        assertEquals(800f, prepared.imShape()[1]);
        assertEquals(200f, prepared.scaleFactor()[0]);
        assertEquals(400f, prepared.scaleFactor()[1]);
        for (float value : prepared.chw()) {
            assertTrue(value >= 0f && value <= 1f, "pixel outside normalized range: " + value);
        }
    }
}

