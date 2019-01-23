package io.ferdon.statespace;

import org.apache.commons.lang.StringEscapeUtils;
import org.checkerframework.common.value.qual.StringVal;

import java.util.*;

class Interpreter {

    public enum ValueType {
        STRING,
        INTEGER,
        BOOLEAN,
        REAL,
        VARIABLE
    }

    public enum OperationType {
        ADD, SUB, MUL, DIV, MOD,
        AND, NOT, OR, XOR,ISTRUE, ISFALSE,
        EQ, NEQ, GT, GTE, LT, LTE,
        SUBSTR, APPEND, ISEMPTY, TRIM,
        IF
    }

    interface Value {
        int getInt();
        double getReal();
        boolean getBoolean();
        String getString();

        String toString();
    }

    interface ArithmeticValue extends Value {
        ArithmeticValue add(ArithmeticValue x);
        ArithmeticValue sub(ArithmeticValue x);
        ArithmeticValue mul(ArithmeticValue x);
        ArithmeticValue div(ArithmeticValue x);
        ArithmeticValue mod(ArithmeticValue x);
    }

    interface BooleanValue extends Value {
        BooleanValue and(BooleanValue x);
        BooleanValue not();
        BooleanValue or(BooleanValue x);
        BooleanValue xor(BooleanValue x);

        BooleanValue isTrue();
        BooleanValue isFalse();
    }

    interface StringValue extends Value {
        BooleanExpression isEmpty();
        StringValue trim();
        StringValue append(StringValue x);
        StringValue substr(IntegerExpression startPos, IntegerExpression endPos);
    }

    interface ComparableValue<T> {
        BooleanExpression isEqual(T x);
        BooleanExpression isNotEqual(T x);
        BooleanExpression isGreater(T x);
        BooleanExpression isGreaterOrEqual(T x);
        BooleanExpression isLess(T x);
        BooleanExpression isLessOrEqual(T x);
    }

    class IntegerExpression implements ArithmeticValue, ComparableValue<ArithmeticValue> {
        private int value;

        IntegerExpression(String x) {
            value = Integer.parseInt(x);
        }

        IntegerExpression(int x) {
            value = x;
        }

        public int getInt() {
            return value;
        }

        public double getReal() {
            return (double) value;
        }

        public boolean getBoolean() {
            return value != 0;
        }

        public String getString() {
            return String.valueOf(value);
        }

        public ArithmeticValue add(ArithmeticValue x) {
            return new IntegerExpression(this.value + x.getInt());
        }

        public ArithmeticValue sub(ArithmeticValue x) {
            return new IntegerExpression(this.value - x.getInt());
        }

        public ArithmeticValue mul(ArithmeticValue x) {
            return new IntegerExpression(this.value * x.getInt());
        }

        public ArithmeticValue div(ArithmeticValue x) {
            return new IntegerExpression(this.value / x.getInt());
        }

        public ArithmeticValue mod(ArithmeticValue x) {
            return new IntegerExpression(this.value % x.getInt());
        }

        public BooleanExpression isEqual(ArithmeticValue x) {
            return new BooleanExpression(this.value == x.getInt());
        };

        public BooleanExpression isNotEqual(ArithmeticValue x) {
            return new BooleanExpression(this.value != x.getInt());
        };

        public BooleanExpression isGreater(ArithmeticValue x) {
            return new BooleanExpression(this.value > x.getInt());
        }

        public BooleanExpression isGreaterOrEqual(ArithmeticValue x) {
            return new BooleanExpression(this.value >= x.getInt());
        }

        public BooleanExpression isLess(ArithmeticValue x) {
            return new BooleanExpression(this.value < x.getInt());
        }

        public BooleanExpression isLessOrEqual(ArithmeticValue x) {
            return new BooleanExpression(this.value <= x.getInt());
        }

        @Override
        public String toString() {
            return String.format("IntegerExpression: %s", this.value);
        }
    }

    class RealExpression implements ArithmeticValue, ComparableValue<ArithmeticValue> {
        private double value;

