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
package com.almalence.opencam;

//-+- -->

import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;

/***
 * Preference activity class - manages preferences
 ***/

public class Preferences extends PreferenceActivity
{
	public static PreferenceActivity	thiz;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// On some devices app crashes on start, because of MainScreen.thiz is
		// null.
		// If MainScreen.thiz is null, we need to start MainScreen to initialize
		// it, before starting Preferences.
		if (MainScreen.thiz == null)
		{
			Intent intent = new Intent(this, MainScreen.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			finish();
		}
	}

	// Called only on Honeycomb and later
	// loading headers for common and plugins
	@Override
	public void onResume()
	{
		super.onResume();
		thiz = this;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		boolean MaxScreenBrightnessPreference = prefs.getBoolean("maxScreenBrightnessPref", false);
		setScreenBrightness(MaxScreenBrightnessPreference);
	}

	@Override
	public void onBuildHeaders(List<Header> target)
	{
		thiz = this;
		loadHeadersFromResource(R.xml.preferences_headers, target);

	}

	public static void closePrefs()
	{
		thiz.finish();
	}

	public static void setScreenBrightness(boolean setMax)
	{
		try
		{
			Window window = thiz.getWindow();
			WindowManager.LayoutParams layoutpars = window.getAttributes();

			// Set the brightness of this window
			if (setMax)
				layoutpars.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
			else
				layoutpars.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;

			// Apply attribute changes to this window
			window.setAttributes(layoutpars);
		} catch (Exception e)
		{

		}
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	protected boolean isValidFragment(String fragmentName)
	{
		return true;
	}

	@Override
	public void onHeaderClick(Header header, int position)
	{
		super.onHeaderClick(header, position);
		if (header.id == R.id.about_header)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.Pref_About_Title)
					.setView(R.layout.about_dialog)
					.setCancelable(true);
			AlertDialog alert = builder.create();
			alert.show();
		}
	}
}
