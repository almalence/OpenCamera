package net.sourceforge.opencamera.test;

import android.media.CamcorderProfile;

import net.sourceforge.opencamera.CameraController.CameraController;
import net.sourceforge.opencamera.CameraController.CameraController2;
import net.sourceforge.opencamera.LocationSupplier;
import net.sourceforge.opencamera.Preview.Preview;
import net.sourceforge.opencamera.Preview.VideoQualityHandler;
import net.sourceforge.opencamera.TextFormatter;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

class Log {
	public static void d(String tag, String text) {
		System.out.println(tag + ": " + text);
	}
}

/**
 * Note, need to run with MyDebug.LOG set to false, due to Android's Log.d not being mocked (good
 * practice to test release code anyway).
 */
public class UnitTest {
	private static final String TAG = "UnitTest";

	@Test
	public void testLocationToDMS() {
		Log.d(TAG, "testLocationToDMS");

		String location_string = LocationSupplier.locationToDMS(0.0);
		Log.d(TAG, "location_string: " + location_string);
		assertTrue(location_string.equals("0°0'0\""));

		location_string = LocationSupplier.locationToDMS(0.0000306);
		Log.d(TAG, "location_string: " + location_string);
		assertTrue(location_string.equals("0°0'0\""));

		location_string = LocationSupplier.locationToDMS(0.000306);
		Log.d(TAG, "location_string: " + location_string);
		assertTrue(location_string.equals("0°0'1\""));

		location_string = LocationSupplier.locationToDMS(0.00306);
		Log.d(TAG, "location_string: " + location_string);
		assertTrue(location_string.equals("0°0'11\""));

		location_string = LocationSupplier.locationToDMS(0.9999);
		Log.d(TAG, "location_string: " + location_string);
		assertTrue(location_string.equals("0°59'59\""));

		location_string = LocationSupplier.locationToDMS(1.7438);
		Log.d(TAG, "location_string: " + location_string);
		assertTrue(location_string.equals("1°44'37\""));

		location_string = LocationSupplier.locationToDMS(53.000137);
		Log.d(TAG, "location_string: " + location_string);
		assertTrue(location_string.equals("53°0'0\""));

		location_string = LocationSupplier.locationToDMS(147.00938);
		Log.d(TAG, "location_string: " + location_string);
		assertTrue(location_string.equals("147°0'33\""));

		location_string = LocationSupplier.locationToDMS(-0.0);
		Log.d(TAG, "location_string: " + location_string);
		assertTrue(location_string.equals("0°0'0\""));

		location_string = LocationSupplier.locationToDMS(-0.0000306);
		Log.d(TAG, "location_string: " + location_string);
		assertTrue(location_string.equals("0°0'0\""));

		location_string = LocationSupplier.locationToDMS(-0.000306);
		Log.d(TAG, "location_string: " + location_string);
		assertTrue(location_string.equals("-0°0'1\""));

		location_string = LocationSupplier.locationToDMS(-0.00306);
		Log.d(TAG, "location_string: " + location_string);
		assertTrue(location_string.equals("-0°0'11\""));

		location_string = LocationSupplier.locationToDMS(-0.9999);
		Log.d(TAG, "location_string: " + location_string);
		assertTrue(location_string.equals("-0°59'59\""));

		location_string = LocationSupplier.locationToDMS(-1.7438);
		Log.d(TAG, "location_string: " + location_string);
		assertTrue(location_string.equals("-1°44'37\""));

		location_string = LocationSupplier.locationToDMS(-53.000137);
		Log.d(TAG, "location_string: " + location_string);
		assertTrue(location_string.equals("-53°0'0\""));

		location_string = LocationSupplier.locationToDMS(-147.00938);
		Log.d(TAG, "location_string: " + location_string);
		assertTrue(location_string.equals("-147°0'33\""));
	}

	@Test
	public void testDateString() throws ParseException {
		Log.d(TAG, "testDateString");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
		Date date1 = sdf.parse("2017/01/31");
		assertEquals( TextFormatter.getDateString("preference_stamp_dateformat_none", date1), "" );
		assertEquals( TextFormatter.getDateString("preference_stamp_dateformat_yyyymmdd", date1), "2017/01/31" );
		assertEquals( TextFormatter.getDateString("preference_stamp_dateformat_ddmmyyyy", date1), "31/01/2017" );
		assertEquals( TextFormatter.getDateString("preference_stamp_dateformat_mmddyyyy", date1), "01/31/2017" );
	}

