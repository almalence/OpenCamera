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

package com.almalence.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

//<!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.R;

//-+- -->
/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.R;
 +++ --> */

public class AppEditorNotifier
{
//	private static final int	DAYS_UNTIL_PROMPT		= 5;
//	private static final int	LAUNCHES_UNTIL_PROMPT	= 30;

	public static void app_launched(Activity mContext)
	{
		SharedPreferences prefs = mContext.getSharedPreferences("appeditornotifier", 0);
		if (prefs.getBoolean("dontshowagaineditornotifier", false))
		{
			return;
		}

//		SharedPreferences.Editor editor = prefs.edit();
//
//		// Increment launch counter
//		long launch_count = prefs.getLong("launch_count_editornotifier", 0) + 1;
//		editor.putLong("launch_count_editornotifier", launch_count);
//
//		// Get date of first launch
//		Long date_firstLaunch = prefs.getLong("date_firstlaunch_editornotifier", 0);
//		if (date_firstLaunch == 0)
//		{
//			date_firstLaunch = System.currentTimeMillis();
//			editor.putLong("date_firstlaunch_editornotifier", date_firstLaunch);
//		}
//
//		editor.commit();
	}

	public static final boolean isABCEditorInstalled(Activity activity)
	{
		try
		{
			activity.getPackageManager().getInstallerPackageName("com.almalence.opencameditor");
		} catch (IllegalArgumentException e)
		{
			return false;
		}

		return true;
	}

	public static boolean showEditorNotifierDialogIfNeeded(final Activity mContext)
	{
		// check if installed
		if (isABCEditorInstalled(MainScreen.getInstance()))
		{
			return false;
		}

		final SharedPreferences prefs = mContext.getSharedPreferences("appeditornotifier", 0);
		if (prefs.getBoolean("dontshowagaineditornotifier", false))
		{
			return false;
		}

//		final long launch_count = prefs.getLong("launch_count_editornotifier", 0) + 1;
//		final Long date_firstLaunch = prefs.getLong("date_firstlaunch_editornotifier", 0);
//
//		if (launch_count >= LAUNCHES_UNTIL_PROMPT)
//		{
//			if (System.currentTimeMillis() >= date_firstLaunch + (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000))
//			{
//				showEditorNotifierDialog(mContext, prefs);
//
//				return true;
//			}
//		}

		return true;
	}

	private static void showEditorNotifierDialog(final Activity mContext, final SharedPreferences prefs)
	{
		final float density = mContext.getResources().getDisplayMetrics().density;

		LinearLayout ll = new LinearLayout(mContext);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding((int) (10 * density), (int) (10 * density), (int) (10 * density), (int) (10 * density));

		ImageView img = new ImageView(mContext);
		img.setImageResource(R.drawable.editor_adv);
		ll.addView(img);

		TextView tv = new TextView(mContext);
		tv.setText(MainScreen.getInstance().getResources().getString(R.string.editorAdvText));
		tv.setWidth((int) (250 * density));
		tv.setPadding((int) (4 * density), 0, (int) (4 * density), (int) (24 * density));
		ll.addView(tv);

		Button b1 = new Button(mContext);
		b1.setText(MainScreen.getInstance().getResources().getString(R.string.widgetInstallText));
		ll.addView(b1);

		Button b3 = new Button(mContext);
		b3.setText(MainScreen.getInstance().getResources().getString(R.string.widgetNoText));
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
				MainScreen.callStoreInstall(mContext, "com.almalence.opencameditor");

				if (prefs != null)
				{
					prefs.edit().putBoolean("dontshowagaineditornotifier", true).commit();

					mContext.finish();
				}

				dialog.dismiss();
			}
		});

		b3.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (prefs != null)
				{
					prefs.edit().putBoolean("dontshowagaineditornotifier", true).commit();

					mContext.finish();
				}

				dialog.dismiss();
			}
		});

		dialog.show();
	}
}