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

import android.content.SharedPreferences;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

/* <!-- +++
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.PluginViewfinder;
 import com.almalence.opencam_plus.R;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginViewfinder;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->
import com.almalence.ui.VerticalSeekBar;

/***
 * Implements zoom functionality - slider, pinch, sound buttons
 ***/

public class ZoomVFPlugin extends PluginViewfinder
{
	private VerticalSeekBar		zoomBar					= null;
	private int					zoomCurrent				= 0;
	private View				zoomPanelView			= null;
	private LinearLayout		zoomPanel				= null;

	private int					mainLayoutHeight		= 0;
	private int					zoomPanelWidth			= 0;

	private boolean				panelOpened				= false;
	private boolean				panelToBeOpen			= false;
	private boolean				panelOpening			= false;
	private boolean				panelClosing			= false;
	private boolean				mZoomDisabled			= false;

	private boolean				isEnabled				= true;

	private Handler				zoomHandler;

	private boolean				zoomStopping			= false;

	private static final int	CLOSE_ZOOM_PANEL		= 0;
	private static final int	CLOSE_ZOOM_PANEL_DELAY	= 1500;

	private class MainHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			if (msg.what == CLOSE_ZOOM_PANEL)
				closeZoomPanel();
		}
	}

	public ZoomVFPlugin()
	{
		super("com.almalence.plugins.zoomvf", R.xml.preferences_vf_zoom, 0, 0, null);

		zoomHandler = new MainHandler();
	}

	@Override
	public void onCreate()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		isEnabled = prefs.getBoolean("enabledPrefZoom", true);

		panelOpened = false;

		LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
		zoomPanelView = inflator.inflate(R.layout.plugin_vf_zoom_layout, null, false);
		this.zoomPanel = (LinearLayout) zoomPanelView.findViewById(R.id.zoomLayout);
		this.zoomPanel.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				return false;
			}
		});

		this.zoomBar = (VerticalSeekBar) zoomPanelView.findViewById(R.id.zoomSeekBar);
		this.zoomBar.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if (mZoomDisabled)
				{
					if (panelOpened)
						zoomHandler.sendEmptyMessageDelayed(CLOSE_ZOOM_PANEL, CLOSE_ZOOM_PANEL_DELAY);
					return true;
				}

				switch (event.getAction() & MotionEvent.ACTION_MASK)
				{
				case MotionEvent.ACTION_DOWN:
					{
						if (!panelOpened)
						{
							openZoomPanel();
							zoomHandler.removeMessages(CLOSE_ZOOM_PANEL);
							return true;
						}
						if (panelClosing)
						{
							panelToBeOpen = true;
							return true;
						}
					}
					break;
				case MotionEvent.ACTION_UP:
					{
						if (panelOpened || panelOpening)
							zoomHandler.sendEmptyMessageDelayed(CLOSE_ZOOM_PANEL, CLOSE_ZOOM_PANEL_DELAY);
					}
					break;
				case MotionEvent.ACTION_MOVE:
					return false;
				default:
					break;
				}

				return false;
			}
		});
		this.zoomBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
		{

			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{
				zoomHandler.removeMessages(CLOSE_ZOOM_PANEL);
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				if (fromUser)
				{
					zoomHandler.removeMessages(CLOSE_ZOOM_PANEL);
					zoomModify(progress - zoomCurrent);
				}
			}
		});
	}

	@Override
	public void onStart()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		isEnabled = prefs.getBoolean("enabledPrefZoom", true);
		zoomStopping = false;
	}

	@Override
	public void onStop()
	{
		zoomStopping = true;
		MainScreen.getGUIManager().removeViews(zoomPanel, R.id.specialPluginsLayout);
	}

	@Override
	public void onGUICreate()
	{
		MainScreen.getGUIManager().removeViews(zoomPanel, R.id.specialPluginsLayout);

		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) MainScreen.getInstance()
				.findViewById(R.id.specialPluginsLayout).getLayoutParams();
		mainLayoutHeight = lp.height;

		zoomPanelWidth = MainScreen.getAppResources().getDrawable(R.drawable.scrubber_control_pressed_holo)
				.getMinimumWidth();

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.MATCH_PARENT);
		params.setMargins(-zoomPanelWidth / 2, 0, 0, 0);
		params.height = (int) (mainLayoutHeight / 2.2);

		params.addRule(RelativeLayout.CENTER_VERTICAL);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout)).addView(this.zoomPanel,
				params);

		this.zoomPanel.setLayoutParams(params);
//		this.zoomPanel.requestLayout();

