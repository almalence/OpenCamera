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

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.almalence.SwapHeap;
import com.almalence.opencamunderground.ApplicationInterface;
import com.almalence.opencamunderground.ApplicationScreen;
import com.almalence.opencamunderground.PluginManager;
import com.almalence.opencamunderground.R;
import com.almalence.opencamunderground.cameracontroller.CameraController;
import com.almalence.util.ImageConversion;
import com.almalence.util.Size;
/* <!-- +++
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.ApplicationInterface;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
// <!-- -+-
//-+- -->
import com.almalence.plugins.processing.groupshot.HorizontalGalleryView.SequenceListener;
import com.almalence.plugins.processing.multishot.MultiShotProcessingPlugin;

/***
 * Implements group shot processing
 ***/
@SuppressWarnings("deprecation")
public class GroupShotProcessingPlugin extends MultiShotProcessingPlugin implements SequenceListener
{
	private View				postProcessingView;

	private long				sessionID					= 0;

	private static final int	MSG_PROGRESS_BAR_INVISIBLE	= 1;
	private static final int	MSG_PROGRESS_BAR_VISIBLE	= 2;
	private static final int	MSG_LEAVING					= 3;
	private static final int	MSG_END_OF_LOADING			= 4;
	private static final int	MSG_SELECTOR_VISIBLE		= 5;
	private static final int	MSG_SELECTOR_INVISIBLE		= 6;

	static final int			img2lay						= 8;					// 16
																					// image-to-layout
																					// subsampling
																					// factor

	private int					mDisplayOrientation;

	static int					OutNV21						= 0;

	static int[]				mPixelsforPreview			= null;

	static int					mBaseFrame					= 0;					// temporary

	static int[]				crop						= new int[5];			// crop
																					// parameters
																					// and
																					// base
																					// image
																					// are
																					// stored
																					// here

	private ImageView			mResultImageView;
	private Button				mSaveButton;

	private final Handler		mHandler					= new Handler(this);

	private ProgressBar			mProgressBar;
//	private Gallery				mGallery;
	private TextView			mInfoTextVeiw;

	private HorizontalGalleryView	    sequenceView;
	private static ArrayList<Bitmap>	thumbnails		= new ArrayList<Bitmap>();
	private ImageAdapter		mImageAdapter;
	private int					iMatrixRotation			= 0;

	@Override
	public void setYUVBufferList(ArrayList<Integer> YUVBufferList)
	{
		GroupShotCore.getInstance().setYUVBufferList(YUVBufferList);
	}

	private final Object	syncObject			= new Object();

	private boolean			postProcessingRun	= false;

	// indicates that no more user interaction needed
	private boolean			mFinishing			= false;
	private boolean			mChangingFace		= false;

	public GroupShotProcessingPlugin()
	{
		super("com.almalence.plugins.groupshotprocessing", "groupshot", 0, 0, 0, null);
	}

	public View getPostProcessingView()
	{
		return postProcessingView;
	}

	public void onStart()
	{
	}

	public void onStartProcessing(long SessionID)
	{
		mFinishing = false;
		mChangingFace = false;
		Message msg = new Message();
		msg.what = ApplicationInterface.MSG_PROCESSING_BLOCK_UI;
		ApplicationScreen.getMessageHandler().sendMessage(msg);

		PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST,
				ApplicationInterface.MSG_CONTROL_LOCKED);

		ApplicationScreen.getGUIManager().lockControls = true;

		sessionID = SessionID;

		PluginManager.getInstance().addToSharedMem("modeSaveName" + sessionID,
				PluginManager.getInstance().getActiveMode().modeSaveName);

		int imageDataOrientation = Integer.valueOf(PluginManager.getInstance().getFromSharedMem(
				"frameorientation1" + sessionID));
		int deviceOrientation = Integer.valueOf(PluginManager.getInstance().getFromSharedMem(
				"deviceorientation1" + sessionID));
		boolean cameraMirrored = Boolean.valueOf(PluginManager.getInstance().getFromSharedMem(
				"framemirrored1" + sessionID));

