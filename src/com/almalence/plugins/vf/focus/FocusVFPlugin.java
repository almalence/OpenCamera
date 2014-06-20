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

package com.almalence.plugins.vf.focus;

import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera.Area;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

/* <!-- +++
import com.almalence.opencam_plus.CameraController;
import com.almalence.opencam_plus.CameraParameters;
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.PluginViewfinder;
import com.almalence.opencam_plus.R;
import com.almalence.opencam_plus.SoundPlayer;
+++ --> */
// <!-- -+-
import com.almalence.opencam.CameraController;
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginViewfinder;
import com.almalence.opencam.SoundPlayer;
import com.almalence.opencam.R;
//-+- -->

import com.almalence.util.Util;

/***
Implements touch to focus functionality
***/

public class FocusVFPlugin extends PluginViewfinder
{
	private static final String TAG = "AlmalenceFocusPlugin";

    private static final int RESET_TOUCH_FOCUS = 0;
    private static final int START_TOUCH_FOCUS = 1;
    private static final int RESET_TOUCH_FOCUS_DELAY = 5000;
    private static final int START_TOUCH_FOCUS_DELAY = 1500;

    private int mState = STATE_INACTIVE;
    private static final int STATE_INACTIVE = -1;
    private static final int STATE_IDLE = 0; // Focus is not active.
    private static final int STATE_FOCUSING = 1; // Focus is in progress.
    // Focus is in progress and the camera should take a picture after focus finishes.
    private static final int STATE_FOCUSING_SNAP_ON_FINISH = 2;
    private static final int STATE_SUCCESS = 3; // Focus finishes and succeeds.
    private static final int STATE_FAIL = 4; // Focus finishes and fails.

    private boolean mFocusSupported = true;
    private boolean mInitialized;
    private boolean mAeAwbLock;
    private Matrix mMatrix;
    private SoundPlayer mSoundPlayerOK;
    private SoundPlayer mSoundPlayerFalse;
    private RelativeLayout focusLayout;
    private RotateLayout mFocusIndicatorRotateLayout;
    private FocusIndicatorView mFocusIndicator;    
    private int mPreviewWidth;
    private int mPreviewHeight;
    private List<Area> mFocusArea; // focus area in driver format
    private List<Area> mMeteringArea; // metering area in driver format
    private int mFocusMode;
    private int mDefaultFocusMode;
    private int mOverrideFocusMode;
    private SharedPreferences mPreferences;
    private Handler mHandler;
    
    //Camera capabilities  	
    private boolean mFocusAreaSupported = false;
    private boolean mMeteringAreaSupported = false;
 	
  	private boolean mFocusDisabled = false;

  	private int preferenceFocusMode;

