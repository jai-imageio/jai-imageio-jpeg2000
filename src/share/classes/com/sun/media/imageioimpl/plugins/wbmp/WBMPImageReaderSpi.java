/*
 * $RCSfile: WBMPImageReaderSpi.java,v $
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
 * $Date: 2005-02-11 05:01:52 $
 * $State: Exp $
 */
package com.sun.media.imageioimpl.plugins.wbmp;

import java.util.Locale;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ServiceRegistry;
import java.io.IOException;
import javax.imageio.ImageReader;
import javax.imageio.IIOException;
import com.sun.media.imageioimpl.common.PackageUtil;

public class WBMPImageReaderSpi extends ImageReaderSpi {

    private static String [] writerSpiNames =
        {"com.sun.media.imageioimpl.plugins.wbmp.WBMPImageWriterSpi"};
    private static String[] formatNames = {"wbmp", "WBMP"};
    private static String[] entensions = {"wbmp"};
    private static String[] mimeType = {"image/vnd.wap.wbmp"};

    private boolean registered = false;

    public WBMPImageReaderSpi() {
        super(PackageUtil.getVendor(),
              PackageUtil.getVersion(),
              formatNames,
              entensions,
              mimeType,
              "com.sun.media.imageioimpl.plugins.wbmp.WBMPImageReader",
              STANDARD_INPUT_TYPE,
              writerSpiNames,
              true,
              null, null, null, null,
              true,
              WBMPMetadata.nativeMetadataFormatName,
	      "com.sun.media.imageioimpl.plugins.wbmp.WBMPMetadataFormat",
	      null, null);
    }

    public void onRegistration(ServiceRegistry registry,
                               Class category) {
        if (registered) {
            return;
        }
        registered = true;
    }

    public String getDescription(Locale locale) {
        return "Standard WBMP Image Reader";
    }

    public boolean canDecodeInput(Object source) throws IOException {
        if (!(source instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream)source;
        byte[] b = new byte[3];

        stream.mark();
        stream.readFully(b);
        stream.reset();

        return ((b[0] == (byte)0) &&  // TypeField == 0
                b[1] == 0 && // FixHeaderField == 0xxx00000; not support ext header
                ((b[2] & 0x8f) != 0 || (b[2] & 0x7f) != 0));  // First width byte
                //XXX: b[2] & 0x8f) != 0 for the bug in Sony Ericsson encoder.
    }

    public ImageReader createReaderInstance(Object extension)
        throws IIOException {
        return new WBMPImageReader(this);
    }
}

