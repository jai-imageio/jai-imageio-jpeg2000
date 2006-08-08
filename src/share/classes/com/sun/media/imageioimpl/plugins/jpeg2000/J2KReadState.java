/*
 * $RCSfile: J2KReadState.java,v $
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
 * $Revision: 1.2 $
 * $Date: 2006-08-08 00:31:47 $
 * $State: Exp $
 */
package com.sun.media.imageioimpl.plugins.jpeg2000;

import javax.imageio.IIOException;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.util.Iterator;

import jj2000.j2k.quantization.dequantizer.*;
import jj2000.j2k.wavelet.synthesis.*;
import jj2000.j2k.image.invcomptransf.*;
import jj2000.j2k.fileformat.reader.*;
import jj2000.j2k.codestream.reader.*;
import jj2000.j2k.entropy.decoder.*;
import jj2000.j2k.codestream.*;
import jj2000.j2k.decoder.*;
import jj2000.j2k.image.*;
import jj2000.j2k.util.*;
import jj2000.j2k.roi.*;
import jj2000.j2k.io.*;
import jj2000.j2k.*;

import com.sun.media.imageioimpl.common.ImageUtil;

public class J2KReadState {
    /** The input stream we read from */
    private ImageInputStream iis = null;

    private FileFormatReader ff;
    private HeaderDecoder hd;
    private RandomAccessIO in;
    private BitstreamReaderAgent breader;
    private EntropyDecoder entdec;
    private ROIDeScaler roids;
    private Dequantizer deq;
    private InverseWT invWT;
    private InvCompTransf ictransf;
    private ImgDataConverter converter,converter2;
    private DecoderSpecs decSpec = null;
    private J2KImageReadParamJava j2krparam = null;
    private int[] destinationBands = null;
    private int[] sourceBands = null;

    private int[] levelShift = null;        // level shift for each component
    private int[] minValues = null;         // The min values
    private int[] maxValues = null;         // The max values
    private int[] fracBits = null;          // fractional bits for each component
    private DataBlkInt[] dataBlocks = null; // data-blocks to request data from src

    private int[] bandOffsets = null;
    private int maxDepth = 0;
    private boolean isSigned = false;

    private ColorModel colorModel = null;
    private SampleModel sampleModel = null;
    private int nComp = 0;
    private int tileWidth = 0;
    private int tileHeight = 0;

    /** Source to destination transform */
    private int scaleX, scaleY, xOffset, yOffset;
    private Rectangle destinationRegion = null;
    private Point sourceOrigin;

    /** Tile grid offsets of the source, also used for destination. */
    private int tileXOffset, tileYOffset;

    private int width;
    private int height;
    private int[] pixbuf = null;
    private byte[] bytebuf = null;
    private int[] channelMap = null;

    private boolean noTransform = true;

    private J2KMetadata metadata;

    private BufferedImage destImage;

    /** Cache the <code>J2KImageReader</code> which creates this object.  This
     *  variable is used to monitor the abortion.
     */
    private J2KImageReader reader;

    /** Constructs <code>J2KReadState</code>.
     *  @param iis The input stream.
     *  @param param The reading parameters.
     *  @param metadata The <code>J2KMetadata</code> to cache the metadata read
     *                  from the input stream.
     *  @param reader The <code>J2KImageReader</code> which holds this state.
     *                It is necessary for processing abortion.
     *  @throw IllegalArgumentException If the provided <code>iis</code>,
     *          <code>param</code> or <code>metadata</code> is <code>null</code>.
     */
    public J2KReadState(ImageInputStream iis,
                        J2KImageReadParamJava param,
                        J2KMetadata metadata,
                        J2KImageReader reader) {
        if (iis == null || param == null || metadata == null)
            throw new IllegalArgumentException(I18N.getString("J2KReadState0"));

        this.iis = iis;
        this.j2krparam = param;
        this.metadata = metadata;
        this.reader = reader;

        initializeRead(0, param, metadata);
    }

