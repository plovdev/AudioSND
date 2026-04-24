package org.plovdev.audioengine.effects;

import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteBuffer;

/**
 * Улучшенный эффект перегруза для гитары с шумоподавителем и оптимизациями
 *
 * @author Anton
 * @version 2.0
 */
public class OverdriveEffect implements AudioEffect {

    public enum Type {
        SOFT_CLIPPING,   // Мягкий клиппинг (блюз, рок)
        HARD_CLIPPING,   // Жёсткий клиппинг (металл)
        TUBE_SIMULATION, // Ламповое звучание (рок, блюз-рок)
        FUZZ,            // Агрессивный фузз (гранж, панк)
        WAVESHAPING      // Волновое формирование (экспериментальный)
    }

    // Константы
    private static final float MIN_DRIVE = 0.0f;
    private static final float MAX_DRIVE = 2.0f;
    private static final float MIN_TONE = 0.0f;
    private static final float MAX_TONE = 1.0f;
    private static final float MIN_LEVEL = 0.0f;
    private static final float MAX_LEVEL = 2.0f;
    private static final float MIN_MIX = 0.0f;
    private static final float MAX_MIX = 1.0f;
    private static final float MIN_NOISE_GATE = 0.0f;
    private static final float MAX_NOISE_GATE = 0.1f;

    // Основные параметры
    private TrackFormat format;
    private Type type;
    private float drive;
    private float tone;
    private float level;
    private float mix;
    private float noiseGateThreshold;

    // Внутренние компоненты
    private ToneFilter toneFilter;
    private float previousSample;
    private float envelope;

    // Кэшированные значения для производительности
    private int bytesPerSample;
    private int channels;
    private boolean needsReset = true;

    // ==================== КОНСТРУКТОРЫ ====================

    public OverdriveEffect() {
        this(Type.FUZZ, 0.6f, 0.1f, 0.6f, 0.6f, 0.01f);
    }

    public OverdriveEffect(Type type, float drive, float tone, float level, float mix) {
        this(type, drive, tone, level, mix, 0.02f);
    }

    public OverdriveEffect(Type type, float drive, float tone, float level, float mix, float noiseGateThreshold) {
        this.type = type;
        this.drive = clamp(drive, MIN_DRIVE, MAX_DRIVE);
        this.tone = clamp(tone, MIN_TONE, MAX_TONE);
        this.level = clamp(level, MIN_LEVEL, MAX_LEVEL);
        this.mix = clamp(mix, MIN_MIX, MAX_MIX);
        this.noiseGateThreshold = clamp(noiseGateThreshold, MIN_NOISE_GATE, MAX_NOISE_GATE);
        this.previousSample = 0.0f;
        this.envelope = 0.0f;
    }

    // ==================== ПРЕСЕТЫ ДЛЯ ГИТАРЫ ====================

    /**
     * Блюзовый перегруз (мягкий, тёплый)
     */
    public static OverdriveEffect bluesOverdrive() {
        return new OverdriveEffect(Type.SOFT_CLIPPING, 0.4f, 0.7f, 0.7f, 0.7f, 0.01f);
    }

    /**
     * Классический рок (ламповый звук)
     */
    public static OverdriveEffect classicRock() {
        return new OverdriveEffect(Type.TUBE_SIMULATION, 0.65f, 0.6f, 0.8f, 0.8f, 0.015f);
    }

    /**
     * Гранж/Фузз (агрессивный, грязный)
     */
    public static OverdriveEffect grungeFuzz() {
        return new OverdriveEffect(Type.FUZZ, 0.95f, 0.3f, 0.9f, 0.95f, 0.025f);
    }

    /**
     * Металл (жёсткий, плотный)
     */
    public static OverdriveEffect metalZone() {
        return new OverdriveEffect(Type.HARD_CLIPPING, 0.85f, 0.5f, 0.9f, 1.0f, 0.02f);
    }

    /**
     * Лёгкий овердрайв (для акустики)
     */
    public static OverdriveEffect lightOverdrive() {
        return new OverdriveEffect(Type.TUBE_SIMULATION, 0.3f, 0.8f, 0.6f, 0.5f, 0.005f);
    }

