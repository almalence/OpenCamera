package com.almalence.widget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.almalence.util.Util;
/* <!-- +++
import com.almalence.opencam_plus.R;
+++ --> */
//<!-- -+-
import com.almalence.opencam.R;
//-+- -->

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class OpenCameraWidgetService extends RemoteViewsService
{
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent)
    {
        return new OpenCameraRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class OpenCameraRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory
{
    private static int mCount = 0;
    private List<OpenCameraWidgetItem> mWidgetItems = new ArrayList<OpenCameraWidgetItem>();
    private Context mContext;
    private int mAppWidgetId;
    
    SharedPreferences prefs;
    
	//public static Map<Integer, OpenCameraWidgetItem> modeGridAssoc;

    public OpenCameraRemoteViewsFactory(Context context, Intent intent)
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
    	fillWidgetItemList();
    }


    @Override
    public void onDestroy()
    {
        // In onDestroy() you should tear down anything that was setup for your data source,
        // eg. cursors, connections, etc.
        mWidgetItems.clear();
    }

    public int getCount()
    {
        return mCount;
    }

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
	        rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_opencamera_mode_grid_element);
	        rv.setImageViewResource(R.id.modeImage, item.modeIconID);
	        
	        
	        // Next, we set a fill-intent which will be used to fill-in the pending intent template
	        // which is set on the collection view in StackWidgetProvider.
	        Bundle extras = new Bundle();
	        extras.putBoolean(OpenCameraWidgetProvider.BROADCAST_PARAM_IS_MODE, true);
	        if(item.modeName.contains("torch") || item.modeName.contains("barcode"))
	        	extras.putString(OpenCameraWidgetConfigureActivity.EXTRA_ITEM, "single");
	        else
	        	extras.putString(OpenCameraWidgetConfigureActivity.EXTRA_ITEM, item.modeName);
	        if(item.isTorchOn)
	        	extras.putBoolean(OpenCameraWidgetConfigureActivity.EXTRA_TORCH, true);
	        if(item.modeName.contains("barcode"))
        		extras.putBoolean(OpenCameraWidgetConfigureActivity.EXTRA_BARCODE, true);
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

    public void onDataSetChanged()
    {
    	fillWidgetItemList();
        // This is triggered when you call AppWidgetManager notifyAppWidgetViewDataChanged
        // on the collection view corresponding to this factory. You can do heaving lifting in
        // here, synchronously. For example, if you need to process an image, fetch something
        // from the network, etc., it is ok to do it here, synchronously. The widget will remain
        // in its current state while work is being done here, so you don't need to worry about
        // locking up the widget.
    }
    
    private void fillWidgetItemList()
    {
    	if(OpenCameraWidgetConfigureActivity.modeGridAssoc != null)
    	{
    		mWidgetItems.clear();
    		Set<Integer> unsorted_keys = OpenCameraWidgetConfigureActivity.modeGridAssoc.keySet();
    		List<Integer> keys = Util.asSortedList(unsorted_keys);
    		Iterator<Integer> it = keys.iterator();
    		while(it.hasNext())
    		{
    			int gridIndex = it.next();
    			OpenCameraWidgetItem modeInfo = OpenCameraWidgetConfigureActivity.modeGridAssoc.get(gridIndex);
    			if(!modeInfo.modeName.contains("hide"))
    				mWidgetItems.add(modeInfo);
    		}
    		
    		mCount = mWidgetItems.size();
    	}
    	else
    	{
    		mWidgetItems.clear();
    		int modeCount = 13;
    		for(int i = 0; i < modeCount; i++)
    		{
    			String modeID = prefs.getString("widgetAddedModeID" + String.valueOf(i), "hide");
    			if(!modeID.contains("hide"))
    			{
    				int modeIcon = prefs.getInt("widgetAddedModeIcon" + String.valueOf(i), 0);
    				OpenCameraWidgetItem modeInfo = new OpenCameraWidgetItem(modeID, modeIcon, modeID.contains("torch"));
    				
    				mWidgetItems.add(modeInfo);
    			}
    		}
    		
    		mCount = mWidgetItems.size();
    	}
    }
    
    private float calculateElementHeight()
    {
    	AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
		Bundle options = appWidgetManager.getAppWidgetOptions(mAppWidgetId);
		int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
		int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
		int maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
    
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager mng = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
		mng.getDefaultDisplay()
			.getMetrics(metrics);
		float ScreenDensity = metrics.density;
		float columnHeight = mContext.getResources().getDimension(R.dimen.widgetElementHeight);
		
		float maxElementHeight = (maxHeight - ScreenDensity*5)/2;
		Log.e("Widget", "Widget minHeight = " + minHeight);
		Log.e("Widget", "Widget maxHeight = " + maxHeight);
		Log.e("Widget", "Widget ScreenDensity = " + ScreenDensity);
		Log.e("Widget", "Widget height = " + minHeight*ScreenDensity);
		Log.e("Widget", "Element height = " + columnHeight);
		if(maxElementHeight < columnHeight)
			return maxElementHeight;			
		
    	return -1;
    }
}