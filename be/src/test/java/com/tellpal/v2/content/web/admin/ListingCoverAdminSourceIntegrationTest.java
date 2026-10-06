package com.tellpal.v2.content.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.tellpal.v2.support.AdminApiIntegrationTestSupport;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.api.AssetProcessingCommands.*;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.infrastructure.media.ListingCoverProcessingExecutor;
import com.tellpal.v2.asset.infrastructure.processing.AssetProcessingJobExecutor;
import com.tellpal.v2.asset.infrastructure.storage.AssetStorageClientRegistry;
import com.tellpal.v2.category.api.*;
import com.tellpal.v2.category.application.CategoryHomeConfigurationService;
import com.tellpal.v2.shared.domain.LanguageCode;

@SpringBootTest(properties="tellpal.asset.processing.poller.enabled=false")
class ListingCoverAdminSourceIntegrationTest extends AdminApiIntegrationTestSupport {
    @Autowired AssetRegistryApi assets;
    @Autowired AssetStorageClientRegistry storage;
    @Autowired AssetProcessingApi jobs;
    @Autowired AssetProcessingJobExecutor executor;
    @Autowired LibraryHomeQueryApi library;
    @Autowired CategoryHomeConfigurationService home;

    @BeforeEach void reset() {
        jdbcTemplate.execute("truncate admin_refresh_tokens,admin_user_roles,admin_users,categories,contents,media_assets restart identity cascade");
    }

    @Test void authenticatedEnglishPreparationUsesItsCurrentLocalizedSourceAndPublishesEnglishCatalogCovers() throws Exception {
        String token = authenticateAdmin();
        long content = jdbcTemplate.queryForObject("insert into contents(type,external_key,is_active,age_range,page_count) values('STORY','localized-admin-cover',true,5,0) returning id", Long.class);
        long turkish = image("admin-tr", 80, 100);
        long oldEnglish = image("admin-en-old", 60, 90);
        long currentEnglish = image("admin-en-current", 120, 160);
        jdbcTemplate.update("insert into content_localizations(content_id,language_code,title,status,processing_status,published_at,cover_media_id) values(?,'tr','TR cover','PUBLISHED','COMPLETED',now(),?),(?,'en','EN cover','PUBLISHED','COMPLETED',now(),?)", content, turkish, content, oldEnglish);
        jdbcTemplate.update("update content_localizations set cover_media_id=? where content_id=? and language_code='en'", currentEnglish, content);
        long row = jdbcTemplate.queryForObject("insert into categories(slug,type) values('localized-admin-cover','STORY') returning id", Long.class);
        jdbcTemplate.update("insert into category_localizations(category_id,language_code,name,status,published_at) values(?,'tr','Turkish','PUBLISHED',now()),(?,'en','English','PUBLISHED',now())", row, row);
        jdbcTemplate.update("insert into category_contents(category_id,content_id,language_code,display_order) values(?,?,'tr',0),(?,?,'en',0)", row, content, row, content);
        home.save(row,"en",new CategoryHomeConfigurationService.Configuration(row,0,"ALL",null));
        var response = mockMvc.perform(post("/api/admin/contents/{id}/localizations/en/listing-cover-processing",content).header("Authorization","Bearer "+token))
                .andExpect(status().isAccepted()).andReturn();
        var payload = readPayload(response);
        assertThat(payload.get("ready").asBoolean()).isFalse();
        var target = AssetProcessingTarget.localization(content, LanguageCode.EN);
        var scheduled = jobs.findByTarget(target, AssetProcessingKind.LISTING_COVER).orElseThrow();
        assertThat(scheduled.coverSourceAssetId()).isEqualTo(currentEnglish);
        assertThat(scheduled.target()).isEqualTo(target);
        assertThat(payload.get("operationId").asLong()).isEqualTo(scheduled.processingId());
        var persisted = jdbcTemplate.queryForMap("select * from asset_processing where id=?", scheduled.processingId());
        assertThat(persisted.get("content_id")).isEqualTo(content);
        assertThat(persisted.get("language_code")).isEqualTo("en");
        assertThat(jobs.findByTarget(AssetProcessingTarget.localization(content,LanguageCode.TR),AssetProcessingKind.LISTING_COVER)).isEmpty();
        executor.process(jobs.start(new StartAssetProcessingCommand(target, AssetProcessingKind.LISTING_COVER)));
        var item = library.query("en",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER).rows().getFirst().items().getFirst();
        assertThat(item.contentId()).isEqualTo(content);
        assertThat(item.contentLanguage()).isEqualTo("en");
        assertThat(item.covers().phone()).isNotNull();
        assertThat(item.covers().tablet()).isNotNull();
        var manifest = jdbcTemplate.queryForMap("select * from listing_cover_revisions");
        assertThat(manifest.get("language_code")).isEqualTo("en");
        assertThat(manifest.get("source_asset_id")).isEqualTo(currentEnglish);
        assertThat(manifest.get("phone_asset_id")).isEqualTo(item.covers().phone().assetId());
        assertThat(manifest.get("tablet_asset_id")).isEqualTo(item.covers().tablet().assetId());
        var phone = assets.findById(item.covers().phone().assetId()).orElseThrow();
        var stored = storage.openObject(StorageProvider.FIREBASE_STORAGE,phone.storageLocation().objectPath(),null).orElseThrow();
        try(var input=stored.content()) { var bitmap=ImageIO.read(input);assertThat(bitmap.getWidth()).isEqualTo(120);assertThat(bitmap.getHeight()).isEqualTo(160); }
        mockMvc.perform(post("/api/admin/contents/{id}/localizations/en/listing-cover-processing",content).header("Authorization","Bearer "+token)).andExpect(status().isOk());
    }

    private long image(String name,int width,int height) throws Exception {
        var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB),"png",output);
        byte[] bytes=output.toByteArray();String path="test/library/"+name+".png";
        storage.uploadObject(StorageProvider.FIREBASE_STORAGE,path,"image/png",bytes.length,new ByteArrayInputStream(bytes));
        return assets.register(new RegisterMediaAssetCommand(AssetStorageProvider.FIREBASE_STORAGE,path,AssetKind.ORIGINAL_IMAGE,"image/png",(long)bytes.length,ListingCoverProcessingExecutor.sha256(bytes))).assetId();
    }
}