	@Test
	public void testTimeString() throws ParseException {
		Log.d(TAG, "testTimeString");
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
		Date time1 = sdf.parse("00:00:00");
		assertEquals( TextFormatter.getTimeString("preference_stamp_timeformat_none", time1), "" );
		assertEquals( TextFormatter.getTimeString("preference_stamp_timeformat_12hour", time1), "12:00:00 AM" );
		assertEquals( TextFormatter.getTimeString("preference_stamp_timeformat_24hour", time1), "00:00:00" );
		Date time2 = sdf.parse("08:15:43");
		assertEquals( TextFormatter.getTimeString("preference_stamp_timeformat_none", time2), "" );
		assertEquals( TextFormatter.getTimeString("preference_stamp_timeformat_12hour", time2), "08:15:43 AM" );
		assertEquals( TextFormatter.getTimeString("preference_stamp_timeformat_24hour", time2), "08:15:43" );
		Date time3 = sdf.parse("12:00:00");
		assertEquals( TextFormatter.getTimeString("preference_stamp_timeformat_none", time3), "" );
		assertEquals( TextFormatter.getTimeString("preference_stamp_timeformat_12hour", time3), "12:00:00 PM" );
		assertEquals( TextFormatter.getTimeString("preference_stamp_timeformat_24hour", time3), "12:00:00" );
		Date time4 = sdf.parse("13:53:06");
		assertEquals( TextFormatter.getTimeString("preference_stamp_timeformat_none", time4), "" );
		assertEquals( TextFormatter.getTimeString("preference_stamp_timeformat_12hour", time4), "01:53:06 PM" );
		assertEquals( TextFormatter.getTimeString("preference_stamp_timeformat_24hour", time4), "13:53:06" );
	}

	@Test
	public void testFormatTime() {
		Log.d(TAG, "testFormatTime");
		assertEquals( TextFormatter.formatTimeMS(952), "00:00:00,952" );
		assertEquals( TextFormatter.formatTimeMS(1092), "00:00:01,092" );
		assertEquals( TextFormatter.formatTimeMS(37301), "00:00:37,301" );
		assertEquals( TextFormatter.formatTimeMS(306921), "00:05:06,921" );
		assertEquals( TextFormatter.formatTimeMS(5391002), "01:29:51,002" );
		assertEquals( TextFormatter.formatTimeMS(92816837), "25:46:56,837" );
		assertEquals( TextFormatter.formatTimeMS(792816000), "220:13:36,000" );
	}

	@Test
	public void testBestPreviewFps() {
		Log.d(TAG, "testBestPreviewFps");

		List<int []> list0 = new ArrayList<>();
		list0.add(new int[]{15000, 15000});
		list0.add(new int[]{15000, 30000});
		list0.add(new int[]{7000, 30000});
		list0.add(new int[]{30000, 30000});
		int [] best_fps0 = Preview.chooseBestPreviewFps(list0);
		assertTrue(best_fps0[0] == 7000 && best_fps0[1] == 30000);

		List<int []> list1 = new ArrayList<>();
		list1.add(new int[]{15000, 15000});
		list1.add(new int[]{7000, 60000});
		list1.add(new int[]{15000, 30000});
		list1.add(new int[]{7000, 30000});
		list1.add(new int[]{30000, 30000});
		int [] best_fps1 = Preview.chooseBestPreviewFps(list1);
		assertTrue(best_fps1[0] == 7000 && best_fps1[1] == 60000);

		List<int []> list2 = new ArrayList<>();
		list2.add(new int[]{15000, 15000});
		list2.add(new int[]{7000, 15000});
		list2.add(new int[]{7000, 10000});
		list2.add(new int[]{8000, 19000});
		int [] best_fps2 = Preview.chooseBestPreviewFps(list2);
		assertTrue(best_fps2[0] == 8000 && best_fps2[1] == 19000);
	}

