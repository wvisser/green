package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.*;

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
			final Expression e = propogate(instance.getFullExpression()            constantVariableMap = new TreeMap<IntVariable, IntConstant>();
);
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
    }

    public Expression propogate(Expression expression) {

        Expression propogated = null;

        try {
            log.log(Level.FINEST, "Before Constant Propogation: " + expression);

            PropogateVisitor propogateVisitor = new PropogateVisitor();
            expression.accept(propogateVisitor);
            propogated =  propogateVisitor.getExpression();

            log.log(Level.FINEST, "After Constant Propogation: " + propogated);
            return propogated;
        } catch (VisitorException ve) {
            log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", ve);
        }
        return expression;
    }

    public class PropogateVisitor extends Visitor {

        private Stack<Expression> stack;
        private LinkedList<IntVariable> variables;
        private LinkedList<IntConstant> constants;

        public PropogateVisitor() {
            stack = new Stack<Expression>();
            variables = new LinkedList<IntVariable>();
            constants = new LinkedList<IntConstant>();
        }

        public Expression getExpression() {
			return stack.pop();
		}

        @Override
		public void preVisit(Operation operation) {

			Operation.Operator op = operation.getOperator();

			if (op.equals(Operation.Operator.EQ)) {

				Expression leftOperand = operation.getOperand(0);
				Expression rightOperand = operation.getOperand(1);

				if (leftOperand instanceof IntConstant)  {
                    if (rightOperand instanceof IntVariable) {
                        variables.add((IntVariable) rightOperand);
                        constants.add((IntConstant) leftOperand);
                    }
				}

                if (leftOperand instanceof IntVariable) {
                    if (rightOperand instanceof IntConstant) {
                        variables.add((IntVariable) leftOperand);
                        constants.add((IntConstant) rightOperand);
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

                Expression first = stack.pop();
				Expression second = stack.pop();

				if (!op.equals(Operation.Operator.EQ)) {

                    if (second instanceof IntVariable) {
						if (variables.contains(second)) {
							second = constants.get(variables.indexOf(second));
						}
					}
					if (first instanceof IntVariable) {
						if (variables.contains(first)) {
							first = constants.get(variables.indexOf(first));
						}
					}
				}
				Operation out = new Operation(operation.getOperator(), second, first);

                stack.push(out);
			}

		}
    }
}
