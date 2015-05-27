#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#include "Ap4.h"

const unsigned int AP4_MUX_DEFAULT_VIDEO_FRAME_RATE = 24;

/*----------------------------------------------------------------------
|   SampleOrder
+---------------------------------------------------------------------*/
struct SampleOrder {
    SampleOrder(AP4_UI32 decode_order, AP4_UI32 display_order) :
        m_DecodeOrder(decode_order),
        m_DisplayOrder(display_order) {}
    AP4_UI32 m_DecodeOrder;
    AP4_UI32 m_DisplayOrder;
};



/*----------------------------------------------------------------------
|   SampleFileStorage
+---------------------------------------------------------------------*/
class SampleFileStorage
{
public:
    static AP4_Result Create(const char* basename, SampleFileStorage*& sample_file_storage);
    ~SampleFileStorage() {
        m_Stream->Release();
        remove(m_Filename.GetChars());
    }

    AP4_Result StoreSample(AP4_Sample& from_sample, AP4_Sample& to_sample) {
        // clone the sample fields
        to_sample = from_sample;

        // read the sample data
        AP4_DataBuffer sample_data;
        AP4_Result result = from_sample.ReadData(sample_data);
        if (AP4_FAILED(result)) return result;

        // mark where we are going to store the sample data
        AP4_Position position;
        m_Stream->Tell(position);
        to_sample.SetOffset(position);

        // write the sample data
        result = m_Stream->Write(sample_data.GetData(), sample_data.GetDataSize());
        if (AP4_FAILED(result)) return result;

        // update the stream for the new sample
        to_sample.SetDataStream(*m_Stream);

        return AP4_SUCCESS;
    }

    AP4_ByteStream* GetStream() { return m_Stream; }

private:
    SampleFileStorage(const char* basename) : m_Stream(NULL) {
        AP4_Size name_length = (AP4_Size)AP4_StringLength(basename);
        char* filename = new char[name_length+2];
        AP4_CopyMemory(filename, basename, name_length);
        filename[name_length]   = '_';
        filename[name_length+1] = '\0';
        m_Filename = filename;
        delete[] filename;
    }

    AP4_ByteStream* m_Stream;
    AP4_String      m_Filename;
};

/*----------------------------------------------------------------------
|   SampleFileStorage::Create
+---------------------------------------------------------------------*/
AP4_Result
SampleFileStorage::Create(const char* basename, SampleFileStorage*& sample_file_storage)
{
    sample_file_storage = NULL;
    SampleFileStorage* object = new SampleFileStorage(basename);
    AP4_Result result = AP4_FileByteStream::Create(object->m_Filename.GetChars(),
                                                   AP4_FileByteStream::STREAM_MODE_WRITE,
                                                   object->m_Stream);
    if (AP4_FAILED(result)) {
        return result;
    }
    sample_file_storage = object;
    return AP4_SUCCESS;
}

/*----------------------------------------------------------------------
|   SortSamples
+---------------------------------------------------------------------*/
void
SortSamples(SampleOrder* array, unsigned int n)
{
    if (n < 2) {
        return;
    }
    SampleOrder pivot = array[n / 2];
    SampleOrder* left  = array;
    SampleOrder* right = array + n - 1;
    while (left <= right) {
        if (left->m_DisplayOrder < pivot.m_DisplayOrder) {
            ++left;
            continue;
        }
        if (right->m_DisplayOrder > pivot.m_DisplayOrder) {
            --right;
            continue;
        }
        SampleOrder temp = *left;
        *left++ = *right;
        *right-- = temp;
    }
    SortSamples(array, (unsigned int)(right - array + 1));
    SortSamples(left, (unsigned int)(array + n - left));
}


