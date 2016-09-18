package com.almalence.plugins.processing.focusstacking;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.widget.OverScroller;
import android.widget.Toast;

public class ShowOffView extends View implements OnGestureListener, OnDoubleTapListener, OnScaleGestureListener
{
	public static final String TAG = "Almalence";
	
	private static final float SWAP_VELOCITY = 0.6f;
	   
	
	private OnShowOffClickListener clickListener;
	
//	private float progress;
	
	private float scaleFactor;
	private float scaleFactorMin;
	private float scaleFactorMax;

	private final GestureDetector gestureDetector;
	private final ScaleGestureDetector scaleGestureDetector;
	private boolean touched;
	
	private Drawable image;

	private final RectF shiftBounds;
	private int			drawableBoundsWidth;
	private int			drawableBoundsHeight;

    private final RectF rectIn ;
    private final RectF rectOut;
    
    private final Matrix matrix;
    
//    private final Paint dividerPaint;

    private final PointerCoords shift0;
//    private final PointerCoords shift1;
    private final OverScroller scroller0;
//    private final OverScroller scroller1;

//    private Drawable thumbDrawable;
//    private final int thumbSize;
//    private boolean thumbTouched;
//    private float thumbLastX;
//    private final int[] thumbState;
    
//    private boolean singleMove = false;
//    private boolean singleMoveNot = false;
    
    private volatile boolean swapping;
    private boolean swapDirection;
    private long swapLastFrame;
    
	
    public ShowOffView(final Context context)
    {
        this(context, null);
    }
    
    public ShowOffView(final Context context, final AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public ShowOffView(final Context context, final AttributeSet attrs, final int defStyle)
    {
        super(context, attrs, defStyle);
        
        this.setPadding(0, 0, 0, 0);
        
//        this.progress = 0.5f;
        this.swapping = false;
        this.scaleFactor = 1.0f;
        this.scaleFactorMin = 1.0f;
        this.scaleFactorMax = 1.0f;

        this.shiftBounds = new RectF();
        
        this.rectIn = new RectF();
    	this.rectOut = new RectF();
    	this.matrix = new Matrix();

        this.gestureDetector = new GestureDetector(context, this);
        this.gestureDetector.setIsLongpressEnabled(true);
        this.gestureDetector.setOnDoubleTapListener(this);
        this.scaleGestureDetector = new ScaleGestureDetector(context, this);
        this.touched = false;
        
//        this.dividerPaint = new Paint();
//        this.dividerPaint.setAntiAlias(true);
//        this.dividerPaint.setColor(context.getResources().getColor(android.R.color.holo_blue_light));
//        this.dividerPaint.setStrokeWidth(DIVIDER_WIDTH);

        this.shift0 = new PointerCoords();
//        this.shift1 = new PointerCoords();
        this.scroller0 = new OverScroller(context);
//        this.scroller1 = new OverScroller(context);

//        this.thumbTouched = false;
//        this.thumbSize = Math.round(context.getResources().getDisplayMetrics().density * THUMB_SIZE);
//        this.thumbDrawable = (StateListDrawable)context.getResources().getDrawable(android.R.drawable.btn_default);
//        this.thumbState = new int[]
//        {
//        	-android.R.attr.state_pressed,
//        	android.R.attr.state_window_focused,
//        	android.R.attr.state_enabled
//        };
        clickListener = null;
    }
    
//    private void showSingleMoveToast()
//    {
//    	if (this.singleMoveNot != this.singleMove)
//    	{
//    		this.singleMoveNot = this.singleMove;
//    		
//	    	Toast.makeText(this.getContext(),
//	    			this.singleMove ? "Single image mode ON." : "Single image mode OFF.",
//	    			Toast.LENGTH_SHORT).show();
//    	}
//    }
    
//    public void setThumbDrawable(final Drawable drawable)
//    {
//    	this.thumbDrawable = drawable;
//    }
    
    public void setOnDataClickListener(OnShowOffClickListener listener)
    {
    	clickListener = listener;
    }
    
    
    private void clearImages()
    {
//    	for (int i = 0; i < IMAGES_COUNT; i++)
//		{
//			this.image[i] = null;
//		}
    	this.image = null;

		this.scroller0.forceFinished(true);
//		this.scroller1.forceFinished(true);
    }
    
    private void updatePositionFromScroller(final boolean interrupt)
    {
		if (!this.scroller0.isFinished())
		{
			if (interrupt)
			{
				this.scroller0.forceFinished(true);
//				this.scroller1.forceFinished(true);
			}
			this.scroller0.computeScrollOffset();
//			this.scroller1.computeScrollOffset();
			this.shift0.x = this.scroller0.getCurrX();
			this.shift0.y = this.scroller0.getCurrY();
//			this.shift1.x = this.scroller1.getCurrX();
//			this.shift1.y = this.scroller1.getCurrY();
		}
    }
    
    /**
     * @param images Array containing two Drawable instances
     */
    public void setData(final Drawable imageToShow)
    {
    	if (imageToShow == null)
    	{
    		this.clearImages();
    		return;
    	}
    	else
    	{
    		this.image = null;
    		System.gc();
    	}
    	
//    	for (int i = 0; i < IMAGES_COUNT; i++)
//    	{
//    		if (images[i] == null)
//    		{
//    			this.clearImages();
//    			return;
//    		}
//    	}
    	
//    	if (image == null)
//		{
//			this.clearImages();
//			return;
//		}
    	
//    	for (int i = 0; i < IMAGES_COUNT; i++)
//		{    		
//    		this.image[i] = images[i];
//		}
    	
    	this.image = imageToShow;
    	
    	this.calcScaleScope();
    	
    	this.configureBounds();
    	
		this.invalidate();
    }

	@Override
	protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec)
	{
		this.setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
	}
	
