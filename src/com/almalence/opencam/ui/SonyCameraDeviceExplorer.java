/* <!-- +++
package com.almalence.opencam_plus.ui;
+++ --> */
//<!-- -+-
package com.almalence.opencam.ui;

//-+- -->

import java.util.List;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.almalence.sony.cameraremote.DeviceListAdapter;
import com.almalence.sony.cameraremote.ServerDevice;
import com.almalence.sony.cameraremote.SimpleSsdpClient;
import com.almalence.sony.cameraremote.utils.WifiListener;
/* <!-- +++
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.MainScreen;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

public class SonyCameraDeviceExplorer implements WifiListener
{
	private SimpleSsdpClient				mSsdpClient;
	private DeviceListAdapter				mListAdapter;
	private SonyCameraDeviceExplorerDialog	dialog				= null;
	private boolean							isSearchingDevice	= false;

	public SonyCameraDeviceExplorer(View gui)
	{
		dialog = new SonyCameraDeviceExplorerDialog(ApplicationScreen.instance);
		mSsdpClient = new SimpleSsdpClient();
		mListAdapter = new DeviceListAdapter(ApplicationScreen.instance);
	}

	public void showExplorer()
	{
		isSearchingDevice = true;

		if (MainScreen.getInstance().getWifiHandler() != null)
		{
			MainScreen.getInstance().getWifiHandler().addListener(this);
		}

		dialog.show();
		dialog.setOnDismissListener(new OnDismissListener()
		{
			@Override
			public void onDismiss(DialogInterface dialog)
			{
				isSearchingDevice = false;
			}
		});

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
		WifiManager wifiManager = (WifiManager) ApplicationScreen.instance
				.getSystemService(ApplicationScreen.WIFI_SERVICE);
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
		hideExplorer();
		
		ApplicationScreen.instance.pauseMain();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		prefs.edit().putInt(ApplicationScreen.sCameraModePref, CameraController.getNumberOfCameras() - 1).commit();
		ApplicationScreen.getGUIManager().setCameraModeGUI(CameraController.getNumberOfCameras() - 1);
		CameraController.setCameraIndex(CameraController.getNumberOfCameras() - 1);
		ApplicationScreen.instance.switchingMode(false);

		CameraController.setTargetServerDevice(device);
		ApplicationScreen.instance.resumeMain();
	}

	public void hideExplorer()
	{
		if (progress != null)
		{
			progress.dismiss();
		}
		
		isSearchingDevice = false;

		if (MainScreen.getInstance().getWifiHandler() != null)
		{
			MainScreen.getInstance().getWifiHandler().removeListener(this);
		}

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
						if (dialog != null)
						{
							dialog.setRotate(GUI.mDeviceOrientation);
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
		if (dialog != null)
		{
			dialog.setRotate(GUI.mDeviceOrientation);
		}
	}

	public static ProgressDialog							progress;
	
	SimpleSsdpClient.SearchResultHandler loopSearchHandler = new SimpleSsdpClient.SearchResultHandler()
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
					if (isSearchingDevice)
						launchRemoteCamera(device);
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
					if (progress != null)
					{
						progress.dismiss();
					}
				}
			});
		}

		@Override
		public void onErrorFinished()
		{
			try
			{
				Thread.sleep(1000);
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (isSearchingDevice) {
				searchForCameraAndOpenLoop();
			}
		}
	};
	
	private void searchForCameraAndOpenLoop()
	{
		mSsdpClient.search(loopSearchHandler);

	}

	@Override
	public void onWifiConnected(String ssid)
	{
		if (isSearchingDevice) {
			ApplicationScreen.instance.runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (progress != null) {
						progress.dismiss();
					}
					progress = ProgressDialog.show(ApplicationScreen.instance, ApplicationScreen.instance.getResources().getString(R.string.title_connecting),
							ApplicationScreen.instance.getResources().getString(R.string.msg_connecting), true, true);
					progress.setOnDismissListener(new OnDismissListener()
					{
						@Override
						public void onDismiss(DialogInterface dialog)
						{
							isSearchingDevice = false;
						}
					});
				}
			});
			searchForCameraAndOpenLoop();
		}
	}

	@Override
	public void onWifiDisconnected()
	{
	}

	@Override
	public void onWifiStartScan()
	{
	}

	@Override
	public void onWifiScanFinished(List<ScanResult> sonyCameraScanResults,
			List<WifiConfiguration> sonyCameraWifiConfiguration)
	{
	}

	@Override
	public void onWifiConnecting(String ssid)
	{
		ApplicationScreen.instance.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				if (progress != null) {
					progress.dismiss();
				}
				progress = ProgressDialog.show(ApplicationScreen.instance, ApplicationScreen.instance.getResources().getString(R.string.title_connecting),
						ApplicationScreen.instance.getResources().getString(R.string.msg_connecting), true, true);
				progress.setOnDismissListener(new OnDismissListener()
				{
					@Override
					public void onDismiss(DialogInterface dialog)
					{
						isSearchingDevice = false;
					}
				});
			}
		});
	}
}
