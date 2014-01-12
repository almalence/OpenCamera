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

package com.almalence.plugins.vf.aeawlock;

import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;


/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginViewfinder;
import com.almalence.opencam_plus.R;
+++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginViewfinder;
import com.almalence.opencam.R;
//-+- -->

import com.almalence.ui.RotateImageView;

/***
Implements viewfinder plugin - controls Auto exposure and Auto white balance locks
***/

public class AeAwLockVFPlugin extends PluginViewfinder							  
{
	private final static Integer icon_ae_lock = R.drawable.gui_almalence_aelock_on;
	private final static Integer icon_ae_unlock = R.drawable.gui_almalence_aelock_off;
	
	private final static Integer icon_aw_lock = R.drawable.gui_almalence_awlock_on;
	private final static Integer icon_aw_unlock = R.drawable.gui_almalence_awlock_off;
	
	RotateImageView aeLockButton;
	RotateImageView awLockButton;
	
	View buttonsLayout;
	
	boolean showAEAWLock;
	
	boolean aeLocked = false;
	boolean awLocked = false;
	
//	private int mLayoutOrientationCurrent;
//	private int mDisplayOrientationCurrent;
	
	public AeAwLockVFPlugin()
	{
		super("com.almalence.plugins.aeawlockvf",
			  R.xml.preferences_vf_aeawlock,
			  0,
			  0,
			  null);		
	}
	
	@Override
	public void onResume()
	{
		refreshPreferences();
	}
	
	private void refreshPreferences()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		showAEAWLock = prefs.getBoolean("showAEAWLockPref", false);		 
	}

	@Override
	public void onGUICreate()
	{
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
		refreshPreferences();
		
		if(showAEAWLock)
		{
		
		LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();
		buttonsLayout = inflator.inflate(R.layout.plugin_vf_aeawlock_layout, null, false);
		buttonsLayout.setVisibility(View.VISIBLE);
		
		aeLockButton = (RotateImageView)buttonsLayout.findViewById(R.id.buttonAELock);
		awLockButton = (RotateImageView)buttonsLayout.findViewById(R.id.buttonAWLock);
		
		if(!MainScreen.thiz.isExposureLockSupported())
			aeLockButton.setVisibility(View.INVISIBLE);
		if(!MainScreen.thiz.isWhiteBalanceLockSupported())
			awLockButton.setVisibility(View.INVISIBLE);
		
		aeLockButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Camera.Parameters params = MainScreen.thiz.getCameraParameters();
				if (params != null)
				{
					if(MainScreen.thiz.isExposureLockSupported() && params.getAutoExposureLock())
						AeUnlock();
					else if(MainScreen.thiz.isExposureLockSupported() && !params.getAutoExposureLock())
						AeLock();
				}
			}
			
		});
		
		awLockButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Camera.Parameters params = MainScreen.thiz.getCameraParameters();
				if (params != null)
				{
					if(MainScreen.thiz.isWhiteBalanceLockSupported() && params.getAutoWhiteBalanceLock())
						AwUnlock();
					else if(MainScreen.thiz.isWhiteBalanceLockSupported() && !params.getAutoWhiteBalanceLock())
						AwLock();
				}
			}
			
		});
		
