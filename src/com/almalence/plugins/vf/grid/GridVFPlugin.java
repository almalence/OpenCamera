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

package com.almalence.plugins.vf.grid;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

/* <!-- +++
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.Plugin;
 import com.almalence.opencam_plus.PluginViewfinder;
 import com.almalence.opencam_plus.R;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.Plugin;
import com.almalence.opencam.PluginViewfinder;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

/***
 * Implements viewfinder plugin - adds different grids on VF
 ***/

public class GridVFPlugin extends PluginViewfinder
{
	ImageView	grid		= null;

	private int	gridType	= 1;

	public GridVFPlugin()
	{
		super("com.almalence.plugins.gridvf", R.xml.preferences_vf_grid, 0, R.drawable.plugin_vf_grid_none, "Grid type");
	}

	@Override
	public void onResume()
	{
		refreshPreferences();
	}

	private void refreshPreferences()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		gridType = Integer.parseInt(prefs.getString("typePrefGrid", "1"));

		switch (gridType)
		{
		case 0:
			quickControlIconID = R.drawable.plugin_vf_grid_golden_icon;
			break;
		case 1:
			quickControlIconID = R.drawable.plugin_vf_grid_thirds_icon;
			break;
		case 2:
			quickControlIconID = R.drawable.plugin_vf_grid_trisec_icon;
			break;
		case 3:
			quickControlIconID = R.drawable.plugin_vf_grid_none;
			break;
		default:
			break;
		}
	}

	@Override
	public void onGUICreate()
	{
		refreshPreferences();

		if (grid == null)
			grid = new ImageView(MainScreen.getMainContext());

		setProperGrid();

		grid.setScaleType(ScaleType.FIT_XY);
		clearViews();
		addView(grid, Plugin.ViewfinderZone.VIEWFINDER_ZONE_FULLSCREEN);

		grid.setVisibility(View.VISIBLE);
	}

	@Override
	public void onQuickControlClick()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		gridType = Integer.parseInt(prefs.getString("typePrefGrid", "1"));

		if (gridType == 4)
			return;

		gridType = (gridType + 1) % 4;

		Editor editor = prefs.edit();
		switch (gridType)
		{
		case 0:
			quickControlIconID = R.drawable.plugin_vf_grid_golden_icon;
			editor.putString("typePrefGrid", "0");
			break;
		case 1:
			quickControlIconID = R.drawable.plugin_vf_grid_thirds_icon;
			editor.putString("typePrefGrid", "1");
			break;
		case 2:
			quickControlIconID = R.drawable.plugin_vf_grid_trisec_icon;
			editor.putString("typePrefGrid", "2");
			break;
		case 3:
			quickControlIconID = R.drawable.plugin_vf_grid_none;
			editor.putString("typePrefGrid", "3");
			break;
		default:
			break;
		}
		editor.commit();

		MainScreen.getGUIManager().removeViewQuick(grid);

		try
		{
			setProperGrid();
			grid.setScaleType(ScaleType.FIT_XY);
			clearViews();
			MainScreen.getGUIManager().addViewQuick(grid, Plugin.ViewfinderZone.VIEWFINDER_ZONE_FULLSCREEN);
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("Histogram", "onQuickControlClick exception: " + e.getMessage());
		}
	}

	private void setProperGrid()
	{

		CameraController.Size previewSize = new CameraController.Size(MainScreen.getPreviewWidth(),
				MainScreen.getPreviewHeight());

		float ratio = (float) previewSize.getWidth() / previewSize.getHeight();

		int ri = 1;
		if (Math.abs(ratio - 4 / 3.f) < 0.1f)
			ri = 1;
		if (Math.abs(ratio - 3 / 2.f) < 0.12f)
			ri = 2;
		if (Math.abs(ratio - 16 / 9.f) < 0.15f)
			ri = 3;
		if (Math.abs(ratio - 1 / 1.f) < 0.1f)
			ri = 4;
		int resID = 0;
		if (0 == gridType)
		{
			switch (ri)
			{
			case 1:
				resID = R.drawable.plugin_vf_grid_golden4x3;
				break;
			case 2:
				resID = R.drawable.plugin_vf_grid_golden3x2;
				break;
			case 3:
				resID = R.drawable.plugin_vf_grid_golden16x9;
				break;
			default:
				resID = R.drawable.plugin_vf_grid_golden4x3;
			}
			grid.setImageDrawable(MainScreen.getAppResources().getDrawable(resID));
		} else if (1 == gridType)
		{
			switch (ri)
			{
			case 1:
				resID = R.drawable.plugin_vf_grid_thirds4x3;
				break;
			case 2:
				resID = R.drawable.plugin_vf_grid_thirds3x2;
				break;
			case 3:
				resID = R.drawable.plugin_vf_grid_thirds16x9;
				break;
			default:
				resID = R.drawable.plugin_vf_grid_thirds4x3;
			}
			grid.setImageDrawable(MainScreen.getAppResources().getDrawable(resID));
		} else if (2 == gridType)
		{
			switch (ri)
			{
			case 1:
				resID = R.drawable.plugin_vf_grid_trisec4x3;
				break;
			case 2:
				resID = R.drawable.plugin_vf_grid_trisec3x2;
				break;
			case 3:
				resID = R.drawable.plugin_vf_grid_trisec16x9;
				break;
			default:
				resID = R.drawable.plugin_vf_grid_trisec4x3;
			}
			grid.setImageDrawable(MainScreen.getAppResources().getDrawable(resID));
		} else if (3 == gridType)
		{
			grid.setImageDrawable(MainScreen.getAppResources()
					.getDrawable(R.drawable.plugin_vf_grid_none_img));
		} else
		{
			switch (ri)
			{
			case 1:
				resID = R.drawable.plugin_vf_grid_thirds4x3;
				break;
			case 2:
				resID = R.drawable.plugin_vf_grid_thirds3x2;
				break;
			case 3:
				resID = R.drawable.plugin_vf_grid_thirds16x9;
				break;
			default:
				resID = R.drawable.plugin_vf_grid_thirds4x3;
			}
			grid.setImageDrawable(MainScreen.getAppResources().getDrawable(resID));
		}

//		grid.requestLayout();
	}
}
