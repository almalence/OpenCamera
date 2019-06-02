package com.almalence.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

public class ListPreferenceAdapter extends ArrayAdapter<String>
{

	private int				mSelectedItem;
	private int				mResource;
	private LayoutInflater	mInflater;
	private int				mFieldId;

	public ListPreferenceAdapter(Context context, int resource, int textViewResourceId, String[] objects, int selectedItem)
	{
		super(context, resource, textViewResourceId, objects);
		mResource = resource;
		mFieldId = textViewResourceId;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mSelectedItem = selectedItem;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View view;
		CheckedTextView text;

		if (convertView == null)
		{
			view = mInflater.inflate(mResource, parent, false);
		} else
		{
			view = convertView;
		}

		text = (CheckedTextView) view.findViewById(mFieldId);

		String item = getItem(position);
		text.setText(item);

		if (position == mSelectedItem)
		{
			text.setChecked(true);
		} else
		{
			text.setChecked(false);
		}

		return view;
	}
}