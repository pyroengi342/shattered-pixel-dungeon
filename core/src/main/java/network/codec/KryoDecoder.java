package network.codec;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import network.NetworkManager;

public class KryoDecoder extends ByteToMessageDecoder {
    private final Kryo kryo;
    public KryoDecoder(Kryo kryo) { this.kryo = kryo; }
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try (ByteBufInputStream bbis = new ByteBufInputStream(in);
             Input input = new Input(bbis)) {
            out.add(kryo.readObject(input, NetworkManager.BundleMessage.class));
        }
    }
}