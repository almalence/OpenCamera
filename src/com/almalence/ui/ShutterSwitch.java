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

package com.almalence.ui;

/* <!-- +++
 import com.almalence.opencam_plus.R;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.R;
//-+- -->

import com.almalence.util.Util;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

public class ShutterSwitch extends View
{
	private static final int			TOUCH_MODE_IDLE		= 0;
	private static final int			TOUCH_MODE_DOWN		= 1;
	private static final int			TOUCH_MODE_DRAGGING	= 2;

	public static final int				STATE_PHOTO_ACTIVE	= 0;
	public static final int				STATE_VIDEO_ACTIVE	= 1;

	private int							state				= STATE_VIDEO_ACTIVE;

	private Drawable					mThumbDrawable;
	private Drawable					mTrackDrawable;
	private int							mSwitchMinWidth;
	private int							mSwitchMinHeight;

	private int							mTouchMode;
	private int							mTouchSlop;
	private float						mTouchX;
	private float						mTouchY;
	private VelocityTracker				mVelocityTracker	= VelocityTracker.obtain();
	private int							mMinFlingVelocity;

	private float						mThumbPosition;
	private float						mThumbPositionTemp;
	private int							mSwitchWidth;
	private int							mSwitchHeight;
	private int							mThumbWidth;											// Does
																								// not
																								// include
																								// padding

	private int							mSwitchLeft;
	private int							mSwitchTop;
	private int							mSwitchRight;
	private int							mSwitchBottom;

	private OnShutterClickListener		onShutterClickListener;
	private OnShutterCheckedListener	onShutterCheckedListener;
	private long						mTouchTimeStart;
	private boolean						mThumbMoved;

	private final Rect					mTempRect			= new Rect();

	private static final int[]			STATE_PRESSED		= { android.R.attr.state_pressed };
	private static final int[]			STATE_DEFAULT		= {};

	/**
	 * Construct a new Switch with default styling.
	 * 
	 * @param context
	 *            The Context that will determine this widget's theming.
	 */
	public ShutterSwitch(Context context)
	{
		this(context, null);
	}

	/**
	 * Construct a new Switch with default styling, overriding specific style
	 * attributes as requested.
	 * 
	 * @param context
	 *            The Context that will determine this widget's theming.
	 * @param attrs
	 *            Specification of attributes that should deviate from default
	 *            styling.
	 */
	public ShutterSwitch(Context context, AttributeSet attrs)
	{
		this(context, attrs, R.attr.switchStyle);
	}

	/**
	 * Construct a new Switch with a default style determined by the given theme
	 * attribute, overriding specific style attributes as requested.
	 * 
	 * @param context
	 *            The Context that will determine this widget's theming.
	 * @param attrs
	 *            Specification of attributes that should deviate from the
	 *            default styling.
	 * @param defStyle
	 *            An attribute ID within the active theme containing a reference
	 *            to the default style for this widget. e.g.
	 *            android.R.attr.switchStyle.
	 */
	public ShutterSwitch(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		Resources res = getResources();

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Switch, defStyle, 0);

		mThumbDrawable = a.getDrawable(R.styleable.Switch_thumb);
		mTrackDrawable = a.getDrawable(R.styleable.Switch_track);
		mSwitchMinWidth = a.getDimensionPixelSize(R.styleable.Switch_switchMinWidth, 0);
		mSwitchMinHeight = a.getDimensionPixelSize(R.styleable.Switch_switchMinHeight, 0);

		a.recycle();

		ViewConfiguration config = ViewConfiguration.get(context);
		mTouchSlop = config.getScaledTouchSlop();
		mMinFlingVelocity = config.getScaledMinimumFlingVelocity();

		// Refresh display with current params
		refreshDrawableState();
		
		setThumbPositionDefault();
	}

	public void setState(int newState)
	{
		state = newState;
		setThumbPositionDefault();
		if (state == STATE_PHOTO_ACTIVE) {
			setThumbResource(R.drawable.button_shutter);
			setTrackResource(R.drawable.gui_almalence_shutter_switch_bg_1);
		} else {
			setThumbResource(R.drawable.gui_almalence_shutter_video_off);
			setTrackResource(R.drawable.gui_almalence_shutter_switch_bg_2);
		}
		invalidate();
	}
	
	/**
	 * Set the minimum width of the switch in pixels. The switch's width will be
	 * the maximum of this value and its measured width as determined by the
	 * switch drawables and text used.
	 * 
	 * @param pixels
	 *            Minimum width of the switch in pixels
	 * 
	 * @attr ref android.R.styleable#Switch_switchMinWidth
	 */
	public void setSwitchMinWidth(int pixels)
	{
		mSwitchMinWidth = pixels;
		requestLayout();
	}

	/**
	 * Set the minimum height of the switch in pixels. The switch's height will
	 * be the maximum of this value and its measured width as determined by the
	 * switch drawables.
	 * 
	 * @param pixels
	 *            Minimum height of the switch in pixels
	 * 
	 * @attr ref android.R.styleable#Switch_switchMinHeight
	 */
	public void setSwitchMinHeight(int pixels)
	{
		mSwitchMinHeight = pixels;
		requestLayout();
	}

	/**
	 * Get the minimum width of the switch in pixels. The switch's width will be
	 * the maximum of this value and its measured width as determined by the
	 * switch drawables and text used.
	 * 
	 * @return Minimum width of the switch in pixels
	 * 
	 * @attr ref android.R.styleable#Switch_switchMinWidth
	 */
	public int getSwitchMinWidth()
	{
		return mSwitchMinWidth;
	}

	/**
	 * Get the minimum height of the switch in pixels. The switch's height will
	 * be the maximum of this value and its measured width as determined by the
	 * switch drawables.
	 * 
	 * @return Minimum height of the switch in pixels
	 * 
	 * @attr ref android.R.styleable#Switch_switchMinHeight
	 */
	public int getSwitchMinHeight()
	{
		return mSwitchMinHeight;
	}

	/**
	 * Set the drawable used for the track that the switch slides within.
	 * 
	 * @param track
	 *            Track drawable
	 * 
	 * @attr ref android.R.styleable#Switch_track
	 */
	public void setTrackDrawable(Drawable track)
	{
		mTrackDrawable = track;
		requestLayout();
	}

	/**
	 * Set the drawable used for the track that the switch slides within.
	 * 
	 * @param resId
	 *            Resource ID of a track drawable
	 * 
	 * @attr ref android.R.styleable#Switch_track
	 */
	public void setTrackResource(int resId)
	{
		setTrackDrawable(getContext().getResources().getDrawable(resId));
	}

	/**
	 * Get the drawable used for the track that the switch slides within.
	 * 
	 * @return Track drawable
	 * 
	 * @attr ref android.R.styleable#Switch_track
	 */
	public Drawable getTrackDrawable()
	{
		return mTrackDrawable;
	}

	/**
	 * Set the drawable used for the switch "thumb" - the piece that the user
	 * can physically touch and drag along the track.
	 * 
	 * @param thumb
	 *            Thumb drawable
	 * 
	 * @attr ref android.R.styleable#Switch_thumb
	 */
	public void setThumbDrawable(Drawable thumb)
	{
		mThumbDrawable = thumb;
		requestLayout();
	}

	/**
	 * Set the drawable used for the switch "thumb" - the piece that the user
	 * can physically touch and drag along the track.
	 * 
	 * @param resId
	 *            Resource ID of a thumb drawable
	 * 
	 * @attr ref android.R.styleable#Switch_thumb
	 */
	public void setThumbResource(int resId)
	{
		setThumbDrawable(getContext().getResources().getDrawable(resId));
	}

	/**
	 * Get the drawable used for the switch "thumb" - the piece that the user
	 * can physically touch and drag along the track.
	 * 
	 * @return Thumb drawable
	 * 
	 * @attr ref android.R.styleable#Switch_thumb
	 */
	public Drawable getThumbDrawable()
	{
		return mThumbDrawable;
	}

	public void setOnShutterClickListener(OnShutterClickListener listener)
	{
		this.onShutterClickListener = listener;
	}

	public void removeOnShutterClickListener(OnShutterClickListener listener)
	{
		if (this.onShutterClickListener == listener)
		{
			this.onShutterClickListener = null;
		}
	}

	public void setOnShutterCheckedListener(OnShutterCheckedListener listener)
	{
		this.onShutterCheckedListener = listener;
	}

	public void removeOnShutterCheckedListener(OnShutterCheckedListener listener)
	{
		if (this.onShutterCheckedListener == listener)
		{
			this.onShutterCheckedListener = null;
		}
	}

	@Override
	protected void onVisibilityChanged(View changedView, int visibility)
	{
		setThumbPositionDefault();
	};

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		mSwitchWidth = getWidth();
		mSwitchHeight = getHeight();
		mThumbWidth = mSwitchHeight;
		
		setThumbPositionDefault();
	}

	/**
	 * @return true if (x, y) is within the target area of the switch thumb
	 */
	private boolean hitThumb(float x, float y)
	{
		mThumbDrawable.getPadding(mTempRect);
		final int thumbTop = mSwitchTop - mTouchSlop;
		final int thumbLeft = mSwitchLeft + (int) (mThumbPosition + 0.5f) - mTouchSlop;
		final int thumbRight = thumbLeft + mThumbWidth + mTempRect.left + mTempRect.right + mTouchSlop;
		final int thumbBottom = mSwitchBottom + mTouchSlop;
		return x > thumbLeft && x < thumbRight && y > thumbTop && y < thumbBottom;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		mVelocityTracker.addMovement(ev);
		final int action = ev.getActionMasked();
		switch (action)
		{
		case MotionEvent.ACTION_DOWN:
			{
				mThumbMoved = false;
				final float x = ev.getX();
				final float y = ev.getY();
				if (isEnabled() && hitThumb(x, y))
				{
					mTouchMode = TOUCH_MODE_DOWN;
					mThumbDrawable.setState(STATE_PRESSED);
					setThumbPositionDefault();
					mTouchX = x;
					mTouchY = y;
					invalidate();
					mTouchTimeStart = System.currentTimeMillis();
					return true;
				}
				break;
			}

		case MotionEvent.ACTION_MOVE:
			{
				switch (mTouchMode)
				{
				case TOUCH_MODE_IDLE:
					// Didn't target the thumb, treat normally.
					break;

				case TOUCH_MODE_DOWN:
					{
						final float x = ev.getX();
						final float y = ev.getY();
						if (Math.abs(x - mTouchX) > mTouchSlop || Math.abs(y - mTouchY) > mTouchSlop)
						{
							mTouchMode = TOUCH_MODE_DRAGGING;
							getParent().requestDisallowInterceptTouchEvent(true);
							mTouchX = x;
							mTouchY = y;
							return true;
						}
						break;
					}

				case TOUCH_MODE_DRAGGING:
					{
						final float x = ev.getX();
						final float dx = x - mTouchX;
						
						float newPos = 0.f;
						if (state == STATE_PHOTO_ACTIVE) {
							newPos = Math.max(0, Math.min(mThumbPositionTemp + dx, getThumbScrollRange()));
						} else {
							newPos = Math.min(mSwitchWidth, Math.min(mThumbPositionTemp + dx, getThumbScrollRange()));
						}
						
						newPos = Util.clamp(newPos, 0.f, mSwitchWidth);
						
						
						if (newPos != mThumbPositionTemp)
						{
							mThumbPositionTemp = newPos;
							if (state == STATE_PHOTO_ACTIVE) {
								if (mThumbPositionTemp >= 20.f || mThumbPositionTemp < mThumbPosition)
								{
									mThumbPosition = mThumbPositionTemp;
									mThumbMoved = true;
								}
							} else {
								if (mThumbPositionTemp <= mSwitchWidth - 20.f || mThumbPositionTemp > mThumbPosition)
								{
									mThumbPosition = mThumbPositionTemp;
									mThumbMoved = true;
								}
							}
							mTouchX = x;
							invalidate();
						}
						return true;
					}
				default:
					break;
				}
				break;
			}

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			{
				mThumbDrawable.setState(STATE_DEFAULT);
				invalidate();
				if (mTouchMode == TOUCH_MODE_DRAGGING)
				{
					stopDrag(ev);
					return true;
				} else if (onShutterClickListener != null && !mThumbMoved && System.currentTimeMillis() - mTouchTimeStart < 1000)
				{
					// if thumb not moved, and it wasn't longClick, the notify listener about onShutterClick().
					onShutterClickListener.onShutterClick();
				}
				mTouchMode = TOUCH_MODE_IDLE;
				mVelocityTracker.clear();
				break;
			}
		default:
			break;
		}

		return super.onTouchEvent(ev);
	}

	private void cancelSuperTouch(MotionEvent ev)
	{
		MotionEvent cancel = MotionEvent.obtain(ev);
		cancel.setAction(MotionEvent.ACTION_CANCEL);
		super.onTouchEvent(cancel);
		cancel.recycle();
	}

	/**
	 * Called from onTouchEvent to end a drag operation.
	 * 
	 * @param ev
	 *            Event that triggered the end of drag mode - ACTION_UP or
	 *            ACTION_CANCEL
	 */
	private void stopDrag(MotionEvent ev)
	{
		mTouchMode = TOUCH_MODE_IDLE;
		// Up and not canceled, also checks the switch has not been disabled
		// during the drag
		boolean commitChange = ev.getAction() == MotionEvent.ACTION_UP && isEnabled();

		cancelSuperTouch(ev);

		if (commitChange)
		{
			boolean newState;
			mVelocityTracker.computeCurrentVelocity(1000);
			float xvel = mVelocityTracker.getXVelocity();
			if (Math.abs(xvel) > mMinFlingVelocity)
			{
				newState = state == STATE_PHOTO_ACTIVE ? xvel > 0 : xvel < 0;
			} else
			{
				newState = getTargetCheckedState();
			}

			if (newState)
			{
				// Change state of switch
				if (state == STATE_PHOTO_ACTIVE) {
					setState(STATE_VIDEO_ACTIVE);
				} else {
					setState(STATE_PHOTO_ACTIVE);
				}
				
				// if "Checked", notify onChecked listener.
				if (onShutterCheckedListener != null)
				{
					onShutterCheckedListener.onShutterChecked(state);
				}
			} else
			{
				// else notify onClick listener if it wasn't longClick.
				if (onShutterClickListener != null && !mThumbMoved
						&& System.currentTimeMillis() - mTouchTimeStart < 1000)
				{
					onShutterClickListener.onShutterClick();
				}
			
				setThumbPositionDefault();
				invalidate();
			}
		}
	}

	private void setThumbPositionDefault() {
		if (state == STATE_PHOTO_ACTIVE) {
			mThumbPosition = 0.0f;
			mThumbPositionTemp = 0.0f;
		} else {
			mThumbPosition = mSwitchWidth - mThumbWidth;
			mThumbPositionTemp = mSwitchWidth - mThumbWidth;
		}
	}
	
	private boolean getTargetCheckedState()
	{
		return state == STATE_PHOTO_ACTIVE ? mThumbPosition >= getThumbScrollRange() / 2 : mThumbPosition <= getThumbScrollRange() / 2;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);

		int switchRight;
		int switchLeft;

		switchRight = getWidth() - getPaddingRight();
		switchLeft = switchRight - mSwitchWidth;

		int switchTop = 0;
		int switchBottom = 0;
		// switch (getGravity() & Gravity.VERTICAL_GRAVITY_MASK)
		// {
		// default:
		// case Gravity.TOP:
		// switchTop = getPaddingTop();
		// switchBottom = switchTop + mSwitchHeight;
		// break;

		// case Gravity.CENTER_VERTICAL:
		switchTop = (getPaddingTop() + getHeight() - getPaddingBottom()) / 2 - mSwitchHeight / 2;
		switchBottom = switchTop + mSwitchHeight;
		// break;

		// case Gravity.BOTTOM:
		// switchBottom = getHeight() - getPaddingBottom();
		// switchTop = switchBottom - mSwitchHeight;
		// break;
		// }

		mSwitchLeft = switchLeft;
		mSwitchTop = switchTop;
		mSwitchBottom = switchBottom;
		mSwitchRight = switchRight;
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		// Draw the switch
		int switchLeft = mSwitchLeft;
		int switchTop = mSwitchTop;
		int switchRight = mSwitchRight;
		int switchBottom = mSwitchBottom;

		mTrackDrawable.setBounds(switchLeft, switchTop, switchRight, switchBottom);
		mTrackDrawable.draw(canvas);

		canvas.save();

		mTrackDrawable.getPadding(mTempRect);
		int switchInnerLeft = switchLeft + mTempRect.left;
		int switchInnerRight = switchRight - mTempRect.right;
		canvas.clipRect(switchInnerLeft, switchTop, switchInnerRight, switchBottom);

		mThumbDrawable.getPadding(mTempRect);
		final int thumbPos = (int) (mThumbPosition + 0.5f);
		int thumbLeft = switchInnerLeft - mTempRect.left + thumbPos;
		int thumbRight = switchInnerLeft + thumbPos + mThumbWidth + mTempRect.right;

		mThumbDrawable.setBounds(thumbLeft, switchTop, thumbRight, switchBottom);
		mThumbDrawable.draw(canvas);

		canvas.restore();
	}

	private int getThumbScrollRange()
	{
		if (mTrackDrawable == null)
		{
			return 0;
		}
		mTrackDrawable.getPadding(mTempRect);
		return mSwitchWidth - mThumbWidth - mTempRect.left - mTempRect.right;
	}

	@Override
	protected int[] onCreateDrawableState(int extraSpace)
	{
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		return drawableState;
	}

	@Override
	protected void drawableStateChanged()
	{
		super.drawableStateChanged();

		int[] myDrawableState = getDrawableState();

		// Set the state of the Drawable
		// Drawable may be null when checked state is set from XML, from super
		// constructor
		if (mThumbDrawable != null)
			mThumbDrawable.setState(myDrawableState);
		if (mTrackDrawable != null)
			mTrackDrawable.setState(myDrawableState);

		invalidate();
	}

	@Override
	protected boolean verifyDrawable(Drawable who)
	{
		return super.verifyDrawable(who) || who == mThumbDrawable || who == mTrackDrawable;
	}

	public interface OnShutterClickListener
	{
		public void onShutterClick();
	}

	public interface OnShutterCheckedListener
	{
		public void onShutterChecked(int newState);
	}
}