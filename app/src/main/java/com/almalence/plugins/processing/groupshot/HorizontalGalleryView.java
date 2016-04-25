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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class HorizontalGalleryView extends View
{
	private static int	BORDER_SIZE	= 4;

	public interface SequenceListener
	{
		public void onSequenceChanged(int selectedIndex);
	}

	private Bitmap[]			images			= null;
	private boolean[]			enabled			= null;
	private int					selected		= 0;
	private int[]				indexes			= null;
	private final Rect			inRect			= new Rect();
	private final RectF			outRect			= new RectF();

	private int					width;
	private int					height;

	private int					touchedIndex	= -1;
	private float				touchedX;
	private float				touchedY;
	private float				touchingX;

	private final Paint			paint			= new Paint();

	private SequenceListener	listener		= null;

	public HorizontalGalleryView(final Context context)
	{
		super(context);

		this.init();
	}

	public HorizontalGalleryView(final Context context, final AttributeSet attrs)
	{
		super(context, attrs);

		this.init();
	}

	public HorizontalGalleryView(final Context context, final AttributeSet attrs, final int defStyle)
	{
		super(context, attrs, defStyle);

		this.init();
	}

	private void init()
	{
		this.paint.setStrokeWidth(BORDER_SIZE);
		this.paint.setStyle(Style.STROKE);
		this.paint.setAntiAlias(true);
	}

	@Override
	protected void onDraw(final Canvas canvas)
	{
		if (this.indexes != null)
		{
			final int imageWidth = this.getImageWidth();
			int touched = -1;

			for (int i = 0; i < this.indexes.length; i++)
			{
				if (this.indexes[i] != selected)
				{
					final int actualIndex = this.indexes[i];

					this.inRect.left = 0;
					this.inRect.top = 0;
					this.inRect.right = this.images[actualIndex].getWidth() - 1;
					this.inRect.bottom = this.images[actualIndex].getHeight() - 1;

					final float imageHeight = Math.min(this.height, this.images[actualIndex].getHeight() * imageWidth
							/ (float) this.images[actualIndex].getWidth());
					this.outRect.left = i * imageWidth;
					this.outRect.top = this.height - imageHeight;
					this.outRect.right = (i + 1) * imageWidth - 1;
					this.outRect.bottom = this.height - 1;

					canvas.drawBitmap(this.images[actualIndex], this.inRect, this.outRect, null);

					this.paint.setColor(Color.WHITE);
					canvas.drawRect(this.outRect.left + BORDER_SIZE / 2.0f, this.outRect.top + BORDER_SIZE / 2.0f,
							this.outRect.right - BORDER_SIZE / 2.0f, this.outRect.bottom - BORDER_SIZE / 2.0f,
							this.paint);
				} else
				{
					touched = i;
				}
			}

			if (touched >= 0)
			{
				this.inRect.left = 0;
				this.inRect.top = 0;
				this.inRect.right = this.images[selected].getWidth() - 1;
				this.inRect.bottom = this.images[selected].getHeight() - 1;

				final float imageHeight = Math.min(this.height, this.images[selected].getHeight() * imageWidth
						/ (float) this.images[selected].getWidth());
				this.outRect.left = touched * imageWidth + this.touchingX - this.touchedX;
				this.outRect.top = this.height - imageHeight;
				this.outRect.right = this.outRect.left + imageWidth;
				this.outRect.bottom = this.height - 1;

				canvas.drawBitmap(this.images[selected], this.inRect, this.outRect, null);

				this.paint.setColor(Color.rgb(0, 160, 255));
				canvas.drawRect(this.outRect.left + BORDER_SIZE / 2.0f, this.outRect.top + BORDER_SIZE / 2.0f,
						this.outRect.right - BORDER_SIZE / 2.0f, this.outRect.bottom - BORDER_SIZE / 2.0f, this.paint);
			}
		}
	}

	@Override
	protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec)
	{
		this.setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
	}

	@Override
	protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh)
	{
		this.width = w;
		this.height = h;

		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event)
	{
		if (!this.isEnabled())
		{
			return true;
		}

		switch (event.getAction())
		{
		case MotionEvent.ACTION_UP:
			{
				touchedX = event.getX();
				this.touchedY = event.getY();
				this.touchingX = event.getX();
				final int touch_pos = (int) (this.touchedX / this.getImageWidth());
				touchedIndex = this.indexes[Math.min(Math.max(touch_pos, 0), this.indexes.length - 1)];
				
				if (touchedIndex != -1)
				{
					if(touchedIndex != selected)
					{
						selected = touchedIndex;
						this.notifyListener();
					}
				}

				this.touchedIndex = -1;
			}
			break;
		default:
			break;
		}

		this.invalidate();
		this.requestLayout();

		return true;
	}

	public void setContent(final Bitmap[] images, final SequenceListener listener)
	{
		if (images != null)
		{
			this.listener = listener;

			this.images = new Bitmap[images.length];
			System.arraycopy(images, 0, this.images, 0, images.length);

			this.initIndexes();
		}
	}

	private void notifyListener()
	{
		if (this.listener != null)
		{
				this.listener.onSequenceChanged(selected);
		}
	}

	private void initIndexes()
	{
		if (this.images != null)
		{
			this.indexes = new int[this.images.length];
			for (int i = 0; i < this.images.length; i++)
			{
				this.indexes[i] = i;
			}

			this.enabled = new boolean[this.images.length];
			for (int i = 0; i < this.images.length; i++)
			{
				this.enabled[i] = true;
			}
		} else
		{
			throw new RuntimeException();
		}
	}

	private int getImageWidth()
	{
		return (this.width / this.images.length);
	}
}
