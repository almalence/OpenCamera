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

package com.almalence.plugins.processing.multishot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.location.Location;
import android.media.ExifInterface;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.almalence.SwapHeap;
import com.almalence.asynctaskmanager.OnTaskCompleteListener;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginProcessing;
import com.almalence.opencam.R;
import com.almalence.plugins.capture.groupshot.GroupShotCapturePlugin;
import com.almalence.plugins.export.standard.GPSTagsConverter;
/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.PluginProcessing;
import com.almalence.opencam_plus.R;
+++ --> */
// <!-- -+-
//-+- -->
import com.almalence.plugins.processing.groupshot.GroupShotProcessingPlugin;
import com.almalence.plugins.processing.objectremoval.ObjectRemovalProcessingPlugin;
import com.almalence.plugins.processing.sequence.SequenceProcessingPlugin;
import com.almalence.ui.RotateImageView;
import com.almalence.util.ImageConversion;
import com.almalence.util.MLocation;
import com.almalence.util.Size;

/***
Implements multishot processing
***/

public class MultiShotProcessingPlugin extends PluginProcessing implements OnTaskCompleteListener, Handler.Callback, OnClickListener {
	
	private static int GROUP_SHOT = 0;
	private static int SEQUENCE = 1;
	private static int OBJECT_REMOVAL = 2;
	
	private View mButtonsLayout;
	
	private GroupShotProcessingPlugin groupShotProcessingPlugin;
	private SequenceProcessingPlugin sequenceProcessingPlugin;
	private ObjectRemovalProcessingPlugin objectRemovalProcessingPlugin;
	
	private int selectedPlugin;
	private long SessionID;
	
	private int mMinSize;
	private boolean isYUV;
	private boolean mSaveInputPreference;
	private static ArrayList<Integer> mYUVBufferList = new ArrayList<Integer>();
	private static ArrayList<byte[]> mJpegBufferList = new ArrayList<byte []>();
	
	public MultiShotProcessingPlugin() {
		super("com.almalence.plugins.multishotprocessing",
			  R.xml.preferences_processing_multishot,
			  0,
			  0,
			  null);
	}
	
