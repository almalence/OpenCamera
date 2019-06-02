/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.almalence.plugins.vf.barcodescanner.result;

import android.app.Activity;

/* <!-- +++
 import com.almalence.opencam_plus.R;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.R;
//-+- -->
import com.google.zxing.client.result.GeoParsedResult;
import com.google.zxing.client.result.ParsedResult;

/**
 * Handles geographic coordinates (typically encoded as geo: URLs).
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class GeoResultHandler extends ResultHandler
{
	private static final int[]	buttons	= { R.string.Button_Show_Map, R.string.Button_Get_Directions };

	public GeoResultHandler(Activity activity, ParsedResult result)
	{
		super(activity, result);
	}

	@Override
	public int getButtonCount()
	{
		return buttons.length;
	}

	@Override
	public int getButtonText(int index)
	{
		return buttons[index];
	}

	@Override
	public void handleButtonPress(int index)
	{
		GeoParsedResult geoResult = (GeoParsedResult) getResult();
		switch (index)
		{
		case 0:
			openMap(geoResult.getGeoURI());
			break;
		case 1:
			getDirections(geoResult.getLatitude(), geoResult.getLongitude());
			break;
		default:
			break;
		}
	}

	@Override
	public int getDisplayTitle()
	{
		return R.string.Result_Geo;
	}
}
