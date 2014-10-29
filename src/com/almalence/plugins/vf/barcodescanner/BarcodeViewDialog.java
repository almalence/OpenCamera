package com.almalence.plugins.vf.barcodescanner;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;


/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.R;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.R;
//-+- -->

import com.almalence.plugins.vf.barcodescanner.result.ResultButtonListener;
import com.almalence.plugins.vf.barcodescanner.result.ResultHandler;
import com.almalence.plugins.vf.barcodescanner.result.ResultHandlerFactory;
import com.almalence.ui.RotateDialog;
import com.almalence.ui.RotateLayout;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

public class BarcodeViewDialog extends RotateDialog
{
	Barcode	barcode;

	public BarcodeViewDialog(Context context, Barcode barcode)
	{
		super(context);
		this.barcode = barcode;
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		Rect displayRectangle = new Rect();
		Window window = MainScreen.getInstance().getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);

		// inflate and adjust layout
		LayoutInflater inflater = (LayoutInflater) MainScreen.getInstance().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		layoutView = inflater.inflate(R.layout.plugin_vf_barcodescanner_view_layout, null);
		layoutView.setMinimumWidth((int) (displayRectangle.width() * 0.7f));
		layoutView.setMinimumHeight((int) (displayRectangle.height() * 0.7f));
		setContentView(layoutView);

		initControls();
	}

	private void initControls()
	{
		Result result = new Result(barcode.getData(), null, null, BarcodeFormat.valueOf(barcode.getFormat()), barcode
				.getDate().getTime());
		ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(MainScreen.getInstance(), result);

		// set the custom dialog components - text, image and button
		TextView dataTextView = (TextView) findViewById(R.id.decodedDataTextView);
		dataTextView.setText(barcode.getData());

		TextView formatTextView = (TextView) findViewById(R.id.formatTextView);
		formatTextView.setText(barcode.getFormat());

		TextView typeTextView = (TextView) findViewById(R.id.typeTextView);
		typeTextView.setText(barcode.getType());

		TextView timeTextView = (TextView) findViewById(R.id.timeTextView);
		timeTextView.setText(barcode.getDate().toString());

		ImageView barcodeImageView = (ImageView) findViewById(R.id.barcodePhotoImageView);
		File imgFile = null;
		if (barcode.getFile() != null)
		{
			imgFile = new File(barcode.getFile());
		}
		if (imgFile != null && imgFile.exists())
		{
			Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
			barcodeImageView.setImageBitmap(myBitmap);
		} else
		{
			barcodeImageView.setImageResource(R.drawable.gui_almalence_settings_scene_barcode_on);
		}

		int buttonCount = resultHandler.getButtonCount();
		ViewGroup buttonView = (ViewGroup) findViewById(R.id.result_button_view);
		buttonView.requestFocus();
		for (int x = 0; x < ResultHandler.MAX_BUTTON_COUNT; x++)
		{
			TextView button = (TextView) buttonView.getChildAt(x);
			if (x < buttonCount)
			{
				button.setVisibility(View.VISIBLE);
				button.setText(resultHandler.getButtonText(x));
				button.setOnClickListener(new ResultButtonListener(resultHandler, x));
			} else
			{
				button.setVisibility(View.GONE);
			}
		}
	}

	// Rotate the view counter-clockwise
	public void setRotate(int degree)
	{
		degree = degree >= 0 ? degree % 360 : degree % 360 + 360;

		if (degree == currentOrientation)
		{
			return;
		}
		currentOrientation = degree;

		LayoutInflater inflater = (LayoutInflater) MainScreen.getInstance().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		if (degree == 90 || degree == 270)
		{
			layoutView = inflater.inflate(R.layout.plugin_vf_barcodescanner_view_layout_landscape, null);
			setContentView(layoutView);

			RotateLayout r = (RotateLayout) findViewById(R.id.rotateLayout);
			r.setAngle(degree);
			r.invalidate();
		} else
		{
			Rect displayRectangle = new Rect();
			Window window = MainScreen.getInstance().getWindow();
			window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);

			layoutView = inflater.inflate(R.layout.plugin_vf_barcodescanner_view_layout, null);
			layoutView.setMinimumWidth((int) (displayRectangle.width() * 0.7f));
			layoutView.setMinimumHeight((int) (displayRectangle.height() * 0.7f));
			setContentView(layoutView);
			layoutView.setRotation(degree);
		}

		initControls();
	}
}
