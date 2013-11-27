package com.almalence.opencam;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class OpenCameraWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new OpenCameraRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class OpenCameraRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private static int mCount = 13;
    private List<OpenCameraWidgetItem> mWidgetItems = new ArrayList<OpenCameraWidgetItem>();
    private Context mContext;
    private int mAppWidgetId;
    
    private final static List<String> modeNames = new ArrayList<String>()
	{
    	{
    		add("single");
    		add("selftimer");
    		add("burstmode");
    		add("expobracketing");
    		add("hdrmode");
    		add("nightmode");
    		add("video");
    		add("pixfix");
    		add("movingobjects");
    		add("groupshot");
    		add("sequence");
    		add("panorama_augmented");
    	}
    };
    		
	private final static List<Integer> modeIcons = new ArrayList<Integer>()
	{
		{
			add(R.drawable.gui_almalence_mode_single);
			add(R.drawable.gui_almalence_mode_selftimer);
			add(R.drawable.gui_almalence_mode_burst);
			add(R.drawable.gui_almalence_mode_expobracketing);
			add(R.drawable.gui_almalence_mode_hdr);
			add(R.drawable.gui_almalence_mode_night);
			add(R.drawable.gui_almalence_mode_video);
			add(R.drawable.gui_almalence_mode_backintime);
			add(R.drawable.gui_almalence_mode_moving);
			add(R.drawable.gui_almalence_mode_groupshot);
			add(R.drawable.gui_almalence_mode_sequence);
			add(R.drawable.gui_almalence_mode_panorama);
		}
	};

    public OpenCameraRemoteViewsFactory(Context context, Intent intent)
    {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }
    
    

    public void onCreate() {
        // In onCreate() you setup any connections / cursors to your data source. Heavy lifting,
        // for example downloading or creating content etc, should be deferred to onDataSetChanged()
        // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.
    	if(OpenCameraWidgetConfigureActivity.modeGridAssoc != null)
    	{
    		Set<Integer> keys = OpenCameraWidgetConfigureActivity.modeGridAssoc.keySet();
    		mCount = keys.size()+1;
    		Iterator<Integer> it = keys.iterator();
    		while(it.hasNext())
    		{
    			int gridIndex = it.next();
    			Mode mode = OpenCameraWidgetConfigureActivity.modeGridAssoc.get(gridIndex); 
    			OpenCameraWidgetItem modeInfo = new OpenCameraWidgetItem(mode.modeID, MainScreen.thiz.getResources().getIdentifier(
																					  mode.icon, "drawable",
																					  MainScreen.thiz.getPackageName()));
    			mWidgetItems.add(modeInfo);
    		}
    	}

        // We sleep for 3 seconds here to show how the empty view appears in the interim.
        // The empty view is set in the StackWidgetProvider and should be a sibling of the
        // collection view.
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    public void onDestroy() {
        // In onDestroy() you should tear down anything that was setup for your data source,
        // eg. cursors, connections, etc.
        mWidgetItems.clear();
    }

    public int getCount() {
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
	        
	        Intent fillInIntent = new Intent(mContext, OpenCameraWidgetConfigureActivity.class);	        
	        rv.setOnClickFillInIntent(R.id.modeSelectLayout, fillInIntent);
    	}
    	else
    	{
	    	OpenCameraWidgetItem item = mWidgetItems.get(position);
	        // We construct a remote views item based on our widget item xml file, and set the
	        // text based on the position.
	        rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_opencamera_mode_grid_element);
	        //rv.setTextViewText(R.id.modeText, mWidgetItems.get(position).modeName);
	        rv.setImageViewResource(R.id.modeImage, item.modeIconID);
	
	        // Next, we set a fill-intent which will be used to fill-in the pending intent template
	        // which is set on the collection view in StackWidgetProvider.
	        Bundle extras = new Bundle();
	        extras.putString(MainScreen.EXTRA_ITEM, item.modeName);
	        Intent fillInIntent = new Intent(mContext, MainScreen.class);
	        fillInIntent.putExtras(extras);
	        rv.setOnClickFillInIntent(R.id.modeSelectLayout, fillInIntent);
    	}
        
     // set intent for item click (opens main activity)
//        Intent viewIntent = new Intent(mContext, MainScreen.class);
//        viewIntent.setAction(Intent.ACTION_MAIN);
//        viewIntent.putExtra(MainScreen.EXTRA_ITEM, item.modeName);
//        viewIntent.setData(Uri.parse(viewIntent.toUri(Intent.URI_INTENT_SCHEME)));
//        
//        PendingIntent viewPendingIntent = PendingIntent.getActivity(mContext, 0, viewIntent, 0);        
//        rv.setOnClickPendingIntent(R.id.modeSelectLayout, viewPendingIntent);
        
     

        // You can do heaving lifting in here, synchronously. For example, if you need to
        // process an image, fetch something from the network, etc., it is ok to do it here,
        // synchronously. A loading view will show up in lieu of the actual contents in the
        // interim.
//        try {
//            System.out.println("Loading view " + position);
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        // Return the remote views object.
        return rv;
    }

    public RemoteViews getLoadingView() {
        // You can create a custom loading view (for instance when getViewAt() is slow.) If you
        // return null here, you will get the default loading view.
        return null;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        // This is triggered when you call AppWidgetManager notifyAppWidgetViewDataChanged
        // on the collection view corresponding to this factory. You can do heaving lifting in
        // here, synchronously. For example, if you need to process an image, fetch something
        // from the network, etc., it is ok to do it here, synchronously. The widget will remain
        // in its current state while work is being done here, so you don't need to worry about
        // locking up the widget.
    }
}