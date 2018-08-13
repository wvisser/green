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

    public ConstantPropogation(Green solver){
        super(solver);
    }

    @Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Expression e = propogate(instance.getFullExpression());
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
    }

    public Expression propogate(Expression expression) {
        Expression propogated = null;

        try {
            log.log(Level.FINEST, "\n**********\n");
            log.log(Level.FINEST, "Before Constant Propogation: " + expression);

            PropogateVisitor propogateVisitor = new PropogateVisitor();
            expression.accept(propogateVisitor);
            expression =  propogateVisitor.getExpression();

            log.log(Level.FINEST, "After Constant Propogation: " + expression);
            log.log(Level.FINEST, "\n**********\n");
            return expression;

        } catch (VisitorException ve) {
            log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", ve);
        }

        return null;
    }

    public class PropogateVisitor extends Visitor {
        private Stack<Expression> stack;
        private Map<IntVariable, IntConstant> map;

        public PropogateVisitor() {
            stack = new Stack<Expression>();
            map = new TreeMap<IntVariable, IntConstant>();
        }

        public Expression getExpression() {
			return stack.pop();
		}

        @Override
		public void preVisit(Operation operation) {

			Operation.Operator op = operation.getOperator();

			if (op.equals(Operation.Operator.EQ)) {
				Expression opL = operation.getOperand(0);
				Expression opR = operation.getOperand(1);
                log.log(Level.FINEST, opL + " " + op + " " + opR);
				if (opL instanceof IntConstant)  {
                    if (opR instanceof IntVariable) {
                        map.put((IntVariable) opR, (IntConstant) opL);
                    }
				} else if (opL instanceof IntVariable) {
                    if (opR instanceof IntConstant) {
                        map.put((IntVariable) opL, (IntConstant) opR);
                    }
				}
			}
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
		public void postVisit(Operation operation) {

			Operation.Operator op = operation.getOperator();

			if (stack.size() >= 2) {
                Expression right = stack.pop();
                log.log(Level.FINEST, "Pop Right: "+right);
				Expression left = stack.pop();
                log.log(Level.FINEST, "Pop Left: "+left);
				if (!op.equals(Operation.Operator.EQ)) {
					if (left instanceof IntVariable) {
						if (map.containsKey(left)) {
							left = map.get(left);
						}
					}
					if (right instanceof IntVariable) {
						if (map.containsKey(right)) {
							right = map.get(right);
						}
					}
				}
				Operation e = new Operation(operation.getOperator(), left, right);

                boolean isTrue = false;
                if (left instanceof IntVariable && right instanceof IntVariable) {
                    switch (op) {
                    case EQ:
                        isTrue = (left.getValue() == right.getValue());
                        break;
                    case NE:
                        isTrue = (left.getValue() != right.getValue());
                        break;
                    case LT:
                        isTrue = (left.getValue() < right.getValue());
                        break;
                    case LE:
                        isTrue = (left.getValue() <= right.getValue());
                        break;
                    case GT:
                        isTrue = (left.getValue() > right.getValue());
                        break;
                    case GE:
                        isTrue = (left.getValue() >= right.getValue());
                        break;
                    default:
                        break;
                    }
                }

                if (isTrue) {
                    stack.push(Operation.TRUE);
                } else {
                    stack.push(e);
                }
			}

		}
    }
}
