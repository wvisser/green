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

import com.sun.javafx.binding.IntegerConstant;

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
	
	private int invocations = 0;

	public ConstantPropogation(Green solver) {
		super(solver);
	}

	@Override
    public Set<Instance> processRequest(Instance instance) {
        @SuppressWarnings("unchecked")
        Set<Instance> result = (Set<Instance>) instance.getData(getClass());
        if (result == null) {
            final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
            final Expression e = simplify(instance.getFullExpression(), map);
            final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
            result = Collections.singleton(i);
            instance.setData(getClass(), result);
        }
        return result;
    }

    @Override
    public void report(Reporter reporter) {
        reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
    }

	public Expression simplify(Expression expression, Map<Variable, Variable> map) {
		log.log(Level.FINEST, "Before simplifying: " + expression);
		invocations += 1;
		
		return expression;
	}

	public class SimplifyVisitor extends Visitor {
		private Stack<Expression> stack;
		private Map<IntVariable, IntConstant> map;

		public SimplifyVisitor() {
			stack = new Stack<Expression>();
			map = new TreeMap<IntVariable, IntConstant>();
		}

		public Expression getExpression() {
			stack.pop();
		}

		@Override
		public void postVisit(IntConstant constant) {
			stack.push(constant);
		}

		@Override
		public void postVisit(Operation operation) {
			Operation.Operator op = operation.getOperator();
			if (op.equals(Operation.Operator.EQ)) {
				Expression opL = operation.getOperand(0);
				Expression opR = operation.getOperand(1);
				if ((opL instanceof IntConstant) && (opR instanceof IntVariable)) {
					map.put((IntVariable) opR, (IntConstant) opL);
				} else if ((opL instanceof IntVariable) && (opR instanceof IntConstant)) {
					map.put((IntVarible) opL, (IntConstant) opR);
				}

			}
		}

		@Override
		public void postVisit(IntVariable variable) {
			stack.push(variable);
		}

		@Override
		public void preVisit(Operation operation) {
			Operation.Operator op = operation.getOperator();

			if (stack.size >= 2) {
				Expression right = stack.pop();
				Expression left = stack.pop();
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

				Operation e = new Operation(Operation.Operator.EQ, left, right);
				stack.push(e);
			}

		}

	}

}