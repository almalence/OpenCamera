package com.almalence.opencam.ui;

import android.content.Context;
import android.view.Window;

import com.almalence.templatecamera.R;
import com.almalence.ui.RotateDialog;
import com.almalence.ui.RotateLayout;

public class SonyCameraDeviceExplorerDialog extends RotateDialog
{
	public SonyCameraDeviceExplorerDialog(Context context)
	{
		super(context);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.opencamera_sony_device_discovery);
	}

	@Override
	public void setRotate(int degree)
	{
		degree = degree >= 0 ? degree % 360 : degree % 360 + 360;

		currentOrientation = degree;

		RotateLayout r = (RotateLayout) findViewById(R.id.rotateSonyCameraExplorerDialog);
		if (r != null) {
			r.setAngle(degree);
			r.requestLayout();
			r.invalidate();
		}
	}
}
