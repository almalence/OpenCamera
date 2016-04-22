package com.almalence.plugins.processing.multishot;

import java.util.ArrayList;

import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;

/* <!-- +++
import com.almalence.opencam_plus.PluginProcessing;
+++ --> */
//<!-- -+-
import com.almalence.opencam.PluginProcessing;
//-+- -->

public abstract class MultiShotProcessingPlugin extends PluginProcessing implements Handler.Callback, OnClickListener
{
	public MultiShotProcessingPlugin(String ID, String mode, int preferenceID,
									 int advancedPreferenceID, int quickControlID,
								 	 String quickControlInitTitle)
	{
		super(ID, mode, preferenceID, advancedPreferenceID, quickControlID,
				quickControlInitTitle);
	}
	public abstract void setYUVBufferList(ArrayList<Integer> list);
	public abstract void onStartPostProcessing();
	public abstract void onStartProcessing(long sessionID);
	public abstract View getPostProcessingView();
	public abstract boolean onKeyDown(int keyCode, KeyEvent event);
}
