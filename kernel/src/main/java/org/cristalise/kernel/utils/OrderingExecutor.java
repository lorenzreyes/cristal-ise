/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.kernel.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * This Executor warrants task ordering for tasks with same key (key have to implement hashCode and equal methods correctly).
 */

@Slf4j
public class OrderingExecutor {

    private ExecutorService executor;
    private Map<String, Queue<Runnable>> keyedTasks = new HashMap<>();
    private Semaphore mutex = new Semaphore(1);

    public OrderingExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public <T> Future<T> submit( Callable<T> task ) {
        return executor.submit( task );
    }

    public <T> Future<T> submit(Callable<T> task, String key) {

        if (task == null) throw new NullPointerException();
        if ( key == null ) {
            return executor.submit( task );
        }

        RunnableFuture<T> futureTask = new FutureTask<T>(task);
        try {
            mutex.acquire();

            Queue<Runnable> runnables = keyedTasks.get( key );
            if ( runnables == null ) {
                runnables = new LinkedList<>();
                keyedTasks.put( key, runnables );
                OrderedTask orderedTask = new OrderedTask( futureTask, runnables, key );
                runnables.add( orderedTask );
                executor.execute(orderedTask);
            } else {
                OrderedTask orderedTask = new OrderedTask( futureTask, runnables, key );
                runnables.add( orderedTask );
            }
        } catch ( InterruptedException ex ) {
            log.error( "OrderingExecutor.submit", ex );
        } finally {
            mutex.release();
        }

        return futureTask;

    }

    class OrderedTask implements Runnable{

        private final Queue<Runnable> dependencyQueue;
        private final Runnable task;
        private final Object key;


        public OrderedTask(Runnable task, Queue<Runnable> dependencyQueue, Object key) {
            this.task = task;
            this.dependencyQueue = dependencyQueue;
            this.key = key;
        }

        @Override
        public void run() {
            try{
                task.run();
            } finally {
                Runnable nextTask = null;
                try {
                    mutex.acquire();
                    if (dependencyQueue.isEmpty()){
                        keyedTasks.remove(key);
                    }else{
                        nextTask = dependencyQueue.poll();
                    }
                } catch ( InterruptedException ex ) {
                    log.error( "OrderingExecutor.run", ex );
                } finally {
                    mutex.release();
                }
                if (nextTask!=null) {
                    executor.execute(nextTask);
                }
            }
        }
    }

}