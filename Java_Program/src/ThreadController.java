import java.util.concurrent.locks.*;

class NumberPrinter {
    // Print Zero
    public void printZero() {
        System.out.print("0");
    }
    // Print Odd
    public void printOdd(int num) {
        System.out.print(num);
    }
    // Print Odd
    public void printEven(int num) {
        System.out.print(num);
    }
}

public class ThreadController {
    private final int n;                   // Maximum number to print
    private final NumberPrinter printer;   // Object to print numbers
    private int current = 1;               // Next number to print
    // Enum to represent which thread's turn it is
    private enum State { ZERO, ODD, EVEN }
    private State state = State.ZERO;      // Start with ZeroThread
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    // Constructor to initialize the controller with n and a NumberPrinter
    public ThreadController(int n, NumberPrinter printer) {
        this.n = n;
        this.printer = printer;
    }

    // Start the three threads for printing zero, odd, and even numbers
    public void startThreads() {
        new Thread(this::zeroThread, "ZeroThread").start();
        new Thread(this::oddThread, "OddThread").start();
        new Thread(this::evenThread, "EvenThread").start();
    }

    // ZeroThread: Prints "0" and then signals either OddThread or EvenThread based on the next number
    private void zeroThread() {
        lock.lock();
        try {
            while (current <= n) {
                // Wait until it's zero's turn
                while (state != State.ZERO) {
                    condition.await();
                }
                printer.printZero();
                if (current > n) break;
                // Set next state: ODD if current is odd, EVEN if even
                state = (current % 2 == 1) ? State.ODD : State.EVEN;
                condition.signalAll();
            }
            // Wake up waiting threads to allow termination
            state = State.ODD;
            condition.signalAll();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    // OddThread: Waits for its turn, prints the odd number, increments current, then signals ZeroThread
    private void oddThread() {
        lock.lock();
        try {
            while (current <= n) {
                // Wait until it's odd's turn and the current number is odd
                while (state != State.ODD || current % 2 == 0) {
                    condition.await();
                }
                printer.printOdd(current);
                current++;
                state = State.ZERO;
                condition.signalAll();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    // EvenThread: Waits for its turn, prints the even number, increments current, then signals ZeroThread
    private void evenThread() {
        lock.lock();
        try {
            while (current <= n) {
                // Wait until it's even's turn and the current number is even
                while (state != State.EVEN || current % 2 == 1) {
                    condition.await();
                }
                printer.printEven(current);
                current++;
                state = State.ZERO;
                condition.signalAll();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        NumberPrinter printer = new NumberPrinter();

        // Example-1
        int n1 = 5;
        ThreadController controller1 = new ThreadController(n1, printer);
        System.out.print("The number is ");
        controller1.startThreads(); // Expected output: 01020304050

        /*
        // Example-2
        int n2 = 10;
        ThreadController controller2 = new ThreadController(n2, printer);
        System.out.print("The number is ");
        controller2.startThreads(); // Expected output: 010203040506070809010011
         */
    }
}
