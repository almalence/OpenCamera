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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
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
import com.almalence.focuscam.PluginProcessing;
import com.almalence.focuscam.R;
import com.almalence.focuscam.cameracontroller.CameraController;
import com.almalence.focusstacking.AlmaShotFocusStacking;
import com.almalence.util.Size;
//import android.util.Size;

/* <!-- +++
 import com.almalence.focuscam_plus.ConfigParser;
 import com.almalence.focuscam_plus.ApplicationScreen;
 import com.almalence.focuscam_plus.ApplicationInterface;
 import com.almalence.focuscam_plus.PluginManager;
 import com.almalence.focuscam_plus.PluginProcessing;
 import com.almalence.focuscam_plus.R;
 import com.almalence.focuscam_plus.cameracontroller.CameraController;
 +++ --> */
// <!-- -+-
//-+- -->
/***
 * Implements Focus stacking processing plugin.
 ***/

public class FocusStackingProcessingPlugin extends PluginProcessing implements OnClickListener,
		OnSeekBarChangeListener, OnItemSelectedListener, OnTaskCompleteListener, OnShowOffClickListener, Handler.Callback
{
	private String				TAG = "FStacking";
	private byte[]				allInFocusYUV;	// Result
//	private int					allInFocusYUV;	// Result
//	private int					baseFrameAddress;
	private byte[]				baseFrame;

	private static boolean		EditMode								= false;
	private static int			SavePreference;
	
	private static String		sEditModePref;
	private static String		sSaveModePref;

	private int					mLayoutOrientationCurrent				= 0;
	private int					mImageDataOrientation	= 0;
	private int					mDisplayOrientationCurrent				= 0;
	private int					mSensorOrientation						= 0;
	private boolean				mCameraMirrored							= false;

	private boolean				postProcessingRun						= false;
	
	private int					mImageWidth;
	private int					mImageHeight;
	
	private int					mResultImageWidth;
	private int					mResultImageHeight;
	
	private int					mImageAmount;
	private int[]				mInputFrames;
	private int[]				mInputFramesLength;
	
//	private int					mOutImageWidth;
//	private int					mOutImageHeight;
	
	protected static List<Long>						ResolutionsOutputMPixList;
	protected static List<CameraController.Size>	ResolutionsOutputSizeList;
	protected static List<String>					ResolutionsOutputIdxesList;
	protected static List<String>					ResolutionsOutputNamesList;
	
	public static String							sOutputImageSizeRearPref;
	public static String							sOutputImageSizeFrontPref;
	public static int								outputImageSizeIdxPreference;
	
	private List<Float>								focusDistances; //Focus distances for each input frame
//	
	private int										mBaseFrameIndex; //Frame around which focus depth will be changed
	private int										mCurrentFocusDepth; //How much frames to take for focus stacking process
//	
	private byte[]									mFocusAreasMap = null; //Shows on which input frame desired area is better focused
	private byte[]									mFocusAreasMapImage = null;
//	private int										mFocusAreasMapAddress = 0;
	private boolean									mShowFocusAreasMap = false;
	private boolean									mKeepResultBitmap = false;
	
	private ComplexBitmap							mBitmap = null;

	private long									sessionID = 0;

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
//			Log.e(TAG, "PAUSE................postProcessingRun = true. Call AlmaShotFocusStacking.Release()");
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
//		ContrastPreference = prefs.getString("contrastPrefHDR", "1");
//		mContrastPreference = prefs.getString("mcontrastPrefHDR", "2");
//		NoisePreference = prefs.getString("noisePrefHDR", "0");
//		ExpoPreference = prefs.getString("expoPrefHDR", "1");
//		ColorPreference = prefs.getString("colorPrefHDR", "1");

		EditMode = prefs.getBoolean(sEditModePref, false);

		SavePreference = Integer.parseInt(prefs.getString(sSaveModePref, "0"));
	}
	
	@Override
	public void onPreferenceCreate(PreferenceFragment pf)
	{
//		CharSequence[] entries = null;
//		CharSequence[] entryValues = null;
//
//		int idx = 0;
//		int currentIdx = -1;
//		String opt1 = "";
//		String opt2 = "";

//		opt1 = sOutputImageSizeRearPref;
//		opt2 = sOutputImageSizeFrontPref;
//		currentIdx = outputImageSizeIdxPreference;
//
//		if (currentIdx == -1)
//		{
//			currentIdx = 0;
//		}
//
//		entries = ResolutionsOutputNamesList.toArray(
//				new CharSequence[ResolutionsOutputNamesList.size()]);
//		entryValues = ResolutionsOutputIdxesList.toArray(
//				new CharSequence[ResolutionsOutputIdxesList.size()]);
//
//		if (ResolutionsOutputIdxesList != null)
//		{
//			ListPreference lp = (ListPreference)pf.findPreference(opt1);
//			ListPreference lp2 = (ListPreference)pf.findPreference(opt2);
//			
//			if (CameraController.getCameraIndex() == 0 && lp2 != null)
//				pf.getPreferenceScreen().removePreference(lp2);
//			else if (lp != null && lp2 != null)
//			{
//				pf.getPreferenceScreen().removePreference(lp);
//				lp = lp2;
//			}
//
//			if (lp != null)
//			{
//				lp.setEntries(entries);
//				lp.setEntryValues(entryValues);
//
//				if (currentIdx != -1)
//				{
//					// set currently selected image size
//					for (idx = 0; idx < entryValues.length; ++idx)
//					{
//						if (Integer.valueOf(entryValues[idx].toString()) == currentIdx)
//						{
//							lp.setValueIndex(idx);
//							break;
//						}
//					}
//				} else
//				{
//					lp.setValueIndex(0);
//				}
//
////				lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
////				{
////					public boolean onPreferenceChange(Preference preference, Object newValue)
////					{
////						MainActivity.outputImageSizeIdxPreference = Integer.parseInt(newValue.toString());
////						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(thiz.getActivity());
////						prefs.edit()
////								.putString(
////										MainActivity.CAMERA_INDEX == 0 ? MainActivity.sOutputImageSizeRearPref
////												: MainActivity.sOutputImageSizeFrontPref, String.valueOf(MainActivity.outputImageSizeIdxPreference)).commit();
////						return true;
////					}
////				});
//			}
//		}
	}
	
	
	@Override
	public void onCameraParametersSetup()
	{
//		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.instance.getBaseContext());
//		
//		populateOutputDimensions();
//		outputImageSizeIdxPreference = Integer.parseInt(prefs.getString(CameraController.getCameraIndex() == 0 ? sOutputImageSizeRearPref : sOutputImageSizeFrontPref, "0"));
//		CameraController.Size outputSize = ResolutionsOutputSizeList.get(outputImageSizeIdxPreference);
//		mOutImageWidth = outputSize.getWidth();
//		mOutImageHeight = outputSize.getHeight();
	}
	
