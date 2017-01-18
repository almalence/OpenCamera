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

package com.almalence.plugins.processing.sequence;

import java.util.Arrays;

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

public class OrderControl extends View
{
	private static int	BORDER_SIZE	= 4;

	public interface SequenceListener
	{
		public void onSequenceChanged(int[] indexes);
	}

	private Bitmap[]			images			= null;
	private boolean[]			enabled			= null;
	private int[]				indexes			= null;
	private int[]				lastIndexes		= null;
	private final Rect			inRect			= new Rect();
	private final RectF			outRect			= new RectF();

	private int					width;
	private int					height;

	private int					touchedIndex	= -1;
	private boolean				touchedClose;
	private float				touchedX;
	private float				touchedY;
	private float				touchingX;

	private final Paint			paint			= new Paint();

	private SequenceListener	listener		= null;

	public OrderControl(final Context context)
	{
		super(context);

		this.init();
	}

	public OrderControl(final Context context, final AttributeSet attrs)
	{
		super(context, attrs);

		this.init();
	}

	public OrderControl(final Context context, final AttributeSet attrs, final int defStyle)
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
				if (this.indexes[i] != this.touchedIndex)
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

					if (!this.enabled[actualIndex])
					{
						this.paint.setColor(Color.rgb(255, 64, 64));
						canvas.drawLine(this.outRect.left, this.outRect.top, this.outRect.right, this.outRect.bottom,
								this.paint);
						canvas.drawLine(this.outRect.left, this.outRect.bottom, this.outRect.right, this.outRect.top,
								this.paint);
					}

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
				this.inRect.right = this.images[this.touchedIndex].getWidth() - 1;
				this.inRect.bottom = this.images[this.touchedIndex].getHeight() - 1;

				final float imageHeight = Math.min(this.height, this.images[this.touchedIndex].getHeight() * imageWidth
						/ (float) this.images[this.touchedIndex].getWidth());
				this.outRect.left = touched * imageWidth + this.touchingX - this.touchedX;
				this.outRect.top = this.height - imageHeight;
				this.outRect.right = this.outRect.left + imageWidth;
				this.outRect.bottom = this.height - 1;

				canvas.drawBitmap(this.images[this.touchedIndex], this.inRect, this.outRect, null);

				if (!this.enabled[this.touchedIndex])
				{
					this.paint.setColor(Color.rgb(255, 64, 64));
					canvas.drawLine(this.outRect.left, this.outRect.top, this.outRect.right, this.outRect.bottom,
							this.paint);
					canvas.drawLine(this.outRect.left, this.outRect.bottom, this.outRect.right, this.outRect.top,
							this.paint);
				}

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
		case MotionEvent.ACTION_MOVE:
			{
				this.touchingX = event.getX();

				final int image_width = this.getImageWidth();
				final int distance = (int) (this.touchingX - this.touchedX);
				final int shift = (distance / image_width) + ((distance % image_width) / (image_width / 2));

				int pos = 0;
				for (int i = 1; i < this.indexes.length; i++)
				{
					if (this.indexes[i] == this.touchedIndex)
					{
						pos = i;
					}
				}

				for (int i = 0; i < Math.abs(shift); i++)
				{
					final int n0 = Math.min(this.indexes.length - 1, Math.max(0, pos + i * (int) Math.signum(shift)));
					final int n1 = Math.min(this.indexes.length - 1,
							Math.max(0, pos + (i + 1) * (int) Math.signum(shift)));

					final int t = this.indexes[n0];
					this.indexes[n0] = this.indexes[n1];
					this.indexes[n1] = t;
				}

				if (Math.abs(shift) > 0)
				{
					this.touchedX += shift * image_width;
				}

				final float dx = event.getX() - this.touchedX;
				final float dy = event.getY() - this.touchedY;
				if (Math.sqrt(dx * dx + dy * dy) > 10.0f * this.getContext().getResources().getDisplayMetrics().density)
				{
					this.touchedClose = false;
				}
			}
			break;

		case MotionEvent.ACTION_DOWN:
			{
				this.touchedX = event.getX();
				this.touchedY = event.getY();
				this.touchingX = event.getX();
				final int touch_pos = (int) (this.touchedX / this.getImageWidth());
				this.touchedIndex = this.indexes[Math.min(Math.max(touch_pos, 0), this.indexes.length - 1)];
				this.touchedClose = true;
			}
			break;

		case MotionEvent.ACTION_UP:
			{
				int count = 0;
				for (int i = 0; i < this.enabled.length; i++)
				{
					if (this.enabled[i])
					{
						count++;
					}
				}

				if (this.touchedClose && this.touchedIndex != -1)
				{
					if (count > 1 || !this.enabled[this.touchedIndex])
					{
						this.enabled[this.touchedIndex] = !this.enabled[this.touchedIndex];
					}
				}

				this.touchedIndex = -1;

				this.notifyListener();
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
			final int[] order = this.generateOrderArray();
			boolean changed = false;

			for (int i = 0; i < order.length; i++)
			{
				if (order[i] != this.lastIndexes[i])
				{
					changed = true;
					break;
				}
			}

			if (changed)
			{
				final int[] msg = Arrays.copyOf(order, order.length);

				this.lastIndexes = order;

				this.listener.onSequenceChanged(msg);
			}
		}
	}

	private int[] generateOrderArray()
	{
		final int[] out = new int[this.indexes.length];

		int count = 0;

		for (int i = 0; i < this.images.length; i++)
		{
			if (this.enabled[this.indexes[i]])
			{
				out[count++] = this.indexes[i];
			}
		}

		while (count < out.length)
		{
			out[count++] = -1;
		}

		return out;
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

			this.lastIndexes = Arrays.copyOf(this.indexes, this.indexes.length);

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
