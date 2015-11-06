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

package com.almalence.plugins.capture.video;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video.VideoColumns;
import android.support.v4.provider.DocumentFile;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.almalence.ui.RotateImageView;
import com.almalence.util.Util;

/* <!-- +++
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.CameraParameters;
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.PluginCapture;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.ui.GUI.ShutterButton;
 import com.almalence.opencam_plus.ApplicationInterface;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.ui.GUI.ShutterButton;
//-+- -->

/***
 * Implements basic functionality of Video capture.
 ***/

public class VideoCapturePlugin extends PluginCapture
{
	private static final String					TAG							= "Almalence";

	private volatile boolean					isRecording;
	private boolean								onPause;
	private boolean								lockPauseButton				= false;

	private MediaRecorder						mMediaRecorder;

	private long								mRecordingStartTime;

	// The video duration limit. 0 means no limit.
	private int									mMaxVideoDurationInMs;

	// video duration text view
	private TextView							mRecordingTimeView;
	private long								mRecorded;

	private boolean								mRecordingTimeCountsDown	= false;

	private boolean								shutterOff					= false;
	
	private boolean 							preferenceVideoMuteMode		= false;

	// vars to work with files for android < 5
	private static File							fileSaved					= null;
	private ArrayList<File>						filesList					= new ArrayList<File>();

	// vars to work with files for android >= 5
	private static DocumentFile					fileSavedNew				= null;
	private static ParcelFileDescriptor			fileSavedNewFd				= null;
	private ArrayList<DocumentFile>				filesListNew				= new ArrayList<DocumentFile>();

	private int									preferenceFocusMode;
	private int									preferenceVideoFocusMode;

	private RotateImageView						timeLapseButton;
	private RotateImageView						pauseVideoButton;
	private RotateImageView						stopVideoButton;
	private RotateImageView						takePictureButton;

	private boolean								showRecording				= false;
	private boolean								pauseBlink					= true;

	private View								buttonsLayout;

	private boolean								snapshotSupported			= false;

	private boolean								videoStabilization			= false;

	public static final int						QUALITY_4K					= 4096;

	private ImageView							rotateToLandscapeNotifier;
	private boolean								showLandscapeNotification	= true;
	private View								rotatorLayout;
	private TimeLapseDialog						timeLapseDialog;

	private boolean								displayTakePicture;
	private ContentValues						values;

	private static Hashtable<Integer, Boolean>	previewSizes				= new Hashtable<Integer, Boolean>()
																			{
																				private static final long	serialVersionUID	= -6076051817063312974L;

																				{
																					put(CamcorderProfile.QUALITY_QCIF,
																							false);
																					put(CamcorderProfile.QUALITY_CIF,
																							false);
																					put(CamcorderProfile.QUALITY_480P,
																							false);
																					put(CamcorderProfile.QUALITY_720P,
																							false);
																					put(CamcorderProfile.QUALITY_1080P,
																							false);
																					put(CamcorderProfile.QUALITY_2160P,
																							false);
																					put(CamcorderProfile.QUALITY_HIGH,
																							false);
																					put(QUALITY_4K, false);
																				}
																			};

	private boolean								qualityCIFSupported			= false;
	private boolean								qualityQCIFSupported		= false;
	private boolean								quality480Supported			= false;
	private boolean								quality720Supported			= false;
	private boolean								quality1080Supported		= false;
	private boolean								quality2160Supported		= false;
	private boolean								quality4KSupported			= false;

	private volatile String						ModePreference;													// 0=DRO
																													// On
																													// 1=DRO
																													// Off
	private boolean								camera2Preference;

	private com.almalence.ui.Switch.Switch		modeSwitcher;

	private DROVideoEngine						droEngine					= new DROVideoEngine();

	public static final String					ACTION_NEW_VIDEO			= "android.hardware.action.NEW_VIDEO";

	public VideoCapturePlugin()
	{
		super("com.almalence.plugins.videocapture", R.xml.preferences_capture_video, 0,
				R.drawable.gui_almalence_video_1080, "Video quality");
	}

