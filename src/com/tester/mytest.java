package com.tester;

import java.io.IOException;

import com.denoiser.Denoiser;
import com.denoiser.WavFileException;

public class mytest {
	public static void main(String[] args) throws IOException, WavFileException {
		String filename = "/Users/karl/Work/database/age/childrennoise/0.wav";
		Denoiser denoiser = new Denoiser(filename, 16000,0.5,18,3,3);
		denoiser.process();
		double SNR = denoiser.getSNR();
		double noise = denoiser.getNoise();
		System.out.println(SNR + " " + noise);
	}
}
