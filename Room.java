package multi_server;

import java.util.Vector;

/**
 * @author HungryShark
 * @since 2019.12.16
 */

public class Room {

	Vector<Service> userList; // 같은 방에 접속한 클라이언트들
    String title; //방제목
    String manager;  //방장
    int count;    //방 인원수

    public Room() {
    	userList = new Vector<>();
	} 
}
