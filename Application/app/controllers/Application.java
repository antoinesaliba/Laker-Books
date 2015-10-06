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
    	String sqlStr = null;
    	ArrayList<bookObject>bookresults = new ArrayList<bookObject>();

    	input = Form.form().bindFromRequest().get("input");

    	try{

    		conn = DriverManager.getConnection(
                       "jdbc:mysql://localhost:3306/lakerbooks", "root", "password"); // Opens connection with mysql database
            
            stmt = conn.createStatement();

            String temp = input.replaceAll("-", ""); //Removes dashes from input and checks to see if ISBN, if not string input is still intact
	    	
	    	//check to see if user entered ISBN or Title
	    	if (temp.matches("[0-9]+")) {
	    		if(temp.length() == 10)
	    			sqlStr="select * from books where isbn10 =" + "'" + temp + "'" + ";";
	    		else if(temp.length() == 13)
	    			sqlStr="select * from books where isbn13 =" + "'" + temp + "'" + ";";
            }else{
            	session("title", input);
            	return ok(views.html.editionbuy.render());
            }	

            ResultSet resp = stmt.executeQuery(sqlStr);
            if(resp.next()==false){
                return ok(views.html.error.render("No textbooks found that matched what you entered. Please try again."));
            }else{
                while(resp.next()!=false){
            	   bookresults.add(new bookObject(resp.getLong("id"),resp.getString("title"), resp.getString("authors"), resp.getString("edition"), resp.getString("isbn13"), resp.getString("isbn10"), resp.getString("state"), resp.getInt("price"), resp.getString("seller_email"), resp.getString("buyer_email"), resp.getString("image_url")));
        	   }
            }
        }catch (SQLException k) {
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
        }
        return ok(views.html.results.render(bookresults));
    }

    public Result buyTitle(){
    	Connection conn = null;
        Statement stmt = null;
    	String title = session("title");
    	String edition = Form.form().bindFromRequest().get("edition");
		ArrayList<bookObject>bookresults = new ArrayList<bookObject>();

    	String sqlStr = "select * from books where title =" + "'" + title + "' and edition =" + "'" + edition + "';";
    	System.out.println(sqlStr);

    	try{
    		conn = DriverManager.getConnection(
                       "jdbc:mysql://localhost:3306/lakerbooks", "root", "password"); // Opens connection with mysql database
            
            stmt = conn.createStatement();
            
    		ResultSet resp = stmt.executeQuery(sqlStr);
            if(resp.next()==false){
                return ok(views.html.error.render("We are very sorry but there was no textbook found that matched what you entered. Please try again."));
            }else{
                while(resp.next()!=false){
                   bookresults.add(new bookObject(resp.getLong("id"),resp.getString("title"), resp.getString("authors"), resp.getString("edition"), resp.getString("isbn13"), resp.getString("isbn10"), resp.getString("state"), resp.getInt("price"), resp.getString("seller_email"), resp.getString("buyer_email"), resp.getString("image_url")));
               }
            }
    	}catch (SQLException k) {
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
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
                else
                    return ok(views.html.error.render("No information found for the textbook you entered. Please try again."));
	    		Document doc = dBuilder.parse(new URL(apiRequest).openStream());
	    		doc.getDocumentElement().normalize();
	    		NodeList nList = doc.getElementsByTagName("book");

	    		if (nList.getLength() == 0) {
	                return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
	            } else {

	                Node nNode = nList.item(0);

	                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
	                	String isbn10=null;
	                	String isbn13=null;
	                	String title=null;
	                	String authors=null;
	                	String edition=null;
	                	String image=null;

	                    Element eElement = (Element) nNode;

						title = eElement.getElementsByTagName("title").item(0).getTextContent().trim();
						
						authors = eElement.getElementsByTagName("author").item(0).getTextContent().trim();
						authors = authors.replaceAll(";"," & "); //make the output of the authors more readable
						
						edition = eElement.getElementsByTagName("edition").item(0).getTextContent().trim();
						if (edition.equals("0")) //if edition is 0, make it display N/A instead
							edition="N/A";
						
						isbn10 = eElement.getElementsByTagName("isbn").item(0).getTextContent().trim();
	                    isbn13 = eElement.getElementsByTagName("ean").item(0).getTextContent().trim();

	                    image = "http://www.directtextbook.com/large/"+isbn10+".jpg";

	                    bookObject newbook = new bookObject(0,title,authors,edition,isbn13,isbn10,null,0,null,null,image);
	                    
	                	session("title", title);
	                	session("authors", authors);
	                	session("edition", edition);
	                	session("isbn13", isbn13);
	                	session("isbn10", isbn10);
	                	session("image", image);
	                    
	                    return ok(views.html.sell.render(newbook,false,false,false));
	                }else
                        return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
	            }


	    	}else{
	    		//apiRequest="http://www.directtextbook.com/xml_search.php?key=14dbcbb1bb6ae2197d0e7352decd4bfd&query="+input;
	    		return ok(views.html.edition.render());
	    	}
    } catch (ParserConfigurationException e) {
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
        }catch (MalformedURLException f) {
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
        }catch (IOException e) {
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
        }catch (SAXException e) {
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
        }
    }

    public Result sellTitle(){
    	String edition = Form.form().bindFromRequest().get("edition");
    	return ok(index.render());
    }
    public Result postItem(){
    	Connection conn = null;
        Statement stmt = null;
        long millis = System.currentTimeMillis();

        //checks to make sure all fields have been entered
        boolean conditionError=false;
        boolean priceError=false;
        boolean emailError=false;

        //retrieve information from session that was found by API
    	String title = session("title");  
    	String authors = session("authors");
    	String edition = session("edition");
    	String isbn13 = session("isbn13");
    	String isbn10 = session("isbn10");
    	String image = session("image");

    	int price = 0;

    	String condition = Form.form().bindFromRequest().get("condition");
    	if(!Form.form().bindFromRequest().get("price").equals(""))
    		price = Integer.parseInt(Form.form().bindFromRequest().get("price"));
    	String seller_email = Form.form().bindFromRequest().get("email");

    	if(condition==null){
    		conditionError=true;
    	}
    	if(price==0)
    		priceError=true;
    	if(seller_email==""||seller_email==null)
    		emailError=true;

    	if(conditionError||priceError||emailError){
    		bookObject newbook = new bookObject(0,title,authors,edition,isbn13,isbn10,condition,price,seller_email,null,image);
    		return ok(views.html.sell.render(newbook, conditionError, priceError, emailError));
    	}else{

	    	try{
		    	conn = DriverManager.getConnection(
		                       "jdbc:mysql://localhost:3306/lakerbooks", "root", "password"); // Opens connection with mysql database
		            
		        stmt = conn.createStatement();

		        String checkUser = "select * from users where email =" + "'" + seller_email + "'" + ";"; //looks inside database to see if anything matches search bar input
	            ResultSet resp = stmt.executeQuery(checkUser);
	            if(resp.next()==false){
	            	String newUser = "insert into users (email, name, plus, minus) values (?,?,0,0)";
	            	PreparedStatement user = conn.prepareStatement(newUser);
			        user.setString(1, seller_email);
			        user.setString(2, "Bob");
			        user.execute();
	            }

		            
		        String command = "insert into books (id, title, authors, edition, isbn13, isbn10, state, price, seller_email,buyer_email, image_url) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?);";
		        PreparedStatement comm = conn.prepareStatement(command);
		        comm.setLong(1, millis);
		        comm.setString(2, title);
		        comm.setString(3, authors);
		        comm.setString(4, edition);
		        comm.setString(5, isbn13);
		        comm.setString(6, isbn10);
		        comm.setString(7, condition);
		        comm.setInt(8, price);
		        comm.setString(9, seller_email);
		        comm.setString(10, null);
		        comm.setString(11, image);
		        comm.execute();
		    }catch (SQLException k) {
	            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
	        }
		    return ok(index.render());
		}
    }
}
