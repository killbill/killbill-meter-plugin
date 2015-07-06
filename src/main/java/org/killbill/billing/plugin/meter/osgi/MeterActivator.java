/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014 The Billing Project, LLC
 * Copyright 2015 Trifon Trifonov
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

package org.killbill.billing.plugin.meter.osgi;

import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.plugin.meter.MeterService;
import org.killbill.billing.plugin.meter.MeterServlet;
import org.killbill.billing.plugin.meter.api.user.MeterUserApi;
import org.killbill.billing.plugin.meter.glue.MeterModule;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.killbill.killbill.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

public class MeterActivator extends KillbillActivatorBase {

	public static final String PLUGIN_NAME = "killbill-meter";

	private MeterService meterService = null;

	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);

		logService.log(LogService.LOG_INFO, "Starting " + PLUGIN_NAME);

		final Injector injector = Guice.createInjector(Stage.PRODUCTION, new MeterModule(dataSource.getDataSource()));
		meterService = (MeterService) injector.getInstance(MeterService.class);
		meterService.start();

		final Clock clock = new DefaultClock();

		MeterUserApi meterUserApi = null;
		meterUserApi = (MeterUserApi) injector.getInstance(MeterUserApi.class);
		
		// Register plug-in servlet
		// http://localhost:8080/plugins/killbill-meter
		final MeterServlet meterServlet = new MeterServlet(logService, meterUserApi, clock);
		registerServlet(context, meterServlet);
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

    private void registerServlet(final BundleContext context, final HttpServlet servlet) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Servlet.class, servlet, props);
    }
}