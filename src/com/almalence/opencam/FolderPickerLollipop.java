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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class FolderPickerLollipop extends Activity
{

	private int						old_value				= 0;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);


		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
	    startActivityForResult(intent, 42);

//		if (savedInstanceState != null)
//		{
//			if (savedInstanceState.containsKey(FolderPickerLollipop.TEMP_DIR))
//			{
//				this.currentPath = new File(savedInstanceState.getString(FolderPickerLollipop.TEMP_DIR));
//			}
//		}

//		if (this.currentPath == null)
//		{
//			String savedPath = PreferenceManager.getDefaultSharedPreferences(this).getString(MainScreen.sSavePathPref,
//					"/");
//
//			if (savedPath.startsWith(this.currentRoot.getAbsolutePath()))
//			{
//				this.currentPath = new File(savedPath);
//			} else
//			{
//				this.currentPath = this.currentRoot;
//			}
//		}

		
		this.old_value = this.getIntent().getExtras().getInt(MainScreen.sSavePathPref, 0);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
	    if (resultCode == RESULT_OK) {
	        Uri treeUri = resultData.getData();
	        
	        File currentPath = new File(treeUri.getPath());
	        
	        
	        PreferenceManager.getDefaultSharedPreferences(this).edit()
			.putString(MainScreen.sSavePathPref, currentPath.getAbsolutePath()).commit();
	        this.finish();
//	        DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
//
//	        // List all existing files inside picked directory
//	        for (DocumentFile file : pickedDir.listFiles()) {
//	            Log.d("Folder picker", "Found file " + file.getName() + " with size " + file.length());
//	        }
//
//	        // Create a new file and write into it
//	        DocumentFile newFile = pickedDir.createFile("text/plain", "My Novel");
//	        OutputStream out = getContentResolver().openOutputStream(newFile.getUri());
//	        out.write("A long time ago...".getBytes());
//	        out.close();
	    }
	    else
	    	this.finish();
	}

	@Override
	public void onSaveInstanceState(Bundle bundle)
	{
		super.onSaveInstanceState(bundle);

//		bundle.putString(FolderPickerLollipop.TEMP_DIR, this.currentPath.getAbsolutePath());

	}

	
//	@Override
//	public Object onRetainNonConfigurationInstance()
//	{
//		if (this.nf_dialog == null)
//		{
//			return null;
//		} else
//		{
//			if (this.nf_dialog.isShowing())
//			{
//				return ((EditText) this.nf_dialog.findViewById(editTextId)).getText().toString();
//			} else
//			{
//				return null;
//			}
//		}
//	}

	@Override
	public void onResume()
	{
		super.onResume();

//		this.refreshItems(this.currentPath);
	}


}
