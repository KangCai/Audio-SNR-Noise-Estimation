package com.denoiser;

import java.io.File;
import java.util.Arrays;

public class Denoiser implements AudioProcessor {

    private static int windowLength;
    private static double overlapRatio;
    private int fs;
    private double noSpeechDuration;
    private int noSpeechSegments;
    private boolean speechFlag;
    private boolean noiseFlag;
    private int noiseCounter;
    private int noiseLength;
    private int noiseThreshold;
    private int frameReset;
    private double g_diff;
    private double g_noise;
    private int g_diffcount;
    private int g_noisecount;
    private String filename;
    
    public Denoiser(String filename, int fs, double noSpeechDuration, int noiseLength, int noiseThreshold, int frameReset) {
        windowLength = 256;
        overlapRatio = 0.5;
        this.filename = filename;
        this.fs = fs;
        this.noSpeechDuration = noSpeechDuration;
        this.noSpeechSegments = (int)Math.floor((noSpeechDuration * fs - windowLength) / (overlapRatio * windowLength) + 1);
        this.speechFlag = false;
        this.noiseFlag = false;
        this.noiseLength = noiseLength;
        this.noiseThreshold = noiseThreshold;
        this.frameReset = frameReset;
    }


    /**
     * Performs speech denoising on array of doubles based on  Speech Enhancement Using a Minimum Mean-Square
     * Error Short-Time Spectral Amplitude Estimator by Eprahiam and Malah
     * @param  input Double array of signal values
     * @return   enhanced Double array of enhanced signal array
     */

    public double getSNR() {
    	if(g_diffcount == 0)
    		return 0;
    	return g_diff / g_diffcount;
    }
    
    public double getNoise() {
    	if(g_noisecount == 0)
    		return 0;
    	return g_noise / g_noisecount;
    }
    public void process() {
    	double[] buffer = null;
		try {
			WavFile wavFile = WavFile.openWavFile(new File(filename));
			int numFrames = (int)wavFile.getNumFrames();
			buffer = new double[numFrames];
			wavFile.readFrames(buffer, numFrames);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		process(buffer);
		
    }
    public void process(double[] input) {
    	g_diff = 0;
    	g_diffcount = 0;
    	g_noise = 0;
    	g_noisecount = 0;
    	
        double[][] sampledSignalWindowed = segmentSignal(input, windowLength, overlapRatio);
        int frames = sampledSignalWindowed[0].length;
        ComplexNumber[][] sampledSignalWindowedComplex = new ComplexNumber[frames][windowLength];
        ComplexNumber[][] signalFFT = new ComplexNumber[frames][windowLength];
        double[][] signalFFTMagnitude = new double[frames][windowLength];

        for (int i = 0; i < frames; i++) {
            for (int k = 0; k < windowLength; k++) {
                sampledSignalWindowedComplex[i][k] = new ComplexNumber(sampledSignalWindowed[k][i]); //convert samples to Complex form for fft and perform transpose
            }
        }

        for (int i = 0; i < frames; i++) {
            signalFFT[i] = Utils.fft(sampledSignalWindowedComplex[i]);
        }

        for (int i = 0; i < frames; i++) {
            for (int k = 0; k < windowLength; k++) {
                signalFFTMagnitude[i][k] = signalFFT[i][k].mod();
            }
        }

        double[][] noise = new double[this.noSpeechSegments][windowLength];
        double[][] noiseMag = new double[this.noSpeechSegments][windowLength];

        int frompos = frames - this.noSpeechSegments;
        int topos = frames;
        noise  = Arrays.copyOfRange(signalFFTMagnitude, frompos, topos);

        for (int i = 0; i < this.noSpeechSegments; i++) {
            for (int k = 0; k < windowLength; k++) {
                noiseMag[i][k] = Math.pow(noise[i][k], 2);
            }
        }

        double[] noiseMean = Utils.mean(noise, 0);
        double[] noiseVar = Utils.mean(noiseMag, 0);

        double[] gain = new double[windowLength];
        double[] gamma = new double[windowLength];

        Arrays.fill(gain, 1);
        Arrays.fill(gamma, 1);

        this.speechFlag = false;
        this.noiseCounter = 0;
        for (int i = 0; i < frames; i++) {
            vad(signalFFTMagnitude[i], noiseMean);

            if (this.speechFlag == false) { // Noise estimate update during segements with no speech
                for (int k = 0; k < windowLength; k++) {
                    noiseMean[k] = (this.noiseLength * noiseMean[k] + signalFFTMagnitude[i][k]) / (this.noiseLength + 1);
                    noiseVar[k] = (this.noiseLength * noiseVar[k] + Math.pow(signalFFTMagnitude[i][k], 2)) / (this.noiseLength + 1);
                }
            }
        }
        
    }

    /**
     * Voice activity detector that predicts whether the current frame contains speech or not
     * @param frame  Current frame
     * @param noise   Current noise estimate
     * @param noiseCounter  Number of previous noise frames
     * @param noiseThreshold User set threshold
     * @param frameReset Number of frames after which speech flag is reset
     */
    private void vad(double[] frame, double[] noise) {
        double[] spectralDifference = new double[windowLength];

        for (int i = 0; i < windowLength; i++) {
            spectralDifference[i] = 20 * (Math.log10(frame[i]) - Math.log10(noise[i]));
            if (spectralDifference[i] < 0) {
                spectralDifference[i] = 0;
            }
        }

        double diff = Utils.mean(spectralDifference);
        double noiseMean = Utils.mean(noise);
        if (diff < this.noiseThreshold) {
            this.noiseFlag = true;
            this.noiseCounter++;
            g_noise += noiseMean;
            g_noisecount ++;
        } else {
            this.noiseFlag = false;
            this.noiseCounter = 0;
            g_diff += diff;
            g_diffcount ++;     
        }

        if (this.noiseCounter > this.frameReset) {
            this.speechFlag = false;
        } else {
            this.speechFlag = true;
        }
    }

    /**
     * Windows sampled signal using overlapping Hamming windows
     * @param ss The sampled signal
     * @param ww The window width
     * @param or The overlap ratio
     * @return seg The overlapping windowed segments
     */

    private double[][] segmentSignal(double[] ss, int ww, double or ) {
        int len = ss.length;
        double d = 1 - or;
        int frames = (int)(Math.floor(len - ww) / ww / d);
        int start = 0;
        int stop = 0;

        double[] window = Utils.hamming(ww);
        double[][] seg = new double[ww][frames];

        for (int i = 0; i < frames; i++) {
            start = (int)(i * ww * or );
            stop =  start + ww;
            for (int k = 0; k < ww; k++) {
                seg[k][i] = ss[start + k] * window[k];
            }
        }
        return seg;
    }
}