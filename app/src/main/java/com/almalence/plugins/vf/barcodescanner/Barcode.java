package com.almalence.plugins.vf.barcodescanner;

import java.io.Serializable;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.zxing.Result;
import com.google.zxing.client.result.ResultParser;

public class Barcode implements Serializable
{
	private String	mData;
	private String	mFormat;
	private String	mType;
	private Date	mDate;
	private String	mFile;

	public Barcode(String data, String format, String type, Date date, String file)
	{
		this.mData = data;
		this.mFormat = format;
		this.mType = type;
		this.mDate = date;
		this.mFile = file;
	}

	public Barcode(Result barcode)
	{
		this.mData = barcode.toString();
		this.mFormat = barcode.getBarcodeFormat().toString();
		this.mType = ResultParser.parseResult(barcode).getType().toString();
		this.mDate = new Date(barcode.getTimestamp());
		this.mFile = "";
	}

	public Barcode(Result barcode, String file)
	{
		this.mData = barcode.toString();
		this.mFormat = barcode.getBarcodeFormat().toString();
		this.mType = ResultParser.parseResult(barcode).getType().toString();
		this.mDate = new Date(barcode.getTimestamp());
		this.mFile = file;
	}

	public JSONObject getJSONObject()
	{
		JSONObject jsonObject = new JSONObject();
		try
		{
			jsonObject.put("data", mData);
			jsonObject.put("format", mFormat);
			jsonObject.put("type", mType);
			jsonObject.put("date", mDate.getTime());
			jsonObject.put("file", mFile);
		} catch (JSONException e)
		{
			e.printStackTrace();
		}
		return jsonObject;
	}

	public String getData()
	{
		return mData;
	}

	public void setData(String data)
	{
		this.mData = data;
	}

	public String getFormat()
	{
		return mFormat;
	}

	public void setFormat(String format)
	{
		this.mFormat = format;
	}

	public String getType()
	{
		return mType;
	}

	public void setType(String type)
	{
		this.mType = type;
	}

	public Date getDate()
	{
		return mDate;
	}

	public void setDate(Date date)
	{
		this.mDate = date;
	}

	public String getFile()
	{
		return mFile;
	}

	public void setFile(String mFile)
	{
		this.mFile = mFile;
	}
}
