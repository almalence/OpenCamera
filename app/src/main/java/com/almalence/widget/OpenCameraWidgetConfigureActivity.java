package com.almalence.widget;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

import com.almalence.util.Util;
/* <!-- +++
import com.almalence.opencam_plus.R;
import com.almalence.opencam_plus.ConfigParser;
import com.almalence.opencam_plus.Mode;
import com.almalence.opencam_plus.cameracontroller.CameraController;
import com.almalence.opencam_plus.ui.ElementAdapter;
+++ --> */
//<!-- -+-
import com.almalence.opencam.R;
import com.almalence.opencam.ConfigParser;
import com.almalence.opencam.Mode;
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.ui.ElementAdapter;
//-+- -->

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class OpenCameraWidgetConfigureActivity extends Activity implements View.OnClickListener
{
	boolean mWidgetConfigurationStarted = false;
	int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	
	private RelativeLayout modeListLayout;
	private ListView       modeList;
	private ElementAdapter modeListAdapter;
	private List<View> modeListViews;
	
	private GridView       modeGrid;
	private ElementAdapter modeGridAdapter;
	private List<View> modeGridViews;
	
	//private Map<String, View> allModeViews;
	
	public static Map<Integer, OpenCameraWidgetItem> modeGridAssoc;
	
	public static Map<View, OpenCameraWidgetItem> listItems;
	
	private OpenCameraWidgetItem currentModeItem;
	private int currentModeIndex;
	
	View buttonBGFirst;
	View buttonBGSecond;
	View buttonBGThird;
	
	private static int colorIndex = 2;
	public static int bgColor = 0x5A000000;
	
	
	private static boolean isFirstLaunch = true;
	SharedPreferences prefs;
	
	public static final String EXTRA_ITEM = "WidgetModeID"; //Clicked mode id from widget.
	public static final String EXTRA_TORCH = "WidgetTorchMode";
	public static final String EXTRA_BARCODE = "WidgetBarcodeMode";
	public static final String EXTRA_SHOP = "WidgetGoShopping";

	@Override
    protected void onCreate(final Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        
//        Log.e("Widget", "Widget Configuration Activity onCreate");
        
        if (!isInstalled("com.almalence.opencam") && !isInstalled("com.almalence.opencam_plus"))
		{
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);

			// 2. Chain together various setter methods to set the dialog characteristics
			builder.setMessage(R.string.widgetShopText)
			       .setTitle(R.string.widgetActivation);
			
			builder.setPositiveButton(R.string.widgetGoShopText, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   
		        	   	CallStoreForOpenCamera(OpenCameraWidgetConfigureActivity.this);
						
		    	        finish();
		           }
		       });
			builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   finish();
		           }
		       });

			// 3. Get the AlertDialog from create()
			AlertDialog d = builder.create();
	        
	        d.setOnKeyListener(new OnKeyListener()
	        {
				@Override
				public boolean onKey(DialogInterface arg0, int keyCode,
						KeyEvent keyEvent)
				{
					if(keyCode == KeyEvent.KEYCODE_BACK)
					{
						finish();
					}
					return true;
				}
	        	
	        });
	        d.show();
		}
        
