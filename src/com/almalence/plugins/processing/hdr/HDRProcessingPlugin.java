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

package com.almalence.plugins.processing.hdr;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.location.Location;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.almalence.SwapHeap;
import com.almalence.asynctaskmanager.OnTaskCompleteListener;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginProcessing;
import com.almalence.opencam.R;
import com.almalence.opencam.util.ImageConversion;
import com.almalence.opencam.util.MLocation;
import com.almalence.plugins.capture.expobracketing.ExpoBracketingCapturePlugin;
import com.almalence.plugins.export.standard.GPSTagsConverter;

/***
Implements HDR processing plugin.
***/

public class HDRProcessingPlugin extends PluginProcessing implements OnItemClickListener, 
																	 OnClickListener,
																	 OnSeekBarChangeListener,
																	 OnItemSelectedListener,
																	 OnTaskCompleteListener
{
	static public boolean preview_computing = false;
	static public boolean processing_computing = false;
	static public boolean should_save = true;
	static public boolean should_unload = true;
	static public boolean should_wait = true;
	static public boolean hdr_processing_returned = false;
	static public Bitmap PreviewBmp;			// on-screen preview
	private byte[] yuv;						// fused result
	static final public int[] crop = new int[4];

	public static final int PREVIEW_TIME_PROGRESS_PARTS = 200;
	
    public static String ContrastPreference;
	public static String mContrastPreference;
	public static String ExpoPreference;
	public static String ColorPreference;
	public static String NoisePreference;
	public static boolean AutoAdjustments = false;
	public static boolean SaveInputPreference;
	
	private int mLayoutOrientationCurrent = 0;
	private int mDisplayOrientationOnStartProcessing = 0;
	private int mDisplayOrientationCurrent = 0;
	private boolean mCameraMirrored = false;
	
	private boolean postProcessingRun = false;
	
	private int mImageWidth;
	private int mImageHeight;
	
	private long sessionID=0;
	
	public HDRProcessingPlugin()
	{
		super("com.almalence.plugins.hdrprocessing",
			  R.xml.preferences_processing_hdr,
			  R.xml.preferences_processing_hdr,
			  MainScreen.thiz.getResources().getString(R.string.Pref_HDR_Preference_Title),
			  MainScreen.thiz.getResources().getString(R.string.Pref_HDR_Preference_Summary),
			  0,
			  null);
	}

	@Override
	public void onStartProcessing(long SessionID)
    {
		if(AutoAdjustments)
		{
			Message msg = new Message();
			msg.what = PluginManager.MSG_PROCESSING_BLOCK_UI;
			MainScreen.H.sendMessage(msg);	
			
			Message msg2 = new Message();
			msg2.arg1 = PluginManager.MSG_CONTROL_LOCKED;
			msg2.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg2);
			
			MainScreen.guiManager.lockControls = true;
		}
		
		Log.e("HDR", "start processing");
		sessionID=SessionID;
		
		mDisplayOrientationOnStartProcessing = MainScreen.guiManager.getDisplayOrientation();
    	mDisplayOrientationCurrent = MainScreen.guiManager.getDisplayOrientation();
    	int orientation = MainScreen.guiManager.getLayoutOrientation();
    	Log.e("PreShot", "onStartProcessing layout orientation: " + orientation);
    	mLayoutOrientationCurrent = orientation == 0 || orientation == 180? orientation: (orientation + 180)%360;
    	mCameraMirrored = MainScreen.getCameraMirrored();
		
		mImageWidth = MainScreen.getImageWidth();
		mImageHeight = MainScreen.getImageHeight();
		
		AlmaShotHDR.Initialize();
		Log.e("HDR", "almashot lib initialize success");

		//hdr processing
		HDRPreview();
		Log.e("HDR", "HDRPreview success");
		
		if(!AutoAdjustments)
		{
			HDRProcessing();
			Log.e("HDR", "HDRProcessing success");
			
			if(mDisplayOrientationOnStartProcessing == 180 || mDisplayOrientationOnStartProcessing == 270)
			{
				byte[] dataRotated = new byte[yuv.length];
				ImageConversion.TransformNV21(yuv, dataRotated, mImageWidth, mImageHeight, 1, 1, 0);
				
				yuv = dataRotated;
			}
			
			int frame_len = yuv.length;
			int frame = SwapHeap.SwapToHeap(yuv);
			
			PluginManager.getInstance().addToSharedMem("resultfromshared"+Long.toString(sessionID), "true");
		
			PluginManager.getInstance().addToSharedMem("writeorientationtag"+Long.toString(sessionID), "false");
			PluginManager.getInstance().addToSharedMem("resultframeorientation1" + String.valueOf(sessionID), String.valueOf(mDisplayOrientationOnStartProcessing));
			PluginManager.getInstance().addToSharedMem("amountofresultframes"+Long.toString(sessionID), "1");
			PluginManager.getInstance().addToSharedMem("resultframe1"+Long.toString(sessionID), String.valueOf(frame));
			PluginManager.getInstance().addToSharedMem("resultframelen1"+Long.toString(sessionID), String.valueOf(frame_len));
			
			PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(sessionID), String.valueOf(MainScreen.getSaveImageWidth()));
	    	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(sessionID), String.valueOf(MainScreen.getSaveImageHeight()));
    	
    	
    		AlmaShotHDR.HDRFreeInstance();
		    AlmaShotHDR.Release();
    	}
	}
	
	private void HDRPreview()
	{
		int SXP, SYP;
    	int[] pview;
    	
    	SXP = mImageWidth/4;
    	SYP = mImageHeight/4;
    	
    	pview = new int[SXP*SYP];	// allocate memory for preview
    	
    	int imagesAmount = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("amountofcapturedframes"+Long.toString(sessionID)));
    	
    	int compressed_frame[] = new int[imagesAmount];
        int compressed_frame_len[] = new int[imagesAmount];

		for (int i=0; i<imagesAmount; i++)
		{
			compressed_frame[i] = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frame" + (i+1)+Long.toString(sessionID)));
			compressed_frame_len[i] = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("framelen" + (i+1)+Long.toString(sessionID)));
		}
        
		if (HDRProcessingPlugin.SaveInputPreference)
		{
			try
	        {
	            File saveDir = PluginManager.getInstance().GetSaveDir();
	
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
				String[] filesSavedNames = new String[imagesAmount];
				int nFilesSaved = 0;
	        	
	            for (int i = 0; i<imagesAmount; ++i)
	            {
			    	float ev_mark = ExpoBracketingCapturePlugin.evValues[i]*ExpoBracketingCapturePlugin.ev_step;
			    	if (ExpoBracketingCapturePlugin.UseLumaAdaptation)
			    		ev_mark -= 2.0;
			    	
			    	String evmark = String.format("_%+3.1fEv", ev_mark);
		            File file = new File(
		            		saveDir, fileFormat+evmark+".jpg"); 
		            FileOutputStream os = new FileOutputStream(file);
		            
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
			            os.write(SwapHeap.CopyFromHeap(
			            		compressed_frame[ExpoBracketingCapturePlugin.evIdx[i]],
			            		compressed_frame_len[ExpoBracketingCapturePlugin.evIdx[i]]));
			            os.close();
			        
			            ExifInterface ei = new ExifInterface(file.getAbsolutePath());
			            int exif_orientation = ExifInterface.ORIENTATION_NORMAL;
		            	switch(mDisplayOrientationOnStartProcessing)
		            	{
		            	default:
		            	case 0:
		            		exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_180 : ExifInterface.ORIENTATION_NORMAL;
		            		break;
		            	case 90:
		            		exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_270 : ExifInterface.ORIENTATION_ROTATE_90;
		            		break;
		            	case 180:
		            		exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_NORMAL : ExifInterface.ORIENTATION_ROTATE_180;
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
//			            	Exiv2.writeGeoDataIntoImage(
//			            		file.getAbsolutePath(), 
//			            		true,
//			            		l.getLatitude(), 
//			            		l.getLongitude(), 
//			            		dateString, 
//			            		android.os.Build.MANUFACTURER != null ? android.os.Build.MANUFACTURER : "Google",
//			            		android.os.Build.MODEL != null ? android.os.Build.MODEL : "Android device");
			            		
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
	        catch (Exception e)
	        {
	        	e.printStackTrace();
	        }
		}
		
    	AlmaShotHDR.HDRConvertFromJpeg(
    			compressed_frame,
    			compressed_frame_len,
    			imagesAmount,
    			mImageWidth, mImageHeight);
    	Log.e("HDR", "PreviewTask.doInBackground AlmaShot.ConvertFromJpeg success");
    	
    	int nf = HDRProcessingPlugin.getNoise();
    	
    	AlmaShotHDR.HDRPreview(imagesAmount, mImageWidth, mImageHeight, pview,
    			HDRProcessingPlugin.getExposure(true),
    			HDRProcessingPlugin.getVividness(true),
    			HDRProcessingPlugin.getContrast(true),
    			HDRProcessingPlugin.getMicrocontrast(true),
    			0, nf,
    			mCameraMirrored);
    	
    	System.gc();
    	
    	AlmaShotHDR.HDRPreview2(mImageWidth, mImageHeight, pview, mCameraMirrored);

    	// android thing (OutOfMemory for bitmaps): http://stackoverflow.com/questions/3117429/garbage-collector-in-android
    	System.gc();		
	}
	
	private void HDRProcessing()
	{
		yuv = AlmaShotHDR.HDRProcess(mImageWidth, mImageHeight, HDRProcessingPlugin.crop,
    			mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270, mCameraMirrored);
	}

	@Override
	public void onResume()
	{
		getPrefs();
	}


	public static int getExposure(boolean forPreview1)
	{
		if (forPreview1)
		{
			try
			{
				return Integer.parseInt(ExpoPreference);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			return 0;
		}
		else
		{
			try
			{
				switch (Integer.parseInt(ExpoPreference))
				{
				case 0:
					return -1;
				case 1:
					return -25;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			return -1;
		}
		
	}
	public static int getVividness(boolean forPreview1)
	{
		if (forPreview1)
		{
			try
			{
				return Integer.parseInt(ColorPreference);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			return 2;
		}
		else
		{
			try
			{
				switch (Integer.parseInt(ColorPreference))
				{
				case 0:
					return -1;
				case 1:
					return -50;
				case 2:
					return -63;
				case 3:
					return -75;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			return -50;
		}
	}
	public static int getContrast(boolean forPreview1)
	{
		if (forPreview1)
		{
			try
			{
				return Integer.parseInt(ContrastPreference);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			return 1;
		}
		else
		{
			try
			{
				switch (Integer.parseInt(ContrastPreference))
				{
				case 0:
					return -50;
				case 1:
					return -75;
				case 2:
					return -100;
				}	
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			return -50;
		}
	}
	public static int getMicrocontrast(boolean forPreview1)
	{
		if (forPreview1)
		{
			try
			{
				return Integer.parseInt(ContrastPreference);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			return 1;
		}
		else
		{
			try
			{
				switch (Integer.parseInt(mContrastPreference))
				{
				case 0:
					return -25;
				case 1:
					return -50;
				case 2:
					return -75;
				}	
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			return -50;
		}
	}
	public static int getNoise()
	{
		try
		{
			return Integer.parseInt(NoisePreference);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return 1;
	}
	
	private void getPrefs()
    {
        // Get the xml/preferences.xml preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.thiz.getBaseContext());
        ContrastPreference = prefs.getString("contrastPrefHDR", "1");
        mContrastPreference = prefs.getString("mcontrastPrefHDR", "1");
        NoisePreference = prefs.getString("noisePrefHDR", "0");
        ExpoPreference = prefs.getString("expoPrefHDR", "1");
        ColorPreference = prefs.getString("colorPrefHDR", "2");
        
        AutoAdjustments = prefs.getBoolean("autoadjustPrefHDR", false);
        
        SaveInputPreference = prefs.getBoolean("saveInputPrefHDR", false);
    }

	
	
	
	
	
	
	
	private static final int ADJUSTMENT_CODE_EXPOSURE = 0;
	private static final int ADJUSTMENT_CODE_VIVIDNESS = 1;
	private static final int ADJUSTMENT_CODE_CONTRAST = 2;
	private static final int ADJUSTMENT_CODE_MICROCONTRAST = 3;
	
	private static final int CUSTOM_PRESET_POSITION = 4;

	private static final String BUNDLE_SELECTION = "adjustment_selected";
	private static final String BUNDLE_PRESET_SELECTION = "adjustment_preset_selected";
	private static final String BUNDLE_TRASHDIALOG = "adjustment_trashdialog";
// <!-- -+-
	private static final String BUNDLE_MODIFIED = "adjustments_modified";
	private static final String BUNDLE_BANNERDIALOG = "adjustment_bannerdialog";
// -+- -->
	
	private static final float PRESET_ICONS_ROUND_RADIUS = 0.2f;
	private static final int PRESET_ICONS_SIZE = 82;
	private static final float PRESET_ICONS_CROP_PART = 2.0f / 3.0f; 

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) 
    {
    	final int side = (int)(Math.min(bitmap.getWidth(), bitmap.getHeight()) * PRESET_ICONS_CROP_PART);

    	System.gc();
    	final Bitmap bitmapCropped = Bitmap.createBitmap(    				
    		bitmap, 
    		(bitmap.getWidth() - side) / 2,
    		(bitmap.getHeight() - side) / 2,
    		side, 
    		side);
    	
    	System.gc();
    	final Bitmap output = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
        
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, output.getWidth(), output.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmapCropped, rect, rect, paint);

        return output;
    }
	
	private class AdjustmentsPreviewTask extends AsyncTask<Object, Object, Object>
	{
		private int exposure = -50;
		private int vividness = -50;
		private int contrast = -50;
		private int microcontrast = -50;
		
		@Override
		protected void onPreExecute()
		{
			for (Adjustment adj : adjustments)
			{
				switch (adj.getCode())
				{
				case ADJUSTMENT_CODE_EXPOSURE:
					this.exposure = adj.getValue();
					break;
				case ADJUSTMENT_CODE_VIVIDNESS:
					this.vividness = adj.getValue();
					break;
				case ADJUSTMENT_CODE_CONTRAST:
					this.contrast = adj.getValue();
					break;
				case ADJUSTMENT_CODE_MICROCONTRAST:
					this.microcontrast = adj.getValue();
					break;
				}
			}
		}
		
		@Override 
		protected void onPostExecute(Object result)
		{
	    	notifyPreviewRecounted();
		}
		
		@Override
		protected Object doInBackground(Object... params) 
		{
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
			
	    	AlmaShotHDR.HDRPreview2a(
	    			MainScreen.getImageWidth(), 
	    			MainScreen.getImageHeight(), 
	    			pview, 
	    			mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270,
	    			this.exposure, this.vividness, this.contrast, this.microcontrast, mCameraMirrored);
	    	
	    	if (!this.isCancelled())
	    	{
	    		bitmap.setPixels(
	    				pview, 
	    				0, 
	    				SYP, 
	    				0, 
	    				0, 
	    				SYP, 
	    				SXP);
	    		
//	    		boolean isGuffyOrientation = mDisplayOrientationCurrent == 180 || mDisplayOrientationCurrent == 270;
//    			
//    			Matrix matrix = new Matrix();
//	    		
//				matrix.postRotate(isGuffyOrientation? (mLayoutOrientationCurrent + 180)%360 : mLayoutOrientationCurrent);
//				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
	    	}
			
			return null;
		}
	}
	
	@Override
    public void onOrientationChanged(int orientation)
    {	    	
    	if(orientation != mDisplayOrientationCurrent)
    	{
    		mLayoutOrientationCurrent = (orientation == 0 || orientation == 180) ? orientation + 90 : orientation - 90;
    		mDisplayOrientationCurrent = orientation;
    		if(postProcessingRun)
    		{
    			Log.e("PreSho Post processing", "orientation = " + orientation + " mLayoutOrientationCurrent =  " + mLayoutOrientationCurrent);
    			this.buttonSave.setRotation(mLayoutOrientationCurrent); 
    			this.buttonSave.invalidate();
    			this.buttonTrash.setRotation(mLayoutOrientationCurrent);
    			this.buttonTrash.invalidate();
    			this.adjustmentsTextView.setRotation(mLayoutOrientationCurrent);
    			this.adjustmentsTextView.invalidate();
    			this.adjustmentsSeekBar.setRotation(mLayoutOrientationCurrent);
    			this.adjustmentsSeekBar.invalidate();
    		}
    	}
    }
	
	
	public class PresetsAdapter extends BaseAdapter 
	{		
		private final LayoutInflater inflater;
		private final Drawable unselectedDrawable;
		private final Drawable selectedDrawable;
		private final ShapeDrawable pressedShape;
		private final PaintDrawable gradientDrawable;
		
	    public PresetsAdapter() 
	    {
	    	this.inflater = LayoutInflater.from(MainScreen.thiz);
	    	
	    	final float density = MainScreen.thiz.getResources().getDisplayMetrics().density;

	    	final int radius = (int)(PRESET_ICONS_ROUND_RADIUS * PRESET_ICONS_SIZE * density);
	    	
	    	RoundRectShape shape = new RoundRectShape(
	    			new float[] { radius, radius, radius, radius, radius, radius, radius, radius }, 
	    			null, 
	    			null);
	    	
	    	this.pressedShape = new ShapeDrawable(shape);
	    	this.pressedShape.getPaint().setARGB(100, 0, 150, 255);
	    	
	    	ShapeDrawable.ShaderFactory sf = new ShapeDrawable.ShaderFactory()
	    	{
	    	    @Override
	    	    public Shader resize(int width, int height) 
	    	    {
	    	    	RadialGradient rg = new RadialGradient(
	    	    			width / 2, 
	    	    			-0.75f * height, 
	    	    			1.25f * height, 
	    	    			new int[] 
	    		    	    { 
		    		    	    Color.argb(13, 255, 255, 255),
		    		    	    Color.argb(13, 0, 0, 0)
	    		    	    }, 
		    	            new float[] 
		    	    	    {
	    	    				0.99f, 1.00f
		    	    	    }, 
	    	    			Shader.TileMode.CLAMP);
	    	    	
	    	    	return rg;
	    	    }
	    	};
	    	this.gradientDrawable = new PaintDrawable();
	    	this.gradientDrawable.setShape(shape);
	    	this.gradientDrawable.setShaderFactory(sf);
	    
	    	ShapeDrawable unselectedShape = new ShapeDrawable(shape);
	    	unselectedShape.getPaint().setARGB(128, 128, 128, 128);
	    	this.unselectedDrawable = new LayerDrawable(new Drawable[] { unselectedShape, this.gradientDrawable });

	    	ShapeDrawable selectedShape = new ShapeDrawable(shape);
	    	selectedShape.getPaint().setARGB(255, 0, 150, 255);
	    	this.selectedDrawable = new LayerDrawable(new Drawable[] { selectedShape, this.gradientDrawable });
	    }

	    public int getCount() 
	    {
	        return presets.size();
	    }

	    public Object getItem(int position) 
	    {
	        return position;
	    }

	    public long getItemId(int position) 
	    {
	        return position;
	    }

	    public View getView(int position, View convertView, ViewGroup parent)
	    {
	    	if (convertView == null)
	    	{
	    		convertView = this.inflater.inflate(R.layout.plugin_processing_hdr_adjustments_preset_cell, null);
	    	}

	    	((ImageView)convertView.findViewById(R.id.presetImage)).setImageBitmap(
	    			presets.get(position).getThumbnail());

	    	StateListDrawable pressedDrawable = new StateListDrawable();
	    	pressedDrawable.addState(new int[] { android.R.attr.state_pressed }, this.pressedShape);
	    	pressedDrawable.addState(new int[] { -android.R.attr.state_pressed }, null);
	    	convertView.findViewById(R.id.presetSelectorView).setBackgroundDrawable(pressedDrawable);
	    	
	    	convertView.findViewById(R.id.presetIconOverlay).setBackgroundDrawable(this.gradientDrawable);
	    	
	    	if (presetSelection == position)
	    	{
	    		convertView.findViewById(R.id.presetIconHolder).setBackgroundDrawable(this.selectedDrawable);
	    	}
	    	else
	    	{
	    		convertView.findViewById(R.id.presetIconHolder).setBackgroundDrawable(this.unselectedDrawable);
	    	}
	    	
	    	((TextView)convertView.findViewById(R.id.presetTitle)).setText(
	    			presets.get(position).toString());

	        return convertView;
	    }
	}
	
		
	private class AdjustmentsAdapter extends BaseAdapter
	{		
		public AdjustmentsAdapter()
		{
		}
		
		@Override
		public int getCount()
		{
			return adjustments.size();
		}

		@Override
		public Object getItem(int arg0) 
		{
			return arg0;
		}

		@Override
		public long getItemId(int arg0)
		{
			return arg0;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) 
		{
			if (convertView == null)
			{
				convertView = new ImageView(MainScreen.thiz);
				
				GridView.LayoutParams layoutParams = new GridView.LayoutParams(
						(int) (MainScreen.thiz.getResources().getDisplayMetrics().density * 48), 
						(int) (MainScreen.thiz.getResources().getDisplayMetrics().density * 54));
				
				((ImageView)convertView).setLayoutParams(layoutParams);
				
				((ImageView)convertView).setScaleType(ScaleType.FIT_END);
			}
			
			((ImageView)convertView).setImageDrawable(adjustments.get(position).getIcon());
			
			if (selection == position)
			{
				((ImageView)convertView).setBackgroundResource(R.drawable.adjustments_tab);
			}
			else
			{
				((ImageView)convertView).setBackgroundResource(0);
			}
			
			((ImageView)convertView).setPadding(
					(int) (MainScreen.thiz.getResources().getDisplayMetrics().density * 5), 
					(int) (MainScreen.thiz.getResources().getDisplayMetrics().density * 0), 
					(int) (MainScreen.thiz.getResources().getDisplayMetrics().density * 5), 
					(int) (MainScreen.thiz.getResources().getDisplayMetrics().density * 3));
			
			
			return convertView;
		}
		
	}
	
	
	
	
	private int SXP = (mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180) ? MainScreen.getImageHeight() / 4 : MainScreen.getImageWidth() / 4;
	private int SYP = (mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180) ? MainScreen.getImageWidth() / 4 : MainScreen.getImageHeight() / 4;
	private int[] pview;
	private Bitmap bitmap;
		
	private ArrayList<Adjustment> adjustments = null;
	private final ArrayList<AdjustmentsPreset> presets = new ArrayList<AdjustmentsPreset>();
	
	private AlertDialog trashDialog = null;
// <!-- -+-
	private AlertDialog bannerDialog = null;
	private boolean modified = false;
// -+- -->
	
	private ImageView imageView;
	
	private int selection = -1;
	
	private AdjustmentsAdapter adapter;
	
	//private AsyncTaskManager mAsyncTaskManager;

	private GridView adjustmentsList;	
	private Button buttonTrash;
	private Button buttonSave;
	
	private AdapterView<Adapter> presetsGallery;
	private PresetsAdapter presetsAdapter;
	private int presetSelection = 0;
	
	private boolean saving = false;
	private boolean saveButtonPressed = false;
	private AdjustmentsPreviewTask previewTaskCurrent = null;
	private AdjustmentsPreviewTask previewTaskPending = null;

	private SeekBar adjustmentsSeekBar;
	private TextView adjustmentsTextView;
	
	private AdjustmentsPreset preset_custom = null;
	
	
	

	@Override
	public boolean isPostProcessingNeeded(){return AutoAdjustments;}
	
	@Override
	public void onStartPostProcessing()
	{
		postProcessingRun = true;
		
		SXP = (mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180) ? MainScreen.getImageHeight() / 4 : MainScreen.getImageWidth() / 4;
		SYP = (mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180) ? MainScreen.getImageWidth() / 4 : MainScreen.getImageHeight() / 4;
		
		postProcessingView = LayoutInflater.from(MainScreen.mainContext).inflate(R.layout.plugin_processing_hdr_adjustments, null);
		
		this.imageView = ((ImageView)postProcessingView.findViewById(R.id.adjustments_previewHolder));
		this.buttonTrash = ((Button)postProcessingView.findViewById(R.id.adjustments_trashButton));
		this.buttonSave = ((Button)postProcessingView.findViewById(R.id.adjustments_saveButton));
		this.presetsGallery = ((AdapterView<Adapter>)postProcessingView.findViewById(R.id.presets_list));
		this.adjustmentsSeekBar = ((SeekBar)postProcessingView.findViewById(R.id.adjustments_seek));		
		this.adjustmentsTextView = ((TextView)postProcessingView.findViewById(R.id.adjustments_seek_title));
		
		saveButtonPressed = false;
	

		//this.mAsyncTaskManager = new AsyncTaskManager(this, this);
		
		Object obj = MainScreen.thiz.getLastNonConfigurationInstance();
		if (obj != null)
		{
			try
			{
				this.adjustments = (ArrayList<Adjustment>)((Object[])obj)[0];
			}
			catch (ClassCastException e)
			{
				e.printStackTrace();
			}
			
//			try
//			{
//				this.mAsyncTaskManager.handleRetainedTask(((Object[])obj)[1]);
//			}
//			catch (ClassCastException e)
//			{
//				e.printStackTrace();
//			}
			
			try
			{
				this.saving = (Boolean)((Object[])obj)[2];
			}
			catch (ClassCastException e)
			{
				e.printStackTrace();
			}
			
			try
			{
				this.preset_custom = (AdjustmentsPreset)((Object[])obj)[3];
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}		
		
		if (this.saving)
		{
			return;
		}
		
		DisplayMetrics dm = MainScreen.thiz.getResources().getDisplayMetrics();
				
		this.pview = new int[this.SXP * this.SYP];
		this.bitmap = Bitmap.createBitmap(this.SYP, this.SXP, Bitmap.Config.ARGB_8888);
		
		this.adapter = new AdjustmentsAdapter();
		
		if (this.adjustments == null)
		{
			this.adjustments = new ArrayList<Adjustment>();;
				
			this.setupAdjustments();
		}
			
		this.setupPresets();
		this.adjustmentsList = ((GridView)postProcessingView.findViewById(R.id.adjustments_list));
		this.adjustmentsList.setAdapter(adapter);
		this.adjustmentsList.setOnItemClickListener(this);
			
		this.adjustmentsSeekBar.setOnSeekBarChangeListener(this);
			
		if (((mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270))
				== (MainScreen.thiz.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
				&& (((mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270)) == (((float)dm.widthPixels / this.SYP) < ((float)dm.heightPixels / this.SXP))))
		{
			this.imageView.setScaleType(ScaleType.FIT_START);
		}
		else
		{
			this.imageView.setScaleType(ScaleType.FIT_CENTER);			
		}
		
		//Add bottom padding to adjustments icons if orientation is portrait
		if (((mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270))
				&& (MainScreen.thiz.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT))
		{
			postProcessingView.findViewById(R.id.adjustmentsRelative).setPadding(0, 0, 0, 
					(int)(Math.max(dm.heightPixels - this.SXP * Math.min(
							((float)dm.widthPixels) / this.SYP, 
							((float)dm.heightPixels) / this.SXP) - PRESET_ICONS_SIZE * dm.density, 0.0f) + 4 * dm.density));
		}
		
		
		this.imageView.setOnClickListener(this);
		this.buttonTrash.setOnClickListener(this);
		this.buttonSave.setOnClickListener(this);
			
		this.presetsAdapter = new PresetsAdapter();
		this.presetsGallery.setAdapter(this.presetsAdapter);
		this.presetsGallery.setOnItemSelectedListener(this);
		this.presetsGallery.setOnItemClickListener(this);
		
		this.presetSelection = CUSTOM_PRESET_POSITION;
			
		this.presetsGallery.setSelection(CUSTOM_PRESET_POSITION);
				
//		if (this.selection >= 0)
//		{
//			this.showSeekBar();
//		}
		
		this.requestPreviewUpdate();		
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK && MainScreen.thiz.findViewById(R.id.postprocessingLayout).getVisibility() == View.VISIBLE)
		{
			if (this.selection != -1)
			{
				hideSeekBar();
			}
			else
			{		
				//fireUpTrashDialog();
				AlmaShotHDR.HDRFreeInstance();
			    AlmaShotHDR.Release();

				Message msg2 = new Message();
	    		msg2.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
	    		msg2.what = PluginManager.MSG_BROADCAST;
	    		MainScreen.H.sendMessage(msg2);
	    		
	    		MainScreen.guiManager.lockControls = false;
	    		
	    		postProcessingRun = false;
	    		
	    		MainScreen.H.sendEmptyMessage(PluginManager.MSG_POSTPROCESSING_FINISHED);
			}
			
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	private void fireUpTrashDialog()
	{
		this.trashDialog = new AlertDialog.Builder(MainScreen.thiz).create();

		this.trashDialog.setTitle(R.string.adjustments_trashDialog_title);	
		this.trashDialog.setIcon(R.drawable.alert_dialog_icon);
		TextView textView = new TextView(MainScreen.thiz);
		textView.setText(R.string.adjustments_trashDialog_content);
		textView.setPadding(
				(int)(MainScreen.thiz.getResources().getDisplayMetrics().density * 8), 
				(int)(MainScreen.thiz.getResources().getDisplayMetrics().density * 5), 
				(int)(MainScreen.thiz.getResources().getDisplayMetrics().density * 8), 
				(int)(MainScreen.thiz.getResources().getDisplayMetrics().density * 5));
		textView.setTextSize((int)(MainScreen.thiz.getResources().getDisplayMetrics().density * 12));
		this.trashDialog.setView(textView);
		
		
//		this.trashDialog.setButton(
//				MainScreen.thiz.getResources().getString(R.string.adjustments_trashDialog_positive), 
//				new DialogInterface.OnClickListener() 
//				{
//				   public void onClick(DialogInterface dialog, int which) 
//				   {
//					   
//					   if (Processing.yuv != 0)
//				       {
//					       Processing.yuv = 0;
//				       }
//			    		
//					   AlmaShot.HDRFreeInstance();
//			    		
//				       AlmaShot.Release();
//					   
//// <!-- -+-
//					   ActivityAds.maintainAds(ActivityAdjustments.this);
//// -+- -->	
//				       
//					   ActivityAdjustments.this.finish();
//				   }
//				});
//		
//		this.trashDialog.setButton2(
//				MainScreen.thiz.getResources().getString(R.string.adjustments_trashDialog_negative),
//				new DialogInterface.OnClickListener() 
//				{
//					public void onClick(DialogInterface dialog,	int which) 
//					{
//						dialog.cancel();
//					}
//				});

		this.trashDialog.show();
	}

	
	private Bitmap createThumbnail()
	{
		int small = (int)(MainScreen.thiz.getResources().getDisplayMetrics().density * PRESET_ICONS_SIZE * (1.0f / PRESET_ICONS_CROP_PART));
		int radius = (int)(small * PRESET_ICONS_ROUND_RADIUS * PRESET_ICONS_CROP_PART);
			
		if (this.SXP > this.SYP)
		{			
			return getRoundedCornerBitmap(
					Bitmap.createScaledBitmap(
							this.bitmap, 
							small, 
							(int)(this.SXP * ((float)small / this.SYP)), 
							true),
					radius);
		}
		else
		{
			return getRoundedCornerBitmap(
					Bitmap.createScaledBitmap(
							this.bitmap, 
							(int)(this.SYP * ((float)small / this.SXP)), 
							small,
							true),
					radius);
		}
	}
	
	private void setupAdjustments()
	{
		this.adjustments.add(new Adjustment(
				ADJUSTMENT_CODE_EXPOSURE,
				MainScreen.thiz.getResources().getString(R.string.adjustments_exposure), 
				getExposure(false), 
				-1, 
				-100, 
				MainScreen.thiz.getResources().getDrawable(R.drawable.adjustments_expo)));
		
		this.adjustments.add(new Adjustment(
				ADJUSTMENT_CODE_VIVIDNESS,
				MainScreen.thiz.getResources().getString(R.string.adjustments_vividness), 
				getVividness(false), 
				-1, 
				-100, 
				MainScreen.thiz.getResources().getDrawable(R.drawable.adjustments_vividness)));
		
		this.adjustments.add(new Adjustment(
				ADJUSTMENT_CODE_CONTRAST,
				MainScreen.thiz.getResources().getString(R.string.adjustments_contrast), 
				getContrast(false), 
				-1, 
				-100, 
				MainScreen.thiz.getResources().getDrawable(R.drawable.adjustments_contrast)));
		
		this.adjustments.add(new Adjustment(
				ADJUSTMENT_CODE_MICROCONTRAST,
				MainScreen.thiz.getResources().getString(R.string.adjustments_microcontrast), 
				getMicrocontrast(false), 
				-1, 
				-100, 
				MainScreen.thiz.getResources().getDrawable(R.drawable.adjustments_microcontrast)));
	}
	
	private void setupPresets()
	{
		this.presets.clear();
		this.presets.add(this.createAdjustmentPresetWithThumbnail(
				MainScreen.thiz.getResources().getString(R.string.adjustments_preset_artistic), 0, -25, 1, -100, 2, -100, 3, -75));
		this.presets.add(this.createAdjustmentPresetWithThumbnail(
				MainScreen.thiz.getResources().getString(R.string.adjustments_preset_bnw), 0, -25, 1, -0, 2, -50, 3, -50));
		this.presets.add(this.createAdjustmentPresetWithThumbnail(
				MainScreen.thiz.getResources().getString(R.string.adjustments_preset_natural), 0, -25, 1, -50, 2, -75, 3, -25));
		this.presets.add(this.createAdjustmentPresetWithThumbnail(
				MainScreen.thiz.getResources().getString(R.string.adjustments_preset_candy), 0, -25, 1, -63, 2, -75, 3, -75));
	
		if (this.preset_custom == null)
		{
			this.presets.add(
					new AdjustmentsPreset(
							MainScreen.thiz.getResources().getString(R.string.adjustments_preset_custom),
							null,
							0, getExposure(false), 
							1, getVividness(false), 
							2, getContrast(false), 
							3, getMicrocontrast(false)));
		}
		else
		{
			this.presets.add(this.preset_custom);
		}
			
		this.presets.add(this.createAdjustmentPresetWithThumbnail(
				MainScreen.thiz.getResources().getString(R.string.adjustments_preset_vanilla), 0, -25, 1, -25, 2, -15, 3, -75));
		this.presets.add(this.createAdjustmentPresetWithThumbnail(
				MainScreen.thiz.getResources().getString(R.string.adjustments_preset_xerox), 0, -100, 1, -0, 2, -50, 3, -50));
		this.presets.add(this.createAdjustmentPresetWithThumbnail(
				MainScreen.thiz.getResources().getString(R.string.adjustments_preset_neon), 0, -66, 1, -100, 2, -100, 3, -0));
	}
	
	private AdjustmentsPreset createAdjustmentPresetWithThumbnail(String title, Integer... sets)
	{
		int exposure = -50;
		int vividness = -50;
		int contrast = -50;
		int microcontrast = -50;
		
		for (int i = 0; i < sets.length / 2; i++)
		{
			switch (sets[i * 2])
			{
			case ADJUSTMENT_CODE_EXPOSURE:
				exposure = sets[i * 2 + 1];
				break;
			case ADJUSTMENT_CODE_VIVIDNESS:
				vividness = sets[i * 2 + 1];
				break;
			case ADJUSTMENT_CODE_CONTRAST:
				contrast = sets[i * 2 + 1];
				break;
			case ADJUSTMENT_CODE_MICROCONTRAST:
				microcontrast = sets[i * 2 + 1];
				break;
			}
		}
		
    	AlmaShotHDR.HDRPreview2a(
    			mImageWidth,
    			mImageHeight,
    			pview,
    			(mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270),
    			exposure, vividness, contrast, microcontrast,
    			mCameraMirrored);
    	
		this.bitmap.setPixels(this.pview, 0, this.SYP, 0, 0, this.SYP, this.SXP);
    	
		System.gc();
		
		return new AdjustmentsPreset(title, this.createThumbnail(), sets);
	}
	
 	private void showSeekBar()
	{
		if (this.selection >= 0 && this.selection < this.adjustments.size())
		{
			this.fillSeekBar();
			
			((RelativeLayout)MainScreen.thiz.findViewById(R.id.adjustments_seekbar_holder)).setVisibility(View.VISIBLE);
		}
	}
	private void hideSeekBar()
	{
		this.selection = -1;
		
		this.adapter.notifyDataSetChanged();
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.adjustments_seekbar_holder)).setVisibility(View.GONE);
	}
	private void fillSeekBar()
	{
		if (this.selection >= 0 && this.selection < this.adjustments.size())
		{
			this.adjustmentsSeekBar.setMax(this.adjustments.get(this.selection).getProgressMax());
			
			this.adjustmentsSeekBar.setProgress(this.adjustments.get(this.selection).getProgress());
			
			this.adjustmentsTextView.setText(this.adjustments.get(this.selection).getTitle());
		}
	}
	
	private void selectPreset(int position)
	{
		this.presetSelection = position;
		
		for (int i = 0; i < this.presets.get(position).getSetsCount(); i++)
		{
			for (int j = 0; j < this.adjustments.size(); j++)
			{
				if (this.adjustments.get(j).getCode() == this.presets.get(position).getSetId(i))
				{
					this.adjustments.get(j).setValue(this.presets.get(position).getSetValue(i));
				}
			}
		}
		
		this.fillSeekBar();
		
		this.requestPreviewUpdate();

// <!-- -+-		
		if (position != CUSTOM_PRESET_POSITION)
		{
			this.modified = true;
		}
// -+- -->	
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
	{	
		if (parent == this.adjustmentsList)
		{
			this.selection = position;
			
			this.adapter.notifyDataSetChanged();
			
			this.showSeekBar();
		}
		else if (parent == this.presetsGallery)
		{
			this.selectPreset(position);
		}
	}
	
	@Override
	public void onClick(View v) 
	{
		if (v == this.buttonTrash)
		{
			cancelAllTasks();
			AlmaShotHDR.HDRFreeInstance();
		    AlmaShotHDR.Release();

			//fireUpTrashDialog();
			Message msg2 = new Message();
    		msg2.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
    		msg2.what = PluginManager.MSG_BROADCAST;
    		MainScreen.H.sendMessage(msg2);
    		
    		MainScreen.guiManager.lockControls = false;
    		
    		postProcessingRun = false;
    		
    		MainScreen.H.sendEmptyMessage(PluginManager.MSG_POSTPROCESSING_FINISHED);
		}
		else if (v == this.buttonSave)
		{
			cancelAllTasks();
			saveButtonPressed = true;
			//saveImage();
			new SaveTask(MainScreen.thiz).execute();
			this.buttonTrash.setVisibility(View.GONE);
			this.buttonSave.setVisibility(View.GONE);
		}
		else if (v == this.imageView)
		{
			this.hideSeekBar();
		}
	}
	
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
	{
		if (this.selection >= 0 && this.selection < this.adjustments.size() && fromUser)
		{
// <!-- -+-
			this.modified = true;
// -+- -->
			
			this.presetSelection = CUSTOM_PRESET_POSITION;
				
			this.presetsGallery.setSelection(CUSTOM_PRESET_POSITION);
			
			this.adjustments.get(this.selection).onProgressChanged(progress);
			
			this.presets.get(CUSTOM_PRESET_POSITION).saveSets(this.adjustments);
			
			this.requestPreviewUpdate();
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
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) 
	{
		this.selectPreset(position);
	}
	@Override
	public void onNothingSelected(AdapterView<?> parent) 
	{
		
	}
	
	
	
	
	private void saveImage()
	{
		if (this.previewTaskCurrent == null)
		{
//			this.saving = true;
//			this.mAsyncTaskManager.setupTask(new AdjustmentsSavingTask(MainScreen.thiz.getResources()));
			HDRProcessing();
			Log.e("HDR", "HDRProcessing success");
			
			if(mDisplayOrientationOnStartProcessing == 180 || mDisplayOrientationOnStartProcessing == 270)
			{
				byte[] dataRotated = new byte[yuv.length];
				ImageConversion.TransformNV21(yuv, dataRotated, mImageWidth, mImageHeight, 1, 1, 0);
				
				yuv = dataRotated;
			}
			
			int frame_len = yuv.length;
			int frame = SwapHeap.SwapToHeap(yuv);
			
			PluginManager.getInstance().addToSharedMem("sessionID", String.valueOf(sessionID));
			
			PluginManager.getInstance().addToSharedMem("resultfromshared"+Long.toString(sessionID), "true");
		
			PluginManager.getInstance().addToSharedMem("writeorientationtag"+Long.toString(sessionID), "false");
			PluginManager.getInstance().addToSharedMem("resultframeorientation1" + String.valueOf(sessionID), String.valueOf(mDisplayOrientationOnStartProcessing));
			PluginManager.getInstance().addToSharedMem("amountofresultframes"+Long.toString(sessionID), "1");
			PluginManager.getInstance().addToSharedMem("resultframe1"+Long.toString(sessionID), String.valueOf(frame));
			PluginManager.getInstance().addToSharedMem("resultframelen1"+Long.toString(sessionID), String.valueOf(frame_len));
			
			PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(sessionID), String.valueOf(MainScreen.getSaveImageWidth()));
	    	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(sessionID), String.valueOf(MainScreen.getSaveImageHeight()));
		}
	}
	
	protected void notifyPreviewRecounted()
	{
		if (this.saveButtonPressed)
		{
			this.previewTaskCurrent = null;
			this.previewTaskPending = null;
			
			//this.saveImage();
			new SaveTask(MainScreen.thiz).execute();
		}
		else
		{			
			this.imageView.invalidate();
			
			if (this.presetSelection == CUSTOM_PRESET_POSITION && this.previewTaskPending == null)
			{
				System.gc();
				this.presets.get(CUSTOM_PRESET_POSITION).setThumbnail(this.createThumbnail());
			}
			
			this.presetsAdapter.notifyDataSetChanged();
			
			if (this.previewTaskPending != null)
			{
				this.previewTaskCurrent = this.previewTaskPending;
				this.previewTaskCurrent.execute();
				
				this.previewTaskPending = null;
			}
			else
			{
				this.previewTaskCurrent = null;
				
				this.imageView.setImageBitmap(this.bitmap);
			}			
			
			MainScreen.thiz.findViewById(R.id.adjustments_trashButton).setVisibility(View.VISIBLE);
			MainScreen.thiz.findViewById(R.id.adjustments_saveButton).setVisibility(View.VISIBLE);
		}
	}
	
	protected void requestPreviewUpdate()
	{
//		if (!this.mAsyncTaskManager.isWorking())
//		{
//			if (this.previewTaskCurrent == null)
//			{
//				this.previewTaskCurrent = new AdjustmentsPreviewTask();
//				this.previewTaskCurrent.execute();
//			}
//			else
//			{
//				if (this.previewTaskPending == null)
//				{
//					this.previewTaskPending = new AdjustmentsPreviewTask();
//				}
//			}
//		}
		
		if (this.previewTaskCurrent == null)
		{
			this.previewTaskCurrent = new AdjustmentsPreviewTask();
			this.previewTaskCurrent.execute();
		}
		else
		{
			if (this.previewTaskPending == null)
			{
				this.previewTaskPending = new AdjustmentsPreviewTask();
			}
		}
	}
	
	protected void cancelAllTasks()
	{
		if (this.previewTaskCurrent != null)
		{
			this.previewTaskCurrent.cancel(false);
		}
	}
	
	private class SaveTask extends AsyncTask<Void, Void, Void>
	{
	    private ProgressDialog mSavingDialog;
	    
		public SaveTask(Context context)
	    {
	    	this.mSavingDialog = new ProgressDialog(context);
	    	this.mSavingDialog.setIndeterminate(true);
	    	this.mSavingDialog.setCancelable(false);
	    	this.mSavingDialog.setMessage("Saving");//(context.getResources().getString(R.string.please_wait));	  
	    }
	    
	    @Override
	    protected void onPreExecute()
	    {  	
	    	this.mSavingDialog.show();
	    }
	    
		@Override
		protected Void doInBackground(Void... params)
		{
			HDRProcessingPlugin.this.saveImage();
			
			return null;
		}
		
	    @Override
	    protected void onPostExecute(Void v)
	    {
			this.mSavingDialog.hide();
			
			AlmaShotHDR.HDRFreeInstance();
		    AlmaShotHDR.Release();

			Message msg2 = new Message();
    		msg2.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
    		msg2.what = PluginManager.MSG_BROADCAST;
    		MainScreen.H.sendMessage(msg2);
    		
    		MainScreen.guiManager.lockControls = false;
    		
    		MainScreen.H.sendEmptyMessage(PluginManager.MSG_POSTPROCESSING_FINISHED);
	    } 
	}
}
