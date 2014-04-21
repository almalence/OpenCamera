package com.almalence.plugins.vf.barcodescanner;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.almalence.opencam.MainScreen;
import com.almalence.opencam.Plugin;
import com.almalence.opencam.PluginViewfinder;
import com.almalence.opencam.R;
import com.almalence.opencam.SoundPlayer;
import com.almalence.ui.RotateImageView;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

public class BarcodeScannerVFPlugin extends PluginViewfinder {
    
	private static final double BOUNDS_FRACTION = 0.6;
	BoundingView mBound = null;
	private final static int ON = 0;
	private final static int OFF = 1;
	private final MultiFormatReader mMultiFormatReader = new MultiFormatReader();
	private SoundPlayer mDecodedPlayer = null;
	public static int mBarcodeScannerState = OFF;
	private int mFrameCounter = 0;
	
	private RotateImageView mBarcodesListButton;
	private View mButtonsLayout;
	
	
	public BarcodeScannerVFPlugin()
	{
		super("com.almalence.plugins.barcodescannervf",
			  R.xml.preferences_vf_barcodescanner,
			  0,
			  R.drawable.gui_almalence_histogram_rgb,
			  "Barcode scanner");
	}

	@Override
	public void onCreate() {
		UpdatePreferences();
		this.mBound = new BoundingView(MainScreen.mainContext);
		
		clearViews();
		addView(this.mBound, ViewfinderZone.VIEWFINDER_ZONE_FULLSCREEN);
	}
	
