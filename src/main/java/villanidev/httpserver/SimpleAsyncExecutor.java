package villanidev.httpserver;

import java.util.concurrent.*;
import java.util.function.Consumer;

public class SimpleAsyncExecutor {
    private final ExecutorService executor;

    /**
     * Create with default virtual thread executor
     */
    public SimpleAsyncExecutor() {
        this.executor = Executors.newFixedThreadPool(
                20,
                Thread.ofVirtual()
                        .name("asyncVthread-", 0)
                        .factory()
        );
    }

    /**
     * Create with custom executor
     * @param executor
     */
    public SimpleAsyncExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Execute a void task (fire-and-forget)
     * @param task
     */
    public void execute(Runnable task) {
        executor.execute(task);
    }

    /**
     * Submit a task that returns a value (returns Future)
     * @param task
     * @return
     * @param <T>
     */
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    /**
     * Submit a task with callback for handling result/exception
     * @param task
     * @param onSuccess
     * @param onError
     * @param <T>
     */
    public <T> void submit(Callable<T> task,
                           Consumer<T> onSuccess,
                           Consumer<Exception> onError) {
        executor.execute(() -> {
            try {
                T result = task.call();
                if (onSuccess != null) {
                    onSuccess.accept(result);
                }
            } catch (Exception e) {
                if (onError != null) {
                    onError.accept(e);
                }
            }
        });
    }

    /**
     * Shutdown the executor
     * @throws InterruptedException
     */
    public void shutdown() {
        executor.shutdown();
        /*if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }*/
    }
}
