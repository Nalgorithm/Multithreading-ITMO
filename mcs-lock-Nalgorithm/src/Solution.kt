import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class Solution (val env: Environment) : Lock<Solution.Node> {
    // todo: необходимые поля (val, используем AtomicReference)
    val tail = AtomicReference<Node>(null)

    override fun lock(): Node {
        val my = Node() // сделали узел
        my.isLocked.set(true)
        val prev = tail.getAndSet(my)
        if (prev != null) {
            prev.next.value = my
            while (my.isLocked.get()) {
                env.park()
            }
        }
        return my // вернули узел
    }

    override fun unlock(node: Node) {
        if (node.next.get() == null) {
            if (tail.compareAndSet(node, null)) {
                return
            }
                while (node.next.get() == null) { }
        }
        node.next.value.isLocked.set(false)
        env.unpark(node.next.value.thread)
    }

    class Node {
        val thread = Thread.currentThread() // запоминаем поток, которые создал узел
        val isLocked = AtomicReference(false)
        val next = AtomicReference<Node>(null)
    }
}