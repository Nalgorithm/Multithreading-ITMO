import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlinx.atomicfu.AtomicArray
import kotlinx.atomicfu.atomicArrayOfNulls

class FCPriorityQueue<E : Comparable<E>> {
    private val lock = ReentrantLock()
    private val q = PriorityQueue<E>()
    private val size = 10
    private val fc_array: AtomicArray<Operation<E>?> = atomicArrayOfNulls(size)


    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        val operation = Operation<E>(Type.Poll)
        return waitOperation(operation)
    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        return q.peek()
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        val operation = Operation(Type.Add, element)
        waitOperation(operation)
    }

    private fun waitOperation(operation: Operation<E>): E? {
        val rand = Random()
        while (true) {
            val index = rand.nextInt(size)
            if (fc_array[index].compareAndSet(null, operation)) {
                while (true) {
                    if (lock.tryLock()) {
                        for (i in 0 until size) {
                            val op = fc_array[i].value ?: continue
                            if (op.type != Type.Finished) {
                                when (op.type) {
                                    Type.Add -> {
                                        q.add(op.value)
                                    }
                                    Type.Poll -> {
                                        op.value = q.poll()
                                    }
                                }
                                op.type = Type.Finished
                            }
                        }
                        lock.unlock()
                    }

                    if (operation.type == Type.Finished) {
                        val res = operation.value
                        fc_array[index].value = null
                        return res
                    }
                }
            }
        }
    }

    class Operation<E>(var type: Type, var value: E? = null)

    enum class Type {
        Add,
        Poll,
        Finished
    }
}