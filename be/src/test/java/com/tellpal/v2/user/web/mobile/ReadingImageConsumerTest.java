package com.tellpal.v2.user.web.mobile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.tellpal.v2.user.application.*;
import com.tellpal.v2.user.infrastructure.firebase.FirebaseAuthenticationProperties;
import com.tellpal.v2.content.api.*;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi;

class ReadingImageConsumerTest {
    @Test void accountIsServerDerivedAndRevocationIsCheckedAfterStorage() {
        var tokens=mock(FirebaseTokenVerifier.class);var api=mock(ReadingImageConsumerApi.class);
        var service=new ReadingImageConsumerService(tokens,new FirebaseAuthenticationProperties(false,"test",null,true),api,"allowed");
        when(tokens.verify("ephemeral")).thenReturn(new VerifiedFirebaseToken("allowed"));
        var identity=new ReadingImageComparisonApi.Identity(1,"tr",2,"revision","digest",java.util.UUID.randomUUID());
        var selection=new ReadingImageDeliveryApi.Selection(com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role.READING_DETAIL,0,"key",false);
        when(api.read(identity,selection)).thenReturn(new ReadingImageConsumerApi.Payload(new byte[]{1},"image/jpeg"));
        assertThat(service.manifest("Bearer ephemeral",1,"tr").accountScope()).isEqualTo("allowed");
        assertThatThrownBy(()->service.read("Bearer ephemeral","wrong",identity,selection)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(api,never()).read(any(),any());
        when(tokens.verify("ephemeral")).thenReturn(new VerifiedFirebaseToken("allowed"),new VerifiedFirebaseToken("revoked"));
        assertThatThrownBy(()->service.read("Bearer ephemeral","allowed",identity,selection)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(api).read(identity,selection);verify(api,never()).status(any());
    }
    @Test void disabledOrStubOrUnrevokedAuthNeverOpensStorage() {
        var tokens=mock(FirebaseTokenVerifier.class);var api=mock(ReadingImageConsumerApi.class);
        for(var auth:java.util.List.of(new FirebaseAuthenticationProperties(true,"test",null,true),new FirebaseAuthenticationProperties(false,"test",null,false))) {
            var service=new ReadingImageConsumerService(tokens,auth,api,"allowed");
            assertThatThrownBy(()->service.manifest("Bearer ephemeral",1,"tr")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        }
        verifyNoInteractions(tokens);verify(api,never()).manifest(anyLong(),anyString());
    }
    @Test void consumerGateRequiresExplicitLocalTestBudgetsAndRejectsProduction() {
        for(String[] profiles:new String[][]{{},{"local"},{"test"},{"local","production"},{"PROD","test"}}) {
            var env=new org.springframework.mock.env.MockEnvironment();env.setActiveProfiles(profiles);
            var p=new com.tellpal.v2.content.application.publication.ReadingImageConsumerProperties(env,true,"local-run",100,100,2000,1000,1000,20,1000);
            assertThat(p.admitted()).isEqualTo(java.util.Arrays.equals(profiles,new String[]{"local"})||java.util.Arrays.equals(profiles,new String[]{"test"}));
        }
    }
}
