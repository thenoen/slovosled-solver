package sk.thenoen.slovosledsolver.model;

public class Letter {

	private String letter;
	private int value;
	private int usageCount;
	private boolean selected;



//	public Letter(String letter, int value, int usageCount, boolean selected) {
//		super();
//		this.letter = letter;
//		this.value = value;
//		this.usageCount = usageCount;
//	}

	public String getLetter() {
		return letter;
	}

	public void setLetter(String letter) {
		this.letter = letter;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public int getUsageCount() {
		return usageCount;
	}

	public void setUsageCount(int usageCount) {
		this.usageCount = usageCount;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
}