	@Override
    protected void onSizeChanged(final int width, final int height, final int old_width, final int old_height)
	{   
        super.onSizeChanged(width, height, old_width, old_height);
        
        this.calcScaleScope();
        
        if (old_width == 0 || old_height == 0)
        {
        	this.shift0.x = 0.0f;
        	this.shift0.y = 0.0f;
//        	this.shift1.x = 0.0f;
//        	this.shift1.y = 0.0f;
        }
        else
        {
        	this.shift0.x = width * (this.shift0.x / (float)old_width);
        	this.shift0.y = height * (this.shift0.y / (float)old_height);
//        	this.shift1.x = width * (this.shift1.x / (float)old_width);
//        	this.shift1.y = height * (this.shift1.y / (float)old_height);
        }
        
        this.configureBounds();
    }
	
	@Override
	protected void onDraw(final Canvas canvas)
	{
//		Log.e(TAG, "onDraw start");
		canvas.drawColor(Color.BLACK);
		
//		if (this.swapping)
//		{
//			final long time = System.currentTimeMillis();
//			
//			if (this.swapDirection)
//			{
//				this.progress += ((time - this.swapLastFrame) / 1000.0f) * SWAP_VELOCITY;
//				
//				if (this.progress >= 1.0f)
//				{
//					this.progress = 1.0f;
//					this.swapDirection = false;
//				}
//			}
//			else
//			{
//				this.progress -= ((time - this.swapLastFrame) / 1000.0f) * SWAP_VELOCITY;
//				
//				if (this.progress <= 0.0f)
//				{
//					this.progress = 0.0f;
//					this.swapDirection = true;
//				}
//			}
//			
//			this.swapLastFrame = time;
//		}
			
		this.updatePositionFromScroller(false);
		
		// Actually draw images
		this.drawImage(canvas, this.image, this.shift0, this.matrix, 0.0f, 1.0f);
//		this.drawImage(canvas, this.image[1], this.shift1, this.matrix[1], 0.0f, this.progress);
		
		
//		final int vwidth = this.getWidth() - this.getPaddingLeft() - this.getPaddingLeft();
//		final int vheight = this.getHeight() - this.getPaddingTop() - this.getPaddingBottom();
		
		// Draw separating line
//		final float pos_x = this.progress * vwidth - DIVIDER_WIDTH / 2.0f;
//		canvas.drawLine(pos_x, 0, pos_x, vheight, this.dividerPaint);
		
		// If not swapping and not interacting to outside of thumb we must also draw the thumb
//		if (!this.swapping && !this.touched)
//		{
//			this.thumbState[0] = this.thumbTouched ? android.R.attr.state_pressed : -android.R.attr.state_pressed;
//			this.thumbDrawable.setState(null);
//			this.thumbDrawable.setState(this.thumbState);
//	        this.thumbDrawable.setBounds(0, 0, this.thumbSize, this.thumbSize);
//			
//			final int saveCount = canvas.getSaveCount();
//			canvas.save();
//			canvas.translate(
//					this.getPaddingLeft() + this.progress * vwidth - this.thumbSize / 2.0f,
//					this.getPaddingTop() + vheight - this.thumbSize);
//			this.thumbDrawable.draw(canvas);
//			canvas.restoreToCount(saveCount);
//		}
		
		if (!this.scroller0.isFinished() || this.swapping)
		{
			this.invalidate();
		}
		
//		Log.e(TAG, "onDraw end");
	}
	
