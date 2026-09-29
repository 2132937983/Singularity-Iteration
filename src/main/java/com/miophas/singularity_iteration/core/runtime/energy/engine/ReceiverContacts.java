// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.energy.engine;

import com.miophas.singularity_iteration.core.runtime.energy.engine.ConductorRegistry.Position;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Per-settlement receiver contacts, preserving receiver order and then face order. */
public final class ReceiverContacts {
    public record Endpoint(Position position, int inputs) {
        public Endpoint {
            Objects.requireNonNull(position, "position");
            if ((inputs & ~ConductorRegistry.ALL_FACES) != 0) throw new IllegalArgumentException("Invalid input faces");
        }
    }
    public record Contact(int receiver, Position position, int vertex) { }
    private final ConductorRegistry.Snapshot snapshot;
    private final Map<Position, List<Integer>> direct;
    private final Map<Integer, List<Contact>> wired;

    public ReceiverContacts(ConductorRegistry.Snapshot snapshot, List<Endpoint> receivers) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        snapshot.revision();
        var directLists = new HashMap<Position, ArrayList<Integer>>();
        var wiredLists = new HashMap<Integer, ArrayList<Contact>>();
        for (int receiver = 0; receiver < receivers.size(); receiver++) {
            var endpoint = Objects.requireNonNull(receivers.get(receiver), "receiver");
            directLists.computeIfAbsent(endpoint.position, ignored -> new ArrayList<>()).add(receiver);
            for (int face = 0; face < 6; face++) {
                if ((endpoint.inputs & (1 << face)) == 0) continue;
                Position at = adjacent(endpoint.position, face);
                if (at == null || !snapshot.contains(at) || !snapshot.permits(at, face ^ 1)) continue;
                wiredLists.computeIfAbsent(snapshot.componentOf(at), ignored -> new ArrayList<>())
                    .add(new Contact(receiver, at, snapshot.vertex(at)));
            }
        }
        var directIndex = new HashMap<Position, List<Integer>>();
        directLists.forEach((position, members) -> directIndex.put(position, List.copyOf(members)));
        direct = Map.copyOf(directIndex);
        var wireIndex = new HashMap<Integer, List<Contact>>();
        wiredLists.forEach((component, contacts) -> wireIndex.put(component, List.copyOf(contacts)));
        wired = Map.copyOf(wireIndex);
    }

    public List<Integer> at(Position position) {
        snapshot.revision();
        return direct.getOrDefault(position, List.of());
    }

    public List<Contact> inComponent(int component) {
        snapshot.revision();
        return wired.getOrDefault(component, List.of());
    }

    /** Merge direct and wired receiver IDs without changing registration order. */
    public Candidates candidates(Position position, int component) {
        return new Candidates(snapshot, at(position), inComponent(component));
    }

    public static final class Candidates {
        private final ConductorRegistry.Snapshot snapshot;
        private final List<Integer> direct;
        private final List<Contact> wired;
        private int directIndex, wireIndex, firstContact, contactEnd, receiver;
        private boolean directContact;

        private Candidates(ConductorRegistry.Snapshot snapshot, List<Integer> direct, List<Contact> wired) {
            this.snapshot = snapshot; this.direct = direct; this.wired = wired;
        }

        public boolean hasWiredContacts() { return !wired.isEmpty(); }

        public boolean next() {
            snapshot.revision();
            if (directIndex == direct.size() && wireIndex == wired.size()) return false;
            int directReceiver = directIndex < direct.size() ? direct.get(directIndex) : Integer.MAX_VALUE;
            int wireReceiver = wireIndex < wired.size() ? wired.get(wireIndex).receiver : Integer.MAX_VALUE;
            receiver = Math.min(directReceiver, wireReceiver);
            directContact = directReceiver == receiver;
            if (directContact) directIndex++;
            firstContact = wireIndex;
            while (wireIndex < wired.size() && wired.get(wireIndex).receiver == receiver) wireIndex++;
            contactEnd = wireIndex;
            return true;
        }

        public int receiver() { return receiver; }
        public boolean direct() { return directContact; }
        public int contactCount() { return contactEnd - firstContact; }
        public Contact contact(int index) {
            Objects.checkIndex(index, contactCount());
            return wired.get(firstContact + index);
        }
    }

    private static Position adjacent(Position at, int face) {
        long x = at.x(), y = at.y(), z = at.z();
        switch (face) {
            case 0 -> y--;
            case 1 -> y++;
            case 2 -> z--;
            case 3 -> z++;
            case 4 -> x--;
            case 5 -> x++;
            default -> throw new IllegalArgumentException("Invalid face");
        }
        return x < Integer.MIN_VALUE || x > Integer.MAX_VALUE || y < Integer.MIN_VALUE || y > Integer.MAX_VALUE
            || z < Integer.MIN_VALUE || z > Integer.MAX_VALUE ? null : new Position((int) x, (int) y, (int) z);
    }
}
