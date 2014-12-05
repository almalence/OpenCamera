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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;
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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.almalence.ui.RotateImageView;
import com.almalence.util.Util;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

/* <!-- +++
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.CameraParameters;
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginCapture;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.ui.AlmalenceGUI.ShutterButton;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.ui.AlmalenceGUI.ShutterButton;

//-+- -->

/***
 * Implements basic functionality of Video capture.
 ***/

public class VideoCapturePlugin extends PluginCapture
{
	private static final String					TAG								= "Almalence";

	private volatile boolean					isRecording;
	private boolean								onPause;
	private boolean								lockPauseButton					= false;
	private int									soundVolume						= 0;

	private MediaRecorder						mMediaRecorder;

	private long								mRecordingStartTime;

	// The video duration limit. 0 means no limit.
	private int									mMaxVideoDurationInMs;

	// video duration text view
	private TextView							mRecordingTimeView;
	private long								mRecorded;

	private boolean								mRecordingTimeCountsDown		= false;

	private boolean								shutterOff						= false;

	private static File							fileSaved						= null;
	private ArrayList<File>						filesList						= new ArrayList<File>();

	private int									preferenceFocusMode;

	private RotateImageView						timeLapseButton;
	private RotateImageView						pauseVideoButton;
	private RotateImageView						stopVideoButton;
	private RotateImageView						takePictureButton;

	private boolean								showRecording					= false;
	private boolean								pauseBlink						= true;

	private View								buttonsLayout;

	private boolean								snapshotSupported				= false;

	private boolean								videoStabilization				= false;

	public static final int						QUALITY_4K						= 4096;

	private ImageView							rotateToLandscapeNotifier;
	private boolean								showRotateToLandscapeNotifier	= false;
	private boolean								showLandscapeNotification		= true;
	private View								rotatorLayout;
	private TimeLapseDialog						timeLapseDialog;

	private boolean								displayTakePicture;
	private ContentValues						values;

