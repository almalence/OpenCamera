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
 package com.almalence.opencam_plus;
 +++ --> */
// <!-- -+-
package com.almalence.opencam;

//-+- -->

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.CountDownTimer;
import android.os.Debug;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.ui.TemplateGUI;
import com.almalence.opencam.ui.GLLayer;
import com.almalence.opencam.ui.GUI;

/***
 * MainScreen - main activity screen with camera functionality
 * 
 * Passes all main events to PluginManager
 ***/

@SuppressWarnings("deprecation")
//public class MainScreen extends Activity implements ApplicationInterface, View.OnClickListener, View.OnTouchListener,
//		SurfaceHolder.Callback, Handler.Callback, Camera.ShutterCallback
public class TemplateScreen extends ApplicationScreen
{
	// >>Description
	// section with different global parameters available for everyone
	//
	// Camera parameters and possibly access to camera instance
	//
	// Global defines and others
	//
	// Description<<

	private static final int			MIN_MPIX_PREVIEW				= 600 * 400;
	
	public static TemplateScreen			thiz;

	// Interface to HALv3 camera and Old style camera

	// HALv3 camera's objects
	private ImageReader					mImageReaderPreviewYUV;
	private ImageReader					mImageReaderYUV;
	private ImageReader					mImageReaderJPEG;
	private ImageReader					mImageReaderRAW;

	private File						forceFilename					= null;
	private Uri							forceFilenameUri;

	// Common preferences
	private int							imageSizeIdxPreference;

	public static String				sInitModeListPref				= "initModeListPref";

	@Override
	protected void duringOnCreate()
	{
		thiz = this;
	}
	
	protected void createPluginManager()
	{
		pluginManager = TemplatePluginManager.getInstance();
	}
	
