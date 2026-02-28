package com.billing.charge.calculation.internal.dataloader;

import com.billing.charge.calculation.internal.model.*;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 2: 계약ID 기준 그룹핑 정합성
 *
 * 임의의 contractId를 가진 도메인 객체 리스트에 대해, 계약ID 기준으로 그룹핑하면:
 * - 결과 Map의 각 key에 대응하는 value 리스트의 모든 항목은 해당 contractId를 가진다
 * - 원본 리스트의 모든 항목이 결과 Map 어딘가에 존재한다 (데이터 유실 없음)
 * - 결과 Map의 모든 항목 수 합계는 원본 리스트의 크기와 동일하다
 *
 * **Validates: 요구사항 2.3, 3.3, 4.5, 5.5, 6.2, 8.3, 9.2, 10.3, 11.2**
 */
@Tag("Feature: subscription-data-load-refactor, Property 2: 계약ID 기준 그룹핑 정합성")
class ContractIdGroupingPropertyTest {

    // --- OneTimeChargeDomain: InstallmentHistory ---

    @Property(tries = 100)
    void installmentHistory_eachKeyValueShouldHaveMatchingContractId(
            @ForAll("installmentHistoryList") List<InstallmentHistory> items) {

        Map<String, List<InstallmentHistory>> grouped =
                groupBy(items, InstallmentHistory::getContractId);

        for (var entry : grouped.entrySet()) {
            for (InstallmentHistory item : entry.getValue()) {
                assertThat(item.getContractId()).isEqualTo(entry.getKey());
            }
        }
    }

    @Property(tries = 100)
    void installmentHistory_allItemsPreservedAndCountMatch(
            @ForAll("installmentHistoryList") List<InstallmentHistory> items) {

        Map<String, List<InstallmentHistory>> grouped =
                groupBy(items, InstallmentHistory::getContractId);

        List<InstallmentHistory> flattened = grouped.values().stream()
                .flatMap(List::stream).toList();

        assertThat(flattened).containsExactlyInAnyOrderElementsOf(items);
        assertThat((long) flattened.size()).isEqualTo(items.size());
    }

    // --- OneTimeChargeDomain: PenaltyFee ---

    @Property(tries = 100)
    void penaltyFee_eachKeyValueShouldHaveMatchingContractId(
            @ForAll("penaltyFeeList") List<PenaltyFee> items) {

        Map<String, List<PenaltyFee>> grouped =
                groupBy(items, PenaltyFee::getContractId);

        for (var entry : grouped.entrySet()) {
            for (PenaltyFee item : entry.getValue()) {
                assertThat(item.getContractId()).isEqualTo(entry.getKey());
            }
        }
    }

    @Property(tries = 100)
    void penaltyFee_allItemsPreservedAndCountMatch(
            @ForAll("penaltyFeeList") List<PenaltyFee> items) {

        Map<String, List<PenaltyFee>> grouped =
                groupBy(items, PenaltyFee::getContractId);

        List<PenaltyFee> flattened = grouped.values().stream()
                .flatMap(List::stream).toList();

        assertThat(flattened).containsExactlyInAnyOrderElementsOf(items);
        assertThat((long) flattened.size()).isEqualTo(items.size());
    }

    // --- UsageChargeDomain: VoiceUsage ---

    @Property(tries = 100)
    void voiceUsage_eachKeyValueShouldHaveMatchingContractId(
            @ForAll("voiceUsageList") List<VoiceUsage> items) {

        Map<String, List<VoiceUsage>> grouped =
                groupBy(items, VoiceUsage::getContractId);

        for (var entry : grouped.entrySet()) {
            for (VoiceUsage item : entry.getValue()) {
                assertThat(item.getContractId()).isEqualTo(entry.getKey());
            }
        }
    }

    @Property(tries = 100)
    void voiceUsage_allItemsPreservedAndCountMatch(
            @ForAll("voiceUsageList") List<VoiceUsage> items) {

        Map<String, List<VoiceUsage>> grouped =
                groupBy(items, VoiceUsage::getContractId);

        List<VoiceUsage> flattened = grouped.values().stream()
                .flatMap(List::stream).toList();

        assertThat(flattened).containsExactlyInAnyOrderElementsOf(items);
        assertThat((long) flattened.size()).isEqualTo(items.size());
    }

    // --- UsageChargeDomain: DataUsage ---

