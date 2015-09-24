package controllers;

import play.*;
import play.mvc.*;
import play.data.Form;
import play.api.db.*;

import java.sql.*;
import java.util.*;

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
    	ArrayList<bookObject>bookresults = new ArrayList<bookObject>();

    	input = Form.form().bindFromRequest().get("input");

    	try{

    		conn = DriverManager.getConnection(
                       "jdbc:mysql://localhost:3306/lakerbooks", "root", "password"); // Opens connection with mysql database
            
            stmt = conn.createStatement();
            
            String sqlStr = "select * from item where title =" + "'" + input + "'" + ";"; //looks inside database to see if anything matches search bar input
            ResultSet resp = stmt.executeQuery(sqlStr);
            while(resp.next()!=false){
            	bookresults.add(new bookObject(resp.getLong("isbn"),resp.getString("title"), resp.getString("authors"), resp.getString("edition"), resp.getString("state"), resp.getInt("price"), resp.getString("seller_email"), resp.getString("buyer_email")));
        	}
        }catch (SQLException k) {
            k.printStackTrace(); //redirect(routes.Application.errorf());
        }
        return ok(views.html.results.render(bookresults));
    }

}
