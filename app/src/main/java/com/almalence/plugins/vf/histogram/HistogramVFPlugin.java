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

package com.almalence.plugins.vf.histogram;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

/* <!-- +++
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.PluginViewfinder;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.PluginViewfinder;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

import com.almalence.util.Util;
import com.almalence.ui.RotateImageView;

/***
 * Implements histogram (RGB and Luminance) based on preview image data
 ***/

public class HistogramVFPlugin extends PluginViewfinder
{
	HistogramView				histogram;
	HistogramRGBView			histogramRGB;

	RotateImageView				histogramRIV;
	RotateImageView				histogramRGBRIV;

	private static final int	RGB				= 0;
	private static final int	LUMA			= 1;
	private static final int	NONE			= 2;

	private int[]				histFacts;
	private int[]				histFactsR;
	private int[]				histFactsG;
	private int[]				histFactsB;

	private int					frameCounter	= 0;

	public static Path			histPath;
	public static Path			histPathR;
	public static Path			histPathG;
	public static Path			histPathB;

	private int					histoHeight		= 0;
	private int					histoWidth		= 0;

	private static int			histogramType	= NONE;

	//takes each X image, skipping other
	private int					skipImgNum		= 6;
	
	public HistogramVFPlugin()
	{
		super("com.almalence.plugins.histogramvf", R.xml.preferences_vf_histogram, 0,
				R.drawable.gui_almalence_histogram_rgb, "Histogram");
	}

