package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestHandler implements Runnable{

    /**
     * 연결 소켓을 전달받아 클라이언트의 요청을 처리하고 응답을 생성하는 클래스
     */

    Socket connection;
    private static final Logger log = Logger.getLogger(RequestHandler.class.getName());

    public RequestHandler(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        log.log(Level.INFO, "New Client Connect! Connected IP : " + connection.getInetAddress() + ", Port : " + connection.getPort());
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()){

            /**
             * connection 객체를 이용해서 클라이언트로부터의 입력 스트림을 얻어와 요청 데이터를 읽을 수 있다.
             * 또한 출력 스트림을 얻어와 HTTP 응답을 클라이언트에게 보낼 수 있다.
             */

            BufferedReader br = new BufferedReader(new InputStreamReader(in)); //줄 단위로 읽어서 요청 헤더 및 바디를 쉽게 처리
            DataOutputStream dos = new DataOutputStream(out); //바이트 데이터를 클라이언트에게 보내는 데에 사용

            /**
             * br의 첫번째 줄은 다음과 같은 정보가 들어있다.
             * HTTP_METHOD URL HTTP_VERSION
             */
            String requestLine = br.readLine(); // 첫 번째 줄을 읽음
            String[] requestParts = requestLine.split(" "); // 공백으로 분리하여 HTTP 메서드, URL, HTTP 버전을 얻음

            //byte[] body = "Hello World".getBytes();

            if (requestParts.length >= 2) { //올바르지 않은 HTTP 형식 예외 처리
                String httpMethod = requestParts[0];
                String url = requestParts[1];

                // url에는 요청한 자원의 경로와 쿼리 문자열이 포함됨
                System.out.println("HTTP Method: " + httpMethod);
                System.out.println("URL: " + url);

                byte[] body;

                // String은 객체이므로 == 비교 연산은 주소 비교가 이루어짐!
                if(url.equals("/") || url.equals("/index.html")){

                    /**
                     * Files 라이브러리를 이용해서 파일의 정보를 바이트 단위로 변환
                     */

                    body = Files.readAllBytes(Paths.get("C:\\Users\\rkddu\\KUIT2_Server_Mission_3\\Backend-Java-Tomcat\\webapp\\index.html"));
                    response200Header(dos, body.length); // 응답의 헤더를 생성
                    responseBody(dos, body); //응답의 본문을 생성
                }else{
                    body = "".getBytes();
                }
                response200Header(dos, body.length); // 응답의 헤더를 생성
                responseBody(dos, body); //응답의 본문을 생성
            }


        } catch (IOException e) {
            log.log(Level.SEVERE,e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

}