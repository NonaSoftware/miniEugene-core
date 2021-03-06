/*
 * Copyright (c) 2014, Boston University
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or 
 * without modification, are permitted provided that the following 
 * conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 *    
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the distribution.
 *    
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR 
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED 
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.cidarlab.minieugene.predicates.templating;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.cidarlab.minieugene.constants.RuleOperator;
import org.cidarlab.minieugene.dom.Component;
import org.cidarlab.minieugene.exception.MiniEugeneException;
import org.cidarlab.minieugene.solver.jacop.Variables;

import org.jacop.constraints.And;
import org.jacop.constraints.Constraint;
import org.jacop.constraints.ExtensionalSupportVA;
import org.jacop.constraints.Not;
import org.jacop.constraints.Or;
import org.jacop.constraints.PrimitiveConstraint;
import org.jacop.constraints.XeqC;
import org.jacop.core.IntVar;
import org.jacop.core.Store;

public class Template 
	extends TemplatingPredicate {
	
	public Template() {
		super(null, new ArrayList<List<Component>>(), false);
	}

	public Template(String name) {
		super(name, new ArrayList<List<Component>>(), false);
	}
	
	public Template(String name, List<List<Component>> components) {
		super(name, components, false);
	}
	
	
	@Override
	public String getOperator() {
		return RuleOperator.TEMPLATE.toString();
	}

	@Override
	public PrimitiveConstraint toJaCoP(Store store, IntVar[][] variables)
			throws MiniEugeneException {
		
		if(this.isNegated()) {
			return this.toJaCoPNot(store, variables);
		}
		
//		int maxN = variables[Variables.PART].length;
//
//		if(maxN < this.getComponents().size()) {
//			throw new EugeneException("The length of the design ("+maxN+") is less than the template size ("+this.getComponents().size()+")");
//		} else if(maxN % this.getComponents().size() != 0) {
//			throw new EugeneException(
//					"The length of the design ("+maxN+") is not a multiple of the template size ("+this.getComponents().size()+")");
//		}
				
		return createTemplate(variables);

		/*
		 * for templates, we use JaCoP's Extensional Support constraints...
		 * 
		 * those constraints take as input a matrix of ``allowed'' combinations...
		 * we need, however, calculate this matrix and impose it into the store...
		 * 
		 * that's why the constraints are called ``Extensional Support''...
		 * we need to tell the constraint solver what valid combinations are...
		 * i.e. we (``externals'') provide support...
		 */
