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
 package com.almalence.opencam_plus;
 +++ --> */
// <!-- -+-
package com.almalence.opencam.ui;

//-+- -->

import java.util.ArrayList;
import java.util.List;

/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.PluginType;
 import com.almalence.opencam_plus.R;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginType;
import com.almalence.opencam.R;
//-+- -->

import com.almalence.ui.RotateDialog;
import com.almalence.ui.RotateImageView;
import com.almalence.ui.RotateLayout;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.Switch;

public class SelfTimer
{
	private RotateImageView	timeLapseButton	= null;
	private SelfTimerDialog	dialog			= null;
	boolean					swChecked		= false;
	String[]				stringInterval	= { "3", "5", "10", "15", "30", "60" };

	public void selfTimerDialog()
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		int interval = prefs.getInt(MainScreen.sDelayedCaptureIntervalPref, 0);
		swChecked = prefs.getBoolean(MainScreen.sSWCheckedPref, false);

		dialog = new SelfTimerDialog(MainScreen.getInstance());
		dialog.setContentView(R.layout.selftimer_dialog);
		final Button bSet = (Button) dialog.findViewById(R.id.button1);
		final NumberPicker np = (NumberPicker) dialog.findViewById(R.id.numberPicker1);
		np.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		np.setMaxValue(5);
		np.setMinValue(0);
		np.setValue(interval);
		np.setDisplayedValues(stringInterval);
		np.setWrapSelectorWheel(false);

		final CheckBox flashCheckbox = (CheckBox) dialog.findViewById(R.id.flashCheckbox);
		boolean flash = prefs.getBoolean(MainScreen.sDelayedFlashPref, false);
		flashCheckbox.setChecked(flash);

		final CheckBox soundCheckbox = (CheckBox) dialog.findViewById(R.id.soundCheckbox);
		boolean sound = prefs.getBoolean(MainScreen.sDelayedSoundPref, false);
		soundCheckbox.setChecked(sound);

		final Switch sw = (Switch) dialog.findViewById(R.id.selftimer_switcher);

