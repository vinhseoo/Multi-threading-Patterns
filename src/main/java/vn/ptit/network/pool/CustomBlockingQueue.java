package vn.ptit.network.pool;


public class CustomBlockingQueue<E> {

    private final Object[] items;
    private int putIndex = 0;
    private int takeIndex = 0;
    private int count = 0;

    public CustomBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Dung lượng hàng đợi phải > 0: " + capacity);
        }
        this.items = new Object[capacity];
    }

    public synchronized boolean offer(E item) {
        if (item == null) {
            throw new NullPointerException("Phần tử thêm vào hàng đợi không được null");
        }
        if (count == items.length) {
            // Hàng đợi đã đầy -> Không chờ vô hạn, trả về false để máy chủ kích hoạt 503 Rejection
            return false;
        }

        enqueue(item);
        return true;
    }

    public synchronized void put(E item) throws InterruptedException {
        if (item == null) {
            throw new NullPointerException("Phần tử thêm vào hàng đợi không được null");
        }
        while (count == items.length) {
            wait();
        }
        enqueue(item);
    }

    public synchronized E take() throws InterruptedException {
        while (count == 0) {
            wait();
        }
        return dequeue();
    }

    private void enqueue(E item) {
        items[putIndex] = item;
        if (++putIndex == items.length) {
            putIndex = 0; // Xoay vòng về đầu mảng
        }
        count++;
        // Đánh thức các Worker Thread đang ngủ ở lệnh take()
        notifyAll();
    }

    @SuppressWarnings("unchecked")
    private E dequeue() {
        E item = (E) items[takeIndex];
        items[takeIndex] = null; // Tránh rò rỉ bộ nhớ (Prevent Memory Leak)
        if (++takeIndex == items.length) {
            takeIndex = 0; // Xoay vòng về đầu mảng
        }
        count--;
        // Đánh thức các Producer nếu có luồng đang chờ ở put()
        notifyAll();
        return item;
    }

    public synchronized int size() {
        return count;
    }

    public synchronized int capacity() {
        return items.length;
    }

    public synchronized boolean isEmpty() {
        return count == 0;
    }

    public synchronized void clear() {
        for (int i = 0; i < items.length; i++) {
            items[i] = null;
        }
        count = 0;
        putIndex = 0;
        takeIndex = 0;
        notifyAll();
    }
}
