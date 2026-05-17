package cn.jingyier.nail.gateway.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AnomalyMonitor {

    private static final Logger log = LoggerFactory.getLogger(AnomalyMonitor.class);
    private final MeterRegistry meterRegistry;
    private long lastErrorCount = 0;

    public AnomalyMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelay = 30000)
    public void checkMetrics() {
        double currentErrors = meterRegistry.counter("gateway.requests.errors",
                "app", "nailong-gateway").count();
        double currentTotal = meterRegistry.counter("gateway.requests.total",
                "app", "nailong-gateway").count();

        if (currentTotal > 10) {
            long recentErrors = (long) (currentErrors - lastErrorCount);
            if (recentErrors > 5) {
                log.warn("[GATEWAY-ALERT] High error rate detected: {} errors in last 30 seconds", recentErrors);
            }
        }

        lastErrorCount = (long) currentErrors;
    }
}
