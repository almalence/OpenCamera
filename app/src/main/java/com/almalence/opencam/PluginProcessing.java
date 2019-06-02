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

public abstract class PluginProcessing extends Plugin
{
	protected String mode = null;
	
	public PluginProcessing(String ID, String mode, int preferenceID, int advancedPreferenceID, int quickControlID,
			String quickControlInitTitle)
	{
		super(ID, preferenceID, advancedPreferenceID, quickControlID, quickControlInitTitle);
		this.mode = mode;
	}

	@Override
	public abstract void onStartProcessing(long SessionID);

	@Override
	public abstract boolean isPostProcessingNeeded();

	@Override
	public abstract void onStartPostProcessing();
}
