/*
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is collection of files collectively known as Open Camera.

The Initial Developer of the Original Code is Almalence Inc.
Portions created by Initial Developer are Copyright (C) 2013
by Almalence Inc. All Rights Reserved.
*/

#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#ifdef LOG_ON
#define LOG_TAG "MPOWriter"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__ )
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__ )
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__ )
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__ )
#else
#define LOG_TAG
#define LOGD(...)
#define LOGE(...)
#define LOGI(...)
#define LOGW(...)
#endif


#define MAX_MPO_FRAMES 10

// This triggers openmp constructors and destructors to be called upon library load/unload
void __attribute__((constructor)) initialize_openmp() {}
void __attribute__((destructor)) release_openmp() {}

static int iImageAmount = 0;

static unsigned char *inputFrame[MAX_MPO_FRAMES] = {NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL}; //JPEG data
static int            framesSizes[MAX_MPO_FRAMES] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}; //in bytes
static float          focusDistances[MAX_MPO_FRAMES] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}; //In diopter


/*static unsigned char markerStart = 0xff;

//Markers
static unsigned char SOIMarker  = 0xd8;
static unsigned char EOIMarker  = 0xd9;
static unsigned char DQTMarker  = 0xdb;
static unsigned char APP1Marker = 0xe1;

static unsigned char APP2Marker = 0xe2;

static unsigned int  MPFormatIdentifier = 0x0046504d; */


//sizes of the APP2 segment parts
#define APP2_HEADER_SIZE 8
#define MP_HEADER_SIZE 8
#define MP_INDEX_IFD_SIZE 42
#define MP_ENTRY_SIZE 16
#define MP_ATTR_FIRST_II_SIZE 26
#define MP_ATTR_SIZE 38

static unsigned int  BigEndian    = 0x2A004D4D;
static unsigned int  LittleEndian = 0x002A4949;

static unsigned int  FirstImageAttribute = 0x0800;
static unsigned int  DefaultImageAttribute = 0x0000;

// MP file APP2 segment tags
enum TAG_MP {
	TAG_MP_VERSION = 0xb000,
	TAG_MP_NUMBER_OF_IMAGES = 0xb001,
	TAG_MP_ENTRY = 0xb002,
	TAG_MP_INDIVIDUAL_FOCUS_DISTANCE = 0xb301
};

//jpeg format markers
enum MARKER_JPEG {
    M_SOI = 0xD8,
    M_APP1 = 0xE1,
    M_APP2 = 0xE2,
	M_DNL = 0xDC,
	M_DRI = 0xDD,
	M_EXP = 0xDF,
	M_TEM = 0x01,
	M_RST = 0xD0,
	M_EOI = 0xD9,

	M_START = 0xFF
};


#pragma pack(push,1)
//Mandatory 3 first tags
//8Byte
struct APP2Format
{
    unsigned short  marker = 0xFFE2; //Big endian? What if device is little endian byte order? 2Byte
    unsigned short  field_length; //Length of entire APP2 section. Have to be calculated. 2Byte
    unsigned int    format_identifier = 0x0046504D; //'M'+'P'+'F'+NULL. 4Byte
};

//MP Header of APP2 section
//8Byte
struct APP2Header
{
    unsigned int mp_endian;              //0x002A4949 - little endian, 0x4D4D2A00- big endian. 4Byte
                                //Value have to be extracted from APP1 section (Exif Attributes)
    unsigned int offset_to_first_ifd = 8;   //MP Index IFD or MP Attributes IFD is specified
                                   //immediately after Offset to First IFD, the value is 8. 4Byte
                                   //Every offset in APP2 section is calculated from the start of
                                   //APP2Header's mp_endian tag
};

//MP Index IFD section
//Used only for very first image in sequence
//42Byte
struct MPIndexIFD
{
    //Count TAG 2Byte
    unsigned short count = 3; //In this case only 4 tags is presented.

    //Version TAG 12Byte
    struct MPFVersion
    {
        unsigned short version_tag_id = TAG_MP_VERSION; //2Byte
        unsigned short type           = 0; //Undefined 2Byte
        unsigned int   count          = 4; //4Byte
        unsigned int   version        = 0x30313030; //4Byte
    };
    MPFVersion mpf_version;