	@Override
	public void onCreate()
	{
		UpdatePreferences();

		histFacts = new int[256];
		histPath = new Path();
		histPath.setFillType(Path.FillType.EVEN_ODD);
		if(this.histogram == null)
			this.histogram = new HistogramView(ApplicationScreen.getMainContext());
		else
			removeViewQuick(this.histogram);

		histFactsR = new int[256];
		histPathR = new Path();
		histPathR.setFillType(Path.FillType.EVEN_ODD);
		histFactsG = new int[256];
		histPathG = new Path();
		histPathG.setFillType(Path.FillType.EVEN_ODD);
		histFactsB = new int[256];
		histPathB = new Path();
		histPathB.setFillType(Path.FillType.EVEN_ODD);
		if(this.histogramRGB == null)
			this.histogramRGB = new HistogramRGBView(ApplicationScreen.getMainContext());
		else
			removeViewQuick(this.histogramRGB);

		UpdatePreferences();

		histogramRGB.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				onClickHistogram(true);
			}
		});

		histogram.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				onClickHistogram(false);
			}
		});

		clearViews();

		if (histogramType == RGB)
			addView(this.histogramRGB, ViewfinderZone.VIEWFINDER_ZONE_BOTTOM_LEFT);
		else if (histogramType == LUMA)
			addView(this.histogram, ViewfinderZone.VIEWFINDER_ZONE_BOTTOM_LEFT);
	}

	public void onClickHistogram(boolean isRGB)
	{
		if (ApplicationScreen.getGUIManager().lockControls)
		{
			return;
		}
		UpdatePreferences();
		// save to shared prefs
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		Editor editor = prefs.edit();

		editor.putString("PrefHistogramVF", isRGB ? "1" : "0");

		histoHeight = (int) (ApplicationScreen.getGUIManager().getMaxPluginViewHeight() * 0.6);
		histoWidth = (int) (ApplicationScreen.getGUIManager().getMaxPluginViewWidth() * 0.6);
		android.widget.RelativeLayout.LayoutParams histLayoutParams = new android.widget.RelativeLayout.LayoutParams(
				histoWidth, histoHeight);

		if (isRGB)
			HistogramVFPlugin.this.histogramRGB.setLayoutParams(histLayoutParams);
		else
			HistogramVFPlugin.this.histogram.setLayoutParams(histLayoutParams);

		clearViews();
		if (isRGB)
			addView(HistogramVFPlugin.this.histogram);
		else
			addView(HistogramVFPlugin.this.histogramRGB);

		if (isRGB)
		{
			ApplicationScreen.getGUIManager().removeViewQuick(HistogramVFPlugin.this.histogramRGB);
			ApplicationScreen.getGUIManager().addViewQuick(HistogramVFPlugin.this.histogram,
					PluginViewfinder.ViewfinderZone.VIEWFINDER_ZONE_BOTTOM_LEFT);
		} else
		{
			ApplicationScreen.getGUIManager().removeViewQuick(HistogramVFPlugin.this.histogram);
			ApplicationScreen.getGUIManager().addViewQuick(HistogramVFPlugin.this.histogramRGB,
					PluginViewfinder.ViewfinderZone.VIEWFINDER_ZONE_BOTTOM_LEFT);
		}
		editor.commit();
		UpdatePreferences();
	}

	@Override
	public void onQuickControlClick()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		Editor editor = prefs.edit();

		switch (histogramType)
		{
		case RGB:
			quickControlIconID = R.drawable.gui_almalence_histogram_luma;
			editor.putString("PrefHistogramVF", "1");
			break;
		case LUMA:
			quickControlIconID = R.drawable.gui_almalence_histogram_off;
			editor.putString("PrefHistogramVF", "2");
			break;
		case NONE:
			quickControlIconID = R.drawable.gui_almalence_histogram_rgb;
			editor.putString("PrefHistogramVF", "0");
			break;
		default:
			break;
		}
		editor.commit();

		UpdatePreferences();

		if (histogramType == NONE)
		{
			histogramRGB.setVisibility(View.GONE);
			histogram.setVisibility(View.GONE);
		} else
		{
			histogramRGB.setVisibility(View.VISIBLE);
			histogram.setVisibility(View.VISIBLE);

			showHisto();
		}
	}

	public boolean needPreviewFrame()
	{
		if (histogramType == RGB || histogramType == LUMA)
			return true;
		else
			return false;
	}
	
	void UpdatePreferences()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		histogramType = Integer.parseInt(prefs.getString("PrefHistogramVF", "2"));

		switch (histogramType)
		{
		case RGB:
			quickControlIconID = R.drawable.gui_almalence_histogram_rgb;
			break;
		case LUMA:
			quickControlIconID = R.drawable.gui_almalence_histogram_luma;
			break;
		case NONE:
			quickControlIconID = R.drawable.gui_almalence_histogram_off;
			break;
		default:
			break;
		}
		
		CameraController.checkNeedPreviewFrame();
	}

	public static int					mDeviceOrientation;

	@Override
	public void onStart()
	{
	}

	@Override
	public void onResume()
	{
		UpdatePreferences();
		if (histogramType == NONE)
		{
			histogramRGB.setVisibility(View.GONE);
			histogram.setVisibility(View.GONE);
		} else
		{
			new CountDownTimer(500, 500)
			{
				public void onTick(long millisUntilFinished)
				{
				}

				public void onFinish()
				{
					histogramRGB.setVisibility(View.VISIBLE);
					histogram.setVisibility(View.VISIBLE);

					showHisto();
				}
			}.start();
		}
	}

	private void showHisto()
	{
		histoHeight = (int) (ApplicationScreen.getGUIManager().getMaxPluginViewHeight() * 0.6);
		histoWidth = (int) (ApplicationScreen.getGUIManager().getMaxPluginViewWidth() * 0.6);
		android.widget.RelativeLayout.LayoutParams histLayoutParams = new android.widget.RelativeLayout.LayoutParams(
				histoWidth, histoHeight);
		histLayoutParams.setMargins(20, 0, 0, 0);
		if (histogramType == RGB)
			HistogramVFPlugin.this.histogramRGB.setLayoutParams(histLayoutParams);
		else
			HistogramVFPlugin.this.histogram.setLayoutParams(histLayoutParams);

		clearViews();
		if (histogramType == RGB)
			addView(HistogramVFPlugin.this.histogramRGB);
		else
			addView(HistogramVFPlugin.this.histogram);

		if (histogramType == RGB)
		{
			ApplicationScreen.getGUIManager().removeViewQuick(HistogramVFPlugin.this.histogram);
			ApplicationScreen.getGUIManager().removeViewQuick(HistogramVFPlugin.this.histogramRGB);
			ApplicationScreen.getGUIManager().addViewQuick(HistogramVFPlugin.this.histogramRGB,
					PluginViewfinder.ViewfinderZone.VIEWFINDER_ZONE_BOTTOM_LEFT);
		} else
		{
			ApplicationScreen.getGUIManager().removeViewQuick(HistogramVFPlugin.this.histogramRGB);
			ApplicationScreen.getGUIManager().removeViewQuick(HistogramVFPlugin.this.histogram);
			ApplicationScreen.getGUIManager().addViewQuick(HistogramVFPlugin.this.histogram,
					PluginViewfinder.ViewfinderZone.VIEWFINDER_ZONE_BOTTOM_LEFT);
		}
	}

	@Override
	public void onPause()
	{
	}

	@Override
	public void onGUICreate()
	{
		if (histogramType == RGB)
		{
			histoHeight = (int) (ApplicationScreen.getGUIManager().getMaxPluginViewHeight() * 0.6);
			histoWidth = (int) (ApplicationScreen.getGUIManager().getMaxPluginViewWidth() * 0.6);

			android.widget.RelativeLayout.LayoutParams histLayoutParams = new android.widget.RelativeLayout.LayoutParams(
					histoWidth, histoHeight);
			histLayoutParams.setMargins(20, 0, 0, 0);
			this.histogram.setLayoutParams(histLayoutParams);
		} else if (histogramType == LUMA)
		{
			histoHeight = (int) (ApplicationScreen.getGUIManager().getMaxPluginViewHeight() * 0.6);
			histoWidth = (int) (ApplicationScreen.getGUIManager().getMaxPluginViewWidth() * 0.6);

			android.widget.RelativeLayout.LayoutParams histLayoutParams = new android.widget.RelativeLayout.LayoutParams(
					histoWidth, histoHeight);
			histLayoutParams.setMargins(20, 0, 0, 0);
			this.histogramRGB.setLayoutParams(histLayoutParams);
		}
	}

	@Override
	public void onCameraSetup()
	{
		if (histogramType == NONE)
			return;
	}

	@Override
	public void onPreviewFrame(byte[] data)
	{
		if (histogramType == NONE)
			return;
		frameCounter++;
		if (frameCounter != skipImgNum)
		{
			return;
		}
		int previewWidth = ApplicationScreen.getPreviewWidth();
		int previewHeight = ApplicationScreen.getPreviewHeight();

		if (histogramType == LUMA)
		{
			Histogram.createHistogram(data, histFacts, previewWidth, previewHeight, 256, histoHeight);

			histPath.reset();
			histPath.moveTo(0, histoHeight);
			for (int i = 1; i < 256; i++)
			{
				histPath.lineTo(((float) histoWidth / 256) * i, histoHeight - histFacts[i]);
			}

			histPath.setLastPoint(histoWidth, histoHeight);

			histogram.invalidate();
		} else if (histogramType == RGB)
		{
			Histogram.createRGBHistogram(data, histFactsR, histFactsG, histFactsB, previewWidth, previewHeight, 256,
					histoHeight);

			histPathR.reset();
			histPathR.moveTo(0, histoHeight);
			histPathG.reset();
			histPathG.moveTo(0, histoHeight);
			histPathB.reset();
			histPathB.moveTo(0, histoHeight);
			for (int i = 1; i < 256; i++)
			{
				histPathR.lineTo(((float) histoWidth / 256) * i, histoHeight - histFactsR[i]);
				histPathG.lineTo(((float) histoWidth / 256) * i, histoHeight - histFactsG[i]);
				histPathB.lineTo(((float) histoWidth / 256) * i, histoHeight - histFactsB[i]);
			}

			histPathR.setLastPoint(histoWidth, histoHeight);
			histPathG.setLastPoint(histoWidth, histoHeight);
			histPathB.setLastPoint(histoWidth, histoHeight);

			histogramRGB.invalidate();

		}
		frameCounter = 0;
	}
	
	@Override
	public void onOrientationChanged(int orientation)
	{
		mDeviceOrientation = ApplicationScreen.getGUIManager().getLayoutOrientation();
	}
}

