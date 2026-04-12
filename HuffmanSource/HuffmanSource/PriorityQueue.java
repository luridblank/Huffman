import java.util.ArrayList;

public class PriorityQueue {

    private final ArrayList<TreeNode> queue;

    public PriorityQueue(){
        queue = new ArrayList<>();
    }

    public TreeNode queue(TreeNode element) {
        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null.");
        }
        for (int i = 0; i < queue.size(); i++) {
            if (element.compareTo(queue.get(i)) < 0) {
                queue.add(i, element);
                return element;
            }
        }
        queue.add(element);
        return element;
    }

    public TreeNode dequeue() {
        if (queue.isEmpty()) {
            return null;
        }
        return queue.remove(0);
    }

    public TreeNode dequeue(TreeNode element) {
        return dequeue();
    }

    public int getSize() {
        return queue.size();
    }

}