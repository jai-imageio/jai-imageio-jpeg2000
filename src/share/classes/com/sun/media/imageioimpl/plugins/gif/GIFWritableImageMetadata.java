/*
 * $RCSfile: GIFWritableImageMetadata.java,v $
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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import org.w3c.dom.Node;

class GIFWritableImageMetadata extends GIFMetadata {

    // package scope
    static final String
        NATIVE_FORMAT_NAME = "javax_imageio_gif_image_1.0";

    static final String[] disposalMethodNames = {
        "none",
        "doNotDispose",
        "restoreToBackgroundColor",
        "restoreToPrevious",
        "undefinedDisposalMethod4",
        "undefinedDisposalMethod5",
        "undefinedDisposalMethod6",
        "undefinedDisposalMethod7"
    };

    // Fields from Image Descriptor
    public int imageLeftPosition;
    public int imageTopPosition;
    public int imageWidth;
    public int imageHeight;
    public boolean interlaceFlag = false;
    public boolean sortFlag = false;
    public byte[] localColorTable = null;

    // Fields from Graphic Control Extension
    public int disposalMethod = 0;
    public boolean userInputFlag = false;
    public boolean transparentColorFlag = false;
    public int delayTime = 0;
    public int transparentColorIndex = 0;

    // Fields from Plain Text Extension
    public boolean hasPlainTextExtension = false;
    public int textGridLeft;
    public int textGridTop;
    public int textGridWidth;
    public int textGridHeight;
    public int characterCellWidth;
    public int characterCellHeight;
    public int textForegroundColor;
    public int textBackgroundColor;
    public byte[] text;

    // Fields from ApplicationExtension
    // List of byte[]
    public List applicationIDs = null; // new ArrayList();

    // List of byte[]
    public List authenticationCodes = null; // new ArrayList();

    // List of byte[]
    public List applicationData = null; // new ArrayList();

    // Fields from CommentExtension
    // List of byte[]
    public List comments = null; // new ArrayList();

    GIFWritableImageMetadata() {
        super(true,
              NATIVE_FORMAT_NAME,
              "com.sun.imageio.plugins.gif.GIFImageMetadataFormat", // XXX J2SE
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

    private String toISO8859(byte[] data) {
        try {
            return new String(data, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private Node getNativeTree() {
        IIOMetadataNode node; // scratch node
        IIOMetadataNode root =
            new IIOMetadataNode(nativeMetadataFormatName);

        // Image descriptor
        node = new IIOMetadataNode("ImageDescriptor");
        node.setAttribute("imageLeftPosition",
                          Integer.toString(imageLeftPosition));
        node.setAttribute("imageTopPosition",
                          Integer.toString(imageTopPosition));
        node.setAttribute("imageWidth", Integer.toString(imageWidth));
        node.setAttribute("imageHeight", Integer.toString(imageHeight));
        node.setAttribute("interlaceFlag",
                          interlaceFlag ? "true" : "false");
        root.appendChild(node);

        // Local color table
        if (localColorTable != null) {
            node = new IIOMetadataNode("LocalColorTable");
            int numEntries = localColorTable.length/3;
            node.setAttribute("sizeOfLocalColorTable",
                              Integer.toString(numEntries));
            node.setAttribute("sortFlag",
                              sortFlag ? "TRUE" : "FALSE");
            
            for (int i = 0; i < numEntries; i++) {
                IIOMetadataNode entry =
                    new IIOMetadataNode("ColorTableEntry");
                entry.setAttribute("index", Integer.toString(i));
                int r = localColorTable[3*i] & 0xff;
                int g = localColorTable[3*i + 1] & 0xff;
                int b = localColorTable[3*i + 2] & 0xff;
                entry.setAttribute("red", Integer.toString(r));
                entry.setAttribute("green", Integer.toString(g));
                entry.setAttribute("blue", Integer.toString(b));
                node.appendChild(entry);
            }
            root.appendChild(node);
        }

        // Graphic control extension
        node = new IIOMetadataNode("GraphicControlExtension");
        node.setAttribute("disposalMethod",
                          disposalMethodNames[disposalMethod]);
        node.setAttribute("userInputFlag",
                          userInputFlag ? "true" : "false");
        node.setAttribute("transparentColorFlag",
                          transparentColorFlag ? "true" : "false");
        node.setAttribute("delayTime", 
                          Integer.toString(delayTime));
        node.setAttribute("transparentColorIndex",
                          Integer.toString(transparentColorIndex));
        root.appendChild(node);

        if (hasPlainTextExtension) {
            node = new IIOMetadataNode("PlainTextExtension");
            node.setAttribute("textGridLeft",
                              Integer.toString(textGridLeft));
            node.setAttribute("textGridTop",
                              Integer.toString(textGridTop));
            node.setAttribute("textGridWidth",
                              Integer.toString(textGridWidth));
            node.setAttribute("textGridHeight",
                              Integer.toString(textGridHeight));
            node.setAttribute("characterCellWidth",
                              Integer.toString(characterCellWidth));
            node.setAttribute("characterCellHeight",
                              Integer.toString(characterCellHeight));
            node.setAttribute("textForegroundColor",
                              Integer.toString(textForegroundColor));
            node.setAttribute("textBackgroundColor",
                              Integer.toString(textBackgroundColor));
            node.setAttribute("text", toISO8859(text));

            root.appendChild(node);
        }

        // Application extensions
        int numAppExtensions = applicationIDs == null ?
            0 : applicationIDs.size();
        if (numAppExtensions > 0) {
            node = new IIOMetadataNode("ApplicationExtensions");
            for (int i = 0; i < numAppExtensions; i++) {
                IIOMetadataNode appExtNode =
                    new IIOMetadataNode("ApplicationExtension");
                byte[] applicationID = (byte[])applicationIDs.get(i);
                appExtNode.setAttribute("applicationID",
                                        toISO8859(applicationID));
                byte[] authenticationCode = (byte[])authenticationCodes.get(i);
                appExtNode.setAttribute("authenticationCode",
                                        toISO8859(authenticationCode));
                byte[] appData = (byte[])applicationData.get(i);
                appExtNode.setUserObject((byte[])appData.clone());
                node.appendChild(appExtNode);
            }

            root.appendChild(node);
        }

        // Comment extensions
        int numComments = comments == null ? 0 : comments.size();
        if (numComments > 0) {
            node = new IIOMetadataNode("CommentExtensions");
            for (int i = 0; i < numComments; i++) {
                IIOMetadataNode commentNode =
                    new IIOMetadataNode("CommentExtension");
                byte[] comment = (byte[])comments.get(i);
                commentNode.setAttribute("value", toISO8859(comment));
                node.appendChild(commentNode);
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

        node = new IIOMetadataNode("NumChannels");
        node.setAttribute("value", transparentColorFlag ? "4" : "3");
        chroma_node.appendChild(node);

        // Gamma not in format

        node = new IIOMetadataNode("BlackIsZero");
        node.setAttribute("value", "TRUE");
        chroma_node.appendChild(node);

        if (localColorTable != null) {
            node = new IIOMetadataNode("Palette");
            int numEntries = localColorTable.length/3;
            for (int i = 0; i < numEntries; i++) {
                IIOMetadataNode entry =
                    new IIOMetadataNode("PaletteEntry");
                entry.setAttribute("index", Integer.toString(i));
                entry.setAttribute("red",
                           Integer.toString(localColorTable[3*i] & 0xff));
                entry.setAttribute("green",
                           Integer.toString(localColorTable[3*i + 1] & 0xff));
                entry.setAttribute("blue",
                           Integer.toString(localColorTable[3*i + 2] & 0xff));
                node.appendChild(entry);
            }
            chroma_node.appendChild(node);
        }

        // BackgroundIndex not in image
        // BackgroundColor not in format

        return chroma_node;
    }

    public IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode compression_node = new IIOMetadataNode("Compression");
        IIOMetadataNode node = null; // scratch node

        node = new IIOMetadataNode("CompressionTypeName");
        node.setAttribute("value", "lzw");
        compression_node.appendChild(node);

        node = new IIOMetadataNode("Lossless");
        node.setAttribute("value", "TRUE");
        compression_node.appendChild(node);

        node = new IIOMetadataNode("NumProgressiveScans");
        node.setAttribute("value", interlaceFlag ? "4" : "1");
        compression_node.appendChild(node);

        // BitRate not in format

        return compression_node;
    }

    public IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode data_node = new IIOMetadataNode("Data");
        IIOMetadataNode node = null; // scratch node

        // PlanarConfiguration not in format

        node = new IIOMetadataNode("SampleFormat");
        node.setAttribute("value", "Index");
        data_node.appendChild(node);

        // BitsPerSample not in image
        // SignificantBitsPerSample not in format
        // SampleMSB not in format
        
        return data_node;
    }

    public IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimension_node = new IIOMetadataNode("Dimension");
        IIOMetadataNode node = null; // scratch node

        // PixelAspectRatio not in image

        node = new IIOMetadataNode("ImageOrientation");
        node.setAttribute("value", "Normal");
        dimension_node.appendChild(node);

        // HorizontalPixelSize not in format
        // VerticalPixelSize not in format
        // HorizontalPhysicalPixelSpacing not in format
        // VerticalPhysicalPixelSpacing not in format
        // HorizontalPosition not in format
        // VerticalPosition not in format

        node = new IIOMetadataNode("HorizontalPixelOffset");
        node.setAttribute("value", Integer.toString(imageLeftPosition));
        dimension_node.appendChild(node);

        node = new IIOMetadataNode("VerticalPixelOffset");
        node.setAttribute("value", Integer.toString(imageTopPosition));
        dimension_node.appendChild(node);

        // HorizontalScreenSize not in image
        // VerticalScreenSize not in image

        return dimension_node;
    }

    // Document not in image

    public IIOMetadataNode getStandardTextNode() {
        if (comments == null) {
            return null;
        }
        Iterator commentIter = comments.iterator();
        if (!commentIter.hasNext()) {
            return null;
        }

        IIOMetadataNode text_node = new IIOMetadataNode("Text");
        IIOMetadataNode node = null; // scratch node
        
        while (commentIter.hasNext()) {
            byte[] comment = (byte[])commentIter.next();
            String s = null;
            try {
                s = new String(comment, "ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding ISO-8859-1 unknown!");
            }

            node = new IIOMetadataNode("TextEntry");
            node.setAttribute("value", s);
            node.setAttribute("encoding", "ISO-8859-1");
            node.setAttribute("compression", "none");
            text_node.appendChild(node);
        }

        return text_node;
    }

    public IIOMetadataNode getStandardTransparencyNode() {
        if (!transparentColorFlag) {
            return null;
        }
        
        IIOMetadataNode transparency_node =
            new IIOMetadataNode("Transparency");
        IIOMetadataNode node = null; // scratch node

        // Alpha not in format

        node = new IIOMetadataNode("TransparentIndex");
        node.setAttribute("value",
                          Integer.toString(transparentColorIndex));
        transparency_node.appendChild(node);

        // TransparentColor not in format
        // TileTransparencies not in format
        // TileOpacities not in format

        return transparency_node;
    }

    public void reset() {
        // Fields from Image Descriptor
        imageLeftPosition = 0;
        imageTopPosition = 0;
        imageWidth = 0;
        imageHeight = 0;
        interlaceFlag = false;
        sortFlag = false;
        localColorTable = null;

        // Fields from Graphic Control Extension
        disposalMethod = 0;
        userInputFlag = false;
        transparentColorFlag = false;
        delayTime = 0;
        transparentColorIndex = 0;

        // Fields from Plain Text Extension
        hasPlainTextExtension = false;
        textGridLeft = 0;
        textGridTop = 0;
        textGridWidth = 0;
        textGridHeight = 0;
        characterCellWidth = 0;
        characterCellHeight = 0;
        textForegroundColor = 0;
        textBackgroundColor = 0;
        text = null;

        // Fields from ApplicationExtension
        applicationIDs = null;
        authenticationCodes = null;
        applicationData = null;

        // Fields from CommentExtension
        // List of byte[]
        comments = null;
    }

    private byte[] fromISO8859(String data) {
        try {
            return data.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            return (new String("")).getBytes();
        }
    }

    protected void mergeNativeTree(Node root) throws IIOInvalidTreeException {
        Node node = root;
        if (!node.getNodeName().equals(nativeMetadataFormatName)) {
            fatal(node, "Root must be " + nativeMetadataFormatName);
        }

        node = node.getFirstChild();
        while (node != null) {
            String name = node.getNodeName();
            
            if(name.equals("ImageDescriptor")) {
                imageLeftPosition = getIntAttribute(node,
                                                    "imageLeftPosition",
                                                    -1, true,
                                                    true, 0, 65535);

                imageTopPosition = getIntAttribute(node,
                                                   "imageTopPosition",
                                                   -1, true,
                                                   true, 0, 65535);

                imageWidth = getIntAttribute(node,
                                             "imageWidth",
                                             -1, true,
                                             true, 1, 65535);

                imageHeight = getIntAttribute(node,
                                              "imageHeight",
                                              -1, true,
                                              true, 1, 65535);

                interlaceFlag = getBooleanAttribute(node, "interlaceFlag",
                                                    false, true);
            } else if(name.equals("LocalColorTable")) {
                int sizeOfLocalColorTable =
                    getIntAttribute(node, "sizeOfLocalColorTable",
                                    true, 2, 256);
                if(sizeOfLocalColorTable != 2 &&
                   sizeOfLocalColorTable != 4 &&
                   sizeOfLocalColorTable != 8 &&
                   sizeOfLocalColorTable != 16 &&
                   sizeOfLocalColorTable != 32 &&
                   sizeOfLocalColorTable != 64 &&
                   sizeOfLocalColorTable != 128 &&
                   sizeOfLocalColorTable != 256) {
                    fatal(node,
                          "Bad value for LocalColorTable attribute sizeOfLocalColorTable!");
                }

                sortFlag = getBooleanAttribute(node, "sortFlag", false, true);

                localColorTable = getColorTable(node, "ColorTableEntry",
                                                true, sizeOfLocalColorTable);
            } else if(name.equals("GraphicControlExtension")) {
                String disposalMethodName =
                    getStringAttribute(node, "disposalMethod", null,
                                       true, disposalMethodNames);
                disposalMethod = 0;
                while(!disposalMethodName.equals(disposalMethodNames[disposalMethod])) {
                    disposalMethod++;
                }

                userInputFlag = getBooleanAttribute(node, "userInputFlag",
                                                    false, true);

                transparentColorFlag =
                    getBooleanAttribute(node, "transparentColorFlag",
                                        false, true);

                delayTime = getIntAttribute(node,
                                            "delayTime",
                                            -1, true,
                                            true, 0, 65535);

                transparentColorIndex =
                    getIntAttribute(node, "transparentColorIndex",
                                    -1, true,
                                    true, 0, 65535);
            } else if(name.equals("PlainTextExtension")) {
                hasPlainTextExtension = true;

                textGridLeft = getIntAttribute(node,
                                               "textGridLeft",
                                               -1, true,
                                               true, 0, 65535);

                textGridTop = getIntAttribute(node,
                                              "textGridTop",
                                              -1, true,
                                              true, 0, 65535);

                textGridWidth = getIntAttribute(node,
                                                "textGridWidth",
                                                -1, true,
                                                true, 1, 65535);

                textGridHeight = getIntAttribute(node,
                                                 "textGridHeight",
                                                 -1, true,
                                                 true, 1, 65535);

                characterCellWidth = getIntAttribute(node,
                                                     "characterCellWidth",
                                                     -1, true,
                                                     true, 1, 65535);

                characterCellHeight = getIntAttribute(node,
                                                      "characterCellHeight",
                                                      -1, true,
                                                      true, 1, 65535);

                textForegroundColor = getIntAttribute(node,
                                                      "textForegroundColor",
                                                      -1, true,
                                                      true, 0, 255);

                textBackgroundColor = getIntAttribute(node,
                                                      "textBackgroundColor",
                                                      -1, true,
                                                      true, 0, 255);

                // XXX The "text" attribute of the PlainTextExtension element
                // is not defined in the GIF image metadata format but it is
                // present in the GIFImageMetadata class. Consequently it is
                // used here but not required and with a default of "". See
                // bug 5082763.

                String textString =
                    getStringAttribute(node, "text", "", false, null);
                text = fromISO8859(textString);
            } else if(name.equals("ApplicationExtensions")) {
                IIOMetadataNode applicationExtension =
                    (IIOMetadataNode)node.getFirstChild();

                if(!applicationExtension.getNodeName().equals("ApplicationExtension")) {
                            fatal(node,
                                  "Only a ApplicationExtension may be a child of a ApplicationExtensions!");
                }

                String applicationIDString =
                    getStringAttribute(applicationExtension, "applicationID",
                                       null, true, null);

                String authenticationCodeString =
                    getStringAttribute(applicationExtension, "authenticationCode",
                                       null, true, null);

                Object applicationExtensionData =
                    applicationExtension.getUserObject();
                if(applicationExtensionData == null ||
                   !(applicationExtensionData instanceof byte[])) {
                    fatal(applicationExtension,
                          "Bad user object in ApplicationExtension!");
                }

                if(applicationIDs == null) {
                    applicationIDs = new ArrayList();
                    authenticationCodes = new ArrayList();
                    applicationData = new ArrayList();
                }

                applicationIDs.add(fromISO8859(applicationIDString));
                authenticationCodes.add(fromISO8859(authenticationCodeString));
                applicationData.add(applicationExtensionData);
            } else if(name.equals("CommentExtensions")) {
                Node commentExtension = node.getFirstChild();
                if(commentExtension != null) {
                    while(commentExtension != null) {
                        if(!commentExtension.getNodeName().equals("CommentExtension")) {
                            fatal(node,
                                  "Only a CommentExtension may be a child of a CommentExtensions!");
                        }

                        if(comments == null) {
                            comments = new ArrayList();
                        }

                        String comment =
                            getStringAttribute(commentExtension, "value", null,
                                               true, null);

                        comments.add(fromISO8859(comment));

                        commentExtension = commentExtension.getNextSibling();
                    }
                }
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
                        localColorTable = getColorTable(childNode,
                                                        "PaletteEntry",
                                                        false, -1);
                        break;
                    }
                    childNode = childNode.getNextSibling();
                }
            } else if(name.equals("Compression")) {
                Node childNode = node.getFirstChild();
                while(childNode != null) {
                    String childName = childNode.getNodeName();
                    if(childName.equals("NumProgressiveScans")) {
                        int numProgressiveScans =
                            getIntAttribute(childNode, "value", 4, false,
                                            true, 1, Integer.MAX_VALUE);
                        if(numProgressiveScans > 1) {
                            interlaceFlag = true;
                        }
                        break;
                    }
                    childNode = childNode.getNextSibling();
                }
            } else if(name.equals("Dimension")) {
                Node childNode = node.getFirstChild();
                while(childNode != null) {
                    String childName = childNode.getNodeName();
                    if(childName.equals("HorizontalPixelOffset")) {
                        imageLeftPosition = getIntAttribute(childNode,
                                                            "value",
                                                            -1, true,
                                                            true, 0, 65535);
                    } else if(childName.equals("VerticalPixelOffset")) {
                        imageTopPosition = getIntAttribute(childNode,
                                                           "value",
                                                           -1, true,
                                                           true, 0, 65535);
                    }
                    childNode = childNode.getNextSibling();
                }
            } else if(name.equals("Text")) {
                Node childNode = node.getFirstChild();
                while(childNode != null) {
                    String childName = childNode.getNodeName();
                    if(childName.equals("TextEntry") &&
                       getAttribute(childNode, "compression",
                                    "none", false).equals("none") &&
                       Charset.isSupported(getAttribute(childNode,
                                                        "encoding",
                                                        "ISO-8859-1",
                                                        false))) {
                        String value = getAttribute(childNode, "value");
                        byte[] comment = fromISO8859(value);
                        if(comments == null) {
                            comments = new ArrayList();
                        }
                        comments.add(comment);
                    }
                    childNode = childNode.getNextSibling();
                }
            } else if(name.equals("Transparency")) {
                Node childNode = node.getFirstChild();
                while(childNode != null) {
                    String childName = childNode.getNodeName();
                    if(childName.equals("TransparentIndex")) {
                        transparentColorIndex = getIntAttribute(childNode,
                                                                "value",
                                                                -1, true,
                                                                true, 0, 255);
                        transparentColorFlag = true;
                        break;
                    }
                    childNode = childNode.getNextSibling();
                }
            }

            node = node.getNextSibling();
        }
    }
}
