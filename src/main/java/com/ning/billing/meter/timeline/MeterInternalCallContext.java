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

import com.ning.billing.meter.MeterCallContext;
import com.ning.billing.util.callcontext.CallContext;

public class MeterInternalCallContext extends MeterCallContext {

    private final Long accountRecordId;
    private final Long tenantRecordId;

    public MeterInternalCallContext(final CallContext callContext, final Long accountRecordId, final Long tenantRecordId) {
        super(callContext.getTenantId(),
              callContext.getUserName(),
              callContext.getCallOrigin(),
              callContext.getUserType(),
              callContext.getReasonCode(),
              callContext.getComments(),
              callContext.getUserToken(),
              callContext.getCreatedDate());
        this.accountRecordId = accountRecordId;
        this.tenantRecordId = tenantRecordId;
    }

    public MeterInternalCallContext() {
        super(null);
        this.accountRecordId = 0L;
        this.tenantRecordId = MeterInternalTenantContext.INTERNAL_TENANT_RECORD_ID;
    }

    public Long getAccountRecordId() {
        return accountRecordId;
    }

    public Long getTenantRecordId() {
        return tenantRecordId;
    }
}
