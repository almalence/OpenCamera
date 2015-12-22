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

package com.almalence.plugins.processing.preshot;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.almalence.SwapHeap;

/* <!-- +++
 import com.almalence.opencam_plus.ApplicationInterface;
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.ConfigParser;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.PluginProcessing;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.ConfigParser;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginProcessing;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

import com.almalence.plugins.capture.preshot.PreShot;

/***
 * Implements back in time prcessing plugin. Prepares images for displaying etc.
 ***/

public class PreshotProcessingPlugin extends PluginProcessing implements OnTouchListener, OnClickListener
{
	public PreshotProcessingPlugin()
	{
		super("com.almalence.plugins.preshotprocessing", "preshot", R.xml.preferences_processing_preshot, 0, 0, null);

		this.mSavingDialog = new ProgressDialog(ApplicationScreen.instance);
		this.mSavingDialog.setIndeterminate(true);
		this.mSavingDialog.setCancelable(false);
		this.mSavingDialog.setMessage("Saving");
	}

	private static int		idx								= 0;
	private static int		imgCnt							= 0;

	private Bitmap[]		mini_frames;
	
	private boolean[]		selected_frames;
	private int				iNumSelected 					= 0; 
	
	private Button			mSaveButton;
	private Button			mSaveAllButton;
	
	private CheckBox			mCheckBox;

	private boolean			isSaveAll						= false;

	private DisplayMetrics	metrics							= null;

	private int				mLayoutOrientationCurrent;
	private int				mDisplayOrientationCurrent;
	private int				mDisplayOrientationOnStartProcessing;
	private boolean			mCameraMirrored					= false;

	boolean					isResultFromProcessingPlugin	= false;

	private boolean			postProcessingRun				= false;

	private boolean			isSlowMode						= false;

	private long			sessionID						= 0;

	private ProgressDialog	mSavingDialog;

	// indicates if it's first launch - to show hint layer.
	private boolean			isFirstLaunch					= true;

	@Override
	public void onStartProcessing(long SessionID)
	{
		Message msg = new Message();
		msg.what = ApplicationInterface.MSG_PROCESSING_BLOCK_UI;
		ApplicationScreen.getMessageHandler().sendMessage(msg);

		ApplicationScreen.getPluginManager().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_LOCKED);

		ApplicationScreen.getGUIManager().lockControls = true;

		mDisplayOrientationOnStartProcessing = ApplicationScreen.getGUIManager().getDisplayOrientation();
		mDisplayOrientationCurrent = ApplicationScreen.getGUIManager().getDisplayOrientation();
		int orientation = ApplicationScreen.getGUIManager().getLayoutOrientation();
		mLayoutOrientationCurrent = orientation == 0 || orientation == 180 ? orientation : (orientation + 180) % 360;
		
		sessionID = SessionID;
		
