package com.almalence.opencam;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.preference.PreferenceManager;

import com.almalence.opencam.ui.SelfTimerAndPhotoTimeLapse;

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
		try
		{
			if (MainScreen.getCameraController().getCamera() == null)
			{
				Intent dialogIntent = new Intent(MainScreen.getInstance(), MainScreen.class);
				dialogIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				MainScreen.getInstance().startActivity(dialogIntent);
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
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

		boolean photoTimeLapseActive = prefs.getBoolean(MainScreen.sPhotoTimeLapseActivePref, false);
		boolean photoTimeLapseIsRunning = prefs.getBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);

		if (!photoTimeLapseActive || !photoTimeLapseIsRunning)
		{
			return;
		}

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

		PluginManager.getInstance().onShutterClickNotUser();
	}

	public void setNextAlarm() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		Editor e = prefs.edit();
		e.putInt(MainScreen.sPhotoTimeLapseCount, prefs.getInt(MainScreen.sPhotoTimeLapseCount, 0) + 1);
		e.commit();
		this.setAlarm(MainScreen.getInstance(), pauseBetweenShots);
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
