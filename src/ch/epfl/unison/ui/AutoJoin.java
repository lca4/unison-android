
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

    // private AutoJoin mAuto = null;

    private static final String TAG = "ch.epfl.unison.ui.AutoJoin";

    // private Long mGid;
    private JsonStruct.Group mRetrievedGroup;
    private AppData mData = null;
    private Activity mActivity;

    // private boolean mAlreadyInGroup = false;
    // private boolean mAlreadyLoggedIn = false;

    private enum State {
        NotLoggedIn,
        LoggedInNotInGroup,
        LoggedInInGroup,
        LoggedInSoloMode
    };

    public AutoJoin(AppData data, Activity activity/*
                                                    * , boolean alreadyInGroup,
                                                    * boolean alreadyLoggedIn
                                                    */) {
        // mGid = gid;
        mData = data;
        mActivity = activity;
        // mAlreadyInGroup = alreadyInGroup;
        // mAlreadyLoggedIn = alreadyLoggedIn;
    }

    // public static AutoJoin getInstance(Long gid, AppData data, Activity
    // activity, boolean alreadyInGroup,
    // boolean alreadyLoggedIn) {
    // if (mAuto == null) {
    // mAuto = new AutoJoin(gid, data, activity, alreadyInGroup,
    // alreadyLoggedIn)
    // }
    // return
    // }

    public boolean joinByGID(Long gid) {
        // if (mDa)
        if (gid == null) {
            return false;
        }

        fetchInfoForJoin(gid);

        return true;
    }

    private State checkState() {
        State state = State.NotLoggedIn;
        if (mData != null) {
            if (mData.getLoggedIn()) {
                state = State.LoggedInNotInGroup;
            }
            if (mData.getInGroup()) {
                state = State.LoggedInInGroup;
            }
        }
        return state;
    }

    private void successNextActivity() {
        State currentState = checkState();
        if (mActivity == null) {
            return;
        }
        switch (currentState) {
            case NotLoggedIn:
                mActivity.startActivity(new Intent(mActivity, LoginActivity.class)
                        .putExtra(Const.Strings.GROUP, mRetrievedGroup)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .setAction(LoginActivity.ACTION_JOIN_GROUP));
                break;
            case LoggedInNotInGroup:
                mActivity.startActivity(new Intent(mActivity, GroupsActivity.class)
                        .putExtra(Const.Strings.GROUP, mRetrievedGroup)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .setAction(GroupsActivity.ACTION_JOIN_GROUP));
                break;
            case LoggedInInGroup:
                mActivity.startActivity(new Intent(mActivity, GroupsActivity.class)
                        .putExtra(Const.Strings.GROUP, mRetrievedGroup)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .setAction(GroupsActivity.ACTION_LEAVE_JOIN_GROUP));
                break;
            case LoggedInSoloMode:
                // TODO
                break;
            default:
                break;
        }

        mActivity.finish();
    }

    private void errorNextActivity() {
        State currentState = checkState();
        if (mActivity == null) {
            return;
        }
        switch (currentState) {
            case NotLoggedIn:
                mActivity.startActivity(new Intent(mActivity, LoginActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                break;
            case LoggedInNotInGroup:
                mActivity.startActivity(new Intent(mActivity, GroupsActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                break;
            case LoggedInInGroup:
                mActivity.startActivity(new Intent(mActivity, GroupsMainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                break;
            case LoggedInSoloMode:
                // TODO
                break;
            default:
                break;
        }
    }

    private void fetchInfoForJoin(Long gid) {
        UnisonAPI api = mData.getAPI();

        api.getGroupInfo(gid.longValue(), new UnisonAPI.Handler<JsonStruct.Group>() {

            @Override
            public void callback(JsonStruct.Group group) {
                if (!validGroup(group)) {
                    onError(null);
                    return;
                }
                mRetrievedGroup = group;
                if (mActivity != null) {
                    successNextActivity();
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
                    errorNextActivity();
                    Toast.makeText(mActivity, "Received group was invalid", Toast.LENGTH_LONG)
                            .show();
                    mActivity.finish();
                }
            }
        });
    }

    private boolean validGroup(JsonStruct.Group group) {
        return group != null && group.gid != null;
    }

    // private void setData(AppData data) {
    // mData = data;
    // }

    // private void setGID(Long gid) {
    // mGid = gid;
    // }
}
