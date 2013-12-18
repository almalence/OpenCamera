package com.almalence.opencam;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

import com.almalence.opencam.ui.ElementAdapter;
import com.almalence.opencam.ui.Panel;
import com.almalence.opencam.util.Util;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
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
import android.widget.TextView;


public class OpenCameraWidgetConfigureActivity extends Activity implements View.OnClickListener
{
	boolean mWidgetConfigurationStarted = false;
	int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	
	private ListView       modeList;
	private ElementAdapter modeListAdapter;
	private List<View> modeListViews;
	
	private GridView       modeGrid;
	private ElementAdapter modeGridAdapter;
	private List<View> modeGridViews;
	
	//private Map<String, View> allModeViews;
	
	public static Map<Integer, OpenCameraWidgetItem> modeGridAssoc;
	
	public static Map<View, OpenCameraWidgetItem> listItems;
	
	private int currentModeIndex;
	
	View buttonBGFirst;
	View buttonBGSecond;
	View buttonBGThird;
	
	private static int colorIndex = 0;
	public static int bgColor = 0x5A3B3131;

	@Override
    protected void onCreate(final Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        
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
        
        View buttonDone = this.findViewById(R.id.doneButtonText);
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
    }
	

	@Override
	public void onClick(View v)
	{
		if(v.getId() == R.id.doneButtonText)
		{
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
		else if(v == buttonBGFirst)
		{
			buttonBGThird.setSelected(false);
			buttonBGSecond.setSelected(false);
			buttonBGFirst.setSelected(true);
			
			modeGrid.setBackgroundColor(0x5A3B3131);
			bgColor = 0x5A3B3131;
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
				bgColor = 0x5A3B3131;				
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
							.getIdentifier(tmp.icon, "drawable",
									this.getPackageName()));

			int id = this.getResources().getIdentifier(tmp.modeName,
					"string", this.getPackageName());
			final String modename = this.getResources().getString(id);

			((TextView) mode.findViewById(R.id.modeText)).setText(modename);
			
			final OpenCameraWidgetItem item = new OpenCameraWidgetItem(tmp.modeID, this.getResources().getIdentifier(
					  tmp.icon, "drawable",
					  this.getPackageName()), false);
			
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
			
//			mode.setOnClickListener(new OnClickListener(){
//				@Override
//				public void onClick(View v)
//				{
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
		
		modeListAdapter.Elements = modeListViews;
		modeList.setAdapter(modeListAdapter);
		
		modeList.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				Log.e("Widget", "onItemClick");
				OpenCameraWidgetItem item = listItems.get(arg1);
				if(item != null)
				{
					modeGridAssoc.put(currentModeIndex, item);
					initModeGrid(false);
				}
				if(modeList.getVisibility() == View.VISIBLE)
					modeList.setVisibility(View.GONE);
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
			try {
				ConfigParser.getInstance().parse(this.getBaseContext());
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			hash = new ArrayList<OpenCameraWidgetItem>();
			List<Mode> modeList = ConfigParser.getInstance().getList();
			Iterator<Mode> it = modeList.iterator();
    		while(it.hasNext())
    		{
    			Mode tmp = it.next();
    			if(tmp.modeName.contains("hdr") || tmp.modeName.contains("panorama"))
    			{
	    			OpenCameraWidgetItem mode = new OpenCameraWidgetItem(tmp.modeID, this.getResources().getIdentifier(
	  					  tmp.icon, "drawable",
	  					  this.getPackageName()), false);			
	    			hash.add(mode);
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
	    		OpenCameraWidgetItem mode = new OpenCameraWidgetItem("single", iconID, true);			
	  			hash.add(mode);
    		}
    		catch(NullPointerException exp)
    		{
    			Log.e("Widget", "Can't create TORCH mode");
    		}
    		
    		for(int i = 0; i < modeList.size() - 2; i++)
    		{
    			OpenCameraWidgetItem mode = new OpenCameraWidgetItem("hide", 0, false);
    			hash.add(mode);
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
			OpenCameraWidgetItem tmp = it.next();
			LayoutInflater inflator = this.getLayoutInflater();
			View mode = inflator.inflate(
					R.layout.widget_opencamera_mode_grid_element, null,
					false);
			// set some mode icon
			if(tmp.modeIconID != 0)
				((ImageView) mode.findViewById(R.id.modeImage)).setImageResource(tmp.modeIconID);

			//int id = this.getResources().getIdentifier(tmp.modeName,
			//		"string", this.getPackageName());
			//String modename = this.getResources().getString(id);

			final int index = i;
			//((TextView) mode.findViewById(R.id.modeText)).setText(modename);
			mode.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v)
				{
					currentModeIndex = index;
					if(modeList.getVisibility() == View.GONE)
						modeList.setVisibility(View.VISIBLE);
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
			if(modeList.getVisibility() == View.VISIBLE)
			{
				modeList.setVisibility(View.GONE);
				return true;
			}
		}
		
		if (super.onKeyDown(keyCode, event))
			return true;
		return false;
	}
}


