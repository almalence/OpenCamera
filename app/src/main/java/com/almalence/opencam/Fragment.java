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
 package com.almalence.opencam_plus;
 +++ --> */
// <!-- -+-
package com.almalence.opencam;

//-+- -->

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.provider.DocumentFile;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

/* <!-- +++
 import com.almalence.opencam_plus.ui.SeekBarPreference;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.cameracontroller.CameraController.Size;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.cameracontroller.CameraController.Size;
import com.almalence.opencam.ui.SeekBarPreference;

//-+- -->

/***
 * New interface for preferences. Loads sections for Common preferences.
 ***/

@TargetApi(11)
public class Fragment extends PreferenceFragment implements OnSharedPreferenceChangeListener
{
	public static PreferenceFragment	thiz;
	public static final int				CHOOSE_FOLDER_CODE	= 15;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		thiz = this;

		String settings = getArguments().getString("type");

		ApplicationScreen.getPluginManager().loadHeaderContent(settings, this);

		if (null == getPreferenceScreen())
			return;
		for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++)
		{
			initSummary(getPreferenceScreen().getPreference(i));
		}

		Preference nightPreference = findPreference("night");
		if (nightPreference != null)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			if (prefs.getBoolean("useCamera2Pref", false))
			{
				getPreferenceScreen().removePreference(nightPreference);
			} else
			{
				Preference superPreference = findPreference("super");
				getPreferenceScreen().removePreference(superPreference);
			}
		}

		final SeekBarPreference brightnessPref = (SeekBarPreference) this.findPreference("brightnessPref");
		if (brightnessPref != null)
		{
			// Set seekbar summary :
			float gamma = PreferenceManager.getDefaultSharedPreferences(getActivity()).getFloat("gammaPref", 0.5f);
			brightnessPref.setSummary(this.getString(R.string.Pref_Super_BrightnessEnhancementValue).replace("$1",
					"" + gamma));
			brightnessPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				private final Map<Integer, Float>	gamma_map	= new HashMap<Integer, Float>()
																{
																	{
																		put(0, 0.5f);
																		put(1, 0.55f);
																		put(2, 0.6f);
																		put(3, 0.65f);
																		put(4, 0.7f);
																	}
																};

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
					int radius = (Integer) newValue;
					float gamma = gamma_map.get(radius);
					PreferenceManager.getDefaultSharedPreferences(Fragment.thiz.getActivity().getApplicationContext())
							.edit().putFloat("gammaPref", gamma).commit();
					brightnessPref.setSummary(getActivity().getString(R.string.Pref_Super_BrightnessEnhancementValue)
							.replace("$1", "" + gamma));
					return true;
				}
			});
		}

		final CheckBoxPreference upscalePref = (CheckBoxPreference) this.findPreference("upscaleResult");
		if (upscalePref != null)
		{
			Size size = CameraController.getMaxCameraImageSize(CameraController.YUV);
			long resMpx = 0;
			float mpix = 0.0f;
			if (size != null)
			{
				resMpx = (long) ((long) size.getWidth() * (long) size.getHeight() * 2.25);
				mpix = (float) resMpx / 1000000.f;
			}

			String name = String.format("%3.1f Mpix ", mpix);

			upscalePref
					.setSummary(getActivity().getString(R.string.Pref_Super_SummaryUpscale).replace("$1", "" + name));
		}

		Preference cameraParameters = findPreference("camera_parameters");
		if (cameraParameters != null)
		{
			cameraParameters.setOnPreferenceClickListener(new OnPreferenceClickListener()
			{
				@Override
				public boolean onPreferenceClick(Preference preference)
				{
					try
					{
						showCameraParameters();
					} catch (Exception e)
					{
						e.printStackTrace();
					}

					return true;
				}
			});
		}

		CheckBoxPreference helpPref = (CheckBoxPreference) findPreference("showHelpPrefCommon");
		if (helpPref != null)
			helpPref.setOnPreferenceClickListener(new OnPreferenceClickListener()
			{
				public boolean onPreferenceClick(Preference preference)
				{
					if (((CheckBoxPreference) preference).isChecked())
					{
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen
								.getMainContext());
						Editor prefsEditor = prefs.edit();
						prefsEditor.putBoolean("droShowHelp", true);
						prefsEditor.putBoolean("sequenceRemovalShowHelp", true);
						prefsEditor.putBoolean("panoramaShowHelp", true);
						prefsEditor.putBoolean("superShowHelp", true);
						prefsEditor.putBoolean("groupshotRemovalShowHelp", true);
						prefsEditor.putBoolean("objectRemovalShowHelp", true);
						prefsEditor.putBoolean("bestShotShowHelp", true);
						prefsEditor.commit();
					}

					return true;
				}
			});

		EditTextPreference prefix = (EditTextPreference) this.findPreference(getResources().getString(
				R.string.Preference_SavePathPrefixValue));
		EditTextPreference postfix = (EditTextPreference) this.findPreference(getResources().getString(
				R.string.Preference_SavePathPostfixValue));
		initExportName(null, null);

		if (prefix != null)
		{
			prefix.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
					initExportName(preference, newValue);
					return true;
				}
			});
		}

		if (postfix != null)
		{
			postfix.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
					initExportName(preference, newValue);
					return true;
				}
			});
		}

		Preference sonyPreference = findPreference(MainScreen.sSonyCamerasPref);
		if (sonyPreference != null)
		{
			sonyPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
					boolean sonyCamerasAvailable = (Boolean) newValue;
					if (sonyCamerasAvailable)
					{
						Toast.makeText(getActivity(),
								getActivity().getString(R.string.pref_general_more_sonyCamera_available),
								Toast.LENGTH_SHORT).show();
					}
					return true;
				}
			});
		}

		ListPreference saveToPreference = (ListPreference) this.findPreference(getResources().getString(
				R.string.Preference_SaveToValue));

		// if android 5+, then remove "save to SD card" option. Because it's
		// equals to "save to custom folder" option.
		if (saveToPreference != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			CharSequence[] entries = saveToPreference.getEntries();
			CharSequence[] entriyValues = saveToPreference.getEntryValues();

			CharSequence[] newEntries = new String[2];
			CharSequence[] newEntriyValues = new String[2];

			newEntries[0] = entries[0];
			newEntries[1] = entries[2];
			newEntriyValues[0] = entriyValues[0];
			newEntriyValues[1] = entriyValues[2];

			saveToPreference.setEntries(newEntries);
			saveToPreference.setEntryValues(newEntriyValues);
		}
		if (saveToPreference != null)
		{

			saveToPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue)
				{
					int v = -1;

					int v_old = 0;

					try
					{
						v = Integer.parseInt(newValue.toString());
						v_old = Integer.parseInt(((ListPreference) preference).getValue());
					} catch (NumberFormatException e)
					{

					}

					if ((v == 2 || v == 1) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
					{
						if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
						{
							Toast.makeText(
									MainScreen.getInstance(),
									MainScreen.getAppResources().getString(
											R.string.pref_advanced_saving_saveToPref_CantSaveToSD), Toast.LENGTH_LONG)
									.show();

							if (isDeviceRooted())
							{
								Intent intent = new Intent(Preferences.thiz, FolderPicker.class);
	
								intent.putExtra(MainScreen.sSavePathPref, v_old);
	
								Preferences.thiz.startActivity(intent);
								
								return true;
							}
							else
								return false;
						}
					}

					if (v == 2 || v == 1)
					{
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
						{
							Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
							startActivityForResult(intent, CHOOSE_FOLDER_CODE);
						} else if (v != 1)
						{
							Intent intent = new Intent(Preferences.thiz, FolderPicker.class);

							intent.putExtra(MainScreen.sSavePathPref, v_old);

							Preferences.thiz.startActivity(intent);
						}
					}

					return true;
				}
			});
		}

		PreferenceCategory cat = (PreferenceCategory) this.findPreference("Pref_VFCommon_Preference_Category");
		if (cat != null)
		{
			CheckBoxPreference cp = (CheckBoxPreference) cat.findPreference("maxScreenBrightnessPref");
			if (cp != null)
			{
				cp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
				{
					public boolean onPreferenceChange(Preference preference, Object newValue)
					{
						boolean value = Boolean.parseBoolean(newValue.toString());
						setScreenBrightness(value);
						return true;
					}
				});
			}
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
		boolean MaxScreenBrightnessPreference = prefs.getBoolean("maxScreenBrightnessPref", false);
		setScreenBrightness(MaxScreenBrightnessPreference);
	}
	
	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	//Detects if device is rooted or not
	public static boolean isDeviceRooted() {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3();
    }

    private static boolean checkRootMethod1() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    private static boolean checkRootMethod2() {
        String[] paths = { "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su" };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private static boolean checkRootMethod3() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[] { "/system/xbin/which", "su" });
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (in.readLine() != null) return true;
            return false;
        } catch (Throwable t) {
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }
    //<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

	public static void setScreenBrightness(boolean setMax)
	{
		try
		{
			Window window = thiz.getActivity().getWindow();

			WindowManager.LayoutParams layoutpars = window.getAttributes();

			// Set the brightness of this window
			if (setMax)
				layoutpars.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
			else
				layoutpars.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;

			// Apply attribute changes to this window
			window.setAttributes(layoutpars);
		} catch (Exception e)
		{

		}
	}

	public static void closePrefs()
	{
		thiz.getFragmentManager().popBackStack();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if (null == getPreferenceScreen())
			return;
		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if (null == getPreferenceScreen())
			return;
		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		updatePrefSummary(findPreference(key));
	}

	private void initSummary(Preference p)
	{
		if (p instanceof PreferenceCategory)
		{
			PreferenceCategory pCat = (PreferenceCategory) p;
			for (int i = 0; i < pCat.getPreferenceCount(); i++)
			{
				initSummary(pCat.getPreference(i));
			}
		} else
		{
			updatePrefSummary(p);
		}
	}

	private void initExportName(Preference preference, Object newValue)
	{
		EditTextPreference prefix = (EditTextPreference) this.findPreference(getResources().getString(
				R.string.Preference_SavePathPrefixValue));
		String prefixValue = "";
		if (prefix != null)
		{
			if (preference != null && prefix.getKey().equals(preference.getKey()))
			{
				prefixValue = newValue.toString();
			} else
			{
				prefixValue = prefix.getText();
			}

			if (!prefixValue.equals(""))
			{
				prefixValue = prefixValue + "_";
			}
		}

		EditTextPreference postfix = (EditTextPreference) this.findPreference(getResources().getString(
				R.string.Preference_SavePathPostfixValue));
		String postfixValue = "";
		if (postfix != null)
		{
			if (preference != null && postfix.getKey().equals(preference.getKey()))
			{
				postfixValue = newValue.toString();
			} else
			{
				postfixValue = postfix.getText();
			}

			if (!postfixValue.equals(""))
			{
				postfixValue = "_" + postfixValue;
			}
		}

		ListPreference exportNameList = (ListPreference) this.findPreference(getResources().getString(
				R.string.Preference_ExportNameValue));
		if (exportNameList != null)
		{
			String[] names = MainScreen.getAppResources().getStringArray(R.array.exportNameArray);
			CharSequence[] newNames = new CharSequence[names.length];
			int i = 0;
			for (String name : names)
			{
				newNames[i] = prefixValue + name + postfixValue;
				i++;
			}
			exportNameList.setEntries(newNames);
			exportNameList.setSummary(exportNameList.getEntries()[Integer.parseInt(exportNameList.getValue()) - 1]);
		}
	}

	private void updatePrefSummary(Preference p)
	{
		if (p instanceof ListPreference)
		{
			ListPreference listPref = (ListPreference) p;
			p.setSummary(listPref.getEntry());
		}
		if (p instanceof EditTextPreference)
		{
			EditTextPreference editTextPref = (EditTextPreference) p;
			if (p.getKey().equalsIgnoreCase("editKey"))
			{
				p.setSummary("*****");
			} else
			{
				p.setSummary(editTextPref.getText());
			}
		}
	}

	@TargetApi(13)
	private void showCameraParameters()
	{
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
		alertDialog.setTitle(R.string.Pref_CameraParameters_Title);
		final StringBuilder about_string = new StringBuilder();
		String version = "UNKNOWN_VERSION";
		int version_code = -1;
		try
		{
			PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
			version = pInfo.versionName;
			version_code = pInfo.versionCode;
		} catch (NameNotFoundException e)
		{
			e.printStackTrace();
		}
		about_string.append("\nApplication name: ");
		about_string.append(MainScreen.getInstance().getResources().getString(R.string.Pref_About));
		about_string.append("\nAndroid API version: ");
		about_string.append(Build.VERSION.SDK_INT);
		about_string.append("\nDevice manufacturer: ");
		about_string.append(Build.MANUFACTURER);
		about_string.append("\nDevice model: ");
		about_string.append(Build.MODEL);
		about_string.append("\nDevice code-name: ");
		about_string.append(Build.HARDWARE);
		about_string.append("\nDevice variant: ");
		about_string.append(Build.DEVICE);
		{
			ActivityManager activityManager = (ActivityManager) getActivity().getSystemService(
					Activity.ACTIVITY_SERVICE);
			about_string.append("\nStandard max heap (MB): ");
			about_string.append(activityManager.getMemoryClass());
			about_string.append("\nLarge max heap (MB): ");
			about_string.append(activityManager.getLargeMemoryClass());
		}
		{
			Point display_size = new Point();
			Display display = getActivity().getWindowManager().getDefaultDisplay();
			display.getSize(display_size);
			about_string.append("\nDisplay size: ");
			about_string.append(display_size.x);
			about_string.append("x");
			about_string.append(display_size.y);
		}

		//show camera 2 support level
		int level = CameraController.getCamera2Level();
		about_string.append("\nCamera2 API: ");
		switch (level)
		{
		case 0://limited
			about_string.append("limited");
			break;
		case 1://full
			about_string.append("full");
			break;
		case 2://legacy
			about_string.append("legacy");
			break;
		default:
			about_string.append("not supported");
		}
		
//		about_string.append("\nSensor orientation, back camera: ");
//		about_string.append(CameraController.getSensorOrientation(0));
//		about_string.append("\nSensor orientation, front camera: ");
//		about_string.append(CameraController.getSensorOrientation(1));
		
		if (MainScreen.getInstance().preview_sizes != null)
		{
			about_string.append("\nPreview resolutions: ");
			for (int i = 0; i < MainScreen.getInstance().preview_sizes.size(); i++)
			{
				if (i > 0)
				{
					about_string.append(", ");
				}
				about_string.append(MainScreen.getInstance().preview_sizes.get(i).getWidth());
				about_string.append("x");
				about_string.append(MainScreen.getInstance().preview_sizes.get(i).getHeight());
			}
		}

		about_string.append("\nCurrent camera ID: ");
		about_string.append(MainScreen.getInstance().cameraId);

		if (MainScreen.getInstance().picture_sizes != null)
		{
			about_string.append("\nPhoto resolutions: ");
			for (int i = 0; i < MainScreen.getInstance().picture_sizes.size(); i++)
			{
				if (i > 0)
				{
					about_string.append(", ");
				}
				about_string.append(MainScreen.getInstance().picture_sizes.get(i).getWidth());
				about_string.append("x");
				about_string.append(MainScreen.getInstance().picture_sizes.get(i).getHeight());
			}
		}

		if (MainScreen.getInstance().video_sizes != null)
		{
			about_string.append("\nVideo resolutions: ");
			for (int i = 0; i < MainScreen.getInstance().video_sizes.size(); i++)
			{
				if (i > 0)
				{
					about_string.append(", ");
				}
				about_string.append(MainScreen.getInstance().video_sizes.get(i).getWidth());
				about_string.append("x");
				about_string.append(MainScreen.getInstance().video_sizes.get(i).getHeight());
			}
		}

		about_string.append("\nVideo stabilization: ");
		about_string.append(MainScreen.getInstance().supports_video_stabilization ? "true" : "false");

		about_string.append("\nFlash modes: ");
		if (MainScreen.getInstance().flash_values != null && MainScreen.getInstance().flash_values.size() > 0)
		{
			for (int i = 0; i < MainScreen.getInstance().flash_values.size(); i++)
			{
				if (i > 0)
				{
					about_string.append(", ");
				}
				about_string.append(MainScreen.getInstance().flash_values.get(i));
			}
		} else
		{
			about_string.append("None");
		}

		about_string.append("\nFocus modes: ");
		if (MainScreen.getInstance().focus_values != null && MainScreen.getInstance().focus_values.size() > 0)
		{
			for (int i = 0; i < MainScreen.getInstance().focus_values.size(); i++)
			{
				if (i > 0)
				{
					about_string.append(", ");
				}
				about_string.append(MainScreen.getInstance().focus_values.get(i));
			}
		} else
		{
			about_string.append("None");
		}

		about_string.append("\nScene modes: ");
		if (MainScreen.getInstance().scene_modes_values != null
				&& MainScreen.getInstance().scene_modes_values.size() > 0)
		{
			for (int i = 0; i < MainScreen.getInstance().scene_modes_values.size(); i++)
			{
				if (i > 0)
				{
					about_string.append(", ");
				}
				about_string.append(MainScreen.getInstance().scene_modes_values.get(i));
			}
		} else
		{
			about_string.append("None");
		}

		about_string.append("\nWhite balances: ");
		if (MainScreen.getInstance().white_balances_values != null
				&& MainScreen.getInstance().white_balances_values.size() > 0)
		{
			for (int i = 0; i < MainScreen.getInstance().white_balances_values.size(); i++)
			{
				if (i > 0)
				{
					about_string.append(", ");
				}
				about_string.append(MainScreen.getInstance().white_balances_values.get(i));
			}
		} else
		{
			about_string.append("None");
		}

		about_string.append("\nISOs: ");
		if (MainScreen.getInstance().isos != null && MainScreen.getInstance().isos.size() > 0)
		{
			for (int i = 0; i < MainScreen.getInstance().isos.size(); i++)
			{
				if (i > 0)
				{
					about_string.append(", ");
				}
				about_string.append(MainScreen.getInstance().isos.get(i));
			}
		} else
		{
			about_string.append("None");
		}

		String save_location = SavingService.getSaveToPath();
		about_string.append("\nSave Location: " + save_location);

		if (MainScreen.getInstance().flattenParamteters != null
				&& !MainScreen.getInstance().flattenParamteters.equals(""))
		{
			about_string.append("\nFULL INFO:\n");
			about_string.append(MainScreen.getInstance().flattenParamteters);
		}

		alertDialog.setMessage(about_string);
		alertDialog.setPositiveButton(R.string.Pref_CameraParameters_Ok, null);
		alertDialog.setNegativeButton(R.string.Pref_CameraParameters_CopyToClipboard,
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(
								Activity.CLIPBOARD_SERVICE);
						ClipData clip = ClipData.newPlainText("OpenCamera About", about_string);
						clipboard.setPrimaryClip(clip);
					}
				});
		alertDialog.show();
	}

	@TargetApi(19)
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == CHOOSE_FOLDER_CODE)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.instance);
			if (resultCode == Activity.RESULT_OK)
			{
				Uri treeUri = data.getData();

				getActivity().getContentResolver().takePersistableUriPermission(treeUri,
						Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

				prefs.edit().putString(ApplicationScreen.sSavePathPref, treeUri.toString()).commit();
			} else
			{
				prefs.edit().putString(ApplicationScreen.sSaveToPref, "0").commit();
			}
		} else
		{
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
}
