/*
 * $RCSfile: TIFFJPEGCompressor.java,v $
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
 * $Revision: 1.1 $
 * $Date: 2005-02-11 05:01:47 $
 * $State: Exp $
 */
package com.sun.media.imageioimpl.plugins.tiff;

import com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.TIFFCompressor;
import com.sun.media.imageio.plugins.tiff.TIFFTag;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 */
public class TIFFJPEGCompressor extends TIFFCompressor {

    private static final boolean DEBUG = false; // XXX false for release.

    // Subsampling factor for chroma bands (Cb Cr).
    static final int CHROMA_SUBSAMPLING = 2;

    // Stream metadata format.
    private static final String STREAM_METADATA_NAME =
        "javax_imageio_jpeg_stream_1.0";

    // Image metadata format.
    private static final String IMAGE_METADATA_NAME =
        "javax_imageio_jpeg_image_1.0";

    // ImageWriteParam passed in.
    private ImageWriteParam param = null;

    // ImageWriteParam for JPEG writer.
    private JPEGImageWriteParam JPEGParam = null;

    // The JPEG writer.
    private ImageWriter JPEGWriter = null;

    // Whether the codecLib native JPEG writer is being used.
    private boolean usingCodecLib;

    // Array-based output stream for non-codecLib use only.
    private IIOByteArrayOutputStream baos;

    // Whether the output has the JPEGTables field.
    private boolean hasJPEGTables = false;

    // Stream metadata equivalent to the tables-only stream in JPEGTables.
    private IIOMetadata streamMetadata = null;

    // An empty image metadata object for JPEGTables output.
    private IIOMetadata imageMetadata = null;

    /**
     * A filter which identifies the ImageReaderSpi of the core JPEG reader.
     */
    private static class JPEGSPIFilter implements ServiceRegistry.Filter {
        JPEGSPIFilter() {}

        public boolean filter(Object provider) {
            ImageReaderSpi readerSPI = (ImageReaderSpi)provider;

            if(readerSPI.getPluginClassName().startsWith("com.sun.imageio")) {
                String streamMetadataName =
                    readerSPI.getNativeStreamMetadataFormatName();
                if(streamMetadataName != null) {
                    return streamMetadataName.indexOf("jpeg_stream") != -1;
                } else {
                    return false;
                }
            }

            return false;
        }
    }

    /**
     * Retrieves the core J2SE JPEG reader.
     */
    static ImageReader getCoreJPEGReader() {
        ImageReader jpegReader = null;

        try {
            IIORegistry registry = IIORegistry.getDefaultInstance();
            Class imageReaderClass =
                Class.forName("javax.imageio.spi.ImageReaderSpi");
            Iterator readerSPIs =
                registry.getServiceProviders(imageReaderClass,
                                             new JPEGSPIFilter(),
                                             true);
            if(readerSPIs.hasNext()) {
                ImageReaderSpi jpegReaderSPI =
                    (ImageReaderSpi)readerSPIs.next();
                jpegReader = jpegReaderSPI.createReaderInstance();
            }
        } catch(Exception e) {
            // Ignore it ...
        }

        return jpegReader;
    }

    /**
     * Removes unwanted nodes from a tree.
     */
    private static void pruneNodes(Node tree) {
        String[] unwantedNodes = new String[] {
            "app0JFIF", "dht", "dqt"
        };

        IIOMetadataNode iioTree = (IIOMetadataNode)tree;
        for(int i = 0; i < unwantedNodes.length; i++) {
            NodeList list = iioTree.getElementsByTagName(unwantedNodes[i]);
            int length = list.getLength();
            for(int j = 0; j < length; j++) {
                Node node = list.item(j);
                if(node != null) {
                    if(DEBUG) {
                        System.out.println("Removing "+node.getNodeName());
                    }
                    node.getParentNode().removeChild(node);
                }
            }
        }
    }

    public TIFFJPEGCompressor(ImageWriteParam param) {
        super("JPEG", BaselineTIFFTagSet.COMPRESSION_JPEG, false);

        this.param = param;
    }

    /**
     * A <code>ByteArrayOutputStream</code> which allows writing to an
     * <code>ImageOutputStream</code>.
     */
    private class IIOByteArrayOutputStream extends ByteArrayOutputStream {
        IIOByteArrayOutputStream() {
            super();
        }

        IIOByteArrayOutputStream(int size) {
            super(size);
        }

        public synchronized void writeTo(ImageOutputStream ios)
            throws IOException {
            ios.write(buf, 0, count);
        }
    }

