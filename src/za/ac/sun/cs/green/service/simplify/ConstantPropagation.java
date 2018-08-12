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
            final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
            final Expression e = propagate(instance.getFullExpression(), map);
            final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
            result = Collections.singleton(i);
            instance.setData(getClass(), result);
        }
        return result;
    }

    public Expression propagate(Expression expression,
                                Map<Variable, Variable> map) {
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
            System.out.println(expression.toString());
            return expression;
        } catch (VisitorException x) {
            System.out.println(x);
        }
        return null;
    }

    private static class PropagationVisitor extends Visitor {

        private HashMap<String, IntConstant> constantValueMap;

        private Stack<Expression> stack;

        public PropagationVisitor() {
            constantValueMap = new HashMap<>();
            stack = new Stack<>();
        }

        public Expression getExpression() {
            return stack.pop();
        }

        @Override
        public void postVisit(Variable variable) {
            stack.push(variable);
        }

        @Override
        public void postVisit(Constant constant) {
            stack.push(constant);
        }

        @Override
        public void postVisit(Operation operation) {
            Operation.Operator op = operation.getOperator();

            if (op.getArity() == 2) {
                Expression l = stack.pop();
                Expression r = stack.pop();

                if(op.name().equals("EQ")) {
                    if (l instanceof IntVariable && r instanceof IntConstant) {
                        constantValueMap.put(((IntVariable) l).getName(), (IntConstant) r);
                    } else if (r instanceof  IntVariable && l instanceof IntConstant) {
                        constantValueMap.put(((IntVariable) r).getName(), (IntConstant) l);
                    }
                } else {
                    if (l instanceof IntVariable) {
                        if (constantValueMap.containsKey(((IntVariable) l).getName())) {
                            l = constantValueMap.get(((IntVariable) l).getName());
                        }
                    } if (r instanceof IntVariable) {
                        if (constantValueMap.containsKey(((IntVariable) r).getName())) {
                            r = constantValueMap.get(((IntVariable) r).getName());
                        }
                    }
                }
                stack.push(new Operation(op, r, l));
            }

        }


    }


}