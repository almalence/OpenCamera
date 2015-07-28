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

import java.util.LinkedList;
import java.util.Queue;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class ProcessingService extends Service
{

	private static final int	NOTIFICATION_ID		= 10;
	private Notification		notification;
	private boolean				notificationShown	= false;
	private int					processingCount		= 0;

	// We need this queue to save images in right order (as they were captured).
	private static Queue<Long>	processingQueue		= new LinkedList<Long>();

	@Override
	public int onStartCommand(Intent intent, int flags, int startid)
	{
		long sessionID = intent.getLongExtra("sessionID", 0);
		if (sessionID == 0)
		{
			return START_NOT_STICKY;
		}

		processingQueue.add(sessionID);
		ProcessingTask task = new ProcessingTask();
		task.sessionID = sessionID;
		task.bundle = intent.getExtras();
		task.execute();

		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	private synchronized void showNotification()
	{
		processingCount++;

		// Notification already shown.
		if (notificationShown)
			return;

		if (notification == null)
		{
			Bitmap bigIcon = BitmapFactory.decodeResource(getResources(), R.drawable.icon);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
			{
				notification = new Notification.Builder(this).setContentTitle(getString(R.string.app_name))
						.setContentText(getString(R.string.string_processing_image))
						.setSmallIcon(R.drawable.icon).setLargeIcon(bigIcon).build();
			} else
			{
				notification = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.icon).setLargeIcon(bigIcon)
						.setContentTitle(getString(R.string.app_name))
						.setContentText(getString(R.string.string_processing_image)).build();
			}
		}

		startForeground(NOTIFICATION_ID, notification);
		notificationShown = true;
	}

	private synchronized void hideNotification()
	{
		processingCount--;

		// Notification already hidden.
		if (!notificationShown)
			return;

		if (processingCount == 0)
		{
			stopForeground(true);
			notificationShown = false;
		}
	}

	private class ProcessingTask extends AsyncTask<Void, Void, Void>
	{
		public long	sessionID	= 0;	// id to identify data flow
		Bundle		bundle;

		@Override
		protected void onPreExecute()
		{
			showNotification();			
		}
		
		@Override
		protected Void doInBackground(Void... params)
		{
			Plugin processing;
			Plugin export;

			// Get processing plugin for current sessionID and remove it from
			// map.
			processing = PluginManager.getInstance().processingPluginList.get(sessionID);
			PluginManager.getInstance().processingPluginList.remove(sessionID);

			export = PluginManager.getInstance().pluginList.get(PluginManager.getInstance().activeExport);

			// if post processing not needed - save few values
			// from intent to shared memory for current session
			if (null != processing)
				if (!processing.isPostProcessingNeeded())
				{
					PluginManager.getInstance().addToSharedMem("imageHeight" + sessionID,
							String.valueOf(bundle.getInt("imageHeight", 0)));
					PluginManager.getInstance().addToSharedMem("imageWidth" + sessionID,
							String.valueOf(bundle.getInt("imageWidth", 0)));
					PluginManager.getInstance().addToSharedMem("wantLandscapePhoto" + sessionID,
							String.valueOf(bundle.getBoolean("wantLandscapePhoto", false)));
					PluginManager.getInstance().addToSharedMem("CameraMirrored" + sessionID,
							String.valueOf(bundle.getBoolean("cameraMirrored", false)));
				}

			if (null != processing)
			{
				processing.onStartProcessing(sessionID);
				if (processing.isPostProcessingNeeded())
				{
					ApplicationScreen.getMessageHandler().sendEmptyMessage(
							ApplicationInterface.MSG_START_POSTPROCESSING);
					return null;
				}
			}

			if (null != export)
			{
				// Sleep until current session become the head of queue.
				while(processingQueue.element() != sessionID) {
					try
					{
						Thread.sleep(10);
					} catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				export.onExportActive(sessionID);
				
				// Remove current session from queue. 
				processingQueue.remove();
			} else
			{
				if (ApplicationScreen.instance != null && ApplicationScreen.getMessageHandler() != null)
				{
					ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED);
				}
				PluginManager.getInstance().clearSharedMemory(sessionID);
			}

			return null;
		}
		
		@Override
		protected void onPostExecute(Void result)
		{
			hideNotification();
		}
	}
}
