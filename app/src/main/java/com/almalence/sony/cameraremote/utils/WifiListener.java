package com.almalence.sony.cameraremote.utils;

import java.util.List;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;

public interface WifiListener {

	void onWifiConnecting(String ssid);
	void onWifiConnected(String ssid);
	void onWifiDisconnected();
	
	void onWifiStartScan();
	void onWifiScanFinished(List<ScanResult> sonyCameraScanResults, 
			List<WifiConfiguration> sonyCameraWifiConfiguration);

}