	private static Hashtable<Integer, Boolean>	previewSizes					= new Hashtable<Integer, Boolean>()
																				{
																					/**
		 * 
		 */
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
																						put(QUALITY_4K, false);
																					}
																				};

	private boolean								qualityCIFSupported				= false;
	private boolean								qualityQCIFSupported			= false;
	private boolean								quality480Supported				= false;
	private boolean								quality720Supported				= false;
	private boolean								quality1080Supported			= false;
	private boolean								quality4KSupported				= false;

	private volatile String						ModePreference;											// 0=DRO
																											// On
																											// 1=DRO
																											// Off
	private boolean								camera2Preference;

	private com.almalence.ui.Switch.Switch		modeSwitcher;

	private DROVideoEngine						droEngine						= new DROVideoEngine();

	public VideoCapturePlugin()
	{
		super("com.almalence.plugins.videocapture", R.xml.preferences_capture_video, 0,
				R.drawable.gui_almalence_video_1080, "Video quality");
	}

	@Override
	public void onCreate()
	{
		mRecordingTimeView = new TextView(MainScreen.getMainContext());
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
		LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
		modeSwitcher = (com.almalence.ui.Switch.Switch) inflator.inflate(R.layout.plugin_capture_standard_modeswitcher,
				null, false);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		ModePreference = prefs.getString("modeVideoDROPref", "1");
		modeSwitcher.setTextOn(MainScreen.getInstance().getString(R.string.Pref_Video_DRO_ON));
		modeSwitcher.setTextOff(MainScreen.getInstance().getString(R.string.Pref_Video_DRO_OFF));
		modeSwitcher.setChecked(ModePreference.compareTo("0") == 0 ? true : false);
		modeSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

				if (isChecked)
				{
					ModePreference = "0";
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
							.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref : MainScreen.sImageSizeVideoFrontPref, "2"));
					if (ImageSizeIdxPreference == 2)
					{
						quickControlIconID = R.drawable.gui_almalence_video_720;
						editor.putString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref
								: MainScreen.sImageSizeVideoFrontPref, "3");
						editor.commit();
						VideoCapturePlugin.this.refreshQuickControl();
					}
				}

				try
				{
					CameraController.stopCameraPreview();
					Camera.Parameters cp = CameraController.getCameraParameters();
					if (cp != null)
					{
						setCameraPreviewSize();
						CameraController.Size sz = new CameraController.Size(
								MainScreen.getPreviewWidth(), MainScreen.getPreviewHeight());
						MainScreen.getGUIManager().setupViewfinderPreviewSize(sz);
					}
					if (VideoCapturePlugin.this.modeDRO())
					{
						takePictureButton.setVisibility(View.GONE);
						timeLapseButton.setVisibility(View.GONE);
						MainScreen.getInstance().showOpenGLLayer(2);
						MainScreen.getInstance().glSetRenderingMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
					} else
					{
						if (displayTakePicture)
							takePictureButton.setVisibility(View.VISIBLE);
						timeLapseButton.setVisibility(View.VISIBLE);

						droEngine.onPause();

						Camera camera = CameraController.getCamera();
						if (camera != null)
							try
							{
								camera.setDisplayOrientation(90);
							} catch (RuntimeException e)
							{
								e.printStackTrace();
							}

						if (camera != null)
							try
							{
								camera.setPreviewDisplay(MainScreen.getPreviewSurfaceHolder());
							} catch (IOException e)
							{
								e.printStackTrace();
							}
						CameraController.startCameraPreview();
						MainScreen.getInstance().hideOpenGLLayer();
					}
				} catch (final Exception e)
				{
					Log.e(TAG, Util.toString(e.getStackTrace(), '\n'));
					e.printStackTrace();
				}
			}
		});

		if (PluginManager.getInstance().getProcessingCounter() == 0)
			modeSwitcher.setEnabled(true);
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

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

		// change shutter icon
		isRecording = false;
		prefs.edit().putBoolean("videorecording", false).commit();

		// if (swChecked)
		// {
		MainScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_START);
		// }
		// else
		// {
		// MainScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_START_WITH_PAUSE);
		// }

		onPreferenceCreate((PreferenceFragment) null);

		int ImageSizeIdxPreference = Integer.parseInt(prefs.getString(
				CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref : MainScreen.sImageSizeVideoFrontPref, "2"));
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
				quality = CamcorderProfile.QUALITY_1080P;
				quickControlIconID = R.drawable.gui_almalence_video_1080;
			}
			break;
		case 3:
			quality = CamcorderProfile.QUALITY_720P;
			quickControlIconID = R.drawable.gui_almalence_video_720;
			break;
		case 4:
			quality = CamcorderProfile.QUALITY_480P;
			quickControlIconID = R.drawable.gui_almalence_video_480;
			break;
		case 5:
			quality = QUALITY_4K;
			quickControlIconID = R.drawable.gui_almalence_video_4096;
			break;
		default:
			break;
		}

		if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality) && !previewSizes.get(quality))
		{
			ImageSizeIdxPreference = 3;
			quality = CamcorderProfile.QUALITY_720P;
			quickControlIconID = R.drawable.gui_almalence_video_720;
			if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality) && !previewSizes.get(quality))
			{
				ImageSizeIdxPreference = 4;
				quality = CamcorderProfile.QUALITY_480P;
				quickControlIconID = R.drawable.gui_almalence_video_480;
			}
		}

		Editor editor = prefs.edit();
		editor.putString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref : MainScreen.sImageSizeVideoFrontPref,
				String.valueOf(ImageSizeIdxPreference));
		editor.commit();

		List<View> specialView = new ArrayList<View>();
		RelativeLayout specialLayout = (RelativeLayout) MainScreen.getInstance().findViewById(
				R.id.specialPluginsLayout2);
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

			((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout3))
					.removeView(this.modeSwitcher);
			((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout3)).addView(
					this.modeSwitcher, params);

			this.modeSwitcher.setLayoutParams(params);
			// this.modeSwitcher.requestLayout();
		}

		// Calculate right sizes for plugin's controls
		DisplayMetrics metrics = new DisplayMetrics();
		MainScreen.getInstance().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		float fScreenDensity = metrics.density;

		int iIndicatorSize = (int) (MainScreen.getMainContext().getResources().getInteger(R.integer.infoControlHeight) * fScreenDensity);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(iIndicatorSize, iIndicatorSize);
		int topMargin = MainScreen.getInstance().findViewById(R.id.paramsLayout).getHeight()
				+ (int) MainScreen.getAppResources().getDimension(R.dimen.viewfinderViewsMarginTop);
		params.setMargins((int) (2 * MainScreen.getGUIManager().getScreenDensity()), topMargin, 0, 0);

		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout2)).addView(
				this.mRecordingTimeView, params);

		this.mRecordingTimeView.setLayoutParams(params);
		// this.mRecordingTimeView.requestLayout();

		// ((RelativeLayout)
		// MainScreen.getInstance().findViewById(R.id.specialPluginsLayout2)).requestLayout();

		LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
		buttonsLayout = inflator.inflate(R.layout.plugin_capture_video_layout, null, false);
		buttonsLayout.setVisibility(View.VISIBLE);

		timeLapseButton = (RotateImageView) buttonsLayout.findViewById(R.id.buttonTimeLapse);
		pauseVideoButton = (RotateImageView) MainScreen.getInstance().findViewById(R.id.buttonVideoPause);
		stopVideoButton = (RotateImageView) MainScreen.getInstance().findViewById(R.id.buttonVideoStop);
		Camera camera = CameraController.getCamera();
		if (camera != null)
		{
			Camera.Parameters cp = CameraController.getCameraParameters();
			if (cp != null)
			{
				if (cp.isVideoSnapshotSupported())
					snapshotSupported = true;
			}
		}
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
		params.height = (int) MainScreen.getAppResources().getDimension(R.dimen.videobuttons_size);

		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout2)).addView(
				this.buttonsLayout, params);

		this.buttonsLayout.setLayoutParams(params);
		// this.buttonsLayout.requestLayout();
		//
		// ((RelativeLayout)
		// MainScreen.getInstance().findViewById(R.id.specialPluginsLayout2)).requestLayout();

		if (snapshotSupported)
		{
			takePictureButton.setOrientation(MainScreen.getGUIManager().getLayoutOrientation());
			takePictureButton.invalidate();
			// takePictureButton.requestLayout();
			displayTakePicture = true;
		} else
		{
			takePictureButton.setVisibility(View.GONE);
			displayTakePicture = false;
		}

		timeLapseButton.setOrientation(MainScreen.getGUIManager().getLayoutOrientation());
		// timeLapseButton.invalidate();
		// timeLapseButton.requestLayout();

		if (this.modeDRO())
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
						MainScreen.getInstance().startActivity(intent);
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						// No button clicked
						break;
					default:
						break;
					}
				}
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(MainScreen.getInstance());
			builder.setMessage("You selected to start standard camera. Start camera?")
					.setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener).show();
		}

		rotatorLayout = inflator.inflate(R.layout.plugin_capture_video_lanscaperotate_layout, null, false);
		rotatorLayout.setVisibility(View.VISIBLE);

		rotateToLandscapeNotifier = (ImageView) rotatorLayout.findViewById(R.id.rotatorImageView);

		List<View> specialViewRotator = new ArrayList<View>();
		RelativeLayout specialLayoutRotator = (RelativeLayout) MainScreen.getInstance().findViewById(
				R.id.specialPluginsLayout);
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
		paramsRotator.height = (int) MainScreen.getAppResources().getDimension(R.dimen.gui_element_2size);

		paramsRotator.addRule(RelativeLayout.CENTER_IN_PARENT);

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout)).addView(this.rotatorLayout,
				paramsRotator);

		// rotatorLayout.setLayoutParams(paramsRotator);
		// rotatorLayout.requestLayout();

		// ((RelativeLayout)
		// MainScreen.getInstance().findViewById(R.id.specialPluginsLayout)).requestLayout();
	}

	@Override
	public void onQuickControlClick()
	{
		if (isRecording)
			return;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		Editor editor = prefs.edit();

		int ImageSizeIdxPreference = Integer.parseInt(prefs.getString(
				CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref : MainScreen.sImageSizeVideoFrontPref, "2"));

		int quality = 0;
		switch (ImageSizeIdxPreference)
		{
		case 0:
			quality = CamcorderProfile.QUALITY_CIF;
			quickControlIconID = R.drawable.gui_almalence_video_cif;
			editor.putString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref
					: MainScreen.sImageSizeVideoFrontPref, "1");
			break;
		case 1:
			if (this.modeDRO())
			{
				quality = CamcorderProfile.QUALITY_720P;
				quickControlIconID = R.drawable.gui_almalence_video_720;
				editor.putString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref
						: MainScreen.sImageSizeVideoFrontPref, "3");
			} else
			{
				quality = CamcorderProfile.QUALITY_1080P;
				quickControlIconID = R.drawable.gui_almalence_video_1080;
				editor.putString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref
						: MainScreen.sImageSizeVideoFrontPref, "2");
			}
			break;
		case 2:
			quality = CamcorderProfile.QUALITY_720P;
			quickControlIconID = R.drawable.gui_almalence_video_720;
			editor.putString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref
					: MainScreen.sImageSizeVideoFrontPref, "3");
			break;
		case 3:
			quality = CamcorderProfile.QUALITY_480P;
			quickControlIconID = R.drawable.gui_almalence_video_480;
			editor.putString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref
					: MainScreen.sImageSizeVideoFrontPref, "4");
			break;
		case 4:
			quality = CamcorderProfile.QUALITY_QCIF;
			quickControlIconID = R.drawable.gui_almalence_video_qcif;
			editor.putString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref
					: MainScreen.sImageSizeVideoFrontPref, "0");
			break;
		case 5:
			quality = QUALITY_4K;
			quickControlIconID = R.drawable.gui_almalence_video_4096;
			editor.putString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref
					: MainScreen.sImageSizeVideoFrontPref, "5");
			break;
		default:
			break;
		}

		editor.commit();

		if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality) && !previewSizes.get(quality))
		{
			ImageSizeIdxPreference = (Integer
					.parseInt(prefs.getString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref
							: MainScreen.sImageSizeVideoFrontPref, "2")) + 1) % 5;
			editor.putString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref
					: MainScreen.sImageSizeVideoFrontPref, String.valueOf(ImageSizeIdxPreference));
			onQuickControlClick();
		}

		CameraController.stopCameraPreview();
		Camera.Parameters cp = CameraController.getCameraParameters();
		if (cp != null)
		{
			setCameraPreviewSize();
			Camera.Size sz = CameraController.getCameraParameters().getPreviewSize();
			MainScreen.getGUIManager().setupViewfinderPreviewSize(
					new CameraController.Size(sz.width, sz.height));
		}
		CameraController.startCameraPreview();

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_PREVIEW_CHANGED);
	}

	@Override
	public void onOrientationChanged(int orientation)
	{
		if (mRecordingTimeView != null)
		{
			mRecordingTimeView.setRotation(MainScreen.getGUIManager().getDisplayRotation());
			mRecordingTimeView.invalidate();
		}
		if (snapshotSupported)
		{
			if (takePictureButton != null)
			{
				takePictureButton.setOrientation(MainScreen.getGUIManager().getLayoutOrientation());
				// takePictureButton.invalidate();
				// takePictureButton.requestLayout();
			}
		}
		if (timeLapseButton != null)
		{
			timeLapseButton.setOrientation(MainScreen.getGUIManager().getLayoutOrientation());
			// timeLapseButton.invalidate();
			// timeLapseButton.requestLayout();
		}

		if (rotatorLayout != null && showLandscapeNotification)
		{
			if (!isRecording && (orientation == 90 || orientation == 270))
			{
				showRotateToLandscapeNotifier = true;
				startrotateAnimation();
				rotatorLayout.findViewById(R.id.rotatorImageView).setVisibility(View.VISIBLE);
				rotatorLayout.findViewById(R.id.rotatorInnerImageView).setVisibility(View.VISIBLE);
			} else
			{
				showRotateToLandscapeNotifier = false;
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
			timeLapseDialog.setRotate(MainScreen.getGUIManager().getLayoutOrientation());
		}
	}

	@Override
	public boolean muteSound()
	{
		return true;
	}

	public void startrotateAnimation()
	{
		try
		{
			if (rotateToLandscapeNotifier != null && rotateToLandscapeNotifier.getVisibility() == View.VISIBLE)
				return;

			int height = (int) MainScreen.getAppResources().getDimension(R.dimen.gui_element_2size);
			Animation rotation = new RotateAnimation(0, -180, height / 2, height / 2);
			rotation.setDuration(2000);
			rotation.setRepeatCount(1000);
			rotation.setInterpolator(new DecelerateInterpolator());

			rotateToLandscapeNotifier.startAnimation(rotation);
		} catch (Exception e)
		{
		}
	}

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

	public void onResume()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		boolean preferenceVideoMuteMode = prefs.getBoolean("preferenceVideoMuteMode", false);
		if (preferenceVideoMuteMode)
		{
			AudioManager audioMgr = (AudioManager) MainScreen.getInstance().getSystemService(Context.AUDIO_SERVICE);
			soundVolume = audioMgr.getStreamVolume(AudioManager.STREAM_RING);
			audioMgr.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
		}

		preferenceFocusMode = prefs.getInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref
				: MainScreen.sFrontFocusModePref, CameraParameters.AF_MODE_AUTO);

		PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).edit()
				.putBoolean("ContinuousCapturing", true).commit();
		
		shutterOff = false;
		showRecording = false;

		swChecked = false;
		interval = 0;
		measurementVal = 0;

		if (this.shouldPreviewToGPU())
		{
			MainScreen.getMessageHandler().sendEmptyMessage(PluginManager.MSG_OPENGL_LAYER_SHOW_V2);
			MainScreen.getMessageHandler().sendEmptyMessage(PluginManager.MSG_OPENGL_LAYER_RENDERMODE_WHEN_DIRTY);
		}

		showLandscapeNotification = prefs.getBoolean("showLandscapeNotification", true);

		frameCnt = 0;
	}

	@Override
	public void onPause()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		prefs.edit()
				.putInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref
						: MainScreen.sFrontFocusModePref, preferenceFocusMode).commit();

		prefs.edit()
		.putBoolean(MainScreen.getMainContext().getResources().getString(R.string.Preference_UseHALv3Key),
				camera2Preference).commit();

		Camera camera = CameraController.getCamera();
		if (null == camera)
			return;

		if (this.isRecording)
		{
			stopRecording();
		}

		if (camera != null)
		{
			try
			{
				Camera.Parameters cp = CameraController.getCameraParameters();
				cp.setRecordingHint(false);
				CameraController.setCameraParameters(cp);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		if (this.buttonsLayout != null)
		{
			MainScreen.getGUIManager().removeViews(buttonsLayout, R.id.specialPluginsLayout2);
		}

		PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).edit()
				.putBoolean("ContinuousCapturing", false).commit();

		if (this.rotatorLayout != null)
		{
			MainScreen.getGUIManager().removeViews(rotatorLayout, R.id.specialPluginsLayout);
		}

		if (this.modeDRO())
		{
			this.droEngine.onPause();
		}

		boolean preferenceVideoMuteMode = prefs.getBoolean("preferenceVideoMuteMode", false);
		if (preferenceVideoMuteMode)
		{
			AudioManager audioMgr = (AudioManager) MainScreen.getInstance().getSystemService(Context.AUDIO_SERVICE);
			audioMgr.setStreamVolume(AudioManager.STREAM_RING, soundVolume, 0);
		}
	}
	

	@Override
	public void onStop()
	{
		MainScreen.getGUIManager().removeViews(modeSwitcher, R.id.specialPluginsLayout3);
		
		if(camera2Preference)
			CameraController.needCameraRelaunch(true);
	}

	@Override
	public void onCameraParametersSetup()
	{
		this.qualityQCIFSupported = false;
		this.qualityCIFSupported = false;
		this.quality480Supported = false;
		this.quality720Supported = false;
		this.quality1080Supported = false;
		this.quality4KSupported = false;
		previewSizes.put(CamcorderProfile.QUALITY_QCIF, false);
		previewSizes.put(CamcorderProfile.QUALITY_CIF, false);
		previewSizes.put(CamcorderProfile.QUALITY_480P, false);
		previewSizes.put(CamcorderProfile.QUALITY_720P, false);
		previewSizes.put(CamcorderProfile.QUALITY_1080P, false);
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
		if (psz.contains(new CameraController.Size(1920, 1080))
				|| psz.contains(new CameraController.Size(1920, 1088)))
		{
			previewSizes.put(CamcorderProfile.QUALITY_1080P, true);
			this.quality1080Supported = true;
		}
		if (psz.contains(new CameraController.Size(4096, 2160)))
		{
			previewSizes.put(QUALITY_4K, true);
			this.quality4KSupported = true;
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

		int ImageSizeIdxPreference = Integer.parseInt(prefs.getString(
				CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref : MainScreen.sImageSizeVideoFrontPref, "2"));
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
				quality = CamcorderProfile.QUALITY_1080P;
				quickControlIconID = R.drawable.gui_almalence_video_1080;
			}
			break;
		case 3:
			quality = CamcorderProfile.QUALITY_720P;
			quickControlIconID = R.drawable.gui_almalence_video_720;
			break;
		case 4:
			quality = CamcorderProfile.QUALITY_480P;
			quickControlIconID = R.drawable.gui_almalence_video_480;
			break;
		case 5:
			quality = QUALITY_4K;
			quickControlIconID = R.drawable.gui_almalence_video_4096;
			break;
		default:
			break;
		}

		if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality) && !previewSizes.get(quality))
		{
			ImageSizeIdxPreference = 3;
			quality = CamcorderProfile.QUALITY_720P;
			quickControlIconID = R.drawable.gui_almalence_video_720;
			if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality) && !previewSizes.get(quality))
			{
				ImageSizeIdxPreference = 4;
				quality = CamcorderProfile.QUALITY_480P;
				quickControlIconID = R.drawable.gui_almalence_video_480;
			}
		}

		Editor editor = prefs.edit();
		editor.putString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref : MainScreen.sImageSizeVideoFrontPref,
				String.valueOf(ImageSizeIdxPreference));
		editor.commit();

		Camera.Parameters cp = CameraController.getCameraParameters();
		if (cp != null)
		{
			cp.setPreviewFrameRate(30);
			cp.setRecordingHint(true);

			CameraController.setCameraParameters(cp);
		}
	}

	@Override
	public void setCameraPreviewSize()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		int ImageSizeIdxPreference = Integer.parseInt(prefs.getString(
				CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref : MainScreen.sImageSizeVideoFrontPref, "2"));

		final CameraController.Size sz = getBestPreviewSizeDRO(ImageSizeIdxPreference);

		Log.i(TAG, String.format("Preview size: %dx%d", sz.getWidth(), sz.getHeight()));

		CameraController.setCameraPreviewSize(sz);
		MainScreen.setPreviewWidth(sz.getWidth());
		MainScreen.setPreviewHeight(sz.getHeight());
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

	private static CameraController.Size getBestPreviewSizeDRO(final int quality)
	{
		final int width;
		final int height;

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
			width = 1920;
			height = 1080;
			break;
		case 3:
			width = 1280;
			height = 720;
			break;
		case 4:
			width = 720;
			height = 480;
			break;
		case 5:
			width = 4096;
			height = 2160;
			break;
		default:
			return getBestPreviewSizeNormal(false);
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
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		int jpegQuality = Integer.parseInt(prefs.getString(MainScreen.sJPEGQualityPref, "95"));
		CameraController.setJpegQuality(jpegQuality);

		if (CameraController.isModeAvailable(CameraController.getSupportedFocusModes(),
				CameraParameters.AF_MODE_CONTINUOUS_VIDEO))
		{
			CameraController.setCameraFocusMode(CameraParameters.AF_MODE_CONTINUOUS_VIDEO);
			PreferenceManager
					.getDefaultSharedPreferences(MainScreen.getMainContext())
					.edit()
					.putInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref
							: MainScreen.sFrontFocusModePref, CameraParameters.AF_MODE_CONTINUOUS_VIDEO).commit();
		}
	}

	private void releaseMediaRecorder()
	{
		captureRate = 24;
		if (mMediaRecorder != null)
		{
			mMediaRecorder.reset(); // clear recorder configuration
			mMediaRecorder.release(); // release the recorder object
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
	private Camera					lastCamera;

	private boolean modeDRO()
	{
		return (ModePreference.compareTo("0") == 0);
	}

	@Override
	public void onShutterClick()
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
	}

	private void stopRecording()
	{
		if (shutterOff)
			return;

		if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2)
			modeSwitcher.setVisibility(View.VISIBLE);

		View mainButtonsVideo = (View) MainScreen.getInstance().guiManager.getMainView().findViewById(
				R.id.mainButtonsVideo);
		mainButtonsVideo.setVisibility(View.GONE);

		View mainButtons = (View) MainScreen.getInstance().guiManager.getMainView().findViewById(R.id.mainButtons);
		mainButtons.setVisibility(View.VISIBLE);
		mainButtons.findViewById(R.id.buttonSelectMode).setVisibility(View.VISIBLE);

		if (this.modeDRO())
		{
			// <!-- -+-
			PluginManager.getInstance().controlPremiumContent();
			// -+- -->

			this.droEngine.stopRecording();

			MainScreen.getGUIManager().lockControls = false;
			// inform the user that recording has stopped
			isRecording = false;
			showRecordingUI(isRecording);
			PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).edit()
					.putBoolean("videorecording", false).commit();

			// change shutter icon
			// if (swChecked)
			// {
			MainScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_START);
			// } else
			// {
			// MainScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_START_WITH_PAUSE);
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

		MainScreen.getInstance().setKeepScreenOn(false);
	}

	protected void onPreExportVideo()
	{
		MainScreen.getGUIManager().startProcessingAnimation();

		File parent = fileSaved.getParentFile();
		String path = parent.toString().toLowerCase();
		String name = parent.getName().toLowerCase();

		values = new ContentValues();
		values.put(VideoColumns.TITLE, fileSaved.getName().substring(0, fileSaved.getName().lastIndexOf(".")));
		values.put(VideoColumns.DISPLAY_NAME, fileSaved.getName());
		values.put(VideoColumns.DATE_TAKEN, System.currentTimeMillis());
		values.put(VideoColumns.MIME_TYPE, "video/mp4");
		values.put(VideoColumns.BUCKET_ID, path.hashCode());
		values.put(VideoColumns.BUCKET_DISPLAY_NAME, name);
		values.put(VideoColumns.DATA, fileSaved.getAbsolutePath());
		values.put(VideoColumns.DURATION, timeStringToMillisecond(mRecordingTimeView.getText().toString()));
		
		if (this.modeDRO()){
			values.put(VideoColumns.RESOLUTION, String.valueOf(MainScreen.getPreviewWidth()) + "x" + String.valueOf(MainScreen.getPreviewHeight()));
		} else {
			if (lastUseProfile) {
				values.put(VideoColumns.RESOLUTION, String.valueOf(lastCamcorderProfile.videoFrameWidth) + "x" + String.valueOf(lastCamcorderProfile.videoFrameHeight));
			} else {
				values.put(VideoColumns.RESOLUTION, String.valueOf(lastSz.getWidth()) + "x" + String.valueOf(lastSz.getHeight()));
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
		
		File fileSaved = this.fileSaved;
		ArrayList<File> filesListToExport = filesList;
		if (filesListToExport.size() > 0)
		{
			File firstFile = filesListToExport.get(0);
			for (int i = 1; i < filesListToExport.size(); i++)
			{
				File currentFile = filesListToExport.get(i);
				append(firstFile.getAbsolutePath(), currentFile.getAbsolutePath());
			}
			// if not onPause, then last video isn't added to list.
			if (!onPause)
			{
				append(firstFile.getAbsolutePath(), fileSaved.getAbsolutePath());
			}

			if (!filesListToExport.get(0).getAbsoluteFile().equals(fileSaved.getAbsoluteFile()))
			{
				fileSaved.delete();
				firstFile.renameTo(fileSaved);
			}
		}

		String[] filesSavedNames = new String[1];
		filesSavedNames[0] = fileSaved.toString();
		filesListToExport.clear();

		MainScreen.getInstance().getContentResolver().insert(Video.Media.EXTERNAL_CONTENT_URI, values);

		try
		{
			Thread.sleep(500);
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		MainScreen.getMessageHandler().sendEmptyMessage(PluginManager.MSG_EXPORT_FINISHED);
	}

	private void startRecording()
	{
		Camera camera = CameraController.getCamera();
		if (null == camera)
			return;
	
		filesList = new ArrayList<File>();
		
		if (shutterOff)
			return;

		if (!swChecked)
		{
			// RotateImageView additionalButton = (RotateImageView)
			// MainScreen.getInstance().guiManager.getMainView().findViewById(R.id.buttonShutterAdditional);
			RotateImageView buttonSelectMode = (RotateImageView) MainScreen.getInstance().guiManager.getMainView()
					.findViewById(R.id.buttonSelectMode);

			// additionalButton.setVisibility(View.VISIBLE);
			buttonSelectMode.setVisibility(View.GONE);
		}

		modeSwitcher.setVisibility(View.GONE);
		if (this.modeDRO())
		{
			shutterOff = true;
			mRecordingStartTime = SystemClock.uptimeMillis();

			MainScreen.getGUIManager().lockControls = true;
			// inform the user that recording has stopped
			isRecording = true;
			onPause = false;

			showRecordingUI(isRecording);
			PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).edit()
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
					// MainScreen.getGUIManager().lockControls = false;
				}
			}.start();
		} else
		{
			this.startVideoRecording();
		}

		View mainButtonsVideo = (View) MainScreen.getInstance().guiManager.getMainView().findViewById(
				R.id.mainButtonsVideo);
		mainButtonsVideo.setVisibility(View.VISIBLE);

		View mainButtons = (View) MainScreen.getInstance().guiManager.getMainView().findViewById(R.id.mainButtons);
		mainButtons.setVisibility(View.INVISIBLE);

		// change shutter icon
		pauseVideoButton.setVisibility(View.VISIBLE);
		pauseVideoButton.setImageResource(R.drawable.plugin_capture_video_pause);

		MainScreen.getInstance().setKeepScreenOn(true);
	}

	private void stopVideoRecording()
	{
		if (shutterOff)
			return;
		Camera camera = CameraController.getCamera();
		if (null == camera)
			return;

		// stop recording and release camera
		try
		{
			mMediaRecorder.stop(); // stop the recording
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("video onShutterClick", "mMediaRecorder.stop() exception: " + e.getMessage());
		}

		releaseMediaRecorder(); // release the MediaRecorder object
		// camera.lock(); // take camera access back from MediaRecorder

		CameraController.stopCameraPreview();
		Camera.Parameters cp = CameraController.getCameraParameters();
		if (cp != null)
		{
			setCameraPreviewSize();
			CameraController.Size sz = new CameraController.Size(MainScreen.getPreviewWidth(),
					MainScreen.getPreviewHeight());
			MainScreen.getGUIManager().setupViewfinderPreviewSize(sz);
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH && videoStabilization)
				CameraController.setVideoStabilization(false);
		}
		CameraController.startCameraPreview();

		MainScreen.getGUIManager().lockControls = false;
		// inform the user that recording has stopped
		isRecording = false;
		showRecordingUI(isRecording);
		PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).edit()
				.putBoolean("videorecording", false).commit();

		// change shutter icon
		// if (swChecked)
		// {
		MainScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_START);
		// } else
		// {
		// MainScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_START_WITH_PAUSE);
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
	}

	private void startVideoRecording()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		Camera camera = CameraController.getCamera();
		
		lastCamera = camera;

		Date curDate = new Date();
		SessionID = curDate.getTime();

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH && videoStabilization)
			CameraController.setVideoStabilization(true);

		shutterOff = true;
		mRecordingStartTime = SystemClock.uptimeMillis();

		mMediaRecorder = new MediaRecorder();
		CameraController.stopCameraPreview();
		CameraController.unlockCamera();
		mMediaRecorder.setCamera(camera);

		// Step 2: Set sources
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		int ImageSizeIdxPreference = Integer.parseInt(prefs.getString(
				CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref : MainScreen.sImageSizeVideoFrontPref, "2"));

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
			quality = CamcorderProfile.QUALITY_1080P;
			break;
		case 3:
			quality = CamcorderProfile.QUALITY_720P;
			break;
		case 4:
			quality = CamcorderProfile.QUALITY_480P;
			break;
		case 5:
			quality = QUALITY_4K;
			break;
		default:
			break;
		}

		boolean useProfile = true;
		if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality) && !previewSizes.get(quality))
		{
			ImageSizeIdxPreference = 3;
			quality = CamcorderProfile.QUALITY_720P;
			if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality) && !previewSizes.get(quality))
			{
				ImageSizeIdxPreference = 4;
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
		editor.putString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeVideoBackPref : MainScreen.sImageSizeVideoFrontPref,
				String.valueOf(ImageSizeIdxPreference));
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
					default:
						break;
					}
					if (!CamcorderProfile.hasProfile(CameraController.getCameraIndex(), quality))
					{
						Toast.makeText(MainScreen.getInstance(), "Time lapse not supported", Toast.LENGTH_LONG).show();
					} else
						quality = qualityTimeLapse;
				}
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e("Video", "Time lapse error catched" + e.getMessage());
				swChecked = false;

				MainScreen.getGUIManager().lockControls = false;

				PluginManager.getInstance()
						.sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);
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
				case CamcorderProfile.QUALITY_1080P:
					{
						if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(),
								CamcorderProfile.QUALITY_720P))
						{
							CamcorderProfile prof = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
							prof.videoFrameHeight = 1080;
							prof.videoFrameWidth = 1920;
							mMediaRecorder.setProfile(prof);
							lastCamcorderProfile = prof;
							useProf = true;
							lastUseProf = useProf;
						} else
						{
							List<CameraController.Size> psz = CameraController.getSupportedPreviewSizes();
							sz = new CameraController.Size(1920, 1080);
//							if (!psz.contains(sz))
//								sz = new CameraController.Size(1920, 1088);
						}
					}
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

			MainScreen.getGUIManager().lockControls = false;
			PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);
			releaseMediaRecorder(); // release the MediaRecorder object
			camera.lock(); // take camera access back from MediaRecorder
			camera.stopPreview();
			camera.startPreview();

			return;
		}

		// Step 4: Set output file
		mMediaRecorder.setOutputFile(getOutputMediaFile().toString());

		// Step 5: Set the preview output
		mMediaRecorder.setPreviewDisplay(MainScreen.getPreviewSurfaceHolder().getSurface());

		mMediaRecorder
				.setOrientationHint(CameraController.isFrontCamera() ? (MainScreen.getWantLandscapePhoto() ? MainScreen
						.getGUIManager().getDisplayOrientation()
						: (MainScreen.getGUIManager().getDisplayOrientation() + 180) % 360) : MainScreen
						.getGUIManager().getDisplayOrientation());

		// Step 6: Prepare configured MediaRecorder
		try
		{
			mMediaRecorder.prepare();

			// Camera is available and unlocked, MediaRecorder is prepared,
			// now you can start recording
			mMediaRecorder.start();
			MainScreen.getGUIManager().lockControls = true;

		} catch (Exception e)
		{
			Log.d("Video", "Exception preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
			Toast.makeText(MainScreen.getInstance(), "Failed to start video recording", Toast.LENGTH_LONG).show();

			MainScreen.getGUIManager().lockControls = false;
			PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);
			camera.lock(); // take camera access back from MediaRecorder
			camera.stopPreview();
			camera.startPreview();

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
				// MainScreen.getGUIManager().lockControls = false;
			}
		}.start();
	}

	@Override
	public void onPreferenceCreate(PreferenceFragment pf)
	{
		if (pf != null && !MainScreen.getCameraController().isVideoStabilizationSupported())
		{
			PreferenceCategory cat = (PreferenceCategory) pf.findPreference("Pref_VideoCapture_Category");
			CheckBoxPreference cp = (CheckBoxPreference) pf.findPreference("videoStabilizationPref");
			if (cp != null && cat != null)
				cat.removePreference(cp);
		}

	}

	private void getPrefs()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

		ModePreference = prefs.getString("modeVideoDROPref", "1");

		camera2Preference = prefs.getBoolean(
				MainScreen.getMainContext().getResources().getString(R.string.Preference_UseHALv3Key), false);
		prefs.edit()
				.putBoolean(MainScreen.getMainContext().getResources().getString(R.string.Preference_UseHALv3Key),
						false).commit();
		
		if(camera2Preference)
		{
			CameraController.isVideoModeLaunched = true;
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
			mRecordingTimeView.setRotation(MainScreen.getGUIManager().getDisplayRotation());
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

			int color = MainScreen.getAppResources().getColor(R.color.recording_time_remaining_text);

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
		Intent intent = MainScreen.getInstance().getIntent();
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
		Camera camera = CameraController.getCamera();
		if (null == camera)
			return;

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
						Toast.makeText(MainScreen.getInstance(),
								MainScreen.getInstance().getString(R.string.video_paused), Toast.LENGTH_SHORT).show();
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
			Toast.makeText(MainScreen.getInstance(), MainScreen.getInstance().getString(R.string.video_paused),
					Toast.LENGTH_SHORT).show();
		}
		this.droEngine.setPaused(this.onPause);
	}

	private void pauseRecording()
	{
		onPause = true;
		// TODO PAUSE
		// MainScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_PAUSED);
		try
		{
			// stop recording and release camera
			mMediaRecorder.stop(); // stop the recording

			ContentValues values = null;
			values = new ContentValues();
			File parent = fileSaved.getParentFile();
			String path = parent.toString().toLowerCase();
			String name = parent.getName().toLowerCase();

			values = new ContentValues();
			values.put(VideoColumns.TITLE, fileSaved.getName().substring(0, fileSaved.getName().lastIndexOf(".")));
			values.put(VideoColumns.DISPLAY_NAME, fileSaved.getName());
			values.put(VideoColumns.DATE_TAKEN, System.currentTimeMillis());
			values.put(VideoColumns.MIME_TYPE, "video/mp4");
			values.put(VideoColumns.BUCKET_ID, path.hashCode());
			values.put(VideoColumns.BUCKET_DISPLAY_NAME, name);
			values.put(VideoColumns.DATA, fileSaved.getAbsolutePath());

			filesList.add(fileSaved);

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
			fileSaved.delete();
			e.printStackTrace();
		}
	}

	/**
	 * Appends mp4 audio/video from {@code anotherFileName} to
	 * {@code mainFileName}.
	 */
	public static boolean append(String mainFileName, String anotherFileName)
	{
		boolean rvalue = false;
		try
		{
			File targetFile = new File(mainFileName);
			File anotherFile = new File(anotherFileName);
			if (targetFile.exists() && targetFile.length() > 0)
			{
				String tmpFileName = mainFileName + ".tmp";

				append(mainFileName, anotherFileName, tmpFileName);
				anotherFile.delete();
				targetFile.delete();
				new File(tmpFileName).renameTo(targetFile);
				rvalue = true;
			} else if (targetFile.createNewFile())
			{
				copyFile(anotherFileName, mainFileName);
				anotherFile.delete();
				rvalue = true;
			}
		} catch (IOException e)
		{
		}
		return rvalue;
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

	public static void append(final String firstFile, final String secondFile, final String newFile) throws IOException
	{

		final FileOutputStream fos = new FileOutputStream(new File(String.format(newFile)));
		final FileChannel fc = fos.getChannel();

		final Movie movieOne = MovieCreator.build(firstFile);
		final Movie movieTwo = MovieCreator.build(secondFile);
		final Movie finalMovie = new Movie();

		final List<Track> movieOneTracks = movieOne.getTracks();
		final List<Track> movieTwoTracks = movieTwo.getTracks();

		for (int i = 0; i < movieOneTracks.size() || i < movieTwoTracks.size(); ++i)
		{
			finalMovie.addTrack(new AppendTrack(movieOneTracks.get(i), movieTwoTracks.get(i)));
		}

		final Container container = new DefaultMp4Builder().build(finalMovie);
		container.writeContainer(fc);
		fc.close();
		fos.close();
	}

	// append video

	public void takePicture()
	{
		CameraController.captureImagesWithParams(1, CameraController.JPEG, null, null, null, null, true);
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

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		interval = Integer.valueOf(prefs.getString("timelapseInterval", "0"));
		measurementVal = Integer.valueOf(prefs.getString("timelapseMeasurementVal", "0"));

		// show time lapse settings
		timeLapseDialog = new TimeLapseDialog(MainScreen.getInstance());
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

					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
					Editor editor = prefs.edit();
					editor.putString("timelapseMeasurementVal", String.valueOf(measurementVal));
					editor.putString("timelapseInterval", String.valueOf(interval));
					editor.commit();

					timeLapseButton.setImageResource(R.drawable.plugin_capture_video_timelapse_active);

					MainScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_START);
				} else
				{
					timeLapseButton.setImageResource(R.drawable.plugin_capture_video_timelapse_inactive);
					MainScreen.getGUIManager().setShutterIcon(ShutterButton.RECORDER_START);
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
				String.valueOf(MainScreen.getGUIManager().getDisplayOrientation()));
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

		PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));
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
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		boolean showDroWarning = prefs.getBoolean("dontshowagainDroWarning", false);
		
		if (showDroWarning)
			return;
		AlertDialog.Builder builder = new AlertDialog.Builder(MainScreen.getInstance());
	    builder.setTitle("HDR Video");
	    builder.setMessage(MainScreen.getAppResources().getString(R.string.dro_warning) + " " +fps);
	    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        @Override
	        public void onClick(DialogInterface dialog, int which) {
	            dialog.cancel();
	        }
	    });
	    builder.setNegativeButton(MainScreen.getAppResources().getString(R.string.helpTextDontShow), new DialogInterface.OnClickListener() {
	        @Override
	        public void onClick(DialogInterface dialog, int which) {
	        	prefs.edit().putBoolean("dontshowagainDroWarning", true).commit();
	        }
	    });
	    builder.show();
	}
}
