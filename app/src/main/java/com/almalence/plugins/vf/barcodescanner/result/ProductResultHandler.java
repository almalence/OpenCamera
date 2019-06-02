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
import com.google.zxing.Result;
import com.google.zxing.client.result.ExpandedProductParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ProductParsedResult;

/**
 * Handles generic products which are not books.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ProductResultHandler extends ResultHandler
{
	private static final int[]	buttons	= { R.string.Button_Product_Search, R.string.Button_Web_Search, };

	public ProductResultHandler(Activity activity, ParsedResult result, Result rawResult)
	{
		super(activity, result, rawResult);
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
		String productID = getProductIDFromResult(getResult());
		switch (index)
		{
		case 0:
			openProductSearch(productID);
			break;
		case 1:
			webSearch(productID);
			break;
		default:
			break;
		}
	}

	private static String getProductIDFromResult(ParsedResult rawResult)
	{
		if (rawResult instanceof ProductParsedResult)
		{
			return ((ProductParsedResult) rawResult).getNormalizedProductID();
		}
		if (rawResult instanceof ExpandedProductParsedResult)
		{
			return ((ExpandedProductParsedResult) rawResult).getDisplayResult();
		}
		throw new IllegalArgumentException(rawResult.getClass().toString());
	}

	@Override
	public int getDisplayTitle()
	{
		return R.string.Result_Product;
	}
}
