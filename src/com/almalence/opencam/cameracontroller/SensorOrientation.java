////import com.almalence.opencam.ApplicationScreen;
////import com.almalence.opencam.ui.EglEncoder;
////import com.almalence.plugins.capture.panoramaaugmented.AugmentedPanoramaEngine;
////import com.almalence.plugins.capture.preshot.PreShot;
////import com.almalence.plugins.capture.video.DROVideoEngine;
////import com.almalence.util.ImageConversion;
////import com.almalence.util.Util;
////
////import android.graphics.Bitmap;
////import android.graphics.Matrix;
////import android.opengl.EGL14;
////
////
////
//
////              Back        Front
////Nexus 5:      90          270
////Nexus 5x:     270         270
////LG Flex2:     90          270
////OnePlus One2: 90          270
////Nexus 6p:     90          90
////Nexus 6:      90          90
//
//
//
//class SensorOrientation
//{
//	SensorOrientation()
//	{
//		//ImageAdapter.decodeJPEG(YUV)FromData		
//		Matrix matrix = new Matrix();
//		//Workaround for Nexus5x, image is flipped because of sensor orientation
//		if(CameraController.isNexus5x)                                                                   //b   f|   f_
//			matrix.postRotate(mCameraMirrored ? (mIsLandscape ? 90 : 270) : 270); //(Nexus 5x: postRotate(270, 270, 90))
//		else
//			matrix.postRotate(mCameraMirrored ? (mIsLandscape ? 90 : 270) : 90);  //(Nexus 5: postRotate(90, -90, 90))
//		
//		
//		
//		//GUI.getImageDataOrientation
//		int i = (mDeviceOrientation + (CameraController.isNexus5x? (CameraController.isFrontCamera()? 90 : 270) : 90)) % 360;
//		
//		
//		
//		//PreshotProcessing.getMultishotBitmap
//		if (mCameraMirrored && (90 == PreShot.getOrientation(index) || 270 == PreShot.getOrientation(index)))
//		{
//			Matrix matrix = new Matrix();
//			matrix.postRotate(180);
//			bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//		}
//
//		if ((CameraController.isFlippedSensorDevice() && mCameraMirrored) || (CameraController.isNexus5x && !mCameraMirrored))
//		{	
//			Matrix matrix = new Matrix();
//			matrix.postRotate(180);
//			bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//		}
//		
//		
//		
//		//ObjectRemoval(Sequence)Processing.handleMessage. MSG_REDRAW
//		Matrix matrix = new Matrix();
//		//Workaround for Nexus5x, image is flipped because of sensor orientation
//		matrix.postRotate(CameraController.isNexus5x? (mCameraMirrored ? 90 : -90) : 90);
//		Bitmap rotated = Bitmap.createBitmap(PreviewBmp, 0, 0, PreviewBmp.getWidth(), PreviewBmp.getHeight(),
//				matrix, true);
//		mImgView.setImageBitmap(rotated);
//		mImgView.setRotation(CameraController.isFrontCamera() ? ((mDisplayOrientation == 0 || mDisplayOrientation == 180) ? 0
//				: 180)
//				: 0);
//				
//		
//		
//		//FocusVFPlugin.initialize
//		Matrix matrix = new Matrix();
//		Util.prepareMatrix(matrix, mirror, (CameraController.isNexus5x && !mirror)? 270 : 90 , mPreviewWidth, mPreviewHeight);
//		
//		
//		
//		//NightCapturePlugin.onPreviewFrame
//		//Workaround for Nexus5x, image is flipped because of sensor orientation
//		if(CameraController.isNexus5x)
//		{
//			dataRotated = new byte[dataS.length];
//			ImageConversion.TransformNV21(dataS, dataRotated, imageWidth, imageHeight, 1, 1, 0);
//			yuvData = dataRotated;
//		}
//		else
//			yuvData = dataS;
//		
//		
//		
//		
//		//Multishots onStartPostProcessing
//		//Workaround for Nexus5x, image is flipped because of sensor orientation
//		if(CameraController.isNexus5x)
//			mImgView.setRotation(mCameraMirrored ? ((mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180) ? 0
//					: 180)
//					: 180);
//		else
//			mImgView.setRotation(mCameraMirrored ? ((mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180) ? 0
//					: 180)
//					: 0);
//		
//		
//		
//		
//		//Workaround for Nexus5x, image is flipped horizontally & vertically because of sensor orientation
//		if(CameraController.isNexus5x && !mCameraMirrored)
//		{
//			x = mDisplayWidth - event.getY();
//			y = event.getX();	
//		}
//		else
//		{
//			x = event.getY();
//			y = mDisplayHeight - 1 - event.getX();
//		}
//		
//		
//		
//		//ObjectRemoval.onTouch
//		//Workaround for Nexus5x, image is flipped horizontally & vertically because of sensor orientation
//		if(CameraController.isNexus5x && !mCameraMirrored)
//		{
//			x = mDisplayWidth - event.getY();
//			y = event.getX();	
//		}
//		else
//		{
//			x = event.getY();
//			y = mDisplayHeight - 1 - event.getX();
//		}
//		
//		
//		
//		
//		//FocusVFPlugin.onTouchFocusAndMeteringArea (onTouchFocusArea + onTouchMeteringArea)
//		//TODO: Logic of coordinate's swapping must be based on sensor orientation not on device model!
//		if (!CameraController.isNexus5x)
//		{
//			int tmpX = xRaw;
//			xRaw = yRaw;
//			yRaw = previewWidth - tmpX - 1;
//		}
//		else
//		{
//			int tmpX = xRaw;
//			xRaw = previewHeight - yRaw - 1;
//			yRaw = tmpX;
//		}
//		
//		
//		
//		
//		
//		//AugmentedPanoramaEngine.AugmentedFrameTaken
//		if (CameraController.isFrontCamera())
//		{
//			if (CameraController.isFlippedSensorDevice())
//				ImageConversion.TransformNV21N(yuv_address,
//						yuv_address,
//						AugmentedPanoramaEngine.this.height,
//						AugmentedPanoramaEngine.this.width,
//						0, 1, 0);
//			else
//				ImageConversion.TransformNV21N(yuv_address,
//						yuv_address,
//						AugmentedPanoramaEngine.this.height,
//						AugmentedPanoramaEngine.this.width,
//						1, 0, 0);
//		}
//		else
//		{
//			//Workaround for Nexus5x, image is flipped because of sensor orientation
//			if(CameraController.isNexus5x)
//			{
//				ImageConversion.TransformNV21N(yuv_address,
//						yuv_address,
//						AugmentedPanoramaEngine.this.height,
//						AugmentedPanoramaEngine.this.width,
//						1, 1, 0);
//			}
//		}
//		
//		
//		
//		
//		//DROVideoEngine.startRecording
//		DROVideoEngine.this.encoder = new EglEncoder(path, DROVideoEngine.this.previewWidth,
//				DROVideoEngine.this.previewHeight, 24, 20000000, CameraController.isNexus5x ? (ApplicationScreen
//						.getGUIManager().getImageDataOrientation() + 180) % 360 : ApplicationScreen
//						.getGUIManager().getImageDataOrientation(), EGL14
//						.eglGetCurrentContext());
//		
//		
//		
//		//GroupShotProcessingPlugin.setupImageView
//		if(CameraController.isNexus5x)
//			mImgView.setRotation(mCameraMirrored ? ((mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180) ? 0
//					: 180)
//					: 180);
//		else
//			mImgView.setRotation(mCameraMirrored ? ((mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180) ? 0
//					: 180)
//					: 0);
//	}
//	
//};
