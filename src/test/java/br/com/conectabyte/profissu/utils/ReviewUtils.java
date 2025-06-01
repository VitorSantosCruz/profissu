package br.com.conectabyte.profissu.utils;

import br.com.conectabyte.profissu.entities.RequestedService;
import br.com.conectabyte.profissu.entities.Review;
import br.com.conectabyte.profissu.entities.User;

public class ReviewUtils {
  public static Review create(User user, RequestedService requestedService) {
    final var review = new Review();

    review.setReview("Revew Test");
    review.setStars(1);
    review.setTitle("Title Test");
    review.setUser(user);
    review.setRequestedService(requestedService);

    return review;
  }
}
