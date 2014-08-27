/*
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is collection of files collectively known as Open Camera.

The Initial Developer of the Original Code is Almalence Inc.
Portions created by Initial Developer are Copyright (C) 2013 
by Almalence Inc. All Rights Reserved.
 */

/* <!-- +++
 package com.almalence.opencam_plus;
 +++ --> */
// <!-- -+-
package com.almalence.opencam;

//-+- -->

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class FolderPicker extends Activity implements OnItemClickListener, OnClickListener
{
	private static final String[]	MOUNT_POINT_FOLDERNAMES	= new String[] 
		{	"sd-ext", 
			"external_sd", 
			"external_SD",
			"sdcard-ext", 
			"extSdCard", 
			"sdcard", 
			"bootsdcard", 
			"emmc", 
			"extSdCard", 
			"ExtSDCard", 
			"sdcard0", 
			"sdcard1",
			"sdcard2", 
			"ext_sdcard", 
			"MicroSD"				
			};

	private static final String[]	ROOT_CANDIDATES			= new String[] 
		{ 	"/storage", 
			"/mnt", 
			"/Removable", 
			"/" 
		};

	private static final String		TEMP_DIR				= "FLDRPICKTMPDIR";

	private FolderPickerAdapter		adapter;
	private ListView				listView;
	private EditText				editText;
	private Button					buttonNewFolder;
	private Button					buttonPick;
	private File					currentRoot;
	private File					currentPath				= null;
	private boolean					show_all				= false;

	private int						old_value				= 0;

	private ArrayList<String>		items					= new ArrayList<String>();

	private AlertDialog				nf_dialog				= null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.folderpicker);

		this.findOutBestRoot();

		if (savedInstanceState != null)
		{
			if (savedInstanceState.containsKey(FolderPicker.TEMP_DIR))
			{
				this.currentPath = new File(savedInstanceState.getString(FolderPicker.TEMP_DIR));
			}
		}

		if (this.currentPath == null)
		{
			String savedPath = PreferenceManager.getDefaultSharedPreferences(this).getString(MainScreen.sSavePathPref,
					"/");

			if (savedPath.startsWith(this.currentRoot.getAbsolutePath()))
			{
				this.currentPath = new File(savedPath);
			} else
			{
				this.currentPath = this.currentRoot;
			}
		}

		this.adapter = new FolderPickerAdapter();

		this.listView = (ListView) this.findViewById(R.id.folderpicker_list);
		this.listView.setAdapter(this.adapter);
		this.listView.setOnItemClickListener(this);

		this.buttonPick = ((Button) this.findViewById(R.id.folderpicker_pick));
		this.buttonPick.setOnClickListener(this);
		this.buttonNewFolder = ((Button) this.findViewById(R.id.folderpicker_newfolder));
		this.buttonNewFolder.setOnClickListener(this);

		this.editText = (EditText) this.findViewById(R.id.folderpicker_address);

		this.old_value = this.getIntent().getExtras().getInt(MainScreen.sSavePathPref, 0);

		Object obj = this.getLastNonConfigurationInstance();
		if (obj != null)
		{
			if (obj instanceof String)
			{
				this.showCreateFolderDialog((String) obj);
			}
		}
	}

	private void findOutBestRoot()
	{
		int[] counts = new int[ROOT_CANDIDATES.length];

		for (int i = 0; i < ROOT_CANDIDATES.length; i++)
		{
			File troot = new File(ROOT_CANDIDATES[i]);

			for (int j = 0; j < MOUNT_POINT_FOLDERNAMES.length; j++)
			{
				try
				{
					if (new File(troot, FolderPicker.MOUNT_POINT_FOLDERNAMES[j]).exists())
					{
						++counts[i];
					}
				} catch (Exception e)
				{

				}
			}
		}

		int max = 0;
		for (int i = 1; i < ROOT_CANDIDATES.length; i++)
		{
			if (counts[i] > counts[max])
			{
				max = i;
			}
		}

		if (counts[max] == 0)
		{
			this.currentRoot = new File("/");
			this.show_all = true;
		} else
		{
			this.currentRoot = new File(ROOT_CANDIDATES[max]);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle bundle)
	{
		super.onSaveInstanceState(bundle);

		bundle.putString(FolderPicker.TEMP_DIR, this.currentPath.getAbsolutePath());

	}

	private static final int	editTextId	= 9853284;

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		if (this.nf_dialog == null)
		{
			return null;
		} else
		{
			if (this.nf_dialog.isShowing())
			{
				return ((EditText) this.nf_dialog.findViewById(editTextId)).getText().toString();
			} else
			{
				return null;
			}
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();

		this.refreshItems(this.currentPath);
	}

	private void showCreateFolderDialog(String name)
	{
		this.nf_dialog = new AlertDialog.Builder(this).create();

		this.nf_dialog.setTitle(R.string.choose_folder_alert_0_2);

		final EditText editText = new EditText(this);
		editText.setText(name);
		editText.setSingleLine();
		editText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT);
		editText.setId(editTextId);

		this.nf_dialog.setView(editText);

		this.nf_dialog.setButton(this.getResources().getString(R.string.choose_folder_alert_2_1),
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{

						try
						{
							new File(FolderPicker.this.currentPath.getAbsoluteFile() + File.separator
									+ editText.getText().toString()).mkdirs();

							FolderPicker.this.refreshItems(FolderPicker.this.currentPath);
						} catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				});

		this.nf_dialog.setButton2(this.getResources().getString(R.string.choose_folder_alert_2_2),
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.cancel();
					}
				});

		this.nf_dialog.show();
	}

	protected void refreshItems(File newDir)
	{
		File[] files = null;

		try
		{
			if (newDir.isDirectory())
			{
				files = newDir.listFiles();
			}
		} catch (Exception e)
		{
			// Couldn't access
		}

		if (files != null)
		{
			this.items.clear();

			if (this.isRoot(newDir) && !this.show_all)
			{
				for (File file : files)
				{
					try
					{
						if (file.isDirectory())
						{
							for (String fname : FolderPicker.MOUNT_POINT_FOLDERNAMES)
							{
								if (file.getName().equals(fname))
								{
									this.items.add(file.getName());

									break;
								}
							}
						}
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			} else
			{
				for (File file : files)
				{
					try
					{
						if (file.isDirectory())
						{
							this.items.add(file.getName());
						}
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}

			Collections.sort(this.items, new Comparator<String>()
			{
				@Override
				public int compare(String object1, String object2)
				{
					return object1.compareToIgnoreCase(object2);
				}
			});

			this.currentPath = newDir;

			this.adapter.notifyDataSetChanged();

			this.editText.setText(this.currentPath.getAbsolutePath());
		}
	}

	private boolean isRoot(File newDir)
	{
		return (this.currentRoot.equals(newDir));
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{

		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			PreferenceManager.getDefaultSharedPreferences(this).edit().putString(getResources().getString(R.string.Preference_SaveToValue), "" + this.old_value)
					.commit();
		}

		return super.onKeyDown(keyCode, event);
	}

	private boolean isCurrentPathWritable()
	{
		try
		{
			if (this.currentPath.canWrite())
			{
				return true;
			}
		} catch (Exception e)
		{

		}

		return false;
	}

	@Override
	public void onClick(View v)
	{
		if (v == this.buttonPick)
		{
			if (this.isCurrentPathWritable())
			{
				PreferenceManager.getDefaultSharedPreferences(this).edit()
						.putString(MainScreen.sSavePathPref, this.currentPath.getAbsolutePath()).commit();

				this.finish();
			} else
			{
				AlertDialog alertDialog = new AlertDialog.Builder(this).create();
				alertDialog.setTitle(R.string.choose_folder_alert_0);
				alertDialog.setMessage(this.getResources().getString(R.string.choose_folder_alert_1));
				alertDialog.setButton(this.getResources().getString(R.string.choose_folder_alert_2),
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int which)
							{
							}
						});
				alertDialog.setIcon(R.drawable.alert_dialog_icon);
				alertDialog.show();

			}
		} else if (v == this.buttonNewFolder)
		{
			if (this.isCurrentPathWritable())
			{
				this.showCreateFolderDialog("A Better Camera");
			} else
			{
				AlertDialog alertDialog = new AlertDialog.Builder(this).create();
				alertDialog.setTitle(R.string.choose_folder_alert_0);
				alertDialog.setMessage(this.getResources().getString(R.string.choose_folder_alert_1_2));
				alertDialog.setButton(this.getResources().getString(R.string.choose_folder_alert_2),
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int which)
							{
							}
						});
				alertDialog.setIcon(R.drawable.alert_dialog_icon);
				alertDialog.show();

			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		if (!this.isRoot(this.currentPath))
		{
			if (position != 0)
			{
				this.refreshItems(new File(this.currentPath.getAbsoluteFile() + File.separator
						+ items.get(position - 1)));
			} else
			{
				this.refreshItems(this.currentPath.getParentFile());
			}
		} else
		{
			this.refreshItems(new File(this.currentPath.getAbsoluteFile() + File.separator + items.get(position)));
		}
	}

	private class FolderPickerAdapter extends BaseAdapter
	{
		private LayoutInflater	inflater	= LayoutInflater.from(FolderPicker.this);

		@Override
		public int getCount()
		{
			return (FolderPicker.this.items.size() + (FolderPicker.this.isRoot(FolderPicker.this.currentPath) ? 0 : 1));
		}

		@Override
		public Object getItem(int position)
		{
			return position;
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			if (convertView == null)
			{
				convertView = this.inflater.inflate(R.layout.folderpicker_cell, null);
			}

			if (!FolderPicker.this.isRoot(FolderPicker.this.currentPath))
			{
				if (position == 0)
				{
					((ImageView) convertView.findViewById(R.id.folderpicker_cell_icon))
							.setImageResource(R.drawable.ic_menu_back);
					((TextView) convertView.findViewById(R.id.folderpicker_cell_text)).setText("...");
				} else
				{
					((ImageView) convertView.findViewById(R.id.folderpicker_cell_icon))
							.setImageResource(R.drawable.ic_menu_archive);
					((TextView) convertView.findViewById(R.id.folderpicker_cell_text)).setText(FolderPicker.this.items
							.get(position - 1));
				}
			} else
			{
				((ImageView) convertView.findViewById(R.id.folderpicker_cell_icon))
						.setImageResource(R.drawable.ic_menu_archive);
				((TextView) convertView.findViewById(R.id.folderpicker_cell_text)).setText(FolderPicker.this.items
						.get(position));
			}

			return convertView;
		}

	}
}
