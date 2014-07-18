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
package com.almalence.opencam.ui;
//-+- -->

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.R;
+++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
//-+- -->
import com.almalence.ui.RotateImageView;

import android.content.SharedPreferences;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class AlmalenceStore 
{
	//store grid adapter
	private ElementAdapter storeAdapter;
	private List<View> storeViews;
	private HashMap<View, Integer> buttonStoreViewAssoc;
	private View guiView;
	
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
        
        //page 1
        RelativeLayout page = (RelativeLayout) inflater.inflate(R.layout.gui_almalence_pager_fragment, null);
		initStoreList();
		RelativeLayout store = (RelativeLayout) inflater.inflate(R.layout.gui_almalence_store, null);
		final ImageView imgStoreNext = (ImageView) store.findViewById(R.id.storeWhatsNew);
		GridView gridview = (GridView) store.findViewById(R.id.storeGrid);
		gridview.setAdapter(storeAdapter);
		
		gridview.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return false;
			}
			
		});
		page.addView(store);
        pages.add(page);
        
        //page 2
        page = (RelativeLayout) inflater.inflate(R.layout.gui_almalence_pager_fragment, null);
        RelativeLayout whatsnew = (RelativeLayout) inflater.inflate(R.layout.gui_almalence_whatsnew, null);
        final ImageView imgWhatNewNext = (ImageView) whatsnew.findViewById(R.id.storeFeatures);
        final ImageView imgWhatNewPrev = (ImageView) whatsnew.findViewById(R.id.storeStore);
        imgWhatNewNext.setVisibility(View.INVISIBLE);
		imgWhatNewPrev.setVisibility(View.INVISIBLE);
        TextView text_whatsnew = (TextView) whatsnew.findViewById(R.id.text_whatsnew);
		text_whatsnew.setText("version 3.24"+
							  "\n- exif tags fixed"+
							  "\n- auto backup/sharing fixed"+
							  "\n- fixed work from 3rd party apps"+
							  "\n- UI corrections"+
							  "\n- video on some devices improved"+
							  "\n- stability fixes");

		page.addView(whatsnew);
        pages.add(page);
        
        //page 3
        page = (RelativeLayout) inflater.inflate(R.layout.gui_almalence_pager_fragment, null);
        RelativeLayout features = (RelativeLayout) inflater.inflate(R.layout.gui_almalence_features, null);
        final ImageView imgFeaturesNext = (ImageView) features.findViewById(R.id.storeTips);
        final ImageView imgFeaturesPrev = (ImageView) features.findViewById(R.id.storeWhatsNew);
		TextView text_features= (TextView) features.findViewById(R.id.text_features);
		text_features.setText("BLA BLA BLA BLA");

		page.addView(features);
        pages.add(page);
        
        //page 4
        page = (RelativeLayout) inflater.inflate(R.layout.gui_almalence_pager_fragment, null);
        RelativeLayout tips = (RelativeLayout) inflater.inflate(R.layout.gui_almalence_tips, null);
        final ImageView imgTipsPrev = (ImageView) tips.findViewById(R.id.storeTips);
		TextView text_tips = (TextView) tips.findViewById(R.id.text_tips);
		text_tips.setText("ABC tips and tricks"+
						  "\n\nIf you long press on any of the quick settings on the top bar you get a dropdown list that lets you select and change them to any setting you'd like."+
						  "\n\nSwipe left/write on main scree to see more/less info on the screen - histogram, grids, info controls, top quick settings menu."+
						  "\n\nPull down top menu to see all quick settings.");

		page.addView(tips);
        pages.add(page);
        
        SamplePagerAdapter pagerAdapter = new SamplePagerAdapter(pages);
        ViewPager viewPager = new ViewPager(MainScreen.getInstance());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(0);
        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
        {
        	@Override
            public void onPageSelected(int position) 
        	{
        		switch (position)
        		{
        		case 0:
        			//0
        			imgStoreNext.setVisibility(View.VISIBLE);
        			//1
        			imgWhatNewNext.setVisibility(View.INVISIBLE);
        			imgWhatNewPrev.setVisibility(View.INVISIBLE);
        			break;
        		case 1:
        			//0
        			imgStoreNext.setVisibility(View.INVISIBLE);
        			//1
        			imgWhatNewNext.setVisibility(View.VISIBLE);
        			imgWhatNewPrev.setVisibility(View.VISIBLE);
        			//2
        			imgFeaturesNext.setVisibility(View.INVISIBLE);
        			imgFeaturesPrev.setVisibility(View.INVISIBLE);
        			break;
        		case 2:
        			//1
        			imgWhatNewNext.setVisibility(View.INVISIBLE);
        			imgWhatNewPrev.setVisibility(View.INVISIBLE);
        			//2
        			imgFeaturesNext.setVisibility(View.VISIBLE);
        			imgFeaturesPrev.setVisibility(View.VISIBLE);
        			//3
        			imgTipsPrev.setVisibility(View.INVISIBLE);
        			break;
        		case 3:
        			//2
        			imgFeaturesNext.setVisibility(View.INVISIBLE);
        			imgFeaturesPrev.setVisibility(View.INVISIBLE);
        			//3
        			imgTipsPrev.setVisibility(View.VISIBLE);
        			break;
    			default:
    				break;
        		}
            }
        });
		
		guiView.findViewById(R.id.buttonGallery).setEnabled(false);
		guiView.findViewById(R.id.buttonShutter).setEnabled(false);
		guiView.findViewById(R.id.buttonSelectMode).setEnabled(false);
		
		Message msg2 = new Message();
		msg2.arg1 = PluginManager.MSG_CONTROL_LOCKED;
		msg2.what = PluginManager.MSG_BROADCAST;
		MainScreen.getMessageHandler().sendMessage(msg2);
		
		MainScreen.getGUIManager().lockControls = true;
		
		if (MainScreen.getInstance().showPromoRedeemed)
		{
			Toast.makeText(MainScreen.getInstance(), "The promo code has been successfully redeemed. All PRO-Features are unlocked", Toast.LENGTH_LONG).show();
			MainScreen.getInstance().showPromoRedeemed = false;
		}
		
		final RelativeLayout pagerLayout = ((RelativeLayout) guiView.findViewById(R.id.viewPagerLayout));
		pagerLayout.addView(viewPager);
		pagerLayout.setVisibility(View.VISIBLE);
		pagerLayout.bringToFront();
	}
	
	public void hideStore()
	{
		((RelativeLayout) guiView.findViewById(R.id.viewPagerLayout)).setVisibility(View.INVISIBLE);
		
		guiView.findViewById(R.id.buttonGallery).setEnabled(true);
		guiView.findViewById(R.id.buttonShutter).setEnabled(true);
		guiView.findViewById(R.id.buttonSelectMode).setEnabled(true);
		
		Message msg2 = new Message();
		msg2.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
		msg2.what = PluginManager.MSG_BROADCAST;
		MainScreen.getMessageHandler().sendMessage(msg2);
		
		MainScreen.getGUIManager().lockControls = false;
	}
	
	private void initStoreList() {
		storeViews.clear();
		buttonStoreViewAssoc.clear();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		boolean bOnSale = prefs.getBoolean("bOnSale", false);
		
		for (int i =0; i<6; i++) {

			LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
			View item = inflator.inflate(
					R.layout.gui_almalence_store_grid_element, null,
					false);
			ImageView icon = (ImageView) item.findViewById(R.id.storeImage);
			TextView description = (TextView) item.findViewById(R.id.storeText);
			TextView price = (TextView) item.findViewById(R.id.storePriceText);
			switch (i)
			{
				case 0:
					// unlock all
					icon.setImageResource(R.drawable.store_all);
					description.setText(MainScreen.getInstance().getResources().getString(R.string.Pref_Upgrde_All_Preference_Title));
					
					if(MainScreen.getInstance().isPurchasedAll())
						price.setText(R.string.already_unlocked);
					else
					{
						if (MainScreen.getInstance().isCouponSale())
						{
							price.setText(MainScreen.getInstance().titleUnlockAllCoupon);
							((ImageView) item.findViewById(R.id.storeSaleImage)).setVisibility(View.VISIBLE);
						}
						else
						{
							price.setText(MainScreen.getInstance().titleUnlockAll);
							if (bOnSale)
								((ImageView) item.findViewById(R.id.storeSaleImage)).setVisibility(View.VISIBLE);
						}
					}
					break;
				case 1:
					// HDR
					icon.setImageResource(R.drawable.store_hdr);
					description.setText(MainScreen.getInstance().getResources().getString(R.string.Pref_Upgrde_HDR_Preference_Title));
					if(MainScreen.getInstance().isPurchasedHDR() || MainScreen.getInstance().isPurchasedAll())
						price.setText(R.string.already_unlocked);
					else
						price.setText(MainScreen.getInstance().titleUnlockHDR);
					break;
				case 2:
					// Panorama
					icon.setImageResource(R.drawable.store_panorama);
					description.setText(MainScreen.getInstance().getResources().getString(R.string.Pref_Upgrde_Panorama_Preference_Title));
					if(MainScreen.getInstance().isPurchasedPanorama() || MainScreen.getInstance().isPurchasedAll())
						price.setText(R.string.already_unlocked);
					else
						price.setText(MainScreen.getInstance().titleUnlockPano);
					break;
				case 3:
					// Moving
					icon.setImageResource(R.drawable.store_moving);
					description.setText(MainScreen.getInstance().getResources().getString(R.string.Pref_Upgrde_Moving_Preference_Title));
					if(MainScreen.getInstance().isPurchasedMoving() || MainScreen.getInstance().isPurchasedAll())
						price.setText(R.string.already_unlocked);
					else
						price.setText(MainScreen.getInstance().titleUnlockMoving);
					break;
				case 4:
					// Groupshot
					icon.setImageResource(R.drawable.store_groupshot);
					description.setText(MainScreen.getInstance().getResources().getString(R.string.Pref_Upgrde_Groupshot_Preference_Title));
					if(MainScreen.getInstance().isPurchasedGroupshot() || MainScreen.getInstance().isPurchasedAll())
						price.setText(R.string.already_unlocked);
					else
						price.setText(MainScreen.getInstance().titleUnlockGroup);
					break;
				case 5:
					// Promo code
					icon.setImageResource(R.drawable.store_promo);
					description.setText(MainScreen.getInstance().getResources().getString(R.string.Pref_Upgrde_PromoCode_Preference_Title));
					if(MainScreen.getInstance().isPurchasedAll())
						price.setText(R.string.already_unlocked);
					else
						price.setText("");
					break;
				default:
    				break;
			}

			item.setOnTouchListener(new OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if(event.getAction() == MotionEvent.ACTION_CANCEL)
						purchasePressed(v);
					return false;
				}
			});
			
			item.setOnClickListener(new OnClickListener() 
			{
				public void onClick(View v) {
					// get inapp associated with pressed button
					purchasePressed(v);
				}
			});
			
			buttonStoreViewAssoc.put(item, i);
			storeViews.add(item);
		}

		storeAdapter.Elements = storeViews;
	}
	
	private void purchasePressed(View v)
	{
		// get inapp associated with pressed button
		Integer id = buttonStoreViewAssoc.get(v);
		switch (id)
		{
		case 0:// unlock all
			MainScreen.getInstance().purchaseAll();
			break;
		case 1:// HDR
			MainScreen.getInstance().purchaseHDR();
			break;
		case 2:// Panorama
			MainScreen.getInstance().purchasePanorama();
			break;
		case 3:// Moving
			MainScreen.getInstance().purchaseMoving();
			break;
		case 4:// Groupshot
			MainScreen.getInstance().purchaseGroupshot();
			break;
		case 5:// Promo
			MainScreen.getInstance().enterPromo();
			break;
		default:
			break;
		}
	}
	
	public void ShowUnlockControl()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		boolean bOnSale = prefs.getBoolean("bOnSale", false);
		final RotateImageView unlock = ((RotateImageView) guiView.findViewById(R.id.Unlock));
		unlock.setImageResource(bOnSale?R.drawable.unlock_sale:R.drawable.unlock);
		unlock.setAlpha(1.0f);
		unlock.setVisibility(View.VISIBLE);
		
		Animation invisible_alpha = new AlphaAnimation(1, 0.4f);
		invisible_alpha.setDuration(7000);
		invisible_alpha.setRepeatCount(0);

		invisible_alpha.setAnimationListener(new AnimationListener() 
		{
			@Override
			public void onAnimationEnd(Animation animation) {
				unlock.clearAnimation();
				unlock.setImageResource(R.drawable.unlock_gray);
				unlock.setAlpha(0.4f);
			}
			@Override
			public void onAnimationRepeat(Animation animation) {}
			@Override
			public void onAnimationStart(Animation animation) {}
		});

		unlock.startAnimation(invisible_alpha);
	}
	
	
	public void ShowGrayUnlockControl()
	{
		final RotateImageView unlock = ((RotateImageView) guiView.findViewById(R.id.Unlock));
		if (unlock.getVisibility() == View.VISIBLE)
			return;
		unlock.setImageResource(R.drawable.unlock_gray);
		unlock.setAlpha(0.4f);
		unlock.setVisibility(View.VISIBLE);
	}
	
	public void HideUnlockControl()
	{
		final RotateImageView unlock = ((RotateImageView) guiView.findViewById(R.id.Unlock));		
		unlock.setVisibility(View.GONE);
	}
	
	public void setOrientation()
	{
		((RotateImageView) guiView.findViewById(R.id.Unlock)).setOrientation(AlmalenceGUI.mDeviceOrientation);
	}
}