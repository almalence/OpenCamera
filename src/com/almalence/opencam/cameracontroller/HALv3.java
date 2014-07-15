/*
	HALv3 for OpenCamera project - interface to camera2 device
    Copyright (C) 2014  Almalence Inc.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/* <!-- +++
package com.almalence.opencam_plus;
+++ --> */
// <!-- -+-
package com.almalence.opencam.cameracontroller;
//-+- -->

import java.util.ArrayList;
import java.util.List;

import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.util.Util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;

import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera.Area;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CameraAccessException;

import android.media.Image;
import android.media.ImageReader;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;

//HALv3 camera's objects
@SuppressLint("NewApi")
@TargetApi(19)
public class HALv3
{
	private static final String TAG = "HALv3Controller";
	
	private static HALv3 instance = null;
	
	private static Rect activeRect = null;
	private static Rect zoomCropPreview = null;
	private static Rect zoomCropCapture = null;
	private static float zoomLevel = 1f;
	private static int[] af_regions;
	private static int[] ae_regions;
	
	private static long exposureTime = 0;
	
	public static HALv3 getInstance()
	{
		if (instance == null)
		{
			instance = new HALv3();
		}
		return instance;
	}
	
	private CameraManager manager = null;
	private CameraCharacteristics camCharacter=null;
//	private cameraAvailableListener availListener = null;
	protected CameraDevice camDevice = null;
	private CaptureRequest.Builder previewRequestBuilder = null;
	
	private static boolean autoFocusTriggered = false;
	
	
	public static void onCreateHALv3()
	{
		// HALv3 code ---------------------------------------------------------
		// get manager for camera devices
		HALv3.getInstance().manager = (CameraManager)MainScreen.mainContext.getSystemService("camera"); // = Context.CAMERA_SERVICE;
		
		// get list of camera id's (usually it will contain just {"0", "1"}
		try {
			CameraController.getInstance().cameraIdList = HALv3.getInstance().manager.getCameraIdList();
		} catch (CameraAccessException e) {
			Log.d("MainScreen", "getCameraIdList failed");
			e.printStackTrace();
		}
	}
	
