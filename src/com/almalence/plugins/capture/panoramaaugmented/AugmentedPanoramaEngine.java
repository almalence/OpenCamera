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

package com.almalence.plugins.capture.panoramaaugmented;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.almalence.SwapHeap;
import com.almalence.YuvImage;

/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

import com.almalence.util.ImageConversion;
import com.almalence.util.Util;
import com.almalence.plugins.capture.panoramaaugmented.AugmentedRotationListener.AugmentedRotationReceiver;

import android.annotation.TargetApi;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.util.Log;

public class AugmentedPanoramaEngine implements Renderer, AugmentedRotationReceiver
{
	public static final String			TAG						= "Almalence";

	public static final int				STATE_STANDBY			= 0;
	public static final int				STATE_CLOSEENOUGH		= 1;
	public static final int				STATE_TAKINGPICTURE		= 2;

	private static final int			TIP_FRAMES_COUNT		= 2;

	public static final long			FRAME_WHITE_FADE_IN		= 500;
	public static final long			FRAME_WHITE_FADE_OUT	= 400;

	private static final int			MAX_TEXTURE_SIZE		= 512;

	private static final ByteBuffer		FRAME_INDICES_BUFFER;
	private static final ByteBuffer		FRAME_LINE_INDICES_BUFFER;
	private static final FloatBuffer	FRAME_TEXTURE_UV;
	private static final FloatBuffer	FRAME_NORMALS;

	private volatile float				frameIntersectionPart	= 0.50f;		// how
																				// much
																				// panorama
																				// frames
																				// should
																				// intersect

	static
	{
		ByteBuffer byteBuf;

		{
			final byte[] indices = { 0, 2, 1, 0, 3, 2, };

			FRAME_INDICES_BUFFER = ByteBuffer.allocateDirect(indices.length);
			FRAME_INDICES_BUFFER.put(indices);
			FRAME_INDICES_BUFFER.position(0);

			final byte[] indices_line = { 2, 1, 0, 3, 2, };

			FRAME_LINE_INDICES_BUFFER = ByteBuffer.allocateDirect(indices_line.length);
			FRAME_LINE_INDICES_BUFFER.put(indices_line);
			FRAME_LINE_INDICES_BUFFER.position(0);

			final float[] uv = { 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, };

			byteBuf = ByteBuffer.allocateDirect(uv.length * 4);
			byteBuf.order(ByteOrder.nativeOrder());
			FRAME_TEXTURE_UV = byteBuf.asFloatBuffer();
			FRAME_TEXTURE_UV.put(uv);
			FRAME_TEXTURE_UV.position(0);

			final float[] normals = { 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, };

			byteBuf = ByteBuffer.allocateDirect(normals.length * 4);
			byteBuf.order(ByteOrder.nativeOrder());
			FRAME_NORMALS = byteBuf.asFloatBuffer();
			FRAME_NORMALS.put(normals);
			FRAME_NORMALS.position(0);
		}
	}

	private static int pow2roundup(int x)
	{
		if (x < 0)
		{
			return 0;
		}

		--x;
		x |= x >> 1;
		x |= x >> 2;
		x |= x >> 4;
		x |= x >> 8;
		x |= x >> 16;

		return (x + 1);
	}

	private static Vector3d rotateVectorAroundAxis(float angle, Vector3d axis, Vector3d vector)
	{
		final float u2 = axis.x * axis.x;
		final float v2 = axis.y * axis.y;
		final float w2 = axis.z * axis.z;
		final float L = u2 + v2 + w2;

		final float[][] rotationMatrix = new float[3][3];
		rotationMatrix[0][0] = (float) (u2 + (v2 + w2) * Math.cos(angle)) / L;
		rotationMatrix[0][1] = (float) (axis.x * axis.y * (1 - Math.cos(angle)) - axis.z * Math.sqrt(L)
				* Math.sin(angle))
				/ L;
		rotationMatrix[0][2] = (float) (axis.x * axis.z * (1 - Math.cos(angle)) + axis.y * Math.sqrt(L)
				* Math.sin(angle))
				/ L;

		rotationMatrix[1][0] = (float) (axis.x * axis.y * (1 - Math.cos(angle)) + axis.z * Math.sqrt(L)
				* Math.sin(angle))
				/ L;
		rotationMatrix[1][1] = (float) (v2 + (u2 + w2) * Math.cos(angle)) / L;
		rotationMatrix[1][2] = (float) (axis.y * axis.z * (1 - Math.cos(angle)) - axis.x * Math.sqrt(L)
				* Math.sin(angle))
				/ L;

		rotationMatrix[2][0] = (float) (axis.x * axis.z * (1 - Math.cos(angle)) - axis.y * Math.sqrt(L)
				* Math.sin(angle))
				/ L;
		rotationMatrix[2][1] = (float) (axis.y * axis.z * (1 - Math.cos(angle)) + axis.x * Math.sqrt(L)
				* Math.sin(angle))
				/ L;
		rotationMatrix[2][2] = (float) (w2 + (u2 + v2) * Math.cos(angle)) / L;

		Vector3d out = new Vector3d();
		out.x = vector.x * rotationMatrix[0][0] + vector.y * rotationMatrix[1][0] + vector.z * rotationMatrix[2][0];
		out.y = vector.x * rotationMatrix[0][1] + vector.y * rotationMatrix[1][1] + vector.z * rotationMatrix[2][1];
		out.z = vector.x * rotationMatrix[0][2] + vector.y * rotationMatrix[1][2] + vector.z * rotationMatrix[2][2];

		return out;
	}

	private boolean									activated				= false;
	private final Object activatedSync = new Object();

	private final LinkedList<AugmentedFrameTaken>	frames					= new LinkedList<AugmentedFrameTaken>();

