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

package com.ning.billing.meter;

import java.util.UUID;

import javax.annotation.Nullable;

import org.joda.time.DateTime;

import com.ning.billing.util.callcontext.CallContext;
import com.ning.billing.util.callcontext.CallOrigin;
import com.ning.billing.util.callcontext.UserType;

public class MeterCallContext extends MeterTenantContext implements CallContext {

    private final String createdBy;
    private final CallOrigin callOrigin;
    private final UserType userType;
    private final String reason;
    private final String comment;
    private final UUID userToken;
    private final DateTime createdDate;

    public MeterCallContext(@Nullable final UUID tenantId, final String createdBy, final CallOrigin callOrigin,
                            final UserType userType, final String reason, final String comment, final UUID userToken,
                            final DateTime createdDate) {
        super(tenantId);
        this.createdBy = createdBy;
        this.callOrigin = callOrigin;
        this.userType = userType;
        this.reason = reason;
        this.comment = comment;
        this.userToken = userToken;
        this.createdDate = createdDate;
    }

    public MeterCallContext(@Nullable final UUID tenantId) {
        this(tenantId, null, CallOrigin.INTERNAL, UserType.SYSTEM, null, null, UUID.randomUUID(), null);
    }

    public MeterCallContext() {
        this(null);
    }

    @Override
    public UUID getUserToken() {
        return userToken;
    }

    @Override
    public String getUserName() {
        return createdBy;
    }

    @Override
    public CallOrigin getCallOrigin() {
        return callOrigin;
    }

    @Override
    public UserType getUserType() {
        return userType;
    }

    @Override
    public String getReasonCode() {
        return reason;
    }

    @Override
    public String getComments() {
        return comment;
    }

    @Override
    public DateTime getCreatedDate() {
        return createdDate;
    }

    @Override
    public DateTime getUpdatedDate() {
        return createdDate;
    }
}
