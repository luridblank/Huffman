public class HuffmanTree implements IHuffConstants {

    private TreeNode root;
    private int size;

    public HuffmanTree() {
        root = null;
        size = 0;
    }

    public TreeNode buildTree(int[] frequencies) {
        if (frequencies == null || frequencies.length != ALPH_SIZE + 1) {
            throw new IllegalArgumentException("frequencies must have length ALPH_SIZE + 1");
        }

        PriorityQueue queue = new PriorityQueue();
        size = 0;

        // Create one leaf per symbol with a nonzero freq
        for (int value = 0; value < frequencies.length; value++) {
            if (frequencies[value] > 0) {
                queue.queue(new TreeNode(value, frequencies[value]));
                size++;
            }
        }
        queue.queue(new TreeNode(PSEUDO_EOF, 1));
        size++;

        // Merge two lowest freq nodes until one root remains
        while (queue.size() > 1) {
            TreeNode left = queue.dequeue();
            TreeNode right = queue.dequeue();
            queue.queue(new TreeNode(left, PSEUDO_EOF, right));
        }

        // The final node in the queue is the root of the Huffman tree.
        root = queue.dequeue();
        return root;
    }

    public TreeNode getRoot() {
        return root;
    }

    public int getLeafCount() {
        return size;
    }

    
}
