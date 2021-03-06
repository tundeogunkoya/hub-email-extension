/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.blackducksoftware.integration.email.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.blackducksoftware.integration.email.EmailEngine;
import com.blackducksoftware.integration.email.mock.MockNotifier;
import com.blackducksoftware.integration.email.mock.TestEmailEngine;

public class ItemNotifierTest {
    private final static String NOTIFIER_KEY = "notifier.key";

    private MockNotifier notifier;

    private EmailEngine engine;

    @Before
    public void initTest() throws Exception {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final URL propFileUrl = classLoader.getResource("extension.properties");
        final File file = new File(propFileUrl.toURI());
        System.setProperty("ext.config.location", file.getCanonicalFile().getParent());
        engine = new TestEmailEngine();
        engine.start();
        notifier = new MockNotifier(engine.getExtensionProperties(), engine.getEmailMessagingService(), engine.getExtConfigDataService(), NOTIFIER_KEY);
    }

    @After
    public void endTest() throws Exception {
        engine.shutDown();
    }

    @Test
    public void testGetName() {
        assertEquals(MockNotifier.class.getName(), notifier.getName());
    }

    @Test
    public void testGetNotifierTemplateName() {
        assertEquals(NOTIFIER_KEY, notifier.getTemplateName());
    }

    @Test
    public void testGetNotifierPropKey() {
        assertEquals(NOTIFIER_KEY, notifier.getNotifierPropertyKey());
    }

    @Test
    public void testGetInterval() {
        assertEquals(MockNotifier.CRON_EXPRESSION, notifier.getCronExpression());
    }

    @Test
    public void testGetDelay() {
        assertEquals(0, notifier.getStartDelayMilliseconds());
    }

    @Test
    public void testRun() {
        notifier.run();
        assertTrue(notifier.hasRun());
    }
}
