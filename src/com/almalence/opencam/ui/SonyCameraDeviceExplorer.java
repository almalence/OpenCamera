package com.almalence.opencam.ui;

import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.sony.cameraremote.DeviceListAdapter;
import com.almalence.sony.cameraremote.ServerDevice;
import com.almalence.sony.cameraremote.SimpleSsdpClient;
import com.almalence.ui.RotateDialog;

public class SonyCameraDeviceExplorer
{
	private SimpleSsdpClient				mSsdpClient;
	private DeviceListAdapter				mListAdapter;
	private SonyCameraDeviceExplorerDialog	dialog	= null;

	public SonyCameraDeviceExplorer(View gui)
	{
		dialog = new SonyCameraDeviceExplorerDialog(ApplicationScreen.instance);
		mSsdpClient = new SimpleSsdpClient();
		mListAdapter = new DeviceListAdapter(ApplicationScreen.instance);
	}

	public void showExplorer()
	{
		dialog.show();

		mListAdapter.clearDevices();
		
		ListView listView = (ListView) dialog.findViewById(R.id.list_device);
		listView.setAdapter(mListAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				ListView listView = (ListView) parent;
				ServerDevice device = (ServerDevice) listView.getAdapter().getItem(position);
				launchRemoteCamera(device);
			}
		});

		dialog.findViewById(R.id.button_search).setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Button btn = (Button) v;
				if (!mSsdpClient.isSearching())
				{
					searchDevices();
					btn.setEnabled(false);
				}
			}
		});

		// Show Wi-Fi SSID.
		TextView textWifiSsid = (TextView) dialog.findViewById(R.id.text_wifi_ssid);
		WifiManager wifiManager = (WifiManager) MainScreen.getInstance().getSystemService(MainScreen.WIFI_SERVICE);
		if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED)
		{
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			String htmlLabel = String.format("SSID: <b>%s</b>", wifiInfo.getSSID());
			textWifiSsid.setText(Html.fromHtml(htmlLabel));
		} else
		{
			textWifiSsid.setText(R.string.msg_wifi_disconnect);
		}
	}

	public void launchRemoteCamera(ServerDevice device)
	{
		ApplicationScreen.instance.pauseMain();
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		prefs.edit().putInt(ApplicationScreen.sCameraModePref, CameraController.getNumberOfCameras() - 1).commit();
		ApplicationScreen.getGUIManager().setCameraModeGUI(CameraController.getNumberOfCameras() - 1);
		CameraController.setCameraIndex(CameraController.getNumberOfCameras() - 1);
		ApplicationScreen.instance.switchingMode(false);
		
		CameraController.setTargetServerDevice(device);
		ApplicationScreen.instance.resumeMain();
		
		hideExplorer();
	}

	public void hideExplorer()
	{
		if (mSsdpClient != null && mSsdpClient.isSearching())
		{
			mSsdpClient.cancelSearching();
		}
		
		dialog.dismiss();
	}

	/**
	 * Start searching supported devices.
	 */
	private void searchDevices()
	{
		mListAdapter.clearDevices();
		// setProgressBarIndeterminateVisibility(true);
		mSsdpClient.search(new SimpleSsdpClient.SearchResultHandler()
		{

			@Override
			public void onDeviceFound(final ServerDevice device)
			{
				// Called by non-UI thread.
				ApplicationScreen.instance.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						mListAdapter.addDevice(device);
						mListAdapter.notifyDataSetChanged();
						if (dialog != null) {
							dialog.setRotate(AlmalenceGUI.mDeviceOrientation);
						}
					}
				});
			}

			@Override
			public void onFinished()
			{
				// Called by non-UI thread.
				ApplicationScreen.instance.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						dialog.findViewById(R.id.button_search).setEnabled(true);
					}
				});
			}

			@Override
			public void onErrorFinished()
			{
				// Called by non-UI thread.
				ApplicationScreen.instance.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						dialog.findViewById(R.id.button_search).setEnabled(true);
					}
				});
			}
		});
	}
	
	public void setOrientation()
	{
		if (dialog != null) {
			dialog.setRotate(AlmalenceGUI.mDeviceOrientation);
		}
	}
}
