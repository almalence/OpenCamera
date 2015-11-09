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

package com.almalence.plugins.vf.zoom;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.almalence.sony.cameraremote.ZoomCallbackSonyRemote;
import com.almalence.ui.VerticalSeekBar;
/* <!-- +++
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.PluginViewfinder;
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.R;
 +++ --> */

// <!-- -+-
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginViewfinder;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;

//-+- -->

/***
 * Implements zoom functionality - slider, pinch, sound buttons
 ***/

public class ZoomVFPlugin extends PluginViewfinder
{
	private ImageButton		mButtonZoomIn		= null;
	private ImageButton		mButtonZoomOut		= null;
	private float			zoomCurrent			= 0;

	private int				mainLayoutHeight	= 0;
	private View			zoomPanelView		= null;
	private LinearLayout	zoomPanel			= null;
	private int				zoomPanelWidth		= 0;

	//private boolean			mZoomDisabled		= false;

	//private boolean			isEnabled			= true;
	
	private ScaleGestureDetector  scaleGestureDetector;

	public ZoomVFPlugin()
	{
		super("com.almalence.plugins.zoomvf", 0, 0, 0, null);
	}

	@Override
	public void onCreate()
	{
//		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
//		isEnabled = prefs.getBoolean("enabledPrefZoom", true);

		LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
		zoomPanelView = inflator.inflate(R.layout.plugin_vf_zoom_layout, null, false);
		zoomPanel = (LinearLayout) zoomPanelView.findViewById(R.id.zoomLayout);
		zoomPanel.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				return false;
			}
		});

		mButtonZoomIn = (ImageButton) zoomPanel.findViewById(R.id.button_zoom_in);
		mButtonZoomOut = (ImageButton) zoomPanel.findViewById(R.id.button_zoom_out);
		initZoomButtons();
		ZoomCallbackSonyRemote zoomCallbackSonyRemote = new ZoomCallbackSonyRemote()
		{
			@Override
			public void onZoomPositionChanged(final int zoomPosition)
			{
				MainScreen.getInstance().runOnUiThread(new Runnable()
				{

					@Override
					public void run()
					{
						if (zoomPosition == 0)
						{
							mButtonZoomIn.setEnabled(true);
							mButtonZoomOut.setEnabled(false);
						} else if (zoomPosition == 100)
						{
							mButtonZoomIn.setEnabled(false);
							mButtonZoomOut.setEnabled(true);
						} else
						{
							mButtonZoomIn.setEnabled(true);
							mButtonZoomOut.setEnabled(true);
						}
					}
				});

			}

			@Override
			public void onZoomAvailabelChanged(final boolean isZoomAvailable)
			{
				MainScreen.getInstance().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						prepareActZoomButtons(isZoomAvailable);
					}
				});
			}
		};
		CameraController.setZoomCallbackSonyRemote(zoomCallbackSonyRemote);
	}

	private void prepareActZoomButtons(boolean flag)
	{
		if (flag)
		{
			zoomPanel.setVisibility(View.VISIBLE);
			mButtonZoomOut.setVisibility(View.VISIBLE);
			mButtonZoomIn.setVisibility(View.VISIBLE);
		} else
		{
			zoomPanel.setVisibility(View.GONE);
			mButtonZoomOut.setVisibility(View.GONE);
			mButtonZoomIn.setVisibility(View.GONE);
		}
	}

	private void initZoomButtons()
	{
		mButtonZoomIn.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				CameraController.actZoomSonyRemote("in", "1shot");
			}
		});

		mButtonZoomOut.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				CameraController.actZoomSonyRemote("out", "1shot");
			}
		});

		mButtonZoomIn.setOnLongClickListener(new View.OnLongClickListener()
		{

			@Override
			public boolean onLongClick(View arg0)
			{
				CameraController.actZoomSonyRemote("in", "start");
				return true;
			}
		});

		mButtonZoomOut.setOnLongClickListener(new View.OnLongClickListener()
		{

			@Override
			public boolean onLongClick(View arg0)
			{
				CameraController.actZoomSonyRemote("out", "start");
				return true;
			}
		});

		mButtonZoomIn.setOnTouchListener(new View.OnTouchListener()
		{

			private long	downTime	= -1;

			@Override
			public boolean onTouch(View v, MotionEvent event)
			{

				if (event.getAction() == MotionEvent.ACTION_UP)
				{
					if (System.currentTimeMillis() - downTime > 500)
					{
						CameraController.actZoomSonyRemote("in", "stop");
					}
				}
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					downTime = System.currentTimeMillis();
				}
				return false;
			}
		});

		mButtonZoomOut.setOnTouchListener(new View.OnTouchListener()
		{

			private long	downTime	= -1;

			@Override
			public boolean onTouch(View v, MotionEvent event)
			{

				if (event.getAction() == MotionEvent.ACTION_UP)
				{
					if (System.currentTimeMillis() - downTime > 500)
					{
						CameraController.actZoomSonyRemote("out", "stop");
					}
				}
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					downTime = System.currentTimeMillis();
				}
				return false;
			}
		});
	}

	@Override
	public void onStart()
	{
//		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
//		isEnabled = prefs.getBoolean("enabledPrefZoom", true);
	}

	@Override
	public void onStop()
	{
		MainScreen.getGUIManager().removeViews(zoomPanel, R.id.specialPluginsLayout);
	}

	@Override
	public void onGUICreate()
	{
		MainScreen.getGUIManager().removeViews(zoomPanel, R.id.specialPluginsLayout);

		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) MainScreen.getInstance()
				.findViewById(R.id.specialPluginsLayout).getLayoutParams();
		mainLayoutHeight = lp.height;

		RelativeLayout.LayoutParams params;

		if (!CameraController.isRemoteCamera())
		{
			params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
			zoomPanelWidth = MainScreen.getAppResources().getDrawable(R.drawable.scrubber_control_pressed_holo)
					.getMinimumWidth();
			params.setMargins(-zoomPanelWidth / 2, 0, 0, 0);
			params.height = (int) (mainLayoutHeight / 2.2);
		} else
		{
			params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.setMargins(10, 0, 0, 0);
		}

		params.addRule(RelativeLayout.CENTER_VERTICAL);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout)).addView(this.zoomPanel,
				params);

		this.zoomPanel.setLayoutParams(params);
		showZoomControls();

		scaleGestureDetector = new ScaleGestureDetector(ApplicationScreen.instance, new OnScaleGestureListener()
		{
			@Override
			public boolean onScale(final ScaleGestureDetector scaleGestureDetector)
			{
				if (CameraController.isRemoteCamera())
				{
					return false;
				}
				
				if (!CameraController.isUseCamera2()) {
					// Division by zero required for smooth zooming.
					zoomModify((scaleGestureDetector.getCurrentSpan() - scaleGestureDetector
							.getPreviousSpan()) / 10);
				} else {
					zoomModify(zoomCurrent * scaleGestureDetector.getScaleFactor() - zoomCurrent);
				}
				
				return true;
			}

			@Override
			public boolean onScaleBegin(final ScaleGestureDetector scaleGestureDetector)
			{
				if (CameraController.isRemoteCamera())
				{
					return false;
				}
				return true;
			}

			@Override
			public void onScaleEnd(final ScaleGestureDetector scaleGestureDetector)
			{

			}
		});
	}

	@Override
	public void onResume()
	{
//		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
//		isEnabled = prefs.getBoolean("enabledPrefZoom", true);
		showZoomControls();
	}

	public void showZoomControls()
	{
		if (!CameraController.isRemoteCamera())
		{
			zoomPanel.findViewById(R.id.zoom_buttons_container).setVisibility(View.GONE);
		}
//		else
//		{
//			if (!isEnabled)
//				zoomPanel.findViewById(R.id.zoom_buttons_container).setVisibility(View.GONE);
//			else
//				zoomPanel.findViewById(R.id.zoom_buttons_container).setVisibility(View.VISIBLE);
//		}
	}

	@Override
	public void onCameraParametersSetup()
	{
		if (!CameraController.isUseCamera2())
		{
			zoomCurrent = 0;
		} else {
			zoomCurrent = 1.f;
		}

		if (CameraController.isZoomSupported())
		{
			zoomPanel.setVisibility(View.VISIBLE);
			CameraController.setZoom(zoomCurrent);
		} else
			zoomPanel.setVisibility(View.GONE);
	}

	private void zoomModify(float delta)
	{
		if (CameraController.isZoomSupported())
		{
			try
			{
				zoomCurrent += delta;

				if (!CameraController.isUseCamera2()) {
					if (zoomCurrent < 0)
						zoomCurrent = 0;
					
				}
				else {
					if (zoomCurrent < 1)
						zoomCurrent = 1;
				}
				
				if (zoomCurrent > CameraController.getMaxZoom())
				{
					zoomCurrent = CameraController.getMaxZoom();
				}

				CameraController.setZoom(zoomCurrent);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_ZOOM_OUT)
		{
			this.zoomModify(-1);
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_ZOOM_IN)
		{
			this.zoomModify(1);
			return true;
		}
		return false;
	}

	PointF				mid		= new PointF();
	static final int	NONE	= 0;
	static final int	DRAG	= 1;
	static final int	ZOOM	= 2;
	int					mode	= NONE;

//	@Override
//	public boolean onBroadcast(int arg1, int arg2)
//	{
//		if (!isEnabled)
//			return false;
//		if (arg1 == ApplicationInterface.MSG_CONTROL_LOCKED)
//			mZoomDisabled = true;
//		else if (arg1 == ApplicationInterface.MSG_CONTROL_UNLOCKED)
//			mZoomDisabled = false;
//		return false;
//	}
	
	@Override
	public boolean onMultiTouch(View view, MotionEvent e)
	{
		return scaleGestureDetector.onTouchEvent(e);
	}
}
