/*
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is collection of files collectively known as Open Camera.

The Initial Developer of the Original Code is Almalence Inc.
Portions created by Initial Developer are Copyright (C) 2013 
by Almalence Inc. All Rights Reserved.
 */

package com.almalence.plugins.export.standard;

public class GPSTagsConverter
{
	private static StringBuilder	sb	= new StringBuilder(20);

	/**
	 * returns ref for latitude which is S or N.
	 * 
	 * @param latitude
	 * @return S or N
	 */
	public static String latitudeRef(double latitude)
	{
		return latitude < 0.0d ? "S" : "N";
	}

	/**
	 * returns ref for latitude which is S or N.
	 * 
	 * @param latitude
	 * @return S or N
	 */
	public static String longitudeRef(double longitude)
	{
		return longitude < 0.0d ? "W" : "E";
	}

	/**
	 * convert latitude into DMS (degree minute second) format. For instance<br/>
	 * -79.948862 becomes<br/>
	 * 79/1,56/1,55903/1000<br/>
	 * It works for latitude and longitude<br/>
	 * 
	 * @param latitude
	 *            could be longitude.
	 * @return
	 */
	public static final synchronized String convert(double latitude)
	{
		latitude = Math.abs(latitude);
		int degree = (int) latitude;
		latitude *= 60;
		latitude -= (degree * 60.0d);
		int minute = (int) latitude;
		latitude *= 60;
		latitude -= (minute * 60.0d);
		int second = (int) (latitude * 1000.0d);

		sb.setLength(0);
		sb.append(degree);
		sb.append("/1,");
		sb.append(minute);
		sb.append("/1,");
		sb.append(second);
		sb.append("/1000,");
		return sb.toString();
	}
}