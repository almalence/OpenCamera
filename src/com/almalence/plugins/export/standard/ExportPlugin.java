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

package com.almalence.plugins.export.standard;

import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;

/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginExport;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.R;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginExport;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
//-+- -->

import com.almalence.ui.RotateImageView;
import com.almalence.util.MLocation;

/***
 * Implements simple export plugin - saves image to disc to specified location
 * in specified format and with specified pattern name
 ***/

public class ExportPlugin extends PluginExport
{
	boolean					should_save						= false;
	private RotateImageView	gpsInfoImage;

	boolean					isResultFromProcessingPlugin	= false;

	private boolean			useGeoTaggingPrefExport;

	boolean					isFirstGpsFix					= true;

	private long			sessionID						= 0;

	public ExportPlugin()
	{
		super("com.almalence.plugins.export", R.xml.preferences_export_export, 0, 0, null);
	}

	@Override
	public void onExportActive(long SessionID)
	{
		sessionID = SessionID;
		getPrefs();

		isResultFromProcessingPlugin = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem(
				"ResultFromProcessingPlugin" + sessionID));

		PluginManager.getInstance().saveResultPicture(sessionID);
	}

	private void getPrefs()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		useGeoTaggingPrefExport = prefs.getBoolean("useGeoTaggingPrefExport", false);
	}

	@Override
	public void onResume()
	{
		getPrefs();

		isFirstGpsFix = true;

		clearInfoViews();

		if (useGeoTaggingPrefExport)
		{
			View v = LayoutInflater.from(MainScreen.getMainContext()).inflate(R.layout.plugin_export_gps, null);
			gpsInfoImage = (RotateImageView) v.findViewById(R.id.gpsInfoImage);
			gpsInfoImage.setImageDrawable(MainScreen.getMainContext().getResources().getDrawable(R.drawable.gps_off));

			addInfoView(gpsInfoImage);

			MLocation.subsribe(MainScreen.getInstance());
			MLocation.lm.addGpsStatusListener(new GpsStatus.Listener()
			{

				@Override
				public void onGpsStatusChanged(int event)
				{

					ExportPlugin.this.ShowGPSStatus(event);
				}
			});
		} else
		{
			View v = LayoutInflater.from(MainScreen.getMainContext()).inflate(R.layout.plugin_export_gps, null);
			gpsInfoImage = (RotateImageView) v.findViewById(R.id.gpsInfoImage);
			gpsInfoImage.setVisibility(View.INVISIBLE);
		}
	}

	public void ShowGPSStatus(int event)
	{
		switch (event)
		{
		case GpsStatus.GPS_EVENT_STARTED:
			gpsInfoImage
					.setImageDrawable(MainScreen.getMainContext().getResources().getDrawable(R.drawable.gps_search));
			gpsInfoImage.setVisibility(View.VISIBLE);
			break;
		case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
			if (!isFirstGpsFix)
				return;
			gpsInfoImage.setImageDrawable(MainScreen.getMainContext().getResources().getDrawable(R.drawable.gps_found));
			gpsInfoImage.setVisibility(View.VISIBLE);
			isFirstGpsFix = false;
			break;
		default:
			break;
		}
	}

	@Override
	public void onDestroy()
	{
		if (useGeoTaggingPrefExport)
		{
			MLocation.unsubscribe();
		}
	}

}