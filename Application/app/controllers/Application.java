package controllers;

import play.*;
import play.mvc.*;
import play.data.Form;
import play.api.db.*;

import java.sql.*;
import java.util.*;

import views.html.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.net.URL;
import org.w3c.dom.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.net.MalformedURLException;

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

    public Result sellItem(){
    	Connection conn = null;
        Statement stmt = null;
        String input = null;
        String apiRequest = null;

    	input = Form.form().bindFromRequest().get("input");
    	String temp = input.replaceAll("-", ""); //Removes dashes from input and checks to see if ISBN, if not string input is still intact

    	try{
    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    	
    	if (temp.matches("[0-9]+")) { //checks to see if user entered ISBN or Title
    		if(temp.length() == 10)
    			apiRequest="http://www.directtextbook.com/xml_buyback.php?key=14dbcbb1bb6ae2197d0e7352decd4bfd&isbn="+temp;
    		else if(temp.length() == 13)
    			apiRequest="http://www.directtextbook.com/xml_buyback.php?key=14dbcbb1bb6ae2197d0e7352decd4bfd&ean="+temp;
    		Document doc = dBuilder.parse(new URL(apiRequest).openStream());
    		doc.getDocumentElement().normalize();
    		NodeList nList = doc.getElementsByTagName("book");

    		if (nList.getLength() == 0) {
                return ok(index.render());
            } else {

                Node nNode = nList.item(0);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                	long isbn10=0;
                	long isbn13=0;
                	String title=null;
                	String authors=null;
                	String edition=null;

                    Element eElement = (Element) nNode;

                    isbn10 = Long.parseLong(eElement.getElementsByTagName("isbn").item(0).getTextContent().trim());
                    isbn13 = Long.parseLong(eElement.getElementsByTagName("ean").item(0).getTextContent().trim());
					title = eElement.getElementsByTagName("title").item(0).getTextContent().trim();
					//authors = eElement.getElementsByTagName("author").item(0).getTextContent().trim();
					edition = eElement.getElementsByTagName("edition").item(0).getTextContent().trim();


                    bookObject newbook = new bookObject(isbn13,title,null,edition,null,0,null,null);
                    return ok(views.html.sell.render(newbook));
                }
            }


    	}else{
    		apiRequest="http://www.directtextbook.com/xml_search.php?key=14dbcbb1bb6ae2197d0e7352decd4bfd&query="+input;
    	}
    } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }catch (MalformedURLException f) {
            f.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }catch (SAXException e) {
            e.printStackTrace();
        }
        return ok(index.render());
    }
}
