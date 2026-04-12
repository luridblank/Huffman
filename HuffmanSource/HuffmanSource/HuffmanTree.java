public class HuffmanTree implements IHuffConstants {

    private TreeNode root;
    private int size;

    public HuffmanTree() {
        root = null;
        size = 0;
    }

    public void buildTree(int[] frequencies) {
        if (frequencies == null || frequencies.length != ALPH_SIZE + 1) {
            throw new IllegalArgumentException("frequencies must have length ALPH_SIZE + 1");
        }

        PriorityQueue queue = new PriorityQueue();
        size = 0;

        // Create one leaf per symbol with a nonzero freq
        for (int value = 0; value < ALPH_SIZE; value++) {
            if (frequencies[value] > 0) {
                queue.enqueue(new TreeNode(value, frequencies[value]));
                size++;
            }
        }
        queue.enqueue(new TreeNode(PSEUDO_EOF, 1));
        size++;

        // Merge two lowest freq nodes until one root remains
        while (queue.size() > 1) {
            TreeNode left = queue.dequeue();
            TreeNode right = queue.dequeue();
            queue.enqueue(new TreeNode(left, 0, right));
        }

        // The final node in the queue is the root of the Huffman tree.
        root = queue.dequeue();
    }

    public String[] getCodes(TreeNode tree) {
        String[] codes = new String[ALPH_SIZE + 1];
        generateCodes(tree, "", codes);
        return codes;
    }

    private void generateCodes(TreeNode node, String code, String[] codes) {
        if (node.isLeaf()) {
            codes[node.getValue()] = code;
        } else {
            generateCodes(node.getLeft(), code + "0", codes);
            generateCodes(node.getRight(), code + "1", codes);
        }
    }

    public TreeNode getRoot() {
        return root;
    }

    public int size() {
        return size;
    }

}
