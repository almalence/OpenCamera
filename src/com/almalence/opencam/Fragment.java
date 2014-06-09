/*
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is collection of files collectively known as Open Camera.

The Initial Developer of the Original Code is Almalence Inc.
Portions created by Initial Developer are Copyright (C) 2013 
by Almalence Inc. All Rights Reserved.
*/

/* <!-- +++
package com.almalence.opencam_plus;
+++ --> */
// <!-- -+-
package com.almalence.opencam;
//-+- -->

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;


/***
New interface for preferences. Loads sections for Common preferences.
***/

@TargetApi(11)
public class Fragment extends PreferenceFragment implements OnSharedPreferenceChangeListener
{
	static public PreferenceFragment thiz;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        thiz=this;
        
        String settings = getArguments().getString("type");

        PluginManager.getInstance().loadHeaderContent(settings, this);
        
        if (null == getPreferenceScreen())
        	return;
        for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++)
		{
		    initSummary(getPreferenceScreen().getPreference(i));
		}
        
        Preference aboutPref = (Preference) findPreference("about");
        if (aboutPref!=null)
        aboutPref.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                Toast.makeText(MainScreen.thiz,
                		MainScreen.thiz.getResources().getString(R.string.Pref_About),
                        Toast.LENGTH_LONG).show();

                return true;
            }
        });
        
        
        CheckBoxPreference helpPref = (CheckBoxPreference) findPreference("showHelpPrefCommon");
        if (helpPref!=null)
        	helpPref.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                if (((CheckBoxPreference)preference).isChecked())
                {
                	SharedPreferences prefs = PreferenceManager
            				.getDefaultSharedPreferences(MainScreen.mainContext);
                	Editor prefsEditor = prefs.edit();
                	prefsEditor.putBoolean("droShowHelp", true);
					prefsEditor.putBoolean("sequenceRemovalShowHelp", true);
					prefsEditor.putBoolean("panoramaShowHelp", true);
					prefsEditor.putBoolean("groupshotRemovalShowHelp", true);
					prefsEditor.putBoolean("objectRemovalShowHelp", true);
					prefsEditor.putBoolean("bestShotShowHelp", true);
					prefsEditor.commit();
                }

                return true;
            }
        });
        
        ListPreference saveToPreference = (ListPreference)this.findPreference("saveToPref");
        if (saveToPreference!=null)
	        saveToPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() 
	        {			
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) 
				{
					int v = -1;
					
					int v_old = 0;
					
					try
					{
						v = Integer.parseInt(newValue.toString());
						v_old = Integer.parseInt(((ListPreference)preference).getValue());
					}
					catch (NumberFormatException e)
					{
						
					}
					
					if ((v == 2 || v == 1) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
					{
						Toast.makeText(MainScreen.thiz, MainScreen.thiz.getResources().getString(R.string.pref_advanced_saving_saveToPref_CantSaveToSD), Toast.LENGTH_LONG).show();
						//return true;
					}
					
					if ((v == 2 || v == 1) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
					{
						Toast.makeText(MainScreen.thiz, MainScreen.thiz.getResources().getString(R.string.pref_advanced_saving_saveToPref_CantSaveToSD), Toast.LENGTH_LONG).show();
						//return true;
					}
					
					if (v == 2)
					{
						Intent intent = new Intent(Preferences.thiz, FolderPicker.class);
						
						intent.putExtra(MainScreen.sSavePathPref, v_old);
						
						Preferences.thiz.startActivity(intent);
					}
					
					return true;
				}
			});
        
        
        PreferenceCategory cat = (PreferenceCategory)this.findPreference("Pref_VFCommon_Preference_Category");
		if(cat != null)
		{
			CheckBoxPreference cp = (CheckBoxPreference)cat.findPreference("maxScreenBrightnessPref");
			if(cp != null)
			{
				cp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					// @Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						boolean value = Boolean.parseBoolean(newValue.toString());
						setScreenBrightness(value);
						return true;
					}
				});
			}
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
		boolean MaxScreenBrightnessPreference = prefs.getBoolean("maxScreenBrightnessPref", false);
		setScreenBrightness(MaxScreenBrightnessPreference);
    }
    
    static public void setScreenBrightness(boolean setMax)
	{
    	try{
			//ContentResolver cResolver = getContentResolver();
			Window window = thiz.getActivity().getWindow();
			
			WindowManager.LayoutParams layoutpars = window.getAttributes();
			
	        //Set the brightness of this window	
			if(setMax)
				layoutpars.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
			else
				layoutpars.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
	
	        //Apply attribute changes to this window
	        window.setAttributes(layoutpars);
    	}
		catch(Exception e)
		{
			
		}
	}
    
    static public void closePrefs()
	{
		thiz.getFragmentManager().popBackStack();
	}
    
    @Override
    public void onResume(){
     super.onResume();
     if (null == getPreferenceScreen())
     	return;
     // Set up a listener whenever a key changes             
     getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
   
    @Override
    public void onPause() { 
     super.onPause();
     if (null == getPreferenceScreen())
     	return;
     // Unregister the listener whenever a key changes             
     getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);     
    } 
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) 
    { 
    	updatePrefSummary(findPreference(key));
    }
    
    private void initSummary(Preference p)
	{
		if (p instanceof PreferenceCategory)
		{
		    PreferenceCategory pCat = (PreferenceCategory)p;
		    for(int i=0;i<pCat.getPreferenceCount();i++)
		    {
		    	initSummary(pCat.getPreference(i));
		    }
		}
		else
   		{
   			updatePrefSummary(p);
   		}
	}
		 
	private void updatePrefSummary(Preference p)
	{
		if (p instanceof ListPreference) 
		{
			ListPreference listPref = (ListPreference) p; 
			p.setSummary(listPref.getEntry()); 
		}
		if (p instanceof EditTextPreference) 
		{
			EditTextPreference editTextPref = (EditTextPreference) p; 
			if(p.getKey().equalsIgnoreCase("editKey"))
			{
				p.setSummary("*****"); 
			}
			else 
			{
				p.setSummary(editTextPref.getText()); 
			}
		}
 	}
}
