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

package com.ning.billing.meter.timeline.persistent;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.skife.jdbi.v2.DefaultMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;

import com.ning.billing.meter.timeline.MeterInternalCallContext;
import com.ning.billing.meter.timeline.MeterInternalTenantContext;
import com.ning.billing.meter.timeline.categories.CategoryIdAndMetricMapper;
import com.ning.billing.meter.timeline.categories.CategoryRecordIdAndMetric;
import com.ning.billing.meter.timeline.chunks.TimelineChunk;
import com.ning.billing.meter.timeline.chunks.TimelineChunkBinder;
import com.ning.billing.meter.timeline.shutdown.StartTimes;
import com.ning.billing.meter.timeline.shutdown.StartTimesBinder;
import com.ning.billing.meter.timeline.shutdown.StartTimesMapper;
import com.ning.billing.meter.timeline.sources.SourceIdAndMetricIdMapper;

@UseStringTemplate3StatementLocator()
@RegisterMapper({CategoryIdAndMetricMapper.class, StartTimesMapper.class, SourceIdAndMetricIdMapper.class, DefaultMapper.class})
public interface TimelineSqlDao extends Transactional<TimelineSqlDao> {

    @SqlQuery
    Integer getSourceRecordId(@Bind("source") final String source,
                              @BindBean final MeterInternalTenantContext context);

    @SqlQuery
    String getSourceName(@Bind("recordId") final Integer sourceRecordId,
                         @BindBean final MeterInternalTenantContext context);

    @SqlQuery
    List<Map<String, Object>> getSources(@BindBean final MeterInternalTenantContext context);

    @SqlUpdate
    void addSource(@Bind("source") final String source,
                   @BindBean final MeterInternalCallContext context);

    @SqlQuery
    Integer getCategoryRecordId(@Bind("category") final String category,
                                @BindBean final MeterInternalTenantContext context);

    @SqlQuery
    String getCategory(@Bind("recordId") final Integer categoryRecordId,
                       @BindBean final MeterInternalTenantContext context);

    @SqlQuery
    List<Map<String, Object>> getCategories(@BindBean final MeterInternalTenantContext context);

    @SqlUpdate
    void addCategory(@Bind("category") final String category,
                     @BindBean final MeterInternalCallContext context);

    @SqlQuery
    Integer getMetricRecordId(@Bind("categoryRecordId") final int categoryRecordId,
                              @Bind("metric") final String metric,
                              @BindBean final MeterInternalTenantContext context);

    @SqlQuery
    Iterable<Integer> getMetricRecordIdsBySourceRecordId(@Bind("sourceRecordId") final Integer sourceRecordId,
                                                         @BindBean final MeterInternalTenantContext context);

    @SqlQuery
    CategoryRecordIdAndMetric getCategoryRecordIdAndMetric(@Bind("recordId") final Integer metricRecordId,
                                                           @BindBean final MeterInternalTenantContext context);

    @SqlQuery
    String getMetric(@Bind("recordId") final Integer metricRecordId,
                     @BindBean final MeterInternalTenantContext context);

    @SqlQuery
    List<Map<String, Object>> getMetrics(@BindBean final MeterInternalTenantContext context);

    @SqlUpdate
    void addMetric(@Bind("categoryRecordId") final int categoryRecordId,
                   @Bind("metric") final String metric,
                   @BindBean final MeterInternalCallContext context);

    @SqlQuery
    int getLastInsertedRecordId(@BindBean final MeterInternalTenantContext context);

    @SqlUpdate
    void insertTimelineChunk(@TimelineChunkBinder final TimelineChunk timelineChunk,
                             @BindBean final MeterInternalCallContext context);

    @SqlBatch
    @BatchChunkSize(1000)
    void bulkInsertTimelineChunks(@TimelineChunkBinder Iterator<TimelineChunk> chunkIterator,
                                  @BindBean final MeterInternalCallContext context);

    @SqlUpdate
    Integer insertLastStartTimes(@StartTimesBinder final StartTimes startTimes,
                                 @BindBean final MeterInternalCallContext context);

    @SqlQuery
    StartTimes getLastStartTimes(@BindBean final MeterInternalTenantContext context);

    @SqlUpdate
    void deleteLastStartTimes(@BindBean final MeterInternalCallContext context);

    @SqlUpdate
    void test(@BindBean final MeterInternalTenantContext context);
}
