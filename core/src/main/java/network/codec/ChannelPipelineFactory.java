package network.codec;

import com.esotericsoftware.kryo.Kryo;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

public class ChannelPipelineFactory {
    private ChannelPipelineFactory() {
        /* This utility class should not be instantiated */
    }

    public static void addCodecs(ChannelPipeline pipeline, Kryo kryo) {
        pipeline.addLast(new LengthFieldPrepender(4));
        pipeline.addLast(new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
        pipeline.addLast(new KryoDecoder(kryo));
        pipeline.addLast(new KryoEncoder(kryo));
    }
}