    /**
     * Initializes the JPEGWriter and JPEGParam instance variables.
     */
    private void initJPEGWriter(boolean preferCodecLib) {
        // Set the writer to null if it does not match preferences.
        if(this.JPEGWriter != null) {
            String writerClassName = JPEGWriter.getClass().getName();
            if((preferCodecLib &&
               !writerClassName.startsWith("com.sun.media")) ||
               (!preferCodecLib &&
                !writerClassName.startsWith("com.sun.imageio"))) {
                this.JPEGWriter = null;
            }
        }

        // Set the writer.
        if(this.JPEGWriter == null) {
            // Use the SPI which should be faster.
            // Get all JPEG writers.
            Iterator iter = ImageIO.getImageWritersByFormatName("jpeg");

            if(!iter.hasNext()) {
                // XXX The exception thrown should really be an IIOException.
                throw new IllegalStateException("No JPEG writers found!");
            }

            // Initialize writer to the first one.
            this.JPEGWriter = (ImageWriter)iter.next();

            if(!preferCodecLib) {
                // Prefer the J2SE core JPEG writer, if available.
                if(!JPEGWriter.getClass().getName().startsWith
                   ("com.sun.imageio")) {
                    while(iter.hasNext()) {
                        ImageWriter nextWriter = (ImageWriter)iter.next();
                        if(nextWriter.getClass().getName().startsWith
                           ("com.sun.imageio")) {
                            JPEGWriter = nextWriter;
                        }
                    }
                }
            }
        }

        this.usingCodecLib =
            JPEGWriter.getClass().getName().startsWith("com.sun.media");
        if(DEBUG) System.out.println("usingCodecLib = "+usingCodecLib);

        // Initialize the ImageWriteParam.
        if(this.JPEGParam == null) {
            if(param != null && param instanceof JPEGImageWriteParam) {
                JPEGParam = (JPEGImageWriteParam)param;
            } else {
                JPEGParam =
                    new JPEGImageWriteParam(writer != null ?
                                            writer.getLocale() : null);
                if(param.getCompressionMode() ==
                   ImageWriteParam.MODE_EXPLICIT) {
                    JPEGParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    JPEGParam.setCompressionQuality(param.getCompressionQuality());
                }
            }
        }
    }

    /**
     * Retrieves image metadata without app0, dht, and dqt markers.
     */
    private IIOMetadata getEmptyImageMetadata() throws IIOException {
        if(DEBUG) {
            System.out.println("getEmptyImageMetadata()");
        }
        if(imageMetadata == null) {
            TIFFImageWriter tiffWriter = (TIFFImageWriter)this.writer;

            // Get default image metadata.
            imageMetadata =
                JPEGWriter.getDefaultImageMetadata(tiffWriter.imageType,
                                                   JPEGParam);

            // Get the DOM tree.
            Node tree = imageMetadata.getAsTree(IMAGE_METADATA_NAME);

            // Remove the app0, dht, and dqt markers.
            pruneNodes(tree);

            // Set the DOM back into the metadata.
            try {
                imageMetadata.setFromTree(IMAGE_METADATA_NAME, tree);
            } catch(IIOInvalidTreeException e) {
                // XXX This should really be a warning that image data
                // segments will be written with tables despite the
                // present of JPEGTables field.
                throw new IIOException("Cannot empty image metadata!", e);
            }
        }

        return imageMetadata;
    }

