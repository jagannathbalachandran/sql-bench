package io.sqlbench.analysis;

import io.sqlbench.model.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DegradationAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(DegradationAnalyzer.class);
    private static final double DEFAULT_THRESHOLD_PERCENT = 15.0;

    private final double thresholdPercent;

    public DegradationAnalyzer() { this(DEFAULT_THRESHOLD_PERCENT); }

    public DegradationAnalyzer(double thresholdPercent) {
        this.thresholdPercent = thresholdPercent;
    }

    public List<DegradationResult> analyze(List<QueryResult> currentResults, Map<String, Long> baseline) {
        List<DegradationResult> regressions = new ArrayList<>();

        for (QueryResult r : currentResults) {
            if (!r.isSuccess()) continue;
            Long base = baseline.get(r.getQueryAlias());
            if (base == null) continue;

            DegradationResult dr = new DegradationResult(r.getQueryAlias(), base, r.getTotalClientTimeMs());
            if (dr.isRegression(thresholdPercent)) {
                regressions.add(dr);
                LOG.warn("REGRESSION: {} — {}ms vs baseline {}ms ({:+.1f}%)",
                        r.getQueryAlias(), r.getTotalClientTimeMs(), base, dr.getChangePercent());
            }
        }

        LOG.info("Degradation analysis: {} regressions out of {} queries (threshold: {}%)",
                regressions.size(), currentResults.size(), thresholdPercent);
        return regressions;
    }
}
