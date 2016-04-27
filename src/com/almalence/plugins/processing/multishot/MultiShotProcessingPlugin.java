package com.almalence.plugins.processing.multishot;

import java.util.ArrayList;

import android.content.SharedPreferences;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;

import com.almalence.asynctaskmanager.OnTaskCompleteListener;
import com.almalence.opencamunderground.ApplicationInterface;
import com.almalence.opencamunderground.ApplicationScreen;
import com.almalence.opencamunderground.PluginManager;
import com.almalence.opencamunderground.PluginProcessing;
import com.almalence.opencamunderground.R;
import com.almalence.opencamunderground.cameracontroller.CameraController;
import com.almalence.plugins.processing.groupshot.GroupShotProcessingPlugin;
import com.almalence.plugins.processing.objectremoval.ObjectRemovalProcessingPlugin;
import com.almalence.plugins.processing.sequence.SequenceProcessingPlugin;
import com.almalence.ui.RotateLayout;
import com.almalence.util.ImageConversion;

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
