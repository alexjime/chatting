package chatting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Vector;

/**
 * @author HungryShark
 * @since 2019.12.16
 */

public class Service extends Thread {
	// 클라이언트가 입장한 방
	Room myRoom; 
	
	// 소켓 입출력 
	BufferedReader in;
	OutputStream out;
	
	// 전체 유저, 대기실에 있는 유저, 방 리스트
	Vector<Service> allUser; 
	Vector<Service> waitUser; 
	Vector<Room> roomList;
	
	// 유저 소켓, 유저 닉네임
	Socket s;
	String nickName;
	
	public Service(Socket s, Server server) {
		allUser = server.allUser;
		waitUser = server.waitUser;
		roomList = server.roomList;
		this.s = s;
		try {
			in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			out = s.getOutputStream();
			start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		try {
			while (true) {
				String msg = in.readLine(); // 클라이언트로부터 모든 메시지 수신
				
				if (msg == null)
					return; // 비정상 종료
				
				if (msg.trim().length() > 0) {
					// 실시간 서버 로그 확인 
					System.out.println("from Client: " + msg + " : " +
							s.getInetAddress().getHostAddress());
					
					// 메시지 프로토콜 분류
					String msgs[] = msg.split("\\|");
					String protocol = msgs[0];

					switch (protocol) {
					
					// 대기실 접속
					case "100": 
						allUser.add(this); 
						waitUser.add(this); 
						break;

					// 닉네임 설정 
					case "150": 
						nickName = msgs[1];
						sendWait("160|" + getRoomInfo());
						sendWait("180|" + getWaitUser());
						break;

					// 채팅방 생성 + 동시에 입장
					case "160": 
						myRoom = new Room();
						myRoom.title = msgs[1];
						myRoom.count = 1;
						myRoom.manager = nickName;
						roomList.add(myRoom);
						
						// 대기실에서 채팅방으로 전처리
						waitUser.remove(this);
						myRoom.userList.add(this);
						sendRoom("200|" + nickName); // 입장 문구

						// (대기실에서) 채팅방 리스트와 대기실에 있는 유저 업데이트
						sendWait("160|" + getRoomInfo());
						sendWait("180|" + getWaitUser());
						break;
						
					// (대기실에서) 채팅방 인원정보가 보임
					case "170": 
						sendTo("170|" + getRoomList(msgs[1]));
						break;

					// (채팅방에서) 채팅방 인원정보가 보임
					case "175": 
						sendRoom("175|" + getRoomList());
						break;

					// 채팅방 입장 
					case "200": 
						for (int i = 0; i < roomList.size(); i++) {
							Room r = roomList.get(i);
							if (r.title.equals(msgs[1])) {
								myRoom = r;
								myRoom.count++;
								break;
							}
						} 
						// 대기실 ----> 채팅방 이동
						waitUser.remove(this);
						myRoom.userList.add(this);
						sendRoom("200|" + nickName);// 방인원에게 입장 알림
						
						// 들어갈 방의 title전달
						sendTo("202|" + myRoom.title);
						
						// (대기실에서) 채팅방 리스트와 대기실에 있는 유저 업데이트
						sendWait("160|" + getRoomInfo());
						sendWait("180|" + getWaitUser());
						break;

					// 다른 클라이언트에게 메시지 전송
					case "300": 
						sendRoom("300|[" + nickName + "]▶ " + msgs[1]);
						break;

					// 채팅방 퇴장
					case "400": 
						myRoom.count--;// 인원수 감소
						sendRoom("400|" + nickName); // 퇴장 문구
						// 채팅방 ----> 대기실 
						myRoom.userList.remove(this);
						waitUser.add(this);
						
						// 방인원이 없으면 방을 제거
						if(myRoom.count == 0) { 
							roomList.remove(myRoom); 
							myRoom = null; 
						}
						else {
							sendRoom("175|" + getRoomList()); //방인원이 남아있다면 채팅방에 인원정보 업데이트
						}
						// (대기실에서) 채팅방 리스트와 대기실에 있는 유저 업데이트
						sendWait("180|" + getWaitUser()); 
						sendWait("160|" + getRoomInfo()); 
						break;
					}
				} 
			} 

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** 
	 *@return str "끝말잇기방--1,고기굽는방--2"
	 */
	public String getRoomInfo() {	
		String str = ""+","+""; // 리스트로 받아야해서 어쩔수없이 이렇게 선언..
		for (int i = 0; i < roomList.size(); i++) {

			Room r = roomList.get(i);
			if(r.count == 0) {
				str = ""+","+"";
			} else {
				str += r.title + "----------" + r.count;
				if (i < roomList.size() - 1)
					str += ",";
			}
		}
		System.out.println(str);
		return str;
	}

	/** 
	 * @ #getRoomList()
	 * @return str "철수,민수,영희"
	 */
	public String getRoomList() {
		String str = "";
		for (int i = 0; i < myRoom.userList.size(); i++) {
			Service ser = myRoom.userList.get(i);
			str += ser.nickName;
			if (i < myRoom.userList.size() - 1)
				str += ",";
		}
		return str;
	}

	/** 
	 * @ #getRoomList(String title)
	 * @return str "철수,민수,영희"
	 */
	public String getRoomList(String title) {
		String str = "";	
		for (int i = 0; i < roomList.size(); i++) {
			Room room = roomList.get(i);
			if (room.title.equals(title)) {
				for (int j = 0; j < room.userList.size(); j++) {
					Service ser = room.userList.get(j);
					str += ser.nickName;
					if (j < room.userList.size() - 1)
						str += ",";
				}
				break;
			}
		}
		return str;
	}

	/** 
	 * @return str "철수,민수,영희"
	 */
	public String getWaitUser() {
		String str = "";
		for (int i = 0; i < waitUser.size(); i++) {
			Service ser = waitUser.get(i);
			str += ser.nickName;
			if (i < waitUser.size() - 1)
				str += ",";
		}
		return str;
	}

	public void sendWait(String msg) {
		for (int i = 0; i < waitUser.size(); i++) {
			Service service = waitUser.get(i); 
			try {
				service.sendTo(msg);
			} catch (IOException e) {
				waitUser.remove(i--); 
				System.out.println("클라이언트 접속 끊음");
			}
		}
	}

	public void sendRoom(String msg) {
		for (int i = 0; i < myRoom.userList.size(); i++) {
			Service service = myRoom.userList.get(i); 
			try {
				service.sendTo(msg);
			} catch (IOException e) {
				myRoom.userList.remove(i--);
				System.out.println("클라이언트 접속 끊음");
			}
		}
	}

	public void sendTo(String msg) throws IOException {
		out.write((msg + "\n").getBytes());
	}

}