        RealExpression(String x) {
            value = Double.parseDouble(x);
        }

        RealExpression (double x) {
            value = x;
        }

        public int getInt() {
            return (int) value;
        }

        public double getReal() {
            return value;
        }

        public boolean getBoolean() { return value != 0.000; }

        public String getString() { return String.valueOf(value); }

        public ArithmeticValue add(ArithmeticValue x) {
            return new RealExpression(this.value + x.getReal());
        }

        public ArithmeticValue sub(ArithmeticValue x) {
            return new RealExpression(this.value - x.getReal());
        }

        public ArithmeticValue mul(ArithmeticValue x) {
            return new RealExpression(this.value * x.getReal());
        }

        public ArithmeticValue div(ArithmeticValue x) {
            return new RealExpression(this.value / x.getReal());
        }

        public ArithmeticValue mod(ArithmeticValue x) {
            return new RealExpression(this.value % x.getReal());
        }

        public BooleanExpression isEqual(ArithmeticValue x) {
            return new BooleanExpression(this.value == x.getReal());
        }

        public BooleanExpression isNotEqual(ArithmeticValue x) {
            return new BooleanExpression(this.value != x.getReal());
        }

        public BooleanExpression isGreater(ArithmeticValue x) {
            return new BooleanExpression(this.value > x.getReal());
        }

        public BooleanExpression isGreaterOrEqual(ArithmeticValue x) {
            return new BooleanExpression(this.value >= x.getReal());
        }

        public BooleanExpression isLess(ArithmeticValue x) {
            return new BooleanExpression(this.value < x.getReal());
        }

        public BooleanExpression isLessOrEqual(ArithmeticValue x) {
            return new BooleanExpression(this.value <= x.getReal());
        }

        @Override
        public String toString() {
            return String.format("RealExpression: %s", this.value);
        }
    }

    class StringExpression implements StringValue, ComparableValue<StringValue> {
        private String value;

        StringExpression(String x) {
            value = x;
        }

        public BooleanExpression isEmpty() {
            return new BooleanExpression(value.length() - 2 == 0);
        }

        public StringValue trim() {
            return new StringExpression(value.substring(1, value.length() - 2).trim());
        }

        public StringValue append(StringValue x) {
            String str1 = value.substring(0, value.length() - 1);
            String str2 = x.getString().substring(1, value.length() - 2);
            return new StringExpression(str1 + str2);
        }

        public StringValue substr(IntegerExpression startPos, IntegerExpression endPos) {
            return new StringExpression(value.substring(startPos.getInt(), endPos.getInt()));
        }

        public int getInt() {
            return Integer.parseInt(value);
        }

        public double getReal() {
            return Double.parseDouble(value);
        }

        public boolean getBoolean() {
            return Boolean.parseBoolean(value);
        }

        public String getString() {
            return value;
        }

        public BooleanExpression isEqual(StringValue x) {
            return new BooleanExpression(this.value.equals(x.getString()));
        }

        public BooleanExpression isNotEqual(StringValue x) {
            return new BooleanExpression(!this.value.equals(x.getString()));
        }

        public BooleanExpression isGreater(StringValue x) {
            return new BooleanExpression(false);
        }

        public BooleanExpression isGreaterOrEqual(StringValue x) {
            return new BooleanExpression(false);
        }

        public BooleanExpression isLess(StringValue x) {
            return new BooleanExpression(false);
        }

        public BooleanExpression isLessOrEqual(StringValue x) {
            return new BooleanExpression(false);
        }

        @Override
        public String toString() {
            return String.format("StringExpression: %s\n", this.value);
        }
    }

    class BooleanExpression implements BooleanValue, ComparableValue<BooleanValue> {
        private boolean value;

        BooleanExpression(String x) {
            value = Boolean.parseBoolean(x);
        }

        BooleanExpression(boolean x) {
            value = x;
        }

        public int getInt() {
            return (value) ? 1 : 0;
        }

