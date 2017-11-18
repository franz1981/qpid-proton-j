/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.qpid.proton.message.impl.jmh;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.UnsignedByte;
import org.apache.qpid.proton.amqp.UnsignedInteger;
import org.apache.qpid.proton.amqp.UnsignedShort;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.Header;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.amqp.messaging.Properties;
import org.apache.qpid.proton.amqp.transport.Disposition;
import org.apache.qpid.proton.amqp.transport.Flow;
import org.apache.qpid.proton.amqp.transport.Role;
import org.apache.qpid.proton.amqp.transport.Transfer;
import org.apache.qpid.proton.codec.AMQPDefinedTypes;
import org.apache.qpid.proton.codec.DecoderImpl;
import org.apache.qpid.proton.codec.EncoderImpl;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
public abstract class MessageBenchmark {

   protected ByteBuffer byteBuf;
   protected DecoderImpl decoder;
   protected EncoderImpl encoder;

   public void init() {
      byteBuf = ByteBuffer.allocate(8192);
      this.decoder = new DecoderImpl();
      this.encoder = new EncoderImpl(decoder);
      AMQPDefinedTypes.registerAllTypes(decoder, encoder);
      //initialize encoders
      encoder.setByteBuffer(byteBuf);
      decoder.setByteBuffer(byteBuf);
   }

   public static class ListOfIntsBench extends MessageBenchmark {

      private static final int LIST_SIZE = 10;
      private ArrayList<Object> listOfInts;

      @Setup
      public void init() {
         super.init();
         initListOfInts();
         encode();
      }

      private void initListOfInts() {
         this.listOfInts = new ArrayList<>(LIST_SIZE);
         for (int i = 0; i < LIST_SIZE; i++) {
            listOfInts.add(i);
         }
      }

      @Benchmark
      @Override
      public ByteBuffer encode() {
         byteBuf.clear();
         encoder.writeList(listOfInts);
         return byteBuf;
      }

      @Benchmark
      @Override
      public List decode() {
         byteBuf.flip();
         return decoder.readList();
      }

   }

   public static class UUIDBench extends MessageBenchmark {

      private UUID uuid;

      @Setup
      public void init() {
         super.init();
         initUUID();
         encode();
      }

      private void initUUID() {
         this.uuid = UUID.randomUUID();
      }

      @Benchmark
      @Override
      public ByteBuffer encode() {
         byteBuf.clear();
         encoder.writeUUID(uuid);
         return byteBuf;
      }

      @Benchmark
      @Override
      public UUID decode() {
         byteBuf.flip();
         return decoder.readUUID();
      }

   }

   public static class HeaderBench extends MessageBenchmark {

      private Header header;

      @Setup
      public void init() {
         super.init();
         initHeader();
         encode();
      }

      private void initHeader() {
         header = new Header();
         header.setDurable(true);
         header.setFirstAcquirer(true);
      }

      @Benchmark
      @Override
      public Object decode() {
         return super.decode();
      }

      @Benchmark
      @Override
      public ByteBuffer encode() {
         return encodeObj(header);
      }
   }

   public static class TransferBench extends MessageBenchmark {

      private Transfer transfer;

      @Setup
      public void init() {
         super.init();
         initTransfer();
         encode();
      }

      private void initTransfer() {
         transfer = new Transfer();
         transfer.setDeliveryTag(new Binary(new byte[]{1, 2, 3}));
         transfer.setHandle(UnsignedInteger.valueOf(10));
         transfer.setMessageFormat(UnsignedInteger.ZERO);
      }

      @Benchmark
      @Override
      public Object decode() {
         return super.decode();
      }

      @Benchmark
      @Override
      public ByteBuffer encode() {
         return encodeObj(transfer);
      }
   }

   public static class FlowBench extends MessageBenchmark {

      private Flow flow;

      @Setup
      public void init() {
         super.init();
         initFlow();
         encode();
      }

      private void initFlow() {
         flow = new Flow();
         flow.setNextIncomingId(UnsignedInteger.valueOf(1));
         flow.setIncomingWindow(UnsignedInteger.valueOf(2047));
         flow.setNextOutgoingId(UnsignedInteger.valueOf(1));
         flow.setOutgoingWindow(UnsignedInteger.MAX_VALUE);
         flow.setHandle(UnsignedInteger.ZERO);
         flow.setDeliveryCount(UnsignedInteger.valueOf(10));
         flow.setLinkCredit(UnsignedInteger.valueOf(1000));
      }

      @Benchmark
      @Override
      public ByteBuffer encode() {
         return encodeObj(flow);
      }

      @Benchmark
      @Override
      public Object decode() {
         return super.decode();
      }
   }

   public static class PropertiesBench extends MessageBenchmark {

      private Properties properties;

      @Setup
      public void init() {
         super.init();
         initProperties();
         encode();
      }

      private void initProperties() {
         properties = new Properties();
         properties.setTo("queue:1");
         properties.setMessageId("ID:Message:1");
         properties.setCreationTime(new Date(System.currentTimeMillis()));
      }

      @Benchmark
      @Override
      public ByteBuffer encode() {
         return encodeObj(properties);
      }

      @Benchmark
      @Override
      public Object decode() {
         return super.decode();
      }
   }

   public static class MessageAnnotationsBench extends MessageBenchmark {

      private MessageAnnotations annotations;

      @Setup
      public void init() {
         super.init();
         initMessageAnnotations();
         encode();
      }

      private void initMessageAnnotations() {
         annotations = new MessageAnnotations(new HashMap<Symbol, Object>());
         annotations.getValue().put(Symbol.valueOf("test1"), UnsignedByte.valueOf((byte) 128));
         annotations.getValue().put(Symbol.valueOf("test2"), UnsignedShort.valueOf((short) 128));
         annotations.getValue().put(Symbol.valueOf("test3"), UnsignedInteger.valueOf((byte) 128));
      }

