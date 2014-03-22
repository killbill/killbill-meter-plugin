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
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.joda.time.DateTime;
import org.killbill.billing.plugin.meter.api.DecimationMode;
import org.killbill.billing.plugin.meter.api.TimeAggregationMode;
import org.killbill.billing.plugin.meter.timeline.TimelineEventHandler;
import org.killbill.billing.plugin.meter.timeline.persistent.TimelineDao;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class DefaultMeterUserApi implements MeterUserApi {

    private static final String AGGREGATE_METRIC_NAME = "__AGGREGATE__";

    private final TimelineEventHandler timelineEventHandler;
    private final TimelineDao timelineDao;

    @Inject
    public DefaultMeterUserApi(final TimelineEventHandler timelineEventHandler,
                               final TimelineDao timelineDao) {
        this.timelineEventHandler = timelineEventHandler;
        this.timelineDao = timelineDao;
    }

    @Override
    public void getUsage(final OutputStream outputStream, final TimeAggregationMode timeAggregationMode,
                         final String source, final Collection<String> categories,
                         final DateTime fromTimestamp, final DateTime toTimestamp, final TenantContext context) throws IOException {
        final ImmutableMap.Builder<String, Collection<String>> metricsPerCategory = new Builder<String, Collection<String>>();
        for (final String category : categories) {
            metricsPerCategory.put(category, ImmutableList.<String>of(AGGREGATE_METRIC_NAME));
        }

        getUsage(outputStream, timeAggregationMode, source, metricsPerCategory.build(), fromTimestamp, toTimestamp, context);
    }

    @Override
    public void getUsage(final OutputStream outputStream, final TimeAggregationMode timeAggregationMode,
                         final String source, final Map<String, Collection<String>> metricsPerCategory,
                         final DateTime fromTimestamp, final DateTime toTimestamp, final TenantContext context) throws IOException {
        final JsonSamplesOutputer outputerJson = new AccumulatingJsonSamplesOutputer(timeAggregationMode, timelineEventHandler, timelineDao, context);
        outputerJson.output(outputStream, ImmutableList.<String>of(source), metricsPerCategory, fromTimestamp, toTimestamp);
    }

    @Override
    public void getUsage(final OutputStream outputStream, final DecimationMode decimationMode, @Nullable final Integer outputCount,
                         final String source, final Map<String, Collection<String>> metricsPerCategory,
                         final DateTime fromTimestamp, final DateTime toTimestamp, final TenantContext context) throws IOException {
        final JsonSamplesOutputer outputerJson = new DecimatingJsonSamplesOutputer(decimationMode, outputCount, timelineEventHandler, timelineDao, context);
        outputerJson.output(outputStream, ImmutableList.<String>of(source), metricsPerCategory, fromTimestamp, toTimestamp);
    }

    @Override
    public void getUsage(final OutputStream outputStream, final String source, final Collection<String> categories,
                         final DateTime fromTimestamp, final DateTime toTimestamp, final TenantContext context) throws IOException {
        final ImmutableMap.Builder<String, Collection<String>> metricsPerCategory = new Builder<String, Collection<String>>();
        for (final String category : categories) {
            metricsPerCategory.put(category, ImmutableList.<String>of(AGGREGATE_METRIC_NAME));
        }

        getUsage(outputStream, source, metricsPerCategory.build(), fromTimestamp, toTimestamp, context);
    }

    @Override
    public void getUsage(final OutputStream outputStream, final String source, final Map<String, Collection<String>> metricsPerCategory,
                         final DateTime fromTimestamp, final DateTime toTimestamp, final TenantContext context) throws IOException {
        final JsonSamplesOutputer outputerJson = new DefaultJsonSamplesOutputer(timelineEventHandler, timelineDao, context);
        outputerJson.output(outputStream, ImmutableList.<String>of(source), metricsPerCategory, fromTimestamp, toTimestamp);
    }

    @Override
    public void incrementUsage(final String source, final String categoryName, final String metricName,
                               final DateTime timestamp, final CallContext context) {
        recordUsage(source,
                    ImmutableMap.<String, Map<String, Object>>of(categoryName, ImmutableMap.<String, Object>of(metricName, (short) 1)),
                    timestamp,
                    context);
    }

    @Override
    public void incrementUsageAndAggregate(final String source, final String categoryName, final String metricName,
                                           final DateTime timestamp, final CallContext context) {
        recordUsage(source,
                    ImmutableMap.<String, Map<String, Object>>of(categoryName, ImmutableMap.<String, Object>of(metricName, (short) 1, AGGREGATE_METRIC_NAME, (short) 1)),
                    timestamp,
                    context);
    }

    @Override
    public void recordUsage(final String source, final Map<String, Map<String, Object>> samplesForCategoriesAndMetrics,
                            final DateTime timestamp, final CallContext context) {
        for (final String category : samplesForCategoriesAndMetrics.keySet()) {
            timelineEventHandler.record(source, category, timestamp, samplesForCategoriesAndMetrics.get(category), context);
        }
    }
}
