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

package com.almalence.plugins.export.standard;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.location.GpsStatus;
import android.location.Location;
import android.media.ExifInterface;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.almalence.SwapHeap;

/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginExport;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.R;
+++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginExport;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
//-+- -->

import com.almalence.util.MLocation;

/***
Implements simple export plugin - saves image to disc 
to specified location 
in specified format and 
with specified pattern name
***/

public class ExportPlugin extends PluginExport
{
	static public String[] filesSavedNames;
	static public int nFilesSaved;

	boolean should_save= false;
	private ImageView gpsInfoImage;

	boolean isResultFromProcessingPlugin = false;
	
	private int saveOption;
//	private int exportFormat;
	private boolean useGeoTaggingPrefExport;

	Thread saving;

	boolean isFirstGpsFix=true;
	
	private long sessionID=0;
	
	public ExportPlugin()
	{
		super("com.almalence.plugins.export",
			  R.xml.preferences_export_export,
			  0,
			  0,
			  null);
	}
	
	@Override
	public void onExportActive(long SessionID)
	{
		sessionID=SessionID;
		getPrefs();
		
		isResultFromProcessingPlugin = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("ResultFromProcessingPlugin"+Long.toString(sessionID)));
		
		savePicture();
	}
	
	private void getPrefs()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        saveOption = Integer.parseInt(prefs.getString("exportName", "2"));
