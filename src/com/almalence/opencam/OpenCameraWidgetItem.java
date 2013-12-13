package com.almalence.opencam;

public class OpenCameraWidgetItem {
	public int    modeIconID;
    public String modeName;
    public boolean isTorchOn; 

    public OpenCameraWidgetItem(String name, int icon_id, boolean torchOn) {
        this.modeName = name;
        this.modeIconID = icon_id;
        this.isTorchOn = torchOn;
        
    }
}