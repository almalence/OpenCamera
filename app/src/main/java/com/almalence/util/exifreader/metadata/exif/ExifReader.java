/*
 * Copyright 2002-2012 Drew Noakes
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 * More information about this project is available at:
 *
 *    http://drewnoakes.com/code/exif/
 *    http://code.google.com/p/metadata-extractor/
 */
package com.almalence.util.exifreader.metadata.exif;

import com.almalence.util.exifreader.lang.BufferBoundsException;
import com.almalence.util.exifreader.lang.BufferReader;
import com.almalence.util.exifreader.lang.Rational;
import com.almalence.util.exifreader.lang.annotations.NotNull;
import com.almalence.util.exifreader.metadata.Directory;
import com.almalence.util.exifreader.metadata.Metadata;
import com.almalence.util.exifreader.metadata.MetadataReader;

import java.util.HashSet;
import java.util.Set;

/**
 * Decodes Exif binary data, populating a {@link Metadata} object with tag values in {@link ExifSubIFDDirectory},
 * {@link ExifThumbnailDirectory}, {@link ExifInteropDirectory}, {@link GpsDirectory} and one of the many camera makernote directories.
 *
 * @author Drew Noakes http://drewnoakes.com
 */
public class ExifReader implements MetadataReader
{
    //  extract a reusable TiffReader from this class with hooks for special tag handling and subdir following
    
    /** The number of bytes used per format descriptor. */
    @NotNull
    private static final int[] BYTES_PER_FORMAT = { 0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8 };

    /** The number of formats known. */
    private static final int MAX_FORMAT_CODE = 12;

    // Format types
    //  use an enum for these?
    /** An 8-bit unsigned integer. */
    private static final int FMT_BYTE = 1;
    /** A fixed-length character string. */
    private static final int FMT_STRING = 2;
    /** An unsigned 16-bit integer. */
    private static final int FMT_USHORT = 3;
    /** An unsigned 32-bit integer. */
    private static final int FMT_ULONG = 4;
    private static final int FMT_URATIONAL = 5;
    /** An 8-bit signed integer. */
    private static final int FMT_SBYTE = 6;
    private static final int FMT_UNDEFINED = 7;
    /** A signed 16-bit integer. */
    private static final int FMT_SSHORT = 8;
    /** A signed 32-bit integer. */
    private static final int FMT_SLONG = 9;
    private static final int FMT_SRATIONAL = 10;
    /** A 32-bit floating point number. */
    private static final int FMT_SINGLE = 11;
    /** A 64-bit floating point number. */
    private static final int FMT_DOUBLE = 12;

    /** This tag is a pointer to the Exif SubIFD. */
    public static final int TAG_EXIF_SUB_IFD_OFFSET = 0x8769;
    /** This tag is a pointer to the Exif Interop IFD. */
    public static final int TAG_INTEROP_OFFSET = 0xA005;
    /** This tag is a pointer to the Exif GPS IFD. */
    public static final int TAG_GPS_INFO_OFFSET = 0x8825;
    /** This tag is a pointer to the Exif Makernote IFD. */
    public static final int TAG_MAKER_NOTE_OFFSET = 0x927C;

    public static final int TIFF_HEADER_START_OFFSET = 6;

    /**
     * Performs the Exif data extraction, adding found values to the specified
     * instance of <code>Metadata</code>.
     *
     * @param reader   The buffer reader from which Exif data should be read.
     * @param metadata The Metadata object into which extracted values should be merged.
     */
    public void extract(@NotNull final BufferReader reader, @NotNull Metadata metadata)
    {
        final ExifSubIFDDirectory directory = metadata.getOrCreateDirectory(ExifSubIFDDirectory.class);

        // check for the header length
        if (reader.getLength() <= 14) {
            directory.addError("Exif data segment must contain at least 14 bytes");
            return;
        }

        // check for the header preamble
        try {
            if (!reader.getString(0, 6).equals("Exif\0\0")) {
                directory.addError("Exif data segment doesn't begin with 'Exif'");
                return;
            }

            extractIFD(metadata, metadata.getOrCreateDirectory(ExifIFD0Directory.class), TIFF_HEADER_START_OFFSET, reader);
        } catch (BufferBoundsException e) {
            directory.addError("Exif data segment ended prematurely");
        }
    }

