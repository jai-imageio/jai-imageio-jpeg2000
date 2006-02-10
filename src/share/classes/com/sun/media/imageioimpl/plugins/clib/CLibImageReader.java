/*
 * $RCSfile: CLibImageReader.java,v $
 *
 * 
 * Copyright (c) 2005 Sun Microsystems, Inc. All  Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 
 * 
 * - Redistribution of source code must retain the above copyright 
 *   notice, this  list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in 
 *   the documentation and/or other materials provided with the
 *   distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of 
 * contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any 
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND 
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL 
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF 
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR 
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES. 
 * 
 * You acknowledge that this software is not designed or intended for 
 * use in the design, construction, operation or maintenance of any 
 * nuclear facility. 
 *
 * $Revision: 1.6 $
 * $Date: 2006-02-10 22:44:59 $
 * $State: Exp $
 */
package com.sun.media.imageioimpl.plugins.clib;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.InputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import com.sun.medialib.codec.jiio.Constants;
import com.sun.medialib.codec.jiio.mediaLibImage;

public abstract class CLibImageReader extends ImageReader {
    protected mediaLibImage mlibImage = null;

    /**
     * Returns true if and only if both arguments are null or
     * both are non-null and have the same length and content.
     */
    private static boolean subBandsMatch(int[] sourceBands,
                                         int[] destinationBands) {
        if(sourceBands == null && destinationBands == null) {
            return true;
        } else if(sourceBands != null && destinationBands != null) {
            if (sourceBands.length != destinationBands.length) {
                // Shouldn't happen ...
                return false;
            }
            for (int i = 0; i < sourceBands.length; i++) {
                if (sourceBands[i] != destinationBands[i]) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Creates a <code>ImageTypeSpecifier</code> corresponding to a
     * <code>mediaLibImage</code>.  The <code>mediaLibImage</code> is
     * assumed always to be either bilevel-packed (MLIB_BIT) or
     * pixel interleaved in the order ((G|I)|RGB)[A] where 'I' indicates
     * an index as for palette images.
     */
    protected static final ImageTypeSpecifier
        createImageType(mediaLibImage mlImage,
                        int bitDepth,
                        byte[] redPalette,
                        byte[] greenPalette,
                        byte[] bluePalette,
                        byte[] alphaPalette) throws IOException {

        // Get the mediaLibImage attributes.
        int mlibType = mlImage.getType();
        int mlibWidth = mlImage.getWidth();
        int mlibHeight = mlImage.getHeight();
        int mlibBands = mlImage.getChannels();
        int mlibStride = mlImage.getStride();

        // Convert mediaLib type to Java2D type.
        int dataType;
        switch(mlibType) {
        case Constants.MLIB_BIT:
        case Constants.MLIB_BYTE:
            dataType = DataBuffer.TYPE_BYTE;
            break;
        case Constants.MLIB_SHORT:
        case Constants.MLIB_USHORT:
            // Deliberately cast MLIB_SHORT to TYPE_USHORT.
            dataType = DataBuffer.TYPE_USHORT;
            break;
        default:
            throw new UnsupportedOperationException
                (I18N.getString("Generic0")+" "+mlibType);
        }

        // Set up the SampleModel.
        SampleModel sampleModel = null;
        if(mlibType == Constants.MLIB_BIT) {
            // Bilevel-packed
            sampleModel =
                new MultiPixelPackedSampleModel(dataType,
                                                mlibWidth,
                                                mlibHeight,
                                                1,
                                                mlibStride,
                                                mlImage.getBitOffset());
        } else {
            // Otherwise has to be interleaved in the order ((G|I)|RGB)[A].
            int[] bandOffsets = new int[mlibBands];
            for(int i = 0; i < mlibBands; i++) {
                bandOffsets[i] = i;
            }

            sampleModel =
                new PixelInterleavedSampleModel(dataType,
                                                mlibWidth,
                                                mlibHeight,
                                                mlibBands,
                                                mlibStride,
                                                bandOffsets);
        }

        // Set up the ColorModel.
        ColorModel colorModel = null;
        if(mlibBands == 1 &&
           redPalette   != null &&
           greenPalette != null &&
           bluePalette  != null &&
           redPalette.length == greenPalette.length &&
           redPalette.length == bluePalette.length) {

            // Indexed image.
            int paletteLength = redPalette.length;
            if(alphaPalette != null) {
                if(alphaPalette.length != paletteLength) {
                    byte[] alphaTmp = new byte[paletteLength];
                    if(alphaPalette.length > paletteLength) {
                        System.arraycopy(alphaPalette, 0,
                                         alphaTmp, 0, paletteLength);
                    } else { // alphaPalette.length < paletteLength
                        System.arraycopy(alphaPalette, 0,
                                         alphaTmp, 0, alphaPalette.length);
                        for(int i = alphaPalette.length; i < paletteLength; i++) {
                            alphaTmp[i] = (byte)255; // Opaque.
                        }
                    }
                    alphaPalette = alphaTmp;
                }

                colorModel = new IndexColorModel(bitDepth, //XXX 8
                                                 paletteLength,
                                                 redPalette,
                                                 greenPalette,
                                                 bluePalette,
                                                 alphaPalette);
            } else {
                colorModel = new IndexColorModel(bitDepth, //XXX 8
                                                 paletteLength,
                                                 redPalette,
                                                 greenPalette,
                                                 bluePalette);
            }
        } else if(mlibType == Constants.MLIB_BIT) {
            // Bilevel image with no palette: assume black-is-zero.
            byte[] cmap = new byte[] { (byte)0x00, (byte)0xFF };
            colorModel = new IndexColorModel(1, 2, cmap, cmap, cmap);
        } else {
            // RGB if more than 2 bands.
            ColorSpace colorSpace =
                ColorSpace.getInstance(mlibBands < 3 ?
                                       ColorSpace.CS_GRAY :
                                       ColorSpace.CS_sRGB);

            // All bands have same depth.
            int[] bits = new int[mlibBands];
            for(int i = 0; i < mlibBands; i++) {
                bits[i] = bitDepth;
            }

            // Alpha if band count is even.
            boolean hasAlpha = mlibBands % 2 == 0;

            colorModel =
                new ComponentColorModel(colorSpace,
                                        bits,
                                        hasAlpha,
                                        false,
                                        hasAlpha ?
                                        Transparency.TRANSLUCENT :
                                        Transparency.OPAQUE,
                                        dataType);
        }

        return new ImageTypeSpecifier(colorModel, sampleModel);
    }

    private static final void subsample(Raster src, int subX, int subY,
                                        WritableRaster dst) {
        int sx0 = src.getMinX();
        int sy0 = src.getMinY();
        int sw = src.getWidth();
        int syUB = sy0 + src.getHeight();

        int dx0 = dst.getMinX();
        int dy0 = dst.getMinY();
        int dw = dst.getWidth();

        int b = src.getSampleModel().getNumBands();
        int t = src.getSampleModel().getDataType();

        int numSubSamples = (sw + subX - 1)/subX;

        if(t == DataBuffer.TYPE_FLOAT || t == DataBuffer.TYPE_DOUBLE) {
            float[] fsamples = new float[sw];
            float[] fsubsamples = new float[numSubSamples];

            for(int k = 0; k < b; k++) {
                for(int sy = sy0, dy = dy0; sy < syUB; sy += subY, dy++) {
                    src.getSamples(sx0, sy, sw, 1, k, fsamples);
                    for(int i = 0, s = 0; i < sw; s++, i += subX) {
                        fsubsamples[s] = fsamples[i];
                    }
                    dst.setSamples(dx0, dy, dw, 1, k, fsubsamples);
                }
            }
        } else {
            int[] samples = new int[sw];
            int[] subsamples = new int[numSubSamples];

            for(int k = 0; k < b; k++) {
                for(int sy = sy0, dy = dy0; sy < syUB; sy += subY, dy++) {
                    src.getSamples(sx0, sy, sw, 1, k, samples);
                    for(int i = 0, s = 0; i < sw; s++, i += subX) {
                        subsamples[s] = samples[i];
                    }
                    dst.setSamples(dx0, dy, dw, 1, k, subsamples);
                }
            }
        }
    }                                 

    protected CLibImageReader(ImageReaderSpi originatingProvider) {
        super(originatingProvider);
    }

    /**
     * An <code>Iterator</code> over a single element.
     */
    private class SoloIterator implements Iterator {
        Object theObject;

        SoloIterator(Object o) {
            if(o == null) {
                new IllegalArgumentException
                    (I18N.getString("CLibImageReader0"));
            }
            theObject = o;
        }

        public boolean hasNext() {
            return theObject != null;
        }

        public Object next() {
            if(theObject == null) {
                throw new NoSuchElementException();
            }
            Object theNextObject = theObject;
            theObject = null;
            return theNextObject;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Decodes an image from the supplied <code>InputStream</code>.
     */
    protected abstract mediaLibImage decode(InputStream stream)
        throws IOException;

    /**
     * Returns the <code>mlibImage</code> instance variable initializing
     * it first if it is <code>null</code>.
     */
    protected synchronized mediaLibImage getImage() throws IOException {
        if(mlibImage == null) {
            if(input == null) {
                throw new IllegalStateException("input == null");
            }
            InputStream stream = null;
            if(input instanceof ImageInputStream) {
                stream = new InputStreamAdapter((ImageInputStream)input);
            } else {
                throw new IllegalArgumentException
                    ("!(input instanceof ImageInputStream)");
            }
            mlibImage = decode(stream);
        }
        return mlibImage;
    }

    public int getNumImages(boolean allowSearch) throws IOException {
        return 1;
    }

    public int getWidth(int imageIndex) throws IOException {
        if(imageIndex != 0) {
            throw new IllegalArgumentException("imageIndex != 0");
        }

        return getImage().getWidth();
    }

    public int getHeight(int imageIndex) throws IOException {
        if(imageIndex != 0) {
            throw new IllegalArgumentException("imageIndex != 0");
        }

        return getImage().getHeight();
    }

    public Iterator getImageTypes(int imageIndex) throws IOException {
        if(imageIndex != 0) {
            throw new IllegalArgumentException("imageIndex != 0");
        }

        ImageTypeSpecifier type = getRawImageType(imageIndex);

        return type != null ? new SoloIterator(type) : null;
    }

    public IIOMetadata getStreamMetadata() throws IOException {
        return null;
    }

    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        if(imageIndex != 0) {
            throw new IllegalArgumentException("imageIndex != 0");
        }

        return null;
    }

    // Override non-abstract superclass definition.
    public abstract ImageTypeSpecifier getRawImageType(int imageIndex)
        throws IOException;

    public synchronized BufferedImage read(int imageIndex,
                                           ImageReadParam param)
        throws IOException {

        processImageStarted(imageIndex);

        processImageProgress(0.0F);
        processImageProgress(0.5F);

        ImageTypeSpecifier imageType = getRawImageType(imageIndex);

        processImageProgress(0.95F);

        mediaLibImage mlImage = getImage();
        int dataOffset = mlImage.getOffset();

        SampleModel sampleModel = imageType.getSampleModel();

        DataBuffer db;
        int smType = sampleModel.getDataType();
        switch(smType) {
        case DataBuffer.TYPE_BYTE:
            byte[] byteData = mlImage.getType() == mediaLibImage.MLIB_BIT ?
                mlImage.getBitData() : mlImage.getByteData();
            db = new DataBufferByte(byteData,
                                    byteData.length - dataOffset,
                                    dataOffset);
            break;
        case DataBuffer.TYPE_USHORT:
            // Deliberately cast MLIB_SHORT to TYPE_USHORT.
            short[] shortData = mlImage.getShortData();
            if(shortData == null) {
                shortData = mlImage.getUShortData();
            }
            db = new DataBufferUShort(shortData,
                                      shortData.length - dataOffset,
                                      dataOffset);
            break;
        default:
            throw new UnsupportedOperationException
                (I18N.getString("Generic0")+" "+smType);
        }

        WritableRaster raster =
            Raster.createWritableRaster(sampleModel, db, null);

        ColorModel colorModel = imageType.getColorModel();

        BufferedImage image =
            new BufferedImage(colorModel,
                              raster,
                              colorModel.isAlphaPremultiplied(),
                              null); // XXX getDestination()?

        Rectangle destRegion = new Rectangle(image.getWidth(),
                                             image.getHeight());
        int[] destinationBands = null;
        int subX = 1;
        int subY = 1;

        if(param != null) {
            BufferedImage destination = param.getDestination();
            destinationBands = param.getDestinationBands();
            Point destinationOffset = param.getDestinationOffset();
            int[] sourceBands = param.getSourceBands();
            Rectangle sourceRegion = param.getSourceRegion();
            subX = param.getSourceXSubsampling();
            subY = param.getSourceYSubsampling();

            boolean isNominal =
                destination == null &&
                destinationBands == null &
                destinationOffset.x == 0 && destinationOffset.y == 0 &&
                sourceBands == null &&
                sourceRegion == null &&
                subX == 1 && subY == 1;

            if(!isNominal) {
                int srcWidth = image.getWidth();
                int srcHeight = image.getHeight();

                if(destination == null) {
                    destination = getDestination(param,
                                                 getImageTypes(imageIndex),
                                                 srcWidth,
                                                 srcHeight);
                }

                checkReadParamBandSettings(param,
                                           image.getSampleModel().getNumBands(),
                                           destination.getSampleModel().getNumBands());

                Rectangle srcRegion = new Rectangle();
                computeRegions(param, srcWidth, srcHeight, destination,
                               srcRegion, destRegion);

                WritableRaster dst =
                    destination.getWritableTile(0, 0).createWritableChild(
                        destRegion.x, destRegion.y,
                        destRegion.width, destRegion.height,
                        destRegion.x, destRegion.y,
                        destinationBands);

                if(subX != 1 || subY != 1) { // Subsampling
                    WritableRaster src =
                        image.getWritableTile(0, 0).createWritableChild(
                                srcRegion.x, srcRegion.y,
                                srcRegion.width, srcRegion.height,
                                srcRegion.x, srcRegion.y,
                                sourceBands);
                    subsample(src, subX, subY, dst);
                } else { // No subsampling
                    WritableRaster src =
                        image.getWritableTile(0, 0).createWritableChild(
                            srcRegion.x, srcRegion.y,
                            srcRegion.width, srcRegion.height,
                            destRegion.x, destRegion.y,
                            sourceBands);
                    dst.setRect(src);
                }

                image = destination;
            }
        }

        processImageUpdate(image,
                           destRegion.x, destRegion.y,
                           destRegion.width, destRegion.height,
                           subX, subY, destinationBands);

        processImageProgress(1.0F);
        processImageComplete();

        return image;
    }

    public void reset() {
        resetLocal();
        super.reset();
    }

    protected void resetLocal() {
        mlibImage = null;
    }

    public void setInput(Object input,
                         boolean seekForwardOnly,
                         boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);
        if (input != null) {
            if (!(input instanceof ImageInputStream)) {
                throw new IllegalArgumentException
                    ("!(input instanceof ImageInputStream)");
            }
        }
        resetLocal();
    }
}
