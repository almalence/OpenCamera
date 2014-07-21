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

import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;
import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;

/***
 * Preference activity class - manages preferences
 ***/

public class Preferences extends PreferenceActivity
{
	public static PreferenceActivity	thiz;

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
}
