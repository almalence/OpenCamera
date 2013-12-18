package com.almalence.opencam;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class OpenCameraWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new OpenCameraRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class OpenCameraRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private static int mCount = 0;
    private List<OpenCameraWidgetItem> mWidgetItems = new ArrayList<OpenCameraWidgetItem>();
    private Context mContext;
    private int mAppWidgetId;

    public OpenCameraRemoteViewsFactory(Context context, Intent intent)
    {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }    
    

    public void onCreate()
    {
    	fillWidgetItemList();
    }

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
    	if(position == mCount-1)
    	{    		
    		rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_opencamera_mode_grid_element);
	        //rv.setTextViewText(R.id.modeText, mWidgetItems.get(position).modeName);
	        rv.setImageViewResource(R.id.modeImage, R.drawable.opencamera_widget_settings);
	
	        // Next, we set a fill-intent which will be used to fill-in the pending intent template
	        // which is set on the collection view in StackWidgetProvider.	        
	        
	        //Intent configIntent = new Intent(mContext, OpenCameraWidgetConfigureActivity.class);
	        Intent configIntent = new Intent();
	        configIntent.putExtra(OpenCameraWidgetProvider.BROADCAST_PARAM_IS_MODE, false);
	        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
	        configIntent.setData(Uri.withAppendedPath(Uri.parse("abc" + "://widget/id/"), String.valueOf(mAppWidgetId)));
	        rv.setOnClickFillInIntent(R.id.modeSelectLayout, configIntent);
    	}
    	else if(mWidgetItems != null && mWidgetItems.size() > position)
    	{
	    	OpenCameraWidgetItem item = mWidgetItems.get(position);
	    	
	    	if(item.modeName.contains("hide"))
	    		return null;
	        // We construct a remote views item based on our widget item xml file, and set the
	        // text based on the position.
	        rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_opencamera_mode_grid_element);
	        rv.setImageViewResource(R.id.modeImage, item.modeIconID);
	
	        // Next, we set a fill-intent which will be used to fill-in the pending intent template
	        // which is set on the collection view in StackWidgetProvider.
	        Bundle extras = new Bundle();
	        extras.putBoolean(OpenCameraWidgetProvider.BROADCAST_PARAM_IS_MODE, true);
	        if(item.modeName.contains("torch"))
	        	extras.putString(MainScreen.EXTRA_ITEM, "single");
	        else
	        	extras.putString(MainScreen.EXTRA_ITEM, item.modeName);
	        if(item.isTorchOn)
	        	extras.putString(MainScreen.EXTRA_TORCH, "on");	
	        //Intent fillInIntent = new Intent(mContext, MainScreen.class);
	        Intent fillInIntent = new Intent();
	        fillInIntent.putExtras(extras);
	        rv.setOnClickFillInIntent(R.id.modeSelectLayout, fillInIntent);
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
    		Set<Integer> keys = OpenCameraWidgetConfigureActivity.modeGridAssoc.keySet();
    		Iterator<Integer> it = keys.iterator();
    		while(it.hasNext())
    		{
    			int gridIndex = it.next();
    			OpenCameraWidgetItem modeInfo = OpenCameraWidgetConfigureActivity.modeGridAssoc.get(gridIndex);
    			if(!modeInfo.modeName.contains("hide"))
    				mWidgetItems.add(modeInfo);
    		}
    		
    		mCount = mWidgetItems.size() + 1;
    	}	
    }
}