package ch.epfl.unison.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import ch.epfl.unison.AppData;
import ch.epfl.unison.Const;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;

public class AutoJoin {
    
//    private AutoJoin mAuto = null;
    
    private static final String TAG = "ch.epfl.unison.ui.AutoJoin";
    
    private Long mGid;
    private JsonStruct.Group mRetrievedGroup;
    private AppData mData = null;
    private Activity mActivity;
    private boolean mAlreadyInGroup = false;
    private boolean mAlreadyLoggedIn = false;
    
    public AutoJoin(Long gid, AppData data, Activity activity, boolean alreadyInGroup,
            boolean alreadyLoggedIn) {
        mGid = gid;
        mData = data;
        mActivity = activity;
        mAlreadyInGroup = alreadyInGroup;
        mAlreadyLoggedIn = alreadyLoggedIn;
    }
    
//    public static AutoJoin getInstance(Long gid, AppData data, Activity activity, boolean alreadyInGroup,
//            boolean alreadyLoggedIn) {
//        if (mAuto == null) {
//            mAuto = new AutoJoin(gid, data, activity, alreadyInGroup, alreadyLoggedIn)
//        }
//        return
//    }
    
    public  boolean joinByGID(Context ctx, Long gid) {
        mActivity = (Activity) ctx;
        AppData.getInstance(mActivity);
        setData(AppData.getInstance(mActivity));
        
        return true;
    }
    
    private  void fetchGroupByGID() {
        UnisonAPI api = mData.getAPI();
        
        api.getGroupInfo(mGid.longValue(), new UnisonAPI.Handler<JsonStruct.Group>() {

            @Override
            public void callback(JsonStruct.Group group) {
                if (!validGroup(group)) {
                    onError(null);
                    return;
                }
                mRetrievedGroup = group;
                if (mActivity != null) {
                    mActivity.startActivity(new Intent(mActivity, GroupsActivity.class)
                    .putExtra(Const.Strings.GROUP, mRetrievedGroup)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .setAction(GroupsActivity.ACTION_JOIN_GROUP));
                }
            }

            @Override
            public void onError(Error error) {
               if (error != null) {
                   Log.d(TAG, error.toString());
               } else {
                   Log.d(TAG, "received an error that was null (could be from an invalid group)");
               }
               if (mActivity != null) {
                   Toast.makeText(mActivity, "Received group was invalid", Toast.LENGTH_LONG).show();
               }
            }
        });
    }
    
    private  boolean validGroup(JsonStruct.Group group) {
        return group != null && group.gid != null;
    }
    
    private  void setData(AppData data) {
        mData = data;
    }
    
    private  void setGID(Long gid) {
        mGid = gid;
    }
}
