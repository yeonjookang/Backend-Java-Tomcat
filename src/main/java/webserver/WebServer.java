package webserver;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class WebServer {

    /**
     * HTTP 요청을 받고, 응답을 보내는 역할의 웹 서버
     */

    private static final int DEFAULT_PORT = 80; // 포트 번호 80으로 지정

    /**
     * FIFO 구조를 멀티 스레딩 구현함으로서 먼저 해결된 요청 먼저 전달하도록 변경
     * 1. DEFAULT_THREAD_NUM 수의 스레드를 가진 스레드 풀 생성
     * 2. 나머지 작업은 큐에서 대기함
     * 3. 과도한 스레드 생성 및 관리로 인한 성능 문제 방지
     */

    private static final int DEFAULT_THREAD_NUM = 50;
    private static final Logger log = Logger.getLogger(WebServer.class.getName());

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        ExecutorService service = Executors.newFixedThreadPool(DEFAULT_THREAD_NUM);

        if (args.length != 0) {
            //커맨드 라인에서 입력받은 포트 번호를 사용하거나 기본 포트 번호를 사용
            port = Integer.parseInt(args[0]);
        }

        /**
         * 요청에 대한 처리를 모두 하나의 포트에서 처리하면 과부화
         * 따라서 환영 포트(welcomeSocket)는 80으로 하되, 연결 소켓(connection)은 매번 다른 포트 번호로 넘긴다.
         */

        // TCP 환영 소켓 : 클라이언트의 연결을 수락하기 위한 환영 소켓
        try (ServerSocket welcomeSocket = new ServerSocket(port)){

            // 연결 소켓 : 실제 연결된 클라이언트와의 통신을 담당
            Socket connection;
            while ((connection = welcomeSocket.accept()) != null) {
                /**
                 * 스레드에 Socket을 전달 -> 다수의 클라이언트 요청을 병렬로 처리 가능
                 * RequestHandler 클래스가 클라이언트와의 통신 및 HTTP 요청 처리 담당
                 */
                service.submit(new RequestHandler(connection));
            }
        }

    }
}
