package com.almalence.opencam.ui;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.sony.cameraremote.ServerDevice;
import com.almalence.sony.cameraremote.SimpleSsdpClient;
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
