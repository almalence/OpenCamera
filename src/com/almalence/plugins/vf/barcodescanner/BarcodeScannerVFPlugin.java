package com.almalence.plugins.vf.barcodescanner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RelativeLayout;

/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.PluginViewfinder;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.SoundPlayer;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginViewfinder;
import com.almalence.opencam.R;
import com.almalence.opencam.SoundPlayer;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->
import com.almalence.ui.RotateImageView;
import com.almalence.util.ImageConversion;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

public class BarcodeScannerVFPlugin extends PluginViewfinder
{

	private static final double			BOUNDS_FRACTION			= 0.6;
	private static final Boolean		ON						= true;
	private static final Boolean		OFF						= false;

	private final MultiFormatReader		mMultiFormatReader		= new MultiFormatReader();
	private SoundPlayer					mSoundPlayer			= null;
	private static Boolean				mBarcodeScannerState	= OFF;
	private static final Object			lock					= new Object();
	private static Boolean				decodedProcessing		= false;
	private int							mFrameCounter			= 0;
	private int							mOrientation			= 0;
	private BoundingView				mBound					= null;
	private RotateImageView				mBarcodesListButton;
	private View						mButtonsLayout;
	private BarcodeHistoryListDialog	barcodeHistoryDialog;
	private BarcodeViewDialog			barcodeViewDialog;

	public BarcodeScannerVFPlugin()
	{
		super("com.almalence.plugins.barcodescannervf", R.xml.preferences_vf_barcodescanner, 0,
				R.drawable.gui_almalence_settings_scene_barcode_on, "Barcode scanner");
	}

	@Override
	public void onResume()
	{
		updatePreferences();
	}

	void updatePreferences()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		mBarcodeScannerState = prefs.getBoolean("PrefBarcodescannerVF", false);

		if (mBarcodeScannerState == ON)
		{
			quickControlIconID = R.drawable.gui_almalence_settings_scene_barcode_on;
		} else
		{
			quickControlIconID = R.drawable.gui_almalence_settings_off_barcode_scanner;
		}

