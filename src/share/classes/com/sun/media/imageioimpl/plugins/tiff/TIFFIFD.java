/*
 * $RCSfile: TIFFIFD.java,v $
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
 * $Revision: 1.3 $
 * $Date: 2006-01-28 00:52:46 $
 * $State: Exp $
 */
package com.sun.media.imageioimpl.plugins.tiff;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.TIFFTag;
import com.sun.media.imageio.plugins.tiff.TIFFTagSet;

public class TIFFIFD {

    /**
     * The largest low-valued tag number in the TIFF 6.0 specification.
     */
    private static final int MAX_LOW_FIELD_TAG_NUM =
        BaselineTIFFTagSet.TAG_REFERENCE_BLACK_WHITE;

    private List tagSets;
    private TIFFTag parentTag;

    private TIFFField[] lowFields = new TIFFField[MAX_LOW_FIELD_TAG_NUM + 1];
    private int numLowFields = 0;
    private Map highFields = new TreeMap();

    private long stripOrTileByteCountsPosition;
    private long stripOrTileOffsetsPosition;
    private long lastPosition;

    public TIFFIFD(List tagSets, TIFFTag parentTag) {
        this.tagSets = tagSets;
        this.parentTag = parentTag;
    }

    public TIFFIFD(List tagSets) {
        this(tagSets, null);
    }

    public List getTagSets() {
        return tagSets;
    }

    public TIFFTag getParentTag() {
        return parentTag;
    }

    /**
     * Returns an <code>Iterator</code> over the TIFF fields. The
     * traversal is in the order of increasing tag number.
     */
    // Note: the sort is guaranteed for low fields by the use of an
    // array wherein the index corresponds to the tag number and for
    // the high fields by the use of a TreeMap with tag number keys.
    public Iterator iterator() {
        List fields = new ArrayList(numLowFields + highFields.size());
        int len = lowFields.length;
        for(int i = 0; i < len; i++) {
            TIFFField f = lowFields[i];
            if(f != null) {
                fields.add(f);
            }
        }
        fields.addAll(highFields.values());
        return fields.iterator();
    }

    public static TIFFTag getTag(int tagNumber, List tagSets) {
        Iterator iter = tagSets.iterator();
        while (iter.hasNext()) {
            TIFFTagSet tagSet = (TIFFTagSet)iter.next();
            TIFFTag tag = tagSet.getTag(tagNumber);
            if (tag != null) {
                return tag;
            }
        }

        return null;
    }

    public TIFFTag getTag(int tagNumber) {
        return getTag(tagNumber, tagSets);
    }

    public static TIFFTag getTag(String tagName, List tagSets) {
        Iterator iter = tagSets.iterator();
        while (iter.hasNext()) {
            TIFFTagSet tagSet = (TIFFTagSet)iter.next();
            TIFFTag tag = tagSet.getTag(tagName);
            if (tag != null) {
                return tag;
            }
        }

        return null;
    }

