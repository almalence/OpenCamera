package com.almalence.plugins.vf.barcodescanner;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
	private final static Boolean ON = true;
	private final static Boolean OFF = false;
	private final MultiFormatReader mMultiFormatReader = new MultiFormatReader();
	private SoundPlayer mSoundPlayer = null;
	BarcodeArrayAdapter mAdapter = null;
	public static Boolean mBarcodeScannerState = OFF;
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
		mBound = new BoundingView(MainScreen.mainContext);
		
		clearViews();
		addView(mBound, ViewfinderZone.VIEWFINDER_ZONE_FULLSCREEN);
	}
	
	@Override
	public void onResume() {
		UpdatePreferences();
	}
	
	void UpdatePreferences()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);
		mBarcodeScannerState = prefs.getBoolean("PrefBarcodescannerVF", false);
		
		if (mBarcodeScannerState == ON) {
			quickControlIconID = R.drawable.gui_almalence_histogram_rgb;
		} else {
			quickControlIconID = R.drawable.gui_almalence_histogram_luma;
		}
		
        showGUI();
	}
	
	@Override
	public void onQuickControlClick()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);
		Editor editor = prefs.edit();
		
		if (mBarcodeScannerState == ON) {
			quickControlIconID = R.drawable.gui_almalence_histogram_luma;
        	editor.putBoolean("PrefBarcodescannerVF", false);
		} else {
			quickControlIconID = R.drawable.gui_almalence_histogram_rgb;
        	editor.putBoolean("PrefBarcodescannerVF", true);
		}
        editor.commit();
        
        UpdatePreferences();
	}
	
	public void showGUI () {
		if (mBarcodeScannerState == ON) {
			if (mBound != null) {
				mBound.setVisibility(View.VISIBLE);
			}
			if (mBarcodesListButton != null) {
				mBarcodesListButton.setVisibility(View.VISIBLE);
			}
		} else {
			if (mBound != null) {
				mBound.setVisibility(View.GONE);
			}
			if (mBarcodesListButton != null) {
				mBarcodesListButton.setVisibility(View.GONE);
			}
		}
	}
	
	public void initializeSoundPlayers() {
        mSoundPlayer = new SoundPlayer(MainScreen.mainContext, MainScreen.mainContext.getResources().openRawResourceFd(R.raw.plugin_vf_focus_ok));
    }
	
	public void releaseSoundPlayer() {
        if (mSoundPlayer != null) {
        	mSoundPlayer.release();
        	mSoundPlayer = null;
        }
    }
	
	@Override
	public void onCameraParametersSetup() {
		initializeSoundPlayers();
	}
	
	@Override
	public void onPause() {
		releaseSoundPlayer();
	}
	
	@Override
	public void onGUICreate() {
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
		
		if (mBound == null)
			mBound = new BoundingView(MainScreen.mainContext);

		clearViews();
		addView(mBound, Plugin.ViewfinderZone.VIEWFINDER_ZONE_FULLSCREEN);
		createScreenButton();
		showGUI();
	}
	
	public void createScreenButton()
	{
		LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();
		mButtonsLayout = inflator.inflate(R.layout.plugin_vf_barcodescanner_layout, null, false);
		mButtonsLayout.setVisibility(View.VISIBLE);
		
		mBarcodesListButton = (RotateImageView) mButtonsLayout.findViewById(R.id.buttonBarcodesList);
	
		List<View> specialView = new ArrayList<View>();
		RelativeLayout specialLayout = (RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout2);
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
				showBarcodesHistoryDialog();
			}
			
		});
		
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.height = (int)MainScreen.thiz.getResources().getDimension(R.dimen.videobuttons_size) * 4;
		params.rightMargin = - (int)MainScreen.thiz.getResources().getDimension(R.dimen.videobuttons_size) / 2 ;
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout2)).addView(mButtonsLayout, params);
		
		mButtonsLayout.setLayoutParams(params);
		mButtonsLayout.requestLayout();
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout2)).requestLayout();
		
		mBarcodesListButton.setOrientation(MainScreen.guiManager.getLayoutOrientation());
		mBarcodesListButton.invalidate();
		mBarcodesListButton.requestLayout();
	}
	
	protected void showBarcodesHistoryDialog() {
		final Dialog dialog = new BarcodeHistoryListDialog(MainScreen.thiz);

		ListView barcodesHistoryListView = (ListView) dialog.findViewById(R.id.barcodesHistoryList);
		mAdapter = new BarcodeArrayAdapter(MainScreen.thiz, BarcodeStorageHelper.getBarcodesList());
		barcodesHistoryListView.setAdapter(mAdapter);
		
		TextView barcodesHistoryEmpty = (TextView) dialog.findViewById(R.id.barcodesHistoryEmpty);
		if (mAdapter.getCount() > 0) {
			barcodesHistoryEmpty.setVisibility(View.GONE);
		}
		
		barcodesHistoryListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Barcode barcode = mAdapter.getItem(position);
				showBarcodeViewDialog(barcode);
			}
		});
		dialog.setOnDismissListener(new OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				mBarcodeScannerState = ON;
			}
		});
		mBarcodeScannerState = OFF;

		dialog.show();
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
	
	public void onDecoded(Result result) {
		Barcode barcode = new Barcode(result);
        BarcodeStorageHelper.addBarcode(barcode);
        
        showBarcodeViewDialog(barcode);
        
        if (mSoundPlayer != null)                
        	if (!MainScreen.ShutterPreference)
        		mSoundPlayer.play();
    }
	
	protected void showBarcodeViewDialog(Barcode barcode) {
    	final Dialog dialog = new Dialog(MainScreen.thiz);
    	dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.plugin_vf_barcodescanner_view_layout);

		// set the custom dialog components - text, image and button
		TextView dataTextView = (TextView) dialog.findViewById(R.id.dataTextView);
		dataTextView.setMovementMethod(LinkMovementMethod.getInstance());
		TextView formatTextView = (TextView) dialog.findViewById(R.id.formatTextView);
		TextView typeTextView = (TextView) dialog.findViewById(R.id.typeTextView);
		TextView timeTextView = (TextView) dialog.findViewById(R.id.timeTextView);
		
		dataTextView.setText(barcode.getData());
		formatTextView.setText(barcode.getFormat());
		typeTextView.setText(barcode.getType());
		timeTextView.setText(barcode.getDate().toString());

		
		Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
		dialogButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		
		dialog.setOnDismissListener(new OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				mBarcodeScannerState = ON;
			}
		});
		
		mBarcodeScannerState = OFF;
		dialog.show();
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
    
    public class BarcodeHistoryListDialog extends Dialog {
    	Context mainContext;
    	ListView list;
    	public BarcodeHistoryListDialog(Context context) {
		    super(context);
		    requestWindowFeature(Window.FEATURE_NO_TITLE);
		    setContentView(R.layout.plugin_vf_barcodescanner_list_layout);
		    mainContext = context;
		    list = (ListView) findViewById(R.id.barcodesHistoryList);
		    registerForContextMenu(list);
		}
		
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			super.onCreateContextMenu(menu, v, menuInfo);
	        MenuInflater inflater = ((Activity) mainContext).getMenuInflater();
	        inflater.inflate(R.menu.context_menu_plugin_vf_barcodescanner, menu);
	        
	        menu.getItem(0).setOnMenuItemClickListener(new OnMenuItemClickListener()
	        {
	            public boolean onMenuItemClick(MenuItem item) {
	            	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
	                Barcode barcode = mAdapter.getItem(info.position);
	                BarcodeStorageHelper.removeBarcode(barcode);
	                mAdapter.notifyDataSetChanged();
	                
	                TextView barcodesHistoryEmpty = (TextView) findViewById(R.id.barcodesHistoryEmpty);
	        		if (mAdapter.getCount() == 0) {
	        			barcodesHistoryEmpty.setVisibility(View.VISIBLE);
	        		}
	                return true;
	            }
	        });
		}
    }
}