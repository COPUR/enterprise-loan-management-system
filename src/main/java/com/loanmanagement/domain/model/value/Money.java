// REMOVED: This class violates DDD shared kernel principles
// Use com.loanmanagement.sharedkernel.domain.value.Money instead

package com.loanmanagement.domain.model.value;

/**
 * @deprecated This class has been removed to maintain DDD shared kernel consistency.
 * All monetary operations should use: com.loanmanagement.sharedkernel.domain.value.Money
 *
 * Migration Guide:
 * - Replace all imports of this class with com.loanmanagement.sharedkernel.domain.value.Money
 * - Use Money.of(BigDecimal) factory method instead of constructor
 * - All arithmetic operations are available in the shared kernel Money class
 */
@Deprecated(forRemoval = true)
public final class Money {
    private Money() {
        throw new UnsupportedOperationException(
            "This class is deprecated. Use com.loanmanagement.sharedkernel.domain.value.Money instead"
        );
    }
}