//		store.impose(
//				createExtensionalSupport(
//						variables, 
//						maxN));
//		return null;
	}
	
	private Constraint createExtensionalSupport(IntVar[][] variables, int N) {

		int[][] template = this.combinations();


		int[][] extSupport = new int[template.length][N];
		for(int i=0; i<N; ) {

			for(int j=0; j<template[0].length; j++, i++) {
				for(int k = 0; k<template.length; k++) {
					extSupport[k][i] = template[k][j];
				}
			}
			
		}
		
		this.printMatrix(extSupport);
		
		// TODO:
		// - how to build the disjunction of Extensional Support constraints?
		return new ExtensionalSupportVA(
				variables[Variables.PART],
				extSupport);
	}
	
	/*
	 * with this function we build all possible combinations
	 * 
	 * Example:
	 * [p1|p2] x [c1|c2] 
	 * 
	 *  [p1, c1]
	 *  [p1, c2]
	 *  [p2, c1]
	 *  [p2, c2]
	 *  
	 *  currently, the algorithm is O ( n*m * log(n*m) )
	 *  n ... number of elements in a template (i.e. 2 in the example)
	 *  m ... number of selections of one templates elements (i.e. 3 in the example)
	 *  
	 *  n and m will, as far as I believe, be small...
	 *  I need to benchmark...
	 *  
	 *  NOTE:
	 *  The algorithm can be improved...
	 *  Any help/improvement is very welcome!
	 */
	private int[][] combinations() {

		// first, we calculate the number of rows
		int rows = 1;
		for(int i=0; i<this.getComponents().size(); i++) {
			rows *= this.getComponents().get(i).size();
		}

		// in the ext matrix, we store the combinations
		int[][] ext = new int[rows][this.getComponents().size()];
		row = 0;
		
		// now, we build the combinations recursively
		this.buildCombinations(this.getComponents(), ext,  0);
		
		// finally, we fill the ``empty'' spots...
		// i.e. all cells that are equal to 0 get the 
		// value from the cell one row above
		for(int i=0; i<ext.length; i++) {
			for(int j=0; j<ext[i].length; j++) {
				if(ext[i][j] == 0) {
					ext[i][j] = ext[i-1][j];
				}
			}
		}
		
		return ext;
	}
	
	
	/*
	 * 
	 */
	private static int row;	
	private void buildCombinations(List<List<Component>> lst, int[][] ext, int col) {
		if(col >= 0 && col < lst.size()) {
			for(int i=0; i<lst.get(col).size(); i++) {
				ext[row][col] = lst.get(col).get(i).getId();
				buildCombinations(lst, ext, col + 1);				
				row++;
			}
			row--;
		}
	}
	
	private void printMatrix(int[][] matrix) {
		// print the array
		for(int k=0; k<matrix.length; k++) {
			for(int j=0; j<matrix[k].length; j++) {
				System.out.print(matrix[k][j]+", ");
			}
			System.out.println();
		}
		
	}
	
	@Override
	public PrimitiveConstraint toJaCoPNot(Store store, IntVar[][] variables)
			throws MiniEugeneException {
//		int maxN = variables[Variables.PART].length;
//		
//		if(maxN < this.getComponents().size()) {
//			throw new EugeneException("I cannot impose "+this.toString());
//		} else if(maxN % this.getComponents().size() != 0) {
//			throw new EugeneException(
//					"The max. length "+maxN+" is not a multiple of the template size ("+this.getComponents().size()+")");
//		}
		
		return new Not(createTemplate(variables));
	}
	
	private PrimitiveConstraint createTemplate(IntVar[][] variables) 
			throws MiniEugeneException {
		
		int N = variables[Variables.PART].length;
		
		PrimitiveConstraint[] pc = null;
		for(int i=0; i<N; i += this.getComponents().size()) {
			
			// get always the first ``selection'' of components of the template
			List<Component> lst_ci = this.getComponents().get(0);
			
			PrimitiveConstraint[] pcTemplate = new PrimitiveConstraint[this.getComponents().size() - 1];
			for(int j=1; j<this.getComponents().size(); j++) {
				List<Component> lst_cj = this.getComponents().get(j);
				
				// disjunction of the possible components
				PrimitiveConstraint[] pcSelection = new PrimitiveConstraint[lst_cj.size()];
				int k=0;
				for(Component cj : lst_cj) {
					pcSelection[k++] = new XeqC(variables[Variables.PART][j+i], cj.getId());
				}
				
				pcTemplate[j-1] = new Or(pcSelection);
			}
			
			
			PrimitiveConstraint[] pcSelection = new PrimitiveConstraint[lst_ci.size()];
			int k=0;
			for(Component ci : lst_ci) {
				pcSelection[k++] = new XeqC(variables[Variables.PART][i], ci.getId());
			}

			if(pc == null) {
				pc = new PrimitiveConstraint[1];
				pc[0] =	new And(
							new Or(pcSelection),
							new And(pcTemplate)); 
			} else {
				pc = ArrayUtils.add(pc, 
					new And(
						new Or(pcSelection),
						new And(pcTemplate)));
			}
		}
		
		if(null == pc) {
			throw new MiniEugeneException("I cannot impose "+this.toString());
		}
		

		return new And(pc);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		if(this.isNegated()) {
			sb.append("NOT ");
		}
		
		if(!bAutoGenerated && null != this.getName() && !this.getName().isEmpty()) {
			sb.append(this.getName()).append(" ");
		}
		sb.append(this.getOperator()).append(" ");
		for(int i=0; i<this.getComponents().size(); i++) {
			
			// SELECTION
			sb.append("[");
			List<Component> lst_selection = this.getComponents().get(i);
			for(int j=0; j<lst_selection.size(); j++) {	
				sb.append(lst_selection.get(j).getName());
				
				if(j < lst_selection.size() - 1) {
					sb.append("|");
				}
			}
					
			sb.append("]");
			
			if(i<this.getComponents().size()-1) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}
}
