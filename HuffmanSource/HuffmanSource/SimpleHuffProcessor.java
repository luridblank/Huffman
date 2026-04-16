/*  Student information for assignment:
 *
 *  On <MY|OUR> honor, <NAME1> (and <NAME2),
 *  this programming assignment is <MY|OUR> own work
 *  and <I|WE> have not provided this code to any other student.
 *
 *  Number of slip days used:
 *
 *  Student 1: Pavel Grinev
 *  UTEID: pg26747
 *  email address: pg26747@my.utexas.edu
 *
 *  Student 2: Elizabeth Kuromiema
 *  UTEID: eik254
 *  email address: eik254@my.utexas.edu
 *
 *  Grader name:
 *  Section number:
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SimpleHuffProcessor implements IHuffProcessor {

    private IHuffViewer myViewer;
    private HuffmanTree tree;
    private int[] counts;
    private String[] codes;
    private int header;
    private int savedBits;


    /**
     * Preprocess data so that compression is possible ---
     * count characters/create tree/store state so that
     * a subsequent call to compress will work. The InputStream
     * is <em>not</em> a BitInputStream, so wrap it int one as needed.
     *
     * @param in           is the stream which could be subsequently compressed
     * @param headerFormat a constant from IHuffProcessor that determines what kind of
     *                     header to use, standard count format, standard tree format, or
     *                     possibly some format added in the future.
     * @return number of bits saved by compression or some other measure
     * Note, to determine the number of
     * bits saved, the number of bits written includes
     * ALL bits that will be written including the
     * magic number, the header format number, the header to
     * reproduce the tree, AND the actual data.
     * @throws IOException if an error occurs while reading from the input file.
     */
    public int preprocessCompress(InputStream in, int headerFormat) throws IOException {
        header = headerFormat;

        BitInputStream bitIn = new BitInputStream(in);
        counts = getFrequencies(bitIn);
        bitIn.close();

        tree = new HuffmanTree(counts);
        TreeNode root = tree.getRoot();
        codes = tree.getCodes(root);

        showString("PSEUDO_EOF code length=" + codes[PSEUDO_EOF].length());
        showString("distinct symbols (nonzero counts)=" + countNonZeroCounts(counts));

        int originalBits = 0;
        for (int value = 0; value < ALPH_SIZE; value++) {
            originalBits += counts[value] * BITS_PER_WORD;
        }

        int compressedBits = 0;
        for (int value = 0; value < ALPH_SIZE; value++) {
            if (counts[value] > 0 && codes[value] != null) {
                compressedBits += counts[value] * codes[value].length();
            }
        }

        compressedBits += codes[PSEUDO_EOF].length();

        int headerBits;

        if (headerFormat == STORE_COUNTS) {
            headerBits = ALPH_SIZE * BITS_PER_INT;
        } else if (headerFormat == STORE_TREE) {
            headerBits = BITS_PER_INT + countTreeBits(root);
        } else {
            throw new IllegalArgumentException("unknown header format");
        }

        int totalBits = BITS_PER_INT + BITS_PER_INT + headerBits + compressedBits;
        savedBits = originalBits - totalBits;

        showString("--- preprocessCompress ---");
        showString("headerFormat=" + headerFormat + (headerFormat == STORE_COUNTS ? " (STORE_COUNTS)" : " (STORE_TREE)"));
        showString("origBits=" + originalBits);
        showString("dataBits(no header)=" + compressedBits);
        showString("headerBits=" + headerBits);
        showString("magic+format bits=" + (BITS_PER_INT + BITS_PER_INT));
        showString("totalBits=" + totalBits);
        showString("savedBits=" + savedBits);

        return originalBits - totalBits;
    }

    private int countNonZeroCounts(int[] a) {
    int c = 0;
    for (int x : a) if (x > 0) c++;
    return c;
}

    /**
     * Reads the stream one byte (8 bits) at a time and returns occurrence counts for each value.
     * Length is ALPH_SIZE + 1 so HuffmanTree.buildTree (and SCF headers) have index PSEUDO_EOF;
     * that slot stays 0 here because that symbol never appears in raw file bytes.
     */
    private int[] getFrequencies(BitInputStream bitIn) throws IOException {
        int[] counts = new int[ALPH_SIZE + 1];
        int bit = bitIn.readBits(BITS_PER_WORD);
        while (bit != -1) {
            counts[bit]++;
            bit = bitIn.readBits(BITS_PER_WORD);
        }
        return counts;
    }

    private int countTreeBits(TreeNode node) {
        if (node.isLeaf()) {
            return 1 + (BITS_PER_WORD + 1);
        }
        return 1 + countTreeBits(node.getLeft()) + countTreeBits(node.getRight());
    }

    /**
     * Compresses input to output, where the same InputStream has
     * previously been pre-processed via <code>preprocessCompress</code>
     * storing state used by this call.
     * <br> pre: <code>preprocessCompress</code> must be called before this method
     *
     * @param in    is the stream being compressed (NOT a BitInputStream)
     * @param out   is bound to a file/stream to which bits are written
     *              for the compressed file (not a BitOutputStream)
     * @param force if this is true create the output file even if it is larger than the input file.
     *              If this is false do not create the output file if it is larger than the input file.
     * @return the number of bits written.
     * @throws IOException if an error occurs while reading from the input file or
     *                     writing to the output file.
     */
    public int compress(InputStream in, OutputStream out, boolean force) throws IOException {
        if (!force && savedBits < 0) {
            myViewer.showError("Could not compress because output is larger than input");
            return 0;
        }

        BitOutputStream bitOut = new BitOutputStream(out);
        BitInputStream bitIn = new BitInputStream(in);
        int writtenBits = writeHeader(bitOut);

        int val = bitIn.readBits(BITS_PER_WORD);

        while (val != -1) {
            String code = codes[val];
            for (int i = 0; i < code.length(); i++) {
                bitOut.writeBits(1, code.charAt(i) - '0');
            }
            writtenBits += code.length();
            val = bitIn.readBits(BITS_PER_WORD);
        }

        String eofCode = codes[PSEUDO_EOF];
        for (int i = 0; i < eofCode.length(); i++) {
            bitOut.writeBits(1, eofCode.charAt(i) - '0');
        }
        writtenBits += eofCode.length();

        bitIn.close();
        bitOut.close();
        return writtenBits;
    }

    private int writeHeader(BitOutputStream bitOut) {
        bitOut.writeBits(BITS_PER_INT, MAGIC_NUMBER);
        int writtenBits = 0;
        writtenBits += BITS_PER_INT;

        bitOut.writeBits(BITS_PER_INT, header);
        writtenBits += BITS_PER_INT;

        if (header == STORE_COUNTS) {
            for (int value = 0; value < ALPH_SIZE; value++) {
                bitOut.writeBits(BITS_PER_INT, counts[value]);
                writtenBits += BITS_PER_INT;
            }
        } else if (header == STORE_TREE) {
            TreeNode root = tree.getRoot();
            int treeBits = countTreeBits(root);
            bitOut.writeBits(BITS_PER_INT, treeBits);
            writtenBits += BITS_PER_INT;
            writtenBits += compressTree(root, bitOut);
        } else {
            throw new IllegalStateException("Unknown header format.");
        }
        return writtenBits;
    }

    private int compressTree(TreeNode node, BitOutputStream out) {
        if (node.isLeaf()) {
            out.writeBits(1, 1);
            out.writeBits(BITS_PER_WORD + 1, node.getValue());
            return 1 + BITS_PER_WORD + 1;
        } else {
            out.writeBits(1, 0);
            int bits = 1;
            bits += compressTree(node.getLeft(), out);
            bits += compressTree(node.getRight(), out);
            return bits;
        }
    }

    /**
     * Uncompress a previously compressed stream in, writing the
     * uncompressed bits/data to out.
     *
     * @param in  is the previously compressed data (not a BitInputStream)
     * @param out is the uncompressed file/stream
     * @return the number of bits written to the uncompressed file/stream
     * @throws IOException if an error occurs while reading from the input file or
     *                     writing to the output file.
     */
    public int uncompress(InputStream in, OutputStream out) throws IOException {

        // Read compressed bits and emit recovered bytes.
        BitInputStream bitIn = new BitInputStream(in);
        BitOutputStream bitOut = new BitOutputStream(out);

        // First 32 bits must be the Huffman file signature.
        int magic = bitIn.readBits(BITS_PER_INT);
        checkMagicNumber(magic);

        // Header determines how to rebuild the decoding tree.
        int headerValue = bitIn.readBits(BITS_PER_INT);
        if (headerValue == -1) {
            bitIn.close();
            bitOut.close();
            throw new IOException("Bad header: missing header format");
        }
        TreeNode root;

        if (headerValue == STORE_COUNTS) {
            // Rebuild tree from 256 stored frequencies.
            counts = fillHeader(bitIn);
            tree = new HuffmanTree(counts);
            root = tree.getRoot();
        } else if (headerValue == STORE_TREE) {
            // Rebuild tree directly from serialized preorder bits.
            root = buildTreeFromTree(bitIn);
        } else {
            bitIn.close();
            bitOut.close();
            throw new IOException("Bad header format in compressed file");
        }

        if (root == null) {
            throw new IOException("Bad header: unable to build decoding tree");
        }

        int writtenBits = decode(root, bitIn, bitOut);
        
        bitIn.close();
        bitOut.close();
        return writtenBits;
    }

    private void checkMagicNumber(int bits) throws IOException {
        // Reject files that do not start with MAGIC_NUMBER.
        if (bits != MAGIC_NUMBER) {
            throw new IOException("Not a Huffman-compressed file: bad magic number");
        }
    }

    private int[] fillHeader(BitInputStream bitIn) throws IOException {
        // Count format stores 256 frequencies, then we restore PSEUDO_EOF.
        int[] freqs = new int[ALPH_SIZE + 1];

        for (int value = 0; value < ALPH_SIZE; value++) {
            // Each frequency is stored in a full 32-bit int.
            int count = bitIn.readBits(BITS_PER_INT);
            if (count == -1) {
                throw new IOException("Bad header: not enough count data");
            }
            freqs[value] = count;
        }

        // PSEUDO_EOF is not stored in count format; restore it here.
        freqs[PSEUDO_EOF] = 1;
        return freqs;
    }

    private TreeNode buildTreeFromTree(BitInputStream bitIn) throws IOException {
        // Tree format starts with number of bits used by the serialized tree.
        int treeBits = bitIn.readBits(BITS_PER_INT);
        if (treeBits == -1) {
            throw new IOException("missing tree size");
        }

        // Track consumed tree bits so we can validate the header size.
        int[] bitsUsed = {0};
        TreeNode root = readTree(bitIn, bitsUsed);
        if (bitsUsed[0] != treeBits) {
            throw new IOException("Bad header: tree size mismatch");
        }
        return root;
    }

    private TreeNode readTree(BitInputStream bitIn, int[] bitsUsed) throws IOException {
        // Preorder decode: 1 => leaf (next 9 bits), 0 => internal (read left, then right).
        int marker = bitIn.readBits(1);
        if (marker == -1) {
            throw new IOException("unexpected end of file");
        }
        bitsUsed[0]++;

        if (marker == 1) {
            // Leaf node stores one 9-bit value (0..256).
            int value = bitIn.readBits(BITS_PER_WORD + 1);
            if (value == -1) {
                throw new IOException("no leaf value");
            }
            bitsUsed[0] += BITS_PER_WORD + 1;
            return new TreeNode(value, 0);
        }

        // Internal node: recursively decode left subtree, then right.
        TreeNode left = readTree(bitIn, bitsUsed);
        TreeNode right = readTree(bitIn, bitsUsed);
        return new TreeNode(left, 0, right);
    }

    private int decode(TreeNode root, BitInputStream bitIn, BitOutputStream bitOut) throws IOException {
        int writtenBits = 0;
        boolean done = false;
        TreeNode current = root;

        while(!done) {
            int bit = bitIn.readBits(1);
            if(bit == -1) {
                throw new IOException("Unexpected end of file, no PSEUDO_EOF found");
            } else {

                // traverse tree
                if(bit == 0) {
                    current = current.getLeft();
                } else {
                    current = current.getRight();
                }

                if (current == null) {
                    throw new IOException("Bad compressed data: traversed to null node");
                }

                // check if reached leaf
                if(current.isLeaf()) {
                    if(current.getValue() == PSEUDO_EOF) {
                        done = true;
                    } else {
                        // write the character and reset
                        bitOut.writeBits(BITS_PER_WORD, current.getValue());
                        writtenBits += BITS_PER_WORD;
                        current = root;
                    }
                }
            }
        }
        return writtenBits;
    }


    public void setViewer(IHuffViewer viewer) {
        myViewer = viewer;
    }

    private void showString(String s) {
        if (myViewer != null) {
            myViewer.update(s);
        }
    }
}
