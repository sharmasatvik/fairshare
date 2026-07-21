package com.fairshare.expense.service;

import com.fairshare.expense.domain.Group;
import com.fairshare.expense.dto.CreateGroupRequest;
import com.fairshare.expense.exception.GroupNotFoundException;
import com.fairshare.expense.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;

    @Transactional
    public Group createGroup(final CreateGroupRequest request) {
        final var group = Group.create(request.name(), request.memberUserIds());

        return groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public Group getGroup(final UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
    }
}
