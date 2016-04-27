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
 package com.almalence.opencam_plus.ui;
 +++ --> */
// <!-- -+-
package com.almalence.opencamunderground.ui;
//-+- -->

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/* <!-- +++
 import com.almalence.opencam_plus.ApplicationInterface;
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
// <!-- -+-
import com.almalence.opencamunderground.R;
import com.almalence.opencamunderground.ApplicationInterface;
import com.almalence.opencamunderground.MainScreen;
import com.almalence.opencamunderground.PluginManager;
import com.almalence.opencamunderground.cameracontroller.CameraController;
//-+- -->

import com.almalence.ui.RotateImageView;

import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.webkit.WebView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AlmalenceStore
{
	// store grid adapter
	private ElementAdapter			storeAdapter;
	private List<View>				storeViews;
	private HashMap<View, Integer>	buttonStoreViewAssoc;
	private View					guiView;

	
	AlmalenceStore(View gui)
	{
		guiView = gui;
		storeAdapter = new ElementAdapter();
		storeViews = new ArrayList<View>();
		buttonStoreViewAssoc = new HashMap<View, Integer>();
	}

	public void showStore()
	{
		LayoutInflater inflater = LayoutInflater.from(MainScreen.getInstance());
		List<RelativeLayout> pages = new ArrayList<RelativeLayout>();
		
		// page 1
		RelativeLayout page = (RelativeLayout) inflater.inflate(R.layout.gui_almalence_pager_fragment, null);
		

		// page 2
		RelativeLayout features = (RelativeLayout) inflater.inflate(R.layout.gui_almalence_features, null);
		final ImageView imgFeaturesPrev = (ImageView) features.findViewById(R.id.storeWhatsNew);
		imgFeaturesPrev.setVisibility(View.INVISIBLE);
		WebView wv = (WebView)features.findViewById(R.id.text_features);
		wv.loadUrl("file:///android_asset/www/features.html");

		page.addView(features);
		pages.add(page);

		SamplePagerAdapter pagerAdapter = new SamplePagerAdapter(pages);
		final ViewPager viewPager = new ViewPager(MainScreen.getInstance());
		viewPager.setAdapter(pagerAdapter);
			viewPager.setCurrentItem(0);
		
		guiView.findViewById(R.id.buttonGallery).setEnabled(false);
		guiView.findViewById(R.id.buttonShutter).setEnabled(false);
		guiView.findViewById(R.id.buttonSelectMode).setEnabled(false);

		PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, 
				ApplicationInterface.MSG_CONTROL_LOCKED);

		MainScreen.getGUIManager().lockControls = true;

		final RelativeLayout pagerLayout = ((RelativeLayout) guiView.findViewById(R.id.viewPagerLayout));
		pagerLayout.addView(viewPager);
				
		final RelativeLayout pagerLayoutMain = ((RelativeLayout) guiView.findViewById(R.id.viewPagerLayoutMain));
		pagerLayoutMain.setVisibility(View.VISIBLE);
		pagerLayoutMain.bringToFront();


		// We need this timer, to show store on top, after we return from google
		// play.
		// In MainScreen there is timer, which brings main buttons on top,
		// after MainScreen activity resumed.
		// So this timer "blocks" timer from MainScreen if we want to show
		// store.
		new CountDownTimer(600, 10)
		{
			public void onTick(long millisUntilFinished)
			{
				pagerLayoutMain.bringToFront();
			}

			public void onFinish()
			{
				pagerLayoutMain.bringToFront();
			}
		}.start();
	}

	public void hideStore()
	{
		((RelativeLayout) guiView.findViewById(R.id.viewPagerLayoutMain)).setVisibility(View.INVISIBLE);

		guiView.findViewById(R.id.buttonGallery).setEnabled(true);
		guiView.findViewById(R.id.buttonShutter).setEnabled(true);
		guiView.findViewById(R.id.buttonSelectMode).setEnabled(true);

		PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, 
				ApplicationInterface.MSG_CONTROL_UNLOCKED);

		MainScreen.getGUIManager().lockControls = false;
	}

	public void setOrientation()
	{
		((RotateImageView) guiView.findViewById(R.id.Unlock)).setOrientation(AlmalenceGUI.mDeviceOrientation);
	}
}
