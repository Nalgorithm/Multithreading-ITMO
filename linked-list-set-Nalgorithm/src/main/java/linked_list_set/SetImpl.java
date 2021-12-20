package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {
    private class Node {
        AtomicRef<Node> next;
        int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }

        boolean isDeleted() {
            return false;
        }

        AtomicRef<Node> getNext() {
            return next;
        }

        Node getNode(){
            return null;
        }
    }

    class Removed extends Node {
        Node node;

        Removed(Node n) {
            super(n.x, null);
            this.node = n;
        }
        boolean isDeleted() {
            return true;
        }
        AtomicRef<Node> getNext() {
            return next;
        }

        Node getNode(){
            return this.node;
        }
    }

    private class Window {
        Node cur, next;
    }

    private final Node head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null));

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        retry: while(true) {
            Window w = new Window();
            w.cur = head;
            w.next = w.cur.getNext().getValue();
            while (w.next.x < x) {
                Node node = w.next.getNext().getValue();
                if (node.isDeleted()) {
                    node = node.getNode();
                    if (!w.cur.getNext().compareAndSet(w.next, node)) {
                        continue retry;
                    }
                } else {
                    w.cur = w.next;
                }
                w.next = node;
            }
            if (w.next.isDeleted()) {
                continue;
            }
            return w;
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x == x && !w.next.getNext().getValue().isDeleted()) {
                return false;
            }

            Node node = new Node(x, w.next);
            if (!w.next.isDeleted() &&
                    w.cur.getNext().compareAndSet(w.next, node)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x != x) {
                return false;
            }
            Node node = w.next.getNext().getValue();
            if (node.isDeleted()) return false;
            Node deleted = new Removed(node);
            if (w.next.getNext().compareAndSet(node, deleted)) {
                w.cur.getNext().compareAndSet(w.next, node);
                return true;
            }
        }
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        return w.next.x != Integer.MAX_VALUE && !w.next.getNext().getValue().isDeleted()  &&
                (w.next.x == x);
    }
}