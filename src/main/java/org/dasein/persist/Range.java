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

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Range implements Iterable<Integer> {
    int start;
    int end;
    
    public Range(int start, int end) {
        this.start = start;
        this.end = end;
    }
    
    public int getEnd() {
        return end;
    }
    
    public int getStart() {
        return start;
    }
    

    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            private volatile int pointer = start-1;
            
            public boolean hasNext() {
                return (pointer < end);
            }

            public Integer next() {
                if( pointer >= end ) {
                    throw new NoSuchElementException("Maximum of range is " + end);
                }
                pointer++;
                return pointer;
            }

            public void remove() {
                throw new UnsupportedOperationException("It makes absolutely no sense to remove a number from a range.");
            }
        };
    }
}
