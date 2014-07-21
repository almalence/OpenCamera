package com.almalence.plugins.export.standard.ExifDriver;

import java.util.Arrays;

import android.content.Context;

import com.almalence.plugins.export.standard.ExifDriver.Values.ExifValue;
import com.almalence.plugins.export.standard.ExifDriver.Values.UndefinedValueAccessException;
import com.almalence.plugins.export.standard.ExifDriver.Values.ValueByteArray;
import com.almalence.plugins.export.standard.ExifDriver.Values.ValueNumber;
import com.almalence.plugins.export.standard.ExifDriver.Values.ValueRationals;

public class ExifManager
{
	ExifDriver	driver;
	Context		context;

	public ExifManager(ExifDriver _driver, Context _context)
	{
		driver = _driver;
		context = _context;
	}

	/*
	 * Here go the user-space set/get methods
	 */
	/**
	 * Get photographer/editor copyright pair string. In case, that only editor
	 * is specified, photographer copyright will contain space character, so it
	 * is recommended to trim it, or use specialized getPhotographerCopyright()
	 * method
	 * 
	 * @return Array of Strings. Item 0 is photographer copyright, 1 holds the
	 *         editor copyright
	 */
	public String[] getCopyright()
	{
		byte[][] result = new byte[2][];
		result[0] = new byte[] { 0 };
		result[1] = new byte[] { 0 };
		ExifValue exifValue = driver.getIfd0().get(ExifDriver.TAG_COPYRIGHT);
		if (exifValue != null && exifValue.getDataType() == ExifDriver.FORMAT_UNDEFINED)
		{
			byte[] values;
			try
			{
				values = exifValue.getBytes();
			} catch (UndefinedValueAccessException e)
			{
				e.printStackTrace();
				return new String[] { "Error" };
			}
			int copyrightIndex = 0;
			result[0] = new byte[values.length];
			Arrays.fill(result[0], (byte) 0);
			result[1] = new byte[values.length];
			Arrays.fill(result[1], (byte) 0);
			int index = 0;
			for (int i = 0; i < values.length && copyrightIndex < 2; i++)
			{
				if (values[i] != 0)
				{
					result[copyrightIndex][index] = values[i];
					index++;
				} else
				{
					copyrightIndex++;
					index = 0;
				}
			}
		}
		return new String[] { new String(result[0]).trim(), new String(result[1]).trim() };
	}

	/**
	 * Photographer copyright in one single string
	 * 
	 * @return Photographer copyright
	 */
	public String getPhotographerCopyright()
	{
		return getCopyright()[0];
	}

	/**
	 * Editor copyright in one single string
	 * 
	 * @return Editor copyright
	 */
	public String getEditorCopyright()
	{
		return getCopyright()[1];
	}

	/**
	 * Get marker note - the marker specific (possibly binary) data. User should
	 * know, what the tag holds.
	 * 
	 * @return Marker note byte[] or null if Marker note could not be found
	 */
	public byte[] getMarkerNote()
	{
		ExifValue exifValue = driver.getIfdExif().get(ExifDriver.TAG_MARKER_NOTE);
		if (exifValue != null && exifValue.getDataType() == ExifDriver.FORMAT_UNDEFINED)
		{
			try
			{
				return exifValue.getBytes();
			} catch (UndefinedValueAccessException e)
			{
				e.printStackTrace();
				return new byte[0];
			}
		} else
		{
			return null;
		}
	}

	/**
	 * Get artist, which is actually an author of the image
	 * 
	 * @return Name of artist or null if the tag could not be found
	 */
	public String getArtist()
	{
		ExifValue exifValue = driver.getIfd0().get(ExifDriver.TAG_ARTIST);
		if (exifValue != null && exifValue.getDataType() == ExifDriver.FORMAT_ASCII_STRINGS)
		{
			try
			{
				return new String(exifValue.getBytes());
			} catch (UndefinedValueAccessException e)
			{
				e.printStackTrace();
				return "Error";
			}
		} else
		{
			return null;
		}
	}

