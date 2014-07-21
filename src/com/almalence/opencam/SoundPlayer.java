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

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

/**
 * Plays an AssetFileDescriptor, but does all the hard work on another thread so
 * that any slowness with preparing or loading doesn't block the calling thread.
 */
public class SoundPlayer implements Runnable
{
	private static final String	TAG			= "SoundPlayer";
	private Thread				mThread;
	private MediaPlayer			mPlayer;
	private int					mPlayCount	= 0;
	private boolean				mExit;
	private AssetFileDescriptor	mAfd;
	private int					mAudioStreamType;
	private Context				mContext;

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				AudioManager audio = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
				if (mPlayer == null)
				{
					MediaPlayer player = new MediaPlayer();
					player.setAudioStreamType(mAudioStreamType);
					player.setDataSource(mAfd.getFileDescriptor(), mAfd.getStartOffset(), mAfd.getLength());
					player.setLooping(false);
					player.prepare();
					mPlayer = player;
					mAfd.close();
					mAfd = null;
				}
				synchronized (this)
				{
					while (true)
					{
						if (mExit)
						{
							return;
						} else if (mPlayCount <= 0)
						{
							wait();
						} else
						{
							mPlayCount--;
							break;
						}
					}
				}
				switch (audio.getRingerMode())
				{
				case AudioManager.RINGER_MODE_NORMAL:
					mPlayer.start();
					break;
				case AudioManager.RINGER_MODE_SILENT:
					break;
				case AudioManager.RINGER_MODE_VIBRATE:
					break;
				default:
					break;
				}
			} catch (Exception e)
			{
				Log.e(TAG, "Error playing sound", e);
			}
		}
	}

	public SoundPlayer(Context mContext, AssetFileDescriptor afd)
	{
		this.mContext = mContext;

		mAfd = afd;
		mAudioStreamType = AudioManager.STREAM_MUSIC;
	}

	public SoundPlayer(Context mContext, AssetFileDescriptor afd, boolean enforceAudible)
	{
		this.mContext = mContext;

		mAfd = afd;
		if (enforceAudible)
		{
			mAudioStreamType = 7;
		} else
		{
			mAudioStreamType = AudioManager.STREAM_MUSIC;
		}
	}

	public void play()
	{
		if (mThread == null)
		{
			mThread = new Thread(this);
			mThread.start();
		}
		synchronized (this)
		{
			mPlayCount++;
			notifyAll();
		}
	}

	public void release()
	{
		if (mThread != null)
		{
			synchronized (this)
			{
				mExit = true;
				notifyAll();
			}
			try
			{
				mThread.join();
			} catch (InterruptedException e)
			{
			}
			mThread = null;
		}
		if (mAfd != null)
		{
			try
			{
				mAfd.close();
			} catch (IOException e)
			{
			}
			mAfd = null;
		}
		if (mPlayer != null)
		{
			mPlayer.release();
			mPlayer = null;
		}
	}
}