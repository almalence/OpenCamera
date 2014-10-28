package com.almalence.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

/* <!-- +++
 import com.almalence.opencam_plus.R;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.R;

//-+- -->

public class RotateLayout extends ViewGroup
{

	public static class LayoutParams extends ViewGroup.LayoutParams
	{

		public int	angle;

		public LayoutParams(Context context, AttributeSet attrs)
		{
			super(context, attrs);
			final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RotateLayout_Layout);
			angle = a.getInt(R.styleable.RotateLayout_Layout_layout_angle, 0);
		}

		public LayoutParams(ViewGroup.LayoutParams layoutParams)
		{
			super(layoutParams);
		}

	}

	public RotateLayout(Context context)
	{
		this(context, null);
	}

	public RotateLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setWillNotDraw(false);
	}

	public View getView()
	{
		return getChildAt(0);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		final View view = getView();
		if (view != null)
		{
			final LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
			if (angle != layoutParams.angle)
			{
				angle = layoutParams.angle;
				angleChanged = true;
			}

			if (Math.abs(angle % 180) == 90)
			{
				measureChild(view, heightMeasureSpec, widthMeasureSpec);
				setMeasuredDimension(resolveSize(view.getMeasuredHeight(), widthMeasureSpec),
						resolveSize(view.getMeasuredWidth(), heightMeasureSpec));
			} else
			{
				measureChild(view, widthMeasureSpec, heightMeasureSpec);
				setMeasuredDimension(resolveSize(view.getMeasuredWidth(), widthMeasureSpec),
						resolveSize(view.getMeasuredHeight(), heightMeasureSpec));
			}
		} else
		{
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		if (angleChanged)
		{
			final RectF layoutRect = tempRectF1;
			final RectF layoutRectRotated = tempRectF2;
			layoutRect.set(0, 0, r - l, b - t);
			rotateMatrix.setRotate(angle, layoutRect.centerX(), layoutRect.centerY());
			rotateMatrix.mapRect(layoutRectRotated, layoutRect);
			layoutRectRotated.round(viewRectRotated);
			angleChanged = false;
		}

		final View view = getView();
		if (view != null)
		{
			view.layout(viewRectRotated.left, viewRectRotated.top, viewRectRotated.right, viewRectRotated.bottom);
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas)
	{
		canvas.save();
		canvas.rotate(-angle, getWidth() / 2f, getHeight() / 2f);
		super.dispatchDraw(canvas);
		canvas.restore();
	}

	@Override
	public ViewParent invalidateChildInParent(int[] location, Rect dirty)
	{
		invalidate();
		return super.invalidateChildInParent(location, dirty);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event)
	{
		viewTouchPoint[0] = event.getX();
		viewTouchPoint[1] = event.getY();

		rotateMatrix.mapPoints(childTouchPoint, viewTouchPoint);
		event.setLocation(childTouchPoint[0], childTouchPoint[1]);
		return super.dispatchTouchEvent(event);
	}

	@Override
	public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs)
	{
		return new RotateLayout.LayoutParams(getContext(), attrs);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams)
	{
		return layoutParams instanceof RotateLayout.LayoutParams;
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams)
	{
		return new RotateLayout.LayoutParams(layoutParams);
	}

	public void setAngle(int angle)
	{
		View view = getView();
		LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
		layoutParams.angle = angle;
		this.angle = angle;
		this.angleChanged = true;
	}

	private int				angle;

	private final Matrix	rotateMatrix	= new Matrix();

	private final Rect		viewRectRotated	= new Rect();

	private final RectF		tempRectF1		= new RectF();
	private final RectF		tempRectF2		= new RectF();

	private final float[]	viewTouchPoint	= new float[2];
	private final float[]	childTouchPoint	= new float[2];

	private boolean			angleChanged	= true;

}
