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

import android.app.ProgressDialog;
import android.content.Context;

public final class AsyncTaskManager implements IProgressTracker
{
    private final OnTaskCompleteListener mTaskCompleteListener;
    private ProgressDialog mProgressDialog;
    private Task mAsyncTask;

    public AsyncTaskManager(Context context, OnTaskCompleteListener taskCompleteListener)
    {
		// Save reference to complete listener (activity)
    	this.mTaskCompleteListener = taskCompleteListener;
		
		// Setup progress dialog
    	this.mProgressDialog = new ProgressDialog(context);
    	this.mProgressDialog.setIndeterminate(true);
    	this.mProgressDialog.setCancelable(false);
    }

    public void setupTask(Task asyncTask)
    {
		// Keep task
    	this.mAsyncTask = asyncTask;
		// Wire task to tracker (this)
    	this.mAsyncTask.setProgressTracker(this);
		// Start task
    	this.mAsyncTask.execute();
    }

    public void HideProgress()
    {
	    this.mProgressDialog.hide();
    }

    @Override
    public void onProgress(String message)
    {
    	if (this.mAsyncTask != null)
    	{
    		this.mAsyncTask.mProgressMessage = message;
    	}
    	
   		this.mProgressDialog.show();
		// Show current message in progress dialog
		this.mProgressDialog.setMessage(message);
    }

    public void cancel()
    {
    	if (this.mAsyncTask != null)
    	{
			// Cancel task
    		this.mAsyncTask.cancel(false);
			// Reset task
    		this.mAsyncTask = null;
    	}
    }
    
    @Override
    public void onComplete()
    {
    	// no idea how, but at the task completion mAsyncTask may happen to be already reset to null
    	// (looks like calling this method twice somehow)
		if (mAsyncTask != null)
		{
			// Notify activity about completion
			mTaskCompleteListener.onTaskComplete(mAsyncTask);
			// Reset task
			mAsyncTask = null;
		}
    }

    public Object retainTask()
    {
		// Detach task from tracker (this) before retain
		if (mAsyncTask != null)
		    mAsyncTask.setProgressTracker(null);
		// Retain task
		return mAsyncTask;
    }

    public void handleRetainedTask(Object instance)
    {
		// Restore retained task and attach it to tracker (this)
		if (instance instanceof Task)
		{
		    mAsyncTask = (Task) instance;
		    mAsyncTask.setProgressTracker(this);
		}
    }

    public boolean isWorking()
    {
		// Track current status
		return (mAsyncTask != null);
	}
}
