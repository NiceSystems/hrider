package hrider.hbase;

import hrider.data.DataCell;
import hrider.data.DataRow;
import hrider.data.ObjectType;
import hrider.data.TypedObject;
import hrider.ui.MessageHandler;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 9/5/12
 * Time: 4:52 PM
 */
public class Scanner {

    private class Marker {

        private TypedObject         key;
        private Collection<DataRow> rows;

        private Marker(TypedObject key, Collection<DataRow> rows) {
            this.key = key;
            this.rows = rows;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Marker && ((Marker)obj).key.equals(this.key);
        }

        @Override
        public int hashCode() {
            return this.key.hashCode();
        }
    }

    //region Variables
    private TableFactory            factory;
    private String                  tableName;
    private long                    rowsCount;
    private Map<String, ObjectType> columnTypes;
    private Stack<Marker>           markers;
    private Collection<DataRow>     current;
    private Collection<String>      columns;
    //endregion

    //region Constructor
    public Scanner(TableFactory factory, String tableName) {
        this.factory = factory;
        this.tableName = tableName;
        this.rowsCount = 0;
        this.markers = new Stack<Marker>();
        this.columns = new ArrayList<String>();
    }
    //endregion

    //region Public Properties
    public String getTableName() {
        return this.tableName;
    }

    public Collection<String> getColumns(int rowsNumber) {
        if (this.columns.isEmpty()) {
            try {
                loadColumns(rowsNumber);
            }
            catch (IOException e) {
                MessageHandler.addError(String.format("Failed to load columns for table %s.", this.tableName), e);
            }
        }
        return this.columns;
    }

    public Map<String, ObjectType> getColumnTypes() {
        return this.columnTypes;
    }

    public void setColumnTypes(Map<String, ObjectType> columnTypes) {
        this.columnTypes = columnTypes;
    }

    public void updateColumnType(String columnName, ObjectType columnType) {
        Collection<DataRow> rows = this.current;
        if (rows != null) {
            for (DataRow row : rows) {
                row.updateColumnType(columnName, columnType);
            }
        }
    }
    //endregion

    //region Public Methods
    public void resetCurrent(TypedObject startKey) {
        this.current = null;
        this.rowsCount = 0;
        this.markers.clear();

        if (startKey != null) {
            this.markers.push(new Marker(startKey, new ArrayList<DataRow>()));
        }
    }

    public Collection<DataRow> current(int rowsNumber) throws IOException {
        if (this.current == null || this.current.size() != rowsNumber) {
            this.current = next(0, rowsNumber);
        }
        return this.current;
    }

    public Collection<DataRow> next(int rowsNumber) throws IOException {
        this.current = next(this.markers.isEmpty() ? 0 : 1, rowsNumber);
        return this.current;
    }

    public Collection<DataRow> prev() throws IOException {
        if (!this.markers.isEmpty()) {
            if (this.markers.size() > 1) {
                popMarker();
            }
            this.current = peekMarker().rows;
        }
        return this.current;
    }

    public long getRowsCount() throws IOException {
        if (this.rowsCount == 0) {
            Scan scan = getScanner();
            scan.setCaching(1000);
            scan.setBatch(1000);

            HTable table = this.factory.create(this.tableName);
            ResultScanner scanner = table.getScanner(scan);

            int count = 0;
            for (Result rr = scanner.next() ; rr != null ; rr = scanner.next()) {
                if (isValidRow(rr)) {
                    ++count;
                }
            }

            this.rowsCount = count;
        }
        return this.rowsCount;
    }
    //endregion

    //region Protected Methods
    protected Scan getScanner() throws IOException {
        return new Scan();
    }

    protected boolean isValidRow(Result result) {
        return true;
    }
    //endregion

    //region Private Methods
    protected TypedObject loadRows(ResultScanner scanner, int offset, int rowsNumber, Map<TypedObject, DataRow> rows) throws IOException {
        ObjectType keyType = this.columnTypes.get("key");

        int index = 0;
        boolean isValid;
        TypedObject key = null;

        do {
            Result result = scanner.next();

            isValid = result != null && rows.size() < rowsNumber;
            if (isValid && isValidRow(result)) {
                if (index >= offset) {
                    key = new TypedObject(keyType, result.getRow());

                    DataRow row = rows.get(key);
                    if (row == null) {
                        row = new DataRow(key);
                        row.addCell(new DataCell(row, "key", key));

                        rows.put(key, row);
                    }

                    NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> familyMap = result.getMap();
                    for (NavigableMap.Entry<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> family : familyMap.entrySet()) {
                        for (NavigableMap.Entry<byte[], NavigableMap<Long, byte[]>> qunitifer : family.getValue().entrySet()) {
                            String columnName = String.format("%s:%s", Bytes.toString(family.getKey()), Bytes.toString(qunitifer.getKey()));
                            if (this.columnTypes.containsKey(columnName)) {
                                ObjectType columnType = this.columnTypes.get(columnName);
                                for (NavigableMap.Entry<Long, byte[]> cell : qunitifer.getValue().entrySet()) {
                                    row.addCell(new DataCell(row, columnName, new TypedObject(columnType, cell.getValue())));
                                }
                            }

                            if (!this.columns.contains(columnName)) {
                                this.columns.add(columnName);
                            }
                        }
                    }
                }

                index++;
            }
        }
        while (isValid);

        return key;
    }

    private void loadColumns(int rowsNumber) throws IOException {
        HTable table = this.factory.create(this.tableName);

        ResultScanner scanner = table.getScanner(new Scan());
        Result row;

        int counter = 0;

        do {
            row = scanner.next();
            if (row != null) {
                NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> familyMap = row.getMap();
                for (NavigableMap.Entry<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> entry : familyMap.entrySet()) {
                    for (byte[] qunitifer : entry.getValue().keySet()) {
                        String columnName = String.format("%s:%s", Bytes.toString(entry.getKey()), Bytes.toString(qunitifer));
                        if (!this.columns.contains(columnName)) {
                            this.columns.add(columnName);
                        }
                    }
                }
            }

            counter++;
        }
        while (row != null && counter < rowsNumber);
    }

    private Collection<DataRow> next(int offset, int rowsNumber) throws IOException {
        Map<TypedObject, DataRow> rows = new HashMap<TypedObject, DataRow>();

        int itemsNumber = rowsNumber <= 1000 ? rowsNumber : 1000;

        Scan scan = getScanner();
        scan.setCaching(itemsNumber);
        scan.setBatch(itemsNumber);

        if (!this.markers.isEmpty()) {
            scan.setStartRow(peekMarker().key.toByteArray());
        }

        HTable table = this.factory.create(this.tableName);
        ResultScanner scanner = table.getScanner(scan);

        TypedObject lastKey = loadRows(scanner, offset, rowsNumber, rows);
        if (lastKey != null) {
            this.markers.push(new Marker(lastKey, rows.values()));
        }

        return rows.values();
    }

    private Marker popMarker() {
        if (!this.markers.isEmpty()) {
            return this.markers.pop();
        }
        return null;
    }

    private Marker peekMarker() {
        if (!this.markers.isEmpty()) {
            return this.markers.peek();
        }
        return null;
    }
    //endregion
}
