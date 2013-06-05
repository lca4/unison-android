
package ch.epfl.unison.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import ch.epfl.unison.AppData;
import ch.epfl.unison.Const.SeedType;
import ch.epfl.unison.R;
import ch.epfl.unison.Uutils;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.JsonStruct.PlaylistsList;
import ch.epfl.unison.api.JsonStruct.TagsList;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;
import ch.epfl.unison.data.PlaylistItem;
import ch.epfl.unison.data.UnisonDB;

import com.actionbarsherlock.view.Menu;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Listing of the playlists.
 * 
 * @author marc bourqui
 */
public class SoloPlaylistsActivity extends AbstractFragmentActivity
        implements SoloPlaylistsLocalFragment.OnPlaylistsLocalListener,
        SoloPlaylistsRemoteFragment.OnPlaylistsRemoteListener {

    /** Possible fragments. */
    @SuppressLint("ValidFragment")
    // Avoids Lint wrong warning due to "Fragment" in the enum name
    protected enum ChildFragment {
        LOCAL, REMOTE, SHARED
    }

    private static final String TAG = "ch.epfl.unison.SoloPlaylistsActivity";
    private static final int RELOAD_INTERVAL = 15 * 60 * 1000; // in ms.

    private UnisonDB mDB;

    private HashMap<ChildFragment, String> mChildFragments;

    @SuppressLint("NewApi")
    // Concerns fragments' stuff
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDB = new UnisonDB(this);
        mChildFragments = new HashMap<SoloPlaylistsActivity.ChildFragment, String>();

        getTabsAdapter().addTab(
                getSupportActBar().newTab().setText(R.string.solo_fragment_playlists_local_title)
                        .setTag(getString(R.string.solo_playlists_fragment_local_tag)),
                SoloPlaylistsLocalFragment.class, null);
        getTabsAdapter().addTab(
                getSupportActBar().newTab().setText(R.string.solo_fragment_playlists_remote_title)
                        .setTag(getString(R.string.solo_playlists_fragment_remote_tag)),
                SoloPlaylistsRemoteFragment.class, null);

        setReloadInterval(RELOAD_INTERVAL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean b = super.onCreateOptionsMenu(menu);
        getMenu().findItem(R.id.menu_item_solo).setVisible(false);
        getMenu().add(
                Menu.NONE,
                R.id.solo_menu_create_playlist,
                1,
                R.string.solo_menu_create_playlist);
        SoloPlaylistsActivity.this.getSupportMenuInflater().inflate(
                R.menu.solo_playlists_menu,
                getMenu());
        return b;
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {

        switch (item.getItemId()) {
            case R.id.solo_menu_create_playlist:
                PickItemsDialogFragment pickSeedDialog = new PickItemsDialogFragment(SeedType.TAGS);
                pickSeedDialog.show(getSupportFragmentManager(), "seedTypes");
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        // startService(new Intent(LibraryService.ACTION_UPDATE));
    }

    @Override
    public void onRefresh() {
        repaintRefresh(true);

        // Update remote playlists
        UnisonAPI.Handler<JsonStruct.PlaylistsList> playlistsHandler =
                new UnisonAPI.Handler<JsonStruct.PlaylistsList>() {

                    @Override
                    public void callback(PlaylistsList struct) {
                        try {
                            if (struct.isEmtpy()) {
                                // TODO display row item to tell no playlist is
                                // available
                                Toast.makeText(SoloPlaylistsActivity.this,
                                        R.string.solo_playlists_noRemotePL,
                                        Toast.LENGTH_LONG).show();
                            } else {
                                refreshPlaylistsRemote(struct.toObject());
                                SoloPlaylistsActivity.this.repaintRefresh(false);
                            }
                        } catch (NullPointerException e) {
                            Log.w(TAG, "playlist or activity is null?", e);
                        }
                    }

                    @Override
                    public void onError(UnisonAPI.Error error) {
                        if (error != null) {
                            Log.d(TAG, error.toString());
                        }
                        if (SoloPlaylistsActivity.this != null) {
                            Toast.makeText(SoloPlaylistsActivity.this,
                                    R.string.error_loading_playlists,
                                    Toast.LENGTH_LONG).show();
                            SoloPlaylistsActivity.this.repaintRefresh(false);
                        }
                    }
                };

        // Update tags
        UnisonAPI.Handler<JsonStruct.TagsList> tagsHandler =
                new UnisonAPI.Handler<JsonStruct.TagsList>() {

                    @Override
                    public void callback(TagsList struct) {
                        for (int i = 0; i < struct.tags.length; i++) {
                            mDB.insert(struct.tags[i].getTagItem());
                        }
                    }

                    @Override
                    public void onError(UnisonAPI.Error error) {
                        if (error != null) {
                            Log.d(TAG, error.toString());
                        }
                        if (SoloPlaylistsActivity.this != null) {
                            Toast.makeText(SoloPlaylistsActivity.this,
                                    R.string.error_loading_tags,
                                    Toast.LENGTH_LONG).show();
                            SoloPlaylistsActivity.this.repaintRefresh(false);
                        }
                    }
                };

        AppData data = AppData.getInstance(this);
        data.getAPI().listUserPlaylists(data.getUid(), playlistsHandler);
        data.getAPI().listTopTags(data.getUid(), tagsHandler);
    }

    // /** Adapter used to populate the ListView listing the playlists.
    // * Kept for reference, may be remove later on.
    // * */
    // private class PlaylistsAdapter extends ArrayAdapter<PlaylistItem> {
    //
    // public static final int ROW_LAYOUT = R.layout.list_row;
    //
    // public PlaylistsAdapter(ArrayList<PlaylistItem> list) {
    // super(SoloPlaylistsActivity.this, 0, list);
    // }
    //
    // @Override
    // public View getView(int position, View view, ViewGroup parent) {
    // PlaylistItem playlist = getItem(position);
    // if (view == null) {
    // LayoutInflater inflater = (LayoutInflater) SoloPlaylistsActivity.this
    // .getSystemService(
    // Context.LAYOUT_INFLATER_SERVICE);
    // view = inflater.inflate(ROW_LAYOUT, parent, false);
    // }
    // ((TextView)
    // view.findViewById(R.id.listrow_title)).setText(playlist.getTitle());
    // String subtitle = null;
    // if (playlist.getListeners() > 0) {
    // String plural = "s";
    // if (playlist.getListeners() == 1) {
    // plural = "";
    // }
    // subtitle = String.format("%d tracks - %d listener" + plural,
    // playlist.getSize(),
    // playlist.getListeners());
    // } else {
    // subtitle = String.format("%d tracks", playlist.getSize());
    // }
    // ((TextView) view.findViewById(R.id.listrow_subtitle)).setText(subtitle);
    //
    // view.setTag(playlist);
    // return view;
    // }
    // }

    // /**
    // * When clicking on "create new group", trigger an AlertView that asks for
    // a
    // * group name and creates the group on the back-end through the API.
    // */
    // private class OnCreatePlaylistListener implements OnClickListener {
    //
    // @Override
    // public void onClick(View v) {
    //
    // /*
    // * TODO allow user to specify title. In order to achieve this, use a
    // * custom view, like the one found on
    // * http://prativas.wordpress.com/category
    // * /android/expandable-list-view-in-android/
    // */
    //
    // PickSeedDialogFragment pickSeedDialog = new PickSeedDialogFragment();
    // pickSeedDialog.show(getSupportFragmentManager(), "seedTypes");
    //
    // // AlertDialog.Builder alert = new
    // // AlertDialog.Builder(SoloPlaylistsActivity.this);
    // //
    // //
    // alert.setTitle(getString(R.string.solo_playlists_alert_newplaylist_title));
    // //
    // alert.setMessage(getString(R.string.solo_playlists_alert_newplaylist_message));
    // //
    // // // Set an EditText view to get user input
    // // final EditText input = new EditText(SoloPlaylistsActivity.this);
    // // alert.setView(input);
    // //
    // // // When clicking on "OK", create the group.
    // //
    // alert.setPositiveButton(getString(R.string.solo_playlists_alert_newplaylist_ok),
    // // new DialogInterface.OnClickListener() {
    // //
    // // @Override
    // // public void onClick(DialogInterface dialog, int whichButton) {
    // // String name = input.getText().toString().trim();
    // // OnCreatePlaylistListener.this.createPlaylist(name);
    // // }
    // // });
    // //
    // //
    // alert.setNegativeButton(getString(R.string.solo_playlists_alert_newplaylist_cancel),
    // // null);
    // // alert.show();
    // }
    //
    // /**
    // * @author marc
    // */
    // @SuppressLint({
    // "ValidFragment", "NewApi"
    // })
    // private class PickSeedDialogFragment extends
    // android.support.v4.app.DialogFragment {
    //
    // @Override
    // public Dialog onCreateDialog(Bundle savedInstanceState) {
    // AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    // builder.setTitle(R.string.solo_playlists_dialog_pick_seedtype);
    // builder.setItems(R.array.seedtypes, new DialogInterface.OnClickListener()
    // {
    // public void onClick(DialogInterface dialog, int which) {
    // // The 'which' argument contains the index position
    // // of the selected item
    // Resources res = getResources();
    // TypedArray seedsTypes = res.obtainTypedArray(R.array.seedtypes);
    // String seed = seedsTypes.getString(which);
    // seedsTypes.recycle();
    // if (seed != null) {
    // SeedType seedType = SeedType.getSeedType(seed);
    // PickItemsDialogFragment pickItemsDialog = new PickItemsDialogFragment(
    // seedType);
    // if (pickItemsDialog != null) {
    // pickItemsDialog.show(getFragmentManager(), seed);
    // }
    // }
    // }
    // });
    // return builder.create();
    // }
    // }
    //
    //
    // }

    /**
     * @author marc
     */
    @SuppressLint("ValidFragment")
    private class PickItemsDialogFragment extends android.support.v4.app.DialogFragment {
        private SeedType mType;
        private ArrayList<Integer> mSelectedItems;
        private LinkedHashMap<String, Integer> mItems;

        public PickItemsDialogFragment(SeedType type) {
            this.mType = type;
            this.mSelectedItems = new ArrayList<Integer>();
            switch (mType) {
                case TAGS:
                    mItems = mDB.getTags();
                    break;
                case TRACKS:
                    mItems = mDB.getLibEntries();
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (mItems != null) {
                final boolean[] checkedItems = new boolean[mItems.size()];
                for (int i = 0; i < checkedItems.length; i++) {
                    checkedItems[i] = false;
                }
                List<CharSequence> items = new ArrayList<CharSequence>();
                Set<String> keys = mItems.keySet();
                Iterator<String> it = keys.iterator();
                while (it.hasNext()) {
                    items.add(it.next());
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(
                        SoloPlaylistsActivity.this);
                builder.setTitle(R.string.solo_playlists_dialog_pick_seeds)
                        .setPositiveButton(R.string.generic_ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        Log.i(TAG, mSelectedItems.toString());
                                        mDB.setChecked(mType, mItems, checkedItems);
                                        generatePlaylist();
                                    }
                                })
                        .setNegativeButton(R.string.generic_cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        mSelectedItems.clear();
                                        mItems.clear();
                                    }
                                })
                        .setMultiChoiceItems(items.toArray(new CharSequence[items.size()]),
                                checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which,
                                            boolean isChecked) {
                                        if (isChecked) {
                                            checkedItems[which] = true;
                                            mSelectedItems.add(which);
                                        } else if
                                        (mSelectedItems.contains(which)) {
                                            // Else, if the item is already
                                            // in
                                            // the array, remove it
                                            checkedItems[which] = false;
                                            mSelectedItems.remove(Integer.valueOf(which));
                                        }
                                    }
                                });
                return builder.create();
            } else {
                return null;
            }
        }
    }

    /**
     * Creates a playlist on the back-end. If it succeeds, the ListView
     * containing the list of playlists is updated. If it fails, a toast
     * notification is shown.
     */
    @SuppressLint("NewApi")
    private void generatePlaylist() {
        JSONObject seeds = Uutils.merge(mDB.getCheckedItems(SeedType.TAGS),
                mDB.getCheckedItems(SeedType.TRACKS));

        if (seeds != null) {
            AppData data = AppData.getInstance(SoloPlaylistsActivity.this);
            JSONObject options = new JSONObject();
            data.getAPI().generatePlaylist(data.getUid(), seeds, options,
                    new UnisonAPI.Handler<JsonStruct.PlaylistJS>() {
                        @Override
                        public void callback(JsonStruct.PlaylistJS struct) {
                            if (struct != null) {
                                Log.i(TAG, "Playlist created!");
                                getPlaylistsRemoteFragment().add(0, struct.toObject());
                                // SoloPlaylistsActivity.this.mPlaylistsRemoteListView
                                // .setAdapter(new
                                // PlaylistsAdapter(mPlaylistsRemote));
                                // SoloPlaylistsActivity.this.mPlaylistsListRemote
                                // .setAdapter(new
                                // PlaylistsAdapter(struct.toObject()));
                            } else {
                                Log.i(TAG, "Playlist created, but could not be fetched...");
                            }
                        }

                        @Override
                        public void onError(Error error) {
                            if (error != null) {
                                Log.d(TAG, error.toString());
                            }
                            if (SoloPlaylistsActivity.this != null) {
                                Toast.makeText(SoloPlaylistsActivity.this,
                                        R.string.error_creating_playlist,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    private void refreshPlaylistsRemote(ArrayList<PlaylistItem> playlists) {
        getPlaylistsRemoteFragment().set(playlists);
    }

    protected UnisonDB getDB() {
        return mDB;
    }

    private SoloPlaylistsRemoteFragment getPlaylistsRemoteFragment() {
        return (SoloPlaylistsRemoteFragment) getSupportFragmentManager()
                .findFragmentByTag(mChildFragments.get(ChildFragment.REMOTE));
    }

    private SoloPlaylistsLocalFragment getPlaylistsLocalFragment() {
        return (SoloPlaylistsLocalFragment) getSupportFragmentManager()
                .findFragmentByTag(mChildFragments.get(ChildFragment.LOCAL));
    }

    @Override
    public boolean onSavePlaylist(PlaylistItem playlist) {
        return getPlaylistsLocalFragment().add(playlist);
    }

    @Override
    public void setPlaylistsRemoteFragmentTag(String tag) {
        mChildFragments.put(ChildFragment.REMOTE, tag);
    }

    @Override
    public boolean onDeletePlaylist(PlaylistItem playlist) {
        return getPlaylistsRemoteFragment().add(playlist);
    }

    @Override
    public void setPlaylistsLocalFragmentTag(String tag) {
        mChildFragments.put(ChildFragment.LOCAL, tag);
    }
}
