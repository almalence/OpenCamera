package net.sourceforge.opencamera.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class PhotoTests {
	// Tests related to taking photos; note that tests to do with photo mode that don't take photos are still part of MainTests
	public static Test suite() {
		TestSuite suite = new TestSuite(MainTests.class.getName());
		// put these tests first as they require various permissions be allowed, that can only be set by user action
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoSAF"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testLocationOn"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testLocationDirectionOn"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testLocationOff"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testDirectionOn"));
		// other tests:
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhoto"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoContinuous"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoContinuousNoTouch"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoAutoStabilise"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoFlashAuto"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoFlashOn"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoFlashTorch"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoAudioButton"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoNoAutofocus"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoNoThumbnail"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoFlashBug"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoFrontCamera"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoFrontCameraScreenFlash"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoLockedFocus"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoExposureCompensation"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoLockedLandscape"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoLockedPortrait"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoPreviewPaused"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoPreviewPausedAudioButton"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoPreviewPausedSAF"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoPreviewPausedTrash"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoPreviewPausedTrashSAF"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoPreviewPausedTrash2"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoQuickFocus"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoRepeatFocus"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoRepeatFocusLocked"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoAfterFocus"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoSingleTap"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoDoubleTap"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoAlt"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoAutoLevel"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoAutoLevelAngles"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTimerBackground"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTimerSettings"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTimerPopup"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoBurst"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testContinuousPicture1"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testContinuousPicture2"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testContinuousPictureFocusBurst"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testPhotoStamp"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoDRO"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakePhotoDROPhotoStamp"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testCreateSaveFolder1"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testCreateSaveFolder2"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testCreateSaveFolder3"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testCreateSaveFolder4"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testCreateSaveFolderUnicode"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testCreateSaveFolderEmpty"));
        return suite;
    }
}
