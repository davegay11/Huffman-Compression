/*
 * Creating the class HuffNode, which are nodes of a Huffman tree.
 * 
 * I implemented a basic constructor, a comparable based on weight (weight = # of occurences of a character in the file to 
 * be compressed) and simple functions to return useful information about the HuffNodes.
 * 
 * @author Dave Gay
 * @since November, 2015
 */

public class HuffNode implements Comparable<HuffNode> {
	
	private int myValue, myWeight;
	private HuffNode myLeft, myRight;
	
	public HuffNode(int value, int weight) {
		this(value, weight, null, null);
	}
	
	public HuffNode(int value, int weight, HuffNode left, HuffNode right) {
		myValue = value;
		myWeight = weight;
		myLeft = left;
		myRight= right;
	}
	
	public int compareTo(HuffNode other) {
		return myWeight - other.myWeight;
	}
	
	public int value() {
		return myValue;
	}
	
	public int weight() {
		return myWeight;
	}
	
	public HuffNode left() {
		return myLeft;
	}
	
	public HuffNode right() {
		return myRight;
	}
	
	public String toString() {
		return myValue + "";
	}
}
