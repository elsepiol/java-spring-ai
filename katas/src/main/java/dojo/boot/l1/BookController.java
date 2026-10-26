package dojo.boot.l1;

import org.springframework.web.bind.annotation.RestController;

/**
 * KATA boot L1 "BookController" (tracks/07-spring-boot-web/L1-basics.md, sections 3-5).
 *
 * A REST resource over the given BookStore, with correct HTTP semantics:
 *   GET    /api/books        200, JSON array of books, in id order
 *   GET    /api/books/{id}   200 with the book, or 404 if there is none
 *   POST   /api/books        body {"title": "...", "author": "...", "priceCents": 1234}
 *                            201 Created, header Location: /api/books/{newId}, body: the new book
 *   DELETE /api/books/{id}   204 No Content, or 404 if there is none
 * Use a record for the request body. BookStore is injected through the constructor.
 */
@RestController
public class BookController {
}
