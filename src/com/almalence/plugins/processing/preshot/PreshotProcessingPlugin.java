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

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.almalence.SwapHeap;

/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.PluginProcessing;
import com.almalence.opencam_plus.R;
+++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginProcessing;
import com.almalence.opencam.R;
//-+- -->

import com.almalence.plugins.capture.preshot.PreShot;

/***
Implements back in time prcessing plugin. Prepares images for displaying etc.
***/

public class PreshotProcessingPlugin extends PluginProcessing implements OnTouchListener, OnClickListener
{
	public PreshotProcessingPlugin()
	{
		super("com.almalence.plugins.preshotprocessing",
			  R.xml.preferences_processing_preshot,
			  0,
			  0,
			  null);
		
		this.mSavingDialog = new ProgressDialog(MainScreen.thiz);
    	this.mSavingDialog.setIndeterminate(true);
    	this.mSavingDialog.setCancelable(false);
    	this.mSavingDialog.setMessage("Saving");
	}
	
	private static int idx=0;
	private static int imgCnt=0;
	
	static public String[] filesSavedNames;
	static public int nFilesSaved;
	
	private Bitmap[] mini_frames;
	private AtomicBoolean miniframesReady = new AtomicBoolean(false);

	private Button mSaveButton;
	private Button mSaveAllButton;
	
	long ProcTimeSt;
	//Thread saving;
	boolean should_save= false;
	boolean isSaveAll = false;
	
	//private View buttonsPanel = null;
	
	private int button_side;
	
	DisplayMetrics metrics = null;
	
	private int mLayoutOrientationCurrent;
	private int mDisplayOrientationCurrent;
	private int mDisplayOrientationOnStartProcessing;	
	private boolean mCameraMirrored = false;
	
	boolean isResultFromProcessingPlugin = false;
	
	private boolean postProcessingRun = false;
	
    public static boolean AccelerometerPreference;
    
    private boolean isSlowMode = false;
    
    private long sessionID=0;
    
    private ProgressDialog mSavingDialog;
    
    private static final int SAVE_DIALOG_SHOW = 0;
    private static final int SAVE_DIALOG_HIDE = 1;
    
 // indicates if it's first launch - to show hint layer.
 	private boolean isFirstLaunch = true;
    
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SAVE_DIALOG_SHOW:
                	mSavingDialog.show();
                break;
        
                case SAVE_DIALOG_HIDE:
                	mSavingDialog.hide();
                break;
        
            }
        }
    };

    @Override
    public void onStartProcessing(long SessionID)
	{
    	Message msg = new Message();
		msg.what = PluginManager.MSG_PROCESSING_BLOCK_UI;
		MainScreen.H.sendMessage(msg);	
		
		Message msg2 = new Message();
		msg2.arg1 = PluginManager.MSG_CONTROL_LOCKED;
		msg2.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg2);
		
		MainScreen.guiManager.lockControls = true;
		
		mDisplayOrientationOnStartProcessing = MainScreen.guiManager.getDisplayOrientation();
    	mDisplayOrientationCurrent = MainScreen.guiManager.getDisplayOrientation();
    	int orientation = MainScreen.guiManager.getLayoutOrientation();
    	mLayoutOrientationCurrent = orientation == 0 || orientation == 180? orientation: (orientation + 180)%360;
    	mCameraMirrored = MainScreen.getCameraMirrored();
		
    	sessionID=SessionID;
    	
    	PluginManager.getInstance().addToSharedMem("modeSaveName"+Long.toString(sessionID), PluginManager.getInstance().getActiveMode().modeSaveName);
    	
    	PluginManager.getInstance().addToSharedMem("modeSaveName"+Long.toString(sessionID), PluginManager.getInstance().getActiveMode().modeSaveName);
    	
    	if (0 == PreShot.MakeCopy())
    	{
    		Log.v("Preshot processing", "Preshot processing make copy faied.");
    		return;
    	}

    	isSlowMode = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("isslowmodeenabled"+Long.toString(sessionID)));
    	//processing only in fast mode.
//    	if(!isSlowMode)
    		ProcessingImages();
    	imgCnt = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("amountofcapturedframes"+Long.toString(sessionID)));
    	PluginManager.getInstance().addToSharedMem("amountofresultframes"+Long.toString(sessionID), String.valueOf(imgCnt));
   		PluginManager.getInstance().addToSharedMem("ResultFromProcessingPlugin"+Long.toString(sessionID), isSlowMode? "true" : "false");
