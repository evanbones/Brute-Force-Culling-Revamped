package com.evandev.brute_force_culling.culling.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.BiConsumer;

public class IndexedSet<E> {
    private final ArrayList<E> list;
    private final HashSet<E> set;

    public IndexedSet() {
        this.list = new ArrayList<>();
        this.set = new HashSet<>();
    }

    public IndexedSet(int initialCapacity) {
        this.list = new ArrayList<>(initialCapacity);
        this.set = new HashSet<>(initialCapacity);
    }

    public boolean add(E element) {
        if (set.add(element)) {
            list.add(element);
            return true;
        }
        return false;
    }

    public boolean remove(E element) {
        if (set.remove(element)) {
            list.remove(element);
            return true;
        }
        return false;
    }

    public void forEach(BiConsumer<? super E, Integer> action) {
        int size = list.size();
        for (int i = 0; i < size; ++i) {
            action.accept(list.get(i), i);
        }
    }

    public E get(int index) {
        return list.get(index);
    }

    public int size() {
        return list.size();
    }

    public boolean contains(E element) {
        return set.contains(element);
    }

    public void clear() {
        list.clear();
        set.clear();
    }

    @Override
    public String toString() {
        return list.toString();
    }
}
