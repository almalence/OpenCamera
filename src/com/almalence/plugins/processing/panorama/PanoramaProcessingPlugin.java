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

package com.almalence.plugins.processing.panorama;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;

import com.almalence.SwapHeap;
import com.almalence.YuvImage;
import com.almalence.util.ImageConversion;

/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.PluginProcessing;
 import com.almalence.opencam_plus.R;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginProcessing;
import com.almalence.opencam.R;
//-+- -->

public class PanoramaProcessingPlugin extends PluginProcessing
{
	private static final String	TAG							= "PanoramaProcessingPlugin";

	private static final String	PREFERENCES_KEY_SAVEINPUT	= "pref_plugin_processing_panorama_saveinput";

	private boolean				prefSaveInput;
	private boolean				prefLandscape;
	private int					mOrientation;
	private int					out_ptr						= 0;

	public PanoramaProcessingPlugin()
	{
		super("com.almalence.plugins.panoramaprocessing", R.xml.preferences_processing_panorama, 0, 0, null);
	}

	@SuppressLint("DefaultLocale")
	@Override
	public void onStartProcessing(final long sessionID)
	{
		// Log.d(TAG, "onStartProcessing");

		this.prefSaveInput = PreferenceManager.getDefaultSharedPreferences(MainScreen.getInstance()).getBoolean(
				PREFERENCES_KEY_SAVEINPUT, false);

		PluginManager.getInstance().addToSharedMem("modeSaveName" + sessionID,
				PluginManager.getInstance().getActiveMode().modeSaveName);

		int orient = Integer.valueOf(PluginManager.getInstance().getFromSharedMem("frameorientation" + sessionID));
		this.prefLandscape = orient == 0 || orient == 180 ? true : false;
		mOrientation = this.prefLandscape ? (orient == 180 ? 90 : 270) : (orient == 270 ? 180 : 0);

		try
		{
			final int input_width = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
					"pano_width" + sessionID));
			final int input_height = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
					"pano_height" + sessionID));
			final int frames_count = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
					"pano_frames_count" + sessionID));
			final int camera_fov = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
					"pano_camera_fov" + sessionID));
			final boolean use_all = PluginManager.getInstance().getFromSharedMem("pano_useall" + sessionID).equals("1");
			final boolean free_input = use_all ? false : PluginManager.getInstance()
					.getFromSharedMem("pano_freeinput" + sessionID).equals("1");
			final boolean mirror = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem(
					"pano_mirror" + sessionID));

			final int[] frames_ptrs = new int[frames_count];
			final float[][][] frame_trs = new float[frames_count][3][3];

			for (int i = 0; i < frames_count; i++)
			{
				frames_ptrs[i] = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
						"pano_frame" + (i + 1) + "." + sessionID));

				for (int y = 0; y < 3; y++)
				{
					for (int x = 0; x < 3; x++)
					{
						frame_trs[i][y][x] = Float.parseFloat(PluginManager.getInstance().getFromSharedMem(
								"pano_frametrs" + (i + 1) + "." + y + x + "." + sessionID));
					}
				}
			}

			// If images are going to be freed during processing we need to save
			// original frames now.
			if (this.prefSaveInput)
			{
				this.saveFrames(frames_ptrs, 0, frames_ptrs.length, input_width, input_height);
			}

			AlmashotPanorama.initialize();
			final int[] result = AlmashotPanorama.process(input_width, input_height, frames_ptrs, frame_trs,
					camera_fov, use_all, free_input);
			this.out_ptr = result[0];
			final int output_width = result[1];
			final int output_height = result[2];
			final int crop_x = result[3];
			final int crop_y = result[4];
			final int crop_w = result[5];
			final int crop_h = result[6];

			if (mirror)
			{
				ImageConversion.TransformNV21N(this.out_ptr, this.out_ptr, output_width, output_height, 1, 1, 0);
			}

			if (!free_input)
			{
				this.freeFrames(frames_ptrs, 0, frames_ptrs.length);
			}

			if (this.prefLandscape)
			{
				PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID, String.valueOf(output_height));
				PluginManager.getInstance().addToSharedMem("saveImageHeight" + sessionID, String.valueOf(output_width));
			} else
			{
				PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID, String.valueOf(output_width));
				PluginManager.getInstance()
						.addToSharedMem("saveImageHeight" + sessionID, String.valueOf(output_height));
			}
			PluginManager.getInstance().addToSharedMem("resultfromshared" + sessionID, "false");
			PluginManager.getInstance().addToSharedMem("resultframeorientation1" + sessionID,
					String.valueOf(mOrientation));
			PluginManager.getInstance().addToSharedMem("amountofresultframes" + sessionID, "1");
			PluginManager.getInstance().addToSharedMem("resultframe1" + sessionID, String.valueOf(this.out_ptr));
			PluginManager.getInstance().addToSharedMem("resultcrop0" + sessionID, String.valueOf(crop_x));
			PluginManager.getInstance().addToSharedMem("resultcrop1" + sessionID, String.valueOf(crop_y));
			PluginManager.getInstance().addToSharedMem("resultcrop2" + sessionID, String.valueOf(crop_w));
			PluginManager.getInstance().addToSharedMem("resultcrop3" + sessionID, String.valueOf(crop_h));
			AlmashotPanorama.release();
		} catch (final NumberFormatException e)
		{
			Log.e(TAG, "Could not parse shared memory data.");
			throw e;
		}
	}

	@SuppressLint("DefaultLocale")
	private void saveFrames(final int[] images, final int offset, final int count, final int input_width,
			final int input_height)
	{
		File saveDir = PluginManager.getSaveDir(false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

		final String modeName = PluginManager.getInstance().getActiveMode().modeSaveName;
		String fileFormat = PluginManager.getInstance().getExportFileName(modeName);

		final Rect crop = new Rect(0, 0, input_width, input_height);
		for (int i = 0; i < count; ++i)
		{
			final int optr = images[offset + i];
			String index = String.format("_%02d", i);
			File file = new File(saveDir, fileFormat + index + ".jpg");

			FileOutputStream os = null;
			try
			{
				try
				{
					os = new FileOutputStream(file);
				} catch (Exception e)
				{
					// save always if not working saving to sdcard
					e.printStackTrace();
					saveDir = PluginManager.getInstance().getSaveDir(true);
					file = new File(saveDir, fileFormat + index + ".jpg");
					os = new FileOutputStream(file);
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}

			final YuvImage out = new com.almalence.YuvImage(optr, ImageFormat.NV21, input_width, input_height, null);

			int jpegQuality = Integer.parseInt(prefs.getString(MainScreen.sJPEGQualityPref, "95"));
			out.compressToJpeg(crop, jpegQuality, os);

			try
			{
				if (os != null)
				{
					os.close();
				}
			} catch (final IOException e)
			{
				e.printStackTrace();
			}

			try
			{
				final ExifInterface ei = new ExifInterface(file.getAbsolutePath());
				ei.saveAttributes();
			} catch (final IOException e)
			{
				e.printStackTrace();
			}

			ContentValues values = new ContentValues();

			values = new ContentValues();
			values.put(
					ImageColumns.TITLE,
					file.getName().substring(
							0,
							file.getName().lastIndexOf(".") >= 0 ? file.getName().lastIndexOf(".") : file.getName()
									.length()));
			values.put(ImageColumns.DISPLAY_NAME, file.getName());
			values.put(ImageColumns.DATE_TAKEN, System.currentTimeMillis());
			values.put(ImageColumns.MIME_TYPE, "image/jpeg");
			values.put(ImageColumns.DATA, file.getAbsolutePath());

			MainScreen.getInstance().getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);

		}
	}

	private void freeFrames(final int[] images, final int offset, final int count)
	{
		for (int i = 0; i < count; ++i)
		{
			SwapHeap.FreeFromHeap(images[offset + i]);
		}
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