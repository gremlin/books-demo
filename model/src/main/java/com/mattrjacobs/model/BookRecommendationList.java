package com.mattrjacobs.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BookRecommendationList {
    private final ResponseType responseType;
    private final int userId;
    private final List<BookRecommendation> recommendations;

    private final static List<Book> CORPUS = new ArrayList<>();

    public ResponseType getResponseType() {
        return responseType;
    }

    public int getUserId() {
        return userId;
    }

    public List<BookRecommendation> getRecommendations() {
        return recommendations;
    }

    static {
        addBook("The Stand", "Stephen King");
        addBook("The Andromeda Strain", "Michael Crichton");
        addBook("Cryptonomicon", "Neal Stephenson");
        addBook("Seveneves", "Neal Stephenson");
        addBook("Cloud Atlas", "David Mitchell");
        addBook("Infinite Jest", "David Foster Wallace");
        addBook("House of Leaves", "Mark Z Danielewski");
        addBook("Cat's Cradle", "Kurt Vonnegut");
        addBook("Things Fall Apart", "Chinua Achebe");
        addBook("The Great Gatsby", "F Scott Fitzgerald");
        addBook("To Kill a Mockingbird", "Harper Lee");
        addBook("Pride and Prejudice", "Jane Austen");
        addBook("1984", "George Orwell");
        addBook("Fahrenheit 451", "Ray Bradbury");
        addBook("Brave New World", "Aldous Huxley");
        addBook("Animal Farm", "George Orwell");
        addBook("Charlotte's Web", "EB White");
        addBook("The Lion, the Witch and the Wardrobe", "CS Lewis");
        addBook("The Grapes of Wrath", "John Steinbeck");
        addBook("Lord of the Flies", "William Golding");
        addBook("A Tale of Two Cities", "Charles Dickens");
        addBook("The Hitchhikers Guide to the Galaxy", "Douglas Adams");
    }

    private BookRecommendationList(final ResponseType responseType, final int userId, final List<BookRecommendation> recommendations) {
        this.responseType = responseType;
        this.userId = userId;
        this.recommendations = recommendations;
    }

    /*
     * Returns a stable set of books for each user.
     * Use modular arithmetic to index into the overall set of books (the CORPUS) and return a slice from there
     */
    public static BookRecommendationList randomFromUser(final ResponseType responseType, final int userId, final int count) {
        final int startingIndex = userId % CORPUS.size();

        final List<Book> recommendedBooks;
        if (count < CORPUS.size() - startingIndex) {
            //can just take a slice
            recommendedBooks = CORPUS.subList(startingIndex, startingIndex + count);
        } else {
            //need to wrap around
            recommendedBooks = new ArrayList<>(CORPUS.subList(startingIndex, CORPUS.size()));
            final int neededFromFront = count - CORPUS.size() + startingIndex;
            recommendedBooks.addAll(CORPUS.subList(0, neededFromFront));
        }

        final List<BookRecommendation> recs = IntStream.range(0, recommendedBooks.size())
                .mapToObj(i -> {
                    final Book book = recommendedBooks.get(i);
                    final double relevance = CORPUS.size() -  (1.3 * (i + 2)) + ((startingIndex + 0.0) / CORPUS.size());
                    return new BookRecommendation(book, relevance);
                })
                .collect(Collectors.toList());
        return new BookRecommendationList(responseType, userId, recs);
    }

    private static void addBook(final String title, final String author) {
        CORPUS.add(new Book(title, author));
    }

    public static BookRecommendationList fallback(final List<BookRecommendation> recs) {
        return new BookRecommendationList(ResponseType.Degraded, -1, recs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BookRecommendationList that = (BookRecommendationList) o;

        if (userId != that.userId) {
            return false;
        }
        if (responseType != that.responseType) {
            return false;
        }
        return recommendations.equals(that.recommendations);
    }

    @Override
    public int hashCode() {
        int result = responseType.hashCode();
        result = 31 * result + userId;
        result = 31 * result + recommendations.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BookRecommendationList{" +
                "responseType=" + responseType +
                ", userId=" + userId +
                ", recommendations=" + recommendations +
                '}';
    }
}
