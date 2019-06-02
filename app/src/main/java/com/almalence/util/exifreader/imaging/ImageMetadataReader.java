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
package com.almalence.util.exifreader.imaging;

import com.almalence.util.exifreader.imaging.jpeg.JpegMetadataReader;
import com.almalence.util.exifreader.lang.annotations.NotNull;
import com.almalence.util.exifreader.lang.annotations.Nullable;
import com.almalence.util.exifreader.metadata.Directory;
import com.almalence.util.exifreader.metadata.Metadata;
import com.almalence.util.exifreader.metadata.MetadataException;
import com.almalence.util.exifreader.metadata.Tag;
import com.almalence.util.exifreader.metadata.exif.ExifIFD0Directory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Obtains metadata from all supported file formats, including JPEG, RAW (NEF/CRw/CR2) and TIFF.
 * If the caller knows their file to be of a particular type, they may prefer to use the dedicated MetadataReader
 * directly (<code>JpegMetadataReader</code> for Jpeg files, or <code>TiffMetadataReader</code> for TIFF and RAW files).
 * The dedicated readers offer a very slight performance improvement, though for most scenarios it is simpler,
 * more convenient and more robust to use this class.
 *
 * @author Drew Noakes http://drewnoakes.com
 */
public class ImageMetadataReader
{
    private static final int JPEG_FILE_MAGIC_NUMBER = 0xFFD8;
    private static final int MOTOROLA_TIFF_MAGIC_NUMBER = 0x4D4D;  // "MM"
    private static final int INTEL_TIFF_MAGIC_NUMBER = 0x4949;     // "II"
    private static final int PSD_MAGIC_NUMBER = 0x3842;            // "8B" note that the full magic number is 8BPS

    /**
     * Reads metadata from an input stream.  The file inputStream examined to determine its type and consequently the
     * appropriate method to extract the data, though this inputStream transparent to the caller.
     *
     * @param inputStream a stream from which the image data may be read.  The stream must be positioned at the
     *                    beginning of the image data.
     * @return a populated Metadata object containing directories of tags with values and any processing errors.
     * @throws ImageProcessingException for general processing errors.
     */
    @NotNull
    public static Metadata readMetadata(@NotNull BufferedInputStream inputStream, boolean waitForBytes) throws ImageProcessingException, IOException
    {
        int magicNumber = readMagicNumber(inputStream);
        return readMetadata(inputStream, null, magicNumber, waitForBytes);
    }