    // ==================== ИНИЦИАЛИЗАЦИЯ ====================

    @Override
    public void setup(TrackFormat format) {
        this.format = format;
        this.bytesPerSample = format.bytesPerSample() / format.channels();
        this.channels = format.channels();
        int sampleRate = format.sampleRate();
        float maxSampleValue = calculateMaxSampleValue(format);
        this.needsReset = false;

        // Инициализируем тональный фильтр
        if (toneFilter == null) {
            this.toneFilter = new ToneFilter(sampleRate, tone);
        } else {
            toneFilter.setSampleRate(sampleRate);
            toneFilter.setCutoff(tone);
        }

        // Сбрасываем состояние
        reset();
    }

    /**
     * Сброс состояния эффекта (для новой обработки)
     */
    public void reset() {
        previousSample = 0.0f;
        envelope = 0.0f;
        if (toneFilter != null) {
            toneFilter.reset();
        }
    }

    // ==================== ОСНОВНАЯ ОБРАБОТКА ====================

    @Override
    public void process(ByteBuffer buffer) {
        if (format == null || needsReset) {
            throw new IllegalStateException("Effect not initialized. Call setup() first.");
        }

        buffer.order(format.byteOrder());
        buffer.rewind();

        int totalSamples = buffer.remaining() / format.bytesPerSample();

        for (int i = 0; i < totalSamples; i++) {
            for (int ch = 0; ch < channels; ch++) {
                int pos = buffer.position();
                float sample = readSample(buffer, bytesPerSample);
                float processed = processSample(sample);
                buffer.position(pos);
                writeSample(buffer, processed, bytesPerSample);
            }
        }
    }

    // ==================== ОБРАБОТКА СЭМПЛА ====================

    private float processSample(float sample) {
        // 1. Шумоподавитель
        float gated = applyNoiseGate(sample);

        // 2. Дисторшн
        float distorted = applyDistortion(gated);

        // 3. Тональный фильтр
        distorted = toneFilter.process(distorted);

        // 4. Смешивание сухого и обработанного сигнала
        float mixed = sample * (1 - mix) + distorted * mix;

        // 5. Выходной уровень
        mixed *= level;

        return denormalizeSample(mixed);
    }

    private float applyDistortion(float sample) {
        float amplified = sample * (1 + drive * 8);

        return switch (type) {
            case SOFT_CLIPPING -> softClipping(amplified);
            case HARD_CLIPPING -> hardClipping(amplified);
            case TUBE_SIMULATION -> tubeSimulation(amplified);
            case FUZZ -> fuzzDistortion(amplified);
            case WAVESHAPING -> waveShaping(amplified);
        };
    }

    // ==================== АЛГОРИТМЫ ДИСТОРШНА ====================

    private float softClipping(float input) {
        final float threshold = 0.8f;
        final float knee = 0.15f;
        final float tpk = threshold + knee;
        final float tmk = threshold - knee;

        if (input > tpk) {
            float diff = input - tpk;
            return threshold + diff / (1 + diff * diff);
        } else if (input > tmk) {
            float diff = input - threshold;
            float ratio = diff * diff / (3 * knee * knee);
            return threshold + diff * (1 - ratio);
        } else if (input < -tpk) {
            float diff = -input - tpk;
            return -threshold - diff / (1 + diff * diff);
        } else if (input < -tmk) {
            float diff = -input - threshold;
            float ratio = diff * diff / (3 * knee * knee);
            return -threshold - diff * (1 - ratio);
        }
        return input;
    }

    private float hardClipping(float input) {
        final float threshold = 0.7f;
        return clamp(input, -threshold, threshold);
    }

    private float tubeSimulation(float input) {
        final float gain = 2.5f;
        final float tanhGain = (float) Math.tanh(gain);

        // Асимметричное искажение (характерно для ламп)
        float shaped = (float) Math.tanh(input * gain) / tanhGain;

        // Лёгкая компрессия для тёплого звука
        if (shaped > 0.5f) {
            shaped = 0.5f + (shaped - 0.5f) * 0.7f;
        } else if (shaped < -0.5f) {
            shaped = -0.5f + (shaped + 0.5f) * 0.7f;
        }

        return shaped;
    }

