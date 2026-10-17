package com.tellpal.v2.support;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.*;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.application.reading.*;
import com.tellpal.v2.asset.domain.*;
import com.tellpal.v2.asset.domain.reading.*;
import com.tellpal.v2.asset.infrastructure.persistence.reading.*;
import com.tellpal.v2.asset.infrastructure.processing.reading.ReadingImageWorker;
import com.tellpal.v2.asset.infrastructure.storage.*;
import com.tellpal.v2.asset.reading.ReadingFixtures;
import com.tellpal.v2.content.application.publication.*;
import com.tellpal.v2.content.application.ReadingImageSnapshotAssembler;
import com.tellpal.v2.content.domain.ContentRepository;
import com.tellpal.v2.content.infrastructure.persistence.publication.JdbcReadingPublicationStore;

/** Gerçek PostgreSQL/HTTP ve encoder; storage disk fake'i olduğundan bulut kanıtı değildir. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers=P4LocalComparisonFixtureIT.NoSchedulers.class)
class P4LocalComparisonFixtureIT extends PostgresIntegrationTestBase {
    public static class NoSchedulers implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override public void initialize(ConfigurableApplicationContext context) {
            context.addBeanFactoryPostProcessor(factory->{
                var registry=(BeanDefinitionRegistry)factory;
                String name=org.springframework.scheduling.config.TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME;
                if(registry.containsBeanDefinition(name))registry.removeBeanDefinition(name);
            });
        }
    }
    @Autowired ConfigurableApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @Autowired AssetRegistryApi registry;
    @Autowired MediaAssetRepository assets;
    @Autowired JdbcReadingSourceAdmissionStore admissionStore;
    @Autowired ReadingJobStore jobs;
    @Autowired ReadingPublicationProperties config;
    @Autowired ReadingPublicationMutationGuard guard;
    @Autowired ContentRepository contents;
    @Autowired JdbcReadingPublicationStore publications;
    @Autowired ReadingImageSnapshotAssembler assembler;
    @TempDir Path directory;

    @Test void oneCopyPublishesAutomaticallyWithFourOutputsButRemainsInvisibleThroughRealHttp() throws Exception {
        requireNoScheduler();
        for(long source:new long[]{9778,9816,9818}) jdbc.update("insert into media_assets(id,provider,object_path,media_type,kind) values(?,'FIREBASE_STORAGE',?,'IMAGE','ORIGINAL_IMAGE')",source,"test/legacy/"+source);
        jdbc.update("insert into media_assets(id,provider,object_path,media_type,kind) values(9900,'LOCAL_STUB','test/audio','AUDIO','ORIGINAL_AUDIO')");
        jdbc.update("insert into contents(id,type,external_key,age_range,page_count,is_active) values(71,'STORY','p4-old-tr71',3,2,true)");
        jdbc.update("insert into content_localizations(content_id,language_code,title,description,cover_media_id,status,processing_status) values(71,'tr','Eski kaynak','Kaynak açıklaması',9778,'DRAFT','PENDING')");
        for(int i=1;i<=2;i++) {
            long page=jdbc.queryForObject("insert into story_pages(content_id,page_number) values(71,?) returning id",Long.class,i);
            jdbc.update("insert into story_page_localizations(story_page_id,language_code,body_text,audio_media_id,illustration_media_id) values(?,'tr',?,9900,?)",page,"Kaynak sayfa "+i,i==1?9816:9818);
        }
        var baseline=P3LocalCmsSetup.snapshot(jdbc);
        var disk=new ReadingFixtures.DiskStorage(directory.resolve("objects"));
        var env=new MockEnvironment();env.setActiveProfiles("test");
        var properties=new AssetStorageFirebaseProperties(true,"synthetic","synthetic","unused",P4LocalComparisonLedger.PREFIX,null,null,null);
        var old=ReadingFixtures.profile();
        var profile=new TestProfile(old.width(),old.height(),old.maxSourceBytes(),old.maxOutputBytes(),old.maxPixels(),old.parentMemoryBytes(),old.heapMiB(),old.directMiB(),old.processMiB(),old.diskBytes(),old.sourceMillis(),old.revisionMillis(),1,1,false,"");
        config.setEnabled(true);config.setProfiles(Map.of(Role.READING_COVER,profile,Role.READING_DETAIL,profile,Role.READING_PAGE,profile));
        try(var ledger=new P4LocalComparisonLedger(directory.resolve("ledger"),Clock.systemUTC())) {
            var storage=new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(new P4LocalComparisonStorage(disk,ledger))));
            var admissions=new ReadingSourceAdmissionService(assets,storage,admissionStore,properties,env,true);
            var preparation=new ReadingImagePreparationService(assets,storage,jobs,env,properties,true,admissions);
            var ready=new ReadingPublicationReadyService(preparation,admissions,jobs,jdbc,manager,storage);
            var publication=new ReadingPublicationService(config,guard,contents,publications,assembler,ready,manager);
            var fixture=P4LocalComparisonFixture.seed(context,ledger);
            P4LocalComparisonSetup.isolation(context,fixture.id);
            assertThat(jdbc.queryForObject("select count(*) from contents",Integer.class)).isEqualTo(2);
            ledger.phase(0);long[] original=new long[3];
            for(int i=0;i<3;i++) {
                byte[] bytes=ReadingFixtures.image(80+i,60);
                String path=P4LocalComparisonLedger.PREFIX+"/canonical/"+i+".png";
                storage.create(StorageProvider.FIREBASE_STORAGE,path,"image/png",bytes,1000000,5000);
                original[i]=registry.register(new RegisterMediaAssetCommand(AssetStorageProvider.FIREBASE_STORAGE,path,AssetKind.ORIGINAL_IMAGE,"image/png",(long)bytes.length,null)).assetId();
            }
            ledger.phase(1);long[] admitted=new long[3];
            for(int i=0;i<3;i++)admitted[i]=admissions.admit(original[i],profile).assetId();
            fixture.rebind(context,admitted);
            ledger.phase(2);publication.reconcile(fixture.id,"tr");
            UUID job=jdbc.queryForObject("select job_id from reading_publications where content_id=?",UUID.class,fixture.id);
            ledger.phase(3);new ReadingImageWorker(preparation,jobs,storage,Clock.systemUTC()).run(job);
            assertThat(jobs.find(job).diagnostic()).isNull();
            assertThat(jobs.find(job).state()).isEqualTo(State.COMPLETED);
            ledger.phase(4);publication.reconcile(fixture.id,"tr");
            assertThat(publication.status(fixture.id,"tr").state()).isEqualTo("LIVE");
            var manifest=new com.fasterxml.jackson.databind.ObjectMapper().readValue(jobs.find(job).manifest(),ReadingWork.Manifest.class);
            assertThat(manifest.outputs()).hasSize(4);
            assertThat(jdbc.queryForObject("select attempt from reading_image_jobs where id=?",Integer.class,job)).isEqualTo(1);
            P4LocalComparisonSetup.isolation(context,fixture.id);
            P3LocalCmsSetup.requirePreserved(baseline,P3LocalCmsSetup.snapshot(jdbc));
            assertThat(jdbc.queryForObject("select count(*) from content_free_access where content_id=? and access_key='default'",Integer.class,fixture.id)).isEqualTo(1);
        } finally {config.setEnabled(false);}
    }
    void requireNoScheduler() {
        assertThat(context.containsBean(org.springframework.scheduling.config.TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)).isFalse();
        assertThat(context.getBeansOfType(org.springframework.scheduling.config.ScheduledTaskHolder.class)).isEmpty();
    }
}
