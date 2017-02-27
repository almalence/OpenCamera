package com.almalence.widget;

/* <!-- +++
import com.almalence.opencam_plus.R;
+++ --> */
//<!-- -+-
import com.almalence.opencam.R;
//-+- -->

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

public class OpenCameraSolidWidgetProvider extends AppWidgetProvider
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
            	boolean barcodeValue = intent.getBooleanExtra(OpenCameraWidgetConfigureActivity.EXTRA_BARCODE, false);
            	
            	Bundle extras = new Bundle();
    	        extras.putString(OpenCameraWidgetConfigureActivity.EXTRA_ITEM, modeName);    	        
	        	extras.putBoolean(OpenCameraWidgetConfigureActivity.EXTRA_TORCH, torchValue);
	        	extras.putBoolean(OpenCameraWidgetConfigureActivity.EXTRA_BARCODE, barcodeValue);
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
            	Intent configIntent = new Intent(context, OpenCameraSolidWidgetConfigureActivity.class);    	        
            	configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
    	        configIntent.setData(Uri.withAppendedPath(Uri.parse("abc" + "://widget/id/"), String.valueOf(widgetID)));
    	        context.startActivity(configIntent);
            }
        }
    	else if(intent.getAction().contentEquals("com.sec.android.widgetapp.APPWIDGET_RESIZE"))
    	{
    		handleResizeSignal(context, intent);
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++)
        {
        	int appWidgetId = appWidgetIds[i];
	            
        	RemoteViews remoteViews = null;
        	remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_opencamera_solid);
            
            /// set intent for widget service that will create the views
            Intent serviceIntent = new Intent(context, OpenCameraSolidWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))); // embed extras so they don't get ignored
            remoteViews.setRemoteAdapter(R.id.widgetSolidGrid, serviceIntent);
            remoteViews.setEmptyView(R.id.widgetSolidGrid, R.id.widgetSolidEmptyView);
            
            Intent intent = new Intent(context, OpenCameraSolidWidgetProvider.class);
            intent.setAction(ACTION_START_ACTIVITY);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            remoteViews.setPendingIntentTemplate(R.id.widgetSolidGrid, pendingIntent);
        	
//        	Intent configIntent = new Intent(context, OpenCameraSolidWidgetConfigureActivity.class);    	        
//        	configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//	        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
//	        configIntent.setData(Uri.withAppendedPath(Uri.parse("abc" + "://widget/id/"), String.valueOf(appWidgetId)));
//	        
//        	PendingIntent pendingIntent2 = PendingIntent.getActivity(context, 0, configIntent, 0);
//	        remoteViews.setOnClickPendingIntent(R.id.fakeImage, pendingIntent2);
            
            // update widget
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
    
    public static RemoteViews buildRemoteViews(Context context, int appWidgetId)
    {
    	RemoteViews remoteViews = null;
    	remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_opencamera_solid);
        
        /// set intent for widget service that will create the views
        Intent serviceIntent = new Intent(context, OpenCameraSolidWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))); // embed extras so they don't get ignored
        remoteViews.setRemoteAdapter(R.id.widgetSolidGrid, serviceIntent);
        remoteViews.setEmptyView(R.id.widgetSolidGrid, R.id.widgetSolidEmptyView);
        
        Intent intent = new Intent(context, OpenCameraSolidWidgetProvider.class);
        intent.setAction(ACTION_START_ACTIVITY);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        remoteViews.setPendingIntentTemplate(R.id.widgetSolidGrid, pendingIntent);
        
        
//        Intent configIntent = new Intent(context, OpenCameraSolidWidgetConfigureActivity.class);    	        
//    	configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
//        configIntent.setData(Uri.withAppendedPath(Uri.parse("abc" + "://widget/id/"), String.valueOf(appWidgetId)));
//        
//    	PendingIntent pendingIntent2 = PendingIntent.getActivity(context, 0, configIntent, 0);
//        remoteViews.setOnClickPendingIntent(R.id.fakeImage, pendingIntent2);
        
        return remoteViews;   
    }
    
    @TargetApi(16)
    @Override
    public void onAppWidgetOptionsChanged(Context context,
                                          AppWidgetManager appWidgetManager,
                                          int appWidgetId,
                                          Bundle newOptions)
    {
    	super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    	
    	RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_opencamera_solid);
        
        /// set intent for widget service that will create the views
        Intent serviceIntent = new Intent(context, OpenCameraSolidWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))); // embed extras so they don't get ignored
        remoteViews.setRemoteAdapter(R.id.widgetSolidGrid, serviceIntent);
        remoteViews.setEmptyView(R.id.widgetSolidGrid, R.id.widgetSolidEmptyView);
        
        Intent intent = new Intent(context, OpenCameraSolidWidgetProvider.class);
        intent.setAction(ACTION_START_ACTIVITY);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        remoteViews.setPendingIntentTemplate(R.id.widgetSolidGrid, pendingIntent);
  
    	// update widget
    	appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }
    
    @TargetApi(16)
    private void handleResizeSignal(Context context, Intent intent)
    {
    	AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        int appWidgetId = intent.getIntExtra("widgetId", 0);
        int widgetSpanX = intent.getIntExtra("widgetspanx", 0);
        int widgetSpanY = intent.getIntExtra("widgetspany", 0);        
        
        if(appWidgetId > 0 && widgetSpanX > 0 && widgetSpanY > 0) {
            Bundle newOptions = new Bundle();
            // We have to convert these numbers for future use
            newOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, widgetSpanY * 74);
            newOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widgetSpanX * 74);
            
            appWidgetManager.updateAppWidgetOptions(appWidgetId, newOptions);

            onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        }
    }
}