    // Stream position initially at beginning, left at end
    // if ignoreUnknownFields is true, do not load fields for which
    // a tag cannot be found in an allowed TagSet.
    public void initialize(ImageInputStream stream,
                           boolean ignoreUnknownFields) throws IOException {
        Arrays.fill(lowFields, (Object)null);
        numLowFields = 0;
        highFields.clear();
        
        int numEntries = stream.readUnsignedShort();
        for (int i = 0; i < numEntries; i++) {
            // Read tag number, value type, and value count.
            int tag = stream.readUnsignedShort();
            int type = stream.readUnsignedShort();
            int count = (int)stream.readUnsignedInt();

            // Get the associated TIFFTag.
            TIFFTag tiffTag = getTag(tag, tagSets);

            // Ignore unknown fields.
            if(ignoreUnknownFields && tiffTag == null) {
                // Skip the value/offset so as to leave the stream
                // position at the start of the next IFD entry.
                stream.skipBytes(4);

                // XXX Warning message ...

                // Continue with the next IFD entry.
                continue;
            }
            
            long nextTagOffset = stream.getStreamPosition() + 4;
            
            int sizeOfType = TIFFTag.getSizeOfType(type);
            if (count*sizeOfType > 4) {
                long value = stream.readUnsignedInt();
                stream.seek(value);
            }
            
            if (tag == BaselineTIFFTagSet.TAG_STRIP_BYTE_COUNTS ||
                tag == BaselineTIFFTagSet.TAG_TILE_BYTE_COUNTS) {
                this.stripOrTileByteCountsPosition =
                    stream.getStreamPosition();
            } else if (tag == BaselineTIFFTagSet.TAG_STRIP_OFFSETS ||
                       tag == BaselineTIFFTagSet.TAG_TILE_OFFSETS) {
                this.stripOrTileOffsetsPosition =
                    stream.getStreamPosition();
            }

            Object obj = null;

            try {
                switch (type) {
                case TIFFTag.TIFF_BYTE:
                case TIFFTag.TIFF_SBYTE:
                case TIFFTag.TIFF_UNDEFINED:
                case TIFFTag.TIFF_ASCII:
                    byte[] bvalues = new byte[count];
                    stream.readFully(bvalues, 0, count);
                
                    if (type == TIFFTag.TIFF_ASCII) {
                        // Can be multiple strings
                        Vector v = new Vector();
                        boolean inString = false;
                        int prevIndex = 0;
                        for (int index = 0; index <= count; index++) {
                            if (index < count && bvalues[index] != 0) {
                                if (!inString) {
                                // start of string
                                    prevIndex = index;
                                    inString = true;
                                }
                            } else { // null or special case at end of string
                                if (inString) {
                                // end of string
                                    String s = new String(bvalues, prevIndex,
                                                          index - prevIndex);
                                    v.add(s);
                                    inString = false;
                                }
                            }
                        }

                        count = v.size();
                        String strings[] = new String[count];
                        for (int c = 0 ; c < count; c++) {
                            strings[c] = (String)v.elementAt(c);
                        }
                    
                        obj = strings;
                    } else {
                        obj = bvalues;
                    }
                    break;
                
                case TIFFTag.TIFF_SHORT:
                    char[] cvalues = new char[count];
                    for (int j = 0; j < count; j++) {
                        cvalues[j] = (char)(stream.readUnsignedShort());
                    }
                    obj = cvalues;
                    break;
                
                case TIFFTag.TIFF_LONG:
                case TIFFTag.TIFF_IFD_POINTER:
                    long[] lvalues = new long[count];
                    for (int j = 0; j < count; j++) {
                        lvalues[j] = stream.readUnsignedInt();
                    }
                    obj = lvalues;
                    break;
                
                case TIFFTag.TIFF_RATIONAL:
                    long[][] llvalues = new long[count][2];
                    for (int j = 0; j < count; j++) {
                        llvalues[j][0] = stream.readUnsignedInt();
                        llvalues[j][1] = stream.readUnsignedInt();
                    }
                    obj = llvalues;
                    break;
                
                case TIFFTag.TIFF_SSHORT:
                    short[] svalues = new short[count];
                    for (int j = 0; j < count; j++) {
                        svalues[j] = stream.readShort();
                    }
                    obj = svalues;
                    break;
                
                case TIFFTag.TIFF_SLONG:
                    int[] ivalues = new int[count];
                    for (int j = 0; j < count; j++) {
                        ivalues[j] = stream.readInt();
                    }
                    obj = ivalues;
                    break;
                
                case TIFFTag.TIFF_SRATIONAL:
                    int[][] iivalues = new int[count][2];
                    for (int j = 0; j < count; j++) {
                        iivalues[j][0] = stream.readInt();
                        iivalues[j][1] = stream.readInt();
                    }
                    obj = iivalues;
                    break;
                
                case TIFFTag.TIFF_FLOAT:
                    float[] fvalues = new float[count];
                    for (int j = 0; j < count; j++) {
                        fvalues[j] = stream.readFloat();
                    }
                    obj = fvalues;
                    break;
                
                case TIFFTag.TIFF_DOUBLE:
                    double[] dvalues = new double[count];
                    for (int j = 0; j < count; j++) {
                        dvalues[j] = stream.readDouble();
                    }
                    obj = dvalues;
                    break;
                
                default:
                    // XXX Warning
                    break;
                }
            } catch(EOFException eofe) {
                // The TIFF 6.0 fields have tag numbers less than or equal
                // to 532 (ReferenceBlackWhite) or equal to 33432 (Copyright).
                // If there is an error reading a baseline tag, then re-throw
                // the exception and fail; otherwise continue with the next
                // field.
                if(tag <= MAX_LOW_FIELD_TAG_NUM ||
                   tag == BaselineTIFFTagSet.TAG_COPYRIGHT) {
                    throw eofe;
                }
            }
            
            if (tiffTag == null) {
                // XXX Warning: unknown tag
            } else if (!tiffTag.isDataTypeOK(type)) {
                // XXX Warning: bad data type
            } else if (tiffTag.isIFDPointer() && obj != null) {
                stream.mark();
                stream.seek(((long[])obj)[0]);

                List tagSets = new ArrayList(1);
                tagSets.add(tiffTag.getTagSet());
                TIFFIFD subIFD = new TIFFIFD(tagSets);

                // XXX Use same ignore policy for sub-IFD fields?
                subIFD.initialize(stream, ignoreUnknownFields);
                obj = subIFD;
                stream.reset();
            }

            if (tiffTag == null) {
                tiffTag = new TIFFTag(null, tag, 1 << type, null);
            }

            // Add the field if its contents have been initialized which
            // will not be the case if an EOF was ignored above.
            if(obj != null) {
                TIFFField f = new TIFFField(tiffTag, type, count, obj);
                addTIFFField(f);
            }

            stream.seek(nextTagOffset);
        }

        this.lastPosition = stream.getStreamPosition();
    }

