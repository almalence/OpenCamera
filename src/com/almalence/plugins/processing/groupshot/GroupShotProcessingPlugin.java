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

/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

import com.almalence.util.ImageConversion;
import com.almalence.util.Size;

/***
 * Implements group shot processing
 ***/
@SuppressWarnings("deprecation")
public class GroupShotProcessingPlugin implements Handler.Callback, OnClickListener
{
	private View						postProcessingView;

	private long						sessionID					= 0;

	private static final int			MSG_PROGRESS_BAR_INVISIBLE	= 1;
	private static final int			MSG_PROGRESS_BAR_VISIBLE	= 2;
	private static final int			MSG_LEAVING					= 3;
	private static final int			MSG_END_OF_LOADING			= 4;
	private static final int			MSG_SELECTOR_VISIBLE		= 5;
	private static final int			MSG_SELECTOR_INVISIBLE		= 6;

	static final int					img2lay						= 8;					// 16
																							// //
																							// image-to-layout
																							// subsampling
																							// factor

	private static int					nFrames;											// number
																							// of
																							// input
																							// images
	private static int					imgWidthFD;
	private static int					imgHeightFD;

	private static int					previewBmpRealWidth;
	private static int					previewBmpRealHeight;

	static Bitmap						PreviewBmpInitial;
	static Bitmap						PreviewBmp;
	private int							mDisplayWidth;
	private int							mDisplayHeight;

	private int							mLayoutOrientationCurrent;
	private int							mDisplayOrientationCurrent;
	private int							mDisplayOrientationOnStartProcessing;
	private boolean						mCameraMirrored;

	static long							SaveTimeSt, SaveTimeEn;
	static long							JpegTimeSt, JpegTimeEn;
	static long							Prev1TimeSt, Prev1TimeEn;
	static long							Prev2TimeSt, Prev2TimeEn;
	static long							ProcTimeSt, ProcTimeEn;

	static int							OutNV21						= 0;

	static int[]						mPixelsforPreview			= null;

	static int							mBaseFrame					= 0;					// temporary

	static int[]						crop						= new int[5];			// crop
																							// parameters
																							// and
																							// base
																							// image
																							// are
																							// stored
																							// here

	private ImageView					mImgView;
	private Button						mSaveButton;

	private int[][]						mChosenFace;

	private final Handler				mHandler					= new Handler(this);

	private ProgressBar					mProgressBar;
	private Gallery						mGallery;
	private TextView					textVeiw;

	private Seamless					mSeamless;

	private ImageAdapter				mImageAdapter;

	/*
	 * Group shot testing start
	 */
	private static ArrayList<Integer>	mYUVBufferList;

	public static void setmYUVBufferList(ArrayList<Integer> mYUVBufferList)
	{
		GroupShotProcessingPlugin.mYUVBufferList = mYUVBufferList;
	}

	ArrayList<ArrayList<Rect>>	mFaceList;

	private static int			mFrameCount				= 0;

	private static final int	MAX_FACE_DETECTED		= 20;
	private static final float	FACE_CONFIDENCE_LEVEL	= 0.4f;

	private final Object		syncObject				= new Object();

	private boolean				postProcessingRun		= false;

	// indicates that no more user interaction needed
	private boolean				finishing				= false;
	private boolean				changingFace			= false;

	public GroupShotProcessingPlugin()
	{
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
		finishing = false;
		changingFace = false;
		Message msg = new Message();
		msg.what = PluginManager.MSG_PROCESSING_BLOCK_UI;
		MainScreen.getMessageHandler().sendMessage(msg);

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, 
				PluginManager.MSG_CONTROL_LOCKED);

		MainScreen.getGUIManager().lockControls = true;

		sessionID = SessionID;

		PluginManager.getInstance().addToSharedMem("modeSaveName" + sessionID,
				PluginManager.getInstance().getActiveMode().modeSaveName);