	@Override
	public void onCreate()
	{
		mRecordingTimeView = new TextView(ApplicationScreen.getMainContext());
		mRecordingTimeView.setTextSize(12);
		mRecordingTimeView.setBackgroundResource(R.drawable.thumbnail_background);
		mRecordingTimeView.setVisibility(View.GONE);
		mRecordingTimeView.setGravity(Gravity.CENTER);
		mRecordingTimeView.setText("00:00");

		this.createModeSwitcher();

		if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2)
		{
			this.modeSwitcher.setVisibility(View.GONE);
		}
	}

	private void createModeSwitcher()
	{
		LayoutInflater inflator = ApplicationScreen.instance.getLayoutInflater();
		modeSwitcher = (com.almalence.ui.Switch.Switch) inflator.inflate(R.layout.plugin_capture_standard_modeswitcher,
				null, false);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		ModePreference = prefs.getString("modeVideoDROPref", "1");
		modeSwitcher.setTextOn(ApplicationScreen.instance.getString(R.string.Pref_Video_DRO_ON));
		modeSwitcher.setTextOff(ApplicationScreen.instance.getString(R.string.Pref_Video_DRO_OFF));
		modeSwitcher.setChecked(ModePreference.compareTo("0") == 0 ? true : false);
		modeSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen
						.getMainContext());

				if (isChecked)
				{
					ModePreference = "0";
					if (CameraController.isNexus6 || CameraController.isNexus5x || CameraController.isNexus6p)
					{
						Toast.makeText(ApplicationScreen.getMainContext(),
								"Not suported currently on your device. Will be available later.", Toast.LENGTH_LONG)
								.show();
						ModePreference = "1";
						modeSwitcher.setChecked(false);
						return;
					}
				} else
				{
					ModePreference = "1";
				}

				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("modeVideoDROPref", ModePreference);
				editor.commit();

				if (modeDRO())
				{
					final int ImageSizeIdxPreference = Integer.parseInt(prefs.getString(CameraController
							.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
							: ApplicationScreen.sImageSizeVideoFrontPref, "2"));
					if (ImageSizeIdxPreference == 2 || ImageSizeIdxPreference == 3 || maxQuality())
					{
						quickControlIconID = R.drawable.gui_almalence_video_720;
						editor.putString(
								CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
										: ApplicationScreen.sImageSizeVideoFrontPref, "4");
						editor.commit();
						VideoCapturePlugin.this.refreshQuickControl();
					}
				}

				try
				{
					CameraController.stopCameraPreview();
//					Camera.Parameters cp = CameraController.getCameraParameters();
//					if (cp != null)
//					{
						setCameraPreviewSize();
						CameraController.Size sz = new CameraController.Size(ApplicationScreen.getPreviewWidth(),
								ApplicationScreen.getPreviewHeight());
						ApplicationScreen.getGUIManager().setupViewfinderPreviewSize(sz);
//					}
					if (VideoCapturePlugin.this.modeDRO())
					{
						takePictureButton.setVisibility(View.GONE);
						timeLapseButton.setVisibility(View.GONE);
						ApplicationScreen.instance.showOpenGLLayer(2);
						ApplicationScreen.instance.glSetRenderingMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
					} else
					{
						if (!CameraController.isRemoteCamera())
						{
							if (displayTakePicture)
								takePictureButton.setVisibility(View.VISIBLE);
							timeLapseButton.setVisibility(View.VISIBLE);
						}

						droEngine.onPause();

//						Camera camera = CameraController.getCamera();
//						if (camera != null)
//							try
//							{
//								camera.setDisplayOrientation(90);
//							} catch (RuntimeException e)
//							{
//								e.printStackTrace();
//							}
//
//						if (camera != null)
//							try
//							{
//								camera.setPreviewDisplay(ApplicationScreen.getPreviewSurfaceHolder());
//							} catch (IOException e)
//							{
//								e.printStackTrace();
//							}
						CameraController.startCameraPreview();
						ApplicationScreen.instance.hideOpenGLLayer();
					}
				} catch (final Exception e)
				{
					Log.e(TAG, Util.toString(e.getStackTrace(), '\n'));
					e.printStackTrace();
				}
			}
		});

	}

	@Override
	public void onStart()
	{
		getPrefs();
	}

	@Override
	public void onGUICreate()
	{
		this.clearViews();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());

		// change shutter icon
		isRecording = false;
		prefs.edit().putBoolean("videorecording", false).commit();

		// if (swChecked)
		// {
		ApplicationScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_START);
		// }
		// else
		// {
		// ApplicationScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_START_WITH_PAUSE);
		// }

		onPreferenceCreate((PreferenceFragment) null);

		int ImageSizeIdxPreference = Integer.parseInt(prefs.getString(
				CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
						: ApplicationScreen.sImageSizeVideoFrontPref, "2"));
		int quality = 0;
		switch (ImageSizeIdxPreference)
		{
		case 0:
			quality = CamcorderProfile.QUALITY_QCIF;
			quickControlIconID = R.drawable.gui_almalence_video_qcif;
			break;
		case 1:
			quality = CamcorderProfile.QUALITY_CIF;
			quickControlIconID = R.drawable.gui_almalence_video_cif;
			break;
		case 2:
			if (this.modeDRO())
			{
				quality = CamcorderProfile.QUALITY_720P;
				quickControlIconID = R.drawable.gui_almalence_video_720;
			} else
			{
				quality = CamcorderProfile.QUALITY_2160P;
				quickControlIconID = R.drawable.gui_almalence_video_2160;
			}
			break;
		case 3:
			if (this.modeDRO())
			{
				quality = CamcorderProfile.QUALITY_720P;
				quickControlIconID = R.drawable.gui_almalence_video_720;
			} else
			{
				quality = CamcorderProfile.QUALITY_1080P;
				quickControlIconID = R.drawable.gui_almalence_video_1080;
			}
			break;
		case 4:
			quality = CamcorderProfile.QUALITY_720P;
			quickControlIconID = R.drawable.gui_almalence_video_720;
			break;
		case 5:
			quality = CamcorderProfile.QUALITY_480P;
			quickControlIconID = R.drawable.gui_almalence_video_480;
			break;
		case 6:
			quality = QUALITY_4K;
			quickControlIconID = R.drawable.gui_almalence_video_4096;
			break;
		default:
			break;
		}

		// If selected profile not supported, then select max from available.
		if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality) && !previewSizes.get(quality))
		{
			ImageSizeIdxPreference = 2;
			quality = CamcorderProfile.QUALITY_2160P;
			quickControlIconID = R.drawable.gui_almalence_video_2160;
			if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality) && !previewSizes.get(quality))
			{
				ImageSizeIdxPreference = 3;
				quality = CamcorderProfile.QUALITY_1080P;
				quickControlIconID = R.drawable.gui_almalence_video_1080;
				if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality)
						&& !previewSizes.get(quality))
				{
					ImageSizeIdxPreference = 4;
					quality = CamcorderProfile.QUALITY_720P;
					quickControlIconID = R.drawable.gui_almalence_video_720;
					if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality)
							&& !previewSizes.get(quality))
					{
						ImageSizeIdxPreference = 5;
						quality = CamcorderProfile.QUALITY_480P;
						quickControlIconID = R.drawable.gui_almalence_video_480;
					}
				}
			}
		}

		if (maxQuality())
		{
			quickControlIconID = -1;
		}

		Editor editor = prefs.edit();
		editor.putString(CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
				: ApplicationScreen.sImageSizeVideoFrontPref, String.valueOf(ImageSizeIdxPreference));
		editor.commit();

		List<View> specialView = new ArrayList<View>();
		RelativeLayout specialLayout = (RelativeLayout) ApplicationScreen.instance
				.findViewById(R.id.specialPluginsLayout2);
		for (int i = 0; i < specialLayout.getChildCount(); i++)
			specialView.add(specialLayout.getChildAt(i));

		for (int j = 0; j < specialView.size(); j++)
		{
			View view = specialView.get(j);
			int view_id = view.getId();
			if (view_id == this.mRecordingTimeView.getId() || view_id == this.modeSwitcher.getId())
			{
				if (view.getParent() != null)
					((ViewGroup) view.getParent()).removeView(view);

				specialLayout.removeView(view);
			}
		}

		{
			final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);

			params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

			((RelativeLayout) ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout3))
					.removeView(this.modeSwitcher);
			((RelativeLayout) ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout3)).addView(
					this.modeSwitcher, params);

			this.modeSwitcher.setLayoutParams(params);
			// this.modeSwitcher.requestLayout();
		}

		// Calculate right sizes for plugin's controls
		DisplayMetrics metrics = new DisplayMetrics();
		ApplicationScreen.instance.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		float fScreenDensity = metrics.density;

		int iIndicatorSize = (int) (ApplicationScreen.getMainContext().getResources()
				.getInteger(R.integer.infoControlHeight) * fScreenDensity);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(iIndicatorSize, iIndicatorSize);
		int topMargin = ApplicationScreen.instance.findViewById(R.id.paramsLayout).getHeight()
				+ (int) ApplicationScreen.getAppResources().getDimension(R.dimen.viewfinderViewsMarginTop);
		params.setMargins((int) (2 * ApplicationScreen.getGUIManager().getScreenDensity()), topMargin, 0, 0);

		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

		((RelativeLayout) ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout2)).addView(
				this.mRecordingTimeView, params);

		this.mRecordingTimeView.setLayoutParams(params);
		// this.mRecordingTimeView.requestLayout();

		// ((RelativeLayout)
		// ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout2)).requestLayout();

		LayoutInflater inflator = ApplicationScreen.instance.getLayoutInflater();
		buttonsLayout = inflator.inflate(R.layout.plugin_capture_video_layout, null, false);
		buttonsLayout.setVisibility(View.VISIBLE);

		timeLapseButton = (RotateImageView) buttonsLayout.findViewById(R.id.buttonTimeLapse);
		pauseVideoButton = (RotateImageView) ApplicationScreen.instance.findViewById(R.id.buttonVideoPause);
		stopVideoButton = (RotateImageView) ApplicationScreen.instance.findViewById(R.id.buttonVideoStop);

		snapshotSupported = CameraController.isVideoSnapshotSupported();
		takePictureButton = (RotateImageView) buttonsLayout.findViewById(R.id.buttonCaptureImage);

		timeLapseButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				TimeLapseDialog();
			}
		});

		pauseVideoButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				pauseVideoRecording();
			}
		});

		stopVideoButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onShutterClick();
			}
		});

		if (snapshotSupported)
		{
			takePictureButton.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					takePicture();
				}

			});
		}

		for (int j = 0; j < specialView.size(); j++)
		{
			View view = specialView.get(j);
			int view_id = view.getId();
			if (view_id == this.buttonsLayout.getId())
			{
				if (view.getParent() != null)
					((ViewGroup) view.getParent()).removeView(view);

				specialLayout.removeView(view);
			}
		}

		params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.height = (int) ApplicationScreen.getAppResources().getDimension(R.dimen.videobuttons_size);

		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

		((RelativeLayout) ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout2)).addView(
				this.buttonsLayout, params);

		this.buttonsLayout.setLayoutParams(params);
		// this.buttonsLayout.requestLayout();
		//
		// ((RelativeLayout)
		// ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout2)).requestLayout();

		if (snapshotSupported)
		{
			takePictureButton.setOrientation(ApplicationScreen.getGUIManager().getLayoutOrientation());
			takePictureButton.invalidate();
			// takePictureButton.requestLayout();
			displayTakePicture = true;
		} else
		{
			takePictureButton.setVisibility(View.GONE);
			displayTakePicture = false;
		}

		timeLapseButton.setOrientation(ApplicationScreen.getGUIManager().getLayoutOrientation());
		// timeLapseButton.invalidate();
		// timeLapseButton.requestLayout();

		if (this.modeDRO() || CameraController.isRemoteCamera())
		{
			takePictureButton.setVisibility(View.GONE);
			timeLapseButton.setVisibility(View.GONE);
		}

		if (prefs.getBoolean("videoStartStandardPref", false))
		{
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					switch (which)
					{
					case DialogInterface.BUTTON_POSITIVE:
						PluginManager.getInstance().onPause(true);
						Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
						ApplicationScreen.instance.startActivity(intent);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						// No button clicked
						break;
					default:
						break;
					}
				}
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(ApplicationScreen.instance);
			builder.setMessage("You selected to start standard camera. Start camera?")
					.setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener).show();
		}

		rotatorLayout = inflator.inflate(R.layout.plugin_capture_video_lanscaperotate_layout, null, false);
		rotatorLayout.setVisibility(View.VISIBLE);

		rotateToLandscapeNotifier = (ImageView) rotatorLayout.findViewById(R.id.rotatorImageView);

		List<View> specialViewRotator = new ArrayList<View>();
		RelativeLayout specialLayoutRotator = (RelativeLayout) ApplicationScreen.instance
				.findViewById(R.id.specialPluginsLayout);
		for (int i = 0; i < specialLayoutRotator.getChildCount(); i++)
			specialViewRotator.add(specialLayoutRotator.getChildAt(i));

		for (int j = 0; j < specialViewRotator.size(); j++)
		{
			View view = specialViewRotator.get(j);
			int view_id = view.getId();
			int layout_id = this.rotatorLayout.getId();
			if (view_id == layout_id)
			{
				if (view.getParent() != null)
					((ViewGroup) view.getParent()).removeView(view);

				specialLayoutRotator.removeView(view);
			}
		}

		RelativeLayout.LayoutParams paramsRotator = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		paramsRotator.height = (int) ApplicationScreen.getAppResources().getDimension(R.dimen.gui_element_2size);

		paramsRotator.addRule(RelativeLayout.CENTER_IN_PARENT);

		((RelativeLayout) ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout)).addView(
				this.rotatorLayout, paramsRotator);

		// rotatorLayout.setLayoutParams(paramsRotator);
		// rotatorLayout.requestLayout();

		// ((RelativeLayout)
		// ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout)).requestLayout();
	}

	@Override
	public void onQuickControlClick()
	{
		if (isRecording)
			return;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		Editor editor = prefs.edit();

		int ImageSizeIdxPreference = Integer.parseInt(prefs.getString(
				CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
						: ApplicationScreen.sImageSizeVideoFrontPref, "3"));

		int quality = 0;
		switch (ImageSizeIdxPreference)
		{
		case 0:
			quality = CamcorderProfile.QUALITY_CIF;
			quickControlIconID = R.drawable.gui_almalence_video_cif;
			editor.putString(CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
					: ApplicationScreen.sImageSizeVideoFrontPref, "1");
			break;
		case 1:
			if (this.modeDRO())
			{
				quality = CamcorderProfile.QUALITY_720P;
				quickControlIconID = R.drawable.gui_almalence_video_720;
				editor.putString(CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
						: ApplicationScreen.sImageSizeVideoFrontPref, "4");
			} else
			{
				quality = CamcorderProfile.QUALITY_2160P;
				quickControlIconID = R.drawable.gui_almalence_video_2160;
				editor.putString(CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
						: ApplicationScreen.sImageSizeVideoFrontPref, "2");
			}
			break;
		case 2:
			if (this.modeDRO())
			{
				quality = CamcorderProfile.QUALITY_720P;
				quickControlIconID = R.drawable.gui_almalence_video_720;
				editor.putString(CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
						: ApplicationScreen.sImageSizeVideoFrontPref, "4");
			} else
			{
				quality = CamcorderProfile.QUALITY_1080P;
				quickControlIconID = R.drawable.gui_almalence_video_1080;
				editor.putString(CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
						: ApplicationScreen.sImageSizeVideoFrontPref, "3");
			}
			break;
		case 3:
			quality = CamcorderProfile.QUALITY_720P;
			quickControlIconID = R.drawable.gui_almalence_video_720;
			editor.putString(CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
					: ApplicationScreen.sImageSizeVideoFrontPref, "4");
			break;
		case 4:
			quality = CamcorderProfile.QUALITY_480P;
			quickControlIconID = R.drawable.gui_almalence_video_480;
			editor.putString(CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
					: ApplicationScreen.sImageSizeVideoFrontPref, "5");
			break;
		case 5:
			quality = CamcorderProfile.QUALITY_QCIF;
			quickControlIconID = R.drawable.gui_almalence_video_qcif;
			editor.putString(CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
					: ApplicationScreen.sImageSizeVideoFrontPref, "0");
			break;
		case 6:
			quality = QUALITY_4K;
			quickControlIconID = R.drawable.gui_almalence_video_4096;
			editor.putString(CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
					: ApplicationScreen.sImageSizeVideoFrontPref, "1");
			break;
		default:
			break;
		}

		editor.commit();

		if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality) && !previewSizes.get(quality))
		{
			ImageSizeIdxPreference = (Integer.parseInt(prefs.getString(
					CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
							: ApplicationScreen.sImageSizeVideoFrontPref, "3")) + 1) % 5;
			editor.putString(CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
					: ApplicationScreen.sImageSizeVideoFrontPref, String.valueOf(ImageSizeIdxPreference));
			onQuickControlClick();
		}

		CameraController.stopCameraPreview();
//		Camera.Parameters cp = CameraController.getCameraParameters();
//		if (cp != null)
//		{
			setCameraPreviewSize();
//			Camera.Size sz = CameraController.getCameraParameters().getPreviewSize();
			CameraController.Size sz = ApplicationScreen.instance.getPreviewSize();
			ApplicationScreen.getGUIManager()
					.setupViewfinderPreviewSize(new CameraController.Size(sz.getWidth(), sz.getHeight()));
