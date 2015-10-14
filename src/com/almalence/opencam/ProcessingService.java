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

import android.content.Intent;
import android.os.AsyncTask;

public class ProcessingService extends NotificationService
{
	@Override
	public int onStartCommand(Intent intent, int flags, int startid)
	{
		long sessionID = intent.getLongExtra("sessionID", 0);
		if (sessionID == 0)
		{
			return START_NOT_STICKY;
		}

		ProcessingTask task = new ProcessingTask();
		task.sessionID = sessionID;
		task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

		return START_NOT_STICKY;
	}

	private class ProcessingTask extends AsyncTask<Void, Void, Void>
	{
		public long	sessionID	= 0;	// id to identify data flow

		@Override
		protected void onPreExecute()
		{
			showNotification();	
		}
		
		@Override
		protected Void doInBackground(Void... params)
		{
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
			Plugin processing;
			Plugin export;

			// Get processing plugin for current sessionID and remove it from
			// map.
			processing = PluginManager.getInstance().processingPluginList.get(sessionID);
			PluginManager.getInstance().processingPluginList.remove(sessionID);

			export = PluginManager.getInstance().pluginList.get(PluginManager.getInstance().activeExport);

			if (null != processing)
			{
//				Log.wtf("AlmaShot", "Processing ThreadPriority: " + android.os.Process.getThreadPriority(android.os.Process.myTid()));
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
				export.onExportActive(sessionID);
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
