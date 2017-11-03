/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.proton.codec;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.UnsignedByte;
import org.apache.qpid.proton.amqp.UnsignedInteger;
import org.apache.qpid.proton.amqp.UnsignedShort;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Header;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.amqp.messaging.Properties;
import org.apache.qpid.proton.amqp.transport.Disposition;
import org.apache.qpid.proton.amqp.transport.Flow;
import org.apache.qpid.proton.amqp.transport.Role;
import org.apache.qpid.proton.amqp.transport.Transfer;

public class Benchmark implements Runnable {

    private static final int ITERATIONS = 10 * 1024 * 1024;

    private ByteBuffer byteBuf = ByteBuffer.allocate(8192);
    private BenchmarkResult resultSet = new BenchmarkResult();
    private boolean warming = true;

    private final DecoderImpl decoder = new DecoderImpl();
    private final EncoderImpl encoder = new EncoderImpl(decoder);

    public static final void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Current PID: " + ManagementFactory.getRuntimeMXBean().getName());
        Benchmark benchmark = new Benchmark();
        benchmark.run();
    }

    @Override
    public void run() {
        AMQPDefinedTypes.registerAllTypes(decoder, encoder);

        encoder.setByteBuffer(byteBuf);
        decoder.setByteBuffer(byteBuf);

        try {
            doBenchmarks();
            warming = false;
            doBenchmarks();
        } catch (IOException e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }
    }

    private void time(String message, BenchmarkResult resultSet) {
        if (!warming) {
            System.out.println("Benchamrk of type: " + message + ": ");
            System.out.println("    Encode time = " + resultSet.getEncodeTimeMills());
            System.out.println("    Decode time = " + resultSet.getDecodeTimeMills());
        }
    }

    private final void doBenchmarks() throws IOException {
        benchmarkListOfInts();
        benchmarkUUIDs();
        benchmarkHeader();
        benchmarkProperties();
        benchmarkMessageAnnotations();
        benchmarkApplicationProperties();
        benchmarkSymbols();
        benchmarkTransfer();
        benchmarkFlow();
        benchmarkDisposition();
        benchmarkString();
        warming = false;
    }

    private void benchmarkListOfInts() throws IOException {
        ArrayList<Object> list = new ArrayList<>(10);
        for (int j = 0; j < 10; j++) {
            list.add(0);
        }

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.clear();
            encoder.writeList(list);
        }
        resultSet.encodesComplete();

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.flip();
            decoder.readList();
        }
        resultSet.decodesComplete();

        time("List<Integer>", resultSet);
    }

    private void benchmarkUUIDs() throws IOException {
        UUID uuid = UUID.randomUUID();

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.clear();
            encoder.writeUUID(uuid);
        }
        resultSet.encodesComplete();

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.flip();
            decoder.readUUID();
        }
        resultSet.decodesComplete();

        time("UUID", resultSet);
    }

    private void benchmarkHeader() throws IOException {
        Header header = new Header();
        header.setDurable(true);
        header.setFirstAcquirer(true);

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.clear();
            encoder.writeObject(header);
        }
        resultSet.encodesComplete();

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.flip();
            decoder.readObject();
        }
        resultSet.decodesComplete();

        time("Header", resultSet);
    }

    private void benchmarkTransfer() throws IOException {
        Transfer transfer = new Transfer();
        transfer.setDeliveryTag(new Binary(new byte[] {1, 2, 3}));
        transfer.setHandle(UnsignedInteger.valueOf(10));
        transfer.setMessageFormat(UnsignedInteger.ZERO);

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.clear();
            encoder.writeObject(transfer);
        }
        resultSet.encodesComplete();

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.flip();
            decoder.readObject();
        }
        resultSet.decodesComplete();

        time("Transfer", resultSet);
    }

    private void benchmarkFlow() throws IOException {
        Flow flow = new Flow();
        flow.setNextIncomingId(UnsignedInteger.valueOf(1));
        flow.setIncomingWindow(UnsignedInteger.valueOf(2047));
        flow.setNextOutgoingId(UnsignedInteger.valueOf(1));
        flow.setOutgoingWindow(UnsignedInteger.MAX_VALUE);
        flow.setHandle(UnsignedInteger.ZERO);
        flow.setDeliveryCount(UnsignedInteger.valueOf(10));
        flow.setLinkCredit(UnsignedInteger.valueOf(1000));

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.clear();
            encoder.writeObject(flow);
        }
        resultSet.encodesComplete();

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.flip();
            decoder.readObject();
        }
        resultSet.decodesComplete();

        time("Flow", resultSet);
    }

    private void benchmarkProperties() throws IOException {
        Properties properties = new Properties();
        properties.setTo("queue:1");
        properties.setMessageId("ID:Message:1");
        properties.setCreationTime(new Date(System.currentTimeMillis()));

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.clear();
            encoder.writeObject(properties);
        }
        resultSet.encodesComplete();

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.flip();
            decoder.readObject();
        }
        resultSet.decodesComplete();

        time("Properties", resultSet);
    }

    private void benchmarkMessageAnnotations() throws IOException {
        MessageAnnotations annotations = new MessageAnnotations(new HashMap<Symbol, Object>());
        annotations.getValue().put(Symbol.valueOf("test1"), UnsignedByte.valueOf((byte) 128));
        annotations.getValue().put(Symbol.valueOf("test2"), UnsignedShort.valueOf((short) 128));
        annotations.getValue().put(Symbol.valueOf("test3"), UnsignedInteger.valueOf((byte) 128));

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.clear();
            encoder.writeObject(annotations);
        }
        resultSet.encodesComplete();

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.flip();
            decoder.readObject();
        }
        resultSet.decodesComplete();

        time("MessageAnnotations", resultSet);
    }

    @SuppressWarnings("unchecked")
    private void benchmarkApplicationProperties() throws IOException {
        ApplicationProperties properties = new ApplicationProperties(new HashMap<String, Object>());
        properties.getValue().put("test1", UnsignedByte.valueOf((byte) 128));
        properties.getValue().put("test2", UnsignedShort.valueOf((short) 128));
        properties.getValue().put("test3", UnsignedInteger.valueOf((byte) 128));

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.clear();
            encoder.writeObject(properties);
        }
        resultSet.encodesComplete();

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.flip();
            decoder.readObject();
        }
        resultSet.decodesComplete();

        time("ApplicationProperties", resultSet);
    }

    private void benchmarkSymbols() throws IOException {
        Symbol symbol1 = Symbol.valueOf("Symbol-1");
        Symbol symbol2 = Symbol.valueOf("Symbol-2");
        Symbol symbol3 = Symbol.valueOf("Symbol-3");

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.clear();
            encoder.writeSymbol(symbol1);
            encoder.writeSymbol(symbol2);
            encoder.writeSymbol(symbol3);
        }
        resultSet.encodesComplete();

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.flip();
            decoder.readSymbol();
            decoder.readSymbol();
            decoder.readSymbol();
        }
        resultSet.decodesComplete();

        time("Symbol", resultSet);
    }

    private void benchmarkDisposition() throws IOException {
        Disposition disposition = new Disposition();
        disposition.setRole(Role.RECEIVER);
        disposition.setSettled(true);
        disposition.setState(Accepted.getInstance());
        disposition.setFirst(UnsignedInteger.valueOf(2));
        disposition.setLast(UnsignedInteger.valueOf(2));

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.clear();
            encoder.writeObject(disposition);
        }
        resultSet.encodesComplete();

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.flip();
            decoder.readObject();
        }
        resultSet.decodesComplete();

        time("Disposition", resultSet);
    }

    private void benchmarkString() throws IOException {
        String string1 = new String("String-1");
        String string2 = new String("String-2");
        String string3 = new String("String-3");

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.clear();
            encoder.writeString(string1);
            encoder.writeString(string2);
            encoder.writeString(string3);
        }
        resultSet.encodesComplete();

        resultSet.start();
        for (int i = 0; i < ITERATIONS; i++) {
            byteBuf.flip();
            decoder.readString();
            decoder.readString();
            decoder.readString();
        }
        resultSet.decodesComplete();

        time("String", resultSet);
    }

    private static class BenchmarkResult {

        private long startTime;

        private long encodeTime;
        private long decodeTime;

        public void start() {
            startTime = System.nanoTime();
        }

        public void encodesComplete() {
            encodeTime = System.nanoTime() - startTime;
        }

        public void decodesComplete() {
            decodeTime = System.nanoTime() - startTime;
        }

        public long getEncodeTimeMills() {
            return TimeUnit.NANOSECONDS.toMillis(encodeTime);
        }

        public long getDecodeTimeMills() {
            return TimeUnit.NANOSECONDS.toMillis(decodeTime);
        }
    }
}
