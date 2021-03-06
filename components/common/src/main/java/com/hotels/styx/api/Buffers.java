/*
  Copyright (C) 2013-2022 Expedia Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package com.hotels.styx.api;

import io.netty.buffer.ByteBuf;

/**
 * Conversions between Styx Buffer and Netty ByteBuf objects.
 */
public final class Buffers {
    private Buffers() {
    }

    /**
     * Builds a Styx Buffer from Netty ByteBuf.
     *
     * @param byteBuf
     * @return
     */
    public static Buffer fromByteBuf(ByteBuf byteBuf) {
        return new Buffer(byteBuf);
    }

    /**
     * Returns a Netty ByteBuf corresponding to a Styx Buffer.
     *
     * @param buffer
     * @return
     */
    public static ByteBuf toByteBuf(Buffer buffer) {
        return buffer.delegate();
    }
}