		Display display = ((WindowManager) MainScreen.getInstance().getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		mDisplayWidth = display.getHeight();
		mDisplayHeight = display.getWidth();

		mDisplayOrientationOnStartProcessing = Integer.valueOf(PluginManager.getInstance().getFromSharedMem("frameorientation1" + sessionID));
		mDisplayOrientationCurrent = MainScreen.getGUIManager().getDisplayOrientation();
		int orientation = MainScreen.getGUIManager().getLayoutOrientation();
//		Log.d("GroupShot", "onStartProcessing layout orientation: " + orientation);
		mLayoutOrientationCurrent = (orientation == 0 || orientation == 180) ? orientation : (orientation + 180) % 360;
		mCameraMirrored = CameraController.isFrontCamera();

		CameraController.Size imageSize = CameraController.getCameraImageSize();

		int iImageWidth = imageSize.getWidth();
		int iImageHeight = imageSize.getHeight();

		if (mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270)
		{
			imgWidthFD = Seamless.getInstance().getWidthForFaceDetection(iImageHeight,
					iImageWidth);
			imgHeightFD = Seamless.getInstance().getHeightForFaceDetection(iImageHeight,
					iImageWidth);
		} else
		{
			imgWidthFD = Seamless.getInstance().getWidthForFaceDetection(iImageWidth,
					iImageHeight);
			imgHeightFD = Seamless.getInstance().getHeightForFaceDetection(iImageWidth,
					iImageHeight);
		}

		try
		{
			int imagesAmount = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
					"amountofcapturedframes" + sessionID));

			if (imagesAmount == 0)
				imagesAmount = 1;

			nFrames = imagesAmount;

			mFrameCount = mYUVBufferList.size();
			if(PreviewBmp != null)
			{
				PreviewBmp.recycle();
				PreviewBmp = null;
			}
			PreviewBmp = ImageConversion.decodeYUVfromBuffer(mYUVBufferList.get(0), iImageWidth, iImageHeight);

			if (mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270)
			{
				Matrix matrix = new Matrix();
				matrix.postRotate(mCameraMirrored ? (mDisplayOrientationOnStartProcessing + 180) % 360
						: mDisplayOrientationOnStartProcessing);
				Bitmap rotatedBitmap = Bitmap.createBitmap(PreviewBmp, 0, 0, PreviewBmp.getWidth(), PreviewBmp.getHeight(),
						matrix, true);
				
				if (rotatedBitmap != PreviewBmp) {
					PreviewBmp.recycle();
					PreviewBmp= rotatedBitmap;
				}
			}

			if ((mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180)
					&& mCameraMirrored)
			{
				Matrix matrix = new Matrix();
				matrix.postRotate((mDisplayOrientationOnStartProcessing + 180) % 360);
				PreviewBmp = Bitmap.createBitmap(PreviewBmp, 0, 0, PreviewBmp.getWidth(), PreviewBmp.getHeight(),
						matrix, true);
				
				Bitmap rotatedBitmap = Bitmap.createBitmap(PreviewBmp, 0, 0, PreviewBmp.getWidth(), PreviewBmp.getHeight(),
						matrix, true);
				if (rotatedBitmap != PreviewBmp) {
					PreviewBmp.recycle();
					PreviewBmp= rotatedBitmap;
				}
			}

			previewBmpRealWidth = PreviewBmp.getWidth();
			previewBmpRealHeight = PreviewBmp.getHeight();

			loadingSeamless();

