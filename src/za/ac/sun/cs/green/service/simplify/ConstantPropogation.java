package za.ac.sun.cs.green.service.simplify;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.service.BasicService;

import java.util.*;

public class ConstantPropogation extends BasicService {
    public ConstantPropogation(Green solver) {
        super(solver);
    }

    @Override
    public Set<Instance> processRequest(Instance instance) {
        @SuppressWarnings("unchecked")
        Set<Instance> result = (Set<Instance>) instance.getData(getClass());
        if (result == null) {
            try {
                final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
                final Expression e = instance.getFullExpression();
                ConstantCheckerVisitor checkerVisitor = new ConstantCheckerVisitor();
                e.accept(checkerVisitor);
                ConstantPropagatorVisitor propagatorVisitor = new ConstantPropagatorVisitor(checkerVisitor.getMap());
                e.accept(propagatorVisitor);
                Expression propagated = propagatorVisitor.getExpression();
                final Instance i = new Instance(getSolver(), instance.getSource(), null, propagated);
                result = Collections.singleton(i);
                instance.setData(getClass(), result);
            } catch (VisitorException e1) {
                e1.printStackTrace();
            }
        }
        return result;
    }

    private static class ConstantCheckerVisitor extends Visitor {

        private int location;
        private Stack<Expression> stack;
        private HashMap<String, Pair> map;

        public ConstantCheckerVisitor() {
            location = 0;
            stack = new Stack<Expression>();
            map = new HashMap<String, Pair>();
        }

        @Override
        public void postVisit(IntConstant constant) {
            location++;
            stack.push(constant);
        }

        @Override
        public void postVisit(IntVariable variable) {
            location++;
            stack.push(variable);
        }

        @Override
        public void postVisit(Operation operation) throws VisitorException {
            location++;
            if (operation.getOperator() != Operation.Operator.EQ) {
                stack.push(operation);
                return;
            }
            Expression a = stack.pop();
            Expression b = stack.pop();
            if (a instanceof Constant && b instanceof Variable) {
                map.put(b.toString(), new Pair(a, location));
            } else if (a instanceof Variable && b instanceof Constant) {
                map.put(a.toString(), new Pair(b, location));
            }
            stack.push(operation);
        }

        public HashMap<String, Pair> getMap() {
            return map;
        }
    }

    private static class ConstantPropagatorVisitor extends Visitor {

        private int location;
        private Stack<Expression> stack;
        private Map<String, Pair> map;

        public ConstantPropagatorVisitor(HashMap<String, Pair> map) {
            location = 0;
            stack = new Stack<Expression>();
            this.map = map;
        }

        @Override
        public void postVisit(IntConstant constant) {
            location++;
            stack.push(constant);
        }

        @Override
        public void postVisit(IntVariable variable) {
            location++;
            stack.push(variable);
        }

        @Override
        public void postVisit(Operation operation) throws VisitorException {
            location++;
            Expression a = stack.pop();
            Expression b = stack.pop();
            if (map.containsKey(a.toString())) {
                if (location != map.get(a.toString()).getGetLocation()) {
                    Expression temp = new Operation(operation.getOperator(), a, map.get(b.toString()).getExpr());
                    stack.push(temp);
                    return;
                }
                stack.push(new Operation(operation.getOperator(), b, a));
            } else if (map.containsKey(b.toString())) {
                if (location != map.get(b.toString()).getGetLocation()) {
                    Expression temp = new Operation(operation.getOperator(), map.get(b.toString()).getExpr(), a);
                    stack.push(temp);
                    return;
                }
                stack.push(new Operation(operation.getOperator(), b, a));
            } else {
                Expression temp = new Operation(operation.getOperator(), b, a);
                stack.push(temp);
            }

        }

        public Expression getExpression() {
            return stack.pop();
        }

    }

    private static class Pair {
        Expression expr;
        int location;

        Pair(Expression expr, int location) {
            this.expr = expr;
            this.location = location;
        }

        public int getGetLocation() {
            return location;
        }

        public Expression getExpr() {
            return expr;
        }
    }

}