void AddH264Track(AP4_Movie&            movie,
             	 const char*           input_name,
             	 AP4_Array<AP4_UI32>&  brands,
             	 SampleFileStorage&    sample_storage)
{
    AP4_ByteStream* input;
    AP4_Result result = AP4_FileByteStream::Create(input_name, AP4_FileByteStream::STREAM_MODE_READ, input);
    if (AP4_FAILED(result)) {
        fprintf(stderr, "ERROR: cannot open input file '%s' (%d))\n", input_name, result);
        return;
    }

    // see if the frame rate is specified
    unsigned int video_frame_rate = AP4_MUX_DEFAULT_VIDEO_FRAME_RATE*1000;

    // create a sample table
    AP4_SyntheticSampleTable* sample_table = new AP4_SyntheticSampleTable();

    // allocate an array to keep track of sample order
    AP4_Array<SampleOrder> sample_orders;

    // parse the input
    AP4_AvcFrameParser parser;
    for (;;) {
        bool eos;
        unsigned char input_buffer[4096];
        AP4_Size bytes_in_buffer = 0;
        result = input->ReadPartial(input_buffer, sizeof(input_buffer), bytes_in_buffer);
        if (AP4_SUCCEEDED(result)) {
        	__android_log_print(ANDROID_LOG_ERROR, "Mp4Editor", "AP4_SUCCEEDED(result). EOS = false");
            eos = false;
        } else if (result == AP4_ERROR_EOS) {
        	__android_log_print(ANDROID_LOG_ERROR, "Mp4Editor", "AP4_ERROR_EOS. EOS = true");
            eos = true;
        } else {
        	__android_log_print(ANDROID_LOG_ERROR, "Mp4Editor", "ERROR: Failed to read from input file");
            break;
        }
        AP4_Size offset = 0;
        bool     found_access_unit = false;
        do {
            AP4_AvcFrameParser::AccessUnitInfo access_unit_info;

            found_access_unit = false;
            AP4_Size bytes_consumed = 0;
            result = parser.Feed(&input_buffer[offset],
                                 bytes_in_buffer,
                                 bytes_consumed,
                                 access_unit_info,
                                 eos);
            if (AP4_FAILED(result)) {
                fprintf(stderr, "ERROR: Feed() failed (%d)\n", result);
                break;
            }
            if (access_unit_info.nal_units.ItemCount()) {
                // we got one access unit
                found_access_unit = true;

                // compute the total size of the sample data
                unsigned int sample_data_size = 0;
                for (unsigned int i=0; i<access_unit_info.nal_units.ItemCount(); i++) {
                    sample_data_size += 4+access_unit_info.nal_units[i]->GetDataSize();
                }

                // store the sample data
                AP4_Position position = 0;
                sample_storage.GetStream()->Tell(position);
                for (unsigned int i=0; i<access_unit_info.nal_units.ItemCount(); i++) {
                    sample_storage.GetStream()->WriteUI32(access_unit_info.nal_units[i]->GetDataSize());
                    sample_storage.GetStream()->Write(access_unit_info.nal_units[i]->GetData(), access_unit_info.nal_units[i]->GetDataSize());
                }

                // add the sample to the track
                sample_table->AddSample(*sample_storage.GetStream(), position, sample_data_size, 1000, 0, 0, 0, access_unit_info.is_idr);

                // remember the sample order
                sample_orders.Append(SampleOrder(access_unit_info.decode_order, access_unit_info.display_order));

                // free the memory buffers
                access_unit_info.Reset();
            }

            offset += bytes_consumed;
            bytes_in_buffer -= bytes_consumed;
        } while (bytes_in_buffer || found_access_unit);
        if (eos) break;
    }

    __android_log_print(ANDROID_LOG_ERROR, "Mp4Editor", "Parse input finished");

    // adjust the sample CTS/DTS offsets based on the sample orders
    if (sample_orders.ItemCount() > 1) {
        unsigned int start = 0;
        for (unsigned int i=1; i<=sample_orders.ItemCount(); i++) {
            if (i == sample_orders.ItemCount() || sample_orders[i].m_DisplayOrder == 0) {
                // we got to the end of the GOP, sort it by display order
                SortSamples(&sample_orders[start], i-start);
                start = i;
            }
        }
    }
    unsigned int max_delta = 0;
    for (unsigned int i=0; i<sample_orders.ItemCount(); i++) {
        if (sample_orders[i].m_DecodeOrder > i) {
            unsigned int delta =sample_orders[i].m_DecodeOrder-i;
            if (delta > max_delta) {
                max_delta = delta;
            }
        }
    }
    for (unsigned int i=0; i<sample_orders.ItemCount(); i++) {
        sample_table->UseSample(sample_orders[i].m_DecodeOrder).SetCts(1000ULL*(AP4_UI64)(i+max_delta));
    }

    // check the video parameters
    AP4_AvcSequenceParameterSet* sps = NULL;
    for (unsigned int i=0; i<=AP4_AVC_SPS_MAX_ID; i++) {
        if (parser.GetSequenceParameterSets()[i]) {
            sps = parser.GetSequenceParameterSets()[i];
            break;
        }
    }
    if (sps == NULL) {
        fprintf(stderr, "ERROR: no sequence parameter set found in video\n");
        input->Release();
        return;
    }
    unsigned int video_width = 0;
    unsigned int video_height = 0;
    sps->GetInfo(video_width, video_height);

    // collect the SPS and PPS into arrays
    AP4_Array<AP4_DataBuffer> sps_array;
    for (unsigned int i=0; i<=AP4_AVC_SPS_MAX_ID; i++) {
        if (parser.GetSequenceParameterSets()[i]) {
            sps_array.Append(parser.GetSequenceParameterSets()[i]->raw_bytes);
        }
    }
    AP4_Array<AP4_DataBuffer> pps_array;
    for (unsigned int i=0; i<=AP4_AVC_PPS_MAX_ID; i++) {
        if (parser.GetPictureParameterSets()[i]) {
            pps_array.Append(parser.GetPictureParameterSets()[i]->raw_bytes);
        }
    }

    // setup the video the sample descripton
    AP4_AvcSampleDescription* sample_description =
        new AP4_AvcSampleDescription(AP4_SAMPLE_FORMAT_AVC1,
                                     video_width,
                                     video_height,
                                     24,
                                     "h264",
                                     sps->profile_idc,
                                     sps->level_idc,
                                     sps->constraint_set0_flag<<7 |
                                     sps->constraint_set1_flag<<6 |
                                     sps->constraint_set2_flag<<5 |
                                     sps->constraint_set3_flag<<4,
                                     4,
                                     sps_array,
                                     pps_array);
    sample_table->AddSampleDescription(sample_description);

    AP4_UI32 movie_timescale      = 1000;
    AP4_UI32 media_timescale      = video_frame_rate;
    AP4_UI64 video_track_duration = AP4_ConvertTime(1000*sample_table->GetSampleCount(), media_timescale, movie_timescale);
    AP4_UI64 video_media_duration = 1000*sample_table->GetSampleCount();

    // create a video track
    AP4_Track* track = new AP4_Track(AP4_Track::TYPE_VIDEO,
                                     sample_table,
                                     0,                    // auto-select track id
                                     movie_timescale,      // movie time scale
                                     video_track_duration, // track duration
                                     video_frame_rate,     // media time scale
                                     video_media_duration, // media duration
                                     "und",                // language
                                     video_width<<16,      // width
                                     video_height<<16      // height
                                     );

    // update the brands list
    brands.Append(AP4_FILE_BRAND_AVC1);

    // cleanup
    input->Release();

    movie.AddTrack(track);
}