class HistogramView extends View
{
	private Paint	histPaint;

	public HistogramView(Context context)
	{
		super(context);

		histPaint = new Paint();
		histPaint.setColor(Color.WHITE);
		histPaint.setAlpha(200);
		histPaint.setStyle(Paint.Style.FILL);
		histPaint.setStrokeWidth(1);
		histPaint.setAntiAlias(true);
	}

	@Override
	public void onDraw(Canvas canvas)
	{
		if (HistogramVFPlugin.mDeviceOrientation != 0)
		{
			canvas.rotate(-HistogramVFPlugin.mDeviceOrientation);
			if (HistogramVFPlugin.mDeviceOrientation == 270)
				canvas.translate(0, -getWidth());
			else if (HistogramVFPlugin.mDeviceOrientation == 90)
				canvas.translate(-getHeight(), 0);
			else if (HistogramVFPlugin.mDeviceOrientation == 180)
				canvas.translate(-getHeight(), -getWidth());

			super.onDraw(canvas);
		}

		canvas.drawPath(HistogramVFPlugin.histPath, histPaint);
	}
}

class HistogramRGBView extends View
{
	private Paint	histPaintR;
	private Paint	histPaintG;
	private Paint	histPaintB;

	public HistogramRGBView(Context context)
	{
		super(context);

		histPaintR = new Paint();
		histPaintR.setColor(16741779);
		histPaintR.setStyle(Paint.Style.FILL);
		histPaintR.setStrokeWidth(1);
		histPaintR.setAntiAlias(true);
		histPaintR.setAlpha(180);

		histPaintG = new Paint();
		histPaintG.setColor(7733176);
		histPaintG.setStyle(Paint.Style.FILL);
		histPaintG.setStrokeWidth(1);
		histPaintG.setAntiAlias(true);
		histPaintG.setAlpha(180);

		histPaintB = new Paint();
		histPaintB.setColor(8943103);
		histPaintB.setStyle(Paint.Style.FILL);
		histPaintB.setStrokeWidth(1);
		histPaintB.setAntiAlias(true);
		histPaintB.setAlpha(180);
	}

	@Override
	public void onDraw(Canvas canvas)
	{
		if (HistogramVFPlugin.mDeviceOrientation != 0)
		{
			canvas.rotate(-HistogramVFPlugin.mDeviceOrientation);
			if (HistogramVFPlugin.mDeviceOrientation == 270)
				canvas.translate(0, -getWidth());
			else if (HistogramVFPlugin.mDeviceOrientation == 90)
				canvas.translate(-getHeight(), 0);
			else if (HistogramVFPlugin.mDeviceOrientation == 180)
				canvas.translate(-getHeight(), -getWidth());

			super.onDraw(canvas);
		}

		canvas.drawPath(HistogramVFPlugin.histPathR, histPaintR);
		canvas.drawPath(HistogramVFPlugin.histPathG, histPaintG);
		canvas.drawPath(HistogramVFPlugin.histPathB, histPaintB);
	}
}
