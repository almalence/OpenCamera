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

import android.graphics.drawable.Drawable;

public class Adjustment
{
	private final String	title;
	private int				value;
	private final int		minimumValue;
	private final int		maximumValue;
	private final int		initialValue;
	private final Drawable	icon;
	private final int		code;

	public Adjustment(int code, String title, int initialValue, int minimumValue, int maximumValue, Drawable icon)
	{
		this.code = code;
		this.title = title;
		this.minimumValue = minimumValue;
		this.maximumValue = maximumValue;
		this.initialValue = initialValue;
		this.value = initialValue;
		this.icon = icon;
	}

	public final int getCode()
	{
		return this.code;
	}

	public final String getTitle()
	{
		return this.title;
	}

	public final int getValue()
	{
		return this.value;
	}

	public final int getMinimum()
	{
		return this.minimumValue;
	}

	public final int getMaximum()
	{
		return this.maximumValue;
	}

	protected final int getInitialValue()
	{
		return this.initialValue;
	}

	public final Drawable getIcon()
	{
		return this.icon;
	}

	public final int getProgressMax()
	{
		return Math.abs(this.maximumValue - this.minimumValue);
	}

	public final int getProgress()
	{
		return (int) (Math.signum(this.maximumValue - this.minimumValue) * (this.value - this.minimumValue));
	}

	public void reset()
	{
		this.value = this.initialValue;
	}

	public void onProgressChanged(int progress)
	{
		this.value = (int) (progress * Math.signum(this.maximumValue - this.minimumValue) + this.minimumValue);
	}

	public void setValue(int value)
	{
		this.value = value;
	}
}
