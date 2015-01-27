package com.almalence.plugins.vf.gyro;

import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.almalence.plugins.capture.panoramaaugmented.AugmentedRotationListener;
import com.almalence.plugins.capture.panoramaaugmented.VfGyroSensor;
import com.almalence.ui.RotateImageView;

/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginViewfinder;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginViewfinder;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

public class GyroVFPlugin extends PluginViewfinder
{

	private static final Boolean		ON						= true;
	private static final Boolean		OFF						= false;

	private int							mOrientation;
	private Boolean						mGyroState;
	private SensorManager				mSensorManager;
	private Sensor						mMagnetometer;
	private Sensor						mAccelerometer;
	private Sensor						mGyroscope;
	private VfGyroSensor				mVfGyroscope;
	private AugmentedRotationListener	mAugmentedListener;
	private AugmentedSurfaceView		mSurfacePreviewAugmented;

	private static Handler				handler					= new Handler();

	private float						viewAngleX				= 55.4f;
	private float						viewAngleY				= 42.7f;

	private int							pictureWidth;
	private int							pictureHeight;

	private int							previewWidth;
	private int							previewHeight;

	private boolean						remapOrientation;
	private boolean						mPrefHardwareGyroscope	= true;

	private RelativeLayout				mHorizonIndicatorContainer;
	private RelativeLayout				mHorizonIndicatorMarkContainer;
	private RotateImageView				mHorizonIndicatorMarkRotation;
	private RotateImageView				mHorizonIndicatorMarkHorizontal;
	private RotateImageView				mHorizonIndicatorMarkTopDown;
	private RotateImageView				mHorizonIndicatorAim;
	private RotateImageView				mHorizonIndicatorAimTopDown;
	private View						mHorizonLayout;
	private Boolean						flat					= false;

	public GyroVFPlugin()
	{
		super("com.almalence.plugins.gyrovf", R.xml.preferences_vf_gyro, 0, R.drawable.gui_almalence_settings_gyro,
				MainScreen.getAppResources().getString(R.string.Pref_TitleGyroVF));
	}

	@Override
	public void onCameraParametersSetup()
	{
		this.checkCoordinatesRemapRequired();

		final Camera.Parameters cp = CameraController.getCameraParameters();
		if (cp == null)
		{
			return;
		}
		this.pictureWidth = cp.getPictureSize().width;
		this.pictureHeight = cp.getPictureSize().height;

		this.previewWidth = cp.getPreviewSize().width;
		this.previewHeight = cp.getPreviewSize().height;

		try
		{
			this.viewAngleX = cp.getHorizontalViewAngle();
			this.viewAngleY = cp.getVerticalViewAngle();
		} catch (final Exception e)
		{
			// Some bugged camera drivers pop ridiculous exception here, use
			// typical view angles then
			this.viewAngleX = 55.4f;
			this.viewAngleY = 42.7f;
		}

		// some devices report incorrect FOV values, use typical view angles
		// then
		if (this.viewAngleX >= 150)
		{
			this.viewAngleX = 55.4f;
			this.viewAngleY = 42.7f;
		}

		// Some cameras report incorrect view angles
		// Usually vertical view angle is incorrect, but eg Htc One report
		// incorrect horizontal view angle
		// If aspect ratio from FOV differs by more than 10% from aspect ratio
		// from W/H
		// - re-compute view angle
		float HorizontalViewFromAspect = 2
				* 180
				* (float) Math.atan((float) this.pictureWidth / (float) this.pictureHeight
						* (float) Math.tan((float) Math.PI * this.viewAngleY / (2 * 180))) / (float) Math.PI;
		float VerticalViewFromAspect = 2
				* 180
				* (float) Math.atan((float) this.pictureHeight / (float) this.pictureWidth
						* (float) Math.tan((float) Math.PI * this.viewAngleX / (2 * 180))) / (float) Math.PI;
		// not expecting very narrow field of view
		if ((VerticalViewFromAspect > 40.f) && (VerticalViewFromAspect < 0.9f * this.viewAngleY))
			this.viewAngleY = VerticalViewFromAspect;
		else if ((HorizontalViewFromAspect < 0.9f * this.viewAngleX)
				|| (HorizontalViewFromAspect > 1.1f * this.viewAngleX))
			this.viewAngleX = HorizontalViewFromAspect;

		mSurfacePreviewAugmented.reset(this.pictureHeight, this.pictureWidth, this.viewAngleY);

		if (!mPrefHardwareGyroscope)
		{
			mVfGyroscope.SetFrameParameters(this.previewWidth, this.previewHeight, this.viewAngleX, this.viewAngleY);
		}
	}

