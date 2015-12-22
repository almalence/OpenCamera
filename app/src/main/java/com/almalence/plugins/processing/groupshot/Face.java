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

package com.almalence.plugins.processing.groupshot;

import android.graphics.PointF;

/**
 * A Face contains all the information identifying the location of a face in a
 * bitmap.
 */
public final class Face
{
	/**
	 * Returns a confidence factor between 0 and 1. This indicates how certain
	 * what has been found is actually a face. A confidence factor above 0.3 is
	 * usually good enough.
	 */
	public float confidence()
	{
		return mConfidence;
	}

	/**
	 * Sets the position of the mid-point between the eyes.
	 * 
	 * @param point
	 *            the PointF coordinates (float values) of the face's mid-point
	 */
	public void getMidPoint(PointF point)
	{
		// don't return a PointF to avoid allocations
		point.set(mMidPointX, mMidPointY);
	}

	/**
	 * Returns the distance between the eyes.
	 */
	public float eyesDistance()
	{
		return mEyesDist;
	}

	public Face()
	{
	}

	public float	mConfidence;
	public float	mMidPointX;
	public float	mMidPointY;
	public float	mEyesDist;
}