//		}
		CameraController.startCameraPreview();

		PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST,
				ApplicationInterface.MSG_PREVIEW_CHANGED);
	}

	@Override
	public void onOrientationChanged(int orientation)
	{
		if (mRecordingTimeView != null)
		{
			mRecordingTimeView.setRotation(ApplicationScreen.getGUIManager().getDisplayRotation());
			mRecordingTimeView.invalidate();
		}
		if (snapshotSupported)
		{
			if (takePictureButton != null)
			{
				takePictureButton.setOrientation(ApplicationScreen.getGUIManager().getLayoutOrientation());
				// takePictureButton.invalidate();
				// takePictureButton.requestLayout();
			}
		}
		if (timeLapseButton != null)
		{
			timeLapseButton.setOrientation(ApplicationScreen.getGUIManager().getLayoutOrientation());
			// timeLapseButton.invalidate();
			// timeLapseButton.requestLayout();
		}

		if (rotatorLayout != null && showLandscapeNotification)
		{
			if (!isRecording && (orientation == 90 || orientation == 270))
			{
				startRotateAnimation();
				rotatorLayout.findViewById(R.id.rotatorImageView).setVisibility(View.VISIBLE);
				rotatorLayout.findViewById(R.id.rotatorInnerImageView).setVisibility(View.VISIBLE);
			} else
			{
				rotatorLayout.findViewById(R.id.rotatorInnerImageView).setVisibility(View.GONE);
				rotatorLayout.findViewById(R.id.rotatorImageView).setVisibility(View.GONE);
				if (rotateToLandscapeNotifier != null)
				{
					rotateToLandscapeNotifier.clearAnimation();
				}
			}
		}

		if (timeLapseDialog != null)
		{
			timeLapseDialog.setRotate(ApplicationScreen.getGUIManager().getLayoutOrientation());
		}
	}

	@Override
	public boolean muteSound()
	{
		return true;
	}

	private void startRotateAnimation()
	{
		try
		{
			if (rotateToLandscapeNotifier != null && rotateToLandscapeNotifier.getVisibility() == View.VISIBLE)
				return;

			int height = (int) ApplicationScreen.getAppResources().getDimension(R.dimen.gui_element_2size);
			Animation rotation = new RotateAnimation(0, -180, height / 2, height / 2);
			rotation.setDuration(2000);
			rotation.setRepeatCount(1000);
			rotation.setInterpolator(new DecelerateInterpolator());

			rotateToLandscapeNotifier.startAnimation(rotation);
		} catch (Exception e)
		{
		}
	}
	
	private void stopRotateAnimation()
	{
		try
		{
			if (rotateToLandscapeNotifier != null && rotateToLandscapeNotifier.getVisibility() == View.VISIBLE)
				return;

			if (rotatorLayout != null && showLandscapeNotification)
			{
				rotatorLayout.findViewById(R.id.rotatorInnerImageView).setVisibility(View.GONE);
				rotatorLayout.findViewById(R.id.rotatorImageView).setVisibility(View.GONE);
				if (rotateToLandscapeNotifier != null)
				{
					rotateToLandscapeNotifier.clearAnimation();
				}
			}
		} catch (Exception e)
		{
		}
	}

	// Get output file method for android < 5.0
	private static File getOutputMediaFile()
	{
		File saveDir = PluginManager.getInstance().getSaveDir(false);

		Calendar d = Calendar.getInstance();
		String fileFormat = String.format("%04d%02d%02d_%02d%02d%02d", d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1,
				d.get(Calendar.DAY_OF_MONTH), d.get(Calendar.HOUR_OF_DAY), d.get(Calendar.MINUTE),
				d.get(Calendar.SECOND));
		fileFormat += ".mp4";

		fileSaved = new File(saveDir, fileFormat);
		return fileSaved;
	}

	// Get output file method for android >= 5.0
	@TargetApi(19)
	private static DocumentFile getOutputMediaFileNew()
	{
		DocumentFile saveDir = PluginManager.getInstance().getSaveDirNew(false);

		if (saveDir == null || !saveDir.exists() || !saveDir.canWrite())
		{
			saveDir = PluginManager.getInstance().getSaveDirNew(true);
		}

		Calendar d = Calendar.getInstance();
		String fileFormat = String.format("%04d%02d%02d_%02d%02d%02d", d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1,
				d.get(Calendar.DAY_OF_MONTH), d.get(Calendar.HOUR_OF_DAY), d.get(Calendar.MINUTE),
				d.get(Calendar.SECOND));

		fileSavedNew = saveDir.createFile("video/mp4", fileFormat);

		return fileSavedNew;
	}

	public void onResume()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		preferenceVideoMuteMode = prefs.getBoolean("preferenceVideoMuteMode", false);
//		if (preferenceVideoMuteMode)
//		{
////			AudioManager audioMgr = (AudioManager) ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE);
////			soundVolume = audioMgr.getStreamVolume(AudioManager.STREAM_RING);
////			audioMgr.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
//			muteAllSounds();
//		}

		preferenceFocusMode = prefs.getInt(CameraController.isFrontCamera() ? ApplicationScreen.sRearFocusModePref
				: ApplicationScreen.sFrontFocusModePref, CameraParameters.AF_MODE_AUTO);

		PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).edit()
				.putBoolean("ContinuousCapturing", true).commit();

		shutterOff = false;
		showRecording = false;

		swChecked = false;
		interval = 0;
		measurementVal = 0;

		if (this.shouldPreviewToGPU())
		{
			ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_OPENGL_LAYER_SHOW_V2);
			ApplicationScreen.getMessageHandler().sendEmptyMessage(
					ApplicationInterface.MSG_OPENGL_LAYER_RENDERMODE_WHEN_DIRTY);
		}

		showLandscapeNotification = prefs.getBoolean("showLandscapeNotification", true);

		frameCnt = 0;

		if (CameraController.isRemoteCamera())
		{
			if (timeLapseButton != null)
			{
				timeLapseButton.setVisibility(View.GONE);
			}
			if (takePictureButton != null)
			{
				takePictureButton.setVisibility(View.GONE);
			}
			if (modeSwitcher != null)
			{
				modeSwitcher.setVisibility(View.GONE);
			}
		}
	}

	@Override
	public void onPause()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		prefs.edit()
				.putInt(CameraController.isFrontCamera() ? ApplicationScreen.sRearFocusModePref
						: ApplicationScreen.sFrontFocusModePref, preferenceFocusMode).commit();

