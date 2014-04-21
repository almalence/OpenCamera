package com.almalence.plugins.vf.barcodescanner;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Point;

import com.almalence.opencam.MainScreen;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class BarcodeStorageHelper {
	
	private final static String FILENAME = "scanned_barcodes.txt";
	private static ArrayList<Barcode> barcodesList;
	
	public static void addBarcode(Barcode barcode) {
		if (searchForBarcode(barcode) >= 0) {
			return;
		}
		barcodesList.add(barcode);
		saveBarcodesToFile();
	}
	
	private static int searchForBarcode(Barcode barcode) {
		if (barcodesList == null) {
			readBarcodesFromFile();
		}
		
		int res = -1;

		String data = barcode.getData();
		for (int i = 0; i < barcodesList.size(); i++) {
			String s = barcodesList.get(i).getData();
			if (s.equals(data)) {
				res = i;
				return res;
			}
		}
		
		return res;
	}
	
	private static void saveBarcodesToFile() {
		Gson gson = new Gson();
		String json = gson.toJson(barcodesList);
		writeToFile(json);
	}
	
	private static void readBarcodesFromFile() {
		Gson gson = new Gson();
		String json = readFromFile();
		barcodesList = gson.fromJson(json, new TypeToken<ArrayList<Barcode>>(){}.getType());
		if (barcodesList == null) {
			barcodesList = new ArrayList<Barcode>();
		}
	}
	
	public static void removeBarcode(Barcode barcode) {
		int position = searchForBarcode(barcode);
		
		if (position >= 0) {
			barcodesList.remove(position);
			saveBarcodesToFile();
		}
	}
	
	public static ArrayList<Barcode> getBarcodesList() {
		if (barcodesList == null) {
			readBarcodesFromFile();
		}
		
		return barcodesList;
	}
	
	public static void getBarcodeInfo() {
		
	}
	
	private static void writeToFile(String data) {
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(MainScreen.mainContext.openFileOutput(FILENAME, Context.MODE_PRIVATE)));
			bw.write(data);
			bw.close();
	    } catch (FileNotFoundException e) {
	      e.printStackTrace();
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	}
	
	private static String readFromFile() {
		String readString = "";
		
		FileInputStream fis = null;
	    try {
	        fis = MainScreen.mainContext.openFileInput(FILENAME);
	        InputStreamReader isr = new InputStreamReader(fis);
	        StringBuilder sb = new StringBuilder();
	        char[] inputBuffer = new char[2048];
	        int l;
	        while ((l = isr.read(inputBuffer)) != -1) {
	            sb.append(inputBuffer, 0, l);
	        }
	        readString = sb.toString();
	        
	        fis.close();
	    } catch (FileNotFoundException e) {
	      e.printStackTrace();
	    } catch (IOException e) {
	      e.printStackTrace();
	    } finally {
	        if (fis != null) {
	            fis = null;
	        }
	    }
	    
	    return readString;
	}

}