    /**
     * Sets the value of the <code>metadata</code> field.
     *
     * <p>The implementation in this class also adds the TIFF fields
     * JPEGTables, YCbCrSubSampling, YCbCrPositioning, and
     * ReferenceBlackWhite superseding any prior settings of those
     * fields.</p>
     *
     * @param metadata the <code>IIOMetadata</code> object for the
     * image being written.
     *
     * @see #getMetadata()
     */
    public void setMetadata(IIOMetadata metadata) {
        super.setMetadata(metadata);

        if (metadata instanceof TIFFImageMetadata) {
            TIFFImageMetadata tim = (TIFFImageMetadata)metadata;
            TIFFIFD rootIFD = tim.getRootIFD();
            BaselineTIFFTagSet base = BaselineTIFFTagSet.getInstance();

            TIFFField f =
                tim.getTIFFField(BaselineTIFFTagSet.TAG_SAMPLES_PER_PIXEL);
            int numBands = f.getAsInt(0);

            if(numBands == 1) {
                // Remove YCbCr fields not relevant for grayscale.

                rootIFD.removeTIFFField(BaselineTIFFTagSet.TAG_Y_CB_CR_SUBSAMPLING);
                rootIFD.removeTIFFField(BaselineTIFFTagSet.TAG_Y_CB_CR_POSITIONING);
                rootIFD.removeTIFFField(BaselineTIFFTagSet.TAG_REFERENCE_BLACK_WHITE);
            } else { // numBands == 3
                // Replace YCbCr fields.

                // YCbCrSubSampling
                TIFFField YCbCrSubSamplingField = new TIFFField
                    (base.getTag(BaselineTIFFTagSet.TAG_Y_CB_CR_SUBSAMPLING),
                     TIFFTag.TIFF_SHORT, 2, new char[] {2, 2});
                rootIFD.addTIFFField(YCbCrSubSamplingField);

                // YCbCrPositioning
                TIFFField YCbCrPositioningField = new TIFFField
                    (base.getTag(BaselineTIFFTagSet.TAG_Y_CB_CR_POSITIONING),
                     TIFFTag.TIFF_SHORT, 1,
                     new char[]
                        {BaselineTIFFTagSet.Y_CB_CR_POSITIONING_CENTERED});
                rootIFD.addTIFFField(YCbCrPositioningField);

                // ReferenceBlackWhite
                TIFFField referenceBlackWhiteField = new TIFFField
                    (base.getTag(BaselineTIFFTagSet.TAG_REFERENCE_BLACK_WHITE),
                     TIFFTag.TIFF_RATIONAL, 6,
                     new long[][] { // no headroon/footroom
                         {0, 1}, {255, 1},
                         {128, 1}, {255, 1},
                         {128, 1}, {255, 1}
                     });
                rootIFD.addTIFFField(referenceBlackWhiteField);
            }

            // JPEGTables field is written if and only if one is
            // already present in the metadata. If one is present
            // and has either zero length or does not represent a
            // valid tables-only stream, then a JPEGTables field
            // will be written initialized to the standard tables-
            // only stream written by the JPEG writer.

            // Retrieve the JPEGTables field.
            TIFFField JPEGTablesField =
                tim.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_TABLES);

            // Initialize the core JPEG writer
            if(JPEGTablesField != null) {
                initJPEGWriter(false);
            }

            // Write JPEGTables field if core writer was available.
            if(JPEGTablesField != null &&
               JPEGWriter.getClass().getName().startsWith("com.sun.imageio")) {
                if(DEBUG) System.out.println("Has JPEGTables ...");

                // Set the JPEGTables flag.
                this.hasJPEGTables = true;

                //Branch based on field value count.
                if(JPEGTablesField.getCount() > 0) {
                    if(DEBUG) System.out.println("JPEGTables > 0");

                    // Derive the stream metadata from the field.

                    // Get the field values.
                    byte[] tables = JPEGTablesField.getAsBytes();

                    // Create an input stream for the tables.
                    ByteArrayInputStream bais =
                        new ByteArrayInputStream(tables);
                    MemoryCacheImageInputStream iis =
                        new MemoryCacheImageInputStream(bais);

                    // Read the tables stream using the core reader.
                    ImageReader jpegReader = getCoreJPEGReader();
                    jpegReader.setInput(iis);

                    // Initialize the stream metadata object.
                    try {
                        streamMetadata = jpegReader.getStreamMetadata();
                    } catch(Exception e) {
                        // Fall back to default tables.
                        streamMetadata = null;
                    } finally {
                        jpegReader.reset();
                    }
                    if(DEBUG) System.out.println(streamMetadata);
                }

                if(streamMetadata == null) {
                    if(DEBUG) System.out.println("JPEGTables == 0");

                    // Derive the field from default stream metadata.

                    // Get default stream metadata.
                    streamMetadata =
                        JPEGWriter.getDefaultStreamMetadata(JPEGParam);

                    // Create an output stream for the tables.
                    ByteArrayOutputStream tableByteStream =
                        new ByteArrayOutputStream();
                    MemoryCacheImageOutputStream tableStream =
                        new MemoryCacheImageOutputStream(tableByteStream);

                    // Write a tables-only stream.
                    JPEGWriter.setOutput(tableStream);
                    try {
                        JPEGWriter.prepareWriteSequence(streamMetadata);
                        tableStream.flush();
                        JPEGWriter.endWriteSequence();

                        // Get the tables-only stream content.
                        byte[] tables = tableByteStream.toByteArray();
                        if(DEBUG) System.out.println("tables.length = "+
                                                     tables.length);

                        // Add the JPEGTables field.
                        JPEGTablesField = new TIFFField
                            (base.getTag(BaselineTIFFTagSet.TAG_JPEG_TABLES),
                             TIFFTag.TIFF_UNDEFINED,
                             tables.length,
                             tables);
                        rootIFD.addTIFFField(JPEGTablesField);
                    } catch(Exception e) {
                        // Do not write JPEGTables field.
                        rootIFD.removeTIFFField(BaselineTIFFTagSet.TAG_JPEG_TABLES);
                        this.hasJPEGTables = false;
                    }
                }
            } else { // Do not write JPEGTables field.
                // Remove any field present.
                rootIFD.removeTIFFField(BaselineTIFFTagSet.TAG_JPEG_TABLES);

                // Initialize the writer preferring codecLib.
                initJPEGWriter(true);
            }
        }
    }

    public int encode(byte[] b, int off,
                      int width, int height,
                      int[] bitsPerSample,
                      int scanlineStride) throws IOException {
        if (!((bitsPerSample.length == 3 &&
               bitsPerSample[0] == 8 &&
               bitsPerSample[1] == 8 &&
               bitsPerSample[2] == 8) ||
              (bitsPerSample.length == 1 &&
               bitsPerSample[0] == 8))) {
            throw new IIOException
                ("Can only JPEG compress 8- and 24-bit images!");
        }

        // Set the stream.
        ImageOutputStream ios;
        long initialStreamPosition; // used only if usingCodecLib == true
        if(usingCodecLib) {
            ios = stream;
            initialStreamPosition = stream.getStreamPosition();
        } else {
            // If not using codecLib then the stream has to be wrapped
            // as the core Java Image I/O JPEG ImageWriter flushes the
            // stream at the end of each write() and this causes problems
            // for the TIFF writer.
            if(baos == null) {
                baos = new IIOByteArrayOutputStream();
            } else {
                baos.reset();
            }
            ios = new MemoryCacheImageOutputStream(baos);
            initialStreamPosition = 0L;
        }
        JPEGWriter.setOutput(ios);

        // Create a DataBuffer.
        DataBufferByte dbb;
        if(off == 0 || usingCodecLib) {
            dbb = new DataBufferByte(b, b.length);
        } else {
            //
            // Workaround for bug in core Java Image I/O JPEG
            // ImageWriter which cannot handle non-zero offsets.
            //
            int bytesPerSegment = scanlineStride*height;
            byte[] btmp = new byte[bytesPerSegment];
            System.arraycopy(b, off, btmp, 0, bytesPerSegment);
            dbb = new DataBufferByte(btmp, bytesPerSegment);
            off = 0;
        }

        // Set up the ColorSpace.
        int[] offsets;
        ColorSpace cs;
        if(bitsPerSample.length == 3) {
            offsets = new int[] { off, off + 1, off + 2 };
            cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        } else {
            offsets = new int[] { off };
            cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        }

        // Create the ColorModel.
        ColorModel cm = new ComponentColorModel(cs,
                                                false,
                                                false,
                                                Transparency.OPAQUE,
                                                DataBuffer.TYPE_BYTE);

        // Create the SampleModel.
        SampleModel sm =
            new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                                            width, height,
                                            bitsPerSample.length,
                                            scanlineStride,
                                            offsets);

        // Create the WritableRaster.
        WritableRaster wras =
            Raster.createWritableRaster(sm, dbb, new Point(0, 0));

        // Create the BufferedImage.
        BufferedImage bi = new BufferedImage(cm, wras, false, null);

        // Compress the image into the output stream.
        int compDataLength;
        if(usingCodecLib) {
            // Write complete JPEG stream
            JPEGWriter.write(null, new IIOImage(bi, null, null), JPEGParam);
            compDataLength =
                (int)(stream.getStreamPosition() - initialStreamPosition);
        } else {
            if(hasJPEGTables) {
                // Write abbreviated JPEG stream

                // First write the tables-only data.
                JPEGWriter.prepareWriteSequence(streamMetadata);
                ios.flush();

                // Rewind to the beginning of the byte array.
                baos.reset();

                // Write the abbreviated image data.
                IIOMetadata emptyMetadata = getEmptyImageMetadata();
                IIOImage image = new IIOImage(bi, null, emptyMetadata);
                JPEGWriter.writeToSequence(image, JPEGParam);
                JPEGWriter.endWriteSequence();
            } else {
                // Write complete JPEG stream
                JPEGWriter.write(null,
                                 new IIOImage(bi, null, null),
                                 JPEGParam);
            }

            compDataLength = baos.size();
            baos.writeTo(stream);
            baos.reset();
        }

        return compDataLength;
    }

    protected void finalize() throws Throwable {
        super.finalize();
        if(JPEGWriter != null) {
            JPEGWriter.dispose();
        }
    }
}