		iMatrixRotation = ApplicationScreen.getGUIManager().getMatrixRotationForBitmap(imageDataOrientation, deviceOrientation, cameraMirrored);
		
		try
		{
			GroupShotCore.getInstance().initializeProcessingParameters(imageDataOrientation, deviceOrientation, cameraMirrored, iMatrixRotation);
			GroupShotCore.getInstance().ProcessFaces();
		} catch (Exception e)
		{
			// make notifier in main thread
			e.printStackTrace();
		}

		PluginManager.getInstance().addToSharedMem("resultfromshared" + sessionID, "false");
		PluginManager.getInstance().addToSharedMem("amountofresultframes" + sessionID, "1");

		CameraController.Size imageSize = CameraController.getCameraImageSize();
		int imageWidth = imageSize.getWidth();
		int imageHeight = imageSize.getHeight();
		PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID, String.valueOf(imageWidth));
		PluginManager.getInstance().addToSharedMem("saveImageHeight" + sessionID, String.valueOf(imageHeight));
		
		int orientation = ApplicationScreen.getGUIManager().getLayoutOrientation();
		mDisplayOrientation = orientation == 0 || orientation == 180 ? orientation : (orientation + 180) % 360;
		mImageAdapter = new ImageAdapter(ApplicationScreen.getMainContext(), GroupShotCore.getInstance().getYUVBufferList(), imageDataOrientation, cameraMirrored);
		
		
		
		
		int imagesAmount = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
				"amountofcapturedframes" + sessionID));
		if(imagesAmount == 0)
			imagesAmount = 1;
		
		ArrayList<Integer>	mYUVBufferList = GroupShotCore.getInstance().getYUVBufferList();
		thumbnails.clear();
		int heightPixels = ApplicationScreen.getAppResources().getDisplayMetrics().heightPixels;
		for (int i = 1; i <= imagesAmount; i++)
		{
			thumbnails
					.add(Bitmap.createScaledBitmap(ImageConversion.decodeYUVfromBuffer(
							mYUVBufferList.get(i - 1), imageWidth, imageHeight), heightPixels
							/ imagesAmount, (int) (imageHeight * (((float)heightPixels / imagesAmount) / imageWidth)),
							false));
		}
	}

	/************************************************
	 * POST PROCESSING
	 ************************************************/
	public boolean isPostProcessingNeeded()
	{
		return true;
	}

	public void onStartPostProcessing()
	{
		LayoutInflater inflator = ApplicationScreen.instance.getLayoutInflater();
		postProcessingView = inflator.inflate(R.layout.plugin_processing_groupshot_postprocessing, null, false);

		mResultImageView = ((ImageView) postProcessingView.findViewById(R.id.groupshotImageHolder));

		mResultImageView.setImageBitmap(GroupShotCore.getInstance().getPreviewBitmap());

		mInfoTextVeiw = ((TextView) postProcessingView.findViewById(R.id.groupshotTextView));
		mInfoTextVeiw.setText("Loading image ...");

		mHandler.sendEmptyMessage(MSG_END_OF_LOADING);
	}

	private void setupImageSelector()
	{
		sequenceView = ((HorizontalGalleryView) postProcessingView.findViewById(R.id.seqView));
		final Bitmap[] thumbnailsArray = new Bitmap[thumbnails.size()];
		for (int i = 0; i < thumbnailsArray.length; i++)
		{
			Bitmap bmp = thumbnails.get(i);
			Matrix matrix = new Matrix();
			if(iMatrixRotation != 0)
				matrix.postRotate(iMatrixRotation);
			//Workaround for Nexus5x, image is flipped because of sensor orientation
//			if(CameraController.isNexus5x)
//				matrix.postRotate(mCameraMirrored ? ((mDisplayOrientation == 0 || mDisplayOrientation == 180) ? 270
//						: 90)
//						: 270);
//			else
//				matrix.postRotate(mCameraMirrored ? ((mDisplayOrientation == 0 || mDisplayOrientation == 180) ? 270
//						: 90)
//						: 90);	
			Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
			thumbnailsArray[i] = rotated;
		}
		sequenceView.setContent(thumbnailsArray, this);
		LayoutParams lp = (LayoutParams) sequenceView.getLayoutParams();
		lp.height = thumbnailsArray[0].getHeight();
		sequenceView.setLayoutParams(lp);
//		mGallery = (Gallery) postProcessingView.findViewById(R.id.groupshotGallery);
//		mGallery.setAdapter(mImageAdapter);
//		mGallery.setOnItemClickListener(new AdapterView.OnItemClickListener()
//		{
//			public void onItemClick(AdapterView<?> parent, View v, int position, long id)
//			{
//				mImageAdapter.setCurrentSeleted(position);
//				mImageAdapter.notifyDataSetChanged();
//				mGallery.setVisibility(Gallery.INVISIBLE);
//				mBaseFrame = position;
//				GroupShotCore.getInstance().setBaseFrame(mBaseFrame);
//				new Thread(new Runnable()
//				{
//					public void run()
//					{
//						mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_VISIBLE);
//						GroupShotCore.getInstance().updateBitmap();
//						mHandler.post(new Runnable()
//						{
//							public void run()
//							{
//								if (GroupShotCore.getInstance().getPreviewBitmap() != null)
//								{
//									mResultImageView.setImageBitmap(GroupShotCore.getInstance().getPreviewBitmap());
//								}
//							}
//						});
//
//						mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_INVISIBLE);
//					}
//				}).start();
//			}
//		});
//
//		mGallery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
//		{
//			@Override
//			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
//			{
//			}
//
//			@Override
//			public void onNothingSelected(AdapterView<?> arg0)
//			{
//			}
//		});
		return;
	}
	
	@Override
	public void onSequenceChanged(int position)
	{
		mBaseFrame = position;
		GroupShotCore.getInstance().setBaseFrame(mBaseFrame);
		new Thread(new Runnable()
		{
			public void run()
			{
				mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_VISIBLE);
				GroupShotCore.getInstance().updateBitmap();
				mHandler.post(new Runnable()
				{
					public void run()
					{
						if (GroupShotCore.getInstance().getPreviewBitmap() != null)
						{
							mResultImageView.setImageBitmap(GroupShotCore.getInstance().getPreviewBitmap());
						}
					}
				});

				mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_INVISIBLE);
			}
		}).start();