	private final Vector3d							initialTopVector		= new Vector3d(0.0f, 1.0f, 1.0f);
	private final Vector3d							initialDirectionVector	= new Vector3d(0.0f, 0.0f, -1.0f);
	// initialTransform is used outside to convert world coordinates into device
	// coordinates
	// when unrolling panorama frames
	public final float[]							initialTransform		= new float[16];

	private final AtomicBoolean						capturing				= new AtomicBoolean(false);

	// access to both currentVector and topVector are synchronized through
	// currentVector
	private final Vector3d							currentVector			= new Vector3d();
	private final Vector3d							topVector				= new Vector3d(0.0f, 0.0f, 1.0f);
	private final Vector3d							sideVector				= new Vector3d(0.0f, 1.0f, 0.0f);

	private final float[]							transform				= new float[16];

	private FloatBuffer								vertexBuffer;

	private int										width;
	private int										height;
	private int										halfWidth;
	private int										halfHeight;

	private int										textureWidth;
	private int										textureHeight;

	private int										state					= STATE_STANDBY;
	private final Object							stateSynch				= new Object();

	private int										currentlyTargetedTarget	= 0;

	private float									verticalViewAngle		= 45.0f;
	private float									radius, radiusGL, radiusEdge;

	private float									angleShift				= 0.0f;
	private volatile float							angleTotal;

	private final AugmentedFrameTarget[]			targetFrames			= new AugmentedFrameTarget[TIP_FRAMES_COUNT];

	private long									capture_time;

	private int										iSurfaceWidth			= 0;
	private int										iSurfaceHeight			= 0;
	private volatile boolean						bViewportCreated		= false;
	private volatile boolean						bCreateViewportNow		= false;
	private volatile int							framesMax;
	
	private volatile float distanceLimit = 0.1f;
	private volatile boolean miniDisplayMode = false;
	

	public void setFrameIntersection(final float intersection)
	{
		this.frameIntersectionPart = intersection;
	}
	
	public void setDistanceLimit(final float value)
	{
		this.distanceLimit = value;
	}
	
	public void setMiniDisplayMode(final boolean value)
	{
		this.miniDisplayMode = value;
	}

	public void reset(final int width, final int height, float verticalViewAngleR)
	{
		Log.i(TAG, String.format("AugmentedPanoramaEngine.reset(%d, %d, %f)",
				width, height, verticalViewAngleR));
		
		synchronized (this.activatedSync)
		{
			this.width = width;
			this.height = height;

			this.halfWidth = this.width / 2;
			this.halfHeight = this.height / 2;

			// in case view angle is unknown - use some default value
			if (verticalViewAngleR == 0)
				verticalViewAngleR = 45;

			this.verticalViewAngle = verticalViewAngleR;

			// using halfWidth here since we are operating on portrait-oriented
			// frames
			// and halfWidth correspond to a camera verticalViewAngle
			// this is the radius to the center of a frame
			this.radius = (float) (this.halfWidth / Math.tan(Math.toRadians(verticalViewAngle / 2.0f)));

			// but for GL we need radius to be calculated like that to properly fit
			// the frame on screen
			this.radiusGL = (float) (this.halfHeight / Math.tan(Math.toRadians(verticalViewAngle / 2.0f)));

			final float HalfTileShiftAngle = (float) Math.atan2((0.5f - this.frameIntersectionPart / 2) * this.width,
					this.radius);

			// this is the radius to the intersection of neighbor frames
			// somehow setting it to the same value as radius to the center gives a
			// more precise results
			this.radiusEdge = this.radius;

			final int circle_frames = (int) Math.ceil(2.0f * Math.PI / (2.0f * HalfTileShiftAngle));
			this.angleShift = (float) (2.0f * Math.PI / circle_frames);

			this.angleTotal = 0.0f;

			final float[] vertices = { -this.width / 2.0f, this.height / 2.0f, 0.0f, this.width / 2.0f, this.height / 2.0f,
					0.0f, this.width / 2.0f, -this.height / 2.0f, 0.0f, -this.width / 2.0f, -this.height / 2.0f, 0.0f, };

			final ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length * 4);
			byteBuf.order(ByteOrder.nativeOrder());
			this.vertexBuffer = byteBuf.asFloatBuffer();
			this.vertexBuffer.put(vertices);
			this.vertexBuffer.position(0);

			this.optimizeTextureDimensions();

			this.activated = true;
		}
		
