package com.almalence.widget;

/* <!-- +++
import com.almalence.opencam_plus.R;
+++ --> */
//<!-- -+-
import com.almalence.opencam.R;
//-+- -->

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class OpenCameraFullSolidWidgetProvider extends AppWidgetProvider
{	
	public static final String ACTION_START_ACTIVITY = "startActivity";
	public static final String BROADCAST_PARAM_IS_MODE = "itemType";
	
	@Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onReceive(final Context context, Intent intent)
    {
    	if(ACTION_START_ACTIVITY.equals(intent.getAction()))
    	{
            boolean isModeCall = intent.getBooleanExtra(BROADCAST_PARAM_IS_MODE, false);            
            if(isModeCall)
            {
            	String modeName = intent.getStringExtra(OpenCameraWidgetConfigureActivity.EXTRA_ITEM);
            	boolean torchValue = intent.getBooleanExtra(OpenCameraWidgetConfigureActivity.EXTRA_TORCH, false);
            	
            	Bundle extras = new Bundle();
    	        extras.putString(OpenCameraWidgetConfigureActivity.EXTRA_ITEM, modeName);    	        
	        	extras.putBoolean(OpenCameraWidgetConfigureActivity.EXTRA_TORCH, torchValue);	
    	        //Intent modeIntent = new Intent(context, MainScreen.class);
    	        //Intent modeIntent = new Intent("com.almalence.opencam");
	        	Intent modeIntent = context.getPackageManager().getLaunchIntentForPackage("com.almalence.opencam_plus");		        	
	        	if(modeIntent != null)
	        	{
	    	        modeIntent.putExtras(extras);
	    	        modeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    	        context.startActivity(modeIntent);
	        	}
	        	else
	        	{
	        		modeIntent = context.getPackageManager().getLaunchIntentForPackage("com.almalence.opencam");
	        		if(modeIntent != null)
		        	{
		    	        modeIntent.putExtras(extras);
		    	        modeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    	        context.startActivity(modeIntent);
		        	}
	        		else
	        		{		        		
		        		try
    		           	{
//		        			Intent shopIntent = context.getPackageManager().getLaunchIntentForPackage("com.android.vending");
//    		               	//Intent shopIntent = new Intent(Intent.ACTION_VIEW);
//		        			shopIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		              		shopIntent.setData(Uri.parse("market://details?id=com.almalence.opencam"));
//		              		context.startActivity(shopIntent);
		              		
		              		Intent shopIntent = new Intent(Intent.ACTION_VIEW);
		              		shopIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		              		shopIntent.setData(Uri.parse("market://details?id=com.almalence.opencam"));
		               		context.startActivity(shopIntent);
    		       	        //context.startActivity(intent);
    		           	}
    		           	catch(ActivityNotFoundException e)
    		           	{
    		           		Log.e("Widget", "Unable to start Google Play");
    		           		return;
    		           	}
	        		}
	        	}            
            }
            else
            {
            	int widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
            	Intent configIntent = new Intent(context, OpenCameraWidgetConfigureActivity.class);    	        
            	configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
    	        configIntent.setData(Uri.withAppendedPath(Uri.parse("abc" + "://widget/id/"), String.valueOf(widgetID)));
    	        context.startActivity(configIntent);
            }
        }    	
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++)
        {
        	int appWidgetId = appWidgetIds[i];
	            
        	RemoteViews remoteViews = null;
        	remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_opencamera_full_solid);
            
            /// set intent for widget service that will create the views
            Intent serviceIntent = new Intent(context, OpenCameraFullSolidWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))); // embed extras so they don't get ignored
            remoteViews.setRemoteAdapter(R.id.widgetSolidGrid, serviceIntent);
            remoteViews.setEmptyView(R.id.widgetSolidGrid, R.id.widgetSolidEmptyView);
            
            Intent intent = new Intent(context, OpenCameraFullSolidWidgetProvider.class);
            intent.setAction(ACTION_START_ACTIVITY);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            remoteViews.setPendingIntentTemplate(R.id.widgetSolidGrid, pendingIntent);
            
            // update widget
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}