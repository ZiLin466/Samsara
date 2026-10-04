package com.samsara.util.render;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import static org.lwjgl.BufferUtils.createByteBuffer;
import static org.lwjgl.system.MemoryUtil.memSlice;

public final class IOUtility {

   private IOUtility() {
   }

   public static ByteBuffer ioResourceToByteBuffer(InputStream inputStream, int bufferSize) {
      try {
         ByteBuffer buffer;

         if (inputStream != null) {
            try (ReadableByteChannel rbc = Channels.newChannel(inputStream)) {
               buffer = createByteBuffer(bufferSize);

               while (true) {
                  int bytes = rbc.read(buffer);
                  if (bytes == -1) {
                     break;
                  }
                  if (buffer.remaining() == 0) {
                     buffer = resizeBuffer(buffer, buffer.capacity() * 3 / 2);
                  }
               }
            }
         } else {
            throw new IllegalArgumentException("InputStream cannot be null");
         }

         buffer.flip();
         return memSlice(buffer);
      } catch (IOException e) {
         e.printStackTrace();
         return null;
      }
   }

   private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
      ByteBuffer newBuffer = createByteBuffer(newCapacity);
      buffer.flip();
      newBuffer.put(buffer);
      return newBuffer;
   }
}
