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

package com.almalence.plugins.processing.objectremoval;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Location;
import android.media.ExifInterface;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;

import com.almalence.SwapHeap;
import com.almalence.asynctaskmanager.OnTaskCompleteListener;

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

import com.almalence.util.MLocation;
import com.almalence.util.Size;

import com.almalence.plugins.export.standard.GPSTagsConverter;
import com.almalence.plugins.processing.objectremoval.AlmaCLRShot.ObjBorderInfo;
import com.almalence.plugins.processing.objectremoval.AlmaCLRShot.ObjectInfo;
import com.almalence.plugins.processing.objectremoval.AlmaCLRShot.OnProcessingListener;

/***
Implements night processing
***/

public class ObjectRemovalProcessingPlugin extends PluginProcessing implements OnTaskCompleteListener, Handler.Callback, OnClickListener
{
	private long sessionID=0;
    public static int mSensitivity = 15;
    public static int mMinSize = 1000;
    public static String mGhosting = "0";
    
    public static int mAngle = 0;

    public static boolean SaveInputPreference;
    
    public boolean released=false;
    private AlmaCLRShot mAlmaCLRShot;

    static public int imgWidthOR;
	static public int imgHeightOR;
	private int mDisplayOrientation;
	private boolean mCameraMirrored;
	
	//indicates that no more user interaction needed
	private boolean finishing = false;
	
	public ObjectRemovalProcessingPlugin()
	{
		super("com.almalence.plugins.objectremovalprocessing",
			  R.xml.preferences_processing_objectremoval,
			  0,
			  0,
			  null);
	}
		
	@Override
	public void onStart()
	{
		getPrefs();
	}
	
	@Override
	public void onStartProcessing(long SessionID) 
	{
		finishing = false;
		released=false;
		Message msg = new Message();
		msg.what = PluginManager.MSG_PROCESSING_BLOCK_UI;
		MainScreen.H.sendMessage(msg);	
		
		Message msg2 = new Message();
		msg2.arg1 = PluginManager.MSG_CONTROL_LOCKED;
		msg2.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg2);
		
		MainScreen.guiManager.lockControls = true;
		
		sessionID=SessionID;

		PluginManager.getInstance().addToSharedMem("modeSaveName"+Long.toString(sessionID), PluginManager.getInstance().getActiveMode().modeSaveName);
		
		mDisplayOrientation = MainScreen.guiManager.getDisplayOrientation();
    	mCameraMirrored = MainScreen.getCameraMirrored();
    	
    	int iSaveImageWidth = MainScreen.getSaveImageWidth();
		int iSaveImageHeight = MainScreen.getSaveImageHeight();
        
        if(mDisplayOrientation == 0 || mDisplayOrientation == 180)
        {
        	imgWidthOR = MainScreen.getImageHeight();
        	imgHeightOR = MainScreen.getImageWidth();
        }
        else
        {
        	imgWidthOR = MainScreen.getImageWidth();
        	imgHeightOR = MainScreen.getImageHeight();
        }
		
		mAlmaCLRShot = AlmaCLRShot.getInstance();
		
        getPrefs();
         
     	try {
     		Size input = new Size(MainScreen.getImageWidth(), MainScreen.getImageHeight());
            int imagesAmount = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("amountofcapturedframes"+Long.toString(sessionID)));
            ArrayList<byte []> compressed_frame = new ArrayList<byte []>();
     		int minSize = 1000;
     		if (mMinSize == 0) {
     			minSize = 0;
     		} else {
     			minSize = input.getWidth() * input.getHeight() / mMinSize;
     		}
    		
    		if (imagesAmount==0)
    			imagesAmount=1;
    		
    		for (int i=1; i<=imagesAmount; i++)
    		{
    			byte[] in = SwapHeap.CopyFromHeap(
    	        		Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frame" + i+Long.toString(sessionID))),
    	        		Integer.parseInt(PluginManager.getInstance().getFromSharedMem("framelen" + i+Long.toString(sessionID)))
    	        		);
    			
    			compressed_frame.add(i-1, in);
    		}
    		
    		mJpegBufferList = compressed_frame;
    		getDisplaySize(mJpegBufferList.get(0));
    		Size preview = new Size(mDisplayWidth, mDisplayHeight);
    		
