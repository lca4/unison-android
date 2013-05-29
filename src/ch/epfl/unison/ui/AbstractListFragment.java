
package ch.epfl.unison.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import ch.epfl.unison.R;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * WORK IN PROGRESS. Offers a fragment containing a list. A header can be
 * displayed, with a title and/or a subtitle. By default, the header is "gone".
 * 
 * @author marc
 */
public class AbstractListFragment extends SherlockFragment {

    private String mClassTag = "ch.epfl.unison.ui.AbstractFragment";

    private AbstractMainActivity mMainActivity;

    private RelativeLayout mHeader;
    private TextView mTitle;
    private TextView mSubtitle;
    private ListView mList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mClassTag = this.getClass().getName();

        View v = inflater.inflate(R.layout.list, container, false);
        mHeader = (RelativeLayout) v.findViewById(R.id.listHeader);
        mTitle = (TextView) v.findViewById(R.id.listTitle);
        mSubtitle = (TextView) v.findViewById(R.id.listSubTitle);
        mList = (ListView) v.findViewById(R.id.listList);

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mMainActivity = (AbstractMainActivity) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    protected String getClassTag() {
        return mClassTag;
    }

    protected AbstractMainActivity getMainActivity() {
        return mMainActivity;
    }

    protected RelativeLayout getHeader() {
        return mHeader;
    }

    protected TextView getTitle() {
        return mTitle;
    }

    protected TextView getSubtitle() {
        return mSubtitle;
    }

    protected ListView getList() {
        return mList;
    }

}
