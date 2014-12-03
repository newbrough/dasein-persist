package org.dasein.persist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;


public class SequencerFactory {
    private final static Logger logger = LoggerFactory.getLogger(SequencerFactory.class);
    private final Class<? extends Sequencer> defaultSequencer;
    private final Map<String,Sequencer> sequencers;
    private final DataSource ds;

       public SequencerFactory(DataSource ds){
        this(ds, DatabaseKeyGenerator.class.getName());
    }

    public SequencerFactory(DataSource ds, String defaultSequencerName){
        this(ds, new HashMap<String, Sequencer>(), defaultSequencerName);

    }

    @SuppressWarnings("unchecked")
    public SequencerFactory(DataSource ds, Map<String, Sequencer> sequencers, String defaultSequencerName){
        Class<? extends Sequencer> _sequencerClass = null;
        try {
            _sequencerClass = (Class<? extends Sequencer>) Class.forName(defaultSequencerName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            _sequencerClass = DatabaseKeyGenerator.class;
        }
        this.defaultSequencer = _sequencerClass;
        this.sequencers = sequencers;
        this.ds = ds;
    }

    public Sequencer getSequencer(String name){
        logger.debug("enter - getTransaction()");
        try {
            Sequencer seq = null;

            if( sequencers.containsKey(name) ) {
                seq = sequencers.get(name);
            }
            if( seq != null ) {
                return seq;
            }
            synchronized( sequencers ) {
                // redundant due to the non-synchronized calls above done for performance
                if( !sequencers.containsKey(name) ) {
                    try {
                        seq = defaultSequencer.getDeclaredConstructor(DataSource.class).newInstance(this.ds);
                    }
                    catch( Exception e ) {
                        logger.debug(e.getMessage(), e);
                        try {
                            seq = defaultSequencer.newInstance();
                        } catch (Exception e1) {
                            return null;
                        }
                    }
                    seq.setName(name);
                    sequencers.put(name, seq);
                    return seq;
                }
                else {
                    return sequencers.get(name);
                }
            }
        }
        finally {
            logger.debug("exit - getTransaction()");
        }
    }
}
