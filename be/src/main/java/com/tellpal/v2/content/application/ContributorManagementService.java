package com.tellpal.v2.content.application;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentContributorNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContributorInUseException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContributorNotFoundException;
import com.tellpal.v2.content.application.ContributorManagementCommands.AssignContentContributorCommand;
import com.tellpal.v2.content.application.ContributorManagementCommands.CreateContributorCommand;
import com.tellpal.v2.content.application.ContributorManagementCommands.DeleteContributorCommand;
import com.tellpal.v2.content.application.ContributorManagementCommands.RenameContributorCommand;
import com.tellpal.v2.content.application.ContributorManagementCommands.UnassignContentContributorCommand;
import com.tellpal.v2.content.application.ContributorManagementResults.ContentContributorRecord;
import com.tellpal.v2.content.application.ContributorManagementResults.ContributorRecord;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentContributor;
import com.tellpal.v2.content.domain.ContentRepository;
import com.tellpal.v2.content.domain.Contributor;
import com.tellpal.v2.content.domain.ContributorRepository;

/**
 * Application service for managing contributors and their assignments to content.
 */
@Service
public class ContributorManagementService {

    private final ContributorRepository contributorRepository;
    private final ContentRepository contentRepository;

    public ContributorManagementService(
            ContributorRepository contributorRepository,
            ContentRepository contentRepository) {
        this.contributorRepository = contributorRepository;
        this.contentRepository = contentRepository;
    }

    /**
     * Creates a contributor identity that can later be assigned to content.
     */
    @Transactional
    public ContributorRecord createContributor(CreateContributorCommand command) {
        return ContentManagementMapper.toContributorRecord(
                contributorRepository.save(Contributor.create(command.displayName())));
    }

    /**
     * Lists recent contributors for admin workflows.
     */
    @Transactional(readOnly = true)
    public List<ContributorRecord> listContributors(int limit) {
        int sanitizedLimit = sanitizeLimit(limit);
        return contributorRepository.findRecent(sanitizedLimit).stream()
                .map(ContentManagementMapper::toContributorRecord)
                .toList();
    }

    /**
     * Renames an existing contributor.
     */
    @Transactional
    public ContributorRecord renameContributor(RenameContributorCommand command) {
        Contributor contributor = loadContributor(command.contributorId());
        contributor.rename(command.displayName());
        return ContentManagementMapper.toContributorRecord(contributorRepository.save(contributor));
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
                command.creditName(),
                command.sortOrder());
        return ContentManagementMapper.toContentContributorRecord(
                command.contentId(),
                contentRepository.save(content).getContributors().stream()
                        .filter(candidate -> candidate == assignment)
                .findFirst()
                .orElse(assignment));
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
        contentRepository.save(content);
    }

    private Contributor loadContributor(Long contributorId) {
        return contributorRepository.findById(contributorId)
                .orElseThrow(() -> new ContributorNotFoundException(contributorId));
    }

    private Content loadContent(Long contentId) {
        return contentRepository.findById(contentId)
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
}