//        if ((isInstalled("com.almalence.hdr_plus")) || (isInstalled("com.almalence.pixfix")))
//		{
//			Editor prefsEditor = prefs.edit();
//			prefsEditor.putBoolean("plugin_almalence_hdr", true);
//			prefsEditor.commit();
//		}
//		if (isInstalled("com.almalence.panorama.smoothpanorama"))
//		{
//			Editor prefsEditor = prefs.edit();
//			prefsEditor.putBoolean("plugin_almalence_panorama", true);
//			prefsEditor.commit();
//		}
//		
//        /**** Check Billing *****/
//		if (false == prefs.contains("unlock_all_forever") &&
//			false == prefs.contains("plugin_almalence_hdr") &&
//			false == prefs.contains("plugin_almalence_panorama") &&
//			false == prefs.contains("plugin_almalence_moving_burst") &&
//			false == prefs.contains("plugin_almalence_groupshot"))
//		{
////			Log.e("Widget", "Show shop dialog!");
//			// 1. Instantiate an AlertDialog.Builder with its constructor
//			AlertDialog.Builder builder = new AlertDialog.Builder(this);
//
//			// 2. Chain together various setter methods to set the dialog characteristics
//			builder.setMessage(R.string.widgetShopText)
//			       .setTitle(R.string.widgetActivation);
//			
//			builder.setPositiveButton(R.string.widgetGoShopText, new DialogInterface.OnClickListener() {
//		           public void onClick(DialogInterface dialog, int id) {
//		        	   Bundle extras = new Bundle();
//		    	        extras.putBoolean(EXTRA_SHOP, true);
//		    	        //Intent modeIntent = new Intent(OpenCameraWidgetConfigureActivity.this, MainScreen.class);
//		    	        //Intent modeIntent = new Intent("com.almalence.opencam");
//		    	        Intent modeIntent = OpenCameraWidgetConfigureActivity.this.getPackageManager().getLaunchIntentForPackage("com.almalence.opencam");
//		    	        modeIntent.putExtras(extras);
//		    	        modeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		    	        OpenCameraWidgetConfigureActivity.this.startActivity(modeIntent);
//						
//		    	        finish();
//		           }
//		       });
//			builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
//		           public void onClick(DialogInterface dialog, int id) {
//		        	   finish();
//		           }
//		       });
//
//			// 3. Get the AlertDialog from create()
//			AlertDialog d = builder.create();
//	        //d.setTitle("Activate widget");
//	        //d.setContentView(R.layout.widget_opencamera_shop_dialog);	        
//	        //Button cancelButton = (Button) d.findViewById(R.id.widgetButtonCancel);
//	        //Button shopButton = (Button) d.findViewById(R.id.widgetButtonGoShop);
//	        
////	        cancelButton.setOnClickListener(new OnClickListener()
////	        {
////				@Override
////				public void onClick(View arg0)
////				{
////					finish();
////				}
////	        	
////	        });
////	        
////	        shopButton.setOnClickListener(new OnClickListener()
////	        {
////				@Override
////				public void onClick(View arg0)
////				{
////					Bundle extras = new Bundle();
////	    	        extras.putBoolean(MainScreen.EXTRA_SHOP, true);
////	    	        Intent modeIntent = new Intent(OpenCameraWidgetConfigureActivity.this, MainScreen.class);    	        
////	    	        modeIntent.putExtras(extras);
////	    	        modeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////	    	        OpenCameraWidgetConfigureActivity.this.startActivity(modeIntent);
////					
////	    	        finish();
////				}
////	        	
////	        });
//	        
//	        d.setOnKeyListener(new OnKeyListener()
//	        {
//				@Override
//				public boolean onKey(DialogInterface arg0, int keyCode,
//						KeyEvent keyEvent)
//				{
//					if(keyCode == KeyEvent.KEYCODE_BACK)
//					{
//						finish();
//					}
//					return true;
//				}
//	        	
//	        });
//	        d.show();
//		}
        
        isFirstLaunch = prefs.getBoolean("widgetFirstLaunch", true);
        
        colorIndex = prefs.getInt("widgetBGColorIndex", 2);
        
        modeListAdapter = new ElementAdapter();
		modeListViews = new ArrayList<View>();
		
		modeGridAdapter = new ElementAdapter();
		modeGridViews = new ArrayList<View>();
		
		if(modeGridAssoc == null)
			modeGridAssoc = new Hashtable<Integer, OpenCameraWidgetItem>();
		
		listItems = new Hashtable<View, OpenCameraWidgetItem>();
		
		//allModeViews = new Hashtable<String, View>();
		
        setResult(RESULT_CANCELED);
        setContentView(R.layout.widget_opencamera_configure);

        final Intent intent = getIntent();
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
        
        View buttonDone = this.findViewById(R.id.doneButton);
        if(null != buttonDone)
        	buttonDone.setOnClickListener(this);
        
        buttonBGFirst = this.findViewById(R.id.bgColorButtonFirst);
        buttonBGSecond = this.findViewById(R.id.bgColorButtonSecond);
        buttonBGThird = this.findViewById(R.id.bgColorButtonThird);
        
        buttonBGFirst.setOnClickListener(this);
        buttonBGSecond.setOnClickListener(this);
        buttonBGThird.setOnClickListener(this);
        
        initModeGrid(modeGridAssoc.size() == 0);
        initModeList();
        initColorButtons();
        
        isFirstLaunch = false;
        prefs.edit().putBoolean("widgetFirstLaunch", isFirstLaunch).commit();
    }
	
	public void ConfigurationFinished()
	{
		prefs.edit().putInt("widgetBGColorIndex", colorIndex).commit();
		prefs.edit().putInt("widgetBGColor", bgColor).commit();
		// First set result OK with appropriate widgetId
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);

		// Build/Update widget
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());

		// This is equivalent to your ChecksWidgetProvider.updateAppWidget()    
		appWidgetManager.updateAppWidget(mAppWidgetId,
		                                 OpenCameraWidgetProvider.buildRemoteViews(getApplicationContext(),
		                                                                       mAppWidgetId));

		// Updates the collection view, not necessary the first time
		appWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.widgetGrid);

		// Destroy activity
		finish();
	}
	

	@Override
	public void onClick(View v)
	{		
		if(v.getId() == R.id.doneButton)
		{
			ConfigurationFinished();			
		}
		else if(v == buttonBGFirst)
		{
			buttonBGThird.setSelected(false);
			buttonBGSecond.setSelected(false);
			buttonBGFirst.setSelected(true);
			
			modeGrid.setBackgroundColor(0x5A3B3131);
			bgColor = 0x5A000000;
//			bgColor = 0x5A3B3131;
			colorIndex = 0;
		}
		else if(v == buttonBGSecond)
		{
			buttonBGThird.setSelected(false);
			buttonBGSecond.setSelected(true);
			buttonBGFirst.setSelected(false);
			
			modeGrid.setBackgroundColor(0xFF000000);
			bgColor = 0xFF000000;
			colorIndex = 1;
		}
		else if(v == buttonBGThird)
		{
			buttonBGThird.setSelected(true);
			buttonBGSecond.setSelected(false);
			buttonBGFirst.setSelected(false);
			
			modeGrid.setBackgroundColor(0x00000000);
			bgColor = 0x00000000;
			colorIndex = 2;			
		}		

		prefs.edit().putInt("widgetBGColorIndex", colorIndex).commit();
	}
	
	
	private void initColorButtons()
	{
		switch(colorIndex)
		{
			case 0:
			{
				buttonBGThird.setSelected(false);
				buttonBGSecond.setSelected(false);
				buttonBGFirst.setSelected(true);
				
				modeGrid.setBackgroundColor(0x5A3B3131);
//				bgColor = 0x5A3B3131;
				bgColor = 0x5A000000;
			} break;
			case 1:
			{
				buttonBGThird.setSelected(false);
				buttonBGSecond.setSelected(true);
				buttonBGFirst.setSelected(false);
				
				modeGrid.setBackgroundColor(0xFF000000);
				bgColor = 0xFF000000;	
			} break;
			case 2:
			{
				buttonBGThird.setSelected(true);
				buttonBGSecond.setSelected(false);
				buttonBGFirst.setSelected(false);
				
				modeGrid.setBackgroundColor(0x00000000);
				bgColor = 0x00000000;	
			} break;
			default: break;
		}
	}
	
	private void initModeList()
	{
		modeListLayout = (RelativeLayout)this.findViewById(R.id.widgetConfListLayout);
		modeList = (ListView)this.findViewById(R.id.widgetConfList);
		listItems.clear();
		modeListViews.clear();
		if (modeListAdapter.Elements != null) {
			modeListAdapter.Elements.clear();
			modeListAdapter.notifyDataSetChanged();
		}
		
		try
		{
			LayoutInflater inflator = this.getLayoutInflater();
			View mode = inflator.inflate(
					R.layout.widget_opencamera_mode_list_element, null,
					false);
			// set some mode icon
//			((ImageView) mode.findViewById(R.id.modeImage))
//					.setImageResource(this.getResources()
//							.getIdentifier("gui_almalence_settings_flash_torch", "drawable",
//									this.getPackageName()));
	
			final String modename = this.getResources().getString(R.string.widgetHideItem);
			((TextView) mode.findViewById(R.id.modeText)).setText(modename);
			
			final OpenCameraWidgetItem item = new OpenCameraWidgetItem("hide", 0, false);
			
//			mode.setOnClickListener(new OnClickListener(){
//				@Override
//				public void onClick(View v)
//				{
//					Log.e("Widget","List item onClick!");
//					modeGridAssoc.put(currentModeIndex, item);
//					initModeGrid(false);
//					if(modeList.getVisibility() == View.VISIBLE)
//						modeList.setVisibility(View.GONE);
//				}
//			});
			
			listItems.put(mode, item);
			modeListViews.add(mode);
		}
		catch(RuntimeException exp)
		{
			
		}
		
		try {
			ConfigParser.getInstance().parse(this.getBaseContext());
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			finish();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			finish();
		}
		List<Mode> hash = ConfigParser.getInstance().getList();
		Iterator<Mode> it = hash.iterator();

		while (it.hasNext())
		{
			final Mode tmp = it.next();
			LayoutInflater inflator = this.getLayoutInflater();
			View mode = inflator.inflate(
					R.layout.widget_opencamera_mode_list_element, null,
					false);
			// set some mode icon
			((ImageView) mode.findViewById(R.id.modeImage))
					.setImageResource(this.getResources()
							.getIdentifier(CameraController.isUseSuperMode() ? tmp.iconHAL : tmp.icon, "drawable",
									this.getPackageName()));

			int id = this.getResources().getIdentifier(CameraController.isUseSuperMode() ? tmp.modeNameHAL : tmp.modeName,
					"string", this.getPackageName());
			final String modename = this.getResources().getString(id);

			((TextView) mode.findViewById(R.id.modeText)).setText(modename);
			
			final OpenCameraWidgetItem item = new OpenCameraWidgetItem(tmp.modeID, this.getResources().getIdentifier(
					CameraController.isUseSuperMode() ? tmp.iconHAL : tmp.icon, "drawable",
					  this.getPackageName()), false);
			
			listItems.put(mode, item);
			modeListViews.add(mode);
		}
		
		
		try
		{
			LayoutInflater inflator = this.getLayoutInflater();
			View mode = inflator.inflate(
					R.layout.widget_opencamera_mode_list_element, null,
					false);
			// set some mode icon
			((ImageView) mode.findViewById(R.id.modeImage))
					.setImageResource(this.getResources()
							.getIdentifier("gui_almalence_settings_flash_torch", "drawable",
									this.getPackageName()));
	
			final String modename = this.getResources().getString(R.string.widgetTorchItem);
			((TextView) mode.findViewById(R.id.modeText)).setText(modename);
			
			final OpenCameraWidgetItem item = new OpenCameraWidgetItem("torch", this.getResources().getIdentifier(
					"gui_almalence_settings_flash_torch", "drawable",
					  this.getPackageName()), true);
			
			listItems.put(mode, item);
			modeListViews.add(mode);
		}
		catch(RuntimeException exp)
		{
			
		}
		
		try
		{
			LayoutInflater inflator = this.getLayoutInflater();
			View mode = inflator.inflate(
					R.layout.widget_opencamera_mode_list_element, null,
					false);
			// set some mode icon
			((ImageView) mode.findViewById(R.id.modeImage))
					.setImageResource(this.getResources()
							.getIdentifier("gui_almalence_settings_scene_barcode_on", "drawable",
									this.getPackageName()));
	
			final String modename = this.getResources().getString(R.string.widgetBarcodeItem);
			((TextView) mode.findViewById(R.id.modeText)).setText(modename);
			
			final OpenCameraWidgetItem item = new OpenCameraWidgetItem("barcode", this.getResources().getIdentifier(
					"gui_almalence_settings_scene_barcode_on", "drawable",
					  this.getPackageName()), false);
			
			listItems.put(mode, item);
			modeListViews.add(mode);
		}
		catch(RuntimeException exp)
		{
			
		}
		
		try
		{
			LayoutInflater inflator = this.getLayoutInflater();
			View mode = inflator.inflate(
					R.layout.widget_opencamera_mode_list_element, null,
					false);
			// set some mode icon
			((ImageView) mode.findViewById(R.id.modeImage))
					.setImageResource(R.drawable.gui_almalence_settings_more_settings);
	
			final String modename = this.getResources().getString(R.string.widgetSettingsItem);
			((TextView) mode.findViewById(R.id.modeText)).setText(modename);
			
			final OpenCameraWidgetItem item = new OpenCameraWidgetItem("settings", R.drawable.gui_almalence_settings_more_settings, false);
			
			listItems.put(mode, item);
			modeListViews.add(mode);
		}
		catch(RuntimeException exp)
		{
			
		}
		
		modeListAdapter.Elements = modeListViews;
		modeList.setAdapter(modeListAdapter);
		
		modeListLayout.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{				
			}
			
		});
		modeList.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
