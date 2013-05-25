package ch.epfl.unison.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * WORK IN PROGRESS.
 * 
 * To be used later in SoloPlaylistsActivity, one fragment per playlist type (local and remote). And
 * potentially any other activity that requires fragments.
 * 
 * @author marc
 *
 */
public class AbstractFragment extends SherlockFragment {
    
    private String mClassTag = "ch.epfl.unison.ui.AbstractFragment";

    private AbstractMainActivity mActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mClassTag = this.getClass().getName();
        
        View v = inflater.inflate(R.layout.list, container, false);

        return v;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (AbstractMainActivity) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
    
    protected String getCName() {
        return mClassTag;
    }

}
