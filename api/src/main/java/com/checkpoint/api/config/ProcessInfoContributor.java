package com.checkpoint.api.config;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.util.Map;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * Adds the JVM start instant to the actuator info endpoint, as
 * {@code process.start} in ISO-8601.
 *
 * <p>The CD pipeline needs to prove that a deployment actually replaced the running
 * process. {@code build.time} cannot answer that on its own: the image is built from
 * a Dockerfile whose layers are cached on the source tree, so redeploying a commit
 * that left {@code api/} untouched rebuilds to a byte-identical image carrying the
 * same {@code build.time}. The deployment is genuine, the container is new, and a
 * check on the build stamp alone would wait for a timestamp that will never move.
 *
 * <p>The start instant moves on every container start regardless of caching, so the
 * two together say different and complementary things: {@code process.start} says the
 * rollout was applied, {@code build.time} says which image it applied.
 */
@Component
public class ProcessInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        builder.withDetail("process", Map.of(
                "start", Instant.ofEpochMilli(runtime.getStartTime()).toString(),
                "uptimeMs", runtime.getUptime()));
    }
}
