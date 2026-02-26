package com.enterprise.openfinance.atmdata.application;

import com.enterprise.openfinance.atmdata.domain.model.AtmDataSettings;
import com.enterprise.openfinance.atmdata.domain.model.AtmListResult;
import com.enterprise.openfinance.atmdata.domain.model.AtmLocation;
import com.enterprise.openfinance.atmdata.domain.port.in.AtmDataUseCase;
import com.enterprise.openfinance.atmdata.domain.port.out.AtmCachePort;
import com.enterprise.openfinance.atmdata.domain.port.out.AtmDirectoryPort;
import com.enterprise.openfinance.atmdata.domain.query.GetAtmsQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AtmDataService implements AtmDataUseCase {

    private final AtmDirectoryPort directoryPort;
    private final AtmCachePort cachePort;
    private final AtmDataSettings settings;
    private final Clock clock;

    public AtmDataService(AtmDirectoryPort directoryPort,
                          AtmCachePort cachePort,
                          AtmDataSettings settings,
                          Clock clock) {
        this.directoryPort = directoryPort;
        this.cachePort = cachePort;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    public AtmListResult listAtms(GetAtmsQuery query) {
        Instant now = Instant.now(clock);
        String cacheKey = "atms:" + query.cacheKeySuffix();

        var cached = cachePort.getAtms(cacheKey, now);
        if (cached.isPresent()) {
            return cached.orElseThrow().withCacheHit(true);
        }

        List<AtmLocation> atms = directoryPort.listAtms();
        if (query.hasLocationFilter()) {
            double lat = query.normalizedLatitude().orElseThrow();
            double lon = query.normalizedLongitude().orElseThrow();
            double radius = query.normalizedRadiusKm().orElseThrow();
            atms = atms.stream()
                    .filter(atm -> atm.isWithinRadiusKm(lat, lon, radius))
                    .toList();
        }

        atms = atms.stream()
                .sorted(Comparator.comparing(AtmLocation::atmId))
                .toList();

        AtmListResult result = new AtmListResult(atms, false);
        cachePort.putAtms(cacheKey, result, now.plus(settings.cacheTtl()));
        return result;
    }
}
