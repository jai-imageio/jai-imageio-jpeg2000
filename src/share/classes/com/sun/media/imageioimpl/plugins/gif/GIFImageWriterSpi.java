/*
 * $RCSfile: GIFImageWriterSpi.java,v $
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
 * $Date: 2005-02-11 05:01:29 $
 * $State: Exp $
 */

package com.sun.media.imageioimpl.plugins.gif;//XXX

import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.util.Locale;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import com.sun.media.imageioimpl.common.PackageUtil;

public class GIFImageWriterSpi extends ImageWriterSpi {

    private static final String[] names = { "gif", "GIF" };

    private static final String[] suffixes = { "gif" };

    private static final String[] MIMETypes = { "image/gif" };

    private static final String writerClassName =
        "com.sun.media.imageioimpl.plugins.gif.GIFImageWriter";

    private static final String[] readerSpiNames = {
        "com.sun.imageio.plugins.gif.GIFImageReaderSpi" // XXX J2SE core
    };

    public GIFImageWriterSpi() {
        super(PackageUtil.getVendor(),
              PackageUtil.getVersion(),
              names,
              suffixes,
              MIMETypes,
              writerClassName,
              STANDARD_OUTPUT_TYPE,
              readerSpiNames,
              true,
              GIFWritableStreamMetadata.NATIVE_FORMAT_NAME,
              "com.sun.imageio.plugins.gif.GIFStreamMetadataFormat",// XXX J2SE core
              null, null,
              true,
              GIFWritableImageMetadata.NATIVE_FORMAT_NAME,
              "com.sun.imageio.plugins.gif.GIFImageMetadataFormat",// XXX J2SE core
              null, null
              );
    }

    public boolean canEncodeImage(ImageTypeSpecifier type) {
        if(type == null) {
            throw new IllegalArgumentException("type == null!");
        }

        SampleModel sm = type.getSampleModel();
        ColorModel cm = type.getColorModel();

        return sm.getNumBands() == 1 && sm.getSampleSize(0) <= 8 &&
            sm.getWidth() <= 65535 && sm.getHeight() <= 65535 &&
            (cm == null || cm.getComponentSize()[0] <= 8);
    }

    public String getDescription(Locale locale) {
        return "Standard GIF image writer";
    }

    public ImageWriter createWriterInstance(Object extension) {
        return new GIFImageWriter(this);
    }
}
