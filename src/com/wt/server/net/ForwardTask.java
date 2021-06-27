package com.wt.server.net;

import com.wt.server.common.Friend;
import com.wt.server.common.Message;
import com.wt.server.common.User;
import com.wt.server.util.Config;
import com.wt.server.util.DbUtil;
import com.wt.server.util.FileUtil;
import com.wt.server.util.LogUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * �������˽�����Ϣת����Task
 * 
 * @author www
 *
 */
public class ForwardTask extends Task {

	static HashMap<String, Socket> map = new HashMap<String, Socket>();
	Socket socket;
	DataInputStream dis;
	DataOutputStream dos;
	DbUtil dbUtil;
	LogUtil log;
	private boolean onWork = true;

	public ForwardTask(Socket socket) {
		this.socket = socket;
		log = new LogUtil();

		dbUtil = new DbUtil();

		try {
			dis = new DataInputStream(new BufferedInputStream(
					socket.getInputStream()));
			dos = new DataOutputStream(new BufferedOutputStream(
					socket.getOutputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Task[] taskCore() throws Exception {
		return null;
	}

	@Override
	protected boolean useDb() {
		return false;
	}

	@Override
	protected boolean needExecuteImmediate() {
		return false;
	}

	@Override
	public String info() {
		return null;
	}

	/**
	 * �����̹߳���״̬��true��ʾ���У�false��ʾ���ر�
	 * 
	 * @param state
	 */
	private void setWorkState(boolean state) {
		onWork = state;
	}

	/**
	 * ����ִ�����
	 */
	public void run() {
		while (onWork) {
			// ������
			try {
				receiveMsg();
			} catch (Exception e) { // �����쳣��������ͨ�������ӶϿ���������ѭ����һ��Task����ִ�����
				e.printStackTrace();
				break;
			}
		}

		try {
			if (socket != null)
				socket.close();
			if (dis != null)
				dis.close();
			if (dos != null)
				dos.close();

			socket = null;
			dis = null;
			dos = null;
		} catch (IOException e) {
			LogUtil.record("�����û��˳�ʱ�����쳣");
		}
	}

	// ������Ϣ
	public void receiveMsg() throws IOException {

		// ��ȡ�������ͣ���¼��ע�ᣬ����ͷ��ȵ�
		int requestType = dis.readInt();

		switch (requestType) {
		case Config.REQUEST_LOGIN: // ������½������
			handLogin();
			break;
		case Config.REQUEST_REGIST: // ����ע�ᡱ����
			handRegist();
			break;
		case Config.REQUEST_ADD_FRIEND: // ������Ӻ��Ѳ���������
			handAddFriend();
			break;
		case Config.REQUEST_ADD_FRIEND_request:// ������Ӻ�����������
			handRequestAddFriend();
			break;
		case Config.ADD_FRIEND_success:// ������Ӻ��ѳɹ�������
			handAddFriendSuc();
			break;
		case Config.REQUEST_UPDATE_HEAD: // ��������ͷ������
			handUpdateHead();
			break;
		case Config.REQUEST_GET_HEAD: // ������ȡͷ������
			handGetHead();
			break;
		case Config.REQUEST_SEND_TXT: // ���������ı���Ϣ������
			handSendText();
			break;
		case Config.REQUEST_SEND_IMG: // ��������ͼƬ��Ϣ������
			handSendImgOrAudio(Config.RECEIVE_IMG, Config.MESSAGE_TYPE_IMG);
			break;
		case Config.REQUEST_SEND_AUDIO: // ��������������Ϣ������
			handSendImgOrAudio(Config.RECEIVE_AUDIO, Config.MESSAGE_TYPE_AUDIO);
			break;
		case Config.REQUEST_GET_OFFLINE_MSG: // ������ȡ������Ϣ������
			handGetOfflineMsg();
			break;
		case Config.REQUEST_GET_FRIENDS: // ������ȡ�����б�����
			handGetFriends();
			break;
		case Config.REQUEST_SEARCH_USER: // ������ѯ�û���Ϣ������
			handSearchUser();
			break;
		case Config.REQUEST_GET_USER:
			handGetUser();
			break;
		case Config.RESULT_GET_HEAD:
			handGetHead();
			break;
		case Config.REQUEST_EXIT: // �����˳���������
			handExit();
			break;
		}
	}

	private void handGetUser() {
		try {
			String userId = dis.readUTF();

			User user = dbUtil.searchUser(userId);
			dos.writeInt(Config.ADD_FRIEND);
			dos.writeUTF(user.getUserId());
			dos.writeUTF(user.getNickName());
			dos.writeUTF(user.getSex());
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ��ȡָ���û���ͷ��
	private void handGetHead() {
		try {
			String userId = dis.readUTF();

			User user = dbUtil.searchUser(userId);
			String headPath = user.getHead();
			String modifyTime = user.getLastModityTime() + "";

			if (headPath.length() != 0) {
				dos.writeInt(Config.RESULT_GET_HEAD);
				dos.writeUTF(userId);
				dos.writeUTF(modifyTime);
				dos.flush();
				//
				readFileSendData(headPath);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// �������ͷ����
	private void handUpdateHead() {
		try {
			String userId = dis.readUTF();
			long lastModifyTime = dis.readLong();

			File file = FileUtil.createHeadFile(Integer.parseInt(userId));

			receiveDataWriteFile(file.getAbsolutePath());

			String headPath = file.getAbsolutePath().replace("\\", "\\\\");
			dbUtil.updateHeadImg(userId, headPath, lastModifyTime);
		} catch (IOException e) {
			System.out.println("handUpdateHead() exception=" + e.toString());
		}
	}

	private void handRegist() {
		try {
			System.out.println("handRegist(): ���յ�ע���û�����");

			String nickName = dis.readUTF();
			String pwd = dis.readUTF();
			String sex = dis.readUTF();
			String email = dis.readUTF();

			boolean isHasImg = dis.readBoolean();

			System.out.println("handRegist() nickName=" + nickName + ", sex="
					+ sex);

			// �鿴���ݿ����Ƿ��������û���������û��Ҫ���������Ĳ�������������ǳ���ͬ���û���ID������ϵͳ�ṩ�ģ�ID����Ψһ�ı���ʶ
			// ������ݿ���ID�������û�ID
			int id = dbUtil.getMaxUserId() + 1;
			System.out.println("handRegist(): ���û�ID=" + id);
			LogUtil.record("---------------------------------------");
			LogUtil.record("���յ�ע���û�����name=" + nickName + " , sex=" + sex
					+ " , isHasImg=" + isHasImg);
			System.out.println("handRegist(): �û�ע����Ϣ name=" + nickName
					+ " , sex=" + sex + " , isHasImg=" + isHasImg);

			String headPath = "";
			long lastModifyTime = 1l; // ����û��ͷ����û�������modifyTime�ֶ�Ϊ1l

			if (isHasImg == true) {
				File file = FileUtil.createHeadFile(id);
				lastModifyTime = file.lastModified();

				receiveDataWriteFile(file.getAbsolutePath());

				headPath = file.getAbsolutePath().replace("\\", "\\\\");
				System.out.println(headPath);
			}

			boolean registResult = dbUtil.regist(nickName, pwd, email, sex,
					headPath, lastModifyTime);

			if (registResult == true) {

				int id1 = dbUtil.getMaxUserId();
				dos.writeInt(Config.RESULT_REGIST);
				dos.writeBoolean(true); // ע��ɹ�
				dos.writeInt(id1); // ������ID
				dos.writeLong(lastModifyTime);

				LogUtil.record("ע��ɹ���userId=" + id1);
			} else {
				dos.writeInt(Config.RESULT_REGIST);
				dos.writeBoolean(false); // ע��ʧ��

				LogUtil.record("ע��ʧ�ܣ�");
			}
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void receiveDataWriteFile(String filePath)
			throws FileNotFoundException, IOException {
		DataOutputStream ddos = new DataOutputStream(new FileOutputStream(
				filePath)); // ��������ͼƬд�뱾��SD��
		int length = 0;
		int totalNum = 0;
		byte[] buffer = new byte[2048];
		while ((length = dis.readInt()) != 0) {
			length = dis.read(buffer, 0, length);
			totalNum += length;
			ddos.write(buffer, 0, length);
			ddos.flush();
		}

		if (ddos != null) {
			try {
				ddos.close();
				ddos = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Log.i(TAG, "handReceiveData() ���� img.totalNum="+totalNum);
	}

	private void handExit() {
		try {
			String userId = dis.readUTF();
			LogUtil.record("---------------------------------------");
			LogUtil.record("�����û�" + userId + "�����˳���¼");

			// �����߳�
			setWorkState(false);

			LogUtil.record("�û�" + userId + "�˳�ǰ,����" + map.size() + "���û�����");
			// ע�����ﲻ����ת����Ϣ��������Ҫ��ѯ�����˳�������û��Ƿ����ߡ�ֻҪ��ͬ��������Socket���Ӿ������ߵ�
			map.remove(userId);
			LogUtil.record("�û�" + userId + "�˳���,����" + map.size() + "���û�����");
			dbUtil.close(); // �رյ����ݿ������

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handRequestAddFriend() {
		try {
			String selfId = dis.readUTF();
			String friendId = dis.readUTF();
			LogUtil.record("�û�" + selfId + "���û�" + friendId + "��������Ӻ��ѵ�����");

			// ����û�friendId���ߣ�������������Ӻ��ѵ���Ϣ���������ֻ��˴洢��Ժ��ѹ�ϵ�����������ߣ�����Ժ��ѹ�ϵ��Ϊ������Ϣ�ݴ��ڷ�������
			if (map.containsKey(friendId)) {
				// �����ݿ��л�ȡIdΪselfId���û�����ϸ��Ϣ
				// A��������������BΪ���ѵ������������Ӧ��A����Ϣ������B��B�������Ϣ����B��friend��
				User user = dbUtil.searchUser(selfId);
				if (user != null) { // ���ڸ��û�����ʵ���ֵ����Ƕ���ģ���Ϊ�ǿͻ����Ȳ�ѯ�˸��û�����Ϣ�������Ϊ���ѵģ�������Ӻ��������Id�ض��Ǵ��ڵ�
					Socket socket = map.get(friendId);
					DataOutputStream out = new DataOutputStream(
							socket.getOutputStream());
					out.writeInt(Config.RESULT_ADD_FRIEND_request);
					out.writeUTF(user.getUserId());
					out.writeUTF(user.getNickName());
					out.writeUTF(user.getSex());
					out.writeUTF(user.getEmail());
					out.flush();

					String headPath = user.getHead();
					File file = new File(headPath);
					if (file.exists() == true) {
						out.writeInt(Config.USER_HAS_IMG);
						out.writeUTF(file.lastModified() + "");
						out.flush();

						// �˴��Ĵ���ͬreadFileSendData()����
						DataInputStream ddis = new DataInputStream(
								new FileInputStream(headPath));
						int length = 0;
						int totalNum = 0;
						byte[] buffer = new byte[1024];

						while ((length = ddis.read(buffer)) != -1) {
							totalNum += length;
							out.writeInt(length);
							out.write(buffer, 0, length);
							out.flush();
						}

						out.writeInt(0);
						out.flush();

						if (ddis != null) {
							ddis.close();
							ddis = null;
						}
					} else {
						out.writeInt(Config.USER_NOT_IMG);
						out.writeUTF(1l + "");
						out.flush();
					}
				}

			} else {
				// �������Ӻ���������Ϊ������Ϣ�洢
				LogUtil.record("�û�" + selfId + "��������û�" + friendId + "Ϊ���ѵ�����,"
						+ friendId + "�����ߣ��������Ӻ���������Ϊ������Ϣ�洢");
				dbUtil.saveMessage(selfId, friendId,
						Config.MESSAGE_TYPE_ADD_FRIEND, "", "");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void handAddFriendSuc() {
		try {
			String selfId = dis.readUTF();
			String friendId = dis.readUTF();
			System.out.println("QQQQQQQQQQ");
			if (map.containsKey(friendId)) {
				User user = dbUtil.searchUser(selfId);
				Socket socket = map.get(friendId);
				DataOutputStream out = new DataOutputStream(
						socket.getOutputStream());
				out.writeInt(Config.ADD_FRIEND_SUCCESS);
				out.writeUTF(user.getUserId());
				out.writeUTF(user.getNickName());
				out.writeUTF(user.getSex());
				out.writeUTF(user.getEmail());
				out.flush();

				String headPath = user.getHead();
				File file = new File(headPath);
				if (file.exists() == true) {
					out.writeInt(Config.USER_HAS_IMG);
					out.writeUTF(file.lastModified() + "");
					out.flush();

					// �˴��Ĵ���ͬreadFileSendData()����
					DataInputStream ddis = new DataInputStream(
							new FileInputStream(headPath));
					int length = 0;
					int totalNum = 0;
					byte[] buffer = new byte[1024];

					while ((length = ddis.read(buffer)) != -1) {
						totalNum += length;
						out.writeInt(length);
						out.write(buffer, 0, length);
						out.flush();
					}

					out.writeInt(0);
					out.flush();

					if (ddis != null) {
						ddis.close();
						ddis = null;
					}
				} else {
					out.writeInt(Config.USER_NOT_IMG);
					out.writeUTF(1l + "");
					out.flush();
					System.out.println("WWWWWWWWW");
				}
			} else {
				dbUtil.saveMessage(selfId, friendId,
						Config.MESSAGE_TYPE_ADD_FRIEND, "", "");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void handAddFriend() {
		try {
			String selfId = dis.readUTF();
			String friendId = dis.readUTF();

			// �����ݿ��������Ժ��ѹ�ϵ
			boolean result = dbUtil.addFriend(selfId, friendId);
			LogUtil.record("�û�" + selfId + "��������û�" + friendId
					+ "Ϊ���ѵ�����,�������˲�����Ժ��ѹ�ϵ��" + result);
			// ����û�friendId���ߣ�������������Ӻ��ѵ���Ϣ���������ֻ��˴洢��Ժ��ѹ�ϵ�����������ߣ�����Ժ��ѹ�ϵ��Ϊ������Ϣ�ݴ��ڷ�������
			if (map.containsKey(friendId)) {
				// �����ݿ��л�ȡIdΪselfId���û�����ϸ��Ϣ
				// A��������������BΪ���ѵ������������Ӧ��A����Ϣ������B��B�������Ϣ����B��friend��
				User user = dbUtil.searchUser(selfId);
				if (user != null) { // ���ڸ��û�����ʵ���ֵ����Ƕ���ģ���Ϊ�ǿͻ����Ȳ�ѯ�˸��û�����Ϣ�������Ϊ���ѵģ�������Ӻ��������Id�ض��Ǵ��ڵ�
					Socket socket = map.get(friendId);
					DataOutputStream out = new DataOutputStream(
							socket.getOutputStream());
					out.writeInt(Config.ADD_FRIEND);
					out.writeUTF(user.getUserId());
					out.writeUTF(user.getNickName());
					out.writeUTF(user.getSex());
					out.writeUTF(user.getEmail());
					out.flush();

					String headPath = user.getHead();
					File file = new File(headPath);
					if (file.exists() == true) {
						out.writeInt(Config.USER_HAS_IMG);
						out.writeUTF(file.lastModified() + "");
						out.flush();

						// �˴��Ĵ���ͬreadFileSendData()����
						DataInputStream ddis = new DataInputStream(
								new FileInputStream(headPath));
						int length = 0;
						int totalNum = 0;
						byte[] buffer = new byte[1024];

						while ((length = ddis.read(buffer)) != -1) {
							totalNum += length;
							out.writeInt(length);
							out.write(buffer, 0, length);
							out.flush();
						}

						out.writeInt(0);
						out.flush();

						if (ddis != null) {
							ddis.close();
							ddis = null;
						}
					} else {
						out.writeInt(Config.USER_NOT_IMG);
						out.writeUTF(1l + "");
						out.flush();
					}
				}
			} else {
				// �������Ӻ���������Ϊ������Ϣ�洢
				LogUtil.record("�û�" + selfId + "��������û�" + friendId + "Ϊ���ѵ�����,"
						+ friendId + "�����ߣ��������Ӻ���������Ϊ������Ϣ�洢");
				dbUtil.saveMessage(selfId, friendId,
						Config.MESSAGE_TYPE_ADD_FRIEND, "", "");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handSearchUser() {
		try {
			String userId = dis.readUTF();
			// �ڷ������ϲ��Ҹ��û� �����û��ĸ�����Ϣ
			User user = dbUtil.searchUser(userId);
			if (user != null) {
				dos.writeInt(Config.RESULT_SEARCH_USER);
				dos.writeInt(Config.SEARCH_USER_SUCCESS);
				dos.writeUTF(user.getUserId());
				dos.writeUTF(user.getNickName());
				dos.writeUTF(user.getSex());
				dos.writeUTF(user.getEmail());
				// ͷ���·��
				String head = user.getHead();
				File file = new File(head);
				// ������ڸ�ͷ��
				if (file.exists() == true) {
					dos.writeInt(Config.USER_HAS_IMG);

					DataInputStream in = new DataInputStream(
							new FileInputStream(file));
					// ���ֽ������0��ʼ��size���ֽڶ�д�����������
					int length = in.available();
					byte[] data = new byte[length];
					int size = in.read(data);
					in.close();
					in = null;
					dos.writeInt(size);
					dos.write(data, 0, size);
				}// ������ͷ��
				else {
					dos.writeInt(Config.USER_NOT_IMG);
				}

				dos.flush();
			} else {
				dos.writeInt(Config.RESULT_SEARCH_USER);
				dos.writeInt(Config.SEARCH_USER_FALSE);
				dos.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void handLogin() {
		try {
			System.out.println("handLogin() �����½����");
			String userId = dis.readUTF();
			System.out.println("handLogin() userId=" + userId);
			String pwd = dis.readUTF();
			System.out.println("handLogin() pwd=" + pwd);

			LogUtil.record("------------------------------------------");
			LogUtil.record("ʹ���û���=" + userId + ", ����=" + pwd + "  ��¼");
			User user = dbUtil.login(userId, pwd);
			if (user != null) {
				System.out.println(userId + "��½, sex=" + user.getSex());
				dos.writeInt(Config.RESULT_LOGIN);
				dos.writeBoolean(true);
				dos.writeUTF(user.getUserId());
				dos.writeUTF(user.getNickName());
				dos.writeUTF(user.getSex());
				dos.writeUTF(user.getEmail());
				String head = user.getHead();
				System.out.println("handLogin() head=" + head);
				if ("".equals(head)) { // ���Ϊ""����û��ͷ��
					dos.writeInt(Config.USER_NOT_IMG);
					System.out.println("handLogin() û��ͷ��");
				} else {
					DataInputStream in = null;
					try {
						in = new DataInputStream(new FileInputStream(head));
					} catch (FileNotFoundException e) {
						LogUtil.record("�����쳣��" + e.toString());
						e.printStackTrace();
					}
					if (in != null) { // ָ�����ļ�û���ҵ������ᷢ��FileNotFoundException���ǲ��ܴ���
						dos.writeInt(Config.USER_HAS_IMG);
						dos.writeLong(user.getLastModityTime());

						System.out.println("handLogin() ��ͷ��");
					} else {
						dos.writeInt(Config.USER_NOT_IMG);
						System.out.println("handLogin() û���ҵ�ͷ���ļ�");
					}
				}

				dos.flush();

				map.put(userId, socket);
				LogUtil.record("�û�" + userId + "��¼�ɹ�");
			} else {
				dos.writeInt(Config.RESULT_LOGIN);
				dos.writeBoolean(false);
				dos.flush();

				LogUtil.record("�û�" + userId + "��¼ʧ�ܣ�userId=" + userId
						+ ", pwd=" + pwd);
			}
		} catch (IOException e) {
			LogUtil.record("�����쳣��" + e.toString());
			e.printStackTrace();
		}
	}

	// �����ı���Ϣ
	public void handSendText() {
		try {
			String sendId = dis.readUTF();
			String receiveId = dis.readUTF();
			String time = dis.readUTF();
			String content = dis.readUTF();
			System.out.println("���յ��ͻ���" + sendId + "��������Ϣ");
			log.record("------------------------------------------------------------------------");
			log.record("�û�" + sendId + " ���û�" + receiveId + "�����ı���Ϣ='"
					+ content + "'");
			// �жϽ������Ƿ�����
			if (map.containsKey(receiveId)) {
				Socket socket = map.get(receiveId);
				log.record("������ͬ��Ϣ������" + sendId + "���ӵ�Socket="
						+ map.get(sendId));
				log.record("������ͬ��Ϣ������" + receiveId + "���ӵ�Socket=" + socket);
				DataOutputStream out = new DataOutputStream(
						socket.getOutputStream());
				out.writeInt(Config.RECEIVE_TEXT);
				out.writeUTF(sendId);
				out.writeUTF(receiveId);
				out.writeUTF(time);
				out.writeUTF(content);
				out.flush();

				log.record("�û�" + receiveId + "���ߣ�����ֱ�ӽ��յ���Ϣ����Ϣ��ת����������"
						+ receiveId);
			} else {
				// д�����ݿ�
				dbUtil.saveMessage(sendId, receiveId, Config.MESSAGE_TYPE_TXT,
						time, content);
				log.record("�û�" + receiveId + "�����ߣ��Ȱ���Ϣ�ݴ��ڷ�������");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handSendImg() {
		try {
			String sendId = dis.readUTF();
			String receiveId = dis.readUTF();
			String time = dis.readUTF();

			log.record("�û�" + sendId + " ���û�" + receiveId + "����ͼƬ��Ϣ'");
			// �жϽ������Ƿ�����
			if (map.containsKey(receiveId)) {
				Socket socket = map.get(receiveId);
				log.record("������ͬ�û�" + receiveId + "���ӵ�Socket=" + socket);
				DataOutputStream out = new DataOutputStream(
						socket.getOutputStream());
				out.writeInt(Config.RECEIVE_IMG);
				out.writeUTF(sendId);
				out.writeUTF(receiveId);
				out.writeUTF(time);
				out.flush();

				int length = 0;
				int totalNum = 0;
				byte[] buffer = new byte[2048];
				while ((length = dis.readInt()) != 0) {
					length = dis.read(buffer, 0, length);
					totalNum += length;

					out.writeInt(length);
					out.write(buffer, 0, length);
					out.flush();
				}

				out.writeInt(0);
				out.flush();

				System.out.println("img.totalNum=" + totalNum);

				log.record("�û�" + receiveId + "���ߣ�����ֱ�ӽ��յ���Ϣ");
			} else {
				// д�����ݿ�
				File file = FileUtil
						.createFile(sendId, Config.MESSAGE_TYPE_IMG);
				FileOutputStream ou = new FileOutputStream(file);

				int length = 0;
				int totalNum = 0;
				byte[] buffer = new byte[2048];
				while ((length = dis.readInt()) != 0) {
					length = dis.read(buffer, 0, length);
					totalNum += length;
					ou.write(buffer, 0, length);
					ou.flush();
				}
				ou.close();
				ou = null;

				System.out.println("img.totalNum=" + totalNum);

				dbUtil.saveMessage(sendId, receiveId, Config.MESSAGE_TYPE_IMG,
						time, file.getAbsolutePath().replace("\\", "\\\\"));
				log.record("�û�" + receiveId + "�����ߣ��Ȱ���Ϣ�ݴ��ڷ�������");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handSendImgOrAudio(int requestType, int messageType) {
		try {
			String sendId = dis.readUTF();
			String receiveId = dis.readUTF();
			String time = dis.readUTF();

			if (messageType == Config.MESSAGE_TYPE_IMG) {
				log.record("�û�" + sendId + " ���û�" + receiveId + "����ͼƬ��Ϣ'");
			} else {
				log.record("�û�" + sendId + " ���û�" + receiveId + "����������Ϣ'");
			}

			// �жϽ������Ƿ�����
			if (map.containsKey(receiveId)) {
				Socket socket = map.get(receiveId);
				log.record("������ͬ�û�" + receiveId + "���ӵ�Socket=" + socket);
				DataOutputStream out = new DataOutputStream(
						socket.getOutputStream());
				out.writeInt(requestType);
				out.writeUTF(sendId);
				out.writeUTF(receiveId);
				out.writeUTF(time);
				out.flush();

				int length = 0;
				int totalNum = 0;
				byte[] buffer = new byte[1024];
				while ((length = dis.readInt()) != 0) {
					length = dis.read(buffer, 0, length);
					totalNum += length;

					out.writeInt(length);
					out.write(buffer, 0, length);
					out.flush();
				}

				out.writeInt(0);
				out.flush();

				System.out.println("img.totalNum=" + totalNum);

				log.record("�û�" + receiveId + "���ߣ�����ֱ�ӽ��յ���Ϣ");
			} else {
				// д�����ݿ�
				File file = FileUtil.createFile(sendId, messageType);
				FileOutputStream ou = new FileOutputStream(file);

				int length = 0;
				int totalNum = 0;
				byte[] buffer = new byte[1024];
				while ((length = dis.readInt()) != 0) {
					length = dis.read(buffer, 0, length);
					totalNum += length;
					ou.write(buffer, 0, length);
					ou.flush();
				}
				ou.close();
				ou = null;

				System.out.println("img.totalNum=" + totalNum);

				dbUtil.saveMessage(sendId, receiveId, messageType, time, file
						.getAbsolutePath().replace("\\", "\\\\"));
				log.record("�û�" + receiveId + "�����ߣ��Ȱ���Ϣ�ݴ��ڷ�������");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//
	private void handSendAudio() {
		try {
			String sendId = dis.readUTF();
			String receiveId = dis.readUTF();
			String time = dis.readUTF();
			int length = dis.readInt();
			byte[] data = new byte[length];
			int realNum = dis.read(data);

			log.record("�û�" + sendId + " ���û�" + receiveId + "����������Ϣ'");
			// �жϽ������Ƿ�����
			if (map.containsKey(receiveId)) {
				Socket socket = map.get(receiveId);
				log.record("������ͬ�û�" + receiveId + "���ӵ�Socket=" + socket);
				DataOutputStream out = new DataOutputStream(
						socket.getOutputStream());
				out.writeInt(Config.RECEIVE_AUDIO);
				out.writeUTF(sendId);
				out.writeUTF(receiveId);
				out.writeUTF(time);
				out.writeInt(realNum);
				out.write(data, 0, realNum);
				out.flush();

				log.record("�û�" + receiveId + "���ߣ�����ֱ�ӽ��յ���Ϣ");
			} else {
				// д�����ݿ�
				File file = FileUtil.createFile(sendId,
						Config.MESSAGE_TYPE_AUDIO);
				FileOutputStream ou = new FileOutputStream(file);
				ou.write(data, 0, realNum);
				ou.flush();
				ou.close();
				ou = null;

				dbUtil.saveMessage(sendId, receiveId,
						Config.MESSAGE_TYPE_AUDIO, time, file.getAbsolutePath());
				log.record("�û�" + receiveId + "�����ߣ��Ȱ���Ϣ�ݴ��ڷ�������");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ������ȡ������Ϣ������
	public void handGetOfflineMsg() {
		try {
			String selfId = dis.readUTF();
			ArrayList<Message> list = dbUtil.getMessages(selfId);
			int listSize = list.size();

			LogUtil.record("----------------------------------------------");
			LogUtil.record("�û�" + selfId + "������ȡ������Ϣ������,���ҵ�������ص�������Ϣ"
					+ listSize + "��");
			if (list != null && listSize > 0) {
				LogUtil.record("��������ѷ���������Ϣ�������ߣ����Կ���ɾ����ص���Ϣ");
				dbUtil.deleteMessages(selfId);
				Message msg = null;

				dos.writeInt(Config.RESULT_GET_OFFLINE_MSG);
				dos.writeInt(listSize);
				LogUtil.record("�ܹ�����" + listSize + "������Ϣ");
				for (int i = 0; i < listSize; i++) {
					msg = list.get(i);
					int type = msg.getType();
					if (type == Config.MESSAGE_TYPE_TXT) {
						dos.writeUTF(msg.getSendId());
						dos.writeUTF(msg.getReceiveId());
						dos.writeInt(msg.getType());
						dos.writeUTF(msg.getTime());
						dos.writeUTF(msg.getContent());
						dos.flush();
					} else {
						dos.writeUTF(msg.getSendId());
						dos.writeUTF(msg.getReceiveId());
						dos.writeInt(msg.getType());
						dos.writeUTF(msg.getTime());
						// ��ȡ��Ƶ�ļ����ֽ�����
						File file = new File(msg.getContent());
						DataInputStream in = new DataInputStream(
								new FileInputStream(file));
						int length = in.available();
						byte[] data = new byte[length];
						int size = in.read(data);
						dos.writeInt(size);
						dos.write(data, 0, size);
						dos.flush();

						in.close();
						in = null;

						// ɾ��ͼƬ�������ļ�
						file.delete();
					}

					LogUtil.record("���͵�" + (i + 1) + "��������Ϣ");
				}
			} else {
				System.out.println("��������û���й��û�" + selfId + "��δ����Ϣ");
			}
		} catch (IOException e) {
			LogUtil.record("handGetOfflineMsg �����쳣��" + e.toString());
			e.printStackTrace();
		}
	}

	public void handGetFriends() {
		try {
			String selfId = dis.readUTF();
			int length = dis.readInt();
			Map<String, String> map = new HashMap<String, String>();

			// ѭ������ͻ��˵ĺ����б�<friendId, modifyTime>
			for (int i = 0; i < length; i++) {
				String friendId = dis.readUTF();
				String modifyTime = dis.readUTF();
				map.put(friendId, modifyTime);
			}

			ArrayList<Friend> list = dbUtil.getFirends(selfId);
			dos.writeInt(Config.RESULT_GET_FRIENDS);
			int size = list.size();
			dos.writeInt(size); // ���߿ͻ���Ҫ���ն�������
			for (int i = 0; i < size; i++) {
				Friend friend = list.get(i);
				String friendId = friend.getFriendID();
				String headPath = friend.getHead();
				String modifyTime = friend.getHeadModifyTime();

				// �������������ѣ���Ƚ�ͷ��ʱ���;���������򽫷������˵�ͷ�񷢸��ͻ���
				if (map.containsKey(friendId)) {
					// ʱ�����ͬ�����������˵�ͷ�񷢸��ͻ���
					if (!map.get(friendId).equals(friend.getHeadModifyTime())) {
						dos.writeInt(Config.IMG_NEED_UPDATE);
						dos.writeUTF(modifyTime); // ͷ���ʱ���
						dos.flush();
						readFileSendData(headPath); // ��ȡ������ͷ��
					} else {
						// ʱ�����ͬ�������Ͳ���Ҫ�ӷ������˰�ͷ��ͼƬ���͵��ͻ���
						dos.writeInt(Config.IMG_NO_UPDATE);
						dos.flush();
					}
				} else { // �ͻ��˵����ݿ��л�û�а���������ѣ����Խ����û���ȫ����Ϣ�����ͻ���
					dos.writeInt(Config.ADD_FRIEND);
					dos.writeUTF(friendId);
					dos.writeUTF(friend.getFriendName());
					dos.writeUTF(friend.getSex());

					if (headPath != null && !headPath.equals("")) {
						dos.writeInt(Config.USER_HAS_IMG);
						dos.writeUTF(modifyTime);
						dos.flush();
						// ����ͷ��
						readFileSendData(headPath);
					} else {
						dos.writeInt(Config.USER_NOT_IMG);
						dos.writeUTF(modifyTime);
						dos.flush();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ��ȡ�ļ�(ͼƬ������)����������
	 * 
	 * @param filePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void readFileSendData(String filePath)
			throws FileNotFoundException, IOException {
		DataInputStream ddis = new DataInputStream(
				new FileInputStream(filePath));
		int length = 0;
		int totalNum = 0;
		byte[] buffer = new byte[1024];

		while ((length = ddis.read(buffer)) != -1) {
			totalNum += length;
			dos.writeInt(length);
			dos.write(buffer, 0, length);
			dos.flush();
		}

		dos.writeInt(0);
		dos.flush();

		if (ddis != null) {
			ddis.close();
			ddis = null;
		}
	}
}
