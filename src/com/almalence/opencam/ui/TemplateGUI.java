package com.almalence.opencam.ui;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import com.almalence.googsharing.Thumbnail;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.R;
import com.almalence.opencam.Plugin.ViewfinderZone;
import com.almalence.opencam.cameracontroller.CameraController.Size;
import com.almalence.ui.RotateImageView;
import com.almalence.util.Util;

public class TemplateGUI extends GUI
{
	//GUI Layout
	private View								guiView;
	
	//Orientation listener
	private OrientationEventListener			orientListener;
	
	// Mode selector layout
	private ElementAdapter						modeAdapter;
	private List<View>							modeViews;
	private ViewGroup							activeMode					= null;
	private boolean								modeSelectorVisible			= false;
	
	// Assoc list for storing association between mode button and mode ID
	private Map<View, String>					buttonModeViewAssoc;

	private Thumbnail							mThumbnail;
	private RotateImageView						thumbnailView;

	private RotateImageView						shutterButton;
	
	@Override
	public void onStart()
	{
		// set orientation listener to rotate controls
		this.orientListener = new OrientationEventListener(ApplicationScreen.getMainContext())
		{
			@Override
			public void onOrientationChanged(int orientation)
			{
				if (orientation == ORIENTATION_UNKNOWN)
					return;

				final Display display = ((WindowManager) ApplicationScreen.instance.getSystemService(
						Context.WINDOW_SERVICE)).getDefaultDisplay();
				final int orientationProc = (display.getWidth() <= display.getHeight()) ? Configuration.ORIENTATION_PORTRAIT
						: Configuration.ORIENTATION_LANDSCAPE;
				final int rotation = display.getRotation();

				boolean remapOrientation = Util.shouldRemapOrientation(orientationProc, rotation);

				if (remapOrientation)
					orientation = (orientation - 90 + 360) % 360;

				TemplateGUI.mDeviceOrientation = Util.roundOrientation(orientation, TemplateGUI.mDeviceOrientation);

				TemplateGUI.mPreviousDeviceOrientation = AlmalenceGUI.mDeviceOrientation;

				ApplicationScreen.getPluginManager().onOrientationChanged(getDisplayOrientation());
			}
		};

	}

	@Override
	public void onStop()
	{
		mDeviceOrientation = 0;
	}

	@Override
	public void onPause()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onResume()
	{
		lockControls = false;
		orientListener.enable();
	}

	@Override
	public void onDestroy()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void createInitialGUI()
	{
		guiView = LayoutInflater.from(ApplicationScreen.getMainContext()).inflate(R.layout.gui_template_layout, null);
	}

	@Override
	public void onCreate()
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void onGUICreate()
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void onCaptureFinished()
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void onPostProcessingStarted()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onPostProcessingFinished()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onExportFinished()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onCameraCreate()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onPluginsInitialized()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onCameraSetup()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setupViewfinderPreviewSize(Size previewSize)
	{
		float cameraAspect = (float) previewSize.getWidth() / previewSize.getHeight();

		RelativeLayout ll = (RelativeLayout) ApplicationScreen.instance.findViewById(R.id.mainLayout1);

		int previewSurfaceWidth = ll.getWidth();
		int previewSurfaceHeight = ll.getHeight();
		float surfaceAspect = (float) previewSurfaceHeight / previewSurfaceWidth;

		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);

		DisplayMetrics metrics = new DisplayMetrics();
		ApplicationScreen.instance.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int screen_height = metrics.heightPixels;

		lp.width = previewSurfaceWidth;
		lp.height = previewSurfaceHeight;
		if (Math.abs(surfaceAspect - cameraAspect) > 0.05d)
		{
			if (surfaceAspect > cameraAspect && (Math.abs(1 - cameraAspect) > 0.05d))
			{
				// if wide-screen - decrease width of surface
				lp.width = previewSurfaceWidth;

				lp.height = screen_height;
			} else if (surfaceAspect > cameraAspect)
			{
				// if wide-screen - decrease width of surface
				lp.width = previewSurfaceWidth;

				lp.height = previewSurfaceWidth;
			}
		}
		
		ApplicationScreen.getPreviewSurfaceView().setLayoutParams(lp);
		ApplicationScreen.setPreviewSurfaceLayoutWidth(lp.width);
		ApplicationScreen.setPreviewSurfaceLayoutHeight(lp.height);
		guiView.findViewById(R.id.fullscreenLayout).setLayoutParams(lp);

	}

	@Override
	public void menuButtonPressed()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onButtonClick(View button)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void hideSecondaryMenus()
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected void addPluginViews(Map<View, ViewfinderZone> views_map)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void addViewQuick(View view, ViewfinderZone zone)
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected void removePluginViews(Map<View, ViewfinderZone> views_map)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void removeViewQuick(View view)
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected void addInfoView(View view, LayoutParams viewLayoutParams)
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected void removeInfoView(View view)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void addMode(View mode)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void SetModeSelected(View v)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void hideModes()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public int getMaxModeViewWidth()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxModeViewHeight()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMinPluginViewHeight()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMinPluginViewWidth()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxPluginViewHeight()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxPluginViewWidth()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getSceneIcon(int sceneMode)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getWBIcon(int wb)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getFocusIcon(int focusMode)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getFlashIcon(int flashMode)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getISOIcon(int isoMode)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean onTouch(View view, MotionEvent e)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onClick(View view)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onHardwareShutterButtonPressed()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onHardwareFocusButtonPressed()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onVolumeBtnExpo(int keyCode)
	{
		// TODO Auto-generated method stub

	}

	@Override
	@TargetApi(14)
	public void setFocusParameters()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setShutterIcon(ShutterButton id)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onKeyDown(boolean isFromMain, int keyCode, KeyEvent event)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void disableCameraParameter(CameraParameter iParam,
			boolean bDisable, boolean bInitMenu)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void startProcessingAnimation()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void processingBlockUI()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void startContinuousCaptureIndication()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void stopCaptureIndication()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void showCaptureIndication()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public float getScreenDensity()
	{
		// TODO Auto-generated method stub
		return 0;
	}
}
