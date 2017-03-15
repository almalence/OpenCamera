package com.almalence.widget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

import com.almalence.util.Util;
/* <!-- +++
import com.almalence.opencam_plus.R;
import com.almalence.opencam_plus.ConfigParser;
import com.almalence.opencam_plus.Mode;
+++ --> */
//<!-- -+-
import com.almalence.opencam.R;
import com.almalence.opencam.ConfigParser;
import com.almalence.opencam.Mode;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class OpenCameraFullSolidWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new OpenCameraFullSolidRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class OpenCameraFullSolidRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private static int mCount = 0;
    private List<OpenCameraWidgetItem> mWidgetItems = new ArrayList<OpenCameraWidgetItem>();
    private Context mContext;
    private int mAppWidgetId;
    
    SharedPreferences prefs;
    
	//public static Map<Integer, OpenCameraWidgetItem> modeGridAssoc;

    public OpenCameraFullSolidRemoteViewsFactory(Context context, Intent intent)
    {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }    
    

    @Override
    public void onCreate()
    {
    	prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
//    	if(modeGridAssoc == null)
//			modeGridAssoc = new Hashtable<Integer, OpenCameraWidgetItem>();
    	//mWidgetItems.clear();
    	AppWidgetManager.getInstance(mContext).notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.widgetSolidGrid);
    	//fillWidgetItemList();
    }

    @Override
    public void onDestroy()
    {
        // In onDestroy() you should tear down anything that was setup for your data source,
        // eg. cursors, connections, etc.
        mWidgetItems.clear();
        AppWidgetManager.getInstance(mContext).notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.widgetSolidGrid);
    }

    @Override
    public int getCount()
    {
        return mCount;
    }

    @Override
    public RemoteViews getViewAt(int position)
    {
        // position will always range from 0 to getCount() - 1.
    	RemoteViews rv = null;
//    	if(position == mCount-1)
//    	{    		
//    		rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_opencamera_mode_grid_element);
//	        //rv.setTextViewText(R.id.modeText, mWidgetItems.get(position).modeName);
//	        rv.setImageViewResource(R.id.modeImage, R.drawable.widget_settings);	        
//	
//	        // Next, we set a fill-intent which will be used to fill-in the pending intent template
//	        // which is set on the collection view in StackWidgetProvider.	        
//	        
//	        //Intent configIntent = new Intent(mContext, OpenCameraWidgetConfigureActivity.class);
//	        Intent configIntent = new Intent();
//	        configIntent.putExtra(OpenCameraWidgetProvider.BROADCAST_PARAM_IS_MODE, false);
//	        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
//	        configIntent.setData(Uri.withAppendedPath(Uri.parse("abc" + "://widget/id/"), String.valueOf(mAppWidgetId)));
//	        //rv.setOnClickFillInIntent(R.id.modeSelectLayout, configIntent);
//	        rv.setOnClickFillInIntent(R.id.modeImage, configIntent);
//    	}
//    	else if(mWidgetItems != null && mWidgetItems.size() > position)
    	if(mWidgetItems != null && mWidgetItems.size() > position)
    	{
	    	OpenCameraWidgetItem item = mWidgetItems.get(position);
	    	
	    	if(item.modeName.contains("hide"))
	    		return null;
   	
//			float elementHeight = -1;
//			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
//				elementHeight = calculateElementHeight();			
			
	        // We construct a remote views item based on our widget item xml file, and set the
	        // text based on the position.
	    	
    		rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_opencamera_full_solid_mode_grid_element);
        	rv.setImageViewResource(R.id.modeImage, item.modeIconID);	        
        
	        // Next, we set a fill-intent which will be used to fill-in the pending intent template
	        // which is set on the collection view in StackWidgetProvider.
	        Bundle extras = new Bundle();
	        extras.putBoolean(OpenCameraWidgetProvider.BROADCAST_PARAM_IS_MODE, true);
	        if(item.modeName.contains("torch"))
	        	extras.putString(OpenCameraWidgetConfigureActivity.EXTRA_ITEM, "single");
	        else
	        	extras.putString(OpenCameraWidgetConfigureActivity.EXTRA_ITEM, item.modeName);
	        if(item.isTorchOn)
	        	extras.putBoolean(OpenCameraWidgetConfigureActivity.EXTRA_TORCH, true);
	        if(item.modeName.contains("settings"))
	        {
	        	extras.putBoolean(OpenCameraWidgetProvider.BROADCAST_PARAM_IS_MODE, false);
	        	extras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
	        }
	        //Intent fillInIntent = new Intent(mContext, MainScreen.class);
	        Intent fillInIntent = new Intent();
	        fillInIntent.putExtras(extras);
	        //rv.setOnClickFillInIntent(R.id.modeSelectLayout, fillInIntent);
	        rv.setOnClickFillInIntent(R.id.modeImage, fillInIntent);
    	}

        // Return the remote views object.
        return rv;
    }

    @Override
    public RemoteViews getLoadingView()
    {
        // You can create a custom loading view (for instance when getViewAt() is slow.) If you
        // return null here, you will get the default loading view.
        return null;
    }

    public int getViewTypeCount()
    {
        return 1;
    }

    public long getItemId(int position)
    {
        return position;
    }

    public boolean hasStableIds()
    {
        return true;
    }

    
    @Override
    public void onDataSetChanged()
    {
    	mWidgetItems.clear();
    	fillWidgetItemList();
        // This is triggered when you call AppWidgetManager notifyAppWidgetViewDataChanged
        // on the collection view corresponding to this factory. You can do heaving lifting in
        // here, synchronously. For example, if you need to process an image, fetch something
        // from the network, etc., it is ok to do it here, synchronously. The widget will remain
        // in its current state while work is being done here, so you don't need to worry about
        // locking up the widget.
    }
    
    private synchronized void fillWidgetItemList()
    {
    	try {
			ConfigParser.getInstance().parse(mContext);
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
		List<Mode> modeList = ConfigParser.getInstance().getList();
		Iterator<Mode> it = modeList.iterator();
		while(it.hasNext())
		{
			Mode tmp = it.next();
			int iconID = mContext.getResources().getIdentifier(
					CameraController.isUseSuperMode() ? tmp.iconHAL : tmp.icon, "drawable",
					mContext.getPackageName());
			OpenCameraWidgetItem mode = new OpenCameraWidgetItem(tmp.modeID, iconID, false);
			mWidgetItems.add(mode);
			
//			if(tmp.modeName.contains("hdr") || tmp.modeName.contains("panorama")
//				|| tmp.modeName.contains("single") || tmp.modeName.contains("video") || tmp.modeName.contains("night"))
//			{  
//				int iconID = mContext.getResources().getIdentifier(
//	  					  tmp.icon, "drawable",
//	  					mContext.getPackageName());
//    			OpenCameraWidgetItem mode = new OpenCameraWidgetItem(tmp.modeID, iconID, false);
//    			mWidgetItems.add(mode);
//			}			
		}
		
//		int iconID = mContext.getResources().getIdentifier("gui_almalence_settings_flash_torch", "drawable", mContext.getPackageName());
//		OpenCameraWidgetItem mode = new OpenCameraWidgetItem("torch", iconID, true);
//		mWidgetItems.add(mode);
		
		mCount = mWidgetItems.size();
    }
}