    /**
     * Performs the Exif data extraction on a TIFF/RAW, adding found values to the specified
     * instance of <code>Metadata</code>.
     *
     * @param reader   The BufferReader from which TIFF data should be read.
     * @param metadata The Metadata object into which extracted values should be merged.
     */
    public void extractTiff(@NotNull BufferReader reader, @NotNull Metadata metadata)
    {
        final ExifIFD0Directory directory = metadata.getOrCreateDirectory(ExifIFD0Directory.class);

        try {
            extractIFD(metadata, directory, 0, reader);
        } catch (BufferBoundsException e) {
            directory.addError("Exif data segment ended prematurely");
        }
    }

    private void extractIFD(@NotNull Metadata metadata, @NotNull final ExifIFD0Directory directory, int tiffHeaderOffset, @NotNull BufferReader reader) throws BufferBoundsException
    {
        // this should be either "MM" or "II"
        String byteOrderIdentifier = reader.getString(tiffHeaderOffset, 2);

        if ("MM".equals(byteOrderIdentifier)) {
            reader.setMotorolaByteOrder(true);
        } else if ("II".equals(byteOrderIdentifier)) {
            reader.setMotorolaByteOrder(false);
        } else {
            directory.addError("Unclear distinction between Motorola/Intel byte ordering: " + byteOrderIdentifier);
            return;
        }

        // Check the next two values for correctness.
        final int tiffMarker = reader.getUInt16(2 + tiffHeaderOffset);

        final int standardTiffMarker = 0x002A;
        final int olympusRawTiffMarker = 0x4F52; // for ORF files
        final int panasonicRawTiffMarker = 0x0055; // for RW2 files

        if (tiffMarker != standardTiffMarker && tiffMarker != olympusRawTiffMarker && tiffMarker != panasonicRawTiffMarker) {
            directory.addError("Unexpected TIFF marker after byte order identifier: 0x" + Integer.toHexString(tiffMarker));
            return;
        }

        int firstDirectoryOffset = reader.getInt32(4 + tiffHeaderOffset) + tiffHeaderOffset;

        // David Ekholm sent a digital camera image that has this problem
        if (firstDirectoryOffset >= reader.getLength() - 1) {
            directory.addError("First exif directory offset is beyond end of Exif data segment");
            // First directory normally starts 14 bytes in -- try it here and catch another error in the worst case
            firstDirectoryOffset = 14;
        }

        Set<Integer> processedDirectoryOffsets = new HashSet<Integer>();

        processDirectory(directory, processedDirectoryOffsets, firstDirectoryOffset, tiffHeaderOffset, metadata, reader);
    }

