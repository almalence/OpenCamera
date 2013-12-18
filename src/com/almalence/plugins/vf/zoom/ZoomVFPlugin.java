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

package com.almalence.plugins.vf.zoom;

import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.graphics.PointF;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginViewfinder;
import com.almalence.opencam.R;
import com.almalence.opencam.ui.VerticalSeekBar;

/***
Implements zoom functionality - slider, pinch, sound buttons
***/

public class ZoomVFPlugin extends PluginViewfinder
{
	private VerticalSeekBar zoomBar = null;
    private int zoomCurrent = 0;
    private View zoomPanelView = null;
    private LinearLayout zoomPanel = null;
    
    private int mainLayoutHeight = 0;
    private int zoomPanelWidth = 0;
    
    private boolean panelOpened = false;
    private boolean panelToBeOpen = false;
    private boolean panelOpening = false;
    private boolean panelClosing = false;
    private boolean mZoomDisabled = false;
    
    private boolean isEnabled =true;
    
    private Handler zoomHandler;
    
    private static final int CLOSE_ZOOM_PANEL = 0;
    private static final int CLOSE_ZOOM_PANEL_DELAY = 1500;
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CLOSE_ZOOM_PANEL: {
                    closeZoomPanel();                    
                    break;
                }
            }
        }
    }
