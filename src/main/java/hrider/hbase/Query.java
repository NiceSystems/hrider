package hrider.hbase;

import hrider.data.ColumnType;

import java.util.Date;

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
 *          This class represents a query object containing all possible information that can be used to filter results on hbase.
 */
public class Query {

    //region Variables
    /**
     * A key to start the scan from.
     */
    private byte[]     startKey;
    /**
     * The type of the start key. The key must be converted to byte array before it can be used in scanner. In order for the scan to match
     * the key it must be converted to the byte array according to its actual type otherwise we will have different byte arrays which will never match.
     */
    private ColumnType startKeyType;
    /**
     * A key where the can should stop.
     */
    private byte[]     endKey;
    /**
     * The type of the end key.
     */
    private ColumnType endKeyType;
    /**
     * The date to start the scan from. Each key/value pair in hbase has a timestamp.
     */
    private Date       startDate;
    /**
     * The date to stop the scan.
     */
    private Date       endDate;
    /**
     * The name of the column family to scan on.
     */
    private String     family;
    /**
     * The name of the column to scan on.
     */
    private String     column;
    /**
     * Holds both column family and column names.
     */
    private String     columnWithFamily;
    /**
     * The operator to use if the scan is performed on values.
     */
    private Operator   operator;
    /**
     * A word to match in the scan.
     */
    private String     word;
    /**
     * The type of the word.
     */
    private ColumnType wordType;
    //endregion

    //region Public Properties

    /**
     * Gets a start key.
     *
     * @return A start key.
     */
    public byte[] getStartKey() {
        return this.startKey;
    }

    /**
     * Gets a start key represented as a {@link String}.
     *
     * @return A start key.
     */
    public String getStartKeyAsString() {
        if (this.startKeyType != null) {
            return this.startKeyType.toString(this.startKey);
        }
        return null;
    }

    /**
     * Sets a new start key.
     *
     * @param value A new start key.
     */
    public void setStartKey(byte[] value) {
        this.startKey = value;
    }

    /**
     * Sets a new start key and its type.
     *
     * @param keyType The type of the key to set.
     * @param key     The key to set.
     */
    public void setStartKey(ColumnType keyType, String key) {
        this.startKeyType = keyType;
        this.startKey = keyType.toBytes(key);
    }

    /**
     * Gets the start key type.
     *
     * @return The start key type.
     */
    public ColumnType getStartKeyType() {
        return this.startKeyType;
    }

    /**
     * Sets a new start key type.
     *
     * @param keyType A new start key type.
     */
    public void setStartKeyType(ColumnType keyType) {
        this.startKeyType = keyType;
    }

    /**
     * Gets an end key.
     *
     * @return An end key.
     */
    public byte[] getEndKey() {
        return this.endKey;
    }

    /**
     * Gets the end key represented as {@link String}.
     *
     * @return The end key.
     */
    public String getEndKeyAsString() {
        if (this.endKeyType != null) {
            return this.endKeyType.toString(this.endKey);
        }
        return null;
    }

    /**
     * Sets a new end key.
     *
     * @param value A new end key.
     */
    public void setEndKey(byte[] value) {
        this.endKey = value;
    }

    /**
     * Sets a new end key and its type.
     *
     * @param keyType The key type.
     * @param key     The key.
     */
    public void setEndKey(ColumnType keyType, String key) {
        this.endKeyType = keyType;
        this.endKey = keyType.toBytes(key);
    }

    /**
     * Gets the end key type.
     *
     * @return The end key type.
     */
    public ColumnType getEndKeyType() {
        return this.endKeyType;
    }

    /**
     * Sets a new key type.
     *
     * @param keyType A new key type.
     */
    public void setEndKeyType(ColumnType keyType) {
        this.endKeyType = keyType;
    }

    /**
     * Gets a start date.
     *
     * @return A start date.
     */
    public Date getStartDate() {
        return this.startDate;
    }

    /**
     * Sets a new start date.
     *
     * @param value A new start date.
     */
    public void setStartDate(Date value) {
        this.startDate = value;
    }

    /**
     * Gets an end date.
     *
     * @return An end date.
     */
    public Date getEndDate() {
        return this.endDate;
    }

    /**
     * Sets a new end date.
     *
     * @param value A new end date.
     */
    public void setEndDate(Date value) {
        this.endDate = value;
    }

    /**
     * Gets a column family.
     *
     * @return A column family.
     */
    public String getFamily() {
        return this.family;
    }

    /**
     * Sets a new column family.
     *
     * @param value A new column family.
     */
    public void setFamily(String value) {
        this.family = value;
    }

    /**
     * Gets a column name.
     *
     * @return A column name.
     */
    public String getColumn() {
        return this.column;
    }

    /**
     * Gets a column family and a column name concatenated as a single string.
     *
     * @return A column family and a column name represented as a single string.
     */
    public String getColumnWithFamily() {
        if (this.columnWithFamily == null) {
            this.columnWithFamily = String.format("%s:%s", this.family, this.column);
        }
        return this.columnWithFamily;
    }

    /**
     * Sets a new column name.
     *
     * @param value A new column name.
     */
    public void setColumn(String value) {
        this.column = value;
    }

    /**
     * Gets an operator.
     *
     * @return An operator.
     */
    public Operator getOperator() {
        return this.operator;
    }

    /**
     * Sets a new operator.
     *
     * @param value A new operator.
     */
    public void setOperator(Operator value) {
        this.operator = value;
    }

    /**
     * Gets a word.
     *
     * @return A word.
     */
    public String getWord() {
        return this.word;
    }

    /**
     * Gets a word represented as a byte array.
     *
     * @return A byte array.
     */
    public byte[] getWordAsByteArray() {
        return this.wordType.toBytes(this.word);
    }

    /**
     * Sets a new value for a word.
     *
     * @param value A new value to set.
     */
    public void setWord(String value) {
        if (value != null && !value.isEmpty()) {
            this.word = value;
        }
        else {
            this.word = null;
        }
    }

    /**
     * Gets a word type.
     *
     * @return A type of the word.
     */
    public ColumnType getWordType() {
        return this.wordType;
    }

    /**
     * Sets a new word type.
     *
     * @param wordType A new word type.
     */
    public void setWordType(ColumnType wordType) {
        this.wordType = wordType;
    }
    //endregion
}
