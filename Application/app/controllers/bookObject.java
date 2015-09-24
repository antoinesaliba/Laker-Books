package controllers;

public class bookObject{

    public long isbn;
    public String title;
    public String authors;
    public String edition;
    public String state;
    public int price;
    public String seller;
    public String buyer;


    public bookObject(long isbn, String title, String authors, String edition, String state, int price, String seller, String buyer){
        this.isbn=isbn;
        this.title=title;
        this.authors=authors;
        this.edition=edition;
        this.state=state;
        this.price=price;
        this.seller=seller;
        this.buyer=buyer;
    }
}
