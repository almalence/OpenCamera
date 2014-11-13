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

import java.util.ArrayList;
import java.util.List;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.almalence.ui.RotateImageView;
//<!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginType;
import com.almalence.opencam.R;
//-+- -->

/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.PluginType;
 import com.almalence.opencam_plus.R;
 +++ --> */

public class SelfTimerAndPhotoTimeLapse
{
	private TextView					timeLapseCount				= null;
	private RotateImageView				timeLapseButton				= null;
	private SelfTimerAndTimeLapseDialog	dialog						= null;
	boolean								swTimerChecked				= false;
	boolean								swTimeLapseChecked			= false;
	int									timeLapseInterval;
	int									timeLapseMeasurementVal;
	String[]							stringTimerInterval			= { "3", "5", "10", "15", "30", "60" };
	public static String[]				stringTimelapseInterval		= { "3", "5", "10", "15", "30", "60" };
	String[]							stringTimelapseMeasurement	= { "seconds", "minutes", "hours" };
	CheckBox							flashCheckbox;
	CheckBox							soundCheckbox;
	NumberPicker						npTimeLapse;
	NumberPicker						npTimeLapseMeasurment;
	NumberPicker						npTimer;

	public void selfTimerInitDialog()
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		int interval = prefs.getInt(MainScreen.sDelayedCaptureIntervalPref, 0);
		swTimerChecked = prefs.getBoolean(MainScreen.sSWCheckedPref, false);

		npTimer = (NumberPicker) dialog.findViewById(R.id.numberPicker1);
		npTimer.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		npTimer.setMaxValue(5);
		npTimer.setMinValue(0);
		npTimer.setValue(interval);
		npTimer.setDisplayedValues(stringTimerInterval);
		npTimer.setWrapSelectorWheel(false);

		flashCheckbox = (CheckBox) dialog.findViewById(R.id.flashCheckbox);
		boolean flash = prefs.getBoolean(MainScreen.sDelayedFlashPref, false);
		flashCheckbox.setChecked(flash);

