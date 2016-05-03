package com.example.audiorecordtest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.swing.text.AbstractDocument.BranchElement;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.Environment;
import android.util.Log;

public class MyUtil {
	public static String dicpathString = Environment.getExternalStorageDirectory()+"/msc";
	public static int dicIndex=0;
	public static String[] dicString={};
	public static void clearState(){
		dicIndex=0;
		dicString=new String[]{};
	}
	
	
	/**
	 * 对文件中预先放好的评测词文件表 进行读取
	 * @param path
	 * @return
	 */
	public static String[] fetchDicString(String path) {
		ArrayList<String> dicArrayList = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(path),"UTF-8"));
			String lineString = br.readLine();
			while(lineString!=null){
				dicArrayList.add(lineString);
				lineString = br.readLine();
			}
			br.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return dicArrayList.toArray(new String[]{});
	}
	
	
	/**
	 * 写一个默认的评测词文件表 
	 * @param path
	 * @return
	 */
	public static void createDicString(String path,String[] dics) {
		try {
			BufferedWriter br = new BufferedWriter(new OutputStreamWriter(new 
					FileOutputStream(path),"UTF-8"));
			for (String string : dics) {
				br.write(string+"\n");
			}
			br.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
	

	/// 这个部分不太好用
//	public static void chstorepam(String label,SpeechRecognizer asrORiat){
//		String filename = MyUtil.fetchfilename(label,false);
//		asrORiat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
//		asrORiat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory().getAbsolutePath() + "/msc/"+filename);
//		
//		asrORiat.setParameter(SpeechConstant.ISE_AUDIO_PATH, Environment.getExternalStorageDirectory().getAbsolutePath() + "/msc/"+filename);
//		
//	}
	

	/**
	 * 这个用来解析 命令词识别的返回结果 返回的是 命令词识别的一个完整的字符串
	 * @param grammerString
	 * @return
	 */
	public static String parseGrammerJson(String grammerString){
		StringBuffer rs = new StringBuffer();
		try{
			JSONObject object = new JSONObject(grammerString);
			JSONArray words = object.getJSONArray("ws");
			for(int i =0;i<words.length();i++){
				JSONArray items = words.getJSONObject(i).getJSONArray("cw");
				for(int j = 0; j < items.length(); j++)
				{
					JSONObject obj = items.getJSONObject(j);
					if(obj.getString("w").contains("nomatch"))
					{
						return "E";
					}
					rs.append(obj.getString("w")+"_"+ obj.getInt("sc")+"+");
				}
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		if(rs.lastIndexOf("+")>-1){
			return rs.substring(0, rs.length()-1);
		}else {
			return rs.toString();
		}
	}
	/**
	 * 用来读取sdcard上的音频文件
	 * @param filepath
	 * @return
	 */
public static byte[] readFileFromSDcard(String filepath) {
	        byte[] buffer = null;
	        FileInputStream in = null;
	        try {
	            in = new FileInputStream(filepath);
	            buffer = new byte[in.available()];
	            in.read(buffer);
	        } catch (Exception e) {
	            e.printStackTrace();
	        } finally {
	            try {
	                if (in != null) {
	                    in.close();
	                    in = null;
	                }
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	        if(buffer==null)
	        	buffer=new byte[3];
	        return buffer;
	    }
	
	
/**
 * 往自定义的文件中追加记录 需要自己加\n
 * @param record
 */
public static void writeRecord(String record,String filepath){
	try{
			BufferedWriter bw=null;
			String recordfilepath = Environment.getExternalStorageDirectory().getAbsolutePath() +
					"/msc/"+filepath;
		bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(recordfilepath,true)));
		bw.write(record);
		bw.close();
	}catch(Exception e){	
		Log.e("mytest", e.getMessage());
	}
}

	/***
	 * 获取当前记录文件记录了多少行数据
	 * @return
	 */
	public static int getWavInd(){
		String recFilePath = dicpathString+"/recordfile_iat.txt";
		File theFile = new File(recFilePath);
		if(!theFile.exists())
			return 0;
		BufferedReader br =null;
		int ind=0;
		try{
			br = new BufferedReader(new InputStreamReader(new FileInputStream(dicpathString+"/recordfile_iat.txt"), "UTF-8"));
			String line = br.readLine();
			while(line!=null){
				ind++;
				line =br.readLine();
			}
			br.close();
			//bw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return ind;
	}
	
	
	/**
	 * 用来翻转 自定义的 文本词典的指针
	 * @return
	 */
	public static  int preDicIndex(){
		dicIndex--;
		if(dicIndex<0)
			dicIndex=0;
		return dicIndex;
	}
	public static int nextDicIndex(){
		dicIndex++;
		dicIndex=dicIndex%dicString.length;
		return dicIndex;
	}

	
	
	/**
	 * 找到当前文件夹下对应的音频文件 
	 * @param label
	 * @param isfetch  isfetch = true 表示 音频文件已经存了，现在想找到这个文件
	 * isfetch = false 表示 准备存文件，不想文件名与之前的文件重复
	 * @return  返回下一个使用的音频文件的名字
	 */
	public static String fetchfilename(String label,boolean isfetch){
		String dirpath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/msc/";
		File dir = new File(dirpath);
		String files[] = dir.list();
		int num =0;
		for (int i = 0; i < files.length; i++) {
			if(files[i].indexOf(label)>-1) num++;
		}
		if(!isfetch)
			num++;
		return label+"_"+num+".wav";
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