// this is a very common operation - use ImageConversion jni interface instead (? - need to avoid global yuv array then?)
extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_capture_video_Mp4Editor_append
(
	JNIEnv* env,
	jobject thiz,
	jobjectArray inputFiles,
//	jstring inputFile,
//	jstring appendedFile,
	jstring newFile
)
{
	char status[1024];

//	const char* input_filename = env->GetStringUTFChars(inputFile, 0);
//	const char* appended_filename = env->GetStringUTFChars(appendedFile, 0);
	const char* output_filename = env->GetStringUTFChars(newFile, 0);

	// create the output stream
	AP4_ByteStream* output = NULL;
	AP4_Result result;
	result = AP4_FileByteStream::Create(output_filename, AP4_FileByteStream::STREAM_MODE_WRITE, output);
	if (AP4_FAILED(result))
	{
		sprintf (status, "ERROR: cannot open output file (%s)\n", output_filename);

		return env->NewStringUTF(status);
	}

	//Create output Movie
	AP4_Movie* output_movie = new AP4_Movie(1000);
	if (output_movie == NULL)
	{
		sprintf (status, "ERROR: output file (%s) doesn't have Movie object\n", output_filename);
		return env->NewStringUTF(status);
	}

	// create an output sample tables
	AP4_SyntheticSampleTable* sample_video_table = new AP4_SyntheticSampleTable();
	AP4_SyntheticSampleTable* sample_audio_table = new AP4_SyntheticSampleTable();

	//Prototypes track is used to pass transform matrix from input files to ouput
	AP4_Track* prototype_track_video = NULL;

	AP4_UI32 duration_movie = 0;
	AP4_UI32 duration_media = 0;

	AP4_UI32         movie_time_scale 		= 0;
	AP4_UI32         media_time_scale 		= 0;
	AP4_UI32         movie_time_scale_audio = 0;
	AP4_UI32         media_time_scale_audio = 0;
	const char*      language 		  		= NULL;
	AP4_UI32         width 			  		= 0;
	AP4_UI32         height 		 		= 0;



	int inputFileCount = env->GetArrayLength(inputFiles);
	for (int i = 0; i < inputFileCount; i++)
	{
//		__android_log_print(ANDROID_LOG_ERROR, "Mp4Editor", "Process file %d", i);
		jstring string = (jstring) env->GetObjectArrayElement(inputFiles, i);
		const char *input_filename = env->GetStringUTFChars(string, 0);

		// create the first input stream
		AP4_ByteStream* input = NULL;
		result = AP4_FileByteStream::Create(input_filename, AP4_FileByteStream::STREAM_MODE_READ, input);
		if (AP4_FAILED(result))
		{
			sprintf (status, "ERROR: cannot open input file (%s)\n", input_filename);
			return env->NewStringUTF(status);
		}

		//Get Movie from first file
		AP4_File first_file(*input);
		input->Release();
		AP4_Movie* input_movie = first_file.GetMovie();
		if (input_movie == NULL)
		{
			sprintf (status, "ERROR: input file (%s) doesn't have Movie object\n", input_filename);
			return env->NewStringUTF(status);
		}

		AP4_Track* track_video = input_movie->GetTrack(AP4_Track::TYPE_VIDEO);
		AP4_Track* track_audio = input_movie->GetTrack(AP4_Track::TYPE_AUDIO);

		AP4_SyntheticSampleTable* sample_video = (AP4_SyntheticSampleTable*)track_video->GetSampleTable();
		AP4_SyntheticSampleTable* sample_audio = (AP4_SyntheticSampleTable*)track_audio->GetSampleTable();


		if(i == 0)
		{
//			__android_log_print(ANDROID_LOG_ERROR, "Mp4Editor", "Clone track from first file");
			prototype_track_video  = track_video->Clone();
//			__android_log_print(ANDROID_LOG_ERROR, "Mp4Editor", "Cloned ");
			movie_time_scale 	   = track_video->GetMovieTimeScale();
			media_time_scale 	   = track_video->GetMediaTimeScale();
			movie_time_scale_audio = track_audio->GetMovieTimeScale();
			media_time_scale_audio = track_audio->GetMediaTimeScale();
			language 		 	   = track_video->GetTrackLanguage();
			width 				   = track_video->GetWidth();
			height 		     	   = track_video->GetHeight();
		}


		/*
		 * ADD VIDEO SAMPLES
		 */
		// add clones of the sample descriptions to the new sample table
		for (unsigned int j=0; ;j++)
		{
			AP4_SampleDescription* sample_description = sample_video->GetSampleDescription(j);
			if (sample_description == NULL) break;
			sample_video_table->AddSampleDescription(sample_description->Clone());
		}


		AP4_Sample  sample;
		AP4_Ordinal index = 0;

		while (AP4_SUCCEEDED(sample_video->GetSample(index, sample)))
		{
			sample.SetDts(0);
			AP4_ByteStream* data_stream;
			data_stream = sample.GetDataStream();
			sample_video_table->AddSample(*data_stream,
									sample.GetOffset(),
									sample.GetSize(),
									sample.GetDuration(),
									sample.GetDescriptionIndex(),
									0,
									sample.GetCtsDelta(),
									sample.IsSync());
			AP4_RELEASE(data_stream); // release our ref, the table has kept its own ref.
			index++;
		}

		duration_movie =+ track_video->GetDuration();
		duration_media =+ track_video->GetMediaDuration();







		/*
		 * ADD AUDIO SAMPLES
		 */
		// add clones of the sample descriptions to the new sample table
		for (unsigned int k=0; ;k++)
		{
			AP4_SampleDescription* sample_description = sample_audio->GetSampleDescription(k);
			if (sample_description == NULL) break;
			sample_audio_table->AddSampleDescription(sample_description->Clone());
		}

		index = 0;
		while (AP4_SUCCEEDED(sample_audio->GetSample(index, sample)))
		{
			sample.SetDts(0);
			AP4_ByteStream* data_stream;
			data_stream = sample.GetDataStream();
			sample_audio_table->AddSample(*data_stream,
									sample.GetOffset(),
									sample.GetSize(),
									sample.GetDuration(),
									sample.GetDescriptionIndex(),
									0,
									sample.GetCtsDelta(),
									sample.IsSync());
			AP4_RELEASE(data_stream); // release our ref, the table has kept its own ref.
			index++;
		}

		env->ReleaseStringUTFChars(string, input_filename);

	}

	// create the output video track
	AP4_Track* output_video_track = new AP4_Track(sample_video_table,
												  0,
												  movie_time_scale,
												  duration_movie,
												  media_time_scale,
												  duration_media,
												  prototype_track_video);

	// create the output audio track
	AP4_Track* output_audio_track = new AP4_Track(AP4_Track::TYPE_AUDIO,
												  sample_audio_table,
												  0,
												  movie_time_scale_audio,
												  duration_movie,
												  media_time_scale_audio,
												  duration_media,
												  language,
												  0,
												  0);
	output_movie->AddTrack(output_video_track);
	output_movie->AddTrack(output_audio_track);




//	AP4_Result result;
//
//	// create the first input stream
//	AP4_ByteStream* input = NULL;
//	result = AP4_FileByteStream::Create(input_filename, AP4_FileByteStream::STREAM_MODE_READ, input);
//	if (AP4_FAILED(result))
//	{
//		sprintf (status, "ERROR: cannot open input file (%s)\n", input_filename);
//		return env->NewStringUTF(status);
//	}
//
//	//Get Movie from first file
//	AP4_File first_file(*input);
//	input->Release();
//	AP4_Movie* input_movie = first_file.GetMovie();
//	if (input_movie == NULL)
//	{
//		sprintf (status, "ERROR: input file (%s) doesn't have Movie object\n", input_filename);
//		return env->NewStringUTF(status);
//	}
//
//
//
//	// create the first input stream
//	AP4_ByteStream* input2 = NULL;
//	result = AP4_FileByteStream::Create(appended_filename, AP4_FileByteStream::STREAM_MODE_READ, input2);
//	if (AP4_FAILED(result))
//	{
//		sprintf (status, "ERROR: cannot open input file (%s)\n", appended_filename);
//		return env->NewStringUTF(status);
//	}
//
//	//Get Movie from first file
//	AP4_File second_file(*input2);
//	input2->Release();
//	AP4_Movie* input_movie2 = second_file.GetMovie();
//	if (input_movie2 == NULL)
//	{
//		sprintf (status, "ERROR: input file (%s) doesn't have Movie object\n", appended_filename);
//		return env->NewStringUTF(status);
//	}
//
//
//
//	// create the output stream
//	AP4_ByteStream* output = NULL;
//	result = AP4_FileByteStream::Create(output_filename, AP4_FileByteStream::STREAM_MODE_WRITE, output);
//	if (AP4_FAILED(result))
//	{
//		sprintf (status, "ERROR: cannot open output file (%s)\n", output_filename);
//
//		return env->NewStringUTF(status);
//	}
//
//	//Create output Movie
//	AP4_Movie* output_movie = new AP4_Movie(1000);
//	if (output_movie == NULL)
//	{
//		sprintf (status, "ERROR: output file (%s) doesn't have Movie object\n", output_filename);
//		return env->NewStringUTF(status);
//	}
//
//	AP4_Track* track_video = input_movie->GetTrack(AP4_Track::TYPE_VIDEO);
//	AP4_Track* track_video2 = input_movie2->GetTrack(AP4_Track::TYPE_VIDEO);
//
//	AP4_Track* track_audio = input_movie->GetTrack(AP4_Track::TYPE_AUDIO);
//	AP4_Track* track_audio2 = input_movie2->GetTrack(AP4_Track::TYPE_AUDIO);
//
//
//
//	// create a sample tables
//	AP4_SyntheticSampleTable* sample_video_table = new AP4_SyntheticSampleTable();
//	AP4_SyntheticSampleTable* sample_audio_table = new AP4_SyntheticSampleTable();
//
//	AP4_SyntheticSampleTable* sample_video = (AP4_SyntheticSampleTable*)track_video->GetSampleTable();
//	AP4_SyntheticSampleTable* sample_video2 = (AP4_SyntheticSampleTable*)track_video2->GetSampleTable();
//	AP4_SyntheticSampleTable* sample_audio = (AP4_SyntheticSampleTable*)track_audio->GetSampleTable();
//	AP4_SyntheticSampleTable* sample_audio2 = (AP4_SyntheticSampleTable*)track_audio2->GetSampleTable();
//
//
//
//	/*
//	 * ADD VIDEO TRACKS FROM 2 INPUT FILES
//	 */
//	// add clones of the sample descriptions to the new sample table
//	for (unsigned int i=0; ;i++) {
//		AP4_SampleDescription* sample_description = sample_video->GetSampleDescription(i);
//		if (sample_description == NULL) break;
//		sample_video_table->AddSampleDescription(sample_description->Clone());
//	}
//
//	AP4_Sample  sample;
//	AP4_Ordinal index = 0;
//	while (AP4_SUCCEEDED(sample_video->GetSample(index, sample)))
//	{
//		sample.SetDts(0);
//		AP4_ByteStream* data_stream;
//		data_stream = sample.GetDataStream();
//		sample_video_table->AddSample(*data_stream,
//								sample.GetOffset(),
//								sample.GetSize(),
//								sample.GetDuration(),
//								sample.GetDescriptionIndex(),
//								sample.GetDts(),
//								sample.GetCtsDelta(),
//								sample.IsSync());
//		AP4_RELEASE(data_stream); // release our ref, the table has kept its own ref.
//		index++;
//	}
//
//	index = 0;
//	while (AP4_SUCCEEDED(sample_video2->GetSample(index, sample)))
//	{
//		sample.SetDts(0);
//		AP4_ByteStream* data_stream;
//		data_stream = sample.GetDataStream();
//		sample_video_table->AddSample(*data_stream,
//								sample.GetOffset(),
//								sample.GetSize(),
//								sample.GetDuration(),
//								sample.GetDescriptionIndex(),
//								sample.GetDts(),
//								sample.GetCtsDelta(),
//								sample.IsSync());
//		AP4_RELEASE(data_stream); // release our ref, the table has kept its own ref.
//		index++;
//	}
//
//
//	AP4_UI32 duration_movie1     = track_video->GetDuration();
//	AP4_UI32 duration_movie2     = track_video2->GetDuration();
//
//	AP4_UI32 duration_media1      = track_video->GetMediaDuration();
//	AP4_UI32 duration_media2      = track_video2->GetMediaDuration();
//
//	// create the output video track
//	AP4_Track* output_video_track = new AP4_Track(track_video->GetType(),
//												  sample_video_table,
//												  0,
//												  track_video->GetMovieTimeScale(),
//												  duration_movie1 + duration_movie2,
//												  track_video->GetMediaTimeScale(),
//												  duration_media1 + duration_media2,
//												  track_video->GetTrackLanguage(),
//												  track_video->GetWidth(),
//												  track_video->GetHeight());
//
//	output_movie->AddTrack(output_video_track);
//
//
//	/*
//	 * ADD AUDIO TRACKS FROM 2 INPUT FILES
//	 */
//	// add clones of the sample descriptions to the new sample table
//	for (unsigned int i=0; ;i++) {
//		AP4_SampleDescription* sample_description = sample_audio->GetSampleDescription(i);
//		if (sample_description == NULL) break;
//		sample_audio_table->AddSampleDescription(sample_description->Clone());
//	}
//
//	index = 0;
//	while (AP4_SUCCEEDED(sample_audio->GetSample(index, sample)))
//	{
//		sample.SetDts(0);
//		AP4_ByteStream* data_stream;
//		data_stream = sample.GetDataStream();
//		sample_audio_table->AddSample(*data_stream,
//								sample.GetOffset(),
//								sample.GetSize(),
//								sample.GetDuration(),
//								sample.GetDescriptionIndex(),
//								sample.GetDts(),
//								sample.GetCtsDelta(),
//								sample.IsSync());
//		AP4_RELEASE(data_stream); // release our ref, the table has kept its own ref.
//		index++;
//	}
//
//	index = 0;
//	while (AP4_SUCCEEDED(sample_audio2->GetSample(index, sample)))
//	{
//		sample.SetDts(0);
//		AP4_ByteStream* data_stream;
//		data_stream = sample.GetDataStream();
//		sample_audio_table->AddSample(*data_stream,
//								sample.GetOffset(),
//								sample.GetSize(),
//								sample.GetDuration(),
//								sample.GetDescriptionIndex(),
//								sample.GetDts(),
//								sample.GetCtsDelta(),
//								sample.IsSync());
//		AP4_RELEASE(data_stream); // release our ref, the table has kept its own ref.
//		index++;
//	}
//
//
//	// create the output video track
//	AP4_Track* output_audio_track = new AP4_Track(track_audio->GetType(),
//												  sample_audio_table,
//												  0,
//												  track_audio->GetMovieTimeScale(),
//												  duration_movie1 + duration_movie2,
//												  track_audio->GetMediaTimeScale(),
//												  duration_media1 + duration_media2,
//												  track_audio->GetTrackLanguage(),
//												  0,
//												  0);
//
//	output_movie->AddTrack(output_audio_track);



	// create a multimedia file
	AP4_File file(output_movie);

	// setup the brands
	AP4_Array<AP4_UI32> brands;
	brands.Append(AP4_FILE_BRAND_ISOM);
	brands.Append(AP4_FILE_BRAND_ISO2);
	brands.Append(AP4_FILE_BRAND_AVC1);

	// set the file type
	file.SetFileType(AP4_FILE_BRAND_ISOM, 0, &brands[0], brands.ItemCount());

//	__android_log_print(ANDROID_LOG_ERROR, "Mp4Editor", "Start to write file");
	// write the file to the output
	AP4_FileWriter::Write(file, *output);
//	__android_log_print(ANDROID_LOG_ERROR, "Mp4Editor", "Output file writed");

	// cleanup
	output->Release();




//
//	track = track->Clone();
//	track2 = track2->Clone();
//	// reset the track ID so that it can be re-assigned
//	track->SetId(0);
//	track2->SetId(0);
//	output_movie->AddTrack(track);
//	output_movie->AddTrack(track2);

//	while (track_item)
//	{
//		AP4_Track* track = track_item->GetData();
//		track = track->Clone();
//		// reset the track ID so that it can be re-assigned
//		track->SetId(0);
//
//		__android_log_print(ANDROID_LOG_ERROR, "Mp4Editor", "Add Track type %d", track->GetType());
//
//		output_movie->AddTrack(track);
//		track_item = track_item->GetNext();
//	}
//
//	//Put tracks from second input file to output Movie
//	while (track_item2)
//	{
//		AP4_Track* track = track_item2->GetData();
//
////		AP4_Track new_track(track->getType(),
////		              AP4_SampleTable* sample_table,     // ownership is transfered to the AP4_Track object
////		              AP4_UI32         track_id,
////		              AP4_UI32         movie_time_scale, // 0 = use default
////		              AP4_UI64         track_duration,   // in the movie timescale
////		              AP4_UI32         media_time_scale,
////		              AP4_UI64         media_duration,   // in the media timescale
////		              const char*      language,
////		              AP4_UI32         width,            // in 16.16 fixed point
////		              AP4_UI32         height);          // in 16.16 fixed point
//
//		track = track->Clone();
//		// reset the track ID so that it can be re-assigned
//		track->SetId(0);
//		__android_log_print(ANDROID_LOG_ERROR, "Mp4Editor", "Add Track2 type %d", track->GetType());
//		output_movie->AddTrack(track);
//		track_item2 = track_item2->GetNext();
//	}
//
//
//	// create a multimedia file
//	AP4_File file(output_movie);
//
//	// setup the brands
//	AP4_Array<AP4_UI32> brands;
//	brands.Append(AP4_FILE_BRAND_ISOM);
//	brands.Append(AP4_FILE_BRAND_MP42);
//	brands.Append(AP4_FILE_BRAND_AVC1);
//
//	// set the file type
//	file.SetFileType(AP4_FILE_BRAND_MP42, 0);
//
//	// write the file to the output
//	AP4_FileWriter::Write(file, *output);
//
//	// cleanup
//	output->Release();

	sprintf (status, "Append finished\n");

	return env->NewStringUTF(status);
}





