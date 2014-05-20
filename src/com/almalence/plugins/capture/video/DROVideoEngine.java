package com.almalence.plugins.capture.video;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.almalence.opencam.MainScreen;
import com.almalence.util.FpsMeasurer;

import android.media.MediaScannerConnection;
import android.opengl.GLES20;
import android.util.Log;

public class DROVideoEngine
{
	private static final String TAG = "Almalence";
	
	private final static int GL_TEXTURE_EXTERNAL_OES = 0x00008d65;
    
	private static final String SHADER_VERTEX =
			"attribute vec2 vPosition;\n"
			+ "attribute vec2 vTexCoord;\n"
			+ "varying vec2 texCoord;\n"
			+ "void main() {\n"
			+ "  texCoord = vTexCoord;\n"
			+ "  gl_Position = vec4 ( vPosition.x, vPosition.y, 1.0, 1.0 );\n"
			+ "}";

	private static final String SHADER_FRAGMENT =
			"#extension GL_OES_EGL_image_external:enable\n"
			+ "precision mediump float;\n"
			+ "uniform samplerExternalOES sTexture;\n"
			+ "varying vec2 texCoord;\n"
			+ "void main() {\n"
			+ "  gl_FragColor = texture2D(sTexture, texCoord);\n"
			+ "}";

