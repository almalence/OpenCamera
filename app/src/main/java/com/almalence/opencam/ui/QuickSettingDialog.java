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
 package com.almalence.opencam_plus.ui;
 +++ --> */
// <!-- -+-
package com.almalence.opencam.ui;

//-+- -->

import android.content.Context;
import android.view.Window;

import com.almalence.ui.RotateDialog;
import com.almalence.ui.RotateLayout;

//<!-- -+-
import com.almalence.opencam.R;
//-+- -->
/* <!-- +++
 import com.almalence.opencam_plus.R;
 +++ --> */

public class QuickSettingDialog extends RotateDialog
{
	public QuickSettingDialog(Context context)
	{
		super(context);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.quick_setting_dialog);
	}

	@Override
	public void setRotate(int degree)
	{
		degree = degree >= 0 ? degree % 360 : degree % 360 + 360;

		if (degree == currentOrientation)
		{
			return;
		}
		currentOrientation = degree;

		RotateLayout r = (RotateLayout) findViewById(R.id.rotateLayoutQuickSettingDialog);
		r.setAngle(degree);
		r.requestLayout();
		r.invalidate();
	}
}
