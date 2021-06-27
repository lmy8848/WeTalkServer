package com.wt.server.util;

import java.io.File;
import java.io.IOException;

public class FileUtil {
	
	public static final String WORK_IMG_PATH="D:\\wetalk\\Img";
	public static final String WORK_AUDIO_PATH="D:\\wetalk\\Audio";
	public static final String WORK_HEAD_PATH="D:\\wetalk\\Head";

	/**
	 * ����fileType,������ͨ��jpg��3gp�ļ�������ͼƬ������
	 * @param selfId
	 * @param fileType
	 * @return
	 */
	public static File createFile(String selfId, int fileType) {
		String nowTime = TimeUtil.getAbsoluteTime();
		String filePath="";
		if(fileType==Config.MESSAGE_TYPE_IMG){
			filePath =WORK_IMG_PATH + selfId;
		}else{
			filePath =WORK_AUDIO_PATH + selfId;
		}
		
		File fileParent = new File(filePath);
		if (fileParent.exists() == false) {
			fileParent.mkdirs();
		}
		File file = null;
		if (fileType == Config.MESSAGE_TYPE_IMG) {
			file = new File(filePath + "\\" + nowTime + ".jpg");
		} else if (fileType == Config.MESSAGE_TYPE_AUDIO) {
			file = new File(filePath + "\\" + nowTime + ".3gp");
		}

		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return file;
	}
	
	//�����û�ID,����һ���Ը�IDΪ�ļ�����jpgͼƬ
	public static File createHeadFile(int userId){
        File fileParent = new File(WORK_HEAD_PATH);
        if (fileParent.exists() == false) {
            fileParent.mkdirs();
        }
        File file = null;
        file = new File(WORK_HEAD_PATH + "\\" + userId+ ".jpg");

        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
	}
}