    //Number of images TAG 12Byte
    struct NumberOfImages
    {
        unsigned short number_of_images_tag_id  = TAG_MP_NUMBER_OF_IMAGES; //2Byte
        unsigned short type                     = 4; //LONG 2Byte
        unsigned int   count                    = 1; //4Byte
        unsigned int   number_of_images; //4Byte
    };
    NumberOfImages number_of_images;


    //MPEntry TAG 12Byte
    struct MPEntryTag
    {
        unsigned short tag_id = TAG_MP_ENTRY; //2Byte
        unsigned short type   = 0; //Undefined 2Bytes
        unsigned int   count; //16*number_of_images 4Byte
        unsigned int   offset_of_first_ifd = 50; //starting from endian tag 4Byte
                                        //This is an offset to MPEntry #1
    };
    MPEntryTag mp_entry_tag;

    unsigned int offset_of_next_ifd; //starting from endian tag 4Byte
                            //Offset to MPAttributesIFD


    //MPEntry* mp_entries; //Array of MPEntries. Size is 16 x number_of_image
};

//MPEntry VALUE 16Byte
struct MPEntry
{
    unsigned int   individual_image_attribute = 0x0000; //4Byte 0x800 for first image and 0x0 for all other images
    unsigned int   individual_image_size; //4Byte Size of the image data between the SOI and EOI markers
    unsigned int   individual_image_data_offset; //4Byte. offset to the SOI marker from the address of the MPEndian field of MPHeader                                                                            Endian field in the MP Header
    unsigned short depended_image1_entry_number = 0; //2Byte. JPEGs in MPO isn't depended on each other
    unsigned short depended_image2_entry_number = 0; //2Byte. JPEGs in MPO isn't depended on each other
};

//MP Attributes IFD
//Specific Image Usage
//In case of Focus stacking contains TAG Focus Distance
//38Byte for all images except first
//26Byte for first image, because it doesn't contain MPFVersion TAG

//IFD for the first image
struct MPFirstIIAttributesIFD
{
    unsigned short count = 1; //Count of specific tags 2Byte.

    //Almalence's MPF TAG 12Byte
    //Shows on which distance from lens image is better focused
    struct MPFocusDistance
    {
        unsigned short tag_id = TAG_MP_INDIVIDUAL_FOCUS_DISTANCE; //2Byte
        unsigned short type   = 5; //RATIONAL 2Byte
        unsigned int   count  = 1; //4Byte
        unsigned int   offset_to_value; //4Byte Offset to focus_distance VALUE field
    };
    MPFocusDistance focus_distance_tag;

    //VALUE of tag MPFocusDistance
    //According to TIFF float value of focus distance must be converted to 2 int variables - numerator and denominator
    unsigned int focus_distance_numerator; //4Byte
    unsigned int focus_distance_denominator; //4Byte

    unsigned int offset_of_next_ifd = 0; //4Byte. MPAttributesIFD is the last IFD in APP2 field
};

//IFD for the rest images
struct MPAttributesIFD
{
    unsigned short count = 2; //Count of specific tags 2Byte.

    //Version TAG 12Byte
    struct MPFVersion
    {
        unsigned short version_tag_id = TAG_MP_VERSION; //2Byte
        unsigned short type           = 0; //Undefined 2Byte
        unsigned int   count          = 4; //4Byte
        unsigned int   version        = 0x30313030; //4Byte
    };
    MPFVersion mpf_version;

    //Almalence's MPF TAG 12Byte
    //Shows on which distance from lens image is better focused
    struct MPFocusDistance
    {
        unsigned short tag_id = TAG_MP_INDIVIDUAL_FOCUS_DISTANCE; //2Byte
        unsigned short type   = 5; //RATIONAL 2Byte
        unsigned int   count  = 1; //4Byte
        unsigned int   offset_to_value; //4Byte Offset to focus_distance VALUE field
    };
    MPFocusDistance focus_distance_tag;

    //VALUE of tag MPFocusDistance
    //According to TIFF float value of focus distance must be converted to 2 int variables - numerator and denominator
    unsigned int focus_distance_numerator; //4Byte
    unsigned int focus_distance_denominator; //4Byte

    unsigned int offset_of_next_ifd = 0; //4Byte. MPAttributesIFD is the last IFD in APP2 field
};
#pragma pack(pop)



