package hrider.ui;

import hrider.data.DataCell;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 8/28/12
 * Time: 4:29 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ChangeTrackerListener {
    void onItemChanged(DataCell cell);
}
