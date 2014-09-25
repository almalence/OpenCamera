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

import java.util.ArrayList;
import java.util.List;

/***
 * Mode class - describes each mode
 * 
 * Mode is a set of plugins. Mode describes current application configuration.
 * 
 * For example: - RGB and Luminance gistogram and center cross VF plugin -
 * expo-bracketing capture plugin - HDR processing plugin - simple filters
 * plugin - png/jpeg/gif export plugins
 * 
 * i.e. modes describes which plugins are active currently.
 * 
 * We can configure as many modes as needed.
 * 
 * Modes are configured with configuration file mode.xml located in assets
 * directory
 * 
 * Mode.java class shouldn't be changed by user - modes will be filled on
 * startup and stored in list in PluginManager
 ***/

public class Mode
{
	public String		modeID;			// unique mode id
	public String		modeName;		// mode visible name - any
	public String		modeNameHAL;	// mode visible name for HAL - any
	public String		modeSaveName;	// mode name for save
	public String		modeSaveNameHAL;	// mode name for save for HAL
	public List<String>	VF;				// list of VF plugins available in this
										// mode
	public String		Capture;		// Capture plugin
	public String		Processing;		// Processing plugin
	public List<String>	Filter;			// list of Filter plugin
	public String		Export;			// Export plugin

	public String		howtoText;		// text string describing how to use
										// mode

	public String		SKU;			// SKU for billing purposes

	public String		icon;			// mode icon name
	public String		iconHAL;		// mode icon name

	public Mode()
	{
		modeID = "";
		modeName = "";
		icon = "";
		modeNameHAL = "";
		iconHAL = "";
		modeSaveName = "";
		modeSaveNameHAL = "";
		Capture = "";
		Processing = "";
		Export = "";
		howtoText = "";
		VF = new ArrayList<String>();
		Filter = new ArrayList<String>();

		SKU = "";
	}
}
