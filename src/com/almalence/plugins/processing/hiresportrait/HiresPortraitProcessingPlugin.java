package com.almalence.plugins.processing.hiresportrait;

//import com.almalence.opencam.MainScreen;
//import com.almalence.opencam.PluginManager;
//import com.almalence.opencam.PluginProcessing;

/***
Implements hires portrait processing plugin
***/

public class HiresPortraitProcessingPlugin //extends PluginProcessing
{
//	private long sessionID=0;
//	
//	public HiresPortraitProcessingPlugin()
//	{
//		super("com.almalence.plugins.hiresportraitprocessing", 0, 0, 0, null);
//	}
//	
//	@Override
//	public void onStartProcessing(long SessionID)
//	{
//		sessionID=SessionID;
//		
//		int frame1 = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frame"/*+Long.toString(sessionID)*/));
//		int len1 = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("framelen"/*+Long.toString(sessionID)*/));
//		boolean wantLandscape1 = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("frameorientation"/*+Long.toString(sessionID)*/));
//		boolean cameraMirrored1 = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("framemirrored"/*+Long.toString(sessionID)*/));
//		
//		int iSaveImageWidth = MainScreen.getSaveImageWidth();
//		int iSaveImageHeight = MainScreen.getSaveImageHeight();
//		
////		int frame2 = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frame2"/*+Long.toString(sessionID)*/));
////		int len2 = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("framelen2"/*+Long.toString(sessionID)*/));
////		boolean wantLandscape2 = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("frameorientation2"/*+Long.toString(sessionID)*/));
////		boolean cameraMirrored2 = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("framemirrored2"/*+Long.toString(sessionID)*/));
//
//		
//		PluginManager.getInstance().addToSharedMem("resultframeformat1"+Long.toString(sessionID), "jpeg");
//		PluginManager.getInstance().addToSharedMem("resultframe1"+Long.toString(sessionID), String.valueOf(frame1));
//    	PluginManager.getInstance().addToSharedMem("resultframelen1"+Long.toString(sessionID), String.valueOf(len1));
//    	PluginManager.getInstance().addToSharedMem("resultframeorientation1" +String.valueOf(sessionID), String.valueOf(wantLandscape1? 0 : 90));
//    	PluginManager.getInstance().addToSharedMem("resultframemirrored1" +String.valueOf(sessionID), String.valueOf(cameraMirrored1));
//		PluginManager.getInstance().addToSharedMem("amountofresultframes"+Long.toString(sessionID), "1");
//		
//		PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(sessionID), String.valueOf(iSaveImageWidth));
//    	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(sessionID), String.valueOf(iSaveImageHeight));
//	}
//
////	public void FreeMemory()
////    {
////		PluginManager.getInstance().removeFromSharedMemory("frame");
////		PluginManager.getInstance().removeFromSharedMemory("framelen");
////		PluginManager.getInstance().removeFromSharedMemory("frameorientation");
////		PluginManager.getInstance().removeFromSharedMemory("framemirrored");
//////		PluginManager.getInstance().removeFromSharedMemory("frame2");
//////		PluginManager.getInstance().removeFromSharedMemory("framelen2");
//////		PluginManager.getInstance().removeFromSharedMemory("frameorientation2");
//////		PluginManager.getInstance().removeFromSharedMemory("framemirrored2");
////
////		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
////		Camera.getCameraInfo(MainScreen.CameraIndex,cameraInfo);
////		if (cameraInfo.facing != Camera.CameraInfo.CAMERA_FACING_FRONT)
////		{
////			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
////			Editor prefsEditor = prefs.edit();
////			prefsEditor.putBoolean("useFrontCamera", true);
////			prefsEditor.commit();
////			
////			MainScreen.thiz.PauseMain();
////			MainScreen.thiz.ResumeMain();
////		}
////    }
//	
//	@Override
//	public boolean isPostProcessingNeeded(){return false;}
//
//	@Override
//	public void onStartPostProcessing(){}
}
