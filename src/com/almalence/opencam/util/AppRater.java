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

package com.almalence.opencam.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.almalence.opencam.MainScreen;
import com.almalence.opencam.R;

public class AppRater
{
	private final static int DAYS_UNTIL_PROMPT = 0;
	private final static int LAUNCHES_UNTIL_PROMPT = 20;

	public static void app_launched(Activity mContext)
	{
		SharedPreferences prefs = mContext.getSharedPreferences("apprater", 0);
		if (prefs.getBoolean("dontshowagain", false))
		{
			return;
		}

		SharedPreferences.Editor editor = prefs.edit();

		// Increment launch counter
		long launch_count = prefs.getLong("launch_count", 0) + 1;
		editor.putLong("launch_count", launch_count);

		// Get date of first launch
		Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
		if (date_firstLaunch == 0)
		{
			date_firstLaunch = System.currentTimeMillis();
			editor.putLong("date_firstlaunch", date_firstLaunch);
		}

		editor.commit();
	}
	
	public static boolean showRateDialogIfNeeded(final Activity mContext)
	{
		final SharedPreferences prefs = mContext.getSharedPreferences("apprater", 0);
		if (prefs.getBoolean("dontshowagain", false))
		{
			return false;
		}
		
		final long launch_count = prefs.getLong("launch_count", 0) + 1;
		final Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
		
		if (launch_count >= LAUNCHES_UNTIL_PROMPT)
		{
			if (System.currentTimeMillis() >= date_firstLaunch
					+ (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000))
			{
				showRateDialog(mContext, prefs);
				
				return true;
			}
		}
		
		return false;
	}

	private static void showRateDialog(final Activity mContext, final SharedPreferences prefs)
	{
		final String APP_TITLE = mContext.getResources().getString(R.string.app_name);
		
		final float density = mContext.getResources().getDisplayMetrics().density;

		LinearLayout ll = new LinearLayout(mContext);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding((int)(10 * density), (int)(10 * density), (int)(10 * density), (int)(10 * density));
		
		TextView tv = new TextView(mContext);
		tv.setText("If you enjoy using "
				+ APP_TITLE + ", please take a moment to rate it.\n\nThanks for your support!");
		tv.setWidth((int)(250 * density));
		tv.setPadding((int)(4 * density), 0, (int)(4 * density), (int)(24 * density));
		ll.addView(tv);

		Button b1 = new Button(mContext);
		b1.setText("Rate " + APP_TITLE);
		ll.addView(b1);

		/*
		Button b2 = new Button(mContext);
		b2.setText("Remind me later");
		ll.addView(b2);
		*/

		Button b3 = new Button(mContext);
		b3.setText("No, thanks");
		ll.addView(b3);
		
		final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setView(ll);
		final AlertDialog dialog = builder.create();
		dialog.setOnCancelListener(new OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				mContext.finish();
			}
		});
		
		b1.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				MainScreen.CallStoreFree(mContext);
				
				if (prefs != null)
				{
					prefs.edit().putBoolean("dontshowagain", true).commit();
					
					mContext.finish();
				}
				
				dialog.dismiss();
			}
		});
		/*
		b2.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				dialog.dismiss();
				
				mContext.finish();
			}
		});
		*/
		b3.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (prefs != null)
				{
					prefs.edit().putBoolean("dontshowagain", true).commit();
					
					mContext.finish();
				}
				
				dialog.dismiss();
			}
		});
		
		dialog.show();
	}
}