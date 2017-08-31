/*
 * @author Dave Gay
 * @since November, 2015
 */
import java.util.*;
public class HuffProcessor implements Processor {

	String[] extractedCodes = new String[ALPH_SIZE+1];
	
	private void extractCodes(HuffNode current, String path){			
		if (current.left() == null && current.right() == null){
			extractedCodes[current.value()]=path;
			return;
		}
		extractCodes(current.left(), path + "0");
		extractCodes(current.right(), path + "1");
	}

	private void writeHeader(HuffNode current, BitOutputStream out){
		if (current.left() == null && current.right()==null){
			out.writeBits(1, 1);
			out.writeBits(9,current.value());
			return;
		}
		out.writeBits(1,0);

		writeHeader(current.left(), out);
		writeHeader(current.right(), out);
	}

	@Override
	public void compress(BitInputStream in, BitOutputStream out) {	
		int[] countOccur = new int[ALPH_SIZE];
		
		int nextCharacter = in.readBits(BITS_PER_WORD);
		while(nextCharacter != -1){					
			countOccur[nextCharacter]++;
			nextCharacter = in.readBits(BITS_PER_WORD);
		}
		in.reset();			

		PriorityQueue<HuffNode> queueNode = new PriorityQueue<HuffNode>();	
		for (int i = 0; i < ALPH_SIZE; i++){
			if (countOccur[i] > 0){
				queueNode.add(new HuffNode(i, countOccur[i]));
			}
		}
		queueNode.add(new HuffNode(PSEUDO_EOF, 0));	
		
		while (queueNode.size() > 1){		
			HuffNode right = queueNode.poll();
			int combinedWeight = left.weight() + right.weight();
			queueNode.add(new HuffNode(-1, combinedWeight, left, right)); 
		}

		HuffNode root = queueNode.poll();
		extractCodes(root, "");					
		out.writeBits(BITS_PER_INT, HUFF_NUMBER);
		writeHeader(root, out);	

		int nextChar = in.readBits(BITS_PER_WORD);	
		while (nextChar != -1){
			String code = extractedCodes[nextChar];				
			out.writeBits(code.length(), Integer.parseInt(code, 2));
			nextChar = in.readBits(BITS_PER_WORD);
		}

		String code = extractedCodes[PSEUDO_EOF];			
		out.writeBits(code.length(), Integer.parseInt(code, 2));
		in.reset();										
	}

	@Override
	public void decompress(BitInputStream in, BitOutputStream out) {
		if (in.readBits(BITS_PER_INT)!=HUFF_NUMBER){	
			throw new HuffException("File does not begin with HuffNumber");
		}

		HuffNode root = readHeader(in);		
		
		HuffNode current = root;
		int nextChar = in.readBits(1);		
		while (nextChar !=-1){
			if (nextChar == 1){					
				current = current.right();
			}
			else {
				current = current.left();
			}

			if (current.left() == null && current.right() == null){		//If we're at a leaf node
				if (current.value() == PSEUDO_EOF){
					return;
				} else {		
					out.writeBits(BITS_PER_WORD,current.value());
					current = root;									
				}
			}
			nextChar = in.readBits(1);		
		}
		throw new HuffException("Missed the PSEUDO_EOF");
	}

	private HuffNode readHeader(BitInputStream in){	
		if (in.readBits(1) == 0){
			HuffNode left = readHeader(in);
			HuffNode right = readHeader(in);
			return new HuffNode(-1, 1, left, right);
		} else {
			return new HuffNode(in.readBits(9), 0);
		}
	}
}
