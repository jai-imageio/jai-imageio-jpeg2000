/*
 * $RCSfile: SimpleCMYKColorSpace.java,v $
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
 * $Date: 2005-02-11 05:01:23 $
 * $State: Exp $
 */
package com.sun.media.imageioimpl.common;

import java.awt.color.ColorSpace;

public final class SimpleCMYKColorSpace extends ColorSpace {
    private ColorSpace csRGB;

    public SimpleCMYKColorSpace() {
        super(TYPE_CMYK, 4);
        csRGB = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
    }

    public boolean equals(Object o) {
        return o != null && o instanceof SimpleCMYKColorSpace;
    }

    public float[] toRGB(float[] colorvalue) {
        float K = colorvalue[3];
        float C = colorvalue[0] - K;
        float M = colorvalue[1] - K;
        float Y = colorvalue[2] - K;

        return new float[] {1.0F - C, 1.0F - M, 1.0F - Y};
    }

    public float[] fromRGB(float[] rgbvalue) {
        float C = 1.0F - rgbvalue[0];
        float M = 1.0F - rgbvalue[1];
        float Y = 1.0F - rgbvalue[2];
        float K = Math.min(C, Math.min(M, Y));

        return new float[] {C, M, Y, K};
    }

    public float[] toCIEXYZ(float[] colorvalue) {
        return csRGB.toCIEXYZ(toRGB(colorvalue));
    }

    public float[] fromCIEXYZ(float[] xyzvalue) {
        return fromRGB(csRGB.fromCIEXYZ(xyzvalue));
    }
}
