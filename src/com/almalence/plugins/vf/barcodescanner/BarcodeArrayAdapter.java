package com.almalence.plugins.vf.barcodescanner;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/* <!-- +++
 import com.almalence.opencam_plus.R;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.R;

//-+- -->
public class BarcodeArrayAdapter extends ArrayAdapter<Barcode>
{
	private final Context				context;
	private final ArrayList<Barcode>	values;

	public BarcodeArrayAdapter(Context context, ArrayList<Barcode> values)
	{
		super(context, android.R.layout.simple_list_item_1, values);
		this.context = context;
		this.values = values;
	}

	static class ViewHolder
	{
		public TextView	textView;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewHolder holder;
		View rowView = convertView;
		if (rowView == null)
		{
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(android.R.layout.simple_list_item_1, null, true);
			holder = new ViewHolder();
			holder.textView = (TextView) rowView.findViewById(android.R.id.text1);
			rowView.setTag(holder);
		} else
		{
			holder = (ViewHolder) rowView.getTag();
		}

		String text = values.get(position).getData();
		int length = values.get(position).getData().length();
		if (length > 25)
		{
			length = 25;
			text = text.substring(0, length - 1) + "...";
		}
		holder.textView.setText(text);
		return rowView;
	}
}