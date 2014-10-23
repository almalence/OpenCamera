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

package com.almalence.plugins.capture.night;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import android.content.Context;
import android.util.Log;

/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
//-+- -->

import com.almalence.util.ImageConversion;

/**
 * This class is an object representation of a Square containing the vertex
 * information and drawing functionality, which is called by the renderer.
 * 
 * @author Savas Ziplies (nea/INsanityDesign)
 */
public class GLCameraPreview
{

	/** The buffer holding the vertices */
	private FloatBuffer	vertexBuffer;

	/** The initial vertex definition */
	private float[]		vertices	= { -1.0f, -1.0f, 0.0f, // Bottom Left
			1.0f, -1.0f, 0.0f, // Bottom Right
			-1.0f, 0.5f, 0.0f, // Top Left
			1.0f, 0.5f, 0.0f		// Top Right
									};

	/** buffer holding the texture coordinates */
	private FloatBuffer	textureBuffer;

	/** Our texture pointer */
	private float[]		texture		= {
									// Mapping coordinates for the vertices
			0.0f, 0.0f, // top left (V2)
			0.0f, 1.0f, // bottom left (V1)
			1.0f, 0.0f, // top right (V4)
			1.0f, 1.0f				// bottom right (V3)
									};
	/** The texture pointer */
	private int[]		textures	= new int[1];

	private byte[]		out;

	private int			surfaceWidth;
	private int			surfaceHeight;
	private int			previewWidth;
	private int			previewHeight;
	private int			previewHalfWidth;
	private int			previewHalfHeight;

	private int			textureHeight;
	private int			textureWidth;

	private int[]		cropRect;

	public void setSurfaceSize(int width, int height)
	{
//		Log.d("GLCameraPreview", "Surface size = " + width + " x " + height);
		surfaceWidth = width;
		surfaceHeight = height;

		out = null;

		vertexBuffer.clear();

		vertices = new float[] { -height / 2.0f, -width / 2.0f, 0.0f, // Bottom
																		// Left
				height / 2.0f, -width / 2.0f, 0.0f, // Bottom Right
				-height / 2.0f, width / 2.0f, 0.0f, // Top Left
				height / 2.0f, width / 2.0f, 0.0f // Top Right
		};
		vertexBuffer.put(vertices);
		vertexBuffer.position(0);
	}

	/**
	 * The Square constructor.
	 * 
	 * Initiate the buffers.
	 */
	public GLCameraPreview(Context context)
	{

		ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		vertexBuffer = byteBuf.asFloatBuffer();
		vertexBuffer.put(vertices);
		vertexBuffer.position(0);

		byteBuf = ByteBuffer.allocateDirect(texture.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		textureBuffer = byteBuf.asFloatBuffer();
		textureBuffer.put(texture);
		textureBuffer.position(0);
	}

	public void generateGLTexture(GL10 gl)
	{
		// Generate one texture pointer...
		gl.glGenTextures(1, textures, 0);
		// ...and bind it to our array
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

		// Create Nearest Filtered Texture
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

		previewWidth = MainScreen.getPreviewWidth();
		previewHeight = MainScreen.getPreviewHeight();
		
//		Log.d("GLCameraPreview", "Preview size from MainScreen = " + previewWidth + " x " + previewHeight);
		
		textureWidth = 512;
		textureHeight = 512;

		previewHalfWidth = textureWidth;
		previewHalfHeight = textureHeight;

		cropRect = new int[] { 0, previewHalfWidth, previewHalfHeight, -previewHalfWidth };
	}

	/**
	 * Load the textures
	 * 
	 * @param gl
	 *            - The GL Context
	 * @param context
	 *            - The Activity context
	 */
	public void loadGLTexture(GL10 gl, byte[] yuv_data, Context context)
	{

		int out_len = previewHalfWidth * previewHalfHeight * 3;
		if (out == null)
			out = new byte[out_len];
		else if (out.length < out_len)
			out = new byte[out_len];

		ImageConversion.convertNV21toGL(yuv_data, out, previewWidth, previewHeight, previewHalfWidth, previewHalfHeight);

		// ...and bind it to our array
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

		gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGB, textureWidth, textureHeight, 0, GL10.GL_RGB,
				GL10.GL_UNSIGNED_BYTE, ByteBuffer.wrap(out));

		((GL11) gl).glTexParameteriv(GL10.GL_TEXTURE_2D, GL11Ext.GL_TEXTURE_CROP_RECT_OES, cropRect, 0);
		((GL11Ext) gl).glDrawTexiOES(0, 0, 0, surfaceWidth, surfaceHeight);

	}

	/**
	 * The object own drawing function. Called from the renderer to redraw this
	 * instance with possible changes in values.
	 * 
	 * @param gl
	 *            - The GL context
	 */
	public void draw(GL10 gl, byte[] yuv_data, Context context)
	{

		if (yuv_data != null && context != null && cropRect != null)
			this.loadGLTexture(gl, yuv_data, context);
		else
		{
			Log.i("NIGHT CAMERA DEBUG",
					"GLCameraPreview.draw finished. rgb_data = null || context = null || cropRect = null");
			return;
		}

		// bind the previously generated texture
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

		// Point to our buffers
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

		// Set the face rotation

		// Point to our vertex buffer
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);

		// Draw the vertices as triangle strip
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);

		// Disable the client state before leaving
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
	}

}
