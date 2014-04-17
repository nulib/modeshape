/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.modeshape.jcr.bus;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit test for {@link org.modeshape.jcr.bus.ChangeSetDispatcher}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ChangeSetDispatcherTest {

    private ChangeSetDispatcher disruptor;

    @Before
    public void setUp() throws Exception {
        disruptor = new ChangeSetDispatcher(Executors.newCachedThreadPool(), 1 << 10);
    }

    @After
    public void tearDown() throws Exception {
        disruptor.stop();
    }

    @Test
    public void shouldNotifySingleConsumerAsync() throws Exception {
        AbstractChangeBusTest.TestChangeSet changeSet = new AbstractChangeBusTest.TestChangeSet();
        disruptor.dispatchAsync(changeSet);
        AbstractChangeBusTest.TestListener listener = new AbstractChangeBusTest.TestListener(1);
        disruptor.addAsyncListener(listener);
        disruptor.dispatchAsync(changeSet);
        Thread.sleep(50);
        disruptor.removeListener(listener);
        Thread.sleep(50);
        disruptor.dispatchAsync(new AbstractChangeBusTest.TestChangeSet());
        listener.assertExpectedEventsCount();
        assertEquals(changeSet, listener.getObservedChangeSet().get(0));
    }

    @Test
    public void shouldNotifySingleConsumerSync() throws Exception {
        AbstractChangeBusTest.TestChangeSet changeSet = new AbstractChangeBusTest.TestChangeSet();
        disruptor.dispatchSync(changeSet);
        AbstractChangeBusTest.TestListener listener = new AbstractChangeBusTest.TestListener(1);
        disruptor.addSyncListener(listener);
        disruptor.dispatchSync(changeSet);
        disruptor.removeListener(listener);
        disruptor.dispatchSync(new AbstractChangeBusTest.TestChangeSet());
        listener.assertExpectedEventsCount();
        assertEquals(changeSet, listener.getObservedChangeSet().get(0));
    }

    @Test
    public void shouldNotifyTwoConsumersAsync() throws Exception {
        AbstractChangeBusTest.TestChangeSet changeSet1 = new AbstractChangeBusTest.TestChangeSet();
        AbstractChangeBusTest.TestChangeSet changeSet2 = new AbstractChangeBusTest.TestChangeSet();

        AbstractChangeBusTest.TestListener listener1 = new AbstractChangeBusTest.TestListener(1);
        disruptor.addAsyncListener(listener1);

        AbstractChangeBusTest.TestListener listener2 = new AbstractChangeBusTest.TestListener(2);
        disruptor.addAsyncListener(listener2);

        disruptor.dispatchAsync(changeSet1);
        Thread.sleep(50);

        disruptor.removeListener(listener1);
        Thread.sleep(50);

        disruptor.dispatchAsync(changeSet2);
        Thread.sleep(50);

        disruptor.removeListener(listener2);

        listener1.assertExpectedEventsCount();
        assertEquals(changeSet1, listener1.getObservedChangeSet().get(0));

        listener2.assertExpectedEventsCount();
        assertEquals(Arrays.asList(changeSet1, changeSet2), listener2.getObservedChangeSet());
    }

    @Test
    public void shouldNotifyTwoConsumersSync() throws Exception {
        AbstractChangeBusTest.TestChangeSet changeSet1 = new AbstractChangeBusTest.TestChangeSet();
        AbstractChangeBusTest.TestChangeSet changeSet2 = new AbstractChangeBusTest.TestChangeSet();

        AbstractChangeBusTest.TestListener listener1 = new AbstractChangeBusTest.TestListener(1);
        disruptor.addSyncListener(listener1);

        AbstractChangeBusTest.TestListener listener2 = new AbstractChangeBusTest.TestListener(2);
        disruptor.addSyncListener(listener2);
        disruptor.dispatchSync(changeSet1);

        disruptor.removeListener(listener1);

        disruptor.dispatchSync(changeSet2);

        disruptor.removeListener(listener2);

        listener1.assertExpectedEventsCount();
        assertEquals(changeSet1, listener1.getObservedChangeSet().get(0));

        listener2.assertExpectedEventsCount();
        assertEquals(Arrays.asList(changeSet1, changeSet2), listener2.getObservedChangeSet());
    }

    @Test
    public void shouldNotifyAsyncOnlyWithEventsFiredPostRegistration() throws Exception {
        AbstractChangeBusTest.TestChangeSet changeSet1 = new AbstractChangeBusTest.TestChangeSet();

        AbstractChangeBusTest.TestListener listener1 = new AbstractChangeBusTest.TestListener(2);
        disruptor.addAsyncListener(listener1);
        Thread.sleep(50);
        disruptor.dispatchAsync(changeSet1);

        AbstractChangeBusTest.TestChangeSet changeSet2 = new AbstractChangeBusTest.TestChangeSet();
        AbstractChangeBusTest.TestListener listener2 = new AbstractChangeBusTest.TestListener(1);
        disruptor.addAsyncListener(listener2);
        Thread.sleep(50);
        disruptor.dispatchAsync(changeSet2);

        disruptor.removeListener(listener1);
        disruptor.removeListener(listener2);

        listener1.assertExpectedEventsCount();
        assertEquals(Arrays.asList(changeSet1, changeSet2), listener1.getObservedChangeSet());

        listener2.assertExpectedEventsCount();
        assertEquals(changeSet2, listener2.getObservedChangeSet().get(0));
    }

    @Test
    public void shouldNotifySyncOnlyWithEventsFiredPostRegistration() throws Exception {
        AbstractChangeBusTest.TestChangeSet changeSet1 = new AbstractChangeBusTest.TestChangeSet();

        AbstractChangeBusTest.TestListener listener1 = new AbstractChangeBusTest.TestListener(2);
        disruptor.addSyncListener(listener1);
        disruptor.dispatchSync(changeSet1);

        AbstractChangeBusTest.TestChangeSet changeSet2 = new AbstractChangeBusTest.TestChangeSet();
        AbstractChangeBusTest.TestListener listener2 = new AbstractChangeBusTest.TestListener(1);
        disruptor.addSyncListener(listener2);
        disruptor.dispatchSync(changeSet2);

        disruptor.removeListener(listener1);
        disruptor.removeListener(listener2);

        listener1.assertExpectedEventsCount();
        assertEquals(Arrays.asList(changeSet1, changeSet2), listener1.getObservedChangeSet());

        listener2.assertExpectedEventsCount();
        assertEquals(changeSet2, listener2.getObservedChangeSet().get(0));
    }

    @Test
    @Ignore("This is a perf test")
    public void shouldNotifyLotsOfConsumersAsync() throws Exception {
        int eventsPerBatch = 300000;
        int listenersPerBatch = 30;
        int batches = 4;
        List<AbstractChangeBusTest.TestListener> listeners = new ArrayList<>();

        long start = System.nanoTime();
        for (int i = 0; i < batches; i++) {
            listeners.addAll(submitBatch(eventsPerBatch, listenersPerBatch, (batches - i) * eventsPerBatch));
            Thread.sleep(50);
        }

        for (AbstractChangeBusTest.TestListener listener : listeners) {
            listener.assertEventsSequential();
        }
        System.out.println("Elapsed: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + " millis");
    }

    private List<AbstractChangeBusTest.TestListener> submitBatch( int eventCount, int listenerCount, int expectedEventsCount ) {
        List<AbstractChangeBusTest.TestListener> listeners = new ArrayList<>();
        for (int i = 0; i < listenerCount; i++) {
            AbstractChangeBusTest.TestListener listener = new AbstractChangeBusTest.TestListener(expectedEventsCount, 500);
            listeners.add(listener);
            disruptor.addAsyncListener(listener);
        }

        for (int i = 0; i < eventCount; i++) {
            disruptor.dispatchAsync(new AbstractChangeBusTest.TestChangeSet());
        }
        return listeners;
    }
}
