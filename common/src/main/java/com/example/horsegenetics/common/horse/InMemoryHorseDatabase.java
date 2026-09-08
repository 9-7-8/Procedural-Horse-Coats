package com.example.horsegenetics.common.horse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Straightforward {@link HashMap}-backed {@link HorseDatabase}. Not
 * thread-safe; callers coordinate (on the server thread, in the mod's case).
 */
public final class InMemoryHorseDatabase implements HorseDatabase {

    private final Map<UUID, HorseRecord> byId = new HashMap<>();

    public InMemoryHorseDatabase() {
    }

    public InMemoryHorseDatabase(Collection<HorseRecord> initial) {
        for (HorseRecord record : initial) {
            byId.put(record.id(), record);
        }
    }

    @Override
    public void record(HorseRecord horse) {
        byId.put(horse.id(), horse);
    }

    @Override
    public Optional<HorseRecord> lookup(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public boolean forget(UUID id) {
        return byId.remove(id) != null;
    }

    @Override
    public int offspringCount(UUID parentA, UUID parentB) {
        int count = 0;
        for (HorseRecord record : byId.values()) {
            Optional<UUID> mother = record.motherId();
            Optional<UUID> father = record.fatherId();
            if (mother.isEmpty() || father.isEmpty()) {
                continue;
            }
            boolean match = (mother.get().equals(parentA) && father.get().equals(parentB))
                    || (mother.get().equals(parentB) && father.get().equals(parentA));
            if (match) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<HorseRecord> ancestorsOf(UUID id, int depth) {
        if (depth <= 0) {
            return List.of();
        }
        List<HorseRecord> ancestors = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        visited.add(id);
        Deque<UUID> frontier = new ArrayDeque<>();
        frontier.add(id);

        for (int generation = 0; generation < depth && !frontier.isEmpty(); generation++) {
            for (int remaining = frontier.size(); remaining > 0; remaining--) {
                HorseRecord current = byId.get(frontier.poll());
                if (current == null) {
                    continue;
                }
                addParent(current.motherId(), ancestors, visited, frontier);
                addParent(current.fatherId(), ancestors, visited, frontier);
            }
        }
        return ancestors;
    }

    private void addParent(Optional<UUID> parentId, List<HorseRecord> ancestors,
                           Set<UUID> visited, Deque<UUID> frontier) {
        if (parentId.isEmpty() || !visited.add(parentId.get())) {
            return;
        }
        HorseRecord parent = byId.get(parentId.get());
        if (parent != null) {
            ancestors.add(parent);
        }
        frontier.add(parentId.get());
    }

    /**
     * Descendants by generation. There is no parent-to-child index - the store
     * is a map by id - so each generation is one pass over the whole table,
     * testing whether either parent is in the frontier. That is
     * {@code O(depth * n)} and it is the right trade at this size: an index
     * would have to be kept correct through every record, rename, transfer and
     * forget, for a query that runs when somebody presses a button.
     */
    @Override
    public List<List<HorseRecord>> descendantsOf(UUID id, int depth) {
        if (depth <= 0) {
            return List.of();
        }
        List<List<HorseRecord>> generations = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        seen.add(id);
        Set<UUID> frontier = new HashSet<>();
        frontier.add(id);

        for (int generation = 0; generation < depth && !frontier.isEmpty(); generation++) {
            List<HorseRecord> children = new ArrayList<>();
            for (HorseRecord record : byId.values()) {
                if (seen.contains(record.id()) || !hasParentIn(record, frontier)) {
                    continue;
                }
                children.add(record);
            }
            if (children.isEmpty()) {
                break;
            }
            Set<UUID> next = new HashSet<>();
            for (HorseRecord child : children) {
                seen.add(child.id());
                next.add(child.id());
            }
            generations.add(List.copyOf(children));
            frontier = next;
        }
        return List.copyOf(generations);
    }

    private static boolean hasParentIn(HorseRecord record, Set<UUID> parents) {
        return record.motherId().filter(parents::contains).isPresent()
                || record.fatherId().filter(parents::contains).isPresent();
    }

    /** Snapshot of every stored record, for serialization. */
    public Collection<HorseRecord> all() {
        return List.copyOf(byId.values());
    }

    public int size() {
        return byId.size();
    }
}
