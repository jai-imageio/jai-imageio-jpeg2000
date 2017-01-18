package com.github.jaiimageio.jpeg2000;

import static org.junit.Assert.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.junit.Test;

public class GrayscaleTest {
    @Test
    public void readGrayscale() throws Exception {
        ImageReader reader = ImageIO.getImageReadersBySuffix("jp2").next();
        
        InputStream jp2 = getClass().getResourceAsStream("/grayscale-wavelet.jp2");
        assertNotNull("Could not load test resource grayscale-wavelet.jp2", jp2);
        
        ImageInputStream iis = ImageIO.createImageInputStream(jp2);
        reader.setInput(iis, true, true);
        BufferedImage image = reader.read(0);

    }
}
