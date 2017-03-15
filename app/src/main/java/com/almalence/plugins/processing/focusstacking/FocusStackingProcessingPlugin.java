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

package com.almalence.plugins.processing.focusstacking;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.almalence.SwapHeap;
import com.almalence.asynctaskmanager.OnTaskCompleteListener;
import com.almalence.focuscam.ApplicationInterface;
import com.almalence.focuscam.ApplicationScreen;
import com.almalence.focuscam.ConfigParser;
import com.almalence.focuscam.PluginManager;
import com.almalence.focuscam.PluginManagerBase;
import com.almalence.focuscam.PluginProcessing;
import com.almalence.focuscam.R;
import com.almalence.focuscam.cameracontroller.CameraController;
import com.almalence.focusstacking.AlmaShotFocusStacking;
import com.almalence.focusstacking.AlmaShotMPOWriter;


/***
 * Implements Focus stacking processing plugin.
 ***/
public class FocusStackingProcessingPlugin extends PluginProcessing implements OnClickListener,
		OnSeekBarChangeListener, OnItemSelectedListener, OnTaskCompleteListener, OnShowOffClickListener, Handler.Callback
{
	private String				TAG = "FStacking";
	private byte[]				allInFocusYUV;	// Result
	private byte[]				baseFrame;
	private boolean				resultIsBaseFrame 						= false;

	private static boolean		EditMode								= false;
	private static int			SavePreference;
	
	private static String		sEditModePref;
	private static String		sSaveModePref;

	private int					mLayoutOrientationCurrent				= 0;
	private int					mImageDataOrientation	= 0;
	private int					mDisplayOrientationCurrent				= 0;
	private boolean				mCameraMirrored							= false;

	private boolean				postProcessingRun						= false;
	
	private int					mImageWidth;
	private int					mImageHeight;
	
	private int					mResultImageWidth;
	private int					mResultImageHeight;
	
	private int					mImageAmount;
	private int[]				mInputFrames;
	private int[]				mInputFramesLength;
	
	public static String							sOutputImageSizeRearPref;
	public static String							sOutputImageSizeFrontPref;

	private List<Float>								focusDistances; //Focus distances for each input frame

	private int										mBaseFrameIndex; //Frame around which focus depth will be changed
	private int										mCurrentFocusDepth; //How much frames to take for focus stacking process
    private int                                     mMaxFocusDepth; //Maximum images amount for focus stacking
	private boolean									mKeepResultBitmap = false;
	
	private ComplexBitmap							mBitmap = null; //Custom bitmap object to show processing result

	private long									sessionID = 0;

    private boolean                                 isAppInPortrait = true;

	public FocusStackingProcessingPlugin()
	{
		super("com.almalence.plugins.focusstackingprocessing", "focusbracketing", R.xml.preferences_processing_focusstacking,
				0, 0, null);
	}
	
	@Override
	public void onCreate()
	{
		sEditModePref = ApplicationScreen.getAppResources().getString(R.string.Preference_FocusBracketingEditModePref);
		sSaveModePref = ApplicationScreen.getAppResources().getString(R.string.Preference_FocusBracketing_Save_Pref);
		
		sOutputImageSizeRearPref = ApplicationScreen.getAppResources().getString(R.string.Preference_OutputImageSizeRearValue);
		sOutputImageSizeFrontPref = ApplicationScreen.getAppResources().getString(R.string.Preference_OutputImageSizeFrontValue);
	}
	
	@Override
	public void onResume()
	{
		getPrefs();
	}
	
	@Override
	public void onPause()
	{
		if(postProcessingRun)
		{
			this.imageView.setData(null);
			postProcessingRun = false;
			AlmaShotFocusStacking.Release();
		}
	}


	private void getPrefs()
	{
		// Get the xml/preferences.xml preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.instance
				.getBaseContext());

		EditMode = prefs.getBoolean(sEditModePref, false);
		SavePreference = Integer.parseInt(prefs.getString(sSaveModePref, "0"));
	}
	

	@Override
	public void onStartProcessing(long SessionID)
	{
		if (EditMode)
		{
			Message msg = new Message();
			msg.what = ApplicationInterface.MSG_PROCESSING_BLOCK_UI;
			ApplicationScreen.getMessageHandler().sendMessage(msg);

			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, 
					ApplicationInterface.MSG_CONTROL_LOCKED);

			ApplicationScreen.getGUIManager().lockControls = true;
		}

		sessionID = SessionID;

		PluginManager.getInstance().addToSharedMem("modeSaveName" + sessionID,
				ConfigParser.getInstance().getMode(mode).modeSaveName);

		mImageDataOrientation = ApplicationScreen.getGUIManager().getImageDataOrientation();
		mDisplayOrientationCurrent = ApplicationScreen.getGUIManager().getDisplayOrientation();
		int orientation = ApplicationScreen.getGUIManager().getLayoutOrientation();
		mLayoutOrientationCurrent = orientation == 0 || orientation == 180 ? orientation : (orientation + 180) % 360;

		mCameraMirrored = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("cameraMirrored" + sessionID));
		

		mImageWidth = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("imageWidth" + sessionID));
		mImageHeight = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("imageHeight" + sessionID));
		
		mImageAmount = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("amountofcapturedframes" + sessionID));

		mInputFrames = new int[mImageAmount];
		mInputFramesLength = new int[mImageAmount];
		
		focusDistances = new ArrayList<>();
		
		for (int i = 0; i < mImageAmount; i++)
		{
			mInputFrames[i] = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
					"frame" + (i + 1) + sessionID));
			mInputFramesLength[i] = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
					"framelen" + (i + 1) + sessionID));
			
			focusDistances.add(Float.parseFloat(PluginManager.getInstance().getFromSharedMem(
					"focusdistance" + (i + 1) + sessionID)));
		}
		
		AlmaShotFocusStacking.Initialize(mInputFrames, focusDistances, mImageAmount, mImageWidth, mImageHeight);

		if (!EditMode)
		{
			// focus stacking processing
			this.mBaseFrameIndex = Math.round(mImageAmount/2); // initial base frame is a middle frame of frames sequence
			this.mCurrentFocusDepth = Math.max(this.mBaseFrameIndex +1, mImageAmount - this.mBaseFrameIndex); //Initial processing result contains all input frames
			
			FStackingProcessing(false);

			saveInputAndProcessedFrames();
			
			if(mImageDataOrientation == 90 || mImageDataOrientation == 270)
			{
				mResultImageWidth = mImageHeight;
				mResultImageHeight = mImageWidth;
			}
			else
			{
				mResultImageWidth = mImageWidth;
				mResultImageHeight = mImageHeight;
			}
			
			int frame_len = allInFocusYUV.length;
			int frame = SwapHeap.SwapToHeap(allInFocusYUV);

			PluginManager.getInstance().addToSharedMem("resultfromshared" + sessionID, "true");

			PluginManager.getInstance().addToSharedMem("writeorientationtag" + sessionID, "true");
			PluginManager.getInstance().addToSharedMem("resultframeorientation1" + sessionID,
					String.valueOf(mImageDataOrientation));
			PluginManager.getInstance().addToSharedMem("amountofresultframes" + sessionID, "1");
			PluginManager.getInstance().addToSharedMem("resultframe1" + sessionID, String.valueOf(frame));
			PluginManager.getInstance().addToSharedMem("resultframelen1" + sessionID, String.valueOf(frame_len));

			PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID, String.valueOf(mResultImageWidth));
			PluginManager.getInstance().addToSharedMem("saveImageHeight" + sessionID, String.valueOf(mResultImageHeight));

			AlmaShotFocusStacking.Release();
		}
	}


	private void FStackingProcessing(boolean transformResult)
	{
		float focusDistance = focusDistances.get(mBaseFrameIndex);
		allInFocusYUV = AlmaShotFocusStacking.Process(focusDistance, mCurrentFocusDepth, mImageDataOrientation, mCameraMirrored, transformResult);
    }

	private byte[] getMPODataForAlignedFrames()
	{
		int[] alignedFrames = AlmaShotFocusStacking.GetAlignedFrames();
		Log.e(TAG, "Aligned frames get into JAVA code");


		//Use MPOWriter to create Almalane's MPO file
		CameraController.Size imageSize = CameraController.getCameraImageSize();
		int jpegQuality = 95;

		int[] jpegdata_pointers = new int[mImageAmount];
		int[] jpegdata_sizes = new int[mImageAmount];

		try
		{
			for(int i = 0; i < mImageAmount; i++)
			{

				// Create buffer image to deal with exif tags.
				OutputStream os = null;

                String fileFormat = PluginManager.getInstance().getFileFormat();
                String evmark = String.format("_inputframe");
                File saveDir = PluginManagerBase.getSaveDir(false);
                File bufFile = new File(saveDir, fileFormat + evmark + i + ".jpg");
				//File bufFile = new File(ApplicationScreen.getMainContext().getFilesDir(), "buffermpo.jpeg");
				try
				{
					os = new FileOutputStream(bufFile);
				} catch (Exception e)
				{
					e.printStackTrace();
				}

				com.almalence.YuvImage image = new com.almalence.YuvImage(alignedFrames[i], ImageFormat.NV21,
						imageSize.getWidth(), imageSize.getHeight(), null);
				// to avoid problems with SKIA
				int cropHeight = image.getHeight() - image.getHeight() % 16;
				image.compressToJpeg(new Rect(0, 0, image.getWidth(), cropHeight), jpegQuality, os);


				ExifInterface ei = new ExifInterface(bufFile.getAbsolutePath());
				int exif_orientation;
				switch (mImageDataOrientation)
				{
					default:
					case 0:
						exif_orientation = ExifInterface.ORIENTATION_NORMAL;
						break;
					case 90:
						if (mCameraMirrored)
						{
							exif_orientation = ExifInterface.ORIENTATION_ROTATE_270;
						} else
						{
							exif_orientation = ExifInterface.ORIENTATION_ROTATE_90;
						}
						break;
					case 180:
						exif_orientation = ExifInterface.ORIENTATION_ROTATE_180;
						break;
					case 270:
						if (mCameraMirrored)
						{
							exif_orientation = ExifInterface.ORIENTATION_ROTATE_90;
						} else
						{
							exif_orientation = ExifInterface.ORIENTATION_ROTATE_270;
						}
						break;
				}
				ei.setAttribute(ExifInterface.TAG_ORIENTATION, "" + exif_orientation);
				ei.saveAttributes();

				// Copy buffer image with exif tags into result file.

				InputStream is;
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				int len;
				byte[] buf = new byte[4096];
				try
				{
					is = new FileInputStream(bufFile);
					while ((len = is.read(buf)) > 0)
					{
						bos.write(buf, 0, len);
					}
					is.close();
					bos.close();
				}
				catch (IOException eIO)
				{
					eIO.printStackTrace();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

//						is.close();
//						os.close();

				byte[] out_bytes = bos.toByteArray();
				int jpeg_size = out_bytes.length;//os.toByteArray().length;
				int pointer = SwapHeap.SwapToHeap(out_bytes/*os.toByteArray()*/);

				jpegdata_pointers[i] = pointer;
				jpegdata_sizes[i] = jpeg_size;

				bufFile.delete();
			}

			AlmaShotMPOWriter.Initialize(jpegdata_pointers, jpegdata_sizes, focusDistances, mImageAmount);
			byte[] mpo_data = AlmaShotMPOWriter.ConstructMPOData();

			return mpo_data;
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}


	@TargetApi(24)
	private void saveInputAndProcessedFrames()
	{
		try
		{
			Log.e(TAG, "saveInputAndProcessedFrames - start");
			// 0 - save only final result
			// 1 - save aligned input frames and final result
			// 2 - save all: non-aligned input frames, aligned input frames and final result
			
			if (FocusStackingProcessingPlugin.SavePreference != 0)
			{
				//Construct MPO file in memory as byte array
				byte[] mpo_data = getMPODataForAlignedFrames();

				// Saving aligned frames
				String fileFormat = PluginManager.getInstance().getFileFormat();
				String evmark = String.format("_alignedFrames");

				File saveDir = PluginManagerBase.getSaveDir(false);
				File file = new File(saveDir, fileFormat + evmark + ".mpo");
				FileOutputStream os = null;

				try
				{
					try
					{
						os = new FileOutputStream(file);
					} catch (Exception e)
					{
						// save always if not working saving to sdcard
						e.printStackTrace();
						saveDir = PluginManagerBase.getSaveDir(true);
						file = new File(saveDir, fileFormat + evmark + ".mpo");

						os = new FileOutputStream(file);
					}

					os.write(mpo_data);
					os.close();
				} catch (FileNotFoundException e1)
				{
					e1.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}


				if(FocusStackingProcessingPlugin.SavePreference == 2)
				{
					//Saving input frames
					fileFormat = PluginManager.getInstance().getFileFormat();
					for (int i = 0; i < mImageAmount; ++i)
					{
						evmark = String.format("_%3.1f", focusDistances.get(i));
						int yuvBuffer = AlmaShotFocusStacking.GetInputFrame(i);
						PluginManager.getInstance().saveInputFile(true, sessionID, i, null, yuvBuffer, fileFormat + evmark);
					}
				}
				
				
			}
			
			Log.e(TAG, "saveInputAndProcessedFrames - end");
		} catch (Exception e)
		{
			e.printStackTrace();
		}		
	}



	private class FocusStackingTask extends AsyncTask<Object, Object, Object>
	{
		@Override
		protected void onPreExecute()
		{
			FocusStackingProcessingPlugin.setProgressBarVisibility(true);
		}

		@Override
		protected void onPostExecute(Object result)
		{
			mHandler.sendEmptyMessage(FocusStackingProcessingPlugin.MSG_REQUEST_PREVIEW);
		}

		@Override
		protected Object doInBackground(Object... params)
		{
			Thread.currentThread().setPriority(Thread.NORM_PRIORITY);

			if(mCurrentFocusDepth > 1)
			{
				FStackingProcessing(false);

				baseFrame = null;
                resultIsBaseFrame = false;
			}
			else
			{
				baseFrame = AlmaShotFocusStacking.GetInputByteFrame(mBaseFrameIndex, 0, false);
				allInFocusYUV = null;

                resultIsBaseFrame = true;
			}
			
			return null;
		}
	}

	@Override
	public void onOrientationChanged(int orientation)
	{
		if (orientation != mDisplayOrientationCurrent)
		{
			mDisplayOrientationCurrent = orientation;

			int compensation1 = isAppInPortrait? 90 : 0;
			int compensation2 = isAppInPortrait? 90 : 180;
			mLayoutOrientationCurrent = (orientation == 0 || orientation == 180) ? orientation + compensation1 : orientation - compensation2;
			if (postProcessingRun)
			{
				this.buttonSave.setRotation(mLayoutOrientationCurrent);
				this.buttonSave.invalidate();
				this.buttonTrash.setRotation(mLayoutOrientationCurrent);
				this.buttonTrash.invalidate();
			}
		}
	}



	private final Handler						mHandler			 = new Handler(this);
	public static final int						MSG_REQUEST_PREVIEW	 = 1;

	private ShowOffView							imageView;

	private Button								buttonTrash;
	private Button								buttonSave;
	
	private static ProgressBar 	progressBar;

	private FocusStackingTask					fstackingTaskCurrent	= null;
	private FocusStackingTask					fstackingTaskPending	= null;

	private SeekBar								focusDepthSeekBar;
	private boolean								isTouchToReFocus 		= false;


	@Override
	public boolean isPostProcessingNeeded()
	{
		return EditMode;
	}

	@Override
	public void onStartPostProcessing()
	{
		Log.e(TAG, "onStartPostProcessing");
		postProcessingRun = true;
		
		mBitmap = null;
		
		isTouchToReFocus = false;

		CameraController.Size imageSize = CameraController.getCameraImageSize();

		postProcessingView = LayoutInflater.from(ApplicationScreen.getMainContext()).inflate(
				R.layout.plugin_processing_focusstacking_postprocessing, null);

		this.imageView = ((ShowOffView) postProcessingView.findViewById(R.id.imageHolder));
		this.buttonTrash = ((Button) postProcessingView.findViewById(R.id.fstacking_trashButton));
		this.buttonSave = ((Button) postProcessingView.findViewById(R.id.fstacking_saveButton));
		
		this.mBaseFrameIndex = Math.round(mImageAmount/2); // initial base frame is a middle frame of frames sequence
		
		this.mCurrentFocusDepth = Math.max(this.mBaseFrameIndex +1, mImageAmount - this.mBaseFrameIndex); //Initial processing result contains all input frames
        this.mMaxFocusDepth     = this.mCurrentFocusDepth;
		
		this.focusDepthSeekBar = ((SeekBar) postProcessingView.findViewById(R.id.focusDepthSeekBar));
        this.focusDepthSeekBar.setMax(100);
        this.focusDepthSeekBar.setProgress(100);
		this.focusDepthSeekBar.setOnSeekBarChangeListener(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());

        isAppInPortrait = prefs.getBoolean("appIsPortrait", CameraController.isSnapdgragonTestDevice? false : true);
		
//		Log.e(TAG, "mImageDataOrientation = " + mImageDataOrientation);
//		Log.e(TAG, "mSensorOrientation = " + mSensorOrientation);
//		Log.e(TAG, "mLayoutOrientationCurrent = " + mLayoutOrientationCurrent);
//		Log.e(TAG, "mDisplayOrientationCurrent = " + mDisplayOrientationCurrent);
//		Log.e(TAG, "MODEL: " + Build.MODEL.toLowerCase(Locale.US).replace(" ", ""));
		if(mImageDataOrientation == 90 || mImageDataOrientation == 270)
		{
			mResultImageWidth = mImageHeight;
			mResultImageHeight = mImageWidth;
		}
		else
		{
			mResultImageWidth = mImageWidth;
			mResultImageHeight = mImageHeight;
		}
		
		this.buttonTrash.setRotation(mLayoutOrientationCurrent);
		this.buttonSave.setRotation(mLayoutOrientationCurrent);
		TextView titleText = ((TextView) postProcessingView.findViewById(R.id.focusDepthTitleText));
		TextView maxText = ((TextView) postProcessingView.findViewById(R.id.focusDepthRightText));
		TextView minText = ((TextView) postProcessingView.findViewById(R.id.focusDepthLeftText));
		
		if(mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
			titleText.setText("DoF");
		
		titleText.setRotation(mLayoutOrientationCurrent);
		maxText.setRotation(mLayoutOrientationCurrent);
		minText.setRotation(mLayoutOrientationCurrent);

		this.imageView.setOnDataClickListener(this);
		this.imageView.setOnClickListener(this);
		this.buttonTrash.setOnClickListener(this);
		this.buttonSave.setOnClickListener(this);
		
		//add progress control
		progressBar = (ProgressBar) postProcessingView.findViewById(R.id.progressBarProcessing);
		progressBar.setVisibility(View.GONE);
		
		this.fstackingTaskCurrent = new FocusStackingTask();
		this.fstackingTaskCurrent.execute();
	}
	
	
	public static void setProgressBarVisibility(boolean visible)
	{
		if(progressBar != null)
		{
			progressBar.setVisibility(visible? View.VISIBLE : View.GONE);
		}
	}
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK && postProcessingRun)
		{
			AlmaShotFocusStacking.Release();

			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, 
					ApplicationInterface.MSG_CONTROL_UNLOCKED);

			ApplicationScreen.getGUIManager().lockControls = false;

			postProcessingRun = false;
			
			ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_POSTPROCESSING_FINISHED);

			return true;
		}

		return super.onKeyDown(keyCode, event);
	}



	@Override
	public void onClick(View v)
	{
		if (v == this.buttonTrash)
		{
			cancelAllTasks();
			AlmaShotFocusStacking.Release();

			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, 
					ApplicationInterface.MSG_CONTROL_UNLOCKED);

			ApplicationScreen.getGUIManager().lockControls = false;

			postProcessingRun = false;

			ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_POSTPROCESSING_FINISHED);
		} else if (v == this.buttonSave)
		{
			cancelAllTasks();
			new SaveTask(ApplicationScreen.instance).execute();
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
	{
		if (seekBar == this.focusDepthSeekBar && !isTouchToReFocus)
		{
            int iFocusDepth = calculateFocusDepth(this.focusDepthSeekBar.getProgress(), this.mMaxFocusDepth);

			if(iFocusDepth != mCurrentFocusDepth)
			{
				mCurrentFocusDepth = iFocusDepth;
				if (this.fstackingTaskCurrent == null)
				{
					this.fstackingTaskCurrent = new FocusStackingTask();
					this.fstackingTaskCurrent.execute();
				}
			 	else
				{
					if (this.fstackingTaskPending == null)
					{
						this.fstackingTaskPending = new FocusStackingTask();
					}
				}
			}
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar)
	{

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar)
	{

	}

    public int calculateFocusDepth(int percentage, int maxFocusDepth)
    {
        int iFocusDepth = 1; //Initial value

        int oneStepDepth = Math.round(100/maxFocusDepth); //How much % of progress bar is in one focus depth step
        float tmpFocusDepth = (float)percentage / (float)oneStepDepth; //Intermediate focus depth value. How to be smart rounded.
        tmpFocusDepth = Math.round(tmpFocusDepth * 2) / 2.0f; //Rounded to .0 or .5 value which closer.
        if(tmpFocusDepth == Math.round(tmpFocusDepth)) //focus depth value doesn't has a 0.5 peace.
            iFocusDepth = (int)tmpFocusDepth;
        else
            iFocusDepth = Math.round(tmpFocusDepth + 0.5f);

        if(iFocusDepth <= 0)
            iFocusDepth = 1;
        else if(iFocusDepth > maxFocusDepth)
            iFocusDepth = maxFocusDepth;

        return iFocusDepth;
    }

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
	{
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent)
	{
	}
	
	@Override
	public void onShowOffClick(float x, float y)
	{
		//Compensate 90 degree bitmap rotation in that 2 cases.
		if (isAppInPortrait && (mImageDataOrientation == 180 || mImageDataOrientation == 0))
		{
			float yTmp = y;
			y = mImageHeight - x;
			x = yTmp;
		}

		if (isAppInPortrait) {
			if (mImageDataOrientation == 90) {
				float yTmp = y;
				y = mImageHeight - x;
				x = yTmp;

			} else if (mImageDataOrientation == 180) {
				x = mResultImageWidth - x;
				y = mResultImageHeight - y;

			} else if (mImageDataOrientation == 270) {
				float xTmp = x;
				x = mImageWidth - y;
				y = xTmp;

			}
		}
		else
		{

			if (mImageDataOrientation == 180) {
				x = mResultImageWidth - x;
				y = mResultImageHeight - y;

			} else if (mImageDataOrientation == 270) {
				x = mResultImageHeight - x;
				y = mResultImageWidth - y;

			}
			else if (mImageDataOrientation == 90) {
				x = mImageWidth - x;
				y = mImageHeight - y;

			}
		}
		
		int focused_image_index = AlmaShotFocusStacking.GetFocusedFrameIndex(Math.round(x), Math.round(y));
		Log.d(TAG, "Focused image index = " + focused_image_index);
		if(focused_image_index < 0)
			focused_image_index = 0;
		
		
		if(mBaseFrameIndex != focused_image_index)
		{
			isTouchToReFocus = true;
			this.mBaseFrameIndex = focused_image_index;

			mMaxFocusDepth = Math.max(this.mBaseFrameIndex +1, mImageAmount - this.mBaseFrameIndex); //Initial processing result contains all input frames

            this.mCurrentFocusDepth = calculateFocusDepth(this.focusDepthSeekBar.getProgress(), this.mMaxFocusDepth);
			
			if(this.mCurrentFocusDepth > mMaxFocusDepth)
				this.mCurrentFocusDepth = mMaxFocusDepth;

			if (this.fstackingTaskCurrent == null)
			{
				this.fstackingTaskCurrent = new FocusStackingTask();
				this.fstackingTaskCurrent.execute();
			}
		 	else
			{
				if (this.fstackingTaskPending == null)
				{
					this.fstackingTaskPending = new FocusStackingTask();
				}
			}
		}
		else
		{
			FocusStackingProcessingPlugin.setProgressBarVisibility(true);
			Handler handler = new Handler();
			Runnable runnable = new Runnable() {

				@Override
				public void run() {
					FocusStackingProcessingPlugin.setProgressBarVisibility(false);
				}
			};
			handler.postDelayed(runnable, 100);
		}
	}

	private void saveImage()
	{
        saveInputAndProcessedFrames();

        int frame_len;
        int frame;
        if(resultIsBaseFrame)
        {
            frame_len = baseFrame.length;
            frame = SwapHeap.SwapToHeap(baseFrame);
        }
        else
        {
            frame_len = allInFocusYUV.length;
            frame = SwapHeap.SwapToHeap(allInFocusYUV);
        }

        PluginManager.getInstance().addToSharedMem("resultfromshared" + sessionID, "true");

        PluginManager.getInstance().addToSharedMem("writeorientationtag" + sessionID, "true");
        PluginManager.getInstance().addToSharedMem("resultframeorientation1" + sessionID,
                String.valueOf(mImageDataOrientation));
        PluginManager.getInstance().addToSharedMem("amountofresultframes" + sessionID, "1");
        PluginManager.getInstance().addToSharedMem("resultframe1" + sessionID, String.valueOf(frame));
        PluginManager.getInstance().addToSharedMem("resultframelen1" + sessionID, String.valueOf(frame_len));

        PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID, String.valueOf(mResultImageWidth));
        PluginManager.getInstance().addToSharedMem("saveImageHeight" + sessionID, String.valueOf(mResultImageHeight));

        PluginManager.getInstance().addToSharedMem("sessionID", String.valueOf(sessionID));

        AlmaShotFocusStacking.Release();
	}
	
	private Object syncObj = new Object();

	@Override
	public boolean handleMessage(Message msg)
	{
		if(msg.what == MSG_REQUEST_PREVIEW)
		{
			synchronized(syncObj)
			{
				FocusStackingProcessingPlugin.setProgressBarVisibility(false);
				requestPreviewUpdate();
			}
		}
		
		return true;
	}


	protected void requestPreviewUpdate()
	{
		isTouchToReFocus = false;

		if (this.fstackingTaskPending != null)
		{
			this.fstackingTaskCurrent = this.fstackingTaskPending;
			this.fstackingTaskCurrent.execute();
			this.fstackingTaskPending = null;
		} else
		{
			this.fstackingTaskCurrent = null;
			int dpiCount = 3;

			this.imageView.setData(null);

			if(mKeepResultBitmap && mBitmap != null)
			{
				mKeepResultBitmap = false;
				
				this.imageView.setData(mBitmap);
				this.imageView.invalidate();
				this.imageView.requestLayout();
			}
			else
			{
                int rotation01 = isAppInPortrait? 270 : 180;
                int rotation02 = isAppInPortrait? 90 : 0;
                int rotation03 = isAppInPortrait? mImageDataOrientation : (mImageDataOrientation - 90)%360;

				if(baseFrame !=null)
				{
					mBitmap = new ComplexBitmap(0, baseFrame, mImageWidth, mImageHeight, 0,
							0, mImageWidth, mImageHeight, dpiCount);
					mBitmap.setRotation((CameraController.isNexus5x || CameraController.isSnapdgragonTestDevice)? rotation01 : rotation03);

					this.imageView.setData(mBitmap);
					this.imageView.invalidate();
					this.imageView.requestLayout();
				}
				else
				{
					mBitmap = new ComplexBitmap(0, allInFocusYUV, mImageWidth, mImageHeight, 0,
							0, mImageWidth, mImageHeight, dpiCount);
					mBitmap.setRotation((CameraController.isNexus5x || CameraController.isSnapdgragonTestDevice)? rotation01 : rotation02);
					this.imageView.setData(mBitmap);
					this.imageView.invalidate();
					this.imageView.requestLayout();
				}
			}
			

			this.buttonTrash.setVisibility(View.VISIBLE);
			this.buttonSave.setVisibility(View.VISIBLE);
		}
	}
	

	protected void cancelAllTasks()
	{
		if (this.fstackingTaskCurrent != null)
			this.fstackingTaskCurrent.cancel(false);
		
		if (this.fstackingTaskPending != null)
			this.fstackingTaskPending.cancel(false);
	}

	private class SaveTask extends AsyncTask<Void, Void, Void>
	{
		private ProgressDialog	mSavingDialog;

		public SaveTask(Context context)
		{
			this.mSavingDialog = new ProgressDialog(context);
			this.mSavingDialog.setIndeterminate(true);
			this.mSavingDialog.setCancelable(false);
			this.mSavingDialog.setMessage("Saving");
		}

		@Override
		protected void onPreExecute()
		{
			this.mSavingDialog.show();
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			FocusStackingProcessingPlugin.this.saveImage();

			return null;
		}

		@Override
		protected void onPostExecute(Void v)
		{
			this.mSavingDialog.hide();

			AlmaShotFocusStacking.Release();

			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, 
					ApplicationInterface.MSG_CONTROL_UNLOCKED);

			ApplicationScreen.getGUIManager().lockControls = false;
			
			postProcessingRun = false;
			
			ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_POSTPROCESSING_FINISHED);
		}
	}
}
