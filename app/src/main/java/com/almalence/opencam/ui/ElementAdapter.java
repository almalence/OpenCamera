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
 package com.almalence.opencam_plus.ui;
 +++ --> */
// <!-- -+-
package com.almalence.opencam.ui;

//-+- -->

import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/***
 * Adapter for grids elements
 ***/

public class ElementAdapter extends BaseAdapter
{

	public List<View>	Elements;

	public ElementAdapter()
	{
	}

	public int getCount()
	{
		return Elements.size();
	}

	public Object getItem(int position)
	{
		return null;
	}

	public long getItemId(int position)
	{
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent)
	{
		if (null == Elements)
			return null;
		return Elements.get(position);
	}
}