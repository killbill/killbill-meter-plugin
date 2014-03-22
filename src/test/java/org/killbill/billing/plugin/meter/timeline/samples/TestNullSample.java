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

package org.killbill.billing.plugin.meter.timeline.samples;

import org.killbill.billing.plugin.meter.MeterTestSuiteNoDB;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestNullSample extends MeterTestSuiteNoDB {

    @Test(groups = "fast")
    public void testConstructor() throws Exception {
        final NullSample sample = new NullSample();

        Assert.assertEquals(sample.getOpcode(), SampleOpcode.NULL);
        Assert.assertNull(sample.getSampleValue());
    }
}