//	public static void populateOutputDimensions()
//	{
//		ResolutionsOutputMPixList = new ArrayList<Long>();
//		ResolutionsOutputSizeList = new ArrayList<CameraController.Size>();
//		ResolutionsOutputIdxesList = new ArrayList<String>();
//		ResolutionsOutputNamesList = new ArrayList<String>();
//		
//		int imageSizeIdxPreference = CameraController.getCameraImageSizeIndex();
//		for (int i = imageSizeIdxPreference; i < CameraController.ResolutionsNamesList.size(); i++)
//		{
//			ResolutionsOutputMPixList.add(CameraController.ResolutionsMPixList.get(i));
//			ResolutionsOutputSizeList.add(CameraController.ResolutionsSizeList.get(i));
//			ResolutionsOutputIdxesList.add(CameraController.ResolutionsIdxesList.get(i));
//			ResolutionsOutputNamesList.add(CameraController.ResolutionsNamesList.get(i));
//		}
//	}	

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
		
		mCameraMirrored = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem(
				"cameraMirrored" + sessionID));
		
		mSensorOrientation = CameraController.getSensorOrientation(mCameraMirrored);
		
		
		mImageWidth = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("imageWidth" + sessionID));
		mImageHeight = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("imageHeight" + sessionID));
		
//		Log.e(TAG, "START!!!!");
//		AlmaShotFocusStacking.AlmaShotInitialize();
		mImageAmount = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
				"amountofcapturedframes" + sessionID));

		mInputFrames = new int[mImageAmount];
		mInputFramesLength = new int[mImageAmount];
		
		focusDistances = new ArrayList<Float>();
		
//		mFocusAreasMap = new byte[mImageWidth/16 * mImageHeight/16];

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
//		Log.e(TAG, "Almashot focus stacking initialized");

		
		if (!EditMode)
		{
			// focus stacking processing
//			FStackingReInit(0, mImageAmount);
			this.mBaseFrameIndex = Math.round(mImageAmount/2); // initial base frame is a middle frame of frames sequence
			this.mCurrentFocusDepth = Math.max(this.mBaseFrameIndex +1, mImageAmount - this.mBaseFrameIndex); //Initial processing result contains all input frames
			
			FStackingProcessing(false);
//			Log.e(TAG, "FStackingProcessing finished");
			
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

//			int imagesAmount = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
//					"amountofcapturedframes" + sessionID));
			AlmaShotFocusStacking.Release();
//			AlmaShotFocusStacking.AlmaShotRelease();
//			Log.e(TAG, "AlmaShotFocusStacking.Released");
		}
//		Log.e(TAG, "FINISHED!!!");
		
	}

