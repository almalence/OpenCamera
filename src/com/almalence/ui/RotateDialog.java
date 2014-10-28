package com.almalence.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.View;

public abstract class RotateDialog extends Dialog
{
	protected View	layoutView;
	protected int	currentOrientation;

	public RotateDialog(Context context)
	{
		super(context);
	}

	/**
	 * Rotate the view counter-clockwise
	 * 
	 * @param degree
	 */
	public abstract void setRotate(int degree);
}