//		sequenceView.setEnabled(false);
//
//		ProcessingTask task = new ProcessingTask();
//		task.idxInput = idx;
//		task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);		
	}

	private void setupImageView()
	{
		GroupShotCore.getInstance().updateBitmap();
		Bitmap mPreviewBitmap = GroupShotCore.getInstance().getPreviewBitmap();
		if (mPreviewBitmap != null)
		{
			mResultImageView.setImageBitmap(mPreviewBitmap);
		}

		mResultImageView.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(final View v, final MotionEvent event)
			{
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					if (mFinishing || mChangingFace)
						return true;
					if (GroupShotCore.getInstance().eventContainsFace(event.getX(), event.getY(), v))
					{
						mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_VISIBLE);
						new Thread(new Runnable()
						{
							public void run()
							{
								synchronized (syncObject)
								{
									mChangingFace = true;
									GroupShotCore.getInstance().updateBitmap();

									// Update screen
									mHandler.post(new Runnable()
									{
										public void run()
										{
											if (GroupShotCore.getInstance().getPreviewBitmap() != null)
											{
												mResultImageView.setImageBitmap(GroupShotCore.getInstance()
														.getPreviewBitmap());
											}
										}
									});
									mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_INVISIBLE);
									mChangingFace = false;
								}
							}
						}).start();
					}
				}
				return false;
			}
		});
	}

	private void setupProgress()
	{
		mProgressBar = (ProgressBar) postProcessingView.findViewById(R.id.groupshotProgressBar);
		mProgressBar.setVisibility(View.INVISIBLE);
	}

	public void setupSaveButton()
	{
		mSaveButton = (Button) postProcessingView.findViewById(R.id.groupshotSaveButton);
		mSaveButton.setOnClickListener(this);
		mSaveButton.setRotation(mDisplayOrientation);
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		switch (msg.what)
		{
		case MSG_END_OF_LOADING:
			setupImageView();
			setupImageSelector();
			setupSaveButton();
			setupProgress();
			mInfoTextVeiw.setVisibility(TextView.INVISIBLE);
			postProcessingRun = true;
			break;
		case MSG_PROGRESS_BAR_INVISIBLE:
			mProgressBar.setVisibility(View.INVISIBLE);
			break;
		case MSG_PROGRESS_BAR_VISIBLE:
			mProgressBar.setVisibility(View.VISIBLE);
			break;
//		case MSG_SELECTOR_VISIBLE:
//			mGallery.setVisibility(View.VISIBLE);
//			break;
//		case MSG_SELECTOR_INVISIBLE:
//			mGallery.setVisibility(View.GONE);
//			break;
		case MSG_LEAVING:
			ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_POSTPROCESSING_FINISHED);
			GroupShotCore.getInstance().release();

			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST,
					ApplicationInterface.MSG_CONTROL_UNLOCKED);

			ApplicationScreen.getGUIManager().lockControls = false;

			postProcessingRun = false;
			return false;
		default:
			return true;
		}
		return true;
	}

	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& ApplicationScreen.instance.findViewById(R.id.postprocessingLayout).getVisibility() == View.VISIBLE)
		{
			if (mFinishing || mChangingFace)
				return true;
			mFinishing = true;
			mHandler.sendEmptyMessage(MSG_LEAVING);
			return true;
		}

		return false;
	}

	@Override
	public void onClick(View v)
	{
		if (v == mSaveButton)
		{
			if (mFinishing || mChangingFace)
				return;
			mFinishing = true;
			savePicture(ApplicationScreen.getMainContext());

			mHandler.sendEmptyMessage(MSG_LEAVING);
		}
	}

	@Override
	public void onOrientationChanged(int orientation)
	{
		if (orientation != mDisplayOrientation)
		{
			mDisplayOrientation = (orientation == 0 || orientation == 180) ? orientation + 90 : orientation - 90;
			if (postProcessingRun)
				mSaveButton.setRotation(mDisplayOrientation);
		}
	}

	public void savePicture(Context context)
	{
		byte[] result = GroupShotCore.getInstance().processingSaveData();
		if (result == null)
		{
			Log.e("GroupShot", "Exception occured in processingSaveData. Picture not saved.");
			return;
		}

		int frame_len = result.length;
		int frame = SwapHeap.SwapToHeap(result);
		PluginManager.getInstance().addToSharedMem("resultframeformat1" + sessionID, "jpeg");
		PluginManager.getInstance().addToSharedMem("resultframe1" + sessionID, String.valueOf(frame));
		PluginManager.getInstance().addToSharedMem("resultframelen1" + sessionID, String.valueOf(frame_len));

		// Nexus 6 has a original front camera sensor orientation, we have to
		// manage it
		//PluginManager.getInstance().addToSharedMem("resultframeorientation1" + sessionID,
		//		String.valueOf(((CameraController.isFlippedSensorDevice() && mCameraMirrored) ? 180 : 0)));
		//PluginManager.getInstance().addToSharedMem("resultframemirrored1" + sessionID, String.valueOf(mCameraMirrored));

		PluginManager.getInstance().addToSharedMem("amountofresultframes" + sessionID, String.valueOf(1));
		PluginManager.getInstance().addToSharedMem("sessionID", String.valueOf(sessionID));
	}
}
