
package ch.epfl.unison.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import ch.epfl.unison.data.PlaylistLibraryService;
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

    /** Hosted fragments. */
    @SuppressLint("ValidFragment")
    // Avoids Lint wrong warning due to "Fragment" in the enum name
    private enum HostedFragment {
        LOCAL, REMOTE// , SHARED
    }

    private UnisonDB mDB;

    private HashMap<HostedFragment, String> mHostedFragments;

    @SuppressLint("NewApi")
    // Concerns fragments' stuff
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAutoRefresh(false);
        mDB = new UnisonDB(this);
        mHostedFragments = new HashMap<SoloPlaylistsActivity.HostedFragment, String>();

        getTabsAdapter().addTab(
                getSupportActBar().newTab().setText(R.string.solo_fragment_playlists_local_title)
                        .setTag(getString(R.string.solo_playlists_fragment_local_tag)),
                SoloPlaylistsLocalFragment.class, null);
        getTabsAdapter().addTab(
                getSupportActBar().newTab().setText(R.string.solo_fragment_playlists_remote_title)
                        .setTag(getString(R.string.solo_playlists_fragment_remote_tag)),
                SoloPlaylistsRemoteFragment.class, null);
        this.onRefresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean b = super.onCreateOptionsMenu(menu);
        getMenu().findItem(R.id.menu_item_solo).setVisible(false);
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
    protected void onResume() {
        super.onResume();
        // Starts the playlist libbrary service.
        startService(new Intent(PlaylistLibraryService.ACTION_UPDATE));
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
                                // Toast.makeText(SoloPlaylistsActivity.this,
                                // R.string.solo_playlists_noRemotePL,
                                // Toast.LENGTH_LONG).show();
                            } else {
                                refreshPlaylistsRemote(struct.toObject());
                            }
                        } catch (NullPointerException e) {
                            Log.w(getClassTag(), "playlist or activity is null?", e);
                        } finally {
                            SoloPlaylistsActivity.this.repaintRefresh(false);
                        }
                    }

                    @Override
                    public void onError(UnisonAPI.Error error) {
                        if (error != null) {
                            Log.d(getClassTag(), error.toString());
                        }
                        if (SoloPlaylistsActivity.this != null) {
                            if (error.hasJsonError()) {
                                switch (error.jsonError.error) {
                                    case UnisonAPI.ErrorCodes.IS_EMPTY:
                                        Toast.makeText(SoloPlaylistsActivity.this,
                                                R.string.error_loading_playlists_no_playlists,
                                                Toast.LENGTH_LONG).show();
                                        break;
                                    default:
                                        Toast.makeText(SoloPlaylistsActivity.this,
                                                R.string.error_loading_playlists,
                                                Toast.LENGTH_LONG).show();
                                        break;
                                }
                            } else {
                                Toast.makeText(SoloPlaylistsActivity.this,
                                        R.string.error_loading_playlists,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                        SoloPlaylistsActivity.this.repaintRefresh(false);
                    }
                };

        // Update tags
        UnisonAPI.Handler<JsonStruct.TagsList> tagsHandler =
                new UnisonAPI.Handler<JsonStruct.TagsList>() {

                    @Override
                    public void callback(TagsList struct) {
                        for (int i = 0; i < struct.tags.length; i++) {
                            mDB.getTagHandler().insert(struct.tags[i].getTagItem());
                        }
                        SoloPlaylistsActivity.this.repaintRefresh(false);
                    }

                    @Override
                    public void onError(UnisonAPI.Error error) {
                        if (error != null) {
                            Log.d(getClassTag(), error.toString());
                        }
                        if (SoloPlaylistsActivity.this != null) {
                            Toast.makeText(SoloPlaylistsActivity.this,
                                    R.string.error_loading_tags,
                                    Toast.LENGTH_LONG).show();
                        }
                        SoloPlaylistsActivity.this.repaintRefresh(false);
                    }
                };

        AppData data = AppData.getInstance(this);
        data.getAPI().listUserPlaylists(data.getUid(), playlistsHandler);
        data.getAPI().listTopTags(data.getUid(), tagsHandler);
    }

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
                    mItems = mDB.getTagHandler().getTags();
                    break;
                case TRACKS:
                    mItems = mDB.getTrackHandler().getLibEntries();
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
                                        Log.i(getClassTag(), mSelectedItems.toString());
                                        mDB.getTagHandler().setChecked(mType, mItems, checkedItems);
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
        JSONObject seeds = Uutils.merge(mDB.getTagHandler().getCheckedItems(SeedType.TAGS),
                mDB.getTagHandler().getCheckedItems(SeedType.TRACKS));

        if (seeds != null) {
            final AppData data = AppData.getInstance(SoloPlaylistsActivity.this);
            JSONObject options = new JSONObject();
            data.getAPI().generatePlaylist(data.getUid(), seeds, options,
                    new UnisonAPI.Handler<JsonStruct.PlaylistJS>() {
                        @Override
                        public void callback(JsonStruct.PlaylistJS struct) {
                            if (struct != null) {
                                Log.i(getClassTag(), "Playlist created!");
                                getPlaylistsRemoteFragment()
                                        .add(0, struct.toObject().setUserId(data.getUid()));
                            } else {
                                Log.i(getClassTag(),
                                        "Playlist created, but could not be fetched...");
                            }
                        }

                        @Override
                        public void onError(Error error) {
                            if (error != null) {
                                Log.d(getClassTag(), error.toString());
                            }
                            if (SoloPlaylistsActivity.this != null) {
                                if (error.hasJsonError()) {
                                    switch (error.jsonError.error) {
                                        case UnisonAPI.ErrorCodes.IS_EMPTY:
                                            Toast.makeText(SoloPlaylistsActivity.this,
                                                    R.string.error_creating_playlist_no_tracks,
                                                    Toast.LENGTH_LONG).show();
                                            break;
                                        case UnisonAPI.ErrorCodes.NO_TAGGED_TRACKS:
                                            Toast.makeText(SoloPlaylistsActivity.this,
                                                    R.string
                                                    .error_creating_playlist_no_tagged_tracks,
                                                    Toast.LENGTH_LONG).show();
                                            break;
                                        default:
                                            Toast.makeText(SoloPlaylistsActivity.this,
                                                    R.string.error_creating_playlist,
                                                    Toast.LENGTH_LONG).show();
                                            break;
                                    }
                                } else {
                                    Toast.makeText(SoloPlaylistsActivity.this,
                                            R.string.error_creating_playlist,
                                            Toast.LENGTH_LONG).show();
                                }
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
                .findFragmentByTag(mHostedFragments.get(HostedFragment.REMOTE));
    }

    private SoloPlaylistsLocalFragment getPlaylistsLocalFragment() {
        return (SoloPlaylistsLocalFragment) getSupportFragmentManager()
                .findFragmentByTag(mHostedFragments.get(HostedFragment.LOCAL));
    }

    @Override
    public boolean onSavePlaylist(PlaylistItem playlist) {
        return getPlaylistsLocalFragment().add(playlist);
    }

    @Override
    public void setPlaylistsRemoteFragmentTag(String tag) {
        mHostedFragments.put(HostedFragment.REMOTE, tag);
    }

    @Override
    public boolean onDeletePlaylist(PlaylistItem playlist) {
        return getPlaylistsRemoteFragment().add(playlist);
    }

    @Override
    public void setPlaylistsLocalFragmentTag(String tag) {
        mHostedFragments.put(HostedFragment.LOCAL, tag);
    }
}
