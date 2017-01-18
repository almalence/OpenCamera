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

package com.almalence.asynctaskmanager;

import android.os.AsyncTask;

public abstract class Task extends AsyncTask<Void, String, Boolean>
{
	private Boolean				mResult;
	public String				mProgressMessage;
	private IProgressTracker	mProgressTracker;

	/* UI Thread */
	public Task()
	{
	}

	/* UI Thread */
	public void setProgressTracker(IProgressTracker progressTracker)
	{
		// Attach to progress tracker
		this.mProgressTracker = progressTracker;
		// Initialize progress tracker with current task state
		if (this.mProgressTracker != null)
		{
			this.mProgressTracker.onProgress(this.mProgressMessage);
			if (this.mResult != null)
			{
				this.mProgressTracker.onComplete();
			}
		}
	}

	/* UI Thread */
	@Override
	protected void onCancelled()
	{
		// Detach from progress tracker
		this.mProgressTracker = null;
	}

	/* UI Thread */
	@Override
	protected void onProgressUpdate(String... values)
	{
		// Update progress message
		this.mProgressMessage = values[0];
		// And send it to progress tracker
		if (this.mProgressTracker != null)
		{
			this.mProgressTracker.onProgress(mProgressMessage);
		}
	}

	/* UI Thread */
	@Override
	protected void onPostExecute(Boolean result)
	{
		// Update result
		this.mResult = result;
		// And send it to progress tracker
		if (this.mProgressTracker != null)
		{
			this.mProgressTracker.onComplete();
		}

		// Detach from progress tracker
		this.mProgressTracker = null;
	}
}
