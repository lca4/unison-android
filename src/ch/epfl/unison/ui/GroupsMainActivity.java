package ch.epfl.unison.ui;

import java.util.HashSet;
import java.util.Set;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import ch.epfl.unison.AppData;
import ch.epfl.unison.Const;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.UnisonAPI;

/**
 * Activity that is displayed once you're inside the group. Displays the music
 * player and information about the group (through fragments).
 * 
 * @see AbstractMainActivity
 * 
 * @author lum
 */
public class GroupsMainActivity extends AbstractMainActivity {

	/** Simple interface to be notified about group info updates. */
	public interface OnGroupInfoListener {
		void onGroupInfo(JsonStruct.Group groupInfo);
	}
	private static final String TAG = "ch.epfl.unison.MainActivity";

	private static final double MAX_DISTANCE = 2000;

	private Set<OnGroupInfoListener> mListeners = new HashSet<OnGroupInfoListener>();

	private JsonStruct.Group mGroup;

	private void autoLeave() {
		AppData data = AppData.getInstance(this);
		Location currentLoc = data.getLocation();
		if (currentLoc != null && mGroup.lat != null && mGroup.lon != null
				&& mGroup.automatic) {
			double lat = currentLoc.getLatitude();
			double lon = currentLoc.getLongitude();
			float[] res = new float[1];

			Location.distanceBetween(mGroup.lat, mGroup.lon, lat, lon, res);
			double dist = res[0];

			if (dist > MAX_DISTANCE) {
				onKeyDown(KeyEvent.KEYCODE_BACK, null);
			}
		}
	}

	public void dispatchGroupInfo(JsonStruct.Group groupInfo) {
		for (OnGroupInfoListener listener : mListeners) {
			listener.onGroupInfo(groupInfo);
		}
	}

	public long getGroupId() {
		return mGroup.gid;
	}

	protected void handleExtras(Bundle extras) {
		if (extras == null || !extras.containsKey(Const.Strings.GROUP)) {
			// Should never happen. If it does, redirect the user to the groups
			// list.
			Log.d(TAG, "Tried creating mainActivity"
					+ " without coming from groupsActivity! Going to close");
			startActivity(new Intent(this, GroupsActivity.class));
			finish();
		} else {
			mGroup = (JsonStruct.Group) extras.get(Const.Strings.GROUP);
			Log.i(TAG, "joined group " + getGroupId());

			setTitle(mGroup.name);
			AppData.getInstance(this).addToHistory(mGroup);

		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTag(TAG);

		getTabsAdapter().addTab(
				getSupportActBar().newTab().setText(
						R.string.fragment_title_player),
				GroupsPlayerFragment.class, null);
		getTabsAdapter().addTab(
				getSupportActBar().newTab().setText(
						R.string.fragment_title_stats),
				GroupsStatsFragment.class, null);
	}

	private void onGroupInfo(JsonStruct.Group group) {
		setTitle(group.name);
	}

	@Override
	public void onRefresh() {
		super.onRefresh();
		UnisonAPI api = AppData.getInstance(this).getAPI();

		autoLeave();

		api.getGroupInfo(getGroupId(),
				new UnisonAPI.Handler<JsonStruct.Group>() {

					@Override
					public void callback(JsonStruct.Group struct) {
						try {
							GroupsMainActivity.this.onGroupInfo(struct);
							GroupsMainActivity.this.dispatchGroupInfo(struct);
							GroupsMainActivity.this.repaintRefresh(false);
						} catch (NullPointerException e) {
							Log.w(TAG, "group or activity is null?", e);
						}
					}

					@Override
					public void onError(UnisonAPI.Error error) {
						Log.d(TAG, error.toString());
						if (GroupsMainActivity.this != null) {
							Toast.makeText(GroupsMainActivity.this,
									R.string.error_loading_info,
									Toast.LENGTH_LONG).show();
							GroupsMainActivity.this.repaintRefresh(false);
						}
					}

				});
	}

	public void registerGroupInfoListener(OnGroupInfoListener listener) {
		mListeners.add(listener);
	}

	public void unregisterGroupInfoListener(OnGroupInfoListener listener) {
		mListeners.remove(listener);
	}
}