	private void calcScaleScope()
	{
		final int vwidth = this.getWidth();// - this.getPaddingLeft() - this.getPaddingRight();
		final int vheight = this.getHeight();// - this.getPaddingTop() - this.getPaddingBottom();

		if (vwidth > 0 && vheight > 0)
		{
			if (image == null)
			{
				return;
			}
//			final float ratio_w = vwidth / (float)image.getIntrinsicWidth();
//			final float ratio_h = vheight / (float)image.getIntrinsicHeight();
			this.scaleFactorMin = 1.0f;//Math.max(ratio_w, ratio_h) / Math.min(ratio_w, ratio_h);
			
			float max = Math.max(
					image.getIntrinsicWidth() / (float)vwidth,
					image.getIntrinsicHeight() / (float)vheight);
			
				
			final float scale_max = Math.max(
					image.getIntrinsicWidth() / (float)vwidth,
					image.getIntrinsicHeight() / (float)vheight);
			
			max = Math.max(max, scale_max);
			
			
			this.scaleFactorMax = 2.0f;//5.0f * max * this.getContext().getResources().getDisplayMetrics().density;
	
	        this.scaleFactor = Math.min(this.scaleFactorMax, Math.max(this.scaleFactorMin, this.scaleFactor));
		}
	}
	
	private void configureBounds()
	{
//		final Drawable image = this.image[0];
		
		if (image != null)
		{
			final int vwidth = this.getWidth() - this.getPaddingLeft() - this.getPaddingRight();
			final int vheight = this.getHeight() - this.getPaddingTop() - this.getPaddingBottom();
			final int dwidth = image.getIntrinsicWidth();
			final int dheight = image.getIntrinsicHeight();
	
			final float ratio = Math.min(vwidth / (float)dwidth, vheight / (float)dheight);
//			final float ratio_width = vwidth / (float)dwidth;
//			final float ratio_height = vheight / (float)dheight;
			final float horizontal_border = (vwidth / this.scaleFactor);
			final float vertical_border = (vheight / this.scaleFactor);

			float bw = (dwidth * ratio - horizontal_border) / 2.0f;
			float bh = (dheight * ratio - vertical_border) / 2.0f;
//			float bw = (vwidth - horizontal_border)/2.0f;
//			float bh = (vheight - vertical_border)/2.0f;
			
			if(bw < 0) bw = 0;
			if(bh < 0) bh = 0;
			
			this.shiftBounds.left = -bw;
			this.shiftBounds.right = bw;
			this.shiftBounds.top = -bh;
			this.shiftBounds.bottom = bh;
			
			this.configureBounds(this.image, this.matrix, this.rectIn, this.rectOut);
		}
	}
	
	private void configureBounds(final Drawable drawable, final Matrix matrix,	final RectF rectIn, final RectF rectOut)
	{
		if (drawable == null)
		{
			return;
		}

		final int dwidth = drawable.getIntrinsicWidth();
		final int dheight = drawable.getIntrinsicHeight();

		final int vwidth = this.getWidth() - this.getPaddingLeft() - this.getPaddingRight();
		final int vheight = this.getHeight() - this.getPaddingTop() - this.getPaddingBottom();

		final float ratio = Math.min(vwidth / (float)dwidth, vheight / (float)dheight);
//		final float ratio_width = vwidth / (float)dwidth;
//		final float ratio_height = vheight / (float)dheight;
		
		this.drawableBoundsWidth = Math.round(ratio * dwidth * this.scaleFactor);
		this.drawableBoundsHeight = Math.round(ratio * dheight * this.scaleFactor);
		
		drawable.setBounds(
				0,
				0,				
				this.drawableBoundsWidth,
				this.drawableBoundsHeight);

		rectIn.set(0, 0, ratio * dwidth, ratio * dheight);
		rectOut.set(0, 0, vwidth, vheight);

		matrix.setRectToRect(rectIn, rectOut, Matrix.ScaleToFit.START);
	}
	