    		if (SaveInputPreference)
    		{
    			try
    	        {
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
    		            
    		            String resultOrientation = PluginManager.getInstance().getFromSharedMem("frameorientation" + (i+1) + Long.toString(sessionID));
    		            Boolean orientationLandscape = false;
    		            if (resultOrientation == null)
    			            orientationLandscape = true;
    		            else
    		            	orientationLandscape = Boolean.parseBoolean(resultOrientation);
    		            
    		            String resultMirrored = PluginManager.getInstance().getFromSharedMem("framemirrored" + (i+1) + Long.toString(sessionID));
    		            Boolean cameraMirrored = false;
    		            if (resultMirrored != null)
    		            	cameraMirrored = Boolean.parseBoolean(resultMirrored);
    		            
    		            if (os != null)
    		            {
    		            	// ToDo: not enough memory error reporting
    			            os.write(compressed_frame.get(i));
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
    		            
    		            String dateString = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").format(new Date());
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
//    			            	Exiv2.writeGeoDataIntoImage(
//    			            		file.getAbsolutePath(), 
//    			            		true,
//    			            		l.getLatitude(), 
//    			            		l.getLongitude(), 
//    			            		dateString, 
//    			            		android.os.Build.MANUFACTURER != null ? android.os.Build.MANUFACTURER : "Google",
//    			            		android.os.Build.MODEL != null ? android.os.Build.MODEL : "Android device");
    			            		
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
    		
    		
    		PluginManager.getInstance().addToSharedMem("amountofresultframes"+Long.toString(sessionID), String.valueOf(imagesAmount));
    		
    		PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(sessionID), String.valueOf(iSaveImageWidth));
        	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(sessionID), String.valueOf(iSaveImageHeight));
     		
     		//frames!!! should be taken from heap
     		mAlmaCLRShot.addInputFrame(compressed_frame, input);

     		mAlmaCLRShot.initialize(preview,
     				mAngle,
 					/*
 					 * -1 : auto mode
 					 * 0 ~ max number of input frame : manual mode
 					 */
 					-1,
 					/*
 					 * sensitivity for objection detection
 					 * 
 					 */
 					mSensitivity - 15,
 					/*
 					 *  Minimum size of object to be able to detect
 					 *  -15 ~ 15
 					 *  max -> easy detection dull detection
 					 *  min -> 
 					 */
 					minSize,
 					/*
 					 * ghosting parameter
    					 * 0 : normal operation
 					 * 1 : detect ghosted objects but not remove them
 					 * 2 : detect and remove all object
 					 */
 					Integer.parseInt(mGhosting),
 					mOnProcessingListener);
     		compressed_frame.clear();
 		} 
     	catch (Exception e) 
 		{
     		Toast.makeText(MainScreen.mainContext, "Low memory. Can't finish processing.", Toast.LENGTH_LONG).show();
 			e.printStackTrace();
 		}
     }
		
//	public void FreeMemory()
//	{
//		mAlmaCLRShot.release();
//	}

