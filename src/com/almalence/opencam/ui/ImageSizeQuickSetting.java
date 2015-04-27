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
import android.media.CamcorderProfile;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.plugins.capture.panoramaaugmented.PanoramaAugmentedCapturePlugin;
import com.almalence.plugins.capture.video.VideoCapturePlugin;
import com.almalence.ui.ListPreferenceAdapter;
import com.almalence.ui.RotateDialog;
//<!-- -+-
//-+- -->

/* <!-- +++
 import com.almalence.opencam_plus.R;
 +++ --> */

public class ImageSizeQuickSetting
{
	RotateDialog		dialog				= null;
	private ListView	imageSizesListView	= null;

	private String[]	mEntries;
	private String[]	mEntryValues;

	private int			mClickedDialogEntryIndex;

	Context				context;

	public ImageSizeQuickSetting(Context context)
	{
		this.context = context;
	}

	public void showDialog()
	{
		int currentIdx = -1;

		String opt1 = MainScreen.sImageSizeRearPref;
		String opt2 = MainScreen.sImageSizeFrontPref;

		final String modeId = PluginManager.getInstance().getActiveModeID();
		if (modeId.equals("panorama_augmented"))
		{
			opt1 = MainScreen.sImageSizePanoramaBackPref;
			opt2 = MainScreen.sImageSizePanoramaFrontPref;
			PanoramaAugmentedCapturePlugin.onDefaultSelectResolutons();
			currentIdx = PanoramaAugmentedCapturePlugin.prefResolution;
			mEntries = PanoramaAugmentedCapturePlugin.getResolutionspicturenameslist().toArray(
					new String[PanoramaAugmentedCapturePlugin.getResolutionspicturenameslist().size()]);
			mEntryValues = PanoramaAugmentedCapturePlugin.getResolutionspictureidxeslist().toArray(
					new String[PanoramaAugmentedCapturePlugin.getResolutionspictureidxeslist().size()]);
		} else if (modeId.equals("nightmode") || modeId.equals("multishot"))
		{
			opt1 = MainScreen.sImageSizeMultishotBackPref;
			opt2 = MainScreen.sImageSizeMultishotFrontPref;
			currentIdx = Integer.parseInt(CameraController.MultishotResolutionsIdxesList.get(MainScreen
					.selectImageDimensionMultishot()));
			mEntries = CameraController.MultishotResolutionsNamesList
					.toArray(new String[CameraController.MultishotResolutionsNamesList.size()]);
			mEntryValues = CameraController.MultishotResolutionsIdxesList
					.toArray(new String[CameraController.MultishotResolutionsIdxesList.size()]);
		} else if (modeId.equals("video"))
		{
			opt1 = MainScreen.sImageSizeVideoBackPref;
			opt2 = MainScreen.sImageSizeVideoFrontPref;

			int idx = 0;
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
			currentIdx = Integer.parseInt(prefs.getString(CameraController.getCameraIndex() == 0 ? opt1 : opt2, "2"));

			CharSequence[] entriesTmp = new CharSequence[6];
			CharSequence[] entryValuesTmp = new CharSequence[6];
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), VideoCapturePlugin.QUALITY_4K))
			{
				entriesTmp[idx] = "4K";
				entryValuesTmp[idx] = "6";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_2160P))
			{
				entriesTmp[idx] = "2160p";
				entryValuesTmp[idx] = "2";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_1080P))
			{
				entriesTmp[idx] = "1080p";
				entryValuesTmp[idx] = "3";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_720P))
			{
				entriesTmp[idx] = "720p";
				entryValuesTmp[idx] = "4";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_480P))
			{
				entriesTmp[idx] = "480p";
				entryValuesTmp[idx] = "5";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_CIF))
			{
				entriesTmp[idx] = "352 x 288";
				entryValuesTmp[idx] = "1";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_QCIF))
			{
				entriesTmp[idx] = "176 x 144";
				entryValuesTmp[idx] = "0";
				idx++;
			}

			mEntries = new String[idx];
			mEntryValues = new String[idx];

			for (int i = 0; i < idx; i++)
			{
				mEntries[i] = (String) entriesTmp[i];
				mEntryValues[i] = (String) entryValuesTmp[i];
			}
		} else
		{
			opt1 = MainScreen.sImageSizeRearPref;
			opt2 = MainScreen.sImageSizeFrontPref;
			currentIdx = Integer.parseInt(MainScreen.getImageSizeIndex());

			if (currentIdx == -1)
			{
				currentIdx = 0;
			}

			mEntries = CameraController.getResolutionsNamesList().toArray(
					new String[CameraController.getResolutionsNamesList().size()]);
			mEntryValues = CameraController.getResolutionsIdxesList().toArray(
					new String[CameraController.getResolutionsIdxesList().size()]);
		}

		final String pref1 = opt1;
		final String pref2 = opt2;

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
		imageSizesListView = (ListView) dialog.findViewById(android.R.id.list);

		ListPreferenceAdapter adapter = new ListPreferenceAdapter(context, R.layout.simple_list_item_single_choice,
				android.R.id.text1, mEntries, mClickedDialogEntryIndex);

		imageSizesListView.setAdapter(adapter);
		imageSizesListView.setOnItemClickListener(new OnItemClickListener()
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
							.putString(CameraController.getCameraIndex() == 0 ? pref1 : pref2,
									String.valueOf(newValue.toString())).commit();

					MainScreen.getInstance().pauseMain();
					MainScreen.getInstance().resumeMain();
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
