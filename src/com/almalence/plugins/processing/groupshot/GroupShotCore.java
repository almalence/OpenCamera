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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.almalence.SwapHeap;
import com.almalence.util.ImageConversion;
import com.almalence.util.Util;

/* <!-- +++
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
//<!-- -+-
 import com.almalence.opencam.ApplicationScreen;
 import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

public class GroupShotCore
{
	private final String				TAG							= this.getClass()
																			.getName()
																			.substring(
																					this.getClass().getName()
																							.lastIndexOf(".") + 1);
	private static final int			MAX_FACE_DETECTED			= 20;
	private static final float			FACE_CONFIDENCE_LEVEL		= 0.4f;
	private static final int			IMAGE_TO_LAYOUT				= 8;
	private static final int			MAX_INPUT_FRAME				= 8;
	private static final int			MAX_WIDTH_FOR_FACEDETECTION	= 1280;

	private int							mNumOfFrame					= 0;
	private int							mBaseFrame					= 0;

	// Width and height of input images.
	private int							mImageWidth;
	private int							mImageHeight;

	// Width and height of input images, rotated according to image data
	// orientation.
	private int							mImageWidthRotated;
	private int							mImageHeightRotated;

	// Width and height of preview image, rotated according to image data
	// orientation.
	private int							mPreviewWidthRotated;
	private int							mPreviewHeightRotated;
	
	private int							mPreviewWidthOriginal;
	private int							mPreviewHeightOriginal;

	// Because of speed reasons image is down scaled to find faces. This is
	// width and height of down scaled images.
	private int							mImageWidthFD;
	private int							mImageHeightFD;

	// Width and height of display
	private int							mDisplayWidth;
	private int							mDisplayHeight;

	// Width and height of post-processing layout.
	private int							mLayoutWidth;
	private int							mLayoutHeight;

	// Bitmap used to show on preview.
	private Bitmap						mPreviewBitmap;

	// Buffer bitmap is used to get seamless preview from JNI and put it into
	// mPreviewBitmap.
	// It always has size mPreviewWidthRotated x mPreviewHeightRotated.
	private Bitmap						mBuffer;

	// Transform for preview rotation. It's required to set proper orientation
	// for preview image (as it was during capturing).
	private Matrix						mDeviceRotationTransform	= new Matrix();
	private int							mMatrixRotation				= 0;

	private int[]							ARGBBuffer				= null;
	private int								mOutNV21;
	private int[]							mCrop;
	private ArrayList<ArrayList<Rect>>		mFacesList				= null;
	private ArrayList<ArrayList<Bitmap>>	mFacesBitmapsList		= null;
	private ArrayList<ArrayList<Rect>>		mFacesBitmapsRect		= null;
	private ArrayList<Integer>				mFacesRadiusList		= null;

	// Rotation of image data. If we rotate image on this angle, its orientation will become 0.
	private int							mImageDataOrientation;
	
	// Rotation of device during capturing. We need this to set preview rotation and to transform touch coordinates.
	private int							mDeviceOrientation;
	private boolean						mCameraMirrored;

	private byte[]						mLayoutData;
	private int[][]						mChosenFaces;
	private boolean						mIsBaseFrameChanged			= false;
	private boolean						mIsFacesChanged				= false;

	private ArrayList<Integer>			mYUVBufferList;															// List
																													// of
																													// input
																													// images.

	public void setYUVBufferList(ArrayList<Integer> YUVBufferList)
	{
		this.mYUVBufferList = YUVBufferList;
	}

	public ArrayList<Integer> getYUVBufferList()
	{
		return mYUVBufferList;
	}

	private GroupShotCore()
	{
		super();
	}

	private static GroupShotCore	mInstance;

	public static GroupShotCore getInstance()
	{
		if (mInstance == null)
		{
			mInstance = new GroupShotCore();
		}
		return mInstance;
	}

	// Initialize all required size parameters.
	private void initializeSizeParameters()
	{
		CameraController.Size imageSize = CameraController.getCameraImageSize();
		mImageWidth = imageSize.getWidth();
		mImageHeight = imageSize.getHeight();

		if (mImageDataOrientation != 0 && mImageDataOrientation != 180)
		{
			mImageWidthRotated = mImageHeight;
			mImageHeightRotated = mImageWidth;
		} else
		{
			mImageWidthRotated = mImageWidth;
			mImageHeightRotated = mImageHeight;
		}

		mImageWidthFD = getWidthForFaceDetection(mImageWidth, mImageHeight, mImageDataOrientation);
		mImageHeightFD = getHeightForFaceDetection(mImageWidth, mImageHeight, mImageDataOrientation);

		mLayoutWidth = mImageWidthRotated / IMAGE_TO_LAYOUT;
		mLayoutHeight = mImageHeightRotated / IMAGE_TO_LAYOUT;

		Display display = ((WindowManager) ApplicationScreen.instance.getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		mDisplayWidth = display.getHeight();
		mDisplayHeight = display.getWidth();
	}

	// Initialize parameters for processing.
	public void initializeProcessingParameters(int imageDataOrientation, int deviceOrientation, boolean cameraMirrored, int rotationMatrix)
	{
		mImageDataOrientation = imageDataOrientation;
		mDeviceOrientation = deviceOrientation;
		mCameraMirrored = cameraMirrored;
		mNumOfFrame = mYUVBufferList.size();

		initializeSizeParameters();
		mIsBaseFrameChanged = true;
		
//		mDeviceRotationTransform.postRotate(rotationMatrix);
		mMatrixRotation = rotationMatrix;
		mDeviceRotationTransform.postRotate(-mDeviceOrientation);
//		int defaultRotation = ApplicationScreen.getGUIManager().getMatrixRotationForBitmap(mImageDataOrientation, mDeviceOrientation, mCameraMirrored);
//		int finalRotation = defaultRotation  - mDeviceOrientation;
//		mDeviceRotationTransform.postRotate(0);
	}

	// Detect faces, create and sort list with faces parameters.
	// Important: input images will be copied and copies will be rotated into
	// GroupShot engine.
	// So Preview data and faces coordinates received from GroupShot engine
	// always be rotated to 0 (eyes on top and mouth on bottom).
	public void ProcessFaces()
	{
		try
		{
			// Detect faces.
			detectFaces();

			// Create list of faces coordinates.
			getFaceRects();

			// Sort faces by position (coordinates).
			sortFaceList();

			// Set initial values for chosen faces.
			initializeChosenFaces();

			createPreviewBitmap();
			createLayoutData();
			
			createFacesRadius();
			createFacesBitmaps();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return;
	}

	// Create coordinate rects for all faces for each frame.
	// Faces will be grouped by frame.
	private void getFaceRects()
	{
		mFacesList = new ArrayList<ArrayList<Rect>>(mNumOfFrame);

		// Array of Face objects used as buffer to get parameters of faces for
		// each frame.
		Face[] faces = new Face[MAX_FACE_DETECTED];
		for (int i = 0; i < MAX_FACE_DETECTED; ++i)
			faces[i] = new Face();

		// Scale factor to map faces coordinates from down-scaled images to
		// input images.
		int scale;
		if (mImageDataOrientation == 90 || mImageDataOrientation == 270)
			scale = mImageHeight / mImageWidthFD;
		else
			scale = mImageWidth / mImageWidthFD;

		// Collect info of detected faces for each frame.
		for (int frame = 0; frame < mNumOfFrame; frame++)
		{
			// Get number of detected faces and theirs parameters.
			int numberOfFacesDetected = AlmaShotGroupShot.GetFaces(frame, faces);

			ArrayList<Rect> rect = new ArrayList<Rect>();
			for (int i = 0; i < numberOfFacesDetected; i++)
			{
				Face face = faces[i];
				PointF myMidPoint = new PointF();
				face.getMidPoint(myMidPoint);
				float myEyesDistance = face.eyesDistance();

				// If we sure, that found "face" (feature point) is a real face,
				// then create a rect for it.
				if (face.confidence() > FACE_CONFIDENCE_LEVEL)
				{
					Rect faceRect = new Rect((int) (myMidPoint.x - myEyesDistance) * scale,
							(int) (myMidPoint.y - myEyesDistance) * scale, (int) (myMidPoint.x + myEyesDistance)
									* scale, (int) (myMidPoint.y + myEyesDistance) * scale);
					rect.add(faceRect);
				}
			}

			mFacesList.add(rect);
		}
	}

	// Sort faces list to order them by position on images.
	private void sortFaceList()
	{
		ArrayList<ArrayList<Rect>> newFaceList = new ArrayList<ArrayList<Rect>>(mNumOfFrame);
		for (int i = 0; i < mNumOfFrame; i++)
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

					newFaceList.get(frameIndex).add(mFacesList.get(frameIndex).remove(bestFaceIndex));
				}
		}

		mFacesList.clear();
		for (ArrayList<Rect> faces : newFaceList)
			mFacesList.add(faces);
	}

	private void initializeChosenFaces()
	{
		// Get maximum number of found faces at one frame.
		int max = 0;
		for (int i = 0; i < mFacesList.size(); i++)
		{
			if (max < mFacesList.get(i).size())
			{
				max = mFacesList.get(i).size();
			}
		}

		mChosenFaces = new int[mFacesList.size()][max];
		for (int i = 0; i < mFacesList.size(); i++)
		{
			Arrays.fill(mChosenFaces[i], i);
		}
	}

	private boolean isAnyFaceInList()
	{
		for (ArrayList<Rect> faceList : mFacesList)
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
		ArrayList<Integer> candidateList = new ArrayList<Integer>(mNumOfFrame);
		ArrayList<Integer> bestCandidateList = new ArrayList<Integer>(mNumOfFrame);

		int i = 0;
		for (ArrayList<Rect> faceFrame : mFacesList)
		{
			baseIndex = 0;
			if (faceFrame.size() > 0)
				for (Rect baseFace : faceFrame)
				{
					candidateList.clear();
					baseFaceX = baseFace.centerX();
					baseFaceY = baseFace.centerY();
					allowedDistance = getRadius(baseFace) * 0.75f;
					presenceNumber = mFacesList.size();

					maxDistance = -1;
					for (int j = 0; j < mFacesList.size(); j++)
					{
						candidateIndex = -1;
						minDistance = -1;

						if (j == i || mFacesList.get(j).size() == 0)
						{
							if (j == i)
								candidateIndex = baseIndex;

							candidateList.add(candidateIndex);
							presenceNumber--;
							continue;
						}

						currIndex = 0;
						for (Rect candidateFace : mFacesList.get(j))
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

	private boolean checkFaceDistance(float radius, float x, float y, int centerX, int centerY)
	{
		float distance = getDistance((int) x, (int) y, centerX, centerY);
		if (distance < (radius * 0.75f))
		{
			return true;
		}
		return false;
	}

	/**
	 * @return distance between 2 points (x,y) and (x0,y0).
	 */
	private int getDistance(int x, int y, int x0, int y0)
	{
		return (int) Math.round(Math.sqrt((x - x0) * (x - x0) + (y - y0) * (y - y0)));
	}

	public void detectFaces() throws Exception
	{
		AlmaShotGroupShot.Initialize();

		int[] yuvPtrs = new int[mNumOfFrame]; // Pointers to YUV image data.
		int[] yuvSizes = new int[mNumOfFrame]; // Sizes of each image in bytes.

		int dataSize = mImageWidth * mImageHeight * 3 / 2;
		for (int i = 0; i < mNumOfFrame; i++)
		{
			yuvPtrs[i] = mYUVBufferList.get(i);
			yuvSizes[i] = dataSize;
			if (yuvPtrs[i] == 0)
			{
				Log.d(TAG, "Out of Memory in Native");
				throw new Exception("Out of Memory in Native");
			}
		}

		int error = 0;
//		int rotation = ApplicationScreen.getGUIManager().getMatrixRotationForBitmap(mImageDataOrientation, 0, mCameraMirrored);
		int rotation = (mImageDataOrientation + (mCameraMirrored?(mImageDataOrientation == 90 || mImageDataOrientation == 270? 180 : 0) : 0))%360;
		error = AlmaShotGroupShot.DetectFacesFromYUVs(yuvPtrs, yuvSizes, mNumOfFrame, mImageWidth, mImageHeight,
				mImageWidthFD, mImageHeightFD, false/*mCameraMirrored*/, rotation);

		if (error < 0)
		{
			Log.d(TAG, "Out Of Memory");
			throw new Exception("Out Of Memory");
		} else if (error < MAX_INPUT_FRAME)
		{
			Log.d(TAG, "YUV buffer is wrong in " + error + " frame");
			throw new Exception("Out Of Memory");
		}

		return;
	}

	// Create bitmap from first input image, with the size same or nearest lower
	// as screen size.
	private void createPreviewBitmap()
	{
		if (mPreviewBitmap != null)
		{
			mPreviewBitmap.recycle();
			mPreviewBitmap = null;
		}
		
		mPreviewBitmap = ImageConversion.decodeYUVfromBuffer(mYUVBufferList.get(0), mImageWidth, mImageHeight);
//		mPreviewBitmap = ImageConversion.decodeYUVfromBuffer(AlmaShotGroupShot.getInputFrame(0), mImageWidthRotated, mImageHeightRotated);
		mPreviewWidthOriginal = mPreviewBitmap.getWidth();
		mPreviewHeightOriginal = mPreviewBitmap.getHeight();
		
//		if(mMatrixRotation != 0)
//			mPreviewBitmap = Bitmap.createBitmap(mPreviewBitmap, 0, 0, mPreviewBitmap.getWidth(), mPreviewBitmap.getHeight(),
//					mDeviceRotationTransform, true);

//		int rotation = ApplicationScreen.getGUIManager().getMatrixRotationForBitmap(mImageDataOrientation, mDeviceOrientation, mCameraMirrored);
		if (mMatrixRotation != 0)
		{
			Matrix rotateMatrix = new Matrix();
			rotateMatrix.postRotate(mMatrixRotation);
			Bitmap rotatedBitmap = Bitmap.createBitmap(mPreviewBitmap, 0, 0, mPreviewBitmap.getWidth(),
					mPreviewBitmap.getHeight(), rotateMatrix, true);

			if (rotatedBitmap != mPreviewBitmap)
			{
				mPreviewBitmap.recycle();
				mPreviewBitmap = rotatedBitmap;
			}
		}
		
		mPreviewWidthRotated = mPreviewBitmap.getWidth();
		mPreviewHeightRotated = mPreviewBitmap.getHeight();

////		int rotation = (mImageDataOrientation + (mCameraMirrored?(mImageDataOrientation == 90 || mImageDataOrientation == 270? 180 : 0) : 0))%360;
//		if(mImageDataOrientation == 0 && mImageDataOrientation == 180)
////		if(rotation == 0 && rotation == 180)
//		{
//			mPreviewWidthRotated = mPreviewWidthOriginal;
//			mPreviewHeightRotated = mPreviewHeightOriginal;
//		}
//		else
//		{
//			mPreviewWidthRotated = mPreviewHeightOriginal;
//			mPreviewHeightRotated = mPreviewWidthOriginal;
//		}
		
		

		ARGBBuffer = new int[mPreviewBitmap.getWidth() * mPreviewBitmap.getHeight() * 4];
	}

	public boolean createLayoutData() throws Exception
	{
		if (mNumOfFrame == 0)
		{
			throw new Exception("Input frames not added");
		}

		int layoutDataLength = mLayoutWidth * mLayoutHeight;
		mLayoutData = new byte[layoutDataLength];

		if (AlmaShotGroupShot.Align(mImageWidthRotated, mImageHeightRotated, mBaseFrame, mNumOfFrame) != 0)
		{
			throw new Exception("Align : error");
		}

		return true;
	}
	
	
	//Calculate average value of each face rectangle
	//That value used to cut equals rects of same faces
	//and to draw equals face area's circles
	public void createFacesRadius()
	{
		// Get maximum number of found faces at one frame.
		int max = 0;
		for (int i = 0; i < mFacesList.size(); i++)
		{
			if (max < mFacesList.get(i).size())
			{
				max = mFacesList.get(i).size();
			}
		}
		
		
		mFacesRadiusList = new ArrayList<Integer>();
		
		for(int i = 0; i < max; i++)
		{
			float averageRadius = 0; //Calculated average value for exact face 
			float sumRadius = 0;
			int   sameFacesCount = 0;
			for(int j = 0; j < mFacesList.size(); j++)
			{
				ArrayList<Rect> faces = mFacesList.get(j);
				if(faces.size() < (i + 1))
					continue;
				Rect faceRect = faces.get(i);
				float faceRadius = getRadius(faceRect);
				sumRadius += faceRadius;
				sameFacesCount++;
			}
			
			averageRadius = sumRadius/sameFacesCount;
			mFacesRadiusList.add(Math.round(averageRadius));
		}
	}
	//Cut rectangles of faces from each frame and construct Bitmap for each face on each frame
	//This bitmaps is using to fast drawing faces on base frame in preview to avoid
	//long time Seamless processing each time when face changes.
	public void createFacesBitmaps()
	{
		mFacesBitmapsList = new ArrayList<ArrayList<Bitmap>>(mNumOfFrame);
		mFacesBitmapsRect = new ArrayList<ArrayList<Rect>>(mNumOfFrame);
		
		for (int i = 0; i < mNumOfFrame; i++)
		{
			int yuvBuffer = AlmaShotGroupShot.getInputFrame(i);
			
			ArrayList<Rect> faceRect = mFacesList.get(i);
			ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();
			ArrayList<Rect> bitmapRects = new ArrayList<Rect>();

			int faceIndex = 0;
			for (Rect rect : faceRect)
			{
				float ratiox;
				float ratioy;
				if (mImageDataOrientation == 90 || mImageDataOrientation == 270)
				{
					ratiox = (float) this.mImageHeight / (float) this.mPreviewWidthRotated;
					ratioy = (float) this.mImageWidth / (float) this.mPreviewHeightRotated;
				} else
				{
					ratiox = (float) this.mImageWidth / (float) this.mPreviewWidthOriginal;
					ratioy = (float) this.mImageHeight / (float) this.mPreviewHeightOriginal;
				}
				
				int faceRadius = mFacesRadiusList.get(faceIndex);
				
				float averageRatio = (ratiox + ratioy)/2;
				int centerX = rect.centerX();
				int centerY = rect.centerY();
				
				float l = centerX - faceRadius;
				float r = centerX + faceRadius;

				float t = centerY - faceRadius;
				float b = centerY + faceRadius;
				
//				l = l < 0? 0 : l;
//				r = r > mImageWidthRotated? mImageWidthRotated : r;
//				t = t < 0? 0 : t;
//				b = b > mImageHeightRotated? mImageHeightRotated : b;
				
				RectF tmpRect = new RectF(l, t, r, b);
				Rect  coverRect = new Rect();
				Util.rectFToRect(tmpRect, coverRect);
				
				Bitmap bitmap = Bitmap.createBitmap(
						AlmaShotGroupShot.NV21toARGB(yuvBuffer, mImageWidthRotated, mImageHeightRotated, coverRect, Math.round((faceRadius*2)/averageRatio), Math.round((faceRadius*2)/averageRatio)),
						Math.round((faceRadius*2)/averageRatio), Math.round((faceRadius*2)/averageRatio), Config.RGB_565);
				
				int bitmapWidth = bitmap.getWidth();
				int bitmapHeight = bitmap.getHeight();
				
	            Bitmap rounding_bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
	            Canvas rounding_canvas = new Canvas(rounding_bitmap);
	            Paint rounding_paint = new Paint();
	            
	            final Rect orect = new Rect(0, 0, rounding_bitmap.getWidth(), rounding_bitmap.getHeight());
	            final RectF rectF = new RectF(orect);

	            rounding_canvas.drawRoundRect(rectF, faceRadius, faceRadius, rounding_paint);
	            rounding_paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	            rounding_canvas.drawBitmap(bitmap, null, orect, rounding_paint);
				
				bitmaps.add(rounding_bitmap);
				bitmapRects.add(coverRect);
				
				faceIndex++;
			}
			
			
			mFacesBitmapsList.add(bitmaps);
			mFacesBitmapsRect.add(bitmapRects);
		}
	}

	public Bitmap getPreviewBitmap()
	{
		return mPreviewBitmap;
	}

	// Initialize mPreviewBitmap with latest actual data.
	public synchronized void updateBitmap()
	{
		// If base frame or some of faces were changed, then prepare new preview
		// data.
		if (mIsBaseFrameChanged || mIsFacesChanged)
		{
			if (mBuffer == null || (mBuffer != null && mIsBaseFrameChanged))
			{
				mBuffer = ImageConversion.decodeYUVfromBuffer(this.mYUVBufferList.get(mBaseFrame), mImageWidth, mImageHeight);
						
				int rotation = ApplicationScreen.getGUIManager().getMatrixRotationForBitmap(mImageDataOrientation, 0, mCameraMirrored);
				if(rotation != 0)
				{
					Matrix rotateMatrix = new Matrix();
					rotateMatrix.postRotate(rotation);
					Bitmap rotatedBitmap = Bitmap.createBitmap(mBuffer, 0, 0, mBuffer.getWidth(),
							mBuffer.getHeight(), rotateMatrix, true);
	
					if (rotatedBitmap != mBuffer)
					{
						mBuffer.recycle();
						mBuffer = rotatedBitmap;
					}
				}
			}
//			makePreview();
			this.prepareLayout();
			mIsBaseFrameChanged = false;
			mIsFacesChanged = false;
		}

//		if (mBuffer == null)
//		{
//			// Create bitmap based on ARGBBuffer data. This bitmap is not
//			// mutable!
//			Bitmap tmpBitmap = Bitmap.createBitmap(ARGBBuffer, mPreviewWidthRotated, mPreviewHeightRotated,
//					Bitmap.Config.ARGB_8888);
//
//			// Initialize mBuffer as copy of tmpBitmap and make it mutable.
//			mBuffer = tmpBitmap.copy(tmpBitmap.getConfig(), true);
//		} else
//		{
//			mBuffer.setPixels(ARGBBuffer, 0, mPreviewWidthRotated, 0, 0, mPreviewWidthRotated, mPreviewHeightRotated);
//		}

		mPreviewBitmap = mBuffer.copy(mBuffer.getConfig(), true);
//		// Image stored into mBuffer has 0 orientation, same as faces
//		// coordinates. Draw faces circles on image before rotation.
////		drawFaceAreasOnPreviewBitmap(mBuffer);
//
		drawFaceBitmapsOnPreviewBitmap(mPreviewBitmap);
		drawFaceAreasOnPreviewBitmap(mPreviewBitmap);
//		
		if(mMatrixRotation != 0)
			mPreviewBitmap = Bitmap.createBitmap(mPreviewBitmap, 0, 0, mPreviewBitmap.getWidth(), mPreviewBitmap.getHeight(),
					mDeviceRotationTransform, true);
		
		// After preview completely prepared we need just rotate it.
//		if(mMatrixRotation != 0)
//		mPreviewBitmap = Bitmap.createBitmap(mBuffer, 0, 0, mBuffer.getWidth(), mBuffer.getHeight(),
//				mDeviceRotationTransform, true);
//		else
//			mPreviewBitmap = mBuffer;
	}
	
