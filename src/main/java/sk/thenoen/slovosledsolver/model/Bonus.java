package sk.thenoen.slovosledsolver.model;

public class Bonus {

	private String pattern;
	private int value;
	private String text;
	private int index;

	public String getPattern() {
		return pattern;
	}

	public int[] getPatternAsArray() {
		return pattern.chars()
					  .map(c -> c - '0')
					  .toArray();
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
}
