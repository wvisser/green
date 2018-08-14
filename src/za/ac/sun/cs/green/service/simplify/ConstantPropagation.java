package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;

public class ConstantPropagation extends BasicService {

    public ConstantPropagation(Green solver) {
		super(solver);
	}

    @Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = propagate(instance.getFullExpression(), map);
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
        return result;
	}

    public Expression propagate(Expression expression, Map<Variable, Variable> map) {
		try {
			PropagateVisitor propagateVisitor = new PropagateVisitor();
			expression.accept(propagateVisitor);
			Expression propagated = propagateVisitor.getExpression();
			return propagated;
		} catch (VisitorException x) {
			log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", x);
		}
		return null;
	}

    private class PropagateVisitor extends Visitor {

        private Stack<Expression> stack;
        private IntConstant saveC = null;
        private IntVariable saveV = null;
        private boolean unsatisfiable;
        private boolean linearInteger;

        //private IntVariable boundVariable;
        //private Integer bound;
        //private int boundCoeff;

        public PropagateVisitor() {
            stack = new Stack<Expression>();
            unsatisfiable = false;
            linearInteger = true;
        }

        @Override
        public void postVisit(IntConstant constant) {
            stack.push(constant);
        }

        @Override
        public void postVisit(IntVariable variable) {
            stack.push(variable);
        }

        /* checks what operator is being used, two expressions are then removed from the stack and re-entered as an operation */
        @Override
        public void postVisit(Operation operation) throws VisitorException {
            Operation.Operator op = operation.getOperator();
            Operation.Operator nop = null;
            switch (op) {
            case EQ:
                nop = Operation.Operator.EQ;
                break;
            case NE:
                nop = Operation.Operator.NE;
                break;
            case LT:
                nop = Operation.Operator.GT;
                break;
            case LE:
                nop = Operation.Operator.GE;
                break;
            case GT:
                nop = Operation.Operator.LT;
                break;
            case GE:
                nop = Operation.Operator.LE;
                break;
            case ADD:
                nop = Operation.Operator.ADD;
                break;
            case AND:
                nop = Operation.Operator.AND;
                break;
            default:
                break;
            }

            if (nop != null) {
                if (nop == Operation.Operator.EQ) {// check if equals
                    Expression r = stack.pop(); //variable1
                    Expression l = stack.pop(); // variable2
                    if ((r instanceof IntVariable) && (l instanceof IntConstant)) {
                        if (saveV == null && saveC == null) {
                            saveV = (IntVariable) r;
                            saveC = (IntConstant) l;
                        }
                        stack.push(new Operation(nop, l, r));
                    }else if ((r instanceof IntConstant) && (l instanceof IntVariable)) {
                        if (saveV == null && saveC == null) {
                            saveV = (IntVariable) l;
                            saveC = (IntConstant) r;
                        }
                        stack.push(new Operation(nop, l, r));
                    }else if ((r instanceof IntVariable) && (l instanceof IntVariable)) {
                        if (saveV != null) {
                            if ((((IntVariable) r).getName().compareTo((saveV).getName()) == 0))  {
                                stack.push(new Operation(nop, l, saveC));
                            } else if ((((IntVariable) l).getName().compareTo((saveV).getName()) == 0)) {
                                stack.push(new Operation(nop, saveC, r));
                            } else {
                                stack.push(new Operation(nop, l, r));
                            }
                        } else {
                            stack.push(new Operation(nop, l, r));
                        }

                    } else {
                        stack.push(new Operation(nop, l, r));
                    }
                } else if (nop == Operation.Operator.ADD) {//check for addition
                    Expression r = stack.pop(); //variable1
                    Expression l = stack.pop(); // variable2
                    if ((r instanceof IntVariable) && (l instanceof IntVariable)) {
                        if (saveV != null) {
                            if ((((IntVariable) r).getName().compareTo((saveV).getName()) == 0))  {
                                stack.push(new Operation(nop, l, saveC));
                            } else if ((((IntVariable) l).getName().compareTo((saveV).getName()) == 0)) {
                                stack.push(new Operation(nop, saveC, r));
                            } else {
                                stack.push(new Operation(nop, l, r));
                            }
                        } else {
                            stack.push(new Operation(nop, l, r));
                        }

                    }
                } else  if (nop == Operation.Operator.AND) {//check for and operation
                    Expression r = stack.pop(); //variable1
                    Expression l = stack.pop(); // variable2
                    stack.push(new Operation(nop, l, r));
                    for (Expression i : stack) {
                        System.out.println("i: " + i);
                    }
                } else {
                    Expression r = stack.pop(); //variable1
                    Expression l = stack.pop(); // variable2
                    stack.push(new Operation(nop, l, r));
                }
            }
        }

        public Expression getExpression() {
            return stack.pop();
        }
    }
}
