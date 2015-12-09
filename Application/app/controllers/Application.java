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
import java.io.*;
import java.net.MalformedURLException;
import java.lang.*;
import java.math.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Application extends Controller {
    static Properties mailServerProperties;
    static Session getMailSession;
    static MimeMessage generateMailMessage;

    public Result index() {
        return redirect(routes.Application.home(0));
    }

    public Result home(int action) {
        return ok(home.render(action));
    }

    public Result redirectHome(int action) {
        return redirect(routes.Application.home(action));
    }

    public Result showResults() {
        Connection conn = null;
        Statement stmt = null;
        String input = null;
        String sqlStr = null;
        ArrayList<bookObject>bookresults = new ArrayList<bookObject>();

        input = Form.form().bindFromRequest().get("input");

        try {
            parse();
            conn = DriverManager.getConnection(
                       session("1"), session("2"), session("3")); // Opens connection with mysql database

            stmt = conn.createStatement();

            String temp = input.replaceAll("-", ""); //Removes dashes from input and checks to see if ISBN, if not string input is still intact
            temp = temp.replace(" ","");

            //check to see if user entered ISBN or Title
            if (temp.matches("[0-9]+")) {
                if (temp.length() == 10)
                    sqlStr = "select * from books where isbn10 =" + "'" + temp + "'" + "and buyer is NULL;";
                else if (temp.length() == 13)
                    sqlStr = "select * from books where isbn13 =" + "'" + temp + "'" + "and buyer is NULL;";
            } else {
                session("title", input);
                return ok(views.html.editionbuy.render(false));
            }

            ResultSet resp = stmt.executeQuery(sqlStr);
            if (resp.next() == false) {
                return ok(views.html.error.render("No textbooks found that matched what you entered. Please try again."));
            } else {
                do {
                    bookresults.add(new bookObject(resp.getLong("id"), resp.getString("title"), resp.getString("authors"), resp.getString("edition"), resp.getString("isbn13"), resp.getString("isbn10"), resp.getString("state"), resp.getBigDecimal("price"), resp.getString("seller"), resp.getString("buyer"), resp.getString("imageURL")));
                } while (resp.next() != false);
            }
        } catch (SQLException k) {
            k.printStackTrace();
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
        }
        return ok(views.html.results.render(bookresults));
    }

    public Result buyTitle() {
        Connection conn = null;
        Statement stmt = null;
        String sqlStr = null;
        String title = session("title");
        String edition = Form.form().bindFromRequest().get("edition");
        ArrayList<bookObject>bookresults = new ArrayList<bookObject>();

        if (edition == null || edition.equals("")) {
            sqlStr = "select * from books where title like '%" + title + "%' and buyer is NULL;";
        } else {
            edition=edition.substring(0,1);
            sqlStr = "select * from books where title like " + "'%" + title + "%' and edition =" + "'" + edition + "'and buyer is NULL;";
        }
        try {
            parse();
            conn = DriverManager.getConnection(session("1"), session("2"), session("3")); // Opens connection with mysql database

            stmt = conn.createStatement();

            ResultSet resp = stmt.executeQuery(sqlStr);
            if (resp.next() == false) {
                return ok(views.html.error.render("Sorry, no one is selling the specified textbook on Laker Books. Please try again at a later time."));
            } else {
                do {
                    bookresults.add(new bookObject(resp.getLong("id"), resp.getString("title"), resp.getString("authors"), resp.getString("edition"), resp.getString("isbn13"), resp.getString("isbn10"), resp.getString("state"), resp.getBigDecimal("price"), resp.getString("seller"), resp.getString("buyer"), resp.getString("imageURL")));
                } while (resp.next() != false);
            }
        } catch (SQLException k) {
            k.printStackTrace();
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
        }
        return ok(views.html.results.render(bookresults));
    }

    public Result confirm() {
        session("id", Form.form().bindFromRequest().get("id"));
        return ok(views.html.confirm.render(false));
    }

    public Result sold() {
        String email = Form.form().bindFromRequest().get("email").toLowerCase();
        String fileName = "/Users/antoinesaliba/Downloads/Oswego_Contacts.xls";
        int rownr = 0, colnr = 0;
        int row = 0;

        try {
            InputStream input = new FileInputStream(fileName);

            HSSFWorkbook wb = new HSSFWorkbook(input);
            HSSFSheet sheet = wb.getSheetAt(0);

            row = findEmail(sheet, email);

        } catch (FileNotFoundException b) {
            b.printStackTrace();
        } catch (IOException c) {
            c.printStackTrace();
        }


        String id = session("id");
        if (email == null || email.equals("") || !email.contains("@oswego.edu") || row == 0)
            return ok(views.html.confirm.render(true));
        else {
            Connection conn = null;
            Statement stmt = null;
            Statement stmt2 = null;
            Statement stmt3 = null;
            String sqlStr = null;

            try {
                parse();
                conn = DriverManager.getConnection(
                           session("1"), session("2"), session("3")); // Opens connection with mysql database

                stmt = conn.createStatement();
                stmt2 = conn.createStatement();
                stmt3 = conn.createStatement();

                ResultSet resp = stmt.executeQuery("select * from books where id=" + id + ";");
                if (resp.next() == false) {
                    return ok(views.html.error.render("Sorry, it looks like someone bought the book a few seconds before you did."));
                } else {
                    String checkUser = "select * from users where email =" + "'" + email + "'" + ";"; //looks inside database to see if anything matches search bar input
                    ResultSet resp2 = stmt3.executeQuery(checkUser);
                    if (resp2.next() == false) {
                        String newUser = "insert into users (email, name, up, down) values (?,?,0,0)";
                        PreparedStatement user = conn.prepareStatement(newUser);
                        user.setString(1, email);
                        user.setString(2, "User");
                        user.execute();
                    } else {
                        if (resp2.getInt("down") == 1) {
                            return ok(views.html.error.render("Unfortunately, it looks like someone has reported you for misconduct and you have been banned from using this site."));
                        }
                    }

                    sqlStr = "update books set buyer = '" + email + "' where id = " + id + ";";
                    stmt2.execute(sqlStr);
                    sendMail(resp.getString("seller"), email, resp.getString("title"), resp.getBigDecimal("price"));
                    return redirectHome(1);
                }

            } catch (SQLException k) {
                k.printStackTrace();
                return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
            }
        }
    }

    public Result sellItem() {
        Connection conn = null;
        Statement stmt = null;
        String input = null;
        String apiRequest = null;

        input = Form.form().bindFromRequest().get("input");
        String temp = input.replaceAll("-", ""); //Removes dashes from input and checks to see if ISBN, if not string input is still intact
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            if (temp.matches("[0-9]+")) { //checks to see if user entered ISBN or Title
                if (temp.length() == 10)
                    apiRequest = "http://www.directtextbook.com/xml.php?key=14dbcbb1bb6ae2197d0e7352decd4bfd&isbn=" + temp;
                else if (temp.length() == 13)
                    apiRequest = "http://www.directtextbook.com/xml.php?key=14dbcbb1bb6ae2197d0e7352decd4bfd&ean=" + temp;
                else
                    return ok(views.html.error.render("No information found for the textbook you entered. Please try again."));
                System.out.println(apiRequest);
                Document doc = dBuilder.parse(new URL(apiRequest).openStream());
                doc.getDocumentElement().normalize();
                NodeList nList = doc.getElementsByTagName("book");

                if (nList.getLength() == 0) {
                    return ok(views.html.error.render("Unfortunately, no information was found for the textbook specified, please make sure you have entered the information correctly and try again."));
                } else {

                    Node nNode = nList.item(0);

                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        String isbn10 = null;
                        String isbn13 = null;
                        String title = null;
                        String authors = null;
                        String edition = null;
                        String image = null;

                        Element eElement = (Element) nNode;

                        title = eElement.getElementsByTagName("title").item(0).getTextContent().trim();

                        authors = eElement.getElementsByTagName("author").item(0).getTextContent().trim();
                        authors = authors.replaceAll(";", " & "); //make the output of the authors more readable

                        if (eElement.getElementsByTagName("edition").item(0) != null)
                            edition = eElement.getElementsByTagName("edition").item(0).getTextContent().trim().substring(0,1);
                        else
                            edition = "N/A";

                        isbn10 = eElement.getElementsByTagName("isbn").item(0).getTextContent().trim();
                        isbn13 = eElement.getElementsByTagName("ean").item(0).getTextContent().trim();

                        image = "http://www.directtextbook.com/large/" + isbn10 + ".jpg";

                        bookObject newbook = new bookObject(0, title, authors, edition, isbn13, isbn10, null, new BigDecimal(0.00), null, null, image);

                        session("title", title);
                        session("authors", authors);
                        session("edition", edition);
                        session("isbn13", isbn13);
                        session("isbn10", isbn10);
                        session("image", image);

                        return ok(views.html.sell.render(newbook, false, false, false));
                    } else
                        return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
                }


            } else {
                input = input.replaceAll("&", "and");
                session("title", input);
                return ok(views.html.edition.render(false));
            }
        } catch (ParserConfigurationException e) {
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
        } catch (MalformedURLException f) {
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
        } catch (IOException e) {
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
        } catch (SAXException e) {
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
        }
    }

    public Result sellTitle() {
        String edition = Form.form().bindFromRequest().get("edition");
        String auth = Form.form().bindFromRequest().get("author");
        String which = Form.form().bindFromRequest().get("which");
        boolean ed = true;

        String title = null;
        String titler = session("title");
        String apiRequest = null;
        ArrayList<bookObject>bookresults = new ArrayList<bookObject>();

        if (which.equals("yes")) {
            if (auth == null || auth.equals(""))
                return ok(views.html.authors.render(true));
            else
                edition = session("edition");
        } else {
            if (edition == null || edition.equals("") || edition.equals("N/A") || edition.equals("0")) {
                edition = "N/A";
                ed = false;
            }else{
                edition=edition.substring(0,1);
            }
        }

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            apiRequest = "http://www.directtextbook.com/xml_search.php?key=14dbcbb1bb6ae2197d0e7352decd4bfd&query=" + titler.replace(" ", "%20");
            System.out.println(apiRequest);
            Document doc = dBuilder.parse(new URL(apiRequest).openStream());
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("results");

            if (nList.getLength() < 2) {
                return ok(views.html.error.render("Unfortunately, no information was found for the textbook specified, please make sure you have entered the information correctly and try again."));
            } else {
                Node nNode = nList.item(1); //xml has two results tag, we want the second with all the books in it
                Element element = (Element) nNode;
                String isbn10 = null;
                String isbn13 = null;
                String authors = null;
                String image = null;

                NodeList books = element.getElementsByTagName("book"); //from the second results tag, get all the book subelements
                for (int i = 0; i < books.getLength(); i++) {
                    Node temp = books.item(i);
                    if (temp.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) temp;
                        if (eElement.getElementsByTagName("title").item(0).getTextContent().trim().toLowerCase().contains(titler.toLowerCase())) {
                            if (eElement.getElementsByTagName("format").item(0)!=null&&!eElement.getElementsByTagName("format").item(0).getTextContent().trim().toLowerCase().contains("code") && !eElement.getElementsByTagName("format").item(0).getTextContent().trim().toLowerCase().equals("ebook")) {
                                if (ed && eElement.getElementsByTagName("edition").item(0) != null && eElement.getElementsByTagName("edition").item(0).getTextContent().trim().equals(edition)) {
                                    title = eElement.getElementsByTagName("title").item(0).getTextContent().trim();
                                    authors = eElement.getElementsByTagName("author").item(0).getTextContent().trim();
                                    authors = authors.replaceAll(";", " & "); //make the output of the authors more readable
                                    isbn10 = eElement.getElementsByTagName("isbn").item(0).getTextContent().trim();
                                    isbn13 = eElement.getElementsByTagName("ean").item(0).getTextContent().trim();

                                    image = "http://www.directtextbook.com/large/" + isbn10 + ".jpg";

                                    bookObject newbook = new bookObject(0, title, authors, edition, isbn13, isbn10, null, new BigDecimal(0.00), null, null, image);
                                    bookresults.add(newbook);
                                } else if (!ed) {
                                    title = eElement.getElementsByTagName("title").item(0).getTextContent().trim();
                                    authors = eElement.getElementsByTagName("author").item(0).getTextContent().trim();
                                    authors = authors.replaceAll(";", " & "); //make the output of the authors more readable

                                    isbn10 = eElement.getElementsByTagName("isbn").item(0).getTextContent().trim();
                                    isbn13 = eElement.getElementsByTagName("ean").item(0).getTextContent().trim();

                                    image = "http://www.directtextbook.com/large/" + isbn10 + ".jpg";

                                    bookObject newbook = new bookObject(0, title, authors, edition, isbn13, isbn10, null, new BigDecimal(0.00), null, null, image);
                                    bookresults.add(newbook);
                                }
                            }
                        }

                    }
                }
                if (bookresults.size() == 0)
                    return ok(views.html.error.render("No information found for the textbook you entered. Please try again."));
                if (bookresults.size() == 1) {
                    session("title", title);
                    session("authors", authors);
                    session("edition", edition);
                    session("isbn13", isbn13);
                    session("isbn10", isbn10);
                    session("image", image);
                    return ok(views.html.sell.render(bookresults.get(0), false, false, false));
                } else if (auth != null && !auth.equals("")) {
                    int i = auth.indexOf(' ');
                    auth = auth.substring(0,i);
                    for (int b = 0; b < bookresults.size(); b++) {
                        if (bookresults.get(b).authors.toLowerCase().contains(auth.toLowerCase())) {
                            bookObject temp = bookresults.get(b);
                            session("title", temp.title);
                            session("authors", temp.authors);
                            session("edition", temp.edition);
                            session("isbn13", temp.isbn13);
                            session("isbn10", temp.isbn10);
                            session("image", temp.imageURL);
                            return ok(views.html.sell.render(temp, false, false, false));
                        }
                    }
                } else {
                    session("edition", edition);
                    return ok(views.html.authors.render(false));
                }
                return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
            }
        } catch (ParserConfigurationException e) {
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
        } catch (MalformedURLException f) {
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
        } catch (IOException e) {
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
        } catch (SAXException e) {
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
        }
    }
    public Result postItem() {
        Connection conn = null;
        Statement stmt = null;
        long millis = System.currentTimeMillis();

        //checks to make sure all fields have been entered
        boolean conditionError = false;
        boolean priceError = false;
        boolean emailError = false;

        //retrieve information from session that was found by API
        String title = session("title");
        String authors = session("authors");
        String edition = session("edition");
        String isbn13 = session("isbn13");
        String isbn10 = session("isbn10");
        String image = session("image");

        BigDecimal price = new BigDecimal(0.00);
        int row = 0;

        String condition = Form.form().bindFromRequest().get("optradio");
        if (!Form.form().bindFromRequest().get("price").equals(""))
            price = new BigDecimal(Form.form().bindFromRequest().get("price"));

        String seller_email = Form.form().bindFromRequest().get("email").toLowerCase();

        String fileName = "/Users/antoinesaliba/Downloads/Oswego_Contacts.xls";
        int rownr = 0, colnr = 0;

        try {
            InputStream input = new FileInputStream(fileName);

            HSSFWorkbook wb = new HSSFWorkbook(input);
            HSSFSheet sheet = wb.getSheetAt(0);

            row = findEmail(sheet, seller_email);

        } catch (FileNotFoundException b) {
            b.printStackTrace();
        } catch (IOException c) {
            c.printStackTrace();
        }

        if (row == 0)
            emailError = true;

        if (condition == null) {
            conditionError = true;
        }
        if (price.compareTo(BigDecimal.ZERO) == 0)
            priceError = true;
        if (seller_email.equals("") || seller_email == null)
            emailError = true;

        if (conditionError || priceError || emailError) {
            bookObject newbook = new bookObject(0, title, authors, edition, isbn13, isbn10, condition, price, seller_email, null, image);
            return ok(views.html.sell.render(newbook, conditionError, priceError, emailError));
        } else {

            try {
                parse();
                conn = DriverManager.getConnection(
                           session("1"), session("2"), session("3")); // Opens connection with mysql database

                stmt = conn.createStatement();

                String checkUser = "select * from users where email =" + "'" + seller_email + "'" + ";"; //looks inside database to see if anything matches search bar input
                ResultSet resp = stmt.executeQuery(checkUser);
                if (resp.next() == false) {
                    String newUser = "insert into users (email, name, up, down) values (?,?,0,0)";
                    PreparedStatement user = conn.prepareStatement(newUser);
                    user.setString(1, seller_email);
                    user.setString(2, "User");
                    user.execute();
                } else {
                    if (resp.getInt("down") == 1) {
                        return ok(views.html.error.render("Unfortunately, it looks like someone has reported you for misconduct and you have been banned from using this site."));
                    }
                }


                String command = "insert into books (id, title, authors, edition, isbn13, isbn10, state, price, seller,buyer, imageURL) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?);";
                PreparedStatement comm = conn.prepareStatement(command);
                comm.setLong(1, millis);
                comm.setString(2, title);
                comm.setString(3, authors);
                comm.setString(4, edition);
                comm.setString(5, isbn13);
                comm.setString(6, isbn10);
                comm.setString(7, condition);
                comm.setBigDecimal(8, price);
                comm.setString(9, seller_email);
                comm.setString(10, null);
                comm.setString(11, image);
                comm.execute();
            } catch (SQLException k) {
                k.printStackTrace();
                return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
            }
            return redirectHome(2);
        }
    }

    private static int findEmail(HSSFSheet sheet, String email) {
        int rowNum = 0;

        for (Row row : sheet) {
            Cell cell = row.getCell(0);
            if (cell.getRichStringCellValue().getString().equals(email)) {
                rowNum = row.getRowNum();
                return rowNum;
            }
        }
        return rowNum;
    }

    private void parse() {
        String fileName = "/Users/antoinesaliba/Documents/temp.txt";
        String line = null;
        ArrayList<String>inf = new ArrayList<String>();
        if (session("1") != null)
            return;
        else {
            try {
                FileReader fileReader = new FileReader(fileName);

                BufferedReader bufferedReader =
                    new BufferedReader(fileReader);
                session("1", bufferedReader.readLine());
                session("2", bufferedReader.readLine());
                session("3", bufferedReader.readLine());
                session("4", bufferedReader.readLine());
                session("5", bufferedReader.readLine());
                bufferedReader.close();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public Result report() {
        String youremail = Form.form().bindFromRequest().get("yours");
        String hisemail = Form.form().bindFromRequest().get("his");
        String explanation = Form.form().bindFromRequest().get("reason");
        Connection conn = null;
        Statement stmt = null;
        try {
            parse();
            conn = DriverManager.getConnection(
                       session("1"), session("2"), session("3")); // Opens connection with mysql database

            stmt = conn.createStatement();

            String checkUser = "select * from books where seller =" + "'" + hisemail + "'" + "and buyer =" + "'" + youremail + "';"; //looks inside database to see if anything matches search bar input
            ResultSet resp = stmt.executeQuery(checkUser);
            if (resp.next() == false) {
                String checkOther = "select * from books where seller =" + "'" + youremail + "'" + "and buyer =" + "'" + hisemail + "';";
                ResultSet resp2 = stmt.executeQuery(checkUser);
                if (resp2.next() != false) {
                    sendReport(youremail, hisemail, explanation);
                }
            } else {
                sendReport(youremail, hisemail, explanation);
            }
        } catch (SQLException k) {
            k.printStackTrace();
            return ok(views.html.error.render("Unfortunately, an error has occured. Sorry for the inconveniance, please try again."));
        }

        return redirectHome(0);
    }

    private void sendReport(String yours, String his, String explanation) {
        try {
            parse();
            mailServerProperties = System.getProperties();
            mailServerProperties.put("mail.smtp.port", "587");
            mailServerProperties.put("mail.smtp.auth", "true");
            mailServerProperties.put("mail.smtp.starttls.enable", "true");

            getMailSession = Session.getDefaultInstance(mailServerProperties, null);
            generateMailMessage = new MimeMessage(getMailSession);
            generateMailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(session("4")));
            generateMailMessage.setSubject("Reported User");
            String emailBody = yours + " has reported user " + his + ".\nHere is the explanation:\n" + explanation;
            generateMailMessage.setContent(emailBody, "text/html");

            Transport transport = getMailSession.getTransport("smtp");

            transport.connect("smtp.gmail.com", session("4"), session("5"));
            transport.sendMessage(generateMailMessage, generateMailMessage.getAllRecipients());
            transport.close();
        } catch (AddressException e) {
            e.printStackTrace();
        } catch (MessagingException f) {
            f.printStackTrace();
        }
    }

    private void sendMail(String seller, String buyer, String title, BigDecimal price) {
        try {
            parse();
            mailServerProperties = System.getProperties();
            mailServerProperties.put("mail.smtp.port", "587");
            mailServerProperties.put("mail.smtp.auth", "true");
            mailServerProperties.put("mail.smtp.starttls.enable", "true");

            getMailSession = Session.getDefaultInstance(mailServerProperties, null);
            generateMailMessage = new MimeMessage(getMailSession);
            generateMailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(seller));
            generateMailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(buyer));
            generateMailMessage.setSubject("Laker Books Buyer and Seller Match Found");
            String emailBody = "Hello " + buyer + " and " + seller + ",\n Congratulations on making this transaction on LakerBooks! " + buyer + " has agreed to buy " + title + " from " + seller + " for $" + price + ".\n You now have each other's emails so please agree on someplace to meet to complete the transaction.";
            generateMailMessage.setContent(emailBody, "text/html");
            Transport transport = getMailSession.getTransport("smtp");

            transport.connect("smtp.gmail.com", session("4"), session("5"));
            transport.sendMessage(generateMailMessage, generateMailMessage.getAllRecipients());
            transport.close();
        } catch (AddressException e) {
            e.printStackTrace();
        } catch (MessagingException f) {
            f.printStackTrace();
        }
    }
}
