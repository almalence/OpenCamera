package com.almalence.plugins.capture.video;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.util.Log;

@SuppressLint("NewApi")
public class AudioRecorder
{
	public static final String		TAG					= "Almalence";

	public static final String		MIME_TYPE_AUDIO		= "audio/mp4a-latm";
	public static final int			SAMPLE_RATE			= 44100;
	public static final int			CHANNEL_COUNT		= 1;
	public static final int			CHANNEL_CONFIG		= AudioFormat.CHANNEL_IN_MONO;
	public static final int			BIT_RATE_AUDIO		= 128000;
	public static final int			SAMPLES_PER_FRAME	= 1024;							// AAC
	public static final int			FRAMES_PER_BUFFER	= 24;
	public static final int			AUDIO_FORMAT		= AudioFormat.ENCODING_PCM_16BIT;
	public static final int			AUDIO_SOURCE		= MediaRecorder.AudioSource.MIC;
	public static final int			TIMEOUT_USEC		= 10000;

	private final MediaMuxer		muxer;
	private final EncodingThread	encodingThread;
	private final Object			sync				= new Object();
	private boolean					started				= false;
	private boolean					running				= false;
	private volatile boolean		record				= false;
	private volatile long			time				= 0;
	private volatile long			timeOrigin			= 0;

	public AudioRecorder(final MediaMuxer muxer)
	{
		this.muxer = muxer;

		this.encodingThread = new EncodingThread();
	}

	public void updateTime(final long time)
	{
		this.time = time;
		this.timeOrigin = System.nanoTime();
	}

	public void record(final boolean record)
	{
		this.record = record;
	}

	public void start()
	{
		synchronized (this.sync)
		{
			if (this.started)
			{
				throw new IllegalStateException("Already started");
			}

			this.started = true;
			this.running = true;

			synchronized (this.encodingThread.sync)
			{
				this.encodingThread.start();

				try
				{
					this.encodingThread.sync.wait();
				} catch (final InterruptedException e)
				{
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	public void stop()
	{
		synchronized (this.sync)
		{
			if (!this.started)
			{
				throw new IllegalStateException("Not started");
			} else if (!this.running)
			{
				throw new IllegalStateException("Already finished");
			}

			this.running = false;

			this.encodingThread.finish();
		}
	}

	private class EncodingThread extends Thread
	{
		private final Object		sync		= new Object();
		private final MediaCodec	encoder;

		private volatile boolean	running		= true;
		private int					iBufferSize;
		private long				timeLast	= 0;

		public EncodingThread()
		{
			final int iMinBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

			this.iBufferSize = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
			if (this.iBufferSize < iMinBufferSize)
			{
				this.iBufferSize = ((iMinBufferSize / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
			}

			final MediaFormat format = MediaFormat.createAudioFormat(MIME_TYPE_AUDIO, SAMPLE_RATE, CHANNEL_COUNT);
			format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
			format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);
			format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE_AUDIO);

			MediaCodec newEncoder = null;
			try
			{
				newEncoder = MediaCodec.createEncoderByType(MIME_TYPE_AUDIO);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.encoder = newEncoder;
			this.encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
			this.encoder.start();
		}

		@Override
		public void run()
		{
			final byte[] recordedBytes = new byte[SAMPLES_PER_FRAME];

			final AudioRecord audioRecorder = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT,
					this.iBufferSize);
			audioRecorder.startRecording();

			final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
			int track = 0;
			ByteBuffer[] buffersInput = null;
			ByteBuffer[] buffersOutput = this.encoder.getOutputBuffers();
			ByteBuffer encodedData;

			while (this.running)
			{
				final int resultRead = audioRecorder.read(recordedBytes, 0, SAMPLES_PER_FRAME);

				if (resultRead == AudioRecord.ERROR_BAD_VALUE || resultRead == AudioRecord.ERROR_INVALID_OPERATION)
				{
					break;
				}

				if (AudioRecorder.this.record)
				{
					try
					{
						if (buffersInput == null)
							buffersInput = this.encoder.getInputBuffers();

						int inputBufferIndex = this.encoder.dequeueInputBuffer(-1);
						if (inputBufferIndex >= 0)
						{
							final ByteBuffer inputBuffer = buffersInput[inputBufferIndex];
							inputBuffer.clear();
							inputBuffer.put(recordedBytes);

							this.timeLast = Math
									.max(this.timeLast + 1,
											(AudioRecorder.this.time + (System.nanoTime() - AudioRecorder.this.timeOrigin)) / 1000);

							this.encoder.queueInputBuffer(inputBufferIndex, 0, recordedBytes.length, this.timeLast, 0);
						}
					} catch (final Throwable t)
					{
						Log.e(TAG, "sendFrameToAudioEncoder exception");
						t.printStackTrace();
						break;
					}
				}

				final int encoderStatus = this.encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
				if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER)
				{

				} else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)
				{
					// not expected for an encoder
					buffersOutput = this.encoder.getOutputBuffers();
				} else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
				{
					final MediaFormat newFormat = this.encoder.getOutputFormat();

					track = AudioRecorder.this.muxer.addTrack(newFormat);

					synchronized (this.sync)
					{
						sync.notify();
					}

				} else if (encoderStatus < 0)
				{
					Log.w(TAG, "Unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
				} else
				{
					encodedData = buffersOutput[encoderStatus];
					if (encodedData == null)
					{
						throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
					}

					if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)
					{
						// The codec config data was pulled out and fed
						// to the muxer when we got
						// the INFO_OUTPUT_FORMAT_CHANGED status. Ignore
						// it.
						bufferInfo.size = 0;
					}

					if (bufferInfo.size != 0)
					{
						// adjust the ByteBuffer values to match
						// BufferInfo (not needed?)
						encodedData.position(bufferInfo.offset);
						encodedData.limit(bufferInfo.offset + bufferInfo.size);

						if (AudioRecorder.this.record)
						{
							synchronized (AudioRecorder.this.muxer)
							{
								AudioRecorder.this.muxer.writeSampleData(track, encodedData, bufferInfo);
							}
						}
					}

					this.encoder.releaseOutputBuffer(encoderStatus, false);

					if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
					{
						Log.w(TAG, "AudioRecorder: EOS reached.");
						break;
					}
				}
			}

			audioRecorder.stop();
			audioRecorder.release();

			this.encoder.stop();
			this.encoder.release();
		}

		public void finish()
		{
			this.running = false;
		}
	}

}