		mCameraMirrored = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem(
				"cameraMirrored" + sessionID));

		ApplicationScreen.getPluginManager().addToSharedMem("modeSaveName" + sessionID,
				ConfigParser.getInstance().getMode(mode).modeSaveName);

		ProcessingImages();

		imgCnt = Integer.parseInt(ApplicationScreen.getPluginManager().getFromSharedMem("amountofcapturedframes" + sessionID));
		ApplicationScreen.getPluginManager().addToSharedMem("amountofresultframes" + sessionID, String.valueOf(imgCnt));
		ApplicationScreen.getPluginManager().addToSharedMem("ResultFromProcessingPlugin" + sessionID,
				isSlowMode ? "true" : "false");

		prepareMiniFrames();
	}

	private Integer ProcessingImages()
	{
		isSlowMode = Boolean.parseBoolean(ApplicationScreen.getPluginManager().getFromSharedMem("IsSlowMode" + sessionID));
		int imagesAmount = Integer.parseInt(ApplicationScreen.getPluginManager().getFromSharedMem(
				"amountofcapturedframes" + sessionID));

		
		int iSaveImageWidth = 0;
		int iSaveImageHeight = 0;
		
		if(isSlowMode)
		{
			CameraController.Size imageSize = CameraController.getCameraImageSize();
			iSaveImageWidth = imageSize.getWidth();
			iSaveImageHeight = imageSize.getHeight();
			
		}
		else
		{
			iSaveImageWidth = ApplicationScreen.getPreviewWidth();
			iSaveImageHeight = ApplicationScreen.getPreviewHeight();
		}

		if (imagesAmount == 0)
			imagesAmount = 1;

		for (int i = 0; i < imagesAmount; i++)
		{
			int iOrientation = PreShot.getOrientation(i);
			if (isSlowMode)
				ApplicationScreen.getPluginManager().addToSharedMem("resultframeorientation" + (i + 1) + sessionID,
						String.valueOf((iOrientation)));
			else
				ApplicationScreen.getPluginManager().addToSharedMem("resultframeorientation" + (i + 1) + sessionID,
						String.valueOf((0)));
			if (iOrientation == 90 || iOrientation == 270)
			{
				ApplicationScreen.getPluginManager().addToSharedMem("saveImageWidth" + sessionID,
						String.valueOf(iSaveImageHeight));
				ApplicationScreen.getPluginManager().addToSharedMem("saveImageHeight" + sessionID,
						String.valueOf(iSaveImageWidth));
			} else
			{
				ApplicationScreen.getPluginManager().addToSharedMem("saveImageWidth" + sessionID,
						String.valueOf(iSaveImageWidth));
				ApplicationScreen.getPluginManager().addToSharedMem("saveImageHeight" + sessionID,
						String.valueOf(iSaveImageHeight));
			}

			ApplicationScreen.getPluginManager().addToSharedMem("resultframemirrored" + (i + 1) + sessionID,
					String.valueOf(mCameraMirrored));
		}

		ApplicationScreen.getPluginManager().addToSharedMem("amountofresultframes" + sessionID, String.valueOf(imagesAmount));
		return null;
	}

	@Override
	public void freeMemory()
	{
		PreShot.FreeBuffer();
	}

	public int getMultishotImageCount()
	{
		return Integer.parseInt(ApplicationScreen.getPluginManager().getFromSharedMem("amountofcapturedframes" + sessionID));
	}

	@Override
	public boolean isPostProcessingNeeded()
	{
		return true;
	}

	@Override
	public void onStartPostProcessing()
	{
		postProcessingView = LayoutInflater.from(ApplicationScreen.getMainContext()).inflate(
				R.layout.plugin_processing_preshot_postprocessing_layout, null);

		idx = 0;
		imgCnt = 0;

		setupSaveButton();

		((ImageView) postProcessingView.findViewById(R.id.imageHolder)).setOnTouchListener(this);

		isResultFromProcessingPlugin = Boolean.parseBoolean(ApplicationScreen.getPluginManager().getFromSharedMem(
				"ResultFromProcessingPlugin" + sessionID));
		metrics = new DisplayMetrics();
		ApplicationScreen.instance.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		imgCnt = Integer.parseInt(ApplicationScreen.getPluginManager().getFromSharedMem("amountofcapturedframes" + sessionID));
		if (0 != imgCnt)
			idx = imgCnt - 1;
		else
			idx = 0;
		idx /= 2;

		Show(true);

		// if first launch - show layout with hints
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		if (prefs.contains("isFirstPreShotLaunch"))
		{
			isFirstLaunch = prefs.getBoolean("isFirstPreShotLaunch", true);
		} else
		{
			Editor prefsEditor = prefs.edit();
			prefsEditor.putBoolean("isFirstPreShotLaunch", false);
			prefsEditor.commit();
			isFirstLaunch = true;
		}

		// show/hide hints
		if (!isFirstLaunch)
			postProcessingView.findViewById(R.id.preShotHintLayout).setVisibility(View.GONE);
		else
		{
			postProcessingView.findViewById(R.id.preShotHintLayout).setVisibility(View.VISIBLE);
			postProcessingView.bringToFront();
		}

		postProcessingRun = true;
	}

	public void setupSaveButton()
	{
		// put save button on screen
		mSaveButton = new Button(ApplicationScreen.instance);
		mSaveButton.setBackgroundResource(R.drawable.plugin_processing_preshot_savethis_background);
		mSaveButton.setOnClickListener(this);
		LayoutParams saveLayoutParams = new LayoutParams(
				(int) (ApplicationScreen.getMainContext().getResources().getDimension(R.dimen.postprocessing_savebutton_size)),
				(int) (ApplicationScreen.getMainContext().getResources().getDimension(R.dimen.postprocessing_savebutton_size)));
		saveLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		saveLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		float density = ApplicationScreen.getAppResources().getDisplayMetrics().density;
		saveLayoutParams.setMargins((int) (density * 8), (int) (density * 8), 0, 0);
		((RelativeLayout) postProcessingView.findViewById(R.id.preshot_processingLayout2)).addView(mSaveButton,
				saveLayoutParams);
		mSaveButton.setRotation(mLayoutOrientationCurrent);
		mSaveButton.invalidate();

		// put save button on screen
		mSaveAllButton = new Button(ApplicationScreen.instance);
		mSaveAllButton.setBackgroundResource(R.drawable.plugin_processing_preshot_saveall_background);
		mSaveAllButton.setOnClickListener(this);
		LayoutParams saveLayoutParams2 = new LayoutParams(
				(int) (ApplicationScreen.getMainContext().getResources().getDimension(R.dimen.postprocessing_savebutton_size)),
				(int) (ApplicationScreen.getMainContext().getResources().getDimension(R.dimen.postprocessing_savebutton_size)));
		saveLayoutParams2.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		saveLayoutParams2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		saveLayoutParams2.setMargins((int) (density * 8), (int) (density * 8), 0, 0);
		((RelativeLayout) postProcessingView.findViewById(R.id.preshot_processingLayout2)).addView(mSaveAllButton,
				saveLayoutParams2);
		mSaveAllButton.setRotation(mLayoutOrientationCurrent);
		mSaveAllButton.invalidate();
		
		// put save all button on screen
		mCheckBox = new CheckBox(ApplicationScreen.instance);
		mCheckBox.setOnClickListener(this);
		LayoutParams saveLayoutParams3 = new LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		saveLayoutParams3.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		saveLayoutParams3.addRule(RelativeLayout.CENTER_HORIZONTAL);
		saveLayoutParams3.setMargins(0, (int) (density * 8), 0, 0);
		((RelativeLayout) postProcessingView.findViewById(R.id.preshot_processingLayout2)).addView(mCheckBox,
				saveLayoutParams3);
		mCheckBox.setScaleX(1.5f);
		mCheckBox.setScaleY(1.5f);
		mCheckBox.setRotation(mLayoutOrientationCurrent);
		mCheckBox.invalidate();
	}

	public void saveTask()
	{
		if (isSaveAll)
			saveAll();
		else
			saveThis();

		ApplicationScreen.getPluginManager().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_UNLOCKED);

		ApplicationScreen.getGUIManager().lockControls = false;

		postProcessingRun = false;

		ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_POSTPROCESSING_FINISHED);
	}

	@Override
	public void onClick(View v)
	{
		if (postProcessingView != null
				&& postProcessingView.findViewById(R.id.preShotHintLayout).getVisibility() == View.VISIBLE)
			postProcessingView.findViewById(R.id.preShotHintLayout).setVisibility(View.GONE);

		if (v == mSaveButton)
		{
			isSaveAll = false;
			saveTask();
		} else if (v == mSaveAllButton)
		{
			isSaveAll = true;
			saveTask();
		}
		else if (v == mCheckBox)
		{
			//mark image checked/unchecked for saving
			selected_frames[idx]=mCheckBox.isChecked();
			if (mCheckBox.isChecked())
				iNumSelected++;
			else
				iNumSelected--;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (postProcessingView != null
				&& postProcessingView.findViewById(R.id.preShotHintLayout).getVisibility() == View.VISIBLE)
			postProcessingView.findViewById(R.id.preShotHintLayout).setVisibility(View.GONE);

		if (keyCode == KeyEvent.KEYCODE_BACK
				&& ApplicationScreen.instance.findViewById(R.id.postprocessingLayout).getVisibility() == View.VISIBLE)
		{
			ApplicationScreen.getPluginManager().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_UNLOCKED);

			ApplicationScreen.getGUIManager().lockControls = false;

			postProcessingRun = false;

			ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_POSTPROCESSING_FINISHED);
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	private void prepareMiniFrames()
	{
		mini_frames = new Bitmap[imgCnt];
		selected_frames = new boolean[imgCnt];
		iNumSelected = 0;

		for (int i = 0; i < imgCnt; i++)
		{
			this.mini_frames[i] = getMultishotBitmap(i);
		}
	}

	@Override
	public void onOrientationChanged(int orientation)
	{
		if (orientation != mDisplayOrientationCurrent)
		{
			mLayoutOrientationCurrent = (orientation == 0 || orientation == 180) ? orientation + 90 : orientation - 90;
			mDisplayOrientationCurrent = orientation;
			if (postProcessingRun)
			{
				mSaveButton.setRotation(mLayoutOrientationCurrent);
				mSaveButton.invalidate();
				mSaveAllButton.setRotation(mLayoutOrientationCurrent);
				mSaveAllButton.invalidate();
				Show(false);
			}
		}
	}

	private void Show(boolean initial)
	{
		if (idx < 0)
			idx = 0;
		else if (idx >= imgCnt)
			idx = imgCnt - 1;
		if (imgCnt == 1 || mini_frames.length == 1)
			idx = 0;
		
		mCheckBox.setChecked(selected_frames[idx]);

		Bitmap photo = mini_frames[idx];
		if (photo != null)
		{
			if (initial)
			{
				if (mDisplayOrientationCurrent == 0 || mDisplayOrientationCurrent == 180) // Device
																							// in
																							// landscape
				{
					Matrix matrix = new Matrix();

					matrix.postRotate(90);
					photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
				}
			} else
			{
				boolean isGuffyOrientation = mDisplayOrientationOnStartProcessing == 180
						|| mDisplayOrientationOnStartProcessing == 270;

				Matrix matrix = new Matrix();

				matrix.postRotate(isGuffyOrientation ? (mLayoutOrientationCurrent + 180) % 360
						: mLayoutOrientationCurrent);
				photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
			}

			((ImageView) postProcessingView.findViewById(R.id.imageHolder)).setImageBitmap(photo);
			String txt = idx + 1 + " of " + imgCnt;
			((TextView) postProcessingView.findViewById(R.id.preshot_image_counter)).setText(txt);
			postProcessingView.findViewById(R.id.preshot_image_counter).setRotation(mLayoutOrientationCurrent);

			ApplicationScreen.getPluginManager().addToSharedMem("previewresultframeindex", String.valueOf(idx + 1));
		}
	}

	static boolean	isFlipping	= false;

	private void flipPhoto(boolean toLeft, float XtoVisible)
	{
		isFlipping = true;
		ImageView imgView = (ImageView) ApplicationScreen.instance.findViewById(R.id.imageHolder);

		float[] f = new float[9];
		imgView.getImageMatrix().getValues(f);

		// Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
		final float scaleX = f[Matrix.MSCALE_X];
		final float scaleY = f[Matrix.MSCALE_Y];

		// Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
		final Drawable d = imgView.getDrawable();
		final int origW = d.getIntrinsicWidth();
		final int origH = d.getIntrinsicHeight();

		// Calculate the actual dimensions
		final int actW = Math.round(origW * scaleX);
		final int actH = Math.round(origH * scaleY);

		AnimationSet visible = new AnimationSet(true);
		visible.setInterpolator(new DecelerateInterpolator());

		int duration_visible = 0;

		if (mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
			duration_visible = com.almalence.util.Util.clamp(Math.abs(Math.round((XtoVisible * 500) / actH)), 250, 500);
		else
			duration_visible = com.almalence.util.Util.clamp(Math.abs(Math.round((XtoVisible * 500) / actW)), 250, 500);

		Animation visible_translate;
		if (mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
			visible_translate = new TranslateAnimation(0, 0, XtoVisible, 0);
		else
			visible_translate = new TranslateAnimation(XtoVisible, 0, 0, 0);
		visible_translate.setDuration(duration_visible);
		visible_translate.setFillAfter(true);

		visible.addAnimation(visible_translate);

		postProcessingView.findViewById(R.id.imageListed).startAnimation(visible);

		visible.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationEnd(Animation animation)
			{
				postProcessingView.findViewById(R.id.imageListed).clearAnimation();
				postProcessingView.findViewById(R.id.imageListed).setVisibility(View.GONE);
				Show(false);
				isFlipping = false;
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}

			@Override
			public void onAnimationStart(Animation animation)
			{
			}
		});
	}

	@Override
	public Bitmap getMultishotBitmap(int index)
	{
		if (!isSlowMode)
		{
			int[] data = PreShot.GetFromBufferRGBA(index, false, false);

			if (data.length == 0)
			{
				return null;
			}

			int H = ApplicationScreen.getPreviewHeight(), W = ApplicationScreen.getPreviewWidth();
			int or = PreShot.getOrientation(index);
			if (90 == PreShot.getOrientation(index) || 270 == PreShot.getOrientation(index))
			{
				H = ApplicationScreen.getPreviewWidth();
				W = ApplicationScreen.getPreviewHeight();
			}

			Bitmap bitmap;
			bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
			bitmap.setPixels(data, 0, W, 0, 0, W, H);
			bitmap = Bitmap.createScaledBitmap(bitmap, W / 2, H / 2, false);

			if (mCameraMirrored && (90 == PreShot.getOrientation(index) || 270 == PreShot.getOrientation(index)))
			{
				Matrix matrix = new Matrix();
				matrix.postRotate(180);
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
			}

			if ((CameraController.isNexus6 && mCameraMirrored) || (CameraController.isNexus5x && !mCameraMirrored))
			{	
				Matrix matrix = new Matrix();
				matrix.postRotate(180);
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
			}
			
			return bitmap;
		} else
		{// slow mode
			byte[] data = PreShot.GetFromBufferToShowInSlow(index, ApplicationScreen.getPreviewHeight(),
					ApplicationScreen.getPreviewWidth());

			if (data.length == 0)
			{
				return null;
			}

			int H = ApplicationScreen.getPreviewHeight(), W = ApplicationScreen.getPreviewWidth();

			Bitmap photo = null;
			photo = BitmapFactory.decodeByteArray(data, 0, data.length);
			photo = Bitmap.createScaledBitmap(photo, W, H, false);

			if (90 == PreShot.getOrientation(index) || 270 == PreShot.getOrientation(index))
			{
				Matrix matrix = new Matrix();
				matrix.postRotate(mCameraMirrored ? 270 : 90);
				photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
			}

			if ((CameraController.isNexus6 && mCameraMirrored) || (CameraController.isNexus5x && !mCameraMirrored))
			{	
				Matrix matrix = new Matrix();
				matrix.postRotate(180);
				photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
			}
			return photo;
		}
	}

	private static float	X				= 0;

	private static float	Xprev			= 0;

	private static float	Xoffset			= 0;

	private static float	XtoLeftVisible	= 0;

	private static float	XtoRightVisible	= 0;

	public boolean onTouch(View v, MotionEvent event)
	{
		if (postProcessingView != null
				&& postProcessingView.findViewById(R.id.preShotHintLayout).getVisibility() == View.VISIBLE)
			postProcessingView.findViewById(R.id.preShotHintLayout).setVisibility(View.GONE);

		if (isFlipping)
			return true;

		boolean isGuffyOrientation = (mDisplayOrientationOnStartProcessing == 180 || mDisplayOrientationOnStartProcessing == 270);

		Bitmap photo = null;

		switch (event.getAction())
		{
		case MotionEvent.ACTION_DOWN:
			{
				if (mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
					X = event.getY();
				else
					X = event.getX();
				Xoffset = X;
				Xprev = X;

				break;
			}
		case MotionEvent.ACTION_UP:
			{
				float difX = 0;
				if (mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
					difX = event.getY();
				else
					difX = event.getX();

				if ((X > difX) && (X - difX > 100))
				{
					int new_idx = isGuffyOrientation ? --idx : ++idx;
					if (new_idx <= imgCnt - 1 && new_idx >= 0)
						flipPhoto(true, XtoLeftVisible);
					else
						Show(false);
				} else if (X < difX && (difX - X > 100))
				{
					int new_idx = isGuffyOrientation ? ++idx : --idx;
					if (new_idx >= 0 && new_idx <= imgCnt - 1)
						flipPhoto(false, XtoRightVisible);
					else
						Show(false);
				} else
				{
					postProcessingView.findViewById(R.id.imageListed).clearAnimation();
					postProcessingView.findViewById(R.id.imageListed).setVisibility(View.GONE);
				}
				break;
			}
		case MotionEvent.ACTION_MOVE:
			{
				float difX = 0;
				if (mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
					difX = event.getY();
				else
					difX = event.getX();

				if ((X > difX && isGuffyOrientation ? idx == 0 : idx == imgCnt - 1)
						|| (X < difX && isGuffyOrientation ? idx == imgCnt - 1 : idx == 0))
					break;

				ImageView imgView = (ImageView) ApplicationScreen.instance.findViewById(R.id.imageHolder);

				int screenWidth = imgView.getWidth();
				int screenHeight = imgView.getHeight();

				float[] f = new float[9];
				imgView.getImageMatrix().getValues(f);

				// Extract the scale values using the constants (if aspect ratio
				// maintained, scaleX == scaleY)
				final float scaleX = f[Matrix.MSCALE_X];
				final float scaleY = f[Matrix.MSCALE_Y];

				// Get the drawable (could also get the bitmap behind the
				// drawable and getWidth/getHeight)
				final Drawable d = imgView.getDrawable();
				if (d == null)
					break;
				final int origW = d.getIntrinsicWidth();
				final int origH = d.getIntrinsicHeight();

				// Calculate the actual dimensions
				final int actW = Math.round(origW * scaleX);
				final int actH = Math.round(origH * scaleY);

				final int startW = Math.round(actW + (screenWidth - actW) / 2);
				final int startH = Math.round(actH + (screenHeight - actH) / 2);

				Animation in_animation;
				Animation out_animation;
				Animation reverseout_animation;

				AnimationSet in_animation_set = new AnimationSet(true);
				in_animation_set.setInterpolator(new DecelerateInterpolator());
				in_animation_set.setFillAfter(true);

				AnimationSet reverseout_animation_set = new AnimationSet(true);
				reverseout_animation_set.setInterpolator(new DecelerateInterpolator());
				reverseout_animation_set.setFillAfter(true);

				boolean toLeft;
				if (difX > Xprev)
				{
					if (mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
						out_animation = new TranslateAnimation(0, 0, Xprev - Xoffset, difX - Xoffset);
					else
						out_animation = new TranslateAnimation(Xprev - Xoffset, difX - Xoffset, 0, 0);
					out_animation.setDuration(10);
					out_animation.setInterpolator(new LinearInterpolator());
					out_animation.setFillAfter(true);

					if (mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
						in_animation = new TranslateAnimation(0, 0, Xprev - Xoffset - startH, difX - Xoffset - startH);
					else
						in_animation = new TranslateAnimation(Xprev - Xoffset - actW, difX - Xoffset - actW, 0, 0);
					in_animation.setDuration(10);
					in_animation.setInterpolator(new LinearInterpolator());
					in_animation.setFillAfter(true);

					if (mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
						reverseout_animation = new TranslateAnimation(0, 0, difX + (startH - Xoffset), Xprev
								+ (startH - Xoffset));
					else
						reverseout_animation = new TranslateAnimation(difX + (actW - Xoffset),
								Xprev + (actW - Xoffset), 0, 0);
					reverseout_animation.setDuration(10);
					reverseout_animation.setInterpolator(new LinearInterpolator());
					reverseout_animation.setFillAfter(true);

					float scale_from = Math.abs(Xprev - X) / -500 + 2;
					float scale_to = Math.abs(difX - X) / -500 + 2;

					scale_from = com.almalence.util.Util.clamp(scale_from, 1, 2);
					scale_to = com.almalence.util.Util.clamp(scale_to, 1, 2);
					in_animation_set.addAnimation(in_animation);

					reverseout_animation_set.addAnimation(reverseout_animation);

					toLeft = false;

					XtoRightVisible = difX - Xoffset - screenWidth;
				} else
				{
					if (mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
						out_animation = new TranslateAnimation(0, 0, difX - Xoffset, Xprev - Xoffset);
					else
						out_animation = new TranslateAnimation(difX - Xoffset, Xprev - Xoffset, 0, 0);
					out_animation.setDuration(10);
					out_animation.setInterpolator(new LinearInterpolator());
					out_animation.setFillAfter(true);

					if (mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
						in_animation = new TranslateAnimation(0, 0, startH + (Xprev - Xoffset), startH
								+ (difX - Xoffset));
					else
						in_animation = new TranslateAnimation(actW + (Xprev - Xoffset), actW + (difX - Xoffset), 0, 0);
					in_animation.setDuration(10);
					in_animation.setInterpolator(new LinearInterpolator());
					in_animation.setFillAfter(true);

					if (mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
						reverseout_animation = new TranslateAnimation(0, 0, Xprev - Xoffset - startH, difX - Xoffset
								- startH);
					else
						reverseout_animation = new TranslateAnimation(Xprev - Xoffset - actW, difX - Xoffset - actW, 0,
								0);
					reverseout_animation.setDuration(10);
					reverseout_animation.setInterpolator(new LinearInterpolator());
					reverseout_animation.setFillAfter(true);

					float scale_from = Math.abs(X - Xprev) / -500 + 2;
					float scale_to = Math.abs(X - difX) / -500 + 2;

					scale_from = com.almalence.util.Util.clamp(scale_from, 1, 2);
					scale_to = com.almalence.util.Util.clamp(scale_to, 1, 2);
					in_animation_set.addAnimation(in_animation);
					reverseout_animation_set.addAnimation(reverseout_animation);

					toLeft = true;

					XtoLeftVisible = screenWidth + (difX - Xoffset);
				}

				if (difX < X && Xprev >= X)
				{
					int new_idx = isGuffyOrientation ? idx - 1 : idx + 1;
					photo = mini_frames[new_idx];
					if (photo != null)
					{
						Matrix matrix = new Matrix();
						matrix.postRotate(isGuffyOrientation ? (mLayoutOrientationCurrent + 180) % 360
								: mLayoutOrientationCurrent);
						photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
						((ImageView) postProcessingView.findViewById(R.id.imageListed)).setImageBitmap(photo);
					}
				} else if (difX > X && Xprev <= X)
				{
					int new_idx = isGuffyOrientation ? idx + 1 : idx - 1;
					photo = mini_frames[new_idx];
					if (photo != null)
					{
						Matrix matrix = new Matrix();

						matrix.postRotate(isGuffyOrientation ? (mLayoutOrientationCurrent + 180) % 360
								: mLayoutOrientationCurrent);
						photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
						((ImageView) postProcessingView.findViewById(R.id.imageListed)).setImageBitmap(photo);
					}
				}

				if ((toLeft && difX < X) || (!toLeft && difX > X))
					postProcessingView.findViewById(R.id.imageListed).startAnimation(in_animation_set);
				else
					postProcessingView.findViewById(R.id.imageListed).startAnimation(reverseout_animation_set);

				if (postProcessingView.findViewById(R.id.imageListed).getVisibility() == View.GONE)
					postProcessingView.findViewById(R.id.imageListed).setVisibility(View.VISIBLE);

				Xprev = Math.round(difX);
			}
			break;
		default:
			break;
		}
		return true;
	}

	private void saveAll()
	{
		int imagesAmount = Integer.parseInt(ApplicationScreen.getPluginManager().getFromSharedMem(
				"amountofcapturedframes" + sessionID));
		if (imagesAmount == 0)
			imagesAmount = 1;

		int j=0;
		for (int i = 0; i < imagesAmount; i++)
		{
			if (iNumSelected == 0)
				pushToExport(i, i);
			else if (selected_frames[i])
			{
				pushToExport(i, j);
				ApplicationScreen.getPluginManager().addToSharedMem("amountofresultframes" + sessionID, String.valueOf(iNumSelected));
				j++;
			}
		}

		ApplicationScreen.getPluginManager().addToSharedMem("sessionID", String.valueOf(sessionID));
	}

	private void saveThis()
	{
		pushToExport(idx, idx);

		int index = idx + 1;

		ApplicationScreen.getPluginManager().addToSharedMem("sessionID", String.valueOf(sessionID));
		ApplicationScreen.getPluginManager().addToSharedMem("resultframeindex" + sessionID, String.valueOf(index));
	}

	//i and j should be the same in common case
	//but if we need to save not all images, but some, than i is real position of original image, j is position of result image
	//i - position in captured buffer, j position in buffer to save
	private void pushToExport(int i, int j)
	{
		if (!isSlowMode)
		{
			byte[] data = null;

			if (mCameraMirrored)
				data = PreShot.GetFromBufferNV21(i, 0, 0, 1);
			else
				data = PreShot.GetFromBufferNV21(i, 0, 0, 0);

			if (data.length == 0)
				return;

			int frame = SwapHeap.SwapToHeap(data);

			ApplicationScreen.getPluginManager().addToSharedMem("resultframe" + (j + 1) + sessionID, String.valueOf(frame));
			ApplicationScreen.getPluginManager().addToSharedMem("resultframelen" + (j + 1) + sessionID,
					String.valueOf(data.length));

		} else if (isSlowMode)
		{
			byte[] data = PreShot.GetFromBufferSimpleNV21(i);

			if (data.length == 0)
				return;

			int frame = SwapHeap.SwapToHeap(data);

			ApplicationScreen.getPluginManager().addToSharedMem("resultframe" + (j + 1) + sessionID, String.valueOf(frame));
			ApplicationScreen.getPluginManager().addToSharedMem("resultframelen" + (j + 1) + sessionID,
					String.valueOf(data.length));
			ApplicationScreen.getPluginManager().addToSharedMem("resultframeformat" + (j + 1) + sessionID, "jpeg");
		}
	}
}
