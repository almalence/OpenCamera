package com.almalence.plugins.capture.video;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

@SuppressLint("NewApi")
public class EglEncoder
{
	private static final String			TAG				= "Almalence";
	private static final boolean		VERBOSE			= false;		// lots
																		// of
																		// logging

	private static final String			SHADER_VERTEX	= "attribute vec2 vPosition;\n"
																+ "attribute vec2 vTexCoord;\n"
																+ "varying vec2 texCoord;\n"
																+ "void main() {\n"
																+ "  texCoord = vTexCoord;\n"
																+ "  gl_Position = vec4 ( vPosition.x, vPosition.y, 1.0, 1.0 );\n"
																+ "}";

	private static final String			SHADER_FRAGMENT	= "#extension GL_OES_EGL_image_external:enable\n"
																+ "precision mediump float;\n"
																+ "uniform samplerExternalOES sTexture;\n"
																+ "varying vec2 texCoord;\n" + "void main() {\n"
																+ "  gl_FragColor = texture2D(sTexture, texCoord);\n"
																+ "}";

	private static final FloatBuffer	VERTEX_BUFFER;

	static
	{
		final float[] vtmp = { 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f };
		VERTEX_BUFFER = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		VERTEX_BUFFER.put(vtmp);
		VERTEX_BUFFER.position(0);
	}

	private static int loadShader(final String vss, final String fss)
	{
		int vshader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
		GLES20.glShaderSource(vshader, vss);
		GLES20.glCompileShader(vshader);
		final int[] compiled = new int[1];
		GLES20.glGetShaderiv(vshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0)
		{
			Log.d(TAG, "Could not compile vertex shader: " + GLES20.glGetShaderInfoLog(vshader));
			GLES20.glDeleteShader(vshader);
			vshader = 0;
		}

		int fshader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
		GLES20.glShaderSource(fshader, fss);
		GLES20.glCompileShader(fshader);
		GLES20.glGetShaderiv(fshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0)
		{
			Log.d(TAG, "Could not compile fragment shader: " + GLES20.glGetShaderInfoLog(fshader));
			GLES20.glDeleteShader(fshader);
			fshader = 0;
		}

		final int program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, vshader);
		GLES20.glAttachShader(program, fshader);
		GLES20.glLinkProgram(program);

