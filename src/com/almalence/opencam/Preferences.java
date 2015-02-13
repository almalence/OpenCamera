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
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

/***
 * Preference activity class - manages preferences
 ***/

public class Preferences extends PreferenceActivity
{
	public static PreferenceActivity	thiz;

	int									cameraId;

	int[]								preview_widths;
	int[]								preview_heights;
	int[]								video_widths;
	int[]								video_heights;

	int[]								widths;
	int[]								heights;
	boolean								supports_video_stabilization;
	String[]							flash_values;
	String[]							focus_values;
	String[]							scene_modes_values;
	String[]							white_balances_values;
	String[]							isos;

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

		final Bundle bundle = getIntent().getExtras();
		cameraId = bundle.getInt("cameraId");

		preview_widths = bundle.getIntArray("preview_widths");
		preview_heights = bundle.getIntArray("preview_heights");
		video_widths = bundle.getIntArray("video_widths");
		video_heights = bundle.getIntArray("video_heights");

		widths = bundle.getIntArray("resolution_widths");
		heights = bundle.getIntArray("resolution_heights");
		supports_video_stabilization = bundle.getBoolean("supports_video_stabilization");
		flash_values = bundle.getStringArray("flash_values");
		focus_values = bundle.getStringArray("focus_values");
		scene_modes_values = bundle.getStringArray("scene_modes");
		white_balances_values = bundle.getStringArray("white_balances");
		isos = bundle.getStringArray("isos");
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

			View view = getLayoutInflater().inflate(R.layout.about_dialog, null);
			builder.setTitle(R.string.Pref_About_Title).setView(view).setCancelable(true);
			AlertDialog alert = builder.create();
			alert.show();

			ImageView img = (ImageView) alert.findViewById(R.id.about_fb_logo);
			img.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);
					intent.addCategory(Intent.CATEGORY_BROWSABLE);
					intent.setData(Uri.parse("https://www.facebook.com/abettercam"));
					startActivity(intent);
				}
			});

			img = (ImageView) alert.findViewById(R.id.about_gplus_logo);
			img.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);
					intent.addCategory(Intent.CATEGORY_BROWSABLE);
					intent.setData(Uri.parse("https://plus.google.com/112729683723267883775"));
					startActivity(intent);
				}
			});

			img = (ImageView) alert.findViewById(R.id.about_youtube_logo);
			img.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);
					intent.addCategory(Intent.CATEGORY_BROWSABLE);
					intent.setData(Uri.parse("http://www.youtube.com/watch?v=s6AusctWugg"));
					startActivity(intent);
				}
			});
		}

		if (header.id == R.id.camera_parameters_header)
		{
			showCameraParameters();
		}
	}

	private void showCameraParameters()
	{
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setTitle(R.string.Pref_CameraParameters_Title);
		final StringBuilder about_string = new StringBuilder();
		String version = "UNKNOWN_VERSION";
		int version_code = -1;
		try
		{
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = pInfo.versionName;
			version_code = pInfo.versionCode;
		} catch (NameNotFoundException e)
		{
			e.printStackTrace();
		}
		about_string.append("\nAndroid API version: ");
		about_string.append(Build.VERSION.SDK_INT);
		about_string.append("\nDevice manufacturer: ");
		about_string.append(Build.MANUFACTURER);
		about_string.append("\nDevice model: ");
		about_string.append(Build.MODEL);
		about_string.append("\nDevice code-name: ");
		about_string.append(Build.HARDWARE);
		about_string.append("\nDevice variant: ");
		about_string.append(Build.DEVICE);
		{
			ActivityManager activityManager = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
			about_string.append("\nStandard max heap (MB): ");
			about_string.append(activityManager.getMemoryClass());
			about_string.append("\nLarge max heap (MB): ");
			about_string.append(activityManager.getLargeMemoryClass());
		}
		{
			Point display_size = new Point();
			Display display = getWindowManager().getDefaultDisplay();
			display.getSize(display_size);
			about_string.append("\nDisplay size: ");
			about_string.append(display_size.x);
			about_string.append("x");
			about_string.append(display_size.y);
		}

		if (preview_widths != null && preview_heights != null)
		{
			about_string.append("\nPreview resolutions: ");
			for (int i = 0; i < preview_widths.length; i++)
			{
				if (i > 0)
				{
					about_string.append(", ");
				}
				about_string.append(preview_widths[i]);
				about_string.append("x");
				about_string.append(preview_heights[i]);
			}
		}

		if (widths != null && heights != null)
		{
			about_string.append("\nPhoto resolutions: ");
			for (int i = 0; i < widths.length; i++)
			{
				if (i > 0)
				{
					about_string.append(", ");
				}
				about_string.append(widths[i]);
				about_string.append("x");
				about_string.append(heights[i]);
			}
		}

		if (video_widths != null && video_heights != null)
		{
			about_string.append("\nVideo resolutions: ");
			for (int i = 0; i < video_widths.length; i++)
			{
				if (i > 0)
				{
					about_string.append(", ");
				}
				about_string.append(video_widths[i]);
				about_string.append("x");
				about_string.append(video_heights[i]);
			}
		}

		about_string.append("\nVideo stabilization: ");
		about_string.append(supports_video_stabilization ? "true" : "false");

		about_string.append("\nFlash modes: ");
		if (flash_values != null && flash_values.length > 0)
		{
			for (int i = 0; i < flash_values.length; i++)
			{
				if (i > 0)
				{
					about_string.append(", ");
				}
				about_string.append(flash_values[i]);
			}
		} else
		{
			about_string.append("None");
		}

		about_string.append("\nFocus modes: ");
		if (focus_values != null && focus_values.length > 0)
		{
			for (int i = 0; i < focus_values.length; i++)
			{
				if (i > 0)
				{
					about_string.append(", ");
				}
				about_string.append(focus_values[i]);
			}
		} else
		{
			about_string.append("None");
		}

		about_string.append("\nScene modes: ");
		if (scene_modes_values != null && scene_modes_values.length > 0)
		{
			for (int i = 0; i < scene_modes_values.length; i++)
			{
				if (i > 0)
				{
					about_string.append(", ");
				}
				about_string.append(scene_modes_values[i]);
			}
		} else
		{
			about_string.append("None");
		}

		about_string.append("\nWhite balances: ");
		if (white_balances_values != null && white_balances_values.length > 0)
		{
			for (int i = 0; i < white_balances_values.length; i++)
			{
				if (i > 0)
				{
					about_string.append(", ");
				}
				about_string.append(white_balances_values[i]);
			}
		} else
		{
			about_string.append("None");
		}

		about_string.append("\nISOs: ");
		if (isos != null && isos.length > 0)
		{
			for (int i = 0; i < isos.length; i++)
			{
				if (i > 0)
				{
					about_string.append(", ");
				}
				about_string.append(isos[i]);
			}
		} else
		{
			about_string.append("None");
		}

		String save_location = MainScreen.getSaveToPath();
		about_string.append("\nSave Location: " + save_location);

		alertDialog.setMessage(about_string);
		alertDialog.setPositiveButton(R.string.Pref_CameraParameters_Ok, null);
		alertDialog.setNegativeButton(R.string.Pref_CameraParameters_CopyToClipboard, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{
				ClipboardManager clipboard = (ClipboardManager) getSystemService(
						Activity.CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("OpenCamera About", about_string);
				clipboard.setPrimaryClip(clip);
			}
		});
		alertDialog.show();
	}
}
