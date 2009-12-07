/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package player;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 *
 * ok, this is game theory
 * input as matrix -> the winvalues for player1 -> between 0-100
 * only zero-sum games
 * the out put will be 2 vectors
 *
 * @author konrad
 */
public class SimplexSolver {
	private static int pivotX;
	private static int pivotY;
	private static Float pivotVal;

	public static void main(String[] args){

		Random random = new Random();
		//just one testcase
		LinkedList<Float> line1 = new LinkedList<Float>();
		line1.add(60f);
		line1.add(40f);
//		line1.add(new Float(random.nextInt(101)));
//		line1.add(new Float(random.nextInt(101)));

		LinkedList<Float> line2 = new LinkedList<Float>();
		line2.add(0f);
		line2.add(0f);
//		line2.add(new Float(random.nextInt(101)));
//		line2.add(new Float(random.nextInt(101)));

		LinkedList<LinkedList<Float>> problem = new LinkedList<LinkedList<Float>>();
		problem.add(line1);
		problem.add(line2);

		System.out.println(problem);

		problem = preprocess(problem);

		System.out.println(problem);

		while(!isSolved(problem)){
			System.out.println("======= next step ===========");
			System.out.println(isSolved(problem));
		
			//set pivotX, pivotY, pivotValue
			choosePivot(problem);
			System.out.println("x: "+pivotX+" y: "+pivotY+" val: "+pivotVal);

			problem = oneStep(problem);
			System.out.println(problem);
		}
	}

	private static void choosePivot(LinkedList<LinkedList<Float>> problem) {
		//choose column
		pivotX = 0;
		Float min = null;
		LinkedList<Float> lastline = problem.get(problem.size()-1);
		for(int i=0; i<(lastline.size()-1); i++){
			if(min == null || lastline.get(i) < min){
				pivotX = i;
				min =  lastline.get(i);
			}
		}

		min = null;
		pivotY = 0;
		//each row
		for(int j=0; j<(problem.size()-1); j++){
			Float valY = problem.get(j).get(problem.get(j).size()-1) / (-problem.get(j).get(pivotX));
			if(valY < 0 && (min == null || valY < min)){
				pivotY = j;
				min = valY;
			}
		}

		// calc it
		pivotVal = problem.get(pivotY).get(pivotX);

	}

	private static boolean isSolved(LinkedList<LinkedList<Float>> problem) {

		LinkedList<Float> lastline = problem.get(problem.size()-1);
		for(int i=0; i<(lastline.size()-1); i++){
			if(lastline.get(i) < 0)
				return false;
		}

		return true;
	}

	private static LinkedList<LinkedList<Float>> oneStep(LinkedList<LinkedList<Float>> problem) {
		LinkedList<LinkedList<Float>> result = new LinkedList<LinkedList<Float>>();
		//each row
		for(int i=0; i<problem.size(); i++){
			LinkedList<Float> line = problem.get(i);
			LinkedList<Float> newLine = new LinkedList<Float>();

			//each element
			for(int j=0; j<line.size(); j++){
				Float element = line.get(j);
				//check which rule to apply

				// same column as pivot
				if(j == pivotX){
					//is it the pivot ?
					if(i == pivotY){
						// PE -> 1/
						newLine.add(1/element);
					} else {
						// PS -> x/PE
						newLine.add(element/pivotVal);
					}
				} else {
					if(i == pivotY){
						//PZ -> x/-PE
						newLine.add(-1*element/pivotVal);
					} else {
						// rectangle rule
						newLine.add(element-(line.get(pivotX)*problem.get(pivotY).get(j)/pivotVal));
					}
				}
			}
			result.add(newLine);
		}
		return result;
	}

	/**
	 * make the tableau complete
	 * andd substract 200 from every element
	 *
	 * @param problem
	 * @return
	 */
	private static LinkedList<LinkedList<Float>> preprocess(LinkedList<LinkedList<Float>> problem) {

		Integer length = null;

		for(int i=0; i<problem.size(); i++){
			LinkedList<Float> line = problem.get(i);
			//substract 100
			for(int j=0; j<line.size(); j++){
				line.set(j, line.get(j)-200);
			}
			if(length == null)
				length = line.size();
			line.add(1f);
		}

		LinkedList line = new LinkedList<Float>();
		for(int i=0; i<length; i++){
			line.add(-1f);
		}
		line.add(0f);
		problem.add(line);

		return problem;
	}

}