	private void checkCoordinatesRemapRequired()
	{
		final Display display = ((WindowManager) MainScreen.getInstance().getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		// This is proved way of checking it so we better use deprecated
		// methods.
		@SuppressWarnings("deprecation")
		final int orientation = (display.getWidth() <= display.getHeight()) ? Configuration.ORIENTATION_PORTRAIT
				: Configuration.ORIENTATION_LANDSCAPE;
		final int rotation = display.getRotation();
		this.remapOrientation = (orientation == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_0)
				|| (orientation == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_180)
				|| (orientation == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_90)
				|| (orientation == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_270);
	}

	@Override
	public void onResume()
	{
		mSensorManager = (SensorManager) MainScreen.getInstance().getSystemService(Context.SENSOR_SERVICE);
		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		//to see if it helps to fix "can't load library error"
		try
		{
			mVfGyroscope = new VfGyroSensor(null);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		mSurfacePreviewAugmented = new AugmentedSurfaceView(this);
		updatePreferences();
	}

	void updatePreferences()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		mGyroState = prefs.getBoolean("PrefGyroVF", false);

		if (!prefs.contains("PrefGyroTypeVF"))
		{
			Editor editor = prefs.edit();
			if (mGyroscope != null)
			{
				editor.putBoolean("PrefGyroTypeVF", true);
			} else
			{
				editor.putBoolean("PrefGyroTypeVF", false);
			}
			editor.commit();
		}
		mPrefHardwareGyroscope = prefs.getBoolean("PrefGyroTypeVF", false);

		if (mGyroState == ON)
		{
			quickControlIconID = R.drawable.gui_almalence_settings_gyro;
			if (mHorizonIndicatorContainer != null)
			{
				mHorizonIndicatorContainer.setVisibility(View.VISIBLE);
			}
			initSensors();
		} else
		{
			quickControlIconID = R.drawable.gui_almalence_settings_gyro_off;
			if (mHorizonIndicatorContainer != null)
			{
				mHorizonIndicatorContainer.setVisibility(View.GONE);
			}
			releaseSensors();
		}
	}

	@Override
	public void onPreviewFrame(byte[] data)
	{
		if (mGyroState == OFF)
			return;

		if (!this.mPrefHardwareGyroscope)
		{
			this.mVfGyroscope.NewData(data);
		}

	}

	@Override
	public void onOrientationChanged(int orientation)
	{
		mOrientation = orientation;
		if (mHorizonIndicatorMarkHorizontal != null)
		{
			mHorizonIndicatorMarkHorizontal.setRotation(orientation - 90);
		}
	}

	@Override
	public void onQuickControlClick()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		Editor editor = prefs.edit();

		if (mGyroState == ON)
		{
			quickControlIconID = R.drawable.gui_almalence_settings_gyro_off;
			editor.putBoolean("PrefGyroVF", false);
		} else
		{
			quickControlIconID = R.drawable.gui_almalence_settings_gyro;
			editor.putBoolean("PrefGyroVF", true);
		}
		editor.commit();

		updatePreferences();
	}

	@Override
	public void onPause()
	{
		releaseSensors();
	}