			int max = 0;
			for (int i = 0; i < mFaceList.size(); i++)
			{
				if (max < mFaceList.get(i).size())
				{
					max = mFaceList.get(i).size();
				}
			}
			mChosenFace = new int[mFaceList.size()][max];
			for (int i = 0; i < mFaceList.size(); i++)
			{
				Arrays.fill(mChosenFace[i], i);
			}
		} catch (Exception e)
		{
			// make notifier in main thread
			e.printStackTrace();
		}

		PluginManager.getInstance().addToSharedMem("resultfromshared" + sessionID, "false");
		PluginManager.getInstance().addToSharedMem("amountofresultframes" + sessionID, "1");

		PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID, String.valueOf(iImageWidth));
		PluginManager.getInstance().addToSharedMem("saveImageHeight" + sessionID, String.valueOf(iImageHeight));
		
		PreviewBmp.recycle();
		PreviewBmp = null;
	}

	private void getFaceRects()
	{
		mFaceList = new ArrayList<ArrayList<Rect>>(mFrameCount);

		Face[] mFaces = new Face[MAX_FACE_DETECTED];
		for (int i = 0; i < MAX_FACE_DETECTED; ++i)
			mFaces[i] = new Face();

		for (int index = 0; index < mFrameCount; index++)
		{
			int numberOfFacesDetected = AlmaShotSeamless.GetFaces(index, mFaces);

			int Scale;
			if (mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270)
				Scale = CameraController.getCameraImageSize().getHeight() / imgWidthFD;
			else
				Scale = CameraController.getCameraImageSize().getWidth() / imgWidthFD;

			ArrayList<Rect> rect = new ArrayList<Rect>();
			for (int i = 0; i < numberOfFacesDetected; i++)
			{
				Face face = mFaces[i];
				PointF myMidPoint = new PointF();
				face.getMidPoint(myMidPoint);
				float myEyesDistance = face.eyesDistance();
				if (face.confidence() > FACE_CONFIDENCE_LEVEL)
				{
					Rect faceRect = new Rect((int) (myMidPoint.x - myEyesDistance) * Scale,
							(int) (myMidPoint.y - myEyesDistance) * Scale, (int) (myMidPoint.x + myEyesDistance)
									* Scale, (int) (myMidPoint.y + myEyesDistance) * Scale);
					rect.add(faceRect);
				}
			}

			mFaceList.add(rect);
		}
	}

	private void loadingSeamless()
	{
		mSeamless = Seamless.getInstance();
		Size preview = new Size(PreviewBmp.getWidth(), PreviewBmp.getHeight());
		// correctness of w/h here depends on orientation while taking image,
		// only product of inputSize is used later - this is why code still
		// works
		CameraController.Size imageSize = CameraController.getCameraImageSize();
		Size inputSize = new Size(imageSize.getWidth(), imageSize.getHeight());
		Size fdSize = new Size(imgWidthFD, imgHeightFD);

		try
		{
			boolean needRotation = mDisplayOrientationOnStartProcessing != 0;
			// Note: DecodeJpegs doing free() to jpeg data!
			int rotation = mCameraMirrored
					&& (mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270) ? (mDisplayOrientationOnStartProcessing + 180) % 360
					: mDisplayOrientationOnStartProcessing;
			mSeamless.addYUVInputFrames(mYUVBufferList, inputSize, fdSize, needRotation, false, rotation);
			getFaceRects();

			sortFaceList();

			mSeamless.initialize(mBaseFrame, mFaceList, preview);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return;
	}

	private void sortFaceList()
	{
		ArrayList<ArrayList<Rect>> newFaceList = new ArrayList<ArrayList<Rect>>(mFrameCount);
		for (int i = 0; i < mFrameCount; i++)
		{
			newFaceList.add(new ArrayList<Rect>());
		}

		while (isAnyFaceInList())
		{
			ArrayList<Integer> bestCandidateList = populateBestCandidate();

			if (bestCandidateList.size() != 0)
				for (int frameIndex = 0; frameIndex < newFaceList.size(); frameIndex++)
				{
					int bestFaceIndex = bestCandidateList.get(frameIndex);
					if (bestFaceIndex == -1)
						continue;

					newFaceList.get(frameIndex).add(mFaceList.get(frameIndex).remove(bestFaceIndex));
				}
		}

		mFaceList.clear();
		for (ArrayList<Rect> faces : newFaceList)
			mFaceList.add(faces);
	}

	private boolean isAnyFaceInList()
	{
		for (ArrayList<Rect> faceList : mFaceList)
		{
			if (faceList.size() > 0)
				return true;
		}

		return false;
	}

	private ArrayList<Integer> populateBestCandidate()
	{
		int baseFaceX, baseFaceY, baseIndex, presenceNumber;
		float allowedDistance, distance, maxDistance, minDistance;

		int bestPresenceNumber = 0;
		float bestMaxDistance = -1;

		int candidateIndex, currIndex;
		ArrayList<Integer> candidateList = new ArrayList<Integer>(mFrameCount);
		ArrayList<Integer> bestCandidateList = new ArrayList<Integer>(mFrameCount);

		int i = 0;
		for (ArrayList<Rect> faceFrame : mFaceList)
		{
			baseIndex = 0;
			if (faceFrame.size() > 0)
				for (Rect baseFace : faceFrame)
				{
					candidateList.clear();
					baseFaceX = baseFace.centerX();
					baseFaceY = baseFace.centerY();
					allowedDistance = getRadius(baseFace) * 0.75f;
					presenceNumber = mFaceList.size();

					maxDistance = -1;
					for (int j = 0; j < mFaceList.size(); j++)
					{
						candidateIndex = -1;
						minDistance = -1;

						if (j == i || mFaceList.get(j).size() == 0)
						{
							if (j == i)
								candidateIndex = baseIndex;

							candidateList.add(candidateIndex);
							presenceNumber--;
							continue;
						}

						currIndex = 0;
						for (Rect candidateFace : mFaceList.get(j))
						{
							distance = getDistance(baseFaceX, baseFaceY, candidateFace.centerX(),
									candidateFace.centerY());
							if (distance < allowedDistance && (distance < minDistance || minDistance == -1))
							{
								minDistance = distance;
								candidateIndex = currIndex;
							}
							currIndex++;
						}

						if (minDistance == -1)
							presenceNumber--;

						if (minDistance > maxDistance)
							maxDistance = minDistance;

						candidateList.add(candidateIndex);
					}

					if (presenceNumber > bestPresenceNumber)
					{
						bestPresenceNumber = presenceNumber;
						bestMaxDistance = maxDistance;

						bestCandidateList.clear();
						for (Integer index : candidateList)
							bestCandidateList.add(index);
					} else if (presenceNumber == bestPresenceNumber
							&& (maxDistance < bestMaxDistance || bestMaxDistance == -1))
					{
						bestMaxDistance = maxDistance;

						bestCandidateList.clear();
						for (Integer index : candidateList)
							bestCandidateList.add(index);
					}

					baseIndex++;
				}
			i++;
		}

		return bestCandidateList;
	}

	private boolean checkDistance(float radius, float x, float y, int centerX, int centerY)
	{
		float distance = getSquareOfDistance((int) x, (int) y, centerX, centerY);
		if (distance < (radius * radius))
		{
			return true;
		}
		return false;
	}

	private boolean checkFaceDistance(float radius, float x, float y, int centerX, int centerY)
	{
		float distance = getDistance((int) x, (int) y, centerX, centerY);
		if (distance < (radius * 0.75f))
		{
			return true;
		}
		return false;
	}

	private float getRadius(Rect rect)
	{
		return (rect.width() + rect.height()) / 2;
	}

	private int getSquareOfDistance(int x, int y, int x0, int y0)
	{
		return (x - x0) * (x - x0) + (y - y0) * (y - y0);
	}

	private int getDistance(int x, int y, int x0, int y0)
	{
		return (int) Math.round(Math.sqrt((x - x0) * (x - x0) + (y - y0) * (y - y0)));
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
		LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
		postProcessingView = inflator.inflate(R.layout.plugin_processing_groupshot_postprocessing, null, false);

		mImgView = ((ImageView) postProcessingView.findViewById(R.id.groupshotImageHolder));
		if(PreviewBmp != null)
		{
			PreviewBmp.recycle();
			PreviewBmp = null;
		}
		
		CameraController.Size imageSize = CameraController.getCameraImageSize();
		PreviewBmp = ImageConversion.decodeYUVfromBuffer(mYUVBufferList.get(0), imageSize.getWidth(),
				imageSize.getHeight());
		if (PreviewBmp != null)
		{
			Matrix matrix = new Matrix();
			matrix.postRotate(90);

			Bitmap rotated = Bitmap.createBitmap(PreviewBmp, 0, 0, PreviewBmp.getWidth(), PreviewBmp.getHeight(),
					matrix, true);
			PreviewBmpInitial = Bitmap.createBitmap(rotated);
			
			if (PreviewBmpInitial != rotated) {
				rotated.recycle();
			}
		}

		mImgView.setImageBitmap(PreviewBmpInitial);
		mImgView.setRotation(mCameraMirrored ? ((mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180) ? 0
				: 180)
				: 0);

		textVeiw = ((TextView) postProcessingView.findViewById(R.id.groupshotTextView));
		textVeiw.setText("Loading image ...");

		mHandler.sendEmptyMessage(MSG_END_OF_LOADING);

	}

	private void setupImageSelector()
	{
		mImageAdapter = new ImageAdapter(MainScreen.getMainContext(), mYUVBufferList,
				mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180,
				mCameraMirrored, true);
		mGallery = (Gallery) postProcessingView.findViewById(R.id.groupshotGallery);
		mGallery.setAdapter(mImageAdapter);
		mGallery.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View v, int position, long id)
			{
				mImageAdapter.setCurrentSeleted(position);
				mImageAdapter.notifyDataSetChanged();
				mGallery.setVisibility(Gallery.INVISIBLE);
				mBaseFrame = position;
				mSeamless.setBaseFrame(mBaseFrame);
				new Thread(new Runnable()
				{
					public void run()
					{
						mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_VISIBLE);
						updateBitmap();
						mHandler.post(new Runnable()
						{
							public void run()
							{
								if (PreviewBmp != null)
								{
									mImgView.setImageBitmap(PreviewBmp);
								}
							}
						});

						// Probably this should be called from mSeamless ?
						mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_INVISIBLE);
					}
				}).start();
			}
		});

		mGallery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
			}
		});
		return;
	}

	public Bitmap decodeFullJPEGfromBuffer(byte[] data)
	{
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Config.ARGB_8888;
		options.inJustDecodeBounds = false;
		options.inSampleSize = 1;
		return BitmapFactory.decodeByteArray(data, 0, data.length, options);
	}

	private void drawFaceRectOnBitmap(Bitmap bitmap, ArrayList<Rect> faceRect)
	{
		float ratiox;
		float ratioy;
		float bWidth = bitmap.getWidth();
		float bHeight = bitmap.getHeight();
		CameraController.Size imageSize = CameraController.getCameraImageSize();
		if (mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270)
		{
			ratiox = (float) imageSize.getHeight() / (float) bWidth;
			ratioy = (float) imageSize.getWidth() / (float) bHeight;
		} else
		{
			ratiox = (float) imageSize.getWidth() / (float) bWidth;
			ratioy = (float) imageSize.getHeight() / (float) bHeight;
		}

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(0xFF00AAEA);
		paint.setStrokeWidth(5);
		paint.setPathEffect(new DashPathEffect(new float[] { 5, 5 }, 0));

		Canvas c = new Canvas(bitmap);

		for (Rect rect : faceRect)
		{
			float radius = getRadius(rect);
			c.drawCircle(rect.centerX() / ratiox, rect.centerY() / ratioy, radius / ((ratiox + ratioy) / 2), paint);
		}

		return;
	}

	private boolean eventContainsFace(float x, float y, ArrayList<Rect> faceRect, View v)
	{
		float ratiox;
		float ratioy;

		CameraController.Size imageSize = CameraController.getCameraImageSize();
		if (mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270)
		{
			ratiox = (float) imageSize.getHeight() / (float) previewBmpRealWidth;
			ratioy = (float) imageSize.getWidth() / (float) previewBmpRealHeight;
		} else
		{
			ratiox = (float) imageSize.getWidth() / (float) previewBmpRealWidth;
			ratioy = (float) imageSize.getHeight() / (float) previewBmpRealHeight;
		}

		if (mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180)
		{
			float x_tmp = x;
			float y_tmp = y;
			x = mDisplayOrientationOnStartProcessing == 180 ? mDisplayWidth - 1 - y_tmp : y_tmp;
			y = mDisplayOrientationOnStartProcessing == 180 ? x_tmp : mDisplayHeight - 1 - x_tmp;
//			Log.d("GroupShot", "Correction 1 coordinates x = " + x + "  y = " + y);
		} else if (!mCameraMirrored && mDisplayOrientationOnStartProcessing == 270)
		{
			x = mDisplayHeight - x;
			y = mDisplayWidth - y;
//			Log.d("GroupShot", "Correction 1 coordinates x = " + x + "  y = " + y);
		} else if (mCameraMirrored && mDisplayOrientationOnStartProcessing == 90)
		{
			x = mDisplayHeight - x;
			y = mDisplayWidth - y;
//			Log.d("GroupShot", "Correction 1 coordinates x = " + x + "  y = " + y);
		}
		// Have to correct touch coordinates coz ImageView centered on the
		// screen
		// and it's coordinate system not aligned with screen coordinate system.
		if ((mDisplayWidth > v.getHeight() || mDisplayHeight > v.getWidth()))
		{
			x = x
					- (((mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270) ? mDisplayHeight
							: mDisplayWidth) - previewBmpRealWidth) / 2;
			if (mCameraMirrored)
			{
				y = y
						- (((mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270) ? mDisplayWidth
								: mDisplayHeight) - previewBmpRealHeight);
			} else
			{
				y = y
						- (((mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270) ? mDisplayWidth
								: mDisplayHeight) - previewBmpRealHeight) / 2;
			}
//			Log.d("GroupShot", "Correction 2 coordinates x = " + x + "  y = " + y);
		}

		int i = 0;
		for (Rect rect : faceRect)
		{
			Rect newRect = new Rect((int) (rect.left / ratiox), (int) (rect.top / ratioy), (int) (rect.right / ratiox),
					(int) (rect.bottom / ratioy));
			float radius = getRadius(newRect);
			if (checkDistance(radius, x, y, newRect.centerX(), newRect.centerY()))
			{
				int newFrameIndex = mChosenFace[mBaseFrame][i] + 1;
				while (!checkFaceIsSuitable(newFrameIndex, i, radius, ratiox, ratioy, newRect))
					newFrameIndex++;

				changingFace = true;
				mChosenFace[mBaseFrame][i] = newFrameIndex;
				mSeamless.changeFace(i, mChosenFace[mBaseFrame][i] % nFrames);
				changingFace = false;
				return true;
			}
			i++;
		}
		return false;
	}

	private boolean checkFaceIsSuitable(int frameIndex, int faceIndex, float faceRadius, float ratioX, float ratioY,
			Rect currFace)
	{
		if (mFaceList.get(frameIndex % nFrames).size() <= faceIndex)
			return false;
		else
		{
			Rect candidateRect = mFaceList.get(frameIndex % nFrames).get(faceIndex);
			Rect newRect = new Rect((int) (candidateRect.left / ratioX), (int) (candidateRect.top / ratioY),
					(int) (candidateRect.right / ratioX), (int) (candidateRect.bottom / ratioY));
			return checkFaceDistance(faceRadius, newRect.centerX(), newRect.centerY(), currFace.centerX(),
					currFace.centerY());
		}
	}

	private void setupImageView()
	{
//		PreviewBmp = PreviewBmp.copy(Config.ARGB_8888, true);

		if (PreviewBmp != null)
		{
			updateBitmap();
			mImgView.setImageBitmap(PreviewBmp);
			mImgView.setRotation(mCameraMirrored ? ((mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180) ? 0
					: 180)
					: 0);
		}

		mImgView.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(final View v, final MotionEvent event)
			{
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					if (finishing)
						return true;
					if (mGallery.getVisibility() == Gallery.VISIBLE)
					{
						mGallery.setVisibility(Gallery.INVISIBLE);
						return false;
					}
					mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_VISIBLE);
					new Thread(new Runnable()
					{
						public void run()
						{
							synchronized (syncObject)
							{
								if (eventContainsFace(event.getX(), event.getY(), mFaceList.get(mBaseFrame), v))
								{
									updateBitmap();
									// Update screen
									mHandler.post(new Runnable()
									{
										public void run()
										{
											if (PreviewBmp != null)
											{
												mImgView.setImageBitmap(PreviewBmp);
												mImgView.setRotation(mCameraMirrored ? ((mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180) ? 0
														: 180)
														: 0);
											}
										}
									});
								} else
								{
									mHandler.sendEmptyMessage(MSG_SELECTOR_VISIBLE);
								}
								mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_INVISIBLE);
							}
						}
					}).start();
				}
				return false;
			}
		});
	}

	private void setupProgress()
	{
		mProgressBar = (ProgressBar) postProcessingView.findViewById(R.id.groupshotProgressBar);
		mProgressBar.setVisibility(View.INVISIBLE);
		return;
	}

	public void setupSaveButton()
	{
		// put save button on screen
		mSaveButton = new Button(MainScreen.getInstance());
		mSaveButton.setBackgroundResource(R.drawable.button_save_background);
		mSaveButton.setOnClickListener(this);
		LayoutParams saveLayoutParams = new LayoutParams(
				(int) (MainScreen.getMainContext().getResources().getDimension(R.dimen.postprocessing_savebutton_size)),
				(int) (MainScreen.getMainContext().getResources().getDimension(R.dimen.postprocessing_savebutton_size)));
		saveLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		saveLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		
		float density = MainScreen.getAppResources().getDisplayMetrics().density;
		saveLayoutParams.setMargins((int) (density * 8), (int) (density * 8), 0, 0);
		((RelativeLayout) postProcessingView.findViewById(R.id.groupshotLayout)).addView(mSaveButton, saveLayoutParams);
		mSaveButton.setRotation(mLayoutOrientationCurrent);
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
			textVeiw.setVisibility(TextView.INVISIBLE);
			postProcessingRun = true;
			break;
		case MSG_PROGRESS_BAR_INVISIBLE:
			mProgressBar.setVisibility(View.INVISIBLE);
			break;
		case MSG_PROGRESS_BAR_VISIBLE:
			mProgressBar.setVisibility(View.VISIBLE);
			break;
		case MSG_SELECTOR_VISIBLE:
			mGallery.setVisibility(View.VISIBLE);
			break;
		case MSG_SELECTOR_INVISIBLE:
			mGallery.setVisibility(View.GONE);
			break;
		case MSG_LEAVING:
			MainScreen.getMessageHandler().sendEmptyMessage(PluginManager.MSG_POSTPROCESSING_FINISHED);
			if (mSeamless != null)
				mSeamless.release();
			for(int yuv: mYUVBufferList)
			{
				SwapHeap.FreeFromHeap(yuv);
			}
			mYUVBufferList.clear();

			PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, 
					PluginManager.MSG_CONTROL_UNLOCKED);

			MainScreen.getGUIManager().lockControls = false;

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
				&& MainScreen.getInstance().findViewById(R.id.postprocessingLayout).getVisibility() == View.VISIBLE)
		{
			if (finishing || changingFace)
				return true;
			finishing = true;
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
			if (finishing || changingFace)
				return;
			finishing = true;
			savePicture(MainScreen.getMainContext());

			mHandler.sendEmptyMessage(MSG_LEAVING);
		}
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

	public synchronized void updateBitmap()
	{
		PreviewBmp = mSeamless.getPreviewBitmap();
//		Log.d("GroupShot", "updateBitmap. PreviewBmp WxH: " + PreviewBmp.getWidth() + " x " + PreviewBmp.getHeight());
//		PreviewBmp = PreviewBmp.copy(Config.ARGB_8888, true);
		drawFaceRectOnBitmap(PreviewBmp, mFaceList.get(mBaseFrame));
		if (mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180)
		{
			Matrix matrix = new Matrix();
			matrix.postRotate((mDisplayOrientationOnStartProcessing + 90 % 360));
			PreviewBmp = Bitmap.createBitmap(PreviewBmp, 0, 0, PreviewBmp.getWidth(), PreviewBmp.getHeight(), matrix,
					true);
		} else if (!mCameraMirrored && mDisplayOrientationOnStartProcessing == 270)
		{
			Matrix matrix = new Matrix();
			matrix.postRotate(180);
			PreviewBmp = Bitmap.createBitmap(PreviewBmp, 0, 0, PreviewBmp.getWidth(), PreviewBmp.getHeight(), matrix,
					true);
		} else if (mCameraMirrored && mDisplayOrientationOnStartProcessing == 90)
		{
			Matrix matrix = new Matrix();
			matrix.postRotate(180);
			PreviewBmp = Bitmap.createBitmap(PreviewBmp, 0, 0, PreviewBmp.getWidth(), PreviewBmp.getHeight(), matrix,
					true);
		}
		return;
	}

	public void savePicture(Context context)
	{
		byte[] result = mSeamless.processingSaveData();
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

		PluginManager.getInstance().addToSharedMem("resultframeorientation1" + sessionID, String.valueOf(0));
		PluginManager.getInstance().addToSharedMem("resultframemirrored1" + sessionID, String.valueOf(mCameraMirrored));

		PluginManager.getInstance().addToSharedMem("amountofresultframes" + sessionID, String.valueOf(1));
		PluginManager.getInstance().addToSharedMem("sessionID", String.valueOf(sessionID));
	}
}