    /** Constructs <code>J2KReadState</code>.
     *  @param iis The input stream.
     *  @param param The reading parameters.
     *  @param reader The <code>J2KImageReader</code> which holds this state.
     *                It is necessary for processing abortion.
     *  @throw IllegalArgumentException If the provided <code>iis</code>,
     *          or <code>param</code> is <code>null</code>.
     */
    public J2KReadState(ImageInputStream iis,
                        J2KImageReadParamJava param,
                        J2KImageReader reader) {
        if (iis == null || param == null)
            throw new IllegalArgumentException(I18N.getString("J2KReadState0"));

        this.iis = iis;
        this.j2krparam = param;
        this.reader = reader;
        initializeRead(0, param, null);
    }

    public int getWidth() throws IOException {
        return width;
    }

    public int getHeight() throws IOException {
        return height;
    }

    public HeaderDecoder getHeader() {
        return hd;
    }

    public Raster getTile(int tileX, int tileY,
                          WritableRaster raster) throws IOException {
        Point nT = ictransf.getNumTiles(null);

        if (noTransform) {
            if (tileX >= nT.x || tileY >= nT.y)
                throw new IllegalArgumentException(I18N.getString("J2KImageReader0"));

            ictransf.setTile(tileX, tileY);

                // The offset of the active tiles is the same for all components,
                // since we don't support different component dimensions.
            int tOffx = ictransf.getCompULX(0) -
                        (ictransf.getImgULX() + ictransf.getCompSubsX(0) - 1) /
                        ictransf.getCompSubsX(0) + destinationRegion.x;
            int tOffy = ictransf.getCompULY(0)-
                        (ictransf.getImgULY() + ictransf.getCompSubsY(0) - 1) /
                        ictransf.getCompSubsY(0) + destinationRegion.y;

            if (raster == null)
                raster = Raster.createWritableRaster(sampleModel,
                                                     new Point(tOffx, tOffy));

            int numBands = sampleModel.getNumBands();
            int cTileHeight = ictransf.getTileHeight();
            int cTileWidth = ictransf.getTileWidth();

            if (tOffx + cTileWidth >=
                destinationRegion.width + destinationRegion.x)
                cTileWidth =
                    destinationRegion.width + destinationRegion.x - tOffx;

            if (tOffy + cTileHeight >=
                destinationRegion.height + destinationRegion.y)
                cTileHeight =
                    destinationRegion.height + destinationRegion.y - tOffy;

            //create the line buffer for pixel data if it is not large enough
            // or null
            if (pixbuf == null || pixbuf.length < cTileWidth * numBands)
                pixbuf = new int[cTileWidth * numBands];
            boolean prog = false;

            // Deliver in lines to reduce memory usage
            for (int l=0; l < cTileHeight;l++) {
                if (reader.getAbortRequest())
                    break;

                // Request line data
                for (int i = 0; i < numBands; i++) {
                    if (reader.getAbortRequest())
                        break;
                    DataBlkInt db = dataBlocks[i];
                    db.ulx = 0;
                    db.uly = l;
                    db.w = cTileWidth;
                    db.h = 1;
                    ictransf.getInternCompData(db, channelMap[sourceBands[i]]);
                    prog = prog || db.progressive;

                    int[] data = db.data;
                    int k1 = db.offset + cTileWidth - 1;

                    int fracBit = fracBits[i];
                    int lS = levelShift[i];
                    int min = minValues[i];
                    int max = maxValues[i];

                    if (ImageUtil.isBinary(sampleModel)) {
                        // Force min max to 0 and 1.
                        min = 0;
                        max = 1;
                        if (bytebuf == null || bytebuf.length < cTileWidth * numBands)
                            bytebuf = new byte[cTileWidth * numBands];
                        for (int j = cTileWidth - 1;
                             j >= 0; j--) {
                            int tmp = (data[k1--] >> fracBit) + lS;
                            bytebuf[j] =
                                (byte)((tmp < min) ? min : 
				       ((tmp > max) ? max : tmp));
                        }

                        ImageUtil.setUnpackedBinaryData(bytebuf,
                                                        raster,
                                                        new Rectangle(tOffx,
                                                                      tOffy + l,
                                                                      cTileWidth,
                                                                      1));
                    } else {

                        for (int j = cTileWidth - 1;
                             j >= 0; j--) {
                            int tmp = (data[k1--] >> fracBit) + lS;
                            pixbuf[j] = (tmp < min) ? min : 
				((tmp > max) ? max : tmp);
                        }

                        raster.setSamples(tOffx,
                                          tOffy + l,
                                          cTileWidth,
                                          1,
                                          destinationBands[i],
                                          pixbuf);
                    }
                }
            }
        } else {
            readSubsampledRaster(raster);
        }

        return raster;
    }

