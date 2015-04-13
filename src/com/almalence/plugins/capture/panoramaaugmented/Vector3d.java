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

package com.almalence.plugins.capture.panoramaaugmented;

public class Vector3d
{
	public float	x;
	public float	y;
	public float	z;

	public Vector3d()
	{
		this(0.0f, 0.0f, 0.0f);
	}

	public Vector3d(final float x, final float y, final float z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3d(final Vector3d original)
	{
		this.x = original.x;
		this.y = original.y;
		this.z = original.z;
	}

	public void set(final Vector3d vector)
	{
		this.x = vector.x;
		this.y = vector.y;
		this.z = vector.z;
	}

	public void multiply(final float multi)
	{
		this.x *= multi;
		this.y *= multi;
		this.z *= multi;
	}

	public void normalize()
	{
		final float length = this.length();

		if (length == 0.0f)
		{
			throw new ArithmeticException("Can't normalize a zero-length vector");
		}

		this.x /= length;
		this.y /= length;
		this.z /= length;
	}

	public float length()
	{
		return (float) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
	}

	@Override
	public String toString()
	{
		return ("(" + this.x + ", " + this.y + ", " + this.z + ")");
	}
}
