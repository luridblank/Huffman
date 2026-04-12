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

        tree = new HuffmanTree();
        tree.buildTree(counts);
        TreeNode root = tree.getRoot();
        codes = tree.getCodes(root);

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
            headerBits = countTreeBits(root);
        } else {
            throw new IllegalArgumentException("unknown header format");
        }

        // One 32-bit word: STORE_COUNTS or STORE_TREE (magic | format), then header, then Huffman-coded body
        int totalBits = BITS_PER_INT + headerBits + compressedBits;
        savedBits = originalBits - totalBits;
        return originalBits - totalBits;
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
        if (tree == null || counts == null || codes == null) {
            throw new IOException("preprocessCompress must be called before compress");
        }

        if (!force && mySavedBits < 0) {
            showString("compression skipped: output would be larger than input");
            return 0;
        }

        int bitsWritten = 0;
        BitOutputStream bitOut = new BitOutputStream(out);
        BitInputStream bitIn = new BitInputStream(in);

        bitsWritten += writeHeader(bitOut);

        int value = bitIn.readBits(BITS_PER_WORD);
        while (value != -1) {
            bitsWritten += writeCode(bitOut, codes[value]);
            value = bitIn.readBits(BITS_PER_WORD);
        }

        bitsWritten += writeCode(bitOut, codes[PSEUDO_EOF]);

        bitIn.close();
        bitOut.close();
        return bitsWritten;
    }

    private int writeHeader(BitOutputStream bitOut) {
        int bitsWritten = 0;

        bitOut.writeBits(BITS_PER_INT, MAGIC_NUMBER);
        bitsWritten += BITS_PER_INT;

        bitOut.writeBits(BITS_PER_INT, myHeaderFormat);
        bitsWritten += BITS_PER_INT;

        if (myHeaderFormat == STORE_COUNTS) {
            for (int value = 0; value < ALPH_SIZE; value++) {
                bitOut.writeBits(BITS_PER_INT, counts[value]);
                bitsWritten += BITS_PER_INT;
            }
        } else if (myHeaderFormat == STORE_TREE) {
            bitsWritten += writeTreeHeader(tree.getRoot(), bitOut);
        } else {
            throw new IllegalStateException("unknown header format");
        }

        return bitsWritten;
    }

    private int writeTreeHeader(TreeNode node, BitOutputStream bitOut) {
        if (node.isLeaf()) {
            bitOut.writeBits(1, 1);
            bitOut.writeBits(BITS_PER_WORD + 1, node.getValue());
            return 1 + (BITS_PER_WORD + 1);
        }

        bitOut.writeBits(1, 0);
        return 1 + writeTreeHeader(node.getLeft(), bitOut) + writeTreeHeader(node.getRight(), bitOut);
    }

    private int writeCode(BitOutputStream bitOut, String code) {
        for (int i = 0; i < code.length(); i++) {
            bitOut.writeBits(1, code.charAt(i) - '0');
        }
        return code.length();
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
        throw new IOException("uncompress not implemented");
        //return 0;
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
