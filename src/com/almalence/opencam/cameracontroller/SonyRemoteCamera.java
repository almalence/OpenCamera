/* <!-- +++
package com.almalence.opencam_plus.cameracontroller;
+++ --> */
//<!-- -+-
package com.almalence.opencam.cameracontroller;

//-+- -->

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.hardware.Camera.Area;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.almalence.SwapHeap;
import com.almalence.sony.cameraremote.PictureCallbackSonyRemote;
import com.almalence.sony.cameraremote.ServerDevice;
import com.almalence.sony.cameraremote.SimpleCameraEventObserver;
import com.almalence.sony.cameraremote.SimpleRemoteApi;
import com.almalence.sony.cameraremote.SimpleStreamSurfaceView;
import com.almalence.sony.cameraremote.ZoomCallbackSonyRemote;
import com.almalence.util.ImageConversion;

/* <!-- +++
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.ApplicationInterface;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.PluginManagerInterface;
 import com.almalence.opencam_plus.R;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginManagerInterface;
import com.almalence.opencam.R;

//-+- -->

public class SonyRemoteCamera
{
	private static final String								TAG							= "SonyRemoteCamera";

	protected static Context								mainContext					= null;
	private static PluginManagerInterface					pluginManager				= null;
	private static ApplicationInterface						appInterface				= null;
	protected static Handler								messageHandler				= null;

	public static ServerDevice								mTargetDevice;
	private static SimpleRemoteApi							mRemoteApi;
	private static Set<String>								mAvailableCameraApiSet		= new HashSet<String>();
	private static Set<String>								mAvailableApiSet			= new HashSet<String>();

	private static SimpleCameraEventObserver				mEventObserver;
	private static SimpleCameraEventObserver.ChangeListener	mEventListener;

	public static List<CameraController.Size>				mPreviewSizes				= new ArrayList<CameraController.Size>();
	public static List<CameraController.Size>				mPictureSizes				= new ArrayList<CameraController.Size>();
	private static int										minExpoCompensation			= 0;
	private static int										maxExpoCompensation			= 0;
	private static float									expoCompensationStep		= 0;
	public static List<String>								availableWBModes			= new ArrayList<String>();
	public static List<String>								availableFocusModes			= new ArrayList<String>();
	public static List<String>								availableIsoModes			= new ArrayList<String>();
	public static List<String>								availableFlashModes			= new ArrayList<String>();
	public static boolean									isZoomAvailable				= false;

	public static HashMap<Long, JSONObject>					mPictureSizeNames			= new HashMap<Long, JSONObject>();

	public static int										previewWidth				= 640;
	public static int										previewHeight				= 480;

	public static String									currentWBMode				= "Auto WB";
	public static String									currentIsoMode				= "auto";
	public static String									currentFlashMode			= "off";
	public static int										currentExposureCompensation	= 0;

	public static Handler									UIhandler					= new Handler(
																								Looper.getMainLooper());

	public static List<Thread>								requestQueue				= new LinkedList<Thread>();
	public static boolean									opening						= false;
	public static ProgressDialog							progress;
	public static ProgressDialog							progressImageDownloading;
	public static ZoomCallbackSonyRemote					zoomCallbackSonyRemote		= null;

	public static double									focusX						= 50;
	public static double									focusY						= 50;

	public static void onCreateSonyRemoteCamera(Context context, ApplicationInterface app,
			PluginManagerInterface pluginManagerBase, Handler msgHandler)
	{
		mainContext = context;
		appInterface = app;
		pluginManager = pluginManagerBase;
		messageHandler = msgHandler;

		mEventListener = new SimpleCameraEventObserver.ChangeListenerTmpl()
		{

			@Override
			public void onShootModeChanged(String shootMode)
			{
				Log.d(TAG, "onShootModeChanged() called: " + shootMode);
				// refreshUi();
			}

			@Override
			public void onCameraStatusChanged(String status)
			{
				Log.d(TAG, "onCameraStatusChanged() called: " + status);
				// refreshUi();
			}

			@Override
			public void onApiListModified(List<String> apis)
			{
				Log.d(TAG, "onApiListModified() called");
				synchronized (mAvailableCameraApiSet)
				{
					mAvailableCameraApiSet.clear();
					for (String api : apis)
					{
						mAvailableCameraApiSet.add(api);
					}
					if (!mEventObserver.getLiveviewStatus() //
							&& isCameraApiAvailable("startLiveview"))
					{
						if (!CameraController.isRemoteCamera())
							return;

						if (appInterface.getSimpleStreamSurfaceView() != null
								&& !appInterface.getSimpleStreamSurfaceView().isStarted())
						{
							startLiveview();
						}
					}

					if (zoomCallbackSonyRemote != null)
					{
						if (isCameraApiAvailable("actZoom"))
						{
							isZoomAvailable = true;
							zoomCallbackSonyRemote.onZoomAvailabelChanged(true);
						} else
						{
							isZoomAvailable = false;
							zoomCallbackSonyRemote.onZoomAvailabelChanged(false);
						}
					}
				}
			}

			@Override
			public void onZoomPositionChanged(int zoomPosition)
			{
				if (zoomCallbackSonyRemote != null)
				{
					zoomCallbackSonyRemote.onZoomPositionChanged(zoomPosition);
				}
			}

			@Override
			public void onLiveviewStatusChanged(boolean status)
			{
				Log.d(TAG, "onLiveviewStatusChanged() called = " + status);
			}

			@Override
			public void onStorageIdChanged(String storageId)
			{
				Log.d(TAG, "onStorageIdChanged() called: " + storageId);
				// refreshUi();
			}

			@Override
			public void onPictureTaken(String takePictureUrl)
			{
				downloadAndProcessImage(takePictureUrl, false);
			}
		};
	}

	static Boolean	isBusy	= false;

	public static void sendRequest()
	{
		synchronized (isBusy)
		{
			if (!isBusy && requestQueue.size() > 0)
			{
				isBusy = true;
				requestQueue.get(0).start();
				requestQueue.remove(0);
			} else if (requestQueue.size() == 0 && opening && previewImagesCount >= 3)
			{
				if (progress != null)
				{
					progress.dismiss();
				}
				opening = false;
				messageHandler.sendEmptyMessage(ApplicationInterface.MSG_SURFACE_READY);
			}
		}

	}

	public static void onRequestResult()
	{
		synchronized (isBusy)
		{
			isBusy = false;
		}
		CameraController.sendMessage(ApplicationInterface.MSG_BROADCAST,
				ApplicationInterface.MSG_REMOTE_CAMERA_PARAMETR_CHANGED);
		sendRequest();
	}

	public static void setZoomCallbackSonyRemote(ZoomCallbackSonyRemote callback)
	{
		zoomCallbackSonyRemote = callback;
	}

	public static void openCameraSonyRemote()
	{
		mRemoteApi = new SimpleRemoteApi(mTargetDevice);
		mEventObserver = new SimpleCameraEventObserver(mainContext, mRemoteApi);
		mEventObserver.activate();

		opening = true;
		previewImagesCount = 0;

		progress = ProgressDialog.show(mainContext, mainContext.getResources().getString(R.string.title_connecting),
				mainContext.getResources().getString(R.string.msg_connecting), true);
		progress.setCancelable(true);
		progress.setOnCancelListener(new OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				switchBackToDeviceCamera();
			}
		});

		prepareOpenConnection();
	}

	private static void prepareOpenConnection()
	{
		Log.d(TAG, "prepareToOpenConection() exec");

		// setProgressBarIndeterminateVisibility(true);

		requestQueue.add(new Thread()
		{

			@Override
			public void run()
			{
				try
				{
					if (mRemoteApi == null) {
						onRequestResult();
						return;
					}
					
					// Get available API list (Camera API)
					JSONObject replyJsonCamera = mRemoteApi.getCameraMethodTypes();
					loadAvailableApiList(replyJsonCamera);

					try
					{
						// Get available API list (AvContent API)
						JSONObject replyJsonAvcontent = mRemoteApi.getAvcontentMethodTypes();
						loadAvailableApiList(replyJsonAvcontent);
					} catch (IOException e)
					{
						Log.d(TAG, "AvContent is not support.");
					}

					if (!isApiAvailable("setCameraFunction"))
					{

						// this device does not support setCameraFunction.
						// No need to check camera status.

						openConnection();

					} else
					{

						// this device supports setCameraFunction.
						// after confirmation of camera state, open connection.
						Log.d(TAG, "this device support set camera function");

						if (!isApiAvailable("getEvent"))
						{
							Log.e(TAG, "this device is not support getEvent");
							openConnection();
							return;
						}

						// confirm current camera status
						String cameraStatus = null;
						JSONObject replyJson = mRemoteApi.getEvent(false);
						JSONArray resultsObj = replyJson.getJSONArray("result");
						JSONObject cameraStatusObj = resultsObj.getJSONObject(1);
						String type = cameraStatusObj.getString("type");
						if ("cameraStatus".equals(type))
						{
							cameraStatus = cameraStatusObj.getString("cameraStatus");
						} else
						{
							throw new IOException();
						}

						if (isShootingStatus(cameraStatus))
						{
							Log.d(TAG, "camera function is Remote Shooting.");
							openConnection();
						} else
						{
							// set Listener
							startOpenConnectionAfterChangeCameraState();

							// set Camera function to Remote Shooting
							replyJson = mRemoteApi.setCameraFunction("Remote Shooting");
						}
					}
				} catch (IOException e)
				{
					Log.w(TAG, "prepareToStartContentsListMode: IOException: " + e.getMessage());
					// DisplayHelper.toast(getApplicationContext(),
					// R.string.msg_error_api_calling);
					// DisplayHelper.setProgressIndicator(SampleCameraActivity.this,
					// false);
				} catch (JSONException e)
				{
					Log.w(TAG, "prepareToStartContentsListMode: JSONException: " + e.getMessage());
					// DisplayHelper.toast(getApplicationContext(),
					// R.string.msg_error_api_calling);
					// DisplayHelper.setProgressIndicator(SampleCameraActivity.this,
					// false);
				} finally
				{
					onRequestResult();
				}
			}
		});
		sendRequest();
	}

	/**
	 * Open connection to the camera device to start monitoring Camera events
	 * and showing liveview.
	 */
	private static void openConnection()
	{

		mEventObserver.setEventChangeListener(mEventListener);
		requestQueue.add(new Thread()
		{

			@Override
			public void run()
			{
				Log.d(TAG, "openConnection(): exec.");
				
				if (mRemoteApi == null) {
					onRequestResult();
					return;
				}

				try
				{
					
					JSONObject replyJson = null;

					// getAvailableApiList
					replyJson = mRemoteApi.getAvailableApiList();
					loadAvailableCameraApiList(replyJson);

					// check version of the server device
					if (isCameraApiAvailable("getApplicationInfo"))
					{
						Log.d(TAG, "openConnection(): getApplicationInfo()");
						replyJson = mRemoteApi.getApplicationInfo();
						if (!isAvailableServerVersion(replyJson))
						{
							return;
						}
					} else
					{
						// never happens;
						return;
					}

					// startRecMode if necessary.
					if (isCameraApiAvailable("startRecMode"))
					{
						Log.d(TAG, "openConnection(): startRecMode()");
						replyJson = mRemoteApi.startRecMode();

						// Call again.
						replyJson = mRemoteApi.getAvailableApiList();
						loadAvailableCameraApiList(replyJson);
					}

					String modeName = PluginManager.getInstance().getActiveModeID();
					if (modeName.contains("video"))
					{
						setShootMode("movie");
					} else
					{
						setShootMode("still");
					}

					// getEvent start
					if (isCameraApiAvailable("getEvent"))
					{
						Log.d(TAG, "openConnection(): EventObserver.start()");
						mEventObserver.start();
					}

					// Liveview start
					if (isCameraApiAvailable("startLiveview"))
					{
						Log.d(TAG, "openConnection(): LiveviewSurface.start()");
						startLiveview();
					}

					// Set exposure mode
					if (isCameraApiAvailable("setExposureMode"))
					{
						Log.d(TAG, "openConnection(): setExposureMode");
						setExposureMode();
					}

					if (zoomCallbackSonyRemote != null)
					{
						if (isCameraApiAvailable("actZoom"))
						{
							isZoomAvailable = true;
							zoomCallbackSonyRemote.onZoomAvailabelChanged(true);
						} else
						{
							isZoomAvailable = false;
							zoomCallbackSonyRemote.onZoomAvailabelChanged(false);
						}
					}

					Log.d(TAG, "openConnection(): completed.");
				} catch (IOException e)
				{
					Log.w(TAG, "openConnection : IOException: " + e.getMessage());
				} finally
				{
					onRequestResult();
				}
			}
		});
		sendRequest();

	}

	public static void onPauseSonyRemoteCamera()
	{
		closeConnection();
	}

	/**
	 * Stop monitoring Camera events and close liveview connection.
	 */
	private static void closeConnection()
	{

		Log.d(TAG, "closeConnection(): exec.");
		// Liveview stop
		Log.d(TAG, "closeConnection(): LiveviewSurface.stop()");
		stopLiveview();

		// getEvent stop
		Log.d(TAG, "closeConnection(): EventObserver.release()");
		if (mEventObserver != null)
		{
			mEventObserver.release();
		}

		// stopRecMode if necessary.
		if (isCameraApiAvailable("stopRecMode"))
		{
			requestQueue.add(new Thread()
			{

				@Override
				public void run()
				{
					Log.d(TAG, "closeConnection(): stopRecMode()");
					
					if (mRemoteApi == null) {
						onRequestResult();
						return;
					}
					
					try
					{
						if (mRemoteApi != null)
						{
							mRemoteApi.stopRecMode();
						}
					} catch (IOException e)
					{
						Log.w(TAG, "closeConnection: IOException: " + e.getMessage());
					} finally
					{
						onRequestResult();
					}
				}
			});
			sendRequest();
		}

		currentWBMode = "Auto WB";
		currentIsoMode = "auto";
		currentFlashMode = "off";
		currentExposureCompensation = 0;
		requestQueue.clear();
		mPreviewSizes.clear();
		mPictureSizes.clear();
		minExpoCompensation = 0;
		maxExpoCompensation = 0;
		expoCompensationStep = 0;
		availableWBModes.clear();
		availableFocusModes.clear();
		availableIsoModes.clear();
		availableFlashModes.clear();
		isZoomAvailable = false;
		mPictureSizeNames.clear();
		previewWidth = 640;
		previewHeight = 480;
		opening = false;

		if (progress != null)
		{
			progress.dismiss();
		}
		if (progressImageDownloading != null)
		{
			progressImageDownloading.dismiss();
		}

		Log.d(TAG, "closeConnection(): completed.");
	}

	private static void startOpenConnectionAfterChangeCameraState()
	{
		Log.d(TAG, "startOpenConectiontAfterChangeCameraState() exec");

		((Activity) mainContext).runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				mEventObserver.setEventChangeListener(new SimpleCameraEventObserver.ChangeListenerTmpl()
				{

					@Override
					public void onCameraStatusChanged(String status)
					{
						Log.d(TAG, "onCameraStatusChanged:" + status);
						if ("IDLE".equals(status))
						{
							openConnection();
						}
						// refreshUi();
					}

					@Override
					public void onShootModeChanged(String shootMode)
					{
						// refreshUi();
					}

					@Override
					public void onStorageIdChanged(String storageId)
					{
						// refreshUi();
					}

					@Override
					public void onPictureTaken(String takePictureUrl)
					{
						downloadAndProcessImage(takePictureUrl, false);
					}
				});

				mEventObserver.start();
			}
		});
	}

	private static boolean isShootingStatus(String currentStatus)
	{
		Set<String> shootingStatus = new HashSet<String>();
		shootingStatus.add("IDLE");
		shootingStatus.add("StillCapturing");
		shootingStatus.add("StillSaving");
		shootingStatus.add("MovieWaitRecStart");
		shootingStatus.add("MovieRecording");
		shootingStatus.add("MovieWaitRecStop");
		shootingStatus.add("MovieSaving");
		shootingStatus.add("IntervalWaitRecStart");
		shootingStatus.add("IntervalRecording");
		shootingStatus.add("IntervalWaitRecStop");
		shootingStatus.add("AudioWaitRecStart");
		shootingStatus.add("AudioRecording");
		shootingStatus.add("AudioWaitRecStop");
		shootingStatus.add("AudioSaving");

		return shootingStatus.contains(currentStatus);
	}

	/**
	 * Call cancelTouchAFPosition
	 * 
	 * @param mode
	 */
	public static void cancelAutoFocusSonyRemote()
	{
		requestQueue.add(new Thread()
		{

			@Override
			public void run()
			{
				if (mRemoteApi == null) {
					onRequestResult();
					return;
				}
				
				try
				{
					if (mRemoteApi == null)
					{
						return;
					}
					JSONObject replyJson = mRemoteApi.cancelTouchAFPosition();
					JSONArray resultsObj = replyJson.getJSONArray("result");
					int resultCode = resultsObj.getInt(0);
					if (resultCode != 0)
					{
						Log.w(TAG, "setShootMode: error: " + resultCode);
					}
				} catch (IOException e)
				{
					Log.w(TAG, "setShootMode: IOException: " + e.getMessage());
				} catch (JSONException e)
				{
					Log.w(TAG, "setShootMode: JSON format error.");
				} finally
				{
					onRequestResult();
				}
			}
		});
		sendRequest();
	}

	/**
	 * Call setTouchAFPosition
	 * 
	 * @param mode
	 */
	public static boolean autoFocusSonyRemote()
	{
		requestQueue.add(new Thread()
		{

			@Override
			public void run()
			{
				if (mRemoteApi == null) {
					CameraController.onAutoFocus(false);
					onRequestResult();
					return;
				}
				
				boolean result = false;
				try
				{
					JSONObject replyJson = mRemoteApi.setTouchAFPosition(focusX, focusY);
					JSONArray resultsArray = replyJson.getJSONArray("result");
					int resultCode = resultsArray.getInt(0);
					if (resultCode != 0)
					{
						Log.w(TAG, "setTouchAFPosition: error: " + resultCode);
					} else
					{
						JSONObject resObj = resultsArray.getJSONObject(1);
						result = resObj.getBoolean("AFResult");
					}
				} catch (IOException e)
				{
					Log.w(TAG, "setTouchAFPosition: IOException: " + e.getMessage());
				} catch (JSONException e)
				{
					Log.w(TAG, "setTouchAFPosition: JSON format error.");
				} finally
				{
					final boolean res = result;
					UIhandler.post(new Runnable()
					{
						@Override
						public void run()
						{
							CameraController.onAutoFocus(res);
						}
					});
					onRequestResult();
				}
			}
		});
		sendRequest();
		return true;
	}

	/**
	 * Call setShootMode
	 * 
	 * @param mode
	 */
	public static void setShootMode(final String mode)
	{
		requestQueue.add(new Thread()
		{

			@Override
			public void run()
			{
				if (mRemoteApi == null) {
					onRequestResult();
					return;
				}
				
				try
				{
					JSONObject replyJson = mRemoteApi.setShootMode(mode);
					JSONArray resultsObj = replyJson.getJSONArray("result");
					int resultCode = resultsObj.getInt(0);
					if (resultCode != 0)
					{
						Log.w(TAG, "setShootMode: error: " + resultCode);
					}
				} catch (IOException e)
				{
					Log.w(TAG, "setShootMode: IOException: " + e.getMessage());
				} catch (JSONException e)
				{
					Log.w(TAG, "setShootMode: JSON format error.");
				} finally
				{
					onRequestResult();
				}
			}
		});
		sendRequest();
	}

	public static void setCameraFocusAreasSonyRemote(List<Area> focusAreas)
	{
		if (focusAreas != null && focusAreas.size() > 0)
		{
			Area area = focusAreas.get(0);
			int x = area.rect.centerX();
			int y = area.rect.centerY();

			focusX = (double) (((previewWidth / 2000) * x + previewWidth / 2) / previewHeight);
			focusY = (double) (((previewHeight / 2000) * y + previewHeight / 2) / previewHeight);
		} else
		{
			focusX = 50;
			focusY = 50;
		}
	}

	public static void takePicture(final PictureCallbackSonyRemote pictureListener)
	{
		requestQueue.add(new Thread()
		{
			@Override
			public void run()
			{
				if (mRemoteApi == null) {
					onRequestResult();
					return;
				}
				
				try
				{
					JSONObject replyJson = mRemoteApi.actTakePicture();
					JSONArray resultsObj = replyJson.getJSONArray("result");
					JSONArray imageUrlsObj = resultsObj.getJSONArray(0);
					String postImageUrl = null;
					if (1 <= imageUrlsObj.length())
					{
						postImageUrl = imageUrlsObj.getString(0);
					}
					if (postImageUrl == null)
					{
						Log.w(TAG, "takeAndFetchPicture: post image URL is null.");
						return;
					}

					downloadAndProcessImage(postImageUrl, true);

				} catch (IOException e)
				{
					Log.w(TAG, "IOException while closing slicer: " + e.getMessage());
				} catch (JSONException e)
				{
					Log.w(TAG, "JSONException while closing slicer");
				} finally
				{
					onRequestResult();
				}
			}
		});
		sendRequest();
	}

	public static void downloadAndProcessImage(final String postImageUrl, final boolean fromRequest)
	{
		((Activity) mainContext).runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				progressImageDownloading = ProgressDialog.show(mainContext,
						mainContext.getResources().getString(R.string.title_downloading), mainContext.getResources()
								.getString(R.string.msg_downloading), true);
			}
		});

		requestQueue.add(new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					URL url = new URL(postImageUrl);
					InputStream istream = new BufferedInputStream(url.openStream());
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					int nRead;
					byte[] data = new byte[1024];

					while ((nRead = istream.read(data, 0, data.length)) != -1)
					{
						buffer.write(data, 0, nRead);
					}

					buffer.flush();
					CameraController.getInstance().onPictureTakenSonyRemote(buffer.toByteArray(), fromRequest);
				} catch (IOException e)
				{
					e.printStackTrace();
				} finally
				{
					((Activity) mainContext).runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							if (progressImageDownloading != null)
							{
								progressImageDownloading.dismiss();
							}
						}
					});
					onRequestResult();
				}
			}
		});
		sendRequest();
	}

	/**
	 * Call startMovieRec
	 */
	public static void startMovieRec()
	{
		requestQueue.add(new Thread()
		{

			@Override
			public void run()
			{
				if (mRemoteApi == null) {
					onRequestResult();
					return;
				}
				
				try
				{
					Log.d(TAG, "startMovieRec: exec.");
					JSONObject replyJson = mRemoteApi.startMovieRec();
					JSONArray resultsObj = replyJson.getJSONArray("result");
					int resultCode = resultsObj.getInt(0);
					if (resultCode != 0)
					{
						Log.w(TAG, "startMovieRec: error: " + resultCode);
					}
				} catch (IOException e)
				{
					Log.w(TAG, "startMovieRec: IOException: " + e.getMessage());
				} catch (JSONException e)
				{
					Log.w(TAG, "startMovieRec: JSON format error.");
				} finally
				{
					onRequestResult();
				}
			}
		});
		sendRequest();
	}

	/**
	 * Call stopMovieRec
	 */
	public static void stopMovieRec()
	{
		requestQueue.add(new Thread()
		{

			@Override
			public void run()
			{
				if (mRemoteApi == null) {
					onRequestResult();
					return;
				}
				
				try
				{
					Log.d(TAG, "stopMovieRec: exec.");
					JSONObject replyJson = mRemoteApi.stopMovieRec();
					JSONArray resultsObj = replyJson.getJSONArray("result");
					String thumbnailUrl = resultsObj.getString(0);
					if (thumbnailUrl == null)
					{
						Log.w(TAG, "stopMovieRec: error");
					}
				} catch (IOException e)
				{
					Log.w(TAG, "stopMovieRec: IOException: " + e.getMessage());
				} catch (JSONException e)
				{
					Log.w(TAG, "stopMovieRec: JSON format error.");
				} finally
				{
					onRequestResult();
				}
			}
		});
		sendRequest();
	}

	public static boolean isZoomAvailable()
	{
		return isZoomAvailable;
	}

	/**
	 * Call actZoom
	 * 
	 * @param direction
	 * @param movement
	 */
	public static void actZoom(final String direction, final String movement)
	{
		new Thread()
		{
			@Override
			public void run()
			{
				if (mRemoteApi == null) {
					onRequestResult();
					return;
				}
				
				try
				{
					JSONObject replyJson = mRemoteApi.actZoom(direction, movement);
					JSONArray resultsObj = replyJson.getJSONArray("result");
					int resultCode = resultsObj.getInt(0);
					if (resultCode == 0)
					{
						// Success, but no refresh UI at the point.
						Log.v(TAG, "actZoom: success");
					} else
					{
						Log.w(TAG, "actZoom: error: " + resultCode);
					}
				} catch (IOException e)
				{
					Log.w(TAG, "actZoom: IOException: " + e.getMessage());
				} catch (JSONException e)
				{
					Log.w(TAG, "actZoom: JSON format error.");
				}
			}
		}.start();
	}

	private static void setExposureMode()
	{
		if (mRemoteApi == null) {
			onRequestResult();
			return;
		}
		
		JSONObject replyJson = null;
		try
		{
			replyJson = mRemoteApi.setExposureMode();

			JSONArray resultArrayJson = replyJson.getJSONArray("result");
			int result = resultArrayJson.getInt(0);
		} catch (JSONException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private static void startLiveview()
	{
		if (!CameraController.isRemoteCamera())
			return;

		if (appInterface.getSimpleStreamSurfaceView() == null)
		{
			Log.w(TAG, "startLiveview mLiveviewSurface is null.");
			return;
		}
		requestQueue.add(new Thread()
		{
			@Override
			public void run()
			{
				if (mRemoteApi == null) {
					onRequestResult();
					return;
				}
				
				try
				{
					JSONObject replyJson = null;
					replyJson = mRemoteApi.startLiveview();

					if (!SimpleRemoteApi.isErrorReply(replyJson))
					{
						JSONArray resultsObj = replyJson.getJSONArray("result");
						if (1 <= resultsObj.length())
						{
							initRemoteCameraFeatures();

							// Obtain liveview URL from the result.
							final String liveviewUrl = resultsObj.getString(0);
							((Activity) mainContext).runOnUiThread(new Runnable()
							{

								@Override
								public void run()
								{
									if (!CameraController.isRemoteCamera())
										return;

									appInterface.getSimpleStreamSurfaceView().start(liveviewUrl,
											new SimpleStreamSurfaceView.StreamFrameListener()
											{

												@Override
												public void onFrameAvailable(byte[] jpegData)
												{
													onPreviewFrame(jpegData);

												}
											}, new SimpleStreamSurfaceView.StreamErrorListener()
											{

												@Override
												public void onError(StreamErrorReason reason)
												{
													switchBackToDeviceCamera();
												}
											});
									appInterface.getSimpleStreamSurfaceView().setVisibility(View.VISIBLE);
									appInterface.getSimpleStreamSurfaceView().bringToFront();
								}
							});
						}
					}
				} catch (IOException e)
				{
					Log.w(TAG, "startLiveview IOException: " + e.getMessage());
				} catch (JSONException e)
				{
					Log.w(TAG, "startLiveview JSONException: " + e.getMessage());
				} finally
				{
					onRequestResult();
				}
			}
		});
		sendRequest();
	}

	private static void switchBackToDeviceCamera()
	{
		((Activity) mainContext).runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen
						.getMainContext());
				prefs.edit().putInt(ApplicationScreen.sCameraModePref, 0).commit();
				appInterface.relaunchCamera();
			}
		});
	}

	private static void stopLiveview()
	{
		Log.e("SonyRemoteCamera", "stopLiveview");
		requestQueue.add(new Thread()
		{
			@Override
			public void run()
			{
				if (mRemoteApi == null) {
					onRequestResult();
					return;
				}
				
				try
				{
					if (mRemoteApi != null)
					{
						mRemoteApi.stopLiveview();
					}
				} catch (IOException e)
				{
					Log.w(TAG, "stopLiveview IOException: " + e.getMessage());
				} finally
				{
					onRequestResult();
				}
			}
		});
		sendRequest();
	}

	static int	previewImagesCount	= 0;

	public static void onPreviewFrame(byte[] jpegData)
	{
		previewImagesCount++;
		if (!opening)
		{
			int frame = ImageConversion.JpegConvert(jpegData, previewWidth, previewHeight, false, false, 0);
			int frameLen = previewWidth * previewHeight + 2 * ((previewWidth + 1) / 2) * ((previewHeight + 1) / 2);
			final byte[] data = SwapHeap.SwapFromHeap(frame, frameLen);

			UIhandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					pluginManager.onPreviewFrame(data);
				}
			});
		}

		if (requestQueue.size() == 0 && opening && previewImagesCount >= 3)
		{
			if (progress != null)
			{
				progress.dismiss();
			}
			opening = false;
			messageHandler.sendEmptyMessage(ApplicationInterface.MSG_SURFACE_READY);
		}
	}

	/**
	 * Retrieve a list of APIs that are available at present.
	 * 
	 * @param replyJson
	 */
	private static void loadAvailableCameraApiList(JSONObject replyJson)
	{
		synchronized (mAvailableCameraApiSet)
		{
			mAvailableCameraApiSet.clear();
			try
			{
				JSONArray resultArrayJson = replyJson.getJSONArray("result");
				JSONArray apiListJson = resultArrayJson.getJSONArray(0);
				for (int i = 0; i < apiListJson.length(); i++)
				{
					mAvailableCameraApiSet.add(apiListJson.getString(i));
				}
			} catch (JSONException e)
			{
				Log.w(TAG, "loadAvailableCameraApiList: JSON format error.");
			}
		}
	}

	/**
	 * Retrieve a list of APIs that are available by the target device.
	 * 
	 * @param replyJson
	 */
	private static void loadAvailableApiList(JSONObject replyJson)
	{
		synchronized (mAvailableApiSet)
		{
			try
			{
				JSONArray resultArrayJson = replyJson.getJSONArray("results");
				for (int i = 0; i < resultArrayJson.length(); i++)
				{
					mAvailableApiSet.add(resultArrayJson.getJSONArray(i).getString(0));
				}
			} catch (JSONException e)
			{
				Log.w(TAG, "loadAvailableApiList: JSON format error.");
			}
		}
	}

	/**
	 * Check if the version of the server is available in this application.
	 * 
	 * @param replyJson
	 * @return
	 */
	private static boolean isAvailableServerVersion(JSONObject replyJson)
	{
		try
		{
			JSONArray resultArrayJson = replyJson.getJSONArray("result");
			String version = resultArrayJson.getString(1);
			String[] separated = version.split("\\.");
			int major = Integer.valueOf(separated[0]);
			if (2 <= major)
			{
				return true;
			}
		} catch (JSONException e)
		{
			Log.w(TAG, "isAvailableServerVersion: JSON format error.");
		} catch (NumberFormatException e)
		{
			Log.w(TAG, "isAvailableServerVersion: Number format error.");
		}
		return false;
	}

	/**
	 * Check if the specified API is available. This is for camera and avContent
	 * service API. The result of this method does not change dynamically.
	 * 
	 * @param apiName
	 * @return
	 */
	private static boolean isApiAvailable(String apiName)
	{
		boolean isAvailable = false;
		synchronized (mAvailableApiSet)
		{
			isAvailable = mAvailableApiSet.contains(apiName);
		}
		return isAvailable;
	}

	/**
	 * Check if the specified API is available at present. This works correctly
	 * only for Camera API.
	 * 
	 * @param apiName
	 * @return
	 */
	private static boolean isCameraApiAvailable(String apiName)
	{
		boolean isAvailable = false;
		synchronized (mAvailableCameraApiSet)
		{
			isAvailable = mAvailableCameraApiSet.contains(apiName);
		}
		return isAvailable;
	}

	/**
	 * Sets a target ServerDevice object.
	 * 
	 * @param device
	 */
	public static void setTargetServerDevice(ServerDevice device)
	{
		mTargetDevice = device;
	}

	/**
	 * Returns a target ServerDevice object.
	 * 
	 * @return return ServiceDevice
	 */
	public static ServerDevice getTargetServerDevice()
	{
		return mTargetDevice;
	}

	/**
	 * Sets a SimpleRemoteApi object to transmit to Activity.
	 * 
	 * @param remoteApi
	 */
	public static void setRemoteApi(SimpleRemoteApi remoteApi)
	{
		mRemoteApi = remoteApi;
	}

	/**
	 * Returns a SimpleRemoteApi object.
	 * 
	 * @return return SimpleRemoteApi
	 */
	public static SimpleRemoteApi getRemoteApi()
	{
		return mRemoteApi;
	}

	/**
	 * Sets a List of available APIs.
	 * 
	 * @param apiList
	 */
	public static void setAvailableApiList(Set<String> apiList)
	{
		mAvailableApiSet = apiList;
	}

	/**
	 * Returns a list of available APIs.
	 * 
	 * @return Returns a list of available APIs.
	 */
	public static Set<String> getAvailableApiList()
	{
		return mAvailableApiSet;
	}

	public static void initRemoteCameraFeatures()
	{
		if (isCameraApiAvailable("getAvailableLiveviewSize"))
		{
			initPreviewSizeList();
		} else
		{
			mPreviewSizes.add(new CameraController.Size(640, 480));
		}

		mPictureSizes.add(new CameraController.Size(1920, 1080));
		if (isCameraApiAvailable("getAvailableStillSize"))
		{
			initPictureSizeList();
		}

		if (isCameraApiAvailable("getAvailableExposureCompensation"))
		{
			initExposureCompensationAvailable();
		}

		if (isCameraApiAvailable("getAvailableFocusMode"))
		{
			initAvailableFocusMode();
		}

		if (isCameraApiAvailable("getAvailableIsoSpeedRate"))
		{
			initAvailableIsoMode();
		}

		if (isCameraApiAvailable("getAvailableWhiteBalance"))
		{
			initAvailableWhiteBalance();
		}

		if (isCameraApiAvailable("getAvailableFlashMode"))
		{
			initAvailableFlashMode();
		}
	}

	public static List<CameraController.Size> getPreviewSizeListRemote()
	{
		if (mPreviewSizes == null || mPreviewSizes.size() == 0)
		{
			mPreviewSizes = new ArrayList<CameraController.Size>();
			mPreviewSizes.add(new CameraController.Size(640, 480));
		}
		return mPreviewSizes;
	}

	public static List<CameraController.Size> initPreviewSizeList()
	{
		mPreviewSizes = new ArrayList<CameraController.Size>();
		mPreviewSizes.add(new CameraController.Size(640, 480));

		if (mRemoteApi == null) {
			return mPreviewSizes;
		}
		
		JSONObject replyJson = null;
		try
		{
			replyJson = mRemoteApi.getAvailableLiveviewSize();
			JSONArray resultArrayJson = replyJson.getJSONArray("result");
			JSONArray availableLiveviewArrayJson = resultArrayJson.getJSONArray(1);
			for (int i = 0; i < availableLiveviewArrayJson.length(); i++)
			{
				if (availableLiveviewArrayJson.getString(i).equals("L"))
				{
					CameraController.Size size = new CameraController.Size(1024, 768);
					mPreviewSizes.add(size);
				}

				if (availableLiveviewArrayJson.getString(i).equals("M"))
				{
					CameraController.Size size = new CameraController.Size(640, 480);
					mPreviewSizes.add(size);
				}
			}
		} catch (JSONException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		return mPreviewSizes;
	}

	private static CameraController.Size convertJsonToSize(JSONObject json) throws NumberFormatException, JSONException
	{
		CameraController.Size size = null;

		long totalPixelCount = Long.valueOf(json.getString("size").replace("M", "")) * 1000000;

		int horizontalRation = Integer.valueOf(json.getString("aspect").replaceAll(":.*", ""));
		int verticalRation = Integer.valueOf(json.getString("aspect").replaceAll(".*:", ""));

		double horizontalToVerticalRation = (double) ((double) horizontalRation / (double) verticalRation);
		double verticalToHorizontalRation = (double) ((double) verticalRation / (double) horizontalRation);

		int width = (int) Math.sqrt(totalPixelCount * horizontalToVerticalRation);
		int height = (int) Math.sqrt(totalPixelCount * verticalToHorizontalRation);

		size = new CameraController.Size(width, height);

		mPictureSizeNames.put((long) (width * height), json);

		return size;
	}

	public static void initPictureSizeList()
	{
		synchronized (mPictureSizes)
		{

			mPictureSizes = new ArrayList<CameraController.Size>();
			mPictureSizes.add(new CameraController.Size(1920, 1080));
			
			if (mRemoteApi == null) {
				return;
			}
			
			JSONObject replyJson = null;
			try
			{
				replyJson = mRemoteApi.getAvailableStillSize();
				JSONArray resultArrayJson = replyJson.getJSONArray("result");
				JSONArray availableStillArrayJson = resultArrayJson.getJSONArray(1);
				if (availableStillArrayJson.length() > 0)
				{
					mPictureSizes.clear();
					mPictureSizeNames.clear();
				}
				for (int i = 0; i < availableStillArrayJson.length(); i++)
				{
					CameraController.Size size = convertJsonToSize(availableStillArrayJson.getJSONObject(i));
					if (size != null)
					{
						mPictureSizes.add(size);
					}
				}
			} catch (JSONException e)
			{
				e.printStackTrace();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public static void fillPictureSizeListRemote(List<CameraController.Size> pictureSizes)
	{
		synchronized (mPictureSizes)
		{
			pictureSizes.clear();
			pictureSizes.addAll(mPictureSizes);
		}
	}

	public static List<CameraController.Size> getPictureSizeListRemote()
	{
		synchronized (mPictureSizes)
		{
			if (mPictureSizes.size() == 0)
			{
				mPictureSizes.add(new CameraController.Size(1920, 1080));
			}
			return mPictureSizes;
		}
	}

	public static void initExposureCompensationAvailable()
	{
		if (mRemoteApi == null) {
			return;
		}
		
		JSONObject replyJson = null;
		try
		{
			replyJson = mRemoteApi.getAvailableExposureCompensation();

			JSONArray resultArrayJson = replyJson.getJSONArray("result");
			maxExpoCompensation = resultArrayJson.getInt(1);
			minExpoCompensation = resultArrayJson.getInt(2);

			int step = resultArrayJson.getInt(3);
			if (step == 1)
			{
				expoCompensationStep = 1f / 3f;
			}
			if (step == 2)
			{
				expoCompensationStep = 1f / 2f;
			}
		} catch (JSONException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static boolean isExposureCompensationAvailable()
	{
		boolean res = false;

		if (minExpoCompensation != maxExpoCompensation && expoCompensationStep != 0)
		{
			res = true;
		}

		return res;
	}

	public static int getMinExposureCompensationRemote()
	{
		return minExpoCompensation;
	}

	public static int getMaxExposureCompensationRemote()
	{
		return maxExpoCompensation;
	}

	public static float getExposureCompensationStepRemote()
	{
		return expoCompensationStep;
	}

	public static String getWhiteBalanceRemote()
	{
		return currentWBMode;
	}

	public static String getIsoModeRemote()
	{
		return currentIsoMode;
	}

	public static String getFlashModeRemote()
	{
		return currentFlashMode;
	}

	public static int getExposureCompensationRemote()
	{
		return currentExposureCompensation;
	}

	public static List<String> getAvailableWhiteBalanceRemote()
	{
		return availableWBModes;
	}

	public static void initAvailableWhiteBalance()
	{
		
		availableWBModes = new ArrayList<String>();

		if (mRemoteApi == null) {
			return;
		}
		
		JSONObject replyJson = null;
		try
		{
			replyJson = mRemoteApi.getAvailableWhiteBalance();

			JSONArray resultArrayJson = replyJson.getJSONArray("result");

			JSONObject currentMode = resultArrayJson.getJSONObject(0);
			currentWBMode = currentMode.getString("whiteBalanceMode");

			JSONArray availableWhiteBalanceArrayJson = resultArrayJson.getJSONArray(1);
			for (int i = 0; i < availableWhiteBalanceArrayJson.length(); i++)
			{
				JSONObject wb = availableWhiteBalanceArrayJson.getJSONObject(i);
				availableWBModes.add(wb.getString("whiteBalanceMode"));
			}
		} catch (JSONException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static List<String> getAvailableFocusModeRemote()
	{
		return availableFocusModes;
	}

	public static void initAvailableFocusMode()
	{
		availableFocusModes = new ArrayList<String>();

		if (mRemoteApi == null) {
			return;
		}
		
		JSONObject replyJson = null;
		try
		{
			replyJson = mRemoteApi.getAvailableFocusMode();

			JSONArray resultArrayJson = replyJson.getJSONArray("result");
			JSONArray availableFocusModeArrayJson = resultArrayJson.getJSONArray(1);
			for (int i = 0; i < availableFocusModeArrayJson.length(); i++)
			{
				String focusMode = availableFocusModeArrayJson.getString(i);

				if (focusMode.equals("AF-S"))
				{
					focusMode = "auto";
					availableFocusModes.add(focusMode);
				}

				if (focusMode.equals("AF-C"))
				{
					focusMode = "continuous-picture";
					availableFocusModes.add(focusMode);
				}
			}
		} catch (JSONException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static boolean isFlashAvailableRemote()
	{
		return (availableFlashModes != null && availableFlashModes.size() > 1);
	}

	public static List<String> getAvailableFlashModeRemote()
	{
		return availableFlashModes;
	}

	public static void initAvailableFlashMode()
	{
		availableFlashModes = new ArrayList<String>();

		if (mRemoteApi == null) {
			return;
		}
		
		JSONObject replyJson = null;
		try
		{
			replyJson = mRemoteApi.getAvailableFlashMode();

			JSONArray resultArrayJson = replyJson.getJSONArray("result");
			JSONArray availableFlashModeArrayJson = resultArrayJson.getJSONArray(1);
			for (int i = 0; i < availableFlashModeArrayJson.length(); i++)
			{
				String flash = availableFlashModeArrayJson.getString(i);
				availableFlashModes.add(flash);
			}
		} catch (JSONException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static boolean isISOModeAvailableRemote()
	{
		return (availableIsoModes != null && availableIsoModes.size() > 1);
	}

	public static List<String> getAvailableIsoModeRemote()
	{
		return availableIsoModes;
	}

	public static void initAvailableIsoMode()
	{
		availableIsoModes = new ArrayList<String>();

		if (mRemoteApi == null) {
			return;
		}
		
		JSONObject replyJson = null;
		try
		{
			replyJson = mRemoteApi.getAvailableIsoSpeedRate();

			JSONArray resultArrayJson = replyJson.getJSONArray("result");
			JSONArray availableISOModeArrayJson = resultArrayJson.getJSONArray(1);
			for (int i = 0; i < availableISOModeArrayJson.length(); i++)
			{
				String iso = availableISOModeArrayJson.getString(i);
				availableIsoModes.add(iso);
			}
		} catch (JSONException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void setExposureCompensationRemote(final int value)
	{
		if (isCameraApiAvailable("setExposureCompensation"))
		{
			requestQueue.add(new Thread()
			{
				@Override
				public void run()
				{
					if (mRemoteApi == null) {
						onRequestResult();
						return;
					}
					
					try
					{
						mRemoteApi.setExposureCompensation(value);
						currentExposureCompensation = value;
					} catch (IOException e)
					{
						e.printStackTrace();
					} finally
					{
						onRequestResult();
					}
				}
			});
			sendRequest();
		}
	}

	public static void setFlashModeRemote(final String value)
	{
		if (isCameraApiAvailable("setFlashMode"))
		{
			requestQueue.add(new Thread()
			{
				@Override
				public void run()
				{
					if (mRemoteApi == null) {
						onRequestResult();
						return;
					}
					
					try
					{
						mRemoteApi.setFlashMode(value.toLowerCase());
						currentFlashMode = value;
					} catch (IOException e)
					{
						e.printStackTrace();
					} finally
					{
						onRequestResult();
					}
				}
			});
			sendRequest();
		}
	}

	public static void setIsoSpeedRateRemote(final String value)
	{
		if (isCameraApiAvailable("setIsoSpeedRate"))
		{
			requestQueue.add(new Thread()
			{
				@Override
				public void run()
				{
					if (mRemoteApi == null) {
						onRequestResult();
						return;
					}
					
					try
					{
						mRemoteApi.setIsoSpeedRate(value.toUpperCase());
						currentIsoMode = value;
					} catch (IOException e)
					{
						e.printStackTrace();
					} finally
					{
						onRequestResult();
					}
				}
			});
			sendRequest();
		}
	}

	public static void setWhiteBalanceRemote(final String value)
	{
		if (isCameraApiAvailable("setWhiteBalance"))
		{
			requestQueue.add(new Thread()
			{
				@Override
				public void run()
				{
					if (mRemoteApi == null) {
						onRequestResult();
						return;
					}
					
					try
					{
						mRemoteApi.setWhiteBalance(value);
						currentWBMode = value;
					} catch (IOException e)
					{
						e.printStackTrace();
					} finally
					{
						onRequestResult();
					}
				}
			});
			sendRequest();
		}
	}

	public static void setPictureSizeRemote(int width, int height)
	{
		final JSONObject jsonObject = mPictureSizeNames.get((long) (width * height));
		if (jsonObject == null)
		{
			return;
		}
		if (isCameraApiAvailable("setStillSize"))
		{
			requestQueue.add(new Thread()
			{
				@Override
				public void run()
				{
					if (mRemoteApi == null) {
						onRequestResult();
						return;
					}
					
					try
					{
						mRemoteApi.setStillSize(jsonObject.getString("aspect"), jsonObject.getString("size"));
						mRemoteApi.setPostviewImageSize("Original");
					} catch (IOException e)
					{
						e.printStackTrace();
					} catch (JSONException e)
					{
						e.printStackTrace();
					} finally
					{
						onRequestResult();
					}
				}
			});
			sendRequest();
		}
	}
}
