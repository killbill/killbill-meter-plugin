/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.billing.meter;

import javax.inject.Inject;

import com.ning.billing.meter.timeline.BackgroundDBChunkWriter;
import com.ning.billing.meter.timeline.TimelineEventHandler;
import com.ning.billing.meter.timeline.aggregator.TimelineAggregator;

public class MeterService {

    private final BackgroundDBChunkWriter backgroundDBChunkWriter;
    private final TimelineEventHandler timelineEventHandler;
    private final TimelineAggregator timelineAggregator;
    private final MeterConfig config;

    @Inject
    public MeterService(final BackgroundDBChunkWriter backgroundDBChunkWriter, final TimelineEventHandler timelineEventHandler, final TimelineAggregator timelineAggregator, final MeterConfig config) {
        this.backgroundDBChunkWriter = backgroundDBChunkWriter;
        this.timelineEventHandler = timelineEventHandler;
        this.timelineAggregator = timelineAggregator;
        this.config = config;
    }

    public void start() {
        // Replay any log files that might not have been committed in the db-- should only occur if we crashed previously
        timelineEventHandler.replay(config.getSpoolDir(), new MeterCallContext());
        // Start the aggregation thread, if enabled
        if (config.getTimelineAggregationEnabled()) {
            timelineAggregator.runAggregationThread();
        }
        // Start the backgroundDBChunkWriter thread
        backgroundDBChunkWriter.runBackgroundWriteThread();
        // Start the purger thread to delete old log files
        timelineEventHandler.startPurgeThread();

    }

    public void stop() {
        // Stop the aggregation thread
        timelineAggregator.stopAggregationThread();
        // . Depending on shutdown mode, commit in memory timeline accumulators
        // . Will flush current direct buffer
        // . Will stop the backgroundDBChunkWriter thread
        // . Will stop the purger thread
        timelineEventHandler.commitAndShutdown(new MeterCallContext());
    }
}
