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

package com.almalence.plugins.processing.bestshot;

/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.PluginProcessing;
+++ --> */
// <!-- -+-
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.util.Log;

import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginProcessing;
//-+- -->
import com.almalence.SwapHeap;
import com.almalence.plugins.processing.bestshot.AlmaShotBestShot;
import com.almalence.util.ImageConversion;

/***
Implements simple processing plugin - just translate shared memory values 
from captured to result.
***/

public class BestshotProcessingPlugin extends PluginProcessing
{
	private long sessionID=0;
	
	public BestshotProcessingPlugin()
	{
		super("com.almalence.plugins.bestshotprocessing", 0, 0, 0, null);
	}
	
	@Override
	public void onStartProcessing(long SessionID)
	{
		sessionID=SessionID;
		
		int iSaveImageWidth = MainScreen.getSaveImageWidth();
		int iSaveImageHeight = MainScreen.getSaveImageHeight();
		
		int mImageWidth = MainScreen.getImageWidth();
		int mImageHeight = MainScreen.getImageHeight();
		
		String num = PluginManager.getInstance().getFromSharedMem("amountofcapturedframes"+Long.toString(sessionID));
		if (num == null)
			return;
		int imagesAmount = Integer.parseInt(num);
		
		if (imagesAmount==0)
			imagesAmount=1;
		
		int orientation = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frameorientation1" +Long.toString(sessionID)));
		AlmaShotBestShot.Initialize();
		
		
		int compressed_frame[] = new int[imagesAmount];
        int compressed_frame_len[] = new int[imagesAmount];
        
        for (int i=0; i<imagesAmount; i++)
		{
			compressed_frame[i] = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frame" + (i+1)+Long.toString(sessionID)));
			compressed_frame_len[i] = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("framelen" + (i+1)+Long.toString(sessionID)));
		}

		AlmaShotBestShot.ConvertFromJpeg(
    			compressed_frame,
    			compressed_frame_len,
    			imagesAmount,
    			mImageWidth, mImageHeight);

		int idxResult = AlmaShotBestShot.BestShotProcess(imagesAmount, mImageWidth, mImageHeight, compressed_frame);				
		Log.e("BESTSHOT", "best is " + idxResult);
		AlmaShotBestShot.Release();

		if(orientation == 90 || orientation == 270)
		{					
			PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(sessionID), String.valueOf(iSaveImageHeight));
	    	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(sessionID), String.valueOf(iSaveImageWidth));
		}
		else
		{
			PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(sessionID), String.valueOf(iSaveImageWidth));
	    	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(sessionID), String.valueOf(iSaveImageHeight));
		}
		
		/**/
//		File saveDir = PluginManager.getInstance().GetSaveDir(false);
//		String fileFormat;
//		for (int i =0;i<imagesAmount;i++)
//		{
//			File file;
//	    	if (MainScreen.ForceFilename == null)
//	        {
//	    		fileFormat = "IMG_" + i+".jpg";
//	    		file = new File(
//	            		saveDir, 
//	            		fileFormat);
//	        }
//	        else
//	        {
//	        	file = MainScreen.ForceFilename;
//	        	MainScreen.ForceFilename = null;
//	        }
//	    	FileOutputStream os = null;
//			try {
//				os = new FileOutputStream(file);
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//			byte[] frame1 = SwapHeap.CopyFromHeap(
//					compressed_frame[i],
//					compressed_frame_len[i]);
//			try {
//				os.write(frame1);
//				os.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//		}
		/**/
		int frame = compressed_frame[idxResult];
		int len = compressed_frame_len[idxResult];
		
		PluginManager.getInstance().addToSharedMem("resultframeformat1"+Long.toString(sessionID), "jpeg");
		PluginManager.getInstance().addToSharedMem("resultframe1"+Long.toString(sessionID), String.valueOf(frame));
    	PluginManager.getInstance().addToSharedMem("resultframelen1"+Long.toString(sessionID), String.valueOf(len));
		
		boolean cameraMirrored = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("framemirrored1" + Long.toString(sessionID)));
    	PluginManager.getInstance().addToSharedMem("resultframeorientation1" + String.valueOf(sessionID), String.valueOf(orientation));
    	PluginManager.getInstance().addToSharedMem("resultframemirrored1" + String.valueOf(sessionID), String.valueOf(cameraMirrored));
		
		PluginManager.getInstance().addToSharedMem("amountofresultframes"+Long.toString(sessionID), String.valueOf(1));
	}

	@Override
	public boolean isPostProcessingNeeded(){return false;}

	@Override
	public void onStartPostProcessing(){}
}
