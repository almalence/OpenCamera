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

/* <!-- +++
 import com.almalence.opencam_plus.R;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.R;
//-+- -->

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

// A view that indicates the focus area or the metering area.
public class FocusIndicatorView extends View implements FocusIndicator
{
	public FocusIndicatorView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public FocusIndicatorView(Context context)
	{
		super(context);
	}

	private void setDrawable(int resid)
	{
		setBackgroundDrawable(getResources().getDrawable(resid));
	}

	@Override
	public void showStart()
	{
		setDrawable(R.drawable.ic_focus_focusing);
	}

	@Override
	public void showSuccess()
	{
		setDrawable(R.drawable.ic_focus_focused);
	}

	@Override
	public void showFail()
	{
		setDrawable(R.drawable.ic_focus_failed);
	}

	@Override
	public void clear()
	{
		setBackgroundDrawable(null);
	}
}