	@Override
	public void onGUICreate() {
		LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();
		mButtonsLayout = inflator.inflate(R.layout.plugin_processing_multishot_options_layout, null, false);
		
		RotateImageView buttonObjectRemoval = (RotateImageView) mButtonsLayout.findViewById(R.id.buttonObjectRemoval);
		RotateImageView buttonGroupShot = (RotateImageView) mButtonsLayout.findViewById(R.id.buttonGroupShot);
		RotateImageView buttonSequence = (RotateImageView) mButtonsLayout.findViewById(R.id.buttonSequence);
	
		MainScreen.guiManager.removeViews(mButtonsLayout, R.id.specialPluginsLayout3);
				
		buttonObjectRemoval.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				selectedPlugin = OBJECT_REMOVAL;
				mButtonsLayout.setVisibility(View.GONE);
			}
		});
		
		buttonGroupShot.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				selectedPlugin = GROUP_SHOT;
				mButtonsLayout.setVisibility(View.GONE);
			}
		});
		
		buttonSequence.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				selectedPlugin = SEQUENCE;
				mButtonsLayout.setVisibility(View.GONE);
			}
		});
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT);
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout3)).addView(mButtonsLayout, params);
		
		buttonObjectRemoval.setOrientation(MainScreen.guiManager.getLayoutOrientation());
		buttonGroupShot.setOrientation(MainScreen.guiManager.getLayoutOrientation());
		buttonSequence.setOrientation(MainScreen.guiManager.getLayoutOrientation());
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout3)).requestLayout();
	}
	
	@Override
	public void onCreate() {
		groupShotProcessingPlugin =  new GroupShotProcessingPlugin();
		sequenceProcessingPlugin = new SequenceProcessingPlugin();
		objectRemovalProcessingPlugin = new ObjectRemovalProcessingPlugin();
	}
	
	@Override
	public View getPostProcessingView() {
		if (selectedPlugin == GROUP_SHOT) {
			return groupShotProcessingPlugin.getPostProcessingView();
		}
		else if (selectedPlugin == SEQUENCE) {
			return sequenceProcessingPlugin.getPostProcessingView();
		}
		else if (selectedPlugin == OBJECT_REMOVAL) {
			return objectRemovalProcessingPlugin.getPostProcessingView();
		}
		
		return null;
	}
	
	@Override
	public void onStart() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.thiz.getBaseContext());
		mSaveInputPreference = prefs.getBoolean("saveInputPrefMultiShot", false);
		
		groupShotProcessingPlugin.onStart();
		sequenceProcessingPlugin.onStart();
		objectRemovalProcessingPlugin.onStart();
	}
	
	@Override
	public void onStartProcessing(long SessionID)  {
		this.SessionID = SessionID;
		
		selectedPlugin = -1;
		
		MainScreen.thiz.runOnUiThread(new Runnable() {
		    public void run() {   
		    	mButtonsLayout.setVisibility(View.VISIBLE);
		    }
		});
		
		prepareDataForProcessing();
		
		while(selectedPlugin == -1) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		        
		if (selectedPlugin == GROUP_SHOT) {
			GroupShotProcessingPlugin.setmJpegBufferList(mJpegBufferList);
			GroupShotProcessingPlugin.setmYUVBufferList(mYUVBufferList);
			groupShotProcessingPlugin.onStartProcessing(SessionID);
		}
		else if (selectedPlugin == SEQUENCE) {
			SequenceProcessingPlugin.setmJpegBufferList(mJpegBufferList);
			SequenceProcessingPlugin.setmYUVBufferList(mYUVBufferList);
			sequenceProcessingPlugin.onStartProcessing(SessionID);
		}
		else if (selectedPlugin == OBJECT_REMOVAL) {
			ObjectRemovalProcessingPlugin.setmJpegBufferList(mJpegBufferList);
//			ObjectRemovalProcessingPlugin.setmYUVBufferList(mYUVBufferList);
			objectRemovalProcessingPlugin.onStartProcessing(SessionID);
		}
	}
	
	private void prepareDataForProcessing() {
        int imagesAmount = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("amountofcapturedframes"+Long.toString(SessionID)));
		
		if (imagesAmount==0)
			imagesAmount=1;
		
		int iImageWidth = MainScreen.getImageWidth();
		int iImageHeight = MainScreen.getImageHeight();
		
		isYUV = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("isyuv"+Long.toString(SessionID)));

		mYUVBufferList.clear();
		mJpegBufferList.clear();
		
		for (int i=1; i<=imagesAmount; i++) {
			if(isYUV) {
				int yuv = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frame" + i+Long.toString(SessionID)));
				mYUVBufferList.add(i-1, yuv);
			}
			else {
    			byte[] in = SwapHeap.CopyFromHeap(
    	        		Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frame" + i+Long.toString(SessionID))),
    	        		Integer.parseInt(PluginManager.getInstance().getFromSharedMem("framelen" + i+Long.toString(SessionID)))
    	        		);
    			
    			mJpegBufferList.add(i-1, in);
			}
		}
		
		int mDisplayOrientation = MainScreen.guiManager.getDisplayOrientation();
		
		if (mSaveInputPreference) {
			try {
				File saveDir = PluginManager.getInstance().GetSaveDir(false);
				
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
				int saveOption = Integer.parseInt(prefs.getString("exportName", "3"));
				Calendar d = Calendar.getInstance();
				String fileFormat = String.format("%04d%02d%02d_%02d%02d%02d",
						d.get(Calendar.YEAR),
						d.get(Calendar.MONTH)+1,
						d.get(Calendar.DAY_OF_MONTH),
						d.get(Calendar.HOUR_OF_DAY),
						d.get(Calendar.MINUTE),
						d.get(Calendar.SECOND));
				switch (saveOption)
				{
				case 1://YEARMMDD_HHMMSS
					break;
					
				case 2://YEARMMDD_HHMMSS_MODE
					fileFormat += "_" + PluginManager.getInstance().getActiveMode().modeSaveName;
					break;
					
				case 3://IMG_YEARMMDD_HHMMSS
					fileFormat = "IMG_" + fileFormat;
					break;
					
				case 4://IMG_YEARMMDD_HHMMSS_MODE
					fileFormat = "IMG_" + fileFormat + "_" + PluginManager.getInstance().getActiveMode().modeSaveName;
					break;
				}
				
				ContentValues values=null;
				
				for (int i = 0; i<imagesAmount; ++i)
				{
					
					String index = String.format("_%02d", i);
					File file = new File(saveDir, fileFormat+index+".jpg");
					
					FileOutputStream os = null;
					try
					{
						os = new FileOutputStream(file);
					}
					catch (Exception e)
					{
						//save always if not working saving to sdcard
						e.printStackTrace();
						saveDir = PluginManager.getInstance().GetSaveDir(true);
						file = new File(saveDir, fileFormat+index+".jpg");
						os = new FileOutputStream(file);
					}	   
					
					String resultOrientation = PluginManager.getInstance().getFromSharedMem("frameorientation" + (i+1) + Long.toString(SessionID));
					Boolean orientationLandscape = false;
					if (resultOrientation == null)
						orientationLandscape = true;
					else
						orientationLandscape = Boolean.parseBoolean(resultOrientation);
					
					String resultMirrored = PluginManager.getInstance().getFromSharedMem("framemirrored" + (i+1) + Long.toString(SessionID));
					Boolean cameraMirrored = false;
					if (resultMirrored != null)
						cameraMirrored = Boolean.parseBoolean(resultMirrored);
					
					if (os != null)
					{
						if(!isYUV)
						{
							// ToDo: not enough memory error reporting
							os.write(mJpegBufferList.get(i));
						}
						else
						{
							com.almalence.YuvImage image = new com.almalence.YuvImage(mYUVBufferList.get(i), ImageFormat.NV21, iImageWidth, iImageHeight, null);
							//to avoid problems with SKIA
							int cropHeight = image.getHeight()-image.getHeight()%16;
							image.compressToJpeg(new Rect(0, 0, image.getWidth(), cropHeight), 100, os);
						}
						os.close();
						
						ExifInterface ei = new ExifInterface(file.getAbsolutePath());
						int exif_orientation = ExifInterface.ORIENTATION_NORMAL;
						switch(mDisplayOrientation)
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
						ei.setAttribute(ExifInterface.TAG_ORIENTATION, "" + exif_orientation);
						ei.saveAttributes();
					}
					
					values = new ContentValues(9);
					values.put(ImageColumns.TITLE, file.getName().substring(0, file.getName().lastIndexOf(".")));
					values.put(ImageColumns.DISPLAY_NAME, file.getName());
					values.put(ImageColumns.DATE_TAKEN, System.currentTimeMillis());
					values.put(ImageColumns.MIME_TYPE, "image/jpeg");
					values.put(ImageColumns.ORIENTATION, (!orientationLandscape && !cameraMirrored) ? 90 : (!orientationLandscape && cameraMirrored) ? -90 : 0);                
					values.put(ImageColumns.DATA, file.getAbsolutePath());
					
					if (prefs.getBoolean("useGeoTaggingPrefExport", false))
					{
						Location l = MLocation.getLocation(MainScreen.mainContext);
						
						if (l != null)
						{	     
							ExifInterface ei = new ExifInterface(file.getAbsolutePath());
							ei.setAttribute(ExifInterface.TAG_GPS_LATITUDE, GPSTagsConverter.convert(l.getLatitude()));
							ei.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, GPSTagsConverter.latitudeRef(l.getLatitude()));
							ei.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, GPSTagsConverter.convert(l.getLongitude()));
							ei.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, GPSTagsConverter.longitudeRef(l.getLongitude()));
							
							ei.saveAttributes();
							
							values.put(ImageColumns.LATITUDE, l.getLatitude());
							values.put(ImageColumns.LONGITUDE, l.getLongitude());
						}
					}
					
					MainScreen.thiz.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
				}
			}
			catch(IOException e) {
				e.printStackTrace();
				MainScreen.H.sendEmptyMessage(PluginManager.MSG_EXPORT_FINISHED_IOEXCEPTION);
				return;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
/************************************************
 * 		POST PROCESSING
 ************************************************/
	@Override
	public boolean isPostProcessingNeeded() {
		return true;
	}
	
	public void onStartPostProcessing() {	
		if (selectedPlugin == GROUP_SHOT) {
			groupShotProcessingPlugin.onStartPostProcessing();
		}
		else if (selectedPlugin == SEQUENCE) {
			sequenceProcessingPlugin.onStartPostProcessing();
		}
		else if (selectedPlugin == OBJECT_REMOVAL) {
			objectRemovalProcessingPlugin.onStartPostProcessing();
		}
	}
	
    @Override
	public void onClick(View v) {
    	if (selectedPlugin == GROUP_SHOT) {
			groupShotProcessingPlugin.onClick(v);
		}
		else if (selectedPlugin == SEQUENCE) {
			sequenceProcessingPlugin.onClick(v);
		}
		else if (selectedPlugin == OBJECT_REMOVAL) {
			objectRemovalProcessingPlugin.onClick(v);
		}
	}
    
    @Override
	public boolean handleMessage(Message msg) {
    	if (selectedPlugin == GROUP_SHOT) {
    		return ((Callback) groupShotProcessingPlugin).handleMessage(msg);
		}
		else if (selectedPlugin == SEQUENCE) {
			return ((Callback) sequenceProcessingPlugin).handleMessage(msg);
		}
		else if (selectedPlugin == OBJECT_REMOVAL) {
			return ((Callback) objectRemovalProcessingPlugin).handleMessage(msg);
		}

    	return false;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean res = false;
		
		if (selectedPlugin == GROUP_SHOT) {
			res = groupShotProcessingPlugin.onKeyDown(keyCode, event);
		}
		else if (selectedPlugin == SEQUENCE) {
			res = sequenceProcessingPlugin.onKeyDown(keyCode, event);
		}
		else if (selectedPlugin == OBJECT_REMOVAL) {
			res = objectRemovalProcessingPlugin.onKeyDown(keyCode, event);
		}
		
		if (res) {
			return res;
		}
		return super.onKeyDown(keyCode, event);
	}
/************************************************
 * 		POST PROCESSING END
 ************************************************/
}
