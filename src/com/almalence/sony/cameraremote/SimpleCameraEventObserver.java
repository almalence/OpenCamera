package com.almalence.sony.cameraremote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple observer class for some status values in Camera. This class supports
 * only a few of values of getEvent result, so please add implementation for the
 * rest of values you want to handle.
 */
public class SimpleCameraEventObserver
{

	private static final String	TAG	= SimpleCameraEventObserver.class.getSimpleName();

	/**
	 * A listener interface to receive these changes. These methods will be
	 * called by UI thread.
	 */
	public interface ChangeListener
	{

		/**
		 * Called when the list of available APIs is modified.
		 * 
		 * @param apis
		 *            a list of available APIs
		 */
		void onApiListModified(List<String> apis);

		/**
		 * Called when the value of "Camera Status" is changed.
		 * 
		 * @param status
		 *            camera status (ex."IDLE")
		 */
		void onCameraStatusChanged(String status);

		/**
		 * Called when the value of "Liveview Status" is changed.
		 * 
		 * @param status
		 *            liveview status (ex.true)
		 */
		void onLiveviewStatusChanged(boolean status);

		/**
		 * Called when the value of "Shoot Mode" is changed.
		 * 
		 * @param shootMode
		 *            shoot mode (ex."still")
		 */
		void onShootModeChanged(String shootMode);

		/**
		 * Called when the value of "zoomPosition" is changed.
		 * 
		 * @param zoomPosition
		 *            zoom position (ex.12)
		 */
		void onZoomPositionChanged(int zoomPosition);

		/**
		 * Called when the value of "storageId" is changed.
		 * 
		 * @param storageId
		 *            storageId (ex. "Memory Card 1")
		 */
		void onStorageIdChanged(String storageId);

		/**
		 * Called when the value of "takePicture" is changed.
		 * 
		 * @param takePictureUrl
		 *            takePictureUrl
		 */
		void onPictureTaken(String takePictureUrl);

		// :
		// : add methods for Event data as necessary.
	}

	/**
	 * Abstract class to receive these changes. please override methods that you
	 * need.
	 */
	public abstract static class ChangeListenerTmpl implements ChangeListener
	{

		@Override
		public void onApiListModified(List<String> apis)
		{
		}

		@Override
		public void onCameraStatusChanged(String status)
		{
		}

		@Override
		public void onLiveviewStatusChanged(boolean status)
		{
		}

		@Override
		public void onShootModeChanged(String shootMode)
		{
		}

		@Override
		public void onZoomPositionChanged(int zoomPosition)
		{
		}

		@Override
		public void onStorageIdChanged(String storageId)
		{
		}

		@Override
		public void onPictureTaken(String takePictureUrl)
		{
		}

	}

	private final Handler	mUiHandler;

	private SimpleRemoteApi	mRemoteApi;

	private ChangeListener	mListener;

	private boolean			mWhileEventMonitoring	= false;

	private boolean			mIsActive				= false;

	// Current Camera Status value.
	private String			mCameraStatus;

	// Current Liveview Status value.
	private boolean			mLiveviewStatus;

	// Current Shoot Mode value.
	private String			mShootMode;

	// Current Zoom Position value.
	private int				mZoomPosition;

	// Current Storage Id value.
	private String			mStorageId;

	// :
	// : add attributes for Event data as necessary.

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            context to notify the changes by UI thread.
	 * @param apiClient
	 *            API client
	 */
	public SimpleCameraEventObserver(Context context, SimpleRemoteApi apiClient)
	{
		if (context == null)
		{
			throw new IllegalArgumentException("context is null.");
		}
		if (apiClient == null)
		{
			throw new IllegalArgumentException("apiClient is null.");
		}
		mRemoteApi = apiClient;
		mUiHandler = new Handler(context.getMainLooper());
	}

