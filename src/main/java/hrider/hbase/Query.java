package hrider.hbase;

import hrider.data.ObjectType;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 8/27/12
 * Time: 1:26 PM
 */
public class Query {

    private byte[]     startKey;
    private ObjectType startKeyType;
    private byte[]     endKey;
    private ObjectType endKeyType;
    private Date       startDate;
    private Date       endDate;
    private String     family;
    private String     column;
    private String     columnWithFamily;
    private Operator   operator;
    private String     word;
    private ObjectType wordType;

    public byte[] getStartKey() {
        return this.startKey;
    }

    public String getStartKeyAsString() {
        if (this.startKeyType != null) {
            return this.startKeyType.fromByteArray(this.startKey).toString();
        }
        return null;
    }

    public void setStartKey(byte[] value) {
        this.startKey = value;
    }

    public void setStartKey(ObjectType keyType, String key) {
        this.startKeyType = keyType;
        this.startKey = keyType.fromString(key);
    }

    public ObjectType getStartKeyType() {
        return this.startKeyType;
    }

    public void setStartKeyType(ObjectType keyType) {
        this.startKeyType = keyType;
    }

    public byte[] getEndKey() {
        return this.endKey;
    }

    public String getEndKeyAsString() {
        if (this.endKeyType != null) {
            return this.endKeyType.fromByteArray(this.endKey).toString();
        }
        return null;
    }

    public void setEndKey(byte[] value) {
        this.endKey = value;
    }

    public void setEndKey(ObjectType keyType, String key) {
        this.endKeyType = keyType;
        this.endKey = keyType.fromString(key);
    }

    public ObjectType getEndKeyType() {
        return this.endKeyType;
    }

    public void setEndKeyType(ObjectType keyType) {
        this.endKeyType = keyType;
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public void setStartDate(Date value) {
        this.startDate = value;
    }

    public Date getEndDate() {
        return this.endDate;
    }

    public void setEndDate(Date value) {
        this.endDate = value;
    }

    public String getFamily() {
        return this.family;
    }

    public void setFamily(String value) {
        this.family = value;
    }

    public String getColumn() {
        return this.column;
    }

    public String getColumnWithFamily() {
        if (this.columnWithFamily == null) {
            this.columnWithFamily = String.format("%s:%s", this.family, this.column);
        }
        return this.columnWithFamily;
    }

    public void setColumn(String value) {
        this.column = value;
    }

    public Operator getOperator() {
        return this.operator;
    }

    public void setOperator(Operator value) {
        this.operator = value;
    }

    public String getWord() {
        return this.word;
    }

    public byte[] getWordAsByteArray() {
        return this.wordType.fromString(this.word);
    }

    public void setWord(String value) {
        this.word = value;
    }

    public ObjectType getWordType() {
        return this.wordType;
    }

    public void setWordType(ObjectType wordType) {
        this.wordType = wordType;
    }
}
