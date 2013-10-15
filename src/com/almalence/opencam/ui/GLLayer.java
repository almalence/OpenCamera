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

package com.almalence.opencam.ui;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.almalence.opencam.PluginManager;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
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
		this.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
		
		this.setRenderer(this);
	}

	/**
	 * The Surface is created/init()
	 */
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{		
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