	/**
	 * 
	 * @return
	 */
	public String getImageDescription()
	{
		ExifValue exifValue = driver.getIfd0().get(ExifDriver.TAG_IMAGE_DESCRIPTION);
		if (exifValue != null && exifValue.getDataType() == ExifDriver.FORMAT_ASCII_STRINGS)
		{
			try
			{
				return new String(exifValue.getBytes());
			} catch (UndefinedValueAccessException e)
			{
				e.printStackTrace();
				return "Error";
			}
		} else
		{
			return null;
		}
	}

	/**
	 * Get user comment it differs from image description - it can hold wide
	 * characters
	 * 
	 * @return
	 */
	public String getUserComment()
	{
		ExifValue exifValue = driver.getIfdExif().get(ExifDriver.TAG_USER_COMMENT);
		if (exifValue != null && exifValue.getDataType() == ExifDriver.FORMAT_UNDEFINED)
		{
			try
			{
				return new String(exifValue.getBytes());
			} catch (UndefinedValueAccessException e)
			{
				e.printStackTrace();
				return "Error";
			}
		} else
		{
			return null;
		}
	}

	/**
	 * Set the Marker note. the value can be whatever byte array
	 * 
	 * @param _value
	 *            byte array - binary or text information
	 */
	public void setMarkerNote(byte[] _value)
	{
		ValueByteArray baValue = new ValueByteArray(ExifDriver.FORMAT_UNDEFINED);
		baValue.setBytes(_value);
		driver.getIfdExif().put(ExifDriver.TAG_MARKER_NOTE, baValue);
	}

	/**
	 * Artist - or author - should be an ASCII string, but sometimes even
	 * unicode works fine
	 * 
	 * @param _artist
	 *            Name of the arist
	 */
	public void setArtist(String _artist)
	{
		ValueByteArray baValue = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
		baValue.setBytes(_artist.getBytes());
		driver.getIfd0().put(ExifDriver.TAG_ARTIST, baValue);
	}

	/**
	 * Copyright for the photographer and editor in common it is used internally
	 * by specialized methods for setting photographer and editor copyright
	 * separately
	 * 
	 * @param _author
	 *            Photographer copyright can be null
	 * @param _editor
	 *            Editor copyright can be null
	 */
	public void setCopyright(String _author, String _editor)
	{
		boolean editorPresented = false;
		String author = _author;
		if (author == null)
		{
			author = "";
		}
		author = author.trim();
		if (author.equals(""))
		{
			author = " ";
		}
		String editor = _editor;
		if (editor == null)
		{
			editor = "";
		}
		editor = editor.trim();
		editorPresented = !(editor.equals(""));
		byte[] authorBytes = author.getBytes();
		byte[] editorBytes = _editor.getBytes();
		int size = authorBytes.length + 1;
		if (editorPresented)
		{
			size += editorBytes.length + 1;
		}
		byte[] value = new byte[size];
		System.arraycopy(authorBytes, 0, value, 0, authorBytes.length);
		value[authorBytes.length] = 0;
		if (editorPresented)
		{
			System.arraycopy(editorBytes, 0, value, authorBytes.length + 1, editorBytes.length);
			value[value.length - 1] = 0;
		}
		ValueByteArray baValue = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
		baValue.setBytes(value);
		driver.getIfd0().put(ExifDriver.TAG_COPYRIGHT, baValue);
	}

	/**
	 * Copyright for the photographer (editor) ASCII string
	 * 
	 * @param _copyright
	 *            Copyright string
	 */
	public void setPhotographerCopyright(String _copyright)
	{
		setCopyright(_copyright, getEditorCopyright());
	}

	/**
	 * Copyright for the editor ASCII string
	 * 
	 * @param _copyright
	 *            Copyright string
	 */
	public void setEditorCopyright(String _copyright)
	{
		setCopyright(getPhotographerCopyright(), _copyright);
	}

	/**
	 * Some nice ASCII image description, like "Picnic in the summer A.D. 2012"
	 * 
	 * @param _desc
	 *            Description of the image
	 */
	public void setImageDescription(String _desc)
	{
		ValueByteArray baValue = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
		baValue.setBytes(_desc.getBytes());
		driver.getIfd0().put(ExifDriver.TAG_IMAGE_DESCRIPTION, baValue);
	}