    /**
     * Process one of the nested Tiff IFD directories.
     * <p/>
     * Header
     * 2 bytes: number of tags
     * <p/>
     * Then for each tag
     * 2 bytes: tag type
     * 2 bytes: format code
     * 4 bytes: component count
     */
    private void processDirectory(@NotNull Directory directory, @NotNull Set<Integer> processedDirectoryOffsets, int dirStartOffset, int tiffHeaderOffset, @NotNull final Metadata metadata, @NotNull final BufferReader reader) throws BufferBoundsException
    {
        // check for directories we've already visited to avoid stack overflows when recursive/cyclic directory structures exist
        if (processedDirectoryOffsets.contains(Integer.valueOf(dirStartOffset)))
            return;

        // remember that we've visited this directory so that we don't visit it again later
        processedDirectoryOffsets.add(dirStartOffset);

        if (dirStartOffset >= reader.getLength() || dirStartOffset < 0) {
            directory.addError("Ignored directory marked to start outside data segment");
            return;
        }

        // First two bytes in the IFD are the number of tags in this directory
        int dirTagCount = reader.getUInt16(dirStartOffset);

        int dirLength = (2 + (12 * dirTagCount) + 4);
        if (dirLength + dirStartOffset > reader.getLength()) {
            directory.addError("Illegally sized directory");
            return;
        }

        // Handle each tag in this directory
        for (int tagNumber = 0; tagNumber < dirTagCount; tagNumber++) {
            final int tagOffset = calculateTagOffset(dirStartOffset, tagNumber);

            // 2 bytes for the tag type
            final int tagType = reader.getUInt16(tagOffset);

            // 2 bytes for the format code
            final int formatCode = reader.getUInt16(tagOffset + 2);
            if (formatCode < 1 || formatCode > MAX_FORMAT_CODE) {
                // This error suggests that we are processing at an incorrect index and will generate
                // rubbish until we go out of bounds (which may be a while).  Exit now.
                directory.addError("Invalid TIFF tag format code: " + formatCode);
                return;
            }

            // 4 bytes dictate the number of components in this tag's data
            final int componentCount = reader.getInt32(tagOffset + 4);
            if (componentCount < 0) {
                directory.addError("Negative TIFF tag component count");
                continue;
            }
            // each component may have more than one byte... calculate the total number of bytes
            final int byteCount = componentCount * BYTES_PER_FORMAT[formatCode];
            final int tagValueOffset;
            if (byteCount > 4) {
                // If it's bigger than 4 bytes, the dir entry contains an offset.
                // dirEntryOffset must be passed, as some makernote implementations (e.g. FujiFilm) incorrectly use an
                // offset relative to the start of the makernote itself, not the TIFF segment.
                final int offsetVal = reader.getInt32(tagOffset + 8);
                if (offsetVal + byteCount > reader.getLength()) {
                    // Bogus pointer offset and / or byteCount value
                    directory.addError("Illegal TIFF tag pointer offset");
                    continue;
                }
                tagValueOffset = tiffHeaderOffset + offsetVal;
            } else {
                // 4 bytes or less and value is in the dir entry itself
                tagValueOffset = tagOffset + 8;
            }

            if (tagValueOffset < 0 || tagValueOffset > reader.getLength()) {
                directory.addError("Illegal TIFF tag pointer offset");
                continue;
            }

            // Check that this tag isn't going to allocate outside the bounds of the data array.
            // This addresses an uncommon OutOfMemoryError.
            if (byteCount < 0 || tagValueOffset + byteCount > reader.getLength()) {
                directory.addError("Illegal number of bytes: " + byteCount);
                continue;
            }

            switch (tagType) {
                case TAG_EXIF_SUB_IFD_OFFSET: {
                    final int subdirOffset = tiffHeaderOffset + reader.getInt32(tagValueOffset);
                    processDirectory(metadata.getOrCreateDirectory(ExifSubIFDDirectory.class), processedDirectoryOffsets, subdirOffset, tiffHeaderOffset, metadata, reader);
                    continue;
                }                             
                case TAG_MAKER_NOTE_OFFSET: {
                    processMakerNote(tagValueOffset, processedDirectoryOffsets, tiffHeaderOffset, metadata, reader);
                    continue;
                }
                default: {
                    processTag(directory, tagType, tagValueOffset, componentCount, formatCode, reader);
                    break;
                }
            }
        }

        // at the end of each IFD is an optional link to the next IFD
        final int finalTagOffset = calculateTagOffset(dirStartOffset, dirTagCount);
        int nextDirectoryOffset = reader.getInt32(finalTagOffset);
        if (nextDirectoryOffset != 0) {
            nextDirectoryOffset += tiffHeaderOffset;
            if (nextDirectoryOffset >= reader.getLength()) {
                // Last 4 bytes of IFD reference another IFD with an address that is out of bounds
                // Note this could have been caused by jhead 1.3 cropping too much
                return;
            } else if (nextDirectoryOffset < dirStartOffset) {
                // Last 4 bytes of IFD reference another IFD with an address that is before the start of this directory
                return;
            }
        }
    }

    private void processMakerNote(int subdirOffset, @NotNull Set<Integer> processedDirectoryOffsets, int tiffHeaderOffset, @NotNull final Metadata metadata, @NotNull BufferReader reader) throws BufferBoundsException
    {
        // Determine the camera model and makernote format
        Directory ifd0Directory = metadata.getDirectory(ExifIFD0Directory.class);

        if (ifd0Directory==null)
            return;
    }

