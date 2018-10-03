package net.sourceforge.opencamera.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class MainTests {
	// Tests that don't fit into another of the Test suites
	public static Test suite() {
        /*return new TestSuiteBuilder(AllTests.class)
        .includeAllPackagesUnderHere()
        .build();*/
		TestSuite suite = new TestSuite(MainTests.class.getName());
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testStartCameraPreviewCount"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testSaveVideoMode"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testSaveFocusMode"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testSaveFlashTorchQuit"));
		//suite.addTest(TestSuite.createTest(MainActivityTest.class, "testSaveFlashTorchSwitchCamera"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testFlashStartup"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testFlashStartup2"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testPreviewSize"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testPreviewSizeWYSIWYG"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testAutoFocus"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testAutoFocusCorners"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testPopup"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testSwitchResolution"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testFaceDetection"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testFocusFlashAvailability"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testSwitchVideo"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testFocusSwitchVideoSwitchCameras"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testFocusRemainMacroSwitchCamera"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testFocusRemainMacroSwitchPhoto"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testFocusSaveMacroSwitchPhoto"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testFocusSwitchVideoResetContinuous"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testContinuousPictureFocus"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testContinuousPictureRepeatTouch"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testContinuousPictureSwitchAuto"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testContinuousVideoFocusForPhoto"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testStartupAutoFocus"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testExposureLockNotSaved"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testSaveQuality"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testZoom"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testZoomIdle"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testZoomSwitchCamera"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testSwitchCameraIdle"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testGallery"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testSettings"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testFolderChooserNew"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testFolderChooserInvalid"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testSaveFolderHistory"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testSaveFolderHistorySAF"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testPreviewRotation"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testSceneMode"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testColorEffect"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testWhiteBalance"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testImageQuality"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testFailOpenCamera"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testAudioControlIcon"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testOnError"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testGPSString"));
        return suite;
    }
}
