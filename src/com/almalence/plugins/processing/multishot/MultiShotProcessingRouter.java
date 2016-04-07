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

package com.almalence.plugins.processing.multishot;

import java.util.ArrayList;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.almalence.asynctaskmanager.OnTaskCompleteListener;
import com.almalence.plugins.processing.groupshot.GroupShotProcessingPlugin;
import com.almalence.plugins.processing.groupshot.GroupShotProcessingPlugin;
import com.almalence.plugins.processing.objectremoval.ObjectRemovalProcessingPlugin;
import com.almalence.plugins.processing.sequence.SequenceProcessingPlugin;
import com.almalence.ui.RotateLayout;
import com.almalence.util.ImageConversion;
/* <!-- +++
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.PluginProcessing;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.ApplicationInterface;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginProcessing;
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.R;

//-+- -->

/***
 * Implements multishot processing
 ***/

public class MultiShotProcessingRouter extends PluginProcessing implements OnTaskCompleteListener, Handler.Callback,
		OnClickListener
{

	private static int								SELECTED_GROUP_SHOT				= 0;
	private static int								SELECTED_SEQUENCE				= 1;
	private static int								SELECTED_OBJECT_REMOVAL			= 2;
	private static int								PROCESSING_CANCELLED			= -2;
	private static int								WAITING_FOR_SELECTION			= -1;

	private View									mButtonsLayout;

	private static GroupShotProcessingPlugin		groupShotProcessingPlugin		= new GroupShotProcessingPlugin();
	private static SequenceProcessingPlugin			sequenceProcessingPlugin		= new SequenceProcessingPlugin();
	private static ObjectRemovalProcessingPlugin	objectRemovalProcessingPlugin	= new ObjectRemovalProcessingPlugin();
	private static MultiShotProcessingPlugin		selectedProcessingPlugin		= null;

	private static LinearLayout						buttonObjectRemoval				= null;
	private static LinearLayout						buttonGroupShot					= null;
	private static LinearLayout						buttonSequence					= null;

	private int										state							= PROCESSING_CANCELLED;
	private long									sessionID;

	private boolean									mSaveInputPreference;
	private static ArrayList<Integer>				mYUVBufferList					= new ArrayList<Integer>();

	public MultiShotProcessingRouter()
	{
		super("com.almalence.plugins.multishotprocessing", "multishot", R.xml.preferences_processing_multishot, 0, 0,
				null);
	}

	@Override
	public void onGUICreate()
	{
		LayoutInflater inflator = ApplicationScreen.instance.getLayoutInflater();
		mButtonsLayout = inflator.inflate(R.layout.plugin_processing_multishot_options_layout, null, false);

		buttonObjectRemoval = (LinearLayout) mButtonsLayout.findViewById(R.id.buttonObjectRemoval);
		buttonGroupShot = (LinearLayout) mButtonsLayout.findViewById(R.id.buttonGroupShot);
		buttonSequence = (LinearLayout) mButtonsLayout.findViewById(R.id.buttonSequence);

		ApplicationScreen.getGUIManager().removeViews(mButtonsLayout, R.id.blockingLayout);

		buttonObjectRemoval.setOnClickListener(this);
		buttonGroupShot.setOnClickListener(this);
		buttonSequence.setOnClickListener(this);
		mButtonsLayout.setOnClickListener(this);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT);

		if (ApplicationScreen.instance.findViewById(R.id.blockingLayout) != null)
			((RelativeLayout) ApplicationScreen.instance.findViewById(R.id.blockingLayout)).addView(mButtonsLayout,
					params);

		if (state == WAITING_FOR_SELECTION)
		{
			mButtonsLayout.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public View getPostProcessingView()
	{
		if (selectedProcessingPlugin != null)
			return selectedProcessingPlugin.getPostProcessingView();
		
		return null;
	}

	@Override
	public void onStart()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.instance
				.getBaseContext());
		mSaveInputPreference = prefs.getBoolean("saveInputPrefMultiShot", false);

		groupShotProcessingPlugin.onStart();
		sequenceProcessingPlugin.onStart();
		objectRemovalProcessingPlugin.onStart();
	}

	@Override
	public void onStartProcessing(long SessionID)
	{
		this.sessionID = SessionID;

		state = WAITING_FOR_SELECTION;
		selectedProcessingPlugin = null;

		ApplicationScreen.instance.runOnUiThread(new Runnable()
		{
			public void run()
			{
				mButtonsLayout.setVisibility(View.VISIBLE);
				ApplicationScreen.instance.findViewById(R.id.blockingText).setVisibility(View.GONE);
				Message msg = new Message();
				msg.what = ApplicationInterface.MSG_PROCESSING_BLOCK_UI;
				ApplicationScreen.getMessageHandler().sendMessage(msg);
			}
		});

		prepareDataForProcessing();

		// While plugin is not selected, sleep thread to prevent unnecessary CPU
		// usage.
		while (state == WAITING_FOR_SELECTION)
		{
			try
			{
				Thread.sleep(100);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}

		ApplicationScreen.instance.runOnUiThread(new Runnable()
		{
			public void run()
			{
				ApplicationScreen.instance.findViewById(R.id.blockingText).setVisibility(View.VISIBLE);
			}
		});

		if (selectedProcessingPlugin != null)
		{
			selectedProcessingPlugin.setYUVBufferList(mYUVBufferList);
			selectedProcessingPlugin.onStartProcessing(sessionID);
		}
	}

	private void prepareDataForProcessing()
	{
		int imagesAmount = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
				"amountofcapturedframes" + sessionID));

		if (imagesAmount == 0)
			imagesAmount = 1;

		mYUVBufferList.clear();

		for (int i = 1; i <= imagesAmount; i++)
		{
			int yuv = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frame" + i + sessionID));
			mYUVBufferList.add(i - 1, yuv);
		}

		if (mSaveInputPreference)
		{
			try
			{
				String fileFormat = PluginManager.getInstance().getFileFormat();

				for (int i = 0; i < imagesAmount; ++i)
				{
					if (state != WAITING_FOR_SELECTION)
					{
						ApplicationScreen.instance.runOnUiThread(new Runnable()
						{
							public void run()
							{
								ApplicationScreen.instance.findViewById(R.id.blockingText).setVisibility(View.VISIBLE);
							}
						});
					}

					String index = String.format("_%02d", i);

					PluginManager.getInstance().saveInputFile(true, sessionID, i, null, mYUVBufferList.get(i),
							fileFormat + index);
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	/************************************************
	 * POST PROCESSING
	 ************************************************/
	@Override
	public boolean isPostProcessingNeeded()
	{
		if (state == PROCESSING_CANCELLED)
		{
			return false;
		}
		return true;
	}

	public void onStartPostProcessing()
	{
		selectedProcessingPlugin.onStartPostProcessing();
	}

	@Override
	public void onClick(View v)
	{
		if (state == WAITING_FOR_SELECTION)
		{
			if (v == buttonObjectRemoval)
			{
				selectedProcessingPlugin = objectRemovalProcessingPlugin;
				state = SELECTED_OBJECT_REMOVAL;
			}
			if (v == buttonGroupShot)
			{
				selectedProcessingPlugin = groupShotProcessingPlugin;			
				state = SELECTED_GROUP_SHOT;
			}
			if (v == buttonSequence)
			{
				selectedProcessingPlugin = sequenceProcessingPlugin;
				state = SELECTED_SEQUENCE;
			}

			mButtonsLayout.setVisibility(View.GONE);
			return;
		}

		if (state != PROCESSING_CANCELLED && selectedProcessingPlugin != null)
			selectedProcessingPlugin.onClick(v);
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		if (selectedProcessingPlugin != null)
			return ((Callback) selectedProcessingPlugin).handleMessage(msg);
		
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		boolean res = false;

		if (selectedProcessingPlugin != null)
			res = selectedProcessingPlugin.onKeyDown(keyCode, event);

		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			if (state == PROCESSING_CANCELLED)
			{
				return false;
			}

			ApplicationScreen.instance.findViewById(R.id.blockingText).setVisibility(View.VISIBLE);
			mButtonsLayout.setVisibility(View.GONE);

			mYUVBufferList.clear();

			ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_POSTPROCESSING_FINISHED);
			state = PROCESSING_CANCELLED;
			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST,
					ApplicationInterface.MSG_CONTROL_UNLOCKED);
			ApplicationScreen.getGUIManager().lockControls = false;

			return true;
		}

		if (res)
		{
			return res;
		}

		return super.onKeyDown(keyCode, event);
	}

	/************************************************
	 * POST PROCESSING END
	 ************************************************/

	@Override
	public void onPause()
	{
		if (mButtonsLayout != null)
		{
			ApplicationScreen.getGUIManager().removeViews(mButtonsLayout, R.id.specialPluginsLayout3);
		}
	}

	@Override
	public void onOrientationChanged(int orientation)
	{
		RotateLayout rotateLayout = (RotateLayout) ApplicationScreen.instance.findViewById(R.id.rotateLayout);
		if (rotateLayout != null)
		{
			rotateLayout.setAngle(orientation - 90);
			rotateLayout.requestLayout();
		}
	}
}
