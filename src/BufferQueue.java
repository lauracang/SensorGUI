import java.util.LinkedList;


public class BufferQueue {
	LinkedList<String> queue;
	Integer size;
	
	public BufferQueue(int size) {
		this.size = size;
		queue = new LinkedList<String>();
	}
	public void push(String s) {
		queue.push(s);
		if (queue.size() > size) {
			queue.removeLast();
		}
	}
}