    /**
     * Reads metadata from a file.  The file is examined to determine its type and consequently the appropriate
     * method to extract the data, though this is transparent to the caller.
     *
     * @param file a file from which the image data may be read.
     * @return a populated Metadata object containing directories of tags with values and any processing errors.
     * @throws ImageProcessingException for general processing errors.
     */
    @NotNull
    public static Metadata readMetadata(@NotNull File file) throws ImageProcessingException, IOException
    {
        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));

        int magicNumber;
        try {
            magicNumber = readMagicNumber(inputStream);
        } finally {
            inputStream.close();
        }

        return readMetadata(null, file, magicNumber, false);
    }

    @NotNull
    private static Metadata readMetadata(@Nullable BufferedInputStream inputStream, @Nullable File file, int magicNumber, boolean waitForBytes) throws ImageProcessingException, IOException
    {
        assert(file!=null ^ inputStream!=null);
        
        // This covers all JPEG files
        if ((magicNumber & JPEG_FILE_MAGIC_NUMBER) == JPEG_FILE_MAGIC_NUMBER) {
            if (inputStream != null)
                return JpegMetadataReader.readMetadata(inputStream, waitForBytes);
            else
                return JpegMetadataReader.readMetadata(file);
        }

        throw new ImageProcessingException("File format is not supported");
    }

    private static int readMagicNumber(@NotNull BufferedInputStream inputStream) throws IOException
    {
        inputStream.mark(2);
        int magicNumber = inputStream.read() << 8 | inputStream.read();
        inputStream.reset();
        return magicNumber;
    }

    private ImageMetadataReader() throws Exception
    {
        throw new Exception("Not intended for instantiation");
    }

    /**
     * An application entry point.  Takes the name of one or more files as arguments and prints the contents of all
     * metadata directories to System.out.  If <code>/thumb</code> is passed, then any thumbnail data will be
     * written to a file with name of the input file having '.thumb.jpg' appended.
     *
     * @param args the command line arguments
     */
    public static void main(@NotNull String[] args) throws MetadataException, IOException
    {
        Collection<String> argList = new ArrayList<String>(Arrays.asList(args));
        boolean thumbRequested = argList.remove("/thumb");
        boolean wikiFormat = argList.remove("/wiki");

        if (argList.size() < 1) {
            System.out.println("Usage: java -jar metadata-extractor-a.b.c.jar <filename> [<filename>] [/thumb] [/wiki]");
            System.exit(1);
        }

        for (String filePath : argList) {
            long startTime = System.nanoTime();
            File file = new File(filePath);

            if (!wikiFormat && argList.size()>1)
                System.out.println("***** PROCESSING: " + filePath);

            Metadata metadata = null;
            try {
                metadata = ImageMetadataReader.readMetadata(file);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
            long took = System.nanoTime() - startTime;
            if (!wikiFormat)
                System.out.println("Processed " + (file.length()/(1024d*1024)) + "MB file in " + (took / 1000000d) + "ms");

            if (wikiFormat) {
                String fileName = file.getName();
                String urlName = fileName.replace(" ", "%20"); // How to do this using framework?
                ExifIFD0Directory exifIFD0Directory = metadata.getOrCreateDirectory(ExifIFD0Directory.class);
                String make = escapeForWiki(exifIFD0Directory.getString(ExifIFD0Directory.TAG_MAKE));
                String model = escapeForWiki(exifIFD0Directory.getString(ExifIFD0Directory.TAG_MODEL));
                System.out.println();
                System.out.println("-----");
                System.out.println();
                System.out.printf("= %s - %s =%n", make, model);
                System.out.println();
                System.out.printf("<a href=\"http://metadata-extractor.googlecode.com/svn/sample-images/%s\">%n", urlName);
                System.out.printf("<img src=\"http://metadata-extractor.googlecode.com/svn/sample-images/%s\" width=\"300\"/><br/>%n", urlName);
                System.out.println(fileName);
                System.out.println("</a>");
                System.out.println();
                System.out.println("|| *Directory* || *Tag Id* || *Tag Name* || *Tag Description* ||");
            }

            // iterate over the metadata and print to System.out
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String tagName = tag.getTagName();
                    String directoryName = directory.getName();
                    String description = tag.getDescription();

                    // truncate the description if it's too long
                    if (description != null && description.length() > 1024) {
                        description = description.substring(0, 1024) + "...";
                    }

                    if (wikiFormat) {
                        System.out.printf("||%s||0x%s||%s||%s||%n",
                                escapeForWiki(directoryName),
                                Integer.toHexString(tag.getTagType()),
                                escapeForWiki(tagName),
                                escapeForWiki(description));
                    }
                    else
                    {
                        System.out.printf("[%s] %s = %s%n", directoryName, tagName, description);
                    }
                }

                // print out any errors
                for (String error : directory.getErrors())
                    System.err.println("ERROR: " + error);
            }
        }
    }

    @Nullable
    private static String escapeForWiki(@Nullable String text)
    {
        if (text==null)
            return null;
        text = text.replaceAll("(\\W|^)(([A-Z][a-z0-9]+){2,})", "$1!$2");
        if (text!=null && text.length() > 120)
            text = text.substring(0, 120) + "...";
        if (text != null)
            text = text.replace("[", "`[`").replace("]", "`]`").replace("<", "`<`").replace(">", "`>`");
        return text;
    }
}
