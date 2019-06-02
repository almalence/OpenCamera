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
 import com.almalence.opencam_plus.ApplicationInterface;
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginViewfinder;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.R;
 +++ --> */

// <!-- -+-
import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginViewfinder;
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.R;

//-+- -->

/***
 * Implements zoom functionality - slider, pinch, sound buttons
 ***/

public class ZoomVFPlugin extends PluginViewfinder
{
	private VerticalSeekBar			zoomBar				= null;
	private ImageButton				mButtonZoomIn		= null;
	private ImageButton				mButtonZoomOut		= null;
	private float					zoomCurrent			= 0;
	private View					zoomPanelView		= null;
	private LinearLayout			zoomPanel			= null;

	private int						mainLayoutHeight	= 0;
	private int						zoomPanelWidth		= 0;

	private boolean					panelOpened			= false;
	private boolean					panelToBeOpen		= false;
	private boolean					panelOpening		= false;
	private boolean					panelClosing		= false;
	private boolean					mZoomDisabled		= false;

	private boolean					isEnabled			= true;

	private Handler					zoomHandler;
	
	private boolean				zoomStopping			= false;
	
	private static final int	CLOSE_ZOOM_PANEL		= 0;
	private static final int	CLOSE_ZOOM_PANEL_DELAY	= 1500;
	
	private class ZoomHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			if (msg.what == CLOSE_ZOOM_PANEL)
				closeZoomPanel();
		}
	}
	
	private ScaleGestureDetector  scaleGestureDetector;

	public ZoomVFPlugin()
	{
		super("com.almalence.plugins.zoomvf",  R.xml.preferences_vf_zoom, 0, 0, null);
		zoomHandler = new ZoomHandler();
	}

	@Override
	public void onCreate()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		isEnabled = prefs.getBoolean("enabledPrefZoom", true);

		panelOpened = false;
		
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
					
					zoomCurrent = progress;
					if (CameraController.isUseCamera2())
					{
						// Calculate scaleFactor from seekBar progress.
						zoomCurrent = (float)progress / 10.f + 1.f;
						Log.e("!!!!!!!", "zoom current caculated = " + zoomCurrent);
					}
					
					if (zoomCurrent > CameraController.getMaxZoom())
					{
						zoomCurrent = CameraController.getMaxZoom();
					}

					//workaround for S7 issue with zooming.
					if (CameraController.isGalaxyS7 && zoomCurrent>1.2)
					{
						zoomCurrent+=0.001f;
					}
					
					CameraController.setZoom(zoomCurrent);
				}
			}
		});
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
		onCameraParametersSetup();
	}

	@Override
	public void onResume()
	{
		zoomStopping = false;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		isEnabled = prefs.getBoolean("enabledPrefZoom", true);
		showZoomControls();
	}

	public void showZoomControls()
	{
		if (!CameraController.isRemoteCamera())
		{
			zoomPanel.findViewById(R.id.zoom_buttons_container).setVisibility(View.GONE);
			if (!isEnabled)

				zoomBar.setVisibility(View.GONE);
			else
				zoomBar.setVisibility(View.VISIBLE);
		} else
		{
			zoomBar.setVisibility(View.GONE);
			if (!isEnabled)
				zoomPanel.findViewById(R.id.zoom_buttons_container).setVisibility(View.GONE);
			else
				zoomPanel.findViewById(R.id.zoom_buttons_container).setVisibility(View.VISIBLE);
		}
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
			// If isCamera2 mode, then decrease value by 1 and multiple maxZoom 10x, because it represent by scale factor (from 1.0 to maxZoom).
			// Else just set the exact value, given by CameraController (from 0 to maxZoom).
			zoomBar.setMax(CameraController.isUseCamera2() ? (int)(CameraController.getMaxZoom() * 10 - 10) : (int)CameraController.getMaxZoom());
			zoomBar.setProgressAndThumb(0);
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

				//workaround for S7 issue with zooming.
				if (CameraController.isGalaxyS7 && zoomCurrent>1.2)
				{
					zoomCurrent+=0.001f;
				}
				CameraController.setZoom(zoomCurrent);
				zoomBar.setProgressAndThumb((int) (CameraController.isUseCamera2() ? zoomCurrent * 10.f - 10 : zoomCurrent));
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
			this.zoomModify(CameraController.isUseCamera2() ? -0.1f : -1);
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_ZOOM_IN)
		{
			this.zoomModify(CameraController.isUseCamera2() ? 0.1f : 1);
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

				if (params == null)
				{
					zoomPanel.clearAnimation();
					return;
				}

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
		if (arg1 == ApplicationInterface.MSG_CONTROL_LOCKED)
			mZoomDisabled = true;
		else if (arg1 == ApplicationInterface.MSG_CONTROL_UNLOCKED)
			mZoomDisabled = false;
		return false;
	}
	
	@Override
	public boolean onMultiTouch(View view, MotionEvent e)
	{
		return scaleGestureDetector.onTouchEvent(e);
	}
}
