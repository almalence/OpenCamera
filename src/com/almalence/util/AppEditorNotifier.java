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
	private static int	DAYS_UNTIL_PROMPT	= 10;

	public static void app_launched(Activity mContext)
	{
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
		final SharedPreferences prefs = mContext.getSharedPreferences("appeditornotifier", 0);
		// check if installed
		if (isABCEditorInstalled(MainScreen.getInstance()))
		{
			return false;
		}
		
		DAYS_UNTIL_PROMPT = prefs.getInt("days_until_prompt", 10);
		Long date_firstLaunch = prefs.getLong("date_firstlaunch_editornotifier", 0);
		if (date_firstLaunch == 0)
		{
			date_firstLaunch = System.currentTimeMillis();
			prefs.edit().putLong("date_firstlaunch_editornotifier", date_firstLaunch).commit();
			showEditorNotifierDialog(mContext, prefs);
			return true;
		}

		if (System.currentTimeMillis() < date_firstLaunch + Long.valueOf((long)DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000))
		{
			MainScreen.getInstance().guiManager.openGallery(true);
			return true;
		}

		showEditorNotifierDialog(mContext, prefs);
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
		tv.setText(MainScreen.getAppResources().getString(R.string.editorAdvText));
		tv.setWidth((int) (250 * density));
		tv.setPadding((int) (4 * density), 0, (int) (4 * density), (int) (24 * density));
		ll.addView(tv);

		Button bInstall = new Button(mContext);
		bInstall.setText(MainScreen.getAppResources().getString(R.string.editorInstallText));
		ll.addView(bInstall);

		Button bLater = new Button(mContext);
		bLater.setText(MainScreen.getAppResources().getString(R.string.editorNoText));
		ll.addView(bLater);

		final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setView(ll);
		final AlertDialog dialog = builder.create();
		dialog.setOnCancelListener(new OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				MainScreen.getInstance().guiManager.openGallery(true);
			}
		});

		bInstall.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				MainScreen.callStoreInstall(mContext, "com.almalence.opencameditor");
				prefs.edit().putInt("days_until_prompt", 999).commit();
				dialog.dismiss();
			}
		});

		bLater.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				dialog.dismiss();
				MainScreen.getInstance().guiManager.openGallery(true);

				if (DAYS_UNTIL_PROMPT <= 10)
					DAYS_UNTIL_PROMPT = 30;
				else if (DAYS_UNTIL_PROMPT == 30)
					DAYS_UNTIL_PROMPT = 999;

				prefs.edit().putInt("days_until_prompt", DAYS_UNTIL_PROMPT).commit();
			}
		});

		dialog.show();
	}
}
