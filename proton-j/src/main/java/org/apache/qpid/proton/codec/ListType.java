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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ListType extends AbstractPrimitiveType<List>
{
    private final ListEncoding _listEncoding;
    private final ListEncoding _shortListEncoding;
    private final ListEncoding _zeroListEncoding;
    private EncoderImpl _encoder;

    private static interface ListEncoding extends PrimitiveTypeEncoding<List>
    {
        void setValue(List value, int length);
    }

    ListType(final EncoderImpl encoder, final DecoderImpl decoder)
    {
        _encoder = encoder;
        _listEncoding = new AllListEncoding(encoder, decoder);
        _shortListEncoding = new ShortListEncoding(encoder, decoder);
        _zeroListEncoding = new ZeroListEncoding(encoder, decoder);
        encoder.register(List.class, this);
        decoder.register(this);
    }

    public Class<List> getTypeClass()
    {
        return List.class;
    }

    public ListEncoding getEncoding(final List val)
    {
        int calculatedSize = calculateSize(val, _encoder);
        ListEncoding encoding = val.isEmpty()
                                    ? _zeroListEncoding
                                    : (val.size() > 255 || calculatedSize >= 254)
                                        ? _listEncoding
                                        : _shortListEncoding;

        encoding.setValue(val, calculatedSize);
        return encoding;
    }

    private static int calculateSize(final List val, EncoderImpl encoder)
    {
        int len = 0;
        final int count = val.size();

        AMQPType lastType = null;

        for(int i = 0; i < count; i++)
        {
            Object element = val.get(i);
            if (element == null)
            {
                lastType = encoder.getNullTypeEncoder();
            }
            else if (lastType == null || !lastType.getTypeClass().equals(element.getClass()))
            {
                lastType = encoder.getType(element);
            }

            if (lastType == null)
            {
                throw new IllegalArgumentException("No encoding defined for type: " + element.getClass());
            }
            TypeEncoding elementEncoding = lastType.getEncoding(element);
            len += elementEncoding.getConstructorSize() + elementEncoding.getValueSize(element);
        }
        return len;
    }

    public ListEncoding getCanonicalEncoding()
    {
        return _listEncoding;
    }

    public Collection<ListEncoding> getAllEncodings()
    {
        return Arrays.asList(_zeroListEncoding, _shortListEncoding, _listEncoding);
    }

    private class AllListEncoding
            extends LargeFloatingSizePrimitiveTypeEncoding<List>
            implements ListEncoding
    {

        private List _value;
        private int _length;

        public AllListEncoding(final EncoderImpl encoder, final DecoderImpl decoder)
        {
            super(encoder, decoder);
        }

        @Override
        protected void writeEncodedValue(final List val)
        {
            getEncoder().writeRaw(val.size());

            final int count = val.size();

            AMQPType lastType = null;

            for(int i = 0; i < count; i++)
            {
                Object element = val.get(i);
                if (element == null)
                {
                    lastType = getEncoder().getNullTypeEncoder();
                }
                else if (lastType == null || !lastType.getTypeClass().equals(element.getClass()))
                {
                    lastType = getEncoder().getType(element);
                }

                TypeEncoding elementEncoding = lastType.getEncoding(element);
                elementEncoding.writeConstructor();
                elementEncoding.writeValue(element);
            }
        }

        @Override
        protected int getEncodedValueSize(final List val)
        {
            return 4 + ((val == _value) ? _length : calculateSize(val, getEncoder()));
        }


        @Override
        public byte getEncodingCode()
        {
            return EncodingCodes.LIST32;
        }

        public ListType getType()
        {
            return ListType.this;
        }

        public boolean encodesSuperset(final TypeEncoding<List> encoding)
        {
            return (getType() == encoding.getType());
        }

        public List readValue()
        {
            DecoderImpl decoder = getDecoder();
            ByteBuffer buffer = decoder.getByteBuffer();

            int size = decoder.readRawInt();
            // todo - limit the decoder with size
            int count = decoder.readRawInt();
            // Ensure we do not allocate an array of size greater then the available data, otherwise there is a risk for an OOM error
            if (count > decoder.getByteBufferRemaining()) {
                throw new IllegalArgumentException("List element count "+count+" is specified to be greater than the amount of data available ("+
                                                   decoder.getByteBufferRemaining()+")");
            }

            TypeConstructor<?> typeConstructor = null;

            List<Object> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++)
            {
                boolean arrayType = false;
                byte code = buffer.get(buffer.position());
                switch (code)
                {
                    case EncodingCodes.ARRAY8:
                    case EncodingCodes.ARRAY32:
                        arrayType = true;
                }

                // Whenever we can just reuse the previously used TypeDecoder instead
                // of spending time looking up the same one again.
                if (typeConstructor == null)
                {
                    typeConstructor = getDecoder().readConstructor();
                }
                else
                {
                    buffer.mark();

                    byte encodingCode = buffer.get();
                    if (encodingCode == EncodingCodes.DESCRIBED_TYPE_INDICATOR || !(typeConstructor instanceof PrimitiveTypeEncoding<?>))
                    {
                        buffer.reset();
                        typeConstructor = getDecoder().readConstructor();
                    }
                    else
                    {
                        PrimitiveTypeEncoding<?> primitiveConstructor = (PrimitiveTypeEncoding<?>) typeConstructor;
                        if (encodingCode != primitiveConstructor.getEncodingCode())
                        {
                            buffer.reset();
                            typeConstructor = getDecoder().readConstructor();
                        }
                    }
                }

                if(typeConstructor == null)
                {
                    throw new DecodeException("Unknown constructor");
                }

                final Object value;

                if (arrayType)
                {
                    value = ((ArrayType.ArrayEncoding) typeConstructor).readValueArray();
                }
                else
                {
                    value = typeConstructor.readValue();
                }

                list.add(value);
            }

            return list;
        }

        public void setValue(final List value, final int length)
        {
            _value = value;
            _length = length;
        }
    }

    private class ShortListEncoding
            extends SmallFloatingSizePrimitiveTypeEncoding<List>
            implements ListEncoding
    {

        private List _value;
        private int _length;

        public ShortListEncoding(final EncoderImpl encoder, final DecoderImpl decoder)
        {
            super(encoder, decoder);
        }

        @Override
        protected void writeEncodedValue(final List val)
        {
            getEncoder().writeRaw((byte)val.size());

            final int count = val.size();

            AMQPType lastType = null;

            for(int i = 0; i < count; i++)
            {
                Object element = val.get(i);
                if (element == null)
                {
                    lastType = getEncoder().getNullTypeEncoder();
                }
                else if (lastType == null || !lastType.getTypeClass().equals(element.getClass()))
                {
                    lastType = getEncoder().getType(element);
                }

                TypeEncoding elementEncoding = lastType.getEncoding(element);
                elementEncoding.writeConstructor();
                elementEncoding.writeValue(element);
            }
        }

        @Override
        protected int getEncodedValueSize(final List val)
        {
            return 1 + ((val == _value) ? _length : calculateSize(val, getEncoder()));
        }


        @Override
        public byte getEncodingCode()
        {
            return EncodingCodes.LIST8;
        }

        public ListType getType()
        {
            return ListType.this;
        }

        public boolean encodesSuperset(final TypeEncoding<List> encoder)
        {
            return encoder == this;
        }

        public List readValue()
        {
            DecoderImpl decoder = getDecoder();
            ByteBuffer buffer = decoder.getByteBuffer();

            int size = ((int)decoder.readRawByte()) & 0xff;
            // todo - limit the decoder with size
            int count = ((int)decoder.readRawByte()) & 0xff;

            TypeConstructor<?> typeConstructor = null;

            List<Object> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++)
            {
                boolean arrayType = false;
                byte code = buffer.get(buffer.position());
                switch (code)
                {
                    case EncodingCodes.ARRAY8:
                    case EncodingCodes.ARRAY32:
                        arrayType = true;
                }

                // Whenever we can just reuse the previously used TypeDecoder instead
                // of spending time looking up the same one again.
                if (typeConstructor == null)
                {
                    typeConstructor = getDecoder().readConstructor();
                }
                else
                {
                    buffer.mark();

                    byte encodingCode = buffer.get();
                    if (encodingCode == EncodingCodes.DESCRIBED_TYPE_INDICATOR || !(typeConstructor instanceof PrimitiveTypeEncoding<?>))
                    {
                        buffer.reset();
                        typeConstructor = getDecoder().readConstructor();
                    }
                    else
                    {
                        PrimitiveTypeEncoding<?> primitiveConstructor = (PrimitiveTypeEncoding<?>) typeConstructor;
                        if (encodingCode != primitiveConstructor.getEncodingCode())
                        {
                            buffer.reset();
                            typeConstructor = getDecoder().readConstructor();
                        }
                    }
                }

                if (typeConstructor == null)
                {
                    throw new DecodeException("Unknown constructor");
                }

                final Object value;

                if (arrayType)
                {
                    value = ((ArrayType.ArrayEncoding) typeConstructor).readValueArray();
                }
                else
                {
                    value = typeConstructor.readValue();
                }

                list.add(value);
            }

            return list;
        }

        public void setValue(final List value, final int length)
        {
            _value = value;
            _length = length;
        }
    }


    private class ZeroListEncoding
            extends FixedSizePrimitiveTypeEncoding<List>
            implements ListEncoding
    {
        public ZeroListEncoding(final EncoderImpl encoder, final DecoderImpl decoder)
        {
            super(encoder, decoder);
        }

        @Override
        public byte getEncodingCode()
        {
            return EncodingCodes.LIST0;
        }

        @Override
        protected int getFixedSize()
        {
            return 0;
        }


        public ListType getType()
        {
           return ListType.this;
        }

        public void setValue(List value, int length)
        {
        }

        public void writeValue(final List val)
        {
        }

        public boolean encodesSuperset(final TypeEncoding<List> encoder)
        {
            return encoder == this;
        }

        public List readValue()
        {
            return Collections.EMPTY_LIST;
        }


    }
}
