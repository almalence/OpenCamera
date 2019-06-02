/* <!-- +++
 package com.almalence.opencam_plus;
 +++ --> */
//<!-- -+-
package com.almalence.opencam;

//-+- -->

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;

/* <!-- +++
import com.almalence.opencam_plus.cameracontroller.CameraController;
+++ --> */
//<!-- -+-
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

public class AlarmReceiver extends BroadcastReceiver
{
	private static final String		TAG							= "ALARM_RECIVER";

	private static AlarmManager		alarmMgr;
	private static PendingIntent	alarmIntent;

	private int						pauseBetweenShotsVal		= 0;
	private static long				pauseBetweenShots			= 0;
	private int						pauseBetweenShotsMeasurment	= 0;
	private static boolean			readyToTakePicture			= false;
	private static AlarmReceiver	thiz						= null;
	private static WakeLock			wakeLock					= null;

	public static AlarmReceiver getInstance()
	{
		if (thiz == null)
		{
			thiz = new AlarmReceiver();
			if (wakeLock == null)
			{
				PowerManager pm = (PowerManager) ApplicationScreen.instance.getApplicationContext()
						.getSystemService(Context.POWER_SERVICE);
				wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
						| PowerManager.ON_AFTER_RELEASE, TAG);
			}
		}
		return thiz;
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (wakeLock == null)
		{
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
					| PowerManager.ON_AFTER_RELEASE, TAG);
		}
		if (!wakeLock.isHeld())
		{
			wakeLock.acquire();
		}

		readyToTakePicture = true;

		try
		{
			if (ApplicationScreen.instance == null || ApplicationScreen.getCameraController() == null ||
				(CameraController.isUseCamera2()? CameraController.getCamera2() == null : CameraController.getCamera() == null))
			{
				Intent dialogIntent = new Intent(context, MainScreen.class);
				dialogIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
						| Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				context.startActivity(dialogIntent);
				if (wakeLock != null)
					if (wakeLock.isHeld())
						wakeLock.release();

			} else
			{
				takePicture();
			}
		} catch (NullPointerException e)
		{
		}
	}

	public void takePicture()
	{
		if (!readyToTakePicture)
		{
			return;
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());

		boolean photoTimeLapseActive = prefs.getBoolean(MainScreen.sPhotoTimeLapseActivePref, false);
		boolean photoTimeLapseIsRunning = prefs.getBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);

		if (!photoTimeLapseActive || !photoTimeLapseIsRunning)
		{
			return;
		}

		PluginManager.getInstance().onShutterClickNotUser();

		readyToTakePicture = false;
	}

	public void setNextAlarm(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());

		pauseBetweenShotsVal = prefs.getInt(MainScreen.sPhotoTimeLapseCaptureIntervalPref, 5);
		if (pauseBetweenShotsVal == 0)
		{
			return;
		}

		pauseBetweenShots = pauseBetweenShotsVal;

		pauseBetweenShotsMeasurment = prefs.getInt(MainScreen.sPhotoTimeLapseCaptureIntervalMeasurmentPref, 0);

		switch (pauseBetweenShotsMeasurment)
		{
		case 0://secs
			pauseBetweenShots = pauseBetweenShots * 1000;
			break;
		case 1://mins
			pauseBetweenShots = pauseBetweenShots * 60000;
			break;
		case 2://hours
			pauseBetweenShots = pauseBetweenShots * 60000 * 60;
			break;
		case 3://days
			pauseBetweenShots = pauseBetweenShots * 60000 * 60 * 24;
			break;
		default:
			break;
		}

		Editor e = prefs.edit();
		e.putInt(MainScreen.sPhotoTimeLapseCount, prefs.getInt(MainScreen.sPhotoTimeLapseCount, 0) + 1);
		e.commit();
		this.setAlarm(context, pauseBetweenShots);

		ComponentName receiver = new ComponentName(context, AlarmReceiver.class);
		PackageManager pm = context.getPackageManager();

		pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);

		if (wakeLock != null)
			if (wakeLock.isHeld())
				wakeLock.release();
	}

	@SuppressLint("NewApi")
	public void setAlarm(Context context, long intervalMillis)
	{
		alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmReceiver.class);
		alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

		if (Build.VERSION.SDK_INT < 19)
		{
			alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + intervalMillis, alarmIntent);
		} else
		{
			alarmMgr.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + intervalMillis, alarmIntent);
		}
	}

	public static void cancelAlarm(Context context)
	{
		if (alarmMgr != null && alarmIntent != null)
		{
			alarmMgr.cancel(alarmIntent);
		}

		if (wakeLock != null)
			if (wakeLock.isHeld())
				wakeLock.release();
	}
}
