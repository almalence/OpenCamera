package com.almalence.opencam;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.almalence.opencam.ui.ElementAdapter;
import com.almalence.opencam.ui.Panel;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
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
	
	public static Map<Integer, Mode> modeGridAssoc;
	
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
		
		modeGridAssoc = new Hashtable<Integer, Mode>();
		
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
        
        initModeGrid(true);
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
			//appWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.notes_list);

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
		modeListViews.clear();
		if (modeListAdapter.Elements != null) {
			modeListAdapter.Elements.clear();
			modeListAdapter.notifyDataSetChanged();
		}
		
		List<Mode> hash = ConfigParser.getInstance().getList();
		Iterator<Mode> it = hash.iterator();

		while (it.hasNext())
		{
			final Mode tmp = it.next();
			LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();
			View mode = inflator.inflate(
					R.layout.widget_opencamera_mode_list_element, null,
					false);
			// set some mode icon
			((ImageView) mode.findViewById(R.id.modeImage))
					.setImageResource(MainScreen.thiz.getResources()
							.getIdentifier(tmp.icon, "drawable",
									MainScreen.thiz.getPackageName()));

			int id = MainScreen.thiz.getResources().getIdentifier(tmp.modeName,
					"string", MainScreen.thiz.getPackageName());
			final String modename = MainScreen.thiz.getResources().getString(id);

			((TextView) mode.findViewById(R.id.modeText)).setText(modename);
			
			mode.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v)
				{
					//View newMode = allModeViews.get(modename);
					
					modeGridAssoc.put(currentModeIndex, tmp);
					initModeGrid(false);
					if(modeList.getVisibility() == View.VISIBLE)
						modeList.setVisibility(View.GONE);
					
//					modeGridViews.remove(currentModeIndex);
//					modeGridViews.add(currentModeIndex, newMode);
//					modeGridAdapter.Elements = modeGridViews;
//					modeGridAdapter.notifyDataSetChanged();
					//modeGrid.invalidate();
					//modeGrid.requestLayout();
				}
			});
			
			modeListViews.add(mode);
		}
		
		modeListAdapter.Elements = modeListViews;
		modeList.setAdapter(modeListAdapter);
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
		
		List<Mode> hash = null;
		if(bInitial)
			hash = ConfigParser.getInstance().getList();
		else
		{
			hash = new ArrayList<Mode>();
			Set<Integer> keys = modeGridAssoc.keySet();    		
    		Iterator<Integer> it = keys.iterator();
    		while(it.hasNext())
    		{
    			int gridIndex = it.next();
    			Mode mode = modeGridAssoc.get(gridIndex);    			
    			hash.add(mode);
    		}
		}
		Iterator<Mode> it = hash.iterator();

		int i = 0;
		while (it.hasNext())
		{
			Mode tmp = it.next();
			LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();
			View mode = inflator.inflate(
					R.layout.widget_opencamera_mode_grid_element, null,
					false);
			// set some mode icon
			((ImageView) mode.findViewById(R.id.modeImage))
					.setImageResource(MainScreen.thiz.getResources()
							.getIdentifier(tmp.icon, "drawable",
									MainScreen.thiz.getPackageName()));

			int id = MainScreen.thiz.getResources().getIdentifier(tmp.modeName,
					"string", MainScreen.thiz.getPackageName());
			String modename = MainScreen.thiz.getResources().getString(id);

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