	private void drawImage(final Canvas canvas, final Drawable drawable, final PointerCoords shift,
			final Matrix matrix, final float progressStart, final float progressEnd)
	{
		if (drawable == null)
		{
			return;
		}

		final int drawableWidth = drawable.getIntrinsicWidth();
		final int drawableHeight = drawable.getIntrinsicHeight();

		if (drawableWidth == 0 || drawableHeight == 0)
		{
			return;
		}
	
		final int saveCount = canvas.getSaveCount();
		canvas.save();

		final int scrollX = this.getScrollX();
		final int scrollY = this.getScrollY();
//		final int vwidth = this.getWidth();// - this.getPaddingLeft() - this.getPaddingLeft();
		
		canvas.clipRect(
				scrollX + this.getPaddingLeft(),
				scrollY + this.getPaddingTop(),
				scrollX + this.getRight() - this.getLeft() - this.getPaddingRight(),// - vwidth * (1.0f - progressEnd),
				scrollY + this.getBottom() - this.getTop() - this.getPaddingBottom());
		
		canvas.translate(
				(float)Math.ceil(this.getPaddingLeft() + (this.shiftBounds.left - shift.x) * this.scaleFactor), 
				(float)Math.ceil(this.getPaddingTop() + (this.shiftBounds.top - shift.y) * this.scaleFactor));
		canvas.concat(matrix);
		
		drawable.draw(canvas);
		
		canvas.restoreToCount(saveCount);
		
//		Log.e(TAG, "drawImage. view.width = " + this.getWidth() + ", view.height = " + this.getHeight());
//		Log.e(TAG, "drawImage. drawableWidth = " + drawableWidth + ", drawableHeight = " + drawableHeight + ", shift.x = " + shift.x + ", shift.y = " + shift.y);
//		Log.e(TAG, "drawImage. shiftBounds.left = " + shiftBounds.left + ", shiftBounds.top = " + shiftBounds.top + ", scaleFactor = " + scaleFactor);
//		Log.e(TAG, "drawImage. scrollX = " + scrollX + ", scrollY = " + scrollY + ", scaleFactor = " + scaleFactor);
//		Log.e(TAG, "drawImage. clipRect(left = " + (scrollX + this.getPaddingLeft()) + ", top = " + (scrollY + this.getPaddingTop()) + 
//				", right = " + (scrollX + this.getRight() - this.getLeft() - this.getPaddingRight()) + ", bottom = " + (scrollY + this.getBottom() - this.getTop() - this.getPaddingBottom()));
	}
	
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(final MotionEvent e)
	{
		final int vwidth = this.getWidth() - this.getPaddingLeft() - this.getPaddingLeft();
		
//		if (!this.swapping)
//		{
//			if (e.getPointerCount() == 1 && e.getAction() == MotionEvent.ACTION_DOWN)
//			{
//				final int vheight = this.getHeight() - this.getPaddingTop() - this.getPaddingBottom();
//				
//				final float dx = Math.abs(this.getPaddingLeft() + this.progress * vwidth - e.getX());
//				final float dy = Math.abs(this.getPaddingTop() + vheight/* - this.thumbSize / 2.0f */- e.getY());
//				
////				if (Math.sqrt(dx * dx + dy * dy) < this.thumbSize)
////				{
////					this.thumbTouched = true;
////				}
//			}
//		}
		
//		if (this.thumbTouched)
//		{
//			switch (e.getAction())
//			{
//			case MotionEvent.ACTION_UP:
//				if (e.getPointerCount() == 1)
//				{
//					this.thumbTouched = false;
//				}
//			case MotionEvent.ACTION_MOVE:
//				this.progress += (e.getX() - this.thumbLastX) / vwidth;
//				this.progress = Math.min(1.0f, Math.max(0.0f, this.progress));
//			case MotionEvent.ACTION_DOWN:
//				this.thumbLastX = e.getX();
//				this.invalidate();
//				break;
//				
//			default:
//				break;
//			}
//		}
//		else
//		{
			if (e.getPointerCount() == 1 && e.getAction() == MotionEvent.ACTION_UP)
			{
				this.touched = false;
				this.invalidate();
			}
			else
			{
				this.touched = true;
			}
			
			this.scaleGestureDetector.onTouchEvent(e);
			this.gestureDetector.onTouchEvent(e);		
			
//		}
		
		return true;
	}

