/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.qpid.proton.codec.transport;

import java.util.Collection;

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.UnsignedLong;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.amqp.transport.Disposition;
import org.apache.qpid.proton.amqp.transport.Role;
import org.apache.qpid.proton.codec.AMQPType;
import org.apache.qpid.proton.codec.BuiltinDescribedTypeConstructor;
import org.apache.qpid.proton.codec.DecodeException;
import org.apache.qpid.proton.codec.Decoder;
import org.apache.qpid.proton.codec.DecoderImpl;
import org.apache.qpid.proton.codec.EncoderImpl;
import org.apache.qpid.proton.codec.EncodingCodes;
import org.apache.qpid.proton.codec.TypeEncoding;
import org.apache.qpid.proton.codec.WritableBuffer;

public class BuiltinDispositionType implements AMQPType<Disposition>, BuiltinDescribedTypeConstructor<Disposition> {

    private static final Object[] DESCRIPTORS =
    {
        UnsignedLong.valueOf(0x0000000000000015L), Symbol.valueOf("amqp:disposition:list"),
    };

    private final DispositionType dispositionType;

    public BuiltinDispositionType(EncoderImpl encoder) {
        this.dispositionType = new DispositionType(encoder);
    }

    public EncoderImpl getEncoder() {
        return dispositionType.getEncoder();
    }

    public DecoderImpl getDecoder() {
        return dispositionType.getDecoder();
    }

    @Override
    public boolean encodesJavaPrimitive() {
        return false;
    }

    @Override
    public Class<Disposition> getTypeClass() {
        return Disposition.class;
    }

    @Override
    public TypeEncoding<Disposition> getEncoding(Disposition disposition) {
        return dispositionType.getEncoding(disposition);
    }

    @Override
    public TypeEncoding<Disposition> getCanonicalEncoding() {
        return dispositionType.getCanonicalEncoding();
    }

    @Override
    public Collection<? extends TypeEncoding<Disposition>> getAllEncodings() {
        return dispositionType.getAllEncodings();
    }

    @Override
    public Disposition readValue() {
        DecoderImpl decoder = getDecoder();
        byte typeCode = decoder.getByteBuffer().get();

        @SuppressWarnings("unused")
        int size = 0;
        int count = 0;

        switch (typeCode) {
            case EncodingCodes.LIST0:
                break;
            case EncodingCodes.LIST8:
                size = ((int)decoder.getByteBuffer().get()) & 0xff;
                count = ((int)decoder.getByteBuffer().get()) & 0xff;
                break;
            case EncodingCodes.LIST32:
                size = decoder.getByteBuffer().getInt();
                count = decoder.getByteBuffer().getInt();
                break;
            default:
                throw new DecodeException("Incorrect type found in Disposition encoding: " + typeCode);
        }

        Disposition disposition = new Disposition();

        for (int index = 0; index < count; ++index) {
            switch (index) {
                case 0:
                    disposition.setRole(Boolean.TRUE.equals(decoder.readBoolean()) ? Role.RECEIVER : Role.SENDER);
                    break;
                case 1:
                    disposition.setFirst(decoder.readUnsignedInteger());
                    break;
                case 2:
                    disposition.setLast(decoder.readUnsignedInteger());
                    break;
                case 3:
                    disposition.setSettled(Boolean.TRUE.equals(decoder.readBoolean()));
                    break;
                case 4:
                    disposition.setState((DeliveryState) decoder.readObject());
                    break;
                case 5:
                    disposition.setBatchable(Boolean.TRUE.equals(decoder.readBoolean()));
                    break;
                default:
                    throw new IllegalStateException("To many entries in Disposition encoding");
            }
        }

        return disposition;
    }

    @Override
    public void write(Disposition disposition) {
        WritableBuffer buffer = getEncoder().getBuffer();
        int count = getElementCount(disposition);
        byte encodingCode = deduceEncodingCode(disposition, count);

        buffer.put(EncodingCodes.DESCRIBED_TYPE_INDICATOR);
        getEncoder().writeUnsignedLong(dispositionType.getDescriptor());

        // Optimized step, no other data to be written.
        if (count == 0 || encodingCode == EncodingCodes.LIST0) {
            buffer.put(EncodingCodes.LIST0);
            return;
        }

        final int fieldWidth;

        if (encodingCode == EncodingCodes.LIST8) {
            fieldWidth = 1;
            buffer.put(EncodingCodes.LIST8);
        } else {
            fieldWidth = 4;
            buffer.put(EncodingCodes.LIST32);
        }

        int startIndex = buffer.position();

        // Reserve space for the size and write the count of list elements.
        if (fieldWidth == 1) {
            buffer.put((byte) 0);
            buffer.put((byte) count);
        } else {
            buffer.putInt(0);
            buffer.putInt(count);
        }

        // Write the list elements and then compute total size written.
        for (int i = 0; i < count; ++i) {
            writeElement(disposition, i);
        }

        // Move back and write the size
        int endIndex = buffer.position();
        int writeSize = endIndex - startIndex - fieldWidth;

        buffer.position(startIndex);
        if (fieldWidth == 1) {
            buffer.put((byte) writeSize);
        } else {
            buffer.putInt(writeSize);
        }
        buffer.position(endIndex);
    }

    private void writeElement(Disposition disposition, int index) {
        switch (index) {
            case 0:
                getEncoder().writeBoolean(disposition.getRole().getValue());
                break;
            case 1:
                getEncoder().writeUnsignedInteger(disposition.getFirst());
                break;
            case 2:
                getEncoder().writeObject(disposition.getLast());
                break;
            case 3:
                getEncoder().writeBoolean(disposition.getSettled());
                break;
            case 4:
                getEncoder().writeObject(disposition.getState());
                break;
            case 5:
                getEncoder().writeBoolean(disposition.getBatchable());
                break;
            default:
                throw new IllegalArgumentException("Unknown Disposition value index: " + index);
        }
    }

    private int getElementCount(Disposition disposition) {
        if (disposition.getBatchable()) {
            return 6;
        } else if (disposition.getState() != null) {
            return 5;
        } else if (disposition.getSettled()) {
            return 4;
        } else if (disposition.getLast() != null) {
            return 3;
        } else {
            return 2;
        }
    }

    private byte deduceEncodingCode(Disposition value, int elementCount) {
        if (elementCount == 0) {
            return EncodingCodes.LIST0;
        } else if (value.getState() == null) {
            return EncodingCodes.LIST8;
        } else if (value.getState() instanceof Accepted || value.getState() instanceof Released) {
            return EncodingCodes.LIST8;
        } else {
            return EncodingCodes.LIST32;
        }
    }

    public static void register(Decoder decoder, EncoderImpl encoder) {
        BuiltinDispositionType type = new BuiltinDispositionType(encoder);
        for(Object descriptor : DESCRIPTORS) {
            decoder.register(descriptor, (BuiltinDescribedTypeConstructor<?>) type);
        }
        encoder.register(type);
    }
}
