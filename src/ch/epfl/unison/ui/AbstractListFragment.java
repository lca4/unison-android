
package ch.epfl.unison.ui;

import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;

/**
 * Offers a fragment containing a list. A header can be displayed, with a title
 * and/or a subtitle. By default, the header is "gone".<br />
 * Child classes have implement an ArrayAdapter.
 * 
 * @see {@link ListFragment} What can be done there?
 * @author marc
 */
public abstract class AbstractListFragment extends SherlockListFragment {

    private String mClassTag = "ch.epfl.unison.ui.AbstractFragment";

    // private AbstractFragmentActivity mHostActivity;

    private RelativeLayout mHeader;
    private TextView mTitle;
    private TextView mSubtitle;
    private ListView mList;

    // @Override
    // public void onAttach(Activity activity) {
    // super.onAttach(activity);
    // mHostActivity = (AbstractFragmentActivity) activity;
    // }

    // @Override
    // public View onCreateView(LayoutInflater inflater, ViewGroup container,
    // Bundle savedInstanceState) {
    //
    // mClassTag = this.getClass().getName();
    //
    // View v = inflater.inflate(R.layout.list, container, false);
    // mHeader = (RelativeLayout) v.findViewById(R.id.list_header);
    // mTitle = (TextView) v.findViewById(R.id.list_title);
    // mSubtitle = (TextView) v.findViewById(R.id.list_subtitle);
    // mList = (ListView) v.findViewById(R.id.list_contentlist);
    //
    // return v;
    // }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    protected String getClassTag() {
        return mClassTag;
    }

    // protected AbstractFragmentActivity getHostActivity() {
    // return mHostActivity;
    // }

    // protected RelativeLayout getHeader() {
    // return mHeader;
    // }
    //
    // protected TextView getTitle() {
    // return mTitle;
    // }
    //
    // protected TextView getSubtitle() {
    // return mSubtitle;
    // }
    //
    // protected ListView getList() {
    // return mList;
    // }

}
