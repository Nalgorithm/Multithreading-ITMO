/**
 * @author :TODO: Логвиненко Никита
 */
class Solution : AtomicCounter {
    // объявите здесь нужные вам поля

    private val last: ThreadLocal<Node> = ThreadLocal()
    private val head: Node = Node(0)

    override fun getAndAdd(x: Int): Int {
        if (last.get() == null) {
            last.set(head)
        }

        do {
            val newNode = Node(last.get().value + x)
            last.set(last.get().consensus.decide(newNode))
        } while (last.get() != newNode)

        return last.get().value - x
    }

    // вам наверняка потребуется дополнительный класс
    private class Node(val value: Int) {
        val consensus = Consensus<Node>()
    }
}