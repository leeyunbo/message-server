package com.readtimeout.infrastructure.adapter.external;

import com.readtimeout.core.domain.port.outbound.ExternalApiPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnExpression("'${server.mode:non-blocking}'.equals('blocking') or '${server.mode:non-blocking}'.equals('non-blocking') or '${server.mode:non-blocking}'.equals('virtual')")
public class ExternalApiAdapter implements ExternalApiPort {

    private final ExternalApiSimulator simulator;
    private final Timer apiTimer;

    public ExternalApiAdapter(ExternalApiSimulator simulator, MeterRegistry meterRegistry) {
        this.simulator = simulator;
        this.apiTimer = Timer.builder("external.api.duration")
                .description("External API call duration")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    @Override
    public void validate(String messageId) {
        apiTimer.record(() -> simulator.callExternalApiBlocking(messageId));
    }
}
