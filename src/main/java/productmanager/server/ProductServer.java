package productmanager.server;

import org.json.JSONArray;
import org.json.JSONObject;
import productmanager.Product;
import productmanager.RequestCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductServer {
    // 필드
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private List<Product> products;
    private int sequence = 0;

    //
    public static void main(String[] args) {
        // 서버 객체 생성
        ProductServer productServer = new ProductServer();

        try {
            productServer.start();
        } catch (IOException e) {
            System.out.println(e.getMessage());
//            productServer.stop();
        }
    }

    private void start() throws IOException {
        // 소켓에 포트 바인딩 (전화기에 번호부여)
        serverSocket = new ServerSocket(50001);
        threadPool = Executors.newFixedThreadPool(100);
        products = new Vector<>();

        // test용 dummy 데이터
        products.add(
                new Product(sequence++, "삼다수", 1000, 20)
        );

        System.out.println("[서버] 시작됨");

        while (true) {
            // 이제 해당 클라이언트와 리턴된 소켓을 통해 서버와 TCP 통신을 할 수 있다.
            Socket socket = serverSocket.accept();
            SocketClient sc = new SocketClient(socket);
        }
    }

    // 중첩 클래스
    public class SocketClient {
        private Socket socket;
        // 해당 클라이언트로부터 요청을 받을 때 사용
        private DataInputStream dis;
        // 해당 클라이언트로 응답을 보낼 때 사용
        private DataOutputStream dos;

        public SocketClient(Socket socket) {
            try {
                this.socket = socket;
                this.dis = new DataInputStream(socket.getInputStream());
                this.dos = new DataOutputStream(socket.getOutputStream());

                receive();
            } catch (IOException e) {}
        }

        // 요청 (클라이언트 -> 서버)
            // 데이터(JSON)를 읽고 어떤 요청인지 확인
            // dis을 통해 데이터를 읽음
        // String -> 객체화 시켜서 사용
        // {
        //      menu: 메뉴 번호,
        //      data: { … }
        // }
        public void receive() {
            threadPool.execute(()->{
                try {
                    while(true) {
                        String receiveJson = dis.readUTF();

                        JSONObject request = new JSONObject(receiveJson);
                        int menu = request.getInt("menu");

                        switch (menu) {
                            case RequestCode.READ:
                                list();
                                break;
                            case RequestCode.CREATE:
                                create(request);
                                break;
                            case RequestCode.UPDATE:
                                update(request);
                                break;
                            case RequestCode.DELETE:
                                delete(request);
                                break;
                        }
                    }
                } catch (IOException e) {
                    close();
                }
            });
        }

        private void close() {
            try {
                socket.close();
            } catch (IOException e) {}
            System.out.println("[클라이언트] 종료됨");
        }

        // list
        // HTTP의 GET 메소드랑 비슷한 동작
        // 상품 목록을 보여줌 (서버 -> 클라이언트)
        public void list() throws IOException {
            JSONArray data = new JSONArray();
            // Product 객체 -> JSON 타입으로 변환
            for (Product p : products) {
                JSONObject product = new JSONObject();
                product.put("no", p.getNo());
                product.put("name", p.getName());
                product.put("price", p.getPrice());
                product.put("stock", p.getStock());
                data.put(product);
            }
            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("data", data);
            dos.writeUTF(response.toString());
            dos.flush();
        }
        // create
        // 클라이언트가 상품을 만들어달라고 요청
        // 만든 상품은 서버에 저장됨
        public void create(JSONObject request) throws IOException {
            // data 안에 상품이름, 가격, 재고수량
            JSONObject data = request.getJSONObject("data");
            Product product = new Product();
            product.setNo(sequence++);
            product.setName(data.getString("name"));
            product.setPrice(data.getInt("price"));
            product.setStock(data.getInt("stock"));
            
            products.add(product);
            
            // response 보내기
            // 1. JSON만들기
            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("data", "");
            // 2. 직렬화하기(문자열화)
            dos.writeUTF(response.toString());
            dos.flush();
        }
        // update
        public void update(JSONObject request) throws IOException {
            // 받은 JSON파일을 파싱해서 읽기
            JSONObject data = request.getJSONObject("data");
            // 해당하는 상품정보 업데이트
            int no = data.getInt("no");
            for(int i = 0; i < products.size(); i++) {
                Product product = products.get(i);
                if(product.getNo() == no) {
                    product.setName(data.getString("name"));
                    product.setPrice(data.getInt("price"));
                    product.setStock(data.getInt("stock"));
                }
            }
            // 클라이언트한테 response
            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("data", new JSONObject());
            dos.writeUTF(response.toString());
            dos.flush();
        }
        // delete
        public void delete(JSONObject request) throws IOException {
            // 데이터 읽고
            JSONObject data = request.getJSONObject("data");
            int no = data.getInt("no");
            // 해당 no 이 products 안에 있는지 확인하고 삭제
            Iterator<Product> iterator = products.iterator();
            while(iterator.hasNext()) {
                Product product = iterator.next();
                if (product.getNo() == no) {
                    iterator.remove();
                }
            }

            // response
            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("data", new JSONObject());
            dos.writeUTF(response.toString());
            dos.flush();
        }
    }
}