//		PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(sessionID), String.valueOf(MainScreen.getSaveImageHeight()));
//    	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(sessionID), String.valueOf(MainScreen.getSaveImageWidth()));
   		
   		prepareMiniFrames();
   		miniframesReady.set(true);
	}

    private Integer ProcessingImages()
    {
    	isSlowMode = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("IsSlowMode"+Long.toString(sessionID)));
		int imagesAmount = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("amountofcapturedframes"+Long.toString(sessionID)));
		
		int iSaveImageWidth = MainScreen.getSaveImageWidth();
		int iSaveImageHeight = MainScreen.getSaveImageHeight();
		
		if (imagesAmount==0)
			imagesAmount=1;
		
		for (int i=0; i<imagesAmount; i++)
		{
	    	if(isSlowMode == false)
	    	{
		    	byte[] data = null;
		    	
		    	if (mCameraMirrored)
		    	{
		    		data = PreShot.GetFromBufferReservedNV21(i, 0, 0, 1);
		    	}
		    	else
		    		data = PreShot.GetFromBufferReservedNV21(i, 0, 0, 0);
		    	
		    	if (data.length ==0)
		    	{
		    		return null;
		    	}
				
		    	int frame = SwapHeap.SwapToHeap(data);
		    	
		    	PluginManager.getInstance().addToSharedMem("resultframe"+(i+1)+Long.toString(sessionID), String.valueOf(frame));
		    	PluginManager.getInstance().addToSharedMem("resultframelen"+(i+1)+Long.toString(sessionID), String.valueOf(data.length));
		    	
	    	}
	    	else if(isSlowMode == true)
		    {
		    	byte[] data = PreShot.GetFromBufferSimpleReservedNV21(i, MainScreen.getImageHeight(), MainScreen.getImageWidth());
		    	
		    	if (data.length ==0)
		    	{
		    		return null;
		    	}
		    	
		    	int frame = SwapHeap.SwapToHeap(data);
		    	
		    	PluginManager.getInstance().addToSharedMem("resultframe"+(i+1)+Long.toString(sessionID), String.valueOf(frame));
		    	PluginManager.getInstance().addToSharedMem("resultframelen"+(i+1)+Long.toString(sessionID), String.valueOf(data.length));
		    	PluginManager.getInstance().addToSharedMem("resultframeformat"+(i+1)+Long.toString(sessionID), "jpeg");
	    	}
	    	
//	    	int isPortrait = PreShot.isPortraitReserved(i);
	    	int iOrientation = PreShot.getOrientationReserved(i);
	    	if(isSlowMode == true)
	    		PluginManager.getInstance().addToSharedMem("resultframeorientation" + (i+1) + String.valueOf(sessionID), String.valueOf((iOrientation)));
	    	else
	    		PluginManager.getInstance().addToSharedMem("resultframeorientation" + (i+1) + String.valueOf(sessionID), String.valueOf((0)));
	    	if(iOrientation == 90 || iOrientation == 270)
	    	{
		    	PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(sessionID), String.valueOf(iSaveImageHeight));
		    	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(sessionID), String.valueOf(iSaveImageWidth));
	    	}
	    	else
	    	{
	    		PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(sessionID), String.valueOf(iSaveImageWidth));
		    	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(sessionID), String.valueOf(iSaveImageHeight));
	    	}

			PluginManager.getInstance().addToSharedMem("resultframemirrored" + (i+1) + String.valueOf(sessionID), String.valueOf(mCameraMirrored));
		}
		
		PluginManager.getInstance().addToSharedMem("amountofresultframes"+Long.toString(sessionID), String.valueOf(imagesAmount));
		return null;
	}
    
    @Override
    public void FreeMemory()
    {
    	PreShot.FreeBufferReserved();
    }
    
	public int getMultishotImageCount()
	{
		return Integer.parseInt(PluginManager.getInstance().getFromSharedMem("amountofcapturedframes"+Long.toString(sessionID)));
	}

	@Override
	public boolean isPostProcessingNeeded(){return true;}

	@Override
	public void onStartPostProcessing()
	{
		//LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();		
		postProcessingView = LayoutInflater.from(MainScreen.mainContext).inflate(R.layout.plugin_processing_preshot_postprocessing_layout, null);			
				
		idx=0;
    	imgCnt=0;
		
    	setupSaveButton();
	    
//	    new OrientationEventListener (MainScreen.mainContext)
//        {
//        	private int mDeviceOrientation;
//
//			@Override
//            public void onOrientationChanged(int orientation)
//            {
//                if (orientation == ORIENTATION_UNKNOWN || !postProcessingRun)
//    				return;
//                
//                final Display display = ((WindowManager)MainScreen.thiz.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
//                final int orientationProc = (display.getWidth() <= display.getHeight()) ? Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE;	
//                final int rotation = display.getRotation();
//    			
//                boolean remapOrientation = (orientationProc == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_0) ||
//        				(orientationProc == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_180) ||
//        				(orientationProc == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_90) ||
//        				(orientationProc == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_270);
//                
//        		if (remapOrientation)
//        			orientation = (orientation - 90 + 360) % 360;
//    			
//    			this.mDeviceOrientation = Util.roundOrientation(orientation, this.mDeviceOrientation);
//    			PreshotProcessingPlugin.this.mSaveButton.setOrientation(this.mDeviceOrientation);
//    			PreshotProcessingPlugin.this.mSaveAllButton.setOrientation(this.mDeviceOrientation);
//            }
//        };
        
        
        ((ImageView)postProcessingView.findViewById(R.id.imageHolder)).setOnTouchListener(this);
		        
		isResultFromProcessingPlugin = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("ResultFromProcessingPlugin" + sessionID));
		metrics = new DisplayMetrics();
		MainScreen.thiz.getWindowManager().getDefaultDisplay()
				.getMetrics(metrics);
		
		imgCnt = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("amountofcapturedframes"+Long.toString(sessionID)));
        if (0!=imgCnt)
        	idx = imgCnt-1;
        else
        	idx = 0;
        idx/=2;
        
        Show(true);
        
     // if first launch - show layout with hints
 		SharedPreferences prefs = PreferenceManager
 				.getDefaultSharedPreferences(MainScreen.mainContext);
 		if (true == prefs.contains("isFirstPreShotLaunch")) {
 			isFirstLaunch = prefs.getBoolean("isFirstPreShotLaunch", true);
 		} else {
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
	
	public void setupSaveButton() {
    	// put save button on screen
        mSaveButton = new Button(MainScreen.thiz);
        mSaveButton.setBackgroundResource(R.drawable.plugin_processing_preshot_savethis_background);
        mSaveButton.setOnClickListener(this);
        LayoutParams saveLayoutParams = new LayoutParams(
        		(int) (MainScreen.mainContext.getResources().getDimension(R.dimen.postprocessing_savebutton_size)), 
        		(int) (MainScreen.mainContext.getResources().getDimension(R.dimen.postprocessing_savebutton_size)));
        saveLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        saveLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        saveLayoutParams.setMargins(
        		(int)(MainScreen.thiz.getResources().getDisplayMetrics().density * 8), 
        		(int)(MainScreen.thiz.getResources().getDisplayMetrics().density * 8), 
        		0, 
        		0);
		((RelativeLayout)postProcessingView.findViewById(R.id.preshot_processingLayout2)).addView(mSaveButton, saveLayoutParams);
		mSaveButton.setRotation(mLayoutOrientationCurrent);
		mSaveButton.invalidate();
		
		// put save button on screen
        mSaveAllButton = new Button(MainScreen.thiz);
        mSaveAllButton.setBackgroundResource(R.drawable.plugin_processing_preshot_saveall_background);
        mSaveAllButton.setOnClickListener(this);
        LayoutParams saveLayoutParams2 = new LayoutParams(
        		(int) (MainScreen.mainContext.getResources().getDimension(R.dimen.postprocessing_savebutton_size)), 
        		(int) (MainScreen.mainContext.getResources().getDimension(R.dimen.postprocessing_savebutton_size)));
        saveLayoutParams2.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        saveLayoutParams2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        saveLayoutParams2.setMargins(
        		(int)(MainScreen.thiz.getResources().getDisplayMetrics().density * 8), 
        		(int)(MainScreen.thiz.getResources().getDisplayMetrics().density * 8), 
        		0, 
        		0);
		((RelativeLayout)postProcessingView.findViewById(R.id.preshot_processingLayout2)).addView(mSaveAllButton, saveLayoutParams2);
		mSaveAllButton.setRotation(mLayoutOrientationCurrent);
		mSaveAllButton.invalidate();
    }
	
	public void saveTask()
	{
		//mSavingDialog.show();
//		mHandler.sendEmptyMessage(SAVE_DIALOG_SHOW);
		
		if(isSaveAll)
			saveAll();
		else
			saveThis();
		
//		mHandler.sendEmptyMessage(SAVE_DIALOG_HIDE);
		
		Message msg2 = new Message();
		msg2.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
		msg2.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg2);
		
		MainScreen.guiManager.lockControls = false;
		
		postProcessingRun = false;
		
		MainScreen.H.sendEmptyMessage(PluginManager.MSG_POSTPROCESSING_FINISHED);
	}
	
	@Override
	public void onClick(View v) 
	{
		if(postProcessingView != null && postProcessingView.findViewById(R.id.preShotHintLayout).getVisibility() == View.VISIBLE)
    		postProcessingView.findViewById(R.id.preShotHintLayout).setVisibility(View.GONE);
		
    	if (v == mSaveButton)
    	{	
			isSaveAll = false;		    		
			saveTask();							
    	}
    	else if (v == mSaveAllButton)
    	{	
			isSaveAll = true;		    		
			saveTask();							
    	}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(postProcessingView != null && postProcessingView.findViewById(R.id.preShotHintLayout).getVisibility() == View.VISIBLE)
    		postProcessingView.findViewById(R.id.preShotHintLayout).setVisibility(View.GONE);
		
		if (keyCode == KeyEvent.KEYCODE_BACK && MainScreen.thiz.findViewById(R.id.postprocessingLayout).getVisibility() == View.VISIBLE)
		{
//			if (saving.isAlive())
//    			saving.interrupt();
    		
    		Message msg2 = new Message();
    		msg2.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
    		msg2.what = PluginManager.MSG_BROADCAST;
    		MainScreen.H.sendMessage(msg2);
    		
    		MainScreen.guiManager.lockControls = false;
    		
    		postProcessingRun = false;
    		
    		MainScreen.H.sendEmptyMessage(PluginManager.MSG_POSTPROCESSING_FINISHED);
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
    private void prepareMiniFrames()
    {
    	this.mini_frames = new Bitmap[imgCnt];     	
    	
        for (int i = 0; i < imgCnt; i++)
        	this.mini_frames[i] = getMultishotBitmap(i);
    }
    
    @Override
    public void onOrientationChanged(int orientation)
    {	    	
    	if(orientation != mDisplayOrientationCurrent)
    	{
    		mLayoutOrientationCurrent = (orientation == 0 || orientation == 180) ? orientation + 90 : orientation - 90;
    		mDisplayOrientationCurrent = orientation;
    		if(postProcessingRun)
    		{
    			Log.e("PreSho Post processing", "orientation = " + orientation + " mLayoutOrientationCurrent =  " + mLayoutOrientationCurrent);
    			mSaveButton.setRotation(mLayoutOrientationCurrent); 
    			mSaveButton.invalidate();
    			mSaveAllButton.setRotation(mLayoutOrientationCurrent);
    			mSaveAllButton.invalidate();
    			Show(false);
    		}
//	    	new Thread(new Runnable() {
//                public void run() {
//                	mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_VISIBLE);
//                	updateBitmap();
//                    mHandler.post(new Runnable() {
//                        public void run() {
//                        	if (PreviewBmp != null) {
//                        		mImgView.setImageBitmap(PreviewBmp);
//                        		mSaveButton.setRotation(mDisplayOrientationCurrent? 90 : 0);
//                        		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mGallery.getLayoutParams();
//                        		int[] rules = lp.getRules();
//                        		if(mDisplayOrientationCurrent)
//                        		{
//                        			lp.addRule(RelativeLayout.CENTER_VERTICAL);
//                        			lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
//
//                        			mGallery.setRotation(90);
//                        			mGallery.setPivotX(mGallery.getHeight());
//                        		}
//                        		else
//                        		{
//                        			rules[RelativeLayout.ALIGN_PARENT_BOTTOM] = 1;
//                        			rules[RelativeLayout.CENTER_VERTICAL] = 0;
//                        			rules[RelativeLayout.ALIGN_PARENT_LEFT] = 0;	                        			
//                        			
//                        			mGallery.setLayoutParams(lp);
//                        			mGallery.requestLayout();
//                        			
//                        			mGallery.setRotation(0);
//                        		}
//                        	}
//                        }
//                    });
//                    
//                    // Probably this should be called from mSeamless ? 
//                    // mSeamless.fillLayoutwithStitchingflag(mFaceRect);
//                    mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_INVISIBLE);
//                }
//            }).start();
    	}
    }
    
    private void Show(boolean initial)
    {    	
    	if (idx < 0)
    		idx = 0;
    	else if (idx >= imgCnt)
    		idx = imgCnt - 1;
    	if (imgCnt==1 || mini_frames.length==1)
    		idx = 0;
    	
    	Bitmap photo = mini_frames[idx];//getMultishotBitmap(idx);
    	//Log.e("MultishotExportPlugin", "Show getMultishotBitmap success");
    	if(photo != null)
    	{    		
    		if(initial)
    		{
	    		if(mDisplayOrientationCurrent == 0 || mDisplayOrientationCurrent == 180) //Device in landscape
				{
					Matrix matrix = new Matrix();
	    		
					matrix.postRotate(90);
					photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
				}
    		}
    		else
    		{
    			boolean isGuffyOrientation = mDisplayOrientationOnStartProcessing == 180 || mDisplayOrientationOnStartProcessing == 270;
    			    			
    			Matrix matrix = new Matrix();
	    		
				matrix.postRotate(isGuffyOrientation? (mLayoutOrientationCurrent + 180)%360 : mLayoutOrientationCurrent);
				photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
    		}
    		
    		((ImageView)postProcessingView.findViewById(R.id.imageHolder)).setImageBitmap(photo);
    		String txt = idx+1 + " of " + imgCnt;
	        ((TextView)postProcessingView.findViewById(R.id.preshot_image_counter)).setText(txt);
	        postProcessingView.findViewById(R.id.preshot_image_counter).setRotation(mLayoutOrientationCurrent);
	        
	        PluginManager.getInstance().addToSharedMem("previewresultframeindex", String.valueOf(idx+1));
    	}
    }
    
    public Bitmap getMultishotBitmap(int index)
	{
    	if(isSlowMode == false)
    	{
	    	int[] data = PreShot.GetFromBufferRGBA(index, false, false);
	    	
	    	if (data.length == 0)
	    	{
				//Toast.makeText(MainScreen.thiz, "No images", Toast.LENGTH_SHORT).show();	
	    		return null;
	    	}
	
	    	int H = MainScreen.previewHeight, W = MainScreen.previewWidth;
	    	//if (1 == PreShot.isPortrait(index))// && !mCameraMirrored)
	    	int or = PreShot.getOrientation(index);
	    	Log.e("PreShot", "getMultishotBitmap orientation: " + or);
	    	if (90 == PreShot.getOrientation(index) || 270 == PreShot.getOrientation(index))
	    	{ 
	    		H = MainScreen.previewWidth;
	    		W = MainScreen.previewHeight;
	    	}
			
			Bitmap bitmap;
			bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
			bitmap.setPixels(data, 0, W, 0, 0, W, H);
			
			if (mCameraMirrored && (90 == PreShot.getOrientation(index) || 270 == PreShot.getOrientation(index))/*1 == PreShot.isPortrait(index)*/)
			{
				Matrix matrix = new Matrix();
				matrix.postRotate(180);
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
			}
			
			return bitmap;
	    }
    	else
    	{//slow mode
    		//Log.e("PreshotProcessingPlugin", "getMultishotBitmap slowMode. START");
    		byte[] data = PreShot.GetFromBufferToShowInSlow(index, MainScreen.previewHeight, MainScreen.previewWidth, MainScreen.getCameraMirrored());
    		//Log.e("PreshotProcessingPlugin", "getMultishotBitmap slowMode. Get from Heap success");
	    	
	    	if (data.length == 0)
	    	{
	    		//Toast.makeText(MainScreen.thiz, "No images", Toast.LENGTH_SHORT).show();	
	    		return null;
	    	}
	
	    	//int H_Source = MainScreen.getImageHeight(), W_Source = MainScreen.getImageWidth();
	    	int H = MainScreen.previewHeight, W = MainScreen.previewWidth;
    		
	        Bitmap photo = null ;
	        //photo = Bitmap.createBitmap(data, W, H, Bitmap.Config.ARGB_8888);
	        photo = BitmapFactory.decodeByteArray(data, 0, data.length);
	        //photo = Bitmap.createBitmap(data, W_Source, H_Source, Bitmap.Config.ARGB_8888);
	        photo = Bitmap.createScaledBitmap(photo, W, H, false);
	        
	        //if(1 == PreShot.isPortrait(index))
	        if(90 == PreShot.getOrientation(index) || 270 == PreShot.getOrientation(index))
			{
				Matrix matrix = new Matrix();
				matrix.postRotate(mCameraMirrored? 270 : 90);
	    		photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
			}
	        
	        //Log.e("PreshotProcessingPlugin", "getMultishotBitmap slowMode. Bitmap created!");

	        return photo;
    	}
	}

    static boolean isFlipping = false;
    private void flipPhoto(boolean toLeft, float XtoVisible)
	{
    	isFlipping = true;
    	ImageView imgView = (ImageView)MainScreen.thiz.findViewById(R.id.imageHolder);
		int screenWidth = imgView.getWidth();
		int screenHeight = imgView.getHeight();
		
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
		
		AnimationSet rlvisible = new AnimationSet(true);
		rlvisible.setInterpolator(new DecelerateInterpolator());
		
		AnimationSet lrvisible = new AnimationSet(true);
		lrvisible.setInterpolator(new DecelerateInterpolator());
		
		int duration_visible = 0;
		
		if(mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
			duration_visible = com.almalence.util.Util.clamp(Math.abs(Math.round((XtoVisible*500)/actH)), 250, 500);
		else
			duration_visible = com.almalence.util.Util.clamp(Math.abs(Math.round((XtoVisible*500)/actW)), 250, 500);
		
		Animation visible_alpha = new AlphaAnimation(0, 1);		
		visible_alpha.setDuration(duration_visible);
		visible_alpha.setRepeatCount(0);
		
		Animation rlvisible_translate;
		if(mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
			rlvisible_translate = new TranslateAnimation(0, 0 , XtoVisible, 0);
		else
			rlvisible_translate = new TranslateAnimation(XtoVisible, 0 , 0, 0);
		rlvisible_translate.setDuration(duration_visible);		
		rlvisible_translate.setFillAfter(true);
		
		Animation lrvisible_translate;
		if(mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
			lrvisible_translate = new TranslateAnimation(0, 0 , XtoVisible, 0);
		else
			lrvisible_translate = new TranslateAnimation(XtoVisible, 0 , 0, 0);
		lrvisible_translate.setDuration(duration_visible);		
		lrvisible_translate.setFillAfter(true);
		
		//rlvisible.addAnimation(visible_alpha);
		rlvisible.addAnimation(rlvisible_translate);
		
		//lrvisible.addAnimation(visible_alpha);
		lrvisible.addAnimation(lrvisible_translate);
		
		
		
		postProcessingView.findViewById(R.id.imageListed).startAnimation(toLeft?rlvisible:lrvisible);
		
		rlvisible.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				postProcessingView.findViewById(R.id.imageListed).clearAnimation();
				postProcessingView.findViewById(R.id.imageListed).setVisibility(View.GONE);
				Show(false);
				isFlipping = false;
			}
			@Override
			public void onAnimationRepeat(Animation animation) {}
			@Override
			public void onAnimationStart(Animation animation) {}
		});
		
		lrvisible.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				postProcessingView.findViewById(R.id.imageListed).clearAnimation();
				postProcessingView.findViewById(R.id.imageListed).setVisibility(View.GONE);
				Show(false);
				isFlipping = false;
			}
			@Override
			public void onAnimationRepeat(Animation animation) {}
			@Override
			public void onAnimationStart(Animation animation) {}
		});
	}
    
    private static float X=0;
    
	private static float Xprev=0;
	
	private static float Xoffset = 0;
	
	private static float XtoLeftVisible = 0;
	
	private static float XtoRightVisible = 0;
	
    public boolean onTouch(View v, MotionEvent event) 
    {
//    	if (saving.isAlive())
//			saving.interrupt();
    	if(postProcessingView != null && postProcessingView.findViewById(R.id.preShotHintLayout).getVisibility() == View.VISIBLE)
    		postProcessingView.findViewById(R.id.preShotHintLayout).setVisibility(View.GONE);
    	
    	if(isFlipping)
    		return true;
    	
    	boolean isGuffyOrientation = (mDisplayOrientationOnStartProcessing == 180 || mDisplayOrientationOnStartProcessing == 270); 
    	
    	Bitmap photo = null;
    	
        switch (event.getAction())
        {
			case MotionEvent.ACTION_DOWN:
			{
				if(mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
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
				if(mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
					difX = event.getY();
				else
					difX = event.getX();
				
				if ((X>difX) && (X -difX > 100))
				{					
					int new_idx = isGuffyOrientation? --idx : ++idx;
					//++idx;
					if(new_idx <= imgCnt - 1 && new_idx >= 0)
						flipPhoto(true, XtoLeftVisible);
					else
						Show(false);
				}
				else if(X<difX && (difX - X > 100))
				{
					int new_idx = isGuffyOrientation? ++idx : --idx;
					//--idx;
					if(new_idx >= 0 && new_idx <= imgCnt - 1)
						flipPhoto(false, XtoRightVisible);
					else
						Show(false);
				}
				break;
			}
			case MotionEvent.ACTION_MOVE:
			{
				float difX = 0;
				if(mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
					difX = event.getY();
				else
					difX = event.getX();
				
				if((X>difX && isGuffyOrientation? idx == 0 : idx == imgCnt - 1) || (X<difX && isGuffyOrientation? idx == imgCnt - 1 : idx == 0))
					break;

				ImageView imgView = (ImageView)MainScreen.thiz.findViewById(R.id.imageHolder);
				
				int screenWidth = imgView.getWidth();
				int screenHeight = imgView.getHeight();
				
				
				float[] f = new float[9];
		        imgView.getImageMatrix().getValues(f);

		        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
		        final float scaleX = f[Matrix.MSCALE_X];
		        final float scaleY = f[Matrix.MSCALE_Y];

		        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
		        final Drawable d = imgView.getDrawable();
		        if (d == null)
		        	break;
		        final int origW = d.getIntrinsicWidth();
		        final int origH = d.getIntrinsicHeight();

		        // Calculate the actual dimensions
		        final int actW = Math.round(origW * scaleX);
		        final int actH = Math.round(origH * scaleY);
		        
		        final int startW = Math.round(actW + (screenWidth-actW)/2);
		        final int startH = Math.round(actH + (screenHeight-actH)/2);
		        
				Animation in_animation;
				Animation out_animation;
				Animation reverseout_animation;
				//Animation in_scale_animation;
				//Animation reverseout_scale_animation;
				
				AnimationSet in_animation_set = new AnimationSet(true);
				in_animation_set.setInterpolator(new DecelerateInterpolator());
				in_animation_set.setFillAfter(true);
				
				AnimationSet reverseout_animation_set = new AnimationSet(true);
				reverseout_animation_set.setInterpolator(new DecelerateInterpolator());
				reverseout_animation_set.setFillAfter(true);
				
				boolean toLeft;
				if(difX > Xprev)
				{
					if(mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
						out_animation = new TranslateAnimation(0, 0, Xprev - Xoffset, difX - Xoffset);
					else
						out_animation = new TranslateAnimation(Xprev - Xoffset, difX - Xoffset, 0, 0);
					out_animation.setDuration(10);
					out_animation.setInterpolator(new LinearInterpolator());
					out_animation.setFillAfter(true);
					
					if(mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
						in_animation = new TranslateAnimation(0, 0, Xprev - Xoffset - startH, difX - Xoffset - startH);
					else
						in_animation = new TranslateAnimation(Xprev - Xoffset - actW, difX - Xoffset - actW, 0, 0);
					in_animation.setDuration(10);
					in_animation.setInterpolator(new LinearInterpolator());
					in_animation.setFillAfter(true);
					
					if(mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
						reverseout_animation = new TranslateAnimation(0, 0, difX + (startH - Xoffset), Xprev + (startH - Xoffset));
					else						
						reverseout_animation = new TranslateAnimation(difX + (actW - Xoffset), Xprev + (actW - Xoffset), 0, 0);
					reverseout_animation.setDuration(10);
					reverseout_animation.setInterpolator(new LinearInterpolator());
					reverseout_animation.setFillAfter(true);
					
					float scale_from = Math.abs(Xprev - X)/-500 + 2;
					float scale_to = Math.abs(difX - X)/-500 + 2;
					
					scale_from = com.almalence.util.Util.clamp(scale_from, 1, 2);
					scale_to = com.almalence.util.Util.clamp(scale_to, 1, 2);
					
//					in_scale_animation = new ScaleAnimation(scale_from, scale_to, scale_from, scale_to, Animation.RELATIVE_TO_SELF, (float)0.5, Animation.RELATIVE_TO_SELF, (float)0.5);
//					in_scale_animation.setDuration(10);
//					in_scale_animation.setInterpolator(new LinearInterpolator());
//					in_scale_animation.setFillAfter(true);
//					
//					reverseout_scale_animation = new ScaleAnimation(scale_to, scale_from, scale_to, scale_from, Animation.RELATIVE_TO_SELF, (float)0.5, Animation.RELATIVE_TO_SELF, (float)0.5);
//					reverseout_scale_animation.setDuration(10);
//					reverseout_scale_animation.setInterpolator(new LinearInterpolator());
//					reverseout_scale_animation.setFillAfter(true);
					
					in_animation_set.addAnimation(in_animation);
//					in_animation_set.addAnimation(in_scale_animation);
					
					reverseout_animation_set.addAnimation(reverseout_animation);
//					reverseout_animation_set.addAnimation(reverseout_scale_animation);
					
					toLeft = false;
					
					XtoRightVisible = difX - Xoffset - screenWidth;
				}
				else
				{
					if(mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
						out_animation = new TranslateAnimation(0, 0, difX - Xoffset, Xprev - Xoffset);
					else
						out_animation = new TranslateAnimation(difX - Xoffset, Xprev - Xoffset, 0, 0);
					out_animation.setDuration(10);
					out_animation.setInterpolator(new LinearInterpolator());
					out_animation.setFillAfter(true);
					
					if(mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
						in_animation = new TranslateAnimation(0, 0, startH + (Xprev - Xoffset), startH + (difX - Xoffset));
					else
						in_animation = new TranslateAnimation(actW + (Xprev - Xoffset), actW + (difX - Xoffset), 0, 0);
					in_animation.setDuration(10);
					in_animation.setInterpolator(new LinearInterpolator());
					in_animation.setFillAfter(true);
					
					if(mLayoutOrientationCurrent == 90 || mLayoutOrientationCurrent == 270)
						reverseout_animation = new TranslateAnimation(0, 0, Xprev - Xoffset - startH, difX - Xoffset - startH);
					else
						reverseout_animation = new TranslateAnimation(Xprev - Xoffset - actW, difX - Xoffset - actW, 0, 0);
					reverseout_animation.setDuration(10);
					reverseout_animation.setInterpolator(new LinearInterpolator());
					reverseout_animation.setFillAfter(true);
					
					float scale_from = Math.abs(X - Xprev)/-500 + 2;
					float scale_to = Math.abs(X - difX)/-500 + 2;
					
					scale_from = com.almalence.util.Util.clamp(scale_from, 1, 2);
					scale_to = com.almalence.util.Util.clamp(scale_to, 1, 2);
					
//					in_scale_animation = new ScaleAnimation(scale_from, scale_to, scale_from, scale_to, Animation.RELATIVE_TO_SELF, (float)0.5, Animation.RELATIVE_TO_SELF, (float)0.5);
//					in_scale_animation.setDuration(10);
//					in_scale_animation.setInterpolator(new LinearInterpolator());
//					in_scale_animation.setFillAfter(true);
					
//					reverseout_scale_animation = new ScaleAnimation(scale_to, scale_from, scale_to, scale_from, Animation.RELATIVE_TO_SELF, (float)0.5, Animation.RELATIVE_TO_SELF, (float)0.5);
//					reverseout_scale_animation.setDuration(10);
//					reverseout_scale_animation.setInterpolator(new LinearInterpolator());
//					reverseout_scale_animation.setFillAfter(true);
					
					in_animation_set.addAnimation(in_animation);
//					in_animation_set.addAnimation(in_scale_animation);
					
					reverseout_animation_set.addAnimation(reverseout_animation);
//					reverseout_animation_set.addAnimation(reverseout_scale_animation);
					
					toLeft = true;
					
					XtoLeftVisible = screenWidth + (difX - Xoffset);
				}
				
				if(difX < X && Xprev >= X)
				{
					int new_idx = isGuffyOrientation? idx-1 : idx+1;
					photo = mini_frames[new_idx];
			    	if(photo != null)
			    	{
			    		Matrix matrix = new Matrix();
			    		matrix.postRotate(isGuffyOrientation? (mLayoutOrientationCurrent + 180)%360 : mLayoutOrientationCurrent);
						//matrix.postRotate(isGuffyOrientation? (mLayoutOrientationCurrent)%360 : mLayoutOrientationCurrent);
			    		//matrix.postRotate(mLayoutOrientationCurrent);
						photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
			    		((ImageView)postProcessingView.findViewById(R.id.imageListed)).setImageBitmap(photo);
			    	}
				}
				else if(difX > X && Xprev <= X)
				{
					int new_idx = isGuffyOrientation? idx+1 : idx-1;
					photo = mini_frames[new_idx];
			    	if(photo != null)
			    	{
			    		Matrix matrix = new Matrix();
			    		
			    		matrix.postRotate(isGuffyOrientation? (mLayoutOrientationCurrent + 180)%360 : mLayoutOrientationCurrent);
						//matrix.postRotate(isGuffyOrientation? (mLayoutOrientationCurrent)%360 : mLayoutOrientationCurrent);
						//matrix.postRotate(mLayoutOrientationCurrent);
						photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
			    		((ImageView)postProcessingView.findViewById(R.id.imageListed)).setImageBitmap(photo);
			    	}
				}
				
				if((toLeft && difX < X) || (!toLeft && difX > X))
					postProcessingView.findViewById(R.id.imageListed).startAnimation(in_animation_set);				
				else
					postProcessingView.findViewById(R.id.imageListed).startAnimation(reverseout_animation_set);				
		    	
				if(postProcessingView.findViewById(R.id.imageListed).getVisibility() == View.GONE)
					postProcessingView.findViewById(R.id.imageListed).setVisibility(View.VISIBLE);
				
				Xprev = Math.round(difX);				
			}
				break;
        }
        return true;
    }
    
    private void saveAll() 
	{
    	PluginManager.getInstance().addToSharedMem("sessionID", String.valueOf(sessionID));		
	}
	
	private void saveThis() 
	{		
		int index = idx+1;
		
		PluginManager.getInstance().addToSharedMem("sessionID", String.valueOf(sessionID));
		PluginManager.getInstance().addToSharedMem("resultframeindex" + Long.toString(sessionID), String.valueOf(index));
	}
}