		soundCheckbox = (CheckBox) dialog.findViewById(R.id.soundCheckbox);
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
					// np.setEnabled(false);
					flashCheckbox.setEnabled(false);
					soundCheckbox.setEnabled(false);
					swTimerChecked = false;
				} else
				{
					// np.setEnabled(true);
					flashCheckbox.setEnabled(true);
					soundCheckbox.setEnabled(true);
					swTimerChecked = true;
				}
			}
		});

		npTimer.setOnScrollListener(new NumberPicker.OnScrollListener()
		{
			@Override
			public void onScrollStateChange(NumberPicker numberPicker, int scrollState)
			{
				sw.setChecked(true);
			}
		});

		// disable control in dialog by default
		if (!swTimerChecked)
		{
			sw.setChecked(false);
			flashCheckbox.setEnabled(false);
			soundCheckbox.setEnabled(false);
			// np.setEnabled(false);
		} else
		{
			// np.setEnabled(true);
			flashCheckbox.setEnabled(true);
			soundCheckbox.setEnabled(true);
			sw.setChecked(true);
		}
	}

	public void photoTimeLapseInitDialog()
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		timeLapseInterval = prefs.getInt(MainScreen.sPhotoTimeLapseCaptureIntervalPref, 0);
		timeLapseMeasurementVal = prefs.getInt(MainScreen.sPhotoTimeLapseCaptureIntervalMeasurmentPref, 0);

		swTimeLapseChecked = prefs.getBoolean(MainScreen.sPhotoTimeLapseActivePref, false);

		npTimeLapse = (NumberPicker) dialog.findViewById(R.id.photoTimeLapseInterval_numberPicker);
		npTimeLapse.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		npTimeLapse.setMaxValue(5);
		npTimeLapse.setMinValue(0);
		npTimeLapse.setValue(timeLapseInterval);
		npTimeLapse.setDisplayedValues(stringTimelapseInterval);
		npTimeLapse.setWrapSelectorWheel(false);

		npTimeLapseMeasurment = (NumberPicker) dialog.findViewById(R.id.photoTimeLapseMeasurment_numberPicker);
		npTimeLapseMeasurment.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		npTimeLapseMeasurment.setMaxValue(2);
		npTimeLapseMeasurment.setMinValue(0);
		npTimeLapseMeasurment.setValue(timeLapseMeasurementVal);
		npTimeLapseMeasurment.setDisplayedValues(stringTimelapseMeasurement);
		npTimeLapseMeasurment.setWrapSelectorWheel(false);

		final Switch sw = (Switch) dialog.findViewById(R.id.photoTimeLapseTitle_switcher);

		// disable/enable controls in dialog
		sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (!sw.isChecked())
				{
					swTimeLapseChecked = false;
				} else
				{
					swTimeLapseChecked = true;
				}
			}
		});

		npTimeLapse.setOnScrollListener(new NumberPicker.OnScrollListener()
		{
			@Override
			public void onScrollStateChange(NumberPicker numberPicker, int scrollState)
			{
				sw.setChecked(true);
			}
		});
		
		npTimeLapseMeasurment.setOnScrollListener(new NumberPicker.OnScrollListener()
		{
			@Override
			public void onScrollStateChange(NumberPicker numberPicker, int scrollState)
			{
				sw.setChecked(true);
			}
		});

		// disable control in dialog by default
		if (swTimeLapseChecked)
		{
			sw.setChecked(true);
		} else
		{
			sw.setChecked(false);
		}
	}

	public void initDismissDialog()
	{
		dialog.setOnDismissListener(new OnDismissListener()
		{
			@Override
			public void onDismiss(DialogInterface dialog)
			{
				final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen
						.getMainContext());

				// time lapse
				int intervalTimeLapse = 0;
				int intervalTimeLapseMeasurment = 0;
				Editor prefsEditor = prefs.edit();

				if (swTimeLapseChecked)
				{
					intervalTimeLapse = npTimeLapse.getValue();
					intervalTimeLapseMeasurment = npTimeLapseMeasurment.getValue();
				} else
				{
					intervalTimeLapse = 0;
					intervalTimeLapseMeasurment = 0;
				}
				prefsEditor.putBoolean(MainScreen.sPhotoTimeLapseActivePref, swTimeLapseChecked);
				prefsEditor.putInt(MainScreen.sPhotoTimeLapseCaptureIntervalPref, intervalTimeLapse);
				prefsEditor
						.putInt(MainScreen.sPhotoTimeLapseCaptureIntervalMeasurmentPref, intervalTimeLapseMeasurment);

				// timer
				int intervalTimer = 0;

				if (swTimerChecked)
					intervalTimeLapse = npTimer.getValue();
				else
					intervalTimeLapse = 0;
				int real_int = Integer.parseInt(stringTimerInterval[npTimer.getValue()]);
				prefsEditor.putBoolean(MainScreen.sSWCheckedPref, swTimerChecked);
				if (swTimerChecked)
					prefsEditor.putInt(MainScreen.sDelayedCapturePref, real_int);
				else
				{
					prefsEditor.putInt(MainScreen.sDelayedCapturePref, 0);
					real_int = 0;
				}
				prefsEditor.putBoolean(MainScreen.sDelayedFlashPref, flashCheckbox.isChecked());
				prefsEditor.putBoolean(MainScreen.sDelayedSoundPref, soundCheckbox.isChecked());
				prefsEditor.putInt(MainScreen.sDelayedCaptureIntervalPref, intervalTimeLapse);

				prefsEditor.commit();

				updateTimelapseButton(real_int);
			}
		});
	}

	public void selfTimerAndPtotoTimeLapseDialog()
	{
		dialog = new SelfTimerAndTimeLapseDialog(MainScreen.getInstance());
		selfTimerInitDialog();
		photoTimeLapseInitDialog();
		initDismissDialog();
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

		LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
		selfTimerControl = inflator.inflate(R.layout.selftimer_capture_layout, null, false);
		selfTimerControl.setVisibility(View.VISIBLE);

		MainScreen.getGUIManager().removeViews(selfTimerControl, R.id.specialPluginsLayout2);

		if (!needToShow
				|| !PluginManager.getInstance().getActivePlugins(PluginType.Capture).get(0).delayedCaptureSupported())
		{
			return;
		}

		timeLapseCount = (TextView) selfTimerControl.findViewById(R.id.timelapseCount);
		
		timeLapseButton = (RotateImageView) selfTimerControl.findViewById(R.id.buttonSelftimer);
		timeLapseButton.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				selfTimerAndPtotoTimeLapseDialog();
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

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		int delayInterval = prefs.getInt(MainScreen.sDelayedCapturePref, 0);
		
		updateTimelapseButton(delayInterval);
		updateTimelapseCount();
	}
	
	public void updateTimelapseCount() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		boolean photoTimeLapseActive = prefs.getBoolean(MainScreen.sPhotoTimeLapseActivePref, false);
		boolean photoTimeLapseIsRunning = prefs.getBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);

		if (photoTimeLapseActive && photoTimeLapseIsRunning) {
			timeLapseCount.setVisibility(View.VISIBLE);
			int count = prefs.getInt(MainScreen.sPhotoTimeLapseCount, 0);
			timeLapseCount.setText(String.valueOf(count));
			timeLapseButton.setVisibility(View.GONE);
		} else {
			timeLapseCount.setText(String.valueOf(0));
			timeLapseCount.setVisibility(View.GONE);
			timeLapseButton.setVisibility(View.VISIBLE);
		}
	}

	private void updateTimelapseButton(int delayInterval)
	{
		switch (delayInterval)
		{
		case 0:
			if (swTimerChecked)
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer_controlcative);
			else
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer_control);
			break;
		case 3:
			if (swTimerChecked)
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer3_controlcative);
			else
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer3_control);
			break;
		case 5:
			if (swTimerChecked)
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer5_controlcative);
			else
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer5_control);
			break;
		case 10:
			if (swTimerChecked)
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer10_controlcative);
			else
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer10_control);
			break;
		case 15:
			if (swTimerChecked)
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer15_controlcative);
			else
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer15_control);
			break;
		case 30:
			if (swTimerChecked)
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer30_controlcative);
			else
				timeLapseButton.setImageResource(R.drawable.gui_almalence_mode_selftimer30_control);
			break;
		case 60:
			if (swTimerChecked)
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
		
		if (timeLapseCount != null)
		{
			timeLapseCount.setRotation(-AlmalenceGUI.mDeviceOrientation);
			timeLapseCount.invalidate();
			timeLapseCount.requestLayout();
		}

		if (dialog != null)
		{
			dialog.setRotate(AlmalenceGUI.mDeviceOrientation);
		}
	}
}
