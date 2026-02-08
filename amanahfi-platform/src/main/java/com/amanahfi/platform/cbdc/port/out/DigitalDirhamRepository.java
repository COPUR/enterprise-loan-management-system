package com.amanahfi.platform.cbdc.port.out;

import com.amanahfi.platform.cbdc.domain.DigitalDirham;
import com.amanahfi.platform.cbdc.domain.DigitalDirhamId;
import com.amanahfi.platform.cbdc.domain.WalletType;
import com.amanahfi.platform.shared.domain.Money;

import java.util.List;
import java.util.Optional;

/**
 * Output port for Digital Dirham persistence
 */
public interface DigitalDirhamRepository {
    
    void save(DigitalDirham digitalDirham);
    
    Optional<DigitalDirham> findById(DigitalDirhamId digitalDirhamId);
    
    Optional<DigitalDirham> findByWalletId(String walletId);
    
    List<DigitalDirham> findByOwnerId(String ownerId);
    
    List<DigitalDirham> findByWalletType(WalletType walletType);
    
    boolean existsByOwnerIdAndWalletType(String ownerId, WalletType walletType);
    
    Money getTotalSupply();
    
    List<DigitalDirham> findActiveWallets();
    
    List<DigitalDirham> findFrozenWallets();
}