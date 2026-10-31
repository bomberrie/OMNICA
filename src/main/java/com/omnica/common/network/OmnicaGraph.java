package com.omnica.common.network;

import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * High-performance network topology graph for OMNICA nodes.
 * Caches adjacencies and connectivity matrices to avoid repetitive world queries.
 */
public class OmnicaGraph {
    private final Set<BlockPos> nodes = new HashSet<>();
    private final Map<BlockPos, Set<BlockPos>> adjacencyMap = new HashMap<>();

    public synchronized void addNode(BlockPos pos) {
        nodes.add(pos);
        adjacencyMap.computeIfAbsent(pos, k -> new HashSet<>());
    }

    public synchronized void removeNode(BlockPos pos) {
        nodes.remove(pos);
        Set<BlockPos> neighbors = adjacencyMap.remove(pos);
        if (neighbors != null) {
            for (BlockPos neighbor : neighbors) {
                Set<BlockPos> neighborLinks = adjacencyMap.get(neighbor);
                if (neighborLinks != null) {
                    neighborLinks.remove(pos);
                }
            }
        }
    }

    public synchronized void link(BlockPos a, BlockPos b) {
        if (a.equals(b) || !a.isWithinDistance(b, 32.5)) return;
        addNode(a);
        addNode(b);
        adjacencyMap.get(a).add(b);
        adjacencyMap.get(b).add(a);
    }

    public synchronized void unlink(BlockPos a, BlockPos b) {
        if (adjacencyMap.containsKey(a)) {
            adjacencyMap.get(a).remove(b);
        }
        if (adjacencyMap.containsKey(b)) {
            adjacencyMap.get(b).remove(a);
        }
    }

    public synchronized Set<BlockPos> getNeighbors(BlockPos pos) {
        return adjacencyMap.getOrDefault(pos, Collections.emptySet());
    }

    public synchronized Set<BlockPos> getAllNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    /**
     * Finds the shortest path between source and target using BFS over cached adjacency.
     */
    public synchronized List<BlockPos> findRoute(BlockPos start, BlockPos end) {
        if (!nodes.contains(start) || !nodes.contains(end)) {
            return Collections.emptyList();
        }
        if (start.equals(end)) {
            return List.of(start);
        }

        Queue<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, BlockPos> parentMap = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (current.equals(end)) {
                List<BlockPos> path = new ArrayList<>();
                BlockPos curr = end;
                while (curr != null) {
                    path.add(curr);
                    curr = parentMap.get(curr);
                }
                Collections.reverse(path);
                return path;
            }

            for (BlockPos neighbor : getNeighbors(current)) {
                if (visited.add(neighbor)) {
                    parentMap.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        return Collections.emptyList();
    }
}
