package com.almalence.plugins.capture.video;

import android.content.Context;
import android.view.Window;

import com.almalence.opencamunderground.R;
import com.almalence.ui.RotateDialog;
import com.almalence.ui.RotateLayout;
/* <!-- +++
  import com.almalence.opencam_plus.R;
 +++ --> */
//<!-- -+-
//-+- -->

public class TimeLapseDialog extends RotateDialog
{

	public TimeLapseDialog(Context context)
	{
		super(context);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
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

		RotateLayout r = (RotateLayout) findViewById(R.id.rotateLayout);
		r.setAngle(degree);
		r.requestLayout();
		r.invalidate();

	}
}
