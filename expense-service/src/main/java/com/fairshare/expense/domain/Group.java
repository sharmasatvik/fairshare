package com.fairshare.expense.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A group is deliberately lightweight here, no separate user-service.
 * Membership is just a set of opaque user IDs; anything richer (profiles,
 * auth) is out of scope for what this project is trying to demonstrate.
 */
@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group {

    @Id
    private UUID id;

    /**
     * The name of the group.
     */
    @Column(nullable = false)
    private String name;

    /**
     * The group members.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "group_members", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "user_id")
    private Set<String> memberUserIds = new HashSet<>();

    /**
     * Represents when the group was created.
     */
    @Column(nullable = false)
    private Instant createdAt;

    /**
     * Creates a new group.
     *
     * @param name          The name of the group.
     * @param memberUserIds The members of the group.
     *
     * @return The group details.
     */
    public static Group create(final String name, final Set<String> memberUserIds) {
        final var group = new Group();

        group.id = UUID.randomUUID();
        group.name = name;
        group.memberUserIds = new HashSet<>(memberUserIds);
        group.createdAt = Instant.now();

        return group;
    }

    /**
     * Checks whether a specified user exists in the group.
     *
     * @param userId The user id.
     *
     * @return Whether the user exists in the group.
     */
    public boolean hasMember(final String userId) {
        return memberUserIds.contains(userId);
    }
}
