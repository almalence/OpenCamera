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

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

/* <!-- +++
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.PluginViewfinder;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.ApplicationInterface;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.PluginViewfinder;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

import com.almalence.ui.RotateImageView;

/***
 * Implements viewfinder plugin - controls Auto exposure and Auto white balance
 * locks
 ***/

public class AeAwLockVFPlugin extends PluginViewfinder
{
	private static final Integer	icon_ae_lock	= R.drawable.gui_almalence_aelock_on;
	private static final Integer	icon_ae_unlock	= R.drawable.gui_almalence_aelock_off;

	private static final Integer	icon_aw_lock	= R.drawable.gui_almalence_awlock_on;
	private static final Integer	icon_aw_unlock	= R.drawable.gui_almalence_awlock_off;

	RotateImageView					aeLockButton;
	RotateImageView					awLockButton;

	View							buttonsLayout;

	boolean							showAEAWLock;

	boolean							aeLocked		= false;
	boolean							awLocked		= false;

	public AeAwLockVFPlugin()
	{
		super("com.almalence.plugins.aeawlockvf", R.xml.preferences_vf_aeawlock, 0, 0, null);
	}

	@Override
	public void onResume()
	{
		refreshPreferences();
	}

	private void refreshPreferences()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		showAEAWLock = prefs.getBoolean("showAEAWLockPref", false);
		
		aeLocked = false;
		awLocked = false;
		prefs.edit().putBoolean(ApplicationScreen.sAWBLockPref, aeLocked).commit();
		prefs.edit().putBoolean(ApplicationScreen.sAWBLockPref, awLocked).commit();
		