	@Override
	public boolean onDown(final MotionEvent e)
	{		
		this.updatePositionFromScroller(true);
		
		return true;
	}

	@Override
	public boolean onFling(final MotionEvent e0, final MotionEvent e1, final float velocityX, final float velocityY)
	{
//		Log.e(TAG, "onFling. shift0.x = " + this.shift0.x);
//		Log.e(TAG, "onFling. shift0.y = " + this.shift0.y);
		this.updatePositionFromScroller(true);
		
		this.scroller0.fling(
				Math.round(this.shift0.x),
				Math.round(this.shift0.y),
				(int)(-velocityX / this.scaleFactor),
				(int)(-velocityY / this.scaleFactor),
				(int)this.shiftBounds.left,
				(int)this.shiftBounds.right, 
				(int)this.shiftBounds.top,
				(int)this.shiftBounds.bottom);
		
//		this.scroller1.fling(
//				Math.round(this.shift1.x),
//				Math.round(this.shift1.y),
//				(int)(-velocityX / this.scaleFactor),
//				(int)(-velocityY / this.scaleFactor),
//				(int)this.shiftBounds.left,
//				(int)this.shiftBounds.right, 
//				(int)this.shiftBounds.top,
//				(int)this.shiftBounds.bottom);
		
		this.invalidate();
		
		return true;
	}

	@Override
	public void onLongPress(final MotionEvent e)
	{
//		if (e.getPointerCount() == 1)
//		{
//			this.singleMove = !this.singleMove;
//    		this.showSingleMoveToast();
//			this.swapping = false;
//			this.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
//		}
	}

	@Override
	public boolean onScroll(final MotionEvent e0, final MotionEvent e1, final float distanceX, final float distanceY)
	{
		this.updatePositionFromScroller(true);
		
//		if (this.singleMove)
//		{
//			if (e1.getX() > this.progress * this.getWidth())
//			{		
//			    this.shift0.x = Math.max(this.shiftBounds.left, Math.min(this.shiftBounds.right, this.shift0.x + distanceX / this.scaleFactor));
//			    this.shift0.y = Math.max(this.shiftBounds.top, Math.min(this.shiftBounds.bottom, this.shift0.y + distanceY / this.scaleFactor));			
//			}
//			else
//			{
//			    this.shift1.x = Math.max(this.shiftBounds.left, Math.min(this.shiftBounds.right, this.shift1.x + distanceX / this.scaleFactor));
//			    this.shift1.y = Math.max(this.shiftBounds.top, Math.min(this.shiftBounds.bottom, this.shift1.y + distanceY / this.scaleFactor));
//			}
//		}
//		else
//		{			
		    this.shift0.x = Math.max(this.shiftBounds.left, Math.min(this.shiftBounds.right, this.shift0.x + distanceX / this.scaleFactor));
		    this.shift0.y = Math.max(this.shiftBounds.top, Math.min(this.shiftBounds.bottom, this.shift0.y + distanceY / this.scaleFactor));
//		    Log.e(TAG, "onScroll. shift.x = " + this.shift0.x);
//		    Log.e(TAG, "onScroll. shift.y = " + this.shift0.y);
			
//		    this.shift1.x = Math.max(this.shiftBounds.left, Math.min(this.shiftBounds.right, this.shift1.x + distanceX / this.scaleFactor));
//		    this.shift1.y = Math.max(this.shiftBounds.top, Math.min(this.shiftBounds.bottom, this.shift1.y + distanceY / this.scaleFactor));
//		}
		
		this.invalidate();
		
		return true;
	}

	@Override
	public void onShowPress(final MotionEvent e)
	{
		
	}

