/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.article.repositry;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.article.entity.Author;

/**
 * @author rrohan419@gmail.com 
 */
public interface AuthorRepository extends JpaRepository<Author, Long> {

}
