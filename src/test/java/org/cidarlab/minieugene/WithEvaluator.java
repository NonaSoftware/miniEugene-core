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

package org.cidarlab.minieugene;

import org.cidarlab.minieugene.MiniEugene;
import org.cidarlab.minieugene.constants.MiniEugeneConstants;

public class WithEvaluator {

	private static final int MAX_DESIGN_SIZE = 10; 
	private static final int NUMBER_OF_RUNS = 20; 
	private static final String NEWLINE = System.getProperty("line.separator"); 
	
	public void evaluate() {
		double[] all_times = new double[MAX_DESIGN_SIZE];
		double[] times = new double[NUMBER_OF_RUNS];
		
		all_times[0] = 0;
		for(int i=2; i<=MAX_DESIGN_SIZE; i++) {
			all_times[i-1] = 0;
			
			System.out.println("*** i: "+i);
			String s=buildScript(i);
			
			for(int k = 0; k < NUMBER_OF_RUNS; k++) {
				
				try {
					MiniEugene me = new MiniEugene();				
		            me.solve(s.split(NEWLINE), i, -1);

					times[k] = me.getStatistics().getValueByKey(MiniEugeneConstants.SOLUTION_FINDING_TIME);
					
					me = null;
					
					System.gc();
					
				} catch(Exception e) {
					e.printStackTrace();
				}
				
			}
			
			all_times[i-1] = calculate_average(times);
		}
		
		rPlot(all_times);
	}
	
	private void rPlot(double[] array) {
		String NEWLINE = System.getProperty("line.separator");
		
		StringBuilder x = new StringBuilder();
		x.append("x=c(");
		StringBuilder y = new StringBuilder();
		y.append("y=c(");
		
		for(int i=0; i<array.length-1; i++) {
			x.append((i+1)).append(",");			
			y.append(array[i]).append(",");			
		}
		
		x.append(array.length).append(");").append(NEWLINE);
		y.append(array[array.length-1]).append(");").append(NEWLINE);
		
		System.out.print(x.toString());
		System.out.print(y.toString());
	}
	
	private double calculate_average(double[] array) {
		double sum = 0.0;
		
		int k=0;
		for(int i=0; i<array.length; i++) {
			sum += array[i];
			
			if(i>=10) {
				k++;
			}
		}
		
		return sum/k;
	}
	public String buildScript(int N) {
		String NEWLINE = System.getProperty("line.separator");
		
		StringBuilder sb = new StringBuilder();
		for(int i=1; i<N; i++) {
			sb.append(i).append(" WITH ").append(i+1).append(NEWLINE);
		}
		return sb.toString();
	}
	
	public static void main(String[] args) {
		WithEvaluator we = new WithEvaluator();
		we.evaluate();
	}
}
