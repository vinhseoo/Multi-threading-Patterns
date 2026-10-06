package vn.ptit.network.pool;

/**
 * [INNOVATION - TECHNICAL DEPTH 30%]:
 * Hàng đợi chặn có giới hạn (Bounded Blocking Queue) tự lập trình 100% từ con số 0.
 * Hoàn toàn KHÔNG sử dụng bất kỳ thư viện có sẵn nào từ java.util.concurrent.*.
 * 
 * Nguyên lý hoạt động:
 * 1. Cấu trúc dữ liệu: Mảng vòng (Circular Array Buffer) với 2 con trỏ putIndex và takeIndex,
 *    đảm bảo độ phức tạp thời gian O(1) cho cả hai thao tác thêm (enqueue) và rút (dequeue).
 * 2. Cơ chế đồng bộ (Synchronization Primitive): Monitor Pattern với từ khóa `synchronized`,
 *    kết hợp `wait()` và `notifyAll()` ở mức máy ảo Java (JVM Intrinsic Monitor).
 * 3. Hỗ trợ bài toán đa luồng Producer - Consumer:
 *    - Producer (Acceptor Thread) gọi offer(task): Nếu hàng đợi đầy, trả về ngay false
 *      để máy chủ kích hoạt Rejection Policy (503 Service Unavailable), tránh làm nghẽn Accept Loop.
 *    - Consumer (CustomWorker Threads) gọi take(): Nếu hàng đợi rỗng, luồng tự động rơi vào
 *      trạng thái WAITING cho đến khi Producer đẩy task mới vào và gọi notifyAll().
 */
public class CustomBlockingQueue<E> {

    private final Object[] items;
    private int putIndex = 0;
    private int takeIndex = 0;
    private int count = 0;

    /**
     * Khởi tạo hàng đợi với sức chứa giới hạn (Bounded Capacity) để chống tràn bộ nhớ (OOM).
     * @param capacity sức chứa tối đa của hàng đợi
     */
    public CustomBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Dung lượng hàng đợi phải > 0: " + capacity);
        }
        this.items = new Object[capacity];
    }

    /**
     * Đẩy phần tử vào hàng đợi mà không chặn (Non-blocking enqueue).
     * @param item tác vụ cần thêm
     * @return true nếu thêm thành công, false nếu hàng đợi đã đầy
     */
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

    /**
     * Đẩy phần tử vào hàng đợi, nếu đầy thì chờ (Blocking enqueue).
     * @param item tác vụ cần thêm
     * @throws InterruptedException nếu luồng bị ngắt trong lúc chờ
     */
    public synchronized void put(E item) throws InterruptedException {
        if (item == null) {
            throw new NullPointerException("Phần tử thêm vào hàng đợi không được null");
        }
        while (count == items.length) {
            wait();
        }
        enqueue(item);
    }

    /**
     * Lấy và xóa phần tử ở đầu hàng đợi. Nếu rỗng thì chờ (Blocking dequeue).
     * @return phần tử đầu hàng đợi
     * @throws InterruptedException nếu luồng bị ngắt trong lúc chờ
     */
    public synchronized E take() throws InterruptedException {
        while (count == 0) {
            wait();
        }
        return dequeue();
    }

    /**
     * Thêm phần tử vào vị trí putIndex và xoay vòng con trỏ.
     */
    private void enqueue(E item) {
        items[putIndex] = item;
        if (++putIndex == items.length) {
            putIndex = 0; // Xoay vòng về đầu mảng
        }
        count++;
        // Đánh thức các Worker Thread đang ngủ ở lệnh take()
        notifyAll();
    }

    /**
     * Lấy phần tử tại vị trí takeIndex và xoay vòng con trỏ.
     */
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

    /**
     * Số lượng phần tử hiện có trong hàng đợi.
     */
    public synchronized int size() {
        return count;
    }

    /**
     * Sức chứa tối đa của hàng đợi.
     */
    public synchronized int capacity() {
        return items.length;
    }

    /**
     * Kiểm tra hàng đợi có rỗng không.
     */
    public synchronized boolean isEmpty() {
        return count == 0;
    }

    /**
     * Xóa sạch toàn bộ phần tử trong hàng đợi (sử dụng khi dừng Thread Pool).
     */
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