//convert float number to UINT32 numerator and denominator
void convert2Rational(float focusDistance, 	unsigned int *numerator, unsigned int *denominator)
{
    LOGE("FOCUS DISTANCE IS %f", focusDistance);
	if (focusDistance > 1)
	{
		*numerator = 0x7FFFffff;
		*denominator = ((float)(*numerator))/focusDistance;
	}
	else
	{
		*denominator = 0x7FFFffff;
		*numerator = ((float)(*denominator))*focusDistance;
	}

	LOGE("NUMERATOR IS %x", *numerator);
	LOGE("DENOMINATOR IS %x", *denominator);
}

void write16(unsigned char *currPtr, unsigned short val, bool isLittleEndian)
{
	if (isLittleEndian)
	{
		currPtr[0] = (unsigned char)val;
		currPtr[1] = (unsigned char)(val >> 8);
	}
	else
	{
		currPtr[1] = (unsigned char)val;
		currPtr[0] = (unsigned char)(val >> 8);
	}
}

void write32(unsigned char *currPtr, unsigned int val, bool isLittleEndian)
{
	if (isLittleEndian)
	{
		currPtr[0] = (unsigned char)val;
		currPtr[1] = (unsigned char)(val >> 8);
		currPtr[2] = (unsigned char)(val >> 16);
		currPtr[3] = (unsigned char)(val >> 24);
	}
	else
	{
		currPtr[3] = (unsigned char)val;
		currPtr[2] = (unsigned char)(val >> 8);
		currPtr[1] = (unsigned char)(val >> 16);
		currPtr[0] = (unsigned char)(val >> 24);
	}
}

/*
 * Initialize focus stacking instance
 * @in - array of pointers to input jpeg data
 * @focusDist - array of focus distance for each input frame (in diopter)
 * @nFrames - number of input frames
 */
extern "C" JNIEXPORT jint JNICALL Java_com_almalence_focusstacking_AlmaShotMPOWriter_MPOWriterInitialize
(
	JNIEnv* env,
	jobject thiz,
	jintArray in,
	jintArray sz,
	jfloatArray focusDist,
	jint nFrames
)
{
	LOGE("MPOWriterInitialize - start");
	if(nFrames > MAX_MPO_FRAMES)
    {
        LOGE("MPOWriterInitialize. Too many frames: %d. Maximum allowed number is %d\nAborting...",(int)nFrames, (int)MAX_MPO_FRAMES);
        return -1;
    }
	int i;
	unsigned char **jpeg;
	int* size;
	float* focus;
	char status[1024];

	iImageAmount = nFrames;

	jpeg = (unsigned char**)env->GetIntArrayElements(in, NULL);
	size = (int*)env->GetIntArrayElements(sz, NULL);
	focus = (float*)env->GetFloatArrayElements(focusDist, NULL);

	for (i = 0; i < iImageAmount; i++)
	{
		inputFrame[i] = jpeg[i];
		framesSizes[i] = size[i];
		focusDistances[i] = focus[i];
	}


/*	__android_log_print(ANDROID_LOG_ERROR, "CameraTest", "START INPUT SAVE");
	for (int i=0; i<nFrames; ++i)
	{
		char str[256];
		sprintf(str, "/sdcard/DCIM/fstackingin%02d.yuv", i);
		FILE *f = fopen (str, "wb");
		fwrite(inputFrame[i], sx*sy+2*((sx+1)/2)*((sy+1)/2), 1, f);
		fclose(f);
	}
	__android_log_print(ANDROID_LOG_ERROR, "CameraTest", "INPUT SAVED");
*/


	env->ReleaseIntArrayElements(in, (jint*)jpeg, JNI_ABORT);
	env->ReleaseFloatArrayElements(focusDist, (jfloat*)focus, JNI_ABORT);

	LOGE("MPOWriter_Initialize. Frames total: %d\n", (int)nFrames);
	return nFrames;
}