//        exportFormat = Integer.parseInt(prefs.getString("exportFormat", "1"));
        useGeoTaggingPrefExport = prefs.getBoolean("useGeoTaggingPrefExport", false);
	}
	
	@Override
	public void onResume()
	{
		getPrefs();
		
		isFirstGpsFix=true;
		
		clearInfoViews();
		
		if (useGeoTaggingPrefExport)
        {
			View v = LayoutInflater.from(MainScreen.mainContext).inflate(R.layout.plugin_export_gps, null);
			gpsInfoImage = (ImageView)v.findViewById(R.id.gpsInfoImage);
			gpsInfoImage.setImageDrawable(MainScreen.mainContext.getResources().getDrawable(R.drawable.gps_off));
			
			addInfoView(gpsInfoImage);
			
        	MLocation.subsribe(MainScreen.thiz);
        	MLocation.lm.addGpsStatusListener(new GpsStatus.Listener() {
				
        		@Override
        	    public void onGpsStatusChanged(int event) {

        			ExportPlugin.this.ShowGPSStatus(event);
        	    }    
			});
		}
		else
		{
			View v = LayoutInflater.from(MainScreen.mainContext).inflate(R.layout.plugin_export_gps, null);
			gpsInfoImage = (ImageView)v.findViewById(R.id.gpsInfoImage);
			gpsInfoImage.setVisibility(View.INVISIBLE);
		}
	}
	
	public void ShowGPSStatus(int event)
	{
		switch(event) {
	    case GpsStatus.GPS_EVENT_STARTED:
	    	gpsInfoImage.setImageDrawable(MainScreen.mainContext.getResources().getDrawable(R.drawable.gps_search));
	    	gpsInfoImage.setVisibility(View.VISIBLE);
	        break;
//	    case GpsStatus.GPS_EVENT_STOPPED:
//	    	gpsInfoImage.setImageDrawable(MainScreen.mainContext.getResources().getDrawable(R.drawable.gps_off));
//	    	gpsInfoImage.setVisibility(View.INVISIBLE);
//	        break;
	    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
	    	if (!isFirstGpsFix)
	    		return;
	    	gpsInfoImage.setImageDrawable(MainScreen.mainContext.getResources().getDrawable(R.drawable.gps_found));
	    	gpsInfoImage.setVisibility(View.VISIBLE);
	    	isFirstGpsFix=false;
	        break;
		}
    }

	@Override
	public void onDestroy()
    {
		if (useGeoTaggingPrefExport)
        {
			MLocation.unsubscribe();
        }
    }
	
	private void savePicture() 
	{
		// save fused result
		try
        {
            File saveDir = PluginManager.getInstance().GetSaveDir();

	    	Calendar d = Calendar.getInstance();
		    	
	    	int imagesAmount = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("amountofresultframes"+Long.toString(sessionID)));
			if (imagesAmount==0)
				imagesAmount=1;
			
			int imageIndex = 0;
			String sImageIndex = PluginManager.getInstance().getFromSharedMem("resultframeindex"+Long.toString(sessionID));
			if(sImageIndex != null)
				imageIndex = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("resultframeindex"+Long.toString(sessionID)));
			
			if(imageIndex != 0)
				imagesAmount = 1;
			
			ContentValues values=null;
			filesSavedNames = new String[imagesAmount];
			nFilesSaved = 0;
			
			for (int i=1; i<=imagesAmount; i++)
			{
				String idx="";
				
				if (imagesAmount!=1)
					idx+="_"+i;
				
		    	//define file name format. from settings!
		    	String fileFormat = String.format(Locale.US, "%04d%02d%02d_%02d%02d%02d",
	            		d.get(Calendar.YEAR),
	            		d.get(Calendar.MONTH)+1,
	            		d.get(Calendar.DAY_OF_MONTH),
	            		d.get(Calendar.HOUR_OF_DAY),
	            		d.get(Calendar.MINUTE),
	            		d.get(Calendar.SECOND));
		    	String modeName = PluginManager.getInstance().getActiveMode().modeSaveName;
		    	switch (saveOption)
		    	{
		    	case 1://YEARMMDD_HHMMSS
		    		break;
		    		
		    	case 2://YEARMMDD_HHMMSS_MODE
		    		fileFormat += (modeName.isEmpty()?"":"_") + modeName;
		    		break;
		    		
		    	case 3://IMG_YEARMMDD_HHMMSS
		    		fileFormat = "IMG_" + fileFormat;
		    		break;
		    		
		    	case 4://IMG_YEARMMDD_HHMMSS_MODE
		    		fileFormat = "IMG_" + fileFormat + (modeName.isEmpty()?"":"_") + modeName;
		    		break;
		    	}
		    		
//		    	if (1 == exportFormat)
		    		fileFormat += idx+".jpg";
//		    	else
//		    		fileFormat += idx+".png";
		    	
		    	File file;
		    	if (MainScreen.ForceFilename == null)
	            {
		    		file = new File(
		            		saveDir, 
		            		fileFormat);
	            }
	            else
	            {
	            	file = MainScreen.ForceFilename;
	            	MainScreen.ForceFilename = null;
	            }
	
	            FileOutputStream os = new FileOutputStream(file);
	            
	            ExifInterface ei = new ExifInterface(file.getAbsolutePath());
	            
	            //Take only one result frame from several results
	            //Used for PreShot plugin that may decide which result to save
	            if(imagesAmount == 1 && imageIndex != 0)
	            	i = imageIndex;
	            
	            String resultOrientation = PluginManager.getInstance().getFromSharedMem("resultframeorientation" + i+Long.toString(sessionID));
	            int orientation = 0;
	            if (resultOrientation != null)
	            	orientation = Integer.parseInt(resultOrientation);
	            
	            String resultMirrored = PluginManager.getInstance().getFromSharedMem("resultframemirrored" + i+Long.toString(sessionID));
	            Boolean cameraMirrored = false;
	            if (resultMirrored != null)
	            	cameraMirrored = Boolean.parseBoolean(resultMirrored);

	            int x = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("saveImageHeight" + Long.toString(sessionID)));
	            int y = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("saveImageWidth" + Long.toString(sessionID)));
	            if (orientation == 0 || orientation == 180)
	            {
	            	x = Integer.valueOf(PluginManager.getInstance().getFromSharedMem("saveImageWidth" + Long.toString(sessionID)));
		            y = Integer.valueOf(PluginManager.getInstance().getFromSharedMem("saveImageHeight" + Long.toString(sessionID)));
	            }
	            
	            Boolean writeOrientationTag = true;
	            String writeOrientTag = PluginManager.getInstance().getFromSharedMem("writeorientationtag"+Long.toString(sessionID));
	            if (writeOrientTag != null)
	            	writeOrientationTag = Boolean.parseBoolean(writeOrientTag);
	            
	            String format = PluginManager.getInstance().getFromSharedMem("resultframeformat"+i+Long.toString(sessionID));
	            if (format != null && format.equalsIgnoreCase("jpeg"))
	            {//if result in jpeg format

		            if (os != null)
		            {
		            	byte[] frame = SwapHeap.SwapFromHeap(
			            		Integer.parseInt(PluginManager.getInstance().getFromSharedMem("resultframe"+i+Long.toString(sessionID))),
			            		Integer.parseInt(PluginManager.getInstance().getFromSharedMem("resultframelen"+i+Long.toString(sessionID))));
	            		os.write(frame);
	            		try
	    		    	{
	    		    		os.close();
	    				}
	    		    	catch (Exception e)
	    		        {
	    		        	e.printStackTrace();
	    		        }
		            }
	            }
	            else
	            {//if result in nv21 format
		            {
			            String res = PluginManager.getInstance().getFromSharedMem("resultfromshared"+Long.toString(sessionID));
			            if ((null == res) || "".equals(res) || "true".equals(res))
			            {
			            	// Why not just compress directly from native?
			            	final int ptr = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("resultframe"+i+Long.toString(sessionID)));
			            	com.almalence.YuvImage image = new com.almalence.YuvImage(ptr, 0x00000011, x, y, null);
			            	//to avoid problems with SKIA
			            	int cropHeight = image.getHeight()-image.getHeight()%16;
					    	image.compressToJpeg(new Rect(0, 0, image.getWidth(), cropHeight), 100, os);
					    	SwapHeap.FreeFromHeap(ptr);
			            }
			            else
			            {
			            	int yuv = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("resultframe" +i+Long.toString(sessionID)));
			            	com.almalence.YuvImage out;
		    	    		out = new com.almalence.YuvImage(yuv, ImageFormat.NV21, x, y, null);
			    	    	if (null == PluginManager.getInstance().getFromSharedMem("resultcrop0"+Long.toString(sessionID)))
			    	    	{
			    	    		out.compressToJpeg(new Rect(0, 0, out.getWidth(), out.getHeight()), 95, os);
			    	    	}
			    	    	else
		    	    		{
			    	    		int crop0 = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("resultcrop0"+Long.toString(sessionID)));
					    		int crop1 = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("resultcrop1"+Long.toString(sessionID)));
					    		int crop2 = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("resultcrop2"+Long.toString(sessionID)));
					    		int crop3 = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("resultcrop3"+Long.toString(sessionID)));
					    		Rect r = new Rect(crop0, crop1, crop0+crop2, crop1+crop3);
					    		out.compressToJpeg(r, 95, os);
		    	    		} 	    	

					    	SwapHeap.FreeFromHeap(yuv);
			            }
		            }
	            }
	            
	            String orientation_tag = String.valueOf(0);
            	switch(orientation)
            	{
            	default:
            	case 0:
            		orientation_tag = String.valueOf(0);//cameraMirrored ? String.valueOf(180) : String.valueOf(0);
            		break;
            	case 90:
            		orientation_tag = cameraMirrored ? String.valueOf(270) : String.valueOf(90);
            		break;
            	case 180:
            		orientation_tag = String.valueOf(180);//cameraMirrored ? String.valueOf(0) : String.valueOf(180);
            		break;
            	case 270:
            		orientation_tag = cameraMirrored ? String.valueOf(90) : String.valueOf(270);
            		break;
            	}
	            
	            
	            values = new ContentValues(9);
                values.put(ImageColumns.TITLE, file.getName().substring(0, file.getName().lastIndexOf(".")));
                values.put(ImageColumns.DISPLAY_NAME, file.getName());
                values.put(ImageColumns.DATE_TAKEN, System.currentTimeMillis());
                values.put(ImageColumns.MIME_TYPE, "image/jpeg");
                //values.put(ImageColumns.ORIENTATION, (!orientationLandscape && writeOrientationTag && !cameraMirrored) ? 90 : ( (!orientationLandscape && writeOrientationTag && cameraMirrored) ? 270 : 0) );
                values.put(ImageColumns.ORIENTATION, writeOrientationTag ? orientation_tag : String.valueOf(0));
                values.put(ImageColumns.DATA, file.getAbsolutePath());
                
                filesSavedNames[nFilesSaved++] = file.toString();
                
		    	if (useGeoTaggingPrefExport)
	            {
	            	Location l = MLocation.getLocation(MainScreen.mainContext);
		            
		            if (l != null)
		            {
		                double lat = l.getLatitude();
		                double lon = l.getLongitude();
		                boolean hasLatLon = (lat != 0.0d) || (lon != 0.0d);

		                if (hasLatLon) {
				            ei.setAttribute(ExifInterface.TAG_GPS_LATITUDE, GPSTagsConverter.convert(l.getLatitude()));
				            ei.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, GPSTagsConverter.latitudeRef(l.getLatitude()));
				            ei.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, GPSTagsConverter.convert(l.getLongitude()));
				            ei.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, GPSTagsConverter.longitudeRef(l.getLongitude()));
				            
				            values.put(ImageColumns.LATITUDE, l.getLatitude());
				            values.put(ImageColumns.LONGITUDE, l.getLongitude());

				            //ei.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, l.getProvider().toUpperCase());
		                    if (l.hasAltitude()) {
		                        l.getAltitude();
		                        int alt = (int)l.getAltitude()*1000;
		                        ei.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, Integer.toString(alt) +"/1000");
		                        if (alt>=0)
		                        	ei.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, Long.toString(0));
		                        else
		                        	ei.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, Long.toString(1));
		                    } else {
		                    	ei.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, Long.toString(0));
		                    	ei.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, Long.toString(0));
		                    }
