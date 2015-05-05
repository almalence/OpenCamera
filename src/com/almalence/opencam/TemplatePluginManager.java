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

import com.almalence.plugins.capture.template.TemplateCapturePlugin;
import com.almalence.plugins.capture.templateburst.TemplateBurstCapturePlugin;
import com.almalence.plugins.export.template.TemplateExportPlugin;
import com.almalence.plugins.processing.template.TemplateProcessingPlugin;
import com.almalence.plugins.vf.templatefocus.TemplateFocusVFPlugin;
import com.almalence.plugins.vf.templategrid.TemplateGridVFPlugin;

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
		TemplateGridVFPlugin gridVFPlugin = new TemplateGridVFPlugin();
		pluginList.put(gridVFPlugin.getID(), gridVFPlugin);
		listVF.add(gridVFPlugin);

		TemplateFocusVFPlugin focusVFPlugin = new TemplateFocusVFPlugin();
		pluginList.put(focusVFPlugin.getID(), focusVFPlugin);
		listVF.add(focusVFPlugin);

		// Capture
		TemplateCapturePlugin testCapturePlugin = new TemplateCapturePlugin();
		pluginList.put(testCapturePlugin.getID(), testCapturePlugin);
		listCapture.add(testCapturePlugin);

		TemplateBurstCapturePlugin burstCapturePlugin = new TemplateBurstCapturePlugin();
		pluginList.put(burstCapturePlugin.getID(), burstCapturePlugin);
		listCapture.add(burstCapturePlugin);

		// Processing
		TemplateProcessingPlugin simpleProcessingPlugin = new TemplateProcessingPlugin();
		pluginList.put(simpleProcessingPlugin.getID(), simpleProcessingPlugin);
		listProcessing.add(simpleProcessingPlugin);

		// Export
		TemplateExportPlugin testExportPlugin = new TemplateExportPlugin();
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

	@Override
	public void onAutoFocusMoving(boolean start)
	{
		// TODO Auto-generated method stub
		
	}
}
