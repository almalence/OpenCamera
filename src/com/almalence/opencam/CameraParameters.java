package com.almalence.opencam;

public final class CameraParameters
{
	private CameraParameters(){}
	
	//SCENE
	public final static int SCENE_MODE_AUTO = 1;
	public final static int SCENE_MODE_ACTION = 2;
	public final static int SCENE_MODE_PORTRAIT = 3;
	public final static int SCENE_MODE_LANDSCAPE = 4;
	public final static int SCENE_MODE_NIGHT = 5;
	public final static int SCENE_MODE_NIGHT_PORTRAIT = 6;
	public final static int SCENE_MODE_THEATRE = 7;
	public final static int SCENE_MODE_BEACH = 8;
	public final static int SCENE_MODE_SNOW = 9;
	public final static int SCENE_MODE_SUNSET = 10;
	public final static int SCENE_MODE_STEADYPHOTO = 11;
	public final static int SCENE_MODE_FIREWORKS = 12;
	public final static int SCENE_MODE_SPORTS = 13;
	public final static int SCENE_MODE_PARTY = 14;
	public final static int SCENE_MODE_CANDLELIGHT = 15;
	public final static int SCENE_MODE_BARCODE = 16;
	
	//WHITE BALANCE
	public final static int AWB_MODE_AUTO = 1;
	public final static int AWB_MODE_INCANDESCENT = 2;
	public final static int AWB_MODE_FLUORESCENT = 3;
	public final static int AWB_MODE_WARM_FLUORESCENT = 4;
	public final static int AWB_MODE_DAYLIGHT = 5;
	public final static int AWB_MODE_CLOUDY_DAYLIGHT = 6;
	public final static int AWB_MODE_TWILIGHT = 7;
	public final static int AWB_MODE_SHADE = 8;
	
	//FOCUS
	public final static int AF_MODE_AUTO = 1;
	public final static int AF_MODE_MACRO = 2;
	public final static int AF_MODE_CONTINUOUS_VIDEO = 3;
	public final static int AF_MODE_CONTINUOUS_PICTURE = 4;
	public final static int AF_MODE_EDOF = 5;
	public final static int AF_MODE_INFINITY = 6;
	public final static int AF_MODE_NORMAL = 7;	
	public final static int AF_MODE_FIXED = 8;
	
	//FLASH
	public final static int FLASH_MODE_OFF = 0;
	public final static int FLASH_MODE_SINGLE = 1;
	public final static int FLASH_MODE_TORCH = 2;
	public final static int FLASH_MODE_AUTO = 3;
	public final static int FLASH_MODE_REDEYE = 4;
	
	//Possible names of iso in Camera.Parameters variable
	public final static String isoParam = "iso";
	public final static String isoParam2 = "iso-speed";
}
