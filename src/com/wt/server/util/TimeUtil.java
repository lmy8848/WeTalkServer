package com.wt.server.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtil {
	/**
	 * ��ȡ����ʱ��(ϵͳ��ǰʱ��),ʱ���ʽΪ"yyMMddHHmmss"
	 * @return
	 */
	public static String getAbsoluteTime(){
		SimpleDateFormat sdf= new SimpleDateFormat("yyMMddHHmmss");
		 return sdf.format(new Date());
	}
	
	/**
	 * ��ȡ���ʱ��(�������ʱ��任�������ϵͳ��ǰʱ��Ĳ�ֵ)����ʽΪ��XX����ǰ��
	 * @return
	 */
	public static String getRelativeTime(String date){
		String time="";
		try {
			SimpleDateFormat sdf= new SimpleDateFormat("yyMMddHHmmss");
			Date dt1=sdf.parse(date);
			
			Calendar cl=Calendar.getInstance();
			int year2=cl.get(Calendar.YEAR);
			int month2=cl.get(Calendar.MONTH);
			int day2=cl.get(Calendar.DAY_OF_MONTH);
			int hour2=cl.get(Calendar.HOUR_OF_DAY);
			int minute2=cl.get(Calendar.MINUTE);
			int second2=cl.get(Calendar.SECOND);
			
			cl.setTime(dt1);
			int year1=cl.get(Calendar.YEAR);
			int month1=cl.get(Calendar.MONTH);
			int day1=cl.get(Calendar.DAY_OF_MONTH);
			int hour1=cl.get(Calendar.HOUR_OF_DAY);
			int minute1=cl.get(Calendar.MINUTE);
			int second1=cl.get(Calendar.SECOND);
			
			if(year1==year2){
				if(month1==month2){
					if(day1==day2){
						if(hour1==hour2){
							if(minute1==minute2){
								time="�ղ�";
							}else{
								time=(minute2-minute1)+"����ǰ";
							}
						}else if(hour2-hour1>3){
							time=formatTime(hour1, minute1);
						}else if(hour2-hour1==1){
							if(minute2-minute1>0){
								time="1Сʱǰ";
							}else{
								time=(60+minute2-minute1)+"����ǰ";
							}
						}else{
							time=(hour2-hour1)+"Сʱǰ";
						}
					}else if(day2-day1==1){  //����
						if(hour1>12){
							time=(month1+1)+"��"+day1+"��  ����";
						}else{
							time=(month1+1)+"��"+day1+"��  ����";
						}
					}else{
						time=(month1+1)+"��"+day1+"��";
					}
				}else{
					time=(month1+1)+"��"+day1+"��";
				}
			}else{
				time=year1+"��"+month1+"��"+day1;
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return time;
	}
	
	private static String formatTime(int hour, int minute){
		String time="";
		if(hour<10){
			time+="0"+hour+":";
		}else{
			time+=hour+":";
		}
		
		if(minute<10){
			time+="0"+minute;
		}else{
		    time+=minute;
		}
		System.out.println("format(hour, minute)="+time);
		return time;
	}
}
