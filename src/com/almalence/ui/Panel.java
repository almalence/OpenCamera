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
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.R;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.R;
//-+- -->

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.GridView;
import android.widget.LinearLayout;

/***
 * Panel - implements sliding panel
 ***/

public class Panel extends LinearLayout
{
	/**
	 * Callback invoked when the panel is opened/closed.
	 */
	public static interface OnPanelListener
	{
		/**
		 * Invoked when the panel becomes fully closed.
		 */
		public void onPanelClosed(Panel panel);

		/**
		 * Invoked when the panel becomes fully opened.
		 */
		public void onPanelOpened(Panel panel);
	}

	private boolean				mIsShrinking;
	private final int			mPosition;
	private final int			mDuration;
	private final float			downSpace;
	private final boolean		mLinearFlying;
	private View				mHandle;
	private View				mContent;
	private final Drawable		mOpenedHandle;
	private boolean				mOpened;
	private boolean				startScrolling	= false;
	private boolean				firstTime		= true;
	private boolean				toTheTop		= false;
	private boolean				locationTop		= false;
	private int					moving			= 0;
	private boolean				handle			= true;
	private final Drawable		mClosedHandle;
	private float				mTrackX;
	private float				mTrackY;
	private float				mVelocity;

	private boolean				outsideControl	= false;

	private OnPanelListener		panelListener;

	private static final int	TOP				= 0;
	private static final int	BOTTOM			= 1;
	private static final int	LEFT			= 2;
	private static final int	RIGHT			= 3;

	private enum State
	{
		ABOUT_TO_ANIMATE, ANIMATING, READY, TRACKING, FLYING,
	}

	private State							mState;
	private Interpolator					mInterpolator;
	private final GestureDetector			mGestureDetector;
	private int								mContentHeight;
	private int								mContentWidth;
	private final int						mOrientation;
	private final PanelOnGestureListener	mGestureListener;

	public Panel(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Panel);
		mDuration = a.getInteger(R.styleable.Panel_animationDuration, 750); // duration
		mPosition = a.getInteger(R.styleable.Panel_position, BOTTOM); // position
		downSpace = a.getDimension(R.styleable.Panel_downSpace, 60);