	/**
	 * Starts monitoring by continuously calling getEvent API.
	 * 
	 * @return true if it successfully started, false if a monitoring is already
	 *         started.
	 */
	public boolean start()
	{
		if (!mIsActive)
		{
			Log.w(TAG, "start() observer is not active.");
			return false;
		}

		if (mWhileEventMonitoring)
		{
			Log.w(TAG, "start() already starting.");
			return false;
		}

		mWhileEventMonitoring = true;
		new Thread()
		{

			@Override
			public void run()
			{
				Log.d(TAG, "start() exec.");
				// Call getEvent API continuously.
				boolean firstCall = true;
				MONITORLOOP: while (mWhileEventMonitoring)
				{

					// At first, call as non-Long Polling.
					boolean longPolling = !firstCall;

					try
					{
						// Call getEvent API.
						JSONObject replyJson = mRemoteApi.getEvent(longPolling);

						// Check error code at first.
						int errorCode = findErrorCode(replyJson);
						Log.d(TAG, "getEvent errorCode: " + errorCode);
						switch (errorCode)
						{
						case 0: // no error
							// Pass through.
							break;
						case 1: // "Any" error
						case 12: // "No such method" error
							break MONITORLOOP; // end monitoring.
						case 2: // "Timeout" error
							// Re-call immediately.
							continue MONITORLOOP;
						case 40402: // "Already polling" error
							// Retry after 5 sec.
							try
							{
								Thread.sleep(5000);
							} catch (InterruptedException e)
							{
								// do nothing.
							}
							continue MONITORLOOP;
						default:
							Log.w(TAG, "SimpleCameraEventObserver: Unexpected error: " + errorCode);
							break MONITORLOOP; // end monitoring.
						}

						List<String> availableApis = findAvailableApiList(replyJson);
						if (!availableApis.isEmpty())
						{
							fireApiListModifiedListener(availableApis);
						}

						// CameraStatus
						String cameraStatus = findCameraStatus(replyJson);
						Log.d(TAG, "getEvent cameraStatus: " + cameraStatus);
						if (cameraStatus != null && !cameraStatus.equals(mCameraStatus))
						{
							mCameraStatus = cameraStatus;
							fireCameraStatusChangeListener(cameraStatus);
						}

						// LiveviewStatus
						Boolean liveviewStatus = findLiveviewStatus(replyJson);
						Log.d(TAG, "getEvent liveviewStatus: " + liveviewStatus);
						if (liveviewStatus != null && !liveviewStatus.equals(mLiveviewStatus))
						{
							mLiveviewStatus = liveviewStatus;
							fireLiveviewStatusChangeListener(liveviewStatus);
						}

						// ShootMode
						String shootMode = findShootMode(replyJson);
						Log.d(TAG, "getEvent shootMode: " + shootMode);
						if (shootMode != null && !shootMode.equals(mShootMode))
						{
							mShootMode = shootMode;
							fireShootModeChangeListener(shootMode);
						}

						// zoomPosition
						int zoomPosition = findZoomInformation(replyJson);
						Log.d(TAG, "getEvent zoomPosition: " + zoomPosition);
						if (zoomPosition != -1)
						{
							mZoomPosition = zoomPosition;
							fireZoomInformationChangeListener(0, 0, zoomPosition, 0);
						}

						// storageId
						String storageId = findStorageId(replyJson);
						Log.d(TAG, "getEvent storageId:" + storageId);
						if (storageId != null && !storageId.equals(mStorageId))
						{
							mStorageId = storageId;
							fireStorageIdChangeListener(storageId);
						}

						// takePictureUrl
						String takePictureUrl = findTakePictureUrl(replyJson);
						Log.d(TAG, "getEvent takePictureUrl:" + takePictureUrl);
						if (takePictureUrl != null)
						{
							firePictureTakenListener(takePictureUrl);
						}
						// :
						// : add implementation for Event data as necessary.

					} catch (IOException e)
					{
						// Occurs when the server is not available now.
						Log.d(TAG, "getEvent timeout by client trigger.");
						break MONITORLOOP;
					} catch (JSONException e)
					{
						Log.w(TAG, "getEvent: JSON format error. " + e.getMessage());
						break MONITORLOOP;
					}

					firstCall = false;
				} // MONITORLOOP end.

				mWhileEventMonitoring = false;
			}
		}.start();

		return true;
	}

