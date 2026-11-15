package com.pvp.optimize.music;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.Obuffer;
import javazoom.jl.decoder.SampleBuffer;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * 统一的 PCM 字节流抽象。三种解码器只负责把文件变成 16-bit PCM，
 * 音量、播放状态、音频线都由 {@link MusicPlayer} 管，解码线程与渲染主线程完全隔离。
 */
public interface AudioStream extends AutoCloseable {

    AudioFormat format();

    /** 把 buf 填成可直接写进 SourceDataLine 的 PCM 数据，返回字节数；EOF 返回 -1。 */
    int read(byte[] buf) throws IOException;

    /** 尽力跳转到指定秒数，不要求精确。 */
    void seek(double seconds) throws IOException;

    double durationSeconds();

    double positionSeconds();

    @Override
    void close();

    static AudioStream open(Path path) throws IOException, UnsupportedAudioFileException {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".mp3")) return new Mp3(path);
        if (name.endsWith(".ogg")) return new Ogg(path);
        if (name.endsWith(".wav")) return new Wav(path);
        throw new UnsupportedAudioFileException("Unsupported format: " + name);
    }

    /** 无法解码/打开时抛出，播放器会跳过该曲目而不是崩游戏。 */
    final class UnsupportedAudioFileException extends Exception {
        UnsupportedAudioFileException(String m) { super(m); }
    }

    static byte[] shortsToBytes(short[] pcm, int count) {
        byte[] out = new byte[count * 2];
        for (int i = 0; i < count; i++) {
            short s = pcm[i];
            out[i * 2] = (byte) (s & 0xFF);
            out[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return out;
    }

    // ================================================================ WAV
    /** JDK 自带解码，零额外依赖。保留文件原始格式，切歌时按新格式重开音频线。 */
    final class Wav implements AudioStream {
        private final Path path;
        private AudioInputStream in;
        private final AudioFormat fmt;
        private final double duration;
        private long bytesRead;

        Wav(Path path) throws IOException {
            this.path = path;
            try {
                this.in = AudioSystem.getAudioInputStream(path.toFile());
            } catch (Exception e) {
                throw new IOException("Cannot open WAV: " + path, e);
            }
            this.fmt = in.getFormat();
            long frames = in.getFrameLength();
            this.duration = frames > 0 && fmt.getFrameRate() > 0
                    ? frames / fmt.getFrameRate() : 0.0;
        }

        @Override public AudioFormat format() { return fmt; }

        @Override public int read(byte[] buf) throws IOException {
            int frame = Math.max(1, fmt.getFrameSize());
            int len = (buf.length / frame) * frame;
            if (len == 0) return -1;
            int n = in.read(buf, 0, len);
            if (n > 0) bytesRead += n;
            return n;
        }

        @Override public void seek(double seconds) throws IOException {
            double bytes = seconds * fmt.getFrameSize() * fmt.getFrameRate();
            long target = (long) bytes;
            target -= target % Math.max(1, fmt.getFrameSize());
            close();
            try {
                in = AudioSystem.getAudioInputStream(path.toFile());
            } catch (Exception e) {
                throw new IOException("Cannot reopen WAV: " + path, e);
            }
            long left = target;
            while (left > 0) {
                long skipped = in.skip(left);
                if (skipped <= 0) break;
                left -= skipped;
            }
            bytesRead = target;
        }

        @Override public double durationSeconds() { return duration; }

        @Override public double positionSeconds() {
            double denom = fmt.getFrameSize() * fmt.getFrameRate();
            return denom > 0 ? bytesRead / denom : 0.0;
        }

        @Override public void close() {
            if (in != null) { try { in.close(); } catch (IOException ignored) {} in = null; }
        }
    }

    // ================================================================ MP3
    /** JLayer (LGPL) 逐帧解码；seek 用跳帧近似，避免整首重解码。 */
    final class Mp3 implements AudioStream {
        private final Path path;
        private InputStream fileIn;
        private Bitstream bitstream;
        private Decoder decoder;
        private AudioFormat fmt;
        private double duration;
        private long pcmBytesWritten;
        private Header firstHeader;
        private byte[] pending = new byte[0];
        private int pendingPos;

        Mp3(Path path) throws IOException {
            this.path = path;
            reopen();
        }

        private void reopen() throws IOException {
            closeHandles();
            fileIn = new BufferedInputStream(Files.newInputStream(path), 64 * 1024);
            bitstream = new Bitstream(fileIn);
            decoder = new Decoder();
            try {
                Header h = bitstream.readFrame();
                if (h == null) throw new IOException("Empty MP3: " + path);
                firstHeader = h;
                Obuffer ob = decoder.decodeFrame(h, bitstream);
                SampleBuffer sb = (SampleBuffer) ob;
                int channels = Math.max(1, sb.getChannelCount());
                float rate = h.frequency();
                fmt = new AudioFormat(rate, 16, channels, true, false);
                pending = shortsToBytes(sb.getBuffer(), sb.getBufferLength());
                pendingPos = 0;
                bitstream.closeFrame();
                long frames = h.max_number_of_frames((int) Files.size(path));
                duration = frames > 0 ? frames * h.ms_per_frame() / 1000.0 : 0.0;
                if (duration <= 0 || duration > 12 * 3600) duration = 0.0;
            } catch (Exception e) {
                closeHandles();
                throw new IOException("Cannot decode MP3: " + path, e);
            }
            pcmBytesWritten = 0;
        }

        @Override public AudioFormat format() { return fmt; }

        @Override public int read(byte[] buf) throws IOException {
            int written = 0;
            while (written < buf.length) {
                if (pendingPos < pending.length) {
                    int n = Math.min(buf.length - written, pending.length - pendingPos);
                    System.arraycopy(pending, pendingPos, buf, written, n);
                    pendingPos += n;
                    written += n;
                    continue;
                }
                Header h;
                try {
                    h = bitstream.readFrame();
                } catch (Exception e) {
                    return written > 0 ? written : -1;
                }
                if (h == null) return written > 0 ? written : -1;
                try {
                    SampleBuffer sb = (SampleBuffer) decoder.decodeFrame(h, bitstream);
                    pending = shortsToBytes(sb.getBuffer(), sb.getBufferLength());
                    pendingPos = 0;
                } catch (Exception e) {
                    return written > 0 ? written : -1;
                } finally {
                    bitstream.closeFrame();
                }
                if (pending.length == 0) return written > 0 ? written : -1;
            }
            pcmBytesWritten += written;
            return written;
        }

        @Override public void seek(double seconds) throws IOException {
            if (seconds <= 0 || firstHeader == null) { reopen(); return; }
            double msPerFrame = firstHeader.ms_per_frame();
            int framesToSkip = msPerFrame > 0 ? (int) (seconds * 1000.0 / msPerFrame) : 0;
            framesToSkip = Math.max(0, Math.min(framesToSkip, 200_000));
            reopen();
            for (int i = 0; i < framesToSkip; i++) {
                try {
                    Header h = bitstream.readFrame();
                    if (h == null) break;
                    bitstream.closeFrame();
                } catch (Exception ignored) { break; }
            }
            // positionSeconds 基于已写出的字节数，这里按目标时间反推，保证进度条连续。
            pcmBytesWritten = (long) (seconds * fmt.getFrameSize() * fmt.getFrameRate());
        }

        @Override public double durationSeconds() { return duration; }

        @Override public double positionSeconds() {
            if (fmt == null) return 0;
            double denom = fmt.getFrameSize() * fmt.getFrameRate();
            return denom > 0 ? pcmBytesWritten / denom : 0.0;
        }

        private void closeHandles() {
            if (bitstream != null) { try { bitstream.close(); } catch (Exception ignored) {} bitstream = null; }
            if (fileIn != null) { try { fileIn.close(); } catch (IOException ignored) {} fileIn = null; }
        }

        @Override public void close() { closeHandles(); }
    }

    // ================================================================ OGG
    /** LWJGL 自带的 stb_vorbis，MC 运行时已有 native 库，不引入新依赖。 */
    final class Ogg implements AudioStream {
        private final Path path;
        private long handle;
        private AudioFormat fmt;
        private final double duration;
        private long shortsWritten;
        private ShortBuffer frameBuffer;
        private byte[] pending = new byte[0];
        private int pendingPos;

        Ogg(Path path) throws IOException {
            this.path = path;
            handle = STBVorbis.stb_vorbis_open_filename(path.toString(), (IntBuffer) null, null);
            if (handle == 0L) throw new IOException("Cannot open OGG: " + path);
            STBVorbisInfo info = STBVorbisInfo.malloc();
            try {
                STBVorbis.stb_vorbis_get_info(handle, info);
                int rate = info.sample_rate();
                int channels = Math.max(1, info.channels());
                fmt = new AudioFormat(rate, 16, channels, true, false);
                int frameSize = Math.max(4096, info.max_frame_size());
                frameBuffer = ByteBuffer.allocateDirect(frameSize * channels * 2 * 2)
                        .order(ByteOrder.nativeOrder()).asShortBuffer();
                int samples = STBVorbis.stb_vorbis_stream_length_in_samples(handle);
                duration = samples > 0 && rate > 0 ? (double) samples / rate : 0.0;
            } finally {
                info.free();
            }
        }

        @Override public AudioFormat format() { return fmt; }

        @Override public int read(byte[] buf) throws IOException {
            int written = 0;
            while (written < buf.length) {
                if (pendingPos < pending.length) {
                    int n = Math.min(buf.length - written, pending.length - pendingPos);
                    System.arraycopy(pending, pendingPos, buf, written, n);
                    pendingPos += n;
                    written += n;
                    continue;
                }
                frameBuffer.clear();
                int n = STBVorbis.stb_vorbis_get_frame_short_interleaved(handle, fmt.getChannels(), frameBuffer);
                if (n <= 0) return written > 0 ? written : -1;
                // LWJGL 只返回写入数量、不会改 buffer 的 position/limit，必须手动圈定范围。
                short[] pcm = new short[n];
                frameBuffer.position(0);
                frameBuffer.limit(n);
                frameBuffer.get(pcm);
                frameBuffer.clear();
                pending = shortsToBytes(pcm, n);
                pendingPos = 0;
            }
            shortsWritten += written / 2;
            return written;
        }

        @Override public void seek(double seconds) {
            if (seconds <= 0) { STBVorbis.stb_vorbis_seek_start(handle); }
            else { STBVorbis.stb_vorbis_seek(handle, (int) (seconds * fmt.getSampleRate())); }
            shortsWritten = (long) (seconds * fmt.getSampleRate() * fmt.getChannels());
            pending = new byte[0];
            pendingPos = 0;
        }

        @Override public double durationSeconds() { return duration; }

        @Override public double positionSeconds() {
            double denom = fmt.getSampleRate() * fmt.getChannels();
            return denom > 0 ? (double) shortsWritten / denom : 0.0;
        }

        @Override public void close() {
            if (handle != 0L) { STBVorbis.stb_vorbis_close(handle); handle = 0L; }
        }
    }
}
