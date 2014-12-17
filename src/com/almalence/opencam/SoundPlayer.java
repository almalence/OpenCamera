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

import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

/**
 * Plays an AssetFileDescriptor, but does all the hard work on another thread so
 * that any slowness with preparing or loading doesn't block the calling thread.
 */
public class SoundPlayer {
	private static final String TAG = "SoundPlayer";
	private SoundPool mPlayer;
	private int soundStartId = 0;
	private AssetFileDescriptor mAfd;
	private int mAudioStreamType;
	AudioManager audio;

	@SuppressLint("NewApi")
	public SoundPlayer(Context mContext, AssetFileDescriptor afd1) {
		mAfd = afd1;
		mAudioStreamType = AudioManager.STREAM_SYSTEM;

		audio = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		if (mPlayer == null) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
				mPlayer = new SoundPool.Builder()
				.setMaxStreams(5)
				.setAudioAttributes(
						new AudioAttributes.Builder()
						.setLegacyStreamType(mAudioStreamType)
						.setContentType(
								AudioAttributes.CONTENT_TYPE_SONIFICATION)
								.build()).build();
			} else {
				mPlayer = new SoundPool(5, mAudioStreamType, 0);
			}
			
			soundStartId = mPlayer.load(mAfd, 1);
			try {
				mAfd.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mAfd = null;
		}
	}

	public void play() {
		switch (audio.getRingerMode()) {
		case AudioManager.RINGER_MODE_NORMAL:
			mPlayer.play(soundStartId, 1, 1, 0, 0, 1);
			break;
		case AudioManager.RINGER_MODE_SILENT:
			break;
		case AudioManager.RINGER_MODE_VIBRATE:
			break;
		default:
			break;
		}
	}
	
	public void release() {
		if (mAfd != null) {
			try {
				mAfd.close();
			} catch (IOException e) {
			}
			mAfd = null;
		}
		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}
	}
}