/*
 * $RCSfile: BMPImageWriteParam.java,v $
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
 * $Date: 2005-02-11 05:01:14 $
 * $State: Exp $
 */
package com.sun.media.imageio.plugins.bmp;

import java.util.Locale;
import javax.imageio.ImageWriteParam;

/**
 * A subclass of <code>ImageWriteParam</code> for encoding images in
 * the BMP format.
 *
 * <p> This class allows for the specification of various parameters
 * while writing a BMP format image file.  By default, the version
 * used is VERSION_3, no compression is used, and the data layout is
 * bottom_up, such that the pixels are stored in bottom-up order, the
 * first scanline being stored last.
 *
 * <p>Compression may be requested if the BMP version is 3.0.
 * The particular compression scheme to be used can be specified by using
 * the <code>setCompressionType()</code> method with the appropriate type
 * string.  The compression scheme specified will be honored if only if it
 * is compatible with the type of image being written.  The compression type
 * strings and the image type(s) each supports are listed in the following
 * table:
 *
 * <p><table border=1>
 * <caption><b>Compression Types</b></caption>
 * <tr><th>Type String</th> <th>Description</th>  <th>Image Types</th></tr>
 * <tr><td>BI_RGB</td>  <td>Uncompressed RLE</td> <td><= 8-bits/sample</td></tr>
 * <tr><td>BI_RLE8</td> <td>8-bit Run Length Encoding</td> <td><= 8-bits/sample</td></tr>
 * <tr><td>BI_RLE4</td> <td>4-bit Run Length Encoding</td> <td><= 4-bits/sample</td></tr>
 * <tr><td>BI_BITFIELDS</td> <td>Packed data</td> <td>16-bit or 32-bit (See below)</td></tr>
 * <tr><td>BI_JPEG</td> <td>JPEG encoded</td> <td>grayscale or RGB image</td></tr>
 * </table>
 *
 * <p> When <code>BI_BITFIELDS</code> is used, if the image encoded has a
 * <code>DirectColorModel</code>, the bit mask in the color model will be
 * written into the stream.  Otherwise, only 5-5-5 16-bit image or 8-8-8 
 * 32-bit images are supported.
 *
 */
public class BMPImageWriteParam extends ImageWriteParam {

    // version constants

    /** Constant for BMP version 2. */
    public static final int VERSION_2 = 0;

    /** Constant for BMP version 3. */
    public static final int VERSION_3 = 1;

    /** Constant for BMP version 4. */
    public static final int VERSION_4 = 2;

    /** Constant for BMP version 5. */
    public static final int VERSION_5 = 3;

    // Default values
    private int version = VERSION_3;
    private boolean topDown = false;

    /**
     * Constructs a <code>BMPImageWriteParam</code> set to use a given
     * <code>Locale</code> and with default values for all parameters.
     *
     * @param locale a <code>Locale</code> to be used to localize
     * compression type names and quality descriptions, or
     * <code>null</code>.
     */
    public BMPImageWriteParam(Locale locale) {
        super(locale);

        // Set compression flag.
        canWriteCompressed = version != VERSION_2;
	compressionMode = MODE_COPY_FROM_METADATA;

        // Set compression types ("BI_RGB" denotes uncompressed).
        compressionTypes = new String[] {
            "BI_RGB", "BI_RLE8", "BI_RLE4", "BI_BITFIELDS",
            "BI_JPEG", "BI_PNG"
        };
    }

    /**
     * Constructs an <code>BMPImageWriteParam</code> object with default
     * values for all parameters and a <code>null</code> <code>Locale</code>.
     */
    public BMPImageWriteParam() {
        this(null);
    }

    //
    // Sets the BMP version to be used.  If <code>versionNumber</code>
    // is <code>VERSION_2</code> then <code>canWriteCompressed</code>
    // is set to <code>false</code> but otherwise to <code>true</code>.
    //
    // @throws IllegalArgumentException if <code>versionNumber</code> is
    // not one of the pre-defined enumerated values <code>VERSION_*</code>.
    //
/*
    public void setVersion(int versionNumber) {
	if ( !(versionNumber == VERSION_2 ||
	       versionNumber == VERSION_3 ||
	       versionNumber == VERSION_4 ||
               versionNumber == VERSION_5) ) {
	    throw new IllegalArgumentException(I18N.getString("BMPImageWriteParam0"));
	}

	this.version = versionNumber;
        canWriteCompressed = versionNumber != VERSION_2;
    }
*/

    /**
     * Returns the BMP version to be used.  The default is
     * <code>VERSION_3</code>.
     *
     * @return the BMP version number.
     */
    public int getVersion() {
	return version;
    }

    /**
     * If set, the data will be written out in a top-down manner, the first
     * scanline being written first.
     *
     * Any compression other than <code>BI_RGB</code> or 
     * <code>BI_BITFIELDS</code> is incompatible with the data being 
     * written in top-down order. Setting the <code>topDown</code> argument
     * to <code>true</code> will be honored only when the compression 
     * type at the time of writing the image is one of the two mentioned
     * above. Otherwise, the <code>topDown</code> setting will be ignored.
     * 
     * @param topDown whether the data are written in top-down order.
     */
    public void setTopDown(boolean topDown) {
	this.topDown = topDown;
    }

    /**
     * Returns the value of the <code>topDown</code> parameter.
     * The default is <code>false</code>.
     *
     * @return whether the data are written in top-down order.
     */
    public boolean isTopDown() {
	return topDown;
    }

    // Override superclass implementation to add a new check that compression 
    // is not being set when image has been specified to be encoded in a top
    // down fashion

    /**
     * Sets the compression type to one of the values indicated by
     * <code>getCompressionTypes</code>.  If a value of
     * <code>null</code> is passed in, any previous setting is
     * removed.
     *
     * <p>The method first invokes
     * {@link javax.imageio.ImageWriteParam.#setCompressionType(String) 
     * <code>setCompressionType()</code>}
     *  with the supplied value of <code>compressionType</code>. Next, 
     * if {@link #isTopDown()} returns <code>true</code> and the
     * value of <code>compressionType</code> is incompatible with top-down
     * order, {@link #setTopDown(boolean)} is invoked with parameter
     * <code>topDown</code> set to <code>false</code>. The image will
     * then be written in bottom-up order with the specified
     * <code>compressionType</code>.</p>
     *
     * @param compressionType one of the <code>String</code>s returned
     * by <code>getCompressionTypes</code>, or <code>null</code> to
     * remove any previous setting.
     *
     * @exception UnsupportedOperationException if the writer does not
     * support compression.
     * @exception IllegalStateException if the compression mode is not
     * <code>MODE_EXPLICIT</code>.
     * @exception UnsupportedOperationException if there are no
     * settable compression types.
     * @exception IllegalArgumentException if
     * <code>compressionType</code> is non-<code>null</code> but is not
     * one of the values returned by <code>getCompressionTypes</code>.
     *
     * @see #isTopDown
     * @see #setTopDown
     * @see #getCompressionTypes
     * @see #getCompressionType
     * @see #unsetCompression
     */
    public void setCompressionType(String compressionType) {
	super.setCompressionType(compressionType);
	if (!(compressionType.equals("BI_RGB")) &&
	    !(compressionType.equals("BI_BITFIELDS")) && topDown == true) {
	    topDown = false;
	}
    }

}