		return program;
	}

	// parameters for the encoder
	private static final String		MIME_TYPE				= "video/avc";	// H.264
																			// Advanced
																			// Video
																			// Coding
	private static final int		IFRAME_INTERVAL			= 10;			// 10
																			// seconds
																			// between
																			// I-frames

	private static final int		EGL_RECORDABLE_ANDROID	= 0x3142;
	public static final int[]		EGL_ATTRIB_LIST			= { EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8,
			EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8, EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
			EGL_RECORDABLE_ANDROID, 1, EGL14.EGL_NONE		};

	private final String			outputPath;
	private final int				mWidth;
	private final int				mHeight;
	private final int				definedFPS;
	private final int				mBitRate;

	// encoder / muxer state
	private MediaCodec				mEncoder;
	private CodecInputSurface		mInputSurface;
	private MediaMuxer				mMuxer;
	private int						mTrackIndex;
	private boolean					mMuxerStarted;

	// allocate one of these up front so we don't need to do it every time
	private MediaCodec.BufferInfo	mBufferInfo;

	private long					timeLast				= -1;
	private long					timeTotal				= 0;

	private boolean					open					= true;

	private int						hProgram;

	private final FloatBuffer		UV_BUFFER;

	private AudioRecorder			audioRecorder;

	private volatile boolean		paused					= true;

	public EglEncoder(final String outputPath, final int width, final int height, final int fps, final int bitrate,
			int orientation)
	{
		this.outputPath = outputPath;
		this.mBitRate = bitrate;
		this.definedFPS = fps;

		final float[] ttmp;
		if (orientation == 0)
		{
			this.mWidth = width;
			this.mHeight = height;
			ttmp = new float[] { 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f };
		} else if (orientation == 90)
		{
			this.mWidth = height;
			this.mHeight = width;
			ttmp = new float[] { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f };
		} else if (orientation == 180)
		{
			this.mWidth = width;
			this.mHeight = height;
			ttmp = new float[] { 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f };
		} else if (orientation == 270)
		{
			this.mWidth = height;
			this.mHeight = width;
			ttmp = new float[] { 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f };
		} else
		{
			throw new IllegalArgumentException("Orientation can only be 0, 90, 180 or 270");
		}

		this.UV_BUFFER = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		this.UV_BUFFER.put(ttmp);
		this.UV_BUFFER.position(0);

		this.prepareEncoder();

		this.hProgram = loadShader(SHADER_VERTEX, SHADER_FRAGMENT);
	}

	public String getPath()
	{
		return this.outputPath;
	}

	@Override
	public void finalize() throws Throwable
	{
		try
		{
			this.close();
		} catch (final IllegalStateException e)
		{
			// Totally normal
		}

		super.finalize();
	}

	private boolean checkPaused()
	{
		if (this.paused)
		{
			if (this.mMuxerStarted)
			{
				this.paused = false;
				this.audioRecorder.record(true);
			}

			return true;
		} else
		{
			return false;
		}
	}

	public void pause()
	{
		this.paused = true;
		this.audioRecorder.record(false);
	}

	public void encode(final int texture)
	{
		final long time = System.nanoTime();
		final long timeDiff;

		if (this.checkPaused())
		{
			timeDiff = 0;
		} else
		{
			timeDiff = time - this.timeLast;
		}

		if (this.timeLast >= 0)
		{
			this.timeTotal += timeDiff;
		}

		this.timeLast = time;

		this.audioRecorder.updateTime(this.timeTotal);

		this.drawEncode(texture);
	}

	public void encode(final int texture, final long nanoSec)
	{
		if (nanoSec < 0)
		{
			throw new IllegalArgumentException("Time shift can't be negative.");
		}

		this.checkPaused();

		this.timeLast = System.nanoTime();
		this.timeTotal += nanoSec;
		this.audioRecorder.updateTime(this.timeTotal);

		this.drawEncode(texture);
	}

	private void drawEncode(final int texture)
	{
		this.mInputSurface.makeCurrent();

		this.drainEncoder(false);
		this.drawTexture(texture);
		this.mInputSurface.setPresentationTime(this.timeTotal);

		this.mInputSurface.swapBuffers();

		this.mInputSurface.unmakeCurrent();
	}

	private void drawTexture(final int texture)
	{
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

		GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		GLES20.glViewport(0, 0, this.mWidth, this.mHeight);

		GLES20.glUseProgram(this.hProgram);

		final int ph = GLES20.glGetAttribLocation(this.hProgram, "vPosition");
		final int tch = GLES20.glGetAttribLocation(this.hProgram, "vTexCoord");
		final int th = GLES20.glGetUniformLocation(this.hProgram, "sTexture");

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture);
		GLES20.glUniform1i(th, 0);

		GLES20.glVertexAttribPointer(ph, 2, GLES20.GL_FLOAT, false, 4 * 2, VERTEX_BUFFER);
		GLES20.glVertexAttribPointer(tch, 2, GLES20.GL_FLOAT, false, 4 * 2, this.UV_BUFFER);
		GLES20.glEnableVertexAttribArray(ph);
		GLES20.glEnableVertexAttribArray(tch);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		GLES20.glDisableVertexAttribArray(ph);
		GLES20.glDisableVertexAttribArray(tch);
	}

	public void close()
	{
		if (this.open)
		{
			this.open = false;
			this.drainEncoder(true);
			this.releaseEncoder();
		} else
		{
			throw new IllegalStateException("Already closed.");
		}
	}

	/**
	 * Returns the first codec capable of encoding the specified MIME type, or
	 * null if no match was found.
	 */
	private static MediaCodecInfo selectCodec(String mimeType)
	{
		int numCodecs = MediaCodecList.getCodecCount();
		for (int i = 0; i < numCodecs; i++)
		{
			MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

			if (!codecInfo.isEncoder())
			{
				continue;
			}

			String[] types = codecInfo.getSupportedTypes();
			for (int j = 0; j < types.length; j++)
			{
				if (types[j].equalsIgnoreCase(mimeType))
				{
					return codecInfo;
				}
			}
		}
		return null;
	}

	/**
	 * Returns a color format that is supported by the codec and by this test
	 * code. If no match is found, this throws a test failure -- the set of
	 * formats known to the test should be expanded for new platforms.
	 */
	public static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType)
	{
		MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
		for (int i = 0; i < capabilities.colorFormats.length; i++)
		{
			int colorFormat = capabilities.colorFormats[i];
			if (isRecognizedFormat(colorFormat))
			{
				return colorFormat;
			}
		}
		Log.d(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
		return 0; // not reached
	}

	/**
	 * Returns true if this is a color format that this test code understands
	 * (i.e. we know how to read and generate frames in this format).
	 */
	private static boolean isRecognizedFormat(int colorFormat)
	{
		switch (colorFormat)
		{
		// these are the formats we know how to handle for this test
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Configures encoder and muxer state, and prepares the input Surface.
	 */
	private void prepareEncoder()
	{
		this.mBufferInfo = new MediaCodec.BufferInfo();

		final MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
		if (codecInfo == null)
		{
			// Don't fail CTS if they don't have an AVC codec (not here,
			// anyway).
			Log.d(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
			throw new RuntimeException("No codec found.");
		}

		final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, this.mWidth, this.mHeight);

		// Set some properties. Failing to specify some of these can cause the
		// MediaCodec
		// configure() call to throw an unhelpful exception.

		// Video
		format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
		format.setInteger(MediaFormat.KEY_BIT_RATE, this.mBitRate);
		format.setInteger(MediaFormat.KEY_FRAME_RATE, this.definedFPS);
		format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
		format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 0);
		format.setInteger(MediaFormat.KEY_PUSH_BLANK_BUFFERS_ON_STOP, 0);

		// Audio
		format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 64000);
		format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);

		if (VERBOSE)
			Log.d(TAG, "format: " + format);

		// Create a MediaCodec encoder, and configure it with our format. Get a
		// Surface
		// we can use for input and wrap it with a class that handles the EGL
		// work.
		//
		// If you want to have two EGL contexts -- one for display, one for
		// recording --
		// you will likely want to defer instantiation of CodecInputSurface
		// until after the
		// "display" EGL context is created, then modify the eglCreateContext
		// call to
		// take eglGetCurrentContext() as the share_context argument.
		try
		{
			this.mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
		}
		catch (IOException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		this.mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		this.mInputSurface = new CodecInputSurface(this.mEncoder.createInputSurface());
		this.mEncoder.start();

		// Create a MediaMuxer. We can't add the video track and start() the
		// muxer here,
		// because our MediaFormat doesn't have the Magic Goodies. These can
		// only be
		// obtained from the encoder after it has started processing data.
		//
		// We're not actually interested in multiplexing audio. We just want to
		// convert
		// the raw H.264 elementary stream we get from MediaCodec into a .mp4
		// file.
		try
		{
			this.mMuxer = new MediaMuxer(this.outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
			this.audioRecorder = new AudioRecorder(this.mMuxer);
			this.audioRecorder.start();
		} catch (final IOException e)
		{
			e.printStackTrace();
			throw new RuntimeException("MediaMuxer creation failed");
		}

		this.mTrackIndex = -1;
		this.mMuxerStarted = false;
	}

	/**
	 * Releases encoder resources. May be called after partial / failed
	 * initialization.
	 */
	private void releaseEncoder()
	{
		if (VERBOSE)
			Log.d(TAG, "releasing encoder objects");

		if (this.audioRecorder != null)
		{
			this.audioRecorder.stop();
			this.audioRecorder = null;
		}
		if (this.mEncoder != null)
		{
			this.mEncoder.stop();
			this.mEncoder.release();
			this.mEncoder = null;
		}
		if (this.mInputSurface != null)
		{
			this.mInputSurface.release();
			this.mInputSurface = null;
		}
		if (this.mMuxer != null)
		{
			this.mMuxer.stop();
			this.mMuxer.release();
			this.mMuxer = null;
		}
	}

	/**
	 * Extracts all pending data from the encoder.
	 * <p>
	 * If endOfStream is not set, this returns when there is no more data to
	 * drain. If it is set, we send EOS to the encoder, and then iterate until
	 * we see EOS on the output. Calling this with endOfStream set should be
	 * done once, right before stopping the muxer.
	 */
	private void drainEncoder(boolean endOfStream)
	{
		final int TIMEOUT_USEC = 10000;
		if (VERBOSE)
			Log.d(TAG, "drainEncoder(" + endOfStream + ")");

		if (endOfStream)
		{
			if (VERBOSE)
				Log.d(TAG, "sending EOS to encoder");
			this.mEncoder.signalEndOfInputStream();
		}

		ByteBuffer[] encoderOutputBuffers = this.mEncoder.getOutputBuffers();
		while (true)
		{
			final int encoderStatus = this.mEncoder.dequeueOutputBuffer(this.mBufferInfo, TIMEOUT_USEC);
			if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER)
			{
				// no output available yet
				if (!endOfStream)
				{
					break; // out of while
				} else
				{
					if (VERBOSE)
						Log.d(TAG, "no output available, spinning to await EOS");
				}
			} else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)
			{
				// not expected for an encoder
				encoderOutputBuffers = this.mEncoder.getOutputBuffers();
			} else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
			{
				// should happen before receiving buffers, and should only
				// happen once
				if (this.mMuxerStarted)
				{
					throw new RuntimeException("format changed twice");
				}
				final MediaFormat newFormat = this.mEncoder.getOutputFormat();
				Log.d(TAG, "encoder output format changed: " + newFormat);

				// now that we have the Magic Goodies, start the muxer
				this.mTrackIndex = this.mMuxer.addTrack(newFormat);
				this.mMuxer.start();
				this.mMuxerStarted = true;
			} else if (encoderStatus < 0)
			{
				Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
				// let's ignore it
			} else
			{
				final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
				if (encodedData == null)
				{
					throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
				}

				if ((this.mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)
				{
					// The codec config data was pulled out and fed to the muxer
					// when we got
					// the INFO_OUTPUT_FORMAT_CHANGED status. Ignore it.
					if (VERBOSE)
						Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
					this.mBufferInfo.size = 0;
				}

				if (this.mBufferInfo.size != 0)
				{
					if (!this.mMuxerStarted)
					{
						throw new RuntimeException("muxer hasn't started");
					}

					// adjust the ByteBuffer values to match BufferInfo (not
					// needed?)
					encodedData.position(this.mBufferInfo.offset);
					encodedData.limit(this.mBufferInfo.offset + this.mBufferInfo.size);

					synchronized (this.mMuxer)
					{
						this.mMuxer.writeSampleData(this.mTrackIndex, encodedData, this.mBufferInfo);
					}
					if (VERBOSE)
						Log.d(TAG, "sent " + this.mBufferInfo.size + " bytes to muxer");
				}

				this.mEncoder.releaseOutputBuffer(encoderStatus, false);

				if ((this.mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
				{
					if (!endOfStream)
					{
						Log.w(TAG, "reached end of stream unexpectedly");
					} else
					{
						if (VERBOSE)
							Log.d(TAG, "end of stream reached");
					}
					break; // out of while
				}
			}
		}
	}

	/**
	 * Holds state associated with a Surface used for MediaCodec encoder input.
	 * <p>
	 * The constructor takes a Surface obtained from
	 * MediaCodec.createInputSurface(), and uses that to create an EGL window
	 * surface. Calls to eglSwapBuffers() cause a frame of data to be sent to
	 * the video encoder.
	 * <p>
	 * This object owns the Surface -- releasing this will release the Surface
	 * too.
	 */
	private static class CodecInputSurface
	{
		private EGLDisplay	eglDisplay		= EGL14.EGL_NO_DISPLAY;
		private EGLContext	eglContext		= EGL14.EGL_NO_CONTEXT;
		private EGLSurface	eglSurfaceDraw	= EGL14.EGL_NO_SURFACE;
		private EGLSurface	eglSurfaceRead	= EGL14.EGL_NO_SURFACE;
		private EGLSurface	eglSurfaceSwap	= EGL14.EGL_NO_SURFACE;

		private Surface		mSurface;

		/**
		 * Creates a CodecInputSurface from a Surface.
		 */
		public CodecInputSurface(final Surface surface)
		{
			if (surface == null)
			{
				throw new NullPointerException();
			}
			this.mSurface = surface;

			this.eglSetup();
		}

		/**
		 * Prepares EGL. We want a GLES 2.0 context and a surface that supports
		 * recording.
		 */
		private void eglSetup()
		{
			this.eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
			if (this.eglDisplay == EGL14.EGL_NO_DISPLAY)
			{
				throw new RuntimeException("unable to get EGL14 display");
			}

			this.eglContext = EGL14.eglGetCurrentContext();
			if (this.eglContext == EGL14.EGL_NO_CONTEXT)
			{
				throw new RuntimeException("unable to get EGL14 context");
			}

			this.eglSurfaceDraw = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
			this.eglSurfaceRead = EGL14.eglGetCurrentSurface(EGL14.EGL_READ);

			EGLConfig[] configs = new EGLConfig[1];
			int[] numConfigs = new int[1];
			EGL14.eglChooseConfig(this.eglDisplay, EGL_ATTRIB_LIST, 0, configs, 0, configs.length, numConfigs, 0);
			checkEglError("eglCreateContext RGB888+recordable ES2");

			// Create a window surface, and attach it to the Surface we
			// received.
			final int[] surfaceAttribs = { EGL14.EGL_NONE };
			this.eglSurfaceSwap = EGL14.eglCreateWindowSurface(this.eglDisplay, configs[0], this.mSurface,
					surfaceAttribs, 0);
			checkEglError("eglCreateWindowSurface");
		}

		/**
		 * Discards all resources held by this class, notably the EGL context.
		 * Also releases the Surface that was passed to our constructor.
		 */
		public void release()
		{
			if (this.eglDisplay != EGL14.EGL_NO_DISPLAY)
			{
				EGL14.eglDestroySurface(this.eglDisplay, this.eglSurfaceSwap);
			}

			this.mSurface.release();
			this.mSurface = null;

			this.eglDisplay = EGL14.EGL_NO_DISPLAY;
			this.eglContext = EGL14.EGL_NO_CONTEXT;
			this.eglSurfaceRead = EGL14.EGL_NO_SURFACE;
			this.eglSurfaceDraw = EGL14.EGL_NO_SURFACE;
			this.eglSurfaceSwap = EGL14.EGL_NO_SURFACE;
		}

		/**
		 * Makes our EGL context and surface current.
		 */
		public void makeCurrent()
		{
			EGL14.eglMakeCurrent(this.eglDisplay, this.eglSurfaceSwap, this.eglSurfaceSwap, this.eglContext);
			checkEglError("eglMakeCurrent");
		}

		public void unmakeCurrent()
		{
			EGL14.eglMakeCurrent(this.eglDisplay, this.eglSurfaceDraw, this.eglSurfaceRead, this.eglContext);
		}

		/**
		 * Calls eglSwapBuffers. Use this to "publish" the current frame.
		 */
		public boolean swapBuffers()
		{
			final boolean result = EGL14.eglSwapBuffers(this.eglDisplay, this.eglSurfaceSwap);
			checkEglError("eglSwapBuffers");
			return result;
		}

		/**
		 * Sends the presentation time stamp to EGL. Time is expressed in
		 * nanoseconds.
		 */
		public void setPresentationTime(final long nsecs)
		{
			EGLExt.eglPresentationTimeANDROID(this.eglDisplay, this.eglSurfaceSwap, nsecs);
			checkEglError("eglPresentationTimeANDROID");
		}

		/**
		 * Checks for EGL errors. Throws an exception if one is found.
		 */
		private static void checkEglError(String msg)
		{
			int error;
			if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS)
			{
				throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
			}
		}
	}
}
