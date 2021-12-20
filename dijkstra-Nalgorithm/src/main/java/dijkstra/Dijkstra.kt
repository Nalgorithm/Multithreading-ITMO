package dijkstra

import java.util.concurrent.Phaser
import kotlin.Comparator
import kotlin.concurrent.thread
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.random.Random

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

class MultiPQ(private val workers: Int) {
    var queueList = Array(workers) { Queue() }

    fun add(v: Node) {
        var index = Random.nextInt(workers)
        while (!queueList[index].add(v)) {
            index = Random.nextInt(workers)
        }
    }

    fun poll(): Node? {
        var index1 = Random.nextInt(workers)
        while (!queueList[index1].lock.tryLock()) {
            index1 = Random.nextInt(workers)
        }

        var index2 = Random.nextInt(workers)
        while (!queueList[index2].lock.tryLock()) {
            index2 = Random.nextInt(workers)
        }

        var v1: Node? = queueList[index1].q.peek()
        var v2: Node? = queueList[index2].q.peek()

        when {
            v1 == null && v2 == null -> {
                queueList[index1].lock.unlock()
                queueList[index2].lock.unlock()
                return null
            }
            v1 == null -> {
                val res = queueList[index1].q.poll()
                queueList[index1].lock.unlock()
                queueList[index2].lock.unlock()
                return res
            }
            v2 == null -> {
                val res = queueList[index2].q.poll()
                queueList[index1].lock.unlock()
                queueList[index2].lock.unlock()
                return res
            }
            else -> {
                val res: Node?
                if (v1.distance < v2.distance) {
                    res = queueList[index1].q.poll()
                } else {
                    res = queueList[index2].q.poll()
                }
                queueList[index1].lock.unlock()
                queueList[index2].lock.unlock()
                return res
            }
        }
    }


    class Queue {
        var q = PriorityQueue(NODE_DISTANCE_COMPARATOR)
        var lock = ReentrantLock()

        fun add(v: Node): Boolean {
            if (!lock.tryLock()) {
                return false
            }
            q.add(v)
            lock.unlock()
            return true
        }
    }
}

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    val q = MultiPQ(workers)
    q.add(start)
    val processed = AtomicInteger(1)
    // Run worker threads and wait until the total work is done
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    repeat(workers) {
        thread {
            while (processed.get() > 0) {

                val cur = q.poll() ?: continue

                for (e in cur.outgoingEdges) {
                    while (true) {
                        val curDist = cur.distance
                        val eDist = e.to.distance
                        if (eDist > curDist + e.weight) {
                            if (e.to.casDistance(eDist, curDist + e.weight)) {
                                q.add(e.to)
                                processed.incrementAndGet()
                                break
                            }
                        } else {
                            break
                        }
                    }
                }
                processed.decrementAndGet()
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}