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

package com.almalence.plugins.processing.simple;

/* <!-- +++
import com.almalence.opencam_plus.ApplicationScreen;
import com.almalence.opencam_plus.ConfigParser;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.PluginProcessing;
import com.almalence.opencam_plus.R;
+++ --> */
//<!-- -+-
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.ConfigParser;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginProcessing;
import com.almalence.opencam.R;
//-+- -->

import com.almalence.SwapHeap;
import android.content.SharedPreferences;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;

/***
 * Implements simple processing plugin - just translate shared memory values
 * from captured to result.
 ***/

public class SimpleProcessingPlugin extends PluginProcessing
{
	private static final String	PREFERENCES_KEY_SAVEINPUT	= "Preference_DroSaveInputPref";

	private long				sessionID					= 0;

	private static boolean		DROLocalTMPreference		= true;
	private static int			prefPullYUV					= 7;								// 9;

	private int					modePrefDro					= 1;
	private static boolean		saveInputPreference			= false;

	public SimpleProcessingPlugin()
	{
		super("com.almalence.plugins.simpleprocessing", "single", R.xml.preferences_capture_dro, 0, 0, null);
	}

	@Override
	public void onResume()
	{
		getPrefs();
	}
	
	private void getPrefs()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.instance
				.getBaseContext());
		saveInputPreference = prefs.getBoolean(PREFERENCES_KEY_SAVEINPUT, false);
	}
	
	@Override
	public void onStartProcessing(long SessionID)
	{
		sessionID = SessionID;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		modePrefDro = Integer.parseInt(prefs.getString("modePrefDro", "1"));

		PluginManager.getInstance().addToSharedMem("modeSaveName" + sessionID,
				ConfigParser.getInstance().getMode(mode).modeSaveName);

		int mImageWidth = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("imageWidth" + sessionID));
		int mImageHeight = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("imageHeight" + sessionID));

//		String szstr =  "resolutions w = " + mImageWidth +" h = " +mImageHeight;
//		Toast.makeText(MainScreen.getMainContext(), szstr, Toast.LENGTH_LONG).show();
		