    public Rectangle getDestinationRegion() {
        return destinationRegion;
    }

    public BufferedImage readBufferedImage() throws IOException {
        colorModel = getColorModel();
        sampleModel = getSampleModel();
        WritableRaster raster = null;
        BufferedImage image = j2krparam.getDestination();

        int x = destinationRegion.x;
        int y = destinationRegion.y;
        destinationRegion.setLocation(j2krparam.getDestinationOffset());
        if (image == null) {
            // If the destination type is specified, use the color model of it.
            ImageTypeSpecifier type = j2krparam.getDestinationType();
            if (type != null)
                colorModel = type.getColorModel();

            raster = Raster.createWritableRaster(
                sampleModel.createCompatibleSampleModel(destinationRegion.x +
                                                        destinationRegion.width,
                                                        destinationRegion.y +
                                                        destinationRegion.height),
                new Point(0, 0));
            image = new BufferedImage(colorModel, raster,
                                      colorModel.isAlphaPremultiplied(),
                                      new Hashtable());
        } else
            raster = image.getWritableTile(0, 0);

        destImage = image;
        readSubsampledRaster(raster);
        destinationRegion.setLocation(x, y);
        destImage = null;
        return image;
    }

    public Raster readAsRaster() throws IOException {
        BufferedImage image = j2krparam.getDestination();
        WritableRaster raster = null;

        if (image == null) {
            raster = Raster.createWritableRaster(
                sampleModel.createCompatibleSampleModel(destinationRegion.x +
                                                        destinationRegion.width,
                                                        destinationRegion.y +
                                                        destinationRegion.height),
                new Point(0, 0));
        } else
            raster = image.getWritableTile(0, 0);

        readSubsampledRaster(raster);
        return raster;
    }