	private static final FloatBuffer VERTEX_BUFFER;
	private static final FloatBuffer UV_BUFFER;
	
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
			Log.e(TAG, "Could not compile vertex shader: "	+ GLES20.glGetShaderInfoLog(vshader));
			GLES20.glDeleteShader(vshader);
			vshader = 0;
		}

		int fshader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
		GLES20.glShaderSource(fshader, fss);
		GLES20.glCompileShader(fshader);
		GLES20.glGetShaderiv(fshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0)
		{
			Log.e(TAG, "Could not compile fragment shader: " + GLES20.glGetShaderInfoLog(fshader));
			GLES20.glDeleteShader(fshader);
			fshader = 0;
		}

		final int program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, vshader);
		GLES20.glAttachShader(program, fshader);
		GLES20.glLinkProgram(program);

		return program;
	}

	/*
    public static int roundOrientation(int orientation, final int orientationHistory) 
    {    	
        final int ORIENTATION_HYSTERESIS = 5;
        
        final boolean changeOrientation;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) 
        {
            changeOrientation = true;
        } 
        else
        {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min( dist, 360 - dist );
            changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
        }
        if (changeOrientation) 
        {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        
        return orientationHistory;
    }
    */
	
	private final Object stateSync = new Object();
	private int instance = 0;
	private int previewWidth = -1;
	private int previewHeight = -1;
	
	private volatile boolean filtering = false;
	private volatile boolean local = false;
	private volatile boolean  night = false;
	private volatile boolean forceUpdate = false;
	private volatile int pullYUV = 0;
	private volatile int uv_desat = 9;
	private volatile int dark_uv_desat = 5;
	private volatile float mix_factor = 0.1f;
	private volatile float gamma = 0.5f;
	private volatile float max_black_level = 64.f;
	private volatile float black_level_atten = 0.5f;
	private volatile float max_amplify = 2.0f;
	private volatile float[] min_limit = new float[]{0.5f,0.5f,0.5f};
	private volatile float[] max_limit = new float[]{3.0f,2.0f,1.2f};
	
	private EglEncoder encoder = null;
	
	private FpsMeasurer fps = new FpsMeasurer(10);
	
	private volatile int orientation = 0;
	
	public DROVideoEngine()
	{
		this.init();
	}
	
	private void init()
	{
		
	}
	
	public void setFilteringEnabled(final boolean enabled)
	{
		this.filtering = enabled;
	}
	
	public void setLocalEnabled(final boolean enabled)
	{
		this.local = enabled;
	}
	
	public void setNightEnabled(final boolean enabled)
	{
		this.night = enabled;
	}
	
	public void setPullYUV(final int value)
	{
		this.pullYUV = value;
	}
	
	public void forceUpdateTM()
	{
		this.forceUpdate = true;
	}
	
	public void startRecording(final String path)
	{
		final Object sync = new Object();
		synchronized (this.stateSync)
		{
			synchronized (sync)
			{
				if (this.encoder == null && this.instance != 0)
				{
					MainScreen.thiz.queueGLEvent(new Runnable()
					{
						@Override
						public void run()
						{							
							DROVideoEngine.this.encoder = new EglEncoder(
									path,
									DROVideoEngine.this.previewWidth,
									DROVideoEngine.this.previewHeight,
									(int)DROVideoEngine.this.fps.getFPS(),
									20000000,
									DROVideoEngine.this.orientation);
							
							synchronized (sync)
							{
								sync.notify();
							}
						}
					});
					
					try
					{
						sync.wait();
					}
					catch (final InterruptedException e)
					{
						Thread.currentThread().interrupt();
					}
				}
				else
				{
					throw new IllegalStateException("Already recording.");
				}
			}
		}
	}
	
	public void stopRecording()
	{
		final Object sync = new Object();
		synchronized (this.stateSync)
		{
			synchronized (sync)
			{
				if (this.encoder != null)
				{
					MainScreen.thiz.queueGLEvent(new Runnable()
					{
						@Override
						public void run()
						{
							final String path = DROVideoEngine.this.encoder.getPath();
							DROVideoEngine.this.encoder.close();
							DROVideoEngine.this.encoder = null;
							
				            MediaScannerConnection.scanFile(
				            		MainScreen.thiz, 
				            		new String[] { path }, 
				            		null, 
				            		null);
							
							synchronized (sync)
							{
								sync.notify();
							}
						}
					});
					
					try
					{
						sync.wait();
					}
					catch (final InterruptedException e)
					{
						Thread.currentThread().interrupt();
					}
				}
				else
				{
					throw new IllegalStateException("Not recording.");
				}
			}
		}
	}
	
	private void stopPreview()
	{
		Log.i(TAG, "DROVideoEngine.stopPreview()");
		
		synchronized (this.stateSync)
		{
			this.stopRecording();
		
			if (this.instance != 0)
			{					
				Log.i(TAG, "DRO instance is not null. Destroying.");
				
				final Object sync = new Object();
				synchronized (sync)
				{
					MainScreen.thiz.queueGLEvent(new Runnable()
					{
						@Override
						public void run()
						{
							RealtimeDRO.release(DROVideoEngine.this.instance);
							DROVideoEngine.this.instance = 0;
							
							synchronized (sync)
							{
								sync.notify();
							}
						}
					});
					
					try
					{
						sync.wait();
					}
					catch (final InterruptedException e)
					{
						Thread.currentThread().interrupt();
					}
				}
			}
		}
	}
	
	public void onResume()
	{
		
	}
	
	public void onPause()
	{		
		this.stopPreview();
	}

	public void onPreviewTextureUpdated(final int texture, final float[] transform)
	{		
		synchronized (this.stateSync)
		{
			if (this.instance != 0)
			{
				if (MainScreen.previewWidth != this.previewWidth
						|| MainScreen.previewHeight != this.previewHeight)
				{
					RealtimeDRO.release(this.instance);
					this.instance = 0;
				}
			}
			
			this.previewWidth = MainScreen.previewWidth;
			this.previewHeight = MainScreen.previewHeight;
			
			if (this.instance == 0)
			{
				this.instance = RealtimeDRO.initialize(
						this.previewWidth,
						this.previewHeight);
			}
			
			if (this.instance != 0)
			{
				RealtimeDRO.render(
						this.instance,
						texture,
						transform,
						this.previewWidth,
						this.previewHeight,
						this.filtering,
						this.local,
						this.max_amplify,
						this.forceUpdate,
						this.uv_desat,
						this.dark_uv_desat,
						this.mix_factor,
						this.gamma,
						this.max_black_level,
						this.black_level_atten,
						this.min_limit,
						this.max_limit,
						this.texture_out);
				
				this.forceUpdate = false;
				
				if (this.encoder != null)
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
	
	
	
	private int hProgram;
		
	private int surfaceWidth;
	private int surfaceHeight;

	private int texture_out;
	
	private void initGL()
	{
		final int[] tex = new int[1];
		GLES20.glGenTextures(1, tex, 0);
		this.texture_out = tex[0];
		
		GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, this.texture_out);
		GLES20.glTexParameteri(
				GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_WRAP_S,
				GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(
				GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_WRAP_T,
				GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(
				GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_MIN_FILTER,
				GLES20.GL_LINEAR);
		GLES20.glTexParameteri(
				GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_MAG_FILTER,
				GLES20.GL_LINEAR);
		
		this.hProgram = loadShader(SHADER_VERTEX, SHADER_FRAGMENT);
	}
	
	public void onSurfaceCreated(final GL10 gl, final EGLConfig config)
	{
		Log.i("Almalence", "Renderer.onSurfaceCreated()");
		
		this.initGL();
		
		DROVideoEngine.this.fps.flush();
	}

	public void onSurfaceChanged(final GL10 gl, final int width, final int height)
	{						
		Log.i(TAG, String.format("Renderer.onSurfaceChanged(%dx%d)", width, height));
		
		this.surfaceWidth = width;
		this.surfaceHeight = height;
	}

	public void onDrawFrame(final GL10 gl)
	{
		DROVideoEngine.this.fps.measure();
				
		this.drawOutputTexture();
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
		GLES20.glVertexAttribPointer(tch, 2, GLES20.GL_FLOAT, false, 4 * 2,	UV_BUFFER);
		GLES20.glEnableVertexAttribArray(ph);
		GLES20.glEnableVertexAttribArray(tch);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		//GLES20.glFlush();
		//GLES20.glFinish();

		GLES20.glDisableVertexAttribArray(ph);
		GLES20.glDisableVertexAttribArray(tch);
	}
}
		
		
