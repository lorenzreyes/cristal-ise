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

import java.util.*;
import java.util.concurrent.*;

/**
 * This Executor warrants task ordering for tasks with same key (key have to implement hashCode and equal methods correctly).
 */

@Slf4j
public class OrderingExecutor {

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final ConcurrentHashMap<String, Queue<Runnable>> keyedTasks = new ConcurrentHashMap<>();
    private static final Semaphore mutex = new Semaphore(1);
    private static final Vector<Long> activeThreads = new Vector<>();

    public OrderingExecutor() {
    }

    private Boolean isSubTask () {
        try {
            mutex.acquire();
            return (activeThreads.contains( Thread.currentThread().getId() ))
                        || (System.getProperty("server.name") == "module");
        } catch ( InterruptedException ex ) {
            log.error( "OrderingExecutor.isSubTask", ex );
        } finally {
            mutex.release();
        }
        return false;
    }

    public void run ( Runnable runnable, String threadName ) {
        OrderedTask orderedTask = new OrderedTask( runnable, threadName );
        executor.execute(orderedTask);
    }

    public <T> T execute ( Callable<T> task, String key ) throws Exception {
        if ( isSubTask() ) {
            return task.call();
        }
        return submit( task, key ).get();
    }

    public <T> T execute ( Callable<T> task ) throws Exception {
        return executor.submit( task ).get();
    }

    private <T> Future<T> submit(Callable<T> task, String key) {
        if ( task == null ) {
            throw new NullPointerException();
        } else if ( key == null ) {
            return executor.submit( task );
        }

        RunnableFuture<T> futureTask = new FutureTask<>(task);
        try {
            mutex.acquire();
            Queue<Runnable> runnables = keyedTasks.get( key );
            boolean uniqueItem = runnables == null;
            if ( uniqueItem ) {
                runnables = new LinkedList<>();
                keyedTasks.put( key, runnables );
            }

            OrderedTask orderedTask = new OrderedTask( futureTask, runnables, key );
            if ( uniqueItem ) {
                executor.execute( orderedTask );
            } else {
                runnables.add( orderedTask );
            }
        } catch ( InterruptedException ex ) {
            log.error( "OrderingExecutor.submit", ex );
        } finally {
            mutex.release();
        }

        return futureTask;
    }

    class OrderedTask implements Runnable {

        private Queue<Runnable> dependencyQueue;
        private Runnable task;
        private Object key;

        private String threadName;

        public OrderedTask(Runnable task, String threadName) {
            this.task = task;
            this.threadName = threadName;
        }

        public OrderedTask(Runnable task, Queue<Runnable> dependencyQueue, Object key) {
            this.task = task;
            this.dependencyQueue = dependencyQueue;
            this.key = key;
        }

        @Override
        public void run() {
            Long currentThread = Thread.currentThread().getId();
            try{
                try {
                    mutex.acquire();
                    activeThreads.add( currentThread );
                } catch ( InterruptedException ex ) {
                    log.error( "OrderingExecutor.run", ex );
                } finally {
                    mutex.release();
                }

                if ( threadName != null ) {
                    Thread.currentThread().setName( threadName );
                }
                task.run();
            } finally {
                Runnable nextTask = null;
                try {
                    mutex.acquire();
                    activeThreads.remove( currentThread );

                    if ( dependencyQueue != null ) {
                        if (dependencyQueue.isEmpty()){
                            keyedTasks.remove(key);
                        }else{
                            nextTask = dependencyQueue.poll();
                        }
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