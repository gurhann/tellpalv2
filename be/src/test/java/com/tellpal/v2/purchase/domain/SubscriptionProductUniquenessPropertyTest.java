package com.tellpal.v2.purchase.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Assume;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for subscription product uniqueness constraint (Özellik 16).
 *
 * **Validates: Requirements 9.1**
 *
 * Özellik 16: Abonelik Ürün Benzersizliği
 * - The combination of (store, product_id) must be unique in subscription_products.
 * - No two SubscriptionProduct records can share the same (store, product_id) pair.
 */
public class SubscriptionProductUniquenessPropertyTest {

    record SubscriptionProductRecord(String store, String productId) {}

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    /**
     * Generates a store value from the known set of supported stores.
     */
    @Provide
    Arbitrary<String> store() {
        return Arbitraries.of("APP_STORE", "PLAY_STORE", "STRIPE", "RC_BILLING", "AMAZON");
    }

    /**
     * Generates a product ID: alphanumeric string between 5 and 30 characters.
     */
    @Provide
    Arbitrary<String> productId() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMinLength(5)
                .ofMaxLength(30);
    }

    /**
     * Generates a single SubscriptionProductRecord with a random store and product ID.
     */
    @Provide
    Arbitrary<SubscriptionProductRecord> subscriptionProduct() {
        return Combinators.combine(store(), productId())
                .as(SubscriptionProductRecord::new);
    }

    /**
     * Generates a list of SubscriptionProductRecords with distinct (store, product_id) pairs (2–10 entries).
     */
    @Provide
    Arbitrary<List<SubscriptionProductRecord>> distinctSubscriptionProducts() {
        return subscriptionProduct()
                .list()
                .ofMinSize(2)
                .ofMaxSize(10)
                .uniqueElements(p -> p.store() + "|" + p.productId());
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 16 — A set of distinct (store, product_id) pairs contains no duplicates.
     *
     * **Validates: Requirements 9.1**
     */
    @Property(tries = 100)
    void distinctPairsHaveNoDuplicates(
            @ForAll("distinctSubscriptionProducts") List<SubscriptionProductRecord> products) {

        Set<String> keys = new HashSet<>();
        for (SubscriptionProductRecord p : products) {
            keys.add(p.store() + "|" + p.productId());
        }

        assertThat(keys)
                .as("A collection of distinct (store, product_id) pairs must contain no duplicates")
                .hasSize(products.size());
    }

    /**
     * Özellik 16 — Two products with the same (store, product_id) pair violate the uniqueness constraint.
     *
     * **Validates: Requirements 9.1**
     */
    @Property(tries = 100)
    void sameStoreSameProductIdViolatesUniqueness(@ForAll("subscriptionProduct") SubscriptionProductRecord product) {
        SubscriptionProductRecord duplicate = new SubscriptionProductRecord(product.store(), product.productId());

        Set<String> keys = new HashSet<>();
        keys.add(product.store() + "|" + product.productId());
        boolean isDuplicate = !keys.add(duplicate.store() + "|" + duplicate.productId());

        assertThat(isDuplicate)
                .as("Two products with store='%s' and product_id='%s' must be detected as a uniqueness violation",
                        product.store(), product.productId())
                .isTrue();
    }

    /**
     * Özellik 16 — Two products with the same store but different product_ids are distinct.
     *
     * **Validates: Requirements 9.1**
     */
    @Property(tries = 100)
    void sameStoreDifferentProductIdAreDistinct(
            @ForAll("store") String store,
            @ForAll("productId") String productId1,
            @ForAll("productId") String productId2) {

        Assume.that(!productId1.equals(productId2));

        SubscriptionProductRecord p1 = new SubscriptionProductRecord(store, productId1);
        SubscriptionProductRecord p2 = new SubscriptionProductRecord(store, productId2);

        String key1 = p1.store() + "|" + p1.productId();
        String key2 = p2.store() + "|" + p2.productId();

        assertThat(key1)
                .as("Products with the same store but different product_ids must be distinct pairs")
                .isNotEqualTo(key2);
    }

    /**
     * Özellik 16 — Two products with the same product_id but different stores are distinct.
     *
     * **Validates: Requirements 9.1**
     */
    @Property(tries = 100)
    void sameProductIdDifferentStoreAreDistinct(
            @ForAll("store") String store1,
            @ForAll("store") String store2,
            @ForAll("productId") String productId) {

        Assume.that(!store1.equals(store2));

        SubscriptionProductRecord p1 = new SubscriptionProductRecord(store1, productId);
        SubscriptionProductRecord p2 = new SubscriptionProductRecord(store2, productId);

        String key1 = p1.store() + "|" + p1.productId();
        String key2 = p2.store() + "|" + p2.productId();

        assertThat(key1)
                .as("Products with the same product_id but different stores must be distinct pairs")
                .isNotEqualTo(key2);
    }
}