	@Override
	protected void afterOnCreate()
	{
		ApplicationScreen.getPluginManager().setupDefaultMode();
		// init gui manager
		guiManager = new TemplateGUI();
		guiManager.createInitialGUI();
		this.findViewById(R.id.mainLayout1).invalidate();
		this.findViewById(R.id.mainLayout1).requestLayout();
		guiManager.onCreate();

		// init plugin manager
		ApplicationScreen.getPluginManager().onCreate();
		
		if (this.getIntent().getAction() != null)
		{
			if (this.getIntent().getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE))
			{
				try
				{
					forceFilenameUri = this.getIntent().getExtras().getParcelable(MediaStore.EXTRA_OUTPUT);
					TemplateScreen.setForceFilename(new File(((Uri) forceFilenameUri).getPath()));
					if (TemplateScreen.getForceFilename().getAbsolutePath().equals("/scrapSpace"))
					{
						TemplateScreen.setForceFilename(new File(Environment.getExternalStorageDirectory()
								.getAbsolutePath() + "/mms/scrapSpace/.temp.jpg"));
						new File(TemplateScreen.getForceFilename().getParent()).mkdirs();
					}
				} catch (Exception e)
				{
					TemplateScreen.setForceFilename(null);
				}
			} else
			{
				TemplateScreen.setForceFilename(null);
			}
		} else
		{
			TemplateScreen.setForceFilename(null);
		}
	}
	
	
	public static TemplateScreen getInstance()
	{
		return thiz;
	}


	@TargetApi(21)
	@Override
	public void createImageReaders(ImageReader.OnImageAvailableListener imageAvailableListener)
	{
		Log.e("MainScreen", "createImageReaders");
		// ImageReader for preview frames in YUV format
		mImageReaderPreviewYUV = ImageReader.newInstance(thiz.previewWidth, thiz.previewHeight,
				ImageFormat.YUV_420_888, 2);
		// thiz.mImageReaderPreviewYUV = ImageReader.newInstance(1280, 960,
		// ImageFormat.YUV_420_888, 1);

		CameraController.Size imageSize = CameraController.getCameraImageSize();
		// ImageReader for YUV still images
		mImageReaderYUV = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(),
				ImageFormat.YUV_420_888, 2);

		// ImageReader for JPEG still images
		if (getCaptureFormat() == CameraController.RAW)
		{
			CameraController.Size imageSizeJPEG = CameraController.getMaxCameraImageSize(CameraController.JPEG);
			mImageReaderJPEG = ImageReader.newInstance(imageSizeJPEG.getWidth(), imageSizeJPEG.getHeight(),
					ImageFormat.JPEG, 2);
		} else
			mImageReaderJPEG = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(),
					ImageFormat.JPEG, 2);

		// ImageReader for RAW still images
		mImageReaderRAW = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(),
				ImageFormat.RAW_SENSOR, 2);

		guiManager.setupViewfinderPreviewSize(new CameraController.Size(thiz.previewWidth, thiz.previewHeight));
		
		
		mImageReaderPreviewYUV.setOnImageAvailableListener(imageAvailableListener, null);
		mImageReaderYUV.setOnImageAvailableListener(imageAvailableListener, null);
		mImageReaderJPEG.setOnImageAvailableListener(imageAvailableListener, null);
		mImageReaderRAW.setOnImageAvailableListener(imageAvailableListener, null);
	}

	@TargetApi(19)
	@Override
	public Surface getPreviewYUVImageSurface()
	{
		return mImageReaderPreviewYUV.getSurface();
	}

	@TargetApi(19)
	@Override
	public Surface getYUVImageSurface()
	{
		return mImageReaderYUV.getSurface();
	}

	@TargetApi(19)
	@Override
	public Surface getJPEGImageSurface()
	{
		return mImageReaderJPEG.getSurface();
	}

	@TargetApi(19)
	@Override
	public Surface getRAWImageSurface()
	{
		return mImageReaderRAW.getSurface();
	}
	
	public static File getForceFilename()
	{
		return TemplateScreen.getInstance().forceFilename;
	}

	public static void setForceFilename(File fileName)
	{
		TemplateScreen.getInstance().forceFilename = fileName;
	}

	public static Uri getForceFilenameURI()
	{
		return TemplateScreen.getInstance().forceFilenameUri;
	}

	public static void setSurfaceHolderSize(int width, int height)
	{
		if (thiz.surfaceHolder != null)
		{
			Log.e("MainScreen", "setSurfaceHolderSize = " + width + "x" + height);
			thiz.surfaceWidth = width;
			thiz.surfaceHeight = height;
			thiz.surfaceHolder.setFixedSize(width, height);
		}
	}
	

	public static int getOrientation()
	{
		return thiz.orientationMain;
	}

	@Override
	public int getImageSizeIndex()
	{
		return imageSizeIdxPreference;
	}

	@Override
	public int getMultishotImageSizeIndex()
	{
		return 0;
	}

	@Override
	public boolean isShutterSoundEnabled()
	{
		return true;
	}

	@Override
	public int isShotOnTap()
	{
		return 1;
	}

	@Override
	public String getSaveToPath()
	{
		return "1";
	}

	@Override
	public String getSaveTo()
	{
		return "1";
	}

	@Override
	public boolean isSortByData()
	{
		return true;
	}

	/*
	 * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ Get/Set method for private variables
	 */

	@Override
	public void onPreferenceCreate(PreferenceFragment prefActivity)
	{
		CharSequence[] entries = null;
		CharSequence[] entryValues = null;

		int idx = 0;
		int currentIdx = -1;
		String opt1 = "";
		String opt2 = "";

		opt1 = sImageSizeRearPref;
		opt2 = sImageSizeFrontPref;
		currentIdx = TemplateScreen.thiz.getImageSizeIndex();

		if (currentIdx == -1)
		{
			currentIdx = 0;
		}

		entries = CameraController.getResolutionsNamesList().toArray(
				new CharSequence[CameraController.getResolutionsNamesList().size()]);
		entryValues = CameraController.getResolutionsIdxesList().toArray(
				new CharSequence[CameraController.getResolutionsIdxesList().size()]);
			
		if (CameraController.getResolutionsIdxesList() != null)
		{
			ListPreference lp = (ListPreference) prefActivity.findPreference(opt1);
			ListPreference lp2 = (ListPreference) prefActivity.findPreference(opt2);

			if (CameraController.getCameraIndex() == 0 && lp2 != null)
				prefActivity.getPreferenceScreen().removePreference(lp2);
			else if (lp != null && lp2 != null)
			{
				prefActivity.getPreferenceScreen().removePreference(lp);
				lp = lp2;
			}
			if (lp != null)
			{
				lp.setEntries(entries);
				lp.setEntryValues(entryValues);

				if (currentIdx != -1)
				{
					// set currently selected image size
					for (idx = 0; idx < entryValues.length; ++idx)
					{
						if (Integer.valueOf(entryValues[idx].toString()) == currentIdx)
						{
							lp.setValueIndex(idx);
							break;
						}
					}
				} else
				{
					lp.setValueIndex(0);
				}

					lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
					{
						public boolean onPreferenceChange(Preference preference, Object newValue)
						{
							thiz.imageSizeIdxPreference = Integer.parseInt(newValue.toString());
							setCameraImageSizeIndex(Integer.parseInt(newValue.toString()), false);
							return true;
						}
					});
			}
		}
	}

	@Override
	public void onAdvancePreferenceCreate(PreferenceFragment prefActivity)
	{
		CheckBoxPreference cp = (CheckBoxPreference) prefActivity.findPreference(getResources().getString(
				R.string.Preference_UseHALv3Key));
		final CheckBoxPreference fp = (CheckBoxPreference) prefActivity.findPreference(TemplateScreen.sCaptureRAWPref);

		if (cp != null)
		{
			if (!CameraController.isHALv3Supported())
				cp.setEnabled(false);
			else
				cp.setEnabled(true);

			cp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				public boolean onPreferenceChange(Preference preference, Object useCamera2)
				{
					PreferenceManager.getDefaultSharedPreferences(TemplateScreen.getMainContext()).edit()
							.putBoolean(TemplateScreen.sInitModeListPref, true).commit();

					boolean new_value = Boolean.parseBoolean(useCamera2.toString());
					if (new_value)
					{
						if (fp != null && CameraController.isRAWCaptureSupported())
							fp.setEnabled(true);
						else
							fp.setEnabled(false);
					} else if (fp != null)
					{
						PreferenceManager.getDefaultSharedPreferences(TemplateScreen.getMainContext()).edit()
								.putBoolean(TemplateScreen.sCaptureRAWPref, false).commit();
						fp.setEnabled(false);
					}

					return true;
				}
			});
		}

		final PreferenceFragment mPref = prefActivity;

		if (fp != null)
		{
			fp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				public boolean onPreferenceChange(Preference preference, Object captureRAW)
				{
					boolean new_value = Boolean.parseBoolean(captureRAW.toString());
					if (new_value)
					{
						new AlertDialog.Builder(mPref.getActivity())
								.setIcon(R.drawable.gui_almalence_alert_dialog_icon)
								.setTitle(R.string.Pref_Common_CaptureRAW_Title)
								.setMessage(R.string.Pref_Common_CaptureRAW_Description)
								.setPositiveButton(android.R.string.ok, null).create().show();
					}
					return true;
				}
			});

			if (CameraController.isRAWCaptureSupported() && CameraController.isUseHALv3())
				fp.setEnabled(true);
			else
				fp.setEnabled(false);
		}
	}

	
	@Override
	protected void onApplicationStop()
	{
		switchingMode = false;
		mApplicationStarted = false;
		orientationMain = 0;
		orientationMainPrevious = 0;
		TemplateScreen.getGUIManager().onStop();
		ApplicationScreen.getPluginManager().onStop();
		CameraController.onStop();

		if (CameraController.isUseHALv3())
			stopImageReaders();
	}

	@TargetApi(21)
	protected void stopImageReaders()
	{
		// IamgeReader should be closed
		if (mImageReaderPreviewYUV != null)
		{
			mImageReaderPreviewYUV.close();
			mImageReaderPreviewYUV = null;
		}
		if (mImageReaderYUV != null)
		{
			mImageReaderYUV.close();
			mImageReaderYUV = null;
		}
		if (mImageReaderJPEG != null)
		{
			mImageReaderJPEG.close();
			mImageReaderJPEG = null;
		}
		if (mImageReaderRAW != null)
		{
			mImageReaderRAW.close();
			mImageReaderRAW = null;
		}
	}

	private CountDownTimer	onResumeTimer	= null;

	@Override
	protected void onApplicationResume()
	{
		isCameraConfiguring = false;

		if (!isCreating)
			onResumeTimer = new CountDownTimer(50, 50)
			{
				public void onTick(long millisUntilFinished)
				{
					// Not used
				}

				public void onFinish()
				{
					SharedPreferences prefs = PreferenceManager
							.getDefaultSharedPreferences(TemplateScreen.getMainContext());

					updatePreferences();

					preview.setKeepScreenOn(keepScreenOn);

					captureFormat = CameraController.JPEG;

					setScreenBrightness(true);

					CameraController.useHALv3(prefs.getBoolean(getResources()
							.getString(R.string.Preference_UseHALv3Key), CameraController.isNexus() ? true : false));
					prefs.edit()
							.putBoolean(getResources().getString(R.string.Preference_UseHALv3Key),
									CameraController.isUseHALv3()).commit();

					// Log.e("MainScreen",
					if (CameraController.isUseHALv3())
					{
						TemplateScreen.setSurfaceHolderSize(1, 1);
					}

					TemplateScreen.getGUIManager().onResume();
					ApplicationScreen.getPluginManager().onResume();
					CameraController.onResume();
					TemplateScreen.thiz.mPausing = false;

					if (CameraController.isUseHALv3())
					{
						TemplateScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
						Log.d("MainScreen", "onResume: CameraController.setupCamera(null)");
						CameraController.setupCamera(null, !switchingMode);

						if (glView != null)
						{
							glView.onResume();
							Log.d("GL", "glView onResume");
						}
					} else if ((surfaceCreated && (!CameraController.isCameraCreated())) ||
					// this is for change mode without camera restart!
							(surfaceCreated && TemplateScreen.getInstance().getSwitchingMode()))
					{
						TemplateScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
						CameraController.setupCamera(surfaceHolder, !switchingMode);

						if (glView != null)
						{
							glView.onResume();
							Log.d("GL", "glView onResume");
						}
					}
					orientListener.enable();
				}
			}.start();

		shutterPlayer = new SoundPlayer(this.getBaseContext(), getResources().openRawResourceFd(
				R.raw.plugin_capture_tick));

		if (screenTimer != null)
		{
			if (isScreenTimerRunning)
				screenTimer.cancel();
			screenTimer.start();
			isScreenTimerRunning = true;
		}

		long memoryFree = getAvailableInternalMemory();
		if (memoryFree < 30)
			Toast.makeText(TemplateScreen.getMainContext(), "Almost no free space left on internal storage.",
					Toast.LENGTH_LONG).show();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TemplateScreen.getMainContext());
		boolean dismissKeyguard = prefs.getBoolean("dismissKeyguard", true);
		if (dismissKeyguard)
			getWindow()
					.addFlags(
							WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
									| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		else
		{
			getWindow()
					.clearFlags(
							WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
									| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		}
	}

	
	private void updatePreferences()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TemplateScreen.getMainContext());
		CameraController.setCameraIndex(!prefs.getBoolean(TemplateScreen.sUseFrontCameraPref, false) ? 0 : 1);
		imageSizeIdxPreference = 0;
