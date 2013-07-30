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

package com.ning.billing.meter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.sql.SQLException;

import org.skife.jdbi.v2.IDBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import com.ning.billing.commons.embeddeddb.EmbeddedDB;
import com.ning.billing.commons.embeddeddb.h2.H2EmbeddedDB;
import com.ning.billing.commons.embeddeddb.mysql.MySQLEmbeddedDB;
import com.ning.billing.meter.glue.MeterModule;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;

public class MeterTestSuiteWithEmbeddedDB extends MeterTestSuite {

    private static final Logger log = LoggerFactory.getLogger(MeterTestSuiteWithEmbeddedDB.class);

    protected static EmbeddedDB helper;

    static {
        if ("true".equals(System.getProperty("com.ning.billing.dbi.test.h2"))) {
            log.info("Using h2 as the embedded database");
            helper = new H2EmbeddedDB();
        } else {
            log.info("Using MySQL as the embedded database");
            helper = new MySQLEmbeddedDB();
        }
    }

    public static IDBI getDBI() {
        try {
            final MeterModule meterModule = new MeterModule(helper.getDataSource());
            return meterModule.getDBI();
        } catch (IOException e) {
            Assert.fail(e.toString());
            return null;
        }
    }

    @BeforeSuite(groups = {"slow", "mysql"})
    public void startMysqlBeforeTestSuite() throws IOException, ClassNotFoundException, SQLException, URISyntaxException {
        helper.initialize();
        helper.start();

        final InputSupplier<InputStream> inputSupplier = new InputSupplier<InputStream>() {
            @Override
            public InputStream getInput() throws IOException {
                return Resources.getResource("com/ning/billing/meter/ddl.sql").openStream();
            }
        };
        final String ddl = CharStreams.toString(CharStreams.newReaderSupplier(inputSupplier, Charsets.UTF_8));
        helper.executeScript(ddl);
        helper.refreshTableNames();
    }

    @BeforeMethod(groups = {"slow", "mysql"})
    public void cleanupTablesBetweenMethods() {
        try {
            helper.cleanupAllTables();
        } catch (Exception ignored) {
        }
    }

    @AfterSuite(groups = {"slow", "mysql"})
    public void shutdownMysqlAfterTestSuite() throws IOException, ClassNotFoundException, SQLException, URISyntaxException {
        if (hasFailed()) {
            log.error("**********************************************************************************************");
            log.error("*** TESTS HAVE FAILED - LEAVING DB RUNNING FOR DEBUGGING - MAKE SURE TO KILL IT ONCE DONE ****");
            log.error(helper.getCmdLineConnectionString());
            log.error("**********************************************************************************************");
            return;
        }

        try {
            helper.stop();
        } catch (Exception ignored) {
        }
    }
}