	@Test
	public void testMatchPreviewFpsToVideo() {
		Log.d(TAG, "matchPreviewFpsToVideo");

		List<int []> list0 = new ArrayList<>();
		list0.add(new int[]{15000, 15000});
		list0.add(new int[]{15000, 30000});
		list0.add(new int[]{7000, 30000});
		list0.add(new int[]{30000, 30000});
		int [] best_fps0 = Preview.matchPreviewFpsToVideo(list0, 30000);
		assertTrue(best_fps0[0] == 30000 && best_fps0[1] == 30000);

		List<int []> list1 = new ArrayList<>();
		list1.add(new int[]{15000, 15000});
		list1.add(new int[]{7000, 60000});
		list1.add(new int[]{15000, 30000});
		list1.add(new int[]{7000, 30000});
		list1.add(new int[]{30000, 30000});
		int [] best_fps1 = Preview.matchPreviewFpsToVideo(list1, 15000);
		assertTrue(best_fps1[0] == 15000 && best_fps1[1] == 15000);

		List<int []> list2 = new ArrayList<>();
		list2.add(new int[]{15000, 15000});
		list2.add(new int[]{7000, 15000});
		list2.add(new int[]{7000, 10000});
		list2.add(new int[]{8000, 19000});
		int [] best_fps2 = Preview.matchPreviewFpsToVideo(list2, 7000);
		assertTrue(best_fps2[0] == 7000 && best_fps2[1] == 10000);
	}

	private void compareVideoQuality(List<String> video_quality, List<String> exp_video_quality) {
		for(int i=0;i<video_quality.size();i++) {
			Log.d(TAG, "supported video quality: " + video_quality.get(i));
		}
		for(int i=0;i<exp_video_quality.size();i++) {
			Log.d(TAG, "expected video quality: " + exp_video_quality.get(i));
		}
		assertTrue( video_quality.size() == exp_video_quality.size() );
		for(int i=0;i<video_quality.size();i++) {
			String quality = video_quality.get(i);
			String exp_quality = exp_video_quality.get(i);
			assertTrue(quality.equals(exp_quality));
		}
	}

	/** Test for setting correct video resolutions and profiles.
	 */
	@Test
	public void testVideoResolutions1() {
		VideoQualityHandler video_quality_handler = new VideoQualityHandler();

		List<CameraController.Size> video_sizes = new ArrayList<>();
		video_sizes.add(new CameraController.Size(1920, 1080));
		video_sizes.add(new CameraController.Size(1280, 720));
		video_sizes.add(new CameraController.Size(1600, 900));
		video_quality_handler.setVideoSizes(video_sizes);
		video_quality_handler.sortVideoSizes();

		List<Integer> profiles = new ArrayList<>();
		List<VideoQualityHandler.Dimension2D> dimensions = new ArrayList<>();
		profiles.add(CamcorderProfile.QUALITY_HIGH);
		dimensions.add(new VideoQualityHandler.Dimension2D(1920, 1080));
		profiles.add(CamcorderProfile.QUALITY_1080P);
		dimensions.add(new VideoQualityHandler.Dimension2D(1920, 1080));
		profiles.add(CamcorderProfile.QUALITY_720P);
		dimensions.add(new VideoQualityHandler.Dimension2D(1280, 720));
		profiles.add(CamcorderProfile.QUALITY_LOW);
		dimensions.add(new VideoQualityHandler.Dimension2D(1280, 720));
		video_quality_handler.initialiseVideoQualityFromProfiles(profiles, dimensions);

		List<String> video_quality = video_quality_handler.getSupportedVideoQuality();
		List<String> exp_video_quality = new ArrayList<>();
		exp_video_quality.add("" + CamcorderProfile.QUALITY_HIGH);
		exp_video_quality.add("" + CamcorderProfile.QUALITY_720P + "_r1600x900");
		exp_video_quality.add("" + CamcorderProfile.QUALITY_720P);
		compareVideoQuality(video_quality, exp_video_quality);
	}