    private class MainHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case RESET_TOUCH_FOCUS:                	
        			cancelAutoFocus();
                    int fm = CameraController.getInstance().getFocusMode();
                	if((preferenceFocusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE ||
    		  	    preferenceFocusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO) &&
    		  	    fm != -1 &&
    		  	    preferenceFocusMode != fm)
                      	{
                          	CameraController.getInstance().setCameraFocusMode(preferenceFocusMode);
                      	}
                    break;
                case START_TOUCH_FOCUS:
                {                	
	    			lastEvent.setAction(MotionEvent.ACTION_UP);
	        		delayedFocus = true;
	        		cancelAutoFocus();
	    			onTouchAreas(lastEvent);
	    			lastEvent.recycle();
                } break;                
            }
        }
    }

    public FocusVFPlugin()
	{
		super("com.almalence.plugins.focusvf",
				0,
				0,
				0,
				null);

		mHandler = new MainHandler();
        mMatrix = new Matrix();
	}
	
    @Override
	public void onCreate()
	{		
		View v = LayoutInflater.from(MainScreen.mainContext).inflate(R.layout.plugin_vf_focus_layout, null);		
		focusLayout = (RelativeLayout)v.findViewById(R.id.focus_layout);
		
		RelativeLayout.LayoutParams viewLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
		viewLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
		
		mFocusIndicatorRotateLayout = (RotateLayout) v.findViewById(R.id.focus_indicator_rotate_layout);
		
		mFocusIndicator = (FocusIndicatorView) mFocusIndicatorRotateLayout.findViewById(
               R.id.focus_indicator);

		resetTouchFocus();
		
		
		mPreferences = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		mDefaultFocusMode = CameraParameters.AF_MODE_AUTO;
	}
    
	
    @Override
	public void onStart()
	{
		mState = STATE_IDLE;
		updateFocusUI();
		
		CameraController.setFocusState(CameraController.FOCUS_STATE_IDLE);
	}
	
    @Override
	public void onStop()
	{
		cancelAutoFocus();
		mState = STATE_INACTIVE;
		updateFocusUI();
		
		CameraController.setFocusState(CameraController.FOCUS_STATE_IDLE);
		
		MainScreen.guiManager.removeViews(focusLayout, R.id.specialPluginsLayout);
	}
	
    @Override
	public void onGUICreate()
	{
    	MainScreen.guiManager.removeViews(focusLayout, R.id.specialPluginsLayout);
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);		
		params.addRule(RelativeLayout.CENTER_IN_PARENT);
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout)).addView(this.focusLayout, params);
		
		this.focusLayout.setLayoutParams(params);
		this.focusLayout.requestLayout();
	}
	
    @Override
	public void onResume()
	{
    	//ToDo: replace here with [CF] mode as default.
    	// Also, check if [CF] is available, and if not - set [AF], if [AF] is not available - set first available 
//    	preferenceFocusMode = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getInt(MainScreen.getCameraMirrored()? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, CameraParameters.AF_MODE_CONTINUOUS_PICTURE);
//    	cancelAutoFocus();
	}
	
    @Override
	public void onPause()
	{
    	PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putInt(MainScreen.getCameraMirrored()? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, preferenceFocusMode).commit();
		releaseSoundPlayer();
		removeMessages();		
	}	

    @Override
	public void onCameraParametersSetup()
	{
    	preferenceFocusMode = CameraController.getInstance().getFocusMode();//PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getString("FocusModeValue", Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

    	initializeParameters();		
		
		initialize(MainScreen.getCameraMirrored(), 90);
        initializeSoundPlayers(MainScreen.thiz.getResources().openRawResourceFd(R.raw.plugin_vf_focus_ok),
        					  MainScreen.thiz.getResources().openRawResourceFd(R.raw.plugin_vf_focus_false));
        
        cancelAutoFocus();
        
     // Set the length of focus indicator according to preview frame size.
        int len = Math.min(mPreviewWidth, mPreviewHeight) / 25;
        ViewGroup.LayoutParams layout = mFocusIndicator.getLayoutParams();
        layout.width = (int) (len * MainScreen.thiz.getResources().getInteger(R.integer.focusIndicator_cropFactor));
        layout.height = (int) (len * MainScreen.thiz.getResources().getInteger(R.integer.focusIndicator_cropFactor));
        mFocusIndicator.requestLayout();
	}


	/*
	 * 
	 * 
	 * FOCUS MANAGER`S FUNCTIONALITY
	 * 
	 * 
	 */
	// This has to be initialized before initialize().
    public void initializeParameters()
    {
        mFocusAreaSupported = (CameraController.maxRegionsSupported > 0
                && isSupported(CameraParameters.AF_MODE_AUTO,
                        CameraController.supportedFocusModes));
        mMeteringAreaSupported = CameraController.maxRegionsSupported > 0;
    }

    public void initialize(boolean mirror, int displayOrientation)
    {        
        mPreviewWidth = MainScreen.thiz.getPreviewWidth();
        mPreviewHeight = MainScreen.thiz.getPreviewHeight();
        
        Matrix matrix = new Matrix();
        Util.prepareMatrix(matrix, mirror, 90,
        		mPreviewWidth, mPreviewHeight);
        // In face detection, the matrix converts the driver coordinates to UI
        // coordinates. In tap focus, the inverted matrix converts the UI
        // coordinates to driver coordinates.
        matrix.invert(mMatrix);
        
        mInitialized = true;
    }
    
    @Override
    public void OnShutterClick()
    {
        if (needAutoFocusCall() && !focusOnShutterDisabled())
        { 
            if (mState == STATE_IDLE &&
            		!(preferenceFocusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE ||
            		preferenceFocusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO)
            		&& !MainScreen.getAutoFocusLock())
            {
            	if(preferenceFocusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE ||
            	           preferenceFocusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO)
            	        {
            	        	CameraController.getInstance().setCameraFocusMode(CameraParameters.AF_MODE_AUTO);
            	        }
            	setFocusParameters();
                autoFocus();
            }
            else if((mState == STATE_SUCCESS || mState == STATE_FAIL) &&
            		(preferenceFocusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE ||
            		preferenceFocusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO) &&
            		preferenceFocusMode != CameraController.getInstance().getFocusMode())
            {
            	// allow driver to choose whatever it wants for focusing / metering
                // without these two lines Continuous focus is not re-enabled on HTC One
            	int focusMode = getFocusMode();
            	if((focusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE ||
  				    focusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO ||
				    focusMode == CameraParameters.AF_MODE_AUTO ||
				    focusMode == CameraParameters.AF_MODE_MACRO) &&
        		    mFocusAreaSupported)
            	{
            		CameraController.getInstance().setCameraFocusAreas(null);
            	}
        		CameraController.getInstance().setCameraFocusMode(preferenceFocusMode);
            }
            else if(mState == STATE_FAIL)
            	MainScreen.guiManager.lockControls = false;
        }
    }
    
    @Override
    public void OnFocusButtonClick()
    {
    	OnShutterClick();
    }
    
    public void setFocusParameters()
	{        
        if (mFocusAreaSupported)
        	CameraController.getInstance().setCameraFocusAreas(getFocusAreas());

        if (mMeteringAreaSupported)
        {
            // Use the same area for focus and metering.
        	List<Area> area = getMeteringAreas();
        	if(area != null)
        		CameraController.getInstance().setCameraMeteringAreas(area);
        }
    }


    @Override
    public void onAutoFocus(boolean focused)
    {
        if (mState == STATE_FOCUSING_SNAP_ON_FINISH)
        {
            // Take the picture no matter focus succeeds or fails. No need
            // to play the AF sound if we're about to play the shutter
            // sound.
            if (focused)
            {
                mState = STATE_SUCCESS;                
            } 
            else
            {
                mState = STATE_FAIL;
                MainScreen.guiManager.lockControls = false;
            }
            updateFocusUI();            
        }
        else if (mState == STATE_FOCUSING)
        {
            // This happens when (1) user is half-pressing the focus key or
            // (2) touch focus is triggered. Play the focus tone. Do not
            // take the picture now.
            if (focused)
            {
                mState = STATE_SUCCESS;
                /* Note: we are always using full-focus scan, even in continuous modes
                
                // Do not play the sound in continuous autofocus mode. It does
                // not do a full scan. The focus callback arrives before doSnap
                // so the state is always STATE_FOCUSING.
                if (!Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(mFocusMode)
                        && mSoundPlayerOK != null)
                */              
                if (mSoundPlayerOK != null)                
                	if (!MainScreen.ShutterPreference
                			&& !PluginManager.getInstance().muteSounds())
                		mSoundPlayerOK.play();
                
                //With enabled preference 'Shot on tap' perform shutter button click after success focusing.
                String modeID = PluginManager.getInstance().getActiveMode().modeID;
                if(MainScreen.ShotOnTapPreference && !modeID.equals("video"))
                	MainScreen.guiManager.onHardwareShutterButtonPressed();
            }
            else
            {
            	if(mSoundPlayerFalse != null)
            		if (!MainScreen.ShutterPreference
                			&& !PluginManager.getInstance().muteSounds())
            			mSoundPlayerFalse.play();
                mState = STATE_FAIL;
            }
            updateFocusUI();
            mHandler.sendEmptyMessageDelayed(RESET_TOUCH_FOCUS, RESET_TOUCH_FOCUS_DELAY);
            // If this is triggered by touch focus, cancel focus after a
            // while.
        }
        else if (mState == STATE_IDLE)
        {
            // User has released the focus key before focus completes.
            // Do nothing.
        }        
    }
    
    private static float X = 0;
    private static float Y = 0;
    private static boolean focusCanceled = false;
    private static boolean delayedFocus = false;
    private static MotionEvent lastEvent = null;
    @Override
    public boolean onTouch(View view, MotionEvent e)
    {
    	//Not handle touch event if no need of autoFocus and refuse 'shot on tap' in video mode.
        if (!mInitialized || mState == STATE_FOCUSING_SNAP_ON_FINISH || mState == STATE_INACTIVE || mFocusDisabled || (!needAutoFocusCall() && !(MainScreen.ShotOnTapPreference &&  !PluginManager.getInstance().getActiveMode().modeID.equals("video")))) return false;

        // Let users be able to cancel previous touch focus.
        if ((mFocusArea != null) && (mState == STATE_FOCUSING) && !delayedFocus)
        {
        	focusCanceled = true;
        	cancelAutoFocus();
        	int fm = CameraController.getInstance().getFocusMode();
        	if((preferenceFocusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE ||
    		  	preferenceFocusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO) &&
        		fm != -1 &&
        		preferenceFocusMode != CameraController.getInstance().getFocusMode())
              	{
                  	CameraController.getInstance().setCameraFocusMode(preferenceFocusMode);
              	}
        	return true;
        }

        
        switch (e.getAction())
        {
        	case MotionEvent.ACTION_DOWN:
        		focusCanceled = false;
        		delayedFocus = false;
        		X = e.getX();
        		Y = e.getY();
        		
        		lastEvent = MotionEvent.obtain(e);
        		mHandler.sendEmptyMessageDelayed(START_TOUCH_FOCUS, START_TOUCH_FOCUS_DELAY);
        		
        		return true;
	        case MotionEvent.ACTION_MOVE:
	        {
	        	float difX = e.getX();
	        	float difY = e.getY();
	        	
				if((Math.abs(difX - X) > 50 || Math.abs(difY - Y) > 50) && !focusCanceled)
				{
					focusCanceled = true;
					cancelAutoFocus();
					mHandler.removeMessages(START_TOUCH_FOCUS);
					return true;
				}
				else
					return true;
	        }
	        case MotionEvent.ACTION_UP:
	        	mHandler.removeMessages(START_TOUCH_FOCUS);
	        	if(focusCanceled || delayedFocus)
	        		return true;
	        	break;
        }
        
		onTouchAreas(e);		

        return true;
    }
    
    public void onTouchAreas(MotionEvent e)
    {
    	// Initialize variables.
        int x = Math.round(e.getX());
        int y = Math.round(e.getY());
        int focusWidth = mFocusIndicatorRotateLayout.getWidth();
        int focusHeight = mFocusIndicatorRotateLayout.getHeight();
        int previewWidth = mPreviewWidth;
        int previewHeight = mPreviewHeight;
        int displayWidth = MainScreen.thiz.getResources().getDisplayMetrics().widthPixels;
        int diffWidth = displayWidth - previewWidth;        
        
        int paramsLayoutHeight = 0;
        
        int xOffset = (focusLayout.getWidth() - previewWidth)/2;
        int yOffset = (focusLayout.getHeight() - previewHeight)/2;
        
        if (mFocusArea == null) {
            mFocusArea = new ArrayList<Area>();
            mFocusArea.add(new Area(new Rect(), 1000));
            mMeteringArea = new ArrayList<Area>();
            mMeteringArea.add(new Area(new Rect(), 1000));
        }

        // Convert the coordinates to driver format.
        // AE area is bigger because exposure is sensitive and
        // easy to over- or underexposure if area is too small.
        calculateTapArea(focusWidth, focusHeight, 1f, x, y, MainScreen.thiz.preview.getWidth(), MainScreen.thiz.preview.getHeight(),
                mFocusArea.get(0).rect);
        if(MainScreen.currentMeteringMode != -1 && MainScreen.currentMeteringMode == CameraParameters.meteringModeSpot)
        	calculateTapArea(20, 20, 1f, x, y, MainScreen.thiz.preview.getWidth(), MainScreen.thiz.preview.getHeight(),
                    mMeteringArea.get(0).rect);
        else
        	mMeteringArea = null;
        	
        // Use margin to set the focus indicator to the touched area.
        RelativeLayout.LayoutParams p =
                (RelativeLayout.LayoutParams) mFocusIndicatorRotateLayout.getLayoutParams();
        int left = Util.clamp(x - focusWidth / 2 + xOffset, diffWidth/2, (previewWidth - focusWidth + xOffset*2) - diffWidth/2);
        int top = Util.clamp(y - focusHeight / 2 + yOffset, paramsLayoutHeight /2, (previewHeight - focusHeight + yOffset*2) - paramsLayoutHeight /2);
        p.setMargins(left, top, 0, 0);
        // Disable "center" rule because we no longer want to put it in the center.
        int[] rules = p.getRules();
        rules[RelativeLayout.CENTER_IN_PARENT] = 0;
        mFocusIndicatorRotateLayout.requestLayout();
        
        // Set the focus area and metering area.        
        if (mFocusAreaSupported && needAutoFocusCall() && (e.getAction() == MotionEvent.ACTION_UP)) {
        	if(preferenceFocusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE ||
        		  	preferenceFocusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO)
        	        {
        	        	CameraController.getInstance().setCameraFocusMode(CameraParameters.AF_MODE_AUTO);
        	        }
            setFocusParameters();
            autoFocus();
        } else if(e.getAction() == MotionEvent.ACTION_UP && MainScreen.ShotOnTapPreference && !PluginManager.getInstance().getActiveMode().modeID.equals("video"))
            	MainScreen.guiManager.onHardwareShutterButtonPressed();        
        else {  // Just show the indicator in all other cases.
            updateFocusUI();
            // Reset the metering area in 3 seconds.
            mHandler.removeMessages(RESET_TOUCH_FOCUS);
            mHandler.sendEmptyMessageDelayed(RESET_TOUCH_FOCUS, RESET_TOUCH_FOCUS_DELAY);
        }
    }

    public void onPreviewStarted()
    {
        mState = STATE_IDLE;
        CameraController.setFocusState(CameraController.FOCUS_STATE_IDLE);
    }

    public void onPreviewStopped()
    {
        mState = STATE_IDLE;
        resetTouchFocus();
        // If auto focus was in progress, it would have been canceled.
        updateFocusUI();
        
        CameraController.setFocusState(CameraController.FOCUS_STATE_IDLE);
    }

    public void onCameraReleased()
    {
        onPreviewStopped();
    }

    private void autoFocus()
    {
        Log.v(TAG, "Start autofocus.");
        if(CameraController.autoFocus())
        {
	        mState = STATE_FOCUSING;
	        updateFocusUI();
	        mHandler.removeMessages(RESET_TOUCH_FOCUS);
        }
    }

    private void cancelAutoFocus()
    {
        // Note: CameraController.getInstance().getFocusMode(); will return 'FOCUS_MODE_AUTO' if actual
        // mode is in fact FOCUS_MODE_CONTINUOUS_PICTURE or FOCUS_MODE_CONTINUOUS_VIDEO
        int fm = CameraController.getInstance().getFocusMode(); // preferenceFocusMode;
        if (fm != -1)
        {
        	if(fm == CameraParameters.AF_MODE_INFINITY)
        	{
        		String modeName = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getString("defaultModeName", null);
        		boolean isVideoRecording = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getBoolean("videorecording", false);
    			if(!(modeName != null && modeName.compareTo("video") == 0 && !isVideoRecording
    			   && (Build.MODEL.contains(MainScreen.deviceSS3_01) || Build.MODEL.contains(MainScreen.deviceSS3_02) ||
    	    				Build.MODEL.contains(MainScreen.deviceSS3_03) || Build.MODEL.contains(MainScreen.deviceSS3_04) ||
    	    				Build.MODEL.contains(MainScreen.deviceSS3_05) || Build.MODEL.contains(MainScreen.deviceSS3_06) ||
    	    				Build.MODEL.contains(MainScreen.deviceSS3_07) || Build.MODEL.contains(MainScreen.deviceSS3_08) ||
    	    				Build.MODEL.contains(MainScreen.deviceSS3_09) || Build.MODEL.contains(MainScreen.deviceSS3_10) ||
    	    				Build.MODEL.contains(MainScreen.deviceSS3_11) || Build.MODEL.contains(MainScreen.deviceSS3_12) ||	Build.MODEL.contains(MainScreen.deviceSS3_13))))
    				CameraController.cancelAutoFocus();
        	}
        	
        	if(fm != preferenceFocusMode)
            	CameraController.getInstance().setCameraFocusMode(preferenceFocusMode);
        }
        
        // Reset the tap area before calling mListener.cancelAutofocus.
        // Otherwise, focus mode stays at auto and the tap area passed to the
        // driver is not reset.
        resetTouchFocus();        
        
        mState = STATE_IDLE;
        CameraController.setFocusState(CameraController.FOCUS_STATE_IDLE);

        updateFocusUI();
        mHandler.removeMessages(RESET_TOUCH_FOCUS);
    }   
 
    public void initializeSoundPlayers(AssetFileDescriptor fd_ok, AssetFileDescriptor fd_false)
    {
        mSoundPlayerOK = new SoundPlayer(MainScreen.mainContext, fd_ok);
        mSoundPlayerFalse = new SoundPlayer(MainScreen.mainContext, fd_false);
    }

    public void releaseSoundPlayer()
    {
        if (mSoundPlayerOK != null)
        {
            mSoundPlayerOK.release();
            mSoundPlayerOK = null;
        }
        
        if (mSoundPlayerFalse != null)
        {
            mSoundPlayerFalse.release();
            mSoundPlayerFalse = null;
        }
    }


    // This can only be called after mParameters is initialized.
    public int getFocusMode()
    {
        if (mOverrideFocusMode != -1) return mOverrideFocusMode;

        if (mFocusAreaSupported && mFocusArea != null)
            mFocusMode = CameraParameters.AF_MODE_AUTO;
    	else
            mFocusMode = mPreferences.getInt(MainScreen.getCameraMirrored()? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, mDefaultFocusMode);
        
        if (!isSupported(mFocusMode, CameraController.supportedFocusModes))
        {
            // For some reasons, the driver does not support the current
            // focus mode. Fall back to auto.
            if (isSupported(CameraParameters.AF_MODE_AUTO, CameraController.supportedFocusModes))
            	mFocusMode = CameraParameters.AF_MODE_AUTO;
            else
                mFocusMode = CameraController.getInstance().getFocusMode();
        }
        return mFocusMode;
    }
    
    public void setFocusMode(int focus_mode)
    {
    	mPreferences.edit().putInt(MainScreen.getCameraMirrored()? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, focus_mode).commit();
    	preferenceFocusMode = focus_mode;
    }

    public List<Area> getFocusAreas()
    {
        return mFocusArea;
    }

    public List<Area> getMeteringAreas()
    {
        return mMeteringArea;
    }

    public void updateFocusUI()
    {
        if (!mInitialized || !mFocusSupported) return;

        FocusIndicator focusIndicator = mFocusIndicator;

        if (mState == STATE_IDLE || mState == STATE_INACTIVE)
        {
            if (mFocusArea == null)
                focusIndicator.clear();
            else
            {
                // Users touch on the preview and the indicator represents the
                // metering area. Either focus area is not supported or
                // autoFocus call is not required.
                focusIndicator.showStart();
            }
        }
        else if (mState == STATE_FOCUSING || mState == STATE_FOCUSING_SNAP_ON_FINISH)
        	focusIndicator.showStart();
        else
        {
            if (mState == STATE_SUCCESS)
                focusIndicator.showSuccess();
            else if (mState == STATE_FAIL)
                focusIndicator.showFail();
        }
    }

    public void resetTouchFocus()
    {
        if (!mInitialized) return;

        // Put focus indicator to the center.
        RelativeLayout.LayoutParams p =
                (RelativeLayout.LayoutParams) mFocusIndicatorRotateLayout.getLayoutParams();
        int[] rules = p.getRules();
        rules[RelativeLayout.CENTER_IN_PARENT] = RelativeLayout.TRUE;
        p.setMargins(0, 0, 0, 0);

        mFocusArea = null;
        mMeteringArea = null; 
        
        // allow driver to choose whatever it wants for focusing / metering
        // without these two lines Continuous focus is not re-enabled on HTC One
        int focusMode = getFocusMode();
    	if((focusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE ||
		    focusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO ||
		    focusMode == CameraParameters.AF_MODE_AUTO ||
		    focusMode == CameraParameters.AF_MODE_MACRO) &&
		    mFocusAreaSupported)
    	{
    		String modeName = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getString("defaultModeName", null);
    		boolean isVideoRecording = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getBoolean("videorecording", false);
			if(!(modeName != null && modeName.compareTo("video") == 0 && !isVideoRecording
			   && (Build.MODEL.contains(MainScreen.deviceSS3_01) || Build.MODEL.contains(MainScreen.deviceSS3_02) ||
	    				Build.MODEL.contains(MainScreen.deviceSS3_03) || Build.MODEL.contains(MainScreen.deviceSS3_04) ||
	    				Build.MODEL.contains(MainScreen.deviceSS3_05) || Build.MODEL.contains(MainScreen.deviceSS3_06) ||
	    				Build.MODEL.contains(MainScreen.deviceSS3_07) || Build.MODEL.contains(MainScreen.deviceSS3_08) ||
	    				Build.MODEL.contains(MainScreen.deviceSS3_09) || Build.MODEL.contains(MainScreen.deviceSS3_10) ||
	    				Build.MODEL.contains(MainScreen.deviceSS3_11) || Build.MODEL.contains(MainScreen.deviceSS3_12) ||	Build.MODEL.contains(MainScreen.deviceSS3_13))))
			{
				CameraController.getInstance().setCameraFocusAreas(null);
			}
    	}
    }

    public void calculateTapArea(int focusWidth, int focusHeight, float areaMultiple,
            int x, int y, int previewWidth, int previewHeight, Rect rect)
    {
        int areaWidth = (int)(focusWidth * areaMultiple);
        int areaHeight = (int)(focusHeight * areaMultiple);
        int left = Util.clamp(x - areaWidth / 2, 0, previewWidth - areaWidth);
        int top = Util.clamp(y - areaHeight / 2, 0, previewHeight - areaHeight);
        
        int right = Util.clamp(x + areaWidth / 2, areaWidth, previewWidth);
        int bottom = Util.clamp(y + areaHeight / 2, areaHeight, previewHeight);
        
        RectF rectF = new RectF(left, top, right, bottom);
        mMatrix.mapRect(rectF);
        Util.rectFToRect(rectF, rect);
        
        if(rect.left < -1000)
        	rect.left = -1000;
        if(rect.left > 1000)
        	rect.left = 1000;
        
        if(rect.right < -1000)
        	rect.right = -1000;
        if(rect.right > 1000)
        	rect.right = 1000;
        
        if(rect.top < -1000)
        	rect.top = -1000;
        if(rect.top > 1000)
        	rect.top = 1000;
        
        if(rect.bottom < -1000)
        	rect.bottom = -1000;
        if(rect.bottom > 1000)
        	rect.bottom = 1000;
    }

    public boolean isFocusCompleted()
    {
        return mState == STATE_SUCCESS || mState == STATE_FAIL;
    }

    public void removeMessages()
    {
        mHandler.removeMessages(RESET_TOUCH_FOCUS);
    }

    public void overrideFocusMode(int focusMode)
    {
        mOverrideFocusMode = focusMode;
    }

    public void setAeAwbLock(boolean lock)
    {
        mAeAwbLock = lock;
    }

    public boolean getAeAwbLock()
    {
        return mAeAwbLock;
    }

    private static boolean isSupported(int value, byte[] supported)
    {
    	boolean isAvailable = false;
		for(int currMode : supported)
		{
			if(currMode == value)
			{
				isAvailable = true;
				break;
			}
		}
		return isAvailable;
    }

    private boolean needAutoFocusCall()
    {
        int focusMode = getFocusMode();
        boolean useFocus = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getBoolean("UseFocus", true);
        return !(focusMode == CameraParameters.AF_MODE_INFINITY
                || focusMode == CameraParameters.AF_MODE_FIXED
                || focusMode == CameraParameters.AF_MODE_EDOF		// FixMe: EDOF likely needs auto-focus call 
                || !useFocus
                || mFocusDisabled);
    }
    
    private boolean focusOnShutterDisabled()
    {
    	return PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getBoolean("ContinuousCapturing", false);
    }
    
    public boolean onBroadcast(int arg1, int arg2)
	{
		if (arg1 == PluginManager.MSG_CONTROL_LOCKED) 
		{
			mFocusDisabled = true;
			cancelAutoFocus();
		}
		else if (arg1 == PluginManager.MSG_CONTROL_UNLOCKED) 
		{
			mFocusDisabled = false;
			cancelAutoFocus();
		}
		else if (arg1 == PluginManager.MSG_CAPTURE_FINISHED) 
		{
			mFocusDisabled = false;
			cancelAutoFocus();
		}
		else if (arg1 == PluginManager.MSG_FOCUS_CHANGED)
		{
			int fm = CameraController.getInstance().getFocusMode();
			if (fm != -1)
				preferenceFocusMode = fm;
		}
		else if (arg1 == PluginManager.MSG_PREVIEW_CHANGED)
		{
			initialize(MainScreen.getCameraMirrored(), 90);
		}
		
		return false;
	}	
}
