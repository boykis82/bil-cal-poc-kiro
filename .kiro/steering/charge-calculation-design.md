# subscription data load 로직 수정
## 문제점
현재는 요금계산에 필요한 모든 데이터를 outer join으로 계속 붙여나가서 한 번의 쿼리로 가져오고 있다.
현실 비즈니스는 이처럼 간단하지 않다. 요금항목 종류가 매우 많아서 수십 개의 테이블을 outer join으로 걸어야 한다. record가 중복으로 추출될 수도 있다.

## 개선사항
- 가장 바깥 sql에서는 요금 계산 대상 가입자 정보만 읽는다. 즉, 계약ID와 최소한의 정보만 가져오는 것이다. 여러 건의 계약id가 입력으로 들어왔으면 이를 모두 sql의 in 조건으로 넣는다. (최대 1000건. 이하 chunk로 칭함)
- 이후에는 각 요금항목별로 load sql을 별도로 작성한다. 다만, db와의 round trip을 최소화하기 위해 chunk단위로 데이터를 가져오게 구현한다.
- 월정액 데이터를 load할때는 상품가입정보, 정지이력을 chunk단위로 한번에 가져온다.
- 통화료, 종량료, 일회성요금은 여러가지 유형이 있다. table도 다르고 비즈니스 로직을 처리하기 위해 가져와야 하는 값도 다르다. 하지만 새로운 종량료가 추가되어도 기존 로직이 수정되면 안되므로 generic을 활용한다. 혹시 더 좋은 방법이 있다면 그 방법을 제안해도 된다. 즉, 한 step 내에서 여러 개의 계산 로직이 호출되는 형태가 되어야 한다.
```
// pseudo code

public interface OneTimeChargeDomain {
    // market interface
}

// 할부이력
public class InstallmentHistory implements OneTimeChargeDomain {
    // 할부이력에 특화된 데이터
}

// data loader
public interface OneTimeChargeDataLoader<T> {
    List<T> loadData(List<ContractInfo>)
}

public class InstallmentHistoryLoader implements OneTimeChargeDataLoader<InstallmentHistory> {
    List<InstallmentHistory> loadData(List<ContractInfo>) {
        // 할부이력을 읽기 위한 mapper 호출
    }
}
```