		showGUI();
	}

	@Override
	public void onOrientationChanged(int orientation)
	{
		mOrientation = orientation;
		if (mBarcodesListButton != null)
		{
			mBarcodesListButton.setOrientation(MainScreen.getGUIManager().getLayoutOrientation());
			mBarcodesListButton.invalidate();
			mBarcodesListButton.requestLayout();
		}
		if (barcodeHistoryDialog != null)
		{
			barcodeHistoryDialog.setRotate(MainScreen.getGUIManager().getLayoutOrientation());
		}
		if (barcodeViewDialog != null)
		{
			barcodeViewDialog.setRotate(MainScreen.getGUIManager().getLayoutOrientation());
		}
	}

	@Override
	public void onQuickControlClick()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		Editor editor = prefs.edit();

		if (mBarcodeScannerState == ON)
		{
			quickControlIconID = R.drawable.gui_almalence_settings_off_barcode_scanner;
			editor.putBoolean("PrefBarcodescannerVF", false);
		} else
		{
			quickControlIconID = R.drawable.gui_almalence_settings_scene_barcode_on;
			editor.putBoolean("PrefBarcodescannerVF", true);
		}
		editor.commit();

		updatePreferences();
	}

	/**
	 * Show or hide GUI elements of plugin. Depends on plugin state and history.
	 */
	public void showGUI()
	{
		if (mBarcodeScannerState == ON)
		{
			if (mBound == null)
			{
				createBoundView();
			}
			if (mBarcodesListButton == null)
			{
				createScreenButton();
			}

			if (mBound != null)
			{
				mBound.setVisibility(View.VISIBLE);
			}
			if (mBarcodesListButton != null)
			{
				if (BarcodeStorageHelper.getBarcodesList() != null && BarcodeStorageHelper.getBarcodesList().size() > 0)
				{
					mBarcodesListButton.setVisibility(View.VISIBLE);
				} else
				{
					mBarcodesListButton.setVisibility(View.GONE);
				}
			}
		} else
		{
			if (mBound != null)
			{
				mBound.setVisibility(View.GONE);
			}
			if (mBarcodesListButton != null)
			{
				mBarcodesListButton.setVisibility(View.GONE);
			}
		}
	}

	public void initializeSoundPlayer()
	{
		mSoundPlayer = new SoundPlayer(MainScreen.getMainContext(), MainScreen.getMainContext().getResources()
				.openRawResourceFd(R.raw.plugin_vf_focus_ok));
	}

	public void releaseSoundPlayer()
	{
		if (mSoundPlayer != null)
		{
			mSoundPlayer.release();
			mSoundPlayer = null;
		}
	}

	@Override
	public void onCameraParametersSetup()
	{
		initializeSoundPlayer();
	}

	@Override
	public void onPause()
	{
		releaseSoundPlayer();
		if (mBound != null)
			mBound.setVisibility(View.GONE);
		if (mBarcodesListButton != null)
			mBarcodesListButton.setVisibility(View.GONE);
		clearViews();
		mBound = null;
		mBarcodesListButton = null;
	}

	@Override
	public void onGUICreate()
	{
		showGUI();
	}

	/**
	 * Create bound view.
	 */
	public void createBoundView()
	{
		if (mBound != null)
		{
			return;
		}
		Camera camera = CameraController.getCamera();
		if (null == camera)
		{
			return;
		}

		mBound = new BoundingView(MainScreen.getMainContext());
		mBound.setVisibility(View.VISIBLE);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout)).addView(mBound, params);

		mBound.setLayoutParams(params);

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout)).requestLayout();
	}

	/**
	 * Create history button.
	 */
	public void createScreenButton()
	{
		LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
		mButtonsLayout = inflator.inflate(R.layout.plugin_vf_barcodescanner_layout, null, false);
		mButtonsLayout.setVisibility(View.VISIBLE);

		mBarcodesListButton = (RotateImageView) mButtonsLayout.findViewById(R.id.buttonBarcodesList);

		MainScreen.getGUIManager().removeViews(mButtonsLayout, R.id.specialPluginsLayout3);

		mBarcodesListButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showBarcodesHistoryDialog();
			}

		});

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout3)).addView(mButtonsLayout,
				params);

		mButtonsLayout.setLayoutParams(params);
		mButtonsLayout.requestLayout();

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout3)).requestLayout();

		mBarcodesListButton.setOrientation(MainScreen.getGUIManager().getLayoutOrientation());
		mBarcodesListButton.invalidate();
		mBarcodesListButton.requestLayout();
	}

	protected void showBarcodesHistoryDialog()
	{
		barcodeHistoryDialog = new BarcodeHistoryListDialog(MainScreen.getInstance());
		barcodeHistoryDialog.setRotate(MainScreen.getGUIManager().getLayoutOrientation());

		barcodeHistoryDialog.list.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				Barcode barcode = (Barcode) barcodeHistoryDialog.list.getAdapter().getItem(position);
				showBarcodeViewDialog(barcode);
			}
		});

		barcodeHistoryDialog.setOnDismissListener(new OnDismissListener()
		{
			public void onDismiss(DialogInterface dialog)
			{
				mBarcodeScannerState = ON;
				showGUI();
			}
		});
		mBarcodeScannerState = OFF;

		barcodeHistoryDialog.show();
	}

	@Override
	public void onPreviewFrame(byte[] data)
	{
		if (mBarcodeScannerState == OFF)
			return;
		mFrameCounter++;
		if (mFrameCounter != 10)
		{
			return;
		}

		new DecodeAsyncTask(MainScreen.getPreviewWidth(), MainScreen.getPreviewHeight()).execute(data);

		mFrameCounter = 0;
	}

	public synchronized PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height,
			Rect boundingRect)
	{
		return new PlanarYUVLuminanceSource(data, width, height, boundingRect.left, boundingRect.top,
				boundingRect.width(), boundingRect.height(), false);
	}

	/**
	 * Handle success decoded barcode.
	 * 
	 * @param barcode
	 */
	public void onDecoded(Barcode barcode)
	{
		if (mBarcodeScannerState == OFF)
		{
			return;
		}

		// <!-- -+-
		// sale hook
		if (barcode.getData().equals("abc.almalence.com/qrpromo") && !MainScreen.getInstance().isUnlockedAll())
		{
			MainScreen.getInstance().activateCouponSale();
			MainScreen.getGUIManager().showStore();
			return;
		}
		// -+- -->

		BarcodeStorageHelper.addBarcode(barcode);

		showBarcodeViewDialog(barcode);

		if (mSoundPlayer != null)
			if (!MainScreen.isShutterSoundEnabled())
				mSoundPlayer.play();
		
		decodedProcessing = false;
	}

	protected void showBarcodeViewDialog(Barcode barcode)
	{
		try
		{
			barcodeViewDialog = new BarcodeViewDialog(MainScreen.getInstance(), barcode);
			barcodeViewDialog.setRotate(MainScreen.getGUIManager().getLayoutOrientation());
			showGUI();

			barcodeViewDialog.setOnDismissListener(new OnDismissListener()
			{
				public void onDismiss(DialogInterface dialog)
				{
					mBarcodeScannerState = ON;
				}
			});

			mBarcodeScannerState = OFF;
			barcodeViewDialog.show();
		} catch (Exception e)
		{
		}
	}

	/**
	 * @return bounding rect for camera
	 */
	public final synchronized Rect getBoundingRect()
	{

		double heightFraction = BOUNDS_FRACTION;
		double widthFraction = BOUNDS_FRACTION;

		int height = (int) (MainScreen.getPreviewHeight() * heightFraction);
		int width = (int) (MainScreen.getPreviewWidth() * widthFraction);
		int left = (int) (MainScreen.getPreviewWidth() * ((1 - widthFraction) / 2));
		int top = (int) (MainScreen.getPreviewHeight() * ((1 - heightFraction) / 2));
		int right = left + width;
		int bottom = top + height;

		return new Rect(left, top, right, bottom);
	}

	/**
	 * @return bounding rect for ui
	 */
	public final synchronized Rect getBoundingRectUi(int uiWidth, int uiHeight)
	{
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
	private class DecodeAsyncTask extends AsyncTask<byte[], Void, Barcode>
	{
		private int	width;
		private int	height;

		private DecodeAsyncTask(int width, int height)
		{
			this.width = width;
			this.height = height;
		}

		@Override
		protected Barcode doInBackground(byte[]... datas)
		{
			Result rawResult = null;
			File file = null;
			final PlanarYUVLuminanceSource source = buildLuminanceSource(datas[0], width, height, getBoundingRect());
			if (source != null)
			{
				BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
				try
				{
					rawResult = mMultiFormatReader.decodeWithState(bitmap);
				} catch (ReaderException re)
				{
					// nothing to do here
				} catch (Exception e)
				{
					e.printStackTrace();
				} finally
				{
					mMultiFormatReader.reset();
				}
			}

			if (rawResult == null)
			{
				return null;
			}

			synchronized (lock)
			{
				if (rawResult != null && !decodedProcessing)
				{
					decodedProcessing = true;
					file = saveDecodedImageToFile(datas);
				} else {
					return null;
				}
			}

			Barcode barcode = null;
			if (file != null)
			{
				barcode = new Barcode(rawResult, file.getAbsolutePath());
			} else
			{
				barcode = new Barcode(rawResult);
			}

			return barcode;
		}

		@Override
		protected void onPostExecute(Barcode barcode)
		{
			if (barcode != null)
			{
				onDecoded(barcode);
			}
		}
	}

	private synchronized File saveDecodedImageToFile(byte[]... datas)
	{
		File file = null;
		byte[] dataRotated = new byte[datas[0].length];
		ImageConversion.TransformNV21(datas[0], dataRotated, MainScreen.getPreviewWidth(),
				MainScreen.getPreviewHeight(), 0, 0, 1);
		datas[0] = dataRotated;

		Rect rect = new Rect(0, 0, MainScreen.getPreviewHeight(), MainScreen.getPreviewWidth());
		YuvImage img = new YuvImage(datas[0], ImageFormat.NV21, MainScreen.getPreviewHeight(),
				MainScreen.getPreviewWidth(), null);

		Calendar d = Calendar.getInstance();
		String fileFormat = String.format("%04d%02d%02d_%02d%02d%02d", d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1,
				d.get(Calendar.DAY_OF_MONTH), d.get(Calendar.HOUR_OF_DAY), d.get(Calendar.MINUTE),
				d.get(Calendar.SECOND));

		File saveDir = PluginManager.getInstance().getSaveDir(false);
		file = new File(saveDir, fileFormat + ".jpg");
		FileOutputStream os = null;
		try
		{
			os = new FileOutputStream(file);
		} catch (Exception e)
		{
			// save always if not working saving to sdcard
			e.printStackTrace();
			saveDir = PluginManager.getInstance().getSaveDir(true);
			file = new File(saveDir, fileFormat + ".jpg");
			try
			{
				os = new FileOutputStream(file);
			} catch (FileNotFoundException e1)
			{
				e1.printStackTrace();
			}
		}

		if (os != null)
		{
			try
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] rawImage = null;
				img.compressToJpeg(rect, 100, baos);
				rawImage = baos.toByteArray();

				// This is the same image as the preview but in JPEG and not
				// rotated
				Bitmap bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length);

				// Rotate the Bitmap
				Matrix matrix = new Matrix();
				matrix.postRotate(mOrientation - 90);

				// We rotate the same Bitmap
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, MainScreen.getPreviewHeight(), MainScreen.getPreviewWidth(),
						matrix, false);

				// We dump the rotated Bitmap to the stream
				bitmap.compress(CompressFormat.JPEG, 100, os);

				os.flush();
				os.close();
			} catch (FileNotFoundException e)
			{
				e.printStackTrace();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		return file;
	}

	/**
	 * View for displaying bounds for active camera region
	 */
	class BoundingView extends View
	{
		private Paint	paint;

		public BoundingView(Context context)
		{
			super(context);
			paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setARGB(110, 128, 128, 128);
		}

		@Override
		protected void onDraw(Canvas canvas)
		{
			int width = canvas.getWidth();
			int height = canvas.getHeight();
			Rect boundingRect = getBoundingRectUi(canvas.getWidth(), canvas.getHeight());

			canvas.drawRect(0, 0, width, boundingRect.top, paint);
			canvas.drawRect(0, boundingRect.top, boundingRect.left, boundingRect.bottom + 1, paint);
			canvas.drawRect(boundingRect.right + 1, boundingRect.top, width, boundingRect.bottom + 1, paint);
			canvas.drawRect(0, boundingRect.bottom + 1, width, height, paint);
			super.onDraw(canvas);
		}
	}

}
