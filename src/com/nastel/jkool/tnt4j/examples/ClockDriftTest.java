/*
 * Copyright 2014 Nastel Technologies, Inc.
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
package com.nastel.jkool.tnt4j.examples;

public class ClockDriftTest {
	  private static final int ONE_MILLION  = 1000000;
	  private static final int HALF_MILLION =  499999;

	  public static void main(String[] args) {
	    long start = System.nanoTime();
	    long base = System.currentTimeMillis() - (start / ONE_MILLION);

	    while (true) {
	      try {
	        Thread.sleep(10000);
	      } catch (InterruptedException e) {
	      }
	      long now = System.nanoTime();
	      long drift = System.currentTimeMillis() - (now / ONE_MILLION) - base;
	      long interval = (now - start + HALF_MILLION) / ONE_MILLION;
	      System.out.println("Clock drift " + drift + " msec after " + interval
	                         + " msec = " + (drift * 1000 / interval) + " msec/sec");
	    }
	  }
	}