//    
	public ZoomVFPlugin()
	{
		super("com.almalence.plugins.zoomvf",
			  R.xml.preferences_vf_zoom,
			  0,
			  0,
			  null);
		
		zoomHandler = new MainHandler();
	}
	
	@Override
	public void onCreate()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		isEnabled = prefs.getBoolean("enabledPrefZoom", true);
		
		panelOpened = false;
		
		LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();		
		zoomPanelView = inflator.inflate(R.layout.plugin_vf_zoom_layout, null, false);
		this.zoomPanel = (LinearLayout)zoomPanelView.findViewById(R.id.zoomLayout);
        this.zoomPanel.setOnTouchListener(new OnTouchListener() 
        {
			@Override
			public boolean onTouch(View v, MotionEvent event) 
			{				
				return false;
			}
		});
		
		this.zoomBar = (VerticalSeekBar)zoomPanelView.findViewById(R.id.zoomSeekBar);
		this.zoomBar.setOnTouchListener(new OnTouchListener() 
        {
			@Override
			public boolean onTouch(View v, MotionEvent event) 
			{
				if(mZoomDisabled)
				{
					if(panelOpened)
						zoomHandler.sendEmptyMessageDelayed(CLOSE_ZOOM_PANEL, CLOSE_ZOOM_PANEL_DELAY);
					return true;
				}
				
				switch(event.getAction() & MotionEvent.ACTION_MASK)
				{
					case MotionEvent.ACTION_DOWN:
					{
						if(!panelOpened)
						{
							openZoomPanel();
							zoomHandler.removeMessages(CLOSE_ZOOM_PANEL);
							return true;
						}
						if(panelClosing)
						{
							panelToBeOpen = true;
							return true;
						}
					} break;
					case MotionEvent.ACTION_UP:
					{
						if(panelOpened || panelOpening)
							zoomHandler.sendEmptyMessageDelayed(CLOSE_ZOOM_PANEL, CLOSE_ZOOM_PANEL_DELAY);
					}
					break;
					case MotionEvent.ACTION_MOVE:
						return false;
				}
				
				return false;
			}
		});
        this.zoomBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar){}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) 
			{
				zoomHandler.removeMessages(CLOSE_ZOOM_PANEL);
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
			{
				if (fromUser)
				{
					zoomHandler.removeMessages(CLOSE_ZOOM_PANEL);
					zoomModify(progress - zoomCurrent);
				}
			}
		});
	}
	
	@Override
	public void onStart()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		isEnabled = prefs.getBoolean("enabledPrefZoom", true);
	}
	
	@Override
	public void onStop()
	{
		List<View> specialView = new ArrayList<View>();
		RelativeLayout specialLayout = (RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout);
		for(int i = 0; i < specialLayout.getChildCount(); i++)
			specialView.add(specialLayout.getChildAt(i));

		for(int j = 0; j < specialView.size(); j++)
		{
			View view = specialView.get(j);
			int view_id = view.getId();
			int zoom_id = this.zoomPanel.getId();
			if(view_id == zoom_id)
			{
				if(view.getParent() != null)
					((ViewGroup)view.getParent()).removeView(view);
				
				specialLayout.removeView(view);
			}
		}	
	}
	
	@Override
	public void onGUICreate()
	{
		List<View> specialView = new ArrayList<View>();
		RelativeLayout specialLayout = (RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout);
		for(int i = 0; i < specialLayout.getChildCount(); i++)
			specialView.add(specialLayout.getChildAt(i));

		for(int j = 0; j < specialView.size(); j++)
		{
			View view = specialView.get(j);
			int view_id = view.getId();
			int zoom_id = this.zoomPanel.getId();
			if(view_id == zoom_id)
			{
				if(view.getParent() != null)
					((ViewGroup)view.getParent()).removeView(view);
				
				specialLayout.removeView(view);
			}
		}
		
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)MainScreen.thiz.findViewById(R.id.specialPluginsLayout).getLayoutParams();
		//mainLayoutHeight = MainScreen.thiz.findViewById(R.id.specialPluginsLayout).getHeight();
		mainLayoutHeight = lp.height;
		
		zoomPanelWidth = MainScreen.thiz.getResources().getDrawable(R.drawable.scrubber_control_pressed_holo).getMinimumWidth();
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
		params.setMargins(-zoomPanelWidth/2, 0, 0, 0);
		params.height = mainLayoutHeight/2;
		
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout)).addView(this.zoomPanel, params);
		
		this.zoomPanel.setLayoutParams(params);
		this.zoomPanel.requestLayout();
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout)).requestLayout();
	}

	@Override
	public void onResume()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		isEnabled = prefs.getBoolean("enabledPrefZoom", true);
		
		if (!isEnabled)
		{
			zoomPanel.setVisibility(View.GONE);
		}
		else {
			zoomPanel.setVisibility(View.VISIBLE);
		}		
		
		String modeName = prefs.getString("defaultModeName", null);
		if(modeName != null && modeName.compareTo("video") == 0
		   && (Build.MODEL.contains(MainScreen.deviceSS3_01) || Build.MODEL.contains(MainScreen.deviceSS3_02) ||
   				Build.MODEL.contains(MainScreen.deviceSS3_03) || Build.MODEL.contains(MainScreen.deviceSS3_04) ||
   				Build.MODEL.contains(MainScreen.deviceSS3_05) || Build.MODEL.contains(MainScreen.deviceSS3_06) ||
   				Build.MODEL.contains(MainScreen.deviceSS3_07) || Build.MODEL.contains(MainScreen.deviceSS3_08) ||
   				Build.MODEL.contains(MainScreen.deviceSS3_09) || Build.MODEL.contains(MainScreen.deviceSS3_10) ||
   				Build.MODEL.contains(MainScreen.deviceSS3_11) || Build.MODEL.contains(MainScreen.deviceSS3_12) ||	Build.MODEL.contains(MainScreen.deviceSS3_13)))
			zoomPanel.setVisibility(View.GONE);
	}	

	@Override
	public void onCameraParametersSetup()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		String modeName = prefs.getString("defaultModeName", null);
		if (!isEnabled || (modeName != null && modeName.compareTo("video") == 0
				   && (Build.MODEL.contains(MainScreen.deviceSS3_01) || Build.MODEL.contains(MainScreen.deviceSS3_02) ||
		    				Build.MODEL.contains(MainScreen.deviceSS3_03) || Build.MODEL.contains(MainScreen.deviceSS3_04) ||
		    				Build.MODEL.contains(MainScreen.deviceSS3_05) || Build.MODEL.contains(MainScreen.deviceSS3_06) ||
		    				Build.MODEL.contains(MainScreen.deviceSS3_07) || Build.MODEL.contains(MainScreen.deviceSS3_08) ||
		    				Build.MODEL.contains(MainScreen.deviceSS3_09) || Build.MODEL.contains(MainScreen.deviceSS3_10) ||
		    				Build.MODEL.contains(MainScreen.deviceSS3_11) || Build.MODEL.contains(MainScreen.deviceSS3_12) ||	Build.MODEL.contains(MainScreen.deviceSS3_13))))
			return;
		
		zoomCurrent = 0;				

		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
		Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
        if (cp.isZoomSupported())
        {
        	zoomBar.setMax(cp.getMaxZoom());
        	zoomBar.setProgressAndThumb(0);
        	zoomPanel.setVisibility(View.VISIBLE);
        }
        else
        	zoomPanel.setVisibility(View.GONE);
	}
	
	private void zoomModify(int delta)
	{
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
		Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
		if (cp==null)
			return;
		
		if (cp.isZoomSupported())
		{		
			try
			{
				zoomCurrent += delta;
					
				if (zoomCurrent < 0)
				{
					zoomCurrent = 0;
				}
				else if (zoomCurrent > cp.getMaxZoom())
				{
					zoomCurrent = cp.getMaxZoom();
				}
					
				cp.setZoom(zoomCurrent);
					
				MainScreen.thiz.setCameraParameters(cp);
				
				zoomBar.setProgressAndThumb(zoomCurrent);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (!isEnabled)
			return false;
		
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
    	{
    		this.zoomModify(-1);
    		return true;
    	}
    	else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
    	{
    		this.zoomModify(1);
    		return true;
    	}
		return false;
	}
	
	PointF mid = new PointF();
	static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;
    
    public void closeZoomPanel()
    { 
    	panelClosing = true;
    	
    	this.zoomPanel.clearAnimation();    	
		Animation animation = new TranslateAnimation(0, -zoomPanelWidth/2 , 0, 0);
		animation.setDuration(500);
		animation.setRepeatCount(0);
		animation.setInterpolator(new LinearInterpolator());
		animation.setFillAfter(true);		
		
		this.zoomPanel.setAnimation(animation);
		
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)zoomPanel.getLayoutParams();
				if (params==null)
					return;
				params.setMargins(-zoomPanelWidth/2, 0, 0, 0);
				zoomPanel.setLayoutParams(params);				
				zoomPanel.clearAnimation();
				
		    	panelOpened = false;
		    	panelClosing = false;
		    	
		    	if(panelToBeOpen)
		    	{
		    		panelToBeOpen = false;
		    		openZoomPanel();
		    	}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {}

			@Override
			public void onAnimationStart(Animation animation) {}
		});			
    }
    
    public void openZoomPanel()
    {
    	panelOpening = true;
    	this.zoomPanel.clearAnimation();    	
		Animation animation = new TranslateAnimation(0, zoomPanelWidth/2 , 0, 0);
		animation.setDuration(500);
		animation.setRepeatCount(0);
		animation.setInterpolator(new LinearInterpolator());
		animation.setFillAfter(true);
		
		this.zoomPanel.setAnimation(animation);
		
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)zoomPanel.getLayoutParams();
				params.setMargins(0, 0, 0, 0);
				zoomPanel.setLayoutParams(params);
			
				zoomPanel.clearAnimation();
				zoomPanel.requestLayout();
				
				panelOpened = true;
				panelOpening = false;
			}

			@Override
			public void onAnimationRepeat(Animation animation) {}

			@Override
			public void onAnimationStart(Animation animation) {}			
		});
    }
    
    @Override
    public boolean onBroadcast(int arg1, int arg2)
	{
    	if (!isEnabled)
			return false;
		if (arg1 == PluginManager.MSG_CONTROL_LOCKED) 
			mZoomDisabled = true;
		else if (arg1 == PluginManager.MSG_CONTROL_UNLOCKED) 
			mZoomDisabled = false;
		return false;
	}
}
