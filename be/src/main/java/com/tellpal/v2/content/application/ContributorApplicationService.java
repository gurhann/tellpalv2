package com.tellpal.v2.content.application;

import com.tellpal.v2.content.domain.ContentContributor;
import com.tellpal.v2.content.domain.ContentContributorRepository;
import com.tellpal.v2.content.domain.Contributor;
import com.tellpal.v2.content.domain.ContributorRepository;
import com.tellpal.v2.content.domain.ContributorRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ContributorApplicationService {

    private final ContributorRepository contributorRepository;
    private final ContentContributorRepository contentContributorRepository;

    public ContributorApplicationService(
            ContributorRepository contributorRepository,
            ContentContributorRepository contentContributorRepository) {
        this.contributorRepository = contributorRepository;
        this.contentContributorRepository = contentContributorRepository;
    }

    public Contributor createContributor(String displayName) {
        return contributorRepository.save(new Contributor(displayName));
    }

    public Contributor updateContributor(Long id, String displayName) {
        Contributor contributor = getContributor(id);
        contributor.setDisplayName(displayName);
        return contributorRepository.save(contributor);
    }

    public void deleteContributor(Long id) {
        if (!contributorRepository.existsById(id)) {
            throw new ContributorNotFoundException(id);
        }
        contributorRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Contributor getContributor(Long id) {
        return contributorRepository.findById(id)
                .orElseThrow(() -> new ContributorNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Contributor> listContributors() {
        return contributorRepository.findAll();
    }

    public ContentContributor addContentContributor(Long contentId, Long contributorId,
            ContributorRole role, String languageCode, String creditName, int sortOrder) {
        if (!contributorRepository.existsById(contributorId)) {
            throw new ContributorNotFoundException(contributorId);
        }
        ContentContributor cc = new ContentContributor(contentId, contributorId, role);
        cc.setLanguageCode(languageCode);
        cc.setCreditName(creditName);
        cc.setSortOrder(sortOrder);
        return contentContributorRepository.save(cc);
    }

    public void removeContentContributor(Long contentId, Long contributorId) {
        contentContributorRepository.deleteByContentIdAndContributorId(contentId, contributorId);
    }

    @Transactional(readOnly = true)
    public List<ContentContributor> listContentContributors(Long contentId) {
        return contentContributorRepository.findByContentIdOrderBySortOrderAsc(contentId);
    }

    public ContentContributor updateSortOrder(Long contentContributorId, int newSortOrder) {
        ContentContributor cc = contentContributorRepository.findById(contentContributorId)
                .orElseThrow(() -> new ContentContributorNotFoundException(contentContributorId));
        cc.setSortOrder(newSortOrder);
        return contentContributorRepository.save(cc);
    }
}
