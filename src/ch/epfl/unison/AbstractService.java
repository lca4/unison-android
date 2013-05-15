package ch.epfl.unison;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * 
 * @author marc
 *
 */
abstract class AbstractService extends Service {
    
    private boolean mIsUpdating;
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    protected boolean isUpdating() {
        return mIsUpdating;
    }
    
    protected void setIsUpdating(boolean isUpdating) {
        mIsUpdating = isUpdating;
    }

}
