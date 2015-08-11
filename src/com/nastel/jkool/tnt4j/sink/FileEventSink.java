/*
 * Copyright 2014-2015 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nastel.jkool.tnt4j.sink;

import java.io.IOException;
import java.io.PrintStream;

import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.core.Snapshot;
import com.nastel.jkool.tnt4j.format.EventFormatter;
import com.nastel.jkool.tnt4j.source.Source;
import com.nastel.jkool.tnt4j.tracker.TrackingActivity;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;

/**
 * <p>
 * This class implements <code>EvenSink</code> with file <code>FileSink</code> as the underlying storage.
 * </p>
 * 
 * 
 * @version $Revision: 1 $
 * 
 * @see OpLevel
 * @see FileSink
 * @see EventFormatter
 * @see AbstractEventSink
 */
public class FileEventSink extends AbstractEventSink {

	FileSink fileSink;
	
	/**
	 * Create a file based event sink instance.
	 * 
	 * @param nm logical event sink name
	 * @param fileName associated with the sink where all entries are recorded
	 * @param append true to append to file, false otherwise (file recreated)
	 * @param frm event formatter to be used for formatting event entries
	 */
	public FileEventSink(String nm, String fileName, boolean append, EventFormatter frm) {
	    super(nm, frm);
	    fileSink = new FileSink(fileName, append, frm);
    }

	@Override
    public boolean isSet(OpLevel sev) {
	    return true;
    }

	@Override
    public Object getSinkHandle() {
	    return fileSink;
    }

	@Override
    public boolean isOpen() {
	    return fileSink.isOpen();
    }

	@Override
    public void open() throws IOException {
		fileSink.open();
	}

	@Override
    public void close() throws IOException {
		fileSink.close();
	}

	@Override
    protected void _checkState() throws IllegalStateException {
		if (fileSink == null || !fileSink.isOpen()) {
			throw new IllegalStateException("Sink is not defined or closed");
		}
	}

	@Override
    protected void _write(Object msg, Object... args) throws IOException, InterruptedException {
		fileSink.write(msg, args);
	}

	@Override
    protected void _log(TrackingEvent event) throws IOException {
        fileSink.write(event);
    }

	@Override
    protected void _log(TrackingActivity activity) throws IOException {
        fileSink.write(activity);
    }

	@Override
    protected void _log(Snapshot snapshot) {
		PrintStream printer = fileSink.getPrintStream();
		printer.println(getEventFormatter().format(snapshot));		
		printer.flush();
	}	

	@Override
    protected void _log(long ttl, Source src, OpLevel sev, String msg, Object... args) {
		PrintStream printer = fileSink.getPrintStream();
		printer.println(getEventFormatter().format(ttl, src, sev, msg, args));		
		printer.flush();
	}	
}
