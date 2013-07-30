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

package com.ning.billing.meter.timeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;

import com.ning.billing.meter.timeline.categories.CategoryRecordIdAndMetric;
import com.ning.billing.meter.timeline.chunks.TimelineChunk;
import com.ning.billing.meter.timeline.consumer.TimelineChunkConsumer;
import com.ning.billing.meter.timeline.persistent.TimelineDao;
import com.ning.billing.meter.timeline.shutdown.StartTimes;
import com.ning.billing.util.callcontext.CallContext;
import com.ning.billing.util.callcontext.TenantContext;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public final class MockTimelineDao implements TimelineDao {

    private final BiMap<Integer, String> hosts = HashBiMap.create();
    private final BiMap<Integer, CategoryRecordIdAndMetric> sampleKinds = HashBiMap.create();
    private final BiMap<Integer, String> eventCategories = HashBiMap.create();
    private final BiMap<Integer, TimelineChunk> timelineChunks = HashBiMap.create();
    private final Map<Integer, Map<Integer, List<TimelineChunk>>> samplesPerHostAndSampleKind = new HashMap<Integer, Map<Integer, List<TimelineChunk>>>();
    private final AtomicReference<StartTimes> lastStartTimes = new AtomicReference<StartTimes>();

    @Override
    public Integer getSourceId(final String host, final TenantContext context) throws UnableToObtainConnectionException, CallbackFailedException {
        synchronized (hosts) {
            return hosts.inverse().get(host);
        }
    }

    @Override
    public String getSource(final Integer hostId, final TenantContext context) throws UnableToObtainConnectionException, CallbackFailedException {
        synchronized (hosts) {
            return hosts.get(hostId);
        }
    }

    @Override
    public BiMap<Integer, String> getSources(final TenantContext context) {
        return hosts;
    }

    @Override
    public int getOrAddSource(final String host, final CallContext context) throws UnableToObtainConnectionException, CallbackFailedException {
        synchronized (hosts) {
            final Integer hostId = getSourceId(host, context);
            if (hostId == null) {
                hosts.put(hosts.size() + 1, host);
                return hosts.size();
            } else {
                return hostId;
            }
        }
    }

    @Override
    public Integer getEventCategoryId(final String eventCategory, final TenantContext context) throws UnableToObtainConnectionException, CallbackFailedException {
        synchronized (eventCategories) {
            return eventCategories.inverse().get(eventCategory);
        }
    }

    @Override
    public String getEventCategory(final Integer eventCategoryId, final TenantContext context) throws UnableToObtainConnectionException, CallbackFailedException {
        synchronized (eventCategories) {
            return eventCategories.get(eventCategoryId);
        }
    }

    @Override
    public int getOrAddEventCategory(final String eventCategory, final CallContext context) throws UnableToObtainConnectionException, CallbackFailedException {
        synchronized (eventCategories) {
            Integer eventCategoryId = getEventCategoryId(eventCategory, context);
            if (eventCategoryId == null) {
                eventCategoryId = eventCategories.size() + 1;
                eventCategories.put(eventCategoryId, eventCategory);
            }

            return eventCategoryId;
        }
    }

    @Override
    public BiMap<Integer, String> getEventCategories(final TenantContext context) throws UnableToObtainConnectionException, CallbackFailedException {
        return eventCategories;
    }

    @Override
    public Integer getMetricId(final int eventCategoryId, final String sampleKind, final TenantContext context) throws UnableToObtainConnectionException, CallbackFailedException {
        synchronized (sampleKinds) {
            return sampleKinds.inverse().get(new CategoryRecordIdAndMetric(eventCategoryId, sampleKind));
        }
    }

    @Override
    public CategoryRecordIdAndMetric getCategoryIdAndMetric(final Integer sampleKindId, final TenantContext context) throws UnableToObtainConnectionException, CallbackFailedException {
        synchronized (sampleKinds) {
            return sampleKinds.get(sampleKindId);
        }
    }

    @Override
    public BiMap<Integer, CategoryRecordIdAndMetric> getMetrics(final TenantContext context) {
        synchronized (sampleKinds) {
            return sampleKinds;
        }
    }

    @Override
    public int getOrAddMetric(final Integer eventCategoryId, final String sampleKind, final CallContext context) throws UnableToObtainConnectionException, CallbackFailedException {
        synchronized (sampleKinds) {
            Integer sampleKindId = getMetricId(eventCategoryId, sampleKind, context);
            if (sampleKindId == null) {
                sampleKindId = sampleKinds.size() + 1;
                sampleKinds.put(sampleKindId, new CategoryRecordIdAndMetric(eventCategoryId, sampleKind));
            }
            return sampleKindId;
        }
    }

    @Override
    public Long insertTimelineChunk(final TimelineChunk chunk, final CallContext context) {
        final Long timelineChunkId;
        synchronized (timelineChunks) {
            timelineChunks.put(timelineChunks.size(), chunk);
            timelineChunkId = (long) timelineChunks.size() - 1;
        }

        synchronized (samplesPerHostAndSampleKind) {
            Map<Integer, List<TimelineChunk>> samplesPerSampleKind = samplesPerHostAndSampleKind.get(chunk.getSourceId());
            if (samplesPerSampleKind == null) {
                samplesPerSampleKind = new HashMap<Integer, List<TimelineChunk>>();
            }

            List<TimelineChunk> chunkAndTimes = samplesPerSampleKind.get(chunk.getMetricId());
            if (chunkAndTimes == null) {
                chunkAndTimes = new ArrayList<TimelineChunk>();
            }

            chunkAndTimes.add(chunk);
            samplesPerSampleKind.put(chunk.getMetricId(), chunkAndTimes);

            samplesPerHostAndSampleKind.put(chunk.getSourceId(), samplesPerSampleKind);
        }

        return timelineChunkId;
    }

    @Override
    public void getSamplesBySourceIdsAndMetricIds(final List<Integer> hostIds, @Nullable final List<Integer> sampleKindIds,
                                                  final DateTime startTime, final DateTime endTime,
                                                  final TimelineChunkConsumer chunkConsumer, final TenantContext context) throws UnableToObtainConnectionException, CallbackFailedException {
        for (final Integer hostId : samplesPerHostAndSampleKind.keySet()) {
            if (hostIds.indexOf(hostId) == -1) {
                continue;
            }

            final Map<Integer, List<TimelineChunk>> samplesPerSampleKind = samplesPerHostAndSampleKind.get(hostId);
            for (final Integer sampleKindId : samplesPerSampleKind.keySet()) {
                if (sampleKindIds != null && sampleKindIds.indexOf(sampleKindId) == -1) {
                    continue;
                }

                for (final TimelineChunk chunk : samplesPerSampleKind.get(sampleKindId)) {
                    if (chunk.getStartTime().isAfter(endTime) || chunk.getEndTime().isBefore(startTime)) {
                        continue;
                    }

                    chunkConsumer.processTimelineChunk(chunk);
                }
            }
        }
    }

    @Override
    public StartTimes getLastStartTimes(final TenantContext context) {
        return lastStartTimes.get();
    }

    @Override
    public Integer insertLastStartTimes(final StartTimes startTimes, final CallContext context) {
        lastStartTimes.set(startTimes);
        return 1;
    }

    @Override
    public void deleteLastStartTimes(final CallContext context) {
        lastStartTimes.set(null);
    }

    @Override
    public void test(final TenantContext context) throws UnableToObtainConnectionException, CallbackFailedException {
    }

    public BiMap<Integer, TimelineChunk> getTimelineChunks() {
        return timelineChunks;
    }

    @Override
    public void bulkInsertTimelineChunks(final List<TimelineChunk> timelineChunkList, final CallContext context) {
        for (final TimelineChunk chunk : timelineChunkList) {
            insertTimelineChunk(chunk, context);
        }
    }
}