	/** Test for setting correct video resolutions and profiles.
	 */
	@Test
	public void testVideoResolutions2() {
		VideoQualityHandler video_quality_handler = new VideoQualityHandler();

		List<CameraController.Size> video_sizes = new ArrayList<>();
		video_sizes.add(new CameraController.Size(1920, 1080));
		video_sizes.add(new CameraController.Size(1280, 720));
		video_sizes.add(new CameraController.Size(1600, 900));
		video_quality_handler.setVideoSizes(video_sizes);
		video_quality_handler.sortVideoSizes();

		List<Integer> profiles = new ArrayList<>();
		List<VideoQualityHandler.Dimension2D> dimensions = new ArrayList<>();
		profiles.add(CamcorderProfile.QUALITY_HIGH);
		dimensions.add(new VideoQualityHandler.Dimension2D(1920, 1080));
		profiles.add(CamcorderProfile.QUALITY_720P);
		dimensions.add(new VideoQualityHandler.Dimension2D(1280, 720));
		profiles.add(CamcorderProfile.QUALITY_LOW);
		dimensions.add(new VideoQualityHandler.Dimension2D(1280, 720));
		video_quality_handler.initialiseVideoQualityFromProfiles(profiles, dimensions);

		List<String> video_quality = video_quality_handler.getSupportedVideoQuality();
		List<String> exp_video_quality = new ArrayList<>();
		exp_video_quality.add("" + CamcorderProfile.QUALITY_HIGH);
		exp_video_quality.add("" + CamcorderProfile.QUALITY_720P + "_r1600x900");
		exp_video_quality.add("" + CamcorderProfile.QUALITY_720P);
		compareVideoQuality(video_quality, exp_video_quality);
	}

	/** Test for setting correct video resolutions and profiles.
	 */
	@Test
	public void testVideoResolutions3() {
		VideoQualityHandler video_quality_handler = new VideoQualityHandler();

		List<CameraController.Size> video_sizes = new ArrayList<>();
		video_sizes.add(new CameraController.Size(1920, 1080));
		video_sizes.add(new CameraController.Size(1280, 720));
		video_sizes.add(new CameraController.Size(960, 720));
		video_sizes.add(new CameraController.Size(800, 480));
		video_sizes.add(new CameraController.Size(720, 576));
		video_sizes.add(new CameraController.Size(720, 480));
		video_sizes.add(new CameraController.Size(768, 576));
		video_sizes.add(new CameraController.Size(640, 480));
		video_sizes.add(new CameraController.Size(320, 240));
		video_sizes.add(new CameraController.Size(352, 288));
		video_sizes.add(new CameraController.Size(240, 160));
		video_sizes.add(new CameraController.Size(176, 144));
		video_sizes.add(new CameraController.Size(128, 96));
		video_quality_handler.setVideoSizes(video_sizes);
		video_quality_handler.sortVideoSizes();

		List<Integer> profiles = new ArrayList<>();
		List<VideoQualityHandler.Dimension2D> dimensions = new ArrayList<>();
		profiles.add(CamcorderProfile.QUALITY_HIGH);
		dimensions.add(new VideoQualityHandler.Dimension2D(1920, 1080));
		profiles.add(CamcorderProfile.QUALITY_1080P);
		dimensions.add(new VideoQualityHandler.Dimension2D(1920, 1080));
		profiles.add(CamcorderProfile.QUALITY_720P);
		dimensions.add(new VideoQualityHandler.Dimension2D(1280, 720));
		profiles.add(CamcorderProfile.QUALITY_480P);
		dimensions.add(new VideoQualityHandler.Dimension2D(720, 480));
		profiles.add(CamcorderProfile.QUALITY_CIF);
		dimensions.add(new VideoQualityHandler.Dimension2D(352, 288));
		profiles.add(CamcorderProfile.QUALITY_QVGA);
		dimensions.add(new VideoQualityHandler.Dimension2D(320, 240));
		profiles.add(CamcorderProfile.QUALITY_LOW);
		dimensions.add(new VideoQualityHandler.Dimension2D(320, 240));
		video_quality_handler.initialiseVideoQualityFromProfiles(profiles, dimensions);

		List<String> video_quality = video_quality_handler.getSupportedVideoQuality();
		List<String> exp_video_quality = new ArrayList<>();
		exp_video_quality.add("" + CamcorderProfile.QUALITY_HIGH);
		exp_video_quality.add("" + CamcorderProfile.QUALITY_720P);
		exp_video_quality.add("" + CamcorderProfile.QUALITY_480P + "_r960x720");
		exp_video_quality.add("" + CamcorderProfile.QUALITY_480P + "_r768x576");
		exp_video_quality.add("" + CamcorderProfile.QUALITY_480P + "_r720x576");
		exp_video_quality.add("" + CamcorderProfile.QUALITY_480P + "_r800x480");
		exp_video_quality.add("" + CamcorderProfile.QUALITY_480P);
		exp_video_quality.add("" + CamcorderProfile.QUALITY_CIF + "_r640x480");
		exp_video_quality.add("" + CamcorderProfile.QUALITY_CIF);
		exp_video_quality.add("" + CamcorderProfile.QUALITY_QVGA);
		exp_video_quality.add("" + CamcorderProfile.QUALITY_LOW + "_r240x160");
		exp_video_quality.add("" + CamcorderProfile.QUALITY_LOW + "_r176x144");
		exp_video_quality.add("" + CamcorderProfile.QUALITY_LOW + "_r128x96");
		compareVideoQuality(video_quality, exp_video_quality);
	}

