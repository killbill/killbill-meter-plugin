/*
 * Copyright 2010-2013 Ning, Inc.
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

import com.ning.billing.meter.MeterTenantContext;

public class MeterInternalTenantContext extends MeterTenantContext {

    public static final long INTERNAL_TENANT_RECORD_ID = 0L;

    private final Long accountRecordId;
    private final Long tenantRecordId;

    public MeterInternalTenantContext() {
        this(0L, INTERNAL_TENANT_RECORD_ID);
    }

    public MeterInternalTenantContext(final Long accountRecordId, final Long tenantRecordId) {
        this.accountRecordId = accountRecordId;
        this.tenantRecordId = tenantRecordId;
    }

    public Long getAccountRecordId() {
        return accountRecordId;
    }

    public Long getTenantRecordId() {
        return tenantRecordId;
    }
}