//	public synchronized void updateFacesOnBitmap()
//	{
//		// If some of faces were changed, then prepare new preview
//		// data.
//		if (mIsFacesChanged)
//		{
//			makePreview();
//			mIsFacesChanged = false;
//		}		
//	}

	private void prepareLayout()
	{
		fillLayoutwithBaseFrame();
		fillLayoutwithStitchingflag();
		fillLayoutWithFaceindex();
	}

	private void makePreview()
	{
		prepareLayout();

		byte[] bufferLayout = mLayoutData.clone();
		AlmaShotGroupShot.Preview(ARGBBuffer, mBaseFrame, mImageWidthRotated, mImageHeightRotated,
				mPreviewWidthRotated, mPreviewHeightRotated, bufferLayout);
		bufferLayout = null;
		return;
	}

	private void fillLayoutwithBaseFrame()
	{
		Arrays.fill(mLayoutData, (byte) mBaseFrame);
	}

	private void fillLayoutwithStitchingflag()
	{
		int i = 0;
		int width = mLayoutWidth;
		int height = mLayoutHeight;

		ArrayList<Rect> faceRect = mFacesList.get(mBaseFrame);

		for (Rect rect : faceRect)
		{
			int frameIndex = mChosenFaces[mBaseFrame][i];
			Rect dst = mFacesList.get(frameIndex).get(i);
			Rect newRect = null;
			if (isFarDistance(rect, dst))
			{
				newRect = new Rect((int) (dst.left / IMAGE_TO_LAYOUT), (int) (dst.top / IMAGE_TO_LAYOUT),
						(int) (dst.right / IMAGE_TO_LAYOUT), (int) (dst.bottom / IMAGE_TO_LAYOUT));
			} else
			{
				newRect = new Rect((int) (rect.left / IMAGE_TO_LAYOUT), (int) (rect.top / IMAGE_TO_LAYOUT),
						(int) (rect.right / IMAGE_TO_LAYOUT), (int) (rect.bottom / IMAGE_TO_LAYOUT));
			}

			float radius = getRadius(newRect) * 2;
			int left, top, right, bottom;

			left = newRect.centerX() - (int) radius;
			top = newRect.centerY() - (int) radius;
			right = newRect.centerX() + (int) radius;
			bottom = newRect.centerY() + (int) radius;

			if (left < 0)
				left = 0;
			if (right > width)
				right = width;
			if (top < 0)
				top = 0;
			if (bottom > height)
				bottom = height;

			for (int yy = top; yy < bottom; ++yy)
			{
				for (int xx = left; xx < right; ++xx)
				{
					int xy = xx + yy * width;
					if (checkDistance(radius, xx, yy, newRect.centerX(), newRect.centerY()))
					{
						mLayoutData[xy] = (byte) 0xFF;
					}
				}
			}
			i++;
		}
	}

	private void fillLayoutWithFaceindex()
	{
		int i = 0;
		int width = mLayoutWidth;
		int height = mLayoutHeight;

		ArrayList<Rect> faceRect = mFacesList.get(mBaseFrame);

		for (Rect rect : faceRect)
		{
			int frameIndex = mChosenFaces[mBaseFrame][i];
			Rect dst = mFacesList.get(frameIndex).get(i);
			Rect newRect = null;
			if (isFarDistance(rect, dst))
			{
				newRect = new Rect((int) (dst.left / IMAGE_TO_LAYOUT), (int) (dst.top / IMAGE_TO_LAYOUT),
						(int) (dst.right / IMAGE_TO_LAYOUT), (int) (dst.bottom / IMAGE_TO_LAYOUT));

			} else
			{
				newRect = new Rect((int) (rect.left / IMAGE_TO_LAYOUT), (int) (rect.top / IMAGE_TO_LAYOUT),
						(int) (rect.right / IMAGE_TO_LAYOUT), (int) (rect.bottom / IMAGE_TO_LAYOUT));
			}

			float radius = getRadius(newRect);
			int left, top, right, bottom;

			left = newRect.centerX() - (int) radius;
			top = newRect.centerY() - (int) radius;
			right = newRect.centerX() + (int) radius;
			bottom = newRect.centerY() + (int) radius;

			if (left < 0)
				left = 0;
			if (right > width)
				right = width;
			if (top < 0)
				top = 0;
			if (bottom > height)
				bottom = height;

			for (int yy = top; yy < bottom; ++yy)
			{
				for (int xx = left; xx < right; ++xx)
				{
					int xy = xx + yy * width;
					if (checkDistance(radius, xx, yy, newRect.centerX(), newRect.centerY()))
					{
						mLayoutData[xy] = (byte) frameIndex;
					}
				}
			}
			i++;
		}
	}

	// Draw circles around detected faces.
	private void drawFaceAreasOnPreviewBitmap(Bitmap bitmap)
	{
		ArrayList<Rect> faceRect = mFacesList.get(mBaseFrame);

		float ratiox;
		float ratioy;
		float bWidth = bitmap.getWidth();
		float bHeight = bitmap.getHeight();
		CameraController.Size imageSize = CameraController.getCameraImageSize();
		if (mImageDataOrientation == 90 || mImageDataOrientation == 270)
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
		
		int i = 0;
		for (Rect rect : faceRect)
		{
			float radius = mFacesRadiusList.get(i);
			c.drawCircle(rect.centerX() / ratiox, rect.centerY() / ratioy, radius / ((ratiox + ratioy) / 2), paint);
			i++;
		}
	}
	
	
	// Draw chosen detected face bitmaps.
	private void drawFaceBitmapsOnPreviewBitmap(Bitmap bitmap)
	{
		ArrayList<Rect> faceRect = mFacesList.get(mBaseFrame);

		float ratiox;
		float ratioy;
		float bWidth = bitmap.getWidth();
		float bHeight = bitmap.getHeight();
		CameraController.Size imageSize = CameraController.getCameraImageSize();
		if (mImageDataOrientation == 90 || mImageDataOrientation == 270)
		{
			ratiox = (float) imageSize.getHeight() / (float) bWidth;
			ratioy = (float) imageSize.getWidth() / (float) bHeight;
		} else
		{
			ratiox = (float) imageSize.getWidth() / (float) bWidth;
			ratioy = (float) imageSize.getHeight() / (float) bHeight;
		}
		
		Canvas c = new Canvas(bitmap);
		
		for (int i = 0; i < faceRect.size(); i++)
		{
			int frameIndex = mChosenFaces[mBaseFrame][i];
			if(frameIndex != mBaseFrame)
			{
				Rect rect = faceRect.get(i);
				Bitmap faceBitmap = mFacesBitmapsList.get(frameIndex).get(i);
				int faceRadius = mFacesRadiusList.get(i);

				int faceWidth = faceRadius;
				int faceHeight = faceRadius;
				
				float left, top, right, bottom;

				float averageRatio = (ratiox + ratioy)/2;
				
				left = rect.centerX()/ratiox - faceWidth/averageRatio;
				top = rect.centerY()/ratioy - faceHeight/averageRatio;
				right = rect.centerX()/ratiox + faceWidth/averageRatio;
				bottom = rect.centerY()/ratioy +  faceHeight/averageRatio;

//				if (left < 0)
//					left = 0;
//				if (right > bWidth)
//					right = bWidth;
//				if (top < 0)
//					top = 0;
//				if (bottom > bHeight)
//					bottom = bHeight;
				
				RectF newRect = new RectF(left, top, right, bottom);
				c.drawBitmap(faceBitmap, newRect.left, newRect.top , null);
			}
		}
	}

	// Check if point with position (x,y) included inside any face area.
	// If it included into some face area, then change its face.
	public boolean eventContainsFace(float x, float y, View v)
	{
		float ratiox;
		float ratioy;

		ArrayList<Rect> faceRect = mFacesList.get(mBaseFrame);

//		ratiox = (float) mImageWidthRotated / (float) mPreviewWidthRotated;
//		ratioy = (float) mImageHeightRotated / (float) mPreviewHeightRotated;
		
		if (mImageDataOrientation == 90 || mImageDataOrientation == 270)
		{
			ratiox = (float) this.mImageHeight / (float) this.mPreviewWidthRotated;
			ratioy = (float) this.mImageWidth / (float) this.mPreviewHeightRotated;
		} else
		{
			ratiox = (float) this.mImageWidth / (float) this.mPreviewWidthOriginal;
			ratioy = (float) this.mImageHeight / (float) this.mPreviewHeightOriginal;
		}
		
//		Log.e(TAG, "eventContainsFace. x = " + x + " y = " + y);
//		Log.e(TAG, "eventContainsFace. image (w x h) = " + mImageWidthRotated + " x " + mImageHeightRotated);
//		Log.e(TAG, "eventContainsFace. preview (w x h) = " + mPreviewWidthOriginal + " x " + mPreviewHeightOriginal);
//		Log.e(TAG, "eventContainsFace. view (w x h) = " + v.getWidth() + " x " + v.getHeight());
//		Log.e(TAG, "eventContainsFace. display (w x h) = " + mDisplayWidth + " x " + mDisplayHeight);
		
		// Transform coordinates of touch to align with coordinate system of
		// preview image
		// (and face coordinates).
		if (mDeviceOrientation == 90)
		{
			float xTmp = x;
			x = v.getHeight() - y;
			y = xTmp;
		}
		if (mDeviceOrientation == 180)
		{
			x = v.getWidth() - x;
			y = v.getHeight() - y;
		}
		if (mDeviceOrientation == 270)
		{
			float yTmp = y;
			y = v.getWidth() - x;
			x = yTmp;
		}

		// Have to correct touch coordinates because ImageView centered on the
		// screen and it's coordinate system not aligned with screen coordinate
		// system.
//		if ((mDisplayWidth > v.getHeight() || mDisplayHeight > v.getWidth()))
//		{
//			if (mDeviceOrientation == 90 || mDeviceOrientation == 270)
//			{
//				y = y - (mDisplayHeight - v.getWidth()) / 2;
//				x = x - (mDisplayWidth - v.getHeight()) / 2;
//			} else
//			{
//				x = x - (mDisplayHeight - v.getWidth()) / 2;
//				y = y - (mDisplayWidth - v.getHeight()) / 2;
//			}
//		}

		int i = 0;
		for (Rect rect : faceRect)
		{
			Rect newRect = new Rect((int) (rect.left / ratiox), (int) (rect.top / ratioy), (int) (rect.right / ratiox),
					(int) (rect.bottom / ratioy));
			float radius = getRadius(newRect);

			// If touch was made inside face area, then change face.
			if (checkDistance(radius, x, y, newRect.centerX(), newRect.centerY()))
			{
				int newFrameIndex = mChosenFaces[mBaseFrame][i] + 1;
				while (!checkFaceIsSuitable(newFrameIndex, i, radius, ratiox, ratioy, newRect))
					newFrameIndex++;

				mChosenFaces[mBaseFrame][i] = newFrameIndex % mNumOfFrame;
				mIsFacesChanged = true;
				return true;
			}
			i++;
		}
		return false;
	}

	// Check if new face coordinates are close to old one.
	private boolean checkFaceIsSuitable(int frameIndex, int faceIndex, float faceRadius, float ratioX, float ratioY,
			Rect currFace)
	{
		if (mFacesList.get(frameIndex % mNumOfFrame).size() <= faceIndex)
			return false;
		else
		{
			Rect candidateRect = mFacesList.get(frameIndex % mNumOfFrame).get(faceIndex);
			Rect newRect = new Rect((int) (candidateRect.left / ratioX), (int) (candidateRect.top / ratioY),
					(int) (candidateRect.right / ratioX), (int) (candidateRect.bottom / ratioY));
			return checkFaceDistance(faceRadius, newRect.centerX(), newRect.centerY(), currFace.centerX(),
					currFace.centerY());
		}
	}

	public void setBaseFrame(int base)
	{
		mBaseFrame = base;
		mIsBaseFrameChanged = true;
	}

	public byte[] processingSaveData()
	{
		byte[] jpegBuffer = null;

		try
		{
			prepareLayout();
			
			mCrop = new int[5];
			mOutNV21 = AlmaShotGroupShot.RealView(mImageWidthRotated, mImageHeightRotated, mCrop, mLayoutData);

			android.graphics.YuvImage out = new android.graphics.YuvImage(SwapHeap.SwapFromHeap(mOutNV21,
					mImageWidthRotated * mImageHeightRotated * 3 / 2), ImageFormat.NV21, mImageWidthRotated,
					mImageHeightRotated, null);
			mOutNV21 = 0;

			ByteArrayOutputStream os = new ByteArrayOutputStream();

			Rect r = new Rect(mCrop[0], mCrop[1], mCrop[0] + mCrop[2], mCrop[1] + mCrop[3]);

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
			int jpegQuality = Integer.parseInt(prefs.getString(ApplicationScreen.sJPEGQualityPref, "95"));
			if (!out.compressToJpeg(r, jpegQuality, os))
			{
				Log.d(TAG, "the compression is not successful");
			}
			jpegBuffer = os.toByteArray();
			os.close();

		} catch (Exception e)
		{
			Log.d(TAG, "Exception occured");
			e.printStackTrace();
		}

		return jpegBuffer;
	}

	// Release all resources.
	public void release()
	{
		AlmaShotGroupShot.Release(mNumOfFrame);

		for (int yuv : mYUVBufferList)
			SwapHeap.FreeFromHeap(yuv);
		mYUVBufferList.clear();

		if (mPreviewBitmap != null)
		{
			mPreviewBitmap.recycle();
			mPreviewBitmap = null;
		}

		if (mBuffer != null)
		{
			mBuffer.recycle();
			mBuffer = null;
		}

		ARGBBuffer = null;
		mCrop = null;
		mFacesList = null;
		mFacesBitmapsList = null;
		mFacesBitmapsRect = null;

		mLayoutData = null;
		mChosenFaces = null;

		if (mOutNV21 != 0)
		{
			SwapHeap.FreeFromHeap(mOutNV21);
			mOutNV21 = 0;
		}

		mInstance = null;
	}

	private static boolean isFarDistance(Rect src, Rect dst)
	{
		if (dst.centerX() > src.left && dst.centerX() < src.right)
		{
			return false;
		}

		if (dst.centerY() > src.top && dst.centerY() < src.bottom)
		{
			return false;
		}
		return true;

	}

	/**
	 * @return width of down-scaled image which will be used for face detection.
	 */
	private static int getWidthForFaceDetection(int w, int h, int imageDataOrientation)
	{
		int ds = (Math.max(w, h) + MAX_WIDTH_FOR_FACEDETECTION - 1) / MAX_WIDTH_FOR_FACEDETECTION;

		return (imageDataOrientation == 90 || imageDataOrientation == 270) ? h / ds : w / ds;
	}

	/**
	 * @return height of down-scaled image which will be used for face
	 *         detection.
	 */
	private static int getHeightForFaceDetection(int w, int h, int imageDataOrientation)
	{
		int ds = (Math.max(w, h) + MAX_WIDTH_FOR_FACEDETECTION - 1) / MAX_WIDTH_FOR_FACEDETECTION;

		return (imageDataOrientation == 90 || imageDataOrientation == 270) ? w / ds : h / ds;
	}

	private static boolean checkDistance(float radius, float x, float y, int centerX, int centerY)
	{
		float distance = getSquareOfDistance((int) x, (int) y, centerX, centerY);
		if (distance < (radius * radius))
		{
			return true;
		}
		return false;
	}

	private static float getRadius(Rect rect)
	{
		return (rect.width() + rect.height()) / 2;
	}

	private static int getSquareOfDistance(int x, int y, int x0, int y0)
	{
		return (x - x0) * (x - x0) + (y - y0) * (y - y0);
	}
}
