package chatting;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

/**
 * @author HungryShark
 * @since 2019.12.16
 */
public class Server implements Runnable {

	Vector<Service> allUser; // 모든 사용자(대기실 + 채팅방 사용자)
	Vector<Service> waitUser; // 대기실 사용자
	Vector<Room> roomList; // 채팅방사용자
	
	public Server() {
		allUser = new Vector<>();
		waitUser = new Vector<>();
		roomList = new Vector<>();
		new Thread(this).start(); // Thread t = new Thread(run메소드의 위치); t.start();
	}

	@Override
	public void run() {
		try {
			ServerSocket ss = new ServerSocket(5000); // 서버소켓
			System.out.println("Start Server.......");
			while (true) {
				Socket s = ss.accept(); // 클라이언트 접속 대기
				Service ser = new Service(s, this);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}// run

	public static void main(String[] args) {
		new Server();
	}
}
