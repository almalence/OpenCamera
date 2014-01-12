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

/***
Preference activity class - manages preferences
***/

public class Preferences extends PreferenceActivity 
{
	static public PreferenceActivity thiz;
	// Called only on Honeycomb and later
	//loading headers for common and plugins
	@Override
	public void onBuildHeaders(List<Header> target) 
	{
		thiz=this;
		loadHeadersFromResource(R.xml.preferences_headers, target);

		// <!-- -+-
		if (MainScreen.thiz.showUnlock)
		{
			MainScreen.thiz.showUnlock=false;
			startWithFragment("com.almalence.opencam.FragmentUpgrade", null, null, 0);
		}
		//-+- -->
	}
	
	static public void closePrefs()
	{
		thiz.finish();
	}
}