    private void initializeRead(int imageIndex, J2KImageReadParamJava param,
                                J2KMetadata metadata) {
        try {
            iis.mark();
            in = new IISRandomAccessIO(iis);

            // **** File Format ****
            // If the codestream is wrapped in the jp2 fileformat, Read the
            // file format wrapper
            ff = new FileFormatReader(in, metadata);
            ff.readFileFormat();
            in.seek(ff.getFirstCodeStreamPos());

	    HeaderInfo hi = new HeaderInfo();
            try{
                hd = new HeaderDecoder(in, j2krparam, hi);
            } catch(EOFException e){
                throw new RuntimeException(I18N.getString("J2KReadState2"));
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }

            this.width = hd.getImgWidth();
            this.height = hd.getImgHeight();

            Rectangle sourceRegion = param.getSourceRegion();
            sourceOrigin = new Point();
            sourceRegion =
                new Rectangle(hd.getImgULX(), hd.getImgULY(),
                              this.width, this.height);
            destinationRegion = (Rectangle)sourceRegion.clone();

            J2KImageReader.computeRegionsWrapper(param,
                                                 false,
                                                 this.width,
                                                 this.height,
                                                 param.getDestination(),
                                                 sourceRegion,
                                                 destinationRegion);

            sourceOrigin = new Point(sourceRegion.x, sourceRegion.y);
            scaleX = param.getSourceXSubsampling();
            scaleY = param.getSourceYSubsampling();
            xOffset = param.getSubsamplingXOffset();
            yOffset = param.getSubsamplingYOffset();

            Point tileOffset = hd.getTilingOrigin(null);
            tileXOffset = tileOffset.x;
            tileYOffset = tileOffset.y;

            this.width = destinationRegion.width;
            this.height = destinationRegion.height;

            this.tileWidth = hd.getNomTileWidth();
            this.tileHeight = hd.getNomTileHeight();

            if (!destinationRegion.equals(sourceRegion))
                noTransform = false;

            // **** Header decoder ****
            // Instantiate header decoder and read main header
            decSpec = hd.getDecoderSpecs();

            // **** Instantiate decoding chain ****
            // Get demixed bitdepths
            nComp = hd.getNumComps();

            // if the subsample rate for components are not consistent
            boolean compConsistent = true;
            int sx = hd.getCompSubsX(0);
            int sy = hd.getCompSubsY(0);
            for (int i = 1; i < nComp; i++) {
                if (sx != hd.getCompSubsX(i) || sy != hd.getCompSubsY(i))
                    throw new RuntimeException(I18N.getString("J2KReadState12"));
            }

            int[] depth = new int[nComp];
            for (int i=0; i<nComp;i++)
                depth[i] = hd.getOriginalBitDepth(i);

            //Get channel mapping
            ChannelDefinitionBox cdb = null;
	    if (metadata != null)
                cdb = (ChannelDefinitionBox)metadata.getElement("JPEG2000ChannelDefinitionBox");

            channelMap = new int[nComp];
            if (cdb != null &&
                metadata.getElement("JPEG2000PaletteBox") == null) {
                short[] assoc = cdb.getAssociation();
                short[] types = cdb.getTypes();
                short[] channels = cdb.getChannel();

                for (int i = 0; i < types.length; i++)
                    if (types[i] == 0)
                        channelMap[channels[i]] = assoc[i] - 1;
                    else if (types[i] == 1 || types[i] == 2)
                        channelMap[channels[i]] = channels[i];
            } else {
                for (int i = 0; i < nComp; i++)
                    channelMap[i] = i;
            }

            // **** Bitstream reader ****
            try {
                breader =
                    BitstreamReaderAgent.createInstance(in, hd,
                                                        j2krparam, decSpec,
							false, hi);
            } catch (IOException e) {
                throw new RuntimeException(I18N.getString("J2KReadState3") + " " +
                          ((e.getMessage() != null) ?
                            (":\n"+e.getMessage()) : ""));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(I18N.getString("J2KReadState4") + " " +
                               ((e.getMessage() != null) ?
                               (":\n"+e.getMessage()) : ""));
            }

            // **** Entropy decoder ****
            try {
                entdec = hd.createEntropyDecoder(breader, j2krparam);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(I18N.getString("J2KReadState5") + " " +
                              ((e.getMessage() != null) ?
                               (":\n"+e.getMessage()) : ""));
            }

            // **** ROI de-scaler ****
            try {
                roids = hd.createROIDeScaler(entdec, j2krparam, decSpec);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(I18N.getString("J2KReadState6") + " " +
                              ((e.getMessage() != null) ?
                               (":\n"+e.getMessage()) : ""));
            }


            // **** Dequantizer ****
            try {
                deq = hd.createDequantizer(roids, depth, decSpec);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(I18N.getString("J2KReadState7") + " " +
                              ((e.getMessage() != null) ?
                               (":\n"+e.getMessage()) : ""));
            }

            // **** Inverse wavelet transform ***
            try {
                // full page inverse wavelet transform
                invWT = InverseWT.createInstance(deq,decSpec);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(I18N.getString("J2KReadState8") + " " +
                              ((e.getMessage() != null) ?
                               (":\n"+e.getMessage()) : ""));
            }

            int res = breader.getImgRes();
            int mrl = decSpec.dls.getMin();
            invWT.setImgResLevel(res);

            // **** Data converter **** (after inverse transform module)
            converter = new ImgDataConverter(invWT,0);

            // **** Inverse component transformation ****
            ictransf = new InvCompTransf(converter, decSpec, depth);

            // If the destination band is set used it
            sourceBands = j2krparam.getSourceBands();

            if (sourceBands == null) {
                sourceBands = new int[nComp];
                for (int i = 0; i < nComp; i++)
                    sourceBands[i] = i;
            }

            nComp = sourceBands.length;

            destinationBands = j2krparam.getDestinationBands();
            if (destinationBands == null) {
                destinationBands = new int[nComp];
                for (int i = 0; i < nComp; i++)
                    destinationBands[i] = i;
            }

            J2KImageReader.checkReadParamBandSettingsWrapper(param,
                                                             hd.getNumComps(),
                                                             destinationBands.length);

            levelShift = new int[nComp];
            minValues = new int[nComp];
            maxValues = new int[nComp];
            fracBits = new int[nComp];
            dataBlocks = new DataBlkInt[nComp];

            depth = new int[nComp];
            bandOffsets = new int[nComp];
            maxDepth = 0;
            isSigned = false;
            for (int i=0; i<nComp;i++) {
                depth[i] = hd.getOriginalBitDepth(sourceBands[i]);
                if (depth[i] > maxDepth)
                    maxDepth = depth[i];
                dataBlocks[i] = new DataBlkInt();

                //XXX: may need to change if ChannelDefinition is used to
                // define the color channels, such as BGR order
                bandOffsets[i] = i;
                if (hd.isOriginalSigned(sourceBands[i]))
                    isSigned = true;
                else {
                    levelShift[i] =
                        1<<(ictransf.getNomRangeBits(sourceBands[i])-1);
		}

		// Get the number of bits in the image, and decide what the max
		// value should be, depending on whether it is signed or not
		int nomRangeBits = ictransf.getNomRangeBits(sourceBands[i]);
		maxValues[i] = (1 << (isSigned == true ? (nomRangeBits-1) :
							  nomRangeBits)) - 1;
		minValues[i] = isSigned ? -(maxValues[i]+1) : 0;

                fracBits[i] = ictransf.getFixedPoint(sourceBands[i]);
            }

            iis.reset();
        } catch (IllegalArgumentException e){
	    throw new RuntimeException(e.getMessage(), e);
	} catch (Error e) {
            if(e.getMessage()!=null)
                throw new RuntimeException(e.getMessage(), e);
            else {
                throw new RuntimeException(I18N.getString("J2KReadState9"), e);
            }
        } catch (RuntimeException e) {
            if(e.getMessage()!=null)
                throw new RuntimeException(I18N.getString("J2KReadState10") + " " +
                      e.getMessage(), e);
            else {
                throw new RuntimeException(I18N.getString("J2KReadState10"), e);
            }
        } catch (Throwable e) {
            throw new RuntimeException(I18N.getString("J2KReadState10"), e);
        }
    }

