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

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.RelativeLayout;

public class SamplePagerAdapter extends PagerAdapter
{

	List<RelativeLayout>	pages	= null;

	public SamplePagerAdapter(List<RelativeLayout> pages)
	{
		this.pages = pages;
	}

	@Override
	public Object instantiateItem(View collection, int position)
	{
		View v = pages.get(position);
		((ViewPager) collection).addView(v, 0);
		return v;
	}

	@Override
	public void destroyItem(View collection, int position, Object view)
	{
		((ViewPager) collection).removeView((View) view);
	}

	@Override
	public int getCount()
	{
		return pages.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object)
	{
		return view.equals(object);
	}
}