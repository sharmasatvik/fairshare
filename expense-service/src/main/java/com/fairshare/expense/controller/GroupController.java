package com.fairshare.expense.controller;

import com.fairshare.expense.domain.Group;
import com.fairshare.expense.dto.CreateGroupRequest;
import com.fairshare.expense.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Manages groups for tracking expenses.
 */
@RestController
@RequestMapping("/groups/")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    /**
     * Creates a new group for tracking expenses.
     *
     * @param request The details of the group.
     *
     * @return The group details.
     */
    @PostMapping
    public ResponseEntity<Group> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        final var group = groupService.createGroup(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    /**
     * Gets the details of a specified group.
     *
     * @param groupId The unique id of the group.
     *
     * @return The details of the group.
     */
    @GetMapping("{groupId}")
    public Group getGroup(@PathVariable UUID groupId) {
        return groupService.getGroup(groupId);
    }
}
