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



public class ConstantPropagation {

    public ConstantPropagation(Green solver) {
        super(solver);
    }


    public Expression simplify(Expression expression, Map<Variable, Variable> map) {

    }

    private static class PropagationVisitor extends Visitor {

        private Stack<Expression> stack;
        private SortedSet<Expression> conjuncts;
        private SortedSet<IntVariable> variableSet;

        public PropagationVisitor() {
            stack = new Stack<Expression>();
            conjuncts = new TreeSet<Expression>();
            variableSet = new TreeSet<IntVariable>();

        }


        @Override
        public void postVisit(Constant constant) {
            // TODO

        }






    }





}