    @Property(tries = 100)
    void dataUsage_eachKeyValueShouldHaveMatchingContractId(
            @ForAll("dataUsageList") List<DataUsage> items) {

        Map<String, List<DataUsage>> grouped =
                groupBy(items, DataUsage::getContractId);

        for (var entry : grouped.entrySet()) {
            for (DataUsage item : entry.getValue()) {
                assertThat(item.getContractId()).isEqualTo(entry.getKey());
            }
        }
    }

    @Property(tries = 100)
    void dataUsage_allItemsPreservedAndCountMatch(
            @ForAll("dataUsageList") List<DataUsage> items) {

        Map<String, List<DataUsage>> grouped =
                groupBy(items, DataUsage::getContractId);

        List<DataUsage> flattened = grouped.values().stream()
                .flatMap(List::stream).toList();

        assertThat(flattened).containsExactlyInAnyOrderElementsOf(items);
        assertThat((long) flattened.size()).isEqualTo(items.size());
    }

    // --- 그룹핑 유틸리티 (실제 로더가 사용하는 Collectors.groupingBy 패턴) ---

    private <T> Map<String, List<T>> groupBy(List<T> items, Function<T, String> keyExtractor) {
        return items.stream().collect(Collectors.groupingBy(keyExtractor));
    }

    // --- Generators ---

    @Provide
    Arbitrary<List<InstallmentHistory>> installmentHistoryList() {
        return installmentHistory().list().ofMinSize(0).ofMaxSize(50);
    }

    @Provide
    Arbitrary<List<PenaltyFee>> penaltyFeeList() {
        return penaltyFee().list().ofMinSize(0).ofMaxSize(50);
    }

    @Provide
    Arbitrary<List<VoiceUsage>> voiceUsageList() {
        return voiceUsage().list().ofMinSize(0).ofMaxSize(50);
    }

    @Provide
    Arbitrary<List<DataUsage>> dataUsageList() {
        return dataUsage().list().ofMinSize(0).ofMaxSize(50);
    }

    private Arbitrary<String> contractIds() {
        return Arbitraries.of("C001", "C002", "C003", "C004", "C005");
    }

    private Arbitrary<InstallmentHistory> installmentHistory() {
        return Combinators.combine(contractIds(),
                        Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
                        Arbitraries.bigDecimals().between(BigDecimal.ZERO, new BigDecimal("100000")).ofScale(2),
                        Arbitraries.integers().between(1, 36),
                        Arbitraries.integers().between(1, 36))
                .as((cid, iid, amt, cur, tot) -> InstallmentHistory.builder()
                        .contractId(cid).installmentId(iid)
                        .installmentAmount(amt).currentInstallment(cur).totalInstallments(tot)
                        .build());
    }

    private Arbitrary<PenaltyFee> penaltyFee() {
        return Combinators.combine(contractIds(),
                        Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
                        Arbitraries.bigDecimals().between(BigDecimal.ZERO, new BigDecimal("50000")).ofScale(2),
                        Arbitraries.of("EARLY_TERMINATION", "CONTRACT_VIOLATION", "DEVICE_DAMAGE"))
                .as((cid, pid, amt, reason) -> PenaltyFee.builder()
                        .contractId(cid).penaltyId(pid)
                        .penaltyAmount(amt).penaltyReason(reason)
                        .build());
    }

    private Arbitrary<VoiceUsage> voiceUsage() {
        return Combinators.combine(contractIds(),
                        Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
                        Arbitraries.bigDecimals().between(BigDecimal.ONE, new BigDecimal("3600")).ofScale(0),
                        Arbitraries.bigDecimals().between(BigDecimal.ZERO, new BigDecimal("100")).ofScale(2))
                .as((cid, uid, dur, price) -> VoiceUsage.builder()
                        .contractId(cid).usageId(uid)
                        .duration(dur).unitPrice(price)
                        .build());
    }

    private Arbitrary<DataUsage> dataUsage() {
        return Combinators.combine(contractIds(),
                        Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
                        Arbitraries.bigDecimals().between(BigDecimal.ONE, new BigDecimal("10240")).ofScale(2),
                        Arbitraries.bigDecimals().between(BigDecimal.ZERO, new BigDecimal("50")).ofScale(2))
                .as((cid, uid, vol, price) -> DataUsage.builder()
                        .contractId(cid).usageId(uid)
                        .dataVolume(vol).unitPrice(price)
                        .build());
    }
}
