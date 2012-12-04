package hrider.ui;

import hrider.data.DataCell;
import hrider.data.DataRow;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 8/27/12
 * Time: 6:02 PM
 */
public class ChangeTracker {

    private Collection<DataRow> changes;
    private Collection<ChangeTrackerListener> listeners;

    public ChangeTracker() {
        this.changes = new ArrayList<DataRow>();
        this.listeners = new ArrayList<ChangeTrackerListener>();
    }

    public synchronized void addListener(ChangeTrackerListener listener) {
        this.listeners.add(listener);
    }

    public synchronized void removeListener(ChangeTrackerListener listener) {
        this.listeners.remove(listener);
    }

    public synchronized boolean hasChanges() {
        return !this.changes.isEmpty();
    }

    public synchronized void addChange(DataCell cell) {

        DataRow row = getRow(cell);
        if (row == null) {
            row = new DataRow(cell.getRow().getKey());

            this.changes.add(row);
        }

        row.addCell(cell);

        for (ChangeTrackerListener listener : this.listeners) {
            listener.onItemChanged(cell);
        }
    }

    public synchronized void removeChange(DataCell cell) {
        DataRow row = removeCell(cell);
        if (row != null && !row.hasCells()) {
            this.changes.remove(row);

            for (ChangeTrackerListener listener : this.listeners) {
                listener.onItemChanged(cell);
            }
        }
    }

    public synchronized Collection<DataRow> getChanges() {
        return new ArrayList<DataRow>(this.changes);
    }

    public synchronized void clear() {
        this.changes.clear();
    }

    private DataRow getRow(DataCell cell) {
        for (DataRow row : this.changes) {
            if (row.getKey().equals(cell.getRow().getKey())) {
                return row;
            }
        }
        return null;
    }

    private DataRow removeCell(DataCell cell) {
        for (DataRow row : this.changes) {
            if (row.getKey().equals(cell.getRow().getKey())) {
                row.removeCell(cell);
                return row;
            }
        }
        return null;
    }
}