    private float fuzzDistortion(float input) {
        // Гистерезис для предотвращения дребезга
        final float hysteresis = 0.03f;

        float distorted;

        // Бинарный клиппинг с зоной перехода
        if (input > hysteresis) {
            distorted = 1.0f;
        } else if (input < -hysteresis) {
            distorted = -1.0f;
        } else {
            // Плавный переход в зоне тишины
            float t = input / hysteresis;
            distorted = t * 0.5f;
        }

        // Добавляем октаву вверх (эффект "жужжания")
        float octaveUp = Math.abs(input) * 0.4f;
        distorted = distorted * 0.8f + octaveUp * 0.2f;

        // Сглаживание с переменным коэффициентом
        float smoothFactor = 0.85f - drive * 0.1f;
        distorted = smoothFactor * previousSample + (1 - smoothFactor) * distorted;
        previousSample = distorted;

        // Лёгкое ограничение
        return clamp(distorted, -0.98f, 0.98f);
    }

    private float waveShaping(float input) {
        float x = clamp(input, -1.0f, 1.0f);
        float x2 = x * x;
        float x3 = x2 * x;
        float x5 = x3 * x2;

        // Полином 5-й степени для более интересного звука
        float shaped = x - x3 * 0.5f + x5 * 0.2f;

        return clamp(shaped, -1.0f, 1.0f);
    }

    // ==================== ШУМОПОДАВИТЕЛЬ ====================

    private float applyNoiseGate(float sample) {
        float absSample = Math.abs(sample);
        float release = 0.0005f; // 0.5 мс релиз

        // Огибающая с быстрым атаком и медленным релизом
        if (absSample > envelope) {
            envelope = absSample; // Мгновенный атак
        } else {
            envelope = envelope * (1 - release) + absSample * release;
        }

        // Плавное отсечение вместо резкого
        if (envelope < noiseGateThreshold) {
            float factor = envelope / noiseGateThreshold;
            return sample * factor * factor; // Квадратичное затухание
        }

        return sample;
    }

    // ==================== ЧТЕНИЕ/ЗАПИСЬ СЭМПЛОВ ====================