//    	mDisplayOrientationCurrent = MainScreen.guiManager.getDisplayOrientation();
//    	int orientation = MainScreen.guiManager.getLayoutOrientation();
//    	mLayoutOrientationCurrent = orientation == 0 || orientation == 180? orientation: (orientation + 180)%360;
		
		
		List<View> specialView = new ArrayList<View>();
		RelativeLayout specialLayout = (RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout2);
		for(int i = 0; i < specialLayout.getChildCount(); i++)
			specialView.add(specialLayout.getChildAt(i));

		for(int j = 0; j < specialView.size(); j++)
		{
			View view = specialView.get(j);
			int view_id = view.getId();
			int layout_id = this.buttonsLayout.getId();
			if(view_id == layout_id)
			{
				if(view.getParent() != null)
					((ViewGroup)view.getParent()).removeView(view);
				
				specialLayout.removeView(view);
			}
		}
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.height = (int)MainScreen.thiz.getResources().getDimension(R.dimen.aeawlock_size);
		
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);		
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout2)).addView(this.buttonsLayout, params);
		
		this.buttonsLayout.setLayoutParams(params);
		this.buttonsLayout.requestLayout();
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout2)).requestLayout();
		
		aeLockButton.setOrientation(MainScreen.guiManager.getLayoutOrientation());
		aeLockButton.invalidate();
		aeLockButton.requestLayout();
		awLockButton.setOrientation(MainScreen.guiManager.getLayoutOrientation());
		awLockButton.invalidate();
		awLockButton.requestLayout();
		}
	}
	
	@Override
	public void onPause()
	{
		if(this.buttonsLayout != null)
		{
			List<View> specialView = new ArrayList<View>();
			RelativeLayout specialLayout = (RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout2);
			for(int i = 0; i < specialLayout.getChildCount(); i++)
				specialView.add(specialLayout.getChildAt(i));
	
			for(int j = 0; j < specialView.size(); j++)
			{
				View view = specialView.get(j);
				int view_id = view.getId();
				int layout_id = this.buttonsLayout.getId();
				if(view_id == layout_id)
				{
					if(view.getParent() != null)
						((ViewGroup)view.getParent()).removeView(view);
					
					specialLayout.removeView(view);
				}
			}
		}
	}
	
	private void AeLock()
	{
		Camera.Parameters params = MainScreen.thiz.getCameraParameters();
		if (params != null)
		{
			if(MainScreen.thiz.isExposureLockSupported())
			{
				params.setAutoExposureLock(true);
				MainScreen.thiz.setCameraParameters(params);
				
				Drawable icon = MainScreen.mainContext.getResources()
						.getDrawable(icon_ae_lock);
				aeLockButton.setImageDrawable(icon);
				
				aeLocked = true;
			}
		}		
	}
	
	private void AwLock()
	{
		Camera.Parameters params = MainScreen.thiz.getCameraParameters();
		if (params != null)
		{			
			if(MainScreen.thiz.isWhiteBalanceLockSupported())
			{
				params.setAutoWhiteBalanceLock(true);
				MainScreen.thiz.setCameraParameters(params);
				
				Drawable icon = MainScreen.mainContext.getResources()
						.getDrawable(icon_aw_lock);
				awLockButton.setImageDrawable(icon);
				
				awLocked = true;
			}
		}		
	}
	
	private void AeUnlock()
	{
		Camera.Parameters params = MainScreen.thiz.getCameraParameters();
		if (params != null)
		{
			if(MainScreen.thiz.isExposureLockSupported() && params.getAutoExposureLock())
			{
				params.setAutoExposureLock(false);
				MainScreen.thiz.setCameraParameters(params);			
			}
			Drawable icon = MainScreen.mainContext.getResources()
					.getDrawable(icon_ae_unlock);
			aeLockButton.setImageDrawable(icon);
			
			aeLocked = false;
		}
	}
	
	private void AwUnlock()
	{
		Camera.Parameters params = MainScreen.thiz.getCameraParameters();
		if (params != null)
		{
			if(MainScreen.thiz.isWhiteBalanceLockSupported() && params.getAutoWhiteBalanceLock())
			{
				params.setAutoWhiteBalanceLock(false);
				MainScreen.thiz.setCameraParameters(params);
			}
			Drawable icon = MainScreen.mainContext.getResources()
					.getDrawable(icon_aw_unlock);
			awLockButton.setImageDrawable(icon);
			
			awLocked = false;
		}
	}
	
	@Override
    public void onOrientationChanged(int orientation)
    {
		if (aeLockButton!= null)
		{
			aeLockButton.setOrientation(MainScreen.guiManager.getLayoutOrientation());
			aeLockButton.invalidate();
			aeLockButton.requestLayout();    			
		}
		if (awLockButton!= null)
		{
			awLockButton.setOrientation(MainScreen.guiManager.getLayoutOrientation());
			awLockButton.invalidate();
			awLockButton.requestLayout();    			
		}
    }
	
	@Override
	public void onCaptureFinished()
	{
		Camera.Parameters params = MainScreen.thiz.getCameraParameters();
		if(aeLocked && MainScreen.thiz.isExposureLockSupported() && !params.getAutoExposureLock())
			AeUnlock();
		if(awLocked && MainScreen.thiz.isWhiteBalanceLockSupported() && !params.getAutoWhiteBalanceLock())
			AwUnlock();
	}
	
	@Override
	public void onPreferenceCreate(PreferenceActivity prefActivity)
	{
		PreferenceCategory cat = (PreferenceCategory)prefActivity.findPreference("Pref_VF_ShowAEAWLock_Category");
		if(cat != null && !MainScreen.guiManager.mEVLockSupported && !MainScreen.guiManager.mWBLockSupported)
		{
			CheckBoxPreference cp = (CheckBoxPreference)cat.findPreference("showAEAWLockPref");
			if(cp != null)
				cp.setEnabled(false);
		}
	}
	
	@Override
	public void onPreferenceCreate(PreferenceFragment prefActivity)
	{
		PreferenceCategory cat = (PreferenceCategory)prefActivity.findPreference("Pref_VF_ShowAEAWLock_Category");
		if(cat != null && !MainScreen.guiManager.mEVLockSupported && !MainScreen.guiManager.mWBLockSupported)
		{
			CheckBoxPreference cp = (CheckBoxPreference)cat.findPreference("showAEAWLockPref");
			if(cp != null)
				cp.setEnabled(false);
		}
	}
}
