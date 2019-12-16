package chatting;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

/**
 * @author HungryShark
 * @since 2019.12.16
 */

public class ChatHandler extends JFrame implements ActionListener, Runnable {
	JList<String> roomInfo, roomUser, waitInfo;
	JScrollPane sp_roomInfo, sp_roomUser, sp_waitInfo;
	JButton bt_create, bt_enter, bt_exit;

	JPanel p;
	ChatClient cc;

	// 소켓 입출력객체
	BufferedReader in;
	OutputStream out;

	String selectedRoom;

	public ChatHandler() {
		setTitle("대기실");

		cc = new ChatClient();
		roomInfo = new JList<String>();
		roomInfo.setBorder(new TitledBorder("방정보"));

		// 방 정보 마우스 이벤트 
		roomInfo.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				String str = roomInfo.getSelectedValue(); // 선택한 방
				if (str == null)
					return;
				selectedRoom = str.substring(0, str.indexOf("-"));
				sendMsg("170|" + selectedRoom); // 채팅방 내의 인원정보 요청
			}
		});

		roomUser = new JList<String>();
		roomUser.setBorder(new TitledBorder("인원정보"));
		waitInfo = new JList<String>();
		waitInfo.setBorder(new TitledBorder("대기실정보"));
		sp_roomInfo = new JScrollPane(roomInfo);
		sp_roomUser = new JScrollPane(roomUser);
		sp_waitInfo = new JScrollPane(waitInfo);
		bt_create = new JButton("방만들기");
		bt_enter = new JButton("방들어가기");
		bt_exit = new JButton("종료하기");

		p = new JPanel();
		sp_roomInfo.setBounds(10, 10, 300, 300);
		sp_roomUser.setBounds(320, 10, 150, 300);
		sp_waitInfo.setBounds(10, 320, 300, 130);
		bt_create.setBounds(320, 330, 150, 30);
		bt_enter.setBounds(320, 370, 150, 30);
		bt_exit.setBounds(320, 410, 150, 30);
		p.setLayout(null);
		p.setBackground(Color.orange);
		p.add(sp_roomInfo);
		p.add(sp_roomUser);
		p.add(sp_waitInfo);
		p.add(bt_create);
		p.add(bt_enter);
		p.add(bt_exit);
		add(p);

		setBounds(300, 200, 500, 500);
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		connect(); // 서버연결시도 (in,out객체생성)
		new Thread(this).start(); // 서버메시지 대기
		sendMsg("100|");// (대기실)접속 알림
		String nickName = JOptionPane.showInputDialog(this, "닉네임:");
		if(nickName==null) {
			System.exit(0); // 강제종료
		}
		sendMsg("150|" + nickName);// 닉네임 전달
		eventUp();
	}

	private void eventUp() { // 이벤트소스-이벤트처리부 연결

		// 대기실(ChatHandler)
		bt_create.addActionListener(this);
		bt_enter.addActionListener(this);
		bt_exit.addActionListener(this);
		
		// 채팅방(ChatClient)
		cc.tf_msg.addActionListener(this);
		cc.bt_change.addActionListener(this);
		cc.bt_exit.addActionListener(this);
	}

	@Override

	// 이벤트별로 서버로 요청
	public void actionPerformed(ActionEvent e) { 
		Object ob = e.getSource();
		
		// 방 만들기 버튼 이벤트 
		if (ob == bt_create) { 
			String title = JOptionPane.showInputDialog(this, "방제목:");
			if(title == null)
				System.exit(0); // 강제종료
			sendMsg("160|" + title); 
			cc.setTitle("채팅방-[" + title + "]");
			sendMsg("175|"); // 채팅방내 인원정보 요청
			setVisible(false);
			cc.setVisible(true); // 채팅방으로 이동
		} else if (ob == bt_enter) { // 방 들어가기 버튼 이벤트 
			if (selectedRoom == null) {
				JOptionPane.showMessageDialog(this, "방을 선택하세요.");
				return;
			}
			sendMsg("200|" + selectedRoom);
			sendMsg("175|"); // 채팅방내 인원정보 요청
			setVisible(false);
			cc.setVisible(true);
		} else if (ob == cc.bt_exit) { // 채팅방 나가기 버튼 이벤트
			sendMsg("400|");
			cc.setVisible(false);
			setVisible(true);
		} else if (ob == cc.tf_msg) { // 채팅방 메시지 전송 
			String msg = cc.tf_msg.getText();
			if (msg.length() > 0) {
				sendMsg("300|" + msg);
				cc.tf_msg.setText("");
			}
		}
		else if (ob == bt_exit) { // 나가기(프로그램종료) 이벤트
			System.exit(0); // 현재 응용프로그램 종료하기
		}

	}

	// 서버연결
	public void connect() { 
		try {
			Socket s = new Socket("localhost", 5000); 
			in = new BufferedReader(new InputStreamReader(s.getInputStream()));	
			out = s.getOutputStream();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 서버에게 메시지 송신
	public void sendMsg(String msg) {
		try {
			out.write((msg + "\n").getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// GUI에 반영  
	public void run() { 
		try {
			while (true) {
				String msg = in.readLine(); // 서버로부터 메시지를 수신함
				// ex) "160|끝말잇기방--1", "300|메시지 전달받아라"
				
				// 메시지 프로토콜 분류
				String msgs[] = msg.split("\\|");
				String protocol = msgs[0];
				
				switch (protocol) {
				case "300":
					cc.ta.append(msgs[1] + "\n");
					cc.ta.setCaretPosition(cc.ta.getText().length());
					break;

				// (대기실에서) 채팅방 리스트 업데이트 
				case "160":
					if (msgs.length > 1) {
						String roomNames[] = msgs[1].split(",");
						roomInfo.setListData(roomNames);
					} 
					break;

				// (대기실에서) 채팅방 인원정보
				case "170":
					String roomUsers[] = msgs[1].split(",");
					roomUser.setListData(roomUsers);
					break;

				// (채팅방에서) 채팅방 인원정보
				case "175":
					String myRoomUsers[] = msgs[1].split(",");
					cc.li_user.setListData(myRoomUsers);
					break;

				// 대기실에 있는 유저정보
				case "180":
					String waitUsers[] = msgs[1].split(",");
					waitInfo.setListData(waitUsers);
					break;

				// 채팅방 입장
				case "200":
					cc.ta.append("===============[" + msgs[1] + "]님 입장===============\n");
					cc.ta.setCaretPosition(cc.ta.getText().length());
					break;

				// 채팅방 퇴장
				case "400":
					cc.ta.append("===============[" + msgs[1] + "]님 퇴장===============\n");
					cc.ta.setCaretPosition(cc.ta.getText().length());
					break;

				// 생성된 방의 제목 가져오기
				case "202":
					cc.setTitle("채팅방-[" + msgs[1] + "]");
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new ChatHandler();
	}
}