	/**
	 * User comment - alternative to image description. Difference is, that this
	 * tag is expected to hold wide characters, so UTF-8 string is possible to
	 * store here
	 * 
	 * @param _comment
	 */
	public void setUserComment(String _comment)
	{
		ValueByteArray baValue = new ValueByteArray(ExifDriver.FORMAT_UNDEFINED);
		baValue.setBytes(_comment.getBytes());
		driver.getIfdExif().put(ExifDriver.TAG_USER_COMMENT, baValue);
	}

	private int[][] toDdMmSs(double _value)
	{
		double value = Math.abs(_value);
		int[][] ddmmss = new int[3][2];
		ddmmss[0][0] = (int) Math.floor(value);
		ddmmss[0][1] = 1;
		value -= Math.floor(value);
		value *= 60;
		ddmmss[1][0] = (int) Math.floor(value);
		ddmmss[1][1] = 1;
		value -= Math.floor(value);
		value *= 60000;
		ddmmss[2][0] = (int) Math.floor(value);
		ddmmss[2][1] = 1000;
		return ddmmss;
	}

	private void setGpsVersion()
	{
		ValueNumber version = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_BYTE);
		version.setIntegers(new int[] { 2, 2, 0, 0 });
		driver.getIfdExif().put(ExifDriver.TAG_GPS_VERSION_ID, version);
	}

	public void setGPSLocation(double _lat, double _lon, double _alt)
	{
		setGpsVersion();
		// Latitude
		ValueByteArray latRef = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
		ValueRationals lat = new ValueRationals(ExifDriver.FORMAT_UNSIGNED_RATIONAL);
		lat.setRationals(toDdMmSs(_lat));
		if (_lat > 0)
		{
			latRef.setBytes(new byte[] { 'N' });
		} else
		{
			latRef.setBytes(new byte[] { 'S' });
		}
		driver.getIfdGps().put(ExifDriver.TAG_GPS_LATITUDE, lat);
		driver.getIfdGps().put(ExifDriver.TAG_GPS_LATITUDE_REF, latRef);
		// Longitude
		ValueByteArray lonRef = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
		ValueRationals lon = new ValueRationals(ExifDriver.FORMAT_UNSIGNED_RATIONAL);
		lon.setRationals(toDdMmSs(_lon));
		if (_lon > 0)
		{
			lonRef.setBytes(new byte[] { 'E' });
		} else
		{
			lonRef.setBytes(new byte[] { 'W' });
		}
		driver.getIfdGps().put(ExifDriver.TAG_GPS_LONGITUDE, lon);
		driver.getIfdGps().put(ExifDriver.TAG_GPS_LONGITUDE_REF, lonRef);
		// Altitude
		ValueNumber altRef = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_BYTE);
		ValueRationals alt = new ValueRationals(ExifDriver.FORMAT_UNSIGNED_RATIONAL);
		int[][] altValue = new int[1][];
		altValue[0] = new int[] { (int) Math.abs(_alt), 1 };
		alt.setRationals(altValue);
		if (_alt >= 0)
		{
			altRef.setIntegers(new int[] { 0 });
		} else
		{
			altRef.setIntegers(new int[] { 1 });
		}
		driver.getIfdGps().put(ExifDriver.TAG_GPS_ALTITUDE, alt);
		driver.getIfdGps().put(ExifDriver.TAG_GPS_ALTITUDE_REF, altRef);
	}

	// Convert string like "123/456" or "1.23" to Rational (2 integers).
	public static int[][] stringToRational(String string)
	{
		int[][] res = null;
		String[] splited = string.split("/");
		if (splited.length == 2)
		{
			res = new int[1][2];
			res[0][0] = Integer.parseInt(splited[0]);
			res[0][1] = Integer.parseInt(splited[1]);
			return res;
		}

		splited = string.split("\\.");
		if (splited.length == 2)
		{
			res = new int[1][2];
			res[0][0] = Integer.parseInt(splited[0] + splited[1]);
			res[0][1] = 10;
			for (int i = 0; i < splited[1].length() - 1; i++)
			{
				res[0][1] *= 10;
			}
			return res;
		}

		return res;
	}
}
