/*
 * $RCSfile: PCXMetadata.java,v $
 *
 * 
 * Copyright (c) 2007 Sun Microsystems, Inc. All  Rights Reserved.
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
 * $Date: 2007-09-05 00:21:08 $
 * $State: Exp $
 */
package com.sun.media.imageioimpl.plugins.pcx;

import javax.imageio.metadata.*;

import org.w3c.dom.Node;

import com.sun.media.imageioimpl.common.ImageUtil;

public class PCXMetadata extends IIOMetadata implements Cloneable, PCXConstants {

    byte bitsPerPixel;
    int vdpi=72,hdpi=72;
    int hsize,vsize;

    private void addXYZPoints(IIOMetadataNode root, String name, double x, double y, double z) {
        IIOMetadataNode node = addChildNode(root, name, null);
        addChildNode(node, "X", new Double(x));
        addChildNode(node, "Y", new Double(y));
        addChildNode(node, "Z", new Double(z));
    }

    private IIOMetadataNode addChildNode(IIOMetadataNode root,
                                         String name,
                                         Object object) {
        IIOMetadataNode child = new IIOMetadataNode(name);
        if (object != null) {
            child.setUserObject(object);
	    child.setNodeValue(ImageUtil.convertObjectToString(object));
	}
        root.appendChild(child);
        return child;
    }

    private Object getObjectValue(Node node) {
        Object tmp = node.getNodeValue();

        if(tmp == null && node instanceof IIOMetadataNode) {
            tmp = ((IIOMetadataNode)node).getUserObject();
        }

        return tmp;
    }

    private String getStringValue(Node node) {
        Object tmp = getObjectValue(node);
        return tmp instanceof String ? (String)tmp : null;
    }

    private Byte getByteValue(Node node) {
        Object tmp = getObjectValue(node);
        Byte value = null;
        if(tmp instanceof String) {
            value = Byte.valueOf((String)tmp);
        } else if(tmp instanceof Byte) {
            value = (Byte)tmp;
        }
        return value;
    }

    private Short getShortValue(Node node) {
        Object tmp = getObjectValue(node);
        Short value = null;
        if(tmp instanceof String) {
            value = Short.valueOf((String)tmp);
        } else if(tmp instanceof Short) {
            value = (Short)tmp;
        }
        return value;
    }

    private Integer getIntegerValue(Node node) {
        Object tmp = getObjectValue(node);
        Integer value = null;
        if(tmp instanceof String) {
            value = Integer.valueOf((String)tmp);
        } else if(tmp instanceof Integer) {
            value = (Integer)tmp;
        } else if(tmp instanceof Byte) {
            value = new Integer(((Byte)tmp).byteValue() & 0xff);
        }
        return value;
    }

    private Double getDoubleValue(Node node) {
        Object tmp = getObjectValue(node);
        Double value = null;
        if(tmp instanceof String) {
            value = Double.valueOf((String)tmp);
        } else if(tmp instanceof Double) {
            value = (Double)tmp;
        }
        return value;
    }

    public Node getAsTree(String formatName) {
	return null;
    }

    public boolean isReadOnly() {
	return false;
    }

    public void mergeTree(String formatName, Node root) throws IIOInvalidTreeException {
    }

    public void reset() {
    }
}