extern "C" JNIEXPORT jint JNICALL Java_com_almalence_focusstacking_AlmaShotMPOWriter_MPOWriterRelease
(
	JNIEnv*,
	jobject
)
{
	for (int i = 0; i < iImageAmount; ++i)
	{
		free(inputFrame[i]);
		inputFrame[i] = NULL;

        framesSizes[i] = 0;
		focusDistances[i] = 0;
	}

	return 0;
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_com_almalence_focusstacking_AlmaShotMPOWriter_MPOWriterProcess
(
	JNIEnv* env,
	jobject thiz
)
{
    LOGE("MPOWriterProcess -- start");

    //Calculate MP file size and Individual Images offset
    unsigned int firstIIsize; // first Individual Image APP2's size
    unsigned int IIsize; // Individual Image APP2's size except first

    firstIIsize = APP2_HEADER_SIZE + MP_HEADER_SIZE;
    firstIIsize += MP_INDEX_IFD_SIZE; //MP Index IFD
    firstIIsize += MP_ENTRY_SIZE*iImageAmount;//Value MP Index IFD
    firstIIsize += MP_ATTR_FIRST_II_SIZE; ///MP Attributes IFD

    IIsize = APP2_HEADER_SIZE + MP_HEADER_SIZE;
    IIsize += MP_ATTR_SIZE; ///MP Attributes IFD


    //MPO file size = summarize all jpeg images + all added APP2 data
    unsigned int mpo_file_size = 0;
    mpo_file_size += firstIIsize; //Size of APP2 field for first image
    mpo_file_size += IIsize*(iImageAmount - 1); //Size of all other image's APP2 fields
    for(int frameIndex = 0; frameIndex < iImageAmount; frameIndex++)
    {
        int size = framesSizes[frameIndex];
        mpo_file_size += size; //Add initial jpeg size without APP2 field
    }

   unsigned char *mpo_data; //MPO file data

    jbyteArray jdata = env->NewByteArray(mpo_file_size);
    mpo_data = (unsigned char*)env->GetByteArrayElements(jdata, NULL);

    unsigned char* currPtr = mpo_data; //Create pointer which will be increased to write data in


    for(int frameIndex = 0; frameIndex < iImageAmount; frameIndex++)
    {
        unsigned int offset = 0; //Offset to some data counting from endian tag

        bool isFirstFrame = (frameIndex == 0);
        unsigned char *jpeg_data = (unsigned char*)inputFrame[frameIndex];
        short* soi = (short*)jpeg_data;
        short* eoi = (short*)(jpeg_data + framesSizes[frameIndex] - sizeof(short));
        short* end_of_jpeg = eoi + 1;
        //short* com = (short*)(first_jpeg + sizeof(short));
        LOGE("jpeg SOI: %x\n", (*soi & 0xffff));
        LOGE("jpeg EOI: %x\n", (*eoi & 0xffff));

        LOGE("size of jpeg (EOI - SOI): %d\n", (end_of_jpeg - soi)*sizeof(short));

        //TEST CODE TO FIND APP1 AND ITS LENGTH
        bool isMarker = false; //Shown that previous byte was 0xFF - indicator of marker
        bool app1Found = false;
        unsigned short* app1;
        unsigned short* app1Length;
        unsigned short app1LengthValue;
        unsigned int* endian;
        bool isLittleEndian;
        unsigned int size = framesSizes[frameIndex];
        LOGE("Frame size: %d\n", size);

        for(int i = 2; i < size; i++)
        {
            /*if(i < 64)
            {
                LOGE("BYTE is: %x\n", *(jpeg_data + i));
            }*/

            unsigned char d = jpeg_data[i];
            if(d == M_START)
            {
                //LOGE("Start of some marker");
                isMarker = true;
                continue;
            }
            else if(isMarker && ((d & 0xFF) == M_APP1)) //Found APP1 section
            {
                //LOGE("Marker is: %x\n", d);
                //LOGE("APP 1 FOUND!!!!,  marker is %x\n", d & 0xff);
                app1Found = true;
                isMarker = false;

                app1 = (unsigned short*)(jpeg_data + i); //Start of APP1 section (Exif Attributes)
                app1Length =(unsigned short*)(jpeg_data + i + 1); //Next to APP1 marker is 2 bytes value of length of APP1 section
                unsigned char* exif_identifier_code = (unsigned char*)(app1Length + 1); //Code is: Exif
                endian = (unsigned int*)(exif_identifier_code + 6); //Big endian or Little endian
                if(*endian == LittleEndian)
                    isLittleEndian = true;
                else
                    isLittleEndian = false;

                unsigned short app1LengthValue = *app1Length;
                app1LengthValue = 256*(app1LengthValue&0xFF) + (app1LengthValue>>8);
                //LOGE("Length of APP1 is: %x\n", app1LengthValue);


                 //START write to output buffer APP1 section
                 unsigned int soi_to_end_of_app1_length = app1LengthValue + 4;//((app1 - soi + 1) + *app1Length)*sizeof(short);
                 //int pure_jpeg_data_length = (end_of_jpeg - (app1 + *app1Length))*sizeof(short);
                 unsigned int pure_jpeg_data_length = framesSizes[frameIndex] - soi_to_end_of_app1_length;//(eoi - (app1 + *app1Length) + 1)*sizeof(short);
                 //LOGE("Length of SOI to APP1 end is: %d\n", soi_to_end_of_app1_length);
                 memcpy(currPtr, jpeg_data, soi_to_end_of_app1_length);
                 currPtr += soi_to_end_of_app1_length;
                 //END

                 //memcpy(currPtr, jpeg_data, 2);
                 //currPtr += 2;


                //LOGE("EXIF identifier code is: %s\n", exif_identifier_code);
                //LOGE("Endian is: %x\n", (*endian & 0xFFFFFFFF));
                //LOGE("APP1 i = %d\n", i);

                //LOGE("SIZEOF(APP2Format) = %d, SIZEOF(APP2Header) = %d\n", sizeof(APP2Format), sizeof(APP2Header));


                //START write to output buffer APP2 section
                //Write APP2Format (Marker + Field length + Format Identifier)
                APP2Format format_identifier;
                //short length =  (isFirstFrame? firstIIsize : IIsize) - 2;
                //length = 256*(length&0xFF) + (length>>8);
                format_identifier.field_length = (isFirstFrame? firstIIsize : IIsize) - 2;

                write16(currPtr, format_identifier.marker, false); currPtr += 2;
                write16(currPtr, format_identifier.field_length, false); currPtr += 2;
                write32(currPtr, format_identifier.format_identifier, true); currPtr += 4;

                //memcpy(currPtr, &format_identifier, MP_HEADER_SIZE);
                //currPtr += MP_HEADER_SIZE;

                //Write APP2Header
                APP2Header app_header;
                //app_header.offset_to_first_ifd = 256*(app_header.offset_to_first_ifd&0xFF) + (app_header.offset_to_first_ifd>>8) + (app_header.offset_to_first_ifd>>16) +(app_header.offset_to_first_ifd>>24);
                //write MP header
                if (isLittleEndian)
                {
                    write32(currPtr, 0x002A4949, true); currPtr += 4;
                }
                else
                {
                    write32(currPtr, 0x4D4D002A, false); currPtr += 4;
                }
                app_header.mp_endian = *endian;

                //write32(currPtr, app_header.mp_endian, isLittleEndian); currPtr += 4;
                write32(currPtr, app_header.offset_to_first_ifd, isLittleEndian); currPtr += 4;
                //memcpy(currPtr, &app_header, APP2_HEADER_SIZE);
                //currPtr += APP2_HEADER_SIZE;

                offset += APP2_HEADER_SIZE;
                LOGE("OFFSET = %d\n", offset);



                //Only very first frame contains MP Index IFD section
                if(isFirstFrame)
                {
                    offset += MP_INDEX_IFD_SIZE;
                    LOGE("OFFSET = %d\n", offset);
                    //LOGE("SIZEOF(APP2Format) = %d, SIZEOF(APP2Header) = %d\n", sizeof(APP2Format), sizeof(APP2Header));
                    //LOGE("sizeof(MPIndexIDF) = %d\n", sizeof(MPIndexIFD));
                    //LOGE("sizeof(MPAttributesIDF) = %d\n", sizeof(MPAttributesIFD));
                    MPIndexIFD mp_index_ifd;
                    mp_index_ifd.number_of_images.number_of_images = iImageAmount;
                    mp_index_ifd.mp_entry_tag.count = 16 * iImageAmount;
                    mp_index_ifd.offset_of_next_ifd = mp_index_ifd.mp_entry_tag.offset_of_first_ifd + iImageAmount * MP_ENTRY_SIZE;

                    //LOGE("sizeof(real MPIndexIDF) = %d\n", sizeof(mp_index_ifd));
                    //LOGE("offset_of_first_ifd = %d\n", mp_index_ifd.mp_entry_tag.offset_of_first_ifd);

                    offset += MP_ENTRY_SIZE * iImageAmount;
                    LOGE("OFFSET = %d\n", offset);

                    //Write MPIndexIFD section
                    //Count
                    write16(currPtr, mp_index_ifd.count, isLittleEndian); currPtr += 2;

                    //MPFVersion
                    write16(currPtr, mp_index_ifd.mpf_version.version_tag_id, isLittleEndian); currPtr += 2;
                    write16(currPtr, mp_index_ifd.mpf_version.type, isLittleEndian); currPtr += 2;
                    write32(currPtr, mp_index_ifd.mpf_version.count, isLittleEndian); currPtr += 4;
                    write32(currPtr, mp_index_ifd.mpf_version.version, isLittleEndian); currPtr += 4;

                    //NumberOfImages
                    write16(currPtr, mp_index_ifd.number_of_images.number_of_images_tag_id, isLittleEndian); currPtr += 2;
                    write16(currPtr, mp_index_ifd.number_of_images.type, isLittleEndian); currPtr += 2;
                    write32(currPtr, mp_index_ifd.number_of_images.count, isLittleEndian); currPtr += 4;
                    write32(currPtr, mp_index_ifd.number_of_images.number_of_images, isLittleEndian); currPtr += 4;

                    //MPEntryTag
                    write16(currPtr, mp_index_ifd.mp_entry_tag.tag_id, isLittleEndian); currPtr += 2;
                    write16(currPtr, mp_index_ifd.mp_entry_tag.type, isLittleEndian); currPtr += 2;
                    write32(currPtr, mp_index_ifd.mp_entry_tag.count, isLittleEndian); currPtr += 4;
                    write32(currPtr, mp_index_ifd.mp_entry_tag.offset_of_first_ifd, isLittleEndian); currPtr += 4;

                    //Offset of next IFD
                    write32(currPtr, mp_index_ifd.offset_of_next_ifd, isLittleEndian); currPtr += 4;
                    //memcpy(currPtr, &mp_index_ifd, MP_INDEX_IFD_SIZE);
                    //currPtr += MP_INDEX_IFD_SIZE;

                    int image_data_offset = framesSizes[0] - (*app1Length) + firstIIsize - APP2_HEADER_SIZE; //Offset to the second jpeg from the endian field
                                                                                                         //of first jpeg's APP2
                    for(int j = 0; j < iImageAmount; j++)
                    {
                        MPEntry entry;
                        if(j == 0) //First entry
                        {
                            entry.individual_image_size = firstIIsize + framesSizes[0];//4Byte Size of the image data between the SOI and EOI markers
                            entry.individual_image_data_offset = 0; //4Byte. offset to the SOI marker from the address of the MPEndian field of MPHeader
                        }
                        else
                        {
                            entry.individual_image_size = IIsize + framesSizes[j];
                            entry.individual_image_data_offset = image_data_offset;
                            image_data_offset += entry.individual_image_size;
                        }

                        //Write MPEntry to VALUE section
                        write32(currPtr, entry.individual_image_attribute, isLittleEndian); currPtr += 4;
                        write32(currPtr, entry.individual_image_size, isLittleEndian); currPtr += 4;
                        write32(currPtr, entry.individual_image_data_offset, isLittleEndian); currPtr += 4;
                        write16(currPtr, entry.depended_image1_entry_number, isLittleEndian); currPtr += 2;
                        write16(currPtr, entry.depended_image2_entry_number, isLittleEndian); currPtr += 2;
                        //memcpy(currPtr, &entry, MP_ENTRY_SIZE);
                        //currPtr += MP_ENTRY_SIZE;
                    }

                    offset += MP_ATTR_FIRST_II_SIZE - 8;
                    LOGE("OFFSET = %d\n", offset);

                    MPFirstIIAttributesIFD mp_attributes_ifd;
                    mp_attributes_ifd.focus_distance_tag.offset_to_value = offset;

                    unsigned int numerator;
                    unsigned int denominator;
                    convert2Rational(focusDistances[frameIndex], &numerator, &denominator);

                    mp_attributes_ifd.focus_distance_numerator = numerator;
                    mp_attributes_ifd.focus_distance_denominator = denominator;

                    //Write MPFirstIIAttributeIFD
                    //Count
                    write16(currPtr, mp_attributes_ifd.count, isLittleEndian); currPtr += 2;

                    //MPFocusDistance
                    write16(currPtr, mp_attributes_ifd.focus_distance_tag.tag_id, isLittleEndian); currPtr += 2;
                    write16(currPtr, mp_attributes_ifd.focus_distance_tag.type, isLittleEndian); currPtr += 2;
                    write32(currPtr, mp_attributes_ifd.focus_distance_tag.count, isLittleEndian); currPtr += 4;
                    write32(currPtr, mp_attributes_ifd.focus_distance_tag.offset_to_value, isLittleEndian); currPtr += 4;

                    //Offset to next IFD
                    write32(currPtr, mp_attributes_ifd.offset_of_next_ifd, isLittleEndian); currPtr += 4;

                    //VALUE of tag MPFocusDistance
                    write32(currPtr, mp_attributes_ifd.focus_distance_numerator, isLittleEndian); currPtr += 4;
                    write32(currPtr, mp_attributes_ifd.focus_distance_denominator, isLittleEndian); currPtr += 4;


                    //memcpy(currPtr, &mp_attributes_ifd, MP_ATTR_FIRST_II_SIZE);
                    //currPtr += MP_ATTR_FIRST_II_SIZE;
                }
                else
                {
                    offset += MP_ATTR_SIZE - 8;

                    MPAttributesIFD mp_attributes_ifd;
                    mp_attributes_ifd.focus_distance_tag.offset_to_value = offset;

                    unsigned int numerator;
                    unsigned int denominator;
                    convert2Rational(focusDistances[frameIndex], &numerator, &denominator);

                    mp_attributes_ifd.focus_distance_numerator = numerator;
                    mp_attributes_ifd.focus_distance_denominator = denominator;

                    //Write MPAttributeIFD
                    //Count
                    write16(currPtr, mp_attributes_ifd.count, isLittleEndian); currPtr += 2;

                    //MPFVersion
                    write16(currPtr, mp_attributes_ifd.mpf_version.version_tag_id, isLittleEndian); currPtr += 2;
                    write16(currPtr, mp_attributes_ifd.mpf_version.type, isLittleEndian); currPtr += 2;
                    write32(currPtr, mp_attributes_ifd.mpf_version.count, isLittleEndian); currPtr += 4;
                    write32(currPtr, mp_attributes_ifd.mpf_version.version, isLittleEndian); currPtr += 4;

                    //MPFocusDistance
                    write16(currPtr, mp_attributes_ifd.focus_distance_tag.tag_id, isLittleEndian); currPtr += 2;
                    write16(currPtr, mp_attributes_ifd.focus_distance_tag.type, isLittleEndian); currPtr += 2;
                    write32(currPtr, mp_attributes_ifd.focus_distance_tag.count, isLittleEndian); currPtr += 4;
                    write32(currPtr, mp_attributes_ifd.focus_distance_tag.offset_to_value, isLittleEndian); currPtr += 4;

                    //Offset to next IFD
                    write32(currPtr, mp_attributes_ifd.offset_of_next_ifd, isLittleEndian); currPtr += 4;

                    //VALUE of tag MPFocusDistance
                    write32(currPtr, mp_attributes_ifd.focus_distance_numerator, isLittleEndian); currPtr += 4;
                    write32(currPtr, mp_attributes_ifd.focus_distance_denominator, isLittleEndian); currPtr += 4;

                    //memcpy(currPtr, &mp_attributes_ifd, MP_ATTR_SIZE);
                    //currPtr += MP_ATTR_SIZE;
                }


                //Write MPAttributeIFD
                memcpy(currPtr, jpeg_data + soi_to_end_of_app1_length, pure_jpeg_data_length);
                currPtr += pure_jpeg_data_length;

               break;
            }
            else
               isMarker = false;
        }
        //LOGE("first_jpeg Comment marker: %x\n", (*com & 0xffff));
    }


    env->ReleaseByteArrayElements(jdata, (jbyte*)mpo_data, 0);

    LOGE("MPOWriterProcess -- end");
    return jdata;
}