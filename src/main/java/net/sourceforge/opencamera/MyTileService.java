package net.sourceforge.opencamera;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;
import android.util.Log;

/** Provides service for quick settings tile.
 */
@TargetApi(Build.VERSION_CODES.N)
public class MyTileService extends TileService {
    private static final String TAG = "MyTileService";
    public static final String TILE_ID = "net.sourceforge.opencamera.TILE_CAMERA";

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        if( MyDebug.LOG )
            Log.d(TAG, "onClick");
        super.onClick();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(TILE_ID);
        startActivity(intent);
    }
}
