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

/* $Id: Sequencer.java,v 1.2 2005/08/15 16:15:59 george Exp $ */
/* Copyright (c) 2002-2004 Valtira Corporation, All Rights Reserved */
package org.dasein.persist;

// J2SE imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * <p>
 * Provides a generic interface for sequence generation that may be
 * implemented by a number of different sequence providers. You may configure what sequencer is used through
 * the configuration file <i>dasein-persistence.properties</i>. You
 * specify a concrete sequencer using the following:<br/>
 * <code>dasein.sequencer.NAME=CLASSNAME</code>
 * </p>
 * <p>
 * For example:<br/>
 * <code>dasein.sequencer.pageId=org.dasein.persist.DaseinSequencer</code>
 * </p>
 * <p>
 * In code, you might have the following:
 * </p>
 * <p>
 * <code>
 * Sequencer seq = Sequencer.getTransaction(this.class);<br/>
 * id = seq.next();
 * </code>
 * </p>
 * <p>
 * Last modified $Date: 2005/08/15 16:15:59 $
 * </p>
 * @version $Revision: 1.2 $
 * @author George Reese
 */
public abstract class Sequencer {
    static private final Logger logger = LoggerFactory.getLogger(Sequencer.class);

    /**
     * All sequencers currently in memory.
     */
    static private final Map<String,Sequencer>      sequencers       = new HashMap<String,Sequencer>();


    /**
     * The name of this sequencer.
     */
    private String name     = null;

    /**
     * Constructs an empty sequencer with no name. This constructor
     * should be followed by a call to {@link #setName(String)} before
     * the sequencer is used.
     */
    public Sequencer() {
        super();
    }
    
    /**
     * @return the name of the sequencer
     */
    public String getName() {
        return name;
    }
    
    /**
     * Generates a new unique number based on the implementation class'
     * algorithm of choice. The generated number must be a Java long
     * that is guaranteed to be unique for all sequences sharing this
     * name.
     * @return a unique number for the sequence of this sequencer's name
     * @throws org.dasein.persist.PersistenceException a data store error
     * occurred while generating the number
     */
    public abstract long next() throws PersistenceException;

    /**
     * Sets the sequencer name.
     * @param nom the name of the sequencer
     */
    public final void setName(String nom) {
        name = nom;
    }
}
