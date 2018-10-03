package net.sourceforge.opencamera.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class HDRTests {
	/** Tests for HDR algorithm - only need to run on a single device
	 *  Should manually look over the images dumped onto DCIM/
	 *  To use these tests, the testdata/ subfolder should be manually copied to the test device in the DCIM/testOpenCamera/
	 *  folder (so you have DCIM/testOpenCamera/testdata/). We don't use assets/ as we'd end up with huge APK sizes which takes
	 *  time to transfer to the device everytime we run the tests.
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(MainTests.class.getName());
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR1"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR2"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR3"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR4"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR5"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR6"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR7"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR8"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR9"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR10"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR11"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR12"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR13"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR14"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR15"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR16"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR17"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR18"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR19"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR20"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR21"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR22"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR23"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR24"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR25"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR26"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR27"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR28"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR29"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR30"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR31"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR32"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR33"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR34"));
		suite.addTest(TestSuite.createTest(MainActivityTest.class, "testHDR35"));
        return suite;
    }
}