//		ApplicationScreen.instance.setFocusModePref(preferenceFocusMode);

		prefs.edit()
				.putBoolean(
						ApplicationScreen.getMainContext().getResources().getString(R.string.Preference_UseCamera2Key),
						camera2Preference).commit();

		if (!CameraController.isRemoteCamera())
		{
			if (null == CameraController.getCamera())
				return;

			if (this.isRecording)
			{
				stopRecording();
			}

			if (!CameraController.isGalaxyS4 && !CameraController.isGalaxyNote3 && !CameraController.isGalaxyNote4)
			{
				try
				{
					CameraController.setRecordingHint(false);
//					Camera.Parameters cp = CameraController.getCameraParameters();
//					cp.setRecordingHint(false);
//					CameraController.setCameraParameters(cp);
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		} else
		{
			if (isRecording)
			{
				stopRecordingSonyRemote();
			}
		}

		if (this.buttonsLayout != null)
		{
			ApplicationScreen.getGUIManager().removeViews(buttonsLayout, R.id.specialPluginsLayout2);
		}

		PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).edit()
				.putBoolean("ContinuousCapturing", false).commit();

		if (this.rotatorLayout != null)
		{
			ApplicationScreen.getGUIManager().removeViews(rotatorLayout, R.id.specialPluginsLayout);
		}

		if (this.modeDRO())
		{
			this.droEngine.onPause();
		}
//
//		if (preferenceVideoMuteMode)
//		{
////			AudioManager audioMgr = (AudioManager) ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE);
////			audioMgr.setStreamVolume(AudioManager.STREAM_RING, soundVolume, 0);
//			unmuteAllSounds();
//		}
	}

	@Override
	public void onStop()
	{
		ApplicationScreen.getGUIManager().removeViews(modeSwitcher, R.id.specialPluginsLayout3);

		if (camera2Preference)
			CameraController.useCamera2OnRelaunch(true);

		CameraController.setUseCamera2(camera2Preference);
	}

	@Override
	public void onCameraParametersSetup()
	{
		this.qualityQCIFSupported = false;
		this.qualityCIFSupported = false;
		this.quality480Supported = false;
		this.quality720Supported = false;
		this.quality1080Supported = false;
		this.quality2160Supported = false;
		this.quality4KSupported = false;
		previewSizes.put(CamcorderProfile.QUALITY_QCIF, false);
		previewSizes.put(CamcorderProfile.QUALITY_CIF, false);
		previewSizes.put(CamcorderProfile.QUALITY_480P, false);
		previewSizes.put(CamcorderProfile.QUALITY_720P, false);
		previewSizes.put(CamcorderProfile.QUALITY_1080P, false);
		previewSizes.put(CamcorderProfile.QUALITY_2160P, false);
		previewSizes.put(QUALITY_4K, false);

		List<CameraController.Size> psz = CameraController.getSupportedPreviewSizes();
		if (psz.contains(new CameraController.Size(176, 144)))
		{
			previewSizes.put(CamcorderProfile.QUALITY_QCIF, true);
			this.qualityQCIFSupported = true;
		}
		if (psz.contains(new CameraController.Size(352, 288)))
		{
			previewSizes.put(CamcorderProfile.QUALITY_CIF, true);
			this.qualityCIFSupported = true;
		}
		if (psz.contains(new CameraController.Size(640, 480)))
		{
			previewSizes.put(CamcorderProfile.QUALITY_480P, true);
			this.quality480Supported = true;
		}
		if (psz.contains(new CameraController.Size(1280, 720)))
		{
			previewSizes.put(CamcorderProfile.QUALITY_720P, true);
			this.quality720Supported = true;
		}
		if (psz.contains(new CameraController.Size(1920, 1080)) || psz.contains(new CameraController.Size(1920, 1088)))
		{
			previewSizes.put(CamcorderProfile.QUALITY_1080P, true);
			this.quality1080Supported = true;
		}
		if (psz.contains(new CameraController.Size(3840, 2160)))
		{
			previewSizes.put(CamcorderProfile.QUALITY_2160P, true);
			this.quality2160Supported = true;
		}
		if (psz.contains(new CameraController.Size(4096, 2160)))
		{
			previewSizes.put(QUALITY_4K, true);
			this.quality4KSupported = true;
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());

		int ImageSizeIdxPreference = Integer.parseInt(prefs.getString(
				CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
						: ApplicationScreen.sImageSizeVideoFrontPref, "2"));
		int quality = 0;
		switch (ImageSizeIdxPreference)
		{
		case 0:
			quality = CamcorderProfile.QUALITY_QCIF;
			quickControlIconID = R.drawable.gui_almalence_video_qcif;
			break;
		case 1:
			quality = CamcorderProfile.QUALITY_CIF;
			quickControlIconID = R.drawable.gui_almalence_video_cif;
			break;
		case 2:
			if (this.modeDRO())
			{
				quality = CamcorderProfile.QUALITY_720P;
				quickControlIconID = R.drawable.gui_almalence_video_720;
			} else
			{
				quality = CamcorderProfile.QUALITY_2160P;
				quickControlIconID = R.drawable.gui_almalence_video_2160;
			}
			break;
		case 3:
			if (this.modeDRO())
			{
				quality = CamcorderProfile.QUALITY_720P;
				quickControlIconID = R.drawable.gui_almalence_video_720;
			} else
			{
				quality = CamcorderProfile.QUALITY_1080P;
				quickControlIconID = R.drawable.gui_almalence_video_1080;
			}
			break;
		case 4:
			quality = CamcorderProfile.QUALITY_720P;
			quickControlIconID = R.drawable.gui_almalence_video_720;
			break;
		case 5:
			quality = CamcorderProfile.QUALITY_480P;
			quickControlIconID = R.drawable.gui_almalence_video_480;
			break;
		case 6:
			quality = QUALITY_4K;
			quickControlIconID = R.drawable.gui_almalence_video_4096;
			break;
		default:
			break;
		}

		// If selected profile not supported, then select max from available.
		if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality) && !previewSizes.get(quality))
		{
			ImageSizeIdxPreference = 2;
			quality = CamcorderProfile.QUALITY_2160P;
			quickControlIconID = R.drawable.gui_almalence_video_2160;
			if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality) && !previewSizes.get(quality))
			{
				ImageSizeIdxPreference = 3;
				quality = CamcorderProfile.QUALITY_1080P;
				quickControlIconID = R.drawable.gui_almalence_video_1080;
				if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality)
						&& !previewSizes.get(quality))
				{
					ImageSizeIdxPreference = 4;
					quality = CamcorderProfile.QUALITY_720P;
					quickControlIconID = R.drawable.gui_almalence_video_720;
					if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality)
							&& !previewSizes.get(quality))
					{
						ImageSizeIdxPreference = 5;
						quality = CamcorderProfile.QUALITY_480P;
						quickControlIconID = R.drawable.gui_almalence_video_480;
					}
				}
			}
		}

		Editor editor = prefs.edit();
		editor.putString(CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
				: ApplicationScreen.sImageSizeVideoFrontPref, String.valueOf(ImageSizeIdxPreference));
		editor.commit();

		if (maxQuality())
		{
			quickControlIconID = -1;
		}

		if (!CameraController.isGalaxyS4 && !CameraController.isGalaxyNote3)
		{
			CameraController.setPreviewFrameRate(30);
			CameraController.setRecordingHint(true);
		}
	}

	@Override
	public void setCameraPreviewSize()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		int ImageSizeIdxPreference = Integer.parseInt(prefs.getString(
				CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
						: ApplicationScreen.sImageSizeVideoFrontPref, "2"));

		final CameraController.Size sz = getBestPreviewSizeDRO(ImageSizeIdxPreference);

		Log.i(TAG, String.format("Preview size: %dx%d", sz.getWidth(), sz.getHeight()));

		ApplicationScreen.instance.setCameraPreviewSize(sz.getWidth(), sz.getHeight());
		// ApplicationScreen.setPreviewWidth(sz.getWidth());
		// ApplicationScreen.setPreviewHeight(sz.getHeight());
	}

	// Get optimal supported preview size with aspect ration 16:9 or 4:3
	private static CameraController.Size getBestPreviewSizeNormal(final boolean aspect169)
	{
		if (aspect169)
		{
			return selectMaxPreviewSize(16.0f / 9.0f);
		} else
		{
			return selectMaxPreviewSize(4.0f / 3.0f);
		}
	}

	private CameraController.Size getBestPreviewSizeDRO(final int quality)
	{
		int width;
		int height;

		switch (quality)
		{
		case 0:
			width = 176;
			height = 144;
			break;
		case 1:
			width = 352;
			height = 288;
			break;
		case 2:
			width = 3840;
			height = 2160;
			break;
		case 3:
			width = 1920;
			height = 1080;
			break;
		case 4:
			width = 1280;
			height = 720;
			break;
		case 5:
			width = 720;
			height = 480;
			break;
		case 6:
			width = 4096;
			height = 2160;
			break;
		default:
			return getBestPreviewSizeNormal(false);
		}

		if (maxQuality() && modeDRO())
		{
			width = 720;
			height = 480;
		} else if (maxQuality())
		{
			width = 1920;
			height = 1080;
		}

		final List<CameraController.Size> sizes = CameraController.getSupportedPreviewSizes();

		CameraController.Size size_best = sizes.get(0);
		for (final CameraController.Size size : sizes)
		{
			if (Math.sqrt(sqr(size.getWidth() - width) + sqr(size.getHeight() - height)) < Math.sqrt(sqr(size_best
					.getWidth() - width)
					+ sqr(size_best.getHeight() - height)))
			{
				size_best = size;
			}
		}

		return size_best;
	}

	private static int sqr(final int v)
	{
		return v * v;
	}

	private static CameraController.Size selectMaxPreviewSize(final float ratioDisplay)
	{
		final List<CameraController.Size> sizes = CameraController.getSupportedPreviewSizes();

		CameraController.Size size_best = sizes.get(0);
		float size_ratio_best = size_best.getWidth() / (float) size_best.getHeight();
		float size_ratio_best_comp = size_ratio_best > ratioDisplay ? size_ratio_best / ratioDisplay : ratioDisplay
				/ size_ratio_best;
		int pixels_best = size_best.getWidth() * size_best.getHeight();

		for (final CameraController.Size size : sizes)
		{
			final float size_ratio = size.getWidth() / (float) size.getHeight();
			final float size_ratio_comp = size_ratio > ratioDisplay ? size_ratio / ratioDisplay : ratioDisplay
					/ size_ratio;

			if (size_ratio_comp == size_ratio_best_comp)
			{
				final int pixels = size.getWidth() * size.getHeight();

				if (pixels >= pixels_best)
				{
					size_best = size;
					size_ratio_best_comp = size_ratio_comp;
					pixels_best = pixels;
				}
			} else if (size_ratio_comp < size_ratio_best_comp)
			{
				final int pixels = size.getWidth() * size.getHeight();
				size_best = size;
				size_ratio_best_comp = size_ratio_comp;
				pixels_best = pixels;
			}
		}

		return size_best;
	}

	@Override
	public void setupCameraParameters()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		int jpegQuality = Integer.parseInt(prefs.getString(ApplicationScreen.sJPEGQualityPref, "95"));
		CameraController.setJpegQuality(jpegQuality);

		preferenceVideoFocusMode = ApplicationScreen.instance.getFocusModePref(CameraParameters.AF_MODE_CONTINUOUS_VIDEO);

		if (CameraController.isModeAvailable(CameraController.getSupportedFocusModes(), preferenceVideoFocusMode))
		{
			CameraController.setCameraFocusMode(preferenceVideoFocusMode);
			ApplicationScreen.instance.setFocusModePref(preferenceVideoFocusMode);
		}
	}

	private void releaseMediaRecorder()
	{
		captureRate = 24;
		if (mMediaRecorder != null)
		{
			mMediaRecorder.reset(); // clear recorder configuration
			//mMediaRecorder.release(); // release the recorder object
			mMediaRecorder = null;
			CameraController.lockCamera(); // lock camera for later use
		}
	}

	// *captureRate/24 - to get correct recording time
	double							captureRate	= 24;

	private boolean					lastUseProfile;
	private boolean					lastUseProf;
	private CamcorderProfile		lastCamcorderProfile;
	private CameraController.Size	lastSz;

	private boolean modeDRO()
	{
		return (ModePreference.compareTo("0") == 0);
	}

	private boolean maxQuality()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		return prefs.getBoolean("preferenceVideoMaxQuality", false);
	}

	@Override
	public void onShutterClick()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (shutterOff)
				return;

			if (isRecording)
			{
				long now = SystemClock.uptimeMillis();
				long delta = now - mRecordingStartTime;
				Handler handler = new Handler();
				handler.postDelayed(new Runnable()
				{
					public void run()
					{
						stopRecording();
					}
				}, 1500 - delta);
			} else
			{
				this.startRecording();
			}
		} else
		{//Sony camera
			pauseVideoButton.setVisibility(View.GONE);
			if (isRecording)
			{
				stopRecordingSonyRemote();
			} else
			{
				startRecordingSonyRemote();
			}
		}
	}

	private void startRecordingSonyRemote()
	{
		ApplicationScreen.getGUIManager().lockControls = true;
		CameraController.startVideoRecordingSonyRemote();
		mRecordingStartTime = SystemClock.uptimeMillis();
		isRecording = true;
		showRecordingUI(isRecording);

		View mainButtonsVideo = (View) ApplicationScreen.instance.guiManager.getMainView().findViewById(
				R.id.mainButtonsVideo);
		mainButtonsVideo.setVisibility(View.VISIBLE);

		View mainButtons = (View) ApplicationScreen.instance.guiManager.getMainView().findViewById(R.id.mainButtons);
		mainButtons.setVisibility(View.INVISIBLE);
	}

	private void stopRecordingSonyRemote()
	{
		CameraController.stopVideoRecordingSonyRemote();
		isRecording = false;
		showRecordingUI(isRecording);
		ApplicationScreen.getGUIManager().lockControls = false;
		PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST,
				ApplicationInterface.MSG_CONTROL_UNLOCKED);

		View mainButtonsVideo = (View) ApplicationScreen.instance.guiManager.getMainView().findViewById(
				R.id.mainButtonsVideo);
		mainButtonsVideo.setVisibility(View.GONE);

		View mainButtons = (View) ApplicationScreen.instance.guiManager.getMainView().findViewById(R.id.mainButtons);
		mainButtons.setVisibility(View.VISIBLE);
		mainButtons.findViewById(R.id.buttonSelectMode).setVisibility(View.VISIBLE);
	}

	private void stopRecording()
	{
		if (shutterOff)
			return;

		if (!CameraController.isRemoteCamera())
		{
			if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2)
				modeSwitcher.setVisibility(View.VISIBLE);
		}

		View mainButtonsVideo = (View) ApplicationScreen.instance.guiManager.getMainView().findViewById(
				R.id.mainButtonsVideo);
		mainButtonsVideo.setVisibility(View.GONE);

		View mainButtons = (View) ApplicationScreen.instance.guiManager.getMainView().findViewById(R.id.mainButtons);
		mainButtons.setVisibility(View.VISIBLE);
		mainButtons.findViewById(R.id.buttonSelectMode).setVisibility(View.VISIBLE);

		if (this.modeDRO())
		{
			// <!-- -+-
			PluginManager.getInstance().controlPremiumContent();
			// -+- -->

			this.droEngine.stopRecording();

			ApplicationScreen.getGUIManager().lockControls = false;
			// inform the user that recording has stopped
			isRecording = false;
			showRecordingUI(isRecording);
			PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).edit()
					.putBoolean("videorecording", false).commit();

			// change shutter icon
			// if (swChecked)
			// {
			ApplicationScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_START);
			// } else
			// {
			// ApplicationScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_START_WITH_PAUSE);
			// }

			onPreExportVideo();
			Runnable runnable = new Runnable()
			{
				@Override
				public void run()
				{
					doExportVideo();
				}
			};
			new Thread(runnable).start();
		} else
		{
			this.stopVideoRecording();
		}

		ApplicationScreen.instance.setKeepScreenOn(false);
	}

	protected void onPreExportVideo()
	{
		ApplicationScreen.getGUIManager().startProcessingAnimation();

		String name = "";
		String data = "";
		if (fileSavedNew != null)
		{
			name = fileSavedNew.getName();
			data = null;

			// If we able to get File object, than get path from it. Gallery
			// doesn't show the file, if it's stored at phone memory and
			// DATA field not set.
			File file = Util.getFileFromDocumentFile(fileSavedNew);
			if (file != null)
			{
				data = file.getAbsolutePath();
			} else {
				// This case should typically happen for files saved to SD
				// card.
				data = Util.getAbsolutePathFromDocumentFile(fileSavedNew);
			}
		} else
		{
			name = fileSaved.getName();
			data = fileSaved.getAbsolutePath();
		}

		values = new ContentValues();
		values.put(VideoColumns.TITLE, name.substring(0, name.lastIndexOf(".")));
		values.put(VideoColumns.DISPLAY_NAME, name);
		values.put(VideoColumns.DATE_TAKEN, System.currentTimeMillis());
		values.put(VideoColumns.MIME_TYPE, "video/mp4");
		if (data != null)
		{
			values.put(VideoColumns.DATA, data);
		}
		values.put(VideoColumns.DURATION, timeStringToMillisecond(mRecordingTimeView.getText().toString()));

		if (this.modeDRO())
		{
			values.put(
					VideoColumns.RESOLUTION,
					String.valueOf(ApplicationScreen.getPreviewWidth()) + "x"
							+ String.valueOf(ApplicationScreen.getPreviewHeight()));
		} else
		{
			if (lastUseProfile)
			{
				values.put(
						VideoColumns.RESOLUTION,
						String.valueOf(lastCamcorderProfile.videoFrameWidth) + "x"
								+ String.valueOf(lastCamcorderProfile.videoFrameHeight));
			} else
			{
				values.put(VideoColumns.RESOLUTION,
						String.valueOf(lastSz.getWidth()) + "x" + String.valueOf(lastSz.getHeight()));
			}
		}

		mRecordingTimeView.setText("00:00");
		mRecorded = 0;
		shutterOff = false;
		showRecording = false;
	}

	protected void doExportVideo()
	{
		boolean onPause = this.onPause;
		this.onPause = false;
		boolean isDro = this.modeDRO();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && (fileSavedNew != null || !isDro))
		{
			DocumentFile fileSaved = VideoCapturePlugin.fileSavedNew;
			ArrayList<DocumentFile> filesListToExport = filesListNew;
			String resultName = fileSaved.getName();
			DocumentFile resultFile = fileSaved;

			if (filesListToExport.size() > 0)
			{
				int inputFileCount = filesListToExport.size();
				if (!onPause)
					inputFileCount++;

				DocumentFile[] inputFiles = new DocumentFile[inputFileCount];

				for (int i = 0; i < filesListToExport.size(); i++)
				{
					inputFiles[i] = filesListToExport.get(i);
				}

				// If video recording hadn't been paused before STOP was
				// pressed, then last recorded file is not in the list with
				// other files, added to list of files manually.
				if (!onPause)
				{
					inputFiles[inputFileCount - 1] = fileSaved;
				}

				resultFile = appendNew(inputFiles);

				// Remove merged files, except first one, because it stores the
				// result of merge.
				for (int i = 0; i < filesListToExport.size(); i++)
				{
					DocumentFile currentFile = filesListToExport.get(i);
					currentFile.delete();
				}

				// If video recording hadn't been paused before STOP was
				// pressed, then last recorded file is not in the list with
				// other files, and should be deleted manually.
				if (!onPause)
					fileSaved.delete();

				String tmpName = resultFile.getName();
				if (resultFile.renameTo(resultName));
				
				// Make sure, that there won't be duplicate broken file
				// in phone memory at gallery.
				String args[] = { tmpName };
				ApplicationScreen.instance.getContentResolver().delete(Video.Media.EXTERNAL_CONTENT_URI,
						Video.Media.DISPLAY_NAME + "=?", args);
			}
			
			String name = resultFile.getName();
			String data = null;
			// If we able to get File object, than get path from it. Gallery
			// doesn't show the file, if it's stored at phone memory and
			// we need insert new file to gallery manually.
			File file = Util.getFileFromDocumentFile(resultFile);
			if (file != null)
			{
				data = file.getAbsolutePath();
			} else {
				// This case should typically happen for files saved to SD
				// card.
				data = Util.getAbsolutePathFromDocumentFile(resultFile);
			}
			
			if (data != null) {
				values.put(VideoColumns.DISPLAY_NAME, name);
				values.put(VideoColumns.DATA, data);
				Uri uri = ApplicationScreen.instance.getContentResolver().insert(Video.Media.EXTERNAL_CONTENT_URI,
						values);
				ApplicationScreen.getMainContext().sendBroadcast(new Intent(ACTION_NEW_VIDEO, uri));
			}
		} else
		{
			File fileSaved = VideoCapturePlugin.fileSaved;
			ArrayList<File> filesListToExport = filesList;

			File firstFile = fileSaved;

			if (filesListToExport.size() > 0)
			{
				firstFile = filesListToExport.get(0);

				int inputFileCount = filesListToExport.size();
				if (!onPause)
					inputFileCount++;

				File[] inputFiles = new File[inputFileCount];

				for (int i = 0; i < filesListToExport.size(); i++)
				{
					inputFiles[i] = filesListToExport.get(i);
				}

				if (!onPause)
					inputFiles[inputFileCount - 1] = fileSaved;

				File resultFile = append(inputFiles);

				for (int i = 0; i < filesListToExport.size(); i++)
				{
					File currentFile = filesListToExport.get(i);
					currentFile.delete();
				}

				if (resultFile != null) {
					if (!resultFile.getAbsoluteFile().equals(fileSaved.getAbsoluteFile()))
					{
						fileSaved.delete();
						resultFile.renameTo(fileSaved);
					}
				}
			}

			filesListToExport.clear();

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isDro)
			{
				DocumentFile outputFile = getOutputMediaFileNew();
				File file = Util.getFileFromDocumentFile(outputFile);

				if (file != null)
				{
					// Don't do anything with ouputFile. It's useless, remove
					// it.
					outputFile.delete();
					Uri uri = ApplicationScreen.instance.getContentResolver().insert(Video.Media.EXTERNAL_CONTENT_URI,
							values);
					ApplicationScreen.getMainContext().sendBroadcast(new Intent(ACTION_NEW_VIDEO, uri));
				} else
				{
					// Copy result file from phone memory to selected folder at
					// SD-card.
					InputStream is = null;
					int len;
					byte[] buf = new byte[4096];
					try
					{
						OutputStream os = ApplicationScreen.instance.getContentResolver().openOutputStream(
								outputFile.getUri());
						is = new FileInputStream(firstFile);
						while ((len = is.read(buf)) > 0)
						{
							os.write(buf, 0, len);
						}
						is.close();
						os.close();
						firstFile.delete();

						// Make sure, that there won't be duplicate broken file
						// in phone memory at gallery.
						String args[] = { firstFile.getAbsolutePath() };
						ApplicationScreen.instance.getContentResolver().delete(Video.Media.EXTERNAL_CONTENT_URI,
								Video.Media.DATA + "=?", args);
						
						String data = Util.getAbsolutePathFromDocumentFile(outputFile);
						if (data != null) {
							values.put(VideoColumns.DATA, data);
							Uri uri = ApplicationScreen.instance.getContentResolver().insert(Video.Media.EXTERNAL_CONTENT_URI,
									values);
							ApplicationScreen.getMainContext().sendBroadcast(new Intent(ACTION_NEW_VIDEO, uri));
						}
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			} else
			{
				Uri uri = ApplicationScreen.instance.getContentResolver().insert(Video.Media.EXTERNAL_CONTENT_URI,
						values);
				ApplicationScreen.getMainContext().sendBroadcast(new Intent(ACTION_NEW_VIDEO, uri));
			}
		}

		try
		{
			Thread.sleep(500);
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED);

	}

	private void startRecording()
	{
//		Camera camera = CameraController.getCamera();
//		if (null == camera)
//			return;

		filesList = new ArrayList<File>();
		filesListNew = new ArrayList<DocumentFile>();

		fileSaved = null;
		fileSavedNew = null;

		if (shutterOff)
			return;

		stopRotateAnimation();
		
		if (!swChecked)
		{
			// RotateImageView additionalButton = (RotateImageView)
			// ApplicationScreen.instance.guiManager.getMainView().findViewById(R.id.buttonShutterAdditional);
			RotateImageView buttonSelectMode = (RotateImageView) ApplicationScreen.instance.guiManager.getMainView()
					.findViewById(R.id.buttonSelectMode);

			// additionalButton.setVisibility(View.VISIBLE);
			buttonSelectMode.setVisibility(View.GONE);
		}

		modeSwitcher.setVisibility(View.GONE);
		if (this.modeDRO())
		{
			shutterOff = true;
			mRecordingStartTime = SystemClock.uptimeMillis();

			ApplicationScreen.getGUIManager().lockControls = true;
			// inform the user that recording has stopped
			isRecording = true;
			onPause = false;

			showRecordingUI(isRecording);
			PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).edit()
					.putBoolean("videorecording", true).commit();

			this.droEngine.startRecording(getOutputMediaFile().toString(), 300);

			new CountDownTimer(1000, 1000)
			{
				public void onTick(long millisUntilFinished)
				{
				}

				public void onFinish()
				{
					shutterOff = false;
					// ApplicationScreen.getGUIManager().lockControls = false;
				}
			}.start();
		} else
		{
			this.startVideoRecording();
		}

		View mainButtonsVideo = (View) ApplicationScreen.instance.guiManager.getMainView().findViewById(
				R.id.mainButtonsVideo);
		mainButtonsVideo.setVisibility(View.VISIBLE);

		View mainButtons = (View) ApplicationScreen.instance.guiManager.getMainView().findViewById(R.id.mainButtons);
		mainButtons.setVisibility(View.INVISIBLE);

		// change shutter icon
		pauseVideoButton.setVisibility(View.VISIBLE);
		pauseVideoButton.setImageResource(R.drawable.plugin_capture_video_pause);

		ApplicationScreen.instance.setKeepScreenOn(true);
	}

	private void stopVideoRecording()
	{
		if (shutterOff)
			return;
//		Camera camera = CameraController.getCamera();
//		if (null == camera)
//		{
//			if(preferenceVideoMuteMode)
//				unmuteAllSounds();
//			return;
//		}

		// stop recording and release camera
		try
		{
			mMediaRecorder.stop(); // stop the recording
		} catch (Exception e)
		{
			if(preferenceVideoMuteMode)
				unmuteAllSounds();
			e.printStackTrace();
			Log.e("video onShutterClick", "mMediaRecorder.stop() exception: " + e.getMessage());
		}

		releaseMediaRecorder(); // release the MediaRecorder object

		// This condition normally should be TRUE only for Android >= 5.
		if (fileSavedNewFd != null)
		{
			try
			{
				fileSavedNewFd.close();
			} catch (IOException e)
			{
				if(preferenceVideoMuteMode)
					unmuteAllSounds();
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		CameraController.stopCameraPreview();
//		Camera.Parameters cp = CameraController.getCameraParameters();
//		if (cp != null)
//		{
			setCameraPreviewSize();
			CameraController.Size sz = new CameraController.Size(ApplicationScreen.getPreviewWidth(),
					ApplicationScreen.getPreviewHeight());
			ApplicationScreen.getGUIManager().setupViewfinderPreviewSize(sz);
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH && videoStabilization)
				CameraController.setVideoStabilization(false);
//		}	
		CameraController.startCameraPreview();

		ApplicationScreen.getGUIManager().lockControls = false;
		// inform the user that recording has stopped
		isRecording = false;
		showRecordingUI(isRecording);
		PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).edit()
				.putBoolean("videorecording", false).commit();

		// change shutter icon
		// if (swChecked)
		// {
		ApplicationScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_START);
		// } else
		// {
		// ApplicationScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_START_WITH_PAUSE);
		// }

		onPreExportVideo();
		Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				doExportVideo();
			}
		};
		new Thread(runnable).start();
		
		if(preferenceVideoMuteMode)
			unmuteAllSounds();
	}

	private void startVideoRecording()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
