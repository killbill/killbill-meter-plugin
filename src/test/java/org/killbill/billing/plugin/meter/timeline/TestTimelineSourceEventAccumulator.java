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

package org.killbill.billing.plugin.meter.timeline;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.billing.plugin.meter.MeterTestSuiteNoDB;
import org.killbill.billing.plugin.meter.timeline.codec.DefaultSampleCoder;
import org.killbill.billing.plugin.meter.timeline.codec.SampleCoder;
import org.killbill.billing.plugin.meter.timeline.samples.SampleOpcode;
import org.killbill.billing.plugin.meter.timeline.samples.ScalarSample;
import org.killbill.billing.plugin.meter.timeline.sources.SourceSamplesForTimestamp;
import org.killbill.billing.plugin.meter.timeline.times.DefaultTimelineCoder;
import org.killbill.billing.plugin.meter.timeline.times.TimelineCoder;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestTimelineSourceEventAccumulator extends MeterTestSuiteNoDB {

    private static final int HOST_ID = 1;
    private static final int EVENT_CATEGORY_ID = 123;

    private static final MockTimelineDao dao = new MockTimelineDao();
    private static final TimelineCoder timelineCoder = new DefaultTimelineCoder();
    private static final SampleCoder sampleCoder = new DefaultSampleCoder();

    @Test(groups = "fast")
    public void testSimpleAggregate() throws IOException {
        final DateTime startTime = new DateTime(DateTimeZone.UTC);
        final TimelineSourceEventAccumulator accumulator = new TimelineSourceEventAccumulator(dao, timelineCoder, sampleCoder, HOST_ID,
                                                                                              EVENT_CATEGORY_ID, startTime);

        // Send a first type of data
        final int sampleCount = 5;
        final int sampleKindId = 1;
        sendData(accumulator, startTime, sampleCount, sampleKindId);
        Assert.assertEquals(accumulator.getStartTime(), startTime);
        Assert.assertEquals(accumulator.getEndTime(), startTime.plusSeconds(sampleCount - 1));
        Assert.assertEquals(accumulator.getSourceId(), HOST_ID);
        Assert.assertEquals(accumulator.getTimelines().size(), 1);
        Assert.assertEquals(accumulator.getTimelines().get(sampleKindId).getSampleCount(), sampleCount);
        Assert.assertEquals(accumulator.getTimelines().get(sampleKindId).getMetricId(), sampleKindId);

        // Send now a second type
        final DateTime secondStartTime = startTime.plusSeconds(sampleCount + 1);
        final int secondSampleCount = 15;
        final int secondSampleKindId = 2;
        sendData(accumulator, secondStartTime, secondSampleCount, secondSampleKindId);
        // We keep the start time of the accumulator
        Assert.assertEquals(accumulator.getStartTime(), startTime);
        Assert.assertEquals(accumulator.getEndTime(), secondStartTime.plusSeconds(secondSampleCount - 1));
        Assert.assertEquals(accumulator.getSourceId(), HOST_ID);
        Assert.assertEquals(accumulator.getTimelines().size(), 2);
        // We advance all timelines in parallel
        Assert.assertEquals(accumulator.getTimelines().get(sampleKindId).getSampleCount(), sampleCount + secondSampleCount);
        Assert.assertEquals(accumulator.getTimelines().get(sampleKindId).getMetricId(), sampleKindId);
        Assert.assertEquals(accumulator.getTimelines().get(secondSampleKindId).getSampleCount(), sampleCount + secondSampleCount);
        Assert.assertEquals(accumulator.getTimelines().get(secondSampleKindId).getMetricId(), secondSampleKindId);
    }

    private void sendData(final TimelineSourceEventAccumulator accumulator, final DateTime startTime, final int sampleCount, final int sampleKindId) {
        final Map<Integer, ScalarSample> samples = new HashMap<Integer, ScalarSample>();

        for (int i = 0; i < sampleCount; i++) {
            samples.put(sampleKindId, new ScalarSample<Long>(SampleOpcode.LONG, i + 1242L));
            final SourceSamplesForTimestamp hostSamplesForTimestamp = new SourceSamplesForTimestamp(HOST_ID, "JVM", startTime.plusSeconds(i), samples);
            accumulator.addSourceSamples(hostSamplesForTimestamp);
        }
    }
}
