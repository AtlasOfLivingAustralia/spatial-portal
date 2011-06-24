package au.org.emii.portal.util;

public class Sequence {
	private int value;
	
	public Sequence() {
		value = 0;
	}
	
	public Sequence(int initialValue) {
		value = initialValue;
	}
	
	public int increment() {
		return ++value;
	}
	
	public int getValue() {
		return value;
	}
	
}
