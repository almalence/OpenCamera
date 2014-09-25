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

/* <!-- +++
 package com.almalence.opencam_plus;
 +++ --> */
// <!-- -+-
package com.almalence.opencam;

//-+- -->

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Xml;

/***
 * Configuration parser.
 * 
 * Parse main configuration - mode.xml, located in assets directory. mode.xml
 * contains information about default plugin for first run and modes.
 * 
 * Each mode is a set of plugins of different type. Plugin order in set is not
 * important.
 * 
 * All tags are processed in readTag function.
 * 
 * config example <mode id="test" name="Test"> <icon id="icon"/> <vf
 * id="com.opencam.testvf"/>         <capture id="com.opencam.testcapture" /> 
 *        <processing id="com.opencam.testprocessing"/>  <filter
 * id="com.opencam.testfilter"/>       <export id="com.opencam.testexport"/>
 * <sku id="com_opencam_modename"/> <howtotext id="text howto use mode"/>
 * </mode> 
 * 
 * each tag has to be in the same format: <tag_name id="value"/>
 * 
 * in case of other tag format will be necessary to add processing function in
 * readTag for new tag
 ***/

public class ConfigParser
{

	private static final String	ns				= null;
	private static List<Mode>	modes;

	private static ConfigParser	configParser;

	private static String		defaultModeID	= "";

	public static ConfigParser getInstance()
	{
		if (configParser == null)
		{
			configParser = new ConfigParser();
		}
		return configParser;
	}

	private ConfigParser()
	{
		modes = new ArrayList<Mode>();
	}

	public Mode getMode(String mode)
	{
		Iterator<Mode> iterator = modes.iterator();
		while (iterator.hasNext())
		{
			Mode tmp = iterator.next();
			if (mode.equals(tmp.modeID))
				return tmp;
		}
		return null;
	}

	public List<Mode> getList()
	{
		return modes;
	}

	public Mode getDefaultMode()
	{
		if (defaultModeID.isEmpty())
			defaultModeID = modes.get(0).modeID;
		return getMode(defaultModeID);
	}

	public boolean parse(Context context) throws XmlPullParserException, IOException
	{
		AssetManager assetManager = context.getAssets();
		InputStream in = null;
		try
		{
			in = assetManager.open("opencamera_modes.xml");
		} catch (IOException e)
		{
			return false;
		}

		try
		{
			if (modes != null && modes.size() > 0)
				modes.clear();
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(in, null);
			parser.nextTag();
			return readConfig(parser);
		} finally
		{
			in.close();
		}
	}

	private boolean readConfig(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		parser.require(XmlPullParser.START_TAG, ns, "config");
		while (parser.next() != XmlPullParser.END_TAG)
		{
			if (parser.getEventType() != XmlPullParser.START_TAG)
			{
				continue;
			}
			String name = parser.getName();
			if (name.equals("mode"))
			{
				Mode tmp = readMode(parser);
				modes.add(tmp);
			} else if (name.equals("defaultmode"))
			{
				defaultModeID = readDefaultMode(parser);
			} else
			{
				skip(parser);
			}
		}
		return true;
	}

	private String readDefaultMode(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		parser.require(XmlPullParser.START_TAG, ns, "defaultmode");
		String modeID = parser.getAttributeValue(null, "id");
		parser.nextTag();
		return modeID;
	}

	private Mode readMode(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		parser.require(XmlPullParser.START_TAG, ns, "mode");

		Mode mode = new Mode();
		mode.modeID = parser.getAttributeValue(null, "id");
		mode.modeName = parser.getAttributeValue(null, "name");
		mode.modeSaveName = parser.getAttributeValue(null, "savename");
		
		if ((mode.modeNameHAL = parser.getAttributeValue(null, "nameHAL")) == null)
			mode.modeNameHAL = mode.modeName;
		if ((mode.modeSaveNameHAL = parser.getAttributeValue(null, "savenameHAL")) == null)
			mode.modeSaveNameHAL = mode.modeSaveName;

		while (parser.next() != XmlPullParser.END_TAG)
		{
			if (parser.getEventType() != XmlPullParser.START_TAG)
			{
				continue;
			}
			readTag(parser, mode);
		}
		return mode;
	}

	private void readTag(XmlPullParser parser, Mode mode) throws IOException, XmlPullParserException
	{
		String tag = parser.getName();
		parser.require(XmlPullParser.START_TAG, ns, tag);
		String id = parser.getAttributeValue(null, "id");
		if (tag.equals("icon") || tag.equals("iconHAL"))
		{
			if (tag.equals("iconHAL"))
			{
				if (id != null)
					mode.iconHAL = id;
				else
					mode.iconHAL = mode.icon;
			}
			else
			{
				if (id != null)
				{
					mode.icon = id;
					mode.iconHAL = id;
				}
			}
		}
		else if (tag.equals("vf"))
		{
			if (id != null)
				mode.VF.add(id);
		} else if (tag.equals("capture"))
		{
			if (id != null)
				mode.Capture = id;
		} else if (tag.equals("processing"))
		{
			if (id != null)
				mode.Processing = id;
		} else if (tag.equals("filter"))
		{
			if (id != null)
				mode.Filter.add(id);
		} else if (tag.equals("export"))
		{
			if (id != null)
				mode.Export = id;
		} else if (tag.equals("sku"))
		{
			if (id != null)
				mode.SKU = id;
		} else if (tag.equals("howtotext"))
		{
			if (id != null)
				mode.howtoText = id;
		}
		/*
		 * add additional tag processing here
		 */
		else
		{
			skip(parser);
			return;
		}
		parser.nextTag();
		return;
	}

	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		if (parser.getEventType() != XmlPullParser.START_TAG)
		{
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0)
		{
			switch (parser.next())
			{
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			default:
				break;
			}
		}
	}
}