    public void writeToStream(ImageOutputStream stream)
        throws IOException {

        int numFields = numLowFields + highFields.size();
        stream.writeShort(numFields);

        long nextSpace = stream.getStreamPosition() + 12*numFields + 4;

        Iterator iter = iterator();
        while (iter.hasNext()) {
            TIFFField f = (TIFFField)iter.next();
            
            TIFFTag tag = f.getTag();

            int type = f.getType();
            int count = f.getCount();

            // Hack to deal with unknown tags
            if (type == 0) {
                type = TIFFTag.TIFF_UNDEFINED;
            }
            int size = count*TIFFTag.getSizeOfType(type);

            if (type == TIFFTag.TIFF_ASCII) {
                int chars = 0;
                for (int i = 0; i < count; i++) {
                    chars += f.getAsString(i).length() + 1;
                }
                count = chars;
                size = count;
            }

            int tagNumber = f.getTagNumber();
            stream.writeShort(tagNumber);
            stream.writeShort(type);
            stream.writeInt(count);

            // Write a dummy value to fill space
            stream.writeInt(0);
            stream.mark(); // Mark beginning of next field
            stream.skipBytes(-4);

            long pos;

            if (size > 4 || tag.isIFDPointer()) {
                if (tag.isIFDPointer()) {
                    // Ensure IFD is written on a word boundary
                    nextSpace = (nextSpace + 3) & ~0x3;
                }
                stream.writeInt((int)nextSpace);
                stream.seek(nextSpace);
                pos = nextSpace;

                if (tag.isIFDPointer()) {
                    TIFFIFD subIFD = (TIFFIFD)f.getData();
                    subIFD.writeToStream(stream);
                    nextSpace = subIFD.lastPosition;
                } else {
                    f.writeData(stream);
                    nextSpace = stream.getStreamPosition();
                }
            } else {
                pos = stream.getStreamPosition();
                f.writeData(stream);
            }

            // If we are writing the data for the
            // StripByteCounts, TileByteCounts, StripOffsets,
            // or TileOffsets fields, record the current stream
            // position for backpatching
            if (tagNumber ==
                BaselineTIFFTagSet.TAG_STRIP_BYTE_COUNTS ||
                tagNumber == BaselineTIFFTagSet.TAG_TILE_BYTE_COUNTS) {
                this.stripOrTileByteCountsPosition = pos;
            } else if (tagNumber ==
                       BaselineTIFFTagSet.TAG_STRIP_OFFSETS ||
                       tagNumber ==
                       BaselineTIFFTagSet.TAG_TILE_OFFSETS) {
                this.stripOrTileOffsetsPosition = pos;
            }

            stream.reset(); // Go to marked position of next field
        }
        
        this.lastPosition = nextSpace;
    }

