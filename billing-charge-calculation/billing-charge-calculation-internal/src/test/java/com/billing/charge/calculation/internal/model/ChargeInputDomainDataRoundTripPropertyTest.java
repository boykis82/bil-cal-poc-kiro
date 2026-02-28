package com.billing.charge.calculation.internal.model;

import net.jqwik.api.*;
import net.jqwik.api.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 3: ChargeInput 도메인 데이터 저장/조회 round-trip
 *
 * 임의의 OneTimeChargeDomain/UsageChargeDomain 구현체 리스트를 ChargeInput에 put한 후
 * 동일 Class 타입으로 get하면 동일 리스트 반환 검증.
 *
 * Validates: 요구사항 13.2, 13.3
 */
@Tag("Feature: subscription-data-load-refactor, Property 3: ChargeInput 도메인 데이터 저장/조회 round-trip")
class ChargeInputDomainDataRoundTripPropertyTest {

    @Property(tries = 100)
    void oneTimeChargeDomain_putThenGet_returnsIdenticalList_installmentHistory(
            @ForAll("installmentHistories") List<InstallmentHistory> data) {

        ChargeInput chargeInput = ChargeInput.builder().build();

        chargeInput.putOneTimeChargeData(InstallmentHistory.class, data);
        List<InstallmentHistory> retrieved = chargeInput.getOneTimeChargeData(InstallmentHistory.class);

        assertThat(retrieved).isEqualTo(data);
    }

    @Property(tries = 100)
    void oneTimeChargeDomain_putThenGet_returnsIdenticalList_penaltyFee(
            @ForAll("penaltyFees") List<PenaltyFee> data) {

        ChargeInput chargeInput = ChargeInput.builder().build();

        chargeInput.putOneTimeChargeData(PenaltyFee.class, data);
        List<PenaltyFee> retrieved = chargeInput.getOneTimeChargeData(PenaltyFee.class);

        assertThat(retrieved).isEqualTo(data);
    }

    @Property(tries = 100)
    void usageChargeDomain_putThenGet_returnsIdenticalList_voiceUsage(
            @ForAll("voiceUsages") List<VoiceUsage> data) {

        ChargeInput chargeInput = ChargeInput.builder().build();

        chargeInput.putUsageChargeData(VoiceUsage.class, data);
        List<VoiceUsage> retrieved = chargeInput.getUsageChargeData(VoiceUsage.class);

        assertThat(retrieved).isEqualTo(data);
    }

    @Property(tries = 100)
    void usageChargeDomain_putThenGet_returnsIdenticalList_dataUsage(
            @ForAll("dataUsages") List<DataUsage> data) {

        ChargeInput chargeInput = ChargeInput.builder().build();

        chargeInput.putUsageChargeData(DataUsage.class, data);
        List<DataUsage> retrieved = chargeInput.getUsageChargeData(DataUsage.class);

        assertThat(retrieved).isEqualTo(data);
    }

    @Property(tries = 100)
    void multipleTypes_putThenGet_doNotInterfere(
            @ForAll("installmentHistories") @Size(min = 0, max = 10) List<InstallmentHistory> installments,
            @ForAll("penaltyFees") @Size(min = 0, max = 10) List<PenaltyFee> penalties,
            @ForAll("voiceUsages") @Size(min = 0, max = 10) List<VoiceUsage> voices,
            @ForAll("dataUsages") @Size(min = 0, max = 10) List<DataUsage> dataUsages) {

        ChargeInput chargeInput = ChargeInput.builder().build();

        chargeInput.putOneTimeChargeData(InstallmentHistory.class, installments);
        chargeInput.putOneTimeChargeData(PenaltyFee.class, penalties);
        chargeInput.putUsageChargeData(VoiceUsage.class, voices);
        chargeInput.putUsageChargeData(DataUsage.class, dataUsages);

        assertThat(chargeInput.getOneTimeChargeData(InstallmentHistory.class)).isEqualTo(installments);
        assertThat(chargeInput.getOneTimeChargeData(PenaltyFee.class)).isEqualTo(penalties);
        assertThat(chargeInput.getUsageChargeData(VoiceUsage.class)).isEqualTo(voices);
        assertThat(chargeInput.getUsageChargeData(DataUsage.class)).isEqualTo(dataUsages);
    }