		mLinearFlying = a.getBoolean(R.styleable.Panel_linearFlying, false); // linearFlying
		mOpenedHandle = a.getDrawable(R.styleable.Panel_openedHandle);
		mClosedHandle = a.getDrawable(R.styleable.Panel_closedHandle);
		a.recycle();
		mOrientation = (mPosition == TOP || mPosition == BOTTOM) ? VERTICAL : HORIZONTAL;
		setOrientation(mOrientation);
		mState = State.READY;
		mGestureListener = new PanelOnGestureListener();
		mGestureDetector = new GestureDetector(mGestureListener);
		mGestureDetector.setIsLongpressEnabled(false);
	}

	/**
	 * Sets the listener that receives a notification when the panel becomes
	 * open/close.
	 * 
	 * @param onPanelListener
	 *            The listener to be notified when the panel is opened/closed.
	 */
	public void setOnPanelListener(OnPanelListener onPanelListener)
	{
		panelListener = onPanelListener;
	}

	/**
	 * Gets Panel's mHandle
	 * 
	 * @return Panel's mHandle
	 */
	public View getHandle()
	{
		return mHandle;
	}

	/**
	 * Gets Panel's mContent
	 * 
	 * @return Panel's mContent
	 */
	public View getContent()
	{
		return mContent;
	}

	/**
	 * Sets the acceleration curve for panel's animation.
	 * 
	 * @param i
	 *            The interpolator which defines the acceleration curve
	 */
	public void setInterpolator(Interpolator i)
	{
		mInterpolator = i;
	}

	/**
	 * Set the opened state of Panel.
	 * 
	 * @param open
	 *            True if Panel is to be opened, false if Panel is to be closed.
	 * @param animate
	 *            True if use animation, false otherwise.
	 * 
	 */
	public void setOpen(boolean open, boolean animate)
	{
		if (isOpen() ^ open)
		{
			mIsShrinking = !open;
			if (animate)
			{
				mState = State.ABOUT_TO_ANIMATE;
				if (!mIsShrinking)
				{
					// this could make flicker so we test mState in
					// dispatchDraw()
					// to see if is equal to ABOUT_TO_ANIMATE
					mContent.setVisibility(VISIBLE);
				}
				post(startAnimation);
			} else
			{
				mContent.setVisibility(open ? VISIBLE : GONE);
				postProcess();
			}
		}
		if (!open)
		{
			startScrolling = false;
			firstTime = true;
		}
	}

	/**
	 * Returns the opened status for Panel.
	 * 
	 * @return True if Panel is opened, false otherwise.
	 */
	public boolean isOpen()
	{
		return mContent.getVisibility() == VISIBLE;
	}

	private void SetOnTouchListener(ViewGroup vg, OnTouchListener lst)
	{
		for (int i = 0; i < vg.getChildCount(); i++)
		{
			View child = vg.getChildAt(i);
			if (child instanceof GridView)
			{
				(child).setOnTouchListener(lst);
			} else if (child instanceof ViewGroup)
			{
				SetOnTouchListener((ViewGroup) child, lst);
			}
		}
	}

	@Override
	protected void onFinishInflate()
	{
		super.onFinishInflate();
		mHandle = findViewById(R.id.panelHandle);
		if (mHandle == null)
			throw new RuntimeException("Your Panel must have a View whose id attribute is 'R.id.panelHandle'");
		mHandle.setOnTouchListener(touchListener);

		mContent = findViewById(R.id.panelContent);
		if (mContent == null)
			throw new RuntimeException("Your Panel must have a View whose id attribute is 'R.id.panelContent'");

		mContent.setOnTouchListener(touchListener);

		SetOnTouchListener((ViewGroup) mContent, touchListener);

		// reposition children
		removeView(mHandle);
		removeView(mContent);
		if (mPosition == TOP || mPosition == LEFT)
		{
			addView(mContent);
			addView(mHandle);
		} else
		{
			addView(mHandle);
			addView(mContent);
		}

		if (mClosedHandle != null)
		{
			mHandle.setBackgroundDrawable(mClosedHandle);
			mOpened = false;
		}
		mContent.setVisibility(GONE);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		super.onLayout(changed, l, t, r, b);
		mContentWidth = mContent.getWidth();
		mContentHeight = mContent.getHeight();
	}

	@Override
	protected void dispatchDraw(Canvas canvas)
	{
		if (mState == State.ABOUT_TO_ANIMATE && !mIsShrinking)
		{
			int delta = mOrientation == VERTICAL ? mContentHeight : mContentWidth;
			if (mPosition == LEFT || mPosition == TOP)
			{
				delta = -delta;
			}
			if (mOrientation == VERTICAL)
			{
				canvas.translate(0, delta);
			} else
			{
				canvas.translate(delta, 0);
			}
		}
		if (mState == State.TRACKING || mState == State.FLYING)
		{
			canvas.translate(mTrackX, mTrackY);
			mContent.getBackground().setAlpha((int) (255 - 255 * Math.abs(mTrackY / mContentHeight)));
		}
		super.dispatchDraw(canvas);
	}

	private float ensureRange(float v, int min, int max)
	{
		v = Math.max(v, min);
		v = Math.min(v, max);
		return v;
	}

	public OnTouchListener			touchListener		= new OnTouchListener()
														{
															int		initX;
															int		initY;
															boolean	setInitialPosition;

															public boolean onTouch(View v, MotionEvent event)
															{
																// if controls
																// locked - skip
																// any events
																if (MainScreen.getGUIManager().lockControls)
																	return false;

																int action = event.getAction();

																if (v == MainScreen.getPreviewSurfaceView()
																		|| v == ((View) MainScreen.getInstance()
																				.findViewById(R.id.mainLayout1))
																		|| v.getParent() == (View) MainScreen
																				.getInstance().findViewById(
																						R.id.paramsLayout))
																{
																	if (!mOpened)
																	{
																		handle = false;
																		if (action == MotionEvent.ACTION_DOWN)
																		{
																			if (event.getRawY() > ((20 + (toTheTop ? 0
																					: 65)) * MainScreen
																					.getMainContext().getResources()
																					.getDisplayMetrics().density))
																				return false;
																			else
																				startScrolling = true;
																		}

																		if (action == MotionEvent.ACTION_MOVE)
																		{
																			if (!startScrolling)
																				return false;
																			if (event.getY() < ((toTheTop ? 0 : 65) * MainScreen
																					.getMainContext().getResources()
																					.getDisplayMetrics().density))
																				return false;
																			reorder(true, false);
																		}
																		if ((action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP)
																				&& !startScrolling)
																			return false;
																		outsideControl = false;
																	} else
																	{
																		if ((event.getY() > (mContentHeight + 30))
																				&& (action != MotionEvent.ACTION_DOWN)
																				&& (action != MotionEvent.ACTION_UP))
																			return false;
																		outsideControl = true;
																	}

																} else
																{
																	handle = true;
																	outsideControl = false;
																}
																if (action == MotionEvent.ACTION_DOWN)
																{
																	initX = 0;
																	initY = 0;
																	if (mContent.getVisibility() == GONE)
																	{
																		mContent.getBackground().setAlpha(0);
																		// since
																		// we
																		// may
																		// not
																		// know
																		// content
																		// dimensions
																		// we
																		// use
																		// factors
																		// here
																		if (mOrientation == VERTICAL)
																		{
																			initY = mPosition == TOP ? -1 : 1;
																		} else
																		{
																			initX = mPosition == LEFT ? -1 : 1;
																		}
																	}
																	setInitialPosition = true;
																} else
																{
																	if (setInitialPosition)
																	{
																		// now
																		// we
																		// know
																		// content
																		// dimensions,
																		// so we
																		// multiply
																		// factors...
																		initX *= mContentWidth;
																		initY *= mContentHeight;
																		// ...
																		// and
																		// set
																		// initial
																		// panel's
																		// position
																		mGestureListener.setScroll(initX, initY);
																		setInitialPosition = false;
																		// for
																		// offsetLocation
																		// we
																		// have
																		// to
																		// invert
																		// values
																		initX = -initX;
																		initY = -initY;
																	}
																	// offset
																	// every
																	// ACTION_MOVE
																	// &
																	// ACTION_UP
																	// event
																	event.offsetLocation(initX, initY);
																}
																if (!mGestureDetector.onTouchEvent(event))
																{
																	if (action == MotionEvent.ACTION_UP)
																	{
																		// tup
																		// up
																		// after
																		// scrolling
																		post(startAnimation);
																	}
																}
																return false;
															}
														};

	Runnable						startAnimation		= new Runnable()
														{
															public void run()
															{
																// this is why
																// we post this
																// Runnable
																// couple of
																// lines above:
																// now its save
																// to use
																// mContent.getHeight()
																// &&
																// mContent.getWidth()
																TranslateAnimation animation;
																int fromXDelta = 0, toXDelta = 0, fromYDelta = 0, toYDelta = 0;
																if (mState == State.FLYING)
																{
																	mIsShrinking = (mPosition == TOP || mPosition == LEFT)
																			^ (mVelocity > 0);
																}
																if (mContentHeight == 0 || mContentWidth == 0)
																	return;
																int calculatedDuration;
																if (mOrientation == VERTICAL)
																{
																	int height = mContentHeight;
																	if (!mIsShrinking)
																	{
																		fromYDelta = mPosition == TOP ? -height
																				: height;
																	} else
																	{
																		toYDelta = mPosition == TOP ? -height : height;
																	}
																	if (mState == State.TRACKING)
																	{
																		if (Math.abs(mTrackY - fromYDelta) < Math
																				.abs(mTrackY - toYDelta))
																		{
																			mIsShrinking = !mIsShrinking;
																			toYDelta = fromYDelta;
																		}
																		fromYDelta = (int) mTrackY;
																	} else if (mState == State.FLYING)
																	{
																		fromYDelta = (int) mTrackY;
																	}
																	// for
																	// FLYING
																	// events we
																	// calculate
																	// animation
																	// duration
																	// based on
																	// flying
																	// velocity
																	// also for
																	// very high
																	// velocity
																	// make sure
																	// duration
																	// >= 20 ms
																	if (mState == State.FLYING && mLinearFlying)
																	{
																		calculatedDuration = mDuration
																				* Math.abs(toYDelta - fromYDelta)
																				/ mContentHeight;
																		calculatedDuration = Math.max(
																				calculatedDuration, 20);
																	} else
																	{
																		calculatedDuration = mDuration
																				* Math.abs(toYDelta - fromYDelta)
																				/ mContentHeight;
																	}
																} else
																{
																	int width = mContentWidth;
																	if (!mIsShrinking)
																	{
																		fromXDelta = mPosition == LEFT ? -width : width;
																	} else
																	{
																		toXDelta = mPosition == LEFT ? -width : width;
																	}
																	if (mState == State.TRACKING)
																	{
																		if (Math.abs(mTrackX - fromXDelta) < Math
																				.abs(mTrackX - toXDelta))
																		{
																			mIsShrinking = !mIsShrinking;
																			toXDelta = fromXDelta;
																		}
																		fromXDelta = (int) mTrackX;
																	} else if (mState == State.FLYING)
																	{
																		fromXDelta = (int) mTrackX;
																	}
																	// for
																	// FLYING
																	// events we
																	// calculate
																	// animation
																	// duration
																	// based on
																	// flying
																	// velocity
																	// also for
																	// very high
																	// velocity
																	// make sure
																	// duration
																	// >= 20 ms
																	if (mState == State.FLYING && mLinearFlying)
																	{
																		calculatedDuration = (int) (1000 * Math
																				.abs((toXDelta - fromXDelta)
																						/ mVelocity));
																		calculatedDuration = Math.max(
																				calculatedDuration, 20);
																	} else
																	{
																		calculatedDuration = mDuration
																				* Math.abs(toXDelta - fromXDelta)
																				/ mContentWidth;
																	}
																}

																mTrackX = mTrackY = 0;
																if (calculatedDuration == 0)
																{
																	mState = State.READY;
																	if (mIsShrinking)
																	{
																		mContent.setVisibility(GONE);
																	}
																	postProcess();
																	return;
																}

																animation = new TranslateAnimation(fromXDelta,
																		toXDelta, fromYDelta, toYDelta);
																animation.setDuration(calculatedDuration);
																animation.setAnimationListener(animationListener);
																if (mState == State.FLYING && mLinearFlying)
																{
																	animation.setInterpolator(new LinearInterpolator());
																} else if (mInterpolator != null)
																{
																	animation.setInterpolator(mInterpolator);
																}
																startAnimation(animation);
															}
														};

	private final AnimationListener	animationListener	= new AnimationListener()
														{
															public void onAnimationEnd(Animation animation)
															{
																mState = State.READY;
																if (mIsShrinking)
																{
																	mContent.setVisibility(GONE);
																}
																postProcess();
															}

															public void onAnimationRepeat(Animation animation)
															{
															}

															public void onAnimationStart(Animation animation)
															{
																mState = State.ANIMATING;
															}
														};

	private void postProcess()
	{
		if (mIsShrinking && mClosedHandle != null)
		{
			moving = 0;
			reorder(locationTop, false);
			mHandle.setBackgroundDrawable(mClosedHandle);
			mOpened = false;
			startScrolling = false;
		} else if (!mIsShrinking && mOpenedHandle != null)
		{
			mOpened = true;
			startScrolling = false;
			firstTime = true;
			mContent.getBackground().setAlpha(255);
			mHandle.setBackgroundDrawable(mOpenedHandle);
		}
		// invoke listener if any
		if (panelListener != null)
		{
			if (mIsShrinking)
			{
				panelListener.onPanelClosed(Panel.this);
			} else
			{
				panelListener.onPanelOpened(Panel.this);
			}
		}
	}

	class PanelOnGestureListener implements OnGestureListener
	{
		float	scrollY;
		float	scrollX;

		public void setScroll(int initScrollX, int initScrollY)
		{
			scrollX = initScrollX;
			scrollY = initScrollY;
		}

		public boolean onDown(MotionEvent e)
		{
			scrollX = scrollY = 0;
			if (mState != State.READY)
			{
				// we are animating or just about to animate
				return false;
			}
			mState = State.ABOUT_TO_ANIMATE;
			mIsShrinking = mContent.getVisibility() == VISIBLE;
			if (!mIsShrinking)
			{
				// this could make flicker so we test mState in dispatchDraw()
				// to see if is equal to ABOUT_TO_ANIMATE
				mContent.setVisibility(VISIBLE);
			}
			return true;
		}

		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
		{
			mState = State.FLYING;
			mVelocity = mOrientation == VERTICAL ? velocityY : velocityX;
			post(startAnimation);
			return true;
		}

		public void onLongPress(MotionEvent e)
		{
		}

		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
		{
			if (startScrolling && firstTime)
			{
				distanceY += mContentHeight;
				firstTime = false;
			}
			mState = State.TRACKING;
			float tmpY = 0, tmpX = 0;
			if (outsideControl && e2.getY() > (mContentHeight))
				return true;

			if (scrollY > -450)
				reorder(true, false);
			if (mOrientation == VERTICAL)
			{

				scrollY -= distanceY;
				if (mPosition == TOP)
				{
					tmpY = ensureRange(scrollY, -mContentHeight, 0);
				} else
				{
					tmpY = ensureRange(scrollY, 0, mContentHeight);
				}
			} else
			{
				scrollX -= distanceX;
				if (mPosition == LEFT)
				{
					tmpX = ensureRange(scrollX, -mContentWidth, 0);
				} else
				{
					tmpX = ensureRange(scrollX, 0, mContentWidth);
				}
			}
			if (tmpX != mTrackX || tmpY != mTrackY)
			{
				mTrackX = tmpX;
				mTrackY = tmpY;
				moving++;
				invalidate();
			}

			return true;
		}

		public void onShowPress(MotionEvent e)
		{
		}

		public boolean onSingleTapUp(MotionEvent e)
		{
			// simple tap: click
			post(startAnimation);
			return true;
		}
	}

	// corrects margin between content and handler
	public void reorder(boolean toTop, boolean isFromGUI)
	{
		if (isFromGUI)
			locationTop = toTop;
		toTheTop = toTop;
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) this.findViewById(R.id.panelHandle)
				.getLayoutParams();
		float d = MainScreen.getMainContext().getResources().getDisplayMetrics().density;
		if (toTheTop)
		{
			if (!isFromGUI && (moving != 0) && !locationTop && handle)
			{
				moving = moving + 5;
				int margin = (int) (downSpace - moving);
				if (margin < 0)
					margin = 0;
				lp.topMargin = margin;
			} else
				lp.topMargin = 0;
		} else
		{
			lp.topMargin = (int) (downSpace);
		}
		mHandle.setLayoutParams(lp);
	}
}
