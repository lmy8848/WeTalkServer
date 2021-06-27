package com.wt.server.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.wt.server.net.ForwardTask;
import com.wt.server.net.ThreadPool;

public class TestServer {
	public static void main(String[] args) {
		System.out.println("服务器启动成功!");
		try {
			ServerSocket serverSocket=new ServerSocket(9999);
			ThreadPool pool=ThreadPool.getInstance();
			while(true){
				Socket socket=serverSocket.accept();
				System.out.println("服务器接收到一个客户端连接");
				ForwardTask task=new ForwardTask(socket);
				pool.addTask(task);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
