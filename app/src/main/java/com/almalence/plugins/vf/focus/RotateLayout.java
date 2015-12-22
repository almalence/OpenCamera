/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.almalence.plugins.vf.focus;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

// A RotateLayout is designed to display a single item and provides the
// capabilities to rotate the item.
public class RotateLayout extends ViewGroup implements Rotatable
{
	private static final String	TAG	= "RotateLayout";
	private int					mOrientation;
	private View				mChild;

	public RotateLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		// The transparent background here is a workaround of the render issue
		// happened when the view is rotated as the device&#39;s orientation
		// changed. The view looks fine in landscape. After rotation, the view
		// is invisible.
		setBackgroundResource(android.R.color.transparent);
	}

	public RotateLayout(Context context)
	{
		super(context);
		// The transparent background here is a workaround of the render issue
		// happened when the view is rotated as the device&#39;s orientation
		// changed. The view looks fine in landscape. After rotation, the view
		// is invisible.
		setBackgroundResource(android.R.color.transparent);
	}

	@TargetApi(11)
	@Override
	protected void onFinishInflate()
	{
		mChild = getChildAt(0);
		if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB))
		{
			mChild.setPivotX(0);
			mChild.setPivotY(0);
		}
	}

	@Override
	protected void onLayout(boolean change, int left, int top, int right, int bottom)
	{
		int width = right - left;
		int height = bottom - top;
		if (mChild == null)
			return;
		switch (mOrientation)
		{
		case 0:
		case 180:
			mChild.layout(0, 0, width, height);
			break;
		case 90:
		case 270:
			mChild.layout(0, 0, height, width);
			break;
		default:
			break;
		}
	}

	@TargetApi(11)
	@Override
	protected void onMeasure(int widthSpec, int heightSpec)
	{
		int w = 0, h = 0;
		if (mChild == null)
		{
			setMeasuredDimension(w, h);
			return;
		}

		switch (mOrientation)
		{
		case 0:
		case 180:
			measureChild(mChild, widthSpec, heightSpec);
			w = mChild.getMeasuredWidth();
			h = mChild.getMeasuredHeight();
			break;
		case 90:
		case 270:
			measureChild(mChild, heightSpec, widthSpec);
			w = mChild.getMeasuredHeight();
			h = mChild.getMeasuredWidth();
			break;
		default:
			break;
		}
		setMeasuredDimension(w, h);

	}

	// Rotate the view counter-clockwise
	public void setOrientation(int orientation)
	{
		orientation = orientation % 360;
		if (mOrientation == orientation)
			return;
		mOrientation = orientation;
		requestLayout();
	}
}