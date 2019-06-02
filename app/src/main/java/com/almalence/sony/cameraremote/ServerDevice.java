package com.almalence.sony.cameraremote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.util.Log;

import com.almalence.sony.cameraremote.utils.SimpleHttpClient;
import com.almalence.sony.cameraremote.utils.XmlElement;

/**
 * A server device description class.
 */
public final class ServerDevice {

    private static final String TAG = ServerDevice.class.getSimpleName();

    /**
     * Camera Remote API service (category). For example, "camera", "guide" and
     * so on. "Action List URL" is API request target URL of each service.
     */
    public static class ApiService {
        private String mName;

        private String mActionListUrl;

        /**
         * Constructor
         * 
         * @param name category name
         * @param actionListUrl action list URL of the category
         */
        public ApiService(String name, String actionListUrl) {
            mName = name;
            mActionListUrl = actionListUrl;
        }

        /**
         * Returns the category name.
         * 
         * @return category name.
         */
        public String getName() {
            return mName;
        }

        /**
         * Sets a category name.
         * 
         * @param name category name
         */
        public void setName(String name) {
            this.mName = name;
        }

        /**
         * Returns the action list URL of the category.
         * 
         * @return action list URL
         */
        public String getActionListUrl() {
            return mActionListUrl;
        }

        /**
         * Sets an action list URL of the category.
         * 
         * @param actionListUrl action list URL of the category
         */
        public void setActionListUrl(String actionListUrl) {
            this.mActionListUrl = actionListUrl;
        }

        /**
         * Returns the endpoint URL of the category.
         * 
         * @return endpoint URL
         */
        public String getEndpointUrl() {
            String url = null;
            if (mActionListUrl == null || mName == null) {
                url = null;
            } else if (mActionListUrl.endsWith("/")) {
                url = mActionListUrl + mName;
            } else {
                url = mActionListUrl + "/" + mName;
            }
            return url;
        }
    }

    private String mDDUrl;

    private String mFriendlyName;

    private String mModelName;

    private String mUDN;

    private String mIconUrl;

    private final List<ApiService> mApiServices;

    private ServerDevice() {
        mApiServices = new ArrayList<ServerDevice.ApiService>();
    }

    /**
     * Returns URL of Device Description XML
     * 
     * @return URL string
     */
    public String getDDUrl() {
        return mDDUrl;
    }

    /**
     * Returns a value of friendlyName in DD.
     * 
     * @return friendlyName
     */
    public String getFriendlyName() {
        return mFriendlyName;
    }

    /**
     * Returns a value of modelName in DD.
     *
     * @return modelName
     */
    public String getModelName() {
        return mModelName;
    }

    /**
     * Returns a value of UDN in DD.
     * 
     * @return UDN
     */
    public String getUDN() {
        return mUDN;
    }

    /**
     * Returns URL of icon in DD.
     * 
     * @return URL of icon
     */
    public String getIconUrl() {
        return mIconUrl;
    }

    /**
     * Returns IP address of the DD.
     * 
     * @return IP address
     */
    public String getIpAddres() {
        String ip = null;
        if (mDDUrl != null) {
            return toHost(mDDUrl);
        }
        return ip;
    }

    /**
     * Returns a list of categories that the server supports.
     * 
     * @return a list of categories
     */
    public List<ApiService> getApiServices() {
        return Collections.unmodifiableList(mApiServices);
    }

    /**
     * Checks to see whether the server supports the category.
     * 
     * @param serviceName category name
     * @return true if it's supported.
     */
    public boolean hasApiService(String serviceName) {
        if (serviceName == null) {
            return false;
        }
        for (ApiService apiService : mApiServices) {
            if (serviceName.equals(apiService.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a ApiService object.
     * 
     * @param serviceName category name
     * @return ApiService object
     */
    public ApiService getApiService(String serviceName) {
        if (serviceName == null) {
            return null;
        }
        for (ApiService apiService : mApiServices) {
            if (serviceName.equals(apiService.getName())) {
                return apiService;
            }
        }
        return null;
    }

    /**
     * Adds a ApiService object.
     * 
     * @param name
     * @param actionUrl
     */
    private void addApiService(String name, String actionUrl) {
        ApiService service = new ApiService(name, actionUrl);
        mApiServices.add(service);
    }

    /**
     * Fetches device description xml file from server and parses it.
     * 
     * @param ddUrl URL of device description xml.
     * @return ServerDevice instance
     */
    public static ServerDevice fetch(String ddUrl) {
        if (ddUrl == null) {
            throw new NullPointerException("ddUrl is null.");
        }

        String ddXml = "";
        try {
            ddXml = SimpleHttpClient.httpGet(ddUrl);
            Log.d(TAG, "fetch () httpGet done.");
        } catch (IOException e) {
            Log.e(TAG, "fetch: IOException.", e);
            return null;
        }

        XmlElement rootElement = XmlElement.parse(ddXml);

        // "root"
        ServerDevice device = null;
        if ("root".equals(rootElement.getTagName())) {
            device = new ServerDevice();
            device.mDDUrl = ddUrl;

            // "device"
            XmlElement deviceElement = rootElement.findChild("device");
            device.mFriendlyName = deviceElement.findChild("friendlyName").getValue();
            device.mModelName = deviceElement.findChild("modelName").getValue();
            device.mUDN = deviceElement.findChild("UDN").getValue();

            // "iconList"
            XmlElement iconListElement = deviceElement.findChild("iconList");
            List<XmlElement> iconElements = iconListElement.findChildren("icon");
            for (XmlElement iconElement : iconElements) {
                // Choose png icon to show Android UI.
                if ("image/png".equals(iconElement.findChild("mimetype").getValue())) {
                    String uri = iconElement.findChild("url").getValue();
                    String hostUrl = toSchemeAndHost(ddUrl);
                    device.mIconUrl = hostUrl + uri;
                }
            }

            // "av:X_ScalarWebAPI_DeviceInfo"
            XmlElement wApiElement = deviceElement.findChild("X_ScalarWebAPI_DeviceInfo");
            XmlElement wApiServiceListElement = wApiElement.findChild("X_ScalarWebAPI_ServiceList");
            List<XmlElement> wApiServiceElements = wApiServiceListElement
                    .findChildren("X_ScalarWebAPI_Service");
            for (XmlElement wApiServiceElement : wApiServiceElements) {
                String serviceName = wApiServiceElement.findChild("X_ScalarWebAPI_ServiceType")
                        .getValue();
                String actionUrl = wApiServiceElement.findChild("X_ScalarWebAPI_ActionList_URL")
                        .getValue();
                device.addApiService(serviceName, actionUrl);
            }
        }
        Log.d(TAG, "fetch () parsing XML done.");
        return device;
    }

    private static String toSchemeAndHost(String url) {
        int i = url.indexOf("://"); // http:// or https://
        if (i == -1) {
            return "";
        }

        int j = url.indexOf("/", i + 3);
        if (j == -1) {
            return "";
        }

        String hostUrl = url.substring(0, j);
        return hostUrl;
    }

    private static String toHost(String url) {
        int i = url.indexOf("://"); // http:// or https://
        if (i == -1) {
            return "";
        }

        int j = url.indexOf(":", i + 3);
        if (j == -1) {
            return "";
        }

        String host = url.substring(i + 3, j);
        return host;
    }
}
