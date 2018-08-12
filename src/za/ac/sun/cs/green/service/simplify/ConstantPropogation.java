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

import java.util.*;

public class ConstantPropogation extends BasicService {

	/**
	 * Number of times the slicer has been invoked.
	 */
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
			final Expression e = canonize(instance.getFullExpression(), map);
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

	public Expression canonize(Expression expression,
			Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Canonization: " + expression);
			invocations++;
			OrderingVisitor orderingVisitor = new OrderingVisitor();
			expression.accept(orderingVisitor);
			expression = orderingVisitor.getExpression();
			log.log(Level.FINEST, "After Canonization: " + expression);
			return expression;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}

	private static class OrderingVisitor extends Visitor {

		private Stack<Expression> stack;
		private static TreeMap<String,Integer> var = new TreeMap<String,Integer>();

		public OrderingVisitor() {
			stack = new Stack<Expression>();
		}

		public Expression getExpression() {
			System.out.println(Arrays.asList(var));
			return stack.pop();
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

			Expression r = stack.pop();
			Expression l = stack.pop();

			if(op.equals(Operation.Operator.EQ) && (r instanceof IntVariable && l instanceof IntConstant) || (l instanceof IntVariable && r instanceof IntConstant)){

				if(r instanceof IntVariable && l instanceof IntConstant){
						var.put(r.toString(), Integer.parseInt(l.toString()));
					}	 else if(l instanceof IntVariable && r instanceof IntConstant){
						var.put(l.toString() , Integer.parseInt(r.toString()));
					}
					//stack.push(new Operation(op, l, r));
					System.out.println("Hello World3");
					stack.push(new Operation(op, l, r));
			}


			else if(var.containsKey(r.toString()) && var.containsKey(l.toString())){
				//l = new IntConstant(7);
				System.out.println("Hello World");
				stack.push(new Operation(op, l, r));

			}	else if(var.get(r.toString()) != null){
				System.out.println("Hello World1");
				r = new IntConstant(var.get(r.toString()));
				stack.push(new Operation(op, l, r));

			}	else if(var.get(l.toString()) != null){
				System.out.println("Hello World2");
				l = new IntConstant(var.get(l.toString()));
				stack.push(new Operation(op, l, r));

			}
		}
	}
}
