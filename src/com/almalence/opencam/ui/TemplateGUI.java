package com.almalence.opencam.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

import com.almalence.googsharing.Thumbnail;
import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.ConfigParser;
import com.almalence.opencam.Plugin;
import com.almalence.opencam.PluginType;
import com.almalence.opencam.TemplateScreen;
import com.almalence.opencam.Mode;
import com.almalence.opencam.Plugin.ViewfinderZone;
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.cameracontroller.CameraController.Size;
import com.almalence.opencam.R;
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
	
	private SonyCameraDeviceExplorer			sonyCameraDeviceExplorer;
	
	// Assoc list for storing association between mode button and mode ID
	private Map<View, String>					buttonModeViewAssoc;

	private Thumbnail							mThumbnail;
	private RotateImageView						thumbnailView;

	private RotateImageView						shutterButton;
	
	
	public TemplateGUI()
	{
		modeAdapter = new ElementAdapter();
		modeViews = new ArrayList<View>();
		buttonModeViewAssoc = new HashMap<View, String>();
	}
	
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

				TemplateGUI.mPreviousDeviceOrientation = TemplateGUI.mDeviceOrientation;

				ApplicationScreen.getPluginManager().onOrientationChanged(getDisplayOrientation());
				
				if (sonyCameraDeviceExplorer != null)
					sonyCameraDeviceExplorer.setOrientation();
			}
		};

	}
	
	

	@Override
	public void onStop()
	{
		removePluginViews();
		mDeviceOrientation = 0;
		mPreviousDeviceOrientation = 0;
	}

	@Override
	public void onPause()
	{
		orientListener.disable();
		if (modeSelectorVisible)
			hideModeList();
		
		lockControls = false;
		guiView.findViewById(R.id.buttonGallery).setEnabled(true);
		guiView.findViewById(R.id.buttonShutter).setEnabled(true);
		guiView.findViewById(R.id.buttonSelectMode).setEnabled(true);
		ApplicationScreen.getPluginManager().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_UNLOCKED);
	}
	
	@Override
	public void showSonyCameraDeviceExplorer()
	{
		sonyCameraDeviceExplorer.showExplorer();
	}

	@Override
	public void hideSonyCameraDeviceExplorer()
	{
		sonyCameraDeviceExplorer.hideExplorer();
	}

	@Override
	public void onResume()
	{
		ApplicationScreen.instance.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				TemplateGUI.this.updateThumbnailButton();
			}
		});
		
		lockControls = false;
		orientListener.enable();
		
		// Create select mode button with appropriate icon
		createMergedSelectModeButton();
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
		
		// Add GUI Layout to main layout of OpenCamera
		((RelativeLayout) ApplicationScreen.instance.findViewById(R.id.mainLayout1)).addView(guiView);
	}

	@Override
	public void onCreate()
	{
		thumbnailView = (RotateImageView) guiView.findViewById(R.id.buttonGallery);

		((RelativeLayout) ApplicationScreen.instance.findViewById(R.id.mainLayout1)).setOnTouchListener(ApplicationScreen.instance);
		
		shutterButton = ((RotateImageView) guiView.findViewById(R.id.buttonShutter));
		
		// Sony remote camera
		sonyCameraDeviceExplorer = new SonyCameraDeviceExplorer(guiView);
		// -- Sony remote camera
	}

	@Override
	public void onGUICreate()
	{
		// Recreate plugin views
		removePluginViews();
		createPluginViews();
				
		ApplicationScreen.instance.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				TemplateGUI.this.updateThumbnailButton();
			}
		});
		
		final View mainButtons = guiView.findViewById(R.id.mainButtons);
		mainButtons.bringToFront();
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
		// stop animation
		if (processingAnim != null)
		{
			processingAnim.clearAnimation();
			processingAnim.setVisibility(View.GONE);
		}
				
		updateThumbnailButton();
		thumbnailView.invalidate();
	}

	@Override
	public void onCameraCreate()
	{
		if (CameraController.isExposureCompensationSupported())
			CameraController.setCameraExposureCompensation(0);
		
		if(CameraController.isSceneModeSupported())
		{
			final int[] supported_scene = CameraController.getSupportedSceneModes();
			if (CameraController.isModeAvailable(supported_scene, CameraParameters.SCENE_MODE_AUTO))
				CameraController.setCameraSceneMode(CameraParameters.SCENE_MODE_AUTO);
		}
		
		if(CameraController.isWhiteBalanceSupported())
		{
			final int[] supported_wb = CameraController.getSupportedWhiteBalance();
			if (CameraController.isModeAvailable(supported_wb, CameraParameters.WB_MODE_AUTO))
				CameraController.setCameraWhiteBalance(CameraParameters.WB_MODE_AUTO);
		}
		
		if(CameraController.isFocusModeSupported())
		{
			final int[] supported_focus = CameraController.getSupportedFocusModes();
			if (CameraController.isModeAvailable(supported_focus, CameraParameters.AF_MODE_AUTO))
					CameraController.setCameraFocusMode(CameraParameters.AF_MODE_AUTO);
			else if (CameraController.isModeAvailable(supported_focus, CameraParameters.AF_MODE_CONTINUOUS_PICTURE))
				CameraController.setCameraFocusMode(CameraParameters.AF_MODE_CONTINUOUS_PICTURE);
			else if (CameraController.isModeAvailable(supported_focus, CameraParameters.AF_MODE_FIXED))
				CameraController.setCameraFocusMode(CameraParameters.AF_MODE_FIXED);
		}
		
		if(CameraController.isFlashModeSupported())
		{
			final int[] supported_flash = CameraController.getSupportedFlashModes();
			if (CameraController.isModeAvailable(supported_flash, CameraParameters.FLASH_MODE_AUTO))
				CameraController.setCameraWhiteBalance(CameraParameters.FLASH_MODE_AUTO);
		}
	}

	@Override
	public void onPluginsInitialized()
	{
		initModeList();
		
		if (modeSelectorVisible)
		{
			guiView.findViewById(R.id.modeLayout).bringToFront();
		}
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
		guiView.findViewById(R.id.specialPluginsLayout).setLayoutParams(lp);

	}

	@Override
	public void menuButtonPressed()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onButtonClick(View button)
	{
		if (!ApplicationScreen.isApplicationStarted())
			return;
		
		int id = button.getId();
		if (lockControls && R.id.buttonShutter != id)
			return;
		
		if (modeSelectorVisible && (R.id.buttonSelectMode != id))
		{
			hideModeList();
			return;
		}
		
		switch (id)
		{
		// BOTTOM BUTTONS - Modes, Shutter
		case R.id.buttonSelectMode:
			if (!modeSelectorVisible)
				showModeList();
			else
				hideModeList();
			break;

		case R.id.buttonShutter:
			shutterButtonPressed();
			break;

		case R.id.buttonGallery:
			openGallery(false);
			break;
		}

	}

	@Override
	public void hideSecondaryMenus()
	{
		if (guiView.findViewById(R.id.modeLayout).getVisibility() == View.GONE)
			return;
		
		guiView.findViewById(R.id.modeLayout).setVisibility(View.GONE);
		ApplicationScreen.getPluginManager().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_UNLOCKED);
	}

	@Override
	protected void addPluginViews(Map<View, ViewfinderZone> views_map)
	{
		Set<View> view_set = views_map.keySet();
		Iterator<View> it = view_set.iterator();
		while (it.hasNext())
		{

			try
			{
				View view = it.next();
				Plugin.ViewfinderZone desire_zone = views_map.get(view);

				android.widget.RelativeLayout.LayoutParams viewLayoutParams = (android.widget.RelativeLayout.LayoutParams) view
						.getLayoutParams();
				viewLayoutParams = this.getTunedPluginLayoutParams(view, desire_zone, viewLayoutParams);

				if (viewLayoutParams == null) // No free space on plugin's
												// layout
					return;

				view.setLayoutParams(viewLayoutParams);
				if (view.getParent() != null)
					((ViewGroup) view.getParent()).removeView(view);

				if (desire_zone == Plugin.ViewfinderZone.VIEWFINDER_ZONE_FULLSCREEN
						|| desire_zone == Plugin.ViewfinderZone.VIEWFINDER_ZONE_CENTER)
					((RelativeLayout) guiView.findViewById(R.id.fullscreenLayout)).addView(view, 0,
							(ViewGroup.LayoutParams) viewLayoutParams);
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e("TemplateGUI", "addPluginViews exception: " + e.getMessage());
			}
		}
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
		if (modeSelectorVisible)
			hideModeList();

		hideSecondaryMenus();
		
		if (!modeSelectorVisible)
			// call onTouch of active vf and capture plugins
			ApplicationScreen.getPluginManager().onTouch(view, e);
		
		return false;
	}

	@Override
	public void onClick(View view)
	{
		hideSecondaryMenus();
	}

	@Override
	public void onHardwareShutterButtonPressed()
	{
		ApplicationScreen.getPluginManager().onShutterClick();
	}

	@Override
	public void onHardwareFocusButtonPressed()
	{
		ApplicationScreen.getPluginManager().onFocusButtonClick();
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
		int res = 0;
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			if (modeSelectorVisible)
			{
				hideModeList();
				res++;
			}
		}

		if (keyCode == KeyEvent.KEYCODE_CAMERA)
		{
			if (modeSelectorVisible)
			{
				hideModeList();
				return false;
			}
			shutterButtonPressed();
			return true;
		}

		// check if back button pressed and processing is in progress
		if (res == 0)
			if (keyCode == KeyEvent.KEYCODE_BACK)
			{
				if (ApplicationScreen.getPluginManager().getProcessingCounter() != 0)
				{
					// splash screen about processing
					AlertDialog.Builder builder = new AlertDialog.Builder(ApplicationScreen.instance)
							.setTitle("Processing...")
							.setMessage(ApplicationScreen.getAppResources().getString(R.string.processing_not_finished))
							.setPositiveButton("Ok", new DialogInterface.OnClickListener()
							{
								public void onClick(DialogInterface dialog, int which)
								{
									// continue with delete
									dialog.cancel();
								}
							});
					AlertDialog alert = builder.create();
					alert.show();
				}
			}

		return res > 0 ? true : false;

	}

	@Override
	public void disableCameraParameter(CameraParameter iParam,
			boolean bDisable, boolean bInitMenu)
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
		((RotateImageView) guiView.findViewById(R.id.buttonShutter)).setImageResource(R.drawable.button_shutter);
	}

	@Override
	public void showCaptureIndication()
	{
		new CountDownTimer(400, 200)
		{
			public void onTick(long millisUntilFinished)
			{
				((RotateImageView) guiView.findViewById(R.id.buttonShutter))
						.setImageResource(R.drawable.gui_almalence_shutter_pressed);
			}

			public void onFinish()
			{
				if (!ApplicationScreen.getPluginManager().getActiveModeID().equals("video"))
				{
					((RotateImageView) guiView.findViewById(R.id.buttonShutter))
							.setImageResource(R.drawable.button_shutter);
				}

			}
		}.start();

	}
	
	
	@Override
	public float getScreenDensity()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	
	
	
	/*
	 * Supplementary methods for template gui	
	 */
	private void createMergedSelectModeButton()
	{
		// create merged image for select mode button
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		String defaultModeName = prefs.getString(TemplateScreen.sDefaultModeName, "");
		Mode mode = ConfigParser.getInstance().getMode(defaultModeName);
		try
		{
			Bitmap bm = null;
			Bitmap iconBase = BitmapFactory.decodeResource(ApplicationScreen.getMainContext().getResources(),
					R.drawable.gui_almalence_select_mode);
			Bitmap iconOverlay = BitmapFactory.decodeResource(
					ApplicationScreen.getMainContext().getResources(),
					ApplicationScreen
							.instance
							.getResources()
							.getIdentifier(CameraController.isUseSuperMode() ? mode.iconHAL : mode.icon, "drawable",
									ApplicationScreen.instance.getPackageName()));
			iconOverlay = Bitmap.createScaledBitmap(iconOverlay, (int) (iconBase.getWidth() / 1.8),
					(int) (iconBase.getWidth() / 1.8), false);

			bm = mergeImage(iconBase, iconOverlay);
			bm = Bitmap.createScaledBitmap(bm,
					(int) (ApplicationScreen.getMainContext().getResources().getDimension(R.dimen.paramsLayoutHeight)),
					(int) (ApplicationScreen.getMainContext().getResources().getDimension(R.dimen.paramsLayoutHeight)), false);
			((RotateImageView) guiView.findViewById(R.id.buttonSelectMode)).setImageBitmap(bm);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	// helper function to draw one bitmap on another
	private Bitmap mergeImage(Bitmap base, Bitmap overlay)
	{
		int adWDelta = (int) (base.getWidth() - overlay.getWidth()) / 2;
		int adHDelta = (int) (base.getHeight() - overlay.getHeight()) / 2;

		Bitmap mBitmap = Bitmap.createBitmap(base.getWidth(), base.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(mBitmap);
		canvas.drawBitmap(base, 0, 0, null);
		canvas.drawBitmap(overlay, adWDelta, adHDelta, null);

		return mBitmap;
	}	

	
	// controls if info about new mode shown or not. to prevent from double info
	private void initModeList()
	{
		boolean initModeList = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getBoolean(TemplateScreen.sInitModeListPref, false);
		if (activeMode != null && !initModeList)
		{
			return;
		}
		
		PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).edit().putBoolean(TemplateScreen.sInitModeListPref, false).commit();

		modeViews.clear();
		buttonModeViewAssoc.clear();

		List<Mode> hash = ConfigParser.getInstance().getList();
		Iterator<Mode> it = hash.iterator();

		int mode_number = 0;
		while (it.hasNext())
		{
			try
			{
				Mode tmp = it.next();

				LayoutInflater inflator = ApplicationScreen.instance.getLayoutInflater();
				View mode = inflator.inflate(R.layout.gui_almalence_select_mode_grid_element, null, false);
				// set some mode icon
				((ImageView) mode.findViewById(R.id.modeImage)).setImageResource(ApplicationScreen
						.instance
						.getResources()
						.getIdentifier(CameraController.isUseSuperMode() ? tmp.iconHAL : tmp.icon, "drawable",
								ApplicationScreen.instance.getPackageName()));

				int id = ApplicationScreen
						.instance
						.getResources()
						.getIdentifier(CameraController.isUseSuperMode() ? tmp.modeNameHAL : tmp.modeName, "string",
								ApplicationScreen.instance.getPackageName());
				String modename = ApplicationScreen.getAppResources().getString(id);

				((TextView) mode.findViewById(R.id.modeText)).setText(modename);
				if (mode_number == 0)
					mode.setOnTouchListener(new OnTouchListener()
					{
						@Override
						public boolean onTouch(View v, MotionEvent event)
						{
							if (event.getAction() == MotionEvent.ACTION_CANCEL)
							{
								return changeMode(v);
							}
							return false;
						}

					});
				mode.setOnClickListener(new OnClickListener()
				{
					public void onClick(View v)
					{
						changeMode(v);
					}
				});
				buttonModeViewAssoc.put(mode, tmp.modeID);
				modeViews.add(mode);

				// select active mode in grid with frame
				if (ApplicationScreen.getPluginManager().getActiveModeID() == tmp.modeID)
				{
					mode.findViewById(R.id.modeSelectLayout2).setBackgroundResource(
							R.drawable.thumbnail_background_selected_inner);

					activeMode = (ViewGroup) mode;
				}
				mode_number++;
			} catch (Exception e)
			{
				e.printStackTrace();
			} catch (OutOfMemoryError e)
			{
				e.printStackTrace();
			}
		}

		modeAdapter.Elements = modeViews;
	}

	private boolean changeMode(View v)
	{
		activeMode.findViewById(R.id.modeSelectLayout2).setBackgroundResource(R.drawable.underlayer);
		v.findViewById(R.id.modeSelectLayout2).setBackgroundResource(R.drawable.thumbnail_background_selected_inner);
		activeMode = (ViewGroup) v;

		hideModeList();

		// get mode associated with pressed button
		String key = buttonModeViewAssoc.get(v);
		Mode mode = ConfigParser.getInstance().getMode(key);
		// if selected the same mode - do not reinitialize camera
		// and other objects.
		if (ApplicationScreen.getPluginManager().getActiveModeID() == mode.modeID)
			return false;

		final Mode tmpActiveMode = mode;

		if (mode.modeID.equals("video"))
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
			if (prefs.getBoolean("videoStartStandardPref", false))
			{
				ApplicationScreen.getPluginManager().onPause(true);
				Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
				ApplicationScreen.instance.startActivity(intent);
				return true;
			}
		}

		new CountDownTimer(100, 100)
		{
			public void onTick(long millisUntilFinished)
			{
				// Not used
			}

			public void onFinish()
			{
				ApplicationScreen.getPluginManager().switchMode(tmpActiveMode);
			}
		}.start();

		// set modes icon inside mode selection icon
		Bitmap bm = null;
		Bitmap iconBase = BitmapFactory.decodeResource(ApplicationScreen.getMainContext().getResources(),
				R.drawable.gui_almalence_select_mode);
		Bitmap iconOverlay = BitmapFactory.decodeResource(
				ApplicationScreen.getMainContext().getResources(),
				ApplicationScreen
						.instance
						.getResources()
						.getIdentifier(CameraController.isUseSuperMode() ? mode.iconHAL : mode.icon, "drawable",
								ApplicationScreen.instance.getPackageName()));
		iconOverlay = Bitmap.createScaledBitmap(iconOverlay, (int) (iconBase.getWidth() / 1.8),
				(int) (iconBase.getWidth() / 1.8), false);

		bm = mergeImage(iconBase, iconOverlay);
		bm = Bitmap.createScaledBitmap(bm,
				(int) (ApplicationScreen.getMainContext().getResources().getDimension(R.dimen.mainButtonHeightSelect)),
				(int) (ApplicationScreen.getMainContext().getResources().getDimension(R.dimen.mainButtonHeightSelect)), false);
		((RotateImageView) guiView.findViewById(R.id.buttonSelectMode)).setImageBitmap(bm);

		int rid = ApplicationScreen.getAppResources().getIdentifier(tmpActiveMode.howtoText, "string",
				ApplicationScreen.instance.getPackageName());
		String howto = "";
		if (rid != 0)
			howto = ApplicationScreen.getAppResources().getString(rid);
		
		return false;
	}
	
	
	private void showModeList()
	{
		hideSecondaryMenus();

		initModeList();
		DisplayMetrics metrics = new DisplayMetrics();
		ApplicationScreen.instance.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int width = metrics.widthPixels;
		int modeHeightByWidth = (int) (width / 3 - 5 * metrics.density);
		int modeHeightByDimen = Math.round(ApplicationScreen.getAppResources().getDimension(R.dimen.gridModeImageSize)
				+ ApplicationScreen.getAppResources().getDimension(R.dimen.gridModeTextLayoutSize));

		int modeHeight = modeHeightByDimen > modeHeightByWidth ? modeHeightByWidth : modeHeightByDimen;

		AbsListView.LayoutParams params = new AbsListView.LayoutParams(LayoutParams.WRAP_CONTENT, modeHeight);

		for (int i = 0; i < modeViews.size(); i++)
		{
			View mode = modeViews.get(i);
			mode.setLayoutParams(params);
		}

		GridView gridview = (GridView) guiView.findViewById(R.id.modeGrid);
		gridview.setAdapter(modeAdapter);

		gridview.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				return false;
			}
		});

		((RelativeLayout) guiView.findViewById(R.id.modeLayout)).setVisibility(View.VISIBLE);
		(guiView.findViewById(R.id.modeGrid)).setVisibility(View.VISIBLE);
		guiView.findViewById(R.id.modeLayout).bringToFront();

		modeSelectorVisible = true;

		ApplicationScreen.getPluginManager().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_LOCKED);
	}

	private void hideModeList()
	{
		RelativeLayout gridview = (RelativeLayout) guiView.findViewById(R.id.modeLayout);

		Animation gone = AnimationUtils
				.loadAnimation(ApplicationScreen.instance, R.anim.gui_almalence_modelist_invisible);
		gone.setFillAfter(true);

		gridview.setAnimation(gone);
		(guiView.findViewById(R.id.modeGrid)).setAnimation(gone);

		gridview.setVisibility(View.GONE);
		(guiView.findViewById(R.id.modeGrid)).setVisibility(View.GONE);

		gone.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationEnd(Animation animation)
			{
				guiView.findViewById(R.id.modeLayout).clearAnimation();
				guiView.findViewById(R.id.modeGrid).clearAnimation();
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
				// Not used
			}

			@Override
			public void onAnimationStart(Animation animation)
			{
				// Not used
			}
		});

		modeSelectorVisible = false;

		ApplicationScreen.getPluginManager().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_UNLOCKED);
	}
	
	
	private UpdateThumbnailButtonTask	t	= null;

	public void updateThumbnailButton()
	{

		t = new UpdateThumbnailButtonTask(ApplicationScreen.instance);
		t.execute();

		new CountDownTimer(1000, 1000)
		{
			public void onTick(long millisUntilFinished)
			{
				// Not used
			}

			public void onFinish()
			{
				try
				{
					if (t != null && t.getStatus() != AsyncTask.Status.FINISHED)
					{
						t.cancel(true);
					}
				} catch (Exception e)
				{
					Log.e("TemplateGUI", "Can't stop thumbnail processing");
				}
			}
		}.start();
	}

	private class UpdateThumbnailButtonTask extends AsyncTask<Void, Void, Void>
	{
		public UpdateThumbnailButtonTask(Context context)
		{
		}

		@Override
		protected void onPreExecute()
		{
			// do nothing.
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			mThumbnail = Thumbnail.getLastThumbnail(ApplicationScreen.instance.getContentResolver());
			return null;
		}

		@Override
		protected void onPostExecute(Void v)
		{
			if (mThumbnail != null)
			{
				final Bitmap bitmap = mThumbnail.getBitmap();

				if (bitmap != null)
				{
					if (bitmap.getHeight() > 0 && bitmap.getWidth() > 0)
					{
						System.gc();

						try
						{
							Bitmap bm = Thumbnail.getRoundedCornerBitmap(bitmap, (int) (ApplicationScreen.getMainContext()
									.getResources().getDimension(R.dimen.mainButtonHeight) * 1.2), (int) (ApplicationScreen
									.getMainContext().getResources().getDimension(R.dimen.mainButtonHeight) / 1.1));

							thumbnailView.setImageBitmap(bm);
						} catch (Exception e)
						{
							Log.v("TemplateGUI", "Can't set thumbnail");
						}
					}
				}
			} else
			{
				try
				{
					Bitmap bitmap = Bitmap.createBitmap(96, 96, Config.ARGB_8888);
					Canvas canvas = new Canvas(bitmap);
					canvas.drawColor(Color.BLACK);
					Bitmap bm = Thumbnail.getRoundedCornerBitmap(bitmap, (int) (ApplicationScreen.getMainContext()
							.getResources().getDimension(R.dimen.mainButtonHeight) * 1.2), (int) (ApplicationScreen
							.getMainContext().getResources().getDimension(R.dimen.mainButtonHeight) / 1.1));
					thumbnailView.setImageBitmap(bm);
				} catch (Exception e)
				{
					Log.v("TemplateGUI", "Can't set thumbnail");
				}
			}
		}
	}
	
	private ImageView	processingAnim;

	@Override
	public void startProcessingAnimation()
	{
		if (processingAnim != null && processingAnim.getVisibility() == View.VISIBLE)
			return;

		processingAnim = ((ImageView) guiView.findViewById(R.id.buttonGallery2));
		processingAnim.setVisibility(View.VISIBLE);

		int height = (int) ApplicationScreen.getAppResources().getDimension(R.dimen.paramsLayoutHeightScanner);
		int width = (int) ApplicationScreen.getAppResources().getDimension(R.dimen.paramsLayoutHeightScanner);
		Animation rotation = new RotateAnimation(0, 360, width / 2, height / 2);
		rotation.setDuration(800);
		rotation.setInterpolator(new LinearInterpolator());
		rotation.setRepeatCount(1000);

		processingAnim.startAnimation(rotation);
	}
	
	private void shutterButtonPressed()
	{
		ApplicationScreen.getPluginManager().onShutterClick();
	}
	
	
	public void openGallery(boolean isOpenExternal)
	{
		if (mThumbnail == null)
			return;

		Uri uri = this.mThumbnail.getUri();

		ApplicationScreen.getPluginManager().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_STOP_CAPTURE);

		openExternalGallery(uri);
	}

	private void openExternalGallery(Uri uri)
	{
		try
		{
			ApplicationScreen.instance.startActivity(new Intent("com.android.camera.action.REVIEW", uri));
		} catch (ActivityNotFoundException ex)
		{
			try
			{
				ApplicationScreen.instance.startActivity(new Intent(Intent.ACTION_VIEW, uri));
			} catch (ActivityNotFoundException e)
			{
				Log.e("AlmalenceGUI", "review image fail. uri=" + uri, e);
			}
		}
	}
	
	private void removePluginViews()
	{
		List<View> fullscreenView = new ArrayList<View>();
		RelativeLayout fullscreenLayout = (RelativeLayout) ApplicationScreen.instance.findViewById(R.id.fullscreenLayout);
		for (int i = 0; i < fullscreenLayout.getChildCount(); i++)
			fullscreenView.add(fullscreenLayout.getChildAt(i));

		for (int j = 0; j < fullscreenView.size(); j++)
		{
			View view = fullscreenView.get(j);
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);

			fullscreenLayout.removeView(view);
		}
	}
	
	public void createPluginViews()
	{
		createPluginViews(PluginType.ViewFinder);
		createPluginViews(PluginType.Capture);
		createPluginViews(PluginType.Processing);
		createPluginViews(PluginType.Filter);
		createPluginViews(PluginType.Export);
	}

	private void createPluginViews(PluginType type)
	{
		Map<View, Plugin.ViewfinderZone> plugin_views = null;

		List<Plugin> plugins = ApplicationScreen.getPluginManager().getActivePlugins(type);
		if (!plugins.isEmpty())
		{
			for (int i = 0; i < plugins.size(); i++)
			{
				Plugin plugin = plugins.get(i);
				if (plugin != null)
				{
					plugin_views = plugin.getPluginViews();
					addPluginViews(plugin_views);
				}
			}
		}
	}
	
	/************************************************************************************************
	 * 
	 * DEFINITION OF PLACE ON LAYOUT FOR PLUGIN'S VIEWS
	 * 
	 ***********************************************************************************************/

	protected android.widget.RelativeLayout.LayoutParams getTunedPluginLayoutParams(View view,
			Plugin.ViewfinderZone desire_zone, android.widget.RelativeLayout.LayoutParams currParams)
	{
		if (currParams == null)
			currParams = new android.widget.RelativeLayout.LayoutParams(getMinPluginViewWidth(),
					getMinPluginViewHeight());

		if(desire_zone == Plugin.ViewfinderZone.VIEWFINDER_ZONE_FULLSCREEN)
		{
			Log.e("GUI", "VIEWFINDER_ZONE_FULLSCREEN");
			currParams.width = ApplicationScreen.instance.getPreviewSize() != null ? ApplicationScreen.instance
					.getPreviewSize().getWidth() : 0;
			currParams.height = ApplicationScreen.instance.getPreviewSize() != null ? ApplicationScreen.instance
					.getPreviewSize().getHeight() : 0;
			currParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			return currParams;
		}

		return currParams;
	}

	@Override
	public void showHelp(String modeName, String text, int imageID, String Prefs)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCameraModeGUI(int mode)
	{
		// TODO Auto-generated method stub
		
	}
}
