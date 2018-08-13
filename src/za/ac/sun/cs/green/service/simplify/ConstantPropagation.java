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

public class ConstantPropogation extends BasicService {

    public ConstantPropogation(Green solver) {
		super(solver);
	}

    @Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = propogate(instance.getFullExpression(), map);
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
	}

    public Expression propogate(Expression expression,
			Map<Variable, Variable> map) {
		try {
			PropogateVisitor propogateVisitor = new PropogateVisitor();
			expression.accept(propogateVisitor);
			Expression propogated = propogateVisitor.getExpression();
			return propogated;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}


}



private static class PropogateVisitor extends Visitor {


    private Stack<Expression> stack;

    private IntConstant saveC;
    private IntVariable saveV;

    private IntVariable boundVariable;

    private Integer bound;

    private int boundCoeff;

    private boolean unsatisfiable;

    private boolean linearInteger;

    public PropogationVisitor() {
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
        default:
            break;
        }

        if (nop != null) {
            if (nop == Operation.Operator.EQ) {
                Expression r = stack.pop(); //variable1
                Expression l = stack.pop(); // variable2
                if ((r instanceof IntVariable) && (l instanceof IntConstant)) {
                    if (saveV == null && saveC == null) {
                        saveV = r;
                        saveC = l;
                    }
                    stack.push(new Operation(nop, r, l));
                }
                if ((r instanceof IntConstant) && (l instanceof IntVariable)) {
                    if (saveV == null && saveC == null) {
                        saveV = l;
                        saveC = r;
                    }
                    stack.push(new Operation(nop, r, l));
                }
                if ((r instanceof IntVariable) && (l instanceof IntVariable)) {

                    if ((((IntVariable) r).getName().compareTo(saveV).getName()) == 0))  {
                        stack.push(new Operation(nop, saveC, l));
                    } else if ((((IntVariable) l).getName().compareTo(saveV).getName()) == 0)) {
                        stack.push(new Operation(nop, r, saveC));
                    } else {
                        stack.push(new Operation(nop, r, l));
                    }


                }





            }



        }


        /*
        if (nop != null) {
            Expression r = stack.pop();
            Expression l = stack.pop();
            if ((r instanceof IntVariable)
                    && (l instanceof IntVariable)
                    && (((IntVariable) r).getName().compareTo(
                            ((IntVariable) l).getName()) < 0)) {
                stack.push(new Operation(nop, r, l));
            } else if ((r instanceof IntVariable)
                    && (l instanceof IntConstant)) {
                stack.push(new Operation(nop, r, l));
            } else {
                stack.push(operation);
            }
        } else if (op.getArity() == 2) {
            Expression r = stack.pop();
            Expression l = stack.pop();
            stack.push(new Operation(op, l, r));
        } else {
            for (int i = op.getArity(); i > 0; i--) {
                stack.pop();
            }
            stack.push(operation);
        }
        */






    }