		this.bCreateViewportNow = true;
	}

	public void setMaxFrames(final int count)
	{
		this.framesMax = count;
//		Log.d("Almalence", "Maximum frames count: " + count);
	}

	public float getRadiusToEdge()
	{
		return this.radiusEdge;
	}

	private void optimizeTextureDimensions()
	{
		this.textureHeight = (int) (0.5f * this.height);

		if (this.textureHeight >= MAX_TEXTURE_SIZE)
		{
			this.textureHeight = MAX_TEXTURE_SIZE;
		}
		else
		{
			this.textureHeight = AugmentedPanoramaEngine.pow2roundup(this.textureHeight);
		}

		this.textureWidth = AugmentedPanoramaEngine.pow2roundup(
				(int)(this.width * ((float) this.textureHeight / this.height)));
	}

	@Override
	public void onRotationChanged(final float[] transform)
	{
		synchronized (this.transform)
		{
			if (transform.length == 16)
			{
				System.arraycopy(transform, 0, this.transform, 0, 16);
			}
			else if (transform.length == 9)
			{
				System.arraycopy(transform, 0, this.transform, 0, 3);
				System.arraycopy(transform, 3, this.transform, 4, 3);
				System.arraycopy(transform, 6, this.transform, 8, 3);

				this.transform[3] = 0;
				this.transform[7] = 0;
				this.transform[11] = 0;

				this.transform[12] = 0;
				this.transform[13] = 0;
				this.transform[14] = 0;

				this.transform[15] = 1;
			}
			else
			{
				throw new RuntimeException();
			}

			synchronized (this.topVector)
			{
				this.topVector.x = this.transform[1];
				this.topVector.y = this.transform[5];
				this.topVector.z = this.transform[9];
				this.topVector.normalize();
			}

			synchronized (this.sideVector)
			{
				this.sideVector.x = this.transform[0];
				this.sideVector.y = this.transform[4];
				this.sideVector.z = this.transform[8];
				this.sideVector.normalize();
			}

			synchronized (this.currentVector)
			{
				this.currentVector.x = -this.transform[2];
				this.currentVector.y = -this.transform[6];
				this.currentVector.z = -this.transform[10];
				this.currentVector.normalize();
				this.currentVector.multiply(this.radiusGL);

				int target = -1;

				synchronized (this.targetFrames)
				{
					for (int i = 0; i < this.targetFrames.length; i++)
					{
						AugmentedFrameTarget frame = this.targetFrames[i];

						if (frame != null)
						{
							if (frame.distance() < this.distanceLimit)
							{
								target = i;
							}
						}
					}
				}

				synchronized (this.stateSynch)
				{
					if (this.state != STATE_TAKINGPICTURE)
					{
						if (target == -1)
						{
							this.state = STATE_STANDBY;
						}
						else
						{
							this.state = STATE_CLOSEENOUGH;
							this.currentlyTargetedTarget = target;
						}
					}
				}
			}
		}
	}

	private boolean	pictureTakingInDelay	= false;
	private long	delay_time;

	public int getPictureTakingState(final boolean autoFocus)
	{
		synchronized (this.frames)
		{
			if (this.isCircular() || this.frames.size() == 0)
			{
				return STATE_STANDBY;
			}
		}

		synchronized (this.stateSynch)
		{
			// a 200ms delay in switching from STATE_CLOSEENOUGH to
			// STATE_TAKINGPICTURE
			// if picture will be taken right away, to allow user to stabilize
			// camera
			if (!autoFocus)
			{
				if (this.state == STATE_CLOSEENOUGH)
				{
					if (this.pictureTakingInDelay)
					{
						if (System.currentTimeMillis() > this.delay_time + 200)
							this.pictureTakingInDelay = false;
						else
							return this.state;
					}
					else
					{
						this.delay_time = System.currentTimeMillis();
						this.pictureTakingInDelay = true;
						return this.state;
					}
				}
			}

			if (this.state == STATE_CLOSEENOUGH)
			{
				this.state = STATE_TAKINGPICTURE;
				this.capture_time = System.currentTimeMillis();

				return STATE_TAKINGPICTURE;
			}
			else
			{
				return this.state;
			}
		}
	}

	public boolean isCircular()
	{
		return (this.angleTotal >= 2.0d * Math.PI);
	}

	public boolean isMax()
	{
		synchronized (this.frames)
		{
			return (this.frames.size() >= this.framesMax);
		}
	}

	public void onCameraError()
	{
		synchronized (this.stateSynch)
		{
			this.state = STATE_STANDBY;
		}
	}

	@SuppressWarnings("unchecked")
	public LinkedList<AugmentedFrameTaken> retrieveFrames()
	{
		final LinkedList<AugmentedFrameTaken> frames;

		final Object syncObject = new Object();
		synchronized (syncObject)
		{
			synchronized (this.stateSynch)
			{
				while (this.state == STATE_TAKINGPICTURE)
				{
					try
					{
						this.stateSynch.wait();
					}
					catch (final InterruptedException e)
					{
						e.printStackTrace();
						Thread.currentThread().interrupt();
					}
				}

				this.state = STATE_STANDBY;
			}

			synchronized (this.frames)
			{
				frames = (LinkedList<AugmentedFrameTaken>) this.frames.clone();
			}

			MainScreen.getInstance().queueGLEvent(new Runnable()
			{
				@Override
				public void run()
				{
					synchronized (AugmentedPanoramaEngine.this.frames)
					{
						synchronized (syncObject)
						{
							syncObject.notify();
						}

						final Iterator<AugmentedFrameTaken> iterator = AugmentedPanoramaEngine.this.frames.iterator();

						while (iterator.hasNext())
						{
							iterator.next().destroy();
							iterator.remove();
						}
					}

					synchronized (AugmentedPanoramaEngine.this.targetFrames)
					{
						for (final AugmentedFrameTarget frame : AugmentedPanoramaEngine.this.targetFrames)
						{
							frame.angle = 0.0f;
						}
					}

					AugmentedPanoramaEngine.this.capturing.set(false);
				}
			});

			Collections.sort(frames, new Comparator<AugmentedFrameTaken>()
			{
				@Override
				public int compare(final AugmentedFrameTaken frame1, final AugmentedFrameTaken frame2)
				{
					return (int)frame1.angleShift - (int)frame2.angleShift;
				}
			});

			try
			{
				syncObject.wait();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();

				Thread.currentThread().interrupt();
			}
		}

		this.angleTotal = 0.0f;

		return frames;
	}

	public int cancelFrame()
	{
		final AugmentedFrameTaken frame;

		final int frames_count;

		synchronized (this.frames)
		{
			frames_count = this.frames.size();

			if (frames_count > 0)
			{
				frame = this.frames.removeLast();
			}
			else
			{
				this.state = STATE_STANDBY;
				frame = null;
			}
		}

		if (frame != null)
		{
			this.targetFrames[frame.angleShift < 0 ? 0 : 1].moveBack();

			new Thread()
			{
				@Override
				public void run()
				{
					MainScreen.getInstance().queueGLEvent(new Runnable()
					{
						@Override
						public void run()
						{
							frame.destroy();
						}
					});

					SwapHeap.FreeFromHeap(frame.getNV21address());
				}
			}.start();
		}

		return (frames_count - 1);
	}

	public float getHorizonErrorAngle()
	{
		final Vector3d top;
		final Vector3d dir;

		synchronized (this.topVector)
		{
			top = new Vector3d(this.topVector);
		}
		synchronized (this.currentVector)
		{
			dir = new Vector3d(this.currentVector);
		}

		final float t = (dir.x * top.y - dir.y * top.x) / (dir.x * dir.x + dir.y * dir.y);

		final Vector3d topProjection = new Vector3d(top.x + dir.y * t, top.y - dir.x * t, top.z);

		final float plane_top_x_signum = Math.signum(topProjection.x) == Math.signum(dir.x) ? 1.0f : -1.0f;
		final float plane_top_x = plane_top_x_signum
				* (float) Math.sqrt(topProjection.x * topProjection.x + topProjection.y * topProjection.y);
		final float plane_top_y = topProjection.z;

		float angle;
		if (Math.abs(plane_top_x) > 0.05f && Math.abs(plane_top_y) > 0.05f)
		{
			angle = (float) Math.asin(plane_top_x / Math.sqrt(plane_top_x * plane_top_x + plane_top_y * plane_top_y));

			if (plane_top_y < 0.0f)
			{
				angle += Math.signum(angle) * (float) Math.PI / 2.0f;
			}
		}
		else
		{
			angle = 0.0f;
		}

		return angle;
	}

	public float getHorizonSideErrorAngle()
	{
		final Vector3d top;
		final Vector3d side;

		synchronized (this.topVector)
		{
			top = new Vector3d(this.topVector);
		}
		synchronized (this.sideVector)
		{
			side = new Vector3d(this.sideVector);
		}

		final float t = (side.x * top.y - side.y * top.x) / (side.x * side.x + side.y * side.y);

		final Vector3d topProjection = new Vector3d(top.x + side.y * t, top.y - side.x * t, top.z);

		final float plane_top_x_signum = Math.signum(topProjection.z)
				* (Math.signum(topProjection.y) == Math.signum(side.y) ? 1.0f : -1.0f);
		final float plane_top_x = plane_top_x_signum
				* (float) Math.sqrt(topProjection.x * topProjection.x + topProjection.y * topProjection.y);
		final float plane_top_y = topProjection.z;

		if (Math.abs(plane_top_x) > 0.05f && Math.abs(plane_top_y) > 0.05f)
		{
			float angle = (float) Math.asin(plane_top_x
					/ Math.sqrt(plane_top_x * plane_top_x + plane_top_y * plane_top_y));

			if (plane_top_y < 0.0f)
			{
				angle = Math.signum(angle) * (float) Math.PI - angle;
			}

			return angle;
		}
		else
		{
			return Float.POSITIVE_INFINITY;
		}
	}

	private final Vector3d		last_position	= new Vector3d();
	private final Vector3d		last_topVec		= new Vector3d();
	private final float[]		last_transform	= new float[16];
	private volatile boolean	addToBeginning;

	public void recordCoordinates()
	{
		this.capturing.set(true);

		synchronized (this.transform)
		{
			if (this.frames.size() == 0)
			{
				synchronized (this.currentVector)
				{
					this.initialTopVector.set(this.topVector);
					this.initialDirectionVector.set(this.currentVector);
				}
				gluInvertMatrix(this.transform, this.initialTransform);

				this.targetFrames[0].move(-1);
				this.targetFrames[1].move(1);

				this.addToBeginning = false;
			}
			else
			{
				this.addToBeginning = (this.currentlyTargetedTarget == 0);
			}

			synchronized (this.currentVector)
			{
				this.last_position.set(this.currentVector);
				this.last_topVec.set(this.topVector);
			}

			System.arraycopy(this.transform, 0, this.last_transform, 0, 16);
		}
	}

	@TargetApi(19)
	public boolean onFrameAdded(final int image)
	{
		final boolean goodPlace;
		final int framesCount;
		synchronized (this.frames)
		{
			framesCount = this.frames.size();
		}

		final AugmentedFrameTarget targetFrame = this.targetFrames[this.addToBeginning ? 0 : 1];

		synchronized (this.stateSynch)
		{
			goodPlace = (framesCount == 0)
					|| (this.state == STATE_TAKINGPICTURE && targetFrame.distance() < this.distanceLimit);
		}

		if (goodPlace)
		{
			this.angleTotal += this.angleShift;

			final Vector3d position = new Vector3d(this.last_position);
			final Vector3d topVec = new Vector3d(this.last_topVec);
			final float[] transform = new float[16];
			System.arraycopy(this.last_transform, 0, transform, 0, 16);
			
			final AugmentedFrameTaken frame = new AugmentedFrameTaken(
					targetFrame.angle, position, topVec, transform, image, framesCount > 0);
			
			synchronized (AugmentedPanoramaEngine.this.frames)
			{
				AugmentedPanoramaEngine.this.frames.add(frame);
			}

			if (framesCount > 0)
			{
				this.targetFrames[this.currentlyTargetedTarget].move();
			}
		}

		synchronized (AugmentedPanoramaEngine.this.stateSynch)
		{
			AugmentedPanoramaEngine.this.state = STATE_STANDBY;
			AugmentedPanoramaEngine.this.stateSynch.notify();
		}

		return goodPlace;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		gl.glLineWidth(4.0f);

		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GLES10.GL_ONE);

		for (int i = 0; i < TIP_FRAMES_COUNT; i++)
		{
			this.targetFrames[i] = new AugmentedFrameTarget();
		}
	}

	@Override
	public void onSurfaceChanged(final GL10 gl, final int width, int height)
	{
		if (height == 0)
		{
			height = 1;
		}

		this.bViewportCreated = false;

		this.iSurfaceWidth = width;
		this.iSurfaceHeight = height;
	}
	
	private void drawAim()
	{
		GLES10.glPushMatrix();
		GLES10.glLoadIdentity();
		GLES10.glTranslatef(0.0f, 0.0f, -this.radiusGL);

		final float scale = 0.25f;
		GLES10.glScalef(scale, scale, scale);

		GLES10.glVertexPointer(3, GL10.GL_FLOAT, 0, AugmentedPanoramaEngine.this.vertexBuffer);
		GLES10.glTexCoordPointer(2, GL10.GL_FLOAT, 0, FRAME_TEXTURE_UV);
		GLES10.glNormalPointer(GL10.GL_FLOAT, 0, AugmentedPanoramaEngine.FRAME_NORMALS);

		GLES10.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		GLES10.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		GLES10.glEnableClientState(GL10.GL_NORMAL_ARRAY);

		GLES10.glFrontFace(GL10.GL_CW);

		GLES10.glDrawElements(GL10.GL_LINE_LOOP, 5, GL10.GL_UNSIGNED_BYTE, FRAME_LINE_INDICES_BUFFER);

		GLES10.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		GLES10.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		GLES10.glDisableClientState(GL10.GL_NORMAL_ARRAY);

		GLES10.glPopMatrix();
	}

	@Override
	public void onDrawFrame(final GL10 gl)
	{
		if (this.bCreateViewportNow || (!this.bViewportCreated))
		{
			GLES10.glViewport(0, 0, this.iSurfaceWidth, this.iSurfaceHeight);
			GLES10.glMatrixMode(GL10.GL_PROJECTION);
			GLES10.glLoadIdentity();

			GLU.gluPerspective(gl, this.verticalViewAngle,
					(float)this.iSurfaceWidth / (float)this.iSurfaceHeight,
					this.radiusGL / 10.0f, this.radiusGL * 10.0f);

			GLES10.glMatrixMode(GL10.GL_MODELVIEW);
			GLES10.glLoadIdentity();

			this.bCreateViewportNow = false;
			this.bViewportCreated = true;
		}

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		synchronized (this.activatedSync)
		{
			if (this.activated)
			{
				gl.glLoadIdentity();

				synchronized (this.currentVector)
				{
					GLU.gluLookAt(gl, 0.0f, 0.0f, 0.0f,
							this.currentVector.x, this.currentVector.y, this.currentVector.z,
							this.topVector.x, this.topVector.y, this.topVector.z);
				}

				synchronized (this.frames)
				{
					for (final AugmentedFrameTaken frame : this.frames)
					{
						frame.distance();
						frame.draw(gl);
					}
				}

				if (this.capturing.get() && !this.isCircular())
				{
					float mind = 0.0f;
					int mint = 0;
					if (this.targetFrames != null)
					{
						mind = this.targetFrames[0].distance();
						mint = this.targetFrames[0].texture;
					}
					
					for (final AugmentedFrameTarget frame : this.targetFrames)
					{
						if (frame.distance() <= mind)
						{
							mind = frame.distance;
							mint = frame.texture;
						}
						frame.draw(gl);
					}
					
					if (this.miniDisplayMode)
					{
						GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, mint);
						this.drawAim();
					}
				}
			}
		}
	}

	public static final boolean gluInvertMatrix(final float[] m, float[] invOut)
	{
		float[] inv = new float[16];
		float det;
		int i;

		inv[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15] + m[9] * m[7] * m[14] + m[13] * m[6]
				* m[11] - m[13] * m[7] * m[10];

		inv[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15] - m[8] * m[7] * m[14] - m[12]
				* m[6] * m[11] + m[12] * m[7] * m[10];

		inv[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15] + m[8] * m[7] * m[13] + m[12] * m[5]
				* m[11] - m[12] * m[7] * m[9];

		inv[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14] - m[8] * m[6] * m[13] - m[12]
				* m[5] * m[10] + m[12] * m[6] * m[9];

		inv[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15] - m[9] * m[3] * m[14] - m[13]
				* m[2] * m[11] + m[13] * m[3] * m[10];

		inv[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15] + m[8] * m[3] * m[14] + m[12] * m[2]
				* m[11] - m[12] * m[3] * m[10];

		inv[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15] - m[8] * m[3] * m[13] - m[12] * m[1]
				* m[11] + m[12] * m[3] * m[9];

		inv[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14] + m[8] * m[2] * m[13] + m[12] * m[1]
				* m[10] - m[12] * m[2] * m[9];

		inv[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15] + m[5] * m[3] * m[14] + m[13] * m[2]
				* m[7] - m[13] * m[3] * m[6];

		inv[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15] - m[4] * m[3] * m[14] - m[12] * m[2]
				* m[7] + m[12] * m[3] * m[6];

		inv[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15] + m[4] * m[3] * m[13] + m[12] * m[1]
				* m[7] - m[12] * m[3] * m[5];

		inv[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14] - m[4] * m[2] * m[13] - m[12] * m[1]
				* m[6] + m[12] * m[2] * m[5];

		inv[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11] - m[5] * m[3] * m[10] - m[9] * m[2]
				* m[7] + m[9] * m[3] * m[6];

		inv[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11] + m[4] * m[3] * m[10] + m[8] * m[2]
				* m[7] - m[8] * m[3] * m[6];

		inv[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11] - m[4] * m[3] * m[9] - m[8] * m[1]
				* m[7] + m[8] * m[3] * m[5];

		inv[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10] + m[4] * m[2] * m[9] + m[8] * m[1]
				* m[6] - m[8] * m[2] * m[5];

		det = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12];

		if (det == 0)
			return false;

		det = 1.0f / det;

		for (i = 0; i < 16; i++)
			invOut[i] = inv[i] * det;

		return true;
	}

	private static final int	TARGET_FRAME_TEXTURE_SIDE	= 1;
	private static final int	TARGET_FRAME_TEXTURE_PIXELS	= TARGET_FRAME_TEXTURE_SIDE * TARGET_FRAME_TEXTURE_SIDE;

	private class AugmentedFrameTarget
	{
		private final float[]		transform		= new float[16];
		private final Vector3d		position		= new Vector3d(1.0f, 1.0f, 1.0f);
		private volatile float		angle			= 0.0f;

		private final byte[]		colors			= new byte[] { 0, 0, -128 };
		private final byte[]		colors_taking	= new byte[3];

		private final ByteBuffer	colorBuffer;

		private float				distance		= 0.0f;

		private int					texture			= 0;

		public AugmentedFrameTarget()
		{
			this.colorBuffer = ByteBuffer.allocateDirect(this.colors.length * TARGET_FRAME_TEXTURE_PIXELS);
			this.colorBuffer.order(ByteOrder.nativeOrder());

			GLES10.glGetError();

			final int[] tex_array = new int[1];
			GLES10.glGenTextures(1, tex_array, 0);

			if (GLES10.glGetError() == GL10.GL_NO_ERROR)
			{
				this.texture = tex_array[0];

				GLES10.glBindTexture(GL10.GL_TEXTURE_2D, this.texture);

				GLES10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
				GLES10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);

				GLES10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
				GLES10.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

				GLES10.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGB, TARGET_FRAME_TEXTURE_SIDE,
						TARGET_FRAME_TEXTURE_SIDE, 0, GL10.GL_RGB, GL10.GL_UNSIGNED_BYTE, null);

				if (GLES10.glGetError() != GL10.GL_NO_ERROR)
				{
					GLES10.glDeleteTextures(1, new int[] { this.texture }, 0);
					this.texture = 0;
				}
			}
		}

		public void draw(GL10 gl)
		{
			float part = 0;

			if (AugmentedPanoramaEngine.this.state == STATE_TAKINGPICTURE)
			{
				synchronized (this.colorBuffer)
				{
					this.colorBuffer.clear();

					part = Math.min((System.currentTimeMillis() - AugmentedPanoramaEngine.this.capture_time)
							/ (float) FRAME_WHITE_FADE_IN, 1.0f);

					for (int i = 0; i < this.colors.length; i++)
					{
						this.colors_taking[i] = (byte) (0xFF & (int) (part * (255 - ((int) this.colors[i] & 0xFF)) + ((int) this.colors[i] & 0xFF)));
					}
					for (int i = 0; i < TARGET_FRAME_TEXTURE_PIXELS; i++)
					{
						this.colorBuffer.put(this.colors_taking);
					}
					this.colorBuffer.position(0);
				}
			}

			gl.glPushMatrix();

			gl.glTranslatef(this.position.x, this.position.y, this.position.z);

			gl.glRotatef(-this.angle,
					AugmentedPanoramaEngine.this.initialTopVector.x,
					AugmentedPanoramaEngine.this.initialTopVector.y,
					AugmentedPanoramaEngine.this.initialTopVector.z);

			gl.glMultMatrixf(this.transform, 0);

			final float scale;
			if (AugmentedPanoramaEngine.this.miniDisplayMode)
			{
				scale = 0.25f;
			}
			else
			{
				// make appearance a bit more stable - do not change scale if
				// distance is small
				scale = Math.max(0.5f, 1.0f - Math.max(0.1f, this.distance - 0.1f)) + part * 0.1f;
			}
			gl.glScalef(scale, scale, scale);

			gl.glBindTexture(GL10.GL_TEXTURE_2D, this.texture);

			gl.glTexSubImage2D(GL10.GL_TEXTURE_2D, 0, 0, 0, TARGET_FRAME_TEXTURE_SIDE, TARGET_FRAME_TEXTURE_SIDE,
					GL10.GL_RGB, GL10.GL_UNSIGNED_BYTE, this.colorBuffer);

			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, AugmentedPanoramaEngine.this.vertexBuffer);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, FRAME_TEXTURE_UV);
			gl.glNormalPointer(GL10.GL_FLOAT, 0, AugmentedPanoramaEngine.FRAME_NORMALS);

			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);

			gl.glFrontFace(GL10.GL_CW);

			gl.glEnable(GLES10.GL_BLEND);
			if (part == 1.0f)
				gl.glColor4f(0, 0, 0, 0.5f);
			else
				gl.glColor4f(0.75f + part / 4, 0.75f + part / 4, 0.75f + part / 4, 0.75f + part / 4);

			gl.glDrawElements(GL10.GL_TRIANGLES, 6, GL10.GL_UNSIGNED_BYTE, AugmentedPanoramaEngine.FRAME_INDICES_BUFFER);

			gl.glDisable(GLES10.GL_BLEND);
			gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);

			gl.glPopMatrix();
		}

		public float distance()
		{
			final float dpos;
			final float drot;
			synchronized (AugmentedPanoramaEngine.this.currentVector)
			{
				dpos = (float) Math.sqrt(Util
						.mathSquare(this.position.x - AugmentedPanoramaEngine.this.currentVector.x)
						+ Util.mathSquare(this.position.y - AugmentedPanoramaEngine.this.currentVector.y)
						+ Util.mathSquare(this.position.z - AugmentedPanoramaEngine.this.currentVector.z));

				drot = (float)Math.sqrt(Util.mathSquare(AugmentedPanoramaEngine.this.initialTopVector.x
						- AugmentedPanoramaEngine.this.topVector.x)
						+ Util.mathSquare(AugmentedPanoramaEngine.this.initialTopVector.y
								- AugmentedPanoramaEngine.this.topVector.y)
						+ Util.mathSquare(AugmentedPanoramaEngine.this.initialTopVector.z
								- AugmentedPanoramaEngine.this.topVector.z));
			}

			final float sizeDim = (float)Math.sqrt(AugmentedPanoramaEngine.this.width
					* AugmentedPanoramaEngine.this.height);

			this.distance = dpos / sizeDim + drot;

			float distanceScaled = Math.max(Math.min(3.0f * this.distance, 1.0f), 0.0f);

			if (AugmentedPanoramaEngine.this.state != STATE_TAKINGPICTURE)
			{
				synchronized (this.colorBuffer)
				{
					this.colorBuffer.clear();
					this.colors[0] = (byte) (0xFF & (int) (255 * (0.5f + Math.min(distanceScaled, 0.5f))));
					this.colors[1] = (byte) (0xFF & (int) (255 * (1.0f - Math.max(distanceScaled - 0.5f, 0.0f))));

					for (int i = 0; i < TARGET_FRAME_TEXTURE_PIXELS; i++)
					{
						this.colorBuffer.put(this.colors);
					}
					this.colorBuffer.position(0);
				}
			}

			return this.distance;
		}

		public void move(final int shift)
		{
			this.angle += Math.toDegrees(AugmentedPanoramaEngine.this.angleShift * shift);

			this.position.set(rotateVectorAroundAxis((float) Math.toRadians(this.angle),
							AugmentedPanoramaEngine.this.initialTopVector,
							AugmentedPanoramaEngine.this.initialDirectionVector));

			System.arraycopy(AugmentedPanoramaEngine.this.initialTransform, 0, this.transform, 0, 16);

		}

		public void move()
		{
			this.move((int)Math.signum(this.angle));
		}

		public void moveBack()
		{
			this.move(-(int)Math.signum(this.angle));
		}
	}

	public class AugmentedFrameTaken
	{
		public final float[]		transform			= new float[16];

		private volatile boolean	created				= true;
		private volatile boolean	textureAllocated	= false;
		private long				creationTime;
		private final int[]			texture				= new int[2];

		private float				distance;

		private final Vector3d		position;
		private final Vector3d		vTop;

		private final float			angleShift;

		private int					nv21address;
		private final Object		nv21addressSync		= new Object();

		private final Object		glSync				= new Object();

		private ByteBuffer			rgba_buffer;
		
		// Perfect display stuff
		private final Vector3d dposition = new Vector3d();
		private final float dangle;
		private final float[] dtransform = new float[16];
		
		

		public int getNV21address()
		{
			synchronized (this.nv21addressSync)
			{
				return this.nv21address;
			}
		}
		
		private AugmentedFrameTaken(final float angleShift, final Vector3d position,
				final Vector3d topVec, final float[] rotation, final boolean displayAsPerfect)
		{
			this.angleShift = angleShift;
			
			this.vTop = new Vector3d(topVec);

			gluInvertMatrix(rotation, this.transform);

			this.position = new Vector3d(position);

			this.transform[12] = position.x;
			this.transform[13] = position.y;
			this.transform[14] = position.z;
			
			if (displayAsPerfect)
			{
				final AugmentedFrameTarget target = targetFrames[currentlyTargetedTarget];
				
				this.dposition.set(target.position);
				this.dangle = target.angle;
				System.arraycopy(target.transform, 0, this.dtransform, 0, 16);
			}
			else
			{
				this.dposition.x = 0.0f;
				this.dposition.y = 0.0f;
				this.dposition.z = 0.0f;
				this.dangle = 0;
				System.arraycopy(this.transform, 0, this.dtransform, 0, 16);
			}
		}
		
		/**
		 * For YUV input
		 */
		public AugmentedFrameTaken(final float angleShift, final Vector3d position,
				final Vector3d topVec, final float[] rotation, final int yuv_address,
				final boolean displayAsPerfect)
		{
			this(angleShift, position, topVec, rotation, displayAsPerfect);

			final Object syncObject = new Object();
			synchronized (syncObject)
			{
				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						// warrant the proper priority for jpeg decoding
						android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);

						synchronized (AugmentedFrameTaken.this.nv21addressSync)
						{							
							synchronized (AugmentedFrameTaken.this.glSync)
							{
								synchronized (syncObject)
								{
									syncObject.notify();
								}

								AugmentedFrameTaken.this.rgba_buffer = ByteBuffer.allocate(
										AugmentedPanoramaEngine.this.textureWidth
											* AugmentedPanoramaEngine.this.textureHeight * 4);
								
								final int in_width = AugmentedPanoramaEngine.this.height;
								final int in_height = AugmentedPanoramaEngine.this.width;
								
								if (CameraController.isFrontCamera())
								{
									ImageConversion.TransformNV21N(yuv_address,
											yuv_address,
											AugmentedPanoramaEngine.this.height,
											AugmentedPanoramaEngine.this.width,
											1, 0, 0);
								}

								ImageConversion.convertNV21toGLN(yuv_address,
										AugmentedFrameTaken.this.rgba_buffer.array(),
										in_width,
										in_height,
										AugmentedPanoramaEngine.this.textureWidth,
										AugmentedPanoramaEngine.this.textureHeight);
								
								ImageConversion.addCornersRGBA8888(
										AugmentedFrameTaken.this.rgba_buffer.array(),
										AugmentedPanoramaEngine.this.textureWidth,
										AugmentedPanoramaEngine.this.textureHeight);

								MainScreen.getInstance().queueGLEvent(new Runnable()
								{
									@Override
									public void run()
									{
										AugmentedFrameTaken.this.createTexture();
									}
								});
							}
							
							AugmentedFrameTaken.this.nv21address = YuvImage.AllocateMemoryForYUV(
									AugmentedPanoramaEngine.this.height,
									AugmentedPanoramaEngine.this.width);
							ImageConversion.TransformNV21N(yuv_address,
									AugmentedFrameTaken.this.nv21address,
									AugmentedPanoramaEngine.this.height,
									AugmentedPanoramaEngine.this.width,
									CameraController.isFrontCamera() ? 1 : 0, CameraController.isFrontCamera() ? 1 : 0, 1);
							SwapHeap.FreeFromHeap(yuv_address);
						}
					}
				}).start();

				try
				{
					syncObject.wait();
				}
				catch (final InterruptedException e)
				{
					Thread.currentThread().interrupt();
				}
			}
		}

		private void createTexture()
		{
			synchronized (this.glSync)
			{
				this.glSync.notify();

				GLES10.glGetError();

				GLES10.glGenTextures(2, this.texture, 0);

				if (GLES10.glGetError() == GLES10.GL_NO_ERROR)
				{
					GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, this.texture[0]);

					GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D,
							GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
					GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D,
							GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);

					GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D,
							GLES10.GL_TEXTURE_WRAP_S, GLES10.GL_CLAMP_TO_EDGE);
					GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D,
							GLES10.GL_TEXTURE_WRAP_T, GLES10.GL_CLAMP_TO_EDGE);

					GLES10.glTexImage2D(GLES10.GL_TEXTURE_2D, 0, GLES10.GL_RGBA,
							AugmentedPanoramaEngine.this.textureWidth,
							AugmentedPanoramaEngine.this.textureHeight,
							0, GLES10.GL_RGBA, GLES10.GL_UNSIGNED_BYTE, this.rgba_buffer);
					this.rgba_buffer = null;

					final ByteBuffer colorBuffer = ByteBuffer.allocateDirect(3 * TARGET_FRAME_TEXTURE_PIXELS);
					colorBuffer.order(ByteOrder.nativeOrder());
					colorBuffer.clear();
					colorBuffer.put((byte) -128);
					colorBuffer.put((byte) -128);
					colorBuffer.put((byte) -128);
					colorBuffer.position(0);
					GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, this.texture[1]);
					GLES10.glTexImage2D(GLES10.GL_TEXTURE_2D, 0, GLES10.GL_RGB, TARGET_FRAME_TEXTURE_SIDE,
							TARGET_FRAME_TEXTURE_SIDE, 0, GLES10.GL_RGB, GLES10.GL_UNSIGNED_BYTE, colorBuffer);

					this.creationTime = System.currentTimeMillis();

					if (GLES10.glGetError() == GLES10.GL_NO_ERROR)
					{
						this.textureAllocated = true;
					}
					else
					{
						GLES10.glDeleteTextures(2, this.texture, 0);

						Log.e("Almalence", "Error creating texture");
						this.textureAllocated = false;
					}
				}
				else
				{
					Log.e("Almalence", "Error generating texture");
					this.textureAllocated = false;
				}
			}
		}
		
		public void getPosition(final Vector3d vector)
		{
			vector.set(this.position);
		}
		
		public void getTop(final Vector3d vector)
		{
			vector.set(this.vTop);
		}

		public void draw(final GL10 gl)
		{
			if (!this.created || !this.textureAllocated)
			{
				return;
			}

			gl.glPushMatrix();

			gl.glTranslatef(this.dposition.x, this.dposition.y, this.dposition.z);

			gl.glRotatef(-this.dangle,
					AugmentedPanoramaEngine.this.initialTopVector.x,
					AugmentedPanoramaEngine.this.initialTopVector.y,
					AugmentedPanoramaEngine.this.initialTopVector.z);

			gl.glMultMatrixf(this.dtransform, 0);

			final float scale;
			if (AugmentedPanoramaEngine.this.miniDisplayMode)
			{
				scale = 0.25f;
			}
			else
			{
				scale = Math.max(0.20f, 1.0f - this.distance);
			}
			gl.glScalef(scale, scale, scale);

			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, AugmentedPanoramaEngine.this.vertexBuffer);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, AugmentedPanoramaEngine.FRAME_TEXTURE_UV);
			gl.glNormalPointer(GL10.GL_FLOAT, 0, AugmentedPanoramaEngine.FRAME_NORMALS);

			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);

			gl.glBindTexture(GL10.GL_TEXTURE_2D, this.texture[0]);
			gl.glDrawElements(GL10.GL_TRIANGLES, 6, GL10.GL_UNSIGNED_BYTE, AugmentedPanoramaEngine.FRAME_INDICES_BUFFER);

			if (System.currentTimeMillis() - this.creationTime < FRAME_WHITE_FADE_OUT)
			{
				final float part = 1.0f - ((System.currentTimeMillis() - this.creationTime) / (float) FRAME_WHITE_FADE_OUT);

				gl.glEnable(GLES10.GL_BLEND);
				gl.glColor4f(part, part, part, part);

				gl.glBindTexture(GL10.GL_TEXTURE_2D, this.texture[1]);
				gl.glDrawElements(GL10.GL_TRIANGLES, 6, GL10.GL_UNSIGNED_BYTE,
						AugmentedPanoramaEngine.FRAME_INDICES_BUFFER);

				gl.glEnable(GLES10.GL_BLEND);
				gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			}

			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);

			gl.glPopMatrix();
		}

		public void distance()
		{
			final float dpos;
			synchronized (AugmentedPanoramaEngine.this.currentVector)
			{
				dpos = (float) Math.sqrt(Util
						.mathSquare(this.position.x - AugmentedPanoramaEngine.this.currentVector.x)
						+ Util.mathSquare(this.position.y - AugmentedPanoramaEngine.this.currentVector.y)
						+ Util.mathSquare(this.position.z - AugmentedPanoramaEngine.this.currentVector.z));
			}

			final float sizeDim = (float) Math.sqrt(AugmentedPanoramaEngine.this.width
					* AugmentedPanoramaEngine.this.height);

			this.distance = 1.5f * dpos / sizeDim;
		}
		

		public void destroy()
		{
			synchronized (this.nv21addressSync)
			{
				if (this.created)
				{
					this.created = false;

					synchronized (this.glSync)
					{
						if (this.textureAllocated)
						{
							GLES10.glDeleteTextures(2, this.texture, 0);
						}
					}
				}
			}
		}
	}
}