        public double getReal() {
            return (value) ? 1 : 0;
        }

        public boolean getBoolean() {
            return value;
        }

        public String getString() {
            return String.valueOf(value);
        }

        public BooleanValue and(BooleanValue x) {
            return new BooleanExpression(this.value && x.getBoolean());
        }

        public BooleanValue not() {
            return new BooleanExpression(!this.value);
        }

        public BooleanValue or(BooleanValue x) {
            return new BooleanExpression(this.value || x.getBoolean());
        }

        public BooleanValue xor(BooleanValue x) {
            return new BooleanExpression(this.value ^ x.getBoolean());
        }

        public BooleanValue isTrue() {
            return new BooleanExpression(this.value);
        }

        public BooleanValue isFalse() {
            return new BooleanExpression(!this.value);
        }

        public BooleanExpression isEqual(BooleanValue x) {
            return new BooleanExpression(this.value == x.getBoolean());
        }

        public BooleanExpression isNotEqual(BooleanValue x) {
            return new BooleanExpression(this.value != x.getBoolean());
        }

        public BooleanExpression isGreater(BooleanValue x) {
            return new BooleanExpression(false);
        }

        public BooleanExpression isGreaterOrEqual(BooleanValue x) {
            return new BooleanExpression(false);
        }

        public BooleanExpression isLess(BooleanValue x) {
            return new BooleanExpression(false);
        }

        public BooleanExpression isLessOrEqual(BooleanValue x) {
            return new BooleanExpression(false);
        }

        @Override
        public String toString() {
            return String.format("BooleanExpression: %s", value);
        }
    }

    /*
    * operator: operation name ~> operationType
    * variables: variable name ~> variable value
    * valueStack: stack for store postfix operands
    * */
    private static Map<String, OperationType> operators = new HashMap<>();
    private Map<String, String> variables = new HashMap<>();
    private Stack<Object> valueStack = new Stack<>();

    Interpreter() {
        operators.put("+", OperationType.ADD);
        operators.put("-", OperationType.SUB);
        operators.put("*", OperationType.MUL);
        operators.put("/", OperationType.DIV);
        operators.put("%", OperationType.MOD);

        operators.put("&&", OperationType.AND);
        operators.put("!", OperationType.NOT);
        operators.put("||", OperationType.OR);
        operators.put("^", OperationType.XOR);
        operators.put("isTrue", OperationType.ISTRUE);
        operators.put("isFalse", OperationType.ISFALSE);

        operators.put("substr", OperationType.SUBSTR);
        operators.put("append", OperationType.APPEND);
        operators.put("isEmpty", OperationType.ISEMPTY);
        operators.put("trim", OperationType.TRIM);

        operators.put("==", OperationType.EQ);
        operators.put("!=", OperationType.NEQ);
        operators.put(">", OperationType.GT);
        operators.put(">=", OperationType.GTE);
        operators.put("<", OperationType.LT);
        operators.put("<=", OperationType.LTE);
        operators.put("if", OperationType.IF);
    }



    private boolean isOperatorToken(String token) {
        return operators.containsKey(token);
    }

    private OperationType getOperationType(String token) {
        return operators.get(token);
    }

    /**
     * Return ValueType of a String token by defined regex
     * @param token String
     * @return ValueType (INTEGER, BOOLEAN, ...), null if wrong token grammar
     */
    private ValueType getValueType(String token) {

        if (token.matches("^([+-]?[1-9]\\d*|0)$")) {
            return ValueType.INTEGER;
        }

        if (token.equals("True") || token.equals("False")) {
            return ValueType.BOOLEAN;
        }

        if (token.matches("[+-]?([0-9]*[.])?[0-9]+")) {
            return ValueType.REAL;
        }

        if (token.charAt(0) == '\'' && token.charAt(token.length() - 1) == '\'') {
            return ValueType.STRING;
        }

        if (token.matches("^[a-zA-Z_$][a-zA-Z_$0-9]*$")) {
            return ValueType.VARIABLE;
        }

        return null;
    }

