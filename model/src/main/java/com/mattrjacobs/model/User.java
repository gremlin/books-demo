package com.mattrjacobs.model;

public class User {

    private final long id;
    private final String name;

    public User(final long id, final String name) {
        this.id = id;
        this.name = name;
    }

    public static User fromToken(final String token) {
        final int hashOfToken = Math.abs(token.hashCode());
        final int mod = hashOfToken % 1000;
        return new User(mod, "user-" + mod);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User user = (User) o;

        if (id != user.id) {
            return false;
        }
        return name.equals(user.name);
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