    private Raster readSubsampledRaster(WritableRaster raster) throws IOException {
        if (raster == null)
            raster = Raster.createWritableRaster(
                sampleModel.createCompatibleSampleModel(destinationRegion.x +
                                                        destinationRegion.width,
                                                        destinationRegion.y +
                                                        destinationRegion.height),
                new Point(destinationRegion.x, destinationRegion.y));

        int pixbuf[] = null;                  // line buffer for pixel data
        boolean prog = false;                  // Flag for progressive data
        Point nT = ictransf.getNumTiles(null);
        int numBands = sourceBands.length;

        Rectangle destRect = raster.getBounds().intersection(destinationRegion);

        int offx = destinationRegion.x;
        int offy = destinationRegion.y;

        int sourceSX = (destRect.x - offx) * scaleX + sourceOrigin.x;
        int sourceSY = (destRect.y - offy) * scaleY + sourceOrigin.y;
        int sourceEX = (destRect.width - 1)* scaleX + sourceSX;
        int sourceEY = (destRect.height - 1) * scaleY + sourceSY;

        int startXTile = (sourceSX - tileXOffset) / tileWidth;
        int startYTile = (sourceSY - tileYOffset) / tileHeight;
        int endXTile = (sourceEX - tileXOffset) / tileWidth;
        int endYTile = (sourceEY - tileYOffset) / tileHeight;

        startXTile = clip(startXTile, 0, nT.x - 1);
        startYTile = clip(startYTile, 0, nT.y - 1);
        endXTile = clip(endXTile, 0, nT.x - 1);
        endYTile = clip(endYTile, 0, nT.y - 1);

        int totalXTiles = endXTile - startXTile + 1;
        int totalYTiles = endYTile - startYTile + 1;
        int totalTiles = totalXTiles * totalYTiles;

        // Start the data delivery to the cached consumers tile by tile
        for(int y=startYTile; y <= endYTile; y++){
            if (reader.getAbortRequest())
                break;

            // Loop on horizontal tiles
            for(int x=startXTile; x <= endXTile; x++){
                if (reader.getAbortRequest())
                    break;

                float initialFraction =
                    (x - startXTile + (y - startYTile)*totalXTiles)/totalTiles;

		ictransf.setTile(x,y);

                int cTileHeight = ictransf.getTileHeight();
                int cTileWidth = ictransf.getTileWidth();

                // Offsets within the tile.
                int tx = 0;
                int ty = 0;

                // The region for this tile
                int startX = tileXOffset + x * tileWidth;
                int startY = tileYOffset + y * tileHeight;

                // sourceSX is guaranteed to be >= startX
                if (sourceSX > startX) {
                    if(startX >= hd.getImgULX()) {
                        tx = sourceSX - startX; // Intra-tile offset.
                        cTileWidth -= tx;       // Reduce effective width.
                    }
                    startX = sourceSX;      // Absolute position.
                }

                // sourceSY is guaranteed to be >= startY
                if (sourceSY > startY) {
                    if(startY >= hd.getImgULY()) {
                        ty = sourceSY - startY; // Intra-tile offset.
                        cTileHeight -= ty;      // Reduce effective width.
                    }
                    startY = sourceSY;      // Absolute position.
                }

                // Decrement dimensions if end position is within tile.
                if (sourceEX < startX + cTileWidth - 1) {
                    cTileWidth += sourceEX - startX - cTileWidth + 1;
                }
                if (sourceEY < startY + cTileHeight - 1) {
                    cTileHeight += sourceEY - startY - cTileHeight + 1;
                }

                // The start X in the destination
                int x1 = (startX + scaleX - 1 - sourceOrigin.x) / scaleX;
                int x2 = (startX + scaleX -1 + cTileWidth - sourceOrigin.x) /
                         scaleX;
                int lineLength = x2 - x1;
                if (pixbuf == null || pixbuf.length < lineLength)
                    pixbuf = new int[lineLength]; // line buffer for pixel data
                x2 = (x2 - 1) * scaleX + sourceOrigin.x - startX;

                int y1 = (startY + scaleY -1 - sourceOrigin.y) /scaleY;

                x1 += offx;
                y1 += offy;

                // Deliver in lines to reduce memory usage
                for (int l = ty, m = y1;
                     l < ty + cTileHeight;
                     l += scaleY, m++) {
                    if (reader.getAbortRequest())
                        break;
                    // Request line data
                    for (int i = 0; i < numBands; i++) {
                        DataBlkInt db = dataBlocks[i];
                        db.ulx = tx;
                        db.uly = l;
                        db.w = cTileWidth;
                        db.h = 1;
                        ictransf.getInternCompData(db, channelMap[sourceBands[i]]);
                        prog = prog || db.progressive;

                        int[] data = db.data;
                        int k1 = db.offset + x2;

                        int fracBit = fracBits[i];
                        int lS = levelShift[i];
                        int min = minValues[i];
                        int max = maxValues[i];

                        if (ImageUtil.isBinary(sampleModel)) {
                            // Force min max to 0 and 1.
                            min = 0;
                            max = 1;
                            if (bytebuf == null || bytebuf.length < cTileWidth * numBands)
                                bytebuf = new byte[cTileWidth * numBands];
                            for (int j = lineLength - 1; j >= 0; j--, k1-=scaleX) {
                                int tmp = (data[k1] >> fracBit) + lS;
                                bytebuf[j] =
                                    (byte)((tmp < min) ? min : 
					   ((tmp > max) ? max : tmp));
                            }

                            ImageUtil.setUnpackedBinaryData(bytebuf,
                                                            raster,
                                                            new Rectangle(x1,
                                                                          m,
                                                                          lineLength,
                                                                          1));
                        } else {
                            for (int j = lineLength - 1; j >= 0; j--, k1-=scaleX) {
                                int tmp = (data[k1] >> fracBit) + lS;
                                pixbuf[j] = (tmp < min) ? min : 
				    ((tmp > max) ? max : tmp);
                            }

                            // Send the line data to the BufferedImage
                            raster.setSamples(x1,
                                              m,
                                              lineLength,
                                              1,
                                              destinationBands[i],
                                              pixbuf);
                        }
                    }

                    if (destImage != null)
                        reader.processImageUpdateWrapper(destImage, x1, m,
                                                         cTileWidth, 1, 1, 1,
                                                         destinationBands);

                    float fraction = initialFraction +
                        (l - ty + 1.0F)/cTileHeight/totalTiles;
                    reader.processImageProgressWrapper(100.0f*fraction);
		}
            } // End loop on horizontal tiles
        } // End loop on vertical tiles

        return raster;
    }

