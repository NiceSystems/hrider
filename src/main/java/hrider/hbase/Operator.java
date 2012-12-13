package hrider.hbase;

import org.apache.hadoop.hbase.filter.CompareFilter;

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
 *          This enum represents an operator that is supported by the query on hbase.
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
    Greater,
    IsNull,
    IsNotNull;

    //region Public Methods

    /**
     * Indicates if the operator is an unary operator.
     *
     * @return True if the operator represents an unary operator or False otherwise.
     */
    public boolean isUnary() {
        return this == IsNull || this == IsNotNull;
    }

    /**
     * Gets a filter according to the operator type.
     * @return A filter to be used in query on hbase.
     */
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
            case IsNull:
                return CompareFilter.CompareOp.EQUAL;
            case IsNotNull:
                return CompareFilter.CompareOp.NOT_EQUAL;
            default:
                throw new IllegalArgumentException(String.format("The specified operator type '%s' is not supported.", this));
        }
    }
    //endregion
}
