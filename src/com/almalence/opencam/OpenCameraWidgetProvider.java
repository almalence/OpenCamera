package com.almalence.opencam;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
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
            	boolean torchValue = intent.getBooleanExtra(MainScreen.EXTRA_TORCH, false);
            	
            	Bundle extras = new Bundle();
    	        extras.putString(MainScreen.EXTRA_ITEM, modeName);    	        
	        	extras.putBoolean(MainScreen.EXTRA_TORCH, torchValue);	
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
    	else if(intent.getAction().contentEquals("com.sec.android.widgetapp.APPWIDGET_RESIZE")) {
    		handleResizeSignal(context, intent);
        }
        super.onReceive(context, intent);
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

//        Log.e("Widget", "onUpdate");
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++)
        {
        	int appWidgetId = appWidgetIds[i];
	            
        	RemoteViews remoteViews = null;
        	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        	{
        		remoteViews = createRemoteViews(context, appWidgetId);
        	}
        	else
        		remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_opencamera_column_4);
            
            /// set intent for widget service that will create the views
            Intent serviceIntent = new Intent(context, OpenCameraWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))); // embed extras so they don't get ignored
            remoteViews.setRemoteAdapter(appWidgetId, R.id.widgetGrid, serviceIntent);
            remoteViews.setEmptyView(R.id.widgetGrid, R.id.widgetEmptyView);
            
            remoteViews.setInt(R.id.widgetGrid, "setBackgroundColor", 
                    OpenCameraWidgetConfigureActivity.bgColor);
            
            Intent intent = new Intent(context, OpenCameraWidgetProvider.class);
            intent.setAction(ACTION_START_ACTIVITY);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            remoteViews.setPendingIntentTemplate(R.id.widgetGrid, pendingIntent);
            
            // update widget
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
    
    public static RemoteViews buildRemoteViews(Context context, int appWidgetId)
    { 
        //RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_opencamera_column_4);
    	RemoteViews remoteViews = null;
    	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
    	{
    		remoteViews = createRemoteViews(context, appWidgetId);
    	}
    	else
    		remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_opencamera_column_4);

        /// set intent for widget service that will create the views
        Intent serviceIntent = new Intent(context, OpenCameraWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))); // embed extras so they don't get ignored
        remoteViews.setRemoteAdapter(appWidgetId, R.id.widgetGrid, serviceIntent);
        remoteViews.setEmptyView(R.id.widgetGrid, R.id.widgetEmptyView);
        
        remoteViews.setInt(R.id.widgetGrid, "setBackgroundColor", 
                OpenCameraWidgetConfigureActivity.bgColor);
        
        Intent intent = new Intent(context, OpenCameraWidgetProvider.class);
        intent.setAction(ACTION_START_ACTIVITY);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        remoteViews.setPendingIntentTemplate(R.id.widgetGrid, pendingIntent);
        
        return remoteViews;   
    }
    
    @TargetApi(16)
    @Override
    public void onAppWidgetOptionsChanged(Context context,
                                          AppWidgetManager appWidgetManager,
                                          int appWidgetId,
                                          Bundle newOptions)
    {
      
//    	Log.e("Widget", "onAppWidgetOptionsChanged");
    	RemoteViews remoteViews = createRemoteViews(context, appWidgetId);      
  
    	/// set intent for widget service that will create the views
    	Intent serviceIntent = new Intent(context, OpenCameraWidgetService.class);
    	serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
    	serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))); // embed extras so they don't get ignored
    	remoteViews.setRemoteAdapter(appWidgetId, R.id.widgetGrid, serviceIntent);
  
    	remoteViews.setEmptyView(R.id.widgetGrid, R.id.widgetEmptyView);
  
    	remoteViews.setInt(R.id.widgetGrid, "setBackgroundColor", 
    			OpenCameraWidgetConfigureActivity.bgColor);      

  
    	Intent intent = new Intent(context, OpenCameraWidgetProvider.class);
    	intent.setAction(ACTION_START_ACTIVITY);
    	PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
    	remoteViews.setPendingIntentTemplate(R.id.widgetGrid, pendingIntent);
  
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
    
    @TargetApi(16)
    private static RemoteViews createRemoteViews(Context context, int appWidgetId)
    {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
		int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
    
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager mng = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		mng.getDefaultDisplay()
			.getMetrics(metrics);
		float ScreenDensity = metrics.density;
		float columnWidth = context.getResources().getDimension(R.dimen.widgetImageSize);
		
//		Log.e("Widget", "minWidth*ScreenDensity = " + minWidth*ScreenDensity);
//		Log.e("Widget", "columnWidth = " + columnWidth);
		
		//TODO: get different widget_opencamera files for differnet grid's columns numbers!!!
		RemoteViews remoteViews = null;		
		if(minWidth*ScreenDensity > columnWidth*4)
			remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_opencamera_column_4);
		else if(minWidth*ScreenDensity > columnWidth*3)
			remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_opencamera_column_3);
		else if(minWidth*ScreenDensity > columnWidth*2)
			remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_opencamera_column_2);
		else
			remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_opencamera_column_1);
		  
		return remoteViews;  
    }
}