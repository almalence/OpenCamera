package com.almalence.plugins.vf.gyro;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginViewfinder;
import com.almalence.opencam_plus.R;
+++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginViewfinder;
import com.almalence.opencam.R;
//-+- -->
import com.almalence.plugins.capture.panoramaaugmented.AugmentedRotationListener;
import com.almalence.plugins.capture.panoramaaugmented.VfGyroSensor;
import com.almalence.ui.RotateImageView;

public class GyroVFPlugin extends PluginViewfinder {
    
	private final static Boolean ON = true;
	private final static Boolean OFF = false;
	
	private int mOrientation;
	private Boolean mGyroState;
	private boolean mUseDeviceGyro = true;
	private SensorManager mSensorManager;
	private Sensor mMagnetometer;
	private Sensor mAccelerometer;
	private Sensor mGyroscope;
	private VfGyroSensor mVfGyroscope;
	private AugmentedRotationListener mAugmentedListener;
	private VerticalListener verticalListener;
	private AugmentedSurfaceView mSurfacePreviewAugmented;
	
	private float viewAngleX = 55.4f;
	private float viewAngleY = 42.7f;
	
	private int pictureWidth;
	private int pictureHeight;
	
	private boolean remapOrientation;
	private boolean mPrefHardwareGyroscope = true;
	
	private RelativeLayout mHorizonIndicatorContainer;
	private LinearLayout mHorizonIndicatorMarkContainer;
	private RotateImageView mHorizonIndicatorMark;
	private View mHorizonLayout;
	
	  
	public GyroVFPlugin() {
		super("com.almalence.plugins.gyrovf",
			  R.xml.preferences_vf_gyro,
			  0,
			  R.drawable.gui_almalence_settings_scene_barcode_on,
			  "Gyro");
		
		mSensorManager = (SensorManager) MainScreen.thiz.getSystemService(Context.SENSOR_SERVICE);
		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mVfGyroscope = new VfGyroSensor(null);
		mSurfacePreviewAugmented = new AugmentedSurfaceView(this);
		verticalListener = new VerticalListener();
	}
	
	@Override
	public void onCameraParametersSetup() {
		this.checkCoordinatesRemapRequired();
		
		final Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
		if (cp == null) {
			return;
		}
		this.pictureWidth = cp.getPictureSize().width;
		this.pictureHeight = cp.getPictureSize().height;
    	

		try {
			this.viewAngleX = cp.getHorizontalViewAngle();
			this.viewAngleY = cp.getVerticalViewAngle();
		}
		catch (final Throwable e) {
			// Some bugged camera drivers pop ridiculous exception here, use typical view angles then 
			this.viewAngleX = 55.4f;
			this.viewAngleY = 42.7f;
		}

		// some devices report incorrect FOV values, use typical view angles then
		if (this.viewAngleX >= 150) {
			this.viewAngleX = 55.4f;
			this.viewAngleY = 42.7f;
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

		mSurfacePreviewAugmented.reset(this.pictureHeight, this.pictureWidth, this.viewAngleY);
	}

	
	
	private void checkCoordinatesRemapRequired() {
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
	
	@Override
	public void onResume() {
		updatePreferences();
	}
	
	void updatePreferences() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);
		mGyroState = prefs.getBoolean("PrefGyroVF", false);
		mPrefHardwareGyroscope = prefs.getBoolean("PrefGyroTypeVF", true);
		
		if (mGyroState == ON) {
			quickControlIconID = R.drawable.gui_almalence_settings_scene_barcode_on;
			if (mHorizonIndicatorContainer != null) {
				mHorizonIndicatorContainer.setVisibility(View.VISIBLE);
			}
			initSensors();
		} else {
			quickControlIconID = R.drawable.gui_almalence_settings_off_barcode_scanner;
			if (mHorizonIndicatorContainer != null) {
				mHorizonIndicatorContainer.setVisibility(View.GONE);
			}
			releaseSensors();
		}
		
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera paramCamera) {
		if (mGyroState == OFF)
			return;
		
		if (mSurfacePreviewAugmented != null) {
			mSurfacePreviewAugmented.onDrawFrame();
		}
	}
	
	@Override
    public void onOrientationChanged(int orientation) {
		mOrientation = orientation;
    }
	
