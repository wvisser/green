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

  /**
  * Number of times the propagator has been invoked.
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
      final Expression e = propagate(instance.getFullExpression());
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

  /**
  * Function that propagates an expression
  * and logs the expression both before and after prpagation.
  *
  * @param expression - The expression to be propogated
  * @return The propogated expression
  */
  public Expression propagate(Expression expression) {
    try {
      log.log(Level.FINEST, "Before Propagation: " + expression);
      invocations++;
      OrderingVisitor orderingVisitor = new OrderingVisitor();
      expression.accept(orderingVisitor);
      expression = orderingVisitor.getExpression();
      log.log(Level.FINEST, "After Propagation: " + expression);
      return expression;
    } catch (VisitorException x) {
      log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", x);
    }
    return null;
  }

  private static class OrderingVisitor extends Visitor {

    private Stack<Expression> stack;
    private static Map<String, Integer> map;


    public OrderingVisitor() {
      stack = new Stack<Expression>();
      map = new HashMap<>();
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
      Operation.Operator nop = null;
      nop = op;

      if (nop != null) {
        Expression r = stack.pop();
        Expression l = stack.pop();
        // If operation is "=="
        if (op == Operation.Operator.EQ) {
          // If right-hand side is a variable and left-hand side is a constant
          if ((r instanceof IntVariable) && (l instanceof IntConstant)) {
            map.put(r.toString(), Integer.parseInt(l.toString())); // Add variable to map of variables
          } else if ((l instanceof IntVariable) && (r instanceof IntConstant)) {
            // Else if left-hand side is a variable and right-hand side is a constant
            map.put(l.toString(), Integer.parseInt(r.toString())); // Add variable to map of variables
          }
          stack.push(new Operation(nop, l, r));
        } else {
          // Assign value to any variables that have previously been given a value
          if (map.containsKey(l.toString())) l = new IntConstant(map.get(l.toString()));
          if (map.containsKey(r.toString()))  r = new IntConstant(map.get(r.toString()));
          stack.push(new Operation(nop, l, r));
        }
      } else if (op.getArity() == 2) {
        Expression r = stack.pop();
        Expression l = stack.pop();

        // Assign value to any variables that have previously been given a value
        if (map.containsKey(l.toString())) l = new IntConstant(map.get(l.toString()));
        if (map.containsKey(r.toString()))   r = new IntConstant(map.get(r.toString()));
        stack.push(new Operation(op, l, r));

      } else { // If nop == null
        for (int i = op.getArity(); i > 0; i--) {
          stack.pop();
        }
        stack.push(operation);
      }
    }

  }

}
