package org.cidarlab.minieugene.predicates.interaction;

import org.cidarlab.minieugene.constants.RuleOperator;
import org.cidarlab.minieugene.dom.Component;
import org.cidarlab.minieugene.exception.EugeneException;
import org.cidarlab.minieugene.predicates.orientation.AllForward;
import org.cidarlab.minieugene.predicates.orientation.AllReverse;
import org.cidarlab.minieugene.predicates.orientation.AllSameOrientation;
import org.cidarlab.minieugene.predicates.position.before.AllBefore;
import org.cidarlab.minieugene.solver.jacop.PartTypes;
import org.cidarlab.minieugene.solver.jacop.Variables;

import JaCoP.constraints.And;
import JaCoP.constraints.IfThen;
import JaCoP.constraints.Not;
import JaCoP.constraints.Or;
import JaCoP.constraints.PrimitiveConstraint;
import JaCoP.constraints.XeqC;
import JaCoP.constraints.XneqC;
import JaCoP.core.IntVar;
import JaCoP.core.Store;

public class Drives 
	extends InteractionPredicate {
	
	public Drives(Component a, Component b) {
		super(a, b);
	}
		

	@Override
	public String getOperator() {
		return RuleOperator.DRIVES.toString();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getA().getName())
			.append(" ").append(RuleOperator.DRIVES).append(" ")
			.append(this.getB().getName());
		return sb.toString();
	}

	@Override
	public PrimitiveConstraint toJaCoP(Store store, IntVar[][] variables) 
				throws EugeneException {

		System.out.println("**** "+this.toString()+" ***");
		
    	// FORWARD ORIENTED
    	PrimitiveConstraint[] pcForward = new PrimitiveConstraint[3];
		pcForward[0] = noTerminatorBetween(variables, this.getA(), this.getB());
		pcForward[1] = new AllBefore(this.getA(), this.getB()).toJaCoP(store, variables);
		pcForward[2] = new AllForward(this.getA()).toJaCoP(store, variables);

		// REVERSE ORIENTED
		PrimitiveConstraint[] pcReverse = new PrimitiveConstraint[3];
		pcReverse[0] = noTerminatorBetween(variables, this.getB(), this.getA());
		pcReverse[1] = new AllBefore(this.getB(), this.getA()).toJaCoP(store, variables);
		pcReverse[2] = new AllReverse(this.getA()).toJaCoP(store, variables);

		
		// a drives b <=>
		//     a same_orientation b /\
		//     ( a before b => position(a) < position(b) \/
		//       a after b  => position(b) < position(a) ) /\
		//     no terminator in between
		PrimitiveConstraint[] pcReturn = new PrimitiveConstraint[2];
		pcReturn[0] = new AllSameOrientation(this.getA(), this.getB()).toJaCoP(store, variables);
		pcReturn[1] = new Or(
						new And(pcForward), 
						new And(pcReverse));
		return new And(pcReturn);
	}
	
	
	private PrimitiveConstraint noTerminatorBetween(
			IntVar[][] variables, Component A, Component B) {
		
		int N = variables[Variables.PART].length;
		
		PrimitiveConstraint pc[] = new PrimitiveConstraint[N];
		for(int i=0; i<N; i++) {
			
			PrimitiveConstraint[] downstream = new PrimitiveConstraint[N];
			for(int j=0; j<N; j++) {
				if(i < j) {
					PrimitiveConstraint[] noTerminator = new PrimitiveConstraint[Math.abs(i-j)];
					for(int k=i; k<j; k++) {
						noTerminator[k-i] = new XneqC(variables[Variables.TYPE][k], PartTypes.get("TERMINATOR"));
					}
					downstream[j] = new IfThen(new XeqC(variables[Variables.PART][j], B.getId()), new And(noTerminator));
				} else if (i==j) {
					downstream[j] = new XneqC(variables[Variables.PART][j], B.getId());
				} else { // i >= j
					PrimitiveConstraint[] noTerminator = new PrimitiveConstraint[Math.abs(i-j)];
					for(int k=j; k<i; k++) {
						noTerminator[k-j] = new XneqC(variables[Variables.TYPE][k], PartTypes.get("TERMINATOR"));
					}
					downstream[j] = new IfThen(new XeqC(variables[Variables.PART][j], B.getId()), new And(noTerminator));
				}
			}
			
			pc[i] = new IfThen(new XeqC(variables[Variables.PART][i], A.getId()), new And(downstream));
		}
		
		return new And(pc);
	}
	

	@Override
	public PrimitiveConstraint toJaCoPNot(Store store, IntVar[][] variables)
			throws EugeneException {
		
		// we just negate the toJaCoP primitive constraint
		return new Not((PrimitiveConstraint)this.toJaCoP(store, variables));
	}

}
