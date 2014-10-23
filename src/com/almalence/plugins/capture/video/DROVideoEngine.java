package com.almalence.plugins.capture.video;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.almalence.util.FpsMeasurer;

import android.content.ContentValues;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;
import android.util.Log;
import android.widget.Toast;

/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
+++ --> */
//<!-- -+-
import com.almalence.opencam.MainScreen;
//-+- -->

public class DROVideoEngine
{
	private static final String			TAG						= "Almalence";

	private static final int			GL_TEXTURE_EXTERNAL_OES	= 0x00008d65;

	private static final String			SHADER_VERTEX = "attribute vec2 vPosition;\n"
														+ "attribute vec2 vTexCoord;\n"
														+ "varying vec2 texCoord;\n"
														+ "void main() {\n"
														+ "  texCoord = vTexCoord;\n"
														+ "  gl_Position = vec4 ( vPosition.x, vPosition.y, 1.0, 1.0 );\n"
														+ "}";

	private static final String			SHADER_FRAGMENT	= "#extension GL_OES_EGL_image_external:enable\n"
														+ "precision mediump float;\n"
														+ "uniform samplerExternalOES sTexture;\n"
														+ "varying vec2 texCoord;\n"
														+ "void main() {\n"
														+ "  gl_FragColor = texture2D(sTexture, texCoord);\n"
														+ "}";

	private static final FloatBuffer	VERTEX_BUFFER;
	private static final FloatBuffer	UV_BUFFER;

