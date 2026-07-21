package com.fairshare.expense.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/**
 * Request to create a new group.
 *
 * @param name          The name of the group.
 * @param memberUserIds The group members.
 */
public record CreateGroupRequest(
        @NotBlank String name,
        @NotEmpty Set<String> memberUserIds
) {
}
