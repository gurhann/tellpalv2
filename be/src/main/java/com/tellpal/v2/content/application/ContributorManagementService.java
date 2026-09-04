package com.tellpal.v2.content.application;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentContributorNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContributorInUseException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContributorNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.DuplicateContributorDisplayNameException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContributorRoleInUseException;
import com.tellpal.v2.content.application.ContributorManagementCommands.AssignContentContributorCommand;
import com.tellpal.v2.content.application.ContributorManagementCommands.CreateContributorCommand;
import com.tellpal.v2.content.application.ContributorManagementCommands.DeleteContributorCommand;
import com.tellpal.v2.content.application.ContributorManagementCommands.RenameContributorCommand;
import com.tellpal.v2.content.application.ContributorManagementCommands.ReorderContentContributorsCommand;
import com.tellpal.v2.content.application.ContributorManagementCommands.UnassignContentContributorCommand;
import com.tellpal.v2.content.application.ContributorManagementResults.ContentContributorRecord;
import com.tellpal.v2.content.application.ContributorManagementResults.ContributorRecord;
import com.tellpal.v2.content.application.ContributorRegistryReadResults.Item;
import com.tellpal.v2.content.application.ContributorRegistryReadResults.Page;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentContributor;
import com.tellpal.v2.content.domain.ContentRepository;
import com.tellpal.v2.content.domain.Contributor;
import com.tellpal.v2.content.domain.ContributorRole;
import com.tellpal.v2.content.domain.ContributorRepository;

/**
 * Application service for managing contributors and their assignments to content.
 */
@Service
public class ContributorManagementService {

    private final ContributorRepository contributorRepository;
    private final ContentRepository contentRepository;
    private final ContributorDuplicateNameResolver duplicateNameResolver;

    public ContributorManagementService(
            ContributorRepository contributorRepository,
            ContentRepository contentRepository,
            ContributorDuplicateNameResolver duplicateNameResolver) {
        this.contributorRepository = contributorRepository;
        this.contentRepository = contentRepository;
        this.duplicateNameResolver = duplicateNameResolver;
    }

    /**
     * Creates a contributor identity that can later be assigned to content.
     */
    @Transactional
    public ContributorRecord createContributor(CreateContributorCommand command) {
        rejectDuplicateName(command.displayName(), null);
        try {
            return ContentManagementMapper.toContributorRecord(contributorRepository.saveAndFlush(
                    Contributor.create(command.displayName(), command.roles())));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateName(command.displayName(), exception);
        }
    }

    /**
     * Lists recent contributors for admin workflows.
     */
    @Transactional(readOnly = true)
    public List<ContributorRecord> listContributors(int limit) {
        return listContributors(limit, null);
    }

    /**
     * Lists recent contributors or searches contributors by display name for admin workflows.
     */
    @Transactional(readOnly = true)
    public List<ContributorRecord> listContributors(int limit, String query) {
        int sanitizedLimit = sanitizeLimit(limit);
        String sanitizedQuery = sanitizeQuery(query);
        List<Contributor> contributors = sanitizedQuery == null
                ? contributorRepository.findRecent(sanitizedLimit)
                : contributorRepository.searchByDisplayName(sanitizedQuery, sanitizedLimit);
        return contributors.stream()
                .map(ContentManagementMapper::toContributorRecord)
                .toList();
    }

