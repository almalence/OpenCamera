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

package com.almalence.opencam;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;


/***
New interface for preferences. Loads sections for Common preferences.
***/

@TargetApi(11)
public class FragmentUpgrade extends PreferenceFragment implements OnSharedPreferenceChangeListener
{
	static public PreferenceFragment thiz;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        thiz=this;
        
        String settings = "plugins_settings";

        PluginManager.getInstance().loadHeaderContent(settings, this);
        
        if (null == getPreferenceScreen())
        	return;
        for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++)
		{
		    initSummary(getPreferenceScreen().getPreference(i));
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