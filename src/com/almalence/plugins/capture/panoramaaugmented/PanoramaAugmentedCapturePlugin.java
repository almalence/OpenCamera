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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorManager;
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
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
import com.almalence.opencam.ui.GUI;
import com.almalence.opencam.ui.GUI.CameraParameter;
import com.almalence.opencam.util.Util;
import com.almalence.plugins.capture.panoramaaugmented.AugmentedPanoramaEngine.AugmentedFrameTaken;

public class PanoramaAugmentedCapturePlugin extends PluginCapture implements AutoFocusCallback
{
	private static final String TAG = "PanoramaAugmentedCapturePlugin";
	
	private static final String PREFERENCES_KEY_RESOLUTION = "pref_plugin_capture_panoramaaugmented_imageheight";
	private static final String PREFERENCES_KEY_USE_DEVICE_GYRO = "pref_plugin_capture_panoramaaugmented_usehardwaregyro";
	private static final String PREFERENCES_KEY_FOCUS = "pref_plugin_capture_panoramaaugmented_focus";
	
	
	private AugmentedPanoramaEngine engine;


	public final int MIN_HEIGHT_SUPPORTED = 640;
	public final List<Point> ResolutionsPictureSizesList = new ArrayList<Point>();
	public final List<String> ResolutionsPictureIdxesList = new ArrayList<String>();
	public final List<String> ResolutionsPictureNamesList = new ArrayList<String>();
	
	private int prefResolution;
	private boolean prefFocusContinuous;
	private boolean prefAutofocus;
	private boolean prefRefocus;
	private boolean prefHardwareGyroscope;
	
	private String preferenceFocusMode;
	
	private float viewAngleX = 54.8f;
	private float viewAngleY = 42.5f;

	private SensorManager sensorManager;
	private Sensor sensorAccelerometer;
	private Sensor sensorMagnetometer;
	private Sensor sensorGyroscope;
	private VfGyroSensor sensorSoftGyroscope;
	private boolean remapOrientation;

	private volatile boolean capturing;
	private final AtomicBoolean takingAlready = new AtomicBoolean();
	
	private AugmentedRotationListener rotationListener;
	
	private int pictureWidth;
	private int pictureHeight;
	private int previewWidth;
	private int previewHeight;
	
	private static boolean isFirstFrame = true;
	
	private volatile boolean coordsRecorded;
	private volatile boolean previewRestartFlag;
	
	public PanoramaAugmentedCapturePlugin()
	{
		super("com.almalence.plugins.panoramacapture_augmented",
				R.xml.preferences_capture_panoramaaugmented,
				0,
				MainScreen.thiz.getResources().getString(R.string.pref_plugin_capture_panoramaaugmented_preference_title),
				MainScreen.thiz.getResources().getString(R.string.pref_plugin_capture_panoramaaugmented_preference_summary),
				0,
				null);
		
		this.capturing = false;
		this.takingAlready.set(false);
		
		this.sensorManager = (SensorManager)MainScreen.mainContext.getSystemService(Context.SENSOR_SERVICE);
		this.sensorAccelerometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		this.sensorMagnetometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		this.sensorGyroscope = this.sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		this.sensorSoftGyroscope = new VfGyroSensor(null);
	}
	
	private void init()
	{	
		this.scanResolutions();	
		
		this.getPrefs();
		
		this.checkCoordinatesRemapRequired();
		
		this.rotationListener = new AugmentedRotationListener(this.remapOrientation);
		
		if (this.prefHardwareGyroscope)
		{
			this.sensorManager.registerListener(
					this.rotationListener, 
					this.sensorGyroscope, 
					SensorManager.SENSOR_DELAY_GAME);
		}
		else
		{
			this.sensorSoftGyroscope.open();
			this.sensorSoftGyroscope.SetListener(this.rotationListener);
		}
		
		this.sensorManager.registerListener(
				this.rotationListener,
				this.sensorAccelerometer,
				SensorManager.SENSOR_DELAY_GAME);
		
		this.sensorManager.registerListener(
				this.rotationListener,
				this.sensorMagnetometer,
				SensorManager.SENSOR_DELAY_GAME);
		
		this.rotationListener.setReceiver(this.engine);
	}
	