    /** Returns a paged contributor registry with usage projections calculated by the database. */
    @Transactional(readOnly = true)
    public Page listContributorRegistry(String query, ContributorRole role, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Registry page must be non-negative and size must be between 1 and 100");
        }
        String normalizedQuery = escapeLikeQuery(query == null ? "" : query.trim());
        com.tellpal.v2.content.domain.ContributorRepository.ContributorRegistryPage result =
                contributorRepository.findRegistryPage(normalizedQuery, role, page, size);
        List<Long> ids = result.contributors().stream().map(Contributor::getId).toList();
        Map<Long, Map<ContributorRole, Long>> usage = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            contentRepository.findContributorUsage(ids).forEach(row -> usage
                    .computeIfAbsent(row.contributorId(), ignored -> new LinkedHashMap<>())
                    .put(row.role(), row.usageCount()));
        }
        List<Item> items = result.contributors().stream().map(contributor -> {
            Map<ContributorRole, Long> byRole = new LinkedHashMap<>();
            contributor.getRoles().forEach(contributorRole -> byRole.put(contributorRole, 0L));
            byRole.putAll(usage.getOrDefault(contributor.getId(), Map.of()));
            return new Item(contributor.getId(), contributor.getDisplayName(), contributor.getRoles(),
                    byRole.values().stream().mapToLong(Long::longValue).sum(), byRole,
                    contributor.getUpdatedAt());
        }).toList();
        return new Page(items, page, size, result.totalItems());
    }

    private static String escapeLikeQuery(String query) {
        return query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * Renames an existing contributor.
     */
    @Transactional
    public ContributorRecord renameContributor(RenameContributorCommand command) {
        Contributor contributor = loadContributor(command.contributorId());
        rejectDuplicateName(command.displayName(), contributor.getId());
        Set<com.tellpal.v2.content.domain.ContributorRole> removedRoles = contributor.getRoles().stream()
                .filter(role -> !command.roles().contains(role))
                .collect(java.util.stream.Collectors.toSet());
        for (com.tellpal.v2.content.domain.ContributorRole role : removedRoles) {
            List<com.tellpal.v2.content.domain.ContentRepository.ContributorRoleUsage> usage =
                    contentRepository.findContributorRoleUsage(command.contributorId(), role);
            if (!usage.isEmpty()) {
                throw new ContributorRoleInUseException(
                        role,
                        contentRepository.countContributorRoleUsage(command.contributorId(), role),
                        usage);
            }
        }
        contributor.updateProfile(command.displayName(), command.roles());
        try {
            return ContentManagementMapper.toContributorRecord(contributorRepository.saveAndFlush(contributor));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateName(command.displayName(), exception);
        }
    }

    /**
     * Deletes one contributor when it is no longer referenced by content assignments.
     */
    @Transactional
    public void deleteContributor(DeleteContributorCommand command) {
        Contributor contributor = loadContributor(command.contributorId());
        if (contentRepository.existsContributorAssignment(command.contributorId())) {
            throw new ContributorInUseException(command.contributorId());
        }
        contributorRepository.delete(contributor);
    }

    /**
     * Assigns a contributor to content for one role and optional language scope.
     */
    @Transactional
    public ContentContributorRecord assignContentContributor(AssignContentContributorCommand command) {
        Content content = loadContent(command.contentId());
        Contributor contributor = loadContributor(command.contributorId());
        ContentContributor assignment = content.assignContributor(
                contributor,
                command.role(),
                command.languageCode(),
                command.creditName());
        Content saved = contentRepository.saveAndFlush(content);
        return ContentManagementMapper.toContentContributorRecord(
                command.contentId(),
                saved.getContributors().stream()
                        .filter(candidate -> candidate.matchesAssignment(
                                command.contributorId(), command.role(), command.languageCode()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Saved contributor assignment was not found")));
    }

    /** Replaces the order of one role/language group while preserving database uniqueness during swaps. */
    @Transactional
    public List<ContentContributorRecord> reorderContentContributors(ReorderContentContributorsCommand command) {
        Content content = loadContentForContributorWrite(command.contentId());
        content.stageContributorReorder(command.role(), command.languageCode(), command.assignmentIds());
        contentRepository.saveAndFlush(content);
        content.completeContributorReorder(command.role(), command.languageCode(), command.assignmentIds());
        Content saved = contentRepository.saveAndFlush(content);
        return saved.getContributors().stream()
                .filter(assignment -> assignment.matchesRoleAndLanguage(command.role(), command.languageCode()))
                .map(assignment -> ContentManagementMapper.toContentContributorRecord(command.contentId(), assignment))
                .sorted(Comparator.comparingInt(ContentContributorRecord::sortOrder)
                        .thenComparing(ContentContributorRecord::assignmentId))
                .toList();
    }

    /**
     * Lists contributor assignments already attached to one content aggregate.
     */
    @Transactional(readOnly = true)
    public List<ContentContributorRecord> listContentContributors(Long contentId) {
        return loadContentForContributorAdminRead(contentId).getContributors().stream()
                .map(assignment -> ContentManagementMapper.toContentContributorRecord(contentId, assignment))
                .sorted(Comparator
                        .comparing(ContentContributorRecord::languageCode, Comparator.nullsFirst(Enum::compareTo))
                        .thenComparing(ContentContributorRecord::role)
                        .thenComparingInt(ContentContributorRecord::sortOrder)
                        .thenComparing(ContentContributorRecord::contributorDisplayName))
                .toList();
    }

    /**
     * Removes one contributor assignment from content by exact role and scope match.
     */
    @Transactional
    public void unassignContentContributor(UnassignContentContributorCommand command) {
        Content content = loadContent(command.contentId());
        try {
            content.unassignContributor(command.contributorId(), command.role(), command.languageCode());
        } catch (IllegalArgumentException exception) {
            throw new ContentContributorNotFoundException(
                    command.contentId(),
                    command.contributorId(),
                    command.role(),
                    command.languageCode());
        }
        List<Long> remainingAssignmentIds = content.getContributors().stream()
                .filter(assignment -> assignment.matchesRoleAndLanguage(command.role(), command.languageCode()))
                .map(ContentContributor::getId)
                .toList();
        content.stageContributorReorder(command.role(), command.languageCode(), remainingAssignmentIds);
        contentRepository.saveAndFlush(content);
        content.completeContributorReorder(command.role(), command.languageCode(), remainingAssignmentIds);
        contentRepository.save(content);
    }

    private Contributor loadContributor(Long contributorId) {
        return contributorRepository.findByIdForWrite(contributorId)
                .orElseThrow(() -> new ContributorNotFoundException(contributorId));
    }

    private void rejectDuplicateName(String displayName, Long ownId) {
        contributorRepository.findByNormalizedDisplayName(normalizeDisplayName(displayName))
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> { throw new DuplicateContributorDisplayNameException(existing.getId()); });
    }

    private DuplicateContributorDisplayNameException duplicateName(
            String displayName, DataIntegrityViolationException cause) {
        return duplicateNameResolver.findExistingContributorId(normalizeDisplayName(displayName))
                .map(DuplicateContributorDisplayNameException::new)
                .orElseThrow(() -> cause);
    }

    private static String normalizeDisplayName(String displayName) {
        return displayName.trim().toLowerCase(Locale.ROOT);
    }

    private Content loadContent(Long contentId) {
        return loadContentForContributorWrite(contentId);
    }

    private Content loadContentForContributorWrite(Long contentId) {
        return contentRepository.findByIdForContributorWrite(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
    }

    private Content loadContentForContributorAdminRead(Long contentId) {
        return contentRepository.findByIdForContributorAdminRead(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
    }

    private static int sanitizeLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Contributor list limit must be positive");
        }
        return Math.min(limit, 100);
    }

    private static String sanitizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String sanitizedQuery = query.trim();
        return sanitizedQuery.isEmpty() ? null : sanitizedQuery;
    }
}
