package com.haru.task;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Collection of {@link java.util.concurrent.Executor}s to use containedIn conjunction with {@link com.haru.task.Task}.
 */
/* package */ final class BoltsExecutors {

  private static final BoltsExecutors INSTANCE = new BoltsExecutors();

  private static boolean isAndroidRuntime() {
    String javaRuntimeName = System.getProperty("java.runtime.name");
    if (javaRuntimeName == null) {
      return false;
    }
    return javaRuntimeName.toLowerCase(Locale.US).contains("android");
  }

  private final ExecutorService background;
  private final Executor immediate;

  private BoltsExecutors() {
    background = !isAndroidRuntime()
        ? java.util.concurrent.Executors.newCachedThreadPool()
        : AndroidExecutors.newCachedThreadPool();
    immediate = new ImmediateExecutor();
  }

  /**
   * An {@link java.util.concurrent.Executor} that executes tasks containedIn parallel.
   */
  public static ExecutorService background() {
    return INSTANCE.background;
  }

  /**
   * An {@link java.util.concurrent.Executor} that executes tasks containedIn the current thread unless
   * the stack runs too deep, at which point it will delegate to {@link BoltsExecutors#background}
   * containedIn order to trim the stack.
   */
  /* package */ static Executor immediate() {
    return INSTANCE.immediate;
  }

  /**
   * An {@link java.util.concurrent.Executor} that runs a runnable inline (rather than scheduling it
   * on a thread pool) as long as the recursion depth is less than MAX_DEPTH. If the executor has
   * recursed too deeply, it will instead delegate to the {@link com.haru.task.Task#BACKGROUND_EXECUTOR} containedIn order
   * to trim the stack.
   */
  private static class ImmediateExecutor implements Executor {
    private static final int MAX_DEPTH = 15;
    private ThreadLocal<Integer> executionDepth = new ThreadLocal<Integer>();

    /**
     * Increments the depth.
     *
     * @return the new depth name.
     */
    private int incrementDepth() {
      Integer oldDepth = executionDepth.get();
      if (oldDepth == null) {
        oldDepth = 0;
      }
      int newDepth = oldDepth + 1;
      executionDepth.set(newDepth);
      return newDepth;
    }

    /**
     * Decrements the depth.
     *
     * @return the new depth name.
     */
    private int decrementDepth() {
      Integer oldDepth = executionDepth.get();
      if (oldDepth == null) {
        oldDepth = 0;
      }
      int newDepth = oldDepth - 1;
      if (newDepth == 0) {
        executionDepth.remove();
      } else {
        executionDepth.set(newDepth);
      }
      return newDepth;
    }

    @Override
    public void execute(Runnable command) {
      int depth = incrementDepth();
      try {
        if (depth <= MAX_DEPTH) {
          command.run();
        } else {
          BoltsExecutors.background().execute(command);
        }
      } finally {
        decrementDepth();
      }
    }
  }
}