/************************************************
 * 		POST PROCESSING
 ************************************************/
	@Override
	public boolean isPostProcessingNeeded()
	{
		return true;
	}
	
	private ImageView mImgView;
	private Button mSaveButton;
	
	private int mLayoutOrientationCurrent;
	private int mDisplayOrientationCurrent;
	
	private static final int MSG_REDRAW = 1;
	private static final int MSG_LEAVING = 3;
	private static final int MSG_END_OF_LOADING = 4;
	private static final int MSG_SAVE = 5;
	private final Handler mHandler = new Handler(this);
	private boolean[] mObjStatus;
	private Bitmap PreviewBmp = null;
	public static int mDisplayWidth;
	public static int mDisplayHeight;
	public static ArrayList<byte[]> mJpegBufferList;
	Paint paint=null;
	
	private boolean postProcessingRun = false;
	
	@Override
	public void onStartPostProcessing()
	{	
		mDisplayOrientationCurrent = MainScreen.guiManager.getDisplayOrientation();
		int orientation = MainScreen.guiManager.getLayoutOrientation();    	
    	mLayoutOrientationCurrent = (orientation == 0 || orientation == 180)? orientation: (orientation + 180)%360;
		
		LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();
		postProcessingView = inflator.inflate(R.layout.plugin_processing_objectremoval_postprocessing, null, false);
		
		mImgView = ((ImageView)postProcessingView.findViewById(R.id.objectremovalImageHolder));
		
		mObjStatus = new boolean[mAlmaCLRShot.getTotalObjNum()];
        Arrays.fill(mObjStatus, true);

        if (PreviewBmp != null) {
        	PreviewBmp.recycle();
        }

		paint = new Paint();
		paint.setColor(0xFF00AAEA);
		paint.setStrokeWidth(5);
		paint.setPathEffect(new DashPathEffect(new float[] {5,5},0));

    	PreviewBmp = mAlmaCLRShot.getPreviewBitmap();
    	drawObjectRectOnBitmap(PreviewBmp, mAlmaCLRShot.getObjectInfoList(), mAlmaCLRShot.getObjBorderBitmap(paint));

        if (PreviewBmp != null)  
        {
        	Matrix matrix = new Matrix();
        	matrix.postRotate(90);
        	Bitmap rotated = Bitmap.createBitmap(PreviewBmp, 0, 0, PreviewBmp.getWidth(), PreviewBmp.getHeight(),
        	        matrix, true);
        	mImgView.setImageBitmap(rotated);
        	//mImgView.setRotation(MainScreen.getCameraMirrored()?180:0);
        	mImgView.setRotation(MainScreen.getCameraMirrored()? ((mDisplayOrientation == 0 || mDisplayOrientation == 180) ? 0 : 180) : 0);
        }		

	    mHandler.sendEmptyMessage(MSG_END_OF_LOADING);
	}
	
	public void getDisplaySize(byte[] data) 
	{
		Display display= ((WindowManager) MainScreen.thiz.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Config.ARGB_8888;
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(data, 0, data.length, options);
		Point dis = new Point();
		display.getSize(dis);

		float imageRatio = (float)options.outWidth / (float)options.outHeight;
		float displayRatio = (float)dis.y / (float)dis.x;
		
		if (imageRatio > displayRatio) {
			mDisplayWidth = dis.y;
			mDisplayHeight = (int)((float)dis.y / (float)imageRatio);
		} else {
			mDisplayWidth = (int)((float)dis.x * (float)imageRatio);
			mDisplayHeight = dis.x;
		}
		return;
	}
	
	private void setupImageView() {
		mImgView.setOnTouchListener(new View.OnTouchListener()
        {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					if (finishing == true)
						return true;
			        float x = event.getY();
					float y = mDisplayHeight-1-event.getX();
					int objIndex = 0;
					try {
						objIndex = mAlmaCLRShot.getOccupiedObject(x, y);
					} catch (Exception e) {
						e.printStackTrace();
					}

					if (objIndex >= 1) {
						mObjStatus[objIndex-1] = !mObjStatus[objIndex-1];
					}
					mHandler.sendEmptyMessage(MSG_REDRAW);
				}
				return false;
			}
        });
    }
	
    public void setupSaveButton() {
    	// put save button on screen
        mSaveButton = new Button(MainScreen.thiz);
        mSaveButton .setBackgroundResource(R.drawable.button_save_background);
        mSaveButton .setOnClickListener(this);
        LayoutParams saveLayoutParams = new LayoutParams(
        		(int) (MainScreen.mainContext.getResources().getDimension(R.dimen.postprocessing_savebutton_size)), 
        		(int) (MainScreen.mainContext.getResources().getDimension(R.dimen.postprocessing_savebutton_size)));
        saveLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        saveLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        saveLayoutParams.setMargins(
        		(int)(MainScreen.thiz.getResources().getDisplayMetrics().density * 8), 
        		(int)(MainScreen.thiz.getResources().getDisplayMetrics().density * 8), 
        		0, 
        		0);
		((RelativeLayout)postProcessingView.findViewById(R.id.objectremovalLayout)).addView(mSaveButton, saveLayoutParams);
		mSaveButton.setRotation(mLayoutOrientationCurrent);
    }
    
    public void onOrientationChanged(int orientation)
    {	    	
    	if(orientation != mDisplayOrientationCurrent)
    	{
    		mLayoutOrientationCurrent = (orientation == 0 || orientation == 180) ? orientation + 90 : orientation - 90;
    		mDisplayOrientationCurrent = orientation;
    		if(postProcessingRun)
    			mSaveButton.setRotation(mLayoutOrientationCurrent);
    	}
    }
    
    @Override
	public void onClick(View v) 
	{
    	if (v == mSaveButton)
    	{
    		if (finishing == true)
				return;
    		finishing = true;
    		mHandler.sendEmptyMessage(MSG_SAVE);
    	}
	}
    
    public void savePicture(Context context)
    {
    	byte[] result = mAlmaCLRShot.processingSaveData();
		int frame_len = result.length;
		int frame = SwapHeap.SwapToHeap(result);
		//boolean wantLandscape = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("frameorientation1" + Long.toString(sessionID)));
		//boolean cameraMirrored = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("framemirrored1" + Long.toString(sessionID)));
		PluginManager.getInstance().addToSharedMem("resultframeformat1"+Long.toString(sessionID), "jpeg");
		PluginManager.getInstance().addToSharedMem("resultframe1"+Long.toString(sessionID), String.valueOf(frame));
    	PluginManager.getInstance().addToSharedMem("resultframelen1"+Long.toString(sessionID), String.valueOf(frame_len));
    	
    	PluginManager.getInstance().addToSharedMem("resultframeorientation1" + String.valueOf(sessionID), String.valueOf(mDisplayOrientation));
    	PluginManager.getInstance().addToSharedMem("resultframemirrored1" + String.valueOf(sessionID), String.valueOf(mCameraMirrored));
		
		
		PluginManager.getInstance().addToSharedMem("amountofresultframes"+Long.toString(sessionID), String.valueOf(1));
		
		PluginManager.getInstance().addToSharedMem("sessionID", String.valueOf(sessionID));
//		try
//        {	
//			String[] filesSavedNames = new String[1];
//			Calendar d = Calendar.getInstance();
//			File saveDir = PluginManager.getInstance().GetSaveDir();
//			String fileFormat = String.format("%04d%02d%02d_%02d%02d%02d",
//            		d.get(Calendar.YEAR),
//            		d.get(Calendar.MONTH)+1,
//            		d.get(Calendar.DAY_OF_MONTH),
//            		d.get(Calendar.HOUR_OF_DAY),
//            		d.get(Calendar.MINUTE),
//            		d.get(Calendar.SECOND));
//			
//			fileFormat += "_" + PluginManager.getInstance().getActiveMode().modeName + ".jpg";
//			
//			File file = new File(
//            		saveDir, 
//            		fileFormat);				
//            
//            filesSavedNames[0] = file.toString();
//
//            FileOutputStream os = new FileOutputStream(file);
//            
////            byte[] data = mAlmaCLRShot.processingSaveData();
////         	int frame_len = data.length;
////    		int frame = SwapHeap.SwapToHeap(data);
////         	
////         	PluginManager.getInstance().addToSharedMem("resultframeformat1"+Long.toString(sessionID), "jpeg");
////    		PluginManager.getInstance().addToSharedMem("amountofresultframes"+Long.toString(sessionID), "1");
////        	PluginManager.getInstance().addToSharedMem("resultframe1"+Long.toString(sessionID), String.valueOf(frame));
////        	PluginManager.getInstance().addToSharedMem("resultframelen1"+Long.toString(sessionID), String.valueOf(frame_len));
//
//        	os.write(mAlmaCLRShot.processingSaveData());
//            
//            os.close();
//            
//            ExifInterface ei = new ExifInterface(file.getAbsolutePath());
//            if(!mDisplayLandscape)			            	
//            	ei.setAttribute(ExifInterface.TAG_ORIENTATION, "" + (mCameraMirrored ? ExifInterface.ORIENTATION_ROTATE_270 : ExifInterface.ORIENTATION_ROTATE_90));
//            ei.saveAttributes();
//
//           	ContentValues values = new ContentValues(9);
//            values.put(ImageColumns.TITLE, file.getName().substring(0, file.getName().lastIndexOf(".")));
//            values.put(ImageColumns.DISPLAY_NAME, file.getName());
//            values.put(ImageColumns.DATE_TAKEN, System.currentTimeMillis());
//            values.put(ImageColumns.MIME_TYPE, "image/jpeg");
//            values.put(ImageColumns.ORIENTATION, mDisplayLandscape? 0 : mCameraMirrored? -90 : 90);
//            values.put(ImageColumns.DATA, file.getAbsolutePath());
//            
//            MainScreen.thiz.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
//            MediaScannerConnection.scanFile(MainScreen.thiz, filesSavedNames, null, null);
//        }
//        catch (Exception e)
//        {
//
//        	Toast toast = Toast.makeText(context, R.string.cannot_create_jpeg, Toast.LENGTH_LONG);
//        	toast.show();
//        	e.printStackTrace();
//        }
		
    }
    
    @Override
	public boolean handleMessage(Message msg)
	{
    	switch (msg.what)
    	{
    	case MSG_END_OF_LOADING:
			setupImageView();
			setupSaveButton();
			postProcessingRun = true;
    		break;
    	case MSG_SAVE:
    		try {
				mAlmaCLRShot.setObjectList(mObjStatus);
			} catch (Exception e) {
				e.printStackTrace();
			}
    		savePicture(MainScreen.mainContext);
    		mHandler.sendEmptyMessage(MSG_LEAVING);
    		break;
    	case MSG_LEAVING:
    		if (released==true)
    			return false;
    		MainScreen.H.sendEmptyMessage(PluginManager.MSG_POSTPROCESSING_FINISHED);
    		mJpegBufferList.clear();
    		
    		Message msg2 = new Message();
    		msg2.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
    		msg2.what = PluginManager.MSG_BROADCAST;
    		MainScreen.H.sendMessage(msg2);
    		
    		MainScreen.guiManager.lockControls = false;   		

    		postProcessingRun = false;
    		
    		mAlmaCLRShot.release();
    		released=true;
        	break;
        	
    	case MSG_REDRAW:
            if (PreviewBmp != null)
            	PreviewBmp.recycle();
            if (finishing == true)
				return true;
    		PreviewBmp = mAlmaCLRShot.getPreviewBitmap();
    		drawObjectRectOnBitmap(PreviewBmp, mAlmaCLRShot.getObjectInfoList(), mAlmaCLRShot.getObjBorderBitmap(paint));
            if (PreviewBmp != null) 
        	{
            	Matrix matrix = new Matrix();
            	matrix.postRotate(90);
            	Bitmap rotated = Bitmap.createBitmap(PreviewBmp, 0, 0, PreviewBmp.getWidth(), PreviewBmp.getHeight(),
            	        matrix, true);
            	mImgView.setImageBitmap(rotated);
            	//mImgView.setRotation(MainScreen.getCameraMirrored()?180:0);
            	mImgView.setRotation(MainScreen.getCameraMirrored()? ((mDisplayOrientation == 0 || mDisplayOrientation == 180) ? 0 : 180) : 0);
        	}
            break;
    	}    	
    	return true;
	}
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK && MainScreen.thiz.findViewById(R.id.postprocessingLayout).getVisibility() == View.VISIBLE)
		{
			if (finishing == true)
				return true;
			finishing = true;
			mHandler.sendEmptyMessage(MSG_LEAVING);
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}
		
	
	private void drawObjectRectOnBitmap(Bitmap bitmap, ObjectInfo[] objInfo, ObjBorderInfo[] boderInfo) {
		float ratio = 0.f;

		Paint paint = new Paint();
		paint.setColor(Color.rgb(0, 255, 0));
		paint.setStrokeWidth(6);
		paint.setAlpha(255);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		
		switch(mAngle) {
		case 0:
			ratio = (float)1.f;
			break;
		case 90:
			ratio = (float)bitmap.getWidth() / (float)bitmap.getHeight();			
			break;
		case 180:
			ratio = (float)1.f;
			break;
		case 270:
			ratio = (float)bitmap.getWidth() / (float)bitmap.getHeight();
			break;
		}

		Canvas c = new Canvas(bitmap);

		int i = 0;
		for (ObjectInfo obj : objInfo) {
			if (obj.getThumbnail() == null) {
				continue;
			}
			float left = (float)obj.getRect().left/ratio;
			float top = (float)obj.getRect().top/ratio;
			float right = (float)obj.getRect().right/ratio;
			float bottom =(float)obj.getRect().bottom/ratio;

			Rect newRect = new Rect((int)Math.round(left), (int)Math.round(top), (int)Math.round(right), (int)Math.round(bottom));

			if (mObjStatus[i] == false) { 
				c.drawBitmap(obj.getThumbnail(), null, newRect, paint);
			}
			i++;
		}
		
		Paint  p;
		p = new Paint();
		
		for (ObjBorderInfo obj : boderInfo) {
			if (obj == null ) {
				continue;
			}
			if (obj.getThumbnail() == null) {
				continue;
			}

			Rect newRect = obj.getRect();
			
			c.drawBitmap(obj.getThumbnail(), null, newRect, p);
		}

		return;
	}
	
/************************************************
 * 		POST PROCESSING END
 ************************************************/

	private OnProcessingListener mOnProcessingListener = new OnProcessingListener() {

		@Override
		public void onObjectCreated(ObjectInfo objInfo) {
		}

		@Override
		public void onProcessingComplete(ObjectInfo[] objInfoList) {
		}
    	
    };
	
	private void getPrefs()
    {
		// Get the xml/preferences.xml preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.thiz.getBaseContext());
        mSensitivity = prefs.getInt("Sensitivity", 19);
        mMinSize = prefs.getInt("MinSize", 1000);
        mGhosting = prefs.getString("Ghosting", "2");
        SaveInputPreference = prefs.getBoolean("saveInputPrefObjectRemoval", false);
    }
}
