package network.codec;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;

import io.netty.channel.ChannelHandlerContext;

import io.netty.handler.codec.MessageToByteEncoder;

public class KryoEncoder extends MessageToByteEncoder<Object> {
    private final Kryo kryo;
    public KryoEncoder(Kryo kryo) { this.kryo = kryo; }
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        try (ByteBufOutputStream bbos = new ByteBufOutputStream(out);
             Output output = new Output(bbos)) {
            kryo.writeObject(output, msg);
            output.flush();
        }
    }
}