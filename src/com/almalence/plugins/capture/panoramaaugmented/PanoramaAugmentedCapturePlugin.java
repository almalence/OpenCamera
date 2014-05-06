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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.almalence.YuvImage;
/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginCapture;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.R;
import com.almalence.opencam_plus.ui.GUI;
import com.almalence.opencam_plus.ui.GUI.CameraParameter;
+++ --> */
// <!-- -+-
import com.almalence.opencamhalv3.CameraController;
import com.almalence.opencamhalv3.CameraParameters;
import com.almalence.opencamhalv3.MainScreen;
import com.almalence.opencamhalv3.PluginCapture;
import com.almalence.opencamhalv3.PluginManager;
import com.almalence.opencamhalv3.R;
import com.almalence.opencamhalv3.ui.GUI;
import com.almalence.opencamhalv3.ui.GUI.CameraParameter;
//-+- -->

import com.almalence.util.Util;

import com.almalence.plugins.capture.panoramaaugmented.AugmentedPanoramaEngine.AugmentedFrameTaken;

public class PanoramaAugmentedCapturePlugin extends PluginCapture //implements AutoFocusCallback
{
	private static final String TAG = "PanoramaAugmentedCapturePlugin";
	
	private static final String PREFERENCES_KEY_RESOLUTION = "pref_plugin_capture_panoramaaugmented_imageheight2_";
	private static final String PREFERENCES_KEY_USE_DEVICE_GYRO = "pref_plugin_capture_panoramaaugmented_usehardwaregyro";
	
	
	private AugmentedPanoramaEngine engine;


	public final int MIN_HEIGHT_SUPPORTED = 640;
	public final List<Point> ResolutionsPictureSizesList = new ArrayList<Point>();
	public final List<String> ResolutionsPictureIdxesList = new ArrayList<String>();
	public final List<String> ResolutionsPictureNamesList = new ArrayList<String>();
	
	private int prefResolution;
	private boolean prefHardwareGyroscope;
	
	private boolean prefMemoryRelax = false;
	
	//private String preferenceFocusMode;
	
	private float viewAngleX = 55.4f;
	private float viewAngleY = 42.7f;

	private SensorManager sensorManager;
	private Sensor sensorGravity;
	private Sensor sensorAccelerometer;
	//private Sensor sensorMagnetometer;
	private Sensor sensorGyroscope;
	private VfGyroSensor sensorSoftGyroscope = null;
	private boolean remapOrientation;

	private volatile boolean capturing;
	private boolean takingAlready = false;
	private boolean aboutToTakePicture=false;
	
	private AugmentedRotationListener rotationListener;
	
	private int pictureWidth;
	private int pictureHeight;
	private int previewWidth;
	private int previewHeight;
	
	private volatile boolean isFirstFrame = true;
	
	private volatile boolean coordsRecorded;
	private volatile boolean previewRestartFlag;

	private boolean showGyroWarnOnce = false;
	
	private int aewblock = 1;
	private boolean aewbLockedByPanorama = false;
	
	public PanoramaAugmentedCapturePlugin()
	{
		super("com.almalence.plugins.panoramacapture_augmented",
				R.xml.preferences_capture_panoramaaugmented,
				0,
				0,
				null);
		
		this.capturing = false;
		
		this.sensorManager = (SensorManager)MainScreen.mainContext.getSystemService(Context.SENSOR_SERVICE);
		this.sensorGravity = this.sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		this.sensorAccelerometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		//this.sensorMagnetometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		this.sensorGyroscope = this.sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
	}
	
	private void init()
	{	
		this.scanResolutions();	
		
		this.getPrefs();
		
		this.checkCoordinatesRemapRequired();
		
		this.rotationListener = new AugmentedRotationListener(
				this.remapOrientation,
				!this.prefHardwareGyroscope);
		
		initSensors();
	}
	
	private void deinit()
	{
		deinitSensors();
	}

	
	private void initSensors()
	{
		//List<Sensor> ls = this.sensorManager.getSensorList(Sensor.TYPE_GRAVITY);
		//for (int i=0; i<ls.size(); ++i)
		//	Log.i("CameraTest", "sensor: "+ls.get(i).getName());
		
		if (this.prefHardwareGyroscope)
		{
			this.sensorManager.registerListener(
					this.rotationListener, 
					this.sensorGyroscope, 
					SensorManager.SENSOR_DELAY_GAME);
		}
		else
		{
			if (this.sensorSoftGyroscope == null)
			{
				this.sensorSoftGyroscope = new VfGyroSensor(null);
			}
			this.sensorSoftGyroscope.open();
			this.sensorSoftGyroscope.SetListener(this.rotationListener);
			//this.sensorSoftGyroscope.SetStabilityOnly(this.prefHardwareGyroscope);
		}

		
		// on some devices accelerometer events are more frequent
		// on some devices gravity events are more frequent
		// we'll use both the same way to get updates as fast as possible
		this.sensorManager.registerListener(
				this.rotationListener,
				this.sensorAccelerometer,
				SensorManager.SENSOR_DELAY_GAME);
		
		// some devices do not like gravity sensor
		// e.g. on Ascend P2/P6 the GL layer will disappear if gravity sensor is registered
		// (and no data will arrive from it either)
		// so, use gravity sensor only on models where accelerometer sensor is no good
		if (
			Build.MODEL.contains("LG-D80")		// Accelerometer on LG G2 is very slow (D80x models) 
			)
		{
			this.sensorManager.registerListener(
					this.rotationListener,
					this.sensorGravity,
					SensorManager.SENSOR_DELAY_GAME);
		}
		
		/*
		// magnetometer - a lot of 'breathing'
		this.sensorManager.registerListener(
				this.rotationListener,
				this.sensorMagnetometer,
				SensorManager.SENSOR_DELAY_GAME);
		*/
		this.rotationListener.setReceiver(this.engine);
	}

	private void deinitSensors()
	{
		if (this.prefHardwareGyroscope)
		{
			this.sensorManager.unregisterListener(this.rotationListener, this.sensorGyroscope);
		}
		else
		{
			if (null != this.sensorSoftGyroscope)
				this.sensorSoftGyroscope.SetListener(null);
		}
		
		this.sensorManager.unregisterListener(this.rotationListener, this.sensorAccelerometer);
		this.sensorManager.unregisterListener(this.rotationListener, this.sensorGravity);
		//this.sensorManager.unregisterListener(this.rotationListener, this.sensorMagnetometer);
	}
	
	
	@Override
	public void onDefaultsSelect()
	{	
		this.scanResolutions();
		
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.thiz);
				
