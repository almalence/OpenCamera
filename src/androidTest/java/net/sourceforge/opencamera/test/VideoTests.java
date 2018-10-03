package net.sourceforge.opencamera.test;

import junit.framework.Test;
import junit.framework.TestSuite;
public class VideoTests {
	// Tests related to video recording; note that tests to do with video mode that don't record are still part of MainTests
	public static Test suite() {
		TestSuite suite = new TestSuite(MainTests.class.getName());
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideo"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoAudioControl"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoSAF"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoSubtitles"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testImmersiveMode"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testImmersiveModeEverything"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoStabilization"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoExposureLock"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoFocusArea"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoQuick"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoQuickSAF"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoMaxDuration"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoMaxDurationRestart"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoMaxDurationRestartInterrupt"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoSettings"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoMacro"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoPause"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoPauseStop"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoFlashVideo"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testVideoTimerInterrupt"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testVideoPopup"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testVideoTimerPopup"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoAvailableMemory"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoAvailableMemory2"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoMaxFileSize1"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoMaxFileSize2"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoMaxFileSize3"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoForceFailure"));
		// put tests which change bitrate, fps or test 4K at end
		// update: now deprecating these tests, as setting these settings can be dodgy on some devices
		/*suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoBitrate"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideoFPS"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testTakeVideo4K"));*/
        return suite;
    }
}
