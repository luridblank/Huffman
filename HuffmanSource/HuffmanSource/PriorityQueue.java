import java.util.ArrayList;

import java.util.ArrayList;

public class PriorityQueue {

    private final static int MINIMUM_ELEMENT_SIZE = -1;
    private final static int MAXIMUM_ELEMENT_SIZE = 256;

    private ArrayList<TreeNode> queue;

    public PriorityQueue(){
        queue = new ArrayList<>();
    }

    public TreeNode queue(int element) {
        if (element < MINIMUM_ELEMENT_SIZE || element > MAXIMUM_ELEMENT_SIZE) {
            throw new IllegalArgumentException("Element must be inbounds of 0-255.");
        }
        

        
    }

}