	public static void onPauseHALv3()
	{
		Log.e(TAG, "onPause");
		// HALv3 code -----------------------------------------
//		if ((HALv3.getInstance().availListener != null) && (HALv3.getInstance().manager != null))
//			HALv3.getInstance().manager.removeAvailabilityListener(HALv3.getInstance().availListener);
		
		if (null != HALv3.getInstance().camDevice)
		{
			HALv3.getInstance().camDevice.close();
			HALv3.getInstance().camDevice = null;
        }
		
//		if (HALv3.getInstance().camDevice != null)
//		{
//			try
//			{
//				HALv3.getInstance().camDevice.stopRepeating();
//				HALv3.getInstance().camDevice.waitUntilIdle();
//				HALv3.getInstance().camDevice.flush();
//				HALv3.getInstance().camDevice.close();
//				HALv3.getInstance().camDevice = null;
//			}
//			catch(CameraAccessException e)	
//			{
//				HALv3.getInstance().camDevice = null;
//				Log.e(TAG, "close camera device failed: " + e.getMessage());
//				e.printStackTrace();
//			}
//		}
	}
	
	
	public static void openCameraHALv3()
	{
		Log.e(TAG, "openCameraHALv3()");
		// HALv3 open camera -----------------------------------------------------------------
		if(HALv3.getCamera2() == null)
		{
			try
			{
				onCreateHALv3();
				Log.e(TAG, "try to manager.openCamera");
				HALv3.getInstance().manager.openCamera (CameraController.getInstance().cameraIdList[CameraController.CameraIndex], HALv3.getInstance().new openListener(), null);
			}
			catch (CameraAccessException e)
			{
				Log.e(TAG, "manager.openCamera failed: " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		try	{
			HALv3.getInstance().camCharacter = HALv3.getInstance().manager.getCameraCharacteristics(CameraController.getInstance().cameraIdList[CameraController.CameraIndex]);
		} catch (CameraAccessException e) {
			Log.e(TAG, "getCameraCharacteristics failed: " + e.getMessage());
			e.printStackTrace();
		}
		
		if (HALv3.getInstance().camCharacter.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
			CameraController.CameraMirrored = true;
		else
			CameraController.CameraMirrored = false;
		
		// Add an Availability Listener as Cameras become available or unavailable
//		HALv3.getInstance().availListener = HALv3.getInstance().new cameraAvailableListener();
//		HALv3.getInstance().manager.addAvailabilityListener(HALv3.getInstance().availListener, null);
		
		CameraController.getInstance().mVideoStabilizationSupported = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES) == null? false : true;
		
		// check that full hw level is supported
		if (HALv3.getInstance().camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) != CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) 
			MainScreen.H.sendEmptyMessage(PluginManager.MSG_NOT_LEVEL_FULL);
		
		//Get sensor size for zoom and focus/metering areas.
		activeRect = HALv3.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
		// ^^ HALv3 open camera -----------------------------------------------------------------
	}
	
	
	public static void setupImageReadersHALv3()
	{
		Log.e(TAG, "setupImageReadersHALv3()");
		//surfaceHolder.setFixedSize(MainScreen.imageWidth, MainScreen.imageHeight);
		
		MainScreen.thiz.surfaceHolder.setFixedSize(1280, 720);
		MainScreen.previewWidth = 1280;
		MainScreen.previewHeight = 720;
		
		//MainScreen.thiz.surfaceHolder.setFixedSize(MainScreen.imageWidth, MainScreen.imageHeight);
//		MainScreen.previewWidth = MainScreen.imageWidth;
//		MainScreen.previewHeight = MainScreen.imageHeight;
		
		// HALv3 code -------------------------------------------------------------------
		MainScreen.mImageReaderPreviewYUV = ImageReader.newInstance(MainScreen.previewWidth, MainScreen.previewHeight, ImageFormat.YUV_420_888, 1);
		MainScreen.mImageReaderPreviewYUV.setOnImageAvailableListener(HALv3.getInstance().new imageAvailableListener(), null);
		
		MainScreen.mImageReaderYUV = ImageReader.newInstance(MainScreen.imageWidth, MainScreen.imageHeight, ImageFormat.YUV_420_888, 1);
		MainScreen.mImageReaderYUV.setOnImageAvailableListener(HALv3.getInstance().new imageAvailableListener(), null);
		
		MainScreen.mImageReaderJPEG = ImageReader.newInstance(MainScreen.imageWidth, MainScreen.imageHeight, ImageFormat.JPEG, 1);
		MainScreen.mImageReaderJPEG.setOnImageAvailableListener(HALv3.getInstance().new imageAvailableListener(), null);
	}
	
	
	public static void populateCameraDimensionsHALv3()
	{
		CameraController.ResolutionsMPixList = new ArrayList<Long>();
		CameraController.ResolutionsSizeList = new ArrayList<CameraController.Size>();
		CameraController.ResolutionsIdxesList = new ArrayList<String>();
		CameraController.ResolutionsNamesList = new ArrayList<String>();
		
		int MinMPIX = CameraController.MIN_MPIX_SUPPORTED;
		CameraCharacteristics params = getCameraParameters2();
		android.hardware.camera2.Size[] cs = params.get(CameraCharacteristics.SCALER_AVAILABLE_PROCESSED_SIZES);

		CharSequence[] RatioStrings = { " ", "4:3", "3:2", "16:9", "1:1" };

		int iHighestIndex = 0;		
		android.hardware.camera2.Size sHighest = cs[iHighestIndex];
		
		int ii = 0;
		for(android.hardware.camera2.Size s : cs)
		{
			int currSizeWidth = s.getWidth();
			int currSizeHeight = s.getHeight();
			int highestSizeWidth = sHighest.getWidth();
			int highestSizeHeight = sHighest.getHeight();
			
			if ((long) currSizeWidth * currSizeHeight > (long) highestSizeWidth * highestSizeHeight)
			{
				sHighest = s;
				iHighestIndex = ii;
			}

			if ((long) currSizeWidth * currSizeHeight < MinMPIX)
				continue;
			
			CameraController.fillResolutionsList(ii, currSizeWidth, currSizeHeight);		
			
			ii++;
		}

		if (CameraController.ResolutionsNamesList.size() == 0) {
			android.hardware.camera2.Size s = cs[iHighestIndex];
			
			int currSizeWidth = s.getWidth();
			int currSizeHeight = s.getHeight();
			
			CameraController.fillResolutionsList(0, currSizeWidth, currSizeHeight);
		}

		return;
	}
	
	
	public static void fillPreviewSizeList(List<CameraController.Size> previewSizes)
	{
		android.hardware.camera2.Size[] cs = HALv3.getInstance().camCharacter.get(CameraCharacteristics.SCALER_AVAILABLE_PROCESSED_SIZES);
		for(android.hardware.camera2.Size sz : cs)
			previewSizes.add(CameraController.getInstance().new Size(sz.getWidth(), sz.getHeight()));
	}
	
	public static void fillPictureSizeList(List<CameraController.Size> pictureSizes)
	{
		android.hardware.camera2.Size[] cs = HALv3.getInstance().camCharacter.get(CameraCharacteristics.SCALER_AVAILABLE_PROCESSED_SIZES);
		for(android.hardware.camera2.Size sz : cs)
			pictureSizes.add(CameraController.getInstance().new Size(sz.getWidth(), sz.getHeight()));
	}
	
	
	public static CameraDevice getCamera2()
	{
		return HALv3.getInstance().camDevice;
	}	
	
	public static CameraCharacteristics getCameraParameters2()
	{
		if (HALv3.getInstance().camCharacter != null)
			return HALv3.getInstance().camCharacter;

		return null;
	}
	
	
	
	
	// Camera parameters interface
	public static boolean isZoomSupportedHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			float maxzoom = HALv3.getInstance().camCharacter.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
			return maxzoom > 0? true : false;
		}
		
		return false;			
	}
	
