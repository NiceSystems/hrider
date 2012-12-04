package hrider.hbase;

import org.apache.hadoop.hbase.filter.CompareFilter;

/**
 * Created with IntelliJ IDEA.
 * User: igorc
 * Date: 29/11/12
 * Time: 11:19
 */
public enum Operator {
    Contains,
    StartsWith,
    EndsWith,
    Less,
    LessOrEqual,
    Equal,
    NotEqual,
    GreaterOrEqual,
    Greater;

    public CompareFilter.CompareOp toFilter() {
        switch (this) {
            case Contains:
            case StartsWith:
            case EndsWith:
                return CompareFilter.CompareOp.EQUAL;
            case Less:
                return CompareFilter.CompareOp.LESS;
            case LessOrEqual:
                return CompareFilter.CompareOp.LESS_OR_EQUAL;
            case Equal:
                return CompareFilter.CompareOp.EQUAL;
            case NotEqual:
                return CompareFilter.CompareOp.NOT_EQUAL;
            case GreaterOrEqual:
                return CompareFilter.CompareOp.GREATER_OR_EQUAL;
            case Greater:
                return CompareFilter.CompareOp.GREATER;
            default:
                throw new IllegalArgumentException(String.format("The specified operator type '%s' is not supported.", this));
        }
    }
}
