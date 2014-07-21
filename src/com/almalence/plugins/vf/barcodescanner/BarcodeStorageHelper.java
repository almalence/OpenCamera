package com.almalence.plugins.vf.barcodescanner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.MainScreen;

//-+- -->

public class BarcodeStorageHelper
{

	private static final String			FILENAME	= "scanned_barcodes.txt";
	private static ArrayList<Barcode>	barcodesList;

	public static void addBarcode(Barcode barcode)
	{
		int position = searchForBarcode(barcode);

		if (position >= 0)
		{
			String oldFile = barcodesList.get(position).getFile();
			if (oldFile != null)
			{
				File file = new File(oldFile);
				if (file.exists())
				{
					file.delete();
				}
			}
			barcodesList.remove(position);
		}

		barcodesList.add(barcode);
		saveBarcodesToFile();
	}

	// Search for barcode.
	// Return pos, or -1 if not found.
	private static int searchForBarcode(Barcode barcode)
	{
		if (barcodesList == null)
		{
			readBarcodesFromFile();
		}

		int res = -1;

		String data = barcode.getData();
		for (int i = 0; i < barcodesList.size(); i++)
		{
			String s = barcodesList.get(i).getData();
			if (s.equals(data))
			{
				res = i;
				return res;
			}
		}

		return res;
	}

	private static void saveBarcodesToFile()
	{
		JSONArray jsonArray = new JSONArray();
		for (int i = 0; i < barcodesList.size(); i++)
		{
			jsonArray.put(barcodesList.get(i).getJSONObject());
		}
		writeToFile(jsonArray.toString());
	}

	private static void readBarcodesFromFile()
	{
		String json = readFromFile();
		try
		{
			barcodesList = new ArrayList<Barcode>();
			JSONArray array = new JSONArray(json);
			for (int i = 0; i < array.length(); i++)
			{
				JSONObject item = array.getJSONObject(i);
				Barcode barcode = new Barcode(item.getString("data"), item.getString("format"), item.getString("type"),
						new Date(item.getLong("date")), item.getString("file"));
				barcodesList.add(barcode);
			}
		} catch (JSONException e)
		{
			e.printStackTrace();
		}
		if (barcodesList == null)
		{
			barcodesList = new ArrayList<Barcode>();
		}
	}

	public static void removeBarcode(Barcode barcode)
	{
		int position = searchForBarcode(barcode);

		if (position >= 0)
		{
			barcodesList.remove(position);
			if (barcode.getFile() != null)
			{
				File file = new File(barcode.getFile());
				if (file.exists())
				{
					file.delete();
				}
			}
			saveBarcodesToFile();
		}
	}

	public static void removeAll()
	{
		for (Barcode barcode : barcodesList)
		{
			if (barcode.getFile() != null)
			{
				File file = new File(barcode.getFile());
				if (file.exists())
				{
					file.delete();
				}
			}
		}

		barcodesList.clear();
		saveBarcodesToFile();
	}

	public static ArrayList<Barcode> getBarcodesList()
	{
		if (barcodesList == null)
		{
			readBarcodesFromFile();
		}

		return barcodesList;
	}

	private static void writeToFile(String data)
	{
		try
		{
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(MainScreen.getMainContext().openFileOutput(
					FILENAME, Context.MODE_PRIVATE)));
			bw.write(data);
			bw.close();
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private static String readFromFile()
	{
		String readString = "";

		FileInputStream fis = null;
		try
		{
			fis = MainScreen.getMainContext().openFileInput(FILENAME);
			InputStreamReader isr = new InputStreamReader(fis);
			StringBuilder sb = new StringBuilder();
			char[] inputBuffer = new char[2048];
			int l;
			while ((l = isr.read(inputBuffer)) != -1)
			{
				sb.append(inputBuffer, 0, l);
			}
			readString = sb.toString();

			fis.close();
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		} finally
		{
			if (fis != null)
			{
				fis = null;
			}
		}

		return readString;
	}
}
