package stack;

import kotlinx.atomicfu.AtomicRef;

import java.util.Random;

public class StackImpl implements Stack {
    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    final int size = 10;

    StackImpl() {
        for (int i = 0; i < size; ++i) {
            elimination[i] = new AtomicRef<>(null);
        }
    }


    // head pointer
    private AtomicRef<Node> head = new AtomicRef<>(null);
    private AtomicRef<Node>[] elimination = new AtomicRef[size];


    @Override
    public void push(int x) {
        Random random = new Random();
        int rand = Math.abs(random.nextInt()) % size;
        Node node = new Node(x,null);

        for (int i = 0; i < 3; ++i) {
            int pos = (rand + i) % size;
            if (elimination[pos].compareAndSet(null, node)) {
                spinWait();
                if (elimination[pos].compareAndSet(node, null)) {
                    break;
                } else {
                    return;
                }
            }
        }

        while (true) {
            Node curHead = head.getValue();
            AtomicRef<Node> newHead = new AtomicRef<Node>(new Node(x, curHead));
            if (head.compareAndSet(curHead, newHead.getValue())) {
                return;
            }
        }
    }

    private void spinWait() {
        for (int i = 0; i < 10; ++i) {
        }
    }

    @Override
    public int pop() {
        Random random = new Random();
        int rand = Math.abs(random.nextInt()) % size;

        for (int i = 0; i < 3; ++i) {
            int pos = (rand + i) % size;

            Node curNode = elimination[pos].getValue();
            if (curNode == null) {
                continue;
            }

            if (elimination[pos].compareAndSet(curNode, null)) {
                return curNode.x;
            }
        }

        while (true) {
            Node curHead = head.getValue();
            if (curHead == null) {
                return Integer.MIN_VALUE;
            }
            if (head.compareAndSet(curHead, curHead.next.getValue())) {
                return curHead.x;
            }
        }
    }
}