      @Benchmark
      @Override
      public ByteBuffer encode() {
         return super.encodeObj(annotations);
      }

      @Benchmark
      @Override
      public Object decode() {
         return super.decode();
      }
   }

   public static class ApplicationPropertiesBench extends MessageBenchmark {

      private ApplicationProperties properties;

      @Setup
      public void init() {
         super.init();
         initApplicationProperties();
         encode();
      }

      private void initApplicationProperties() {
         properties = new ApplicationProperties(new HashMap<String, Object>());
         properties.getValue().put("test1", UnsignedByte.valueOf((byte) 128));
         properties.getValue().put("test2", UnsignedShort.valueOf((short) 128));
         properties.getValue().put("test3", UnsignedInteger.valueOf((byte) 128));
      }

      @Benchmark
      @Override
      public ByteBuffer encode() {
         return encodeObj(properties);
      }

      @Benchmark
      @Override
      public Object decode() {
         return super.decode();
      }
   }

   public static class SymbolsBench extends MessageBenchmark {

      private Symbol symbol1;
      private Symbol symbol2;
      private Symbol symbol3;
      private Blackhole blackhole;

      @Setup
      public void init(Blackhole blackhole) {
         this.blackhole = blackhole;
         super.init();
         initSymbols();
         encode();
      }

      private void initSymbols() {
         symbol1 = Symbol.valueOf("Symbol-1");
         symbol2 = Symbol.valueOf("Symbol-2");
         symbol3 = Symbol.valueOf("Symbol-3");
      }

      @Benchmark
      @Override
      public ByteBuffer encode() {
         byteBuf.clear();
         encoder.writeSymbol(symbol1);
         encoder.writeSymbol(symbol2);
         encoder.writeSymbol(symbol3);
         return byteBuf;
      }

      @Benchmark
      @Override
      public Object decode() {
         byteBuf.flip();
         //these ones are necessary to avoid JVM erase the decode processing/symbol allocations
         blackhole.consume(decoder.readSymbol());
         blackhole.consume(decoder.readSymbol());
         blackhole.consume(decoder.readSymbol());
         return byteBuf;
      }
   }

   public static class DispositionBench extends MessageBenchmark {

      private Disposition disposition;

      @Setup
      public void init() {
         super.init();
         initDisposition();
         encode();
      }

      private void initDisposition() {
         disposition = new Disposition();
         disposition.setRole(Role.RECEIVER);
         disposition.setSettled(true);
         disposition.setState(Accepted.getInstance());
         disposition.setFirst(UnsignedInteger.valueOf(2));
         disposition.setLast(UnsignedInteger.valueOf(2));
      }

      @Benchmark
      @Override
      public ByteBuffer encode() {
         return super.encodeObj(disposition);
      }

      @Benchmark
      @Override
      public Object decode() {
         return super.decode();
      }
   }

   public static class StringsBench extends MessageBenchmark {

      private Blackhole blackhole;
      private String string1;
      private String string2;
      private String string3;

      @Setup
      public void init(Blackhole blackhole) {
         this.blackhole = blackhole;
         super.init();
         initStrings();
         encode();
      }

      private void initStrings() {
         string1 = new String("String-1");
         string2 = new String("String-2");
         string3 = new String("String-3");
      }

      @Benchmark
      @Override
      public ByteBuffer encode() {
         byteBuf.clear();
         encoder.writeString(string1);
         encoder.writeString(string2);
         encoder.writeString(string3);
         return byteBuf;
      }

      @Benchmark
      @Override
      public Object decode() {
         byteBuf.flip();
         blackhole.consume(decoder.readString());
         blackhole.consume(decoder.readString());
         blackhole.consume(decoder.readString());
         return byteBuf;
      }
   }

   public static class DataBench extends MessageBenchmark {

      private Blackhole blackhole;
      private Data data1;
      private Data data2;
      private Data data3;

      @Setup
      public void init(Blackhole blackhole) {
         this.blackhole = blackhole;
         super.init();
         initData();
         encode();
      }

      private void initData() {
         data1 = new Data(new Binary(new byte[]{1, 2, 3}));
         data2 = new Data(new Binary(new byte[]{4, 5, 6}));
         data3 = new Data(new Binary(new byte[]{7, 8, 9}));
      }

      @Benchmark
      @Override
      public ByteBuffer encode() {
         byteBuf.clear();
         encoder.writeObject(data1);
         encoder.writeObject(data2);
         encoder.writeObject(data3);
         return byteBuf;
      }

      @Benchmark
      @Override
      public Object decode() {
         byteBuf.flip();
         blackhole.consume(decoder.readObject());
         blackhole.consume(decoder.readObject());
         blackhole.consume(decoder.readObject());
         return byteBuf;
      }
   }

   public abstract ByteBuffer encode();

   protected final ByteBuffer encodeObj(Object obj) {
      byteBuf.clear();
      encoder.writeObject(obj);
      return byteBuf;
   }

   /**
    * By default it performs a {@link DecoderImpl#readObject()}.
    */
   protected Object decode() {
      byteBuf.flip();
      return decoder.readObject();
   }

   public static void main(String[] args) throws RunnerException {

      final Options opt = new OptionsBuilder()
         .include(MessageBenchmark.class.getSimpleName())
         .addProfiler(GCProfiler.class)
         .shouldDoGC(true)
         .warmupIterations(5)
         .measurementIterations(5)
         .forks(1)
         .build();
      new Runner(opt).run();
   }

}
