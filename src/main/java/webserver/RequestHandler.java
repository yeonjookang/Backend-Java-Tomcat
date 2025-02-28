package webserver;

import db.MemoryUserRepository;
import http.util.IOUtils;
import model.User;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static http.util.HttpRequestUtils.parseQueryParameter;

public class RequestHandler implements Runnable{

    /**
     * 연결 소켓을 전달받아 클라이언트의 요청을 처리하고 응답을 생성하는 클래스
     */

    Socket connection;
    final String ROOT_URL="C:\\Users\\rkddu\\KUIT2_Server_Mission_3\\Backend-Java-Tomcat\\webapp";
    private static final Logger log = Logger.getLogger(RequestHandler.class.getName());
    MemoryUserRepository memoryUserRepository= MemoryUserRepository.getInstance();

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

            /**
             * 첫번째 줄 정보 외 content-length와 cookie 정보를 읽어오기 위한 반복문
             */
            int requestContentLength = 0;
            String cookie = "";

            while(true){
                final String line = br.readLine();
                if (line.equals("")){
                    break;
                }
                if (line.startsWith("Content-Length")){
                    requestContentLength = Integer.parseInt(line.split(": ")[1]);
                }
                if (line.startsWith("Cookie")){
                    cookie = line.split(": ")[1].split(";")[0];
                }
            }

            /**
             * 들어온 요청에 따른 매핑 시작
             */