	/** Test for setting correct video resolutions and profiles.
	 *  Case from https://sourceforge.net/p/opencamera/discussion/general/thread/b95bfb83/?limit=25#14ac
	 */
	@Test
	public void testVideoResolutions4() {
		VideoQualityHandler video_quality_handler = new VideoQualityHandler();

		// Video quality: 4_r864x480, 4, 2
		// Video resolutions: 176x144, 480x320, 640x480, 864x480, 1280x720, 1920x1080
		List<CameraController.Size> video_sizes = new ArrayList<>();
		video_sizes.add(new CameraController.Size(176, 144));
		video_sizes.add(new CameraController.Size(480, 320));
		video_sizes.add(new CameraController.Size(640, 480));
		video_sizes.add(new CameraController.Size(864, 480));
		video_sizes.add(new CameraController.Size(1280, 720));
		video_sizes.add(new CameraController.Size(1920, 1080));
		video_quality_handler.setVideoSizes(video_sizes);
		video_quality_handler.sortVideoSizes();

		List<Integer> profiles = new ArrayList<>();
		List<VideoQualityHandler.Dimension2D> dimensions = new ArrayList<>();
		profiles.add(CamcorderProfile.QUALITY_HIGH);
		dimensions.add(new VideoQualityHandler.Dimension2D(1920, 1080));
		profiles.add(CamcorderProfile.QUALITY_480P);
		dimensions.add(new VideoQualityHandler.Dimension2D(640, 480));
		profiles.add(CamcorderProfile.QUALITY_QCIF);
		dimensions.add(new VideoQualityHandler.Dimension2D(176, 144));
		video_quality_handler.initialiseVideoQualityFromProfiles(profiles, dimensions);

		List<String> video_quality = video_quality_handler.getSupportedVideoQuality();
		List<String> exp_video_quality = new ArrayList<>();
		exp_video_quality.add("" + CamcorderProfile.QUALITY_HIGH);
		exp_video_quality.add("" + CamcorderProfile.QUALITY_480P + "_r1280x720");
		exp_video_quality.add("" + CamcorderProfile.QUALITY_480P + "_r864x480");
		exp_video_quality.add("" + CamcorderProfile.QUALITY_480P);
		exp_video_quality.add("" + CamcorderProfile.QUALITY_QCIF + "_r480x320");
		exp_video_quality.add("" + CamcorderProfile.QUALITY_QCIF);
		compareVideoQuality(video_quality, exp_video_quality);
	}

	@Test
	public void testScaleForExposureTime() {
		Log.d(TAG, "testScaleForExposureTime");
		final double delta = 1.0e-6;
		final double full_exposure_time_scale = 0.5f;
		final long fixed_exposure_time = 1000000000L/60; // we only scale the exposure time at all if it's less than this value
		final long scaled_exposure_time = 1000000000L/120; // we only scale the exposure time by the full_exposure_time_scale if the exposure time is less than this value
		assertEquals( 1.0, CameraController2.getScaleForExposureTime(1000000000L/12, fixed_exposure_time, scaled_exposure_time, full_exposure_time_scale), delta );
		assertEquals( 1.0, CameraController2.getScaleForExposureTime(1000000000L/60, fixed_exposure_time, scaled_exposure_time, full_exposure_time_scale), delta );
		assertEquals( 1.0, CameraController2.getScaleForExposureTime(1000000000L/60, fixed_exposure_time, scaled_exposure_time, full_exposure_time_scale), delta );
		assertEquals( 2.0/3.0, CameraController2.getScaleForExposureTime(1000000000L/90, fixed_exposure_time, scaled_exposure_time, full_exposure_time_scale), delta );
		assertEquals( 0.5, CameraController2.getScaleForExposureTime(1000000000L/120, fixed_exposure_time, scaled_exposure_time, full_exposure_time_scale), delta );
		assertEquals( 0.5, CameraController2.getScaleForExposureTime(1000000000L/240, fixed_exposure_time, scaled_exposure_time, full_exposure_time_scale), delta );
	}
}