    /**
     * Receive String token and convert to operator, do operation with arguments popped from stack,
     * push result back to stack after finish
     * @param token String
     */
    private void doOperation(String token) throws ClassCastException {
        try {
            OperationType operationType = getOperationType(token);
            switch (operationType) {
                case ADD: {
                    ArithmeticValue arg1 = (ArithmeticValue) valueStack.pop();
                    ArithmeticValue arg2 = (ArithmeticValue) valueStack.pop();
                    valueStack.push(arg2.add(arg1));
                    break;
                }
                case SUB: {
                    ArithmeticValue arg1 = (ArithmeticValue) valueStack.pop();
                    ArithmeticValue arg2 = (ArithmeticValue) valueStack.pop();
                    valueStack.push(arg2.sub(arg1));
                    break;
                }
                case MUL: {
                    ArithmeticValue arg1 = (ArithmeticValue) valueStack.pop();
                    ArithmeticValue arg2 = (ArithmeticValue) valueStack.pop();
                    valueStack.push(arg2.mul(arg1));
                    break;
                }
                case DIV: {
                    ArithmeticValue arg1 = (ArithmeticValue) valueStack.pop();
                    ArithmeticValue arg2 = (ArithmeticValue) valueStack.pop();
                    valueStack.push(arg2.div(arg1));
                    break;
                }
                case MOD: {
                    ArithmeticValue arg1 = (ArithmeticValue) valueStack.pop();
                    ArithmeticValue arg2 = (ArithmeticValue) valueStack.pop();
                    valueStack.push(arg2.mod(arg1));
                    break;
                }
                case AND: {
                    BooleanExpression arg1 = (BooleanExpression) valueStack.pop();
                    BooleanExpression arg2 = (BooleanExpression) valueStack.pop();
                    valueStack.push(arg2.and(arg1));
                    break;
                }
                case NOT: {
                    BooleanExpression arg1 = (BooleanExpression) valueStack.pop();
                    valueStack.push(arg1.not());
                    break;
                }
                case OR: {
                    BooleanExpression arg1 = (BooleanExpression) valueStack.pop();
                    BooleanExpression arg2 = (BooleanExpression) valueStack.pop();
                    valueStack.push(arg2.or(arg1));
                    break;
                }
                case XOR: {
                    BooleanExpression arg1 = (BooleanExpression) valueStack.pop();
                    BooleanExpression arg2 = (BooleanExpression) valueStack.pop();
                    valueStack.push(arg2.xor(arg1));
                    break;
                }
                case ISTRUE: {
                    BooleanExpression arg1 = (BooleanExpression) valueStack.pop();
                    valueStack.push(arg1.isTrue());
                    break;
                }
                case ISFALSE: {
                    BooleanExpression arg1 = (BooleanExpression) valueStack.pop();
                    valueStack.push(arg1.isFalse());
                    break;
                }
                case SUBSTR: {
                    IntegerExpression arg1 = (IntegerExpression) valueStack.pop();
                    IntegerExpression arg2 = (IntegerExpression) valueStack.pop();
                    StringValue arg3 = (StringValue) valueStack.pop();
                    valueStack.push(arg3.substr(arg2, arg1));
                    break;
                }
                case APPEND: {
                    StringValue arg1 = (StringValue) valueStack.pop();
                    StringValue arg2 = (StringValue) valueStack.pop();
                    valueStack.push(arg2.append(arg1));
                    break;
                }
                case ISEMPTY: {
                    StringValue arg1 = (StringValue) valueStack.pop();
                    valueStack.push(arg1.isEmpty());
                    break;
                }
                case TRIM: {
                    StringValue arg1 = (StringValue) valueStack.pop();
                    valueStack.push(arg1.trim());
                    break;
                }
                case EQ: {
                    ComparableValue arg1 = (ComparableValue) valueStack.pop();
                    ComparableValue arg2 = (ComparableValue) valueStack.pop();
                    valueStack.push(arg2.isEqual(arg1));
                    break;
                }
                case NEQ: {
                    ComparableValue arg1 = (ComparableValue) valueStack.pop();
                    ComparableValue arg2 = (ComparableValue) valueStack.pop();
                    valueStack.push(arg2.isNotEqual(arg1));
                    break;
                }
                case GT: {
                    ComparableValue arg1 = (ComparableValue) valueStack.pop();
                    ComparableValue arg2 = (ComparableValue) valueStack.pop();
                    valueStack.push(arg2.isGreater(arg1));
                    break;
                }
                case GTE: {
                    ComparableValue arg1 = (ComparableValue) valueStack.pop();
                    ComparableValue arg2 = (ComparableValue) valueStack.pop();
                    valueStack.push(arg2.isGreaterOrEqual(arg1));
                    break;
                }
                case LT: {
                    ComparableValue arg1 = (ComparableValue) valueStack.pop();
                    ComparableValue arg2 = (ComparableValue) valueStack.pop();
                    valueStack.push(arg2.isLess(arg1));
                    break;
                }
                case LTE: {
                    ComparableValue arg1 = (ComparableValue) valueStack.pop();
                    ComparableValue arg2 = (ComparableValue) valueStack.pop();
                    valueStack.push(arg2.isLessOrEqual(arg1));
                    break;
                }
                case IF: {
                    BooleanExpression arg1 = (BooleanExpression) valueStack.pop();
                    Value arg2 = (Value) valueStack.pop();
                    Value arg3 = (Value) valueStack.pop();
                    if (arg1.isTrue().getBoolean()) {
                        valueStack.push(arg2);
                    } else {
                        valueStack.push(arg3);
                    }
                    break;
                }
            }
        }
        catch (ClassCastException e) {
            System.out.println("Wrong expression: \n\t" + e);
            System.exit(0);
        }
    }

