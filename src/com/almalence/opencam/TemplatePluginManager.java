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

import android.preference.PreferenceFragment;

import com.almalence.plugins.capture.burst.BurstCapturePlugin;
import com.almalence.plugins.capture.standard.CapturePlugin;
import com.almalence.plugins.export.standard.ExportPlugin;
import com.almalence.plugins.processing.simple.SimpleProcessingPlugin;
import com.almalence.plugins.vf.focus.FocusVFPlugin;
import com.almalence.plugins.vf.grid.GridVFPlugin;

/***
 * Plugins managing class.
 * 
 * Controls plugins interaction with ApplicationScreen and processing, controls
 * different stages of activity workflow
 * 
 * may be used by other plugins to retrieve some parameters/settings from other
 * plugins
 ***/

public class TemplatePluginManager extends PluginManagerBase
{
	
	private static TemplatePluginManager		pluginManager;

	public static TemplatePluginManager getInstance()
	{
		if (pluginManager == null)
		{
			pluginManager = new TemplatePluginManager();
		}
		return pluginManager;
	}

	// plugin manager ctor. plugins initialization and filling plugin list
	private TemplatePluginManager()
	{
		super();
	}
	
	@Override
	protected void createPlugins()
	{
		// init plugins and add to pluginList
		// probably will be created only active for memory saving purposes.

		/*
		 * Insert any new plugin below (create and add to list of concrete type)
		 */

		// VF
		GridVFPlugin gridVFPlugin = new GridVFPlugin();
		pluginList.put(gridVFPlugin.getID(), gridVFPlugin);
		listVF.add(gridVFPlugin);

		FocusVFPlugin focusVFPlugin = new FocusVFPlugin();
		pluginList.put(focusVFPlugin.getID(), focusVFPlugin);
		listVF.add(focusVFPlugin);

		// Capture
		CapturePlugin testCapturePlugin = new CapturePlugin();
		pluginList.put(testCapturePlugin.getID(), testCapturePlugin);
		listCapture.add(testCapturePlugin);

		BurstCapturePlugin burstCapturePlugin = new BurstCapturePlugin();
		pluginList.put(burstCapturePlugin.getID(), burstCapturePlugin);
		listCapture.add(burstCapturePlugin);

		// Processing
		SimpleProcessingPlugin simpleProcessingPlugin = new SimpleProcessingPlugin();
		pluginList.put(simpleProcessingPlugin.getID(), simpleProcessingPlugin);
		listProcessing.add(simpleProcessingPlugin);

		// Export
		ExportPlugin testExportPlugin = new ExportPlugin();
		pluginList.put(testExportPlugin.getID(), testExportPlugin);
		listExport.add(testExportPlugin);		
	}
	
	@Override
	public void onManagerCreate()
	{
		
	}

	public void loadHeaderContent(String settings, PreferenceFragment pf)
	{
		
	}

	
	@Override
	public boolean isPreviewDependentMode()
	{
		return false;
	}
	
	@Override
	public boolean isCamera2InterfaceAllowed()
	{
		return true;
	}
}