//		imageSizeIdxPreference = prefs.getInt(CameraController.getCameraIndex() == 0 ? TemplateScreen.sImageSizeRearPref
//				: TemplateScreen.sImageSizeFrontPref, -1);

		keepScreenOn = prefs.getBoolean("keepScreenOn", false);
	}

	
	@Override
	public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height)
	{
		Log.e("MainScreen", "SURFACE CHANGED");
		mCameraSurface = holder.getSurface();

		if (isCameraConfiguring)
		{
			ApplicationScreen.getPluginManager().sendMessage(ApplicationInterface.MSG_SURFACE_CONFIGURED, 0);
			isCameraConfiguring = false;
		} else if (!isCreating)
		{
			new CountDownTimer(50, 50)
			{
				public void onTick(long millisUntilFinished)
				{
					// Not used
				}

				public void onFinish()
				{
					updatePreferences();

					if (!TemplateScreen.thiz.mPausing && surfaceCreated && (!CameraController.isCameraCreated()))
					{
						TemplateScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
						if (!CameraController.isUseHALv3())
						{
							CameraController.setupCamera(holder, !switchingMode);
						}
						else
							messageHandler.sendEmptyMessage(ApplicationInterface.MSG_SURFACE_READY);
					}
				}
			}.start();
		} else
		{
			updatePreferences();
		}
	}
	
	
	@TargetApi(21)
	@Override
	public void createCaptureSession()
	{
		mCameraSurface = surfaceHolder.getSurface();
		surfaceList.add(mCameraSurface); // surface for viewfinder preview

//		if (captureFormat != CameraController.RAW) // when capture RAW preview frames is not available
		surfaceList.add(mImageReaderPreviewYUV.getSurface()); // surface for preview yuv
		// images
		if (captureFormat == CameraController.YUV)
		{
			Log.d("MainScreen",
					"add mImageReaderYUV " + mImageReaderYUV.getWidth() + " x " + mImageReaderYUV.getHeight());
			surfaceList.add(mImageReaderYUV.getSurface()); // surface for yuv
															// image
			// capture
		} else if (captureFormat == CameraController.JPEG)
		{
			Log.d("MainScreen",
					"add mImageReaderJPEG " + mImageReaderJPEG.getWidth() + " x " + mImageReaderJPEG.getHeight());
			surfaceList.add(mImageReaderJPEG.getSurface()); // surface for jpeg
															// image
			// capture
		} else if (captureFormat == CameraController.RAW)
		{
			Log.d("MainScreen", "add mImageReaderRAW + mImageReaderJPEG " + mImageReaderRAW.getWidth() + " x "
					+ mImageReaderRAW.getHeight());
			surfaceList.add(mImageReaderJPEG.getSurface()); // surface for jpeg
															// image
			// capture
			if (CameraController.isRAWCaptureSupported())
				surfaceList.add(mImageReaderRAW.getSurface());
		}

		// sfl.add(mImageReaderJPEG.getSurface());
		CameraController.setPreviewSurface(mImageReaderPreviewYUV.getSurface());

		// guiManager.setupViewfinderPreviewSize(new
		// CameraController.Size(this.previewWidth, this.previewHeight));
		// guiManager.setupViewfinderPreviewSize(new CameraController.Size(1280,
		// 960));

		CameraController.setCaptureFormat(captureFormat);
		// configure camera with all the surfaces to be ever used

		// If camera device isn't initialized (equals null) just force stop
		// application.
		if (!CameraController.createCaptureSession(surfaceList))
			ApplicationScreen.getPluginManager().sendMessage(ApplicationInterface.MSG_APPLICATION_STOP, 0);
	}

	
	@Override
	public boolean onKeyUpEvent(int keyCode, KeyEvent event) {
		// Prevent system sounds, for volume buttons.
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean onKeyDownEvent(int keyCode, KeyEvent event)
	{
		if (!mApplicationStarted)
			return true;

		// menu button processing
		if (keyCode == KeyEvent.KEYCODE_MENU)
		{
			menuButtonPressed();
			return true;
		}
		// shutter/camera button processing
		if (keyCode == KeyEvent.KEYCODE_CAMERA || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
		{
			TemplateScreen.getGUIManager().onHardwareShutterButtonPressed();
			return true;
		}
		// focus/half-press button processing
		if (keyCode == KeyEvent.KEYCODE_FOCUS)
		{
			if (event.getDownTime() == event.getEventTime())
			{
				TemplateScreen.getGUIManager().onHardwareFocusButtonPressed();
			}
			return true;
		}

		// check if Headset Hook button has some functions except standard
		if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TemplateScreen.getMainContext());
			boolean headsetFunc = prefs.getBoolean("headsetPrefCommon", false);
			if (headsetFunc)
			{
				TemplateScreen.getGUIManager().onHardwareFocusButtonPressed();
				TemplateScreen.getGUIManager().onHardwareShutterButtonPressed();
				return true;
			}
		}

		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{
			TemplateScreen.getGUIManager().onHardwareFocusButtonPressed();
			TemplateScreen.getGUIManager().onHardwareShutterButtonPressed();
			return true;
		}

		if (ApplicationScreen.getPluginManager().onKeyDown(true, keyCode, event))
			return true;
		if (guiManager.onKeyDown(true, keyCode, event))
			return true;

		return false;
	}
	

	@Override
	public void showCaptureIndication(boolean playShutter)
	{
		// play tick sound
		TemplateScreen.getGUIManager().showCaptureIndication();
		if(playShutter)
			TemplateScreen.playShutter();
	}

	public void playShutter(int sound)
	{
		if (!TemplateScreen.getInstance().isShutterSoundEnabled())
		{
			MediaPlayer mediaPlayer = MediaPlayer.create(TemplateScreen.thiz, sound);
			mediaPlayer.start();
		}
	}

	public static void playShutter()
	{
		if (!TemplateScreen.getInstance().isShutterSoundEnabled())
		{
			if (thiz.shutterPlayer != null)
				thiz.shutterPlayer.play();
		}
	}
}