//				Log.e("Widget", "onItemClick");
				final OpenCameraWidgetItem item = listItems.get(arg1);
				if(currentModeItem.modeName.contains("settings") && !item.modeName.contains("settings"))
				{
					AlertDialog.Builder builder = new AlertDialog.Builder(OpenCameraWidgetConfigureActivity.this);

					// 2. Chain together various setter methods to set the dialog characteristics
					builder.setMessage(R.string.widgetHideSettingsText)
					       .setTitle(R.string.widgetAlert);
					
					builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   modeGridAssoc.put(currentModeIndex, item);
								initModeGrid(false);
								
								prefs.edit().putString("widgetAddedModeID" + String.valueOf(currentModeIndex), item.modeName).commit();
				    			prefs.edit().putInt("widgetAddedModeIcon" + String.valueOf(currentModeIndex), item.modeIconID).commit();
				    			
				    			if(modeListLayout.getVisibility() == View.VISIBLE)
									modeListLayout.setVisibility(View.GONE);
				           }
				       });
					builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   dialog.cancel();
				           }
				       });

					// 3. Get the AlertDialog from create()
					AlertDialog d = builder.create();			        
			        
			        d.setOnKeyListener(new OnKeyListener()
			        {
						@Override
						public boolean onKey(DialogInterface dialog, int keyCode,
								KeyEvent keyEvent)
						{
							if(keyCode == KeyEvent.KEYCODE_BACK)
							{
								dialog.cancel();
							}
							
							return true;
						}
			        	
			        });
			        d.show();
				}				
				else if(item != null)
				{
					modeGridAssoc.put(currentModeIndex, item);
					initModeGrid(false);
					
					prefs.edit().putString("widgetAddedModeID" + String.valueOf(currentModeIndex), item.modeName).commit();
	    			prefs.edit().putInt("widgetAddedModeIcon" + String.valueOf(currentModeIndex), item.modeIconID).commit();
	    			
	    			if(modeListLayout.getVisibility() == View.VISIBLE)
						modeListLayout.setVisibility(View.GONE);
				}				
			}			
		});
	}
	
	
	private void initModeGrid(boolean bInitial)
	{
		modeGrid = (GridView)this.findViewById(R.id.widgetConfGrid);
		modeGridViews.clear();
		//allModeViews.clear();
		if (modeGridAdapter.Elements != null) {
			modeGridAdapter.Elements.clear();
			modeGridAdapter.notifyDataSetChanged();
		}
		
		List<OpenCameraWidgetItem> hash = null;
		if(bInitial)
		{
			hash = new ArrayList<OpenCameraWidgetItem>();
			try {
				ConfigParser.getInstance().parse(this.getBaseContext());
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			List<Mode> modeList = ConfigParser.getInstance().getList();
			if(isFirstLaunch)
			{
				int modeCount = 0;
				
				Iterator<Mode> it = modeList.iterator();
	    		while(it.hasNext())
	    		{
	    			Mode tmp = it.next();
	    			if(tmp.modeName.contains("hdr") || tmp.modeName.contains("panorama")
	    				|| tmp.modeName.contains("single") || tmp.modeName.contains("burst")
	    				|| tmp.modeName.contains("night") || tmp.modeName.contains("video")
	    				|| tmp.modeName.contains("super"))
	    			{  
	    				int iconID = this.getResources().getIdentifier(
	    						CameraController.isUseSuperMode() ? tmp.iconHAL : tmp.icon, "drawable",
	  	  					  this.getPackageName());
		    			OpenCameraWidgetItem mode = new OpenCameraWidgetItem(tmp.modeID, iconID, false);			
		    			hash.add(mode);
		    			
		    			prefs.edit().putString("widgetAddedModeID" + String.valueOf(modeCount), tmp.modeID).commit();
		    			prefs.edit().putInt("widgetAddedModeIcon" + String.valueOf(modeCount), iconID).commit();
		    			
		    			modeCount++;
	    			}
	//    			else
	//    			{
	//    				OpenCameraWidgetItem mode = new OpenCameraWidgetItem("hide", 0, false);			
	//  	    			hash.add(mode);
	//    			}
	    		}	    		
	    		
	    		try
	    		{
		    		int iconID = this.getResources().getIdentifier("gui_almalence_settings_flash_torch", "drawable", this.getPackageName());
		    		OpenCameraWidgetItem mode = new OpenCameraWidgetItem("torch", iconID, true);			
		  			hash.add(mode);
		  			
	    			prefs.edit().putString("widgetAddedModeID" + String.valueOf(modeCount), "torch").commit();
	    			prefs.edit().putInt("widgetAddedModeIcon" + String.valueOf(modeCount), iconID).commit();
	    			
	    			modeCount++;
	    		}
	    		catch(NullPointerException exp)
	    		{
	    			Log.e("Widget", "Can't create TORCH mode");
	    		}    		
	    		
	    		try
	    		{
		    		int iconID = R.drawable.gui_almalence_settings_more_settings;
		    		OpenCameraWidgetItem mode = new OpenCameraWidgetItem("settings", iconID, false);
		  			hash.add(mode);
		  			
	    			prefs.edit().putString("widgetAddedModeID" + String.valueOf(modeCount), "settings").commit();
	    			prefs.edit().putInt("widgetAddedModeIcon" + String.valueOf(modeCount), iconID).commit();
	    		}
	    		catch(NullPointerException exp)
	    		{
	    			Log.e("Widget", "Can't create SETTINGS button");
	    		}
			
    		
	    		for(int i = 0; i < modeList.size() - 2; i++)
	    		{
	    			OpenCameraWidgetItem mode = new OpenCameraWidgetItem("hide", 0, false);
	    			hash.add(mode);
	    		}
			}
			else
			{
				
				for(int i = 0; i < modeList.size() + 2; i++)
	    		{
	    			String modeID = prefs.getString("widgetAddedModeID" + String.valueOf(i), "hide");
	    			
	    			int modeIcon = prefs.getInt("widgetAddedModeIcon" + String.valueOf(i), 0);
	    			OpenCameraWidgetItem mode = new OpenCameraWidgetItem(modeID, modeIcon, modeID.contains("torch"));
	    				
	    			hash.add(mode);
	    			
	    		}
			}
		}
		else
		{
			hash = new ArrayList<OpenCameraWidgetItem>();
			Set<Integer> unsorted_keys = modeGridAssoc.keySet();
			List<Integer> keys = Util.asSortedList(unsorted_keys);
    		Iterator<Integer> it = keys.iterator();
    		while(it.hasNext())
    		{
    			int gridIndex = it.next();
    			OpenCameraWidgetItem mode = modeGridAssoc.get(gridIndex);    			
    			hash.add(mode);
    		}
		}
		Iterator<OpenCameraWidgetItem> it = hash.iterator();

		int i = 0;
		while (it.hasNext())
		{
			final OpenCameraWidgetItem tmp = it.next();
			LayoutInflater inflator = this.getLayoutInflater();
			View mode = inflator.inflate(
					R.layout.widget_opencamera_mode_grid_element, null,
					false);
			// set some mode icon
			if(tmp.modeIconID != 0)
				((ImageView) mode.findViewById(R.id.modeImage)).setImageResource(tmp.modeIconID);
			else
				((ImageView) mode.findViewById(R.id.modeImage)).setBackgroundResource(R.drawable.widget_mode_grid_button_background);

			//int id = this.getResources().getIdentifier(tmp.modeName,
			//		"string", this.getPackageName());
			//String modename = this.getResources().getString(id);

			final int index = i;
			//((TextView) mode.findViewById(R.id.modeText)).setText(modename);
			mode.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v)
				{
					currentModeItem = tmp;
					currentModeIndex = index;
					if(modeListLayout.getVisibility() == View.GONE)
						modeListLayout.setVisibility(View.VISIBLE);
				}
			});
			
			modeGridViews.add(mode);
			modeGridAssoc.put(i++, tmp);
			
			//allModeViews.put(modename, mode);
		}
		
		modeGridAdapter.Elements = modeGridViews;
		modeGrid.setAdapter(modeGridAdapter);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{		
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			if(modeListLayout.getVisibility() == View.VISIBLE)
			{
				modeListLayout.setVisibility(View.GONE);
				return true;
			}
		}
		
		if (super.onKeyDown(keyCode, event))
			return true;
		return false;
	}
	
	
	private boolean isInstalled(String packageName) {
		PackageManager pm = getPackageManager();
		boolean installed = false;
		try {
			pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			installed = true;
		} catch (PackageManager.NameNotFoundException e) {
			installed = false;
		}
		return installed;
	}
	
	public void CallStoreForOpenCamera(Context context)
    {
    	try
    	{
        	Intent intent = new Intent(Intent.ACTION_VIEW);
       		intent.setData(Uri.parse("market://details?id=com.almalence.opencam"));
       		this.startActivityForResult(intent, 1);
	        //context.startActivity(intent);
    	}
    	catch(ActivityNotFoundException e)
    	{
    		return;
    	}
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.e("Widget", "requestCode = " + requestCode + " resultCode = " + resultCode);
	}
}