    public long getStripOrTileByteCountsPosition() {
        return stripOrTileByteCountsPosition;
    }

    public long getStripOrTileOffsetsPosition() {
        return stripOrTileOffsetsPosition;
    }

    public long getLastPosition() {
        return lastPosition;
    }

    public void addTIFFField(TIFFField f) {
        int tagNumber = f.getTagNumber();
        if(tagNumber <= MAX_LOW_FIELD_TAG_NUM) {
            if(lowFields[tagNumber] == null) {
                numLowFields++;
            }
            lowFields[tagNumber] = f;
        } else {
            highFields.put(new Integer(tagNumber), f);
        }
    }

    public TIFFField getTIFFField(int tagNumber) {
        TIFFField f;
        if(tagNumber <= MAX_LOW_FIELD_TAG_NUM) {
            f = lowFields[tagNumber];
        } else {
            f = (TIFFField)highFields.get(new Integer(tagNumber));
        }
        return f;
    }

    public void removeTIFFField(int tagNumber) {
        if(tagNumber <= MAX_LOW_FIELD_TAG_NUM) {
            if(lowFields[tagNumber] != null) {
                numLowFields--;
                lowFields[tagNumber] = null;
            }
        } else {
            highFields.remove(new Integer(tagNumber));
        }
    }

    /**
     * Returns a <code>TIFFIFD</code> wherein all fields from the
     * <code>BaselineTIFFTagSet</code> are copied by value and all other
     * fields copied by reference.
     */
    public TIFFIFD getShallowClone() {
        // Get the baseline TagSet.
        TIFFTagSet baselineTagSet = BaselineTIFFTagSet.getInstance();

        // If the baseline TagSet is not included just return.
        if(!tagSets.contains(baselineTagSet)) {
            return this;
        }

        // Create a new object.
        TIFFIFD shallowClone = new TIFFIFD(tagSets, parentTag);

        // Get the tag numbers in the baseline set.
        Set baselineTagNumbers = baselineTagSet.getTagNumbers();

        // Iterate over the fields in this IFD.
        Iterator fields = iterator();
        while(fields.hasNext()) {
            // Get the next field.
            TIFFField field = (TIFFField)fields.next();

            // Get its tag number.
            Integer tagNumber = new Integer(field.getTagNumber());

            // Branch based on membership in baseline set.
            TIFFField fieldClone;
            if(baselineTagNumbers.contains(tagNumber)) {
                // Copy by value.
                Object fieldData = field.getData();

                int fieldType = field.getType();

                try {
                    switch (fieldType) {
                    case TIFFTag.TIFF_BYTE:
                    case TIFFTag.TIFF_SBYTE:
                    case TIFFTag.TIFF_UNDEFINED:
                        fieldData = ((byte[])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_ASCII:
                        fieldData = ((String[])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_SHORT:
                        fieldData = ((char[])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_LONG:
                    case TIFFTag.TIFF_IFD_POINTER:
                        fieldData = ((long[])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_RATIONAL:
                        fieldData = ((long[][])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_SSHORT:
                        fieldData = ((short[])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_SLONG:
                        fieldData = ((int[])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_SRATIONAL:
                        fieldData = ((int[][])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_FLOAT:
                        fieldData = ((float[])fieldData).clone();
                        break;
                    case TIFFTag.TIFF_DOUBLE:
                        fieldData = ((double[])fieldData).clone();
                        break;
                    default:
                        // Shouldn't happen but do nothing ...
                    }
                } catch(Exception e) {
                    // Ignore it and copy by reference ...
                }

                fieldClone = new TIFFField(field.getTag(), fieldType,
                                           field.getCount(), fieldData);
            } else {
                // Copy by reference.
                fieldClone = field;
            }

            // Add the field to the clone.
            shallowClone.addTIFFField(fieldClone);
        }

        return shallowClone;
    }
}
