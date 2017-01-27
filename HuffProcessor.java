/*
 * The compress and decompress methods. 
 * 
 * @author Dave Gay
 * @since November, 2015
 */

import java.util.*;
public class HuffProcessor implements Processor {


	String[] extractedCodes = new String[ALPH_SIZE+1];		//Global array to hold the path to each node. It is APLH_SIZE+1 to hold each character in the sequence plus the pseudo EOF character.

	/*
	 * This is a helper method to get the codes for each node and put them in our global array 
	 * that holds each path (string of 0s and 1s).
	 */
	private void extractCodes(HuffNode current, String path){			
		if (current.left() == null && current.right() == null){
			extractedCodes[current.value()]=path;
			return;
		}

		extractCodes(current.left(), path + "0");
		extractCodes(current.right(), path + "1");
	}


	/*
	 * This is a helper method to write the Huffman Header, which will allow us to recreate the exact tree
	 * and accurately decompress the file.
	 */
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





	/*
	 *						Steps of Compress Method
	 * 
	 * Step 1: Loop through entire input stream and count occurrences of characters in the file.
	 * Step 2: Create a Huffman Tree (build HuffNodes and use the Huffman algorithm.)
	 * Step 3: Traverse tree and extract all the codes from the tree.
	 * Step 4: Write the file header (contains info for decompress to recreate the Huffman Tree.)
	 * Step 5: Compress and write the body of the file.
	 * Step 6: Write the psuedo EOF so we know when to stop decompressing.
	 * 
	 */


	/*
	 * This is the method to compress files. It calls two helper methods, extractCodes and writeHeader.
	 */
	@Override
	public void compress(BitInputStream in, BitOutputStream out) {	
		int[] countOccur = new int[ALPH_SIZE];			//Here I'm reading the file and counting the occurrence of each character by incrementing a counter in an Integer array. The position I increment corresponds to the ASCII value of the character encountered.
		
		for (int i=0; i< ALPH_SIZE; i++){				//Initialize each count to 0 before reading the file.
			countOccur[i]=0;
		}
		
		int nextCharacter = in.readBits(BITS_PER_WORD);
		while(nextCharacter != -1){						//Increment the Array.
			countOccur[nextCharacter]++;
			nextCharacter = in.readBits(BITS_PER_WORD);
		}
		in.reset();										//Reset the BitInputStream so I can use it again later in the method.


		
		PriorityQueue<HuffNode> queueNode = new PriorityQueue<HuffNode>();		//Here I'm creating a priority queue full of each of the nodes that correspond to characters that occur at least once in the file to be compressed.
		for (int i=0; i<ALPH_SIZE; i++){
			if (countOccur[i]>0){
				queueNode.add(new HuffNode(i, countOccur[i]));
			}
		}
		queueNode.add(new HuffNode(PSEUDO_EOF, 0));			//Adding one additional node, the pseudo end of file character, to signal the method when it has finished compressing the file.

		
		//We now have Priority Queue full of nodes


		while (queueNode.size()>1){					//Here I'm building my tree by pulling the two nodes with the smallest weight, combining them, and reinserting the new node into the priority queue. I repeat this until my priority queue only has one node, the root of the Huffman Tree.
			HuffNode left = queueNode.poll();
			HuffNode right = queueNode.poll();
			int combinedWeight = left.weight()+right.weight();
			queueNode.add(new HuffNode(-1, combinedWeight, left, right)); 
		}

		HuffNode root = queueNode.poll();
		extractCodes(root, "");						//Here I'm traversing tree and extracting codes for each node.
		out.writeBits(BITS_PER_INT, HUFF_NUMBER);	//Here I'm writing the HuffNumber at the beginning of the file to signal to the decompress method that it is okay to proceed with decompression.
		writeHeader(root, out);						//Here I'm writing the Header, which will allow the decompression method to assemble an identical Huffman Tree to ensure accurate decompression.


		int nextChar = in.readBits(BITS_PER_WORD);	//Here I'm compressing the body of the file by writing getting the codes from the array I initialized earlier and writing them to the BitOutputStream.
		while (nextChar != -1){
			String code = extractedCodes[nextChar];				
			out.writeBits(code.length(), Integer.parseInt(code, 2));
			nextChar = in.readBits(BITS_PER_WORD);
		}

		String code = extractedCodes[PSEUDO_EOF];				//Here I'm writing the pseudo end of file character so that I don't have to compress/decompress to the end of the file once I've already read all of the characters.
		out.writeBits(code.length(), Integer.parseInt(code, 2));
		in.reset();												//Resetting the BitInputStream in case I have to come back later and use it again.
	}







	/*
	 * 		Steps of Decompress Method
	 * 
	 * Step 1: Check beginning of file for HuffNumber
	 * Step 2: Recreate HuffMan Tree from header
	 * Step 3: Parse body of compressed file
	 */
	
	
	/*
	 * This is the decompress method. It calls one helper method to read the header of the compressed file.
	 */
	@Override
	public void decompress(BitInputStream in, BitOutputStream out) {
		if (in.readBits(BITS_PER_INT)!=HUFF_NUMBER){			//Here we check the beginning of the file to see if it contains a HuffNumber. If not, the file has not been compressed properly, so we throw a new HuffException.
			throw new HuffException("File does not begin with HuffNumber");
		}

		HuffNode root = readHeader(in);			//Here we call a helper method to recreate the exact Huffman tree used in the compression method. This allows us to accurately decompress the file.
		
		HuffNode current = root;
		int nextChar = in.readBits(1);			//Here we decompress the body of the file one bit at a time by reading the file and going left or right down our recreated tree until we reach a leaf. Once we reach a leaf, we record the character and reset our node to the root for the next character to be decompressed.
		while (nextChar !=-1){
			if (nextChar == 1){					
				current = current.right();
			}
			else {
				current = current.left();
			}

			if (current.left() == null && current.right() == null){		//If we're at a leaf node
				if (current.value() == PSEUDO_EOF){						//If the leaf is the pseudo EOF, we know we're done decompressing and can return.
					return;
				}

				else {													//Otherwise, we're at a character, so we write the character to our BitOutputStream
					out.writeBits(BITS_PER_WORD,current.value());
					current = root;										//Reset the node to the root so we can find our next character
				}

			}
			nextChar = in.readBits(1);									//Read in the next bit.

		}

		throw new HuffException("Missed the PSEUDO_EOF");				//The only way we could get here in the code is if we missed our PSEUDO_EOF, so we throw a new HuffException.


	}

	private HuffNode readHeader(BitInputStream in){		//This is my helper method to read the header and recreate the exact Huffman Tree we used in compression.
		if (in.readBits(1)==0){
			HuffNode left = readHeader(in);
			HuffNode right = readHeader(in);
			return new HuffNode(-1,1,left,right);
		}
		else{
			return new HuffNode(in.readBits(9),0);
		}

	}

}
