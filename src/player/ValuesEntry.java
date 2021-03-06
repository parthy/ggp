package player;

import java.io.Serializable;

/**
 *
 * @author konrad
 */
class ValuesEntry implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8239692783219098472L;
	private int[] values;
	private int occ;

	public ValuesEntry(int[] values, int occ){
		this.values = values;
		this.occ = occ;
	}

	public int[] getGoalArray(){
		return values;
	}

	public void setGoalArray(int[] values){
		this.values = values;
	}

	public int getOccurences(){
		return occ;
	}

	public void setOccurences(int occ){
		this.occ = occ;
	}

	public String toString() {
		String str = "[";
		for(int i=0; i<values.length; i++) {
			str += (i == values.length-1) ? values[i]+"]" : values[i]+", ";
		}
		return str+"^"+occ;
	}

}