	/**
	 * Requests to stop the monitoring.
	 */
	public void stop()
	{
		mWhileEventMonitoring = false;
	}

	/**
	 * Requests to release resource.
	 */
	public void release()
	{
		mWhileEventMonitoring = false;
		mIsActive = false;
	}

	public void activate()
	{
		mIsActive = true;
	}

	/**
	 * Checks to see whether a monitoring is already started.
	 * 
	 * @return true when monitoring is started.
	 */
	public boolean isStarted()
	{
		return mWhileEventMonitoring;
	}

	/**
	 * Sets a listener object.
	 * 
	 * @param listener
	 */
	public void setEventChangeListener(ChangeListener listener)
	{
		mListener = listener;
	}

	/**
	 * Clears a listener object.
	 */
	public void clearEventChangeListener()
	{
		mListener = null;
	}

	/**
	 * Returns the current Camera Status value.
	 * 
	 * @return camera status
	 */
	public String getCameraStatus()
	{
		return mCameraStatus;
	}

	/**
	 * Returns the current Camera Status value.
	 * 
	 * @return camera status
	 */
	public boolean getLiveviewStatus()
	{
		return mLiveviewStatus;
	}

	/**
	 * Returns the current Shoot Mode value.
	 * 
	 * @return shoot mode
	 */
	public String getShootMode()
	{
		return mShootMode;
	}

	/**
	 * Returns the current Zoom Position value.
	 * 
	 * @return zoom position
	 */
	public int getZoomPosition()
	{
		return mZoomPosition;
	}

	/**
	 * Returns the current Storage Id value.
	 * 
	 * @return
	 */
	public String getStorageId()
	{
		return mStorageId;
	}