		// disable/enable controls in dialog
		sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (!sw.isChecked())
				{
					np.setEnabled(false);
					flashCheckbox.setEnabled(false);
					soundCheckbox.setEnabled(false);
					swChecked = false;
				} else
				{
					np.setEnabled(true);
					flashCheckbox.setEnabled(true);
					soundCheckbox.setEnabled(true);
					swChecked = true;
					bSet.setEnabled(true);
				}
			}
		});

		// disable control in dialog by default
		if (!swChecked)
		{
			sw.setChecked(false);
			flashCheckbox.setEnabled(false);
			soundCheckbox.setEnabled(false);
			np.setEnabled(false);
			bSet.setEnabled(false);
		} else
		{
			np.setEnabled(true);
			flashCheckbox.setEnabled(true);
			soundCheckbox.setEnabled(true);
			bSet.setEnabled(true);
			sw.setChecked(true);
		}

		// set button in dialog pressed
		bSet.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				dialog.dismiss();
				int interval = 0;
				Editor prefsEditor = prefs.edit();

				if (swChecked)
					interval = np.getValue();
				else
					interval = 0;
				int real_int = Integer.parseInt(stringInterval[np.getValue()]);
				prefsEditor.putBoolean(MainScreen.sSWCheckedPref, swChecked);
				if (swChecked)
					prefsEditor.putInt(MainScreen.sDelayedCapturePref, real_int);
				else
				{
					prefsEditor.putInt(MainScreen.sDelayedCapturePref, 0);
					real_int = 0;
				}
				prefsEditor.putBoolean(MainScreen.sDelayedFlashPref, flashCheckbox.isChecked());
				prefsEditor.putBoolean(MainScreen.sDelayedSoundPref, soundCheckbox.isChecked());
				prefsEditor.putInt(MainScreen.sDelayedCaptureIntervalPref, interval);
				prefsEditor.commit();

				updateTimelapseButton(real_int);
			}
		});
		dialog.show();
	}

	public void addSelfTimerControl(boolean needToShow)
	{
		View selfTimerControl = null;
		// Calculate right sizes for plugin's controls
		DisplayMetrics metrics = new DisplayMetrics();
		MainScreen.getInstance().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		float screenDensity = metrics.density;

		int iIndicatorSize = (int) (MainScreen.getMainContext().getResources().getInteger(R.integer.infoControlHeight) * screenDensity);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(iIndicatorSize, iIndicatorSize);
		int topMargin = MainScreen.getInstance().findViewById(R.id.paramsLayout).getHeight()
				+ (int) MainScreen.getInstance().getResources().getDimension(R.dimen.viewfinderViewsMarginTop);
		params.setMargins((int) (2 * MainScreen.getGUIManager().getScreenDensity()), topMargin, 0, 0);

		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout2)).requestLayout();

		LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
		selfTimerControl = inflator.inflate(R.layout.selftimer_capture_layout, null, false);
		selfTimerControl.setVisibility(View.VISIBLE);

		MainScreen.getGUIManager().removeViews(selfTimerControl, R.id.specialPluginsLayout2);

		if (!needToShow
				|| !PluginManager.getInstance().getActivePlugins(PluginType.Capture).get(0).delayedCaptureSupported())
		{
			return;
		}

		timeLapseButton = (RotateImageView) selfTimerControl.findViewById(R.id.buttonSelftimer);

		timeLapseButton.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				selfTimerDialog();
			}

		});

		List<View> specialView2 = new ArrayList<View>();
		RelativeLayout specialLayout2 = (RelativeLayout) MainScreen.getInstance().findViewById(
				R.id.specialPluginsLayout2);
		for (int i = 0; i < specialLayout2.getChildCount(); i++)
			specialView2.add(specialLayout2.getChildAt(i));

		params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.height = (int) MainScreen.getInstance().getResources().getDimension(R.dimen.videobuttons_size);

		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout2)).addView(selfTimerControl,
				params);

		selfTimerControl.setLayoutParams(params);
		selfTimerControl.requestLayout();

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout2)).requestLayout();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		int delayInterval = prefs.getInt(MainScreen.sDelayedCapturePref, 0);
		updateTimelapseButton(delayInterval);
	}

	private void updateTimelapseButton(int delayInterval)
	{
		switch (delayInterval)
		{
		case 0:
			if (swChecked)
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer_controlcative);
			else
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer_control);
			break;
		case 3:
			if (swChecked)
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer3_controlcative);
			else
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer3_control);
			break;
		case 5:
			if (swChecked)
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer5_controlcative);
			else
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer5_control);
			break;
		case 10:
			if (swChecked)
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer10_controlcative);
			else
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer10_control);
			break;
		case 15:
			if (swChecked)
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer15_controlcative);
			else
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer15_control);
			break;
		case 30:
			if (swChecked)
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer30_controlcative);
			else
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer30_control);
			break;
		case 60:
			if (swChecked)
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer60_controlcative);
			else
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer60_control);
			break;
		default:
			break;
		}
	}

	public void setOrientation()
	{
		if (timeLapseButton != null)
		{
			timeLapseButton.setOrientation(AlmalenceGUI.mDeviceOrientation);
			timeLapseButton.invalidate();
			timeLapseButton.requestLayout();
		}

		if (dialog != null)
		{
			dialog.setRotate(AlmalenceGUI.mDeviceOrientation);
		}
	}

	protected class SelfTimerDialog extends RotateDialog
	{
		public SelfTimerDialog(Context context)
		{
			super(context);
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}

		@Override
		public void setRotate(int degree)
		{
			degree = degree >= 0 ? degree % 360 : degree % 360 + 360;

			if (degree == currentOrientation)
			{
				return;
			}
			currentOrientation = degree;

			RotateLayout r = (RotateLayout) findViewById(R.id.rotateLayout);
			r.setAngle(degree);
			r.requestLayout();
			r.invalidate();

		}
	}
}