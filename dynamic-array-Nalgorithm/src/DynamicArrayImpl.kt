import kotlinx.atomicfu.*

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val core = atomic(Core<E>(INITIAL_CAPACITY, 0))
    private val moving = atomic(false)

    override fun get(index: Int): E {
        if (core.value.headIdx.value <= index) {
            throw IllegalArgumentException()
        }

        return core.value.array[index].value!!.elem
    }

    override fun put(index: Int, element: E) {
        while (true) {
            val curArray = core.value
            if (curArray.headIdx.value <= index) {
                throw IllegalArgumentException()
            }
            val last = curArray.array[index].value
            if (!last!!.isDeleted &&
                curArray.array[index].compareAndSet(last, Node(element, false))
            ) {
                return
            }
        }
    }

    override fun pushBack(element: E) {
        while (true) {
            val curArray = core.value
            val curHead = curArray.headIdx.value
            if (curHead >= curArray.capacity &&
                moving.compareAndSet(false, true)
            ) {
                move(curArray, curHead)
            } else if (curHead < curArray.capacity &&
                curArray.array[curHead].compareAndSet(null, Node(element, false))
            ) {
                curArray.headIdx.value = curArray.headIdx.value + 1
                break
            }
        }
    }

    private fun move(curArray: Core<E>, headIdx: Int) {
        val newArray = Core<E>(curArray.capacity * 2, headIdx)
        var idx = 0
        while (idx < headIdx) {
            val elem = curArray.array[idx].value
            if (curArray.array[idx].compareAndSet(elem, Node(elem!!.elem, true))) {
                newArray.array[idx].value = elem
                idx++
            }
        }
        core.compareAndSet(curArray, newArray)
        moving.compareAndSet(true, false)
    }

    override val size: Int
        get() {
            return (core.value.headIdx.value)
        }
}

class Node<E>(
    val elem: E,
    val isDeleted: Boolean
)

private class Core<E>(
    capacity: Int,
    headIdx: Int
) {
    val array = atomicArrayOfNulls<Node<E>>(capacity)
    val headIdx = atomic(headIdx)
    val capacity = capacity
}

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME