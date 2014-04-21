package com.almalence.plugins.vf.barcodescanner;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.almalence.opencam.R;

public class BarcodeListActivity extends ListActivity{

	BarcodeArrayAdapter mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.plugin_vf_barcodescanner_list_layout);
		
		mAdapter = new BarcodeArrayAdapter(this, BarcodeStorageHelper.getBarcodesList());
		setListAdapter(mAdapter);
		registerForContextMenu(getListView());
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		Barcode barcode = mAdapter.getItem(position);
		Intent intent = new Intent(this, BarcodeViewActivity.class);
		intent.putExtra("barcode", barcode);
		startActivity(intent);
	}
	
	@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu_plugin_vf_barcodescanner, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.action_delete:
                ListView l = getListView();
                Barcode barcode = mAdapter.getItem(info.position);
                BarcodeStorageHelper.removeBarcode(barcode);
                mAdapter.notifyDataSetChanged();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
