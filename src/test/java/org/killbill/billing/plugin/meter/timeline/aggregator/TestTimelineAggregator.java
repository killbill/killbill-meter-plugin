/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014 The Billing Project, LLC
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

package org.killbill.billing.plugin.meter.timeline.aggregator;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.billing.plugin.meter.MeterConfig;
import org.killbill.billing.plugin.meter.MeterTestSuiteWithEmbeddedDB;
import org.killbill.billing.plugin.meter.timeline.TimelineSourceEventAccumulator;
import org.killbill.billing.plugin.meter.timeline.chunks.TimelineChunk;
import org.killbill.billing.plugin.meter.timeline.codec.DefaultSampleCoder;
import org.killbill.billing.plugin.meter.timeline.codec.SampleCoder;
import org.killbill.billing.plugin.meter.timeline.consumer.TimelineChunkConsumer;
import org.killbill.billing.plugin.meter.timeline.persistent.DefaultTimelineDao;
import org.killbill.billing.plugin.meter.timeline.persistent.TimelineDao;
import org.killbill.billing.plugin.meter.timeline.samples.SampleOpcode;
import org.killbill.billing.plugin.meter.timeline.samples.ScalarSample;
import org.killbill.billing.plugin.meter.timeline.sources.SourceSamplesForTimestamp;
import org.killbill.billing.plugin.meter.timeline.times.DefaultTimelineCoder;
import org.killbill.billing.plugin.meter.timeline.times.TimelineCoder;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class TestTimelineAggregator extends MeterTestSuiteWithEmbeddedDB {

    private static final UUID HOST_UUID = UUID.randomUUID();
    private static final String HOST_NAME = HOST_UUID.toString();
    private static final String EVENT_TYPE = "myType";
    private static final int EVENT_TYPE_ID = 123;
    private static final String MIN_HEAPUSED_KIND = "min_heapUsed";
    private static final String MAX_HEAPUSED_KIND = "max_heapUsed";
    private static final DateTime START_TIME = new DateTime(DateTimeZone.UTC);
    private static final TimelineCoder timelineCoder = new DefaultTimelineCoder();
    private static final SampleCoder sampleCoder = new DefaultSampleCoder();

    private TimelineDao timelineDao;
    private TimelineAggregator aggregator;

    private Integer hostId = null;
    private Integer minHeapUsedKindId = null;
    private Integer maxHeapUsedKindId = null;

    @BeforeMethod(groups = "mysql")
    public void setUp() throws Exception {
        timelineDao = new DefaultTimelineDao(getDBI());
        final Properties properties = System.getProperties();
        properties.put("killbill.usage.timelines.chunksToAggregate", "2,2");
        final MeterConfig config = new ConfigurationObjectFactory(properties).build(MeterConfig.class);
        aggregator = new TimelineAggregator(getDBI(), timelineDao, timelineCoder, sampleCoder, config);
    }

    @Test(groups = "mysql")
    public void testAggregation() throws Exception {
        // Create the host
        hostId = timelineDao.getOrAddSource(HOST_NAME, callContext);
        Assert.assertNotNull(hostId);
        Assert.assertEquals(timelineDao.getSources(callContext).values().size(), 1);

        // Create the sample kinds
        minHeapUsedKindId = timelineDao.getOrAddMetric(EVENT_TYPE_ID, MIN_HEAPUSED_KIND, callContext);
        Assert.assertNotNull(minHeapUsedKindId);
        maxHeapUsedKindId = timelineDao.getOrAddMetric(EVENT_TYPE_ID, MAX_HEAPUSED_KIND, callContext);
        Assert.assertNotNull(maxHeapUsedKindId);
        Assert.assertEquals(timelineDao.getMetrics(callContext).values().size(), 2);

        // Create two sets of times: T - 125 ... T - 65 ; T - 60 ... T (note the gap!)
        createAOneHourTimelineChunk(125);
        createAOneHourTimelineChunk(60);

        // Check the getSamplesByHostIdsAndSampleKindIds DAO method works as expected
        // You might want to draw timelines on a paper and remember boundaries are inclusive to understand these numbers
        checkSamplesForATimeline(185, 126, 0);
        checkSamplesForATimeline(185, 125, 2);
        checkSamplesForATimeline(64, 61, 0);
        checkSamplesForATimeline(125, 65, 2);
        checkSamplesForATimeline(60, 0, 2);
        checkSamplesForATimeline(125, 0, 4);
        checkSamplesForATimeline(124, 0, 4);
        checkSamplesForATimeline(124, 66, 2);

        aggregator.getAndProcessTimelineAggregationCandidates();

        Assert.assertEquals(timelineDao.getSources(callContext).values().size(), 1);
        Assert.assertEquals(timelineDao.getMetrics(callContext).values().size(), 2);

        // Similar than above, but we have only 2 now
        checkSamplesForATimeline(185, 126, 0);
        checkSamplesForATimeline(185, 125, 2);
        // Note, the gap is filled now
        checkSamplesForATimeline(64, 61, 2);
        checkSamplesForATimeline(125, 65, 2);
        checkSamplesForATimeline(60, 0, 2);
        checkSamplesForATimeline(125, 0, 2);
        checkSamplesForATimeline(124, 0, 2);
        checkSamplesForATimeline(124, 66, 2);
    }

    private void checkSamplesForATimeline(final Integer startTimeMinutesAgo, final Integer endTimeMinutesAgo, final long expectedChunks) throws InterruptedException {
        final AtomicLong timelineChunkSeen = new AtomicLong(0);

        timelineDao.getSamplesBySourceIdsAndMetricIds(ImmutableList.<Integer>of(hostId), ImmutableList.<Integer>of(minHeapUsedKindId, maxHeapUsedKindId),
                                                      START_TIME.minusMinutes(startTimeMinutesAgo), START_TIME.minusMinutes(endTimeMinutesAgo), new TimelineChunkConsumer() {

                    @Override
                    public void processTimelineChunk(final TimelineChunk chunk) {
                        Assert.assertEquals((Integer) chunk.getSourceId(), hostId);
                        Assert.assertTrue(chunk.getMetricId() == minHeapUsedKindId || chunk.getMetricId() == maxHeapUsedKindId);
                        timelineChunkSeen.incrementAndGet();
                    }
                }, callContext
                                                     );

        Assert.assertEquals(timelineChunkSeen.get(), expectedChunks);
    }

    private void createAOneHourTimelineChunk(final int startTimeMinutesAgo) throws IOException {
        final DateTime firstSampleTime = START_TIME.minusMinutes(startTimeMinutesAgo);
        final TimelineSourceEventAccumulator accumulator = new TimelineSourceEventAccumulator(timelineDao, timelineCoder, sampleCoder, hostId, EVENT_TYPE_ID, firstSampleTime);
        // 120 samples per hour
        for (int i = 0; i < 120; i++) {
            final DateTime eventDateTime = firstSampleTime.plusSeconds(i * 30);
            final Map<Integer, ScalarSample> event = createEvent(eventDateTime.getMillis());
            final SourceSamplesForTimestamp samples = new SourceSamplesForTimestamp(hostId, EVENT_TYPE, eventDateTime, event);
            accumulator.addSourceSamples(samples);
        }

        accumulator.extractAndQueueTimelineChunks();
    }

    private Map<Integer, ScalarSample> createEvent(final long ts) {
        return ImmutableMap.<Integer, ScalarSample>of(
                minHeapUsedKindId, new ScalarSample(SampleOpcode.LONG, Long.MIN_VALUE + ts),
                maxHeapUsedKindId, new ScalarSample(SampleOpcode.LONG, Long.MAX_VALUE - ts)
                                                     );
    }
}