    private void processTag(@NotNull Directory directory, int tagType, int tagValueOffset, int componentCount, int formatCode, @NotNull final BufferReader reader) throws BufferBoundsException
    {
        // Directory simply stores raw values
        // The display side uses a Descriptor class per directory to turn the raw values into 'pretty' descriptions
        switch (formatCode) {
            case FMT_UNDEFINED:
                // this includes exif user comments
                directory.setByteArray(tagType, reader.getBytes(tagValueOffset, componentCount));
                break;
            case FMT_STRING:
                String string = reader.getNullTerminatedString(tagValueOffset, componentCount);
                directory.setString(tagType, string);
                break;
            case FMT_SRATIONAL:
                if (componentCount == 1) {
                    directory.setRational(tagType, new Rational(reader.getInt32(tagValueOffset), reader.getInt32(tagValueOffset + 4)));
                } else if (componentCount > 1) {
                    Rational[] rationals = new Rational[componentCount];
                    for (int i = 0; i < componentCount; i++)
                        rationals[i] = new Rational(reader.getInt32(tagValueOffset + (8 * i)), reader.getInt32(tagValueOffset + 4 + (8 * i)));
                    directory.setRationalArray(tagType, rationals);
                }
                break;
            case FMT_URATIONAL:
                if (componentCount == 1) {
                    directory.setRational(tagType, new Rational(reader.getUInt32(tagValueOffset), reader.getUInt32(tagValueOffset + 4)));
                } else if (componentCount > 1) {
                    Rational[] rationals = new Rational[componentCount];
                    for (int i = 0; i < componentCount; i++)
                        rationals[i] = new Rational(reader.getUInt32(tagValueOffset + (8 * i)), reader.getUInt32(tagValueOffset + 4 + (8 * i)));
                    directory.setRationalArray(tagType, rationals);
                }
                break;
            case FMT_SINGLE:
                if (componentCount == 1) {
                    directory.setFloat(tagType, reader.getFloat32(tagValueOffset));
                } else {
                    float[] floats = new float[componentCount];
                    for (int i = 0; i < componentCount; i++)
                        floats[i] = reader.getFloat32(tagValueOffset + (i * 4));
                    directory.setFloatArray(tagType, floats);
                }
                break;
            case FMT_DOUBLE:
                if (componentCount == 1) {
                    directory.setDouble(tagType, reader.getDouble64(tagValueOffset));
                } else {
                    double[] doubles = new double[componentCount];
                    for (int i = 0; i < componentCount; i++)
                        doubles[i] = reader.getDouble64(tagValueOffset + (i * 4));
                    directory.setDoubleArray(tagType, doubles);
                }
                break;

            //
            // Note that all integral types are stored as int32 internally (the largest supported by TIFF)
            //

            case FMT_SBYTE:
                if (componentCount == 1) {
                    directory.setInt(tagType, reader.getInt8(tagValueOffset));
                } else {
                    int[] bytes = new int[componentCount];
                    for (int i = 0; i < componentCount; i++)
                        bytes[i] = reader.getInt8(tagValueOffset + i);
                    directory.setIntArray(tagType, bytes);
                }
                break;
            case FMT_BYTE:
                if (componentCount == 1) {
                    directory.setInt(tagType, reader.getUInt8(tagValueOffset));
                } else {
                    int[] bytes = new int[componentCount];
                    for (int i = 0; i < componentCount; i++)
                        bytes[i] = reader.getUInt8(tagValueOffset + i);
                    directory.setIntArray(tagType, bytes);
                }
                break;
            case FMT_USHORT:
                if (componentCount == 1) {
                    int i = reader.getUInt16(tagValueOffset);
                    directory.setInt(tagType, i);
                } else {
                    int[] ints = new int[componentCount];
                    for (int i = 0; i < componentCount; i++)
                        ints[i] = reader.getUInt16(tagValueOffset + (i * 2));
                    directory.setIntArray(tagType, ints);
                }
                break;
            case FMT_SSHORT:
                if (componentCount == 1) {
                    int i = reader.getInt16(tagValueOffset);
                    directory.setInt(tagType, i);
                } else {
                    int[] ints = new int[componentCount];
                    for (int i = 0; i < componentCount; i++)
                        ints[i] = reader.getInt16(tagValueOffset + (i * 2));
                    directory.setIntArray(tagType, ints);
                }
                break;
            case FMT_SLONG:
            case FMT_ULONG:
                // NOTE 'long' in this case means 32 bit, not 64
                if (componentCount == 1) {
                    int i = reader.getInt32(tagValueOffset);
                    directory.setInt(tagType, i);
                } else {
                    int[] ints = new int[componentCount];
                    for (int i = 0; i < componentCount; i++)
                        ints[i] = reader.getInt32(tagValueOffset + (i * 4));
                    directory.setIntArray(tagType, ints);
                }
                break;
            default:
                directory.addError("Unknown format code " + formatCode + " for tag " + tagType);
        }
    }

    /**
     * Determine the offset at which a given InteropArray entry begins within the specified IFD.
     *
     * @param dirStartOffset the offset at which the IFD starts
     * @param entryNumber    the zero-based entry number
     */
    private int calculateTagOffset(int dirStartOffset, int entryNumber)
    {
        // add 2 bytes for the tag count
        // each entry is 12 bytes, so we skip 12 * the number seen so far
        return dirStartOffset + 2 + (12 * entryNumber);
    }
}