//		Camera camera = CameraController.getCamera();
//
//		lastCamera = camera;

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH && videoStabilization)
			CameraController.setVideoStabilization(true);

		shutterOff = true;
		mRecordingStartTime = SystemClock.uptimeMillis();

		mMediaRecorder = ApplicationScreen.instance.getMediaRecorder();
		CameraController.stopCameraPreview();
		CameraController.unlockCamera();
		CameraController.configureMediaRecorder(mMediaRecorder);

		// Step 2: Set sources
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		int ImageSizeIdxPreference = Integer.parseInt(prefs.getString(
				CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
						: ApplicationScreen.sImageSizeVideoFrontPref, "2"));

		int quality = 0;
		switch (ImageSizeIdxPreference)
		{
		case 0:
			quality = CamcorderProfile.QUALITY_QCIF;
			break;
		case 1:
			quality = CamcorderProfile.QUALITY_CIF;
			break;
		case 2:
			quality = CamcorderProfile.QUALITY_2160P;
			break;
		case 3:
			quality = CamcorderProfile.QUALITY_1080P;
			break;
		case 4:
			quality = CamcorderProfile.QUALITY_720P;
			break;
		case 5:
			quality = CamcorderProfile.QUALITY_480P;
			break;
		case 6:
			quality = QUALITY_4K;
			break;
		default:
			break;
		}

		if (maxQuality())
		{
			quality = CamcorderProfile.QUALITY_HIGH;
		}

		boolean useProfile = true;
		if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality) && !previewSizes.get(quality))
		{
			ImageSizeIdxPreference = 4;
			quality = CamcorderProfile.QUALITY_720P;
			if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality) && !previewSizes.get(quality))
			{
				ImageSizeIdxPreference = 5;
				quality = CamcorderProfile.QUALITY_480P;

				if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality)
						&& !previewSizes.get(quality))
				{
					ImageSizeIdxPreference = 0;
					quality = CamcorderProfile.QUALITY_QCIF;
					if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality)
							&& !previewSizes.get(quality))
					{
						ImageSizeIdxPreference = 1;
						quality = CamcorderProfile.QUALITY_CIF;
						if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality))
						{
							return;
						}
					} else if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality))
						useProfile = false;
					else
						return;
				} else if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality))
					useProfile = false;
			} else if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality))
				useProfile = false;
		} else if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality))
			useProfile = false;

		Editor editor = prefs.edit();
		editor.putString(CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizeVideoBackPref
				: ApplicationScreen.sImageSizeVideoFrontPref, String.valueOf(ImageSizeIdxPreference));
		editor.commit();

		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
		try
		{

			try
			{
				if (swChecked)
				{
					int qualityTimeLapse = quality;
					// if time lapse activated
					switch (quality)
					{
					case CamcorderProfile.QUALITY_QCIF:
						quality = CamcorderProfile.QUALITY_TIME_LAPSE_QCIF;
						break;
					case CamcorderProfile.QUALITY_CIF:
						quality = CamcorderProfile.QUALITY_TIME_LAPSE_CIF;
						break;
					case CamcorderProfile.QUALITY_2160P:
						quality = CamcorderProfile.QUALITY_TIME_LAPSE_2160P;
						break;
					case CamcorderProfile.QUALITY_1080P:
						quality = CamcorderProfile.QUALITY_TIME_LAPSE_1080P;
						break;
					case CamcorderProfile.QUALITY_720P:
						quality = CamcorderProfile.QUALITY_TIME_LAPSE_720P;
						break;
					case CamcorderProfile.QUALITY_480P:
						quality = CamcorderProfile.QUALITY_TIME_LAPSE_480P;
						break;
					case QUALITY_4K:
						quality = QUALITY_4K;
						break;
					case CamcorderProfile.QUALITY_HIGH:
						quality = CamcorderProfile.QUALITY_TIME_LAPSE_HIGH;
						break;
					default:
						break;
					}
					if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality))
					{
						Toast.makeText(ApplicationScreen.instance, "Time lapse not supported", Toast.LENGTH_LONG)
								.show();
					} else
						quality = qualityTimeLapse;
				}
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e("Video", "Time lapse error catched" + e.getMessage());
				swChecked = false;

				ApplicationScreen.getGUIManager().lockControls = false;

				PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST,
						ApplicationInterface.MSG_CONTROL_UNLOCKED);
			}

			lastUseProfile = useProfile;
			if (useProfile)
			{
				CamcorderProfile pr = CamcorderProfile.get(CameraController.getCameraIndex(), quality);
				mMediaRecorder.setProfile(pr);
				lastCamcorderProfile = pr;
			} else
			{
				boolean useProf = false;
				lastUseProf = useProf;
				CameraController.Size sz = null;
				switch (quality)
				{
				case CamcorderProfile.QUALITY_QCIF:
					sz = new CameraController.Size(176, 144);
					break;
				case CamcorderProfile.QUALITY_CIF:
					sz = new CameraController.Size(352, 288);
					break;
				case CamcorderProfile.QUALITY_2160P:
					sz = new CameraController.Size(3840, 2160);
					break;
				case CamcorderProfile.QUALITY_1080P:
					sz = new CameraController.Size(1920, 1080);
					break;
				case CamcorderProfile.QUALITY_720P:
					sz = new CameraController.Size(1280, 720);
					break;
				case CamcorderProfile.QUALITY_480P:
					sz = new CameraController.Size(640, 480);
					break;
				case QUALITY_4K:
					{
						if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(),
								CamcorderProfile.QUALITY_1080P))
						{
							CamcorderProfile prof = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
							prof.videoFrameHeight = 2160;
							prof.videoFrameWidth = 4096;
							mMediaRecorder.setProfile(prof);
							lastCamcorderProfile = prof;
							useProf = true;
							lastUseProf = useProf;
						} else
							sz = new CameraController.Size(4096, 2160);
					}
					break;
				default:
					break;
				}

				if (!useProf)
				{
					mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
					mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
					mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
					mMediaRecorder.setVideoSize(sz.getWidth(), sz.getHeight());
					lastSz = sz;
				}
			}

			if (swChecked)
			{
				double val1 = Double.valueOf(stringInterval[interval]);
				int val2 = measurementVal;
				switch (val2)
				{
				case 0:
					val2 = 1;
					break;
				case 1:
					val2 = 60;
					break;
				case 2:
					val2 = 3600;
					break;
				default:
					break;
				}
				captureRate = 1 / (val1 * val2);
				mMediaRecorder.setCaptureRate(captureRate);
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("Video", "On shutter pressed " + e.getMessage());

			ApplicationScreen.getGUIManager().lockControls = false;
			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST,
					ApplicationInterface.MSG_CONTROL_UNLOCKED);
			releaseMediaRecorder(); // release the MediaRecorder object
			CameraController.lockCamera(); // take camera access back from MediaRecorder
			CameraController.stopCameraPreview();
			CameraController.startCameraPreview();

			return;
		}

		// Step 4: Set output file
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			DocumentFile file = getOutputMediaFileNew();
			try
			{
				fileSavedNewFd = ApplicationScreen.instance.getContentResolver().openFileDescriptor(file.getUri(), "w");
				FileDescriptor fileDescriptor = fileSavedNewFd.getFileDescriptor();
				mMediaRecorder.setOutputFile(fileDescriptor);
			} catch (FileNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				mMediaRecorder.setOutputFile(getOutputMediaFile().toString());
			}
		} else
		{
			mMediaRecorder.setOutputFile(getOutputMediaFile().toString());
		}

		// Step 5: Set the preview output
		mMediaRecorder.setPreviewDisplay(ApplicationScreen.getPreviewSurfaceHolder().getSurface());

		if (CameraController.isNexus6 && CameraController.isFrontCamera())
		{
			mMediaRecorder.setOrientationHint(ApplicationScreen.getWantLandscapePhoto() ? (ApplicationScreen
					.getGUIManager().getImageDataOrientation() + 180) % 360 : (ApplicationScreen.getGUIManager()
					.getImageDataOrientation()) % 360);
		} else
		{
			mMediaRecorder.setOrientationHint(CameraController.isFrontCamera() ? (ApplicationScreen
					.getWantLandscapePhoto() ? ApplicationScreen.getGUIManager().getImageDataOrientation()
					: (ApplicationScreen.getGUIManager().getImageDataOrientation() + 180) % 360) : ApplicationScreen
					.getGUIManager().getImageDataOrientation());
		}

		// Step 6: Prepare configured MediaRecorder
		try
		{
			if(preferenceVideoMuteMode)
				muteAllSounds();
			mMediaRecorder.prepare();

			// Camera is available and unlocked, MediaRecorder is prepared,
			// now you can start recording
			mMediaRecorder.start();
			ApplicationScreen.getGUIManager().lockControls = true;

		} catch (Exception e)
		{
			Log.d("Video", "Exception preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
			Toast.makeText(ApplicationScreen.instance, "Failed to start video recording", Toast.LENGTH_LONG).show();

			ApplicationScreen.getGUIManager().lockControls = false;
			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST,
					ApplicationInterface.MSG_CONTROL_UNLOCKED);
			CameraController.lockCamera(); // take camera access back from MediaRecorder
			CameraController.stopCameraPreview();
			CameraController.startCameraPreview();
			
			if(preferenceVideoMuteMode)
				unmuteAllSounds();
			
			return;
		}

		// inform the user that recording has started
		isRecording = true;
		showRecordingUI(isRecording);
		if (onPause)
		{
			onPause = false;
			showRecordingUI(isRecording);
			onPause = true;
		}
		prefs.edit().putBoolean("videorecording", true).commit();

		new CountDownTimer(1000, 1000)
		{
			public void onTick(long millisUntilFinished)
			{
			}

			public void onFinish()
			{
				shutterOff = false;
				// ApplicationScreen.getGUIManager().lockControls = false;
			}
		}.start();
	}
	
	protected void muteAllSounds()
	{
		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_ALARM, true);
		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_DTMF, true);
		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_MUSIC, true);
		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_RING, true);
		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_SYSTEM, true);
		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_VOICE_CALL, true);
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(AudioManager.STREAM_DTMF, 0, 0);
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(AudioManager.STREAM_RING, 0, 0);
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0);
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(AudioManager.STREAM_VOICE_CALL, 0, 0);
		
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_MUTE, 0);
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_DTMF, AudioManager.ADJUST_MUTE, 0);
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0);
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_MUTE, 0);
	}
	
	protected void unmuteAllSounds()
	{
		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_ALARM, false);
		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_DTMF, false);
		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_MUSIC, false);
		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_RING, false);
		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_SYSTEM, false);
		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_VOICE_CALL, false);
		
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(AudioManager.STREAM_DTMF, 0, 0);
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(AudioManager.STREAM_RING, 0, 0);
//		((AudioManager)ApplicationScreen.instance.getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0);
	}

	@Override
	public void onPreferenceCreate(PreferenceFragment pf)
	{
		if (pf != null && !ApplicationScreen.getCameraController().isVideoStabilizationSupported())
		{
			PreferenceCategory cat = (PreferenceCategory) pf.findPreference("Pref_VideoCapture_Category");
			CheckBoxPreference cp = (CheckBoxPreference) pf.findPreference("videoStabilizationPref");
			if (cp != null && cat != null)
				cat.removePreference(cp);
		}

	}

	private void getPrefs()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());

		ModePreference = prefs.getString("modeVideoDROPref", "1");

		camera2Preference = prefs.getBoolean(
				ApplicationScreen.getMainContext().getResources().getString(R.string.Preference_UseCamera2Key), false);
		prefs.edit()
				.putBoolean(
						ApplicationScreen.getMainContext().getResources().getString(R.string.Preference_UseCamera2Key),
						false).commit();
		CameraController.setUseCamera2(false);

		if (camera2Preference)
		{
			CameraController.isOldCameraOneModeLaunched = true;
			PluginManager.getInstance().setSwitchModeType(true);
		}

		videoStabilization = prefs.getBoolean("videoStabilizationPref", false);

		readVideoPreferences(prefs);
	}

	@Override
	public boolean shouldPreviewToGPU()
	{
		return this.modeDRO();
	}

	@Override
	public boolean isGLSurfaceNeeded()
	{
		return this.modeDRO();
	}

	private void showRecordingUI(boolean recording)
	{
		if (recording)
		{
			mRecordingTimeView.setRotation(ApplicationScreen.getGUIManager().getDisplayRotation());
			mRecordingTimeView.invalidate();
			if (!onPause)
			{
				mRecordingTimeView.setText("");
				pauseVideoButton.setImageResource(R.drawable.plugin_capture_video_pause);
				pauseBlink = true;
			}
			mRecordingTimeView.setVisibility(View.VISIBLE);
			updateRecordingTime();
		} else
		{
			mRecordingTimeView.setVisibility(View.GONE);
		}
	}

	private void blinkPause()
	{
		if (!onPause)
		{
			return;
		}

		new CountDownTimer(500, 500)
		{
			public void onTick(long millisUntilFinished)
			{
			}

			public void onFinish()
			{
				blinkPause();
			}
		}.start();

		if (pauseBlink)
		{
			pauseVideoButton.setImageResource(R.drawable.plugin_capture_video_pause_transparent);
			pauseBlink = false;
		} else
		{
			pauseVideoButton.setImageResource(R.drawable.plugin_capture_video_pause);
			pauseBlink = true;
		}
	}

	// update recording time indicator.
	private void updateRecordingTime()
	{
		if (!isRecording && !onPause)
		{
			mRecordingTimeView.setText("00:00");
			mRecorded = 0;
			return;
		}

		if (onPause)
		{
			mRecorded = timeStringToMillisecond(mRecordingTimeView.getText().toString());

			blinkPause();

			return;
		}

		long now = SystemClock.uptimeMillis();
		long delta = now - mRecordingStartTime + mRecorded;

		// Starting a minute before reaching the max duration
		// limit, we'll countdown the remaining time instead.
		boolean countdownRemainingTime = (mMaxVideoDurationInMs != 0 && delta >= mMaxVideoDurationInMs - 60000);

		long deltaAdjusted = (long) (delta * captureRate / 24);
		// *captureRate/24 needed for time lapse
		if (countdownRemainingTime)
		{
			deltaAdjusted = Math.max(0, mMaxVideoDurationInMs - deltaAdjusted) + 999;
		}
		String text;

		long targetNextUpdateDelay;
		text = millisecondToTimeString(deltaAdjusted, false);
		targetNextUpdateDelay = 900;

		mRecordingTimeView.setText(text);

		if (mRecordingTimeCountsDown != countdownRemainingTime)
		{
			// Avoid setting the color on every update, do it only
			// when it needs changing.
			mRecordingTimeCountsDown = countdownRemainingTime;

			int color = ApplicationScreen.getAppResources().getColor(R.color.recording_time_remaining_text);

			mRecordingTimeView.setTextColor(color);
		}

		long actualNextUpdateDelay = targetNextUpdateDelay - (delta % targetNextUpdateDelay);

		new CountDownTimer(actualNextUpdateDelay, actualNextUpdateDelay)
		{
			public void onTick(long millisUntilFinished)
			{
			}

			public void onFinish()
			{
				updateRecordingTime();
			}
		}.start();

		// show recording shutter
		if (showRecording)
		{
			stopVideoButton.setImageResource(R.drawable.plugin_capture_video_stop_square);
			showRecording = false;
		} else
		{
			stopVideoButton.setImageResource(R.drawable.plugin_capture_video_stop_square_red);
			showRecording = true;
		}
	}

	private void readVideoPreferences(SharedPreferences prefs)
	{
		Intent intent = ApplicationScreen.instance.getIntent();
		// Set video duration limit. The limit is read from the preference,
		// unless it is specified in the intent.
		if (intent.hasExtra(MediaStore.EXTRA_DURATION_LIMIT))
		{
			int seconds = intent.getIntExtra(MediaStore.EXTRA_DURATION_LIMIT, 0);
			mMaxVideoDurationInMs = 1000 * seconds;
		} else
			mMaxVideoDurationInMs = 0;
	}

	private static long timeStringToMillisecond(String time)
	{
		long res = 0;
		String[] timeSplited = time.split(":");
		if (timeSplited.length > 2)
		{
			res = Long.parseLong(timeSplited[2]) * 1000;
			res += Long.parseLong(timeSplited[1]) * 60 * 1000;
			res += Long.parseLong(timeSplited[0]) * 60 * 60 * 1000;
		} else
		{
			res = Long.parseLong(timeSplited[1]) * 1000;
			res += Long.parseLong(timeSplited[0]) * 60 * 1000;
		}

		return res;
	}

	private static String millisecondToTimeString(long milliSeconds, boolean displayCentiSeconds)
	{
		long seconds = milliSeconds / 1000; // round down to compute seconds
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long remainderMinutes = minutes - (hours * 60);
		long remainderSeconds = seconds - (minutes * 60);

		StringBuilder timeStringBuilder = new StringBuilder();

		// Hours
		if (hours > 0)
		{
			if (hours < 10)
			{
				timeStringBuilder.append('0');
			}
			timeStringBuilder.append(hours);

			timeStringBuilder.append(':');
		}

		// Minutes
		if (remainderMinutes < 10)
		{
			timeStringBuilder.append('0');
		}
		timeStringBuilder.append(remainderMinutes);
		timeStringBuilder.append(':');

		// Seconds
		if (remainderSeconds < 10)
		{
			timeStringBuilder.append('0');
		}
		timeStringBuilder.append(remainderSeconds);

		// Centi seconds
		if (displayCentiSeconds)
		{
			timeStringBuilder.append('.');
			long remainderCentiSeconds = (milliSeconds - seconds * 1000) / 10;
			if (remainderCentiSeconds < 10)
			{
				timeStringBuilder.append('0');
			}
			timeStringBuilder.append(remainderCentiSeconds);
		}

		return timeStringBuilder.toString();
	}

	@Override
	public void onAutoFocus(boolean paramBoolean)
	{
	}

	private void pauseVideoRecording()
	{
//		Camera camera = CameraController.getCamera();
//		if (null == camera)
//			return;

		if (!isRecording)
			return;

		// Continue video recording
		if (this.modeDRO())
		{
			long now = SystemClock.uptimeMillis();
			long delta = now - mRecordingStartTime;
			Handler handler = new Handler();
			handler.postDelayed(new Runnable()
			{
				public void run()
				{
					pauseDRORecording();
				}
			}, 1500 - delta);
		} else
		{
			if (lockPauseButton)
			{
				return;
			}

			if (onPause)
			{
				startVideoRecording();
				onPause = false;

			}
			// Pause video recording, merge files and remove last.
			else
			{
				lockPauseButton = true;
				long now = SystemClock.uptimeMillis();
				long delta = now - mRecordingStartTime;
				Handler handler = new Handler();
				handler.postDelayed(new Runnable()
				{
					public void run()
					{
						pauseRecording();
						Toast.makeText(ApplicationScreen.instance,
								ApplicationScreen.instance.getString(R.string.video_paused), Toast.LENGTH_SHORT).show();
					}
				}, 1500 - delta);
			}
		}
	}

	private void pauseDRORecording()
	{
		if (onPause)
		{
			mRecordingStartTime = SystemClock.uptimeMillis();
			showRecordingUI(isRecording);
			onPause = false;
			showRecordingUI(isRecording);

			pauseVideoButton.setImageResource(R.drawable.plugin_capture_video_pause);
		} else
		{
			onPause = true;
			stopVideoButton.setImageResource(R.drawable.plugin_capture_video_stop_square);
			pauseVideoButton.setImageResource(R.drawable.plugin_capture_video_pause_transparent);
			Toast.makeText(ApplicationScreen.instance, ApplicationScreen.instance.getString(R.string.video_paused),
					Toast.LENGTH_SHORT).show();
		}
		this.droEngine.setPaused(this.onPause);
	}

	private void pauseRecording()
	{
		onPause = true;
		// TODO PAUSE
		// ApplicationScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_PAUSED);
		try
		{
			// stop recording and release camera
			try
			{
				mMediaRecorder.stop(); // stop the recording
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e("video pauseVideoRecording", "mMediaRecorder.stop() exception: " + e.getMessage());
			}

			releaseMediaRecorder(); // release the MediaRecorder object

			String name = "";
			String data = "";

			// This condition normally should be TRUE only for Android >= 5.
			if (fileSavedNew != null)
			{
				if (fileSavedNewFd != null)
				{
					try
					{
						fileSavedNewFd.close();
					} catch (IOException e)
					{
						e.printStackTrace();
					}
				}
				name = fileSavedNew.getName();
				data = null;

				// If we able to get File object, than get path from it. Gallery
				// doesn't show file, if it's stored at phone memory and
				// DATA field not set.
				File file = Util.getFileFromDocumentFile(fileSavedNew);
				if (file != null)
				{
					data = file.getAbsolutePath();
				} else {
					// This case should typically happen for files saved to SD
					// card.
					data = Util.getAbsolutePathFromDocumentFile(fileSavedNew);
				}
				filesListNew.add(fileSavedNew);
			} else
			{
				name = fileSaved.getName();
				data = fileSaved.getAbsolutePath();
				filesList.add(fileSaved);
			}

			values = new ContentValues();
			values.put(VideoColumns.TITLE, name.substring(0, name.lastIndexOf(".")));
			values.put(VideoColumns.DISPLAY_NAME, name);
			values.put(VideoColumns.DATE_TAKEN, System.currentTimeMillis());
			values.put(VideoColumns.MIME_TYPE, "video/mp4");
			if (data != null)
			{
				values.put(VideoColumns.DATA, data);
			}
			values.put(VideoColumns.DURATION, timeStringToMillisecond(mRecordingTimeView.getText().toString()));

			if (lastUseProfile)
			{
				values.put(
						VideoColumns.RESOLUTION,
						String.valueOf(lastCamcorderProfile.videoFrameWidth) + "x"
								+ String.valueOf(lastCamcorderProfile.videoFrameHeight));
			} else
			{
				values.put(VideoColumns.RESOLUTION,
						String.valueOf(lastSz.getWidth()) + "x" + String.valueOf(lastSz.getHeight()));
			}

			lockPauseButton = false;

			stopVideoButton.setImageResource(R.drawable.plugin_capture_video_stop_square);
			pauseVideoButton.setImageResource(R.drawable.plugin_capture_video_pause_transparent);
		} catch (RuntimeException e)
		{
			// Note that a RuntimeException is intentionally thrown to the
			// application,
			// if no valid audio/video data has been received when stop() is
			// called.
			// This happens if stop() is called immediately after start().
			// The failure lets the application take action accordingly to clean
			// up the output file (delete the output file,
			// for instance), since the output file is not properly constructed
			// when this happens.
			if (fileSavedNew != null)
			{
				fileSavedNew.delete();
			} else
			{
				fileSaved.delete();
			}
			e.printStackTrace();
		}
	}

	/**
	 * Appends mp4 audio/video from {@code anotherFileDescriptor} to
	 * {@code mainFileDescriptor}.
	 */
	public static DocumentFile appendNew(DocumentFile[] inputFiles)
	{
		try
		{
			DocumentFile targetFile = inputFiles[0];
			int[] inputFilesFds = new int[inputFiles.length];
			ArrayList<ParcelFileDescriptor> pfdsList = new ArrayList<ParcelFileDescriptor>();

			int i = 0;
			for (DocumentFile f : inputFiles)
			{
				ParcelFileDescriptor pfd = ApplicationScreen.instance.getContentResolver().openFileDescriptor(
						f.getUri(), "rw");
				pfdsList.add(pfd);
				inputFilesFds[i] = pfd.getFd();
				i++;
			}

			if (targetFile.exists() && targetFile.length() > 0)
			{
				String tmpFileName = targetFile.getName() + ".tmp";
				DocumentFile tmpTargetFile = targetFile.getParentFile().createFile("video/mp4", tmpFileName);
				ParcelFileDescriptor targetFilePfd = ApplicationScreen.instance.getContentResolver()
						.openFileDescriptor(tmpTargetFile.getUri(), "rw");
				
				Mp4Editor.appendFds(inputFilesFds, targetFilePfd.getFd());

				targetFilePfd.close();
				for (ParcelFileDescriptor pfd : pfdsList)
				{
					pfd.close();
				}

				return tmpTargetFile;
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Appends mp4 audio/video from {@code anotherFileName} to
	 * {@code mainFileName}.
	 */
	public static File append(File[] inputFiles)
	{
		try
		{
			File targetFile = inputFiles[0];
			int[] inputFilesFds = new int[inputFiles.length];
			ArrayList<ParcelFileDescriptor> pfdsList = new ArrayList<ParcelFileDescriptor>();

			int i = 0;
			for (File f : inputFiles)
			{
				ParcelFileDescriptor pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_WRITE);
				pfdsList.add(pfd);
				inputFilesFds[i] = pfd.getFd();
				i++;
			}

			if (targetFile.exists() && targetFile.length() > 0)
			{
				File tmpTargetFile = new File(targetFile.getAbsolutePath() + ".tmp");

				ParcelFileDescriptor targetFilePfd = ParcelFileDescriptor.open(tmpTargetFile,
						ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_READ_WRITE);
				
				Mp4Editor.appendFds(inputFilesFds, targetFilePfd.getFd());

				targetFilePfd.close();
				for (ParcelFileDescriptor pfd : pfdsList)
				{
					pfd.close();
				}

				return tmpTargetFile;
			} else if (targetFile.createNewFile())
			{
				copyFile(inputFiles[1].getAbsolutePath(), inputFiles[0].getAbsolutePath());
				return targetFile;
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public static void copyFile(final String from, final String destination) throws IOException
	{
		FileInputStream in = new FileInputStream(from);
		FileOutputStream out = new FileOutputStream(destination);
		copy(in, out);
		in.close();
		out.close();
	}

	public static void copy(FileInputStream in, FileOutputStream out) throws IOException
	{
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0)
		{
			out.write(buf, 0, len);
		}
	}

	public void takePicture()
	{
		// Do nothing if capture was started and not stopped yet.
		if (inCapture) {
			return;
		}
		
		inCapture = true;
		SessionID = System.currentTimeMillis();
		createRequestIDList(1);
		CameraController.captureImagesWithParams(1, CameraController.JPEG, null, null, null, null, false, true, true);
	}

	// timelapse values
	public int		interval			= 0;
	public int		measurementVal		= 0;
	public boolean	swChecked			= false;

	String[]		stringInterval		= { "0.1", "0.2", "0.3", "0.4", "0.5", "1", "1.5", "2", "2.5", "3", "4", "5",
			"6", "10", "12", "15", "24" };
	String[]		stringMeasurement	= { "seconds", "minutes", "hours" };

	public void TimeLapseDialog()
	{
		if (isRecording)
			return;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		interval = Integer.valueOf(prefs.getString("timelapseInterval", "0"));
		measurementVal = Integer.valueOf(prefs.getString("timelapseMeasurementVal", "0"));

		// show time lapse settings
		timeLapseDialog = new TimeLapseDialog(ApplicationScreen.instance);
		timeLapseDialog.setContentView(R.layout.plugin_capture_video_timelapse_dialog);
		final NumberPicker np = (NumberPicker) timeLapseDialog.findViewById(R.id.numberPicker1);
		np.setMaxValue(16);
		np.setMinValue(0);
		np.setValue(interval);
		np.setDisplayedValues(stringInterval);
		np.setWrapSelectorWheel(false);
		np.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

		final NumberPicker np2 = (NumberPicker) timeLapseDialog.findViewById(R.id.numberPicker2);
		np2.setMaxValue(2);
		np2.setMinValue(0);
		np2.setValue(measurementVal);
		np2.setWrapSelectorWheel(false);
		np2.setDisplayedValues(stringMeasurement);
		np2.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

		final Switch sw = (Switch) timeLapseDialog.findViewById(R.id.timelapse_switcher);

		// disable/enable controls in dialog
		sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (!sw.isChecked())
				{
					swChecked = false;
				} else
				{
					swChecked = true;
				}
			}
		});

		np2.setOnScrollListener(new NumberPicker.OnScrollListener()
		{
			@Override
			public void onScrollStateChange(NumberPicker numberPicker, int scrollState)
			{
				sw.setChecked(true);
			}
		});
		np.setOnScrollListener(new NumberPicker.OnScrollListener()
		{
			@Override
			public void onScrollStateChange(NumberPicker numberPicker, int scrollState)
			{
				sw.setChecked(true);
			}
		});

		// disable control in dialog by default
		if (!swChecked)
		{
			sw.setChecked(false);
		} else
		{
			sw.setChecked(true);
		}

		timeLapseDialog.setOnDismissListener(new OnDismissListener()
		{
			@Override
			public void onDismiss(DialogInterface dialog)
			{
				if (swChecked)
				{
					measurementVal = np2.getValue();
					interval = np.getValue();

					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen
							.getMainContext());
					Editor editor = prefs.edit();
					editor.putString("timelapseMeasurementVal", String.valueOf(measurementVal));
					editor.putString("timelapseInterval", String.valueOf(interval));
					editor.commit();

					timeLapseButton.setImageResource(R.drawable.plugin_capture_video_timelapse_active);

					ApplicationScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_START);
				} else
				{
					timeLapseButton.setImageResource(R.drawable.plugin_capture_video_timelapse_inactive);
					ApplicationScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_START);
				}

			}
		});
		timeLapseDialog.show();
	}

	@Override
	public void onImageTaken(int frame, byte[] frameData, int frame_len, int format)
	{
		PluginManager.getInstance().addToSharedMem("frame1" + SessionID, String.valueOf(frame));
		PluginManager.getInstance().addToSharedMem("framelen1" + SessionID, String.valueOf(frame_len));
		PluginManager.getInstance().addToSharedMem("frameorientation1" + SessionID,
				String.valueOf(ApplicationScreen.getGUIManager().getImageDataOrientation()));
		PluginManager.getInstance().addToSharedMem("framemirrored1" + SessionID,
				String.valueOf(CameraController.isFrontCamera()));

		PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID, "1");

		if (format == CameraController.JPEG && frameData != null)
			PluginManager.getInstance().addToSharedMemExifTagsFromJPEG(frameData, SessionID, -1);

		try
		{
			CameraController.startCameraPreview();
		} catch (RuntimeException e)
		{
			Log.i("View capture still image", "StartPreview fail");
		}

		PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));
		
		inCapture = false;
	}

	private int			frameCnt	= 0;
	private long		timeStart	= 0;
	private long		time		= 0;
	private final int	MIN_FPS		= 12;

	@Override
	public void onPreviewFrame(byte[] data)
	{
	}

	@Override
	public void onFrameAvailable()
	{
		if (frameCnt <= 50)
		{
			// check if FPS rate higher than minimum allowed
			if (frameCnt == 0)
				timeStart = System.currentTimeMillis();
			frameCnt++;
			// frames number
			if (frameCnt == 50)
			{
				time = (System.currentTimeMillis() - timeStart);
				long fps = (1000 * frameCnt) / time;
				if (fps < MIN_FPS)
					showHDRWarning(fps);
			}
		}
		this.droEngine.onFrameAvailable();
	}

	@Override
	public void onGLSurfaceCreated(final GL10 gl, final EGLConfig config)
	{
		this.droEngine.onSurfaceCreated(gl, config);
	}

	@Override
	public void onGLSurfaceChanged(final GL10 gl, final int width, final int height)
	{
		this.droEngine.onSurfaceChanged(gl, width, height);
	}

	@Override
	public void onGLDrawFrame(final GL10 gl)
	{
		this.droEngine.onDrawFrame(gl);
	}

	private void showHDRWarning(long fps)
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen
				.getMainContext());
		boolean showDroWarning = prefs.getBoolean("dontshowagainDroWarning", false);

		if (showDroWarning)
			return;
		AlertDialog.Builder builder = new AlertDialog.Builder(ApplicationScreen.instance);
		builder.setTitle("HDR Video");
		builder.setMessage(ApplicationScreen.getAppResources().getString(R.string.dro_warning) + " " + fps);
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.cancel();
			}
		});
		builder.setNegativeButton(ApplicationScreen.getAppResources().getString(R.string.helpTextDontShow),
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						prefs.edit().putBoolean("dontshowagainDroWarning", true).commit();
					}
				});
		builder.show();
	}
}
