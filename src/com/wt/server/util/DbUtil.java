package com.wt.server.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import com.wt.server.common.Friend;
import com.wt.server.common.Message;
import com.wt.server.common.User;

/**
 * ������ת�����������ݿ�Ĳ���
 * 
 * @author www
 *
 */
public class DbUtil {
	Connection con = null;
	Statement sql = null;
	ResultSet rs = null;
	final String driver = "com.mysql.cj.jdbc.Driver";
	final String url = "jdbc:mysql://localhost:3306/wetalk?useUnicode=true&characterEncoding=utf-8";

	/**
	 * �ڹ��췽���н������ݿ�����
	 */
	public DbUtil() {
		try {
			Class.forName(driver);
			con = DriverManager.getConnection(url, "root", "root");
			sql = con.createStatement();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ��֤�û��ĺϷ���
	 * 
	 * @param userId
	 * @param pwd
	 * @return
	 */
	public User login(String userId, String pwd) {
		User user = null;
		String sqlStr = "select * from user where userId=" + userId
				+ " and pwd='" + pwd + "'";
		try {
			System.out.println(sqlStr);
			rs = sql.executeQuery(sqlStr);
//			while (rs.next()){
////				String ss=rs.getString("nickName");
////				String email = rs.getString("email");
////				System.out.println(ss);
////				System.out.println(email);
//				System.out.println("��Ϊ�գ�������������������");
//			}
			if (rs.next()) {
//				System.out.println(rs.getString("userId"));
//				rs.next();
				user = new User();
				user.setUserId(rs.getString("userId"));
				user.setNickName(rs.getString("nickName"));
				user.setSex(rs.getString("sex"));
				user.setEmail(rs.getString("email"));
				user.setHead(rs.getString("head"));
				user.setLastModityTime(rs.getLong("modifyTime"));

				System.out.println(userId);
				System.out.println(rs.getString("nickName"));
				System.out.println(rs.getString("sex"));
				System.out.println(rs.getString("email"));
				System.out.println(rs.getString("head"));
				System.out.println(rs.getLong("modifyTime"));
			}
			else {
				user = null;
				System.out.println("����Ϊ�գ�");
			}

		} catch (SQLException e) {
			e.printStackTrace();
			user = null;
		}

		return user;
	}

	// ��ȡID�������û�ID
	public int getMaxUserId() {
		int maxId = 0;
		String sqlStr = "select max(userId) from user";
		try {
			rs = sql.executeQuery(sqlStr);
			if (rs != null) {
				rs.next();
				maxId = rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return maxId;
	}

	/**
	 * 
	 * @param pwd
	 * @param nickName
	 * @param sex
	 * @param email
	 * @return boolean
	 */
	public boolean regist(String nickName, String pwd, String email,
			String sex, String head, long modifyTime) {
		String sqlStr = "insert into user( nickName,pwd,email, sex, head, modifyTime) values('"
				+ nickName
				+ "', '"
				+ pwd
				+ "', '"
				+ email
				+ "', '"
				+ sex
				+ "', '" + head + "', '" + modifyTime + "')";

		try {
			int i = sql.executeUpdate(sqlStr);
			if (i == 1) {
				System.out.println("regist() �ɹ�");
				return true;
			} else {
				System.out.println("regist() ʧ��");
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public String getHeadPath(String userId) {
		String sqlStr = "select head from user where userId=" + userId;
		String headPath = "";
		try {
			rs = sql.executeQuery(sqlStr);
			if (rs != null) {
				rs.first();
				headPath = rs.getString("head");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return headPath;
	}

	public void updateHeadImg(String userId, String headPath,
			long lastModifyTime) {
		String sqlStr = "update user set head='" + headPath
				+ "' , modifyTime='" + lastModifyTime + "' where userId="
				+ userId;
		try {
			int num = sql.executeUpdate(sqlStr);
			if (num == 1) {
				System.out.println("����ͷ��ɹ�: " + sqlStr);
			} else {
				System.out.println("����ͷ��ʧ�ܣ�" + sqlStr);
			}
		} catch (SQLException e) {
			System.out.println("updateHeadImg() exception=" + e.toString());
			System.out.println("sql=" + sqlStr);
		}
	}

	/**
	 * ����ĳ�˵ĺ��ѣ����غ����б�
	 * 
	 * @param selfId
	 *            ���Լ���ID
	 * @return
	 */
	public ArrayList<Friend> getFirends(String selfId) {
		ArrayList<Friend> list = new ArrayList<Friend>();
		String sqlStr = "select userId, nickName, sex, head, modifyTime from user, friend where userId=friendId and selfId='"
				+ selfId + "'";
		try {
			rs = sql.executeQuery(sqlStr);
			while (rs.next()) {
				Friend friend = new Friend();
				friend.setFriendID(rs.getString("userId"));
				friend.setFriendName(rs.getString("nickName"));
				friend.setSex(rs.getString("sex"));
				friend.setHead(rs.getString("head"));
				friend.setHeadModifyTime(rs.getString("modifyTime"));
				list.add(friend);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * ��ȡ�����Լ��Ļ�δ���͵���Ϣ
	 * 
	 *
	 * @return
	 */
	public ArrayList<Message> getMessages(String receiveId) {
		ArrayList<Message> list = new ArrayList<Message>();
		String sqlStr = "select * from message where receiveId='" + receiveId
				+ "'";
		try {
			rs = sql.executeQuery(sqlStr);
			LogUtil.record("DBUtil getMessage() ����ķ����α�=" + rs);
			while (rs.next()) {
				Message message = new Message();
				message.setSendId(rs.getString("sendId"));
				message.setReceiveId(receiveId);
				message.setType(rs.getInt("type"));
				message.setTime(rs.getString("time"));
				message.setContent(rs.getString("content"));
				list.add(message);
			}
		} catch (SQLException e) {
			LogUtil.record("��������ȡ������Ϣʱ������=" + e.toString());
			e.printStackTrace();
		}

		return list;
	}

	public void saveMessage(String sendId, String receiveId, int type,
			String time, String content) {
		String sqlStr = "insert into message values(" + sendId + ", "
				+ receiveId + ", " + type + ", '" + time + "', '" + content
				+ "')";
		System.out.println("saveMessage() sqlStr=" + sqlStr);
		try {
			sql.executeUpdate(sqlStr);
			System.out.println("������Ϣ�ɹ�");
		} catch (SQLException e) {
			System.out.println("������Ϣʧ��");
			e.printStackTrace();
		}
	}

	// ɾ��ĳ����Ϣ
	public void deleteMessages(String receiveId) {
		LogUtil.record("��������ѷ���������Ϣ�������ߣ����Կ���ɾ����ص���Ϣ");
		String deleteStr = "delete from message where receiveId='" + receiveId
				+ "'";
		try {
			sql.executeUpdate(deleteStr);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public User searchUser(String userId) {
		String sqlStr = "select nickName,email, sex, head, modifyTime from user where userId='"
				+ userId + "'";
		try {
			rs = sql.executeQuery(sqlStr);
			if (rs != null) {
				rs.next();
				User user = new User();
				user.setUserId(userId);
				user.setNickName(rs.getString("nickName"));
				user.setEmail(rs.getString("email"));
				user.setSex(rs.getString("sex"));
				user.setHead(rs.getString("head"));
				user.setLastModityTime(Long.parseLong(rs
						.getString("modifyTime")));

				return user;
			}else{
				return null;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean addFriend(String selfId, String friendId) {
		boolean result = true;
		String sqlStr1 = "insert into friend(selfId, friendId) values('"
				+ selfId + "', '" + friendId + "')";
		String sqlStr2 = "insert into friend(selfId, friendId) values('"
				+ friendId + "', '" + selfId + "')";
		try {
			con.setAutoCommit(false);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		try {
			System.out.println("addFriend() sql1=" + sqlStr1);
			System.out.println("addFriend() sql2=" + sqlStr2);
			sql.executeUpdate(sqlStr1);
			sql.executeUpdate(sqlStr2);

			// �ύ����
			con.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			try {
				// �ع�����
				con.rollback();
				con.setAutoCommit(true);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}

			result = false;
		}

		return result;
	}

	public void close() {
		try {
			if (con != null)
				con.close();
			if (sql != null)
				sql.close();
			if (rs != null)
				rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
