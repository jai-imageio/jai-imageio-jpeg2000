/*
 * $RCSfile: GIFWritableStreamMetadata.java,v $
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

//package com.sun.imageio.plugins.gif;
package com.sun.media.imageioimpl.plugins.gif;

//
// The source for this class was copied verbatim from the source for
// package com.sun.imageio.plugins.gif.GIFImageMetadata and then modified
// to make the class read-write capable.
//

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import org.w3c.dom.Node;

class GIFWritableStreamMetadata extends GIFMetadata {

    // package scope
    static final String
        NATIVE_FORMAT_NAME = "javax_imageio_gif_stream_1.0";

    public static final String[] versionStrings = { "87a", "89a" };

    public String version; // 87a or 89a
    public int logicalScreenWidth;
    public int logicalScreenHeight;
    public int colorResolution; // 1 to 8
    public int pixelAspectRatio;

    public int backgroundColorIndex; // Valid if globalColorTable != null
    public boolean sortFlag; // Valid if globalColorTable != null

    public static final String[] colorTableSizes = {
        "2", "4", "8", "16", "32", "64", "128", "256"
    };

    // Set global color table flag in header to 0 if null, 1 otherwise
    public byte[] globalColorTable = null;

    GIFWritableStreamMetadata() {
        super(true, 
              NATIVE_FORMAT_NAME,
              "com.sun.imageio.plugins.gif.GIFStreamMetadataFormat", // XXX J2SE
              null, null);

    }

    public boolean isReadOnly() {
        return false;
    }
    
    public Node getAsTree(String formatName) {
        if (formatName.equals(nativeMetadataFormatName)) {
            return getNativeTree();
        } else if (formatName.equals
                   (IIOMetadataFormatImpl.standardMetadataFormatName)) {
            return getStandardTree();
        } else {
            throw new IllegalArgumentException("Not a recognized format!");
        }
    }

    private Node getNativeTree() {
        IIOMetadataNode node; // scratch node
        IIOMetadataNode root =
            new IIOMetadataNode(nativeMetadataFormatName);
            
        node = new IIOMetadataNode("Version");
        node.setAttribute("value", version);
        root.appendChild(node);
        
        // Image descriptor
        node = new IIOMetadataNode("LogicalScreenDescriptor");
        node.setAttribute("logicalScreenWidth",
                          Integer.toString(logicalScreenWidth));
        node.setAttribute("logicalScreenHeight",
                          Integer.toString(logicalScreenHeight));
        // Stored value plus one
        node.setAttribute("colorResolution",
                          Integer.toString(colorResolution));
        node.setAttribute("pixelAspectRatio",
                          Integer.toString(pixelAspectRatio));
        root.appendChild(node);

        if (globalColorTable != null) {
            node = new IIOMetadataNode("GlobalColorTable");
            int numEntries = globalColorTable.length/3;
            node.setAttribute("sizeOfGlobalColorTable",
                              Integer.toString(numEntries));
            node.setAttribute("backgroundColorIndex",
                              Integer.toString(backgroundColorIndex));
            node.setAttribute("sortFlag",
                              sortFlag ? "TRUE" : "FALSE");

            for (int i = 0; i < numEntries; i++) {
                IIOMetadataNode entry =
                    new IIOMetadataNode("ColorTableEntry");
                entry.setAttribute("index", Integer.toString(i));
                int r = globalColorTable[3*i] & 0xff;
                int g = globalColorTable[3*i + 1] & 0xff;
                int b = globalColorTable[3*i + 2] & 0xff;
                entry.setAttribute("red", Integer.toString(r));
                entry.setAttribute("green", Integer.toString(g));
                entry.setAttribute("blue", Integer.toString(b));
                node.appendChild(entry);
            }
            root.appendChild(node);
        }

        return root;
    }

    public IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma_node = new IIOMetadataNode("Chroma");
        IIOMetadataNode node = null; // scratch node

        node = new IIOMetadataNode("ColorSpaceType");
        node.setAttribute("name", "RGB");
        chroma_node.appendChild(node);

        node = new IIOMetadataNode("BlackIsZero");
        node.setAttribute("value", "TRUE");
        chroma_node.appendChild(node);

        // NumChannels not in stream
        // Gamma not in format

        if (globalColorTable != null) {
            node = new IIOMetadataNode("Palette");
            int numEntries = globalColorTable.length/3;
            for (int i = 0; i < numEntries; i++) {
                IIOMetadataNode entry =
                    new IIOMetadataNode("PaletteEntry");
                entry.setAttribute("index", Integer.toString(i));
                entry.setAttribute("red",
                           Integer.toString(globalColorTable[3*i] & 0xff));
                entry.setAttribute("green",
                           Integer.toString(globalColorTable[3*i + 1] & 0xff));
                entry.setAttribute("blue",
                           Integer.toString(globalColorTable[3*i + 2] & 0xff));
                node.appendChild(entry);
            }
            chroma_node.appendChild(node);

            // backgroundColorIndex is valid iff there is a color table
            node = new IIOMetadataNode("BackgroundIndex");
            node.setAttribute("value", Integer.toString(backgroundColorIndex));
            chroma_node.appendChild(node);
        }

        return chroma_node;
    }

    public IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode compression_node = new IIOMetadataNode("Compression");
        IIOMetadataNode node = null; // scratch node

        node = new IIOMetadataNode("CompressionTypeName");
        node.setAttribute("value", "lzw");
        compression_node.appendChild(node);

        node = new IIOMetadataNode("Lossless");
        node.setAttribute("value", "true");
        compression_node.appendChild(node);

        // NumProgressiveScans not in stream
        // BitRate not in format

        return compression_node;
    }

    public IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode data_node = new IIOMetadataNode("Data");
        IIOMetadataNode node = null; // scratch node

        // PlanarConfiguration

        node = new IIOMetadataNode("SampleFormat");
        node.setAttribute("value", "Index");
        data_node.appendChild(node);

        node = new IIOMetadataNode("BitsPerSample");
        node.setAttribute("value", Integer.toString(colorResolution));
        data_node.appendChild(node);
        
        // SignificantBitsPerSample
        // SampleMSB
        
        return data_node;
    }

    public IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimension_node = new IIOMetadataNode("Dimension");
        IIOMetadataNode node = null; // scratch node

        node = new IIOMetadataNode("PixelAspectRatio");
        float aspectRatio = 1.0F;
        if (pixelAspectRatio != 0) {
            aspectRatio = (pixelAspectRatio + 15)/64.0F;
        }
        node.setAttribute("value", Float.toString(aspectRatio));
        dimension_node.appendChild(node);

        node = new IIOMetadataNode("ImageOrientation");
        node.setAttribute("value", "Normal");
        dimension_node.appendChild(node);

        // HorizontalPixelSize not in format
        // VerticalPixelSize not in format
        // HorizontalPhysicalPixelSpacing not in format
        // VerticalPhysicalPixelSpacing not in format
        // HorizontalPosition not in format
        // VerticalPosition not in format
        // HorizontalPixelOffset not in stream
        // VerticalPixelOffset not in stream

        node = new IIOMetadataNode("HorizontalScreenSize");
        node.setAttribute("value", Integer.toString(logicalScreenWidth));
        dimension_node.appendChild(node);
        
        node = new IIOMetadataNode("VerticalScreenSize");
        node.setAttribute("value", Integer.toString(logicalScreenHeight));
        dimension_node.appendChild(node);
        
        return dimension_node;
    }

    public IIOMetadataNode getStandardDocumentNode() {
        IIOMetadataNode document_node = new IIOMetadataNode("Document");
        IIOMetadataNode node = null; // scratch node

        node = new IIOMetadataNode("FormatVersion");
        node.setAttribute("value", version);
        document_node.appendChild(node);

        // SubimageInterpretation not in format
        // ImageCreationTime not in format
        // ImageModificationTime not in format

        return document_node;
    }

    public IIOMetadataNode getStandardTextNode() {
        // Not in stream
        return null;
    }

    public IIOMetadataNode getStandardTransparencyNode() {
        // Not in stream
        return null;
    }

    public void mergeTree(String formatName, Node root)
        throws IIOInvalidTreeException {
        if (formatName.equals(nativeMetadataFormatName)) {
            if (root == null) {
                throw new IllegalArgumentException("root == null!");
            }
            mergeNativeTree(root);
        } else if (formatName.equals
                   (IIOMetadataFormatImpl.standardMetadataFormatName)) {
            if (root == null) {
                throw new IllegalArgumentException("root == null!");
            }
            mergeStandardTree(root);
        } else {
            throw new IllegalArgumentException("Not a recognized format!");
        }
    }

    public void reset() {
        version = null;

        logicalScreenWidth = 0;
        logicalScreenHeight = 0;
        colorResolution = 0;
        pixelAspectRatio = 0;

        backgroundColorIndex = 0;
        sortFlag = false;
        globalColorTable = null;
    }

    protected void mergeNativeTree(Node root) throws IIOInvalidTreeException {
        Node node = root;
        if (!node.getNodeName().equals(nativeMetadataFormatName)) {
            fatal(node, "Root must be " + nativeMetadataFormatName);
        }

        node = node.getFirstChild();
        while (node != null) {
            String name = node.getNodeName();
            
            if(name.equals("Version")) {
                version = getStringAttribute(node, "value", null,
                                             true, versionStrings);
            } else if(name.equals("LogicalScreenDescriptor")) {
                logicalScreenWidth = getIntAttribute(node,
                                                     "logicalScreenWidth",
                                                     -1, true,
                                                     true, 1, 65535);

                logicalScreenHeight = getIntAttribute(node,
                                                      "logicalScreenHeight",
                                                      -1, true,
                                                      true, 1, 65535);

                colorResolution = getIntAttribute(node,
                                                  "colorResolution",
                                                  -1, true,
                                                  true, 1, 8);

                pixelAspectRatio = getIntAttribute(node,
                                                   "pixelAspectRatio",
                                                   -1, true,
                                                   true, 0, 255);
            } else if(name.equals("GlobalColorTable")) {
                int sizeOfGlobalColorTable =
                    getIntAttribute(node, "sizeOfGlobalColorTable",
                                    true, 2, 256);
                if(sizeOfGlobalColorTable != 2 &&
                   sizeOfGlobalColorTable != 4 &&
                   sizeOfGlobalColorTable != 8 &&
                   sizeOfGlobalColorTable != 16 &&
                   sizeOfGlobalColorTable != 32 &&
                   sizeOfGlobalColorTable != 64 &&
                   sizeOfGlobalColorTable != 128 &&
                   sizeOfGlobalColorTable != 256) {
                    fatal(node,
                          "Bad value for GlobalColorTable attribute sizeOfGlobalColorTable!");
                }

                backgroundColorIndex = getIntAttribute(node,
                                                       "backgroundColorIndex",
                                                       -1, true,
                                                       true, 0, 255);

                sortFlag = getBooleanAttribute(node, "sortFlag", false, true);

                globalColorTable = getColorTable(node, "ColorTableEntry",
                                                 true, sizeOfGlobalColorTable);
            } else {
                fatal(node, "Unknown child of root node!");
            }

            node = node.getNextSibling();
        }
    }

    protected void mergeStandardTree(Node root)
        throws IIOInvalidTreeException {
        Node node = root;
        if (!node.getNodeName()
            .equals(IIOMetadataFormatImpl.standardMetadataFormatName)) {
            fatal(node, "Root must be " +
                  IIOMetadataFormatImpl.standardMetadataFormatName);
        }

        node = node.getFirstChild();
        while (node != null) {
            String name = node.getNodeName();
            
            if(name.equals("Chroma")) {
                Node childNode = node.getFirstChild();
                while(childNode != null) {
                    String childName = childNode.getNodeName();
                    if(childName.equals("Palette")) {
                        globalColorTable = getColorTable(childNode,
                                                         "PaletteEntry",
                                                         false, -1);

                    } else if(childName.equals("BackgroundIndex")) {
                        backgroundColorIndex = getIntAttribute(childNode,
                                                               "value",
                                                               -1, true,
                                                               true, 0, 255);
                    }
                    childNode = childNode.getNextSibling();
                }
            } else if(name.equals("Data")) {
                Node childNode = node.getFirstChild();
                while(childNode != null) {
                    String childName = childNode.getNodeName();
                    if(childName.equals("BitsPerSample")) {
                        colorResolution = getIntAttribute(childNode,
                                                          "value",
                                                          -1, true,
                                                          true, 1, 8);
                        break;
                    }
                    childNode = childNode.getNextSibling();
                }
            } else if(name.equals("Dimension")) {
                Node childNode = node.getFirstChild();
                while(childNode != null) {
                    String childName = childNode.getNodeName();
                    if(childName.equals("PixelAspectRatio")) {
                        float aspectRatio = getFloatAttribute(childNode,
                                                              "value");
                        if(aspectRatio == 1.0F) {
                            pixelAspectRatio = 0;
                        } else {
                            int ratio = (int)(aspectRatio*64.0F - 15.0F);
                            pixelAspectRatio =
                                Math.max(Math.min(ratio, 255), 0);
                        }
                    } else if(childName.equals("HorizontalScreenSize")) {
                        logicalScreenWidth = getIntAttribute(childNode,
                                                             "value",
                                                             -1, true,
                                                             true, 1, 65535);
                    } else if(childName.equals("VerticalScreenSize")) {
                        logicalScreenHeight = getIntAttribute(childNode,
                                                              "value",
                                                              -1, true,
                                                              true, 1, 65535);
                    }
                    childNode = childNode.getNextSibling();
                }
            } else if(name.equals("Document")) {
                Node childNode = node.getFirstChild();
                while(childNode != null) {
                    String childName = childNode.getNodeName();
                    if(childName.equals("FormatVersion")) {
                        String formatVersion =
                            getStringAttribute(childNode, "value", null,
                                               true, null);
                        for(int i = 0; i < versionStrings.length; i++) {
                            if(formatVersion.equals(versionStrings[i])) {
                                version = formatVersion;
                                break;
                            }
                        }
                        break;
                    }
                    childNode = childNode.getNextSibling();
                }
            }

            node = node.getNextSibling();
        }
    }
}
