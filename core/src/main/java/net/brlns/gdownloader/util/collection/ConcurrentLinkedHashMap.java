/*
 * Copyright (C) 2024 hstr0100
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.brlns.gdownloader.util.collection;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Gabriel / hstr0100 / vertx010
 */
public class ConcurrentLinkedHashMap<K, V> extends LinkedHashMap<K, V> {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    @Override
    public V put(K key, V value) {
        lock.writeLock().lock();
        try {
            return super.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public V remove(Object key) {
        lock.writeLock().lock();
        try {
            return super.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public V get(Object key) {
        lock.readLock().lock();
        try {
            return super.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        lock.readLock().lock();
        try {
            return super.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsValue(Object value) {
        lock.readLock().lock();
        try {
            return super.containsValue(value);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return super.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return super.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> otherMap) {
        lock.writeLock().lock();
        try {
            super.putAll(otherMap);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            super.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Set<K> keySet() {
        lock.readLock().lock();
        try {
            return super.keySet();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Collection<V> values() {
        lock.readLock().lock();
        try {
            return super.values();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        lock.readLock().lock();
        try {
            return super.entrySet();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean equals(Object o) {
        lock.readLock().lock();
        try {
            return super.equals(o);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int hashCode() {
        lock.readLock().lock();
        try {
            return super.hashCode();
        } finally {
            lock.readLock().unlock();
        }
    }
}
