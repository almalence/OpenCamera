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

import java.util.Locale;

import android.app.Activity;

/* <!-- +++
 import com.almalence.opencam_plus.R;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.R;
//-+- -->
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.URIParsedResult;

/**
 * Offers appropriate actions for URLS.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class URIResultHandler extends ResultHandler
{
	// URIs beginning with entries in this array will not be saved to history or
	// copied to the
	// clipboard for security.
	private static final String[]	SECURE_PROTOCOLS	= { "otpauth:" };

	private static final int[]		buttons				= { R.string.Button_Open_Browser,
			R.string.Button_Share_By_Email, R.string.Button_Share_By_Sms, };

	public URIResultHandler(Activity activity, ParsedResult result)
	{
		super(activity, result);
	}

	@Override
	public int getButtonCount()
	{
		if (LocaleManager.isBookSearchUrl(((URIParsedResult) getResult()).getURI()))
		{
			return buttons.length;
		}
		return buttons.length - 1;
	}

	@Override
	public int getButtonText(int index)
	{
		return buttons[index];
	}

	@Override
	public Integer getDefaultButtonID()
	{
		return 0;
	}

	@Override
	public void handleButtonPress(int index)
	{
		URIParsedResult uriResult = (URIParsedResult) getResult();
		String uri = uriResult.getURI();
		switch (index)
		{
		case 0:
			openURL(uri);
			break;
		case 1:
			shareByEmail(uri);
			break;
		case 2:
			shareBySMS(uri);
			break;
		default:
			break;
		}
	}

	@Override
	public int getDisplayTitle()
	{
		return R.string.Result_Uri;
	}

	@Override
	public boolean areContentsSecure()
	{
		URIParsedResult uriResult = (URIParsedResult) getResult();
		String uri = uriResult.getURI().toLowerCase(Locale.ENGLISH);
		for (String secure : SECURE_PROTOCOLS)
		{
			if (uri.startsWith(secure))
			{
				return true;
			}
		}
		return false;
	}
}
