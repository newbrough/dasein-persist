/**
 * Copyright (C) 1998-2011 enStratusNetworks LLC
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.persist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.dasein.persist.jdbc.Counter;
import org.dasein.persist.jdbc.Creator;
import org.dasein.persist.jdbc.Deleter;
import org.dasein.persist.jdbc.Loader;
import org.dasein.persist.jdbc.Updater;
import org.dasein.persist.jdbc.AutomatedSql.Operator;
import org.dasein.persist.jdbc.AutomatedSql.TranslationMethod;
import org.dasein.util.CacheLoader;
import org.dasein.util.CachedItem;
import org.dasein.util.CacheManagementException;
import org.dasein.util.DaseinUtilTasks;
import org.dasein.util.JitCollection;
import org.dasein.util.Jiterator;
import org.dasein.util.JiteratorFilter;
import org.dasein.util.MapLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

public final class RelationalCache<T extends CachedItem> extends PersistentCache<T> {
    static public final Logger logger = LoggerFactory.getLogger(RelationalCache.class);

    static public class OrderedColumn {
        public String  column;
        public boolean descending = false;
    }
    
    private String            readDataSource    = null;
    private TranslationMethod translationMethod = TranslationMethod.NONE;
    private String            writeDataSource   = null;
    private final DataSource        ds;

    public RelationalCache(DataSource ds, Sequencer sequencer) {
        super(sequencer);
        this.ds = ds;
    }
    
    /**
     * Constructs a new persistent factory for objects of the specified class with 
     * the named unique identifier attributes.
     * @param cls the class of objects managed by this factory
     * @param keys a list of unique identifiers for instances of the specified class
     */
    protected void init(Class<T> cls, Key ... keys) {
        readDataSource = Execution.getDataSourceName(cls.getName(), true);
        writeDataSource = Execution.getDataSourceName(cls.getName(), false);
        if( readDataSource == null ) {
            readDataSource = writeDataSource;
        }
        if( writeDataSource == null ) {
            writeDataSource = readDataSource;
        }
    }
    
    public void setTranslationMethod(TranslationMethod translationMethod) {
    	this.translationMethod = translationMethod;
    }
    
    private Counter getCounter(SearchTerm[] whereTerms) {
        final SearchTerm[] terms = whereTerms;
        final RelationalCache<T> self = this;

        return new Counter() {
            public void init() {
                setTarget(self.getEntityClassName());
                if( terms != null && terms.length > 0 ) {
                    ArrayList<Criterion> criteria = new ArrayList<Criterion>();

                    for( SearchTerm term : terms ) {
                        criteria.add(new Criterion(term.getColumn(), term.getOperator()));
                    }
                    setCriteria(criteria.toArray(new Criterion[criteria.size()]));
                }
            }

            public boolean isReadOnly() {
                return true;
            }
        };
    }
    
    private Creator getCreator() {
        final RelationalCache<T> self = this;

        return new Creator() {
            public void init() {
                setTarget(self.getEntityClassName());
                switch (translationMethod) {
                case CUSTOM: setCustomTranslating(); break;
                case STANDARD: setTranslating(true); break;
                case NONE: setTranslating(false); break;
                }
            }

            public boolean isReadOnly() {
                return false;
            }
        };
    }
    
    private Deleter getDeleter(SearchTerm ... terms) {
        final SearchTerm[] killTerms = terms;
        final RelationalCache<T> self = this;

        return new Deleter() {
            public void init() {
                setTarget(self.getEntityClassName());
                if( killTerms != null && killTerms.length > 0 ) {
                    ArrayList<Criterion> criteria = new ArrayList<Criterion>();

                    for( SearchTerm term : killTerms ) {
                        criteria.add(new Criterion(term.getJoinEntity(), term.getColumn(), term.getOperator()));
                    }
                    setCriteria(criteria.toArray(new Criterion[criteria.size()]));
                }
                else {
                    setCriteria(self.getPrimaryKey().getFields());
                }
                switch (translationMethod) {
                case CUSTOM: setCustomTranslating(); break;
                case STANDARD: setTranslating(true); break;
                case NONE: setTranslating(false); break;
                }

            }

            public boolean isReadOnly() {
                return false;
            }
        };
    }
    
    private Loader getLoader(SearchTerm[] whereTerms, OrderedColumn[] orderBy) {
        final SearchTerm[] terms = whereTerms;
        final OrderedColumn[] order = orderBy;
        final RelationalCache<T> self = this;

        return new Loader() {
            public void init() {
                setTarget(self.getEntityClassName());
                setEntityJoins(getJoins());
                if( terms != null && terms.length > 0 ) {
                    ArrayList<Criterion> criteria = new ArrayList<Criterion>();

                    for( SearchTerm term : terms ) {
                        criteria.add(new Criterion(term.getJoinEntity(), term.getColumn(), term.getOperator()));
                    }
                    setCriteria(criteria.toArray(new Criterion[criteria.size()]));
                }
                if( order != null && order.length > 0 ) {
                    ArrayList<String> cols = new ArrayList<String>();
                    boolean desc = order[0].descending;

                    for( OrderedColumn col : order ) {
                        cols.add(col.column);
                    }
                    setOrder(desc, cols.toArray(new String[cols.size()]));
                }
                switch (translationMethod) {
                case CUSTOM: setCustomTranslating(); break;
                case STANDARD: setTranslating(true); break;
                case NONE: setTranslating(false); break;
                }
            }

            public boolean isReadOnly() {
                return true;
            }
        };
    }
    
    private Updater getUpdater() {
        final RelationalCache<T> self = this;

        return new Updater() {
            public void init() {
                setTarget(self.getEntityClassName());
                setCriteria(self.getPrimaryKey().getFields());
                switch (translationMethod) {
                case CUSTOM: setCustomTranslating(); break;
                case STANDARD: setTranslating(true); break;
                case NONE: setTranslating(false); break;
                }
            }

            public boolean isReadOnly() {
                return false;
            }
        };
    }
    
    /**
     * Counts the total number of objects governed by this factory in the database.
     * @return the number of objects in the database
     * @throws PersistenceException an error occurred counting the elements in the database
     */
    @Override
    public long count() throws PersistenceException {
        logger.debug("enter - count()");
        try {
            Transaction xaction = Transaction.getInstance(ds, true);
            Counter counter = getCounter(null);            
            
            try {
                Map<String,Object> results;
                long count;
    
                results = xaction.execute(counter, new HashMap<String,Object>(), readDataSource);
                count = ((Number)results.get("count")).longValue();
                xaction.commit();
                return count;
            }
            finally {
                xaction.rollback();
            }
        }
        finally {
            logger.debug("exit - count()");
        }
    }
        
    @Override
    public long count(SearchTerm ... terms) throws PersistenceException {
        logger.debug("enter - count(SearchTerm...)");
        try {
            Transaction xaction = Transaction.getInstance(ds, true);
            Counter counter = getCounter(terms);            
            
            try {
                Map<String,Object> params = toParams(terms);                
                Map<String,Object> results;
                long count;
    
                results = xaction.execute(counter, params, readDataSource);
                count = ((Number)results.get("count")).longValue();
                xaction.commit();
                return count;
            }
            finally {
                xaction.rollback();
            }
        }
        finally {
            logger.debug("exit - count(SearchTerm...)");
        }
    }
    
    /**
     * Creates the specified object with the data provided in the specified state under
     * the governance of the specified transaction.
     * @param xaction the transaction governing this event
     * @param state the new state for the new object
     * @throws PersistenceException an error occurred talking to the data store, or
     * creates are not supported
     */
    @Override
    public T create(Transaction xaction, Map<String,Object> state) throws PersistenceException {
        state.put("--key--", getPrimaryKey().getFields()[0]);
        xaction.execute(getCreator(), state, writeDataSource);
        return getCache().find(state);
    }
    
    @Override
    @Nonnull
    public Collection<T> find(@Nonnull SearchTerm[] terms, JiteratorFilter<T> filter, Boolean orderDesc, String ... orderFields) throws PersistenceException {
        logger.debug("enter - find(SearchTerm[], JiteratorFilter, Boolean, String)");
        try {
            OrderedColumn[] order;
            
            if( orderFields == null ) {
                order = new OrderedColumn[0];
            }
            else {
                int i = 0;
                
                order = new OrderedColumn[orderFields.length];
                for( String field : orderFields ) {
                    order[i] = new OrderedColumn();
                    order[i].column = field;
                    order[i].descending = (orderDesc != null && orderDesc);
                    i++;
                }
            }
            return this.load(getLoader(terms, order), filter, toParams(terms), false);
        }
        finally {
            logger.debug("exit - find(SearchTerm[], JiteratorFilter, Boolean, String...)");
        }
    }
    
    /**
     * Retrieves the object uniquely identified by the value for the specified ID field.
     * @param primaryKeyValue the ID field identifying the object
     * @return the object matching the query criterion
     * @throws PersistenceException an error occurred talking to the data store
     */
    @Override
    public T get(Object primaryKeyValue) throws PersistenceException {
        try {
            CacheLoader<T> loader;
            
            loader = new CacheLoader<T>() {
                public T load(Object ... args) {
                    SearchTerm[] terms = new SearchTerm[1];
                    Collection<T> list;
                    
                    terms[0] = new SearchTerm((String)args[0], Operator.EQUALS, args[1]);
                    try {
                        list = RelationalCache.this.load(getLoader(terms, null), null, toParams(terms), true);
                    }
                    catch( PersistenceException e ) {
                        try {
                            try { Thread.sleep(1000L); }
                            catch( InterruptedException ignore ) { }
                            list = RelationalCache.this.load(getLoader(terms, null), null, toParams(terms), true);
                        }
                        catch( Throwable forgetIt ) {
                            logger.error(forgetIt.getMessage(), forgetIt);
                            throw new RuntimeException(e);
                        }
                    }
                    if( list.isEmpty() ) {
                        return null;
                    }
                    return list.iterator().next();
                }
            };
            logger.debug("Executing cache find...");
            try {
                return getCache().find(getPrimaryKeyField(), primaryKeyValue, loader, getPrimaryKeyField(), primaryKeyValue);
            }
            catch( CacheManagementException e ) {
                throw new PersistenceException(e);
            }
            catch( RuntimeException e ) {
                Throwable t = e.getCause();
                
                if( t != null && t instanceof PersistenceException ) {
                    throw (PersistenceException)t;
                }
                if( logger.isDebugEnabled() ) {
                    logger.error(e.getMessage(), e);
                }
                throw new PersistenceException(e);
            }
            finally {
                logger.debug("Executed.");
            }
        }
        finally {
            logger.debug("exit - get(String,Object)");
        }
    }

    @Override
    public String getSchema() throws PersistenceException {

        return "CREATE TABLE " + getSqlNameForClassName(getEntityClassName()) + " (" + ");";
    }

    protected String getSqlName(String nom) {
        StringBuilder sql = new StringBuilder();

        for(int i=0; i<nom.length(); i++) {
            char c = nom.charAt(i);

            if( Character.isLetter(c) && !Character.isLowerCase(c) ) {
                if( i != 0 ) {
                    sql.append("_");
                }
                sql.append(Character.toLowerCase(c));
            }
            else {
                sql.append(c);
            }
        }
        return sql.toString();
    }

    protected String getSqlNameForClassName(String cname) {
        String[] parts = cname.split("\\.");
        int i;

        if( parts.length > 1 ) {
            cname = parts[parts.length-1];
        }
        i = cname.lastIndexOf("$");
        if( i != -1 ) {
            cname = cname.substring(i+1);
        }
        return getSqlName(cname);
    }

    /**
     * Loads all elements of this class from the data store. Use this method only when you know
     * exactly what you are doing. Otherwise, you will pull a lot of data.
     * @return all objects from the database
     * @throws PersistenceException an error occurred executing the query
     */
    @Override
    public Collection<T> list() throws PersistenceException {
        logger.debug("enter - list()");
        try {
            return find(new SearchTerm[]{}, null, false);
        }
        finally {
            logger.debug("exit - list()");
        }
    }
    
    private Map<String,Object> toParams(SearchTerm ... searchTerms) {
        HashMap<String,Object> params = new HashMap<String,Object>();
        
        if( searchTerms != null ) {
            for( SearchTerm term : searchTerms ) {
                params.put(term.getColumn(), term.getValue());
            }
        }
        return params;
    }

    /** from list of results, 
     *  find corresponding values from cache if available (otherwise use loader),
     *  put resulting objects into iterator.
     *  
     *  may result in stale data in iterator even if results contains fresh
     */
    private class UpdateIteratorFromCacheOrLoader implements Runnable {
        private final Jiterator<T> it;
        private final Map<String,Object> results;

        private UpdateIteratorFromCacheOrLoader(Jiterator<T> it, Map<String,Object> results) {
            this.it = it;
            this.results = results;
        }

        @Override
        public void run() {
            try {
                for( Map<String,Object> map: (Collection<Map<String,Object>>)this.results.get(Loader.LISTING) ) {
                    for( String fieldName : map.keySet() ) {
                        LookupDelegate delegate = getLookupDelegate(fieldName);

                        if( delegate != null && !delegate.validate((String)map.get(fieldName)) ) {
                            throw new PersistenceException("Unable to validate " + fieldName + " value of " + map.get(fieldName));
                        }
                    }
                    this.it.push(getCache().find(map));
                }
                this.it.complete();
            }
            catch( Exception e ) {
                this.it.setLoadException(e);
            }
            catch( Throwable t ) {
                this.it.setLoadException(new RuntimeException(t));
            }
        }
    }
    
    /** from list of results only,
     * construct objects and add to iterator
     * 
     * does not use cached values.
     */
    private class UpdateIteratorFromResults implements Runnable {
        private final Jiterator<T> it;
        private final Map<String,Object> results;

        private UpdateIteratorFromResults(Jiterator<T> it, Map<String,Object> results) {
            this.it = it;
            this.results = results;
        }

        @Override
        public void run() {
            try {
                final MapLoader<T> loader = new MapLoader<T>(getCache().getTarget());
                for( Map<String,Object> map: (Collection<Map<String,Object>>)this.results.get(Loader.LISTING) ) {
                    for( String fieldName : map.keySet() ) {
                        LookupDelegate delegate = getLookupDelegate(fieldName);

                        if( delegate != null && !delegate.validate((String)map.get(fieldName)) ) {
                            throw new PersistenceException("Unable to validate " + fieldName + " value of " + map.get(fieldName));
                        }
                    }
                    
                    this.it.push(loader.load(map));
                }
                this.it.complete();
            }
            catch( Exception e ) {
                this.it.setLoadException(e);
            }
            catch( Throwable t ) {
                this.it.setLoadException(new RuntimeException(t));
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private Collection<T> load(Loader loader, JiteratorFilter<T> filter, Map<String,Object> params, boolean readCache) throws PersistenceException {
        logger.debug("enter - load(Class,SearchTerm...)");
        try {
            Transaction xaction = Transaction.getInstance(ds, true);
            final Jiterator<T> it = new Jiterator<T>(filter);

            params.put("--key--", getPrimaryKey().getFields()[0]);
            try {
                final Map<String,Object> results;
                
                results = xaction.execute(loader, params, readDataSource);
                xaction.commit();

                if(readCache) { DaseinUtilTasks.submit(new UpdateIteratorFromCacheOrLoader(it, results)); }
                else { DaseinUtilTasks.submit(new UpdateIteratorFromResults(it, results)); }
                
                return new JitCollection<T>(it, getEntityClassName());
            }
            catch( PersistenceException e ) {
                it.setLoadException(e);
                throw e;
            }
            catch( RuntimeException e ) {
                it.setLoadException(e);
                throw e;
            }
            catch( Throwable t ) {
                RuntimeException e = new RuntimeException(t);
                
                it.setLoadException(e);
                throw e;
            }
            finally {
                xaction.rollback();
            }
        }
        finally {
            logger.debug("exit - load(Class,Map)");
        }
    }
    
    /**
     * Removes the specified item from the system permanently.
     * @param xaction the transaction under which this event is occurring
     * @param item the item to be removed
     * @throws PersistenceException an error occurred talking to the data store or
     * removal of these objects is prohibited
     */
    @Override
    public void remove(Transaction xaction, T item) throws PersistenceException {
        xaction.execute(getDeleter(), getCache().getKeys(item), writeDataSource);
        getCache().release(item);
    }
    
    @Override
    public void remove(Transaction xaction, SearchTerm ... terms) throws PersistenceException {
        xaction.execute(getDeleter(terms), toParams(terms), writeDataSource);
    }
    
    public String toString() {
        return getCache().toString();
    }
    
    /**
     * Updates the specified object with the data provided in the specified state under
     * the governance of the specified transaction.
     * @param xaction the transaction governing this event
     * @param item the item to be updated
     * @param state the new state for the updated object
     * @throws PersistenceException an error occurred talking to the data store, or
     * updates are not supported
     */
    @Override
    public void update(Transaction xaction, T item, Map<String,Object> state) throws PersistenceException {     
        state.put("--key--", getPrimaryKey().getFields()[0]);
        xaction.execute(getUpdater(), state, writeDataSource);
    }    
}
