package controllers;

import play.*;
import play.mvc.*;
import play.data.Form;
import play.api.db.*;

import java.sql.*;

import views.html.*;

import javax.xml.parsers.ParserConfigurationException;

public class Application extends Controller {

    public Result index() {
        return ok(index.render());
    }

    public Result showResults(){
    	Connection conn = null;
        Statement stmt = null;
    	String input = null;

    	input = Form.form().bindFromRequest().get("input");

    	try{

    		conn = DriverManager.getConnection(
                       "jdbc:mysql://localhost:3306/lakerbooks", "root", "password"); // Opens connection with mysql database
            
            stmt = conn.createStatement();
            
            String sqlStr = "select * from item where isbn =" + "'" + input + "'" + ";"; 
            System.out.println(sqlStr);
            ResultSet resp = stmt.executeQuery(sqlStr);
            while(resp.next()!=false)
            	System.out.println(resp.getString("title"));


        }catch (SQLException k) {
            k.printStackTrace(); //redirect(routes.Application.errorf());
        }

        return ok(results.render());
    }

}