	private void updatehHardwareGyro()
	{
		if (mGyroState == OFF)
			return;

		if (mSurfacePreviewAugmented != null)
		{
			mSurfacePreviewAugmented.onDrawFrame();
		}
		handler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				updatehHardwareGyro();
			}
		}, 50);
	}

	private void initSensors()
	{
		if (mGyroState == ON)
		{
			if (mSensorManager == null)
			{
				mSensorManager = (SensorManager) MainScreen.getInstance().getSystemService(Context.SENSOR_SERVICE);
			}
			if (mSensorManager == null)
			{
				return;
			}

			mSurfacePreviewAugmented.reset(this.pictureHeight, this.pictureWidth, this.viewAngleY);

			mAugmentedListener = new AugmentedRotationListener(remapOrientation, !mPrefHardwareGyroscope);

			if (this.mGyroscope != null)
			{
				if (mPrefHardwareGyroscope)
				{
					mSensorManager.registerListener(mAugmentedListener, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
					updatehHardwareGyro();
					handler.postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							updatehHardwareGyro();
						}
					}, 500);
				}
			}

			if (!mPrefHardwareGyroscope)
			{
				if (mVfGyroscope == null)
				{
					mVfGyroscope = new VfGyroSensor(null);
				}
				mVfGyroscope.open();
				mVfGyroscope.SetListener(mAugmentedListener);
			}

			mSensorManager.registerListener(mAugmentedListener, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
			mSensorManager.registerListener(mAugmentedListener, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);

			mAugmentedListener.setReceiver(mSurfacePreviewAugmented);
		}
	}

	private void releaseSensors()
	{
		mGyroState = OFF;
		if (mPrefHardwareGyroscope)
		{
			if (mSensorManager != null)
				mSensorManager.unregisterListener(mAugmentedListener, mGyroscope);
		} else
		{
			if (null != mVfGyroscope)
				mVfGyroscope.SetListener(null);
		}

		if (mSensorManager != null)
		{
			mSensorManager.unregisterListener(mAugmentedListener, mAccelerometer);
			mSensorManager.unregisterListener(mAugmentedListener, mMagnetometer);
		}
	}

	@Override
	public void onGUICreate()
	{
		createGyroUI();

		if (mGyroState == ON)
		{
			if (mHorizonIndicatorContainer != null)
			{
				mHorizonIndicatorContainer.setVisibility(View.VISIBLE);
			}
		} else
		{
			if (mHorizonIndicatorContainer != null)
			{
				mHorizonIndicatorContainer.setVisibility(View.GONE);
			}
		}
	}

	private AtomicBoolean	horizon_updating	= new AtomicBoolean(false);

	public void updateHorizonIndicator(float verticalError, float horizontalError, final float sideErrorVertical,
			final float sideErrorHorizontal)
	{

		if (mHorizonIndicatorContainer == null || mHorizonIndicatorContainer == null
				|| mHorizonIndicatorMarkTopDown == null || mHorizonIndicatorMarkRotation == null
				|| mHorizonIndicatorMarkHorizontal == null || mHorizonIndicatorAim == null
				|| mHorizonIndicatorAimTopDown == null || mHorizonIndicatorMarkContainer == null)
		{
			return;
		}

		if (MainScreen.getGUIManager().lockControls)
		{
			mHorizonIndicatorContainer.setVisibility(View.GONE);
			return;
		}

		mHorizonIndicatorContainer.setVisibility(View.VISIBLE);

		if (!horizon_updating.compareAndSet(false, true))
		{
			return;
		}

		if (Math.abs(horizontalError) <= 0.01f && Math.abs(verticalError) <= 0.01f)
		{
			mHorizonIndicatorMarkContainer.setPadding(0, 0, 0, 0);
			if (!flat)
			{
				rotateHorizonIndicator(sideErrorVertical, sideErrorHorizontal);
			}

			int color = 255;
			mHorizonIndicatorMarkTopDown.setColorFilter(Color.rgb(color, color, 0), Mode.MULTIPLY);
			mHorizonIndicatorMarkRotation.setColorFilter(Color.rgb(color, color, 0), Mode.MULTIPLY);

			horizon_updating.set(false);
			return;
		}

		float density = MainScreen.getAppResources().getDisplayMetrics().density;
		if ((Math.abs(horizontalError) > 1.0f && (mOrientation == 0 || mOrientation == 180 || flat))
				|| (Math.abs(verticalError) > 1.0f && (mOrientation == 90 || mOrientation == 270 || flat)))
		{
			flat = true;
			mHorizonIndicatorMarkRotation.setVisibility(View.GONE);
			mHorizonIndicatorMarkHorizontal.setVisibility(View.GONE);
			mHorizonIndicatorMarkTopDown.setVisibility(View.VISIBLE);
			mHorizonIndicatorAim.setVisibility(View.GONE);
			mHorizonIndicatorAimTopDown.setVisibility(View.VISIBLE);

			if (Math.abs(verticalError) > 0.9f)
			{
				if (verticalError > 0.0f)
				{
					verticalError = (float) (verticalError - Math.PI / 2);
				} else
				{
					verticalError = (float) (verticalError + Math.PI / 2);
				}
			}

			if (Math.abs(horizontalError) > 0.9f)
			{
				if (horizontalError > 0.0f)
				{
					horizontalError = (float) (horizontalError - Math.PI / 2);
				} else
				{
					horizontalError = (float) (horizontalError + Math.PI / 2);
				}
			}

			final int marginVerticalValue = (int) (300.0f * Math.abs(verticalError) * density);
			final int marginHorizontalValue = (int) (300.0f * Math.abs(horizontalError) * density);

			int color = (255 - (marginVerticalValue + marginHorizontalValue) * 4);
			if (color > 50)
			{
				mHorizonIndicatorMarkTopDown.setColorFilter(Color.rgb(color, color, 0), Mode.MULTIPLY);
			} else
			{
				mHorizonIndicatorMarkTopDown.clearColorFilter();
			}

			if (verticalError < 0.0f)
			{
				if (horizontalError < 0.0f)
				{
					mHorizonIndicatorMarkContainer.setPadding(0, marginVerticalValue, marginHorizontalValue, 0);
				} else
				{
					mHorizonIndicatorMarkContainer.setPadding(marginHorizontalValue, marginVerticalValue, 0, 0);
				}
			} else
			{
				if (horizontalError < 0.0f)
				{
					mHorizonIndicatorMarkContainer.setPadding(0, 0, marginHorizontalValue, marginVerticalValue);
				} else
				{
					mHorizonIndicatorMarkContainer.setPadding(marginHorizontalValue, 0, 0, marginVerticalValue);
				}
			}
			horizon_updating.set(false);
		} else
		{
			flat = false;
			mHorizonIndicatorMarkRotation.setVisibility(View.VISIBLE);
			mHorizonIndicatorMarkHorizontal.setVisibility(View.VISIBLE);
			mHorizonIndicatorMarkTopDown.setVisibility(View.GONE);
			mHorizonIndicatorAim.setVisibility(View.VISIBLE);
			mHorizonIndicatorAimTopDown.setVisibility(View.GONE);
			final int marginVerticalValue = (int) (300.0f * Math.abs(verticalError) * density);
			final int marginHorizontalValue = (int) (300.0f * Math.abs(horizontalError) * density);

			rotateHorizonIndicator(sideErrorVertical, sideErrorHorizontal);

			if (mOrientation == 90 || mOrientation == 270)
			{
				if (verticalError < 0.0f)
				{
					mHorizonIndicatorMarkContainer.setPadding(0, marginVerticalValue, 0, 0);
				} else
				{
					mHorizonIndicatorMarkContainer.setPadding(0, 0, 0, marginVerticalValue);
				}
			} else
			{
				if (horizontalError < 0.0f)
				{
					mHorizonIndicatorMarkContainer.setPadding(0, 0, marginHorizontalValue, 0);
				} else
				{
					mHorizonIndicatorMarkContainer.setPadding(marginHorizontalValue, 0, 0, 0);
				}
			}
			horizon_updating.set(false);
		}
	}

	private void rotateHorizonIndicator(float sideErrorVertical, float sideErrorHorizontal)
	{
		mHorizonIndicatorAim.setOrientation(mOrientation - 90);

		int colorVert = (255 - Math.abs(((int) Math.toDegrees(sideErrorVertical) - 90) % 90) * 15);
		int colorHorz = (255 - Math.abs(((int) Math.toDegrees(sideErrorHorizontal) - 90) % 90) * 15);

		if (mOrientation == 90 || mOrientation == 270)
		{
			if (colorVert > 100)
			{
				mHorizonIndicatorMarkRotation.setColorFilter(Color.rgb(colorVert, colorVert, 0), Mode.MULTIPLY);
			} else
			{
				mHorizonIndicatorMarkRotation.clearColorFilter();
			}
		} else
		{
			if (colorHorz > 100)
			{
				mHorizonIndicatorMarkRotation.setColorFilter(Color.rgb(colorHorz, colorHorz, 0), Mode.MULTIPLY);
			} else
			{
				mHorizonIndicatorMarkRotation.clearColorFilter();
			}
		}

		if (mOrientation == 90)
		{
			mHorizonIndicatorMarkRotation.setOrientation((int) Math.toDegrees(sideErrorVertical) + 180 + mOrientation);
		}

		if (mOrientation == 270)
		{
			mHorizonIndicatorMarkRotation.setOrientation(-(int) Math.toDegrees(sideErrorVertical) + mOrientation);
		}

		if (mOrientation == 180)
		{
			mHorizonIndicatorMarkRotation
					.setOrientation((int) Math.toDegrees(sideErrorHorizontal) + 180 + mOrientation);
		}

		if (mOrientation == 0)
		{
			mHorizonIndicatorMarkRotation.setOrientation(-(int) Math.toDegrees(sideErrorHorizontal) + mOrientation);
		}
	}

	private void createGyroUI()
	{
		LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
		mHorizonLayout = inflator.inflate(R.layout.plugin_vf_gyro_layout, null, false);
		mHorizonLayout.setVisibility(View.VISIBLE);

		MainScreen.getGUIManager().removeViews(mHorizonLayout, R.id.specialPluginsLayout);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout)).addView(mHorizonLayout,
				params);
		// mHorizonLayout.requestLayout();
		// ((RelativeLayout)
		// MainScreen.getInstance().findViewById(R.id.specialPluginsLayout)).requestLayout();

		mHorizonIndicatorAim = (RotateImageView) mHorizonLayout.findViewById(R.id.horizon_indicator_aim);
		mHorizonIndicatorAimTopDown = (RotateImageView) mHorizonLayout
				.findViewById(R.id.horizon_indicator_aim_top_down);
		mHorizonIndicatorMarkRotation = (RotateImageView) mHorizonLayout
				.findViewById(R.id.horizon_indicator_mark_rotation);
		mHorizonIndicatorMarkHorizontal = (RotateImageView) mHorizonLayout
				.findViewById(R.id.horizon_indicator_mark_horizontal);
		mHorizonIndicatorMarkTopDown = (RotateImageView) mHorizonLayout
				.findViewById(R.id.horizon_indicator_mark_top_down);
		mHorizonIndicatorContainer = (RelativeLayout) mHorizonLayout.findViewById(R.id.horizon_indicator_container);
		mHorizonIndicatorMarkContainer = (RelativeLayout) mHorizonLayout
				.findViewById(R.id.horizon_indicator_mark_container);
	}
}
