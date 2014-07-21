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

import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class MLocation
{
	public static LocationManager	lm;

	public static void subsribe(Context context)
	{
		lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		// exceptions will be thrown if provider is not permitted.
		boolean gps_enabled = false;
		try
		{
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
		boolean network_enabled = false;
		try
		{
			network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}

		if (gps_enabled)
		{
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGps);
		}

		if (network_enabled)
		{
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);
		}
	}

	public static void unsubscribe()
	{
		if (lm != null)
		{
			lm.removeUpdates(locationListenerGps);
			lm.removeUpdates(locationListenerNetwork);
		}
	}

	public static Location getLocation(Context context)
	{
		if (lastGpsLocation != null)
		{
			unsubscribe();
			return lastGpsLocation;
		} else if (lastNetworkLocation != null)
		{
			unsubscribe();
			return lastNetworkLocation;
		} else
		{
			unsubscribe();
			return getLastChanceLocation(context);
		}
	}

	private static Location getLastChanceLocation(Context ctx)
	{
		LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = lm.getProviders(true);

		// Loop over the array backwards, and if you get an accurate location,
		// then break out the loop
		Location l = null;

		for (int i = providers.size() - 1; i >= 0; i--)
		{
			l = lm.getLastKnownLocation(providers.get(i));
			if (l != null)
				break;
		}
		return l;
	}

	private static Location			lastGpsLocation			= null;
	private static Location			lastNetworkLocation		= null;

	private static LocationListener	locationListenerGps		= new LocationListener()
															{
																public void onLocationChanged(Location location)
																{
																	lm.removeUpdates(this);
																	lm.removeUpdates(locationListenerNetwork);

																	lastGpsLocation = location;
																}

																public void onProviderDisabled(String provider)
																{
																}

																public void onProviderEnabled(String provider)
																{
																}

																public void onStatusChanged(String provider,
																		int status, Bundle extras)
																{
																}
															};

	private static LocationListener	locationListenerNetwork	= new LocationListener()
															{
																public void onLocationChanged(Location location)
																{
																	lm.removeUpdates(this);
																	lm.removeUpdates(locationListenerGps);

																	lastNetworkLocation = location;
																}

																public void onProviderDisabled(String provider)
																{
																}

																public void onProviderEnabled(String provider)
																{
																}

																public void onStatusChanged(String provider,
																		int status, Bundle extras)
																{
																}
															};
}
