package com.tellpal.v2.purchase.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.tellpal.v2.purchase.application.PurchaseLookupCatalog;

@Repository
public class JdbcPurchaseLookupCatalog implements PurchaseLookupCatalog {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPurchaseLookupCatalog(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean hasActiveEventType(String code) {
        return exists(
                "select exists(select 1 from purchase_event_types where code = ? and is_active = true)",
                code);
    }

    @Override
    public boolean hasActiveSubscriptionPeriodType(String code) {
        return exists(
                "select exists(select 1 from subscription_period_types where code = ? and is_active = true)",
                code);
    }

    @Override
    public boolean hasActiveStore(String code) {
        return exists(
                "select exists(select 1 from purchase_stores where code = ? and is_active = true)",
                code);
    }

    @Override
    public boolean hasActiveEnvironment(String code) {
        return exists(
                "select exists(select 1 from purchase_environments where code = ? and is_active = true)",
                code);
    }

    @Override
    public boolean hasActiveReasonCode(String reasonType, String code) {
        return exists(
                """
                select exists(
                    select 1
                    from purchase_reason_codes
                    where reason_type = ?
                      and code = ?
                      and is_active = true
                )
                """,
                reasonType,
                code);
    }

    private boolean exists(String sql, Object... arguments) {
        Boolean result = jdbcTemplate.queryForObject(sql, Boolean.class, arguments);
        return Boolean.TRUE.equals(result);
    }
}
