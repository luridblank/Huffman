public class PriorityQueue {

    private final static int PRIORITY_QUEUE_SIZE = 257;
    private int[] queue;

    public PriorityQueue(){
        queue = new int[PRIORITY_QUEUE_SIZE];
    }

    public int queue(int element) {
        if (element < 0 || element > 255) {
            throw new IllegalArgumentException("Element must be inbounds of 0-255.");
        }
        
    }

    private class WeightedNodes implements Comparable<WeightedNodes> {

    }
}