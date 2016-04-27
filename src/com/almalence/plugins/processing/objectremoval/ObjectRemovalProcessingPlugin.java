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

package com.almalence.plugins.processing.objectremoval;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.almalence.SwapHeap;
import com.almalence.opencamunderground.ApplicationInterface;
import com.almalence.opencamunderground.ApplicationScreen;
import com.almalence.opencamunderground.PluginManager;
import com.almalence.opencamunderground.R;
import com.almalence.opencamunderground.cameracontroller.CameraController;
import com.almalence.util.Size;
/* <!-- +++
 import com.almalence.opencam_plus.ApplicationInterface;
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 
 +++ --> */
// <!-- -+-
//-+- -->

import com.almalence.plugins.processing.multishot.MultiShotProcessingPlugin;
import com.almalence.plugins.processing.multishot.AlmaCLRShot.ObjBorderInfo;
import com.almalence.plugins.processing.multishot.AlmaCLRShot.ObjectInfo;
/***
 * Implements night processing
 ***/

public class ObjectRemovalProcessingPlugin extends MultiShotProcessingPlugin
{

	private View			postProcessingView;

	private long			sessionID		= 0;

	private static int		mAngle			= 0;

	private boolean			released		= false;

	private int				mImageDataOrientation;
	private int				mLayoutOrientation;
	private boolean			mCameraMirrored;
	private int				mSensorOrientation;

	// indicates that no more user interaction needed
	private boolean			finishing		= false;
	
	public ObjectRemovalProcessingPlugin()
	{
		super("com.almalence.plugins.objectremovalprocessing", "objectremoval", 0, 0, 0, null);
	}

	public View getPostProcessingView()
	{
		return postProcessingView;
	}

	public void onStart()
	{
		getPrefs();
	}

	public void onStartProcessing(long SessionID)
	{
		finishing = false;
		released = false;
		Message msg = new Message();
		msg.what = ApplicationInterface.MSG_PROCESSING_BLOCK_UI;
		ApplicationScreen.getMessageHandler().sendMessage(msg);

		PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, 
				ApplicationInterface.MSG_CONTROL_LOCKED);

		ApplicationScreen.getGUIManager().lockControls = true;

		sessionID = SessionID;

		PluginManager.getInstance().addToSharedMem("modeSaveName" + sessionID,
				PluginManager.getInstance().getActiveMode().modeSaveName);

		mImageDataOrientation = Integer.valueOf(PluginManager.getInstance().getFromSharedMem("frameorientation1" + sessionID));
		mCameraMirrored = Boolean.valueOf(PluginManager.getInstance().getFromSharedMem("framemirrored1" + sessionID));
		mSensorOrientation = CameraController.getSensorOrientation(mCameraMirrored);
		
		mLayoutOrientation = ApplicationScreen.getGUIManager().getLayoutOrientation();
		
		CameraController.Size imageSize = CameraController.getCameraImageSize();
		int iSaveImageWidth = imageSize.getWidth();
		int iSaveImageHeight = imageSize.getHeight();

		getPrefs();
	
