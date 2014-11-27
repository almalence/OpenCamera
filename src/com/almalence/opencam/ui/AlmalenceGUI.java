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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
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
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.almalence.googsharing.Thumbnail;

import com.almalence.ui.Panel;
import com.almalence.ui.Panel.OnPanelListener;
import com.almalence.ui.RotateImageView;
import com.almalence.util.AppEditorNotifier;
import com.almalence.util.Util;
//<!-- -+-
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.ConfigParser;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.Mode;
import com.almalence.opencam.Plugin;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginType;
import com.almalence.opencam.Preferences;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;

//-+- -->
/* <!-- +++
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.CameraParameters;
 import com.almalence.opencam_plus.ConfigParser;
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.Mode;
 import com.almalence.opencam_plus.Plugin;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.PluginType;
 import com.almalence.opencam_plus.Preferences;
 import com.almalence.opencam_plus.R;

 +++ --> */

/***
 * AlmalenceGUI is an instance of GUI class, implements current GUI
 ***/

public class AlmalenceGUI extends GUI implements SeekBar.OnSeekBarChangeListener, View.OnLongClickListener,
		View.OnClickListener
{
	private SharedPreferences	preferences;

	private static final int	INFO_ALL	= 0;
	private static final int	INFO_NO		= 1;
	private static final int	INFO_GRID	= 2;
	private static final int	INFO_PARAMS	= 3;
	private int					infoSet		= INFO_PARAMS;

	public enum ShutterButton
	{
		DEFAULT, RECORDER_START_WITH_PAUSE, RECORDER_START, RECORDER_STOP_WITH_PAUSE, RECORDER_STOP, RECORDER_RECORDING_WITH_PAUSE, RECORDER_RECORDING, RECORDER_PAUSED, TIMELAPSE_ACTIVE
	}

	public enum SettingsType
	{
		SCENE, WB, FOCUS, FLASH, ISO, METERING, CAMERA, EV, MORE
	}

	private OrientationEventListener			orientListener;

	// certain quick control visible
	private boolean								quickControlsVisible		= false;

	// Quick control customization variables
	private ElementAdapter						quickControlAdapter;
	private List<View>							quickControlChangeres;
	private View								currentQuickView			= null;
	// Current quick control to replace If qc customization layout is showing
	// now
	private boolean								quickControlsChangeVisible	= false;

	// Settings layout
	private ElementAdapter						settingsAdapter;
	private List<View>							settingsViews;
	private boolean								settingsControlsVisible		= false;
	// If quick settings layout is showing now

	// Mode selector layout
	private ElementAdapter						modeAdapter;
	private List<View>							modeViews;
	private ViewGroup							activeMode					= null;
	private boolean								modeSelectorVisible			= false;
	// If quick settings layout is showing now

	// <!-- -+-
	private AlmalenceStore						store;
	// -+- -->

	private SelfTimerAndPhotoTimeLapse			selfTimer;

	// Assoc list for storing association between mode button and mode ID
	private Map<View, String>					buttonModeViewAssoc;

	private Thumbnail							mThumbnail;
	private RotateImageView						thumbnailView;

	private RotateImageView						shutterButton;

	private static final Integer				ICON_EV						= R.drawable.gui_almalence_settings_exposure;
	private static final Integer				ICON_CAM					= R.drawable.gui_almalence_settings_changecamera;
	private static final Integer				ICON_SETTINGS				= R.drawable.gui_almalence_settings_more_settings;

	private static final int					FOCUS_AF_LOCK				= 10;

	// Lists of icons for camera parameters (scene mode, flash mode, focus mode,
	// white balance, iso)
	private static final Map<Integer, Integer>	ICONS_SCENE					= new HashMap<Integer, Integer>()
																			{
																				{
																					put(CameraParameters.SCENE_MODE_AUTO,
																							R.drawable.gui_almalence_settings_scene_auto);
																					put(CameraParameters.SCENE_MODE_ACTION,
																							R.drawable.gui_almalence_settings_scene_action);
																					put(CameraParameters.SCENE_MODE_PORTRAIT,
																							R.drawable.gui_almalence_settings_scene_portrait);
																					put(CameraParameters.SCENE_MODE_LANDSCAPE,
																							R.drawable.gui_almalence_settings_scene_landscape);
																					put(CameraParameters.SCENE_MODE_NIGHT,
																							R.drawable.gui_almalence_settings_scene_night);
																					put(CameraParameters.SCENE_MODE_NIGHT_PORTRAIT,
																							R.drawable.gui_almalence_settings_scene_nightportrait);
																					put(CameraParameters.SCENE_MODE_THEATRE,
																							R.drawable.gui_almalence_settings_scene_theater);
																					put(CameraParameters.SCENE_MODE_BEACH,
																							R.drawable.gui_almalence_settings_scene_beach);
																					put(CameraParameters.SCENE_MODE_SNOW,
																							R.drawable.gui_almalence_settings_scene_snow);
																					put(CameraParameters.SCENE_MODE_SUNSET,
																							R.drawable.gui_almalence_settings_scene_sunset);
																					put(CameraParameters.SCENE_MODE_STEADYPHOTO,
																							R.drawable.gui_almalence_settings_scene_steadyphoto);
																					put(CameraParameters.SCENE_MODE_FIREWORKS,
																							R.drawable.gui_almalence_settings_scene_fireworks);
																					put(CameraParameters.SCENE_MODE_SPORTS,
																							R.drawable.gui_almalence_settings_scene_sports);
																					put(CameraParameters.SCENE_MODE_PARTY,
																							R.drawable.gui_almalence_settings_scene_party);
																					put(CameraParameters.SCENE_MODE_CANDLELIGHT,
																							R.drawable.gui_almalence_settings_scene_candlelight);
																					put(CameraParameters.SCENE_MODE_BARCODE,
																							R.drawable.gui_almalence_settings_scene_barcode);
																				}
																			};

	private static final Map<Integer, Integer>	ICONS_WB					= new HashMap<Integer, Integer>()
																			{
																				{
																					put(CameraParameters.WB_MODE_AUTO,
																							R.drawable.gui_almalence_settings_wb_auto);
																					put(CameraParameters.WB_MODE_INCANDESCENT,
																							R.drawable.gui_almalence_settings_wb_incandescent);
																					put(CameraParameters.WB_MODE_FLUORESCENT,
																							R.drawable.gui_almalence_settings_wb_fluorescent);
																					put(CameraParameters.WB_MODE_WARM_FLUORESCENT,
																							R.drawable.gui_almalence_settings_wb_warmfluorescent);
																					put(CameraParameters.WB_MODE_DAYLIGHT,
																							R.drawable.gui_almalence_settings_wb_daylight);
																					put(CameraParameters.WB_MODE_CLOUDY_DAYLIGHT,
																							R.drawable.gui_almalence_settings_wb_cloudydaylight);
																					put(CameraParameters.WB_MODE_TWILIGHT,
																							R.drawable.gui_almalence_settings_wb_twilight);
																					put(CameraParameters.WB_MODE_SHADE,
																							R.drawable.gui_almalence_settings_wb_shade);
																				}
																			};

	private static final Map<Integer, Integer>	ICONS_FOCUS					= new HashMap<Integer, Integer>()
																			{
																				{
																					put(CameraParameters.AF_MODE_AUTO,
																							R.drawable.gui_almalence_settings_focus_auto);
																					put(CameraParameters.AF_MODE_INFINITY,
																							R.drawable.gui_almalence_settings_focus_infinity);
																					put(CameraParameters.AF_MODE_NORMAL,
																							R.drawable.gui_almalence_settings_focus_normal);
																					put(CameraParameters.AF_MODE_MACRO,
																							R.drawable.gui_almalence_settings_focus_macro);
																					put(CameraParameters.AF_MODE_FIXED,
																							R.drawable.gui_almalence_settings_focus_fixed);
																					put(CameraParameters.AF_MODE_EDOF,
																							R.drawable.gui_almalence_settings_focus_edof);
																					put(CameraParameters.AF_MODE_CONTINUOUS_VIDEO,
																							R.drawable.gui_almalence_settings_focus_continiuousvideo);
																					put(CameraParameters.AF_MODE_CONTINUOUS_PICTURE,
																							R.drawable.gui_almalence_settings_focus_continiuouspicture);
																					put(FOCUS_AF_LOCK,
																							R.drawable.gui_almalence_settings_focus_aflock);
																				}
																			};

	private static final Map<Integer, Integer>	ICONS_FLASH					= new HashMap<Integer, Integer>()
																			{
																				{
																					put(CameraParameters.FLASH_MODE_OFF,
																							R.drawable.gui_almalence_settings_flash_off);
																					put(CameraParameters.FLASH_MODE_AUTO,
																							R.drawable.gui_almalence_settings_flash_auto);
																					put(CameraParameters.FLASH_MODE_SINGLE,
																							R.drawable.gui_almalence_settings_flash_on);
																					put(CameraParameters.FLASH_MODE_REDEYE,
																							R.drawable.gui_almalence_settings_flash_redeye);
																					put(CameraParameters.FLASH_MODE_TORCH,
																							R.drawable.gui_almalence_settings_flash_torch);
																				}
																			};

	private static final Map<Integer, Integer>	ICONS_ISO					= new HashMap<Integer, Integer>()
																			{
																				{
																					put(CameraParameters.ISO_AUTO,
																							R.drawable.gui_almalence_settings_iso_auto);
																					put(CameraParameters.ISO_50,
																							R.drawable.gui_almalence_settings_iso_50);
																					put(CameraParameters.ISO_100,
																							R.drawable.gui_almalence_settings_iso_100);
																					put(CameraParameters.ISO_200,
																							R.drawable.gui_almalence_settings_iso_200);
																					put(CameraParameters.ISO_400,
																							R.drawable.gui_almalence_settings_iso_400);
																					put(CameraParameters.ISO_800,
																							R.drawable.gui_almalence_settings_iso_800);
																					put(CameraParameters.ISO_1600,
																							R.drawable.gui_almalence_settings_iso_1600);
																					put(CameraParameters.ISO_3200,
																							R.drawable.gui_almalence_settings_iso_3200);
																				}
																			};

	private static final Map<String, Integer>	ICONS_DEFAULT_ISO			= new HashMap<String, Integer>()
																			{
																				{
																					put(MainScreen
																							.getInstance()
																							.getResources()
																							.getString(
																									R.string.isoAutoDefaultSystem),
																							R.drawable.gui_almalence_settings_iso_auto);
																					put(MainScreen
																							.getInstance()
																							.getResources()
																							.getString(
																									R.string.iso50DefaultSystem),
																							R.drawable.gui_almalence_settings_iso_50);
																					put(MainScreen
																							.getInstance()
																							.getResources()
																							.getString(
																									R.string.iso100DefaultSystem),
																							R.drawable.gui_almalence_settings_iso_100);
																					put(MainScreen
																							.getInstance()
																							.getResources()
																							.getString(
																									R.string.iso200DefaultSystem),
																							R.drawable.gui_almalence_settings_iso_200);
																					put(MainScreen
																							.getInstance()
																							.getResources()
																							.getString(
																									R.string.iso400DefaultSystem),
																							R.drawable.gui_almalence_settings_iso_400);
																					put(MainScreen
																							.getInstance()
																							.getResources()
																							.getString(
																									R.string.iso800DefaultSystem),
																							R.drawable.gui_almalence_settings_iso_800);
																					put(MainScreen
																							.getInstance()
																							.getResources()
																							.getString(
																									R.string.iso1600DefaultSystem),
																							R.drawable.gui_almalence_settings_iso_1600);
																					put(MainScreen
																							.getInstance()
																							.getResources()
																							.getString(
																									R.string.iso3200DefaultSystem),
																							R.drawable.gui_almalence_settings_iso_3200);
																				}
																			};

	private static final Map<Integer, Integer>	ICONS_METERING				= new HashMap<Integer, Integer>()
																			{
																				{
																					put(0,
																							R.drawable.gui_almalence_settings_metering_auto);
																					put(1,
																							R.drawable.gui_almalence_settings_metering_matrix);
																					put(2,
																							R.drawable.gui_almalence_settings_metering_center);
																					put(3,
																							R.drawable.gui_almalence_settings_metering_spot);
																				}
																			};

	// List of localized names for camera parameters values
	private static final Map<Integer, String>	NAMES_SCENE					= new HashMap<Integer, String>()
																			{
																				{
																					put(CameraParameters.SCENE_MODE_AUTO,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.sceneAuto));
																					put(CameraParameters.SCENE_MODE_ACTION,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.sceneAction));
																					put(CameraParameters.SCENE_MODE_PORTRAIT,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.scenePortrait));
																					put(CameraParameters.SCENE_MODE_LANDSCAPE,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.sceneLandscape));
																					put(CameraParameters.SCENE_MODE_NIGHT,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.sceneNight));
																					put(CameraParameters.SCENE_MODE_NIGHT_PORTRAIT,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.sceneNightPortrait));
																					put(CameraParameters.SCENE_MODE_THEATRE,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.sceneTheatre));
																					put(CameraParameters.SCENE_MODE_BEACH,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.sceneBeach));
																					put(CameraParameters.SCENE_MODE_SNOW,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.sceneSnow));
																					put(CameraParameters.SCENE_MODE_SUNSET,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.sceneSunset));
																					put(CameraParameters.SCENE_MODE_STEADYPHOTO,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.sceneSteadyPhoto));
																					put(CameraParameters.SCENE_MODE_FIREWORKS,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.sceneFireworks));
																					put(CameraParameters.SCENE_MODE_SPORTS,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.sceneSports));
																					put(CameraParameters.SCENE_MODE_PARTY,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.sceneParty));
																					put(CameraParameters.SCENE_MODE_CANDLELIGHT,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.sceneCandlelight));
																					put(CameraParameters.SCENE_MODE_BARCODE,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.sceneBarcode));
																				}
																			};

	private static final Map<Integer, String>	NAMES_WB					= new HashMap<Integer, String>()
																			{
																				{
																					put(CameraParameters.WB_MODE_AUTO,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.wbAuto));
																					put(CameraParameters.WB_MODE_INCANDESCENT,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.wbIncandescent));
																					put(CameraParameters.WB_MODE_FLUORESCENT,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.wbFluorescent));
																					put(CameraParameters.WB_MODE_WARM_FLUORESCENT,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.wbWarmFluorescent));
																					put(CameraParameters.WB_MODE_DAYLIGHT,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.wbDaylight));
																					put(CameraParameters.WB_MODE_CLOUDY_DAYLIGHT,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.wbCloudyDaylight));
																					put(CameraParameters.WB_MODE_TWILIGHT,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.wbTwilight));
																					put(CameraParameters.WB_MODE_SHADE,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.wbShade));
																				}
																			};

	private static final Map<Integer, String>	NAMES_FOCUS					= new HashMap<Integer, String>()
																			{
																				{
																					put(CameraParameters.AF_MODE_AUTO,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.focusAuto));
																					put(CameraParameters.AF_MODE_INFINITY,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.focusInfinity));
																					put(CameraParameters.AF_MODE_NORMAL,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.focusNormal));
																					put(CameraParameters.AF_MODE_MACRO,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.focusMacro));
																					put(CameraParameters.AF_MODE_FIXED,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.focusFixed));
																					put(CameraParameters.AF_MODE_EDOF,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.focusEdof));
																					put(CameraParameters.AF_MODE_CONTINUOUS_VIDEO,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.focusContinuousVideo));
																					put(CameraParameters.AF_MODE_CONTINUOUS_PICTURE,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.focusContinuousPicture));
																				}
																			};

	private static final Map<Integer, String>	NAMES_FLASH					= new HashMap<Integer, String>()
																			{
																				{
																					put(CameraParameters.FLASH_MODE_OFF,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.flashOff));
																					put(CameraParameters.FLASH_MODE_AUTO,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.flashAuto));
																					put(CameraParameters.FLASH_MODE_SINGLE,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.flashOn));
																					put(CameraParameters.FLASH_MODE_REDEYE,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.flashRedEye));
																					put(CameraParameters.FLASH_MODE_TORCH,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.flashTorch));
																				}
																			};

	private static final Map<Integer, String>	NAMES_ISO					= new HashMap<Integer, String>()
																			{
																				{
																					put(CameraParameters.ISO_AUTO,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.isoAuto));
																					put(CameraParameters.ISO_50,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.iso50));
																					put(CameraParameters.ISO_100,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.iso100));
																					put(CameraParameters.ISO_200,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.iso200));
																					put(CameraParameters.ISO_400,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.iso400));
																					put(CameraParameters.ISO_800,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.iso800));
																					put(CameraParameters.ISO_1600,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.iso1600));
																					put(CameraParameters.ISO_3200,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.iso3200));
																				}
																			};

	private static final Map<Integer, String>	NAMES_METERING				= new HashMap<Integer, String>()
																			{
																				{
																					put(0,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.meteringAutoSystem));
																					put(1,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.meteringMatrixSystem));
																					put(2,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.meteringCenterSystem));
																					put(3,
																							MainScreen
																									.getInstance()
																									.getResources()
																									.getString(
																											R.string.meteringSpotSystem));
																				}
																			};

	private static final Map<String, String>	UNLOCK_MODE_PREFERENCES		= new HashMap<String, String>()
																			{
																				{
																					put("hdrmode",
																							"plugin_almalence_hdr");
																					put("movingobjects",
																							"plugin_almalence_moving_burst");
																					put("sequence",
																							"plugin_almalence_hdr");
																					put("groupshot",
																							"plugin_almalence_groupshot");
																					put("panorama_augmented",
																							"plugin_almalence_panorama");
																				}
																			};

	// Defining for top menu buttons (camera parameters settings)
	private static final int					MODE_EV						= R.id.evButton;
	private static final int					MODE_SCENE					= R.id.sceneButton;
	private static final int					MODE_WB						= R.id.wbButton;
	private static final int					MODE_FOCUS					= R.id.focusButton;
	private static final int					MODE_FLASH					= R.id.flashButton;
	private static final int					MODE_ISO					= R.id.isoButton;
	private static final int					MODE_CAM					= R.id.camerachangeButton;
	private static final int					MODE_MET					= R.id.meteringButton;

	private Map<Integer, View>					topMenuButtons;

	// Each plugin may have one top menu (and appropriate quick control) button
	private Map<String, View>					topMenuPluginButtons;

	// Current quick controls
	private View								quickControl1				= null;
	private View								quickControl2				= null;
	private View								quickControl3				= null;
	private View								quickControl4				= null;

	private ElementAdapter						scenemodeAdapter;
	private ElementAdapter						wbmodeAdapter;
	private ElementAdapter						focusmodeAdapter;
	private ElementAdapter						flashmodeAdapter;
	private ElementAdapter						isoAdapter;
	private ElementAdapter						meteringmodeAdapter;

	private Map<Integer, View>					sceneModeButtons;
	private Map<Integer, View>					wbModeButtons;
	private Map<Integer, View>					focusModeButtons;
	private Map<Integer, View>					flashModeButtons;
	private Map<Integer, View>					isoButtons;
	private Map<Integer, View>					meteringModeButtons;

	// Camera settings values which is exist at current device
	private List<View>							activeScene;
	private List<View>							activeWB;
	private List<View>							activeFocus;
	private List<View>							activeFlash;
	private List<View>							activeISO;
	private List<View>							activeMetering;

	private List<Integer>						activeSceneNames;
	private List<Integer>						activeWBNames;
	private List<Integer>						activeFocusNames;
	private List<Integer>						activeFlashNames;
	private List<Integer>						activeISONames;
	private List<Integer>						activeMeteringNames;

	private boolean								isEVEnabled					= true;
	private boolean								isSceneEnabled				= true;
	private boolean								isWBEnabled					= true;
	private boolean								isFocusEnabled				= true;
	private boolean								isFlashEnabled				= true;
	private boolean								isIsoEnabled				= true;
	private boolean								isCameraChangeEnabled		= true;
	private boolean								isMeteringEnabled			= true;

	// GUI Layout
	private View								guiView;

	// Current camera parameters
	private int									mEV							= 0;
	private int									mSceneMode					= -1;
	private int									mFlashMode					= -1;
	private int									mFocusMode					= -1;
	private int									mWB							= -1;
	private int									mISO						= -1;
	private int									mMeteringMode				= -1;

	private float								fScreenDensity;

	private int									iInfoViewMaxHeight;
	private int									iInfoViewMaxWidth;
	private int									iInfoViewHeight;

	private int									iCenterViewMaxHeight;
	private int									iCenterViewMaxWidth;

	// indicates if it's first launch - to show hint layer.
	private boolean								isFirstLaunch				= true;

	private static int							iScreenType					= MainScreen.getAppResources().getInteger(
																					R.integer.screen_type);

	public AlmalenceGUI()
	{

		mThumbnail = null;
		topMenuButtons = new HashMap<Integer, View>();
		topMenuPluginButtons = new HashMap<String, View>();

		scenemodeAdapter = new ElementAdapter();
		wbmodeAdapter = new ElementAdapter();
		focusmodeAdapter = new ElementAdapter();
		flashmodeAdapter = new ElementAdapter();
		isoAdapter = new ElementAdapter();
		meteringmodeAdapter = new ElementAdapter();

		sceneModeButtons = new HashMap<Integer, View>();
		wbModeButtons = new HashMap<Integer, View>();
		focusModeButtons = new HashMap<Integer, View>();
		flashModeButtons = new HashMap<Integer, View>();
		isoButtons = new HashMap<Integer, View>();
		meteringModeButtons = new HashMap<Integer, View>();

		activeScene = new ArrayList<View>();
		activeWB = new ArrayList<View>();
		activeFocus = new ArrayList<View>();
		activeFlash = new ArrayList<View>();
		activeISO = new ArrayList<View>();
		activeMetering = new ArrayList<View>();

		activeSceneNames = new ArrayList<Integer>();
		activeWBNames = new ArrayList<Integer>();
		activeFocusNames = new ArrayList<Integer>();
		activeFlashNames = new ArrayList<Integer>();
		activeISONames = new ArrayList<Integer>();
		activeMeteringNames = new ArrayList<Integer>();

		settingsAdapter = new ElementAdapter();
		settingsViews = new ArrayList<View>();

		quickControlAdapter = new ElementAdapter();
		quickControlChangeres = new ArrayList<View>();

		modeAdapter = new ElementAdapter();
		modeViews = new ArrayList<View>();
		buttonModeViewAssoc = new HashMap<View, String>();

	}

	/*
	 * CAMERA PARAMETERS SECTION Supplementary methods for those plugins that
	 * need an icons of supported camera parameters (scene, iso, wb, flash,
	 * focus) Methods return id of drawable icon
	 */
	public int getSceneIcon(int sceneMode)
	{
		if (ICONS_SCENE.containsKey(sceneMode))
			return ICONS_SCENE.get(sceneMode);
		else
			return -1;
	}

	public int getWBIcon(int wb)
	{
		if (ICONS_WB.containsKey(wb))
			return ICONS_WB.get(wb);
		else
			return -1;
	}

	public int getFocusIcon(int focusMode)
	{
		if (ICONS_FOCUS.containsKey(focusMode))
		{
			try
			{
				return ICONS_FOCUS.get(focusMode);
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e("getFocusIcon", "icons_focus.get exception: " + e.getMessage());
				return -1;
			}
		} else
			return -1;
	}

	public int getFlashIcon(int flashMode)
	{
		if (ICONS_FLASH.containsKey(flashMode))
			return ICONS_FLASH.get(flashMode);
		else
			return -1;
	}

	public int getISOIcon(int isoMode)
	{
		if (ICONS_ISO.containsKey(isoMode))
			return ICONS_ISO.get(isoMode);
		else if (ICONS_DEFAULT_ISO.containsKey(isoMode))
			return ICONS_DEFAULT_ISO.get(isoMode);
		else
			return -1;
	}

	public int getMeteringIcon(String meteringMode)
	{
		if (ICONS_METERING.containsKey(meteringMode))
			return ICONS_METERING.get(meteringMode);
		else
			return -1;
	}

	@Override
	public float getScreenDensity()
	{
		return fScreenDensity;
	}

	@Override
	public void onStart()
	{

		// Calculate right sizes for plugin's controls
		DisplayMetrics metrics = new DisplayMetrics();
		MainScreen.getInstance().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		fScreenDensity = metrics.density;

		iInfoViewMaxHeight = (int) (MainScreen.getMainContext().getResources().getInteger(R.integer.infoControlHeight) * fScreenDensity);
		iInfoViewMaxWidth = (int) (MainScreen.getMainContext().getResources().getInteger(R.integer.infoControlWidth) * fScreenDensity);

		iCenterViewMaxHeight = (int) (MainScreen.getMainContext().getResources().getInteger(R.integer.centerViewHeight) * fScreenDensity);
		iCenterViewMaxWidth = (int) (MainScreen.getMainContext().getResources().getInteger(R.integer.centerViewWidth) * fScreenDensity);

		// Create orientation listener
		initOrientationListener();

		// <!-- -+-
		RotateImageView unlock = ((RotateImageView) guiView.findViewById(R.id.Unlock));
		unlock.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (guiView.findViewById(R.id.postprocessingLayout).getVisibility() == View.VISIBLE)
					return;

				if (MainScreen.getInstance().titleUnlockAll == null
						|| MainScreen.getInstance().titleUnlockAll.endsWith("check for sale"))
				{
					Toast.makeText(MainScreen.getMainContext(),
							"Error connecting to Google Play. Check internet connection.", Toast.LENGTH_LONG).show();
					return;
				}
				// start store
				showStore();
			}
		});
		// -+- -->

		// added immersive full-screen mode support
		// guiView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
	}

	private void initOrientationListener()
	{
		// set orientation listener to rotate controls
		this.orientListener = new OrientationEventListener(MainScreen.getMainContext())
		{
			@Override
			public void onOrientationChanged(int orientation)
			{
				if (orientation == ORIENTATION_UNKNOWN)
					return;

				final Display display = ((WindowManager) MainScreen.getInstance().getSystemService(
						Context.WINDOW_SERVICE)).getDefaultDisplay();
				final int orientationProc = (display.getWidth() <= display.getHeight()) ? Configuration.ORIENTATION_PORTRAIT
						: Configuration.ORIENTATION_LANDSCAPE;
				final int rotation = display.getRotation();

				boolean remapOrientation = Util.shouldRemapOrientation(orientationProc, rotation);

				if (remapOrientation)
					orientation = (orientation - 90 + 360) % 360;

				AlmalenceGUI.mDeviceOrientation = Util.roundOrientation(orientation, AlmalenceGUI.mDeviceOrientation);

				((RotateImageView) topMenuButtons.get(MODE_EV)).setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) topMenuButtons.get(MODE_SCENE)).setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) topMenuButtons.get(MODE_WB)).setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) topMenuButtons.get(MODE_FOCUS)).setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) topMenuButtons.get(MODE_FLASH)).setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) topMenuButtons.get(MODE_ISO)).setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) topMenuButtons.get(MODE_CAM)).setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) topMenuButtons.get(MODE_MET)).setOrientation(AlmalenceGUI.mDeviceOrientation);

				Set<String> keys = topMenuPluginButtons.keySet();
				Iterator<String> it = keys.iterator();
				while (it.hasNext())
				{
					String key = it.next();
					((RotateImageView) topMenuPluginButtons.get(key)).setOrientation(AlmalenceGUI.mDeviceOrientation);
				}

				((RotateImageView) guiView.findViewById(R.id.buttonGallery))
						.setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) guiView.findViewById(R.id.buttonShutter))
						.setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) guiView.findViewById(R.id.buttonSelectMode))
						.setOrientation(AlmalenceGUI.mDeviceOrientation);

				final int degree = AlmalenceGUI.mDeviceOrientation >= 0 ? AlmalenceGUI.mDeviceOrientation % 360
						: AlmalenceGUI.mDeviceOrientation % 360 + 360;
				if (AlmalenceGUI.mPreviousDeviceOrientation != AlmalenceGUI.mDeviceOrientation)
					rotateSquareViews(degree, 250);

				((TextView) guiView.findViewById(R.id.blockingText)).setRotation(-AlmalenceGUI.mDeviceOrientation);

				// <!-- -+-
				store.setOrientation();
				// -+- -->

				AlmalenceGUI.mPreviousDeviceOrientation = AlmalenceGUI.mDeviceOrientation;

				PluginManager.getInstance().onOrientationChanged(getDisplayOrientation());

				if (selfTimer != null)
					selfTimer.setOrientation();
			}
		};
	}

	private void createMergedSelectModeButton()
	{
		// create merged image for select mode button
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		String defaultModeName = prefs.getString(MainScreen.sDefaultModeName, "");
		Mode mode = ConfigParser.getInstance().getMode(defaultModeName);
		try
		{
			Bitmap bm = null;
			Bitmap iconBase = BitmapFactory.decodeResource(MainScreen.getMainContext().getResources(),
					R.drawable.gui_almalence_select_mode);
			Bitmap iconOverlay = BitmapFactory.decodeResource(
					MainScreen.getMainContext().getResources(),
					MainScreen
							.getInstance()
							.getResources()
							.getIdentifier(CameraController.isUseSuperMode() ? mode.iconHAL : mode.icon, "drawable",
									MainScreen.getInstance().getPackageName()));
			iconOverlay = Bitmap.createScaledBitmap(iconOverlay, (int) (iconBase.getWidth() / 1.8),
					(int) (iconBase.getWidth() / 1.8), false);

			bm = mergeImage(iconBase, iconOverlay);
			bm = Bitmap.createScaledBitmap(bm,
					(int) (MainScreen.getMainContext().getResources().getDimension(R.dimen.paramsLayoutHeight)),
					(int) (MainScreen.getMainContext().getResources().getDimension(R.dimen.paramsLayoutHeight)), false);
			((RotateImageView) guiView.findViewById(R.id.buttonSelectMode)).setImageBitmap(bm);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
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
		if (quickControlsChangeVisible)
			closeQuickControlsSettings();
		orientListener.disable();
		if (modeSelectorVisible)
			hideModeList();
		if (settingsControlsVisible)
			((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false, true);

		// <!-- -+-
		if (((RelativeLayout) guiView.findViewById(R.id.viewPagerLayoutMain)).getVisibility() == View.VISIBLE)
			hideStore();
		// -+- -->

		lockControls = false;
		guiView.findViewById(R.id.buttonGallery).setEnabled(true);
		guiView.findViewById(R.id.buttonShutter).setEnabled(true);
		guiView.findViewById(R.id.buttonSelectMode).setEnabled(true);
		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);
	}

	@Override
	public void showStore()
	{
		// <!-- -+-
		store.showStore();
		// -+- -->
	}

	@Override
	public void hideStore()
	{
		// <!-- -+-
		store.hideStore();
		// -+- -->
	}

	@Override
	public void onResume()
	{
		MainScreen.getInstance().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				AlmalenceGUI.this.updateThumbnailButton();
			}
		});
		
		
		setShutterIcon(ShutterButton.DEFAULT);

		lockControls = false;

		orientListener.enable();

		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_EV, false, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_SCENE, false, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_WB, false, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_FOCUS, false, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_FLASH, false, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_ISO, false, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_METERING, false, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_CAMERACHANGE, false, true);

		// if first launch - show layout with hints
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		
		if (prefs.getBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false)) {
			setShutterIcon(ShutterButton.TIMELAPSE_ACTIVE);
		}
		
		if (prefs.contains("isFirstLaunch"))
		{
			isFirstLaunch = prefs.getBoolean("isFirstLaunch", true);
		} else
		{
			Editor prefsEditor = prefs.edit();
			prefsEditor.putBoolean("isFirstLaunch", false);
			prefsEditor.commit();
			isFirstLaunch = true;
		}

		// show/hide hints
		if (!isFirstLaunch)
			guiView.findViewById(R.id.hintLayout).setVisibility(View.GONE);
		else
			guiView.findViewById(R.id.hintLayout).setVisibility(View.VISIBLE);

		// <!-- -+-
		manageUnlockControl();
		// -+- -->

		// Create select mode button with appropriate icon
		createMergedSelectModeButton();
	}

	@Override
	public void onDestroy()
	{
		// Not used
	}

	@Override
	public void createInitialGUI()
	{
		guiView = LayoutInflater.from(MainScreen.getMainContext()).inflate(R.layout.gui_almalence_layout, null);
		// Add GUI Layout to main layout of OpenCamera
		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.mainLayout1)).addView(guiView);
	}

	// Create standard OpenCamera's buttons and theirs OnClickListener
	@Override
	public void onCreate()
	{
		// Get application preferences object
		preferences = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

		guiView.findViewById(R.id.evButton).setOnTouchListener(MainScreen.getInstance());
		guiView.findViewById(R.id.sceneButton).setOnTouchListener(MainScreen.getInstance());
		guiView.findViewById(R.id.wbButton).setOnTouchListener(MainScreen.getInstance());
		guiView.findViewById(R.id.focusButton).setOnTouchListener(MainScreen.getInstance());
		guiView.findViewById(R.id.flashButton).setOnTouchListener(MainScreen.getInstance());
		guiView.findViewById(R.id.isoButton).setOnTouchListener(MainScreen.getInstance());
		guiView.findViewById(R.id.meteringButton).setOnTouchListener(MainScreen.getInstance());
		guiView.findViewById(R.id.camerachangeButton).setOnTouchListener(MainScreen.getInstance());

		// Long clicks are needed to open quick controls customization layout
		guiView.findViewById(R.id.evButton).setOnLongClickListener(this);
		guiView.findViewById(R.id.sceneButton).setOnLongClickListener(this);
		guiView.findViewById(R.id.wbButton).setOnLongClickListener(this);
		guiView.findViewById(R.id.focusButton).setOnLongClickListener(this);
		guiView.findViewById(R.id.flashButton).setOnLongClickListener(this);
		guiView.findViewById(R.id.isoButton).setOnLongClickListener(this);
		guiView.findViewById(R.id.meteringButton).setOnLongClickListener(this);
		guiView.findViewById(R.id.camerachangeButton).setOnLongClickListener(this);

		// Get all top menu buttons
		topMenuButtons.put(MODE_EV, guiView.findViewById(R.id.evButton));
		topMenuButtons.put(MODE_SCENE, guiView.findViewById(R.id.sceneButton));
		topMenuButtons.put(MODE_WB, guiView.findViewById(R.id.wbButton));
		topMenuButtons.put(MODE_FOCUS, guiView.findViewById(R.id.focusButton));
		topMenuButtons.put(MODE_FLASH, guiView.findViewById(R.id.flashButton));
		topMenuButtons.put(MODE_ISO, guiView.findViewById(R.id.isoButton));
		topMenuButtons.put(MODE_MET, guiView.findViewById(R.id.meteringButton));
		topMenuButtons.put(MODE_CAM, guiView.findViewById(R.id.camerachangeButton));

		sceneModeButtons = initCameraParameterModeButtons(ICONS_SCENE, NAMES_SCENE, sceneModeButtons, MODE_SCENE);
		wbModeButtons = initCameraParameterModeButtons(ICONS_WB, NAMES_WB, wbModeButtons, MODE_WB);
		focusModeButtons = initCameraParameterModeButtons(ICONS_FOCUS, NAMES_FOCUS, focusModeButtons, MODE_FOCUS);
		flashModeButtons = initCameraParameterModeButtons(ICONS_FLASH, NAMES_FLASH, flashModeButtons, MODE_FLASH);
		isoButtons = initCameraParameterModeButtons(ICONS_ISO, NAMES_ISO, isoButtons, MODE_ISO);
		meteringModeButtons = initCameraParameterModeButtons(ICONS_METERING, NAMES_METERING, meteringModeButtons,
				MODE_MET);

		// Create top menu buttons for plugins (each plugin may have only one
		// top menu button)
		createPluginTopMenuButtons();

		thumbnailView = (RotateImageView) guiView.findViewById(R.id.buttonGallery);

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.mainLayout1)).setOnTouchListener(MainScreen
				.getInstance());
		((LinearLayout) MainScreen.getInstance().findViewById(R.id.evLayout)).setOnTouchListener(MainScreen
				.getInstance());

		shutterButton = ((RotateImageView) guiView.findViewById(R.id.buttonShutter));
		shutterButton.setOnLongClickListener(this);

		// <!-- -+-
		store = new AlmalenceStore(guiView);
		manageUnlockControl();
		// -+- -->
	}

	// <!-- -+-
	private void manageUnlockControl()
	{
		// manage unlock control
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		if (prefs.getBoolean("unlock_all_forever", false))
			store.HideUnlockControl();
		else
		{
			String modeID = PluginManager.getInstance().getActiveMode().modeID;
			visibilityUnlockControl(UNLOCK_MODE_PREFERENCES.get(modeID));
		}
	}

	private void visibilityUnlockControl(String prefName)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

		if (prefs.getBoolean(prefName, false))
			store.HideUnlockControl();
		else
			store.ShowUnlockControl();
	}

	// -+- -->

	private Map<Integer, View> initCameraParameterModeButtons(Map<Integer, Integer> icons_map,
			Map<Integer, String> names_map, Map<Integer, View> paramMap, final int mode)
	{
		paramMap.clear();
		Set<Integer> keys = icons_map.keySet();
		Iterator<Integer> it = keys.iterator();
		LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
		while (it.hasNext())
		{
			final int system_name = it.next();
			final String value_name = names_map.get(system_name);
			View paramMode = inflator.inflate(R.layout.gui_almalence_quick_control_grid_element, null, false);
			// set some mode icon
			((ImageView) paramMode.findViewById(R.id.imageView)).setImageResource(icons_map.get(system_name));
			((TextView) paramMode.findViewById(R.id.textView)).setText(value_name);

			if ((mode == MODE_FOCUS && system_name == CameraParameters.AF_MODE_AUTO)
					|| (mode == MODE_FLASH && system_name == CameraParameters.FLASH_MODE_OFF)
					|| (mode == MODE_ISO && system_name == CameraParameters.ISO_AUTO)
					|| (mode == MODE_SCENE && system_name == CameraParameters.SCENE_MODE_AUTO)
					|| (mode == MODE_MET && system_name == CameraParameters.meteringModeAuto)
					|| (mode == MODE_WB && system_name == CameraParameters.WB_MODE_AUTO))
				paramMode.setOnTouchListener(new OnTouchListener()
				{

					@Override
					public boolean onTouch(View v, MotionEvent event)
					{
						if (event.getAction() == MotionEvent.ACTION_CANCEL)
						{
							settingsModeClicked(mode, system_name);
							return false;
						}
						return false;
					}
				});

			paramMode.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					settingsModeClicked(mode, system_name);
					guiView.findViewById(R.id.topPanel).setVisibility(View.VISIBLE);
					quickControlsVisible = false;
				}
			});

			paramMap.put(system_name, paramMode);
		}

		return paramMap;
	}

	private void settingsModeClicked(int mode, int system_name)
	{
		switch (mode)
		{
		case MODE_SCENE:
			setSceneMode(system_name);
			break;
		case MODE_WB:
			setWhiteBalance(system_name);
			break;
		case MODE_FOCUS:
			setFocusMode(system_name);
			break;
		case MODE_FLASH:
			setFlashMode(system_name);
			break;
		case MODE_ISO:
			setISO(system_name);
			break;
		case MODE_MET:
			setMeteringMode(system_name);
			break;
		default:
			break;
		}
	}

	@Override
	public void onGUICreate()
	{
//		Log.e("GUI", "onGUICreate");
		if (MainScreen.getInstance().findViewById(R.id.infoLayout).getVisibility() == View.VISIBLE)
			iInfoViewHeight = MainScreen.getInstance().findViewById(R.id.infoLayout).getHeight();
		// Recreate plugin views
		removePluginViews();
		createPluginViews();

		// add self-timer control
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		boolean showDelayedCapturePrefCommon = prefs.getBoolean(MainScreen.sShowDelayedCapturePref, false);
		selfTimer = new SelfTimerAndPhotoTimeLapse();
		selfTimer.addSelfTimerControl(showDelayedCapturePrefCommon);

		LinearLayout infoLayout = (LinearLayout) guiView.findViewById(R.id.infoLayout);
		RelativeLayout.LayoutParams infoParams = (RelativeLayout.LayoutParams) infoLayout.getLayoutParams();
		if (infoParams != null)
		{
			int width = infoParams.width;
			if (infoLayout.getChildCount() == 0)
				infoParams.rightMargin = -width;
			else
				infoParams.rightMargin = 0;
			infoLayout.setLayoutParams(infoParams);
			// infoLayout.requestLayout();
		}

		infoSet = prefs.getInt(MainScreen.sDefaultInfoSetPref, INFO_PARAMS);
		if (infoSet == INFO_PARAMS && !isAnyViewOnViewfinder())
		{
			infoSet = INFO_ALL;
			prefs.edit().putInt(MainScreen.sDefaultInfoSetPref, infoSet).commit();
		}
		setInfo(false, 0, 0, false);

		MainScreen.getInstance().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				AlmalenceGUI.this.updateThumbnailButton();
			}
		});

		final View blockingLayout = guiView.findViewById(R.id.blockingLayout);
		final View postProcessingLayout = guiView.findViewById(R.id.postprocessingLayout);
		final View topPanel = guiView.findViewById(R.id.topPanel);
		final View mainButtons = guiView.findViewById(R.id.mainButtons);
		final View qcLayout = guiView.findViewById(R.id.qcLayout);
		final View buttonsLayout = guiView.findViewById(R.id.buttonsLayout);
		final View hintLayout = guiView.findViewById(R.id.hintLayout);

		mainButtons.bringToFront();
		qcLayout.bringToFront();
		buttonsLayout.bringToFront();
		topPanel.bringToFront();
		blockingLayout.bringToFront();
		postProcessingLayout.bringToFront();
		hintLayout.bringToFront();

		View help = guiView.findViewById(R.id.mode_help);
		help.bringToFront();

		// <!-- -+-
		if (MainScreen.getInstance().isShowStore())
		{
			showStore();
			MainScreen.getInstance().setShowStore(false);
		}
		// -+- -->
	}

	@Override
	public void setupViewfinderPreviewSize(CameraController.Size previewSize)
	{
		Log.e("GUI",
				"setupViewfinderPreviewSize. Width = " + previewSize.getWidth() + " Height = "
						+ previewSize.getHeight());
		float cameraAspect = (float) previewSize.getWidth() / previewSize.getHeight();

		RelativeLayout ll = (RelativeLayout) MainScreen.getInstance().findViewById(R.id.mainLayout1);

		int previewSurfaceWidth = ll.getWidth();
		int previewSurfaceHeight = ll.getHeight();
		float surfaceAspect = (float) previewSurfaceHeight / previewSurfaceWidth;

		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);

		DisplayMetrics metrics = new DisplayMetrics();
		MainScreen.getInstance().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int screen_height = metrics.heightPixels;

		lp.width = previewSurfaceWidth;
		lp.height = previewSurfaceHeight;
		if (Math.abs(surfaceAspect - cameraAspect) > 0.05d)
		{
			if (surfaceAspect > cameraAspect && (Math.abs(1 - cameraAspect) > 0.05d))
			{
				int paramsLayoutHeight = (int) MainScreen.getAppResources().getDimension(R.dimen.paramsLayoutHeight);
				// if wide-screen - decrease width of surface
				lp.width = previewSurfaceWidth;

				lp.height = (int) (screen_height - 2 * paramsLayoutHeight);
				lp.topMargin = (int) (paramsLayoutHeight);
			} else if (surfaceAspect > cameraAspect)
			{
				int paramsLayoutHeight = (int) MainScreen.getAppResources().getDimension(R.dimen.paramsLayoutHeight);
				// if wide-screen - decrease width of surface
				lp.width = previewSurfaceWidth;

				lp.height = previewSurfaceWidth;
				lp.topMargin = (int) (paramsLayoutHeight);
			}
		}
		
		Log.e("GUI", "setLayoutParams. width = " + lp.width + " height = " + lp.height);
		MainScreen.getPreviewSurfaceView().setLayoutParams(lp);
		guiView.findViewById(R.id.fullscreenLayout).setLayoutParams(lp);
		guiView.findViewById(R.id.specialPluginsLayout).setLayoutParams(lp);
	}

	/*
	 * Each plugin may have only one top menu button Icon id and Title (plugin's
	 * members) is use to make design of button
	 */
	public void createPluginTopMenuButtons()
	{
		topMenuPluginButtons.clear();

		createPluginTopMenuButtons(PluginManager.getInstance().getActivePlugins(PluginType.ViewFinder));
		createPluginTopMenuButtons(PluginManager.getInstance().getActivePlugins(PluginType.Capture));
		createPluginTopMenuButtons(PluginManager.getInstance().getActivePlugins(PluginType.Processing));
		createPluginTopMenuButtons(PluginManager.getInstance().getActivePlugins(PluginType.Filter));
		createPluginTopMenuButtons(PluginManager.getInstance().getActivePlugins(PluginType.Export));
	}

	public void createPluginTopMenuButtons(List<Plugin> plugins)
	{
		if (!plugins.isEmpty())
		{
			for (int i = 0; i < plugins.size(); i++)
			{
				Plugin plugin = plugins.get(i);

				if (plugin == null || plugin.getQuickControlIconID() <= 0)
					continue;

				LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
				ImageView qcView = (ImageView) inflator.inflate(R.layout.gui_almalence_quick_control_button,
						(ViewGroup) guiView.findViewById(R.id.paramsLayout), false);

				qcView.setOnTouchListener(MainScreen.getInstance());
				qcView.setOnClickListener(this);
				qcView.setOnLongClickListener(this);

				topMenuPluginButtons.put(plugin.getID(), qcView);
			}
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
		int iInfoControlsRemainingHeight = iInfoViewHeight;

		List<View> info_views = null;
		Map<View, Plugin.ViewfinderZone> plugin_views = null;

		List<Plugin> plugins = PluginManager.getInstance().getActivePlugins(type);
		if (!plugins.isEmpty())
		{
			for (int i = 0; i < plugins.size(); i++)
			{
				Plugin plugin = plugins.get(i);
				if (plugin != null)
				{
					plugin_views = plugin.getPluginViews();
					addPluginViews(plugin_views);

					// Add info controls
					info_views = plugin.getInfoViews();
					for (int j = 0; j < info_views.size(); j++)
					{
						View infoView = info_views.get(j);

						// Calculate appropriate size of added plugin's view
						android.widget.LinearLayout.LayoutParams viewLayoutParams = (android.widget.LinearLayout.LayoutParams) infoView
								.getLayoutParams();
						viewLayoutParams = this.getTunedLinearLayoutParams(infoView, viewLayoutParams,
								iInfoViewMaxWidth, iInfoViewMaxHeight);

						if (iInfoControlsRemainingHeight >= viewLayoutParams.height)
						{
							iInfoControlsRemainingHeight -= viewLayoutParams.height;
							this.addInfoView(infoView, viewLayoutParams);
						}
					}
				}
			}
		}
	}

	//
	private void initDefaultQuickControls()
	{
		quickControl1 = null;
		quickControl2 = null;
		quickControl3 = null;
		quickControl4 = null;
		initDefaultQuickControls(quickControl1);
		initDefaultQuickControls(quickControl2);
		initDefaultQuickControls(quickControl3);
		initDefaultQuickControls(quickControl4);
	}

	private void initDefaultQuickControls(View quickControl)
	{
		LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
		quickControl = inflator.inflate(R.layout.gui_almalence_invisible_button,
				(ViewGroup) guiView.findViewById(R.id.paramsLayout), false);
		quickControl.setOnLongClickListener(this);
		quickControl.setOnClickListener(this);
	}

	// Called when camera object created in MainScreen.
	// After camera creation it is possibly to obtain
	// all camera possibilities such as supported scene mode, flash mode and
	// etc.
	@Override
	public void onCameraCreate()
	{
		String defaultQuickControl1 = "";
		String defaultQuickControl2 = "";
		String defaultQuickControl3 = "";
		String defaultQuickControl4 = "";

		// Remove buttons from previous camera start
		removeAllViews(topMenuButtons);
		removeAllViews(topMenuPluginButtons);

		mEVSupported = false;
		mSceneModeSupported = false;
		mWBSupported = false;
		mFocusModeSupported = false;
		mFlashModeSupported = false;
		mISOSupported = false;
		mMeteringAreasSupported = false;
		mCameraChangeSupported = false;

		mEVLockSupported = false;
		mWBLockSupported = false;

		activeScene.clear();
		activeWB.clear();
		activeFocus.clear();
		activeFlash.clear();
		activeISO.clear();
		activeMetering.clear();

		activeSceneNames.clear();
		activeWBNames.clear();
		activeFocusNames.clear();
		activeFlashNames.clear();
		activeISONames.clear();
		activeMeteringNames.clear();

		removeAllQuickViews();
		initDefaultQuickControls();

		createPluginTopMenuButtons();

		if (CameraController.isExposureLockSupported())
			mEVLockSupported = true;
		if (CameraController.isWhiteBalanceLockSupported())
			mWBLockSupported = true;

		// Create Exposure compensation button and slider with supported values
		if (CameraController.isExposureCompensationSupported())
		{
			mEVSupported = true;
			defaultQuickControl1 = String.valueOf(MODE_EV);

			float ev_step = CameraController.getExposureCompensationStep();

			int minValue = CameraController.getMinExposureCompensation();
			int maxValue = CameraController.getMaxExposureCompensation();

			SeekBar evBar = (SeekBar) guiView.findViewById(R.id.evSeekBar);
			if (evBar != null)
			{
				int initValue = preferences.getInt(MainScreen.sEvPref, 0);
				evBar.setMax(maxValue - minValue);
				evBar.setProgress(initValue + maxValue);

				TextView leftText = (TextView) guiView.findViewById(R.id.seekBarLeftText);
				TextView rightText = (TextView) guiView.findViewById(R.id.seekBarRightText);

				int minValueReal = Math.round(minValue * ev_step);
				int maxValueReal = Math.round(maxValue * ev_step);
				String minString = String.valueOf(minValueReal);
				String maxString = String.valueOf(maxValueReal);

				if (minValueReal > 0)
					minString = "+" + minString;
				if (maxValueReal > 0)
					maxString = "+" + maxString;

				leftText.setText(minString);
				rightText.setText(maxString);

				mEV = initValue;
				CameraController.setCameraExposureCompensation(mEV);

				evBar.setOnSeekBarChangeListener(this);
			}

			RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_EV);
			but.setImageResource(ICON_EV);
		} else
			mEVSupported = false;

		// Create Scene mode button and adding supported scene modes
		int[] supported_scene = CameraController.getSupportedSceneModes();
		if (supported_scene != null && supported_scene.length > 0 && activeScene != null)
		{
			for (int scene_name : supported_scene)
			{
				if (sceneModeButtons.containsKey(scene_name))
				{
					if (scene_name != CameraParameters.SCENE_MODE_NIGHT)
						activeScene.add(sceneModeButtons.get(Integer.valueOf(scene_name)));
					activeSceneNames.add(Integer.valueOf(scene_name));
				}
			}

			if (!activeSceneNames.isEmpty())
			{
				mSceneModeSupported = true;
				scenemodeAdapter.Elements = activeScene;
				GridView gridview = (GridView) guiView.findViewById(R.id.scenemodeGrid);
				gridview.setAdapter(scenemodeAdapter);

				int initValue = preferences.getInt(MainScreen.sSceneModePref, MainScreen.sDefaultValue);
				if (!activeSceneNames.contains(initValue))
				{
					if (CameraController.isFrontCamera())
						initValue = activeSceneNames.get(0);
					else
						initValue = CameraParameters.SCENE_MODE_AUTO;
				}

				setButtonSelected(sceneModeButtons, initValue);
				setCameraParameterValue(MODE_SCENE, initValue);

				if (ICONS_SCENE != null && ICONS_SCENE.containsKey(initValue))
				{
					RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_SCENE);
					int icon_id = ICONS_SCENE.get(initValue);
					but.setImageResource(icon_id);
				}

				CameraController.setCameraSceneMode(mSceneMode);
			} else
			{
				mSceneModeSupported = false;
				mSceneMode = -1;
			}
		} else
		{
			mSceneModeSupported = false;
			mSceneMode = -1;
		}

		// Create White Balance mode button and adding supported white balances
		int[] supported_wb = CameraController.getSupportedWhiteBalance();
		if (supported_wb != null && supported_wb.length > 0 && activeWB != null)
		{
			for (int wb_name : supported_wb)
			{
				if (wbModeButtons.containsKey(wb_name))
				{
					activeWB.add(wbModeButtons.get(Integer.valueOf(wb_name)));
					activeWBNames.add(Integer.valueOf(wb_name));
				}
			}

			if (!activeWBNames.isEmpty())
			{
				mWBSupported = true;

				wbmodeAdapter.Elements = activeWB;
				GridView gridview = (GridView) guiView.findViewById(R.id.wbGrid);
				gridview.setAdapter(wbmodeAdapter);

				int initValue = preferences.getInt(MainScreen.sWBModePref, MainScreen.sDefaultValue);
				if (!activeWBNames.contains(initValue))
				{
					if (CameraController.isFrontCamera())
						initValue = activeWBNames.get(0);
					else
						initValue = MainScreen.sDefaultValue;
				}
				setButtonSelected(wbModeButtons, initValue);
				setCameraParameterValue(MODE_WB, initValue);

				if (ICONS_WB != null && ICONS_WB.containsKey(initValue))
				{
					RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_WB);
					int icon_id = ICONS_WB.get(initValue);
					but.setImageResource(icon_id);
				}

				CameraController.setCameraWhiteBalance(mWB);
			} else
			{
				mWBSupported = false;
				mWB = -1;
			}
		} else
		{
			mWBSupported = false;
			mWB = -1;
		}

		// Create Focus mode button and adding supported focus modes
		final int[] supported_focus = CameraController.getSupportedFocusModes();
		if (supported_focus != null && supported_focus.length > 0 && activeFocus != null)
		{
			for (int focus_name : supported_focus)
			{
				if (focusModeButtons.containsKey(focus_name))
				{
					activeFocus.add(focusModeButtons.get(Integer.valueOf(focus_name)));
					activeFocusNames.add(Integer.valueOf(focus_name));
				}
			}

			if (!activeFocusNames.isEmpty())
			{
				mFocusModeSupported = true;
				defaultQuickControl3 = String.valueOf(MODE_FOCUS);

				if (CameraController.isModeAvailable(supported_focus, CameraParameters.AF_MODE_AUTO)
						|| CameraController.isModeAvailable(supported_focus, CameraParameters.AF_MODE_MACRO))
				{
					LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
					View paramMode = inflator.inflate(R.layout.gui_almalence_quick_control_grid_element, null, false);

					String aflock_name = MainScreen.getAppResources().getString(R.string.focusAFLock);
					((ImageView) paramMode.findViewById(R.id.imageView))
							.setImageResource(R.drawable.gui_almalence_settings_focus_aflock);
					((TextView) paramMode.findViewById(R.id.textView)).setText(aflock_name);

					paramMode.setOnClickListener(new OnClickListener()
					{

						@Override
						public void onClick(View v)
						{
							try
							{
								RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_FOCUS);
								but.setImageResource(R.drawable.gui_almalence_settings_focus_aflock);
							} catch (Exception e)
							{
								e.printStackTrace();
								Log.d("set AF-L failed", "icons_focus.get exception: " + e.getMessage());
							}

							mFocusMode = FOCUS_AF_LOCK;

							int afMode = -1;
							if (CameraController.isModeAvailable(supported_focus, CameraParameters.AF_MODE_AUTO))
								afMode = CameraParameters.AF_MODE_AUTO;
							else if (CameraController.isModeAvailable(supported_focus, CameraParameters.AF_MODE_MACRO))
								afMode = CameraParameters.AF_MODE_MACRO;
							else
								afMode = supported_focus[0];

							CameraController.setCameraFocusMode(afMode);
							MainScreen.setAutoFocusLock(true);

							preferences
									.edit()
									.putInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref
											: MainScreen.sFrontFocusModePref, afMode).commit();

							PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST,
									PluginManager.MSG_FOCUS_CHANGED);

							initSettingsMenu(true);
							hideSecondaryMenus();
							unselectPrimaryTopMenuButtons(-1);

							guiView.findViewById(R.id.topPanel).setVisibility(View.VISIBLE);
							quickControlsVisible = false;
						}
					});

					focusModeButtons.put(FOCUS_AF_LOCK, paramMode);
					activeFocus.add(focusModeButtons.get(FOCUS_AF_LOCK));
					activeFocusNames.add(FOCUS_AF_LOCK);
				}

				focusmodeAdapter.Elements = activeFocus;
				GridView gridview = (GridView) guiView.findViewById(R.id.focusmodeGrid);
				gridview.setAdapter(focusmodeAdapter);

				int initValue = preferences.getInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref
						: MainScreen.sFrontFocusModePref, MainScreen.sDefaultFocusValue);
				if (!activeFocusNames.contains(initValue))
				{
					if (activeFocusNames.contains(MainScreen.sDefaultValue))
						initValue = MainScreen.sDefaultValue;
					else
						initValue = activeFocusNames.get(0);
				}

				setButtonSelected(focusModeButtons, initValue);
				setCameraParameterValue(MODE_FOCUS, initValue);

				if (ICONS_FOCUS != null && ICONS_FOCUS.containsKey(initValue))
				{
					RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_FOCUS);
					try
					{
						int icon_id = ICONS_FOCUS.get(initValue);
						but.setImageResource(icon_id);
					} catch (Exception e)
					{
						e.printStackTrace();
						Log.e("onCameraCreate", "icons_focus.get exception: " + e.getMessage());
					}
				}

				if (mFocusMode == FOCUS_AF_LOCK)
				{
					int afMode = -1;
					if (CameraController.isModeAvailable(supported_focus, CameraParameters.AF_MODE_AUTO))
						afMode = CameraParameters.AF_MODE_AUTO;
					else if (CameraController.isModeAvailable(supported_focus, CameraParameters.AF_MODE_MACRO))
						afMode = CameraParameters.AF_MODE_MACRO;
					else
						afMode = supported_focus[0];

					CameraController.setCameraFocusMode(afMode);
				} else
					CameraController.setCameraFocusMode(mFocusMode);
			} else
			{
				mFocusModeSupported = false;
				mFocusMode = -1;
			}
		} else
		{
			mFocusMode = -1;
			mFocusModeSupported = false;
		}

		// Create Flash mode button and adding supported flash modes
		int[] supported_flash = CameraController.getSupportedFlashModes();
		if (supported_flash != null
				&& supported_flash.length > 0
				&& activeFlash != null
				&& !(supported_flash.length == 1 && CameraController.isModeAvailable(supported_flash,
						CameraParameters.FLASH_MODE_OFF)))
		{
			for (int flash_name : supported_flash)
			{
				if (flashModeButtons.containsKey(flash_name))
				{
					activeFlash.add(flashModeButtons.get(Integer.valueOf(flash_name)));
					activeFlashNames.add(Integer.valueOf(flash_name));
				}
			}

			if (!activeFlashNames.isEmpty())
			{
				mFlashModeSupported = true;
				defaultQuickControl2 = String.valueOf(MODE_FLASH);

				flashmodeAdapter.Elements = activeFlash;
				GridView gridview = (GridView) guiView.findViewById(R.id.flashmodeGrid);
				gridview.setAdapter(flashmodeAdapter);

				int initValue = preferences.getInt(MainScreen.sFlashModePref, MainScreen.sDefaultFlashValue);
				if (!activeFlashNames.contains(initValue))
				{
					if (CameraController.isFrontCamera())
						initValue = activeFlashNames.get(0);
					else
						initValue = MainScreen.sDefaultFlashValue;
				}
				setButtonSelected(flashModeButtons, initValue);
				setCameraParameterValue(MODE_FLASH, initValue);

				if (ICONS_FLASH != null && ICONS_FLASH.containsKey(initValue))
				{
					RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_FLASH);
					int icon_id = ICONS_FLASH.get(initValue);
					but.setImageResource(icon_id);
				}

				CameraController.setCameraFlashMode(mFlashMode);
			} else
			{
				mFlashModeSupported = false;
				mFlashMode = -1;
			}
		} else
		{
			mFlashModeSupported = false;
			mFlashMode = -1;
		}

		// Create ISO button and adding supported ISOs
		int[] supported_iso = CameraController.getSupportedISO();
		if ((supported_iso != null && supported_iso.length > 0 && activeISO != null)
				|| (CameraController.isISOSupported() && activeISO != null))
		{
			if (supported_iso != null && supported_iso.length != 0)
				for (int iso_name : supported_iso)
				{
					if (isoButtons.containsKey(iso_name))
					{
						activeISO.add(isoButtons.get(Integer.valueOf(iso_name)));
						activeISONames.add(Integer.valueOf(iso_name));
					}
				}
			else
			{
				for (String iso_name : CameraController.getIsoDefaultList())
				{
					activeISO.add(isoButtons.get(CameraController.getIsoKey().get(iso_name)));
					activeISONames.add(CameraController.getIsoKey().get(iso_name));
				}
			}

			if (!activeISONames.isEmpty())
			{
				mISOSupported = true;

				isoAdapter.Elements = activeISO;
				GridView gridview = (GridView) guiView.findViewById(R.id.isoGrid);
				gridview.setAdapter(isoAdapter);

				int initValue = preferences.getInt(MainScreen.sISOPref, MainScreen.sDefaultValue);
				if (!activeISONames.contains(initValue))
				{
					if (CameraController.isFrontCamera())
						initValue = activeISONames.get(0);
					else
						initValue = MainScreen.sDefaultValue;

					preferences.edit().putInt(MainScreen.sISOPref, initValue).commit();
				}
				setButtonSelected(isoButtons, initValue);
				setCameraParameterValue(MODE_ISO, initValue);

				if (ICONS_ISO != null && ICONS_ISO.containsKey(initValue))
				{
					RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_ISO);
					int icon_id = ICONS_ISO.get(initValue);
					but.setImageResource(icon_id);
				}
				CameraController.setCameraISO(mISO);
			} else
			{
				mISOSupported = false;
				mISO = -1;
			}
		} else
		{
			mISOSupported = false;
			mISO = -1;
		}

		int iMeteringAreasSupported = CameraController.getMaxNumMeteringAreas();
		if (iMeteringAreasSupported > 0)
		{
			Collection<Integer> unsorted_keys = NAMES_METERING.keySet();
			List<Integer> keys = Util.asSortedList(unsorted_keys);
			Iterator<Integer> it = keys.iterator();
			while (it.hasNext())
			{
				int metering_name = it.next();
				if (meteringModeButtons.containsKey(metering_name))
				{
					activeMetering.add(meteringModeButtons.get(metering_name));
					activeMeteringNames.add(metering_name);
				}
			}

			if (!activeMeteringNames.isEmpty())
			{
				this.mMeteringAreasSupported = true;

				meteringmodeAdapter.Elements = activeMetering;
				GridView gridview = (GridView) guiView.findViewById(R.id.meteringmodeGrid);
				gridview.setAdapter(meteringmodeAdapter);

				int initValue = preferences.getInt(MainScreen.sMeteringModePref, MainScreen.sDefaultMeteringValue);
				if (!activeMeteringNames.contains(initValue))
					initValue = activeMeteringNames.get(0);

				setButtonSelected(meteringModeButtons, initValue);
				setCameraParameterValue(MODE_MET, initValue);

				if (ICONS_METERING != null && ICONS_METERING.containsKey(initValue))
				{
					RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_MET);
					int icon_id = ICONS_METERING.get(initValue);
					but.setImageResource(icon_id);
				}

				MainScreen.getInstance().setCameraMeteringMode(mMeteringMode);
			} else
			{
				mMeteringAreasSupported = false;
				mMeteringMode = -1;
			}
		} else
		{
			this.mMeteringAreasSupported = false;
			this.mMeteringMode = -1;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
		{
			addCameraChangeButton();
			defaultQuickControl4 = String.valueOf(MODE_CAM);
		} else
			mCameraChangeSupported = false;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		String qc1 = prefs.getString(MainScreen.getAppResources().getString(R.string.Preference_QuickControlButton1),
				defaultQuickControl1);
		String qc2 = prefs.getString(MainScreen.getAppResources().getString(R.string.Preference_QuickControlButton2),
				defaultQuickControl2);
		String qc3 = prefs.getString(MainScreen.getAppResources().getString(R.string.Preference_QuickControlButton3),
				defaultQuickControl3);
		String qc4 = prefs.getString(MainScreen.getAppResources().getString(R.string.Preference_QuickControlButton4),
				defaultQuickControl4);

		quickControl1 = isCameraParameterSupported(qc1) ? getQuickControlButton(qc1, quickControl1)
				: getFreeQuickControlButton(qc1, qc2, qc3, qc4, quickControl1);
		quickControl2 = isCameraParameterSupported(qc2) ? getQuickControlButton(qc2, quickControl2)
				: getFreeQuickControlButton(qc1, qc2, qc3, qc4, quickControl2);
		quickControl3 = isCameraParameterSupported(qc3) ? getQuickControlButton(qc3, quickControl3)
				: getFreeQuickControlButton(qc1, qc2, qc3, qc4, quickControl3);
		quickControl4 = isCameraParameterSupported(qc4) ? getQuickControlButton(qc4, quickControl4)
				: getFreeQuickControlButton(qc1, qc2, qc3, qc4, quickControl4);

		try
		{
			((LinearLayout) guiView.findViewById(R.id.paramsLayout)).addView(quickControl1);
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("AlmalenceGUI", "addView exception: " + e.getMessage());
		}

		try
		{
			((LinearLayout) guiView.findViewById(R.id.paramsLayout)).addView(quickControl2);
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("AlmalenceGUI", "addView exception: " + e.getMessage());
		}

		try
		{
			((LinearLayout) guiView.findViewById(R.id.paramsLayout)).addView(quickControl3);
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("AlmalenceGUI", "addView exception: " + e.getMessage());
		}

		try
		{
			((LinearLayout) guiView.findViewById(R.id.paramsLayout)).addView(quickControl4);
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("AlmalenceGUI", "addView exception: " + e.getMessage());
		}

		if (mEVSupported)
		{
			PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_EV_CHANGED);
		}

	}

	@Override
	public void onPluginsInitialized()
	{
		// Hide all opened menu
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		// create and fill drawing slider
		initSettingsMenu(true);
		initModeList();

		Panel.OnPanelListener pListener = new OnPanelListener()
		{
			public void onPanelOpened(Panel panel)
			{
				settingsControlsVisible = true;

				if (modeSelectorVisible)
					hideModeList();
				if (quickControlsChangeVisible)
					closeQuickControlsSettings();
				if (isSecondaryMenusVisible())
					hideSecondaryMenus();

				PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_LOCKED);
			}

			public void onPanelClosed(Panel panel)
			{
				settingsControlsVisible = false;

				PluginManager.getInstance()
						.sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);
				((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false, false);
			}
		};

		guiView.findViewById(R.id.topPanel).bringToFront();
		guiView.findViewById(R.id.blockingLayout).bringToFront();
		guiView.findViewById(R.id.postprocessingLayout).bringToFront();
		if (modeSelectorVisible)
		{
			guiView.findViewById(R.id.modeLayout).bringToFront();
		}
		((Panel) guiView.findViewById(R.id.topPanel)).setOnPanelListener(pListener);
	}

	private boolean isCameraParameterSupported(String param)
	{
		if (!param.equals("") && topMenuPluginButtons.containsKey(param))
			return true;
		else if (!param.equals("") && com.almalence.util.Util.isNumeric(param))
		{
			int cameraParameter = Integer.valueOf(param);
			switch (cameraParameter)
			{
			case MODE_EV:
				return mEVSupported;
			case MODE_SCENE:
				return mSceneModeSupported;
			case MODE_WB:
				return mWBSupported;
			case MODE_FLASH:
				return mFlashModeSupported;
			case MODE_FOCUS:
				return mFocusModeSupported;
			case MODE_ISO:
				return mISOSupported;
			case MODE_MET:
				return mMeteringAreasSupported;
			case MODE_CAM:
				return mCameraChangeSupported;
			default:
				break;
			}
		}

		return false;
	}

	// bInitMenu - by default should be true. if called several simultaneously -
	// all should be false and last - true
	public void disableCameraParameter(CameraParameter iParam, boolean bDisable, boolean bInitMenu)
	{
		View topMenuView = null;
		switch (iParam)
		{
		case CAMERA_PARAMETER_EV:
			topMenuView = topMenuButtons.get(MODE_EV);
			isEVEnabled = !bDisable;
			break;
		case CAMERA_PARAMETER_SCENE:
			topMenuView = topMenuButtons.get(MODE_SCENE);
			isSceneEnabled = !bDisable;
			break;
		case CAMERA_PARAMETER_WB:
			topMenuView = topMenuButtons.get(MODE_WB);
			isWBEnabled = !bDisable;
			break;
		case CAMERA_PARAMETER_FOCUS:
			topMenuView = topMenuButtons.get(MODE_FOCUS);
			isFocusEnabled = !bDisable;
			break;
		case CAMERA_PARAMETER_FLASH:
			topMenuView = topMenuButtons.get(MODE_FLASH);
			isFlashEnabled = !bDisable;
			break;
		case CAMERA_PARAMETER_ISO:
			topMenuView = topMenuButtons.get(MODE_ISO);
			isIsoEnabled = !bDisable;
			break;
		case CAMERA_PARAMETER_METERING:
			topMenuView = topMenuButtons.get(MODE_MET);
			isMeteringEnabled = !bDisable;
			break;
		case CAMERA_PARAMETER_CAMERACHANGE:
			topMenuView = topMenuButtons.get(MODE_CAM);
			isCameraChangeEnabled = !bDisable;
			break;
		default:
			break;
		}

		if (topMenuView != null)
		{
			correctTopMenuButtonBackground(topMenuView, !bDisable);

			if (bInitMenu)
				initSettingsMenu(true);
		}

	}

	private void correctTopMenuButtonBackground(View topMenuView, boolean isEnabled)
	{
		if (topMenuView != null)
		{
			if (!isEnabled)
			{
				((RotateImageView) topMenuView).setColorFilter(0x50FAFAFA, PorterDuff.Mode.DST_IN);
			} else
			{
				((RotateImageView) topMenuView).clearColorFilter();
			}
		}
	}

	private View getQuickControlButton(String qcID, View defaultView)
	{
		if (!qcID.equals("") && topMenuPluginButtons.containsKey(qcID))
		{
			Plugin plugin = PluginManager.getInstance().getPlugin(qcID);
			RotateImageView view = (RotateImageView) topMenuPluginButtons.get(qcID);
			view.setImageResource(plugin.getQuickControlIconID());
			return view;
		} else if (!qcID.equals("") && topMenuButtons.containsKey(Integer.valueOf(qcID)))
			return topMenuButtons.get(Integer.valueOf(qcID));

		return defaultView;
	}

	// Method for finding a button for a top menu which not yet represented in
	// that top menu
	private View getFreeQuickControlButton(String qc1, String qc2, String qc3, String qc4, View emptyView)
	{
		Set<Integer> topMenuButtonsKeys = topMenuButtons.keySet();
		Iterator<Integer> topMenuButtonsIterator = topMenuButtonsKeys.iterator();

		Set<String> topMenuPluginButtonsKeys = topMenuPluginButtons.keySet();
		Iterator<String> topMenuPluginButtonsIterator = topMenuPluginButtonsKeys.iterator();

		// Searching for free button in the top menu buttons list (scene mode,
		// wb, focus, flash, camera switch)
		while (topMenuButtonsIterator.hasNext())
		{
			int id1, id2, id3, id4;
			try
			{
				id1 = Integer.valueOf(qc1);
			} catch (NumberFormatException exp)
			{
				id1 = -1;
			}
			try
			{
				id2 = Integer.valueOf(qc2);
			} catch (NumberFormatException exp)
			{
				id2 = -1;
			}
			try
			{
				id3 = Integer.valueOf(qc3);
			} catch (NumberFormatException exp)
			{
				id3 = -1;
			}
			try
			{
				id4 = Integer.valueOf(qc4);
			} catch (NumberFormatException exp)
			{
				id4 = -1;
			}

			int buttonID = topMenuButtonsIterator.next();
			View topMenuButton = topMenuButtons.get(buttonID);

			if (checkTopMenuButtonIntegerID(topMenuButton, buttonID, id1, id2, id3, id4)
					&& isCameraParameterSupported(String.valueOf(buttonID)))
				return topMenuButton;
		}

		// If top menu buttons dosn't have a free button, search in plugin's
		// buttons list
		while (topMenuPluginButtonsIterator.hasNext())
		{
			String buttonID = topMenuPluginButtonsIterator.next();
			View topMenuButton = topMenuPluginButtons.get(buttonID);

			if (checkTopMenuButtonStringID(topMenuButton, buttonID, qc1, qc2, qc3, qc4))
				return topMenuButton;
		}

		// If no button is found create a empty button
		return emptyView;
	}

	// Util function used to check if topMenuButton already added to top menu
	private boolean checkTopMenuButtonStringID(final View topMenuButton, final String buttonID, final String qc1,
			final String qc2, final String qc3, final String qc4)
	{
		return topMenuButton != quickControl1 && topMenuButton != quickControl2 && topMenuButton != quickControl3
				&& topMenuButton != quickControl4 && buttonID != qc1 && buttonID != qc2 && buttonID != qc3
				&& buttonID != qc4;
	}

	// Util function used to check if topMenuButton already added to top menu
	private boolean checkTopMenuButtonIntegerID(final View topMenuButton, final int buttonID, final int qc1,
			final int qc2, final int qc3, final int qc4)
	{
		return topMenuButton != quickControl1 && topMenuButton != quickControl2 && topMenuButton != quickControl3
				&& topMenuButton != quickControl4 && buttonID != qc1 && buttonID != qc2 && buttonID != qc3
				&& buttonID != qc4;
	}

	private void addCameraChangeButton()
	{
		if (CameraController.getNumberOfCameras() > 1)
		{
			mCameraChangeSupported = true;

			RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_CAM);
			but.setImageResource(ICON_CAM);
		} else
			mCameraChangeSupported = false;
	}

	public void rotateSquareViews(final int degree, int duration)
	{
		if (AlmalenceGUI.mPreviousDeviceOrientation != AlmalenceGUI.mDeviceOrientation || duration == 0)
		{
			int startDegree = AlmalenceGUI.mPreviousDeviceOrientation == 0 ? 0
					: 360 - AlmalenceGUI.mPreviousDeviceOrientation;
			int endDegree = AlmalenceGUI.mDeviceOrientation == 0 ? 0 : 360 - AlmalenceGUI.mDeviceOrientation;

			int diff = endDegree - startDegree;
			// diff = diff >= 0 ? diff : 360 + diff; // make it in range [0,
			// 359]
			//
			// // Make it in range [-179, 180]. That's the shorted distance
			// between the
			// // two angles
			endDegree = diff > 180 ? endDegree - 360 : diff < -180 && endDegree == 0 ? 360 : endDegree;

			if (modeSelectorVisible)
				rotateViews(modeViews, startDegree, endDegree, duration);

			if (!settingsViews.isEmpty())
			{
				int delay = ((Panel) guiView.findViewById(R.id.topPanel)).isOpen() ? duration : 0;
				rotateViews(settingsViews, startDegree, endDegree, delay);
			}

			if (!quickControlChangeres.isEmpty() && this.quickControlsChangeVisible)
				rotateViews(quickControlChangeres, startDegree, endDegree, duration);

			rotateViews(activeScene, startDegree, endDegree, duration);
			rotateViews(activeWB, startDegree, endDegree, duration);
			rotateViews(activeFocus, startDegree, endDegree, duration);
			rotateViews(activeFlash, startDegree, endDegree, duration);
			rotateViews(activeISO, startDegree, endDegree, duration);
			rotateViews(activeMetering, startDegree, endDegree, duration);
		}
	}

	private void rotateViews(List<View> views, final float startDegree, final float endDegree, long duration)
	{
		for (int i = 0; i < views.size(); i++)
		{
			float start = startDegree;
			float end = endDegree;
			final View view = views.get(i);

			if (view == null)
				continue;

			duration = 0;
			if (duration == 0)
			{
				view.clearAnimation();
				view.setRotation(endDegree);
			} else
			{
				start = startDegree - view.getRotation();
				end = endDegree - view.getRotation();

				RotateAnimation animation = new RotateAnimation(start, end, view.getWidth() / 2, view.getHeight() / 2);

				animation.setDuration(duration);
				animation.setFillAfter(true);
				animation.setInterpolator(new DecelerateInterpolator());
				view.clearAnimation();
				view.startAnimation(animation);
			}
		}
	}

	/***************************************************************************************
	 * 
	 * addQuickSetting method
	 * 
	 * type: EV, SCENE, WB, FOCUS, FLASH, CAMERA or MORE
	 * 
	 * Common method for tune quick control's and quick setting view's icon and
	 * text
	 * 
	 * Quick controls and Quick settings views has a similar design but
	 * different behavior
	 * 
	 ****************************************************************************************/
	private void addQuickSetting(SettingsType type, boolean isQuickControl)
	{
		int icon_id = -1;
		CharSequence icon_text = "";
		boolean isEnabled = true;

		switch (type)
		{
		case SCENE:
			icon_id = ICONS_SCENE.get(mSceneMode);
			icon_text = MainScreen.getAppResources().getString(R.string.settings_mode_scene);
			isEnabled = isSceneEnabled;
			break;
		case WB:
			icon_id = ICONS_WB.get(mWB);
			icon_text = MainScreen.getAppResources().getString(R.string.settings_mode_wb);
			isEnabled = isWBEnabled;
			break;
		case FOCUS:
			try
			{
				if (mFocusMode == FOCUS_AF_LOCK)
					icon_id = R.drawable.gui_almalence_settings_focus_aflock;
				else
					icon_id = ICONS_FOCUS.get(mFocusMode);
				icon_text = MainScreen.getAppResources().getString(R.string.settings_mode_focus);
				isEnabled = isFocusEnabled;
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e("addQuickSetting", "icons_focus.get exception: " + e.getMessage());
			}
			break;
		case FLASH:
			icon_id = ICONS_FLASH.get(mFlashMode);
			icon_text = MainScreen.getAppResources().getString(R.string.settings_mode_flash);
			isEnabled = isFlashEnabled;
			break;
		case ISO:
			icon_id = ICONS_ISO.get(mISO);
			icon_text = MainScreen.getAppResources().getString(R.string.settings_mode_iso);
			isEnabled = isIsoEnabled;
			break;
		case METERING:
			icon_id = ICONS_METERING.get(mMeteringMode);
			icon_text = MainScreen.getAppResources().getString(R.string.settings_mode_metering);
			isEnabled = isMeteringEnabled;
			break;
		case CAMERA:
			icon_id = ICON_CAM;
			if (!preferences.getBoolean(MainScreen.sUseFrontCameraPref, false))
				icon_text = MainScreen.getAppResources().getString(R.string.settings_mode_rear);
			else
				icon_text = MainScreen.getAppResources().getString(R.string.settings_mode_front);

			isEnabled = isCameraChangeEnabled;
			break;
		case EV:
			icon_id = ICON_EV;
			icon_text = MainScreen.getAppResources().getString(R.string.settings_mode_exposure);
			isEnabled = isEVEnabled;
			break;
		case MORE:
			icon_id = ICON_SETTINGS;
			icon_text = MainScreen.getAppResources().getString(R.string.settings_mode_moresettings);
			break;
		default:
			break;
		}

		// Get required size of button
		LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
		View settingView = inflator.inflate(R.layout.gui_almalence_quick_control_grid_element, null, false);
		ImageView iconView = (ImageView) settingView.findViewById(R.id.imageView);
		iconView.setImageResource(icon_id);
		TextView textView = (TextView) settingView.findViewById(R.id.textView);
		textView.setText(icon_text);

		if (!isEnabled && !isQuickControl)
		{
			iconView.setColorFilter(MainScreen.getMainContext().getResources().getColor(R.color.buttonDisabled),
					PorterDuff.Mode.DST_IN);
			textView.setTextColor(MainScreen.getMainContext().getResources().getColor(R.color.textDisabled));
		}

		// Create onClickListener of right type
		switch (type)
		{
		case SCENE:
			if (isQuickControl)
				createQuickControlSceneOnClick(settingView);
			else
				createSettingSceneOnClick(settingView);
			break;
		case WB:
			if (isQuickControl)
				createQuickControlWBOnClick(settingView);
			else
				createSettingWBOnClick(settingView);
			break;
		case FOCUS:
			if (isQuickControl)
				createQuickControlFocusOnClick(settingView);
			else
				createSettingFocusOnClick(settingView);
			break;
		case FLASH:
			if (isQuickControl)
				createQuickControlFlashOnClick(settingView);
			else
				createSettingFlashOnClick(settingView);
			break;
		case ISO:
			if (isQuickControl)
				createQuickControlIsoOnClick(settingView);
			else
				createSettingIsoOnClick(settingView);
			break;
		case METERING:
			if (isQuickControl)
				createQuickControlMeteringOnClick(settingView);
			else
				createSettingMeteringOnClick(settingView);
			break;
		case CAMERA:
			if (isQuickControl)
				createQuickControlCameraChangeOnClick(settingView);
			else
				createSettingCameraOnClick(settingView);
			break;
		case EV:
			if (isQuickControl)
				createQuickControlEVOnClick(settingView);
			else
				createSettingEVOnClick(settingView);
			break;
		case MORE:
			if (isQuickControl)
				return;
			else
				createSettingMoreOnClick(settingView);
			break;
		default:
			break;
		}

		if (isQuickControl)
			quickControlChangeres.add(settingView);
		else
			settingsViews.add(settingView);
	}

	private void addPluginQuickSetting(Plugin plugin, boolean isQuickControl)
	{
		int iconID = plugin.getQuickControlIconID();
		String title = plugin.getQuickControlTitle();

		if (iconID <= 0)
			return;

		LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
		View qcView = inflator.inflate(R.layout.gui_almalence_quick_control_grid_element, null, false);
		((ImageView) qcView.findViewById(R.id.imageView)).setImageResource(iconID);
		((TextView) qcView.findViewById(R.id.textView)).setText(title);

		createPluginQuickControlOnClick(plugin, qcView, isQuickControl);

		if (isQuickControl)
			quickControlChangeres.add(qcView);
		else
			settingsViews.add(qcView);

		plugin.setQuickControlView(qcView);
	}

	/***************************************************************************************
	 * 
	 * QUICK CONTROLS CUSTOMIZATION METHODS
	 * 
	 * begin >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	 ****************************************************************************************/
	private void initQuickControlsMenu(View currentView)
	{
		quickControlChangeres.clear();
		if (quickControlAdapter.Elements != null)
		{
			quickControlAdapter.Elements.clear();
			quickControlAdapter.notifyDataSetChanged();
		}

		Set<Integer> keys = topMenuButtons.keySet();
		Iterator<Integer> it = keys.iterator();
		while (it.hasNext())
		{
			Integer id = it.next();
			switch (id)
			{
			case R.id.evButton:
				if (mEVSupported)
					addQuickSetting(SettingsType.EV, true);
				break;
			case R.id.sceneButton:
				if (mSceneModeSupported)
					addQuickSetting(SettingsType.SCENE, true);
				break;
			case R.id.wbButton:
				if (mWBSupported)
					addQuickSetting(SettingsType.WB, true);
				break;
			case R.id.focusButton:
				if (mFocusModeSupported)
					addQuickSetting(SettingsType.FOCUS, true);
				break;
			case R.id.flashButton:
				if (mFlashModeSupported)
					addQuickSetting(SettingsType.FLASH, true);
				break;
			case R.id.isoButton:
				if (mISOSupported)
					addQuickSetting(SettingsType.ISO, true);
				break;
			case R.id.meteringButton:
				if (mMeteringAreasSupported)
					addQuickSetting(SettingsType.METERING, true);
				break;
			case R.id.camerachangeButton:
				if (mCameraChangeSupported)
					addQuickSetting(SettingsType.CAMERA, true);
				break;
			default:
				break;
			}
		}

		// Add quick conrols from plugins
		initPluginQuickControls(PluginManager.getInstance().getActivePlugins(PluginType.ViewFinder));
		initPluginQuickControls(PluginManager.getInstance().getActivePlugins(PluginType.Capture));
		initPluginQuickControls(PluginManager.getInstance().getActivePlugins(PluginType.Processing));
		initPluginQuickControls(PluginManager.getInstance().getActivePlugins(PluginType.Filter));
		initPluginQuickControls(PluginManager.getInstance().getActivePlugins(PluginType.Export));

		quickControlAdapter.Elements = quickControlChangeres;
		quickControlAdapter.notifyDataSetChanged();
	}

	private void initPluginQuickControls(List<Plugin> plugins)
	{
		if (!plugins.isEmpty())
		{
			for (int i = 0; i < plugins.size(); i++)
			{
				Plugin plugin = plugins.get(i);
				if (plugin == null)
					continue;

				addPluginQuickSetting(plugin, true);
			}
		}
	}

	private void initPluginSettingsControls(List<Plugin> plugins)
	{
		if (!plugins.isEmpty())
		{
			for (int i = 0; i < plugins.size(); i++)
			{
				Plugin plugin = plugins.get(i);
				if (plugin == null)
					continue;

				addPluginQuickSetting(plugin, false);
			}
		}
	}

	private void createPluginQuickControlOnClick(final Plugin plugin, View view, boolean isQuickControl)
	{
		if (isQuickControl)
			view.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					RotateImageView pluginButton = (RotateImageView) topMenuPluginButtons.get(plugin.getID());
					pluginButton.setImageResource(plugin.getQuickControlIconID());

					switchViews(currentQuickView, pluginButton, plugin.getID());
					recreateQuickControlsMenu();
					changeCurrentQuickControl(pluginButton);
					initQuickControlsMenu(currentQuickView);
					showQuickControlsSettings();
				}
			});
		else
			view.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					try
					{
						plugin.onQuickControlClick();

						int icon_id = plugin.getQuickControlIconID();
						String title = plugin.getQuickControlTitle();
						Drawable icon = MainScreen.getMainContext().getResources().getDrawable(icon_id);
						((ImageView) v.findViewById(R.id.imageView)).setImageResource(icon_id);
						((TextView) v.findViewById(R.id.textView)).setText(title);

						RotateImageView pluginButton = (RotateImageView) topMenuPluginButtons.get(plugin.getID());
						pluginButton.setImageDrawable(icon);

						initSettingsMenu(true);
					} catch (Exception e)
					{
						e.printStackTrace();
						Log.e("Almalence GUI", "createPluginQuickControlOnClick exception" + e.getMessage());
					}
				}
			});
	}

	private void createQuickControlEVOnClick(View ev)
	{
		ev.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				quickControlOnClick(MODE_EV, MainScreen.getMainContext().getResources().getDrawable(ICON_EV));
			}

		});
	}

	private void createQuickControlSceneOnClick(View scene)
	{
		scene.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				quickControlOnClick(MODE_SCENE, MainScreen.getMainContext().getResources().getDrawable(ICONS_SCENE.get(mSceneMode)));
			}
		});
	}

	private void createQuickControlWBOnClick(View wb)
	{
		wb.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				quickControlOnClick(MODE_WB, MainScreen.getMainContext().getResources().getDrawable(ICONS_WB.get(mWB)));
			}
		});
	}

	private void createQuickControlFocusOnClick(View focus)
	{
		focus.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				try
				{
					quickControlOnClick(MODE_FOCUS, MainScreen.getMainContext().getResources().getDrawable(ICONS_FOCUS.get(mFocusMode)));
				} catch (Exception e)
				{
					e.printStackTrace();
					Log.e("createQuickControlFocusOnClick", "icons_focus.get exception: " + e.getMessage());
				}
			}
		});
	}

	private void createQuickControlFlashOnClick(View flash)
	{
		flash.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				quickControlOnClick(MODE_ISO, MainScreen.getMainContext().getResources().getDrawable(ICONS_FLASH.get(mFlashMode)));
			}

		});
	}

	private void createQuickControlIsoOnClick(View iso)
	{
		iso.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				quickControlOnClick(MODE_ISO, MainScreen.getMainContext().getResources().getDrawable(ICONS_ISO.get(mISO)));
			}

		});
	}

	private void createQuickControlMeteringOnClick(View metering)
	{
		metering.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				quickControlOnClick(MODE_MET, MainScreen.getMainContext().getResources()
						.getDrawable(ICONS_METERING.get(mMeteringMode)));
			}

		});
	}

	private void createQuickControlCameraChangeOnClick(View cameraChange)
	{
		cameraChange.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				quickControlOnClick(MODE_CAM, MainScreen.getMainContext().getResources().getDrawable(ICON_CAM));
			}

		});
	}
	
	private void quickControlOnClick(int qc, Drawable icon)
	{
		RotateImageView view = (RotateImageView) topMenuButtons.get(qc);

		view.setImageDrawable(icon);
		
		switchViews(currentQuickView, view, String.valueOf(qc));
		recreateQuickControlsMenu();
		changeCurrentQuickControl(view);
		initQuickControlsMenu(currentQuickView);
		showQuickControlsSettings();
	}

	public void changeCurrentQuickControl(View newCurrent)
	{
		if (currentQuickView != null)
			currentQuickView.setBackgroundResource(R.drawable.transparent_background);

		currentQuickView = newCurrent;
		newCurrent.setBackgroundResource(R.drawable.layout_border_qc_button);
		((RotateImageView) newCurrent).setBackgroundEnabled(true);
	}

	private void showQuickControlsSettings()
	{
		unselectPrimaryTopMenuButtons(-1);
		hideSecondaryMenus();

		GridView gridview = (GridView) guiView.findViewById(R.id.qcGrid);
		gridview.setAdapter(quickControlAdapter);

		((RelativeLayout) guiView.findViewById(R.id.qcLayout)).setVisibility(View.VISIBLE);

		((LinearLayout) guiView.findViewById(R.id.paramsLayout))
				.setBackgroundResource(R.drawable.blacktransparentlayertop);

		Set<Integer> topmenu_keys = topMenuButtons.keySet();
		Iterator<Integer> it = topmenu_keys.iterator();
		while (it.hasNext())
		{
			int key = it.next();
			if (currentQuickView != topMenuButtons.get(key))
			{
				((RotateImageView) topMenuButtons.get(key)).setBackgroundEnabled(true);
				topMenuButtons.get(key).setBackgroundResource(R.drawable.transparent_background);
			}
		}

		quickControlsChangeVisible = true;

		final int degree = AlmalenceGUI.mDeviceOrientation >= 0 ? AlmalenceGUI.mDeviceOrientation % 360
				: AlmalenceGUI.mDeviceOrientation % 360 + 360;
		rotateSquareViews(degree, 0);

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_LOCKED);
	}

	private void closeQuickControlsSettings()
	{
		RelativeLayout gridview = (RelativeLayout) guiView.findViewById(R.id.qcLayout);
		gridview.setVisibility(View.INVISIBLE);
		quickControlsChangeVisible = false;

		currentQuickView.setBackgroundResource(R.drawable.transparent_background);
		currentQuickView = null;

		((LinearLayout) guiView.findViewById(R.id.paramsLayout))
				.setBackgroundResource(R.drawable.blacktransparentlayertop);

		correctTopMenuButtonBackground(MainScreen.getInstance().findViewById(MODE_EV), isEVEnabled);
		correctTopMenuButtonBackground(MainScreen.getInstance().findViewById(MODE_SCENE), isSceneEnabled);
		correctTopMenuButtonBackground(MainScreen.getInstance().findViewById(MODE_WB), isWBEnabled);
		correctTopMenuButtonBackground(MainScreen.getInstance().findViewById(MODE_FOCUS), isFocusEnabled);
		correctTopMenuButtonBackground(MainScreen.getInstance().findViewById(MODE_FLASH), isFlashEnabled);
		correctTopMenuButtonBackground(MainScreen.getInstance().findViewById(MODE_ISO), isIsoEnabled);
		correctTopMenuButtonBackground(MainScreen.getInstance().findViewById(MODE_MET), isMeteringEnabled);
		correctTopMenuButtonBackground(MainScreen.getInstance().findViewById(MODE_CAM), isCameraChangeEnabled);

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);

		guiView.findViewById(R.id.topPanel).setVisibility(View.VISIBLE);
	}

	/***************************************************************************************
	 * 
	 * QUICK CONTROLS CUSTOMIZATION METHODS
	 * 
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< end
	 ***************************************************************************************/

	/***************************************************************************************
	 * 
	 * SETTINGS MENU METHODS
	 * 
	 * begin >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	 ****************************************************************************************/
	private void initSettingsMenu(boolean isDelayed)
	{
		if (isDelayed)
		{
			Handler h = new Handler();
			h.postDelayed(new Runnable()
			{
	
				@Override
				public void run()
				{
					initSettingsMenuBody();
				}
			}, 2000);
		}
		else
			initSettingsMenuBody();
	}
	
	private void initSettingsMenuBody()
	{
		// Clear view list to recreate all settings buttons
		settingsViews.clear();
		if (settingsAdapter.Elements != null)
		{
			settingsAdapter.Elements.clear();
			settingsAdapter.notifyDataSetChanged();
		}

		// Obtain all theoretical buttons we know
		Set<Integer> keys = topMenuButtons.keySet();
		Iterator<Integer> it = keys.iterator();
		while (it.hasNext())
		{
			// If such camera feature is supported then add a button to
			// settings
			// menu
			Integer id = it.next();
			switch (id)
			{
			case R.id.evButton:
				if (mEVSupported)
					addQuickSetting(SettingsType.EV, false);
				break;
			case R.id.sceneButton:
				if (mSceneModeSupported)
					addQuickSetting(SettingsType.SCENE, false);
				break;
			case R.id.wbButton:
				if (mWBSupported)
					addQuickSetting(SettingsType.WB, false);
				break;
			case R.id.focusButton:
				if (mFocusModeSupported)
					addQuickSetting(SettingsType.FOCUS, false);
				break;
			case R.id.flashButton:
				if (mFlashModeSupported)
					addQuickSetting(SettingsType.FLASH, false);
				break;
			case R.id.isoButton:
				if (mISOSupported)
					addQuickSetting(SettingsType.ISO, false);
				break;
			case R.id.meteringButton:
				if (mMeteringAreasSupported)
					addQuickSetting(SettingsType.METERING, false);
				break;
			case R.id.camerachangeButton:
				if (mCameraChangeSupported)
					addQuickSetting(SettingsType.CAMERA, false);
				break;
			default:
				break;
			}
		}

		// Add quick conrols from plugins
		initPluginSettingsControls(PluginManager.getInstance().getActivePlugins(PluginType.ViewFinder));
		initPluginSettingsControls(PluginManager.getInstance().getActivePlugins(PluginType.Capture));
		initPluginSettingsControls(PluginManager.getInstance().getActivePlugins(PluginType.Processing));
		initPluginSettingsControls(PluginManager.getInstance().getActivePlugins(PluginType.Filter));
		initPluginSettingsControls(PluginManager.getInstance().getActivePlugins(PluginType.Export));

		// The very last control is always MORE SETTINGS
		addQuickSetting(SettingsType.MORE, false);

		settingsAdapter.Elements = settingsViews;

		final int degree = AlmalenceGUI.mDeviceOrientation >= 0 ? AlmalenceGUI.mDeviceOrientation % 360
				: AlmalenceGUI.mDeviceOrientation % 360 + 360;
		rotateSquareViews(degree, 0);

		GridView gridview = (GridView) guiView.findViewById(R.id.settingsGrid);
		gridview.setAdapter(settingsAdapter);
		settingsAdapter.notifyDataSetChanged();
	}

	private void createSettingSceneOnClick(View settingView)
	{
		settingView.setOnClickListener(new OnClickListener()
		{
			public void onClick(final View v)
			{
				if (!isSceneEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.BOTTOM,
							MainScreen.getAppResources().getString(R.string.settings_not_available), true, false);
					return;
				}
				int[] supported_scene = CameraController.getSupportedSceneModes();
				if (supported_scene == null)
					return;
				if (supported_scene.length > 0)
				{
					if (iScreenType == 0)
						((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false, false);

					if (guiView.findViewById(R.id.scenemodeLayout).getVisibility() != View.VISIBLE)
					{
						hideSecondaryMenus();
						showParams(MODE_SCENE);
					} else
						hideSecondaryMenus();
				}
			}
		});
	}

	private void createSettingWBOnClick(View settingView)
	{
		settingView.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (!isWBEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							MainScreen.getAppResources().getString(R.string.settings_not_available), true, false);
					return;
				}
				int[] supported_wb = CameraController.getSupportedWhiteBalance();
				if (supported_wb == null)
					return;
				if (supported_wb.length > 0)
				{
					if (iScreenType == 0)
						((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false, false);

					if (guiView.findViewById(R.id.wbLayout).getVisibility() != View.VISIBLE)
					{
						hideSecondaryMenus();
						showParams(MODE_WB);
					}

					else
						hideSecondaryMenus();
				}
			}
		});
	}

	private void createSettingFocusOnClick(View settingView)
	{
		settingView.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (!isFocusEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							MainScreen.getAppResources().getString(R.string.settings_not_available), true, false);
					return;
				}
				int[] supported_focus = CameraController.getSupportedFocusModes();
				if (supported_focus == null)
					return;
				if (supported_focus.length > 0)
				{
					if (iScreenType == 0)
						((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false, false);

					if (guiView.findViewById(R.id.focusmodeLayout).getVisibility() != View.VISIBLE)
					{
						hideSecondaryMenus();
						showParams(MODE_FOCUS);
					} else
						hideSecondaryMenus();
				}
			}
		});
	}

	private void createSettingFlashOnClick(View settingView)
	{
		settingView.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (!isFlashEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							MainScreen.getAppResources().getString(R.string.settings_not_available), true, false);
					return;
				}
				int[] supported_flash = CameraController.getSupportedFlashModes();
				if (supported_flash == null)
					return;
				if (supported_flash.length > 0)
				{
					if (iScreenType == 0)
						((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false, false);

					if (guiView.findViewById(R.id.flashmodeLayout).getVisibility() != View.VISIBLE)
					{
						hideSecondaryMenus();
						showParams(MODE_FLASH);
					} else
						hideSecondaryMenus();
				}
			}
		});
	}

	private void createSettingIsoOnClick(View settingView)
	{
		settingView.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (!isIsoEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							MainScreen.getAppResources().getString(R.string.settings_not_available), true, false);
					return;
				}
				int[] supported_iso = CameraController.getSupportedISO();
				if ((supported_iso != null && supported_iso.length > 0) || CameraController.isISOSupported())
				{
					if (iScreenType == 0)
						((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false, false);

					if (guiView.findViewById(R.id.isoLayout).getVisibility() != View.VISIBLE)
					{
						hideSecondaryMenus();
						showParams(MODE_ISO);
					} else
						hideSecondaryMenus();
				}
			}
		});
	}

	private void createSettingMeteringOnClick(View settingView)
	{
		settingView.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (!isMeteringEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							MainScreen.getAppResources().getString(R.string.settings_not_available), true, false);
					return;
				}
				int iMeteringAreasSupported = CameraController.getMaxNumMeteringAreas();
				if (iMeteringAreasSupported > 0)
				{
					if (iScreenType == 0)
						((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false, false);

					if (guiView.findViewById(R.id.meteringLayout).getVisibility() != View.VISIBLE)
					{
						hideSecondaryMenus();
						showParams(MODE_MET);
					} else
						hideSecondaryMenus();
				}
			}
		});
	}

	private void createSettingCameraOnClick(View settingView)
	{
		settingView.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (!isCameraChangeEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							MainScreen.getAppResources().getString(R.string.settings_not_available), true, false);
					return;
				}
				cameraSwitched(true);
			}
		});
	}

	private void createSettingEVOnClick(View settingView)
	{
		settingView.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (!isEVEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							MainScreen.getAppResources().getString(R.string.settings_not_available), true, false);
					return;
				}
				if (iScreenType == 0)
					((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false, false);

				if (guiView.findViewById(R.id.evLayout).getVisibility() != View.VISIBLE)
				{
					hideSecondaryMenus();
					showParams(MODE_EV);
				} else
					hideSecondaryMenus();
			}
		});
	}

	private void createSettingMoreOnClick(View settingView)
	{
		settingView.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				PluginManager.getInstance().onShowPreferences();
				Intent settingsActivity = new Intent(MainScreen.getMainContext(), Preferences.class);
				MainScreen.getInstance().startActivity(settingsActivity);
				((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false, false);
			}
		});
	}

	/***************************************************************************************
	 * 
	 * SETTINGS MENU METHODS
	 * 
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< end
	 ***************************************************************************************/

	private void cameraSwitched(boolean restart)
	{
		if (PluginManager.getInstance().getProcessingCounter() != 0)
			return;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		boolean isFrontCamera = prefs.getBoolean(
				MainScreen.getMainContext().getResources().getString(R.string.Preference_UseFrontCameraValue), false);
		prefs.edit()
				.putBoolean(
						MainScreen.getMainContext().getResources().getString(R.string.Preference_UseFrontCameraValue),
						!isFrontCamera).commit();

		if (restart)
		{
			MainScreen.getInstance().pauseMain();
			MainScreen.getInstance().resumeMain();
		}
	}

	private void showModeList()
	{
		unselectPrimaryTopMenuButtons(-1);
		hideSecondaryMenus();

		initModeList();
		DisplayMetrics metrics = new DisplayMetrics();
		MainScreen.getInstance().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int width = metrics.widthPixels;
		int modeHeightByWidth = (int) (width / 3 - 5 * metrics.density);
		int modeHeightByDimen = Math.round(MainScreen.getAppResources().getDimension(R.dimen.gridModeImageSize)
				+ MainScreen.getAppResources().getDimension(R.dimen.gridModeTextLayoutSize));

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

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_LOCKED);
	}

	private void hideModeList()
	{
		RelativeLayout gridview = (RelativeLayout) guiView.findViewById(R.id.modeLayout);

		Animation gone = AnimationUtils
				.loadAnimation(MainScreen.getInstance(), R.anim.gui_almalence_modelist_invisible);
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

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);
	}

	@Override
	public void onHardwareShutterButtonPressed()
	{
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);
		PluginManager.getInstance().onShutterClick();
	}

	@Override
	public void onHardwareFocusButtonPressed()
	{
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);
		PluginManager.getInstance().onFocusButtonClick();
	}

	private void shutterButtonPressed()
	{
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);
		lockControls = true;
		PluginManager.getInstance().onShutterClick();
	}

	private void infoSlide(boolean toLeft, float xToVisible, float xToInvisible)
	{
		if ((infoSet == INFO_ALL & !toLeft) && isAnyViewOnViewfinder())
			infoSet = INFO_PARAMS;
		else if (infoSet == INFO_ALL && !isAnyViewOnViewfinder())
			infoSet = INFO_NO;
		else if (isAnyViewOnViewfinder())
			infoSet = (infoSet + 1 * (toLeft ? 1 : -1)) % 4;
		else
			infoSet = INFO_ALL;
		setInfo(toLeft, xToVisible, xToInvisible, true);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		prefs.edit().putInt(MainScreen.sDefaultInfoSetPref, infoSet).commit();
	}

	private void setInfo(boolean toLeft, float xToVisible, float xToInvisible, boolean isAnimate)
	{
		int pluginzoneWidth = guiView.findViewById(R.id.pluginsLayout).getWidth();
		int infozoneWidth = guiView.findViewById(R.id.infoLayout).getWidth();
		int screenWidth = pluginzoneWidth + infozoneWidth;

		AnimationSet rlinvisible = new AnimationSet(true);
		rlinvisible.setInterpolator(new DecelerateInterpolator());

		AnimationSet rlvisible = new AnimationSet(true);
		rlvisible.setInterpolator(new DecelerateInterpolator());

		AnimationSet lrinvisible = new AnimationSet(true);
		lrinvisible.setInterpolator(new DecelerateInterpolator());

		AnimationSet lrvisible = new AnimationSet(true);
		lrvisible.setInterpolator(new DecelerateInterpolator());

		int duration_invisible = 0;
		duration_invisible = isAnimate ? com.almalence.util.Util
				.clamp(Math.abs(Math.round(((toLeft ? xToVisible : (screenWidth - xToVisible)) * 500) / screenWidth)),
						10, 500) : 0;

		int duration_visible = 0;
		duration_visible = isAnimate ? com.almalence.util.Util.clamp(
				Math.abs(Math.round(((toLeft ? xToInvisible : (screenWidth - xToInvisible)) * 500) / screenWidth)), 10,
				500) : 0;

		Animation invisible_alpha = new AlphaAnimation(1, 0);
		invisible_alpha.setDuration(duration_invisible);
		invisible_alpha.setRepeatCount(0);

		Animation visible_alpha = new AlphaAnimation(0, 1);
		visible_alpha.setDuration(duration_visible);
		visible_alpha.setRepeatCount(0);

		Animation rlinvisible_translate = new TranslateAnimation(xToInvisible, -screenWidth, 0, 0);
		rlinvisible_translate.setDuration(duration_invisible);
		rlinvisible_translate.setFillAfter(true);

		Animation lrinvisible_translate = new TranslateAnimation(xToInvisible, screenWidth, 0, 0);
		lrinvisible_translate.setDuration(duration_invisible);
		lrinvisible_translate.setFillAfter(true);

		Animation rlvisible_translate = new TranslateAnimation(xToVisible, 0, 0, 0);
		rlvisible_translate.setDuration(duration_visible);
		rlvisible_translate.setFillAfter(true);

		Animation lrvisible_translate = new TranslateAnimation(xToVisible, 0, 0, 0);
		lrvisible_translate.setDuration(duration_visible);
		lrvisible_translate.setFillAfter(true);

		// Add animations to appropriate set
		rlinvisible.addAnimation(invisible_alpha);
		rlinvisible.addAnimation(rlinvisible_translate);

		rlvisible.addAnimation(rlvisible_translate);

		lrinvisible.addAnimation(invisible_alpha);
		lrinvisible.addAnimation(lrinvisible_translate);

		lrvisible.addAnimation(lrvisible_translate);

		int[] zonesVisibility = new int[] { View.GONE, View.GONE, View.GONE };
		switch (infoSet)
		{
		case INFO_ALL:
			zonesVisibility = initZonesVisibility(View.VISIBLE, View.VISIBLE, View.VISIBLE);
			break;

		case INFO_NO:
			zonesVisibility = initZonesVisibility(View.GONE, View.GONE, View.GONE);
			break;

		case INFO_GRID:
			zonesVisibility = initZonesVisibility(View.GONE, View.GONE, View.VISIBLE);
			break;

		case INFO_PARAMS:
			zonesVisibility = initZonesVisibility(View.VISIBLE, View.GONE, View.GONE);
			break;
		default:
			break;
		}
		applyZonesVisibility(zonesVisibility, isAnimate, toLeft, rlvisible, lrvisible, rlinvisible, lrinvisible);

		rlinvisible.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationEnd(Animation animation)
			{
				clearLayoutsAnimation();
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

		lrinvisible.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationEnd(Animation animation)
			{
				clearLayoutsAnimation();
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

		// checks preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		Editor prefsEditor = prefs.edit();
		prefsEditor.putInt(MainScreen.sDefaultInfoSetPref, infoSet);
		prefsEditor.commit();
	}

	private int[] initZonesVisibility(final int zoneVisibiltyFirst, final int zoneVisibiltySecond,
			final int zoneVisibilityThird)
	{
		int[] zonesVisibility = new int[3];
		zonesVisibility[0] = zoneVisibiltyFirst;
		zonesVisibility[1] = zoneVisibiltySecond;
		zonesVisibility[2] = zoneVisibilityThird;

		return zonesVisibility;
	}

	private void applyZonesVisibility(int[] zonesVisibility, boolean isAnimate, boolean toLeft, AnimationSet rlvisible,
			AnimationSet lrvisible, AnimationSet rlinvisible, AnimationSet lrinvisible)
	{
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		if (guiView.findViewById(R.id.paramsLayout).getVisibility() != zonesVisibility[0])
		{
			if (isAnimate)
				guiView.findViewById(R.id.paramsLayout).startAnimation(
						toLeft ? zonesVisibility[0] == View.VISIBLE ? rlvisible : rlinvisible
								: zonesVisibility[0] == View.VISIBLE ? lrvisible : lrinvisible);
			guiView.findViewById(R.id.paramsLayout).setVisibility(zonesVisibility[0]);
			((Panel) guiView.findViewById(R.id.topPanel)).reorder(zonesVisibility[0] == View.GONE, true);
		}

		if (guiView.findViewById(R.id.pluginsLayout).getVisibility() != zonesVisibility[1])
		{
			if (isAnimate)
				guiView.findViewById(R.id.pluginsLayout).startAnimation(
						toLeft ? zonesVisibility[1] == View.VISIBLE ? rlvisible : rlinvisible
								: zonesVisibility[1] == View.VISIBLE ? lrvisible : lrinvisible);
			guiView.findViewById(R.id.pluginsLayout).setVisibility(zonesVisibility[1]);

			if (isAnimate)
				guiView.findViewById(R.id.infoLayout).startAnimation(
						toLeft ? zonesVisibility[1] == View.VISIBLE ? rlvisible : rlinvisible
								: zonesVisibility[1] == View.VISIBLE ? lrvisible : lrinvisible);
			guiView.findViewById(R.id.infoLayout).setVisibility(zonesVisibility[1]);
		}

		if (guiView.findViewById(R.id.fullscreenLayout).getVisibility() != zonesVisibility[2])
		{
			if (isAnimate)
				guiView.findViewById(R.id.fullscreenLayout).startAnimation(
						toLeft ? zonesVisibility[2] == View.VISIBLE ? rlvisible : rlinvisible
								: zonesVisibility[2] == View.VISIBLE ? lrvisible : lrinvisible);
			guiView.findViewById(R.id.fullscreenLayout).setVisibility(zonesVisibility[2]);
		}
	}

	private void clearLayoutsAnimation()
	{
		guiView.findViewById(R.id.paramsLayout).clearAnimation();
		guiView.findViewById(R.id.pluginsLayout).clearAnimation();
		guiView.findViewById(R.id.fullscreenLayout).clearAnimation();
		guiView.findViewById(R.id.infoLayout).clearAnimation();
	}

	// Method used by quick controls customization feature. Swaps current quick
	// control with selected from qc grid.
	private void switchViews(View currentView, View newView, String qcID)
	{
		if (currentView == newView)
			return;

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

		int currentQCNumber = -1;
		String currentViewID = "";
		if (currentView == quickControl1)
		{
			currentViewID = pref.getString(
					MainScreen.getAppResources().getString(R.string.Preference_QuickControlButton1), "");
			currentQCNumber = 1;
		} else if (currentView == quickControl2)
		{
			currentViewID = pref.getString(
					MainScreen.getAppResources().getString(R.string.Preference_QuickControlButton2), "");
			currentQCNumber = 2;
		} else if (currentView == quickControl3)
		{
			currentViewID = pref.getString(
					MainScreen.getAppResources().getString(R.string.Preference_QuickControlButton3), "");
			currentQCNumber = 3;
		} else if (currentView == quickControl4)
		{
			currentViewID = pref.getString(
					MainScreen.getAppResources().getString(R.string.Preference_QuickControlButton4), "");
			currentQCNumber = 4;
		}

		if (newView == quickControl1)
		{
			quickControl1 = currentView;
			pref.edit()
					.putString(MainScreen.getAppResources().getString(R.string.Preference_QuickControlButton1),
							currentViewID).commit();
		} else if (newView == quickControl2)
		{
			quickControl2 = currentView;
			pref.edit()
					.putString(MainScreen.getAppResources().getString(R.string.Preference_QuickControlButton2),
							currentViewID).commit();
		} else if (newView == quickControl3)
		{
			quickControl3 = currentView;
			pref.edit()
					.putString(MainScreen.getAppResources().getString(R.string.Preference_QuickControlButton3),
							currentViewID).commit();
		} else if (newView == quickControl4)
		{
			quickControl4 = currentView;
			pref.edit()
					.putString(MainScreen.getAppResources().getString(R.string.Preference_QuickControlButton4),
							currentViewID).commit();
		} else
		{
			if (currentView.getParent() != null)
				((ViewGroup) currentView.getParent()).removeView(currentView);
		}

		switch (currentQCNumber)
		{
		case 1:
			quickControl1 = newView;
			pref.edit().putString("quickControlButton1", qcID).commit();
			break;
		case 2:
			quickControl2 = newView;
			pref.edit().putString("quickControlButton2", qcID).commit();
			break;
		case 3:
			quickControl3 = newView;
			pref.edit().putString("quickControlButton3", qcID).commit();
			break;
		case 4:
			quickControl4 = newView;
			pref.edit().putString("quickControlButton4", qcID).commit();
			break;
		default:
			break;
		}
	}

	private void recreateQuickControlsMenu()
	{
		removeAllViews(topMenuButtons);
		removeAllViews(topMenuPluginButtons);
		removeAllQuickViews();

		((LinearLayout) guiView.findViewById(R.id.paramsLayout)).addView(quickControl1);
		((LinearLayout) guiView.findViewById(R.id.paramsLayout)).addView(quickControl2);
		((LinearLayout) guiView.findViewById(R.id.paramsLayout)).addView(quickControl3);
		((LinearLayout) guiView.findViewById(R.id.paramsLayout)).addView(quickControl4);
	}

	private void removeAllQuickViews()
	{
		if (quickControl1 != null && quickControl1.getParent() != null)
			((ViewGroup) quickControl1.getParent()).removeView(quickControl1);
		if (quickControl2 != null && quickControl2.getParent() != null)
			((ViewGroup) quickControl2.getParent()).removeView(quickControl2);
		if (quickControl3 != null && quickControl3.getParent() != null)
			((ViewGroup) quickControl3.getParent()).removeView(quickControl3);
		if (quickControl4 != null && quickControl4.getParent() != null)
			((ViewGroup) quickControl4.getParent()).removeView(quickControl4);
	}

	private void removeAllViews(Map<?, View> buttons)
	{
		Collection<View> button_set = buttons.values();
		Iterator<View> it = button_set.iterator();
		while (it.hasNext())
		{
			View view = it.next();
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);
		}
	}

	private void removePluginViews()
	{
		List<View> pluginsView = new ArrayList<View>();
		RelativeLayout pluginsLayout = (RelativeLayout) MainScreen.getInstance().findViewById(R.id.pluginsLayout);
		for (int i = 0; i < pluginsLayout.getChildCount(); i++)
			pluginsView.add(pluginsLayout.getChildAt(i));

		for (int j = 0; j < pluginsView.size(); j++)
		{
			View view = pluginsView.get(j);
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);

			pluginsLayout.removeView(view);
		}

		List<View> fullscreenView = new ArrayList<View>();
		RelativeLayout fullscreenLayout = (RelativeLayout) MainScreen.getInstance().findViewById(R.id.fullscreenLayout);
		for (int i = 0; i < fullscreenLayout.getChildCount(); i++)
			fullscreenView.add(fullscreenLayout.getChildAt(i));

		for (int j = 0; j < fullscreenView.size(); j++)
		{
			View view = fullscreenView.get(j);
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);

			fullscreenLayout.removeView(view);
		}

		List<View> infoView = new ArrayList<View>();
		LinearLayout infoLayout = (LinearLayout) MainScreen.getInstance().findViewById(R.id.infoLayout);
		for (int i = 0; i < infoLayout.getChildCount(); i++)
			infoView.add(infoLayout.getChildAt(i));

		for (int j = 0; j < infoView.size(); j++)
		{
			View view = infoView.get(j);
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);

			infoLayout.removeView(view);
		}
	}

	public boolean onLongClick(View v)
	{
		if (quickControlsChangeVisible)
			return true;

		if (modeSelectorVisible)
			return true;

		if (shutterButton != v)
		{
			changeCurrentQuickControl(v);

			initQuickControlsMenu(v);
			showQuickControlsSettings();
			guiView.findViewById(R.id.topPanel).setVisibility(View.GONE);
		}
		return true;
	}

	@Override
	public void onClick(View v)
	{
		hideSecondaryMenus();
		if (!quickControlsChangeVisible)
		{
			if (topMenuPluginButtons.containsValue(v))
			{
				Set<String> pluginIDset = topMenuPluginButtons.keySet();
				Iterator<String> it = pluginIDset.iterator();
				while (it.hasNext())
				{
					String pluginID = it.next();
					if (v == topMenuPluginButtons.get(pluginID))
					{
						Plugin plugin = PluginManager.getInstance().getPlugin(pluginID);
						plugin.onQuickControlClick();

						int icon_id = plugin.getQuickControlIconID();
						Drawable icon = MainScreen.getMainContext().getResources().getDrawable(icon_id);
						((RotateImageView) v).setImageDrawable(icon);

						initSettingsMenu(false);
						break;
					}
				}
			}
			return;
		}

		changeCurrentQuickControl(v);

		initQuickControlsMenu(v);
		showQuickControlsSettings();
	}

	@Override
	public void onButtonClick(View button)
	{
		// hide hint screen
		if (guiView.findViewById(R.id.hintLayout).getVisibility() == View.VISIBLE)
			guiView.findViewById(R.id.hintLayout).setVisibility(View.INVISIBLE);

		if (guiView.findViewById(R.id.mode_help).getVisibility() == View.VISIBLE)
			guiView.findViewById(R.id.mode_help).setVisibility(View.INVISIBLE);

		int id = button.getId();
		if (lockControls && R.id.buttonShutter != id)
			return;

		// 1. if quick settings slider visible - lock everything
		// 2. if modes visible - allow only selectmode button
		// 3. if change quick controls visible - allow only OK button

		if (settingsControlsVisible || quickControlsChangeVisible
				|| (modeSelectorVisible && (R.id.buttonSelectMode != id)))
		{
			// if change control visible and
			if (quickControlsChangeVisible)
			{
				// quick control button pressed
				if ((button != quickControl1) && (button != quickControl2) && (button != quickControl3)
						&& (button != quickControl4))
				{
					closeQuickControlsSettings();
					guiView.findViewById(R.id.topPanel).setVisibility(View.VISIBLE);
					return;
				}
			}
			if (settingsControlsVisible)
			{
				((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false, true);
				return;
			}
			if (modeSelectorVisible)
			{
				hideModeList();
				return;
			}
		}

		switch (id)
		{
		// BOTTOM BUTTONS - Modes, Shutter
		case R.id.buttonSelectMode:
			if (quickControlsChangeVisible || settingsControlsVisible)
				break;

			if (!modeSelectorVisible)
				showModeList();
			else
				hideModeList();
			break;

		case R.id.buttonShutter:
			if (quickControlsChangeVisible || settingsControlsVisible)
			{
				break;
			}

			if (quickControlsVisible)
			{
				hideSecondaryMenus();
				unselectPrimaryTopMenuButtons(-1);
				guiView.findViewById(R.id.topPanel).setVisibility(View.VISIBLE);
				quickControlsVisible = false;
				break;
			}

			shutterButtonPressed();
			break;

		case R.id.buttonGallery:
			if (quickControlsChangeVisible || settingsControlsVisible)
				break;

			openGallery(false);
			break;

		// TOP MENU BUTTONS - Scene mode, white balance, focus mode, flash mode,
		// settings
		case R.id.evButton:
			{
				if (changeQuickControlIfVisible(button))
					break;

				if (!isEVEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							MainScreen.getAppResources().getString(R.string.settings_not_available), true, false);
					break;
				}

				LinearLayout layout = (LinearLayout) guiView.findViewById(R.id.evLayout);
				if (layout.getVisibility() == View.GONE)
				{
					unselectPrimaryTopMenuButtons(MODE_EV);
					hideSecondaryMenus();
					showParams(MODE_EV);
					quickControlsVisible = true;
				} else
				{
					quickControlsVisible = false;
					unselectPrimaryTopMenuButtons(-1);
					hideSecondaryMenus();
				}
			}
			break;
		case R.id.sceneButton:
			{
				if (changeQuickControlIfVisible(button))
					break;

				if (!isSceneEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							MainScreen.getAppResources().getString(R.string.settings_not_available), true, false);
					break;
				}

				RelativeLayout layout = (RelativeLayout) guiView.findViewById(R.id.scenemodeLayout);
				if (layout.getVisibility() == View.GONE)
				{
					quickControlsVisible = true;
					unselectPrimaryTopMenuButtons(MODE_SCENE);
					hideSecondaryMenus();
					showParams(MODE_SCENE);
				} else
				{
					quickControlsVisible = false;
					unselectPrimaryTopMenuButtons(-1);
					hideSecondaryMenus();
				}
			}
			break;
		case R.id.wbButton:
			{
				if (changeQuickControlIfVisible(button))
					break;

				if (!isWBEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							MainScreen.getAppResources().getString(R.string.settings_not_available), true, false);
					break;
				}

				RelativeLayout layout = (RelativeLayout) guiView.findViewById(R.id.wbLayout);
				if (layout.getVisibility() == View.GONE)
				{
					quickControlsVisible = true;
					unselectPrimaryTopMenuButtons(MODE_WB);
					hideSecondaryMenus();
					showParams(MODE_WB);
				} else
				{
					quickControlsVisible = false;
					unselectPrimaryTopMenuButtons(-1);
					hideSecondaryMenus();
				}
			}
			break;
		case R.id.focusButton:
			{
				if (changeQuickControlIfVisible(button))
					break;

				if (!isFocusEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							MainScreen.getAppResources().getString(R.string.settings_not_available), true, false);
					break;
				}

				RelativeLayout layout = (RelativeLayout) guiView.findViewById(R.id.focusmodeLayout);
				if (layout.getVisibility() == View.GONE)
				{
					quickControlsVisible = true;
					unselectPrimaryTopMenuButtons(MODE_FOCUS);
					hideSecondaryMenus();
					showParams(MODE_FOCUS);
				} else
				{
					quickControlsVisible = false;
					unselectPrimaryTopMenuButtons(-1);
					hideSecondaryMenus();
				}
			}
			break;
		case R.id.flashButton:
			{
				if (changeQuickControlIfVisible(button))
					break;

				if (!isFlashEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							MainScreen.getAppResources().getString(R.string.settings_not_available), true, false);
					break;
				}

				RelativeLayout layout = (RelativeLayout) guiView.findViewById(R.id.flashmodeLayout);
				if (layout.getVisibility() == View.GONE)
				{
					quickControlsVisible = true;
					unselectPrimaryTopMenuButtons(MODE_FLASH);
					hideSecondaryMenus();
					showParams(MODE_FLASH);
				} else
				{
					quickControlsVisible = false;
					unselectPrimaryTopMenuButtons(-1);
					hideSecondaryMenus();
				}
			}
			break;
		case R.id.isoButton:
			{
				if (changeQuickControlIfVisible(button))
					break;

				if (!isIsoEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							MainScreen.getAppResources().getString(R.string.settings_not_available), true, false);
					break;
				}

				RelativeLayout layout = (RelativeLayout) guiView.findViewById(R.id.isoLayout);
				if (layout.getVisibility() == View.GONE)
				{
					quickControlsVisible = true;
					unselectPrimaryTopMenuButtons(MODE_ISO);
					hideSecondaryMenus();
					showParams(MODE_ISO);
				} else
				{
					quickControlsVisible = false;
					unselectPrimaryTopMenuButtons(-1);
					hideSecondaryMenus();
				}
			}
			break;
		case R.id.meteringButton:
			{
				if (changeQuickControlIfVisible(button))
					break;

				if (!isMeteringEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							MainScreen.getAppResources().getString(R.string.settings_not_available), true, false);
					break;
				}

				RelativeLayout layout = (RelativeLayout) guiView.findViewById(R.id.meteringLayout);
				if (layout.getVisibility() == View.GONE)
				{
					quickControlsVisible = true;
					unselectPrimaryTopMenuButtons(MODE_MET);
					hideSecondaryMenus();
					showParams(MODE_MET);
				} else
				{
					quickControlsVisible = false;
					unselectPrimaryTopMenuButtons(-1);
					hideSecondaryMenus();
				}
			}
			break;
		case R.id.camerachangeButton:
			{
				if (changeQuickControlIfVisible(button))
					break;

				unselectPrimaryTopMenuButtons(-1);
				hideSecondaryMenus();

				if (!isCameraChangeEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							MainScreen.getAppResources().getString(R.string.settings_not_available), true, false);
					break;
				}

				cameraSwitched(true);
				break;
			}

		// EXPOSURE COMPENSATION BUTTONS (-\+)
		case R.id.evMinusButton:
			expoMinus();
			break;
		case R.id.evPlusButton:
			expoPlus();
			break;
		default:
			break;
		}
		this.initSettingsMenu(false);
	}

	private boolean changeQuickControlIfVisible(View button)
	{
		if (quickControlsChangeVisible)
		{
			changeCurrentQuickControl(button);
			initQuickControlsMenu(button);
			showQuickControlsSettings();
			return true;
		}

		return false;
	}

	private void setSceneMode(int newMode)
	{
		if (newMode != -1 && sceneModeButtons.containsKey(newMode))
		{
			if (newMode != CameraParameters.SCENE_MODE_AUTO)
				CameraController.setCameraISO(CameraParameters.ISO_AUTO);
			CameraController.setCameraSceneMode(newMode);
			mSceneMode = newMode;
		} else if (sceneModeButtons.containsKey(CameraParameters.SCENE_MODE_AUTO))
		{
			CameraController.setCameraSceneMode(CameraParameters.SCENE_MODE_AUTO);
			mSceneMode = CameraParameters.SCENE_MODE_AUTO;
		} else if (CameraController.getSupportedSceneModes() != null)
		{
			CameraController.setCameraSceneMode(CameraController.getSupportedSceneModes()[0]);
			mSceneMode = CameraController.getSupportedSceneModes()[0];
		}

		// After change scene mode it may be changed other stuff such as
		// flash, wb, focus mode.
		// Need to get this information and update state of current
		// parameters.
		int wbNew = CameraController.getWBMode();
		int flashNew = CameraController.getFlashMode();
		int focusNew = CameraController.getFocusMode();
		int isoNew = CameraController.getISOMode();

		// Save new params value
		if (wbNew != -1 && wbModeButtons.containsKey(wbNew))
			mWB = wbNew;
		else if (wbModeButtons.containsKey(CameraParameters.WB_MODE_AUTO))
			mWB = CameraParameters.WB_MODE_AUTO;
		else if (CameraController.isWhiteBalanceSupported())
			mWB = CameraController.getSupportedWhiteBalance()[0];
		else
			mWB = -1;

		if (focusNew != -1 && focusModeButtons.containsKey(focusNew))
			mFocusMode = focusNew;
		else if (focusModeButtons.containsKey(CameraParameters.AF_MODE_AUTO))
			mFocusMode = CameraParameters.AF_MODE_AUTO;
		else if (CameraController.isFocusModeSupported())
			mFocusMode = CameraController.getSupportedFocusModes()[0];
		else
			mFocusMode = -1;

		if (flashNew != -1 && flashModeButtons.containsKey(flashNew))
			mFlashMode = flashNew;
		else if (focusModeButtons.containsKey(CameraParameters.FLASH_MODE_AUTO))
			mFlashMode = CameraParameters.FLASH_MODE_AUTO;
		else if (CameraController.isFlashModeSupported())
			mFlashMode = CameraController.getSupportedFlashModes()[0];
		else
			mFlashMode = -1;

		if (isoNew != -1 && isoButtons.containsKey(isoNew))
			mISO = isoNew;
		else if (isoButtons.containsKey(CameraParameters.ISO_AUTO))
			mISO = CameraParameters.ISO_AUTO;
		else if (CameraController.getSupportedISO() != null)
			mISO = CameraController.getSupportedISO()[0];
		else
			mISO = -1;

		// Set appropriate params buttons pressed
		setButtonSelected(sceneModeButtons, mSceneMode);
		setButtonSelected(wbModeButtons, mWB);
		setButtonSelected(focusModeButtons, mFocusMode);
		setButtonSelected(flashModeButtons, mFlashMode);
		setButtonSelected(isoButtons, mISO);

		// Update icons for other camera parameter buttons
		RotateImageView but = null;
		int icon_id = -1;
		if (mWB != -1)
		{
			but = (RotateImageView) topMenuButtons.get(MODE_WB);
			icon_id = ICONS_WB.get(mWB);
			but.setImageResource(icon_id);
			preferences.edit().putInt(MainScreen.sWBModePref, mWB).commit();

			PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_WB_CHANGED);
		}
		if (mFocusMode != -1)
		{
			try
			{
				but = (RotateImageView) topMenuButtons.get(MODE_FOCUS);
				icon_id = ICONS_FOCUS.get(mFocusMode);
				but.setImageResource(icon_id);
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e("setSceneMode", "icons_focus.get exception: " + e.getMessage());
			}

			preferences
					.edit()
					.putInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref
							: MainScreen.sFrontFocusModePref, mFocusMode).commit();

			PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_FOCUS_CHANGED);
		}
		if (mFlashMode != -1)
		{
			but = (RotateImageView) topMenuButtons.get(MODE_FLASH);
			icon_id = ICONS_FLASH.get(mFlashMode);
			but.setImageResource(icon_id);
			preferences.edit().putInt(MainScreen.sFlashModePref, mFlashMode).commit();

			PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_FLASH_CHANGED);
		}
		if (mISO != -1)
		{
			but = (RotateImageView) topMenuButtons.get(MODE_ISO);
			icon_id = ICONS_ISO.get(mISO);
			but.setImageResource(icon_id);
			preferences.edit().putInt(MainScreen.sISOPref, mISO).commit();

			PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_ISO_CHANGED);
		}
		preferences.edit().putInt(MainScreen.sSceneModePref, newMode).commit();

		but = (RotateImageView) topMenuButtons.get(MODE_SCENE);
		icon_id = ICONS_SCENE.get(mSceneMode);
		but.setImageResource(icon_id);

		initSettingsMenu(false);
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_SCENE_CHANGED);
	}

	private void setWhiteBalance(int newMode)
	{
		if (newMode != -1)
		{
			if ((mSceneMode != CameraParameters.SCENE_MODE_AUTO || mWB != newMode)
					&& CameraController.isSceneModeSupported())
				setSceneMode(CameraParameters.SCENE_MODE_AUTO);

			CameraController.setCameraWhiteBalance(newMode);

			mWB = newMode;
			setButtonSelected(wbModeButtons, mWB);

			preferences.edit().putInt(MainScreen.sWBModePref, newMode).commit();
		}

		RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_WB);
		int icon_id = ICONS_WB.get(mWB);
		but.setImageResource(icon_id);

		initSettingsMenu(false);
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_WB_CHANGED);
	}

	private void setFocusMode(int newMode)
	{
		if (newMode != -1)
		{
			if (mSceneMode != CameraParameters.SCENE_MODE_AUTO && mFocusMode != CameraParameters.AF_MODE_AUTO)
				if (CameraController.isSceneModeSupported())
					setSceneMode(CameraParameters.SCENE_MODE_AUTO);

			CameraController.setCameraFocusMode(newMode);

			mFocusMode = newMode;
			setButtonSelected(focusModeButtons, mFocusMode);

			preferences
					.edit()
					.putInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref
							: MainScreen.sFrontFocusModePref, newMode).commit();
		}

		try
		{
			RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_FOCUS);
			int icon_id = ICONS_FOCUS.get(mFocusMode);
			but.setImageResource(icon_id);
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("setFocusMode", "icons_focus.get exception: " + e.getMessage());
		}

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_FOCUS_CHANGED);

		initSettingsMenu(false);
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		MainScreen.setAutoFocusLock(false);
	}

	private void setFlashMode(int newMode)
	{
		if (newMode != -1)
		{
			if (mSceneMode != CameraParameters.SCENE_MODE_AUTO && mFlashMode != CameraParameters.FLASH_MODE_AUTO
					&& CameraController.isSceneModeSupported())
				setSceneMode(CameraParameters.SCENE_MODE_AUTO);

			CameraController.setCameraFlashMode(newMode);
			mFlashMode = newMode;
			setButtonSelected(flashModeButtons, mFlashMode);

			preferences.edit().putInt(MainScreen.sFlashModePref, newMode).commit();
		}

		RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_FLASH);
		int icon_id = ICONS_FLASH.get(mFlashMode);
		but.setImageResource(icon_id);

		initSettingsMenu(false);
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_FLASH_CHANGED);
	}

	private void setISO(int newMode)
	{
		if (newMode != -1)
		{
			if (mSceneMode != CameraParameters.SCENE_MODE_AUTO && CameraController.isSceneModeSupported())
				setSceneMode(CameraParameters.SCENE_MODE_AUTO);

			CameraController.setCameraISO(newMode);
			mISO = newMode;
			setButtonSelected(isoButtons, mISO);

			preferences.edit().putInt(MainScreen.sISOPref, newMode).commit();
		}

		RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_ISO);
		int icon_id = ICONS_ISO.get(mISO);
		but.setImageResource(icon_id);

		initSettingsMenu(false);
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_ISO_CHANGED);
	}

	private void setMeteringMode(int newMode)
	{
		if (newMode != -1 && mMeteringMode != newMode)
		{
			mMeteringMode = newMode;
			setButtonSelected(meteringModeButtons, mMeteringMode);

			preferences.edit().putInt(MainScreen.sMeteringModePref, newMode).commit();
			MainScreen.getInstance().setCameraMeteringMode(newMode);
		}

		RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_MET);
		int icon_id = ICONS_METERING.get(mMeteringMode);
		but.setImageResource(icon_id);

		initSettingsMenu(false);
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);
	}

	// Hide all pop-up layouts
	private void unselectPrimaryTopMenuButtons(int iTopMenuButtonSelected)
	{
		Set<Integer> keys = topMenuButtons.keySet();
		Iterator<Integer> it = keys.iterator();
		while (it.hasNext())
		{
			Integer it_button = it.next();
			(topMenuButtons.get(it_button)).setPressed(false);
			(topMenuButtons.get(it_button)).setSelected(false);
		}

		if ((iTopMenuButtonSelected > -1) && topMenuButtons.containsKey(iTopMenuButtonSelected))
		{
			RotateImageView pressed_button = (RotateImageView) topMenuButtons.get(iTopMenuButtonSelected);
			pressed_button.setPressed(false);
			pressed_button.setSelected(true);
		}

		quickControlsVisible = false;
	}

	private int findTopMenuButtonIndex(View view)
	{
		Set<Integer> keys = topMenuButtons.keySet();
		Iterator<Integer> it = keys.iterator();
		Integer pressed_button = -1;
		while (it.hasNext())
		{
			Integer it_button = it.next();
			View v = topMenuButtons.get(it_button);
			if (v == view)
			{
				pressed_button = it_button;
				break;
			}
		}

		return pressed_button;
	}

	private void topMenuButtonPressed(int iTopMenuButtonPressed)
	{
		Set<Integer> keys = topMenuButtons.keySet();
		Iterator<Integer> it = keys.iterator();
		while (it.hasNext())
		{
			Integer it_button = it.next();
			if (isTopMenuButtonEnabled(it_button))
				(topMenuButtons.get(it_button)).setBackgroundDrawable(MainScreen.getMainContext().getResources()
						.getDrawable(R.drawable.transparent_background));
		}

		if ((iTopMenuButtonPressed > -1 && isTopMenuButtonEnabled(iTopMenuButtonPressed))
				&& topMenuButtons.containsKey(iTopMenuButtonPressed))
		{
			RotateImageView pressed_button = (RotateImageView) topMenuButtons.get(iTopMenuButtonPressed);
			pressed_button.setBackgroundDrawable(MainScreen.getMainContext().getResources()
					.getDrawable(R.drawable.almalence_gui_button_background_pressed));

		}
	}

	public boolean isTopMenuButtonEnabled(int iTopMenuButtonKey)
	{
		boolean isEnabled = false;
		switch (iTopMenuButtonKey)
		{
		case MODE_SCENE:
			if (isSceneEnabled)
				isEnabled = true;
			break;
		case MODE_WB:
			if (isWBEnabled)
				isEnabled = true;
			break;
		case MODE_FOCUS:
			if (isFocusEnabled)
				isEnabled = true;
			break;
		case MODE_FLASH:
			if (isFlashEnabled)
				isEnabled = true;
			break;
		case MODE_EV:
			if (isEVEnabled)
				isEnabled = true;
			break;
		case MODE_ISO:
			if (isIsoEnabled)
				isEnabled = true;
			break;
		case MODE_MET:
			if (isMeteringEnabled)
				isEnabled = true;
			break;
		case MODE_CAM:
			if (isCameraChangeEnabled)
				isEnabled = true;
			break;
		default:
			break;
		}

		return isEnabled;
	}

	// Hide all pop-up layouts
	@Override
	public void hideSecondaryMenus()
	{
		if (!isSecondaryMenusVisible())
			return;

		guiView.findViewById(R.id.evLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.scenemodeLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.wbLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.focusmodeLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.flashmodeLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.isoLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.meteringLayout).setVisibility(View.GONE);

		guiView.findViewById(R.id.modeLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.vfLayout).setVisibility(View.GONE);

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);
	}

	public boolean isSecondaryMenusVisible()
	{
		if (guiView.findViewById(R.id.evLayout).getVisibility() == View.VISIBLE
				|| guiView.findViewById(R.id.scenemodeLayout).getVisibility() == View.VISIBLE
				|| guiView.findViewById(R.id.wbLayout).getVisibility() == View.VISIBLE
				|| guiView.findViewById(R.id.focusmodeLayout).getVisibility() == View.VISIBLE
				|| guiView.findViewById(R.id.flashmodeLayout).getVisibility() == View.VISIBLE
				|| guiView.findViewById(R.id.isoLayout).getVisibility() == View.VISIBLE
				|| guiView.findViewById(R.id.meteringLayout).getVisibility() == View.VISIBLE)
			return true;
		return false;
	}

	// Decide what layout to show when some main's parameters button is clicked
	private void showParams(int iButton)
	{
		DisplayMetrics metrics = new DisplayMetrics();
		MainScreen.getInstance().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int width = metrics.widthPixels;
		int modeHeightByWidth = (int) (width / 3 - 5 * metrics.density);
		int modeHeightByDimen = Math.round(MainScreen.getAppResources().getDimension(R.dimen.gridImageSize)
				+ MainScreen.getAppResources().getDimension(R.dimen.gridTextLayoutHeight));

		int modeHeight = modeHeightByDimen > modeHeightByWidth ? modeHeightByWidth : modeHeightByDimen;

		AbsListView.LayoutParams params = new AbsListView.LayoutParams(LayoutParams.WRAP_CONTENT, modeHeight);

		List<View> views = null;
		switch (iButton)
		{
		case MODE_SCENE:
			views = activeScene;
			break;
		case MODE_WB:
			views = activeWB;
			break;
		case MODE_FOCUS:
			views = activeFocus;
			break;
		case MODE_FLASH:
			views = activeFlash;
			break;
		case MODE_ISO:
			views = activeISO;
			break;
		case MODE_MET:
			views = activeMetering;
			break;
		default:
			break;
		}

		if (views != null)
		{
			for (int i = 0; i < views.size(); i++)
			{
				View param = views.get(i);
				if (param != null)
					param.setLayoutParams(params);
			}
		}

		switch (iButton)
		{
		case MODE_EV:
			guiView.findViewById(R.id.evLayout).setVisibility(View.VISIBLE);
			break;
		case MODE_SCENE:
			guiView.findViewById(R.id.scenemodeLayout).setVisibility(View.VISIBLE);
			break;
		case MODE_WB:
			guiView.findViewById(R.id.wbLayout).setVisibility(View.VISIBLE);
			break;
		case MODE_FOCUS:
			guiView.findViewById(R.id.focusmodeLayout).setVisibility(View.VISIBLE);
			break;
		case MODE_FLASH:
			guiView.findViewById(R.id.flashmodeLayout).setVisibility(View.VISIBLE);
			break;
		case MODE_ISO:
			guiView.findViewById(R.id.isoLayout).setVisibility(View.VISIBLE);
			break;
		case MODE_MET:
			guiView.findViewById(R.id.meteringLayout).setVisibility(View.VISIBLE);
			break;
		default:
			break;
		}

		quickControlsVisible = true;

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_LOCKED);
	}

	private void setButtonSelected(Map<Integer, View> buttonsList, int mode_id)
	{
		Set<Integer> keys = buttonsList.keySet();
		Iterator<Integer> it = keys.iterator();
		while (it.hasNext())
		{
			int it_button = it.next();
			((RelativeLayout) buttonsList.get(it_button)).setPressed(false);
		}

		if (buttonsList.containsKey(mode_id))
		{
			RelativeLayout pressed_button = (RelativeLayout) buttonsList.get(mode_id);
			pressed_button.setPressed(false);
		}
	}

	private void setCameraParameterValue(int iParameter, int sValue)
	{
		switch (iParameter)
		{
		case MODE_SCENE:
			mSceneMode = sValue;
			break;
		case MODE_WB:
			mWB = sValue;
			break;
		case MODE_FOCUS:
			mFocusMode = sValue;
			break;
		case MODE_FLASH:
			mFlashMode = sValue;
			break;
		case MODE_ISO:
			mISO = sValue;
			break;
		case MODE_MET:
			mMeteringMode = sValue;
			break;
		default:
			break;
		}
	}

	/************************************************************************************
	 * 
	 * Methods for adding Viewfinder plugin's controls and informational
	 * controls from other plugins
	 * 
	 ***********************************************************************************/
	private void addInfoControl(View info_control)
	{
		// Calculate appropriate size of added plugin's view
		android.widget.LinearLayout.LayoutParams viewLayoutParams = (android.widget.LinearLayout.LayoutParams) info_control
				.getLayoutParams();
		viewLayoutParams = this.getTunedLinearLayoutParams(info_control, viewLayoutParams, iInfoViewMaxWidth,
				iInfoViewMaxHeight);

		((LinearLayout) guiView.findViewById(R.id.infoLayout)).addView(info_control, viewLayoutParams);
	}

	// Public interface for all plugins
	// Automatically decide where to put view and correct view's size if
	// necessary
	@Override
	protected void addPluginViews(Map<View, Plugin.ViewfinderZone> views_map)
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
				else
					((RelativeLayout) guiView.findViewById(R.id.pluginsLayout)).addView(view, viewLayoutParams);
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e("Almalence GUI", "addPluginViews exception: " + e.getMessage());
			}
		}
	}

	@Override
	public void addViewQuick(View view, Plugin.ViewfinderZone desire_zone)
	{
		android.widget.RelativeLayout.LayoutParams viewLayoutParams = (android.widget.RelativeLayout.LayoutParams) view
				.getLayoutParams();
		viewLayoutParams = this.getTunedPluginLayoutParams(view, desire_zone, viewLayoutParams);

		if (viewLayoutParams == null) // No free space on plugin's layout
			return;

		view.setLayoutParams(viewLayoutParams);
		if (desire_zone == Plugin.ViewfinderZone.VIEWFINDER_ZONE_FULLSCREEN
				|| desire_zone == Plugin.ViewfinderZone.VIEWFINDER_ZONE_CENTER)
			((RelativeLayout) guiView.findViewById(R.id.fullscreenLayout)).addView(view, 0,
					(ViewGroup.LayoutParams) viewLayoutParams);
		else
			((RelativeLayout) guiView.findViewById(R.id.pluginsLayout)).addView(view, viewLayoutParams);
	}

	@Override
	protected void removePluginViews(Map<View, Plugin.ViewfinderZone> views_map)
	{
		Set<View> view_set = views_map.keySet();
		Iterator<View> it = view_set.iterator();
		while (it.hasNext())
		{
			View view = it.next();
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);

			((RelativeLayout) guiView.findViewById(R.id.pluginsLayout)).removeView(view);
		}
	}

	@Override
	public void removeViewQuick(View view)
	{
		if (view == null)
			return;
		if (view.getParent() != null)
			((ViewGroup) view.getParent()).removeView(view);

		((RelativeLayout) guiView.findViewById(R.id.pluginsLayout)).removeView(view);
	}

	/* Private section for adding plugin's views */

	// INFO VIEW SECTION
	@Override
	protected void addInfoView(View view, android.widget.LinearLayout.LayoutParams viewLayoutParams)
	{
		if (((LinearLayout) guiView.findViewById(R.id.infoLayout)).getChildCount() != 0)
			viewLayoutParams.topMargin = 4;

		((LinearLayout) guiView.findViewById(R.id.infoLayout)).addView(view, viewLayoutParams);
	}

	@Override
	protected void removeInfoView(View view)
	{
		if (view.getParent() != null)
			((ViewGroup) view.getParent()).removeView(view);
		((LinearLayout) guiView.findViewById(R.id.infoLayout)).removeView(view);
	}

	protected boolean isAnyViewOnViewfinder()
	{
		RelativeLayout pluginsLayout = (RelativeLayout) MainScreen.getInstance().findViewById(R.id.pluginsLayout);
		LinearLayout infoLayout = (LinearLayout) MainScreen.getInstance().findViewById(R.id.infoLayout);
		RelativeLayout fullScreenLayout = (RelativeLayout) MainScreen.getInstance().findViewById(R.id.fullscreenLayout);

		return pluginsLayout.getChildCount() > 0 || infoLayout.getChildCount() > 0
				|| fullScreenLayout.getChildCount() > 0;
	}

	// controls if info about new mode shown or not. to prevent from double info
	private void initModeList()
	{
		boolean initModeList = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).getBoolean(MainScreen.sInitModeListPref, false);
		if (activeMode != null && !initModeList)
		{
			return;
		}
		
		PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).edit().putBoolean(MainScreen.sInitModeListPref, false).commit();

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

				LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
				View mode = inflator.inflate(R.layout.gui_almalence_select_mode_grid_element, null, false);
				// set some mode icon
				((ImageView) mode.findViewById(R.id.modeImage)).setImageResource(MainScreen
						.getInstance()
						.getResources()
						.getIdentifier(CameraController.isUseSuperMode() ? tmp.iconHAL : tmp.icon, "drawable",
								MainScreen.getInstance().getPackageName()));

				int id = MainScreen
						.getInstance()
						.getResources()
						.getIdentifier(CameraController.isUseSuperMode() ? tmp.modeNameHAL : tmp.modeName, "string",
								MainScreen.getInstance().getPackageName());
				String modename = MainScreen.getAppResources().getString(id);

				((TextView) mode.findViewById(R.id.modeText)).setText(modename);
				if (mode_number == 0)
					mode.setOnTouchListener(new OnTouchListener()
					{
						@Override
						public boolean onTouch(View v, MotionEvent event)
						{
							if (event.getAction() == MotionEvent.ACTION_CANCEL)// &&
																				// isFirstMode)
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
				if (PluginManager.getInstance().getActiveModeID() == tmp.modeID)
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
		if (PluginManager.getInstance().getActiveModeID() == mode.modeID)
			return false;

		final Mode tmpActiveMode = mode;

		if (mode.modeID.equals("video"))
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
			if (prefs.getBoolean("videoStartStandardPref", false))
			{
				PluginManager.getInstance().onPause(true);
				Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
				MainScreen.getInstance().startActivity(intent);
				return true;
			}
		}

		// <!-- -+-
		if (!MainScreen.getInstance().checkLaunches(tmpActiveMode))
			return false;
		// -+- -->

		new CountDownTimer(100, 100)
		{
			public void onTick(long millisUntilFinished)
			{
				// Not used
			}

			public void onFinish()
			{
				PluginManager.getInstance().switchMode(tmpActiveMode);
			}
		}.start();

		// set modes icon inside mode selection icon
		Bitmap bm = null;
		Bitmap iconBase = BitmapFactory.decodeResource(MainScreen.getMainContext().getResources(),
				R.drawable.gui_almalence_select_mode);
		Bitmap iconOverlay = BitmapFactory.decodeResource(
				MainScreen.getMainContext().getResources(),
				MainScreen
						.getInstance()
						.getResources()
						.getIdentifier(CameraController.isUseSuperMode() ? mode.iconHAL : mode.icon, "drawable",
								MainScreen.getInstance().getPackageName()));
		iconOverlay = Bitmap.createScaledBitmap(iconOverlay, (int) (iconBase.getWidth() / 1.8),
				(int) (iconBase.getWidth() / 1.8), false);

		bm = mergeImage(iconBase, iconOverlay);
		bm = Bitmap.createScaledBitmap(bm,
				(int) (MainScreen.getMainContext().getResources().getDimension(R.dimen.mainButtonHeightSelect)),
				(int) (MainScreen.getMainContext().getResources().getDimension(R.dimen.mainButtonHeightSelect)), false);
		((RotateImageView) guiView.findViewById(R.id.buttonSelectMode)).setImageBitmap(bm);

		int rid = MainScreen.getAppResources().getIdentifier(tmpActiveMode.howtoText, "string",
				MainScreen.getInstance().getPackageName());
		String howto = "";
		if (rid != 0)
			howto = MainScreen.getAppResources().getString(rid);
		// show toast on mode changed
		showToast(
				v,
				Toast.LENGTH_SHORT,
				Gravity.CENTER,
				((TextView) v.findViewById(R.id.modeText)).getText() + " "
						+ MainScreen.getAppResources().getString(R.string.almalence_gui_selected)
						+ (tmpActiveMode.howtoText.isEmpty() ? "" : "\n") + howto, false, true);
		return false;
	}

	public void showToast(final View v, final int showLength, final int gravity, final String toastText,
			final boolean withBackground, final boolean startOffset)
	{
		MainScreen.getInstance().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				final RelativeLayout modeLayout = (RelativeLayout) guiView.findViewById(R.id.changeModeToast);

				DisplayMetrics metrics = new DisplayMetrics();
				MainScreen.getInstance().getWindowManager().getDefaultDisplay().getMetrics(metrics);
				int screen_height = metrics.heightPixels;
				int screen_width = metrics.widthPixels;

				RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) modeLayout.getLayoutParams();
				int[] rules = lp.getRules();
				if (gravity == Gravity.CENTER || (mDeviceOrientation != 0 && mDeviceOrientation != 360))
					lp.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
				else
				{
					rules[RelativeLayout.CENTER_IN_PARENT] = 0;
					lp.addRule(RelativeLayout.CENTER_HORIZONTAL, 1);

					View shutter = guiView.findViewById(R.id.buttonShutter);
					int shutter_height = shutter.getHeight() + shutter.getPaddingBottom();
					lp.setMargins(
							0,
							(int) (screen_height
									- MainScreen.getAppResources().getDimension(R.dimen.paramsLayoutHeight) - shutter_height),
							0, shutter_height);
				}

				if (withBackground)
					modeLayout.setBackgroundDrawable(MainScreen.getAppResources().getDrawable(
							R.drawable.almalence_gui_toast_background));
				else
					modeLayout.setBackgroundDrawable(null);
				RotateImageView imgView = (RotateImageView) modeLayout.findViewById(R.id.selectModeIcon);
				TextView text = (TextView) modeLayout.findViewById(R.id.selectModeText);

				if (v != null)
				{
					RelativeLayout.LayoutParams pm = (RelativeLayout.LayoutParams) imgView.getLayoutParams();
					pm.width = (int) MainScreen.getAppResources().getDimension(R.dimen.mainButtonHeight);
					pm.height = (int) MainScreen.getAppResources().getDimension(R.dimen.mainButtonHeight);
					imgView.setImageDrawable(((ImageView) v.findViewById(R.id.modeImage)).getDrawable()
							.getConstantState().newDrawable());
				} else
				{
					RelativeLayout.LayoutParams pm = (RelativeLayout.LayoutParams) imgView.getLayoutParams();
					pm.width = 0;
					pm.height = 0;
				}
				text.setText("  " + toastText);
				text.setTextSize(16);
				text.setTextColor(Color.WHITE);

				text.setMaxWidth((int) Math.round(screen_width * 0.7));

				Animation visible_alpha = new AlphaAnimation(0, 1);
				visible_alpha.setStartOffset(startOffset ? 2000 : 0);
				visible_alpha.setDuration(1000);
				visible_alpha.setRepeatCount(0);

				final Animation invisible_alpha = new AlphaAnimation(1, 0);
				invisible_alpha.setStartOffset(showLength == Toast.LENGTH_SHORT ? 1000 : 3000);
				invisible_alpha.setDuration(1000);
				invisible_alpha.setRepeatCount(0);

				visible_alpha.setAnimationListener(new AnimationListener()
				{
					@Override
					public void onAnimationEnd(Animation animation)
					{
						modeLayout.startAnimation(invisible_alpha);
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

				invisible_alpha.setAnimationListener(new AnimationListener()
				{
					@Override
					public void onAnimationEnd(Animation animation)
					{
						modeLayout.setVisibility(View.GONE);
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

				modeLayout.setRotation(-mDeviceOrientation);
				modeLayout.setVisibility(View.VISIBLE);
				modeLayout.bringToFront();
				modeLayout.startAnimation(visible_alpha);
			}
		});
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

	// Supplementary methods to find suitable size for plugin's view
	// For LINEARLAYOUT
	private android.widget.RelativeLayout.LayoutParams getTunedRelativeLayoutParams(View view,
			android.widget.RelativeLayout.LayoutParams currParams, int goodWidth, int goodHeight)
	{
		int viewHeight, viewWidth;

		if (currParams != null)
		{
			viewHeight = currParams.height;
			viewWidth = currParams.width;

			if ((viewHeight > goodHeight || viewHeight <= 0) && goodHeight > 0)
				viewHeight = goodHeight;

			if ((viewWidth > goodWidth || viewWidth <= 0) && goodWidth > 0)
				viewWidth = goodWidth;

			currParams.width = viewWidth;
			currParams.height = viewHeight;

			view.setLayoutParams(currParams);
		} else
		{
			currParams = new android.widget.RelativeLayout.LayoutParams(goodWidth, goodHeight);
			view.setLayoutParams(currParams);
		}

		return currParams;
	}

	// For RELATIVELAYOUT
	private android.widget.LinearLayout.LayoutParams getTunedLinearLayoutParams(View view,
			android.widget.LinearLayout.LayoutParams currParams, int goodWidth, int goodHeight)
	{
		if (currParams != null)
		{
			int viewHeight = currParams.height;
			int viewWidth = currParams.width;

			if ((viewHeight > goodHeight || viewHeight <= 0) && goodHeight > 0)
				viewHeight = goodHeight;

			if ((viewWidth > goodWidth || viewWidth <= 0) && goodWidth > 0)
				viewWidth = goodWidth;

			currParams.width = viewWidth;
			currParams.height = viewHeight;

			view.setLayoutParams(currParams);
		} else
		{
			currParams = new android.widget.LinearLayout.LayoutParams(goodWidth, goodHeight);
			view.setLayoutParams(currParams);
		}

		return currParams;
	}

	/************************************************************************************************
	 * >>>>>>>>>
	 * 
	 * DEFINITION OF PLACE ON LAYOUT FOR PLUGIN'S VIEWS
	 * 
	 * >>>>>>>>> BEGIN
	 ***********************************************************************************************/

	protected Plugin.ViewfinderZone[]	zones	= { Plugin.ViewfinderZone.VIEWFINDER_ZONE_TOP_LEFT,
			Plugin.ViewfinderZone.VIEWFINDER_ZONE_TOP_RIGHT, Plugin.ViewfinderZone.VIEWFINDER_ZONE_CENTER_RIGHT,
			Plugin.ViewfinderZone.VIEWFINDER_ZONE_BOTTOM_RIGHT, Plugin.ViewfinderZone.VIEWFINDER_ZONE_BOTTOM_LEFT,
			Plugin.ViewfinderZone.VIEWFINDER_ZONE_CENTER_LEFT };

	protected android.widget.RelativeLayout.LayoutParams getTunedPluginLayoutParams(View view,
			Plugin.ViewfinderZone desire_zone, android.widget.RelativeLayout.LayoutParams currParams)
	{
		int left = 0, right = 0, top = 0, bottom = 0;

		RelativeLayout pluginLayout = (RelativeLayout) MainScreen.getInstance().findViewById(R.id.pluginsLayout);

		if (currParams == null)
			currParams = new android.widget.RelativeLayout.LayoutParams(getMinPluginViewWidth(),
					getMinPluginViewHeight());

		switch (desire_zone)
		{
		case VIEWFINDER_ZONE_TOP_LEFT:
			{
				left = 0;
				right = currParams.width;
				top = 0;
				bottom = currParams.height;

				currParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

			}
			break;
		case VIEWFINDER_ZONE_TOP_RIGHT:
			{
				left = pluginLayout.getWidth() - currParams.width;
				right = pluginLayout.getWidth();
				top = 0;
				bottom = currParams.height;

				currParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			}
			break;
		case VIEWFINDER_ZONE_CENTER_LEFT:
			{
				left = 0;
				right = currParams.width;
				top = pluginLayout.getHeight() / 2 - currParams.height / 2;
				bottom = pluginLayout.getHeight() / 2 + currParams.height / 2;

				currParams.addRule(RelativeLayout.CENTER_VERTICAL);
				currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			}
			break;
		case VIEWFINDER_ZONE_CENTER_RIGHT:
			{
				left = pluginLayout.getWidth() - currParams.width;
				right = pluginLayout.getWidth();
				top = pluginLayout.getHeight() / 2 - currParams.height / 2;
				bottom = pluginLayout.getHeight() / 2 + currParams.height / 2;

				currParams.addRule(RelativeLayout.CENTER_VERTICAL);
				currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			}
			break;
		case VIEWFINDER_ZONE_BOTTOM_LEFT:
			{
				left = 0;
				right = currParams.width;
				top = pluginLayout.getHeight() - currParams.height;
				bottom = pluginLayout.getHeight();

				currParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			}
			break;
		case VIEWFINDER_ZONE_BOTTOM_RIGHT:
			{
				left = pluginLayout.getWidth() - currParams.width;
				right = pluginLayout.getWidth();
				top = pluginLayout.getHeight() - currParams.height;
				bottom = pluginLayout.getHeight();

				currParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			}
			break;
		case VIEWFINDER_ZONE_FULLSCREEN:
			{
				Log.e("GUI", "VIEWFINDER_ZONE_FULLSCREEN");
				currParams.width = MainScreen.getInstance().getPreviewSize() != null ? MainScreen.getInstance()
						.getPreviewSize().getWidth() : 0;
				currParams.height = MainScreen.getInstance().getPreviewSize() != null ? MainScreen.getInstance()
						.getPreviewSize().getHeight() : 0;
				currParams.addRule(RelativeLayout.CENTER_IN_PARENT);
				return currParams;
			}
		case VIEWFINDER_ZONE_CENTER:
			{
				if (currParams.width > iCenterViewMaxWidth)
					currParams.width = iCenterViewMaxWidth;

				if (currParams.height > iCenterViewMaxHeight)
					currParams.height = iCenterViewMaxHeight;

				currParams.addRule(RelativeLayout.CENTER_IN_PARENT);
				return currParams;
			}
		default:
			break;
		}

		return findFreeSpaceOnLayout(new Rect(left, top, right, bottom), currParams, desire_zone);
	}

	protected android.widget.RelativeLayout.LayoutParams findFreeSpaceOnLayout(Rect currRect,
			android.widget.RelativeLayout.LayoutParams currParams, Plugin.ViewfinderZone currZone)
	{
		boolean isFree = true;
		Rect childRect;

		View pluginLayout = MainScreen.getInstance().findViewById(R.id.pluginsLayout);

		// Looking in all zones at clockwise direction for free place
		for (int j = Plugin.ViewfinderZone.getInt(currZone), counter = 0; j < zones.length; j++, counter++)
		{
			isFree = true;
			// Check intersections with already added views
			for (int i = 0; i < ((ViewGroup) pluginLayout).getChildCount(); ++i)
			{
				View nextChild = ((ViewGroup) pluginLayout).getChildAt(i);

				currRect = getPluginViewRectInZone(currParams, zones[j]);
				childRect = getPluginViewRect(nextChild);

				if (currRect.intersect(childRect))
				{
					isFree = false;
					break;
				}
			}

			if (isFree) // Free zone has found
			{
				if (currZone == zones[j]) // Current zone is free
					return currParams;
				else
				{
					int[] rules = currParams.getRules();
					for (int i = 0; i < rules.length; i++)
						currParams.addRule(i, 0);

					switch (zones[j])
					{
					case VIEWFINDER_ZONE_TOP_LEFT:
						{
							currParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
							currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
						}
						break;
					case VIEWFINDER_ZONE_TOP_RIGHT:
						{
							currParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
							currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
						}
						break;
					case VIEWFINDER_ZONE_CENTER_LEFT:
						{
							currParams.addRule(RelativeLayout.CENTER_VERTICAL);
							currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
						}
						break;
					case VIEWFINDER_ZONE_CENTER_RIGHT:
						{
							currParams.addRule(RelativeLayout.CENTER_VERTICAL);
							currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
						}
						break;
					case VIEWFINDER_ZONE_BOTTOM_LEFT:
						{
							currParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
							currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
						}
						break;
					case VIEWFINDER_ZONE_BOTTOM_RIGHT:
						{
							currParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
							currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
						}
						break;
					default:
						break;
					}

					return currParams;
				}
			} else
			{
				if (counter == zones.length - 1) // Already looked in all zones
													// and they are all full
					return null;

				if (j == zones.length - 1) // If we started not from a first
											// zone (top-left)
					j = -1;
			}
		}

		return null;
	}

	protected Rect getPluginViewRectInZone(RelativeLayout.LayoutParams currParams, Plugin.ViewfinderZone zone)
	{
		int left = -1, right = -1, top = -1, bottom = -1;

		RelativeLayout pluginLayout = (RelativeLayout) MainScreen.getInstance().findViewById(R.id.pluginsLayout);

		int viewWidth = currParams.width;
		int viewHeight = currParams.height;

		int layoutWidth = pluginLayout.getWidth();
		int layoutHeight = pluginLayout.getHeight();

		switch (zone)
		{
		case VIEWFINDER_ZONE_TOP_LEFT:
			{
				left = 0;
				right = viewWidth;
				top = 0;
				bottom = viewHeight;
			}
			break;
		case VIEWFINDER_ZONE_TOP_RIGHT:
			{
				left = layoutWidth - viewWidth;
				right = layoutWidth;
				top = 0;
				bottom = viewHeight;
			}
			break;
		case VIEWFINDER_ZONE_CENTER_LEFT:
			{
				left = 0;
				right = viewWidth;
				top = layoutHeight / 2 - viewHeight / 2;
				bottom = layoutHeight / 2 + viewHeight / 2;
			}
			break;
		case VIEWFINDER_ZONE_CENTER_RIGHT:
			{
				left = layoutWidth - viewWidth;
				right = layoutWidth;
				top = layoutHeight / 2 - viewHeight / 2;
				bottom = layoutHeight / 2 + viewHeight / 2;
			}
			break;
		case VIEWFINDER_ZONE_BOTTOM_LEFT:
			{
				left = 0;
				right = viewWidth;
				top = layoutHeight - viewHeight;
				bottom = layoutHeight;
			}
			break;
		case VIEWFINDER_ZONE_BOTTOM_RIGHT:
			{
				left = layoutWidth - viewWidth;
				right = layoutWidth;
				top = layoutHeight - viewHeight;
				bottom = layoutHeight;
			}
			break;
		default:
			break;
		}

		return new Rect(left, top, right, bottom);
	}

	protected Rect getPluginViewRect(View view)
	{
		int left = -1, right = -1, top = -1, bottom = -1;

		RelativeLayout pluginLayout = (RelativeLayout) MainScreen.getInstance().findViewById(R.id.pluginsLayout);
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) view.getLayoutParams();
		int[] rules = lp.getRules();

		int viewWidth = lp.width;
		int viewHeight = lp.height;

		int layoutWidth = pluginLayout.getWidth();
		int layoutHeight = pluginLayout.getHeight();

		// Get X coordinates
		if (rules[RelativeLayout.ALIGN_PARENT_LEFT] != 0)
		{
			left = 0;
			right = viewWidth;
		} else if (rules[RelativeLayout.ALIGN_PARENT_RIGHT] != 0)
		{
			left = layoutWidth - viewWidth;
			right = layoutWidth;
		}

		// Get Y coordinates
		if (rules[RelativeLayout.ALIGN_PARENT_TOP] != 0)
		{
			top = 0;
			bottom = viewHeight;
		} else if (rules[RelativeLayout.ALIGN_PARENT_BOTTOM] != 0)
		{
			top = layoutHeight - viewHeight;
			bottom = layoutHeight;
		} else if (rules[RelativeLayout.CENTER_VERTICAL] != 0)
		{
			top = layoutHeight / 2 - viewHeight / 2;
			bottom = layoutHeight / 2 + viewHeight / 2;
		}

		return new Rect(left, top, right, bottom);
	}

	// Supplementary methods for getting appropriate sizes for plugin's views
	@Override
	public int getMaxPluginViewHeight()
	{
		return ((RelativeLayout) guiView.findViewById(R.id.pluginsLayout)).getHeight() / 3;
	}

	@Override
	public int getMaxPluginViewWidth()
	{
		return ((RelativeLayout) guiView.findViewById(R.id.pluginsLayout)).getWidth() / 2;
	}

	@Override
	public int getMinPluginViewHeight()
	{
		return ((RelativeLayout) guiView.findViewById(R.id.pluginsLayout)).getHeight() / 12;
	}

	@Override
	public int getMinPluginViewWidth()
	{
		return ((RelativeLayout) guiView.findViewById(R.id.pluginsLayout)).getWidth() / 4;
	}

	/************************************************************************************************
	 * <<<<<<<<<<
	 * 
	 * DEFINITION OF PLACE ON LAYOUT FOR PLUGIN'S VIEWS
	 * 
	 * <<<<<<<<<< END
	 ***********************************************************************************************/

	private static float	X					= 0;

	private static float	Xprev				= 0;

	private static float	Xoffset				= 0;

	private static float	XtoLeftInvisible	= 0;
	private static float	XtoLeftVisible		= 0;

	private static float	XtoRightInvisible	= 0;
	private static float	XtoRightVisible		= 0;

	private MotionEvent		prevEvent;
	private MotionEvent		downEvent;
	private boolean			scrolling			= false;

	@Override
	public boolean onTouch(View view, MotionEvent event)
	{
		// hide hint screen
		if (guiView.findViewById(R.id.hintLayout).getVisibility() == View.VISIBLE)
		{
			if (event.getAction() == MotionEvent.ACTION_UP)
				guiView.findViewById(R.id.hintLayout).setVisibility(View.INVISIBLE);
			return true;
		}

		if (guiView.findViewById(R.id.mode_help).getVisibility() == View.VISIBLE)
		{
			guiView.findViewById(R.id.mode_help).setVisibility(View.INVISIBLE);
			return true;
		}

		if (view == (LinearLayout) guiView.findViewById(R.id.evLayout)
				|| (lockControls && !PluginManager.getInstance().getActiveModeID().equals("video")))
			return true;

		// to possibly slide-out top panel
		if (view == MainScreen.getPreviewSurfaceView()
				|| view == (View) MainScreen.getInstance().findViewById(R.id.mainLayout1))
			((Panel) guiView.findViewById(R.id.topPanel)).touchListener.onTouch(view, event);

		else if (view.getParent() == (View) MainScreen.getInstance().findViewById(R.id.paramsLayout)
				&& !quickControlsChangeVisible)
		{

			if (event.getAction() == MotionEvent.ACTION_DOWN)
			{
				downEvent = MotionEvent.obtain(event);
				prevEvent = MotionEvent.obtain(event);
				scrolling = false;

				topMenuButtonPressed(findTopMenuButtonIndex(view));

				return false;
			} else if (event.getAction() == MotionEvent.ACTION_UP)
			{
				topMenuButtonPressed(-1);
				if (scrolling)
					((Panel) guiView.findViewById(R.id.topPanel)).touchListener.onTouch(view, event);
				scrolling = false;
				if (prevEvent == null || downEvent == null)
					return false;
				if (prevEvent.getAction() == MotionEvent.ACTION_DOWN)
					return false;
				if (prevEvent.getAction() == MotionEvent.ACTION_MOVE)
				{
					if ((event.getY() - downEvent.getY()) < 50)
						return false;
				}
			} else if (event.getAction() == MotionEvent.ACTION_MOVE && !scrolling)
			{
				if (downEvent == null)
					return false;
				if ((event.getY() - downEvent.getY()) < 50)
					return false;
				else
				{
					scrolling = true;
					((Panel) guiView.findViewById(R.id.topPanel)).touchListener.onTouch(view, downEvent);
				}
			}
			((Panel) guiView.findViewById(R.id.topPanel)).touchListener.onTouch(view, event);
		}

		// to allow quickControl's to process onClick, onLongClick
		if (view.getParent() == (View) MainScreen.getInstance().findViewById(R.id.paramsLayout))
		{
			return false;
		}

		boolean isMenuOpened = false;
		if (quickControlsChangeVisible || modeSelectorVisible || settingsControlsVisible || isSecondaryMenusVisible())
			isMenuOpened = true;

		if (quickControlsChangeVisible
				&& view.getParent() != (View) MainScreen.getInstance().findViewById(R.id.paramsLayout))
			closeQuickControlsSettings();

		if (modeSelectorVisible)
			hideModeList();

		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		if (settingsControlsVisible)
			return true;
		else if (!isMenuOpened)
			// call onTouch of active vf and capture plugins
			PluginManager.getInstance().onTouch(view, event);

		RelativeLayout pluginLayout = (RelativeLayout) guiView.findViewById(R.id.pluginsLayout);
		RelativeLayout fullscreenLayout = (RelativeLayout) guiView.findViewById(R.id.fullscreenLayout);
		LinearLayout paramsLayout = (LinearLayout) guiView.findViewById(R.id.paramsLayout);
		LinearLayout infoLayout = (LinearLayout) guiView.findViewById(R.id.infoLayout);
		// OnTouch listener to show info and sliding grids
		switch (event.getAction())
		{
		case MotionEvent.ACTION_DOWN:
			{
				X = event.getX();
				Xoffset = X;
				Xprev = X;

				pluginLayout.clearAnimation();
				fullscreenLayout.clearAnimation();
				paramsLayout.clearAnimation();
				infoLayout.clearAnimation();

				topMenuButtonPressed(findTopMenuButtonIndex(view));

				return true;
			}
		case MotionEvent.ACTION_UP:
			{
				float difX = event.getX();
				if ((X > difX) && (X - difX > 100))
				{
					sliderLeftEvent();
					return true;
				} else if (X < difX && (difX - X > 100))
				{
					sliderRightEvent();
					return true;
				}

				pluginLayout.clearAnimation();
				fullscreenLayout.clearAnimation();
				paramsLayout.clearAnimation();
				infoLayout.clearAnimation();

				topMenuButtonPressed(-1);

				break;
			}
		case MotionEvent.ACTION_MOVE:
			{
				int pluginzoneWidth = guiView.findViewById(R.id.pluginsLayout).getWidth();
				int infozoneWidth = guiView.findViewById(R.id.infoLayout).getWidth();
				int screenWidth = pluginzoneWidth + infozoneWidth;

				float difX = event.getX();

				Animation in_animation;
				Animation out_animation;
				Animation reverseout_animation;
				boolean toLeft;
				if (difX > Xprev)
				{
					out_animation = new TranslateAnimation(Xprev - Xoffset, difX - Xoffset, 0, 0);
					out_animation.setDuration(10);
					out_animation.setInterpolator(new LinearInterpolator());
					out_animation.setFillAfter(true);

					in_animation = new TranslateAnimation(Xprev - Xoffset - screenWidth, difX - Xoffset - screenWidth,
							0, 0);
					in_animation.setDuration(10);
					in_animation.setInterpolator(new LinearInterpolator());
					in_animation.setFillAfter(true);

					reverseout_animation = new TranslateAnimation(difX + (screenWidth - Xoffset), Xprev
							+ (screenWidth - Xoffset), 0, 0);
					reverseout_animation.setDuration(10);
					reverseout_animation.setInterpolator(new LinearInterpolator());
					reverseout_animation.setFillAfter(true);

					toLeft = false;

					XtoRightInvisible = difX - Xoffset;
					XtoRightVisible = difX - Xoffset - screenWidth;
				} else
				{
					out_animation = new TranslateAnimation(difX - Xoffset, Xprev - Xoffset, 0, 0);
					out_animation.setDuration(10);
					out_animation.setInterpolator(new LinearInterpolator());
					out_animation.setFillAfter(true);

					in_animation = new TranslateAnimation(screenWidth + (Xprev - Xoffset), screenWidth
							+ (difX - Xoffset), 0, 0);
					in_animation.setDuration(10);
					in_animation.setInterpolator(new LinearInterpolator());
					in_animation.setFillAfter(true);

					reverseout_animation = new TranslateAnimation(Xprev - Xoffset - screenWidth, difX - Xoffset
							- screenWidth, 0, 0);
					reverseout_animation.setDuration(10);
					reverseout_animation.setInterpolator(new LinearInterpolator());
					reverseout_animation.setFillAfter(true);

					toLeft = true;

					XtoLeftInvisible = Xprev - Xoffset;
					XtoLeftVisible = screenWidth + (difX - Xoffset);
				}

				switch (infoSet)
				{
				case INFO_ALL:
					{
						pluginLayout.startAnimation(out_animation);
						fullscreenLayout.startAnimation(out_animation);
						infoLayout.startAnimation(out_animation);
						if ((difX < X) || !isAnyViewOnViewfinder())
							paramsLayout.startAnimation(out_animation);
					}
					break;
				case INFO_NO:
					{
						if ((toLeft && difX < X) || (!toLeft && difX > X))
							fullscreenLayout.startAnimation(in_animation);
						else
							paramsLayout.startAnimation(reverseout_animation);
						if (!toLeft && isAnyViewOnViewfinder())
						{
							pluginLayout.startAnimation(in_animation);
							fullscreenLayout.startAnimation(in_animation);
							infoLayout.startAnimation(in_animation);
						} else if (toLeft && difX > X && isAnyViewOnViewfinder())
						{
							pluginLayout.startAnimation(reverseout_animation);
							paramsLayout.startAnimation(reverseout_animation);
							infoLayout.startAnimation(reverseout_animation);
						}
					}
					break;
				case INFO_GRID:
					{
						if (difX > X)// to INFO_NO
							fullscreenLayout.startAnimation(out_animation);
						else
						// to INFO_PARAMS
						{
							fullscreenLayout.startAnimation(out_animation);
							paramsLayout.startAnimation(in_animation);
						}
					}
					break;
				case INFO_PARAMS:
					{
						fullscreenLayout.startAnimation(in_animation);
						if (difX > X)
							paramsLayout.startAnimation(out_animation);
						if (toLeft)
						{
							pluginLayout.startAnimation(in_animation);
							infoLayout.startAnimation(in_animation);
						} else if (difX < X)
						{
							pluginLayout.startAnimation(reverseout_animation);
							infoLayout.startAnimation(reverseout_animation);
						}
					}
					break;
				default:
					break;
				}

				Xprev = Math.round(difX);

			}
			break;
		default:
			break;
		}
		return false;
	}

	private int getRelativeLeft(View myView)
	{
		if (myView.getParent() == myView.getRootView())
			return myView.getLeft();
		else
			return myView.getLeft() + getRelativeLeft((View) myView.getParent());
	}

	private int getRelativeTop(View myView)
	{
		if (myView.getParent() == myView.getRootView())
			return myView.getTop();
		else
			return myView.getTop() + getRelativeTop((View) myView.getParent());
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
	{

		int iEv = progress - CameraController.getMaxExposureCompensation();
		CameraController.setCameraExposureCompensation(iEv);

		preferences.edit().putInt(MainScreen.sEvPref, iEv).commit();

		mEV = iEv;

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_EV_CHANGED);
	}

	@Override
	public void onVolumeBtnExpo(int keyCode)
	{
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
			expoMinus();
		else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
			expoPlus();
	}

	private void expoMinus()
	{
		SeekBar evBar = (SeekBar) guiView.findViewById(R.id.evSeekBar);
		if (evBar != null)
		{
			int minValue = CameraController.getMinExposureCompensation();
			int step = 1;
			int currProgress = evBar.getProgress();
			int iEv = currProgress - step;
			if (iEv < 0)
				iEv = 0;

			CameraController.setCameraExposureCompensation(iEv + minValue);

			preferences
					.edit()
					.putInt(MainScreen.sEvPref,
							Math.round((iEv + minValue) * CameraController.getExposureCompensationStep())).commit();

			evBar.setProgress(iEv);
		}
	}

	private void expoPlus()
	{
		SeekBar evBar = (SeekBar) guiView.findViewById(R.id.evSeekBar);
		if (evBar != null)
		{
			int minValue = CameraController.getMinExposureCompensation();
			int maxValue = CameraController.getMaxExposureCompensation();

			int step = 1;

			int currProgress = evBar.getProgress();
			int iEv = currProgress + step;
			if (iEv > maxValue - minValue)
				iEv = maxValue - minValue;

			CameraController.setCameraExposureCompensation(iEv + minValue);

			preferences
					.edit()
					.putInt(MainScreen.sEvPref,
							Math.round((iEv + minValue) * CameraController.getExposureCompensationStep())).commit();

			evBar.setProgress(iEv);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar)
	{
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar)
	{
	}

	// Slider events handler

	private void sliderLeftEvent()
	{
		infoSlide(true, XtoLeftVisible, XtoLeftInvisible);
	}

	private void sliderRightEvent()
	{
		infoSlide(false, XtoRightVisible, XtoRightInvisible);
	}

	// shutter icons setter
	public void setShutterIcon(ShutterButton id)
	{
		RotateImageView mainButton = (RotateImageView) guiView.findViewById(R.id.buttonShutter);
		// RotateImageView additionalButton = (RotateImageView)
		// guiView.findViewById(R.id.buttonShutterAdditional);
		RotateImageView buttonSelectMode = (RotateImageView) guiView.findViewById(R.id.buttonSelectMode);
		LinearLayout buttonShutterContainer = (LinearLayout) guiView.findViewById(R.id.buttonShutterContainer);

		// additionalButton.setVisibility(View.VISIBLE);
		// buttonSelectMode.setVisibility(View.GONE);

		if (id == ShutterButton.TIMELAPSE_ACTIVE)
		{
			mainButton.setImageResource(R.drawable.gui_almalence_shutter_timelapse);
		}

		// 1 button
		if (id == ShutterButton.DEFAULT || id == ShutterButton.RECORDER_START || id == ShutterButton.RECORDER_STOP
				|| id == ShutterButton.RECORDER_RECORDING)
		{
			// buttonShutterContainer.setOrientation(LinearLayout.VERTICAL);
			// buttonShutterContainer.setPadding(0, 0, 0, 0);

			// additionalButton.setVisibility(View.GONE);
			// buttonSelectMode.setVisibility(View.VISIBLE);
			if (id == ShutterButton.DEFAULT)
			{
				mainButton.setImageResource(R.drawable.button_shutter);
			} else if (id == ShutterButton.RECORDER_START)
			{
				mainButton.setImageResource(R.drawable.gui_almalence_shutter_video_off);
			}
			// else if (id == ShutterButton.RECORDER_STOP)
			// {
			// mainButton.setImageResource(R.drawable.gui_almalence_shutter_video_stop);
			// } else if (id == ShutterButton.RECORDER_RECORDING)
			// {
			// mainButton.setImageResource(R.drawable.gui_almalence_shutter_video_stop_red);
			// }

			// int dp = (int)
			// MainScreen.getAppResources().getDimension(R.dimen.shutterHeight);
			// mainButton.getLayoutParams().width = dp;
			// mainButton.getLayoutParams().height = dp;
		}
		// // video with pause (2 butons)
		// else
		// {
		// // buttonShutterContainer.setOrientation(LinearLayout.HORIZONTAL);
		// // buttonShutterContainer.setPadding(0, Util.dpToPixel(15), 0, 0);
		//
		// // int dp = (int)
		// //
		// MainScreen.getAppResources().getDimension(R.dimen.videoShutterHeight);
		// // mainButton.getLayoutParams().width = dp;
		// // mainButton.getLayoutParams().height = dp;
		//
		// if (id == ShutterButton.RECORDER_START_WITH_PAUSE)
		// {
		// mainButton.setImageResource(R.drawable.gui_almalence_shutter_video_off);
		// additionalButton.setImageResource(R.drawable.gui_almalence_shutter_video_pause);
		// } else if (id == ShutterButton.RECORDER_STOP_WITH_PAUSE)
		// {
		// mainButton.setImageResource(R.drawable.gui_almalence_shutter_video_stop);
		// additionalButton.setImageResource(R.drawable.gui_almalence_shutter_video_pause);
		// } else if (id == ShutterButton.RECORDER_PAUSED)
		// {
		// additionalButton.setImageResource(R.drawable.gui_almalence_shutter_video_pause_red);
		// mainButton.setImageResource(R.drawable.gui_almalence_shutter_video_stop);
		// } else if (id == ShutterButton.RECORDER_RECORDING_WITH_PAUSE)
		// {
		// mainButton.setImageResource(R.drawable.gui_almalence_shutter_video_stop_red);
		// }
		// }
	}

	public boolean onKeyDown(boolean isFromMain, int keyCode, KeyEvent event)
	{
		// hide hint screen
		if (guiView.findViewById(R.id.hintLayout).getVisibility() == View.VISIBLE)
			guiView.findViewById(R.id.hintLayout).setVisibility(View.INVISIBLE);

		if (guiView.findViewById(R.id.mode_help).getVisibility() == View.VISIBLE)
		{
			guiView.findViewById(R.id.mode_help).setVisibility(View.INVISIBLE);
			return true;
		}

		int res = 0;
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			if (quickControlsChangeVisible)
			{
				closeQuickControlsSettings();
				res++;
				guiView.findViewById(R.id.topPanel).setVisibility(View.VISIBLE);
			} else if (settingsControlsVisible)
			{
				((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false, true);
				res++;
			} else if (modeSelectorVisible)
			{
				hideModeList();
				res++;
			} else if (quickControlsVisible)
			{
				unselectPrimaryTopMenuButtons(-1);
				hideSecondaryMenus();
				res++;
				guiView.findViewById(R.id.topPanel).setVisibility(View.VISIBLE);
				quickControlsVisible = false;
			}

			// <!-- -+-
			if (((RelativeLayout) guiView.findViewById(R.id.viewPagerLayoutMain)).getVisibility() == View.VISIBLE)
			{
				hideStore();
				res++;
			}
			// -+- -->
		}

		if (keyCode == KeyEvent.KEYCODE_CAMERA /*
												 * || keyCode ==
												 * KeyEvent.KEYCODE_DPAD_CENTER
												 */)
		{
			if (settingsControlsVisible || quickControlsChangeVisible || modeSelectorVisible)
			{
				if (quickControlsChangeVisible)
					closeQuickControlsSettings();
				if (settingsControlsVisible)
				{
					((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false, true);
					return false;
				}
				if (modeSelectorVisible)
				{
					hideModeList();
					return false;
				}
			}
			shutterButtonPressed();
			return true;
		}

		// check if back button pressed and processing is in progress
		if (res == 0)
			if (keyCode == KeyEvent.KEYCODE_BACK)
			{
				if (PluginManager.getInstance().getProcessingCounter() != 0)
				{
					// splash screen about processing
					AlertDialog.Builder builder = new AlertDialog.Builder(MainScreen.getInstance())
							.setTitle("Processing...")
							.setMessage(MainScreen.getAppResources().getString(R.string.processing_not_finished))
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

	public void openGallery(boolean isOpenExternal)
	{
		if (mThumbnail == null)
			return;

		Uri uri = this.mThumbnail.getUri();

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_STOP_CAPTURE);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		boolean isAllowedExternal = prefs.getBoolean(
				MainScreen.getAppResources().getString(R.string.Preference_allowExternalGalleries), false);
		if (isAllowedExternal || isOpenExternal)
		{
			openExternalGallery(uri);
		} else
		{
			// if installed - run ABC Editor
			if (AppEditorNotifier.isABCEditorInstalled(MainScreen.getInstance()))
			{
				MainScreen.getInstance().startActivity(new Intent("com.almalence.opencameditor.action.REVIEW", uri));// com.almalence.opencameditor
			}
			// if not installed - show that we have editor and let user install
			// it of run standard dialog
			else
			{
				// if not - show default gallery
				if (!AppEditorNotifier.showEditorNotifierDialogIfNeeded(MainScreen.getInstance()))
					openExternalGallery(uri);
			}
		}
	}

	private void openExternalGallery(Uri uri)
	{
		try
		{
			MainScreen.getInstance().startActivity(new Intent(Intent.ACTION_VIEW, uri));
		} catch (ActivityNotFoundException ex)
		{
			try
			{
				MainScreen.getInstance().startActivity(new Intent(Intent.ACTION_VIEW, uri));
			} catch (ActivityNotFoundException e)
			{
				Log.e("AlmalenceGUI", "review image fail. uri=" + uri, e);
			}
		}
	}

	public static boolean isUriValid(Uri uri, ContentResolver resolver)
	{
		if (uri == null)
			return false;

		try
		{
			ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
			if (pfd == null)
			{
				Log.e("AlmalenceGUI", "Fail to open URI. URI=" + uri);
				return false;
			}
			pfd.close();
		} catch (IOException ex)
		{
			return false;
		}

		return true;
	}

	@Override
	public void onCaptureFinished()
	{
		// Not used
	}

	// called to set any indication when export plugin work finished.
	@Override
	public void onExportFinished()
	{
		// stop animation
		if (processingAnim != null)
		{
			processingAnim.clearAnimation();
			processingAnim.setVisibility(View.GONE);
		}
		RelativeLayout rl = (RelativeLayout) guiView.findViewById(R.id.blockingLayout);
		if (rl.getVisibility() == View.VISIBLE)
		{
			rl.setVisibility(View.GONE);
		}

		updateThumbnailButton();
		thumbnailView.invalidate();

		if (0 != PluginManager.getInstance().getProcessingCounter())
		{
			new CountDownTimer(10, 10)
			{
				public void onTick(long millisUntilFinished)
				{
					// Not used
				}

				public void onFinish()
				{
					startProcessingAnimation();
				}
			}.start();
		}
	}

	@Override
	public void onPostProcessingStarted()
	{
		guiView.findViewById(R.id.buttonGallery).setEnabled(false);
		guiView.findViewById(R.id.buttonShutter).setEnabled(false);
		guiView.findViewById(R.id.buttonSelectMode).setEnabled(false);
		guiView.findViewById(R.id.postprocessingLayout).setVisibility(View.VISIBLE);
		guiView.findViewById(R.id.postprocessingLayout).bringToFront();
		List<Plugin> processingPlugins = PluginManager.getInstance().getActivePlugins(PluginType.Processing);
		if (!processingPlugins.isEmpty())
		{
			View postProcessingView = processingPlugins.get(0).getPostProcessingView();
			if (postProcessingView != null)
				((RelativeLayout) guiView.findViewById(R.id.postprocessingLayout)).addView(postProcessingView);
		}
	}

	@Override
	public void onPostProcessingFinished()
	{
		List<View> postprocessingView = new ArrayList<View>();
		RelativeLayout pluginsLayout = (RelativeLayout) MainScreen.getInstance()
				.findViewById(R.id.postprocessingLayout);
		for (int i = 0; i < pluginsLayout.getChildCount(); i++)
			postprocessingView.add(pluginsLayout.getChildAt(i));

		for (int j = 0; j < postprocessingView.size(); j++)
		{
			View view = postprocessingView.get(j);
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);

			pluginsLayout.removeView(view);
		}

		guiView.findViewById(R.id.postprocessingLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.buttonGallery).setEnabled(true);
		guiView.findViewById(R.id.buttonShutter).setEnabled(true);
		guiView.findViewById(R.id.buttonSelectMode).setEnabled(true);

		updateThumbnailButton();
		thumbnailView.invalidate();
	}

	private UpdateThumbnailButtonTask	t	= null;

	public void updateThumbnailButton()
	{

		t = new UpdateThumbnailButtonTask(MainScreen.getInstance());
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
					Log.e("AlmalenceGUI", "Can't stop thumbnail processing");
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
			mThumbnail = Thumbnail.getLastThumbnail(MainScreen.getInstance().getContentResolver());
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
							Bitmap bm = Thumbnail.getRoundedCornerBitmap(bitmap, (int) (MainScreen.getMainContext()
									.getResources().getDimension(R.dimen.mainButtonHeight) * 1.2), (int) (MainScreen
									.getMainContext().getResources().getDimension(R.dimen.mainButtonHeight) / 1.1));

							thumbnailView.setImageBitmap(bm);
						} catch (Exception e)
						{
							Log.v("AlmalenceGUI", "Can't set thumbnail");
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
					Bitmap bm = Thumbnail.getRoundedCornerBitmap(bitmap, (int) (MainScreen.getMainContext()
							.getResources().getDimension(R.dimen.mainButtonHeight) * 1.2), (int) (MainScreen
							.getMainContext().getResources().getDimension(R.dimen.mainButtonHeight) / 1.1));
					thumbnailView.setImageBitmap(bm);
				} catch (Exception e)
				{
					Log.v("AlmalenceGUI", "Can't set thumbnail");
				}
			}
		}
	}

	private ImageView	processingAnim;

	public void startProcessingAnimation()
	{
		if (processingAnim != null && processingAnim.getVisibility() == View.VISIBLE)
			return;

		processingAnim = ((ImageView) guiView.findViewById(R.id.buttonGallery2));
		processingAnim.setVisibility(View.VISIBLE);

		int height = (int) MainScreen.getAppResources().getDimension(R.dimen.paramsLayoutHeightScanner);
		int width = (int) MainScreen.getAppResources().getDimension(R.dimen.paramsLayoutHeightScanner);
		Animation rotation = new RotateAnimation(0, 360, width / 2, height / 2);
		rotation.setDuration(800);
		rotation.setInterpolator(new LinearInterpolator());
		rotation.setRepeatCount(1000);

		processingAnim.startAnimation(rotation);
	}

	public void processingBlockUI()
	{
		RelativeLayout rl = (RelativeLayout) guiView.findViewById(R.id.blockingLayout);
		if (rl.getVisibility() == View.GONE)
		{
			rl.setVisibility(View.VISIBLE);
			rl.bringToFront();

			guiView.findViewById(R.id.buttonGallery).setEnabled(false);
			guiView.findViewById(R.id.buttonShutter).setEnabled(false);
			guiView.findViewById(R.id.buttonSelectMode).setEnabled(false);
		}
	}

	// capture indication - will play shutter icon opened/closed
	private boolean	captureIndication	= true;
	private boolean	isIndicationOn		= false;

	public void startContinuousCaptureIndication()
	{
		captureIndication = true;
		new CountDownTimer(200, 200)
		{
			public void onTick(long millisUntilFinished)
			{
				// Not used
			}

			public void onFinish()
			{
				if (captureIndication)
				{
					if (isIndicationOn)
					{
						((RotateImageView) guiView.findViewById(R.id.buttonShutter))
								.setImageResource(R.drawable.gui_almalence_shutter);
						isIndicationOn = false;
					} else
					{
						((RotateImageView) guiView.findViewById(R.id.buttonShutter))
								.setImageResource(R.drawable.gui_almalence_shutter_pressed);
						isIndicationOn = true;
					}
					startContinuousCaptureIndication();
				}
			}
		}.start();
	}

	public void stopCaptureIndication()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		boolean photoTimeLapseActive = prefs.getBoolean(MainScreen.sPhotoTimeLapseActivePref, false);
		if (photoTimeLapseActive) {
			MainScreen.getInstance().guiManager.setShutterIcon(ShutterButton.DEFAULT);
			selfTimer.updateTimelapseCount();			
			return;
		}
		
		captureIndication = false;
		if (!PluginManager.getInstance().getActiveModeID().equals("video"))
		{
			((RotateImageView) guiView.findViewById(R.id.buttonShutter)).setImageResource(R.drawable.button_shutter);
		}
	}

	public void showCaptureIndication()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		boolean photoTimeLapseActive = prefs.getBoolean(MainScreen.sPhotoTimeLapseActivePref, false);
		boolean photoTimeLapseIsRunning = prefs.getBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);
		if (photoTimeLapseActive && photoTimeLapseIsRunning) {
			MainScreen.getInstance().guiManager.setShutterIcon(ShutterButton.TIMELAPSE_ACTIVE);
			selfTimer.updateTimelapseCount();			
			return;
		}

		new CountDownTimer(400, 200)
		{
			public void onTick(long millisUntilFinished)
			{
				((RotateImageView) guiView.findViewById(R.id.buttonShutter))
						.setImageResource(R.drawable.gui_almalence_shutter_pressed);
			}

			public void onFinish()
			{
				if (!PluginManager.getInstance().getActiveModeID().equals("video"))
				{
					((RotateImageView) guiView.findViewById(R.id.buttonShutter))
							.setImageResource(R.drawable.button_shutter);
				}

			}
		}.start();
	}

	@Override
	public void onCameraSetup()
	{
		// Not used
	}

	@Override
	public void menuButtonPressed()
	{
		// Not used
	}

	@Override
	public void addMode(View mode)
	{
		// Not used
	}

	@Override
	public void SetModeSelected(View v)
	{
		// Not used
	}

	@Override
	public void hideModes()
	{
		// Not used
	}

	@Override
	public int getMaxModeViewWidth()
	{
		return -1;
	}

	@Override
	public int getMaxModeViewHeight()
	{
		return -1;
	}

	@Override
	@TargetApi(14)
	public void setFocusParameters()
	{
		// Not used
	}

	// mode help procedure
	@Override
	public void showHelp(String modeName, String text, int imageID, String preferences)
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		boolean needToShow = prefs.getBoolean(preferences, true);

		// check show help settings
		MainScreen.setShowHelp(prefs.getBoolean("showHelpPrefCommon", true));
		if (!needToShow || !MainScreen.isShowHelp())
			return;

		if (guiView.findViewById(R.id.postprocessingLayout).getVisibility() == View.VISIBLE)
			return;

		final String preference = preferences;

		final View help = guiView.findViewById(R.id.mode_help);
		ImageView helpImage = (ImageView) guiView.findViewById(R.id.helpImage);
		helpImage.setImageResource(imageID);
		TextView helpText = (TextView) guiView.findViewById(R.id.helpText);
		helpText.setText(text);

		TextView helpTextModeName = (TextView) guiView.findViewById(R.id.helpTextModeName);
		helpTextModeName.setText(modeName);

		Button button = (Button) guiView.findViewById(R.id.buttonOk);
		button.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				help.setVisibility(View.GONE);
			}
		});

		Button buttonDontShow = (Button) guiView.findViewById(R.id.buttonDontShow);
		buttonDontShow.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				help.setVisibility(View.GONE);

				{
					Editor prefsEditor = prefs.edit();

					prefsEditor.putBoolean(preference, false);
					prefsEditor.commit();
				}
			}
		});

		help.setVisibility(View.VISIBLE);
		help.bringToFront();
	}

	public View getMainView()
	{
		return guiView;
	}
}
