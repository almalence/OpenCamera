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

/* <!-- +++
 package com.almalence.opencam_plus.ui;
 +++ --> */
// <!-- -+-
package com.almalence.opencam.ui;

//-+- -->

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.almalence.ui.ListPreferenceAdapter;
import com.almalence.ui.RotateDialog;

//<!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.R;
 +++ --> */

public class ColorEffectQuickSetting
{
	RotateDialog			dialog					= null;
	private ListView		colorEffetcsListView	= null;

	private CharSequence[]	mEntries;
	private CharSequence[]	mEntryValues;

	private int				mClickedDialogEntryIndex;

	Context					context;

	public ColorEffectQuickSetting(Context context)
	{
		this.context = context;
	}

	public void showDialog()
	{
		int currentIdx = -1;

		final String pref1 = MainScreen.sRearColorEffectPref;
		final String pref2 = MainScreen.sFrontColorEffectPref;

		int[] colorEfects = CameraController.getSupportedColorEffects();

		// Normally it should never happens. It's paranoia check.
		if (colorEfects == null || colorEfects.length == 0 || CameraController.ColorEffectsNamesList == null
				|| !CameraController.isColorEffectSupported())
		{
			return;
		}

		mEntries = CameraController.ColorEffectsNamesList.toArray(new String[CameraController.ColorEffectsNamesList
				.size()]);

		mEntryValues = new CharSequence[colorEfects.length];
		for (int i = 0; i < colorEfects.length; i++)
		{
			mEntryValues[i] = Integer.toString(colorEfects[i]);
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		currentIdx = 0;
		try
		{
			currentIdx = Integer.parseInt(prefs.getString(CameraController.isFrontCamera() ? pref1 : pref2, "0"));
		} catch (Exception e)
		{
			currentIdx = prefs.getInt(CameraController.isFrontCamera() ? pref1 : pref2,
					MainScreen.sDefaultColorEffectValue);
		}

		int idx = 0;
		if (currentIdx != -1)
		{
			// set currently selected image size
			for (idx = 0; idx < mEntryValues.length; ++idx)
			{
				if (Integer.valueOf(mEntryValues[idx].toString()) == currentIdx)
				{
					mClickedDialogEntryIndex = idx;
					break;
				}
			}
		} else
		{
			mClickedDialogEntryIndex = 0;
		}

		dialog = new QuickSettingDialog(context);
		colorEffetcsListView = (ListView) dialog.findViewById(android.R.id.list);

		ListPreferenceAdapter adapter = new ListPreferenceAdapter(context, R.layout.simple_list_item_single_choice,
				android.R.id.text1, (String[]) mEntries, mClickedDialogEntryIndex);

		colorEffetcsListView.setAdapter(adapter);
		colorEffetcsListView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				if (mClickedDialogEntryIndex != position)
				{
					mClickedDialogEntryIndex = position;
					Object newValue = mEntryValues[mClickedDialogEntryIndex];

					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
					prefs.edit()
							.putString(CameraController.isFrontCamera() ? pref1 : pref2,
									String.valueOf(newValue.toString())).commit();

					CameraController.setCameraColorEffect(Integer.parseInt(newValue.toString()));
				}
				dialog.dismiss();
			}
		});
		dialog.show();
	}

	public void setOrientation()
	{
		if (dialog != null)
		{
			dialog.setRotate(AlmalenceGUI.mDeviceOrientation);
		}
	}
}
