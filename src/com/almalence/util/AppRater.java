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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.LayerDrawable;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.almalence.opencam.MainScreen;
import com.almalence.opencam.R;

public class AppRater
{
	private static final int	DAYS_UNTIL_PROMPT		= 0;
	private static final int	LAUNCHES_UNTIL_PROMPT	= 15;

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
		// temp research - disable rater for 4.03 - 4.0.4
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
			return false;

		final SharedPreferences prefs = mContext.getSharedPreferences("apprater", 0);
		if (prefs.getBoolean("dontshowagain", false))
		{
			return false;
		}

		final long launch_count = prefs.getLong("launch_count", 0) + 1;
		final Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);

		if (launch_count >= LAUNCHES_UNTIL_PROMPT)
		{
			if (System.currentTimeMillis() >= date_firstLaunch + Long.valueOf((long)DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000))
			{
				showRateDialog(mContext, prefs);

				return true;
			}
		}

		return false;
	}

	private static void showRateDialog(final Activity mContext, final SharedPreferences prefs)
	{
		final float density = mContext.getResources().getDisplayMetrics().density;

		LinearLayout ll = new LinearLayout(mContext);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding((int) (10 * density), (int) (10 * density), (int) (10 * density), (int) (10 * density));

		TextView tv = new TextView(mContext);
		tv.setText(mContext.getResources().getString(R.string.raterMain));
		tv.setWidth((int) (250 * density));
		tv.setPadding((int) (4 * density), 0, (int) (4 * density), (int) (24 * density));
		ll.addView(tv);

		// rating bar
		final RatingBar ratingBar = new RatingBar(mContext);
		ratingBar.setNumStars(5);
		ratingBar.setStepSize(1);
		LayerDrawable stars = (LayerDrawable) ratingBar.getProgressDrawable();
		stars.getDrawable(0).setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
		stars.getDrawable(2).setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_ATOP);

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.CENTER_HORIZONTAL;
		params.setMargins(0, 5, 0, 10);
		ratingBar.setLayoutParams(params);
		ll.addView(ratingBar);

		Button b3 = new Button(mContext);
		b3.setText(mContext.getResources().getString(R.string.raterNo));
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

		ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener()
		{
			public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser)
			{
				if (prefs != null)
				{
					prefs.edit().putBoolean("dontshowagain", true).commit();
				}
				dialog.dismiss();

				if (rating >= 4)
				{
					mContext.finish();
					MainScreen.callStoreFree(mContext);
				} else
					contactSupport();
			}
		});

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

	private static void contactSupport()
	{
		final float density = MainScreen.getAppResources().getDisplayMetrics().density;

		LinearLayout ll = new LinearLayout(MainScreen.getInstance());
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding((int) (10 * density), (int) (10 * density), (int) (10 * density), (int) (10 * density));

		TextView tv = new TextView(MainScreen.getInstance());
		tv.setText(MainScreen.getAppResources().getString(R.string.raterSendReview));
		tv.setWidth((int) (250 * density));
		tv.setPadding((int) (4 * density), 0, (int) (4 * density), (int) (24 * density));
		ll.addView(tv);

		Button b1 = new Button(MainScreen.getInstance());
		b1.setText(MainScreen.getAppResources().getString(R.string.raterSend));
		ll.addView(b1);

		Button b2 = new Button(MainScreen.getInstance());
		b2.setText(MainScreen.getAppResources().getString(R.string.raterNo));
		ll.addView(b2);

		final AlertDialog.Builder builder = new AlertDialog.Builder(MainScreen.getInstance());
		builder.setView(ll);
		final AlertDialog dialog = builder.create();
		dialog.setOnCancelListener(new OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				MainScreen.getInstance().finish();
			}
		});

		b1.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				Intent intent = new Intent(Intent.ACTION_SENDTO);
				intent.setType("message/rfc822");
				intent.putExtra(Intent.EXTRA_SUBJECT, "A Better Camera inapp review");
				intent.setData(Uri.parse("mailto:support@abc.almalence.com"));
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				MainScreen.getInstance().startActivity(intent);
				dialog.dismiss();
				MainScreen.getInstance().finish();
			}
		});
		b2.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				dialog.dismiss();
				MainScreen.getInstance().finish();
			}
		});
		dialog.show();
	}

}