    private float readSample(ByteBuffer buffer, int bytesPerSample) {
        return switch (bytesPerSample) {
            case 1 -> format.signed() ?
                    buffer.get() / 127.0f :
                    (buffer.get() - 128) / 128.0f;
            case 2 -> buffer.getShort() / 32767.0f;
            case 3 -> {
                byte b1 = buffer.get();
                byte b2 = buffer.get();
                byte b3 = buffer.get();
                int sample24;
                if (format.byteOrder() == java.nio.ByteOrder.LITTLE_ENDIAN) {
                    sample24 = ((b3 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b1 & 0xFF);
                } else {
                    sample24 = ((b1 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b3 & 0xFF);
                }
                yield format.signed() ?
                        sample24 / 8388607.0f :
                        (sample24 - 8388608) / 8388608.0f;
            }
            case 4 -> format.audioCodec() == TrackFormat.AudioCodec.FLOAT32 ?
                    buffer.getFloat() :
                    buffer.getInt() / 2147483647.0f;
            case 8 -> (float) buffer.getDouble();
            default -> throw new IllegalArgumentException("Unsupported sample size: " + bytesPerSample);
        };
    }

    private void writeSample(ByteBuffer buffer, float sample, int bytesPerSample) {
        sample = clamp(sample, -1.0f, 1.0f);

        switch (bytesPerSample) {
            case 1:
                buffer.put((byte) (format.signed() ?
                        sample * 127.0f :
                        sample * 128.0f + 128.0f));
                break;
            case 2:
                buffer.putShort((short) (sample * 32767.0f));
                break;
            case 3:
                int sample24 = (int) (sample * (format.signed() ? 8388607.0f : 8388608.0f));
                if (!format.signed()) sample24 += 8388608;
                if (format.byteOrder() == java.nio.ByteOrder.LITTLE_ENDIAN) {
                    buffer.put((byte) (sample24 & 0xFF));
                    buffer.put((byte) ((sample24 >> 8) & 0xFF));
                    buffer.put((byte) ((sample24 >> 16) & 0xFF));
                } else {
                    buffer.put((byte) ((sample24 >> 16) & 0xFF));
                    buffer.put((byte) ((sample24 >> 8) & 0xFF));
                    buffer.put((byte) (sample24 & 0xFF));
                }
                break;
            case 4:
                if (format.audioCodec() == TrackFormat.AudioCodec.FLOAT32) {
                    buffer.putFloat(sample);
                } else {
                    buffer.putInt((int) (sample * 2147483647.0f));
                }
                break;
            case 8:
                buffer.putDouble(sample);
                break;
        }
    }

    private float denormalizeSample(float sample) {
        // Предотвращаем денормальные числа (очень маленькие значения)
        if (Math.abs(sample) < 1e-25f) {
            return 0.0f;
        }
        return sample;
    }

    private float calculateMaxSampleValue(TrackFormat format) {
        if (format.audioCodec() == TrackFormat.AudioCodec.FLOAT32 ||
                format.audioCodec() == TrackFormat.AudioCodec.FLOAT64) {
            return 1.0f;
        }

        int bits = format.bitDepth();
        if (format.signed()) {
            return (float) Math.pow(2, bits - 1) - 1;
        } else {
            return (float) Math.pow(2, bits) - 1;
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    // ==================== GETTERS / SETTERS ====================

    public void setDrive(float drive) {
        this.drive = clamp(drive, MIN_DRIVE, MAX_DRIVE);
    }

    public void setTone(float tone) {
        this.tone = clamp(tone, MIN_TONE, MAX_TONE);
        if (toneFilter != null) {
            toneFilter.setCutoff(tone);
        }
    }

    public void setLevel(float level) {
        this.level = clamp(level, MIN_LEVEL, MAX_LEVEL);
    }

    public void setMix(float mix) {
        this.mix = clamp(mix, MIN_MIX, MAX_MIX);
    }

    public void setNoiseGateThreshold(float threshold) {
        this.noiseGateThreshold = clamp(threshold, MIN_NOISE_GATE, MAX_NOISE_GATE);
    }

    public void setType(Type type) {
        this.type = type;
        reset();
    }

    // ==================== ВНУТРЕННИЙ КЛАСС ТОНАЛЬНОГО ФИЛЬТРА ====================

    private static class ToneFilter {
        private float sampleRate;
        private float cutoff;
        private float alpha;
        private float lowPassOutput;
        private float highPassOutput;
        private boolean needsUpdate = true;

        public ToneFilter(float sampleRate, float cutoff) {
            this.sampleRate = sampleRate;
            setCutoff(cutoff);
            reset();
        }

        public void setSampleRate(float sampleRate) {
            if (this.sampleRate != sampleRate) {
                this.sampleRate = sampleRate;
                this.needsUpdate = true;
            }
        }

        public void setCutoff(float cutoff) {
            // Диапазон частот: 100 Гц - 8000 Гц
            float newCutoff = 100.0f + cutoff * 7900.0f;
            if (Math.abs(this.cutoff - newCutoff) > 1.0f) {
                this.cutoff = newCutoff;
                this.needsUpdate = true;
            }
        }

        public float process(float input) {
            if (needsUpdate) {
                updateCoefficients();
            }

            lowPassOutput = alpha * input + (1 - alpha) * lowPassOutput;
            highPassOutput = input - lowPassOutput;

            // Баланс между низкими и высокими частотами
            float mixFactor = (cutoff - 100.0f) / 7900.0f;
            return lowPassOutput * (1 - mixFactor) + highPassOutput * mixFactor;
        }

        private void updateCoefficients() {
            float rc = 1.0f / (2.0f * (float) Math.PI * cutoff);
            float dt = 1.0f / sampleRate;
            alpha = dt / (rc + dt);
            needsUpdate = false;
        }

        public void reset() {
            lowPassOutput = 0.0f;
            highPassOutput = 0.0f;
        }
    }
}