package com.mattrjacobs.model;

public class BookRecommendation {
    private final String bookTitle;
    private final String bookAuthor;
    private double relevance;

    public BookRecommendation(final Book book, final double relevance) {
        this.bookTitle = book.getTitle();
        this.bookAuthor = book.getAuthor();
        this.relevance = relevance;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public String getBookAuthor() {
        return bookAuthor;
    }

    public double getRelevance() {
        return relevance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BookRecommendation that = (BookRecommendation) o;

        if (Double.compare(that.relevance, relevance) != 0) {
            return false;
        }
        if (!bookTitle.equals(that.bookTitle)) {
            return false;
        }
        return bookAuthor.equals(that.bookAuthor);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = bookTitle.hashCode();
        result = 31 * result + bookAuthor.hashCode();
        temp = Double.doubleToLongBits(relevance);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "BookRecommendation{" +
                "bookTitle='" + bookTitle + '\'' +
                ", bookAuthor='" + bookAuthor + '\'' +
                ", relevance=" + relevance +
                '}';
    }
}
