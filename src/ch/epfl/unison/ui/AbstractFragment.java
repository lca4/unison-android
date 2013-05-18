package ch.epfl.unison.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ch.epfl.unison.R;
import com.actionbarsherlock.app.SherlockFragment;

/**
 * 
 * @author marc
 *
 */
public class AbstractFragment extends SherlockFragment {
    
    private static String smClassTag = "ch.epfl.unison.ui.AbstractFragment";

    private AbstractMainActivity mActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        smClassTag = this.getClass().getName();
        
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
        return smClassTag;
    }

}
