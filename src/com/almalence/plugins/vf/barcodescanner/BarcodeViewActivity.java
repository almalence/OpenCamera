package com.almalence.plugins.vf.barcodescanner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.almalence.opencam.R;

public class BarcodeViewActivity extends Activity{
	Barcode mBarcode;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.plugin_vf_barcodescanner_view_layout);
		
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
		    mBarcode = (Barcode) extras.get("barcode");
		}
		
		TextView dataTextView = (TextView) findViewById(R.id.dataTextView);
		TextView typeTextView = (TextView) findViewById(R.id.typeTextView);
		TextView formatTextView = (TextView) findViewById(R.id.formatTextView);
		TextView timeTextView = (TextView) findViewById(R.id.timeTextView);
		
		dataTextView.setText(mBarcode.getData());
		typeTextView.setText(mBarcode.getType());
		formatTextView.setText(mBarcode.getFormat());
		timeTextView.setText(mBarcode.getDate().toString());
	}

}