    /**
     * Receive String token and convert to suitable type, push to stack, wait for doing operation
     * @param token String
     * @throws Exception token's grammar is wrong
     */
    private void pushOperandToStack(String token) throws Exception {

        ValueType valueType = getValueType(token);
        if (valueType == null) throw new Exception("Syntax Error");

        switch (valueType) {
            case VARIABLE: {
                String variableValue = variables.get(token);
                if (variableValue == null) {
                    System.out.println("Variable name without value, set default to 0");
                    variableValue = "0";
                }
                pushOperandToStack(variableValue);  /* change variable with value, so next time valueType != VARIABLE */
                break;
            }
            case INTEGER: {
                IntegerExpression arg = new IntegerExpression(token);
                valueStack.push(arg);
                break;
            }
            case REAL: {
                RealExpression arg = new RealExpression(token);
                valueStack.push(arg);
                break;
            }
            case STRING: {
                StringExpression arg = new StringExpression(token);
                valueStack.push(arg);
                break;
            }
            case BOOLEAN: {
                BooleanExpression arg = new BooleanExpression(token);
                valueStack.push(arg);
            }
        }
    }

    /**
     * Return a Value after running the expression
     * @param expression String
     * @param variables  Map: variable name ~> variable value
     * @throws Exception throw exception when the grammar is not correct.
     * @return Value
     */
    public Value interpret(String expression, Map<String, String> variables) throws Exception {

        String rawExpression = StringEscapeUtils.escapeJava(expression);
        this.variables = variables;
        this.valueStack.empty();

        String[] tokens = rawExpression.split(" ");
        for (String token : tokens) {
            if (isOperatorToken(token)) {
                doOperation(token);
            } else {
                pushOperandToStack(token);
            }
        }

        return (Value) valueStack.peek();
    }

    public static void main(String args[]) throws Exception {
        Interpreter interpreter = new Interpreter();
        Map<String, String> vars = new HashMap<>();
        vars.put("a", "2");
        Interpreter.Value a = interpreter.interpret("-1 1 +", vars);
        System.out.println(a.toString());
    }
}