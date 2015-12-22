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

package com.almalence.plugins.processing.hdr;

import java.util.ArrayList;

import android.graphics.Bitmap;

public final class AdjustmentsPreset
{
	private final ArrayList<Integer[]>	sets	= new ArrayList<Integer[]>();
	private final String				title;
	private Bitmap						thumbnail;

	public AdjustmentsPreset(String title, Bitmap thumbnail, Integer... sets)
	{
		if (sets.length % 2 != 0)
		{
			throw new IllegalArgumentException("Invalid adjustments sets");
		}

		this.title = title;

		for (int i = 0; i < sets.length / 2; i++)
		{
			int id = sets[i * 2];
			int value = sets[i * 2 + 1];

			this.sets.add(new Integer[] { id, value });
		}

		this.thumbnail = thumbnail;
	}

	public final int getSetsCount()
	{
		return this.sets.size();
	}

	public final int getSetId(int position)
	{
		return this.sets.get(position)[0];
	}

	public final int getSetValue(int position)
	{
		return this.sets.get(position)[1];
	}

	public final void saveSets(ArrayList<Adjustment> sets)
	{
		for (int i = 0; i < this.sets.size(); i++)
		{
			for (int j = 0; j < sets.size(); j++)
			{
				if (sets.get(j).getCode() == this.sets.get(i)[0])
				{
					this.sets.get(i)[1] = sets.get(j).getValue();
					break;
				}
			}
		}
	}

	public final Bitmap getThumbnail()
	{
		return this.thumbnail;
	}

	public final void setThumbnail(Bitmap thumb)
	{
		this.thumbnail = thumb;
	}

	@Override
	public String toString()
	{
		return this.title;
	}
}
