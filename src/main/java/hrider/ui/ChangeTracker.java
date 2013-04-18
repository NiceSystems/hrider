package hrider.ui;

import hrider.data.DataCell;
import hrider.data.DataRow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Copyright (C) 2012 NICE Systems ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Igor Cher
 * @version %I%, %G%
 *          <p/>
 *          This class represents a tracker for all changes performed on cells.
 */
public class ChangeTracker implements Serializable {

    //region Constants
    private static final long serialVersionUID = -7106669090052759327L;
    //endregion

    //region Variables
    /**
     * Holds the changed rows.
     */
    private Collection<DataRow> changes;
    /**
     * A list of components registered for change events.
     */
    private Collection<ChangeTrackerListener> listeners;
    //endregion

    //region Constructor

    /**
     * Initializes a new instance of the {@link ChangeTracker} class.
     */
    public ChangeTracker() {
        this.changes = new ArrayList<DataRow>();
        this.listeners = new ArrayList<ChangeTrackerListener>();
    }
    //endregion

    //region Public Methods

    /**
     * Adds a new listener to the list of registered listeners.
     * @param listener The listener to add.
     */
    public synchronized void addListener(ChangeTrackerListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Removes the listener from the list of registered listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeListener(ChangeTrackerListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Indicates if there is at least one changed row.
     * @return True if there is at least one changed row or False otherwise.
     */
    public synchronized boolean hasChanges() {
        return !this.changes.isEmpty();
    }

    /**
     * Adds a new cell that was updated.
     * @param cell A cell to add.
     */
    public synchronized void addChange(DataCell cell) {

        DataRow row = getRow(cell);
        if (row == null) {
            row = new DataRow(cell.getRow().getKey());

            this.changes.add(row);
        }

        row.addCell(cell);

        for (ChangeTrackerListener listener : this.listeners) {
            listener.onCellChanged(cell);
        }
    }

    /**
     * Removes the cell from the list of updated cells.
     * @param cell The cell to remove.
     */
    public synchronized void removeChange(DataCell cell) {
        DataRow row = removeCell(cell);
        if (row != null && !row.hasCells()) {
            this.changes.remove(row);

            for (ChangeTrackerListener listener : this.listeners) {
                listener.onCellChanged(cell);
            }
        }
    }

    /**
     * Gets all updated rows. The rows contain only updated cells and not the full data.
     * @return A list of rows that contain updated cells.
     */
    public synchronized Collection<DataRow> getChanges() {
        return new ArrayList<DataRow>(this.changes);
    }

    /**
     * Clears all changes.
     */
    public synchronized void clear() {
        this.changes.clear();
    }
    //endregion

    //region Private Methods

    /**
     * Gets a row by cell.
     * @param cell The cell to look for a row.
     * @return A row that contains the provided cell.
     */
    private DataRow getRow(DataCell cell) {
        for (DataRow row : this.changes) {
            if (row.getKey().equals(cell.getRow().getKey())) {
                return row;
            }
        }
        return null;
    }

    /**
     * Removes a cell from the row.
     * @param cell A cell to remove.
     * @return The row that contained the removed cell.
     */
    private DataRow removeCell(DataCell cell) {
        for (DataRow row : this.changes) {
            if (row.getKey().equals(cell.getRow().getKey())) {
                row.removeCell(cell);
                return row;
            }
        }
        return null;
    }
    //endregion
}