//		ApplicationScreen.instance.runOnUiThread(new Runnable()
//		{
//			public void run()
//			{
//				int mImageWidth = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("imageWidth" + sessionID));
//				int mImageHeight = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("imageHeight" + sessionID));
//				String szstr =  "resolutions w = " + mImageWidth +" h = " +mImageHeight;
//				Toast.makeText(MainScreen.getMainContext(), szstr, Toast.LENGTH_LONG).show();
//			}
//		});
		
		String num = PluginManager.getInstance().getFromSharedMem("amountofcapturedframes" + sessionID);

		if (num == null)
			return;
		int imagesAmount = Integer.parseInt(num);

		String numRAW = PluginManager.getInstance().getFromSharedMem("amountofcapturedrawframes" + sessionID);

		int imagesAmountRAW = 0;
		if (numRAW != null)
			imagesAmountRAW = Integer.parseInt(numRAW);

		int frameNumRAW = 0;
		int frameNum = 0;

		for (int i = 1; i <= imagesAmount; i++)
		{
			int orientation = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
					"frameorientation" + i + sessionID));
			String isDRO = PluginManager.getInstance().getFromSharedMem("isdroprocessing" + sessionID);

			if (isDRO != null && isDRO.equals("0"))
			{//dro processing
				AlmaShotDRO.Initialize();

				int inputYUV = 0;
				inputYUV = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frame" + i + sessionID));

				if (saveInputPreference)
				{
					try
					{
						String fileFormat = PluginManager.getInstance().getFileFormat();
						fileFormat = fileFormat + "_DROSRC";

						PluginManager.getInstance().saveInputFile(true, sessionID, i, null, inputYUV, fileFormat);
						
//						//>>save YUV
//						int len = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("framelen" + i + sessionID));
//						byte[] buffer = SwapHeap.CopyFromHeap(inputYUV, len);
//						
//						
//						File saveDir = PluginManager.getSaveDir(false);
//						File file = new File(saveDir, fileFormat + ".yuv");
//						FileOutputStream os = null;
//
//						try
//						{
//							try
//							{
//								os = new FileOutputStream(file);
//							} catch (Exception e)
//							{
//								// save always if not working saving to sdcard
//								e.printStackTrace();
//								saveDir = PluginManager.getSaveDir(true);
//								file = new File(saveDir, fileFormat + ".jpg");
//
//								os = new FileOutputStream(file);
//							}
//						} catch (FileNotFoundException e1)
//						{
//							// TODO Auto-generated catch block
//							e1.printStackTrace();
//						}
//
//						try
//						{
//							if (os != null)
//								os.write(buffer);
//							os.close();
//						}
//						catch (Exception e)
//						{
//							e.printStackTrace();
//						}
//						//Save YUV<<
						
						
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}

				float[] gammaTable = new float[] { 0.5f, 0.6f, 0.7f };
				int val = 1;
				try
				{
					val = Integer.parseInt(prefs.getString("noisePrefDro", "1"));
				} catch (Exception e)
				{
					e.printStackTrace();
				}
				float dark_noise_pass = 0.35f;
				switch (val)
				{
				case 0:
					dark_noise_pass = 0.1f;
					break;
				case 1:
					dark_noise_pass = 0.35f;
					break;
				case 2:
					dark_noise_pass = 0.7f;
					break;
				default:
					break;
				}
				
				int yuv = AlmaShotDRO.DroProcess(inputYUV, mImageWidth, mImageHeight, 1.5f, DROLocalTMPreference, 0,
						prefPullYUV, dark_noise_pass, gammaTable[modePrefDro]);

				AlmaShotDRO.Release();

				if (orientation == 90 || orientation == 270)
				{
					PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID,
							String.valueOf(mImageHeight));
					PluginManager.getInstance().addToSharedMem("saveImageHeight" + sessionID,
							String.valueOf(mImageWidth));
				} else
				{
					PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID,
							String.valueOf(mImageWidth));
					PluginManager.getInstance().addToSharedMem("saveImageHeight" + sessionID,
							String.valueOf(mImageHeight));
				}
				
				

				PluginManager.getInstance().addToSharedMem("resultframe" + i + sessionID, String.valueOf(yuv));
			} else
			{//single shot processing + raw processing
				int frame = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frame" + i + sessionID));
				int len = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("framelen" + i + sessionID));

				boolean isRAW = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem(
						"frameisraw" + i + sessionID));

				int frameIndex = isRAW ? (++frameNumRAW) : (imagesAmountRAW + (++frameNum));
				
				PluginManager.getInstance().addToSharedMem("resultframeformat" + frameIndex + sessionID,
						isRAW ? "dng" : "jpeg");
				PluginManager.getInstance().addToSharedMem("resultframe" + frameIndex + sessionID,
						String.valueOf(frame));
				PluginManager.getInstance().addToSharedMem("resultframelen" + frameIndex + sessionID,
						String.valueOf(len));

				PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID, String.valueOf(mImageWidth));
				PluginManager.getInstance().addToSharedMem("saveImageHeight" + sessionID, String.valueOf(mImageHeight));
			}

			boolean cameraMirrored = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem(
					"framemirrored" + i + sessionID));
			PluginManager.getInstance().addToSharedMem("resultframeorientation" + i + sessionID,
					String.valueOf(orientation));
			PluginManager.getInstance().addToSharedMem("resultframemirrored" + i + sessionID,
					String.valueOf(cameraMirrored));
		}

		PluginManager.getInstance().addToSharedMem("amountofresultframes" + sessionID, String.valueOf(imagesAmount));
	}

	@Override
	public boolean isPostProcessingNeeded()
	{
		return false;
	}

	@Override
	public void onStartPostProcessing()
	{
	}
}
