package controllers;
import java.math.*;

public class bookObject{

    public long id;
    public String isbn13;
    public String isbn10;
    public String title;
    public String authors;
    public String edition;
    public String state;
    public BigDecimal price;
    public String seller;
    public String buyer;
    public String imageURL;


    public bookObject(long id, String title, String authors, String edition, String isbn13, String isbn10, String state, BigDecimal price, String seller, String buyer, String imageURL){
        this.id=id;
        this.title=title;
        this.authors=authors;
        this.edition=edition;
        this.isbn13=isbn13;
        this.isbn10=isbn10;
        this.state=state;
        this.price=price;
        this.seller=seller;
        this.buyer=buyer;
        this.imageURL=imageURL;
    }
}