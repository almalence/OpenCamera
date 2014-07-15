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
package com.almalence.util.exifreader.imaging.jpeg;

import com.almalence.util.exifreader.lang.ByteArrayReader;
import com.almalence.util.exifreader.lang.annotations.NotNull;
import com.almalence.util.exifreader.metadata.Metadata;
import com.almalence.util.exifreader.metadata.exif.ExifReader;
import com.almalence.util.exifreader.metadata.jpeg.JpegCommentReader;
import com.almalence.util.exifreader.metadata.jpeg.JpegDirectory;
import com.almalence.util.exifreader.metadata.jpeg.JpegReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Obtains all available metadata from Jpeg formatted files.
 *
 * @author Drew Noakes http://drewnoakes.com
 */
public class JpegMetadataReader
{
    // investigate supporting javax.imageio

    @NotNull
    public static Metadata readMetadata(@NotNull InputStream inputStream) throws JpegProcessingException
    {
        return readMetadata(inputStream, true);
    }

    @NotNull
    public static Metadata readMetadata(@NotNull InputStream inputStream, final boolean waitForBytes) throws JpegProcessingException
    {
        JpegSegmentReader segmentReader = new JpegSegmentReader(inputStream, waitForBytes);
        return extractMetadataFromJpegSegmentReader(segmentReader.getSegmentData());
    }

    @NotNull
    public static Metadata readMetadata(@NotNull File file) throws JpegProcessingException, IOException
    {
        JpegSegmentReader segmentReader = new JpegSegmentReader(file);
        return extractMetadataFromJpegSegmentReader(segmentReader.getSegmentData());
    }

    @NotNull
    public static Metadata extractMetadataFromJpegSegmentReader(@NotNull JpegSegmentData segmentReader)
    {
        final Metadata metadata = new Metadata();

        // Loop through looking for all SOFn segments.  When we find one, we know what type of compression
        // was used for the JPEG, and we can process the JPEG metadata in the segment too.
        for (byte i = 0; i < 16; i++) {
            // There are no SOF4 or SOF12 segments, so don't bother
            if (i == 4 || i == 12)
                continue;
            // Should never have more than one SOFn for a given 'n'.
            byte[] jpegSegment = segmentReader.getSegment((byte)(JpegSegmentReader.SEGMENT_SOF0 + i));
            if (jpegSegment == null)
                continue;
            JpegDirectory directory = metadata.getOrCreateDirectory(JpegDirectory.class);
            directory.setInt(JpegDirectory.TAG_JPEG_COMPRESSION_TYPE, i);
            new JpegReader().extract(new ByteArrayReader(jpegSegment), metadata);
            break;
        }

        // There should never be more than one COM segment.
        byte[] comSegment = segmentReader.getSegment(JpegSegmentReader.SEGMENT_COM);
        if (comSegment != null)
            new JpegCommentReader().extract(new ByteArrayReader(comSegment), metadata);

        
        // Loop through all APP1 segments, checking the leading bytes to identify the format of each.
        for (byte[] app1Segment : segmentReader.getSegments(JpegSegmentReader.SEGMENT_APP1)) {
            if (app1Segment.length > 3 && "EXIF".equalsIgnoreCase(new String(app1Segment, 0, 4)))
                new ExifReader().extract(new ByteArrayReader(app1Segment), metadata);            
        }

        return metadata;
    }

    private JpegMetadataReader() throws Exception
    {
        throw new Exception("Not intended for instantiation");
    }
}

