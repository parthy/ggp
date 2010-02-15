package player;

/**
 *
 * @author konrad
 */
class ValuesEntry {
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


}