	public static float getMaxZoomHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
			return HALv3.getInstance().camCharacter.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)*10.0f;			
		
		return 0;
	}
	
	public static void setZoom(float newZoom)
	{
		if(newZoom < 1f)
		{
			zoomLevel = 1f;
			return;
		}
		zoomLevel = newZoom;
		zoomCropPreview = getZoomRect(zoomLevel, activeRect.width(), activeRect.height());
		HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomCropPreview);
		try 
		{
			CameraController.iCaptureID = HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
		}
		catch (CameraAccessException e)
		{
			e.printStackTrace();
		}
	}
	
	public static Rect getZoomRect(float zoom, int imgWidth, int imgHeight)
	{
		int CropWidth  = (int)(imgWidth/zoom)+2*64;
		int CropHeight = (int)(imgHeight/zoom)+2*64;
		// ensure crop w,h divisible by 4 (SZ requirement)
		CropWidth  -= CropWidth&3;
		CropHeight -= CropHeight&3;
		// crop area for standard frame
		int CropWidthStd  = CropWidth-2*64;
		int CropHeightStd = CropHeight-2*64;
		
		return new Rect((imgWidth-CropWidthStd)/2, (imgHeight-CropHeightStd)/2,
				(imgWidth+CropWidthStd)/2, (imgHeight+CropHeightStd)/2);
	}
	
	public static boolean isExposureCompensationSupportedHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			int expRange[] = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
			return expRange[1] == expRange[0] ? false : true;
		}
		
		return false;	
	}
	
	public static int getMinExposureCompensationHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			int expRange[] = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
			return expRange[0];
		}
		
		return 0;
	}
	
	public static int getMaxExposureCompensationHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			int expRange[] = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
			return expRange[1];
		}
		
		return 0;
	}

	
	public static float getExposureCompensationStepHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			float step = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP).toFloat();
			return step;
		}
		
		return 0;	
	}

	
	public static void resetExposureCompensationHALv3()
	{
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
	}
	

	public static byte[] getSupportedSceneModesHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			byte scenes[]  = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
			if(scenes.length > 0 && scenes[0] != CameraCharacteristics.CONTROL_SCENE_MODE_UNSUPPORTED)
				return scenes;				
		}
		
		return null;
	}
	

	public static byte[] getSupportedWhiteBalanceHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			byte wb[]  = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
			if(wb.length > 0 )
				return wb;				
		}
		
		return null;
	}
	

	public static byte[] getSupportedFocusModesHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			byte focus[]  = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
			if(focus.length > 0 )
				return focus;				
		}
		
		return null;
	}
	
	
	public static boolean isFlashModeSupportedHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			return HALv3.getInstance().camCharacter.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == 1? true : false;						
		}
		
		return false;
	}
	
	
	public static byte[] getSupportedISOModesHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			int iso[]  = HALv3.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
			int max_analog_iso = HALv3.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_MAX_ANALOG_SENSITIVITY);
			int max_iso = iso[1];
			
			int index = 0;
			for(index = 0; index < CameraController.getIsoValuesList().size(); index++)
			{
				if(max_iso <= CameraController.getIsoValuesList().get(index))
				{
					++index;
					break;
				}
			}
			byte[] iso_values = new byte[index];
			for(int i = 0; i < index; i++)
				iso_values[i] = CameraController.getIsoValuesList().get(i).byteValue();

			if(iso_values.length > 0 )
				return iso_values;				
		}
		
		return null;
	}
	
	public static boolean isISOModeSupportedHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			int iso[]  = HALv3.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
			if(iso[0] == iso[1])
				return false;
			return true;
		}
		
		return false;
	}
	
	public static int getMaxNumMeteringAreasHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			return HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_MAX_REGIONS);
		}
		
		return 0;
	}
	
	public static int getMaxNumFocusAreasHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			return HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_MAX_REGIONS);						
		}
		
		return 0;
	}
	
	
	
	
	
	
	public static void setCameraSceneModeHALv3(int mode)
	{
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, mode);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
		
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putInt(MainScreen.sSceneModePref, mode).commit();
	}
	
	public static void setCameraWhiteBalanceHALv3(int mode)
	{
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, mode);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
		
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putInt(MainScreen.sWBModePref, mode).commit();
	}
	

	public static void setCameraFocusModeHALv3(int mode)
	{
		Log.e(TAG, "setCameraFocusModeHALv3 start = " + mode);
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{	
			Log.e(TAG, "setCameraFocusModeHALv3 = " + mode);
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, mode);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
		
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, mode).commit();
	}
	
	
	public static void setCameraFlashModeHALv3(int mode)
	{
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.FLASH_MODE, mode);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
		
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putInt(MainScreen.sFlashModePref, mode).commit();
	}
	
	public static void setCameraISOModeHALv3(int mode)
	{
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{
			if(mode != 1)
				HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, CameraController.getIsoModeHALv3().get(mode));
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
		
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putInt(MainScreen.sISOPref, mode).commit();
	}
	
	
	public static void setCameraExposureCompensationHALv3(int iEV)
	{
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, iEV);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
		
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putInt(MainScreen.sEvPref, iEV).commit();
	}
	
	public static void setCameraFocusAreasHALv3(List<Area> focusAreas)
	{
		Rect zoomRect = getZoomRect(zoomLevel, activeRect.width(), activeRect.height());
		if(focusAreas != null)
		{
			af_regions = new int[5*focusAreas.size()];
			for(int i = 0; i < focusAreas.size(); i++)
			{
				Rect r = focusAreas.get(i).rect;
				Log.e(TAG, "focusArea: " + r.left + " " + r.top + " " + r.right + " " + r.bottom);
				
				Matrix matrix = new Matrix();
				matrix.setScale(1,1);
		        matrix.preTranslate(1000.0f, 1000.0f);
		        matrix.postScale((zoomRect.width()-1)/2000.0f, (zoomRect.height()-1)/2000.0f);	        
		        
		        RectF rectF = new RectF(r.left, r.top, r.right, r.bottom);
		        matrix.mapRect(rectF);
		        Util.rectFToRect(rectF, r);
		        Log.e(TAG, "focusArea after matrix: " + r.left + " " + r.top + " " + r.right + " " + r.bottom);
		        
		        int currRegion = i*5;
		        af_regions[currRegion] = r.left;
		        af_regions[currRegion+1] = r.top;
		        af_regions[currRegion+2] = r.right;
		        af_regions[currRegion+3] = r.bottom;
		        af_regions[currRegion+4] = 10;
//		        af_regions = new int[5];
//				af_regions[0] = 0;
//		        af_regions[1] = 0;
//		        af_regions[2] = activeRect.width()-1;
//		        af_regions[3] = activeRect.height()-1;
//		        af_regions[4] = 10;
			}
		}
		else
		{
			af_regions = new int[5];
			af_regions[0] = 0;
	        af_regions[1] = 0;
	        af_regions[2] = activeRect.width()-1;
	        af_regions[3] = activeRect.height()-1;
	        af_regions[4] = 0;
		}
		Log.e(TAG, "activeRect: " + activeRect.left + " " + activeRect.top + " " + activeRect.right + " " + activeRect.bottom);
		Log.e(TAG, "zoomRect: " + zoomRect.left + " " + zoomRect.top + " " + zoomRect.right + " " + zoomRect.bottom);
		
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, af_regions);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
		
	}
	
	public static void setCameraMeteringAreasHALv3(List<Area> meteringAreas)
	{
		Rect zoomRect = getZoomRect(zoomLevel, activeRect.width(), activeRect.height());
		if(meteringAreas != null)
		{
			ae_regions = new int[5*meteringAreas.size()];
			for(int i = 0; i < meteringAreas.size(); i++)
			{
				Rect r = meteringAreas.get(i).rect;
				
				Matrix matrix = new Matrix();
				matrix.setScale(1,1);		        
		        matrix.preTranslate(1000.0f, 1000.0f);
		        matrix.postScale((zoomRect.width()-1)/2000.0f, (zoomRect.height()-1)/2000.0f);	        
		        
		        RectF rectF = new RectF(r.left, r.top, r.right, r.bottom);
		        matrix.mapRect(rectF);
		        Util.rectFToRect(rectF, r);
		        
		        int currRegion = i*5;
		        ae_regions[currRegion] = r.left;
		        ae_regions[currRegion+1] = r.top;
		        ae_regions[currRegion+2] = r.right;
		        ae_regions[currRegion+3] = r.bottom;
		        ae_regions[currRegion+4] = 10;
			}
		}
		else
		{
			ae_regions = new int[5];
			ae_regions[0] = 0;
	        ae_regions[1] = 0;
	        ae_regions[2] = activeRect.width()-1;
	        ae_regions[3] = activeRect.height()-1;
	        ae_regions[4] = 0;
		}
		
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, ae_regions);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}		
	}
	
	public static int getPreviewFrameRateHALv3()
	{
		int range[] = {0 , 0};
		range = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
		return range[range.length-1]; 
	}
	
	
	
	
	
	
	public static int captureImageHALv3(int nFrames, int format)
	{
	// stop preview
	//		try {
	//			HALv3.getInstance().camDevice.stopRepeating();
	//		} catch (CameraAccessException e1) {
	//			Log.e("MainScreen", "Can't stop preview");
	//			e1.printStackTrace();
	//		}
			
		// create capture requests for the burst of still images
		//Log.e("CameraController", "captureImage 1");
		int requestID = -1;
		CaptureRequest.Builder stillRequestBuilder = null;
		try
		{
			stillRequestBuilder = HALv3.getInstance().camDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
			stillRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
			stillRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
			stillRequestBuilder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_HIGH_QUALITY);
			if(zoomLevel >= 1.0f)
			{
				zoomCropCapture = getZoomRect(zoomLevel, activeRect.width(), activeRect.height());
				stillRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomCropCapture);
			}
			
			// no re-focus needed, already focused in preview, so keeping the same focusing mode for snapshot
			//stillRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
			// Google: note: CONTROL_AF_MODE_OFF causes focus to move away from current position 
			//stillRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
	//					Log.e("CameraController", "captureImage 2");
			if(format == CameraController.JPEG)
			{
				Log.e("HALv3", "Capture " + nFrames + " JPEGs");
				stillRequestBuilder.addTarget(MainScreen.mImageReaderJPEG.getSurface());
	//						Log.e("CameraController", "captureImage 3.1");
			}
			else
			{
				Log.e("HALv3", "Capture " + nFrames + " YUV");
				stillRequestBuilder.addTarget(MainScreen.mImageReaderYUV.getSurface());
	//						Log.e("CameraController", "captureImage 3.2");
			}
	
			// Google: throw: "Burst capture implemented yet", when to expect implementation?
			/*
			List<CaptureRequest> requests = new ArrayList<CaptureRequest>();
			for (int n=0; n<NUM_FRAMES; ++n)
				requests.add(stillRequestBuilder.build());
			
			camDevice.captureBurst(requests, new captureListener() , null);
			*/
			
			// requests for SZ input frames
			for (int n=0; n< nFrames; ++n)
			{
				requestID = HALv3.getInstance().camDevice.capture(stillRequestBuilder.build(), HALv3.getInstance().new captureListener() , null);
			}
	//					Log.e("CameraController", "captureImage 4");				
			// One more capture for comparison with a standard frame
	//			stillRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
	//			stillRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
	//			// set crop area for the scaler to have interpolation applied by camera HW
	//			stillRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomCrop);
	//			camDevice.capture(stillRequestBuilder.build(), new captureListener() , null);
		}
		catch (CameraAccessException e)
		{
			Log.e("MainScreen", "setting up still image capture request failed");
			e.printStackTrace();
			throw new RuntimeException();
		}
		
		return requestID;
	}
	
	
	public static int captureImageWithParamsHALv3(final int nFrames, final int format, final int pause, final int[] evRequested)
	{
		int requestID = -1;
		final CaptureRequest.Builder stillRequestBuilder;
		try
		{
			stillRequestBuilder = HALv3.getInstance().camDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
			stillRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
			stillRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
			stillRequestBuilder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_HIGH_QUALITY);
			if(zoomLevel >= 1.0f)
			{
				zoomCropCapture = getZoomRect(zoomLevel, activeRect.width(), activeRect.height());
				stillRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomCropCapture);
			}
			
			// no re-focus needed, already focused in preview, so keeping the same focusing mode for snapshot
			//stillRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
			// Google: note: CONTROL_AF_MODE_OFF causes focus to move away from current position 
			//stillRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
	//					Log.e("CameraController", "captureImage 2");
			if(format == CameraController.JPEG)
			{
				Log.e("HALv3", "Capture " + nFrames + " JPEGs");
				stillRequestBuilder.addTarget(MainScreen.mImageReaderJPEG.getSurface());
	//						Log.e("CameraController", "captureImage 3.1");
			}
			else
			{
				Log.e("HALv3", "Capture " + nFrames + " YUVs");
				stillRequestBuilder.addTarget(MainScreen.mImageReaderYUV.getSurface());
	//						Log.e("CameraController", "captureImage 3.2");
			}
	
			// Google: throw: "Burst capture implemented yet", when to expect implementation?
			/*
			List<CaptureRequest> requests = new ArrayList<CaptureRequest>();
			for (int n=0; n<NUM_FRAMES; ++n)
				requests.add(stillRequestBuilder.build());
			
			camDevice.captureBurst(requests, new captureListener() , null);
			*/
			
			HALv3.getInstance().camDevice.stopRepeating();
			// requests for SZ input frames
			if(pause > 0)
			{
				new CountDownTimer(pause, nFrames*pause)
				{
					int index = 0;
					public void onTick(long millisUntilFinished)
					{
						Log.e(TAG, "onTick " + index + " millisUntilFinished =  " + millisUntilFinished);
						if(evRequested != null && evRequested.length > index)
						{
							stillRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, evRequested[index]);
//							setCameraExposureCompensationHALv3(evRequested[index]);
						}
					 
						try
						{
							HALv3.getInstance().camDevice.capture(stillRequestBuilder.build(), HALv3.getInstance().new captureListener() , null);
						}
						catch (CameraAccessException e)
						{
							e.printStackTrace();
						}
						
						index++;
					}
					 
					public void onFinish()
					{	
						Log.e(TAG, "onFinish index = " + index);
						if(evRequested != null && evRequested.length > index)
						{
							stillRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, evRequested[index]);
//							setCameraExposureCompensationHALv3(evRequested[index]);
						}
					 
						try
						{
							HALv3.getInstance().camDevice.capture(stillRequestBuilder.build(), HALv3.getInstance().new captureListener() , null);
						}
						catch (CameraAccessException e)
						{
							e.printStackTrace();
						}
					}
				}.start();
			
			}
			else
			{
				if(evRequested != null && evRequested.length >= nFrames)
				{
//					for (int n=0; n<nFrames; ++n)
//					{
//						//stillRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, evRequested[n]);
//						long expTime = n == 1 ? exposureTime/10 : n == 2 ? exposureTime*10 : exposureTime;
////						Log.e(TAG, "Exposure time = " + expTime);
//						stillRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
//						stillRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expTime);
//						requestID = HALv3.getInstance().camDevice.capture(stillRequestBuilder.build(), HALv3.getInstance().new captureListener() , null);					
//					}
					
					stillRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, evRequested[0]);
//					setCameraExposureCompensationHALv3(evRequested[0]);
//					final long expt = exposureTime;
//					stillRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
//					stillRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expt);
					Log.e(TAG, "evRequested != null");
					new CountDownTimer(500*nFrames, 500)
					{
						int index = 1;
						public void onTick(long millisUntilFinished)
						{
							if(index >= nFrames)
								return;
							
							Log.e(TAG, "onTick " + index + " millisUntilFinished =  " + millisUntilFinished);
							if(evRequested != null && evRequested.length > index)
							{
//								long expTime = index == 2 ? expt/2 : index == 3 ? expt*2 : expt;
//								Log.e(TAG, "Exp Time = " + expTime);
//								stillRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
//								stillRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expTime);
								
								stillRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, evRequested[index]);
//								setCameraExposureCompensationHALv3(evRequested[index]);
							}
						 
							try
							{
								HALv3.getInstance().camDevice.capture(stillRequestBuilder.build(), HALv3.getInstance().new captureListener() , null);
							}
							catch (CameraAccessException e)
							{
								e.printStackTrace();
							}
							
							index++;
						}
						 
						public void onFinish()
						{
							if(index > nFrames)
								return;
							
							Log.e(TAG, "onFinish index = " + index);
							if(evRequested != null && evRequested.length > index)
							{
//								long expTime = index == 2 ? expt/2 : index == 3 ? expt*2 : expt;
//								
//								Log.e(TAG, "Exp Time = " + expTime);
//								
//								stillRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
//								stillRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expTime);
								
								stillRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, evRequested[index]);
//								setCameraExposureCompensationHALv3(evRequested[index]);
							}
						 
							try
							{
								HALv3.getInstance().camDevice.capture(stillRequestBuilder.build(), HALv3.getInstance().new captureListener() , null);
							}
							catch (CameraAccessException e)
							{
								e.printStackTrace();
							}
						}
					}.start();
					Log.e(TAG, "CountDownTimer started");
				}
				else
				{
					for (int n=0; n<nFrames; ++n)
					{
//						if(evRequested != null && evRequested.length > n)
//						{
//							stillRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, evRequested[n]);
//							setCameraExposureCompensationHALv3(evRequested[n]);
//						}
						requestID = HALv3.getInstance().camDevice.capture(stillRequestBuilder.build(), HALv3.getInstance().new captureListener() , null);					
					}
				}
			}
			