	@Override
	public void onQuickControlClick() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);
		Editor editor = prefs.edit();
		
		if (mGyroState == ON) {
			quickControlIconID = R.drawable.gui_almalence_settings_off_barcode_scanner;
        	editor.putBoolean("PrefGyroVF", false);
		} else {
			quickControlIconID = R.drawable.gui_almalence_settings_scene_barcode_on;
        	editor.putBoolean("PrefGyroVF", true);
		}
        editor.commit();
        
        updatePreferences();
	}
	
	@Override
	public void onPause() {
		releaseSensors();
	}
	
	private void initSensors() {
		mSensorManager.registerListener(verticalListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		
		if (mGyroState == ON) {	
			mSurfacePreviewAugmented.reset(this.pictureHeight, this.pictureWidth, this.viewAngleY);
			
			mAugmentedListener = new AugmentedRotationListener(remapOrientation,mPrefHardwareGyroscope);
			
			if (this.mGyroscope != null) {
				if (mPrefHardwareGyroscope) {
					mSensorManager.registerListener(mAugmentedListener, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
				}
			}
			
			if (!mPrefHardwareGyroscope) {
				mVfGyroscope.open();
				mVfGyroscope.SetListener(mAugmentedListener);
			}
			
			mSensorManager.registerListener(mAugmentedListener, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
			mSensorManager.registerListener(mAugmentedListener, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
			
			mAugmentedListener.setReceiver(mSurfacePreviewAugmented);
		}
	}
	
	private void releaseSensors() {
		if (mPrefHardwareGyroscope) {
			mSensorManager.unregisterListener(mAugmentedListener, mGyroscope);
		}
		else {
			if (null != mVfGyroscope)
				mVfGyroscope.SetListener(null);
		}
		
		mSensorManager.unregisterListener(mAugmentedListener, mAccelerometer);
		mSensorManager.unregisterListener(mAugmentedListener, mMagnetometer);
	}
	
	@Override
	public void onGUICreate() {
		createGyroUI();
		
		if (mGyroState == ON) {
			if (mHorizonIndicatorContainer != null) {
				mHorizonIndicatorContainer.setVisibility(View.VISIBLE);
			}
		} else {
			if (mHorizonIndicatorContainer != null) {
				mHorizonIndicatorContainer.setVisibility(View.GONE);
			}
		}
	}
	
	private AtomicBoolean horizon_updating = new AtomicBoolean(false);
	public void updateHorizonIndicator(final float error, final float sideError) {
		if (!horizon_updating.compareAndSet(false, true)) {
			return;
		}	
		final int marginValue = (int)(300.0f * Math.abs(error) * MainScreen.thiz.getResources().getDisplayMetrics().density);
		MainScreen.thiz.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (sideError == Float.POSITIVE_INFINITY) {
					mHorizonIndicatorMark.setOrientation(mOrientation - 90);
				}
				else {
					mHorizonIndicatorMark.setOrientation((int)Math.toDegrees(sideError));
				}	

				if (mOrientation == 90 || mOrientation == 270) {
					if (error < 0.0f) {
						mHorizonIndicatorMarkContainer.setPadding(0, marginValue, 0, 0);
					}
					else {
						mHorizonIndicatorMarkContainer.setPadding(0, 0, 0, marginValue);
					}
				}
				else {
					mHorizonIndicatorMarkContainer.setPadding(0, 0, 0, 0);
				}
				horizon_updating.set(false);
			}
		});
	}
	
	private void createGyroUI() {
		LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();
		mHorizonLayout = inflator.inflate(R.layout.plugin_vf_gyro_layout, null, false);
		mHorizonLayout.setVisibility(View.VISIBLE);
		
		List<View> specialView = new ArrayList<View>();
		RelativeLayout specialLayout = (RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout);
		for(int i = 0; i < specialLayout.getChildCount(); i++)
			specialView.add(specialLayout.getChildAt(i));

		for(int j = 0; j < specialView.size(); j++)
		{
			View view = specialView.get(j);
			int view_id = view.getId();
			int layout_id = mHorizonLayout.getId();
			if(view_id == layout_id)
			{
				if(view.getParent() != null)
					((ViewGroup)view.getParent()).removeView(view);
				
				specialLayout.removeView(view);
			}
		}
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout)).addView(mHorizonLayout, params);
		mHorizonLayout.requestLayout();
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout)).requestLayout();
		
		mHorizonIndicatorMark = (RotateImageView) mHorizonLayout.findViewById(R.id.horizon_indicator_mark);
		mHorizonIndicatorContainer = (RelativeLayout) mHorizonLayout.findViewById(R.id.horizon_indicator_container);
		mHorizonIndicatorMarkContainer = (LinearLayout) mHorizonLayout.findViewById(R.id.horizon_indicator_mark_container);
	}
}