	void UpdatePreferences()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);
		mBarcodeScannerState = Integer.parseInt(prefs
				.getString("PrefBarcodescannerVF", "0"));
		
		switch (mBarcodeScannerState)
        {
        	case ON:
        	quickControlIconID = R.drawable.gui_almalence_histogram_rgb;
        	break;
        	case OFF:
        	quickControlIconID = R.drawable.gui_almalence_histogram_luma;
        	break;
        }
	}
	
	@Override
	public void onQuickControlClick()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);
		Editor editor = prefs.edit();
		
        switch (mBarcodeScannerState)
        {
	        case ON:
	        	quickControlIconID = R.drawable.gui_almalence_histogram_luma;
	        	editor.putString("PrefBarcodescannerVF", "1");
	        	break;
	        case OFF:
	        	quickControlIconID = R.drawable.gui_almalence_histogram_off;
	        	editor.putString("PrefBarcodescannerVF", "0");
	        	break;
        }
        editor.commit();
        
        UpdatePreferences();
        
        if (mBarcodeScannerState == OFF) {
			this.mBound.setVisibility(View.GONE);
		} else {
			this.mBound.setVisibility(View.VISIBLE);
		}
        
        UpdatePreferences();
	}
	
	@Override
	public void onGUICreate() {
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
		
		if (this.mBound == null)
			this.mBound = new BoundingView(MainScreen.mainContext);

		clearViews();
		addView(this.mBound, Plugin.ViewfinderZone.VIEWFINDER_ZONE_FULLSCREEN);
		createScreenButton();
	}
	
	public void createScreenButton()
	{
		LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();
		mButtonsLayout = inflator.inflate(R.layout.plugin_vf_barcodescanner_layout, null, false);
		mButtonsLayout.setVisibility(View.VISIBLE);
		
		mBarcodesListButton = (RotateImageView) mButtonsLayout.findViewById(R.id.buttonBarcodesList);
	
		List<View> specialView = new ArrayList<View>();
		RelativeLayout specialLayout = (RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout);
		for(int i = 0; i < specialLayout.getChildCount(); i++)
			specialView.add(specialLayout.getChildAt(i));

		for(int j = 0; j < specialView.size(); j++)
		{
			View view = specialView.get(j);
			int view_id = view.getId();
			int layout_id = mButtonsLayout.getId();
			if(view_id == layout_id)
			{
				if(view.getParent() != null)
					((ViewGroup)view.getParent()).removeView(view);
				
				specialLayout.removeView(view);
			}
		}
		
		mBarcodesListButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(MainScreen.mainContext, BarcodeListActivity.class);
				MainScreen.thiz.startActivity(intent);
			}
			
		});
		
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.height = (int)MainScreen.thiz.getResources().getDimension(R.dimen.aeawlock_size);
		params.addRule(RelativeLayout.CENTER_IN_PARENT);
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout)).addView(mButtonsLayout, params);
		
		mButtonsLayout.setLayoutParams(params);
		mButtonsLayout.requestLayout();
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout)).requestLayout();
		
		mBarcodesListButton.setOrientation(MainScreen.guiManager.getLayoutOrientation());
		mBarcodesListButton.invalidate();
		mBarcodesListButton.requestLayout();
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera paramCamera) {
		if (mBarcodeScannerState == OFF)
			return;
		mFrameCounter++;
		if (mFrameCounter != 20) {
			return;
		}

		Camera.Parameters params = MainScreen.thiz.getCameraParameters();
		if (params == null)
			return;

		int previewWidth = params.getPreviewSize().width;
		int previewHeight = params.getPreviewSize().height;

        new DecodeAsyncTask(previewWidth, previewHeight).execute(data);
        
		mFrameCounter = 0;
	}
	
    public synchronized PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height, Rect boundingRect) {
        return new PlanarYUVLuminanceSource(data, width, height, boundingRect.left, boundingRect.top,
                boundingRect.width(), boundingRect.height(), false);
    }
	
	/**
	 * Asynchronous task for decoding and finding barcode
	 */
	private class DecodeAsyncTask extends AsyncTask<byte[], Void, Result> {
	    private int width;
	    private int height;
	    
	    private DecodeAsyncTask(int width, int height) {
	        this.width = width;
	        this.height = height;
	    }
	    
	    @Override
	    protected Result doInBackground(byte[]... datas) {
	        Result rawResult = null;
	        final PlanarYUVLuminanceSource source = buildLuminanceSource(datas[0], width,
                            height, getBoundingRect());
	        if (source != null) {
	            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
	            try {
	                rawResult = mMultiFormatReader.decodeWithState(bitmap);
	            } catch (ReaderException re) {
	                // nothing to do here
	            } finally {
	                mMultiFormatReader.reset();
	            }
	        }
	        return rawResult;
	    }
	    
	    @Override
        protected void onPostExecute(Result result) {
            if (result != null) {
                onDecoded(result);
            }
        }
	}
	
	public void onDecoded(Result result) {
        BarcodeStorageHelper.addBarcode(new Barcode(result));
        
		Toast.makeText(MainScreen.mainContext, result.toString(), Toast.LENGTH_SHORT).show();
        
        if (mDecodedPlayer == null) {
        	mDecodedPlayer = new SoundPlayer(MainScreen.mainContext, MainScreen.mainContext.getResources().openRawResourceFd(R.raw.plugin_vf_focus_ok));
        }
        if (mDecodedPlayer != null) {
        	mDecodedPlayer.play();	
        }
    }
	
	/**
     * @return bounding rect for camera
     */
    public final synchronized Rect getBoundingRect() {
    	Camera.Parameters params = MainScreen.thiz.getCameraParameters();
        if (params != null) {
            Camera.Size previewSize = params.getPreviewSize();
            int previewHeight = previewSize.height;
            int previewWidth = previewSize.width;

            double heightFraction = BOUNDS_FRACTION;
            double widthFraction = BOUNDS_FRACTION;

            int height = (int) (previewHeight * heightFraction);
            int width = (int) (previewWidth * widthFraction);
            int left = (int) (previewWidth * ((1 - widthFraction) / 2));
            int top = (int) (previewHeight * ((1 - heightFraction) / 2));
            int right = left + width;
            int bottom = top + height;

            return new Rect(left, top, right, bottom);
        }
        return null;
    }
	
	/**
     * @return bounding rect for ui
     */
    public final synchronized Rect getBoundingRectUi(int uiWidth, int uiHeight) {
        double heightFraction = BOUNDS_FRACTION;
        double widthFraction = BOUNDS_FRACTION;

        int height = (int) (uiHeight * heightFraction);
        int width = (int) (uiWidth * widthFraction);
        int left = (int) (uiWidth * ((1 - widthFraction) / 2));
        int top = (int) (uiHeight * ((1 - heightFraction) / 2));
        int right = left + width;
        int bottom = top + height;

        return new Rect(left, top, right, bottom);
    }
    
    /**
     * View for displaying bounds for active camera region
     */
    class BoundingView extends View {
            public BoundingView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setARGB(110, 110, 110, 50);
            Rect boundingRect = getBoundingRectUi(canvas.getWidth(), canvas.getHeight());
            canvas.drawRect(boundingRect, paint);
            super.onDraw(canvas);
        }
    }
}

