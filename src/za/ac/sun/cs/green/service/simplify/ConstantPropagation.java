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


public class ConstantPropagation extends BasicService{

	private int invocations = 0;

	public ConstantPropagation(Green solver) {
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = propagation(instance.getFullExpression(), map);
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

	public Expression propagation(Expression expression,Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before PROPAGATION: " + expression);
			invocations++;
			Map<Variable, Constant> mapVC;
			mapVC = new HashMap<Variable, Constant> ();
			boolean flag = true;
			boolean flag1 = true;
			while(flag1){
				/*System.out.println("----------New round");*/
				OrderingVisitor orderingVisitor = new OrderingVisitor(mapVC);
				expression.accept(orderingVisitor);
				expression = orderingVisitor.getExpression();
				mapVC = orderingVisitor.getMap();
				if(!flag){
					flag1 = false;
				}
				if(!orderingVisitor.getFlag()){
					flag = false;
				}
			}
			log.log(Level.FINEST, "After PROPAGATION: " + expression);
			return expression;
		} catch (VisitorException x) {
			log.log(Level.SEVERE, "encountered an exception -- this should not be happening!",x);
		}
		return null;
	}

	private static class OrderingVisitor extends Visitor {

		private Stack<Expression> stack;
		private Map<Variable, Constant> map;
		private boolean flag = true;

		public OrderingVisitor(Map<Variable, Constant> maps) {
			stack = new Stack<Expression>();
			map = new HashMap<Variable, Constant> ();
			map = maps;
		}

		public Map<Variable, Constant> getMap(){
			return map;
		}

		public boolean getFlag(){
			return flag;
		}

		public Expression getExpression() {
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
			/*Pop the left and right side of the operation into a seperate sides*/
			Expression RIGHT = stack.pop();
			Expression LEFT = stack.pop();
			/*System.out.println("Operation checking");*/

			switch (op) {
				/*If EQ then insert the change into the map and add to the stack*/
				case EQ:
					if (LEFT instanceof Variable && RIGHT instanceof Constant) {
						/*Check for duplicate keys on left */
						if(map.containsKey(LEFT)){
							/*System.out.println("Duplicate keys: " + LEFT + " as " + RIGHT + " NOT ADDED");*/
							flag = false;
							stack.push(new Operation(op, LEFT, RIGHT));
						}else{
							flag = true;
							map.put((Variable)LEFT, (Constant)RIGHT);
							/*System.out.println("Inserted: " + LEFT + " as " + RIGHT);*/
							stack.push(new Operation(op, LEFT, RIGHT));
						}
					}else if (RIGHT instanceof Variable && LEFT instanceof Constant) {
						/*Check for duplicate keys on right*/
						if(map.containsKey(RIGHT)){
							/*System.out.println("Duplicate keys: " + RIGHT + " as " + LEFT + " NOT ADDED");*/
							flag = false;
							stack.push(new Operation(op, LEFT, RIGHT));
						}else{
							flag = true;
							map.put((Variable)RIGHT, (Constant)LEFT);
							/*System.out.println("Inserted " + RIGHT + " as " + LEFT);*/
							stack.push(new Operation(op, RIGHT, LEFT));
						}
					}else if(LEFT instanceof Variable && RIGHT instanceof Variable){
						/*
						for use case x == y
						ex.
						We have x == 1
						then x == y ---> 1 == y
						*/
						if (map.containsKey(LEFT)) {
							/* System.out.println("Inserted: " + RIGHT + " as " + map.get(LEFT));*/
							map.put((Variable)RIGHT, (Constant)map.get(LEFT));
							stack.push(new Operation(op, RIGHT, map.get(LEFT)));
						}else if (map.containsKey(RIGHT)) {
							/*System.out.println("Inserted: " + LEFT + " as " + map.get(RIGHT));*/
							map.put((Variable)LEFT, (Constant)map.get(RIGHT));
							stack.push(new Operation(op, LEFT, map.get(RIGHT)));
						}else{
							stack.push(new Operation(op, LEFT, RIGHT));
						}
					}else{
						stack.push(new Operation(op, LEFT, RIGHT));
					}
					break;
				default:
					/*Adds the operation if not EQ to the stack*/
					if (map.containsKey(RIGHT)) {
						RIGHT = map.get(RIGHT);
					}
					if (map.containsKey(LEFT)) {
						LEFT = map.get(LEFT);
					}
					stack.push(new Operation(op, LEFT, RIGHT));
					break;
			}
		}
	}
}
