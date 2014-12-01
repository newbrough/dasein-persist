package org.dasein.persist;

import org.dasein.util.CachedItem;

/**
 * Interface to support multiple backing Cache types. In particular this is designed to facilitate
 * a Mock cache instance which can be used in tests.
 *
 * @author Tom Howe
 */
public interface PersistentCacheFactory {

    <T extends CachedItem> PersistentCache<T> getCacheInstance(Class<T> cacheClass);
}
