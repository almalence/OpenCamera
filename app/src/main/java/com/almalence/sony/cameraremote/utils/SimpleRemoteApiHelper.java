/*
 * Copyright 2014 Sony Corporation
 */

package com.almalence.sony.cameraremote.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.almalence.sony.cameraremote.SimpleRemoteApi;

public final class SimpleRemoteApiHelper {

    private static final String TAG = SimpleRemoteApiHelper.class.getSimpleName();

    private SimpleRemoteApiHelper() {

    }

    /**
     * Prepare request params and calls SimpleRemoteApi method to get date list
     * of storage contents. Request JSON data is such like as below.
     * 
     * <pre>
     * {
     *   "method": "getContentList",
     *   "params": [{
     *      "sort" : "ascending"
     *      "view": "date"
     *      "uri": "storage:memoryCard1"
     *      }],
     *   "id": 2,
     *   "version": "1.3"
     * }
     * </pre>
     * 
     * @param object of SimpleRemoteAPi
     * @return JSON data of response
     * @throws IOException all errors and exception are wrapped by this
     *             Exception.
     */
    public static JSONObject getContentDateList(SimpleRemoteApi simpleRemoteApi) throws IOException {

        try {
            List<String> uri = getSupportedStorages(simpleRemoteApi);

            if (uri == null) {
                Log.w(TAG, "supported Uri is null");
                throw new IOException();
            }

            JSONObject replyJson = null;
            JSONObject paramObj = new JSONObject().put("sort", "ascending").put("view", "date")
                    .put("uri", uri.get(0));
            JSONArray params = new JSONArray().put(0, paramObj);

            replyJson = simpleRemoteApi.getContentList(params);
            return replyJson;

        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Prepare request params and calls SimpleRemoteApi method to get contents
     * list of storage. Request JSON data is such like as below.
     * 
     * <pre>
     * {
     *   "method": "getContentList",
     *   "params": [{
     *      "sort" : "ascending"
     *      "view": "date"
     *      "type" : [
     *          "still",
     *          "movie_mp4",
     *          "movie_xavcs"
     *       ],
     *      "uri": "storage:memoryCard1?path=2014-03-31"
     *      }],
     *   "id": 2,
     *   "version": "1.3"
     * }
     * </pre>
     * 
     * @param simpleRemoteApi object of simpleRemoteApi
     * @param uri uri of target date
     * @param isStreamSupported set true if target device supported streaming
     *            playback
     * @return JSON data of response
     * @throws IOException IOException all errors and exception are wrapped by
     *             this Exception.
     */
    public static JSONObject
            getContentListOfDay(SimpleRemoteApi simpleRemoteApi, String uri, //
                    Boolean isStreamSupported) throws IOException {

        try {
            JSONObject replyJson = null;
            JSONArray typeParam;
            if (isStreamSupported) {
                // Device supports streaming API.
                // get still and movie contents.
                typeParam = new JSONArray().put("still").put("movie_mp4").put("movie_xavcs");
            } else {
                // Device does not support streaming API.
                // get only still contents.
                typeParam = new JSONArray().put("still");
            }
            JSONObject paramObj =
                    new JSONObject().put("sort", "ascending").put("view", "date") //
                            .put("type", typeParam).put("uri", uri);
            JSONArray params = new JSONArray().put(0, paramObj);

            replyJson = simpleRemoteApi.getContentList(params);
            return replyJson;

        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private static List<String> getSupportedStorages(SimpleRemoteApi simpleRemoteApi) //
            throws IOException, JSONException {

        // Confirm Scheme
        JSONObject replyJsonScheme = simpleRemoteApi.getSchemeList();

        if (SimpleRemoteApi.isErrorReply(replyJsonScheme)) {
            JSONArray resultsObjScheme = replyJsonScheme.getJSONArray("error");
            int resultCode = resultsObjScheme.getInt(0);
            Log.w(TAG, "getSchemeList Error:" + resultCode);
            throw new IOException();
        }

        Set<String> schemeSet = new HashSet<String>();
        JSONArray resultsObjScheme = replyJsonScheme.getJSONArray("result").getJSONArray(0);

        for (int i = 0; i < resultsObjScheme.length(); i++) {
            schemeSet.add(resultsObjScheme.getJSONObject(i).getString("scheme"));
        }

        if (!schemeSet.contains("storage")) {
            Log.w(TAG, "This device does not support storage.");
            throw new IOException();
        }

        // Confirm Source
        JSONObject replyJsonSource = simpleRemoteApi.getSourceList("storage");

        if (SimpleRemoteApi.isErrorReply(replyJsonSource)) {
            JSONArray resultsObjSource = replyJsonSource.getJSONArray("error");
            int resultCode = resultsObjSource.getInt(0);
            Log.w(TAG, "getSourceList Error:" + resultCode);
            throw new IOException();
        }

        List<String> sourceList = new ArrayList<String>();
        JSONArray resultsObjSource = replyJsonSource.getJSONArray("result").getJSONArray(0);

        for (int i = 0; i < resultsObjSource.length(); i++) {
            sourceList.add(resultsObjSource.getJSONObject(i).getString("source"));
        }

        return sourceList;
    }

}
