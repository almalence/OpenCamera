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
import android.telephony.PhoneNumberUtils;

/* <!-- +++
 import com.almalence.opencam_plus.R;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.R;
//-+- -->
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.SMSParsedResult;

/**
 * Handles SMS addresses, offering a choice of composing a new SMS or MMS
 * message.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class SMSResultHandler extends ResultHandler
{
	private static final int[]	buttons	= { R.string.Button_Sms, R.string.Button_Mms };

	public SMSResultHandler(Activity activity, ParsedResult result)
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
		SMSParsedResult smsResult = (SMSParsedResult) getResult();
		String number = smsResult.getNumbers()[0];
		switch (index)
		{
		case 0:
			// Don't know of a way yet to express a SENDTO intent with multiple
			// recipients
			sendSMS(number, smsResult.getBody());
			break;
		case 1:
			sendMMS(number, smsResult.getSubject(), smsResult.getBody());
			break;
		default:
			break;
		}
	}

	@Override
	public CharSequence getDisplayContents()
	{
		SMSParsedResult smsResult = (SMSParsedResult) getResult();
		String[] rawNumbers = smsResult.getNumbers();
		String[] formattedNumbers = new String[rawNumbers.length];
		for (int i = 0; i < rawNumbers.length; i++)
		{
			formattedNumbers[i] = PhoneNumberUtils.formatNumber(rawNumbers[i]);
		}
		StringBuilder contents = new StringBuilder(50);
		ParsedResult.maybeAppend(formattedNumbers, contents);
		ParsedResult.maybeAppend(smsResult.getSubject(), contents);
		ParsedResult.maybeAppend(smsResult.getBody(), contents);
		return contents.toString();
	}

	@Override
	public int getDisplayTitle()
	{
		return R.string.Result_Sms;
	}
}