            if (requestParts.length >= 2) { //올바르지 않은 HTTP 형식 예외 처리
                String method = requestParts[0];
                String url = requestParts[1];

                byte[] body;

                // String은 객체이므로 == 비교 연산은 주소 비교가 이루어짐!
                if(url.equals("/") || url.equals("/index.html")){ //미션 1번

                    /**
                     * Files 라이브러리를 이용해서 파일의 정보를 바이트 단위로 변환
                     */

                    body = Files.readAllBytes(Paths.get(ROOT_URL+"\\index.html"));
                    response200Header(dos, body.length); // 응답의 헤더를 생성
                    responseBody(dos, body); //응답의 본문을 생성


                }else if(url.equals("/user/form.html")){ //미션 2번

                    body = Files.readAllBytes(Paths.get(ROOT_URL+"\\user\\form.html"));
                    response200Header(dos, body.length); // 응답의 헤더를 생성
                    responseBody(dos, body); //응답의 본문을 생성

                }else if(url.startsWith("/user/signup")){ //미션 2,3번

                    if(method.equals("GET")){

                        /**
                         * get 방식으로 들어온 경우에는 url에 쿼리 파라미터 형식이 포함되어 있다.
                         * 따라서 url에 있는 정보를 파싱해서 데이터를 뽑아내자.
                         */

                        String[] split = url.split("\\?");
                        String s = split[1];

                        Map<String, String> queryParameter = parseQueryString(s);
                        User user = new User(queryParameter.get("userId"), queryParameter.get("password"), queryParameter.get("name"), queryParameter.get("email"));
                        memoryUserRepository.addUser(user);

                        System.out.println(user.getUserId());

                        body = Files.readAllBytes(Paths.get(ROOT_URL+"index.html"));
                        response200Header(dos, body.length); // 응답의 헤더를 생성
                        responseBody(dos, body); //응답의 본문을 생성
                    }

                    else if(method.equals("POST")){

                        /**
                         * sign up 버튼을 누르면서 요청 바디 부분에 form 정보를 쿼리 파라미터 형식으로 같이 넘긴다
                         * 요청 메시지를 읽을 때 적절한 조건을 설정하여 요청 메시지의 끝을 감지하고, 그 이후에는 더 이상 읽지 않도록 해야한다.
                         * IOUtils에 있는 readData 를 이용하여 contentLength 정보로 정확하게 데이터를 읽자
                         */

                        String queryString = IOUtils.readData(br, requestContentLength);

                        Map<String, String> queryParameters = parseQueryString(queryString);

                        String userId = queryParameters.get("userId");
                        String password = queryParameters.get("password");
                        String name = queryParameters.get("name");
                        String email = queryParameters.get("email");

                        User user = new User(userId, password, name, email);
                        memoryUserRepository.addUser(user);

                        Collection<User> all = memoryUserRepository.findAll();
                        System.out.println(all.size());

                        //미션 4번

                        /**
                         * post sign up이 끝났을 때 index.html로 리다이렉션해주기 위해 상태코드를 302로 반환!
                         * 3xx 응답 결과에 Location 헤더가 있으면 Location 위치로 자동 이동! (리다이엑션)
                         * <상태 코드 목록>
                         * 2xx : 요청 정상 처리
                         * 3xx : 요청을 완료하려면 추가 행동이 필요
                         * 4xx : 클라이언트 오류
                         * 5xx : 서버 오류
                         */
                        body="".getBytes();

                        response302Header(dos, body.length,"/"); // 응답의 헤더를 생성
                        responseBody(dos, body); //응답의 본문을 생성
                    }
                    else {
                        body = "".getBytes();
                        response200Header(dos, body.length); // 응답의 헤더를 생성
                        responseBody(dos, body); //응답의 본문을 생성
                    }

                }else if(url.equals("/user/login.html")){ //미션 5번

                    if(method.equals("GET")){
                        body = Files.readAllBytes(Paths.get(ROOT_URL+"\\user\\login.html"));
                        response200Header(dos, body.length); // 응답의 헤더를 생성
                        responseBody(dos, body); //응답의 본문을 생성
                    }
                }else if(url.equals("/user/login")){ //미션 5번
                    if(method.equals("POST")) {
                        String queryString = IOUtils.readData(br, requestContentLength);

                        Map<String, String> queryParameters = parseQueryString(queryString);

                        String userId = queryParameters.get("userId");
                        String password = queryParameters.get("password");

                        System.out.println(userId);

                        /**
                         * Repository에 존재하는 유저면 헤더에 Cookie: logined=true를 추가하고 index.html로 리다이렉트
                         * 실패하면 logined_failed.html로 리다이렉트
                         */

                        User findUser = memoryUserRepository.findUserById(userId);

                        if (findUser != null) {
                            if (findUser.getPassword().equals(password)) {
                                System.out.println("로그인 성공!");
                                //로그인 성공
                                body = "".getBytes();
                                responseLoginSuccessHeader(dos, body.length, "/index.html");
                                responseBody(dos, body); //응답의 본문을 생성
                            } else {
                                //로그인 실패
                                System.out.println("로그인 실패!");
                                body = "".getBytes();
                                response302Header(dos, body.length, "/login_failed.html");
                                responseBody(dos, body); //응답의 본문을 생성
                            }
                        } else {
                            //로그인 실패
                            System.out.println("로그인 실패!");
                            body = "".getBytes();
                            response302Header(dos, body.length, "/login_failed.html");
                            responseBody(dos, body); //응답의 본문을 생성
                        }
                    }
                }else if(url.equals("/login_failed.html")){
                    body = Files.readAllBytes(Paths.get(ROOT_URL+"\\user\\login_failed.html"));
                    response200Header(dos, body.length); // 응답의 헤더를 생성
                    responseBody(dos, body); //응답의 본문을 생성
                }
                else if(url.equals("/user/userList")){ //미션 6번
                    System.out.println(cookie);
                    if(cookie.equals("logined=true")){
                        body = Files.readAllBytes(Paths.get(ROOT_URL+"\\user\\list.html"));
                        response200Header(dos, body.length); // 응답의 헤더를 생성
                        responseBody(dos, body); //응답의 본문을 생성
                    }
                    else{
                        body="".getBytes();
                        response302Header(dos,body.length,"/user/login.html");
                        responseBody(dos, body);
                    }
                }else if(method.equals("GET")&&url.endsWith(".css")){ //미션 7번
                    body=Files.readAllBytes(Paths.get(ROOT_URL+"\\"+url));
                    response200HeaderWithCss(dos, body.length);
                    responseBody(dos, body);
                }else{
                    body = "".getBytes();
                    response200Header(dos, body.length); // 응답의 헤더를 생성
                    responseBody(dos, body); //응답의 본문을 생성
                }
            }


        } catch (IOException e) {
            log.log(Level.SEVERE,e.getMessage());
        }
    }

    private void response200HeaderWithCss(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
            dos.flush();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private void responseLoginSuccessHeader(DataOutputStream dos,  int lengthOfBodyContent,String redirectionURL) {
        try {
            dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
            dos.writeBytes("Location: "+redirectionURL+"\r\n");
            dos.writeBytes("Set-Cookie: logined=true" + "\r\n");
            dos.writeBytes("\r\n");
            dos.flush();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos, int lengthOfBodyContent,String redirectionURL) {
        try {
            dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
            dos.writeBytes("Location: "+redirectionURL+"\r\n");
            dos.writeBytes("\r\n");
            dos.flush();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private Map<String, String> parseQueryString(String queryString) {

        /**
         * 쿼리 스트링의 내용을 &으로 한번 쪼갰다가,
         * = 으로 key, value로 한번 더 쪼개서 Map 자료구조에 넣어 반환하는 메소드
         */

        Map<String, String> queryParameters = new HashMap<>();
        String[] parameterPairs = queryString.split("&");

        for(String parameterPair : parameterPairs) {
            String[] keyValue = parameterPair.split("=");
            if(keyValue.length==2){
                String key = keyValue[0];
                String value = keyValue[1];
                queryParameters.put(key,value);
            }
        }
        return queryParameters;
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
            dos.flush();
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