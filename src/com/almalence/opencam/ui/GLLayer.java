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


/* <!-- +++
package com.almalence.opencam_plus.ui;
+++ --> */
// <!-- -+-
package com.almalence.opencam.ui;
//-+- -->


import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;





import com.almalence.opencam.MainScreen;
/* <!-- +++
import com.almalence.opencam_plus.PluginManager;
+++ --> */
// <!-- -+-
import com.almalence.opencam.PluginManager;
//-+- -->


import com.almalence.plugins.capture.video.EglEncoder;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Build;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

/**
 * This class uses OpenGL ES to render the camera's viewfinder image on the
 * screen. Unfortunately I don't know much about OpenGL (ES). The code is mostly
 * copied from some examples. The only interesting stuff happens in the main
 * loop (the run method) and the onPreviewFrame method.
 */
public class GLLayer extends GLSurfaceView implements SurfaceHolder.Callback, Renderer
{
	private final static int GL_TEXTURE_EXTERNAL_OES = 0x00008d65;
	
	private int texture_preview;
	
	public GLLayer(Context c)
	{
		super(c);
        this.init();       
	}
	
	public GLLayer(Context c, AttributeSet attrs)
	{
		super(c, attrs);
		this.init();
	}
	
	private void init()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
		{
			this.setEGLConfigChooser(new EGLConfigChooser()
			{
				@Override
				public EGLConfig chooseConfig(final EGL10 egl, final EGLDisplay display)
				{	
		            EGLConfig[] configs = new EGLConfig[1];
		            int[] numConfigs = new int[1];
		            egl.eglChooseConfig(display, EglEncoder.EGL_ATTRIB_LIST, configs, configs.length, numConfigs);
		            
		            return configs[0];
				}
			});
		}
		else
		{
			this.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		}
		
		this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
		
		this.setRenderer(this);
	}
	
	public int getPreviewTexture()
	{
		return this.texture_preview;
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		this.setRenderMode(RENDERMODE_CONTINUOUSLY);
	}

	/**
	 * The Surface is created/init()
	 */
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		final int[] tex = new int[1];
		GLES20.glGenTextures(1, tex, 0);
		this.texture_preview = tex[0];
		
		GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, this.texture_preview);
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
		GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
		
		if (PluginManager.getInstance().shouldPreviewToGPU())
		{	
			MainScreen.H.sendEmptyMessage(MainScreen.MSG_START_CAMERA);
		}
		
		PluginManager.getInstance().onGLSurfaceCreated(gl, config);
	}
	
	/**
	 * If the surface changes, reset the view
	 */
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		PluginManager.getInstance().onGLSurfaceChanged(gl, width, height);	
	}

	/**
	 * Here we do our drawing
	 */
	public void onDrawFrame(GL10 gl)
	{
		PluginManager.getInstance().onGLDrawFrame(gl);
	}	
}