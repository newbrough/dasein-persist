package org.dasein.persist;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Used for tests.
 *
 * @author Tom Howe
 */
public class MockSequencer extends Sequencer {
    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public long next() throws PersistenceException {
        return counter.getAndIncrement();
    }
}
