package com.almalence.plugins.vf.barcodescanner;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.R;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.R;
//-+- -->
import com.almalence.ui.RotateLayout;
import com.almalence.ui.RotateDialog;

public class BarcodeHistoryListDialog extends RotateDialog implements android.view.View.OnClickListener
{
	Context		mainContext;
	ListView	list;

	public BarcodeHistoryListDialog(Context context)
	{
		super(context);
		mainContext = context;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		layoutView = (LinearLayout) getLayoutInflater().inflate(R.layout.plugin_vf_barcodescanner_list_layout, null);

		// Set dialog size
		Rect displayRectangle = new Rect();
		Window window = MainScreen.getInstance().getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
		layoutView.setMinimumWidth((int) (displayRectangle.width() * 0.7f));
		layoutView.setMinimumHeight((int) (displayRectangle.height() * 0.7f));

		setContentView(layoutView);

		list = (ListView) findViewById(R.id.barcodesHistoryList);
		BarcodeArrayAdapter adapter = new BarcodeArrayAdapter(MainScreen.getInstance(),
				BarcodeStorageHelper.getBarcodesList());
		list.setAdapter(adapter);

		Button clearBarcodesButton = (Button) findViewById(R.id.clearBarcodesButton);
		clearBarcodesButton.setOnClickListener(this);
		registerForContextMenu(list);

		TextView barcodesHistoryEmpty = (TextView) findViewById(R.id.barcodesHistoryEmpty);
		if (adapter.getCount() > 0)
		{
			barcodesHistoryEmpty.setVisibility(View.GONE);
		}

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = ((Activity) mainContext).getMenuInflater();
		inflater.inflate(R.menu.context_menu_plugin_vf_barcodescanner, menu);

		// Delete button onClick listener.
		menu.getItem(0).setOnMenuItemClickListener(new OnMenuItemClickListener()
		{
			public boolean onMenuItemClick(MenuItem item)
			{
				BarcodeArrayAdapter adapter = (BarcodeArrayAdapter) list.getAdapter();
				AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
				Barcode barcode = adapter.getItem(info.position);
				BarcodeStorageHelper.removeBarcode(barcode);
				adapter.notifyDataSetChanged();

				TextView barcodesHistoryEmpty = (TextView) findViewById(R.id.barcodesHistoryEmpty);
				if (adapter.getCount() == 0)
				{
					barcodesHistoryEmpty.setVisibility(View.VISIBLE);
				}
				return true;
			}
		});
	}

	@Override
	public void onClick(View v)
	{
		if (v.getId() == R.id.clearBarcodesButton)
		{
			BarcodeArrayAdapter adapter = (BarcodeArrayAdapter) list.getAdapter();
			BarcodeStorageHelper.removeAll();
			adapter.notifyDataSetChanged();

			TextView barcodesHistoryEmpty = (TextView) findViewById(R.id.barcodesHistoryEmpty);
			if (adapter.getCount() == 0)
			{
				barcodesHistoryEmpty.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	public void setRotate(int degree)
	{
		degree = degree >= 0 ? degree % 360 : degree % 360 + 360;

		if (degree == currentOrientation)
		{
			return;
		}
		currentOrientation = degree;

		RotateLayout r = (RotateLayout) findViewById(R.id.rotateLayout);
		r.setAngle(degree);
		r.requestLayout();
		r.invalidate();

	}
}