	private void deinit()
	{
		if (this.prefHardwareGyroscope)
		{
			this.sensorManager.unregisterListener(this.rotationListener, this.sensorGyroscope);
		}
		else
		{
			this.sensorSoftGyroscope.SetListener(null);
		}
		
		this.sensorManager.unregisterListener(this.rotationListener, this.sensorAccelerometer);
		this.sensorManager.unregisterListener(this.rotationListener, this.sensorMagnetometer);
	}
	
	@Override
	public void onDefaultsSelect()
	{	
		this.scanResolutions();
		
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.thiz);
		
		if (!prefs.contains(PREFERENCES_KEY_RESOLUTION))
		{
			if (ResolutionsPictureIdxesList.size() > 0)
			{	
				boolean found = false;
				int idx = 0;
				
				// first - try to select resolution <= 5mpix and with at least 5 frames available 
				for (int i = 0; i < ResolutionsPictureSizesList.size(); i++)
				{
					final Point point = ResolutionsPictureSizesList.get(i);
					final int frames_fit_count = 
							(int)(getAmountOfMemoryToFitFrames() / getFrameSizeInBytes(point.x, point.y));
					
					if ((frames_fit_count >= 5) && (point.x*point.y < 5200000))
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
				
	    		prefs.edit().putString(
	    						PREFERENCES_KEY_RESOLUTION, 
	    						ResolutionsPictureIdxesList.get(idx)).commit();
			}
		}
		
		if (!prefs.contains(PREFERENCES_KEY_USE_DEVICE_GYRO))
		{
			prefs.edit().putBoolean(
							PREFERENCES_KEY_USE_DEVICE_GYRO,
							this.sensorGyroscope != null).commit();
		}

		if (!prefs.contains(PREFERENCES_KEY_FOCUS))
		{
			Camera camera = MainScreen.thiz.getCamera();
	    	if (null==camera)
	    		return;
			// set default focus mode to continuous if available (works faster for augmented mode)
	    	final Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
			List<String> supportedFocusModes = cp.getSupportedFocusModes();
			if (supportedFocusModes!=null)
			{
				if ((supportedFocusModes.indexOf(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) >= 0) &&
					(supportedFocusModes.indexOf(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) >= 0))
				{
		    		prefs.edit().putString(PREFERENCES_KEY_FOCUS, "3").commit();
				}
			}
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
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		preferenceFocusMode = prefs.getString(MainScreen.getCameraMirrored() 
				? GUI.sRearFocusModePref : GUI.sFrontFocusModePref, Camera.Parameters.FOCUS_MODE_AUTO);
	}
	
	@Override
	public void onPause()
	{
		this.deinit();
		
		if (this.capturing)
		{
			if (!this.takingAlready.get())
			{
				this.capturing = false;
				this.stopCapture();
			}
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);        
        prefs.edit().putString(MainScreen.getCameraMirrored()? GUI.sRearFocusModePref : GUI.sFrontFocusModePref, preferenceFocusMode).commit();
	}
	
	@Override
	public void onGUICreate()
	{
		MainScreen.thiz.disableCameraParameter(CameraParameter.CAMERA_PARAMETER_SCENE, true, false);
		MainScreen.thiz.disableCameraParameter(CameraParameter.CAMERA_PARAMETER_FOCUS, true, true);
		
		this.clearViews();
		
		MainScreen.guiManager.showHelp("Panorama help", MainScreen.thiz.getResources().getString(R.string.Panorama_Help), R.drawable.plugin_help_panorama, "panoramaShowHelp");
	}
	

	@Override
	public void SelectImageDimension()
    {
		this.init();
		
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
		
		final Parameters params = MainScreen.thiz.getCameraParameters();
		final List<Camera.Size> cs = params.getSupportedPictureSizes();
		
		final Size size = cs.get(this.prefResolution);
		
		this.pictureWidth = size.width;
		this.pictureHeight = size.height;
		Log.e("Almalence", String.format("Picture dimensions: %dx%d", size.width, size.height));
		
		
    	MainScreen.setImageWidth(this.pictureWidth);
    	MainScreen.setImageHeight(this.pictureHeight);
    }
	
	@Override
	public void SetCameraPreviewSize(Camera.Parameters params)
	{
		final Camera camera = MainScreen.thiz.getCamera();
    	if (camera == null)
    		return;
		
		
		final Size previewSize = this.getOptimalPreviewSize(
				params.getSupportedPreviewSizes(), this.pictureWidth, this.pictureHeight);

		this.previewWidth = previewSize.width;
		this.previewHeight = previewSize.height;
		
		params.setPreviewSize(previewSize.width, previewSize.height);
		
    	MainScreen.thiz.setCameraParameters(params);
	}
	
	@Override
	public void SetCameraPictureSize()
	{
		final Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
    	final List<Size> picture_sizes = cp.getSupportedPictureSizes();
    	
		this.pictureWidth = picture_sizes.get(this.prefResolution).width;
		this.pictureHeight = picture_sizes.get(this.prefResolution).height;
    	
		cp.setPictureSize(this.pictureWidth, this.pictureHeight);
		cp.setJpegQuality(100);
    	
		String sUserFocusMode;
    	if (this.prefFocusContinuous
    			&& MainScreen.supportedFocusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
    		sUserFocusMode = Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
    	else if (MainScreen.supportedFocusModes.contains(Parameters.FOCUS_MODE_INFINITY))
    		sUserFocusMode = Parameters.FOCUS_MODE_INFINITY;
    	else
    		sUserFocusMode = MainScreen.supportedFocusModes.get(0);
		
    	cp.setFocusMode(sUserFocusMode);
		
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putString(MainScreen.getCameraMirrored()? GUI.sRearFocusModePref : GUI.sFrontFocusModePref, sUserFocusMode).commit();
		
		try
		{
			this.viewAngleX = cp.getHorizontalViewAngle();
			this.viewAngleY = cp.getVerticalViewAngle();
			
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

    	MainScreen.thiz.setCameraParameters(cp);
    	
		this.engine.reset(this.pictureHeight, this.pictureWidth, this.viewAngleY);
		
		this.sensorSoftGyroscope.SetFrameParameters(this.previewWidth, this.previewHeight, this.viewAngleX, this.viewAngleY);
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
		if (!this.takingAlready.get())
		{
			if (this.capturing)
			{
				this.capturing = false;
				this.stopCapture();
			}
			else
			{
				isFirstFrame = true;
				this.capturing = true;
				this.takingAlready.set(true);
				this.startCapture();
			}
		}
	}
	
	@Override
	public boolean onBroadcast(final int command, final int arg)
	{
		if (command == PluginManager.MSG_NEXT_FRAME)
		{
			final Camera camera = MainScreen.thiz.getCamera();
	    	if (camera == null)
	    	{
	    		Log.e("Almalence", "onBroadcast(): camera is null");
	    		return false;
	    	}
	    	
	    	this.previewRestartFlag = true;
	    	try {
				Thread.sleep(000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		camera.startPreview();
    		
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
						camera.setPreviewCallbackWithBuffer(MainScreen.thiz);
						camera.addCallbackBuffer(MainScreen.thiz.pviewBuffer);
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
		else if (command == PluginManager.MSG_BAD_FRAME)
		{
			Toast.makeText(
					MainScreen.thiz,
					MainScreen.thiz.getResources().getString(R.string.plugin_capture_panoramaaugmented_badframe),
					Toast.LENGTH_SHORT).show();
			return true;
		}
		else if (command == PluginManager.MSG_FORCE_FINISH_CAPTURE)
		{
			this.capturing = false;
			this.stopCapture();
			
			return true;
		}

		return false;
	}
	
	private void getPrefs()
    {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        
		try
		{
			this.prefResolution = Integer.parseInt(prefs.getString(PREFERENCES_KEY_RESOLUTION, "0"));
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			Log.e("Panorama", "getPrefs exception: " + e.getMessage());
			this.prefResolution = 0;
		}
        final int focusPref = Integer.parseInt(prefs.getString(PREFERENCES_KEY_FOCUS, "0"));
        if (focusPref == 0)
		{
			this.prefFocusContinuous = false;
			this.prefAutofocus = true;
			this.prefRefocus = true;
		}
		else if (focusPref == 1)
		{
			this.prefFocusContinuous = false;
			this.prefAutofocus = true;
			this.prefRefocus = false;
		}
		else if (focusPref == 2)
		{
			this.prefFocusContinuous = false;
			this.prefAutofocus = false;
			this.prefRefocus = false;
		}
		else if (focusPref == 3)
		{
			this.prefFocusContinuous = true;
			this.prefAutofocus = false;
			this.prefRefocus = false;
		}
        this.prefHardwareGyroscope = prefs.getBoolean(PREFERENCES_KEY_USE_DEVICE_GYRO, this.sensorGyroscope != null);
    }
	
	private void createPrefs(final ListPreference lp, final Preference ud_pref)
	{

		final CharSequence[] entries;
		final CharSequence[] entryValues;
		
		if (ResolutionsPictureIdxesList != null)
        {
	        entries = ResolutionsPictureNamesList.toArray(new CharSequence[ResolutionsPictureNamesList.size()]);
	        entryValues = ResolutionsPictureIdxesList.toArray(new CharSequence[ResolutionsPictureIdxesList.size()]);

	        if(lp != null)
	        {
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
		
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
		final Parameters cp = MainScreen.thiz.getCameraParameters();
		final List<Camera.Size> cs = cp.getSupportedPictureSizes();   
        
        int maxIndex = 0;

        if (cs!=null)
        {
	    	for (int ii=0; ii<cs.size(); ++ii)
	    	{
	            final Size s = cs.get(ii); 
	
	            if (s.width > cs.get(maxIndex).width)
	            {
	            	maxIndex = ii;
	            }
	
	            if ((long)s.width >= MIN_HEIGHT_SUPPORTED)
	            {
	            	// find good location in a list
	            	int loc;
	            	boolean shouldInsert = true;
	            	for (loc = 0; loc < ResolutionsPictureSizesList.size(); ++loc)
	            	{
	            		final Point psize = ResolutionsPictureSizesList.get(loc);
	            		if (psize.x == s.width)
	            		{
	            			if (s.height > psize.y)
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
	            		else if (psize.x < s.width)
	            		{
	            			break;
	            		}
	            	}
	            	
	            	if (shouldInsert)
	            	{
		            	ResolutionsPictureNamesList.add(loc, String.format("%dpx", s.width));
		            	ResolutionsPictureIdxesList.add(loc, String.format("%d", ii));
		            	ResolutionsPictureSizesList.add(loc, new Point(s.width, s.height));
	            	}
	            }
	        }
	    	
	    	if (ResolutionsPictureIdxesList.size() == 0)
	    	{
	            final Size s = cs.get(maxIndex); 
	    		
	            ResolutionsPictureNamesList.add(String.format("%dpx", s.width));
	            ResolutionsPictureIdxesList.add(String.format("%d", maxIndex));
	            ResolutionsPictureSizesList.add(new Point(s.width, s.height));
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
		final int frames_fit_count = 
				(int)(getAmountOfMemoryToFitFrames() / getFrameSizeInBytes(this.pictureWidth, this.pictureHeight));
		this.engine.setMaxFrames(frames_fit_count);
		
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
		
		final int state = this.engine.getPictureTakingState(this.prefRefocus);
		
		if (state == AugmentedPanoramaEngine.STATE_TAKINGPICTURE
				&& this.takingAlready.compareAndSet(false, true))
		{
			MainScreen.H.sendEmptyMessage(PluginManager.MSG_TAKE_PICTURE);
		}
	}
	
	private final AutoFocusCallback autoFocusCallbackReceiver = new AutoFocusCallback()
	{
		@Override
		public void onAutoFocus(final boolean success, final Camera camera)
		{
			PanoramaAugmentedCapturePlugin.this.takePictureReal();
		}
	};
	
	private void tryAutoFocus()
	{
		try
		{
			final Camera camera = MainScreen.thiz.getCamera();
	    	if (camera == null)
	    	{
	    		Log.e("Almalence", "tryAutoFocus(): camera is null");
	    		return;
	    	}
			camera.autoFocus(this.autoFocusCallbackReceiver);
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			this.takePicture();
		}
	}
	
	@Override
	public void onAutoFocus(final boolean success, final Camera camera)
	{
		
	}
	
	@Override
	public void takePicture()
	{		
		try
		{
			if (this.prefAutofocus)
			{
				this.tryAutoFocus();
			}
			else
			{
				this.takePictureReal();
			}
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}
	}

	private void takePictureReal()
	{
		Camera camera = MainScreen.thiz.getCamera();
    	if (camera == null)
    	{
    		this.takingAlready.set(false);
    		Log.e("Almalence", "takePicture(): camera is null");
    		return;
    	}
    	
    	this.coordsRecorded = false;
    	
		camera.takePicture(MainScreen.thiz, null, null, MainScreen.thiz);
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
		if (!this.coordsRecorded)
		{
			this.engine.recordCoordinates();
		}
		
		final boolean goodPlace = this.engine.onPictureTaken(paramArrayOfByte);
		this.takingAlready.set(false);
		
		if (isFirstFrame)
		{
			PluginManager.getInstance().addToSharedMem_ExifTagsFromJPEG(paramArrayOfByte);
			isFirstFrame = false;
		}
		
		final boolean done = this.engine.isCircular();

		{
			final Message msg = new Message();
			msg.arg1 = PluginManager.MSG_NEXT_FRAME;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessageDelayed(msg, 200);
		}
		
		if (!goodPlace)
		{
			final Message msg = new Message();
			msg.arg1 = PluginManager.MSG_BAD_FRAME;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);
		}
		
		if (done)
		{
			final Message msg = new Message();
			msg.arg1 = PluginManager.MSG_FORCE_FINISH_CAPTURE;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);
		}
	}
	
	@SuppressLint("FloatMath")
	private void stopCapture()
	{		
		final LinkedList<AugmentedFrameTaken> frames = this.engine.retrieveFrames();
		
		if (frames.size() > 0)
		{
			PluginManager.getInstance().addToSharedMem("frameorientation" + String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(MainScreen.guiManager.getDisplayOrientation()));
			PluginManager.getInstance().addToSharedMem("pano_mirror" + String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(MainScreen.getCameraMirrored()));
			PluginManager.getInstance().addToSharedMem("pano_width"+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(this.pictureHeight));
			PluginManager.getInstance().addToSharedMem("pano_height"+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(this.pictureWidth));
			PluginManager.getInstance().addToSharedMem("pano_frames_count"+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(frames.size()));
			PluginManager.getInstance().addToSharedMem("pano_camera_fov"+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf((int)(this.viewAngleY + 0.5f)));
			PluginManager.getInstance().addToSharedMem("pano_useall"+String.valueOf(PluginManager.getInstance().getSessionID()), "1");
			PluginManager.getInstance().addToSharedMem("pano_freeinput"+String.valueOf(PluginManager.getInstance().getSessionID()), "0");
			
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
				
				PluginManager.getInstance().addToSharedMem("pano_frame"+(frame_cursor+1)+"."+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(frame.getNV21address()));

				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".00."+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(FloatMath.cos(angleR)));
				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".01."+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(-FloatMath.sin(angleR)));
				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".02."+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(PixelsShiftX));
				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".10."+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(FloatMath.sin(angleR)));
				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".11."+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(FloatMath.cos(angleR)));
				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".12."+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(PixelsShiftY));
				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".20."+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(0.0f));
				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".21."+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(0.0f));
				PluginManager.getInstance().addToSharedMem("pano_frametrs"+(frame_cursor+1)+".22."+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(1.0f));
				
				if (!iterator.hasNext()) break;
				
				frame = iterator.next();
				
				frame_cursor++;
			}

	    	MainScreen.H.sendEmptyMessage(PluginManager.MSG_CAPTURE_FINISHED);
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
