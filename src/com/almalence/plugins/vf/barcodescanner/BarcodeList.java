package com.almalence.plugins.vf.barcodescanner;

import android.app.ListActivity;
import android.os.Bundle;

import com.almalence.opencam.R;

public class BarcodeList extends ListActivity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.plugin_vf_barcodescanner_list_layout);
		
		BarcodeArrayAdapter adapter = new BarcodeArrayAdapter(this, BarcodeStorageHelper.getBarcodesList());
		setListAdapter(adapter);
	}

}
