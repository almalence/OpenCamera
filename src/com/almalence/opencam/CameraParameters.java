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
//<!-- -+-
package com.almalence.opencam;
//-+- -->

public final class CameraParameters
{
	private CameraParameters(){}
	
	//SCENE
	public static final int SCENE_MODE_DISABLED = -1;
	public static final int SCENE_MODE_AUTO = 1;	
	public static final int SCENE_MODE_ACTION = 2;
	public static final int SCENE_MODE_PORTRAIT = 3;
	public static final int SCENE_MODE_LANDSCAPE = 4;
	public static final int SCENE_MODE_NIGHT = 5;
	public static final int SCENE_MODE_NIGHT_PORTRAIT = 6;
	public static final int SCENE_MODE_THEATRE = 7;
	public static final int SCENE_MODE_BEACH = 8;
	public static final int SCENE_MODE_SNOW = 9;
	public static final int SCENE_MODE_SUNSET = 10;
	public static final int SCENE_MODE_STEADYPHOTO = 11;
	public static final int SCENE_MODE_FIREWORKS = 12;
	public static final int SCENE_MODE_SPORTS = 13;
	public static final int SCENE_MODE_PARTY = 14;
	public static final int SCENE_MODE_CANDLELIGHT = 15;
	public static final int SCENE_MODE_BARCODE = 16;
	public static final int SCENE_MODE_HIGH_SPEED_VIDEO = 17;
	
	//WHITE BALANCE
	public static final int WB_MODE_AUTO = 1;
	public static final int WB_MODE_INCANDESCENT = 2;
	public static final int WB_MODE_FLUORESCENT = 3;
	public static final int WB_MODE_WARM_FLUORESCENT = 4;
	public static final int WB_MODE_DAYLIGHT = 5;
	public static final int WB_MODE_CLOUDY_DAYLIGHT = 6;
	public static final int WB_MODE_TWILIGHT = 7;
	public static final int WB_MODE_SHADE = 8;
	
	//FOCUS
	public static final int AF_MODE_AUTO = 1;
	public static final int AF_MODE_MACRO = 2;
	public static final int AF_MODE_CONTINUOUS_VIDEO = 3;
	public static final int AF_MODE_CONTINUOUS_PICTURE = 4;
	public static final int AF_MODE_EDOF = 5;
	public static final int AF_MODE_INFINITY = 6;
	public static final int AF_MODE_NORMAL = 7;	
	public static final int AF_MODE_FIXED = 8;
	
	//FLASH
	public static final int FLASH_MODE_OFF = 0;
	public static final int FLASH_MODE_AUTO = 1;
	public static final int FLASH_MODE_SINGLE = 2;
	public static final int FLASH_MODE_REDEYE = 3;
	public static final int FLASH_MODE_TORCH = 4;
	
	//ISO	
	public static final int ISO_50 = 0;
	public static final int ISO_AUTO = 1;
	public static final int ISO_100 = 2;
	public static final int ISO_200 = 3;
	public static final int ISO_400 = 4;
	public static final int ISO_800 = 5;
	public static final int ISO_1600 = 6;
	public static final int ISO_3200 = 7;
	
	//Possible names of iso in Camera.Parameters variable
	public static final String isoParam = "iso";
	public static final String isoParam2 = "iso-speed";
	public static final String isoParam3 = "nv-picture-iso";
	
	public static final int meteringModeAuto = 0;
	public static final int meteringModeMatrix = 1;	
	public static final int meteringModeCenter = 2;
	public static final int meteringModeSpot = 3;
}
