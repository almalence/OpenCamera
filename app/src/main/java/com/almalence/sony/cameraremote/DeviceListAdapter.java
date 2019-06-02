package com.almalence.sony.cameraremote;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.almalence.sony.cameraremote.ServerDevice.ApiService;
/* <!-- +++
import com.almalence.opencam_plus.R;
+++ --> */
//<!-- -+-
import com.almalence.opencam.R;
//-+- -->

public class DeviceListAdapter extends BaseAdapter {

        private final List<ServerDevice> mDeviceList;

        private final LayoutInflater mInflater;

        public DeviceListAdapter(Context context) {
            mDeviceList = new ArrayList<ServerDevice>();
            mInflater = LayoutInflater.from(context);
        }

        public void addDevice(ServerDevice device) {
            mDeviceList.add(device);
            notifyDataSetChanged();
        }

        public void clearDevices() {
            mDeviceList.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mDeviceList.size();
        }

        @Override
        public Object getItem(int position) {
            return mDeviceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0; // not fine
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

        	RelativeLayout view = (RelativeLayout) convertView;
            if (view == null) {
                view = (RelativeLayout) mInflater.inflate(R.layout.gui_almalence_device_list_item, parent, false);
            }
            ServerDevice device = (ServerDevice) getItem(position);
            ApiService apiService = device.getApiService("camera");
            String endpointUrl = null;
            if (apiService != null) {
                endpointUrl = apiService.getEndpointUrl();
            }

            Button buttonView = (Button) view.findViewById(R.id.list_item_text);
            
            // Label
            String htmlLabel = device.getFriendlyName();
            buttonView.setText(Html.fromHtml(htmlLabel));
            
            return view;
        }
    }