		AeUnlock();
		AwUnlock();
	}

	@Override
	public void onGUICreate()
	{
		refreshPreferences();

		if (showAEAWLock)
		{
			LayoutInflater inflator = ApplicationScreen.instance.getLayoutInflater();
			buttonsLayout = inflator.inflate(R.layout.plugin_vf_aeawlock_layout, null, false);
			buttonsLayout.setVisibility(View.VISIBLE);

			aeLockButton = (RotateImageView) buttonsLayout.findViewById(R.id.buttonAELock);
			awLockButton = (RotateImageView) buttonsLayout.findViewById(R.id.buttonAWLock);

			if (!CameraController.isExposureLockSupported())
				aeLockButton.setVisibility(View.INVISIBLE);
			if (!CameraController.isWhiteBalanceLockSupported())
				awLockButton.setVisibility(View.INVISIBLE);

			aeLockButton.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (CameraController.isExposureLockSupported() && aeLocked)
						AeUnlock();
					else if (CameraController.isExposureLockSupported() && !aeLocked)
						AeLock();
				}

			});

			awLockButton.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (CameraController.isWhiteBalanceLockSupported() && awLocked)
						AwUnlock();
					else if (CameraController.isWhiteBalanceLockSupported() && !awLocked)
						AwLock();
				}

			});

			ApplicationScreen.getGUIManager().removeViews(buttonsLayout, R.id.specialPluginsLayout2);

			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT);
			params.height = (int) ApplicationScreen.getAppResources().getDimension(R.dimen.aeawlock_size);

			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

			((RelativeLayout) ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout2)).addView(
					this.buttonsLayout, params);

			this.buttonsLayout.setLayoutParams(params);
			this.buttonsLayout.requestLayout();

			((RelativeLayout) ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout2)).requestLayout();
		}
	}

	@Override
	public void onPause()
	{
		if (this.buttonsLayout != null)
			ApplicationScreen.getGUIManager().removeViews(buttonsLayout, R.id.specialPluginsLayout2);
	}

	private void AeLock()
	{
		if (CameraController.isExposureLockSupported())
		{
			CameraController.setAutoExposureLock(true);
	
			Drawable icon = ApplicationScreen.getMainContext().getResources().getDrawable(icon_ae_lock);
			aeLockButton.setImageDrawable(icon);
	
			aeLocked = true;
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
			prefs.edit().putBoolean(ApplicationScreen.sAELockPref, aeLocked).commit();
		}
	}

	private void AwLock()
	{
		if (CameraController.isWhiteBalanceLockSupported())
		{
			CameraController.setAutoWhiteBalanceLock(true);
	
			Drawable icon = ApplicationScreen.getMainContext().getResources().getDrawable(icon_aw_lock);
			awLockButton.setImageDrawable(icon);
	
			awLocked = true;
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
			prefs.edit().putBoolean(ApplicationScreen.sAWBLockPref, awLocked).commit();
		}
	}

	private void AeUnlock()
	{
		if (CameraController.isExposureLockSupported())
			CameraController.setAutoExposureLock(false);
		
		if (aeLockButton!=null)
		{
			Drawable icon = ApplicationScreen.getMainContext().getResources().getDrawable(icon_ae_unlock);
			aeLockButton.setImageDrawable(icon);
		}
	
		aeLocked = false;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		prefs.edit().putBoolean(ApplicationScreen.sAELockPref, aeLocked).commit();
	}

	private void AwUnlock()
	{
		if (CameraController.isWhiteBalanceLockSupported())
			CameraController.setAutoWhiteBalanceLock(false);
		if (aeLockButton!=null)
		{
			Drawable icon = ApplicationScreen.getMainContext().getResources().getDrawable(icon_aw_unlock);
			awLockButton.setImageDrawable(icon);
		}
	
		awLocked = false;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		prefs.edit().putBoolean(ApplicationScreen.sAWBLockPref, awLocked).commit();
	}

	@Override
	public void onOrientationChanged(int orientation)
	{
		if (aeLockButton != null)
		{
			aeLockButton.setOrientation(ApplicationScreen.getGUIManager().getLayoutOrientation());
			aeLockButton.invalidate();
			aeLockButton.requestLayout();
		}
		if (awLockButton != null)
		{
			awLockButton.setOrientation(ApplicationScreen.getGUIManager().getLayoutOrientation());
			awLockButton.invalidate();
			awLockButton.requestLayout();
		}
	}

	@Override
	public void onCaptureFinished()
	{
		if (aeLocked && CameraController.isExposureLockSupported() && !CameraController.isExposureLocked())
			AeUnlock();
		if (awLocked && CameraController.isWhiteBalanceLockSupported() && !CameraController.isWhiteBalanceLocked())
			AwUnlock();
	}

	@Override
	public void onPreferenceCreate(PreferenceFragment prefActivity)
	{
		PreferenceCategory cat = (PreferenceCategory) prefActivity.findPreference("Pref_VF_ShowAEAWLock_Category");
		if (cat != null && !ApplicationScreen.getGUIManager().mEVLockSupported && !ApplicationScreen.getGUIManager().mWBLockSupported)
		{
			CheckBoxPreference cp = (CheckBoxPreference) cat.findPreference("showAEAWLockPref");
			if (cp != null)
				cp.setEnabled(false);
		}
	}
	
	@Override
	public boolean onBroadcast(int arg1, int arg2)
	{
		if (arg1 == ApplicationInterface.MSG_AEWB_CHANGED)
		{
			if (CameraController.isExposureLockSupported() && CameraController.isExposureLocked())
			{
				Drawable icon = ApplicationScreen.getMainContext().getResources().getDrawable(icon_ae_lock);
				if (aeLockButton!=null)
					aeLockButton.setImageDrawable(icon);
			}
			else
			{
				Drawable icon = ApplicationScreen.getMainContext().getResources().getDrawable(icon_ae_unlock);
				if (aeLockButton!=null)
					aeLockButton.setImageDrawable(icon);
			}
			if (CameraController.isWhiteBalanceLockSupported() && CameraController.isWhiteBalanceLocked())
			{
				Drawable icon = ApplicationScreen.getMainContext().getResources().getDrawable(icon_aw_lock);
				if (awLockButton!=null)
					awLockButton.setImageDrawable(icon);
			}
			else
			{
				Drawable icon = ApplicationScreen.getMainContext().getResources().getDrawable(icon_aw_unlock);
				if (awLockButton!=null)
					awLockButton.setImageDrawable(icon);
			}
			return true;
		}
		return false;
	}
}
