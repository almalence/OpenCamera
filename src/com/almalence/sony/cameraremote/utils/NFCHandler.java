package com.almalence.sony.cameraremote.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcF;
import android.os.Parcelable;
import android.util.Pair;

@TargetApi(10)
public class NFCHandler {

	private static String SONY_MIME_TYPE = "application/x-sony-pmm";

	public static String[][] getTechListArray() {
		return new String[][] { new String[] { NfcF.class.getName() } };
	}
	
	public static IntentFilter[] getIntentFilterArray() {
		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		try {
			ndef.addDataType(SONY_MIME_TYPE);
		}
		catch (MalformedMimeTypeException e) {
			throw new RuntimeException("fail", e);
		}
		return new IntentFilter[] {ndef};
	}
	
	public static PendingIntent getPendingIntent(Activity activity) {
		return PendingIntent.getActivity(activity, 0, 
				new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
	}
	
	public static Pair<String, String> parseIntent(Intent intent) throws Exception {

		Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

		if(tagFromIntent != null && messages != null) {
			return getCameraWifiSettingsFromTag(tagFromIntent, messages);
		}
		
		return null;
	}
	
	public static Pair<String, String> getCameraWifiSettingsFromTag(Tag tag, Parcelable[] messages) 
			throws Exception{

		NdefRecord record = ((NdefMessage) messages[0]).getRecords()[0];
		Pair<String, String> cameraWifiSettings = decodeSonyPPMMessage(record);

		return cameraWifiSettings;
	}
	
	private static Pair<String, String> decodeSonyPPMMessage(NdefRecord ndefRecord) {

		if(!SONY_MIME_TYPE.equals(new String(ndefRecord.getType()))) {
			return null;
		}

		try { 
			byte[] payload = ndefRecord.getPayload(); 

			int ssidBytesStart = 8;
			int ssidLength = payload[ssidBytesStart];

			byte[] ssidBytes = new byte[ssidLength];
			int ssidPointer = 0;
			for (int i=ssidBytesStart+1; i<=ssidBytesStart+ssidLength; i++) {
				ssidBytes[ssidPointer++] = payload[i];
			}
			String ssid = new String(ssidBytes);

			int passwordBytesStart = ssidBytesStart+ssidLength+4;
			int passwordLength = payload[passwordBytesStart];

			byte[] passwordBytes = new byte[passwordLength];
			int passwordPointer = 0;
			for (int i=passwordBytesStart+1; i<=passwordBytesStart+passwordLength; i++) {
				passwordBytes[passwordPointer++] = payload[i];
			}
			String password = new String(passwordBytes);
			
			return new Pair<String, String>(ssid, password);

		} catch(Exception e) {
			return null;
		}
	}
	
}
