#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#include "Ap4.h"

const unsigned int AP4_MUX_DEFAULT_VIDEO_FRAME_RATE = 24;

extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_capture_video_Mp4Editor_appendFds
(
	JNIEnv* env,
	jobject thiz,
	jintArray inputFiles,
	jint newFile
)
{
	char status[1024];

	int output_fd = newFile;

	// create the output stream
	AP4_ByteStream* output = NULL;
	AP4_Result result;
	result = AP4_FileByteStream::Create(output_fd, AP4_FileByteStream::STREAM_MODE_WRITE, output);
	if (AP4_FAILED(result))
	{
		sprintf (status, "ERROR: cannot open output file (%d)\n", output_fd);

		return env->NewStringUTF(status);
	}

	//Create output Movie
	AP4_Movie* output_movie = new AP4_Movie(1000);
	if (output_movie == NULL)
	{
		sprintf (status, "ERROR: output file (%d) doesn't have Movie object\n", output_fd);
		return env->NewStringUTF(status);
	}

	// create an output sample tables
	AP4_SyntheticSampleTable* sample_video_table = new AP4_SyntheticSampleTable();
	AP4_SyntheticSampleTable* sample_audio_table = new AP4_SyntheticSampleTable();

	//Prototypes track is used to pass transform matrix from input files to ouput
	AP4_Track* prototype_track_video = NULL;

	AP4_UI64 duration_movie = 0;
	AP4_UI64 duration_media = 0;
	AP4_UI64 duration_media_audio = 0;

	AP4_UI32         movie_time_scale 		= 0;
	AP4_UI32         media_time_scale 		= 0;
	AP4_UI32         movie_time_scale_audio = 0;
	AP4_UI32         media_time_scale_audio = 0;
	const char*      language 		  		= NULL;
	AP4_UI32         width 			  		= 0;
	AP4_UI32         height 		 		= 0;

	//developer noticed, that sample should be copied only once!!!
	bool sampleOnce = true;

	int inputFileCount = env->GetArrayLength(inputFiles);
	jint *fileFds = env->GetIntArrayElements(inputFiles, 0);
	for (int i = 0; i < inputFileCount; i++)
	{
		jint fileFd = fileFds[i];

		// create the first input stream
		AP4_ByteStream* input = NULL;
		result = AP4_FileByteStream::Create(fileFd, AP4_FileByteStream::STREAM_MODE_READ, input);
		if (AP4_FAILED(result))
		{
			sprintf (status, "ERROR: cannot open input file (%d)\n", fileFd);
			return env->NewStringUTF(status);
		}

		//Get Movie from first file
		AP4_File first_file(*input);
		input->Release();
		AP4_Movie* input_movie = first_file.GetMovie();
		if (input_movie == NULL)
		{
			sprintf (status, "ERROR: input file (%d) doesn't have Movie object\n", fileFd);
			return env->NewStringUTF(status);
		}

		AP4_Track* track_video = input_movie->GetTrack(AP4_Track::TYPE_VIDEO);
		AP4_Track* track_audio = input_movie->GetTrack(AP4_Track::TYPE_AUDIO);

		AP4_SyntheticSampleTable* sample_video = (AP4_SyntheticSampleTable*)track_video->GetSampleTable();
		AP4_SyntheticSampleTable* sample_audio = (AP4_SyntheticSampleTable*)track_audio->GetSampleTable();


		if(i == 0)
		{
			prototype_track_video  = track_video->Clone();
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
		if (sampleOnce)
		for (unsigned int j=0; ;j++)
		{
			AP4_SampleDescription* sample_descriptionVideo = sample_video->GetSampleDescription(j);
			if (sample_descriptionVideo == NULL) break;
			sample_video_table->AddSampleDescription(sample_descriptionVideo->Clone());
			break;
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

		duration_movie = duration_movie + track_video->GetDuration();
		duration_media = duration_media + track_video->GetMediaDuration();
		duration_media_audio = duration_media_audio + track_audio->GetMediaDuration();



		/*
		 * ADD AUDIO SAMPLES
		 */
		// add clones of the sample descriptions to the new sample table
		if (sampleOnce)
		for (unsigned int k=0; ;k++)
		{
			AP4_SampleDescription* sample_descriptionAudio = sample_audio->GetSampleDescription(k);
			if (sample_descriptionAudio == NULL) break;
			sample_audio_table->AddSampleDescription(sample_descriptionAudio->Clone());
			break;
		}

		sampleOnce = false;

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
	}


	AP4_UI64 video_track_duration = AP4_ConvertTime(movie_time_scale*sample_video_table->GetSampleCount(), media_time_scale, movie_time_scale);
	AP4_UI64 video_media_duration = media_time_scale*sample_video_table->GetSampleCount();

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
												  duration_media_audio,
												  language,
												  0,
												  0);
	output_movie->AddTrack(output_video_track);
	output_movie->AddTrack(output_audio_track);

	// create a multimedia file
	AP4_File file(output_movie);

	// setup the brands
	AP4_Array<AP4_UI32> brands;
	brands.Append(AP4_FILE_BRAND_ISOM);
	brands.Append(AP4_FILE_BRAND_ISO2);
	brands.Append(AP4_FILE_BRAND_AVC1);
	brands.Append(AP4_FILE_BRAND_3GP4);


	// set the file type
	file.SetFileType(AP4_FILE_BRAND_ISOM, 0, &brands[0], brands.ItemCount());

	// write the file to the output
	AP4_FileWriter::Write(file, *output);

	// cleanup
	output->Release();

	sprintf (status, "Append finished\n");

	return env->NewStringUTF(status);
}



