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
import android.view.Window;

import com.almalence.opencam.ui.SelfTimerAndPhotoTimeLapse;
//<!-- -+-
//-+- -->

/* <!-- +++
 import com.almalence.opencam_plus.ui.SelfTimerAndPhotoTimeLapse;
 +++ --> */

public class AlarmReceiver extends BroadcastReceiver
{
	private static final String		TAG							= "ALARM_RECIVER";

	private static AlarmManager		alarmMgr;
	private static PendingIntent	alarmIntent;

	private int						pauseBetweenShotsVal		= 0;
	private static long				pauseBetweenShots			= 0;
	private int						pauseBetweenShotsMeasurment	= 0;
	private static AlarmReceiver	thiz						= null;

	public static AlarmReceiver getInstance()
	{
		if (thiz == null)
		{
			thiz = new AlarmReceiver();
		}
		return thiz;
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		PowerManager pm = (PowerManager) context.getApplicationContext().getSystemService(
				Context.POWER_SERVICE);
		WakeLock wakeLock = pm
				.newWakeLock(
						(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP),
						TAG);
		wakeLock.acquire();
		try
		{
			if (MainScreen.getCameraController().getCamera() == null)
			{
				Intent dialogIntent = new Intent(MainScreen.getInstance(), MainScreen.class);
				dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				MainScreen.getInstance().startActivity(dialogIntent);

			} else
			{
				takePicture();
			}
		} catch (NullPointerException e)
		{
		} finally {
			wakeLock.release();
		}
	}

	public void takePicture()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

		boolean photoTimeLapseActive = prefs.getBoolean(MainScreen.sPhotoTimeLapseActivePref, false);
		boolean photoTimeLapseIsRunning = prefs.getBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);

		if (!photoTimeLapseActive || !photoTimeLapseIsRunning)
		{
			return;
		}

		PluginManager.getInstance().onShutterClickNotUser();
	}

	public void setNextAlarm()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

		pauseBetweenShotsVal = prefs.getInt(MainScreen.sPhotoTimeLapseCaptureIntervalPref, -1);
		if (pauseBetweenShotsVal == -1)
		{
			return;
		}

		pauseBetweenShots = Long.parseLong(SelfTimerAndPhotoTimeLapse.stringTimelapseInterval[pauseBetweenShotsVal]);

		pauseBetweenShotsMeasurment = prefs.getInt(MainScreen.sPhotoTimeLapseCaptureIntervalMeasurmentPref, 0);

		switch (pauseBetweenShotsMeasurment)
		{
		case 0:
			pauseBetweenShots = pauseBetweenShots * 1000;
			break;
		case 1:
			pauseBetweenShots = pauseBetweenShots * 60000;
			break;
		case 2:
			pauseBetweenShots = pauseBetweenShots * 60000 * 60;
			break;
		default:
			break;
		}

		Editor e = prefs.edit();
		e.putInt(MainScreen.sPhotoTimeLapseCount, prefs.getInt(MainScreen.sPhotoTimeLapseCount, 0) + 1);
		e.commit();
		this.setAlarm(MainScreen.getInstance(), pauseBetweenShots);

		ComponentName receiver = new ComponentName(MainScreen.getInstance(), AlarmReceiver.class);
		PackageManager pm = MainScreen.getInstance().getPackageManager();

		pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);
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
	}
}