//		((RelativeLayout) MainScreen.getInstasnce().findViewById(R.id.specialPluginsLayout)).requestLayout();
	}

	@Override
	public void onResume()
	{
		zoomStopping = false;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		isEnabled = prefs.getBoolean("enabledPrefZoom", true);

		if (!isEnabled)
		{
//			zoomPanel.setVisibility(View.GONE);
//			zoomPanelView.setVisibility(View.GONE);
			zoomBar.setVisibility(View.GONE);
		} else
		{
//			zoomPanel.setVisibility(View.VISIBLE);
//			zoomPanelView.setVisibility(View.VISIBLE);
			zoomBar.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onCameraParametersSetup()
	{
		zoomCurrent = 0;

		if (CameraController.isZoomSupported())
		{
			zoomBar.setMax(CameraController.getMaxZoom());
			zoomBar.setProgressAndThumb(0);
			zoomPanel.setVisibility(View.VISIBLE);
		} else
			zoomPanel.setVisibility(View.GONE);
	}

	private void zoomModify(int delta)
	{
		if (CameraController.isZoomSupported())
		{
			try
			{
				zoomCurrent += delta;

				if (zoomCurrent < 0)
				{
					zoomCurrent = 0;
				} else if (zoomCurrent > CameraController.getMaxZoom())
				{
					zoomCurrent = CameraController.getMaxZoom();
				}

				CameraController.setZoom(zoomCurrent);

				zoomBar.setProgressAndThumb(zoomCurrent);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
//		if (!isEnabled)
//			return false;

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

	public void closeZoomPanel()
	{
		//Log.d("ZoomPlugin", "closeZoomPanel");
		panelClosing = true;

		this.zoomPanel.clearAnimation();
		Animation animation = new TranslateAnimation(0, -zoomPanelWidth / 2, 0, 0);
		animation.setDuration(300);
		animation.setRepeatCount(0);
		animation.setInterpolator(new LinearInterpolator());
		animation.setFillAfter(true);

		this.zoomPanel.setAnimation(animation);

		animation.setAnimationListener(new AnimationListener()
		{

			@Override
			public void onAnimationEnd(Animation animation)
			{
//				Log.d("ZoomPlugin", "onAnimationEnd");
				if (zoomStopping)
				{
					List<View> specialView = new ArrayList<View>();
					RelativeLayout specialLayout = (RelativeLayout) MainScreen.getInstance().findViewById(
							R.id.specialPluginsLayout);
					for (int i = 0; i < specialLayout.getChildCount(); i++)
						specialView.add(specialLayout.getChildAt(i));

					for (int j = 0; j < specialView.size(); j++)
					{
						final View view = specialView.get(j);
						int view_id = view.getId();
						int zoom_id = zoomPanel.getId();
						if (view_id == zoom_id)
						{
							final ViewGroup parentView = (ViewGroup) view.getParent();
							if (parentView != null)
							{
								parentView.post(new Runnable()
								{
									public void run()
									{
										// it works without the runOnUiThread,
										// but all UI updates must
										// be done on the UI thread
										MainScreen.getInstance().runOnUiThread(new Runnable()
										{
											public void run()
											{
												parentView.removeView(view);
											}
										});
									}
								});
							}
						}
					}
					return;
				}

				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) zoomPanel.getLayoutParams();
				if (params == null)
				{
					zoomPanel.clearAnimation();
					return;
				}
				params.setMargins(-zoomPanelWidth / 2, 0, 0, 0);
				zoomPanel.setLayoutParams(params);
				zoomPanel.clearAnimation();

				panelOpened = false;
				panelClosing = false;

				if (panelToBeOpen)
				{
					panelToBeOpen = false;
					openZoomPanel();
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}

			@Override
			public void onAnimationStart(Animation animation)
			{
			}
		});
	}

	public void openZoomPanel()
	{
		panelOpening = true;
		this.zoomPanel.clearAnimation();
		Animation animation = new TranslateAnimation(0, zoomPanelWidth / 2, 0, 0);
		animation.setDuration(500);
		animation.setRepeatCount(0);
		animation.setInterpolator(new LinearInterpolator());
		animation.setFillAfter(true);

		this.zoomPanel.setAnimation(animation);

		animation.setAnimationListener(new AnimationListener()
		{

			@Override
			public void onAnimationEnd(Animation animation)
			{
				RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) zoomPanel.getLayoutParams();
				params.setMargins(0, 0, 0, 0);
				zoomPanel.setLayoutParams(params);

				zoomPanel.clearAnimation();
				zoomPanel.requestLayout();

				panelOpened = true;
				panelOpening = false;
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}

			@Override
			public void onAnimationStart(Animation animation)
			{
			}
		});
	}

	@Override
	public boolean onBroadcast(int arg1, int arg2)
	{
		if (!isEnabled)
			return false;
		if (arg1 == PluginManager.MSG_CONTROL_LOCKED)
			mZoomDisabled = true;
		else if (arg1 == PluginManager.MSG_CONTROL_UNLOCKED)
			mZoomDisabled = false;
		return false;
	}
}
