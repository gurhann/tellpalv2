package com.tellpal.v2.asset.application.reading;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi.Limits;

/** Harcanan rezervasyonlar başarısızlık veya restart ile geri verilmez. */
@Component
public class ReadingComparisonBudget {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    public ReadingComparisonBudget(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc=jdbc;this.tx=new TransactionTemplate(manager);
    }
    public void reserve(Limits limits,long bytes) {
        ReadingPreparationCapacity.requireNoTransaction();
        boolean accepted=Boolean.TRUE.equals(tx.execute(s->{
            jdbc.update("insert into reading_comparison_budgets(run_id,max_operations,max_bytes) values(?,?,?) on conflict do nothing",
                    limits.runId(),limits.maxTotalOperations(),limits.maxTotalBytes());
            return jdbc.update("""
                    update reading_comparison_budgets set operations=operations+1,reserved_bytes=reserved_bytes+?
                    where run_id=? and max_operations=? and max_bytes=?
                    and operations<max_operations and ?<=max_bytes-reserved_bytes
                    """,bytes,limits.runId(),limits.maxTotalOperations(),limits.maxTotalBytes(),bytes)==1;
        }));
        if(!accepted) throw new ReadingVerificationInconclusiveException("DELIVERY_TOTAL_RESOURCE_LIMIT");
    }
}
