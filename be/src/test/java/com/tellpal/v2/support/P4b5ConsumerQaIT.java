package com.tellpal.v2.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.time.Clock;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi.Limits;
import com.tellpal.v2.asset.application.reading.ReadingComparisonBudget;

/** Yalnız açıkça seçilmiş sentetik yerel PostgreSQL'de V37 restart borcunu sınar. */
class P4b5ConsumerQaIT {
    @TempDir Path directory;
    @Test void realV37QuotaSurvivesNewBudgetInstanceAndFailedStorageIo() throws Exception {
        String url=System.getenv("P4B5_SYNTHETIC_JDBC_URL");
        Assumptions.assumeTrue(url!=null,"İzole sentetik JDBC seçilmedi");
        assertThat(url).matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/p4b5_synthetic_[a-z0-9_]+");
        var source=new DriverManagerDataSource(url,System.getProperty("user.name"),"");var jdbc=new JdbcTemplate(source);
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/migration/V37__add_reading_comparison_budget.sql")));
        jdbc.update("insert into reading_comparison_budgets(run_id,max_operations,max_bytes,operations,reserved_bytes) values(?,64,67108864,39,44742970)",P4b5ConsumerQaLedger.RUN);
        var manager=new DataSourceTransactionManager(source);
        var limits=new Limits(8388608,4800000,134217728,45000,P4b5ConsumerQaLedger.RUN,64,67108864);
        new ReadingComparisonBudget(jdbc,manager).reserve(limits,3292721);
        assertThatThrownBy(()->{throw new java.io.IOException("SYNTHETIC_STORAGE_FAILURE");}).isInstanceOf(java.io.IOException.class);
        for(int i=0;i<24;i++)new ReadingComparisonBudget(jdbc,manager).reserve(limits,0);
        assertThat(jdbc.queryForObject("select operations from reading_comparison_budgets where run_id=?",Long.class,P4b5ConsumerQaLedger.RUN)).isEqualTo(64);
        assertThat(jdbc.queryForObject("select reserved_bytes from reading_comparison_budgets where run_id=?",Long.class,P4b5ConsumerQaLedger.RUN)).isEqualTo(48035691);
        assertThatThrownBy(()->new ReadingComparisonBudget(jdbc,manager).reserve(limits,0)).hasMessage("DELIVERY_TOTAL_RESOURCE_LIMIT");
    }
    @Test void postContextCloseDatabaseReservationCannotBecomeSuccessfulOutcome() throws Exception {
        String url=System.getenv("P4B5_FINAL_SYNTHETIC_JDBC_URL");
        Assumptions.assumeTrue(url!=null,"İzole nihai JDBC seçilmedi");
        assertThat(url).matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/p4b5_synthetic_[a-z0-9_]+");
        var source=new DriverManagerDataSource(url,System.getProperty("user.name"),"");var jdbc=new JdbcTemplate(source);
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/migration/V37__add_reading_comparison_budget.sql")));
        jdbc.update("insert into reading_comparison_budgets(run_id,max_operations,max_bytes,operations,reserved_bytes) values(?,64,67108864,39,44742970)",P4b5ConsumerQaLedger.RUN);
        var budget=new ReadingComparisonBudget(jdbc,new DataSourceTransactionManager(source));
        var limits=new Limits(8388608,4800000,134217728,45000,P4b5ConsumerQaLedger.RUN,64,67108864);
        var ledger=new P4b5ConsumerQaLedger(directory.resolve("ledger.log"),P4b5ConsumerQaLedgerTest.fixture("proposal.json"),P4b5ConsumerQaLedgerTest.fixture("terminal-ledger.txt"),P4b5ConsumerQaLedgerTest.fixture("retained-ready.json"),P4b5ConsumerQaLedgerTest.approval(),Clock.systemUTC(),()->0,false);
        ledger.importLoginReservation();ledger.openWindow(directory.resolve("window"));ledger.request("manifest",new Object[]{"synthetic",94L,"tr"});ledger.verify();
        for(int i=0;i<4;i++)P4b5ConsumerQaLedgerTest.media(ledger,i);
        for(int i=0;i<ledger.deliveryOperations();i++)budget.reserve(limits,i==0?ledger.reservedBytes():0);
        var context=mock(org.springframework.context.ConfigurableApplicationContext.class);
        var owner=new P4b5ConsumerQaSetup.OwnedContext(context);var links=P4b5ConsumerQaLedgerTest.JSON.createObjectNode();
        assertThatThrownBy(()->P4b5ConsumerQaSetup.successfulOutcome(directory,jdbc,ledger,owner.closed,links)).hasMessage("BACKEND_NOT_SUCCESSFULLY_DRAINED");
        doAnswer(invocation->{budget.reserve(limits,0);return null;}).when(context).close();
        owner.close();ledger.close();verify(context).close();
        assertThat(owner.closed).isTrue();assertThat(ledger.isClosed()).isTrue();
        assertThat(jdbc.queryForObject("select operations from reading_comparison_budgets where run_id=?",Long.class,P4b5ConsumerQaLedger.RUN)).isEqualTo(53);
        assertThatThrownBy(()->P4b5ConsumerQaSetup.successfulOutcome(directory,jdbc,ledger,owner.closed,links)).hasMessage("V37_RECONCILIATION_FAILED");
        assertThat(Files.exists(directory.resolve("backend-closed.json"))).isFalse();
    }

}
