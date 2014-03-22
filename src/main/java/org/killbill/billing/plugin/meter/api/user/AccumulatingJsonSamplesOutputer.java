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

package org.killbill.billing.plugin.meter.api.user;

import java.io.IOException;
import java.util.Collection;

import org.killbill.billing.plugin.meter.api.TimeAggregationMode;
import org.killbill.billing.plugin.meter.timeline.TimelineEventHandler;
import org.killbill.billing.plugin.meter.timeline.categories.CategoryRecordIdAndMetric;
import org.killbill.billing.plugin.meter.timeline.chunks.TimelineChunk;
import org.killbill.billing.plugin.meter.timeline.consumer.AccumulatorSampleConsumer;
import org.killbill.billing.plugin.meter.timeline.consumer.CSVConsumer;
import org.killbill.billing.plugin.meter.timeline.consumer.CSVSampleProcessor;
import org.killbill.billing.plugin.meter.timeline.metrics.SamplesForMetricAndSource;
import org.killbill.billing.plugin.meter.timeline.persistent.TimelineDao;
import org.killbill.billing.util.callcontext.TenantContext;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.Strings;

public class AccumulatingJsonSamplesOutputer extends JsonSamplesOutputer {

    private final AccumulatorSampleConsumer accumulatorSampleConsumer;

    private String lastSource;
    private String lastEventCategory;
    private String lastMetric;

    public AccumulatingJsonSamplesOutputer(final TimeAggregationMode timeAggregationMode, final TimelineEventHandler timelineEventHandler,
                                           final TimelineDao timelineDao, final TenantContext context) {
        super(timelineEventHandler, timelineDao, context);
        this.accumulatorSampleConsumer = new AccumulatorSampleConsumer(timeAggregationMode, new CSVSampleProcessor());
    }

    @Override
    protected void writeJsonForChunks(final JsonGenerator generator, final Collection<? extends TimelineChunk> chunksForSourceAndMetric) throws IOException {
        for (final TimelineChunk chunk : chunksForSourceAndMetric) {
            final String source = timelineDao.getSource(chunk.getSourceId(), context);
            final CategoryRecordIdAndMetric categoryIdAndMetric = timelineDao.getCategoryIdAndMetric(chunk.getMetricId(), context);
            final String eventCategory = timelineDao.getEventCategory(categoryIdAndMetric.getEventCategoryId(), context);
            final String metric = categoryIdAndMetric.getMetric();

            final String samples = CSVConsumer.getSamplesAsCSV(sampleCoder, chunk, accumulatorSampleConsumer);

            // Don't write out empty samples
            if (!Strings.isNullOrEmpty(samples)) {
                generator.writeObject(new SamplesForMetricAndSource(source, eventCategory, metric, samples));
            }

            lastSource = source;
            lastEventCategory = eventCategory;
            lastMetric = metric;
        }
    }

    @Override
    protected void writeRemainingData(final JsonGenerator generator) throws IOException {
        final String samples = accumulatorSampleConsumer.flush();
        // Don't write out empty samples
        if (!Strings.isNullOrEmpty(samples)) {
            generator.writeObject(new SamplesForMetricAndSource(lastSource, lastEventCategory, lastMetric, samples));
        }
    }
}
