# Huffman-Compression
A java implementation of David Huffman's algorithm for lossless data compression and decompression

I originally wrote this implementation in November, 2015. My implementation has an average space savings of 43% for regular text files. It has about an 8% space savings for grayscale images and 3% space savings for color images. The reduced savings for images stem from the fact that there are many more possible values for "characters" (pixels) in images than in a typical ASCII text file. The Huffman algorithm works by identifying the most commonly occuring characters and representing them with fewer bits than less common characters. Thus, the less variety of possible values of characters in a file, the more likely characters are to repeat and the more space saved.
