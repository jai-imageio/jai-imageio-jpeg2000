/*
 * $RCSfile: CLibPNGImageReaderSpi.java,v $
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
 * $Date: 2005-02-11 05:01:38 $
 * $State: Exp $
 */
package com.sun.media.imageioimpl.plugins.png;

import java.util.Locale;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.spi.ServiceRegistry;
import java.io.IOException;
import javax.imageio.ImageReader;
import javax.imageio.IIOException;
import com.sun.media.imageioimpl.common.PackageUtil;

public class CLibPNGImageReaderSpi extends ImageReaderSpi {

    private static final String[] names = {"png", "PNG"};

    private static final String[] suffixes = {"png"};
    
    private static final String[] MIMETypes = {"image/png", "image/x-png"};

    private static final String readerClassName =
        "com.sun.media.imageioimpl.plugins.png.CLibPNGImageReader";

    private static final String[] writerSpiNames = {
        "com.sun.media.imageioimpl.plugins.png.CLibPNGImageWriterSpi"
    };

    public CLibPNGImageReaderSpi() {
        super(PackageUtil.getVendor(),
              PackageUtil.getVersion(),
              names,
              suffixes,
              MIMETypes,
              readerClassName,
              STANDARD_INPUT_TYPE,
              writerSpiNames,
              false,
              null, null,
              null, null,
              true,
              CLibPNGMetadata.nativeMetadataFormatName,
              "com.sun.media.imageioimpl.plugins.png.CLibPNGMetadataFormat",
              null, null);
    }

    public void onRegistration(ServiceRegistry registry,
                               Class category) {
        // Branch as a function of codecLib availability.
        if(!PackageUtil.isCodecLibAvailable()) {
            // Deregister provider.
            registry.deregisterServiceProvider(this);
        } else {
            // Set pairwise ordering to give codecLib reader precedence
            // over Sun core J2SE reader.
            Class coreReaderSPIClass = null;
            try {
                coreReaderSPIClass =
                    Class.forName("com.sun.imageio.plugins.png.PNGImageReaderSpi");
            } catch(Throwable t) {
                // Ignore it.
            }

            if(coreReaderSPIClass != null) {
                Object coreReaderSPI =
                    registry.getServiceProviderByClass(coreReaderSPIClass);
                if(coreReaderSPI != null) {
                    registry.setOrdering(category, this, coreReaderSPI);
                }
            }
        }
    }

    public String getDescription(Locale locale) {
        return "codecLib PNG Image Reader";
    }

    public boolean canDecodeInput(Object source) throws IOException {
        if (!(source instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream)source;
        byte[] b = new byte[8];
        stream.mark();
        stream.readFully(b);
        stream.reset();
        
        return (b[0] == (byte)137 &&
                b[1] == (byte)80 &&
                b[2] == (byte)78 &&
                b[3] == (byte)71 &&
                b[4] == (byte)13 &&
                b[5] == (byte)10 &&
                b[6] == (byte)26 &&
                b[7] == (byte)10);
    }
    
    public ImageReader createReaderInstance(Object extension) 
        throws IIOException {
        return new CLibPNGImageReader(this);
    }
}