//	private void FStackingReInit(int baseFrameIndex, int focusDepth)
//	{
//		Log.e(TAG, "FStackingPreview. baseFrameIndex = " + baseFrameIndex + " focusDepth = " + focusDepth);
//		//int[] compressed_frame = new int[focusDepth];
//		int frameAmount = mImageAmount;
//		int firstFrameIndex = 0;
//		int lastFrameIndex = mImageAmount - 1;
//		
//		if(baseFrameIndex > 0)
//		{
//			firstFrameIndex = baseFrameIndex - (focusDepth - 1);
//			lastFrameIndex = baseFrameIndex + (focusDepth - 1);
//			
//			if(firstFrameIndex < 0) firstFrameIndex = 0;
//			if(lastFrameIndex > (mImageAmount - 1)) lastFrameIndex = mImageAmount - 1;
//		}
//		
//		frameAmount = lastFrameIndex - firstFrameIndex + 1;
//		Log.e(TAG, "FStackingPreview. frameAmount = " + frameAmount);
//		int[] compressed_frame = new int[frameAmount];
//		float[] focusDist = new float[frameAmount];
//		
//		int index = 0;
//		for(int i = firstFrameIndex; i <= lastFrameIndex; i++)
//		{
//			compressed_frame[index] = mInputFrames[i];
//			focusDist[index] = focusDistances[i];
//			index++;
//		}
//		
//		AlmaShotFocusStacking.FStackingInitialize(compressed_frame, focusDist, frameAmount, mImageWidth, mImageHeight, mFocusAreasMap);
////		Log.e(TAG, "AddYUVFrames");
////		AlmaShotFocusStacking.AddYUVFrames(compressed_frame, focusDistances, frameAmount, mImageWidth, mImageHeight);
//		
//		//Saving input frames
////		String fileFormat = PluginManager.getInstance().getFileFormat();
////		for (int i = 0; i < imagesAmount; ++i)
////		{
////			String evmark = String.format("_%01d", i);
////			int yuvBuffer = AlmaShotFocusStacking.getInputFrame(i);
////			PluginManager.getInstance().saveInputFile(true, sessionID, i, null, yuvBuffer, fileFormat + evmark);
////		}
//		
////		Log.e(TAG, "Align");
////		AlmaShotFocusStacking.Align(mFocusAreasMap, mImageWidth, mImageHeight, frameAmount);
//		
//		
////		for(int i = 0; i < imagesAmount; ++i)
////		{
////			int alignedFrame = AlmaShotFocusStacking.getAlignedInputFrame(i);
////			Log.e(TAG, "Aligned frame " + i + " is " + alignedFrame);
////		}
////		Log.e(TAG, "GetFocusAreasMap");
////		res = AlmaShotFocusStacking.GetFocusAreasMap(mImageWidth, mImageHeight, imagesAmount, focusedAreasMap);
////		Log.e(TAG, "GetFocusAreasMap res = " + res);
//		
//		
//		
////		int nf = FocusStackingProcessingPlugin.getNoise();
////		
////		if(CameraController.isNexus6 && CameraController.isUseCamera2())
////			nf = -1;
////
////		AlmaShotFocusStacking.HDRPreview(imagesAmount, mImageWidth, mImageHeight, pview, FocusStackingProcessingPlugin.getExposure(true),
////				FocusStackingProcessingPlugin.getVividness(true), FocusStackingProcessingPlugin.getContrast(true),
////				FocusStackingProcessingPlugin.getMicrocontrast(true), 0, nf, mCameraMirrored);
//
//		// android thing (OutOfMemory for bitmaps):
//		// http://stackoverflow.com/questions/3117429/garbage-collector-in-android
//		System.gc();
//	}

	private void FStackingProcessing(boolean transformResult)
	{
		float focusDistance = focusDistances.get(mBaseFrameIndex);
//		if(allInFocusYUV != -1)
//			AlmaShotFocusStacking.FStackingFreeResult(allInFocusYUV);
		allInFocusYUV = AlmaShotFocusStacking.Process(focusDistance, mCurrentFocusDepth, mImageDataOrientation, mCameraMirrored, transformResult);
//		allInFocusYUV = AlmaShotFocusStacking.FStackingProcess(mImageDataOrientation, mCameraMirrored, transformResult);
	}
	
	
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
				int[] alignedFrames = AlmaShotFocusStacking.GetAlignedFrames();
				Log.e(TAG, "Aligned frames get into JAVA code");
				//Saving aligned frames
				String fileFormat = PluginManager.getInstance().getFileFormat();
//				for (int i = 0; i < mImageAmount; ++i)
//				{
//					String evmark = String.format("_aligned_%3.1f", focusDistances.get(i));
//					int yuvBuffer = alignedFrames[i];//AlmaShotFocusStacking.GetAlignedFrame(i);
//					PluginManager.getInstance().saveInputFile(true, sessionID, i, null, yuvBuffer, fileFormat + evmark);
//				}
				
				String evmark = String.format("_alignedFrames");
				PluginManager.getInstance().saveInputFileMPO(true, sessionID, mImageAmount, alignedFrames, fileFormat + evmark);
				Log.e(TAG, "Aligned frames saved into MPO file");
				
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
			Log.e(TAG, "FocusStackingTask.onPreExecute " + this);
			FocusStackingProcessingPlugin.setProgressBarVisibility(true);
		}

		@Override
		protected void onPostExecute(Object result)
		{
			Log.e(TAG, "FocusStackingTask.onPostExecute " + this);
//			FocusStackingProcessingPlugin.setProgressBarVisibility(false);
			mHandler.sendEmptyMessage(FocusStackingProcessingPlugin.MSG_REQUEST_PREVIEW);
//			requestPreviewUpdate();
		}

		@Override
		protected Object doInBackground(Object... params)
		{
			Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
			Log.e(TAG, "FocusStackingTask.doInBackground " + this);
			
			if(mCurrentFocusDepth > 1)
			{
				Log.e(TAG, "Focus depth = " + mCurrentFocusDepth + " Start processing");
				FStackingProcessing(false);
				Log.e(TAG, "Processing finished");
				
//				baseFrameAddress = 0;
				baseFrame = null;
			}
			else
			{
				Log.e(TAG, "Focus depth = " + mCurrentFocusDepth + " get input byte frame " + mBaseFrameIndex);
//				allInFocusYUV = AlmaShotFocusStacking.GetInputByteFrame(mBaseFrameIndex, mImageDataOrientation, mCameraMirrored);
//				baseFrameAddress = AlmaShotFocusStacking.GetInputFrame(mBaseFrameIndex);
				baseFrame = AlmaShotFocusStacking.GetInputByteFrame(mBaseFrameIndex, 0, false);
				allInFocusYUV = null;
				Log.e(TAG, "Input byte array get");
			}
			
			return null;
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
				this.buttonSave.setRotation(mLayoutOrientationCurrent);
				this.buttonSave.invalidate();
				this.buttonTrash.setRotation(mLayoutOrientationCurrent);
				this.buttonTrash.invalidate();
				this.buttonFMap.setRotation(mLayoutOrientationCurrent);
				this.buttonFMap.invalidate();
			}
		}
	}


	
	
	

	private int									SXP					=  0;
	private int									SYP					=  0;
	public static int							mPreviewWidth;
	public static int							mPreviewHeight;
	public static int							mDisplayWidth;
	public static int							mDisplayHeight;
	private Size								mPreviewSize;
	private Size								mInputFrameSize;
	private int[]								ARGBBuffer;
	private Bitmap								PreviewBitmap;
	
	private final Handler						mHandler			 = new Handler(this);
	public static final int						MSG_REQUEST_PREVIEW	 = 1;
	public static final int						MSG_FULLRES_CALCULATED = 2;

	private ComplexBitmap						bitmapRes;
	private ShowOffView							imageView;
