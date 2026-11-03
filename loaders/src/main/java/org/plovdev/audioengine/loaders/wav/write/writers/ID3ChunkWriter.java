package org.plovdev.audioengine.loaders.wav.write.writers;

import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.metadata.MetaKey;
import org.plovdev.audioengine.metadata.TrackMetadata;
import org.plovdev.audioengine.metadata.image.TrackImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.plovdev.audioengine.loaders.ExportUtils.intToLittleEndian;

public class ID3ChunkWriter implements WavChunkWriter<TrackMetadata> {
    private static final Logger log = LoggerFactory.getLogger(ID3ChunkWriter.class);

    @Override
    public Class<?> getType() {
        return TrackMetadata.class;
    }

    @Override
    public void write(OutputStream stream, TrackFormat outputFormat, TrackMetadata metadata) {
        try {
            ByteArrayOutputStream id3Data = new ByteArrayOutputStream();

            id3Data.write("ID3".getBytes());      // ID3 signature (3 байта)
            id3Data.write(0x03);                  // Version 2.3
            id3Data.write(0x00);                  // Revision
            id3Data.write(0x00);                  // Flags

            int sizePos = id3Data.size();
            id3Data.write(new byte[4]);

            int dataSize = 0;

            dataSize += writeId3TextFrame(id3Data, MetaKey.TITLE, metadata.getTitle().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.ARTIST, metadata.getArtist().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.ALBUM, metadata.getAlbum().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.ALBUM_ARTIST, metadata.getArtist().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.YEAR, metadata.getYear().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.TRACK_NUMBER, metadata.getTrackNumber().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.GENRE, metadata.getGenre().orElse(null));

            dataSize += writeId3TextFrame(id3Data, MetaKey.COMPOSER, metadata.getComposer().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.LYRICIST, metadata.getLyricist().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.PUBLISHER, metadata.getPublisher().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.BPM, metadata.getBpm().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.KEY, metadata.getKey().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.MOOD, metadata.getMood().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.ISRC, metadata.getIsrc().orElse(null));

            dataSize += writeId3TextFrame(id3Data, MetaKey.ENCODER, metadata.getEncoder().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.LANGUAGE, metadata.getLanguage().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.COPYRIGHT, metadata.getCopyright().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.COMMENT, metadata.getComment().orElse(null));

            dataSize += writeId3TextFrame(id3Data, MetaKey.DISC_NUMBER, metadata.getDiscNumber().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.DISC_TOTAL, metadata.getDiscTotal().orElse(null));
            dataSize += writeId3TextFrame(id3Data, MetaKey.TRACK_TOTAL, metadata.getTrackTotal().orElse(null));

            dataSize += writeId3TextFrame(id3Data, metadata.getAlbumImage().orElse(null));

            byte[] allId3Data = id3Data.toByteArray();
            byte[] syncSafeSize = intToSyncSafe(dataSize);
            System.arraycopy(syncSafeSize, 0, allId3Data, sizePos, 4);

            stream.write("ID3 ".getBytes(StandardCharsets.US_ASCII));
            stream.write(intToLittleEndian(allId3Data.length));
            stream.write(allId3Data);

            if (allId3Data.length % 2 != 0) {
                stream.write(0);
            }
        } catch (Exception e) {
            log.error("Error writing ID3 chunk", e);
        }
    }

    private int writeId3TextFrame(ByteArrayOutputStream bos, MetaKey frameId, Date value) {
        if (value == null) {
            return 0;
        }
        return writeId3TextFrame(bos, frameId, String.valueOf(value.getYear()));
    }

    private int writeId3TextFrame(ByteArrayOutputStream bos, MetaKey frameId, Integer value) {
        if (value == null) {
            return 0;
        }
        return writeId3TextFrame(bos, frameId, String.valueOf(value));
    }

    private int writeId3TextFrame(ByteArrayOutputStream bos, MetaKey frameId, Float value) {
        if (value == null) {
            return 0;
        }
        return writeId3TextFrame(bos, frameId, String.valueOf(value));
    }

    private int writeId3TextFrame(ByteArrayOutputStream bos, MetaKey frameId, String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            byte[] textBytes = value.getBytes(StandardCharsets.ISO_8859_1);
            int frameSize = 1 + textBytes.length + 1;
            bos.write(frameId.getKey().getBytes(StandardCharsets.ISO_8859_1));

            bos.write((frameSize >> 24) & 0xFF);
            bos.write((frameSize >> 16) & 0xFF);
            bos.write((frameSize >> 8) & 0xFF);
            bos.write(frameSize & 0xFF);

            bos.write(0x00);
            bos.write(0x00);
            bos.write(0x00);

            bos.write(textBytes);
            bos.write(0x00);

            return 10 + frameSize;
        } catch (Exception e) {
            log.error("Error to write id3 tag: ", e);
            return 0;
        }
    }

    private int writeId3TextFrame(ByteArrayOutputStream bos, TrackImage image) {
        if (image == null) return 0;
        try {
            ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
            byte[] imageData = imageBytes.toByteArray();

            ByteArrayOutputStream frameData = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(frameData);

            dos.writeByte(0x00);
            dos.write(image.mimeType().toString().getBytes(StandardCharsets.ISO_8859_1));
            dos.writeByte(0x00);
            dos.writeByte(0x03);
            dos.writeByte(0x00);

            dos.write(imageData);

            String type = image.mimeType().toString().substring(image.mimeType().toString().lastIndexOf("/") + 1);
            ImageIO.write(image.image(), type.toUpperCase(), frameData);

            byte[] fullFrameData = frameData.toByteArray();
            int frameSize = fullFrameData.length;

            bos.write("APIC".getBytes(StandardCharsets.ISO_8859_1));
            bos.write((frameSize >> 24) & 0xFF);
            bos.write((frameSize >> 16) & 0xFF);
            bos.write((frameSize >> 8) & 0xFF);
            bos.write(frameSize & 0xFF);

            bos.write(0x00);
            bos.write(0x00);
            bos.write(fullFrameData);

            return 10 + frameSize;
        } catch (Exception e) {
            log.error("Error to write id3 tag: ", e);
            return 0;
        }
    }

    private byte[] intToSyncSafe(int value) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((value >> 21) & 0x7F);
        bytes[1] = (byte) ((value >> 14) & 0x7F);
        bytes[2] = (byte) ((value >> 7) & 0x7F);
        bytes[3] = (byte) (value & 0x7F);
        return bytes;
    }
}