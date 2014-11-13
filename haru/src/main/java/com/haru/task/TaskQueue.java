
package com.haru.task;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Simple Task queue for queing multiple tasks.
 */
public class TaskQueue {

    private Queue<Task> taskQueue;

    public TaskQueue() {
        taskQueue = new LinkedList<Task>();
    }

    public void enqueue(Task task) {
        if (!taskQueue.isEmpty()) {
            taskQueue.peek().continueWithTask(new Continuation<Object, Task>() {
                @Override
                public Task then(Task task) throws Exception {
                    return taskQueue.peek();
                }
            });
        }

        taskQueue.offer(task.continueWith(new Continuation() {
            @Override
            public Object then(Task task) throws Exception {
                if (task.isFaulted()) throw task.getError();
                taskQueue.poll();
                return task.getResult();
            }
        }));
    }
}