		final String sizeKey = PREFERENCES_KEY_RESOLUTION + CameraController.CameraIndex;
		
		if (!prefs.contains(sizeKey))
		{
			if (ResolutionsPictureIdxesList.size() > 0)
			{	
				boolean found = false;
				int idx = 0;
				
				// first - try to select resolution <= 5mpix and with at least 10 frames available 
				for (int i = 0; i < ResolutionsPictureSizesList.size(); i++)
				{
					final Point point = ResolutionsPictureSizesList.get(i);
					final int frames_fit_count = 
							(int)(getAmountOfMemoryToFitFrames() / getFrameSizeInBytes(point.x, point.y));
					
					if ((frames_fit_count >= 10) && (point.x*point.y < 5200000))
					{
						idx = i;
			    		found = true;
			    		break;
					}
				}
				
				if (!found)
				{
					int best_fit_count = 1;
					
					// try to select resolution with highest number of frames available (the lowest resolution really)
					for (int i = 0; i < ResolutionsPictureSizesList.size(); i++)
					{
						final Point point = ResolutionsPictureSizesList.get(i);
						final int frames_fit_count = 
								(int)(getAmountOfMemoryToFitFrames() / getFrameSizeInBytes(point.x, point.y));
						
						if (frames_fit_count > best_fit_count)
						{
							best_fit_count = frames_fit_count; 
							idx = i;
				    		found = true;
						}
					}
				}
				
	    		prefs.edit().putString(sizeKey, 
	    						ResolutionsPictureIdxesList.get(idx)).commit();
			}
		}
		
