package com.recycling.toolsapp.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque
import java.util.TreeMap

/**
 * 带优先级的任务队列（协程友好）。
 * - 优先级数值越大，优先级越高（priority: higher number = higher priority）。
 * - 同一优先级内保持 FIFO（先进先出）。
 * - 当有等待的消费者时，始终分配当前可用的最高优先级任务。
 */
class PriorityTaskQueue<T> {
    private val mutex = Mutex()

    // 每个优先级一个队列；使用 TreeMap(降序) 便于快速拿到最高优先级
    private val priorityToQueue = TreeMap<Int, ArrayDeque<T>>(compareByDescending { it })

    // 等待者队列（不区分优先级，消费者总是想要“最高可用”）
    private val waiters = ArrayDeque<CompletableDeferred<T>>()

    /**
     * 入队任务，带优先级
     */
    suspend fun enqueue(task: T, priority: Int) {
        mutex.withLock {
            // 先把任务放入其优先级队列，保证后续“从所有可用中取最高优先级”的语义
            val q = priorityToQueue.getOrPut(priority) { ArrayDeque() }
            q.addLast(task)

            // 如果有等待者，立刻分配当前可用的最高优先级任务
            if (waiters.isNotEmpty()) {
                val next = pollHighestLocked() ?: return
                val waiter = waiters.removeFirst()
                waiter.complete(next)
            }
        }
    }

    /**
     * 出队一个任务；若空则挂起直到有新任务到达。
     */
    suspend fun dequeue(): T {
        val deferred = CompletableDeferred<T>()
        mutex.withLock {
            val existing = pollHighestLocked()
            if (existing != null) return existing
            waiters.addLast(deferred)
        }
        return deferred.await()
    }

    /**
     * 当前总大小（所有优先级之和）
     */
    suspend fun size(): Int = mutex.withLock { priorityToQueue.values.sumOf { it.size } }

    /**
     * 仅在已持锁的情况下调用，从所有优先级中取出最高优先级的一个任务
     */
    private fun pollHighestLocked(): T? {
        val iter = priorityToQueue.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val q = entry.value
            if (q.isNotEmpty()) {
                val item = q.removeFirst()
                if (q.isEmpty()) iter.remove()
                return item
            } else {
                iter.remove()
            }
        }
        return null
    }
}


