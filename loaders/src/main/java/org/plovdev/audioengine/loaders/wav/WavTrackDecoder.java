package org.plovdev.audioengine.loaders.wav;

import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.format.AudioFormat;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.TrackDecoder;
import org.plovdev.audioengine.loaders.wav.decoders.ALawDecoder;
import org.plovdev.audioengine.loaders.wav.decoders.CoderUtils;
import org.plovdev.audioengine.loaders.wav.decoders.IMAADPCMDecoder;
import org.plovdev.audioengine.loaders.wav.decoders.ULawDecoder;
import org.plovdev.audioengine.metadata.TrackMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;

import static org.plovdev.audioengine.format.TrackFormat.AudioCodec.*;

public class WavTrackDecoder implements TrackDecoder {
    private static final List<TrackFormat.AudioCodec> allowFromDecodeCodecs = List.of(PCM8, PCM16, PCM24, PCM32, FLOAT32, FLOAT64, ULAW, ALAW, IMA_ADPCM, MIC_ADPCM, GSM_6);
    private static final Logger log = LoggerFactory.getLogger(WavTrackDecoder.class);

    @Override
    public Track decode(Track input, @NonNull TrackFormat inputFormat) {
        TrackFormat.AudioCodec inCodec = inputFormat.audioCodec();

        if (!allowFromDecodeCodecs.contains(inCodec)) {
            log.warn("Unsupported codec.");
            return input;
        }

        Duration inputDuration = input.getDuration();
        TrackMetadata metadata = input.getMetaData();

        ByteBuffer trackData = input.getTrackData();
        AudioFormat outFormat = inputFormat.toAudioFormat();
        return switch (inCodec) {
            case ULAW -> new Track(new ULawDecoder().decode(trackData), inputDuration, outFormat, metadata);
            case ALAW -> new Track(new ALawDecoder().decode(trackData), inputDuration, outFormat, metadata);
            case IMA_ADPCM ->
                    new Track(new IMAADPCMDecoder(1024).decode(trackData), inputDuration, outFormat, metadata);
            default -> new Track(CoderUtils.decodeToFloat32(trackData, inCodec), inputDuration, outFormat, metadata);
        };
    }
}