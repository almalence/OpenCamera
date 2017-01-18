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

#include <string.h>
#include <stdlib.h>

void TransformPlane8bit
(
	unsigned char * In,
	unsigned char * Out,
	int sx,
	int sy,
	int flipLeftRight,
	int flipUpDown,
	int rotate90
)
{
	int y;
	int osx, osy;

	// no transform case
	if ((!flipLeftRight) && (!flipUpDown) && (!rotate90))
	{
		if (In!=Out)
			memcpy (Out, In, sx*sy*sizeof(unsigned char));
		return;
	}

	// can't rotate in-place
	if (rotate90 && (In == Out))
		return;

	if (rotate90) {osx = sy; osy = sx;}
		else {osx = sx; osy = sy;}

	// processing 4 mirrored locations at once
	// +1 here to cover case when image dimensions are odd
	#pragma omp parallel for schedule(guided)
	for (y=0; y<(sy+1)/2; ++y)
	{
		int x;
		int ox, oy;
		unsigned char t1, t2, t3, t4;

		for (x=0; x<(sx+1)/2; ++x)
		{
			if (rotate90)
			{
				if (flipLeftRight) ox = y;
					else ox = osx-1-y;
				if (flipUpDown) oy = osy-1-x;
					else oy = x;

				t1 = In[x + y*sx];
				t2 = In[sx-1-x + y*sx];
				t3 = In[x + (sy-1-y)*sx];
				t4 = In[sx-1-x + (sy-1-y)*sx];

				Out[ox + oy*osx] = t1;
				Out[osx-1-ox + oy*osx] = t3;
				Out[ox + (osy-1-oy)*osx] = t2;
				Out[osx-1-ox + (osy-1-oy)*osx] = t4;
			}
			else
			{
				if (flipLeftRight) ox = sx-1-x;
					else ox = x;
				if (flipUpDown) oy = sy-1-y;
					else oy = y;

				t1 = In[x + y*sx];
				t2 = In[sx-1-x + y*sx];
				t3 = In[x + (sy-1-y)*sx];
				t4 = In[sx-1-x + (sy-1-y)*sx];

				Out[ox + oy*osx] = t1;
				Out[osx-1-ox + oy*osx] = t2;
				Out[ox + (osy-1-oy)*osx] = t3;
				Out[osx-1-ox + (osy-1-oy)*osx] = t4;
			}
		}
	}
}


void TransformPlane16bit
(
	unsigned short * In,
	unsigned short * Out,
	int sx,
	int sy,
	int flipLeftRight,
	int flipUpDown,
	int rotate90
)
{
	int y;
	int osx, osy;

	// no transform case
	if ((!flipLeftRight) && (!flipUpDown) && (!rotate90))
	{
		if (In!=Out)
			memcpy (Out, In, sx*sy*sizeof(unsigned short));
		return;
	}

	// can't rotate in-place
	if (rotate90 && (In == Out))
		return;

	if (rotate90) {osx = sy; osy = sx;}
		else {osx = sx; osy = sy;}

	// processing 4 mirrored locations at once
	#pragma omp parallel for schedule(guided)
	for (y=0; y<(sy+1)/2; ++y)
	{
		int x;
		int ox, oy;
		unsigned short t1, t2, t3, t4;

		for (x=0; x<(sx+1)/2; ++x)
		{
			if (rotate90)
			{
				if (flipLeftRight) ox = y;
					else ox = osx-1-y;
				if (flipUpDown) oy = osy-1-x;
					else oy = x;

				t1 = In[x + y*sx];
				t2 = In[sx-1-x + y*sx];
				t3 = In[x + (sy-1-y)*sx];
				t4 = In[sx-1-x + (sy-1-y)*sx];

				Out[ox + oy*osx] = t1;
				Out[osx-1-ox + oy*osx] = t3;
				Out[ox + (osy-1-oy)*osx] = t2;
				Out[osx-1-ox + (osy-1-oy)*osx] = t4;
			}
			else
			{
				if (flipLeftRight) ox = sx-1-x;
					else ox = x;
				if (flipUpDown) oy = sy-1-y;
					else oy = y;

				t1 = In[x + y*sx];
				t2 = In[sx-1-x + y*sx];
				t3 = In[x + (sy-1-y)*sx];
				t4 = In[sx-1-x + (sy-1-y)*sx];

				Out[ox + oy*osx] = t1;
				Out[osx-1-ox + oy*osx] = t2;
				Out[ox + (osy-1-oy)*osx] = t3;
				Out[osx-1-ox + (osy-1-oy)*osx] = t4;
			}
		}
	}
}


// mirror and/or rotate NV21 image
//
// Note:
//   - mirroring can be performed in-place, rotation can not
//   - rotation is 90 degree clockwise
//   - if need to rotate 180 degree - call with flipLeftRight=1, flipUpDown=1
//   - if need to rotate 90 degree counter-clockwise - call with flipLeftRight=1, flipUpDown=1, rotate90=1
//   - it is assumed that image width is even and image height is even
//   - crop coordinates are also transformed if given, but no cropping is performed
//
void TransformNV21
(
	unsigned char * InNV21,
	unsigned char * OutNV21,
	int sx,
	int sy,
	int *crop,
	int flipLeftRight,
	int flipUpDown,
	int rotate90
)
{
	int tmp;

	TransformPlane8bit(InNV21, OutNV21, sx, sy, flipLeftRight, flipUpDown, rotate90);

	// treat UV as a single 16bit entity - makes transform faster
	TransformPlane16bit((unsigned short*)(InNV21+sx*sy), (unsigned short*)(OutNV21+sx*sy), sx/2, sy/2, flipLeftRight, flipUpDown, rotate90);

	if (crop)
	{
		if (rotate90)
		{
			tmp = crop[0]; crop[0] = crop[1]; crop[1] = tmp;
			tmp = crop[2]; crop[2] = crop[3]; crop[3] = tmp;
			if (!flipLeftRight) crop[0] = sy-(crop[0]+crop[2]);
			if (flipUpDown) crop[1] = sx-(crop[1]+crop[3]);
		}
		else
		{
			if (flipLeftRight) crop[0] = sx-(crop[0]+crop[2]);
			if (flipUpDown) crop[1] = sy-(crop[1]+crop[3]);
		}
	}
}
