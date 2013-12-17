package com.almalence.opencam;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class OpenCameraWidgetProvider extends AppWidgetProvider
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
    public void onReceive(Context context, Intent intent)
    {
    	if(ACTION_START_ACTIVITY.equals(intent.getAction()))
    	{
            boolean isModeCall = intent.getBooleanExtra(BROADCAST_PARAM_IS_MODE, false);            
            if(isModeCall)
            {
            	String modeName = intent.getStringExtra(MainScreen.EXTRA_ITEM);
            	String torchValue = intent.getStringExtra(MainScreen.EXTRA_TORCH);
            	
            	
            	Bundle extras = new Bundle();
    	        extras.putString(MainScreen.EXTRA_ITEM, modeName);
    	        if(torchValue != null && torchValue.contains("on"))
    	        	extras.putString(MainScreen.EXTRA_TORCH, "on");	
    	        Intent modeIntent = new Intent(context, MainScreen.class);    	        
    	        modeIntent.putExtras(extras);
    	        modeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	        context.startActivity(modeIntent);
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

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_opencamera);
            
            /// set intent for widget service that will create the views
            Intent serviceIntent = new Intent(context, OpenCameraWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))); // embed extras so they don't get ignored
            remoteViews.setRemoteAdapter(appWidgetId, R.id.widgetGrid, serviceIntent);
            remoteViews.setEmptyView(R.id.widgetGrid, R.id.widgetEmptyView);
            
            remoteViews.setInt(R.id.widgetGrid, "setBackgroundColor", 
                    OpenCameraWidgetConfigureActivity.bgColor);
            
            // set intent for item click (opens main activity)
//            Intent viewIntent = new Intent(context, MainScreen.class);
//            viewIntent.setAction(Intent.ACTION_MAIN);
//            //viewIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
//            //viewIntent.setData(Uri.parse(viewIntent.toUri(Intent.URI_INTENT_SCHEME)));
//            
//            PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);
//            remoteViews.setPendingIntentTemplate(R.id.widgetGrid, viewPendingIntent);
            //remoteViews.setOnClickPendingIntent(R.id.widgetGrid, viewPendingIntent);
            
//            Intent intent = new Intent(OpenCameraWidgetProvider.SETTING_BUTTON);
//	        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//	        remoteViews.setOnClickPendingIntent(R.id.widgetGrid, pendingIntent );
            
            // update widget
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
    
    public static RemoteViews buildRemoteViews(Context context, int appWidgetId)
    { 
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_opencamera);

        /// set intent for widget service that will create the views
        Intent serviceIntent = new Intent(context, OpenCameraWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))); // embed extras so they don't get ignored
        remoteViews.setRemoteAdapter(appWidgetId, R.id.widgetGrid, serviceIntent);
        remoteViews.setEmptyView(R.id.widgetGrid, R.id.widgetEmptyView);
        
        remoteViews.setInt(R.id.widgetGrid, "setBackgroundColor", 
                OpenCameraWidgetConfigureActivity.bgColor);
        
        // set intent for item click (opens main activity)
        //Intent viewIntent = new Intent(context, MainScreen.class);
//        Intent viewIntent = new Intent(context, OpenCameraWidgetConfigureActivity.class);
//        viewIntent.setAction(Intent.ACTION_MAIN);
//        
//        PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);
//        remoteViews.setPendingIntentTemplate(R.id.widgetGrid, viewPendingIntent);
        
        
        
        Intent intent = new Intent(context, OpenCameraWidgetProvider.class);
        intent.setAction(ACTION_START_ACTIVITY);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        remoteViews.setPendingIntentTemplate(R.id.widgetGrid, pendingIntent);
        
        
        return remoteViews;   
    }
}