	static
	{
		final float[] vtmp = { 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f };
		VERTEX_BUFFER = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		VERTEX_BUFFER.put(vtmp);
		VERTEX_BUFFER.position(0);

		final float[] ttmp = { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f };
		UV_BUFFER = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		UV_BUFFER.put(ttmp);
		UV_BUFFER.position(0);
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

	private final Object		stateSync			= new Object();
	private int					instance			= 0;
	private int					previewWidth		= -1;
	private int					previewHeight		= -1;

	private volatile boolean	local				= true;
	private volatile boolean	forceUpdate			= false;
	private volatile int		uv_desat			= 9;
	private volatile int		dark_uv_desat		= 5;
	private volatile float		dark_noise_pass		= 0.45f;
	private volatile float		mix_factor			= 0.1f;
	private volatile float		gamma				= 0.65f; // 0.5f;
	private volatile float		max_black_level		= 64.0f;
	private volatile float		black_level_atten	= 0.5f;
	private volatile float		max_amplify			= 2.0f;
	private volatile float[]	min_limit			= new float[] { 0.5f, 0.5f, 0.5f };
	private volatile float[]	max_limit			= new float[] { 3.0f, 2.0f, 2.0f };

	private final float[]		transform			= new float[16];
	private volatile boolean	filled				= false;

	private EglEncoder			encoder				= null;

	private FpsMeasurer			fps					= new FpsMeasurer(5);

	private volatile long		recordingDelayed;
	private boolean				paused;

	public DROVideoEngine()
	{

	}

	public void startRecording(final String path, final long delay)
	{
		this.recordingDelayed = System.currentTimeMillis() + delay;

		final Object sync = new Object();
		synchronized (sync)
		{
			MainScreen.getInstance().queueGLEvent(new Runnable()
			{
				@Override
				public void run()
				{
					synchronized (DROVideoEngine.this.stateSync)
					{
						if (DROVideoEngine.this.encoder == null && DROVideoEngine.this.instance != 0)
						{
							DROVideoEngine.this.paused = false;

							try
							{
								DROVideoEngine.this.encoder = new EglEncoder(path, DROVideoEngine.this.previewWidth,
									DROVideoEngine.this.previewHeight, 24, 20000000, (MainScreen.getGUIManager()
											.getDisplayOrientation()) % 360);
							}
							catch(RuntimeException e)
							{
								e.printStackTrace();
								MainScreen.getInstance().runOnUiThread(new Runnable() {
								    public void run() {
								        Toast.makeText(MainScreen.getInstance(), "Can't record HDR Video. MediaMuxer creation failed.", Toast.LENGTH_LONG).show();
								    }
								});
								
							}
						}
					}

					synchronized (sync)
					{
						sync.notify();
					}
				}
			});

			try
			{
				sync.wait();
			} catch (final InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}
	}

	public void stopRecording()
	{
		final Object sync = new Object();
		synchronized (sync)
		{
			MainScreen.getInstance().queueGLEvent(new Runnable()
			{
				@Override
				public void run()
				{
					synchronized (DROVideoEngine.this.stateSync)
					{

						if (DROVideoEngine.this.encoder != null)
						{
							final String path = DROVideoEngine.this.encoder.getPath();
							DROVideoEngine.this.encoder.close();
							DROVideoEngine.this.encoder = null;

							File fileSaved = new File(path);
							File parent = fileSaved.getParentFile();
							String parentPath = parent.toString().toLowerCase(Locale.US);
							String parentName = parent.getName().toLowerCase(Locale.US);
							
							ContentValues values = new ContentValues();
							values.put(VideoColumns.TITLE, fileSaved.getName().substring(0, fileSaved.getName().lastIndexOf(".")));
							values.put(VideoColumns.DISPLAY_NAME, fileSaved.getName());
							values.put(VideoColumns.DATE_TAKEN, System.currentTimeMillis());
							values.put(VideoColumns.MIME_TYPE, "video/mp4");
							values.put(VideoColumns.BUCKET_ID, parentPath.hashCode());
							values.put(VideoColumns.BUCKET_DISPLAY_NAME, parentName);
							values.put(VideoColumns.DATA, fileSaved.getAbsolutePath());
							
							MainScreen.getInstance().getContentResolver().insert(Video.Media.EXTERNAL_CONTENT_URI, values);
						}
					}

					synchronized (sync)
					{
						sync.notify();
					}
				}
			});

			try
			{
				sync.wait();
			} catch (final InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}
	}

	private void stopPreview()
	{
		Log.i(TAG, "DROVideoEngine.stopPreview()");

		this.stopRecording();

		final Object sync = new Object();
		synchronized (sync)
		{
			MainScreen.getInstance().queueGLEvent(new Runnable()
			{
				@Override
				public void run()
				{
					synchronized (DROVideoEngine.this.stateSync)
					{
						if (DROVideoEngine.this.instance != 0)
						{
							Log.i(TAG, "DRO instance is not null. Releasing.");
							RealtimeDRO.release(DROVideoEngine.this.instance);
							DROVideoEngine.this.instance = 0;
							Log.i(TAG, "DRO instance released");
						}
					}

					synchronized (sync)
					{
						sync.notify();
					}
				}
			});

			try
			{
				sync.wait();
			} catch (final InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}
	}

	public void onPause()
	{
		this.stopPreview();
	}

	public void onFrameAvailable()
	{
		this.filled = true;
		MainScreen.getInstance().glRequestRender();
	}

	public void setPaused(final boolean paused)
	{
		MainScreen.getInstance().queueGLEvent(new Runnable()
		{
			@Override
			public void run()
			{
				if (!DROVideoEngine.this.paused && paused && DROVideoEngine.this.encoder != null)
				{
					DROVideoEngine.this.encoder.pause();
				}

				DROVideoEngine.this.paused = paused;
			}
		});
	}

	private int	hProgram;

	private int	surfaceWidth;
	private int	surfaceHeight;

	private int	texture_out;

	private void initGL()
	{
		final int[] tex = new int[1];
		GLES20.glGenTextures(1, tex, 0);
		this.texture_out = tex[0];

		GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, this.texture_out);
		GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

		this.hProgram = loadShader(SHADER_VERTEX, SHADER_FRAGMENT);
	}

	public void onSurfaceCreated(final GL10 gl, final EGLConfig config)
	{
		this.initGL();

		DROVideoEngine.this.fps.flush();

		this.filled = false;
	}

	public void onSurfaceChanged(final GL10 gl, final int width, final int height)
	{
		Log.i(TAG, String.format("DROVideoEngine.onSurfaceChanged(%dx%d)", width, height));

		this.surfaceWidth = width;
		this.surfaceHeight = height;
	}

	public void onDrawFrame(final GL10 gl)
	{
		if (this.filled)
		{
			this.filled = false;

			try
			{
				final SurfaceTexture surfaceTexture = MainScreen.getInstance().glGetSurfaceTexture();
				surfaceTexture.updateTexImage();
				surfaceTexture.getTransformMatrix(this.transform);
			}
			catch (final Exception e)
			{
				return;
			}
		} else
		{
			return;
		}

		synchronized (this.stateSync)
		{
			if (this.instance != 0)
			{
				if (MainScreen.getPreviewWidth() != this.previewWidth
						|| MainScreen.getPreviewHeight() != this.previewHeight)
				{
					RealtimeDRO.release(this.instance);
					this.instance = 0;
				}
			}

			this.previewWidth = MainScreen.getPreviewWidth();
			this.previewHeight = MainScreen.getPreviewHeight();

			if (this.instance == 0)
			{
				this.instance = RealtimeDRO.initialize(this.previewWidth, this.previewHeight);
				Log.d(TAG, String.format("RealtimeDRO.initialize(%d, %d)", this.previewWidth, this.previewHeight));
				this.forceUpdate = true;
			}

			if (this.instance != 0)
			{
				long t;

				t = System.currentTimeMillis();

				RealtimeDRO.render(this.instance, MainScreen.getInstance().glGetPreviewTexture(), this.transform,
						this.previewWidth, this.previewHeight, true, this.local, this.max_amplify, this.forceUpdate,
						this.uv_desat, this.dark_uv_desat, this.dark_noise_pass, this.mix_factor, this.gamma, this.max_black_level,
						this.black_level_atten, this.min_limit, this.max_limit, this.texture_out);

				t = System.currentTimeMillis() - t;

				this.forceUpdate = false;

				this.drawOutputTexture();

				DROVideoEngine.this.fps.measure();

				if (this.encoder != null && System.currentTimeMillis() > this.recordingDelayed && !this.paused)
				{
					this.encoder.encode(this.texture_out);
				}
			}
			else
			{
				throw new RuntimeException("Unable to create DRO instance.");
			}
		}
	}

	private void drawOutputTexture()
	{
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		GLES20.glViewport(0, 0, this.surfaceWidth, this.surfaceHeight);

		GLES20.glUseProgram(this.hProgram);

		final int ph = GLES20.glGetAttribLocation(this.hProgram, "vPosition");
		final int tch = GLES20.glGetAttribLocation(this.hProgram, "vTexCoord");
		final int th = GLES20.glGetUniformLocation(this.hProgram, "sTexture");

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, this.texture_out);
		GLES20.glUniform1i(th, 0);

		GLES20.glVertexAttribPointer(ph, 2, GLES20.GL_FLOAT, false, 4 * 2, VERTEX_BUFFER);
		GLES20.glVertexAttribPointer(tch, 2, GLES20.GL_FLOAT, false, 4 * 2, UV_BUFFER);
		GLES20.glEnableVertexAttribArray(ph);
		GLES20.glEnableVertexAttribArray(tch);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		GLES20.glDisableVertexAttribArray(ph);
		GLES20.glDisableVertexAttribArray(tch);
	}
}
