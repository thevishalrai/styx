/*
  Copyright (C) 2013-2021 Expedia Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package com.hotels.styx.client;

import com.hotels.styx.api.extension.Origin;
import com.hotels.styx.client.applications.OriginStats;
import com.hotels.styx.client.applications.metrics.OriginMetrics;
import com.hotels.styx.common.SimpleCache;
import com.hotels.styx.metrics.CentralisedMetrics;

import static java.util.Objects.requireNonNull;


/**
 * A factory that creates {@link OriginStats} instances using a metric registry it wraps. If an {@link OriginStats} already
 * exists for an origin, the same instance will be returned again.
 */

public interface OriginStatsFactory {
    OriginStats originStats(Origin origin);

    /**
     * A caching OriginStatsFactory. A newly created OriginStats object is cached,
     * and the cached copy is returned for future invocations.
     */
    class CachingOriginStatsFactory implements OriginStatsFactory {
        private final SimpleCache<Origin, OriginMetrics> metricsByOrigin;

        /**
         * Constructs a new instance.
         *
         * @param metrics centralised meter registry
         */
        public CachingOriginStatsFactory(CentralisedMetrics metrics) {
            requireNonNull(metrics);
            this.metricsByOrigin = new SimpleCache<>(origin -> new OriginMetrics(metrics, origin));
        }

        /**
         * Construct a new {@link OriginStats} for an origin, or return a previously created one if it exists.
         *
         * @param origin origin to collect stats for
         * @return the {@link OriginStats}
         */
        public OriginStats originStats(Origin origin) {
            return metricsByOrigin.get(origin);
        }
    }
}