		if (!prefs.contains(PREFERENCES_KEY_USE_DEVICE_GYRO))
		{
			prefs.edit().putBoolean(
							PREFERENCES_KEY_USE_DEVICE_GYRO,
							this.sensorGyroscope != null).commit();
		}
	}
	
	@Override
	public void onCreate()
	{		
		this.engine = new AugmentedPanoramaEngine();
	}
	
	@Override
	public void onResume()
	{		
		MainScreen.thiz.MuteShutter(false);
		
        final Message msg = new Message();
		msg.what = PluginManager.MSG_OPENGL_LAYER_SHOW;
		MainScreen.H.sendMessage(msg);
		
		showGyroWarnOnce = false;
		
//		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
//		preferenceFocusMode = prefs.getString(MainScreen.getCameraMirrored() 
//				? GUI.sRearFocusModePref : GUI.sFrontFocusModePref, Camera.Parameters.FOCUS_MODE_AUTO);
		
		//this.rotationListener = new AugmentedRotationListener(this.remapOrientation);

		//initSensors();
		aewbLockedByPanorama = false;
		this.getPrefs();
	}
	
	@Override
	public void onPause()
	{
		this.deinit();
		
		synchronized (this.engine)
		{
			if (this.capturing)
			{
				if (!this.takingAlready)
				{
					this.stopCapture();
				}
			}
		}
		
//		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);        
//        prefs.edit().putString(MainScreen.getCameraMirrored()? GUI.sRearFocusModePref : GUI.sFrontFocusModePref, preferenceFocusMode).commit();
	}
	
	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			if (this.capturing && !this.takingAlready)
			{
				if (this.engine != null)
				{		
					synchronized (this.engine)
					{
						final int result = engine.cancelFrame();
						
						if (result <= 0)
						{
							this.stopCapture();
							Message msg = new Message();
		    				msg.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
		    				msg.what = PluginManager.MSG_BROADCAST;
		    				MainScreen.H.sendMessage(msg);
		    				MainScreen.guiManager.lockControls = false;
						}
						
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	@Override
	public void onGUICreate()
	{
		MainScreen.thiz.disableCameraParameter(CameraParameter.CAMERA_PARAMETER_SCENE, true, false);
		
		this.clearViews();
		
		MainScreen.guiManager.showHelp("Panorama help",
				MainScreen.thiz.getResources().getString(R.string.Panorama_Help),
				R.drawable.plugin_help_panorama, "panoramaShowHelp");
	}
	

	@Override
	public void SelectImageDimension()
    {
		this.init();
		
//		Camera camera = CameraController.getInstance().getCamera();
//    	if (null==camera)
//    		return;
		
//		final Parameters params = CameraController.getInstance().getCameraParameters();
//		final List<Camera.Size> cs = params.getSupportedPictureSizes();
		
		final List<CameraController.Size> cs = CameraController.getInstance().getSupportedPictureSizes();
		if(Build.MODEL.contains("HTC One X"))
		{
			if (MainScreen.getCameraMirrored() == false)
			{
				CameraController.Size additional= null;
				additional= CameraController.getInstance().new Size(3264, 2448);
				additional.setWidth(3264);
				additional.setHeight(2448);
				cs.add(additional);
			}
		}
		
		final CameraController.Size size = cs.get(this.prefResolution);
		
		this.pictureWidth = size.getWidth();
		this.pictureHeight = size.getHeight();
		Log.e("Almalence", String.format("Picture dimensions: %dx%d", size.getWidth(), size.getHeight()));
		
		
    	MainScreen.setImageWidth(this.pictureWidth);
    	MainScreen.setImageHeight(this.pictureHeight);
    }
	
	@Override
	public void SetCameraPreviewSize(Camera.Parameters params)
	{
		final Camera camera = CameraController.getCamera();
    	if (camera == null)
    		return;
		
		
		final CameraController.Size previewSize = this.getOptimalPreviewSize(
				CameraController.getInstance().getSupportedPreviewSizes(), this.pictureWidth, this.pictureHeight);

		this.previewWidth = previewSize.getWidth();
		this.previewHeight = previewSize.getHeight();
		
		params.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
		
    	CameraController.getInstance().setCameraParameters(params);
	}
	
	@Override
	public void SetCameraPictureSize()
	{
//		final Camera.Parameters cp = CameraController.getInstance().getCameraParameters();
//		if (cp == null)
//		{
//			return;
//		}
    	final List<CameraController.Size> picture_sizes = CameraController.getInstance().getSupportedPictureSizes();
		
    	if(Build.MODEL.contains("HTC One X"))
		{
			if (MainScreen.getCameraMirrored() == false)
			{
				CameraController.Size additional= null;
				additional= CameraController.getInstance().new Size(3264, 2448);
				additional.setWidth(3264);
				additional.setHeight(2448);
				picture_sizes.add(additional);
			}
		}
    	
		this.pictureWidth = picture_sizes.get(this.prefResolution).getWidth();
		this.pictureHeight = picture_sizes.get(this.prefResolution).getHeight();
    	
		CameraController.getInstance().setPictureSize(this.pictureWidth, this.pictureHeight);
		CameraController.getInstance().setJpegQuality(100);
    	

//		String sUserFocusMode = null;
		
//    	if (sUserFocusMode != null)
//    	{
//        	cp.setFocusMode(sUserFocusMode);
//        	
//	    	PreferenceManager.getDefaultSharedPreferences(
//	    			MainScreen.mainContext).edit().putString(
//	    					MainScreen.getCameraMirrored()
//	    						? GUI.sRearFocusModePref : GUI.sFrontFocusModePref, 
//	    								sUserFocusMode).commit();
//    	}
//		
//		if (MainScreen.supportedFocusModes != null)
//		{
//			if (MainScreen.supportedFocusModes.contains(
//					Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
//			{
//				sUserFocusMode = Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
//			}
//		}
    	

		try
		{
//			try
//			{
//				this.viewAngleX = cp.getHorizontalViewAngle();
//				this.viewAngleY = cp.getVerticalViewAngle();
//			}
//			catch (final Throwable e)
//			{
//				// Some bugged camera drivers pop ridiculous exception here, use typical view angles then 
//				this.viewAngleX = 55.4f;
//				this.viewAngleY = 42.7f;
//			}
			
			//TODO: Temporary default values. HALv3 not supported view angles yet.
			this.viewAngleX = 59.63f;
			this.viewAngleY = 46.66f;
			
			// some devices report incorrect FOV values, use typical view angles then
			if (this.viewAngleX >= 150)
			{
				this.viewAngleX = 55.4f;
				this.viewAngleY = 42.7f;
			}

			// Some devices report incorrect value for vertical view angle
			if (this.viewAngleY == this.viewAngleX)
				this.viewAngleY = this.viewAngleX*3/4;
		}
		catch (final Throwable e)
		{
			// Some bugged camera drivers pop ridiculous exception here 
		}

		// Some cameras report incorrect view angles
		// Usually vertical view angle is incorrect, but eg Htc One report incorrect horizontal view angle
		// If aspect ratio from FOV differs by more than 10% from aspect ratio from W/H
		// - re-compute view angle
		float HorizontalViewFromAspect = 2*180*(float)Math.atan((float)this.pictureWidth/(float)this.pictureHeight*
				(float)Math.tan((float)Math.PI*this.viewAngleY/(2*180))) / (float)Math.PI; 
		float VerticalViewFromAspect = 2*180*(float)Math.atan((float)this.pictureHeight/(float)this.pictureWidth*
				(float)Math.tan((float)Math.PI*this.viewAngleX/(2*180))) / (float)Math.PI;
		// not expecting very narrow field of view
		if ((VerticalViewFromAspect > 40.f) && (VerticalViewFromAspect < 0.9f*this.viewAngleY))
				this.viewAngleY = VerticalViewFromAspect;
		else if ((HorizontalViewFromAspect < 0.9f*this.viewAngleX) || (HorizontalViewFromAspect > 1.1f*this.viewAngleX))
			this.viewAngleX = HorizontalViewFromAspect;

		//Log.i(TAG,"viewAngleX: "+this.viewAngleX);
		//Log.i(TAG,"viewAngleY: "+this.viewAngleY);

//    	CameraController.getInstance().setCameraParameters(cp);
    	
		this.engine.reset(this.pictureHeight, this.pictureWidth, this.viewAngleY);
		
		if (!this.prefHardwareGyroscope)
		{
			this.sensorSoftGyroscope.SetFrameParameters(this.previewWidth,
					this.previewHeight, this.viewAngleX, this.viewAngleY);
		}
	}
	
	@Override
	public void onCameraParametersSetup()
	{
		
	}
	
	@Override
	public boolean isGLSurfaceNeeded()
	{
		return true;
	}
	
	@Override
	public void onGLSurfaceCreated(final GL10 gl, final EGLConfig config)
	{		
		this.engine.onSurfaceCreated(gl, config);
	}

	@Override
	public void onGLSurfaceChanged(final GL10 gl, final int width, final int height)
	{
		this.engine.onSurfaceChanged(gl, width, height);
	}

	@Override
	public void onGLDrawFrame(final GL10 gl)
	{
		this.engine.onDrawFrame(gl);
	}
	
	@Override
	public void OnShutterClick()
	{
		synchronized (this.engine)
		{
			if (!this.takingAlready)
			{
				if (this.capturing)
				{
					this.stopCapture();
				}
				else
				{
					this.isFirstFrame = true;
					this.capturing = true;
//					this.takingAlready = true;
//					this.startCapture();
					int fm =CameraController.getInstance().getFocusMode();
					int fs = CameraController.getFocusState();
					if(takingAlready == false && (fs == CameraController.FOCUS_STATE_IDLE ||
							fs == CameraController.FOCUS_STATE_FOCUSING)							
							&& !(fm == CameraParameters.AF_MODE_INFINITY
							|| fm == CameraParameters.AF_MODE_FIXED
							|| fm == CameraParameters.AF_MODE_EDOF
							|| fm == CameraParameters.AF_MODE_CONTINUOUS_PICTURE
							|| fm == CameraParameters.AF_MODE_CONTINUOUS_VIDEO)
							&& !MainScreen.getAutoFocusLock())			
							aboutToTakePicture = true;			
					else if(takingAlready == false)
					{
						//takePicture();
						this.startCapture();
					}
				}
			}
		}
	}
	
	
	@Override
	public boolean onBroadcast(final int command, final int arg)
	{
		if (command == PluginManager.MSG_NEXT_FRAME)
		{
//			final Camera camera = CameraController.getInstance().getCamera();
//	    	if (camera == null)
//	    	{
//	    		Log.e("Almalence", "onBroadcast(): camera is null");
//	    		return false;
//	    	}
	    	
	    	this.previewRestartFlag = true;
	    	
    		CameraController.startCameraPreview();
    		
    		//initSensors();	// attempt to fix LG G2 accelerometer slowdown 

    		new CountDownTimer(1000, 330)
    		{
    			private boolean first = true;
				public void onTick(final long millisUntilFinished)
				{
					if (this.first)
					{
						this.first = false;
						return;
					}
					
					if (PanoramaAugmentedCapturePlugin.this.previewRestartFlag)
					{
						Log.e("Almalence", String.format("Emergency preview restart"));
						CameraController.setPreviewCallbackWithBuffer();
					}
					else
					{
						this.cancel();
					}
				}

				public void onFinish()
				{
					
				}
			}.start();
    		
    		return true;
		}
		else if (command == PluginManager.MSG_FORCE_FINISH_CAPTURE)
		{
			this.stopCapture();
			
			return true;
		}
		else if (command == PluginManager.MSG_BAD_FRAME)
		{
			Toast.makeText(
					MainScreen.thiz,
					MainScreen.thiz.getResources().getString(R.string.plugin_capture_panoramaaugmented_badframe),
					Toast.LENGTH_SHORT).show();
			return true;
		}
		else if (command == PluginManager.MSG_OUT_OF_MEMORY)
		{
			Toast.makeText(
					MainScreen.thiz,
					MainScreen.thiz.getResources().getString(R.string.plugin_capture_panoramaaugmented_outofmemory),
					Toast.LENGTH_LONG).show();
			return true;
		}
		else if (command == PluginManager.MSG_NOTIFY_LIMIT_REACHED)
		{
			Toast.makeText(
					MainScreen.thiz,
					MainScreen.thiz.getResources().getString(R.string.plugin_capture_panoramaaugmented_stopcapture),
					Toast.LENGTH_LONG).show();
			return true;
		}

		return false;
	}
	
	private void getPrefs()
    {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        
		try
		{
			this.prefResolution = Integer.parseInt(prefs.getString(
					PREFERENCES_KEY_RESOLUTION + CameraController.CameraIndex, "0"));
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			Log.e("Panorama", "getPrefs exception: " + e.getMessage());
			this.prefResolution = 0;
		}
        this.prefHardwareGyroscope = prefs.getBoolean(PREFERENCES_KEY_USE_DEVICE_GYRO, this.sensorGyroscope != null);
        
        this.prefMemoryRelax = prefs.getBoolean("pref_plugin_capture_panoramaaugmented_memory", false);
        
         int overlap = Integer.parseInt(prefs.getString("pref_plugin_capture_panoramaaugmented_frameoverlap", "1"));
         switch(overlap)
         {
         case 0:
        	 AugmentedPanoramaEngine.setFrameIntersection(0.70f);
        	 break;
         case 1:
        	 AugmentedPanoramaEngine.setFrameIntersection(0.50f);
        	 break;
         case 2:
        	 AugmentedPanoramaEngine.setFrameIntersection(0.30f);
        	 break;
/*       case 3:
        	 AugmentedPanoramaEngine.setFrameIntersection(0.25f);
        	 break;
         case 4:
        	 AugmentedPanoramaEngine.setFrameIntersection(0.15f);
        	 break;
*/
         }
         
         aewblock = Integer.parseInt(prefs.getString("pref_plugin_capture_panoramaaugmented_aelock", "1"));
    }
	
	private void createPrefs(final ListPreference lp, final Preference ud_pref)
	{
		final CharSequence[] entries;
		final CharSequence[] entryValues;
		
		if (ResolutionsPictureIdxesList != null)
        {
	        entries = ResolutionsPictureNamesList.toArray(new CharSequence[ResolutionsPictureNamesList.size()]);
	        entryValues = ResolutionsPictureIdxesList.toArray(new CharSequence[ResolutionsPictureIdxesList.size()]);

	        if (lp != null)
	        {
	        	lp.setKey(PREFERENCES_KEY_RESOLUTION + CameraController.CameraIndex);
	        	
		        lp.setEntries(entries);
		        lp.setEntryValues(entryValues);
		        
		        // set currently selected image size
				int idx;
				for (idx = 0; idx < ResolutionsPictureIdxesList.size(); ++idx)
				{
					if (Integer.parseInt(ResolutionsPictureIdxesList.get(idx)) == this.prefResolution)
					{
						break;
					}
				}
				if (idx < ResolutionsPictureIdxesList.size())
				{
					
					lp.setValueIndex(idx);
//				}
//				else 
//				{
//					idx=0;
//					lp.setValueIndex(idx);
//				}
					lp.setSummary(entries[idx]);
			        lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			        {
						//@Override
						public boolean onPreferenceChange(Preference preference, Object newValue)
						{
							int value = Integer.parseInt(newValue.toString());
							PanoramaAugmentedCapturePlugin.this.prefResolution = value;
				    		
							for (int i = 0; i < ResolutionsPictureIdxesList.size(); i++)
							{
								if (ResolutionsPictureIdxesList.get(i).equals(newValue))
								{
									final int idx = i;
									final Point point = ResolutionsPictureSizesList.get(idx);
									
									// frames_fit_count may decrease when returning to main view due to slightly
									// more memory used, so in text messages we report both exact and decreased count to the user 
									final int frames_fit_count = 
											(int)(getAmountOfMemoryToFitFrames()
													/ getFrameSizeInBytes(point.x, point.y));
									
									{
										Toast.makeText(	MainScreen.thiz, String.format(
												MainScreen.thiz.getString(
														R.string.pref_plugin_capture_panoramaaugmented_imageheight_warning),
														frames_fit_count), Toast.LENGTH_SHORT).show();
										
										return true;
									}
								}
							}
							
			                return true;
						}
			        });
			        
				}
	        }
        }
		
		ud_pref.setOnPreferenceChangeListener(
	        new OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(final Preference preference, final Object newValue)
				{
					getPrefs();
					if (!PanoramaAugmentedCapturePlugin.this.prefHardwareGyroscope && !((Boolean)newValue))
					{					
						final AlertDialog ad = new AlertDialog.Builder(MainScreen.thiz)
		    			.setIcon(R.drawable.alert_dialog_icon)
		    			.setTitle(R.string.pref_plugin_capture_panoramaaugmented_nogyro_dialog_title)
		    			.setMessage(R.string.pref_plugin_capture_panoramaaugmented_nogyro_dialog_text)
		    			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
		    			{
		    				public void onClick(final DialogInterface dialog, final int whichButton)
		    				{
		    					dialog.dismiss();
		    				}
		    			})
		    			.create();
				
						if (showGyroWarnOnce == true)
							return false;
						showGyroWarnOnce = true;
						ad.show();
						
						return false;
					}
					else
					{
						return true;
					}
				}
			});
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onPreferenceCreate(final PreferenceActivity prefActivity)
	{
    	this.createPrefs((ListPreference)prefActivity.findPreference(PREFERENCES_KEY_RESOLUTION),
    			prefActivity.findPreference(PREFERENCES_KEY_USE_DEVICE_GYRO));
	}
	@Override
	public void onPreferenceCreate(final PreferenceFragment prefActivity)
	{
    	this.createPrefs((ListPreference)prefActivity.findPreference(PREFERENCES_KEY_RESOLUTION),
    			prefActivity.findPreference(PREFERENCES_KEY_USE_DEVICE_GYRO));
	}
	
	private void scanResolutions()
	{
    	ResolutionsPictureNamesList.clear();
    	ResolutionsPictureIdxesList.clear();
    	ResolutionsPictureSizesList.clear();
		
//		Camera camera = CameraController.getInstance().getCamera();
//    	if (null==camera)
//    		return;
//		final Parameters cp = CameraController.getInstance().getCameraParameters();
//		final List<Camera.Size> cs = cp.getSupportedPictureSizes();
    	
    	final List<CameraController.Size> cs = CameraController.getInstance().getSupportedPictureSizes();
    	if(Build.MODEL.contains("HTC One X"))
		{
			if (MainScreen.getCameraMirrored() == false)
			{
				CameraController.Size additional= null;
				additional= CameraController.getInstance().new Size(3264, 2448);
				additional.setWidth(3264);
				additional.setHeight(2448);
				cs.add(additional);
			}
		}
        
        int maxIndex = 0;

        if (cs!=null)
        {
	    	for (int ii=0; ii<cs.size(); ++ii)
	    	{
	            final CameraController.Size s = cs.get(ii); 
	
	            if (s.getWidth() > cs.get(maxIndex).getWidth())
	            {
	            	maxIndex = ii;
	            }
	
	            if ((long)s.getWidth() >= MIN_HEIGHT_SUPPORTED)
	            {
	            	// find good location in a list
	            	int loc;
	            	boolean shouldInsert = true;
	            	for (loc = 0; loc < ResolutionsPictureSizesList.size(); ++loc)
	            	{
	            		final Point psize = ResolutionsPictureSizesList.get(loc);
	            		if (psize.x == s.getWidth())
	            		{
	            			if (s.getHeight() > psize.y)
	            			{
	    		            	ResolutionsPictureNamesList.remove(loc);
	    		            	ResolutionsPictureIdxesList.remove(loc);
	    		            	ResolutionsPictureSizesList.remove(loc);
		            			break;
	            			}
	            			else
	            			{
		            			shouldInsert = false;
		            			break;
	            			}
	            		}
	            		else if (psize.x < s.getWidth())
	            		{
	            			break;
	            		}
	            	}
	            	
	            	if (shouldInsert)
	            	{
		            	ResolutionsPictureNamesList.add(loc, String.format("%dpx", s.getWidth()));
		            	ResolutionsPictureIdxesList.add(loc, String.format("%d", ii));
		            	ResolutionsPictureSizesList.add(loc, new Point(s.getWidth(), s.getHeight()));
	            	}
	            }
	        }
	    	
	    	if (ResolutionsPictureIdxesList.size() == 0)
	    	{
	            final CameraController.Size s = cs.get(maxIndex); 
	    		
	            ResolutionsPictureNamesList.add(String.format("%dpx", s.getWidth()));
	            ResolutionsPictureIdxesList.add(String.format("%d", maxIndex));
	            ResolutionsPictureSizesList.add(new Point(s.getWidth(), s.getHeight()));
	    	}
        }
	}
	
	private static long getAmountOfMemoryToFitFrames()
	{
		// activityManager returning crap (way less than really available)
        //this.activityManager.getMemoryInfo(this.memoryInfo);
		//Log.i(TAG,"memoryInfo.availMem: "+this.memoryInfo.availMem+"memoryInfo.threshold: "+this.memoryInfo.threshold);
		//return (long)((this.memoryInfo.availMem - this.memoryInfo.threshold - 2097152) * 0.7f);
		
		final int[] mi = Util.getMemoryInfo();

		Log.e(TAG, "Memory: used: "+mi[0]+"Mb  free: "+mi[1]+"Mb");

		// memory required for stitched panorama about equals to memory required for input frames
		// (total height can be more, but frames are overlapped by 1/3 horizontally)   
		// in sweep mode: approx. one out of three frames used,
		// therefore increase in stitched panorama height due to non-straight frames is compensated
		
		// augmented mode: for output panorama: ~nFrames*bytesPerFrame
		// for intermediate pre-rendered frames: just a bit more than nFrames*bytesPerFrame (20% is possible)
		// also, there is a possibility of memory fragmentation
		
		return (long) ((mi[1] - 10.f) * 1000000.f * 0.8f);	// use up to 80% and ensure at least 64Mb left free
	}
	
	public static int getFrameSizeInBytes(int width, int height)
	{
		return (5 * width * height + width + 256);
	}
	
	private void checkCoordinatesRemapRequired()
	{
		final Display display = ((WindowManager)MainScreen.thiz.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		// This is proved way of checking it so we better use deprecated methods.
        @SuppressWarnings("deprecation")
		final int orientation = (display.getWidth() <= display.getHeight()) ? Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE;	
        final int rotation = display.getRotation();
		this.remapOrientation = (orientation == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_0) ||
				(orientation == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_180) ||
				(orientation == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_90) ||
				(orientation == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_270);
	}
	
	private void startCapture()
	{
		lockAEWB();
		
		Date curDate = new Date();
		SessionID = curDate.getTime();

		final int frames_fit_count = 
				(int)(getAmountOfMemoryToFitFrames() / getFrameSizeInBytes(this.pictureWidth, this.pictureHeight));
		this.engine.setMaxFrames(prefMemoryRelax?frames_fit_count*2:frames_fit_count);
		
		this.engine.ViewportCreationTime();
		
		MainScreen.H.sendEmptyMessage(PluginManager.MSG_TAKE_PICTURE);
	}
	
	@Override
	public void onPreviewFrame(final byte[] data, final Camera paramCamera)
	{		
		this.previewRestartFlag = false;
		
		if (!this.prefHardwareGyroscope)
		{
			this.sensorSoftGyroscope.NewData(data);
		}
		
		synchronized (this.engine)
		{
			if (!this.takingAlready)
			{
				final int state = this.engine.getPictureTakingState(
						CameraController.getInstance().getFocusMode() == CameraParameters.AF_MODE_AUTO);
				
				if (state == AugmentedPanoramaEngine.STATE_TAKINGPICTURE)
				{
					this.takingAlready = true;
					MainScreen.H.sendEmptyMessage(PluginManager.MSG_TAKE_PICTURE);
				}
			}
		}
	}
	
	@TargetApi(19)
	@Override
	public void onPreviewAvailable(Image im)
	{
		this.previewRestartFlag = false;
		
		if (!this.prefHardwareGyroscope)
		{
			ByteBuffer Y = im.getPlanes()[0].getBuffer();
			ByteBuffer U = im.getPlanes()[1].getBuffer();
			ByteBuffer V = im.getPlanes()[2].getBuffer();

			if ( (!Y.isDirect()) || (!U.isDirect()) || (!V.isDirect()) )
			{
				Log.e("PanoramaCapturePlugin", "Oops, YUV ByteBuffers isDirect failed");
				return;
			}
			
			int imageWidth = im.getWidth();
			int imageHeight = im.getHeight();
			// Note: android documentation guarantee that:
			// - Y pixel stride is always 1
			// - U and V strides are the same
			//   So, passing all these parameters is a bit overkill
			int status = YuvImage.CreateYUVImage(Y, U, V,
					im.getPlanes()[0].getPixelStride(),
					im.getPlanes()[0].getRowStride(),
					im.getPlanes()[1].getPixelStride(),
					im.getPlanes()[1].getRowStride(),
					im.getPlanes()[2].getPixelStride(),
					im.getPlanes()[2].getRowStride(),
					imageWidth, imageHeight, 1);
			
			if (status != 0)
				Log.e("CapturePlugin", "Error while cropping: "+status);
			
			
			byte[] data = YuvImage.GetByteFrame(1);
			this.sensorSoftGyroscope.NewData(data);
			YuvImage.RemoveFrame(1);
			System.gc();
		}
		
		synchronized (this.engine)
		{
			if (!this.takingAlready)
			{
				final int state = this.engine.getPictureTakingState(
						CameraController.getInstance().getFocusMode() == CameraParameters.AF_MODE_AUTO);
				
				if (state == AugmentedPanoramaEngine.STATE_TAKINGPICTURE)
				{					
					this.takingAlready = true;
					Log.e(TAG, "MSG_TAKE_PICTURE onPreviewAvailable");
					MainScreen.H.sendEmptyMessage(PluginManager.MSG_TAKE_PICTURE);
				}
			}
		}
	}
	
//	private final AutoFocusCallback autoFocusCallbackReceiver = new AutoFocusCallback()
//	{
//		@Override
//		public void onAutoFocus(final boolean success, final Camera camera)
//		{
//			PanoramaAugmentedCapturePlugin.this.takePictureReal();
//		}
//	};
//	
//	private void tryAutoFocus()
//	{
//		try
//		{
////			final Camera camera = CameraController.getInstance().getCamera();
////	    	if (camera == null)
////	    	{
////	    		Log.e("Almalence", "tryAutoFocus(): camera is null");
////	    		return;
////	    	}
////			camera.autoFocus(this.autoFocusCallbackReceiver);
//			CameraController.autoFocus(this.autoFocusCallbackReceiver);
//		}
//		catch (final Throwable e)
//		{
//			e.printStackTrace();
//			this.takePicture();
//		}
//	}
//	
//	@Override
//	public void onAutoFocus(final boolean success, final Camera camera)
//	{
//		
//	}
	
	@Override
	public void onAutoFocus(final boolean success)
	{
		if(aboutToTakePicture == true)
			startCapture();
	}
	
	@Override
	public void takePicture()
	{		
		synchronized (this.engine)
		{
			if (!this.capturing)
			{
				this.takingAlready = false;
				aboutToTakePicture = false;
				return;
			}
		}
		
//		try
//		{
//			if (CameraController.getInstance().getFocusMode() == CameraParameters.AF_MODE_AUTO)
//			{
//				this.tryAutoFocus();
//			}
//			else
//			{
//				this.takePictureReal();
//			}
//		}
//		catch (final Throwable e)
//		{
//			e.printStackTrace();
//		}
		
		this.takingAlready = true;
		
		this.takePictureReal();
	}
	
	private void lockAEWB()
	{
		boolean lock = false;
		switch(aewblock)
		{
			case 0:
				lock = false;
				break;
			case 1:
				if(MainScreen.guiManager.getDisplayOrientation() == 90 || MainScreen.guiManager.getDisplayOrientation() == 180 )
					lock = true;
				break;
			case 2:
				lock = true;
				break;
		}
		
		if (lock)
		{
			Camera.Parameters params = CameraController.getInstance().getCameraParameters();
			if (params != null)
			{
				if(CameraController.getInstance().isWhiteBalanceLockSupported() && !params.getAutoWhiteBalanceLock())
				{
					params.setAutoWhiteBalanceLock(true);
					CameraController.getInstance().setCameraParameters(params);
					aewbLockedByPanorama = true;
				}
				if(CameraController.getInstance().isExposureLockSupported() && !params.getAutoExposureLock())
				{
					params.setAutoExposureLock(true);
					CameraController.getInstance().setCameraParameters(params);
					aewbLockedByPanorama = true;
				}
			}
		}
		lock = false;
	}
	
	private void unlockAEWB()
	{
		if (aewbLockedByPanorama)
		{
			Camera.Parameters params = CameraController.getInstance().getCameraParameters();
			if (params != null)
			{
				if(CameraController.getInstance().isWhiteBalanceLockSupported() && params.getAutoWhiteBalanceLock())
				{
					params.setAutoWhiteBalanceLock(false);
					CameraController.getInstance().setCameraParameters(params);
				}
				if(CameraController.getInstance().isExposureLockSupported() && params.getAutoExposureLock())
				{
					params.setAutoExposureLock(false);
					CameraController.getInstance().setCameraParameters(params);
				}
			}
			aewbLockedByPanorama = false;
		}
	}

	private void takePictureReal()
	{    	
    	this.coordsRecorded = false;
    	try 
    	{
    		Log.e(TAG, "Perform CAPTURE Panorama");
			requestID = CameraController.captureImage(1, CameraController.YUV);
		}
    	catch (Exception e)
		{
			e.printStackTrace();
			
			final Message msg = new Message();
			msg.arg1 = PluginManager.MSG_FORCE_FINISH_CAPTURE;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);
			
			takingAlready = false;
			capturing = false;
			aboutToTakePicture = false;
		}
	}

	@Override
	public void onShutter()
	{
		this.coordsRecorded = true;
		this.engine.recordCoordinates();
	}
	
	@Override
	public void onPictureTaken(final byte[] paramArrayOfByte, final Camera paramCamera)
	{		
		final boolean goodPlace;
		
		synchronized (this.engine)
		{
			this.takingAlready = false;
			this.engine.notifyAll();
			
			if (!this.coordsRecorded)
			{
				this.engine.recordCoordinates();
			}
			
			goodPlace = this.engine.onPictureTaken(paramArrayOfByte);
			
			if (this.isFirstFrame)
			{
				PluginManager.getInstance().addToSharedMem_ExifTagsFromJPEG(paramArrayOfByte, SessionID);
				this.isFirstFrame = false;
			}
		}
		
		final boolean done = this.engine.isCircular();
		final boolean oom = this.engine.isMax();
		
		if (oom && !done)
		{
			final Message msg = new Message();
			msg.arg1 = PluginManager.MSG_OUT_OF_MEMORY;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);
		}
		else if (done)
		{
			final Message msg = new Message();
			msg.arg1 = PluginManager.MSG_NOTIFY_LIMIT_REACHED;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);
		}

		{
			final Message msg = new Message();
			msg.arg1 = PluginManager.MSG_NEXT_FRAME;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessageDelayed(msg, 300);
		}
		
		if (!goodPlace)
		{
			final Message msg = new Message();
			msg.arg1 = PluginManager.MSG_BAD_FRAME;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);
		}
		
		if (done || oom)
		{
			final Message msg = new Message();
			msg.arg1 = PluginManager.MSG_FORCE_FINISH_CAPTURE;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);
		}
	}
	
	@TargetApi(19)
	@Override
	public void onImageAvailable(Image im)
	{
		final boolean goodPlace;
		
		synchronized (this.engine)
		{
			this.takingAlready = false;
			this.engine.notifyAll();
			
			if (!this.coordsRecorded)
			{
				this.engine.recordCoordinates();
			}
			
			goodPlace = this.engine.onImageAvailable(im);
			
//			if (this.isFirstFrame)
//			{
//				PluginManager.getInstance().addToSharedMem_ExifTagsFromJPEG(paramArrayOfByte, SessionID);
//				this.isFirstFrame = false;
//			}
		}
		
		final boolean done = this.engine.isCircular();
		final boolean oom = this.engine.isMax();
		
		if (oom && !done)
		{
			final Message msg = new Message();
			msg.arg1 = PluginManager.MSG_OUT_OF_MEMORY;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);
		}
		else if (done)
		{
			final Message msg = new Message();
			msg.arg1 = PluginManager.MSG_NOTIFY_LIMIT_REACHED;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);
		}

		{
			final Message msg = new Message();
			msg.arg1 = PluginManager.MSG_NEXT_FRAME;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessageDelayed(msg, 300);
		}
		
		if (!goodPlace)
		{
			final Message msg = new Message();
			msg.arg1 = PluginManager.MSG_BAD_FRAME;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);
		}
		
		if (done || oom)
		{
			final Message msg = new Message();
			msg.arg1 = PluginManager.MSG_FORCE_FINISH_CAPTURE;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);
		}
	}
	
	
	@TargetApi(19)
	@Override
	public void onCaptureCompleted(CaptureResult result)
	{
		if(result.get(CaptureResult.REQUEST_ID) == requestID)
		{
			if (this.isFirstFrame)
			{
	    		PluginManager.getInstance().addToSharedMem_ExifTagsFromCaptureResult(result, SessionID);
	    		this.isFirstFrame = false;
			}
		}
	}
	
	
	
	@SuppressLint("FloatMath")
	private void stopCapture()
	{		
		this.capturing = false;
		
		unlockAEWB();
		
		final LinkedList<AugmentedFrameTaken> frames = this.engine.retrieveFrames();
		
		if (frames.size() > 0)
		{
			PluginManager.getInstance().addToSharedMem("frameorientation" + String.valueOf(SessionID), String.valueOf(MainScreen.guiManager.getDisplayOrientation()));
			PluginManager.getInstance().addToSharedMem("pano_mirror" + String.valueOf(SessionID), String.valueOf(MainScreen.getCameraMirrored()));
			PluginManager.getInstance().addToSharedMem("pano_width"+String.valueOf(SessionID), String.valueOf(this.pictureHeight));
			PluginManager.getInstance().addToSharedMem("pano_height"+String.valueOf(SessionID), String.valueOf(this.pictureWidth));
			PluginManager.getInstance().addToSharedMem("pano_frames_count"+String.valueOf(SessionID), String.valueOf(frames.size()));
			PluginManager.getInstance().addToSharedMem("pano_camera_fov"+String.valueOf(SessionID), String.valueOf((int)(this.viewAngleY + 0.5f)));
			PluginManager.getInstance().addToSharedMem("pano_useall"+String.valueOf(SessionID), "1");
			PluginManager.getInstance().addToSharedMem("pano_freeinput"+String.valueOf(SessionID), "0");
			
			Vector3d normalLast = new Vector3d();
			Vector3d normalCurrent = new Vector3d();
			Vector3d vTop = new Vector3d();

			float R = this.engine.getRadiusToEdge();
			float PixelsShiftX = 0.0f;
			float PixelsShiftY = 0.0f;
			float angleXprev = 0;
			float angleX = 0;
			float angleY = 0;
			float angleR = 0;
			final Iterator<AugmentedFrameTaken> iterator = frames.iterator();
			AugmentedFrameTaken frame = iterator.next();

			// baseTransform matrix converts from world coordinate system into
			// camera coordinate system (as it was during taking first frame):
			// x,y - is screen surface (in portrait mode x pointing right, y pointing down)
			// z - axis is perpendicular to screen surface, pointing in direction opposite to where camera is pointing
			float[] baseTransform16 = this.engine.initialTransform;
			float[] baseTransform = new float[9];
			baseTransform[0] = baseTransform16[0]; baseTransform[1] = baseTransform16[1]; baseTransform[2] = baseTransform16[2]; 
			baseTransform[3] = baseTransform16[4]; baseTransform[4] = baseTransform16[5]; baseTransform[5] = baseTransform16[6]; 
			baseTransform[6] = baseTransform16[8]; baseTransform[7] = baseTransform16[9]; baseTransform[8] = baseTransform16[10]; 
			
			frame.getPosition(normalLast);
			normalLast = transformVector(normalLast, baseTransform);
			
			//final int[] frames_ptr = new int[frames.size()];
			//final float[][][] frames_trs = new float[frames.size()][][];
			int frame_cursor = 0;
			
			while (true)
			{
				frame.getPosition(normalCurrent);
				frame.getTop(vTop);
				
				normalCurrent = transformVector(normalCurrent, baseTransform);
				vTop = transformVector(vTop, baseTransform);
				angleR = getAngle(normalCurrent, vTop, R);

				angleX = (float)Math.atan2(-normalLast.z, normalLast.x) - (float)Math.atan2(-normalCurrent.z, normalCurrent.x);
				angleY = (float)Math.asin(-normalCurrent.y/R);
									
				// make sure angle difference is within bounds
				// along X axis the angle difference is always positive measuring from the previous frame 
				while (angleX-angleXprev > 2*Math.PI) angleX -= 2*Math.PI;
				while (angleX-angleXprev < 0) angleX += 2*Math.PI;
				while (angleY > Math.PI) angleY -= 2*Math.PI;
				while (angleY < -Math.PI) angleY += 2*Math.PI;
				
				angleXprev = angleX;
				
				PixelsShiftX = angleX * R; 
				PixelsShiftY = angleY * R; 
				
				// convert rotation around center into rotation around top-left corner
				PixelsShiftX +=   MainScreen.getImageWidth()/2 * (1-FloatMath.cos(angleR)) + MainScreen.getImageHeight()/2 * FloatMath.sin(angleR); 
				PixelsShiftY += - MainScreen.getImageWidth()/2 * FloatMath.sin(angleR)     + MainScreen.getImageHeight()/2 * (1-FloatMath.cos(angleR)); 
				
				//Log.i("CameraTest","vTop: " + vTop + " angleR: " + angleR*180/Math.PI);
				
				PluginManager.getInstance().addToSharedMem("pano_frame"+(frame_cursor+1)+"."+String.valueOf(SessionID), String.valueOf(frame.getNV21address()));

				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".00."+String.valueOf(SessionID), String.valueOf(FloatMath.cos(angleR)));
				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".01."+String.valueOf(SessionID), String.valueOf(-FloatMath.sin(angleR)));
				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".02."+String.valueOf(SessionID), String.valueOf(PixelsShiftX));
				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".10."+String.valueOf(SessionID), String.valueOf(FloatMath.sin(angleR)));
				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".11."+String.valueOf(SessionID), String.valueOf(FloatMath.cos(angleR)));
				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".12."+String.valueOf(SessionID), String.valueOf(PixelsShiftY));
				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".20."+String.valueOf(SessionID), String.valueOf(0.0f));
				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".21."+String.valueOf(SessionID), String.valueOf(0.0f));
				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".22."+String.valueOf(SessionID), String.valueOf(1.0f));
				
				if (!iterator.hasNext()) break;
				
				frame = iterator.next();
				
				frame_cursor++;
			}

	    	
			Message message = new Message();
			message.obj = String.valueOf(SessionID);
			message.what = PluginManager.MSG_CAPTURE_FINISHED;
			MainScreen.H.sendMessage(message);
			
		}
	}	
	
	private static Vector3d transformVector(final Vector3d vec, final float[] mat)
	{
		final Vector3d vo = new Vector3d();
		
		vo.x = vec.x * mat[0] + vec.y * mat[1] + vec.z * mat[2]; 
		vo.y = vec.x * mat[3] + vec.y * mat[4] + vec.z * mat[5]; 
		vo.z = vec.x * mat[6] + vec.y * mat[7] + vec.z * mat[8]; 
				
		return vo;
	}	
	
	// as in: http://stackoverflow.com/questions/5188561/signed-angle-between-two-3d-vectors-with-same-origin-within-the-same-plane-reci
	private static float signedAngle(final Vector3d Va, final Vector3d Vb, final Vector3d Vn)
	{
		try
		{
			Vector3d Vx = new Vector3d();
			
			Vx.x = Va.y*Vb.z - Va.z*Vb.y;
			Vx.y = Va.z*Vb.x - Va.x*Vb.z;
			Vx.z = Va.x*Vb.y - Va.y*Vb.x;
			
			float sina = Vx.length() / ( Va.length() * Vb.length() );
			float cosa = (Va.x*Vb.x+Va.y*Vb.y+Va.z*Vb.z) / ( Va.length() * Vb.length() );

			float angle = (float)Math.atan2( sina, cosa );

			float sign = Vn.x*Vx.x+Vn.y*Vx.y+Vn.z*Vx.z;
			if(sign<0) return -angle;
				else return angle;
		}
		catch (final Exception e)	// if div0 happens
		{
			return 0;
		}
	}
		
	// compute rotation angle (around normal vector, relative to Y axis)
	private static float getAngle (final Vector3d normal, final Vector3d vTop, final float radius)
	{
		final float angle;
		
		Vector3d vA = new Vector3d();

		// assuming y axis pointing upwards (ideal top vector = (0,1,0))
		if (normal.y > 0)
		{
			vA.x = -normal.x;
			vA.z = -normal.z;
			vA.y = radius*radius/normal.y-normal.y;
		}
		else if (normal.y < 0)
		{
			vA.x = normal.x;
			vA.z = normal.z;
			vA.y = -radius*radius/normal.y+normal.y;
		}
		else { vA.x = 0; vA.y = 1; vA.z = 0; }
		
		if (vA.length() > 0)
		{
			angle = signedAngle(vA, vTop, normal);
		}
		else
			angle = 0;	// angle is unknown at pole locations

		return angle;
	}
}
