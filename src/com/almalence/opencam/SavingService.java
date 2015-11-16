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
//<!-- -+-
package com.almalence.opencam;

//-+- -->

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.hardware.camera2.DngCreator;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import com.almalence.SwapHeap;
import com.almalence.plugins.export.ExifDriver.ExifDriver;
import com.almalence.plugins.export.ExifDriver.ExifManager;
import com.almalence.plugins.export.ExifDriver.Values.ValueByteArray;
import com.almalence.plugins.export.ExifDriver.Values.ValueNumber;
import com.almalence.plugins.export.ExifDriver.Values.ValueRationals;
import com.almalence.util.MLocation;
import com.almalence.util.Util;

/* <!-- +++
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

public class SavingService extends NotificationService
{

	@Override
	public int onStartCommand(Intent intent, int flags, int startid)
	{
		long sessionID = intent.getLongExtra("sessionID", 0);
		if (sessionID == 0)
		{
			return START_NOT_STICKY;
		}

		SavingTask task = new SavingTask();
		task.sessionID = sessionID;
		task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

		return START_NOT_STICKY;
	}

	private class SavingTask extends AsyncTask<Void, Void, Void>
	{
		public long	sessionID	= 0;	// id to identify data flow

		@Override
		protected void onPreExecute()
		{
			showNotification();
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			{
				saveResultPictureNew(sessionID);
			} else
			{
				saveResultPicture(sessionID);
			}

			PluginManager.getInstance().clearSharedMemory(sessionID);
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			hideNotification();
		}
	}

	private static int		jpegQuality				= 95;
	protected static int	saveOption;
	protected static boolean saveOptionSeparator	= false;
	private static boolean	useGeoTaggingPrefExport;
	private static boolean	enableExifTagOrientation;
	private static int		additionalRotation;
	private static int		additionalRotationValue	= 0;
	private static String	saveToPath;
	private static String	saveToPreference;
	private static boolean	sortByData;

	public static String getSaveToPath()
	{
		return saveToPath;
	}

	public void initSavingPrefs()
	{
		initSavingPrefs(getApplicationContext());
	}

	public static void initSavingPrefs(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		saveOption = Integer.parseInt(prefs.getString(ApplicationScreen.sExportNamePref, "2"));
		useGeoTaggingPrefExport = prefs.getBoolean("useGeoTaggingPrefExport", false);
		enableExifTagOrientation = prefs.getBoolean(ApplicationScreen.sEnableExifOrientationTagPref, true);
		additionalRotation = Integer.parseInt(prefs.getString(ApplicationScreen.sAdditionalRotationPref, "0"));

		saveToPath = prefs.getString(MainScreen.sSavePathPref, Environment.getExternalStorageDirectory()
				.getAbsolutePath());
		saveToPreference = prefs.getString(MainScreen.sSaveToPref, "0");
		sortByData = prefs.getBoolean(MainScreen.sSortByDataPref, false);

		switch (additionalRotation)
		{
		case 0:
			additionalRotationValue = 0;
			break;
		case 1:
			additionalRotationValue = -90;
			break;
		case 2:
			additionalRotationValue = 90;
			break;
		case 3:
			additionalRotationValue = 180;
			break;
		}
		
//		if(CameraController.isNexus5x)
//			additionalRotationValue = 180;
	}

	// save result pictures method for android < 5.0
	public void saveResultPicture(long sessionID)
	{
		initSavingPrefs();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		// save fused result
		try
		{
			File saveDir = getSaveDir(false);

			Calendar d = Calendar.getInstance();

			int imagesAmount = Integer.parseInt(getFromSharedMem("amountofresultframes" + Long.toString(sessionID)));

			if (imagesAmount == 0)
				imagesAmount = 1;

			int imageIndex = 0;
			String sImageIndex = getFromSharedMem("resultframeindex" + Long.toString(sessionID));
			if (sImageIndex != null)
				imageIndex = Integer.parseInt(getFromSharedMem("resultframeindex" + Long.toString(sessionID)));

			if (imageIndex != 0)
				imagesAmount = 1;

			ContentValues values = null;

			boolean hasDNGResult = false;
			for (int i = 1; i <= imagesAmount; i++)
			{
				hasDNGResult = false;
				String format = getFromSharedMem("resultframeformat" + i + Long.toString(sessionID));

				if (format != null && format.equalsIgnoreCase("dng"))
					hasDNGResult = true;

				String idx = "";

				if (imagesAmount != 1)
					idx += "_"
							+ ((format != null && !format.equalsIgnoreCase("dng") && hasDNGResult) ? i - imagesAmount
									/ 2 : i);

				String modeName = getFromSharedMem("modeSaveName" + Long.toString(sessionID));
				// define file name format. from settings!
				String fileFormat = getExportFileName(modeName);
				fileFormat += idx + ((format != null && format.equalsIgnoreCase("dng")) ? ".dng" : ".jpg");

				File file;
				if (ApplicationScreen.getForceFilename() == null)
				{
					file = new File(saveDir, fileFormat);
				} else
				{
					file = ApplicationScreen.getForceFilename();
				}

				OutputStream os = null;
				if (ApplicationScreen.getForceFilename() != null)
				{
					os = getApplicationContext().getContentResolver().openOutputStream(
							ApplicationScreen.getForceFilenameURI());
				} else
				{
					try
					{
						os = new FileOutputStream(file);
					} catch (Exception e)
					{
						// save always if not working saving to sdcard
						e.printStackTrace();
						saveDir = getSaveDir(true);
						if (ApplicationScreen.getForceFilename() == null)
						{
							file = new File(saveDir, fileFormat);
						} else
						{
							file = ApplicationScreen.getForceFilename();
						}
						os = new FileOutputStream(file);
					}
				}

				// Take only one result frame from several results
				// Used for PreShot plugin that may decide which result to save
				if (imagesAmount == 1 && imageIndex != 0)
					i = imageIndex;

				String resultOrientation = getFromSharedMem("resultframeorientation" + i + Long.toString(sessionID));
				int orientation = 0;
				if (resultOrientation != null)
					orientation = Integer.parseInt(resultOrientation);

				String resultMirrored = getFromSharedMem("resultframemirrored" + i + Long.toString(sessionID));
				Boolean cameraMirrored = false;
				if (resultMirrored != null)
					cameraMirrored = Boolean.parseBoolean(resultMirrored);

				int x = Integer.parseInt(getFromSharedMem("saveImageHeight" + Long.toString(sessionID)));
				int y = Integer.parseInt(getFromSharedMem("saveImageWidth" + Long.toString(sessionID)));
				if (orientation == 0 || orientation == 180 || (format != null && format.equalsIgnoreCase("dng")))
				{
					x = Integer.valueOf(getFromSharedMem("saveImageWidth" + Long.toString(sessionID)));
					y = Integer.valueOf(getFromSharedMem("saveImageHeight" + Long.toString(sessionID)));
				}

				Boolean writeOrientationTag = true;
				String writeOrientTag = getFromSharedMem("writeorientationtag" + Long.toString(sessionID));
				if (writeOrientTag != null)
					writeOrientationTag = Boolean.parseBoolean(writeOrientTag);

				if (format != null && format.equalsIgnoreCase("jpeg"))
				{// if result in jpeg format

					if (os != null)
					{
						byte[] frame = SwapHeap.SwapFromHeap(
								Integer.parseInt(getFromSharedMem("resultframe" + i + Long.toString(sessionID))),
								Integer.parseInt(getFromSharedMem("resultframelen" + i + Long.toString(sessionID))));
						os.write(frame);
						try
						{
							os.close();
						} catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				} else if (format != null && format.equalsIgnoreCase("dng"))
				{
					saveDNGPicture(i, sessionID, os, x, y, orientation, cameraMirrored);
				} else
				{// if result in nv21 format
					int yuv = Integer.parseInt(getFromSharedMem("resultframe" + i + Long.toString(sessionID)));
					com.almalence.YuvImage out = new com.almalence.YuvImage(yuv, ImageFormat.NV21, x, y, null);
					Rect r;

					String res = getFromSharedMem("resultfromshared" + Long.toString(sessionID));
					if ((null == res) || "".equals(res) || "true".equals(res))
					{
						// to avoid problems with SKIA
						int cropHeight = out.getHeight() - out.getHeight() % 16;
						r = new Rect(0, 0, out.getWidth(), cropHeight);
					} else
					{
						if (null == getFromSharedMem("resultcrop0" + Long.toString(sessionID)))
						{
							// to avoid problems with SKIA
							int cropHeight = out.getHeight() - out.getHeight() % 16;
							r = new Rect(0, 0, out.getWidth(), cropHeight);
						} else
						{
							int crop0 = Integer.parseInt(getFromSharedMem("resultcrop0" + Long.toString(sessionID)));
							int crop1 = Integer.parseInt(getFromSharedMem("resultcrop1" + Long.toString(sessionID)));
							int crop2 = Integer.parseInt(getFromSharedMem("resultcrop2" + Long.toString(sessionID)));
							int crop3 = Integer.parseInt(getFromSharedMem("resultcrop3" + Long.toString(sessionID)));

							r = new Rect(crop0, crop1, crop0 + crop2, crop1 + crop3);

						}
					}

					jpegQuality = Integer.parseInt(prefs.getString(ApplicationScreen.sJPEGQualityPref, "95"));
					if (!out.compressToJpeg(r, jpegQuality, os))
					{
						if (ApplicationScreen.instance != null && ApplicationScreen.getMessageHandler() != null)
						{
							ApplicationScreen.getMessageHandler().sendEmptyMessage(
									ApplicationInterface.MSG_EXPORT_FINISHED_IOEXCEPTION);
						}
						return;
					}
					SwapHeap.FreeFromHeap(yuv);
				}

				String orientation_tag = String.valueOf(0);
//				int sensorOrientation = CameraController.getSensorOrientation();
//				int displayOrientation = CameraController.getDisplayOrientation();
//				sensorOrientation = (360 + sensorOrientation + (cameraMirrored ? -displayOrientation
//						: displayOrientation)) % 360;

				if (CameraController.isNexus6 && cameraMirrored)
					orientation = (orientation + 180) % 360;

				switch (orientation)
				{
				default:
				case 0:
					orientation_tag = String.valueOf(0);
					break;
				case 90:
					orientation_tag = cameraMirrored ? String.valueOf(270) : String.valueOf(90);
					break;
				case 180:
					orientation_tag = String.valueOf(180);
					break;
				case 270:
					orientation_tag = cameraMirrored ? String.valueOf(90) : String.valueOf(270);
					break;
				}

				int exif_orientation = ExifInterface.ORIENTATION_NORMAL;
				if (writeOrientationTag)
				{
					switch ((orientation + 360) % 360)
					{
					default:
					case 0:
						exif_orientation = ExifInterface.ORIENTATION_NORMAL;
						break;
					case 90:
						exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_270
								: ExifInterface.ORIENTATION_ROTATE_90;
						break;
					case 180:
						exif_orientation = ExifInterface.ORIENTATION_ROTATE_180;
						break;
					case 270:
						exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_90
								: ExifInterface.ORIENTATION_ROTATE_270;
						break;
					}
				} else
				{
					switch ((additionalRotationValue + 360) % 360)
					{
					default:
					case 0:
						exif_orientation = ExifInterface.ORIENTATION_NORMAL;
						break;
					case 90:
						exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_270
								: ExifInterface.ORIENTATION_ROTATE_90;
						break;
					case 180:
						exif_orientation = ExifInterface.ORIENTATION_ROTATE_180;
						break;
					case 270:
						exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_90
								: ExifInterface.ORIENTATION_ROTATE_270;
						break;
					}
				}

				if (!enableExifTagOrientation)
					exif_orientation = ExifInterface.ORIENTATION_NORMAL;

				File parent = file.getParentFile();
				String path = parent.toString().toLowerCase();
				String name = parent.getName().toLowerCase();

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

				if (enableExifTagOrientation)
				{
					if (writeOrientationTag)
					{
						values.put(ImageColumns.ORIENTATION, String.valueOf((Integer.parseInt(orientation_tag)
								+ additionalRotationValue + 360) % 360));
					} else
					{
						values.put(ImageColumns.ORIENTATION, String.valueOf((additionalRotationValue + 360) % 360));
					}
				} else
				{
					values.put(ImageColumns.ORIENTATION, String.valueOf(0));
				}

				values.put(ImageColumns.BUCKET_ID, path.hashCode());
				values.put(ImageColumns.BUCKET_DISPLAY_NAME, name);
				values.put(ImageColumns.DATA, file.getAbsolutePath());

				File tmpFile;
				if (ApplicationScreen.getForceFilename() == null)
				{
					tmpFile = file;
				} else
				{
					tmpFile = new File(getApplicationContext().getFilesDir(), "buffer.jpeg");
					tmpFile.createNewFile();
					copyFromForceFileName(tmpFile);
				}

				if (!enableExifTagOrientation)
				{
					Matrix matrix = new Matrix();
					if (writeOrientationTag && (orientation + additionalRotationValue) != 0)
					{
						matrix.postRotate((orientation + additionalRotationValue + 360) % 360);
						rotateImage(tmpFile, matrix);
					} else if (!writeOrientationTag && additionalRotationValue != 0)
					{
						matrix.postRotate((additionalRotationValue + 360) % 360);
						rotateImage(tmpFile, matrix);
					}
				}

				if (useGeoTaggingPrefExport)
				{
					Location l = MLocation.getLocation(getApplicationContext());
					if (l != null)
					{
						double lat = l.getLatitude();
						double lon = l.getLongitude();
						boolean hasLatLon = (lat != 0.0d) || (lon != 0.0d);
						if (hasLatLon)
						{
							values.put(ImageColumns.LATITUDE, l.getLatitude());
							values.put(ImageColumns.LONGITUDE, l.getLongitude());
						}
					}
				}

				File modifiedFile = saveExifTags(tmpFile, sessionID, i, x, y, exif_orientation,
						useGeoTaggingPrefExport, enableExifTagOrientation);
				if (ApplicationScreen.getForceFilename() == null)
				{
					file.delete();
					modifiedFile.renameTo(file);
				} else
				{
					copyToForceFileName(modifiedFile);
					tmpFile.delete();
					modifiedFile.delete();
				}

				Uri uri = getApplicationContext().getContentResolver()
						.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
				broadcastNewPicture(uri);
			}

			ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED);
		} catch (IOException e)
		{
			e.printStackTrace();
			ApplicationScreen.getMessageHandler()
					.sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED_IOEXCEPTION);
			return;
		} catch (Exception e)
		{
			e.printStackTrace();
			ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED);
		} finally
		{
			ApplicationScreen.setForceFilename(null);
		}
	}

	// save result pictures method for android >= 5.0
	public void saveResultPictureNew(long sessionID)
	{
		if (ApplicationScreen.getForceFilename() != null)
		{
			saveResultPicture(sessionID);
			return;
		}

		initSavingPrefs();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		// save fused result
		try
		{
			DocumentFile saveDir = getSaveDirNew(false);

			int imagesAmount = Integer.parseInt(getFromSharedMem("amountofresultframes" + Long.toString(sessionID)));

			if (imagesAmount == 0)
				imagesAmount = 1;

			int imageIndex = 0;
			String sImageIndex = getFromSharedMem("resultframeindex" + Long.toString(sessionID));
			if (sImageIndex != null)
				imageIndex = Integer.parseInt(getFromSharedMem("resultframeindex" + Long.toString(sessionID)));

			if (imageIndex != 0)
				imagesAmount = 1;

			ContentValues values = null;

			boolean hasDNGResult = false;
			for (int i = 1; i <= imagesAmount; i++)
			{
				hasDNGResult = false;
				String format = getFromSharedMem("resultframeformat" + i + Long.toString(sessionID));

				if (format != null && format.equalsIgnoreCase("dng"))
					hasDNGResult = true;

				String idx = "";

				if (imagesAmount != 1)
					idx += "_"
							+ ((format != null && !format.equalsIgnoreCase("dng") && hasDNGResult) ? i - imagesAmount
									/ 2 : i);

				String modeName = getFromSharedMem("modeSaveName" + Long.toString(sessionID));

				// define file name format. from settings!
				String fileFormat = getExportFileName(modeName);
				fileFormat += idx;

				DocumentFile file = null;
				if (ApplicationScreen.getForceFilename() == null)
				{
					if (hasDNGResult)
					{
						file = saveDir.createFile("image/x-adobe-dng", fileFormat + ".dng");
					} else
					{
						file = saveDir.createFile("image/jpeg", fileFormat);
					}
				} else
				{
					file = DocumentFile.fromFile(ApplicationScreen.getForceFilename());
				}

				// Create buffer image to deal with exif tags.
				OutputStream os = null;
				File bufFile = new File(getApplicationContext().getFilesDir(), "buffer.jpeg");
				try
				{
					os = new FileOutputStream(bufFile);
				} catch (Exception e)
				{
					e.printStackTrace();
				}

				// Take only one result frame from several results
				// Used for PreShot plugin that may decide which result to save
				if (imagesAmount == 1 && imageIndex != 0)
				{
					i = imageIndex;
					//With changed frame index we have to get appropriate frame format
					format = getFromSharedMem("resultframeformat" + i + Long.toString(sessionID));
				}

				String resultOrientation = getFromSharedMem("resultframeorientation" + i + Long.toString(sessionID));
				int orientation = 0;
				if (resultOrientation != null)
					orientation = Integer.parseInt(resultOrientation);

				String resultMirrored = getFromSharedMem("resultframemirrored" + i + Long.toString(sessionID));
				Boolean cameraMirrored = false;
				if (resultMirrored != null)
					cameraMirrored = Boolean.parseBoolean(resultMirrored);

				int x = Integer.parseInt(getFromSharedMem("saveImageHeight" + Long.toString(sessionID)));
				int y = Integer.parseInt(getFromSharedMem("saveImageWidth" + Long.toString(sessionID)));
				if (orientation == 0 || orientation == 180 || (format != null && format.equalsIgnoreCase("dng")))
				{
					x = Integer.valueOf(getFromSharedMem("saveImageWidth" + Long.toString(sessionID)));
					y = Integer.valueOf(getFromSharedMem("saveImageHeight" + Long.toString(sessionID)));
				}

				Boolean writeOrientationTag = true;
				String writeOrientTag = getFromSharedMem("writeorientationtag" + Long.toString(sessionID));
				if (writeOrientTag != null)
					writeOrientationTag = Boolean.parseBoolean(writeOrientTag);

				if (format != null && format.equalsIgnoreCase("jpeg"))
				{// if result in jpeg format

					if (os != null)
					{
						byte[] frame = SwapHeap.SwapFromHeap(
								Integer.parseInt(getFromSharedMem("resultframe" + i + Long.toString(sessionID))),
								Integer.parseInt(getFromSharedMem("resultframelen" + i + Long.toString(sessionID))));
						os.write(frame);
						try
						{
							os.close();
						} catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				} else if (format != null && format.equalsIgnoreCase("dng")
						&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
				{
					saveDNGPicture(i, sessionID, os, x, y, orientation, cameraMirrored);
				} else
				{// if result in nv21 format
					int yuv = Integer.parseInt(getFromSharedMem("resultframe" + i + Long.toString(sessionID)));
					com.almalence.YuvImage out = new com.almalence.YuvImage(yuv, ImageFormat.NV21, x, y, null);
					Rect r;

					String res = getFromSharedMem("resultfromshared" + Long.toString(sessionID));
					if ((null == res) || "".equals(res) || "true".equals(res))
					{
						// to avoid problems with SKIA
						int cropHeight = out.getHeight() - out.getHeight() % 16;
						r = new Rect(0, 0, out.getWidth(), cropHeight);
					} else
					{
						if (null == getFromSharedMem("resultcrop0" + Long.toString(sessionID)))
						{
							// to avoid problems with SKIA
							int cropHeight = out.getHeight() - out.getHeight() % 16;
							r = new Rect(0, 0, out.getWidth(), cropHeight);
						} else
						{
							int crop0 = Integer.parseInt(getFromSharedMem("resultcrop0" + Long.toString(sessionID)));
							int crop1 = Integer.parseInt(getFromSharedMem("resultcrop1" + Long.toString(sessionID)));
							int crop2 = Integer.parseInt(getFromSharedMem("resultcrop2" + Long.toString(sessionID)));
							int crop3 = Integer.parseInt(getFromSharedMem("resultcrop3" + Long.toString(sessionID)));

							r = new Rect(crop0, crop1, crop0 + crop2, crop1 + crop3);
						}
					}

					jpegQuality = Integer.parseInt(prefs.getString(ApplicationScreen.sJPEGQualityPref, "95"));
					if (!out.compressToJpeg(r, jpegQuality, os))
					{
						ApplicationScreen.getMessageHandler().sendEmptyMessage(
								ApplicationInterface.MSG_EXPORT_FINISHED_IOEXCEPTION);
						return;
					}
					SwapHeap.FreeFromHeap(yuv);
				}

				String orientation_tag = String.valueOf(0);
//				int sensorOrientation = CameraController.getSensorOrientation(CameraController.getCameraIndex());
//				int displayOrientation = CameraController.getDisplayOrientation();
////				sensorOrientation = (360 + sensorOrientation + (cameraMirrored ? -displayOrientation
////						: displayOrientation)) % 360;
//				if (cameraMirrored) displayOrientation = -displayOrientation;
//				
//				// Calculate desired JPEG orientation relative to camera orientation to make
//				// the image upright relative to the device orientation
//				orientation = (sensorOrientation + displayOrientation + 360) % 360;
				
				if (CameraController.isNexus6 && cameraMirrored)
					orientation = (orientation + 180) % 360;

				switch (orientation)
				{
				default:
				case 0:
					orientation_tag = String.valueOf(0);
					break;
				case 90:
					orientation_tag = cameraMirrored ? String.valueOf(270) : String.valueOf(90);
					break;
				case 180:
					orientation_tag = String.valueOf(180);
					break;
				case 270:
					orientation_tag = cameraMirrored ? String.valueOf(90) : String.valueOf(270);
					break;
				}

				int exif_orientation = ExifInterface.ORIENTATION_NORMAL;
				if (writeOrientationTag)
				{
					switch ((orientation + 360) % 360)
					{
					default:
					case 0:
						exif_orientation = ExifInterface.ORIENTATION_NORMAL;
						break;
					case 90:
						exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_270
								: ExifInterface.ORIENTATION_ROTATE_90;
						break;
					case 180:
						exif_orientation = ExifInterface.ORIENTATION_ROTATE_180;
						break;
					case 270:
						exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_90
								: ExifInterface.ORIENTATION_ROTATE_270;
						break;
					}
				} else
				{
					switch ((additionalRotationValue + 360) % 360)
					{
					default:
					case 0:
						exif_orientation = ExifInterface.ORIENTATION_NORMAL;
						break;
					case 90:
						exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_270
								: ExifInterface.ORIENTATION_ROTATE_90;
						break;
					case 180:
						exif_orientation = ExifInterface.ORIENTATION_ROTATE_180;
						break;
					case 270:
						exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_90
								: ExifInterface.ORIENTATION_ROTATE_270;
						break;
					}
				}

				if (!enableExifTagOrientation)
					exif_orientation = ExifInterface.ORIENTATION_NORMAL;

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

				if (enableExifTagOrientation)
				{
					if (writeOrientationTag)
					{
						values.put(ImageColumns.ORIENTATION, String.valueOf((Integer.parseInt(orientation_tag)
								+ additionalRotationValue + 360) % 360));
					} else
					{
						values.put(ImageColumns.ORIENTATION, String.valueOf((additionalRotationValue + 360) % 360));
					}
				} else
				{
					values.put(ImageColumns.ORIENTATION, String.valueOf(0));
				}

				String filePath = file.getName();

				// If we able to get File object, than get path from it.
				// fileObject should not be null for files on phone memory.
				File fileObject = Util.getFileFromDocumentFile(file);
				if (fileObject != null)
				{
					filePath = fileObject.getAbsolutePath();
					values.put(ImageColumns.DATA, filePath);
				} else
				{
					// This case should typically happen for files saved to SD
					// card.
					String documentPath = Util.getAbsolutePathFromDocumentFile(file);
					values.put(ImageColumns.DATA, documentPath);
				}

				if (!enableExifTagOrientation && !hasDNGResult)
				{
					Matrix matrix = new Matrix();
					if (writeOrientationTag && (orientation + additionalRotationValue) != 0)
					{
						matrix.postRotate((orientation + additionalRotationValue + 360) % 360);
						rotateImage(bufFile, matrix);
					} else if (!writeOrientationTag && additionalRotationValue != 0)
					{
						matrix.postRotate((additionalRotationValue + 360) % 360);
						rotateImage(bufFile, matrix);
					}
				}

				if (useGeoTaggingPrefExport)
				{
					Location l = MLocation.getLocation(getApplicationContext());
					if (l != null)
					{
						double lat = l.getLatitude();
						double lon = l.getLongitude();
						boolean hasLatLon = (lat != 0.0d) || (lon != 0.0d);
						if (hasLatLon)
						{
							values.put(ImageColumns.LATITUDE, l.getLatitude());
							values.put(ImageColumns.LONGITUDE, l.getLongitude());
						}
					}
				}

				File modifiedFile = null;
				if (!hasDNGResult)
				{
					modifiedFile = saveExifTags(bufFile, sessionID, i, x, y, exif_orientation, useGeoTaggingPrefExport,
							enableExifTagOrientation);
				}
				if (modifiedFile != null)
				{
					bufFile.delete();

					if (ApplicationScreen.getForceFilename() == null)
					{
						// Copy buffer image with exif tags into result file.
						InputStream is = null;
						int len;
						byte[] buf = new byte[4096];
						try
						{
							os = getApplicationContext().getContentResolver().openOutputStream(file.getUri());
							is = new FileInputStream(modifiedFile);
							while ((len = is.read(buf)) > 0)
							{
								os.write(buf, 0, len);
							}
							is.close();
							os.close();
						}
						catch (IOException eIO)
						{
							eIO.printStackTrace();
							final IOException eIOFinal = eIO;
							ApplicationScreen.instance.runOnUiThread(new Runnable()
							{
								public void run()
								{
									Toast.makeText(MainScreen.getMainContext(), "Error ocurred:" + eIOFinal.getLocalizedMessage(),
											Toast.LENGTH_LONG).show();
								}
							});
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					} else
					{
						copyToForceFileName(modifiedFile);
					}

					modifiedFile.delete();
				} else
				{
					// Copy buffer image into result file.
					InputStream is = null;
					int len;
					byte[] buf = new byte[4096];
					try
					{
						os = getApplicationContext().getContentResolver().openOutputStream(file.getUri());
						is = new FileInputStream(bufFile);
						while ((len = is.read(buf)) > 0)
						{
							os.write(buf, 0, len);
						}
						is.close();
						os.close();
					} catch (Exception e)
					{
						e.printStackTrace();
					}
					bufFile.delete();
				}

				Uri uri = getApplicationContext().getContentResolver()
						.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
				broadcastNewPicture(uri);
			}

			ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED);
		} catch (IOException e)
		{
			e.printStackTrace();
			ApplicationScreen.getMessageHandler()
					.sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED_IOEXCEPTION);
			return;
		} catch (Exception e)
		{
			e.printStackTrace();
			ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED);
		}
	}

	public static void broadcastNewPicture(Uri uri)
	{
		if (ApplicationScreen.instance != null)
		{
			ApplicationScreen.instance.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
			ApplicationScreen.getMainContext().sendBroadcast(new Intent("android.hardware.action.NEW_PICTURE", uri));
			// Keep compatibility
			ApplicationScreen.getMainContext().sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));
		}
	}

	protected File saveExifTags(File file, long sessionID, int i, int x, int y, int exif_orientation,
			boolean useGeoTaggingPrefExport, boolean enableExifTagOrientation)
	{
		addTimestamp(file, exif_orientation);

		// Set tag_model using ExifInterface.
		// If we try set tag_model using ExifDriver, then standard
		// gallery of android (Nexus 4) will crash on this file.
		// Can't figure out why, other Exif tools work fine.
		try
		{
			ExifInterface ei = new ExifInterface(file.getAbsolutePath());
			String tag_model = getFromSharedMem("exiftag_model" + Long.toString(sessionID));
			String tag_make = getFromSharedMem("exiftag_make" + Long.toString(sessionID));
			if (tag_model == null)
				tag_model = Build.MODEL; 
			ei.setAttribute(ExifInterface.TAG_MODEL, tag_model);
			if (tag_make == null)
				tag_make = Build.MANUFACTURER;	
			ei.setAttribute(ExifInterface.TAG_MAKE, tag_make);
			ei.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(exif_orientation));
			ei.saveAttributes();
		} catch (IOException e1)
		{
			e1.printStackTrace();
		}

		// Open ExifDriver.
		ExifDriver exifDriver = ExifDriver.getInstance(file.getAbsolutePath());
		ExifManager exifManager = null;
		if (exifDriver != null)
		{
			exifManager = new ExifManager(exifDriver, getApplicationContext());
		}

		if (useGeoTaggingPrefExport)
		{
			Location l = MLocation.getLocation(getApplicationContext());

			if (l != null)
			{
				double lat = l.getLatitude();
				double lon = l.getLongitude();
				boolean hasLatLon = (lat != 0.0d) || (lon != 0.0d);

				if (hasLatLon)
				{
					exifManager.setGPSLocation(l.getLatitude(), l.getLongitude(), l.getAltitude());
				}

				String GPSDateString = new SimpleDateFormat("yyyy:MM:dd").format(new Date(l.getTime()));
				if (GPSDateString != null)
				{
					ValueByteArray value = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
					value.setBytes(GPSDateString.getBytes());
					exifDriver.getIfdGps().put(ExifDriver.TAG_GPS_DATE_STAMP, value);
				}
			}
		}

		String tag_exposure_time = getFromSharedMem("exiftag_exposure_time" + Long.toString(sessionID));
		String tag_aperture = getFromSharedMem("exiftag_aperture" + Long.toString(sessionID));
		String tag_flash = getFromSharedMem("exiftag_flash" + Long.toString(sessionID));
		String tag_focal_length = getFromSharedMem("exiftag_focal_lenght" + Long.toString(sessionID));
		String tag_iso = getFromSharedMem("exiftag_iso" + Long.toString(sessionID));
		String tag_white_balance = getFromSharedMem("exiftag_white_balance" + Long.toString(sessionID));
		String tag_spectral_sensitivity = getFromSharedMem("exiftag_spectral_sensitivity" + Long.toString(sessionID));
		String tag_version = getFromSharedMem("exiftag_version" + Long.toString(sessionID));
		String tag_scene = getFromSharedMem("exiftag_scene_capture_type" + Long.toString(sessionID));
		String tag_metering_mode = getFromSharedMem("exiftag_metering_mode" + Long.toString(sessionID));

		if (exifDriver != null)
		{
			if (tag_exposure_time != null)
			{
				int[][] ratValue = ExifManager.stringToRational(tag_exposure_time);
				if (ratValue != null)
				{
					ValueRationals value = new ValueRationals(ExifDriver.FORMAT_UNSIGNED_RATIONAL);
					value.setRationals(ratValue);
					exifDriver.getIfdExif().put(ExifDriver.TAG_EXPOSURE_TIME, value);
				}
			} else
			{ // hack for expo bracketing
				tag_exposure_time = getFromSharedMem("exiftag_exposure_time" + Integer.toString(i)
						+ Long.toString(sessionID));
				if (tag_exposure_time != null)
				{
					int[][] ratValue = ExifManager.stringToRational(tag_exposure_time);
					if (ratValue != null)
					{
						ValueRationals value = new ValueRationals(ExifDriver.FORMAT_UNSIGNED_RATIONAL);
						value.setRationals(ratValue);
						exifDriver.getIfdExif().put(ExifDriver.TAG_EXPOSURE_TIME, value);
					}
				}
			}
			if (tag_aperture != null)
			{
				int[][] ratValue = ExifManager.stringToRational(tag_aperture);
				if (ratValue != null)
				{
					ValueRationals value = new ValueRationals(ExifDriver.FORMAT_UNSIGNED_RATIONAL);
					value.setRationals(ratValue);
					exifDriver.getIfdExif().put(ExifDriver.TAG_APERTURE_VALUE, value);
				}
			}
			if (tag_flash != null)
			{
				ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT, Integer.parseInt(tag_flash));
				exifDriver.getIfdExif().put(ExifDriver.TAG_FLASH, value);
			}
			if (tag_focal_length != null)
			{
				int[][] ratValue = ExifManager.stringToRational(tag_focal_length);
				if (ratValue != null)
				{
					ValueRationals value = new ValueRationals(ExifDriver.FORMAT_UNSIGNED_RATIONAL);
					value.setRationals(ratValue);
					exifDriver.getIfdExif().put(ExifDriver.TAG_FOCAL_LENGTH, value);
				}
			}
			try
			{
				if (tag_iso != null)
				{
					if (tag_iso.indexOf("ISO") > 0)
					{
						tag_iso = tag_iso.substring(0, 2);
					}
					ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT, Integer.parseInt(tag_iso));
					exifDriver.getIfdExif().put(ExifDriver.TAG_ISO_SPEED_RATINGS, value);
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			if (tag_scene != null)
			{
				ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT, Integer.parseInt(tag_scene));
				exifDriver.getIfdExif().put(ExifDriver.TAG_SCENE_CAPTURE_TYPE, value);
			} else
			{
				int sceneMode = CameraController.getSceneMode();

				int sceneModeVal = 0;
				if (sceneMode == CameraParameters.SCENE_MODE_LANDSCAPE)
				{
					sceneModeVal = 1;
				} else if (sceneMode == CameraParameters.SCENE_MODE_PORTRAIT)
				{
					sceneModeVal = 2;
				} else if (sceneMode == CameraParameters.SCENE_MODE_NIGHT)
				{
					sceneModeVal = 3;
				}

				ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT, sceneModeVal);
				exifDriver.getIfdExif().put(ExifDriver.TAG_SCENE_CAPTURE_TYPE, value);
			}
			if (tag_white_balance != null)
			{
				exifDriver.getIfd0().remove(ExifDriver.TAG_LIGHT_SOURCE);

				ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT,
						Integer.parseInt(tag_white_balance));
				exifDriver.getIfdExif().put(ExifDriver.TAG_WHITE_BALANCE, value);
				exifDriver.getIfdExif().put(ExifDriver.TAG_LIGHT_SOURCE, value);
			} else
			{
				exifDriver.getIfd0().remove(ExifDriver.TAG_LIGHT_SOURCE);

				int whiteBalance = CameraController.getWBMode();
				int whiteBalanceVal = 0;
				int lightSourceVal = 0;
				if (whiteBalance == CameraParameters.WB_MODE_AUTO)
				{
					whiteBalanceVal = 0;
					lightSourceVal = 0;
				} else
				{
					whiteBalanceVal = 1;
					lightSourceVal = 0;
				}

				if (whiteBalance == CameraParameters.WB_MODE_DAYLIGHT)
				{
					lightSourceVal = 1;
				} else if (whiteBalance == CameraParameters.WB_MODE_FLUORESCENT)
				{
					lightSourceVal = 2;
				} else if (whiteBalance == CameraParameters.WB_MODE_WARM_FLUORESCENT)
				{
					lightSourceVal = 2;
				} else if (whiteBalance == CameraParameters.WB_MODE_INCANDESCENT)
				{
					lightSourceVal = 3;
				} else if (whiteBalance == CameraParameters.WB_MODE_TWILIGHT)
				{
					lightSourceVal = 3;
				} else if (whiteBalance == CameraParameters.WB_MODE_CLOUDY_DAYLIGHT)
				{
					lightSourceVal = 10;
				} else if (whiteBalance == CameraParameters.WB_MODE_SHADE)
				{
					lightSourceVal = 11;
				}

				ValueNumber valueWB = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT, whiteBalanceVal);
				exifDriver.getIfdExif().put(ExifDriver.TAG_WHITE_BALANCE, valueWB);

				ValueNumber valueLS = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT, lightSourceVal);
				exifDriver.getIfdExif().put(ExifDriver.TAG_LIGHT_SOURCE, valueLS);
			}
			if (tag_spectral_sensitivity != null)
			{
				ValueByteArray value = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
				value.setBytes(tag_spectral_sensitivity.getBytes());
				exifDriver.getIfd0().put(ExifDriver.TAG_SPECTRAL_SENSITIVITY, value);
			}
			if (tag_version != null && !tag_version.equals("48 50 50 48"))
			{
				ValueByteArray value = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
				value.setBytes(tag_version.getBytes());
				exifDriver.getIfd0().put(ExifDriver.TAG_EXIF_VERSION, value);
			} else
			{
				ValueByteArray value = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
				byte[] version = { (byte) 48, (byte) 50, (byte) 50, (byte) 48 };
				value.setBytes(version);
				exifDriver.getIfd0().put(ExifDriver.TAG_EXIF_VERSION, value);
			}
			if (tag_metering_mode != null && !tag_metering_mode.equals("")
					&& Integer.parseInt(tag_metering_mode) <= 255)
			{
				ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT,
						Integer.parseInt(tag_metering_mode));
				exifDriver.getIfdExif().put(ExifDriver.TAG_METERING_MODE, value);
				exifDriver.getIfd0().put(ExifDriver.TAG_METERING_MODE, value);
			} else
			{
				ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT, 0);
				exifDriver.getIfdExif().put(ExifDriver.TAG_METERING_MODE, value);
				exifDriver.getIfd0().put(ExifDriver.TAG_METERING_MODE, value);
			}

			ValueNumber xValue = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_LONG, x);
			exifDriver.getIfdExif().put(ExifDriver.TAG_IMAGE_WIDTH, xValue);

			ValueNumber yValue = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_LONG, y);
			exifDriver.getIfdExif().put(ExifDriver.TAG_IMAGE_HEIGHT, yValue);

			String dateString = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").format(new Date());
			if (dateString != null)
			{
				ValueByteArray value = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
				// Date string length is 19 bytes. But exif tag
				// specification length is 20 bytes.
				// That's why we add "empty" byte (0x00) in the end.
				byte[] bytes = dateString.getBytes();
				byte[] res = new byte[20];
				for (int ii = 0; ii < bytes.length; ii++)
				{
					res[ii] = bytes[ii];
				}
				res[19] = 0x00;
				value.setBytes(res);
				exifDriver.getIfd0().put(ExifDriver.TAG_DATETIME, value);
				exifDriver.getIfdExif().put(ExifDriver.TAG_DATETIME_DIGITIZED, value);
				exifDriver.getIfdExif().put(ExifDriver.TAG_DATETIME_ORIGINAL, value);
			}

			// extract mode name
			String tag_modename = getFromSharedMem("mode_name" + Long.toString(sessionID));
			if (tag_modename == null)
				tag_modename = "";
			String softwareString = getResources().getString(R.string.app_name) + ", " + tag_modename;
			ValueByteArray softwareValue = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
			softwareValue.setBytes(softwareString.getBytes());
			exifDriver.getIfd0().put(ExifDriver.TAG_SOFTWARE, softwareValue);

			if (enableExifTagOrientation)
			{
				ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT, exif_orientation);
				exifDriver.getIfd0().put(ExifDriver.TAG_ORIENTATION, value);
			} else
			{
				ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT, ExifInterface.ORIENTATION_NORMAL);
				exifDriver.getIfd0().put(ExifDriver.TAG_ORIENTATION, value);
			}

			// Save exif info to new file, and replace old file with new
			// one.
			File modifiedFile = new File(file.getAbsolutePath() + ".tmp");
			exifDriver.save(modifiedFile.getAbsolutePath());
			return modifiedFile;
		}
		return null;
	}

	protected void addTimestamp(File file, int exif_orientation)
	{
		try
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

			int dateFormat = Integer.parseInt(prefs.getString(ApplicationScreen.sTimestampDate, "0"));
			boolean abbreviation = prefs.getBoolean(ApplicationScreen.sTimestampAbbreviation, false);
			int saveGeo = Integer.parseInt(prefs.getString(ApplicationScreen.sTimestampGeo, "0"));
			int timeFormat = Integer.parseInt(prefs.getString(ApplicationScreen.sTimestampTime, "0"));
			int separator = Integer.parseInt(prefs.getString(ApplicationScreen.sTimestampSeparator, "0"));
			String customText = prefs.getString(ApplicationScreen.sTimestampCustomText, "");
			int color = Integer.parseInt(prefs.getString(ApplicationScreen.sTimestampColor, "1"));
			int fontSizeC = Integer.parseInt(prefs.getString(ApplicationScreen.sTimestampFontSize, "80"));

			String formattedCurrentDate = "";
			if (dateFormat == 0 && timeFormat == 0 && customText.equals("") && saveGeo == 0)
				return;

			String geoText = "";
			// show geo data on time stamp
			if (saveGeo != 0)
			{
				Location l = MLocation.getLocation(getApplicationContext());

				if (l != null)
				{
					if (saveGeo == 2)
					{
						Geocoder geocoder = new Geocoder(MainScreen.getMainContext(), Locale.getDefault());
						List<Address> list = geocoder.getFromLocation(l.getLatitude(), l.getLongitude(), 1);
						if (!list.isEmpty())
						{
							String country = list.get(0).getCountryName();
							String locality = list.get(0).getLocality();
							String adminArea = list.get(0).getSubAdminArea();// city
																				// localized
							String street = list.get(0).getThoroughfare();// street
																			// localized
							String address = list.get(0).getAddressLine(0);

							// replace street and city with localized name
							if (street != null)
								address = street;
							if (adminArea != null)
								locality = adminArea;

							geoText = (country != null ? country : "") + (locality != null ? (", " + locality) : "")
									+ (address != null ? (", \n" + address) : "");
							
							if (geoText.equals(""))
								geoText = "lat:" + l.getLatitude() + "\nlng:" + l.getLongitude();
						}
					} else
						geoText = "lat:" + l.getLatitude() + "\nlng:" + l.getLongitude();
				}
			}

			String dateFormatString = "";
			String timeFormatString = "";
			String separatorString = ".";
			String monthString = abbreviation ? "MMMM" : "MM";

			switch (separator)
			{
			case 0:
				separatorString = "/";
				break;
			case 1:
				separatorString = ".";
				break;
			case 2:
				separatorString = "-";
				break;
			case 3:
				separatorString = " ";
				break;
			default:
				separatorString = " ";
			}

			switch (dateFormat)
			{
			case 1:
				dateFormatString = "yyyy" + separatorString + monthString + separatorString + "dd";
				break;
			case 2:
				dateFormatString = "dd" + separatorString + monthString + separatorString + "yyyy";
				break;
			case 3:
				dateFormatString = monthString + separatorString + "dd" + separatorString + "yyyy";
				break;
			default:
			}

			switch (timeFormat)
			{
			case 1:
				timeFormatString = " hh:mm:ss a";
				break;
			case 2:
				timeFormatString = " HH:mm:ss";
				break;
			default:
			}

			Date currentDate = Calendar.getInstance().getTime();
			java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat(dateFormatString
					+ timeFormatString);
			formattedCurrentDate = simpleDateFormat.format(currentDate);

			formattedCurrentDate += (customText.isEmpty() ? "" : ("\n" + customText))
					+ (geoText.isEmpty() ? "" : ("\n" + geoText));

			if (formattedCurrentDate.equals(""))
				return;

			Bitmap sourceBitmap;
			Bitmap bitmap;

			int rotation = 0;
			Matrix matrix = new Matrix();
			if (exif_orientation == ExifInterface.ORIENTATION_ROTATE_90)
			{
				rotation = 90;
			} else if (exif_orientation == ExifInterface.ORIENTATION_ROTATE_180)
			{
				rotation = 180;
			} else if (exif_orientation == ExifInterface.ORIENTATION_ROTATE_270)
			{
				rotation = 270;
			}
			matrix.postRotate(rotation);

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inMutable = true;

			sourceBitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
			bitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix,
					false);

			sourceBitmap.recycle();

			int width = bitmap.getWidth();
			int height = bitmap.getHeight();

			Paint p = new Paint();

			Canvas canvas = new Canvas(bitmap);

			final float scale = getResources().getDisplayMetrics().density;

			p.setColor(Color.WHITE);
			switch (color)
			{
			case 0:
				color = Color.BLACK;
				p.setColor(Color.BLACK);
				break;
			case 1:
				color = Color.WHITE;
				p.setColor(Color.WHITE);
				break;
			case 2:
				color = Color.YELLOW;
				p.setColor(Color.YELLOW);
				break;

			}

			if (width > height)
			{
				p.setTextSize(height / fontSizeC * scale + 0.5f); // convert dps
																	// to pixels
			} else
			{
				p.setTextSize(width / fontSizeC * scale + 0.5f); // convert dps
																	// to pixels
			}
			p.setTextAlign(Align.RIGHT);
			drawTextWithBackground(canvas, p, formattedCurrentDate, color, Color.BLACK, width, height);

			Matrix matrix2 = new Matrix();
			matrix2.postRotate(360 - rotation);
			sourceBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix2, false);

			bitmap.recycle();

			FileOutputStream outStream;
			outStream = new FileOutputStream(file);
			sourceBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outStream);
			sourceBitmap.recycle();
			outStream.flush();
			outStream.close();
		} catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e)
		{
			e.printStackTrace();
		} catch (OutOfMemoryError e)
		{
			e.printStackTrace();
		}
	}

	protected void drawTextWithBackground(Canvas canvas, Paint paint, String text, int foreground, int background,
			int imageWidth, int imageHeight)
	{
		Rect text_bounds = new Rect();
		paint.setColor(foreground);
		String[] resText = text.split("\n");
		String maxLengthText = "";

		if (resText.length > 1)
		{
			maxLengthText = resText[0].length() > resText[1].length() ? resText[0] : resText[1];
		} else if (resText.length > 0)
		{
			maxLengthText = resText[0];
		}

		final float scale = getResources().getDisplayMetrics().density;
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(background);
		paint.setAlpha(64);
		paint.getTextBounds(text, 0, maxLengthText.length(), text_bounds);
		final int padding = (int) (2 * scale + 0.5f); // convert dps to pixels

		int textWidth = 0;
		int textHeight = text_bounds.bottom - text_bounds.top;
		if (paint.getTextAlign() == Paint.Align.RIGHT || paint.getTextAlign() == Paint.Align.CENTER)
		{
			float width = paint.measureText(maxLengthText); // n.b., need to use
															// measureText
															// rather than
															// getTextBounds
															// here
			textWidth = (int) width;
		}

		text_bounds.left = imageWidth - textWidth - resText.length * padding;
		text_bounds.right = imageWidth - padding;
		if (resText.length > 1)
		{
			text_bounds.top = imageHeight - resText.length * padding - resText.length * textHeight - textHeight;
		} else
		{
			text_bounds.top = imageHeight - 2 * padding - textHeight;
			textHeight /= 3;
		}
		text_bounds.bottom = imageHeight - padding;

		paint.setColor(foreground);
		if (resText.length > 0)
		{
			canvas.drawText(resText[0], imageWidth - 5 * padding, imageHeight - resText.length * textHeight
					- textHeight / 2, paint);
		}
		if (resText.length > 1)
		{
			canvas.drawText(resText[1], imageWidth - 5 * padding, imageHeight - (resText.length - 1) * textHeight,
					paint);
		}
		if (resText.length > 2)
		{
			canvas.drawText(resText[2], imageWidth - 5 * padding, imageHeight - (resText.length - 2) * textHeight
					+ textHeight / 2, paint);
		}
		if (resText.length > 3)
		{
			canvas.drawText(resText[3], imageWidth - 5 * padding, imageHeight - textHeight / 4, paint);
		}
	}

	private void rotateImage(File file, Matrix matrix)
	{
		try
		{
			Bitmap sourceBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
			Bitmap rotatedBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(),
					sourceBitmap.getHeight(), matrix, true);

			FileOutputStream outStream;
			outStream = new FileOutputStream(file);
			rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outStream);
			outStream.flush();
			outStream.close();
		} catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@TargetApi(21)
	private void saveDNGPicture(int frameNum, long sessionID, OutputStream os, int width, int height, int orientation,
			boolean cameraMirrored)
	{
		DngCreator creator = new DngCreator(CameraController.getCameraCharacteristics(), PluginManager.getInstance()
				.getFromRAWCaptureResults("captureResult" + frameNum + sessionID));
		byte[] frame = SwapHeap.SwapFromHeap(
				Integer.parseInt(getFromSharedMem("resultframe" + frameNum + Long.toString(sessionID))),
				Integer.parseInt(getFromSharedMem("resultframelen" + frameNum + Long.toString(sessionID))));

		ByteBuffer buff = ByteBuffer.allocateDirect(frame.length);
		buff.put(frame);

		int exif_orientation = ExifInterface.ORIENTATION_NORMAL;
		switch ((orientation + 360) % 360)
		{
		default:
		case 0:
			exif_orientation = ExifInterface.ORIENTATION_NORMAL;
			break;
		case 90:
			exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_270
					: ExifInterface.ORIENTATION_ROTATE_90;
			break;
		case 180:
			exif_orientation = ExifInterface.ORIENTATION_ROTATE_180;
			break;
		case 270:
			exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_90
					: ExifInterface.ORIENTATION_ROTATE_270;
			break;
		}

		try
		{
			creator.setOrientation(exif_orientation);
			creator.writeByteBuffer(os, new Size(width, height), buff, 0);
		} catch (IOException e)
		{
			creator.close();
			e.printStackTrace();
			Log.e("Open Camera", "saveDNGPicture error: " + e.getMessage());
		}

		creator.close();
	}

	protected static final String[]	MEMCARD_DIR_PATH		= new String[] { "/storage", "/mnt", "", "/storage",
			"/Removable", "/storage", "/storage", "", "/mnt", "/" };

	protected static final String[]	MEMCARD_DIR_PATH_NAMES	= new String[] { "sdcard1", "extSdCard", "external_sd",
			"external_SD", "MicroSD", "emulated", "sdcard0", "sdcard-ext", "sdcard-ext", "sdcard" };

	protected static final String[]	SAVEDIR_DIR_PATH_NAMES	= new String[] { "sdcard1/DCIM/", "extSdCard/DCIM/",
			"external_sd/DCIM/", "external_SD/DCIM/", "MicroSD/DCIM/", "emulated/0/DCIM/", "sdcard0/DCIM/",
			"sdcard-ext/DCIM/", "sdcard-ext/DCIM/", "sdcard/DCIM/" };

	// get file saving directory
	// toInternalMemory - should be true only if force save to internal
	public static File getSaveDir(boolean forceSaveToInternalMemory)
	{
		File dcimDir, saveDir = null, memcardDir;
		boolean usePhoneMem = true;

		String abcDir = "Camera";
		if (sortByData)
		{
			Calendar rightNow = Calendar.getInstance();
			abcDir = String.format("%tF", rightNow);
		}

		if (Integer.parseInt(saveToPreference) == 1)
		{
			dcimDir = Environment.getExternalStorageDirectory();

			for (int i = 0; i < SAVEDIR_DIR_PATH_NAMES.length; i++)
			{
				if (MEMCARD_DIR_PATH[i].isEmpty())
				{
					memcardDir = new File(dcimDir, MEMCARD_DIR_PATH_NAMES[i]);
					if (memcardDir.exists())
					{
						saveDir = new File(dcimDir, SAVEDIR_DIR_PATH_NAMES[i] + abcDir);
						usePhoneMem = false;
						break;
					}
				} else
				{
					memcardDir = new File(MEMCARD_DIR_PATH[i], MEMCARD_DIR_PATH_NAMES[i]);
					if (memcardDir.exists())
					{
						saveDir = new File(MEMCARD_DIR_PATH[i], SAVEDIR_DIR_PATH_NAMES[i] + abcDir);
						usePhoneMem = false;
						break;
					}
				}
			}
		} else if ((Integer.parseInt(saveToPreference) == 2))
		{
			if (sortByData)
			{
				saveDir = new File(saveToPath, abcDir);
			} else
			{
				saveDir = new File(saveToPath);
			}
			usePhoneMem = false;
		}

		if (usePhoneMem || forceSaveToInternalMemory) // phone memory (internal
														// sd card)
		{
			dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
			saveDir = new File(dcimDir, abcDir);
		}
		if (!saveDir.exists())
			saveDir.mkdirs();

		// if creation failed - try to switch to phone mem
		if (!saveDir.exists())
		{
			dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
			saveDir = new File(dcimDir, abcDir);

			if (!saveDir.exists())
				saveDir.mkdirs();
		}
		return saveDir;
	}

	// get file saving directory
	// toInternalMemory - should be true only if force save to internal
	@TargetApi(19)
	public static DocumentFile getSaveDirNew(boolean forceSaveToInternalMemory)
	{
		DocumentFile saveDir = null;
		boolean usePhoneMem = true;

		String abcDir = "Camera";
		if (sortByData)
		{
			Calendar rightNow = Calendar.getInstance();
			abcDir = String.format("%tF", rightNow);
		}

		int saveToValue = Integer.parseInt(saveToPreference);
		if (saveToValue == 1 || saveToValue == 2)
		{
			boolean canWrite = false;
			String uri = saveToPath;
			try
			{
				saveDir = DocumentFile.fromTreeUri(ApplicationScreen.instance, Uri.parse(uri));
			} catch (Exception e)
			{
				saveDir = null;
			}
			List<UriPermission> perms = ApplicationScreen.instance.getContentResolver().getPersistedUriPermissions();
			for (UriPermission p : perms)
			{
				if (p.getUri().toString().equals(uri.toString()) && p.isWritePermission())
				{
					canWrite = true;
					break;
				}
			}

			if (saveDir != null && canWrite && saveDir.exists())
			{
				if (sortByData)
				{
					DocumentFile dateFolder = saveDir.findFile(abcDir);
					if (dateFolder == null)
					{
						dateFolder = saveDir.createDirectory(abcDir);
					}
					saveDir = dateFolder;
				}
				usePhoneMem = false;
			}
		}

		if (usePhoneMem || forceSaveToInternalMemory) // phone memory (internal
														// sd card)
		{
			saveDir = DocumentFile.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
			DocumentFile abcFolder = saveDir.findFile(abcDir);
			if (abcFolder == null || !abcFolder.exists())
			{
				abcFolder = saveDir.createDirectory(abcDir);
			}
			saveDir = abcFolder;
		}

		return saveDir;
	}

	public static String getExportFileName(String modeName)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		saveOption = Integer.parseInt(prefs.getString(ApplicationScreen.sExportNamePref, "2"));
		saveOptionSeparator = prefs.getBoolean(ApplicationScreen.sExportNameSeparatorPref, false);

		String prefix = prefs.getString(ApplicationScreen.sExportNamePrefixPref, "");
		if (!prefix.equals(""))
			prefix = prefix + "_";

		String postfix = prefs.getString(ApplicationScreen.sExportNamePostfixPref, "");
		if (!postfix.equals(""))
			postfix = "_" + postfix;

		Calendar d = Calendar.getInstance();
		String fileFormat = "";
		
		//if using separator in file name
		if (!saveOptionSeparator)
			fileFormat = String.format("%04d%02d%02d_%02d%02d%02d", d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1,
					d.get(Calendar.DAY_OF_MONTH), d.get(Calendar.HOUR_OF_DAY), d.get(Calendar.MINUTE),
					d.get(Calendar.SECOND));
		else
			fileFormat = String.format("%04d-%02d-%02d_%02d:%02d:%02d", d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1,
					d.get(Calendar.DAY_OF_MONTH), d.get(Calendar.HOUR_OF_DAY), d.get(Calendar.MINUTE),
					d.get(Calendar.SECOND));
		
		switch (saveOption)
		{
		case 1:// YEARMMDD_HHMMSS
			fileFormat = prefix + fileFormat + postfix;
			break;

		case 2:// YEARMMDD_HHMMSS_MODE
			fileFormat = prefix + fileFormat + (modeName.equals("") ? "" : ("_" + modeName)) + postfix;
			break;

		case 3:// IMG_YEARMMDD_HHMMSS
			fileFormat = prefix + "IMG_" + fileFormat + postfix;
			break;

		case 4:// IMG_YEARMMDD_HHMMSS_MODE
			fileFormat = prefix + "IMG_" + fileFormat + (modeName.equals("") ? "" : ("_" + modeName)) + postfix;
			break;
		default:
			break;
		}

		return fileFormat;
	}

	public void copyFromForceFileName(File dst) throws IOException
	{
		InputStream in = getContentResolver().openInputStream(ApplicationScreen.getForceFilenameURI());
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0)
		{
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	public void copyToForceFileName(File src) throws IOException
	{
		InputStream in = new FileInputStream(src);
		OutputStream out = getContentResolver().openOutputStream(ApplicationScreen.getForceFilenameURI());

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0)
		{
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	private String getFromSharedMem(String key)
	{
		return PluginManager.getInstance().getFromSharedMem(key);
	}
}