    public ImageTypeSpecifier getImageType()
        throws IOException {

        getSampleModel();
        getColorModel();

        return new ImageTypeSpecifier(colorModel, sampleModel);
    }

    public SampleModel getSampleModel() {
        if (sampleModel != null)
            return sampleModel;

        if (nComp == 1 && (maxDepth == 1 || maxDepth == 2 || maxDepth == 4))
            sampleModel =
                new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE,
                                                tileWidth,
                                                tileHeight,
                                                maxDepth);
        else if (maxDepth <= 8)
            sampleModel =
                new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                                                tileWidth,
                                                tileHeight,
                                                nComp,
                                                tileWidth * nComp,
                                                bandOffsets);
        else if (maxDepth <=16)
            sampleModel =
                new PixelInterleavedSampleModel(isSigned ?
                                                DataBuffer.TYPE_SHORT :
                                                DataBuffer.TYPE_USHORT,
                                                tileWidth, tileHeight,
                                                nComp,
                                                tileWidth * nComp,
                                                bandOffsets);
        else if (maxDepth <= 32)
            sampleModel =
                new PixelInterleavedSampleModel(DataBuffer.TYPE_INT,
                                                tileWidth,
                                                tileHeight,
                                                nComp,
                                                tileWidth * nComp,
                                                bandOffsets);
        else
            throw new IllegalArgumentException(I18N.getString("J2KReadState11") + " " +
						+ maxDepth);
        return sampleModel;
    }

    public ColorModel getColorModel() {

        if (colorModel != null)
            return colorModel;

        colorModel = ff.getColorModel();
        if (colorModel != null)
            return colorModel;

        if(sampleModel == null) {
            sampleModel = getSampleModel();
        }

        if (sampleModel == null)
            return null;

        return ImageUtil.createColorModel(null, sampleModel);
    }

    private int clip(int value, int min, int max) {
        if (value < min)
            value = min;
        if (value > max)
            value = max;
        return value;
    }

    private void clipDestination(Rectangle dest) {
        Point offset = j2krparam.getDestinationOffset();
        if (dest.x < offset.x) {
            dest.width += dest.x - offset.x;
            dest.x = offset.x ;
        }
        if (dest.y < offset.y) {
            dest.height += dest.y - offset.y;
            dest.y = offset.y ;
        }
    }
}
