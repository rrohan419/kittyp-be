/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.kittyp.article.entity.Author;

/**
 * @author rrohan419@gmail.com 
 */
public interface AuthorDao {

	Author saveAuthor (Author author);

	Author authorById(Long id);

	Page<Author> getAllAuthors(Pageable pageable);
}