//	private TouchImageView						imageView;

	private Button								buttonTrash;
	private Button								buttonFMap;
	private Button								buttonSave;
	
	private static ProgressBar 	progressBar;

	private boolean								saving					= false;
	private boolean								saveButtonPressed		= false;
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
		
		mShowFocusAreasMap = false;
		mFocusAreasMap = null;
		
		mBitmap = null;
		
		isTouchToReFocus = false;

		CameraController.Size imageSize = CameraController.getCameraImageSize();

		postProcessingView = LayoutInflater.from(ApplicationScreen.getMainContext()).inflate(
				R.layout.plugin_processing_focusstacking_postprocessing, null);

		this.imageView = ((ShowOffView) postProcessingView.findViewById(R.id.imageHolder));
//		this.imageView = ((TouchImageView) postProcessingView.findViewById(R.id.imageHolder));
		this.buttonTrash = ((Button) postProcessingView.findViewById(R.id.fstacking_trashButton));
		this.buttonFMap = ((Button) postProcessingView.findViewById(R.id.fstacking_fmapButton));
		this.buttonSave = ((Button) postProcessingView.findViewById(R.id.fstacking_saveButton));
		
		this.mBaseFrameIndex = Math.round(mImageAmount/2); // initial base frame is a middle frame of frames sequence
		
		this.mCurrentFocusDepth = Math.max(this.mBaseFrameIndex +1, mImageAmount - this.mBaseFrameIndex); //Initial processing result contains all input frames
		
		this.focusDepthSeekBar = ((SeekBar) postProcessingView.findViewById(R.id.focusDepthSeekBar));
		this.focusDepthSeekBar.setMax((this.mCurrentFocusDepth - 1)/* * 10*/); //0 - is a minimum value of seek bar. mImageAmount's minimum value is 1.
		this.focusDepthSeekBar.setProgress((this.mCurrentFocusDepth - 1)/* * 10*/); //Initially full range focus stacking is calculating
		this.focusDepthSeekBar.setOnSeekBarChangeListener(this);

		saveButtonPressed = false;
		
		getDisplaySize();
		
		Log.e(TAG, "mImageDataOrientation = " + mImageDataOrientation);
		Log.e(TAG, "mSensorOrientation = " + mSensorOrientation);
		Log.e(TAG, "mLayoutOrientationCurrent = " + mLayoutOrientationCurrent);
		Log.e(TAG, "mDisplayOrientationCurrent = " + mDisplayOrientationCurrent);
		Log.e(TAG, "MODEL: " + Build.MODEL.toLowerCase(Locale.US).replace(" ", ""));
		if(mImageDataOrientation == 90 || mImageDataOrientation == 270)
		{
			mInputFrameSize = new Size(imageSize.getHeight(), imageSize.getWidth());
			mPreviewSize = new Size(mDisplayHeight, mDisplayWidth);
			mResultImageWidth = mImageHeight;
			mResultImageHeight = mImageWidth;
		}
		else
		{
			mInputFrameSize = new Size(imageSize.getWidth(), imageSize.getHeight());
			mPreviewSize = new Size(mDisplayWidth, mDisplayHeight);
			mResultImageWidth = mImageWidth;
			mResultImageHeight = mImageHeight;
		}
		
		this.buttonTrash.setRotation(mLayoutOrientationCurrent);
		this.buttonFMap.setRotation(mLayoutOrientationCurrent);
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
		this.buttonFMap.setOnClickListener(this);
		this.buttonSave.setOnClickListener(this);
		
		//add progress control
		progressBar = (ProgressBar) postProcessingView.findViewById(R.id.progressBarProcessing);
		progressBar.setVisibility(View.GONE);
		
		mFocusAreasMap = null;
		
		Log.e(TAG, "new FocusStackinTask execute!");
		this.fstackingTaskCurrent = new FocusStackingTask();
		this.fstackingTaskCurrent.execute();
		
//		Log.e(TAG, "INITIAL. Set initial downscaled input frame to ImageView -- start");
//		int baseFrame = AlmaShotFocusStacking.GetInputFrame(mBaseFrameIndex);
//		Log.e(TAG, "INITIAL. GetInputFrame done");
//		if(baseFrame != 0)
//		{
//			float scale = Math.max(mImageWidth, mImageHeight) / Math.max(mDisplayWidth, mDisplayHeight) + 4;
//			Log.e(TAG, "INITIAL. scale = " + scale);
//			
//			int scaledWidth = Math.round(mImageWidth/scale);
//			int scaledHeight = Math.round(mImageHeight/scale);
//			Rect rect = new Rect(0, 0, mImageWidth, mImageHeight);
////			ImageConversion.TransformNV21N(baseFrame, baseFrame, mImageWidth, mImageHeight, CameraController.isNexus5x? 1 : 0, 0, 0);
//			int[] ARGBBuffer = ImageConversion.NV21toARGB(baseFrame, new Size(mImageWidth, mImageHeight), rect, new Size(scaledWidth, scaledHeight));
//			Log.e(TAG, "INITIAL. NV21toARGB done");
//			Bitmap bitmap = Bitmap.createBitmap(ARGBBuffer, scaledWidth, scaledHeight, Config.ARGB_8888);
//			Log.e(TAG, "INITIAL. createBitmap done");
//			
//			Matrix matrix = new Matrix();
//			matrix.postRotate(CameraController.isNexus5x? 270 : 90);
//			Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap , 0, 0, bitmap .getWidth(), bitmap .getHeight(), matrix, true);
//			
//			Log.e(TAG, "INITIAL. createRotatedBitmap done");
//			BitmapDrawable bm0 = new BitmapDrawable(MainScreen.getAppResources(), rotatedBitmap);
//			Log.e(TAG, "INITIAL. new BitmapDrawable done");
//			
////			ComplexBitmap bm0 = new ComplexBitmap(baseFrame, allInFocusYUV, mImageWidth, mImageHeight, 0,
////					0, mImageWidth, mImageHeight, 3);
////			bm0.setRotation(CameraController.isNexus5x? 270 : 90);
//
//			this.imageView.setData(bm0);
//			Log.e(TAG, "INITIAL. imageView.setData done");
//			
//			this.imageView.invalidate();
//			this.imageView.requestLayout();
//			Log.e(TAG, "INITIAL. Set initial downscaled input frame to ImageView -- end");
//		}
		
