package productmanager.db;

import org.json.JSONObject;
import productmanager.Product;

import java.sql.*;
import java.util.Vector;

public class ProductRepository {
    public Connection conn = null;

    public ProductRepository() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/productmanager",
                    "root",
                    "3042");
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        System.out.println("DB 연결 성공");
    }

    public void add(JSONObject request) {
        try {
            JSONObject data = request.getJSONObject("data");
            String sql = "" +
                    "INSERT INTO products (name, price, stock) "
                    + "VALUES (?, ?, ?)";
            // PreparedStatement
            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, data.getString("name"));
            pstmt.setInt(2, data.getInt("price"));
            pstmt.setInt(3, data.getInt("stock"));

            int rows = pstmt.executeUpdate();
            System.out.println("저장된 행 수: " + rows);

            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(Product product) {
        try {
            String sql = "UPDATE products SET name=?, price=?, stock=? WHERE no=?";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, product.getName());
            pstmt.setInt(2, product.getPrice());
            pstmt.setInt(3, product.getStock());
            pstmt.setInt(4, product.getNo());

            int rows = pstmt.executeUpdate();
            System.out.println("수정된 행 수: " + rows);

            pstmt.close();
        } catch (SQLException e) {

        }
    }

    public void delete(int no) {
        try {
            String sql = "DELETE FROM products WHERE no=?";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, no);

            int rows = pstmt.executeUpdate();
            System.out.println("삭제된 행 수: " + rows);

            pstmt.close();
        } catch (SQLException e) {

        }
    }

    public Vector<Product> getProducts() {

        Vector<Product> products = new Vector<>();

        try {
            String sql = "SELECT * FROM products";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            //SQL문 실행 후, ResultSet을 통해 데이터 읽기
            ResultSet rs = pstmt.executeQuery();

            while(rs.next()) {
                Product product = new Product();
                product.setNo(rs.getInt("no"));
                product.setName(rs.getString("name"));
                product.setPrice(rs.getInt("price"));
                product.setStock(rs.getInt("stock"));

                products.add(product);
            }
            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            return products;
        }
    }
}
