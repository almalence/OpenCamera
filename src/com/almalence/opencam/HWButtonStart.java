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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class HWButtonStart extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		try
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
			boolean useHWShutterButtonLauncher = prefs.getBoolean("useHWShutterButtonLauncher", false);

			if (!useHWShutterButtonLauncher)
				return;

			abortBroadcast();

			Intent startActivity = new Intent();
			startActivity.setClass(context, MainScreen.class);
			startActivity.setAction(MainScreen.class.getName());
			startActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			context.startActivity(startActivity);
		} catch (Exception e)
		{
		}
	}
}