//			requestID = HALv3.getInstance().camDevice.captureBurst(requestList, HALv3.getInstance().new captureListener() , null);
				//requestID = HALv3.getInstance().camDevice.capture(stillRequestBuilder.build(), HALv3.getInstance().new captureListener() , null);
	//					Log.e("CameraController", "captureImage 4");				
			// One more capture for comparison with a standard frame
	//			stillRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
	//			stillRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
	//			// set crop area for the scaler to have interpolation applied by camera HW
	//			stillRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomCrop);
	//			camDevice.capture(stillRequestBuilder.build(), new captureListener() , null);
		}
		catch (CameraAccessException e)
		{
			Log.e(TAG, "setting up still image capture request failed");
			e.printStackTrace();
			throw new RuntimeException();
		}
		
		return requestID;
	}
	
	
	public static boolean autoFocusHALv3()
	{
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
//			if(af_regions != null)
//				HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, af_regions);
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraCharacteristics.CONTROL_AF_TRIGGER_START);
			try 
			{	
				Log.e(TAG, "autoFocusHALv3. CaptureRequest.CONTROL_AF_TRIGGER, CameraCharacteristics.CONTROL_AF_TRIGGER_START");
				CameraController.iCaptureID = HALv3.getInstance().camDevice.capture(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new focusListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
				return false;
			}
			
			HALv3.autoFocusTriggered = true;
			
			return true;
		}
		
		return false;
	}
	
	public static void cancelAutoFocusHALv3()
	{
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraCharacteristics.CONTROL_AF_TRIGGER_CANCEL);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.capture(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
			
			HALv3.autoFocusTriggered = false;
		}	
	}
	
	
	
	public void configurePreviewRequest() throws CameraAccessException
	{
		Log.e(TAG, "configurePreviewRequest()");
		HALv3.getInstance().previewRequestBuilder = HALv3.getInstance().camDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
		HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
		HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.REQUEST_ID, 555);
		HALv3.getInstance().previewRequestBuilder.addTarget(MainScreen.thiz.getCameraSurface());
		HALv3.getInstance().previewRequestBuilder.addTarget(MainScreen.thiz.getPreviewYUVSurface());
		HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);		
	}
	
	
	// HALv3 ------------------------------------------------ camera-related listeners

		// Note: never received onCameraAvailable notifications, only onCameraUnavailable
		@SuppressLint("Override")
		@TargetApi(19)
		public class cameraAvailableListener extends CameraManager.AvailabilityListener
		{
			@Override
			public void onCameraAvailable(java.lang.String cameraId)
			{
				// should we call this?
				super.onCameraAvailable(cameraId);
				
				Log.d(TAG, "CameraManager.AvailabilityListener.onCameraAvailable");
			}
			
			@Override
			public void onCameraUnavailable(java.lang.String cameraId)
			{
				// should we call this?
				super.onCameraUnavailable(cameraId);
				
				Log.d(TAG, "CameraManager.AvailabilityListener.onCameraUnavailable");
			}
		}

		@SuppressLint("Override")
		@TargetApi(19)
		public class openListener extends CameraDevice.StateListener
		{
			@Override
			public void onDisconnected(CameraDevice arg0) {
				Log.e(TAG, "CameraDevice.StateListener.onDisconnected");
				if (HALv3.getInstance().camDevice != null)
				{
					try
					{
						HALv3.getInstance().camDevice.stopRepeating();
						HALv3.getInstance().camDevice.waitUntilIdle();
						HALv3.getInstance().camDevice.flush();
						HALv3.getInstance().camDevice.close();
						HALv3.getInstance().camDevice = null;
					}
					catch(CameraAccessException e)	
					{
						HALv3.getInstance().camDevice = null;
						Log.e(TAG, "close camera device failed: " + e.getMessage());
						e.printStackTrace();
					}
				}
			}

			@Override
			public void onError(CameraDevice arg0, int arg1) {
				Log.e(TAG, "CameraDevice.StateListener.onError: "+arg1);
			}

			@Override
			public void onOpened(CameraDevice arg0)
			{
				Log.e(TAG, "CameraDevice.StateListener.onOpened");

				HALv3.getInstance().camDevice = arg0;
				
				MainScreen.H.sendEmptyMessage(PluginManager.MSG_CAMERA_OPENED);

				//dumpCameraCharacteristics();
			}
		}

		// Note: there other onCaptureXxxx methods in this listener which we do not implement
		@TargetApi(19)
		public class focusListener extends CameraDevice.CaptureListener
		{
			@Override
			public void onCapturePartial(CameraDevice camera, CaptureRequest request, CaptureResult result)
			{
				Log.e(TAG, "onFocusPartial. AF State = " + result.get(CaptureResult.CONTROL_AF_STATE));
			}
			@Override
			public void onCaptureCompleted(CameraDevice camera, CaptureRequest request, CaptureResult result)
			{
				PluginManager.getInstance().onCaptureCompleted(result);
				try
				{
//					HALv3.exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
//					Log.e(TAG, "EXPOSURE TIME = " + HALv3.exposureTime);
					Log.e(TAG, "onFocusCompleted. AF State = " + result.get(CaptureResult.CONTROL_AF_STATE));
					if(result.get(CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED && HALv3.autoFocusTriggered)
					{
						Log.e(TAG, "onFocusCompleted. CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED");
						resetCaptureListener();
						CameraController.getInstance().onAutoFocus(true);
						HALv3.autoFocusTriggered = false;
						
					}				
					else if(result.get(CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED && HALv3.autoFocusTriggered)
					{
						Log.e(TAG, "onFocusCompleted. CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED");
						resetCaptureListener();
						CameraController.getInstance().onAutoFocus(false);
						HALv3.autoFocusTriggered = false;
					}
					else if(result.get(CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN && HALv3.autoFocusTriggered)
					{
						Log.e(TAG, "onFocusCompleted. CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN");
//						resetCaptureListener();
//						CameraController.getInstance().onAutoFocus(false);
//						HALv3.autoFocusTriggered = false;
					}
				}
				catch(Exception e)
				{
					Log.e(TAG, "Exception: " + e.getMessage());					
				}
				
//				if(result.get(CaptureResult.REQUEST_ID) == iCaptureID)
//				{
//					//Log.e(TAG, "Image metadata received. Capture timestamp = " + result.get(CaptureResult.SENSOR_TIMESTAMP));
//					iPreviewFrameID = result.get(CaptureResult.SENSOR_TIMESTAMP);
//				}
				
				// Note: result arriving here is just image metadata, not the image itself
				// good place to extract sensor gain and other parameters

				// Note: not sure which units are used for exposure time (ms?)
//						currentExposure = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
//						currentSensitivity = result.get(CaptureResult.SENSOR_SENSITIVITY);
				
				//dumpCaptureResult(result);
			}
			
			private void resetCaptureListener()
			{
				if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
				{
					int focusMode = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, CameraParameters.AF_MODE_AUTO);
					HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, focusMode);
					HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraCharacteristics.CONTROL_AF_TRIGGER_CANCEL);
					try 
					{
						//HALv3.getInstance().camDevice.stopRepeating();
						CameraController.iCaptureID = HALv3.getInstance().camDevice.capture(HALv3.getInstance().previewRequestBuilder.build(), null, null);
					}
					catch (CameraAccessException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		
		@TargetApi(19)
		public class captureListener extends CameraDevice.CaptureListener
		{
			@Override
			public void onCaptureCompleted(CameraDevice camera, CaptureRequest request, CaptureResult result)
			{
				PluginManager.getInstance().onCaptureCompleted(result);
				try
				{
//					HALv3.exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
//					Log.e(TAG, "EXPOSURE TIME = " + HALv3.exposureTime);
					if(result.get(CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED && HALv3.autoFocusTriggered)
					{
						Log.e(TAG, "onCaptureCompleted. CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED");
						resetCaptureListener();
						CameraController.getInstance().onAutoFocus(true);
						HALv3.autoFocusTriggered = false;
						
					}				
					else if(result.get(CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED && HALv3.autoFocusTriggered)
					{
						Log.e(TAG, "onCaptureCompleted. CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED");
						resetCaptureListener();
						CameraController.getInstance().onAutoFocus(false);
						HALv3.autoFocusTriggered = false;
					}
					else if(result.get(CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN && HALv3.autoFocusTriggered)
					{
						Log.e(TAG, "onCaptureCompleted. CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN");
//						resetCaptureListener();
//						CameraController.getInstance().onAutoFocus(false);
//						HALv3.autoFocusTriggered = false;
					}
				}
				catch(Exception e)
				{
					Log.e(TAG, "Exception: " + e.getMessage());					
				}
				
//				if(result.get(CaptureResult.REQUEST_ID) == iCaptureID)
//				{
//					//Log.e(TAG, "Image metadata received. Capture timestamp = " + result.get(CaptureResult.SENSOR_TIMESTAMP));
//					iPreviewFrameID = result.get(CaptureResult.SENSOR_TIMESTAMP);
//				}
				
				// Note: result arriving here is just image metadata, not the image itself
				// good place to extract sensor gain and other parameters

				// Note: not sure which units are used for exposure time (ms?)
//						currentExposure = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
//						currentSensitivity = result.get(CaptureResult.SENSOR_SENSITIVITY);
				
				//dumpCaptureResult(result);
			}
			
			private void resetCaptureListener()
			{
				if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
				{
					int focusMode = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, CameraParameters.AF_MODE_AUTO);
					HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, focusMode);
					HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraCharacteristics.CONTROL_AF_TRIGGER_CANCEL);
					try 
					{
						//HALv3.getInstance().camDevice.stopRepeating();
						CameraController.iCaptureID = HALv3.getInstance().camDevice.capture(HALv3.getInstance().previewRequestBuilder.build(), null, null);
					}
					catch (CameraAccessException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		
		@TargetApi(19)
		public class imageAvailableListener implements ImageReader.OnImageAvailableListener
		{
			@Override
			public void onImageAvailable(ImageReader ir)
			{			
				// Contrary to what is written in Aptina presentation acquireLatestImage is not working as described
				// Google: Also, not working as described in android docs (should work the same as acquireNextImage in our case, but it is not)
				//Image im = ir.acquireLatestImage();
				
				Image im = ir.acquireNextImage();
				//if(iPreviewFrameID == im.getTimestamp())
				if(ir.getSurface() == CameraController.mPreviewSurface)
					PluginManager.getInstance().onPreviewAvailable(im);
				else
				{
					Log.e("HALv3", "onImageAvailable");
					PluginManager.getInstance().onImageAvailable(im);
				}

				// Image should be closed after we are done with it
				im.close();
			}
		}
			// ^^ HALv3 code -------------------------------------------------------------- camera-related listeners
}