	@Override
	public boolean onSingleTapUp(final MotionEvent e)
	{
		final float click_x = e.getX();
		final float click_y = e.getY();
		
//		final float bounds_w = this.shiftBounds.width();
//		final float bounds_h = this.shiftBounds.height();
		
		final float image_w = this.image.getIntrinsicWidth();
		final float image_h = this.image.getIntrinsicHeight();
		
		final float scale_x = this.drawableBoundsWidth / image_w;
		final float scale_y = this.drawableBoundsHeight / image_h;
		
//		final float shift_x = Math.abs(this.shift0.x * 2.0f);
//		final float shift_y = Math.abs(this.shift0.y * 2.0f);
		
//		final float shift_scale_x = this.drawableBoundsWidth / shift_x;
//		final float shift_scale_y = this.drawableBoundsHeight / shift_y;
		
//		Log.e(TAG, "shift0.x = " + this.shift0.x);
//		Log.e(TAG, "shift0.y = " + this.shift0.y);
//		Log.e(TAG, "shift X = " + shift_x);
//		Log.e(TAG, "shift Y = " + shift_y);
		
		
//		final float bound_click_x = (shift_x * this.scaleFactor) + click_x;
//		final float bound_click_y = (shift_y * this.scaleFactor) + click_y;
		
		final float bound_click_x = (this.shift0.x != 0? ((this.shiftBounds.width()/2.0f + this.shift0.x) * scaleFactor) : 0) + click_x;
		final float bound_click_y = (this.shift0.y != 0? ((this.shiftBounds.height()/2.0f + this.shift0.y) * scaleFactor) : 0) + click_y;
		
		final float real_click_x = bound_click_x / scale_x;
		final float real_click_y = bound_click_y / scale_y;
		
//		Log.e(TAG, "onSingleTapUp. click_x = " + click_x + " click_y = " + click_y);
//		Log.e(TAG, "onSingleTapUp. Source image width = " + image_w + " height = " + image_h);
//		Log.e(TAG, "onSingleTapUp. ImageView width = " + this.getWidth() + " height = " + this.getHeight());
//		Log.e(TAG, "onSingleTapUp. shiftBounds width = " + this.shiftBounds.width() + " height = " + this.shiftBounds.height());
//		Log.e(TAG, "onSingleTapUp. bounds_w = " + this.drawableBoundsWidth + " bounds_h = " + this.drawableBoundsHeight);
//		Log.e(TAG, "onSingleTapUp. Scale factor = " + scaleFactor);
//		Log.e(TAG, "onSingleTapUp. scale_x = " + scale_x + " scale_y = " + scale_y);
//		Log.e(TAG, "onSingleTapUp. shift0.x = " + this.shift0.x);
//		Log.e(TAG, "onSingleTapUp. shift0.y = " + this.shift0.y);
//		Log.e(TAG, "onSingleTapUp. shift_x = " + shift_x + " shift_y = " + shift_y);
//		Log.e(TAG, "onSingleTapUp. shiftScale_x = " + this.drawableBoundsWidth/shift_x + " shift_y = " + this.drawableBoundsHeight/shift_y);
//		Log.e(TAG, "onSingleTapUp. bound_click_x = " + bound_click_x + " bound_click_y = " + bound_click_y);
//		Log.e(TAG, "onSingleTapUp. real_click_x = " + real_click_x + " real_click_y = " + real_click_y);
		
		if(clickListener != null)
			clickListener.onShowOffClick(real_click_x, real_click_y);
		
		if (this.swapping)
		{
			this.swapping = false;
		}
		
		return true;
	}

	@Override
	public boolean onDoubleTap(final MotionEvent e)
	{
//		if (this.singleMove) return false;
//		
//		this.swapping = true;
//		this.swapLastFrame = System.currentTimeMillis();
//		if (this.progress <= 0.5f)
//		{
//			this.swapDirection = true;
//		}
//		else
//		{
//			this.swapDirection = false;
//		}
//		
//		this.invalidate();
//		
		return true;
	}

	@Override
	public boolean onDoubleTapEvent(final MotionEvent e)
	{
		return true;
	}

	@Override
	public boolean onSingleTapConfirmed(final MotionEvent e)
	{		
		return true;
	}

	@Override
	public boolean onScale(final ScaleGestureDetector scaleGestureDetector)
	{
		this.updatePositionFromScroller(true);
		
		this.scaleFactor *= scaleGestureDetector.getScaleFactor();
        this.scaleFactor = Math.min(this.scaleFactorMax, Math.max(this.scaleFactorMin, this.scaleFactor));

        this.configureBounds();
        
		this.invalidate();
		
		return true;
	}

	@Override
	public boolean onScaleBegin(final ScaleGestureDetector scaleGestureDetector)
	{
		return true;
	}

	@Override
	public void onScaleEnd(final ScaleGestureDetector scaleGestureDetector)
	{
		
	}
}
