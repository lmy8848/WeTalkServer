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
 * 这是中转服务器对数据库的操作
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
	 * 在构造方法中建立数据库连接
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
	 * 验证用户的合法性
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
//				System.out.println("不为空！！！！！！！！！！");
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
				System.out.println("对象为空！");
			}

		} catch (SQLException e) {
			e.printStackTrace();
			user = null;
		}

		return user;
	}

	// 获取ID号最大的用户ID
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
				System.out.println("regist() 成功");
				return true;
			} else {
				System.out.println("regist() 失败");
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
				System.out.println("更新头像成功: " + sqlStr);
			} else {
				System.out.println("更新头像失败：" + sqlStr);
			}
		} catch (SQLException e) {
			System.out.println("updateHeadImg() exception=" + e.toString());
			System.out.println("sql=" + sqlStr);
		}
	}

	/**
	 * 查找某人的好友，返回好友列表
	 * 
	 * @param selfId
	 *            ：自己的ID
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
	 * 获取发给自己的还未发送的消息
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
			LogUtil.record("DBUtil getMessage() 结果的返回游标=" + rs);
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
			LogUtil.record("服务器读取离线消息时出问题=" + e.toString());
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
			System.out.println("保存消息成功");
		} catch (SQLException e) {
			System.out.println("保存消息失败");
			e.printStackTrace();
		}
	}

	// 删除某条消息
	public void deleteMessages(String receiveId) {
		LogUtil.record("因服务器已发送离线消息给接收者，所以可以删除相关的消息");
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

			// 提交事物
			con.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			try {
				// 回滚事物
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