//		Log.e(TAG, "new FocusStackinTask execute!");
//		this.fstackingTaskCurrent = new FocusStackingTask();
//		this.fstackingTaskCurrent.execute();
	}
	
	
	public static void setProgressBarVisibility(boolean visible)
	{
		if(progressBar != null)
		{
			progressBar.setVisibility(visible? View.VISIBLE : View.GONE);
		}
	}
	
	public void getDisplaySize()
	{
		Display display = ((WindowManager) ApplicationScreen.instance.getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		Point dis = new Point();
		display.getSize(dis);

		CameraController.Size imageSize = CameraController.getCameraImageSize();
		float imageRatio = (float) imageSize.getWidth() / (float) imageSize.getHeight();
		float displayRatio = (float) dis.y / (float) dis.x;

		if (imageRatio > displayRatio)
		{
			mDisplayWidth = dis.y;
			mDisplayHeight = (int) ((float) dis.y / (float) imageRatio);
		} else
		{
			mDisplayWidth = (int) ((float) dis.x * (float) imageRatio);
			mDisplayHeight = dis.x;
		}
		return;
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
			
//			if(mFocusAreasMapAddress != 0)
//				SwapHeap.FreeFromHeap(mFocusAreasMapAddress);

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
			Log.e(TAG, "SAVE. Button pressed");
			cancelAllTasks();
//			saveButtonPressed = true;
			new SaveTask(ApplicationScreen.instance).execute();
			
//			if(mFocusAreasMapAddress != 0)
//				SwapHeap.FreeFromHeap(mFocusAreasMapAddress);
//			this.buttonTrash.setVisibility(View.GONE);
//			this.buttonSave.setVisibility(View.GONE);
		} else if (v == this.buttonFMap)
		{
			mShowFocusAreasMap = !mShowFocusAreasMap;
			requestPreviewUpdate();
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
	{
		if (seekBar == this.focusDepthSeekBar && !isTouchToReFocus)
		{
			int iFocusDepth = progress/*/10*/ + 1;
			if(iFocusDepth != mCurrentFocusDepth)
			{
				Log.e(TAG, "iFocusDepth = " + iFocusDepth);
				mCurrentFocusDepth = iFocusDepth;
				if (this.fstackingTaskCurrent == null)
				{
					Log.e(TAG, "fstackingTaskCurrent == null. new FocusStackinTask execute!");
					this.fstackingTaskCurrent = new FocusStackingTask();
					this.fstackingTaskCurrent.execute();
				}
			 	else
				{
			 		Log.e(TAG, "fstackingTaskCurrent != null FocusStackinTask already executed");
					if (this.fstackingTaskPending == null)
					{
						Log.e(TAG, "fstackingTaskPending == null. Create fstackingTaskPending");
						this.fstackingTaskPending = new FocusStackingTask();
					}
					else
					{
//						Log.e(TAG, "pending FocusStackinTask already created");
					}
				}
			}
//			CameraController.setCameraFocusDistance(iDistance / 100);
//			preferences.edit().putFloat(MainScreen.sFocusDistancePref, (float) iDistance / 100).commit();
//			mFocusDistance = iDistance / 100;
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

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
	{
//		this.selectPreset(position);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent)
	{

	}
	
	@Override
	public void onShowOffClick(float x, float y)
	{
//		Log.e(TAG, "REAL IMAGE x = " + mImageWidth + " y = " + mImageHeight);
		
		//Compensate 90 degree bitmap rotation in that 2 cases.
		if (mImageDataOrientation == 180 || mImageDataOrientation == 0)
		{
			float yTmp = y;
			y = mImageHeight - x;
			x = yTmp;
		}
//		else if(mImageDataOrientation == 0)
//		{
//			float xTmp = x;
//			x = mImageWidth - y;
//			y = xTmp;
//		}
//		Log.e(TAG, "CLICK ON REAL IMAGE. x = " + x + " y = " + y);
//		Log.e(TAG, "Focus area map lenght = " + this.mFocusAreasMap.length);
//		Log.e(TAG, "Image data orientation = " + mImageDataOrientation);
//		
//		int x_lenght = this.mResultImageWidth/16;
////		int y_lenght = this.mResultImageHeight/16;
//		
//		int index_x = Math.round(x/16 - 1);
//		int index_y = Math.round(y/16 - 1);
//		
//		int map_index = index_y * x_lenght + index_x;
//		
//		if(map_index > this.mFocusAreasMap.length - 1)
//			return;
//		
//		int focused_image_index = this.mFocusAreasMap[map_index];
//		Log.e(TAG, "Map index = " + map_index);
//		Log.e(TAG, "Focused image index = " + focused_image_index);
		
		if (mImageDataOrientation == 90)
		{
			float yTmp = y;
			y = mImageHeight - x;
			x = yTmp;
		}
		else if (mImageDataOrientation == 180)
		{
			
			x = mResultImageWidth - x;
			y = mResultImageHeight - y;
		}
		else if (mImageDataOrientation == 270)
		{
			float xTmp = x;
			x = mImageWidth - y;
			y = xTmp;
		}
		
		Log.e(TAG, "Final coordinates. x = " + x + " y = " + y);
		
		int focused_image_index = AlmaShotFocusStacking.GetFocusedFrameIndex(Math.round(x), Math.round(y));
		Log.e(TAG, "Focused image index = " + focused_image_index);
		if(focused_image_index < 0)
			focused_image_index = 0;
		
		
		if(mBaseFrameIndex != focused_image_index || mCurrentFocusDepth != 1 )
		{
			isTouchToReFocus = true;
//			this.mCurrentFocusDepth = 1;
			this.mBaseFrameIndex = focused_image_index;
			//this.mCurrentFocusDepth = 1;
			
			int maxFocusDepth = Math.max(this.mBaseFrameIndex +1, mImageAmount - this.mBaseFrameIndex); //Initial processing result contains all input frames
			
			this.focusDepthSeekBar.setMax((maxFocusDepth - 1)/* * 10*/); //0 - is a minimum value of seek bar. mImageAmount's minimum value is 1.
			if(this.mCurrentFocusDepth > maxFocusDepth)
				this.mCurrentFocusDepth = maxFocusDepth;
			
			this.focusDepthSeekBar.setProgress(this.mCurrentFocusDepth - 1);
			//this.focusDepthSeekBar.setProgress(0); //Initially full range focus stacking is calculating
			
			if (this.fstackingTaskCurrent == null)
			{
				Log.e(TAG, "fstackingTaskCurrent == null. new FocusStackinTask execute!");
				this.fstackingTaskCurrent = new FocusStackingTask();
				this.fstackingTaskCurrent.execute();
			}
		 	else
			{
		 		Log.e(TAG, "fstackingTaskCurrent != null. FocusStackinTask already executed");
				if (this.fstackingTaskPending == null)
				{
					Log.e(TAG, "fstackingTaskPending == null. create fstackingTaskPending");
					this.fstackingTaskPending = new FocusStackingTask();
				}
				else
				{
//					Log.e(TAG, "pending FocusStackinTask already created");
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
			
			PluginManager.getInstance().addToSharedMem("sessionID", String.valueOf(sessionID));

			AlmaShotFocusStacking.Release();
	}
	
	private Object syncObj = new Object();
	@Override
	public boolean handleMessage(Message msg)
	{
		switch (msg.what)
		{
			case MSG_REQUEST_PREVIEW:
				synchronized(syncObj)
				{
					Log.e(TAG, "handle MSG_REQUEST_PREVIEW");
					FocusStackingProcessingPlugin.setProgressBarVisibility(false);
					requestPreviewUpdate();
					Log.e(TAG, "requestPreviewUpdate() done");
				} break;
			case MSG_FULLRES_CALCULATED:
				synchronized(syncObj)
				{
					Log.e(TAG, "handle MSG_FULLRES_CALCULATED");
					this.imageView.setData(bitmapRes);
					this.imageView.invalidate();
					this.imageView.requestLayout();
					Log.e(TAG, "Full-res ComplexBitmap set");
				} break;
		}
		
		return true;
	}


	protected void requestPreviewUpdate()
	{
		isTouchToReFocus = false;
//		Bitmap previewBitmap = getPreviewBitmap();
//		this.imageView.invalidate();
//		this.imageView.setImageBitmap(previewBitmap);
		if(mFocusAreasMap == null)
		{
			mFocusAreasMap = AlmaShotFocusStacking.GetFocusAreasMap();
			createFocusAreasMapImage();
		}
		
		if(mShowFocusAreasMap)
		{
			this.imageView.setData(null);
//			this.imageView.setImageBitmap(null);
			
//			Rect rect = new Rect(0, 0, mImageWidth/16, mImageHeight/16);
//			int[] ARGBBuffer = ImageConversion.NV21ByteArraytoARGB(mFocusAreasMapImage, mImageWidth/16, mImageHeight/16, rect, mImageWidth/16, mImageHeight/16);
//			
//			Bitmap bitmap = Bitmap.createBitmap(ARGBBuffer, mImageWidth/16, mImageHeight/16, Config.ARGB_8888);
			
			ComplexBitmap bm0 = new ComplexBitmap(0, mFocusAreasMapImage, mImageWidth/16, mImageHeight/16, 0,
					0, mImageWidth/16, mImageHeight/16, 1);
//			ComplexBitmap bm0 = new ComplexBitmap(mFocusAreasMapAddress, null, mImageWidth/16, mImageHeight/16, 0,
//					0, mImageWidth/16, mImageHeight/16, 1);
			Log.e(TAG, "Created ComplexBitmap");
	//		this.imageView.setProgress(0.0f);
			
			Log.e(TAG, "set Bitmap to ShowOff View");
			bm0.setRotation((mImageDataOrientation + mLayoutOrientationCurrent)%360);
			
			this.imageView.setData(bm0);
//			this.imageView.setImageBitmap(bitmap);
			Log.e(TAG, "preview imageView data set");
			this.imageView.invalidate();
			this.imageView.requestLayout();
			
			this.buttonTrash.setVisibility(View.VISIBLE);
			this.buttonFMap.setVisibility(View.VISIBLE);
			this.buttonSave.setVisibility(View.VISIBLE);
			
			mKeepResultBitmap = true;
			return;
		}
		
		
		
//		FileOutputStream out;
//		try {
//			String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/FStacking/focus_map";
//			out = new FileOutputStream(fileName);
//			out.write(mFocusAreasMap);
//			out.close();
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		  
		
		if (this.fstackingTaskPending != null)
		{
			Log.e(TAG, "requestPreviewUpdate(). fstackingTaskPending != null. this.fstackingTaskCurrent = this.fstackingTaskPending; this.fstackingTaskCurrent.execute();");
			this.fstackingTaskCurrent = this.fstackingTaskPending;
			this.fstackingTaskCurrent.execute();

			Log.e(TAG, "requestPreviewUpdate(). fstackingTaskPending set to null");
			this.fstackingTaskPending = null;
		} else
		{
			Log.e(TAG, "requestPreviewUpdate(). this.fstackingTaskPending == null. update Result image!");
			Log.e(TAG, "requestPreviewUpdate(). fstackingTaskCurrent set to null");
			this.fstackingTaskCurrent = null;
			int dpiCount = 3;
//			int dpiCount = 1;
			// If image is very large, decrease dpiCount to prevent OutOfMemmory.
//			if (mImageWidth > 6000) {
//				dpiCount = 2;
//			}
			
//			if(mFocusAreasMap != null)
//			{
//				Log.e(TAG, "mFocusAreasMap length = " + mFocusAreasMap.length);
//			}
			
			this.imageView.setData(null);
//			this.imageView.setImageBitmap(null);
//			ComplexBitmap bm0 = null;
			
			if(mKeepResultBitmap && mBitmap != null)
			{
				mKeepResultBitmap = false;
				
				this.imageView.setData(mBitmap);
				this.imageView.invalidate();
				this.imageView.requestLayout();
			}
			else
			{
//				if(baseFrameAddress != 0)
				if(baseFrame !=null)
				{
//					mBitmap = new ComplexBitmap(baseFrameAddress, null, mImageWidth, mImageHeight, 0,
//							0, mImageWidth, mImageHeight, dpiCount);
					mBitmap = new ComplexBitmap(0, baseFrame, mImageWidth, mImageHeight, 0,
							0, mImageWidth, mImageHeight, dpiCount);
					mBitmap.setRotation((CameraController.isNexus5x || CameraController.isSnapdgragonTestDevice)? 270 : mImageDataOrientation);
	//				if(mImageDataOrientation == 90 || mImageDataOrientation == 270)
	//					bm0.setRotation(270);
	//				else
	//					bm0.setRotation(90);
					this.imageView.setData(mBitmap);
					this.imageView.invalidate();
					this.imageView.requestLayout();
				}
				else
				{
					mBitmap = new ComplexBitmap(0, allInFocusYUV, mImageWidth, mImageHeight, 0,
							0, mImageWidth, mImageHeight, dpiCount);
					mBitmap.setRotation((CameraController.isNexus5x || CameraController.isSnapdgragonTestDevice)? 270 : 90);
					this.imageView.setData(mBitmap);
					this.imageView.invalidate();
					this.imageView.requestLayout();
					
					
	//				final AtomicInteger sync = new AtomicInteger(1);
	//				synchronized (sync)
	//				{
	//					Log.e(TAG, "RESULT PREVIEW. -- start");
	//					
	//					float scale = Math.max(mImageWidth, mImageHeight) / Math.max(mDisplayWidth, mDisplayHeight) + 4;
	//					
	//					int scaledWidth = Math.round(mImageWidth/scale);
	//					int scaledHeight = Math.round(mImageHeight/scale);
	//					Rect rect = new Rect(0, 0, mImageWidth, mImageHeight);
	////					ImageConversion.TransformNV21N(baseFrame, baseFrame, mImageWidth, mImageHeight, CameraController.isNexus5x? 1 : 0, 0, 0);
	//					int[] ARGBBuffer = ImageConversion.NV21toARGB(allInFocusYUV, new Size(mImageWidth, mImageHeight), rect, new Size(scaledWidth, scaledHeight));
	//					Bitmap bitmap = Bitmap.createBitmap(ARGBBuffer, scaledWidth, scaledHeight, Config.ARGB_8888);
	//					
	//					Matrix matrix = new Matrix();
	//					matrix.postRotate(CameraController.isNexus5x? 270 : 90);
	//					Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap , 0, 0, bitmap .getWidth(), bitmap .getHeight(), matrix, true);
	//					
	//					BitmapDrawable bm0 = new BitmapDrawable(MainScreen.getAppResources(), rotatedBitmap);
	//					
	//					this.imageView.setData(bm0);
	//					this.imageView.invalidate();
	//					this.imageView.requestLayout();
	//					
	//					Log.e(TAG, "RESULT PREVIEW. -- end");
	//					
	//					new Thread()
	//					{
	//						@Override
	//						public void run()
	//						{
	////							Log.e(TAG, "RUN COMPLEX BITMAP CREATION");
	//							int dpiCount = 3;
	//							if (mImageWidth > 6000)
	//								dpiCount = 2;
	//							
	//							bitmapRes = new ComplexBitmap(0, allInFocusYUV, mImageWidth, mImageHeight, 0,
	//									0, mImageWidth, mImageHeight, dpiCount);
	//								bitmapRes.setRotation(CameraController.isNexus5x? 270 : 90);
	//							
	////							Log.e(TAG, "COMPLEX BITMAP CREATED");
	//							
	//							mHandler.sendEmptyMessage(FocusStackingProcessingPlugin.MSG_FULLRES_CALCULATED);
	//							
	////								synchronized (sync)
	////								{
	////									sync.decrementAndGet();
	////									sync.notify();
	////								}
	//						}
	//					}.start();					
	//				}
					
					
	//				Log.e(TAG, "111111111 create scaled Bitmap from NV21 frame");
	//				float scale = Math.max(mResultImageWidth / mDisplayWidth, mResultImageHeight/ mDisplayHeight);
	//				Rect rect = new Rect(0, 0, mResultImageWidth, mResultImageHeight);
	//				int[] ARGBBuffer = ImageConversion.NV21toARGB(allInFocusYUV, new Size(mResultImageWidth, mResultImageHeight), rect, new Size(Math.round(mResultImageWidth/scale), Math.round(mResultImageHeight/scale)));
	//				Bitmap bitmap = Bitmap.createBitmap(ARGBBuffer, Math.round(mResultImageWidth/scale), Math.round(mResultImageHeight/scale), Config.ARGB_8888);
	//				Log.e(TAG, "222222222 Bitmnap created");
	//				BitmapDrawable bm0 = new BitmapDrawable(MainScreen.getAppResources(), bitmap);
	//				Log.e(TAG, "3333333333 Drawbale created for ShowOff view");
	//				
	//				this.imageView.setData(bm0);
				}
			}
			
			
//			this.imageView.setData(bm0);
			
			
//			if(baseFrameAddress != 0)
//			{
//				Log.e(TAG, "YuvBitmap.createFromAddress -- start");
//				Bitmap baseFrameBitmap = YuvBitmap.createFromAddress(baseFrameAddress, mImageWidth, mImageHeight, 0, 0, mImageWidth, mImageHeight);
//				Log.e(TAG, "YuvBitmap.createFromAddress -- end");
//			}
			
//			Log.e(TAG, "111111111 set Bitmap to ShowOff View");
//			
//			float scale = 1;//Math.max(mImageWidth / mDisplayWidth, mImageHeight/ mDisplayHeight);
//			
//			Rect rect = new Rect(0, 0, mImageWidth, mImageHeight);
//			int[] ARGBBuffer = ImageConversion.NV21ByteArraytoARGB(allInFocusYUV, mImageWidth, mImageHeight, rect, Math.round(mImageWidth/scale), Math.round(mImageHeight/scale));
//			Log.e(TAG, "22222222  preview imageView data set");
//			Bitmap bitmap = Bitmap.createBitmap(ARGBBuffer, Math.round(mImageWidth/scale), Math.round(mImageHeight/scale), Config.ARGB_8888);
////			
////			this.imageView.setImageBitmap(bitmap);
//			Log.e(TAG, "333333333  preview imageView data set");
//			this.imageView.invalidate();
//			this.imageView.requestLayout();
			
			this.buttonTrash.setVisibility(View.VISIBLE);
			this.buttonFMap.setVisibility(View.VISIBLE);
			this.buttonSave.setVisibility(View.VISIBLE);
		}
		
//		if (this.previewTaskCurrent == null)
//		{
//			this.previewTaskCurrent = new AdjustmentsPreviewTask();
//			this.previewTaskCurrent.execute();
//		} else
//		{
//			if (this.previewTaskPending == null)
//			{
//				this.previewTaskPending = new AdjustmentsPreviewTask();
//			}
//		}
	}
	
	private void createFocusAreasMapImage()
	{
		int sx = mImageWidth/16;
		int sy = mImageHeight/16;
		
		int imageSize = sx*sy+2*((sx+1)/2)*((sy+1)/2);
		
		int offset = sx*sy;
		
		mFocusAreasMapImage = new byte[imageSize];
		
		int yIndex = 0;
        int uvIndex = offset;
		int R, G, B, Y, V, U;
		int index = 0;
	        
		for (int j = 0; j < sy; j++)
		{
            for (int i = 0; i < sx; i++)
            {
            	int frameIndex = mFocusAreasMap[index];
            	
            	switch(frameIndex)
            	{
	            	case 0: //RED
	            	{
	            		R = 255;
	            		G = 0;
	            		B = 0;
	            		break;
	            	}
	            	case 1: //ORANGE
	            	{
	            		R = 255;
	            		G = 125;
	            		B = 0;
	            		break;
	            	}
	            	case 2: //YELLOW
	            	{
	            		R = 255;
	            		G = 255;
	            		B = 0;
	            		break;
	            	}
	            	case 3: //GREEN
	            	{
	            		R = 0;
	            		G = 255;
	            		B = 0;
	            		break;
	            	}
	            	case 4: //BLUE
	            	{
	            		R = 0;
	            		G = 255;
	            		B = 255;
	            		break;
	            	}
	            	case 5: //DARK BLUE
	            	{
	            		R = 0;
	            		G = 0;
	            		B = 255;
	            		break;
	            	}
	            	case 6: //VIOLET
	            	{
	            		R = 100;
	            		G = 0;
	            		B = 255;
	            		break;
	            	}
	            	case 7: //PURPLE
	            	{
	            		R = 175;
	            		G = 0;
	            		B = 255;
	            		break;
	            	}
	        		default:
	        		{
	            		R = 0;
	            		G = 0;
	            		B = 0;
	            		break;
	            	}
            	}
            	
//            	if(frameIndex > 1)
//            	{
//            		R = 0;
//            		G = 0;
//            		B = 0;
//            	}
//            	else
//            	{
//            		R = 255;
//            		G = 255;
//            		B = 255;
//            	}
				 // well known RGB to YUV algorithm
		        Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
		        U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
		        V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;
		        
		        mFocusAreasMapImage[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
		        if (j % 2 == 0 && index % 2 == 0)
		        { 
		        	mFocusAreasMapImage[uvIndex++] = (byte)((V<0) ? 0 : ((V > 255) ? 255 : V));
		        	mFocusAreasMapImage[uvIndex++] = (byte)((U<0) ? 0 : ((U > 255) ? 255 : U));
		        }

		        index++;
            }
		}
		
//		this.mFocusAreasMapAddress = SwapHeap.SwapToHeap(mFocusAreasMapImage);
		
		//Saving
//		int yuv = SwapHeap.SwapToHeap(mFocusAreasMapImage);
//		String fileFormat = PluginManager.getInstance().getFileFormat();
//		String evmark = String.format("_MAP");		
//		PluginManager.getInstance().saveFocusAreasFile(yuv, fileFormat + evmark, sx, sy);
		
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
