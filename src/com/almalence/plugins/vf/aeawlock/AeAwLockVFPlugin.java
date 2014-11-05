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
import android.hardware.Camera;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.PluginViewfinder;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
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
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		showAEAWLock = prefs.getBoolean("showAEAWLockPref", false);
	}

	@Override
	public void onGUICreate()
	{
		refreshPreferences();

		if (showAEAWLock)
		{
			LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
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
					Camera.Parameters params = CameraController.getCameraParameters();
					if (params != null)
					{
						if (CameraController.isExposureLockSupported() && params.getAutoExposureLock())
							AeUnlock();
						else if (CameraController.isExposureLockSupported()
								&& !params.getAutoExposureLock())
							AeLock();
					}
				}

			});

			awLockButton.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Camera.Parameters params = CameraController.getCameraParameters();
					if (params != null)
					{
						if (CameraController.isWhiteBalanceLockSupported()
								&& params.getAutoWhiteBalanceLock())
							AwUnlock();
						else if (CameraController.isWhiteBalanceLockSupported()
								&& !params.getAutoWhiteBalanceLock())
							AwLock();
					}
				}

			});

			MainScreen.getGUIManager().removeViews(buttonsLayout, R.id.specialPluginsLayout2);

			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT);
			params.height = (int) MainScreen.getAppResources().getDimension(R.dimen.aeawlock_size);

			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

			((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout2)).addView(
					this.buttonsLayout, params);

			this.buttonsLayout.setLayoutParams(params);
			this.buttonsLayout.requestLayout();

			((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout2)).requestLayout();

//			aeLockButton.setOrientation(MainScreen.getGUIManager().getLayoutOrientation());
//			aeLockButton.invalidate();
//			aeLockButton.requestLayout();
//			awLockButton.setOrientation(MainScreen.getGUIManager().getLayoutOrientation());
//			awLockButton.invalidate();
//			awLockButton.requestLayout();
		}
	}

	@Override
	public void onPause()
	{
		if (this.buttonsLayout != null)
			MainScreen.getGUIManager().removeViews(buttonsLayout, R.id.specialPluginsLayout2);
	}

	private void AeLock()
	{
		Camera.Parameters params = CameraController.getCameraParameters();
		if (params != null)
		{
			if (CameraController.isExposureLockSupported())
			{
				params.setAutoExposureLock(true);
				CameraController.setCameraParameters(params);

				Drawable icon = MainScreen.getMainContext().getResources().getDrawable(icon_ae_lock);
				aeLockButton.setImageDrawable(icon);

				aeLocked = true;
			}
		}
	}

	private void AwLock()
	{
		Camera.Parameters params = CameraController.getCameraParameters();
		if (params != null)
		{
			if (CameraController.isWhiteBalanceLockSupported())
			{
				params.setAutoWhiteBalanceLock(true);
				CameraController.setCameraParameters(params);

				Drawable icon = MainScreen.getMainContext().getResources().getDrawable(icon_aw_lock);
				awLockButton.setImageDrawable(icon);

				awLocked = true;
			}
		}
	}

	private void AeUnlock()
	{
		Camera.Parameters params = CameraController.getCameraParameters();
		if (params != null)
		{
			if (CameraController.isExposureLockSupported() && params.getAutoExposureLock())
			{
				params.setAutoExposureLock(false);
				CameraController.setCameraParameters(params);
			}
			Drawable icon = MainScreen.getMainContext().getResources().getDrawable(icon_ae_unlock);
			aeLockButton.setImageDrawable(icon);

			aeLocked = false;
		}
	}

	private void AwUnlock()
	{
		Camera.Parameters params = CameraController.getCameraParameters();
		if (params != null)
		{
			if (CameraController.isWhiteBalanceLockSupported() && params.getAutoWhiteBalanceLock())
			{
				params.setAutoWhiteBalanceLock(false);
				CameraController.setCameraParameters(params);
			}
			Drawable icon = MainScreen.getMainContext().getResources().getDrawable(icon_aw_unlock);
			awLockButton.setImageDrawable(icon);

			awLocked = false;
		}
	}

	@Override
	public void onOrientationChanged(int orientation)
	{
		if (aeLockButton != null)
		{
			aeLockButton.setOrientation(MainScreen.getGUIManager().getLayoutOrientation());
			aeLockButton.invalidate();
			aeLockButton.requestLayout();
		}
		if (awLockButton != null)
		{
			awLockButton.setOrientation(MainScreen.getGUIManager().getLayoutOrientation());
			awLockButton.invalidate();
			awLockButton.requestLayout();
		}
	}

	@Override
	public void onCaptureFinished()
	{
		Camera.Parameters params = CameraController.getCameraParameters();
		if (aeLocked && CameraController.isExposureLockSupported() && !params.getAutoExposureLock())
			AeUnlock();
		if (awLocked && CameraController.isWhiteBalanceLockSupported()
				&& !params.getAutoWhiteBalanceLock())
			AwUnlock();
	}

	@Override
	public void onPreferenceCreate(PreferenceFragment prefActivity)
	{
		PreferenceCategory cat = (PreferenceCategory) prefActivity.findPreference("Pref_VF_ShowAEAWLock_Category");
		if (cat != null && !MainScreen.getGUIManager().mEVLockSupported && !MainScreen.getGUIManager().mWBLockSupported)
		{
			CheckBoxPreference cp = (CheckBoxPreference) cat.findPreference("showAEAWLockPref");
			if (cp != null)
				cp.setEnabled(false);
		}
	}
	
	@Override
	public boolean onBroadcast(int arg1, int arg2)
	{
		if (arg1 == PluginManager.MSG_AEWB_CHANGED)
		{
//			Camera.Parameters params = CameraController.getCameraParameters();
			if (CameraController.isExposureLockSupported() && CameraController.isExposureLock())
			{
				Drawable icon = MainScreen.getMainContext().getResources().getDrawable(icon_ae_lock);
				if (aeLockButton!=null)
					aeLockButton.setImageDrawable(icon);
			}
			else
			{
				Drawable icon = MainScreen.getMainContext().getResources().getDrawable(icon_ae_unlock);
				if (aeLockButton!=null)
					aeLockButton.setImageDrawable(icon);
			}
			if (CameraController.isWhiteBalanceLockSupported() && CameraController.isWhiteBalanceLock())
			{
				Drawable icon = MainScreen.getMainContext().getResources().getDrawable(icon_aw_lock);
				if (awLockButton!=null)
					awLockButton.setImageDrawable(icon);
			}
			else
			{
				Drawable icon = MainScreen.getMainContext().getResources().getDrawable(icon_aw_unlock);
				if (awLockButton!=null)
					awLockButton.setImageDrawable(icon);
			}
			return true;
		}
		return false;
	}
}
