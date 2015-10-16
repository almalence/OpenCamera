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

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class NotificationService extends Service
{

	private static final int	NOTIFICATION_ID		= 10;
	private Notification		notification;
	private boolean				notificationShown	= false;
	private int					notificationCount		= 0;

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	protected synchronized void showNotification()
	{
		notificationCount++;

		// Notification already shown.
		if (notificationShown)
			return;

		if (notification == null)
		{
			Bitmap bigIcon = BitmapFactory.decodeResource(getResources(), R.drawable.icon);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
			{
				notification = new Notification.Builder(this).setContentTitle(getString(R.string.app_name))
						.setContentText(getString(R.string.string_processing_and_saving_image))
						.setSmallIcon(getApplicationInfo().icon).setLargeIcon(bigIcon).build();
			} else
			{
				notification = new NotificationCompat.Builder(this).setSmallIcon(getApplicationInfo().icon).setLargeIcon(bigIcon)
						.setContentTitle(getString(R.string.app_name))
						.setContentText(getString(R.string.string_processing_and_saving_image)).build();
			}
		}

		startForeground(NOTIFICATION_ID, notification);
		notificationShown = true;
	}

	protected synchronized void hideNotification()
	{
		notificationCount--;

		// Notification already hidden.
		if (!notificationShown)
			return;

		if (notificationCount == 0)
		{
			stopForeground(true);
			notificationShown = false;
		}
	}
}