//		                    if (l.getTime() != 0) {
//			                    SimpleDateFormat formatterTime = new SimpleDateFormat("HH/1,mm/1,ss/1");
//			                    Date currentTime = new Date(l.getTime());
//			                    String asGMTStringTime = currentTime.toGMTString();
//			                    String asStringTime = formatterTime.format(currentTime);
//			                    //ei.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, asStringTime);
//			                    
//			                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy:MM:dd");
//			                    Date currentDate = new Date(l.getTime());
//			                    String asString = formatter.format(currentDate);
//			                    ei.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, asString);
//			                }
		                }
			                
		            	//ei.saveAttributes();
	            	}
	            	else
	            	{
	            		//Toast.makeText(MainScreen.mainContext, "Can't get location. Turn on \"use GPS satellites\" setting", Toast.LENGTH_LONG).show();
	            		
	            	}
	            }
    	    	
    	    	String tag_exposure_time = PluginManager.getInstance().getFromSharedMem("exiftag_exposure_time"+Long.toString(sessionID));
	            String tag_aperture = PluginManager.getInstance().getFromSharedMem("exiftag_aperture"+Long.toString(sessionID));
	            String tag_flash = PluginManager.getInstance().getFromSharedMem("exiftag_flash"+Long.toString(sessionID));
	            String tag_focal_length = PluginManager.getInstance().getFromSharedMem("exiftag_focal_lenght"+Long.toString(sessionID));
	            String tag_iso = PluginManager.getInstance().getFromSharedMem("exiftag_iso"+Long.toString(sessionID));
	            String tag_white_balance = PluginManager.getInstance().getFromSharedMem("exiftag_white_balance"+Long.toString(sessionID));
	            String tag_make = PluginManager.getInstance().getFromSharedMem("exiftag_make"+Long.toString(sessionID));
	            String tag_model = PluginManager.getInstance().getFromSharedMem("exiftag_model"+Long.toString(sessionID));
	            String tag_spectral_sensitivity = PluginManager.getInstance().getFromSharedMem("exiftag_spectral_sensitivity"+Long.toString(sessionID));
	            String tag_version = PluginManager.getInstance().getFromSharedMem("exiftag_version"+Long.toString(sessionID));
	            	            
	            if(tag_exposure_time != null)
	            	ei.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, tag_exposure_time);
	            if(tag_aperture != null)
	            	ei.setAttribute(ExifInterface.TAG_APERTURE, tag_aperture);
	            if(tag_flash != null)
	            	ei.setAttribute(ExifInterface.TAG_FLASH, tag_flash);
	            if(tag_focal_length != null)
	            	ei.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, tag_focal_length);
	            if(tag_iso != null)
	            {
	            	ei.setAttribute(ExifInterface.TAG_ISO, tag_iso);
	            	ei.setAttribute("ISOSpeedRatings", tag_iso);
	            }
	            if(tag_white_balance != null)
	            	ei.setAttribute(ExifInterface.TAG_WHITE_BALANCE, tag_white_balance);
	            if(tag_make != null)
	            	ei.setAttribute(ExifInterface.TAG_MAKE, tag_make);
	            if(tag_model != null)
	            	ei.setAttribute(ExifInterface.TAG_MODEL, tag_model);
	            if(tag_spectral_sensitivity != null)
	            	ei.setAttribute("SpectralSensitivity", tag_spectral_sensitivity);
	            
	            String xStr = String.valueOf(x);
	            if (xStr != null)
	            	ei.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, xStr);
	            String yStr = String.valueOf(y);
	            if (yStr != null)
	            	ei.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, yStr);	            
	            
	            String dateString = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").format(new Date());
	            if (dateString != null)
	            {
	            	ei.setAttribute(ExifInterface.TAG_DATETIME, dateString);
	            	ei.setAttribute("DateTimeOriginal", dateString);
	            }

	            //extract mode name
	            String tag_modename = PluginManager.getInstance().getFromSharedMem("mode_name"+Long.toString(sessionID));
	            if (tag_modename == null)
	            	tag_modename = "";
	            ei.setAttribute("Software", MainScreen.thiz.getResources().getString(R.string.app_name) + ", " + tag_modename);
	            if(tag_version != null)
	            	ei.setAttribute("ExifVersion", tag_version);
	            
	            if(writeOrientationTag)			            	
	            {
	            	int exif_orientation = ExifInterface.ORIENTATION_NORMAL;
	            	switch(orientation)
	            	{
	            	default:
	            	case 0:
	            		exif_orientation = ExifInterface.ORIENTATION_NORMAL;//cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_180 : ExifInterface.ORIENTATION_NORMAL;
	            		break;
	            	case 90:
	            		exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_270 : ExifInterface.ORIENTATION_ROTATE_90;
	            		break;
	            	case 180:
	            		exif_orientation = ExifInterface.ORIENTATION_ROTATE_180;//cameraMirrored ? ExifInterface.ORIENTATION_NORMAL : ExifInterface.ORIENTATION_ROTATE_180;
	            		break;
	            	case 270:
	            		exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_90 : ExifInterface.ORIENTATION_ROTATE_270;
	            		break;
	            	}
	            	ei.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(exif_orientation));	            
	            }
	            else
	            	ei.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
	            
	            ei.saveAttributes();
		    	
		    	MainScreen.thiz.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
			}
			//MainScreen.FramesShot = true;
            //MediaScannerConnection.scanFile(MainScreen.thiz, filesSavedNames, null, null);
            
            MainScreen.H.sendEmptyMessage(PluginManager.MSG_EXPORT_FINISHED);
        }
        catch (Exception e)
        {
        	e.printStackTrace();
        	MainScreen.H.sendEmptyMessage(PluginManager.MSG_EXPORT_FINISHED);
        }
	}
}