		int imagesAmount = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
				"amountofcapturedframes" + sessionID));

		if (imagesAmount == 0)
			imagesAmount = 1;

		PluginManager.getInstance()
				.addToSharedMem("amountofresultframes" + sessionID, String.valueOf(imagesAmount));

		PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID, String.valueOf(iSaveImageWidth));
		PluginManager.getInstance().addToSharedMem("saveImageHeight" + sessionID, String.valueOf(iSaveImageHeight));

		try
		{
			getDisplaySize();
			ObjectRemovalCore.getInstance().initializeParameters(mDisplayWidth, mDisplayHeight);
			ObjectRemovalCore.getInstance().onStartProcessing();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/************************************************
	 * POST PROCESSING
	 ************************************************/
	public boolean isPostProcessingNeeded()
	{
		return true;
	}

	private ImageView				mImgView;
	private Button					mSaveButton;

	private int						mLayoutOrientationCurrent;
	private int						mDisplayOrientationCurrent;

	private static final int		MSG_REDRAW			= 1;
	private static final int		MSG_LEAVING			= 3;
	private static final int		MSG_END_OF_LOADING	= 4;
	private static final int		MSG_SAVE			= 5;
	private final Handler			mHandler			= new Handler(this);
	private boolean[]				mObjStatus;
	private Bitmap					PreviewBmp			= null;
	public static int				mPreviewWidth;
	public static int				mPreviewHeight;
	public static int				mDisplayWidth;
	public static int				mDisplayHeight;
	
	@Override
	public void setYUVBufferList(ArrayList<Integer> YUVBufferList)
	{
		ObjectRemovalCore.getInstance().setYUVBufferList(YUVBufferList);
	}

	Paint								paint				= null;

	private boolean						postProcessingRun	= false;

	public void onStartPostProcessing()
	{
		mDisplayOrientationCurrent = ApplicationScreen.getGUIManager().getDisplayOrientation();
		int orientation = ApplicationScreen.getGUIManager().getLayoutOrientation();
		mLayoutOrientationCurrent = (orientation == 0 || orientation == 180) ? orientation : (orientation + 180) % 360;

		LayoutInflater inflator = ApplicationScreen.instance.getLayoutInflater();
		postProcessingView = inflator.inflate(R.layout.plugin_processing_objectremoval_postprocessing, null, false);

		mImgView = ((ImageView) postProcessingView.findViewById(R.id.objectremovalImageHolder));

		mObjStatus = new boolean[ObjectRemovalCore.getTotalObjNum()];
		Arrays.fill(mObjStatus, true);

		if (PreviewBmp != null)
		{
			PreviewBmp.recycle();
		}

		paint = new Paint();
		paint.setColor(0xFF00AAEA);
		paint.setStrokeWidth(5);
		paint.setPathEffect(new DashPathEffect(new float[] { 5, 5 }, 0));
		
		PreviewBmp = ObjectRemovalCore.getPreviewBitmap();
		mPreviewWidth = PreviewBmp.getWidth();
		mPreviewHeight = PreviewBmp.getHeight();
		drawObjectRectOnBitmap(PreviewBmp, ObjectRemovalCore.getObjectInfoList(), ObjectRemovalCore.getObjBorderBitmap(paint));

		if (PreviewBmp != null)
		{
			int rotation = ApplicationScreen.getGUIManager().getMatrixRotationForBitmap(mImageDataOrientation, mLayoutOrientation, mCameraMirrored);
			
			if(rotation != 0)
			{
				Matrix matrix = new Matrix();
				matrix.postRotate(rotation);
				Bitmap rotated = Bitmap.createBitmap(PreviewBmp, 0, 0, PreviewBmp.getWidth(), PreviewBmp.getHeight(),
						matrix, true);
				mImgView.setImageBitmap(rotated);
			}
			else
				mImgView.setImageBitmap(PreviewBmp);
		}

		mHandler.sendEmptyMessage(MSG_END_OF_LOADING);
	}

	public void getDisplaySize()
	{
		Display display = ((WindowManager) ApplicationScreen.instance.getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		Point dis = new Point();
		display.getSize(dis);

		CameraController.Size imageSize = CameraController.getCameraImageSize();
		float imageRatio = (float) imageSize.getWidth() / (float) imageSize.getHeight();
		float displayRatio = (float) dis.y / (float) dis.x;

		if (imageRatio > displayRatio)
		{
			mDisplayWidth = dis.y;
			mDisplayHeight = (int) ((float) dis.y / (float) imageRatio);
		} else
		{
			mDisplayWidth = (int) ((float) dis.x * (float) imageRatio);
			mDisplayHeight = dis.x;
		}
		return;
	}

	private void setupImageView()
	{
		mImgView.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					if (finishing)
						return true;
					float x = event.getX();
					float y = event.getY();
					
					//Image data has different orientation according to device's sensor orientation
					//Object removal operate with data as is, it means that seeing preview is differ by orientation from processing data.
					//So we have to calculate new touch coordinates for real data
					if(mSensorOrientation == 270)
					{
						x = mDisplayWidth - event.getY();
						y = event.getX();
					}
					else if(mSensorOrientation == 90)
					{
						x = event.getY();
						y = mDisplayHeight - 1 - event.getX();
					}
					else if(mSensorOrientation == 180)
					{
						x = mDisplayHeight - 1 - event.getX();
						y = mDisplayWidth - 1 - event.getY();
					}
					
					int objIndex = 0;
					try
					{
						objIndex = ObjectRemovalCore.getOccupiedObject(x, y);
					} catch (Exception e)
					{
						e.printStackTrace();
					}

					if (objIndex >= 1)
					{
						mObjStatus[objIndex - 1] = !mObjStatus[objIndex - 1];
					}
					mHandler.sendEmptyMessage(MSG_REDRAW);
				}
				return false;
			}
		});
	}

	public void setupSaveButton()
	{
		// put save button on screen
		mSaveButton = new Button(ApplicationScreen.instance);
		mSaveButton.setBackgroundResource(R.drawable.button_save_background);
		mSaveButton.setOnClickListener(this);
		LayoutParams saveLayoutParams = new LayoutParams(
				(int) (ApplicationScreen.getMainContext().getResources().getDimension(R.dimen.postprocessing_savebutton_size)),
				(int) (ApplicationScreen.getMainContext().getResources().getDimension(R.dimen.postprocessing_savebutton_size)));
		saveLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		saveLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		float density = ApplicationScreen.getAppResources().getDisplayMetrics().density;
		saveLayoutParams.setMargins((int) (density * 8), (int) (density * 8), 0, 0);
		((RelativeLayout) postProcessingView.findViewById(R.id.objectremovalLayout)).addView(mSaveButton,
				saveLayoutParams);
		mSaveButton.setRotation(mLayoutOrientationCurrent);
	}

	public void onOrientationChanged(int orientation)
	{
		if (orientation != mDisplayOrientationCurrent)
		{
			mLayoutOrientationCurrent = (orientation == 0 || orientation == 180) ? orientation + 90 : orientation - 90;
			mDisplayOrientationCurrent = orientation;
			if (postProcessingRun)
				mSaveButton.setRotation(mLayoutOrientationCurrent);
		}
	}

	@Override
	public void onClick(View v)
	{
		if (v == mSaveButton)
		{
			if (finishing)
				return;
			finishing = true;
			mHandler.sendEmptyMessage(MSG_SAVE);
		}
	}

	public void savePicture(Context context)
	{
		byte[] result = ObjectRemovalCore.processingSaveData();
		int frame_len = result.length;
		int frame = SwapHeap.SwapToHeap(result);
		PluginManager.getInstance().addToSharedMem("resultframeformat1" + sessionID, "jpeg");
		PluginManager.getInstance().addToSharedMem("resultframe1" + sessionID, String.valueOf(frame));
		PluginManager.getInstance().addToSharedMem("resultframelen1" + sessionID, String.valueOf(frame_len));

		PluginManager.getInstance().addToSharedMem("resultframeorientation1" + sessionID,String.valueOf(mImageDataOrientation));
		PluginManager.getInstance().addToSharedMem("resultframemirrored1" + sessionID, String.valueOf(mCameraMirrored));

		PluginManager.getInstance().addToSharedMem("amountofresultframes" + sessionID, String.valueOf(1));

		PluginManager.getInstance().addToSharedMem("sessionID", String.valueOf(sessionID));
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		switch (msg.what)
		{
		case MSG_END_OF_LOADING:
			setupImageView();
			setupSaveButton();
			postProcessingRun = true;
			break;
		case MSG_SAVE:
			try
			{
				ObjectRemovalCore.setObjectList(mObjStatus);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			savePicture(ApplicationScreen.getMainContext());
			mHandler.sendEmptyMessage(MSG_LEAVING);
			break;
		case MSG_LEAVING:
			if (released)
				return false;
			ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_POSTPROCESSING_FINISHED);
			
			ObjectRemovalCore.getInstance().clear();

			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, 
					ApplicationInterface.MSG_CONTROL_UNLOCKED);

			ApplicationScreen.getGUIManager().lockControls = false;

			postProcessingRun = false;

			ObjectRemovalCore.release();
			released = true;
			break;

		case MSG_REDRAW:
			if (PreviewBmp != null)
				PreviewBmp.recycle();
			if (finishing)
				return true;
			PreviewBmp = ObjectRemovalCore.getPreviewBitmap();
			mPreviewWidth = PreviewBmp.getWidth();
			mPreviewHeight = PreviewBmp.getHeight();
			drawObjectRectOnBitmap(PreviewBmp, ObjectRemovalCore.getObjectInfoList(), ObjectRemovalCore.getObjBorderBitmap(paint));
			if (PreviewBmp != null)
			{
				int rotation = ApplicationScreen.getGUIManager().getMatrixRotationForBitmap(mImageDataOrientation, mLayoutOrientation, mCameraMirrored);
				if(rotation != 0)
				{
					Matrix matrix = new Matrix();
					matrix.postRotate(rotation);
					Bitmap rotated = Bitmap.createBitmap(PreviewBmp, 0, 0, PreviewBmp.getWidth(), PreviewBmp.getHeight(),
							matrix, true);
					mImgView.setImageBitmap(rotated);
				}
				else
					mImgView.setImageBitmap(PreviewBmp);	
			}
			break;
		default:
			break;
		}
		return true;
	}

	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& ApplicationScreen.instance.findViewById(R.id.postprocessingLayout).getVisibility() == View.VISIBLE)
		{
			if (finishing)
				return true;
			finishing = true;
			mHandler.sendEmptyMessage(MSG_LEAVING);
			return true;
		}

		return false;
	}

	private void drawObjectRectOnBitmap(Bitmap bitmap, ObjectInfo[] objInfo, ObjBorderInfo[] boderInfo)
	{
		float ratio = 0.f;

		Paint paint = new Paint();
		paint.setColor(Color.rgb(0, 255, 0));
		paint.setStrokeWidth(6);
		paint.setAlpha(255);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);

		switch (mAngle)
		{
		case 0:
			ratio = (float) 1.f;
			break;
		case 90:
			ratio = (float) bitmap.getWidth() / (float) bitmap.getHeight();
			break;
		case 180:
			ratio = (float) 1.f;
			break;
		case 270:
			ratio = (float) bitmap.getWidth() / (float) bitmap.getHeight();
			break;
		default:
			break;
		}

		Canvas c = new Canvas(bitmap);

		int i = 0;
		for (ObjectInfo obj : objInfo)
		{
			if (obj.getThumbnail() == null)
			{
				continue;
			}
			float left = (float) obj.getRect().left / ratio;
			float top = (float) obj.getRect().top / ratio;
			float right = (float) obj.getRect().right / ratio;
			float bottom = (float) obj.getRect().bottom / ratio;

			Rect newRect = new Rect((int) Math.round(left), (int) Math.round(top), (int) Math.round(right),
					(int) Math.round(bottom));

			if (!mObjStatus[i])
			{
				c.drawBitmap(obj.getThumbnail(), null, newRect, paint);
			}
			i++;
		}

		Paint p;
		p = new Paint();

		for (ObjBorderInfo obj : boderInfo)
		{
			if (obj == null)
			{
				continue;
			}
			if (obj.getThumbnail() == null)
			{
				continue;
			}

			Rect newRect = obj.getRect();

			c.drawBitmap(obj.getThumbnail(), null, newRect, p);
		}

		return;
	}

	/************************************************
	 * POST PROCESSING END
	 ************************************************/

	private void getPrefs()
	{
		/*
		 Code commented out because there are no correspondent controls exposed to the user
		 ToDo: either delete (more likely), or add these controls as advanced
		 
		// Get the xml/preferences.xml preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.instance
				.getBaseContext());
		mSensitivity = prefs.getInt("Sensitivity", 19); // Should we manage this
														// parameter or it's
														// final value of 19?
		mMinSize = prefs.getInt("MinSize", 1000); // Should we manage this
													// parameter or it's final
													// value of 1000?
		mGhosting = prefs.getString("Ghosting", "2"); // Should we manage this
														// parameter or it's
														// final value of 2?
		*/
	}
}
