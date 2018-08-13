package za.ac.sun.cs.green.service.simplify;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.service.canonizer.SATCanonizerService;

import java.util.*;

public class ConstantPropagation extends BasicService {

    public ConstantPropagation(Green solver) {
        super(solver);
    }

    @Override
    public Set<Instance> processRequest(Instance instance) {
        @SuppressWarnings("unchecked")
        Set<Instance> result = (Set<Instance>) instance.getData(getClass());
        if (result == null) {
            final Expression e = propagate(instance.getFullExpression());
            final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
            result = Collections.singleton(i);
            instance.setData(getClass(), result);
        }
        return result;
    }

    /*
     * This function creates the PropagationVisitor object and
     * has the expression accept it. The purpose of the do-while is to continuously
     * re-propagate the expression until it can no longer be further propagated, at which point
     * it is returned to processRequest
     *
     * @param expression: The expresssion to be propagated
     */
    public Expression propagate(Expression expression) {
        try {
            PropagationVisitor propagationVisitor= new PropagationVisitor();
            expression.accept(propagationVisitor);
            expression = propagationVisitor.getExpression();
            Expression old_expression = null;
            do {
                old_expression = expression;
                expression.accept(propagationVisitor);
                expression = propagationVisitor.getExpression();
            } while(!old_expression.equals(expression));
            if (expression instanceof IntConstant) {
                if (((IntConstant) expression).getValue() == 0) {
                    return (new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(1)));
                } else if (((IntConstant) expression).getValue() == 1) {
                    return (new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(0)));

                }
            } else {
                return expression;
            }

        } catch (VisitorException x) {
            System.out.println(x);
        }
        return null;
    }

    /*
     * PropagationVisitor pushes constants and variables to the stack
     * When it encounters an operation, it pops the two expressions from the top of the stack,
     * checks the operator and operands and propagates the expression accordingly
     */
    private static class PropagationVisitor extends Visitor {

        // HashMap that stores the variable names and their corresponding values
        private HashMap<String, IntConstant> constantValueMap;

        // Stack for storing and retrieving expressions
        private Stack<Expression> stack;

        public PropagationVisitor() {
            constantValueMap = new HashMap<>();
            stack = new Stack<>();
        }

        /*
         * Called when one side is an operation and the other a constant value. This function
         * takes the constant values over to one side and simplifies them
         *
         * @param exp: Expression array with the operation in the first index and the constant in the second
         * @out exp: Expression array with the constant in the first index and the variable in the second
         */
        public Expression[] takeOver(Expression exp[]) {
            Expression op = exp[0];
            Expression constant = exp[1];

            Operation.Operator int_op = ((Operation) op).getOperator();
            Iterable<Expression> ops = ((Operation) op).getOperands();

            boolean hasVar = false;
            boolean hasConst = false;
            int varVal = 0;
            Expression var = null;

            for (Expression operand : ops) {
                if (operand instanceof IntVariable) {
                    hasVar = true;
                    var = operand;
                } else if (operand instanceof IntConstant) {
                    hasConst = true;
                    varVal = ((IntConstant) operand).getValue();
                }
            }

            if (hasVar && hasConst) {
                switch (int_op) {
                    case ADD:
                        exp[0] = new IntConstant(((IntConstant) exp[1]).getValue() - varVal);
                        exp[1] = var;
                        break;
                    case SUB:
                        exp[0] = new IntConstant(((IntConstant) exp[1]).getValue() + varVal);
                        exp[1] = var;
                        break;
                    case MUL:
                        exp[0] = new IntConstant(((IntConstant) exp[1]).getValue() / varVal);
                        exp[1] = var;
                        break;
                    case DIV:
                        exp[0] = new IntConstant(((IntConstant) exp[1]).getValue() * varVal);
                        exp[1] = var;
                        break;
                    default:
                        break;
                }
            }

            return exp;
        }

        /*
         * Called when two constant operate on each other in some way (Mathematical operator)
         *
         * @param r: Right constant
         * @param l: Left constant
         * @param op: Operator that operates on both
         *
         * @out: the constant result of the two constants operated on each other
         */
        public int simplifyConstants(Expression r, Expression l, Operation.Operator op) {
            l = (IntConstant) l;
            r = (IntConstant) r;
            switch(op) {
                case ADD:
                    return ((IntConstant) r).getValue() + ((IntConstant) l).getValue();
                case SUB:
                    return ((IntConstant) r).getValue() - ((IntConstant) l).getValue();
                case MUL:
                    return  ((IntConstant) r).getValue() * ((IntConstant) l).getValue();
                case DIV:
                    return ((IntConstant) r).getValue() / ((IntConstant) l).getValue();
                case MOD:
                    return ((IntConstant) r).getValue() % ((IntConstant) l).getValue();
            }
            return 0;
        }

        /*
         * Returns the expression at the top of the stack
         */
        public Expression getExpression() {
            return stack.pop();
        }

        @Override
        /*
         * Pushes variable to the top of the stack
         */
        public void postVisit(Variable variable) {
            stack.push(variable);
        }

        @Override
        /*
         * Pushes constant to the top of the stack
         */
        public void postVisit(Constant constant) {
            stack.push(constant);
        }

        @Override
        /*
         * Propagates operations by examining the operator and both operands
         * If the operator is "==" and one of the operands is a variable
         * and the other a constant, the variable and value are put into the
         * HashMap. If that variable is accessed in future operations/propagations, the variable name
         * will be replaced by the value associated with it in the HashMap, and the new value will be pushed
         * to the stack instead of the variable name
         */
        public void postVisit(Operation operation) {
            Operation.Operator op = operation.getOperator();
            if (op.getArity() == 2) {
                // Left and right expressions are popped from the top of the stack
                Expression l = stack.pop();
                Expression r = stack.pop();
                Expression take[];

                // Check if the right hand side of the expression is a constant
                if (r instanceof IntConstant) {
                    if (op.name().equals("AND")) {
                        if (((IntConstant) r).getValue() > 0) {
                            stack.push(l);
                            return;
                        }
                    }
                    // Both left and right side are constants, some constant operation
                    if (l instanceof IntConstant) {
                        switch(op) {
                            case AND:
                                if (((IntConstant) r).getValue() == 1 && ((IntConstant) l).getValue() == 1) {
                                    stack.push(new IntConstant(1));
                                } else {
                                    stack.push(new IntConstant(0));
                                }
                                break;
                            case LE:
                                if (((IntConstant) r).getValue() <= ((IntConstant) l).getValue()) {
                                    stack.push(new IntConstant(1));
                                } else {
                                    stack.push(new IntConstant(0));
                                }
                                break;
                            case LT:
                                if (((IntConstant) r).getValue() < ((IntConstant) l).getValue()) {
                                    stack.push(new IntConstant(1));
                                } else {
                                    stack.push(new IntConstant(0));
                                }
                                break;
                            case GT:
                                if (((IntConstant) r).getValue() > ((IntConstant) l).getValue()) {
                                    stack.push(new IntConstant(1));
                                } else {
                                    stack.push(new IntConstant(0));
                                }
                                break;
                            case GE:
                                if (((IntConstant) r).getValue() >= ((IntConstant) l).getValue()) {
                                    stack.push(new IntConstant(1));
                                } else {
                                    stack.push(new IntConstant(0));
                                }
                                break;
                            case EQ:
                                if (((IntConstant) r).getValue() == ((IntConstant) l).getValue()) {
                                    stack.push(new IntConstant(1));
                                } else {
                                    stack.push(new IntConstant(0));
                                }
                                break;
                            default:
                                stack.push(new IntConstant(simplifyConstants(l,r,op)));
                                break;
                        }
                        return;
                    // One side is constant, other side is a operation: Take constants over
                    } else if (l instanceof Operation) {
                        take = takeOver(new Expression[]{l, r});
                        r = take[0];
                        l = take[1];

                        if (r instanceof IntConstant) {
                            if (op.name().equals("EQ") && l instanceof IntVariable) {
                                constantValueMap.put(((IntVariable) l).getName(), (IntConstant) r);
                            }
                        }
                     // One side is constant, other side is variable. Put in HashMap if its the `==' operation
                    } else if (l instanceof IntVariable) {
                        if (op.name().equals("EQ")) {
                            constantValueMap.put(((IntVariable) l).getName(), (IntConstant) r);
                        } else if (constantValueMap.containsKey(((IntVariable) l).getName())){
                            l = constantValueMap.get(((IntVariable) l).getName());
                        }
                    }
                // Check if right side is variable
                } else if (r instanceof IntVariable) {
                    // Left and right side are variables
                     if (l instanceof IntVariable) {
                         if (constantValueMap.containsKey(((IntVariable) r).getName())) {
                             r = constantValueMap.get(((IntVariable) r).getName());
                         }
                    }
                    if (r instanceof IntConstant) {
                         if (op.name().equals("EQ")) {
                             constantValueMap.put(((IntVariable) l).getName(), (IntConstant) r);
                         }
                     }

                }

                //Check if left side is constant
                if (l instanceof IntConstant) {
                    if (op.name().equals("AND")) {
                        if (((IntConstant) l).getValue() > 0) {
                            stack.push(r);
                            return;
                        }
                    }
                    //Both left and right side are constants, some constant operation
                    if (r instanceof IntConstant) {
                        switch(op) {
                            case AND:
                                if (((IntConstant) r).getValue() == 1 && ((IntConstant) l).getValue() == 1) {
                                    stack.push(new IntConstant(1));
                                } else {
                                    stack.push(new IntConstant(0));
                                }
                                break;
                            case LE:
                                if (((IntConstant) r).getValue() <= ((IntConstant) l).getValue()) {
                                    stack.push(new IntConstant(1));
                                } else {
                                    stack.push(new IntConstant(0));
                                }
                                break;
                            case LT:
                                if (((IntConstant) r).getValue() < ((IntConstant) l).getValue()) {
                                    stack.push(new IntConstant(1));
                                } else {
                                    stack.push(new IntConstant(0));
                                }
                                break;
                            case GT:
                                if (((IntConstant) r).getValue() > ((IntConstant) l).getValue()) {
                                    stack.push(new IntConstant(1));
                                } else {
                                    stack.push(new IntConstant(0));
                                }
                                break;
                            case GE:
                                if (((IntConstant) r).getValue() >= ((IntConstant) l).getValue()) {
                                    stack.push(new IntConstant(1));
                                } else {
                                    stack.push(new IntConstant(0));
                                }
                                break;
                            case EQ:
                                if (((IntConstant) r).getValue() == ((IntConstant) l).getValue()) {
                                    stack.push(new IntConstant(1));
                                } else {
                                    stack.push(new IntConstant(0));
                                }
                                break;
                            default:
                                stack.push(new IntConstant(simplifyConstants(r,l,op)));
                                break;
                        }
                        return;
                     // One side is constant, one side is operation: Take constants over
                    } else if (r instanceof Operation) {
                        take = takeOver(new Expression[]{r, l});
                        l =  take[0];
                        r = take[1];

                        if (l instanceof IntConstant) {
                            if (op.name().equals("EQ") && r instanceof IntVariable) {
                                constantValueMap.put(((IntVariable) r).getName(), (IntConstant) l);
                            }
                        }
                    // One side is constant, other side is variable. Put in HashMap if its the `==' operation
                    } else if (r instanceof IntVariable) {
                        if (op.name().equals("EQ")) {
                            constantValueMap.put(((IntVariable) r).getName(), (IntConstant) l);
                        } else if (constantValueMap.containsKey(((IntVariable) r).getName())) {
                            r = constantValueMap.get(((IntVariable) r).getName());
                        }
                    }
                 // Check if left side is variable
                } else if (l instanceof IntVariable) {
                    // Both sides are variables
                    if (r instanceof IntVariable) {
                        if (constantValueMap.containsKey(((IntVariable) l).getName())) {
                            l = constantValueMap.get(((IntVariable) l).getName());
                        }
                    }
                    if (l instanceof IntConstant) {
                        if (op.name().equals("EQ")) {
                            constantValueMap.put(((IntVariable) r).getName(), (IntConstant) l);
                        }
                    }
                }

                // Push new operation to the stack
                stack.push(new Operation(op, r, l));
            }

        }

    }
}