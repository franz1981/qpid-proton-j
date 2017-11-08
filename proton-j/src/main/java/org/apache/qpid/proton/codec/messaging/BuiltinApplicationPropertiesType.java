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
package org.apache.qpid.proton.codec.messaging;

import java.util.Collection;

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.UnsignedLong;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.codec.AMQPType;
import org.apache.qpid.proton.codec.BuiltinDescribedTypeConstructor;
import org.apache.qpid.proton.codec.Decoder;
import org.apache.qpid.proton.codec.DecoderImpl;
import org.apache.qpid.proton.codec.EncoderImpl;
import org.apache.qpid.proton.codec.EncodingCodes;
import org.apache.qpid.proton.codec.TypeEncoding;
import org.apache.qpid.proton.codec.WritableBuffer;

public class BuiltinApplicationPropertiesType implements AMQPType<ApplicationProperties>, BuiltinDescribedTypeConstructor<ApplicationProperties> {

    private static final Object[] DESCRIPTORS =
    {
        UnsignedLong.valueOf(0x0000000000000074L), Symbol.valueOf("amqp:application-properties:map"),
    };

    private final ApplicationPropertiesType propertiesType;

    public BuiltinApplicationPropertiesType(EncoderImpl encoder) {
        this.propertiesType = new ApplicationPropertiesType(encoder);
    }

    public EncoderImpl getEncoder() {
        return propertiesType.getEncoder();
    }

    public DecoderImpl getDecoder() {
        return propertiesType.getDecoder();
    }

    @Override
    public boolean encodesJavaPrimitive() {
        return false;
    }

    @Override
    public Class<ApplicationProperties> getTypeClass() {
        return ApplicationProperties.class;
    }

    @Override
    public TypeEncoding<ApplicationProperties> getEncoding(ApplicationProperties val) {
        return propertiesType.getEncoding(val);
    }

    @Override
    public TypeEncoding<ApplicationProperties> getCanonicalEncoding() {
        return propertiesType.getCanonicalEncoding();
    }

    @Override
    public Collection<? extends TypeEncoding<ApplicationProperties>> getAllEncodings() {
        return propertiesType.getAllEncodings();
    }

    @Override
    public ApplicationProperties readValue() {
        return new ApplicationProperties(getDecoder().readMap());
    }

    @Override
    public void write(ApplicationProperties val) {
        WritableBuffer buffer = getEncoder().getBuffer();

        buffer.put(EncodingCodes.DESCRIBED_TYPE_INDICATOR);
        getEncoder().writeUnsignedLong(propertiesType.getDescriptor());
        getEncoder().writeMap(val.getValue());
    }

    public static void register(Decoder decoder, EncoderImpl encoder) {
        BuiltinApplicationPropertiesType type = new BuiltinApplicationPropertiesType(encoder);
        for(Object descriptor : DESCRIPTORS)
        {
            decoder.register(descriptor, type);
        }
        encoder.register(type);
    }
}