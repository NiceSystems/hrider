package hrider.filters;

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
 *          This class is a filter that represents an equation.
 */
public class EquationFilter implements Filter {

    //region Variables
    private EquationOperator operator;
    private Filter           left;
    private Filter           right;
    //endregion

    //region Constructor
    public EquationFilter(EquationOperator operator, Filter left, Filter right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }
    //endregion

    //region Public Methods
    public static Filter parse(String regex) {
        return parseRecursively(regex);
    }

    @Override
    public boolean match(String value) {
        switch (this.operator) {
            case AND:
                return left.match(value) && right.match(value);
            case OR:
                return left.match(value) || right.match(value);
            default:
                throw new IllegalArgumentException(String.format("The specified operator type '%s' is not supported.", this.operator));
        }
    }
    //endregion

    //region Private Methods
    private static Filter parseRecursively(String regex) {
        boolean isNegative = false;

        if (regex.startsWith("~")) {
            isNegative = true;
            regex = regex.substring(1);
        }

        if (regex.startsWith("(") && regex.endsWith(")")) {
            regex = regex.substring(1, regex.length() - 1);
        }

        if (regex.contains("AND") || regex.contains("OR")) {

            Filter left = null;
            Filter right = null;
            EquationOperator operator = EquationOperator.OR;

            for (int i = 0, count = 0, pos = 0 ; i < regex.length() ; i++) {
                char letter = regex.charAt(i);

                if (letter == '(' || letter == '~') {
                    if (pos == 0) {
                        pos = i;
                    }

                    if (letter == '(') {
                        count++;
                    }
                }
                else if (letter == ')') {
                    count--;

                    if (count == 0) {
                        Filter filter = parse(regex.substring(pos, i + 1));
                        if (left == null) {
                            left = filter;
                        }
                        else {
                            right = filter;
                        }
                    }
                }

                if (count == 0) {
                    if (letter == 'A') {
                        operator = EquationOperator.AND;
                        i += 2;
                    }
                    else if (letter == 'O') {
                        operator = EquationOperator.OR;
                        i++;
                    }
                }
            }

            return new EquationFilter(operator, left, right);
        }

        return new PatternFilter(regex, isNegative);
    }
    //endregion
}
