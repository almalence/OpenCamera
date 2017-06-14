package net.sourceforge.opencamera.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import net.sourceforge.opencamera.MainActivity;
import net.sourceforge.opencamera.PreferenceKeys;
import net.sourceforge.opencamera.SaveLocationHistory;
import net.sourceforge.opencamera.CameraController.CameraController;
import net.sourceforge.opencamera.Preview.Preview;
import net.sourceforge.opencamera.UI.FolderChooserDialog;
import net.sourceforge.opencamera.UI.PopupView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
//import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
//import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
//import android.os.Environment;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ZoomControls;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
	private static final String TAG = "MainActivityTest";
	private MainActivity mActivity = null;
	private Preview mPreview = null;

	@SuppressWarnings("deprecation")
	public MainActivityTest() {
		super("net.sourceforge.opencamera", MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		Log.d(TAG, "setUp");
		super.setUp();

	    setActivityInitialTouchMode(false);

	    // use getTargetContext() as we haven't started the activity yet (and don't want to, as we want to set prefs before starting)
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.getInstrumentation().getTargetContext());
		SharedPreferences.Editor editor = settings.edit();
		editor.clear();
		//editor.putBoolean(PreferenceKeys.getUseCamera2PreferenceKey(), true); // uncomment to test Camera2 API
		editor.apply();
		
	    Intent intent = new Intent();
	    intent.putExtra("test_project", true);
	    setActivityIntent(intent);
	    mActivity = getActivity();
	    mPreview = mActivity.getPreview();

		//restart(); // no longer need to restart, as we reset prefs before starting up; not restarting makes tests run faster!

		//Camera camera = mPreview.getCamera();
	    /*mSpinner = (Spinner) mActivity.findViewById(
	        com.android.example.spinner.R.id.Spinner01
	      );*/

	    //mPlanetData = mSpinner.getAdapter();
	}

	@Override
	protected void tearDown() throws Exception {
		Log.d(TAG, "tearDown");

		assertTrue( mPreview.getCameraController() == null || mPreview.getCameraController().count_camera_parameters_exception == 0 );
		assertTrue( mPreview.getCameraController() == null || mPreview.getCameraController().count_precapture_timeout == 0 );

		// reset back to defaults
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.clear();
		editor.apply();

		super.tearDown();
	}

    public void testPreConditions() {
		assertTrue(mPreview != null);
		//assertTrue(mPreview.getCamera() != null);
		//assertTrue(mCamera != null);
		//assertTrue(mSpinner.getOnItemSelectedListener() != null);
		//assertTrue(mPlanetData != null);
		//assertEquals(mPlanetData.getCount(),ADAPTER_COUNT);
	}

	private void restart() {
		Log.d(TAG, "restart");
	    mActivity.finish();
	    setActivity(null);
		Log.d(TAG, "now starting");
	    mActivity = getActivity();
	    mPreview = mActivity.getPreview();
		Log.d(TAG, "restart done");
	}
	
	private void pauseAndResume() {
		Log.d(TAG, "pauseAndResume");
	    // onResume has code that must run on UI thread
		mActivity.runOnUiThread(new Runnable() {
			public void run() {
				Log.d(TAG, "pause...");
				getInstrumentation().callActivityOnPause(mActivity);
				Log.d(TAG, "resume...");
				getInstrumentation().callActivityOnResume(mActivity);
			}
		});
		// need to wait for UI code to finish before leaving
		this.getInstrumentation().waitForIdleSync();
	}

	private void updateForSettings() {
		Log.d(TAG, "updateForSettings");
	    // updateForSettings has code that must run on UI thread
		mActivity.runOnUiThread(new Runnable() {
			public void run() {
				mActivity.updateForSettings();
			}
		});
		// need to wait for UI code to finish before leaving
		this.getInstrumentation().waitForIdleSync();
	}

	private void clickView(final View view) {
		// TouchUtils.clickView doesn't work properly if phone held in portrait mode!
	    //TouchUtils.clickView(MainActivityTest.this, view);
		Log.d(TAG, "clickView: "+ view);
		assertTrue(view.getVisibility() == View.VISIBLE);
		mActivity.runOnUiThread(new Runnable() {
			public void run() {
				assertTrue(view.performClick());
			}
		});
		// need to wait for UI code to finish before leaving
		this.getInstrumentation().waitForIdleSync();
	}

	private void switchToFlashValue(String required_flash_value) {
		if( mPreview.supportsFlash() ) {
		    String flash_value = mPreview.getCurrentFlashValue();
			Log.d(TAG, "start flash_value: "+ flash_value);
			Log.d(TAG, "required_flash_value: "+ required_flash_value);
			if( !flash_value.equals(required_flash_value) ) {
				assertFalse( mActivity.popupIsOpen() );
			    View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);
			    clickView(popupButton);
				Log.d(TAG, "wait for popup to open");
			    while( !mActivity.popupIsOpen() ) {
			    }
				Log.d(TAG, "popup is now open");
			    View currentFlashButton = mActivity.getPopupButton("TEST_FLASH_" + flash_value);
			    assertTrue(currentFlashButton != null);
			    assertTrue(currentFlashButton.getAlpha() == PopupView.ALPHA_BUTTON_SELECTED);
			    View flashButton = mActivity.getPopupButton("TEST_FLASH_" + required_flash_value);
			    assertTrue(flashButton != null);
			    assertTrue(flashButton.getAlpha() == PopupView.ALPHA_BUTTON);
			    clickView(flashButton);
			    flash_value = mPreview.getCurrentFlashValue();
				Log.d(TAG, "changed flash_value to: "+ flash_value);
			}
		    assertTrue(flash_value.equals(required_flash_value));
			String controller_flash_value = mPreview.getCameraController().getFlashValue();
			Log.d(TAG, "controller_flash_value: "+ controller_flash_value);
		    if( flash_value.equals("flash_frontscreen_auto") || flash_value.equals("flash_frontscreen_on") ) {
		    	// for frontscreen flash, the controller flash value will be "" (due to real flash not supported) - although on Galaxy Nexus this is "flash_off" due to parameters.getFlashMode() returning Camera.Parameters.FLASH_MODE_OFF
			    assertTrue(controller_flash_value.equals("") || controller_flash_value.equals("flash_off"));
		    }
		    else {
			    String expected_flash_value = flash_value;
				Log.d(TAG, "expected_flash_value: "+ expected_flash_value);
			    assertTrue(expected_flash_value.equals( controller_flash_value ));
		    }
		}
	}

	private void switchToFocusValue(String required_focus_value) {
		Log.d(TAG, "switchToFocusValue: "+ required_focus_value);
	    if( mPreview.supportsFocus() ) {
		    String focus_value = mPreview.getCurrentFocusValue();
			Log.d(TAG, "start focus_value: "+ focus_value);
			if( !focus_value.equals(required_focus_value) ) {
				assertFalse( mActivity.popupIsOpen() );
			    View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);
			    clickView(popupButton);
			    while( !mActivity.popupIsOpen() ) {
			    }
			    View focusButton = mActivity.getPopupButton("TEST_FOCUS_" + required_focus_value);
			    assertTrue(focusButton != null);
			    clickView(focusButton);
			    focus_value = mPreview.getCurrentFocusValue();
				Log.d(TAG, "changed focus_value to: "+ focus_value);
			}
		    assertTrue(focus_value.equals(required_focus_value));
		    String actual_focus_value = mPreview.getCameraController().getFocusValue();
			Log.d(TAG, "actual_focus_value: "+ actual_focus_value);
			String compare_focus_value = focus_value;
			if( compare_focus_value.equals("focus_mode_locked") )
				compare_focus_value = "focus_mode_auto";
			else if( compare_focus_value.equals("focus_mode_infinity") && mPreview.usingCamera2API() )
				compare_focus_value = "focus_mode_manual2";
		    assertTrue(compare_focus_value.equals(actual_focus_value));
	    }
	}
	
	private void switchToISO(int required_iso) {
		Log.d(TAG, "switchToISO: "+ required_iso);
	    if( mPreview.supportsFocus() ) {
		    int iso = mPreview.getCameraController().getISO();
			Log.d(TAG, "start iso: "+ iso);
			if( iso != required_iso ) {
				assertFalse( mActivity.popupIsOpen() );
			    View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);
			    clickView(popupButton);
			    while( !mActivity.popupIsOpen() ) {
			    }
			    View isoButton = mActivity.getPopupButton("TEST_ISO_" + required_iso);
			    assertTrue(isoButton != null);
			    clickView(isoButton);
			    iso = mPreview.getCameraController().getISO();
				Log.d(TAG, "changed iso to: "+ iso);
			}
		    assertTrue(iso == required_iso);
	    }
	}
	
	/* Sets the camera up to a predictable state:
	 * - Back camera
	 * - Photo mode
	 * - Flash off (if flash supported)
	 * - Focus mode auto (if focus modes supported)
	 * As a side-effect, the camera and/or camera parameters values may become invalid.
	 */
	private void setToDefault() {
		if( mPreview.isVideo() ) {
			Log.d(TAG, "turn off video mode");
		    View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
		    clickView(switchVideoButton);
		}
		assertTrue(!mPreview.isVideo());

		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
			int cameraId = mPreview.getCameraId();
			Log.d(TAG, "start cameraId: "+ cameraId);
			while( cameraId != 0 ) {
			    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
			    clickView(switchCameraButton);
			    // camera becomes invalid when switching cameras
				cameraId = mPreview.getCameraId();
				Log.d(TAG, "changed cameraId to: "+ cameraId);
			}
		}

		switchToFlashValue("flash_off");
		switchToFocusValue("focus_mode_auto");
	}

	/* Ensures that we only start the camera preview once when starting up.
	 */
	public void testStartCameraPreviewCount() {
		Log.d(TAG, "testStartCameraPreviewCount");
		/*Log.d(TAG, "1 count_cameraStartPreview: " + mPreview.count_cameraStartPreview);
		int init_count_cameraStartPreview = mPreview.count_cameraStartPreview;
	    mActivity.finish();
	    setActivity(null);
	    mActivity = this.getActivity();
	    mPreview = mActivity.getPreview();
		Log.d(TAG, "2 count_cameraStartPreview: " + mPreview.count_cameraStartPreview);
		assertTrue(mPreview.count_cameraStartPreview == init_count_cameraStartPreview);
		this.getInstrumentation().callActivityOnPause(mActivity);
		Log.d(TAG, "3 count_cameraStartPreview: " + mPreview.count_cameraStartPreview);
		assertTrue(mPreview.count_cameraStartPreview == init_count_cameraStartPreview);
		this.getInstrumentation().callActivityOnResume(mActivity);
		Log.d(TAG, "4 count_cameraStartPreview: " + mPreview.count_cameraStartPreview);
		assertTrue(mPreview.count_cameraStartPreview == init_count_cameraStartPreview+1);*/
		setToDefault();

		restart();
	    // onResume has code that must run on UI thread
		mActivity.runOnUiThread(new Runnable() {
			public void run() {
				Log.d(TAG, "1 count_cameraStartPreview: " + mPreview.count_cameraStartPreview);
				assertTrue(mPreview.count_cameraStartPreview == 1);
				getInstrumentation().callActivityOnPause(mActivity);
				Log.d(TAG, "2 count_cameraStartPreview: " + mPreview.count_cameraStartPreview);
				assertTrue(mPreview.count_cameraStartPreview == 1);
				getInstrumentation().callActivityOnResume(mActivity);
				Log.d(TAG, "3 count_cameraStartPreview: " + mPreview.count_cameraStartPreview);
				assertTrue(mPreview.count_cameraStartPreview == 2);
			}
		});
		// need to wait for UI code to finish before leaving
		this.getInstrumentation().waitForIdleSync();
	}

	/* Ensures that we save the video mode.
	 * Also tests the icons and content descriptions of the take photo and switch photo/video buttons are as expected.
	 */
	public void testSaveVideoMode() {
		Log.d(TAG, "testSaveVideoMode");
		setToDefault();

		View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
	    View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);

	    assertTrue(!mPreview.isVideo());
		assertTrue( takePhotoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.take_photo) ) );
		assertTrue( switchVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.switch_to_video) ) );

	    clickView(switchVideoButton);
	    assertTrue(mPreview.isVideo());
		assertTrue( takePhotoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.start_video) ) );
		assertTrue( switchVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.switch_to_photo) ) );

		restart();
	    assertTrue(mPreview.isVideo());
		assertTrue( takePhotoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.start_video) ) );
		assertTrue( switchVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.switch_to_photo) ) );

	    pauseAndResume();
	    assertTrue(mPreview.isVideo());
		assertTrue( takePhotoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.start_video) ) );
		assertTrue( switchVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.switch_to_photo) ) );
	}

	/* Ensures that we save the focus mode for photos when restarting.
	 * Note that saving the focus mode for video mode is tested in testFocusSwitchVideoResetContinuous.
	 */
	public void testSaveFocusMode() {
		Log.d(TAG, "testSaveVideoMode");
		if( !mPreview.supportsFocus() ) {
			return;
		}

		setToDefault();
		switchToFocusValue("focus_mode_macro");

		restart();
		String focus_value = mPreview.getCameraController().getFocusValue();
		assertTrue(focus_value.equals("focus_mode_macro"));

		pauseAndResume();
		focus_value = mPreview.getCameraController().getFocusValue();
		assertTrue(focus_value.equals("focus_mode_macro"));
	}

	/* Ensures that we save the flash mode torch when quitting and restarting.
	 */
	public void testSaveFlashTorchQuit() throws InterruptedException {
		Log.d(TAG, "testSaveFlashTorchQuit");

		if( !mPreview.supportsFlash() ) {
			return;
		}

		setToDefault();
		
		switchToFlashValue("flash_torch");

		restart();
		Thread.sleep(4000); // needs to be long enough for the autofocus to complete
	    String controller_flash_value = mPreview.getCameraController().getFlashValue();
		Log.d(TAG, "controller_flash_value: " + controller_flash_value);
	    assertTrue(controller_flash_value.equals("flash_torch"));
	    String flash_value = mPreview.getCurrentFlashValue();
		Log.d(TAG, "flash_value: " + flash_value);
	    assertTrue(flash_value.equals("flash_torch"));
	}

	/* Ensures that we save the flash mode torch when switching to front camera and then to back
	 * Note that this sometimes fail on Galaxy Nexus, because flash turns off after autofocus (and other camera apps do this too), but this only seems to happen some of the time!
	 * And Nexus 7 has no flash anyway.
	 * So commented out test for now.
	 */
	/*public void testSaveFlashTorchSwitchCamera() {
		Log.d(TAG, "testSaveFlashTorchSwitchCamera");

		if( !mPreview.supportsFlash() ) {
			return;
		}
		else if( Camera.getNumberOfCameras() <= 1 ) {
			return;
		}

		setToDefault();
		
		switchToFlashValue("flash_torch");

		int cameraId = mPreview.getCameraId();
	    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    clickView(switchCameraButton);
		int new_cameraId = mPreview.getCameraId();
		assertTrue(cameraId != new_cameraId);

	    clickView(switchCameraButton);
		new_cameraId = mPreview.getCameraId();
		assertTrue(cameraId == new_cameraId);

		Camera camera = mPreview.getCamera();
	    Camera.Parameters parameters = camera.getParameters();
		Log.d(TAG, "parameters flash mode: " + parameters.getFlashMode());
	    assertTrue(parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH));
	    String flash_value = mPreview.getCurrentFlashValue();
		Log.d(TAG, "flash_value: " + flash_value);
	    assertTrue(flash_value.equals("flash_torch"));
	}*/
	
	public void testFlashStartup() throws InterruptedException {
		Log.d(TAG, "testFlashStartup");
		setToDefault();

		if( !mPreview.supportsFlash() ) {
			return;
		}

		Log.d(TAG, "# switch to flash on");
		switchToFlashValue("flash_on");
		Log.d(TAG, "# restart");
		restart();

		Log.d(TAG, "# switch flash mode");
		// now switch to torch - the idea is that this is done while the camera is starting up
	    // though note that sometimes we might not be quick enough here!
		// don't use switchToFlashValue here, it'll get confused due to the autofocus changing the parameters flash mode
		// update: now okay to use it, now we have the popup UI
	    //View flashButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.flash);
	    //clickView(flashButton);
		switchToFlashValue("flash_torch");

	    //Camera camera = mPreview.getCamera();
	    //Camera.Parameters parameters = camera.getParameters();
	    //String flash_mode = mPreview.getCurrentFlashMode();
	    String flash_value = mPreview.getCurrentFlashValue();
		Log.d(TAG, "# flash value is now: " + flash_value);
		Log.d(TAG, "# sleep");
		Thread.sleep(4000); // needs to be long enough for the autofocus to complete
	    /*parameters = camera.getParameters();
		Log.d(TAG, "# parameters flash mode: " + parameters.getFlashMode());
	    assertTrue(parameters.getFlashMode().equals(flash_mode));*/
		String camera_flash_value = mPreview.getCameraController().getFlashValue();
		Log.d(TAG, "# camera flash value: " + camera_flash_value);
	    assertTrue(camera_flash_value.equals(flash_value));
	}
	
	/** Tests that flash remains on, with the startup focus flash hack.
	 */
	public void testFlashStartup2() throws InterruptedException {
		Log.d(TAG, "testFlashStartup2");
		setToDefault();

		if( !mPreview.supportsFlash() ) {
			return;
		}

		Log.d(TAG, "# switch to flash on");
		switchToFlashValue("flash_on");
		Log.d(TAG, "# restart");
		restart();
		Thread.sleep(3000);
	    String flash_value = mPreview.getCameraController().getFlashValue();
		Log.d(TAG, "1 flash value is now: " + flash_value);
		assertTrue(flash_value.equals("flash_on"));

		switchToFocusValue("focus_mode_continuous_picture");
		restart();
		Thread.sleep(3000);
	    flash_value = mPreview.getCameraController().getFlashValue();
		Log.d(TAG, "2 flash value is now: " + flash_value);
		assertTrue(flash_value.equals("flash_on"));
	}
	
	private void checkOptimalPreviewSize() {
		Log.d(TAG, "preview size: " + mPreview.getCameraController().getPreviewSize().width + ", " + mPreview.getCameraController().getPreviewSize().height);
        List<CameraController.Size> sizes = mPreview.getSupportedPreviewSizes();
    	CameraController.Size best_size = mPreview.getOptimalPreviewSize(sizes);
		Log.d(TAG, "best size: " + best_size.width + ", " + best_size.height);
    	assertTrue( best_size.width == mPreview.getCameraController().getPreviewSize().width );
    	assertTrue( best_size.height == mPreview.getCameraController().getPreviewSize().height );
	}

	private void checkOptimalVideoPictureSize(double targetRatio) {
        // even the picture resolution should have same aspect ratio for video - otherwise have problems on Nexus 7 with Android 4.4.3
		Log.d(TAG, "video picture size: " + mPreview.getCameraController().getPictureSize().width + ", " + mPreview.getCameraController().getPictureSize().height);
        List<CameraController.Size> sizes = mPreview.getSupportedPictureSizes();
    	CameraController.Size best_size = mPreview.getOptimalVideoPictureSize(sizes, targetRatio);
		Log.d(TAG, "best size: " + best_size.width + ", " + best_size.height);
    	assertTrue( best_size.width == mPreview.getCameraController().getPictureSize().width );
    	assertTrue( best_size.height == mPreview.getCameraController().getPictureSize().height );
	}

	private void checkSquareAspectRatio() {
		Log.d(TAG, "preview size: " + mPreview.getCameraController().getPreviewSize().width + ", " + mPreview.getCameraController().getPreviewSize().height);
		Log.d(TAG, "frame size: " + mPreview.getView().getWidth() + ", " + mPreview.getView().getHeight());
		double frame_aspect_ratio = ((double)mPreview.getView().getWidth()) / (double)mPreview.getView().getHeight();
		double preview_aspect_ratio = ((double)mPreview.getCameraController().getPreviewSize().width) / (double)mPreview.getCameraController().getPreviewSize().height;
		Log.d(TAG, "frame_aspect_ratio: " + frame_aspect_ratio);
		Log.d(TAG, "preview_aspect_ratio: " + preview_aspect_ratio);
		// we calculate etol like this, due to errors from rounding
		//double etol = 1.0f / Math.min((double)mPreview.getWidth(), (double)mPreview.getHeight()) + 1.0e-5;
		double etol = (double)mPreview.getView().getWidth() / (double)(mPreview.getView().getHeight() * (mPreview.getView().getHeight()-1) ) + 1.0e-5;
		assertTrue( Math.abs(frame_aspect_ratio - preview_aspect_ratio) <= etol );
	}
	
	/* Ensures that preview resolution is set as expected in non-WYSIWYG mode
	 */
	public void testPreviewSize() {
		Log.d(TAG, "testPreviewSize");

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getPreviewSizePreferenceKey(), "preference_preview_size_display");
		editor.apply();
		updateForSettings();

        Point display_size = new Point();
        {
            Display display = mActivity.getWindowManager().getDefaultDisplay();
            display.getSize(display_size);
			Log.d(TAG, "display_size: " + display_size.x + " x " + display_size.y);
        }
        //double targetRatio = mPreview.getTargetRatioForPreview(display_size);
        double targetRatio = mPreview.getTargetRatio();
        double expTargetRatio = ((double)display_size.x) / (double)display_size.y;
        assertTrue( Math.abs(targetRatio - expTargetRatio) <= 1.0e-5 );
        checkOptimalPreviewSize();
		checkSquareAspectRatio();

		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
			Log.d(TAG, "switch camera");
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    clickView(switchCameraButton);

	        //targetRatio = mPreview.getTargetRatioForPreview(display_size);
	        targetRatio = mPreview.getTargetRatio();
	        assertTrue( Math.abs(targetRatio - expTargetRatio) <= 1.0e-5 );
	        checkOptimalPreviewSize();
			checkSquareAspectRatio();
		}
	}

	/* Ensures that preview resolution is set as expected in WYSIWYG mode
	 */
	public void testPreviewSizeWYSIWYG() {
		Log.d(TAG, "testPreviewSizeWYSIWYG");

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getPreviewSizePreferenceKey(), "preference_preview_size_wysiwyg");
		editor.apply();
		updateForSettings();

        Point display_size = new Point();
        {
            Display display = mActivity.getWindowManager().getDefaultDisplay();
            display.getSize(display_size);
			Log.d(TAG, "display_size: " + display_size.x + " x " + display_size.y);
        }
        CameraController.Size picture_size = mPreview.getCameraController().getPictureSize();
        CameraController.Size preview_size = mPreview.getCameraController().getPreviewSize();
        //double targetRatio = mPreview.getTargetRatioForPreview(display_size);
        double targetRatio = mPreview.getTargetRatio();
        double expTargetRatio = ((double)picture_size.width) / (double)picture_size.height;
        double previewRatio = ((double)preview_size.width) / (double)preview_size.height;
        assertTrue( Math.abs(targetRatio - expTargetRatio) <= 1.0e-5 );
        assertTrue( Math.abs(previewRatio - expTargetRatio) <= 1.0e-5 );
        checkOptimalPreviewSize();
		checkSquareAspectRatio();

		Log.d(TAG, "switch to video");
	    View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
	    clickView(switchVideoButton);
	    assertTrue(mPreview.isVideo());
    	CamcorderProfile profile = mPreview.getCamcorderProfile();
        CameraController.Size video_preview_size = mPreview.getCameraController().getPreviewSize();
        //targetRatio = mPreview.getTargetRatioForPreview(display_size);
        targetRatio = mPreview.getTargetRatio();
        expTargetRatio = ((double)profile.videoFrameWidth) / (double)profile.videoFrameHeight;
        previewRatio = ((double)video_preview_size.width) / (double)video_preview_size.height;
        assertTrue( Math.abs(targetRatio - expTargetRatio) <= 1.0e-5 );
        assertTrue( Math.abs(previewRatio - expTargetRatio) <= 1.0e-5 );
        checkOptimalPreviewSize();
		checkSquareAspectRatio();
        checkOptimalVideoPictureSize(expTargetRatio);

	    clickView(switchVideoButton);
	    assertTrue(!mPreview.isVideo());
        CameraController.Size new_picture_size = mPreview.getCameraController().getPictureSize();
        CameraController.Size new_preview_size = mPreview.getCameraController().getPreviewSize();
	    Log.d(TAG, "picture_size: " + picture_size.width + " x " + picture_size.height);
	    Log.d(TAG, "new_picture_size: " + new_picture_size.width + " x " + new_picture_size.height);
	    Log.d(TAG, "preview_size: " + preview_size.width + " x " + preview_size.height);
	    Log.d(TAG, "new_preview_size: " + new_preview_size.width + " x " + new_preview_size.height);
	    assertTrue(new_picture_size.equals(picture_size));
	    assertTrue(new_preview_size.equals(preview_size));

		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
			Log.d(TAG, "switch camera");
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    clickView(switchCameraButton);

	        picture_size = mPreview.getCameraController().getPictureSize();
	        preview_size = mPreview.getCameraController().getPreviewSize();
	        //targetRatio = mPreview.getTargetRatioForPreview(display_size);
	        targetRatio = mPreview.getTargetRatio();
	        expTargetRatio = ((double)picture_size.width) / (double)picture_size.height;
	        previewRatio = ((double)preview_size.width) / (double)preview_size.height;
	        assertTrue( Math.abs(targetRatio - expTargetRatio) <= 1.0e-5 );
	        assertTrue( Math.abs(previewRatio - expTargetRatio) <= 1.0e-5 );
	        checkOptimalPreviewSize();
			checkSquareAspectRatio();
			
			Log.d(TAG, "switch to video again");
		    clickView(switchVideoButton);
		    assertTrue(mPreview.isVideo());
	    	profile = mPreview.getCamcorderProfile();
	        video_preview_size = mPreview.getCameraController().getPreviewSize();
		    //targetRatio = mPreview.getTargetRatioForPreview(display_size);
		    targetRatio = mPreview.getTargetRatio();
	        expTargetRatio = ((double)profile.videoFrameWidth) / (double)profile.videoFrameHeight;
	        previewRatio = ((double)video_preview_size.width) / (double)video_preview_size.height;
	        assertTrue( Math.abs(targetRatio - expTargetRatio) <= 1.0e-5 );
	        assertTrue( Math.abs(previewRatio - expTargetRatio) <= 1.0e-5 );
	        checkOptimalPreviewSize();
			checkSquareAspectRatio();
	        checkOptimalVideoPictureSize(expTargetRatio);

		    clickView(switchVideoButton);
		    assertTrue(!mPreview.isVideo());
	        new_picture_size = mPreview.getCameraController().getPictureSize();
	        new_preview_size = mPreview.getCameraController().getPreviewSize();
		    assertTrue(new_picture_size.equals(picture_size));
		    assertTrue(new_preview_size.equals(preview_size));
		}
	}

	/* Tests camera error handling.
	 */
	public void testOnError() {
		Log.d(TAG, "testOnError");
		setToDefault();

		mActivity.runOnUiThread(new Runnable() {
			public void run() {
				Log.d(TAG, "onError...");
				mPreview.getCameraController().onError();
			}
		});
		this.getInstrumentation().waitForIdleSync();
		assertTrue( mPreview.getCameraController() == null );
	}

	/* Various tests for auto-focus.
	 */
	public void testAutoFocus() throws InterruptedException {
		Log.d(TAG, "testAutoFocus");
	    if( !mPreview.supportsFocus() ) {
	    	return;
	    }
		//int saved_count = mPreview.count_cameraAutoFocus;
	    int saved_count = 0; // set to 0 rather than count_cameraAutoFocus, as on Galaxy Nexus, it can happen that startup autofocus has already occurred by the time we reach here
	    Log.d(TAG, "saved_count: " + saved_count);
		setToDefault();
		switchToFocusValue("focus_mode_auto");

		assertTrue(!mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);

		Thread.sleep(1000); // wait until autofocus startup
	    Log.d(TAG, "1 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus + " compare to saved_count: " + saved_count);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
		assertTrue(!mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);

		// touch to auto-focus with focus area
	    saved_count = mPreview.count_cameraAutoFocus;
		TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
		Log.d(TAG, "2 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
		assertTrue(mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() != null);
	    assertTrue(mPreview.getCameraController().getFocusAreas().size() == 1);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);

	    saved_count = mPreview.count_cameraAutoFocus;
	    // test selecting same mode doesn't set off an autofocus or reset the focus area
		switchToFocusValue("focus_mode_auto");
		Log.d(TAG, "3 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count);
		assertTrue(mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() != null);
	    assertTrue(mPreview.getCameraController().getFocusAreas().size() == 1);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);

	    saved_count = mPreview.count_cameraAutoFocus;
	    // test switching mode sets off an autofocus, and resets the focus area
		switchToFocusValue("focus_mode_macro");
		Log.d(TAG, "4 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
		assertTrue(!mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);

	    saved_count = mPreview.count_cameraAutoFocus;
	    // switching to focus locked shouldn't set off an autofocus
		switchToFocusValue("focus_mode_locked");
		Log.d(TAG, "5 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count);

		saved_count = mPreview.count_cameraAutoFocus;
		// touch to focus should autofocus
		TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
		Log.d(TAG, "6 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);

		saved_count = mPreview.count_cameraAutoFocus;
	    // switching to focus continuous shouldn't set off an autofocus
		switchToFocusValue("focus_mode_continuous_picture");
		Log.d(TAG, "7 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(!mPreview.isFocusWaiting());
		assertTrue(mPreview.count_cameraAutoFocus == saved_count);

		// but touch to focus should
		TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
		Log.d(TAG, "8 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
		assertTrue(mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() != null);
	    assertTrue(mPreview.getCameraController().getFocusAreas().size() == 1);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);
	    
		switchToFocusValue("focus_mode_locked"); // change to a mode that isn't auto (so that the first iteration of the next loop will set of an autofocus, due to changing the focus mode)
		List<String> supported_focus_values = mPreview.getSupportedFocusValues();
		assertTrue( supported_focus_values != null );
		assertTrue( supported_focus_values.size() > 1 );
		for(String supported_focus_value : supported_focus_values) {
			Log.d(TAG, "supported_focus_value: " + supported_focus_value);
		    saved_count = mPreview.count_cameraAutoFocus;
			Log.d(TAG, "saved autofocus count: " + saved_count);
		    //View focusModeButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.focus_mode);
		    //clickView(focusModeButton);
		    switchToFocusValue(supported_focus_value);
		    // test that switching focus mode resets the focus area
			assertTrue(!mPreview.hasFocusArea());
		    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
		    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);
		    // test that switching focus mode sets off an autofocus in focus auto or macro mode
		    String focus_value = mPreview.getCameraController().getFocusValue();
			Log.d(TAG, "changed focus_value to: "+ focus_value);
			Log.d(TAG, "count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
			if( focus_value.equals("focus_mode_auto") || focus_value.equals("focus_mode_macro") ) {
				assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
			}
			else {
				assertTrue(!mPreview.isFocusWaiting());
				assertTrue(mPreview.count_cameraAutoFocus == saved_count);
			}

		    // test that touch to auto-focus region only works in focus auto, macro or continuous mode, and that we set off an autofocus for focus auto and macro
			// test that touch to set metering area works in any focus mode
		    saved_count = mPreview.count_cameraAutoFocus;
			TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
			Log.d(TAG, "count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
			if( focus_value.equals("focus_mode_auto") || focus_value.equals("focus_mode_macro") || focus_value.equals("focus_mode_continuous_picture") || focus_value.equals("focus_mode_continuous_video") ) {
				if( focus_value.equals("focus_mode_continuous_picture") || focus_value.equals("focus_mode_continuous_video") ) {
					assertTrue(!mPreview.isFocusWaiting());
					assertTrue(mPreview.count_cameraAutoFocus == saved_count);
				}
				else {
					assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
				}
				assertTrue(mPreview.hasFocusArea());
			    assertTrue(mPreview.getCameraController().getFocusAreas() != null);
			    assertTrue(mPreview.getCameraController().getFocusAreas().size() == 1);
			    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
			    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);
			}
			else {
				assertTrue(mPreview.count_cameraAutoFocus == saved_count);
				assertTrue(!mPreview.hasFocusArea());
			    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
			    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
			    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);
			}
			// also check that focus mode is unchanged
		    assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));
			if( focus_value.equals("focus_mode_auto") ) {
				break;
			}
	    }
	}

	/* Test we do startup autofocus as expected depending on focus mode.
	 */
	public void testStartupAutoFocus() throws InterruptedException {
		Log.d(TAG, "testStartupAutoFocus");
	    if( !mPreview.supportsFocus() ) {
	    	return;
	    }
		//int saved_count = mPreview.count_cameraAutoFocus;
	    int saved_count = 0; // set to 0 rather than count_cameraAutoFocus, as on Galaxy Nexus, it can happen that startup autofocus has already occurred by the time we reach here
	    Log.d(TAG, "saved_count: " + saved_count);
		setToDefault();
		switchToFocusValue("focus_mode_auto");

		Thread.sleep(1000);
	    Log.d(TAG, "1 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus + " compare to saved_count: " + saved_count);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);

	    restart();
		//saved_count = mPreview.count_cameraAutoFocus;
	    saved_count = 0;
	    Log.d(TAG, "saved_count: " + saved_count);
		Thread.sleep(1000);
	    Log.d(TAG, "2 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus + " compare to saved_count: " + saved_count);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);

		if( mPreview.getSupportedFocusValues().contains("focus_mode_infinity") ) {
			switchToFocusValue("focus_mode_infinity");
		    restart();
			//saved_count = mPreview.count_cameraAutoFocus;
		    saved_count = 0;
		    Log.d(TAG, "saved_count: " + saved_count);
			Thread.sleep(1000);
		    Log.d(TAG, "3 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus + " compare to saved_count: " + saved_count);
			assertTrue(mPreview.count_cameraAutoFocus == saved_count);
		}

		if( mPreview.getSupportedFocusValues().contains("focus_mode_macro") ) {
			switchToFocusValue("focus_mode_macro");
		    restart();
			//saved_count = mPreview.count_cameraAutoFocus;
		    saved_count = 0;
		    Log.d(TAG, "saved_count: " + saved_count);
			Thread.sleep(1000);
		    Log.d(TAG, "4 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus + " compare to saved_count: " + saved_count);
			assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
		}
		
		if( mPreview.getSupportedFocusValues().contains("focus_mode_locked") ) {
			switchToFocusValue("focus_mode_locked");
		    restart();
			//saved_count = mPreview.count_cameraAutoFocus;
		    saved_count = 0;
		    Log.d(TAG, "saved_count: " + saved_count);
			Thread.sleep(1000);
		    Log.d(TAG, "5 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus + " compare to saved_count: " + saved_count);
			assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
		}
		
		if( mPreview.getSupportedFocusValues().contains("focus_mode_continuous_picture") ) {
			switchToFocusValue("focus_mode_continuous_picture");
		    restart();
			//saved_count = mPreview.count_cameraAutoFocus;
		    saved_count = 0;
		    Log.d(TAG, "saved_count: " + saved_count);
			Thread.sleep(1000);
		    Log.d(TAG, "6 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus + " compare to saved_count: " + saved_count);
			assertTrue(mPreview.count_cameraAutoFocus == saved_count);
		}
	}
	
	/* Test doing touch to auto-focus region by swiping to all four corners works okay.
	 */
	public void testAutoFocusCorners() {
		Log.d(TAG, "testAutoFocusCorners");
	    if( !mPreview.supportsFocus() ) {
	    	return;
	    }
		setToDefault();
		int [] gui_location = new int[2];
		mPreview.getView().getLocationOnScreen(gui_location);
		final int step_dist_c = 2;
		final float scale = mActivity.getResources().getDisplayMetrics().density;
		final int large_step_dist_c = (int) (60 * scale + 0.5f); // convert dps to pixels
		final int step_count_c = 10;
		int width = mPreview.getView().getWidth();
		int height = mPreview.getView().getHeight();
		Log.d(TAG, "preview size: " + width + " x " + height);

		assertTrue(!mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);

		Log.d(TAG, "top-left");
	    TouchUtils.drag(MainActivityTest.this, gui_location[0] + step_dist_c, gui_location[0], gui_location[1] + step_dist_c, gui_location[1], step_count_c);
		assertTrue(mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() != null);
	    assertTrue(mPreview.getCameraController().getFocusAreas().size() == 1);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);

	    mPreview.clearFocusAreas();
		assertTrue(!mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);
	    
		// do larger step at top-right, due to conflicting with Settings button
		// but we now ignore swipes - so we now test for that instead
		Log.d(TAG, "top-right");
	    TouchUtils.drag(MainActivityTest.this, gui_location[0]+width-1-large_step_dist_c, gui_location[0]+width-1, gui_location[1]+large_step_dist_c, gui_location[1], step_count_c);
		assertTrue(!mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);

		Log.d(TAG, "bottom-left");
	    TouchUtils.drag(MainActivityTest.this, gui_location[0]+step_dist_c, gui_location[0], gui_location[1]+height-1-step_dist_c, gui_location[1]+height-1, step_count_c);
		assertTrue(mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() != null);
	    assertTrue(mPreview.getCameraController().getFocusAreas().size() == 1);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);

	    mPreview.clearFocusAreas();
		assertTrue(!mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);

	    // skip bottom right, conflicts with zoom on various devices
	}

	/* Test face detection, and that we don't get the focus/metering areas set.
	 */
	public void testFaceDetection() throws InterruptedException {
		Log.d(TAG, "testFaceDetection");
	    if( !mPreview.supportsFaceDetection() ) {
			Log.d(TAG, "face detection not supported");
	    	return;
	    }
		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getFaceDetectionPreferenceKey(), true);
		editor.apply();
		updateForSettings();

		int saved_count = mPreview.count_cameraAutoFocus;
		Log.d(TAG, "0 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		// autofocus shouldn't be immediately, but after a delay
		Thread.sleep(1000);
		Log.d(TAG, "1 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
		assertTrue(!mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);
	    boolean face_detection_started = false;
	    if( !mPreview.getCameraController().startFaceDetection() ) {
	    	// should throw RuntimeException if face detection already started
	    	face_detection_started = true;
		}
	    assertTrue(face_detection_started);

		// touch to auto-focus with focus area
	    saved_count = mPreview.count_cameraAutoFocus;
		TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
		Log.d(TAG, "2 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
		assertTrue(!mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);

		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    clickView(switchCameraButton);
		    face_detection_started = false;
		    if( !mPreview.getCameraController().startFaceDetection() ) {
		    	// should throw RuntimeException if face detection already started
		    	face_detection_started = true;
			}
		    assertTrue(face_detection_started);
		}
	}

	private void subTestPopupButtonAvailability(String test_key, String option, boolean expected) {
		View button = mActivity.getPopupButton(test_key + "_" + option);
		if( expected ) {
			boolean is_video = mPreview.isVideo();
			if( option.equals("focus_mode_continuous_picture") && is_video ) {
				// not allowed in video mode
				assertTrue(button == null);
			}
			else if( option.equals("focus_mode_continuous_video") && !is_video ) {
				// not allowed in picture mode
				assertTrue(button == null);
			}
			else {
				assertTrue(button != null);
			}
		}
		else {
			Log.d(TAG, "option? "+ option);
			Log.d(TAG, "button? "+ button);
			assertTrue(button == null);
		}
	}

	private void subTestPopupButtonAvailability(String test_key, String option, List<String> options) {
		subTestPopupButtonAvailability(test_key, option, options != null && options.contains(option));
	}
	
	private void subTestPopupButtonAvailability(String option, boolean expected) {
	    View button = mActivity.getPopupButton(option);
	    if( expected ) {
	    	assertTrue(button != null);
	    }
	    else {
	    	assertTrue(button == null);
	    }
	}
	
	private void subTestPopupButtonAvailability() {
		List<String> supported_flash_values = mPreview.getSupportedFlashValues();
		subTestPopupButtonAvailability("TEST_FLASH", "flash_off", supported_flash_values);
		subTestPopupButtonAvailability("TEST_FLASH", "flash_auto", supported_flash_values);
		subTestPopupButtonAvailability("TEST_FLASH", "flash_on", supported_flash_values);
		subTestPopupButtonAvailability("TEST_FLASH", "flash_torch", supported_flash_values);
		subTestPopupButtonAvailability("TEST_FLASH", "flash_red_eye", supported_flash_values);
		List<String> supported_focus_values = mPreview.getSupportedFocusValues();
		subTestPopupButtonAvailability("TEST_FOCUS", "focus_mode_auto", supported_focus_values);
		subTestPopupButtonAvailability("TEST_FOCUS", "focus_mode_locked", supported_focus_values);
		subTestPopupButtonAvailability("TEST_FOCUS", "focus_mode_infinity", supported_focus_values);
		subTestPopupButtonAvailability("TEST_FOCUS", "focus_mode_macro", supported_focus_values);
		subTestPopupButtonAvailability("TEST_FOCUS", "focus_mode_fixed", supported_focus_values);
		subTestPopupButtonAvailability("TEST_FOCUS", "focus_mode_edof", supported_focus_values);		
		subTestPopupButtonAvailability("TEST_FOCUS", "focus_mode_continuous_picture", supported_focus_values);
		subTestPopupButtonAvailability("TEST_FOCUS", "focus_mode_continuous_video", supported_focus_values);
		if( mPreview.supportsISORange() ) {
			subTestPopupButtonAvailability("TEST_ISO", "auto", true);
			subTestPopupButtonAvailability("TEST_ISO", "100", true);
			subTestPopupButtonAvailability("TEST_ISO", "200", true);
			subTestPopupButtonAvailability("TEST_ISO", "400", true);
			subTestPopupButtonAvailability("TEST_ISO", "800", true);
			subTestPopupButtonAvailability("TEST_ISO", "1600", true);
		}
		else {
			List<String> supported_iso_values = mPreview.getSupportedISOs();
			subTestPopupButtonAvailability("TEST_ISO", "auto", supported_iso_values);
			subTestPopupButtonAvailability("TEST_ISO", "100", supported_iso_values);
			subTestPopupButtonAvailability("TEST_ISO", "200", supported_iso_values);
			subTestPopupButtonAvailability("TEST_ISO", "400", supported_iso_values);
			subTestPopupButtonAvailability("TEST_ISO", "800", supported_iso_values);
			subTestPopupButtonAvailability("TEST_ISO", "1600", supported_iso_values);
		}
		subTestPopupButtonAvailability("TEST_WHITE_BALANCE", mPreview.getSupportedWhiteBalances() != null);
		subTestPopupButtonAvailability("TEST_SCENE_MODE", mPreview.getSupportedSceneModes() != null);
		subTestPopupButtonAvailability("TEST_COLOR_EFFECT", mPreview.getSupportedColorEffects() != null);
	}
	
	private void subTestFocusFlashAvailability() {
	    //View focusModeButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.focus_mode);
	    //View flashButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.flash);
	    View exposureButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure);
	    View exposureLockButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure_lock);
	    View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);
	    /*boolean focus_visible = focusModeButton.getVisibility() == View.VISIBLE;
		Log.d(TAG, "focus_visible? "+ focus_visible);
	    boolean flash_visible = flashButton.getVisibility() == View.VISIBLE;
		Log.d(TAG, "flash_visible? "+ flash_visible);*/
	    boolean exposure_visible = exposureButton.getVisibility() == View.VISIBLE;
		Log.d(TAG, "exposure_visible? "+ exposure_visible);
	    boolean exposure_lock_visible = exposureLockButton.getVisibility() == View.VISIBLE;
		Log.d(TAG, "exposure_lock_visible? "+ exposure_lock_visible);
	    boolean popup_visible = popupButton.getVisibility() == View.VISIBLE;
		Log.d(TAG, "popup_visible? "+ popup_visible);
		boolean has_focus = mPreview.supportsFocus();
		Log.d(TAG, "has_focus? "+ has_focus);
		boolean has_flash = mPreview.supportsFlash();
		Log.d(TAG, "has_flash? "+ has_flash);
		boolean has_exposure = mPreview.supportsExposures();
		Log.d(TAG, "has_exposure? "+ has_exposure);
		boolean has_exposure_lock = mPreview.supportsExposureLock();
		Log.d(TAG, "has_exposure_lock? "+ has_exposure_lock);
		//assertTrue(has_focus == focus_visible);
		//assertTrue(has_flash == flash_visible);
		assertTrue(has_exposure == exposure_visible);
		assertTrue(has_exposure_lock == exposure_lock_visible);
		assertTrue(popup_visible);
		
	    clickView(popupButton);
	    while( !mActivity.popupIsOpen() ) {
	    }
	    subTestPopupButtonAvailability();
	}

	/*
	 * For each camera, test that visibility of flash and focus etc buttons matches the availability of those camera parameters.
	 * Added to guard against a bug where on Nexus 7, the flash and focus buttons were made visible by showGUI, even though they aren't supported by Nexus 7 front camera.
	 */
	public void testFocusFlashAvailability() {
		Log.d(TAG, "testFocusFlashAvailability");
		setToDefault();

		subTestFocusFlashAvailability();

		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
			int cameraId = mPreview.getCameraId();
			Log.d(TAG, "cameraId? "+ cameraId);
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    //mActivity.clickedSwitchCamera(switchCameraButton);
		    clickView(switchCameraButton);
			int new_cameraId = mPreview.getCameraId();
			Log.d(TAG, "new_cameraId? "+ new_cameraId);
			assertTrue(cameraId != new_cameraId);

		    subTestFocusFlashAvailability();
		}
	}

	/* Tests switching to/from video mode, for front and back cameras, and tests the focus mode changes as expected.
	 */
	public void testSwitchVideo() throws InterruptedException {
		Log.d(TAG, "testSwitchVideo");

		setToDefault();
		assertTrue(!mPreview.isVideo());

	    View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
	    clickView(switchVideoButton);
		assertTrue(mPreview.isVideo());
	    String focus_value = mPreview.getCameraController().getFocusValue();
		Log.d(TAG, "video focus_value: "+ focus_value);
	    if( mPreview.supportsFocus() ) {
	    	assertTrue(focus_value.equals("focus_mode_continuous_video"));
	    }

	    int saved_count = mPreview.count_cameraAutoFocus;
	    Log.d(TAG, "0 count_cameraAutoFocus: " + saved_count);
	    clickView(switchVideoButton);
		assertTrue(!mPreview.isVideo());
	    focus_value = mPreview.getCameraController().getFocusValue();
		Log.d(TAG, "picture focus_value: "+ focus_value);
	    if( mPreview.supportsFocus() ) {
	    	assertTrue(focus_value.equals("focus_mode_auto"));
	    	// check that this doesn't cause an autofocus
	    	assertTrue(!mPreview.isFocusWaiting());
		    Log.d(TAG, "1 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
			assertTrue(mPreview.count_cameraAutoFocus == saved_count);
	    }

		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
			int cameraId = mPreview.getCameraId();
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    clickView(switchCameraButton);
			int new_cameraId = mPreview.getCameraId();
			assertTrue(cameraId != new_cameraId);
		    focus_value = mPreview.getCameraController().getFocusValue();
			Log.d(TAG, "front picture focus_value: "+ focus_value);
		    if( mPreview.supportsFocus() ) {
		    	assertTrue(focus_value.equals("focus_mode_auto"));
		    }

		    clickView(switchVideoButton);
			assertTrue(mPreview.isVideo());
		    focus_value = mPreview.getCameraController().getFocusValue();
			Log.d(TAG, "front video focus_value: "+ focus_value);
		    if( mPreview.supportsFocus() ) {
		    	assertTrue(focus_value.equals("focus_mode_continuous_video"));
		    }

		    clickView(switchVideoButton);
			assertTrue(!mPreview.isVideo());
		    focus_value = mPreview.getCameraController().getFocusValue();
			Log.d(TAG, "front picture focus_value: "+ focus_value);
		    if( mPreview.supportsFocus() ) {
		    	assertTrue(focus_value.equals("focus_mode_auto"));
		    }

			// now switch back
			clickView(switchCameraButton);
			new_cameraId = mPreview.getCameraId();
			assertTrue(cameraId == new_cameraId);
	    }

		if( mPreview.supportsFocus() ) {
			// now test we remember the focus mode for photo and video

			switchToFocusValue("focus_mode_continuous_picture");

			clickView(switchVideoButton);
			assertTrue(mPreview.isVideo());
			focus_value = mPreview.getCameraController().getFocusValue();
			Log.d(TAG, "video focus_value: "+ focus_value);
			assertTrue(focus_value.equals("focus_mode_continuous_video"));

			switchToFocusValue("focus_mode_macro");

			clickView(switchVideoButton);
			assertTrue(!mPreview.isVideo());
			focus_value = mPreview.getCameraController().getFocusValue();
			Log.d(TAG, "picture focus_value: "+ focus_value);
			assertTrue(focus_value.equals("focus_mode_continuous_picture"));

			clickView(switchVideoButton);
			assertTrue(mPreview.isVideo());
			focus_value = mPreview.getCameraController().getFocusValue();
			Log.d(TAG, "video focus_value: "+ focus_value);
			assertTrue(focus_value.equals("focus_mode_macro"));
		}



	}

	/* Tests continuous picture focus, including switching to video and back.
	 * Tends to fail on Galaxy Nexus, where the continuous picture focusing doesn't happen too often.
	 */
	public void testContinuousPictureFocus() throws InterruptedException {
		Log.d(TAG, "testContinuousPictureFocus");

	    if( !mPreview.supportsFocus() ) {
	    	return;
	    }

	    setToDefault();
		// first switch to auto-focus (if we're already in continuous picture mode, we might have already done the continuous focus moving
		switchToFocusValue("focus_mode_auto");
		pauseAndResume();
		switchToFocusValue("focus_mode_continuous_picture");

		// check continuous focus is working
		int saved_count_cameraContinuousFocusMoving = mPreview.count_cameraContinuousFocusMoving;
		Thread.sleep(1000);
		int new_count_cameraContinuousFocusMoving = mPreview.count_cameraContinuousFocusMoving;
		Log.d(TAG, "count_cameraContinuousFocusMoving compare saved: "+ saved_count_cameraContinuousFocusMoving + " to new: " + new_count_cameraContinuousFocusMoving);
		assertTrue( new_count_cameraContinuousFocusMoving > saved_count_cameraContinuousFocusMoving );

		// switch to video
		View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
	    clickView(switchVideoButton);
	    String focus_value = mPreview.getCameraController().getFocusValue();
		Log.d(TAG, "video focus_value: "+ focus_value);
    	assertTrue(focus_value.equals("focus_mode_continuous_video"));

		// switch to photo
	    clickView(switchVideoButton);
	    focus_value = mPreview.getCameraController().getFocusValue();
		Log.d(TAG, "video focus_value: "+ focus_value);
    	assertTrue(focus_value.equals("focus_mode_continuous_picture"));
	    
		// check continuous focus is working
		saved_count_cameraContinuousFocusMoving = mPreview.count_cameraContinuousFocusMoving;
		Thread.sleep(3000);
		new_count_cameraContinuousFocusMoving = mPreview.count_cameraContinuousFocusMoving;
		Log.d(TAG, "count_cameraContinuousFocusMoving compare saved: "+ saved_count_cameraContinuousFocusMoving + " to new: " + new_count_cameraContinuousFocusMoving);
		assertTrue( new_count_cameraContinuousFocusMoving > saved_count_cameraContinuousFocusMoving );
	}

	/* Tests everything works okay if starting in continuous video focus mode when in photo mode, including opening popup, and switching to video and back.
	 * This shouldn't be possible normal, but could happen if a user is upgrading from version 1.28 or earlier, to version 1.29 or later.
	 */
	public void testContinuousVideoFocusForPhoto() throws InterruptedException {
		Log.d(TAG, "testContinuousVideoFocusForPhoto");

	    if( !mPreview.supportsFocus() ) {
	    	return;
	    }

	    setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getFocusPreferenceKey(mPreview.getCameraId(), false), "focus_mode_continuous_video");
		editor.apply();
		restart();
		
		Thread.sleep(1000);

	    View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);
	    clickView(popupButton);
	    while( !mActivity.popupIsOpen() ) {
	    }

		Thread.sleep(1000);

		View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
		clickView(switchVideoButton);
	}

	/* Tests continuous picture focus with burst mode.
	 */
	public void testContinuousPictureFocusBurst() throws InterruptedException {
		Log.d(TAG, "testContinuousPictureFocusBurst");

	    if( !mPreview.supportsFocus() ) {
	    	return;
	    }

	    setToDefault();
		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(PreferenceKeys.getBurstModePreferenceKey(), "3");
			editor.apply();
		}
		switchToFocusValue("focus_mode_continuous_picture");

		// count initial files in folder
		File folder = mActivity.getImageFolder();
		int n_files = folder.listFiles().length;
		Log.d(TAG, "n_files at start: " + n_files);

		assertTrue(mPreview.count_cameraTakePicture==0);

		View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take photo");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take photo");
		assertTrue(!mPreview.isOnTimer());

		// wait until photos taken
		// wait 15s, and test that we've taken the photos by then
		while( mPreview.count_cameraTakePicture < 3 ) {
		}
		Thread.sleep(1500); // allow pictures to save
	    assertTrue(mPreview.isPreviewStarted()); // check preview restarted
		Log.d(TAG, "count_cameraTakePicture: " + mPreview.count_cameraTakePicture);
		assertTrue(mPreview.count_cameraTakePicture==3);
		int n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		assertTrue(n_new_files == 3);
	}

	/* Test for continuous picture photo mode.
	 * Touch, wait 8s, check that continuous focus mode has resumed, then take photo.
	 */
	public void testContinuousPicture1() throws InterruptedException {
		Log.d(TAG, "testContinuousPicture1");

	    if( !mPreview.supportsFocus() ) {
	    	return;
	    }

	    setToDefault();
		switchToFocusValue("focus_mode_continuous_picture");

		String focus_value = "focus_mode_continuous_picture";
		String focus_value_ui = "focus_mode_continuous_picture";

		// count initial files in folder
		File folder = mActivity.getImageFolder();
		int n_files = folder.listFiles().length;
		Log.d(TAG, "n_files at start: " + n_files);

		Thread.sleep(1000);
		assertTrue(mPreview.count_cameraTakePicture==0);
		assertTrue(mPreview.getCurrentFocusValue().equals(focus_value_ui));
		assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));

		Log.d(TAG, "about to click preview for autofocus");
		int saved_count = mPreview.count_cameraAutoFocus;
		TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
		Log.d(TAG, "1 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
		assertTrue(mPreview.getCurrentFocusValue().equals("focus_mode_continuous_picture"));
		assertTrue(mPreview.getCameraController().getFocusValue().equals("focus_mode_auto"));
		assertTrue(mPreview.getCurrentFocusValue().equals(focus_value_ui));
		if( focus_value.equals("focus_mode_continuous_picture") )
			assertTrue(mPreview.getCameraController().getFocusValue().equals("focus_mode_auto")); // continuous focus mode switches to auto focus on touch
		else
			assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));

		Thread.sleep(8000);
		assertTrue(mPreview.getCurrentFocusValue().equals(focus_value_ui));
		assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));

		View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take photo");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take photo");

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
	    }
		Log.d(TAG, "done taking photo");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");

		assertTrue(mPreview.getCurrentFocusValue().equals(focus_value_ui));
		assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));
		assertTrue(mPreview.count_cameraTakePicture==1);
		mActivity.waitUntilImageQueueEmpty();

		assertTrue( folder.exists() );
		int n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		assertTrue(n_new_files == 1);
	}

	/* Test for continuous picture photo mode.
	 * Touch, wait 1s, check that continuous focus mode hasn't resumed, then take photo, then check continuous focus mode has resumed.
	 */
	public void testContinuousPicture2() throws InterruptedException {
		Log.d(TAG, "testContinuousPicture1");

	    if( !mPreview.supportsFocus() ) {
	    	return;
	    }

	    setToDefault();
		switchToFocusValue("focus_mode_continuous_picture");

		String focus_value = "focus_mode_continuous_picture";
		String focus_value_ui = "focus_mode_continuous_picture";

		// count initial files in folder
		File folder = mActivity.getImageFolder();
		int n_files = folder.listFiles().length;
		Log.d(TAG, "n_files at start: " + n_files);

		Thread.sleep(1000);
		assertTrue(mPreview.count_cameraTakePicture==0);
		assertTrue(mPreview.getCurrentFocusValue().equals(focus_value_ui));
		assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));

		Log.d(TAG, "about to click preview for autofocus");
		int saved_count = mPreview.count_cameraAutoFocus;
		TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "1 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
		assertTrue(mPreview.getCurrentFocusValue().equals("focus_mode_continuous_picture"));
		assertTrue(mPreview.getCameraController().getFocusValue().equals("focus_mode_auto"));
		assertTrue(mPreview.getCurrentFocusValue().equals(focus_value_ui));
		if( focus_value.equals("focus_mode_continuous_picture") )
			assertTrue(mPreview.getCameraController().getFocusValue().equals("focus_mode_auto")); // continuous focus mode switches to auto focus on touch
		else
			assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));

		int saved_count_cameraContinuousFocusMoving = mPreview.count_cameraContinuousFocusMoving;

		Thread.sleep(1000);
		assertTrue(mPreview.getCurrentFocusValue().equals(focus_value_ui));
		if( focus_value.equals("focus_mode_continuous_picture") )
			assertTrue(mPreview.getCameraController().getFocusValue().equals("focus_mode_auto")); // continuous focus mode switches to auto focus on touch
		else
			assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));
		int new_count_cameraContinuousFocusMoving = mPreview.count_cameraContinuousFocusMoving;
		assertTrue( new_count_cameraContinuousFocusMoving == saved_count_cameraContinuousFocusMoving );
		Log.d(TAG, "2 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);

		View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take photo");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take photo");

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
	    }
		Log.d(TAG, "done taking photo");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");

		assertTrue(mPreview.getCurrentFocusValue().equals(focus_value_ui));
		assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));
		assertTrue(mPreview.count_cameraTakePicture==1);
		Log.d(TAG, "3 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
		mActivity.waitUntilImageQueueEmpty();

		assertTrue( folder.exists() );
		int n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		assertTrue(n_new_files == 1);
	}

	/* Test for continuous picture photo mode.
	 * Touch repeatedly with 1s delays for 8 times, make sure continuous focus mode hasn't resumed.
	 * Then wait 5s, and check continuous focus mode has resumed.
	 */
	public void testContinuousPictureRepeatTouch() throws InterruptedException {
		Log.d(TAG, "testContinuousPictureRepeatTouch");

	    if( !mPreview.supportsFocus() ) {
	    	return;
	    }

	    setToDefault();
		switchToFocusValue("focus_mode_continuous_picture");

		String focus_value = "focus_mode_continuous_picture";
		String focus_value_ui = "focus_mode_continuous_picture";

		Thread.sleep(1000);
		assertTrue(mPreview.getCurrentFocusValue().equals(focus_value_ui));
		assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));
		
		for(int i=0;i<8;i++) {
			Log.d(TAG, "about to click preview for autofocus: " + i);
			int saved_count = mPreview.count_cameraAutoFocus;
			TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
			this.getInstrumentation().waitForIdleSync();
			Log.d(TAG, "1 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
			assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
			int saved_count_cameraContinuousFocusMoving = mPreview.count_cameraContinuousFocusMoving;
			Thread.sleep(1000);

			assertTrue(mPreview.getCurrentFocusValue().equals("focus_mode_continuous_picture"));
			assertTrue(mPreview.getCameraController().getFocusValue().equals("focus_mode_auto"));
			assertTrue(mPreview.getCurrentFocusValue().equals(focus_value_ui));
			if( focus_value.equals("focus_mode_continuous_picture") )
				assertTrue(mPreview.getCameraController().getFocusValue().equals("focus_mode_auto")); // continuous focus mode switches to auto focus on touch
			else
				assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));
			int new_count_cameraContinuousFocusMoving = mPreview.count_cameraContinuousFocusMoving;
			assertTrue( new_count_cameraContinuousFocusMoving == saved_count_cameraContinuousFocusMoving );
		}

		int saved_count = mPreview.count_cameraAutoFocus;
		Thread.sleep(5000);
		assertTrue(mPreview.getCurrentFocusValue().equals(focus_value_ui));
		assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));
		Log.d(TAG, "2 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count);
	}

	/* Test for continuous picture photo mode.
	 * Touch, then after 1s switch to focus auto in UI, wait 8s, ensure still in autofocus mode.
	 */
	public void testContinuousPictureSwitchAuto() throws InterruptedException {
		Log.d(TAG, "testContinuousPictureSwitchAuto");

	    if( !mPreview.supportsFocus() ) {
	    	return;
	    }

	    setToDefault();
		switchToFocusValue("focus_mode_continuous_picture");

		String focus_value = "focus_mode_continuous_picture";
		String focus_value_ui = "focus_mode_continuous_picture";

		Thread.sleep(1000);
		assertTrue(mPreview.getCurrentFocusValue().equals(focus_value_ui));
		assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));
		
		Log.d(TAG, "about to click preview for autofocus");
		int saved_count = mPreview.count_cameraAutoFocus;
		TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "1 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
		int saved_count_cameraContinuousFocusMoving = mPreview.count_cameraContinuousFocusMoving;
		Thread.sleep(1000);

		assertTrue(mPreview.getCurrentFocusValue().equals(focus_value_ui));
		if( focus_value.equals("focus_mode_continuous_picture") )
			assertTrue(mPreview.getCameraController().getFocusValue().equals("focus_mode_auto")); // continuous focus mode switches to auto focus on touch
		else
			assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));
		int new_count_cameraContinuousFocusMoving = mPreview.count_cameraContinuousFocusMoving;
		assertTrue( new_count_cameraContinuousFocusMoving == saved_count_cameraContinuousFocusMoving );

		Thread.sleep(1000);
		assertTrue(mPreview.getCurrentFocusValue().equals(focus_value_ui));
		if( focus_value.equals("focus_mode_continuous_picture") )
			assertTrue(mPreview.getCameraController().getFocusValue().equals("focus_mode_auto")); // continuous focus mode switches to auto focus on touch
		else
			assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));
		new_count_cameraContinuousFocusMoving = mPreview.count_cameraContinuousFocusMoving;
		assertTrue( new_count_cameraContinuousFocusMoving == saved_count_cameraContinuousFocusMoving );

		switchToFocusValue("focus_mode_auto");
		assertTrue(mPreview.getCurrentFocusValue().equals("focus_mode_auto"));
		assertTrue(mPreview.getCameraController().getFocusValue().equals("focus_mode_auto"));
		new_count_cameraContinuousFocusMoving = mPreview.count_cameraContinuousFocusMoving;
		assertTrue( new_count_cameraContinuousFocusMoving == saved_count_cameraContinuousFocusMoving );

		Thread.sleep(8000);
		assertTrue(mPreview.getCurrentFocusValue().equals("focus_mode_auto"));
		assertTrue(mPreview.getCameraController().getFocusValue().equals("focus_mode_auto"));
		new_count_cameraContinuousFocusMoving = mPreview.count_cameraContinuousFocusMoving;
		assertTrue( new_count_cameraContinuousFocusMoving == saved_count_cameraContinuousFocusMoving );
	}

	/* Start in photo mode with auto focus:
	 * - go to video mode
	 * - then switch to front camera
	 * - then stop video
	 * - then go to back camera
	 * Check focus mode has returned to auto.
	 * This test is important when front camera doesn't support focus modes, but back camera does - we won't be able to reset to auto focus for the front camera, but need to do so when returning to back camera
	 */
	public void testFocusSwitchVideoSwitchCameras() {
		Log.d(TAG, "testFocusSwitchVideoSwitchCameras");

		if( mPreview.getCameraControllerManager().getNumberOfCameras() <= 1 ) {
			return;
		}

	    if( !mPreview.supportsFocus() ) {
	    	return;
	    }

		setToDefault();

	    View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
	    clickView(switchVideoButton);
	    String focus_value = mPreview.getCameraController().getFocusValue();
		Log.d(TAG, "video focus_value: "+ focus_value);
	    assertTrue(focus_value.equals("focus_mode_continuous_video"));

	    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    clickView(switchCameraButton);
	    // camera becomes invalid when switching cameras
	    focus_value = mPreview.getCameraController().getFocusValue();
		Log.d(TAG, "front video focus_value: "+ focus_value);
		// don't care when focus mode is for front camera (focus may not be supported for front camera)

	    clickView(switchVideoButton);
	    focus_value = mPreview.getCameraController().getFocusValue();
		Log.d(TAG, "front focus_value: "+ focus_value);
		// don't care when focus mode is for front camera (focus may not be supported for front camera)

	    clickView(switchCameraButton);
	    // camera becomes invalid when switching cameras
	    focus_value = mPreview.getCameraController().getFocusValue();
		Log.d(TAG, "end focus_value: "+ focus_value);
	    assertTrue(focus_value.equals("focus_mode_auto"));
	}

	/* Start in photo mode with focus macro:
	 * - switch to front camera
	 * - switch to back camera
	 * Check focus mode is still macro.
	 * This test is important when front camera doesn't support focus modes, but back camera does - need to remain in macro mode for the back camera.
	 */
	public void testFocusRemainMacroSwitchCamera() {
		Log.d(TAG, "testFocusRemainMacroSwitchCamera");

		if( mPreview.getCameraControllerManager().getNumberOfCameras() <= 1 ) {
			return;
		}

	    if( !mPreview.supportsFocus() ) {
	    	return;
	    }

		setToDefault();
		switchToFocusValue("focus_mode_macro");

	    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    // n.b., call twice, to switch to front then to back
	    clickView(switchCameraButton);
	    clickView(switchCameraButton);

	    String focus_value = mPreview.getCameraController().getFocusValue();
		Log.d(TAG, "focus_value: "+ focus_value);
	    assertTrue(focus_value.equals("focus_mode_macro"));
	}

	/* Start in photo mode with focus auto:
	 * - switch to video mode
	 * - switch to focus macro
	 * - switch to picture mode
	 * Check focus mode is now auto.
	 * As of 1.26, we now remember the focus mode for photos.
	 */
	public void testFocusRemainMacroSwitchPhoto() {
		Log.d(TAG, "testFocusRemainMacroSwitchPhoto");

	    if( !mPreview.supportsFocus() ) {
	    	return;
	    }

		setToDefault();

	    View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
	    clickView(switchVideoButton);
	    String focus_value = mPreview.getCameraController().getFocusValue();
		Log.d(TAG, "focus_value after switching to video mode: "+ focus_value);
	    assertTrue(focus_value.equals("focus_mode_continuous_video"));

		switchToFocusValue("focus_mode_macro");

	    clickView(switchVideoButton);

	    focus_value = mPreview.getCameraController().getFocusValue();
		Log.d(TAG, "focus_value after switching to picture mode: " + focus_value);
	    assertTrue(focus_value.equals("focus_mode_auto"));
	}

	/* Start in photo mode with focus auto:
	 * - switch to focus macro
	 * - switch to video mode
	 * - switch to picture mode
	 * Check focus mode is still macro.
	 * As of 1.26, we now remember the focus mode for photos.
	 */
	public void testFocusSaveMacroSwitchPhoto() {
		Log.d(TAG, "testFocusSaveMacroSwitchPhoto");

	    if( !mPreview.supportsFocus() ) {
	    	return;
	    }

		setToDefault();

		switchToFocusValue("focus_mode_macro");

	    View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
	    clickView(switchVideoButton);
	    String focus_value = mPreview.getCameraController().getFocusValue();
		Log.d(TAG, "focus_value after switching to video mode: "+ focus_value);
	    assertTrue(focus_value.equals("focus_mode_continuous_video"));

	    clickView(switchVideoButton);

	    focus_value = mPreview.getCameraController().getFocusValue();
		Log.d(TAG, "focus_value after switching to picture mode: " + focus_value);
	    assertTrue(focus_value.equals("focus_mode_macro"));
	}

	/* Start in photo mode with auto focus:
	 * - go to video mode
	 * - check in continuous focus mode
	 * - switch to auto focus mode
	 * - then pause and resume
	 * - then check still in video mode, still in auto focus mode
	 * - then repeat with restarting instead
	 * (Note the name is a bit misleading - it used to be that we reset to continuous mode, now we don't.)
	 */
	public void testFocusSwitchVideoResetContinuous() {
		Log.d(TAG, "testFocusSwitchVideoResetContinuous");

	    if( !mPreview.supportsFocus() ) {
	    	return;
	    }

		setToDefault();
		switchToFocusValue("focus_mode_auto");

	    View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
	    clickView(switchVideoButton);
	    String focus_value = mPreview.getCameraController().getFocusValue();
	    assertTrue(focus_value.equals("focus_mode_continuous_video"));

		switchToFocusValue("focus_mode_auto");
	    focus_value = mPreview.getCameraController().getFocusValue();
	    assertTrue(focus_value.equals("focus_mode_auto"));

	    this.pauseAndResume();
	    assertTrue(mPreview.isVideo());
	    
	    focus_value = mPreview.getCameraController().getFocusValue();
	    assertTrue(focus_value.equals("focus_mode_auto"));

	    // now with restart

		switchToFocusValue("focus_mode_auto");
	    focus_value = mPreview.getCameraController().getFocusValue();
	    assertTrue(focus_value.equals("focus_mode_auto"));

	    restart();
	    assertTrue(mPreview.isVideo());

	    focus_value = mPreview.getCameraController().getFocusValue();
	    assertTrue(focus_value.equals("focus_mode_auto"));
	}

	public void testTakePhotoExposureCompensation() throws InterruptedException {
		Log.d(TAG, "testTakePhotoExposureCompensation");
		setToDefault();
		
	    View exposureButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure);
		View exposureContainer = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure_container);
		SeekBar seekBar = (SeekBar) mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure_seekbar);
	    ZoomControls seekBarZoom = (ZoomControls) mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure_seekbar_zoom);
	    assertTrue(exposureButton.getVisibility() == (mPreview.supportsExposures() ? View.VISIBLE : View.GONE));
	    assertTrue(exposureContainer.getVisibility() == View.GONE);
	    assertTrue(seekBarZoom.getVisibility() == View.GONE);
	    
	    if( !mPreview.supportsExposures() ) {
	    	return;
	    }

	    clickView(exposureButton);
	    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureContainer.getVisibility() == View.VISIBLE);
	    assertTrue(seekBarZoom.getVisibility() == View.VISIBLE);

	    assertTrue( mPreview.getMaximumExposure() - mPreview.getMinimumExposure() == seekBar.getMax() );
	    assertTrue( mPreview.getCurrentExposure() - mPreview.getMinimumExposure() == seekBar.getProgress() );
		Log.d(TAG, "change exposure to 1");
	    mActivity.changeExposure(1);
		this.getInstrumentation().waitForIdleSync();
	    assertTrue( mPreview.getCurrentExposure() == 1 );
	    assertTrue( mPreview.getCurrentExposure() - mPreview.getMinimumExposure() == seekBar.getProgress() );
		Log.d(TAG, "set exposure to min");
	    seekBar.setProgress(0);
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "actual exposure is now " + mPreview.getCurrentExposure());
		Log.d(TAG, "expected exposure to be " + mPreview.getMinimumExposure());
	    assertTrue( mPreview.getCurrentExposure() == mPreview.getMinimumExposure() );
	    assertTrue( mPreview.getCurrentExposure() - mPreview.getMinimumExposure() == seekBar.getProgress() );

	    // test the exposure button clears and reopens without changing exposure level
	    clickView(exposureButton);
	    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureContainer.getVisibility() == View.GONE);
	    assertTrue(seekBarZoom.getVisibility() == View.GONE);
	    clickView(exposureButton);
	    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureContainer.getVisibility() == View.VISIBLE);
	    assertTrue(seekBarZoom.getVisibility() == View.VISIBLE);
	    assertTrue( mPreview.getCurrentExposure() == mPreview.getMinimumExposure() );
	    assertTrue( mPreview.getCurrentExposure() - mPreview.getMinimumExposure() == seekBar.getProgress() );

	    // test touch to focus clears the exposure controls
		int [] gui_location = new int[2];
		mPreview.getView().getLocationOnScreen(gui_location);
		final int step_dist_c = 2;
		final int step_count_c = 10;
	    TouchUtils.drag(MainActivityTest.this, gui_location[0]+step_dist_c, gui_location[0], gui_location[1]+step_dist_c, gui_location[1], step_count_c);
	    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureContainer.getVisibility() == View.GONE);
	    assertTrue(seekBarZoom.getVisibility() == View.GONE);
	    clickView(exposureButton);
	    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureContainer.getVisibility() == View.VISIBLE);
	    assertTrue(seekBarZoom.getVisibility() == View.VISIBLE);
	    assertTrue( mPreview.getCurrentExposure() == mPreview.getMinimumExposure() );
	    assertTrue( mPreview.getCurrentExposure() - mPreview.getMinimumExposure() == seekBar.getProgress() );

		Log.d(TAG, "set exposure to -1");
	    seekBar.setProgress(-1 - mPreview.getMinimumExposure());
		this.getInstrumentation().waitForIdleSync();
	    assertTrue( mPreview.getCurrentExposure() == -1 );
	    assertTrue( mPreview.getCurrentExposure() - mPreview.getMinimumExposure() == seekBar.getProgress() );

	    // clear again so as to not interfere with take photo routine
	    TouchUtils.drag(MainActivityTest.this, gui_location[0]+step_dist_c, gui_location[0], gui_location[1]+step_dist_c, gui_location[1], step_count_c);
	    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureContainer.getVisibility() == View.GONE);
	    assertTrue(seekBarZoom.getVisibility() == View.GONE);

	    subTestTakePhoto(false, false, true, true, false, false, false, false);

	    if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
			Log.d(TAG, "switch camera");
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    clickView(switchCameraButton);

		    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
		    assertTrue(exposureContainer.getVisibility() == View.GONE);
		    assertTrue(seekBarZoom.getVisibility() == View.GONE);
		    assertTrue( mPreview.getCurrentExposure() == -1 );
		    assertTrue( mPreview.getCurrentExposure() - mPreview.getMinimumExposure() == seekBar.getProgress() );

		    clickView(exposureButton);
		    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
		    assertTrue(exposureContainer.getVisibility() == View.VISIBLE);
		    assertTrue(seekBarZoom.getVisibility() == View.VISIBLE);
		    assertTrue( mPreview.getCurrentExposure() == -1 );
		    assertTrue( mPreview.getCurrentExposure() - mPreview.getMinimumExposure() == seekBar.getProgress() );
		}
	}

	public void testExposureLockNotSaved() {
		Log.d(TAG, "testExposureLockNotSaved");

	    if( !mPreview.supportsExposureLock() ) {
	    	return;
	    }

		setToDefault();

	    View exposureLockButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure_lock);
	    clickView(exposureLockButton);
	    assertTrue(mPreview.getCameraController().getAutoExposureLock());

	    this.pauseAndResume();
	    assertTrue(!mPreview.getCameraController().getAutoExposureLock());

	    // now with restart

	    clickView(exposureLockButton);
	    assertTrue(mPreview.getCameraController().getAutoExposureLock());

	    restart();
	    assertTrue(!mPreview.getCameraController().getAutoExposureLock());
	}

	public void testTakePhotoManualISOExposure() throws InterruptedException {
		Log.d(TAG, "testTakePhotoManualISOExposure");
		if( !mPreview.usingCamera2API() ) {
			return;
		}
		else if( !mPreview.supportsISORange() ) {
			return;
		}
		setToDefault();

		switchToISO(100);

		View exposureButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure);
		View exposureContainer = mActivity.findViewById(net.sourceforge.opencamera.R.id.manual_exposure_container);
		SeekBar isoSeekBar = (SeekBar) mActivity.findViewById(net.sourceforge.opencamera.R.id.iso_seekbar);
	    SeekBar exposureTimeSeekBar = (SeekBar) mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure_time_seekbar);
	    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureContainer.getVisibility() == View.GONE);

	    clickView(exposureButton);
	    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
		assertTrue(exposureContainer.getVisibility() == View.VISIBLE);
		assertTrue(isoSeekBar.getVisibility() == View.VISIBLE);
	    assertTrue(exposureTimeSeekBar.getVisibility() == (mPreview.supportsExposureTime() ? View.VISIBLE : View.GONE));

	    assertTrue( isoSeekBar.getMax() == 100 );
	    if( mPreview.supportsExposureTime() )
		    assertTrue( exposureTimeSeekBar.getMax() == 100 );

		Log.d(TAG, "change ISO to min");
	    isoSeekBar.setProgress(0);
		this.getInstrumentation().waitForIdleSync();
		assertTrue( mPreview.getCameraController().getISO() == mPreview.getMinimumISO() );

	    if( mPreview.supportsExposureTime() ) {
			Log.d(TAG, "change exposure time to min");
		    exposureTimeSeekBar.setProgress(0);
			this.getInstrumentation().waitForIdleSync();
			assertTrue( mPreview.getCameraController().getISO() == mPreview.getMinimumISO() );
			assertTrue( mPreview.getCameraController().getExposureTime() == mPreview.getMinimumExposureTime() );
	    }

		Log.d(TAG, "change ISO to max");
	    isoSeekBar.setProgress(100);
		this.getInstrumentation().waitForIdleSync();
		assertTrue( mPreview.getCameraController().getISO() == mPreview.getMaximumISO() );

		// n.b., currently don't test this on devices with long shutter times (e.g., OnePlus 3T) until this is properly supported
	    if( mPreview.supportsExposureTime() && mPreview.getMaximumExposureTime() < 1000000000 ) {
			Log.d(TAG, "change exposure time to max");
		    exposureTimeSeekBar.setProgress(100);
			this.getInstrumentation().waitForIdleSync();
			assertTrue( mPreview.getCameraController().getISO() == mPreview.getMaximumISO() );
			assertTrue( mPreview.getCameraController().getExposureTime() == mPreview.getMaximumExposureTime() );
	    }
		long saved_exposure_time = mPreview.getCameraController().getExposureTime();

	    // test the exposure button clears and reopens without changing exposure level
	    clickView(exposureButton);
	    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureContainer.getVisibility() == View.GONE);
	    clickView(exposureButton);
	    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureContainer.getVisibility() == View.VISIBLE);
		assertTrue(isoSeekBar.getVisibility() == View.VISIBLE);
	    assertTrue(exposureTimeSeekBar.getVisibility() == (mPreview.supportsExposureTime() ? View.VISIBLE : View.GONE));
		assertTrue( mPreview.getCameraController().getISO() == mPreview.getMaximumISO() );
	    if( mPreview.supportsExposureTime() )
			assertTrue( mPreview.getCameraController().getExposureTime() == saved_exposure_time );

	    // test touch to focus clears the exposure controls
		int [] gui_location = new int[2];
		mPreview.getView().getLocationOnScreen(gui_location);
		final int step_dist_c = 2;
		final int step_count_c = 10;
	    TouchUtils.drag(MainActivityTest.this, gui_location[0]+step_dist_c, gui_location[0], gui_location[1]+step_dist_c, gui_location[1], step_count_c);
	    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureContainer.getVisibility() == View.GONE);
	    clickView(exposureButton);
	    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureContainer.getVisibility() == View.VISIBLE);
		assertTrue(isoSeekBar.getVisibility() == View.VISIBLE);
	    assertTrue(exposureTimeSeekBar.getVisibility() == (mPreview.supportsExposureTime() ? View.VISIBLE : View.GONE));
		assertTrue( mPreview.getCameraController().getISO() == mPreview.getMaximumISO() );
	    if( mPreview.supportsExposureTime() )
			assertTrue( mPreview.getCameraController().getExposureTime() == saved_exposure_time );

	    // clear again so as to not interfere with take photo routine
	    TouchUtils.drag(MainActivityTest.this, gui_location[0]+step_dist_c, gui_location[0], gui_location[1]+step_dist_c, gui_location[1], step_count_c);
	    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureContainer.getVisibility() == View.GONE);

	    subTestTakePhoto(false, false, true, true, false, false, false, false);

		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
			Log.d(TAG, "switch camera");
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    clickView(switchCameraButton);

		    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
		    assertTrue(exposureContainer.getVisibility() == View.GONE);
			assertTrue( mPreview.getCameraController().getISO() == mPreview.getMaximumISO() );
		    if( mPreview.supportsExposureTime() ) {
				Log.d(TAG, "exposure time: " + mPreview.getCameraController().getExposureTime());
				Log.d(TAG, "min exposure time: " + mPreview.getMinimumExposureTime());
				Log.d(TAG, "max exposure time: " + mPreview.getMaximumExposureTime());
				if( saved_exposure_time < mPreview.getMinimumExposureTime() )
					saved_exposure_time = mPreview.getMinimumExposureTime();
				if( saved_exposure_time > mPreview.getMaximumExposureTime() )
					saved_exposure_time = mPreview.getMaximumExposureTime();
				assertTrue( mPreview.getCameraController().getExposureTime() == saved_exposure_time );
			}

		    clickView(exposureButton);
		    assertTrue(exposureButton.getVisibility() == View.VISIBLE);
		    assertTrue(exposureContainer.getVisibility() == View.VISIBLE);
			assertTrue(isoSeekBar.getVisibility() == View.VISIBLE);
		    assertTrue(exposureTimeSeekBar.getVisibility() == (mPreview.supportsExposureTime() ? View.VISIBLE : View.GONE));
			assertTrue( mPreview.getCameraController().getISO() == mPreview.getMaximumISO() );
		    if( mPreview.supportsExposureTime() )
				assertTrue( mPreview.getCameraController().getExposureTime() == saved_exposure_time );
		}
	}

	/** Tests that the audio control icon is visible or not as expect (guards against bug fixed in 1.30)
	 */
	public void testAudioControlIcon() {
		Log.d(TAG, "testAudioControlIcon");

		setToDefault();

	    View audioControlButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.audio_control);
	    assertTrue( audioControlButton.getVisibility() == View.GONE );

	    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getAudioControlPreferenceKey(), "noise");
		editor.apply();
		updateForSettings();
	    assertTrue( audioControlButton.getVisibility() == View.VISIBLE );

	    restart();
	    // reset due to restarting!
	    settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		editor = settings.edit();
	    audioControlButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.audio_control);

		assertTrue( audioControlButton.getVisibility() == View.VISIBLE );

		editor.putString(PreferenceKeys.getAudioControlPreferenceKey(), "none");
		editor.apply();
		updateForSettings();
		Log.d(TAG, "visibility is now: " + audioControlButton.getVisibility());
	    assertTrue( audioControlButton.getVisibility() == View.GONE );

		editor.putString(PreferenceKeys.getAudioControlPreferenceKey(), "voice");
		editor.apply();
		updateForSettings();
	    assertTrue( audioControlButton.getVisibility() == View.VISIBLE );

		editor.putString(PreferenceKeys.getAudioControlPreferenceKey(), "none");
		editor.apply();
		updateForSettings();
		Log.d(TAG, "visibility is now: " + audioControlButton.getVisibility());
	    assertTrue( audioControlButton.getVisibility() == View.GONE );
	}

	/*
	 * Note that we pass test_wait_capture_result as a parameter rather than reading from the activity, as for some reason this sometimes resets to false?! Declaring it volatile doesn't fix the problem.
	 */
	private void subTestTakePhoto(boolean locked_focus, boolean immersive_mode, boolean touch_to_focus, boolean wait_after_focus, boolean single_tap_photo, boolean double_tap_photo, boolean is_raw, boolean test_wait_capture_result) throws InterruptedException {
		assertTrue(mPreview.isPreviewStarted());

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
		boolean has_thumbnail_anim = sharedPreferences.getBoolean(PreferenceKeys.getThumbnailAnimationPreferenceKey(), true);
		boolean has_audio_control_button = !sharedPreferences.getString(PreferenceKeys.getAudioControlPreferenceKey(), "none").equals("none");
		boolean is_dro = mActivity.supportsDRO() && sharedPreferences.getString(PreferenceKeys.getPhotoModePreferenceKey(), "preference_photo_mode_std").equals("preference_photo_mode_dro");
		boolean is_hdr = mActivity.supportsHDR() && sharedPreferences.getString(PreferenceKeys.getPhotoModePreferenceKey(), "preference_photo_mode_std").equals("preference_photo_mode_hdr");
		boolean hdr_save_expo =  sharedPreferences.getBoolean(PreferenceKeys.getHDRSaveExpoPreferenceKey(), false);
		boolean is_expo = mActivity.supportsHDR() && sharedPreferences.getString(PreferenceKeys.getPhotoModePreferenceKey(), "preference_photo_mode_std").equals("preference_photo_mode_expo_bracketing");
		String n_expo_images_s = sharedPreferences.getString(PreferenceKeys.getExpoBracketingNImagesPreferenceKey(), "3");
		int n_expo_images = Integer.parseInt(n_expo_images_s);
		
		int saved_count_cameraTakePicture = mPreview.count_cameraTakePicture;
		
		// count initial files in folder
		File folder = mActivity.getImageFolder();
		Log.d(TAG, "folder: " + folder);
		File [] files = folder.listFiles();
		int n_files = files.length;
		Log.d(TAG, "n_files at start: " + n_files);
		
	    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
	    //View flashButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.flash);
	    //View focusButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.focus_mode);
	    View exposureButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure);
	    View exposureLockButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure_lock);
	    View audioControlButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.audio_control);
	    View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);
	    View trashButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.trash);
	    View shareButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.share);
	    assertTrue(switchCameraButton.getVisibility() == (immersive_mode ? View.GONE : View.VISIBLE));
	    assertTrue(switchVideoButton.getVisibility() == (immersive_mode ? View.GONE : View.VISIBLE));
	    int exposureVisibility = exposureButton.getVisibility();
	    int exposureLockVisibility = exposureLockButton.getVisibility();
	    assertTrue(audioControlButton.getVisibility() == ((has_audio_control_button && !immersive_mode) ? View.VISIBLE : View.GONE));
	    assertTrue(popupButton.getVisibility() == (immersive_mode ? View.GONE : View.VISIBLE));
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);

		String focus_value = mPreview.getCameraController().getFocusValue();
		String focus_value_ui = mPreview.getCurrentFocusValue();
		boolean can_auto_focus = false;
        boolean manual_can_auto_focus = false;
		boolean can_focus_area = false;
        if( focus_value.equals("focus_mode_auto") || focus_value.equals("focus_mode_macro") ) {
        	can_auto_focus = true;
        }
        if( focus_value.equals("focus_mode_auto") || focus_value.equals("focus_mode_macro") || focus_value.equals("focus_mode_continuous_picture") ) {
        	manual_can_auto_focus = true;
        }
        if( mPreview.getMaxNumFocusAreas() != 0 && ( focus_value.equals("focus_mode_auto") || focus_value.equals("focus_mode_macro") || focus_value.equals("focus_mode_continuous_picture") || focus_value.equals("focus_mode_continuous_video") || focus_value.equals("focus_mode_manual2") ) ) {
        	can_focus_area = true;
        }
		Log.d(TAG, "focus_value? " + focus_value);
		Log.d(TAG, "can_auto_focus? " + can_auto_focus);
		Log.d(TAG, "manual_can_auto_focus? " + manual_can_auto_focus);
		Log.d(TAG, "can_focus_area? " + can_focus_area);
	    int saved_count = mPreview.count_cameraAutoFocus;
	    String new_focus_value_ui = mPreview.getCurrentFocusValue();
		assertTrue(new_focus_value_ui == focus_value_ui || new_focus_value_ui.equals(focus_value_ui)); // also need to do == check, as strings may be null if focus not supported
		assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));
	    if( touch_to_focus ) {
			// touch to auto-focus with focus area (will also exit immersive mode)
			// autofocus shouldn't be immediately, but after a delay
			Thread.sleep(1000);
		    saved_count = mPreview.count_cameraAutoFocus;
			Log.d(TAG, "saved count_cameraAutoFocus: " + saved_count);
			Log.d(TAG, "about to click preview for autofocus");
			if( double_tap_photo ) {
				TouchUtils.tapView(MainActivityTest.this, mPreview.getView());
			}
			else {
				TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
			}
			this.getInstrumentation().waitForIdleSync();
			Log.d(TAG, "1 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
			assertTrue(mPreview.count_cameraAutoFocus == (manual_can_auto_focus ? saved_count+1 : saved_count));
			assertTrue(mPreview.hasFocusArea() == can_focus_area);
			if( can_focus_area ) {
			    assertTrue(mPreview.getCameraController().getFocusAreas() != null);
			    assertTrue(mPreview.getCameraController().getFocusAreas().size() == 1);
			    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
			    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);
			}
			else {
			    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
			    // we still set metering areas
			    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
			    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);
			}
		    new_focus_value_ui = mPreview.getCurrentFocusValue();
			assertTrue(new_focus_value_ui == focus_value_ui || new_focus_value_ui.equals(focus_value_ui)); // also need to do == check, as strings may be null if focus not supported
			if( focus_value.equals("focus_mode_continuous_picture") )
				assertTrue(mPreview.getCameraController().getFocusValue().equals("focus_mode_auto")); // continuous focus mode switches to auto focus on touch
			else
				assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));
			if( double_tap_photo ) {
				Thread.sleep(100);
				Log.d(TAG, "about to click preview again for double tap");
				//TouchUtils.tapView(MainActivityTest.this, mPreview.getView());
				mPreview.onDoubleTap(); // calling tapView twice doesn't seem to work consistently, so we call this directly!
				this.getInstrumentation().waitForIdleSync();
			}
			if( wait_after_focus && !single_tap_photo && !double_tap_photo) {
				// don't wait after single or double tap photo taking, as the photo taking operation is already started
				Log.d(TAG, "wait after focus...");
				Thread.sleep(3000);
			}
	    }
		Log.d(TAG, "saved count_cameraAutoFocus: " + saved_count);

		if( !single_tap_photo && !double_tap_photo ) {
			View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
			assertFalse( mActivity.hasThumbnailAnimation() );
			Log.d(TAG, "about to click take photo");
		    clickView(takePhotoButton);
			Log.d(TAG, "done clicking take photo");
		}

		Log.d(TAG, "wait until finished taking photo");
		long time_s = System.currentTimeMillis();
	    while( mPreview.isTakingPhoto() ) {
			assertTrue( System.currentTimeMillis() - time_s < 20000 ); // make sure the test fails rather than hanging, if for some reason we get stuck (note that testTakePhotoManualISOExposure takes over 10s on Nexus 6)
		    assertTrue(!mPreview.isTakingPhoto() || switchCameraButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || switchVideoButton.getVisibility() == View.GONE);
		    //assertTrue(!mPreview.isTakingPhoto() || flashButton.getVisibility() == View.GONE);
		    //assertTrue(!mPreview.isTakingPhoto() || focusButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || exposureButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || exposureLockButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || audioControlButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || popupButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || trashButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || shareButton.getVisibility() == View.GONE);
	    }
		Log.d(TAG, "done taking photo");

		Date date = new Date();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(date);
        String suffix = "";
		if( is_dro ) {
			suffix = "_DRO";
		}
		else if( is_hdr ) {
			suffix = "_HDR";
		}
		else if( is_expo ) {
			suffix = "_EXP" + (n_expo_images-1);
		}
		String expected_filename = "IMG_" + timeStamp + suffix + ".jpg";
		// allow for possibility that the time has passed on by 1s since taking the photo
		Date date1 = new Date(date.getTime() - 1000);
        String timeStamp1 = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(date1);
		String expected_filename1= "IMG_" + timeStamp1 + suffix + ".jpg";
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");
		Log.d(TAG, "take picture count: " + mPreview.count_cameraTakePicture);
		assertTrue(mPreview.count_cameraTakePicture==saved_count_cameraTakePicture+1);
		if( test_wait_capture_result ) {
			// if test_wait_capture_result, then we'll have waited too long for thumbnail animation
		}
		else if( has_thumbnail_anim ) {
			while( !mActivity.hasThumbnailAnimation() ) {
				Log.d(TAG, "waiting for thumbnail animation");
				Thread.sleep(10);
			}
		}
		else {
			assertFalse( mActivity.hasThumbnailAnimation() );
		}
		mActivity.waitUntilImageQueueEmpty();
		Log.d(TAG, "mActivity.hasThumbnailAnimation()?: " + mActivity.hasThumbnailAnimation());

		// focus should be back to normal now:
	    new_focus_value_ui = mPreview.getCurrentFocusValue();
		assertTrue(new_focus_value_ui == focus_value_ui || new_focus_value_ui.equals(focus_value_ui)); // also need to do == check, as strings may be null if focus not supported
		Log.d(TAG, "focus_value: " + focus_value);
		Log.d(TAG, "new focus_value: " + mPreview.getCameraController().getFocusValue());
		assertTrue(mPreview.getCameraController().getFocusValue().equals(focus_value));

		assertTrue( folder.exists() );
		File [] files2 = folder.listFiles();
		int n_new_files = files2.length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		int exp_n_new_files;
		if( is_raw )
			exp_n_new_files = 2;
		else if( is_hdr && hdr_save_expo )
			exp_n_new_files = 4;
		else if( is_expo )
			exp_n_new_files = n_expo_images;
		else
			exp_n_new_files = 1;
		Log.d(TAG, "exp_n_new_files: " + exp_n_new_files);
		assertTrue(n_new_files == exp_n_new_files);
		// check files have names as expected
		String filename_jpeg = null;
		String filename_dng = null;
		for(File file : files2) {
			Log.d(TAG, "file: " + file);
			boolean is_new = true;
			for(int j=0;j<n_files && is_new;j++) {
				if( file.equals( files[j] ) ) {
					is_new = false;
				}
			}
			if( is_new ) {
				Log.d(TAG, "file is new");
				String filename = file.getName();
				assertTrue(filename.startsWith("IMG_"));
				if( filename.endsWith(".jpg") ) {
					assertTrue(hdr_save_expo || is_expo || filename_jpeg == null);
					if( is_hdr && hdr_save_expo ) {
						// only look for the "_HDR" image
						if( filename.contains("_HDR") )
							filename_jpeg = filename;
					}
					else if( is_expo ) {
						if( filename_jpeg != null ) {
							// check same root
							String filename_base_jpeg = filename_jpeg.substring(0, filename_jpeg.length()-5);
							String filename_base = filename.substring(0, filename.length()-5);
							assertTrue( filename_base_jpeg.equals(filename_base) );
						}
						filename_jpeg = filename; // store the last name, to match mActivity.test_last_saved_image
					}
					else {
						filename_jpeg = filename;
					}
				}
				else if( filename.endsWith(".dng") ) {
					assertTrue(is_raw);
					assertTrue(filename_dng == null);
					filename_dng = filename;
				}
				else {
					assertTrue(false);
				}
			}
		}
		assertTrue( filename_jpeg != null );
		assertTrue( (filename_dng != null) == is_raw );
		if( is_raw ) {
			// check we have same filenames (ignoring extensions)
			String filename_base_jpeg = filename_jpeg.substring(0, filename_jpeg.length()-4);
			Log.d(TAG, "filename_base_jpeg: " + filename_base_jpeg);
			String filename_base_dng = filename_dng.substring(0, filename_dng.length()-4);
			Log.d(TAG, "filename_base_dng: " + filename_base_dng);
			assertTrue( filename_base_jpeg.equals(filename_base_dng) );
		}
		Thread.sleep(1500); // wait until we've scanned
		if( test_wait_capture_result ) {
			// if test_wait_capture_result, then it may take longer before we've scanned
		}
		else {
			Log.d(TAG, "failed to scan: " + mActivity.getStorageUtils().failed_to_scan);
			assertFalse(mActivity.getStorageUtils().failed_to_scan);
		}
		
		assertTrue(mActivity.test_last_saved_image != null);
		File saved_image_file = new File(mActivity.test_last_saved_image);
		Log.d(TAG, "saved name: " + saved_image_file.getName());
		Log.d(TAG, "expected name: " + expected_filename);
		Log.d(TAG, "expected name1: " + expected_filename1);
		assertTrue(expected_filename.equals(saved_image_file.getName()) || expected_filename1.equals(saved_image_file.getName()));
		
		// in locked focus mode, taking photo should never redo an auto-focus
		// if photo mode, we may do a refocus if the previous auto-focus failed, but not if it succeeded
		Log.d(TAG, "2 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		if( locked_focus ) {
			assertTrue(mPreview.count_cameraAutoFocus == (can_auto_focus ? saved_count+1 : saved_count));
		}
		if( test_wait_capture_result ) {
			// if test_wait_capture_result, then we'll have waited too long, so focus settings may have changed
		}
		else if( touch_to_focus ) {
			Log.d(TAG, "can_focus_area?: " + can_focus_area);
			Log.d(TAG, "hasFocusArea?: " + mPreview.hasFocusArea());
			assertTrue(mPreview.hasFocusArea() == can_focus_area);
			if( can_focus_area ) {
			    assertTrue(mPreview.getCameraController().getFocusAreas() != null);
			    assertTrue(mPreview.getCameraController().getFocusAreas().size() == 1);
			    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
			    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);
			}
			else {
			    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
			    // we still set metering areas
			    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
			    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);
			}
		}
		else {
			assertFalse(mPreview.hasFocusArea());
		    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
		    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);
		}

		// trash/share only shown when preview is paused after taking a photo

		assertTrue(mPreview.isPreviewStarted()); // check preview restarted
	    assertTrue(switchCameraButton.getVisibility() == View.VISIBLE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    if( !immersive_mode ) {
	    	assertTrue(exposureButton.getVisibility() == exposureVisibility);
	    	assertTrue(exposureLockButton.getVisibility() == exposureLockVisibility);
	    }
	    assertTrue(audioControlButton.getVisibility() == (has_audio_control_button ? View.VISIBLE : View.GONE));
	    assertTrue(popupButton.getVisibility() == View.VISIBLE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);
	}

	public void testTakePhoto() throws InterruptedException {
		Log.d(TAG, "testTakePhoto");
		setToDefault();
		subTestTakePhoto(false, false, true, true, false, false, false, false);
	}
	
	/** Test taking photo with JPEG + DNG (RAW).
	 */
	public void testTakePhotoRaw() throws InterruptedException {
		Log.d(TAG, "testTakePhotoRaw");
		if( !mPreview.usingCamera2API() ) {
			return;
		}
		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getRawPreferenceKey(), "preference_raw_yes");
		editor.apply();
		updateForSettings();
		
		subTestTakePhoto(false, false, true, true, false, false, true, false);
	}
	
	/** Test taking photo with JPEG + DNG (RAW), with test_wait_capture_result.
	 */
	public void testTakePhotoRawWaitCaptureResult() throws InterruptedException {
		Log.d(TAG, "testTakePhotoRawWaitCaptureResult");
		if( !mPreview.usingCamera2API() ) {
			return;
		}
		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getRawPreferenceKey(), "preference_raw_yes");
		editor.apply();
		updateForSettings();
		
		mPreview.getCameraController().test_wait_capture_result = true;
		subTestTakePhoto(false, false, true, true, false, false, true, true);
	}
	
	/** Test taking multiple RAW photos.
	 */
	public void testTakePhotoRawMulti() {
		Log.d(TAG, "testTakePhotoRawMulti");
		if( !mPreview.usingCamera2API() ) {
			return;
		}
		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getRawPreferenceKey(), "preference_raw_yes");
		editor.apply();
		updateForSettings();

		// count initial files in folder
		File folder = mActivity.getImageFolder();
		int n_files = folder.listFiles().length;
		Log.d(TAG, "n_files at start: " + n_files);

		int start_count = mPreview.count_cameraTakePicture;
		final int n_photos = 5;
		for(int i=0;i<n_photos;i++) {
		    View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
			Log.d(TAG, "about to click take photo count: " + i);
		    clickView(takePhotoButton);
			Log.d(TAG, "wait until finished taking photo count: " + i);
		    while( mPreview.isTakingPhoto() ) {
		    }
			Log.d(TAG, "done taking photo count: " + i);
			this.getInstrumentation().waitForIdleSync();

			/*int n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == mPreview.count_cameraTakePicture - start_count);*/
			assertTrue(i+1 == mPreview.count_cameraTakePicture - start_count);
		}

		mActivity.waitUntilImageQueueEmpty();
		int n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		assertTrue(n_new_files == 2*n_photos); // if we fail here, be careful we haven't lost images (i.e., waitUntilImageQueueEmpty() returns before all images are saved)
	}

	public void testTakePhotoAutoStabilise() throws InterruptedException {
		Log.d(TAG, "testTakePhotoAutoStabilise");
		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getAutoStabilisePreferenceKey(), true);
		editor.apply();

		subTestTakePhoto(false, false, true, true, false, false, false, false);
	}
	
	/** Test taking photo with continuous photo mode.
	 *  Touching to focus will mean the photo is taken whilst the camera controller is actually
	 *  in autofocus mode.
	 */
	public void testTakePhotoContinuous() throws InterruptedException {
		Log.d(TAG, "testTakePhotoContinuous");
		setToDefault();
		switchToFocusValue("focus_mode_continuous_picture");
		subTestTakePhoto(false, false, true, true, false, false, false, false);
	}
	
	/** Test taking photo with continuous photo mode. Don't touch to focus first, so we take the
	 *  photo in continuous focus mode.
	 */
	public void testTakePhotoContinuousNoTouch() throws InterruptedException {
		Log.d(TAG, "testTakePhotoContinuousNoTouch");
		setToDefault();
		switchToFocusValue("focus_mode_continuous_picture");
		subTestTakePhoto(false, false, false, false, false, false, false, false);
	}
	
	public void testTakePhotoFlashAuto() throws InterruptedException {
		Log.d(TAG, "testTakePhotoFlashAuto");
		if( !mPreview.supportsFlash() ) {
			return;
		}

		setToDefault();
		switchToFlashValue("flash_auto");
		Thread.sleep(2000); // wait so we don't take the photo immediately, to be more realistic
		subTestTakePhoto(false, false, false, false, false, false, false, false);
	}

	public void testTakePhotoFlashOn() throws InterruptedException {
		Log.d(TAG, "testTakePhotoFlashOn");
		if( !mPreview.supportsFlash() ) {
			return;
		}

		setToDefault();
		switchToFlashValue("flash_on");
		Thread.sleep(2000); // wait so we don't take the photo immediately, to be more realistic
		subTestTakePhoto(false, false, false, false, false, false, false, false);
	}

	public void testTakePhotoFlashTorch() throws InterruptedException {
		Log.d(TAG, "testTakePhotoFlashTorch");
		if( !mPreview.supportsFlash() ) {
			return;
		}

		setToDefault();
		switchToFlashValue("flash_torch");
		Thread.sleep(2000); // wait so we don't take the photo immediately, to be more realistic
		subTestTakePhoto(false, false, false, false, false, false, false, false);
	}

	/** Tests the "fake" flash mode. Important to do this even for devices where standard Camera2 flash work fine, as we use
	 *  fake flash for modes like HDR (plus it's good to still test the fake flash mode on as many devices as possible).
	 *  We do more tests with flash on than flash auto (especially due to bug on OnePlus 3T where fake flash auto never fires the flash
	 *  anyway).
     */
	public void testTakePhotoFlashAutoFakeMode() throws InterruptedException {
		Log.d(TAG, "testTakePhotoFlashAutoFakeMode");
		if( !mPreview.supportsFlash() ) {
			return;
		}
		if( !mPreview.usingCamera2API() ) {
			return;
		}

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getCamera2FakeFlashPreferenceKey(), true);
		editor.apply();
		updateForSettings();

		switchToFlashValue("flash_auto");
		switchToFocusValue("focus_mode_auto");
		Thread.sleep(2000); // wait so we don't take the photo immediately, to be more realistic
		assertTrue( mPreview.getCameraController().getUseCamera2FakeFlash() ); // make sure we turned on the option in the camera controller
		subTestTakePhoto(false, false, false, false, false, false, false, false);

		// now test continuous focus mode
		Thread.sleep(1000);
		switchToFocusValue("focus_mode_continuous_picture");
		assertTrue( mPreview.getCameraController().getUseCamera2FakeFlash() ); // make sure we turned on the option in the camera controller
		subTestTakePhoto(false, false, false, false, false, false, false, false);
	}

	/** Tests the "fake" flash mode. Important to do this even for devices where standard Camera2 flash work fine, as we use
	 *  fake flash for modes like HDR (plus it's good to still test the fake flash mode on as many devices as possible).
	 *  We do more tests with flash on than flash auto (especially due to bug on OnePlus 3T where fake flash auto never fires the flash
	 *  anyway).
	 *  May have precapture timeout if phone is face down (at least on Nexus 6 and OnePlus 3T) - issue that we've already ae converged,
	 *  so we think fake-precapture never starts when firing the flash for taking photo.
	 */
	public void testTakePhotoFlashOnFakeMode() throws InterruptedException {
		Log.d(TAG, "testTakePhotoFlashOnFakeMode");
		if( !mPreview.supportsFlash() ) {
			return;
		}
		if( !mPreview.usingCamera2API() ) {
			return;
		}

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getCamera2FakeFlashPreferenceKey(), true);
		editor.apply();
		updateForSettings();

		switchToFocusValue("focus_mode_auto");
		switchToFlashValue("flash_on");
		Thread.sleep(2000); // wait so we don't take the photo immediately, to be more realistic
		assertTrue( mPreview.getCameraController().getUseCamera2FakeFlash() ); // make sure we turned on the option in the camera controller
		subTestTakePhoto(false, false, false, false, false, false, false, false);
		assertTrue( mPreview.getCameraController() == null || mPreview.getCameraController().count_precapture_timeout == 0 );

		// now test doing autofocus, waiting, then taking photo
		Thread.sleep(1000);
		assertTrue( mPreview.getCameraController().getUseCamera2FakeFlash() ); // make sure we turned on the option in the camera controller
		subTestTakePhoto(false, false, true, true, false, false, false, false);
		assertTrue( mPreview.getCameraController() == null || mPreview.getCameraController().count_precapture_timeout == 0 );

		// now test doing autofocus, then taking photo immediately
		Thread.sleep(1000);
		assertTrue( mPreview.getCameraController().getUseCamera2FakeFlash() ); // make sure we turned on the option in the camera controller
		subTestTakePhoto(false, false, true, false, false, false, false, false);
		assertTrue( mPreview.getCameraController() == null || mPreview.getCameraController().count_precapture_timeout == 0 );

		// now test it all again with continuous focus mode
		switchToFocusValue("focus_mode_continuous_picture");
		Thread.sleep(1000);
		assertTrue( mPreview.getCameraController().getUseCamera2FakeFlash() ); // make sure we turned on the option in the camera controller
		subTestTakePhoto(false, false, false, false, false, false, false, false);
		assertTrue( mPreview.getCameraController() == null || mPreview.getCameraController().count_precapture_timeout == 0 );

		// now test doing autofocus, waiting, then taking photo
		Thread.sleep(1000);
		assertTrue( mPreview.getCameraController().getUseCamera2FakeFlash() ); // make sure we turned on the option in the camera controller
		subTestTakePhoto(false, false, true, true, false, false, false, false);
		assertTrue( mPreview.getCameraController() == null || mPreview.getCameraController().count_precapture_timeout == 0 );

		// now test doing autofocus, then taking photo immediately
		Thread.sleep(1000);
		assertTrue( mPreview.getCameraController().getUseCamera2FakeFlash() ); // make sure we turned on the option in the camera controller
		subTestTakePhoto(false, false, true, false, false, false, false, false);
		assertTrue( mPreview.getCameraController() == null || mPreview.getCameraController().count_precapture_timeout == 0 );

		//mPreview.getCameraController().count_precapture_timeout = 0; // hack - precapture timeouts are more common with fake flash precapture mode, especially when phone is face down during testing
	}

	public void testTakePhotoSingleTap() throws InterruptedException {
		Log.d(TAG, "testTakePhotoSingleTap");
		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getTouchCapturePreferenceKey(), "single");
		editor.apply();
		updateForSettings();

		subTestTakePhoto(false, false, true, true, true, false, false, false);
	}

	public void testTakePhotoDoubleTap() throws InterruptedException {
		Log.d(TAG, "testTakePhotoDoubleTap");
		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getTouchCapturePreferenceKey(), "double");
		editor.apply();
		updateForSettings();

		subTestTakePhoto(false, false, true, true, false, true, false, false);
	}

	public void testTakePhotoNoAutofocus() throws InterruptedException {
		Log.d(TAG, "testTakePhotoNoAutofocus");
		setToDefault();
		subTestTakePhoto(false, false, false, false, false, false, false, false);
	}

	public void testTakePhotoNoThumbnail() throws InterruptedException {
		Log.d(TAG, "testTakePhotoNoThumbnail");
		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getThumbnailAnimationPreferenceKey(), false);
		editor.apply();
		subTestTakePhoto(false, false, true, true, false, false, false, false);
	}

	/* Tests manually focusing, then immediately taking a photo.
	 */
	public void testTakePhotoAfterFocus() throws InterruptedException {
		Log.d(TAG, "testTakePhotoAfterFocus");
		setToDefault();
		subTestTakePhoto(false, false, true, false, false, false, false, false);
	}

	/* Tests bug fixed by take_photo_after_autofocus in Preview, where the app would hang due to taking a photo after touching to focus. */
	public void testTakePhotoFlashBug() throws InterruptedException {
		Log.d(TAG, "testTakePhotoFlashBug");
		if( !mPreview.supportsFlash() ) {
			return;
		}

		setToDefault();
		switchToFlashValue("flash_on");
		subTestTakePhoto(false, false, true, false, false, false, false, false);
	}

	/* Tests taking a photo with front camera.
	 * Also tests the content descriptions for switch camera button.
	 * And tests that we save the current camera when pausing and resuming.
	 */
	public void testTakePhotoFrontCamera() throws InterruptedException {
		Log.d(TAG, "testTakePhotoFrontCamera");
		if( mPreview.getCameraControllerManager().getNumberOfCameras() <= 1 ) {
			return;
		}
		setToDefault();
		int cameraId = mPreview.getCameraId();
		boolean is_front_facing = mPreview.getCameraControllerManager().isFrontFacing(cameraId);

		View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    CharSequence contentDescription = switchCameraButton.getContentDescription();
	    clickView(switchCameraButton);

	    int new_cameraId = mPreview.getCameraId();
		boolean new_is_front_facing = mPreview.getCameraControllerManager().isFrontFacing(new_cameraId);
	    CharSequence new_contentDescription = switchCameraButton.getContentDescription();

		Log.d(TAG, "cameraId: " + cameraId);
		Log.d(TAG, "is_front_facing: " + is_front_facing);
		Log.d(TAG, "contentDescription: " + contentDescription);
		Log.d(TAG, "new_cameraId: " + new_cameraId);
		Log.d(TAG, "new_is_front_facing: " + new_is_front_facing);
		Log.d(TAG, "new_contentDescription: " + new_contentDescription);

		assertTrue(cameraId != new_cameraId);
		assertTrue( contentDescription.equals( mActivity.getResources().getString(new_is_front_facing ? net.sourceforge.opencamera.R.string.switch_to_front_camera : net.sourceforge.opencamera.R.string.switch_to_back_camera) ) );
		assertTrue( new_contentDescription.equals( mActivity.getResources().getString(is_front_facing ? net.sourceforge.opencamera.R.string.switch_to_front_camera : net.sourceforge.opencamera.R.string.switch_to_back_camera) ) );
		subTestTakePhoto(false, false, true, true, false, false, false, false);
		
		// check still front camera after pause/resume
		pauseAndResume();

		int restart_cameraId = mPreview.getCameraId();
	    CharSequence restart_contentDescription = switchCameraButton.getContentDescription();
		Log.d(TAG, "restart_contentDescription: " + restart_contentDescription);
		assertTrue(restart_cameraId == new_cameraId);
		assertTrue( restart_contentDescription.equals( mActivity.getResources().getString(is_front_facing ? net.sourceforge.opencamera.R.string.switch_to_front_camera : net.sourceforge.opencamera.R.string.switch_to_back_camera) ) );

		// now test mirror mode
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getFrontCameraMirrorKey(), "preference_front_camera_mirror_photo");
		editor.apply();
		updateForSettings();
		subTestTakePhoto(false, false, true, true, false, false, false, false);
	}

	/* Tests taking a photo with front camera and screen flash.
	 * And tests that we save the current camera when pausing and resuming.
	 */
	public void testTakePhotoFrontCameraScreenFlash() throws InterruptedException {
		Log.d(TAG, "testTakePhotoFrontCameraScreenFlash");
		if( mPreview.getCameraControllerManager().getNumberOfCameras() <= 1 ) {
			return;
		}
		setToDefault();
		
		int cameraId = mPreview.getCameraId();

		View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    clickView(switchCameraButton);
		this.getInstrumentation().waitForIdleSync();

	    int new_cameraId = mPreview.getCameraId();

		Log.d(TAG, "cameraId: " + cameraId);
		Log.d(TAG, "new_cameraId: " + new_cameraId);

		assertTrue(cameraId != new_cameraId);

	    switchToFlashValue("flash_frontscreen_on");

		subTestTakePhoto(false, false, true, true, false, false, false, false);
	}

	public void testTakePhotoLockedFocus() throws InterruptedException {
		Log.d(TAG, "testTakePhotoLockedFocus");
		setToDefault();
		switchToFocusValue("focus_mode_locked");
		subTestTakePhoto(true, false, true, true, false, false, false, false);
	}

	public void testTakePhotoManualFocus() throws InterruptedException {
		Log.d(TAG, "testTakePhotoManualFocus");
		if( !mPreview.usingCamera2API() ) {
			return;
		}
		setToDefault();
	    SeekBar seekBar = (SeekBar) mActivity.findViewById(net.sourceforge.opencamera.R.id.focus_seekbar);
	    assertTrue(seekBar.getVisibility() == View.INVISIBLE);
		switchToFocusValue("focus_mode_manual2");
	    assertTrue(seekBar.getVisibility() == View.VISIBLE);
		seekBar.setProgress( (int)(0.25*(seekBar.getMax()-1)) );
		subTestTakePhoto(false, false, true, true, false, false, false, false);
	}

	public void testTakePhotoLockedLandscape() throws InterruptedException {
		Log.d(TAG, "testTakePhotoLockedLandscape");
		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getLockOrientationPreferenceKey(), "landscape");
		editor.apply();
		updateForSettings();
		subTestTakePhoto(false, false, true, true, false, false, false, false);
	}

	public void testTakePhotoLockedPortrait() throws InterruptedException {
		Log.d(TAG, "testTakePhotoLockedPortrait");
		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getLockOrientationPreferenceKey(), "portrait");
		editor.apply();
		updateForSettings();
		subTestTakePhoto(false, false, true, true, false, false, false, false);
	}

	// If this test fails, make sure we've manually selected that folder (as permission can't be given through the test framework).
	public void testTakePhotoSAF() throws InterruptedException {
		Log.d(TAG, "testTakePhotoSAF");

		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ) {
			Log.d(TAG, "SAF requires Android Lollipop or better");
			return;
		}

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getUsingSAFPreferenceKey(), true);
		editor.putString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), "content://com.android.externalstorage.documents/tree/primary%3ADCIM%2FOpenCamera");
		editor.apply();
		updateForSettings();

		subTestTakePhoto(false, false, true, true, false, false, false, false);
	}

	public void testTakePhotoAudioButton() throws InterruptedException {
		Log.d(TAG, "testTakePhotoAudioButton");
		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getAudioControlPreferenceKey(), "voice");
		editor.apply();
		updateForSettings();

		subTestTakePhoto(false, false, true, true, false, false, false, false);
	}

	// If this fails with a SecurityException about needing INJECT_EVENTS permission, this seems to be due to the "help popup" that Android shows - can be fixed by clearing that manually, then rerunning the test.
	public void testImmersiveMode() throws InterruptedException {
		Log.d(TAG, "testImmersiveMode");

		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ) {
			Log.d(TAG, "immersive mode requires Android Kitkat or better");
			return;
		}

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getImmersiveModePreferenceKey(), "immersive_mode_gui");
		editor.putString(PreferenceKeys.getAudioControlPreferenceKey(), "voice");
		editor.apply();
		updateForSettings();

		boolean has_audio_control_button = true;

	    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
	    View exposureButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure);
	    View exposureLockButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure_lock);
	    View audioControlButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.audio_control);
	    View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);
	    View trashButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.trash);
	    View shareButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.share);
	    View zoomSeekBar = mActivity.findViewById(net.sourceforge.opencamera.R.id.zoom_seekbar);
		View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		View pauseVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.pause_video);
	    assertTrue(switchCameraButton.getVisibility() == View.VISIBLE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    int exposureVisibility = exposureButton.getVisibility();
	    int exposureLockVisibility = exposureLockButton.getVisibility();
	    assertTrue(audioControlButton.getVisibility() == (has_audio_control_button ? View.VISIBLE : View.GONE));
	    assertTrue(popupButton.getVisibility() == View.VISIBLE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);
	    assertTrue(zoomSeekBar.getVisibility() == View.VISIBLE);
		assertTrue(takePhotoButton.getVisibility() == View.VISIBLE);
		assertTrue(pauseVideoButton.getVisibility() == View.INVISIBLE);

	    // now wait for immersive mode to kick in
	    Thread.sleep(6000);
	    assertTrue(switchCameraButton.getVisibility() == View.GONE);
	    assertTrue(switchVideoButton.getVisibility() == View.GONE);
	    assertTrue(exposureButton.getVisibility() == View.GONE);
	    assertTrue(exposureLockButton.getVisibility() == View.GONE);
	    assertTrue(audioControlButton.getVisibility() == View.GONE);
	    assertTrue(popupButton.getVisibility() == View.GONE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);
	    assertTrue(zoomSeekBar.getVisibility() == View.GONE);
	    assertTrue(takePhotoButton.getVisibility() == View.VISIBLE);
		assertTrue(pauseVideoButton.getVisibility() == View.INVISIBLE);

	    subTestTakePhoto(false, true, true, true, false, false, false, false);
	    
	    // test now exited immersive mode
	    assertTrue(switchCameraButton.getVisibility() == View.VISIBLE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureButton.getVisibility() == exposureVisibility);
	    assertTrue(exposureLockButton.getVisibility() == exposureLockVisibility);
	    assertTrue(audioControlButton.getVisibility() == (has_audio_control_button ? View.VISIBLE : View.GONE));
	    assertTrue(popupButton.getVisibility() == View.VISIBLE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);
	    assertTrue(zoomSeekBar.getVisibility() == View.VISIBLE);
	    assertTrue(takePhotoButton.getVisibility() == View.VISIBLE);
		assertTrue(pauseVideoButton.getVisibility() == View.INVISIBLE);

	    // wait for immersive mode to kick in again
	    Thread.sleep(6000);
	    assertTrue(switchCameraButton.getVisibility() == View.GONE);
	    assertTrue(switchVideoButton.getVisibility() == View.GONE);
	    assertTrue(exposureButton.getVisibility() == View.GONE);
	    assertTrue(exposureLockButton.getVisibility() == View.GONE);
	    assertTrue(audioControlButton.getVisibility() == View.GONE);
	    assertTrue(popupButton.getVisibility() == View.GONE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);
	    assertTrue(zoomSeekBar.getVisibility() == View.GONE);
	    assertTrue(takePhotoButton.getVisibility() == View.VISIBLE);
		assertTrue(pauseVideoButton.getVisibility() == View.INVISIBLE);

	    subTestTakePhotoPreviewPaused(true, false);

	    // test now exited immersive mode
	    assertTrue(switchCameraButton.getVisibility() == View.VISIBLE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureButton.getVisibility() == exposureVisibility);
	    assertTrue(exposureLockButton.getVisibility() == exposureLockVisibility);
	    assertTrue(audioControlButton.getVisibility() == (has_audio_control_button ? View.VISIBLE : View.GONE));
	    assertTrue(popupButton.getVisibility() == View.VISIBLE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);
	    assertTrue(zoomSeekBar.getVisibility() == View.VISIBLE);
	    assertTrue(takePhotoButton.getVisibility() == View.VISIBLE);
		assertTrue(pauseVideoButton.getVisibility() == View.INVISIBLE);

	    // need to switch video before going back to immersive mode
		if( !mPreview.isVideo() ) {
			clickView(switchVideoButton);
		}
	    // test now exited immersive mode
	    assertTrue(switchCameraButton.getVisibility() == View.VISIBLE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureButton.getVisibility() == exposureVisibility);
	    assertTrue(exposureLockButton.getVisibility() == exposureLockVisibility);
	    assertTrue(audioControlButton.getVisibility() == (has_audio_control_button ? View.VISIBLE : View.GONE));
	    assertTrue(popupButton.getVisibility() == View.VISIBLE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);
	    assertTrue(zoomSeekBar.getVisibility() == View.VISIBLE);
	    assertTrue(takePhotoButton.getVisibility() == View.VISIBLE);
		assertTrue(pauseVideoButton.getVisibility() == View.INVISIBLE);

	    // wait for immersive mode to kick in again
	    Thread.sleep(6000);
	    assertTrue(switchCameraButton.getVisibility() == View.GONE);
	    assertTrue(switchVideoButton.getVisibility() == View.GONE);
	    assertTrue(exposureButton.getVisibility() == View.GONE);
	    assertTrue(exposureLockButton.getVisibility() == View.GONE);
	    assertTrue(audioControlButton.getVisibility() == View.GONE);
	    assertTrue(popupButton.getVisibility() == View.GONE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);
	    assertTrue(zoomSeekBar.getVisibility() == View.GONE);
	    assertTrue(takePhotoButton.getVisibility() == View.VISIBLE);
		assertTrue(pauseVideoButton.getVisibility() == View.INVISIBLE);

	    subTestTakeVideo(false, false, false, true, null, 5000, false, false);

	    // test touch exits immersive mode
		TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
	    assertTrue(switchCameraButton.getVisibility() == View.VISIBLE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureButton.getVisibility() == exposureVisibility);
	    assertTrue(exposureLockButton.getVisibility() == exposureLockVisibility);
	    assertTrue(audioControlButton.getVisibility() == (has_audio_control_button ? View.VISIBLE : View.GONE));
	    assertTrue(popupButton.getVisibility() == View.VISIBLE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);
	    assertTrue(zoomSeekBar.getVisibility() == View.VISIBLE);
	    assertTrue(takePhotoButton.getVisibility() == View.VISIBLE);
		assertTrue(pauseVideoButton.getVisibility() == View.INVISIBLE);

	    // switch back to photo mode
		if( mPreview.isVideo() ) {
			clickView(switchVideoButton);
		}

		if( mPreview.usingCamera2API() && mPreview.supportsISORange() ) {
		    // now test exposure button disappears when in manual ISO mode
			switchToISO(100);

			// wait for immersive mode to kick in again
		    Thread.sleep(6000);
		    assertTrue(switchCameraButton.getVisibility() == View.GONE);
		    assertTrue(switchVideoButton.getVisibility() == View.GONE);
		    assertTrue(exposureButton.getVisibility() == View.GONE);
		    assertTrue(exposureLockButton.getVisibility() == View.GONE);
		    assertTrue(audioControlButton.getVisibility() == View.GONE);
		    assertTrue(popupButton.getVisibility() == View.GONE);
		    assertTrue(trashButton.getVisibility() == View.GONE);
		    assertTrue(shareButton.getVisibility() == View.GONE);
		    assertTrue(zoomSeekBar.getVisibility() == View.GONE);
		    assertTrue(takePhotoButton.getVisibility() == View.VISIBLE);
			assertTrue(pauseVideoButton.getVisibility() == View.INVISIBLE);
		}
	}

	// See note under testImmersiveMode() if this fails with a SecurityException about needing INJECT_EVENTS permission.
	public void testImmersiveModeEverything() throws InterruptedException {
		Log.d(TAG, "testImmersiveModeEverything");

		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ) {
			Log.d(TAG, "immersive mode requires Android Kitkat or better");
			return;
		}

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getImmersiveModePreferenceKey(), "immersive_mode_everything");
		editor.apply();
		updateForSettings();

	    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
	    View exposureButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure);
	    View exposureLockButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure_lock);
	    View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);
	    View trashButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.trash);
	    View shareButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.share);
		View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		View pauseVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.pause_video);
	    View zoomSeekBar = mActivity.findViewById(net.sourceforge.opencamera.R.id.zoom_seekbar);
	    assertTrue(switchCameraButton.getVisibility() == View.VISIBLE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    int exposureVisibility = exposureButton.getVisibility();
	    int exposureLockVisibility = exposureLockButton.getVisibility();
	    assertTrue(popupButton.getVisibility() == View.VISIBLE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);
	    assertTrue(zoomSeekBar.getVisibility() == View.VISIBLE);
	    assertTrue(takePhotoButton.getVisibility() == View.VISIBLE);
		assertTrue(pauseVideoButton.getVisibility() == View.INVISIBLE);

	    // now wait for immersive mode to kick in
	    Thread.sleep(6000);
	    assertTrue(switchCameraButton.getVisibility() == View.GONE);
	    assertTrue(switchVideoButton.getVisibility() == View.GONE);
	    assertTrue(exposureButton.getVisibility() == View.GONE);
	    assertTrue(exposureLockButton.getVisibility() == View.GONE);
	    assertTrue(popupButton.getVisibility() == View.GONE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);
	    assertTrue(zoomSeekBar.getVisibility() == View.GONE);
	    assertTrue(takePhotoButton.getVisibility() == View.GONE);
		assertTrue(pauseVideoButton.getVisibility() == View.INVISIBLE);

		// now touch to exit immersive mode
		TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
		Thread.sleep(500);

		// test now exited immersive mode
	    assertTrue(switchCameraButton.getVisibility() == View.VISIBLE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureButton.getVisibility() == exposureVisibility);
	    assertTrue(exposureLockButton.getVisibility() == exposureLockVisibility);
	    assertTrue(popupButton.getVisibility() == View.VISIBLE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);
	    assertTrue(zoomSeekBar.getVisibility() == View.VISIBLE);
	    assertTrue(takePhotoButton.getVisibility() == View.VISIBLE);
		assertTrue(pauseVideoButton.getVisibility() == View.INVISIBLE);

	    // test touch exits immersive mode
		TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
	    assertTrue(switchCameraButton.getVisibility() == View.VISIBLE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    assertTrue(exposureButton.getVisibility() == exposureVisibility);
	    assertTrue(exposureLockButton.getVisibility() == exposureLockVisibility);
	    assertTrue(popupButton.getVisibility() == View.VISIBLE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);
	    assertTrue(zoomSeekBar.getVisibility() == View.VISIBLE);
	    assertTrue(takePhotoButton.getVisibility() == View.VISIBLE);
		assertTrue(pauseVideoButton.getVisibility() == View.INVISIBLE);
	}
	
	private void subTestTakePhotoPreviewPaused(boolean immersive_mode, boolean is_raw) throws InterruptedException {
		mPreview.count_cameraTakePicture = 0;

		// count initial files in folder
		File folder = mActivity.getImageFolder();
		int n_files = folder.listFiles().length;
		Log.d(TAG, "n_files at start: " + n_files);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getPausePreviewPreferenceKey(), true);
		editor.apply();

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
		boolean has_audio_control_button = !sharedPreferences.getString(PreferenceKeys.getAudioControlPreferenceKey(), "none").equals("none");

		Log.d(TAG, "check if preview is started");
		assertTrue(mPreview.isPreviewStarted());
		
	    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
	    //View flashButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.flash);
	    //View focusButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.focus_mode);
	    View exposureButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure);
	    View exposureLockButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure_lock);
	    View audioControlButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.audio_control);
	    View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);
	    View trashButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.trash);
	    View shareButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.share);
	    assertTrue(switchCameraButton.getVisibility() == (immersive_mode ? View.GONE : View.VISIBLE));
	    assertTrue(switchVideoButton.getVisibility() == (immersive_mode ? View.GONE : View.VISIBLE));
	    // store status to compare with later
	    int exposureVisibility = exposureButton.getVisibility();
	    int exposureLockVisibility = exposureLockButton.getVisibility();
	    assertTrue(audioControlButton.getVisibility() == ((has_audio_control_button && !immersive_mode) ? View.VISIBLE : View.GONE));
	    assertTrue(popupButton.getVisibility() == (immersive_mode ? View.GONE : View.VISIBLE));
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);

	    View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take photo");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take photo");

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
		    assertTrue(!mPreview.isTakingPhoto() || switchCameraButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || switchVideoButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || popupButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || audioControlButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || exposureButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || exposureLockButton.getVisibility() == View.GONE);
		    // trash/share not yet shown, as still taking the photo
		    assertTrue(!mPreview.isTakingPhoto() || trashButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || shareButton.getVisibility() == View.GONE);
	    }
		Log.d(TAG, "done taking photo");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");
		assertTrue(mPreview.count_cameraTakePicture==1);
		
		// don't need to wait until image queue empty, as Open Camera shouldn't use background thread for preview pause option

		Bitmap thumbnail = mActivity.gallery_bitmap;
		assertTrue(thumbnail != null);

		int n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		int exp_n_new_files = is_raw ? 2 : 1;
		Log.d(TAG, "exp_n_new_files: " + exp_n_new_files);
		assertTrue(n_new_files == exp_n_new_files);

		// now preview should be paused
		assertTrue(!mPreview.isPreviewStarted()); // check preview paused
	    assertTrue(switchCameraButton.getVisibility() == View.GONE);
	    assertTrue(switchVideoButton.getVisibility() == View.GONE);
	    assertTrue(exposureButton.getVisibility() == View.GONE);
	    assertTrue(exposureLockButton.getVisibility() == View.GONE);
	    assertTrue(audioControlButton.getVisibility() == View.GONE);
	    assertTrue(popupButton.getVisibility() == View.GONE);
	    assertTrue(trashButton.getVisibility() == View.VISIBLE);
	    assertTrue(shareButton.getVisibility() == View.VISIBLE);

		Log.d(TAG, "about to click preview");
	    TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
		Log.d(TAG, "done click preview");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync 3");

		// check photo not deleted
		n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		Log.d(TAG, "exp_n_new_files: " + exp_n_new_files);
		assertTrue(n_new_files == exp_n_new_files);

	    assertTrue(mPreview.isPreviewStarted()); // check preview restarted
	    assertTrue(switchCameraButton.getVisibility() == View.VISIBLE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    //assertTrue(flashButton.getVisibility() == flashVisibility);
	    //assertTrue(focusButton.getVisibility() == focusVisibility);
	    if( !immersive_mode ) {
		    assertTrue(exposureButton.getVisibility() == exposureVisibility);
		    assertTrue(exposureLockButton.getVisibility() == exposureLockVisibility);
	    }
	    assertTrue(audioControlButton.getVisibility() == (has_audio_control_button ? View.VISIBLE : View.GONE));
	    assertTrue(popupButton.getVisibility() == View.VISIBLE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);

	    // check still same icon even after a delay
		assertTrue(mActivity.gallery_bitmap == thumbnail);
	    Thread.sleep(1000);
		assertTrue(mActivity.gallery_bitmap == thumbnail);
	}

	public void testTakePhotoPreviewPaused() throws InterruptedException {
		Log.d(TAG, "testTakePhotoPreviewPaused");
		setToDefault();
		subTestTakePhotoPreviewPaused(false, false);
	}
	
	public void testTakePhotoPreviewPausedAudioButton() throws InterruptedException {
		Log.d(TAG, "testTakePhotoPreviewPausedAudioButton");
		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getAudioControlPreferenceKey(), "voice");
		editor.apply();
		updateForSettings();

		subTestTakePhotoPreviewPaused(false, false);
	}
	
	// If this test fails, make sure we've manually selected that folder (as permission can't be given through the test framework).
	public void testTakePhotoPreviewPausedSAF() throws InterruptedException {
		Log.d(TAG, "testTakePhotoPreviewPausedSAF");

		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ) {
			Log.d(TAG, "SAF requires Android Lollipop or better");
			return;
		}

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getUsingSAFPreferenceKey(), true);
		editor.putString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), "content://com.android.externalstorage.documents/tree/primary%3ADCIM%2FOpenCamera");
		editor.apply();
		updateForSettings();

		subTestTakePhotoPreviewPaused(false, false);
	}
	
	private void subTestTakePhotoPreviewPausedTrash(boolean is_raw) throws InterruptedException {
		// count initial files in folder
		File folder = mActivity.getImageFolder();
		int n_files = folder.listFiles().length;
		Log.d(TAG, "n_files at start: " + n_files);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getPausePreviewPreferenceKey(), true);
		editor.apply();

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
		boolean has_audio_control_button = !sharedPreferences.getString(PreferenceKeys.getAudioControlPreferenceKey(), "none").equals("none");

	    assertTrue(mPreview.isPreviewStarted());
		
	    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
	    //View flashButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.flash);
	    //View focusButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.focus_mode);
	    View exposureButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure);
	    View exposureLockButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure_lock);
	    View audioControlButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.audio_control);
	    View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);
	    View trashButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.trash);
	    View shareButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.share);
	    assertTrue(switchCameraButton.getVisibility() == View.VISIBLE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    // flash and focus etc default visibility tested in another test
	    // but store status to compare with later
	    //int flashVisibility = flashButton.getVisibility();
	    //int focusVisibility = focusButton.getVisibility();
	    int exposureVisibility = exposureButton.getVisibility();
	    int exposureLockVisibility = exposureLockButton.getVisibility();
	    assertTrue(audioControlButton.getVisibility() == (has_audio_control_button ? View.VISIBLE : View.GONE));
	    assertTrue(popupButton.getVisibility() == View.VISIBLE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);

	    View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take photo");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take photo");

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
		    assertTrue(!mPreview.isTakingPhoto() || switchCameraButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || switchVideoButton.getVisibility() == View.GONE);
		    //assertTrue(!mPreview.isTakingPhoto() || flashButton.getVisibility() == View.GONE);
		    //assertTrue(!mPreview.isTakingPhoto() || focusButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || exposureButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || exposureLockButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || audioControlButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || popupButton.getVisibility() == View.GONE);
		    // trash/share not yet shown, as still taking the photo
		    assertTrue(!mPreview.isTakingPhoto() || trashButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || shareButton.getVisibility() == View.GONE);
	    }
		Log.d(TAG, "done taking photo");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");
		Log.d(TAG, "count_cameraTakePicture: " + mPreview.count_cameraTakePicture);
		assertTrue(mPreview.count_cameraTakePicture==1);
		
		// don't need to wait until image queue empty, as Open Camera shouldn't use background thread for preview pause option

		Bitmap thumbnail = mActivity.gallery_bitmap;
		assertTrue(thumbnail != null);
		
		int n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		int exp_n_new_files = is_raw ? 2 : 1;
		Log.d(TAG, "exp_n_new_files: " + exp_n_new_files);
		assertTrue(n_new_files == exp_n_new_files);

		// now preview should be paused
		assertTrue(!mPreview.isPreviewStarted()); // check preview restarted
	    assertTrue(switchCameraButton.getVisibility() == View.GONE);
	    assertTrue(switchVideoButton.getVisibility() == View.GONE);
	    //assertTrue(flashButton.getVisibility() == View.GONE);
	    //assertTrue(focusButton.getVisibility() == View.GONE);
	    assertTrue(exposureButton.getVisibility() == View.GONE);
	    assertTrue(exposureLockButton.getVisibility() == View.GONE);
	    assertTrue(audioControlButton.getVisibility() == View.GONE);
	    assertTrue(popupButton.getVisibility() == View.GONE);
	    assertTrue(trashButton.getVisibility() == View.VISIBLE);
	    assertTrue(shareButton.getVisibility() == View.VISIBLE);

		Log.d(TAG, "about to click trash");
		clickView(trashButton);
		Log.d(TAG, "done click trash");

		// check photo(s) deleted
		n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		assertTrue(n_new_files == 0);

		assertTrue(mPreview.isPreviewStarted()); // check preview restarted
	    assertTrue(switchCameraButton.getVisibility() == View.VISIBLE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    //assertTrue(flashButton.getVisibility() == flashVisibility);
	    //assertTrue(focusButton.getVisibility() == focusVisibility);
	    assertTrue(exposureButton.getVisibility() == exposureVisibility);
	    assertTrue(exposureLockButton.getVisibility() == exposureLockVisibility);
	    assertTrue(audioControlButton.getVisibility() == (has_audio_control_button ? View.VISIBLE : View.GONE));
	    assertTrue(popupButton.getVisibility() == View.VISIBLE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);

	    // icon may be null, or have been set to another image - only changed after a delay
	    Thread.sleep(2000);
		Log.d(TAG, "gallery_bitmap: " + mActivity.gallery_bitmap);
		Log.d(TAG, "thumbnail: " + thumbnail);
		assertTrue(mActivity.gallery_bitmap != thumbnail);
	}

	public void testTakePhotoPreviewPausedTrash() throws InterruptedException {
		Log.d(TAG, "testTakePhotoPreviewPausedTrash");
		setToDefault();
		subTestTakePhotoPreviewPausedTrash(false);
	}

	/** Equivalent of testTakePhotoPreviewPausedTrash(), but for Storage Access Framework.
	 *  If this test fails, make sure we've manually selected that folder (as permission can't be given through the test framework).
	 */
	public void testTakePhotoPreviewPausedTrashSAF() throws InterruptedException {
		Log.d(TAG, "testTakePhotoPreviewPausedTrashSAF");

		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ) {
			Log.d(TAG, "SAF requires Android Lollipop or better");
			return;
		}

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getUsingSAFPreferenceKey(), true);
		editor.putString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), "content://com.android.externalstorage.documents/tree/primary%3ADCIM%2FOpenCamera");
		editor.apply();
		updateForSettings();

		subTestTakePhotoPreviewPausedTrash(false);
	}

	/** Like testTakePhotoPreviewPausedTrash() but taking 2 photos, only deleting the most recent - make
	 *  sure we don't delete both images!
	 */
	public void testTakePhotoPreviewPausedTrash2() throws InterruptedException {
		Log.d(TAG, "testTakePhotoPreviewPausedTrash2");
		setToDefault();

		subTestTakePhotoPreviewPaused(false, false);

		mPreview.count_cameraTakePicture = 0; // need to reset

		subTestTakePhotoPreviewPausedTrash(false);
	}

	/** Equivalent of testTakePhotoPreviewPausedTrash(), but with Raw enabled.
	 */
	public void testTakePhotoPreviewPausedTrashRaw() throws InterruptedException {
		Log.d(TAG, "testTakePhotoPreviewPausedTrashRaw");
		if( !mPreview.usingCamera2API() ) {
			return;
		}
		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getRawPreferenceKey(), "preference_raw_yes");
		editor.apply();
		updateForSettings();

		subTestTakePhotoPreviewPausedTrash(true);
	}

	/** Take a photo with RAW that we keep, then take a photo without RAW that we delete, and ensure we
	 *  don't delete the previous RAW image!
	 */
	public void testTakePhotoPreviewPausedTrashRaw2() throws InterruptedException {
		Log.d(TAG, "testTakePhotoPreviewPausedTrashRaw2");
		if( !mPreview.usingCamera2API() ) {
			return;
		}
		setToDefault();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getRawPreferenceKey(), "preference_raw_yes");
		editor.apply();
		updateForSettings();

		subTestTakePhotoPreviewPaused(false, true);

		settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		editor = settings.edit();
		editor.putString(PreferenceKeys.getRawPreferenceKey(), "preference_raw_no");
		editor.apply();
		updateForSettings();
		mPreview.count_cameraTakePicture = 0; // need to reset

		subTestTakePhotoPreviewPausedTrash(false);
	}

	/* Tests that we don't do an extra autofocus when taking a photo, if recently touch-focused.
	 */
	public void testTakePhotoQuickFocus() throws InterruptedException {
		Log.d(TAG, "testTakePhotoQuickFocus");
		setToDefault();
		
		assertTrue(mPreview.isPreviewStarted());
		
		// touch to auto-focus with focus area
		// autofocus shouldn't be immediately, but after a delay
		Thread.sleep(1000);
	    int saved_count = mPreview.count_cameraAutoFocus;
		TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
		Log.d(TAG, "1 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
		assertTrue(mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() != null);
	    assertTrue(mPreview.getCameraController().getFocusAreas().size() == 1);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);

	    // wait 3s for auto-focus to complete
		Thread.sleep(3000);

		View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take photo");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take photo");

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
	    }
		Log.d(TAG, "done taking photo");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");
		assertTrue(mPreview.count_cameraTakePicture==1);

		// taking photo shouldn't have done an auto-focus, and still have focus areas
		Log.d(TAG, "2 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
		assertTrue(mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() != null);
	    assertTrue(mPreview.getCameraController().getFocusAreas().size() == 1);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);
	}

	private void takePhotoRepeatFocus(boolean locked) throws InterruptedException {
		Log.d(TAG, "takePhotoRepeatFocus");
		setToDefault();
		if( locked ) {
			switchToFocusValue("focus_mode_locked");
		}

		assertTrue(mPreview.isPreviewStarted());
		
		// touch to auto-focus with focus area
		// autofocus shouldn't be immediately, but after a delay
		Thread.sleep(1000);
	    int saved_count = mPreview.count_cameraAutoFocus;
		TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
		Log.d(TAG, "1 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
		assertTrue(mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() != null);
	    assertTrue(mPreview.getCameraController().getFocusAreas().size() == 1);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);

	    // wait 3s for auto-focus to complete, and 5s to require additional auto-focus when taking a photo
		Thread.sleep(8000);

		View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take photo");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take photo");

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
	    }
		Log.d(TAG, "done taking photo");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");
		assertTrue(mPreview.count_cameraTakePicture==1);

		// taking photo should have done an auto-focus iff in automatic mode, and still have focus areas
		Log.d(TAG, "2 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		assertTrue(mPreview.count_cameraAutoFocus == (locked ? saved_count+1 : saved_count+2));
		assertTrue(mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() != null);
	    assertTrue(mPreview.getCameraController().getFocusAreas().size() == 1);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);
	}

	/* Tests that we do an extra autofocus when taking a photo, if too long since last touch-focused.
	 */
	public void testTakePhotoRepeatFocus() throws InterruptedException {
		Log.d(TAG, "testTakePhotoRepeatFocus");
		takePhotoRepeatFocus(false);
	}

	/* Tests that we don't do an extra autofocus when taking a photo, if too long since last touch-focused, when in locked focus mode.
	 */
	public void testTakePhotoRepeatFocusLocked() throws InterruptedException {
		Log.d(TAG, "testTakePhotoRepeatFocusLocked");
		takePhotoRepeatFocus(true);
	}

	/* Tests taking a photo with animation and shutter disabled, and not setting focus areas
	 */
	public void testTakePhotoAlt() throws InterruptedException {
		Log.d(TAG, "testTakePhotoAlt");
		setToDefault();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getThumbnailAnimationPreferenceKey(), false);
		editor.putBoolean(PreferenceKeys.getShutterSoundPreferenceKey(), false);
		editor.apply();

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
		boolean has_audio_control_button = !sharedPreferences.getString(PreferenceKeys.getAudioControlPreferenceKey(), "none").equals("none");

	    assertTrue(mPreview.isPreviewStarted());
		
		// count initial files in folder
		File folder = mActivity.getImageFolder();
		int n_files = folder.listFiles().length;
		Log.d(TAG, "n_files at start: " + n_files);
		
	    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
	    //View flashButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.flash);
	    //View focusButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.focus_mode);
	    View exposureButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure);
	    View exposureLockButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure_lock);
	    View audioControlButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.audio_control);
	    View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);
	    View trashButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.trash);
	    View shareButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.share);
	    assertTrue(switchCameraButton.getVisibility() == View.VISIBLE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    // flash and focus etc default visibility tested in another test
	    // but store status to compare with later
	    //int flashVisibility = flashButton.getVisibility();
	    //int focusVisibility = focusButton.getVisibility();
	    int exposureVisibility = exposureButton.getVisibility();
	    int exposureLockVisibility = exposureLockButton.getVisibility();
	    assertTrue(audioControlButton.getVisibility() == (has_audio_control_button ? View.VISIBLE : View.GONE));
	    assertTrue(popupButton.getVisibility() == View.VISIBLE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);

		// autofocus shouldn't be immediately, but after a delay
		Thread.sleep(2000);
	    int saved_count = mPreview.count_cameraAutoFocus;

	    View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take photo");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take photo");

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
		    assertTrue(!mPreview.isTakingPhoto() || switchCameraButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || switchVideoButton.getVisibility() == View.GONE);
		    //assertTrue(!mPreview.isTakingPhoto() || flashButton.getVisibility() == View.GONE);
		    //assertTrue(!mPreview.isTakingPhoto() || focusButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || exposureButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || exposureLockButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || audioControlButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || popupButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || trashButton.getVisibility() == View.GONE);
		    assertTrue(!mPreview.isTakingPhoto() || shareButton.getVisibility() == View.GONE);
	    }
		Log.d(TAG, "done taking photo");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");
		assertTrue(mPreview.count_cameraTakePicture==1);

		mActivity.waitUntilImageQueueEmpty();
		
		int n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		assertTrue(n_new_files == 1);

		// taking photo should have done an auto-focus, and no focus areas
		Log.d(TAG, "2 count_cameraAutoFocus: " + mPreview.count_cameraAutoFocus);
		Log.d(TAG, "saved_count: " + saved_count);
		assertTrue(mPreview.count_cameraAutoFocus == saved_count+1);
		assertTrue(!mPreview.hasFocusArea());
	    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
	    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);

		// trash/share only shown when preview is paused after taking a photo

		assertTrue(mPreview.isPreviewStarted()); // check preview restarted
	    assertTrue(switchCameraButton.getVisibility() == View.VISIBLE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    //assertTrue(flashButton.getVisibility() == flashVisibility);
	    //assertTrue(focusButton.getVisibility() == focusVisibility);
	    assertTrue(exposureButton.getVisibility() == exposureVisibility);
	    assertTrue(exposureLockButton.getVisibility() == exposureLockVisibility);
	    assertTrue(audioControlButton.getVisibility() == (has_audio_control_button ? View.VISIBLE : View.GONE));
	    assertTrue(popupButton.getVisibility() == View.VISIBLE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);
	}

	private void takePhotoLoop(int count) {
		// count initial files in folder
		File folder = mActivity.getImageFolder();
		int n_files = folder.listFiles().length;
		Log.d(TAG, "n_files at start: " + n_files);

		int start_count = mPreview.count_cameraTakePicture;
		for(int i=0;i<count;i++) {
		    View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
			Log.d(TAG, "about to click take photo");
		    clickView(takePhotoButton);
			Log.d(TAG, "wait until finished taking photo");
		    while( mPreview.isTakingPhoto() ) {
		    }
			Log.d(TAG, "done taking photo");
			this.getInstrumentation().waitForIdleSync();

			/*int n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == mPreview.count_cameraTakePicture - start_count);*/
			assertTrue(i+1 == mPreview.count_cameraTakePicture - start_count);
		}

		mActivity.waitUntilImageQueueEmpty();
		int n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		assertTrue(n_new_files == count);
	}

	/* Tests taking photos repeatedly with auto-stabilise enabled.
	 * Tests with front and back; and then tests again with test_low_memory set.
	 */
	public void testTakePhotoAutoLevel() {
		Log.d(TAG, "testTakePhotoAutoLevel");
		setToDefault();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getAutoStabilisePreferenceKey(), true);
		editor.apply();

		assertTrue(mPreview.isPreviewStarted());
		final int n_photos_c = 5;

		takePhotoLoop(n_photos_c);
		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
			int cameraId = mPreview.getCameraId();
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    while( switchCameraButton.getVisibility() != View.VISIBLE ) {
		    	// wait until photo is taken and button is visible again
		    }
		    clickView(switchCameraButton);
			int new_cameraId = mPreview.getCameraId();
			assertTrue(cameraId != new_cameraId);
			takePhotoLoop(n_photos_c);
		    while( switchCameraButton.getVisibility() != View.VISIBLE ) {
		    	// wait until photo is taken and button is visible again
		    }
		    clickView(switchCameraButton);
			new_cameraId = mPreview.getCameraId();
			assertTrue(cameraId == new_cameraId);
		}

		mActivity.test_low_memory = true;

		takePhotoLoop(n_photos_c);
		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
			int cameraId = mPreview.getCameraId();
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    while( switchCameraButton.getVisibility() != View.VISIBLE ) {
		    	// wait until photo is taken and button is visible again
		    }
		    clickView(switchCameraButton);
			int new_cameraId = mPreview.getCameraId();
			assertTrue(cameraId != new_cameraId);
			takePhotoLoop(n_photos_c);
		    while( switchCameraButton.getVisibility() != View.VISIBLE ) {
		    	// wait until photo is taken and button is visible again
		    }
		    clickView(switchCameraButton);
			new_cameraId = mPreview.getCameraId();
			assertTrue(cameraId == new_cameraId);
		}
	}

	private void takePhotoLoopAngles(int [] angles) {
		// count initial files in folder
		mActivity.test_have_angle = true;
		File folder = mActivity.getImageFolder();
		int n_files = folder.listFiles().length;
		Log.d(TAG, "n_files at start: " + n_files);

		int start_count = mPreview.count_cameraTakePicture;
		for(int i=0;i<angles.length;i++) {
			mActivity.test_angle = angles[mPreview.count_cameraTakePicture - start_count];
		    View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
			Log.d(TAG, "about to click take photo count: " + i);
		    clickView(takePhotoButton);
			Log.d(TAG, "wait until finished taking photo count: " + i);
		    while( mPreview.isTakingPhoto() ) {
		    }
			Log.d(TAG, "done taking photo count: " + i);
			this.getInstrumentation().waitForIdleSync();

			/*int n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == mPreview.count_cameraTakePicture - start_count);*/
			assertTrue(i+1 == mPreview.count_cameraTakePicture - start_count);
		}

		mActivity.waitUntilImageQueueEmpty();
		int n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		assertTrue(n_new_files == angles.length); // if we fail here, be careful we haven't lost images (i.e., waitUntilImageQueueEmpty() returns before all images are saved); note that in some cases, this test fails here because the activity onPause() after clicking take photo?!

		mActivity.test_have_angle = false;
	}

	/* Tests taking photos repeatedly with auto-stabilise enabled, at various angles.
	 * Tests with front and back; and then tests again with test_low_memory set.
	 */
	public void testTakePhotoAutoLevelAngles() {
		Log.d(TAG, "testTakePhotoAutoLevel");
		setToDefault();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getAutoStabilisePreferenceKey(), true);
		editor.apply();

		assertTrue(mPreview.isPreviewStarted());
		final int [] angles = new int[]{0, -129, 30, -44, 61, -89, 179};

		takePhotoLoopAngles(angles);
		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
			int cameraId = mPreview.getCameraId();
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    while( switchCameraButton.getVisibility() != View.VISIBLE ) {
		    	// wait until photo is taken and button is visible again
		    }
		    clickView(switchCameraButton);
			int new_cameraId = mPreview.getCameraId();
			assertTrue(cameraId != new_cameraId);
			takePhotoLoopAngles(angles);
		    while( switchCameraButton.getVisibility() != View.VISIBLE ) {
		    	// wait until photo is taken and button is visible again
		    }
		    clickView(switchCameraButton);
			new_cameraId = mPreview.getCameraId();
			assertTrue(cameraId == new_cameraId);
		}

		mActivity.test_low_memory = true;

		takePhotoLoopAngles(angles);
		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
			int cameraId = mPreview.getCameraId();
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    while( switchCameraButton.getVisibility() != View.VISIBLE ) {
		    	// wait until photo is taken and button is visible again
		    }
		    clickView(switchCameraButton);
			int new_cameraId = mPreview.getCameraId();
			assertTrue(cameraId != new_cameraId);
			takePhotoLoopAngles(angles);
		    while( switchCameraButton.getVisibility() != View.VISIBLE ) {
		    	// wait until photo is taken and button is visible again
		    }
		    clickView(switchCameraButton);
			new_cameraId = mPreview.getCameraId();
			assertTrue(cameraId == new_cameraId);
		}
	}

	private interface VideoTestCallback {
		int doTest(); // return expected number of new files (or -1 to indicate not to check this)
	}
	
	private void subTestTakeVideo(boolean test_exposure_lock, boolean test_focus_area, boolean allow_failure, boolean immersive_mode, VideoTestCallback test_cb, long time_ms, boolean max_filesize, boolean subtitles) throws InterruptedException {
		assertTrue(mPreview.isPreviewStarted());

		if( test_exposure_lock && !mPreview.supportsExposureLock() ) {
			return;
		}

		View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		View pauseVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.pause_video);
	    View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
		if( mPreview.isVideo() ) {
			assertTrue( (Integer)takePhotoButton.getTag() == net.sourceforge.opencamera.R.drawable.take_video_selector );
			assertTrue( takePhotoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.start_video) ) );
			assertTrue( pauseVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.pause_video) ) );
			assertTrue( switchVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.switch_to_photo) ) );
		}
		else {
			assertTrue( (Integer)takePhotoButton.getTag() == net.sourceforge.opencamera.R.drawable.take_photo_selector );
			assertTrue( takePhotoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.take_photo) ) );
			assertTrue( pauseVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.pause_video) ) );
			assertTrue( switchVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.switch_to_video) ) );
		}
		assertTrue( pauseVideoButton.getVisibility() == View.INVISIBLE );

		if( !mPreview.isVideo() ) {
			clickView(switchVideoButton);
		}
	    assertTrue(mPreview.isVideo());
		assertTrue(mPreview.isPreviewStarted());
		assertTrue( (Integer)takePhotoButton.getTag() == net.sourceforge.opencamera.R.drawable.take_video_selector );
		assertTrue( takePhotoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.start_video) ) );
		assertTrue( pauseVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.pause_video) ) );
		assertTrue( switchVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.switch_to_photo) ) );
		assertTrue( pauseVideoButton.getVisibility() == View.INVISIBLE );

		// count initial files in folder
		File folder = mActivity.getImageFolder();
		Log.d(TAG, "folder: " + folder);
		int n_files = folder.listFiles().length;
		Log.d(TAG, "n_files at start: " + n_files);
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
		boolean has_audio_control_button = !sharedPreferences.getString(PreferenceKeys.getAudioControlPreferenceKey(), "none").equals("none");

	    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    //View flashButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.flash);
	    //View focusButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.focus_mode);
	    View exposureButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure);
	    View exposureLockButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure_lock);
	    View audioControlButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.audio_control);
	    View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);
	    View trashButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.trash);
	    View shareButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.share);
	    assertTrue(switchCameraButton.getVisibility() == (immersive_mode ? View.GONE : View.VISIBLE));
	    assertTrue(switchVideoButton.getVisibility() == (immersive_mode ? View.GONE : View.VISIBLE));
	    // but store status to compare with later
	    int exposureVisibility = exposureButton.getVisibility();
	    int exposureLockVisibility = exposureLockButton.getVisibility();
	    assertTrue(audioControlButton.getVisibility() == ((has_audio_control_button && !immersive_mode) ? View.VISIBLE : View.GONE));
	    assertTrue(popupButton.getVisibility() == (immersive_mode ? View.GONE : View.VISIBLE));
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);

	    assertTrue( (Integer)takePhotoButton.getTag() == net.sourceforge.opencamera.R.drawable.take_video_selector );
		assertTrue( takePhotoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.start_video) ) );
		Log.d(TAG, "about to click take video");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take video");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");

		int exp_n_new_files = 0;
	    if( mPreview.isTakingPhoto() ) {
		    assertTrue( (Integer)takePhotoButton.getTag() == net.sourceforge.opencamera.R.drawable.take_video_recording );
			assertTrue( takePhotoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.stop_video) ) );
			assertTrue( pauseVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.pause_video) ) );
			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N )
				assertTrue( pauseVideoButton.getVisibility() == View.VISIBLE );
			else
				assertTrue( pauseVideoButton.getVisibility() == View.INVISIBLE );
		    assertTrue(switchCameraButton.getVisibility() == View.GONE);
		    assertTrue(switchVideoButton.getVisibility() == (immersive_mode ? View.GONE : View.VISIBLE));
		    assertTrue(audioControlButton.getVisibility() == View.GONE);
		    assertTrue(popupButton.getVisibility() == (!immersive_mode && mPreview.supportsFlash() ? View.VISIBLE : View.GONE)); // popup button only visible when recording video if flash supported
		    assertTrue(exposureButton.getVisibility() == exposureVisibility);
		    assertTrue(exposureLockButton.getVisibility() == exposureLockVisibility);
		    assertTrue(trashButton.getVisibility() == View.GONE);
		    assertTrue(shareButton.getVisibility() == View.GONE);

		    if( test_cb == null ) {
			    if( !immersive_mode && time_ms > 500 ) {
				    // test turning torch on/off (if in immersive mode, popup button will be hidden)
					switchToFlashValue("flash_torch");
				    Thread.sleep(500);
					switchToFlashValue("flash_off");
			    }
			    
		    	Thread.sleep(time_ms);
			    assertTrue( (Integer)takePhotoButton.getTag() == net.sourceforge.opencamera.R.drawable.take_video_recording );
				assertTrue( takePhotoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.stop_video) ) );
				assertTrue( pauseVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.pause_video) ) );

				assertTrue(!mPreview.hasFocusArea());
				if( !allow_failure ) {
				    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
				    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);
				}

			    if( test_focus_area ) {
					// touch to auto-focus with focus area
					Log.d(TAG, "touch to focus");
					TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
				    Thread.sleep(1000); // wait for autofocus
					assertTrue(mPreview.hasFocusArea());
				    assertTrue(mPreview.getCameraController().getFocusAreas() != null);
				    assertTrue(mPreview.getCameraController().getFocusAreas().size() == 1);
				    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
				    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);
					Log.d(TAG, "done touch to focus");

					// this time, don't wait
					Log.d(TAG, "touch again to focus");
					TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
			    }
			    
			    if( test_exposure_lock ) {
					Log.d(TAG, "test exposure lock");
				    assertTrue( !mPreview.getCameraController().getAutoExposureLock() );
				    clickView(exposureLockButton);
					this.getInstrumentation().waitForIdleSync();
					Log.d(TAG, "after idle sync");
				    assertTrue( mPreview.getCameraController().getAutoExposureLock() );
				    Thread.sleep(2000);
			    }
		
			    assertTrue( (Integer)takePhotoButton.getTag() == net.sourceforge.opencamera.R.drawable.take_video_recording );
				assertTrue( takePhotoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.stop_video) ) );
				assertTrue( pauseVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.pause_video) ) );
				Log.d(TAG, "about to click stop video");
			    clickView(takePhotoButton);
				Log.d(TAG, "done clicking stop video");
				this.getInstrumentation().waitForIdleSync();
				Log.d(TAG, "after idle sync");
		    }
		    else {
		    	exp_n_new_files = test_cb.doTest();
		    }
	    }
	    else {
			Log.d(TAG, "didn't start video");
			assertTrue(allow_failure);
	    }

		assertTrue( folder.exists() );
		int n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		if( test_cb == null ) {
			if( time_ms <= 500 ) {
				// if quick, should have deleted corrupt video - but may be device dependent, sometimes we manage to record a video anyway!
				assertTrue(n_new_files == 0 || n_new_files == 1);
			}
			else if( subtitles ) {
				assertEquals(n_new_files, 2);
			}
			else {
				assertEquals(n_new_files, 1);
			}
		}
		else {
			Log.d(TAG, "exp_n_new_files: " + exp_n_new_files);
			if( exp_n_new_files >= 0 ) {
				assertEquals(n_new_files, exp_n_new_files);
			}
		}

		// trash/share only shown when preview is paused after taking a photo

		assertTrue(mPreview.isPreviewStarted()); // check preview restarted
	    if( !max_filesize ) {
	    	// if doing restart on max filesize, we may have already restarted by now (on Camera2 API at least)
		    assertTrue(switchCameraButton.getVisibility() == (immersive_mode ? View.GONE : View.VISIBLE));
		    assertTrue(audioControlButton.getVisibility() == ((has_audio_control_button && !immersive_mode) ? View.VISIBLE : View.GONE));
	    }
	    assertTrue(switchVideoButton.getVisibility() == (immersive_mode ? View.GONE : View.VISIBLE));
	    assertTrue(exposureButton.getVisibility() == exposureVisibility);
	    assertTrue(exposureLockButton.getVisibility() == exposureLockVisibility);
	    assertTrue(popupButton.getVisibility() == (immersive_mode ? View.GONE : View.VISIBLE));
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);

	    assertTrue( (Integer)takePhotoButton.getTag() == net.sourceforge.opencamera.R.drawable.take_video_selector );
		assertEquals( takePhotoButton.getContentDescription(), mActivity.getResources().getString(net.sourceforge.opencamera.R.string.start_video) );
		assertTrue( pauseVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.pause_video) ) );
		Log.d(TAG, "pauseVideoButton.getVisibility(): " + pauseVideoButton.getVisibility());
		assertTrue( pauseVideoButton.getVisibility() == View.INVISIBLE );
	}

	public void testTakeVideo() throws InterruptedException {
		Log.d(TAG, "testTakeVideo");

		setToDefault();

		subTestTakeVideo(false, false, false, false, null, 5000, false, false);
	}

	public void testTakeVideoAudioControl() throws InterruptedException {
		Log.d(TAG, "testTakeVideoAudioControl");

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getAudioControlPreferenceKey(), "voice");
		editor.apply();
		updateForSettings();

		subTestTakeVideo(false, false, false, false, null, 5000, false, false);
	}

	// If this test fails, make sure we've manually selected that folder (as permission can't be given through the test framework).
	public void testTakeVideoSAF() throws InterruptedException {
		Log.d(TAG, "testTakeVideoSAF");

		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ) {
			Log.d(TAG, "SAF requires Android Lollipop or better");
			return;
		}

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getUsingSAFPreferenceKey(), true);
		editor.putString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), "content://com.android.externalstorage.documents/tree/primary%3ADCIM%2FOpenCamera");
		editor.apply();
		updateForSettings();

		subTestTakeVideo(false, false, false, false, null, 5000, false, false);
	}

	public void testTakeVideoSubtitles() throws InterruptedException {
		Log.d(TAG, "testTakeVideoSubtitles");

		setToDefault();
		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(PreferenceKeys.getVideoSubtitlePref(), "preference_video_subtitle_yes");
			editor.apply();
			updateForSettings();
		}

		subTestTakeVideo(false, false, false, false, null, 5000, false, true);
	}

	/** Set pausing and resuming video.
	 */
	public void testTakeVideoPause() throws InterruptedException {
		Log.d(TAG, "testTakeVideoPause");

		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.N ) {
			Log.d(TAG, "pause video requires Android N or better");
			return;
		}

		setToDefault();

		subTestTakeVideo(false, false, false, false, new VideoTestCallback() {
			@Override
			public int doTest() {
				View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
				View pauseVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.pause_video);
				final long time_tol_ms = 1000;

				Log.d(TAG, "wait before pausing");
				try {
					Thread.sleep(3000);
				}
				catch(InterruptedException e) {
					e.printStackTrace();
					assertTrue(false);
				}
				assertTrue( takePhotoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.stop_video) ) );
				assertTrue( pauseVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.pause_video) ) );
				assertTrue( pauseVideoButton.getVisibility() == View.VISIBLE );
				assertTrue( mPreview.isVideoRecording() );
				assertTrue( !mPreview.isVideoRecordingPaused() );
				long video_time = mPreview.getVideoTime();
				Log.d(TAG, "video time: " + video_time);
				assertTrue( video_time >= 3000 - time_tol_ms );
				assertTrue( video_time <= 3000 + time_tol_ms );

				Log.d(TAG, "about to click pause video");
				clickView(pauseVideoButton);
				Log.d(TAG, "done clicking pause video");
				getInstrumentation().waitForIdleSync();
				Log.d(TAG, "after idle sync");

				assertTrue( pauseVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.resume_video) ) );
				assertTrue( pauseVideoButton.getVisibility() == View.VISIBLE );
				assertTrue( mPreview.isVideoRecording() );
				assertTrue( mPreview.isVideoRecordingPaused() );

				Log.d(TAG, "wait before resuming");
				try {
					Thread.sleep(3000);
				}
				catch(InterruptedException e) {
					e.printStackTrace();
					assertTrue(false);
				}
				assertTrue( pauseVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.resume_video) ) );
				assertTrue( pauseVideoButton.getVisibility() == View.VISIBLE );
				assertTrue( mPreview.isVideoRecording() );
				assertTrue( mPreview.isVideoRecordingPaused() );
				video_time = mPreview.getVideoTime();
				Log.d(TAG, "video time: " + video_time);
				assertTrue( video_time >= 3000 - time_tol_ms );
				assertTrue( video_time <= 3000 + time_tol_ms );

				Log.d(TAG, "about to click resume video");
				clickView(pauseVideoButton);
				Log.d(TAG, "done clicking resume video");
				getInstrumentation().waitForIdleSync();
				Log.d(TAG, "after idle sync");

				assertTrue( pauseVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.pause_video) ) );
				assertTrue( pauseVideoButton.getVisibility() == View.VISIBLE );
				assertTrue( mPreview.isVideoRecording() );
				assertTrue( !mPreview.isVideoRecordingPaused() );

				Log.d(TAG, "wait before stopping");
				try {
					Thread.sleep(3000);
				}
				catch(InterruptedException e) {
					e.printStackTrace();
					assertTrue(false);
				}
				Log.d(TAG, "takePhotoButton description: " + takePhotoButton.getContentDescription());
				assertTrue( takePhotoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.stop_video) ) );
				assertTrue( pauseVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.pause_video) ) );
				assertTrue( pauseVideoButton.getVisibility() == View.VISIBLE );
				assertTrue( mPreview.isVideoRecording() );
				assertTrue( !mPreview.isVideoRecordingPaused() );
				video_time = mPreview.getVideoTime();
				Log.d(TAG, "video time: " + video_time);
				assertTrue( video_time >= 6000 - time_tol_ms );
				assertTrue( video_time <= 6000 + time_tol_ms );

				Log.d(TAG, "about to click stop video");
				clickView(takePhotoButton);
				Log.d(TAG, "done clicking stop video");
				getInstrumentation().waitForIdleSync();
				Log.d(TAG, "after idle sync");

				return 1;
			}
		}, 5000, false, false);
	}

	/** Set pausing and stopping video.
	 */
	public void testTakeVideoPauseStop() throws InterruptedException {
		Log.d(TAG, "testTakeVideoPauseStop");

		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.N ) {
			Log.d(TAG, "pause video requires Android N or better");
			return;
		}

		setToDefault();

		subTestTakeVideo(false, false, false, false, new VideoTestCallback() {
			@Override
			public int doTest() {
				View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
				View pauseVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.pause_video);
				final long time_tol_ms = 1000;

				Log.d(TAG, "wait before pausing");
				try {
					Thread.sleep(3000);
				}
				catch(InterruptedException e) {
					e.printStackTrace();
					assertTrue(false);
				}
				assertTrue( takePhotoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.stop_video) ) );
				assertTrue( pauseVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.pause_video) ) );
				assertTrue( pauseVideoButton.getVisibility() == View.VISIBLE );
				assertTrue( mPreview.isVideoRecording() );
				assertTrue( !mPreview.isVideoRecordingPaused() );
				long video_time = mPreview.getVideoTime();
				Log.d(TAG, "video time: " + video_time);
				assertTrue( video_time >= 3000 - time_tol_ms );
				assertTrue( video_time <= 3000 + time_tol_ms );

				Log.d(TAG, "about to click pause video");
				clickView(pauseVideoButton);
				Log.d(TAG, "done clicking pause video");
				getInstrumentation().waitForIdleSync();
				Log.d(TAG, "after idle sync");

				assertTrue( pauseVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.resume_video) ) );
				assertTrue( pauseVideoButton.getVisibility() == View.VISIBLE );
				assertTrue( mPreview.isVideoRecording() );
				assertTrue( mPreview.isVideoRecordingPaused() );

				Log.d(TAG, "wait before stopping");
				try {
					Thread.sleep(3000);
				}
				catch(InterruptedException e) {
					e.printStackTrace();
					assertTrue(false);
				}
				Log.d(TAG, "takePhotoButton description: " + takePhotoButton.getContentDescription());
				assertTrue( takePhotoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.stop_video) ) );
				assertTrue( pauseVideoButton.getContentDescription().equals( mActivity.getResources().getString(net.sourceforge.opencamera.R.string.resume_video) ) );
				assertTrue( pauseVideoButton.getVisibility() == View.VISIBLE );
				assertTrue( mPreview.isVideoRecording() );
				assertTrue( mPreview.isVideoRecordingPaused() );
				video_time = mPreview.getVideoTime();
				Log.d(TAG, "video time: " + video_time);
				assertTrue( video_time >= 3000 - time_tol_ms );
				assertTrue( video_time <= 3000 + time_tol_ms );

				Log.d(TAG, "about to click stop video");
				clickView(takePhotoButton);
				Log.d(TAG, "done clicking stop video");
				getInstrumentation().waitForIdleSync();
				Log.d(TAG, "after idle sync");

				return 1;
			}
		}, 5000, false, false);
	}

	/** Set available memory to make sure that we stop before running out of memory.
	 *  This test is fine-tuned to Nexus 6, as we measure hitting max filesize based on time.
	 */
	public void testTakeVideoAvailableMemory() throws InterruptedException {
		Log.d(TAG, "testTakeVideoAvailableMemory");

		setToDefault();
		
		mActivity.getApplicationInterface().test_set_available_memory = true;
		mActivity.getApplicationInterface().test_available_memory = 50000000;

		subTestTakeVideo(false, false, false, false, new VideoTestCallback() {
			@Override
			public int doTest() {
		    	// wait until automatically stops
				Log.d(TAG, "wait until video recording stops");
				long time_s = System.currentTimeMillis();
				long video_time_s = mPreview.getVideoTime();
		    	while( mPreview.isVideoRecording() ) {
				    assertTrue( System.currentTimeMillis() - time_s <= 30000 );
				    long video_time = mPreview.getVideoTime();
				    assertTrue( video_time >= video_time_s );
		    	}
				Log.d(TAG, "video recording now stopped");
				return 1;
			}
		}, 5000, true, false);
	}

	/** Set available memory small enough to make sure we don't even attempt to record video.
	 */
	public void testTakeVideoAvailableMemory2() throws InterruptedException {
		Log.d(TAG, "testTakeVideoAvailableMemory2");

		setToDefault();
		
		mActivity.getApplicationInterface().test_set_available_memory = true;
		mActivity.getApplicationInterface().test_available_memory = 5000000;

		subTestTakeVideo(false, false, true, false, new VideoTestCallback() {
			@Override
			public int doTest() {
		    	// wait until automatically stops
				Log.d(TAG, "wait until video recording stops");
				assertFalse( mPreview.isVideoRecording() );
				Log.d(TAG, "video recording now stopped");
				return 0;
			}
		}, 5000, true, false);
	}

	/** Set maximum filesize so that we get approx 3s of video time. Check that recording stops and restarts within 10s.
	 *  Then check recording stops again within 10s.
	 *  This test is fine-tuned to Nexus 6, as we measure hitting max filesize based on time.
	 */
	public void testTakeVideoMaxFileSize1() throws InterruptedException {
		Log.d(TAG, "testTakeVideoMaxFileSize1");

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		//editor.putString(PreferenceKeys.getVideoQualityPreferenceKey(mPreview.getCameraId()), "" + CamcorderProfile.QUALITY_HIGH); // set to highest quality (4K on Nexus 6)
		//editor.putString(PreferenceKeys.getVideoMaxFileSizePreferenceKey(), "15728640"); // approx 3-4s on Nexus 6 at 4K
		editor.putString(PreferenceKeys.getVideoMaxFileSizePreferenceKey(), "9437184"); // approx 3-4s on Nexus 6 at 4K
		editor.apply();
		updateForSettings();

		subTestTakeVideo(false, false, false, false, new VideoTestCallback() {
			@Override
			public int doTest() {
		    	// wait until automatically stops
				Log.d(TAG, "wait until video recording stops");
				long time_s = System.currentTimeMillis();
				long video_time_s = mPreview.getVideoTime();
		    	while( mPreview.isVideoRecording() ) {
				    assertTrue( System.currentTimeMillis() - time_s <= 8000 );
				    long video_time = mPreview.getVideoTime();
				    assertTrue( video_time >= video_time_s );
		    	}
				Log.d(TAG, "video recording now stopped - wait for restart");
				video_time_s = mPreview.getVideoAccumulatedTime();
				Log.d(TAG, "video_time_s: " + video_time_s);
				// now ensure we'll restart within a reasonable time
				time_s = System.currentTimeMillis();
		    	while( !mPreview.isVideoRecording() ) {
		    		long c_time = System.currentTimeMillis();
		    		if( c_time - time_s > 10000 ) {
				    	Log.e(TAG, "time: " + (c_time - time_s));
		    		}
				    assertTrue( c_time - time_s <= 10000 );
		    	}
		    	// wait for stop again
				time_s = System.currentTimeMillis();
		    	while( mPreview.isVideoRecording() ) {
		    		long c_time = System.currentTimeMillis();
		    		if( c_time - time_s > 10000 ) {
				    	Log.e(TAG, "time: " + (c_time - time_s));
		    		}
				    assertTrue( c_time - time_s <= 10000 );
				    long video_time = mPreview.getVideoTime();
				    if( video_time < video_time_s )
				    	Log.d(TAG, "compare: " + video_time_s + " to " + video_time);
				    assertTrue( video_time + 1 >= video_time_s );
		    	}
				Log.d(TAG, "video recording now stopped again");
				return -1; // the number of videos recorded can very, as the max duration corresponding to max filesize can vary widly
			}
		}, 5000, true, false);
	}

	/** Max filesize is for ~4.5s, and max duration is 5s, check we only get 1 video.
	 *  This test is fine-tuned to Nexus 6, as we measure hitting max filesize based on time.
	 */
	public void testTakeVideoMaxFileSize2() throws InterruptedException {
		Log.d(TAG, "testTakeVideoMaxFileSize2");

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getVideoQualityPreferenceKey(mPreview.getCameraId()), "" + CamcorderProfile.QUALITY_HIGH); // set to highest quality (4K on Nexus 6)
		editor.putString(PreferenceKeys.getVideoMaxFileSizePreferenceKey(), "23592960"); // approx 4.5s on Nexus 6 at 4K
		editor.putString(PreferenceKeys.getVideoMaxDurationPreferenceKey(), "5");
		editor.apply();
		updateForSettings();

		subTestTakeVideo(false, false, false, false, new VideoTestCallback() {
			@Override
			public int doTest() {
		    	// wait until automatically stops
				Log.d(TAG, "wait until video recording stops");
				long time_s = System.currentTimeMillis();
				long video_time_s = mPreview.getVideoTime();
		    	while( mPreview.isVideoRecording() ) {
				    assertTrue( System.currentTimeMillis() - time_s <= 8000 );
				    long video_time = mPreview.getVideoTime();
				    assertTrue( video_time >= video_time_s );
		    	}
				Log.d(TAG, "video recording now stopped - check we don't restart");
				video_time_s = mPreview.getVideoAccumulatedTime();
				Log.d(TAG, "video_time_s: " + video_time_s);
				// now ensure we don't restart
				time_s = System.currentTimeMillis();
		    	while( System.currentTimeMillis() - time_s <= 5000 ) {
				    assertFalse( mPreview.isVideoRecording() );
		    	}
				return 1;
			}
		}, 5000, true, false);
	}

	/* Max filesize for ~5s, max duration 7s, max n_repeats 1 - to ensure we're not repeating indefinitely.
	 * This test is fine-tuned to Nexus 6, as we measure hitting max filesize based on time.
	 */
	public void testTakeVideoMaxFileSize3() throws InterruptedException {
		Log.d(TAG, "testTakeVideoMaxFileSize3");

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getVideoQualityPreferenceKey(mPreview.getCameraId()), "" + CamcorderProfile.QUALITY_HIGH); // set to highest quality (4K on Nexus 6)
		//editor.putString(PreferenceKeys.getVideoMaxFileSizePreferenceKey(), "26214400"); // approx 5s on Nexus 6 at 4K
		editor.putString(PreferenceKeys.getVideoMaxFileSizePreferenceKey(), "15728640"); // approx 5s on Nexus 6 at 4K
		editor.putString(PreferenceKeys.getVideoMaxDurationPreferenceKey(), "7");
		editor.putString(PreferenceKeys.getVideoRestartPreferenceKey(), "1");
		editor.apply();
		updateForSettings();

		subTestTakeVideo(false, false, false, false, new VideoTestCallback() {
			@Override
			public int doTest() {
		    	// wait until we should have stopped - 2x7s, but add 6s for each of 4 restarts
				Log.d(TAG, "wait until video recording completely stopped");
				try {
					Thread.sleep(38000);
				}
				catch(InterruptedException e) {
					e.printStackTrace();
					assertTrue(false);
				}
				Log.d(TAG, "ensure we've really stopped");
				long time_s = System.currentTimeMillis();
		    	while( System.currentTimeMillis() - time_s <= 5000 ) {
				    assertFalse( mPreview.isVideoRecording() );
		    	}
				return -1; // the number of videos recorded can very, as the max duration corresponding to max filesize can vary widly
			}
		}, 5000, true, false);
	}

	public void testTakeVideoStabilization() throws InterruptedException {
		Log.d(TAG, "testTakeVideoStabilization");

	    if( !mPreview.supportsVideoStabilization() ) {
			Log.d(TAG, "video stabilization not supported");
	    	return;
	    }
	    assertFalse(mPreview.getCameraController().getVideoStabilization());

	    setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getVideoStabilizationPreferenceKey(), true);
		editor.apply();
		updateForSettings();
	    assertTrue(mPreview.getCameraController().getVideoStabilization());

		subTestTakeVideo(false, false, false, false, null, 5000, false, false);

	    assertTrue(mPreview.getCameraController().getVideoStabilization());
	}

	public void testTakeVideoExposureLock() throws InterruptedException {
		Log.d(TAG, "testTakeVideoExposureLock");

		setToDefault();

		subTestTakeVideo(true, false, false, false, null, 5000, false, false);
	}

	public void testTakeVideoFocusArea() throws InterruptedException {
		Log.d(TAG, "testTakeVideoFocusArea");

		setToDefault();

		subTestTakeVideo(false, true, false, false, null, 5000, false, false);
	}

	public void testTakeVideoQuick() throws InterruptedException {
		Log.d(TAG, "testTakeVideoQuick");

		setToDefault();

    	// still need a short delay (at least 500ms, otherwise Open Camera will ignore the repeated stop)
		subTestTakeVideo(false, false, false, false, null, 500, false, false);
	}

	// If this test fails, make sure we've manually selected that folder (as permission can't be given through the test framework).
	public void testTakeVideoQuickSAF() throws InterruptedException {
		Log.d(TAG, "testTakeVideoQuickSAF");

		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ) {
			Log.d(TAG, "SAF requires Android Lollipop or better");
			return;
		}

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getUsingSAFPreferenceKey(), true);
		editor.putString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), "content://com.android.externalstorage.documents/tree/primary%3ADCIM%2FOpenCamera");
		editor.apply();
		updateForSettings();

    	// still need a short delay (at least 500ms, otherwise Open Camera will ignore the repeated stop)
		subTestTakeVideo(false, false, false, false, null, 500, false, false);
	}

	public void testTakeVideoForceFailure() throws InterruptedException {
		Log.d(TAG, "testTakeVideoForceFailure");

		setToDefault();

		mActivity.getPreview().test_video_failure = true;
		subTestTakeVideo(false, false, true, false, null, 5000, false, false);
	}

	/* Test can be reliable on some devices, test no longer run as part of test suites.
	 */
	public void testTakeVideo4K() throws InterruptedException {
		Log.d(TAG, "testTakeVideo4K");
		
		if( !mActivity.supportsForceVideo4K() ) {
			return;
		}

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getForceVideo4KPreferenceKey(), true);
		editor.apply();
		updateForSettings();

		subTestTakeVideo(false, false, true, false, null, 5000, false, false);
	}

	/* Test can be reliable on some devices, test no longer run as part of test suites.
	 */
	public void testTakeVideoFPS() throws InterruptedException {
		Log.d(TAG, "testTakeVideoFPS");
		
		setToDefault();
		final String [] fps_values = new String[]{"15", "24", "25", "30", "60"};
		for(String fps_value : fps_values) {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(PreferenceKeys.getVideoFPSPreferenceKey(), fps_value);
			editor.apply();
			restart(); // should restart to emulate what happens in real app

			Log.d(TAG, "test video with fps: " + fps_value);
			boolean allow_failure = fps_value.equals("24") || fps_value.equals("25") || fps_value.equals("60");
			subTestTakeVideo(false, false, allow_failure, false, null, 5000, false, false);
		}
	}

	/* Test can be reliable on some devices, test no longer run as part of test suites.
	 */
	public void testTakeVideoBitrate() throws InterruptedException {
		Log.d(TAG, "testTakeVideoBitrate");
		
		setToDefault();
		final String [] bitrate_values = new String[]{"1000000", "10000000", "20000000", "50000000"};
		//final String [] bitrate_values = new String[]{"1000000", "10000000", "20000000", "30000000"};
		for(String bitrate_value : bitrate_values) {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(PreferenceKeys.getVideoBitratePreferenceKey(), bitrate_value);
			editor.apply();
			restart(); // should restart to emulate what happens in real app

			Log.d(TAG, "test video with bitrate: " + bitrate_value);
			boolean allow_failure = bitrate_value.equals("30000000") || bitrate_value.equals("50000000");
			subTestTakeVideo(false, false, allow_failure, false, null, 5000, false, false);
		}
	}

	private void subTestTakeVideoMaxDuration(boolean restart, boolean interrupt) throws InterruptedException {
		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(PreferenceKeys.getVideoMaxDurationPreferenceKey(), "15");
			if( restart ) {
				editor.putString(PreferenceKeys.getVideoRestartPreferenceKey(), "1");
			}
			editor.apply();
		}

		assertTrue(mPreview.isPreviewStarted());

		View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
		if( !mPreview.isVideo() ) {
			clickView(switchVideoButton);
		}
	    assertTrue(mPreview.isVideo());
		assertTrue(mPreview.isPreviewStarted());
		
		// count initial files in folder
		File folder = mActivity.getImageFolder();
		int n_files = folder.listFiles().length;
		Log.d(TAG, "n_files at start: " + n_files);
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
		boolean has_audio_control_button = !sharedPreferences.getString(PreferenceKeys.getAudioControlPreferenceKey(), "none").equals("none");

		View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    //View flashButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.flash);
	    //View focusButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.focus_mode);
	    View exposureButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure);
	    View exposureLockButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.exposure_lock);
	    View audioControlButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.audio_control);
	    View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);
	    View trashButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.trash);
	    View shareButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.share);
	    assertTrue(switchCameraButton.getVisibility() == View.VISIBLE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    // flash and focus etc default visibility tested in another test
	    // but store status to compare with later
	    //int flashVisibility = flashButton.getVisibility();
	    //int focusVisibility = focusButton.getVisibility();
	    int exposureVisibility = exposureButton.getVisibility();
	    int exposureLockVisibility = exposureLockButton.getVisibility();
	    assertTrue(audioControlButton.getVisibility() == (has_audio_control_button ? View.VISIBLE : View.GONE));
	    assertTrue(popupButton.getVisibility() == View.VISIBLE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);

	    View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take video");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take video");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");

		assertTrue( mPreview.isTakingPhoto() );

		assertTrue(switchCameraButton.getVisibility() == View.GONE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    //assertTrue(flashButton.getVisibility() == flashVisibility);
	    //assertTrue(focusButton.getVisibility() == View.GONE);
	    assertTrue(exposureButton.getVisibility() == exposureVisibility);
	    assertTrue(exposureLockButton.getVisibility() == exposureLockVisibility);
	    assertTrue(audioControlButton.getVisibility() == View.GONE);
	    assertTrue(popupButton.getVisibility() == (mPreview.supportsFlash() ? View.VISIBLE : View.GONE)); // popup button only visible when recording video if flash supported
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);

	    Thread.sleep(10000);
		Log.d(TAG, "check still taking video");
		assertTrue( mPreview.isTakingPhoto() );

		int n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		assertTrue(n_new_files == 1);

		if( restart ) {
			if( interrupt ) {
			    Thread.sleep(5100);
			    restart();
				Log.d(TAG, "done restart");
				// now wait, and check we don't crash
			    Thread.sleep(5000);
			    return;
			}
			else {
			    Thread.sleep(10000);
				Log.d(TAG, "check restarted video");
				assertTrue( mPreview.isTakingPhoto() );
				assertTrue( folder.exists() );
				n_new_files = folder.listFiles().length - n_files;
				Log.d(TAG, "n_new_files: " + n_new_files);
				assertTrue(n_new_files == 2);

				Thread.sleep(15000);
			}
		}
		else {
		    Thread.sleep(8000);
		}
		Log.d(TAG, "check stopped taking video");
		assertTrue( !mPreview.isTakingPhoto() );
		
		assertTrue( folder.exists() );
		n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		assertTrue(n_new_files == (restart ? 2 : 1));

		// trash/share only shown when preview is paused after taking a photo

		assertTrue(mPreview.isPreviewStarted()); // check preview restarted
	    assertTrue(switchCameraButton.getVisibility() == View.VISIBLE);
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);
	    //assertTrue(flashButton.getVisibility() == flashVisibility);
	    //assertTrue(focusButton.getVisibility() == focusVisibility);
	    assertTrue(exposureButton.getVisibility() == exposureVisibility);
	    assertTrue(exposureLockButton.getVisibility() == exposureLockVisibility);
	    assertTrue(audioControlButton.getVisibility() == (has_audio_control_button ? View.VISIBLE : View.GONE));
	    assertTrue(popupButton.getVisibility() == View.VISIBLE);
	    assertTrue(trashButton.getVisibility() == View.GONE);
	    assertTrue(shareButton.getVisibility() == View.GONE);
	}

	public void testTakeVideoMaxDuration() throws InterruptedException {
		Log.d(TAG, "testTakeVideoMaxDuration");
		
		setToDefault();
		
		subTestTakeVideoMaxDuration(false, false);
	}

	public void testTakeVideoMaxDurationRestart() throws InterruptedException {
		Log.d(TAG, "testTakeVideoMaxDurationRestart");
		
		setToDefault();
		
		subTestTakeVideoMaxDuration(true, false);
	}

	public void testTakeVideoMaxDurationRestartInterrupt() throws InterruptedException {
		Log.d(TAG, "testTakeVideoMaxDurationRestartInterrupt");
		
		setToDefault();
		
		subTestTakeVideoMaxDuration(true, true);
	}

	public void testTakeVideoSettings() throws InterruptedException {
		Log.d(TAG, "testTakeVideoSettings");
		
		setToDefault();
		
		assertTrue(mPreview.isPreviewStarted());

		View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
		if( !mPreview.isVideo() ) {
			clickView(switchVideoButton);
		}
	    assertTrue(mPreview.isVideo());
		assertTrue(mPreview.isPreviewStarted());
		
		// count initial files in folder
		File folder = mActivity.getImageFolder();
		int n_files = folder.listFiles().length;
		Log.d(TAG, "n_files at start: " + n_files);
		
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);

	    View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take video");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take video");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");

		assertTrue( mPreview.isTakingPhoto() );

	    Thread.sleep(2000);
		Log.d(TAG, "check still taking video");
		assertTrue( mPreview.isTakingPhoto() );

		int n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		assertTrue(n_new_files == 1);

		// now go to settings
	    View settingsButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.settings);
		Log.d(TAG, "about to click settings");
	    clickView(settingsButton);
		Log.d(TAG, "done clicking settings");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");
		assertTrue( !mPreview.isTakingPhoto() );

		assertTrue( folder.exists() );
		n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		assertTrue(n_new_files == 1);

		Thread.sleep(500);
		mActivity.runOnUiThread(new Runnable() {
			public void run() {
				Log.d(TAG, "on back pressed...");
			    mActivity.onBackPressed();
			}
		});
		// need to wait for UI code to finish before leaving
		this.getInstrumentation().waitForIdleSync();
	    Thread.sleep(500);
		assertTrue( !mPreview.isTakingPhoto() );
		
		Log.d(TAG, "about to click take video");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take video");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");

		assertTrue( mPreview.isTakingPhoto() );

		assertTrue( folder.exists() );
		n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		assertTrue(n_new_files == 2);

	}

	/** Switch to macro focus, go to settings, check switched to continuous mode, leave settings, check back in macro mode, then test recording.
	 */
	public void testTakeVideoMacro() throws InterruptedException {
		Log.d(TAG, "testTakeVideoMacro");
	    if( !mPreview.supportsFocus() ) {
	    	return;
	    }
		
		setToDefault();
		
		assertTrue(mPreview.isPreviewStarted());

		View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
		if( !mPreview.isVideo() ) {
			clickView(switchVideoButton);
		}
	    assertTrue(mPreview.isVideo());
		assertTrue(mPreview.isPreviewStarted());

		switchToFocusValue("focus_mode_macro");

		// now go to settings
	    View settingsButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.settings);
		Log.d(TAG, "about to click settings");
	    clickView(settingsButton);
		Log.d(TAG, "done clicking settings");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");
		assertTrue( !mPreview.isTakingPhoto() );

		Thread.sleep(500);

		assertTrue(mPreview.getCurrentFocusValue().equals("focus_mode_macro"));

		mActivity.runOnUiThread(new Runnable() {
			public void run() {
				Log.d(TAG, "on back pressed...");
			    mActivity.onBackPressed();
			}
		});
		// need to wait for UI code to finish before leaving
		this.getInstrumentation().waitForIdleSync();
	    Thread.sleep(500);

		// count initial files in folder
		File folder = mActivity.getImageFolder();
		int n_files = folder.listFiles().length;
		Log.d(TAG, "n_files at start: " + n_files);
		
	    assertTrue(switchVideoButton.getVisibility() == View.VISIBLE);

	    View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take video");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take video");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");

		assertTrue( mPreview.isTakingPhoto() );

	    Thread.sleep(2000);
		Log.d(TAG, "check still taking video");
		assertTrue( mPreview.isTakingPhoto() );

		int n_new_files = folder.listFiles().length - n_files;
		Log.d(TAG, "n_new_files: " + n_new_files);
		assertTrue(n_new_files == 1);

	}

	public void testTakeVideoFlashVideo() throws InterruptedException {
		Log.d(TAG, "testTakeVideoFlashVideo");

		if( !mPreview.supportsFlash() ) {
			return;
		}
		
		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getVideoFlashPreferenceKey(), true);
		editor.apply();
		updateForSettings();
		
		assertTrue(mPreview.isPreviewStarted());

		View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
		if( !mPreview.isVideo() ) {
			clickView(switchVideoButton);
		}
	    assertTrue(mPreview.isVideo());
		assertTrue(mPreview.isPreviewStarted());
		
	    View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take video");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take video");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");

		assertTrue( mPreview.isTakingPhoto() );

	    Thread.sleep(1500);
		Log.d(TAG, "check still taking video");
		assertTrue( mPreview.isTakingPhoto() );

		// wait until flash off
		long time_s = System.currentTimeMillis();
		for(;;) {
		    if( !mPreview.getCameraController().getFlashValue().equals("flash_torch") ) {
		    	break;
		    }
		    assertTrue( System.currentTimeMillis() - time_s <= 200 );
		}
		
		// wait until flash on
		time_s = System.currentTimeMillis();
		for(;;) {
		    if( mPreview.getCameraController().getFlashValue().equals("flash_torch") ) {
		    	break;
		    }
		    assertTrue( System.currentTimeMillis() - time_s <= 1100 );
		}

		// wait until flash off
		time_s = System.currentTimeMillis();
		for(;;) {
		    if( !mPreview.getCameraController().getFlashValue().equals("flash_torch") ) {
		    	break;
		    }
		    assertTrue( System.currentTimeMillis() - time_s <= 200 );
		}

		// wait until flash on
		time_s = System.currentTimeMillis();
		for(;;) {
		    if( mPreview.getCameraController().getFlashValue().equals("flash_torch") ) {
		    	break;
		    }
		    assertTrue( System.currentTimeMillis() - time_s <= 1100 );
		}

		Log.d(TAG, "about to click stop video");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking stop video");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");
		
		// test flash now off
	    assertTrue( !mPreview.getCameraController().getFlashValue().equals("flash_torch") );
	}

	// type: 0 - go to background; 1 - go to settings; 2 - go to popup
	private void subTestTimer(int type) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getTimerPreferenceKey(), "10");
		editor.putBoolean(PreferenceKeys.getTimerBeepPreferenceKey(), false);
		editor.apply();

		assertTrue(!mPreview.isOnTimer());

		// count initial files in folder
		File folder = mActivity.getImageFolder();
		int n_files = folder.listFiles().length;
		Log.d(TAG, "n_files at start: " + n_files);

		View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take photo");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take photo");
		assertTrue(mPreview.isOnTimer());
		assertTrue(mPreview.count_cameraTakePicture==0);
		
		try {
			// wait 2s, and check we are still on timer, and not yet taken a photo
			Thread.sleep(2000);
			assertTrue(mPreview.isOnTimer());
			assertTrue(mPreview.count_cameraTakePicture==0);
			// quit and resume
			if( type == 0 )
				restart();
			else if( type == 1 ) {
			    View settingsButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.settings);
				Log.d(TAG, "about to click settings");
			    clickView(settingsButton);
				Log.d(TAG, "done clicking settings");
				this.getInstrumentation().waitForIdleSync();
				Log.d(TAG, "after idle sync");

				mActivity.runOnUiThread(new Runnable() {
					public void run() {
						Log.d(TAG, "on back pressed...");
					    mActivity.onBackPressed();
					}
				});
				// need to wait for UI code to finish before leaving
				this.getInstrumentation().waitForIdleSync();
			    Thread.sleep(500);
			}
			else {
			    View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);
			    clickView(popupButton);
			    while( !mActivity.popupIsOpen() ) {
			    }
			}
			takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		    // check timer cancelled, and not yet taken a photo
			assertTrue(!mPreview.isOnTimer());
			assertTrue(mPreview.count_cameraTakePicture==0);
			int n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == 0);

			// start timer again
			Log.d(TAG, "about to click take photo");
			assertTrue(mPreview.getCameraController() != null);
		    clickView(takePhotoButton);
			assertTrue(mPreview.getCameraController() != null);
			Log.d(TAG, "done clicking take photo");
			assertTrue(mPreview.isOnTimer());
			assertTrue(mPreview.count_cameraTakePicture==0);
			n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == 0);
			
			// wait 15s, and ensure we took a photo
			Thread.sleep(15000);
			Log.d(TAG, "waited, count now " + mPreview.count_cameraTakePicture);
			assertTrue(!mPreview.isOnTimer());
			assertTrue(mPreview.count_cameraTakePicture==1);
			n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == 1);
			
			// now set timer to 5s, and turn on pause_preview
			editor.putString(PreferenceKeys.getTimerPreferenceKey(), "5");
			editor.putBoolean(PreferenceKeys.getPausePreviewPreferenceKey(), true);
			editor.apply();

			Log.d(TAG, "about to click take photo");
			assertTrue(mPreview.getCameraController() != null);
		    clickView(takePhotoButton);
			assertTrue(mPreview.getCameraController() != null);
			Log.d(TAG, "done clicking take photo");
			assertTrue(mPreview.isOnTimer());
			assertTrue(mPreview.count_cameraTakePicture==1);
			n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == 1);

			// wait 10s, and ensure we took a photo
			Thread.sleep(10000);
			Log.d(TAG, "waited, count now " + mPreview.count_cameraTakePicture);
			assertTrue(!mPreview.isOnTimer());
			assertTrue(mPreview.count_cameraTakePicture==2);
			n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == 2);
			
			// now test cancelling
			Log.d(TAG, "about to click take photo");
			assertTrue(mPreview.getCameraController() != null);
		    clickView(takePhotoButton);
			assertTrue(mPreview.getCameraController() != null);
			Log.d(TAG, "done clicking take photo");
			assertTrue(mPreview.isOnTimer());
			assertTrue(mPreview.count_cameraTakePicture==2);
			n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == 2);

			// wait 2s, and cancel
			Thread.sleep(2000);
			Log.d(TAG, "about to click take photo to cance");
			assertTrue(mPreview.getCameraController() != null);
		    clickView(takePhotoButton);
			assertTrue(mPreview.getCameraController() != null);
			Log.d(TAG, "done clicking take photo to cancel");
			assertTrue(!mPreview.isOnTimer());
			assertTrue(mPreview.count_cameraTakePicture==2);
			n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == 2);

			// wait 8s, and ensure we didn't take a photo
			Thread.sleep(8000);
			Log.d(TAG, "waited, count now " + mPreview.count_cameraTakePicture);
			assertTrue(!mPreview.isOnTimer());
			assertTrue(mPreview.count_cameraTakePicture==2);
			n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == 2);
		}
		catch(InterruptedException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
	
	/* Test with 10s timer, start a photo, go to background, then back, then take another photo. We should only take 1 photo - the original countdown should not be active (nor should we crash)!
	 */
	public void testTimerBackground() {
		Log.d(TAG, "testTimerBackground");
		setToDefault();
		
		subTestTimer(0);
	}
	
	/* Test and going to settings.
	 */
	public void testTimerSettings() {
		Log.d(TAG, "testTimerSettings");
		setToDefault();
		
		subTestTimer(1);
	}
	
	/* Test and going to popup.
	 */
	public void testTimerPopup() {
		Log.d(TAG, "testTimerPopup");
		setToDefault();
		
		subTestTimer(2);
	}
	
	/* Takes video on a timer, but interrupts with restart.
	 */
	public void testVideoTimerInterrupt() {
		Log.d(TAG, "testVideoTimerInterrupt");
		setToDefault();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getTimerPreferenceKey(), "5");
		editor.putBoolean(PreferenceKeys.getTimerBeepPreferenceKey(), false);
		editor.apply();

		assertTrue(!mPreview.isOnTimer());

		// count initial files in folder
		File folder = mActivity.getImageFolder();
		int n_files = folder.listFiles().length;
		Log.d(TAG, "n_files at start: " + n_files);

	    View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
	    clickView(switchVideoButton);
	    assertTrue(mPreview.isVideo());

	    View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take photo");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take photo");
		assertTrue(mPreview.isOnTimer());
		assertTrue(mPreview.count_cameraTakePicture==0);
		
		try {
			// wait a moment after 5s, then restart
			Thread.sleep(5100);
			assertTrue(mPreview.count_cameraTakePicture==0);
			// quit and resume
			restart();
			Log.d(TAG, "done restart");

		    // check timer cancelled; may or may not have managed to take a photo
			assertTrue(!mPreview.isOnTimer());
		}
		catch(InterruptedException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	/* Tests that selecting a new flash and focus option, then reopening the popup menu, still has the correct option highlighted.
	 */
	public void testPopup() {
		Log.d(TAG, "testPopup");
		setToDefault();

		switchToFlashValue("flash_off");
		switchToFlashValue("flash_on");

		switchToFocusValue("focus_mode_macro");
		switchToFocusValue("focus_mode_auto");
	}

	/* Tests to do with video and popup menu.
	 */
	private void subTestVideoPopup(boolean on_timer) {
		Log.d(TAG, "subTestVideoPopup");

		assertTrue(!mPreview.isOnTimer());
		assertTrue(!mActivity.popupIsOpen());
	    View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);

	    if( !mPreview.isVideo() ) {
			View switchVideoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_video);
		    clickView(switchVideoButton);
		    assertTrue(mPreview.isVideo());
	    }

	    if( !on_timer ) {
	    	// open popup now
		    clickView(popupButton);
		    while( !mActivity.popupIsOpen() ) {
		    }
	    }
	    
	    View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take photo");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take photo");
		if( on_timer ) {
			assertTrue(mPreview.isOnTimer());
		}
		
		try {
			if( on_timer ) {
				Thread.sleep(2000);
	
				// now open popup
			    clickView(popupButton);
			    while( !mActivity.popupIsOpen() ) {
			    }

			    // check timer is cancelled
				assertTrue( !mPreview.isOnTimer() );

				// wait for timer (if it was still going)
				Thread.sleep(4000);

				// now check we still aren't recording, and that popup is still open
				assertTrue( mPreview.isVideo() );
				assertTrue( !mPreview.isTakingPhoto() );
				assertTrue( !mPreview.isOnTimer() );
				assertTrue( mActivity.popupIsOpen() );
			}
			else {
				Thread.sleep(1000);

				// now check we are recording video, and that popup is closed
				assertTrue( mPreview.isVideo() );
				assertTrue( mPreview.isTakingPhoto() );
				assertTrue( !mActivity.popupIsOpen() );
			}

			if( !on_timer ) {
				// (if on timer, the video will have stopped)
				List<String> supported_flash_values = mPreview.getSupportedFlashValues();
				if( supported_flash_values == null ) {
					// button shouldn't show at all
					assertTrue( popupButton.getVisibility() == View.GONE );
				}
				else {
					// now open popup again
				    clickView(popupButton);
				    while( !mActivity.popupIsOpen() ) {
				    }
					subTestPopupButtonAvailability("TEST_FLASH", "flash_off", supported_flash_values);
					subTestPopupButtonAvailability("TEST_FLASH", "flash_auto", supported_flash_values);
					subTestPopupButtonAvailability("TEST_FLASH", "flash_on", supported_flash_values);
					subTestPopupButtonAvailability("TEST_FLASH", "flash_torch", supported_flash_values);
					subTestPopupButtonAvailability("TEST_FLASH", "flash_red_eye", supported_flash_values);
					// only flash should be available
					subTestPopupButtonAvailability("TEST_FOCUS", "focus_mode_auto", null);
					subTestPopupButtonAvailability("TEST_FOCUS", "focus_mode_locked", null);
					subTestPopupButtonAvailability("TEST_FOCUS", "focus_mode_infinity", null);
					subTestPopupButtonAvailability("TEST_FOCUS", "focus_mode_macro", null);
					subTestPopupButtonAvailability("TEST_FOCUS", "focus_mode_fixed", null);
					subTestPopupButtonAvailability("TEST_FOCUS", "focus_mode_edof", null);
					subTestPopupButtonAvailability("TEST_FOCUS", "focus_mode_continuous_picture", null);
					subTestPopupButtonAvailability("TEST_FOCUS", "focus_mode_continuous_video", null);
					subTestPopupButtonAvailability("TEST_ISO", "auto", null);
					subTestPopupButtonAvailability("TEST_ISO", "100", null);
					subTestPopupButtonAvailability("TEST_ISO", "200", null);
					subTestPopupButtonAvailability("TEST_ISO", "400", null);
					subTestPopupButtonAvailability("TEST_ISO", "800", null);
					subTestPopupButtonAvailability("TEST_ISO", "1600", null);
					subTestPopupButtonAvailability("TEST_WHITE_BALANCE", false);
					subTestPopupButtonAvailability("TEST_SCENE_MODE", false);
					subTestPopupButtonAvailability("TEST_COLOR_EFFECT", false);
				}
			}

			Log.d(TAG, "now stop video");
		    clickView(takePhotoButton);
			Log.d(TAG, "done clicking stop video");
			this.getInstrumentation().waitForIdleSync();
			Log.d(TAG, "after idle sync");
			assertTrue( !mPreview.isTakingPhoto() );
			assertTrue( !mActivity.popupIsOpen() );

		}
		catch(InterruptedException e) {
			e.printStackTrace();
			assertTrue(false);
		}

		// now open popup again
	    clickView(popupButton);
	    while( !mActivity.popupIsOpen() ) {
	    }
	    subTestPopupButtonAvailability();
	}
	
	/* Tests that popup menu closes when we record video; then tests behaviour of popup.
	 */
	public void testVideoPopup() {
		Log.d(TAG, "testVideoPopup");
		setToDefault();

		subTestVideoPopup(false);

		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
			Log.d(TAG, "switch camera");
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    clickView(switchCameraButton);
			subTestVideoPopup(false);
	    }
	}

	/* Takes video on a timer, but checks that the popup menu stops video timer; then tests behaviour of popup.
	 */
	public void testVideoTimerPopup() {
		Log.d(TAG, "testVideoTimerPopup");
		setToDefault();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getTimerPreferenceKey(), "5");
		editor.putBoolean(PreferenceKeys.getTimerBeepPreferenceKey(), false);
		editor.apply();
		
		subTestVideoPopup(true);

		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
			Log.d(TAG, "switch camera");
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    clickView(switchCameraButton);
			subTestVideoPopup(true);
	    }
	}
	
	/* Tests taking photos repeatedly with auto-repeat "burst" method.
	 */
	public void testTakePhotoBurst() {
		Log.d(TAG, "testTakePhotoBurst");
		setToDefault();

		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(PreferenceKeys.getBurstModePreferenceKey(), "3");
			editor.apply();
		}

		// count initial files in folder
		File folder = mActivity.getImageFolder();
		int n_files = folder.listFiles().length;
		Log.d(TAG, "n_files at start: " + n_files);

		assertTrue(mPreview.count_cameraTakePicture==0);

		View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take photo");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take photo");
		assertTrue(!mPreview.isOnTimer());

		try {
			// wait 6s, and test that we've taken the photos by then
			Thread.sleep(6000);
		    assertTrue(mPreview.isPreviewStarted()); // check preview restarted
			Log.d(TAG, "count_cameraTakePicture: " + mPreview.count_cameraTakePicture);
			assertTrue(mPreview.count_cameraTakePicture==3);
			int n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == 3);

			// now test pausing and resuming
		    clickView(takePhotoButton);
		    pauseAndResume();
			// wait 5s, and test that we haven't taken any photos
			Thread.sleep(5000);
		    assertTrue(mPreview.isPreviewStarted()); // check preview restarted
			assertTrue(mPreview.count_cameraTakePicture==3);
			n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == 3);

			// test with preview paused
			{
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(PreferenceKeys.getPausePreviewPreferenceKey(), true);
				editor.apply();
			}
		    clickView(takePhotoButton);
			Thread.sleep(6000);
			assertTrue(mPreview.count_cameraTakePicture==6);
			n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == 6);
			assertTrue(!mPreview.isPreviewStarted()); // check preview paused

		    TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
			this.getInstrumentation().waitForIdleSync();
			n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == 6);
		    assertTrue(mPreview.isPreviewStarted()); // check preview restarted
			{
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(PreferenceKeys.getPausePreviewPreferenceKey(), false);
				editor.apply();
			}

			// now test burst interval
			{
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
				SharedPreferences.Editor editor = settings.edit();
				editor.putString(PreferenceKeys.getBurstModePreferenceKey(), "2");
				editor.putString(PreferenceKeys.getBurstIntervalPreferenceKey(), "3");
				editor.putBoolean(PreferenceKeys.getTimerBeepPreferenceKey(), false);
				editor.apply();
			}
		    clickView(takePhotoButton);
		    while( mPreview.isTakingPhoto() ) {
		    }
			Log.d(TAG, "done taking 1st photo");
			this.getInstrumentation().waitForIdleSync();
			assertTrue(mPreview.count_cameraTakePicture==7);
			mActivity.waitUntilImageQueueEmpty();
			n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == 7);
			// wait 2s, should still not have taken another photo
			Thread.sleep(2000);
			assertTrue(mPreview.count_cameraTakePicture==7);
			n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == 7);
			// wait another 5s, should have taken another photo (need to allow time for the extra auto-focus)
			Thread.sleep(5000);
			assertTrue(mPreview.count_cameraTakePicture==8);
			n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == 8);
			// wait 4s, should not have taken any more photos
			Thread.sleep(4000);
			assertTrue(mPreview.count_cameraTakePicture==8);
			n_new_files = folder.listFiles().length - n_files;
			Log.d(TAG, "n_new_files: " + n_new_files);
			assertTrue(n_new_files == 8);
		}
		catch(InterruptedException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	/* Tests that saving quality (i.e., resolution) settings can be done per-camera. Also checks that the supported picture sizes is as expected.
	 */
	public void testSaveQuality() {
		Log.d(TAG, "testSaveQuality");

		if( mPreview.getCameraControllerManager().getNumberOfCameras() <= 1 ) {
			return;
		}
		setToDefault();

	    List<CameraController.Size> preview_sizes = mPreview.getSupportedPictureSizes();

	    // change back camera to the last size
		CameraController.Size size = preview_sizes.get(preview_sizes.size()-1);
	    {
		    Log.d(TAG, "set size to " + size.width + " x " + size.height);
		    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(PreferenceKeys.getResolutionPreferenceKey(mPreview.getCameraId()), size.width + " " + size.height);
			editor.apply();
	    }
		
		// need to resume activity for it to take effect (for camera to be reopened)
	    pauseAndResume();
	    CameraController.Size new_size = mPreview.getCameraController().getPictureSize();
	    Log.d(TAG, "size is now " + new_size.width + " x " + new_size.height);
	    assertTrue(size.equals(new_size));

	    // switch camera to front
		int cameraId = mPreview.getCameraId();
	    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    clickView(switchCameraButton);
		int new_cameraId = mPreview.getCameraId();
		assertTrue(cameraId != new_cameraId);

	    List<CameraController.Size> front_preview_sizes = mPreview.getSupportedPictureSizes();

	    // change front camera to the last size
		CameraController.Size front_size = front_preview_sizes.get(front_preview_sizes.size()-1);
	    {
		    Log.d(TAG, "set front_size to " + front_size.width + " x " + front_size.height);
		    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(PreferenceKeys.getResolutionPreferenceKey(mPreview.getCameraId()), front_size.width + " " + front_size.height);
			editor.apply();
	    }
		
		// need to resume activity for it to take effect (for camera to be reopened)
	    pauseAndResume();
	    // check still on front camera
	    Log.d(TAG, "camera id " + mPreview.getCameraId());
		assertTrue(mPreview.getCameraId() == new_cameraId);
	    CameraController.Size front_new_size = mPreview.getCameraController().getPictureSize();
	    Log.d(TAG, "front size is now " + front_new_size.width + " x " + front_new_size.height);
	    assertTrue(front_size.equals(front_new_size));

	    // change front camera to the first size
		front_size = front_preview_sizes.get(0);
	    {
		    Log.d(TAG, "set front_size to " + front_size.width + " x " + front_size.height);
		    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(PreferenceKeys.getResolutionPreferenceKey(mPreview.getCameraId()), front_size.width + " " + front_size.height);
			editor.apply();
	    }
		
		// need to resume activity for it to take effect (for camera to be reopened)
	    pauseAndResume();
	    front_new_size = mPreview.getCameraController().getPictureSize();
	    Log.d(TAG, "front size is now " + front_new_size.width + " x " + front_new_size.height);
	    assertTrue(front_size.equals(front_new_size));

	    // switch camera to back
	    clickView(switchCameraButton);
		new_cameraId = mPreview.getCameraId();
		assertTrue(cameraId == new_cameraId);
		
		// now back camera size should still be what it was
	    {
		    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			String settings_size = settings.getString(PreferenceKeys.getResolutionPreferenceKey(mPreview.getCameraId()), "");
		    Log.d(TAG, "settings key is " + PreferenceKeys.getResolutionPreferenceKey(mPreview.getCameraId()));
		    Log.d(TAG, "settings size is " + settings_size);
	    }
	    new_size = mPreview.getCameraController().getPictureSize();
	    Log.d(TAG, "size is now " + new_size.width + " x " + new_size.height);
	    assertTrue(size.equals(new_size));
	}

	private void testExif(String file, boolean expect_gps) throws IOException {
		//final String TAG_GPS_IMG_DIRECTION = "GPSImgDirection";
		//final String TAG_GPS_IMG_DIRECTION_REF = "GPSImgDirectionRef";
		ExifInterface exif = new ExifInterface(file);
		assertTrue(exif.getAttribute(ExifInterface.TAG_ORIENTATION) != null);
		assertTrue(exif.getAttribute(ExifInterface.TAG_MAKE) != null);
		assertTrue(exif.getAttribute(ExifInterface.TAG_MODEL) != null);
		if( expect_gps ) {
			assertTrue(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null);
			assertTrue(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) != null);
			assertTrue(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null);
			assertTrue(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) != null);
			// can't read custom tags, even though we can write them?!
			//assertTrue(exif.getAttribute(TAG_GPS_IMG_DIRECTION) != null);
			//assertTrue(exif.getAttribute(TAG_GPS_IMG_DIRECTION_REF) != null);
		}
		else {
			assertTrue(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) == null);
			assertTrue(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) == null);
			assertTrue(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) == null);
			assertTrue(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) == null);
			// can't read custom tags, even though we can write them?!
			//assertTrue(exif.getAttribute(TAG_GPS_IMG_DIRECTION) == null);
			//assertTrue(exif.getAttribute(TAG_GPS_IMG_DIRECTION_REF) == null);
		}
	}

	private void subTestLocationOn(boolean gps_direction) throws IOException {
		Log.d(TAG, "subTestLocationOn");
		setToDefault();

		assertTrue(!mActivity.getLocationSupplier().hasLocationListeners());
		Log.d(TAG, "turn on location");
		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean(PreferenceKeys.getLocationPreferenceKey(), true);
			if( gps_direction ) {
				editor.putBoolean(PreferenceKeys.getGPSDirectionPreferenceKey(), true);
			}
			editor.apply();
			Log.d(TAG, "update settings after turning on location");
			updateForSettings();
			Log.d(TAG, "location should now be on");
		}

		assertTrue(mActivity.getLocationSupplier().hasLocationListeners());
		Log.d(TAG, "wait until received location");

		long start_t = System.currentTimeMillis();
		while( !mActivity.getLocationSupplier().testHasReceivedLocation() ) {
			this.getInstrumentation().waitForIdleSync();
			if( System.currentTimeMillis() - start_t > 20000 ) {
				// need to allow long time for testing devices without mobile network; will likely fail altogether if don't even have wifi
				assertTrue(false);
			}
		}
		Log.d(TAG, "have received location");
		this.getInstrumentation().waitForIdleSync();
	    assertTrue(mActivity.getLocationSupplier().getLocation() != null);
	    assertTrue(mPreview.count_cameraTakePicture==0);

		View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		mActivity.test_last_saved_image = null;
	    clickView(takePhotoButton);

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
	    }
		this.getInstrumentation().waitForIdleSync();
		assertTrue(mPreview.count_cameraTakePicture==1);
		mActivity.waitUntilImageQueueEmpty();
		assertTrue(mActivity.test_last_saved_image != null);
		testExif(mActivity.test_last_saved_image, true);

		// now test with auto-stabilise
		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean(PreferenceKeys.getAutoStabilisePreferenceKey(), true);
			editor.apply();
		}
		mActivity.test_last_saved_image = null;
	    clickView(takePhotoButton);

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
	    }
		this.getInstrumentation().waitForIdleSync();
		assertTrue(mPreview.count_cameraTakePicture==2);
		mActivity.waitUntilImageQueueEmpty();
		assertTrue(mActivity.test_last_saved_image != null);
		testExif(mActivity.test_last_saved_image, true);

		// switch to front camera
		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    clickView(switchCameraButton);
			assertTrue(mActivity.getLocationSupplier().hasLocationListeners());
			// shouldn't need to wait for test_has_received_location to be true, as should remember from before switching camera
		    assertTrue(mActivity.getLocationSupplier().getLocation() != null);
		}
	}

	/* Tests we save location data; also tests that we save other exif data.
	 * May fail on devices without mobile network, especially if we don't even have wifi.
	 */
	public void testLocationOn() throws IOException {
		Log.d(TAG, "testLocationOn");
		subTestLocationOn(false);
	}

	/* Tests we save location and gps direction.
	 * May fail on devices without mobile network, especially if we don't even have wifi.
	 */
	public void testLocationDirectionOn() throws IOException {
		Log.d(TAG, "testLocationDirectionOn");
		subTestLocationOn(true);
	}

	/* Tests we don't save location data; also tests that we save other exif data.
	 */
	private void subTestLocationOff(boolean gps_direction) throws IOException {
		setToDefault();

		if( gps_direction ) {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean(PreferenceKeys.getGPSDirectionPreferenceKey(), true);
			editor.apply();
			updateForSettings();
		}
		this.getInstrumentation().waitForIdleSync();
		assertTrue(!mActivity.getLocationSupplier().hasLocationListeners());
	    assertTrue(mActivity.getLocationSupplier().getLocation() == null);
	    assertTrue(mPreview.count_cameraTakePicture==0);

		View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		mActivity.test_last_saved_image = null;
	    clickView(takePhotoButton);

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
	    }
		this.getInstrumentation().waitForIdleSync();
		assertTrue(mPreview.count_cameraTakePicture==1);
		mActivity.waitUntilImageQueueEmpty();
		assertTrue(mActivity.test_last_saved_image != null);
		testExif(mActivity.test_last_saved_image, false);

		// now test with auto-stabilise
		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean(PreferenceKeys.getAutoStabilisePreferenceKey(), true);
			editor.apply();
		}
		mActivity.test_last_saved_image = null;
	    clickView(takePhotoButton);

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
	    }
		this.getInstrumentation().waitForIdleSync();
		assertTrue(mPreview.count_cameraTakePicture==2);
		mActivity.waitUntilImageQueueEmpty();
		assertTrue(mActivity.test_last_saved_image != null);
		testExif(mActivity.test_last_saved_image, false);

		// switch to front camera
		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    clickView(switchCameraButton);
			this.getInstrumentation().waitForIdleSync();
		    assertTrue(mActivity.getLocationSupplier().getLocation() == null);

		    clickView(switchCameraButton);
			this.getInstrumentation().waitForIdleSync();
		    assertTrue(mActivity.getLocationSupplier().getLocation() == null);
		}

		// now switch location back on
		Log.d(TAG, "now switch location back on");
		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean(PreferenceKeys.getLocationPreferenceKey(), true);
			editor.apply();
			restart(); // need to restart for this preference to take effect
		}

		long start_t = System.currentTimeMillis();
		while( !mActivity.getLocationSupplier().testHasReceivedLocation() ) {
			this.getInstrumentation().waitForIdleSync();
			if( System.currentTimeMillis() - start_t > 20000 ) {
				// need to allow long time for testing devices without mobile network; will likely fail altogether if don't even have wifi
				assertTrue(false);
			}
		}
		this.getInstrumentation().waitForIdleSync();
	    assertTrue(mActivity.getLocationSupplier().getLocation() != null);

		// switch to front camera
		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    clickView(switchCameraButton);
			// shouldn't need to wait for test_has_received_location to be true, as should remember from before switching camera
		    assertTrue(mActivity.getLocationSupplier().getLocation() != null);
		}
	}

	/* Tests we don't save location data; also tests that we save other exif data.
	 * May fail on devices without mobile network, especially if we don't even have wifi.
	 */
	public void testLocationOff() throws IOException {
		Log.d(TAG, "testLocationOff");
		subTestLocationOff(false);
	}

	/* Tests we save gps direction.
	 * May fail on devices without mobile network, especially if we don't even have wifi.
	 */
	public void testDirectionOn() throws IOException {
		Log.d(TAG, "testDirectionOn");
		subTestLocationOff(false);
	}

	/* Tests we can stamp date/time and location to photo.
	 * May fail on devices without mobile network, especially if we don't even have wifi.
	 */
	public void testPhotoStamp() throws IOException {
		Log.d(TAG, "testPhotoStamp");

		setToDefault();

		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(PreferenceKeys.getStampPreferenceKey(), "preference_stamp_yes");
			editor.apply();
			updateForSettings();
		}

	    assertTrue(mPreview.count_cameraTakePicture==0);

	    View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
	    clickView(takePhotoButton);

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
	    }
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "photo count: " + mPreview.count_cameraTakePicture);
		assertTrue(mPreview.count_cameraTakePicture==1);

		// now again with location
		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean(PreferenceKeys.getLocationPreferenceKey(), true);
			editor.apply();
			updateForSettings();
		}

		assertTrue( mActivity.getLocationSupplier().hasLocationListeners() );
		long start_t = System.currentTimeMillis();
		while( !mActivity.getLocationSupplier().testHasReceivedLocation() ) {
			this.getInstrumentation().waitForIdleSync();
			if( System.currentTimeMillis() - start_t > 20000 ) {
				// need to allow long time for testing devices without mobile network; will likely fail altogether if don't even have wifi
				assertTrue(false);
			}
		}
		this.getInstrumentation().waitForIdleSync();
	    assertTrue(mActivity.getLocationSupplier().getLocation() != null);

	    clickView(takePhotoButton);

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
	    }
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "photo count: " + mPreview.count_cameraTakePicture);
		assertTrue(mPreview.count_cameraTakePicture==2);

		// now again with custom text
		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(PreferenceKeys.getTextStampPreferenceKey(), "Test stamp!£$");
			editor.apply();
			updateForSettings();
		}

		assertTrue( mActivity.getLocationSupplier().hasLocationListeners() );
		while( !mActivity.getLocationSupplier().testHasReceivedLocation() ) {
		}
		this.getInstrumentation().waitForIdleSync();
	    assertTrue(mActivity.getLocationSupplier().getLocation() != null);

	    clickView(takePhotoButton);

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
	    }
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "photo count: " + mPreview.count_cameraTakePicture);
		assertTrue(mPreview.count_cameraTakePicture==3);

		// now test with auto-stabilise
		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean(PreferenceKeys.getAutoStabilisePreferenceKey(), true);
			editor.apply();
		}

	    clickView(takePhotoButton);

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
	    }
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "photo count: " + mPreview.count_cameraTakePicture);
		assertTrue(mPreview.count_cameraTakePicture==4);

	}

	/* Tests we can stamp custom text to photo.
	 */
	public void testCustomTextStamp() throws IOException {
		Log.d(TAG, "testCustomTextStamp");

		setToDefault();

		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(PreferenceKeys.getTextStampPreferenceKey(), "Test stamp!£$");
			editor.apply();
			updateForSettings();
		}

	    assertTrue(mPreview.count_cameraTakePicture==0);

	    View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
	    clickView(takePhotoButton);

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
	    }
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "photo count: " + mPreview.count_cameraTakePicture);
		assertTrue(mPreview.count_cameraTakePicture==1);

		// now test with auto-stabilise
		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean(PreferenceKeys.getAutoStabilisePreferenceKey(), true);
			editor.apply();
		}

	    clickView(takePhotoButton);

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
	    }
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "photo count: " + mPreview.count_cameraTakePicture);
		assertTrue(mPreview.count_cameraTakePicture==2);

	}

	/* Tests zoom.
	 */
	public void testZoom() {
		Log.d(TAG, "testZoom");
		setToDefault();

	    if( !mPreview.supportsZoom() ) {
			Log.d(TAG, "zoom not supported");
	    	return;
	    }

	    final ZoomControls zoomControls = (ZoomControls) mActivity.findViewById(net.sourceforge.opencamera.R.id.zoom);
		assertTrue(zoomControls.getVisibility() == View.INVISIBLE);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(PreferenceKeys.getShowZoomControlsPreferenceKey(), true);
		editor.apply();
		updateForSettings();

		assertTrue(zoomControls.getVisibility() == View.VISIBLE);
	    final SeekBar zoomSeekBar = (SeekBar) mActivity.findViewById(net.sourceforge.opencamera.R.id.zoom_seekbar);
		assertTrue(zoomSeekBar.getVisibility() == View.VISIBLE);
		int max_zoom = mPreview.getMaxZoom();
		assertTrue(zoomSeekBar.getMax() == max_zoom);
		Log.d(TAG, "zoomSeekBar progress = " + zoomSeekBar.getProgress());
		Log.d(TAG, "actual zoom = " + mPreview.getCameraController().getZoom());
		assertTrue(max_zoom-zoomSeekBar.getProgress() == mPreview.getCameraController().getZoom());

	    if( mPreview.supportsFocus() ) {
			assertTrue(!mPreview.hasFocusArea());
		    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
		    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);

			// touch to auto-focus with focus area
			TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
			assertTrue(mPreview.hasFocusArea());
		    assertTrue(mPreview.getCameraController().getFocusAreas() != null);
		    assertTrue(mPreview.getCameraController().getFocusAreas().size() == 1);
		    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
		    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);
	    }

	    int zoom = mPreview.getCameraController().getZoom();

	    // use buttons to zoom
		Log.d(TAG, "zoom in");
	    mActivity.zoomIn();
		this.getInstrumentation().waitForIdleSync();
	    Log.d(TAG, "compare actual zoom " + mPreview.getCameraController().getZoom() + " to zoom " + zoom);
	    assertTrue(mPreview.getCameraController().getZoom() == zoom+1);
		assertTrue(max_zoom-zoomSeekBar.getProgress() == mPreview.getCameraController().getZoom());
	    if( mPreview.supportsFocus() ) {
	    	// check that focus areas cleared
			assertTrue(!mPreview.hasFocusArea());
		    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
		    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);

			// touch to auto-focus with focus area
			TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
			assertTrue(mPreview.hasFocusArea());
		    assertTrue(mPreview.getCameraController().getFocusAreas() != null);
		    assertTrue(mPreview.getCameraController().getFocusAreas().size() == 1);
		    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
		    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);
	    }

		Log.d(TAG, "zoom out");
		mActivity.zoomOut();
		this.getInstrumentation().waitForIdleSync();
	    Log.d(TAG, "compare actual zoom " + mPreview.getCameraController().getZoom() + " to zoom " + zoom);
	    assertTrue(mPreview.getCameraController().getZoom() == zoom);
		assertTrue(max_zoom-zoomSeekBar.getProgress() == mPreview.getCameraController().getZoom());
	    if( mPreview.supportsFocus() ) {
	    	// check that focus areas cleared
			assertTrue(!mPreview.hasFocusArea());
		    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
		    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);

			// touch to auto-focus with focus area
			TouchUtils.clickView(MainActivityTest.this, mPreview.getView());
			assertTrue(mPreview.hasFocusArea());
		    assertTrue(mPreview.getCameraController().getFocusAreas() != null);
		    assertTrue(mPreview.getCameraController().getFocusAreas().size() == 1);
		    assertTrue(mPreview.getCameraController().getMeteringAreas() != null);
		    assertTrue(mPreview.getCameraController().getMeteringAreas().size() == 1);
	    }

	    // now test multitouch zoom
	    mPreview.scaleZoom(2.0f);
		this.getInstrumentation().waitForIdleSync();
	    Log.d(TAG, "compare actual zoom " + mPreview.getCameraController().getZoom() + " to zoom " + zoom);
	    assertTrue(mPreview.getCameraController().getZoom() > zoom);
		assertTrue(max_zoom-zoomSeekBar.getProgress() == mPreview.getCameraController().getZoom());

	    mPreview.scaleZoom(0.5f);
		this.getInstrumentation().waitForIdleSync();
	    Log.d(TAG, "compare actual zoom " + mPreview.getCameraController().getZoom() + " to zoom " + zoom);
	    assertTrue(mPreview.getCameraController().getZoom() == zoom);
		assertTrue(max_zoom-zoomSeekBar.getProgress() == mPreview.getCameraController().getZoom());

		// test to max/min
	    mPreview.scaleZoom(10000.0f);
		this.getInstrumentation().waitForIdleSync();
	    Log.d(TAG, "compare actual zoom " + mPreview.getCameraController().getZoom() + " to max_zoom " + max_zoom);
	    assertTrue(mPreview.getCameraController().getZoom() == max_zoom);
		assertTrue(max_zoom-zoomSeekBar.getProgress() == mPreview.getCameraController().getZoom());
		
	    mPreview.scaleZoom(1.0f/10000.0f);
		this.getInstrumentation().waitForIdleSync();
	    Log.d(TAG, "compare actual zoom " + mPreview.getCameraController().getZoom() + " to zero");
	    assertTrue(mPreview.getCameraController().getZoom() == 0);
		assertTrue(max_zoom-zoomSeekBar.getProgress() == mPreview.getCameraController().getZoom());

		// use seekbar to zoom
		Log.d(TAG, "zoom to max");
		Log.d(TAG, "progress was: " + zoomSeekBar.getProgress());
	    zoomSeekBar.setProgress(0);
		this.getInstrumentation().waitForIdleSync();
	    Log.d(TAG, "compare actual zoom " + mPreview.getCameraController().getZoom() + " to max_zoom " + max_zoom);
	    assertTrue(mPreview.getCameraController().getZoom() == max_zoom);
		assertTrue(max_zoom-zoomSeekBar.getProgress() == mPreview.getCameraController().getZoom());
	    if( mPreview.supportsFocus() ) {
	    	// check that focus areas cleared
			assertTrue(!mPreview.hasFocusArea());
		    assertTrue(mPreview.getCameraController().getFocusAreas() == null);
		    assertTrue(mPreview.getCameraController().getMeteringAreas() == null);
	    }
	}

	public void testZoomIdle() {
		Log.d(TAG, "testZoomIdle");
		setToDefault();

	    if( !mPreview.supportsZoom() ) {
			Log.d(TAG, "zoom not supported");
	    	return;
	    }

	    final SeekBar zoomSeekBar = (SeekBar) mActivity.findViewById(net.sourceforge.opencamera.R.id.zoom_seekbar);
		assertTrue(zoomSeekBar.getVisibility() == View.VISIBLE);
	    int max_zoom = mPreview.getMaxZoom();
	    zoomSeekBar.setProgress(0);
		this.getInstrumentation().waitForIdleSync();
	    Log.d(TAG, "compare actual zoom " + mPreview.getCameraController().getZoom() + " to zoom " + max_zoom);
	    assertTrue(mPreview.getCameraController().getZoom() == max_zoom);
		assertTrue(max_zoom-zoomSeekBar.getProgress() == mPreview.getCameraController().getZoom());

		pauseAndResume();
	    Log.d(TAG, "after pause and resume: compare actual zoom " + mPreview.getCameraController().getZoom() + " to zoom " + max_zoom);
	    assertTrue(mPreview.getCameraController().getZoom() == max_zoom);
		assertTrue(max_zoom-zoomSeekBar.getProgress() == mPreview.getCameraController().getZoom());
	}

	public void testZoomSwitchCamera() {
		Log.d(TAG, "testZoomSwitchCamera");
		setToDefault();

	    if( !mPreview.supportsZoom() ) {
			Log.d(TAG, "zoom not supported");
	    	return;
	    }
	    else if( mPreview.getCameraControllerManager().getNumberOfCameras() <= 1 ) {
			return;
		}

	    final SeekBar zoomSeekBar = (SeekBar) mActivity.findViewById(net.sourceforge.opencamera.R.id.zoom_seekbar);
		assertTrue(zoomSeekBar.getVisibility() == View.VISIBLE);
	    int max_zoom = mPreview.getMaxZoom();
	    zoomSeekBar.setProgress(0);
		this.getInstrumentation().waitForIdleSync();
	    Log.d(TAG, "compare actual zoom " + mPreview.getCameraController().getZoom() + " to zoom " + max_zoom);
	    assertTrue(mPreview.getCameraController().getZoom() == max_zoom);
		assertTrue(max_zoom-zoomSeekBar.getProgress() == mPreview.getCameraController().getZoom());

	    int cameraId = mPreview.getCameraId();
	    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    clickView(switchCameraButton);
		int new_cameraId = mPreview.getCameraId();
		assertTrue(cameraId != new_cameraId);

	    max_zoom = mPreview.getMaxZoom();
	    Log.d(TAG, "after pause and resume: compare actual zoom " + mPreview.getCameraController().getZoom() + " to zoom " + max_zoom);
	    assertTrue(mPreview.getCameraController().getZoom() == max_zoom);
		assertTrue(max_zoom-zoomSeekBar.getProgress() == mPreview.getCameraController().getZoom());
	}

	/** Switch to front camera, pause and resume, check still on the front camera.
	 */
	public void testSwitchCameraIdle() {
		Log.d(TAG, "testSwitchCameraIdle");
		setToDefault();

		if( mPreview.getCameraControllerManager().getNumberOfCameras() <= 1 ) {
			return;
		}

	    int cameraId = mPreview.getCameraId();
	    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    clickView(switchCameraButton);
		int new_cameraId = mPreview.getCameraId();
		assertTrue(cameraId != new_cameraId);

		pauseAndResume();

	    int new2_cameraId = mPreview.getCameraId();
		assertTrue(new2_cameraId == new_cameraId);

	}

	/* Tests going to gallery.
	 */
	public void testGallery() {
		Log.d(TAG, "testGallery");
		setToDefault();

	    View galleryButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.gallery);
	    clickView(galleryButton);
	    
	}

	/* Tests going to settings.
	 */
	public void testSettings() {
		Log.d(TAG, "testSettings");
		setToDefault();

	    View settingsButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.settings);
	    clickView(settingsButton);
	    
	}

	private void subTestCreateSaveFolder(boolean use_saf, String save_folder, boolean delete_folder) {
		setToDefault();

		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			if( use_saf ) {
				editor.putBoolean(PreferenceKeys.getUsingSAFPreferenceKey(), true);
				editor.putString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), save_folder);
			}
			else {
				editor.putString(PreferenceKeys.getSaveLocationPreferenceKey(), save_folder);
			}
			editor.apply();
			updateForSettings();
			if( use_saf ) {
				// need to call this directly, as we don't call mActivity.onActivityResult
				mActivity.updateFolderHistorySAF(save_folder);
			}
		}
		
		SaveLocationHistory save_location_history = use_saf ? mActivity.getSaveLocationHistorySAF() : mActivity.getSaveLocationHistory();
		assertTrue(save_location_history.size() > 0);
		assertTrue(save_location_history.contains(save_folder));
		assertTrue(save_location_history.get( save_location_history.size()-1 ).equals(save_folder));

		File folder = mActivity.getImageFolder();
		if( folder.exists() && delete_folder ) {
			assertTrue(folder.isDirectory());
			// delete folder - need to delete contents first
			if( folder.isDirectory() ) {
		        String [] children = folder.list();
				for(String child : children) {
		            File file = new File(folder, child);
		            file.delete();
		        	MediaScannerConnection.scanFile(mActivity, new String[] { file.getAbsolutePath() }, null, null);
		        }
			}
			folder.delete();
		}
		int n_old_files = 0;
		if( folder.exists() ) {
			n_old_files = folder.listFiles().length;
		}
		Log.d(TAG, "n_old_files: " + n_old_files);

	    View takePhotoButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.take_photo);
		Log.d(TAG, "about to click take photo");
	    clickView(takePhotoButton);
		Log.d(TAG, "done clicking take photo");

		Log.d(TAG, "wait until finished taking photo");
	    while( mPreview.isTakingPhoto() ) {
	    }
		Log.d(TAG, "done taking photo");
		this.getInstrumentation().waitForIdleSync();
		Log.d(TAG, "after idle sync");
		assertTrue(mPreview.count_cameraTakePicture==1);

		mActivity.waitUntilImageQueueEmpty();
		
		assertTrue( folder.exists() );
		int n_new_files = folder.listFiles().length;
		Log.d(TAG, "n_new_files: " + n_new_files);
		assertTrue(n_new_files == n_old_files+1);

		// change back to default, so as to not be annoying
		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			if( use_saf ) {
				editor.putString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), "content://com.android.externalstorage.documents/tree/primary%3ADCIM%2FOpenCamera");
			}
			else {
				editor.putString(PreferenceKeys.getSaveLocationPreferenceKey(), "OpenCamera");
			}
			editor.apply();
		}
	}

	/** Tests taking a photo with a new save folder.
	 */
	public void testCreateSaveFolder1() {
		Log.d(TAG, "testCreateSaveFolder1");
		subTestCreateSaveFolder(false, "OpenCameraTest", true);
	}

	/** Tests taking a photo with a new save folder.
	 */
	public void testCreateSaveFolder2() {
		Log.d(TAG, "testCreateSaveFolder2");
		subTestCreateSaveFolder(false, "OpenCameraTest/", true);
	}

	/** Tests taking a photo with a new save folder.
	 */
	public void testCreateSaveFolder3() {
		Log.d(TAG, "testCreateSaveFolder3");
		subTestCreateSaveFolder(false, "OpenCameraTest_a/OpenCameraTest_b", true);
	}

	/** Tests taking a photo with a new save folder.
	 */
	@SuppressLint("SdCardPath")
	public void testCreateSaveFolder4() {
		Log.d(TAG, "testCreateSaveFolder4");
		subTestCreateSaveFolder(false, "/sdcard/Pictures/OpenCameraTest", true);
	}

	/** Tests taking a photo with a new save folder.
	 */
	public void testCreateSaveFolderUnicode() {
		Log.d(TAG, "testCreateSaveFolderUnicode");
		subTestCreateSaveFolder(false, "éúíóá!£$%^&()", true);
	}

	/** Tests taking a photo with a new save folder.
	 */
	public void testCreateSaveFolderEmpty() {
		Log.d(TAG, "testCreateSaveFolderEmpty");
		subTestCreateSaveFolder(false, "", false);
	}

	/** Tests taking a photo with a new save folder.
	 *  If this test fails, make sure we've manually selected that folder (as permission can't be given through the test framework).
	 */
	public void testCreateSaveFolderSAF() {
		Log.d(TAG, "testCreateSaveFolderSAF");

		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ) {
			Log.d(TAG, "SAF requires Android Lollipop or better");
			return;
		}

		subTestCreateSaveFolder(true, "content://com.android.externalstorage.documents/tree/primary%3ADCIM", true);
	}

	/** Tests launching the folder chooser on a new folder.
	 */
	public void testFolderChooserNew() throws InterruptedException {
		setToDefault();

		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(PreferenceKeys.getSaveLocationPreferenceKey(), "OpenCameraTest");
			editor.apply();
			updateForSettings();
		}

		File folder = mActivity.getImageFolder();
		if( folder.exists() ) {
			assertTrue(folder.isDirectory());
			// delete folder - need to delete contents first
			if( folder.isDirectory() ) {
		        String [] children = folder.list();
				for(String child : children) {
					File file = new File(folder, child);
		            file.delete();
		        	MediaScannerConnection.scanFile(mActivity, new String[] { file.getAbsolutePath() }, null, null);
		        }
			}
			folder.delete();
		}

		FolderChooserDialog fragment = new FolderChooserDialog();
		fragment.show(mActivity.getFragmentManager(), "FOLDER_FRAGMENT");
		Thread.sleep(1000); // wait until folderchooser started up
		Log.d(TAG, "started folderchooser");
		assertTrue(fragment.getCurrentFolder() != null);
		assertTrue(fragment.getCurrentFolder().equals(folder));
		assertTrue(folder.exists());
	}

	/** Tests launching the folder chooser on a folder we don't have access to.
	 * (Shouldn't be possible to get into this state, but just in case.)
	 */
	public void testFolderChooserInvalid() throws InterruptedException {
		setToDefault();

		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(PreferenceKeys.getSaveLocationPreferenceKey(), "/OpenCameraTest");
			editor.apply();
			updateForSettings();
		}

		FolderChooserDialog fragment = new FolderChooserDialog();
		fragment.show(mActivity.getFragmentManager(), "FOLDER_FRAGMENT");
		Thread.sleep(1000); // wait until folderchooser started up
		Log.d(TAG, "started folderchooser");
		assertTrue(fragment.getCurrentFolder() != null);
		Log.d(TAG, "current folder: " + fragment.getCurrentFolder());
		assertTrue(fragment.getCurrentFolder().exists());
	}

	private void subTestSaveFolderHistory(final boolean use_saf) {
		// clearFolderHistory has code that must be run on UI thread
		mActivity.runOnUiThread(new Runnable() {
			public void run() {
				Log.d(TAG, "clearFolderHistory");
				if( use_saf )
					mActivity.clearFolderHistorySAF();
				else
					mActivity.clearFolderHistory();
			}
		});
		// need to wait for UI code to finish before leaving
		this.getInstrumentation().waitForIdleSync();
		SaveLocationHistory save_location_history = use_saf ? mActivity.getSaveLocationHistorySAF() : mActivity.getSaveLocationHistory();
		Log.d(TAG, "save_location_history size: " + save_location_history.size());
		assertTrue(save_location_history.size() == 1);
		String current_folder;
		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			current_folder = use_saf ? settings.getString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), "") : settings.getString(PreferenceKeys.getSaveLocationPreferenceKey(), "OpenCamera");
			Log.d(TAG, "current_folder: " + current_folder);
			Log.d(TAG, "save_location_history entry: " + save_location_history.get(0));
			assertTrue(save_location_history.get(0).equals(current_folder));
		}
		
		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(use_saf ? PreferenceKeys.getSaveLocationSAFPreferenceKey() : PreferenceKeys.getSaveLocationPreferenceKey(), "new_folder_history_entry");
			editor.apply();
			updateForSettings();
			if( use_saf ) {
				// need to call this directly, as we don't call mActivity.onActivityResult
				mActivity.updateFolderHistorySAF("new_folder_history_entry");
			}
		}
		save_location_history = use_saf ? mActivity.getSaveLocationHistorySAF() : mActivity.getSaveLocationHistory();
		Log.d(TAG, "save_location_history size: " + save_location_history.size());
		for(int i=0;i<save_location_history.size();i++) {
			Log.d(TAG, save_location_history.get(i));
		}
		assertTrue(save_location_history.size() == 2);
		assertTrue(save_location_history.get(0).equals(current_folder));
		assertTrue(save_location_history.get(1).equals("new_folder_history_entry"));
		
		restart();

		save_location_history = use_saf ? mActivity.getSaveLocationHistorySAF() : mActivity.getSaveLocationHistory();
		Log.d(TAG, "save_location_history size: " + save_location_history.size());
		for(int i=0;i<save_location_history.size();i++) {
			Log.d(TAG, save_location_history.get(i));
		}
		assertTrue(save_location_history.size() == 2);
		Log.d(TAG, "current_folder: " + current_folder);
		assertTrue(save_location_history.get(0).equals(current_folder));
		assertTrue(save_location_history.get(1).equals("new_folder_history_entry"));

		{
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(use_saf ? PreferenceKeys.getSaveLocationSAFPreferenceKey() : PreferenceKeys.getSaveLocationPreferenceKey(), current_folder);
			editor.apply();
			// now (for non-SAF) call testUsedFolderPicker() instead of updateForSettings(), to simulate using the recent folder picker
			// clearFolderHistory has code that must be run on UI thread
			final String current_folder_f = current_folder;
			mActivity.runOnUiThread(new Runnable() {
				public void run() {
					if( use_saf ) {
						// need to call this directly, as we don't call mActivity.onActivityResult
						mActivity.updateFolderHistorySAF(current_folder_f);
					}
					else {
						mActivity.usedFolderPicker();
					}
				}
			});
			// need to wait for UI code to finish before leaving
			this.getInstrumentation().waitForIdleSync();
		}
		save_location_history = use_saf ? mActivity.getSaveLocationHistorySAF() : mActivity.getSaveLocationHistory();
		assertTrue(save_location_history.size() == 2);
		assertTrue(save_location_history.get(0).equals("new_folder_history_entry"));
		assertTrue(save_location_history.get(1).equals(current_folder));

		// clearFolderHistory has code that must be run on UI thread
		mActivity.runOnUiThread(new Runnable() {
			public void run() {
				if( use_saf )
					mActivity.clearFolderHistorySAF();
				else
					mActivity.clearFolderHistory();
			}
		});
		// need to wait for UI code to finish before leaving
		this.getInstrumentation().waitForIdleSync();
		save_location_history = use_saf ? mActivity.getSaveLocationHistorySAF() : mActivity.getSaveLocationHistory();
		assertTrue(save_location_history.size() == 1);
		assertTrue(save_location_history.get(0).equals(current_folder));
	}

	public void testSaveFolderHistory() {
		setToDefault();
		
		subTestSaveFolderHistory(false);
	}

	public void testSaveFolderHistorySAF() {
		setToDefault();
		
		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ) {
			Log.d(TAG, "SAF requires Android Lollipop or better");
			return;
		}

		{
			String save_folder = "content://com.android.externalstorage.documents/tree/primary%3ADCIM/OpenCamera";
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean(PreferenceKeys.getUsingSAFPreferenceKey(), true);
			editor.putString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), save_folder);
			editor.apply();
			updateForSettings();
			// need to call this directly, as we don't call mActivity.onActivityResult
			mActivity.updateFolderHistorySAF(save_folder);
		}

		subTestSaveFolderHistory(true);
	}

	public void testPreviewRotation() {
		Log.d(TAG, "testPreviewRotation");

		setToDefault();
		
		int display_orientation = mPreview.getDisplayRotation();
		Log.d(TAG, "display_orientation = " + display_orientation);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getRotatePreviewPreferenceKey(), "180");
		editor.apply();
		updateForSettings();

		int new_display_orientation = mPreview.getDisplayRotation();
		Log.d(TAG, "new_display_orientation = " + new_display_orientation);
		assertTrue( new_display_orientation == ((display_orientation + 2) % 4) );
	}

	public void testSceneMode() {
		Log.d(TAG, "testSceneMode");

		setToDefault();
		
	    List<String> scene_modes = mPreview.getSupportedSceneModes();
	    if( scene_modes == null ) {
	    	return;
	    }
		Log.d(TAG, "scene mode: " + mPreview.getCameraController().getSceneMode());
	    assertTrue( mPreview.getCameraController().getSceneMode() == null || mPreview.getCameraController().getSceneMode().equals(mPreview.getCameraController().getDefaultSceneMode()) );

	    String scene_mode = null;
	    // find a scene mode that isn't default
	    for(String this_scene_mode : scene_modes) {
	    	if( !this_scene_mode.equals(mPreview.getCameraController().getDefaultSceneMode()) ) {
	    		scene_mode = this_scene_mode;
	    		break;
	    	}
	    }
	    if( scene_mode == null ) {
	    	return;
	    }
		Log.d(TAG, "change to scene_mode: " + scene_mode);
	    
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getSceneModePreferenceKey(), scene_mode);
		editor.apply();
		updateForSettings();

		String new_scene_mode = mPreview.getCameraController().getSceneMode();
		Log.d(TAG, "scene_mode is now: " + new_scene_mode);
	    assertTrue( new_scene_mode.equals(scene_mode) );
	}

	public void testColorEffect() {
		Log.d(TAG, "testColorEffect");

		setToDefault();
		
	    List<String> color_effects = mPreview.getSupportedColorEffects();
	    if( color_effects == null ) {
	    	return;
	    }
		Log.d(TAG, "color effect: " + mPreview.getCameraController().getColorEffect());
	    assertTrue( mPreview.getCameraController().getColorEffect() == null || mPreview.getCameraController().getColorEffect().equals(mPreview.getCameraController().getDefaultColorEffect()) );

	    String color_effect = null;
	    // find a color effect that isn't default
	    for(String this_color_effect : color_effects) {
	    	if( !this_color_effect.equals(mPreview.getCameraController().getDefaultColorEffect()) ) {
	    		color_effect = this_color_effect;
	    		break;
	    	}
	    }
	    if( color_effect == null ) {
	    	return;
	    }
		Log.d(TAG, "change to color_effect: " + color_effect);
	    
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getColorEffectPreferenceKey(), color_effect);
		editor.apply();
		updateForSettings();

		String new_color_effect = mPreview.getCameraController().getColorEffect();
		Log.d(TAG, "color_effect is now: " + new_color_effect);
	    assertTrue( new_color_effect.equals(color_effect) );
	}

	public void testWhiteBalance() {
		Log.d(TAG, "testWhiteBalance");

		setToDefault();
		
	    List<String> white_balances = mPreview.getSupportedWhiteBalances();
	    if( white_balances == null ) {
	    	return;
	    }
		Log.d(TAG, "white balance: " + mPreview.getCameraController().getWhiteBalance());
	    assertTrue( mPreview.getCameraController().getWhiteBalance() == null || mPreview.getCameraController().getWhiteBalance().equals(mPreview.getCameraController().getDefaultWhiteBalance()) );

	    String white_balance = null;
	    // find a white balance that isn't default
	    for(String this_white_balances : white_balances) {
	    	if( !this_white_balances.equals(mPreview.getCameraController().getDefaultWhiteBalance()) ) {
	    		white_balance = this_white_balances;
	    		break;
	    	}
	    }
	    if( white_balance == null ) {
	    	return;
	    }
		Log.d(TAG, "change to white_balance: " + white_balance);
	    
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getWhiteBalancePreferenceKey(), white_balance);
		editor.apply();
		updateForSettings();

		String new_white_balance = mPreview.getCameraController().getWhiteBalance();
		Log.d(TAG, "white_balance is now: " + new_white_balance);
	    assertTrue( new_white_balance.equals(white_balance) );
	}

	public void testImageQuality() {
		Log.d(TAG, "testImageQuality");

		setToDefault();
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getQualityPreferenceKey(), "100");
		editor.apply();
		updateForSettings();

		int quality = mPreview.getCameraController().getJpegQuality();
		Log.d(TAG, "quality is: " + quality);
	    assertTrue( quality == 100 );
	}

	/** Tests that changing resolutions doesn't close the popup.
	 */
	public void testSwitchResolution() throws InterruptedException {
		Log.d(TAG, "testSwitchResolution");

		View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);
        CameraController.Size old_picture_size = mPreview.getCameraController().getPictureSize();

        // open popup
        assertFalse( mActivity.popupIsOpen() );
	    clickView(popupButton);
	    while( !mActivity.popupIsOpen() ) {
	    }

	    TextView photoResolutionButton = (TextView)mActivity.getPopupButton("PHOTO_RESOLUTIONS");
	    assertTrue(photoResolutionButton != null);
		String exp_size_string = old_picture_size.width + " x " + old_picture_size.height + " " + Preview.getMPString(old_picture_size.width, old_picture_size.height);
		Log.d(TAG, "size string: " + photoResolutionButton.getText());
	    assertTrue( photoResolutionButton.getText().equals(exp_size_string) );

	    // change photo resolution
	    View photoResolutionNextButton = mActivity.getPopupButton("PHOTO_RESOLUTIONS_NEXT");
	    assertTrue(photoResolutionNextButton != null);
		this.getInstrumentation().waitForIdleSync();
	    clickView(photoResolutionNextButton);

	    // check
	    Thread.sleep(2000);
	    CameraController.Size new_picture_size = mPreview.getCameraController().getPictureSize();
		Log.d(TAG, "old picture size: " + old_picture_size.width + " x " + old_picture_size.height);
		Log.d(TAG, "old new_picture_size size: " + new_picture_size.width + " x " + new_picture_size.height);
	    assertTrue( !new_picture_size.equals(old_picture_size) );
		assertTrue( mActivity.popupIsOpen() );

		exp_size_string = new_picture_size.width + " x " + new_picture_size.height + " " + Preview.getMPString(new_picture_size.width, new_picture_size.height);
		Log.d(TAG, "size string: " + photoResolutionButton.getText());
	    assertTrue( photoResolutionButton.getText().equals(exp_size_string) );

	    TextView videoResolutionButton = (TextView)mActivity.getPopupButton("VIDEO_RESOLUTIONS");
	    assertTrue(videoResolutionButton != null);
	    CharSequence oldVideoResolutionString = videoResolutionButton.getText();

	    // change video resolution
	    View videoResolutionNextButton = mActivity.getPopupButton("VIDEO_RESOLUTIONS_NEXT");
	    assertTrue(videoResolutionNextButton != null);
	    clickView(videoResolutionNextButton);

	    // check
	    Thread.sleep(500);
		assertTrue( mActivity.popupIsOpen() );
	    assertTrue( !videoResolutionButton.getText().equals(oldVideoResolutionString) );

	}

	/* Test for failing to open camera.
	 */
	public void testFailOpenCamera() throws InterruptedException {
		Log.d(TAG, "testFailOpenCamera");

		setToDefault();

		assertTrue(mPreview.getCameraControllerManager() != null);
		assertTrue(mPreview.getCameraController() != null);
		mPreview.test_fail_open_camera = true;

		// can't test on startup, as camera is created when we create activity, so instead test by switching camera
		if( mPreview.getCameraControllerManager().getNumberOfCameras() > 1 ) {
			Log.d(TAG, "switch camera");
		    View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
		    clickView(switchCameraButton);
			assertTrue(mPreview.getCameraControllerManager() != null);
			assertTrue(mPreview.getCameraController() == null);
			this.getInstrumentation().waitForIdleSync();
		
			assertFalse( mActivity.popupIsOpen() );
		    View popupButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.popup);
			Log.d(TAG, "about to click popup");
		    clickView(popupButton);
			Log.d(TAG, "done clicking popup");
			Thread.sleep(500);
			// if camera isn't opened, popup shouldn't open
			assertFalse( mActivity.popupIsOpen() );

		    View settingsButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.settings);
			Log.d(TAG, "about to click settings");
		    clickView(settingsButton);
			Log.d(TAG, "done clicking settings");
			this.getInstrumentation().waitForIdleSync();
			Log.d(TAG, "after idle sync");
		}

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getVolumeKeysPreferenceKey(), "volume_exposure");
		editor.apply();
		this.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_UP);
	}

	public void testTakePhotoDRO() throws InterruptedException {
		Log.d(TAG, "testTakePhotoDRO");
		if( !mActivity.supportsDRO() ) {
			return;
		}

		setToDefault();

		assertTrue( mActivity.getApplicationInterface().getImageQualityPref() == 90 );

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getPhotoModePreferenceKey(), "preference_photo_mode_dro");
		editor.apply();
		updateForSettings();

		assertTrue( mActivity.getApplicationInterface().getImageQualityPref() == 100 );

		subTestTakePhoto(false, false, true, true, false, false, false, false);

		assertTrue( mActivity.getApplicationInterface().getImageQualityPref() == 100 );

		editor.putString(PreferenceKeys.getPhotoModePreferenceKey(), "preference_photo_mode_std");
		editor.apply();
		updateForSettings();

		assertTrue( mActivity.getApplicationInterface().getImageQualityPref() == 90 );
	}

	public void testTakePhotoDROPhotoStamp() throws InterruptedException {
		Log.d(TAG, "testTakePhotoDROPhotoStamp");
		if( !mActivity.supportsDRO() ) {
			return;
		}

		setToDefault();

		assertTrue( mActivity.getApplicationInterface().getImageQualityPref() == 90 );

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getPhotoModePreferenceKey(), "preference_photo_mode_dro");
		editor.putString(PreferenceKeys.getStampPreferenceKey(), "preference_stamp_yes");
		editor.apply();
		updateForSettings();

		assertTrue( mActivity.getApplicationInterface().getImageQualityPref() == 100 );

		subTestTakePhoto(false, false, true, true, false, false, false, false);

		assertTrue( mActivity.getApplicationInterface().getImageQualityPref() == 100 );

		editor.putString(PreferenceKeys.getPhotoModePreferenceKey(), "preference_photo_mode_std");
		editor.apply();
		updateForSettings();

		assertTrue( mActivity.getApplicationInterface().getImageQualityPref() == 90 );
	}

	public void testTakePhotoHDR() throws InterruptedException {
		Log.d(TAG, "testTakePhotoHDR");
		if( !mActivity.supportsHDR() ) {
			return;
		}

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getPhotoModePreferenceKey(), "preference_photo_mode_hdr");
		editor.apply();
		updateForSettings();

		subTestTakePhoto(false, false, true, true, false, false, false, false);
		Log.d(TAG, "test_capture_results: " + mPreview.getCameraController().test_capture_results);
		assertTrue(mPreview.getCameraController().test_capture_results == 1);
	}

	public void testTakePhotoHDRSaveExpo() throws InterruptedException {
		Log.d(TAG, "testTakePhotoHDRSaveExpo");
		if( !mActivity.supportsHDR() ) {
			return;
		}

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getPhotoModePreferenceKey(), "preference_photo_mode_hdr");
		editor.putBoolean(PreferenceKeys.getHDRSaveExpoPreferenceKey(), true);
		editor.apply();
		updateForSettings();

		subTestTakePhoto(false, false, true, true, false, false, false, false);
		Log.d(TAG, "test_capture_results: " + mPreview.getCameraController().test_capture_results);
		assertTrue(mPreview.getCameraController().test_capture_results == 1);
	}

	public void testTakePhotoHDRFrontCamera() throws InterruptedException {
		Log.d(TAG, "testTakePhotoHDRFrontCamera");
		if( !mActivity.supportsHDR() ) {
			return;
		}
		if( mPreview.getCameraControllerManager().getNumberOfCameras() <= 1 ) {
			return;
		}
		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getPhotoModePreferenceKey(), "preference_photo_mode_hdr");
		editor.apply();
		updateForSettings();

		int cameraId = mPreview.getCameraId();

		View switchCameraButton = mActivity.findViewById(net.sourceforge.opencamera.R.id.switch_camera);
	    clickView(switchCameraButton);

	    int new_cameraId = mPreview.getCameraId();

		Log.d(TAG, "cameraId: " + cameraId);
		Log.d(TAG, "new_cameraId: " + new_cameraId);

		assertTrue(cameraId != new_cameraId);

		subTestTakePhoto(false, false, true, true, false, false, false, false);
		Log.d(TAG, "test_capture_results: " + mPreview.getCameraController().test_capture_results);
		assertTrue(mPreview.getCameraController().test_capture_results == 1);
	}

	public void testTakePhotoHDRAutoStabilise() throws InterruptedException {
		Log.d(TAG, "testTakePhotoHDRAutoStabilise");
		if( !mActivity.supportsHDR() ) {
			return;
		}

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getPhotoModePreferenceKey(), "preference_photo_mode_hdr");
		editor.putBoolean(PreferenceKeys.getAutoStabilisePreferenceKey(), true);
		editor.apply();
		updateForSettings();

		subTestTakePhoto(false, false, true, true, false, false, false, false);
		Log.d(TAG, "test_capture_results: " + mPreview.getCameraController().test_capture_results);
		assertTrue(mPreview.getCameraController().test_capture_results == 1);
	}

	public void testTakePhotoHDRPhotoStamp() throws InterruptedException {
		Log.d(TAG, "testTakePhotoHDRPhotoStamp");
		if( !mActivity.supportsHDR() ) {
			return;
		}

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getPhotoModePreferenceKey(), "preference_photo_mode_hdr");
		editor.putString(PreferenceKeys.getStampPreferenceKey(), "preference_stamp_yes");
		editor.apply();
		updateForSettings();

		subTestTakePhoto(false, false, true, true, false, false, false, false);
		Log.d(TAG, "test_capture_results: " + mPreview.getCameraController().test_capture_results);
		assertTrue(mPreview.getCameraController().test_capture_results == 1);
	}

	/** Tests expo bracketing with default values.
     */
	public void testTakePhotoExpo() throws InterruptedException {
		Log.d(TAG, "testTakePhotoExpo");
		if( !mActivity.supportsExpoBracketing() ) {
			return;
		}

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getPhotoModePreferenceKey(), "preference_photo_mode_expo_bracketing");
		editor.apply();
		updateForSettings();

		subTestTakePhoto(false, false, true, true, false, false, false, false);
		Log.d(TAG, "test_capture_results: " + mPreview.getCameraController().test_capture_results);
		assertTrue(mPreview.getCameraController().test_capture_results == 1);
	}

	/** Tests expo bracketing with 5 images, 1 stop.
	 *  Note this test [usually] fails on OnePlus 3T as onImageAvailable is only called 4 times, we never receive the 5th image.
	 */
	public void testTakePhotoExpo5() throws InterruptedException {
		Log.d(TAG, "testTakePhotoExpo5");
		if( !mActivity.supportsExpoBracketing() ) {
			return;
		}

		setToDefault();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PreferenceKeys.getPhotoModePreferenceKey(), "preference_photo_mode_expo_bracketing");
		editor.putString(PreferenceKeys.getExpoBracketingNImagesPreferenceKey(), "5");
		editor.putString(PreferenceKeys.getExpoBracketingStopsPreferenceKey(), "1");
		editor.apply();
		updateForSettings();

		subTestTakePhoto(false, false, true, true, false, false, false, false);
		Log.d(TAG, "test_capture_results: " + mPreview.getCameraController().test_capture_results);
		assertTrue(mPreview.getCameraController().test_capture_results == 1);
	}

	/*private Bitmap getBitmapFromAssets(String filename) throws IOException {
		Log.d(TAG, "getBitmapFromAssets: " + filename);
		AssetManager assetManager = getInstrumentation().getContext().getResources().getAssets();
	    InputStream is = assetManager.open(filename);
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inMutable = true;
	    Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
	    is.close();
		Log.d(TAG, "    done: " + bitmap);
	    return bitmap;
    }*/

	private Bitmap getBitmapFromFile(String filename) throws FileNotFoundException {
		Log.d(TAG, "getBitmapFromFile: " + filename);
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inMutable = true;
		Bitmap bitmap = BitmapFactory.decodeFile(filename, options);
		if( bitmap == null )
			throw new FileNotFoundException();
		Log.d(TAG, "    done: " + bitmap);
	    return bitmap;
    }

	/* Tests restarting a large number of times - can be useful for testing for memory/resource leaks.
	 */
	public void testRestart() {
		Log.d(TAG, "testRestart");
		setToDefault();

		final int n_restarts = 150;
		for(int i=0;i<n_restarts;i++) {
			Log.d(TAG, "restart: " + i + " / " + n_restarts);
			restart();
		}
	}

	public void testGPSString() {
		Log.d(TAG, "testGPSString");
		setToDefault();

		Location location1 = new Location("");
		location1.setLatitude(0.0);
		location1.setLongitude(0.0);
		assertEquals(mActivity.getTextFormatter().getGPSString("preference_stamp_gpsformat_none", true, location1, true, Math.toRadians(180)), "");
		assertEquals(mActivity.getTextFormatter().getGPSString("preference_stamp_gpsformat_default", true, location1, true, Math.toRadians(180)), "0, 0, 180°");
		assertEquals(mActivity.getTextFormatter().getGPSString("preference_stamp_gpsformat_dms", true, location1, true, Math.toRadians(180)), "0°0'0\", 0°0'0\", 180°");
		assertEquals(mActivity.getTextFormatter().getGPSString("preference_stamp_gpsformat_default", true, location1, false, Math.toRadians(180)), "0, 0");
		assertEquals(mActivity.getTextFormatter().getGPSString("preference_stamp_gpsformat_dms", true, location1, false, Math.toRadians(180)), "0°0'0\", 0°0'0\"");
		assertEquals(mActivity.getTextFormatter().getGPSString("preference_stamp_gpsformat_default", false, null, true, Math.toRadians(180)), "180°");
		assertEquals(mActivity.getTextFormatter().getGPSString("preference_stamp_gpsformat_dms", false, null, true, Math.toRadians(180)), "180°");

		Location location2 = new Location("");
		location2.setLatitude(-29.3);
		location2.setLongitude(47.6173);
		location2.setAltitude(106.5);
		assertEquals(mActivity.getTextFormatter().getGPSString("preference_stamp_gpsformat_none", true, location2, true, Math.toRadians(74)), "");
		assertEquals(mActivity.getTextFormatter().getGPSString("preference_stamp_gpsformat_default", true, location2, true, Math.toRadians(74)), "-29.3, 47.6173, 106.5m, 74°");
		assertEquals(mActivity.getTextFormatter().getGPSString("preference_stamp_gpsformat_dms", true, location2, true, Math.toRadians(74)), "-29°18'0\", 47°37'2\", 106.5m, 74°");
		assertEquals(mActivity.getTextFormatter().getGPSString("preference_stamp_gpsformat_default", true, location2, false, Math.toRadians(74)), "-29.3, 47.6173, 106.5m");
		assertEquals(mActivity.getTextFormatter().getGPSString("preference_stamp_gpsformat_dms", true, location2, false, Math.toRadians(74)), "-29°18'0\", 47°37'2\", 106.5m");
		assertEquals(mActivity.getTextFormatter().getGPSString("preference_stamp_gpsformat_default", false, null, true, Math.toRadians(74)), "74°");
		assertEquals(mActivity.getTextFormatter().getGPSString("preference_stamp_gpsformat_dms", false, null, true, Math.toRadians(74)), "74°");
	}

	/** The following testHDRX tests test the HDR algorithm on a given set of input images.
	 *  By testing on a fixed sample, this makes it easier to finetune the HDR algorithm for quality and performance.
	 *  To use these tests, the testdata/ subfolder should be manually copied to the test device in the DCIM/testOpenCamera/
	 *  folder (so you have DCIM/testOpenCamera/testdata/). We don't use assets/ as we'd end up with huge APK sizes which takes
	 *  time to transfer to the device everytime we run the tests.
	 */
	private void subTestHDR(List<Bitmap> inputs, String output_name, boolean test_dro) throws IOException, InterruptedException {
		Log.d(TAG, "subTestHDR");

		Thread.sleep(1000); // wait for camera to open

		Bitmap dro_bitmap_in = null;
		if( test_dro ) {
			// save copy of input bitmap to also test DRO (since the HDR routine will free the inputs)
			//dro_bitmap_in = inputs.get(0);
			dro_bitmap_in = inputs.get(1);
			dro_bitmap_in = dro_bitmap_in.copy(dro_bitmap_in.getConfig(), true);
		}

    	long time_s = System.currentTimeMillis();
		mActivity.getApplicationInterface().getHDRProcessor().processHDR(inputs, true, null, true);
		Log.d(TAG, "HDR time: " + (System.currentTimeMillis() - time_s));
		
		File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/" + output_name);
		OutputStream outputStream = new FileOutputStream(file);
		inputs.get(0).compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        outputStream.close();
        mActivity.getStorageUtils().broadcastFile(file, true, false, true);
		inputs.get(0).recycle();
		inputs.clear();

		if( test_dro ) {
			inputs.add(dro_bitmap_in);
			time_s = System.currentTimeMillis();
			mActivity.getApplicationInterface().getHDRProcessor().processHDR(inputs, true, null, true);
			Log.d(TAG, "DRO time: " + (System.currentTimeMillis() - time_s));

			file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/dro" + output_name);
			outputStream = new FileOutputStream(file);
			inputs.get(0).compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
			outputStream.close();
			mActivity.getStorageUtils().broadcastFile(file, true, false, true);
			inputs.get(0).recycle();
			inputs.clear();
		}
		Thread.sleep(500);
	}

	/** Checks that the HDR offsets used for auto-alignment are as expected.
     */
	private void checkHDROffsets(int [] exp_offsets_x, int [] exp_offsets_y) {
		int [] offsets_x = mActivity.getApplicationInterface().getHDRProcessor().offsets_x;
		int [] offsets_y = mActivity.getApplicationInterface().getHDRProcessor().offsets_y;
		for(int i=0;i<3;i++) {
			Log.d(TAG, "offsets " + i + " ( " + offsets_x[i] + " , " + offsets_y[i] + " ), expected ( " + exp_offsets_x[i] + " , " + exp_offsets_y[i] + ")");
			assertTrue( offsets_x[i] == exp_offsets_x[i] );
			assertTrue( offsets_y[i] == exp_offsets_y[i] );
		}
	}
	
	final private String hdr_images_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/testOpenCamera/testdata/hdrsamples/";

	/** Tests HDR algorithm on test samples "saintpaul".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR1() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR1");

		setToDefault();

		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "saintpaul/input2.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "saintpaul/input3.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "saintpaul/input4.jpg") );

		subTestHDR(inputs, "testHDR1_output.jpg", false);

		int [] exp_offsets_x = {0, 0, 0};
		int [] exp_offsets_y = {0, 0, 0};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "saintpaul".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR2() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR2");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "stlouis/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "stlouis/input2.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "stlouis/input3.jpg") );
		
		subTestHDR(inputs, "testHDR2_output.jpg", false);

		int [] exp_offsets_x = {0, 0, 2};
		int [] exp_offsets_y = {0, 0, 0};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR3".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR3() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR3");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR3/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR3/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR3/input2.jpg") );
		
		subTestHDR(inputs, "testHDR3_output.jpg", false);

		int [] exp_offsets_x = {0, 0, 0};
		int [] exp_offsets_y = {1, 0, -1};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR4".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR4() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR4");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR4/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR4/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR4/input2.jpg") );
		
		subTestHDR(inputs, "testHDR4_output.jpg", true);

		int [] exp_offsets_x = {-2, 0, 2};
		int [] exp_offsets_y = {-1, 0, 1};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR5".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR5() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR5");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR5/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR5/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR5/input2.jpg") );
		
		subTestHDR(inputs, "testHDR5_output.jpg", false);

		int [] exp_offsets_x = {0, 0, 0};
		int [] exp_offsets_y = {-1, 0, 0};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR6".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR6() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR6");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR6/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR6/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR6/input2.jpg") );
		
		subTestHDR(inputs, "testHDR6_output.jpg", false);

		int [] exp_offsets_x = {0, 0, 0};
		int [] exp_offsets_y = {1, 0, -1};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR7".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR7() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR7");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR7/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR7/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR7/input2.jpg") );
		
		subTestHDR(inputs, "testHDR7_output.jpg", false);

		int [] exp_offsets_x = {0, 0, 0};
		int [] exp_offsets_y = {0, 0, 1};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR8".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR8() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR8");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR8/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR8/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR8/input2.jpg") );
		
		subTestHDR(inputs, "testHDR8_output.jpg", false);

		int [] exp_offsets_x = {0, 0, 0};
		int [] exp_offsets_y = {0, 0, 0};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR9".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR9() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR9");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR9/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR9/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR9/input2.jpg") );
		
		subTestHDR(inputs, "testHDR9_output.jpg", false);

		int [] exp_offsets_x = {-1, 0, 1};
		int [] exp_offsets_y = {0, 0, -1};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR10".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR10() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR10");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR10/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR10/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR10/input2.jpg") );
		
		subTestHDR(inputs, "testHDR10_output.jpg", false);

		int [] exp_offsets_x = {2, 0, 0};
		int [] exp_offsets_y = {5, 0, 0};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR11".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR11() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR11");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR11/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR11/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR11/input2.jpg") );
		
		subTestHDR(inputs, "testHDR11_output.jpg", true);

		int [] exp_offsets_x = {-2, 0, 1};
		int [] exp_offsets_y = {1, 0, -1};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR12".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR12() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR12");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR12/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR12/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR12/input2.jpg") );
		
		subTestHDR(inputs, "testHDR12_output.jpg", true);

		int [] exp_offsets_x = {0, 0, 7};
		int [] exp_offsets_y = {0, 0, 8};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR13".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR13() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR13");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR13/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR13/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR13/input2.jpg") );
		
		subTestHDR(inputs, "testHDR13_output.jpg", false);

		int [] exp_offsets_x = {0, 0, 2};
		int [] exp_offsets_y = {0, 0, -1};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR14".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR14() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR14");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR14/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR14/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR14/input2.jpg") );
		
		subTestHDR(inputs, "testHDR14_output.jpg", false);

		int [] exp_offsets_x = {0, 0, 1};
		int [] exp_offsets_y = {0, 0, -1};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR15".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR15() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR15");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR15/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR15/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR15/input2.jpg") );
		
		subTestHDR(inputs, "testHDR15_output.jpg", false);

		int [] exp_offsets_x = {1, 0, -1};
		int [] exp_offsets_y = {2, 0, -3};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR16".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR16() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR16");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR16/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR16/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR16/input2.jpg") );
		
		subTestHDR(inputs, "testHDR16_output.jpg", false);

		int [] exp_offsets_x = {-1, 0, 2};
		int [] exp_offsets_y = {1, 0, -6};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR17".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR17() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR17");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR17/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR17/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR17/input2.jpg") );
		
		subTestHDR(inputs, "testHDR17_output.jpg", true);

		int [] exp_offsets_x = {0, 0, -3};
		int [] exp_offsets_y = {0, 0, -4};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR18".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR18() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR18");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR18/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR18/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR18/input2.jpg") );
		
		subTestHDR(inputs, "testHDR18_output.jpg", true);

		int [] exp_offsets_x = {0, 0, 0};
		int [] exp_offsets_y = {0, 0, 0};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR19".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR19() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR19");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR19/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR19/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR19/input2.jpg") );
		
		subTestHDR(inputs, "testHDR19_output.jpg", true);

		int [] exp_offsets_x = {0, 0, 0};
		int [] exp_offsets_y = {0, 0, 0};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR20".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR20() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR20");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR20/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR20/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR20/input2.jpg") );
		
		subTestHDR(inputs, "testHDR20_output.jpg", true);

		int [] exp_offsets_x = {0, 0, 0};
		int [] exp_offsets_y = {-1, 0, 0};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR21".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR21() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR21");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR21/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR21/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR21/input2.jpg") );
		
		subTestHDR(inputs, "testHDR21_output.jpg", true);

		int [] exp_offsets_x = {0, 0, 0};
		int [] exp_offsets_y = {0, 0, 0};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR22".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR22() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR22");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR22/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR22/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR22/input2.jpg") );
		
		subTestHDR(inputs, "testHDR22_output.jpg", true);

		int [] exp_offsets_x = {1, 0, -5};
		int [] exp_offsets_y = {1, 0, -6};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR23".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR23() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR23");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR23/memorial0064.png") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR23/memorial0066.png") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR23/memorial0068.png") );
		
		subTestHDR(inputs, "testHDR23_output.jpg", false);

		int [] exp_offsets_x = {0, 0, 0};
		int [] exp_offsets_y = {0, 0, 0};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR24".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR24() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR24");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR24/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR24/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR24/input2.jpg") );
		
		subTestHDR(inputs, "testHDR24_output.jpg", true);

		int [] exp_offsets_x = {0, 0, 1};
		int [] exp_offsets_y = {0, 0, 0};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR25".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR25() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR25");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR25/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR25/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR25/input2.jpg") );
		
		subTestHDR(inputs, "testHDR25_output.jpg", true);

		int [] exp_offsets_x = {0, 0, 0};
		int [] exp_offsets_y = {1, 0, -1};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR26".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR26() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR26");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR26/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR26/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR26/input2.jpg") );
		
		subTestHDR(inputs, "testHDR26_output.jpg", true);

		int [] exp_offsets_x = {-1, 0, 1};
		int [] exp_offsets_y = {1, 0, -1};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR27".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR27() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR27");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR27/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR27/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR27/input2.jpg") );
		
		subTestHDR(inputs, "testHDR27_output.jpg", true);

		int [] exp_offsets_x = {0, 0, 2};
		int [] exp_offsets_y = {0, 0, 0};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR28".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR28() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR28");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR28/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR28/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR28/input2.jpg") );
		
		subTestHDR(inputs, "testHDR28_output.jpg", true);

		int [] exp_offsets_x = {0, 0, 2};
		int [] exp_offsets_y = {0, 0, -1};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR29".
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDR29() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR29");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR29/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR29/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR29/input2.jpg") );
		
		subTestHDR(inputs, "testHDR29_output.jpg", false);

		int [] exp_offsets_x = {-1, 0, 3};
		int [] exp_offsets_y = {0, 0, -1};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR30".
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void testHDR30() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR30");

		setToDefault();

		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR30/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR30/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR30/input2.jpg") );

		subTestHDR(inputs, "testHDR30_output.jpg", false);

		// offsets for full image
		//int [] exp_offsets_x = {-6, 0, -1};
		//int [] exp_offsets_y = {23, 0, -13};
		// offsets using centre quarter image
		int [] exp_offsets_x = {-5, 0, 0};
		int [] exp_offsets_y = {22, 0, -13};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR31".
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void testHDR31() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR31");

		setToDefault();

		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR31/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR31/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR31/input2.jpg") );

		subTestHDR(inputs, "testHDR31_output.jpg", false);

		// offsets for full image
		//int [] exp_offsets_x = {0, 0, 4};
		//int [] exp_offsets_y = {21, 0, -11};
		// offsets using centre quarter image
		int [] exp_offsets_x = {0, 0, 3};
		int [] exp_offsets_y = {21, 0, -11};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR32".
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void testHDR32() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR32");

		setToDefault();

		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR32/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR32/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR32/input2.jpg") );

		subTestHDR(inputs, "testHDR32_output.jpg", true);

		int [] exp_offsets_x = {1, 0, 0};
		int [] exp_offsets_y = {13, 0, -10};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR33".
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void testHDR33() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR33");

		setToDefault();

		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR33/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR33/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR33/input2.jpg") );

		subTestHDR(inputs, "testHDR33_output.jpg", true);

		int [] exp_offsets_x = {13, 0, -10};
		int [] exp_offsets_y = {24, 0, -12};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR34".
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void testHDR34() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR34");

		setToDefault();

		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR34/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR34/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR34/input2.jpg") );

		subTestHDR(inputs, "testHDR34_output.jpg", true);

		int [] exp_offsets_x = {5, 0, -8};
		int [] exp_offsets_y = {0, 0, -2};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDR35".
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void testHDR35() throws IOException, InterruptedException {
		Log.d(TAG, "testHDR35");

		setToDefault();

		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR35/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR35/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDR35/input2.jpg") );

		subTestHDR(inputs, "testHDR35_output.jpg", true);

		int [] exp_offsets_x = {-10, 0, 3};
		int [] exp_offsets_y = {7, 0, -3};
		checkHDROffsets(exp_offsets_x, exp_offsets_y);
	}

	/** Tests HDR algorithm on test samples "testHDRtemp".
	 *  Used for one-off testing, or to recreate HDR images from the base exposures to test an updated alorithm.
	 *  The test images should be copied to the test device into DCIM/testOpenCamera/testdata/hdrsamples/testHDRtemp/ .
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void testHDRtemp() throws IOException, InterruptedException {
		Log.d(TAG, "testHDRtemp");

		setToDefault();
		
		// list assets
		List<Bitmap> inputs = new ArrayList<>();
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDRtemp/input0.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDRtemp/input1.jpg") );
		inputs.add( getBitmapFromFile(hdr_images_path + "testHDRtemp/input2.jpg") );
		
		subTestHDR(inputs, "testHDRtemp_output.jpg", true);
	}
}
