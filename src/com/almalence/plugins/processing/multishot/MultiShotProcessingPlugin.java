package com.almalence.plugins.processing.multishot;

import java.util.ArrayList;

import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;

public abstract class MultiShotProcessingPlugin implements Handler.Callback, OnClickListener
{
	public abstract void setYUVBufferList(ArrayList<Integer> list);
	public abstract void onStartPostProcessing();
	public abstract void onStartProcessing(long sessionID);
	public abstract View getPostProcessingView();
	public abstract boolean onKeyDown(int keyCode, KeyEvent event);
}