    @Property(tries = 100)
    void getWithoutPut_returnsEmptyList() {
        ChargeInput chargeInput = ChargeInput.builder().build();

        assertThat(chargeInput.getOneTimeChargeData(InstallmentHistory.class)).isEmpty();
        assertThat(chargeInput.getOneTimeChargeData(PenaltyFee.class)).isEmpty();
        assertThat(chargeInput.getUsageChargeData(VoiceUsage.class)).isEmpty();
        assertThat(chargeInput.getUsageChargeData(DataUsage.class)).isEmpty();
    }

    // --- Generators ---

    @Provide
    Arbitrary<List<InstallmentHistory>> installmentHistories() {
        return installmentHistory().list().ofMinSize(0).ofMaxSize(20);
    }

    @Provide
    Arbitrary<List<PenaltyFee>> penaltyFees() {
        return penaltyFee().list().ofMinSize(0).ofMaxSize(20);
    }

    @Provide
    Arbitrary<List<VoiceUsage>> voiceUsages() {
        return voiceUsage().list().ofMinSize(0).ofMaxSize(20);
    }

    @Provide
    Arbitrary<List<DataUsage>> dataUsages() {
        return dataUsage().list().ofMinSize(0).ofMaxSize(20);
    }

    private Arbitrary<InstallmentHistory> installmentHistory() {
        Arbitrary<String> contractIds = Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(15);
        Arbitrary<String> installmentIds = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10);
        Arbitrary<BigDecimal> amounts = Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("100000")).ofScale(2);
        Arbitrary<Integer> current = Arbitraries.integers().between(1, 36);
        Arbitrary<Integer> total = Arbitraries.integers().between(1, 36);

        return Combinators.combine(contractIds, installmentIds, amounts, current, total)
                .as((cid, iid, amt, cur, tot) -> InstallmentHistory.builder()
                        .contractId(cid).installmentId(iid)
                        .installmentAmount(amt).currentInstallment(cur).totalInstallments(tot)
                        .build());
    }

    private Arbitrary<PenaltyFee> penaltyFee() {
        Arbitrary<String> contractIds = Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(15);
        Arbitrary<String> penaltyIds = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10);
        Arbitrary<BigDecimal> amounts = Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("50000")).ofScale(2);
        Arbitrary<String> reasons = Arbitraries.of("EARLY_TERMINATION", "CONTRACT_VIOLATION", "DEVICE_DAMAGE");

        return Combinators.combine(contractIds, penaltyIds, amounts, reasons)
                .as((cid, pid, amt, reason) -> PenaltyFee.builder()
                        .contractId(cid).penaltyId(pid)
                        .penaltyAmount(amt).penaltyReason(reason)
                        .build());
    }

    private Arbitrary<VoiceUsage> voiceUsage() {
        Arbitrary<String> contractIds = Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(15);
        Arbitrary<String> usageIds = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10);
        Arbitrary<BigDecimal> durations = Arbitraries.bigDecimals()
                .between(BigDecimal.ONE, new BigDecimal("3600")).ofScale(0);
        Arbitrary<BigDecimal> unitPrices = Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("100")).ofScale(2);

        return Combinators.combine(contractIds, usageIds, durations, unitPrices)
                .as((cid, uid, dur, price) -> VoiceUsage.builder()
                        .contractId(cid).usageId(uid)
                        .duration(dur).unitPrice(price)
                        .build());
    }

    private Arbitrary<DataUsage> dataUsage() {
        Arbitrary<String> contractIds = Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(15);
        Arbitrary<String> usageIds = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10);
        Arbitrary<BigDecimal> volumes = Arbitraries.bigDecimals()
                .between(BigDecimal.ONE, new BigDecimal("10240")).ofScale(2);
        Arbitrary<BigDecimal> unitPrices = Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("50")).ofScale(2);

        return Combinators.combine(contractIds, usageIds, volumes, unitPrices)
                .as((cid, uid, vol, price) -> DataUsage.builder()
                        .contractId(cid).usageId(uid)
                        .dataVolume(vol).unitPrice(price)
                        .build());
    }
}