	/**
	 * Notify the change of available APIs
	 * 
	 * @param availableApis
	 */
	private void fireApiListModifiedListener(final List<String> availableApis)
	{
		mUiHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (mListener != null)
				{
					mListener.onApiListModified(availableApis);
				}
			}
		});
	}

	/**
	 * Notify the change of Camera Status.
	 * 
	 * @param status
	 */
	private void fireCameraStatusChangeListener(final String status)
	{
		mUiHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (mListener != null)
				{
					mListener.onCameraStatusChanged(status);
				}
			}
		});
	}

	/**
	 * Notify the change of Liveview Status.
	 * 
	 * @param status
	 */
	private void fireLiveviewStatusChangeListener(final boolean status)
	{
		mUiHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (mListener != null)
				{
					mListener.onLiveviewStatusChanged(status);
				}
			}
		});
	}

	/**
	 * Notify the change of Shoot Mode.
	 * 
	 * @param shootMode
	 */
	private void fireShootModeChangeListener(final String shootMode)
	{
		mUiHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (mListener != null)
				{
					mListener.onShootModeChanged(shootMode);
				}
			}
		});
	}

	/**
	 * Notify the change of Zoom Information
	 * 
	 * @param zoomIndexCurrentBox
	 * @param zoomNumberBox
	 * @param zoomPosition
	 * @param zoomPositionCurrentBox
	 */
	private void fireZoomInformationChangeListener(final int zoomIndexCurrentBox, final int zoomNumberBox,
			final int zoomPosition, final int zoomPositionCurrentBox)
	{
		mUiHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (mListener != null)
				{
					mListener.onZoomPositionChanged(zoomPosition);
				}
			}
		});
	}

	/**
	 * Notify the change of Storage Id.
	 * 
	 * @param storageId
	 */
	private void fireStorageIdChangeListener(final String storageId)
	{
		mUiHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (mListener != null)
				{
					mListener.onStorageIdChanged(storageId);
				}
			}
		});
	}

	/**
	 * Notify the picture taken.
	 * 
	 * @param storageId
	 */
	private void firePictureTakenListener(final String takePictureUrl)
	{
		mUiHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (mListener != null)
				{
					mListener.onPictureTaken(takePictureUrl);
				}
			}
		});
	}

	/**
	 * Finds and extracts an error code from reply JSON data.
	 * 
	 * @param replyJson
	 * @return
	 * @throws JSONException
	 */
	private static int findErrorCode(JSONObject replyJson) throws JSONException
	{
		int code = 0; // 0 means no error.
		if (replyJson.has("error"))
		{
			JSONArray errorObj = replyJson.getJSONArray("error");
			code = errorObj.getInt(0);
		}
		return code;
	}

	/**
	 * Finds and extracts a list of available APIs from reply JSON data. As for
	 * getEvent v1.0, results[0] => "availableApiList"
	 * 
	 * @param replyJson
	 * @return
	 * @throws JSONException
	 */
	private static List<String> findAvailableApiList(JSONObject replyJson) throws JSONException
	{
		List<String> availableApis = new ArrayList<String>();
		int indexOfAvailableApiList = 0;
		JSONArray resultsObj = replyJson.getJSONArray("result");
		if (!resultsObj.isNull(indexOfAvailableApiList))
		{
			JSONObject availableApiListObj = resultsObj.getJSONObject(indexOfAvailableApiList);
			String type = availableApiListObj.getString("type");
			if ("availableApiList".equals(type))
			{
				JSONArray apiArray = availableApiListObj.getJSONArray("names");
				for (int i = 0; i < apiArray.length(); i++)
				{
					availableApis.add(apiArray.getString(i));
				}
			} else
			{
				Log.w(TAG, "Event reply: Illegal Index (0: AvailableApiList) " + type);
			}
		}
		return availableApis;
	}

	/**
	 * Finds and extracts a value of Camera Status from reply JSON data. As for
	 * getEvent v1.0, results[1] => "cameraStatus"
	 * 
	 * @param replyJson
	 * @return
	 * @throws JSONException
	 */
	private static String findCameraStatus(JSONObject replyJson) throws JSONException
	{
		String cameraStatus = null;
		int indexOfCameraStatus = 1;
		JSONArray resultsObj = replyJson.getJSONArray("result");
		if (!resultsObj.isNull(indexOfCameraStatus))
		{
			JSONObject cameraStatusObj = resultsObj.getJSONObject(indexOfCameraStatus);
			String type = cameraStatusObj.getString("type");
			if ("cameraStatus".equals(type))
			{
				cameraStatus = cameraStatusObj.getString("cameraStatus");
			} else
			{
				Log.w(TAG, "Event reply: Illegal Index (1: CameraStatus) " + type);
			}
		}
		return cameraStatus;
	}

	/**
	 * Finds and extracts a value of Liveview Status from reply JSON data. As
	 * for getEvent v1.0, results[3] => "liveviewStatus"
	 * 
	 * @param replyJson
	 * @return
	 * @throws JSONException
	 */
	private static Boolean findLiveviewStatus(JSONObject replyJson) throws JSONException
	{
		Boolean liveviewStatus = null;
		int indexOfLiveviewStatus = 3;
		JSONArray resultsObj = replyJson.getJSONArray("result");
		if (!resultsObj.isNull(indexOfLiveviewStatus))
		{
			JSONObject liveviewStatusObj = resultsObj.getJSONObject(indexOfLiveviewStatus);
			String type = liveviewStatusObj.getString("type");
			if ("liveviewStatus".equals(type))
			{
				liveviewStatus = liveviewStatusObj.getBoolean("liveviewStatus");
			} else
			{
				Log.w(TAG, "Event reply: Illegal Index (3: LiveviewStatus) " + type);
			}
		}
		return liveviewStatus;
	}

	/**
	 * Finds and extracts a value of Shoot Mode from reply JSON data. As for
	 * getEvent v1.0, results[21] => "shootMode"
	 * 
	 * @param replyJson
	 * @return
	 * @throws JSONException
	 */
	private static String findShootMode(JSONObject replyJson) throws JSONException
	{
		String shootMode = null;
		int indexOfShootMode = 21;
		JSONArray resultsObj = replyJson.getJSONArray("result");
		if (!resultsObj.isNull(indexOfShootMode))
		{
			JSONObject shootModeObj = resultsObj.getJSONObject(indexOfShootMode);
			String type = shootModeObj.getString("type");
			if ("shootMode".equals(type))
			{
				shootMode = shootModeObj.getString("currentShootMode");
			} else
			{
				Log.w(TAG, "Event reply: Illegal Index (21: ShootMode) " + type);
			}
		}
		return shootMode;
	}

	/**
	 * Finds and extracts a value of Zoom Information from reply JSON data. As
	 * for getEvent v1.0, results[2] => "zoomInformation"
	 * 
	 * @param replyJson
	 * @return
	 * @throws JSONException
	 */
	private static int findZoomInformation(JSONObject replyJson) throws JSONException
	{
		int zoomPosition = -1;
		int indexOfZoomInformation = 2;
		JSONArray resultsObj = replyJson.getJSONArray("result");
		if (!resultsObj.isNull(indexOfZoomInformation))
		{
			JSONObject zoomInformationObj = resultsObj.getJSONObject(indexOfZoomInformation);
			String type = zoomInformationObj.getString("type");
			if ("zoomInformation".equals(type))
			{
				zoomPosition = zoomInformationObj.getInt("zoomPosition");
			} else
			{
				Log.w(TAG, "Event reply: Illegal Index (2: zoomInformation) " + type);
			}
		}
		return zoomPosition;
	}

	/**
	 * Finds and extracts value of Storage Id from reply JSON data. As for
	 * getEvent v1.0, results[10] => "storageInformation"
	 * 
	 * @param replyJson
	 * @return
	 * @throws JSONException
	 */
	private static String findStorageId(JSONObject replyJson) throws JSONException
	{
		String storageId = null;
		int indexOfStorageInfomation = 10;
		JSONArray resultsObj = replyJson.getJSONArray("result");
		if (!resultsObj.isNull(indexOfStorageInfomation))
		{
			JSONArray storageInformationArray = resultsObj.getJSONArray(indexOfStorageInfomation);
			if (!storageInformationArray.isNull(0))
			{
				JSONObject storageInformationObj = storageInformationArray.getJSONObject(0);
				String type = storageInformationObj.getString("type");
				if ("storageInformation".equals(type))
				{
					storageId = storageInformationObj.getString("storageID");
				} else
				{
					Log.w(TAG, "Event reply: Illegal Index (11: storageInformation) " + type);
				}
			}
		}

		return storageId;
	}

	/**
	 * Finds and extracts value of picture url from reply JSON data. As for
	 * getEvent v1.0, results[5] => "takePicture"
	 * 
	 * @param replyJson
	 * @return
	 * @throws JSONException
	 */
	private static String findTakePictureUrl(JSONObject replyJson) throws JSONException
	{
		String takePictureUrl = null;
		int indexOfTakePictureUrl = 5;
		JSONArray resultsObj = replyJson.getJSONArray("result");
		if (!resultsObj.isNull(indexOfTakePictureUrl))
		{
			JSONArray takePictureArray = resultsObj.getJSONArray(indexOfTakePictureUrl);
			if (!takePictureArray.isNull(0))
			{
				JSONObject takePictureObj = takePictureArray.getJSONObject(0);
				String type = takePictureObj.getString("type");
				if ("takePicture".equals(type))
				{
					JSONArray takePictureUrlArray = takePictureObj.getJSONArray("takePictureUrl");
					if (takePictureUrlArray.length() > 0) {
						takePictureUrl = takePictureUrlArray.getString(0);
					}
				} else
				{
					Log.w(TAG, "Event reply: Illegal Index (11: takePicture) " + type);
				}
			}
		}

		return takePictureUrl;
	}
}
