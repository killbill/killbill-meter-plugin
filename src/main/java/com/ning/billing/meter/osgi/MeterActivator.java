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

package com.ning.billing.meter.osgi;

import org.osgi.framework.BundleContext;

import com.ning.billing.meter.MeterService;
import com.ning.billing.meter.glue.MeterModule;
import com.ning.killbill.osgi.libs.killbill.KillbillActivatorBase;
import com.ning.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

public class MeterActivator extends KillbillActivatorBase {

    private MeterService meterService = null;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final Injector injector = Guice.createInjector(Stage.PRODUCTION, new MeterModule(dataSource.getDataSource()));
        meterService = (MeterService) injector.getInstance(MeterService.class);
        meterService.start();
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);

        if (meterService != null) {
            meterService.stop();
        }
    }

    @Override
    public OSGIKillbillEventHandler getOSGIKillbillEventHandler() {
        return null;
    }
}
